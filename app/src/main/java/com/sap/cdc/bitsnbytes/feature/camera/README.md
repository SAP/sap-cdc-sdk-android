# Camera Test Activity

## Overview

This directory contains a **development/testing activity** for verifying the CameraX integration from the `mrz-reader` module. This is **NOT** part of the main application flow and is provided solely for testing the camera functionality on physical devices.

## Purpose

The `CameraTestActivity` demonstrates:
- CameraX initialization and lifecycle management
- Real-time camera preview
- Frame processing with throttling
- Camera state monitoring
- Configuration switching (Performance/Default/High Quality)
- Flash/torch control
- Camera permission handling

## Launching the Test Activity

### Option 1: From Launcher
After installing the app, you'll see **two launcher icons**:
1. **Bits N Bytes** - The main application
2. **Camera Test** - The camera testing activity

Simply tap the "Camera Test" icon to launch.

### Option 2: From Android Studio
1. Open the project in Android Studio
2. Select `app` module
3. In the run configuration dropdown, you'll see:
   - `.ui.activity.MainActivity`
   - `.feature.camera.CameraTestActivity` ← Select this
4. Click Run

### Option 3: Via ADB
```bash
adb shell am start -n com.sap.cdc.bitsnbytes.demo/.feature.camera.CameraTestActivity
```

## Using the Test Activity

### 1. Grant Camera Permission
- On first launch, the app will request camera permission
- Tap "Grant Permission" button
- Accept the permission dialog

### 2. Camera Preview
Once permission is granted:
- Live camera preview appears
- Status overlay shows:
  - Camera state (Idle/Initializing/Active/Error)
  - Number of frames processed
  
### 3. Configuration Testing
At the bottom of the screen, three configuration presets are available:

#### Performance Mode
- **Resolution:** 960x540
- **Throttle:** Every 5th frame (~6 FPS)
- **Use Case:** Older devices, battery saving

#### Default Mode
- **Resolution:** 1280x720
- **Throttle:** Every 3rd frame (~10 FPS)
- **Use Case:** Balanced performance (recommended)

#### High Quality Mode
- **Resolution:** 1920x1080
- **Throttle:** Every 2nd frame (~15 FPS)
- **Use Case:** High-end devices, best OCR accuracy

### 4. Flash Control
- Tap "Flash ON/OFF" button to toggle the camera flash
- If device has no flash, a toast message will appear

### 5. Monitoring
Watch the frame counter to verify:
- Camera is actively processing frames
- Frame throttling is working correctly
- Performance characteristics of each config

## What to Verify

### ✅ Successful Test Criteria

1. **Camera Initialization**
   - Status changes: Idle → Initializing → Active
   - No error states
   - Preview displays correctly

2. **Frame Processing**
   - Frame counter increments continuously
   - Counter speed reflects throttle setting:
     - Performance: Slowest increment
     - Default: Medium increment
     - High Quality: Fastest increment

3. **Configuration Switching**
   - Selecting different configs restarts camera
   - Frame counter resets to 0
   - Preview remains stable
   - No crashes or errors

4. **Flash Control**
   - Flash toggles on/off (if device supports)
   - Appropriate toast messages

5. **Lifecycle Handling**
   - Rotating device: Camera restarts correctly
   - Backgrounding app: Camera stops
   - Returning to app: Camera resumes
   - Closing app: No memory leaks

### ❌ Potential Issues

1. **Camera Unavailable**
   - Error: "Camera not available on this device"
   - Likely: Emulator without camera support
   - Solution: Use physical device

2. **Permission Denied**
   - Error: "Camera permission not granted"
   - Solution: Grant permission from Settings

3. **Initialization Failed**
   - Error message with exception details
   - Check logcat for full stack trace
   - May indicate device-specific camera issues

4. **Frame Counter Not Incrementing**
   - Camera may not be properly bound
   - Check device camera app works
   - Try restarting the test activity

## Device Requirements

- **Android API 26+** (Android 8.0 Oreo)
- **Physical device with rear camera** (emulator support limited)
- **Camera permission** granted

## Removing Before Production

**IMPORTANT:** Before releasing the app to production:

1. Remove the launcher intent-filter from `AndroidManifest.xml`:
   ```xml
   <!-- Remove this intent-filter -->
   <intent-filter>
       <action android:name="android.intent.action.MAIN" />
       <category android:name="android.intent.category.LAUNCHER" />
   </intent-filter>
   ```

2. Or delete the entire activity declaration if not needed:
   ```xml
   <!-- Delete this entire <activity> block -->
   <activity
       android:name=".feature.camera.CameraTestActivity"
       ...
   </activity>
   ```

3. Optionally delete this entire directory:
   ```
   app/src/main/java/com/sap/cdc/bitsnbytes/feature/camera/
   ```

## Integration Notes

This test activity uses the production `MRZCameraManager` from the `mrz-reader` module. Any issues found here will likely affect the real MRZ scanning implementation.

### Architecture

```
CameraTestActivity (Compose)
    ↓
MRZCameraManager (mrz-reader module)
    ↓
CameraX (androidx.camera)
    ↓
Camera2 API (Android)
```

### What's Being Tested

✅ Camera initialization  
✅ Lifecycle management  
✅ Preview rendering  
✅ Frame analysis pipeline  
✅ Configuration switching  
✅ Resource cleanup  
✅ Error handling  
✅ State management  

### What's NOT Tested

❌ OCR/Text recognition (Phase 3.2)  
❌ MRZ parsing (already unit tested)  
❌ Integration with actual document scanning  

## Troubleshooting

### Camera Preview is Black
- Check camera permission is granted
- Verify device camera works in other apps
- Check logcat for binding errors

### App Crashes on Launch
- Check Android Studio logcat
- Look for CameraX initialization errors
- Verify device meets minimum requirements

### Frame Counter Stuck at 0
- Camera may not be bound correctly
- Check camera state shows "Active"
- Try switching configurations
- Restart activity

### Poor Performance/Lag
- Try "Performance" configuration
- Check device isn't overheating
- Close other apps
- Frame throttling may need adjustment

## Next Steps

After verifying camera functionality:
1. Proceed with **Phase 3.2: ML Kit OCR Integration**
2. Integrate text recognition into the frame processor
3. Connect OCR output to MRZ parsers
4. Build full document scanning flow

## Questions?

This is a testing utility. For production MRZ scanning implementation, refer to:
- `mrz-reader/docs/phases/Phase_3.1_CameraX_Integration.md`
- `mrz-reader/docs/phases/Phase_3.1_IMPLEMENTATION_SUMMARY.md`
