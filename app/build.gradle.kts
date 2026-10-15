plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.lindabinti"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.lindabinti"
        minSdk = 24
        targetSdk = 36
        versionCode = 46
        versionName = "3.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Empty = backend disabled until configured. Emulator loopback for local Ktor:
        buildConfigField("String", "BACKEND_BASE_URL", "\"\"")
    }

    buildTypes {
        debug {
            // Physical tablet + laptop LAN (verified health: http://192.168.100.44:8080/v1/health).
            // Emulator users can temporarily switch back to http://10.0.2.2:8080/
            buildConfigField("String", "BACKEND_BASE_URL", "\"http://192.168.100.11:8080/\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            // Production API — HTTPS only (lindabinti.com). Override via gradle.properties:
            // linda.production.api.url=https://your-domain.com/
            val productionApiUrl =
                (project.findProperty("linda.production.api.url") as String?)
                    ?: "https://lindabinti.com/"
            buildConfigField("String", "BACKEND_BASE_URL", "\"$productionApiUrl\"")
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
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.security.crypto)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.appcompat)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.gson)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}