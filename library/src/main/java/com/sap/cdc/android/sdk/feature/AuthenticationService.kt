package com.sap.cdc.android.sdk.feature

import androidx.core.content.edit
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.events.CIAMEventBusProvider
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import com.sap.cdc.android.sdk.feature.account.AuthAccount
import com.sap.cdc.android.sdk.feature.account.IAuthAccount
import com.sap.cdc.android.sdk.feature.notifications.CIAMNotificationManager
import com.sap.cdc.android.sdk.feature.notifications.CIAMNotificationOptions
import com.sap.cdc.android.sdk.feature.notifications.IFCMTokenRequest
import com.sap.cdc.android.sdk.feature.session.AuthSession
import com.sap.cdc.android.sdk.feature.session.IAuthSession
import com.sap.cdc.android.sdk.feature.session.SessionService
import com.sap.cdc.android.sdk.feature.session.validation.SessionValidationConfig
import com.sap.cdc.android.sdk.feature.session.validation.SessionValidationService
import kotlinx.serialization.json.Json

/**
 * Main entry point for SAP Customer Data Cloud SDK operations.
 * Provides access to authentication, account management, and session handling.
 * 
 * ## Usage
 * ```kotlin
 * val siteConfig = SiteConfig(context)
 * val authService = AuthenticationService(siteConfig)
 * 
 * // Authentication operations
 * authService.authenticate().login().credentials(credentials) { /* callbacks */ }
 * 
 * // Account operations
 * authService.account().get() { /* callbacks */ }
 * 
 * // Session operations
 * authService.session().getSession()
 * ```
 * 
 * @param siteConfig Configuration object containing API key, domain, and application context
 * @see SiteConfig
 * @see IAuthApis
 * @see IAuthAccount
 * @see IAuthSession
 */
class AuthenticationService(
    val siteConfig: SiteConfig,
) {
    val coreClient: CoreClient = CoreClient(siteConfig)
    
    // Lazy initialization to ensure CIAMEventBus is initialized first (in init block)
    // before SessionService/SessionSecure attempts to subscribe to events
    val sessionService: SessionService by lazy { SessionService(siteConfig) }
    
    private lateinit var _notificationManager: CIAMNotificationManager
    private var _sessionValidationService: SessionValidationService? = null

    init {
        // Initialize the lifecycle-aware event bus when the SDK is first created
        if (!CIAMEventBusProvider.isInitialized()) {
            CIAMEventBusProvider.initialize()
        }
    }

    companion object {
        const val CDC_AUTHENTICATION_SERVICE_SECURE_PREFS =
            "cdc_secure_prefs_authentication_service"

        const val CDC_GMID = "cdc_gmid"
        const val CDC_GMID_REFRESH_TS = "cdc_gmid_refresh_ts"
        const val CDC_DEVICE_INFO = "cdc_device_info"
    }

    /**
     * Access authentication operations.
     * 
     * ## Usage
     * ```kotlin
     * authService.authenticate().login().credentials(creds) { /* callbacks */ }
     * authService.authenticate().register().credentials(creds) { /* callbacks */ }
     * authService.authenticate().provider().signIn(activity, provider) { /* callbacks */ }
     * ```
     * 
     * @return Authentication flow interface for login, register, social, OTP, TFA, etc.
     * @see IAuthApis
     */
    fun authenticate(): IAuthApis =
        AuthApis(coreClient, sessionService)

    /**
     * Access account management operations.
     * 
     * ## Usage
     * ```kotlin
     * authService.account().get() { /* callbacks */ }
     * authService.account().set(params) { /* callbacks */ }
     * authService.account().link().toSocial(activity, provider, context) { /* callbacks */ }
     * ```
     * 
     * @return Account operations interface for getting/setting profile data and linking accounts
     * @see IAuthAccount
     */
    fun account(): IAuthAccount =
        AuthAccount(coreClient, sessionService)

    /**
     * Access session management operations.
     * 
     * ## Usage
     * ```kotlin
     * val session = authService.session().getSession()
     * val isValid = authService.session().availableSession()
     * authService.session().clearSession()
     * ```
     * 
     * @return Session interface for session retrieval, validation, and management
     * @see IAuthSession
     */
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
        notificationOptions: CIAMNotificationOptions? = CIAMNotificationOptions()
    ) = apply {
        _notificationManager = CIAMNotificationManager(
            authenticationService = this,
            notificationOptions = notificationOptions!!
        )
        fcmTokenRequest.requestFCMToken()
    }

    /**
     * Registers for periodic session validation in the background.
     * Uses WorkManager to ensure validation continues even if the app is killed.
     * 
     * @param config Configuration for session validation intervals and settings
     */
    fun registerForSessionValidation(
        config: SessionValidationConfig = SessionValidationConfig()
    ) = apply {
        _sessionValidationService = SessionValidationService(
            siteConfig = siteConfig
        ).apply {
            configure(config)
        }
        
        // Connect the session service to trigger validation on new sessions
        sessionService.setValidationTrigger { 
            _sessionValidationService?.onNewSession() 
        }
    }

    /**
     * Session validation service instance.
     * Controls periodic session validation in the background.
     * Returns null if session validation has not been registered.
     * 
     * @return SessionValidationService instance or null if not registered
     */
    fun validationService(): SessionValidationService? = _sessionValidationService

}
