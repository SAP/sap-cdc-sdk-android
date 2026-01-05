# Technical Context

## Technology Stack

### Core Technologies

**Language:** Kotlin 2.2.21
- Coroutines for async operations
- Serialization plugin for JSON handling
- Parcelize plugin for Parcelable implementation
- Compose compiler plugin

**Build System:** Gradle 8.0+ with Kotlin DSL
- Version catalog for dependency management
- Multi-module configuration
- Custom build variants (debug, variant)

**Android Platform:**
- Min SDK: 24 (Android 7.0)
- Target SDK: Latest
- Compile SDK: Latest
- Android Gradle Plugin: 8.13.1

### Library Dependencies

#### Networking
**Ktor 3.3.3** - HTTP client
```kotlin
dependencies {
    implementation("io.ktor:ktor-client-android:3.3.3")
    implementation("io.ktor:ktor-client-serialization:3.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")
    implementation("io.ktor:ktor-client-logging-jvm:3.3.3")
}
```

**Why Ktor:**
- Kotlin-native HTTP client
- Coroutine-first design
- Multiplatform support
- Easy mocking for tests
- Content negotiation with JSON

#### Security & Storage
**Android Keystore** - Hardware-backed encryption
- Session encryption
- Biometric key storage
- Secure credential storage

**AndroidX Security** - SecureSharedPreferences
- Encrypted shared preferences
- AES-256-GCM encryption
- Key management via Keystore

**AndroidX Biometric 1.1.0**
- Biometric authentication
- Hardware-backed keys
- Fallback to device credentials

#### Background Work
**WorkManager 2.11.0**
- Session validation scheduling
- Periodic background tasks
- Battery-aware execution
- Guaranteed execution

#### Serialization
**Kotlinx Serialization**
- JSON encoding/decoding
- Type-safe serialization
- Code generation at compile time

**org.json 20250517**
- Legacy JSON handling where needed
- CDC API response parsing

### App Dependencies

#### UI Framework
**Jetpack Compose** (BOM 2025.12.00)
```kotlin
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.12.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.12.1")
}
```

**Material 3 1.4.0**
- Modern Material Design
- Theming support
- Icon library
- Material Icons Extended 1.7.8

#### Architecture Components
**Lifecycle 2.10.0**
```kotlin
dependencies {
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
}
```

**Navigation Compose 2.9.6**
- Type-safe navigation
- Deep linking support
- Nested navigation graphs

**StateFlow/SharedFlow**
- Reactive state management
- Cold/hot flows
- Lifecycle-aware collection

#### Social Providers (App Only)
**Google Sign-In**
- Credential Manager API
- androidx.credentials 1.5.0
- googleid 1.1.1

**Facebook Login** - latest.release
**WeChat SDK** - 6.8.0
**Line SDK** - latest.release

#### Firebase (App Only)
**Firebase BOM 34.6.0**
- Firebase Cloud Messaging
- Push notifications
- FCM token management

#### Image Loading
**Coil 2.7.0** - Compose-native image loading

#### Permissions
**Accompanist Permissions 0.37.3** - Runtime permission handling

### Testing Dependencies

#### Unit Testing
**JUnit 4.13.2** - Test framework
**Kotlin Test** - Kotlin-specific assertions
**Kotlinx Coroutines Test 1.9.0** - Coroutine testing

**Mockito**
- mockito-core 5.20.0
- mockito-kotlin 6.1.0
- mockito-inline 5.2.0 (for final classes)

**Ktor Client Mock 3.3.3** - HTTP client mocking

#### Instrumented Testing
**AndroidX Test**
- junit 1.3.0
- espresso-core 3.7.0

**Compose Testing**
- ui-test-junit4
- ui-test-manifest

### Development Tools

**Dokka 2.1.0** - API documentation generation
**JReleaser 1.21.0** - Library publishing to MavenCentral

## Development Setup

### IDE Requirements
**Android Studio Iguana or later**
- Kotlin plugin enabled
- Compose support
- Gradle sync

### JDK
**Java 11+** required for compilation

### Gradle Configuration

**gradle.properties:**
```properties
kotlin.code.style=official
android.useAndroidX=true
android.enableJetifier=true
org.gradle.jvmargs=-Xmx2048m
```

**Version Catalog Location:** `gradle/libs.versions.toml`

### Build Variants

**Library:**
- No variants (single build)
- Published to MavenCentral

**App:**
- `debug` - Development build with logging
- `variant` - Alternative environment configuration

## API Integration

### SAP CDC REST API

**Base URL Structure:**
```
https://{domain}/accounts.{method}
```

**Domain:** Configurable (default: us1.gigya.com)
- us1.gigya.com (US data center)
- eu1.gigya.com (EU data center)
- au1.gigya.com (Australia data center)
- Custom CNAME support

**Authentication:**
- API Key (required for all requests)
- Session token (for authenticated requests)
- User secret (for server-side calls)

**Request Format:**
- HTTP POST (primary)
- HTTP GET (some endpoints)
- application/x-www-form-urlencoded
- JSON response

**Key Endpoints:**
- `accounts.login` - User login
- `accounts.register` - User registration
- `accounts.socialLogin` - Social provider login
- `accounts.getAccountInfo` - Get user profile
- `accounts.setAccountInfo` - Update user profile
- `accounts.logout` - User logout
- `accounts.verifyLogin` - Session validation

### Request/Response Pattern

**Ktor Client Configuration:**
```kotlin
HttpClient(Android) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        })
    }
    
    install(Logging) {
        logger = Logger.ANDROID
        level = LogLevel.BODY
    }
    
    defaultRequest {
        header("Content-Type", "application/x-www-form-urlencoded")
    }
}
```

**Error Handling:**
- HTTP status codes
- CDC error codes (documented in AuthErrorCodes)
- Network exceptions
- Parsing exceptions

## Security Architecture

### Session Storage

**Plain Session Storage:**
1. Session JSON from CDC
2. Serialize with Kotlinx Serialization
3. Encrypt with SecureSharedPreferences
4. Store in EncryptedSharedPreferences
5. Android Keystore manages encryption keys

**Biometric Session Storage:**
1. Generate biometric-bound key in Keystore
2. Encrypt session with biometric key
3. Store encrypted session
4. Require biometric auth to decrypt

### Encryption Standards

**AES-256-GCM** - Session encryption
**RSA-2048** - Key wrapping (Keystore)
**SHA-256** - Request signing (when enabled)

### Certificate Pinning

**Optional Configuration:**
- Can be enabled via Ktor client config
- Public key pinning
- Certificate pinning
- Backup pins recommended

## Performance Considerations

### Network Optimization
- Connection pooling (Ktor)
- Request/response compression
- Conditional requests (ETags)
- Response caching where appropriate

### Memory Management
- Lifecycle-aware subscriptions
- Proper cleanup in onCleared()
- WeakReference for long-lived callbacks
- Efficient JSON parsing

### Background Work
- WorkManager for session validation
- Constraints: battery, network
- Exponential backoff for retries
- Periodic vs one-time work

## Compatibility

### Android API Levels

**Minimum (24 - Android 7.0):**
- Basic Keystore support
- SharedPreferences
- Basic biometric (fingerprint)

**Recommended (26+ - Android 8.0):**
- Enhanced Keystore features
- Better biometric support
- Notification channels

**Optimal (28+ - Android 9.0):**
- BiometricPrompt API
- Hardware-backed keys guaranteed
- Enhanced security features

### Device Requirements

**For Full Functionality:**
- Internet connectivity (required)
- Biometric hardware (optional, for biometric features)
- Google Play Services (for Google Sign-In)
- Camera (for passkeys, optional)

### Social Provider Compatibility

**Google Sign-In:**
- Requires Google Play Services
- Credential Manager API (Android 14+)
- Fallback to legacy API (older versions)

**Facebook Login:**
- Facebook app installed (optional)
- WebView fallback available

**WeChat:**
- WeChat app required
- China region considerations

**Line:**
- Line app installed (recommended)
- Web fallback available

## Known Limitations

### Platform Constraints
- Android Keystore varies by device manufacturer
- Biometric hardware not guaranteed
- WebView JavaScript bridge requires HTTPS
- Background work subject to battery optimization

### API Constraints
- Rate limiting on CDC servers
- Session expiration (configurable)
- Maximum request size
- Concurrent request limits

### Library Design
- No social SDK bundling (by design)
- Requires app-side provider implementation
- WebView needed for Screen-Sets
- Internet required (no offline mode)

## Migration Considerations

### From Legacy SDK
- Session format differences
- API endpoint changes
- Callback pattern updates
- Provider interface changes

### Version Updates
- Semantic versioning
- Deprecation warnings
- Migration guides in CHANGELOG
- Backward compatibility where possible

## Environment Configuration

### Development
**SiteConfig in strings.xml:**
```xml
<string name="com.sap.ciam.apikey">DEV_API_KEY</string>
<string name="com.sap.ciam.domain">us1.gigya.com</string>
```

### Production
**Build variant strings:**
```
src/release/res/values/strings.xml
src/debug/res/values/strings.xml
```

### Multi-Environment
- Flavor-specific resources
- Build config fields
- Environment switching
- Debug/release configurations

## Debugging Tools

### Logging
**CIAMDebuggable interface:**
- Enable/disable SDK logging
- Ktor logging levels
- WebView debugging
- Network request/response logs

### Chrome DevTools
- WebView inspection
- JavaScript debugging
- Network monitoring
- Console logs

### Android Studio
- Compose preview
- Layout inspector
- Network profiler
- Memory profiler

## Publishing Configuration

### Library Publishing

**JReleaser Configuration:**
- Maven Central deployment
- PGP signing
- Artifact validation
- Release notes generation

**Artifact Details:**
```
groupId: com.sap.oss.ciam-android-sdk
artifactId: ciam-android-sdk
version: 0.3.0
```

**Published Components:**
- AAR library
- Source JAR
- Javadoc JAR
- POM file

### Release Process
1. Version bump in build.gradle.kts
2. Update CHANGELOG.md
3. Run validation script
4. Execute JReleaser
5. Verify on Maven Central
6. Create GitHub release
7. Update documentation
