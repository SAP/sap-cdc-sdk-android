package com.sap.cdc.bitsnbytes.feature.auth

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.events.emitTokenReceived
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.AuthError
import com.sap.cdc.android.sdk.feature.AuthenticationService
import com.sap.cdc.android.sdk.feature.Credentials
import com.sap.cdc.android.sdk.feature.CustomIdCredentials
import com.sap.cdc.android.sdk.feature.LinkingContext
import com.sap.cdc.android.sdk.feature.notifications.IFCMTokenRequest
import com.sap.cdc.android.sdk.feature.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.feature.provider.passkey.IPasskeysAuthenticationProvider
import com.sap.cdc.android.sdk.feature.provider.web.WebAuthenticationProvider
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJS
import com.sap.cdc.android.sdk.feature.session.Session
import com.sap.cdc.android.sdk.feature.session.SessionSecureLevel
import com.sap.cdc.bitsnbytes.feature.auth.model.AccountEntity
import com.sap.cdc.bitsnbytes.feature.provider.FacebookAuthenticationProvider
import com.sap.cdc.bitsnbytes.feature.provider.GoogleAuthenticationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Complete authentication state solution for ViewModels.
 * Provides direct access to CDC SDK and manages authentication state.
 *
 * Key benefits:
 * - Direct CDC SDK access (eliminates repository passthrough)
 * - Centralized authentication state management
 * - Activity-scoped lifecycle when used with ViewModelScopeProvider
 * - Single instance shared across all ViewModels
 */
class AuthenticationFlowDelegate(context: Context) {

    // Direct CDC SDK access - eliminates repository passthrough boilerplate
    /**
     * Initialize the site configuration class
     */
    var siteConfig = SiteConfig(context)

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
     * Re-init the session service with new site configuration.
     */
    fun reinitializeSessionService(siteConfig: SiteConfig) =
        authenticationService.session().resetWithConfig(siteConfig)

    /**
     * Get session security level (STANDARD/BIOMETRIC).
     */
    fun sessionSecurityLevel(): SessionSecureLevel =
        authenticationService.session().sessionSecurityLevel()

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

    // Authentication state flows
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _userAccount = MutableStateFlow<AccountEntity?>(null)
    val userAccount: StateFlow<AccountEntity?> = _userAccount.asStateFlow()

    // JSON serializer for parsing account data
    private val json = Json { ignoreUnknownKeys = true }

    // Coroutine scope for async operations
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        // Initialize state based on existing CDC session
        syncWithCDCSession()
    }

    // State management methods
    fun setAuthenticated(isAuth: Boolean) {
        _isAuthenticated.value = isAuth
    }

    fun clearAuthenticationState() {
        _isAuthenticated.value = false
        _userAccount.value = null
    }

    // CDC Session integration helpers
    fun updateFromCDCSession(session: Session?) {
        if (session != null) {
            setAuthenticated(true)
        } else {
            setAuthenticated(false)
        }
    }

    // Convenience methods for common CDC operations
    fun hasValidSession(): Boolean = authenticationService.session().availableSession()
    fun getCurrentCDCSession(): Session? = authenticationService.session().getSession()
    fun clearCDCSession() = authenticationService.session().clearSession()

    // Helper method to sync with CDC session
    private fun syncWithCDCSession() {
        val cdcSession = getCurrentCDCSession()
        updateFromCDCSession(cdcSession)
    }

    /**
     * Handle session expiration - clears both CDC session and local state
     */
    fun handleSessionExpired() {
        clearCDCSession()
        clearAuthenticationState()
    }

    //region LOGIN / LOGOUT / REGISTER METHODS
    suspend fun login(credentials: Credentials, authCallbacks: AuthCallbacks.() -> Unit) {
        authenticationService.authenticate().login()
            .credentials(credentials = credentials, configure = authCallbacks)
    }

    suspend fun loginWithCustomId(credentials: CustomIdCredentials, authCallbacks: AuthCallbacks.() -> Unit) {
        authenticationService.authenticate().login()
            .customIdentifier(credentials = credentials, authCallbacks = authCallbacks)
    }

    suspend fun register(
        credentials: Credentials,
        authCallbacks: AuthCallbacks.() -> Unit,
        parameters: MutableMap<String, String> = mutableMapOf()
    ) {
        authenticationService.authenticate().register().credentials(
            credentials = credentials, configure = authCallbacks,
            parameters = parameters
        )
    }

    suspend fun resolvePendingRegistration(
        missingFieldsSerialized: MutableMap<String, String>,
        regToken: String,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.authenticate().register().resolve().pendingRegistrationWith(
            missingFields = missingFieldsSerialized,
            regToken = regToken,
            configure = authCallbacks
        )
    }

    suspend fun logOut(authCallbacks: AuthCallbacks.() -> Unit) {
        authenticationService.authenticate().logout {
            // Register original callbacks first
            authCallbacks()

            // Add state management side-effect to clear account state on successful logout
            doOnSuccess {
                clearAuthenticationState()
            }

            doOnError {
                // Even on error, clear local state since logout was attempted
                clearAuthenticationState()
            }
        }
    }

    //endregion

    //region ACCOUNT MANAGEMENT METHODS

    /**
     * Get account information with state management.
     * This method intercepts the response and updates the userAccount StateFlow.
     */
    suspend fun getAccountInfo(
        parameters: MutableMap<String, String> = mutableMapOf(),
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.account().get(
            parameters = parameters,
            includeFields = listOf(
                "data",
                "profile",
                "emails",
                "missing-required-fields",
                "customIdentifiers"
            )
        ) {
            // Register original callbacks first
            authCallbacks()

            // Add state management side-effect
            doOnSuccess { authSuccess ->
                try {
                    // Parse and update the state
                    val accountData = json.decodeFromString<AccountEntity>(authSuccess.jsonData)
                    _userAccount.value = accountData
                } catch (e: Exception) {
                    // Handle parsing errors silently - don't break the callback chain
                    // Could add logging here if needed
                }
            }
        }
    }

    suspend fun setAccountInfo(
        parameters: MutableMap<String, String> = mutableMapOf(),
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.account().set(
            parameters = parameters,
            refreshOnSuccess = true
        ) {
            // Register original callbacks first
            authCallbacks()

            // Add state management side-effect
            doOnSuccess { authSuccess ->
                try {
                    // Parse and update the state
                    val accountData = json.decodeFromString<AccountEntity>(authSuccess.jsonData)
                    _userAccount.value = accountData
                } catch (e: Exception) {
                    // Handle parsing errors silently - don't break the callback chain
                    // Could add logging here if needed
                }
            }
        }
    }

    //endregion

    //region ADDITIONAL AUTHENTICATION METHODS
    suspend fun otpSendCode(
        parameters: MutableMap<String, String>,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.authenticate().otp().sendCode(
            parameters = parameters,
            authCallbacks = authCallbacks
        )
    }

    suspend fun otpVerify(
        code: String,
        vToken: String,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        //TODO: Add error break if vToken is null in otpContext
        authenticationService.authenticate().otp().resolve().login(
            code = code,
            vToken = vToken,
            authCallbacks = authCallbacks
        )
    }

    suspend fun getSaptchaToken(
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.authenticate().captcha().getSaptchaToken(authCallbacks)
    }

    suspend fun passkeyLogin(
        provider: IPasskeysAuthenticationProvider,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.authenticate().passkeys().login(
            authenticationProvider = provider,
            authCallbacks = authCallbacks
        )
    }

    suspend fun registerForAuthNotifications(
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.authenticate().push().registerForAuthNotifications(authCallbacks)
    }

    //endregion

    //region ACCOUNT LINKING METHODS

    suspend fun linkToSiteAccount(
        loginId: String, password: String,
        linkingContext: LinkingContext,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.account().link().toSite(
            mutableMapOf("loginID" to loginId, "password" to password),
            linkingContext = linkingContext,
            authCallbacks = authCallbacks
        )
    }

    suspend fun linkToSocialProvider(
        hostActivity: ComponentActivity,
        provider: String,
        linkingContext: LinkingContext,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val authenticationProvider = getAuthenticationProvider(provider)
        if (authenticationProvider == null) {
            // Handle unknown provider error
            authCallbacks.invoke(AuthCallbacks().apply {
                onError?.let { it(AuthError(message = "Unknown authentication provider: $provider")) }
            })
            return
        }
        authenticationService.account().link().toSocial(
            hostActivity = hostActivity,
            authenticationProvider = authenticationProvider,
            linkingContext = linkingContext,
            authCallbacks = authCallbacks
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

/**
 * Extension functions for AuthenticationDelegate
 */
fun AuthenticationFlowDelegate.isUserLoggedIn(): Boolean = isAuthenticated.value

fun AuthenticationFlowDelegate.getCurrentUser(): AccountEntity? = userAccount.value
