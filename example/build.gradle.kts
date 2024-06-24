
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlinx-serialization")
}

android {
    namespace = "com.sap.cdc.android.sdk.example"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sap.cdc.android.sdk.example"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {

        getByName("debug") {
            keyAlias = "key0"
            keyPassword = "android"
            storeFile = file("keystore/debug-key.keystore")
            storePassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
        }

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
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.runtime:runtime-livedata:1.6.8")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.8")


    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation(project(":sdk-authentication"))
    implementation(project(":sdk-sceensets"))

    // Used social providers.
    implementation("com.facebook.android:facebook-login:latest.release")
    implementation("com.linecorp.linesdk:linesdk:latest.release")
    implementation("com.tencent.mm.opensdk:wechat-sdk-android-without-mta:6.8.0")
    //noinspection CredentialDependency
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")

}