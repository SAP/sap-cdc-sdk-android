package com.sap.cdc.android.sdk.storage

/**
 * Secure storage interface for encrypted key-value storage operations.
 * 
 * This interface provides a modern, Result-based API for secure storage operations,
 * replacing the deprecated EncryptedSharedPreferences implementation.
 * 
 * All operations return Result<T> to enable proper error handling without exceptions.
 */
interface SecureStorage {
    
    /**
     * Stores a string value securely with the given key.
     * 
     * @param key The key to store the value under
     * @param value The string value to store
     * @return Result<Unit> indicating success or failure
     */
    suspend fun putString(key: String, value: String): Result<Unit>
    
    /**
     * Retrieves a string value for the given key.
     * 
     * @param key The key to retrieve the value for
     * @param defaultValue The default value to return if key doesn't exist
     * @return Result<String> containing the value or error
     */
    suspend fun getString(key: String, defaultValue: String = ""): Result<String>
    
    /**
     * Stores an integer value securely with the given key.
     * 
     * @param key The key to store the value under
     * @param value The integer value to store
     * @return Result<Unit> indicating success or failure
     */
    suspend fun putInt(key: String, value: Int): Result<Unit>
    
    /**
     * Retrieves an integer value for the given key.
     * 
     * @param key The key to retrieve the value for
     * @param defaultValue The default value to return if key doesn't exist
     * @return Result<Int> containing the value or error
     */
    suspend fun getInt(key: String, defaultValue: Int = 0): Result<Int>
    
    /**
     * Stores a long value securely with the given key.
     * 
     * @param key The key to store the value under
     * @param value The long value to store
     * @return Result<Unit> indicating success or failure
     */
    suspend fun putLong(key: String, value: Long): Result<Unit>
    
    /**
     * Retrieves a long value for the given key.
     * 
     * @param key The key to retrieve the value for
     * @param defaultValue The default value to return if key doesn't exist
     * @return Result<Long> containing the value or error
     */
    suspend fun getLong(key: String, defaultValue: Long = 0L): Result<Long>
    
    /**
     * Stores a boolean value securely with the given key.
     * 
     * @param key The key to store the value under
     * @param value The boolean value to store
     * @return Result<Unit> indicating success or failure
     */
    suspend fun putBoolean(key: String, value: Boolean): Result<Unit>
    
    /**
     * Retrieves a boolean value for the given key.
     * 
     * @param key The key to retrieve the value for
     * @param defaultValue The default value to return if key doesn't exist
     * @return Result<Boolean> containing the value or error
     */
    suspend fun getBoolean(key: String, defaultValue: Boolean = false): Result<Boolean>
    
    /**
     * Stores a float value securely with the given key.
     * 
     * @param key The key to store the value under
     * @param value The float value to store
     * @return Result<Unit> indicating success or failure
     */
    suspend fun putFloat(key: String, value: Float): Result<Unit>
    
    /**
     * Retrieves a float value for the given key.
     * 
     * @param key The key to retrieve the value for
     * @param defaultValue The default value to return if key doesn't exist
     * @return Result<Float> containing the value or error
     */
    suspend fun getFloat(key: String, defaultValue: Float = 0f): Result<Float>
    
    /**
     * Stores a set of strings securely with the given key.
     * 
     * @param key The key to store the value under
     * @param values The set of strings to store
     * @return Result<Unit> indicating success or failure
     */
    suspend fun putStringSet(key: String, values: Set<String>): Result<Unit>
    
    /**
     * Retrieves a set of strings for the given key.
     * 
     * @param key The key to retrieve the value for
     * @param defaultValue The default value to return if key doesn't exist
     * @return Result<Set<String>> containing the value or error
     */
    suspend fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Result<Set<String>>
    
    /**
     * Checks if a key exists in the secure storage.
     * 
     * @param key The key to check for existence
     * @return Result<Boolean> indicating if the key exists or error
     */
    suspend fun contains(key: String): Result<Boolean>
    
    /**
     * Removes a key-value pair from the secure storage.
     * 
     * @param key The key to remove
     * @return Result<Unit> indicating success or failure
     */
    suspend fun remove(key: String): Result<Unit>
    
    /**
     * Removes all key-value pairs from the secure storage.
     * 
     * @return Result<Unit> indicating success or failure
     */
    suspend fun clear(): Result<Unit>
    
    /**
     * Gets all keys currently stored in the secure storage.
     * 
     * @return Result<Set<String>> containing all keys or error
     */
    suspend fun getAllKeys(): Result<Set<String>>
}
