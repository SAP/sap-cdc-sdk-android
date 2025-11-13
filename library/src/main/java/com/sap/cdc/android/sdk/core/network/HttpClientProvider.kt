package com.sap.cdc.android.sdk.core.network

import io.ktor.client.HttpClient

/**
 * Abstraction for providing HttpClient instances.
 * This allows injecting different Ktor engines for production vs testing.
 */
interface HttpClientProvider {
    /**
     * Creates and configures an HttpClient instance.
     * @return Configured HttpClient ready for use
     */
    fun createHttpClient(): HttpClient
}
