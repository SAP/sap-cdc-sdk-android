package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.auth.biometric.BiometricAuth
import com.sap.cdc.android.sdk.auth.session.SessionSecureLevel
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.example.cdc.model.AccountEntity
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

interface IAccountViewModel {

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
}

class AccountViewModel(context: Context) : BaseViewModel(context), IAccountViewModel {

    /**
     * Holding reference to account information object.
     */
    private var accountInfo by mutableStateOf<AccountEntity?>(null)

    /**
     * Getter for account information view model interactions.
     */
    override fun accountInfo(): AccountEntity? = accountInfo

    init {
        if (identityService.sessionSecurityLevel() == SessionSecureLevel.STANDARD) {
            // request account information on view model initialization.
            getAccountInfo(mutableMapOf(), success = {}, onFailed = {})
        }
    }

    /**
     * Request account information.
     */
    override fun getAccountInfo(
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
}