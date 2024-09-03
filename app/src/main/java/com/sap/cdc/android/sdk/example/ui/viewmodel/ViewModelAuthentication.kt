package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthResolvable
import com.sap.cdc.android.sdk.auth.AuthResponse
import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.core.api.model.CDCError
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Created by Tal Mirmelshtein on 20/06/2024
 * Copyright: SAP LTD.
 */

/**
 * Authentication view model interface.
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
        onPendingRegistration: (IAuthResponse?) -> Unit,
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

    fun resolvePendingRegistrationWithMissingProfileFields(
        map: MutableMap<String, String>,
        regToken: String,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {

    }

}

/**
 * Preview mock view model.
 */
class ViewModelAuthenticationPreview : IViewModelAuthentication

/**
 * Authentication view model.
 * View model is relevant to all authentication views.
 */
class ViewModelAuthentication(context: Context) : ViewModelBase(context), IViewModelAuthentication {

    override fun register(
        email: String,
        password: String,
        onLogin: () -> Unit,
        onFailed: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.register(email, password)
            if (authResponse.toDisplayError() != null) {
                // Error in flow.
                onFailed(authResponse.toDisplayError())
                return@launch
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
            if (authResponse.toDisplayError() != null) {
                // Error in flow
                onFailed(authResponse.toDisplayError())
                return@launch
            }
            onLogin()
        }
    }

    override fun socialSignInWith(
        hostActivity: ComponentActivity,
        provider: IAuthenticationProvider?,
        onLogin: () -> Unit,
        onPendingRegistration: (IAuthResponse?) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        if (provider == null) {
            onFailedWith(CDCError.providerError())
            return
        }
        viewModelScope.launch {
            val authResponse = identityService.nativeSocialSignIn(
                hostActivity, provider
            )
            val error = authResponse.toDisplayError()
            if (error != null) {
                if (authResponse.isResolvable()) {
                    if (error.errorCode == AuthResolvable.ERR_ACCOUNT_PENDING_REGISTRATION) {
                        onPendingRegistration(authResponse!!)
                    }
                } else {
                    // Unresolvable error in flow.
                    onFailedWith(error)
                }
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
            if (authResponse.toDisplayError() != null) {
                // Error in flow
                onFailedWith(authResponse.toDisplayError())
                return@launch
            }
            onLogin()
        }
    }

    override fun getAuthenticationProvider(name: String): IAuthenticationProvider? {
        return identityService.getAuthenticationProvider(name)
    }

    override fun resolvePendingRegistrationWithMissingProfileFields(
        map: MutableMap<String, String>,
        regToken: String,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val jsonMap = mutableMapOf<String, JsonPrimitive>()
            map.forEach { (key, value) ->
                jsonMap[key] = JsonPrimitive(value)
            }
            val authResponse = identityService.resolvePendingRegistrationWithMissingFields(
                "profile", JsonObject(jsonMap).toString(), regToken,
            )
            if (authResponse.toDisplayError() != null) {
                // Error in flow
                onFailedWith(authResponse.toDisplayError())
                return@launch
            }
            onLogin()
        }

    }

}

