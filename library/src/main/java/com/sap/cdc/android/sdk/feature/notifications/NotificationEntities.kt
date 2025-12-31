package com.sap.cdc.android.sdk.feature.notifications

import android.R
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.concurrent.TimeUnit
import kotlin.jvm.java

/**
 * Notification customization class.
 */
data class CIAMNotificationOptions(
    val contentView: Class<*>? = null, // Class used for setContent
    val actionsReceiver: Class<*>? = CIAMNotificationReceiver::class.java,
    val smallIcon: Int? = R.drawable.ic_dialog_info,
    val backgroundColor: Int? = null, // argb color
    val autoCancel: Boolean? = true,
    val timeout: Long? = TimeUnit.SECONDS.toMillis(5),
    val channelTitle: String? = "CIAM Authorization channel. Used for applying an additional " +
            "authentication security layer for your application",
    val actionPositive: CIAMNotificationAction? = CIAMNotificationAction(title = "Approve"),
    val actionNegative: CIAMNotificationAction? = CIAMNotificationAction(title = "Deny"),
    val notificationVerified: CIAMNotificationAcknowledged? = CIAMNotificationAcknowledged(title = "Verified"),
    val notificationUnverified: CIAMNotificationAcknowledged? = CIAMNotificationAcknowledged(title = "Unverified"),
)

data class CIAMNotificationAction(
    val icon: Int? = null,
    val title: String? = "",
)

data class CIAMNotificationAcknowledged(
    val icon: Int? = null,
    val title: String? = "",
    val body: String? = ""
)

@Parcelize
data class CIAMNotificationActionData(
    val mode: String,
    val gigyaAssertion: String? = null,
    val verificationToken: String? = null,
    val vToken: String? = null,
    val notificationId: Int
) : Parcelable
