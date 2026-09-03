import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("dagger.hilt.android.plugin")
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

// Read from local.properties, which is gitignored, so the Maps key never lands in the repo.
// CI and fresh clones can pass -PMAPS_API_KEY=... or set MAPS_API_KEY in the environment.
val mapsApiKey: String = run {
    val local = rootProject.file("local.properties")
    val fromLocal = if (local.exists()) {
        Properties().apply { local.inputStream().use { load(it) } }.getProperty("MAPS_API_KEY")
    } else {
        null
    }
    fromLocal
        ?: (project.findProperty("MAPS_API_KEY") as String?)
        ?: System.getenv("MAPS_API_KEY")
        ?: ""
}

android {
    compileSdk = 36
    namespace = "com.srijeesolution.rojgaarwaala"

    defaultConfig {
        applicationId = "com.srijeesolution.rojgaarwaala"
        minSdk = 23
        targetSdk = 36
        versionCode = 24
        versionName = "2.0.7"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndkVersion = "28.2.13676358"
        manifestPlaceholders["mapsApiKey"] = mapsApiKey
        buildConfigField("boolean", "HAS_MAPS_KEY", (mapsApiKey.isNotBlank()).toString())
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
            isShrinkResources = true
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

    testOptions {
        unitTests {
            // Repository and parser code touches android.util.Log and org.json, neither of
            // which is implemented in the JVM stub jar. Returning defaults keeps those calls
            // harmless so the surrounding logic can be tested without Robolectric.
            isReturnDefaultValues = true
        }
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

    // Google Play 16 KB page-size requirement (targetSdk 35+).
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    // Exercises the Retrofit interface and Gson models against real backend payloads.
    testImplementation("com.squareup.okhttp3:mockwebserver:4.9.0")
    // Real org.json implementation, since the stub in the Android JVM jar throws.
    testImplementation("org.json:json:20231013")
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
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")
    implementation("androidx.media3:media3-datasource:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    
    //implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")


    //image load and loader
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    // implementation ("com.kaopiz:kprogresshud:1.0.2")
    implementation ("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")

    // Material3 NavigationBar
    implementation("com.google.android.material:material:1.9.0")

    // Firebase (only used SDKs — drop unused Firestore/Storage to cut memory on low-RAM devices)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Chrome Custom Tabs host the Vegaah payment page. Keeping checkout in the
    // browser rather than a WebView means card data never enters this app.
    implementation("androidx.browser:browser:1.8.0")

    // Fused location provider for employee attendance punch in/out
    implementation("com.google.android.gms:play-services-location:21.3.0")
    // Map + geofence circle on the attendance screen
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
}
kapt { correctErrorTypes = true }