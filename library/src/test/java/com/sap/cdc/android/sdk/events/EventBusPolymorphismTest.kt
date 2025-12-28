package com.sap.cdc.android.sdk.events

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test to verify that the event bus properly handles polymorphism.
 * This test ensures that subscribers to base event classes receive events from their subclasses.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EventBusPolymorphismTest {

    private lateinit var eventBus: CDCLifecycleEventBus
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        eventBus = CDCLifecycleEventBus()
        CDCEventBusProvider.reset()
        CDCEventBusProvider.initialize(eventBus)
    }

    @After
    fun tearDown() {
        CDCEventBusProvider.reset()
        Dispatchers.resetMain()
    }

    @Test
    fun `test MessageEvent subscriber receives TokenReceived events`() = runTest {
        // Given
        var receivedEvent: MessageEvent? = null
        val subscription = eventBus.subscribeManual(
            eventClass = MessageEvent::class,
            scope = EventScope.GLOBAL
        ) { event ->
            receivedEvent = event
        }

        // When - emit a TokenReceived event (subclass of MessageEvent)
        val tokenEvent = MessageEvent.TokenReceived("test-token-123")
        eventBus.emit(tokenEvent, EventScope.GLOBAL)

        // Allow some time for the event to be processed
        delay(100)

        // Then
        assertTrue(receivedEvent is MessageEvent.TokenReceived, "Should receive TokenReceived event")
        assertEquals("test-token-123", (receivedEvent as MessageEvent.TokenReceived).token)

        subscription.unsubscribe()
    }

    @Test
    fun `test MessageEvent subscriber receives RemoteMessageReceived events`() = runTest {
        // Given
        var receivedEvent: MessageEvent? = null
        val subscription = eventBus.subscribeManual(
            eventClass = MessageEvent::class,
            scope = EventScope.GLOBAL
        ) { event ->
            receivedEvent = event
        }

        // When - emit a RemoteMessageReceived event (subclass of MessageEvent)
        val messageData = mapOf("key1" to "value1", "key2" to "value2")
        val remoteMessageEvent = MessageEvent.RemoteMessageReceived(messageData)
        eventBus.emit(remoteMessageEvent, EventScope.GLOBAL)

        // Allow some time for the event to be processed
        delay(100)

        // Then
        assertTrue(receivedEvent is MessageEvent.RemoteMessageReceived, "Should receive RemoteMessageReceived event")
        assertEquals(messageData, (receivedEvent as MessageEvent.RemoteMessageReceived).data)

        subscription.unsubscribe()
    }

    @Test
    fun `test specific subscriber still receives only specific events`() = runTest {
        // Given
        var receivedTokenEvents = 0
        var receivedRemoteMessageEvents = 0

        val tokenSubscription = eventBus.subscribeManual(
            eventClass = MessageEvent.TokenReceived::class,
            scope = EventScope.GLOBAL
        ) { _ ->
            receivedTokenEvents++
        }

        val remoteMessageSubscription = eventBus.subscribeManual(
            eventClass = MessageEvent.RemoteMessageReceived::class,
            scope = EventScope.GLOBAL
        ) { _ ->
            receivedRemoteMessageEvents++
        }

        // When - emit both types of events
        eventBus.emit(MessageEvent.TokenReceived("token1"), EventScope.GLOBAL)
        eventBus.emit(MessageEvent.RemoteMessageReceived(mapOf("test" to "data")), EventScope.GLOBAL)
        eventBus.emit(MessageEvent.TokenReceived("token2"), EventScope.GLOBAL)

        // Allow some time for events to be processed
        delay(100)

        // Then
        assertEquals(2, receivedTokenEvents, "Should receive only TokenReceived events")
        assertEquals(1, receivedRemoteMessageEvents, "Should receive only RemoteMessageReceived events")

        tokenSubscription.unsubscribe()
        remoteMessageSubscription.unsubscribe()
    }

    @Test
    fun `test extension function emitTokenReceived works with MessageEvent subscriber`() = runTest {
        // Given
        var receivedEvent: MessageEvent? = null
        val subscription = eventBus.subscribeManual(
            eventClass = MessageEvent::class,
            scope = EventScope.GLOBAL
        ) { event ->
            receivedEvent = event
        }

        // When - use the extension function to emit token
        Any().emitTokenReceived("extension-token-456")

        // Allow some time for the event to be processed
        delay(100)

        // Then
        assertTrue(receivedEvent is MessageEvent.TokenReceived, "Should receive TokenReceived event via extension")
        assertEquals("extension-token-456", (receivedEvent as MessageEvent.TokenReceived).token)

        subscription.unsubscribe()
    }
}
