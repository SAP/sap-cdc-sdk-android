plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlinx-serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.sap.cdc.android.sdk.example"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gigya.android.sample"
//        applicationId = "com.sap.cdc.android.sdk.example"
        minSdk = 26
        //noinspection EditedTargetSdkVersion,OldTargetApi
        targetSdk = 35
        versionCode = 4
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {

//        getByName("debug") {
//            keyAlias = "key0"
//            keyPassword = "android"
//            storeFile = file("keystore/debug-key.keystore")
//            storePassword = "android"
//        }
    }

    buildTypes {
//        getByName("debug") {
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//            isMinifyEnabled = false
//            signingConfig = signingConfigs.getByName("debug")
//            isDebuggable = true
//        }

        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.12"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.ktx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.compose)
    implementation(libs.bundles.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.coil.compose)

    implementation(project(":library"))

    // Used social providers.
    implementation("com.facebook.android:facebook-login:latest.release")
    implementation("com.linecorp.linesdk:linesdk:latest.release")
    implementation("com.tencent.mm.opensdk:wechat-sdk-android-without-mta:6.8.0")
    //noinspection CredentialDependency
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

}