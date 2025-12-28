package com.sap.cdc.android.sdk.feature.provider.web

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.R
import com.sap.cdc.android.sdk.extensions.parseQueryStringParams

/**
 * WebView-based login activity for social authentication.
 * 
 * Displays a WebView for OAuth authentication with social providers that don't
 * have native SDK support. Handles URL interception for OAuth callbacks and
 * extracts authentication tokens from the redirect URI.
 * 
 * @author Tal Mirmelshtein
 * @since 10/06/2024
 * 
 * Copyright: SAP LTD.
 * 
 * @see WebAuthenticationProvider
 */
class WebLoginActivity : ComponentActivity() {

    companion object {
        const val LOG_TAG = "WebLoginActivity"

        const val EXTRA_URI = "extra_uri"
    }

    private var progressBar: ProgressBar? = null
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Always secure activity instance.
        if (window != null) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        // Application can override this XML file and customize is according to app specifications.
        // View ids however are strict and need to remain.
        setContentView(R.layout.sap_cdc_android_webloginactivity)

        // ProgressBar widget is available to show on WebView start/finished state.
        progressBar = findViewById(R.id.sapCdcAndroidWebLoginProgressBar)

        // WebView widget is mandatory for this Activity.
        webView = findViewById(R.id.sapCdcAndroidWebLoginWebView)
        setupWebViewElement()

        // Get URI extra.
        val uri = intent.getStringExtra(EXTRA_URI)
        if (uri == null) {
            setResult(RESULT_CANCELED)
        }

        // Begin web authentication flow.
        webView.loadUrl(uri!!)
    }

    /**
     * Configures the WebView with JavaScript enabled and custom WebViewClient.
     * Handles page load progress and URL interception for OAuth callbacks.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebViewElement() {
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.loadsImagesAutomatically = true
        webView.settings.javaScriptEnabled = true
        webView.settings.useWideViewPort = false

        webView.webViewClient = object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                CDCDebuggable.log(LOG_TAG, "onPageStarted: $url")
                progressBar?.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                CDCDebuggable.log(LOG_TAG, "onPageFinished: $url")
                progressBar?.visibility = View.INVISIBLE
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                CDCDebuggable.log(LOG_TAG, "shouldOverrideUrlLoading: ${request?.url}")
                if (request == null) return true
                if (loginResult(request.url)) {
                    return false
                }
                view!!.loadUrl(request.url.toString())
                return true
            }
        }
    }

    /**
     * Checks if the current URL is the OAuth redirect callback.
     * Extracts authentication parameters from the URL fragment and returns them.
     * @param uri The URI to check
     * @return true if this is the callback URL, false otherwise
     */
    private fun loginResult(uri: Uri?): Boolean {
        if (uri == null) return false
        val scheme = uri.scheme
        val host = uri.host
        CDCDebuggable.log(LOG_TAG, "loginResult: scheme:$scheme host:$host")

        if (scheme == null || host == null) return false
        if (scheme == "gigya" && host == "gsapi") {
            val encodedFragment = uri.encodedFragment
            Log.d(LOG_TAG, "loginResult: $encodedFragment")

            // Map query string parameters.
            val queryMap = encodedFragment!!.parseQueryStringParams()

            // Create result bundle.
            val bundle = Bundle()
            queryMap.forEach() {
                bundle.putString(it.key, it.value)
            }

            Log.d(LOG_TAG, "loginResult: setResult OK")

            val resultIntent = Intent()
            resultIntent.putExtras(bundle)
            setResult(RESULT_OK, resultIntent)
            finish()
            return true
        }
        return false
    }

    /**
     * Handles back button press by canceling the authentication flow.
     */
    override fun getOnBackInvokedDispatcher(): OnBackInvokedDispatcher {
        setResult(RESULT_CANCELED)
        return super.getOnBackInvokedDispatcher()
    }

}
