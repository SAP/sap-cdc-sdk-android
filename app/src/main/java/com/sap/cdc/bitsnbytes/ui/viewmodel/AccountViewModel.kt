package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.auth.biometric.BiometricAuth
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.bitsnbytes.cdc.model.AccountEntity
import com.sap.cdc.bitsnbytes.extensions.splitFullName
import kotlinx.coroutines.launch
import kotlinx.serialization.json.encodeToJsonElement
import java.util.concurrent.Executor

interface IAccountViewModel {

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
        success: () -> Unit, onFailed: (CDCError) -> Unit
    ) {
        //Stub
    }

    fun logOut(success: () -> Unit, onFailed: (CDCError) -> Unit) {
        //Stub
    }
}

class AccountViewModelPreview: IAccountViewModel

class AccountViewModel(context: Context) : BaseViewModel(context), IAccountViewModel {

    private var biometricAuth = BiometricAuth(identityService.authenticationService.sessionService)


    /**
     * Holding reference to account information object.
     */
    private var accountInfo by mutableStateOf<AccountEntity?>(null)

    /**
     * Getter for account information view model interactions.
     */
    override fun accountInfo(): AccountEntity? = accountInfo

    /**
     * Request account information.
     */
    final override fun getAccountInfo(
        parameters: MutableMap<String, String>?,
        success: () -> Unit,
        onFailed: (CDCError) -> Unit
    ) {
        if (!identityService.availableSession()) {
            return
        }
        viewModelScope.launch {
            val authResponse = identityService.getAccountInfo()
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    // Deserialize account data.
                    accountInfo =
                        json.decodeFromString<AccountEntity>(authResponse.asJsonString()!!)
                    success()
                }

                else -> {
                    onFailed(authResponse.toDisplayError()!!)
                }
            }
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
            val setAuthResponse = identityService.setAccountInfo(parameters)
            when (setAuthResponse.state()) {
                AuthState.SUCCESS -> {
                    getAccountInfo(success = success, onFailed = onFailed)
                }

                else -> onFailed(setAuthResponse.toDisplayError()!!)
            }
        }
    }

    /**
     * Log out of current session.
     */
    override fun logOut(success: () -> Unit, onFailed: (CDCError) -> Unit) {
        viewModelScope.launch {
            val authResponse = identityService.logout()
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    success()
                }

                else -> {
                    onFailed(authResponse.toDisplayError()!!)
                }
            }
        }
    }

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