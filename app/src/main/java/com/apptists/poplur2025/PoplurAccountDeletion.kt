package com.apptists.poplur2025

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import com.apptists.poplur2025.analytics.AnalyticsRelay
import com.apptists.poplur2025.prelogin.NativePreloginDraft
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

/**
 * In-app account deletion (Google Play "Account deletion" policy).
 *
 * The shipping web Settings row posts `deleteAccount` through the bridge; the
 * password check, the `deleteMyAccount` callable, the sign-out and the final
 * confirmation stay native — the same shape as iOS PoplurAccountDeletion.swift,
 * against the same server function (functions/src/accountDeletion.ts, us-east1).
 *
 * One dialog, PoplurDeletionDialog, carries the whole flow as on iOS: the password
 * step turns into the progress state in place, a refused password returns to the
 * password step with an inline message, and the outcome is shown with one OK.
 *
 * The work runs in a process-wide scope, not an activity scope: the server call
 * may take minutes, the person may background Poplur meanwhile, and the outcome
 * must still be applied and reported once an activity is back on screen. The
 * coordinator owns the dialog's phase, so the card is rebuilt on whichever
 * activity binds next. Success is reported only after the server answered
 * `deleted: true`; anything else is an explicit retry message, never a success.
 */
object PoplurAccountDeletion {
    private const val TAG = "PoplurAccountDeletion"
    private const val REGION = "us-east1"
    private const val CALL_TIMEOUT_SECONDS = 540L
    private const val FAILURE_TITLE = "Account deletion"
    private const val WRONG_PASSWORD = "That password wasn't accepted. Your account has not been deleted. Try again."
    private const val RETRY_MESSAGE = "Deletion could not be confirmed. Some data may already have been removed. " +
        "Check your connection and retry Delete account to finish."

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var host: WeakReference<Activity>? = null
    /** What the dialog shows. Null when no deletion dialog is open or queued. */
    private var phase: DeletionPhase? = null
    /** The account the open password step is about. */
    private var target: Target? = null
    /** The card currently on screen, attached to one activity's window. */
    private var dialog: PoplurDeletionDialog? = null
    /** The deleted session is signed out; the signed-in surface must leave the screen. */
    private var pendingSignedOutRoute = false

    private class Target(val user: FirebaseUser, val email: String)

    /** Activities that may show the deletion dialog call this from onResume. */
    fun bind(activity: Activity) {
        host = WeakReference(activity)
        if (pendingSignedOutRoute && routeToPrelogin(activity)) return
        render(activity)
    }

    /** From onPause: the card must not outlive the window that owns it. Its phase is kept. */
    fun unbind(activity: Activity) {
        if (host?.get() === activity) host = null
        if (dialog?.activity === activity) {
            dialog?.dismiss()
            dialog = null
        }
    }

    /** The web Settings row was tapped. Ask for the password in the native dialog. */
    fun present(activity: Activity) {
        if (phase != null || activity.isFinishing || activity.isDestroyed) return
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val email = user.email
        phase = if (email.isNullOrBlank()) {
            DeletionPhase.Result(FAILURE_TITLE, "This account has no email and password to verify, so it cannot be deleted from this screen.")
        } else {
            target = Target(user, email)
            DeletionPhase.Confirm()
        }
        host = WeakReference(activity)
        render(activity)
    }

    /** Shows the current phase on [activity], reusing the card when it already lives there. */
    private fun render(activity: Activity) {
        val current = phase
        if (current == null) {
            close()
            return
        }
        if (activity.isFinishing || activity.isDestroyed) return
        val open = dialog
        if (open != null && open.activity === activity) {
            open.phase = current
            return
        }
        open?.dismiss()
        dialog = PoplurDeletionDialog(
            activity = activity,
            initial = current,
            onConfirm = ::confirm,
            onCancel = ::close,
            onAcknowledge = ::close,
        ).also { it.show() }
    }

    /** Cancel on the password step, or OK on the outcome. */
    private fun close() {
        phase = null
        target = null
        dialog?.dismiss()
        dialog = null
    }

    private fun update(next: DeletionPhase) {
        phase = next
        host?.get()?.let(::render)
    }

    private fun confirm(password: String) {
        if (phase !is DeletionPhase.Confirm) return
        val account = target ?: return close()
        if (password.isEmpty()) {
            update(DeletionPhase.Confirm("Enter your password to verify that this is your account."))
            return
        }
        val app = (dialog?.activity ?: host?.get())?.applicationContext ?: return
        // The card stays up and turns into its progress state, which blocks
        // settings/session changes while either authentication request is in
        // flight, including a slow token refresh.
        update(DeletionPhase.Working)
        scope.launch {
            var requestSent = false
            val user = account.user
            val outcome = try {
                val auth = FirebaseAuth.getInstance()
                if (auth.currentUser?.uid != user.uid) throw AccountChanged()
                user.reauthenticate(EmailAuthProvider.getCredential(account.email, password)).await()
                if (auth.currentUser?.uid != user.uid) throw AccountChanged()
                user.getIdToken(true).await()
                if (auth.currentUser?.uid != user.uid) throw AccountChanged()
                // The server compares expectedUid with its authenticated caller. It
                // never uses a client-supplied identity as the deletion target.
                requestSent = true
                val result = FirebaseFunctions.getInstance(REGION)
                    .getHttpsCallable("deleteMyAccount")
                    .withTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .call(mapOf("confirm" to true, "expectedUid" to user.uid))
                    .await()
                if ((result.getData() as? Map<*, *>)?.get("deleted") != true) throw Incomplete()
                endDeletedSession(app, user.uid, account.email)
                target = null
                DeletionPhase.Result(DELETION_DONE_TITLE, "The Poplur account you confirmed and its personal data have been deleted.")
            } catch (error: Exception) {
                Log.w(TAG, "account deletion did not complete (requestSent=$requestSent)", error)
                if (error is FirebaseAuthInvalidCredentialsException) {
                    // Nothing reached the server: try again in place, as on iOS.
                    DeletionPhase.Confirm(WRONG_PASSWORD)
                } else {
                    target = null
                    DeletionPhase.Result(FAILURE_TITLE, failureMessage(error, requestSent))
                }
            }
            phase = outcome
            host?.get()?.let(::bind)
        }
    }

    private fun failureMessage(error: Exception, requestSent: Boolean): String = when {
        error is AccountChanged ->
            "The signed-in account changed. Start account deletion again for the account you want to remove."
        error is FirebaseAuthInvalidUserException ->
            "This sign-in is no longer valid. Sign in again, then retry Delete account. No deletion request was sent."
        !requestSent && error is FirebaseNetworkException ->
            "Your password could not be checked. Check your connection and try again. No deletion request was sent."
        !requestSent ->
            "Your password could not be checked. Try again. No deletion request was sent."
        // The server refuses these before touching any data, with a message written for the person.
        error is FirebaseFunctionsException && error.code in REFUSED_BEFORE_DELETING && !error.message.isNullOrBlank() ->
            "${error.message} Your account has not been deleted."
        else -> RETRY_MESSAGE
    }

    /**
     * Only after the server confirmed deletion. A different session that replaced
     * this one while the request ran keeps its credentials and local data.
     */
    private fun endDeletedSession(context: Context, uid: String, email: String) {
        val auth = FirebaseAuth.getInstance()
        val current = auth.currentUser
        if (current != null && current.uid != uid) return
        val biometrics = context.getSharedPreferences("poplur_native_biometrics", Context.MODE_PRIVATE)
        val savedEmail = biometrics.getString("email", null)
        if (savedEmail == null || savedEmail.equals(email, ignoreCase = true)) biometrics.edit().clear().apply()
        NativePreloginDraft.clear(context)
        val onboarding = context.getSharedPreferences("poplur_native_onboarding", Context.MODE_PRIVATE)
        if (onboarding.getString("uid", null) == uid) onboarding.edit().clear().apply()
        File(context.filesDir, "profile.jpg").delete()
        // Clear embedded per-account caches before another person signs in.
        runCatching { WebStorage.getInstance().deleteAllData() }
        runCatching { CookieManager.getInstance().apply { removeAllCookies(null); flush() } }
        AnalyticsRelay.setUserId(null)
        // Ending the deleted session cannot depend on a dialog being visible or
        // acknowledged: this can finish while the device is locked.
        if (auth.currentUser?.uid == uid) auth.signOut()
        pendingSignedOutRoute = true
    }

    /**
     * Returns true when the caller is leaving the screen for the pre-login flow.
     * The outcome card follows on the pre-login screen, which binds on resume.
     */
    private fun routeToPrelogin(activity: Activity): Boolean {
        pendingSignedOutRoute = false
        if (activity is NativePreloginActivity || activity.isFinishing) return false
        if (dialog?.activity === activity) {
            dialog?.dismiss()
            dialog = null
        }
        activity.startActivity(
            Intent(activity, NativePreloginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        activity.finish()
        return true
    }

    private val REFUSED_BEFORE_DELETING = setOf(
        FirebaseFunctionsException.Code.INVALID_ARGUMENT,
        FirebaseFunctionsException.Code.FAILED_PRECONDITION,
        FirebaseFunctionsException.Code.PERMISSION_DENIED,
        FirebaseFunctionsException.Code.UNAUTHENTICATED,
    )

    private class AccountChanged : Exception("The signed-in account changed.")
    private class Incomplete : Exception("The server did not confirm deletion.")
}
