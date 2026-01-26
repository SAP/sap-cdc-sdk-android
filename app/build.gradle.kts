plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

android {
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sap.cdc.bitsnbytes"
        minSdk = 26
        //noinspection EditedTargetSdkVersion,OldTargetApi
        targetSdk = 35
        versionCode = 5
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        getByName("debug") {
            val keyAlias = findProperty("exampleComposeKeyAlias") as? String
            val keyPassword = findProperty("exampleComposeKeyPassword") as? String
            val storePassword = findProperty("exampleComposeStorePassword") as? String
            
            if (keyAlias != null && keyPassword != null && storePassword != null) {
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
                this.storeFile = file("keystore/debug")
                this.storePassword = storePassword
            }
        }
    }

    flavorDimensions.add("sso")
    productFlavors {
        create("demo") {
            dimension = "sso"
            applicationIdSuffix = ".demo"
        }

        create("variant") {
            dimension = "sso"
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
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.12"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    namespace = "com.sap.cdc.bitsnbytes"
}

dependencies {

    implementation(libs.androidx.ktx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.compose)
    implementation(libs.bundles.material)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.camera.lifecycle)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.coil.compose)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    
    implementation(project(":library"))
    implementation(project(":mrz-reader"))
    
    // CameraX dependencies (needed for test activity since mrz-reader uses 'implementation')
    implementation(libs.camerax.core)
    implementation(libs.camerax.view)

    // Used social providers.
    implementation(libs.facebook.login)
    implementation(libs.linesdk)
    implementation(libs.wechat)

    implementation(libs.bundles.credentials)
    implementation(libs.googleid)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.accompanist.permissions)
}
