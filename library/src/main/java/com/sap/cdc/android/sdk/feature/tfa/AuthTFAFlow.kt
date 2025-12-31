package com.sap.cdc.android.sdk.feature.tfa

import com.sap.cdc.android.sdk.CIAMDebuggable
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_ACCOUNTS_FINALIZE_REGISTRATION
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_TFA_EMAILS_COMPLETE_VERIFICATION
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_TFA_EMAILS_SEND_CODE
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_TFA_EMAIL_GET
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_TFA_FINALIZE
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_TFA_INIT
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_TFA_PHONE_COMPLETE_VERIFICATION
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_TFA_PHONE_GET
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_TFA_PHONE_SEND_CODE
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_TFA_PUSH_OPT_IN
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_TFA_PUSH_VERIFY
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_TFA_TOTP_REGISTER
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_TFA_TOTP_VERIFY
import com.sap.cdc.android.sdk.feature.AuthFlow
import com.sap.cdc.android.sdk.feature.AuthenticationApi
import com.sap.cdc.android.sdk.feature.AuthenticationService.Companion.CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
import com.sap.cdc.android.sdk.feature.AuthenticationService.Companion.CDC_DEVICE_INFO
import com.sap.cdc.android.sdk.feature.TwoFactorContext
import com.sap.cdc.android.sdk.feature.session.SessionService
import kotlinx.serialization.json.Json

class AuthTFAFlow(coreClient: CoreClient, sessionService: SessionService) :
    AuthFlow(coreClient, sessionService) {

    companion object {
        const val TFA_ENDPOINT = "tfa"
    }

    /**
     * Initiate push TFA registration.
     * NOTE: Requires deviceInfo to be sent.
     */
    suspend fun optInForPushNotifications(parameters: MutableMap<String, String>, authCallbacks: AuthCallbacks) {
        CIAMDebuggable.log(LOG_TAG, "optInForPushTFA: with parameters:$parameters")

        val initTFA = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_INIT,
            parameters
        )

        // Error case (init)
        if (initTFA.isError()) {
            authCallbacks.onError?.invoke(createAuthError(initTFA))
            return
        }

        val assertion = initTFA.stringField("gigyaAssertion") ?: ""

        // Obtain device info from secure storage.
        val esp = coreClient.siteConfig.applicationContext.getEncryptedPreferences(
            CDC_AUTHENTICATION_SERVICE_SECURE_PREFS
        )
        val deviceInfo = esp.getString(CDC_DEVICE_INFO, "") ?: ""

        parameters.clear()
        parameters["gigyaAssertion"] = assertion
        parameters["deviceInfo"] = deviceInfo
        val optIn = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_PUSH_OPT_IN,
            parameters
        )

        // Error case (opt-in)
        if (optIn.isError()) {
            authCallbacks.onError?.invoke(createAuthError(optIn))
        }

        // Success case
        val authSuccess = createAuthSuccess(optIn)
        authCallbacks.onSuccess?.invoke(authSuccess)
    }

    suspend fun verifyPushNotification(
        parameters: MutableMap<String, String>,
        finalize: Boolean = false,
        authCallbacks: AuthCallbacks
    ) {
        CIAMDebuggable.log(LOG_TAG, "finalizeOptInForPushTFA: with parameters:$parameters")

        val verify = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_PUSH_VERIFY,
            parameters
        )

        // Error case (verify)
        if (verify.isError()) {
            authCallbacks.onError?.invoke(createAuthError(verify))
            return
        }

        if (!finalize) {
            // Success case (verify only)
            val authSuccess = createAuthSuccess(verify)
            authCallbacks.onSuccess?.invoke(authSuccess)
            return
        }

        val providerAssertion = verify.stringField("providerAssertion") ?: ""

        parameters.remove("verificationToken")
        parameters["providerAssertion"] = providerAssertion
        val finalize = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_FINALIZE,
            parameters
        )

        // Error case (finalize)
        if (finalize.isError()) {
            authCallbacks.onError?.invoke(createAuthError(finalize))
        }
        // Success case
        val authSuccess = createAuthSuccess(finalize)
        authCallbacks.onSuccess?.invoke(authSuccess)
    }

    suspend fun getRegisteredEmails(
        parameters: MutableMap<String, String> = mutableMapOf(),
        twoFactorContext: TwoFactorContext,
        authCallbacks: AuthCallbacks
    ) {
        CIAMDebuggable.log(LOG_TAG, "getRegisteredEmails: with parameters:$parameters")
        parameters["regToken"] = twoFactorContext.regToken!!
        val init = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_INIT,
            parameters
        )

        // Error case (init)
        if (init.isError()) {
            authCallbacks.onError?.invoke(createAuthError(init))
            return
        }

        val assertion = init.stringField("gigyaAssertion") ?: ""

        val getEmails = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_EMAIL_GET,
            mutableMapOf("gigyaAssertion" to assertion)
        )

        // Error case (get emails)
        if (getEmails.isError()) {
            authCallbacks.onError?.invoke(createAuthError(getEmails))
            return
        }

        val emailsJson = getEmails.stringField("emails")
        val emails = Json.decodeFromString<List<TFAEmailEntity>>(emailsJson!!)

        // Success case
        val authSuccess = createAuthSuccess(getEmails)
        authCallbacks.onSuccess?.invoke(authSuccess)

        // Invoke enriched continuation callback
        twoFactorContext.emails = emails
        authCallbacks.onTwoFactorContextUpdated?.invoke(twoFactorContext)
    }

    suspend fun sendCodeToEmailAddress(
        parameters: MutableMap<String, String>,
        twoFactorContext: TwoFactorContext,
        authCallbacks: AuthCallbacks
    ) {
        CIAMDebuggable.log(LOG_TAG, "sendEmailCode: with parameters:$parameters")
        parameters["gigyaAssertion"] = twoFactorContext.assertion!!
        val sendCode = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_EMAILS_SEND_CODE,
            parameters
        )
        // Error case (send code)
        if (sendCode.isError()) {
            authCallbacks.onError?.invoke(createAuthError(sendCode))
            return
        }

        val phvToken = sendCode.stringField("phvToken") ?: ""

        // Success case
        val authSuccess = createAuthSuccess(sendCode)
        authCallbacks.onSuccess?.invoke(authSuccess)

        // Invoke enriched continuation callback
        twoFactorContext.phvToken = phvToken
        authCallbacks.onTwoFactorContextUpdated?.invoke(twoFactorContext)
    }

    suspend fun registerPhone(
        parameters: MutableMap<String, String>,
        twoFactorContext: TwoFactorContext,
        authCallbacks: AuthCallbacks
    ) {
        CIAMDebuggable.log(LOG_TAG, "registerPhone: with parameters:$parameters")
        parameters["regToken"] = twoFactorContext.regToken!!
        val init = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_INIT,
            parameters
        )
        // Error case (init)
        if (init.isError()) {
            authCallbacks.onError?.invoke(createAuthError(init))
            return
        }

        val assertion = init.stringField("gigyaAssertion") ?: ""
        twoFactorContext.assertion = assertion

        val sendCode = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_PHONE_SEND_CODE,
            mutableMapOf(
                "gigyaAssertion" to assertion, "phone" to parameters["phoneNumber"]!!,
                "method" to "sms",
                "lang" to parameters["lang"]!!
            )
        )

        // Error case (send code)
        if (sendCode.isError()) {
            authCallbacks.onError?.invoke(createAuthError(sendCode))
            return
        }

        val phvToken = sendCode.stringField("phvToken") ?: ""
        twoFactorContext.phvToken = phvToken

        // Success case
        val authSuccess = createAuthSuccess(sendCode)
        authCallbacks.onSuccess?.invoke(authSuccess)

        // Invoke enriched continuation callback
        authCallbacks.onTwoFactorContextUpdated?.invoke(twoFactorContext)
    }

    suspend fun getRegisteredPhoneNumbers(
        parameters: MutableMap<String, String>,
        twoFactorContext: TwoFactorContext,
        authCallbacks: AuthCallbacks
    ) {
        CIAMDebuggable.log(LOG_TAG, "getRegisteredPhoneNumbers: with parameters:$parameters")
        parameters["regToken"] = twoFactorContext.regToken!!
        val init = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_INIT,
            parameters
        )

        // Error case (init)
        if (init.isError()) {
            authCallbacks.onError?.invoke(createAuthError(init))
            return
        }

        val assertion = init.stringField("gigyaAssertion") ?: ""

        parameters["gigyaAssertion"] = assertion
        val getPhoneNumbers = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_PHONE_GET,
            mutableMapOf("gigyaAssertion" to assertion)
        )

        // Error case (get phone numbers)
        if (getPhoneNumbers.isError()) {
            authCallbacks.onError?.invoke(createAuthError(getPhoneNumbers))
            return
        }

        // Success case
        val authSuccess = createAuthSuccess(getPhoneNumbers)
        authCallbacks.onSuccess?.invoke(authSuccess)

        // Invoke enriched continuation callback
        val phones =
            json.decodeFromString<TFARegisteredPhoneEntities>(getPhoneNumbers.asJson()!!)
        twoFactorContext.phones = phones.phones
        twoFactorContext.assertion = assertion
        authCallbacks.onTwoFactorContextUpdated?.invoke(twoFactorContext)
    }

    suspend fun sendCodeToPhoneNumber(
        parameters: MutableMap<String, String>,
        twoFactorContext: TwoFactorContext,
        authCallbacks: AuthCallbacks
    ) {
        CIAMDebuggable.log(LOG_TAG, "sendPhoneCode: with parameters:$parameters")
        parameters["gigyaAssertion"] = twoFactorContext.assertion!!
        val sendCode = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_PHONE_SEND_CODE,
            parameters
        )

        // Error case (send code)
        if (sendCode.isError()) {
            authCallbacks.onError?.invoke(createAuthError(sendCode))
            return
        }

        val phvToken = sendCode.stringField("phvToken") ?: ""

        // Success case
        val authSuccess = createAuthSuccess(sendCode)
        authCallbacks.onSuccess?.invoke(authSuccess)

        // Invoke enriched continuation callback
        twoFactorContext.phvToken = phvToken
        authCallbacks.onTwoFactorContextUpdated?.invoke(twoFactorContext)
    }

    suspend fun registerTimeBasedOneTimePassword(
        parameters: MutableMap<String, String>,
        twoFactorContext: TwoFactorContext,
        authCallbacks: AuthCallbacks
    ) {
        CIAMDebuggable.log(LOG_TAG, "registerTOTP: with parameters:$parameters")
        parameters["regToken"] = twoFactorContext.regToken!!
        val init = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_INIT,
            parameters
        )

        // Error case (init)
        if (init.isError()) {
            authCallbacks.onError?.invoke(createAuthError(init))
            return
        }

        val assertion = init.stringField("gigyaAssertion") ?: ""

        parameters["gigyaAssertion"] = assertion
        val register = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_TOTP_REGISTER,
            mutableMapOf("gigyaAssertion" to assertion),
        )

        // Error case (register)
        if (register.isError()) {
            authCallbacks.onError?.invoke(createAuthError(register))
            return
        }

        // Success case
        val authSuccess = createAuthSuccess(register)
        authCallbacks.onSuccess?.invoke(authSuccess)

        // Invoke enriched continuation callback
        val qrCode = register.stringField("qrCode") ?: ""
        val sctToken = register.stringField("sctToken") ?: ""

        twoFactorContext.assertion = assertion
        twoFactorContext.qrCode = qrCode
        twoFactorContext.sctToken = sctToken
        authCallbacks.onTwoFactorContextUpdated?.invoke(twoFactorContext)
    }

    suspend fun verifyCode(
        parameters: MutableMap<String, String>,
        twoFactorContext: TwoFactorContext,
        provider: TFAProvider,
        rememberDevice: Boolean,
        authCallbacks: AuthCallbacks
    ) {
        CIAMDebuggable.log(LOG_TAG, "verifyCode: with parameters:$parameters")
        var assertion: String? = null
        if (twoFactorContext.assertion == null) {
            // Need to re-initiate TFA flow.
            val init = AuthenticationApi(coreClient, sessionService).send(
                EP_TFA_INIT,
                mutableMapOf(
                    "regToken" to twoFactorContext.regToken!!,
                    "provider" to provider.value,
                    "mode" to "verify"
                )
            )

            // Error case (init)
            if (init.isError()) {
                authCallbacks.onError?.invoke(createAuthError(init))
                return
            }
            assertion = init.stringField("gigyaAssertion") ?: ""
        } else {
            assertion = twoFactorContext.assertion!!
        }
        parameters["gigyaAssertion"] = assertion


        if (twoFactorContext.phvToken != null) {
            parameters["phvToken"] = twoFactorContext.phvToken!!
        }

        if (twoFactorContext.sctToken != null) {
            parameters["sctToken"] = twoFactorContext.sctToken!!
        }

        val verifyCode = AuthenticationApi(coreClient, sessionService).send(
            when (provider) {
                TFAProvider.EMAIL -> EP_TFA_EMAILS_COMPLETE_VERIFICATION
                TFAProvider.PHONE -> EP_TFA_PHONE_COMPLETE_VERIFICATION
                TFAProvider.TOTP -> EP_TFA_TOTP_VERIFY
                else -> "" // Redundant.
            },
            parameters
        )

        // Error case (verify code)
        if (verifyCode.isError()) {
            authCallbacks.onError?.invoke(createAuthError(verifyCode))
            return
        }

        val providerAssertion = verifyCode.stringField("providerAssertion") ?: ""

        val finalizeTFA = AuthenticationApi(coreClient, sessionService).send(
            EP_TFA_FINALIZE,
            mutableMapOf(
                "regToken" to twoFactorContext.regToken!!,
                "gigyaAssertion" to assertion,
                "providerAssertion" to providerAssertion,
                "tempDevice" to (!rememberDevice).toString()
            )
        )

        // Error case (finalize)
        if (finalizeTFA.isError()) {
            authCallbacks.onError?.invoke(createAuthError(finalizeTFA))
            return
        }

        val finalizeRegistration = AuthenticationApi(coreClient, sessionService).send(
            EP_ACCOUNTS_FINALIZE_REGISTRATION,
            mutableMapOf(
                "regToken" to twoFactorContext.regToken,
                "includeUserInfo" to "true"
            )
        )

        // Error case (finalize registration)
        if (finalizeRegistration.isError()) {
            authCallbacks.onError?.invoke(createAuthError(finalizeRegistration))
            return
        }

        secureNewSession(finalizeRegistration)
        // Success case
        val authSuccess = createAuthSuccess(finalizeRegistration)
        authCallbacks.onSuccess?.invoke(authSuccess)
    }
}