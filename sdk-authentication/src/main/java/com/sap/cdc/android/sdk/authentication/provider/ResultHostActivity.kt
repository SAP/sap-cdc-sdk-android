package com.sap.cdc.android.sdk.authentication.provider

import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity


/**
 * Created by Tal Mirmelshtein on 20/06/2024
 * Copyright: SAP LTD.
 */
class ResultHostActivity : ComponentActivity() {

    companion object {

        const val LOG_TAG = "CDC_ResultHostActivity"
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(LOG_TAG, "onNewIntent: recieved")
        setResult(RESULT_OK, intent)
        finish()
    }
}