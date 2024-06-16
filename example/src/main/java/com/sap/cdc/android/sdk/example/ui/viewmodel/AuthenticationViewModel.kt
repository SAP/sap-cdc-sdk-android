package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.authentication.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.example.cdc.IdentityServiceRepository
import com.sap.cdc.android.sdk.example.cdc.model.AccountEntity
import com.sap.cdc.android.sdk.sceensets.WebBridgeJS
import com.sap.cdc.android.sdk.session.api.model.CDCError
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json.Default.decodeFromString

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

/**
 * Abstract authentication view model. Created to add Compose preview interface.
 */
abstract class IAuthenticationViewModel : ViewModel() {

    var lastName by mutableStateOf("")
        internal set

    var firstName by mutableStateOf("")
        internal set

    abstract fun register(
        email: String,
        password: String,
        onLogin: () -> Unit,
        onFailed: (CDCError?) -> Unit
    )

    abstract fun getAccountInfo()

    abstract fun setAccountInfo(parameters: MutableMap<String, String>)

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

    abstract fun getWebBridgeJS(): WebBridgeJS?
}

/**
 * Main authentication view model interface.
 */
class AuthenticationViewModel(context: Context) : IAuthenticationViewModel() {

    private val identityService: IdentityServiceRepository =
        IdentityServiceRepository.getInstance(context)

    /**
     * Initiate credentials registration flow.
     */
    override fun register(
        email: String,
        password: String,
        onLogin: () -> Unit,
        onFailed: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.register(email, password)
            if (authResponse.authenticationError() != null) {
                // Error in account request.
                //TODO: Add error handling.
                onFailed(authResponse.authenticationError())
                return@launch
            }
            onLogin()
        }
    }

    /**
     * Request account information.
     */
    override fun getAccountInfo() {
        viewModelScope.launch {
            val authResponse = identityService.getAccountInfo()
            if (authResponse.authenticationError() != null) {
                // Error in account request.
                //TODO: Add error handling.
                return@launch
            }
            // Deserialize account data.
            val account = decodeFromString<AccountEntity>(authResponse.authenticationJson()!!)

            // Update UI stateful parameters.
            firstName = account.profile.firstName
            lastName = account.profile.lastName
        }
    }

    override fun setAccountInfo(parameters: MutableMap<String, String>) {
        TODO("Not yet implemented")
    }

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
                //TODO: Add error handling.
                return@launch
            }
            // Deserialize account data.
            val account = decodeFromString<AccountEntity>(authResponse.authenticationJson()!!)
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
                //TODO: Add error handling.
                return@launch
            }
            // Deserialize account data.
            val account = decodeFromString<AccountEntity>(authResponse.authenticationJson()!!)
        }
    }

    override fun getWebBridgeJS(): WebBridgeJS {
        return identityService.getWebBridge()
    }


}

/**
 * Compose preview mock view model interface.
 */
class AuthenticationViewModelPreviewMock() : IAuthenticationViewModel() {

    init {
        firstName = "John"
        lastName = "Doe"
    }

    override fun register(
        email: String,
        password: String,
        onLogin: () -> Unit,
        onFailed: (CDCError?) -> Unit
    ) {
        // Stub.
    }

    override fun getAccountInfo() {
        // Stub.
    }

    override fun setAccountInfo(parameters: MutableMap<String, String>) {
        // Stub.
    }

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

    override fun getWebBridgeJS(): WebBridgeJS? {
        return null
    }


}