package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.auth.provider.IPasskeysAuthenticationProvider
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.bitsnbytes.cdc.PasskeysAuthenticationProvider
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

interface ISignInViewModel : ISocialSignInViewModel {

    fun passkeySignIn(
        activity: ComponentActivity,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub.
    }
}

// Mock preview class for the SignInViewModel
class SignInViewModelPreview : ISignInViewModel

class SignInViewModel(context: Context) : SocialSignInViewModel(context), ISignInViewModel {

    private var passkeysAuthenticationProvider: IPasskeysAuthenticationProvider? = null

    override fun passkeySignIn(
        activity: ComponentActivity,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        if (passkeysAuthenticationProvider == null) {
            passkeysAuthenticationProvider = PasskeysAuthenticationProvider(WeakReference(activity))
        }
        viewModelScope.launch {
            val authResponse = identityService.passkeySignIn(passkeysAuthenticationProvider!!)
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    // Handle success.
                    onLogin()
                }

                else -> {
                    onFailedWith(authResponse.cdcResponse().toCDCError())
                }
            }
        }
    }
}