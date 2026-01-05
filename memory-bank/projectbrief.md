# Project Brief

## Project Identity

**Name:** SAP Customer Data Cloud SDK for Android  
**Type:** Android SDK Library + Example Application  
**License:** Apache 2.0  
**Repository:** https://github.com/SAP/sap-cdc-sdk-android  
**Primary Language:** Kotlin  
**Maintainers:** SAP (tal.mirmelshtein@sap.com, sagi.shmuel@sap.com)

## Core Purpose

Provide a comprehensive, modern Android SDK for integrating SAP Customer Data Cloud (CDC/CIAM) services into Android applications. The SDK offers type-safe Kotlin DSL for authentication flows, session management, and user account operations with advanced callback chaining and transformation capabilities.

## Project Structure

This is a **multi-module Android project** with three main components:

### 1. Library Module (`library/`)
The core SDK published to MavenCentral as `com.sap.oss.ciam-android-sdk:ciam-android-sdk`

**Key Responsibilities:**
- Authentication service with Kotlin DSL callbacks
- Session management with validation and encryption
- Biometric authentication integration
- Social provider framework (decoupled from native SDKs)
- WebView-based Screen-Sets with JavaScript bridge
- Push notification handling
- Event bus system for lifecycle-aware events

### 2. App Module (`app/`)
Comprehensive example application demonstrating SDK integration

**Architecture:** MVVM + Jetpack Compose + Delegate Pattern

**Key Features:**
- Complete authentication flows (login, register, social, OTP, 2FA)
- Delegate-based state management (`AuthenticationFlowDelegate`)
- ScreenSets integration in Compose
- Biometric authentication demo
- Type-safe navigation with deep linking
- Multiple social providers (Google, Facebook, WeChat, Line)

### 3. MRZ Reader Module (`mrz-reader/`)
Experimental module for machine-readable zone (MRZ) scanning from passports/IDs

**Status:** In development (Phase 1 - Data Models)

## Key Design Principles

### 1. Modern Kotlin-First API
- Coroutine-based async operations
- Type-safe DSL for callbacks and configuration
- Nullable types for optional parameters
- Extension functions for enhanced usability

### 2. Decoupled Social Provider Architecture
SDK does **NOT** bundle social provider SDKs (Facebook, Google, etc.)

**Rationale:**
- Avoid version lock-in
- Let developers use any SDK version
- Reduce SDK size and dependencies
- Implement `IAuthenticationProvider` interface in app code

### 3. Advanced Callback System
Two distinct patterns for callback composition:

**Pattern A: Side Effects** (register user callbacks FIRST, add side effects AFTER)
```kotlin
credentials(credentials) {
    authCallbacks()           // User callbacks first
    doOnSuccess { /* side */ } // Side effects after
}
```

**Pattern B: Overrides** (register override FIRST, user callbacks AFTER)
```kotlin
signIn(parameters) {
    doOnAnyAndOverride { /* transform */ }  // Override first
    authCallbacks()                         // User callbacks after
}
```

### 4. Lifecycle-Aware Event System
Global event bus for session and messaging events with automatic cleanup for Android components

### 5. Type Safety Throughout
- Sealed classes for authentication results
- Data classes for API entities
- Compile-time navigation safety
- No reflection where avoidable

## Target Platform

**Minimum Requirements:**
- Android API Level 24+ (Android 7.0)
- Kotlin 1.9+
- Java 11+
- Gradle 8.0+

**Current Versions:**
- Kotlin: 2.2.21
- Compose BOM: 2025.12.00
- Android Gradle Plugin: 8.13.1
- Ktor: 3.3.3 (HTTP client)

## Distribution

**Library:** Published to MavenCentral  
**Version:** 0.3.0 (latest)  
**Artifact:** `com.sap.oss.ciam-android-sdk:ciam-android-sdk`

## Development Workflow

**Build System:** Gradle with Kotlin DSL  
**IDE:** Android Studio Iguana or later  
**Version Catalog:** `gradle/libs.versions.toml` for dependency management  
**Build Variants:** debug, variant (for multi-environment testing)  
**Release:** JReleaser configured for library publishing

## Testing Strategy

**Library Testing:**
- Unit tests with JUnit and Mockito
- Ktor mock client for network testing
- Coroutine testing

**App Testing:**
- Instrumented tests for Android components
- UI tests with Compose test framework
- Manual testing with example app

## Documentation

**Primary:** README.md files at root and app level  
**API Docs:** Generated with Dokka  
**Contribution Guide:** CONTRIBUTING.md, CONTRIBUTING_USING_GENAI.md  
**Code Standards:** REUSE compliance for licensing  
**Additional:** Phase-based documentation in `mrz-reader/docs/`

## Key Stakeholders

**Users:**
- Android developers integrating SAP CDC
- Enterprise mobile app developers
- Identity and access management teams

**Contributors:**
- Open source community
- SAP internal teams
- AI-assisted development (with guidelines)

## Success Metrics

1. **Developer Experience:** Easy integration, clear documentation, type-safe APIs
2. **Reliability:** Stable authentication flows, proper error handling
3. **Flexibility:** Support multiple authentication methods and providers
4. **Maintainability:** Clean architecture, testable code, minimal dependencies
5. **Security:** Secure session handling, biometric encryption, proper key storage
