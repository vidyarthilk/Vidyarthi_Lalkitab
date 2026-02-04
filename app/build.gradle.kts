plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.vidyarthi_lalkitab"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.vidyarthi_lalkitab"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
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
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {

    // --- Core Android ---
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // --- Material ---
    implementation("com.google.android.material:material:1.12.0")

    // --- Fragment ---
    implementation("androidx.fragment:fragment-ktx:1.7.1")

    // --- ViewPager2 ---
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // --- RecyclerView ---
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // --- Lifecycle ---
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // --- Room Database ---
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // --- Swiss Ephemeris ---
    implementation(files("libs/swisseph-2.01.00-01.jar"))

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))

    // Core Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Material 3
    implementation("androidx.compose.material3:material3")

    // Debug tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
}
