package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.core.api.model.CDCError
import kotlinx.coroutines.launch

interface ISignInViewModel : ISocialSignInViewModel {

    fun passkeySignIn(
        hostActivity: ComponentActivity,
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
        hostActivity: ComponentActivity,
        success: () -> Unit, onFailed: (CDCError) -> Unit,
    ) {
        viewModelScope.launch {
            val authResponse = identityService.passkeySignIn(hostActivity)
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