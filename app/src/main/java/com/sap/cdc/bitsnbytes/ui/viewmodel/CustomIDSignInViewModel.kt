package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.auth.flow.AuthCallbacks
import com.sap.cdc.android.sdk.feature.auth.model.CustomIdCredentials
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import kotlinx.coroutines.launch

interface ICustomIDSignInViewModel {

    companion object {
        const val CUSTOM_ID_PREFIX = "gigya.com/identifiers/customIdentifiers/"
    }

    fun login(
        identifier: String,
        identifierType: String,
        password: String,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        // Stub
    }
}

class CustomIDSignInViewModelPreview : ICustomIDSignInViewModel

class CustomIDSignInViewModel(
    context: Context,
    private val authenticationFlowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), ICustomIDSignInViewModel {


    override fun login(
        identifier: String,
        identifierType: String,
        password: String,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        viewModelScope.launch {
            val credentials = CustomIdCredentials(
                identifier = identifier,
                identifierType = ICustomIDSignInViewModel.CUSTOM_ID_PREFIX + identifierType,
                password = password
            )
            authenticationFlowDelegate.loginWithCustomId(
                credentials = credentials,
                authCallbacks = authCallbacks
            )
        }
    }
}