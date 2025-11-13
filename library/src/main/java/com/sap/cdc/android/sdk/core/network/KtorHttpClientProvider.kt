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
 * This is the default implementation that should be used in production.
 */
class KtorHttpClientProvider : HttpClientProvider {
    
    companion object {
        private const val LOG_TAG = "KtorHttpClientProvider"
        private const val TIME_OUT = 30_000
    }

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
