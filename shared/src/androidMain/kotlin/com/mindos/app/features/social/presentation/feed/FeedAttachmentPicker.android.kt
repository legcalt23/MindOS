package com.mindos.app.features.social.presentation.feed

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.mindos.app.features.social.domain.feed.GlobalFeedAttachmentType
import com.mindos.app.features.social.domain.feed.GlobalFeedDraftAttachment
import com.mindos.app.features.social.domain.feed.GlobalFeedMaxAudioBytes
import com.mindos.app.features.social.domain.feed.GlobalFeedMaxImageBytes
import com.mindos.app.features.social.domain.feed.GlobalFeedMaxPdfBytes
import com.mindos.app.features.social.domain.feed.GlobalFeedMaxTextFileBytes
import com.mindos.app.features.social.domain.feed.GlobalFeedMaxVideoBytes
import com.mindos.app.features.social.domain.feed.normalizedGlobalFeedMimeType
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

@Composable
actual fun rememberFeedAttachmentPicker(
    onPicked: (List<GlobalFeedDraftAttachment>) -> Unit,
    onError: (String) -> Unit,
): FeedAttachmentPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnPicked by rememberUpdatedState(onPicked)
    val currentOnError by rememberUpdatedState(onError)

    fun handleUris(uris: List<Uri>, type: GlobalFeedAttachmentType) {
        if (uris.isEmpty()) return
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    uris.mapIndexed { index, uri ->
                        val resolver = context.contentResolver
                        val fileName = resolver.displayName(uri) ?: type.defaultFileName(index)
                        val mimeType = resolveAttachmentMimeType(
                            rawMimeType = resolver.getType(uri),
                            fileName = fileName,
                            type = type,
                        )
                        val maxBytes = maxBytesFor(type, mimeType)
                        val reportedSize = resolver.sizeBytes(uri)
                        if (reportedSize != null && reportedSize > maxBytes) {
                            error("Un adjunto supera el limite permitido.")
                        }
                        val bytes = resolver.readBytesBounded(uri, maxBytes + 1)
                        if (bytes.size.toLong() > maxBytes) {
                            error("Un adjunto supera el limite permitido.")
                        }
                        GlobalFeedDraftAttachment(
                            id = attachmentId(type, index),
                            type = type,
                            fileName = fileName,
                            mimeType = mimeType,
                            sizeBytes = reportedSize ?: bytes.size.toLong(),
                            bytes = bytes,
                        )
                    }
                }
            }.fold(
                onSuccess = currentOnPicked,
                onFailure = { error -> currentOnError(error.message ?: "No se pudo preparar el contenido.") },
            )
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        handleUris(uris, GlobalFeedAttachmentType.Image)
    }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        handleUris(uris, GlobalFeedAttachmentType.Video)
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        handleUris(uris, GlobalFeedAttachmentType.File)
    }

    return remember(photoLauncher, videoLauncher, fileLauncher) {
        object : FeedAttachmentPicker {
            override fun pick(kind: FeedAttachmentPickKind) {
                when (kind) {
                    FeedAttachmentPickKind.Photos -> photoLauncher.launch("image/*")
                    FeedAttachmentPickKind.Videos -> videoLauncher.launch("video/*")
                    FeedAttachmentPickKind.Files -> fileLauncher.launch(
                        arrayOf(
                            "application/pdf",
                            "text/plain",
                            "application/json",
                            "audio/wav",
                            "audio/x-wav",
                            "audio/wave",
                            "audio/vnd.wave",
                        )
                    )
                }
            }
        }
    }
}

private fun android.content.ContentResolver.displayName(uri: Uri): String? =
    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) cursor.getString(index) else null
        } else {
            null
        }
    } ?: uri.lastPathSegment

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

private fun GlobalFeedAttachmentType.defaultMimeType(): String =
    when (this) {
        GlobalFeedAttachmentType.Image -> "image/jpeg"
        GlobalFeedAttachmentType.Video -> "video/mp4"
        GlobalFeedAttachmentType.Audio -> "audio/mp4"
        GlobalFeedAttachmentType.File -> "application/octet-stream"
    }

private fun GlobalFeedAttachmentType.defaultFileName(index: Int): String =
    when (this) {
        GlobalFeedAttachmentType.Image -> "foto_${index + 1}.jpg"
        GlobalFeedAttachmentType.Video -> "video_${index + 1}.mp4"
        GlobalFeedAttachmentType.Audio -> "audio_${index + 1}.m4a"
        GlobalFeedAttachmentType.File -> "archivo_${index + 1}"
    }

private fun resolveAttachmentMimeType(
    rawMimeType: String?,
    fileName: String,
    type: GlobalFeedAttachmentType,
): String {
    val normalizedMime = rawMimeType?.normalizedGlobalFeedMimeType().orEmpty()
    return when {
        normalizedMime.isNotBlank() && normalizedMime != "application/octet-stream" -> normalizedMime
        else -> mimeTypeFromFileName(fileName) ?: type.defaultMimeType().normalizedGlobalFeedMimeType()
    }
}

private fun mimeTypeFromFileName(fileName: String): String? =
    when (fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "json" -> "application/json"
        "wav" -> "audio/wav"
        else -> null
    }

private fun attachmentId(type: GlobalFeedAttachmentType, index: Int): String =
    "att_${type.name.lowercase()}_${System.currentTimeMillis()}_${index}_${Random.nextLong().toULong().toString(36)}"

private fun maxBytesFor(type: GlobalFeedAttachmentType, mimeType: String): Long =
    when (type) {
        GlobalFeedAttachmentType.Image -> GlobalFeedMaxImageBytes
        GlobalFeedAttachmentType.Video -> GlobalFeedMaxVideoBytes
        GlobalFeedAttachmentType.Audio -> GlobalFeedMaxAudioBytes
        GlobalFeedAttachmentType.File -> when (mimeType.normalizedGlobalFeedMimeType()) {
            "application/pdf" -> GlobalFeedMaxPdfBytes
            "text/plain", "application/json" -> GlobalFeedMaxTextFileBytes
            "audio/wav" -> GlobalFeedMaxAudioBytes
            else -> GlobalFeedMaxTextFileBytes
        }
    }
