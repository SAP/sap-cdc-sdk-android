package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.core.api.model.CDCError
import kotlinx.coroutines.launch

/**
 * Created by Tal Mirmelshtein on 20/06/2024
 * Copyright: SAP LTD.
 */

interface IViewModelAuthentication {

    fun register(
        email: String,
        password: String,
        onLogin: () -> Unit,
        onFailed: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun login(
        email: String,
        password: String,
        onLogin: () -> Unit,
        onFailed: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun socialSignInWith(
        hostActivity: ComponentActivity,
        provider: IAuthenticationProvider?,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun socialWebSignInWith(
        hostActivity: ComponentActivity,
        provider: String,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun getAuthenticationProvider(name: String): IAuthenticationProvider? {
        return null
    }
}

class ViewModelAuthentication(context: Context) : ViewModelBase(context), IViewModelAuthentication {

    override fun register(
        email: String,
        password: String,
        onLogin: () -> Unit,
        onFailed: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.register(email, password)
            if (authResponse.authenticationError() != null) {
                // Error in flow
                onFailed(authResponse.authenticationError())
            }
            onLogin()
        }
    }

    override fun login(
        email: String,
        password: String,
        onLogin: () -> Unit,
        onFailed: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.login(email, password)
            if (authResponse.authenticationError() != null) {
                // Error in flow
                onFailed(authResponse.authenticationError())
                return@launch
            }
            onLogin()
        }
    }

    override fun socialSignInWith(
        hostActivity: ComponentActivity,
        provider: IAuthenticationProvider?,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        if(provider == null) {
            onFailedWith(CDCError.providerError())
            return
        }
        viewModelScope.launch {
            val authResponse = identityService.nativeSocialSignIn(
                hostActivity, provider
            )
            if (authResponse.authenticationError() != null) {
                // Error in flow
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
                // Error in flow
                onFailedWith(authResponse.authenticationError())
                return@launch
            }
            onLogin()
        }
    }

    override fun getAuthenticationProvider(name: String): IAuthenticationProvider? {
        return identityService.getAuthenticationProvider(name)
    }
}

/**
 * Preview mock view model.
 */
class ViewModelAuthenticationPreview : IViewModelAuthentication

