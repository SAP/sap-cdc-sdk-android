package com.sap.cdc.android.sdk

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

    private val eventFlow = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 1)
    private var sessionScope: CoroutineScope? = null
    private val pendingSubscriptions = mutableListOf<suspend (SessionEvent) -> Unit>()

    fun initializeSessionScope(scope: CoroutineScope) {
        sessionScope = scope
        // Process pending subscriptions
        pendingSubscriptions.forEach { block ->
            eventFlow.onEach(block).launchIn(scope)
        }
        pendingSubscriptions.clear()
    }

    fun subscribeToSessionEvents(block: suspend (SessionEvent) -> Unit) {
        sessionScope?.let {
            eventFlow.onEach(block).launchIn(it)
        } ?: run {
            // Store the subscription if the scope is not initialized
            pendingSubscriptions.add(block)
        }
    }

    fun emitSessionEvent(sessionEvent: SessionEvent) {
        sessionScope?.launch { eventFlow.emit(sessionEvent) }
    }

    fun dispose() {
        sessionScope?.cancel()
        sessionScope = null
    }
}

sealed class SessionEvent {

    data object ExpiredSession : SessionEvent()

    data object VerifySession : SessionEvent()
}


