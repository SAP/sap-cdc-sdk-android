package com.sap.cdc.android.sdk

import com.sap.cdc.android.sdk.auth.notifications.CDCNotificationActionData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Created by Tal Mirmelshtein on 20/07/2024
 * Copyright: SAP LTD.
 */

/**
 * CDC Message Event Bus.
 * A shared flow event bus for message events.
 */
object CDCMessageEventBus {

    // Flow for session events.
    private val sessionEventFlow = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 1)
    private var sessionScope: CoroutineScope? = null
    private val pendingSubscriptions = mutableListOf<suspend (SessionEvent) -> Unit>()

    // Flow for messaging events.
    private val messageEventFlow = MutableSharedFlow<MessageEvent>(extraBufferCapacity = 1)
    private var messageScope: CoroutineScope? = null
    private val pendingMessageSubscriptions = mutableListOf<suspend (MessageEvent) -> Unit>()

    fun initializeSessionScope(scope: CoroutineScope) {
        sessionScope = scope
        // Process pending subscriptions
        pendingSubscriptions.forEach { block ->
            sessionEventFlow.onEach(block).launchIn(scope)
        }
        pendingSubscriptions.clear()
    }

    fun initializeMessageScope(scope: CoroutineScope) {
        messageScope = scope
        // Process pending subscriptions
        pendingMessageSubscriptions.forEach { block ->
            messageEventFlow.onEach(block).launchIn(scope)
        }
        pendingMessageSubscriptions.clear()
    }

    fun subscribeToSessionEvents(block: suspend (SessionEvent) -> Unit) {
        sessionScope?.let {
            sessionEventFlow.onEach(block).launchIn(it)
        } ?: run {
            // Store the subscription if the scope is not initialized
            pendingSubscriptions.add(block)
        }
    }

    fun subscribeToMessageEvents(block: suspend (MessageEvent) -> Unit) {
        messageScope?.let {
            messageEventFlow.onEach(block).launchIn(it)
        } ?: run {
            // Store the subscription if the scope is not initialized
            pendingMessageSubscriptions.add(block)
        }
    }

    fun emitSessionEvent(sessionEvent: SessionEvent) {
        sessionScope?.launch { sessionEventFlow.emit(sessionEvent) }
    }

    fun emitMessageEvent(messageEvent: MessageEvent) {
        messageScope?.launch { messageEventFlow.emit(messageEvent) }
    }

    fun dispose() {
        sessionScope?.cancel()
        sessionScope = null
        messageScope?.cancel()
        messageScope = null
    }
}

sealed class SessionEvent {

    data object ExpiredSession : SessionEvent()

    data object VerifySession : SessionEvent()
}

/**
 * Message event.
 * Represents a message event.
 */
sealed class MessageEvent {

    data class EventWithToken(val token: String) : MessageEvent()

    data class EventWithRemoteMessageData(val data: Map<String, String>) : MessageEvent()

    data class EventWithRemoteActionData(val action: String, val data: CDCNotificationActionData) :
        MessageEvent()
}


