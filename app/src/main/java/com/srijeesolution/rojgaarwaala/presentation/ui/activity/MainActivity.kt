package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.srijeesolution.rojgaarwaala.R
import dagger.hilt.android.AndroidEntryPoint

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
        tabAddJob.setOnClickListener { selectTab(1) }
        tabCategories.setOnClickListener { selectTab(2) }

        // Show home by default
        if (savedInstanceState == null) {
            selectTab(0)
        }
    }

    private fun selectTab(index: Int) {
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
}
