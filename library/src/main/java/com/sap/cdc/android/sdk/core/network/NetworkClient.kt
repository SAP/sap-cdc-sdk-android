package com.sap.cdc.android.sdk.core.network

import com.sap.cdc.android.sdk.CDCDebuggable
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders


/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
class NetworkClient(
    private val httpClientProvider: HttpClientProvider = KtorHttpClientProvider()
) {
    companion object {
        internal const val LOG_TAG = "NetworkClient"
    }

    fun http(): HttpClient = httpClientProvider.createHttpClient()
}

class HttpExceptions(
    response: HttpResponse,
    failureReason: String?,
    cachedResponseText: String,
) : ResponseException(response, cachedResponseText) {
    override val message: String = "Status: ${response.status}." + " Failure: $failureReason"
}
