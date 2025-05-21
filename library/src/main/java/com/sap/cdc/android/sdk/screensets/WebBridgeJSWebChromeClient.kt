package com.sap.cdc.android.sdk.screensets

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView


/**
 * Created by Tal Mirmelshtein on 13/06/2024
 * Copyright: SAP LTD.
 */
class WebBridgeJSWebChromeClient(
    private val launchFileChooser: (Intent) -> Unit
) : WebChromeClient() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        // Save the callback
        this.filePathCallback = filePathCallback

        // Launch the file chooser intent
        val intent = createFileChooserIntent()
        launchFileChooser(intent)
        return true
    }

    fun handleActivityResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            // Pass the result to the callback
            filePathCallback?.onReceiveValue(arrayOf(data.data!!))
        } else {
            // Notify the callback of a cancellation
            filePathCallback?.onReceiveValue(null)
        }
        // Clear the callback to reset the state
        filePathCallback = null
    }

    private fun createFileChooserIntent(): Intent {
        val capture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val select = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        return Intent(Intent.ACTION_CHOOSER).apply {
            putExtra(Intent.EXTRA_INTENT, select)
            putExtra(Intent.EXTRA_TITLE, "Image Chooser")
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(capture))
        }
    }
}