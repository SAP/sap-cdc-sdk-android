package com.sap.cdc.android.sdk.events

import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.reflect.KClass

/**
 * Lifecycle-aware event bus interface for the CIAM SDK.
 * Provides both lifecycle-aware subscriptions for UI components and manual subscriptions for SDK internal components.
 */
interface LifecycleAwareEventBus {
    
    /**
     * Subscribe to events with automatic lifecycle management.
     * Subscriptions are automatically cleaned up when the lifecycle owner is destroyed.
     * Events are only delivered when the lifecycle is in STARTED state or above.
     * 
     * @param lifecycleOwner The lifecycle owner (Activity, Fragment, etc.) that manages this subscription
     * @param eventClass The class of events to subscribe to
     * @param scope The scope for event filtering (default: GLOBAL)
     * @param dispatcher The coroutine dispatcher for event handling (default: Main)
     * @param onEvent The callback function to handle received events
     */
    fun <T : Any> subscribe(
        lifecycleOwner: LifecycleOwner,
        eventClass: KClass<T>,
        scope: EventScope = EventScope.GLOBAL,
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        onEvent: suspend (T) -> Unit
    )
    
    /**
     * Subscribe to events with manual lifecycle management.
     * Used by SDK internal components that don't have access to Android lifecycle components.
     * The caller is responsible for calling unsubscribe() to clean up resources.
     * 
     * @param eventClass The class of events to subscribe to
     * @param scope The scope for event filtering (default: GLOBAL)
     * @param dispatcher The coroutine dispatcher for event handling (default: Main)
     * @param onEvent The callback function to handle received events
     * @return EventSubscription handle for manual cleanup
     */
    fun <T : Any> subscribeManual(
        eventClass: KClass<T>,
        scope: EventScope = EventScope.GLOBAL,
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        onEvent: suspend (T) -> Unit
    ): EventSubscription
    
    /**
     * Emit an event to all subscribers in the specified scope.
     * 
     * @param event The event to emit
     * @param scope The scope for event distribution (default: GLOBAL)
     * @param dispatcher The coroutine dispatcher for event emission (default: Main)
     */
    fun <T : Any> emit(
        event: T,
        scope: EventScope = EventScope.GLOBAL,
        dispatcher: CoroutineDispatcher = Dispatchers.Main
    )
    
    /**
     * Clear all subscriptions and channels for a specific scope.
     * Useful for cleaning up when a scope (like a user session) ends.
     * 
     * @param scope The scope to clear
     */
    fun clearScope(scope: EventScope)
}
