package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.auth.flow.AuthCallbacks
import com.sap.cdc.android.sdk.feature.biometric.BiometricAuth
import com.sap.cdc.bitsnbytes.extensions.splitFullName
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import kotlinx.coroutines.launch
import kotlinx.serialization.json.encodeToJsonElement
import java.util.concurrent.Executor

interface IAccountViewModel {

    val flowDelegate: AuthenticationFlowDelegate?
        get() = null

    fun promptBiometricUnlockIfNeeded(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor
    ) {
        // Stub
    }

    fun getAccountInfo(
        parameters: MutableMap<String, String>? = mutableMapOf(),
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        // Stub
    }

    fun setAccountInfo(
        newName: String,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        // Stub
    }

    fun logOut(authCallbacks: AuthCallbacks.() -> Unit = {}) {
        //Stub
    }
}

// Mocked preview class for AccountViewModel
class AccountViewModelPreview : IAccountViewModel

class AccountViewModel(context: Context, override val flowDelegate: AuthenticationFlowDelegate) :
    BaseViewModel(context),
    IAccountViewModel {

    private var biometricAuth = BiometricAuth(identityService.authenticationService.sessionService)

    override fun getAccountInfo(
        parameters: MutableMap<String, String>?,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        viewModelScope.launch {
            // Delegate to AuthenticationFlowDelegate which manages state
            flowDelegate.getAccountInfo(parameters = parameters ?: mutableMapOf(), authCallbacks = authCallbacks)
        }
    }

    override fun setAccountInfo(newName: String, authCallbacks: AuthCallbacks.() -> Unit) {
        val newName = newName.splitFullName()
        val profileObject =
            json.encodeToJsonElement(
                mutableMapOf("firstName" to newName.first, "lastName" to newName.second)
            )
        val parameters = mutableMapOf("profile" to profileObject.toString())
        viewModelScope.launch {
            flowDelegate.setAccountInfo(parameters = parameters, authCallbacks = authCallbacks)
        }
    }

    /**
     * Log out of current session.
     */
    override fun logOut(authCallbacks: AuthCallbacks.() -> Unit) {
        viewModelScope.launch {
            flowDelegate.logOut(authCallbacks = authCallbacks)
        }
    }

    /**
     * Show biometric unlock prompt if session is locked.
     */
    override fun promptBiometricUnlockIfNeeded(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor
    ) {
        if (identityService.authenticationService.sessionService.biometricLocked()
        ) {
            biometricAuth.unlockSessionWithBiometricAuthentication(
                activity = activity,
                promptInfo = promptInfo,
                executor = executor,
                onAuthenticationError = { _, _ ->

                },
                onAuthenticationFailed = {

                },
                onAuthenticationSucceeded = {

                }
            )
        }
    }
}
