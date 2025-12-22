package com.sap.cdc.bitsnbytes.feature.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
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
import com.sap.cdc.android.sdk.feature.biometric.BiometricAuth
import com.sap.cdc.android.sdk.feature.notifications.IFCMTokenRequest
import com.sap.cdc.android.sdk.feature.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.feature.provider.passkey.IPasskeysAuthenticationProvider
import com.sap.cdc.android.sdk.feature.provider.sso.SSOAuthenticationProvider
import com.sap.cdc.android.sdk.feature.provider.web.WebAuthenticationProvider
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJS
import com.sap.cdc.android.sdk.feature.session.Session
import com.sap.cdc.android.sdk.feature.session.SessionSecureLevel
import com.sap.cdc.android.sdk.feature.session.validation.SessionValidationConfig
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
import java.util.concurrent.Executor

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
     * Site configuration - use reinitializeWithNewConfig() to update
     */
    var siteConfig = SiteConfig(context)
        private set

    /**
     * Authentication service - use reinitializeWithNewConfig() to update
     */
    var authenticationService = createAuthenticationService(SiteConfig(context))
        private set

    /**
     * Creates and configures a new AuthenticationService instance.
     * Centralizes service initialization to avoid duplication.
     *
     * @param config The SiteConfig to initialize the service with
     * @return Configured AuthenticationService instance
     */
    private fun createAuthenticationService(config: SiteConfig): AuthenticationService {
        return AuthenticationService(config)
            .registerForPushAuthentication(
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
                })
            .registerForSessionValidation(
                config = SessionValidationConfig(
                    intervalMinutes = 20L,
                    enabled = false, // Debug trigger to disable when needed
                )
            )
    }

    /**
     * Re-initializes the entire authentication system with a new SiteConfig.
     *
     * This method performs a complete reinitialization when configuration changes:
     * 1. Clears existing authentication state and session data
     * 2. Creates new AuthenticationService with fresh CoreClient and SessionService
     * 3. Re-registers all authentication providers with new config
     * 4. Resets biometric authentication (will reinitialize on next use)
     *
     * **Important:** Sessions are API-key specific, so changing configuration
     * requires clearing the old session and starting fresh.
     *
     * @param newSiteConfig The new site configuration to use
     */
    fun reinitializeWithNewConfig(newSiteConfig: SiteConfig) {
        // 1. Clear existing state
        clearAuthenticationState()
        clearCDCSession()
        clearAuthOptionsState()

        // 2. Update siteConfig reference
        this.siteConfig = newSiteConfig

        // 3. Create NEW AuthenticationService (includes fresh CoreClient + SessionService)
        authenticationService = createAuthenticationService(newSiteConfig)

        // 4. Re-register authentication providers
        initializeProviders()

        // Note: biometricAuth is lazy - will reinitialize with new sessionService on next access
    }

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

    /**
     * Initializes authentication providers.
     * Centralizes provider registration to avoid duplication.
     */
    private fun initializeProviders() {
        authenticationProviderMap.clear()

        // Register application specific authentication providers
        registerAuthenticationProvider("facebook", FacebookAuthenticationProvider())

        // Register Google with both modern and legacy names (google + googleplus)
        // This supports legacy server configurations that use "googleplus" without duplicating instances
        registerAuthenticationProvider(
            aliases = listOf("google", "googleplus"),
            provider = GoogleAuthenticationProvider()
        )

        registerAuthenticationProvider(
            "linkedIn", WebAuthenticationProvider(
                "linked",
                siteConfig = authenticationService.siteConfig,
            )
        )
    }

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

        // Initialize authentication providers
        initializeProviders()
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
        biometricAuth.invalidateBiometricAuthenticationState()
    }

    //region LOGIN / LOGOUT / REGISTER METHODS

    suspend fun login(credentials: Credentials, authCallbacks: AuthCallbacks.() -> Unit) {
        authenticationService.authenticate().login()
            .credentials(credentials = credentials) {
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

    suspend fun loginWithCustomId(credentials: CustomIdCredentials, authCallbacks: AuthCallbacks.() -> Unit) {
        authenticationService.authenticate().login()
            .customIdentifier(credentials = credentials) {
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

    suspend fun signInWithProvider(
        hostActivity: ComponentActivity,
        provider: String,
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
        authenticationService.authenticate().provider().signIn(
            hostActivity = hostActivity,
            authenticationProvider = authenticationProvider,
            authCallbacks = authCallbacks
        )
    }

    suspend fun register(
        credentials: Credentials,
        parameters: MutableMap<String, String> = mutableMapOf(),
        authCallbacks: AuthCallbacks.() -> Unit,
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

    suspend fun revokePasskey(
        keyId: String,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.authenticate().passkeys().revoke(
            id = keyId,
            authCallbacks = authCallbacks
        )
    }

    suspend fun getPasskeys(authCallbacks: AuthCallbacks.() -> Unit) {
        authenticationService.authenticate().passkeys().get(authCallbacks)
    }

    suspend fun optInForTwoFactorNotifications(
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.authenticate().tfa().optInForNotifications(authCallbacks)
    }

    suspend fun optInForAuthenticationNotifications(
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.authenticate().push().optInForNotifications(authCallbacks)
    }

    suspend fun optOutForAuthenticationNotifications(
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.authenticate().push().outOutForNotifications(authCallbacks)
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

    suspend fun registerNewAuthenticatorApp(
        twoFactorContext: TwoFactorContext,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.authenticate().tfa().registerTOTP(
            twoFactorContext = twoFactorContext,
            authCallbacks = authCallbacks
        )
    }

    suspend fun verifyTotpCode(
        verificationCode: String,
        rememberDevice: Boolean = false,
        twoFactorContext: TwoFactorContext,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.authenticate().tfa().verifyTOTPCode(
            twoFactorContext = twoFactorContext,
            code = verificationCode,
            rememberDevice = rememberDevice,
            authCallbacks = authCallbacks
        )
    }

    suspend fun singleSignOn(
        hostActivity: ComponentActivity,
        parameters: MutableMap<String, String>?,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationService.authenticate().provider().signIn(
            hostActivity = hostActivity,
            authenticationProvider = SSOAuthenticationProvider(siteConfig, mutableMapOf()),
            parameters = parameters,
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
            )
        }
        return authenticationProviderMap[name]
    }

    /**
     * Register authentication provider with multiple aliases.
     *
     * This method allows registering the same provider instance under multiple names,
     * which is useful for supporting legacy provider names without duplicating instances.
     *
     * ## Use Case: Legacy Provider Names
     *
     * Some servers may use legacy provider names (e.g., "googleplus") while the client
     * uses modern names (e.g., "google"). Rather than creating duplicate provider instances,
     * this method allows mapping multiple names to a single provider.
     *
     * ## Example
     *
     * ```
     * // Google provider can be accessed via "google" OR "googleplus"
     * registerAuthenticationProvider(
     *     aliases = listOf("google", "googleplus"),
     *     provider = GoogleAuthenticationProvider()
     * )
     *
     * // Both work and return the same instance:
     * getAuthenticationProvider("google")      // Same instance
     * getAuthenticationProvider("googleplus")  // Same instance
     * ```
     *
     * @param aliases List of names (including primary and legacy names) that map to this provider
     * @param provider The authentication provider instance to register
     */
    private fun registerAuthenticationProvider(aliases: List<String>, provider: IAuthenticationProvider) {
        aliases.forEach { alias ->
            authenticationProviderMap[alias] = provider
        }
    }

    /**
     * Register authentication provider with a single name.
     * Convenience method for providers without legacy names.
     *
     * @param name The provider name
     * @param provider The authentication provider instance
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

    //region BIOMETRIC AUTHENTICATION

    /**
     * Lazy biometric auth instance - only initialized when first used to save memory
     * since it contains keystore elements that shouldn't be allocated until needed.
     */
    private val biometricAuth: BiometricAuth by lazy {
        BiometricAuth(authenticationService.sessionService)
    }

    /**
     * Stores the navigation route before biometric lock to restore user's position after unlock.
     * Null indicates no route was saved (e.g., first app load with biometric active).
     */
    private var routeBeforeLock: String? = null

    /**
     * Save the current navigation route before locking the biometric session.
     * This allows restoring the user's position after unlock.
     *
     * @param route The current navigation route to save, or null if not available
     */
    fun setRouteBeforeLock(route: String?) {
        routeBeforeLock = route
    }

    /**
     * Get the saved navigation route from before the biometric lock.
     *
     * @return The saved route, or null if no route was saved (first load scenario)
     */
    fun getRouteBeforeLock(): String? {
        return routeBeforeLock
    }

    /**
     * Clear the saved navigation route after it has been used.
     * This prevents stale route data from being used in future unlock operations.
     */
    fun clearRouteBeforeLock() {
        routeBeforeLock = null
    }

    /**
     * Check if biometric session encryption is active.
     */
    fun isBiometricActive(): Boolean {
        return sessionSecurityLevel() == SessionSecureLevel.BIOMETRIC
    }

    /**
     * Check if biometric session is locked.
     */
    fun isBiometricLocked(): Boolean {
        return authenticationService.sessionService.biometricLocked()
    }

    /**
     * Opt in for biometric session encryption.
     */
    fun biometricOptIn(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        biometricAuth.optInForBiometricSessionAuthentication(
            activity = activity,
            promptInfo = promptInfo,
            executor = executor,
            authCallbacks = authCallbacks
        )
    }

    /**
     * Opt out of biometric session encryption.
     */
    fun biometricOptOut(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        biometricAuth.optOutFromBiometricSessionAuthentication(
            activity = activity,
            promptInfo = promptInfo,
            executor = executor,
            authCallbacks = authCallbacks
        )
    }

    /**
     * Lock the biometric session - remove from memory only.
     */
    fun biometricLock() {
        biometricAuth.lockBiometricSession()
    }

    /**
     * Unlock the session with biometric authentication.
     */
    fun biometricUnlock(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor,
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        biometricAuth.unlockSessionWithBiometricAuthentication(
            activity = activity,
            promptInfo = promptInfo,
            executor = executor,
            authCallbacks = authCallbacks
        )
    }

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
        PUSH_AUTHENTICATION,
        PUSH_TWO_FACTOR_AUTHENTICATION,
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
            AuthOption.PUSH_AUTHENTICATION -> state.pushAuthentication
            AuthOption.PUSH_TWO_FACTOR_AUTHENTICATION -> state.pushTwoFactorAuthentication
        }
    }

    /**
     * Set the state of a specific authentication option
     */
    fun setAuthOptionState(option: AuthOption, isActive: Boolean) {
        val currentState = getAuthOptionsState()
        val newState = when (option) {
            AuthOption.PUSH_AUTHENTICATION -> currentState.copy(pushAuthentication = isActive)
            AuthOption.PUSH_TWO_FACTOR_AUTHENTICATION -> currentState.copy(pushTwoFactorAuthentication = isActive)
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
