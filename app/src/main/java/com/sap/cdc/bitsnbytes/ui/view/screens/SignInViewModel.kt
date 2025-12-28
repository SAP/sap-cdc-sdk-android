package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.feature.provider.passkey.IPasskeysAuthenticationProvider
import com.sap.cdc.bitsnbytes.extensions.toJson
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.feature.provider.PasskeysAuthenticationProvider
import com.sap.cdc.bitsnbytes.navigation.ProfileScreenRoute
import com.sap.cdc.bitsnbytes.ui.state.SignInNavigationEvent
import com.sap.cdc.bitsnbytes.ui.state.SignInState
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

interface ISignInViewModel {
    val state: StateFlow<SignInState>
    val navigationEvents: SharedFlow<SignInNavigationEvent>

    fun onEmailSignInClick()

    fun onCustomIdSignInClick()

    fun onPhoneSignInClick()

    fun passkeyLogin(activity: ComponentActivity)

    fun getAuthenticationProvider(name: String): IAuthenticationProvider? {
        return null
    }

    fun socialSignInWith(
        hostActivity: ComponentActivity,
        provider: String
    )
}

// Mock preview class for the SignInViewModel
class SignInViewModelPreview : ISignInViewModel {
    override val state: StateFlow<SignInState> = MutableStateFlow(SignInState()).asStateFlow()
    override val navigationEvents: SharedFlow<SignInNavigationEvent> = MutableSharedFlow<SignInNavigationEvent>().asSharedFlow()
    override fun onEmailSignInClick() {}
    override fun onCustomIdSignInClick() {}
    override fun onPhoneSignInClick() {}
    override fun passkeyLogin(activity: ComponentActivity) {}
    override fun socialSignInWith(hostActivity: ComponentActivity, provider: String) {}
}

class SignInViewModel(context: Context, val flowDelegate: AuthenticationFlowDelegate) :
    BaseViewModel(context), ISignInViewModel {

    private val _state = MutableStateFlow(SignInState())
    override val state: StateFlow<SignInState> = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<SignInNavigationEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    override val navigationEvents: SharedFlow<SignInNavigationEvent> = _navigationEvents.asSharedFlow()

    private var passkeysAuthenticationProvider: IPasskeysAuthenticationProvider? = null

    /**
     * Handle email sign in button click.
     */
    override fun onEmailSignInClick() {
        viewModelScope.launch {
            _navigationEvents.emit(SignInNavigationEvent.NavigateToAuthTab(1))
        }
    }

    /**
     * Handle custom ID sign in button click.
     */
    override fun onCustomIdSignInClick() {
        viewModelScope.launch {
            _navigationEvents.emit(SignInNavigationEvent.NavigateToCustomIdSignIn)
        }
    }

    /**
     * Handle phone sign in button click.
     */
    override fun onPhoneSignInClick() {
        viewModelScope.launch {
            _navigationEvents.emit(SignInNavigationEvent.NavigateToOTPSignIn(OTPType.PHONE.value.toString()))
        }
    }

    /**
     * Perform passkey login.
     */
    override fun passkeyLogin(activity: ComponentActivity) {
        if (passkeysAuthenticationProvider == null) {
            passkeysAuthenticationProvider = PasskeysAuthenticationProvider(WeakReference(activity))
        }

        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            flowDelegate.passkeyLogin(
                provider = passkeysAuthenticationProvider!!
            ) {
                onSuccess = {
                    _state.update { it.copy(isLoading = false, error = null) }
                    _navigationEvents.tryEmit(
                        SignInNavigationEvent.NavigateToProfile(
                            ProfileScreenRoute.MyProfile.route
                        )
                    )
                }

                onError = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            }
        }
    }

    /**
     * Helper method to fetch a registered authentication provider.
     */
    override fun getAuthenticationProvider(name: String): IAuthenticationProvider? {
        return flowDelegate.getAuthenticationProvider(name)
    }

    /**
     * Social sign in flow.
     * ViewModel handles all navigation decisions based on authentication flow results.
     */
    override fun socialSignInWith(
        hostActivity: ComponentActivity,
        provider: String
    ) {
        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            flowDelegate.signInWithProvider(
                hostActivity = hostActivity,
                provider = provider
            ) {
                onSuccess = {
                    _state.update { it.copy(isLoading = false, error = null) }
                    _navigationEvents.tryEmit(
                        SignInNavigationEvent.NavigateToProfile(
                            ProfileScreenRoute.MyProfile.route
                        )
                    )
                }

                onError = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }

                onPendingRegistration = { registrationContext ->
                    _state.update { it.copy(isLoading = false) }
                    _navigationEvents.tryEmit(
                        SignInNavigationEvent.NavigateToPendingRegistration(
                            registrationContext.toJson()
                        )
                    )
                }

                onLinkingRequired = { linkingContext ->
                    _state.update { it.copy(isLoading = false) }
                    _navigationEvents.tryEmit(
                        SignInNavigationEvent.NavigateToLinkAccount(
                            linkingContext.toJson()
                        )
                    )
                }
            }
        }
    }
}
