plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
}

val ciVersionCode = (findProperty("CI_VERSION_CODE") as String?)?.toIntOrNull() ?: 2
val ciVersionName = (findProperty("CI_VERSION_NAME") as String?) ?: "1.1"
val courierMobileApiKey = (findProperty("COURIER_MOBILE_API_KEY") as String?)
    ?: System.getenv("COURIER_MOBILE_API_KEY")
    ?: "courier-mobile-ydz-2026"

android {
    namespace = "com.cechriza.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cechrza.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 10011
        versionName = "1.1.11"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "COURIER_MOBILE_API_KEY", "\"$courierMobileApiKey\"")
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
    implementation("androidx.compose.runtime:runtime-saveable")
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.google.play.services.location)

    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.3.0")
    implementation("com.google.mlkit:face-detection:16.1.7")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    implementation("com.google.accompanist:accompanist-pager:0.32.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.32.0")
    implementation("io.coil-kt:coil-compose:2.5.0")


    implementation("androidx.room:room-runtime:2.7.2")
    kapt("androidx.room:room-compiler:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")

    implementation("androidx.datastore:datastore-preferences:1.1.7")

    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")


    implementation ("androidx.compose.runtime:runtime-livedata:1.6.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("io.github.vanpra.compose-material-dialogs:datetime:0.9.0")


}
