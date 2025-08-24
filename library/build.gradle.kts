plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.dokka)
    alias(libs.plugins.jreleaser)
}

group = "com.sap.cdc.android"
version = "0.3.0"

ext["name"] = "SAP Customer Data Cloud for Android"
ext["artifactId"] = "sdk"
ext["description"] = "SAP Customer Data Cloud for Android"
ext["url"] = ""

android {
    namespace = "com.sap.cdc.android.sdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        val testApiKey: String = project.findProperty("testApiKey") as String? ?: ""
        buildConfigField("String", "TEST_API_KEY", "\"$testApiKey\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    packaging {
        resources {
            excludes.add("META-INF/LICENSE.md")
            excludes.add("META-INF/LICENSE-notice.md")
        }
    }

    publishing {
        singleVariant("release")
    }
}

dependencies {

    implementation(libs.androidx.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.core)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.json)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Http engine, JSON serialization/deserialization, kotlinx.serialization, Logging HTTP requests
    api(libs.bundles.ktor)
    // Jetpack biometric
    api(libs.androidx.biometric)
    // Jetpack work manager
    api(libs.androidx.work.runtime.ktx)
    // Browser (CustomTabs)
    api(libs.androidx.browser)
}
