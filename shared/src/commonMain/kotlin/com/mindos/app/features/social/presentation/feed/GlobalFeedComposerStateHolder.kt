package com.mindos.app.features.social.presentation.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mindos.app.core.media.PlatformFile
import com.mindos.app.core.session.AuthenticatedUser
import com.mindos.app.features.dm.presentation.media.DMAudioRecording
import com.mindos.app.features.social.domain.feed.CreateGlobalFeedPostRequest
import com.mindos.app.features.social.domain.feed.GlobalFeedAttachmentType
import com.mindos.app.features.social.domain.feed.GlobalFeedDraftAttachment
import com.mindos.app.features.social.domain.feed.GlobalFeedMaxAudioClips
import com.mindos.app.features.social.domain.feed.GlobalFeedMaxDescriptionLength
import com.mindos.app.features.social.domain.feed.GlobalFeedMaxTextLength
import com.mindos.app.features.social.domain.feed.GlobalFeedPost
import com.mindos.app.features.social.domain.feed.GlobalFeedPostType
import com.mindos.app.features.social.domain.feed.GlobalFeedRepository
import com.mindos.app.features.social.domain.feed.GlobalFeedTextAlign
import com.mindos.app.features.social.domain.feed.GlobalFeedTextBackgroundStyle
import com.mindos.app.features.social.domain.feed.GlobalFeedTextStyle
import com.mindos.app.features.social.domain.feed.globalFeedCodePointLength
import com.mindos.app.features.social.domain.feed.limitGlobalFeedDescription
import com.mindos.app.features.social.domain.feed.normalizedGlobalFeedDescription
import com.mindos.app.features.social.domain.feed.normalizedGlobalFeedText
import com.mindos.app.features.social.shared.util.DomainResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.mindos.app.core.time.EpochClock
import com.mindos.app.core.time.SystemEpochClock
import kotlin.random.Random

enum class ComposerMode {
    PHOTO,
    VIDEO,
    TEXT,
    AUDIO,
}

data class ComposerMediaSelection(
    val attachment: GlobalFeedDraftAttachment,
    val previewBytes: ByteArray?,
    val durationMs: Long? = null,
)

data class ComposerAudioClip(
    val id: String,
    val attachment: GlobalFeedDraftAttachment,
    val localPath: String?,
    val durationMs: Long,
    val waveform: List<Float>,
)

data class FeedAudioPlaybackUiState(
    val isPlaying: Boolean = false,
    val progress: Float = 0f,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

data class GlobalFeedComposerUiState(
    val selectedMode: ComposerMode = ComposerMode.PHOTO,
    val selectedPhoto: ComposerMediaSelection? = null,
    val selectedVideo: ComposerMediaSelection? = null,
    val textContent: String = "",
    val description: String = "",
    val textStyleConfig: GlobalFeedTextStyle = defaultNeuTextStyle(),
    val audioRecordings: List<ComposerAudioClip> = emptyList(),
    val isRecording: Boolean = false,
    val recordingDurationMs: Long = 0L,
    val playbackStateByAudioId: Map<String, FeedAudioPlaybackUiState> = emptyMap(),
    val isPublishing: Boolean = false,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null,
) {
    val canPublish: Boolean
        get() = when (selectedMode) {
            ComposerMode.PHOTO -> selectedPhoto != null && !isPublishing
            ComposerMode.VIDEO -> selectedVideo != null && !isPublishing
            ComposerMode.TEXT -> textContent.trim().isNotEmpty() && !isPublishing
            ComposerMode.AUDIO -> audioRecordings.isNotEmpty() && !isPublishing
        }
}

@Stable
class GlobalFeedComposerStateHolder(
    private val user: AuthenticatedUser,
    private val repository: GlobalFeedRepository,
    private val scope: CoroutineScope,
    // Injected so tests are deterministic and never touch kotlinx-datetime's
    // Clock.System (removed from the 0.7.1 runtime classpath). Production uses
    // the default SystemEpochClock.
    private val clock: EpochClock = SystemEpochClock,
) {
    var uiState by mutableStateOf(GlobalFeedComposerUiState())
        private set

    fun onModeSwipeNext() {
        val next = when (uiState.selectedMode) {
            ComposerMode.PHOTO -> ComposerMode.VIDEO
            ComposerMode.VIDEO -> ComposerMode.TEXT
            ComposerMode.TEXT -> ComposerMode.AUDIO
            ComposerMode.AUDIO -> null
        }
        if (next == null) {
            emitFeedback("Límite de modos alcanzado.")
            return
        }
        uiState = uiState.copy(selectedMode = next, feedbackMessage = null, errorMessage = null)
    }

    fun onModeSwipePrevious() {
        val previous = when (uiState.selectedMode) {
            ComposerMode.PHOTO -> null
            ComposerMode.VIDEO -> ComposerMode.PHOTO
            ComposerMode.TEXT -> ComposerMode.VIDEO
            ComposerMode.AUDIO -> ComposerMode.TEXT
        }
        if (previous == null) {
            emitFeedback("PHOTO es el modo inicial.")
            return
        }
        uiState = uiState.copy(selectedMode = previous, feedbackMessage = null, errorMessage = null)
    }

    fun onSelectPhoto(file: PlatformFile) {
        val attachment = GlobalFeedDraftAttachment(
            id = draftId("photo"),
            type = GlobalFeedAttachmentType.Image,
            fileName = file.name,
            mimeType = file.mimeType,
            sizeBytes = file.size,
            bytes = file.bytes,
        )
        uiState = uiState.copy(
            selectedMode = ComposerMode.PHOTO,
            selectedPhoto = ComposerMediaSelection(attachment = attachment, previewBytes = file.bytes),
            feedbackMessage = "Imagen preparada.",
            errorMessage = null,
        )
    }

    fun onSelectVideo(file: PlatformFile) {
        val attachment = GlobalFeedDraftAttachment(
            id = draftId("video"),
            type = GlobalFeedAttachmentType.Video,
            fileName = file.name,
            mimeType = file.mimeType,
            sizeBytes = file.size,
            bytes = file.bytes,
            durationMs = file.durationMs,
            thumbnailBytes = file.thumbnailBytes,
        )
        uiState = uiState.copy(
            selectedMode = ComposerMode.VIDEO,
            selectedVideo = ComposerMediaSelection(
                attachment = attachment,
                previewBytes = file.thumbnailBytes,
                durationMs = file.durationMs,
            ),
            feedbackMessage = "Vídeo preparado.",
            errorMessage = null,
        )
    }

    fun onTextEditorClick() {
        uiState = uiState.copy(selectedMode = ComposerMode.TEXT, errorMessage = null, feedbackMessage = null)
    }

    fun onTextChanged(text: String) {
        uiState = uiState.copy(textContent = text.take(GlobalFeedMaxTextLength), errorMessage = null, feedbackMessage = null)
    }

    fun onDescriptionChanged(description: String) {
        uiState = uiState.copy(
            description = description.limitGlobalFeedDescription(),
            errorMessage = null,
            feedbackMessage = null,
        )
    }

    fun onTextStyleChanged(transform: (GlobalFeedTextStyle) -> GlobalFeedTextStyle) {
        uiState = uiState.copy(textStyleConfig = transform(uiState.textStyleConfig), errorMessage = null, feedbackMessage = null)
    }

    fun onApplyTextPreset(preset: GlobalFeedTextBackgroundStyle) {
        uiState = uiState.copy(textStyleConfig = preset.toPreset(uiState.textStyleConfig), errorMessage = null, feedbackMessage = "Preset aplicado.")
    }

    fun onResetTextStyle() {
        uiState = uiState.copy(textStyleConfig = defaultNeuTextStyle(), feedbackMessage = "Estilo reiniciado.", errorMessage = null)
    }

    fun onStartAudioRecording() {
        if (uiState.audioRecordings.size >= GlobalFeedMaxAudioClips) {
            uiState = uiState.copy(errorMessage = "Máximo $GlobalFeedMaxAudioClips audios por publicación.", feedbackMessage = null)
            return
        }
        uiState = uiState.copy(selectedMode = ComposerMode.AUDIO, isRecording = true, recordingDurationMs = 0L, errorMessage = null, feedbackMessage = "Grabando audio.")
    }

    fun onStopAudioRecording() {
        if (!uiState.isRecording) return
        uiState = uiState.copy(isRecording = false, feedbackMessage = "Audio capturado.", errorMessage = null)
    }

    fun onAudioLongPressStart() = onStartAudioRecording()

    fun onAudioLongPressEnd() = onStopAudioRecording()

    fun onRecordingDurationChanged(durationMs: Long) {
        if (!uiState.isRecording) return
        uiState = uiState.copy(recordingDurationMs = durationMs.coerceAtLeast(0L))
    }

    fun onAudioRecorded(recording: DMAudioRecording) {
        val durationMs = recording.durationMs ?: 0L
        if (durationMs < 1_000L) {
            uiState = uiState.copy(isRecording = false, recordingDurationMs = 0L, errorMessage = "Audio demasiado corto.", feedbackMessage = null)
            return
        }
        if (uiState.audioRecordings.size >= GlobalFeedMaxAudioClips) {
            uiState = uiState.copy(isRecording = false, recordingDurationMs = 0L, errorMessage = "Máximo $GlobalFeedMaxAudioClips audios por publicación.", feedbackMessage = null)
            return
        }
        val clipId = draftId("audio")
        val attachment = GlobalFeedDraftAttachment(
            id = clipId,
            type = GlobalFeedAttachmentType.Audio,
            fileName = recording.fileName,
            mimeType = recording.mimeType,
            sizeBytes = recording.sizeBytes,
            bytes = recording.bytes,
            durationMs = durationMs,
        )
        val clip = ComposerAudioClip(
            id = clipId,
            attachment = attachment,
            localPath = recording.localPath,
            durationMs = durationMs,
            waveform = emptyList(),
        )
        uiState = uiState.copy(
            selectedMode = ComposerMode.AUDIO,
            audioRecordings = uiState.audioRecordings + clip,
            isRecording = false,
            recordingDurationMs = 0L,
            feedbackMessage = "Audio guardado.",
            errorMessage = null,
        )
    }

    fun onPlayPauseAudio(audioId: String, playback: FeedAudioPlaybackUiState) {
        uiState = uiState.copy(
            playbackStateByAudioId = uiState.playbackStateByAudioId + (audioId to playback),
            errorMessage = null,
        )
    }

    fun onDeleteAudio(audioId: String) {
        uiState = uiState.copy(
            audioRecordings = uiState.audioRecordings.filterNot { it.id == audioId },
            playbackStateByAudioId = uiState.playbackStateByAudioId - audioId,
            errorMessage = null,
            feedbackMessage = "Audio eliminado.",
        )
    }

    fun onPickerCancelled(message: String = "Selección cancelada.") {
        emitFeedback(message)
    }

    fun onPickerError(message: String) {
        uiState = uiState.copy(errorMessage = message, feedbackMessage = null)
    }

    private fun validatePublish(): String? {
        if (uiState.description.globalFeedCodePointLength() > GlobalFeedMaxDescriptionLength) {
            return "La descripción supera el máximo de $GlobalFeedMaxDescriptionLength caracteres."
        }
        return validate()
    }

    fun onResetComposer() {
        uiState = GlobalFeedComposerUiState()
    }

    fun onPublishClick(onPublished: (GlobalFeedPost) -> Unit = {}) {
        if (uiState.isPublishing) return
        val validationError = validatePublish()
        if (validationError != null) {
            uiState = uiState.copy(errorMessage = validationError, feedbackMessage = null)
            return
        }

        val request = createRequest()
        scope.launch {
            uiState = uiState.copy(isPublishing = true, errorMessage = null, feedbackMessage = "Publicando.")
            when (val result = repository.createPost(request)) {
                is DomainResult.Success -> {
                    onPublished(result.value)
                    onResetComposer()
                }
                is DomainResult.Failure -> {
                    uiState = uiState.copy(isPublishing = false, errorMessage = result.error.message, feedbackMessage = null)
                }
            }
        }
    }

    private fun createRequest(): CreateGlobalFeedPostRequest =
        when (uiState.selectedMode) {
            ComposerMode.PHOTO -> CreateGlobalFeedPostRequest(
                userId = user.uid,
                authorUsername = user.feedUsername(),
                authorDisplayName = user.feedDisplayName(),
                postType = GlobalFeedPostType.PHOTO,
                text = null,
                description = uiState.description.normalizedGlobalFeedDescription(),
                textStyle = null,
                attachments = listOfNotNull(uiState.selectedPhoto?.attachment),
            )
            ComposerMode.VIDEO -> CreateGlobalFeedPostRequest(
                userId = user.uid,
                authorUsername = user.feedUsername(),
                authorDisplayName = user.feedDisplayName(),
                postType = GlobalFeedPostType.VIDEO,
                text = null,
                description = uiState.description.normalizedGlobalFeedDescription(),
                textStyle = null,
                attachments = listOfNotNull(uiState.selectedVideo?.attachment),
            )
            ComposerMode.TEXT -> CreateGlobalFeedPostRequest(
                userId = user.uid,
                authorUsername = user.feedUsername(),
                authorDisplayName = user.feedDisplayName(),
                postType = GlobalFeedPostType.TEXT,
                text = uiState.textContent.normalizedGlobalFeedText(),
                description = uiState.description.normalizedGlobalFeedDescription(),
                textStyle = uiState.textStyleConfig,
                attachments = emptyList(),
            )
            ComposerMode.AUDIO -> CreateGlobalFeedPostRequest(
                userId = user.uid,
                authorUsername = user.feedUsername(),
                authorDisplayName = user.feedDisplayName(),
                postType = GlobalFeedPostType.AUDIO,
                text = null,
                description = uiState.description.normalizedGlobalFeedDescription(),
                textStyle = null,
                attachments = uiState.audioRecordings.map { it.attachment },
            )
        }

    private fun validate(): String? =
        when (uiState.selectedMode) {
            ComposerMode.PHOTO -> if (uiState.selectedPhoto == null) "Selecciona una imagen." else null
            ComposerMode.VIDEO -> if (uiState.selectedVideo == null) "Selecciona un vídeo." else null
            ComposerMode.TEXT -> if (uiState.textContent.trim().isBlank()) "Escribe un texto antes de publicar." else null
            ComposerMode.AUDIO -> if (uiState.audioRecordings.isEmpty()) "Graba al menos un audio." else null
        }

    private fun emitFeedback(message: String) {
        uiState = uiState.copy(feedbackMessage = message, errorMessage = null)
    }

    private fun draftId(prefix: String): String =
        "${prefix}_${clock.nowMillis()}_${Random.nextLong().toULong().toString(36)}"
}

@Composable
fun rememberGlobalFeedComposerStateHolder(
    user: AuthenticatedUser,
    repository: GlobalFeedRepository,
    scope: CoroutineScope,
): GlobalFeedComposerStateHolder =
    remember(user.uid, repository, scope) {
        GlobalFeedComposerStateHolder(user = user, repository = repository, scope = scope)
    }

private fun AuthenticatedUser.feedDisplayName(): String =
    displayName
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: email?.substringBefore("@")?.trim()?.takeIf { it.isNotBlank() }
        ?: uid.take(18)

private fun AuthenticatedUser.feedUsername(): String {
    val candidate = email?.substringBefore("@") ?: displayName ?: uid.take(18)
    val normalized = candidate
        .lowercase()
        .map { char -> if (char.isLetterOrDigit() || char == '.' || char == '_') char else '_' }
        .joinToString("")
        .trim('.')
        .replace(Regex("\\.{2,}"), ".")
        .take(30)
    if (normalized.length >= 3 && !normalized.startsWith(".") && !normalized.endsWith(".") && !normalized.contains("..")) {
        return normalized
    }
    val uidAlias = uid
        .lowercase()
        .filter { it.isLetterOrDigit() }
        .ifBlank { "neu" }
        .take(18)
    return "user_$uidAlias".take(30)
}

fun defaultNeuTextStyle(): GlobalFeedTextStyle = GlobalFeedTextStyle()

private fun GlobalFeedTextBackgroundStyle.toPreset(current: GlobalFeedTextStyle): GlobalFeedTextStyle =
    when (this) {
        GlobalFeedTextBackgroundStyle.Solid -> current.copy(
            backgroundStyle = this,
            backgroundPrimaryArgb = 0xFF0A0F12,
            backgroundSecondaryArgb = 0xFF0A0F12,
            textColorArgb = 0xFFF8FFFD,
            glowIntensity = 0.22f,
        )
        GlobalFeedTextBackgroundStyle.Gradient -> current.copy(
            backgroundStyle = this,
            backgroundPrimaryArgb = 0xFF071C19,
            backgroundSecondaryArgb = 0xFF153F37,
            textColorArgb = 0xFFF4FFF9,
            glowIntensity = 0.40f,
        )
        GlobalFeedTextBackgroundStyle.DarkPremium -> current.copy(
            backgroundStyle = this,
            backgroundPrimaryArgb = 0xFF020504,
            backgroundSecondaryArgb = 0xFF07100E,
            textColorArgb = 0xFFF4FFF9,
            glowIntensity = 0.28f,
        )
        GlobalFeedTextBackgroundStyle.NeonGlass -> current.copy(
            backgroundStyle = this,
            backgroundPrimaryArgb = 0xCC081613,
            backgroundSecondaryArgb = 0xCC153A35,
            textColorArgb = 0xFFF8FFFC,
            glowIntensity = 0.68f,
        )
        GlobalFeedTextBackgroundStyle.MinimalLight -> current.copy(
            backgroundStyle = this,
            backgroundPrimaryArgb = 0xFFF7FBFA,
            backgroundSecondaryArgb = 0xFFE9F4F1,
            textColorArgb = 0xFF08110F,
            glowIntensity = 0.08f,
        )
        GlobalFeedTextBackgroundStyle.MinimalDark -> current.copy(
            backgroundStyle = this,
            backgroundPrimaryArgb = 0xFF0D1214,
            backgroundSecondaryArgb = 0xFF11191C,
            textColorArgb = 0xFFEFF9F5,
            glowIntensity = 0.16f,
        )
        GlobalFeedTextBackgroundStyle.NeuRebel -> current.copy(
            backgroundStyle = this,
            backgroundPrimaryArgb = 0xFF071311,
            backgroundSecondaryArgb = 0xFF102A24,
            textColorArgb = 0xFFF4FFF9,
            glowIntensity = 0.45f,
            borderColorArgb = 0x5528F0C6,
        )
        GlobalFeedTextBackgroundStyle.Executive -> current.copy(
            backgroundStyle = this,
            backgroundPrimaryArgb = 0xFF111317,
            backgroundSecondaryArgb = 0xFF1B2027,
            textColorArgb = 0xFFF8F9FB,
            glowIntensity = 0.12f,
            borderColorArgb = 0x33FFFFFF,
        )
    }
