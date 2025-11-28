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
    implementation("com.sap.oss.cdc-android-sdk:cdc-android-sdk:0.3.0")
}
```

The library is available on [MavenCentral](https://search.maven.org/artifact/com.sap.oss.cdc-android-sdk/cdc-android-sdk).

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
<string name="com.sap.cxcdc.apikey">YOUR_API_KEY_HERE</string>

<!-- Optional - defaults to "us1.gigya.com" if not specified -->
<string name="com.sap.cxcdc.domain">YOUR_API_DOMAIN_HERE</string>

<!-- Optional -->
<string name="com.sap.cxcdc.cname">YOUR_CNAME_HERE</string>
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
                // Handle 2FA requirement
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
- **Two-Factor Authentication:** When 2FA is required for login/registration
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
            // - Available email addresses for 2FA
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
