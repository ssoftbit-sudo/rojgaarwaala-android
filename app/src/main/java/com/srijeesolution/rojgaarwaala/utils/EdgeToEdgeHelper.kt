package com.srijeesolution.rojgaarwaala.utils

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.srijeesolution.rojgaarwaala.R

/**
 * Marker for activities that apply their own system-bar padding / immersive UI.
 * All other activities get default content root padding from [EdgeToEdgeLifecycleCallbacks].
 */
interface ManualEdgeToEdge

object EdgeToEdgeHelper {
    private const val TAG = "EdgeToEdgeHelper"

    fun enable(activity: ComponentActivity) {
        try {
            // Explicit ARGB colors avoid OEM theme ColorStateList lookups that crash
            // some MIUI devices inside PhoneWindow.installDecor / ResourcesImpl.
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            )
        } catch (t: Throwable) {
            Log.w(TAG, "enableEdgeToEdge failed; continuing without edge-to-edge", t)
        }
    }

    fun hideSystemBars(activity: ComponentActivity, root: View) {
        try {
            WindowInsetsControllerCompat(activity.window, root).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } catch (t: Throwable) {
            Log.w(TAG, "hideSystemBars failed", t)
        }
    }

    fun showSystemBars(activity: ComponentActivity, root: View) {
        try {
            WindowInsetsControllerCompat(activity.window, root)
                .show(WindowInsetsCompat.Type.systemBars())
        } catch (t: Throwable) {
            Log.w(TAG, "showSystemBars failed", t)
        }
    }

    fun padWithSystemBars(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }
        ViewCompat.requestApplyInsets(root)
    }
}

class EdgeToEdgeLifecycleCallbacks : android.app.Application.ActivityLifecycleCallbacks {
    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is ComponentActivity) {
            EdgeToEdgeHelper.enable(activity)
        }
    }

    override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is ManualEdgeToEdge) return
        if (activity !is ComponentActivity) return
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val root = content.getChildAt(0) ?: return
        if (root.getTag(R.id.tag_edge_to_edge_padded) == true) return
        root.setTag(R.id.tag_edge_to_edge_padded, true)
        try {
            EdgeToEdgeHelper.padWithSystemBars(root)
        } catch (t: Throwable) {
            Log.w("EdgeToEdgeHelper", "padWithSystemBars failed", t)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
