package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.feature.auth.flow.AuthCallbacks
import com.sap.cdc.android.sdk.feature.biometric.BiometricAuth
import com.sap.cdc.bitsnbytes.extensions.splitFullName
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.feature.auth.model.AccountEntity
import kotlinx.coroutines.launch
import kotlinx.serialization.json.encodeToJsonElement
import java.util.concurrent.Executor

interface IAccountViewModel {

    val flowDelegate: AuthenticationFlowDelegate?
        get() = null

    fun updateAccountInfoWith(name: String, success: () -> Unit, onFailed: (CDCError) -> Unit) {
        //Stub
    }

    fun promptBiometricUnlockIfNeeded(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor
    ) {
        // Stub
    }

    fun accountInfo(): AccountEntity? = null

    fun getAccountInfo(
        parameters: MutableMap<String, String>? = mutableMapOf(),
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

class AccountViewModel(context: Context, override val flowDelegate: AuthenticationFlowDelegate) : BaseViewModel(context),
    IAccountViewModel {

    private var biometricAuth = BiometricAuth(identityService.authenticationService.sessionService)

    /**
     * Getter for account information view model interactions.
     * Now delegates to AuthenticationFlowDelegate for single source of truth.
     */
    override fun accountInfo(): AccountEntity? = flowDelegate.userAccount.value

    override fun getAccountInfo(
        parameters: MutableMap<String, String>?,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        viewModelScope.launch {
            // Delegate to AuthenticationFlowDelegate which manages state
            flowDelegate.getAccountInfo(parameters, authCallbacks)
        }
    }


    /**
     * Update account information with new name.
     * Name parameter will be split to firstName & lastName to update profile fields.
     */
    override fun updateAccountInfoWith(
        name: String,
        success: () -> Unit,
        onFailed: (CDCError) -> Unit
    ) {
        val newName = name.splitFullName()
        val profileObject =
            json.encodeToJsonElement(
                mutableMapOf("firstName" to newName.first, "lastName" to newName.second)
            )
        val parameters = mutableMapOf("profile" to profileObject.toString())
        viewModelScope.launch {
//            val setAuthResponse = identityService.setAccountInfo(parameters)
//            when (setAuthResponse.state()) {
//                AuthState.SUCCESS -> {
//                    getAccountInfo(success = success, onFailed = onFailed)
//                }
//
//                else -> onFailed(setAuthResponse.toDisplayError()!!)
//            }
        }
    }

    /**
     * Log out of current session.
     */
    override fun logOut(authCallbacks: AuthCallbacks.() -> Unit) {
        viewModelScope.launch {
            flowDelegate.cdc.logOut(authCallbacks = authCallbacks)
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
