package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.bitsnbytes.extensions.toJson
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.state.OtpSignInNavigationEvent
import com.sap.cdc.bitsnbytes.ui.state.OtpSignInState
import com.sap.cdc.bitsnbytes.ui.view.model.Country
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface IOtpSignInViewModel {
    val state: StateFlow<OtpSignInState>
    val navigationEvents: SharedFlow<OtpSignInNavigationEvent>

    fun updateInputField(value: String) {}
    fun updateSelectedCountry(country: Country) {}
    fun onSignIn(otpType: OTPType)
}

class OtpSignInViewModel(
    context: Context,
    val authenticationFlowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), IOtpSignInViewModel {

    private val _state = MutableStateFlow(OtpSignInState())
    override val state: StateFlow<OtpSignInState> = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<OtpSignInNavigationEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    override val navigationEvents: SharedFlow<OtpSignInNavigationEvent> = _navigationEvents.asSharedFlow()

    override fun updateInputField(value: String) {
        _state.update { it.copy(inputField = value) }
    }

    override fun updateSelectedCountry(country: Country) {
        _state.update { it.copy(selectedCountry = country) }
    }

    override fun onSignIn(otpType: OTPType) {
        val currentState = _state.value
        
        // For phone numbers, combine country code with the number
        val finalInputField = if (otpType == OTPType.PHONE) {
            "${currentState.selectedCountry.dialCode}${currentState.inputField}"
        } else {
            currentState.inputField
        }
        
        _state.update { it.copy(isLoading = true, error = null) }
        
        val parameters = mutableMapOf<String, String>()
        when (otpType) {
            OTPType.PHONE -> {
                parameters["phoneNumber"] = finalInputField
            }
            OTPType.Email -> {
                parameters["email"] = finalInputField
            }
        }
        
        viewModelScope.launch {
            authenticationFlowDelegate.otpSendCode(parameters) {
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    _navigationEvents.tryEmit(
                        OtpSignInNavigationEvent.NavigateToOtpVerify(
                            otpContext = "",
                            otpType = otpType.value,
                            inputField = finalInputField
                        )
                    )
                }

                onOTPRequired = { otpContext ->
                    _state.update { it.copy(isLoading = false) }
                    _navigationEvents.tryEmit(
                        OtpSignInNavigationEvent.NavigateToOtpVerify(
                            otpContext = otpContext.toJson(),
                            otpType = otpType.value,
                            inputField = finalInputField
                        )
                    )
                }
                
                onError = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            }
        }
    }
}

// Mocked preview class for OtpSignInViewModel
class OtpSignInViewModelPreview : IOtpSignInViewModel {
    override val state: StateFlow<OtpSignInState> = MutableStateFlow(OtpSignInState()).asStateFlow()
    override val navigationEvents: SharedFlow<OtpSignInNavigationEvent> = MutableSharedFlow<OtpSignInNavigationEvent>().asSharedFlow()
    
    override fun onSignIn(otpType: OTPType) {}
}
