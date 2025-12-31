package com.sap.cdc.android.sdk.events

import androidx.lifecycle.LifecycleOwner
import com.sap.cdc.android.sdk.feature.notifications.CIAMNotificationActionData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Extension functions for easy usage of the lifecycle-aware event bus.
 * Provides convenient methods for subscribing to specific event types.
 */

// Global event bus instance accessed through the provider
private val CIAMEventBus: LifecycleAwareEventBus
    get() = CIAMEventBusProvider.getEventBus()

/**
 * Extension functions for UI components (lifecycle-aware subscriptions)
 */

/**
 * Subscribe to session events with automatic lifecycle management.
 * Events are only delivered when the lifecycle is in STARTED state or above.
 * 
 * @param scope The scope for event filtering (default: GLOBAL)
 * @param onEvent The callback function to handle received session events
 */
fun LifecycleOwner.subscribeToSessionEvents(
    scope: EventScope = EventScope.GLOBAL,
    onEvent: suspend (SessionEvent) -> Unit
) {
    CIAMEventBus.subscribe(this, SessionEvent::class, scope, onEvent = onEvent)
}

/**
 * Subscribe to message events with automatic lifecycle management.
 * Events are only delivered when the lifecycle is in STARTED state or above.
 * 
 * @param scope The scope for event filtering (default: GLOBAL)
 * @param onEvent The callback function to handle received message events
 */
fun LifecycleOwner.subscribeToMessageEvents(
    scope: EventScope = EventScope.GLOBAL,
    onEvent: suspend (MessageEvent) -> Unit
) {
    CIAMEventBus.subscribe(this, MessageEvent::class, scope, onEvent = onEvent)
}

/**
 * Extension functions for SDK internal components (manual subscriptions)
 */

/**
 * Subscribe to session events with manual lifecycle management.
 * The caller is responsible for calling unsubscribe() on the returned EventSubscription.
 * 
 * @param scope The scope for event filtering (default: GLOBAL)
 * @param dispatcher The coroutine dispatcher for event handling (default: IO for background work)
 * @param onEvent The callback function to handle received session events
 * @return EventSubscription handle for manual cleanup
 */
fun Any.subscribeToSessionEventsManual(
    scope: EventScope = EventScope.GLOBAL,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    onEvent: suspend (SessionEvent) -> Unit
): EventSubscription {
    return CIAMEventBus.subscribeManual(SessionEvent::class, scope, dispatcher, onEvent)
}

/**
 * Subscribe to message events with manual lifecycle management.
 * The caller is responsible for calling unsubscribe() on the returned EventSubscription.
 * 
 * @param scope The scope for event filtering (default: GLOBAL)
 * @param dispatcher The coroutine dispatcher for event handling (default: IO for background work)
 * @param onEvent The callback function to handle received message events
 * @return EventSubscription handle for manual cleanup
 */
fun Any.subscribeToMessageEventsManual(
    scope: EventScope = EventScope.GLOBAL,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    onEvent: suspend (MessageEvent) -> Unit
): EventSubscription {
    return CIAMEventBus.subscribeManual(MessageEvent::class, scope, dispatcher, onEvent)
}

/**
 * Emit extension functions for SDK components
 */

/**
 * Emit a session event to all subscribers in the specified scope.
 * 
 * @param event The session event to emit
 * @param scope The scope for event distribution (default: GLOBAL)
 */
fun Any.emitSessionEvent(
    event: SessionEvent,
    scope: EventScope = EventScope.GLOBAL
) {
    CIAMEventBus.emit(event, scope)
}

/**
 * Emit a message event to all subscribers in the specified scope.
 * 
 * @param event The message event to emit
 * @param scope The scope for event distribution (default: GLOBAL)
 */
fun Any.emitMessageEvent(
    event: MessageEvent,
    scope: EventScope = EventScope.GLOBAL
) {
    CIAMEventBus.emit(event, scope)
}

/**
 * Convenience functions for common session event emissions
 */

/**
 * Emit a session expired event.
 * 
 * @param sessionId The identifier of the expired session
 * @param scope The scope for event distribution (default: GLOBAL)
 */
fun Any.emitSessionExpired(
    sessionId: String,
    scope: EventScope = EventScope.GLOBAL
) {
    emitSessionEvent(SessionEvent.SessionExpired(sessionId), scope)
}

/**
 * Emit a session refreshed event.
 * 
 * @param sessionId The identifier of the refreshed session
 * @param newExpirationTime The new expiration timestamp for the session
 * @param scope The scope for event distribution (default: GLOBAL)
 */
fun Any.emitSessionRefreshed(
    sessionId: String,
    newExpirationTime: Long,
    scope: EventScope = EventScope.GLOBAL
) {
    emitSessionEvent(SessionEvent.SessionRefreshed(sessionId, newExpirationTime), scope)
}

/**
 * Emit a verify session event.
 * 
 * @param sessionId The identifier of the session to verify
 * @param scope The scope for event distribution (default: GLOBAL)
 */
fun Any.emitVerifySession(
    sessionId: String,
    scope: EventScope = EventScope.GLOBAL
) {
    emitSessionEvent(SessionEvent.VerifySession(sessionId), scope)
}

/**
 * Convenience functions for common message event emissions
 */

/**
 * Emit a token received event.
 * 
 * @param token The Firebase messaging token
 * @param scope The scope for event distribution (default: GLOBAL)
 */
fun Any.emitTokenReceived(
    token: String,
    scope: EventScope = EventScope.GLOBAL
) {
    emitMessageEvent(MessageEvent.TokenReceived(token), scope)
}

/**
 * Emit a remote message received event.
 * 
 * @param data The message data received from Firebase
 * @param scope The scope for event distribution (default: GLOBAL)
 */
fun Any.emitRemoteMessageReceived(
    data: Map<String, String>,
    scope: EventScope = EventScope.GLOBAL
) {
    emitMessageEvent(MessageEvent.RemoteMessageReceived(data), scope)
}

/**
 * Emit a notification action received event.
 * 
 * @param action The action identifier
 * @param data The notification action data
 * @param scope The scope for event distribution (default: GLOBAL)
 */
fun Any.emitNotificationActionReceived(
    action: String,
    data: CIAMNotificationActionData,
    scope: EventScope = EventScope.GLOBAL
) {
    emitMessageEvent(MessageEvent.NotificationActionReceived(action, data), scope)
}
