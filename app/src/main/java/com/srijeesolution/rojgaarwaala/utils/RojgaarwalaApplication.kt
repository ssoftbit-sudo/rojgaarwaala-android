package com.srijeesolution.rojgaarwaala.utils

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.srijeesolution.rojgaarwaala.BuildConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RojgaarwalaApplication : Application(), DefaultLifecycleObserver {

    override fun onCreate() {
        super<Application>.onCreate()
        instance = this

        try {
            FirebaseApp.initializeApp(this)
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("app_version", BuildConfig.VERSION_NAME)
                setCustomKey("version_code", BuildConfig.VERSION_CODE)
            }
        } catch (t: Throwable) {
            // Never block app launch if Firebase/Crashlytics fails on an OEM build.
            Log.w("RojgaarwalaApplication", "Firebase init failed; continuing", t)
        }

        registerActivityLifecycleCallbacks(SecureWindowLifecycleCallbacks())
        registerActivityLifecycleCallbacks(EdgeToEdgeLifecycleCallbacks())
    }

    companion object {
        @JvmStatic
        @get:Synchronized
        var instance: RojgaarwalaApplication? = null
            private set

        @JvmStatic
        fun getAppContext(): Context {
            return checkNotNull(instance) { "Application not initialized yet" }.applicationContext
        }

        var isActivityVisible = false
            private set

        @JvmField
        var isAppInBackground = false

        @JvmStatic
        fun activityVisible() {
            isActivityVisible = true
        }

        @JvmStatic
        fun activityNotVisible() {
            isActivityVisible = false
        }
    }
}
