package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

interface IBiometricLockedViewModel {
    fun unlockWithBiometrics(
        activity: ComponentActivity,
        authCallbacks: com.sap.cdc.android.sdk.feature.AuthCallbacks.() -> Unit
    ) {
        // Stub.
    }
}

class BiometricLockedViewModel(
    context: Context,
    private val flowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), IBiometricLockedViewModel {

    override fun unlockWithBiometrics(
        activity: ComponentActivity,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
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
                executor = executor,
                authCallbacks = authCallbacks
            )
        }
    }
}

// Mock preview class for the BiometricLockedViewModel
class BiometricLockedViewModelPreview : IBiometricLockedViewModel

