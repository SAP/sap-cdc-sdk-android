package com.sap.cdc.android.sdk.events

/**
 * Represents a manual event subscription that can be controlled and cleaned up.
 * Used for SDK internal components that don't have access to Android lifecycle components.
 */
interface EventSubscription {
    /**
     * Unsubscribes from the event stream and cleans up resources.
     * After calling this method, no more events will be delivered to the subscriber.
     */
    fun unsubscribe()
    
    /**
     * Indicates whether this subscription is still active.
     * Returns false after unsubscribe() has been called.
     */
    val isActive: Boolean
}
