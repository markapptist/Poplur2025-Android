package com.apptists.poplur2025

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.webkit.WebView
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

/** Resolves the advertiser-provided hero image for an in-app commerce card. */
object PoplurProductImage {
    private const val MAX_HTML_BYTES = 400_000L
    private const val MAX_IMAGE_BYTES = 8_000_000L
    private const val MAX_EDGE = 720

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()
    private val executor = Executors.newFixedThreadPool(2)
    private val cache = ConcurrentHashMap<String, String>()
    private val failed = ConcurrentHashMap.newKeySet<String>()
    private val inFlight = ConcurrentHashMap.newKeySet<String>()

    private val metaTag = Regex("""<meta\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val attribute = Regex(
        """([a-zA-Z_:][-a-zA-Z0-9_:.]*)\s*=\s*(?:\"([^\"]*)\"|'([^']*)'|([^\s>]+))"""
    )
    private val preferredKeys = listOf("og:image:secure_url", "og:image", "twitter:image")

    fun handle(urlString: String, webView: WebView) {
        val page = urlString.toHttpUrlOrNull()?.takeIf(::permitted)
        if (page == null) {
            deliver(urlString, null, webView)
            return
        }
        cache[urlString]?.let {
            deliver(urlString, it, webView)
            return
        }
        if (failed.contains(urlString)) {
            deliver(urlString, null, webView)
            return
        }
        if (!inFlight.add(urlString)) return

        executor.execute {
            val image = runCatching { resolve(page) }.getOrNull()
            if (image == null) failed.add(urlString) else cache[urlString] = image
            inFlight.remove(urlString)
            deliver(urlString, image, webView)
        }
    }

    private fun permitted(url: HttpUrl): Boolean =
        url.isHttps && url.host.contains('.')

    private fun resolve(page: HttpUrl): String? {
        val html = request(page, MAX_HTML_BYTES, truncatable = true)
            ?.toString(Charsets.UTF_8) ?: return null
        val imageUrl = imageUrl(html, page) ?: return null
        val bytes = request(imageUrl, MAX_IMAGE_BYTES, truncatable = false) ?: return null
        val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val scaled = downscaled(source)
        if (scaled !== source) source.recycle()
        return try {
            val output = ByteArrayOutputStream()
            if (!scaled.compress(Bitmap.CompressFormat.JPEG, 82, output)) return null
            "data:image/jpeg;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        } finally {
            scaled.recycle()
        }
    }

    /**
     * The body, capped at [limit].
     *
     * NOT `readByteArray(limit)`: that one demands EXACTLY `limit` bytes and
     * throws EOFException on anything shorter, which is very nearly every
     * response we make — an og:image is a couple of hundred KB against an 8MB
     * cap. The cap has to come from buffering up TO it and taking whatever
     * arrived, not from asking for it.
     *
     * [truncatable] is what a body over the cap means. Half an HTML document is
     * still the half we want, because the `<head>` is in the first bytes. Half a
     * JPEG is worse than none: BitmapFactory decodes it to an image that is
     * grey below the cut rather than failing, so the card would show a broken
     * picture instead of falling back to its poster.
     */
    private fun request(url: HttpUrl, limit: Long, truncatable: Boolean): ByteArray? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Poplur/1.0")
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val source = response.body?.source() ?: return null
            // One byte past the cap, so the answer also tells us whether the
            // body ran over it.
            val overran = source.request(limit + 1)
            if (overran && !truncatable) return null
            val buffer = source.buffer
            buffer.readByteArray(minOf(buffer.size, limit))
        }
    }

    private fun imageUrl(html: String, page: HttpUrl): HttpUrl? {
        val candidates = HashMap<String, String>()
        metaTag.findAll(html).forEach { match ->
            val attributes = attribute.findAll(match.value).associate { item ->
                val value = item.groupValues.drop(2).firstOrNull(String::isNotEmpty).orEmpty()
                item.groupValues[1].lowercase(Locale.ROOT) to value
            }
            val key = (attributes["property"] ?: attributes["name"])
                ?.lowercase(Locale.ROOT)
                ?: return@forEach
            val content = attributes["content"]?.trim().orEmpty()
            if (key in preferredKeys && content.isNotEmpty()) candidates.putIfAbsent(key, content)
        }
        preferredKeys.forEach { key ->
            val value = candidates[key] ?: return@forEach
            page.resolve(value)?.takeIf(::permitted)?.let { return it }
        }
        return null
    }

    private fun downscaled(source: Bitmap): Bitmap {
        val longest = max(source.width, source.height)
        if (longest <= MAX_EDGE) return source
        val scale = MAX_EDGE.toFloat() / longest.toFloat()
        val width = (source.width * scale).roundToInt().coerceAtLeast(1)
        val height = (source.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, width, height, true)
    }

    private fun deliver(urlString: String, image: String?, webView: WebView) {
        val payload = JSONObject()
            .put("url", urlString)
            .put("image", image ?: JSONObject.NULL)
            .toString()
        webView.post {
            webView.evaluateJavascript(
                "window.poplurProductImage && window.poplurProductImage($payload); true;",
                null
            )
        }
    }
}
