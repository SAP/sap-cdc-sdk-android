package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.LinkingContext
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.state.LinkAccountNavigationEvent
import com.sap.cdc.bitsnbytes.ui.state.LinkAccountState
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface ILinkAccountViewModel {
    val state: StateFlow<LinkAccountState>
    val navigationEvents: SharedFlow<LinkAccountNavigationEvent>

    fun onPasswordChanged(password: String)
    fun onLinkToSiteAccount(loginId: String, linkingContext: LinkingContext)
    fun onLinkToSocialProvider(hostActivity: ComponentActivity, provider: String, linkingContext: LinkingContext)
}

// Mocked preview class for LinkAccountViewModel
class LinkAccountViewModelPreview : ILinkAccountViewModel {
    override val state: StateFlow<LinkAccountState> = MutableStateFlow(LinkAccountState()).asStateFlow()
    override val navigationEvents: SharedFlow<LinkAccountNavigationEvent> = MutableSharedFlow<LinkAccountNavigationEvent>().asSharedFlow()
    
    override fun onPasswordChanged(password: String) {}
    override fun onLinkToSiteAccount(loginId: String, linkingContext: LinkingContext) {}
    override fun onLinkToSocialProvider(hostActivity: ComponentActivity, provider: String, linkingContext: LinkingContext) {}
}

class LinkAccountViewModel(context: Context, val flowDelegate: AuthenticationFlowDelegate) : BaseViewModel(context),
    ILinkAccountViewModel {

    private val _state = MutableStateFlow(LinkAccountState())
    override val state: StateFlow<LinkAccountState> = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<LinkAccountNavigationEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    override val navigationEvents: SharedFlow<LinkAccountNavigationEvent> = _navigationEvents.asSharedFlow()

    override fun onPasswordChanged(password: String) {
        _state.update { it.copy(password = password) }
    }

    /**
     * Resolve link account interruption with credentials input.
     */
    override fun onLinkToSiteAccount(
        loginId: String,
        linkingContext: LinkingContext
    ) {
        val currentState = _state.value
        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            flowDelegate.linkToSiteAccount(
                loginId,
                currentState.password,
                linkingContext
            ) {
                onSuccess = {
                    _state.update { it.copy(isLoading = false, error = null) }
                    _navigationEvents.tryEmit(LinkAccountNavigationEvent.NavigateToMyProfile)
                }
                onError = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            }
        }
    }

    /**
     * Resolve link account interruption to social account.
     */
    override fun onLinkToSocialProvider(
        hostActivity: ComponentActivity,
        provider: String,
        linkingContext: LinkingContext
    ) {
        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            flowDelegate.linkToSocialProvider(
                hostActivity,
                provider,
                linkingContext
            ) {
                onSuccess = {
                    _state.update { it.copy(isLoading = false, error = null) }
                    _navigationEvents.tryEmit(LinkAccountNavigationEvent.NavigateToMyProfile)
                }
                onError = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            }
        }
    }
}
