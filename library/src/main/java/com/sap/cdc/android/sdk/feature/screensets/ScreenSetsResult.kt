package com.sap.cdc.android.sdk.feature.screensets

import com.sap.cdc.android.sdk.core.api.model.CDCError

/**
 * Event data for ScreenSets events
 */
data class ScreenSetsEventData(
    val eventName: String,
    val content: Map<String, Any?>? = null,
    val screenSetId: String? = null,
    val sourceContainerID: String? = null
)

/**
 * Error information for ScreenSets events
 */
data class ScreenSetsError(
    val message: String,
    val eventName: String? = null,
    val cdcError: CDCError? = null,
    val details: Map<String, Any?>? = null
)

/**
 * Sealed class representing the result of ScreenSets operations
 */
sealed class ScreenSetsResult {
    data class Event(val eventData: ScreenSetsEventData) : ScreenSetsResult()
    data class Error(val screenSetsError: ScreenSetsError) : ScreenSetsResult()
}
