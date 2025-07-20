package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var tabHome: LinearLayout
    private lateinit var tabAddJob: LinearLayout
    private lateinit var tabCategories: LinearLayout
    private lateinit var iconHome: ImageView
    private lateinit var iconAddJob: ImageView
    private lateinit var iconCategories: ImageView
    private lateinit var textHome: TextView
    private lateinit var textAddJob: TextView
    private lateinit var textCategories: TextView

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    // Track current tab
    private var currentTabIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tabHome = findViewById(R.id.tabHome)
        tabAddJob = findViewById(R.id.tabAddJob)
        tabCategories = findViewById(R.id.tabCategories)
        iconHome = findViewById(R.id.iconHome)
        iconAddJob = findViewById(R.id.iconAddJob)
        iconCategories = findViewById(R.id.iconCategories)
        textHome = findViewById(R.id.textHome)
        textAddJob = findViewById(R.id.textAddJob)
        textCategories = findViewById(R.id.textCategories)

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

        // Show home by default
        if (savedInstanceState == null) {
            selectTab(0)
        }
    }

    override fun onBackPressed() {
        when (currentTabIndex) {
            0 -> {
                // On Home tab, close the app
                super.onBackPressed()
            }
            1, 2 -> {
                // On Add Job or Categories tab, go back to Home
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
        textHome.setTextColor(unselectedColor)
        textAddJob.setTextColor(unselectedColor)
        textCategories.setTextColor(unselectedColor)

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
}
