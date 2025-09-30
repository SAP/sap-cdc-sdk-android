package com.sap.cdc.android.sdk.feature

import androidx.core.content.edit
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.events.CDCEventBusProvider
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import com.sap.cdc.android.sdk.feature.account.AuthAccount
import com.sap.cdc.android.sdk.feature.account.IAuthAccount
import com.sap.cdc.android.sdk.feature.notifications.CDCNotificationManager
import com.sap.cdc.android.sdk.feature.notifications.CDCNotificationOptions
import com.sap.cdc.android.sdk.feature.notifications.IFCMTokenRequest
import com.sap.cdc.android.sdk.feature.session.AuthSession
import com.sap.cdc.android.sdk.feature.session.IAuthSession
import com.sap.cdc.android.sdk.feature.session.SessionService
import kotlinx.serialization.json.Json

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
class AuthenticationService(
    val siteConfig: SiteConfig,
    private val coreClient: CoreClient = CoreClient(siteConfig),
    private val sessionService: SessionService = SessionService(siteConfig),
    private val notificationManagerFactory: (AuthenticationService, CDCNotificationOptions) -> CDCNotificationManager = ::CDCNotificationManager
) {
    private lateinit var notificationManager: CDCNotificationManager

    init {
        // Initialize the lifecycle-aware event bus when the SDK is first created
        if (!CDCEventBusProvider.isInitialized()) {
            CDCEventBusProvider.initialize()
        }
    }

    companion object {
        const val CDC_AUTHENTICATION_SERVICE_SECURE_PREFS =
            "cdc_secure_prefs_authentication_service"

        const val CDC_GMID = "cdc_gmid"
        const val CDC_GMID_REFRESH_TS = "cdc_gmid_refresh_ts"
        const val CDC_DEVICE_INFO = "cdc_device_info"
    }

    fun authenticate(): IAuthApis =
        AuthApis(coreClient, sessionService)

    fun account(): IAuthAccount =
        AuthAccount(coreClient, sessionService)

    fun session(): IAuthSession = AuthSession(
        sessionService
    )

    /**
     * Update device info in secure storage.
     * Device info is used for various purposes, such as TFA/Auth push registration & Passkey management
     */
    internal fun updateDeviceInfo(deviceInfo: DeviceInfo) {
        val esp = siteConfig.applicationContext.getEncryptedPreferences(
            CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
        )
        val json = Json {
            encodeDefaults = true
        }
        val deviceInfoJson = json.encodeToString(deviceInfo)
        esp.edit() { putString(CDC_DEVICE_INFO, deviceInfoJson) }
    }

    /**
     * Registers the device for push authentication handling. TFA & Auth flows.
     */
    fun registerForPushAuthentication(
        fcmTokenRequest: IFCMTokenRequest,
        notificationOptions: CDCNotificationOptions? = CDCNotificationOptions()
    ) = apply {
        notificationManager = notificationManagerFactory(
            this,
            notificationOptions!!
        )
        fcmTokenRequest.requestFCMToken()
    }

}
