package com.mindos.app.features.profile.presentation.state

import androidx.compose.runtime.Immutable
import com.mindos.app.features.profile.domain.model.MindOSProfile

/** Top-level state for the MindOS profile screen. */
sealed interface MindOSProfileUiState {
    data object Loading : MindOSProfileUiState

    @Immutable
    data class Loaded(
        val profile: MindOSProfile,
        val isOwnProfile: Boolean,
        val audio: AudioUiState,
        /** Non-null while the emoji-prefix picker is open (owner only). */
        val emojiPicker: EmojiPickerUiState? = null,
        /** Non-null while a custom-link edit form is open (owner only). */
        val linkEditor: CustomLinkEditState? = null,
        /** Permission guidance shown when the mic is denied. */
        val micPermissionMessage: String? = null,
        /** Transient error/info banner. */
        val transientMessage: String? = null,
    ) : MindOSProfileUiState

    data class Error(val message: String) : MindOSProfileUiState
}

/**
 * Explicit audio state machine for the profile audio card. The waveform values
 * always come from real captured amplitudes (see AudioWaveform), never random.
 */
sealed interface AudioUiState {
    /** No audio saved — render a flat line + microphone button. */
    data object Empty : AudioUiState

    @Immutable
    data class Recording(
        val elapsedMillis: Long,
        val liveWaveform: List<Float>,
    ) : AudioUiState

    @Immutable
    data class RecordedPreview(
        val durationMillis: Long,
        val waveform: List<Float>,
        val isPlaying: Boolean = false,
    ) : AudioUiState

    data object Saving : AudioUiState

    @Immutable
    data class Saved(
        val durationMillis: Long,
        val waveform: List<Float>,
        val isPlaying: Boolean = false,
    ) : AudioUiState

    data class Error(val message: String) : AudioUiState
}

/** Curated emoji choices for the username-prefix picker. */
@Immutable
data class EmojiPickerUiState(
    val options: List<String> = DefaultEmojiOptions,
) {
    companion object {
        val DefaultEmojiOptions: List<String> = listOf(
            "🧠", "✨", "🚀", "🎨", "🔬", "🎧", "🌙", "🔥",
            "💎", "🌊", "🪐", "⚡", "🦋", "🌱", "👁️", "🧬",
            "🎭", "📡", "🛰️", "🧩", "🪐", "💫", "🌀", "🫧",
        )
    }
}

/** Edit form state for a single custom-link slot. */
@Immutable
data class CustomLinkEditState(
    val slotIndex: Int,
    val url: String,
    /** Bytes of a freshly-picked square icon (1:1), if any. */
    val pickedIconBytes: ByteArray? = null,
    val pickedIconMimeType: String? = null,
    /** Reference/bytes of an already-saved icon, preserved when not re-picked. */
    val existingIconReference: String? = null,
    val existingIconBytes: ByteArray? = null,
    val validationError: String? = null,
    val isSaving: Boolean = false,
) {
    val hasIcon: Boolean
        get() = pickedIconBytes != null || existingIconBytes != null || !existingIconReference.isNullOrBlank()
}

/** Convenience accessor: the [MindOSProfile] currently displayed, if loaded. */
val MindOSProfileUiState.profileOrNull: MindOSProfile?
    get() = (this as? MindOSProfileUiState.Loaded)?.profile
