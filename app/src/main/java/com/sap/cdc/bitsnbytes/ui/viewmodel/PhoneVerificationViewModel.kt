package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.auth.ResolvableContext
import com.sap.cdc.android.sdk.core.api.model.CDCError
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

interface IPhoneVerificationViewModel {

    fun startOtpTimer(whenEnded: () -> Unit) {
        // Stub
    }

    fun verifyTFACode(
        code: String,
        resolvableContext: ResolvableContext,
        rememberDevice: Boolean,
        onVerified: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        // Stub
    }
}

// Mock preview class for the PhoneVerificationViewModel
class PhoneVerificationViewModelPreview : IPhoneVerificationViewModel

class PhoneVerificationViewModel(context: Context) : BaseViewModel(context),
    IPhoneVerificationViewModel {

    private val _timer = MutableStateFlow(0L)
    val timer = _timer.asStateFlow()
    private var timerJob: Job? = null

    override fun verifyTFACode(
        code: String,
        resolvableContext: ResolvableContext,
        rememberDevice: Boolean,
        onVerified: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.verifyTFAPhoneCode(
                code,
                resolvableContext,
                rememberDevice
            )
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onVerified()
                }

                AuthState.ERROR, AuthState.INTERRUPTED -> {
                    onFailedWith(authResponse.toDisplayError())
                }
            }
        }
    }

    //region TIMER

    override fun startOtpTimer(whenEnded: () -> Unit) {
        startTimer {
            whenEnded()
        }
    }

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

    //endregion
}