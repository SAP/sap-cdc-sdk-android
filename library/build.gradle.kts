plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
}

group = "com.sap.cdc.android"
version = "0.0.1"

ext["name"] = "SAP Customer Data Cloud for Android"
ext["artifactId"] = "sdk"
ext["description"] = "SAP Customer Data Cloud for Android"
ext["url"] = ""

android {
    namespace = "com.sap.cdc.android.sdk"
    compileSdk = 34

    android.buildFeatures.buildConfig = true

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {

        debug {
            buildConfigField("String", "VERSION", "\"${version}\"")
        }

        release {
            buildConfigField("String", "VERSION", "\"${version}\"")

            isMinifyEnabled = false
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
}

dependencies {

    implementation(libs.androidx.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Http engine, JSON serialization/deserialization, kotlinx.serialization, Logging HTTP requests
    api(libs.bundles.ktor)
    // Jetpack security.
    api(libs.androidx.security.crypto)
    // Jetpack biometric
    api(libs.androidx.biometric)
    // Jetpack work manager
    api(libs.androidx.work.runtime.ktx)
}

apply(from = "../publish-package.gradle")
