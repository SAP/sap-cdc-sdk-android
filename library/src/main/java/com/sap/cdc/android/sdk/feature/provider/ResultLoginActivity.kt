package com.sap.cdc.android.sdk.feature.provider

import android.content.Intent
import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.CIAMDebuggable

/**
 * Result handler activity for authentication provider flows.
 * 
 * Receives intent results from authentication providers (OAuth callbacks, etc.)
 * and forwards them back to the calling activity. Used as an intermediary
 * for handling deep links and authorization redirects.
 * 
 * @author Tal Mirmelshtein
 * @since 20/06/2024
 * 
 * Copyright: SAP LTD.
 */
class ResultLoginActivity : ComponentActivity() {

    companion object {

        const val LOG_TAG = "ResultHostActivity"
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        CIAMDebuggable.log(LOG_TAG, "onNewIntent: recieved")
        setResult(RESULT_OK, intent)
        finish()
    }
}
