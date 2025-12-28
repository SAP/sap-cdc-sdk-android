package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.TwoFactorContext
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.state.PhoneVerificationNavigationEvent
import com.sap.cdc.bitsnbytes.ui.state.PhoneVerificationState
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface IPhoneVerificationViewModel {
    val state: StateFlow<PhoneVerificationState>
    val navigationEvents: SharedFlow<PhoneVerificationNavigationEvent>
    
    fun updateOtpValue(value: String)
    fun onVerifyCode()
    fun onCodeSent()
}

class PhoneVerificationViewModel(context: Context, val flowDelegate: AuthenticationFlowDelegate) :
    BaseViewModel(context), IPhoneVerificationViewModel {

    private val _state = MutableStateFlow(PhoneVerificationState())
    override val state: StateFlow<PhoneVerificationState> = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<PhoneVerificationNavigationEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    override val navigationEvents: SharedFlow<PhoneVerificationNavigationEvent> = _navigationEvents.asSharedFlow()

    private var _twoFactorContext: TwoFactorContext? = null

    fun initializeWithContext(twoFactorContext: TwoFactorContext) {
        _twoFactorContext = twoFactorContext
    }

    override fun updateOtpValue(value: String) {
        _state.update { it.copy(otpValue = value) }
    }

    override fun onCodeSent() {
        _state.update { it.copy(codeSent = true) }
    }

    override fun onVerifyCode() {
        _state.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            flowDelegate.verifyPhoneCode(
                verificationCode = _state.value.otpValue,
                rememberDevice = false,
                twoFactorContext = _twoFactorContext!!
            ) {
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    _navigationEvents.tryEmit(PhoneVerificationNavigationEvent.NavigateToMyProfile)
                }

                onError = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            }
        }
    }
}

// Mock preview class for the PhoneVerificationViewModel
class PhoneVerificationViewModelPreview : IPhoneVerificationViewModel {
    override val state: StateFlow<PhoneVerificationState> = MutableStateFlow(PhoneVerificationState()).asStateFlow()
    override val navigationEvents: SharedFlow<PhoneVerificationNavigationEvent> = 
        MutableSharedFlow<PhoneVerificationNavigationEvent>().asSharedFlow()
    
    override fun updateOtpValue(value: String) {}
    override fun onVerifyCode() {}
    override fun onCodeSent() {}
}
