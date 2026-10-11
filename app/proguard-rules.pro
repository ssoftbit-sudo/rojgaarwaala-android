# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve line numbers for readable Crashlytics stack traces in release builds.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep class com.pierfrancescosoffritti.** { *; }

# Firebase Crashlytics
-keepattributes *Annotation*
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Firebase Analytics
-keep class com.google.firebase.analytics.** { *; }
-keep class com.google.android.gms.measurement.** { *; }

# Firebase Messaging
-keep class com.google.firebase.messaging.** { *; }
-keep class com.google.firebase.iid.FirebaseInstanceIdReceiver { *; }

# Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.PlatformTypes

# Retrofit & Gson
-keep class com.google.gson.** { *; }
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Hilt
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class androidx.hilt.** { *; }
-keep class com.google.dagger.** { *; }
-dontwarn dagger.hilt.internal.**

# Glide
-keep class com.bumptech.glide.** { *; }
-keep interface com.bumptech.glide.** { *; }

# Preserve class names used via reflection
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
# "-keep class * { @Keep *; }" used to sit here. It kept EVERY class in the app and
# its libraries unrenamed (only 2% of classes were obfuscated), which is what Play's
# "DEX code optimization below threshold" warning was about. androidx.annotation ships
# its own rule for @Keep, so nothing needs to be said here.

# Retrofit 2.9 has no rules for R8 full mode. Without these, the generic types of
# suspend API methods are stripped and every call fails at runtime.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking class io.reactivex.rxjava3.core.Observable
-keep,allowobfuscation,allowshrinking class io.reactivex.rxjava3.core.Single
-keep,allowobfuscation,allowshrinking class io.reactivex.rxjava3.core.Flowable
-keep,allowobfuscation,allowshrinking class io.reactivex.rxjava3.core.Maybe
-keep,allowobfuscation,allowshrinking class io.reactivex.rxjava3.core.Completable
-keep,allowobfuscation,allowshrinking class kotlinx.coroutines.flow.Flow

# Firebase
-keep class com.google.firebase.** { *; }

# Retrofit
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.google.gson.** { *; }

# Hilt
-keep class dagger.** { *; }

# Glide
-keep class com.bumptech.glide.** { *; }

# Keep data models for JSON serialization/deserialization
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.** { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.* { *; }

# Keep all fields and methods in data models
-keepclassmembers class com.srijeesolution.rojgaarwaala.data.remote.model.** {
    <fields>;
    <methods>;
}

# Keep Gson annotations
-keepclassmembers class com.srijeesolution.rojgaarwaala.data.remote.model.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep all data classes
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.HomePagBaseApiModel { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.HomePageData { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.BannerList { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.TopVideo { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.Category { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.CategoryVideo { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.UserData { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.VideoDetailsResponse { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.VideoDetailsData { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.JobListResponse { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.JobListData { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.JobItem { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.CategoryVideosResponse { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.CategoryVideosData { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.ImageListResponse { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.ImageListData { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.ImageSubItem { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.ImageData { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.StoriesResponse { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.StoriesData { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.TimeGroup { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.Story { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.ImagesApiResponse { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.ImageCategory { *; }
-keep class com.srijeesolution.rojgaarwaala.data.remote.model.ScheduledImage { *; }

# Keep all data class constructors and fields
-keepclassmembers class com.srijeesolution.rojgaarwaala.data.remote.model.** {
    public <init>(...);
    public <fields>;
}

# Keep network error models
-keep class com.srijeesolution.rojgaarwaala.network.handler.** { *; }
-keepclassmembers class com.srijeesolution.rojgaarwaala.network.handler.** {
    <fields>;
    <methods>;
}

# androidx.browser ships its own consumer rules, and payment DTOs are already
# covered by the Gson keeps above, so the gateway needs no extra entries.

# Debug and verbose logs never ship: they have printed tokens and full API
# responses before. Warnings and errors stay for Crashlytics.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
