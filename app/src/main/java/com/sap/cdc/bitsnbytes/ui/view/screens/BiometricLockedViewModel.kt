package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.state.BiometricLockedNavigationEvent
import com.sap.cdc.bitsnbytes.ui.state.BiometricLockedState
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface IBiometricLockedViewModel {
    val state: StateFlow<BiometricLockedState>
    val navigationEvents: SharedFlow<BiometricLockedNavigationEvent>
    
    fun onUnlockClick(activity: ComponentActivity)
}

class BiometricLockedViewModel(
    context: Context,
    private val flowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), IBiometricLockedViewModel {

    private val _state = MutableStateFlow(BiometricLockedState())
    override val state: StateFlow<BiometricLockedState> = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<BiometricLockedNavigationEvent>(
        replay = 1,
        extraBufferCapacity = 0
    )
    override val navigationEvents: SharedFlow<BiometricLockedNavigationEvent> = _navigationEvents.asSharedFlow()

    override fun onUnlockClick(activity: ComponentActivity) {
        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val executor = ContextCompat.getMainExecutor(activity)

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Session")
                .setSubtitle("Use your biometric credential to unlock your session")
                .setNegativeButtonText("Cancel")
                .build()

            flowDelegate.biometricUnlock(
                activity = activity as FragmentActivity,
                promptInfo = promptInfo,
                executor = executor
            ) {
                onSuccess = {
                    _state.update { it.copy(isLoading = false, error = null) }
                    _navigationEvents.tryEmit(BiometricLockedNavigationEvent.NavigateToMyProfile)
                }
                onError = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            }
        }
    }
}

// Mock preview class for the BiometricLockedViewModel
class BiometricLockedViewModelPreview : IBiometricLockedViewModel {
    override val state: StateFlow<BiometricLockedState> = MutableStateFlow(BiometricLockedState()).asStateFlow()
    override val navigationEvents: SharedFlow<BiometricLockedNavigationEvent> = MutableSharedFlow<BiometricLockedNavigationEvent>().asSharedFlow()
    
    override fun onUnlockClick(activity: ComponentActivity) {}
}
