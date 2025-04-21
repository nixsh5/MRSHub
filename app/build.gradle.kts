plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.mrshub"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mrshub"
        minSdk = 33
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
}

dependencies {

    implementation(platform(libs.firebase.bom))
    implementation(libs.appcompat.v161)
    implementation(libs.activity.v182)
    implementation(libs.constraintlayout.v214)
    implementation(libs.material)

    // ✅ Firebase Auth
    implementation(libs.firebase.auth.v2231)

    // ✅ Google Sign-In (play services)
    implementation(libs.play.services.auth)

    // ✅ Firebase Realtime Database
    implementation(libs.firebase.database.v2030)

    implementation(libs.firebase.storage)

    // ✅ Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.v115)
    androidTestImplementation(libs.espresso.core.v351)


    implementation(platform(libs.firebase.bom))
    implementation(libs.google.firebase.database)
    implementation(libs.google.firebase.storage)
    implementation(libs.google.firebase.auth)

    // Add this line for Firebase UI
    implementation(libs.firebase.ui.database)

    implementation(libs.glide)
    annotationProcessor(libs.compiler)

    implementation(libs.firebase.analytics)

}
