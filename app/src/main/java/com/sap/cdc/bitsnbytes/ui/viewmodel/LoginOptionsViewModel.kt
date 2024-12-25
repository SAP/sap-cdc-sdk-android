package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import com.linecorp.linesdk.ActionResult
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.auth.biometric.BiometricAuth
import com.sap.cdc.android.sdk.auth.provider.IPasskeysAuthenticationProvider
import com.sap.cdc.android.sdk.auth.session.SessionSecureLevel
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.bitsnbytes.cdc.PasskeysAuthenticationProvider
import kotlinx.coroutines.launch
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

    fun createPasskey(
        success: () -> Unit,
        onFailed: (CDCError) -> Unit
    ) {
        // Stub.
    }

    fun clearPasskey(
        success: () -> Unit,
        onFailed: (CDCError) -> Unit
    ) {
        // Stub.
    }

}

/**
 * Preview mock view model.
 */
class LoginOptionsViewModelPreview : ILoginOptionsViewModel {
    override fun isBiometricActive(): Boolean = false

    override fun isBiometricLocked(): Boolean = false
}

class LoginOptionsViewModel(context: Context) : BaseViewModel(context),
    ILoginOptionsViewModel {

    val passkeysAuthenticationProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
        PasskeysAuthenticationProvider()
    }

    //region BIOMETRICS

    /**
     * Create instance of the biometric auth (no need to singleton it).
     */
    private var biometricAuth = BiometricAuth(identityService.authenticationService.sessionService)

    private var biometricLock by mutableStateOf(false)

    private var biometricActive by mutableStateOf(
        identityService.authenticationService.session()
            .sessionSecurityLevel() == SessionSecureLevel.BIOMETRIC
    )

    override fun isBiometricActive(): Boolean = biometricActive

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

    //region PASSKEYS

    override fun createPasskey(
        success: () -> Unit,
        onFailed: (CDCError) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.createPasskey()
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
        success: () -> Unit,
        onFailed: (CDCError) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.clearPasskey()
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

