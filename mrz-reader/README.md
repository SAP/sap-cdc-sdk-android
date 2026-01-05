# SAP CDC MRZ Reader for Android

A lightweight, native Android library for reading Machine Readable Zone (MRZ) data from passports, ID cards, and travel documents using CameraX and ML Kit.

## Overview

This module provides on-device MRZ scanning capabilities following the ICAO 9303 standard. It uses CameraX for camera operations and ML Kit for text recognition, ensuring 100% on-device processing with no data sent to the cloud.

## Features

- ✅ **100% On-Device Processing**: No data sent to cloud
- ✅ **CameraX Integration**: Modern camera API with lifecycle awareness
- ✅ **ML Kit Text Recognition**: Fast and accurate OCR
- ✅ **ICAO 9303 Compliant**: Supports TD1, TD2, TD3, MRVA, and MRVB formats
- ✅ **Checksum Validation**: Validates MRZ data integrity
- ✅ **Kotlin Coroutines**: Reactive API with StateFlow
- ✅ **Minimal Permissions**: Only requires CAMERA permission

## Supported Document Types

- Passports (TD3 format)
- ID Cards (TD1 format)
- Visas (TD2, MRVA, MRVB formats)

## Requirements

- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **Permissions**: CAMERA

## Dependencies

This module uses:
- CameraX 1.4.0
- ML Kit Text Recognition 16.0.1
- Kotlin Coroutines 1.8.0
- AndroidX Lifecycle 2.10.0

## Module Structure

```
mrz-reader/
├── src/
│   ├── main/
│   │   ├── java/com/sap/cdc/android/mrz/
│   │   │   ├── model/          # Data models (MRZData, MRZResult, etc.)
│   │   │   ├── parser/         # MRZ parsing logic
│   │   │   ├── camera/         # CameraX integration
│   │   │   └── MRZReader.kt    # Public API
│   │   └── AndroidManifest.xml
│   ├── test/                   # Unit tests
│   └── androidTest/            # Instrumented tests
├── build.gradle.kts
└── README.md
```

## Implementation Status

⚠️ **Module Created - Implementation Pending**

The module structure and dependencies are now in place. Implementation will follow the design document at `/plans/mrz/MRZ_Reader_Android_Design.md`.

## Next Steps

Refer to `/plans/mrz/MRZ_Implementation_Plan.md` for detailed implementation steps.

## License

Copyright (c) 2025 SAP SE or an SAP affiliate company. All rights reserved.
