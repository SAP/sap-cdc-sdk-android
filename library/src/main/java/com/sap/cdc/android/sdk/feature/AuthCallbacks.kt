package com.sap.cdc.android.sdk.feature

import com.sap.cdc.android.sdk.feature.account.LinkEntities
import com.sap.cdc.android.sdk.feature.tfa.TFAEmailEntity
import com.sap.cdc.android.sdk.feature.tfa.TFAPhoneEntity
import com.sap.cdc.android.sdk.feature.tfa.TFAProvidersEntity
import kotlinx.serialization.Serializable

sealed class AuthResult {
    data class Success(val authSuccess: AuthSuccess) : AuthResult()
    data class Error(val authError: AuthError) : AuthResult()
    data class PendingRegistration(val context: RegistrationContext) : AuthResult()
    data class LinkingRequired(val context: LinkingContext) : AuthResult()
    data class TwoFactorRequired(val context: TwoFactorContext) : AuthResult()
    data class OTPRequired(val context: OTPContext) : AuthResult()
    object CaptchaRequired : AuthResult()
}

data class AuthSuccess(
    val jsonData: String,
    val userData: Map<String, Any>
)

@Serializable
data class AuthError(
    val message: String,
    val code: String? = null,
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
    val regToken: String? = null,
    val originatingError: AuthError? = null
)

@Serializable
data class OTPContext(
    var vToken: String? = null,
    val regToken: String? = null,
    val originatingError: AuthError? = null
)

@Serializable
data class RegistrationContext(
    var missingRequiredFields: List<String>? = null,
    val regToken: String? = null,
    val originatingError: AuthError? = null
)

@Serializable
data class LinkingContext(
    var provider: String? = null,
    var authToken: String? = null,
    var conflictingAccounts: LinkEntities? = null,
    val regToken: String? = null,
    val originatingError: AuthError? = null
)

data class AuthCallbacks(
    // Original callback lists (backward compatible)
    private var _onSuccess: MutableList<(AuthSuccess) -> Unit> = mutableListOf(),
    private var _onError: MutableList<(AuthError) -> Unit> = mutableListOf(),
    private var _onPendingRegistration: MutableList<(RegistrationContext) -> Unit> = mutableListOf(),
    private var _onLinkingRequired: MutableList<(LinkingContext) -> Unit> = mutableListOf(),
    private var _onTwoFactorRequired: MutableList<(TwoFactorContext) -> Unit> = mutableListOf(),
    private var _onOTPRequired: MutableList<(OTPContext) -> Unit> = mutableListOf(),
    private var _onCaptchaRequired: MutableList<() -> Unit> = mutableListOf(),
    
    // NEW: Context update callbacks for multi-step flows
    private var _onTwoFactorContextUpdated: MutableList<(TwoFactorContext) -> Unit> = mutableListOf(),
    private var _onRegistrationContextUpdated: MutableList<(RegistrationContext) -> Unit> = mutableListOf(),
    private var _onLinkingContextUpdated: MutableList<(LinkingContext) -> Unit> = mutableListOf(),
    private var _onOTPContextUpdated: MutableList<(OTPContext) -> Unit> = mutableListOf(),
    
    // NEW: Override transformers stored separately
    private var _onSuccessOverrides: MutableList<suspend (AuthSuccess) -> AuthSuccess> = mutableListOf(),
    private var _onErrorOverrides: MutableList<suspend (AuthError) -> AuthError> = mutableListOf(),
    private var _onPendingRegistrationOverrides: MutableList<suspend (RegistrationContext) -> RegistrationContext> = mutableListOf(),
    private var _onLinkingRequiredOverrides: MutableList<suspend (LinkingContext) -> LinkingContext> = mutableListOf(),
    private var _onTwoFactorRequiredOverrides: MutableList<suspend (TwoFactorContext) -> TwoFactorContext> = mutableListOf(),
    private var _onOTPRequiredOverrides: MutableList<suspend (OTPContext) -> OTPContext> = mutableListOf(),
    private var _onCaptchaRequiredOverrides: MutableList<suspend (Unit) -> Unit> = mutableListOf()
) {
    
    // Public setters that append to the chain (backward compatible)
    var onSuccess: ((AuthSuccess) -> Unit)?
        get() = if (_onSuccess.isEmpty()) null else { authSuccess -> 
            // Handle universal override internally
            if (hasUniversalOverride()) {
                // Convert to AuthResult, apply universal override, then route back
                val initialResult = AuthResult.Success(authSuccess)
                // This needs to be handled asynchronously, but we can't do that in a getter
                // The universal override will be handled when the callback is actually invoked
                _onSuccess.forEach { it(authSuccess) }
            } else if (_onSuccessOverrides.isNotEmpty()) {
                throw IllegalStateException("Cannot execute synchronously when override transformers are present. Use suspend execution.")
            } else {
                _onSuccess.forEach { it(authSuccess) }
            }
        }
        set(value) {
            value?.let { _onSuccess.add(it) }
        }

    var onError: ((AuthError) -> Unit)?
        get() = if (_onError.isEmpty()) null else { authError -> 
            // Handle universal override internally
            if (hasUniversalOverride()) {
                _onError.forEach { it(authError) }
            } else if (_onErrorOverrides.isNotEmpty()) {
                throw IllegalStateException("Cannot execute synchronously when override transformers are present. Use suspend execution.")
            } else {
                _onError.forEach { it(authError) }
            }
        }
        set(value) {
            value?.let { _onError.add(it) }
        }

    var onPendingRegistration: ((RegistrationContext) -> Unit)?
        get() = if (_onPendingRegistration.isEmpty()) null else { context -> 
            if (_onPendingRegistrationOverrides.isNotEmpty()) {
                throw IllegalStateException("Cannot execute synchronously when override transformers are present. Use suspend execution.")
            }
            _onPendingRegistration.forEach { it(context) }
        }
        set(value) {
            value?.let { _onPendingRegistration.add(it) }
        }

    var onLinkingRequired: ((LinkingContext) -> Unit)?
        get() = if (_onLinkingRequired.isEmpty()) null else { context -> 
            if (_onLinkingRequiredOverrides.isNotEmpty()) {
                throw IllegalStateException("Cannot execute synchronously when override transformers are present. Use suspend execution.")
            }
            _onLinkingRequired.forEach { it(context) }
        }
        set(value) {
            value?.let { _onLinkingRequired.add(it) }
        }

    var onTwoFactorRequired: ((TwoFactorContext) -> Unit)?
        get() = if (_onTwoFactorRequired.isEmpty()) null else { context -> 
            if (_onTwoFactorRequiredOverrides.isNotEmpty()) {
                throw IllegalStateException("Cannot execute synchronously when override transformers are present. Use suspend execution.")
            }
            _onTwoFactorRequired.forEach { it(context) }
        }
        set(value) {
            value?.let { _onTwoFactorRequired.add(it) }
        }

    var onOTPRequired: ((OTPContext) -> Unit)?
        get() = if (_onOTPRequired.isEmpty()) null else { context -> 
            if (_onOTPRequiredOverrides.isNotEmpty()) {
                throw IllegalStateException("Cannot execute synchronously when override transformers are present. Use suspend execution.")
            }
            _onOTPRequired.forEach { it(context) }
        }
        set(value) {
            value?.let { _onOTPRequired.add(it) }
        }

    var onCaptchaRequired: (() -> Unit)?
        get() = if (_onCaptchaRequired.isEmpty()) null else {
            { 
                if (_onCaptchaRequiredOverrides.isNotEmpty()) {
                    throw IllegalStateException("Cannot execute synchronously when override transformers are present. Use suspend execution.")
                }
                _onCaptchaRequired.forEach { it() }
            }
        }
        set(value) {
            value?.let { _onCaptchaRequired.add(it) }
        }

    // NEW: Context update callback setters
    /**
     * Called when TwoFactor context is updated with enriched data.
     * 
     * ✅ RECOMMENDED for TwoFactor UI: This provides ready-to-use enriched context.
     * 
     * Benefits:
     * - Automatic SDK enrichment (emails, tokens, etc.)
     * - Progressive context updates as flow progresses
     * - Type-safe TwoFactor-specific data
     * - Ready for immediate UI binding
     * 
     * This callback is called IN ADDITION to onSuccess, not instead of it.
     * Use this when you want SDK to handle context enrichment automatically.
     */
    var onTwoFactorContextUpdated: ((TwoFactorContext) -> Unit)?
        get() = if (_onTwoFactorContextUpdated.isEmpty()) null else { context -> 
            _onTwoFactorContextUpdated.forEach { it(context) }
        }
        set(value) {
            value?.let { _onTwoFactorContextUpdated.add(it) }
        }

    /**
     * Called when Registration context is updated with enriched data.
     * 
     * This callback provides enriched registration context for multi-step registration flows.
     * Called IN ADDITION to onSuccess, not instead of it.
     */
    var onRegistrationContextUpdated: ((RegistrationContext) -> Unit)?
        get() = if (_onRegistrationContextUpdated.isEmpty()) null else { context -> 
            _onRegistrationContextUpdated.forEach { it(context) }
        }
        set(value) {
            value?.let { _onRegistrationContextUpdated.add(it) }
        }

    /**
     * Called when Linking context is updated with enriched data.
     * 
     * This callback provides enriched linking context for account linking flows.
     * Called IN ADDITION to onSuccess, not instead of it.
     */
    var onLinkingContextUpdated: ((LinkingContext) -> Unit)?
        get() = if (_onLinkingContextUpdated.isEmpty()) null else { context -> 
            _onLinkingContextUpdated.forEach { it(context) }
        }
        set(value) {
            value?.let { _onLinkingContextUpdated.add(it) }
        }

    /**
     * Called when OTP context is updated with enriched data.
     * 
     * This callback provides enriched OTP context for one-time password flows.
     * Called IN ADDITION to onSuccess, not instead of it.
     */
    var onOTPContextUpdated: ((OTPContext) -> Unit)?
        get() = if (_onOTPContextUpdated.isEmpty()) null else { context -> 
            _onOTPContextUpdated.forEach { it(context) }
        }
        set(value) {
            value?.let { _onOTPContextUpdated.add(it) }
        }

    // Methods for side-effect callbacks (execute before original callbacks) - backward compatible
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

    // NEW: Context update callback methods
    fun doOnTwoFactorContextUpdated(callback: (TwoFactorContext) -> Unit) = apply {
        _onTwoFactorContextUpdated.add(0, callback)
    }

    fun doOnRegistrationContextUpdated(callback: (RegistrationContext) -> Unit) = apply {
        _onRegistrationContextUpdated.add(0, callback)
    }

    fun doOnLinkingContextUpdated(callback: (LinkingContext) -> Unit) = apply {
        _onLinkingContextUpdated.add(0, callback)
    }

    fun doOnOTPContextUpdated(callback: (OTPContext) -> Unit) = apply {
        _onOTPContextUpdated.add(0, callback)
    }

    // NEW: Override methods for response transformation
    fun doOnSuccessAndOverride(transformer: suspend (AuthSuccess) -> AuthSuccess) = apply {
        _onSuccessOverrides.add(0, transformer)
    }

    fun doOnErrorAndOverride(transformer: suspend (AuthError) -> AuthError) = apply {
        _onErrorOverrides.add(0, transformer)
    }

    fun doOnPendingRegistrationAndOverride(transformer: suspend (RegistrationContext) -> RegistrationContext) = apply {
        _onPendingRegistrationOverrides.add(0, transformer)
    }

    fun doOnLinkingRequiredAndOverride(transformer: suspend (LinkingContext) -> LinkingContext) = apply {
        _onLinkingRequiredOverrides.add(0, transformer)
    }

    fun doOnTwoFactorRequiredAndOverride(transformer: suspend (TwoFactorContext) -> TwoFactorContext) = apply {
        _onTwoFactorRequiredOverrides.add(0, transformer)
    }

    fun doOnOTPRequiredAndOverride(transformer: suspend (OTPContext) -> OTPContext) = apply {
        _onOTPRequiredOverrides.add(0, transformer)
    }

    fun doOnCaptchaRequiredAndOverride(transformer: suspend (Unit) -> Unit) = apply {
        _onCaptchaRequiredOverrides.add(0, transformer)
    }

    // NEW: Universal override method for any callback type transformation
    private var _universalOverride: (suspend (AuthResult) -> AuthResult)? = null

    fun doOnAnyAndOverride(transformer: suspend (AuthResult) -> AuthResult) = apply {
        _universalOverride = transformer
    }

    suspend fun executeUniversalOverride(authResult: AuthResult): AuthResult {
        return _universalOverride?.invoke(authResult) ?: authResult
    }

    fun hasUniversalOverride(): Boolean = _universalOverride != null

    // NEW: Suspend execution methods for async callback chains
    suspend fun executeOnSuccess(authSuccess: AuthSuccess): AuthSuccess {
        var currentValue = authSuccess
        
        // Apply all override transformers first
        for (transformer in _onSuccessOverrides) {
            currentValue = transformer(currentValue)
        }
        
        // Execute all callbacks with the final transformed value
        _onSuccess.forEach { it(currentValue) }
        
        return currentValue
    }

    suspend fun executeOnError(authError: AuthError): AuthError {
        var currentValue = authError
        
        // Apply all override transformers first
        for (transformer in _onErrorOverrides) {
            currentValue = transformer(currentValue)
        }
        
        // Execute all callbacks with the final transformed value
        _onError.forEach { it(currentValue) }
        
        return currentValue
    }

    suspend fun executeOnPendingRegistration(context: RegistrationContext): RegistrationContext {
        var currentValue = context
        
        // Apply all override transformers first
        for (transformer in _onPendingRegistrationOverrides) {
            currentValue = transformer(currentValue)
        }
        
        // Execute all callbacks with the final transformed value
        _onPendingRegistration.forEach { it(currentValue) }
        
        return currentValue
    }

    suspend fun executeOnLinkingRequired(context: LinkingContext): LinkingContext {
        var currentValue = context
        
        // Apply all override transformers first
        for (transformer in _onLinkingRequiredOverrides) {
            currentValue = transformer(currentValue)
        }
        
        // Execute all callbacks with the final transformed value
        _onLinkingRequired.forEach { it(currentValue) }
        
        return currentValue
    }

    suspend fun executeOnTwoFactorRequired(context: TwoFactorContext): TwoFactorContext {
        var currentValue = context
        
        // Apply all override transformers first
        for (transformer in _onTwoFactorRequiredOverrides) {
            currentValue = transformer(currentValue)
        }
        
        // Execute all callbacks with the final transformed value
        _onTwoFactorRequired.forEach { it(currentValue) }
        
        return currentValue
    }

    suspend fun executeOnOTPRequired(context: OTPContext): OTPContext {
        var currentValue = context
        
        // Apply all override transformers first
        for (transformer in _onOTPRequiredOverrides) {
            currentValue = transformer(currentValue)
        }
        
        // Execute all callbacks with the final transformed value
        _onOTPRequired.forEach { it(currentValue) }
        
        return currentValue
    }

    suspend fun executeOnCaptchaRequired() {
        var currentValue = Unit
        
        // Apply all override transformers first
        for (transformer in _onCaptchaRequiredOverrides) {
            currentValue = transformer(currentValue)
        }
        
        // Execute all callbacks
        _onCaptchaRequired.forEach { it() }
    }

    // Helper methods to check if async execution is required
    fun hasOverrideTransformers(): Boolean {
        return _onSuccessOverrides.isNotEmpty() ||
               _onErrorOverrides.isNotEmpty() ||
               _onPendingRegistrationOverrides.isNotEmpty() ||
               _onLinkingRequiredOverrides.isNotEmpty() ||
               _onTwoFactorRequiredOverrides.isNotEmpty() ||
               _onOTPRequiredOverrides.isNotEmpty() ||
               _onCaptchaRequiredOverrides.isNotEmpty() ||
               hasUniversalOverride()
    }
}
