package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.state.MyProfileNavigationEvent
import com.sap.cdc.bitsnbytes.ui.state.MyProfileState
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface IMyProfileViewModel {
    val state: StateFlow<MyProfileState>
    val navigationEvents: SharedFlow<MyProfileNavigationEvent>
    val flowDelegate: AuthenticationFlowDelegate?
        get() = null

    fun getAccountInfo(
        parameters: MutableMap<String, String>? = mutableMapOf(),
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        // Stub
    }

    fun onLogout()
}

class MyProfileViewModel(context: Context, override val flowDelegate: AuthenticationFlowDelegate) :
    BaseViewModel(context), IMyProfileViewModel {

    private val _state = MutableStateFlow(MyProfileState())
    override val state: StateFlow<MyProfileState> = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<MyProfileNavigationEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    override val navigationEvents: SharedFlow<MyProfileNavigationEvent> = _navigationEvents.asSharedFlow()

    override fun getAccountInfo(
        parameters: MutableMap<String, String>?,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        _state.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            // Delegate to AuthenticationFlowDelegate which manages state
            flowDelegate.getAccountInfo(parameters = parameters ?: mutableMapOf()) {
                authCallbacks()
                
                doOnSuccess {
                    _state.update { it.copy(isLoading = false, isRefreshing = false) }
                }
                
                doOnError {
                    _state.update { it.copy(isLoading = false, isRefreshing = false) }
                }
            }
        }
    }

    /**
     * Log out of current session.
     */
    override fun onLogout() {
        viewModelScope.launch {
            flowDelegate.logOut {
                onSuccess = {
                    _navigationEvents.tryEmit(MyProfileNavigationEvent.NavigateToWelcome)
                }
                onError = {
                    // Even on error, navigate to Welcome
                    _navigationEvents.tryEmit(MyProfileNavigationEvent.NavigateToWelcome)
                }
            }
        }
    }
}

// Mock preview class for the MyProfileViewModel
class MyProfileViewModelPreview : IMyProfileViewModel {
    override val state: StateFlow<MyProfileState> = MutableStateFlow(MyProfileState()).asStateFlow()
    override val navigationEvents: SharedFlow<MyProfileNavigationEvent> = MutableSharedFlow<MyProfileNavigationEvent>().asSharedFlow()
    
    override fun onLogout() {}
}
