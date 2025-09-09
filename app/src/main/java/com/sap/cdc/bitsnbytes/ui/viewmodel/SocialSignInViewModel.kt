package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.feature.auth.IAuthResponse
import com.sap.cdc.android.sdk.feature.provider.IAuthenticationProvider

interface ISocialSignInViewModel {

    fun getAuthenticationProvider(name: String): IAuthenticationProvider? {
        return null
    }

    fun socialSignInWith(
        hostActivity: ComponentActivity,
        provider: String,
        authenticationProvider: IAuthenticationProvider?,
        onLogin: () -> Unit,
        onPendingRegistration: (IAuthResponse?) -> Unit,
        onLoginIdentifierExists: (IAuthResponse?) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

}

open class SocialSignInViewModel(context: Context) : BaseViewModel(context), IRegisterViewModel {

    /**
     * Helper method to fetch a registered authentication provider.
     */
    override fun getAuthenticationProvider(name: String): IAuthenticationProvider? {
        return identityService.getAuthenticationProvider(name)
    }

    /**
     * Social sign in flow.
     * ViewModel example flow allows both account linking & pending registration interruption handling.
     */
    override fun socialSignInWith(
        hostActivity: ComponentActivity,
        provider: String,
        authenticationProvider: IAuthenticationProvider?,
        onLogin: () -> Unit,
        onPendingRegistration: (IAuthResponse?) -> Unit,
        onLoginIdentifierExists: (IAuthResponse?) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        if (authenticationProvider == null) {
            onFailedWith(CDCError.providerError())
            return
        }
//        viewModelScope.launch {
//            var authResponse: IAuthResponse?
//            authResponse = if (authenticationProvider is WebAuthenticationProvider) {
//                identityService.webSocialSignIn(
//                    hostActivity, provider
//                )
//            } else
//                identityService.nativeSocialSignIn(
//                    hostActivity, authenticationProvider
//                )
//            when (authResponse.state()) {
//                AuthState.SUCCESS -> {
//                    onLogin()
//                }
//
//                AuthState.ERROR -> {
//                    // Error in flow.
//                    onFailedWith(authResponse.toDisplayError())
//                }
//
//                AuthState.INTERRUPTED -> {
//                    // Handle available interruption.
//                    when (authResponse.cdcResponse().errorCode()) {
//                        ResolvableContext.ERR_ACCOUNT_PENDING_REGISTRATION -> {
//                            onPendingRegistration(authResponse)
//                        }
//
//                        ResolvableContext.ERR_ENTITY_EXIST_CONFLICT -> {
//                            onLoginIdentifierExists(authResponse)
//                        }
//                    }
//                }
//            }
//        }
    }

}