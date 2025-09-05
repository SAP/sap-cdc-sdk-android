package com.sap.cdc.android.sdk.feature.provider.sso

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.feature.provider.web.WebLoginActivity

class SSOLoginActivity : ComponentActivity() {

    companion object {

        const val LOG_TAG = "SSOLoginActivity"

        const val EXTRA_URI = "extra_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get URI extra.
        val uri = intent.getStringExtra(WebLoginActivity.Companion.EXTRA_URI)
        if (uri == null) {
            setResult(RESULT_CANCELED)
        }

        val customTabsIntent: CustomTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(this, Uri.parse(uri))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        CDCDebuggable.log(LOG_TAG, "onNewIntent: recieved")
        setResult(RESULT_OK, intent)
        finish()
    }

    /**
     * Cancel flow on back press.
     */
    override fun getOnBackInvokedDispatcher(): OnBackInvokedDispatcher {
        setResult(RESULT_CANCELED)
        return super.getOnBackInvokedDispatcher()
    }
}