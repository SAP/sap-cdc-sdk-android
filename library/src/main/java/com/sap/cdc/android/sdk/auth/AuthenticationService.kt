package com.sap.cdc.android.sdk.auth

import androidx.core.content.edit
import com.sap.cdc.android.sdk.auth.notifications.CDCNotificationManager
import com.sap.cdc.android.sdk.auth.notifications.CDCNotificationOptions
import com.sap.cdc.android.sdk.auth.notifications.IFCMTokenRequest
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
class AuthenticationService(
    val siteConfig: SiteConfig,
) {
    val coreClient: CoreClient = CoreClient(siteConfig)
    val sessionService: SessionService = SessionService(siteConfig)
    private lateinit var notificationManager: CDCNotificationManager

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

    fun resolve(): IAuthResolvers =
        AuthResolvers(coreClient, sessionService)

    fun tfa(): IAuthTFA =
        AuthTFA(coreClient, sessionService)

    /**
     * Update device info in secure storage.
     * Device info is used for various purposes, such as TFA/Auth push registration & Passkey management
     */
    internal fun updateDeviceInfo(deviceInfo: DeviceInfo) {
        val esp = siteConfig.applicationContext?.getEncryptedPreferences(
            CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
        )
        val json = Json {
            encodeDefaults = true
        }
        val deviceInfoJson = json.encodeToString(deviceInfo)
        esp?.edit() { putString(CDC_DEVICE_INFO, deviceInfoJson) }
    }

    /**
     * Registers the device for push authentication handling. TFA & Auth flows.
     */
    fun registerForPushAuthentication(
        fcmTokenRequest: IFCMTokenRequest,
        notificationOptions: CDCNotificationOptions? = CDCNotificationOptions()
    ) = apply {
        notificationManager = CDCNotificationManager(
            authenticationService = this,
            notificationOptions = notificationOptions!!
        )
        fcmTokenRequest.requestFCMToken()
    }

}