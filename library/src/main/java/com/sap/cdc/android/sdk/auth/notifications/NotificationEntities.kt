package com.sap.cdc.android.sdk.auth.notifications

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.concurrent.TimeUnit

/**
 * Notification customization class.
 */
data class CDCNotificationOptions(
    val contentView: Class<*>? = null, // Class used for setContent
    val actionsReceiver: Class<*>? = CDCNotificationReceiver::class.java,
    val smallIcon: Int? = android.R.drawable.ic_dialog_info,
    val backgroundColor: Int? = null, // argb color
    val autoCancel: Boolean? = true,
    val timeout: Long? = TimeUnit.SECONDS.toMillis(5),
    val channelTitle: String? = "CDC Authorization channel. Used for applying an additional " +
            "authentication security layer for your application",
    val actionPositive: CDCNotificationAction? = CDCNotificationAction(title = "Approve"),
    val actionNegative: CDCNotificationAction? = CDCNotificationAction(title = "Deny"),
    val notificationVerified: CDCNotificationAcknowledged? = CDCNotificationAcknowledged(title = "Verified"),
    val notificationUnverified: CDCNotificationAcknowledged? = CDCNotificationAcknowledged(title = "Unverified"),
)

data class CDCNotificationAction(
    val icon: Int? = null,
    val title: String? = "",
)

data class CDCNotificationAcknowledged(
    val icon: Int? = null,
    val title: String? = "",
    val body: String? = ""
)

@Parcelize
data class CDCNotificationActionData(
    val mode: String,
    val gigyaAssertion: String,
    val verificationToken: String,
    val notificationId: Int
) : Parcelable
