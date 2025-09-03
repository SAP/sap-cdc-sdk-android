package com.sap.cdc.android.sdk.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.events.CDCEventBusProvider
import com.sap.cdc.android.sdk.events.emitNotificationActionReceived

/**
 * Notification receiver for push authentication flows.
 * Receives actionable notifications and emits the action data.
 */
class CDCNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val LOG_TAG = "NotificationReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        CDCDebuggable.log(LOG_TAG, "onReceive: ")

        // Check if broadcast is actionable. return if not.
        if (intent == null) {
            CDCDebuggable.log(LOG_TAG, "Intent is null.")
            return
        }
        if (intent.extras == null) {
            CDCDebuggable.log(LOG_TAG, "Intent extras are null.")
            return
        }

        // Broadcast is actionable.
        val actionData: CDCNotificationActionData? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(
                    CDCNotificationManager.BUNDLE_ID_ACTION_DATA,
                    CDCNotificationActionData::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(CDCNotificationManager.BUNDLE_ID_ACTION_DATA)
            }

        // Cancel the notification.
        val notificationManager = NotificationManagerCompat.from(context!!)
        notificationManager.cancel(actionData?.notificationId ?: 0)

        CDCDebuggable.log(LOG_TAG, "onReceive: emitting actionData: $actionData")


        // Emit action data.
        CDCEventBusProvider.getEventBus().emitNotificationActionReceived(
            intent.action!!,
            actionData!!
        )
    }

}
