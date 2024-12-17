package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.example.cdc.model.AccountEntity
import kotlinx.coroutines.launch

interface IAccountViewModel {

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
        // request account information on view model initialization.
        getAccountInfo(mutableMapOf(), success = {}, onFailed = {})
    }

    /**
     * Request account information.
     */
    override fun getAccountInfo(
        parameters: MutableMap<String, String>?,
        success: () -> Unit,
        onFailed: (CDCError) -> Unit
    ) {
        if (!identityService.validSession()) {
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