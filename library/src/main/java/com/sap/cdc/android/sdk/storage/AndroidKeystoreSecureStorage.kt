package com.sap.cdc.android.sdk.storage

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore-based secure storage implementation using AES-256-GCM encryption.
 * 
 * This implementation provides hardware-backed encryption when available (TEE/Secure Element)
 * and falls back to software-based encryption on older devices.
 * 
 * Features:
 * - AES-256-GCM authenticated encryption
 * - Hardware-backed key storage when available
 * - Automatic key generation and management
 * - Result-based error handling
 * - Thread-safe operations using coroutines
 * 
 * @param context Application context for SharedPreferences access
 * @param preferencesName Name of the SharedPreferences file (default: "secure_storage")
 * @param keyAlias Alias for the encryption key in Android Keystore (default: "secure_storage_key")
 */
class AndroidKeystoreSecureStorage(
    private val context: Context,
    private val preferencesName: String = "secure_storage",
    private val keyAlias: String = "secure_storage_key"
) : SecureStorage {
    
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
    }
    
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }
    
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }
    
    /**
     * Gets or creates the encryption key from Android Keystore.
     */
    private suspend fun getOrCreateSecretKey(): Result<SecretKey> = withContext(Dispatchers.IO) {
        try {
            // Check if key already exists
            if (keyStore.containsAlias(keyAlias)) {
                val existingKey = keyStore.getKey(keyAlias, null) as? SecretKey
                if (existingKey != null) {
                    return@withContext Result.success(existingKey)
                }
            }
            
            // Generate new key
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            val secretKey = keyGenerator.generateKey()
            
            Result.success(secretKey)
        } catch (e: Exception) {
            Result.failure(SecureStorageException("Failed to get or create secret key", e))
        }
    }
    
    /**
     * Encrypts data using AES-256-GCM.
     */
    private suspend fun encrypt(data: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val secretKey = getOrCreateSecretKey().getOrThrow()
            
            val cipher = Cipher.getInstance(AES_MODE)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
            // Combine IV and encrypted data
            val combined = ByteArray(iv.size + encryptedData.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedData, 0, combined, iv.size, encryptedData.size)
            
            val encoded = Base64.encodeToString(combined, Base64.DEFAULT)
            Result.success(encoded)
        } catch (e: Exception) {
            Result.failure(SecureStorageException("Failed to encrypt data", e))
        }
    }
    
    /**
     * Decrypts data using AES-256-GCM.
     */
    private suspend fun decrypt(encryptedData: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val secretKey = getOrCreateSecretKey().getOrThrow()
            
            val combined = Base64.decode(encryptedData, Base64.DEFAULT)
            
            // Extract IV and encrypted data
            val iv = ByteArray(GCM_IV_LENGTH)
            val encrypted = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, iv.size)
            System.arraycopy(combined, iv.size, encrypted, 0, encrypted.size)
            
            val cipher = Cipher.getInstance(AES_MODE)
            val gcmParameterSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec)
            
            val decryptedData = cipher.doFinal(encrypted)
            val result = String(decryptedData, Charsets.UTF_8)
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(SecureStorageException("Failed to decrypt data", e))
        }
    }
    
    override suspend fun putString(key: String, value: String): Result<Unit> {
        return try {
            val encryptedValue = encrypt(value).getOrThrow()
            withContext(Dispatchers.IO) {
                sharedPreferences.edit { putString(key, encryptedValue) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(SecureStorageException("Failed to store string", e))
        }
    }
    
    override suspend fun getString(key: String, defaultValue: String): Result<String> {
        return try {
            val encryptedValue = withContext(Dispatchers.IO) {
                sharedPreferences.getString(key, null)
            }
            
            if (encryptedValue == null) {
                Result.success(defaultValue)
            } else {
                decrypt(encryptedValue)
            }
        } catch (e: Exception) {
            Result.failure(SecureStorageException("Failed to retrieve string", e))
        }
    }
    
    override suspend fun putInt(key: String, value: Int): Result<Unit> {
        return putString(key, value.toString())
    }
    
    override suspend fun getInt(key: String, defaultValue: Int): Result<Int> {
        return try {
            val stringValue = getString(key, defaultValue.toString()).getOrThrow()
            Result.success(stringValue.toInt())
        } catch (e: NumberFormatException) {
            Result.failure(SecureStorageException("Failed to parse integer value", e))
        } catch (e: Exception) {
            Result.failure(SecureStorageException("Failed to retrieve integer", e))
        }
    }
    
    override suspend fun putLong(key: String, value: Long): Result<Unit> {
        return putString(key, value.toString())
    }
    
    override suspend fun getLong(key: String, defaultValue: Long): Result<Long> {
        return try {
            val stringValue = getString(key, defaultValue.toString()).getOrThrow()
            Result.success(stringValue.toLong())
        } catch (e: NumberFormatException) {
            Result.failure(SecureStorageException("Failed to parse long value", e))
        } catch (e: Exception) {
            Result.failure(SecureStorageException("Failed to retrieve long", e))
        }
    }
    
    override suspend fun putBoolean(key: String, value: Boolean): Result<Unit> {
        return putString(key, value.toString())
    }
    
    override suspend fun getBoolean(key: String, defaultValue: Boolean): Result<Boolean> {
        return try {
            val stringValue = getString(key, defaultValue.toString()).getOrThrow()
            Result.success(stringValue.toBoolean())
        } catch (e: Exception) {
            Result.failure(SecureStorageException("Failed to retrieve boolean", e))
        }
    }
    
    override suspend fun putFloat(key: String, value: Float): Result<Unit> {
        return putString(key, value.toString())
    }
    
    override suspend fun getFloat(key: String, defaultValue: Float): Result<Float> {
        return try {
            val stringValue = getString(key, defaultValue.toString()).getOrThrow()
            Result.success(stringValue.toFloat())
        } catch (e: NumberFormatException) {
            Result.failure(SecureStorageException("Failed to parse float value", e))
        } catch (e: Exception) {
            Result.failure(SecureStorageException("Failed to retrieve float", e))
        }
    }
    
    override suspend fun putStringSet(key: String, values: Set<String>): Result<Unit> {
        return try {
            val serialized = values.joinToString(separator = "\u001F") // Unit Separator character
            putString(key, serialized)
        } catch (e: Exception) {
            Result.failure(SecureStorageException("Failed to store string set", e))
        }
    }
    
    override suspend fun getStringSet(key: String, defaultValue: Set<String>): Result<Set<String>> {
        return try {
            val serialized = getString(key, "").getOrThrow()
            if (serialized.isEmpty()) {
                Result.success(defaultValue)
            } else {
                val values = serialized.split("\u001F").toSet()
                Result.success(values)
            }
        } catch (e: Exception) {
            Result.failure(SecureStorageException("Failed to retrieve string set", e))
        }
    }
    
    override suspend fun contains(key: String): Result<Boolean> {
        return try {
            val exists = withContext(Dispatchers.IO) {
                sharedPreferences.contains(key)
            }
            Result.success(exists)
        } catch (e: Exception) {
            Result.failure(SecureStorageException("Failed to check key existence", e))
        }
    }
    
    override suspend fun remove(key: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                sharedPreferences.edit { remove(key) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(SecureStorageException("Failed to remove key", e))
        }
    }
    
    override suspend fun clear(): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                sharedPreferences.edit { clear() }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(SecureStorageException("Failed to clear storage", e))
        }
    }
    
    override suspend fun getAllKeys(): Result<Set<String>> {
        return try {
            val keys = withContext(Dispatchers.IO) {
                sharedPreferences.all.keys
            }
            Result.success(keys)
        } catch (e: Exception) {
            Result.failure(SecureStorageException("Failed to get all keys", e))
        }
    }
}

/**
 * Exception thrown by SecureStorage operations.
 */
class SecureStorageException(message: String, cause: Throwable? = null) : Exception(message, cause)
