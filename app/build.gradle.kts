plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sap.cdc.android.sdk.example"
        minSdk = 26
        //noinspection EditedTargetSdkVersion,OldTargetApi
        targetSdk = 35
        versionCode = 4
        versionName = "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {

        getByName("debug") {
            keyAlias = findProperty("exampleComposeKeyAlias") as String
            keyPassword = findProperty("exampleComposeKeyPassword") as String
            storeFile = file("keystore/debug-key.keystore")
            storePassword = findProperty("exampleComposeStorePassword") as String
        }
    }

    flavorDimensions.add("client")
    productFlavors {
        create("demo") {
            dimension = "client"
        }

        create("variant") {
            dimension = "client"
            applicationIdSuffix = ".variant"
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
        kotlinCompilerExtensionVersion = "1.5.12"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    namespace = "com.sap.cdc.android.sdk.example"
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
    implementation(libs.facebook.login)
    implementation(libs.linesdk)
    implementation(libs.wechat)

    implementation(libs.bundles.credentials)
    implementation(libs.googleid)

}