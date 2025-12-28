package com.sap.cdc.android.sdk.core.api

import com.sap.cdc.android.sdk.BuildConfig
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.core.api.utils.AndroidBase64Encoder
import com.sap.cdc.android.sdk.core.api.utils.Signing
import com.sap.cdc.android.sdk.core.api.utils.SigningSpec
import io.ktor.http.HttpMethod
import io.ktor.util.generateNonce

/**
 * Builder class for constructing SAP CDC API requests.
 * 
 * This class provides a fluent interface for building CDC API requests with all necessary
 * parameters, headers, authentication, and signatures. It automatically handles:
 * - Default request parameters (API key, SDK version, format, nonce)
 * - HTTP method configuration
 * - Request signing for authenticated operations
 * - User-Agent header extraction and configuration
 * - GMID handling
 * 
 * The CDCRequest uses a builder pattern with method chaining to construct requests:
 * ```
 * CDCRequest(siteConfig)
 *     .api("accounts.getAccountInfo")
 *     .authenticated(token)
 *     .timestamp(timestamp)
 *     .sign(secret)
 * ```
 * 
 * @property siteConfig The SAP CDC site configuration containing API credentials
 * 
 * @constructor Creates a new CDCRequest with default parameters initialized from the site configuration.
 * 
 * @author Tal Mirmelshtein
 * @since 10/06/2024
 * 
 * Copyright: SAP LTD.
 * 
 * @see com.sap.cdc.android.sdk.core.SiteConfig
 * @see com.sap.cdc.android.sdk.core.api.Api
 */
class CDCRequest(
    siteConfig: SiteConfig
) {
    companion object {
        /**
         * Log tag for CDCRequest-related logging operations.
         */
        const val LOG_TAG = "CDCRequest"
    }

    /**
     * The HTTP method for the request. Defaults to POST.
     */
    private var method: String = HttpMethod.Post.value // Default method is post.
    
    /**
     * The CDC API endpoint to call (e.g., "accounts.login", "accounts.getAccountInfo").
     */
    var api: String = ""
    
    /**
     * Request parameters to be sent with every API call.
     * 
     * Pre-populated with default parameters:
     * - apiKey: The site's API key
     * - sdk: SDK version identifier
     * - targetEnv: Always set to "mobile"
     * - format: Response format (json)
     * - httpStatusCodes: Enable HTTP status codes in responses
     * - nonce: Unique request identifier for security
     */
    var parameters: MutableMap<String, String> = sortedMapOf(
        "apiKey" to siteConfig.apiKey,
        "sdk" to "Android_rev2_${BuildConfig.VERSION}",
        "targetEnv" to "mobile",
        "format" to "json",
        "httpStatusCodes" to "true", //TODO: Make configurable.
        "nonce" to generateNonce(),
    )

    /**
     * The User-Agent string extracted from system properties.
     * Null if the system property cannot be accessed.
     */
    private var userAgent: String? = null

    /**
     * HTTP headers to be sent with the request.
     * 
     * Pre-populated with:
     * - apiKey: The site's API key
     * - User-Agent: Device/browser identification (if available)
     */
    var headers: MutableMap<String, String> = mutableMapOf(
        "apiKey" to siteConfig.apiKey
    )

    init {
        try {
            userAgent = System.getProperty("http.agent")
        } catch (ex: Exception) {
            CDCDebuggable.log(LOG_TAG, "Unable to fetch system property http.agent.")
        }
        if (userAgent != null) {
            headers["User-Agent"] = userAgent!!
        }
    }

    /**
     * Sets the HTTP method for the request.
     * 
     * @param method The HTTP method (e.g., HttpMethod.Get.value, HttpMethod.Post.value)
     * @return This CDCRequest instance for method chaining
     */
    fun method(method: String) = apply {
        this.method = method
    }

    /**
     * Adds custom parameters to the request.
     * 
     * This method merges the provided parameters with existing ones. It ensures that
     * the `targetEnv` parameter is always set to "mobile" by removing any non-mobile
     * target environment values from the provided parameters.
     * 
     * @param parameters A map of parameter key-value pairs to add to the request
     * @return This CDCRequest instance for method chaining
     */
    fun parameters(parameters: MutableMap<String, String>) = apply {
        // Make sure that we always send mobile as targetEnv field.
        val targetEnvironment = parameters["targetEnv"]
        if (targetEnvironment != null && targetEnvironment != "mobile") {
            parameters.remove("targetEnv")
        }
        this.parameters += parameters
    }

    /**
     * Adds custom HTTP headers to the request.
     * 
     * This method merges the provided headers with existing ones. If headers is null,
     * no changes are made.
     * 
     * @param headers A map of header key-value pairs to add to the request, or null
     * @return This CDCRequest instance for method chaining
     */
    fun headers(headers: MutableMap<String, String>?) = apply {
        if (headers != null) {
            this.headers += headers
        }
    }

    /**
     * Sets the GMID parameter for the request.
     * 
     * The GMID is a unique identifier used by CDC to track user sessions across devices
     * and platforms. This should be set when a GMID is available from previous interactions.
     * 
     * @param gmid The GMID to include in the request
     * @return This CDCRequest instance for method chaining
     */
    fun gmid(gmid: String) = apply {
        parameters["gmid"] = gmid
    }

    /**
     * Sets the CDC API endpoint to call.
     * 
     * The API endpoint should be specified in the format "namespace.method"
     * (e.g., "accounts.login", "accounts.getAccountInfo", "accounts.setAccountInfo").
     * 
     * @param api The CDC API endpoint name
     * @return This CDCRequest instance for method chaining
     */
    fun api(api: String) = apply {
        this.api = api
    }

    /**
     * Marks the request as authenticated by adding an OAuth token.
     * 
     * This method should be called for API operations that require user authentication.
     * The token is typically obtained from a successful login or registration flow.
     * 
     * @param token The OAuth token obtained from a previous authentication
     * @return This CDCRequest instance for method chaining
     */
    fun authenticated(token: String) = apply {
        parameters["oauth_token"] = token
    }

    /**
     * Sets the timestamp parameter for the request.
     * 
     * The timestamp is used in request signing to prevent replay attacks. It should
     * be synchronized with the server time using the timestamp from SiteConfig.
     * 
     * @param timestamp The Unix epoch timestamp in seconds as a string
     * @return This CDCRequest instance for method chaining
     * 
     * @see com.sap.cdc.android.sdk.core.SiteConfig.getServerTimestamp
     */
    fun timestamp(timestamp: String) = apply {
        parameters["timestamp"] = timestamp
    }

    /**
     * Signs the request with the provided secret key.
     * 
     * This method generates a cryptographic signature for the request using the provided
     * secret key, API endpoint, HTTP method, and all request parameters. The signature
     * is added to the parameters as "sig" and is used by CDC to verify request authenticity
     * and prevent tampering.
     * 
     * Request signing is required for:
     * - Operations that modify user data
     * - Authenticated API calls
     * - Sensitive operations that require additional security
     * 
     * The signature is calculated using the Base64-encoded HMAC-SHA1 hash of the
     * canonicalized request parameters.
     * 
     * @param secret The secret key used for signing (typically the user secret or API secret)
     * @return This CDCRequest instance for method chaining
     * 
     * @see Signing
     * @see SigningSpec
     */
    fun sign(secret: String) = apply {
        val signature = Signing(base64Encoder = AndroidBase64Encoder()).newSignature(
            SigningSpec(
                secret,
                api,
                method,
                parameters
            )
        )
        parameters["sig"] = signature
    }

}
