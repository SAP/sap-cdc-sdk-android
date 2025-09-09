package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.core.api.model.CDCError

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

// Mock preview class for the WelcomeViewModel
class WelcomeViewModelPreview: IWelcomeViewModel

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
//        viewModelScope.launch {
//            val authResponse = identityService.sso(hostActivity, parameters ?: mutableMapOf())
//            when (authResponse.state()) {
//                AuthState.SUCCESS -> {
//                    onLogin()
//                }
//
//                else -> {
//                    onFailedWith(authResponse.toDisplayError())
//                }
//            }
//        }
    }
}