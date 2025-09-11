package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
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

// Mocked preview class for OtpSignInViewModel
class OtpSignInViewModelPreview : IOtpSignInViewModel