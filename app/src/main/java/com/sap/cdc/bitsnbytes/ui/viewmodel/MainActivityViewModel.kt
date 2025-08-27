package com.sap.cdc.bitsnbytes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sap.cdc.bitsnbytes.ui.navigation.AppStateManager
import kotlinx.coroutines.launch

/**
 * ViewModel for MainActivity with proper state management and lifecycle handling.
 * 
 * Responsibilities:
 * - Manage app-wide state through AppStateManager
 * - Handle initialization logic
 * - Coordinate between different app components
 * - Provide lifecycle-aware operations
 */
class MainActivityViewModel : ViewModel() {
    
    val appStateManager = AppStateManager()
    
    /**
     * Initialize the application with necessary startup operations
     */
    fun initializeApp() {
        viewModelScope.launch {
            // Clear any previous error states
            appStateManager.setError(null)
            
            // Any other initialization logic can go here
            // For example: check for updates, sync data, etc.
        }
    }
    
    /**
     * Handle session expiration
     */
    fun handleSessionExpired() {
        viewModelScope.launch {
            appStateManager.clearState()
            appStateManager.setCanNavigateBack(false)
        }
    }
    
    /**
     * Handle session verification
     */
    fun handleSessionVerification() {
        viewModelScope.launch {
            // Trigger session verification logic
            // This could involve checking with the backend
            appStateManager.setLoading(true)
            appStateManager.setError(null)
            
            try {
                // Session verification logic would go here
                // For now, just clear loading state
                appStateManager.setLoading(false)
            } catch (e: Exception) {
                appStateManager.setError("Session verification failed: ${e.message}")
                appStateManager.setLoading(false)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Any cleanup needed when ViewModel is cleared
    }
}
