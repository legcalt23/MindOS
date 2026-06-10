package com.mindos.app.features.profile.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mindos.app.features.profile.domain.audio.AudioWaveform
import com.mindos.app.features.profile.domain.media.decodeImagePixelSize
import com.mindos.app.features.profile.domain.model.LinkValidationResult
import com.mindos.app.features.profile.domain.model.MindOSProfile
import com.mindos.app.features.profile.domain.model.ProfileAudioClip
import com.mindos.app.features.profile.domain.model.ProfileCustomLink
import com.mindos.app.features.profile.domain.model.ProfileImageRef
import com.mindos.app.features.profile.domain.model.ProfileImageTarget
import com.mindos.app.features.profile.domain.model.ProfileLinkValidator
import com.mindos.app.features.profile.domain.repository.MindOSProfileRepository
import com.mindos.app.features.profile.domain.repository.ProfileObserverRepository
import com.mindos.app.features.profile.domain.repository.ProfilePresenceRepository
import com.mindos.app.features.profile.presentation.audio.ProfileAudioCapture
import com.mindos.app.features.profile.presentation.state.AudioUiState
import com.mindos.app.features.profile.presentation.state.CustomLinkEditState
import com.mindos.app.features.profile.presentation.state.EmojiPickerUiState
import com.mindos.app.features.profile.presentation.state.MindOSProfileUiState
import com.mindos.app.features.social.domain.model.ProfileMediaUpload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Owns all state and interaction logic for the MindOS profile screen, keeping
 * the composable thin. Platform side effects (recording, playback, permission,
 * image picking, foreground lifecycle) are performed by the UI layer, which
 * calls back into the typed transitions exposed here — so this holder is fully
 * unit-testable with fakes.
 */
class MindOSProfileStateHolder(
    private val profileRepository: MindOSProfileRepository,
    private val observerRepository: ProfileObserverRepository,
    private val presenceRepository: ProfilePresenceRepository,
    private val scope: CoroutineScope,
) {
    var uiState: MindOSProfileUiState by mutableStateOf(MindOSProfileUiState.Loading)
        private set

    private var viewerId: String = ""
    private var ownerId: String = ""
    private var presenceJob: Job? = null

    // Live capture buffers (main-thread only).
    private val rawAmplitudes = mutableListOf<Float>()
    private var pendingCapture: ProfileAudioCapture? = null

    /**
     * Load (or reload) a profile. [username] is the authoritative identity from
     * auth/search and always overrides whatever the extras store holds.
     */
    suspend fun load(viewerId: String, ownerId: String, username: String, isOwnProfile: Boolean) {
        this.viewerId = viewerId
        this.ownerId = ownerId
        uiState = MindOSProfileUiState.Loading
        runCatching {
            val stored = profileRepository.getProfile(ownerId)
                ?: MindOSProfile(userId = ownerId, username = username)
            val count = observerRepository.observerCount(ownerId)
            val observing = if (isOwnProfile) false else observerRepository.isObserving(viewerId, ownerId)
            val presence = presenceRepository.presence(ownerId).value
            val profile = stored.copy(
                username = username,
                observerCount = count,
                isObservedByViewer = observing,
                presence = presence,
            )
            uiState = MindOSProfileUiState.Loaded(
                profile = profile,
                isOwnProfile = isOwnProfile,
                audio = audioStateFor(profile.audio),
            )
            observePresence(ownerId)
        }.onFailure {
            uiState = MindOSProfileUiState.Error("No se pudo cargar el perfil.")
        }
    }

    // ── Presence ────────────────────────────────────────────────────────────

    /** Push this device's foreground state into the presence model (own dot). */
    fun setForeground(isForeground: Boolean) {
        if (viewerId.isBlank()) return
        presenceRepository.setForegroundPresence(viewerId, isForeground)
    }

    private fun observePresence(ownerId: String) {
        presenceJob?.cancel()
        presenceJob = scope.launch {
            presenceRepository.presence(ownerId).collectLatest { presence ->
                updateProfile { it.copy(presence = presence) }
            }
        }
    }

    // ── Photos (foto_portada / foto_perfil) ───────────────────────────────────

    fun onImagePicked(target: ProfileImageTarget, upload: ProfileMediaUpload) {
        if (!isOwner()) return
        scope.launch {
            runCatching {
                val ref = ProfileImageRef(localBytes = upload.bytes)
                val stored = when (target) {
                    ProfileImageTarget.CoverPhoto -> profileRepository.updateCoverPhoto(ownerId, ref)
                    ProfileImageTarget.ProfilePhoto -> profileRepository.updateProfilePhoto(ownerId, ref)
                }
                applyStored(stored)
            }.onFailure { showProfileUpdateFailure(it) }
        }
    }

    fun onImagePickError(message: String) {
        updateLoaded { it.copy(transientMessage = message) }
    }

    // ── Username emoji prefix ──────────────────────────────────────────────────

    fun openEmojiPicker() {
        if (!isOwner()) return
        updateLoaded { it.copy(emojiPicker = EmojiPickerUiState()) }
    }

    fun dismissEmojiPicker() {
        updateLoaded { it.copy(emojiPicker = null) }
    }

    fun selectUsernameEmoji(emoji: String?) {
        if (!isOwner()) return
        scope.launch {
            runCatching {
                val stored = profileRepository.updateUsernamePrefixEmoji(ownerId, emoji)
                applyStored(stored)
                dismissEmojiPicker()
            }.onFailure { showProfileUpdateFailure(it) }
        }
    }

    // ── Observe / unobserve ────────────────────────────────────────────────────

    fun toggleObserve() {
        val loaded = loaded() ?: return
        if (loaded.isOwnProfile) return
        scope.launch {
            runCatching {
                val shouldObserve = !loaded.profile.isObservedByViewer
                if (shouldObserve) {
                    observerRepository.observeUser(viewerId, ownerId)
                } else {
                    observerRepository.unobserveUser(viewerId, ownerId)
                }
                val observing = observerRepository.isObserving(viewerId, ownerId)
                val count = observerRepository.observerCount(ownerId)
                updateProfile { it.copy(isObservedByViewer = observing, observerCount = count) }
            }.onFailure { showProfileUpdateFailure(it) }
        }
    }

    // ── Audio recording / playback ─────────────────────────────────────────────

    fun onRecordingStarted() {
        rawAmplitudes.clear()
        pendingCapture = null
        updateLoaded { it.copy(audio = AudioUiState.Recording(elapsedMillis = 0L, liveWaveform = emptyList())) }
    }

    fun onAmplitudeSample(amplitude: Float, elapsedMillis: Long) {
        rawAmplitudes.add(amplitude.coerceIn(0f, 1f))
        val live = AudioWaveform.fromAmplitudes(rawAmplitudes)
        updateLoaded {
            it.copy(audio = AudioUiState.Recording(elapsedMillis.coerceAtMost(MaxAudioMillis), live))
        }
    }

    fun onRecordingFinished(capture: ProfileAudioCapture?) {
        if (capture == null || capture.bytes.isEmpty()) {
            restoreAudioFromProfile()
            return
        }
        pendingCapture = capture
        val waveform = AudioWaveform.fromAmplitudes(rawAmplitudes)
        val duration = capture.durationMillis.coerceIn(0L, MaxAudioMillis)
        updateLoaded {
            it.copy(audio = AudioUiState.RecordedPreview(durationMillis = duration, waveform = waveform))
        }
    }

    fun onRecordingError(message: String) {
        updateLoaded { it.copy(audio = AudioUiState.Error(message)) }
    }

    fun discardRecording() {
        pendingCapture = null
        rawAmplitudes.clear()
        restoreAudioFromProfile()
    }

    fun saveRecordedAudio() {
        val capture = pendingCapture ?: return
        val preview = (loaded()?.audio as? AudioUiState.RecordedPreview) ?: return
        scope.launch {
            updateLoaded { it.copy(audio = AudioUiState.Saving) }
            runCatching {
                val clip = ProfileAudioClip(
                    localBytes = capture.bytes,
                    mimeType = capture.mimeType,
                    durationMillis = preview.durationMillis,
                    waveform = preview.waveform,
                )
                val stored = profileRepository.updateProfileAudio(ownerId, clip)
                applyStored(stored)
                pendingCapture = null
                updateLoaded {
                    it.copy(audio = AudioUiState.Saved(preview.durationMillis, preview.waveform))
                }
            }.onFailure { error ->
                updateLoaded { it.copy(audio = AudioUiState.Error(profileUpdateFailureMessage(error))) }
            }
        }
    }

    fun setAudioPlaying(isPlaying: Boolean) {
        updateLoaded { loaded ->
            val next = when (val audio = loaded.audio) {
                is AudioUiState.RecordedPreview -> audio.copy(isPlaying = isPlaying)
                is AudioUiState.Saved -> audio.copy(isPlaying = isPlaying)
                else -> audio
            }
            loaded.copy(audio = next)
        }
    }

    fun setMicPermissionMessage(message: String?) {
        updateLoaded { it.copy(micPermissionMessage = message) }
    }

    /** The clip the audio player should play for the current audio state, if any. */
    fun currentPlayableClip(): ProfileAudioClip? {
        val loaded = loaded() ?: return null
        return when (loaded.audio) {
            is AudioUiState.RecordedPreview -> pendingCapture?.let {
                ProfileAudioClip(localBytes = it.bytes, mimeType = it.mimeType, durationMillis = it.durationMillis)
            }
            is AudioUiState.Saved -> loaded.profile.audio
            else -> null
        }
    }

    // ── Custom links ────────────────────────────────────────────────────────────

    fun openLinkEditor(slotIndex: Int) {
        if (!isOwner()) return
        val slot = loaded()?.profile?.customLinks?.getOrNull(slotIndex) ?: return
        updateLoaded {
            it.copy(
                linkEditor = CustomLinkEditState(
                    slotIndex = slotIndex,
                    url = slot.url.orEmpty(),
                    existingIconReference = slot.iconReference,
                    existingIconBytes = slot.iconLocalBytes,
                ),
            )
        }
    }

    fun updateLinkEditorUrl(url: String) {
        updateLinkEditor { it.copy(url = url, validationError = null) }
    }

    fun onLinkIconPicked(upload: ProfileMediaUpload) {
        val size = decodeImagePixelSize(upload.bytes)
        if (size != null && !size.isSquare) {
            updateLinkEditor { it.copy(validationError = "El icono debe ser cuadrado (1:1).") }
            return
        }
        updateLinkEditor {
            it.copy(
                pickedIconBytes = upload.bytes,
                pickedIconMimeType = upload.mimeType,
                validationError = null,
            )
        }
    }

    fun onLinkIconPickError(message: String) {
        updateLinkEditor { it.copy(validationError = message) }
    }

    fun saveLink() {
        val editor = loaded()?.linkEditor ?: return
        when (val result = ProfileLinkValidator.validate(editor.url)) {
            is LinkValidationResult.Invalid ->
                updateLinkEditor { it.copy(validationError = result.message) }

            is LinkValidationResult.Valid -> scope.launch {
                updateLinkEditor { it.copy(isSaving = true) }
                runCatching {
                    val icon = ProfileImageRef(
                        reference = if (editor.pickedIconBytes == null) editor.existingIconReference else null,
                        localBytes = editor.pickedIconBytes ?: editor.existingIconBytes,
                    )
                    val stored = profileRepository.updateCustomLink(
                        userId = ownerId,
                        slotIndex = editor.slotIndex,
                        url = result.normalizedUrl,
                        icon = icon.takeUnless { it.isEmpty },
                    )
                    applyStored(stored)
                    dismissLinkEditor()
                }.onFailure { error ->
                    updateLinkEditor {
                        it.copy(isSaving = false, validationError = profileUpdateFailureMessage(error))
                    }
                }
            }
        }
    }

    fun clearLink(slotIndex: Int) {
        if (!isOwner()) return
        scope.launch {
            runCatching {
                val stored = profileRepository.updateCustomLink(ownerId, slotIndex, url = null, icon = null)
                applyStored(stored)
                dismissLinkEditor()
            }.onFailure { error ->
                updateLinkEditor {
                    it.copy(isSaving = false, validationError = profileUpdateFailureMessage(error))
                }
            }
        }
    }

    fun dismissLinkEditor() {
        updateLoaded { it.copy(linkEditor = null) }
    }

    fun dismissTransientMessage() {
        updateLoaded { it.copy(transientMessage = null) }
    }

    // ── Internals ────────────────────────────────────────────────────────────────

    private fun isOwner(): Boolean = loaded()?.isOwnProfile == true

    private fun loaded(): MindOSProfileUiState.Loaded? = uiState as? MindOSProfileUiState.Loaded

    private fun updateLoaded(transform: (MindOSProfileUiState.Loaded) -> MindOSProfileUiState.Loaded) {
        val current = loaded() ?: return
        uiState = transform(current)
    }

    private fun updateProfile(transform: (MindOSProfile) -> MindOSProfile) {
        updateLoaded { it.copy(profile = transform(it.profile).bumped()) }
    }

    private fun updateLinkEditor(transform: (CustomLinkEditState) -> CustomLinkEditState) {
        updateLoaded { loaded ->
            val editor = loaded.linkEditor ?: return@updateLoaded loaded
            loaded.copy(linkEditor = transform(editor))
        }
    }

    /** Overlay repository-owned extras onto the live identity/presence overlay. */
    private fun applyStored(stored: MindOSProfile) {
        updateLoaded { loaded ->
            loaded.copy(
                profile = stored.copy(
                    username = loaded.profile.username,
                    observerCount = loaded.profile.observerCount,
                    isObservedByViewer = loaded.profile.isObservedByViewer,
                    presence = loaded.profile.presence,
                ),
            )
        }
    }

    private fun restoreAudioFromProfile() {
        updateLoaded { it.copy(audio = audioStateFor(it.profile.audio)) }
    }

    private fun showProfileUpdateFailure(error: Throwable) {
        updateLoaded { it.copy(transientMessage = profileUpdateFailureMessage(error)) }
    }

    private fun profileUpdateFailureMessage(error: Throwable): String =
        error.message?.takeIf { it.isNotBlank() } ?: "No se pudo actualizar el perfil."

    private fun audioStateFor(clip: ProfileAudioClip?): AudioUiState =
        if (clip != null && AudioWaveform.hasSignal(clip.waveform)) {
            AudioUiState.Saved(durationMillis = clip.durationMillis, waveform = clip.waveform)
        } else {
            AudioUiState.Empty
        }

    companion object {
        /** Hard cap on profile audio length. */
        const val MaxAudioMillis = 10_000L
    }
}
