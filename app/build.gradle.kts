plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.sleistikow.roamingborders"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sleistikow.roamingborders"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.gson)
    implementation(libs.appcompat.v170)
    implementation(libs.material)
    implementation(libs.constraintlayout.v220)
    implementation(libs.preference)
    implementation(libs.core.ktx) // still handy even in Java projects
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}