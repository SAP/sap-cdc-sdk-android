package com.sap.cdc.android.sdk.core.network

import io.ktor.client.HttpClient

/**
 * Abstraction for providing configured HttpClient instances.
 * 
 * This interface enables dependency injection of HTTP client implementations, allowing
 * different Ktor engines and configurations for production versus testing environments.
 * 
 * The primary purpose is to decouple the HTTP client creation from the networking layer,
 * enabling:
 * - Easy substitution of HTTP engines (Android, OkHttp, MockEngine for tests)
 * - Custom client configuration per environment
 * - Testability without requiring actual network calls
 * 
 * Production implementation: [KtorHttpClientProvider]
 * 
 * Example test implementation:
 * ```
 * class MockHttpClientProvider : HttpClientProvider {
 *     override fun createHttpClient(): HttpClient = HttpClient(MockEngine) {
 *         // Configure mock responses
 *     }
 * }
 * ```
 * 
 * @see com.sap.cdc.android.sdk.core.network.KtorHttpClientProvider
 * @see com.sap.cdc.android.sdk.core.network.NetworkClient
 * @see com.sap.cdc.android.sdk.core.CoreClient
 */
interface HttpClientProvider {
    /**
     * Creates and configures an HttpClient instance.
     * 
     * Implementations should return a fully configured Ktor HttpClient with appropriate:
     * - Engine configuration (timeouts, connection pools, etc.)
     * - Plugins (logging, content negotiation, authentication, etc.)
     * - Default headers and request settings
     * 
     * The returned client will be used by [NetworkClient] for all CIAM API communications.
     * 
     * @return A configured HttpClient ready for making HTTP requests
     * 
     * @see NetworkClient.http
     */
    fun createHttpClient(): HttpClient
}
