package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.authentication.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.example.cdc.IdentityServiceRepository
import kotlinx.coroutines.launch


/**
 * Created by Tal Mirmelshtein on 18/06/2024
 * Copyright: SAP LTD.
 */

abstract class ASocialSelectionViewModel(context: Context) : ViewModelBase(context) {

    abstract fun socialSignInWith(
        hostActivity: ComponentActivity,
        provider: IAuthenticationProvider,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    )

    abstract fun socialWebSignInWith(
        hostActivity: ComponentActivity,
        provider: String,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    )
}

class SocialSelectionViewModelModel(context: Context) : ASocialSelectionViewModel(context) {

    private val identityService: IdentityServiceRepository =
        IdentityServiceRepository.getInstance(context)

    override fun socialSignInWith(
        hostActivity: ComponentActivity,
        provider: IAuthenticationProvider,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.nativeSocialSignIn(
                hostActivity, provider
            )
            if (authResponse.authenticationError() != null) {
                // Error in account request.
                onFailedWith(authResponse.authenticationError())
                return@launch
            }
            onLogin()
        }
    }

    override fun socialWebSignInWith(
        hostActivity: ComponentActivity,
        provider: String,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.webSocialSignIn(
                hostActivity, provider
            )
            if (authResponse.authenticationError() != null) {
                // Error in account request.
                onFailedWith(authResponse.authenticationError())
                return@launch
            }
            onLogin()
        }
    }
}

class SocialSelectionViewModelPreview(context: Context) : ASocialSelectionViewModel(context) {

    override fun socialSignInWith(
        hostActivity: ComponentActivity,
        provider: IAuthenticationProvider,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        // Stub.
    }

    override fun socialWebSignInWith(
        hostActivity: ComponentActivity,
        provider: String,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        // Stub.
    }

}