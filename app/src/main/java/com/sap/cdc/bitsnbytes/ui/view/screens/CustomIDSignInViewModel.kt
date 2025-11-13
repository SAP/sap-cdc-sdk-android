package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.CustomIdCredentials
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.state.CustomIDSignInNavigationEvent
import com.sap.cdc.bitsnbytes.ui.state.CustomIDSignInState
import com.sap.cdc.bitsnbytes.ui.view.screens.ICustomIDSignInViewModel.Companion.CUSTOM_ID_PREFIX
import com.sap.cdc.bitsnbytes.ui.view.screens.ICustomIDSignInViewModel.Companion.IDENTIFIER_TYPE_NATIONAL_ID
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface ICustomIDSignInViewModel {
    val state: StateFlow<CustomIDSignInState>
    val navigationEvents: SharedFlow<CustomIDSignInNavigationEvent>

    companion object {
        const val CUSTOM_ID_PREFIX = "gigya.com/identifiers/customIdentifiers/"
        const val IDENTIFIER_TYPE_NATIONAL_ID = "nationalId"
    }

    fun onIdentifierChanged(identifier: String)
    fun onPasswordChanged(password: String)
    fun onPasswordVisibilityToggled()
    fun onLoginClick()
}

class CustomIDSignInViewModelPreview : ICustomIDSignInViewModel {
    override val state: StateFlow<CustomIDSignInState> = MutableStateFlow(CustomIDSignInState()).asStateFlow()
    override val navigationEvents: SharedFlow<CustomIDSignInNavigationEvent> = MutableSharedFlow<CustomIDSignInNavigationEvent>().asSharedFlow()
    
    override fun onIdentifierChanged(identifier: String) {}
    override fun onPasswordChanged(password: String) {}
    override fun onPasswordVisibilityToggled() {}
    override fun onLoginClick() {}
}

class CustomIDSignInViewModel(
    context: Context,
    private val flowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), ICustomIDSignInViewModel {

    private val _state = MutableStateFlow(CustomIDSignInState())
    override val state: StateFlow<CustomIDSignInState> = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<CustomIDSignInNavigationEvent>(
        replay = 1,
        extraBufferCapacity = 0
    )
    override val navigationEvents: SharedFlow<CustomIDSignInNavigationEvent> = _navigationEvents.asSharedFlow()

    override fun onIdentifierChanged(identifier: String) {
        _state.update { it.copy(identifier = identifier) }
    }

    override fun onPasswordChanged(password: String) {
        _state.update { it.copy(password = password) }
    }

    override fun onPasswordVisibilityToggled() {
        _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    override fun onLoginClick() {
        val currentState = _state.value
        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val credentials = CustomIdCredentials(
                identifier = currentState.identifier,
                identifierType = CUSTOM_ID_PREFIX + IDENTIFIER_TYPE_NATIONAL_ID,
                password = currentState.password
            )
            flowDelegate.loginWithCustomId(credentials = credentials) {
                onSuccess = {
                    _state.update { it.copy(isLoading = false, error = null) }
                    _navigationEvents.tryEmit(CustomIDSignInNavigationEvent.NavigateToMyProfile)
                }
                onError = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            }
        }
    }
}
