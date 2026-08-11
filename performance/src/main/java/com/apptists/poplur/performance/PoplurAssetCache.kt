package com.apptists.poplur.performance

import java.util.LinkedHashMap

/**
 * A deterministic, byte-accounted LRU for app-owned assets.
 *
 * This is deliberately an asset cache budget, not an operating-system process RAM cap. Every
 * insertion is either brought under both byte and count limits by immediate LRU eviction or is
 * refused. Callers provide the estimated retained cost because Android cannot infer GPU/model
 * allocation sizes reliably.
 */
class PoplurAssetCache(
    val maximumBytes: Long,
    val maximumEntries: Int,
    private val typeLimits: Map<AssetType, Int> = defaultTypeLimits(maximumEntries),
    private val debugLogger: ((String) -> Unit)? = null,
) {
    enum class AssetType {
        MODEL,
        TEXTURE,
        SCENE,
        DECODED_IMAGE,
        TEMPORARY,
        VIDEO_RESOURCE,
    }

    data class Diagnostics(
        val estimatedBytes: Long,
        val maximumBytes: Long,
        val assetCount: Int,
        val maximumEntries: Int,
        val countsByType: Map<AssetType, Int>,
        val rejectedInsertions: Long,
        val evictions: Long,
    )

    private data class Entry(
        val key: String,
        val type: AssetType,
        val value: Any,
        val estimatedBytes: Long,
        val release: (Any) -> Unit,
        var pinned: Boolean,
    )

    private val lock = Any()
    private val entries = LinkedHashMap<String, Entry>(16, 0.75f, true)
    private var estimatedBytes = 0L
    private var rejectedInsertions = 0L
    private var evictions = 0L

    init {
        require(maximumBytes > 0L) { "maximumBytes must be positive" }
        require(maximumEntries > 0) { "maximumEntries must be positive" }
        require(typeLimits.values.all { it > 0 }) { "Per-type limits must be positive" }
    }

    fun <T : Any> put(
        key: String,
        type: AssetType,
        value: T,
        estimatedByteCost: Long,
        pinned: Boolean = false,
        release: (T) -> Unit = { resource ->
            if (resource is AutoCloseable) runCatching { resource.close() }
        },
    ): Boolean {
        require(key.isNotBlank()) { "Cache key must not be blank" }
        require(estimatedByteCost >= 0L) { "estimatedByteCost must not be negative" }

        val releases = mutableListOf<Entry>()
        var rejectedValue = false
        val accepted = synchronized(lock) {
            if (estimatedByteCost > maximumBytes) {
                rejectedInsertions += 1
                rejectedValue = true
                false
            } else {
                val previous = entries[key]
                if (previous?.pinned == true && previous.value !== value) {
                    rejectedInsertions += 1
                    rejectedValue = true
                    false
                } else {
                    if (previous != null) {
                        entries.remove(key)
                        estimatedBytes -= previous.estimatedBytes
                    }

                    val typeLimit = typeLimits[type] ?: maximumEntries
                    while (countOfTypeLocked(type) >= typeLimit) {
                        val evicted = removeOldestLocked(type = type, includePinned = false) ?: break
                        releases += evicted
                    }
                    while (entries.size >= maximumEntries || estimatedBytes + estimatedByteCost > maximumBytes) {
                        val evicted = removeOldestLocked(type = null, includePinned = false) ?: break
                        releases += evicted
                    }

                    val fits = countOfTypeLocked(type) < typeLimit &&
                        entries.size < maximumEntries &&
                        estimatedBytes + estimatedByteCost <= maximumBytes
                    if (fits) {
                        @Suppress("UNCHECKED_CAST")
                        entries[key] = Entry(
                            key = key,
                            type = type,
                            value = value,
                            estimatedBytes = estimatedByteCost,
                            release = release as (Any) -> Unit,
                            pinned = pinned,
                        )
                        estimatedBytes += estimatedByteCost
                        if (previous != null && previous.value !== value) releases += previous
                        true
                    } else {
                        if (previous != null) {
                            entries[key] = previous
                            estimatedBytes += previous.estimatedBytes
                        }
                        rejectedInsertions += 1
                        rejectedValue = previous?.value !== value
                        false
                    }
                }
            }
        }
        releaseEntries(releases)
        if (rejectedValue) runCatching { release(value) }
        if (!accepted) debugLogger?.invoke("Refused $type asset '$key' (${estimatedByteCost.toMiB()} MB)")
        return accepted
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: String): T? = synchronized(lock) {
        entries[key]?.value as? T
    }

    fun contains(key: String): Boolean = synchronized(lock) { entries.containsKey(key) }

    fun setPinned(key: String, pinned: Boolean): Boolean = synchronized(lock) {
        val entry = entries[key] ?: return@synchronized false
        entry.pinned = pinned
        true
    }

    fun remove(key: String): Boolean {
        val removed = synchronized(lock) {
            entries.remove(key)?.also { estimatedBytes -= it.estimatedBytes }
        } ?: return false
        releaseEntries(listOf(removed))
        return true
    }

    /** Releases all inactive cached resources immediately. */
    fun purgeUnused(): Long {
        val removed = synchronized(lock) {
            val values = entries.values.filterNot { it.pinned }
            values.forEach { entry ->
                entries.remove(entry.key)
                estimatedBytes -= entry.estimatedBytes
            }
            values
        }
        releaseEntries(removed)
        val bytes = removed.sumOf { it.estimatedBytes }
        if (bytes > 0L) debugLogger?.invoke("Purged ${bytes.toMiB()} MB asset cache")
        return bytes
    }

    /**
     * Moderate pressure evicts temporary/video resources and then half of the remaining inactive
     * LRU. Critical pressure releases every inactive entry. Pinned visible assets remain bounded
     * by the same hard insertion limits and are never destroyed behind a live renderer.
     */
    fun trimForMemoryPressure(pressure: PoplurMemoryPressure): Long {
        if (pressure == PoplurMemoryPressure.NONE) return 0L
        val removed = synchronized(lock) {
            val selected = mutableListOf<Entry>()
            val candidates = entries.values.filterNot { it.pinned }
            val targets = if (pressure == PoplurMemoryPressure.CRITICAL) {
                candidates
            } else {
                val transient = candidates.filter {
                    it.type == AssetType.TEMPORARY || it.type == AssetType.VIDEO_RESOURCE
                }
                val remaining = candidates.filterNot { it in transient }
                transient + remaining.take((remaining.size + 1) / 2)
            }
            targets.forEach { entry ->
                if (entries.remove(entry.key) != null) {
                    estimatedBytes -= entry.estimatedBytes
                    selected += entry
                }
            }
            selected
        }
        releaseEntries(removed)
        val bytes = removed.sumOf { it.estimatedBytes }
        if (bytes > 0L) debugLogger?.invoke("Purged ${bytes.toMiB()} MB for $pressure memory pressure")
        return bytes
    }

    fun diagnostics(): Diagnostics = synchronized(lock) {
        Diagnostics(
            estimatedBytes = estimatedBytes,
            maximumBytes = maximumBytes,
            assetCount = entries.size,
            maximumEntries = maximumEntries,
            countsByType = AssetType.entries.associateWith(::countOfTypeLocked),
            rejectedInsertions = rejectedInsertions,
            evictions = evictions,
        )
    }

    private fun countOfTypeLocked(type: AssetType): Int = entries.values.count { it.type == type }

    private fun removeOldestLocked(type: AssetType?, includePinned: Boolean): Entry? {
        val candidate = entries.values.firstOrNull { entry ->
            (type == null || entry.type == type) && (includePinned || !entry.pinned)
        } ?: return null
        entries.remove(candidate.key)
        estimatedBytes -= candidate.estimatedBytes
        evictions += 1
        return candidate
    }

    private fun releaseEntries(removed: Collection<Entry>) {
        removed.forEach { entry ->
            runCatching { entry.release(entry.value) }
                .onFailure { debugLogger?.invoke("Release failed for '${entry.key}': ${it.message}") }
        }
    }

    companion object {
        private fun defaultTypeLimits(maximumEntries: Int): Map<AssetType, Int> = mapOf(
            AssetType.MODEL to minOf(maximumEntries, 24),
            AssetType.TEXTURE to minOf(maximumEntries, 48),
            AssetType.SCENE to minOf(maximumEntries, 16),
            AssetType.DECODED_IMAGE to minOf(maximumEntries, 48),
            AssetType.TEMPORARY to minOf(maximumEntries, 24),
            AssetType.VIDEO_RESOURCE to minOf(maximumEntries, 4),
        )
    }
}

private fun Long.toMiB(): Long = this / (1024L * 1024L)

