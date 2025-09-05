package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.feature.auth.AuthState
import com.sap.cdc.android.sdk.feature.auth.IAuthResponse
import com.sap.cdc.android.sdk.feature.auth.ResolvableContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

interface IOtpVerifyViewModel {

    fun startOtpTimer(whenEnded: () -> Unit) {
        // Stub
    }

    fun resolveLoginWithCode(
        code: String,
        resolvable: ResolvableContext,
        onLogin: () -> Unit,
        onPendingRegistration: (IAuthResponse?) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }
}

// Mocked preview class for OtpVerifyViewModel
class OtpVerifyViewModelPreview : IOtpVerifyViewModel

class OtpVerifyViewModel(context: Context): BaseViewModel(context), IOtpVerifyViewModel {

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

    /**
     * Resolve phone login. Verify code sent to phone number.
     */
    override fun resolveLoginWithCode(
        code: String,
        resolvable: ResolvableContext,
        onLogin: () -> Unit,
        onPendingRegistration: (IAuthResponse?) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.resolveLoginWithCode(code, resolvable)
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onLogin()
                }

                AuthState.INTERRUPTED -> {
                    when (authResponse.cdcResponse().errorCode()) {
                        ResolvableContext.ERR_ACCOUNT_PENDING_REGISTRATION -> {
                            onPendingRegistration(authResponse)
                        }
                    }
                }

                else -> {
                    onFailedWith(authResponse.toDisplayError()!!)
                }
            }
        }
    }

}

