package com.mindos.app.features.social.presentation.feed

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mindos.app.core.network.StorageDefaults
import com.mindos.app.core.network.withStorageReadTimeout
import com.mindos.app.features.social.domain.feed.GlobalFeedAttachment
import com.mindos.app.features.social.domain.feed.GlobalFeedPost
import com.mindos.app.features.social.domain.feed.GlobalFeedPostType
import com.mindos.app.features.social.presentation.media.MediaPreloadManager
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

private class AndroidFeedAudioPlayer(context: android.content.Context) : FeedAudioPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(FeedAudioPlayerState())
    override val state: StateFlow<FeedAudioPlayerState> = _state

    private val player = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_OFF
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            true
        )
        volume = 1f
        addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val durationMs = currentDuration()
                val positionMs = currentPosition.coerceAtLeast(0L)
                _state.value = _state.value.copy(
                    isPlaying = isPlaying,
                    durationMs = durationMs,
                    positionMs = positionMs,
                    progress = progress(positionMs, durationMs),
                )
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    _state.value = _state.value.copy(isPlaying = false, progress = 1f, positionMs = currentDuration(), durationMs = currentDuration())
                }
            }
        })
    }

    init {
        scope.launch {
            while (true) {
                if (_state.value.activeAudioId != null) {
                    val durationMs = currentDuration()
                    val positionMs = player.currentPosition.coerceAtLeast(0L)
                    _state.value = _state.value.copy(
                        positionMs = positionMs,
                        durationMs = durationMs,
                        progress = progress(positionMs, durationMs),
                    )
                }
                delay(42L)
            }
        }
    }

    override fun playOrPause(source: FeedAudioSource) {
        val current = _state.value
        if (current.activeAudioId == source.audioId) {
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0)
                }
                player.play()
            }
            return
        }
        val uri = source.localPath?.takeIf { it.isNotBlank() }?.let { Uri.fromFile(File(it)) }
            ?: source.remoteUrl?.takeIf { it.isNotBlank() }?.let(Uri::parse)
            ?: return
        val item = MediaItem.Builder().setUri(uri).setMimeType(source.mimeType).build()
        player.setMediaItem(item)
        player.prepare()
        player.playWhenReady = true
        player.play()
        _state.value = FeedAudioPlayerState(
            activeAudioId = source.audioId,
            isPlaying = true,
            progress = 0f,
            positionMs = 0L,
            durationMs = source.durationMs ?: 0L,
        )
    }

    override fun stop() {
        player.stop()
        _state.value = FeedAudioPlayerState()
    }

    fun dispose() {
        player.release()
        scope.cancel()
    }

    private fun currentDuration(): Long =
        player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L

    private fun progress(positionMs: Long, durationMs: Long): Float =
        if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

@Composable
actual fun rememberFeedAudioPlayer(): FeedAudioPlayer {
    val context = LocalContext.current
    val player = remember(context) { AndroidFeedAudioPlayer(context) }
    DisposableEffect(player) {
        onDispose { player.dispose() }
    }
    return player
}

private class AndroidFeedAttachmentDownloader(
    private val context: Context,
) : FeedAttachmentDownloader {
    private var job: kotlinx.coroutines.Job? = null
    private var activeDownloadId: Long? = null

    override fun download(
        attachment: GlobalFeedAttachment,
        onProgress: (Float) -> Unit,
        onStateChanged: (Boolean) -> Unit,
    ) {
        cancel()
        job = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            val resolvedUrl = resolveFeedUrl(attachment)
            if (resolvedUrl == null) {
                onStateChanged(false)
                onProgress(0f)
                return@launch
            }
            onStateChanged(true)
            onProgress(0f)
            val request = DownloadManager.Request(Uri.parse(resolvedUrl))
                .setTitle(attachment.fileName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, attachment.fileName)
                .setAllowedOverMetered(true)
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = manager.enqueue(request)
            activeDownloadId = downloadId
            val query = DownloadManager.Query().setFilterById(downloadId)
            while (true) {
                val cursor = manager.query(query)
                val keepPolling = cursor.use { result ->
                    if (!result.moveToFirst()) return@use false
                    val status = result.getInt(result.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val total = result.getLong(result.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val downloaded = result.getLong(result.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    if (total > 0L) {
                        onProgress((downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                    }
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            onProgress(1f)
                            false
                        }
                        DownloadManager.STATUS_FAILED -> false
                        else -> true
                    }
                }
                if (!keepPolling) break
                delay(120L)
            }
            onStateChanged(false)
        }
    }

    override fun cancel() {
        job?.cancel()
        job = null
        activeDownloadId = null
    }
}

@Composable
actual fun rememberFeedAttachmentDownloader(): FeedAttachmentDownloader {
    val context = LocalContext.current
    val downloader = remember(context) { AndroidFeedAttachmentDownloader(context) }
    DisposableEffect(downloader) {
        onDispose { downloader.cancel() }
    }
    return downloader
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
actual fun FeedPostMediaSurface(
    post: GlobalFeedPost,
    attachment: GlobalFeedAttachment?,
    isActive: Boolean,
    isPlaying: Boolean,
    modifier: Modifier,
) {
    when (post.postType) {
        GlobalFeedPostType.PHOTO -> FeedFullscreenImage(attachment = attachment, modifier = modifier)
        GlobalFeedPostType.VIDEO -> FeedFullscreenVideo(attachment = attachment, isPlaying = isActive && isPlaying, modifier = modifier)
        GlobalFeedPostType.TEXT,
        GlobalFeedPostType.AUDIO -> Box(
            modifier = modifier.background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Color(0xFF091615), Color(0xFF020304), Color(0xFF12201D)),
                ),
            ),
        )
    }
}

@Composable
private fun FeedFullscreenImage(
    attachment: GlobalFeedAttachment?,
    modifier: Modifier,
) {
    var model by remember(attachment?.storagePath, attachment?.thumbnailUrl, attachment?.url) { mutableStateOf<String?>(null) }
    LaunchedEffect(attachment?.storagePath, attachment?.thumbnailUrl, attachment?.url) {
        model = attachment?.let { resolveFeedImageUrl(it) }
    }
    if (model != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(model).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
            Text("CARGANDO", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun FeedFullscreenVideo(
    attachment: GlobalFeedAttachment?,
    isPlaying: Boolean,
    modifier: Modifier,
) {
    val context = LocalContext.current
    var url by remember(attachment?.storagePath, attachment?.url) { mutableStateOf<String?>(null) }
    LaunchedEffect(attachment?.storagePath, attachment?.url) {
        url = attachment?.let { resolveFeedUrl(it) }
    }
    val player = remember(url) {
        if (url == null) null else {
            MediaPreloadManager.initialize(context)
            ExoPlayer.Builder(context)
                .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(10_000, 40_000, 1_500, 3_000).build())
                .build()
                .apply {
                    val dataSourceFactory = MediaPreloadManager.getCacheDataSourceFactory()
                    val mediaItem = MediaItem.Builder().setUri(url).setMimeType(attachment?.mimeType).build()
                    val source = androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
                    setMediaSource(source)
                    repeatMode = Player.REPEAT_MODE_ONE
                    prepare()
                    playWhenReady = true
                }
        }
    }
    DisposableEffect(player) {
        onDispose { player?.release() }
    }
    LaunchedEffect(player, isPlaying) {
        if (isPlaying) player?.play() else player?.pause()
    }
    Box(modifier = modifier.background(Color.Black)) {
        if (player != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        this.player = player
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                update = { view -> if (view.player !== player) view.player = player },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            FeedFullscreenImage(attachment = attachment, modifier = Modifier.fillMaxSize())
        }
    }
}

private suspend fun resolveFeedImageUrl(attachment: GlobalFeedAttachment): String? =
    attachment.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { directOrStorageUrl(it) }
        ?: directOrStorageUrl(attachment.url)

private suspend fun resolveFeedUrl(attachment: GlobalFeedAttachment): String? =
    directOrStorageUrl(attachment.url) ?: attachment.storagePath.takeIf { it.isNotBlank() }?.let { storagePath ->
        resolveStorageDownloadUrl(storagePath)
    }

private suspend fun directOrStorageUrl(value: String): String? =
    when {
        value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true) -> value
        value.startsWith("content://", ignoreCase = true) || value.startsWith("file://", ignoreCase = true) -> value
        value.startsWith("local-feed://", ignoreCase = true) -> null
        value.isBlank() -> null
        else -> resolveStorageDownloadUrl(value)
    }

private suspend fun resolveStorageDownloadUrl(storagePath: String): String? =
    runCatching {
        withStorageReadTimeout(StorageDefaults.DownloadUrlTimeout) {
            Firebase.storage.reference.child(storagePath).downloadUrl.await().toString()
        }
    }.getOrNull()
