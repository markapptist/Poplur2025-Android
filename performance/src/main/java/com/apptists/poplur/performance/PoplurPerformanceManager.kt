package com.apptists.poplur.performance

import android.app.ActivityManager
import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Process-wide Android thermal, power, lifecycle, renderer, and cache coordinator.
 *
 * A registration never strongly retains its renderer. Heavy registrations are arbitrated so at
 * most one visible foreground renderer is active; the most recently made visible wins, and the
 * prior visible renderer is paused immediately. Lightweight registrations may coexist, but still
 * stop with process visibility and receive the same quality/FPS policy.
 */
class PoplurPerformanceManager private constructor(private val application: Application) :
    DefaultLifecycleObserver,
    ComponentCallbacks2 {

    class RendererRegistration internal constructor(
        manager: PoplurPerformanceManager,
        private val id: Long,
    ) : AutoCloseable {
        private val manager = WeakReference(manager)
        private val closed = AtomicBoolean(false)

        fun setVisible(visible: Boolean) {
            if (!closed.get()) manager.get()?.setRendererVisible(id, visible)
        }

        override fun close() {
            if (closed.compareAndSet(false, true)) manager.get()?.unregisterRenderer(id)
        }
    }

    private data class RendererRecord(
        val id: Long,
        val renderer: WeakReference<PoplurManagedRenderer>,
        val heavy: Boolean,
        var visible: Boolean,
        var visibilityOrder: Long,
        var lastActive: Boolean = false,
    )

    private data class Notification(
        val renderer: PoplurManagedRenderer,
        val active: Boolean,
        val releaseInactiveResources: Boolean,
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private val lock = Any()
    private val nextRendererId = AtomicLong(1L)
    private var visibilityOrder = 0L
    private val renderers = LinkedHashMap<Long, RendererRecord>()
    private val debug = (application.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    private val powerManager = application.getSystemService(Context.POWER_SERVICE) as PowerManager
    val deviceResourceTier: PoplurDeviceResourceTier = detectResourceTier(application)
    val assetCache = PoplurAssetCache(
        maximumBytes = deviceResourceTier.cacheBudgetBytes,
        maximumEntries = deviceResourceTier.maxCacheEntries,
        debugLogger = ::log,
    )

    @Volatile private var thermalCondition = PoplurThermalCondition.UNKNOWN
    @Volatile private var memoryPressure = PoplurMemoryPressure.NONE
    @Volatile private var powerSaveMode = powerManager.isPowerSaveMode
    @Volatile private var appInForeground = false
    @Volatile private var appInteractive = false
    @Volatile private var memoryWarningCount = 0
    @Volatile private var activeHeavyRendererId: Long? = null

    private val initialSnapshot = createSnapshotLocked()
    private val _state = MutableStateFlow(initialSnapshot)
    val state: StateFlow<PoplurPerformanceSnapshot> = _state.asStateFlow()

    private val powerSaveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                updatePowerSaveMode(powerManager.isPowerSaveMode)
            }
        }
    }

    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null

    init {
        val processLifecycle = ProcessLifecycleOwner.get().lifecycle
        appInForeground = processLifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        appInteractive = processLifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        processLifecycle.addObserver(this)
        application.registerComponentCallbacks(this)
        ContextCompat.registerReceiver(
            application,
            powerSaveReceiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) registerThermalListenerApi29()
        publishState("Initialized")
    }

    fun registerRenderer(
        renderer: PoplurManagedRenderer,
        heavy: Boolean = true,
    ): RendererRegistration {
        val id = nextRendererId.getAndIncrement()
        synchronized(lock) {
            renderers[id] = RendererRecord(
                id = id,
                renderer = WeakReference(renderer),
                heavy = heavy,
                visible = false,
                visibilityOrder = 0L,
            )
        }
        publishState("Registered ${renderer.performanceRendererName}")
        return RendererRegistration(this, id)
    }

    fun currentSnapshot(): PoplurPerformanceSnapshot = synchronized(lock) {
        createSnapshotLocked()
    }

    /** Debug-only diagnostics suitable for adb/logcat or an internal overlay. */
    fun diagnosticsLine(): String {
        val snapshot = currentSnapshot()
        return buildString {
            append("mode=${snapshot.mode} reason=${snapshot.reason} thermal=${snapshot.thermalCondition}")
            append(" fps=${snapshot.targetFps} foreground=${snapshot.appInForeground}/${snapshot.appInteractive}")
            append(" renderers=${snapshot.activeHeavyRendererCount}/${snapshot.visibleHeavyRendererCount}")
            append(" current=${snapshot.currentRendererName ?: "none"}")
            append(" cache=${snapshot.cacheEstimatedBytes / (1024 * 1024)}MB/")
            append("${snapshot.modelTextureCacheBudgetBytes / (1024 * 1024)}MB(${snapshot.cachedAssetCount})")
            append(" warnings=${snapshot.memoryWarningCount}")
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        appInForeground = true
        if (memoryPressure != PoplurMemoryPressure.NONE) memoryPressure = PoplurMemoryPressure.NONE
        publishState("Process foreground")
    }

    override fun onResume(owner: LifecycleOwner) {
        appInForeground = true
        appInteractive = true
        publishState("Process interactive")
    }

    override fun onPause(owner: LifecycleOwner) {
        appInteractive = false
        publishState("Process paused")
    }

    override fun onStop(owner: LifecycleOwner) {
        appInteractive = false
        appInForeground = false
        assetCache.purgeUnused()
        publishState("Process background")
    }

    override fun onConfigurationChanged(newConfig: Configuration) = Unit

    override fun onLowMemory() {
        updateMemoryPressure(PoplurMemoryPressure.CRITICAL, "onLowMemory")
    }

    override fun onTrimMemory(level: Int) {
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE ->
                updateMemoryPressure(PoplurMemoryPressure.CRITICAL, "trim complete")
            level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE ->
                updateMemoryPressure(PoplurMemoryPressure.CRITICAL, "trim moderate")
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ->
                updateMemoryPressure(PoplurMemoryPressure.MODERATE, "trim background")
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                assetCache.purgeUnused()
                publishState("UI hidden")
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ->
                updateMemoryPressure(PoplurMemoryPressure.CRITICAL, "running critical")
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ->
                updateMemoryPressure(PoplurMemoryPressure.MODERATE, "running low")
        }
    }

    private fun setRendererVisible(id: Long, visible: Boolean) {
        synchronized(lock) {
            val record = renderers[id] ?: return
            if (record.visible == visible) return
            record.visible = visible
            if (visible) record.visibilityOrder = ++visibilityOrder
        }
        publishState(if (visible) "Renderer visible" else "Renderer hidden")
    }

    private fun unregisterRenderer(id: Long) {
        val name = synchronized(lock) {
            val removed = renderers.remove(id) ?: return
            removed.renderer.get()?.performanceRendererName
        }
        publishState("Unregistered ${name ?: "renderer"}")
    }

    private fun updatePowerSaveMode(enabled: Boolean) {
        if (powerSaveMode == enabled) return
        powerSaveMode = enabled
        publishState(if (enabled) "Power saver" else "Power saver ended")
    }

    private fun updateMemoryPressure(pressure: PoplurMemoryPressure, reason: String) {
        memoryWarningCount += 1
        if (pressure.ordinal > memoryPressure.ordinal) memoryPressure = pressure
        assetCache.trimForMemoryPressure(pressure)
        publishState(reason, releaseInactiveResources = pressure == PoplurMemoryPressure.CRITICAL)
    }

    private fun updateThermalCondition(condition: PoplurThermalCondition) {
        if (thermalCondition == condition) return
        thermalCondition = condition
        if (condition >= PoplurThermalCondition.SEVERE) {
            assetCache.trimForMemoryPressure(
                if (condition >= PoplurThermalCondition.CRITICAL) {
                    PoplurMemoryPressure.CRITICAL
                } else {
                    PoplurMemoryPressure.MODERATE
                }
            )
        }
        publishState("Thermal $condition", releaseInactiveResources = condition >= PoplurThermalCondition.CRITICAL)
    }

    private fun publishState(reasonHint: String, releaseInactiveResources: Boolean = false) {
        val notifications: List<Notification>
        val oldSnapshot = _state.value
        val newSnapshot: PoplurPerformanceSnapshot
        synchronized(lock) {
            val iterator = renderers.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().value.renderer.get() == null) iterator.remove()
            }

            activeHeavyRendererId = if (appInForeground && appInteractive) {
                renderers.values
                    .asSequence()
                    .filter { it.heavy && it.visible && it.renderer.get() != null }
                    .maxByOrNull { it.visibilityOrder }
                    ?.id
            } else {
                null
            }
            newSnapshot = createSnapshotLocked()
            notifications = renderers.values.mapNotNull { record ->
                val renderer = record.renderer.get() ?: return@mapNotNull null
                val active = appInForeground && appInteractive && record.visible &&
                    (!record.heavy || record.id == activeHeavyRendererId)
                val changed = record.lastActive != active || oldSnapshot.mode != newSnapshot.mode ||
                    oldSnapshot.targetFps != newSnapshot.targetFps ||
                    oldSnapshot.maximumPreferredTextureResolution != newSnapshot.maximumPreferredTextureResolution
                record.lastActive = active
                if (changed || releaseInactiveResources) {
                    Notification(renderer, active, releaseInactiveResources && !active)
                } else {
                    null
                }
            }
            _state.value = newSnapshot
        }

        dispatchNotifications(notifications, newSnapshot)
        if (oldSnapshot.mode != newSnapshot.mode) {
            log("Mode changed ${oldSnapshot.mode} -> ${newSnapshot.mode}; reason=${newSnapshot.reason}")
        }
        if (oldSnapshot.currentRendererName != newSnapshot.currentRendererName) {
            oldSnapshot.currentRendererName?.let { log("Paused renderer: $it") }
            newSnapshot.currentRendererName?.let { log("Active renderer: $it") }
        }
        if (debug && reasonHint.isNotBlank() && oldSnapshot == newSnapshot) {
            // No frame-level logging: this path is only lifecycle/OS/registration driven.
            Log.v(TAG, "$reasonHint; ${diagnosticsLine()}")
        }
    }

    private fun dispatchNotifications(
        notifications: List<Notification>,
        snapshot: PoplurPerformanceSnapshot,
    ) {
        val deliver = {
            notifications.forEach { notification ->
                runCatching {
                    notification.renderer.onPoplurPerformanceChanged(snapshot, notification.active)
                    if (notification.releaseInactiveResources) {
                        notification.renderer.releaseInactiveResources()
                    }
                }.onFailure { error ->
                    log("Renderer callback failed for ${notification.renderer.performanceRendererName}: ${error.message}")
                }
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) deliver() else mainHandler.post(deliver)
    }

    private fun createSnapshotLocked(): PoplurPerformanceSnapshot {
        val modeAndReason = deriveModeAndReason()
        val mode = modeAndReason.first
        val cache = assetCache.diagnostics()
        val visibleHeavy = renderers.values.count { it.heavy && it.visible && it.renderer.get() != null }
        val activeName = activeHeavyRendererId?.let { renderers[it]?.renderer?.get()?.performanceRendererName }
        val tierResolution = deviceResourceTier.preferredTextureResolution
        val modeResolution = when (mode) {
            PoplurPerformanceMode.FULL -> 2048
            PoplurPerformanceMode.REDUCED -> 2048
            PoplurPerformanceMode.LOW_POWER -> 1024
            PoplurPerformanceMode.EMERGENCY -> 512
        }
        return PoplurPerformanceSnapshot(
            mode = mode,
            reason = modeAndReason.second,
            targetFps = mode.targetFps,
            thermalCondition = thermalCondition,
            memoryPressure = memoryPressure,
            powerSaveMode = powerSaveMode,
            appInForeground = appInForeground,
            appInteractive = appInteractive,
            current3dSceneVisible = visibleHeavy > 0,
            particlesAllowed = mode.particlesAllowed,
            particleDensityMultiplier = mode.particleDensityMultiplier,
            dynamicShadowsAllowed = mode.dynamicShadowsAllowed,
            postProcessingAllowed = mode.postProcessingAllowed,
            physicsQuality = mode.physicsQuality,
            maximumPreferredTextureResolution = minOf(tierResolution, modeResolution),
            modelTextureCacheBudgetBytes = cache.maximumBytes,
            decorativeRenderingAllowed = mode.decorativeRenderingAllowed,
            expensiveVideoTexturesAllowed = mode.expensiveVideoTexturesAllowed,
            activeHeavyRendererCount = if (activeName == null) 0 else 1,
            visibleHeavyRendererCount = visibleHeavy,
            currentRendererName = activeName,
            cachedAssetCount = cache.assetCount,
            cacheEstimatedBytes = cache.estimatedBytes,
            memoryWarningCount = memoryWarningCount,
        )
    }

    private fun deriveModeAndReason(): Pair<PoplurPerformanceMode, String> {
        var mode = if (deviceResourceTier == PoplurDeviceResourceTier.LOW) {
            PoplurPerformanceMode.REDUCED
        } else {
            PoplurPerformanceMode.FULL
        }
        var reason = if (mode == PoplurPerformanceMode.REDUCED) "Low-memory device tier" else "Nominal"

        fun raise(candidate: PoplurPerformanceMode, candidateReason: String) {
            if (candidate.ordinal > mode.ordinal) {
                mode = candidate
                reason = candidateReason
            }
        }

        when (thermalCondition) {
            PoplurThermalCondition.MODERATE -> raise(PoplurPerformanceMode.REDUCED, "Thermal pressure")
            PoplurThermalCondition.SEVERE -> raise(PoplurPerformanceMode.LOW_POWER, "Severe thermal pressure")
            PoplurThermalCondition.CRITICAL,
            PoplurThermalCondition.EMERGENCY,
            PoplurThermalCondition.SHUTDOWN -> raise(PoplurPerformanceMode.EMERGENCY, "Critical thermal pressure")
            else -> Unit
        }
        if (powerSaveMode) raise(PoplurPerformanceMode.LOW_POWER, "System power saver")
        when (memoryPressure) {
            PoplurMemoryPressure.MODERATE -> raise(PoplurPerformanceMode.REDUCED, "Memory pressure")
            PoplurMemoryPressure.CRITICAL -> raise(PoplurPerformanceMode.EMERGENCY, "Critical memory pressure")
            PoplurMemoryPressure.NONE -> Unit
        }
        return mode to reason
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun registerThermalListenerApi29() {
        updateThermalCondition(mapThermalStatus(powerManager.currentThermalStatus))
        val listener = PowerManager.OnThermalStatusChangedListener { status ->
            updateThermalCondition(mapThermalStatus(status))
        }
        thermalListener = listener
        powerManager.addThermalStatusListener(application.mainExecutor, listener)
    }

    private fun log(message: String) {
        if (debug) Log.d(TAG, message)
    }

    companion object {
        private const val TAG = "PoplurPerformance"

        @Volatile private var instance: PoplurPerformanceManager? = null

        @JvmStatic
        fun getInstance(context: Context): PoplurPerformanceManager {
            return instance ?: synchronized(this) {
                instance ?: PoplurPerformanceManager(context.applicationContext as Application).also {
                    instance = it
                }
            }
        }

        /** For render callbacks whose third-party Application API does not expose Android Context. */
        @JvmStatic
        fun getInitializedInstance(): PoplurPerformanceManager? = instance

        private fun detectResourceTier(context: Context): PoplurDeviceResourceTier {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            val totalGiB = memoryInfo.totalMem.toDouble() / (1024.0 * 1024.0 * 1024.0)
            return when {
                activityManager.isLowRamDevice || totalGiB < 4.5 -> PoplurDeviceResourceTier.LOW
                totalGiB >= 8.0 -> PoplurDeviceResourceTier.HIGH
                else -> PoplurDeviceResourceTier.MID
            }
        }

        private fun mapThermalStatus(status: Int): PoplurThermalCondition = when (status) {
            PowerManager.THERMAL_STATUS_NONE -> PoplurThermalCondition.NOMINAL
            PowerManager.THERMAL_STATUS_LIGHT -> PoplurThermalCondition.LIGHT
            PowerManager.THERMAL_STATUS_MODERATE -> PoplurThermalCondition.MODERATE
            PowerManager.THERMAL_STATUS_SEVERE -> PoplurThermalCondition.SEVERE
            PowerManager.THERMAL_STATUS_CRITICAL -> PoplurThermalCondition.CRITICAL
            PowerManager.THERMAL_STATUS_EMERGENCY -> PoplurThermalCondition.EMERGENCY
            PowerManager.THERMAL_STATUS_SHUTDOWN -> PoplurThermalCondition.SHUTDOWN
            else -> PoplurThermalCondition.UNKNOWN
        }
    }
}
