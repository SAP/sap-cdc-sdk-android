package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthError
import com.sap.cdc.android.sdk.feature.ResolvableContext
import com.sap.cdc.bitsnbytes.extensions.toJson
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.state.OtpVerifyNavigationEvent
import com.sap.cdc.bitsnbytes.ui.state.OtpVerifyState
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

interface IOtpVerifyViewModel {
    val state: StateFlow<OtpVerifyState>
    val navigationEvents: SharedFlow<OtpVerifyNavigationEvent>

    fun updateOtpValue(value: String) {}
    fun onVerifyCode(vToken: String)
    fun onResendCode()

    fun startOtpTimer(whenEnded: () -> Unit) {
        // Stub
    }

    fun resolveLoginWithCode(
        code: String,
        resolvable: ResolvableContext,
        onLogin: () -> Unit,
        onFailedWith: (AuthError?) -> Unit
    ) {
        //Stub
    }
}

class OtpVerifyViewModel(
    context: Context,
    val authenticationFlowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), IOtpVerifyViewModel {

    private val _state = MutableStateFlow(OtpVerifyState())
    override val state: StateFlow<OtpVerifyState> = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<OtpVerifyNavigationEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    override val navigationEvents: SharedFlow<OtpVerifyNavigationEvent> = _navigationEvents.asSharedFlow()

    override fun updateOtpValue(value: String) {
        _state.update { it.copy(otpValue = value) }
    }

    override fun onVerifyCode(vToken: String) {
        _state.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            authenticationFlowDelegate.otpVerify(_state.value.otpValue, vToken) {
                onSuccess = {
                    _state.update { it.copy(isLoading = false, error = null) }
                    _navigationEvents.tryEmit(OtpVerifyNavigationEvent.NavigateToMyProfile)
                }

                onError = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }

                onPendingRegistration = { registrationContext ->
                    _state.update { it.copy(isLoading = false) }
                    _navigationEvents.tryEmit(
                        OtpVerifyNavigationEvent.NavigateToPendingRegistration(
                            registrationContext.toJson()
                        )
                    )
                }
            }
        }
    }

    override fun onResendCode() {
        _state.update { it.copy(codeSent = true) }
        startOtpTimer {
            _state.update { it.copy(codeSent = false) }
        }
    }

    //region TIMER

    override fun startOtpTimer(whenEnded: () -> Unit) {
        startTimer {
            whenEnded()
        }
    }

    private val _timer = MutableStateFlow(0L)
    val timer = _timer.asStateFlow()

    private var timerJob: Job? = null

    private fun startTimer(finished: () -> Unit) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(TimeUnit.SECONDS.toMillis(10))
                _timer.value++
                finished()
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
    }

    private fun stopTimer() {
        _timer.value = 0
        timerJob?.cancel()
    }

    override fun cancelAllTimers() {
        stopTimer()
    }

    //endregion TIMER

    override fun resolveLoginWithCode(
        code: String,
        resolvable: ResolvableContext,
        onLogin: () -> Unit,
        onFailedWith: (AuthError?) -> Unit
    ) {
        viewModelScope.launch {
            // Commented out implementation - keeping stub
        }
    }
}

// Mocked preview class for OtpVerifyViewModel
class OtpVerifyViewModelPreview : IOtpVerifyViewModel {
    override val state: StateFlow<OtpVerifyState> = MutableStateFlow(OtpVerifyState()).asStateFlow()
    override val navigationEvents: SharedFlow<OtpVerifyNavigationEvent> = MutableSharedFlow<OtpVerifyNavigationEvent>().asSharedFlow()
    
    override fun onVerifyCode(vToken: String) {}
    override fun onResendCode() {}
}
