package com.sap.cdc.android.sdk.feature.tfa

import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.TwoFactorContext
import com.sap.cdc.android.sdk.feature.session.SessionService

interface IAuthTFA {

    suspend fun optInForNotifications(
        authCallbacks: AuthCallbacks.() -> Unit = {}
    )

    suspend fun verifyNotification(
        parameters: MutableMap<String, String>,
        finalize: Boolean = false,
        authCallbacks: AuthCallbacks.() -> Unit = {}
    )

    suspend fun getRegisteredEmails(
        twoFactorContext: TwoFactorContext,
        authCallbacks: AuthCallbacks.() -> Unit
    )

    suspend fun sendEmailCode(
        twoFactorContext: TwoFactorContext,
        emailAddress: String,
        language: String?,
        authCallbacks: AuthCallbacks.() -> Unit
    )

    suspend fun registerPhone(
        twoFactorContext: TwoFactorContext,
        phoneNumber: String,
        language: String?,
        method: TFAPhoneMethod? = TFAPhoneMethod.SMS,
        authCallbacks: AuthCallbacks.() -> Unit
    )

    suspend fun registerTOTP(
        twoFactorContext: TwoFactorContext,
        authCallbacks: AuthCallbacks.() -> Unit
    )

    suspend fun getRegisteredPhoneNumbers(
        twoFactorContext: TwoFactorContext,
        authCallbacks: AuthCallbacks.() -> Unit
    )

    suspend fun sendPhoneCode(
        twoFactorContext: TwoFactorContext,
        phoneId: String,
        method: TFAPhoneMethod? = TFAPhoneMethod.SMS,
        language: String? = "en",
        authCallbacks: AuthCallbacks.() -> Unit
    )

    suspend fun verifyEmailCode(
        twoFactorContext: TwoFactorContext,
        code: String,
        rememberDevice: Boolean? = false,
        authCallbacks: AuthCallbacks.() -> Unit
    )

    suspend fun verifyPhoneCode(
        twoFactorContext: TwoFactorContext,
        code: String,
        rememberDevice: Boolean? = false,
        authCallbacks: AuthCallbacks.() -> Unit
    )

    suspend fun verifyTOTPCode(
        twoFactorContext: TwoFactorContext,
        code: String,
        rememberDevice: Boolean? = false,
        authCallbacks: AuthCallbacks.() -> Unit
    )
}

internal class AuthTFA(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthTFA {

    override suspend fun optInForNotifications(
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        val parameters = mutableMapOf(
            "provider" to TFAProvider.PUSH.value, "mode" to "register"
        )
        AuthTFAFlow(coreClient, sessionService)
            .optInForPushNotifications(parameters, callbacks)
    }

    override suspend fun verifyNotification(
        parameters: MutableMap<String, String>,
        finalize: Boolean,
        authCallbacks: AuthCallbacks.() -> Unit,
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        AuthTFAFlow(coreClient, sessionService)
            .verifyPushNotification(parameters, finalize, callbacks)
    }


    override suspend fun getRegisteredEmails(
        twoFactorContext: TwoFactorContext,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        val parameters = mutableMapOf(
            "provider" to TFAProvider.EMAIL.value, "mode" to "verify"
        )
        AuthTFAFlow(coreClient, sessionService)
            .getRegisteredEmails(parameters, twoFactorContext, callbacks)
    }

    override suspend fun sendEmailCode(
        twoFactorContext: TwoFactorContext,
        emailAddress: String,
        language: String?,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        val parameters = mutableMapOf(
            "emailID" to emailAddress,
            "lang" to (language ?: "en")
        )
        AuthTFAFlow(coreClient, sessionService)
            .sendCodeToEmailAddress(parameters, twoFactorContext, callbacks)
    }

    override suspend fun registerPhone(
        twoFactorContext: TwoFactorContext,
        phoneNumber: String,
        language: String?,
        method: TFAPhoneMethod?,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        val parameters = mutableMapOf(
            "provider" to TFAProvider.PHONE.value,
            "lang" to (language ?: "en"),
            "mode" to "register",
            "method" to (method?.value ?: TFAPhoneMethod.SMS.value),
            "phoneID" to phoneNumber
        )
        AuthTFAFlow(coreClient, sessionService)
            .registerPhone(parameters, twoFactorContext, callbacks)
    }

    override suspend fun registerTOTP(
        twoFactorContext: TwoFactorContext,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        val parameters = mutableMapOf(
            "provider" to TFAProvider.TOTP.value,
            "mode" to "register"
        )
        AuthTFAFlow(coreClient, sessionService)
            .registerTimeBasedOneTimePassword(parameters, twoFactorContext, callbacks)
    }

    override suspend fun getRegisteredPhoneNumbers(
        twoFactorContext: TwoFactorContext,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        val parameters = mutableMapOf(
            "provider" to TFAProvider.PHONE.value, "mode" to "verify"
        )
        AuthTFAFlow(coreClient, sessionService)
            .getRegisteredPhoneNumbers(parameters, twoFactorContext, callbacks)

    }

    override suspend fun sendPhoneCode(
        twoFactorContext: TwoFactorContext,
        phoneId: String,
        method: TFAPhoneMethod?,
        language: String?,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        val parameters = mutableMapOf(
            "phoneID" to phoneId,
            "method" to (method?.value ?: TFAPhoneMethod.SMS.value),
            "lang" to (language ?: "en")
        )
        AuthTFAFlow(coreClient, sessionService)
            .sendCodeToPhoneNumber(parameters, twoFactorContext, callbacks)
    }

    override suspend fun verifyEmailCode(
        twoFactorContext: TwoFactorContext,
        code: String,
        rememberDevice: Boolean?,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        val parameters = mutableMapOf(
            "code" to code
        )
        AuthTFAFlow(coreClient, sessionService)
            .verifyCode(
                parameters, twoFactorContext, TFAProvider.EMAIL,
                rememberDevice ?: false, callbacks
            )
    }

    override suspend fun verifyPhoneCode(
        twoFactorContext: TwoFactorContext,
        code: String,
        rememberDevice: Boolean?,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        val parameters = mutableMapOf(
            "code" to code
        )
        AuthTFAFlow(coreClient, sessionService)
            .verifyCode(
                parameters, twoFactorContext, TFAProvider.PHONE,
                rememberDevice ?: false, callbacks
            )
    }

    override suspend fun verifyTOTPCode(
        twoFactorContext: TwoFactorContext,
        code: String,
        rememberDevice: Boolean?,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        val callbacks = AuthCallbacks().apply(authCallbacks)
        val parameters = mutableMapOf(
            "code" to code
        )
        AuthTFAFlow(coreClient, sessionService)
            .verifyCode(
                parameters, twoFactorContext, TFAProvider.TOTP,
                rememberDevice ?: false, callbacks
            )
    }
}