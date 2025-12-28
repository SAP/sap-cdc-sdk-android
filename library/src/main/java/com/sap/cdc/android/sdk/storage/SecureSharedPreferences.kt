package com.sap.cdc.android.sdk.storage

import android.content.SharedPreferences
import kotlinx.coroutines.runBlocking

/**
 * SharedPreferences compatibility wrapper for SecureStorage.
 * 
 * This class provides a drop-in replacement for EncryptedSharedPreferences,
 * allowing existing code to use the new SecureStorage implementation without
 * requiring extensive refactoring.
 * 
 * Note: This wrapper uses runBlocking for synchronous operations to maintain
 * compatibility with the SharedPreferences interface. For new code, prefer
 * using SecureStorage directly with proper coroutine support.
 * 
 * @param secureStorage The underlying SecureStorage implementation
 */
class SecureSharedPreferences(
    private val secureStorage: SecureStorage
) : SharedPreferences {
    
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()
    
    override fun getAll(): MutableMap<String, *> {
        return runBlocking {
            val keys = secureStorage.getAllKeys().getOrElse { emptySet() }
            val result = mutableMapOf<String, Any?>()
            
            keys.forEach { key ->
                // Try to get as string first, since all values are stored as encrypted strings
                secureStorage.getString(key, "").onSuccess { value ->
                    if (value.isNotEmpty()) {
                        result[key] = value
                    }
                }
            }
            
            result
        }
    }
    
    override fun getString(key: String?, defValue: String?): String? {
        if (key == null) return defValue
        
        return runBlocking {
            secureStorage.getString(key, defValue ?: "").getOrElse { defValue ?: "" }
        }
    }
    
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        if (key == null) return defValues
        
        return runBlocking {
            secureStorage.getStringSet(key, defValues ?: emptySet()).getOrElse { 
                defValues ?: mutableSetOf() 
            }.toMutableSet()
        }
    }
    
    override fun getInt(key: String?, defValue: Int): Int {
        if (key == null) return defValue
        
        return runBlocking {
            secureStorage.getInt(key, defValue).getOrElse { defValue }
        }
    }
    
    override fun getLong(key: String?, defValue: Long): Long {
        if (key == null) return defValue
        
        return runBlocking {
            secureStorage.getLong(key, defValue).getOrElse { defValue }
        }
    }
    
    override fun getFloat(key: String?, defValue: Float): Float {
        if (key == null) return defValue
        
        return runBlocking {
            secureStorage.getFloat(key, defValue).getOrElse { defValue }
        }
    }
    
    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        if (key == null) return defValue
        
        return runBlocking {
            secureStorage.getBoolean(key, defValue).getOrElse { defValue }
        }
    }
    
    override fun contains(key: String?): Boolean {
        if (key == null) return false
        
        return runBlocking {
            secureStorage.contains(key).getOrElse { false }
        }
    }
    
    override fun edit(): SharedPreferences.Editor {
        return SecureEditor()
    }
    
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        listener?.let { listeners.add(it) }
    }
    
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        listener?.let { listeners.remove(it) }
    }
    
    private fun notifyListeners(key: String) {
        listeners.forEach { listener ->
            listener.onSharedPreferenceChanged(this, key)
        }
    }
    
    /**
     * Editor implementation for SecureSharedPreferences.
     */
    private inner class SecureEditor : SharedPreferences.Editor {
        private val pendingChanges = mutableMapOf<String, Any?>()
        private val pendingRemovals = mutableSetOf<String>()
        private var shouldClear = false
        
        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) {
                pendingChanges[key] = value
            }
            return this
        }
        
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) {
                pendingChanges[key] = values
            }
            return this
        }
        
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) {
                pendingChanges[key] = value
            }
            return this
        }
        
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) {
                pendingChanges[key] = value
            }
            return this
        }
        
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) {
                pendingChanges[key] = value
            }
            return this
        }
        
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) {
                pendingChanges[key] = value
            }
            return this
        }
        
        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) {
                pendingRemovals.add(key)
                pendingChanges.remove(key)
            }
            return this
        }
        
        override fun clear(): SharedPreferences.Editor {
            shouldClear = true
            pendingChanges.clear()
            pendingRemovals.clear()
            return this
        }
        
        override fun commit(): Boolean {
            return runBlocking {
                try {
                    applyChanges()
                    true
                } catch (e: Exception) {
                    false
                }
            }
        }
        
        override fun apply() {
            runBlocking {
                try {
                    applyChanges()
                } catch (e: Exception) {
                    // Apply is fire-and-forget, so we don't propagate exceptions
                }
            }
        }
        
        private suspend fun applyChanges() {
            if (shouldClear) {
                secureStorage.clear().getOrThrow()
            }
            
            // Apply removals
            pendingRemovals.forEach { key ->
                secureStorage.remove(key).getOrThrow()
                notifyListeners(key)
            }
            
            // Apply changes
            pendingChanges.forEach { (key, value) ->
                when (value) {
                    is String -> secureStorage.putString(key, value).getOrThrow()
                    is Int -> secureStorage.putInt(key, value).getOrThrow()
                    is Long -> secureStorage.putLong(key, value).getOrThrow()
                    is Float -> secureStorage.putFloat(key, value).getOrThrow()
                    is Boolean -> secureStorage.putBoolean(key, value).getOrThrow()
                    is Set<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        val stringSet = value as? Set<String> ?: emptySet()
                        secureStorage.putStringSet(key, stringSet).getOrThrow()
                    }
                    null -> secureStorage.remove(key).getOrThrow()
                }
                notifyListeners(key)
            }
            
            // Clear pending changes
            pendingChanges.clear()
            pendingRemovals.clear()
            shouldClear = false
        }
    }
}
