package com.sap.cdc.bitsnbytes

import android.app.Application
import com.sap.cdc.android.sdk.CDCDebuggable

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Allow WebView debugging.
        CDCDebuggable.setLogs(true)
        CDCDebuggable.setWebViewDebuggable(true)
    }
}