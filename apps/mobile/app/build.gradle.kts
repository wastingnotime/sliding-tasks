plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val uploadKeystore = System.getenv("SLIDING_TASKS_UPLOAD_KEYSTORE")
val uploadStorePassword = System.getenv("SLIDING_TASKS_UPLOAD_STORE_PASSWORD")
val uploadKeyPassword = System.getenv("SLIDING_TASKS_UPLOAD_KEY_PASSWORD")
val uploadKeyAlias = System.getenv("SLIDING_TASKS_UPLOAD_KEY_ALIAS")
val hasUploadSigning = listOf(
    uploadKeystore,
    uploadStorePassword,
    uploadKeyPassword,
    uploadKeyAlias,
).all { !it.isNullOrBlank() }

android {
    namespace = "org.wastingnotime.slidingtasks"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.wastingnotime.slidingtasks"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasUploadSigning) {
            create("playUpload") {
                storeFile = file(requireNotNull(uploadKeystore))
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (hasUploadSigning) {
                signingConfig = signingConfigs.getByName("playUpload")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
