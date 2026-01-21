plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}


android {
    namespace = "com.spm.mynanasapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.spm.mynanasapp"
        minSdk = 28
        targetSdk = 36
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
    kotlinOptions {
        jvmTarget = "11"
    }
}
dependencies {
    // --- CRITICAL FIXES FOR CRASH & DUPLICATES ---
    // This provides the AbstractResolvableFuture class you were missing
    implementation("androidx.concurrent:concurrent-futures-ktx:1.2.0")

    // This provides the actual ListenableFuture class
    implementation("com.google.guava:guava:33.3.0-android")

    // This is the MAGIC FIX: It tells Gradle to stop including separate
    // listenablefuture.jar files because Guava already has it.
    implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")

    // Fixes the specific ProfileInstaller crash you saw in logs
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")


    // --- CORE & BACKGROUND ---
    implementation(libs.androidx.work.runtime.ktx)
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // --- UI & NAVIGATION ---
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.glide)

    // --- NETWORKING ---
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
    implementation("com.google.code.gson:gson:2.11.0")

    // --- SERVICES & TESTING ---
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

//    --- MAPS ---
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("org.osmdroid:osmdroid-mapsforge:6.1.18")

}