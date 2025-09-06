package com.sap.cdc.android.sdk.feature.auth.flow

import com.sap.cdc.android.sdk.feature.auth.model.LinkEntities
import com.sap.cdc.android.sdk.feature.auth.model.TFAEmailEntity
import com.sap.cdc.android.sdk.feature.auth.model.TFAPhoneEntity
import com.sap.cdc.android.sdk.feature.auth.model.TFAProvidersEntity
import kotlinx.serialization.Serializable

data class AuthSuccess(
    val jsonData: String,
    val userData: Map<String, Any>
)

data class AuthError(
    val message: String,
    val code: String?,
    val details: String? = null,
    val asJson: String? = null
)

@Serializable
enum class TwoFactorInitiator {
    REGISTRATION, VERIFICATION
}

@Serializable
data class TwoFactorContext(
    var initiator: TwoFactorInitiator? = null,
    var assertion: String? = null,
    var phvToken: String? = null,
    var tfaProviders: TFAProvidersEntity? = null,
    var emails: List<TFAEmailEntity>? = null,
    var phones: List<TFAPhoneEntity>? = null,
    var qrCode: String? = null,
    var sctToken: String? = null,
)

@Serializable
data class OTPContext(var vToken: String? = null)

@Serializable
data class RegistrationContext(
    var regToken: String? = null,
    var missingRequiredFields: List<String>? = null)

@Serializable
data class LinkingContext(
    var provider: String? = null,
    var authToken: String? = null,
    var conflictingAccounts: LinkEntities? = null
)

data class AuthCallbacks(
    private var _onSuccess: MutableList<(AuthSuccess) -> Unit> = mutableListOf(),
    private var _onError: MutableList<(AuthError) -> Unit> = mutableListOf(),
    private var _onPendingRegistration: MutableList<(RegistrationContext) -> Unit> = mutableListOf(),
    private var _onLinkingRequired: MutableList<(LinkingContext) -> Unit> = mutableListOf(),
    private var _onTwoFactorRequired: MutableList<(TwoFactorContext) -> Unit> = mutableListOf(),
    private var _onOTPRequired: MutableList<(OTPContext) -> Unit> = mutableListOf(),
    private var _onCaptchaRequired: MutableList<() -> Unit> = mutableListOf()
) {
    // Public setters that append to the chain
    var onSuccess: ((AuthSuccess) -> Unit)?
        get() = if (_onSuccess.isEmpty()) null else { authSuccess -> _onSuccess.forEach { it(authSuccess) } }
        set(value) {
            value?.let { _onSuccess.add(it) }
        }

    var onError: ((AuthError) -> Unit)?
        get() = if (_onError.isEmpty()) null else { authError -> _onError.forEach { it(authError) } }
        set(value) {
            value?.let { _onError.add(it) }
        }

    var onPendingRegistration: ((RegistrationContext) -> Unit)?
        get() = if (_onPendingRegistration.isEmpty()) null else { context -> _onPendingRegistration.forEach { it(context) } }
        set(value) {
            value?.let { _onPendingRegistration.add(it) }
        }

    var onLinkingRequired: ((LinkingContext) -> Unit)?
        get() = if (_onLinkingRequired.isEmpty()) null else { context -> _onLinkingRequired.forEach { it(context) } }
        set(value) {
            value?.let { _onLinkingRequired.add(it) }
        }

    var onTwoFactorRequired: ((TwoFactorContext) -> Unit)?
        get() = if (_onTwoFactorRequired.isEmpty()) null else { context -> _onTwoFactorRequired.forEach { it(context) } }
        set(value) {
            value?.let { _onTwoFactorRequired.add(it) }
        }

    var onOTPRequired: ((OTPContext) -> Unit)?
        get() = if (_onOTPRequired.isEmpty()) null else { context -> _onOTPRequired.forEach { it(context) } }
        set(value) {
            value?.let { _onOTPRequired.add(it) }
        }

    var onCaptchaRequired: (() -> Unit)?
        get() = if (_onCaptchaRequired.isEmpty()) null else {
            { _onCaptchaRequired.forEach { it() } }
        }
        set(value) {
            value?.let { _onCaptchaRequired.add(it) }
        }

    // Methods for side-effect callbacks (execute before original callbacks)
    fun doOnSuccess(callback: (AuthSuccess) -> Unit) = apply {
        _onSuccess.add(0, callback)
    }

    fun doOnError(callback: (AuthError) -> Unit) = apply {
        _onError.add(0, callback)
    }

    fun doOnPendingRegistration(callback: (RegistrationContext) -> Unit) = apply {
        _onPendingRegistration.add(0, callback)
    }

    fun doOnLinkingRequired(callback: (LinkingContext) -> Unit) = apply {
        _onLinkingRequired.add(0, callback)
    }

    fun doOnTwoFactorRequired(callback: (TwoFactorContext) -> Unit) = apply {
        _onTwoFactorRequired.add(0, callback)
    }

    fun doOnOTPRequired(callback: (OTPContext) -> Unit) = apply {
        _onOTPRequired.add(0, callback)
    }

    fun doOnCaptchaRequired(callback: () -> Unit) = apply {
        _onCaptchaRequired.add(0, callback)
    }
}
