package com.sap.cdc.bitsnbytes.feature.auth

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.events.CDCEventBusProvider
import com.sap.cdc.android.sdk.events.emitTokenReceived
import com.sap.cdc.android.sdk.feature.auth.AuthenticationService
import com.sap.cdc.android.sdk.feature.auth.IAuthResponse
import com.sap.cdc.android.sdk.feature.auth.ResolvableContext
import com.sap.cdc.android.sdk.feature.auth.session.Session
import com.sap.cdc.android.sdk.feature.auth.session.SessionSecureLevel
import com.sap.cdc.android.sdk.feature.notifications.IFCMTokenRequest
import com.sap.cdc.android.sdk.feature.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.feature.provider.passkey.IPasskeysAuthenticationProvider
import com.sap.cdc.android.sdk.feature.provider.sso.SSOAuthenticationProvider
import com.sap.cdc.android.sdk.feature.provider.web.WebAuthenticationProvider
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJS
import com.sap.cdc.bitsnbytes.feature.provider.FacebookAuthenticationProvider
import com.sap.cdc.bitsnbytes.feature.provider.GoogleAuthenticationProvider

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

/**
 * Singleton class for interacting with the CDC SDK.
 * This approach is the simplest one to assure a single instance of the SDK is used throughout the application.
 * Using Kotlin object class is also a valid approach.
 */
class IdentityServiceRepository private constructor(context: Context) {

    companion object {
        @Volatile
        private var instance: IdentityServiceRepository? = null

        fun getInstance(context: Context): IdentityServiceRepository {
            return instance ?: synchronized(this) {
                instance ?: IdentityServiceRepository(context = context).also { instance = it }
            }
        }
    }

    //region INIT

    /**
     * Initialize the site configuration class
     */
    private var siteConfig = SiteConfig(context)

    init {
        // Initialize the lifecycle-aware event bus when the SDK is first created
        if (!CDCEventBusProvider.isInitialized()) {
            CDCEventBusProvider.initialize()
        }
    }

    /**
     * Initialize authentication service.
     */
    var authenticationService = AuthenticationService(siteConfig).registerForPushAuthentication(
        object : IFCMTokenRequest {
            override fun requestFCMToken() {
                FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        return@OnCompleteListener
                    }

                    // Get new FCM registration token
                    val token = task.result
                    emitTokenReceived(token)
                })
            }
        }
    )

    /**
     * Authentication providers map.
     * Keeps record to registered authenticators (for this example Google, Facebook, WeChat & Line are used).
     */
    private var authenticationProviderMap: MutableMap<String, IAuthenticationProvider> =
        mutableMapOf()

    init {
        // Using session migrator to try and migrate an existing session in an application using old versions
        // of the gigya-android-sdk library.
        val sessionMigrator = SessionMigrator(context)
        sessionMigrator.tryMigrateSession(
            authenticationService,
            success = {
                Log.e(SessionMigrator.LOG_TAG, "Session migration success")

            },
            failure = {
                Log.e(SessionMigrator.LOG_TAG, "Session migration failed")
            })

        // Register application specific authentication providers.
        registerAuthenticationProvider("facebook", FacebookAuthenticationProvider())
        registerAuthenticationProvider("google", GoogleAuthenticationProvider())
    }

    //endregion

    //region CONFIGURATION

    /**
     * Re-init the session service with new site configuration.
     */
    fun reinitializeSessionService(siteConfig: SiteConfig) =
        authenticationService.session().resetWithConfig(siteConfig)


    /**
     * Get current instance of the current SiteConfig class.
     */
    fun getConfig() = siteConfig

    //endregion

    //region SESSION MANAGEMENT

    /**
     * Set a new session (secure it).
     */
    fun setSession(session: Session) {
        authenticationService.session().setSession(session)
    }

    /**
     * Get current secured session.
     */
    fun getSession(): Session? = authenticationService.session().getSession()


    /**
     * Clear session secure session.
     */
    fun invalidateSession() = authenticationService.session().clearSession()

    /**
     * Check if a valid session is available.
     */
    fun availableSession(): Boolean = authenticationService.session().availableSession()


    /**
     * Get session security level (STANDARD/BIOMETRIC).
     */
    fun sessionSecurityLevel(): SessionSecureLevel =
        authenticationService.session().sessionSecurityLevel()

    //endregion

    //region AUTHENTICATION FLOWS

    /**
     * Initiate cdc SDK native social provider login flow.
     */
    suspend fun nativeSocialSignIn(
        hostActivity: ComponentActivity,
        provider: IAuthenticationProvider
    ): IAuthResponse =
        authenticationService.authenticate().provider().signIn(
            hostActivity, provider
        )

    /**
     * Initiate single sign on provider flow.
     */
    suspend fun sso(
        hostActivity: ComponentActivity,
        parameters: MutableMap<String, String>,
    ): IAuthResponse =
        authenticationService.authenticate().provider().signIn(
            hostActivity, SSOAuthenticationProvider(
                siteConfig = authenticationService.siteConfig,
                mutableMapOf()
            )
        )


    /**
     * Initiate cdc Web social provider login (any provider that is currently noy native).
     */
    suspend fun webSocialSignIn(
        hostActivity: ComponentActivity,
        socialProvider: String
    ): IAuthResponse {
        val webAuthenticationProvider = WebAuthenticationProvider(
            socialProvider,
            siteConfig,
            authenticationService.session().getSession()
        )
        return authenticationService.authenticate().provider().signIn(
            hostActivity, webAuthenticationProvider
        )
    }

    /**
     * Initiate cdc phone number sign in flow.
     * This is a 2 step flow.
     * 1. call signInWithPhone to request authentication code to be sent to the phone number provided.
     * 2. login/update using the code received.
     */
//    suspend fun otpSignIn(
//        parameters: MutableMap<String, String>
//    ): IAuthResponse = authenticationService.authenticate().otp().sendCode(parameters)

    suspend fun createPasskey(
        passkeysAuthenticationProvider: IPasskeysAuthenticationProvider
    ): IAuthResponse {
        return authenticationService.authenticate().passkeys()
            .create(passkeysAuthenticationProvider)
    }

    suspend fun passkeySignIn(
        passkeysAuthenticationProvider: IPasskeysAuthenticationProvider
    ): IAuthResponse {
        return authenticationService.authenticate().passkeys()
            .signIn(passkeysAuthenticationProvider)
    }

    suspend fun clearPasskey(
        passkeysAuthenticationProvider: IPasskeysAuthenticationProvider
    ): IAuthResponse {
        return authenticationService.authenticate().passkeys()
            .clear(passkeysAuthenticationProvider)
    }

//    suspend fun getSaptchaToken(): IAuthResponse {
//        return authenticationService.authenticate().captcha().getSaptchaToken()
//    }

    //endregion

    //region PUSH

    suspend fun optInForPushTFA(): IAuthResponse {
        return authenticationService.tfa().optInForPushAuthentication()
    }

    suspend fun optInForPushAuth(): IAuthResponse {
        return authenticationService.authenticate().push().registerForAuthNotifications()
    }

    //endregion

    //region RESOLVE INTERRUPTIONS


    /**
     * Attempt to resolve account linking interruption to an existing site account.
     */
    suspend fun resolveLinkToSiteAccount(
        loginId: String,
        password: String,
        resolvableContext: ResolvableContext
    ): IAuthResponse {
        return authenticationService.resolve().linkSiteAccount(
            mutableMapOf("loginID" to loginId, "password" to password),
            resolvableContext,
        )
    }

    /**
     * Attempt to resolve account linking interruption to an existing social account.
     */
    suspend fun resolveLinkToSocialAccount(
        hostActivity: ComponentActivity,
        authenticationProvider: IAuthenticationProvider,
        resolvableContext: ResolvableContext
    ): IAuthResponse {
        return authenticationService.resolve().linkSocialAccount(
            hostActivity, authenticationProvider, resolvableContext,
        )
    }

    /**
     * Attempt to resolve phone number sign in flow.
     */
//    suspend fun resolveLoginWithCode(
//        code: String,
//        resolvableContext: ResolvableContext
//    ): IAuthResponse {
//        return authenticationService.authenticate()
//            .otp()
//            .resolve()
//            .login(code, resolvableContext)
//    }

    //endregion

    //region TWO FACTOR AUTHENTICATION

    suspend fun registerTFAPhoneNumber(
        phoneNumber: String,
        resolvableContext: ResolvableContext,
        language: String? = "en",
    ): IAuthResponse {
        return authenticationService.tfa().registerPhone(
            phoneNumber = phoneNumber,
            resolvableContext = resolvableContext,
            language = language
        )
    }

    suspend fun sendRegisteredPhoneCode(
        phoneId: String,
        resolvableContext: ResolvableContext,
        language: String? = "en",
    ): IAuthResponse {
        return authenticationService.tfa().sendPhoneCode(
            phoneId = phoneId,
            resolvableContext = resolvableContext,
            language = language,
        )
    }

    suspend fun verifyTFAPhoneCode(
        code: String,
        resolvableContext: ResolvableContext,
        rememberDevice: Boolean,
    ): IAuthResponse {
        return authenticationService.tfa().verifyPhoneCode(
            code = code,
            resolvableContext = resolvableContext,
            rememberDevice = rememberDevice
        )
    }

    suspend fun getRegisteredTFAPhoneNumbers(
        resolvableContext: ResolvableContext,
    ): IAuthResponse {
        return authenticationService.tfa().getRegisteredPhoneNumbers(resolvableContext)
    }

    suspend fun registerNewAuthenticatorApp(
        resolvableContext: ResolvableContext,
    ): IAuthResponse {
        return authenticationService.tfa().registerTOTP(resolvableContext)
    }

    suspend fun verifyTotpCode(
        code: String,
        resolvableContext: ResolvableContext,
        rememberDevice: Boolean? = false,
    ): IAuthResponse {
        return authenticationService.tfa().verifyTOTPCode(
            resolvableContext,
            code, rememberDevice
        )
    }

    //endregion

    //region SOCIAL PROVIDERS

    /**
     * Get reference to registered authentication provider.
     */
    fun getAuthenticationProvider(name: String): IAuthenticationProvider? {
        if (!authenticationProviderMap.containsKey(name)) {
            return WebAuthenticationProvider(
                name,
                siteConfig,
                authenticationService.session().getSession()
            )
        }
        return authenticationProviderMap[name]
    }

    /**
     * Register new authentication provider.
     */
    private fun registerAuthenticationProvider(name: String, provider: IAuthenticationProvider) {
        authenticationProviderMap[name] = provider
    }

    fun getAuthenticatorMap() = authenticationProviderMap

    //endregion

    //region WEB BRIDGE

    /**
     * Instantiate a new WebBridgeJS element.
     */
    fun getWebBridge(): WebBridgeJS = WebBridgeJS(authenticationService)

    //endregion
}