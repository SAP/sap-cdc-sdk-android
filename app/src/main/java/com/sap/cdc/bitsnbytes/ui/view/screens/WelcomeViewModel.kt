package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

interface IWelcomeViewModel {

    fun singleSignOn(
        hostActivity: ComponentActivity,
        parameters: MutableMap<String, String>?,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        //Stub
    }

}

class WelcomeViewModel(context: Context, val flowDelegate: AuthenticationFlowDelegate) : BaseViewModel(context),
    IWelcomeViewModel {

    /**
     * Single sign on provider flow.
     */
    override fun singleSignOn(
        hostActivity: ComponentActivity,
        parameters: MutableMap<String, String>?,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        viewModelScope.launch {
            flowDelegate.singleSignOn(
                hostActivity = hostActivity,
                parameters = parameters,
                authCallbacks = authCallbacks
            )
        }
    }
}

// Mock preview class for the WelcomeViewModel
class WelcomeViewModelPreview : IWelcomeViewModel