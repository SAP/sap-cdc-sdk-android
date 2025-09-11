package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.provider.IAuthenticationProvider
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

interface IRegisterViewModel {

    fun getAuthenticationProvider(name: String): IAuthenticationProvider? {
        return null
    }

    fun socialSignInWith(
        hostActivity: ComponentActivity,
        provider: String,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        //Stub
    }
}

// Mock preview class for the RegisterViewModel
class RegisterViewModelPreview : IRegisterViewModel

class RegisterViewModel(context: Context, val flowDelegate: AuthenticationFlowDelegate) :
    BaseViewModel(context),
    IRegisterViewModel {

    /**
     * Helper method to fetch a registered authentication provider.
     */
    override fun getAuthenticationProvider(name: String): IAuthenticationProvider? {
        return flowDelegate.getAuthenticationProvider(name)
    }

    /**
     * Social sign in flow.
     * ViewModel example flow allows both account linking & pending registration interruption handling.
     */
    override fun socialSignInWith(
        hostActivity: ComponentActivity,
        provider: String,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        viewModelScope.launch {
            flowDelegate.signInWithProvider(
                hostActivity = hostActivity,
                provider = provider,
                authCallbacks = authCallbacks
            )
        }
    }

}
