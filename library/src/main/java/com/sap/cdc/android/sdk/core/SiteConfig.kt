package com.sap.cdc.android.sdk.core

import android.content.Context
import com.sap.cdc.android.sdk.extensions.requiredStringResourceFromKey
import com.sap.cdc.android.sdk.extensions.stringResourceFromKey

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
}
