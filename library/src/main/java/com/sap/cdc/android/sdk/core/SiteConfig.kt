package com.sap.cdc.android.sdk.core

import android.content.Context
import android.util.Log
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
    var cname: String? = null
) {
    // Failure to retrieve apiKey, domain will issue an IllegalArgumentException.
    constructor(context: Context) : this(
        context,
        context.requiredStringResourceFromKey("com.sap.cxcdc.apikey"),
        context.requiredStringResourceFromKey("com.sap.cxcdc.domain"),
        context.stringResourceFromKey("com.sap.cxcdc.cname"),
    )

    private var serverOffset: Long = 0

    companion object {
        const val CDC_SERVER_OFFSET_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz"
    }

    fun getServerTimestamp(): String {
        val timestamp: String =
            ((System.currentTimeMillis() / 1000) + serverOffset).toString()
        Log.d(Api.LOG_TAG, "serverOffset - get: $timestamp")
        return timestamp
    }

    fun setServerOffset(date: String?) {
        if (date == null) return
        val format = SimpleDateFormat(
            CDC_SERVER_OFFSET_FORMAT,
            Locale.ENGLISH
        )
        val serverDate = format.parse(date) ?: return
        serverOffset = (serverDate.time - System.currentTimeMillis()) / 1000
        Log.d(Api.LOG_TAG, "serverOffset - set: $serverOffset")
    }
}
