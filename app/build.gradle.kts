plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.rgupta.tonalwidgets"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rgupta.tonalwidgets"
        minSdk = 31          // Android 12+ — needed for Material You dynamic colours
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
    }

    // A fixed sideload key kept in the repo, so every CI build can install as an
    // update over the previous one. Make a private key before publishing to Play.
    signingConfigs {
        create("sideload") {
            storeFile = file("sideload.keystore")
            storePassword = "tonalwidgets"
            keyAlias = "tonal"
            keyPassword = "tonalwidgets"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("sideload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}
