package com.apptists.poplur2025.map

import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Device-local transport for the native SDK, not a development server. The
 * random path and loopback binding keep the tile endpoint private to the app. */
internal class NativeMapTileServer(cacheDirectory: File) : AutoCloseable {
    private val socket = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
    private val nonce = UUID.randomUUID().toString()
    private val acceptor = Executors.newSingleThreadExecutor()
    private val workers = Executors.newFixedThreadPool(4)
    private val client = OkHttpClient.Builder().cache(Cache(File(cacheDirectory, "native-map-ranges"), 48L * 1024 * 1024))
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
    private val archives = ConcurrentHashMap<String, PMTilesArchive>()
    @Volatile private var closed = false

    init {
        acceptor.execute {
            while (!closed) {
                val connection = try { socket.accept() } catch (_: Exception) { break }
                workers.execute {
                    connection.use { peer ->
                        peer.soTimeout = 5000
                        try {
                            val reader = peer.getInputStream().bufferedReader(Charsets.US_ASCII)
                            val request = reader.readLine()?.split(' ') ?: return@use
                            val path = request.getOrNull(1)?.split('/') ?: return@use
                            var headers = 0
                            while (!reader.readLine().isNullOrEmpty()) { if (++headers > 64) return@use }
                            val tile = if (request[0] == "GET" && path.size == 7 && path[1] == nonce) {
                                val archive = archives[path[2]]
                                val z = path[3].toIntOrNull(); val x = path[4].toIntOrNull(); val y = path[5].toIntOrNull()
                                if (archive != null && z != null && x != null && y != null && path[6] == "tile.mvt") archive.tile(z,x,y) else null
                            } else null
                            val body = tile?.bytes ?: ByteArray(0)
                            val code = if (tile == null) "204 No Content" else "200 OK"
                            val gzip = if (tile?.gzip == true) "Content-Encoding: gzip\r\n" else ""
                            peer.getOutputStream().write(("HTTP/1.1 $code\r\nContent-Type: application/vnd.mapbox-vector-tile\r\nContent-Length: ${body.size}\r\n${gzip}Cache-Control: max-age=86400\r\nConnection: close\r\n\r\n").toByteArray(Charsets.US_ASCII))
                            peer.getOutputStream().write(body)
                        } catch (_: Exception) {
                            runCatching { peer.getOutputStream().write("HTTP/1.1 503 Service Unavailable\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray()) }
                        }
                    }
                }
            }
        }
    }

    fun tiles(url: String): String {
        require(url.startsWith("https://") && url.endsWith(".pmtiles"))
        val id = UUID.nameUUIDFromBytes(url.toByteArray()).toString()
        archives.computeIfAbsent(id) { PMTilesArchive(url, client) }
        return "http://127.0.0.1:${socket.localPort}/$nonce/$id/{z}/{x}/{y}/tile.mvt"
    }
    override fun close() {
        closed = true; socket.close(); acceptor.shutdownNow(); workers.shutdownNow()
        client.dispatcher.cancelAll(); client.connectionPool.evictAll(); client.cache?.close(); archives.clear()
    }
}
