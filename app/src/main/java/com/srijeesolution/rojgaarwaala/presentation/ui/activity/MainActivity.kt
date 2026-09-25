package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.annotation.SuppressLint
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
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.MainToolbarViewModel
import com.srijeesolution.rojgaarwaala.utils.DeviceKeyUtils
import com.srijeesolution.rojgaarwaala.utils.InAppNotification
import com.srijeesolution.rojgaarwaala.utils.InAppNotificationInbox
import com.srijeesolution.rojgaarwaala.utils.InAppNotificationStore
import com.srijeesolution.rojgaarwaala.utils.JobAlertNavigation
import com.srijeesolution.rojgaarwaala.utils.MainTabs
import com.srijeesolution.rojgaarwaala.utils.NotificationUtils
import com.srijeesolution.rojgaarwaala.utils.HomeLocationDefaults
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), com.srijeesolution.rojgaarwaala.utils.ManualEdgeToEdge {

    private lateinit var rootView: LinearLayout
    private lateinit var mainToolbar: LinearLayout
    private lateinit var mainSearchRow: LinearLayout
    private lateinit var mainSearchEdit: EditText
    private lateinit var mainSearchClear: ImageButton
    private lateinit var toolbarTitle: TextView
    private lateinit var toolbarLocationLabel: TextView
    private lateinit var toolbarNotification: ImageButton
    private lateinit var toolbarNotificationContainer: FrameLayout
    private lateinit var toolbarNotificationBadge: TextView
    private lateinit var toolbarOverflow: ImageButton
    private lateinit var bottomNav: LinearLayout

    private lateinit var tabProfile: LinearLayout
    private lateinit var tabHome: LinearLayout
    private lateinit var tabCategories: LinearLayout
    private lateinit var tabImages: LinearLayout
    private lateinit var tabStories: LinearLayout

    private lateinit var iconProfile: ImageView
    private lateinit var iconHome: ImageView
    private lateinit var iconCategories: ImageView
    private lateinit var iconImages: ImageView
    private lateinit var iconStories: ImageView

    private lateinit var textProfile: TextView
    private lateinit var textHome: TextView
    private lateinit var textCategories: TextView
    private lateinit var textImages: TextView
    private lateinit var textStories: TextView

    private lateinit var pillProfile: FrameLayout
    private lateinit var pillHome: FrameLayout
    private lateinit var pillCategories: FrameLayout
    private lateinit var pillImages: FrameLayout
    private lateinit var pillStories: FrameLayout

    private lateinit var storiesNavBadge: View

    private val mainToolbarViewModel: MainToolbarViewModel by viewModels()
    private val homePageViewModel: HomePageViewModel by viewModels()

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    private var currentTabIndex = MainTabs.HOME
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
        setupStoriesNavBadgeObserver()
        preloadStories()

        if (savedInstanceState == null) {
            selectTab(MainTabs.HOME)
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
        toolbarNotificationContainer = findViewById(R.id.toolbarNotificationContainer)
        toolbarNotificationBadge = findViewById(R.id.toolbarNotificationBadge)
        toolbarOverflow = findViewById(R.id.toolbarOverflow)
        bottomNav = findViewById(R.id.customBottomNav)

        tabProfile = findViewById(R.id.tabProfile)
        tabHome = findViewById(R.id.tabHome)
        tabCategories = findViewById(R.id.tabCategories)
        tabImages = findViewById(R.id.tabImages)
        tabStories = findViewById(R.id.tabStories)

        iconProfile = findViewById(R.id.iconProfile)
        iconHome = findViewById(R.id.iconHome)
        iconCategories = findViewById(R.id.iconCategories)
        iconImages = findViewById(R.id.iconImages)
        iconStories = findViewById(R.id.iconStories)

        textProfile = findViewById(R.id.textProfile)
        textHome = findViewById(R.id.textHome)
        textCategories = findViewById(R.id.textCategories)
        textImages = findViewById(R.id.textImages)
        textStories = findViewById(R.id.textStories)

        pillProfile = findViewById(R.id.pillProfile)
        pillHome = findViewById(R.id.pillHome)
        pillCategories = findViewById(R.id.pillCategories)
        pillImages = findViewById(R.id.pillImages)
        pillStories = findViewById(R.id.pillStories)
        storiesNavBadge = findViewById(R.id.storiesNavBadge)
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
            openNotificationsScreen()
        }

        toolbarOverflow.setOnClickListener { showOverflowMenu() }

        refreshNotificationUi()
    }

    private fun openNotificationsScreen() {
        InAppNotificationStore.markAllRead(sharedPrefs)
        startActivity(Intent(this, NotificationsActivity::class.java))
        refreshNotificationUi()
    }

    private fun openJobStatusScreen() {
        if (sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
            startActivity(Intent(this, AppliedJobsActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            Toast.makeText(this, "Please login to view job status", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPaidApplicationsScreen() {
        if (sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
            startActivity(
                Intent(this, AppliedJobsActivity::class.java).apply {
                    putExtra(AppliedJobsActivity.EXTRA_PAID_ONLY, true)
                },
            )
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            Toast.makeText(this, "Please login to view paid applications", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAddJob() {
        if (sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
            selectTab(MainTabs.ADD_JOB)
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            Toast.makeText(this, "Please login to add jobs", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateNotificationBadge(count: Int) {
        if (count > 0) {
            toolbarNotificationBadge.visibility = View.VISIBLE
            toolbarNotificationBadge.text = if (count > 9) "9+" else count.toString()
        } else {
            toolbarNotificationBadge.visibility = View.GONE
        }
    }

    private fun refreshNotificationUi() {
        toolbarNotificationContainer.visibility = View.VISIBLE
        updateNotificationBadge(InAppNotificationStore.unreadCount(sharedPrefs))
    }

    private fun showOverflowMenu() {
        val popup = PopupMenu(this, toolbarOverflow)
        popup.menu.add(0, MENU_ADD_JOB, 0, getString(R.string.add_job))
        popup.menu.add(0, MENU_JOB_STATUS, 1, getString(R.string.job_status))
        popup.menu.add(0, MENU_PAID_APPLICATIONS, 2, getString(R.string.paid_applications))
        popup.menu.add(0, MENU_HELP_DESK, 3, getString(R.string.help_desk))
        popup.menu.add(0, MENU_LOCATION, 4, getString(R.string.change_location))

        val isDefaultLocation = HomeLocationDefaults.skipsDistrictFilter(
            mainToolbarViewModel.selectedLocation.value,
        )
        if (!isDefaultLocation) {
            popup.menu.add(0, MENU_CLEAR_LOCATION, 5, getString(R.string.reset_all_chhattisgarh))
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_ADD_JOB -> {
                    openAddJob()
                    true
                }
                MENU_JOB_STATUS -> {
                    openJobStatusScreen()
                    true
                }
                MENU_HELP_DESK -> {
                    startActivity(Intent(this, HelpDeskActivity::class.java))
                    true
                }
                MENU_PAID_APPLICATIONS -> {
                    openPaidApplicationsScreen()
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
            selectTab(MainTabs.PROFILE)
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

    private fun setupStoriesNavBadgeObserver() {
        homePageViewModel.hasUnseenStoriesLiveData.observe(this) { hasUnseen ->
            storiesNavBadge.visibility = if (hasUnseen) View.VISIBLE else View.GONE
        }
    }

    private fun setupBottomNav() {
        tabProfile.setOnClickListener { openProfileSection() }
        tabCategories.setOnClickListener { selectTab(MainTabs.CATEGORIES) }
        tabHome.setOnClickListener { selectTab(MainTabs.HOME) }
        tabImages.setOnClickListener { selectTab(MainTabs.FREE_JOB) }
        tabStories.setOnClickListener { selectTab(MainTabs.STORIES) }
    }

    private fun updateChromeForTab(index: Int) {
        val isHome = index == MainTabs.HOME
        mainToolbar.visibility = View.VISIBLE
        mainSearchRow.visibility = if (isHome) View.VISIBLE else View.GONE
        toolbarTitle.alpha = if (isHome) 1f else 0.92f
        toolbarTitle.text = getString(
            when (index) {
                MainTabs.HOME -> R.string.home
                MainTabs.PROFILE -> R.string.profile
                MainTabs.ADD_JOB -> R.string.add_job
                MainTabs.CATEGORIES -> R.string.categories
                MainTabs.FREE_JOB -> R.string.free_job
                MainTabs.STORIES -> R.string.stories
                else -> R.string.app_name
            }
        )
    }

    private fun refreshNotificationBadgeUi() {
        updateNotificationBadge(InAppNotificationStore.unreadCount(sharedPrefs))
    }

    private fun selectTab(index: Int) {
        currentTabIndex = index
        val selectedTextColor = ContextCompat.getColor(this, R.color.tab_selected)
        val unselectedColor = ContextCompat.getColor(this, R.color.tab_unselected)
        val selectedIconColor = ContextCompat.getColor(this, R.color.app_background)

        iconProfile.setColorFilter(unselectedColor)
        iconHome.setColorFilter(unselectedColor)
        iconCategories.setColorFilter(unselectedColor)
        iconImages.setColorFilter(unselectedColor)
        iconStories.setColorFilter(unselectedColor)

        textProfile.setTextColor(unselectedColor)
        textHome.setTextColor(unselectedColor)
        textCategories.setTextColor(unselectedColor)
        textImages.setTextColor(unselectedColor)
        textStories.setTextColor(unselectedColor)

        pillProfile.background = null
        pillHome.background = null
        pillCategories.background = null
        pillImages.background = null
        pillStories.background = null

        when (index) {
            MainTabs.HOME -> {
                iconHome.setColorFilter(selectedIconColor)
                textHome.setTextColor(selectedTextColor)
                pillHome.setBackgroundResource(R.drawable.bg_bottom_nav_selected)
                showFragment(HomeFragment())
            }
            MainTabs.PROFILE -> {
                iconProfile.setColorFilter(selectedIconColor)
                textProfile.setTextColor(selectedTextColor)
                pillProfile.setBackgroundResource(R.drawable.bg_bottom_nav_selected)
                showFragment(ProfileFragment.newInstance())
            }
            MainTabs.ADD_JOB -> {
                showFragment(AddJobFragment())
            }
            MainTabs.CATEGORIES -> {
                iconCategories.setColorFilter(selectedIconColor)
                textCategories.setTextColor(selectedTextColor)
                pillCategories.setBackgroundResource(R.drawable.bg_bottom_nav_selected)
                showFragment(CategoriesFragment())
            }
            MainTabs.FREE_JOB -> {
                iconImages.setColorFilter(selectedIconColor)
                textImages.setTextColor(selectedTextColor)
                pillImages.setBackgroundResource(R.drawable.bg_bottom_nav_selected)
                showFragment(ImagesFragment())
            }
            MainTabs.STORIES -> {
                iconStories.setColorFilter(selectedIconColor)
                textStories.setTextColor(selectedTextColor)
                pillStories.setBackgroundResource(R.drawable.bg_bottom_nav_selected)
                homePageViewModel.dismissStoriesNavBadge()
                showFragment(StoriesFragment())
            }
        }
        updateChromeForTab(index)
        refreshNotificationUi()
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
        refreshNotificationUi()
        val deviceKey = DeviceKeyUtils.getOrCreateDeviceKey(sharedPrefs)
        homePageViewModel.getActiveStories(deviceKey, forceRefresh = true)
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
        if (currentTabIndex == MainTabs.HOME) {
            super.onBackPressed()
        } else {
            selectTab(MainTabs.HOME)
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun handleNotificationNavigation(intent: Intent?) {
        if (notificationProcessed) return

        intent?.let {
            var notificationType = it.getStringExtra("notification_type")
            var notificationId = it.getStringExtra("notification_id")

            if (notificationType.isNullOrEmpty()) notificationType = it.getStringExtra("type")
            if (notificationId.isNullOrEmpty()) notificationId = it.getStringExtra("id")
            if (notificationId.isNullOrEmpty()) notificationId = it.getStringExtra("scheduled_image_id")

            if (notificationType.isNullOrEmpty() && notificationId.isNullOrEmpty()) {
                it.data?.let { uri ->
                    notificationType = uri.getQueryParameter("type")
                    notificationId = uri.getQueryParameter("id")
                        ?: uri.getQueryParameter("application_id")
                }
            }

            var applicationId = it.getStringExtra("application_id")
            if (applicationId.isNullOrEmpty()) {
                applicationId = notificationId
            }
            if (applicationId.isNullOrEmpty()) {
                it.data?.let { uri ->
                    applicationId = uri.getQueryParameter("application_id")
                }
            }

            val resolvedType = notificationType?.takeIf { it.isNotEmpty() } ?: return
            if (!it.getBooleanExtra("from_inbox", false)) {
                rememberIncomingNotification(it, resolvedType, notificationId ?: applicationId)
            }

            when (resolvedType) {
                "home" -> selectTab(MainTabs.HOME)
                "vlp" -> {
                    val videoIntent = Intent(this, CategoryVideosActivity::class.java)
                    videoIntent.putExtra("category_id", notificationId)
                    videoIntent.putExtra("category_title", "Videos")
                    startActivity(videoIntent)
                }
                "vdp", "video_published" -> {
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
                "job_application_status" -> {
                    if (!applicationId.isNullOrBlank()) {
                        val statusIntent = Intent(this, ApplicationStatusActivity::class.java)
                        statusIntent.putExtra("application_id", applicationId)
                        startActivity(statusIntent)
                    } else {
                        openJobStatusScreen()
                    }
                }
                "ot_request" -> startActivity(Intent(this, OtReviewActivity::class.java))
                "missed_punch_request" -> startActivity(Intent(this, BulkAttendanceActivity::class.java))
                "missed_punch_submitted" -> startActivity(
                    Intent(this, AttendanceRequestActivity::class.java)
                        .putExtra(AttendanceRequestActivity.EXTRA_MODE, AttendanceRequestActivity.MODE_MISSED),
                )
                "ot_approved",
                "ot_rejected" -> startActivity(
                    Intent(this, AttendanceRequestActivity::class.java)
                        .putExtra(AttendanceRequestActivity.EXTRA_MODE, AttendanceRequestActivity.MODE_OT),
                )
                "missed_punch_approved",
                "missed_punch_rejected" -> startActivity(
                    Intent(this, AttendanceRequestActivity::class.java)
                        .putExtra(AttendanceRequestActivity.EXTRA_MODE, AttendanceRequestActivity.MODE_MISSED),
                )
                "paper_cut_job", "scheduled_image", "free_job" -> {
                    selectTab(JobAlertNavigation.TAB_FREE_JOB)
                }
                "attendance_punch_in_reminder",
                "attendance_punch_out_reminder",
                "attendance_missed_punch",
                "attendance_geofence_arrival" -> {
                    val punch = if (resolvedType == "attendance_punch_out_reminder") {
                        AttendanceDashboardActivity.PUNCH_OUT
                    } else {
                        AttendanceDashboardActivity.PUNCH_IN
                    }
                    startActivity(
                        Intent(this, AttendanceDashboardActivity::class.java)
                            .putExtra(AttendanceDashboardActivity.EXTRA_PUNCH, punch),
                    )
                }
                else -> selectTab(MainTabs.HOME)
            }

            it.removeExtra("notification_type")
            it.removeExtra("notification_id")
            it.removeExtra("type")
            it.removeExtra("id")
            it.removeExtra("from_notification")
            it.removeExtra("from_inbox")
            notificationProcessed = true
        }
    }

    private fun rememberIncomingNotification(intent: Intent, type: String, targetId: String?) {
        val resolvedId = InAppNotificationInbox.resolveTargetId(
            id = targetId,
            scheduledImageId = intent.getStringExtra("scheduled_image_id"),
            videoId = intent.getStringExtra("video_id"),
            applicationId = intent.getStringExtra("application_id"),
            categoryId = intent.getStringExtra("category_id"),
        )
        val title = intent.getStringExtra("title")
            ?: intent.getStringExtra("notification_title")
            ?: ""
        val body = intent.getStringExtra("body")
            ?: intent.getStringExtra("notification_body")
            ?: ""
        InAppNotificationStore.add(
            sharedPrefs,
            InAppNotification(
                type = type,
                title = title.ifBlank { getString(R.string.app_name) },
                body = body,
                targetId = resolvedId,
                receivedAt = System.currentTimeMillis(),
                read = false,
            ),
        )
        refreshNotificationUi()
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
        private const val MENU_ADD_JOB = 1
        private const val MENU_JOB_STATUS = 6
        private const val MENU_HELP_DESK = 4
        private const val MENU_PAID_APPLICATIONS = 5
        private const val MENU_LOCATION = 2
        private const val MENU_CLEAR_LOCATION = 3
    }
}
