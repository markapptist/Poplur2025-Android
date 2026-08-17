package com.apptists.poplur2025

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService

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
 * Receives token rotations from FCM. Registered in AndroidManifest.
 *
 * `onMessageReceived` is deliberately NOT implemented yet — displaying an
 * engagement notification is Phase 4, and there is no sender until Phase 3, so
 * an implementation now would be untestable code guarding an event that cannot
 * occur.
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

    private companion object {
        const val TAG = "PoplurMessaging"
    }
}
