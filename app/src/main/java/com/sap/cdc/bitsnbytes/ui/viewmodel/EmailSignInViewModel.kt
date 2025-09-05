package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.feature.auth.AuthState
import com.sap.cdc.android.sdk.feature.auth.flow.AuthCallbacks
import com.sap.cdc.android.sdk.feature.auth.model.Credentials
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import kotlinx.coroutines.launch

/**
 * Created by Tal Mirmelshtein on 17/12/2024
 * Copyright: SAP LTD.
 */

interface IEmailSignInViewModel {

    fun getSaptchaToken(
        token: (String) -> Unit,
        onFailedWith: (CDCError?) -> Unit,
    ) {
        //Stub
    }

    fun login(
        credentials: Credentials,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        // Stub
    }
}

// Mock preview class for the EmailSignInViewModel
class EmailSignInViewModelPreview : IEmailSignInViewModel

class EmailSignInViewModel(
    context: Context,
    private val authenticationFlowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), IEmailSignInViewModel {

    override fun getSaptchaToken(
        token: (String) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.getSaptchaToken()
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    token(authResponse.cdcResponse().stringField("saptchaToken") as String)
                }

                else -> {
                    onFailedWith(authResponse.toDisplayError())
                }
            }
        }
    }

    override fun login(
        credentials: Credentials,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        viewModelScope.launch {
            authenticationFlowDelegate.cdc.login(
                credentials = credentials,
                authCallbacks = authCallbacks
            )
        }
    }
}
