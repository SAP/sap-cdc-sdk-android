plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlinx-serialization")
    id("kotlin-parcelize")
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

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-inline:5.0.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // HTTP engine: that handles network requests.
    api(libs.ktor.client.android)
    // JSON serialization and deserialization settings and //is recommended for multiplatform projects
    api(libs.ktor.client.serialization)
    // kotlinx.serialization, which is used for entity //serialization
    api(libs.ktor.serialization.kotlinx.json)
    // Logging HTTP requests
    api(libs.ktor.client.logging.jvm)
    // Jetpack security.
    api(libs.androidx.security.crypto)
    // Jetpack biometric
    api(libs.androidx.biometric)
    // Jetpack work manager
    api(libs.androidx.work.runtime.ktx)
}

apply(from = "../publish-package.gradle")
