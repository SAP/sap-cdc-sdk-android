package com.sap.cdc.bitsnbytes

import android.app.Application
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import com.sap.cdc.android.sdk.CDCDebuggable
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Allow WebView debugging.
        CDCDebuggable.setLogs(true)
        CDCDebuggable.setWebViewDebuggable(true)

        // Print the key hash for the app. Used for FIDO configuration.
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures!!) {
                val md: MessageDigest = MessageDigest.getInstance("SHA256")
                md.update(signature.toByteArray())
                val hash = Base64.encodeToString(md.digest(),
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                Log.i("MY KEY HASH:",hash)
            }
        } catch (e: PackageManager.NameNotFoundException) {
        } catch (e: NoSuchAlgorithmException) {
        }
    }
}