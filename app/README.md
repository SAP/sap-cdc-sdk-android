[![REUSE status](https://api.reuse.software/badge/github.com/SAP/sap-customer-data-cloud-sdk-for-android)](https://api.reuse.software/info/github.com/SAP/sap-customer-data-cloud-sdk-for-android)

# Example Application

This example application demonstrates comprehensive integration of the SAP Customer Data Cloud SDK for Android using modern Android development practices with Jetpack Compose, Kotlin Coroutines, and MVVM architecture.

## Overview

### What This App Demonstrates

- **Complete Authentication Flows** - Login, registration, social providers, OTP, 2FA
- **ScreenSets Integration** - WebView-based authentication UI in Compose
- **Biometric Authentication** - Secure session encryption with device biometrics
- **Session Management** - Automatic validation and lifecycle management
- **State Management** - Reactive state flows with centralized authentication
- **Navigation** - Type-safe navigation with deep linking support
- **Social Providers** - Google, Facebook, WeChat, Line integration
- **Passkeys** - WebAuthn credential management

## Architecture

### MVVM + Single Delegate Pattern

This app uses a **delegate-based architecture** that eliminates repository boilerplate while providing direct SDK access and centralized state management.

```
┌─────────────────────────────────────────────────────────┐
│                    Composable UI                         │
│  (Screens, Components, Navigation)                       │
└─────────────────────┬───────────────────────────────────┘
                      │ observes StateFlow
                      │ calls suspend functions
┌─────────────────────▼───────────────────────────────────┐
│                    ViewModel                             │
│  (Screen-specific business logic)                        │
│  - Holds UI state (StateFlow)                            │
│  - Delegates to AuthenticationFlowDelegate               │
└─────────────────────┬───────────────────────────────────┘
                      │ delegates all auth operations
                      │ receives state updates
┌─────────────────────▼───────────────────────────────────┐
│            AuthenticationFlowDelegate                    │
│  (Centralized authentication & state manager)            │
│  - Direct CDC SDK access                                 │
│  - Manages isAuthenticated / userAccount state           │
│  - Wraps SDK calls with state side-effects               │
└─────────────────────┬───────────────────────────────────┘
                      │ uses
┌─────────────────────▼───────────────────────────────────┐
│              AuthenticationService                       │
│  (SAP CDC SDK)                                           │
└─────────────────────────────────────────────────────────┘
```

### Key Components

#### AuthenticationFlowDelegate

The heart of the application - a single instance shared across all ViewModels that provides direct CDC SDK access and manages authentication state.

**Purpose:**
- Eliminates repository layer boilerplate
- Provides direct access to `AuthenticationService`
- Manages centralized authentication state (`isAuthenticated`, `userAccount`)
- Wraps SDK callbacks with automatic state updates

**Usage Example:**
```kotlin
class EmailSignInViewModel(
    val flowDelegate: AuthenticationFlowDelegate
) : BaseViewModel() {
    
    suspend fun signIn(email: String, password: String) {
        flowDelegate.login(Credentials(email, password)) {
            onSuccess = { authSuccess ->
                // State automatically updated by delegate
                navigate(Routes.MyProfile)
            }
            
            onError = { error ->
                showError(error.message)
            }
            
            onTwoFactorRequired = { context ->
                navigate(Routes.TwoFactor(context))
            }
        }
    }
}
```

**Key Features:**
- **State Management** - `isAuthenticated: StateFlow<Boolean>`, `userAccount: StateFlow<AccountEntity?>`
- **Convenience Methods** - Wraps common SDK operations with state side-effects
- **Provider Management** - Handles social provider registration and retrieval
- **Biometric Support** - Manages biometric session encryption lifecycle

#### ViewModel Structure

**BaseViewModel**
- Common functionality for all ViewModels
- Context access and lifecycle management
- Error handling patterns

**Screen ViewModels**
- Screen-specific business logic
- UI state management with `StateFlow`
- Navigation events with `SharedFlow`
- Delegates authentication to `AuthenticationFlowDelegate`

**Example:**
```kotlin
class EmailSignInViewModel(
    context: Context,
    val flowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context) {
    
    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()
    
    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()
}
```

#### Navigation System

**NavigationCoordinator**
- Centralized navigation logic
- Deep linking support
- Navigation state management

**Routes**
- Type-safe route definitions
- Compile-time safety for navigation
- Support for navigation arguments

**Example:**
```kotlin
sealed class Routes(val route: String) {
    object Welcome : Routes("welcome")
    object SignIn : Routes("sign_in")
    object MyProfile : Routes("my_profile")
    data class TwoFactor(val context: TwoFactorContext) : Routes("two_factor")
}
```

## Project Structure

```
app/src/main/java/com/sap/cdc/bitsnbytes/
├── feature/
│   ├── auth/                          # Authentication logic
│   │   ├── AuthenticationFlowDelegate.kt    # Central auth manager
│   │   ├── BiometricLifecycleManager.kt     # Biometric lifecycle
│   │   ├── SessionMigrator.kt               # Legacy session migration
│   │   └── model/
│   │       └── AccountEntity.kt             # User account data model
│   ├── messaging/                     # Push notifications
│   │   ├── AppMessagingService.kt           # FCM service
│   │   └── NotificationPermissionManager.kt
│   └── provider/                      # Social authentication providers
│       ├── FacebookAuthenticationProvider.kt
│       ├── GoogleAuthenticationProvider.kt
│       ├── LineAuthenticationProvider.kt
│       ├── PasskeysAuthenticationProvider.kt
│       └── WeChatAuthenticationProvider.kt
├── navigation/                        # Navigation coordination
│   ├── AppStateManager.kt                   # App-wide state
│   ├── NavigationCoordinator.kt             # Navigation logic
│   ├── NavigationDebugLogger.kt             # Debug logging
│   ├── ProfileNavHost.kt                    # Profile navigation graph
│   ├── Routes.kt                            # Route definitions
│   └── SettingsNavHost.kt                   # Settings navigation graph
├── ui/
│   ├── activity/                      # Main activity
│   │   ├── MainActivity.kt
│   │   └── MainActivityViewModel.kt
│   ├── state/                         # UI state classes
│   │   ├── SignInState.kt
│   │   ├── RegisterState.kt
│   │   ├── ScreenSetState.kt
│   │   └── ... (one per screen)
│   ├── view/
│   │   ├── composables/              # Reusable UI components
│   │   │   ├── Buttons.kt
│   │   │   ├── InputFields.kt
│   │   │   ├── SocialProviderSelectionView.kt
│   │   │   └── ...
│   │   └── screens/                  # Screen implementations
│   │       ├── Welcome.kt / WelcomeViewModel.kt
│   │       ├── SignIn.kt / SignInViewModel.kt
│   │       ├── ScreenSet.kt / ScreenSetViewModel.kt
│   │       └── ... (paired screen + viewmodel)
│   └── viewmodel/                    # ViewModel utilities
│       ├── BaseViewModel.kt                 # Base ViewModel class
│       └── factory/
│           ├── CustomViewModelFactory.kt    # ViewModel factory
│           └── ViewModelScopeProvider.kt    # Scope provider
├── apptheme/                          # Material Design theming
│   ├── AppDesignSystem.kt
│   ├── AppTheme.kt
│   ├── Color.kt
│   ├── Size.kt
│   └── Type.kt
└── extensions/                        # Kotlin extensions
    ├── JsonEncode.kt
    └── StringExt.kt
```

## Key Patterns

### Pattern 1: Delegate-Based State Management

**Problem:** ViewModels need authentication state and SDK access without creating repository boilerplate.

**Solution:** `AuthenticationFlowDelegate` provides direct SDK access plus managed state:

```kotlin
class AuthenticationFlowDelegate(context: Context) {
    
    // Direct SDK access
    val authenticationService = AuthenticationService(SiteConfig(context))
    
    // Managed authentication state
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private val _userAccount = MutableStateFlow<AccountEntity?>(null)
    val userAccount: StateFlow<AccountEntity?> = _userAccount.asStateFlow()
    
    // Wrapped SDK methods with automatic state updates
    suspend fun login(credentials: Credentials, authCallbacks: AuthCallbacks.() -> Unit) {
        authenticationService.authenticate().login()
            .credentials(credentials) {
                // User's callbacks first
                authCallbacks()
                
                // Add state management side-effect
                doOnSuccess { authSuccess ->
                    val account = json.decodeFromString<AccountEntity>(authSuccess.jsonData)
                    _userAccount.value = account
                    _isAuthenticated.value = true
                }
            }
    }
}
```

**Benefits:**
- No repository layer needed
- State automatically synced with SDK
- ViewModels remain simple and focused
- Single source of truth for authentication

### Pattern 2: WebView in Compose with Priority-Based Navigation

**Problem:** Managing WebView lifecycle in Compose is complex. CDC WebBridge fires multiple events during a single flow (e.g., both `onLogin` and `onHide` when user logs in), causing duplicate navigation.

**Solution:** `ScreenSetViewModel` implements priority-based navigation suppression:

```kotlin
private enum class ScreenSetNavigationPriority(val priority: Int) {
    LOGIN(100),      // Highest - user successfully authenticated
    LOGOUT(80),      // High - user logged out
    CANCELED(60),    // Medium - user cancelled flow
    HIDE(40)         // Lowest - generic screenset closure
}

// Track highest priority navigation event
private var currentNavigationPriority: ScreenSetNavigationPriority? = null

private fun shouldNavigate(eventPriority: ScreenSetNavigationPriority): Boolean {
    val current = currentNavigationPriority
    return if (current == null || eventPriority.isHigherThan(current)) {
        currentNavigationPriority = eventPriority
        true  // Proceed with navigation
    } else {
        false  // Suppress lower-priority event
    }
}

// Usage in event handlers
private fun handleOnLogin(eventData: ScreenSetsEventData) {
    if (shouldNavigate(ScreenSetNavigationPriority.LOGIN)) {
        _navigationEvents.emit(NavigateToMyProfile)
    }
}

private fun handleOnHide(eventData: ScreenSetsEventData) {
    if (shouldNavigate(ScreenSetNavigationPriority.HIDE)) {
        _navigationEvents.emit(NavigateBack)
    }
}
```

**Result:**
- `onLogin` fires → priority 100 → navigates to MyProfile ✅
- `onHide` fires 100ms later → priority 40 < 100 → **suppressed** ✅

### Pattern 3: Callback Side-Effects

**Problem:** Need to update UI state when authentication succeeds without breaking the callback chain or requiring ViewModels to parse responses.

**Solution:** Delegate wraps SDK callbacks and adds state management side-effects:

```kotlin
suspend fun login(credentials: Credentials, authCallbacks: AuthCallbacks.() -> Unit) {
    authenticationService.authenticate().login()
        .credentials(credentials) {
            // 1. Register user's callbacks first
            authCallbacks()
            
            // 2. Add state management side-effect
            doOnSuccess { authSuccess ->
                try {
                    // Parse and update state automatically
                    val account = json.decodeFromString<AccountEntity>(authSuccess.jsonData)
                    _userAccount.value = account
                    _isAuthenticated.value = true
                } catch (e: Exception) {
                    // Handle errors silently - don't break callback chain
                }
            }
        }
}
```

**Benefits:**
- ViewModels don't need to parse API responses
- State updates happen automatically
- Callback chain remains intact
- Error handling doesn't break the flow

### Pattern 4: Compose + WebView Lifecycle

**Problem:** WebView disposal in Compose requires careful lifecycle management to prevent memory leaks.

**Solution:** ViewModel-managed WebBridge lifecycle:

```kotlin
// In ScreenSetViewModel
private var webBridgeJS: WebBridgeJS? = null

override fun setupWebBridge(webView: WebView) {
    webBridgeJS = flowDelegate.getWebBridge()
    webBridgeJS?.attachBridgeTo(webView)
    webBridgeJS?.onScreenSetEvents { /* callbacks */ }
    webBridgeJS?.load(webView, screenSetUrl)
}

override fun handleWebViewDisposal(webView: WebView) {
    webBridgeJS?.detachBridgeFrom(webView)
    webBridgeJS = null
}

// In Composable
DisposableEffect(viewKey) {
    // Setup on composition
    viewModel.setupWebBridge(webView)
    
    onDispose {
        // Cleanup on disposal
        viewModel.handleWebViewDisposal(webView)
    }
}
```

## Running the App

### Prerequisites

- Android Studio Iguana or later
- JDK 11+
- Android SDK API 24+ (Android 7.0+)

### Configuration

1. **Add your CDC API key** to `app/src/main/res/values/strings.xml`:

```xml
<!-- Required -->
<string name="com.sap.cxcdc.apikey">YOUR_API_KEY_HERE</string>

<!-- Optional - defaults to us1.gigya.com -->
<string name="com.sap.cxcdc.domain">YOUR_API_DOMAIN</string>

<!-- Optional -->
<string name="com.sap.cxcdc.cname">YOUR_CNAME</string>
```

2. **Configure social providers** (optional):
   - Google: Add `google-services.json` to `app/` directory
   - Facebook: Configure Facebook App ID in `strings.xml`
   - WeChat: Add WeChat App ID and configure deep linking
   - Line: Configure Line Channel ID

3. **Build Variants:**
   - `debug` - Development build with debug logging
   - `variant` - Alternative configuration for testing different environments

### Build and Run

**Using Android Studio:**
1. Open project in Android Studio
2. Select `app` module
3. Choose `demoDebug` or `variantDebug` variant
4. Click Run (▶️)

**Using Command Line:**
```bash
# Build debug APK
./gradlew :app:assembleDebug

# Install on connected device
./gradlew :app:installDebug

# Build and install
./gradlew :app:installDebug

# Run specific variant
./gradlew :app:installVariantDebug
```

## Learning Path

### For SDK Users

1. **Start here:** [Root README](../README.md) - Learn SDK basics and API usage
2. **Understand the delegate:** Study `AuthenticationFlowDelegate.kt` to see how the SDK is wrapped
3. **Simple examples:** Look at `EmailSignInViewModel.kt` for basic authentication flows
4. **Advanced patterns:** Examine `ScreenSetViewModel.kt` for WebView integration
5. **State management:** Review how StateFlow is used throughout ViewModels
6. **Navigation:** Check `NavigationCoordinator.kt` for routing patterns

### For Contributors

1. **Architecture:** Understand the delegate pattern and its benefits
2. **State Flow:** Learn how state is managed and propagated to UI
3. **Navigation:** Review the type-safe navigation implementation
4. **WebView Integration:** Study the priority-based navigation system
5. **Testing:** Examine test examples and patterns
6. **Code Style:** Follow existing patterns for consistency

## Common Scenarios

### Scenario 1: Adding a New Authentication Screen

1. Create state class in `ui/state/`:
```kotlin
data class MyNewScreenState(
    val isLoading: Boolean = false,
    val error: String? = null
)
```

2. Create ViewModel:
```kotlin
class MyNewScreenViewModel(
    context: Context,
    val flowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context) {
    
    private val _state = MutableStateFlow(MyNewScreenState())
    val state: StateFlow<MyNewScreenState> = _state.asStateFlow()
    
    fun performAction() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            flowDelegate.someAuthMethod {
                onSuccess = { /* handle success */ }
                onError = { /* handle error */ }
            }
        }
    }
}
```

3. Create Composable in `ui/view/screens/`
4. Add route to `Routes.kt`
5. Register in `NavigationCoordinator.kt`

### Scenario 2: Using the Delegate in Your Own App

1. Copy `AuthenticationFlowDelegate.kt` to your project
2. Initialize in your Application class or dependency injection:
```kotlin
class MyApp : Application() {
    lateinit var authDelegate: AuthenticationFlowDelegate
    
    override fun onCreate() {
        super.onCreate()
        authDelegate = AuthenticationFlowDelegate(this)
    }
}
```

3. Provide to ViewModels via ViewModelFactory or DI
4. Use in ViewModels:
```kotlin
class MyViewModel(val flowDelegate: AuthenticationFlowDelegate) : ViewModel() {
    
    val isLoggedIn = flowDelegate.isAuthenticated
    val user = flowDelegate.userAccount
    
    suspend fun login(email: String, password: String) {
        flowDelegate.login(Credentials(email, password)) {
            onSuccess = { /* success */ }
            onError = { /* error */ }
        }
    }
}
```

### Scenario 3: Customizing WebView ScreenSets

1. Use `ScreenSetViewModel` as a template
2. Customize event handlers for your UI:
```kotlin
bridge.onScreenSetEvents {
    onLoad = { data -> /* custom loading UI */ }
    onError = { error -> /* custom error handling */ }
    onLogin = { data -> /* custom success flow */ }
}
```

3. Adjust navigation priorities if needed
4. Add custom validation or data transformation

## Troubleshooting

### Common Issues

**Issue:** WebView not loading ScreenSet
- Check API key configuration
- Verify network connectivity
- Check logcat for WebView errors

**Issue:** Social provider login not working
- Verify provider configuration (google-services.json, Facebook App ID, etc.)
- Check redirect URIs match provider console
- Ensure provider is registered in `AuthenticationFlowDelegate`

**Issue:** Biometric authentication not available
- Check device has biometric hardware
- Verify biometric is enrolled in device settings
- Check app has biometric permission

**Issue:** State not updating after authentication
- Verify delegate side-effects are registered
- Check StateFlow collection in Composable
- Ensure ViewModel is properly scoped

## Navigation

- 📚 [Library Documentation (Root README)](../README.md) - SDK API reference and usage
- 🔧 [Library Source Code](../library/src) - SDK implementation details
- 💬 [Contributing Guidelines](../CONTRIBUTING.md) - How to contribute

# Support, Feedback, Contributing

This project is open to feature requests/suggestions, bug reports, etc. via [GitHub issues](https://github.com/SAP/sap-customer-data-cloud-sdk-for-android/issues). Contribution and feedback are encouraged and always welcome. For more information about how to contribute, the project structure, and additional contribution information, see our [Contribution Guidelines](../CONTRIBUTING.md).

# Security / Disclosure

If you find any bug that may be a security problem, please follow our instructions at [in our security policy](https://github.com/SAP/sap-customer-data-cloud-sdk-for-android/security/policy) on how to report it. Please do not create GitHub issues for security-related doubts or problems.

# Code of Conduct

As members, contributors, and leaders pledge to make participation in our community a harassment-free experience for everyone. By participating in this project, you agree to always abide by its [Code of Conduct](https://github.com/SAP/.github/blob/main/CODE_OF_CONDUCT.md).

# Licensing

Copyright 2024 SAP SE or an SAP affiliate company and sap-customer-data-cloud-sdk-for-android contributors. Please see our [LICENSE](../LICENSE) for copyright and license information. Detailed information including third-party components and their licensing/copyright information is available [via the REUSE tool](https://api.reuse.software/info/github.com/SAP/sap-customer-data-cloud-sdk-for-android).
