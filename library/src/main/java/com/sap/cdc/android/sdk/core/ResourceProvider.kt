package com.sap.cdc.android.sdk.core

import android.content.SharedPreferences

/**
 * Created by Tal Mirmelshtein on 30/09/2024
 * Copyright: SAP LTD.
 *
 * Interface for providing string resources and SharedPreferences access to SDK components.
 * This abstraction allows for easier testing by enabling mock implementations
 * that don't require Android Context dependencies.
 */
interface ResourceProvider {
    /**
     * Gets a required string resource by key.
     * @param key The resource key to look up
     * @return The string value
     * @throws IllegalArgumentException if the resource is not found
     */
    fun getRequiredString(key: String): String

    /**
     * Gets an optional string resource by key.
     * @param key The resource key to look up
     * @return The string value or null if not found
     */
    fun getString(key: String): String?

    /**
     * Gets encrypted SharedPreferences instance by name.
     * @param name The preferences file name
     * @return SharedPreferences instance for the given name
     */
    fun getEncryptedSharedPreferences(name: String): SharedPreferences
}
