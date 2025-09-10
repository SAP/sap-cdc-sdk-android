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
import com.sap.cdc.android.sdk.feature.biometric.BiometricAuth
import com.sap.cdc.android.sdk.feature.provider.passkey.IPasskeysAuthenticationProvider
import com.sap.cdc.android.sdk.feature.session.SessionSecureLevel
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.feature.provider.PasskeysAuthenticationProvider
import com.sap.cdc.bitsnbytes.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.Executor

interface ILoginOptionsViewModel {

    fun isBiometricActive(): Boolean

    fun isBiometricLocked(): Boolean

    fun isPasswordlessLoginActive(): Boolean

    fun togglePasswordlessLogin()

    fun isPushAuthenticationActive(): Boolean

    fun togglePushAuthentication()

    fun isPushTwoFactorAuthActive(): Boolean

    fun togglePushTwoFactorAuth()

    fun toggleBiometricAuthentication()

    fun biometricOptIn(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor
    ) {
        // Stub/.
    }

    fun biometricOptOut(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor
    ) {
        // Stub.
    }

    fun biometricLock() {
        //Stub.
    }

    fun biometricUnlock(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor
    ) {
        // Stub.
    }

    fun optInForTwoFactorNotifications(
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        //Stub.
    }

    fun optOnForAuthenticationNotifications(
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

    fun clearPasskey(
        activity: ComponentActivity,
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        // Stub.
    }

}

// Mocked preview class for LoginOptionsViewModel
class LoginOptionsViewModelPreview : ILoginOptionsViewModel {

    override fun isBiometricActive(): Boolean = true
    override fun isBiometricLocked(): Boolean = false
    override fun isPasswordlessLoginActive(): Boolean = false
    override fun togglePasswordlessLogin() {}
    override fun isPushAuthenticationActive(): Boolean = false
    override fun togglePushAuthentication() {}
    override fun isPushTwoFactorAuthActive(): Boolean = false
    override fun togglePushTwoFactorAuth() {}
    override fun toggleBiometricAuthentication() {}
}

class LoginOptionsViewModel(
    context: Context,
    val authenticationFlowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context),
    ILoginOptionsViewModel {


    /**
     * Create instance of the biometric auth (no need to singleton it).
     */
    private var biometricAuth = BiometricAuth(authenticationFlowDelegate.authenticationService.sessionService)

    private var biometricLock by mutableStateOf(false)

    private var biometricActive by mutableStateOf(
        authenticationFlowDelegate.authenticationService.session()
            .sessionSecurityLevel() == SessionSecureLevel.BIOMETRIC
    )

    //region BIOMETRIC

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
        executor: Executor
    ) {
        biometricAuth.optInForBiometricSessionAuthentication(
            activity = activity,
            promptInfo = promptInfo,
            executor = executor,
            onAuthenticationError = { _, _ ->

            },
            onAuthenticationFailed = {

            },
            onAuthenticationSucceeded = {
                biometricActive =
                    authenticationFlowDelegate.sessionSecurityLevel() == SessionSecureLevel.BIOMETRIC
            }
        )
    }

    /**
     * Opt out of biometric session encryption.
     */
    override fun biometricOptOut(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor
    ) {
        biometricAuth.optOutFromBiometricSessionAuthentication(
            activity = activity,
            promptInfo = promptInfo,
            executor = executor,
            onAuthenticationError = { _, _ ->

            },
            onAuthenticationFailed = {

            },
            onAuthenticationSucceeded = {
                biometricActive =
                    authenticationFlowDelegate.sessionSecurityLevel() == SessionSecureLevel.BIOMETRIC
            }
        )
    }

    /**
     * Lock the session - remove from memory only.
     */
    override fun biometricLock() {
        biometricAuth.lockBiometricSession()
        biometricLock = true
    }

    override fun biometricUnlock(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor
    ) {
        biometricAuth.unlockSessionWithBiometricAuthentication(
            activity = activity,
            promptInfo = promptInfo,
            executor = executor,
            onAuthenticationError = { _, _ -> },
            onAuthenticationFailed = {},
            onAuthenticationSucceeded = {
                biometricLock = false
            }
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

    override fun optOnForAuthenticationNotifications(
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        viewModelScope.launch {
            authenticationFlowDelegate.optOnForAuthenticationNotifications {
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
                authCallbacks
            )
        }
    }

    override fun clearPasskey(
        activity: ComponentActivity,
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        if (passkeysAuthenticationProvider == null) {
            passkeysAuthenticationProvider = PasskeysAuthenticationProvider(WeakReference(activity))
        }
    }

    //endregion

    //region AUTHENTICATION OPTIONS STATE MANAGEMENT

    /**
     * Check if passwordless login is active
     */
    override fun isPasswordlessLoginActive(): Boolean {
        return authenticationFlowDelegate.isAuthOptionActive(
            AuthenticationFlowDelegate.AuthOption.PASSWORDLESS_LOGIN
        )
    }

    /**
     * Toggle passwordless login state
     */
    override fun togglePasswordlessLogin() {
        val currentState = authenticationFlowDelegate.isAuthOptionActive(
            AuthenticationFlowDelegate.AuthOption.PASSWORDLESS_LOGIN
        )
        authenticationFlowDelegate.setAuthOptionState(
            AuthenticationFlowDelegate.AuthOption.PASSWORDLESS_LOGIN,
            !currentState
        )
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
        val currentState = authenticationFlowDelegate.isAuthOptionActive(
            AuthenticationFlowDelegate.AuthOption.BIOMETRIC_AUTHENTICATION
        )
        authenticationFlowDelegate.setAuthOptionState(
            AuthenticationFlowDelegate.AuthOption.BIOMETRIC_AUTHENTICATION,
            !currentState
        )
    }

    //endregion
}
