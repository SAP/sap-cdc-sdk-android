package com.sap.cdc.bitsnbytes.feature.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.content.edit
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
import com.sap.cdc.android.sdk.feature.TwoFactorContext
import com.sap.cdc.android.sdk.feature.notifications.IFCMTokenRequest
import com.sap.cdc.android.sdk.feature.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.feature.provider.passkey.IPasskeysAuthenticationProvider
import com.sap.cdc.android.sdk.feature.provider.web.WebAuthenticationProvider
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJS
import com.sap.cdc.android.sdk.feature.session.Session
import com.sap.cdc.android.sdk.feature.session.SessionSecureLevel
import com.sap.cdc.android.sdk.feature.tfa.TFAPhoneMethod
import com.sap.cdc.bitsnbytes.feature.auth.model.AccountEntity
import com.sap.cdc.bitsnbytes.feature.provider.FacebookAuthenticationProvider
import com.sap.cdc.bitsnbytes.feature.provider.GoogleAuthenticationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
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

    // Context reference for SharedPreferences
    private val appContext = context.applicationContext

    // SharedPreferences for authentication options state
    private val authOptionsPrefs: SharedPreferences by lazy {
        appContext.getSharedPreferences("auth_options_state", Context.MODE_PRIVATE)
    }

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
                clearAuthOptionsState()
            }

            doOnError {
                // Even on error, clear local state since logout was attempted
                clearAuthenticationState()
                clearAuthOptionsState()
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

    suspend fun passkeyRegister(
        provider: IPasskeysAuthenticationProvider,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.authenticate().passkeys().create(
            authenticationProvider = provider,
            authCallbacks = authCallbacks
        )
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

    suspend fun optInForTwoFactorNotifications(
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.authenticate().tfa().optInForNotifications(authCallbacks)
    }

    suspend fun optOnForAuthenticationNotifications(
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.authenticate().push().optInForNotifications(authCallbacks)
    }

    suspend fun getRegisteredPhoneNumbers(
        twoFactorContext: TwoFactorContext,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.authenticate().tfa().getRegisteredPhoneNumbers(
            twoFactorContext = twoFactorContext,
            authCallbacks = authCallbacks
        )
    }

    suspend fun registerPhoneNumber(
        phoneNumber: String,
        language: String = "en",
        twoFactorContext: TwoFactorContext,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.authenticate().tfa().registerPhone(
            twoFactorContext = twoFactorContext,
            phoneNumber = phoneNumber,
            language = language,
            authCallbacks = authCallbacks
        )
    }

    suspend fun sendPhoneCode(
        phoneId: String,
        method: TFAPhoneMethod?,
        language: String = "en",
        twoFactorContext: TwoFactorContext,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.authenticate().tfa().sendPhoneCode(
            twoFactorContext = twoFactorContext,
            phoneId = phoneId,
            method = method,
            language = language,
            authCallbacks = authCallbacks
        )
    }

    suspend fun verifyPhoneCode(
        verificationCode: String,
        rememberDevice: Boolean = false,
        twoFactorContext: TwoFactorContext,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.authenticate().tfa().verifyPhoneCode(
            twoFactorContext = twoFactorContext,
            code = verificationCode,
            rememberDevice = rememberDevice,
            authCallbacks = authCallbacks
        )
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

    //region AUTHENTICATION OPTIONS STATE MANAGEMENT

    /**
     * Data class representing the authentication options state
     */
    @Serializable
    data class AuthOptionsState(
        val passwordlessLogin: Boolean = false,
        val pushAuthentication: Boolean = false,
        val pushTwoFactorAuthentication: Boolean = false,
        val biometricAuthentication: Boolean = false
    )

    /**
     * Enum representing the different authentication options
     */
    enum class AuthOption {
        PASSWORDLESS_LOGIN,
        PUSH_AUTHENTICATION,
        PUSH_TWO_FACTOR_AUTHENTICATION,
        BIOMETRIC_AUTHENTICATION
    }

    companion object {
        private const val AUTH_OPTIONS_STATE_KEY = "auth_options_state_json"
        private const val LOG_TAG = "AuthOptionsState"
    }

    /**
     * Save the authentication options state as JSON string in SharedPreferences
     */
    fun saveAuthOptionsState(state: AuthOptionsState) {
        try {
            val jsonString = json.encodeToString(state)
            authOptionsPrefs.edit {
                putString(AUTH_OPTIONS_STATE_KEY, jsonString)
            }
            Log.d(LOG_TAG, "Auth options state saved: $jsonString")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to save auth options state", e)
        }
    }

    /**
     * Get the authentication options state from SharedPreferences
     */
    fun getAuthOptionsState(): AuthOptionsState {
        return try {
            val jsonString = authOptionsPrefs.getString(AUTH_OPTIONS_STATE_KEY, null)
            if (jsonString != null) {
                json.decodeFromString<AuthOptionsState>(jsonString)
            } else {
                AuthOptionsState() // Return default state
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to load auth options state, returning default", e)
            AuthOptionsState() // Return default state on error
        }
    }

    /**
     * Check if a specific authentication option is active
     */
    fun isAuthOptionActive(option: AuthOption): Boolean {
        val state = getAuthOptionsState()
        return when (option) {
            AuthOption.PASSWORDLESS_LOGIN -> state.passwordlessLogin
            AuthOption.PUSH_AUTHENTICATION -> state.pushAuthentication
            AuthOption.PUSH_TWO_FACTOR_AUTHENTICATION -> state.pushTwoFactorAuthentication
            AuthOption.BIOMETRIC_AUTHENTICATION -> state.biometricAuthentication
        }
    }

    /**
     * Set the state of a specific authentication option
     */
    fun setAuthOptionState(option: AuthOption, isActive: Boolean) {
        val currentState = getAuthOptionsState()
        val newState = when (option) {
            AuthOption.PASSWORDLESS_LOGIN -> currentState.copy(passwordlessLogin = isActive)
            AuthOption.PUSH_AUTHENTICATION -> currentState.copy(pushAuthentication = isActive)
            AuthOption.PUSH_TWO_FACTOR_AUTHENTICATION -> currentState.copy(pushTwoFactorAuthentication = isActive)
            AuthOption.BIOMETRIC_AUTHENTICATION -> {
                val updatedState = currentState.copy(biometricAuthentication = isActive)
                // When biometric is activated, deactivate all other options
                if (isActive) {
                    updatedState.copy(
                        passwordlessLogin = false,
                        pushAuthentication = false,
                        pushTwoFactorAuthentication = false
                    )
                } else {
                    updatedState
                }
            }
        }
        saveAuthOptionsState(newState)
    }

    /**
     * Clear all authentication options state
     */
    fun clearAuthOptionsState() {
        authOptionsPrefs.edit {
            remove(AUTH_OPTIONS_STATE_KEY)
        }
        Log.d(LOG_TAG, "Auth options state cleared")
    }

    //endregion
}

/**
 * Extension functions for AuthenticationDelegate
 */
fun AuthenticationFlowDelegate.isUserLoggedIn(): Boolean = isAuthenticated.value

fun AuthenticationFlowDelegate.getCurrentUser(): AccountEntity? = userAccount.value
