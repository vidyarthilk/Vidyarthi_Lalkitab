import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

fun loadAdmobProperties(): Properties {
    val props = Properties()
    val file = rootProject.file("admob.properties")
    if (!file.exists()) return props
    val text = file.readText(Charsets.UTF_8).removePrefix("\uFEFF")
    props.load(text.reader())
    return props
}

fun loadKeystoreProperties(): Properties? {
    val file = rootProject.file("keystore.properties")
    if (!file.exists()) return null
    return Properties().apply { file.inputStream().use { load(it) } }
}

fun Properties.readRequired(name: String): String {
    val value = getProperty(name)
        ?: getProperty("\uFEFF$name") // handle UTF-8 BOM at first key
        ?: entries.firstOrNull { entry ->
            entry.key.toString()
                .replace("\uFEFF", "")
                .replace("ï»¿", "")
                .trim() == name
        }?.value?.toString()
    return value?.trim()?.takeIf { it.isNotEmpty() }
        ?: throw GradleException("Missing '$name' in keystore.properties (check encoding)")
}

android {
    namespace = "com.vidyarthi.lalkitab"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vidyarthi.lalkitab"
        minSdk = 23
        targetSdk = 35
        versionCode = 29
        versionName = "5.4.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Keep Hindi resources in release AAB (shrinkResources strips runtime-only locales otherwise).
        resourceConfigurations += listOf("en", "hi")
    }

    signingConfigs {
        loadKeystoreProperties()?.let { ks ->
            create("release") {
                storeFile = rootProject.file(ks.readRequired("storeFile"))
                storePassword = ks.readRequired("storePassword")
                keyAlias = ks.readRequired("keyAlias")
                keyPassword = ks.readRequired("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Google official test ad units — safe for development only.
            resValue("string", "admob_app_id", "ca-app-pub-3940256099942544~3347511713")
            resValue("string", "admob_banner_main", "ca-app-pub-3940256099942544/6300978111")
            resValue("string", "admob_interstitial_main", "ca-app-pub-3940256099942544/1033173712")
        }
        release {
            // Hardening for Play release: obfuscate + shrink to make reverse engineering harder.
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val admob = loadAdmobProperties()
            val appId = admob.getProperty("ADMOB_APP_ID")?.trim().orEmpty()
            val bannerId = admob.getProperty("ADMOB_BANNER_ID")?.trim().orEmpty()
            val interstitialId = admob.getProperty("ADMOB_INTERSTITIAL_ID")?.trim().orEmpty()
            if (interstitialId.isNotEmpty() && interstitialId == bannerId) {
                logger.warn(
                    "WARNING: ADMOB_INTERSTITIAL_ID equals ADMOB_BANNER_ID — create a separate " +
                        "interstitial unit in AdMob and update admob.properties."
                )
            }
            // Placeholders until admob.properties exists; validated before release tasks run.
            val fallbackApp = "ca-app-pub-3940256099942544~3347511713"
            val fallbackBanner = "ca-app-pub-3940256099942544/6300978111"
            resValue("string", "admob_app_id", appId.ifEmpty { fallbackApp })
            resValue("string", "admob_banner_main", bannerId.ifEmpty { fallbackBanner })
            resValue(
                "string",
                "admob_interstitial_main",
                interstitialId.ifEmpty { bannerId.ifEmpty { fallbackBanner } }
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

configurations.configureEach {
    exclude(group = "com.google.guava", module = "listenablefuture")
    // Crashlytics without its Gradle plugin crashes at startup (missing build ID).
    exclude(group = "com.google.firebase", module = "firebase-crashlytics")
    exclude(group = "com.google.firebase", module = "firebase-crashlytics-ktx")
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

    // --- Google AdMob (free app monetization) ---
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.ump:user-messaging-platform:3.1.0")

    // --- Encrypted local credentials ---
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // --- Google Play Billing (premium unlimited saves) ---
    implementation("com.android.billingclient:billing-ktx:7.1.1")

    // --- Google Play In-App Updates ---
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // --- Firebase Analytics (google-services.json required) ---
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))

    // Core Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Material 3
    implementation("androidx.compose.material3:material3")

    // Debug tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}

val releaseAdTasks = listOf("assembleRelease", "bundleRelease", "publishRelease")
tasks.configureEach {
    if (name in releaseAdTasks) {
        doFirst {
            val admob = loadAdmobProperties()
            val appId = admob.getProperty("ADMOB_APP_ID")?.trim().orEmpty()
            val bannerId = admob.getProperty("ADMOB_BANNER_ID")?.trim().orEmpty()
            if (appId.isEmpty() || bannerId.isEmpty()) {
                throw GradleException(
                    """
                    Release build needs real AdMob IDs in admob.properties.
                    1. Copy admob.properties.example → admob.properties
                    2. Add IDs from https://admob.google.com
                    """.trimIndent()
                )
            }
            if (!file("google-services.json").exists()) {
                logger.warn(
                    "WARNING: app/google-services.json missing — Firebase Analytics will not run until you add it " +
                        "(see google-services.json.example)."
                )
            }
        }
    }
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
