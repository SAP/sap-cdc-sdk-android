package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.feature.auth.AuthState
import com.sap.cdc.android.sdk.feature.auth.ResolvableContext
import com.sap.cdc.bitsnbytes.ui.view.screens.OTPType
import kotlinx.coroutines.launch

interface IOtpSignInViewModel {

    fun otpSignIn(
        otpType: OTPType,
        inputField: String,
        success: (ResolvableContext) -> Unit, onFailed: (CDCError) -> Unit
    ) {
        //Stub
    }
}

// Mocked preview class for OtpSignInViewModel
class OtpSignInViewModelPreview : IOtpSignInViewModel

class OtpSignInViewModel(context: Context) : BaseViewModel(context), IOtpSignInViewModel {

    /**
     * Sign in with phone number.
     */
    override fun otpSignIn(
        otpType: OTPType,
        inputField: String,
        success: (ResolvableContext) -> Unit,
        onFailed: (CDCError) -> Unit
    ) {
        viewModelScope.launch {
            val parameters = mutableMapOf<String, String>()
            when (otpType) {
                OTPType.PHONE -> {
                    parameters["phoneNumber"] = inputField
                }

                OTPType.Email -> {
                    parameters["email"] = inputField
                }
            }
            val authResponse = identityService.otpSignIn(parameters)
            when (authResponse.state()) {
                AuthState.INTERRUPTED, AuthState.SUCCESS -> {
                    success(authResponse.resolvable()!!)
                }

                else -> {
                    onFailed(authResponse.toDisplayError()!!)
                }
            }
        }
    }
}