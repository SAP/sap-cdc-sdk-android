package com.sap.cdc.bitsnbytes.feature.auth

import android.content.Context
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.events.emitTokenReceived
import com.sap.cdc.android.sdk.feature.auth.AuthenticationService
import com.sap.cdc.android.sdk.feature.auth.flow.AuthCallbacks
import com.sap.cdc.android.sdk.feature.auth.model.Credentials
import com.sap.cdc.android.sdk.feature.auth.model.CustomIdCredentials
import com.sap.cdc.android.sdk.feature.auth.session.Session
import com.sap.cdc.android.sdk.feature.notifications.IFCMTokenRequest
import com.sap.cdc.bitsnbytes.feature.auth.model.AccountEntity
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
    private var siteConfig = SiteConfig(context)

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
}

/**
 * Extension functions for AuthenticationDelegate
 */
fun AuthenticationFlowDelegate.isUserLoggedIn(): Boolean = isAuthenticated.value

fun AuthenticationFlowDelegate.getCurrentUser(): AccountEntity? = userAccount.value
