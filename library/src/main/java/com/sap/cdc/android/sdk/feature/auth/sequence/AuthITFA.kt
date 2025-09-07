package com.sap.cdc.android.sdk.feature.auth.sequence

import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.feature.auth.IAuthResponse
import com.sap.cdc.android.sdk.feature.auth.ResolvableContext
import com.sap.cdc.android.sdk.feature.auth.flow.TFAAuthFlow
import com.sap.cdc.android.sdk.feature.auth.model.TFAPhoneMethod
import com.sap.cdc.android.sdk.feature.auth.model.TFAProvider
import com.sap.cdc.android.sdk.feature.auth.model.TFAProvidersEntity
import com.sap.cdc.android.sdk.feature.session.SessionService

interface IAuthTFA {

    suspend fun getProviders(regToken: String): IAuthResponse

    fun parseTFAProviders(authResponse: IAuthResponse): TFAProvidersEntity

    suspend fun optInForPushAuthentication(): IAuthResponse

    suspend fun finalizeOtpInForPushAuthentication(parameters: MutableMap<String, String>): IAuthResponse

    suspend fun verifyPushTFA(parameters: MutableMap<String, String>): IAuthResponse

    suspend fun getRegisteredEmails(
        resolvableContext: ResolvableContext,
    ): IAuthResponse

    suspend fun sendEmailCode(
        resolvableContext: ResolvableContext,
        emailAddress: String,
        language: String?
    ): IAuthResponse

    suspend fun registerPhone(
        phoneNumber: String,
        resolvableContext: ResolvableContext,
        language: String?,
        method: TFAPhoneMethod? = TFAPhoneMethod.SMS
    ): IAuthResponse

    suspend fun registerTOTP(
        resolvableContext: ResolvableContext,
    ): IAuthResponse

    suspend fun getRegisteredPhoneNumbers(resolvableContext: ResolvableContext): IAuthResponse

    suspend fun sendPhoneCode(
        resolvableContext: ResolvableContext,
        phoneId: String,
        method: TFAPhoneMethod? = TFAPhoneMethod.SMS,
        language: String? = "en"
    ): IAuthResponse

    suspend fun verifyEmailCode(
        resolvableContext: ResolvableContext,
        code: String,
        rememberDevice: Boolean? = false,
    ): IAuthResponse

    suspend fun verifyPhoneCode(
        resolvableContext: ResolvableContext,
        code: String,
        rememberDevice: Boolean? = false
    ): IAuthResponse

    suspend fun verifyTOTPCode(
        resolvableContext: ResolvableContext,
        code: String,
        rememberDevice: Boolean? = false
    ): IAuthResponse
}

internal class AuthTFA(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthTFA {

    override suspend fun getProviders(regToken: String): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.getTFAProviders(mutableMapOf("regToken" to regToken))
    }

    override fun parseTFAProviders(authResponse: IAuthResponse): TFAProvidersEntity {
        val parsedEntity = authResponse.cdcResponse().json.decodeFromString<TFAProvidersEntity>(
            authResponse.asJsonString()!!
        )
        return parsedEntity
    }

    override suspend fun optInForPushAuthentication(): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        val parameters = mutableMapOf(
            "provider" to TFAProvider.PUSH.value, "mode" to "register"
        )
        return tfaFlow.optInForPushTFA(parameters = parameters)
    }

    override suspend fun finalizeOtpInForPushAuthentication(
        parameters: MutableMap<String, String>
    ): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.finalizeOptInForPushTFA(parameters)
    }

    override suspend fun verifyPushTFA(parameters: MutableMap<String, String>): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.verifyPushTFA(parameters)
    }

    override suspend fun getRegisteredEmails(
        resolvableContext: ResolvableContext
    ): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.getRegisteredEmails(
            resolvableContext,
            mutableMapOf("provider" to TFAProvider.EMAIL.value, "mode" to "verify")
        )
    }

    override suspend fun sendEmailCode(
        resolvableContext: ResolvableContext,
        emailAddress: String,
        language: String?
    ): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.sendEmailCode(
            resolvableContext,
            mutableMapOf("emailID" to emailAddress, "lang" to (language ?: "en"))
        )
    }

    override suspend fun registerPhone(
        phoneNumber: String,
        resolvableContext: ResolvableContext,
        language: String?,
        method: TFAPhoneMethod?
    ): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.registerPhone(
            resolvableContext, phoneNumber,
            mutableMapOf(
                "provider" to TFAProvider.PHONE.value,
                "lang" to (language ?: "en"),
                "mode" to "register",
                "method" to (method?.value ?: TFAPhoneMethod.SMS.value)
            )
        )
    }

    override suspend fun registerTOTP(resolvableContext: ResolvableContext): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.registerTOTP(
            resolvableContext, mutableMapOf(
                "provider" to TFAProvider.TOTP.value,
                "mode" to "register"
            )
        )
    }

    override suspend fun getRegisteredPhoneNumbers(resolvableContext: ResolvableContext): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.getRegisteredPhoneNumbers(
            resolvableContext, mutableMapOf(
                "provider" to TFAProvider.PHONE.value,
                "mode" to "verify"
            )
        )
    }

    override suspend fun sendPhoneCode(
        resolvableContext: ResolvableContext,
        phoneId: String,
        method: TFAPhoneMethod?,
        language: String?
    ): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.sendPhoneCode(
            resolvableContext,
            mutableMapOf(
                "lang" to (language ?: "en"),
                "phoneID" to phoneId,
                "method" to (method?.value ?: TFAPhoneMethod.SMS.value)
            )
        )
    }

    override suspend fun verifyEmailCode(
        resolvableContext: ResolvableContext,
        code: String,
        rememberDevice: Boolean?
    ): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.verifyCode(
            resolvableContext,
            mutableMapOf("code" to code),
            TFAProvider.EMAIL,
            rememberDevice = rememberDevice!!
        )
    }

    override suspend fun verifyPhoneCode(
        resolvableContext: ResolvableContext,
        code: String,
        rememberDevice: Boolean?
    ): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.verifyCode(
            resolvableContext,
            mutableMapOf("code" to code),
            TFAProvider.PHONE,
            rememberDevice = rememberDevice!!
        )
    }

    override suspend fun verifyTOTPCode(
        resolvableContext: ResolvableContext,
        code: String,
        rememberDevice: Boolean?
    ): IAuthResponse {
        val tfaFlow = TFAAuthFlow(coreClient, sessionService)
        return tfaFlow.verifyCode(
            resolvableContext,
            mutableMapOf("code" to code),
            TFAProvider.TOTP,
            rememberDevice = rememberDevice!!
        )
    }
}
