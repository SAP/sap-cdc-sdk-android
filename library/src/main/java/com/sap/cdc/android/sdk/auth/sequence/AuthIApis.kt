package com.sap.cdc.android.sdk.auth.sequence

import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.auth.AuthTFA
import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.IAuthTFA
import com.sap.cdc.android.sdk.auth.ResolvableContext
import com.sap.cdc.android.sdk.auth.flow.AccountAuthFlow
import com.sap.cdc.android.sdk.auth.flow.CaptchaAuthFlow
import com.sap.cdc.android.sdk.auth.flow.LoginAuthFlow
import com.sap.cdc.android.sdk.auth.flow.LogoutAuthFlow
import com.sap.cdc.android.sdk.auth.flow.PasskeysAuthFlow
import com.sap.cdc.android.sdk.auth.flow.ProviderAuthFow
import com.sap.cdc.android.sdk.auth.flow.RegistrationAuthFlow
import com.sap.cdc.android.sdk.auth.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.auth.provider.IPasskeysAuthenticationProvider
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.CDCResponse
import java.lang.ref.WeakReference

/**
 * Authentication APIs interface.
 */
interface IAuthApis {

    /**
     * Log out of current account interface
     */
    suspend fun logout(): IAuthResponse

    fun register(): IAuthRegister

    fun login(): IAuthLogin

    fun passkeys(): IAuthPasskeys

    fun otp(): IAuthOtp

    fun captcha(): IAuthCaptcha

    fun push(): IAuthPush

    fun provider(): IAuthProvider

    fun tfa(): IAuthTFA

}

/**
 * Authentication APIs initiators/implementors.
 */
internal class AuthApis(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthApis {

    /**
     * Log out of current account implementation.
     * Logging out will remove all session data.
     */
    override suspend fun logout(): IAuthResponse {
        val flow = LogoutAuthFlow(coreClient, sessionService)
        return flow.logout()
    }

    override fun register(): IAuthRegister = AuthRegister(coreClient, sessionService)

    override fun login(): IAuthLogin = AuthLogin(coreClient, sessionService)

    override fun passkeys(): IAuthPasskeys = AuthPasskeys(coreClient, sessionService)

    override fun otp(): IAuthOtp = AuthOtp(coreClient, sessionService)

    override fun captcha(): IAuthCaptcha = AuthCaptcha(coreClient, sessionService)

    override fun push(): IAuthPush = AuthPush(coreClient, sessionService)

    override fun provider(): IAuthProvider = AuthProvider(coreClient, sessionService)

    override fun tfa(): IAuthTFA = AuthTFA(coreClient, sessionService)

}

// region IAuthRegister

interface IAuthRegister {

    fun resolve(): IAuthRegisterResolvers

    suspend fun withParameters(
        parameters: MutableMap<String, String>
    ): IAuthResponse

    suspend fun withLoginIDCredentials(
        loginId: String,
        password: String
    ): IAuthResponse

    suspend fun withEmailCredentials(
        email: String,
        password: String
    ): IAuthResponse
}


internal class AuthRegister(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthRegister {

    override fun resolve(): IAuthRegisterResolvers =
        AuthRegisterResolvers(coreClient, sessionService)

    /**
     * initiate credentials registration flow interface.
     */
    override suspend fun withParameters(
        parameters: MutableMap<String, String>
    ): IAuthResponse {
        val flow = RegistrationAuthFlow(coreClient, sessionService)
        return flow.register(parameters)
    }

    override suspend fun withLoginIDCredentials(
        loginId: String,
        password: String
    ): IAuthResponse {
        val parameters = mutableMapOf<String, String>()
        parameters["loginID"] = loginId
        parameters["password"] = password
        return withParameters(parameters)
    }

    override suspend fun withEmailCredentials(
        email: String,
        password: String
    ): IAuthResponse {
        val parameters = mutableMapOf<String, String>()
        parameters["email"] = email
        parameters["password"] = password
        return withParameters(parameters)
    }
}

interface IAuthRegisterResolvers {

    /**
     * Finalize registration process interface.
     */
    suspend fun finalizeRegistration(
        parameters: MutableMap<String, String>
    ): IAuthResponse

    /**
     * Resolve "Account Pending Registration" interruption error.
     * 1. Flow will initiate a "setAccount" flow to update account information.
     * 2. Flow will attempt to finalize the registration to complete registration process.
     */
    suspend fun pendingRegistrationWith(
        regToken: String,
        missingFields: MutableMap<String, String>
    ): IAuthResponse
}

internal class AuthRegisterResolvers(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthRegisterResolvers {

    /**
     * Finalize registration process implementation.
     */
    override suspend fun finalizeRegistration(
        parameters: MutableMap<String, String>
    ): IAuthResponse {
        val resolver = RegistrationAuthFlow(coreClient, sessionService)
        return resolver.finalize(parameters)
    }

    override suspend fun pendingRegistrationWith(
        regToken: String,
        missingFields: MutableMap<String, String>
    ): IAuthResponse {
        val setAccountResolver = AccountAuthFlow(coreClient, sessionService)
        missingFields["regToken"] = regToken
        val setAccountAuthResponse = setAccountResolver.setAccountInfo(missingFields)
        when (setAccountAuthResponse.state()) {
            AuthState.SUCCESS -> {
                // Error in flow.
                val finalizeRegistrationResolver = RegistrationAuthFlow(coreClient, sessionService)
                return finalizeRegistrationResolver.finalize(mutableMapOf("regToken" to regToken))
            }

            else -> {
                return setAccountAuthResponse
            }
        }
    }
}

// endregion

// region IAuthLogin

interface IAuthLogin {

    suspend fun withParameters(
        parameters: MutableMap<String, String>
    ): IAuthResponse

    suspend fun withLoginIDCredentials(
        loginId: String,
        password: String
    ): IAuthResponse

    suspend fun withEmailCredentials(
        email: String,
        password: String
    ): IAuthResponse
}

internal class AuthLogin(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthLogin {

    /**
     * initiate credentials login flow interface.
     */
    override suspend fun withParameters(
        parameters: MutableMap<String, String>
    ): IAuthResponse {
        val flow = LoginAuthFlow(coreClient, sessionService)
        return flow.login(parameters)
    }

    override suspend fun withLoginIDCredentials(loginId: String, password: String): IAuthResponse {
        val parameters = mutableMapOf<String, String>()
        parameters["loginID"] = loginId
        parameters["password"] = password
        return withParameters(parameters)
    }

    override suspend fun withEmailCredentials(email: String, password: String): IAuthResponse {
        val parameters = mutableMapOf<String, String>()
        parameters["email"] = email
        parameters["password"] = password
        return withParameters(parameters)
    }

}

// endregion

//region IAuthPasskeys

interface IAuthPasskeys {

    suspend fun create(
        authenticationProvider: IPasskeysAuthenticationProvider,
    ): IAuthResponse

    suspend fun signIn(
        authenticationProvider: IPasskeysAuthenticationProvider,
    ): IAuthResponse

    suspend fun clear(
        authenticationProvider: IPasskeysAuthenticationProvider,
    ): IAuthResponse
}

internal class AuthPasskeys(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthPasskeys {

    override suspend fun create(
        authenticationProvider: IPasskeysAuthenticationProvider,
    ): IAuthResponse {
        val flow =
            PasskeysAuthFlow(
                coreClient, sessionService,
                authenticationProvider = authenticationProvider
            )
        return flow.createPasskey()
    }

    override suspend fun signIn(
        authenticationProvider: IPasskeysAuthenticationProvider,
    ): IAuthResponse {
        val flow =
            PasskeysAuthFlow(
                coreClient, sessionService,
                authenticationProvider = authenticationProvider
            )
        return flow.authenticateWithPasskey()
    }

    override suspend fun clear(
        authenticationProvider: IPasskeysAuthenticationProvider,
    ): IAuthResponse {
        val flow =
            PasskeysAuthFlow(
                coreClient, sessionService,
                authenticationProvider = authenticationProvider
            )
        return flow.clearPasskeyCredential()
    }
}

//endregion

//region IAuthOtp

interface IAuthOtp {

    fun resolve(): IAuthOtpResolvers

    suspend fun sendCode(parameters: MutableMap<String, String>): IAuthResponse

}

internal class AuthOtp(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthOtp {

    override fun resolve(): IAuthOtpResolvers = AuthOtpResolvers(coreClient, sessionService)

    /**
     * Initiate phone number sign in flow.
     */
    override suspend fun sendCode(parameters: MutableMap<String, String>): IAuthResponse {
        val flow = LoginAuthFlow(coreClient, sessionService)
        return flow.otpSendCode(parameters)
    }
}

interface IAuthOtpResolvers {

    suspend fun login(
        code: String,
        resolvableContext: ResolvableContext
    ): IAuthResponse

    suspend fun otpUpdate(
        code: String,
        resolvableContext: ResolvableContext
    ): IAuthResponse
}

internal class AuthOtpResolvers(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthOtpResolvers {

    /**
     * Resolve phone login flow using provided code/vToken available in the "AuthResolvable" entity.
     */
    override suspend fun login(
        code: String,
        resolvableContext: ResolvableContext
    ): IAuthResponse {
        val codeVerify = LoginAuthFlow(coreClient, sessionService)
        return codeVerify.otpLogin(
            mutableMapOf(
                "vToken" to resolvableContext.otp?.vToken!!,
                "code" to code
            )
        )
    }

    /**
     * Resolve phone update flow provided code/vToken available in the "AuthResolvable" entity.
     */
    override suspend fun otpUpdate(
        code: String,
        resolvableContext: ResolvableContext
    ): IAuthResponse {
        val codeVerify = LoginAuthFlow(coreClient, sessionService)
        return codeVerify.otpUpdate(
            mutableMapOf(
                "vToken" to resolvableContext.otp?.vToken!!,
                "code" to code
            )
        )
    }
}

//endregion

//region IAuthSaptcha

interface IAuthCaptcha {

    suspend fun getSaptchaToken(): IAuthResponse

}

internal class AuthCaptcha(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthCaptcha {

    override suspend fun getSaptchaToken(): IAuthResponse {
        val flow = CaptchaAuthFlow(coreClient, sessionService)
        return flow.getSaptchaToken()
    }
}

//endregion

//region IAuthPush

interface IAuthPush {

    suspend fun registerForAuthNotifications(): IAuthResponse

    suspend fun verifyAuthNotification(vToken: String): IAuthResponse
}

internal class AuthPush(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthPush {

    override suspend fun registerForAuthNotifications(): IAuthResponse {
        val flow = AccountAuthFlow(coreClient, sessionService)
        return flow.registerAuthDevice()
    }

    override suspend fun verifyAuthNotification(vToken: String): IAuthResponse {
        val flow = AccountAuthFlow(coreClient, sessionService)
        return flow.verifyAuthPush(vToken)
    }

}

//endregion

//region IAuthProvider

interface IAuthProvider {

    /**
     * initiate provider authentication flow interface.
     */
    suspend fun signIn(
        hostActivity: ComponentActivity,
        authenticationProvider: IAuthenticationProvider,
        parameters: MutableMap<String, String>? = null
    ): IAuthResponse

    /**
     * Remove social connection from current account interface.
     */
    suspend fun removeConnection(
        provider: String
    ): CDCResponse
}

internal class AuthProvider(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthProvider {

    /**
     * initiate provider authentication flow implementation.
     */
    override suspend fun signIn(
        hostActivity: ComponentActivity,
        authenticationProvider: IAuthenticationProvider,
        parameters: MutableMap<String, String>?
    ): IAuthResponse {
        val flow = ProviderAuthFow(
            coreClient, sessionService, authenticationProvider, WeakReference(hostActivity)
        )
        return flow.signIn(parameters ?: mutableMapOf())
    }

    /**
     * Remove social connection from current account implementation.
     */
    override suspend fun removeConnection(provider: String): CDCResponse = ProviderAuthFow(
        coreClient, sessionService
    ).removeConnection(provider)

}

//endregion
