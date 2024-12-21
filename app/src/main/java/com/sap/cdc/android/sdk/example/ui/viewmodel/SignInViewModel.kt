package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.example.cdc.auth.PasskeysAuthenticationProvider
import kotlinx.coroutines.launch

interface ISignInViewModel : ISocialSignInViewModel {

    fun passkeySignIn(
        authenticationProvider: PasskeysAuthenticationProvider,
        success: () -> Unit, onFailed: (CDCError) -> Unit,
    ) {
        //Stub/
    }
}

/**
 * Preview mock view model.
 */
class SignInViewModelPreview : ISignInViewModel {}

class SignInViewModel(context: Context) : SocialSignInViewModel(context), ISignInViewModel {

    override fun passkeySignIn(
        authenticationProvider: PasskeysAuthenticationProvider,        success: () -> Unit, onFailed: (CDCError) -> Unit,
    ) {
        viewModelScope.launch {
            val authResponse = identityService.passkeySignIn(authenticationProvider)
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    success()
                }

                else -> {
                    onFailed(authResponse.toDisplayError()!!)
                }
            }
        }
    }
}