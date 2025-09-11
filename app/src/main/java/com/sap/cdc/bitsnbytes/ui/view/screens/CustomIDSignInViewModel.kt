package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.CustomIdCredentials
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.view.screens.ICustomIDSignInViewModel.Companion.CUSTOM_ID_PREFIX
import com.sap.cdc.bitsnbytes.ui.view.screens.ICustomIDSignInViewModel.Companion.IDENTIFIER_TYPE_ALIAS
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

interface ICustomIDSignInViewModel {

    companion object {
        const val CUSTOM_ID_PREFIX = "gigya.com/identifiers/customIdentifiers/"
        const val IDENTIFIER_TYPE_ALIAS = "alias"
    }

    fun login(
        identifier: String,
        password: String,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        // Stub
    }
}

class CustomIDSignInViewModelPreview : ICustomIDSignInViewModel

class CustomIDSignInViewModel(
    context: Context,
    private val flowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), ICustomIDSignInViewModel {


    override fun login(
        identifier: String,
        password: String,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        viewModelScope.launch {
            val credentials = CustomIdCredentials(
                identifier = identifier,
                identifierType = CUSTOM_ID_PREFIX + IDENTIFIER_TYPE_ALIAS,
                password = password
            )
            flowDelegate.loginWithCustomId(
                credentials = credentials,
                authCallbacks = authCallbacks
            )
        }
    }
}