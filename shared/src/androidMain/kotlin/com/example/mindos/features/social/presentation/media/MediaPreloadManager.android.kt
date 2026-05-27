package com.example.mindos.features.social.presentation.media

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.ProgressiveDownloader
import com.example.mindos.features.social.shared.util.MediaContentType
import java.io.File
import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

actual object MediaPreloadManager {
    private var cache: SimpleCache? = null
    private var context: Context? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = Collections.synchronizedSet(mutableSetOf<String>())

    @Synchronized
    fun initialize(androidContext: Context) {
        if (context != null) return
        context = androidContext.applicationContext
        setupCache()
    }

    @Synchronized
    private fun setupCache() {
        if (cache != null) return
        val ctx = context ?: return
        val cacheDir = File(ctx.cacheDir, "media_viewer_cache").apply { mkdirs() }
        cache = SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(100L * 1024L * 1024L),
            StandaloneDatabaseProvider(ctx),
        )
    }

    fun getCache(): SimpleCache? = cache

    fun getCacheDataSourceFactory(): CacheDataSource.Factory {
        val ctx = context ?: throw IllegalStateException("MediaPreloadManager not initialized")
        if (cache == null) setupCache()
        val upstream = DefaultDataSource.Factory(ctx, DefaultHttpDataSource.Factory())
        return CacheDataSource.Factory()
            .setCache(cache ?: throw IllegalStateException("Media cache not available"))
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    actual fun preload(url: String, type: MediaContentType) {
        if (type != MediaContentType.Video) return
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) return
        if (!inFlight.add(url)) return

        scope.launch {
            try {
                val mediaItem = MediaItem.fromUri(url)
                val downloader = ProgressiveDownloader(
                    mediaItem,
                    getCacheDataSourceFactory(),
                )
                downloader.download { _, bytesDownloaded, _ ->
                    if (bytesDownloaded >= 512L * 1024L) {
                        downloader.cancel()
                    }
                }
            } catch (_: Exception) {
                // Best-effort cache warmup.
            } finally {
                inFlight.remove(url)
            }
        }
    }
}
