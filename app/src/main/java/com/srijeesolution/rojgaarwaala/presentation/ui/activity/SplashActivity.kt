package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.srijeesolution.rojgaarwaala.BuildConfig
import com.srijeesolution.rojgaarwaala.network.constant.NetworkBaseUrls
import com.srijeesolution.rojgaarwaala.network.retorfit.RetrofitApiService
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity(),
    com.srijeesolution.rojgaarwaala.utils.ManualEdgeToEdge {
    @Inject
    lateinit var sharedPrefs: SharedPrefs

    private var keepSplash = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { keepSplash }

        lifecycleScope.launch {
            // Short timeout so low-RAM warm starts are not blocked on network.
            val blocked = withTimeoutOrNull(1_200L) { checkForceUpdate() } ?: false
            if (blocked) {
                keepSplash = false
                return@launch
            }
            // If cached force-update says block, show dialog even when network timed out.
            if (isBlockedByCache()) {
                showForceUpdateDialog(
                    sharedPrefs.getPrefs(SharedPrefsConstant.CACHED_FORCE_UPDATE_TITLE, "")
                        .orEmpty().ifBlank { "Update required" },
                    sharedPrefs.getPrefs(SharedPrefsConstant.CACHED_FORCE_UPDATE_MESSAGE, "")
                        .orEmpty().ifBlank {
                            "A new version of Rojgaarwaala is available. Please update to continue."
                        },
                    sharedPrefs.getPrefs(SharedPrefsConstant.CACHED_FORCE_UPDATE_STORE_URL, "")
                        .orEmpty().ifBlank {
                            "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
                        },
                )
                keepSplash = false
                return@launch
            }
            navigateNext()
        }
    }

    /**
     * Only trust the cached flag while this build is still older than the min
     * version it was cached for; otherwise an updated build stays locked out.
     */
    private fun isBlockedByCache(): Boolean {
        if (!sharedPrefs.getPrefs(SharedPrefsConstant.CACHED_FORCE_UPDATE, false)) return false
        val cachedMin = sharedPrefs.getPrefs(SharedPrefsConstant.CACHED_MIN_VERSION_CODE, -1)
        return cachedMin > 0 && BuildConfig.VERSION_CODE < cachedMin
    }

    private suspend fun checkForceUpdate(): Boolean {
        return try {
            val response = withContext(Dispatchers.IO) {
                RetrofitApiService.create(NetworkBaseUrls.BASE_URL)
                    .getAppConfig(BuildConfig.VERSION_CODE)
            }
            if (!response.isSuccessful) return false
            val androidCfg = response.body()?.data?.android ?: return false
            val minVersionCode = androidCfg.minVersionCode ?: -1
            // Re-derive locally: never lock a build that already meets the minimum.
            val force = androidCfg.forceUpdate == true &&
                minVersionCode > 0 &&
                BuildConfig.VERSION_CODE < minVersionCode

            sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.CACHED_FORCE_UPDATE, force))
            sharedPrefs.setPrefsData(
                Pair(SharedPrefsConstant.CACHED_MIN_VERSION_CODE, minVersionCode)
            )
            androidCfg.title?.let {
                sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.CACHED_FORCE_UPDATE_TITLE, it))
            }
            androidCfg.message?.let {
                sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.CACHED_FORCE_UPDATE_MESSAGE, it))
            }
            androidCfg.storeUrl?.let {
                sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.CACHED_FORCE_UPDATE_STORE_URL, it))
            }

            if (!force) return false

            val title = androidCfg.title?.takeIf { it.isNotBlank() } ?: "Update required"
            val message = androidCfg.message?.takeIf { it.isNotBlank() }
                ?: "A new version of Rojgaarwaala is available. Please update to continue."
            val storeUrl = androidCfg.storeUrl?.takeIf { it.isNotBlank() }
                ?: "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"

            showForceUpdateDialog(title, message, storeUrl)
            true
        } catch (t: Throwable) {
            Log.w("SplashActivity", "app-config check failed; continuing", t)
            false
        }
    }

    private fun showForceUpdateDialog(title: String, message: String, storeUrl: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Update") { _, _ ->
                openStore(storeUrl)
            }
            .setNegativeButton("Exit") { _, _ ->
                finishAffinity()
            }
            .show()
    }

    private fun openStore(storeUrl: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl)))
        } catch (_: Exception) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=${BuildConfig.APPLICATION_ID}"),
                ),
            )
        }
    }

    private fun navigateNext() {
        keepSplash = false
        val intent = when {
            sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false) -> {
                Intent(this, MainActivity::class.java)
            }
            sharedPrefs.getPrefs(SharedPrefsConstant.USER_SKIP_STATUS, false) -> {
                Intent(this, MainActivity::class.java)
            }
            else -> {
                Intent(this, LoginActivity::class.java)
            }
        }
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
