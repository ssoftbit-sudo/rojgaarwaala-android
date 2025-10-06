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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keep class com.pierfrancescosoffritti.** { *; }

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
-keep class * {
    @androidx.annotation.Keep *;
}

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
