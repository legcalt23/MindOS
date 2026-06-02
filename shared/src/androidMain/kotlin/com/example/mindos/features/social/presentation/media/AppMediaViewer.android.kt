package com.example.mindos.features.social.presentation.media

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mindos.core.network.StorageDefaults
import com.example.mindos.core.network.withStorageReadTimeout
import com.example.mindos.features.social.shared.util.MediaContentType
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

@Composable
actual fun NativeMediaRenderer(
    url: String?,
    storagePath: String,
    thumbnailUrl: String?,
    type: MediaContentType,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onError: (MediaError) -> Unit,
    modifier: Modifier,
) {
    BackHandler(onBack = onDismiss)

    Box(modifier = modifier.fillMaxSize()) {
        when (type) {
        MediaContentType.Image -> PremiumViewerImage(
            url = url ?: thumbnailUrl,
            thumbnailUrl = thumbnailUrl,
            onError = onError
        )
        MediaContentType.Video -> {
            if (url != null) PremiumViewerVideo(url = url, isPlaying = isPlaying, onError = onError)
            else PremiumViewerPlaceholder(thumbnailUrl = thumbnailUrl, onError = onError)
        }
        MediaContentType.Audio -> {
            if (url != null) PremiumViewerVideo(
                url = url,
                isPlaying = isPlaying,
                onError = onError,
                showVideoSurface = false,
            ) else ViewerLoading()
        }
        MediaContentType.Pdf -> PremiumViewerPdf(storagePath = storagePath, onError = onError)
        MediaContentType.Text -> PremiumViewerText(storagePath = storagePath, onError = onError)
        else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
             ViewerErrorView(error = MediaError.UnsupportedFormat, onDismiss = onDismiss)
        }
        }
    }
}

@Composable
private fun PremiumViewerPlaceholder(thumbnailUrl: String?, onError: (MediaError) -> Unit) {
    if (thumbnailUrl.isNullOrBlank()) {
        ViewerLoading()
        return
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(thumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onState = { state ->
                if (state is coil.compose.AsyncImagePainter.State.Error) {
                    onError(MediaError.DecodingImageError)
                }
            },
            modifier = Modifier.fillMaxSize(0.95f)
        )
    }
}

@Composable
private fun PremiumViewerImage(url: String?, thumbnailUrl: String?, onError: (MediaError) -> Unit) {
    if (url.isNullOrBlank()) {
        ViewerLoading()
        return
    }

    var scale by remember(url) { mutableStateOf(1f) }
    var offsetX by remember(url) { mutableStateOf(0f) }
    var offsetY by remember(url) { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(url) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 4f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .placeholderMemoryCacheKey(thumbnailUrl)
                .crossfade(400)
                .build(),
            contentDescription = "Media",
            contentScale = ContentScale.Fit,
            onState = { state ->
                if (state is coil.compose.AsyncImagePainter.State.Error) {
                    onError(state.result.throwable.toMediaError(default = MediaError.DecodingImageError))
                }
            },
            modifier = Modifier
                .fillMaxSize(0.95f)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                )
        )
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun PremiumViewerVideo(
    url: String,
    isPlaying: Boolean,
    onError: (MediaError) -> Unit,
    showVideoSurface: Boolean = true,
    mediaItemMimeType: String? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onErrorState = rememberUpdatedState(onError)
    val isPlayingState = rememberUpdatedState(isPlaying)
    
    val exoPlayer = remember(url, mediaItemMimeType) {
        MediaPreloadManager.initialize(context)
        val dataSourceFactory = MediaPreloadManager.getCacheDataSourceFactory()
        
        ExoPlayer.Builder(context)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(10_000, 40_000, 1_500, 3_000)
                    .build()
            )
            .build().apply {
                val mediaItem = MediaItem.Builder()
                    .setUri(url)
                    .apply {
                        if (mediaItemMimeType != null) setMimeType(mediaItemMimeType)
                    }
                    .build()
                val mediaSource = androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
                setMediaSource(mediaSource)
                
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        val mediaError = when (error.errorCode) {
                            androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                            androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> MediaError.NetworkTimeout
                            androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                            androidx.media3.common.PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> MediaError.ConnectionError
                            androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> MediaError.UnsupportedFormat
                            androidx.media3.common.PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> MediaError.NotFound
                            else -> MediaError.PlaybackError
                        }
                        onErrorState.value(mediaError)
                    }
                })
                
                prepare()
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_ONE
            }
    }

    DisposableEffect(exoPlayer, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (isPlayingState.value) exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer, isPlaying) {
        if (isPlaying) exoPlayer.play() else exoPlayer.pause()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (showVideoSurface) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                update = { view ->
                    if (view.player !== exoPlayer) view.player = exoPlayer
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            PremiumViewerAudioSurface(modifier = Modifier.align(Alignment.Center))
        }
        
        VideoProgressLine(
            player = exoPlayer,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )
    }
}

@Composable
private fun PremiumViewerAudioSurface(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ViewerDesign.Green.copy(alpha = 0.08f))
                .border(1.dp, ViewerDesign.Green.copy(alpha = 0.22f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "WAV",
                color = ViewerDesign.Green,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "AUDIO",
            color = ViewerDesign.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun VideoProgressLine(player: Player, modifier: Modifier = Modifier) {
    var progress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(player) {
        while (true) {
            if (player.isPlaying) {
                val duration = player.duration
                if (duration > 0L && duration != C.TIME_UNSET) {
                    progress = (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
                }
            }
            delay(32)
        }
    }

    ViewerProgressLine(
        progress = progress,
        modifier = modifier
    )
}

@Composable
private fun PremiumViewerPdf(storagePath: String, onError: (MediaError) -> Unit) {
    val context = LocalContext.current
    var pages by remember(storagePath) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var loading by remember(storagePath) { mutableStateOf(true) }

    LaunchedEffect(storagePath) {
        loading = true
        when (val result = withContext(Dispatchers.IO) { loadPdfPages(context.cacheDir, storagePath) }) {
            is PdfLoadResult.Success -> {
                pages = result.pages
                loading = false
            }
            is PdfLoadResult.Failure -> {
                pages = emptyList()
                loading = false
                onError(result.error)
            }
        }
    }

    DisposableEffect(pages) {
        onDispose {
            pages.forEach { it.recycle() }
        }
    }

    if (loading) ViewerLoading()
    else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 100.dp, bottom = 120.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(pages) { _, bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, ViewerDesign.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
private fun PremiumViewerText(storagePath: String, onError: (MediaError) -> Unit) {
    var content by remember(storagePath) { mutableStateOf<String?>(null) }
    var loading by remember(storagePath) { mutableStateOf(true) }

    LaunchedEffect(storagePath) {
        loading = true
        val bytes = withContext(Dispatchers.IO) {
            downloadStorageBytes(storagePath, 2L * 1024L * 1024L)
        }
        if (bytes != null) {
            content = bytes.decodeToString()
        } else {
            content = null
            onError(MediaError.NotFound)
        }
        loading = false
    }

    if (loading) ViewerLoading()
    else if (content != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(120.dp))
            Text(
                text = content!!,
                color = ViewerDesign.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp,
                    letterSpacing = 0.2.sp
                )
            )
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

private sealed interface PdfLoadResult {
    data class Success(val pages: List<Bitmap>) : PdfLoadResult
    data class Failure(val error: MediaError) : PdfLoadResult
}

private suspend fun loadPdfPages(cacheDir: File, storagePath: String): PdfLoadResult {
    val bytes = downloadStorageBytes(storagePath, 32L * 1024L * 1024L)
        ?: return PdfLoadResult.Failure(MediaError.ConnectionError)

    var fd: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
    var tempFile: File? = null
    val rendered = mutableListOf<Bitmap>()
    return try {
        tempFile = File.createTempFile("v_pdf_", ".pdf", cacheDir)
        tempFile.writeBytes(bytes)
        fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(fd)
        val pageCount = renderer.pageCount.coerceAtMost(MaxRenderedPdfPages)
        if (pageCount <= 0) {
            PdfLoadResult.Failure(MediaError.CorruptFile)
        } else {
            repeat(pageCount) { index ->
                val page = renderer.openPage(index)
                try {
                    val scale = min(2f, MaxPdfBitmapWidthPx / page.width.toFloat()).coerceAtLeast(1f)
                    val width = (page.width * scale).toInt().coerceAtLeast(1)
                    val height = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(
                        bitmap,
                        Rect(0, 0, width, height),
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                    )
                    rendered += bitmap
                } finally {
                    page.close()
                }
            }
            PdfLoadResult.Success(rendered.toList())
        }
    } catch (_: OutOfMemoryError) {
        rendered.forEach { it.recycle() }
        PdfLoadResult.Failure(MediaError.InsufficientMemory)
    } catch (_: java.io.FileNotFoundException) {
        rendered.forEach { it.recycle() }
        PdfLoadResult.Failure(MediaError.NotFound)
    } catch (_: Exception) {
        rendered.forEach { it.recycle() }
        PdfLoadResult.Failure(MediaError.RenderingPdfError)
    } finally {
        renderer?.close()
        fd?.close()
        tempFile?.delete()
    }
}

private suspend fun downloadStorageBytes(storagePath: String, maxBytes: Long): ByteArray? =
    try {
        val timeout = if (maxBytes > LARGE_STORAGE_READ_THRESHOLD_BYTES) {
            StorageDefaults.LargeReadTimeout
        } else {
            StorageDefaults.SmallReadTimeout
        }
        withStorageReadTimeout(timeout) {
            Firebase.storage.reference.child(storagePath).getBytes(maxBytes).await()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

private fun Throwable.toMediaError(default: MediaError): MediaError =
    when (this) {
        is java.net.UnknownHostException -> MediaError.NetworkUnavailable
        is java.net.SocketTimeoutException -> MediaError.NetworkTimeout
        is java.io.FileNotFoundException -> MediaError.NotFound
        is java.io.IOException -> MediaError.ConnectionError
        else -> default
    }

private const val LARGE_STORAGE_READ_THRESHOLD_BYTES = 5L * 1024L * 1024L

private const val MaxRenderedPdfPages = 12
private const val MaxPdfBitmapWidthPx = 1440f
