package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthResolvable
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.core.api.model.CDCError
import com.sap.cdc.android.sdk.example.cdc.model.AccountEntity
import com.sap.cdc.android.sdk.example.cdc.model.ProfileEntity
import com.sap.cdc.android.sdk.example.extensions.splitFullName
import com.sap.cdc.android.sdk.example.ui.view.flow.OTPType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import java.util.concurrent.TimeUnit

/**
 * Created by Tal Mirmelshtein on 20/06/2024
 * Copyright: SAP LTD.
 */

/**
 * Authentication view model interface.
 */
interface IViewModelAuthentication {

    fun validSession(): Boolean = false

    fun accountInfo(): AccountEntity?

    fun register(
        email: String,
        password: String,
        name: String,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun login(
        email: String,
        password: String,
        onLogin: () -> Unit,
        onLoginIdentifierExists: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun socialSignInWith(
        hostActivity: ComponentActivity,
        provider: IAuthenticationProvider?,
        onLogin: () -> Unit,
        onPendingRegistration: (IAuthResponse?) -> Unit,
        onLoginIdentifierExists: (IAuthResponse?) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun socialWebSignInWith(
        hostActivity: ComponentActivity,
        provider: String,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun getAuthenticationProvider(name: String): IAuthenticationProvider? {
        return null
    }

    fun resolvePendingRegistrationWithMissingProfileFields(
        map: MutableMap<String, String>,
        regToken: String,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun resolveLinkToSiteAccount(
        loginId: String, password: String,
        authResolvable: AuthResolvable,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun resolveLinkToSocialAccount(
        hostActivity: ComponentActivity,
        provider: String,
        authResolvable: AuthResolvable,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun getAccountInfo(
        parameters: MutableMap<String, String>? = mutableMapOf(),
        success: () -> Unit, onFailed: (CDCError) -> Unit
    ) {
        //Stub
    }

    fun updateAccountInfoWith(name: String, success: () -> Unit, onFailed: (CDCError) -> Unit) {
        //Stub
    }

    fun logOut(success: () -> Unit, onFailed: (CDCError) -> Unit) {
        //Stub
    }

    fun otpSignIn(
        otpType: OTPType,
        inputField: String,
        success: (AuthResolvable) -> Unit, onFailed: (CDCError) -> Unit
    ) {
        //Stub
    }

    fun resolveLoginWithCode(
        code: String,
        resolvable: AuthResolvable,
        onLogin: () -> Unit,
        onPendingRegistration: (IAuthResponse?) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        //Stub
    }

    fun startOtpTimer(whenEnded: () -> Unit) {
        // Stub
    }
}

/**
 * Authentication view model.
 * View model is relevant to all authentication views.
 */
class ViewModelAuthentication(context: Context) : ViewModelBase(context), IViewModelAuthentication {

    /**
     * Holding reference to account information object.
     */
    private var accountInfo by mutableStateOf<AccountEntity?>(null)

    /**
     * Getter for account information view model interactions.
     */
    override fun accountInfo(): AccountEntity? = accountInfo

    /**
     * Check Identity session state.
     */
    override
    fun validSession(): Boolean = identityService.getSession() != null

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    /**
     * Register new account using credentials (email,password)
     * Additional profile fields are included to set profile.firstName & profile.lastName fields.
     */
    override fun register(
        email: String,
        password: String,
        name: String,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val namePair = name.splitFullName()
            val profileObject =
                json.encodeToJsonElement(
                    mutableMapOf(
                        "firstName" to namePair.first,
                        "lastName" to namePair.second
                    )
                )
            val authResponse = identityService.register(email, password, profileObject.toString())
            // Check response state for flow success/error/continuation.
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onLogin()
                }

                else -> {
                    onFailedWith(authResponse.toDisplayError())
                }
            }
        }
    }

    /**
     * Login to existing account using credentials (email/password)
     * ViewModel example flow allows account linking interruption handling on login.
     */
    override fun login(
        email: String,
        password: String,
        onLogin: () -> Unit,
        onLoginIdentifierExists: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.login(email, password)
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onLogin()
                }

                AuthState.ERROR -> {
                    onFailedWith(authResponse.toDisplayError())
                }

                AuthState.INTERRUPTED -> {
                    when (authResponse.cdcResponse().errorCode()) {
                        AuthResolvable.ERR_ENTITY_EXIST_CONFLICT -> {
                            onLoginIdentifierExists()
                        }
                    }
                }
            }
        }
    }

    /**
     * Social sign in flow.
     * ViewModel example flow allows both account linking & pending registration interruption handling.
     */
    override fun socialSignInWith(
        hostActivity: ComponentActivity,
        provider: IAuthenticationProvider?,
        onLogin: () -> Unit,
        onPendingRegistration: (IAuthResponse?) -> Unit,
        onLoginIdentifierExists: (IAuthResponse?) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        if (provider == null) {
            onFailedWith(CDCError.providerError())
            return
        }
        viewModelScope.launch {
            val authResponse = identityService.nativeSocialSignIn(
                hostActivity, provider
            )
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onLogin()
                }

                AuthState.ERROR -> {
                    // Error in flow.
                    onFailedWith(authResponse.toDisplayError())
                }

                AuthState.INTERRUPTED -> {
                    // Handle available interruption.
                    when (authResponse.cdcResponse().errorCode()) {
                        AuthResolvable.ERR_ACCOUNT_PENDING_REGISTRATION -> {
                            onPendingRegistration(authResponse)
                        }

                        AuthResolvable.ERR_ENTITY_EXIST_CONFLICT -> {
                            onLoginIdentifierExists(authResponse)
                        }
                    }
                }
            }
        }
    }

    /**
     * Social sign in with web provider flow.
     * ViewModel example for signing in with a provider giving provider name only. This flow will
     * default to the WebAuthenticationProvider class.
     */
    override fun socialWebSignInWith(
        hostActivity: ComponentActivity,
        provider: String,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.webSocialSignIn(
                hostActivity, provider
            )
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onLogin()
                }

                else -> {
                    onFailedWith(authResponse.toDisplayError())
                }
            }
        }
    }

    /**
     * Helper method to fetch a registered authentication provider.
     */
    override fun getAuthenticationProvider(name: String): IAuthenticationProvider? {
        return identityService.getAuthenticationProvider(name)
    }

    /**
     * Request account information.
     */
    override fun getAccountInfo(
        parameters: MutableMap<String, String>?,
        success: () -> Unit,
        onFailed: (CDCError) -> Unit
    ) {
        if (!identityService.validSession()) {
            return
        }
        viewModelScope.launch {
            val authResponse = identityService.getAccountInfo()
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    // Deserialize account data.
                    accountInfo =
                        json.decodeFromString<AccountEntity>(authResponse.asJsonString()!!)
                    success()
                }

                else -> {
                    onFailed(authResponse.toDisplayError()!!)
                }
            }
        }
    }

    /**
     * Update account information with new name.
     * Name parameter will be split to firstName & lastName to update profile fields.
     */
    override fun updateAccountInfoWith(
        name: String,
        success: () -> Unit,
        onFailed: (CDCError) -> Unit
    ) {
        val newName = name.splitFullName()
        val profileObject =
            json.encodeToJsonElement(
                mutableMapOf("firstName" to newName.first, "lastName" to newName.second)
            )
        val parameters = mutableMapOf("profile" to profileObject.toString())
        viewModelScope.launch {
            val setAuthResponse = identityService.setAccountInfo(parameters)
            when (setAuthResponse.state()) {
                AuthState.SUCCESS -> {
                    getAccountInfo(success = success, onFailed = onFailed)
                }

                else -> onFailed(setAuthResponse.toDisplayError()!!)
            }
        }
    }

    /**
     * Log out of current session.
     */
    override fun logOut(success: () -> Unit, onFailed: (CDCError) -> Unit) {
        viewModelScope.launch {
            val authResponse = identityService.logout()
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    success()
                }

                else -> {
                    onFailed(authResponse.toDisplayError()!!)
                }
            }
        }
    }

    /**
     * Resolve link account interruption with credentials input.
     */
    override fun resolveLinkToSiteAccount(
        loginId: String,
        password: String,
        authResolvable: AuthResolvable,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.resolveLinkToSiteAccount(
                loginId = loginId, password = password, authResolvable = authResolvable
            )
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onLogin()
                }

                else -> onFailedWith(authResponse.toDisplayError())
            }
        }
    }

    /**
     * Resolve link account interruption to social account.
     */
    override fun resolveLinkToSocialAccount(
        hostActivity: ComponentActivity,
        provider: String,
        authResolvable: AuthResolvable,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.resolveLinkToSocialAccount(
                hostActivity, identityService.getAuthenticationProvider(provider)!!, authResolvable,
            )
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onLogin()
                }

                else -> {
                    onFailedWith(authResponse.toDisplayError())
                }
            }
        }
    }

    /**
     * Resolve pending registration interruption with provided missing fields for registration.
     */
    override fun resolvePendingRegistrationWithMissingProfileFields(
        map: MutableMap<String, String>,
        regToken: String,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val jsonMap = mutableMapOf<String, JsonPrimitive>()
            map.forEach { (key, value) ->
                // Removing "profile.+" prefix for value... This is dynamic and should not be taken
                // as a best practice form.
                jsonMap[key] = JsonPrimitive(value.substring(0, value.lastIndexOf(".")))
            }
            val authResponse = identityService.resolvePendingRegistrationWithMissingFields(
                "profile", JsonObject(jsonMap).toString(), regToken,
            )
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onLogin()
                }

                else -> {
                    onFailedWith(authResponse.toDisplayError())
                }
            }
        }
    }

    /**
     * Sign in with phone number.
     */
    override fun otpSignIn(
        otpType: OTPType,
        inputField: String,
        success: (AuthResolvable) -> Unit,
        onFailed: (CDCError) -> Unit
    ) {
        viewModelScope.launch {
            val parameters = mutableMapOf<String, String>()
            when (otpType) {
                OTPType.PHONE -> {
                    parameters["phoneNumber"] = inputField
                }

                OTPType.Email -> {
                    parameters["email"] = inputField
                }
            }
            val authResponse = identityService.otpSignIn(parameters)
            when (authResponse.state()) {
                AuthState.INTERRUPTED, AuthState.SUCCESS -> {
                    success(authResponse.resolvable()!!)
                }

                else -> {
                    onFailed(authResponse.toDisplayError()!!)
                }
            }
        }
    }

    /**
     * Resolve phone login. Verify code sent to phone number.
     */
    override fun resolveLoginWithCode(
        code: String,
        resolvable: AuthResolvable,
        onLogin: () -> Unit,
        onPendingRegistration: (IAuthResponse?) -> Unit,
        onFailedWith: (CDCError?) -> Unit
    ) {
        viewModelScope.launch {
            val authResponse = identityService.resolveLoginWithCode(code, resolvable)
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    onLogin()
                }
                AuthState.INTERRUPTED -> {
                    when (authResponse.cdcResponse().errorCode()) {
                        AuthResolvable.ERR_ACCOUNT_PENDING_REGISTRATION -> {
                            onPendingRegistration(authResponse)
                        }
                    }
                }

                else -> {
                    onFailedWith(authResponse.toDisplayError()!!)
                }
            }
        }
    }

    //regin OTP code timer

    override fun startOtpTimer(whenEnded: () -> Unit) {
        startTimer {
            whenEnded()
        }
    }

    private val _timer = MutableStateFlow(0L)
    val timer = _timer.asStateFlow()

    private var timerJob: Job? = null

    private fun startTimer(finished: () -> Unit) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(TimeUnit.SECONDS.toMillis(10))
                _timer.value++
                finished()
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
    }

    private fun stopTimer() {
        _timer.value = 0
        timerJob?.cancel()
    }

    override fun cancelAllTimers() {
        stopTimer()
    }

    //endregion


}


/**
 * Preview mock view model.
 */
class ViewModelAuthenticationPreview : IViewModelAuthentication {
    override fun accountInfo(): AccountEntity = AccountEntity(
        uid = "1234",
        profile = ProfileEntity(firstName = "John", lastName = "Doe", email = "johndoe@gmail.com")
    )
}

