package com.sap.cdc.android.sdk.feature.auth.sequence

import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.CDCResponse
import com.sap.cdc.android.sdk.feature.auth.IAuthResponse
import com.sap.cdc.android.sdk.feature.auth.flow.AccountAuthFlow
import com.sap.cdc.android.sdk.feature.auth.flow.AuthCallbacks
import com.sap.cdc.android.sdk.feature.auth.flow.PasskeysAuthFlow
import com.sap.cdc.android.sdk.feature.auth.flow.ProviderAuthFow
import com.sap.cdc.android.sdk.feature.auth.flow.captcha.AuthCaptcha
import com.sap.cdc.android.sdk.feature.auth.flow.captcha.IAuthCaptcha
import com.sap.cdc.android.sdk.feature.auth.flow.login.AuthLogin
import com.sap.cdc.android.sdk.feature.auth.flow.login.IAuthLogin
import com.sap.cdc.android.sdk.feature.auth.flow.logout.AuthLogoutFlow
import com.sap.cdc.android.sdk.feature.auth.flow.otp.AuthOtp
import com.sap.cdc.android.sdk.feature.auth.flow.otp.IAuthOtp
import com.sap.cdc.android.sdk.feature.auth.flow.register.AuthRegister
import com.sap.cdc.android.sdk.feature.auth.flow.register.IAuthRegister
import com.sap.cdc.android.sdk.feature.auth.session.SessionService
import com.sap.cdc.android.sdk.feature.provider.IAuthenticationProvider
import com.sap.cdc.android.sdk.feature.provider.passkey.IPasskeysAuthenticationProvider
import java.lang.ref.WeakReference

/**
 * Authentication APIs interface.
 */
interface IAuthApis {

    /**
     * Log out of current account interface
     */
    suspend fun logout(authCallbacks: AuthCallbacks.() -> Unit)

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
    override suspend fun logout(authCallbacks: AuthCallbacks.() -> Unit) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        AuthLogoutFlow(coreClient, sessionService).logout(callbacks)
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
