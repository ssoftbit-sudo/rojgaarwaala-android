package com.srijeesolution.rojgaarwaala.utils

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.view.WindowManager

/**
 * Marks every activity window as secure so the system refuses screenshots, screen
 * recording, and recents thumbnails. Applied from the Application rather than each
 * Activity so a new screen cannot be added without it.
 *
 * This is what Android offers. A rooted device, or a recorder that captures the
 * display below the app, can still copy the pixels.
 */
class SecureWindowLifecycleCallbacks : Application.ActivityLifecycleCallbacks {

    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        lock(activity)
    }

    /**
     * Pre-created only fires from API 29. Older devices would otherwise keep
     * screenshots on every screen except the two that set FLAG_SECURE by hand.
     */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return
        lock(activity)
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit

    private fun lock(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
    }
}
