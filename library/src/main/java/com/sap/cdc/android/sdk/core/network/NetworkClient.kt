package com.sap.cdc.android.sdk.core.network

import android.util.Log
import com.sap.cdc.android.sdk.core.api.Api
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
import java.text.SimpleDateFormat
import java.util.Locale


/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
class NetworkClient(
) {
    companion object {
        private const val LOG_TAG = "NetworkClient"
        private const val TIME_OUT = 30_000
    }

    fun http() = HttpClient(Android) {

        engine {
            connectTimeout = TIME_OUT
            socketTimeout = TIME_OUT
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.v("Logger Ktor =>", message)
                }

            }
            level = LogLevel.ALL
        }

//        HttpResponseValidator {
//            validateResponse { response: HttpResponse ->
//                if (!response.status.isSuccess()) {
//                    val httpFailureReason = when (response.status) {
//                        HttpStatusCode.Unauthorized -> HttpStatusCode.Unauthorized.description
//                        HttpStatusCode.Forbidden -> HttpStatusCode.Forbidden.description
//                        HttpStatusCode.RequestTimeout -> HttpStatusCode.RequestTimeout.description
//                        in HttpStatusCode.InternalServerError..HttpStatusCode.GatewayTimeout -> "${response.status.value} Server Error"
//                        else -> "Network error!"
//                    }
//
//                    throw HttpExceptions(
//                        response = response,
//                        cachedResponseText = response.bodyAsText(),
//                        failureReason = httpFailureReason,
//                    )
//                }
//            }
//        }

        install(ResponseObserver) {
            onResponse { response ->
                Log.d(LOG_TAG, "HTTP Status: ${response.status.value}")
            }
        }

        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded)
        }
    }
}

class HttpExceptions(
    response: HttpResponse,
    failureReason: String?,
    cachedResponseText: String,
) : ResponseException(response, cachedResponseText) {
    override val message: String = "Status: ${response.status}." + " Failure: $failureReason"
}

