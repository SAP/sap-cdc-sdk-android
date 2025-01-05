package com.sap.cdc.android.sdk.auth.provider.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Created by Tal Mirmelshtein on 13/12/2024
 * Copyright: SAP LTD.
 */

/**
 * PKCE util class.
 */
class PKCEUtil {

    var verifier: String? = null
    internal var challenge: String? = null

    /**
     * Generate a new challenge.
     */
    fun newChallenge() {
        verifier = generateCodeVerifier()
        challenge = generateCodeChallenge(verifier!!)
    }

    /**
     * Generate a new code verifier.
     */
    private fun generateCodeVerifier(): String {
        val sr = SecureRandom()
        val code = ByteArray(32)
        sr.nextBytes(code)
        val v = Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        return v
    }

    /**
     * Generate a new code challenge.
     */
    private fun generateCodeChallenge(verifier: String): String {
        val bytes: ByteArray = verifier.toByteArray(charset("UTF-8"))
        val md = MessageDigest.getInstance("SHA-256")
        md.update(bytes, 0, bytes.size)
        val digest = md.digest()
        val c =
            Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        return c
    }
}