package com.sap.cdc.android.sdk.core

import com.sap.cdc.android.sdk.core.network.HttpClientProvider
import com.sap.cdc.android.sdk.core.network.KtorHttpClientProvider
import com.sap.cdc.android.sdk.core.network.NetworkClient


/**
 * Created by Tal Mirmelshtein on 18/06/2024
 * Copyright: SAP LTD.
 */
class CoreClient(
    val siteConfig: SiteConfig,
    private val httpClientProvider: HttpClientProvider = KtorHttpClientProvider()
) {
    val networkClient: NetworkClient = NetworkClient(httpClientProvider)
}
