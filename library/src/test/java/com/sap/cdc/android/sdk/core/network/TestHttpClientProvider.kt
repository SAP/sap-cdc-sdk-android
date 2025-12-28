package com.sap.cdc.android.sdk.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/**
 * Test implementation of HttpClientProvider using Ktor MockEngine.
 * Allows full control over HTTP responses for testing purposes.
 */
class TestHttpClientProvider(
    private val mockResponses: Map<String, String> = emptyMap(),
    private val defaultStatusCode: HttpStatusCode = HttpStatusCode.OK
) : HttpClientProvider {

    override fun createHttpClient(): HttpClient = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                val url = request.url.toString()
                val responseContent = mockResponses[url] ?: """{"statusCode": 200, "data": {}}"""
                
                respond(
                    content = responseContent,
                    status = defaultStatusCode,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded)
        }
    }
}
