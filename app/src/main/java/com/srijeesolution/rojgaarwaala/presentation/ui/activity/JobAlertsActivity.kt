package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.ImageData
import com.srijeesolution.rojgaarwaala.data.remote.model.ScheduledImage
import com.srijeesolution.rojgaarwaala.databinding.ActivityImagesListBinding
import com.srijeesolution.rojgaarwaala.network.handler.ApiResult
import com.srijeesolution.rojgaarwaala.presentation.adaptor.ImagesGridAdapter
import com.srijeesolution.rojgaarwaala.presentation.viewmodel.HomePageViewModel
import com.srijeesolution.rojgaarwaala.utils.JobAlertFeed
import com.srijeesolution.rojgaarwaala.utils.ProfileLocationStore
import com.srijeesolution.rojgaarwaala.utils.SpaceItemDecoration
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefs
import com.srijeesolution.rojgaarwaala.utils.sp.SharedPrefsConstant
import dagger.hilt.android.AndroidEntryPoint
import java.util.ArrayList
import javax.inject.Inject

@AndroidEntryPoint
class JobAlertsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImagesListBinding
    private lateinit var viewModel: HomePageViewModel
    private var imagesAdapter: ImagesGridAdapter? = null
    private var allImagesList: List<ImageData> = emptyList()

    @Inject
    lateinit var sharedPrefs: SharedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!sharedPrefs.getPrefs(SharedPrefsConstant.USER_LOGGED_IN_STATUS, false)) {
            Toast.makeText(this, getString(R.string.job_alerts_login), Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityImagesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[HomePageViewModel::class.java]
        setupViews()
        setupObservers()
        viewModel.getProfileData()
        viewModel.getScheduledImages()
    }

    private fun setupViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.job_alerts)
        }
        binding.imagesRecyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.imagesRecyclerView.addItemDecoration(SpaceItemDecoration(8, 8))
        imagesAdapter = ImagesGridAdapter { image, imageIndex -> onImageClick(image, imageIndex) }
        binding.imagesRecyclerView.adapter = imagesAdapter
        showEmpty(getString(R.string.job_alerts_loading))
    }

    private fun setupObservers() {
        viewModel.profileUpdateLiveData.observe(this) { result ->
            val profile = (result as? ApiResult.Success)?.data?.dataObj?.userDetails ?: return@observe
            ProfileLocationStore.save(sharedPrefs, profile)
            renderFromLoadedImages()
        }
        viewModel.imageListLiveData.observe(this) { result ->
            when (result) {
                is ApiResult.Success -> renderFromLoadedImages()
                is ApiResult.Error -> showEmpty(getString(R.string.job_alerts_failed))
                is ApiResult.Loading -> showEmpty(getString(R.string.job_alerts_loading))
            }
        }
    }

    private fun renderFromLoadedImages() {
        val categories = (viewModel.imageListLiveData.value as? ApiResult.Success)
            ?.data?.data?.categoryImages.orEmpty()
        if (categories.isEmpty() && viewModel.imageListLiveData.value !is ApiResult.Success) {
            return
        }
        val preferred = ProfileLocationStore.preferredCategory(sharedPrefs)
        val images = JobAlertFeed.imagesForPreferredCategory(categories, preferred)
        allImagesList = images
        if (images.isNotEmpty()) {
            binding.emptyStateText.visibility = View.GONE
            binding.imagesRecyclerView.visibility = View.VISIBLE
            imagesAdapter?.submitList(images)
        } else {
            showEmpty(getString(R.string.job_alerts_empty))
        }
    }

    private fun showEmpty(message: String) {
        binding.imagesRecyclerView.visibility = View.GONE
        binding.emptyStateText.visibility = View.VISIBLE
        binding.emptyStateText.text = message
    }

    private fun onImageClick(image: ImageData, imageIndex: Int) {
        if (image.imageUrl.isNullOrEmpty() || imageIndex !in allImagesList.indices) {
            Toast.makeText(this, "No image available", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, ImageViewerActivity::class.java)
        val scheduledImages = allImagesList.map { imageData ->
            ScheduledImage(
                id = imageData.id,
                title = imageData.title,
                description = imageData.description,
                imagePath = imageData.imageUrl,
                location = imageData.location,
                publishDate = imageData.publishDate,
                status = null,
                createdAt = imageData.createdAt,
                updatedAt = null,
                phoneNumber = imageData.phoneNumber,
            )
        }
        intent.putParcelableArrayListExtra("scheduled_images", ArrayList(scheduledImages))
        intent.putExtra("current_index", imageIndex)
        intent.putExtra(
            ImageViewerActivity.EXTRA_IMAGE_CATEGORY,
            ProfileLocationStore.preferredCategory(sharedPrefs).ifBlank { getString(R.string.job_alerts) },
        )
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
