package com.sap.cdc.android.sdk.auth

import androidx.activity.ComponentActivity
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

}

// region IAuthRegister

interface IAuthRegister {

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

    suspend fun sendCode(parameters: MutableMap<String, String>): IAuthResponse

}

internal class AuthOtp(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthOtp {

    /**
     * Initiate phone number sign in flow.
     */
    override suspend fun sendCode(parameters: MutableMap<String, String>): IAuthResponse {
        val flow = LoginAuthFlow(coreClient, sessionService)
        return flow.otpSendCode(parameters)
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
