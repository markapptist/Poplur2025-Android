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

    private val permission =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
        else Manifest.permission.READ_EXTERNAL_STORAGE

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun requestPermission(activity: Activity) {
        ActivityCompat.requestPermissions(activity, arrayOf(permission), REQUEST_CODE)
    }

    fun deliverDenied(webView: WebView) {
        webView.evaluateJavascript(
            "window.poplurCameraRoll && window.poplurCameraRoll({denied:true})", null
        )
    }

    fun push(webView: WebView) {
        val context = webView.context.applicationContext
        thread(name = "PoplurWebCameraRoll") {
            val uris = mutableListOf<String>()
            try {
                val projection = arrayOf(MediaStore.Images.Media._ID)
                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${MediaStore.Images.Media.DATE_ADDED} DESC"
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    while (cursor.moveToNext() && uris.size < ITEM_LIMIT) {
                        val uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            cursor.getLong(idColumn)
                        )
                        val bitmap: Bitmap = try {
                            context.contentResolver.loadThumbnail(uri, Size(THUMB_SIDE, THUMB_SIDE), null)
                        } catch (_: Exception) { continue }
                        val out = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
                        uris.add(
                            "data:image/jpeg;base64," +
                                Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                        )
                    }
                }
            } catch (_: Exception) { /* deliver whatever decoded */ }

            val json = uris.joinToString(prefix = "[", postfix = "]") {
                """{"kind":"photo","src":"$it"}"""
            }
            webView.post {
                webView.evaluateJavascript(
                    "window.poplurCameraRoll && window.poplurCameraRoll($json)", null
                )
            }
        }
    }
}
