package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.ResolvableContext
import com.sap.cdc.android.sdk.auth.tfa.TFAPhoneEntity
import com.sap.cdc.android.sdk.core.api.model.CDCError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface IPhoneSelectionViewModel {

    val phoneList: StateFlow<List<TFAPhoneEntity>>

    fun registerTFAPhoneNumber(
        phoneNumber: String,
        resolvableContext: ResolvableContext,
        language: String?,
        onVerificationCodeSent: (IAuthResponse?) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun getRegisteredPhoneNumbers(
        resolvableContext: ResolvableContext,
        onRegisteredPhoneNumbers: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun sendRegisteredPhoneCode(
        phoneId: String,
        resolvableContext: ResolvableContext,
        language: String?,
        onVerificationCodeSent: (IAuthResponse?) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {

        // Stub
    }
}

// Mock preview class for the PhoneSelectionViewModel
class PhoneSelectionViewModelPreview : IPhoneSelectionViewModel {

    override val phoneList: StateFlow<List<TFAPhoneEntity>>
        get() {
            return MutableStateFlow(emptyList())
        }
}


class PhoneSelectionViewModel(context: Context) : BaseViewModel(context), IPhoneSelectionViewModel {

    private val _phoneList = MutableStateFlow<List<TFAPhoneEntity>>(emptyList())
    override val phoneList: StateFlow<List<TFAPhoneEntity>> = _phoneList

    override fun registerTFAPhoneNumber(
        phoneNumber: String,
        resolvableContext: ResolvableContext,
        language: String?,
        onVerificationCodeSent: (IAuthResponse?) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.registerTFAPhoneNumber(
                phoneNumber,
                resolvableContext,
                language,
            )
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onVerificationCodeSent(authResponse)
                }

                AuthState.ERROR, AuthState.INTERRUPTED -> {
                    onFailedWith(authResponse.toDisplayError())
                }
            }
        }
    }

    override fun getRegisteredPhoneNumbers(
        resolvableContext: ResolvableContext,
        onRegisteredPhoneNumbers: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.getRegisteredTFAPhoneNumbers(resolvableContext)
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    _phoneList.value = authResponse.resolvable()?.tfa?.phones!!
                    onRegisteredPhoneNumbers()
                }

                AuthState.ERROR, AuthState.INTERRUPTED -> {
                    onFailedWith(authResponse.toDisplayError())
                }
            }
        }
    }

    override fun sendRegisteredPhoneCode(
        phoneId: String,
        resolvableContext: ResolvableContext,
        language: String?,
        onVerificationCodeSent: (IAuthResponse?) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.sendRegisteredPhoneCode(
                phoneId,
                resolvableContext,
                language,
            )
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onVerificationCodeSent(authResponse)
                }

                AuthState.ERROR, AuthState.INTERRUPTED -> {
                    onFailedWith(authResponse.toDisplayError())
                }
            }
        }
    }
}
