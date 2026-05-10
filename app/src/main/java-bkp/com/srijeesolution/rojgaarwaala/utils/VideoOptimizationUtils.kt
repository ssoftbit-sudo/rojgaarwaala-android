package com.srijeesolution.rojgaarwaala.utils

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter

/**
 * Utility class for video streaming optimizations
 */
@UnstableApi
object VideoOptimizationUtils {
    
    /**
     * Get optimized HTTP data source factory with better streaming performance
     */
    fun getOptimizedHttpDataSourceFactory(): DefaultHttpDataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(10000) // Reduced to 10 seconds for faster connection
            .setReadTimeoutMs(10000)    // Reduced to 10 seconds for faster reading
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(mapOf(
                "User-Agent" to "Rojgaarwaala-Android-App",
                "Accept" to "*/*",
                "Connection" to "keep-alive",
                "Cache-Control" to "max-age=7200", // Cache for 2 hours
                "Accept-Encoding" to "gzip, deflate" // Enable compression
            ))
    }
    
    /**
     * Get mobile data optimized HTTP data source factory
     */
    fun getMobileDataOptimizedHttpDataSourceFactory(): DefaultHttpDataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(5000)  // Very fast connection for mobile
            .setReadTimeoutMs(5000)     // Very fast reading for mobile
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(mapOf(
                "User-Agent" to "Rojgaarwaala-Android-App",
                "Accept" to "*/*",
                "Connection" to "keep-alive",
                "Cache-Control" to "max-age=28800", // Cache for 8 hours on mobile
                "Accept-Encoding" to "gzip, deflate, br", // Enable all compression
                "X-Requested-With" to "XMLHttpRequest", // Optimize for mobile networks
                "Pragma" to "no-cache", // Force fresh requests when needed
                "Range" to "bytes=0-" // Enable range requests for faster streaming
            ))
    }
    
    /**
     * Get ultra-aggressive mobile data optimized HTTP data source factory
     */
    fun getUltraMobileDataOptimizedHttpDataSourceFactory(): DefaultHttpDataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(3000) // 3 seconds
            .setReadTimeoutMs(3000)    // 3 seconds
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(
                mapOf(
                    "User-Agent" to "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36",
                    "Accept" to "video/*,audio/*,*/*",
                    "Accept-Encoding" to "gzip, deflate, br",
                    "Connection" to "keep-alive",
                    "Cache-Control" to "max-age=43200", // 12 hours
                    "X-Requested-With" to "XMLHttpRequest",
                    "Pragma" to "no-cache",
                    "Range" to "bytes=0-",
                    "TE" to "trailers",
                    "Upgrade-Insecure-Requests" to "1"
                )
            )
    }
    
    /**
     * Get ultra-fast HTTP data source factory for immediate playback
     */
    fun getUltraFastHttpDataSourceFactory(): DefaultHttpDataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(2000) // 2 seconds - ultra fast
            .setReadTimeoutMs(2000)    // 2 seconds - ultra fast
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(
                mapOf(
                    "User-Agent" to "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36",
                    "Accept" to "video/*,audio/*,*/*",
                    "Accept-Encoding" to "gzip, deflate, br",
                    "Connection" to "keep-alive",
                    "Cache-Control" to "max-age=86400", // 24 hours
                    "X-Requested-With" to "XMLHttpRequest",
                    "Pragma" to "no-cache",
                    "Range" to "bytes=0-",
                    "TE" to "trailers",
                    "Upgrade-Insecure-Requests" to "1",
                    "Priority" to "high"
                )
            )
    }
    
    /**
     * Get optimized load control for better buffering
     */
    fun getOptimizedLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                3000,  // Reduced min buffer duration
                20000, // Reduced max buffer duration
                500,   // Reduced buffer for playback
                500    // Reduced buffer for playback after rebuffer
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }
    
    /**
     * Get aggressive load control for faster start
     */
    fun getAggressiveLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                2000,  // Very low min buffer for faster start
                15000, // Moderate max buffer
                300,   // Very low buffer for playback
                300    // Very low buffer for playback after rebuffer
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }
    
    /**
     * Get bandwidth meter for adaptive streaming
     */
    fun getBandwidthMeter(context: Context): DefaultBandwidthMeter {
        return DefaultBandwidthMeter.Builder(context).build()
    }
    
    /**
     * Check if device has good network connection for high quality streaming
     */
    fun isGoodNetworkConnection(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo?.isConnected == true && (
            networkInfo.type == android.net.ConnectivityManager.TYPE_WIFI ||
            networkInfo.type == android.net.ConnectivityManager.TYPE_ETHERNET ||
            (networkInfo.type == android.net.ConnectivityManager.TYPE_MOBILE && 
             networkInfo.subtype >= android.telephony.TelephonyManager.NETWORK_TYPE_HSPA)
        )
    }
    
    /**
     * Check if device is using mobile data
     */
    fun isMobileDataConnection(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo?.isConnected == true && 
               networkInfo.type == android.net.ConnectivityManager.TYPE_MOBILE
    }
    
    /**
     * Get optimal buffer settings based on network quality
     */
    fun getOptimalBufferSettings(context: Context): DefaultLoadControl {
        return if (isGoodNetworkConnection(context)) {
            if (isMobileDataConnection(context)) {
                if (isHighSpeedMobileNetwork(context)) {
                    getUltraMobileDataLoadControl() // Ultra-aggressive for 4G/5G
                } else {
                    getMobileDataOptimizedLoadControl() // Standard mobile optimization
                }
            } else {
                getOptimizedLoadControl() // WiFi optimization
            }
        } else {
            getAggressiveLoadControl() // Poor network optimization
        }
    }
    
    /**
     * Get mobile data optimized load control for faster streaming
     */
    fun getMobileDataOptimizedLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                2000,  // Very low min buffer for mobile
                15000, // Moderate max buffer
                300,   // Very low buffer for playback
                300    // Very low buffer for playback after rebuffer
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }
    
    /**
     * Get ultra-aggressive mobile data load control for fastest streaming
     */
    fun getUltraMobileDataLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                1000,  // Ultra low min buffer
                10000, // Lower max buffer
                200,   // Ultra low buffer for playback
                200    // Ultra low buffer for playback after rebuffer
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }
    
    /**
     * Get ultra-fast load control for immediate playback
     */
    fun getUltraFastLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                200,   // Ultra low min buffer - 0.2 seconds
                5000,  // Low max buffer - 5 seconds
                100,   // Ultra low buffer for playback - 0.1 seconds
                100    // Ultra low buffer for playback after rebuffer - 0.1 seconds
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }
    
    /**
     * Get instant load control for zero buffering
     */
    fun getInstantLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                100,   // Instant min buffer - 0.1 seconds
                2000,  // Very low max buffer - 2 seconds
                50,    // Instant buffer for playback - 0.05 seconds
                50     // Instant buffer for playback after rebuffer - 0.05 seconds
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }
    
    /**
     * Get adaptive quality load control based on network conditions
     */
    fun getAdaptiveQualityLoadControl(context: Context): DefaultLoadControl {
        val qualitySettings = getAdaptiveQualitySettings(context)
        
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                qualitySettings.bufferSize,           // Min buffer based on quality
                qualitySettings.bufferSize * 5,       // Max buffer (5x min)
                qualitySettings.bufferSize / 2,       // Buffer for playback
                qualitySettings.bufferSize / 2        // Buffer for playback after rebuffer
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }
    
    /**
     * Check mobile network quality for ultra optimization
     */
    fun isHighSpeedMobileNetwork(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo?.isConnected == true && 
               networkInfo.type == android.net.ConnectivityManager.TYPE_MOBILE &&
               (networkInfo.subtype >= android.telephony.TelephonyManager.NETWORK_TYPE_LTE ||
                networkInfo.subtype >= android.telephony.TelephonyManager.NETWORK_TYPE_NR)
    }
    
    /**
     * Check if network is slow (2G/3G or poor WiFi)
     */
    fun isSlowNetwork(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        
        return if (networkInfo?.isConnected == true) {
            when (networkInfo.type) {
                android.net.ConnectivityManager.TYPE_MOBILE -> {
                    // Consider 2G and 3G as slow networks
                    networkInfo.subtype < android.telephony.TelephonyManager.NETWORK_TYPE_HSPA
                }
                android.net.ConnectivityManager.TYPE_WIFI -> {
                    // For WiFi, we'll use bandwidth detection
                    false // Will be determined by bandwidth meter
                }
                else -> false
            }
        } else {
            false
        }
    }
    
    /**
     * Get adaptive quality settings based on network conditions
     */
    fun getAdaptiveQualitySettings(context: Context): AdaptiveQualitySettings {
        return when {
            isHighSpeedMobileNetwork(context) -> AdaptiveQualitySettings.HIGH_QUALITY
            isSlowNetwork(context) -> AdaptiveQualitySettings.LOW_QUALITY
            isGoodNetworkConnection(context) -> AdaptiveQualitySettings.MEDIUM_QUALITY
            else -> AdaptiveQualitySettings.LOW_QUALITY
        }
    }
    
    /**
     * Adaptive quality settings enum
     */
    enum class AdaptiveQualitySettings(val maxBitrate: Int, val maxHeight: Int, val bufferSize: Int) {
        LOW_QUALITY(300_000, 360, 500),       // 300kbps, 360p, 0.5s buffer - Ultra fast
        MEDIUM_QUALITY(800_000, 480, 1000),   // 800kbps, 480p, 1s buffer - Fast
        HIGH_QUALITY(1_500_000, 720, 2000)    // 1.5Mbps, 720p, 2s buffer - Standard
    }
    
    /**
     * Get recommended cache size based on available storage and network quality
     */
    fun getRecommendedCacheSize(context: Context): Long {
        val availableSpace = context.cacheDir.freeSpace
        val isGoodNetwork = isGoodNetworkConnection(context)
        val isMobileData = isMobileDataConnection(context)
        
        return when {
            availableSpace > 500 * 1024 * 1024 && isGoodNetwork && isMobileData -> 400 * 1024 * 1024L // 400MB for mobile with good network
            availableSpace > 500 * 1024 * 1024 && isGoodNetwork -> 300 * 1024 * 1024L // 300MB for WiFi with good network
            availableSpace > 500 * 1024 * 1024 && isMobileData -> 250 * 1024 * 1024L // 250MB for mobile with poor network
            availableSpace > 500 * 1024 * 1024 -> 200 * 1024 * 1024L // 200MB for poor network
            availableSpace > 200 * 1024 * 1024 -> 150 * 1024 * 1024L // 150MB
            else -> 100 * 1024 * 1024L // 100MB default
        }
    }
    
    /**
     * Get zero-buffer load control for instant playback
     */
    fun getZeroBufferLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                50,    // Zero min buffer - 0.05 seconds
                1000,  // Minimal max buffer - 1 second
                25,    // Zero buffer for playback - 0.025 seconds
                25     // Zero buffer for playback after rebuffer - 0.025 seconds
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }
    
    /**
     * Get instant HTTP data source factory for zero delay
     */
    fun getInstantHttpDataSourceFactory(): DefaultHttpDataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(1000) // 1 second - instant
            .setReadTimeoutMs(1000)    // 1 second - instant
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(
                mapOf(
                    "User-Agent" to "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36",
                    "Accept" to "video/*,audio/*,*/*",
                    "Accept-Encoding" to "gzip, deflate, br",
                    "Connection" to "keep-alive",
                    "Cache-Control" to "max-age=86400", // 24 hours
                    "X-Requested-With" to "XMLHttpRequest",
                    "Pragma" to "no-cache",
                    "Range" to "bytes=0-",
                    "TE" to "trailers",
                    "Upgrade-Insecure-Requests" to "1",
                    "Priority" to "high",
                    "X-Priority" to "1"
                )
            )
    }
} 