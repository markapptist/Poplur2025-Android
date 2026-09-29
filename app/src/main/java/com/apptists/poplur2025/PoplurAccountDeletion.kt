package com.apptists.poplur2025

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.EditText
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
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
 * The work runs in a process-wide scope, not an activity scope: the server call
 * may take minutes, the person may background Poplur meanwhile, and the outcome
 * must still be applied and reported once an activity is back on screen. Success
 * is reported only after the server answered `deleted: true`; anything else is
 * an explicit retry message, never a success.
 */
object PoplurAccountDeletion {
    private const val TAG = "PoplurAccountDeletion"
    private const val REGION = "us-east1"
    private const val CALL_TIMEOUT_SECONDS = 540L
    private const val RETRY_MESSAGE = "Deletion could not be confirmed. Some data may already have been removed. " +
        "Check your connection and retry Delete account to finish."

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var host: WeakReference<Activity>? = null
    /** One of our dialogs is on screen or queued; further Settings taps are ignored meanwhile. */
    private var presenting = false
    /** Re-authentication or the server call is running. */
    private var inFlight = false
    private var progress: AlertDialog? = null
    private var pendingResult: Pair<String, String>? = null
    /** The deleted session is signed out; the signed-in surface must leave the screen. */
    private var pendingSignedOutRoute = false

    /** Activities that may show deletion dialogs call this from onResume. */
    fun bind(activity: Activity) {
        host = WeakReference(activity)
        if (inFlight) showProgress(activity)
        if (pendingSignedOutRoute && routeToPrelogin(activity)) return
        presentPendingResult(activity)
    }

    /** From onPause: a dialog must not outlive the window that owns it. */
    fun unbind(activity: Activity) {
        if (host?.get() === activity) host = null
        dismissProgress()
    }

    /** The web Settings row was tapped. Ask for the password in a native dialog. */
    fun present(activity: Activity) {
        if (presenting || inFlight || activity.isFinishing || activity.isDestroyed) return
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val email = user.email
        if (email.isNullOrBlank()) {
            showResult("Account deletion", "This account has no email and password to verify, so it cannot be deleted from this screen.")
            return
        }
        presenting = true
        val input = EditText(activity).apply {
            hint = "Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSingleLine = true
            setAutofillHints(View.AUTOFILL_HINT_PASSWORD)
        }
        val margin = (20 * activity.resources.displayMetrics.density).toInt()
        val container = FrameLayout(activity).apply {
            setPadding(margin, 0, margin, 0)
            addView(input, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Delete account?")
            .setMessage("Permanently delete your Poplur account, profile, posts, uploaded media and messages you sent. " +
                "This cannot be undone. Enter your password to confirm.")
            .setView(container)
            .setNegativeButton("Cancel") { _, _ -> presenting = false }
            .setPositiveButton("Delete account") { _, _ ->
                val password = input.text.toString()
                input.text.clear()
                delete(activity, user, email, password)
            }
            .setOnCancelListener { presenting = false }
            .create()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
        input.requestFocus()
    }

    private fun delete(activity: Activity, user: FirebaseUser, email: String, password: String) {
        if (password.isEmpty()) {
            presenting = false
            showResult("Password required", "Enter your password to verify that this is your account.")
            return
        }
        inFlight = true
        showProgress(activity)
        val app = activity.applicationContext
        scope.launch {
            var requestSent = false
            try {
                val auth = FirebaseAuth.getInstance()
                if (auth.currentUser?.uid != user.uid) throw AccountChanged()
                // Block settings/session changes while either authentication
                // request is in flight, including a slow token refresh.
                user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
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
                endDeletedSession(app, user.uid, email)
                pendingResult = "Account deleted" to "The Poplur account you confirmed and its personal data have been deleted."
            } catch (error: Exception) {
                Log.w(TAG, "account deletion did not complete (requestSent=$requestSent)", error)
                pendingResult = "Account deletion" to failureMessage(error, requestSent)
            } finally {
                inFlight = false
                dismissProgress()
            }
            host?.get()?.let(::bind)
        }
    }

    private fun failureMessage(error: Exception, requestSent: Boolean): String = when {
        error is AccountChanged ->
            "The signed-in account changed. Start account deletion again for the account you want to remove."
        error is FirebaseAuthInvalidCredentialsException ->
            "The password was not accepted. Your account has not been deleted. Try again."
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

    /** Returns true when the caller is leaving the screen for the pre-login flow. */
    private fun routeToPrelogin(activity: Activity): Boolean {
        pendingSignedOutRoute = false
        if (activity is NativePreloginActivity || activity.isFinishing) return false
        activity.startActivity(
            Intent(activity, NativePreloginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        activity.finish()
        return true
    }

    private fun showProgress(activity: Activity) {
        if (progress?.isShowing == true || activity.isFinishing || activity.isDestroyed) return
        progress = AlertDialog.Builder(activity)
            .setTitle("Deleting account…")
            .setMessage("Removing your account and personal data. Please keep Poplur open.")
            .setCancelable(false)
            .create()
            .also { it.show() }
    }

    private fun dismissProgress() {
        progress?.let { dialog -> runCatching { if (dialog.isShowing) dialog.dismiss() } }
        progress = null
    }

    private fun showResult(title: String, message: String) {
        presenting = true
        pendingResult = title to message
        host?.get()?.let(::presentPendingResult)
    }

    private fun presentPendingResult(activity: Activity) {
        val result = pendingResult ?: return
        // bind() runs inside onResume, before the lifecycle reports RESUMED; a
        // started activity can already own a dialog.
        val started = (activity as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) ?: true
        if (!started || activity.isFinishing || activity.isDestroyed) return
        pendingResult = null
        presenting = true
        AlertDialog.Builder(activity)
            .setTitle(result.first)
            .setMessage(result.second)
            .setPositiveButton("OK", null)
            .setOnDismissListener { presenting = false }
            .create()
            .show()
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
