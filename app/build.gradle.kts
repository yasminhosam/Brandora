plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// brandora.businessId lives in local.properties (gitignored); read via providers so the
// configuration cache invalidates when it changes.
val defaultBusinessId = "00000000-0000-0000-0000-000000000000"
val currentBusinessId: String = run {
    val text = providers.fileContents(rootProject.layout.projectDirectory.file("local.properties")).asText.orNull
    if (text == null) {
        defaultBusinessId
    } else {
        text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("brandora.businessId=") }
            ?.substringAfter('=')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: defaultBusinessId
    }
}

android {
    namespace = "com.yasminhosam.brandora"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.yasminhosam.brandora"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "CURRENT_BUSINESS_ID", "\"$currentBusinessId\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig=true
    }
}

dependencies {
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)

    // Lifecycle + ViewModel (BaseViewModel, collectAsStateWithLifecycle)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt (KSP, not kapt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // Images and local storage
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.datastore.preferences)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}