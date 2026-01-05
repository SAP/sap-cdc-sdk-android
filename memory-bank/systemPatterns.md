# System Patterns

## Core Architecture

### Multi-Module Structure

```
sap-cdc-sdk-android/
├── library/          # Published SDK (MavenCentral)
├── app/              # Example application
└── mrz-reader/       # Experimental MRZ scanning (in development)
```

**Key Principle:** Library is fully decoupled from app. App depends on library, but library has zero app dependencies.

## Library Architecture

### Layer Structure

```
SDK Entry Point
    ↓
AuthenticationService
    ↓
Feature Flows (Login, Register, Provider, etc.)
    ↓
Core Client (API + Network)
    ↓
Ktor HTTP Client
    ↓
CDC Server
```

### Core Components

#### 1. AuthenticationService
**Location:** `library/src/main/java/com/sap/cdc/android/sdk/feature/AuthenticationService.kt`

**Purpose:** Single entry point for all SDK operations

**Key Methods:**
- `authenticate()` → Returns AuthFlow for authentication operations
- `account()` → Returns IAuthAccount for account management
- `session()` → Returns IAuthSession for session operations
- `registerForSessionValidation()` → Opt-in session validation

**Pattern:**
```kotlin
val service = AuthenticationService(siteConfig)

// Fluent API chains operations
service.authenticate()
    .login()
    .credentials(credentials) { /* callbacks */ }
```

#### 2. Feature Flows
**Pattern:** Each authentication method has dedicated flow class

**Examples:**
- `AuthLoginFlow` - Email/password login
- `AuthRegisterFlow` - User registration
- `AuthProviderFlow` - Social provider login
- `AuthOtpFlow` - One-time password flows
- `AuthTFAFlow` - Two-factor authentication
- `AuthPasskeysFlow` - WebAuthn passkeys

**Common Structure:**
```kotlin
class AuthLoginFlow(
    private val coreClient: CoreClient,
    private val sessionService: SessionService
) : IAuthLogin {
    
    suspend fun credentials(
        credentials: Credentials,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        // 1. Build API request
        val request = buildLoginRequest(credentials)
        
        // 2. Execute network call
        val response = coreClient.execute(request)
        
        // 3. Process response
        val result = evaluateResponse(response)
        
        // 4. Update session if successful
        if (result is AuthResult.Success) {
            sessionService.setSession(result.authSuccess.session)
        }
        
        // 5. Execute callbacks
        executeCallbacks(result, authCallbacks)
    }
}
```

#### 3. Callback System
**Location:** `library/src/main/java/com/sap/cdc/android/sdk/feature/AuthCallbacks.kt`

**Core Design:** DSL builder with internal callback chain

**Two Registration Patterns:**

**Pattern A: Side Effects** (User callbacks first, side effects after)
```kotlin
credentials(credentials) {
    authCallbacks()           // User callbacks registered first
    doOnSuccess { /* side */ } // Side effects added after
}

// Execution order:
// 1. Side effect runs
// 2. User's onSuccess runs
// Both receive SAME original data
```

**Pattern B: Overrides** (Override first, user callbacks after)
```kotlin
credentials(credentials) {
    doOnSuccessAndOverride { original ->
        // Transform data
        transformedData
    }
    authCallbacks()  // User callbacks registered after
}

// Execution order:
// 1. Override transforms data
// 2. User's onSuccess receives TRANSFORMED data
```

**Implementation Pattern:**
```kotlin
class AuthCallbacks {
    // User-facing callbacks
    var onSuccess: ((AuthSuccess) -> Unit)? = null
    var onError: ((AuthError) -> Unit)? = null
    var onTwoFactorRequired: ((TwoFactorContext) -> Unit)? = null
    
    // Internal callback chains (for side effects & overrides)
    internal val successChain = mutableListOf<(AuthSuccess) -> Unit>()
    internal val errorChain = mutableListOf<(AuthError) -> Unit>()
    internal var successOverride: ((AuthResult.Success) -> AuthResult)? = null
    
    // DSL methods
    fun doOnSuccess(block: (AuthSuccess) -> Unit) {
        successChain.add(block)
    }
    
    fun doOnSuccessAndOverride(block: (AuthSuccess) -> AuthSuccess) {
        successOverride = { result ->
            AuthResult.Success(block(result.authSuccess))
        }
    }
}
```

#### 4. Session Management
**Location:** `library/src/main/java/com/sap/cdc/android/sdk/feature/session/`

**Components:**
- `SessionService` - Session CRUD operations
- `SessionSecure` - Encryption/decryption with biometric keys
- `SessionValidator` - Optional periodic validation
- `SessionValidationWorker` - Background WorkManager for validation

**Storage Strategy:**
```kotlin
// Plain session storage
SecureSharedPreferences (Android KeyStore encrypted)

// Biometric-encrypted session
SessionSecure (Biometric key + KeyStore)
```

**Session Validation (Opt-In):**
```kotlin
authenticationService.registerForSessionValidation(
    SessionValidationConfig(
        intervalMinutes = 20L,
        enabled = true
    )
)

// Triggers SessionValidationWorker via WorkManager
// Emits events via CIAMEventBus
```

#### 5. Event Bus System
**Location:** `library/src/main/java/com/sap/cdc/android/sdk/events/`

**Purpose:** Lifecycle-aware global event distribution

**Event Types:**
- `SessionEvent` - Session lifecycle (expired, refreshed, validated)
- `MessageEvent` - Push notifications and FCM tokens

**Key Components:**
- `CIAMEventBusProvider` - Global singleton event bus
- `LifecycleAwareEventBus` - Automatic subscription cleanup
- `EventSubscription` - Manual subscription handle

**Usage Patterns:**

**Lifecycle-Aware (Activities/Fragments):**
```kotlin
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        subscribeToSessionEvents { event ->
            when (event) {
                is SessionEvent.SessionExpired -> handleExpired()
                is SessionEvent.SessionRefreshed -> handleRefreshed()
            }
        }
        // Automatic cleanup when lifecycle ends
    }
}
```

**Manual (ViewModels/Services):**
```kotlin
class MyViewModel : ViewModel() {
    private var subscription: EventSubscription? = null
    
    init {
        subscription = subscribeToSessionEventsManual { event ->
            handleEvent(event)
        }
    }
    
    override fun onCleared() {
        subscription?.unsubscribe()
        super.onCleared()
    }
}
```

#### 6. Social Provider Framework
**Location:** `library/src/main/java/com/sap/cdc/android/sdk/feature/provider/`

**Key Principle:** SDK does NOT bundle social provider SDKs

**Interface:**
```kotlin
interface IAuthenticationProvider {
    fun getProvider(): String  // "facebook", "google", etc.
    suspend fun signIn(hostActivity: ComponentActivity?): AuthenticatorProviderResult
    suspend fun signOut(hostActivity: ComponentActivity?)
    fun dispose()
}
```

**Result Format:**
```kotlin
AuthenticatorProviderResult(
    provider = "google",
    type = ProviderType.NATIVE,  // or ProviderType.WEB
    providerSessions = """{"idToken": "..."}"""  // Provider-specific JSON
)
```

**Built-In Providers:**
- `WebAuthenticationProvider` - OAuth via WebView (no SDK needed)
- `SSOAuthenticationProvider` - Generic SSO with PKCE

**App Implementations (examples):**
- `GoogleAuthenticationProvider` - Uses Credential Manager
- `FacebookAuthenticationProvider` - Uses Facebook SDK
- Custom providers - Implement interface

#### 7. WebView Bridge (Screen-Sets)
**Location:** `library/src/main/java/com/sap/cdc/android/sdk/feature/screensets/`

**Purpose:** JavaScript ↔ Native communication for web-based auth UI

**Components:**
- `WebBridgeJS` - Main bridge coordinator
- `WebBridgeJSWebViewClient` - Intercepts page loads
- `WebBridgeJSWebChromeClient` - Handles JavaScript dialogs
- `ScreenSetsCallbacks` - Type-safe event handlers

**JavaScript Injection Pattern:**
```kotlin
// Bridge attaches to WebView
webBridgeJS.attachBridgeTo(webView)

// Injects JavaScript interface
@JavascriptInterface
fun receiveEvent(eventJson: String) {
    // Parse event from Screen-Set
    // Route to appropriate callback
}
```

**Event Flow:**
```
Screen-Set (Web) → JavaScript Event → Bridge → Native Callback → App UI
```

#### 8. Network Layer
**Location:** `library/src/main/java/com/sap/cdc/android/sdk/core/network/`

**Stack:** Ktor HTTP Client

**Structure:**
```kotlin
interface NetworkClient {
    suspend fun execute(request: CIAMRequest): CIAMResponse
}

class KtorHttpClientProvider : NetworkClient {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }
}
```

**Request/Response Pattern:**
```kotlin
// Request builder
CIAMRequest.Builder()
    .endpoint(endpoint)
    .method(HttpMethod.POST)
    .addParameter("apiKey", siteConfig.apiKey)
    .addParameter("loginID", email)
    .build()

// Response evaluation
CIAMResponseEvaluator.evaluate(response) // → AuthResult
```

## App Architecture (Example)

### Delegate-Based Pattern

**Core Principle:** Single AuthenticationFlowDelegate replaces repository layer

```
UI (Compose) ← observes StateFlow ← ViewModel ← delegates ← AuthenticationFlowDelegate ← uses ← AuthenticationService (SDK)
```

#### AuthenticationFlowDelegate
**Location:** `app/src/main/java/com/sap/cdc/bitsnbytes/feature/auth/AuthenticationFlowDelegate.kt`

**Purpose:**
1. Direct SDK access (`authenticationService`)
2. Centralized authentication state (`isAuthenticated`, `userAccount`)
3. Wrapped SDK methods with automatic state updates
4. Social provider registry

**Key Pattern:**
```kotlin
class AuthenticationFlowDelegate(context: Context) {
    
    // Direct SDK access
    val authenticationService = AuthenticationService(SiteConfig(context))
    
    // Managed state
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private val _userAccount = MutableStateFlow<AccountEntity?>(null)
    val userAccount: StateFlow<AccountEntity?> = _userAccount.asStateFlow()
    
    // Wrapped SDK methods with side effects
    suspend fun login(credentials: Credentials, authCallbacks: AuthCallbacks.() -> Unit) {
        authenticationService.authenticate().login()
            .credentials(credentials) {
                // User callbacks FIRST
                authCallbacks()
                
                // State update side effect AFTER
                doOnSuccess { authSuccess ->
                    val account = json.decodeFromString<AccountEntity>(authSuccess.jsonData)
                    _userAccount.value = account
                    _isAuthenticated.value = true
                }
            }
    }
}
```

#### ViewModel Pattern
**Structure:**
- `BaseViewModel` - Common functionality (context, error handling)
- Screen ViewModels - Screen-specific logic with StateFlow

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
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            flowDelegate.login(Credentials(email, password)) {
                onSuccess = {
                    _navigationEvents.emit(NavigateToMyProfile)
                }
                onError = { error ->
                    _state.update { it.copy(error = error.message) }
                }
            }
        }
    }
}
```

### WebView in Compose Pattern

**Challenge:** WebView disposal + Multiple CDC events (onLogin + onHide)

**Solution:** Priority-based navigation suppression

```kotlin
class ScreenSetViewModel {
    private enum class NavigationPriority(val priority: Int) {
        LOGIN(100),    // Highest
        LOGOUT(80),
        CANCELED(60),
        HIDE(40)       // Lowest
    }
    
    private var currentNavigationPriority: NavigationPriority? = null
    
    private fun shouldNavigate(eventPriority: NavigationPriority): Boolean {
        val current = currentNavigationPriority
        return if (current == null || eventPriority.priority > current.priority) {
            currentNavigationPriority = eventPriority
            true  // Allow navigation
        } else {
            false  // Suppress lower priority event
        }
    }
}
```

**Result:** onLogin (100) executes, onHide (40) suppressed

### Navigation Pattern

**Components:**
- `Routes` - Sealed class hierarchy for type-safe routes
- `NavigationCoordinator` - Centralized navigation logic
- `ProfileNavHost`, `SettingsNavHost` - Feature-specific nav graphs

**Type-Safe Navigation:**
```kotlin
sealed class Routes(val route: String) {
    object Welcome : Routes("welcome")
    object SignIn : Routes("sign_in")
    data class TwoFactor(val context: TwoFactorContext) : Routes("two_factor/{contextJson}") {
        companion object {
            fun createRoute(context: TwoFactorContext): String {
                val json = Json.encodeToString(context)
                return "two_factor/${Uri.encode(json)}"
            }
        }
    }
}
```

## Key Design Patterns

### 1. Builder Pattern
Used extensively for configuration objects:
- `CIAMRequest.Builder()`
- `WebBridgeJSConfig.Builder()`
- `SessionValidationConfig.Builder()`

### 2. Strategy Pattern
Authentication flows implement common interface:
- `IAuthLogin`, `IAuthRegister`, `IAuthProvider` all implement authentication contracts
- Allows swapping authentication strategies

### 3. Observer Pattern
- StateFlow for reactive state management
- Event bus for cross-component communication
- Lifecycle-aware subscriptions

### 4. Decorator Pattern
Callback system decorates user callbacks with side effects and transformations

### 5. Facade Pattern
`AuthenticationService` provides simplified interface to complex subsystems

### 6. Factory Pattern
`CustomViewModelFactory` creates ViewModels with dependencies

### 7. Delegate Pattern
`AuthenticationFlowDelegate` delegates to SDK while managing state

## Critical Implementation Details

### Session Storage Security
```
Plain Session:
SecureSharedPreferences → Android KeyStore → AES encryption

Biometric Session:
SessionSecure → Biometric Key → Android KeyStore → AES encryption
```

### Callback Execution Order

**Side Effects:**
1. SDK returns response
2. Side effects execute with original data
3. User callbacks execute with same original data

**Overrides:**
1. SDK returns response
2. Override transforms data
3. Side effects execute with transformed data (if any)
4. User callbacks execute with transformed data

### Provider Session JSON Format
Each provider requires specific JSON structure sent to CDC:

**Google:**
```json
{"idToken": "..."}
```

**Facebook:**
```json
{"authToken": "..."}
```

**Custom providers must match CDC server expectations**

### WebView JavaScript Bridge Security
- JavaScript interface only available on HTTPS
- Event data validated before processing
- Optional obfuscation via `WebBridgeJSConfig`

## Testing Patterns

### Library Testing
- Ktor MockEngine for network simulation
- Mockito for dependency mocking
- Coroutine test dispatchers

### App Testing
- ViewModel testing with fake delegates
- Compose UI testing
- Navigation testing with test nav controller

## Build System Patterns

### Version Catalog
All dependencies centralized in `gradle/libs.versions.toml`:
```toml
[versions]
kotlin = "2.2.21"
compose-bom = "2025.12.00"

[libraries]
androidx-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "ktx" }

[bundles]
compose = ["compose-ui", "compose-material3", "compose-navigation"]
```

### Multi-Module Configuration
- Library: Android Library plugin, published to MavenCentral
- App: Android Application plugin, not published
- Shared Kotlin plugin configurations

### Build Variants
- `debug` - Development with debug logging
- `variant` - Alternative configuration for multi-environment testing
