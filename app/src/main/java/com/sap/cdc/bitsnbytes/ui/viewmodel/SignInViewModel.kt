package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.provider.passkey.IPasskeysAuthenticationProvider
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.feature.provider.PasskeysAuthenticationProvider
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

interface ISignInViewModel: ISocialSignInViewModel {

    fun passkeyLogin(
        activity: ComponentActivity,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        //Stub.
    }
}

// Mock preview class for the SignInViewModel
class SignInViewModelPreview : ISignInViewModel

class SignInViewModel(context: Context, val authenticationFlowDelegate: AuthenticationFlowDelegate) :
    SocialSignInViewModel(context), ISignInViewModel {

    private var passkeysAuthenticationProvider: IPasskeysAuthenticationProvider? = null

    override fun passkeyLogin(
        activity: ComponentActivity,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        if (passkeysAuthenticationProvider == null) {
            passkeysAuthenticationProvider = PasskeysAuthenticationProvider(WeakReference(activity))
        }
        viewModelScope.launch {
            authenticationFlowDelegate.passkeyLogin(
                provider = passkeysAuthenticationProvider!!,
                authCallbacks = authCallbacks
            )
        }
    }
}