package com.sap.cdc.android.sdk.sceensets

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by Tal Mirmelshtein on 15/06/2024
 * Copyright: SAP LTD.
 */
class ScreenSetFragment : Fragment() {

    companion object {

        const val LOG_TAG = "CDC_ScreenSetFragment"

        const val ARG_HTML_URL = "html_url"
    }

    private var progressBar: ProgressBar? = null
    private lateinit var webView: WebView

    private var screenSetUrl: String? = null
    private lateinit var webViewWebChromeClient: WebBridgeJSWebChromeClient
    private lateinit var webViewClient: WebBridgeJSWebViewClient

    var webBridgeJS: WebBridgeJS? = null
    val events: (WebBridgeJSEvent?) -> Unit = { }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        screenSetUrl = arguments?.getString(ARG_HTML_URL)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Application can override this XML file and customize is according to app specifications.
        // View ids however are strict and need to remain.
        return inflater.inflate(R.layout.sap_cdc_android_screensetactivity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // ProgressBar widget is available to show on WebView start/finished state.
        progressBar = view.findViewById(R.id.sapCdcScreenSetWebView)
        // WebView widget is mandatory for this Activity.
        webView = view.findViewById(R.id.sapCdcScreenSetProgressBar)

        setupWebViewElement()
    }

    @SuppressLint("SetJavaScriptEnabled")
    /**
     * Setting up main WebView widget.
     */
    private fun setupWebViewElement() {
        webView.settings.javaScriptEnabled = true
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.loadsImagesAutomatically = true
        webView.settings.javaScriptEnabled = true
        webView.settings.useWideViewPort = false

        // Add only hen file access is required.
        webView.settings.allowFileAccess = true
        webViewWebChromeClient = WebBridgeJSWebChromeClient()
        webView.webChromeClient = webViewWebChromeClient

        webViewClient = WebBridgeJSWebViewClient(webBridgeJS!!) { browserUri ->
            //TODO: Check for legacy action. is it required??
            val intent = Intent(Intent.ACTION_VIEW, browserUri)
            context?.startActivity(intent)
        }
        webView.webViewClient = webViewClient

        CoroutineScope(Dispatchers.IO).launch {
            webBridgeJS
                ?.attachBridgeTo(webView) { webBridgeJSEvent ->
                    // Streamed WebBridgeJS event.
                    Log.d(LOG_TAG, webBridgeJSEvent.name() ?: "")
                    events.invoke(webBridgeJSEvent)
                }
        }

        webView.loadDataWithBaseURL(
            WebBridgeJS.BASE_URL,
            screenSetUrl ?: "",
            WebBridgeJS.MIME_TYPE,
            WebBridgeJS.ENCODING,
            null
        );
    }

    override fun onDestroyView() {
        webBridgeJS?.detachBridgeFrom(webView)
        super.onDestroyView()
    }
}