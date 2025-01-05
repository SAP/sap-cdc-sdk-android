package com.sap.cdc.android.sdk.auth.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.CDCMessageEventBus
import com.sap.cdc.android.sdk.MessageEvent
import com.sap.cdc.android.sdk.auth.AuthenticationService
import com.sap.cdc.android.sdk.auth.DeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import kotlin.math.abs


/**
 * Token request interface.
 * The SDK requires tracking the FCM token in order to receive push notifications.
 * This interface is used with conjunction with the  FirebaseMessaging.getInstance().token API.
 */
interface IFCMTokenRequest {

    fun requestFCMToken()
}

/**
 * CDC Notification Manager.
 * Manages push notifications for CDC authentication flows.
 */
class CDCNotificationManager(
    private val authenticationService: AuthenticationService,
    private val notificationOptions: CDCNotificationOptions,
) {

    companion object {
        const val LOG_TAG = "CDCNotificationManager"

        const val CDC_NOTIFICATIONS_CHANNEL_ID = "CDC_AUTHENTICATION_NOTIFICATIONS"
        const val CDC_NOTIFICATIONS_ACTIONS_REQUEST_CODE = 2020
        const val CDC_NOTIFICATIONS_CONTENT_REQUEST_CODE = 2021

        const val BUNDLE_ID_ACTION_DATA = "cdc_action_data"
    }

    private lateinit var notificationManager: NotificationManagerCompat

    init {
        // Reference context.
        val context = authenticationService.siteConfig.applicationContext
        notificationManager = NotificationManagerCompat.from(context)

        // Create a new CoroutineScope with a Job
        val job = Job()
        val scope = CoroutineScope(Dispatchers.Main + job)

        CDCMessageEventBus.initializeMessageScope(scope)
        CDCMessageEventBus.subscribeToMessageEvents {
            when (it) {
                is MessageEvent.EventWithToken -> onNewToken(it.token)
                is MessageEvent.EventWithRemoteMessageData -> onMessageReceived(it.data)
                is MessageEvent.EventWithRemoteActionData -> onActionReceived(it.action, it.data)
            }
        }

        // Create notification manager.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CDC_NOTIFICATIONS_CHANNEL_ID,
                notificationOptions.channelTitle,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Handle new token.
     * @param token: The new token.
     */
    private fun onNewToken(token: String) {
        // Handle the token
        authenticationService.updateDeviceInfo(DeviceInfo(pushToken = token))
    }

    /**
     * Handle message received.
     * @param data: The data fields of the notification.
     */
    private fun onMessageReceived(data: Map<String, String>) {
        // Handle the message
        val mode = data["mode"] ?: ""
        when (mode) {
            "optin", "verify" -> {
                CDCDebuggable.log(LOG_TAG, "Received actionable notification with mode: $mode")
                notifyActionable(mode, data)
            }

            "cancel" -> {
                CDCDebuggable.log(LOG_TAG, "Received cancel notification.")
                val gigyaAssertion = data["gigyaAssertion"]
                if (gigyaAssertion != null) {
                    cancel(abs(gigyaAssertion.hashCode().toDouble()).toInt())
                }
            }

            "" -> return
        }
    }

    /**
     * Handle actionable notification.
     * @param action: The action of the notification.
     * @param data: The data fields of the notification.
     */
    private fun onActionReceived(action: String, data: CDCNotificationActionData) {
        CDCDebuggable.log(LOG_TAG, "onActionReceived: action: $action, data: $data")

        when (action) {
            "Approve" -> {

                // Create a new CoroutineScope with a Job
                val job = Job()
                val scope = CoroutineScope(Dispatchers.Main + job)

                when (data.mode) {
                    "optin" -> {
                        scope.launch {
                            try {
                                CDCDebuggable.log(LOG_TAG, "Finalizing push TFA.")
                                val authResponse =
                                    authenticationService.tfa().finalizeOtpInForPushAuthentication(
                                        mutableMapOf(
                                            "verificationToken" to data.verificationToken,
                                            "gigyaAssertion" to data.gigyaAssertion
                                        )
                                    )
                                if (authResponse.cdcResponse().isError()) {
                                    CDCDebuggable.log(
                                        LOG_TAG,
                                        "Error finalizing push TFA: ${
                                            authResponse.cdcResponse().errorMessage()
                                        }"
                                    )
                                    return@launch
                                }
                                //Send notification.
                                notify(notificationOptions.notificationVerified?.title!!, "")
                            } finally {
                                CDCDebuggable.log(LOG_TAG, "Finalized push TFA. Canceling job")
                                // Cancel the scope once the coroutine completes
                                job.cancel()
                            }
                        }
                    }

                    "verify" -> {
                        scope.launch {
                            try {
                                CDCDebuggable.log(LOG_TAG, "Verifying push TFA.")
                                val authResponse = authenticationService.tfa().verifyPushTFA(
                                    mutableMapOf(
                                        "verificationToken" to data.verificationToken,
                                        "gigyaAssertion" to data.gigyaAssertion
                                    )
                                )
                                if (authResponse.cdcResponse().isError()) {
                                    CDCDebuggable.log(
                                        LOG_TAG,
                                        "Error verifying push TFA: ${
                                            authResponse.cdcResponse().errorMessage()
                                        }"
                                    )
                                    return@launch
                                }
                                //Send notification.
                                notify(notificationOptions.notificationUnverified?.title!!, "")
                            } finally {
                                CDCDebuggable.log(LOG_TAG, "Verified push TFA. Canceling job")
                                // Cancel the scope once the coroutine completes
                                job.cancel()
                            }
                        }
                    }
                }
            }

            "Deny" -> {
                // Redundant.
            }
        }
    }

    /**
     * Notify notification.
     * @param title: The title of the notification.
     * @param body: The body of the notification.
     */
    private fun notify(title: String, body: String) {
        // Reference context.
        val context = authenticationService.siteConfig.applicationContext

        // Build notification.
        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(context, CDC_NOTIFICATIONS_CHANNEL_ID)
                .setSmallIcon(notificationOptions.smallIcon!!)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(notificationOptions.autoCancel!!)

        // Set a 3 second timeout for the notification display.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setTimeoutAfter(TimeUnit.SECONDS.toMillis(3))
        }

        // Notify.
        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(SecureRandom().nextInt(), builder.build())
        } else {
            CDCDebuggable.log(LOG_TAG, "Notifications permissions not enabled.")
        }
    }

    /**
     * Notify actionable notification.
     * @param mode: The mode of the notification.
     * @param data: The data fields of the notification.
     */
    private fun notifyActionable(mode: String, data: Map<String, String>) {
        CDCDebuggable.log(LOG_TAG, "notifyActionable: mode: $mode, data: $data")

        // Parse data fields from cdc push.
        val title = data["title"]
        val body = data["body"]
        val gigyaAssertion = data["gigyaAssertion"]
        val verificationToken = data["verificationToken"]

        if (gigyaAssertion == null || verificationToken == null) {
            CDCDebuggable.log(LOG_TAG, "Missing gigyaAssertion in notification data.")
            return
        }

        // the unique notification id will be the hash code of the gigyaAssertion field.
        // gigyaAssertion hash will act as notification id.
        val notificationId = abs(gigyaAssertion.hashCode())

        val actionData =
            CDCNotificationActionData(mode, gigyaAssertion, verificationToken, notificationId)
        // Reference context.
        val context = authenticationService.siteConfig.applicationContext

        // Build notification.
        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(context, CDC_NOTIFICATIONS_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title?.trim { it <= ' ' } ?: "")
                .setContentText(body?.trim { it <= ' ' } ?: "")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setTimeoutAfter(notificationOptions.timeout!!)
                .setAutoCancel(true)

        // Notification channel required for Android O and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CDC_NOTIFICATIONS_CHANNEL_ID)
        }

        // Define actions pending intent flags.
        val flags = PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // Activity content view to handle when the notification is clicked.
        if (notificationOptions.contentView != null) {
            // Content activity pending intent.
            val intent = Intent(context, notificationOptions.contentView)
            intent.putExtra(BUNDLE_ID_ACTION_DATA, actionData)

            val pendingIntent = PendingIntent.getActivity(
                context, CDC_NOTIFICATIONS_CONTENT_REQUEST_CODE,
                intent, flags
            )

            // We don't want the annoying enter animation.
            intent.addFlags(
                (Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )

            builder.setContentIntent(pendingIntent)
        }

        // Deny action.
        val denyIntent =
            Intent(context, CDCNotificationReceiver::class.java) // Missing receiver class
        denyIntent.putExtra(BUNDLE_ID_ACTION_DATA, actionData)

        denyIntent.setAction("Deny") // Missing resource string
        val denyPendingIntent =
            PendingIntent.getBroadcast(
                context, CDC_NOTIFICATIONS_ACTIONS_REQUEST_CODE, denyIntent,
                flags
            )


        // Approve action.
        val approveIntent = Intent(
            authenticationService.siteConfig.applicationContext,
            CDCNotificationReceiver::class.java
        ) // Missing receiver class
        approveIntent.putExtra(BUNDLE_ID_ACTION_DATA, actionData)

        approveIntent.setAction("Approve") // Missing resource string
        val approvePendingIntent =
            PendingIntent.getBroadcast(
                context, CDC_NOTIFICATIONS_ACTIONS_REQUEST_CODE, approveIntent,
                flags
            )

        builder
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                notificationOptions.actionNegative?.title ?: "Deny",
                denyPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_save,
                notificationOptions.actionPositive?.title ?: "Approve",
                approvePendingIntent
            )


        // Notify.
        if (notificationManager.areNotificationsEnabled()) {
            CDCDebuggable.log(
                LOG_TAG,
                "Notify actionable notification with id = $notificationId"
            )
            notificationManager.notify(notificationId, builder.build())
        } else {
            CDCDebuggable.log(LOG_TAG, "Notifications permissions not enabled.")
        }
    }

    /**
     * Cancel notification by id.
     * @param idToCancel: The id of the notification to cancel.
     */
    private fun cancel(idToCancel: Int) {
        if (idToCancel == 0) {
            return
        }
        CDCDebuggable.log(
            LOG_TAG,
            "Cancel notification with id = $idToCancel"
        )

        // Reference context.
        val context = authenticationService.siteConfig.applicationContext

        // Cancel notification.
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(idToCancel)
    }
}