package com.apptists.poplur2025

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

/**
 * Phase 1 of engagement push notifications: get this device a token and keep it
 * on the user's document. Nothing here sends or displays anything — the sender
 * is a Cloud Firestore trigger (Phase 3) and display lands in Phase 4.
 *
 * Tokens are stored as a MAP KEYED BY TOKEN, not an array:
 *
 *   users/{uid}.fcmTokens = { "<token>": { platform: "android", updatedAt: <ts> } }
 *
 * An array would force read-modify-write to prune a dead token, and two devices
 * registering at once would clobber each other. A keyed map lets a single
 * `update` add one token or `FieldValue.delete()` one that FCM has reported
 * unregistered, with no read and no race.
 */
object PoplurMessaging {
    private const val TAG = "PoplurMessaging"

    /**
     * Writes this device's token to the signed-in user's document.
     *
     * Safe to call repeatedly and from anywhere. It resolves the token itself, so
     * callers do not have to care whether `onNewToken` has already fired — which
     * matters because the token usually arrives BEFORE sign-in on a fresh
     * install, and that ordering is exactly how a device ends up registered to
     * nobody.
     */
    fun syncTokenForCurrentUser() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            // Not an error. On a fresh install the token lands before the user
            // signs in; AuthViewModel calls this again once there is a uid.
            Log.i(TAG, "syncTokenForCurrentUser: no signed-in user yet — deferring")
            return
        }
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isNullOrBlank()) {
                    Log.w(TAG, "syncTokenForCurrentUser: FCM returned a blank token")
                    return@addOnSuccessListener
                }
                writeToken(uid, token)
            }
            .addOnFailureListener { error ->
                // Worth a log rather than silence: with no token, every send to
                // this device fails later with nothing pointing back to here.
                Log.w(TAG, "syncTokenForCurrentUser: could not get FCM token", error)
            }
    }

    /** Records one token under the user, merging so other devices survive. */
    fun writeToken(uid: String, token: String) {
        val payload = mapOf(
            "fcmTokens" to mapOf(
                token to mapOf(
                    "platform" to "android",
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
        )
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            // merge, NOT set: the user document is shared with the profile
            // fields (profilePictureURL, profileSelfieVideoURL and the rest), and
            // a plain set would delete them.
            .set(payload, SetOptions.merge())
            .addOnSuccessListener {
                Log.i(TAG, "writeToken: registered ${token.take(12)}… for $uid")
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "writeToken: failed for $uid", error)
            }
    }
}

/**
 * Receives token rotations and engagement notifications from FCM. Registered in
 * AndroidManifest.
 */
class PoplurMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // FCM rotates tokens on reinstall, restore-to-new-device and data clear.
        // Every rotation invalidates the stored one, so this must write through
        // rather than assume the first registration is permanent.
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.i(TAG, "onNewToken: no signed-in user — will sync after sign-in")
            return
        }
        PoplurMessaging.writeToken(uid, token)
    }

    /**
     * Displays an engagement notification.
     *
     * Only reached when the app is in the FOREGROUND, or for a data-only
     * message. A notification-payload message arriving while the app is
     * backgrounded is drawn by the system directly and never comes through
     * here — which is why the Cloud Function also sets `channelId` and `tag` in
     * its android block: those must be right on the path this code cannot touch.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val kind = data["kind"].orEmpty()
        val postId = data["postId"].orEmpty()
        val threadId = data["threadId"].orEmpty()
        val title = message.notification?.title ?: data["title"].orEmpty()
        val body = message.notification?.body ?: data["body"].orEmpty()
        if (title.isBlank() && body.isBlank()) return

        val notificationId = data["notificationId"].orEmpty()
        val actorHandle = data["actorHandle"].orEmpty()
        if (kind == "dm") {
            if (!PoplurNotifications.messagesPushEnabled(applicationContext)) return
            if (PoplurNotifications.appInForeground) {
                // On screen: the web layer draws the banner (Instagram-style,
                // tap opens the thread). The system banner over the app is
                // exactly the thing this path used to produce.
                deliverInAppBanner(
                    id = notificationId.ifEmpty { data["messageId"].orEmpty().ifEmpty { threadId } },
                    kind = kind, title = title, body = body,
                    actorHandle = actorHandle, postId = "", threadId = threadId,
                )
                return
            }
            val id = if (threadId.isNotEmpty()) threadId.hashCode() else message.messageId.hashCode()
            PoplurNotifications.post(
                applicationContext,
                id,
                title,
                body,
                destinationThreadId = threadId,
            )
            Log.i(TAG, "onMessageReceived: posted DM notification for thread=$threadId")
            return
        }

        // One notification per POST rather than per engagement: id is derived
        // from postId so a second like on the same post replaces the first
        // instead of stacking a fresh banner for every tap of applause. The
        // Cloud Function's coalescing already limits how often this fires; this
        // is the belt to that braces.
        if (PoplurNotifications.appInForeground) {
            val drawn = deliverInAppBanner(
                id = notificationId.ifEmpty { data["engagementId"].orEmpty().ifEmpty { postId } },
                kind = kind, title = title, body = body,
                actorHandle = actorHandle, postId = postId, threadId = "",
            )
            if (drawn) {
                Log.i(TAG, "onMessageReceived: drew in-app banner for post=$postId")
                return
            }
        }
        val id = if (postId.isNotEmpty()) postId.hashCode() else message.messageId.hashCode()
        PoplurNotifications.post(
            applicationContext,
            id,
            title,
            body,
            destinationPostId = postId.ifEmpty { null },
        )
        Log.i(TAG, "onMessageReceived: posted engagement notification for post=$postId")
    }

    /** The push as the web inbox row shape, so the banner and the Notifications
     *  screen describe one event one way. False when no web view can draw it. */
    private fun deliverInAppBanner(
        id: String,
        kind: String,
        title: String,
        body: String,
        actorHandle: String,
        postId: String,
        threadId: String,
    ): Boolean {
        val sink = PoplurNotifications.foregroundBannerSink ?: return false
        val row = JSONObject()
            .put("id", id.ifEmpty { "push-${System.currentTimeMillis()}" })
            .put("kind", kind)
            .put("title", title)
            .put("detail", body)
            .put("createdAtMillis", System.currentTimeMillis())
            .put("unread", true)
        if (actorHandle.isNotEmpty()) row.put("actorHandle", actorHandle)
        if (postId.isNotEmpty()) row.put("postId", postId)
        if (threadId.isNotEmpty()) row.put("threadId", threadId)
        return runCatching { sink(row) }.getOrDefault(false)
    }

    private companion object {
        const val TAG = "PoplurMessaging"
    }
}
