package com.sap.cdc.android.sdk.feature.screensets

/**
 * Callback system for handling ScreenSets events.
 * 
 * Provides event handlers for web-based authentication UI lifecycle, form interactions,
 * and authentication events. Supports multiple callbacks per event type through chaining.
 * 
 * ## Usage
 * ```kotlin
 * val callbacks = ScreenSetsCallbacks().apply {
 *     onLoad = { event -> 
 *         println("ScreenSet loaded: ${event.screenSetId}")
 *     }
 *     
 *     onLogin = { event -> 
 *         navigateToMainScreen()
 *     }
 *     
 *     onError = { error -> 
 *         showError(error.message)
 *     }
 * }
 * 
 * webBridge.attachCallbacks(callbacks)
 * ```
 * 
 * ## Multiple Callbacks
 * ```kotlin
 * callbacks.apply {
 *     // Primary callback
 *     onLogin = { event -> navigateToMainScreen() }
 *     
 *     // Add side-effect callback
 *     doOnLogin { event -> analytics.track("login") }
 * }
 * ```
 * 
 * ## Event Filtering
 * ```kotlin
 * callbacks.apply {
 *     // Filter by specific events
 *     filterByEventName("login", "logout")
 *     
 *     // Filter by screenset
 *     filterByScreenSetId("Default-RegistrationLogin")
 *     
 *     // Custom filter
 *     filterEvents { event -> event.screenSetId != null }
 * }
 * ```
 * 
 * @see WebBridgeJS
 * @see ScreenSetsEventData
 * @see ScreenSetsError
 */
data class ScreenSetsCallbacks(
    // Callback lists for each event type
    private var _onBeforeScreenLoad: MutableList<(ScreenSetsEventData) -> Unit> = mutableListOf(),
    private var _onLoad: MutableList<(ScreenSetsEventData) -> Unit> = mutableListOf(),
    private var _onAfterScreenLoad: MutableList<(ScreenSetsEventData) -> Unit> = mutableListOf(),
    private var _onHide: MutableList<(ScreenSetsEventData) -> Unit> = mutableListOf(),
    
    // Form interaction callbacks
    private var _onFieldChanged: MutableList<(ScreenSetsEventData) -> Unit> = mutableListOf(),
    private var _onBeforeValidation: MutableList<(ScreenSetsEventData) -> Unit> = mutableListOf(),
    private var _onAfterValidation: MutableList<(ScreenSetsEventData) -> Unit> = mutableListOf(),
    private var _onBeforeSubmit: MutableList<(ScreenSetsEventData) -> Unit> = mutableListOf(),
    private var _onSubmit: MutableList<(ScreenSetsEventData) -> Unit> = mutableListOf(),
    private var _onAfterSubmit: MutableList<(ScreenSetsEventData) -> Unit> = mutableListOf(),
    
    // Authentication callbacks
    private var _onLoginStarted: MutableList<(ScreenSetsEventData) -> Unit> = mutableListOf(),
    private var _onLogin: MutableList<(ScreenSetsEventData) -> Unit> = mutableListOf(),
    private var _onLogout: MutableList<(ScreenSetsEventData) -> Unit> = mutableListOf(),
    private var _onAddConnection: MutableList<(ScreenSetsEventData) -> Unit> = mutableListOf(),
    private var _onRemoveConnection: MutableList<(ScreenSetsEventData) -> Unit> = mutableListOf(),
    private var _onCanceled: MutableList<(ScreenSetsEventData) -> Unit> = mutableListOf(),
    
    // Error handling
    private var _onError: MutableList<(ScreenSetsError) -> Unit> = mutableListOf(),
    
    // Universal event callback
    private var _onAnyEvent: MutableList<(ScreenSetsEventData) -> Unit> = mutableListOf(),
    
    // Event filters
    private var _eventFilters: MutableList<(ScreenSetsEventData) -> Boolean> = mutableListOf()
) {

    // Public property setters - simplified without overrides
    var onBeforeScreenLoad: ((ScreenSetsEventData) -> Unit)?
        get() = if (_onBeforeScreenLoad.isEmpty()) null else { eventData ->
            if (_eventFilters.all { it(eventData) }) {
                _onBeforeScreenLoad.forEach { it(eventData) }
            }
        }
        set(value) { value?.let { _onBeforeScreenLoad.add(it) } }

    var onLoad: ((ScreenSetsEventData) -> Unit)?
        get() = if (_onLoad.isEmpty()) null else { eventData ->
            if (_eventFilters.all { it(eventData) }) {
                _onLoad.forEach { it(eventData) }
            }
        }
        set(value) { value?.let { _onLoad.add(it) } }

    var onAfterScreenLoad: ((ScreenSetsEventData) -> Unit)?
        get() = if (_onAfterScreenLoad.isEmpty()) null else { eventData ->
            if (_eventFilters.all { it(eventData) }) {
                _onAfterScreenLoad.forEach { it(eventData) }
            }
        }
        set(value) { value?.let { _onAfterScreenLoad.add(it) } }

    var onHide: ((ScreenSetsEventData) -> Unit)?
        get() = if (_onHide.isEmpty()) null else { eventData ->
            if (_eventFilters.all { it(eventData) }) {
                _onHide.forEach { it(eventData) }
            }
        }
        set(value) { value?.let { _onHide.add(it) } }

    var onFieldChanged: ((ScreenSetsEventData) -> Unit)?
        get() = if (_onFieldChanged.isEmpty()) null else { eventData ->
            if (_eventFilters.all { it(eventData) }) {
                _onFieldChanged.forEach { it(eventData) }
            }
        }
        set(value) { value?.let { _onFieldChanged.add(it) } }

    var onBeforeValidation: ((ScreenSetsEventData) -> Unit)?
        get() = if (_onBeforeValidation.isEmpty()) null else { eventData ->
            if (_eventFilters.all { it(eventData) }) {
                _onBeforeValidation.forEach { it(eventData) }
            }
        }
        set(value) { value?.let { _onBeforeValidation.add(it) } }

    var onAfterValidation: ((ScreenSetsEventData) -> Unit)?
        get() = if (_onAfterValidation.isEmpty()) null else { eventData ->
            if (_eventFilters.all { it(eventData) }) {
                _onAfterValidation.forEach { it(eventData) }
            }
        }
        set(value) { value?.let { _onAfterValidation.add(it) } }

    var onBeforeSubmit: ((ScreenSetsEventData) -> Unit)?
        get() = if (_onBeforeSubmit.isEmpty()) null else { eventData ->
            if (_eventFilters.all { it(eventData) }) {
                _onBeforeSubmit.forEach { it(eventData) }
            }
        }
        set(value) { value?.let { _onBeforeSubmit.add(it) } }

    var onSubmit: ((ScreenSetsEventData) -> Unit)?
        get() = if (_onSubmit.isEmpty()) null else { eventData ->
            if (_eventFilters.all { it(eventData) }) {
                _onSubmit.forEach { it(eventData) }
            }
        }
        set(value) { value?.let { _onSubmit.add(it) } }

    var onAfterSubmit: ((ScreenSetsEventData) -> Unit)?
        get() = if (_onAfterSubmit.isEmpty()) null else { eventData ->
            if (_eventFilters.all { it(eventData) }) {
                _onAfterSubmit.forEach { it(eventData) }
            }
        }
        set(value) { value?.let { _onAfterSubmit.add(it) } }

    var onLoginStarted: ((ScreenSetsEventData) -> Unit)?
        get() = if (_onLoginStarted.isEmpty()) null else { eventData ->
            if (_eventFilters.all { it(eventData) }) {
                _onLoginStarted.forEach { it(eventData) }
            }
        }
        set(value) { value?.let { _onLoginStarted.add(it) } }

    var onLogin: ((ScreenSetsEventData) -> Unit)?
        get() = if (_onLogin.isEmpty()) null else { eventData ->
            if (_eventFilters.all { it(eventData) }) {
                _onLogin.forEach { it(eventData) }
            }
        }
        set(value) { value?.let { _onLogin.add(it) } }

    var onLogout: ((ScreenSetsEventData) -> Unit)?
        get() = if (_onLogout.isEmpty()) null else { eventData ->
            if (_eventFilters.all { it(eventData) }) {
                _onLogout.forEach { it(eventData) }
            }
        }
        set(value) { value?.let { _onLogout.add(it) } }

    var onAddConnection: ((ScreenSetsEventData) -> Unit)?
        get() = if (_onAddConnection.isEmpty()) null else { eventData ->
            if (_eventFilters.all { it(eventData) }) {
                _onAddConnection.forEach { it(eventData) }
            }
        }
        set(value) { value?.let { _onAddConnection.add(it) } }

    var onRemoveConnection: ((ScreenSetsEventData) -> Unit)?
        get() = if (_onRemoveConnection.isEmpty()) null else { eventData ->
            if (_eventFilters.all { it(eventData) }) {
                _onRemoveConnection.forEach { it(eventData) }
            }
        }
        set(value) { value?.let { _onRemoveConnection.add(it) } }

    var onCanceled: ((ScreenSetsEventData) -> Unit)?
        get() = if (_onCanceled.isEmpty()) null else { eventData ->
            if (_eventFilters.all { it(eventData) }) {
                _onCanceled.forEach { it(eventData) }
            }
        }
        set(value) { value?.let { _onCanceled.add(it) } }

    var onError: ((ScreenSetsError) -> Unit)?
        get() = if (_onError.isEmpty()) null else { error ->
            _onError.forEach { it(error) }
        }
        set(value) { value?.let { _onError.add(it) } }

    var onAnyEvent: ((ScreenSetsEventData) -> Unit)?
        get() = if (_onAnyEvent.isEmpty()) null else { eventData ->
            if (_eventFilters.all { it(eventData) }) {
                _onAnyEvent.forEach { it(eventData) }
            }
        }
        set(value) { value?.let { _onAnyEvent.add(it) } }

    // Chaining methods for adding multiple callbacks
    fun doOnBeforeScreenLoad(callback: (ScreenSetsEventData) -> Unit) = apply {
        _onBeforeScreenLoad.add(0, callback)
    }

    fun doOnLoad(callback: (ScreenSetsEventData) -> Unit) = apply {
        _onLoad.add(0, callback)
    }

    fun doOnAfterScreenLoad(callback: (ScreenSetsEventData) -> Unit) = apply {
        _onAfterScreenLoad.add(0, callback)
    }

    fun doOnHide(callback: (ScreenSetsEventData) -> Unit) = apply {
        _onHide.add(0, callback)
    }

    fun doOnFieldChanged(callback: (ScreenSetsEventData) -> Unit) = apply {
        _onFieldChanged.add(0, callback)
    }

    fun doOnBeforeValidation(callback: (ScreenSetsEventData) -> Unit) = apply {
        _onBeforeValidation.add(0, callback)
    }

    fun doOnAfterValidation(callback: (ScreenSetsEventData) -> Unit) = apply {
        _onAfterValidation.add(0, callback)
    }

    fun doOnBeforeSubmit(callback: (ScreenSetsEventData) -> Unit) = apply {
        _onBeforeSubmit.add(0, callback)
    }

    fun doOnSubmit(callback: (ScreenSetsEventData) -> Unit) = apply {
        _onSubmit.add(0, callback)
    }

    fun doOnAfterSubmit(callback: (ScreenSetsEventData) -> Unit) = apply {
        _onAfterSubmit.add(0, callback)
    }

    fun doOnLoginStarted(callback: (ScreenSetsEventData) -> Unit) = apply {
        _onLoginStarted.add(0, callback)
    }

    fun doOnLogin(callback: (ScreenSetsEventData) -> Unit) = apply {
        _onLogin.add(0, callback)
    }

    fun doOnLogout(callback: (ScreenSetsEventData) -> Unit) = apply {
        _onLogout.add(0, callback)
    }

    fun doOnAddConnection(callback: (ScreenSetsEventData) -> Unit) = apply {
        _onAddConnection.add(0, callback)
    }

    fun doOnRemoveConnection(callback: (ScreenSetsEventData) -> Unit) = apply {
        _onRemoveConnection.add(0, callback)
    }

    fun doOnCanceled(callback: (ScreenSetsEventData) -> Unit) = apply {
        _onCanceled.add(0, callback)
    }

    fun doOnError(callback: (ScreenSetsError) -> Unit) = apply {
        _onError.add(0, callback)
    }

    fun doOnAnyEvent(callback: (ScreenSetsEventData) -> Unit) = apply {
        _onAnyEvent.add(0, callback)
    }

    // Event filtering methods
    fun filterEvents(predicate: (ScreenSetsEventData) -> Boolean) = apply {
        _eventFilters.add(predicate)
    }

    fun filterByEventName(vararg eventNames: String) = apply {
        filterEvents { event -> event.eventName in eventNames }
    }

    fun filterByScreenSetId(screenSetId: String) = apply {
        filterEvents { event -> event.screenSetId == screenSetId }
    }
}
