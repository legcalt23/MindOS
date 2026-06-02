package com.example.mindos.core.media

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
actual fun rememberPlatformFilePicker(
    onPicked: (PlatformFile) -> Unit,
    onError: (String) -> Unit
): PlatformFilePicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnPicked by rememberUpdatedState(onPicked)
    val currentOnError by rememberUpdatedState(onError)

    fun handleUri(uri: Uri?, kind: FileKind) {
        if (uri == null) return
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val resolver = context.contentResolver
                    val mimeType = resolver.getType(uri) ?: kind.defaultMimeType()
                    val name = resolver.displayName(uri) ?: "file"
                    val maxBytes = kind.maxBytes()
                    val reportedSize = resolver.sizeBytes(uri)
                    if (reportedSize != null && reportedSize > maxBytes) {
                        error("El archivo seleccionado supera el limite permitido.")
                    }
                    val bytes = resolver.readBytesBounded(uri, maxBytes + 1)
                    if (bytes.size.toLong() > maxBytes) {
                        error("El archivo seleccionado supera el limite permitido.")
                    }
                    val size = reportedSize ?: bytes.size.toLong()

                    var durationMs: Long? = null
                    var thumbnailBytes: ByteArray? = null

                    if (kind == FileKind.Video || (kind == FileKind.File && mimeType.startsWith("video/"))) {
                        durationMs = readVideoDurationMillis(context.contentResolver, uri)
                        thumbnailBytes = readVideoThumbnailBytes(context.contentResolver, uri)
                    }

                    PlatformFile(
                        bytes = bytes,
                        name = name,
                        mimeType = mimeType,
                        size = size,
                        durationMs = durationMs,
                        thumbnailBytes = thumbnailBytes
                    )
                }
            }.fold(
                onSuccess = { currentOnPicked(it) },
                onFailure = { currentOnError(it.message ?: "Error al procesar el archivo") }
            )
        }
    }

    var currentKind by remember { mutableStateOf(FileKind.File) }
    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        handleUri(uri, currentKind)
    }

    return remember(pickerLauncher) {
        object : PlatformFilePicker {
            override fun pick(kind: FileKind) {
                currentKind = kind
                val mimeType = when (kind) {
                    FileKind.Photo -> "image/*"
                    FileKind.Video -> "video/*"
                    FileKind.Audio -> "audio/*"
                    FileKind.File -> "*/*"
                }
                pickerLauncher.launch(mimeType)
            }
        }
    }
}

private fun FileKind.defaultMimeType(): String = when (this) {
    FileKind.Photo -> "image/jpeg"
    FileKind.Video -> "video/mp4"
    FileKind.Audio -> "audio/mpeg"
    FileKind.File -> "application/octet-stream"
}

private fun android.content.ContentResolver.displayName(uri: Uri): String? {
    return query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        } else null
    } ?: uri.lastPathSegment
}

private fun android.content.ContentResolver.sizeBytes(uri: Uri): Long? {
    return query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0) cursor.getLong(index).takeIf { it >= 0L } else null
        } else null
    }
}

private fun android.content.ContentResolver.readBytesBounded(uri: Uri, maxBytes: Long): ByteArray {
    val limit = maxBytes.coerceAtLeast(1L)
    val outputStream = ByteArrayOutputStream()
    openInputStream(uri)?.use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (outputStream.size().toLong() < limit) {
            val remaining = (limit - outputStream.size()).toInt()
            val read = input.read(buffer, 0, minOf(buffer.size, remaining))
            if (read == -1) break
            outputStream.write(buffer, 0, read)
        }
    } ?: error("No se pudo leer el archivo seleccionado.")
    return outputStream.toByteArray()
}

private fun readVideoDurationMillis(resolver: android.content.ContentResolver, uri: Uri): Long? {
    val retriever = MediaMetadataRetriever()
    return try {
        resolver.openFileDescriptor(uri, "r")?.use {
            retriever.setDataSource(it.fileDescriptor)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        }
    } catch (e: Exception) {
        null
    } finally {
        retriever.release()
    }
}

private fun readVideoThumbnailBytes(resolver: android.content.ContentResolver, uri: Uri): ByteArray? {
    val retriever = MediaMetadataRetriever()
    return try {
        resolver.openFileDescriptor(uri, "r")?.use {
            retriever.setDataSource(it.fileDescriptor)
            val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.toByteArray()
        }
    } catch (e: Exception) {
        null
    } finally {
        retriever.release()
    }
}

private fun FileKind.maxBytes(): Long = when (this) {
    FileKind.Photo -> MAX_PHOTO_BYTES
    FileKind.Video -> MAX_VIDEO_BYTES
    FileKind.Audio -> MAX_AUDIO_BYTES
    FileKind.File -> MAX_GENERIC_FILE_BYTES
}

private const val MAX_PHOTO_BYTES = 10L * 1024L * 1024L
private const val MAX_VIDEO_BYTES = 75L * 1024L * 1024L
private const val MAX_AUDIO_BYTES = 25L * 1024L * 1024L
private const val MAX_GENERIC_FILE_BYTES = 25L * 1024L * 1024L
