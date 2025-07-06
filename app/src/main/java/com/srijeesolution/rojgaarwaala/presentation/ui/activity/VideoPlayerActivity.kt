package com.srijeesolution.rojgaarwaala.presentation.ui.activity

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.srijeesolution.rojgaarwaala.databinding.ActivityVideoPlayerBinding

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val videoUrl = intent.getStringExtra("video_url") ?: return

        if (isYouTubeUrl(videoUrl)) {
            playYouTubeVideo(videoUrl)
        } else {
            playCustomVideo(videoUrl)
        }
    }

    private fun isYouTubeUrl(url: String): Boolean {
        return url.contains("youtube.com") || url.contains("youtu.be")
    }

    private fun playYouTubeVideo(url: String) {
        val videoId = extractYouTubeVideoId(url) ?: return

        with(binding.youtubePlayerView) {
            visibility = View.VISIBLE
            lifecycle.addObserver(this)
            addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                override fun onReady(player: YouTubePlayer) {
                    player.loadVideo(videoId, 0f)
                }
            })
        }
    }

    private fun playCustomVideo(url: String) {
        with(binding.customVideoView) {
            visibility = View.VISIBLE
            setVideoURI(Uri.parse(url))
            setOnPreparedListener { it.start() }
            setOnCompletionListener { finish() }
        }
    }

    private fun extractYouTubeVideoId(url: String): String? {
        val regex = Regex("(?:v=|be/|embed/|shorts/)([a-zA-Z0-9_-]{11})")
        return regex.find(url)?.groupValues?.get(1)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.youtubePlayerView.release()
    }
}
