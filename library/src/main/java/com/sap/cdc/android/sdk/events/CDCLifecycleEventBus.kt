package com.sap.cdc.android.sdk.events

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sap.cdc.android.sdk.CDCDebuggable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Concrete implementation of the lifecycle-aware event bus for the CDC SDK.
 * Uses coroutines and channels for efficient event distribution with lifecycle awareness.
 */
class CDCLifecycleEventBus : LifecycleAwareEventBus {
    
    companion object {
        private const val LOG_TAG = "CDCLifecycleEventBus"
    }
    
    // Event channels for different event types and scopes
    private val eventChannels = ConcurrentHashMap<EventKey, Channel<Any>>()
    
    // Track lifecycle subscriptions for automatic cleanup
    private val lifecycleSubscriptions = ConcurrentHashMap<LifecycleOwner, MutableSet<EventKey>>()
    
    // Track manual subscriptions for cleanup
    private val manualSubscriptions = ConcurrentHashMap<EventKey, MutableSet<ManualEventSubscription>>()
    
    override fun <T : Any> subscribe(
        lifecycleOwner: LifecycleOwner,
        eventClass: KClass<T>,
        scope: EventScope,
        dispatcher: CoroutineDispatcher,
        onEvent: suspend (T) -> Unit
    ) {
        val eventKey = EventKey(eventClass, scope)
        
        CDCDebuggable.log(LOG_TAG, "Subscribing to ${eventClass.simpleName} events with scope $scope")
        
        // Create or get existing channel
        val channel = eventChannels.getOrPut(eventKey) {
            Channel(Channel.UNLIMITED)
        }
        
        // Track subscription for cleanup
        lifecycleSubscriptions.getOrPut(lifecycleOwner) { mutableSetOf() }.add(eventKey)
        
        // Subscribe with lifecycle awareness
        lifecycleOwner.lifecycleScope.launch(dispatcher) {
            // Simple lifecycle awareness - only process events when lifecycle is at least STARTED
            while (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                try {
                    for (event in channel) {
                        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            @Suppress("UNCHECKED_CAST")
                            onEvent(event as T)
                        }
                    }
                } catch (e: Exception) {
                    CDCDebuggable.log(LOG_TAG, "Event handling error: ${e.message}")
                    break
                }
            }
        }
        
        // Auto-cleanup on destroy
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                cleanupLifecycleSubscriptions(owner)
            }
        })
    }
    
    override fun <T : Any> subscribeManual(
        eventClass: KClass<T>,
        scope: EventScope,
        dispatcher: CoroutineDispatcher,
        onEvent: suspend (T) -> Unit
    ): EventSubscription {
        val eventKey = EventKey(eventClass, scope)
        
        CDCDebuggable.log(LOG_TAG, "Manual subscription to ${eventClass.simpleName} events with scope $scope")
        
        // Create or get existing channel
        val channel = eventChannels.getOrPut(eventKey) {
            Channel(Channel.UNLIMITED)
        }
        
        // Create manual subscription
        val subscription = ManualEventSubscription(
            eventKey = eventKey,
            job = CoroutineScope(dispatcher).launch {
                for (event in channel) {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        onEvent(event as T)
                    } catch (e: Exception) {
                        CDCDebuggable.log(LOG_TAG, "Event handling error: ${e.message}")
                    }
                }
            },
            onUnsubscribe = { unsubscribeManual(eventKey, it) }
        )
        
        // Track manual subscription
        manualSubscriptions.getOrPut(eventKey) { mutableSetOf() }.add(subscription)
        
        return subscription
    }
    
    override fun <T : Any> emit(
        event: T,
        scope: EventScope,
        dispatcher: CoroutineDispatcher
    ) {
        // Find all channels that should receive this event
        // This includes exact class matches and parent class matches (for polymorphism)
        val matchingChannels = eventChannels.filterKeys { eventKey ->
            eventKey.scope == scope && (
                eventKey.eventClass == event::class || 
                eventKey.eventClass.java.isAssignableFrom(event::class.java)
            )
        }
        
        if (matchingChannels.isNotEmpty()) {
            CDCDebuggable.log(LOG_TAG, "Emitting ${event::class.simpleName} event with scope $scope to ${matchingChannels.size} subscriber(s)")
            
            CoroutineScope(dispatcher).launch {
                matchingChannels.values.forEach { channel ->
                    try {
                        channel.trySend(event)
                    } catch (e: Exception) {
                        CDCDebuggable.log(LOG_TAG, "Event emission error: ${e.message}")
                    }
                }
            }
        } else {
            CDCDebuggable.log(LOG_TAG, "No subscribers for ${event::class.simpleName} event with scope $scope")
        }
    }
    
    override fun clearScope(scope: EventScope) {
        CDCDebuggable.log(LOG_TAG, "Clearing scope: $scope")
        
        val keysToRemove = eventChannels.keys.filter { it.scope == scope }
        
        keysToRemove.forEach { eventKey ->
            // Close and remove channel
            eventChannels[eventKey]?.close()
            eventChannels.remove(eventKey)
            
            // Clean up manual subscriptions
            manualSubscriptions[eventKey]?.forEach { subscription ->
                subscription.job.cancel()
            }
            manualSubscriptions.remove(eventKey)
            
            // Clean up lifecycle subscriptions
            lifecycleSubscriptions.values.forEach { subscriptionSet ->
                subscriptionSet.remove(eventKey)
            }
        }
    }
    
    private fun cleanupLifecycleSubscriptions(lifecycleOwner: LifecycleOwner) {
        CDCDebuggable.log(LOG_TAG, "Cleaning up lifecycle subscriptions for $lifecycleOwner")
        
        lifecycleSubscriptions[lifecycleOwner]?.forEach { eventKey ->
            // Only close channel if no manual subscriptions exist
            if (manualSubscriptions[eventKey]?.isEmpty() != false) {
                eventChannels[eventKey]?.close()
                eventChannels.remove(eventKey)
            }
        }
        lifecycleSubscriptions.remove(lifecycleOwner)
    }
    
    private fun unsubscribeManual(eventKey: EventKey, subscription: ManualEventSubscription) {
        CDCDebuggable.log(LOG_TAG, "Unsubscribing manual subscription for ${eventKey.eventClass.simpleName}")
        
        manualSubscriptions[eventKey]?.remove(subscription)
        
        // Clean up empty sets and channels
        if (manualSubscriptions[eventKey]?.isEmpty() == true) {
            manualSubscriptions.remove(eventKey)
            
            // Only close channel if no lifecycle subscriptions exist
            if (lifecycleSubscriptions.values.none { it.contains(eventKey) }) {
                eventChannels[eventKey]?.close()
                eventChannels.remove(eventKey)
            }
        }
    }
}

/**
 * Internal implementation of EventSubscription for manual subscriptions.
 */
private class ManualEventSubscription(
    internal val eventKey: EventKey,
    internal val job: Job,
    private val onUnsubscribe: (ManualEventSubscription) -> Unit
) : EventSubscription {
    
    override fun unsubscribe() {
        if (job.isActive) {
            job.cancel()
            onUnsubscribe(this)
        }
    }
    
    override val isActive: Boolean
        get() = job.isActive
}
