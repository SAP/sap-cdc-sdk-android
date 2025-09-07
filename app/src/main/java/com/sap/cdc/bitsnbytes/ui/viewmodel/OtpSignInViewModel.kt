package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.auth.flow.AuthCallbacks
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.view.screens.OTPType
import kotlinx.coroutines.launch

interface IOtpSignInViewModel {

    fun signIn(
        otpType: OTPType,
        inputField: String,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        //Stub
    }

}

// Mocked preview class for OtpSignInViewModel
class OtpSignInViewModelPreview : IOtpSignInViewModel

class OtpSignInViewModel(
    context: Context,
    val authenticationFlowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), IOtpSignInViewModel {

    override fun signIn(otpType: OTPType, inputField: String, authCallbacks: AuthCallbacks.() -> Unit) {
        val parameters = mutableMapOf<String, String>()
        when (otpType) {
            OTPType.PHONE -> {
                parameters["phoneNumber"] = inputField
            }

            OTPType.Email -> {
                parameters["email"] = inputField
            }
        }
        viewModelScope.launch {
            authenticationFlowDelegate.otpSendCode(
                parameters,
                authCallbacks
            )
        }

    }
}