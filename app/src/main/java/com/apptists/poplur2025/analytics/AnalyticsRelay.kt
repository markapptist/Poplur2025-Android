package com.apptists.poplur2025.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import org.json.JSONObject

/**
 * Native end of the Firebase Analytics bridge.
 *
 * Ownership is split exactly once. The WEB page (poplur-clay-map src/analytics.ts) is the
 * source of `screen_view` and every in-app action event and posts them over
 * `PoplurNative.onEvent` as
 *
 *     { type: "analyticsEvent",        name, params }
 *     { type: "analyticsUserProperty", name, value }
 *
 * This relay re-validates each message against the GA4 limits and hands it to the SDK.
 * Native itself logs only `login` / `sign_up` (AuthViewModel) plus the SDK's automatic
 * lifecycle events; automatic screen reporting is switched off in AndroidManifest.xml so
 * PostLoginWebActivity never emits a second screen_view. Anything the page must not own,
 * the native-owned events and params that could only carry an identifier, content or a
 * location, is dropped here rather than forwarded. Only names and keys are ever logged,
 * never values.
 */
object AnalyticsRelay {
    private const val TAG = "PoplurAnalytics"

    const val TYPE_EVENT = "analyticsEvent"
    const val TYPE_USER_PROPERTY = "analyticsUserProperty"

    private const val MAX_PARAMS = 25
    private const val MAX_NAME_LENGTH = 40
    private const val MAX_STRING_LENGTH = 100
    private const val MAX_USER_PROPERTY_NAME_LENGTH = 24
    private const val MAX_USER_PROPERTY_VALUE_LENGTH = 36

    private val NAME_PATTERN = Regex("^[a-z][a-z0-9_]{0,39}$")
    private val RESERVED_PREFIXES = listOf("firebase_", "google_", "ga_")

    /** Events native owns (or the SDK logs on its own); the page may never send them. */
    private val NATIVE_OWNED_EVENTS = setOf(
        "login", "sign_up", "purchase", "refund", "in_app_purchase",
        "first_open", "session_start", "app_open", "user_engagement",
        "app_update", "app_remove", "app_clear_data", "app_exception", "os_update",
        "notification_receive", "notification_open", "notification_dismiss",
        "notification_foreground", "dynamic_link_first_open", "dynamic_link_app_open",
        "dynamic_link_app_update", "ad_impression", "ad_click", "ad_query", "ad_exposure",
        "ad_reward", "ad_activeview", "adunit_exposure", "error",
    )

    /** GA4 reserved user property names. */
    private val RESERVED_USER_PROPERTIES = setOf(
        "first_open_time", "first_visit_time", "last_deep_link_referrer", "user_id",
        "first_open_after_install", "lifetime_user_engagement", "session_number", "session_id",
    )

    /** Param keys that could only carry an identifier, content or a location. */
    private val FORBIDDEN_PARAM_KEYS = setOf(
        "id", "uid", "user_id", "userid", "post_id", "postid", "thread_id", "message_id",
        "other_id", "client_message_id", "handle", "username", "email", "phone", "name",
        "caption", "text", "message", "body", "url", "media_url", "playback_url", "image",
        "lat", "lng", "lon", "latitude", "longitude", "coords", "location",
    )

    /** Bridge entry: [type] is [TYPE_EVENT] or [TYPE_USER_PROPERTY], [body] the parsed message. */
    fun handle(context: Context, type: String, body: JSONObject) {
        when (type) {
            TYPE_EVENT -> logBridgedEvent(context, body)
            TYPE_USER_PROPERTY -> setBridgedUserProperty(context, body)
            else -> Unit
        }
    }

    // ---- native-owned auth events + identity ----

    fun logLogin(uid: String?) = logNativeAuthEvent(FirebaseAnalytics.Event.LOGIN, uid)

    fun logSignUp(uid: String?) = logNativeAuthEvent(FirebaseAnalytics.Event.SIGN_UP, uid)

    /** Pseudonymous Firebase uid so GA4 can stitch one person across installs; null on sign-out. */
    fun setUserId(uid: String?) {
        val analytics = analytics(null) ?: return
        runCatching { analytics.setUserId(uid) }
            .onFailure { Log.w(TAG, "setUserId failed", it) }
    }

    private fun logNativeAuthEvent(event: String, uid: String?) {
        val analytics = analytics(null) ?: return
        runCatching {
            // Identity first so the auth event itself already carries the user id.
            if (uid != null) analytics.setUserId(uid)
            analytics.logEvent(
                event,
                Bundle().apply { putString(FirebaseAnalytics.Param.METHOD, "password") }
            )
        }.onFailure { Log.w(TAG, "$event failed", it) }
    }

    // ---- bridged messages ----

    private fun logBridgedEvent(context: Context, body: JSONObject) {
        val name = body.optString("name")
        if (!isValidName(name, MAX_NAME_LENGTH)) return reject("event name", name)
        if (name in NATIVE_OWNED_EVENTS) return reject("native-owned event", name)

        val bundle = Bundle()
        val params = body.optJSONObject("params")
        if (params != null) {
            val keys = params.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (bundle.size() >= MAX_PARAMS) {
                    reject("more than $MAX_PARAMS params, rest dropped", name)
                    break
                }
                if (!isValidName(key, MAX_NAME_LENGTH)) {
                    reject("param name", key)
                    continue
                }
                if (key in FORBIDDEN_PARAM_KEYS) {
                    reject("forbidden param", key)
                    continue
                }
                val value: Any = params.opt(key) ?: continue
                if (value === JSONObject.NULL) continue
                when (value) {
                    is Boolean -> bundle.putString(key, if (value) "1" else "0")
                    is Int -> bundle.putLong(key, value.toLong())
                    is Long -> bundle.putLong(key, value)
                    is Number -> bundle.putDouble(key, value.toDouble())
                    is String -> bundle.putString(key, value.take(MAX_STRING_LENGTH))
                    else -> reject("param value type", key) // arrays and objects never cross
                }
            }
        }

        val analytics = analytics(context) ?: return
        runCatching { analytics.logEvent(name, bundle) }
            .onFailure { Log.w(TAG, "logEvent $name failed", it) }
    }

    private fun setBridgedUserProperty(context: Context, body: JSONObject) {
        val name = body.optString("name")
        if (!isValidName(name, MAX_USER_PROPERTY_NAME_LENGTH)) return reject("user property name", name)
        if (name in RESERVED_USER_PROPERTIES) return reject("reserved user property", name)

        val raw = body.opt("value")
        val value: String? = when {
            raw == null || raw === JSONObject.NULL -> null
            raw is Boolean -> if (raw) "1" else "0"
            raw is Number || raw is String -> raw.toString().take(MAX_USER_PROPERTY_VALUE_LENGTH)
            else -> return reject("user property value type", name)
        }

        val analytics = analytics(context) ?: return
        runCatching { analytics.setUserProperty(name, value) }
            .onFailure { Log.w(TAG, "setUserProperty $name failed", it) }
    }

    // ---- helpers ----

    private fun isValidName(name: String, maxLength: Int): Boolean =
        name.length in 1..maxLength &&
            NAME_PATTERN.matches(name) &&
            RESERVED_PREFIXES.none { name.startsWith(it) }

    private fun reject(reason: String, subject: String) {
        Log.w(TAG, "Dropped bridged analytics message ($reason): $subject")
    }

    private fun analytics(context: Context?): FirebaseAnalytics? {
        val appContext = context?.applicationContext
            ?: runCatching { FirebaseApp.getInstance().applicationContext }.getOrNull()
            ?: return null
        return runCatching { FirebaseAnalytics.getInstance(appContext) }
            .onFailure { Log.w(TAG, "FirebaseAnalytics unavailable", it) }
            .getOrNull()
    }
}
