[![REUSE status](https://api.reuse.software/badge/github.com/SAP/sap-customer-data-cloud-sdk-for-android)](https://api.reuse.software/info/github.com/SAP/sap-customer-data-cloud-sdk-for-android)

# Description

SAP Customer Data Cloud SDK for Android is a comprehensive solution for integrating SAP Customer Data Cloud services into Android applications. The SDK provides a modern, type-safe Kotlin DSL for authentication flows, session management, and user account operations with powerful callback chaining and transformation capabilities.

**Key Features:**
- Modern Kotlin DSL with coroutine support
- Advanced callback system with side-effects and overrides
- Biometric authentication integration
- Web Screen-Sets support with JavaScript bridge
- Social provider authentication (Google, Facebook, WeChat, Line)
- Two-factor authentication and OTP support
- Passkey authentication
- Push notifications for authentication

# Requirements

- **Android API Level:** 24+ (Android 7.0)
- **Kotlin:** 1.9+
- **Java:** 11+
- **Gradle:** 8.0+

# Implementation

Add the SDK dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.sap.oss.ciam-android-sdk:ciam-android-sdk:0.3.0")
}
```

The library is available on [MavenCentral](https://search.maven.org/artifact/com.sap.oss.ciam-android-sdk/ciam-android-sdk).

# SDK Setup

## SiteConfig

The `SiteConfig` class manages site-specific configuration data required for SDK initialization. It automatically retrieves configuration parameters from your app's `strings.xml` file.

```kotlin
// Initialize the configuration object
val siteConfig = SiteConfig(context)
```

**Required configuration in `strings.xml`:**

```xml
<!-- Mandatory -->
<string name="com.sap.ciam.apikey">YOUR_API_KEY_HERE</string>

<!-- Optional - defaults to "us1.gigya.com" if not specified -->
<string name="com.sap.ciam.domain">YOUR_API_DOMAIN_HERE</string>

<!-- Optional -->
<string name="com.sap.ciam.cname">YOUR_CNAME_HERE</string>
```

**Multi-flavor Support:**
The SDK supports multiple build flavors by placing flavor-specific `strings.xml` files in corresponding directories (e.g., `src/debug/res/values/strings.xml`, `src/release/res/values/strings.xml`).

## AuthenticationService

The `AuthenticationService` is the primary interface for all authentication operations. Initialize it with your `SiteConfig`:

```kotlin
// Initialize authentication service
val authenticationService = AuthenticationService(siteConfig)
```

**Core Methods:**
- `authenticate()` - Handle authentication flows (login, register, social, etc.)
- `account()` - Manage user account data (get/set profile information)
- `session()` - Manage user sessions and security levels

# Kotlin DSL AuthCallbacks

The SDK provides a powerful Kotlin DSL for authentication callbacks with support for response transformation, side-effects, and multiple event handlers.

## Basic Callback Usage

```kotlin
suspend fun login(email: String, password: String) {
    val credentials = Credentials(email, password)
    
    authenticationService.authenticate().login()
        .credentials(credentials) {
            onSuccess = { authSuccess ->
                // Handle successful login
                navigateToMainScreen()
            }
            
            onError = { authError ->
                // Handle login error
                showError(authError.message)
            }
            
            onTwoFactorRequired = { context ->
                // Handle TFA requirement
                navigateToTwoFactorScreen(context)
            }
        }
}
```

## Advanced DSL Features

### Callback Registration Patterns

There are two distinct patterns for registering callbacks, each with different registration order requirements:

#### Pattern 1: Side Effect Pattern - Register User Callbacks FIRST

When wrapping SDK calls to add side effects (e.g., state management, analytics), register the user's callbacks **FIRST**, then add your side effects:

```kotlin
// ✅ CORRECT: Side Effect Pattern
suspend fun login(credentials: Credentials, authCallbacks: AuthCallbacks.() -> Unit) {
    authenticationService.authenticate().login()
        .credentials(credentials) {
            // 1. Register user's callbacks FIRST
            authCallbacks()
            
            // 2. Add your side effects AFTER
            doOnSuccess { authSuccess ->
                updateLocalState(authSuccess)
                analytics.track("login_success")
            }
        }
}
```

**Why this order?** Your side effects execute before the user's callbacks, allowing you to prepare state or log events before their code runs.

#### Pattern 2: Override Pattern - Register User Callbacks AFTER

When transforming responses with overrides, set up the override **FIRST**, then register user callbacks **AFTER**:

```kotlin
// ✅ CORRECT: Override Pattern
suspend fun linkToProvider(
    linkingContext: LinkingContext,
    authCallbacks: AuthCallbacks.() -> Unit
) {
    signIn(parameters) {
        // 1. Set up override transformation FIRST
        doOnAnyAndOverride { authResult ->
            when (authResult) {
                is AuthResult.Success -> transformSuccess(authResult)
                else -> authResult
            }
        }
        
        // 2. Register user callbacks AFTER override
        authCallbacks()
    }
}
```

**Why this order?** The override transforms the data first, then the user's callbacks receive the transformed data.

#### Pattern Comparison

Both patterns use the **same DSL syntax** - only the **order differs**:

```kotlin
// Pattern 1: Side Effects (callbacks FIRST, side effects AFTER)
credentials(credentials) {
    authCallbacks()           // User callbacks FIRST
    doOnSuccess { /* side */ } // Side effects AFTER
}

// Pattern 2: Overrides (override FIRST, callbacks AFTER)  
signIn(parameters) {
    doOnAnyAndOverride { /* transform */ }  // Override FIRST
    authCallbacks()                         // User callbacks AFTER
}
```

#### Common Mistakes

```kotlin
// ❌ WRONG: Side Effect Pattern with callbacks in wrong order
authenticationService.authenticate().login()
    .credentials(credentials) {
        doOnSuccess { /* side effect */ }
        authCallbacks()  // Side effects run before user code - incorrect
    }

// ❌ WRONG: Override Pattern with callbacks in wrong order
signIn(parameters) {
    authCallbacks()  // User callbacks registered first
    doOnAnyAndOverride { /* transform */ }  // Override after - won't transform
}
```

### Side Effects vs. Overrides: Understanding the Difference

The SDK provides two distinct callback mechanisms with different purposes and execution behaviors:

#### Side Effects with `doOn*` Methods

**Purpose:** Execute additional logic **alongside** your main callbacks without modifying the response data.

**When to use:** Logging, analytics, state management, notifications - any action that should happen in addition to your main logic.

**Registration order:** User callbacks FIRST with `authCallbacks()`, then side effects AFTER.

**Execution order:** Side effects execute **before** main callbacks, but both receive the **same original data**.

```kotlin
// Wrapped SDK call with side effects
suspend fun login(credentials: Credentials, authCallbacks: AuthCallbacks.() -> Unit) {
    authenticationService.authenticate().login()
        .credentials(credentials) {
            // 1. Register user's callbacks FIRST
            authCallbacks()
            
            // 2. Add side effect AFTER registration
            doOnSuccess { authSuccess ->
                analytics.track("login_success", authSuccess.userData)
                updateLocalCache(authSuccess)
                // This doesn't change what the main callback receives
            }
        }
}

// Usage - user's callback receives original data
login(credentials) {
    onSuccess = { authSuccess ->
        // Receives the same original authSuccess as the side effect
        navigateToMainScreen()
    }
}
```

#### Response Transformation with Override Methods

**Purpose:** **Transform or modify** the response data before it reaches your main callbacks.

**When to use:** Data transformation, localization, enrichment, filtering - when you need to change what your main callbacks receive.

**Registration order:** Override FIRST with `doOnSuccessAndOverride {}`, then user callbacks AFTER with `authCallbacks()`.

**Execution order:** Overrides execute **first** and transform the data, then main callbacks receive the **transformed data**.

```kotlin
// Wrapped SDK call with override
suspend fun linkToProvider(
    linkingContext: LinkingContext,
    authCallbacks: AuthCallbacks.() -> Unit
) {
    signIn(parameters) {
        // 1. Set up override transformation FIRST
        doOnSuccessAndOverride { authSuccess ->
            // Transform the response
            authSuccess.copy(
                userData = authSuccess.userData + mapOf(
                    "loginTime" to System.currentTimeMillis(),
                    "deviceInfo" to getDeviceInfo()
                )
            )
        }
        
        // 2. Register user callbacks AFTER override
        authCallbacks()
    }
}

// Usage - user's callback receives transformed data
linkToProvider(linkingContext) {
    onSuccess = { transformedSuccess ->
        // This receives the enhanced data with loginTime and deviceInfo
        handleLogin(transformedSuccess)
    }
}
```

### Execution Flow Comparison

**With Side Effects:**
```
1. API Response arrives
2. doOnSuccess executes (original data)
3. onSuccess executes (same original data)
```

**With Overrides:**
```
1. API Response arrives
2. doOnSuccessAndOverride executes and transforms data
3. onSuccess executes (transformed data)
```

**Combined Usage:**
```kotlin
authenticationService.authenticate().login()
    .credentials(credentials) {
        // 1. Override transforms the data first
        doOnSuccessAndOverride { authSuccess ->
            authSuccess.copy(userData = authSuccess.userData + ("enhanced" to true))
        }
        
        // 2. Side effect executes with transformed data
        doOnSuccess { transformedSuccess ->
            analytics.track("login", transformedSuccess.userData) // Has "enhanced" = true
        }
        
        // 3. Main callback receives the same transformed data
        onSuccess = { transformedSuccess ->
            handleLogin(transformedSuccess) // Also has "enhanced" = true
        }
    }
```

### Universal Override for All Callback Types

Handle any callback type with a single universal override that can **transform the result type**. Since this is an override, it must be registered **FIRST**, then user callbacks **AFTER**.

**Key Feature:** The universal override can change the result type (e.g., Error → Success), and the SDK's internal callback router will invoke only the callbacks matching the **final transformed type**.

```kotlin
// Wrapped SDK call with universal override for error recovery
suspend fun loginWithErrorRecovery(
    credentials: Credentials,
    authCallbacks: AuthCallbacks.() -> Unit
) {
    authenticationService.authenticate().login()
        .credentials(credentials) {
            // 1. Universal override FIRST - can transform ANY result type to ANY other type
            doOnAnyAndOverride { authResult ->
                when (authResult) {
                    is AuthResult.Error -> {
                        // Example: Attempt recovery via alternative endpoint
                        val recoveryAttempt = tryAlternativeAuthEndpoint(credentials)
                        if (recoveryAttempt != null) {
                            // Transform Error → Success
                            // Only onSuccess callback will execute (not onError)
                            AuthResult.Success(recoveryAttempt)
                        } else {
                            // Keep as Error
                            // Only onError callback will execute
                            authResult
                        }
                    }
                    is AuthResult.Success -> {
                        // Enrich success data
                        val enriched = authResult.authSuccess.copy(
                            userData = authResult.authSuccess.userData + 
                                ("loginTime" to System.currentTimeMillis())
                        )
                        AuthResult.Success(enriched)
                    }
                    // Pass through other types unchanged
                    else -> authResult
                }
            }
            
            // 2. User callbacks AFTER override
            authCallbacks()
        }
}

// Usage - callbacks receive the TRANSFORMED result
loginWithErrorRecovery(credentials) {
    onSuccess = { authSuccess ->
        // Executes if original succeeded OR if error was recovered
        navigateToMainScreen()
    }
    onError = { authError ->
        // Only executes if recovery also failed
        showError(authError.message)
    }
}
```

**How the callback router works:**

1. **Universal override executes first** and can transform the result type
2. **SDK's internal router** evaluates the **final result type** after transformation
3. **Only matching callbacks execute** based on the final type:
   - If final result is `AuthResult.Success` → only `onSuccess` executes
   - If final result is `AuthResult.Error` → only `onError` executes
   - If final result is `AuthResult.TwoFactorRequired` → only `onTwoFactorRequired` executes

This routing mechanism ensures type safety and prevents callbacks from executing for the wrong result type.

### Context Update Callbacks

Context update callbacks are specifically designed to handle **interrupted authentication flows** that require additional steps to complete. These flows are "interrupted" because they cannot complete immediately and need user intervention or additional data.

**Purpose:** Handle multi-step authentication scenarios where the SDK provides enriched context data to help you resolve the interruption and continue the flow.

**Common interrupted flows:**
- **Account Linking:** When a social login conflicts with an existing account
- **Two-Factor Authentication:** When TFA is required for login/registration
- **Registration Missing Fields:** When required profile fields are missing during registration

**How it works:**
1. Initial authentication attempt fails with a specific interruption error
2. SDK enriches the context with additional data needed for resolution
3. Context update callback receives the enriched data
4. You use this data to present appropriate UI and continue the flow
5. Flow completion triggers normal success/error callbacks

```kotlin
authenticationService.authenticate().login()
    .credentials(credentials) {
        // Handle the interruption - basic context
        onTwoFactorRequired = { context ->
            // Initial interruption - minimal context data
            navigateToTwoFactorScreen(context)
        }
        
        // Handle enriched context - enhanced data for flow completion
        onTwoFactorContextUpdated = { enrichedContext ->
            // SDK-enriched context with additional data:
            // - Available email addresses for TFA
            // - Phone numbers registered for SMS
            // - QR codes for authenticator apps
            // - Tokens needed for flow continuation
            updateTwoFactorUI(enrichedContext)
            
            // Use enriched data to help user complete the flow
            if (enrichedContext.emails?.isNotEmpty() == true) {
                showEmailTwoFactorOption(enrichedContext.emails!!)
            }
            if (enrichedContext.phones?.isNotEmpty() == true) {
                showSMSTwoFactorOption(enrichedContext.phones!!)
            }
        }
    }
```

**Example: Account Linking Flow**
```kotlin
authenticationService.authenticate().provider().signIn(activity, provider) {
    onLinkingRequired = { context ->
        // Basic linking context - account conflict detected
        showAccountLinkingScreen(context)
    }
    
    onLinkingContextUpdated = { enrichedContext ->
        // Enriched context with detailed conflicting account info
        // - Existing account details
        // - Available linking options
        // - Tokens for linking continuation
        updateLinkingUI(enrichedContext.conflictingAccounts)
        
        // Present user with clear linking choices
        showLinkingOptions(enrichedContext)
    }
}
```

These callbacks work **in addition to** standard callbacks, providing you with the detailed information needed to guide users through complex authentication scenarios seamlessly.

## Real-World Example: Complete Authentication Flow

```kotlin
class AuthenticationFlowDelegate(context: Context) {
    private val authenticationService = AuthenticationService(SiteConfig(context))
    
    suspend fun login(credentials: Credentials, authCallbacks: AuthCallbacks.() -> Unit) {
        authenticationService.authenticate().login()
            .credentials(credentials) {
                // Apply user's callbacks first
                authCallbacks()
                
                // Add state management side-effect
                doOnSuccess { authSuccess ->
                    try {
                        val accountData = Json.decodeFromString<AccountEntity>(authSuccess.jsonData)
                        _userAccount.value = accountData
                        _isAuthenticated.value = true
                    } catch (e: Exception) {
                        // Handle parsing errors gracefully
                    }
                }
            }
    }
}

// Usage
authFlowDelegate.login(credentials) {
    onSuccess = { 
        navigateToMainScreen() 
    }
    onError = { error -> 
        showErrorMessage(error.message) 
    }
    onTwoFactorRequired = { context -> 
        navigateToTwoFactorScreen(context) 
    }
}
```

This powerful DSL system allows you to:
- **Chain multiple callbacks** for the same event type
- **Transform responses** before they reach your UI
- **Add side effects** for analytics, logging, and state management  
- **Handle complex flows** with enriched context data
- **Maintain clean separation** between business logic and UI logic

# Session Event Bus

The SDK provides a lifecycle-aware event bus for session and messaging events. This allows your app to react to session changes (expiration, refresh, verification) and push notifications across your entire application in a decoupled, type-safe manner.

## Event Types

### Session Events

**Core Session Events (Always Available):**
- `SessionEvent.SessionExpired` - Session has expired
- `SessionEvent.SessionRefreshed` - Session was successfully refreshed  
- `SessionEvent.VerifySession` - Session verification requested

**Session Validation Events (Opt-In):**

These events only fire if you enable session validation when initializing the AuthenticationService:

```kotlin
val authenticationService = AuthenticationService(siteConfig)
    .registerForSessionValidation(
        config = SessionValidationConfig(
            intervalMinutes = 20L,  // Check session every 20 minutes
            enabled = true          // Enable validation
        )
    )
```

**Validation Events:**
- `SessionEvent.ValidationStarted` - Session validation has started
- `SessionEvent.ValidationSucceeded` - Session validation succeeded
- `SessionEvent.ValidationFailed` - Session validation failed (includes reason)

⚠️ **Note:** Session validation adds periodic background checks to ensure session validity. Only enable if your app requires this level of session monitoring.

### Message Events  

Push notification and Firebase messaging events:
- `MessageEvent.TokenReceived` - FCM token received
- `MessageEvent.RemoteMessageReceived` - Push notification received
- `MessageEvent.NotificationActionReceived` - Notification action triggered

## Usage

### Lifecycle-Aware Subscription (Activities/Fragments)

For UI components that implement `LifecycleOwner` (Activities, Fragments), use lifecycle-aware subscriptions that automatically clean up:

```kotlin
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize event bus (only needed once in your app)
        if (!CIAMEventBusProvider.isInitialized()) {
            CIAMEventBusProvider.initialize()
        }
        
        // Subscribe with automatic lifecycle management
        subscribeToSessionEvents { event ->
            when (event) {
                is SessionEvent.SessionExpired -> {
                    // Handle session expiration
                    navigateToLogin()
                }
                is SessionEvent.SessionRefreshed -> {
                    // Handle session refresh
                    updateSessionUI()
                }
                is SessionEvent.ValidationFailed -> {
                    // Handle validation failure
                    showSessionWarning(event.reason)
                }
                else -> { /* Handle other events */ }
            }
        }
    }
    
    // No cleanup needed - automatically unsubscribes when lifecycle ends
}
```

### Manual Subscription (ViewModels/Services)

For components without lifecycle support (ViewModels, Services), use manual subscriptions with explicit cleanup:

```kotlin
class MainActivityViewModel(
    private val authenticationFlowDelegate: AuthenticationFlowDelegate
) : ViewModel() {
    
    private var sessionEventSubscription: EventSubscription? = null
    
    init {
        // Initialize event bus
        if (!CIAMEventBusProvider.isInitialized()) {
            CIAMEventBusProvider.initialize()
        }
        
        // Manual subscription requires explicit cleanup
        sessionEventSubscription = subscribeToSessionEventsManual { event ->
            when (event) {
                is SessionEvent.SessionExpired -> handleSessionExpired()
                is SessionEvent.SessionRefreshed -> handleSessionRefreshed()
                is SessionEvent.ValidationSucceeded -> handleValidationSucceeded()
                else -> { /* Handle other events */ }
            }
        }
    }
    
    private fun handleSessionExpired() {
        authenticationFlowDelegate.handleSessionExpired()
        // Navigate to login screen
    }
    
    override fun onCleared() {
        // Important: Unsubscribe to prevent memory leaks
        sessionEventSubscription?.unsubscribe()
        sessionEventSubscription = null
        super.onCleared()
    }
}
```

## Event Scopes

Events can be scoped for targeted distribution:

- `EventScope.GLOBAL` - All subscribers receive the event (default)
- `EventScope.SCOPED` - Only subscribers in a specific scope receive the event

```kotlin
// Emit to all subscribers (default)
emitSessionExpired(sessionId)

// Emit to specific scope
emitSessionExpired(sessionId, scope = EventScope.SCOPED)
```

## Benefits

✅ **Decoupled Architecture** - Components don't need direct references to each other  
✅ **Lifecycle-Aware** - Automatic cleanup prevents memory leaks in Activities/Fragments  
✅ **Type-Safe** - Compile-time event type checking with sealed classes  
✅ **Flexible** - Works with Activities, Fragments, ViewModels, and Services  
✅ **Testable** - Easy to mock and test event handling  
✅ **Thread-Safe** - Events dispatched on appropriate coroutine dispatchers

## Common Use Cases

### 1. Global Session Expiration Handling

Handle session expiration across your entire app from a single ViewModel:

```kotlin
class MainActivityViewModel : ViewModel() {
    init {
        subscribeToSessionEventsManual { event ->
            if (event is SessionEvent.SessionExpired) {
                // Clear app state
                clearUserData()
                // Navigate to login
                navigateToLogin()
            }
        }
    }
}
```

### 2. Push Notification Handling

React to push notifications throughout your app:

```kotlin
subscribeToMessageEvents { event ->
    when (event) {
        is MessageEvent.RemoteMessageReceived -> {
            // Show in-app notification
            showInAppNotification(event.data)
        }
        is MessageEvent.NotificationActionReceived -> {
            // Handle notification action
            handleNotificationAction(event.action, event.data)
        }
        else -> { /* Handle other events */ }
    }
}
```

### 3. Session Monitoring

Monitor session health with validation events:

```kotlin
subscribeToSessionEvents { event ->
    when (event) {
        is SessionEvent.ValidationStarted -> showLoadingIndicator()
        is SessionEvent.ValidationSucceeded -> hideLoadingIndicator()
        is SessionEvent.ValidationFailed -> {
            hideLoadingIndicator()
            showSessionWarning("Session validation failed: ${event.reason}")
        }
        else -> { /* Handle other events */ }
    }
}
```

## Important Notes

⚠️ **Initialization:** Call `CIAMEventBusProvider.initialize()` once in your app (e.g., in MainActivity or Application class)

⚠️ **Manual Subscriptions:** Always call `unsubscribe()` on manual subscriptions to prevent memory leaks

⚠️ **Thread Safety:** Event handlers run on background threads (IO dispatcher) for manual subscriptions. Use appropriate dispatchers for UI updates.

⚠️ **Validation Events:** Only subscribe to validation events if you've enabled session validation via `registerForSessionValidation()`

# Social Provider Authentication

The SDK provides a flexible system for integrating social login providers (Google, Facebook, WeChat, Line, etc.) through the `IAuthenticationProvider` interface. The SDK is **intentionally decoupled** from third-party social SDKs, allowing you to use any version of social provider SDKs without compatibility issues.

## Architecture Overview

**Key Principle:** The SDK doesn't include social provider SDKs (Facebook SDK, Google Sign-In, etc.). Instead, you implement the authentication logic in your app and provide the results to the SDK through a standardized interface.

```
Your App → Social Provider SDK → IAuthenticationProvider → CIAM SDK → CIAM Server
```

## IAuthenticationProvider Interface

Implement this interface to integrate any social provider that requires a native SDK:

```kotlin
interface IAuthenticationProvider {
    fun getProvider(): String  // Provider identifier (e.g., "facebook", "google")
    suspend fun signIn(hostActivity: ComponentActivity?): AuthenticatorProviderResult
    suspend fun signOut(hostActivity: ComponentActivity?)
    fun dispose()  // Clean up resources
}
```

## AuthenticatorProviderResult

This is the key object that bridges your provider implementation with the CIAM SDK. It contains the authentication token in a JSON format that the CIAM server validates.

```kotlin
AuthenticatorProviderResult(
    provider = "facebook",           // Provider identifier
    type = ProviderType.NATIVE,      // Provider type
    providerSessions = providerSessionJSON  // JSON with auth token(s)
)
```

### Provider Session Format

The `providerSessions` parameter is a JSON string containing authentication tokens. **The exact structure varies by provider** (Facebook uses `authToken`, Google uses `idToken`, etc.).

**Important:** Each social provider has its own specific JSON structure required by CIAM servers. Refer to the example implementations in the app module for the exact format:

- **Facebook:** `app/.../provider/FacebookAuthenticationProvider.kt`
- **Google:** `app/.../provider/GoogleAuthenticationProvider.kt`
- **WeChat, Line:** Similar pattern with provider-specific token fields

## Provider Types

### 1. NATIVE Providers
Social providers using their official native SDKs (Facebook, Google, WeChat, Line).

**When to use:** When you want the best user experience with native UI and full SDK features.

**Implementation required:**
- Implement `IAuthenticationProvider` interface
- Use the provider's native SDK for authentication
- Generate correct `providerSessions` JSON
- Return `AuthenticatorProviderResult` with `ProviderType.NATIVE`

**Example providers in app module:**
- `FacebookAuthenticationProvider.kt` - Facebook Login SDK
- `GoogleAuthenticationProvider.kt` - Credential Manager API

### 2. WEB Providers
Social providers without native SDKs, handled through OAuth in WebView.

**When to use:** For providers like LinkedIn, Twitter, or any OAuth provider without a native Android SDK.

**Implementation:** Use the built-in `WebAuthenticationProvider` - **no custom implementation needed**.

```kotlin
// For providers without native SDKs (e.g., LinkedIn)
val linkedInProvider = WebAuthenticationProvider(
    "linkedin",
    siteConfig,
    currentSession
)

// Register and use like any other provider
authenticationProviderMap["linkedin"] = linkedInProvider
```

The SDK automatically handles the OAuth flow in a WebView and extracts the session.

## Using Social Login

Once you have implemented your authentication providers, use them through the SDK's provider API:

```kotlin
// Create your provider instance (native or web)
val facebookProvider = FacebookAuthenticationProvider()  // Your implementation
// OR
val linkedInProvider = WebAuthenticationProvider("linkedin", siteConfig, session)

// Execute social login
authenticationService.authenticate().provider().signIn(
    hostActivity = activity,
    authenticationProvider = facebookProvider
) {
    onSuccess = { authSuccess ->
        navigateToMainScreen()
    }
    onError = { authError ->
        showError(authError.message)
    }
    onLinkingRequired = { context ->
        // Account conflict - user needs to link accounts
        showAccountLinkingScreen(context)
    }
}
```

### Provider Management

How you manage and register providers depends on your app's architecture. The example app demonstrates one approach using a provider registry pattern in `AuthenticationFlowDelegate.kt`, but you can organize this however fits your architecture best.

## Complete Implementation Examples

The example app includes complete, working implementations for reference:

### Files to Reference:
- **Native Providers:**
  - `app/src/main/java/com/sap/cdc/bitsnbytes/feature/provider/FacebookAuthenticationProvider.kt`
  - `app/src/main/java/com/sap/cdc/bitsnbytes/feature/provider/GoogleAuthenticationProvider.kt`
  
- **Provider Registration & Usage:**
  - `app/src/main/java/com/sap/cdc/bitsnbytes/feature/auth/AuthenticationFlowDelegate.kt`

These files show:
- How to integrate native SDKs (Facebook SDK, Google Credential Manager)
- How to construct correct `providerSessions` JSON for each provider
- How to handle errors with `ProviderException`
- How to register and use providers in authentication flows

## Benefits of This Architecture

✅ **SDK Independence** - Use any version of social provider SDKs
✅ **Flexibility** - Add custom providers or use web-based providers easily
✅ **Maintainability** - Update social SDKs without SDK updates
✅ **Consistency** - Standardized interface for all providers
✅ **Testing** - Easy to mock providers for testing
✅ **Future-Proof** - New social providers can be added anytime

## Important Notes

⚠️ **Provider Session JSON:** Each provider requires a specific JSON structure. Always refer to the example implementations for the exact format.

⚠️ **Provider Names:** Use the exact provider name expected by CIAM servers (e.g., "facebook", "google", not "Facebook" or "GOOGLE").

⚠️ **Dependencies:** Add social provider SDKs to your **app's** `build.gradle.kts`, not the CIAM SDK. The SDK remains decoupled.

⚠️ **WebAuthenticationProvider:** For providers without native SDKs (LinkedIn, Twitter, etc.), use the built-in `WebAuthenticationProvider` - no custom implementation needed.

# Web Screen-Sets

Web Screen-Sets provide customizable web-based authentication UI that integrates seamlessly with your Android app through the `WebBridgeJS` component.

## Basic Usage

```kotlin
// 1. Create WebBridge instance
val webBridgeJS = WebBridgeJS(authenticationService)

// 2. Configure with obfuscation (optional)
webBridgeJS.addConfig(
    WebBridgeJSConfig.Builder()
        .obfuscate(true)
        .build()
)

// 3. Attach to WebView
webBridgeJS.attachBridgeTo(webView)

// 4. Set native social providers (optional)
webBridgeJS.setNativeSocialProviders(authenticationProviderMap)

// 5. Register event callbacks using ScreenSetsCallbacks
webBridgeJS.attachCallbacks(ScreenSetsCallbacks().apply {
    onLoad = { event ->
        // Handle screen-set load
        println("ScreenSet loaded: ${event.screenSetId}")
    }
    
    onLogin = { event ->
        // Handle login success
        navigateToMainScreen()
    }
    
    onError = { error ->
        // Handle errors
        showError(error.message)
    }
    
    onFieldChanged = { event ->
        // Handle form field changes
        validateField(event)
    }
})

// 6. Load screen-set
webBridgeJS.load(webView, screenSetUrl)

// 7. Clean up when done
webBridgeJS.detachBridgeFrom(webView)
webBridgeJS.detachCallbacks()
```

## Available Events

The `ScreenSetsCallbacks` provides handlers for all screen-set lifecycle events:

**Lifecycle Events:**
- `onBeforeScreenLoad` - Before screen loads
- `onLoad` - Screen loaded and ready
- `onAfterScreenLoad` - After screen fully rendered
- `onHide` - Screen hidden or dismissed

**Form Events:**
- `onFieldChanged` - Form field value changed
- `onBeforeValidation` - Before form validation
- `onAfterValidation` - After form validation
- `onBeforeSubmit` - Before form submission
- `onSubmit` - Form submitted
- `onAfterSubmit` - After form submission

**Authentication Events:**
- `onLoginStarted` - Login process initiated
- `onLogin` - Login successful
- `onLogout` - User logged out
- `onAddConnection` - Social account connected
- `onRemoveConnection` - Social account disconnected
- `onCanceled` - User canceled the flow

**Error Handling:**
- `onError` - Error occurred
- `onAnyEvent` - Universal handler for all events

**Benefits:**
- **Flexibility:** Customize authentication UI using HTML, CSS, and JavaScript
- **Consistency:** Maintain consistent branding across platforms
- **Reduced Development:** Leverage existing web technologies
- **Native Integration:** Seamless communication between web and native code
- **Type-Safe Events:** Strongly-typed event handling with ScreenSetsCallbacks

# Example Application

This repository includes a comprehensive example application demonstrating SDK integration with Jetpack Compose, MVVM architecture, and modern Android development practices.

**See the [Example App Documentation](app/README.md) for:**
- Complete architecture overview and patterns
- Delegate-based state management approach
- WebView integration in Compose
- Real-world implementation examples
- Step-by-step usage guides

## Navigation

- 📱 [Example Application README](app/README.md) - Architecture, patterns, and implementation guide
- 🔧 [Library Source Code](library/src) - SDK implementation details
- 💬 [Contributing Guidelines](CONTRIBUTING.md) - How to contribute to this project

# Support, Feedback, Contributing

This project is open to feature requests/suggestions, bug reports, etc. via [GitHub issues](https://github.com/SAP/sap-customer-data-cloud-sdk-for-android/issues). Contribution and feedback are encouraged and always welcome. For more information about how to contribute, the project structure, and additional contribution information, see our [Contribution Guidelines](CONTRIBUTING.md).

# Security / Disclosure

If you find any bug that may be a security problem, please follow our instructions at [in our security policy](https://github.com/SAP/sap-customer-data-cloud-sdk-for-android/security/policy) on how to report it. Please do not create GitHub issues for security-related doubts or problems.

# Code of Conduct

As members, contributors, and leaders pledge to make participation in our community a harassment-free experience for everyone. By participating in this project, you agree to always abide by its [Code of Conduct](https://github.com/SAP/.github/blob/main/CODE_OF_CONDUCT.md).

# Licensing

Copyright 2024 SAP SE or an SAP affiliate company and sap-customer-data-cloud-sdk-for-android contributors. Please see our [LICENSE](LICENSE) for copyright and license information. Detailed information including third-party components and their licensing/copyright information is available [via the REUSE tool](https://api.reuse.software/info/github.com/SAP/sap-customer-data-cloud-sdk-for-android).
