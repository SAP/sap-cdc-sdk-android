package com.sap.cdc.bitsnbytes.feature.messaging

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Utility class to manage notification permissions according to Android guidelines.
 * Handles the POST_NOTIFICATIONS permission required for API 33+ (Android 13+).
 */
object NotificationPermissionManager {

    /**
     * Check if notification permission is required for this device.
     * POST_NOTIFICATIONS permission is only required for API 33+ (Android 13+).
     */
    fun isNotificationPermissionRequired(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    /**
     * Check if notification permission is currently granted.
     * For devices below API 33, this always returns true as permission is not required.
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (isNotificationPermissionRequired()) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Permission not required for API < 33
            true
        }
    }

    /**
     * Get the permission string for POST_NOTIFICATIONS.
     * This is used with permission request libraries like Accompanist.
     */
    fun getNotificationPermission(): String {
        return Manifest.permission.POST_NOTIFICATIONS
    }
}