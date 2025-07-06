package com.srijeesolution.rojgaarwaala.presentation.adaptor

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.recyclerview.widget.RecyclerView
import com.srijeesolution.rojgaarwaala.R
import com.srijeesolution.rojgaarwaala.data.remote.model.BannerList
import java.util.regex.Pattern

class YouTubePagerAdapter(private val imageUrls: ArrayList<BannerList>?) : RecyclerView.Adapter<YouTubePagerAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val webView: WebView = itemView.findViewById(R.id.webView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.youtube_item, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        // Load image using Glide or any other image loading library
        if (!TextUtils.isEmpty(imageUrls?.get(position)?.imageUrl)) {
            val webSettings: WebSettings = holder.webView.settings
            webSettings.javaScriptEnabled = true
            webSettings.domStorageEnabled = true
            webSettings.loadWithOverviewMode = true
            webSettings.useWideViewPort = true

            holder.webView.webChromeClient = WebChromeClient()
            holder.webView.webViewClient = WebViewClient()

            // The dynamic YouTube video URL
            val youtubeUrl = imageUrls?.get(position)?.imageUrl?:""

            // Extract video ID from URL
            val videoId = extractYoutubeVideoId(youtubeUrl)

            // Check if the videoId is valid
            if (videoId != null) {
                // Load the video using YouTube Player API via JavaScript
                loadYouTubeVideoWithJS(holder.webView, videoId)
            } else {
                // Handle invalid YouTube URL case
            }
        }
    }
    // Function to extract YouTube video ID
    private fun extractYoutubeVideoId(url: String): String? {
        val pattern = "(?:youtube(?:-nocookie)?\\.com/(?:[^/\\n\\s]+/\\S+/|(?:v|e(?:mbed)?)/|\\S*?[?&]v=)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
        val regex = Pattern.compile(pattern)
        val matcher = regex.matcher(url)
        return if (matcher.find()) matcher.group(1) else null
    }

    // Function to load the YouTube video with JS control (No autoplay)
    private fun loadYouTubeVideoWithJS(webView: WebView, videoId: String) {
        val html = """
            <html>
            <body>
            <div id="player"></div>
            <script>
              var tag = document.createElement('script');
              tag.src = "https://www.youtube.com/iframe_api";
              var firstScriptTag = document.getElementsByTagName('script')[0];
              firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

              var player;
              function onYouTubeIframeAPIReady() {
                player = new YT.Player('player', {
                  height: '100%',
                  width: '100%',
                  videoId: '$videoId',
                  events: {
                    'onReady': onPlayerReady
                  },
                  playerVars: {
                    'controls': 1,   // Show player controls
                    'autoplay': 0,   // Do not autoplay
                    'rel': 0,        // Don't show related videos after the video ends
                    'showinfo': 0    // Hide video title
                  }
                });
              }

              function onPlayerReady(event) {
                // Do nothing - user will click to play the video
              }
            </script>
            </body>
            </html>
        """
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    override fun getItemCount(): Int = imageUrls!!.size
}

