package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.auth.biometric.BiometricAuth
import com.sap.cdc.android.sdk.auth.provider.IPasskeysAuthenticationProvider
import com.sap.cdc.android.sdk.auth.session.SessionSecureLevel
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.bitsnbytes.cdc.PasskeysAuthenticationProvider
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.Executor

interface ILoginOptionsViewModel {

    fun isBiometricActive(): Boolean

    fun isBiometricLocked(): Boolean

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

    fun optInForPushTFA(
        success: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub.
    }

    fun optOnForPushAuth(
        success: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        // Stub.
    }

    fun createPasskey(
        activity: ComponentActivity,
        success: () -> Unit,
        onFailed: (CDCError) -> Unit
    ) {
        // Stub.
    }

    fun clearPasskey(
        activity: ComponentActivity,
        success: () -> Unit,
        onFailed: (CDCError) -> Unit
    ) {
        // Stub.
    }

}

// Mocked preview class for LoginOptionsViewModel
class LoginOptionsViewModelPreview : ILoginOptionsViewModel {

    override fun isBiometricActive(): Boolean = true
    override fun isBiometricLocked(): Boolean = false
}

class LoginOptionsViewModel(context: Context) : BaseViewModel(context),
    ILoginOptionsViewModel {


    /**
     * Create instance of the biometric auth (no need to singleton it).
     */
    private var biometricAuth = BiometricAuth(identityService.authenticationService.sessionService)

    private var biometricLock by mutableStateOf(false)

    private var biometricActive by mutableStateOf(
        identityService.authenticationService.session()
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
                    identityService.sessionSecurityLevel() == SessionSecureLevel.BIOMETRIC
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
                    identityService.sessionSecurityLevel() == SessionSecureLevel.BIOMETRIC
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

    override fun optInForPushTFA(
        success: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val response = identityService.optInForPushTFA()
            when (response.state()) {
                AuthState.SUCCESS -> {
                    // Success.
                    success()
                }

                else -> {
                    onFailedWith(response.toDisplayError())
                }
            }
        }
    }

    //endregion

    //region PUSH AUTH

    override fun optOnForPushAuth(
        success: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val response = identityService.optInForPushTFA()
            when (response.state()) {
                AuthState.SUCCESS -> {
                    // Success.
                    success()
                }

                else -> {
                    onFailedWith(response.toDisplayError())
                }
            }
        }
    }

    //endregion

    //region PASSKEYS

    private var passkeysAuthenticationProvider: IPasskeysAuthenticationProvider? = null

    override fun createPasskey(
        activity: ComponentActivity,
        success: () -> Unit,
        onFailed: (CDCError) -> Unit
    ) {
        if (passkeysAuthenticationProvider == null) {
            passkeysAuthenticationProvider = PasskeysAuthenticationProvider(WeakReference(activity))
        }
        viewModelScope.launch {
            val authResponse = identityService.createPasskey(passkeysAuthenticationProvider!!)
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    // Handle success.
                    success()
                }

                else -> {
                    onFailed(authResponse.cdcResponse().toCDCError())
                }
            }
        }
    }

    override fun clearPasskey(
        activity: ComponentActivity,
        success: () -> Unit,
        onFailed: (CDCError) -> Unit
    ) {
        if (passkeysAuthenticationProvider == null) {
            passkeysAuthenticationProvider = PasskeysAuthenticationProvider(WeakReference(activity))
        }
        viewModelScope.launch {
            val authResponse = identityService.clearPasskey(passkeysAuthenticationProvider!!)
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    // Handle success.
                    success()
                }

                else -> {
                    onFailed(authResponse.cdcResponse().toCDCError())
                }
            }
        }
    }

    //endregion
}

