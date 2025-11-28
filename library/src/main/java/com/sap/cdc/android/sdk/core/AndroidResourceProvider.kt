package com.sap.cdc.android.sdk.core

import android.content.Context
import android.content.SharedPreferences
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import com.sap.cdc.android.sdk.extensions.requiredStringResourceFromKey
import com.sap.cdc.android.sdk.extensions.stringResourceFromKey

/**
 * Android implementation of ResourceProvider.
 * 
 * Uses Android Context to access string resources and encrypted SharedPreferences,
 * providing secure storage and resource loading capabilities for the SDK.
 * 
 * @property context Android application context for accessing resources
 * 
 * @author Tal Mirmelshtein
 * @since 30/09/2024
 * 
 * Copyright: SAP LTD.
 * 
 * @see ResourceProvider
 */
class AndroidResourceProvider(private val context: Context) : ResourceProvider {

    override fun getRequiredString(key: String): String {
        return context.requiredStringResourceFromKey(key)
    }

    override fun getString(key: String): String? {
        return context.stringResourceFromKey(key)
    }

    override fun getEncryptedSharedPreferences(name: String): SharedPreferences {
        return context.getEncryptedPreferences(name)
    }
}
