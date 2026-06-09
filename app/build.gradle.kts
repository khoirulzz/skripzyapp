import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

// Load signing configuration if available
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "id.skripzy.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "id.skripzy.app"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "2.10.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Signing configuration
    signingConfigs {
        create("release") {
            if (keystoreProperties.isNotEmpty()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            } else {
                // Fallback to environment variables or project properties
                storeFile = file(
                    System.getenv("SIGNING_STORE_FILE") 
                        ?: project.properties["signing.store.file"] 
                        ?: "skripzy-release.jks"
                )
                storePassword = System.getenv("SIGNING_STORE_PASSWORD") 
                    ?: (project.properties["signing.store.password"] as? String) ?: ""
                keyAlias = System.getenv("SIGNING_KEY_ALIAS") 
                    ?: (project.properties["signing.key.alias"] as? String) ?: ""
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD") 
                    ?: (project.properties["signing.key.password"] as? String) ?: ""
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            
            // Use signing config if available
            if (keystoreProperties.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
}

dependencies {
    // Compose Bill of Materials
    val composeBom = platform("androidx.compose:compose-bom:2025.08.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    // Compose dependencies
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    // Core
    implementation(libs.androidx.core.ktx)
    // Webkit
    implementation(libs.androidx.webkit)
    // SplashScreen
    implementation(libs.androidx.core.splashscreen)
    // Swiperefreshlayout
    implementation(libs.androidx.swiperefreshlayout)
}