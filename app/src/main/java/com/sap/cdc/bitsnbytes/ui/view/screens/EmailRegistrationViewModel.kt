package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.Credentials
import com.sap.cdc.bitsnbytes.extensions.parseRequiredMissingFieldsForRegistration
import com.sap.cdc.bitsnbytes.extensions.splitFullName
import com.sap.cdc.bitsnbytes.extensions.toJson
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.state.EmailRegistrationNavigationEvent
import com.sap.cdc.bitsnbytes.ui.state.EmailRegistrationState
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Created by Tal Mirmelshtein on 17/12/2024
 * Copyright: SAP LTD.
 */

interface IEmailRegistrationViewModel {
    val state: StateFlow<EmailRegistrationState>
    val navigationEvents: SharedFlow<EmailRegistrationNavigationEvent>

    fun onNameChanged(name: String)
    fun onEmailChanged(email: String)
    fun onPasswordChanged(password: String)
    fun onConfirmPasswordChanged(confirmPassword: String)
    fun onPasswordVisibilityToggled()
    fun onRegisterClick()
}

// Mock preview class for the EmailRegisterViewModel
class EmailRegistrationViewModelPreview : IEmailRegistrationViewModel {
    override val state: StateFlow<EmailRegistrationState> = MutableStateFlow(EmailRegistrationState()).asStateFlow()
    override val navigationEvents: SharedFlow<EmailRegistrationNavigationEvent> = MutableSharedFlow<EmailRegistrationNavigationEvent>().asSharedFlow()
    
    override fun onNameChanged(name: String) {}
    override fun onEmailChanged(email: String) {}
    override fun onPasswordChanged(password: String) {}
    override fun onConfirmPasswordChanged(confirmPassword: String) {}
    override fun onPasswordVisibilityToggled() {}
    override fun onRegisterClick() {}
}

class EmailRegistrationViewModel(
    context: Context,
    private val flowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), IEmailRegistrationViewModel {

    private val _state = MutableStateFlow(EmailRegistrationState())
    override val state: StateFlow<EmailRegistrationState> = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<EmailRegistrationNavigationEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    override val navigationEvents: SharedFlow<EmailRegistrationNavigationEvent> = _navigationEvents.asSharedFlow()

    override fun onNameChanged(name: String) {
        _state.update { it.copy(name = name) }
    }

    override fun onEmailChanged(email: String) {
        _state.update { it.copy(email = email) }
    }

    override fun onPasswordChanged(password: String) {
        _state.update { it.copy(password = password) }
    }

    override fun onConfirmPasswordChanged(confirmPassword: String) {
        _state.update { it.copy(confirmPassword = confirmPassword) }
    }

    override fun onPasswordVisibilityToggled() {
        _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    /**
     * Register new account using credentials (email,password)
     * Additional profile fields are included to set profile.firstName & profile.lastName fields.
     */
    override fun onRegisterClick() {
        val currentState = _state.value
        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val namePair = currentState.name.splitFullName()
            val profileObject = json.encodeToJsonElement(
                mutableMapOf(
                    "firstName" to namePair.first,
                    "lastName" to namePair.second
                )
            )
            
            val credentials = Credentials(
                email = currentState.email,
                password = currentState.password
            )
            
            flowDelegate.register(
                credentials,
                mutableMapOf("profile" to profileObject.toString())
            ) {
                doOnPendingRegistrationAndOverride { registrationContext ->
                    val parsedMissingRequiredFieldsFromErrorDetails =
                        registrationContext.originatingError?.details?.parseRequiredMissingFieldsForRegistration()
                    registrationContext.copy(missingRequiredFields = parsedMissingRequiredFieldsFromErrorDetails)
                }

                onSuccess = {
                    _state.update { it.copy(isLoading = false, error = null) }
                    _navigationEvents.tryEmit(EmailRegistrationNavigationEvent.NavigateToMyProfile)
                }
                
                onError = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
                
                onTwoFactorRequired = { twoFactorContext ->
                    _state.update { it.copy(isLoading = false, error = null) }
                    _navigationEvents.tryEmit(
                        EmailRegistrationNavigationEvent.NavigateToAuthMethods(twoFactorContext.toJson())
                    )
                }
                
                onPendingRegistration = { registrationContext ->
                    _state.update { it.copy(isLoading = false, error = null) }
                    _navigationEvents.tryEmit(
                        EmailRegistrationNavigationEvent.NavigateToPendingRegistration(registrationContext.toJson())
                    )
                }
            }
        }
    }
}
