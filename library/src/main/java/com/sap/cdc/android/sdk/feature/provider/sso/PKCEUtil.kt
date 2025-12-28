package com.sap.cdc.android.sdk.feature.provider.sso

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * PKCE (Proof Key for Code Exchange) utility for OAuth 2.0.
 * 
 * Generates and manages PKCE code verifier and challenge pairs for
 * secure OAuth 2.0 authorization flows, protecting against authorization
 * code interception attacks.
 * 
 * @author Tal Mirmelshtein
 * @since 13/12/2024
 * 
 * Copyright: SAP LTD.
 */
class PKCEUtil {

    var verifier: String? = null
    internal var challenge: String? = null

    /**
     * Generates a new PKCE code verifier and challenge pair.
     * Stores the verifier and challenge in instance properties.
     */
    fun newChallenge() {
        verifier = generateCodeVerifier()
        challenge = generateCodeChallenge(verifier!!)
    }

    /**
     * Generates a cryptographically random code verifier.
     * Creates a 32-byte random value and Base64-encodes it.
     * @return Base64-encoded code verifier string
     */
    private fun generateCodeVerifier(): String {
        val sr = SecureRandom()
        val code = ByteArray(32)
        sr.nextBytes(code)
        val v = Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        return v
    }

    /**
     * Generates a code challenge from the verifier using SHA-256.
     * @param verifier The code verifier to hash
     * @return Base64-encoded SHA-256 hash of the verifier
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
