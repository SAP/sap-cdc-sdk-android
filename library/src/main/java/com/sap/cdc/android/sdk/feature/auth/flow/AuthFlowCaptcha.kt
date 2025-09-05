package com.sap.cdc.android.sdk.feature.auth.flow

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.api.utils.AndroidBase64Encoder
import com.sap.cdc.android.sdk.extensions.encodeWith
import com.sap.cdc.android.sdk.extensions.jwtDecode
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_RISK_SAPTCHA_GET_CHALLENGE
import com.sap.cdc.android.sdk.feature.auth.AuthEndpoints.Companion.EP_RISK_SAPTCHA_VERIFY
import com.sap.cdc.android.sdk.feature.auth.AuthResponse
import com.sap.cdc.android.sdk.feature.auth.AuthenticationApi
import com.sap.cdc.android.sdk.feature.auth.IAuthResponse
import com.sap.cdc.android.sdk.feature.auth.session.SessionService

class CaptchaAuthFlow(coreClient: CoreClient, sessionService: SessionService) :
    AuthFlow(coreClient, sessionService) {

    companion object {
        const val LOG_TAG = "AuthFlowCaptcha"
    }

    suspend fun getSaptchaToken(): IAuthResponse {
        CDCDebuggable.log(TFAAuthFlow.LOG_TAG, "startChallenge")
        val getJwt =
            AuthenticationApi(coreClient, sessionService).send(
                EP_RISK_SAPTCHA_GET_CHALLENGE,
                parameters = mutableMapOf()
            )
        val authResponse = AuthResponse(getJwt)
        if (authResponse.isError()) {
            // Flow ends with error.
            return authResponse
        }

        val token = getJwt.stringField("saptchaToken") as String
        CDCDebuggable.log(TFAAuthFlow.LOG_TAG, "token: $token")

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

        val verifyResponse = AuthResponse(verifyJwt)
        return verifyResponse
    }

    private fun verifyChallenge(challengeId: String, pattern: String, i: Int): Boolean {
        val value = "$challengeId.$i".encodeWith("SHA-512")
        val regex = pattern.toRegex()
        return regex.containsMatchIn(value)
    }

}
