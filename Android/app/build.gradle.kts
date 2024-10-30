import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

// local.properties 사용
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val apiKey: String = localProperties.getProperty("API_KEY") ?: ""
val naverApiKey: String = localProperties.getProperty("NAVER_API_KEY") ?: ""
val naverClientId: String = localProperties.getProperty("NAVER_CLIENT_ID") ?: ""

android {
    namespace = "com.example.safedrive"
    compileSdk = 34


    defaultConfig {
        applicationId = "com.example.safedrive"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        aaptOptions {
            noCompress("tflite")
        }

        buildConfigField("String", "API_KEY", "\"$apiKey\"")

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
        dataBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.view)
    implementation(project(":opencv"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Dependency for Tensorflow
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.10.0")

    // TMap SDK
    implementation(files("libs/tmap-sdk-1.4.aar"))
    implementation(files("libs/vsm-tmap-sdk-v2-android-1.6.60.aar"))

    // Chart - Dashboard
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Google Map SDK
    implementation("com.google.android.gms:play-services-maps:19.0.0")

}