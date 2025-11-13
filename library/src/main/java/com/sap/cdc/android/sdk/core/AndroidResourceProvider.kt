package com.sap.cdc.android.sdk.core

import android.content.Context
import android.content.SharedPreferences
import com.sap.cdc.android.sdk.extensions.getEncryptedPreferences
import com.sap.cdc.android.sdk.extensions.requiredStringResourceFromKey
import com.sap.cdc.android.sdk.extensions.stringResourceFromKey

/**
 * Created by Tal Mirmelshtein on 30/09/2024
 * Copyright: SAP LTD.
 *
 * Android implementation of ResourceProvider that uses Android Context
 * to access string resources and SharedPreferences from the app.
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
