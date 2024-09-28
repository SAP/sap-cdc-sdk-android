package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthResolvable
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.model.ConflictingAccountsEntity
import com.sap.cdc.android.sdk.auth.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.example.cdc.model.AccountEntity
import com.sap.cdc.android.sdk.example.cdc.model.ProfileEntity
import com.sap.cdc.android.sdk.example.extensions.splitFullName
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Created by Tal Mirmelshtein on 20/06/2024
 * Copyright: SAP LTD.
 */

/**
 * Authentication view model interface.
 */
interface IViewModelAuthentication {

    fun validSession(): Boolean = false

    fun accountInfo(): AccountEntity?

    fun register(
        email: String,
        password: String,
        name: String,
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

    fun getAccountInfo(
        parameters: MutableMap<String, String>? = mutableMapOf(),
        success: () -> Unit, onFailed: (CDCError) -> Unit
    ) {
    }

    fun updateAccountInfoWith(name: String, success: () -> Unit, onFailed: (CDCError) -> Unit) {}

    fun logOut(success: () -> Unit, onFailed: (CDCError) -> Unit) {}

}

/**
 * Authentication view model.
 * View model is relevant to all authentication views.
 */
class ViewModelAuthentication(context: Context) : ViewModelBase(context), IViewModelAuthentication {

    private var accountInfo by mutableStateOf<AccountEntity?>(null)

    override fun accountInfo(): AccountEntity? = accountInfo

    /**
     * Check Identity session state.
     */
    override
    fun validSession(): Boolean = identityService.getSession() != null

    override fun register(
        email: String,
        password: String,
        name: String,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val namePair = name.splitFullName()
            val profileObject =
                json.encodeToJsonElement(
                    mutableMapOf(
                        "firstName" to namePair.first,
                        "lastName" to namePair.second
                    )
                )
            val authResponse = identityService.register(email, password, profileObject.toString())
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

    override fun getAccountInfo(
        parameters: MutableMap<String, String>?,
        success: () -> Unit,
        onFailed: (CDCError) -> Unit
    ) {
        if (!identityService.validSession()) {
            return
        }
        viewModelScope.launch {
            val authResponse = identityService.getAccountInfo()
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    // Deserialize account data.
                    accountInfo =
                        json.decodeFromString<AccountEntity>(authResponse.asJsonString()!!)
                    success()
                }

                else -> {
                    onFailed(authResponse.toDisplayError()!!)
                }
            }
        }
    }

    override fun updateAccountInfoWith(
        name: String,
        success: () -> Unit,
        onFailed: (CDCError) -> Unit
    ) {
        val newName = name.splitFullName()
        val profileObject =
            json.encodeToJsonElement(
                mutableMapOf(
                    "firstName" to newName.first,
                    "lastName" to newName.second
                )
            )
        val parameters = mutableMapOf("profile" to profileObject.toString())
        viewModelScope.launch {
            val setAuthResponse = identityService.setAccountInfo(parameters)
            when (setAuthResponse.state()) {
                AuthState.SUCCESS -> {
                    getAccountInfo(success = success, onFailed = onFailed)
                }

                else -> onFailed(setAuthResponse.toDisplayError()!!)
            }
        }
    }

    override fun logOut(success: () -> Unit, onFailed: (CDCError) -> Unit) {
        viewModelScope.launch {
            val authResponse = identityService.logout()
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    success()
                }

                else -> {
                    onFailed(authResponse.toDisplayError()!!)
                }
            }
        }
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


/**
 * Preview mock view model.
 */
class ViewModelAuthenticationPreview : IViewModelAuthentication {
    override fun accountInfo(): AccountEntity = AccountEntity(
        uid = "1234",
        profile = ProfileEntity(firstName = "John", lastName = "Doe", email = "johndoe@gmail.com")
    )
}

