package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.auth.ResolvableContext
import com.sap.cdc.android.sdk.core.api.model.CDCError
import kotlinx.coroutines.launch

/**
 * Created by Tal Mirmelshtein on 17/12/2024
 * Copyright: SAP LTD.
 */

interface IEmailSignInViewModel {

    fun login(
        email: String,
        password: String,
        onLogin: () -> Unit,
        onLoginIdentifierExists: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }
}

/**
 * Preview mock view model.
 */
class EmailSignInViewModelPreview() : IEmailSignInViewModel {}

class EmailSignInViewModel(context: Context) : BaseViewModel(context), IEmailSignInViewModel {

    /**
     * Login to existing account using credentials (email/password)
     * ViewModel example flow allows account linking interruption handling on login.
     */
    override fun login(
        email: String,
        password: String,
        onLogin: () -> Unit,
        onLoginIdentifierExists: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.login(email, password)
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onLogin()
                }

                AuthState.ERROR -> {
                    onFailedWith(authResponse.toDisplayError())
                }

                AuthState.INTERRUPTED -> {
                    when (authResponse.cdcResponse().errorCode()) {
                        ResolvableContext.ERR_ENTITY_EXIST_CONFLICT -> {
                            onLoginIdentifierExists()
                        }
                    }
                }
            }
        }
    }
}