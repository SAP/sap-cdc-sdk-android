package com.sap.cdc.android.sdk.events

import kotlin.reflect.KClass

/**
 * Defines the scope for event distribution in the lifecycle-aware event bus.
 * Events can be scoped to different contexts to provide better separation of concerns.
 */
sealed class EventScope {
    /**
     * Global scope - events are delivered to all subscribers regardless of context.
     */
    object GLOBAL : EventScope()
    
    /**
     * Session scope - events are scoped to a specific session.
     * @param sessionId The unique identifier for the session
     */
    data class SESSION(val sessionId: String) : EventScope()
    
    /**
     * User scope - events are scoped to a specific user.
     * @param userId The unique identifier for the user
     */
    data class USER(val userId: String) : EventScope()
    
    /**
     * Feature scope - events are scoped to a specific feature or module.
     * @param feature The feature identifier
     */
    data class FEATURE(val feature: String) : EventScope()
    
    /**
     * Custom scope - events are scoped to a custom identifier.
     * @param identifier The custom scope identifier
     */
    data class CUSTOM(val identifier: String) : EventScope()
}

/**
 * Internal key used to identify event channels in the event bus.
 * Combines event class type with scope for unique identification.
 */
internal data class EventKey(
    val eventClass: KClass<*>,
    val scope: EventScope
)
