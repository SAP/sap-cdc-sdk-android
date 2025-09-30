package com.sap.cdc.android.sdk.feature.session

/**
 * Interface for session security operations.
 * Provides an abstraction layer for SessionSecure to enable testability and dependency injection.
 */
interface SessionSecureProvider {
    
    /**
     * Check if a session is available.
     * @return true if session exists, false otherwise
     */
    fun availableSession(): Boolean
    
    /**
     * Get the current session.
     * @return Session object if available, null otherwise
     */
    fun getSession(): Session?
    
    /**
     * Set a new session.
     * @param session The session to store
     */
    fun setSession(session: Session)
    
    /**
     * Invalidate the current session (clear and cleanup).
     */
    fun invalidateSession()
    
    /**
     * Clear the current session without invalidation.
     */
    fun clearSession()
    
    /**
     * Get the current session security level.
     * @return SessionSecureLevel indicating encryption type
     */
    fun sessionSecureLevel(): SessionSecureLevel
    
    /**
     * Secure session with biometric authentication.
     * @param encryptedSession Base64 encoded encrypted session
     * @param iv Base64 encoded initialization vector
     */
    fun secureBiometricSession(encryptedSession: String, iv: String)
    
    /**
     * Unlock biometric secured session.
     * @param decryptedSession Decrypted session JSON
     */
    fun unlockBiometricSession(decryptedSession: String)
    
    /**
     * Check if session is biometric locked.
     * @return true if session requires biometric unlock, false otherwise
     */
    fun biometricLocked(): Boolean
}
