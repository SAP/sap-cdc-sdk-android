package com.sap.cdc.android.sdk.session.api

import android.content.Context
import com.sap.cdc.android.sdk.session.SessionService
import com.sap.cdc.android.sdk.session.api.model.GMIDEntity
import com.sap.cdc.android.sdk.session.extensions.getEncryptedPreferences
import com.sap.cdc.android.sdk.session.extensions.isOnline
import com.sap.cdc.android.sdk.session.extensions.prepareApiUrl
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
open class Api(
    private val sessionService: SessionService
) {

    companion object {
        const val CDC_SERVER_OFFSET = "cdc_server_offset"
        const val CDC_SERVER_OFFSET_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz"

        const val EP_GET_IDS = "socialize.getIDs"

        fun getServerTimestamp(context: Context): String {
            val esp =
                context.getEncryptedPreferences(SessionService.CDC_SECURE_PREFS)
            val timestamp: String =
                ((System.currentTimeMillis() / 1000) + esp.getLong(CDC_SERVER_OFFSET, 0)).toString()
            return timestamp
        }
    }

    /**
     * Check network connectivity.
     */
    private fun networkAvailable(): Boolean =
        sessionService.siteConfig.applicationContext.isOnline()

    /**
     * Perform generic get request.
     */
    private suspend fun get(request: Request): CDCResponse {
        if (!networkAvailable()) {
            // Propagate network error.
            return CDCResponse().noNetwork()
        }
        if (!sessionService.gmidValid()) {
            // getIDS first.
            getIds()
        }
        // Add authentication parameters if needed.
        injectOnAuthenticated(request)
        val result: HttpResponse = sessionService.networkClient.http()
            .get(request.api) {
                headers {
                    request.headers.map { (k, v) ->
                        headers.append(k, v)
                    }
                }
                url {
                    request.parameters.map { (k, v) ->
                        parameters.append(k, v)
                    }

                }
            }
        val serverDate: String? = result.headers["date"]
        // Set server offset.
        setServerOffset(serverDate)
        // Forward response.
        return CDCResponse().fromJSON(result.body())
    }

    /**
     * Perform generic post request.
     */
    private suspend fun post(
        request: Request
    ): CDCResponse {
        if (!networkAvailable()) {
            // Propagate network error.
            return CDCResponse().noNetwork()
        }
        if (!sessionService.gmidValid()) {
            // getIDS first.
            getIds()
        }
        // Add authentication parameters if needed.
        injectOnAuthenticated(request)
        val result: HttpResponse = sessionService.networkClient.http().post(request.api) {
            headers {
                request.headers.map { (k, v) ->
                    headers.append(k, v)
                }
            }
            setBody(request.parameters.toEncodedQuery())
        }
        val serverDate: String? = result.headers["date"]
        // Set server offset.
        setServerOffset(serverDate)
        // Forward response.
        return CDCResponse().fromJSON(result.body())
    }

    /**
     * Set server offset parameter to ensure correct time alignment.
     */
    private fun setServerOffset(date: String?) {
        if (date == null) return
        val format = SimpleDateFormat(
            CDC_SERVER_OFFSET_FORMAT,
            Locale.ENGLISH
        )
        val serverDate = format.parse(date) ?: return
        val offset = (serverDate.time - System.currentTimeMillis()) / 1000
        val esp =
            sessionService.siteConfig.applicationContext.getEncryptedPreferences(SessionService.CDC_SECURE_PREFS)
        esp.edit().putLong(CDC_SERVER_OFFSET, offset).apply()
    }

    /**
     * Add authentication parameters when session data is available.
     */
    private fun injectOnAuthenticated(request: Request) {
        val session = sessionService.sessionSecure.getSession()
        if (session != null) {
            request.parameters["oauth_token"] = session.token
            request.parameters["timestamp"] =
                getServerTimestamp(sessionService.siteConfig.applicationContext)
            request.sign(session)
        }
    }

    /**
     * Generic send request function or REST operation.
     */
    @JvmOverloads
    open suspend fun genericSend(
        api: String,
        parameters: MutableMap<String, String> = mutableMapOf(),
        method: String? = HttpMethod.Post.value,
        headers: MutableMap<String, String>? = mutableMapOf()
    ): CDCResponse {
        if (method?.equals(HttpMethod.Get.value) == true) {
            return get(
                Request(sessionService.siteConfig)
                    .method(HttpMethod.Get.value)
                    .api(api.prepareApiUrl(sessionService.siteConfig))
                    .parameters(parameters)
                    .headers(headers)
            )
        }
        return post(
            Request(sessionService.siteConfig)
                .method(HttpMethod.Get.value)
                .api(api.prepareApiUrl(sessionService.siteConfig))
                .parameters(parameters)
                .headers(headers)
        )
    }

    /**
     * GMD field is crucial for SDK interactions. However initializing it will now be the host
     * responsibility. Initialization will fetch the GMID.
     */
    suspend fun getIds() {
        val esp =
            sessionService.siteConfig.applicationContext.getEncryptedPreferences(SessionService.CDC_SECURE_PREFS)
        val request = Request(sessionService.siteConfig).method(HttpMethod.Get.value).api(
            EP_GET_IDS.prepareApiUrl(
                sessionService.siteConfig
            )
        )
        val result: HttpResponse = sessionService.networkClient.http().post(request.api) {
            setBody(request.parameters.toEncodedQuery())
        }
        val serializable =
            CDCResponse().fromJSON(result.body()).serializeTo<GMIDEntity>()
        if (serializable != null) {
            // Update GMID & GMID refresh timestamp.
            esp.edit().putString(SessionService.CDC_GMID, serializable.gmid)
                .putLong(SessionService.CDC_GMID_REFRESH_TS, serializable.refreshTime!!)
                .apply()
        }
    }
}

