package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.TwoFactorContext
import com.sap.cdc.android.sdk.feature.tfa.TFAPhoneEntity
import com.sap.cdc.android.sdk.feature.tfa.TFAPhoneMethod
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface IPhoneSelectionViewModel {

    val phoneList: StateFlow<List<TFAPhoneEntity>>
    val twoFactorContext: StateFlow<TwoFactorContext?>

    fun updateTwoFactorContext(newContext: TwoFactorContext) {
        // Stub
    }

    fun registerPhoneNumber(
        phoneNumber: String,
        language: String = "en",
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        //Stub
    }

    fun sendCode(
        phoneId: String,
        language: String = "en",
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        // Stub
    }

    fun getRegisteredPhoneNumbers(
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        //Stub
    }

}

class PhoneSelectionViewModel(context: Context, val flowDelegate: AuthenticationFlowDelegate) : BaseViewModel(context),
    IPhoneSelectionViewModel {

    private val _phoneList = MutableStateFlow<List<TFAPhoneEntity>>(emptyList())
    override val phoneList: StateFlow<List<TFAPhoneEntity>> = _phoneList

    private val _twoFactorContext = MutableStateFlow<TwoFactorContext?>(null)
    override val twoFactorContext: StateFlow<TwoFactorContext?> = _twoFactorContext

    override fun updateTwoFactorContext(newContext: TwoFactorContext) {
        _twoFactorContext.value = newContext
    }

    override fun getRegisteredPhoneNumbers(
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        viewModelScope.launch {
            flowDelegate.getRegisteredPhoneNumbers(
                twoFactorContext.value!!
            ) {
                authCallbacks()

                doOnTwoFactorContextUpdated { context ->
                    // Phone list will be populated at this point.
                    _phoneList.value = context.phones!!
                }
            }
        }
    }

    override fun registerPhoneNumber(
        phoneNumber: String, language: String,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        viewModelScope.launch {
            flowDelegate.registerPhoneNumber(
                twoFactorContext = twoFactorContext.value!!,
                phoneNumber = phoneNumber,
                language = language,
            ) {
                authCallbacks()

                doOnTwoFactorContextUpdated { context ->
                    // phvToken will be populated at this point.
                    _twoFactorContext.value = context
                }
            }
        }
    }

    override fun sendCode(
        phoneId: String,
        language: String,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        viewModelScope.launch {
            flowDelegate.sendPhoneCode(
                twoFactorContext = twoFactorContext.value!!,
                phoneId = phoneId,
                method = TFAPhoneMethod.SMS,
                language = language,
            ) {
                authCallbacks()

                doOnTwoFactorContextUpdated { context ->
                    // phvToken will be populated at this point.
                    _twoFactorContext.value = context
                }
            }
        }
    }
}

// Mock preview class for the PhoneSelectionViewModel
class PhoneSelectionViewModelPreview : IPhoneSelectionViewModel {

    override val phoneList: StateFlow<List<TFAPhoneEntity>>
        get() = MutableStateFlow(emptyList())
    override val twoFactorContext: StateFlow<TwoFactorContext?>
        get() = MutableStateFlow(TwoFactorContext())

}