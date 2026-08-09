package com.apptists.poplur2025

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.util.Size
import android.webkit.WebView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread

/**
 * Real device photo library for the web camera roll (mirrors iOS
 * WebCameraRollProvider): the web posts requestCameraRoll, this requests the
 * OS media permission and pushes the most recent photo thumbnails into the page
 * as data URIs via window.poplurCameraRoll([...]); denial pushes {denied:true}.
 */
object PoplurWebCameraRoll {
    const val REQUEST_CODE = 7402
    private const val ITEM_LIMIT = 24
    private const val THUMB_SIDE = 240

    private fun permissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        )
        Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun hasPermission(context: Context): Boolean =
        permissions().any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    fun requestPermission(activity: Activity) {
        ActivityCompat.requestPermissions(activity, permissions(), REQUEST_CODE)
    }

    fun deliverDenied(webView: WebView) {
        webView.evaluateJavascript(
            "window.poplurCameraRoll && window.poplurCameraRoll({denied:true})", null
        )
    }

    fun push(
        webView: WebView,
        registerFullResolutionSource: (uri: android.net.Uri, mimeType: String) -> String
    ) {
        val context = webView.context.applicationContext
        thread(name = "PoplurWebCameraRoll") {
            val items = JSONArray()
            try {
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.MIME_TYPE,
                    MediaStore.Images.Media.WIDTH,
                    MediaStore.Images.Media.HEIGHT
                )
                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${MediaStore.Images.Media.DATE_ADDED} DESC"
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val mimeColumn = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                    val widthColumn = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
                    val heightColumn = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                    while (cursor.moveToNext() && items.length() < ITEM_LIMIT) {
                        val uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            cursor.getLong(idColumn)
                        )
                        val mimeType = mimeColumn.takeIf { it >= 0 }
                            ?.let(cursor::getString)
                            ?.takeIf { it.startsWith("image/") }
                            ?: "image/jpeg"
                        val bitmap: Bitmap = try {
                            context.contentResolver.loadThumbnail(uri, Size(THUMB_SIDE, THUMB_SIDE), null)
                        } catch (_: Exception) { continue }
                        val out = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
                        val thumbnail = "data:image/jpeg;base64," +
                            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                        items.put(JSONObject().apply {
                            put("kind", "photo")
                            // Keep the grid cheap and backwards-compatible.
                            // The shared composer consumes fullSrc only after a
                            // tile is selected, so the original asset (4K when
                            // available) is never replaced by this thumbnail.
                            put("src", thumbnail)
                            put("fullSrc", registerFullResolutionSource(uri, mimeType))
                            if (widthColumn >= 0) put("width", cursor.getInt(widthColumn))
                            if (heightColumn >= 0) put("height", cursor.getInt(heightColumn))
                        })
                    }
                }
            } catch (_: Exception) { /* deliver whatever decoded */ }

            webView.post {
                webView.evaluateJavascript(
                    "window.poplurCameraRoll && window.poplurCameraRoll($items)", null
                )
            }
        }
    }
}
