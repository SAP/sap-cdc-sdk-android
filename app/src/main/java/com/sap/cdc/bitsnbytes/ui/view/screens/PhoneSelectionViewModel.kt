package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.TwoFactorContext
import com.sap.cdc.android.sdk.feature.tfa.TFAPhoneEntity
import com.sap.cdc.android.sdk.feature.tfa.TFAPhoneMethod
import com.sap.cdc.bitsnbytes.extensions.toJson
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.state.PhoneSelectionNavigationEvent
import com.sap.cdc.bitsnbytes.ui.state.PhoneSelectionState
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface IPhoneSelectionViewModel {
    val state: StateFlow<PhoneSelectionState>
    val navigationEvents: SharedFlow<PhoneSelectionNavigationEvent>
    val phoneList: StateFlow<List<TFAPhoneEntity>>
    val twoFactorContext: StateFlow<TwoFactorContext?>

    fun updateTwoFactorContext(newContext: TwoFactorContext)
    fun updateInputField(value: String) {}
    fun onRegisterPhoneNumber()
    fun onSendCode(phoneId: String)
    fun loadRegisteredPhoneNumbers()
}

class PhoneSelectionViewModel(context: Context, val flowDelegate: AuthenticationFlowDelegate) : 
    BaseViewModel(context), IPhoneSelectionViewModel {

    private val _state = MutableStateFlow(PhoneSelectionState())
    override val state: StateFlow<PhoneSelectionState> = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<PhoneSelectionNavigationEvent>(
        replay = 1,
        extraBufferCapacity = 0
    )
    override val navigationEvents: SharedFlow<PhoneSelectionNavigationEvent> = _navigationEvents.asSharedFlow()

    private val _phoneList = MutableStateFlow<List<TFAPhoneEntity>>(emptyList())
    override val phoneList: StateFlow<List<TFAPhoneEntity>> = _phoneList

    private val _twoFactorContext = MutableStateFlow<TwoFactorContext?>(null)
    override val twoFactorContext: StateFlow<TwoFactorContext?> = _twoFactorContext

    override fun updateTwoFactorContext(newContext: TwoFactorContext) {
        _twoFactorContext.value = newContext
    }

    override fun updateInputField(value: String) {
        _state.update { it.copy(inputField = value) }
    }

    override fun loadRegisteredPhoneNumbers() {
        _state.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            flowDelegate.getRegisteredPhoneNumbers(twoFactorContext.value!!) {
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                }

                doOnTwoFactorContextUpdated { context ->
                    _phoneList.value = context.phones!!
                    _state.update { it.copy(isLoading = false) }
                }

                onError = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            }
        }
    }

    override fun onRegisterPhoneNumber() {
        _state.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            flowDelegate.registerPhoneNumber(
                twoFactorContext = twoFactorContext.value!!,
                phoneNumber = _state.value.inputField,
                language = "en"
            ) {
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                }

                doOnTwoFactorContextUpdated { context ->
                    _twoFactorContext.value = context
                    _state.update { it.copy(isLoading = false) }
                    _navigationEvents.tryEmit(
                        PhoneSelectionNavigationEvent.NavigateToPhoneVerification(context.toJson())
                    )
                }

                onError = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            }
        }
    }

    override fun onSendCode(phoneId: String) {
        _state.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            flowDelegate.sendPhoneCode(
                twoFactorContext = twoFactorContext.value!!,
                phoneId = phoneId,
                method = TFAPhoneMethod.SMS,
                language = "en"
            ) {
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                }

                doOnTwoFactorContextUpdated { context ->
                    _twoFactorContext.value = context
                    _state.update { it.copy(isLoading = false) }
                    _navigationEvents.tryEmit(
                        PhoneSelectionNavigationEvent.NavigateToPhoneVerification(context.toJson())
                    )
                }

                onError = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
            }
        }
    }
}

// Mock preview class for the PhoneSelectionViewModel
class PhoneSelectionViewModelPreview : IPhoneSelectionViewModel {
    override val state: StateFlow<PhoneSelectionState> = MutableStateFlow(PhoneSelectionState()).asStateFlow()
    override val navigationEvents: SharedFlow<PhoneSelectionNavigationEvent> = 
        MutableSharedFlow<PhoneSelectionNavigationEvent>().asSharedFlow()
    override val phoneList: StateFlow<List<TFAPhoneEntity>> = MutableStateFlow(emptyList())
    override val twoFactorContext: StateFlow<TwoFactorContext?> = MutableStateFlow(TwoFactorContext())
    
    override fun updateTwoFactorContext(newContext: TwoFactorContext) {}
    override fun onRegisterPhoneNumber() {}
    override fun onSendCode(phoneId: String) {}
    override fun loadRegisteredPhoneNumbers() {}
}
