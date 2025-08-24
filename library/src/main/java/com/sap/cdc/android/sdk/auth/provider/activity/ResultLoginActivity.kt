package com.sap.cdc.android.sdk.auth.provider.activity

import android.content.Intent
import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.CDCDebuggable


/**
 * Created by Tal Mirmelshtein on 20/06/2024
 * Copyright: SAP LTD.
 */
class ResultLoginActivity : ComponentActivity() {

    companion object {

        const val LOG_TAG = "ResultHostActivity"
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        CDCDebuggable.log(LOG_TAG, "onNewIntent: recieved")
        setResult(RESULT_OK, intent)
        finish()
    }
}
