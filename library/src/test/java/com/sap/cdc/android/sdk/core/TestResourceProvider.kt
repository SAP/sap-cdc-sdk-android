package com.sap.cdc.android.sdk.core

import android.content.SharedPreferences

/**
 * Test implementation of ResourceProvider for unit testing.
 * Allows setting predefined string values and provides in-memory SharedPreferences
 * without requiring Android Context.
 */
class TestResourceProvider : ResourceProvider {

    private val stringResources = mutableMapOf<String, String>()
    private val sharedPreferencesMap = mutableMapOf<String, InMemorySharedPreferences>()

    /**
     * Sets a string resource value for testing.
     * @param key The resource key
     * @param value The string value to return for this key
     */
    fun setString(key: String, value: String) {
        stringResources[key] = value
    }

    /**
     * Sets up SharedPreferences to return specific string values.
     * @param name The preferences file name
     * @param key The preference key
     * @param value The value to return
     */
    fun putString(name: String, key: String, value: String?) {
        val prefs = getOrCreateInMemorySharedPreferences(name)
        if (value != null) {
            prefs.putString(key, value)
        } else {
            prefs.remove(key)
        }
    }

    /**
     * Sets up SharedPreferences to return specific long values.
     * @param name The preferences file name
     * @param key The preference key
     * @param value The value to return
     */
    fun putLong(name: String, key: String, value: Long) {
        val prefs = getOrCreateInMemorySharedPreferences(name)
        prefs.putLong(key, value)
    }

    /**
     * Gets or creates an in-memory SharedPreferences instance for the given name.
     * @param name The preferences file name
     * @return InMemorySharedPreferences instance
     */
    private fun getOrCreateInMemorySharedPreferences(name: String): InMemorySharedPreferences {
        return sharedPreferencesMap.getOrPut(name) { InMemorySharedPreferences() }
    }

    /**
     * Clears all stored string resources and SharedPreferences.
     */
    fun clear() {
        stringResources.clear()
        sharedPreferencesMap.clear()
    }

    override fun getRequiredString(key: String): String {
        return stringResources[key] ?: throw IllegalArgumentException("Required string resource not found: $key")
    }

    override fun getString(key: String): String? {
        return stringResources[key]
    }

    override fun getEncryptedSharedPreferences(name: String): SharedPreferences {
        return getOrCreateInMemorySharedPreferences(name)
    }

    /**
     * In-memory implementation of SharedPreferences for testing.
     */
    private class InMemorySharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any?>()

        fun putString(key: String, value: String?) {
            data[key] = value
        }

        fun putLong(key: String, value: Long) {
            data[key] = value
        }

        fun remove(key: String) {
            data.remove(key)
        }

        override fun getAll(): MutableMap<String, *> = data.toMutableMap()

        override fun getString(key: String?, defValue: String?): String? {
            return data[key] as? String ?: defValue
        }

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            return data[key] as? MutableSet<String> ?: defValues
        }

        override fun getInt(key: String?, defValue: Int): Int {
            return data[key] as? Int ?: defValue
        }

        override fun getLong(key: String?, defValue: Long): Long {
            return data[key] as? Long ?: defValue
        }

        override fun getFloat(key: String?, defValue: Float): Float {
            return data[key] as? Float ?: defValue
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return data[key] as? Boolean ?: defValue
        }

        override fun contains(key: String?): Boolean = data.containsKey(key)

        override fun edit(): SharedPreferences.Editor = InMemoryEditor()

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            // No-op for testing
        }

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            // No-op for testing
        }

        private inner class InMemoryEditor : SharedPreferences.Editor {
            private val edits = mutableMapOf<String, Any?>()

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                key?.let { edits[it] = value }
                return this
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                key?.let { edits[it] = values }
                return this
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                key?.let { edits[it] = value }
                return this
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                key?.let { edits[it] = value }
                return this
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                key?.let { edits[it] = value }
                return this
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                key?.let { edits[it] = value }
                return this
            }

            override fun remove(key: String?): SharedPreferences.Editor {
                key?.let { edits[it] = null }
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                data.clear()
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                edits.forEach { (key, value) ->
                    if (value == null) {
                        data.remove(key)
                    } else {
                        data[key] = value
                    }
                }
                edits.clear()
            }
        }
    }
}
