package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import com.srijeesolution.rojgaarwaala.utils.NotificationUtils
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.CategoryVideosActivity
import com.srijeesolution.rojgaarwaala.presentation.ui.activity.VideoPlayerActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var tabHome: LinearLayout
    private lateinit var tabAddJob: LinearLayout
    private lateinit var tabCategories: LinearLayout
    private lateinit var tabImages: LinearLayout
    private lateinit var iconHome: ImageView
    private lateinit var iconAddJob: ImageView
    private lateinit var iconCategories: ImageView
    private lateinit var iconImages: ImageView
    private lateinit var textHome: TextView
    private lateinit var textAddJob: TextView
    private lateinit var textCategories: TextView
    private lateinit var textImages: TextView

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    // Track current tab
    private var currentTabIndex = 0
    // Track if notification navigation has been processed
    private var notificationProcessed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request notification permission and get Firebase token
        NotificationUtils.requestNotificationPermission(this)
        NotificationUtils.getFirebaseToken { token ->
            Log.d("MainActivity", "Firebase Token: $token")
            // Save FCM token to SharedPreferences
            if (token.isNotEmpty()) {
                sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.FCM_TOKEN, token))
                Log.d("MainActivity", "FCM Token saved to SharedPreferences: $token")
            }
        }

        tabHome = findViewById(R.id.tabHome)
        tabAddJob = findViewById(R.id.tabAddJob)
        tabCategories = findViewById(R.id.tabCategories)
        tabImages = findViewById(R.id.tabImages)
        iconHome = findViewById(R.id.iconHome)
        iconAddJob = findViewById(R.id.iconAddJob)
        iconCategories = findViewById(R.id.iconCategories)
        iconImages = findViewById(R.id.iconImages)
        textHome = findViewById(R.id.textHome)
        textAddJob = findViewById(R.id.textAddJob)
        textCategories = findViewById(R.id.textCategories)
        textImages = findViewById(R.id.textImages)

        tabHome.setOnClickListener { selectTab(0) }
        tabAddJob.setOnClickListener { 
            if (sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
                selectTab(1)
            } else {
                // User is not logged in, navigate to login screen
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                Toast.makeText(this, "Please login to add jobs", Toast.LENGTH_SHORT).show()
            }
        }
        tabCategories.setOnClickListener { selectTab(2) }
        tabImages.setOnClickListener { selectTab(3) }

        // Show home by default
        if (savedInstanceState == null) {
            selectTab(0)
        }
        
        // Handle notification navigation (both from onCreate and onNewIntent)
        handleNotificationNavigation(intent)
        
        // Also check if we came from a background notification
        checkForBackgroundNotification()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "=== onNewIntent called ===")
        Log.d("MainActivity", "Intent action: ${intent?.action}")
        Log.d("MainActivity", "Intent data: ${intent?.data}")
        Log.d("MainActivity", "Intent extras: ${intent?.extras?.keySet()}")
        
        // Reset notification processed flag for new intents
        notificationProcessed = false
        
        // Handle notification navigation when app is already running
        handleNotificationNavigation(intent)
    }

    override fun onResume() {
        super.onResume()
        // Check if we came from a notification (for background notifications)
        intent?.let { intent ->
            if (intent.hasExtra("type") || intent.hasExtra("notification_type") || intent.data != null) {
                Log.d("MainActivity", "Resumed from notification")
                handleNotificationNavigation(intent)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Also check for notification data when app starts
        checkForBackgroundNotification()
    }

    override fun onBackPressed() {
        when (currentTabIndex) {
            0 -> {
                // On Home tab, close the app
                super.onBackPressed()
            }
            1, 2, 3 -> {
                // On Add Job, Categories, or Images tab, go back to Home
                selectTab(0)
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    private fun selectTab(index: Int) {
        currentTabIndex = index
        val selectedColor = ContextCompat.getColor(this, R.color.tab_selected)
        val unselectedColor = ContextCompat.getColor(this, R.color.tab_unselected)

        // Reset all
        iconHome.setTint(unselectedColor)
        iconAddJob.setTint(unselectedColor)
        iconCategories.setTint(unselectedColor)
        iconImages.setTint(unselectedColor)
        textHome.setTextColor(unselectedColor)
        textAddJob.setTextColor(unselectedColor)
        textCategories.setTextColor(unselectedColor)
        textImages.setTextColor(unselectedColor)

        // Set selected
        when (index) {
            0 -> {
                iconHome.setTint(selectedColor)
                textHome.setTextColor(selectedColor)
                showFragment(HomeFragment())
            }
            1 -> {
                iconAddJob.setTint(selectedColor)
                textAddJob.setTextColor(selectedColor)
                showFragment(AddJobFragment())
            }
            2 -> {
                iconCategories.setTint(selectedColor)
                textCategories.setTextColor(selectedColor)
                showFragment(CategoriesFragment())
            }
            3 -> {
                iconImages.setTint(selectedColor)
                textImages.setTextColor(selectedColor)
                showFragment(ImagesFragment())
            }
        }
    }

    private fun ImageView.setTint(color: Int) {
        this.setColorFilter(color)
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun selectTabFromFragment(index: Int) {
        selectTab(index)
    }

    private fun handleNotificationNavigation(intent: Intent?) {
        if (notificationProcessed) {
            Log.d("MainActivity", "Notification navigation already processed, skipping.")
            return
        }

        intent?.let {
            var notificationType = it.getStringExtra("notification_type")
            var notificationId = it.getStringExtra("notification_id")
            
            // If not found in our custom extras, check for Firebase's default extras
            if (notificationType.isNullOrEmpty()) {
                notificationType = it.getStringExtra("type")
            }
            if (notificationId.isNullOrEmpty()) {
                notificationId = it.getStringExtra("id")
            }
            
            // Also check for data in the intent data URI (deep link)
            if (notificationType.isNullOrEmpty() && notificationId.isNullOrEmpty()) {
                it.data?.let { uri ->
                    Log.d("MainActivity", "Checking deep link URI: $uri")
                    notificationType = uri.getQueryParameter("type")
                    notificationId = uri.getQueryParameter("id")
                    Log.d("MainActivity", "Deep link params - Type: $notificationType, ID: $notificationId")
                }
            }
            
            Log.d("MainActivity", "Final Notification Type: $notificationType, ID: $notificationId")
            Log.d("MainActivity", "All intent extras: ${it.extras?.keySet()}")
            
            // Log all extras for debugging
            it.extras?.keySet()?.forEach { key ->
                Log.d("MainActivity", "Extra $key: ${it.extras?.get(key)}")
            }
            
            when (notificationType) {
                "home" -> {
                    // Navigate to homepage (default tab)
                    selectTab(0)
                }
                "vlp" -> {
                    // Navigate to video listing page with id
                    val videoIntent = Intent(this, CategoryVideosActivity::class.java)
                    videoIntent.putExtra("category_id", notificationId)
                    videoIntent.putExtra("category_title", "Videos")
                    Log.d("MainActivity", "Starting CategoryVideosActivity with category_id: $notificationId")
                    startActivity(videoIntent)
                }
                "vdp" -> {
                    // Navigate to video details page with id
                    val videoIntent = Intent(this, VideoPlayerActivity::class.java)
                    videoIntent.putExtra("video_id", notificationId)
                    Log.d("MainActivity", "Starting VideoPlayerActivity with video_id: $notificationId")
                    startActivity(videoIntent)
                }
                "clp" -> {
                    // Navigate to category listing page with id
                    val categoryIntent = Intent(this, CategoryVideosActivity::class.java)
                    categoryIntent.putExtra("category_id", notificationId)
                    categoryIntent.putExtra("category_title", "Category")
                    Log.d("MainActivity", "Starting CategoryVideosActivity with category_id: $notificationId")
                    startActivity(categoryIntent)
                }
                else -> {
                    // Default case - navigate to homepage
                    selectTab(0)
                }
            }
            
            // Clear notification flags after processing to prevent loops
            it.removeExtra("notification_type")
            it.removeExtra("notification_id")
            it.removeExtra("type")
            it.removeExtra("id")
            it.removeExtra("from_notification")
            notificationProcessed = true
        }
    }

    private fun checkForBackgroundNotification() {
        // Check if we have any notification data in the intent
        intent?.let { intent ->
            
            // Check for notification data in various possible locations
            val hasNotificationData = intent.hasExtra("type") || 
                                    intent.hasExtra("notification_type") ||
                                    intent.hasExtra("id") ||
                                    intent.hasExtra("notification_id") ||
                                    intent.data != null
            
            if (hasNotificationData) {
                Log.d("MainActivity", "Found notification data in intent")
                handleNotificationNavigation(intent)
            } else {
                Log.d("MainActivity", "No notification data found in intent")
            }
        }
    }
}
