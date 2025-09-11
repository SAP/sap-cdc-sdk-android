package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

interface IMyProfileViewModel {

    val flowDelegate: AuthenticationFlowDelegate?
        get() = null


    fun getAccountInfo(
        parameters: MutableMap<String, String>? = mutableMapOf(),
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        // Stub
    }

    fun logOut(authCallbacks: AuthCallbacks.() -> Unit = {}) {
        //Stub
    }

}

class MyProfileViewModel(context: Context, override val flowDelegate: AuthenticationFlowDelegate) :
    BaseViewModel(context), IMyProfileViewModel {

    override fun getAccountInfo(
        parameters: MutableMap<String, String>?,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        viewModelScope.launch {
            // Delegate to AuthenticationFlowDelegate which manages state
            flowDelegate.getAccountInfo(parameters = parameters ?: mutableMapOf(), authCallbacks = authCallbacks)
        }
    }

    /**
     * Log out of current session.
     */
    override fun logOut(authCallbacks: AuthCallbacks.() -> Unit) {
        viewModelScope.launch {
            flowDelegate.logOut(authCallbacks = authCallbacks)
        }
    }
}

// Mock preview class for the MyProfileViewModel
class MyProfileViewModelPreview : IMyProfileViewModel {

}