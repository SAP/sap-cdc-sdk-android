plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.dokka)
}

group = "com.sap.cdc.android"
version = "0.3.0"

ext["name"] = "SAP Customer Data Cloud for Android"
ext["artifactId"] = "sdk"
ext["description"] = "SAP Customer Data Cloud for Android"
ext["url"] = ""

android {
    namespace = "com.sap.cdc.android.sdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    testImplementation(libs.mockito.core)
    testImplementation(libs.ktor.client.mock)

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
    // Browser (CustomTabs)
    api(libs.androidx.browser)
}

tasks.dokkaHtml {
    outputDirectory.set(layout.buildDirectory.dir("docs/dokka").get().asFile)
}

tasks.assemble {
    dependsOn(tasks.dokkaHtml)
}

apply(from = "../publish-package.gradle")
