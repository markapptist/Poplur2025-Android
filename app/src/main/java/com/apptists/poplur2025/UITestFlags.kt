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
    var authBypass: Boolean = BuildConfig.DEBUG
        set(value) { field = value && BuildConfig.DEBUG }

    var hooksEnabled: Boolean = false
        set(value) { field = value && BuildConfig.DEBUG }
}
