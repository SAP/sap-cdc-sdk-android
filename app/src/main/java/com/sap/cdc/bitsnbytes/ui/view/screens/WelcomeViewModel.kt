package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.state.WelcomeNavigationEvent
import com.sap.cdc.bitsnbytes.ui.state.WelcomeState
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface IWelcomeViewModel {
    val state: StateFlow<WelcomeState>
    val navigationEvents: SharedFlow<WelcomeNavigationEvent>
    
    fun onSingleSignOn(hostActivity: ComponentActivity)
}

class WelcomeViewModel(context: Context, val flowDelegate: AuthenticationFlowDelegate) : 
    BaseViewModel(context), IWelcomeViewModel {

    private val _state = MutableStateFlow(WelcomeState())
    override val state: StateFlow<WelcomeState> = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<WelcomeNavigationEvent>(
        replay = 1,
        extraBufferCapacity = 0
    )
    override val navigationEvents: SharedFlow<WelcomeNavigationEvent> = _navigationEvents.asSharedFlow()

    override fun onSingleSignOn(hostActivity: ComponentActivity) {
        _state.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            flowDelegate.singleSignOn(
                hostActivity = hostActivity,
                parameters = mutableMapOf()
            ) {
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    _navigationEvents.tryEmit(WelcomeNavigationEvent.NavigateToMyProfile)
                }

                onError = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            }
        }
    }
}

// Mock preview class for the WelcomeViewModel
class WelcomeViewModelPreview : IWelcomeViewModel {
    override val state: StateFlow<WelcomeState> = MutableStateFlow(WelcomeState()).asStateFlow()
    override val navigationEvents: SharedFlow<WelcomeNavigationEvent> = 
        MutableSharedFlow<WelcomeNavigationEvent>().asSharedFlow()
    
    override fun onSingleSignOn(hostActivity: ComponentActivity) {}
}
