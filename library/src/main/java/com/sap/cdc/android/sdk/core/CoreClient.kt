package com.sap.cdc.android.sdk.core

import com.sap.cdc.android.sdk.core.network.NetworkClient


/**
 * Created by Tal Mirmelshtein on 18/06/2024
 * Copyright: SAP LTD.
 */
class CoreClient(val siteConfig: SiteConfig) {

    companion object {

        const val CDC_CODE_CLIENT_SECURED_PREF = "cdc_secure_prefs_core_client"
        const val CDC_SERVER_OFFSET = "cdc_server_offset"
    }

    val networkClient = NetworkClient()
}