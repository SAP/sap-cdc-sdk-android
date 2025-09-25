package com.sap.cdc.android.sdk.feature.screensets

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ScreenSetsCallbacks
 */
class ScreenSetsCallbacksTest {

    @Test
    fun testCallbackAssignmentAndExecution() {
        val callbacks = ScreenSetsCallbacks()
        val testEventData = ScreenSetsEventData(
            eventName = "test_event",
            content = mapOf("key" to "value"),
            screenSetId = "test-screen-set",
            sourceContainerID = "test-container"
        )
        
        var called = false
        var receivedEvent: ScreenSetsEventData? = null

        callbacks.onLoad = { event: ScreenSetsEventData ->
            called = true
            receivedEvent = event
        }

        callbacks.onLoad?.invoke(testEventData)

        assertTrue("Callback should be called", called)
        assertEquals("Event data should match", testEventData, receivedEvent)
    }

    @Test
    fun testChainingMethods() {
        val callbacks = ScreenSetsCallbacks()
        val callOrder = mutableListOf<String>()

        val result = callbacks
            .doOnLoad { callOrder.add("first") }
            .doOnLoad { callOrder.add("second") }

        assertSame("Should return same instance for chaining", callbacks, result)
        
        val testEventData = ScreenSetsEventData("test", null, null, null)
        callbacks.onLoad?.invoke(testEventData)

        assertEquals("Should have two calls", 2, callOrder.size)
        assertEquals("Calls should be in LIFO order", listOf("second", "first"), callOrder)
    }

    @Test
    fun testErrorCallback() {
        val callbacks = ScreenSetsCallbacks()
        val testError = ScreenSetsError(
            message = "Test error",
            eventName = "error",
            cdcError = null,
            details = null
        )
        
        var called = false
        var receivedError: ScreenSetsError? = null

        callbacks.onError = { error: ScreenSetsError ->
            called = true
            receivedError = error
        }

        callbacks.onError?.invoke(testError)

        assertTrue("Error callback should be called", called)
        assertEquals("Error should match", testError, receivedError)
    }

    @Test
    fun testCallbackIsNullWhenEmpty() {
        val callbacks = ScreenSetsCallbacks()
        assertNull("onLoad should be null when no callbacks added", callbacks.onLoad)
        assertNull("onError should be null when no callbacks added", callbacks.onError)
    }

    @Test
    fun testEventFiltering() {
        val callbacks = ScreenSetsCallbacks()
        var calledCount = 0
        
        callbacks
            .filterByEventName("allowed_event")
            .doOnAnyEvent { calledCount++ }

        val testEventData = ScreenSetsEventData("test", null, null, null)
        
        // Test allowed event
        val allowedEvent = testEventData.copy(eventName = "allowed_event")
        callbacks.onAnyEvent?.invoke(allowedEvent)
        assertEquals("Should be called for allowed event", 1, calledCount)

        // Test filtered out event
        val filteredEvent = testEventData.copy(eventName = "filtered_event")
        callbacks.onAnyEvent?.invoke(filteredEvent)
        assertEquals("Should not be called for filtered event", 1, calledCount)
    }
}
