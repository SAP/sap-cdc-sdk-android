package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.TwoFactorContext
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.state.TOTPVerificationNavigationEvent
import com.sap.cdc.bitsnbytes.ui.state.TOTPVerificationState
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface ITOTPVerificationViewModel {
    val state: StateFlow<TOTPVerificationState>
    val navigationEvents: SharedFlow<TOTPVerificationNavigationEvent>
    val twoFactorContext: StateFlow<TwoFactorContext?>
    val qACode: StateFlow<Bitmap?>

    fun updateTwoFactorContext(newContext: TwoFactorContext)
    fun updateOtpValue(value: String)
    fun onRegisterNewAuthenticatorApp()
    fun onVerifyCode()
}

class TOTPVerificationViewModel(context: Context, val flowDelegate: AuthenticationFlowDelegate) :
    BaseViewModel(context), ITOTPVerificationViewModel {

    private val _state = MutableStateFlow(TOTPVerificationState())
    override val state: StateFlow<TOTPVerificationState> = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<TOTPVerificationNavigationEvent>(
        replay = 1,
        extraBufferCapacity = 0
    )
    override val navigationEvents: SharedFlow<TOTPVerificationNavigationEvent> = _navigationEvents.asSharedFlow()

    private val _twoFactorContext = MutableStateFlow<TwoFactorContext?>(null)
    override val twoFactorContext: StateFlow<TwoFactorContext?> = _twoFactorContext

    private val _qACode = MutableStateFlow<Bitmap?>(null)
    override val qACode: StateFlow<Bitmap?> = _qACode

    override fun updateTwoFactorContext(newContext: TwoFactorContext) {
        _twoFactorContext.value = newContext
    }

    override fun updateOtpValue(value: String) {
        _state.update { it.copy(otpValue = value) }
    }

    override fun onRegisterNewAuthenticatorApp() {
        _state.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            flowDelegate.registerNewAuthenticatorApp(twoFactorContext = twoFactorContext.value!!) {
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                }

                doOnTwoFactorContextUpdated { twoFactorContext ->
                    val bitmap = decodeImage(twoFactorContext.qrCode!!)
                    _qACode.value = bitmap
                    _state.update { it.copy(isLoading = false) }
                }

                onError = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            }
        }
    }

    override fun onVerifyCode() {
        _state.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            flowDelegate.verifyTotpCode(
                verificationCode = _state.value.otpValue,
                rememberDevice = false,
                twoFactorContext = twoFactorContext.value!!
            ) {
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    _navigationEvents.tryEmit(TOTPVerificationNavigationEvent.NavigateToMyProfile)
                }

                onError = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            }
        }
    }

    private fun decodeImage(encodedImage: String): Bitmap {
        val decoded = Base64.decode(encodedImage.split(",".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()[1], Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
    }
}

// Mock preview class for the TOTPVerificationViewModel
class TOTPVerificationViewModelPreview : ITOTPVerificationViewModel {
    override val state: StateFlow<TOTPVerificationState> = MutableStateFlow(TOTPVerificationState()).asStateFlow()
    override val navigationEvents: SharedFlow<TOTPVerificationNavigationEvent> = 
        MutableSharedFlow<TOTPVerificationNavigationEvent>().asSharedFlow()
    override val twoFactorContext: StateFlow<TwoFactorContext?> = MutableStateFlow(TwoFactorContext())
    override val qACode: StateFlow<Bitmap?> = MutableStateFlow(null)
    
    override fun updateTwoFactorContext(newContext: TwoFactorContext) {}
    override fun updateOtpValue(value: String) {}
    override fun onRegisterNewAuthenticatorApp() {}
    override fun onVerifyCode() {}
}
