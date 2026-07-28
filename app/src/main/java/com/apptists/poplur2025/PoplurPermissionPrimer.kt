package com.apptists.poplur2025

import android.content.Context

/**
 * Records that a user has answered Poplur's custom permission primer. Android
 * permission status alone cannot distinguish a first ask from a prior denial,
 * so this prevents the clay primer from reappearing on every authenticated
 * launch after the user has already made a choice.
 */
object PoplurPermissionPrimer {
    const val LOCATION = "location"
    const val PHOTOS = "photos"

    private const val PREFS = "poplur_permission_primers"

    fun wasHandled(context: Context, permission: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(permission, false)

    fun markHandled(context: Context, permission: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(permission, true)
            .apply()
    }
}
