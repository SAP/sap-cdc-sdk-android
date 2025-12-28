package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import android.util.Log
import android.webkit.WebView
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.screensets.ScreenSetUrlBuilder
import com.sap.cdc.android.sdk.feature.screensets.ScreenSetsError
import com.sap.cdc.android.sdk.feature.screensets.ScreenSetsEventData
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJS
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJSConfig
import com.sap.cdc.android.sdk.feature.screensets.onScreenSetEvents
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.state.ScreenSetEvent
import com.sap.cdc.bitsnbytes.ui.state.ScreenSetNavigationEvent
import com.sap.cdc.bitsnbytes.ui.state.ScreenSetState
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Created by Tal Mirmelshtein on 20/06/2024
 * Copyright: SAP LTD.
 */
interface IViewModelScreenSet {
    val state: StateFlow<ScreenSetState>
    val navigationEvents: SharedFlow<ScreenSetNavigationEvent>

    fun initializeScreenSet(screenSet: String, startScreen: String)
    fun setupWebBridge(webView: WebView)
    fun handleWebViewDisposal(webView: WebView)
    fun onWebViewPageStarted()
    fun onWebViewPageFinished()
}

// Mocked preview class for ScreenSetViewModel
class ScreenSetViewModelPreview : IViewModelScreenSet {
    override val state: StateFlow<ScreenSetState> = MutableStateFlow(ScreenSetState()).asStateFlow()
    override val navigationEvents: SharedFlow<ScreenSetNavigationEvent> =
        MutableSharedFlow<ScreenSetNavigationEvent>().asSharedFlow()

    override fun initializeScreenSet(screenSet: String, startScreen: String) {}
    override fun setupWebBridge(webView: WebView) {}
    override fun handleWebViewDisposal(webView: WebView) {}
    override fun onWebViewPageStarted() {}
    override fun onWebViewPageFinished() {}
}

/**
 * Defines the priority hierarchy for ScreenSet navigation events.
 *
 * ## Problem Being Solved
 *
 * The CDC WebBridge fires multiple events during a single user flow. For example, when a user
 * successfully logs in, the WebBridge fires BOTH:
 * - `onLogin` event (authentication succeeded)
 * - `onHide` event (screenset is closing)
 *
 * If both events trigger navigation, the app will navigate twice:
 * 1. Navigate to MyProfile (from onLogin) ✅
 * 2. Navigate back (from onHide) ❌ Wrong!
 *
 * ## The Solution: Event Priority
 *
 * This enum establishes a priority system where higher-priority events take precedence over
 * lower-priority ones. Once a high-priority event triggers navigation, subsequent lower-priority
 * events are suppressed.
 *
 * ## Priority Levels (Highest to Lowest)
 *
 * 1. **LOGIN** - User successfully authenticated
 *    - Should navigate to authenticated content (e.g., MyProfile)
 *    - Highest priority because it represents successful completion
 *
 * 2. **LOGOUT** - User logged out
 *    - Should navigate back to welcome/login screen
 *    - High priority as it's a deliberate user action
 *
 * 3. **CANCELED** - User explicitly canceled the flow
 *    - Should navigate back to previous screen
 *    - Medium priority as it's a deliberate cancellation
 *
 * 4. **HIDE** - Generic screenset closure
 *    - Should navigate back to previous screen
 *    - Lowest priority because it fires for ANY screenset closure,
 *      including after LOGIN, LOGOUT, or CANCELED events
 *
 * ## Why HIDE Has Lowest Priority
 *
 * The `onHide` event is a **generic lifecycle event** that fires whenever a screenset closes,
 * regardless of the reason. It will fire:
 * - After successful login (along with onLogin)
 * - After logout (along with onLogout)
 * - After user cancels (along with onCanceled)
 * - When user manually closes screenset
 *
 * Because `onHide` doesn't distinguish between these cases, it has the lowest priority.
 * If a more specific event (LOGIN, LOGOUT, CANCELED) has already triggered navigation,
 * the subsequent `onHide` event should be suppressed.
 *
 * ## Usage Example
 *
 * ```kotlin
 * // Scenario: User completes login
 * // 1. onLogin fires → priority = LOGIN → navigates to MyProfile
 * // 2. onHide fires 100ms later → priority = HIDE < LOGIN → SUPPRESSED ✅
 *
 * // Scenario: User manually closes screenset
 * // 1. onHide fires → priority = HIDE → navigates back ✅
 * ```
 *
 * @property priority The numeric priority value (higher = more important)
 */
private enum class ScreenSetNavigationPriority(val priority: Int) {
    /**
     * Login event - highest priority.
     * Triggered when user successfully authenticates.
     */
    LOGIN(100),

    /**
     * Logout event - high priority.
     * Triggered when user logs out.
     */
    LOGOUT(80),

    /**
     * Canceled event - medium priority.
     * Triggered when user explicitly cancels the flow.
     */
    CANCELED(60),

    /**
     * Hide event - lowest priority.
     * Triggered when screenset closes for ANY reason (including after LOGIN, LOGOUT, CANCELED).
     * Should only trigger navigation if no higher-priority event has occurred.
     */
    HIDE(40);

    /**
     * Check if this priority is higher than another.
     * Used to determine if an event should suppress a subsequent event.
     */
    fun isHigherThan(other: ScreenSetNavigationPriority): Boolean {
        return this.priority > other.priority
    }
}

class ScreenSetViewModel(
    private val context: Context,
    val flowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), IViewModelScreenSet {

    companion object {
        private const val LOG_TAG = "ScreenSetViewModel"
    }

    private val _state = MutableStateFlow(ScreenSetState())
    override val state: StateFlow<ScreenSetState> = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<ScreenSetNavigationEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    override val navigationEvents: SharedFlow<ScreenSetNavigationEvent> = _navigationEvents.asSharedFlow()

    // WebBridgeJS instance managed by ViewModel
    private var webBridgeJS: WebBridgeJS? = null

    /**
     * Tracks the highest priority navigation event that has occurred during the current
     * screenset lifecycle. This prevents lower-priority events from triggering duplicate
     * or conflicting navigation.
     *
     * ## Why This Is Needed
     *
     * CDC WebBridge fires multiple events during a single flow. For example:
     * - User logs in → fires `onLogin` (priority 100) AND `onHide` (priority 40)
     * - User cancels → fires `onCanceled` (priority 60) AND `onHide` (priority 40)
     *
     * Without priority tracking, both events would trigger navigation, causing:
     * 1. Navigate to correct destination (from high-priority event) ✅
     * 2. Navigate back/away (from low-priority onHide) ❌ WRONG!
     *
     * ## How It Works
     *
     * 1. Reset to null when screenset initializes
     * 2. Set to event priority when first navigation event occurs
     * 3. Subsequent events check: if (myPriority > currentPriority) → navigate, else suppress
     *
     * ## Example Flow
     *
     * ```
     * initializeScreenSet() → currentNavigationPriority = null
     * onLogin fires → priority(100) > null → navigate to MyProfile, set to LOGIN
     * onHide fires → priority(40) < LOGIN(100) → SUPPRESSED ✅
     * ```
     *
     * @see ScreenSetNavigationPriority for priority definitions and rationale
     */
    private var currentNavigationPriority: ScreenSetNavigationPriority? = null

    /**
     * Determines if a navigation event should be processed based on priority.
     *
     * This method implements the priority-based suppression logic to prevent duplicate
     * navigation from multiple WebBridge events.
     *
     * @param eventPriority The priority of the event attempting to navigate
     * @return true if navigation should proceed, false if it should be suppressed
     */
    private fun shouldNavigate(eventPriority: ScreenSetNavigationPriority): Boolean {
        val current = currentNavigationPriority

        return if (current == null) {
            // First navigation event - always proceed
            currentNavigationPriority = eventPriority
            Log.d(LOG_TAG, "First navigation event: $eventPriority (priority ${eventPriority.priority}) - PROCEEDING")
            true
        } else if (eventPriority.isHigherThan(current)) {
            // Higher priority event - override previous
            Log.d(
                LOG_TAG,
                "Higher priority navigation: $eventPriority (${eventPriority.priority}) > $current (${current.priority}) - PROCEEDING"
            )
            currentNavigationPriority = eventPriority
            true
        } else {
            // Lower or equal priority - suppress
            Log.d(
                LOG_TAG,
                "Lower priority navigation: $eventPriority (${eventPriority.priority}) <= $current (${current.priority}) - SUPPRESSED"
            )
            false
        }
    }

    /**
     * Initialize ScreenSet with given parameters and build the URL
     */
    override fun initializeScreenSet(screenSet: String, startScreen: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }

                // Reset navigation priority for new screenset lifecycle
                currentNavigationPriority = null
                Log.d(LOG_TAG, "Navigation priority reset for new screenset: $screenSet - $startScreen")

                // Create screen set request parameters
                val params = mutableMapOf<String, Any>(
                    "screenSet" to screenSet,
                    "startScreen" to startScreen
                )

                // Build URL
                val screenSetUrl = ScreenSetUrlBuilder.Builder()
                    .apiKey(flowDelegate.siteConfig.apiKey)
                    .domain(flowDelegate.siteConfig.domain)
                    .params(params)
                    .build()

                // Initialize WebBridgeJS
                webBridgeJS = flowDelegate.getWebBridge()

                // Add configuration
                webBridgeJS?.addConfig(
                    WebBridgeJSConfig.Builder().obfuscate(true).build()
                )

                _state.update {
                    it.copy(
                        screenSetUrl = screenSetUrl,
                        isLoading = false
                    )
                }

                Log.d(LOG_TAG, "ScreenSet initialized: $screenSet - $startScreen")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error initializing ScreenSet", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to initialize: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Setup WebBridge with the given WebView and attach event handlers
     */
    override fun setupWebBridge(webView: WebView) {
        try {
            val bridge = webBridgeJS ?: run {
                Log.e(LOG_TAG, "WebBridgeJS is null during setup")
                _state.update { it.copy(error = "WebBridge not initialized") }
                return
            }

            // Attach bridge to WebView with ViewModel scope
            bridge.attachBridgeTo(webView)

            // Set native social providers
            bridge.setNativeSocialProviders(flowDelegate.getAuthenticatorMap())

            // Setup event handlers
            bridge.onScreenSetEvents {
                onLoad = { eventData ->
                    handleOnLoad(eventData)
                }

                onHide = { eventData ->
                    handleOnHide(eventData)
                }

                onCanceled = { eventData ->
                    handleOnCanceled(eventData)
                }

                onLogin = { eventData ->
                    handleOnLogin(eventData)
                }

                onLogout = { eventData ->
                    handleOnLogout(eventData)
                }

                onError = { error ->
                    handleOnError(error)
                }
            }

            // Load the screen set URL
            val url = _state.value.screenSetUrl
            if (url != null) {
                bridge.load(webView, url)
                _state.update { it.copy(isInitialized = true) }
                Log.d(LOG_TAG, "WebBridge setup completed and URL loaded")
            } else {
                Log.e(LOG_TAG, "ScreenSet URL is null, cannot load")
                _state.update { it.copy(error = "ScreenSet URL not available") }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error setting up WebBridge", e)
            _state.update { it.copy(error = "Failed to setup WebView: ${e.message}") }
        }
    }

    /**
     * Handle WebView disposal and cleanup
     */
    override fun handleWebViewDisposal(webView: WebView) {
        viewModelScope.launch {
            try {
                webBridgeJS?.detachBridgeFrom(webView)
                webBridgeJS = null
                Log.d(LOG_TAG, "WebView disposed successfully")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error during WebView disposal", e)
            }
        }
    }

    // Event handlers - All run on viewModelScope to ensure proper lifecycle management
    private fun handleOnLoad(eventData: ScreenSetsEventData) {
        viewModelScope.launch {
            Log.d(LOG_TAG, "Screen set loaded: ${eventData.screenSetId}")
            _state.update { it.copy(lastEvent = ScreenSetEvent.OnLoad(eventData)) }
        }
    }

    private fun handleOnHide(eventData: ScreenSetsEventData) {
        viewModelScope.launch {
            Log.d(LOG_TAG, "Hide event received")
            _state.update { it.copy(lastEvent = ScreenSetEvent.OnHide(eventData)) }

            // Check priority before navigating
            if (shouldNavigate(ScreenSetNavigationPriority.HIDE)) {
                Log.d(LOG_TAG, "HIDE event - navigating back")

                // Log navigation event
                com.sap.cdc.bitsnbytes.navigation.NavigationDebugLogger.logNavigationEvent(
                    source = "ScreenSetViewModel",
                    event = ScreenSetNavigationEvent.NavigateBack
                )

                _navigationEvents.emit(ScreenSetNavigationEvent.NavigateBack)
            }
        }
    }

    private fun handleOnCanceled(eventData: ScreenSetsEventData) {
        viewModelScope.launch {
            Log.d(LOG_TAG, "Canceled event received")
            _state.update {
                it.copy(
                    lastEvent = ScreenSetEvent.OnCanceled(eventData),
                    error = "Operation canceled"
                )
            }

            // Check priority before navigating
            if (shouldNavigate(ScreenSetNavigationPriority.CANCELED)) {
                Log.d(LOG_TAG, "CANCELED event - navigating back")

                // Log navigation event
                com.sap.cdc.bitsnbytes.navigation.NavigationDebugLogger.logNavigationEvent(
                    source = "ScreenSetViewModel",
                    event = ScreenSetNavigationEvent.NavigateBack
                )

                _navigationEvents.emit(ScreenSetNavigationEvent.NavigateBack)
            }
        }
    }

    private fun handleOnLogin(eventData: ScreenSetsEventData) {
        viewModelScope.launch {
            Log.d(LOG_TAG, "Login event received")
            _state.update { it.copy(lastEvent = ScreenSetEvent.OnLogin(eventData)) }

            // Check priority before navigating
            if (shouldNavigate(ScreenSetNavigationPriority.LOGIN)) {
                Log.d(LOG_TAG, "LOGIN event - navigating to MyProfile")

                // Log navigation event
                com.sap.cdc.bitsnbytes.navigation.NavigationDebugLogger.logNavigationEvent(
                    source = "ScreenSetViewModel",
                    event = ScreenSetNavigationEvent.NavigateToMyProfile
                )

                _navigationEvents.emit(ScreenSetNavigationEvent.NavigateToMyProfile)
            }
        }
    }

    private fun handleOnLogout(eventData: ScreenSetsEventData) {
        viewModelScope.launch {
            Log.d(LOG_TAG, "Logout event received")
            _state.update { it.copy(lastEvent = ScreenSetEvent.OnLogout(eventData)) }

            // Check priority before navigating
            if (shouldNavigate(ScreenSetNavigationPriority.LOGOUT)) {
                Log.d(LOG_TAG, "LOGOUT event - navigating back")

                // Log navigation event
                com.sap.cdc.bitsnbytes.navigation.NavigationDebugLogger.logNavigationEvent(
                    source = "ScreenSetViewModel",
                    event = ScreenSetNavigationEvent.NavigateBack
                )

                _navigationEvents.emit(ScreenSetNavigationEvent.NavigateBack)
            }
        }
    }

    private fun handleOnError(error: ScreenSetsError) {
        viewModelScope.launch {
            Log.e(LOG_TAG, "ScreenSet error: ${error.message}")
            _state.update {
                it.copy(
                    lastEvent = ScreenSetEvent.OnError(error),
                    // NO NEED FOR "error = error.message"
                    // Error display is handled within the screenset itself so there is no need to update the UI error message
                    //  If additional UI intervention is  required, create your own implementation
                )
            }
        }
    }

    /**
     * Called when WebView starts loading a page
     */
    override fun onWebViewPageStarted() {
        viewModelScope.launch {
            Log.d(LOG_TAG, "WebView page started loading")
            _state.update { it.copy(isLoading = true) }
        }
    }

    /**
     * Called when WebView finishes loading a page
     */
    override fun onWebViewPageFinished() {
        viewModelScope.launch {
            Log.d(LOG_TAG, "WebView page finished loading")
            _state.update { it.copy(isLoading = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webBridgeJS = null
        Log.d(LOG_TAG, "ViewModel cleared")
    }
}
