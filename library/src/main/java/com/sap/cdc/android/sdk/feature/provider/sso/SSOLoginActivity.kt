package com.sap.cdc.android.sdk.feature.provider.sso

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    private var customTabsLaunched = false
    private var resultReceived = false
    private val handler = Handler(Looper.getMainLooper())
    private var checkDismissalRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get URI extra.
        val uri = intent.getStringExtra(WebLoginActivity.Companion.EXTRA_URI)
        if (uri == null) {
            CDCDebuggable.log(LOG_TAG, "onCreate: URI is null, canceling")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        CDCDebuggable.log(LOG_TAG, "onCreate: Launching Custom Tabs with URI: $uri")
        val customTabsIntent: CustomTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(this, Uri.parse(uri))
        customTabsLaunched = true
    }

    override fun onResume() {
        super.onResume()
        CDCDebuggable.log(LOG_TAG, "onResume: customTabsLaunched=$customTabsLaunched, resultReceived=$resultReceived")
        
        if (customTabsLaunched && !resultReceived) {
            // Custom Tabs was launched but we haven't received a result yet
            // Schedule a check to see if Custom Tabs was dismissed
            checkDismissalRunnable = Runnable {
                if (!resultReceived && !isFinishing) {
                    CDCDebuggable.log(LOG_TAG, "Custom Tabs appears to have been dismissed by user")
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
            // Small delay to allow onNewIntent to be called if user is returning with a result
            handler.postDelayed(checkDismissalRunnable!!, 100)
        }
    }

    override fun onPause() {
        super.onPause()
        // Cancel the dismissal check if we're pausing (e.g., Custom Tabs is opening)
        checkDismissalRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        CDCDebuggable.log(LOG_TAG, "onNewIntent: received result")
        resultReceived = true
        
        // Cancel any pending dismissal check
        checkDismissalRunnable?.let { handler.removeCallbacks(it) }
        
        setResult(RESULT_OK, intent)
        finish()
    }

    /**
     * Cancel flow on back press.
     */
    override fun getOnBackInvokedDispatcher(): OnBackInvokedDispatcher {
        CDCDebuggable.log(LOG_TAG, "getOnBackInvokedDispatcher: User pressed back button")
        setResult(RESULT_CANCELED)
        return super.getOnBackInvokedDispatcher()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up handler callbacks
        checkDismissalRunnable?.let { handler.removeCallbacks(it) }
    }
}
