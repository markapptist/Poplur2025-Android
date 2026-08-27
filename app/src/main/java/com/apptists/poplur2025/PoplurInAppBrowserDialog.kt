package com.apptists.poplur2025

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.net.URI
import kotlin.math.roundToInt

/** The Android half of Poplur Browser's shared HTTPS-only boundary. */
internal fun normalizePoplurInAppBrowserUrl(raw: String): String? {
    val value = raw.trim()
    if (value.isEmpty() || value.length > 2_048) return null
    val uri = runCatching { URI(value) }.getOrNull() ?: return null
    if (!uri.scheme.equals("https", ignoreCase = true)
        || uri.host.isNullOrBlank()
        || uri.rawUserInfo != null
    ) return null
    return uri.toASCIIString()
}

internal enum class PoplurBrowserPrivacyMode { IsolatedProfile, StatelessFallback }

internal fun poplurBrowserPrivacyMode(multiProfileSupported: Boolean): PoplurBrowserPrivacyMode =
    if (multiProfileSupported) PoplurBrowserPrivacyMode.IsolatedProfile
    else PoplurBrowserPrivacyMode.StatelessFallback

/**
 * A Poplur-owned, full-screen web surface for ad and source-link destinations.
 *
 * This WebView is intentionally separate from the signed-in Home WebView. It
 * receives no JavaScript interface, no asset-loader access, and no Poplur
 * message bridge. Web links, redirects, and target=_blank navigation stay in
 * this one controller; Done or Back returns directly to Poplur.
 */
internal class PoplurInAppBrowserDialog(
    private val activity: ComponentActivity,
    private val destination: String,
) : Dialog(activity, android.R.style.Theme_DeviceDefault_NoActionBar) {
    private val chromeColor = Color.rgb(26, 12, 23)
    private val accentColor = Color.rgb(255, 72, 154)
    private val textColor = Color.rgb(255, 240, 248)
    private lateinit var browser: WebView
    private lateinit var hostLabel: TextView
    private lateinit var progress: ProgressBar
    private var externalProfile: Profile? = null
    private var fallbackCookieAcceptance: Boolean? = null
    private var browserDisposed = false

    private fun Int.dp(): Int = (this * activity.resources.displayMetrics.density).roundToInt()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setBackgroundDrawable(ColorDrawable(chromeColor))

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(chromeColor)
        }
        val chrome = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10.dp(), 0, 14.dp(), 0)
            setBackgroundColor(chromeColor)
        }

        val done = Button(activity).apply {
            text = "DONE"
            setTextColor(accentColor)
            setBackgroundColor(Color.TRANSPARENT)
            contentDescription = "Close browser"
            setOnClickListener { dismiss() }
        }
        val back = Button(activity).apply {
            text = "‹"
            textSize = 30f
            setTextColor(textColor)
            setBackgroundColor(Color.TRANSPARENT)
            contentDescription = "Back"
            setOnClickListener { navigateBackOrClose() }
        }
        val lock = TextView(activity).apply {
            text = "●"
            textSize = 10f
            setTextColor(accentColor)
            setPadding(4.dp(), 0, 6.dp(), 0)
            contentDescription = "Secure connection"
        }
        hostLabel = TextView(activity).apply {
            text = displayHost(destination)
            textSize = 14f
            setTextColor(textColor)
            maxLines = 1
        }
        val badge = TextView(activity).apply {
            text = "IN POPLUR"
            textSize = 10f
            setTextColor(accentColor)
            gravity = Gravity.END
        }

        chrome.addView(done, LinearLayout.LayoutParams(82.dp(), 52.dp()))
        chrome.addView(back, LinearLayout.LayoutParams(48.dp(), 52.dp()))
        chrome.addView(lock, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 52.dp()))
        chrome.addView(hostLabel, LinearLayout.LayoutParams(0, 52.dp(), 1f))
        chrome.addView(badge, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 52.dp()))

        progress = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = android.content.res.ColorStateList.valueOf(accentColor)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
        }

        browser = WebView(activity).apply {
            configurePrivateProfile(this)
            setBackgroundColor(chromeColor)
            settings.javaScriptEnabled = true
            // External state belongs to this visit, not Poplur's signed-in
            // WebView profile. Multi-profile WebView providers get a dedicated
            // profile below; this also keeps the compatibility path stateless.
            settings.domStorageEnabled = false
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            // The default false value makes target=_blank/window.open reuse
            // this WebView rather than creating another browser surface.
            settings.setSupportMultipleWindows(false)
            settings.mediaPlaybackRequiresUserGesture = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) settings.safeBrowsingEnabled = true
            settings.setGeolocationEnabled(false)
            // Deliberately no addJavascriptInterface: external pages never get
            // the native authority attached to the signed-in Home WebView.
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean = normalizePoplurInAppBrowserUrl(request.url.toString()) == null

                override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    hostLabel.text = displayHost(url)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    this@PoplurInAppBrowserDialog.progress.progress = newProgress
                    this@PoplurInAppBrowserDialog.progress.visibility =
                        if (newProgress >= 100) android.view.View.INVISIBLE else android.view.View.VISIBLE
                }

                override fun onPermissionRequest(request: PermissionRequest) {
                    request.deny()
                }

                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: GeolocationPermissions.Callback,
                ) {
                    callback.invoke(origin, false, false)
                }
            }
        }

        root.addView(chrome, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 52.dp()))
        root.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2.dp()))
        root.addView(browser, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        ))

        setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                navigateBackOrClose()
                true
            } else false
        }
        browser.loadUrl(destination, NO_STORE_HEADERS)
    }

    override fun show() {
        super.show()
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun dismiss() {
        if (::browser.isInitialized && !browserDisposed) {
            browserDisposed = true
            val profileName = externalProfile?.name
            externalProfile?.cookieManager?.removeAllCookies(null)
            externalProfile?.webStorage?.deleteAllData()
            browser.stopLoading()
            browser.loadUrl("about:blank")
            browser.clearHistory()
            browser.removeAllViews()
            browser.destroy()
            externalProfile = null
            if (profileName != null) {
                runCatching { ProfileStore.getInstance().deleteProfile(profileName) }
            }
            fallbackCookieAcceptance?.let { accepted ->
                CookieManager.getInstance().setAcceptCookie(accepted)
            }
            fallbackCookieAcceptance = null
        }
        super.dismiss()
    }

    /** Park only the isolated external page when its host Activity is hidden. */
    fun hostOnPause() {
        if (::browser.isInitialized && !browserDisposed) browser.onPause()
    }

    /** Resume only the isolated external page when Poplur returns foreground. */
    fun hostOnResume() {
        if (::browser.isInitialized && !browserDisposed) browser.onResume()
    }

    private fun navigateBackOrClose() {
        if (::browser.isInitialized && !browserDisposed && browser.canGoBack()) browser.goBack()
        else dismiss()
    }

    /**
     * Prefer Chromium's real per-profile isolation. Older providers do not
     * expose MULTI_PROFILE, so their scoped fallback rejects all new cookies
     * while Home is parked and keeps every JS storage/cache API disabled.
     * It never clears the default profile that owns signed-in Poplur.
     */
    private fun configurePrivateProfile(webView: WebView) {
        val privacyMode = poplurBrowserPrivacyMode(
            WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE),
        )
        if (privacyMode == PoplurBrowserPrivacyMode.IsolatedProfile) {
            val configured = runCatching {
                val store = ProfileStore.getInstance()
                store.deleteProfile(EXTERNAL_PROFILE_NAME)
                WebViewCompat.setProfile(webView, EXTERNAL_PROFILE_NAME)
                externalProfile = WebViewCompat.getProfile(webView)
                externalProfile?.cookieManager?.removeAllCookies(null)
                externalProfile?.cookieManager?.setAcceptThirdPartyCookies(webView, false)
                externalProfile?.webStorage?.deleteAllData()
                true
            }.getOrDefault(false)
            if (configured) return
        }

        val cookies = CookieManager.getInstance()
        fallbackCookieAcceptance = cookies.acceptCookie()
        cookies.setAcceptCookie(false)
        cookies.setAcceptThirdPartyCookies(webView, false)
    }

    private fun displayHost(raw: String?): String {
        val host = runCatching { Uri.parse(raw).host }.getOrNull().orEmpty()
        return host.removePrefix("www.")
    }

    private companion object {
        const val EXTERNAL_PROFILE_NAME = "poplur-external-browser"
        val NO_STORE_HEADERS = mapOf(
            "Cache-Control" to "no-store, no-cache, max-age=0",
            "Pragma" to "no-cache",
        )
    }
}
