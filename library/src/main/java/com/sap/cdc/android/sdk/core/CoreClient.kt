package com.sap.cdc.android.sdk.core

import com.sap.cdc.android.sdk.core.network.RequestQueue
import com.sap.cdc.android.sdk.core.network.NetworkClient


/**
 * Created by Tal Mirmelshtein on 18/06/2024
 * Copyright: SAP LTD.
 */
class CoreClient(
    val siteConfig: SiteConfig,
) {
    val networkClient: NetworkClient = NetworkClient()

    init {
        // Initialize the com.sap.cdc.android.sdk.core.network.RequestQueue with the network client
        RequestQueue.initialize(networkClient.http())
    }
}