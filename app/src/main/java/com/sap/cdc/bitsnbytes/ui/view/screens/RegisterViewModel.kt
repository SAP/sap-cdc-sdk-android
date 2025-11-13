package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.provider.IAuthenticationProvider
import com.sap.cdc.bitsnbytes.extensions.toJson
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.state.RegisterNavigationEvent
import com.sap.cdc.bitsnbytes.ui.state.RegisterState
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface IRegisterViewModel {
    val state: StateFlow<RegisterState>
    val navigationEvents: SharedFlow<RegisterNavigationEvent>

    fun getAuthenticationProvider(name: String): IAuthenticationProvider? {
        return null
    }

    fun onSocialSignIn(hostActivity: ComponentActivity, provider: String)
}

// Mock preview class for the RegisterViewModel
class RegisterViewModelPreview : IRegisterViewModel {
    override val state: StateFlow<RegisterState> = MutableStateFlow(RegisterState()).asStateFlow()
    override val navigationEvents: SharedFlow<RegisterNavigationEvent> = 
        MutableSharedFlow<RegisterNavigationEvent>().asSharedFlow()
    
    override fun onSocialSignIn(hostActivity: ComponentActivity, provider: String) {}
}

class RegisterViewModel(context: Context, val flowDelegate: AuthenticationFlowDelegate) :
    BaseViewModel(context), IRegisterViewModel {

    private val _state = MutableStateFlow(RegisterState())
    override val state: StateFlow<RegisterState> = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<RegisterNavigationEvent>(
        replay = 1,
        extraBufferCapacity = 0
    )
    override val navigationEvents: SharedFlow<RegisterNavigationEvent> = _navigationEvents.asSharedFlow()

    override fun getAuthenticationProvider(name: String): IAuthenticationProvider? {
        return flowDelegate.getAuthenticationProvider(name)
    }

    override fun onSocialSignIn(hostActivity: ComponentActivity, provider: String) {
        _state.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            flowDelegate.signInWithProvider(
                hostActivity = hostActivity,
                provider = provider
            ) {
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    _navigationEvents.tryEmit(RegisterNavigationEvent.NavigateToMyProfile)
                }
                
                onError = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }

                onPendingRegistration = { registrationContext ->
                    _state.update { it.copy(isLoading = false) }
                    _navigationEvents.tryEmit(
                        RegisterNavigationEvent.NavigateToPendingRegistration(registrationContext.toJson())
                    )
                }

                onLinkingRequired = { linkingContext ->
                    _state.update { it.copy(isLoading = false) }
                    _navigationEvents.tryEmit(
                        RegisterNavigationEvent.NavigateToLinkAccount(linkingContext.toJson())
                    )
                }
            }
        }
    }
}
