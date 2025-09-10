package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.TwoFactorContext
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

interface IPhoneVerificationViewModel {

    fun verifyCode(
        verificationCode: String,
        rememberDevice: Boolean,
        twoFactorContext: TwoFactorContext,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        // Stub
    }
}

class PhoneVerificationViewModel(context: Context, val flowDelegate: AuthenticationFlowDelegate) :
    BaseViewModel(context), IPhoneVerificationViewModel {

    override fun verifyCode(
        verificationCode: String,
        rememberDevice: Boolean,
        twoFactorContext: TwoFactorContext,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        viewModelScope.launch {
            flowDelegate.verifyPhoneCode(
                verificationCode = verificationCode,
                rememberDevice = rememberDevice,
                twoFactorContext = twoFactorContext,
                authCallbacks = authCallbacks
            )
        }
    }
}

// Mock preview class for the PhoneVerificationViewModel
class PhoneVerificationViewModelPreview : IPhoneVerificationViewModel