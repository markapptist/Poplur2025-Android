package com.apptists.poplur2025

/**
 * Test-only navigation scaffolding: lets a UI test skip real Firebase sign-in and reveal hidden
 * testTag-marked navigation hooks. Set once from Intent extras in MainActivity.onCreate
 * (`adb shell am start ... -e uiTestAuthBypass true -e uiTestHooks true`).
 *
 * Both flags are hard-locked to false in non-debug builds via the `BuildConfig.DEBUG` guard in
 * their setters, so this entire mechanism is inert in a release build regardless of what Intent
 * extras are supplied.
 */
object UITestFlags {
    // Defaults to false, like hooksEnabled beside it, because MainActivity only
    // assigns this when the Intent actually carries `uiTestAuthBypass` — and a
    // normal launcher launch never does. Initialising it to BuildConfig.DEBUG
    // therefore left the bypass ON in every debug build that was not started by
    // a UI test, which is every build a human runs.
    //
    // The consequence was worse than skipping a login screen. AuthViewModel.init
    // sets _isSignedIn = true without performing a Firebase sign-in, so the app
    // is signed in enough to route past pre-login and signed out enough that
    // FirebaseAuth.currentUser is null. Every Firestore read then bails on its
    // null-user guard: deliverWebProfile logged "no FirebaseAuth.currentUser —
    // nothing sent", so the profile picture and selfie video could never arrive
    // on a debug build, and the same is true of anything else keyed to the
    // signed-in user.
    //
    // Opt in explicitly, exactly as the class comment already documents:
    //   adb shell am start ... -e uiTestAuthBypass true
    // The BuildConfig.DEBUG guard in the setter still makes this inert in
    // release, so nothing here weakens a shipping build.
    var authBypass: Boolean = false
        set(value) { field = value && BuildConfig.DEBUG }

    var hooksEnabled: Boolean = false
        set(value) { field = value && BuildConfig.DEBUG }
}
