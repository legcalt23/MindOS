package com.example.neuv3.features.social.presentation.profile

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.neuv3.features.social.domain.model.ProfileMediaUpload
import com.example.neuv3.features.social.domain.model.ProfileMediaUploadKind
import com.example.neuv3.features.social.domain.model.ProfileModuleId
import com.example.neuv3.features.social.domain.model.ProfileThumbnailMaxBytes
import com.example.neuv3.features.social.domain.model.maxAllowedBytes
import com.example.neuv3.features.social.domain.model.maxAllowedBytesMessage
import com.example.neuv3.features.social.domain.model.normalizedProfileMimeType
import com.example.neuv3.features.social.shared.util.logProfileSave
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.android
import dev.gitlive.firebase.storage.storage
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

@Composable
actual fun rememberProfileMediaPicker(
    onPicked: (ProfileModuleId, ProfileMediaUpload) -> Unit,
    onError: (String) -> Unit,
): ProfileMediaPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnPicked by rememberUpdatedState(onPicked)
    val currentOnError by rememberUpdatedState(onError)
    var pendingRequest by remember { mutableStateOf<PendingPickerRequest?>(null) }

    fun handleUri(uri: Uri?, kind: ProfileMediaUploadKind) {
        val request = pendingRequest ?: return
        pendingRequest = null
        if (uri == null) return
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val resolver = context.contentResolver
                    val mimeType = (resolver.getType(uri) ?: kind.defaultMimeType()).normalizedProfileMimeType()
                    val reportedSize = resolver.sizeBytes(uri)
                    logProfileSave(
                        "event=media_pick_start module=${request.moduleId.persistedName} kind=$kind " +
                            "mime=$mimeType reported_size_known=${reportedSize != null}"
                    )
                    val maxBytes = kind.maxAllowedBytes(request.moduleId)
                    if (reportedSize != null && reportedSize > maxBytes) {
                        error(kind.maxAllowedBytesMessage(request.moduleId))
                    }
                    val bytes = resolver.readBytesBounded(uri, maxBytes + 1)
                    if (bytes.size.toLong() > maxBytes) {
                        error(kind.maxAllowedBytesMessage(request.moduleId))
                    }
                    val durationMillis = if (kind == ProfileMediaUploadKind.Video) {
                        readVideoDurationMillis(context.contentResolver, uri)
                    } else {
                        null
                    }
                    val thumbnailBytes = if (kind == ProfileMediaUploadKind.Video) {
                        readVideoThumbnailBytes(context.contentResolver, uri)
                    } else {
                        null
                    }
                    ProfileMediaUpload(
                        kind = kind,
                        bytes = bytes,
                        thumbnailBytes = thumbnailBytes,
                        mimeType = mimeType,
                        originalName = resolver.displayName(uri),
                        durationMillis = durationMillis,
                    )
                }
            }.fold(
                onSuccess = { upload -> currentOnPicked(request.moduleId, upload) },
                onFailure = { error ->
                    logProfileSave(
                        "event=media_pick_failure module=${request.moduleId.persistedName} kind=$kind " +
                            "exception=${error::class.simpleName.orEmpty()}"
                    )
                    currentOnError(error.message ?: "No se pudo preparar el contenido.")
                },
            )
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        handleUri(uri, ProfileMediaUploadKind.Photo)
    }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        handleUri(uri, ProfileMediaUploadKind.Video)
    }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        handleUri(uri, ProfileMediaUploadKind.Audio)
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        handleUri(uri, ProfileMediaUploadKind.File)
    }

    return remember(imageLauncher, videoLauncher, audioLauncher, fileLauncher) {
        object : ProfileMediaPicker {
            override fun pick(moduleId: ProfileModuleId, kind: ProfileMediaUploadKind) {
                pendingRequest = PendingPickerRequest(moduleId, kind)
                when (kind) {
                    ProfileMediaUploadKind.Photo -> imageLauncher.launch("image/*")
                    ProfileMediaUploadKind.Video -> videoLauncher.launch("video/*")
                    ProfileMediaUploadKind.Audio -> audioLauncher.launch("audio/*")
                    ProfileMediaUploadKind.File -> fileLauncher.launch("*/*")
                }
            }
        }
    }
}

@Composable
actual fun ProfilePlatformMediaPreview(
    source: ProfileMediaRenderSource,
    modifier: Modifier,
    contentScale: ContentScale,
    fallback: @Composable () -> Unit,
) {
    var model by remember(
        source.storagePath,
        source.thumbnailStoragePath,
        source.pendingPreviewBytes,
        source.isVideo,
        source.directUrl,
        source.thumbnailUrl,
    ) {
        mutableStateOf<Any?>(null)
    }
    var failed by remember(
        source.storagePath,
        source.thumbnailStoragePath,
        source.pendingPreviewBytes,
        source.isVideo,
        source.directUrl,
        source.thumbnailUrl,
    ) {
        mutableStateOf(false)
    }

    LaunchedEffect(
        source.storagePath,
        source.thumbnailStoragePath,
        source.pendingPreviewBytes,
        source.isVideo,
        source.directUrl,
        source.thumbnailUrl,
    ) {
        failed = false
        model = null
        val pendingBytes = source.pendingPreviewBytes
        if (pendingBytes != null) {
            model = pendingBytes
        } else {
            val directModel = source.thumbnailUrl
                ?.takeIf { it.isNotBlank() }
                ?: source.directUrl?.takeIf { it.isNotBlank() }
            if (directModel != null) {
                model = directModel
            } else {
                val path = source.thumbnailStoragePath ?: source.storagePath?.takeIf { !source.isVideo }
                if (path.isNullOrBlank()) {
                    failed = true
                } else {
                    val resolved = try {
                        resolveStorageUrl(path)
                    } catch (e: CancellationException) {
                        throw e
                    }
                    if (resolved != null) {
                        model = resolved
                    } else {
                        failed = true
                    }
                }
            }
        }
    }

    if (model != null && !failed) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(model)
                .crossfade(true)
                .build(),
            contentDescription = source.contentDescription,
            contentScale = contentScale,
            onState = { state ->
                if (state is coil.compose.AsyncImagePainter.State.Error) {
                    failed = true
                }
            },
            modifier = modifier,
        )
    } else {
        fallback()
    }
}

private suspend fun resolveStorageUrl(storagePath: String): String? =
    suspendCancellableCoroutine { continuation ->
        Firebase.storage.reference
            .child(storagePath)
            .android
            .downloadUrl
            .addOnSuccessListener { uri ->
                if (continuation.isActive) continuation.resume(uri.toString())
            }
            .addOnFailureListener {
                if (continuation.isActive) continuation.resume(null)
            }
}

private data class PendingPickerRequest(
    val moduleId: ProfileModuleId,
    val kind: ProfileMediaUploadKind,
)

private fun ProfileMediaUploadKind.defaultMimeType(): String =
    when (this) {
        ProfileMediaUploadKind.Photo -> "image/jpeg"
        ProfileMediaUploadKind.Video -> "video/mp4"
        ProfileMediaUploadKind.Audio -> "audio/mp4"
        ProfileMediaUploadKind.File -> "application/octet-stream"
    }

private fun android.content.ContentResolver.displayName(uri: Uri): String? {
    return query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        } else {
            null
        }
    } ?: uri.lastPathSegment
}

private fun android.content.ContentResolver.sizeBytes(uri: Uri): Long? =
    query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0) cursor.getLong(index).takeIf { it >= 0L } else null
        } else {
            null
        }
    }

private fun android.content.ContentResolver.readBytesBounded(uri: Uri, maxBytes: Long): ByteArray {
    val limit = maxBytes.coerceAtLeast(1L)
    val output = ByteArrayOutputStream()
    openInputStream(uri)?.use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            val remaining = (limit - output.size()).toInt()
            if (remaining <= 0) break
            val toWrite = minOf(read, remaining)
            output.write(buffer, 0, toWrite)
            if (toWrite < read) break
        }
    } ?: error("No se pudo leer el archivo seleccionado.")
    return output.toByteArray()
}

private fun readVideoDurationMillis(
    resolver: android.content.ContentResolver,
    uri: Uri,
): Long? {
    val retriever = MediaMetadataRetriever()
    return try {
        val descriptor = resolver.openFileDescriptor(uri, "r") ?: return null
        descriptor.use {
            retriever.setDataSource(it.fileDescriptor)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        }
    } finally {
        retriever.release()
    }
}

private fun readVideoThumbnailBytes(
    resolver: android.content.ContentResolver,
    uri: Uri,
): ByteArray? {
    val retriever = MediaMetadataRetriever()
    return try {
        val descriptor = resolver.openFileDescriptor(uri, "r") ?: return null
        descriptor.use {
            retriever.setDataSource(it.fileDescriptor)
            val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
            try {
                bitmap.compressJpegBounded(ProfileThumbnailMaxBytes)
            } finally {
                bitmap.recycle()
            }
        }
    } finally {
        retriever.release()
    }
}

private fun Bitmap.compressJpegBounded(maxBytes: Long): ByteArray? {
    val scaled = scaleForThumbnail()
    return try {
        var quality = 82
        var bytes = scaled.compressJpeg(quality)
        while (bytes.size.toLong() > maxBytes && quality > 55) {
            quality -= 9
            bytes = scaled.compressJpeg(quality)
        }
        bytes.takeIf { it.size.toLong() <= maxBytes }
    } finally {
        if (scaled !== this) scaled.recycle()
    }
}

private fun Bitmap.scaleForThumbnail(): Bitmap {
    val maxSide = maxOf(width, height)
    if (maxSide <= MaxThumbnailSidePx) return this
    val scale = MaxThumbnailSidePx.toFloat() / maxSide.toFloat()
    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

private fun Bitmap.compressJpeg(quality: Int): ByteArray {
    val output = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, quality, output)
    return output.toByteArray()
}

private const val MaxThumbnailSidePx = 720
