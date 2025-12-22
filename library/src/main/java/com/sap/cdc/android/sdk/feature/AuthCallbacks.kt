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

    // Override transformers - single override per type for simplified architecture
    private var _onSuccessOverride: (suspend (AuthSuccess) -> AuthResult)? = null,
    private var _onErrorOverride: (suspend (AuthError) -> AuthResult)? = null,
    private var _onPendingRegistrationOverride: (suspend (RegistrationContext) -> AuthResult)? = null,
    private var _onLinkingRequiredOverride: (suspend (LinkingContext) -> AuthResult)? = null,
    private var _onTwoFactorRequiredOverride: (suspend (TwoFactorContext) -> AuthResult)? = null,
    private var _onOTPRequiredOverride: (suspend (OTPContext) -> AuthResult)? = null,
    private var _onCaptchaRequiredOverride: (suspend (Unit) -> AuthResult)? = null
) {

    // Public setters that append to the chain (backward compatible)
    var onSuccess: ((AuthSuccess) -> Unit)?
        get() = if (_onSuccess.isEmpty() && _onSuccessOverride == null && !hasUniversalOverride()) {
            null
        } else { authSuccess ->
            if (_onSuccessOverride != null || hasUniversalOverride()) {
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
        get() = if (_onError.isEmpty() && _onErrorOverride == null && !hasUniversalOverride()) {
            null
        } else { authError ->
            if (_onErrorOverride != null || hasUniversalOverride()) {
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
        get() = if (_onPendingRegistration.isEmpty() && _onPendingRegistrationOverride == null && !hasUniversalOverride()) {
            null
        } else { context ->
            if (_onPendingRegistrationOverride != null || hasUniversalOverride()) {
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
        get() = if (_onLinkingRequired.isEmpty() && _onLinkingRequiredOverride == null && !hasUniversalOverride()) {
            null
        } else { context ->
            if (_onLinkingRequiredOverride != null || hasUniversalOverride()) {
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
        get() = if (_onTwoFactorRequired.isEmpty() && _onTwoFactorRequiredOverride == null && !hasUniversalOverride()) {
            null
        } else { context ->
            if (_onTwoFactorRequiredOverride != null || hasUniversalOverride()) {
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
        get() = if (_onOTPRequired.isEmpty() && _onOTPRequiredOverride == null && !hasUniversalOverride()) {
            null
        } else { context ->
            if (_onOTPRequiredOverride != null || hasUniversalOverride()) {
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
        get() = if (_onCaptchaRequired.isEmpty() && _onCaptchaRequiredOverride == null && !hasUniversalOverride()) {
            null
        } else {
            {
                if (_onCaptchaRequiredOverride != null || hasUniversalOverride()) {
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

    // Override methods for response transformation with type transformation support
    /**
     * Register an override transformer for Success results.
     * 
     * This override can transform the Success result into ANY AuthResult type, enabling:
     * - Data enrichment (Success → Success with enriched data)
     * - Error handling (Success → Error if validation fails)
     * - Chained operations (e.g., automatic account linking)
     * 
     * The override persists through authentication interruptions, only executing when
     * an actual Success result is received.
     * 
     * **Single Override Restriction:** Only ONE Success override is allowed per callback flow.
     * Attempting to register multiple overrides will throw IllegalArgumentException.
     * 
     * @param transformer Suspend function that transforms AuthSuccess to any AuthResult type
     * @throws IllegalArgumentException if an override is already registered
     */
    fun doOnSuccessAndOverride(transformer: suspend (AuthSuccess) -> AuthResult) = apply {
        require(_onSuccessOverride == null) {
            "Only one Success override allowed per callback flow. " +
            "Multiple overrides detected - consolidate logic into a single override."
        }
        _onSuccessOverride = transformer
    }

    /**
     * Register an override transformer for Error results.
     * 
     * **Single Override Restriction:** Only ONE Error override is allowed per callback flow.
     * 
     * @param transformer Suspend function that transforms AuthError to any AuthResult type
     * @throws IllegalArgumentException if an override is already registered
     */
    fun doOnErrorAndOverride(transformer: suspend (AuthError) -> AuthResult) = apply {
        require(_onErrorOverride == null) {
            "Only one Error override allowed per callback flow. " +
            "Multiple overrides detected - consolidate logic into a single override."
        }
        _onErrorOverride = transformer
    }

    /**
     * Register an override transformer for PendingRegistration results.
     * 
     * **Single Override Restriction:** Only ONE PendingRegistration override is allowed per callback flow.
     * 
     * @param transformer Suspend function that transforms RegistrationContext to any AuthResult type
     * @throws IllegalArgumentException if an override is already registered
     */
    fun doOnPendingRegistrationAndOverride(transformer: suspend (RegistrationContext) -> AuthResult) = apply {
        require(_onPendingRegistrationOverride == null) {
            "Only one PendingRegistration override allowed per callback flow. " +
            "Multiple overrides detected - consolidate logic into a single override."
        }
        _onPendingRegistrationOverride = transformer
    }

    /**
     * Register an override transformer for LinkingRequired results.
     * 
     * **Single Override Restriction:** Only ONE LinkingRequired override is allowed per callback flow.
     * 
     * @param transformer Suspend function that transforms LinkingContext to any AuthResult type
     * @throws IllegalArgumentException if an override is already registered
     */
    fun doOnLinkingRequiredAndOverride(transformer: suspend (LinkingContext) -> AuthResult) = apply {
        require(_onLinkingRequiredOverride == null) {
            "Only one LinkingRequired override allowed per callback flow. " +
            "Multiple overrides detected - consolidate logic into a single override."
        }
        _onLinkingRequiredOverride = transformer
    }

    /**
     * Register an override transformer for TwoFactorRequired results.
     * 
     * **Single Override Restriction:** Only ONE TwoFactorRequired override is allowed per callback flow.
     * 
     * @param transformer Suspend function that transforms TwoFactorContext to any AuthResult type
     * @throws IllegalArgumentException if an override is already registered
     */
    fun doOnTwoFactorRequiredAndOverride(transformer: suspend (TwoFactorContext) -> AuthResult) = apply {
        require(_onTwoFactorRequiredOverride == null) {
            "Only one TwoFactorRequired override allowed per callback flow. " +
            "Multiple overrides detected - consolidate logic into a single override."
        }
        _onTwoFactorRequiredOverride = transformer
    }

    /**
     * Register an override transformer for OTPRequired results.
     * 
     * **Single Override Restriction:** Only ONE OTPRequired override is allowed per callback flow.
     * 
     * @param transformer Suspend function that transforms OTPContext to any AuthResult type
     * @throws IllegalArgumentException if an override is already registered
     */
    fun doOnOTPRequiredAndOverride(transformer: suspend (OTPContext) -> AuthResult) = apply {
        require(_onOTPRequiredOverride == null) {
            "Only one OTPRequired override allowed per callback flow. " +
            "Multiple overrides detected - consolidate logic into a single override."
        }
        _onOTPRequiredOverride = transformer
    }

    /**
     * Register an override transformer for CaptchaRequired results.
     * 
     * **Single Override Restriction:** Only ONE CaptchaRequired override is allowed per callback flow.
     * 
     * @param transformer Suspend function that transforms Unit to any AuthResult type
     * @throws IllegalArgumentException if an override is already registered
     */
    fun doOnCaptchaRequiredAndOverride(transformer: suspend (Unit) -> AuthResult) = apply {
        require(_onCaptchaRequiredOverride == null) {
            "Only one CaptchaRequired override allowed per callback flow. " +
            "Multiple overrides detected - consolidate logic into a single override."
        }
        _onCaptchaRequiredOverride = transformer
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

    // Recursion prevention flag for re-routing
    private var isRerouting = false

    // Internal execution methods - apply individual overrides and execute callbacks
    // (Universal override is already applied by the router before reaching here)
    private suspend fun executeOnSuccessInternal(authSuccess: AuthSuccess) {
        // Apply single override if present
        val result = _onSuccessOverride?.invoke(authSuccess) 
            ?: AuthResult.Success(authSuccess)
        
        // Check if type changed and not already re-routing
        if (result !is AuthResult.Success && !isRerouting) {
            isRerouting = true
            executeCallback(result)  // Re-route to correct handler
            isRerouting = false
            return
        }
        
        // Still Success - execute callbacks
        val finalSuccess = (result as AuthResult.Success).authSuccess
        _onSuccess.forEach { it(finalSuccess) }
    }

    private suspend fun executeOnErrorInternal(authError: AuthError) {
        val result = _onErrorOverride?.invoke(authError) 
            ?: AuthResult.Error(authError)
        
        if (result !is AuthResult.Error && !isRerouting) {
            isRerouting = true
            executeCallback(result)
            isRerouting = false
            return
        }
        
        val finalError = (result as AuthResult.Error).authError
        _onError.forEach { it(finalError) }
    }

    private suspend fun executeOnPendingRegistrationInternal(context: RegistrationContext) {
        val result = _onPendingRegistrationOverride?.invoke(context) 
            ?: AuthResult.PendingRegistration(context)
        
        if (result !is AuthResult.PendingRegistration && !isRerouting) {
            isRerouting = true
            executeCallback(result)
            isRerouting = false
            return
        }
        
        val finalContext = (result as AuthResult.PendingRegistration).context
        _onPendingRegistration.forEach { it(finalContext) }
    }

    private suspend fun executeOnLinkingRequiredInternal(context: LinkingContext) {
        val result = _onLinkingRequiredOverride?.invoke(context) 
            ?: AuthResult.LinkingRequired(context)
        
        if (result !is AuthResult.LinkingRequired && !isRerouting) {
            isRerouting = true
            executeCallback(result)
            isRerouting = false
            return
        }
        
        val finalContext = (result as AuthResult.LinkingRequired).context
        _onLinkingRequired.forEach { it(finalContext) }
    }

    private suspend fun executeOnTwoFactorRequiredInternal(context: TwoFactorContext) {
        val result = _onTwoFactorRequiredOverride?.invoke(context) 
            ?: AuthResult.TwoFactorRequired(context)
        
        if (result !is AuthResult.TwoFactorRequired && !isRerouting) {
            isRerouting = true
            executeCallback(result)
            isRerouting = false
            return
        }
        
        val finalContext = (result as AuthResult.TwoFactorRequired).context
        _onTwoFactorRequired.forEach { it(finalContext) }
    }

    private suspend fun executeOnOTPRequiredInternal(context: OTPContext) {
        val result = _onOTPRequiredOverride?.invoke(context) 
            ?: AuthResult.OTPRequired(context)
        
        if (result !is AuthResult.OTPRequired && !isRerouting) {
            isRerouting = true
            executeCallback(result)
            isRerouting = false
            return
        }
        
        val finalContext = (result as AuthResult.OTPRequired).context
        _onOTPRequired.forEach { it(finalContext) }
    }

    private suspend fun executeOnCaptchaRequiredInternal() {
        val result = _onCaptchaRequiredOverride?.invoke(Unit) 
            ?: AuthResult.CaptchaRequired
        
        if (result !is AuthResult.CaptchaRequired && !isRerouting) {
            isRerouting = true
            executeCallback(result)
            isRerouting = false
            return
        }
        
        // Execute all callbacks
        _onCaptchaRequired.forEach { it() }
    }

    // Helper methods to check if async execution is required
    fun hasOverrideTransformers(): Boolean {
        return _onSuccessOverride != null ||
                _onErrorOverride != null ||
                _onPendingRegistrationOverride != null ||
                _onLinkingRequiredOverride != null ||
                _onTwoFactorRequiredOverride != null ||
                _onOTPRequiredOverride != null ||
                _onCaptchaRequiredOverride != null ||
                hasUniversalOverride()
    }
}
