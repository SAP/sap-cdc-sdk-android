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

The SDK's most powerful feature is the Kotlin DSL `AuthCallbacks` system, which provides advanced callback chaining, transformation, and side-effect capabilities.

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

### Side Effects with `doOn*` Methods

Side effects execute **before** your main callbacks, perfect for logging, analytics, or state management:

```kotlin
authenticationService.authenticate().login()
    .credentials(credentials) {
        // Side effect - executes first
        doOnSuccess { authSuccess ->
            // Log successful login
            analytics.track("login_success")
            // Update local state
            updateUserSession(authSuccess)
        }
        
        // Main callback - executes after side effect
        onSuccess = { authSuccess ->
            navigateToMainScreen()
        }
    }
```

### Response Transformation with Override Methods

Transform responses before they reach your callbacks using `doOn*AndOverride` methods:

```kotlin
authenticationService.authenticate().login()
    .credentials(credentials) {
        // Transform the success response
        doOnSuccessAndOverride { authSuccess ->
            // Add custom data or modify response
            authSuccess.copy(
                userData = authSuccess.userData + ("loginTime" to System.currentTimeMillis())
            )
        }
        
        // Transform error responses
        doOnErrorAndOverride { authError ->
            // Localize error messages
            authError.copy(
                message = localizeErrorMessage(authError.message)
            )
        }
        
        onSuccess = { transformedSuccess ->
            // Receives the transformed response
            handleLogin(transformedSuccess)
        }
    }
```

### Universal Override for All Callback Types

Handle any callback type with a single universal override:

```kotlin
authenticationService.authenticate().login()
    .credentials(credentials) {
        // Universal override for all callback types
        doOnAnyAndOverride { authResult ->
            when (authResult) {
                is AuthResult.Success -> {
                    analytics.track("auth_success")
                    authResult
                }
                is AuthResult.Error -> {
                    analytics.track("auth_error", authResult.authError.code)
                    authResult
                }
                is AuthResult.TwoFactorRequired -> {
                    analytics.track("auth_2fa_required")
                    authResult
                }
                // Handle other types...
                else -> authResult
            }
        }
        
        onSuccess = { /* handle success */ }
        onError = { /* handle error */ }
        onTwoFactorRequired = { /* handle 2FA */ }
    }
```

### Context Update Callbacks

Get enriched context data for multi-step flows:

```kotlin
authenticationService.authenticate().login()
    .credentials(credentials) {
        // Enriched context with additional SDK data
        onTwoFactorContextUpdated = { enrichedContext ->
            // Context contains emails, phone numbers, QR codes, etc.
            updateTwoFactorUI(enrichedContext)
        }
        
        // Standard callback still works
        onTwoFactorRequired = { context ->
            navigateToTwoFactorScreen(context)
        }
    }
```

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
// Create WebBridge instance
val webBridgeJS = WebBridgeJS(authenticationService)

// Configure with obfuscation
webBridgeJS.addConfig(
    WebBridgeJSConfig.Builder()
        .obfuscate(true)
        .build()
)

// Attach to WebView
webBridgeJS.attachBridgeTo(webView, lifecycleScope)

// Set native social providers (optional)
webBridgeJS.setNativeSocialProviders(authenticationProviderMap)

// Register for events
webBridgeJS.registerForEvents { event ->
    // Handle web screen-set events
    when (event.type) {
        "login" -> handleLoginEvent(event)
        "register" -> handleRegisterEvent(event)
        // Handle other events
    }
}

// Load screen-set
webBridgeJS.load(webView, screenSetUrl)

// Clean up when done
webBridgeJS.detachBridgeFrom(webView)
```

**Benefits:**
- **Flexibility:** Customize authentication UI using HTML, CSS, and JavaScript
- **Consistency:** Maintain consistent branding across platforms
- **Reduced Development:** Leverage existing web technologies
- **Native Integration:** Seamless communication between web and native code

# Support, Feedback, Contributing

This project is open to feature requests/suggestions, bug reports, etc. via [GitHub issues](https://github.com/SAP/sap-customer-data-cloud-sdk-for-android/issues). Contribution and feedback are encouraged and always welcome. For more information about how to contribute, the project structure, and additional contribution information, see our [Contribution Guidelines](CONTRIBUTING.md).

# Security / Disclosure

If you find any bug that may be a security problem, please follow our instructions at [in our security policy](https://github.com/SAP/sap-customer-data-cloud-sdk-for-android/security/policy) on how to report it. Please do not create GitHub issues for security-related doubts or problems.

# Code of Conduct

As members, contributors, and leaders pledge to make participation in our community a harassment-free experience for everyone. By participating in this project, you agree to always abide by its [Code of Conduct](https://github.com/SAP/.github/blob/main/CODE_OF_CONDUCT.md).

# Licensing

Copyright 2024 SAP SE or an SAP affiliate company and sap-customer-data-cloud-sdk-for-android contributors. Please see our [LICENSE](LICENSE) for copyright and license information. Detailed information including third-party components and their licensing/copyright information is available [via the REUSE tool](https://api.reuse.software/info/github.com/SAP/sap-customer-data-cloud-sdk-for-android).
