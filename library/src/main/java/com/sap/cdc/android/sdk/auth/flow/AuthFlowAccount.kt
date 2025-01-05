package com.sap.cdc.android.sdk.auth.flow

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_FINALIZE_REGISTRATION
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_GET_ACCOUNT_INFO
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_GET_CONFLICTING_ACCOUNTS
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_ID_TOKEN_EXCHANGE
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_ACCOUNTS_SET_ACCOUNT_INFO
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_TFA_EMAILS_COMPLETE_VERIFICATION
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_TFA_EMAILS_SEND_CODE
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_TFA_EMAIL_GET
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_TFA_FINALIZE
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_TFA_GET_PROVIDERS
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_TFA_INIT
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_TFA_PHONE_COMPLETE_VERIFICATION
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_TFA_PHONE_GET
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_TFA_PHONE_SEND_CODE
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_TFA_PUSH_OPT_IN
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_TFA_PUSH_VERIFY
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_TFA_TOTP_REGISTER
import com.sap.cdc.android.sdk.auth.AuthEndpoints.Companion.EP_TFA_TOTP_VERIFY
import com.sap.cdc.android.sdk.auth.AuthResponse
import com.sap.cdc.android.sdk.auth.AuthenticationApi
import com.sap.cdc.android.sdk.auth.AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
import com.sap.cdc.android.sdk.auth.AuthenticationService.Companion.CDC_DEVICE_INFO
import com.sap.cdc.android.sdk.auth.IAuthResponse
import com.sap.cdc.android.sdk.auth.ResolvableContext
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.auth.tfa.TFAEmailEntity
import com.sap.cdc.android.sdk.auth.tfa.TFAPhoneEntity
import com.sap.cdc.android.sdk.auth.tfa.TFAProvider
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import kotlinx.serialization.json.Json

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class AccountAuthFlow(coreClient: CoreClient, sessionService: SessionService) :
    AuthFlow(coreClient, sessionService) {

    companion object {
        const val LOG_TAG = "AccountAuthFlow"
    }

    /**
     * Request updated account information.
     *
     * @see [accounts.getAccountInfo](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/cab69a86edae49e2be93fd51b78fc35b.html?q=accounts.getAccountInfo)
     */
    suspend fun getAccountInfo(parameters: MutableMap<String, String>? = mutableMapOf()): IAuthResponse {
        CDCDebuggable.log(LOG_TAG, "getAccountInfo: with parameters:$parameters")
        val getAccountInfo =
            AuthenticationApi(coreClient, sessionService).genericSend(
                EP_ACCOUNTS_GET_ACCOUNT_INFO,
                parameters!!
            )
        return AuthResponse(getAccountInfo)
    }

    /**
     * Update account information.
     * NOTE: some account parameters needs to be JSON serialized.
     *
     * @see [accounts.setAccountInfo](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/41398a8670b21014bbc5a10ce4041860.html?q=accounts.getAccountInfo)
     */
    suspend fun setAccountInfo(parameters: MutableMap<String, String>? = mutableMapOf()): IAuthResponse {
        CDCDebuggable.log(LOG_TAG, "setAccountInfo: with parameters:$parameters")
        val setAccount =
            AuthenticationApi(coreClient, sessionService).genericSend(
                EP_ACCOUNTS_SET_ACCOUNT_INFO,
                parameters ?: mutableMapOf()
            )
        return AuthResponse(setAccount)
    }

    /**
     * Request conflicting accounts information.
     * NOTE: Call requires regToken due to interruption source.
     *
     * @see [accounts.getConflictingAccounts](https://help.sap.com/docs/SAP_CUSTOMER_DATA_CLOUD/8b8d6fffe113457094a17701f63e3d6a/4134d7df70b21014bbc5a10ce4041860.html?q=conflictingAccounts)
     */
    suspend fun getConflictingAccounts(parameters: MutableMap<String, String>? = mutableMapOf()): IAuthResponse {
        CDCDebuggable.log(LOG_TAG, "getConflictingAccounts: with parameters:$parameters")
        val getConflictingAccounts = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_ACCOUNTS_GET_CONFLICTING_ACCOUNTS,
            parameters ?: mutableMapOf()
        )
        return AuthResponse(getConflictingAccounts)
    }

    /**
     * Applications (mobile/web) within the same site group are now able to share a session from the mobile application
     * to a web page running the JS SDK.
     *
     * Request code required to exchange the session.
     */
    suspend fun getAuthCode(parameters: MutableMap<String, String>): IAuthResponse {
        parameters["response_type"] = "code"
        CDCDebuggable.log(LOG_TAG, "getAuthCode: with parameters:$parameters")
        val tokenExchange = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_ACCOUNTS_ID_TOKEN_EXCHANGE,
            parameters
        )
        return AuthResponse(tokenExchange)
    }

    /**
     * Request account two factor authentication providers:
     * Active - Providers that are currently active and the user can use to authenticate.
     * Inactive - Providers that are currently inactive and the user can activate to use for authentication.
     */
    suspend fun getTFAProviders(parameters: MutableMap<String, String>? = mutableMapOf()): IAuthResponse {
        val tfaProvidersResponse = AuthenticationApi(coreClient, sessionService).genericSend(
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
        val initTFAResponse = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_TFA_INIT,
            parameters
        )
        if (initTFAResponse.isError()) return AuthResponse(initTFAResponse)

        // Clear parameters for reuse.
        parameters.clear()

        val assertion = initTFAResponse.stringField("gigyaAssertion") ?: ""

        // Obtain device info from secure storage.
        val esp = coreClient.siteConfig.applicationContext.getEncryptedPreferences(
            CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
        )
        val deviceInfo = esp.getString(CDC_DEVICE_INFO, "") ?: ""

        parameters["gigyaAssertion"] = assertion
        parameters["deviceInfo"] = deviceInfo
        val pushOptInResponse = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_TFA_PUSH_OPT_IN,
            parameters
        )
        return AuthResponse(pushOptInResponse)
    }

    suspend fun finalizeOptInForPushTFA(parameters: MutableMap<String, String>): IAuthResponse {
        val verifyPushResponse = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_TFA_PUSH_VERIFY,
            parameters
        )
        if (verifyPushResponse.isError()) return AuthResponse(verifyPushResponse)

        // Clear parameters for reuse.
        parameters.remove("verificationToken")
        parameters["providerAssertion"] = verifyPushResponse.stringField("providerAssertion") ?: ""

        val finalizePushResponse = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_TFA_FINALIZE,
            parameters
        )
        return AuthResponse(finalizePushResponse)
    }

    suspend fun verifyPushTFA(parameters: MutableMap<String, String>): IAuthResponse {
        val verifyPushResponse = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_TFA_PUSH_VERIFY,
            parameters
        )
        return AuthResponse(verifyPushResponse)
    }

    suspend fun getRegisteredEmails(
        resolvableContext: ResolvableContext,
        parameters: MutableMap<String, String>
    ): IAuthResponse {
        parameters["regToken"] = resolvableContext.regToken!!
        val initTFAResponse = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_TFA_INIT,
            parameters
        )
        if (initTFAResponse.isError()) return AuthResponse(initTFAResponse)

        // Clear parameters for reuse.
        parameters.clear()

        val assertion = initTFAResponse.stringField("gigyaAssertion") ?: ""

        parameters["gigyaAssertion"] = assertion
        val getEmailsResponse = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_TFA_EMAIL_GET,
            parameters
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
        parameters["gigyaAssertion"] = resolvableContext.tfa?.assertion!!
        val sendCodeResponse = AuthenticationApi(coreClient, sessionService).genericSend(
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
        parameters: MutableMap<String, String>
    ): IAuthResponse {
        parameters["regToken"] = resolvableContext.regToken!!
        val initTFAResponse = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_TFA_INIT,
            parameters
        )

        // Clear parameters for reuse.
        parameters.remove("provider")

        val assertion = initTFAResponse.stringField("gigyaAssertion") ?: ""

        parameters["gigyaAssertion"] = assertion
        parameters["phone"] = phoneNumber
        if (initTFAResponse.isError()) return AuthResponse(initTFAResponse)
        val sendCodeResponse = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_TFA_PHONE_SEND_CODE,
            parameters
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
        parameters["regToken"] = resolvableContext.regToken!!
        val initTFAResponse = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_TFA_INIT,
            parameters
        )
        if (initTFAResponse.isError()) return AuthResponse(initTFAResponse)

        // Clear parameters for reuse.
        parameters.remove("provider")
        parameters.remove("mode")

        val assertion = initTFAResponse.stringField("gigyaAssertion") ?: ""

        parameters["gigyaAssertion"] = assertion
        val getPhoneNumbersResult = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_TFA_PHONE_GET,
            parameters
        )
        if (getPhoneNumbersResult.isError()) return AuthResponse(getPhoneNumbersResult)

        val phonesJson = getPhoneNumbersResult.stringField("phones")
        val phones = Json.decodeFromString<List<TFAPhoneEntity>>(phonesJson!!)

        val authResponse = AuthResponse(getPhoneNumbersResult)
        resolvableContext.tfa?.assertion = assertion
        resolvableContext.tfa?.phones = phones
        authResponse.resolvableContext = resolvableContext
        return authResponse
    }

    suspend fun sendPhoneCode(
        resolvableContext: ResolvableContext,
        parameters: MutableMap<String, String>
    ): IAuthResponse {
        val sendCodeResponse = AuthenticationApi(coreClient, sessionService).genericSend(
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
        parameters["gigyaAssertion"] = resolvableContext.tfa?.assertion!!
        if (resolvableContext.tfa?.phvToken != null) {
            parameters["phvToken"] = resolvableContext.tfa?.phvToken!!
        }
        if (resolvableContext.tfa?.sctToken != null) {
            parameters["sctToken"] = resolvableContext.tfa?.sctToken!!
        }
        val verifyCodeResponse = AuthenticationApi(coreClient, sessionService).genericSend(
            when (provider) {
                TFAProvider.EMAIL -> EP_TFA_EMAILS_COMPLETE_VERIFICATION
                TFAProvider.PHONE -> EP_TFA_PHONE_COMPLETE_VERIFICATION
                TFAProvider.TOTP -> EP_TFA_TOTP_VERIFY
                else -> "" // Redundant.
            },
            parameters
        )
        if (verifyCodeResponse.isError()) return AuthResponse(verifyCodeResponse)

        // Clear parameters for reuse.
        parameters.clear()

        val providerAssertion = verifyCodeResponse.stringField("providerAssertion") ?: ""

        parameters["regToken"] = resolvableContext.regToken ?: ""
        parameters["gigyaAssertion"] = resolvableContext.tfa?.assertion ?: ""
        parameters["providerAssertion"] = providerAssertion
        parameters["tempDevice"] = (!rememberDevice).toString()
        val finalizeTFAResult = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_TFA_FINALIZE,
            parameters
        )

        // Clear parameters for reuse.
        parameters.clear()

        // Call finalize registration to complete the flow.
        parameters["regToken"] = resolvableContext.regToken ?: ""
        parameters["includeUserInfo"] = "true"
        val finalizeRegistrationResult = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_ACCOUNTS_FINALIZE_REGISTRATION,
            parameters
        )
        return AuthResponse(finalizeRegistrationResult)
    }

    suspend fun registerTOTP(
        resolvableContext: ResolvableContext,
        parameters: MutableMap<String, String>
    ): IAuthResponse {
        parameters["regToken"] = resolvableContext.regToken!!
        val initTFAResponse = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_TFA_INIT,
            parameters
        )
        if (initTFAResponse.isError()) return AuthResponse(initTFAResponse)

        // Clear parameters for reuse.
        parameters.clear()

        val assertion = initTFAResponse.stringField("gigyaAssertion") ?: ""

        parameters["gigyaAssertion"] = assertion
        val registerTOTPResponse = AuthenticationApi(coreClient, sessionService).genericSend(
            EP_TFA_TOTP_REGISTER,
            parameters,
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