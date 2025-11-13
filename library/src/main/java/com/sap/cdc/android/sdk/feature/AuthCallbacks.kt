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
    val data: Map<String, Any>
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
        get() = if (_onSuccess.isEmpty() && _onSuccessOverrides.isEmpty() && !hasUniversalOverride()) {
            null
        } else { authSuccess ->
            if (_onSuccessOverrides.isNotEmpty() || hasUniversalOverride()) {
                // Auto-bridge to async execution
                kotlinx.coroutines.runBlocking {
                    executeOnSuccess(authSuccess)
                }
            } else {
                // Pure sync execution for backward compatibility
                _onSuccess.forEach { it(authSuccess) }
            }
        }
        set(value) {
            value?.let { _onSuccess.add(it) }
        }

    var onError: ((AuthError) -> Unit)?
        get() = if (_onError.isEmpty() && _onErrorOverrides.isEmpty() && !hasUniversalOverride()) {
            null
        } else { authError ->
            if (_onErrorOverrides.isNotEmpty() || hasUniversalOverride()) {
                // Auto-bridge to async execution
                kotlinx.coroutines.runBlocking {
                    executeOnError(authError)
                }
            } else {
                // Pure sync execution for backward compatibility
                _onError.forEach { it(authError) }
            }
        }
        set(value) {
            value?.let { _onError.add(it) }
        }

    var onPendingRegistration: ((RegistrationContext) -> Unit)?
        get() = if (_onPendingRegistration.isEmpty() && _onPendingRegistrationOverrides.isEmpty() && !hasUniversalOverride()) {
            null
        } else { context ->
            if (_onPendingRegistrationOverrides.isNotEmpty() || hasUniversalOverride()) {
                // Auto-bridge to async execution
                kotlinx.coroutines.runBlocking {
                    executeOnPendingRegistration(context)
                }
            } else {
                // Pure sync execution for backward compatibility
                _onPendingRegistration.forEach { it(context) }
            }
        }
        set(value) {
            value?.let { _onPendingRegistration.add(it) }
        }

    var onLinkingRequired: ((LinkingContext) -> Unit)?
        get() = if (_onLinkingRequired.isEmpty() && _onLinkingRequiredOverrides.isEmpty() && !hasUniversalOverride()) {
            null
        } else { context ->
            if (_onLinkingRequiredOverrides.isNotEmpty() || hasUniversalOverride()) {
                // Auto-bridge to async execution
                kotlinx.coroutines.runBlocking {
                    executeOnLinkingRequired(context)
                }
            } else {
                // Pure sync execution for backward compatibility
                _onLinkingRequired.forEach { it(context) }
            }
        }
        set(value) {
            value?.let { _onLinkingRequired.add(it) }
        }

    var onTwoFactorRequired: ((TwoFactorContext) -> Unit)?
        get() = if (_onTwoFactorRequired.isEmpty() && _onTwoFactorRequiredOverrides.isEmpty() && !hasUniversalOverride()) {
            null
        } else { context ->
            if (_onTwoFactorRequiredOverrides.isNotEmpty() || hasUniversalOverride()) {
                // Auto-bridge to async execution
                kotlinx.coroutines.runBlocking {
                    executeOnTwoFactorRequired(context)
                }
            } else {
                // Pure sync execution for backward compatibility
                _onTwoFactorRequired.forEach { it(context) }
            }
        }
        set(value) {
            value?.let { _onTwoFactorRequired.add(it) }
        }

    var onOTPRequired: ((OTPContext) -> Unit)?
        get() = if (_onOTPRequired.isEmpty() && _onOTPRequiredOverrides.isEmpty() && !hasUniversalOverride()) {
            null
        } else { context ->
            if (_onOTPRequiredOverrides.isNotEmpty() || hasUniversalOverride()) {
                // Auto-bridge to async execution
                kotlinx.coroutines.runBlocking {
                    executeOnOTPRequired(context)
                }
            } else {
                // Pure sync execution for backward compatibility
                _onOTPRequired.forEach { it(context) }
            }
        }
        set(value) {
            value?.let { _onOTPRequired.add(it) }
        }

    var onCaptchaRequired: (() -> Unit)?
        get() = if (_onCaptchaRequired.isEmpty() && _onCaptchaRequiredOverrides.isEmpty() && !hasUniversalOverride()) {
            null
        } else {
            {
                if (_onCaptchaRequiredOverrides.isNotEmpty() || hasUniversalOverride()) {
                    // Auto-bridge to async execution
                    kotlinx.coroutines.runBlocking {
                        executeOnCaptchaRequired()
                    }
                } else {
                    // Pure sync execution for backward compatibility
                    _onCaptchaRequired.forEach { it() }
                }
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

    // NEW: Central callback router that applies universal override and routes to correct callback type.
    // This ensures that when universal override changes the result type (e.g., Success → Error),
    // only the appropriate callbacks execute.
    private suspend fun executeCallback(authResult: AuthResult) {
        // Apply universal override first if present
        val finalResult = if (hasUniversalOverride()) {
            executeUniversalOverride(authResult)
        } else {
            authResult
        }

        // Route to correct handler based on FINAL result type
        when (finalResult) {
            is AuthResult.Success -> executeOnSuccessInternal(finalResult.authSuccess)
            is AuthResult.Error -> executeOnErrorInternal(finalResult.authError)
            is AuthResult.PendingRegistration -> executeOnPendingRegistrationInternal(finalResult.context)
            is AuthResult.LinkingRequired -> executeOnLinkingRequiredInternal(finalResult.context)
            is AuthResult.TwoFactorRequired -> executeOnTwoFactorRequiredInternal(finalResult.context)
            is AuthResult.OTPRequired -> executeOnOTPRequiredInternal(finalResult.context)
            is AuthResult.CaptchaRequired -> executeOnCaptchaRequiredInternal()
        }
    }

    // NEW: Public execute methods that wrap inputs and route through central router
    suspend fun executeOnSuccess(authSuccess: AuthSuccess) {
        executeCallback(AuthResult.Success(authSuccess))
    }

    suspend fun executeOnError(authError: AuthError) {
        executeCallback(AuthResult.Error(authError))
    }

    suspend fun executeOnPendingRegistration(context: RegistrationContext) {
        executeCallback(AuthResult.PendingRegistration(context))
    }

    suspend fun executeOnLinkingRequired(context: LinkingContext) {
        executeCallback(AuthResult.LinkingRequired(context))
    }

    suspend fun executeOnTwoFactorRequired(context: TwoFactorContext) {
        executeCallback(AuthResult.TwoFactorRequired(context))
    }

    suspend fun executeOnOTPRequired(context: OTPContext) {
        executeCallback(AuthResult.OTPRequired(context))
    }

    suspend fun executeOnCaptchaRequired() {
        executeCallback(AuthResult.CaptchaRequired)
    }

    // Internal execution methods - apply individual overrides and execute callbacks
    // (Universal override is already applied by the router before reaching here)
    private suspend fun executeOnSuccessInternal(authSuccess: AuthSuccess) {
        var currentValue = authSuccess

        // Apply individual override transformers
        for (transformer in _onSuccessOverrides) {
            currentValue = transformer(currentValue)
        }

        // Execute all callbacks with the final transformed value
        _onSuccess.forEach { it(currentValue) }
    }

    private suspend fun executeOnErrorInternal(authError: AuthError) {
        var currentValue = authError

        // Apply individual override transformers
        for (transformer in _onErrorOverrides) {
            currentValue = transformer(currentValue)
        }

        // Execute all callbacks with the final transformed value
        _onError.forEach { it(currentValue) }
    }

    private suspend fun executeOnPendingRegistrationInternal(context: RegistrationContext) {
        var currentValue = context

        // Apply individual override transformers
        for (transformer in _onPendingRegistrationOverrides) {
            currentValue = transformer(currentValue)
        }

        // Execute all callbacks with the final transformed value
        _onPendingRegistration.forEach { it(currentValue) }
    }

    private suspend fun executeOnLinkingRequiredInternal(context: LinkingContext) {
        var currentValue = context

        // Apply individual override transformers
        for (transformer in _onLinkingRequiredOverrides) {
            currentValue = transformer(currentValue)
        }

        // Execute all callbacks with the final transformed value
        _onLinkingRequired.forEach { it(currentValue) }
    }

    private suspend fun executeOnTwoFactorRequiredInternal(context: TwoFactorContext) {
        var currentValue = context

        // Apply individual override transformers
        for (transformer in _onTwoFactorRequiredOverrides) {
            currentValue = transformer(currentValue)
        }

        // Execute all callbacks with the final transformed value
        _onTwoFactorRequired.forEach { it(currentValue) }
    }

    private suspend fun executeOnOTPRequiredInternal(context: OTPContext) {
        var currentValue = context

        // Apply individual override transformers
        for (transformer in _onOTPRequiredOverrides) {
            currentValue = transformer(currentValue)
        }

        // Execute all callbacks with the final transformed value
        _onOTPRequired.forEach { it(currentValue) }
    }

    private suspend fun executeOnCaptchaRequiredInternal() {
        var currentValue = Unit

        // Apply individual override transformers
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
