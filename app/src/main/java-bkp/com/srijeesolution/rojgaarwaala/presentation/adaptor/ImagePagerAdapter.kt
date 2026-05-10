package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.graphics.Bitmap
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.BannerList


class ImagePagerAdapter(private val imageUrls: ArrayList<BannerList>?) : RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.image_item, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        // Load image using Glide or any other image loading library
        if (!TextUtils.isEmpty(imageUrls?.get(position)?.imageUrl)) {
            Glide.with(holder.imageView.context)
                .asBitmap()
                .load(imageUrls?.get(position)?.imageUrl)
                .placeholder(R.drawable.no_image_placeholder)
                .into(object : BitmapImageViewTarget(holder.imageView) {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        val aspectRatio = resource.width.toFloat() / resource.height.toFloat()
                        val width = holder.imageView.width
                        val height = (width / aspectRatio).toInt()

                        // Update ImageView height dynamically
                        holder.imageView.layoutParams.height = height
                        holder.imageView.requestLayout()

                        // Set the image
                        holder.imageView.setImageBitmap(resource)
                    }
                })
        }
    }

    override fun getItemCount(): Int = imageUrls!!.size
}

