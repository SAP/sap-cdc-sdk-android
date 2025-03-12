package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.ResolvableContext
import com.sap.cdc.android.sdk.core.api.model.CDCError
import kotlinx.coroutines.launch

interface ITOTPVerificationViewModel {

    fun registerNewAuthenticatorApp(
        resolvableContext: ResolvableContext,
        onQACode: (IAuthResponse, Bitmap) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun verifyTOTPCode(
        resolvableContext: ResolvableContext,
        code: String,
        onVerificationSuccess: (IAuthResponse) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }
}

// Mock preview class for the TOTPVerificationViewModel
class TOTPVerificationViewModelPreview() : ITOTPVerificationViewModel {

}

class TOTPVerificationViewModel(context: Context) : BaseViewModel(context),
    ITOTPVerificationViewModel {

    override fun registerNewAuthenticatorApp(
        resolvableContext: ResolvableContext,
        onQACode: (IAuthResponse, Bitmap) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.registerNewAuthenticatorApp(resolvableContext)
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onQACode(
                        authResponse,
                        decodeImage(authResponse.resolvable()?.tfa?.qrCode ?: "")
                    )
                }

                AuthState.ERROR, AuthState.INTERRUPTED -> {
                    onFailedWith(authResponse.toDisplayError())
                }
            }

        }
    }

    private fun decodeImage(encodedImage: String): Bitmap {
        // Decoding the image received (Base64).
        val decoded = Base64.decode(encodedImage.split(",".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()[1], Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
    }

    override fun verifyTOTPCode(
        resolvableContext: ResolvableContext,
        code: String,
        onVerificationSuccess: (IAuthResponse) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.verifyTotpCode(
                code, resolvableContext, false,
            )
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onVerificationSuccess(authResponse)
                }

                AuthState.ERROR, AuthState.INTERRUPTED -> {
                    onFailedWith(authResponse.toDisplayError())
                }
            }
        }
    }
}

