plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlinx-serialization")
    id("kotlin-parcelize")
}

group = "com.sap.cdc.android"
version = "0.0.1"

ext["name"] = "SAP-CDC-Android-SDK"
ext["artifactId"] = "sdk"
ext["description"] = "SAP CX-CDC (Gigya) Android SDK"
ext["url"] = ""

android {
    namespace = "com.sap.cdc.android.sdk.core"
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

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-inline:5.0.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // HTTP engine: that handles network requests.
    api("io.ktor:ktor-client-android:2.3.5")
    // JSON serialization and deserialization settings and //is recommended for multiplatform projects
    api("io.ktor:ktor-client-serialization:2.3.5")
    // kotlinx.serialization, which is used for entity //serialization
    api("io.ktor:ktor-serialization-kotlinx-json:2.3.5")
    // Logging HTTP requests
    api("io.ktor:ktor-client-logging-jvm:2.3.5")

    api("androidx.security:security-crypto:1.0.0")

    api("androidx.biometric:biometric:1.1.0")

    api("androidx.work:work-runtime-ktx:2.9.1")
}

apply(from = "../publish-package.gradle")
