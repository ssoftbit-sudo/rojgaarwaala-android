package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.BannerList
import com.srijeesolution.rojgaarwaala.data.remote.model.Category
import com.srijeesolution.rojgaarwaala.databinding.ActivityMainBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.adaptor.CategoryAdapter
import com.srijeesolution.rojgaarwaala.presentation.adaptor.ImagePagerAdapter
import com.srijeesolution.rojgaarwaala.presentation.adaptor.YouTubePagerAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.SpaceItemDecoration
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var homePageViewModel: HomePageViewModel
    private var backPressedTime: Long = 0
    private lateinit var backToast: Toast

    @Inject
    lateinit var sharedPrefs: SharedPrefs
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var navHeaderTitle: TextView
    private val youtubeList: ArrayList<BannerList> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the toolbar
        setSupportActionBar(binding.toolBar)

        // Initialize ViewModel
        homePageViewModel = ViewModelProvider(this)[HomePageViewModel::class.java]

        // Initialize views using binding
        binding.progressBar.visibility = View.VISIBLE
        binding.mainRecyclerView.visibility = View.GONE
        setDrawerMenu()
        // Fetch initial data
        homePageViewModel.getHomePageData("")
        observeHomePageData()
        observeLogoutData()
        setupSearchBar()
        setupClickListeners()
    }

    private fun setDrawerMenu() {
        setSupportActionBar(binding.toolBar)

        drawerLayout = findViewById(R.id.my_drawer_layout)
        navView = findViewById(R.id.navigationView)

        toggle = ActionBarDrawerToggle(
            this, drawerLayout, binding.toolBar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        toggle.drawerArrowDrawable.color = ContextCompat.getColor(this, android.R.color.black)

        val headerView = navView.getHeaderView(0)
        navHeaderTitle = headerView.findViewById(R.id.nav_header_title)

        // 🔑 Conditionally set drawer menu based on skip status
        if (sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
            navView.menu.clear()
            navView.inflateMenu(R.menu.drawer_menu) // 👈 default (logged-in) menu
        } else {
            navView.menu.clear()
            navView.inflateMenu(R.menu.drawer_guest_menu) // 👈 guest or skip-login menu
        }

        navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_profile -> {
                    drawerLayout.closeDrawers()
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_add_job -> {
                    drawerLayout.closeDrawers()
                    startActivity(Intent(this, AddJobActivity::class.java))
                    true
                }
                R.id.nav_logout -> {
                    drawerLayout.closeDrawers()
                    sharedPrefs.removeSharedPrefs(SharedPrefsConstant.USER_AUTH_TOKEN)
                    sharedPrefs.removeSharedPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS)
                    Toast.makeText(this, "Successfully logged out!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                    true
                }
                R.id.nav_login -> {
                    drawerLayout.closeDrawers()
                    startActivity(Intent(this, LoginActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }



    private fun observeHomePageData() {
        homePageViewModel.homepageLiveData.observe(this) { apiResponse ->
            when (apiResponse) {
                is ApiResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.mainRecyclerView.visibility = View.GONE
                }

                is ApiResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    /*if (apiResponse.data?.dataObj?.bannerList.isNullOrEmpty()) {
                        binding.rvProductList.visibility = View.GONE
                        binding.tvNoItems.visibility = View.VISIBLE
                        Toast.makeText(this, "No items found", Toast.LENGTH_SHORT).show()
                    } else {
                        binding.rvProductList.visibility = View.VISIBLE
                        setAdaptor(apiResponse.data?.dataObj?.bannerList)
                    }*/
                    if (apiResponse.data?.dataObj?.bannerList.isNullOrEmpty().not()) {
                        setUpViewPager(apiResponse.data?.dataObj?.bannerList)
                    } else {
                        binding.parentAdd.visibility=View.GONE
                    }
                    if (apiResponse.data?.dataObj?.userDetails != null) {
                        navHeaderTitle.text = "Hello, "+apiResponse.data.dataObj.userDetails.name+"!"
                    } else {

                    }
                    if (apiResponse.data?.dataObj?.categoryList.isNullOrEmpty().not()) {
                        setUpCategoriesList(apiResponse.data?.dataObj?.categoryList)
                    } else {
                        binding.mainRecyclerView.visibility=View.GONE
                    }
                }

                is ApiResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.mainRecyclerView.visibility = View.VISIBLE
                    // Handle error state, maybe show a toast or log the error
                    Toast.makeText(this, "Error fetching data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun setUpViewPager(bannerItems: ArrayList<BannerList>?) {
        binding.parentAdd.visibility=View.VISIBLE
        val adapter = ImagePagerAdapter(bannerItems)
        binding.viewPager.adapter = adapter
        // Auto-scroll functionality
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val currentItem = binding.viewPager.currentItem
                val itemCount = adapter.itemCount
                binding.viewPager.currentItem = (currentItem + 1) % itemCount
                handler.postDelayed(this, 5000) // Adjust delay as needed
            }
        }
        handler.postDelayed(runnable, 5000)
    }


    private fun setupSearchBar() {
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            private val handler = Handler(Looper.getMainLooper())
            private val delay = 1000L // 1 second delay
            private val runnable = Runnable {
                filterProducts(binding.searchBar.text.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                handler.removeCallbacks(runnable)
                if (s != null && s.toString().length > 2) {
                    handler.postDelayed(runnable, delay)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun filterProducts(query: String) {
        homePageViewModel.getHomePageData(query)
    }

    private fun setupClickListeners() {
        binding.ivNotification.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.ivNotification -> {
                /*homePageViewModel.onLogoutData()
                if (sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
                    homePageViewModel.onLogoutData()
                }*/
                Toast.makeText(this,"Upcoming feature",Toast.LENGTH_SHORT).show()
            }
            // Add more cases as needed
        }
    }

    private lateinit var categoryAdapter: CategoryAdapter

    private fun setUpCategoriesList(categoryList: List<Category>?) {
        binding.mainRecyclerView.visibility=View.VISIBLE
        binding.mainRecyclerView.layoutManager = LinearLayoutManager(this)
        categoryAdapter = CategoryAdapter(categoryList ?: emptyList())
        binding.mainRecyclerView.adapter = categoryAdapter
    }


    private fun observeLogoutData() {
        homePageViewModel.loginRegisterLiveData.observe(this) { apiResponse ->
            when (apiResponse) {
                is ApiResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is ApiResult.Success -> {
                    sharedPrefs.removeSharedPrefs(SharedPrefsConstant.USER_AUTH_TOKEN)
                    sharedPrefs.removeSharedPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS)
                    Toast.makeText(this, "Successfully logged out!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                }
                is ApiResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Log.d("MANISH", apiResponse.data?.message.toString())
                }
            }
        }
    }

    override fun onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            backToast.cancel()
            super.onBackPressed()
            return
        } else {
            backToast = Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT)
            backToast.show()
        }
        backPressedTime = System.currentTimeMillis()
    }
}
