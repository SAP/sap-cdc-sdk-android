package com.sap.cdc.android.sdk.feature.screensets

import com.sap.cdc.android.sdk.feature.AuthError

/**
 * Event data container for ScreenSets lifecycle events.
 * 
 * Contains information about web-based authentication UI events triggered
 * by user interactions or system state changes.
 * 
 * @property eventName The name of the event (e.g., "load", "login", "error")
 * @property content Optional map containing event-specific data
 * @property screenSetId Identifier of the screen set that triggered the event
 * @property sourceContainerID Container ID where the event originated
 * 
 * @see ScreenSetsCallbacks
 * @see WebBridgeJS
 */
data class ScreenSetsEventData(
    val eventName: String,
    val content: Map<String, Any?>? = null,
    val screenSetId: String? = null,
    val sourceContainerID: String? = null
)

/**
 * Error information for ScreenSets operations.
 * 
 * Wraps error details from web-based authentication flows, providing
 * context about what went wrong and where.
 * 
 * @property message Human-readable error message
 * @property eventName Name of the event where the error occurred
 * @property authError Structured authentication error object if available
 * @property details Additional error context and metadata
 * 
 * @see AuthError
 * @see ScreenSetsCallbacks.onError
 */
data class ScreenSetsError(
    val message: String,
    val eventName: String? = null,
    val authError: AuthError? = null,
    val details: Map<String, Any?>? = null
)

/**
 * Sealed class representing ScreenSets operation results.
 * 
 * Provides type-safe handling of either successful events or errors
 * from web-based authentication flows.
 * 
 * @see ScreenSetsEventData
 * @see ScreenSetsError
 */
sealed class ScreenSetsResult {
    /**
     * Successful event result.
     * @property eventData The event data
     */
    data class Event(val eventData: ScreenSetsEventData) : ScreenSetsResult()
    
    /**
     * Error result.
     * @property screenSetsError The error information
     */
    data class Error(val screenSetsError: ScreenSetsError) : ScreenSetsResult()
}
