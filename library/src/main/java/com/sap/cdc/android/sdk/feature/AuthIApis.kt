package com.sap.cdc.android.sdk.feature

import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.captcha.AuthCaptcha
import com.sap.cdc.android.sdk.feature.captcha.IAuthCaptcha
import com.sap.cdc.android.sdk.feature.login.AuthLogin
import com.sap.cdc.android.sdk.feature.login.IAuthLogin
import com.sap.cdc.android.sdk.feature.logout.AuthLogoutFlow
import com.sap.cdc.android.sdk.feature.notifications.AuthPush
import com.sap.cdc.android.sdk.feature.notifications.IAuthPush
import com.sap.cdc.android.sdk.feature.otp.AuthOtp
import com.sap.cdc.android.sdk.feature.otp.IAuthOtp
import com.sap.cdc.android.sdk.feature.provider.AuthProvider
import com.sap.cdc.android.sdk.feature.provider.IAuthProvider
import com.sap.cdc.android.sdk.feature.provider.passkey.AuthPasskeys
import com.sap.cdc.android.sdk.feature.provider.passkey.IAuthPasskeys
import com.sap.cdc.android.sdk.feature.register.AuthRegister
import com.sap.cdc.android.sdk.feature.register.IAuthRegister
import com.sap.cdc.android.sdk.feature.session.SessionService
import com.sap.cdc.android.sdk.feature.tfa.AuthTFA
import com.sap.cdc.android.sdk.feature.tfa.IAuthTFA

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
