package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.provider.passkey.IPasskeysAuthenticationProvider
import com.sap.cdc.android.sdk.feature.provider.passkey.PasskeyCredentials
import com.sap.cdc.android.sdk.feature.session.SessionSecureLevel
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.feature.messaging.NotificationPermissionManager
import com.sap.cdc.bitsnbytes.feature.provider.PasskeysAuthenticationProvider
import com.sap.cdc.bitsnbytes.ui.state.LoginOptionsState
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.Executor

interface ILoginOptionsViewModel {
    val state: StateFlow<LoginOptionsState>

    fun showBanner(message: String) {}
    fun hideBanner() {}
    fun setLoading(isLoading: Boolean) {}
    fun setError(error: String?) {}

    fun isBiometricActive(): Boolean

    fun isBiometricLocked(): Boolean

    fun isPasswordlessLoginActive(): Boolean

    fun isPasswordlessKeyActive(): Boolean

    fun isPushAuthenticationActive(): Boolean

    fun togglePushAuthentication()

    fun isPushTwoFactorAuthActive(): Boolean

    fun togglePushTwoFactorAuth()

    fun toggleBiometricAuthentication()

    /**
     * Check if notification permission is required and granted
     */
    fun isNotificationPermissionGranted(): Boolean

    /**
     * Request to enable push authentication with permission check
     */
    fun requestPushAuthentication(
        isPermissionGranted: Boolean,
        onPermissionRequired: () -> Unit,
        authCallbacks: AuthCallbacks.() -> Unit
    )

    /**
     * Request to enable push 2-factor authentication with permission check
     */
    fun requestPushTwoFactorAuth(
        isPermissionGranted: Boolean,
        onPermissionRequired: () -> Unit,
        authCallbacks: AuthCallbacks.() -> Unit
    )

    fun biometricOptIn(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        // Stub/.
    }

    fun biometricOptOut(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        // Stub.
    }

    fun biometricLock() {
        //Stub.
    }

    fun biometricUnlock(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        // Stub.
    }

    fun optInForTwoFactorNotifications(
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        //Stub.
    }

    fun optInForAuthenticationNotifications(
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        // Stub.
    }

    fun optOutForAuthenticationNotifications(
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        // Stub.
    }

    fun createPasskey(
        activity: ComponentActivity,
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        // Stub.
    }

    fun loadPasskeys() {
        // Stub.
    }

    val isLoadingPasskeys: Boolean get() = false

}

// Mocked preview class for LoginOptionsViewModel
class LoginOptionsViewModelPreview : ILoginOptionsViewModel {
    override val state: StateFlow<LoginOptionsState> = MutableStateFlow(LoginOptionsState()).asStateFlow()
    
    override fun showBanner(message: String) {}
    override fun hideBanner() {}
    override fun setLoading(isLoading: Boolean) {}
    override fun setError(error: String?) {}
    
    override fun isBiometricActive(): Boolean = true
    override fun isBiometricLocked(): Boolean = false
    override fun isPasswordlessKeyActive(): Boolean = false
    override fun isPasswordlessLoginActive(): Boolean = false
    override fun isPushAuthenticationActive(): Boolean = false
    override fun togglePushAuthentication() {}
    override fun isPushTwoFactorAuthActive(): Boolean = false
    override fun togglePushTwoFactorAuth() {}
    override fun toggleBiometricAuthentication() {}
    override fun isNotificationPermissionGranted(): Boolean = true

    override fun requestPushAuthentication(
        isPermissionGranted: Boolean,
        onPermissionRequired: () -> Unit,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {}
    override fun requestPushTwoFactorAuth(
        isPermissionGranted: Boolean,
        onPermissionRequired: () -> Unit,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {}
}

class LoginOptionsViewModel(
    private val context: Context,
    val authenticationFlowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context),
    ILoginOptionsViewModel {

    private val _state = MutableStateFlow(LoginOptionsState())
    override val state: StateFlow<LoginOptionsState> = _state.asStateFlow()

    override fun showBanner(message: String) {
        _state.update { it.copy(showBanner = true, bannerText = message) }
    }

    override fun hideBanner() {
        _state.update { it.copy(showBanner = false) }
    }

    override fun setLoading(isLoading: Boolean) {
        _state.update { it.copy(isLoading = isLoading) }
    }

    override fun setError(error: String?) {
        _state.update { it.copy(error = error) }
    }

    //region BIOMETRIC

    private var biometricLock by mutableStateOf(false)

    private var biometricActive by mutableStateOf(
        authenticationFlowDelegate.sessionSecurityLevel() == SessionSecureLevel.BIOMETRIC
    )

    /**
     * Check if biometric session encryption is active.
     * This is an application feature only to support the UI design.
     */
    override fun isBiometricActive(): Boolean = biometricActive

    /**
     * Check if biometric session encryption is locked.
     */
    override fun isBiometricLocked(): Boolean = biometricLock

    /**
     * Opt in for biometric session encryption.
     */
    override fun biometricOptIn(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationFlowDelegate.biometricOptIn(
            activity = activity,
            promptInfo = promptInfo,
            executor = executor,
            authCallbacks = authCallbacks
        )
    }

    /**
     * Opt out of biometric session encryption.
     */
    override fun biometricOptOut(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationFlowDelegate.biometricOptOut(
            activity = activity,
            promptInfo = promptInfo,
            executor = executor,
            authCallbacks)
    }

    /**
     * Lock the session - remove from memory only.
     */
    override fun biometricLock() {
        authenticationFlowDelegate.biometricLock()
        biometricLock = true
    }

    override fun biometricUnlock(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        authenticationFlowDelegate.biometricUnlock(
            activity = activity,
            promptInfo = promptInfo,
            executor = executor,
            authCallbacks = authCallbacks
        )
    }

    //endregion

    //region PUSH TFA

    override fun optInForTwoFactorNotifications(
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        viewModelScope.launch {
            authenticationFlowDelegate.optInForTwoFactorNotifications {
                authCallbacks()

                doOnSuccess {
                    togglePushTwoFactorAuth()
                }
            }
        }
    }

    //endregion

    //region PUSH AUTH

    override fun optInForAuthenticationNotifications(
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        viewModelScope.launch {
            authenticationFlowDelegate.optInForAuthenticationNotifications {
                authCallbacks()

                doOnSuccess {
                    togglePushAuthentication()
                }
            }
        }
    }

    override fun optOutForAuthenticationNotifications(
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        viewModelScope.launch {
            authenticationFlowDelegate.optOutForAuthenticationNotifications {
                authCallbacks()

                doOnSuccess {
                    togglePushAuthentication()
                }
            }
        }
    }

    //endregion

    //region PASSKEYS

    private var passkeysAuthenticationProvider: IPasskeysAuthenticationProvider? = null

    // PasskeyCredentials state management
    private var _passkeyCredentials by mutableStateOf<PasskeyCredentials?>(null)
    val passkeyCredentials: PasskeyCredentials? get() = _passkeyCredentials

    // Loading state for passkeys
    private var _isLoadingPasskeys by mutableStateOf(false)
    override val isLoadingPasskeys: Boolean get() = _isLoadingPasskeys


    override fun isPasswordlessKeyActive(): Boolean = _passkeyCredentials?.credentials?.isNotEmpty() == true

    /**
     * Load passkeys using the doOnSuccess pattern.
     * This method is called from the Composable when the screen is displayed.
     */
    override fun loadPasskeys() {
        _isLoadingPasskeys = true
        viewModelScope.launch {
            authenticationFlowDelegate.getPasskeys {
                doOnSuccess { authSuccess ->
                    try {
                        // Parse and update the passkey credentials state
                        val credentials = json.decodeFromString<PasskeyCredentials>(authSuccess.jsonData)
                        _passkeyCredentials = credentials
                    } catch (e: Exception) {
                        // Handle parsing errors silently - don't break the callback chain
                        // Could add logging here if needed
                    } finally {
                        _isLoadingPasskeys = false
                    }
                }

                doOnError { authError ->
                    // Handle error silently for loading
                    // Could add logging here if needed
                    _isLoadingPasskeys = false
                }
            }
        }
    }

    override fun createPasskey(
        activity: ComponentActivity,
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        if (passkeysAuthenticationProvider == null) {
            passkeysAuthenticationProvider = PasskeysAuthenticationProvider(WeakReference(activity))
        }
        viewModelScope.launch {
            authenticationFlowDelegate.passkeyRegister(
                passkeysAuthenticationProvider!!,
            ) {
                authCallbacks()

                doOnSuccess {
                    // Refresh the passkeys list after successful creation
                    loadPasskeys()
                }
            }
        }
    }

    //endregion

    //region AUTHENTICATION OPTIONS STATE MANAGEMENT

    /**
     * Check if passwordless login is active based on available passkeys
     */
    override fun isPasswordlessLoginActive(): Boolean {
        // Check if user has any passkeys available
        return _passkeyCredentials?.credentials?.isNotEmpty() == true
    }

    /**
     * Check if push authentication is active
     */
    override fun isPushAuthenticationActive(): Boolean {
        return authenticationFlowDelegate.isAuthOptionActive(
            AuthenticationFlowDelegate.AuthOption.PUSH_AUTHENTICATION
        )
    }

    /**
     * Toggle push authentication state
     */
    override fun togglePushAuthentication() {
        val currentState = authenticationFlowDelegate.isAuthOptionActive(
            AuthenticationFlowDelegate.AuthOption.PUSH_AUTHENTICATION
        )
        authenticationFlowDelegate.setAuthOptionState(
            AuthenticationFlowDelegate.AuthOption.PUSH_AUTHENTICATION,
            !currentState
        )
    }

    /**
     * Check if push 2-factor authentication is active
     */
    override fun isPushTwoFactorAuthActive(): Boolean {
        return authenticationFlowDelegate.isAuthOptionActive(
            AuthenticationFlowDelegate.AuthOption.PUSH_TWO_FACTOR_AUTHENTICATION
        )
    }

    /**
     * Toggle push 2-factor authentication state
     */
    override fun togglePushTwoFactorAuth() {
        val currentState = authenticationFlowDelegate.isAuthOptionActive(
            AuthenticationFlowDelegate.AuthOption.PUSH_TWO_FACTOR_AUTHENTICATION
        )
        authenticationFlowDelegate.setAuthOptionState(
            AuthenticationFlowDelegate.AuthOption.PUSH_TWO_FACTOR_AUTHENTICATION,
            !currentState
        )
    }

    /**
     * Toggle biometric authentication state
     */
    override fun toggleBiometricAuthentication() {
        val currentState = isBiometricActive()
        biometricActive = !currentState
        if (!biometricActive) {
            // If disabling biometric, also clear any lock state
            biometricLock = false
        }
    }

    //endregion

    //region NOTIFICATION PERMISSION MANAGEMENT

    /**
     * Check if notification permission is granted using the NotificationPermissionManager
     */
    override fun isNotificationPermissionGranted(): Boolean {
        return NotificationPermissionManager.isNotificationPermissionGranted(context)
    }

    /**
     * Request to enable push authentication with permission check
     * This method follows the view/viewmodel separation pattern
     */
    override fun requestPushAuthentication(
        isPermissionGranted: Boolean,
        onPermissionRequired: () -> Unit,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        if (!isPermissionGranted && NotificationPermissionManager.isNotificationPermissionRequired()) {
            // Permission is required but not granted, trigger permission request in view
            onPermissionRequired()
        } else {
            // Permission is granted or not required, proceed with authentication opt-in
            optInForAuthenticationNotifications(authCallbacks)
        }
    }

    /**
     * Request to enable push 2-factor authentication with permission check
     * This method follows the view/viewmodel separation pattern
     */
    override fun requestPushTwoFactorAuth(
        isPermissionGranted: Boolean,
        onPermissionRequired: () -> Unit,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        if (!isPermissionGranted && NotificationPermissionManager.isNotificationPermissionRequired()) {
            // Permission is required but not granted, trigger permission request in view
            onPermissionRequired()
        } else {
            // Permission is granted or not required, proceed with 2FA opt-in
            optInForTwoFactorNotifications(authCallbacks)
        }
    }

    //endregion
}
