package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.LinkingContext
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

interface ILinkAccountViewModel {

    fun linkToSiteAccount(
        loginId: String, password: String,
        linkingContext: LinkingContext,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        //Stub
    }

    fun linkToSocialProvider(
        hostActivity: ComponentActivity,
        provider: String,
        linkingContext: LinkingContext,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        //Stub
    }

}

// Mocked preview class for LinkAccountViewModel
class LinkAccountViewModelPreview : ILinkAccountViewModel

class LinkAccountViewModel(context: Context, val flowDelegate: AuthenticationFlowDelegate) : BaseViewModel(context),
    ILinkAccountViewModel {

    /**
     * Resolve link account interruption with credentials input.
     */
    override fun linkToSiteAccount(
        loginId: String, password: String,
        linkingContext: LinkingContext,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        viewModelScope.launch {
            flowDelegate.linkToSiteAccount(
                loginId,
                password,
                linkingContext,
                authCallbacks
            )
        }
    }


    /**
     * Resolve link account interruption to social account.
     */
    override fun linkToSocialProvider(
        hostActivity: ComponentActivity,
        provider: String,
        linkingContext: LinkingContext,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        viewModelScope.launch {
            flowDelegate.linkToSocialProvider(
                hostActivity,
                provider,
                linkingContext,
                authCallbacks
            )
        }
    }


}