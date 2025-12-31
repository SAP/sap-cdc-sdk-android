package com.sap.cdc.android.sdk.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.sap.cdc.android.sdk.CIAMDebuggable
import com.sap.cdc.android.sdk.storage.AndroidKeystoreSecureStorage
import com.sap.cdc.android.sdk.storage.SecureSharedPreferences

/**
 * Context extension functions for SAP CIAM SDK.
 * 
 * Provides utility functions for secure storage, network connectivity checks,
 * and resource loading. Updated to use modern Android Keystore-based encryption.
 * 
 * @author Tal Mirmelshtein
 * @since 10/06/2024
 * 
 * Copyright: SAP LTD.
 * 
 * @see AndroidKeystoreSecureStorage
 * @see SecureSharedPreferences
 */

/**
 * Gets secure encrypted SharedPreferences using Android Keystore.
 * 
 * This method now uses AndroidKeystoreSecureStorage with AES-256-GCM encryption
 * instead of the deprecated EncryptedSharedPreferences. The new implementation
 * provides:
 * - Hardware-backed encryption when available (TEE/Secure Element)
 * - Better error handling with Result-based API
 * - Modern cryptographic practices
 * - Backward compatibility through SharedPreferences interface
 * 
 * @param fileName The name of the preferences file
 * @return SharedPreferences instance with secure storage backend
 */
fun Context.getEncryptedPreferences(fileName: String): SharedPreferences {
    val secureStorage = AndroidKeystoreSecureStorage(
        context = this,
        preferencesName = fileName,
        keyAlias = "${fileName}_key"
    )
    return SecureSharedPreferences(secureStorage)
}

/**
 * Gets the underlying SecureStorage implementation for advanced usage.
 * 
 * This method provides direct access to the SecureStorage interface for
 * applications that want to use the modern Result-based API with proper
 * coroutine support instead of the synchronous SharedPreferences interface.
 * 
 * @param fileName The name of the preferences file
 * @param keyAlias Optional custom key alias (defaults to "${fileName}_key")
 * @return AndroidKeystoreSecureStorage instance
 */
fun Context.getSecureStorage(
    fileName: String, 
    keyAlias: String = "${fileName}_key"
): AndroidKeystoreSecureStorage {
    return AndroidKeystoreSecureStorage(
        context = this,
        preferencesName = fileName,
        keyAlias = keyAlias
    )
}

/**
 * Checks if the device has an active network connection.
 * Checks for cellular, WiFi, or ethernet connectivity.
 * @return true if network is available, false otherwise
 */
fun Context.isOnline(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    if (capabilities != null) {
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            CIAMDebuggable.log("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
            return true
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            CIAMDebuggable.log("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
            return true
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            CIAMDebuggable.log("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
            return true
        }
    }
    return false
}

/**
 * Gets a required string resource by its key name.
 * Throws an exception if the resource is not found.
 * @param key The resource key name (e.g., "sap_cdc_api_key")
 * @return The string resource value
 * @throws IllegalArgumentException if resource not found
 */
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

/**
 * Gets an optional string resource by its key name.
 * Returns null if the resource is not found.
 * @param key The resource key name
 * @return The string resource value or null if not found
 */
@SuppressLint("DiscouragedApi") // Not possible to access host R file.
fun Context.stringResourceFromKey(key: String): String? {
    val resource = resources.getIdentifier(key, "string", packageName)
    if (resource == 0) return null
    return getString(resource)
}
