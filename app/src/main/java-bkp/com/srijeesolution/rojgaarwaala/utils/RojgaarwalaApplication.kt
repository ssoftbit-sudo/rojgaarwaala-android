package com.srijeesolution.rojgaarwaala.utils

import android.app.Application
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import dagger.hilt.android.HiltAndroidApp
import com.google.firebase.FirebaseApp

@HiltAndroidApp
class RojgaarwalaApplication : Application(), DefaultLifecycleObserver {

    override fun onCreate() {
        super<Application>.onCreate()
        instance = this

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize the default uncaught exception handler
        mDefaultUEH = Thread.getDefaultUncaughtExceptionHandler()

        // Set your custom uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(mCaughtExceptionHandler)
    }

    companion object {
        @JvmStatic
        @get:Synchronized
        var instance: RojgaarwalaApplication? = null
            private set

        // Context function to access the context throughout the app
        @JvmStatic
        fun getAppContext(): Context {
            return instance!!.applicationContext
        }

        var isActivityVisible = false
            private set

        private var mDefaultUEH: Thread.UncaughtExceptionHandler? = null

        @JvmField
        var isAppInBackground = false

        private val mCaughtExceptionHandler =
            Thread.UncaughtExceptionHandler { thread, ex -> // Custom logic goes here
                // This will make Crashlytics do its job
                mDefaultUEH?.uncaughtException(thread, ex)
            }

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