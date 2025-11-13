package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.state.AuthMethodsNavigationEvent
import com.sap.cdc.bitsnbytes.ui.state.AuthMethodsState
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface IAuthMethodsViewModel {
    val state: StateFlow<AuthMethodsState>
    val navigationEvents: SharedFlow<AuthMethodsNavigationEvent>

    fun onSendCodeToEmail()
    fun onSendCodeToPhone()
    fun onUseTOTPApp()
    fun onBackToLogin()
}

class AuthMethodsViewModel(
    context: Context,
    val flowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), IAuthMethodsViewModel {

    private val _state = MutableStateFlow(AuthMethodsState())
    override val state: StateFlow<AuthMethodsState> = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<AuthMethodsNavigationEvent>(
        replay = 1,
        extraBufferCapacity = 0
    )
    override val navigationEvents: SharedFlow<AuthMethodsNavigationEvent> = _navigationEvents.asSharedFlow()

    fun initializeWithContext(twoFactorContextJson: String) {
        _state.update { it.copy(twoFactorContext = twoFactorContextJson) }
    }

    override fun onSendCodeToEmail() {
        // TODO: Implement send code to email logic
    }

    override fun onSendCodeToPhone() {
        viewModelScope.launch {
            _navigationEvents.emit(AuthMethodsNavigationEvent.NavigateToPhoneSelection)
        }
    }

    override fun onUseTOTPApp() {
        viewModelScope.launch {
            _navigationEvents.emit(
                AuthMethodsNavigationEvent.NavigateToTOTPVerification(_state.value.twoFactorContext)
            )
        }
    }

    override fun onBackToLogin() {
        viewModelScope.launch {
            _navigationEvents.emit(AuthMethodsNavigationEvent.NavigateToLogin)
        }
    }
}

// Mocked preview class for AuthMethodsViewModel
class AuthMethodsViewModelPreview : IAuthMethodsViewModel {
    override val state: StateFlow<AuthMethodsState> = MutableStateFlow(AuthMethodsState()).asStateFlow()
    override val navigationEvents: SharedFlow<AuthMethodsNavigationEvent> = MutableSharedFlow<AuthMethodsNavigationEvent>().asSharedFlow()
    
    override fun onSendCodeToEmail() {}
    override fun onSendCodeToPhone() {}
    override fun onUseTOTPApp() {}
    override fun onBackToLogin() {}
}
