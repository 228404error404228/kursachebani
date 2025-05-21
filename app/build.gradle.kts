plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.kurs"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.kurs"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Для Google Maps API
        manifestPlaceholders["MAPS_API_KEY"] = "YOUR_API_KEY"
        buildConfigField("String", "MAPS_API_KEY", "\"YOUR_API_KEY\"")

        buildFeatures {
            buildConfig = true
        }
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
        viewBinding = true
    }

}



dependencies {
    // AndroidX
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)


    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database)

    // Google Maps
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Firebase BoM (управление версиями)
    implementation (platform("com.google.firebase:firebase-bom:32.8.0"))

// Firebase Auth (без явного указания версии)
    implementation ("com.google.firebase:firebase-auth")

// Google Sign-In (указать одну версию)
    implementation ("com.google.android.gms:play-services-auth:21.0.0")

// Material Design
    implementation ("com.google.android.material:material:1.10.0")

// Navigation (если используешь Navigation Component)
    implementation ("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation ("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("com.google.firebase:firebase-database")

    implementation ("com.google.android.libraries.places:places:3.3.0")
    implementation ("com.google.android.gms:play-services-maps:18.2.0")
    implementation ("com.google.android.gms:play-services-location:21.0.1")
    implementation ("com.google.android.libraries.places:places:3.4.0")




}