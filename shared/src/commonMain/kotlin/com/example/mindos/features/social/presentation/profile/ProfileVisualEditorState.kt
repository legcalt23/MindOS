package com.example.mindos.features.social.presentation.profile

import com.example.mindos.features.social.domain.model.ProfileColorTarget
import com.example.mindos.features.social.domain.model.ProfileMediaUpload
import com.example.mindos.features.social.domain.model.ProfileModuleId
import com.example.mindos.features.social.domain.model.ProfileVisualConfig

enum class ChromaCoreInteractionMode {
    Idle,
    Dragging,
    Precision,
    Settling
}

data class ChromaCoreSessionState(
    val isVisible: Boolean = false,
    val persistedColor: Long = 0xFF13ECA4,
    val sessionInitialColor: Long = 0xFF13ECA4,
    val previewColor: Long = 0xFF13ECA4,
    val interactionMode: ChromaCoreInteractionMode = ChromaCoreInteractionMode.Idle
) {
    val isDirty: Boolean
        get() = previewColor != sessionInitialColor

    val canConfirm: Boolean
        get() = isDirty
}

data class ProfileEditorUiState(
    val isEditing: Boolean = false,
    val draft: ProfileVisualConfig? = null,
    val activeTarget: ProfileEditTarget? = null,
    val pendingMedia: Map<ProfileModuleId, ProfileMediaUpload> = emptyMap(),
    val selectedColorTarget: ProfileColorTarget = ProfileColorTarget.Primary,
    val chromaSession: ChromaCoreSessionState = ChromaCoreSessionState(),
    val customColorArgb: Long = 0xFF13ECA4,
    val isCustomColorPanelVisible: Boolean = false,
    val isSaving: Boolean = false,
    val activeSaveAttemptId: String? = null,
    val lastSaveAttemptId: String? = null,
    val lastSaveErrorCode: String? = null,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null,
)
