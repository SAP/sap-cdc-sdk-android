package com.sap.cdc.android.sdk.feature.captcha

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.utils.AndroidBase64Encoder
import com.sap.cdc.android.sdk.extensions.encodeWith
import com.sap.cdc.android.sdk.extensions.jwtDecode
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_RISK_SAPTCHA_GET_CHALLENGE
import com.sap.cdc.android.sdk.feature.AuthEndpoints.Companion.EP_RISK_SAPTCHA_VERIFY
import com.sap.cdc.android.sdk.feature.AuthFlow
import com.sap.cdc.android.sdk.feature.AuthenticationApi
import com.sap.cdc.android.sdk.feature.session.SessionService

class AuthCaptchaFlow(coreClient: CoreClient, sessionService: SessionService) :
    AuthFlow(coreClient, sessionService) {

    companion object {
        const val LOG_TAG = "AuthCaptchaFlow"
    }

    suspend fun getSaptchaToken(authCallbacks: AuthCallbacks) {
        CDCDebuggable.log(LOG_TAG, "startChallenge")

        val getJwt =
            AuthenticationApi(coreClient, sessionService).send(
                EP_RISK_SAPTCHA_GET_CHALLENGE,
                parameters = mutableMapOf()
            )

        // Error clause
        if (getJwt.isError()) {
            val authError = createAuthError(getJwt)
            authCallbacks.onError?.invoke(authError)
            return
        }

        val token = getJwt.stringField("saptchaToken") as String
        CDCDebuggable.log(LOG_TAG, "token: $token")

        val jwtObject = token.jwtDecode(AndroidBase64Encoder())
        val jti = jwtObject.getString("jti")
        val pattern = jwtObject.getString("pattern")

        // Verify challenge
        var i = 0
        var isFinished = false
        while (!isFinished) {
            i += 1
            isFinished = verifyChallenge(jti, pattern, i)
        }

        // Verify token
        val params = mutableMapOf("token" to "$token|$i")
        val verifyJwt =
            AuthenticationApi(coreClient, sessionService).send(
                EP_RISK_SAPTCHA_VERIFY,
                parameters = params
            )

        // Error clause
        if (verifyJwt.isError()) {
            val authError = createAuthError(verifyJwt)
            authCallbacks.onError?.invoke(authError)
            return
        }

        val authSuccess = createAuthSuccess(verifyJwt)
        authCallbacks.onSuccess?.invoke(authSuccess)
    }

    private fun verifyChallenge(challengeId: String, pattern: String, i: Int): Boolean {
        val value = "$challengeId.$i".encodeWith("SHA-512")
        val regex = pattern.toRegex()
        return regex.containsMatchIn(value)
    }
}