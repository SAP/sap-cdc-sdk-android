package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.auth.ResolvableContext
import com.sap.cdc.android.sdk.core.api.model.CDCError
import kotlinx.coroutines.launch

interface ILinkAccountViewModel {

    fun resolveLinkToSiteAccount(
        loginId: String, password: String,
        resolvableContext: ResolvableContext,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun resolveLinkToSocialAccount(
        hostActivity: ComponentActivity,
        provider: String,
        resolvableContext: ResolvableContext,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

}


/**
 * Preview mock view model.
 */
class LinkAccountViewModelPreview : ILinkAccountViewModel {}

class LinkAccountViewModel(context: Context) : BaseViewModel(context), ILinkAccountViewModel {

    /**
     * Resolve link account interruption with credentials input.
     */
    override fun resolveLinkToSiteAccount(
        loginId: String,
        password: String,
        resolvableContext: ResolvableContext,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.resolveLinkToSiteAccount(
                loginId = loginId, password = password, resolvableContext = resolvableContext
            )
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onLogin()
                }

                else -> onFailedWith(authResponse.toDisplayError())
            }
        }
    }


    /**
     * Resolve link account interruption to social account.
     */
    override fun resolveLinkToSocialAccount(
        hostActivity: ComponentActivity,
        provider: String,
        resolvableContext: ResolvableContext,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.resolveLinkToSocialAccount(
                hostActivity,
                identityService.getAuthenticationProvider(provider)!!,
                resolvableContext,
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