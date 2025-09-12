package com.sap.cdc.bitsnbytes.feature.auth

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.bitsnbytes.navigation.NavigationCoordinator
import com.sap.cdc.bitsnbytes.navigation.ProfileScreenRoute

/**
 * BiometricLifecycleManager handles automatic biometric session locking/unlocking
 * based on application lifecycle events (background/foreground).
 *
 * Behavior:
 * - When app goes to background: If biometric is active, lock the session
 * - When app comes to foreground: If biometric is locked, navigate to BiometricLocked screen
 */
class BiometricLifecycleManager(
    private val authenticationFlowDelegate: AuthenticationFlowDelegate
) : LifecycleEventObserver {

    companion object {
        private const val LOG_TAG = "BiometricLifecycleManager"
    }

    /**
     * Initialize the lifecycle manager by registering it with ProcessLifecycleOwner.
     * This should be called once from the Application onCreate method.
     */
    fun initialize() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        CDCDebuggable.log(LOG_TAG, "BiometricLifecycleManager initialized")
    }

    /**
     * Handle lifecycle state changes.
     * - ON_STOP: App moved to background - lock biometric session if active
     * - ON_START: App came to foreground - navigate to unlock screen if biometric is locked
     */
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                // App moved to background
                if (authenticationFlowDelegate.hasValidSession() && 
                    authenticationFlowDelegate.isBiometricActive() && 
                    !authenticationFlowDelegate.isBiometricLocked()) {
                    
                    CDCDebuggable.log(LOG_TAG, "App backgrounded - locking biometric session")
                    authenticationFlowDelegate.biometricLock()
                }
            }
            Lifecycle.Event.ON_START -> {
                // App came to foreground
                // Note: Don't check hasValidSession() as session will be null after lock
                if (authenticationFlowDelegate.isBiometricLocked()) {
                    
                    CDCDebuggable.log(LOG_TAG, "App foregrounded - biometric is locked, checking if navigation is available")
                    
                    // Check if navigation graph is available before attempting navigation
                    // This prevents crashes when app starts from killed state
                    if (isNavigationAvailable()) {
                        CDCDebuggable.log(LOG_TAG, "Navigation available - navigating to unlock screen")
                        NavigationCoordinator.INSTANCE.popToRootAndNavigate(
                            toRoute = ProfileScreenRoute.BiometricLocked.route,
                            rootRoute = ProfileScreenRoute.BiometricLocked.route
                        )
                    } else {
                        CDCDebuggable.log(LOG_TAG, "Navigation not available yet - skipping navigation to unlock screen")
                    }
                }
            }
            else -> {
                // Ignore other lifecycle events
            }
        }
    }

    /**
     * Check if navigation is available and safe to use.
     * This prevents crashes when the app starts from a killed state and navigation hasn't been initialized yet.
     */
    private fun isNavigationAvailable(): Boolean {
        return try {
            NavigationCoordinator.INSTANCE.isNavigationAvailable()
        } catch (e: Exception) {
            CDCDebuggable.log(LOG_TAG, "Navigation availability check failed: ${e.message}")
            false
        }
    }

    /**
     * Clean up the lifecycle observer.
     * Should be called when the application is being destroyed.
     */
    fun cleanup() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        CDCDebuggable.log(LOG_TAG, "BiometricLifecycleManager cleaned up")
    }
}
