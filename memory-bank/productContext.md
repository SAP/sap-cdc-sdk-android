# Product Context

## What Problem Does This Solve?

### The Authentication Integration Challenge

Mobile applications need to integrate with identity providers for user authentication, but this integration is complex:

1. **Multiple Authentication Methods:** Email/password, social login, biometrics, OTP, 2FA, passkeys
2. **Session Management:** Secure storage, validation, refresh, expiration handling
3. **Platform Differences:** Each provider has unique SDK, API, and flow requirements
4. **Security Requirements:** Encryption, secure storage, biometric integration
5. **State Synchronization:** Keep UI in sync with authentication state across app
6. **Complex Flows:** Account linking, interrupted flows, context-aware navigation

### SAP CDC Backend

SAP Customer Data Cloud (formerly Gigya) is an enterprise identity and customer data management platform. It provides:
- Centralized user authentication and profile management
- Multi-provider social login orchestration
- Compliance and consent management
- User data storage and retrieval
- Customizable authentication flows (Screen-Sets)

## What This SDK Provides

### For Android Developers

**Before This SDK:**
- Manual REST API integration with CDC servers
- Custom callback handling and error management
- Manual session lifecycle management
- Complex WebView integration for Screen-Sets
- Separate integration for each social provider
- Manual state synchronization across app

**With This SDK:**
- **Type-Safe Kotlin DSL:** Compile-time safety for authentication flows
- **Automatic Session Management:** Secure storage, validation, encryption
- **Callback Chaining:** Compose multiple callbacks with side-effects and transformations
- **WebView Bridge:** JavaScript bridge for Screen-Sets with native integration
- **Event Bus:** Lifecycle-aware event system for session and messaging events
- **Biometric Integration:** Built-in biometric authentication and session encryption
- **Provider Abstraction:** Unified interface for all social providers
- **Coroutine Support:** Modern async/await patterns

### User Experience Goals

**For End Users:**
1. **Seamless Authentication:** Quick, secure login across multiple methods
2. **Social Login:** One-tap login with Google, Facebook, WeChat, Line
3. **Security Options:** Biometric authentication for sensitive operations
4. **Persistent Sessions:** Stay logged in securely across app restarts
5. **Account Linking:** Connect multiple login methods to one account
6. **Customizable UI:** Branded authentication screens via Screen-Sets

**For Developers:**
1. **Quick Integration:** Add authentication in hours, not days
2. **Clean Code:** Declarative DSL reduces boilerplate
3. **Flexible:** Works with any architecture (MVVM, MVI, etc.)
4. **Testable:** Mock-friendly design for unit testing
5. **Maintainable:** Clear separation of concerns
6. **Future-Proof:** New auth methods added without breaking changes

## How It Works

### Architecture Flow

```
User Action → App UI (Compose/Views)
    ↓
ViewModel/Presenter
    ↓
AuthenticationService (SDK Entry Point)
    ↓
Authentication Flow (login/register/social/etc.)
    ↓
Network Client (Ktor) → CDC API Server
    ↓
Response Processing
    ↓
Callback Chain Execution
    ↓
State Update → UI Refresh
```

### Key User Journeys

#### 1. Email/Password Login
```
User enters credentials
    ↓
ViewModel calls flowDelegate.login()
    ↓
SDK validates and sends to CDC server
    ↓
Success → Session stored securely → Navigate to home
Error → Show error message
TFA Required → Navigate to 2FA screen
```

#### 2. Social Login (e.g., Google)
```
User taps "Sign in with Google"
    ↓
App's GoogleAuthenticationProvider handles native flow
    ↓
Provider returns auth token to SDK
    ↓
SDK sends token to CDC server for validation
    ↓
Success → Session created → Navigate to home
Account Linking → Show linking options
Error → Show error message
```

#### 3. Screen-Sets (WebView)
```
Load Screen-Set URL in WebView
    ↓
WebBridgeJS attaches JavaScript bridge
    ↓
User interacts with web form (login/register)
    ↓
JavaScript events sent to native via bridge
    ↓
Native code processes events (onLogin, onError, etc.)
    ↓
Navigate based on event type
```

#### 4. Biometric Session Encryption
```
User enables biometric lock
    ↓
SDK encrypts session with biometric key
    ↓
App restart → Biometric prompt shown
    ↓
User authenticates → Session decrypted
    ↓
App continues with authenticated session
```

## Core Value Propositions

### 1. Developer Productivity
**Time Savings:** What takes days with raw API integration takes hours with SDK
**Code Quality:** Type-safe DSL prevents runtime errors
**Testing:** Easy to mock and test authentication flows

### 2. Security by Default
**Secure Storage:** Android Keystore for session encryption
**Biometric Integration:** Hardware-backed authentication
**Session Validation:** Automatic validation and refresh
**Certificate Pinning:** Optional for enhanced security

### 3. Flexibility Without Complexity
**Multiple Auth Methods:** Support all CDC authentication types
**Custom Providers:** Implement any social provider
**Callback Composition:** Build complex flows from simple callbacks
**Event System:** React to auth events anywhere in app

### 4. Modern Android Best Practices
**Kotlin-First:** Idiomatic Kotlin with coroutines
**Jetpack Compatible:** Works with Compose, ViewModel, Navigation
**Lifecycle-Aware:** Automatic cleanup prevents leaks
**Material Design:** Example app demonstrates modern UI patterns

## Success Stories

### What Users Can Build

1. **E-Commerce Apps:** Secure checkout with biometric authentication
2. **Banking Apps:** Multi-factor authentication with session validation
3. **Social Apps:** Quick social login with account linking
4. **Enterprise Apps:** SSO integration with corporate identity providers
5. **Content Apps:** Persistent sessions with customizable branding

### Integration Benefits

- **Reduced Development Time:** 70% faster than manual REST integration
- **Improved Security:** Enterprise-grade security out of the box
- **Better UX:** Consistent authentication experience across features
- **Easier Maintenance:** SDK updates handle API changes
- **Scalability:** Same patterns work for simple and complex flows
