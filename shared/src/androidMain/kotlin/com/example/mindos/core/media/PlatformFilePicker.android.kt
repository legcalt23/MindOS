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
                    val size = resolver.sizeBytes(uri) ?: 0L
                    
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } 
                        ?: error("No se pudo leer el archivo seleccionado.")

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
            if (index >= 0) cursor.getLong(index) else null
        } else null
    }
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
