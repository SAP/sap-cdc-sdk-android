package com.sap.cdc.android.sdk.events

/**
 * Global provider for the CDC Event Bus instance.
 * This singleton ensures that the event bus is accessible throughout the SDK
 * while maintaining proper initialization lifecycle.
 */
object CDCEventBusProvider {
    
    @Volatile
    private var eventBusInstance: LifecycleAwareEventBus? = null
    
    /**
     * Initialize the global event bus instance.
     * This should be called once during SDK initialization.
     * 
     * @param eventBus The event bus implementation to use
     */
    fun initialize(eventBus: LifecycleAwareEventBus = CDCLifecycleEventBus()) {
        synchronized(this) {
            if (eventBusInstance == null) {
                eventBusInstance = eventBus
            }
        }
    }
    
    /**
     * Get the global event bus instance.
     * Throws an exception if the event bus hasn't been initialized.
     * 
     * @return The global event bus instance
     * @throws IllegalStateException if the event bus hasn't been initialized
     */
    fun getEventBus(): LifecycleAwareEventBus {
        return eventBusInstance ?: throw IllegalStateException(
            "CDCEventBus has not been initialized. Call CDCEventBusProvider.initialize() first."
        )
    }
    
    /**
     * Check if the event bus has been initialized.
     * 
     * @return true if initialized, false otherwise
     */
    fun isInitialized(): Boolean {
        return eventBusInstance != null
    }
    
    /**
     * Reset the event bus instance (primarily for testing).
     * This will clear the current instance and allow re-initialization.
     */
    internal fun reset() {
        synchronized(this) {
            eventBusInstance = null
        }
    }
}
