package com.sap.cdc.android.sdk.feature.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.sap.cdc.android.sdk.CIAMDebuggable
import com.sap.cdc.android.sdk.events.CIAMEventBusProvider
import com.sap.cdc.android.sdk.events.emitNotificationActionReceived

/**
 * Notification receiver for push authentication flows.
 * Receives actionable notifications and emits the action data.
 */
class CIAMNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val LOG_TAG = "NotificationReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        CIAMDebuggable.log(LOG_TAG, "onReceive: ")

        // Check if broadcast is actionable. return if not.
        if (intent == null) {
            CIAMDebuggable.log(LOG_TAG, "Intent is null.")
            return
        }
        if (intent.extras == null) {
            CIAMDebuggable.log(LOG_TAG, "Intent extras are null.")
            return
        }

        // Broadcast is actionable.
        val actionData: CIAMNotificationActionData? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(
                    CIAMNotificationManager.BUNDLE_ID_ACTION_DATA,
                    CIAMNotificationActionData::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(CIAMNotificationManager.BUNDLE_ID_ACTION_DATA)
            }

        // Cancel the notification.
        val notificationManager = NotificationManagerCompat.from(context!!)
        notificationManager.cancel(actionData?.notificationId ?: 0)

        CIAMDebuggable.log(LOG_TAG, "onReceive: emitting actionData: $actionData")


        // Emit action data.
        CIAMEventBusProvider.getEventBus().emitNotificationActionReceived(
            intent.action!!,
            actionData!!
        )
    }

}
