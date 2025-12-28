package com.sap.cdc.android.sdk.core.network

import com.sap.cdc.android.sdk.CDCDebuggable
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders

/**
 * Production implementation of HttpClientProvider using Ktor with Android engine.
 * 
 * This is the default HTTP client provider used by the SAP CDC SDK in production
 * environments. It configures a Ktor HttpClient with the Android engine optimized
 * for CDC API communication.
 * 
 * Configuration includes:
 * - **Android Engine**: Native Android HTTP engine for optimal performance
 * - **Timeouts**: 30-second connect and socket timeouts
 * - **Logging**: Comprehensive request/response logging through CDCDebuggable
 * - **Response Observer**: HTTP status monitoring for debugging
 * - **Default Headers**: Content-Type set to application/x-www-form-urlencoded
 * 
 * The client is configured with sensible defaults suitable for most CDC operations.
 * For custom configurations or testing, implement a custom [HttpClientProvider].
 * 
 * @see com.sap.cdc.android.sdk.core.network.HttpClientProvider
 * @see com.sap.cdc.android.sdk.core.network.NetworkClient
 * @see com.sap.cdc.android.sdk.core.CoreClient
 */
class KtorHttpClientProvider : HttpClientProvider {
    
    companion object {
        /**
         * Log tag for KtorHttpClientProvider logging operations.
         */
        private const val LOG_TAG = "KtorHttpClientProvider"
        
        /**
         * Timeout duration in milliseconds for both connect and socket operations.
         * Set to 30 seconds (30,000 ms) to allow sufficient time for CDC API responses.
         */
        private const val TIME_OUT = 30_000
    }

    /**
     * Creates and configures a production-ready HttpClient with Android engine.
     * 
     * This method creates a new Ktor HttpClient instance configured with:
     * 
     * **Engine Configuration:**
     * - Android native HTTP engine
     * - 30-second connect timeout
     * - 30-second socket timeout
     * 
     * **Installed Plugins:**
     * - **Logging**: Logs all HTTP traffic (headers, body, etc.) through CDCDebuggable
     * - **ResponseObserver**: Monitors HTTP response status codes for debugging
     * - **DefaultRequest**: Sets default Content-Type header to application/x-www-form-urlencoded
     * 
     * @return A fully configured HttpClient instance ready for CDC API requests
     * 
     * @see HttpClientProvider.createHttpClient
     */
    override fun createHttpClient(): HttpClient = HttpClient(Android) {
        engine {
            connectTimeout = TIME_OUT
            socketTimeout = TIME_OUT
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    CDCDebuggable.log(LOG_TAG, message)
                }
            }
            level = LogLevel.ALL
        }

        install(ResponseObserver) {
            onResponse { response ->
                CDCDebuggable.log(LOG_TAG, "HTTP Status: ${response.status.value}")
            }
        }

        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded)
        }
    }
}
