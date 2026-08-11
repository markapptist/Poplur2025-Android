package com.apptists.poplur.performance

/** Shared Android policy values. The FPS targets are for decorative rendering. */
enum class PoplurPerformanceMode(
    val targetFps: Int,
    val particlesAllowed: Boolean,
    val particleDensityMultiplier: Float,
    val dynamicShadowsAllowed: Boolean,
    val postProcessingAllowed: Boolean,
    val physicsQuality: PhysicsQuality,
    val decorativeRenderingAllowed: Boolean,
    val expensiveVideoTexturesAllowed: Boolean,
) {
    FULL(
        targetFps = 30,
        particlesAllowed = true,
        particleDensityMultiplier = 1f,
        dynamicShadowsAllowed = true,
        postProcessingAllowed = true,
        physicsQuality = PhysicsQuality.FULL,
        decorativeRenderingAllowed = true,
        expensiveVideoTexturesAllowed = true,
    ),
    REDUCED(
        targetFps = 24,
        particlesAllowed = true,
        particleDensityMultiplier = 0.5f,
        dynamicShadowsAllowed = false,
        postProcessingAllowed = false,
        physicsQuality = PhysicsQuality.REDUCED,
        decorativeRenderingAllowed = true,
        expensiveVideoTexturesAllowed = true,
    ),
    LOW_POWER(
        targetFps = 15,
        particlesAllowed = true,
        particleDensityMultiplier = 0.22f,
        dynamicShadowsAllowed = false,
        postProcessingAllowed = false,
        physicsQuality = PhysicsQuality.LOW,
        decorativeRenderingAllowed = true,
        expensiveVideoTexturesAllowed = false,
    ),
    EMERGENCY(
        targetFps = 10,
        particlesAllowed = false,
        particleDensityMultiplier = 0f,
        dynamicShadowsAllowed = false,
        postProcessingAllowed = false,
        physicsQuality = PhysicsQuality.SUSPENDED,
        decorativeRenderingAllowed = false,
        expensiveVideoTexturesAllowed = false,
    ),
}

enum class PhysicsQuality { FULL, REDUCED, LOW, SUSPENDED }

enum class PoplurThermalCondition {
    UNKNOWN,
    NOMINAL,
    LIGHT,
    MODERATE,
    SEVERE,
    CRITICAL,
    EMERGENCY,
    SHUTDOWN,
}

enum class PoplurMemoryPressure { NONE, MODERATE, CRITICAL }

enum class PoplurDeviceResourceTier(
    val cacheBudgetBytes: Long,
    val preferredTextureResolution: Int,
    val maxCacheEntries: Int,
) {
    LOW(
        cacheBudgetBytes = 150L * 1024L * 1024L,
        preferredTextureResolution = 1024,
        maxCacheEntries = 64,
    ),
    MID(
        cacheBudgetBytes = 300L * 1024L * 1024L,
        preferredTextureResolution = 2048,
        maxCacheEntries = 128,
    ),
    HIGH(
        cacheBudgetBytes = 450L * 1024L * 1024L,
        // 4096 remains an explicit per-asset exception, never the general default.
        preferredTextureResolution = 2048,
        maxCacheEntries = 192,
    ),
}

data class PoplurPerformanceSnapshot(
    val mode: PoplurPerformanceMode,
    val reason: String,
    val targetFps: Int,
    val thermalCondition: PoplurThermalCondition,
    val memoryPressure: PoplurMemoryPressure,
    val powerSaveMode: Boolean,
    val appInForeground: Boolean,
    val appInteractive: Boolean,
    val current3dSceneVisible: Boolean,
    val particlesAllowed: Boolean,
    val particleDensityMultiplier: Float,
    val dynamicShadowsAllowed: Boolean,
    val postProcessingAllowed: Boolean,
    val physicsQuality: PhysicsQuality,
    val maximumPreferredTextureResolution: Int,
    val modelTextureCacheBudgetBytes: Long,
    val decorativeRenderingAllowed: Boolean,
    val expensiveVideoTexturesAllowed: Boolean,
    val activeHeavyRendererCount: Int,
    val visibleHeavyRendererCount: Int,
    val currentRendererName: String?,
    val cachedAssetCount: Int,
    val cacheEstimatedBytes: Long,
    val memoryWarningCount: Int,
) {
    val frameIntervalMillis: Long
        get() = (1_000L / targetFps.coerceAtLeast(1)).coerceAtLeast(1L)

    val frameIntervalNanos: Long
        get() = 1_000_000_000L / targetFps.coerceAtLeast(1)
}

/** Implementations must not be retained strongly by the performance manager. */
interface PoplurManagedRenderer {
    val performanceRendererName: String

    /** [isActive] is false when hidden, backgrounded, or pre-empted by another heavy renderer. */
    fun onPoplurPerformanceChanged(snapshot: PoplurPerformanceSnapshot, isActive: Boolean)

    /** Called only for an inactive renderer during critical memory pressure. */
    fun releaseInactiveResources() = Unit
}

