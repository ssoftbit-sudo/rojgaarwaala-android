plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("dagger.hilt.android.plugin")
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
}

android {
    compileSdk = 36
    namespace = "com.srijeesolution.rojgaarwaala"

    defaultConfig {
        applicationId = "com.srijeesolution.rojgaarwaala"
        minSdk = 23
        targetSdk = 35
        versionCode = 13
        versionName = "2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("play_store_file")
            storePassword = "Rojgaarwaala@123"
            keyAlias = "Rojgaarwaala"
            keyPassword = "Rojgaarwaala@123"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }


    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        dataBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Android 12+ Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    //Network
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:okhttp:4.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.0")
    implementation ("com.squareup.retrofit2:adapter-rxjava3:2.9.0")
    
    // ExoPlayer for better video streaming performance
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-common:1.2.1")
    implementation("androidx.media3:media3-datasource:1.2.1")
    implementation("androidx.media3:media3-session:1.2.1")
    
    //implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")


    //image load and loader
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    // implementation ("com.kaopiz:kprogresshud:1.0.2")
    implementation ("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")

    // Material3 NavigationBar
    implementation("com.google.android.material:material:1.9.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Razorpay Payment Gateway
    implementation("com.razorpay:checkout:1.6.35")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
}
kapt { correctErrorTypes = true }