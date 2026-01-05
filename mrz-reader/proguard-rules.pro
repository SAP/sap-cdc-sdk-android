# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep MRZ data models
-keep class com.sap.cdc.android.mrz.model.** { *; }

# Keep public API
-keep public class com.sap.cdc.android.mrz.MRZReader { *; }
-keep public class com.sap.cdc.android.mrz.MRZReaderBuilder { *; }

# CameraX rules
-keep class androidx.camera.** { *; }

# ML Kit rules
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.** { *; }
