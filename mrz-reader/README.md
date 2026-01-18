# SAP CDC MRZ Reader for Android

A lightweight, interface-based Android library for reading Machine Readable Zone (MRZ) data from passports, ID cards, and travel documents using ML Kit.

## Overview

This module provides an **interface-based API** for on-device MRZ scanning following the ICAO 9303 standard. The module uses ML Kit for text recognition and provides MRZ parsing capabilities, while **you maintain full control** over camera implementation and UI.

## Features

- ✅ **Interface-Based API**: You manage CameraX, we process the images
- ✅ **100% On-Device Processing**: No data sent to cloud
- ✅ **ML Kit Text Recognition**: Fast and accurate OCR (bundled)
- ✅ **ICAO 9303 Compliant**: Supports TD1, TD2, TD3 formats
- ✅ **Checksum Validation**: Validates MRZ data integrity per ICAO standard
- ✅ **Kotlin Coroutines**: Suspend functions for async processing
- ✅ **Flexible Integration**: Works with any camera implementation or image source
- ✅ **Configurable**: Adjust accuracy, performance, and debug settings

## Supported Document Types

- 🛂 Passports (TD3 format - 2 lines, 44 characters each)
- 🪪 ID Cards (TD1 format - 3 lines, 30 characters each)
- 📋 Visas (TD2 format - 2 lines, 36 characters each)

## Architecture

The module provides a **processing interface** rather than a complete camera solution:

```
┌─────────────────────────────┐
│   Your App                  │
│   - CameraX setup           │
│   - UI/Activity             │
│   - Camera lifecycle        │
└────────────┬────────────────┘
             │ ImageProxy
             ↓
┌─────────────────────────────┐
│   MRZ SDK                   │
│   - MRZImageProcessor       │
│   - ML Kit OCR              │
│   - MRZ Parsing             │
│   - Returns MRZResult       │
└─────────────────────────────┘
```

## Quick Start

### 1. Add Dependency

```kotlin
dependencies {
    implementation(project(":mrz-reader"))
    
    // Your CameraX dependencies
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
}
```

### 2. Create Processor

```kotlin
import com.sap.cdc.android.mrz.MRZImageProcessor
import com.sap.cdc.android.mrz.MRZProcessorConfig

// Create processor
val mrzProcessor = MRZImageProcessor.create(
    context = this,
    config = MRZProcessorConfig.BALANCED
)
```

### 3. Setup Your Camera

```kotlin
// Setup your CameraX implementation as normal
val imageAnalyzer = ImageAnalysis.Builder()
    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
    .build()
    .also { analysis ->
        analysis.setAnalyzer(executor) { imageProxy ->
            lifecycleScope.launch {
                // Process with MRZ SDK
                when (val result = mrzProcessor.processImage(imageProxy)) {
                    is MRZResult.Success -> {
                        val data = result.data
                        println("Found: ${data.fullName}")
                        // Handle success
                    }
                    is MRZResult.Error -> {
                        // Handle error
                    }
                    is MRZResult.Scanning -> {
                        // Still scanning
                    }
                }
                imageProxy.close()
            }
        }
    }
```

### 4. Handle Results

```kotlin
when (val result = mrzProcessor.processImage(imageProxy)) {
    is MRZResult.Success -> {
        val data = result.data
        // Access parsed data
        println("Name: ${data.fullName}")
        println("Document: ${data.documentNumber}")
        println("Nationality: ${data.nationality}")
        println("Birth Date: ${data.dateOfBirth}")
        println("Expiration: ${data.expirationDate}")
        println("Valid: ${data.isValid}")
    }
    is MRZResult.Error -> {
        println("Error: ${result.message}")
    }
    is MRZResult.Scanning -> {
        // Continue scanning
    }
}
```

## Configuration

Adjust processing behavior with predefined or custom configs:

```kotlin
// High accuracy (slower, more reliable)
MRZProcessorConfig.HIGH_ACCURACY

// Balanced (default)
MRZProcessorConfig.BALANCED

// Performance (faster, may miss some)
MRZProcessorConfig.PERFORMANCE

// Debug (with logging)
MRZProcessorConfig.DEBUG

// Custom configuration
MRZProcessorConfig(
    confidenceThreshold = 0.8f,
    debugMode = true,
    minTextLength = 30,
    maxTextLength = 50,
    allowPartialMatch = false
)
```

## Requirements

- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **Permissions**: CAMERA (managed by your app)
- **Dependencies**: CameraX (managed by your app)

## Module Structure

```
mrz-reader/
├── src/main/java/com/sap/cdc/android/mrz/
│   ├── MRZImageProcessor.kt        # Public API interface
│   ├── MRZProcessorConfig.kt       # Configuration options
│   ├── MRZImageProcessorImpl.kt    # Implementation
│   ├── model/                      # Data models
│   │   ├── MRZData.kt             # Parsed MRZ data
│   │   ├── MRZResult.kt           # Result sealed class
│   │   ├── DocumentType.kt        # Document types
│   │   └── Gender.kt              # Gender enum
│   ├── parser/                     # MRZ parsing logic
│   │   ├── MRZParser.kt           # Parser interface
│   │   ├── MRZParserFactory.kt    # Parser factory
│   │   ├── TD1Parser.kt           # ID card parser
│   │   ├── TD2Parser.kt           # Visa parser
│   │   ├── TD3Parser.kt           # Passport parser
│   │   └── ...                    # Validation utilities
│   └── recognition/                # ML Kit integration
│       └── MRZTextRecognizer.kt   # Text recognition
├── docs/
│   └── INTEGRATION_GUIDE.md       # Detailed integration guide
└── README.md                       # This file
```

## Documentation

- **[Integration Guide](docs/INTEGRATION_GUIDE.md)**: Detailed integration instructions
- **[Example Implementation](../app/src/main/java/com/sap/cdc/bitsnbytes/feature/camera/CameraTestActivity.kt)**: Complete working example

## Key Benefits

### For SDK Suppliers (You)
- ✅ No Activity or View to maintain
- ✅ Clean interface-based API
- ✅ Client manages their own camera implementation
- ✅ Focus on core MRZ processing logic

### For SDK Consumers (Clients)
- ✅ Full control over camera and UI
- ✅ Easy integration into existing camera flows
- ✅ Works with any camera implementation
- ✅ Process images from any source (camera, gallery, etc.)

## Performance Tips

1. **Frame Throttling**: Process every Nth frame to avoid overload
2. **Background Processing**: Use coroutines for async processing
3. **Resource Cleanup**: Call `processor.release()` when done
4. **Configuration**: Use `PERFORMANCE` config for faster processing

## Next Steps

## License

Copyright (c) 2025 SAP SE or an SAP affiliate company. All rights reserved.
