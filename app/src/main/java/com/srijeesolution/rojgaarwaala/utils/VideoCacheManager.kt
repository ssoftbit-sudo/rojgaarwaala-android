package com.srijeesolution.rojgaarwaala.utils

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.cache.CacheDataSource
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
    private const val TAG = "VideoCacheManager"
    private const val MAX_PREFETCH_VIDEOS = 3
    private const val PREFETCH_BYTES_MOBILE = 256L * 1024L // 256KB on mobile data.
    private const val PREFETCH_BYTES_WIFI = 1024L * 1024L // 1MB on regular Wi-Fi.
    private const val PREFETCH_BYTES_GOOD_WIFI = 2L * 1024L * 1024L // 2MB on strong Wi-Fi/ethernet.
    
    private var cache: Cache? = null
    private val cacheLock = Object()
    private val preloadExecutor = Executors.newFixedThreadPool(3) // 3 background threads
    private val preloadedVideos = ConcurrentHashMap.newKeySet<String>() // Track preloaded videos safely
    private val inFlightPrefetch = ConcurrentHashMap.newKeySet<String>()
    
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
        val appContext = context.applicationContext
        val candidates = videoUrls
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { preloadedVideos.contains(it) }
            .take(MAX_PREFETCH_VIDEOS)
            .toList()

        if (candidates.isEmpty()) {
            return
        }

        candidates.forEach { videoUrl ->
            if (!inFlightPrefetch.add(videoUrl)) return@forEach
            preloadExecutor.execute {
                try {
                    prefetchSingleVideo(appContext, videoUrl)
                } finally {
                    inFlightPrefetch.remove(videoUrl)
                }
            }
        }
    }

    private fun prefetchSingleVideo(context: Context, videoUrl: String) {
        try {
            if (preloadedVideos.contains(videoUrl)) {
                return
            }
            val prefetchBytes = getAdaptivePrefetchBytes(context)

            // Warm cache by reading the first chunk through CacheDataSource. No decoder is created.
            val httpDataSourceFactory = VideoOptimizationUtils.getInstantHttpDataSourceFactory()
            val cacheDataSource = CacheDataSource.Factory()
                .setCache(getCache(context))
                .setUpstreamDataSourceFactory(httpDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                .createDataSource()

            val dataSpec = DataSpec.Builder()
                .setUri(Uri.parse(videoUrl))
                .setPosition(0)
                .setLength(prefetchBytes)
                .build()

            readToCache(cacheDataSource, dataSpec, prefetchBytes)
            preloadedVideos.add(videoUrl)
            Log.d(TAG, "Cache warmed for: $videoUrl (${prefetchBytes / 1024}KB)")
        } catch (e: Exception) {
            Log.e(TAG, "Error warming cache for $videoUrl: ${e.message}")
        }
    }

    private fun getAdaptivePrefetchBytes(context: Context): Long {
        return when {
            VideoOptimizationUtils.isMobileDataConnection(context) -> PREFETCH_BYTES_MOBILE
            VideoOptimizationUtils.isGoodNetworkConnection(context) -> PREFETCH_BYTES_GOOD_WIFI
            else -> PREFETCH_BYTES_WIFI
        }
    }

    private fun readToCache(
        dataSource: DataSource,
        dataSpec: DataSpec,
        maxBytes: Long
    ) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var totalRead = 0L
        try {
            dataSource.open(dataSpec)
            while (totalRead < maxBytes) {
                val toRead = minOf(buffer.size.toLong(), maxBytes - totalRead).toInt()
                val read = dataSource.read(buffer, 0, toRead)
                if (read == C.RESULT_END_OF_INPUT) break
                if (read > 0) totalRead += read
            }
        } finally {
            dataSource.close()
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
            inFlightPrefetch.clear()
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
            inFlightPrefetch.clear()
            preloadedVideos.clear()
            
            // Clear cache
            cache?.release()
            cache = null
        }
    }
} 