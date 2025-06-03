package com.sap.cdc.android.sdk.auth

import com.sap.cdc.android.sdk.auth.flow.TFAAuthFlow
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.auth.model.TFAPhoneMethod
import com.sap.cdc.android.sdk.auth.model.TFAProvider
import com.sap.cdc.android.sdk.auth.model.TFAProvidersEntity
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.CDCResponse
import com.sap.cdc.android.sdk.core.api.model.CDCError
import kotlinx.serialization.json.JsonObject

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */


/**
 * Authentication result state enum.
 * ERROR - Indicates an unresolvable error in the the API response.
 * SUCCESS - API success or end of flow.
 * INTERRUPTED - Indicates an resolvable error occurred in the the API response. Flow can continue according to the error.
 */
enum class AuthState {
    ERROR, SUCCESS, INTERRUPTED
}

/**
 * Authentication response main class interface.
 */
interface IAuthResponse {

    fun cdcResponse(): CDCResponse
    fun asJsonString(): String?
    fun asJsonObject(): JsonObject?
    fun toDisplayError(): CDCError?
    fun state(): AuthState
    fun resolvable(): ResolvableContext?
}

/**
 * Authentication flow main response class.
 */
class AuthResponse(private val cdcResponse: CDCResponse) : IAuthResponse {

    var resolvableContext: ResolvableContext? = null

    override fun cdcResponse(): CDCResponse = cdcResponse

    override fun asJsonString(): String? = this.cdcResponse.asJson()

    override fun asJsonObject(): JsonObject? = this.cdcResponse.jsonObject

    internal fun isError(): Boolean = cdcResponse.isError()

    override fun toDisplayError(): CDCError = this.cdcResponse.toCDCError()

    fun isResolvable(): Boolean =
        ResolvableContext.resolvables.containsKey(cdcResponse.errorCode()) || cdcResponse.containsKey(
            "vToken"
        )

    /**
     * Defines flow state.
     * Success - marks the end of the flow.
     * Error - indicates an unresolvable error in the the API response.
     * Interrupted - indicates a continuation of the flow is available providing additional data/steps/
     */
    override fun state(): AuthState {
        if (isResolvable()) {
            return AuthState.INTERRUPTED
        }
        if (isError()) {
            return AuthState.ERROR
        }
        return AuthState.SUCCESS
    }

    /**
     * Get reference to resolvable data entity.
     * This data is required to complete interrupted flows.
     */
    override fun resolvable(): ResolvableContext? = resolvableContext
}

//region IAuthTFA

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

//endregion
