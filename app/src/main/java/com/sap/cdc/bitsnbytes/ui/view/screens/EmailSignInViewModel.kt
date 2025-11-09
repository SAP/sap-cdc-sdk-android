package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.Credentials
import com.sap.cdc.bitsnbytes.extensions.toJson
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.state.EmailSignInNavigationEvent
import com.sap.cdc.bitsnbytes.ui.state.EmailSignInState
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Created by Tal Mirmelshtein on 17/12/2024
 * Copyright: SAP LTD.
 */

interface IEmailSignInViewModel {
    val state: StateFlow<EmailSignInState>
    val navigationEvents: SharedFlow<EmailSignInNavigationEvent>

    fun onEmailChanged(email: String)
    fun onPasswordChanged(password: String)
    fun onPasswordVisibilityToggled()
    fun onLoginClick()
    fun onGetCaptchaToken()
}

// Mock preview class for the EmailSignInViewModel
class EmailSignInViewModelPreview : IEmailSignInViewModel {
    override val state: StateFlow<EmailSignInState> = MutableStateFlow(EmailSignInState()).asStateFlow()
    override val navigationEvents: SharedFlow<EmailSignInNavigationEvent> = MutableSharedFlow<EmailSignInNavigationEvent>().asSharedFlow()
    
    override fun onEmailChanged(email: String) {}
    override fun onPasswordChanged(password: String) {}
    override fun onPasswordVisibilityToggled() {}
    override fun onLoginClick() {}
    override fun onGetCaptchaToken() {}
}

class EmailSignInViewModel(
    context: Context,
    private val flowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), IEmailSignInViewModel {

    private val _state = MutableStateFlow(EmailSignInState())
    override val state: StateFlow<EmailSignInState> = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<EmailSignInNavigationEvent>(
        replay = 1,
        extraBufferCapacity = 0
    )
    override val navigationEvents: SharedFlow<EmailSignInNavigationEvent> = _navigationEvents.asSharedFlow()

    override fun onEmailChanged(email: String) {
        _state.update { it.copy(email = email) }
    }

    override fun onPasswordChanged(password: String) {
        _state.update { it.copy(password = password) }
    }

    override fun onPasswordVisibilityToggled() {
        _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    override fun onGetCaptchaToken() {
        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            flowDelegate.getSaptchaToken {
                onSuccess = { authSuccess ->
                    _state.update { it.copy(isLoading = false, captchaRequired = false) }
                    val token = authSuccess.userData["saptchaToken"] as? String
                    // Token retrieved but not stored in state - could be added if needed
                }
                onError = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            }
        }
    }

    override fun onLoginClick() {
        val currentState = _state.value
        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val credentials = Credentials(
                loginId = currentState.email,
                password = currentState.password
            )
            
            flowDelegate.login(credentials = credentials) {
                onSuccess = {
                    _state.update { it.copy(isLoading = false, error = null) }
                    _navigationEvents.tryEmit(EmailSignInNavigationEvent.NavigateToMyProfile)
                }
                
                onError = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
                
                onLinkingRequired = {
                    _state.update { it.copy(isLoading = false) }
                    // Linking flow not implemented yet
                }
                
                onTwoFactorRequired = { twoFactorContext ->
                    _state.update { it.copy(isLoading = false, error = null) }
                    _navigationEvents.tryEmit(
                        EmailSignInNavigationEvent.NavigateToAuthMethods(twoFactorContext.toJson())
                    )
                }
                
                onCaptchaRequired = {
                    _state.update { it.copy(isLoading = false, captchaRequired = true, error = "Captcha required") }
                }
                
                onPendingRegistration = { registrationContext ->
                    _state.update { it.copy(isLoading = false, error = null) }
                    _navigationEvents.tryEmit(
                        EmailSignInNavigationEvent.NavigateToPendingRegistration(registrationContext.toJson())
                    )
                }
            }
        }
    }
}
