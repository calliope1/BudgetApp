import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val properties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    properties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.budgetapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.budgetapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "SERVER_URL",
            "${properties.getProperty("SERVER_URL")}"
        )
        buildConfigField(
            "String",
            "SHARED_SECRET",
            "${properties.getProperty("SHARED_SECRET")}"
        )
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
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.core:core-ktx:1.9.0") // Or latest
    implementation("androidx.appcompat:appcompat:1.6.1") // Or latest
    implementation("com.google.android.material:material:1.10.0") // Or latest
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // Or latest

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2") // Or latest
    // LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2") // Or latest
    // Activity KTX for by viewModels()
    implementation("androidx.activity:activity-ktx:1.8.0") // Or latest

    // OkHttp (already there in your original code)
    implementation("com.squareup.okhttp3:okhttp:4.11.0") // Or latest
    // Coroutines (already there in your original code)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") // Or latest
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") // Or latest
}