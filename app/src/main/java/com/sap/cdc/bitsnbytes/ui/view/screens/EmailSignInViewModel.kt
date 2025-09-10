package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.Credentials
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.viewmodel.BaseViewModel
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
    private val flowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), IEmailSignInViewModel {

    override fun getSaptchaToken(
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        viewModelScope.launch {
            flowDelegate.getSaptchaToken {
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
            flowDelegate.login(
                credentials = credentials,
                authCallbacks = authCallbacks
            )
        }
    }
}
