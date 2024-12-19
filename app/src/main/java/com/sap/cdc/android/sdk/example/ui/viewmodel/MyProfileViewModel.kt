package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.auth.biometric.BiometricAuth
import com.sap.cdc.android.sdk.core.api.model.CDCError
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

interface IMyProfileViewModel : IAccountViewModel {

    fun logOut(success: () -> Unit, onFailed: (CDCError) -> Unit) {
        //Stub
    }
}

class MyProfileViewModelPreview : IMyProfileViewModel {}

class MyProfileViewModel(context: Context) : BaseViewModel(context), IMyProfileViewModel {

    private var biometricAuth = BiometricAuth(identityService.authenticationService.sessionService)

    /**
     * Log out of current session.
     */
    override fun logOut(success: () -> Unit, onFailed: (CDCError) -> Unit) {
        viewModelScope.launch {
            val authResponse = identityService.logout()
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    success()
                }

                else -> {
                    onFailed(authResponse.toDisplayError()!!)
                }
            }
        }
    }

    override fun promptBiometricUnlockIfNeeded(
        activity: FragmentActivity,
        promptInfo: BiometricPrompt.PromptInfo,
        executor: Executor
    ) {
        if (identityService.authenticationService.sessionService.biometricLocked()
        ) {
            biometricAuth.unlockSessionWithBiometricAuthentication(
                activity = activity,
                promptInfo = promptInfo,
                executor = executor,
                onAuthenticationError = { _, _ ->

                },
                onAuthenticationFailed = {

                },
                onAuthenticationSucceeded = {

                }
            )
        }
    }

}