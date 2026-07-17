package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.MainToolbarViewModel
import com.srijeesolution.rojgaarwaala.utils.DeviceKeyUtils
import com.srijeesolution.rojgaarwaala.utils.NotificationUtils
import com.srijeesolution.rojgaarwaala.utils.HomeLocationDefaults
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var rootView: LinearLayout
    private lateinit var mainToolbar: LinearLayout
    private lateinit var mainSearchRow: LinearLayout
    private lateinit var mainSearchEdit: EditText
    private lateinit var mainSearchClear: ImageButton
    private lateinit var toolbarTitle: TextView
    private lateinit var toolbarLocationLabel: TextView
    private lateinit var toolbarNotification: ImageButton
    private lateinit var toolbarOverflow: ImageButton
    private lateinit var bottomNav: LinearLayout

    private lateinit var tabHome: LinearLayout
    private lateinit var tabAddJob: LinearLayout
    private lateinit var tabCategories: LinearLayout
    private lateinit var tabImages: LinearLayout
    private lateinit var tabStories: LinearLayout

    private lateinit var iconHome: ImageView
    private lateinit var iconAddJob: ImageView
    private lateinit var iconCategories: ImageView
    private lateinit var iconImages: ImageView
    private lateinit var iconStories: ImageView

    private lateinit var textHome: TextView
    private lateinit var textAddJob: TextView
    private lateinit var textCategories: TextView
    private lateinit var textImages: TextView
    private lateinit var textStories: TextView

    private lateinit var pillHome: FrameLayout
    private lateinit var pillAddJob: FrameLayout
    private lateinit var pillCategories: FrameLayout
    private lateinit var pillImages: FrameLayout
    private lateinit var pillStories: FrameLayout

    private val mainToolbarViewModel: MainToolbarViewModel by viewModels()
    private val homePageViewModel: HomePageViewModel by viewModels()

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    private var currentTabIndex = 0
    private var notificationProcessed = false

    private val pickLocationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val location =
                result.data?.getStringExtra(LocationPickerActivity.EXTRA_SELECTED_LOCATION).orEmpty()
            if (location.isNotBlank()) {
                val normalized = HomeLocationDefaults.normalize(location)
                mainToolbarViewModel.setSelectedLocation(normalized)
                sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.HOME_SELECTED_LOCATION, normalized))
                Toast.makeText(this, location, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NotificationUtils.requestNotificationPermission(this)
        NotificationUtils.getFirebaseToken { token ->
            Log.d("MainActivity", "Firebase Token: $token")
            if (token.isNotEmpty()) {
                sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.FCM_TOKEN, token))
            }
        }

        bindViews()
        setupSystemNavigationBar()
        restoreToolbarState()
        setupToolbarChrome()
        setupBottomNav()
        preloadStories()

        if (savedInstanceState == null) {
            selectTab(0)
        }

        handleNotificationNavigation(intent)
        checkForBackgroundNotification()
    }

    private fun bindViews() {
        rootView = findViewById(R.id.root)
        mainToolbar = findViewById(R.id.mainToolbar)
        mainSearchRow = findViewById(R.id.mainSearchRow)
        mainSearchEdit = findViewById(R.id.mainSearchEdit)
        mainSearchClear = findViewById(R.id.mainSearchClear)
        toolbarTitle = findViewById(R.id.toolbarTitle)
        toolbarLocationLabel = findViewById(R.id.toolbarLocationLabel)
        toolbarNotification = findViewById(R.id.toolbarNotification)
        toolbarOverflow = findViewById(R.id.toolbarOverflow)
        bottomNav = findViewById(R.id.customBottomNav)

        tabHome = findViewById(R.id.tabHome)
        tabAddJob = findViewById(R.id.tabAddJob)
        tabCategories = findViewById(R.id.tabCategories)
        tabImages = findViewById(R.id.tabImages)
        tabStories = findViewById(R.id.tabStories)

        iconHome = findViewById(R.id.iconHome)
        iconAddJob = findViewById(R.id.iconAddJob)
        iconCategories = findViewById(R.id.iconCategories)
        iconImages = findViewById(R.id.iconImages)
        iconStories = findViewById(R.id.iconStories)

        textHome = findViewById(R.id.textHome)
        textAddJob = findViewById(R.id.textAddJob)
        textCategories = findViewById(R.id.textCategories)
        textImages = findViewById(R.id.textImages)
        textStories = findViewById(R.id.textStories)

        pillHome = findViewById(R.id.pillHome)
        pillAddJob = findViewById(R.id.pillAddJob)
        pillCategories = findViewById(R.id.pillCategories)
        pillImages = findViewById(R.id.pillImages)
        pillStories = findViewById(R.id.pillStories)
    }

    private fun restoreToolbarState() {
        val savedLocation = sharedPrefs.getPrefs(
            SharedPrefsConstant.HOME_SELECTED_LOCATION,
            "",
        ).orEmpty()
        val location = HomeLocationDefaults.normalize(savedLocation)
        mainToolbarViewModel.setSelectedLocation(location)
        if (savedLocation.isBlank()) {
            sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.HOME_SELECTED_LOCATION, location))
        }

        mainToolbarViewModel.searchQuery.observe(this) { query ->
            val safeQuery = query.orEmpty()
            if (mainSearchEdit.text?.toString().orEmpty() != safeQuery) {
                mainSearchEdit.setText(safeQuery)
                mainSearchEdit.setSelection(safeQuery.length)
            }
            mainSearchClear.visibility = if (safeQuery.isBlank()) View.GONE else View.VISIBLE
        }

        mainToolbarViewModel.selectedLocation.observe(this) { location ->
            updateSelectedLocationUi(location.orEmpty())
        }
    }

    private fun setupToolbarChrome() {
        mainSearchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty()
                mainToolbarViewModel.setSearchQuery(query)
                mainSearchClear.visibility = if (query.isBlank()) View.GONE else View.VISIBLE
            }
        })

        mainSearchClear.setOnClickListener {
            mainSearchEdit.setText("")
            mainToolbarViewModel.setSearchQuery("")
            hideKeyboard(mainSearchEdit)
        }

        toolbarNotification.setOnClickListener {
            sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.NOTIFICATION_BADGE_PENDING, false))
            toolbarNotification.visibility = View.GONE
            Toast.makeText(this, R.string.notifications, Toast.LENGTH_SHORT).show()
        }

        toolbarOverflow.setOnClickListener { showOverflowMenu() }

        refreshNotificationBadgeUi()
    }

    private fun showOverflowMenu() {
        val popup = PopupMenu(this, toolbarOverflow)
        popup.menu.add(0, MENU_PROFILE, 0, getString(R.string.profile))
        popup.menu.add(0, MENU_HELP_DESK, 1, getString(R.string.help_desk))

        popup.menu.add(0, MENU_LOCATION, 2, getString(R.string.change_location))

        val isDefaultLocation = HomeLocationDefaults.skipsDistrictFilter(
            mainToolbarViewModel.selectedLocation.value,
        )
        if (!isDefaultLocation) {
            popup.menu.add(0, MENU_CLEAR_LOCATION, 3, getString(R.string.reset_all_chhattisgarh))
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_PROFILE -> {
                    openProfileSection()
                    true
                }
                MENU_HELP_DESK -> {
                    startActivity(Intent(this, HelpDeskActivity::class.java))
                    true
                }
                MENU_LOCATION -> {
                    pickLocationLauncher.launch(Intent(this, LocationPickerActivity::class.java))
                    true
                }
                MENU_CLEAR_LOCATION -> {
                    clearSelectedLocation()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun openProfileSection() {
        if (sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
            startActivity(Intent(this, ProfileActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            Toast.makeText(this, "Please login to access profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearSelectedLocation() {
        val defaultLocation = HomeLocationDefaults.ALL_CHHATTISGARH
        mainToolbarViewModel.setSelectedLocation(defaultLocation)
        sharedPrefs.setPrefsData(Pair(SharedPrefsConstant.HOME_SELECTED_LOCATION, defaultLocation))
        Toast.makeText(this, R.string.location_reset_all_cg, Toast.LENGTH_SHORT).show()
    }

    private fun updateSelectedLocationUi(location: String) {
        val safeLocation = location.trim()
        toolbarLocationLabel.text = safeLocation
        toolbarLocationLabel.visibility = if (safeLocation.isBlank()) View.GONE else View.VISIBLE
    }

    private fun setupBottomNav() {
        tabHome.setOnClickListener { selectTab(0) }
        tabAddJob.setOnClickListener {
            if (sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
                selectTab(1)
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
                Toast.makeText(this, "Please login to add jobs", Toast.LENGTH_SHORT).show()
            }
        }
        tabCategories.setOnClickListener { selectTab(2) }
        tabImages.setOnClickListener { selectTab(3) }
        tabStories.setOnClickListener { selectTab(4) }
    }

    private fun updateChromeForTab(index: Int) {
        val isHome = index == 0
        mainSearchRow.visibility = if (isHome) View.VISIBLE else View.GONE
        toolbarTitle.alpha = if (isHome) 1f else 0.92f
        toolbarTitle.text = getString(
            when (index) {
                0 -> R.string.home
                1 -> R.string.add_job
                2 -> R.string.categories
                3 -> R.string.free_job
                4 -> R.string.stories
                else -> R.string.app_name
            }
        )
    }

    private fun refreshNotificationBadgeUi() {
        val pending = sharedPrefs.getPrefs(SharedPrefsConstant.NOTIFICATION_BADGE_PENDING, false)
        toolbarNotification.visibility = if (pending) View.VISIBLE else View.GONE
    }

    private fun selectTab(index: Int) {
        currentTabIndex = index
        val selectedTextColor = ContextCompat.getColor(this, R.color.tab_selected)
        val unselectedColor = ContextCompat.getColor(this, R.color.tab_unselected)
        val selectedIconColor = ContextCompat.getColor(this, R.color.app_background)

        iconHome.setColorFilter(unselectedColor)
        iconAddJob.setColorFilter(unselectedColor)
        iconCategories.setColorFilter(unselectedColor)
        iconImages.setColorFilter(unselectedColor)
        iconStories.setColorFilter(unselectedColor)

        textHome.setTextColor(unselectedColor)
        textAddJob.setTextColor(unselectedColor)
        textCategories.setTextColor(unselectedColor)
        textImages.setTextColor(unselectedColor)
        textStories.setTextColor(unselectedColor)

        pillHome.background = null
        pillAddJob.background = null
        pillCategories.background = null
        pillImages.background = null
        pillStories.background = null

        when (index) {
            0 -> {
                iconHome.setColorFilter(selectedIconColor)
                textHome.setTextColor(selectedTextColor)
                pillHome.setBackgroundResource(R.drawable.bg_bottom_nav_selected)
                showFragment(HomeFragment())
            }
            1 -> {
                iconAddJob.setColorFilter(selectedIconColor)
                textAddJob.setTextColor(selectedTextColor)
                pillAddJob.setBackgroundResource(R.drawable.bg_bottom_nav_selected)
                showFragment(AddJobFragment())
            }
            2 -> {
                iconCategories.setColorFilter(selectedIconColor)
                textCategories.setTextColor(selectedTextColor)
                pillCategories.setBackgroundResource(R.drawable.bg_bottom_nav_selected)
                showFragment(CategoriesFragment())
            }
            3 -> {
                iconImages.setColorFilter(selectedIconColor)
                textImages.setTextColor(selectedTextColor)
                pillImages.setBackgroundResource(R.drawable.bg_bottom_nav_selected)
                showFragment(ImagesFragment())
            }
            4 -> {
                iconStories.setColorFilter(selectedIconColor)
                textStories.setTextColor(selectedTextColor)
                pillStories.setBackgroundResource(R.drawable.bg_bottom_nav_selected)
                showFragment(StoriesFragment())
            }
        }
        updateChromeForTab(index)
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun preloadStories() {
        val deviceKey = DeviceKeyUtils.getOrCreateDeviceKey(sharedPrefs)
        homePageViewModel.preloadStories(deviceKey)
    }

    fun selectTabFromFragment(index: Int) {
        selectTab(index)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        notificationProcessed = false
        handleNotificationNavigation(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshNotificationBadgeUi()
        intent?.let { incoming ->
            if (incoming.hasExtra("type") || incoming.hasExtra("notification_type") || incoming.data != null) {
                handleNotificationNavigation(incoming)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        checkForBackgroundNotification()
    }

    override fun onBackPressed() {
        if (currentTabIndex == 0) {
            super.onBackPressed()
        } else {
            selectTab(0)
        }
    }

    private fun handleNotificationNavigation(intent: Intent?) {
        if (notificationProcessed) return

        intent?.let {
            var notificationType = it.getStringExtra("notification_type")
            var notificationId = it.getStringExtra("notification_id")

            if (notificationType.isNullOrEmpty()) notificationType = it.getStringExtra("type")
            if (notificationId.isNullOrEmpty()) notificationId = it.getStringExtra("id")

            if (notificationType.isNullOrEmpty() && notificationId.isNullOrEmpty()) {
                it.data?.let { uri ->
                    notificationType = uri.getQueryParameter("type")
                    notificationId = uri.getQueryParameter("id")
                }
            }

            when (notificationType) {
                "home" -> selectTab(0)
                "vlp" -> {
                    val videoIntent = Intent(this, CategoryVideosActivity::class.java)
                    videoIntent.putExtra("category_id", notificationId)
                    videoIntent.putExtra("category_title", "Videos")
                    startActivity(videoIntent)
                }
                "vdp" -> {
                    val videoIntent = Intent(this, VideoPlayerActivity::class.java)
                    videoIntent.putExtra("video_id", notificationId)
                    startActivity(videoIntent)
                }
                "clp" -> {
                    val categoryIntent = Intent(this, CategoryVideosActivity::class.java)
                    categoryIntent.putExtra("category_id", notificationId)
                    categoryIntent.putExtra("category_title", "Category")
                    startActivity(categoryIntent)
                }
                else -> selectTab(0)
            }

            it.removeExtra("notification_type")
            it.removeExtra("notification_id")
            it.removeExtra("type")
            it.removeExtra("id")
            it.removeExtra("from_notification")
            notificationProcessed = true
        }
    }

    private fun checkForBackgroundNotification() {
        intent?.let { incoming ->
            val hasNotificationData = incoming.hasExtra("type") ||
                incoming.hasExtra("notification_type") ||
                incoming.hasExtra("id") ||
                incoming.hasExtra("notification_id") ||
                incoming.data != null

            if (hasNotificationData) {
                handleNotificationNavigation(incoming)
            }
        }
    }

    private fun setupSystemNavigationBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = ContextCompat.getColor(this, R.color.app_background)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.app_background)

        val toolbarStart = mainToolbar.paddingLeft
        val toolbarTop = mainToolbar.paddingTop
        val toolbarEnd = mainToolbar.paddingRight
        val toolbarBottom = mainToolbar.paddingBottom
        val bottomStart = bottomNav.paddingLeft
        val bottomTop = bottomNav.paddingTop
        val bottomEnd = bottomNav.paddingRight
        val bottomBottom = bottomNav.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            mainToolbar.setPadding(
                toolbarStart + insets.left,
                toolbarTop + insets.top,
                toolbarEnd + insets.right,
                toolbarBottom
            )
            bottomNav.setPadding(
                bottomStart + insets.left,
                bottomTop,
                bottomEnd + insets.right,
                bottomBottom + insets.bottom
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(rootView)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager ?: return
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    companion object {
        private const val MENU_PROFILE = 1
        private const val MENU_HELP_DESK = 4
        private const val MENU_LOCATION = 2
        private const val MENU_CLEAR_LOCATION = 3
    }
}
