package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.srijeesolution.rojgaarwaala.databinding.ActivityProductDetailsBinding

class ProductDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUrl = intent.getStringExtra("imageUrl")
        val title = intent.getStringExtra("title")
        val subtitle = intent.getStringExtra("subtitle")
        val size = intent.getStringExtra("size")
        val price = intent.getStringExtra("price")

        Glide.with(this).load(imageUrl).into(binding.imageView)
        binding.titleTextView.text = title
        binding.subtitleTextView.text = subtitle
        binding.sizeTextView.text = "Size: $size"
        binding.subtitleTextViewPrice.text = "Price: $price"
    }
}

