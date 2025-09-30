package com.sap.cdc.android.sdk.feature.session

/**
 * Test implementation of SessionSecureProvider for unit testing.
 * Provides configurable behavior and in-memory session storage.
 */
class TestSessionSecureProvider : SessionSecureProvider {
    
    // Test state variables
    private var session: Session? = null
    private var secureLevel: SessionSecureLevel = SessionSecureLevel.STANDARD
    private var isBiometricLocked: Boolean = false
    private var biometricEncryptedSession: String? = null
    private var biometricIv: String? = null
    
    // Configuration for test behavior
    var shouldReturnNullSession: Boolean = false
    var shouldThrowOnGetSession: Boolean = false
    var shouldReturnSessionAvailable: Boolean = false
    
    override fun availableSession(): Boolean {
        return if (shouldReturnSessionAvailable) {
            shouldReturnSessionAvailable
        } else {
            session != null
        }
    }
    
    override fun getSession(): Session? {
        if (shouldThrowOnGetSession) {
            throw RuntimeException("Test exception")
        }
        if (shouldReturnNullSession) {
            return null
        }
        if (isBiometricLocked) {
            return null // Biometric locked sessions can't be retrieved
        }
        return session
    }
    
    override fun setSession(session: Session) {
        this.session = session
        this.isBiometricLocked = false
        this.secureLevel = SessionSecureLevel.STANDARD
    }
    
    override fun invalidateSession() {
        clearSession()
    }
    
    override fun clearSession() {
        session = null
        isBiometricLocked = false
        biometricEncryptedSession = null
        biometricIv = null
        secureLevel = SessionSecureLevel.STANDARD
    }
    
    override fun sessionSecureLevel(): SessionSecureLevel {
        return secureLevel
    }
    
    override fun secureBiometricSession(encryptedSession: String, iv: String) {
        this.biometricEncryptedSession = encryptedSession
        this.biometricIv = iv
        this.secureLevel = SessionSecureLevel.BIOMETRIC
        this.isBiometricLocked = true
    }
    
    override fun unlockBiometricSession(decryptedSession: String) {
        if (isBiometricLocked && biometricEncryptedSession != null) {
            // Simulate unlocking by parsing the decrypted session JSON
            // In real implementation, this would deserialize the JSON
            this.isBiometricLocked = false
        }
    }
    
    override fun biometricLocked(): Boolean {
        return isBiometricLocked
    }
    
    // Test helper methods
    fun reset() {
        clearSession()
        shouldReturnNullSession = false
        shouldThrowOnGetSession = false
        shouldReturnSessionAvailable = false
    }
    
    fun simulateBiometricLock() {
        isBiometricLocked = true
        secureLevel = SessionSecureLevel.BIOMETRIC
    }
    
    fun simulateSessionExpiry() {
        session = null
    }
    
    fun getStoredBiometricData(): Pair<String?, String?> {
        return Pair(biometricEncryptedSession, biometricIv)
    }
}
