# Custom Review Focus

## SAP Customer Data Cloud SDK for Android - Project Context

### System Overview
SAP Customer Data Cloud SDK for Android is an enterprise-grade authentication and identity management SDK built with modern Kotlin and Jetpack Compose. It provides comprehensive authentication lifecycle management including email/password login, social provider integration, biometric authentication, session management, two-factor authentication, passkeys, and WebView-based Screen-Sets integration for SAP's Customer Data Cloud (CDC) platform.

### Architecture Patterns
- **Multi-Module Architecture**: Clear separation between published SDK library, example application, and experimental MRZ reader
- **MVVM + Compose**: Modern Android architecture with reactive StateFlow/SharedFlow patterns
- **Delegate Pattern**: `AuthenticationFlowDelegate` eliminates repository boilerplate while providing direct SDK access and centralized state management
- **DSL-Based Callbacks**: Type-safe Kotlin DSL with advanced side-effects and override patterns for callback composition
- **Event-Driven**: Lifecycle-aware event bus for session and messaging events with automatic cleanup
- **Decoupled Social Providers**: SDK does NOT bundle social provider SDKs; apps implement `IAuthenticationProvider` interface

### Key Technical Components
- **AuthenticationService**: Main SDK entry point providing fluent API for all authentication operations
- **Feature Flows**: Dedicated flow classes per auth method (Login, Register, Social, OTP, TFA, Passkeys, Logout)
- **Session Management**: `SessionService` with secure storage, encryption via Android Keystore, biometric support, optional periodic validation
- **WebBridgeJS**: JavaScript bridge for Screen-Sets with type-safe event callbacks and native provider integration
- **Event Bus**: `CIAMEventBusProvider` with lifecycle-aware and manual subscription patterns
- **Callback System**: Advanced DSL supporting side-effects (doOnSuccess) and overrides (doOnSuccessAndOverride)

### Security & Integration Focus
- **Storage**: Android Keystore for hardware-backed encryption, SecureSharedPreferences for encrypted data persistence
- **Biometric**: BiometricPrompt API integration with hardware-backed keys for session encryption
- **Networking**: Ktor HTTP client with coroutine support, JSON serialization, logging, certificate pinning support
- **Background Work**: WorkManager for session validation with battery-aware scheduling
- **Social Providers**: Google (Credential Manager), Facebook, WeChat, Line - all implemented in app layer

### Project Structure
- **library/**: Published SDK module (MavenCentral: `com.sap.oss.ciam-android-sdk:ciam-android-sdk:0.3.0`)
  - `core/`: API, networking, configuration (SiteConfig, CoreClient, Ktor integration)
  - `feature/`: Authentication flows, session, biometric, providers, screensets, notifications
  - `events/`: Event bus system with lifecycle awareness
  - `storage/`: Secure storage and encryption
- **app/**: Comprehensive example application
  - `feature/auth/`: AuthenticationFlowDelegate, BiometricLifecycleManager, SessionMigrator
  - `feature/provider/`: Social provider implementations (Google, Facebook, WeChat, Line, Passkeys)
  - `navigation/`: Type-safe navigation with Routes sealed classes
  - `ui/`: Jetpack Compose screens, ViewModels, state classes, reusable composables
- **mrz-reader/**: Experimental MRZ scanning module (Phase 1 - data models in progress)
- **Test Projects**: Unit tests (JUnit, Mockito, Ktor Mock), instrumented tests (Compose UI tests)

### Performance & Quality Considerations
- **Coroutines**: All SDK operations return `suspend` functions for non-blocking async operations
- **StateFlow**: Cold reactive streams for UI state with lifecycle-aware collection in Compose
- **Memory Management**: Lifecycle-aware event subscriptions with automatic cleanup, proper ViewModel disposal
- **Network Optimization**: Ktor connection pooling, request/response compression, efficient JSON parsing
- **Type Safety**: Sealed classes for results, data classes for entities, compile-time navigation safety
- **Compose Best Practices**: Side effects properly managed with DisposableEffect, LaunchedEffect, remember

### Deployment & Monitoring
- **Published**: MavenCentral with JReleaser automation
- **Versioning**: Semantic versioning (current: v0.3.0)
- **Build System**: Gradle 8.0+ with Kotlin DSL, version catalog for dependency management
- **Multi-Variant**: Debug and variant build types for multi-environment testing
- **Documentation**: Comprehensive README, Dokka API docs, Memory Bank files
- **CI/CD**: GitHub Actions for automated testing and release
- **Compatibility**: Android API 24+ (Android 7.0+), Kotlin 2.2.21, Compose BOM 2025.12.00

---

## Memory Bank Summary

### Project Identity
**Name:** SAP Customer Data Cloud SDK for Android  
**Type:** Android SDK Library + Example Application  
**Version:** 0.3.0 (published on MavenCentral)  
**Language:** Kotlin 2.2.21  
**Min SDK:** Android API 24 (7.0)  
**License:** Apache 2.0

### Core Architecture Principles
1. **Kotlin-First API**: Coroutines, type-safe DSL, nullable types, extension functions
2. **Decoupled Social Providers**: No bundled SDKs; apps implement `IAuthenticationProvider`
3. **Advanced Callback System**: Two patterns - Side Effects (callbacks first, effects after) and Overrides (override first, callbacks after)
4. **Lifecycle-Aware Events**: Automatic cleanup for Activities/Fragments, manual cleanup for ViewModels
5. **Type Safety**: Sealed classes, data classes, compile-time checks throughout

### Key Components

**Library Module (Published SDK):**
- `AuthenticationService` - Single entry point with fluent API
- `AuthLoginFlow`, `AuthRegisterFlow`, `AuthProviderFlow`, `AuthOtpFlow`, `AuthTFAFlow`, `AuthPasskeysFlow` - Feature flows
- `SessionService` - Session CRUD with encryption
- `SessionSecure` - Biometric encryption/decryption
- `WebBridgeJS` - JavaScript bridge for Screen-Sets
- `CIAMEventBusProvider` - Global event bus
- `IAuthenticationProvider` - Social provider interface
- `AuthCallbacks` - DSL callback system with chains and overrides

**App Module (Example Application):**
- `AuthenticationFlowDelegate` - Direct SDK access + centralized state management
- `BaseViewModel` - Common ViewModel functionality
- Screen ViewModels - Screen-specific logic with StateFlow/SharedFlow
- `NavigationCoordinator` - Type-safe navigation
- `Routes` - Sealed class hierarchy for routes
- Social provider implementations (Google, Facebook, WeChat, Line, Passkeys)

### Critical Patterns

**Callback Registration Order (CRITICAL):**
```kotlin
// Pattern A: Side Effects (user callbacks FIRST)
credentials(credentials) {
    authCallbacks()           // 1. User callbacks
    doOnSuccess { /* side */ } // 2. Side effects
}

// Pattern B: Overrides (override FIRST)
signIn(parameters) {
    doOnAnyAndOverride { /* transform */ }  // 1. Override
    authCallbacks()                         // 2. User callbacks
}
```

**WebView Priority Navigation:**
- LOGIN (100) > LOGOUT (80) > CANCELED (60) > HIDE (40)
- Prevents duplicate navigation from multiple CDC events

**Session Storage Security:**
```
Plain: SecureSharedPreferences → Android Keystore → AES-256-GCM
Biometric: SessionSecure → Biometric Key → Keystore → AES-256-GCM
```

**Provider Session JSON:** Each provider has specific JSON format (Google: `{"idToken":"..."}`, Facebook: `{"authToken":"..."}`)

### Technology Stack
- **Networking:** Ktor 3.3.3 (coroutine-first HTTP client)
- **UI:** Jetpack Compose BOM 2025.12.00, Material 3 1.4.0
- **Architecture:** Lifecycle 2.10.0, Navigation Compose 2.9.6, StateFlow/SharedFlow
- **Security:** Android Keystore, Biometric 1.1.0, SecureSharedPreferences
- **Background:** WorkManager 2.11.0
- **Serialization:** Kotlinx Serialization, org.json 20250517
- **Testing:** JUnit 4.13.2, Mockito 5.20.0, Ktor Mock 3.3.3, Compose UI Test

### Project Status
**Current State:** Production-ready, stable v0.3.0 published  
**Completed Features:** All authentication flows, session management, biometric auth, WebView bridge, event bus, social providers, complete example app  
**In Progress:** MRZ Reader Phase 1 (data models)  
**No Known Issues:** Project fully functional

### Common Pitfalls
1. Wrong callback registration order breaks side effects/overrides
2. Missing WebView bridge disposal causes memory leaks
3. Manual event subscriptions need explicit cleanup in onCleared()
4. Provider session JSON must match CDC server expectations
5. WebView JavaScript bridge requires HTTPS

---

## Core Code Hygiene - Kotlin/Android Specific
- No unused imports (optimize imports on commit)
- No dead/unused code (variables, functions, classes, resources)
- No commented-out code blocks
- No TODO/FIXME comments without associated issue/ticket
- No hardcoded strings that should be in `strings.xml`
- No stray `Log.d`/`println` statements outside `CIAMDebuggable` interface
- No magic numbers: extract to constants (prefer existing constants)
- No formatting-only or unrelated drive-by changes
- Avoid inline comments except `// Arrange`, `// Act`, `// Assert` in tests
- No leaked Android context references in static/singleton scope

## Types & API Surface - Kotlin Specific
- No nullable types (`?`) without explicit null handling
- No `!!` (force unwrap) operators - use safe calls or explicit null checks
- No implicit `Any` types - leverage Kotlin's type inference correctly
- Public API minimal: avoid adding/expanding visibility modifiers unless required
- No `open` classes/functions in library module unless designed for extension
- Sealed classes preferred over enums for type-safe result hierarchies
- Data classes for immutable DTOs, avoid mutable properties in public API

## Function & Module Quality - Kotlin Conventions
- Functions < 40 lines (excluding blank lines), single responsibility
- Prefer expression-body functions for simple operations
- Early returns over deep conditional nesting
- Pure/side-effect-free unless clearly orchestrator (e.g., ViewModel, Flow class)
- Coroutine scopes properly managed (viewModelScope, lifecycleScope)
- No blocking operations in main thread - use `withContext(Dispatchers.IO)`
- Extension functions placed in appropriate extension files
- No cyclic module dependencies

## Architecture & Placement - Android Structure
- Files in correct module (`library/` for SDK, `app/` for examples)
- Library code has zero dependencies on app module
- Resources in proper variant directories (`src/main`, `src/debug`, `src/variant`)
- Feature code organized by domain (auth, provider, session, screensets, etc.)
- ViewModels, StateClasses, Screens co-located by feature
- No business logic in Composables - delegate to ViewModel
- Navigation logic centralized in NavigationCoordinator

## Dependencies - Android/Kotlin
- No new external libraries without justification
- Version catalog (`gradle/libs.versions.toml`) updated for all dependency changes
- No version conflicts in multi-module setup
- Compose BOM for Compose dependencies (no individual version specifications)
- No unused dependencies in `build.gradle.kts`
- SDK module dependencies minimal (Ktor, AndroidX Security, Biometric, WorkManager only)

## Consistency & Naming - Kotlin Style
- PascalCase for classes, interfaces, sealed classes
- camelCase for functions, variables, properties
- SCREAMING_SNAKE_CASE for constants
- Private backing fields prefixed with underscore: `_state` / `state`
- Composables are PascalCase functions (e.g., `EmailSignInScreen()`)
- ViewModel names end with `ViewModel`
- State classes end with `State`
- Flow/Repository classes end with descriptive suffix
- File names match primary class name

## Scope & Change Control
- No unrelated refactors bundled with feature/bug fix
- Callback pattern order preserved (side effects vs overrides)
- No breaking changes to `AuthenticationService` public API
- WebView bridge lifecycle methods not altered without Memory Bank update
- Delegate pattern maintained - no repository layer reintroduction
- No changes to event bus subscription contracts

## Backward Compatibility - SDK Contract
- No breaking changes to public library API without major version bump
- Deprecated methods marked with `@Deprecated` and migration guidance
- Session storage format changes require migration logic
- Provider interface changes need backward compatibility layer
- Event types not removed - only added with care

## Error Handling & Logging - Android Best Practices
- Network errors wrapped in `AuthResult.Error` with `AuthError` details
- No silent exception swallowing - log or propagate
- Try-catch blocks specific, not broad `catch (e: Exception)`
- Coroutine exception handling via `CoroutineExceptionHandler`
- Logging via `CIAMDebuggable.debug()` for SDK, standard Android logging for app
- Sensitive data (passwords, tokens) not logged - use `[Sensitive]` marker concept

## Security Hardening - Android Specific
- No hardcoded API keys/secrets in code
- SiteConfig reads from `strings.xml` securely
- Session data encrypted before storage
- Biometric keys bound to hardware (`setUserAuthenticationRequired`)
- WebView JavaScript bridge validates event data
- No dynamic code execution (`eval` equivalent in Kotlin)
- Certificate pinning configured where applicable
- ProGuard rules preserve SDK public API

## Compose-Specific Quality
- Composables are stateless/pure UI - state hoisted to ViewModel
- Side effects in `LaunchedEffect`, `DisposableEffect`, or `rememberCoroutineScope`
- Navigation events via SharedFlow, not direct NavController calls in Composable
- `remember` for expensive calculations, `rememberSaveable` for config changes
- Modifier parameters last in Composable signatures
- Preview functions provided for non-trivial Composables
- No ViewModel instantiation in Composables (use `viewModel()` or factory)

## Testing Requirements
- New library features have unit tests (JUnit + Mockito)
- Network calls mocked with Ktor MockEngine
- Coroutine tests use `runTest` and test dispatchers
- ViewModels tested with fake delegates
- Compose UI tests for new screens
- Test names follow convention: `methodName_condition_expectedResult`
- No `Thread.sleep` in tests - use proper coroutine test utilities

## Memory Bank Alignment
- Changes to core patterns documented in Memory Bank (callback order, navigation priorities, session security)
- New features added to activeContext.md and progress.md
- Architecture changes reflected in systemPatterns.md
- Dependency updates logged in techContext.md
- Breaking changes require Memory Bank update BEFORE merge

## Reference & Automation
- **Severity Guidelines:**
  - **BLOCKER**: Breaking API change w/out version bump, new dependency w/out justification, missing tests for core logic, memory leak (Context/ViewModel), callback pattern order violation, cyclic dependency
  - **MUST FIX**: Unused code/imports/resources, magic values, hardcoded strings (not in strings.xml), main thread blocking, improper module placement, silent exception swallow, nullable force unwrap (`!!`)
  - **SHOULD**: Naming clarity, Compose best practices, StateFlow usage over LiveData, coroutine scope management, performance micro-optimizations
  - **INFO**: Stylistic nit (formatting, comment style) that doesn't alter behavior

- **Suggested Automated Gates (CI/Bot):**
  - Detect added/removed public API methods in library module
  - Flag new dependencies or version changes in version catalog
  - Enforce Kotlin code style (ktlint/detekt)
  - Max function length (< 40 lines) excluding blank/comment lines
  - Detect `!!` (force unwrap) usage - suggest safe alternatives
  - Detect main thread blocking calls (network, file I/O)
  - Flag broad `catch (e: Exception)` with empty/minimal handling
  - Detect Context leaks in static/singleton scope
  - Verify test coverage for new library code (> 70%)
  - Check for hardcoded strings not in `strings.xml`
  - Validate ProGuard rules for new public APIs
  - Detect missing `@Composable` annotation on UI functions
  - Flag ViewModel instantiation in Composables
