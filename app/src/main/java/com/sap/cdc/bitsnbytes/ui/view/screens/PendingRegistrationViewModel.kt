package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.RegistrationContext
import com.sap.cdc.bitsnbytes.extensions.parseRequiredMissingFieldsForRegistration
import com.sap.cdc.bitsnbytes.extensions.toJson
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.state.PendingRegistrationNavigationEvent
import com.sap.cdc.bitsnbytes.ui.state.PendingRegistrationState
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

interface IPendingRegistrationViewModel {
    val state: StateFlow<PendingRegistrationState>
    val navigationEvents: SharedFlow<PendingRegistrationNavigationEvent>

    fun initializeMissingFields(registrationContext: RegistrationContext)
    fun updateFieldValue(field: String, value: String)
    fun onResolve(regToken: String)
}

// Mock preview class for the PendingRegistrationViewModel
class PendingRegistrationViewModelPreview : IPendingRegistrationViewModel {
    override val state: StateFlow<PendingRegistrationState> = MutableStateFlow(
        PendingRegistrationState(missingFields = listOf("nickname"))
    ).asStateFlow()
    override val navigationEvents: SharedFlow<PendingRegistrationNavigationEvent> = 
        MutableSharedFlow<PendingRegistrationNavigationEvent>().asSharedFlow()
    
    override fun initializeMissingFields(registrationContext: RegistrationContext) {}
    override fun updateFieldValue(field: String, value: String) {}
    override fun onResolve(regToken: String) {}
}

class PendingRegistrationViewModel(
    context: Context,
    private val authenticationFlowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), IPendingRegistrationViewModel {

    private val _state = MutableStateFlow(PendingRegistrationState())
    override val state: StateFlow<PendingRegistrationState> = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<PendingRegistrationNavigationEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    override val navigationEvents: SharedFlow<PendingRegistrationNavigationEvent> = _navigationEvents.asSharedFlow()

    override fun initializeMissingFields(registrationContext: RegistrationContext) {
        val fields = registrationContext.originatingError?.details?.parseRequiredMissingFieldsForRegistration() ?: emptyList()
        val fieldValues = fields.associateWith { "" }
        _state.update { it.copy(missingFields = fields, fieldValues = fieldValues) }
    }

    override fun updateFieldValue(field: String, value: String) {
        _state.update { 
            it.copy(fieldValues = it.fieldValues.toMutableMap().apply { this[field] = value })
        }
    }

    override fun onResolve(regToken: String) {
        _state.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            val jsonMap = mutableMapOf<String, JsonPrimitive>()
            _state.value.fieldValues.forEach { (key, value) ->
                jsonMap[key.substring(key.lastIndexOf(".") + 1)] = JsonPrimitive(value)
            }

            authenticationFlowDelegate.resolvePendingRegistration(
                missingFieldsSerialized = mutableMapOf("profile" to JsonObject(jsonMap).toString()),
                regToken = regToken
            ) {
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    _navigationEvents.tryEmit(PendingRegistrationNavigationEvent.NavigateToMyProfile)
                }
                
                onError = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }

                onLinkingRequired = { linkingContext ->
                    _state.update { it.copy(isLoading = false) }
                    _navigationEvents.tryEmit(
                        PendingRegistrationNavigationEvent.NavigateToLinkAccount(linkingContext.toJson())
                    )
                }

                onPendingRegistration = { registrationContext ->
                    val newFields = registrationContext.originatingError?.details?.parseRequiredMissingFieldsForRegistration() ?: emptyList()
                    val newFieldValues = newFields.associateWith { "" }
                    _state.update { it.copy(isLoading = false, missingFields = newFields, fieldValues = newFieldValues) }
                }
            }
        }
    }
}
