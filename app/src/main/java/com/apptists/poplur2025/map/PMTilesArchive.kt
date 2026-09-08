package com.apptists.poplur2025.map

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream

private fun InputStream.readBounded(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (output.size() < limit) {
        val count = read(buffer, 0, minOf(buffer.size, limit - output.size()))
        if (count < 0) break
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

/** Reads the same immutable PMTiles v3 archive as the web map. Only visible
 * vector tiles are fetched; the world archive is never downloaded in full. */
internal class PMTilesArchive(private val url: String, private val client: OkHttpClient) {
    data class Tile(val bytes: ByteArray, val gzip: Boolean)
    private data class Entry(val id: Long, val run: Long, val offset: Long, val length: Int)
    private data class Header(val rootOffset: Long, val rootLength: Int, val leafOffset: Long,
                              val tileOffset: Long, val compression: Int, val tileCompression: Int,
                              val minZoom: Int, val maxZoom: Int)
    private var header: Header? = null
    private val directories = object : LinkedHashMap<String, List<Entry>>(16, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Entry>>) = size > 16
    }

    private fun range(offset: Long, length: Int): ByteArray {
        require(offset >= 0 && length in 1..16_777_216)
        val request = Request.Builder().url(url).header("Range", "bytes=$offset-${offset + length - 1}")
            .header("Accept-Encoding", "identity").build()
        client.newCall(request).execute().use { response ->
            if (response.code != 206) throw IOException("PMTiles range returned ${response.code}")
            val expected = "bytes $offset-${offset + length - 1}/"
            if (response.header("Content-Range")?.startsWith(expected) != true) throw IOException("PMTiles range mismatch")
            val bytes = response.body?.byteStream()?.readBounded(length + 1) ?: throw IOException("Empty PMTiles response")
            if (bytes.size != length) throw IOException("Incomplete PMTiles range")
            return bytes
        }
    }

    @Synchronized private fun getHeader(): Header {
        header?.let { return it }
        val bytes = range(0, 127)
        require(String(bytes, 0, 7, Charsets.US_ASCII) == "PMTiles" && bytes[7].toInt() == 3)
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val h = Header(b.getLong(8), b.getLong(16).toInt(), b.getLong(40), b.getLong(56),
            bytes[97].toInt(), bytes[98].toInt(), bytes[100].toInt(), bytes[101].toInt())
        require(bytes[99].toInt() == 1 && h.compression in 1..2 && h.tileCompression in 1..2)
        header = h
        return h
    }

    @Synchronized private fun directory(offset: Long, length: Int, compression: Int): List<Entry> {
        val key = "$offset:$length"
        directories[key]?.let { return it }
        val raw = range(offset, length)
        val data = if (compression == 2) GZIPInputStream(ByteArrayInputStream(raw)).use { it.readBounded(16_777_217) } else raw
        require(data.size <= 16_777_216)
        var cursor = 0
        fun varint(): Long {
            var value = 0L
            for (shift in 0..63 step 7) {
                require(cursor < data.size)
                val next = data[cursor++].toInt() and 255
                value = value or ((next and 127).toLong() shl shift)
                if (next < 128) return value
            }
            throw IOException("Invalid PMTiles varint")
        }
        val count = varint().toInt()
        require(count in 0..1_000_000)
        val ids = LongArray(count); var id = 0L
        for (i in ids.indices) { id += varint(); ids[i] = id }
        val runs = LongArray(count) { varint() }
        val lengths = IntArray(count) { varint().toInt().also { require(it > 0) } }
        val offsets = LongArray(count)
        for (i in offsets.indices) { val n = varint(); offsets[i] = if (n == 0L && i > 0) offsets[i-1] + lengths[i-1] else n - 1 }
        return ids.indices.map { Entry(ids[it], runs[it], offsets[it], lengths[it]) }.also { directories[key] = it }
    }

    fun tile(z: Int, x: Int, y: Int): Tile? {
        val h = getHeader()
        if (z !in h.minZoom..h.maxZoom || x !in 0 until (1 shl z) || y !in 0 until (1 shl z)) return null
        val id = tileId(z, x, y)
        var offset = h.rootOffset; var length = h.rootLength
        repeat(4) {
            val entries = directory(offset, length, h.compression)
            val match = entries.binarySearch { it.id.compareTo(id) }
            val index = if (match >= 0) match else -match - 2
            val entry = entries.getOrNull(index) ?: return null
            if (entry.run == 0L) { offset = h.leafOffset + entry.offset; length = entry.length }
            else if (id - entry.id < entry.run) return Tile(range(h.tileOffset + entry.offset, entry.length), h.tileCompression == 2)
            else return null
        }
        throw IOException("PMTiles directory depth exceeded")
    }

    companion object {
        fun tileId(z: Int, x: Int, y: Int): Long {
            require(z in 0..26 && x in 0 until (1 shl z) && y in 0 until (1 shl z))
            var result = ((1L shl (2*z)) - 1) / 3
            var tx = x; var ty = y; var s = (1 shl z) shr 1
            while (s > 0) {
                val rx = if (tx and s != 0) 1 else 0; val ry = if (ty and s != 0) 1 else 0
                result += s.toLong() * s * ((3 * rx) xor ry)
                if (ry == 0) { if (rx != 0) { tx = s - 1 - tx; ty = s - 1 - ty }; val swap = tx; tx = ty; ty = swap }
                s = s shr 1
            }
            return result
        }
    }
}
