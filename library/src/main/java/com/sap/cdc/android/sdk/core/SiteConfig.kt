package com.sap.cdc.android.sdk.core

import android.content.Context
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.api.Api
import com.sap.cdc.android.sdk.extensions.requiredStringResourceFromKey
import com.sap.cdc.android.sdk.extensions.stringResourceFromKey
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
class SiteConfig(
    val applicationContext: Context,
    val apiKey: String,
    val domain: String,
    var cname: String? = null,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    // Failure to retrieve apiKey, domain will issue an IllegalArgumentException.
    constructor(context: Context) : this(
        context,
        context.requiredStringResourceFromKey("com.sap.cxcdc.apikey"),
        context.requiredStringResourceFromKey("com.sap.cxcdc.domain"),
        context.stringResourceFromKey("com.sap.cxcdc.cname"),
    )

    /**
     * Constructor that accepts a ResourceProvider for better testability.
     * This allows injecting mock resource providers during testing.
     */
    constructor(
        context: Context, 
        resourceProvider: ResourceProvider
    ) : this(
        context,
        resourceProvider.getRequiredString("com.sap.cxcdc.apikey"),
        resourceProvider.getRequiredString("com.sap.cxcdc.domain"),
        resourceProvider.getString("com.sap.cxcdc.cname")
    )

    private var serverOffset: Long = 0

    companion object {
        const val CDC_SERVER_OFFSET_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz"
    }

    fun getServerTimestamp(): String {
        val timestamp: String =
            ((timeProvider() / 1000) + serverOffset).toString()
        CDCDebuggable.log(Api.LOG_TAG, "serverOffset - get: $timestamp")
        return timestamp
    }

    fun setServerOffset(date: String?) {
        if (date == null) return
        val format = SimpleDateFormat(
            CDC_SERVER_OFFSET_FORMAT,
            Locale.ENGLISH
        )
        try {
            val serverDate = format.parse(date) ?: return
            serverOffset = (serverDate.time - timeProvider()) / 1000
            CDCDebuggable.log(Api.LOG_TAG, "serverOffset - set: $serverOffset")
        } catch (e: java.text.ParseException) {
            // Invalid date format - ignore and keep current offset
            CDCDebuggable.log(Api.LOG_TAG, "Invalid date format, keeping current serverOffset: $serverOffset")
        }
    }
}
