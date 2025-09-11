package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.TwoFactorContext
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface ITOTPVerificationViewModel {

    val twoFactorContext: StateFlow<TwoFactorContext?>
    val qACode: StateFlow<Bitmap?>

    fun updateTwoFactorContext(newContext: TwoFactorContext) {
        // Stub
    }

    fun registerNewAuthenticatorApp(
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        //Stub
    }

    fun verifyCode(
        verificationCode: String,
        rememberDevice: Boolean,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        // Stub
    }

}

class TOTPVerificationViewModel(context: Context, val flowDelegate: AuthenticationFlowDelegate) :
    BaseViewModel(context), ITOTPVerificationViewModel {

    private val _twoFactorContext = MutableStateFlow<TwoFactorContext?>(null)
    override val twoFactorContext: StateFlow<TwoFactorContext?> = _twoFactorContext

    private val _qACode = MutableStateFlow<Bitmap?>(null)
    override val qACode: StateFlow<Bitmap?> = _qACode

    override fun updateTwoFactorContext(newContext: TwoFactorContext) {
        _twoFactorContext.value = newContext
    }

    override fun registerNewAuthenticatorApp(
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        viewModelScope.launch {
            flowDelegate.registerNewAuthenticatorApp(
                twoFactorContext = twoFactorContext.value!!
            ) {

                authCallbacks()

                doOnTwoFactorContextUpdated { twoFactorContext ->
                    val bitmap = decodeImage(twoFactorContext.qrCode!!)
                    _qACode.value = bitmap

                }
            }
        }
    }

    override fun verifyCode(
        verificationCode: String,
        rememberDevice: Boolean,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        viewModelScope.launch {
            flowDelegate.verifyTotpCode(
                verificationCode = verificationCode,
                rememberDevice = rememberDevice,
                twoFactorContext = twoFactorContext.value!!,
                authCallbacks = authCallbacks
            )
        }
    }

    private fun decodeImage(encodedImage: String): Bitmap {
        // Decoding the image received (Base64).
        val decoded = Base64.decode(encodedImage.split(",".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()[1], Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
    }
}

// Mock preview class for the TOTPVerificationViewModel
class TOTPVerificationViewModelPreview : ITOTPVerificationViewModel {
    override val twoFactorContext: StateFlow<TwoFactorContext?>
        get() = MutableStateFlow(TwoFactorContext())

    override val qACode: StateFlow<Bitmap?>
        get() = MutableStateFlow(null)
}