# Active Context

## Current Focus

**Status:** Memory Bank Initialization Complete

The Memory Bank has been successfully initialized with comprehensive documentation covering all aspects of the SAP Customer Data Cloud SDK for Android project.

## Recent Changes

### Memory Bank Files Created (January 4, 2026)

1. **projectbrief.md** - Project identity, structure, and core principles
2. **productContext.md** - Problem space, solutions, and user journeys
3. **systemPatterns.md** - Architecture patterns and implementation details
4. **techContext.md** - Technology stack and technical specifications
5. **activeContext.md** - This file, tracking current work context
6. **progress.md** - Project status and roadmap

## Active Development Areas

### 1. Core SDK (Library Module)
**Status:** Stable - Version 0.3.0 published to MavenCentral

**Key Features Implemented:**
- AuthenticationService with Kotlin DSL callbacks
- Advanced callback system (side effects + overrides)
- Session management with encryption
- Biometric authentication integration
- WebView bridge for Screen-Sets
- Event bus system (lifecycle-aware)
- Social provider framework (decoupled)
- All authentication flows (login, register, social, OTP, 2FA, passkeys)

**No Active Issues:** Library is production-ready and stable

### 2. Example App (App Module)
**Status:** Complete demonstration of all SDK features

**Architecture:**
- MVVM + Jetpack Compose
- Delegate-based state management via `AuthenticationFlowDelegate`
- Type-safe navigation with deep linking
- Priority-based WebView navigation handling

**Implemented Features:**
- Email/password authentication
- Social provider integration (Google, Facebook, WeChat, Line)
- Screen-Sets integration in Compose
- Biometric session encryption
- Two-factor authentication
- OTP flows
- Passkeys support
- Push notifications

**No Active Issues:** Example app fully functional

### 3. MRZ Reader Module
**Status:** Experimental - Phase 1 in development

**Current Phase:** Data Models (Phase 1)
- MRZ data structures being defined
- Documentation in `mrz-reader/docs/phases/`
- Not yet integrated with main SDK

**Next Steps for MRZ Reader:**
- Complete Phase 1 data models
- Phase 2: OCR integration
- Phase 3: Validation logic
- Phase 4: Integration with CDC SDK

## Next Steps

### Immediate Priorities
None - Memory Bank initialization complete. Project is stable and well-documented.

### Future Enhancements (Potential)

**SDK Enhancements:**
1. Additional social providers (Twitter, LinkedIn, etc.)
2. Enhanced certificate pinning options
3. Offline mode support for certain operations
4. Additional WebAuthn features

**Example App Improvements:**
1. More UI/UX polish
2. Additional usage examples
3. Performance optimization demos
4. Error handling showcases

**MRZ Reader:**
1. Complete remaining phases
2. Integration with CDC identity verification
3. Document extraction from passports/IDs

## Important Patterns & Preferences

### Callback Registration Order

**Pattern A: Side Effects** (User callbacks FIRST, then side effects)
```kotlin
credentials(credentials) {
    authCallbacks()           // User callbacks first
    doOnSuccess { /* side */ } // Side effects after
}
```

**Pattern B: Overrides** (Override FIRST, then user callbacks)
```kotlin
signIn(parameters) {
    doOnAnyAndOverride { /* transform */ }  // Override first
    authCallbacks()                         // User callbacks after
}
```

**Critical:** Order matters! Always register in the correct sequence for each pattern.

### WebView Event Handling

**Priority-Based Navigation:**
- LOGIN (100) - Highest priority
- LOGOUT (80)
- CANCELED (60)
- HIDE (40) - Lowest priority

**Prevents:** Duplicate navigation when CDC fires multiple events (e.g., onLogin + onHide)

### State Management

**Delegate Pattern Preferred:**
- `AuthenticationFlowDelegate` provides direct SDK access
- Manages centralized auth state
- Wraps SDK calls with automatic state updates
- Eliminates repository boilerplate

**ViewModel Pattern:**
- Screen-specific logic only
- Observes delegate's StateFlows
- Emits navigation events via SharedFlow
- Delegates all auth operations to delegate

## Project Insights

### Key Design Decisions

1. **Decoupled Social Providers:** SDK intentionally does NOT bundle social provider SDKs to avoid version lock-in and reduce library size

2. **Callback Patterns:** Two distinct patterns (side effects vs overrides) provide flexibility while maintaining type safety

3. **Event Bus:** Lifecycle-aware event system prevents memory leaks while enabling global communication

4. **Delegate Architecture:** Single `AuthenticationFlowDelegate` replaces traditional repository layer, providing direct SDK access with managed state

5. **WebView Integration:** Priority-based navigation prevents duplicate actions from multiple CDC events

### Common Pitfalls to Avoid

1. **Callback Order:** Registering callbacks in wrong order breaks side effects or overrides
2. **WebView Disposal:** Must properly detach bridge to prevent memory leaks
3. **Event Subscriptions:** Manual subscriptions need explicit cleanup
4. **Provider Sessions:** Each social provider has specific JSON format required by CDC

### Testing Approach

**Library:**
- Unit tests with Mockito
- Ktor MockEngine for network testing
- Coroutine test dispatchers

**App:**
- ViewModel testing with fake delegates
- Compose UI tests
- Manual testing with example app

## Current Environment

**Development Tools:**
- Android Studio Iguana+
- Kotlin 2.2.21
- Gradle 8.0+
- JDK 11+

**Published Version:** 0.3.0 on MavenCentral

**Repository:** https://github.com/SAP/sap-cdc-sdk-android

**Maintainers:** SAP (tal.mirmelshtein@sap.com, sagi.shmuel@sap.com)

## Learnings & Best Practices

### What Works Well

1. **Kotlin DSL Callbacks:** Developers love the type-safe, composable callback system
2. **Delegate Pattern:** Eliminates repository boilerplate while keeping code clean
3. **Event Bus:** Lifecycle-aware events prevent common Android pitfalls
4. **Decoupled Providers:** Flexibility to use any social SDK version
5. **WebView Bridge:** Clean JavaScript ↔ Native communication

### Areas for Future Consideration

1. **Offline Support:** Consider caching strategies for certain operations
2. **Certificate Pinning:** Make it easier to configure for production apps
3. **Error Recovery:** More sophisticated retry logic for network failures
4. **Testing Utilities:** Provide more mock helpers for developers

## Documentation Status

**Complete:**
- Root README.md - SDK usage and API reference
- App README.md - Example app architecture and patterns
- CONTRIBUTING.md - Contribution guidelines
- Memory Bank files - Comprehensive project documentation

**Up-to-Date:**
- All documentation reflects current 0.3.0 release
- Code examples tested and verified
- Architecture diagrams accurate

## Notes for Future Development

### When Adding New Features

1. Update Memory Bank files as needed
2. Add examples to example app
3. Update root README.md with API changes
4. Consider backward compatibility
5. Update CHANGELOG.md
6. Version bump following semantic versioning

### When Updating Dependencies

1. Test thoroughly with new versions
2. Update gradle/libs.versions.toml
3. Check for breaking API changes
4. Update documentation if APIs change
5. Consider impact on minimum SDK version

### When Publishing New Version

1. Follow release process in techContext.md
2. Update version in library/build.gradle.kts
3. Run validation scripts
4. Execute JReleaser
5. Create GitHub release
6. Update documentation

## Contact & Support

**Issues:** https://github.com/SAP/sap-cdc-sdk-android/issues
**Email:** tal.mirmelshtein@sap.com, sagi.shmuel@sap.com
**License:** Apache 2.0
