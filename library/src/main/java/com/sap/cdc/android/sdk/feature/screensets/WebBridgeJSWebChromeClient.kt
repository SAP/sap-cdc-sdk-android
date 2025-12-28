package com.sap.cdc.android.sdk.feature.screensets

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
 *
 * Custom WebChromeClient for handling file chooser events in as ScreenSets dedicated WebView.
 */
class WebBridgeJSWebChromeClient(
    private val launchFileChooser: (Intent) -> Unit
) : WebChromeClient() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    /**
     * Override required methods to handle file chooser events.
     * ScreenSet that uses account update information may contain file upload elements such as
     * profile picture upload.
     */
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

    /**
     * Handle the result of the file chooser.
     * This method should be called from the activity that launched the file chooser.
     */
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

    /**
     * Create an intent for the file chooser.
     * This intent will allow the user to select an image from the gallery or take a new photo.
     */
    private fun createFileChooserIntent(): Intent {
        val capture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val select = Intent(Intent.ACTION_GET_CONTENT).apply {
            setType("image/*")
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        return Intent(Intent.ACTION_CHOOSER).apply {
            putExtra(Intent.EXTRA_INTENT, select)
            putExtra(Intent.EXTRA_TITLE, "Image Chooser")
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(capture))
        }
    }
}