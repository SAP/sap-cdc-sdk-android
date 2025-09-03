package com.sap.cdc.android.sdk.events

import com.sap.cdc.android.sdk.notifications.CDCNotificationActionData
import java.util.UUID

/**
 * Base interface for all CDC events in the lifecycle-aware event bus.
 * Provides common properties for event identification and tracking.
 */
interface CDCEvent {
    /**
     * Timestamp when the event was created (milliseconds since epoch).
     */
    val timestamp: Long
    
    /**
     * Unique identifier for this event instance.
     */
    val eventId: String
    
    /**
     * Source component or service that generated this event.
     */
    val source: String
}

/**
 * Session-related events in the CDC SDK.
 * These events are emitted during session lifecycle operations.
 */
sealed class SessionEvent : CDCEvent {
    
    /**
     * Event emitted when a session has expired.
     * @param sessionId The identifier of the expired session
     * @param timestamp When the event was created
     * @param eventId Unique identifier for this event
     * @param source The source component that detected the expiration
     */
    data class SessionExpired(
        val sessionId: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val eventId: String = UUID.randomUUID().toString(),
        override val source: String = "SessionManager"
    ) : SessionEvent()
    
    /**
     * Event emitted when a session has been successfully refreshed.
     * @param sessionId The identifier of the refreshed session
     * @param newExpirationTime The new expiration timestamp for the session
     * @param timestamp When the event was created
     * @param eventId Unique identifier for this event
     * @param source The source component that performed the refresh
     */
    data class SessionRefreshed(
        val sessionId: String,
        val newExpirationTime: Long,
        override val timestamp: Long = System.currentTimeMillis(),
        override val eventId: String = UUID.randomUUID().toString(),
        override val source: String = "SessionManager"
    ) : SessionEvent()
    
    /**
     * Event emitted when session verification is requested.
     * @param sessionId The identifier of the session to verify
     * @param timestamp When the event was created
     * @param eventId Unique identifier for this event
     * @param source The source component requesting verification
     */
    data class VerifySession(
        val sessionId: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val eventId: String = UUID.randomUUID().toString(),
        override val source: String = "SessionManager"
    ) : SessionEvent()
}

/**
 * Message-related events in the CDC SDK.
 * These events are emitted during Firebase messaging operations.
 */
sealed class MessageEvent : CDCEvent {
    
    /**
     * Event emitted when a new Firebase token is received.
     * @param token The new Firebase messaging token
     * @param timestamp When the event was created
     * @param eventId Unique identifier for this event
     * @param source The source component that received the token
     */
    data class TokenReceived(
        val token: String,
        override val timestamp: Long = System.currentTimeMillis(),
        override val eventId: String = UUID.randomUUID().toString(),
        override val source: String = "FirebaseMessagingService"
    ) : MessageEvent()
    
    /**
     * Event emitted when a remote message is received.
     * @param data The message data received from Firebase
     * @param timestamp When the event was created
     * @param eventId Unique identifier for this event
     * @param source The source component that received the message
     */
    data class RemoteMessageReceived(
        val data: Map<String, String>,
        override val timestamp: Long = System.currentTimeMillis(),
        override val eventId: String = UUID.randomUUID().toString(),
        override val source: String = "FirebaseMessagingService"
    ) : MessageEvent()
    
    /**
     * Event emitted when a notification action is received.
     * @param action The action identifier
     * @param data The notification action data
     * @param timestamp When the event was created
     * @param eventId Unique identifier for this event
     * @param source The source component that received the action
     */
    data class NotificationActionReceived(
        val action: String,
        val data: CDCNotificationActionData,
        override val timestamp: Long = System.currentTimeMillis(),
        override val eventId: String = UUID.randomUUID().toString(),
        override val source: String = "NotificationReceiver"
    ) : MessageEvent()
}
