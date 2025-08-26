package com.srijeesolution.rojgaarwaala.utils

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Singleton manager for video cache to prevent multiple SimpleCache instances
 * from using the same directory
 */
@UnstableApi
object VideoCacheManager {
    
    private var cache: Cache? = null
    private val cacheLock = Object()
    private val preloadPlayers = ConcurrentHashMap<String, ExoPlayer>()
    private val preloadExecutor = Executors.newFixedThreadPool(3) // 3 background threads
    private val preloadHandler = Handler(Looper.getMainLooper())
    private val preloadedVideos = mutableSetOf<String>() // Track preloaded videos
    
    /**
     * Get or create the singleton cache instance
     */
    fun getCache(context: Context): Cache {
        synchronized(cacheLock) {
            if (cache == null) {
                val cacheSize = VideoOptimizationUtils.getRecommendedCacheSize(context)
                val cacheEvictor = LeastRecentlyUsedCacheEvictor(cacheSize)
                val cacheDir = File(context.cacheDir, "video_cache")
                
                // Ensure cache directory exists
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                
                cache = SimpleCache(cacheDir, cacheEvictor)
            }
            return cache!!
        }
    }
    
    /**
     * Preload videos in background for better user experience
     */
    fun preloadVideosInBackground(context: Context, videoUrls: List<String>) {
        preloadExecutor.execute {
            try {
                Log.d("VideoCacheManager", "Starting background preload for ${videoUrls.size} videos")
                
                videoUrls.forEachIndexed { index, videoUrl ->
                    if (videoUrl.isNotEmpty()) {
                        // Add delay between preloads to avoid overwhelming the network
                        if (index > 0) {
                            Thread.sleep(500) // 500ms delay between preloads
                        }
                        
                        preloadSingleVideo(context, videoUrl)
                    }
                }
                
                Log.d("VideoCacheManager", "Background preload completed for ${videoUrls.size} videos")
            } catch (e: Exception) {
                Log.e("VideoCacheManager", "Error in background preload: ${e.message}")
            }
        }
    }

    /**
     * Preload a single video in background
     */
    private fun preloadSingleVideo(context: Context, videoUrl: String) {
        try {
            // Check if already preloading
            if (preloadPlayers.containsKey(videoUrl)) {
                return
            }

            // Create optimized data source factory
            val httpDataSourceFactory = VideoOptimizationUtils.getInstantHttpDataSourceFactory()
            val dataSourceFactory = CacheDataSource.Factory()
                .setCache(getCache(context))
                .setUpstreamDataSourceFactory(httpDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            // Create preload player
            val preloadPlayer = ExoPlayer.Builder(context)
                .setLoadControl(VideoOptimizationUtils.getZeroBufferLoadControl())
                .build()

            // Store player reference
            preloadPlayers[videoUrl] = preloadPlayer

            // Create media item and start preloading
            val mediaItem = MediaItem.Builder().setUri(videoUrl).build()
            preloadPlayer.setMediaItem(mediaItem)
            preloadPlayer.prepare()

            // Mark as preloaded
            preloadedVideos.add(videoUrl)

            // Preload for 3 seconds then release
            preloadHandler.postDelayed({
                releasePreloadPlayer(videoUrl)
            }, 3000)

            Log.d("VideoCacheManager", "Started preloading: $videoUrl")
            
        } catch (e: Exception) {
            Log.e("VideoCacheManager", "Error preloading video $videoUrl: ${e.message}")
            releasePreloadPlayer(videoUrl)
        }
    }

    /**
     * Release preload player and clean up
     */
    private fun releasePreloadPlayer(videoUrl: String) {
        try {
            val player = preloadPlayers.remove(videoUrl)
            player?.release()
            Log.d("VideoCacheManager", "Released preload player for: $videoUrl")
        } catch (e: Exception) {
            Log.e("VideoCacheManager", "Error releasing preload player: ${e.message}")
        }
    }

    /**
     * Check if video is cached
     */
    fun isVideoCached(videoUrl: String): Boolean {
        return try {
            // Check if video was preloaded
            preloadedVideos.contains(videoUrl)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): String {
        return try {
            val cache = cache
            if (cache != null) {
                val cacheSize = cache.cacheSpace
                val maxCacheSize = cache.cacheSpace
                "Cache: ${cacheSize / 1024 / 1024}MB / ${maxCacheSize / 1024 / 1024}MB"
            } else {
                "Cache: Not initialized"
            }
        } catch (e: Exception) {
            "Cache: Error getting stats"
        }
    }

    /**
     * Release the cache instance
     */
    fun releaseCache() {
        synchronized(cacheLock) {
            // Release all preload players
            preloadPlayers.values.forEach { player ->
                try {
                    player.release()
                } catch (e: Exception) {
                    Log.e("VideoCacheManager", "Error releasing player: ${e.message}")
                }
            }
            preloadPlayers.clear()
            preloadedVideos.clear()
            
            // Release cache
            cache?.release()
            cache = null
        }
    }
    
    /**
     * Clear the cache
     */
    fun clearCache() {
        synchronized(cacheLock) {
            // Release all preload players
            preloadPlayers.values.forEach { player ->
                try {
                    player.release()
                } catch (e: Exception) {
                    Log.e("VideoCacheManager", "Error releasing player: ${e.message}")
                }
            }
            preloadPlayers.clear()
            preloadedVideos.clear()
            
            // Clear cache
            cache?.release()
            cache = null
        }
    }
} 