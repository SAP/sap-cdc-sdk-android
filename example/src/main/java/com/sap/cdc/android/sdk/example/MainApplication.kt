package com.sap.cdc.android.sdk.example

import android.app.Application
import android.webkit.WebView

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Allow WebViews debugging.
        WebView.setWebContentsDebuggingEnabled(true)
    }
}