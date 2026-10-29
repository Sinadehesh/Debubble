import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// The release signing key comes from the environment: CI writes the repository's keystore to
// a file and points DEBUBBLE_STORE_FILE at it. Without one the release build is left unsigned
// rather than failing, so a fresh clone still builds.
val uploadStore = System.getenv("DEBUBBLE_STORE_FILE")?.let(::file)?.takeIf { it.exists() }

// Google Play refuses an upload whose versionCode is not higher than the last, so CI numbers
// each build by its run; a local build is 1.
val build = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull()

android {
    namespace = "com.debubble.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.debubble.app"
        minSdk = 26
        targetSdk = 36
        versionCode = build ?: 1
        versionName = if (build != null) "0.1.$build" else "0.1"
    }

    signingConfigs {
        if (uploadStore != null) {
            create("upload") {
                storeFile = uploadStore
                storePassword = System.getenv("DEBUBBLE_STORE_PASSWORD")
                keyAlias = System.getenv("DEBUBBLE_KEY_ALIAS")
                // Most keys share the store's password; a separate one is used when given.
                keyPassword = System.getenv("DEBUBBLE_KEY_PASSWORD")?.takeIf { it.isNotEmpty() }
                    ?: System.getenv("DEBUBBLE_STORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("upload")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.serialization.json)
}
