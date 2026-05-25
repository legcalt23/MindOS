package com.example.neuv3.features.social.presentation.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.neuv3.features.social.shared.util.MediaContentType
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import platform.AVFoundation.*
import platform.AVKit.*
import platform.Foundation.*
import platform.PDFKit.*
import platform.UIKit.*
import platform.CoreGraphics.*

@OptIn(ExperimentalForeignApi::class)
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
    when (type) {
        MediaContentType.Image -> PremiumViewerImage(
            url = url ?: thumbnailUrl ?: "", 
            thumbnailUrl = thumbnailUrl,
            onError = onError
        )
        MediaContentType.Video -> {
            if (url != null) PremiumViewerVideo(url = url, isPlaying = isPlaying, onError = onError)
            else PremiumViewerPlaceholder(thumbnailUrl = thumbnailUrl, onError = onError)
        }
        MediaContentType.Audio -> {
            if (url != null) PremiumViewerVideo(url = url, isPlaying = isPlaying, onError = onError)
            else ViewerLoading()
        }
        MediaContentType.Pdf -> PremiumViewerPdf(url = url ?: "", onError = onError)
        MediaContentType.Text -> PremiumViewerText(url = url ?: "", onError = onError)
        else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ViewerErrorView(error = MediaError.UnsupportedFormat, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun PremiumViewerPlaceholder(thumbnailUrl: String?, onError: (MediaError) -> Unit) {
    PremiumViewerImage(url = thumbnailUrl ?: "", thumbnailUrl = null, onError = onError)
}

private val sessionImageCache = mutableMapOf<String, UIImage>()

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun PremiumViewerImage(url: String, thumbnailUrl: String?, onError: (MediaError) -> Unit) {
    if (url.isEmpty()) return
    
    val placeholderImage = remember(thumbnailUrl) { thumbnailUrl?.let { sessionImageCache[it] } }
    var image by remember(url) { mutableStateOf(sessionImageCache[url]) }
    var failed by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        failed = false
        sessionImageCache[url]?.let {
            image = it
            return@LaunchedEffect
        }
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl == null) {
            failed = true
            onError(MediaError.InvalidUrl)
            return@LaunchedEffect
        }
        val loaded = withContext(Dispatchers.IO) {
            NSData.dataWithContentsOfURL(nsUrl)?.let { data -> UIImage.imageWithData(data) }
        }
        if (loaded != null) {
            sessionImageCache[url] = loaded
            if (sessionImageCache.size > 32) {
                sessionImageCache.remove(sessionImageCache.keys.first())
            }
            image = loaded
        } else {
            failed = true
            onError(MediaError.ConnectionError)
        }
    }
    
    UIKitView(
        factory = {
            UIImageView().apply {
                contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
                clipsToBounds = true
                alpha = 0f
            }
        },
        update = { view ->
            val loaded = image
            if (loaded != null && view.image != loaded) {
                UIView.animateWithDuration(0.3) {
                    view.image = loaded
                    view.alpha = 1f
                }
            } else if (view.image == null && placeholderImage != null && !failed) {
                view.image = placeholderImage
                view.alpha = 0.5f
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun PremiumViewerVideo(url: String, isPlaying: Boolean, onError: (MediaError) -> Unit) {
    val onErrorState = rememberUpdatedState(onError)
    val player = remember(url) {
        val nsUrl = NSURL.URLWithString(url) ?: run {
             onError(MediaError.InvalidUrl)
             return@remember AVPlayer() 
        }
        val asset = MediaPreloadManager.getPreloadedAsset(url) ?: AVURLAsset.assetWithURL(nsUrl)
        val playerItem = AVPlayerItem.playerItemWithAsset(asset)
        AVPlayer.playerWithPlayerItem(playerItem).apply {
            actionAtItemEnd = AVPlayerActionAtItemEndNone
        }
    }

    DisposableEffect(player) {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            AVPlayerItemFailedToPlayToEndTimeNotification,
            null,
            NSOperationQueue.mainQueue
        ) { _ ->
            onErrorState.value(MediaError.PlaybackError)
        }
        onDispose {
            player.pause()
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }

    LaunchedEffect(player, isPlaying) {
        if (isPlaying) player.play() else player.pause()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        UIKitView(
            factory = {
                val playerLayer = AVPlayerLayer.playerLayerWithPlayer(player)
                val containerView = UIView()
                containerView.layer.addSublayer(playerLayer)
                containerView
            },
            update = { view ->
                CATransaction.begin()
                CATransaction.setDisableActions(true)
                view.layer.sublayers?.filterIsInstance<AVPlayerLayer>()?.firstOrNull()?.let {
                    it.setFrame(view.bounds)
                }
                CATransaction.commit()
            },
            modifier = Modifier.fillMaxSize()
        )

        VideoProgressLine(
            player = player,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )
    }
}

@Composable
private fun VideoProgressLine(player: AVPlayer, modifier: Modifier = Modifier) {
    var progress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(player) {
        while (true) {
            val current = CMTimeGetSeconds(player.currentTime())
            val duration = CMTimeGetSeconds(player.currentItem?.duration ?: CMTime() )
            if (!duration.isNaN() && duration > 0) {
                progress = (current / duration).toFloat()
            }
            delay(32)
        }
    }

    ViewerProgressLine(
        progress = progress,
        modifier = modifier
    )
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun PremiumViewerPdf(url: String, onError: (MediaError) -> Unit) {
    UIKitView(
        factory = {
            PDFView().apply {
                autoScales = true
                displayMode = PDFDisplayModeSinglePageContinuous
                displayDirection = PDFDisplayDirectionVertical
            }
        },
        update = { view ->
            if (url.isEmpty()) return@UIKitView
            val nsUrl = NSURL.URLWithString(url)
            if (nsUrl != null) {
                val doc = PDFDocument(nsUrl)
                if (doc.pageCount == 0L) {
                    onError(MediaError.CorruptFile)
                } else {
                    view.document = doc
                }
            } else {
                onError(MediaError.InvalidUrl)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun PremiumViewerText(url: String, onError: (MediaError) -> Unit) {
    var content by remember(url) { mutableStateOf<String?>(null) }
    var loading by remember(url) { mutableStateOf(true) }

    LaunchedEffect(url) {
        loading = true
        val loaded = withContext(Dispatchers.IO) {
            val nsUrl = NSURL.URLWithString(url)
            val data = nsUrl?.let { NSData.dataWithContentsOfURL(it) }
            if (data != null) {
                NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
            } else {
                null
            }
        }
        if (loaded != null) {
            content = loaded
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
