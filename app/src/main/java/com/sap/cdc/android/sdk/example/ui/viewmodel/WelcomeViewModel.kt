package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.core.api.model.CDCError
import kotlinx.coroutines.launch

interface IWelcomeViewModel {

    fun singleSignOn(
        hostActivity: ComponentActivity,
        parameters: MutableMap<String, String>?,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

}

/**
 * Preview mock view model.
 */
class WelcomeViewModelPreview : IWelcomeViewModel {}

class WelcomeViewModel(context: Context) : BaseViewModel(context), IWelcomeViewModel {

    /**
     * Single sign on provider flow.
     */
    override fun singleSignOn(
        hostActivity: ComponentActivity,
        parameters: MutableMap<String, String>?,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.sso(hostActivity, parameters ?: mutableMapOf())
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