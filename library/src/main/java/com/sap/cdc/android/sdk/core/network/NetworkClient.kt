package com.sap.cdc.android.sdk.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse


/**
 * Network client wrapper for HTTP operations in the SAP CIAM SDK.
 * 
 * This class serves as a lightweight wrapper around Ktor's HttpClient, providing
 * a consistent interface for network operations throughout the SDK. It abstracts
 * the HTTP client creation and management through the HttpClientProvider pattern.
 * 
 * The NetworkClient is responsible for:
 * - Providing access to a configured HttpClient instance
 * - Delegating HTTP client creation to the injected provider
 * - Enabling flexible HTTP client configuration through dependency injection
 * 
 * By default, it uses [KtorHttpClientProvider] for production environments,
 * but can accept custom providers for testing or specialized configurations.
 * 
 * @property httpClientProvider The provider responsible for creating and configuring
 *                              the HttpClient. Defaults to [KtorHttpClientProvider].
 * 
 * @constructor Creates a NetworkClient with the specified HTTP client provider.
 * 
 * @author Tal Mirmelshtein
 * @since 10/06/2024
 * 
 * Copyright: SAP LTD.
 * 
 * @see com.sap.cdc.android.sdk.core.network.HttpClientProvider
 * @see com.sap.cdc.android.sdk.core.network.KtorHttpClientProvider
 * @see com.sap.cdc.android.sdk.core.CoreClient
 */
class NetworkClient(
    private val httpClientProvider: HttpClientProvider = KtorHttpClientProvider()
) {
    companion object {
        /**
         * Log tag for NetworkClient-related logging operations.
         */
        internal const val LOG_TAG = "NetworkClient"
    }

    /**
     * Provides access to the configured HttpClient instance.
     * 
     * This method delegates to the HttpClientProvider to create or retrieve
     * the HttpClient. The client is configured with all necessary settings
     * for CIAM API communication (timeouts, logging, default headers, etc.).
     * 
     * @return A configured Ktor HttpClient ready for making HTTP requests
     * 
     * @see HttpClientProvider.createHttpClient
     */
    fun http(): HttpClient = httpClientProvider.createHttpClient()
}

/**
 * Custom exception class for HTTP-related errors in CDC operations.
 * 
 * This exception extends Ktor's ResponseException to provide additional context
 * about HTTP failures, including both the HTTP status code and a descriptive
 * failure reason.
 * 
 * The exception message format: "Status: {status_code}. Failure: {failure_reason}"
 * 
 * This exception is typically caught and converted to a [CIAMResponse] error
 * by the [Api] class for consistent error handling throughout the SDK.
 * 
 * @property response The HTTP response that caused the exception
 * @property failureReason A descriptive reason for the failure (may be null)
 * @property cachedResponseText The cached response body text for error analysis
 * 
 * @constructor Creates an HttpExceptions with HTTP response details and failure information.
 *
 * @see com.sap.cdc.android.sdk.core.api.CIAMResponse.fromHttpException
 * @see com.sap.cdc.android.sdk.core.api.Api
 */
class HttpExceptions(
    response: HttpResponse,
    failureReason: String?,
    cachedResponseText: String,
) : ResponseException(response, cachedResponseText) {
    /**
     * The formatted error message including HTTP status and failure reason.
     * 
     * Format: "Status: {status_code}. Failure: {failure_reason}"
     */
    override val message: String = "Status: ${response.status}." + " Failure: $failureReason"
}
