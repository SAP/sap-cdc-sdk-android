package com.sap.cdc.android.sdk.feature.auth.flow

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_ACCOUNTS_FINALIZE_REGISTRATION
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_TFA_EMAILS_COMPLETE_VERIFICATION
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_TFA_EMAILS_SEND_CODE
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_TFA_EMAIL_GET
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_TFA_FINALIZE
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_TFA_GET_PROVIDERS
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_TFA_INIT
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_TFA_PHONE_COMPLETE_VERIFICATION
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_TFA_PHONE_GET
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_TFA_PHONE_SEND_CODE
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_TFA_PUSH_OPT_IN
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_TFA_PUSH_VERIFY
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_TFA_TOTP_REGISTER
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_TFA_TOTP_VERIFY
import com.sap.cdc.android.sdk.feature.auth.AuthResponse
import com.sap.cdc.android.sdk.feature.auth.AuthenticationApi
import com.sap.cdc.android.sdk.feature.auth.AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
import com.sap.cdc.android.sdk.feature.auth.AuthenticationService.Companion.CDC_DEVICE_INFO
import com.sap.cdc.android.sdk.feature.auth.IAuthResponse
import com.sap.cdc.android.sdk.feature.auth.ResolvableContext
import com.sap.cdc.android.sdk.feature.auth.model.TFAEmailEntity
import com.sap.cdc.android.sdk.feature.auth.model.TFAProvider
import com.sap.cdc.android.sdk.feature.auth.model.TFARegisteredPhoneEntities
import com.sap.cdc.android.sdk.feature.auth.session.SessionService
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import kotlinx.serialization.json.Json

/**
 * Created by Tal Mirmelshtein on 12/08/2024
 * Copyright: SAP LTD.
 */

class TFAAuthFlow(coreClient: CoreClient, sessionService: SessionService) :
    AuthFlow(coreClient, sessionService) {

    companion object {
        const val LOG_TAG = "TFAAuthFlow"
    }

    /**
     * Request account two factor authentication providers:
     * Active - Providers that are currently active and the user can use to authenticate.
     * Inactive - Providers that are currently inactive and the user can activate to use for authentication.
     */
    suspend fun getTFAProviders(parameters: MutableMap<String, String>? = mutableMapOf()): IAuthResponse {
        CDCDebuggable.log(LOG_TAG, "getTFAProviders: with parameters:$parameters")
        val tfaProvidersResponse = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_GET_PROVIDERS,
            parameters!!
        )
        return AuthResponse(tfaProvidersResponse)
    }

    /**
     * Initiate push TFA registration.
     * NOTE: Requires deviceInfo to be sent.
     */
    suspend fun optInForPushTFA(parameters: MutableMap<String, String>): IAuthResponse {
        CDCDebuggable.log(LOG_TAG, "optInForPushTFA: with parameters:$parameters")
        val initTFAResponse = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_INIT,
            parameters
        )
        if (initTFAResponse.isError()) return AuthResponse(initTFAResponse)

        val assertion = initTFAResponse.stringField("gigyaAssertion") ?: ""

        // Obtain device info from secure storage.
        val esp = coreClient.siteConfig.applicationContext.getEncryptedPreferences(
            CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
        )
        val deviceInfo = esp.getString(CDC_DEVICE_INFO, "") ?: ""

        val pushOptInResponse = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_PUSH_OPT_IN,
            mutableMapOf("gigyaAssertion" to assertion, "deviceInfo" to deviceInfo)
        )
        return AuthResponse(pushOptInResponse)
    }

    suspend fun finalizeOptInForPushTFA(parameters: MutableMap<String, String>): IAuthResponse {
        CDCDebuggable.log(LOG_TAG, "finalizeOptInForPushTFA: with parameters:$parameters")
        val verifyPushResponse = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_PUSH_VERIFY,
            parameters
        )
        if (verifyPushResponse.isError()) return AuthResponse(verifyPushResponse)

        val providerAssertion = verifyPushResponse.stringField("providerAssertion") ?: ""

        val finalizePushResponse = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_FINALIZE,
            mutableMapOf("providerAssertion" to providerAssertion)
        )
        return AuthResponse(finalizePushResponse)
    }

    suspend fun verifyPushTFA(parameters: MutableMap<String, String>): IAuthResponse {
        CDCDebuggable.log(LOG_TAG, "verifyPushTFA: with parameters:$parameters")
        val verifyPushResponse = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_PUSH_VERIFY,
            parameters
        )
        return AuthResponse(verifyPushResponse)
    }

    suspend fun getRegisteredEmails(
        resolvableContext: ResolvableContext,
        parameters: MutableMap<String, String>
    ): IAuthResponse {
        CDCDebuggable.log(LOG_TAG, "getRegisteredEmails: with parameters:$parameters")
        parameters["regToken"] = resolvableContext.regToken!!
        val initTFAResponse = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_INIT,
            parameters
        )
        if (initTFAResponse.isError()) return AuthResponse(initTFAResponse)

        val assertion = initTFAResponse.stringField("gigyaAssertion") ?: ""

        val getEmailsResponse = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_EMAIL_GET,
            mutableMapOf("gigyaAssertion" to assertion)
        )
        if (getEmailsResponse.isError()) return AuthResponse(getEmailsResponse)

        val emailsJson = getEmailsResponse.stringField("emails")
        val emails = Json.decodeFromString<List<TFAEmailEntity>>(emailsJson!!)

        val authResponse = AuthResponse(getEmailsResponse)
        resolvableContext.tfa?.assertion = assertion
        resolvableContext.tfa?.emails = emails
        authResponse.resolvableContext = resolvableContext
        return authResponse
    }

    suspend fun sendEmailCode(
        resolvableContext: ResolvableContext,
        parameters: MutableMap<String, String>
    ): IAuthResponse {
        CDCDebuggable.log(LOG_TAG, "sendEmailCode: with parameters:$parameters")
        parameters["gigyaAssertion"] = resolvableContext.tfa?.assertion!!
        val sendCodeResponse = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_EMAILS_SEND_CODE,
            parameters
        )
        if (sendCodeResponse.isError()) return AuthResponse(sendCodeResponse)
        val phvToken = sendCodeResponse.stringField("phvToken") ?: ""
        val authResponse = AuthResponse(sendCodeResponse)
        resolvableContext.tfa?.phvToken = phvToken
        authResponse.resolvableContext = resolvableContext
        return authResponse
    }

    suspend fun registerPhone(
        resolvableContext: ResolvableContext,
        phoneNumber: String,
        parameters: MutableMap<String, String>,
        language: String? = "en"
    ): IAuthResponse {
        CDCDebuggable.log(LOG_TAG, "registerPhone: with parameters:$parameters")
        parameters["regToken"] = resolvableContext.regToken!!
        val initTFAResponse = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_INIT,
            parameters
        )

        val assertion = initTFAResponse.stringField("gigyaAssertion") ?: ""
        resolvableContext.tfa?.assertion = assertion

        if (initTFAResponse.isError()) return AuthResponse(initTFAResponse)
        val sendCodeResponse = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_PHONE_SEND_CODE,
            mutableMapOf(
                "gigyaAssertion" to assertion, "phone" to phoneNumber,
                "method" to "sms",
                "lang" to language!!
            )
        )
        val phvToken = sendCodeResponse.stringField("phvToken") ?: ""
        val authResponse = AuthResponse(sendCodeResponse)
        resolvableContext.tfa?.phvToken = phvToken
        authResponse.resolvableContext = resolvableContext
        return authResponse
    }

    suspend fun getRegisteredPhoneNumbers(
        resolvableContext: ResolvableContext,
        parameters: MutableMap<String, String>
    ): IAuthResponse {
        CDCDebuggable.log(LOG_TAG, "getRegisteredPhoneNumbers: with parameters:$parameters")
        parameters["regToken"] = resolvableContext.regToken!!
        val initTFAResponse = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_INIT,
            parameters
        )
        if (initTFAResponse.isError()) return AuthResponse(initTFAResponse)

        val assertion = initTFAResponse.stringField("gigyaAssertion") ?: ""

        parameters["gigyaAssertion"] = assertion
        val getPhoneNumbersResult = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_PHONE_GET,
            mutableMapOf("gigyaAssertion" to assertion)
        )
        if (getPhoneNumbersResult.isError()) return AuthResponse(getPhoneNumbersResult)

        val phones =
            json.decodeFromString<TFARegisteredPhoneEntities>(getPhoneNumbersResult.asJson()!!)

        val authResponse = AuthResponse(getPhoneNumbersResult)
        resolvableContext.tfa?.assertion = assertion
        resolvableContext.tfa?.phones = phones.phones
        authResponse.resolvableContext = resolvableContext
        return authResponse
    }

    suspend fun sendPhoneCode(
        resolvableContext: ResolvableContext,
        parameters: MutableMap<String, String>
    ): IAuthResponse {
        parameters["gigyaAssertion"] = resolvableContext.tfa?.assertion!!
        CDCDebuggable.log(LOG_TAG, "sendPhoneCode: with parameters:$parameters")
        val sendCodeResponse = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_PHONE_SEND_CODE,
            parameters
        )
        if (sendCodeResponse.isError()) return AuthResponse(sendCodeResponse)
        val phvToken = sendCodeResponse.stringField("phvToken") ?: ""
        val authResponse = AuthResponse(sendCodeResponse)
        resolvableContext.tfa?.phvToken = phvToken
        authResponse.resolvableContext = resolvableContext
        return authResponse
    }

    suspend fun verifyCode(
        resolvableContext: ResolvableContext,
        parameters: MutableMap<String, String>,
        provider: TFAProvider,
        rememberDevice: Boolean
    ): IAuthResponse {
        CDCDebuggable.log(LOG_TAG, "verifyCode: with parameters:$parameters")
        var assertion: String? = null
        if (resolvableContext.tfa?.assertion == null) {
            // Need to re-initiate TFA flow.
            val initTFAResponse = AuthenticationApi(coreClient, sessionService).send(
                EP_TFA_INIT,
                mutableMapOf(
                    "regToken" to resolvableContext.regToken!!,
                    "provider" to provider.value,
                    "mode" to "verify"
                )
            )
            if (initTFAResponse.isError()) return AuthResponse(initTFAResponse)
            assertion = initTFAResponse.stringField("gigyaAssertion") ?: ""
        } else {
            assertion = resolvableContext.tfa?.assertion!!
        }
        parameters["gigyaAssertion"] = assertion

        if (resolvableContext.tfa?.phvToken != null) {
            parameters["phvToken"] = resolvableContext.tfa?.phvToken!!
        }
        if (resolvableContext.tfa?.sctToken != null) {
            parameters["sctToken"] = resolvableContext.tfa?.sctToken!!
        }
        val verifyCodeResponse = AuthenticationApi(coreClient, sessionService).send(
            when (provider) {
                TFAProvider.EMAIL -> EP_TFA_EMAILS_COMPLETE_VERIFICATION
                TFAProvider.PHONE -> EP_TFA_PHONE_COMPLETE_VERIFICATION
                TFAProvider.TOTP -> EP_TFA_TOTP_VERIFY
                else -> "" // Redundant.
            },
            parameters
        )
        if (verifyCodeResponse.isError()) return AuthResponse(verifyCodeResponse)

        val providerAssertion = verifyCodeResponse.stringField("providerAssertion") ?: ""

        val finalizeTFAResult = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_FINALIZE,
            mutableMapOf(
                "regToken" to resolvableContext.regToken!!,
                "gigyaAssertion" to assertion,
                "providerAssertion" to providerAssertion,
                "tempDevice" to (!rememberDevice).toString()
            )
        )

        val finalizeRegistrationResult = AuthenticationApi(coreClient, sessionService).send(
            EP_ACCOUNTS_FINALIZE_REGISTRATION,
            mutableMapOf(
                "regToken" to resolvableContext.regToken!!,
                "includeUserInfo" to "true"
            )
        )

        if (finalizeRegistrationResult.isError()) return AuthResponse(finalizeRegistrationResult)
        secureNewSession(finalizeRegistrationResult)

        return AuthResponse(finalizeRegistrationResult)
    }

    suspend fun registerTOTP(
        resolvableContext: ResolvableContext,
        parameters: MutableMap<String, String>
    ): IAuthResponse {
        CDCDebuggable.log(LOG_TAG, "registerTOTP: with parameters:$parameters")
        parameters["regToken"] = resolvableContext.regToken!!
        val initTFAResponse = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_INIT,
            parameters
        )
        if (initTFAResponse.isError()) return AuthResponse(initTFAResponse)

        val assertion = initTFAResponse.stringField("gigyaAssertion") ?: ""

        parameters["gigyaAssertion"] = assertion
        val registerTOTPResponse = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_TOTP_REGISTER,
            mutableMapOf("gigyaAssertion" to assertion),
        )

        val qrCode = registerTOTPResponse.stringField("qrCode") ?: ""
        val sctToken = registerTOTPResponse.stringField("sctToken") ?: ""

        resolvableContext.tfa?.assertion = assertion
        resolvableContext.tfa?.qrCode = qrCode
        resolvableContext.tfa?.sctToken = sctToken

        val authResponse = AuthResponse(registerTOTPResponse)
        authResponse.resolvableContext = resolvableContext
        return authResponse
    }
}
