package com.sap.cdc.bitsnbytes.ui.state

import androidx.compose.runtime.Immutable
import com.sap.cdc.android.sdk.feature.screensets.ScreenSetsError
import com.sap.cdc.android.sdk.feature.screensets.ScreenSetsEventData
import java.util.UUID

/**
 * Created by Tal Mirmelshtein on 27/11/2024
 * Copyright: SAP LTD.
 *
 * Immutable state object for ScreenSet screen.
 * This state is managed by ScreenSetViewModel and observed by ScreenSetView.
 * 
 * Note: Navigation events are handled separately via SharedFlow to ensure
 * they are true one-time events that don't accidentally retrigger.
 */

@Immutable
data class ScreenSetState(
    val isLoading: Boolean = true,
    val isInitialized: Boolean = false,
    val error: String? = null,
    val screenSetUrl: String? = null,
    val webViewKey: String = UUID.randomUUID().toString(),
    val lastEvent: ScreenSetEvent? = null
)

/**
 * Navigation events for ScreenSet screen.
 * These are one-time events emitted via SharedFlow.
 * UI collects these events and handles navigation accordingly.
 */
@Immutable
sealed class ScreenSetNavigationEvent {
    data object NavigateBack : ScreenSetNavigationEvent()
    data object NavigateToMyProfile : ScreenSetNavigationEvent()
    data class NavigateToRoute(val route: String) : ScreenSetNavigationEvent()
}

/**
 * ScreenSet events that can be exposed to the UI layer if needed.
 * These represent significant events from the WebView/ScreenSet lifecycle.
 */
@Immutable
sealed class ScreenSetEvent {
    data class OnLoad(val eventData: ScreenSetsEventData) : ScreenSetEvent()
    data class OnHide(val eventData: ScreenSetsEventData) : ScreenSetEvent()
    data class OnLogin(val eventData: ScreenSetsEventData) : ScreenSetEvent()
    data class OnLogout(val eventData: ScreenSetsEventData) : ScreenSetEvent()
    data class OnCanceled(val eventData: ScreenSetsEventData) : ScreenSetEvent()
    data class OnError(val error: ScreenSetsError) : ScreenSetEvent()
}
