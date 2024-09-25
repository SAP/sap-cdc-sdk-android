package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthResolvable
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.model.ConflictingAccountsEntity
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
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun login(
        email: String,
        password: String,
        onLogin: () -> Unit,
        onLoginIdentifierExists: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun socialSignInWith(
        hostActivity: ComponentActivity,
        provider: IAuthenticationProvider?,
        onLogin: () -> Unit,
        onPendingRegistration: (IAuthResponse?) -> Unit,
        onLoginIdentifierExists: (regToken: String, conflictingAccounts: ConflictingAccountsEntity) -> Unit,
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
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.register(email, password)
            // Check response state for flow success/error/continuation.
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onLogin()
                }

                else -> {
                    onFailedWith(authResponse.toDisplayError())
                }
            }
        }
    }

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
                        AuthResolvable.ERR_LOGIN_IDENTIFIER_EXISTS -> {
                            onLoginIdentifierExists()
                        }
                    }
                }
            }
        }
    }

    override fun socialSignInWith(
        hostActivity: ComponentActivity,
        provider: IAuthenticationProvider?,
        onLogin: () -> Unit,
        onPendingRegistration: (IAuthResponse?) -> Unit,
        onLoginIdentifierExists: (regToken: String, conflictingAccounts: ConflictingAccountsEntity) -> Unit,
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
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onLogin()
                }

                AuthState.ERROR -> {
                    // Error in flow.
                    onFailedWith(authResponse.toDisplayError())
                }

                AuthState.INTERRUPTED -> {
                    // Handle available interruption.
                    when (authResponse.cdcResponse().errorCode()) {
                        AuthResolvable.ERR_ACCOUNT_PENDING_REGISTRATION -> {
                            onPendingRegistration(authResponse)
                        }

                        AuthResolvable.ERR_LOGIN_IDENTIFIER_EXISTS -> {
                            val regToken = authResponse.cdcResponse().stringField("regToken")
                            if (regToken == null) {
                                onFailedWith(authResponse.toDisplayError())
                                return@launch
                            }
                            val conflictingAccounts =
                                identityService.getConflictingAccounts(regToken)
                            onLoginIdentifierExists(regToken, conflictingAccounts)
                        }
                    }
                }
            }
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
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onLogin()
                }

                else -> {
                    onFailedWith(authResponse.toDisplayError())
                }
            }
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
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onLogin()
                }

                else -> {
                    onFailedWith(authResponse.toDisplayError())
                }
            }
        }
    }

}

