package com.sap.cdc.android.sdk.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.sap.cdc.android.sdk.CDCDebuggable

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
fun Context.getEncryptedPreferences(fileName: String): SharedPreferences {
    return EncryptedSharedPreferences.create(
        fileName,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        this,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
fun Context.isOnline(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    if (capabilities != null) {
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            CDCDebuggable.log("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
            return true
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            CDCDebuggable.log("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
            return true
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            CDCDebuggable.log("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
            return true
        }
    }
    return false
}

@SuppressLint("DiscouragedApi") // Not possible to access host R file.
fun Context.requiredStringResourceFromKey(key: String): String {
    val resource = resources.getIdentifier(key, "string", packageName)
    require(resource != 0) {
        String.format(
            "Provided resource key:$key is not defined in your resource files"
        )
    }
    return getString(resource)
}

@SuppressLint("DiscouragedApi") // Not possible to access host R file.
fun Context.stringResourceFromKey(key: String): String? {
    val resource = resources.getIdentifier(key, "string", packageName)
    if (resource == 0) return null
    return getString(resource)
}