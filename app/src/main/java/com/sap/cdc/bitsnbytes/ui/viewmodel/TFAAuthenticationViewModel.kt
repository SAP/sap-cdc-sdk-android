package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.ResolvableContext
import com.sap.cdc.android.sdk.auth.tfa.TFAPhoneEntity
import com.sap.cdc.android.sdk.core.api.model.CDCError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface ITFAAuthenticationViewModel {

    val phoneList: StateFlow<List<TFAPhoneEntity>>
    val resolvableContext: StateFlow<ResolvableContext?>

    fun updateResolvableContext(newContext: ResolvableContext) {
        // Stub
    }

    fun registerTFAPhoneNumber(
        phoneNumber: String,
        language: String?,
        onVerificationCodeSent: (IAuthResponse?) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun getRegisteredPhoneNumbers(
        onRegisteredPhoneNumbers: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun sendRegisteredPhoneCode(
        phoneId: String,
        language: String?,
        onVerificationCodeSent: (IAuthResponse?) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        // Stub
    }

    fun verifyPhoneCode(
        code: String,
        rememberDevice: Boolean,
        onVerified: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        // Stub
    }

    fun registerNewAuthenticatorApp(
        onQACode: (Bitmap) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun verifyTOTPCode(
        code: String,
        onVerificationSuccess: (IAuthResponse) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

}

class TFAAuthenticationViewModelPreview : ITFAAuthenticationViewModel {

    override val phoneList: StateFlow<List<TFAPhoneEntity>>
        get() {
            return MutableStateFlow(emptyList())
        }
    override val resolvableContext: StateFlow<ResolvableContext?>
        get() {
            return MutableStateFlow(ResolvableContext())
        }

}


class TFAAuthenticationViewModel(context: Context) : BaseViewModel(context),
    ITFAAuthenticationViewModel {

    private val _phoneList = MutableStateFlow<List<TFAPhoneEntity>>(emptyList())
    override val phoneList: StateFlow<List<TFAPhoneEntity>> = _phoneList

    private val _resolvableContext = MutableStateFlow<ResolvableContext?>(null)
    override val resolvableContext: StateFlow<ResolvableContext?> = _resolvableContext

    override fun updateResolvableContext(newContext: ResolvableContext) {
        _resolvableContext.value = newContext
    }

    //region Phone

    override fun registerTFAPhoneNumber(
        phoneNumber: String,
        language: String?,
        onVerificationCodeSent: (IAuthResponse?) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.registerTFAPhoneNumber(
                phoneNumber,
                resolvableContext.value!!,
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
        onRegisteredPhoneNumbers: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.getRegisteredTFAPhoneNumbers(resolvableContext.value!!)
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
        language: String?,
        onVerificationCodeSent: (IAuthResponse?) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.sendRegisteredPhoneCode(
                phoneId,
                resolvableContext.value!!,
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

    override fun verifyPhoneCode(
        code: String,
        rememberDevice: Boolean,
        onVerified: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.verifyTFAPhoneCode(
                code,
                resolvableContext.value!!,
                rememberDevice
            )
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onVerified()
                }

                AuthState.ERROR, AuthState.INTERRUPTED -> {
                    onFailedWith(authResponse.toDisplayError())
                }
            }
        }
    }

    //endregion

    //region TOTP

    override fun registerNewAuthenticatorApp(
        onQACode: (Bitmap) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.registerNewAuthenticatorApp(resolvableContext.value!!)
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    updateResolvableContext(authResponse.resolvable()!!)
                    onQACode(
                        decodeImage(authResponse.resolvable()?.tfa?.qrCode ?: "")
                    )
                }

                AuthState.ERROR, AuthState.INTERRUPTED -> {
                    onFailedWith(authResponse.toDisplayError())
                }
            }

        }
    }

    private fun decodeImage(encodedImage: String): Bitmap {
        // Decoding the image received (Base64).
        val decoded = Base64.decode(encodedImage.split(",".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()[1], Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
    }

    override fun verifyTOTPCode(
        code: String,
        onVerificationSuccess: (IAuthResponse) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.verifyTotpCode(
                code, resolvableContext.value!!, false,
            )
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onVerificationSuccess(authResponse)
                }

                AuthState.ERROR, AuthState.INTERRUPTED -> {
                    onFailedWith(authResponse.toDisplayError())
                }
            }
        }
    }

    //endregion
}