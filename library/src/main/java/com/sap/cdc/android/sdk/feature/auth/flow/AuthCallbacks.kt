package com.sap.cdc.android.sdk.feature.auth.flow

import com.sap.cdc.android.sdk.feature.auth.model.LinkEntities
import com.sap.cdc.android.sdk.feature.auth.model.TFAEmailEntity
import com.sap.cdc.android.sdk.feature.auth.model.TFAPhoneEntity
import com.sap.cdc.android.sdk.feature.auth.model.TFAProvidersEntity

data class AuthSuccess(val userData: Map<String, Any>)

data class AuthError(
    val message: String,
    val code: String?,
    val details: String? = null,
    val asJson: String? = null
)

enum class TwoFactorInitiator {
    REGISTRATION, VERIFICATION
}

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

data class OTPContext(var vToken: String? = null)

data class RegistrationContext(var missingRequiredFields: List<String>? = null)

data class LinkingContext(
    var provider: String? = null,
    var authToken: String? = null,
    var conflictingAccounts: LinkEntities? = null
)

data class AuthCallbacks(
    var onSuccess: ((AuthSuccess) -> Unit)? = null,                         // Called when login succeeds
    var onError: ((AuthError) -> Unit)? = null,                             // Called when authentication fails
    var onTwoFactorRequired: ((TwoFactorContext) -> Unit)? = null,          // Called when server requires TFA
    var onOTPRequired: ((OTPContext) -> Unit)? = null,                      // Called when server requires OTP
    var onPendingRegistration: ((RegistrationContext) -> Unit)? = null,     // Called when server requires additional registration info
    var onLinkingRequired: ((LinkingContext) -> Unit)? = null,
    var onCaptchaRequired: () -> Unit? = { /* no-op */ },                                        // Called when server requires linking
)