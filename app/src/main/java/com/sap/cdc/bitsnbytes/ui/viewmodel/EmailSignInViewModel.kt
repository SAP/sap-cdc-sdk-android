package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.auth.model.Credentials
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import kotlinx.coroutines.launch

/**
 * Created by Tal Mirmelshtein on 17/12/2024
 * Copyright: SAP LTD.
 */

interface IEmailSignInViewModel {

    fun getSaptchaToken(
        authCallbacks: AuthCallbacks.() -> Unit,
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
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        viewModelScope.launch {
            authenticationFlowDelegate.getSaptchaToken {
                authCallbacks()

                doOnSuccess { authSuccess ->
                    val token = authSuccess.userData["saptchaToken"] as String
                }
            }
        }
    }

    override fun login(
        credentials: Credentials,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        viewModelScope.launch {
            authenticationFlowDelegate.login(
                credentials = credentials,
                authCallbacks = authCallbacks
            )
        }
    }
}
