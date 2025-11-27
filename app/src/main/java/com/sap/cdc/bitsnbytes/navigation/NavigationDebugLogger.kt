package com.sap.cdc.bitsnbytes.navigation

import android.util.Log
import androidx.navigation.NavController
import com.sap.cdc.bitsnbytes.ApplicationConfig

/**
 * Centralized navigation debug logger with full route tracking.
 * 
 * Features:
 * - Logs FROM → TO route transitions
 * - Shows full back stack state
 * - Tracks back stack size
 * - Structured, readable output
 * 
 * Usage in logcat: adb logcat -s NAV_DEBUG
 * 
 * The logger only outputs when ApplicationConfig.debugNavigationLogging is true.
 */
object NavigationDebugLogger {
    const val TAG = "NAV_DEBUG"
    
    /**
     * Log navigation with from/to route information from NavController.
     * 
     * @param source The component triggering the navigation (e.g., "ScreenSetViewModel")
     * @param action The navigation action being performed (e.g., "navigate", "navigateUp")
     * @param navController The NavController to extract route information from
     * @param targetRoute The target route for navigate() calls (optional)
     * @param details Additional context or details about the navigation (optional)
     */
    fun logNavigation(
        source: String,
        action: String,
        navController: NavController? = null,
        targetRoute: String? = null,
        details: String? = null
    ) {
        if (!ApplicationConfig.debugNavigationLogging) return
        
        val currentRoute = navController?.currentDestination?.route ?: "unknown"
        val previousRoute = navController?.previousBackStackEntry?.destination?.route
        val backStackSize = navController?.currentBackStack?.value?.size ?: 0
        
        val message = buildString {
            append("╔═══ [$source] $action ═══\n")
            append("║ FROM: $currentRoute\n")
            
            // Determine destination route
            when {
                targetRoute != null -> {
                    append("║ TO: $targetRoute\n")
                }
                action.contains("up", ignoreCase = true) || 
                action.contains("back", ignoreCase = true) -> {
                    append("║ TO: ${previousRoute ?: "root/exit"}\n")
                }
            }
            
            append("║ BackStack Size: $backStackSize\n")
            
            // Add optional details
            details?.let { append("║ Details: $it\n") }
            
            // Log full back stack for complete visibility
            if (backStackSize > 0) {
                val backStack = navController?.currentBackStack?.value
                    ?.mapNotNull { it.destination.route }
                    ?.joinToString(" → ")
                
                if (!backStack.isNullOrEmpty()) {
                    append("║ BackStack: $backStack\n")
                }
            }
            
            append("╚═══════════════════════")
        }
        
        Log.d(TAG, message)
    }
    
    /**
     * Log navigation event from ViewModel (before NavController receives it).
     * 
     * @param source The ViewModel emitting the event
     * @param event The navigation event being emitted
     * @param navController Optional NavController for current route context
     */
    fun logNavigationEvent(
        source: String, 
        event: Any, 
        navController: NavController? = null
    ) {
        if (!ApplicationConfig.debugNavigationLogging) return
        
        val currentRoute = navController?.currentDestination?.route ?: "unknown"
        
        val message = buildString {
            append("╔═══ [$source] Navigation Event ═══\n")
            append("║ Event: ${event::class.simpleName}\n")
            append("║ Current Route: $currentRoute\n")
            append("║ Event Details: $event\n")
            append("╚═══════════════════════")
        }
        
        Log.d(TAG, message)
    }
    
    /**
     * Log WebView lifecycle event with current navigation context.
     * 
     * @param source The component (e.g., "ScreenSetView")
     * @param lifecycleEvent The lifecycle event (e.g., "DisposableEffect", "LaunchedEffect")
     * @param navController Optional NavController for route context
     * @param details Additional context about the lifecycle event
     */
    fun logWebViewLifecycle(
        source: String,
        lifecycleEvent: String,
        navController: NavController? = null,
        details: String? = null
    ) {
        if (!ApplicationConfig.debugNavigationLogging) return
        
        val currentRoute = navController?.currentDestination?.route ?: "unknown"
        val backStackSize = navController?.currentBackStack?.value?.size ?: 0
        
        val message = buildString {
            append("╔═══ [$source] $lifecycleEvent ═══\n")
            append("║ Current Route: $currentRoute\n")
            append("║ BackStack Size: $backStackSize\n")
            details?.let { append("║ Details: $it\n") }
            append("╚═══════════════════════")
        }
        
        Log.d(TAG, message)
    }
}
