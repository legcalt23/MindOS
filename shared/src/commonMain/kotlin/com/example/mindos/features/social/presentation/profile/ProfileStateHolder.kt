package com.example.mindos.features.social.presentation.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.mindos.core.security.SafeInput
import com.example.mindos.features.social.domain.error.ProfileSaveError
import com.example.mindos.features.social.domain.error.ProfileSaveException
import com.example.mindos.features.social.domain.error.ProfileSaveNotAuthenticated
import com.example.mindos.features.social.domain.error.ProfileSavePartialRemoteFailure
import com.example.mindos.features.social.domain.error.ProfileSavePermissionDenied
import com.example.mindos.features.social.domain.error.ProfileSaveUnknownRemoteFailure
import com.example.mindos.features.social.domain.error.ProfileSaveValidationError
import com.example.mindos.features.social.domain.model.IdentityPosition
import com.example.mindos.features.social.domain.model.ProfileColorTarget
import com.example.mindos.features.social.domain.model.ProfileDisplayNameMaxLength
import com.example.mindos.features.social.domain.model.ProfileLayoutSlot
import com.example.mindos.features.social.domain.model.ProfileMediaUpload
import com.example.mindos.features.social.domain.model.ProfileModuleContentType
import com.example.mindos.features.social.domain.model.ProfileModuleId
import com.example.mindos.features.social.domain.model.ProfileSmallTextMaxLength
import com.example.mindos.features.social.domain.model.ProfileUserphraseMaxLength
import com.example.mindos.features.social.domain.model.ProfileVisualConfig
import com.example.mindos.features.social.domain.model.isValidProfileUsername
import com.example.mindos.features.social.domain.model.normalizeProfileUsername
import com.example.mindos.features.social.domain.model.safeProfileDisplayName
import com.example.mindos.features.social.domain.model.safeProfileUsername
import com.example.mindos.features.social.domain.repository.ProfileVisualRepository
import com.example.mindos.features.social.domain.repository.ObservationRepository
import com.example.mindos.features.social.domain.repository.ObserveUserResult
import com.example.mindos.features.social.domain.usecase.BlockUserUseCase
import com.example.mindos.features.social.domain.usecase.FollowUserUseCase
import com.example.mindos.features.social.domain.usecase.GetProfileUseCase
import com.example.mindos.features.social.domain.usecase.ProfileDetails
import com.example.mindos.features.social.domain.usecase.UnblockUserUseCase
import com.example.mindos.features.social.domain.usecase.UnfollowUserUseCase
import com.example.mindos.features.social.domain.valueobject.UserId
import com.example.mindos.features.social.presentation.state.ProfileActionState
import com.example.mindos.features.social.presentation.state.ProfileUiState
import com.example.mindos.features.social.presentation.state.toUiMessage
import com.example.mindos.features.social.shared.util.AppClock
import com.example.mindos.features.social.shared.util.DomainResult
import com.example.mindos.features.social.shared.util.getOrNull
import com.example.mindos.features.social.shared.util.logProfileSave
import com.example.mindos.features.social.shared.util.profileSaveExceptionLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

@Stable
class ProfileStateHolder(
    private val getProfile: GetProfileUseCase,
    private val followUser: FollowUserUseCase,
    private val unfollowUser: UnfollowUserUseCase,
    private val blockUser: BlockUserUseCase,
    private val unblockUser: UnblockUserUseCase,
    private val profileVisualRepository: ProfileVisualRepository,
    private val observationRepository: ObservationRepository? = null,
    private val clock: AppClock,
) {
    private val composeUiState = mutableStateOf(ProfileUiState())
    private val _uiStateFlow = MutableStateFlow(ProfileUiState())
    val uiStateFlow: StateFlow<ProfileUiState> = _uiStateFlow.asStateFlow()

    var uiState: ProfileUiState
        get() = composeUiState.value
        private set(value) {
            composeUiState.value = value
            _uiStateFlow.value = value
        }

    private var viewerId: UserId? = null
    private var ownerId: UserId? = null
    private val saveMutex = Mutex()
    private val actionMutex = Mutex()
    private var nextSaveAttemptNumber = 1L

    suspend fun load(viewerId: UserId, ownerId: UserId) {
        val targetChanged = this.ownerId != ownerId
        this.viewerId = viewerId
        this.ownerId = ownerId
        uiState = if (targetChanged) {
            ProfileUiState(isLoading = true)
        } else {
            uiState.copy(isLoading = true, errorMessage = null)
        }
        uiState = when (val result = getProfile(viewerId, ownerId)) {
            is DomainResult.Success -> {
                val fallbackConfig = result.value.toDefaultVisualConfig(ownerId)
                val visualResult = runCatching {
                    profileVisualRepository.getConfig(ownerId)
                }
                val remoteObservationState = loadRemoteObservationState(viewerId, ownerId)
                if (visualResult.isFailure) {
                    ProfileUiState(
                        details = result.value.withRemoteObservation(remoteObservationState),
                        visualConfig = fallbackConfig.copy(
                            observersCount = remoteObservationState.observersCount ?: fallbackConfig.observersCount,
                        ).normalized(),
                        errorMessage = "No se pudo sincronizar el perfil visual: ${visualResult.exceptionOrNull()?.message.orEmpty()}",
                    )
                } else {
                    val loadedVisual = visualResult.getOrNull()
                    val observersCount = remoteObservationState.observersCount
                        ?: loadedVisual?.observersCount
                        ?: result.value.counters?.followersCount
                        ?: fallbackConfig.observersCount
                    ProfileUiState(
                        details = result.value.withRemoteObservation(remoteObservationState),
                        visualConfig = (loadedVisual ?: fallbackConfig).copy(
                            observersCount = observersCount,
                        ).normalized(),
                    )
                }
            }

            is DomainResult.Failure -> ProfileUiState(errorMessage = result.error.toUiMessage())
        }
    }

    suspend fun follow() {
        val observations = observationRepository
        if (observations == null) {
            runAction("Observacion actualizada.") { actor, target ->
                followUser(actor, target)
            }
        } else {
            runObservationAction(observations)
        }
    }

    suspend fun unfollow() = runAction("Observacion retirada.") { actor, target ->
        unfollowUser(actor, target)
    }

    suspend fun block() = runAction("Usuario bloqueado.") { actor, target ->
        blockUser(actor, target)
    }

    suspend fun unblock() = runAction("Usuario desbloqueado.") { actor, target ->
        unblockUser(actor, target)
    }

    fun enterEditMode(target: ProfileEditTarget? = null) {
        if (uiState.editor.isSaving) return
        val config = uiState.visualConfig ?: return
        uiState = uiState.copy(
            editor = ProfileEditorUiState(
                isEditing = true,
                draft = config,
                activeTarget = target,
                customColorArgb = config.colors.secondaryArgb,
                chromaSession = ChromaCoreSessionState(
                    isVisible = false,
                    persistedColor = config.colors.secondaryArgb,
                    sessionInitialColor = config.colors.secondaryArgb,
                    previewColor = config.colors.secondaryArgb,
                ),
            ),
        )
    }

    fun focusEditTarget(target: ProfileEditTarget) {
        if (uiState.editor.isSaving) return
        if (!uiState.editor.isEditing) {
            enterEditMode(target)
            return
        }
        uiState = uiState.copy(
            editor = uiState.editor.copy(
                activeTarget = target,
                errorMessage = null,
                feedbackMessage = null,
            ),
        )
    }

    fun clearEditTarget() {
        if (uiState.editor.isSaving || !uiState.editor.isEditing) return
        uiState = uiState.copy(
            editor = uiState.editor.copy(
                activeTarget = null,
                errorMessage = null,
            ),
        )
    }

    fun cancelEdit() {
        if (uiState.editor.isSaving) return
        uiState = uiState.copy(editor = ProfileEditorUiState())
    }

    suspend fun saveEdit(keepEditing: Boolean = false) {
        if (!saveMutex.tryLock()) {
            logProfileSave(
                "event=save_ignored reason=already_saving active_save_attempt_id=${uiState.editor.activeSaveAttemptId.orEmpty()}"
            )
            return
        }

        val attemptId = nextSaveAttemptId()
        try {
            saveEditLocked(attemptId, keepEditing)
        } finally {
            saveMutex.unlock()
        }
    }

    fun updateDisplayName(value: String) {
        if (uiState.editor.isSaving) return
        mutateDraft { it.copy(displayName = value.take(ProfileDisplayNameMaxLength)) }
        uiState = uiState.copy(editor = uiState.editor.copy(activeTarget = ProfileEditTarget.Username))
    }

    fun updateUsername(value: String) {
        if (uiState.editor.isSaving) return
        mutateDraft { it.copy(username = normalizeProfileUsername(value)) }
        uiState = uiState.copy(editor = uiState.editor.copy(activeTarget = ProfileEditTarget.Username))
    }

    fun updateUserphrase(value: String) {
        if (uiState.editor.isSaving) return
        mutateDraft { it.copy(userphrase = SafeInput.normalizeSingleLineHumanText(value, ProfileUserphraseMaxLength)) }
        uiState = uiState.copy(editor = uiState.editor.copy(activeTarget = ProfileEditTarget.UserPhrase))
    }

    fun updateIdentityPosition(position: IdentityPosition) {
        if (uiState.editor.isSaving) return
        mutateDraft { it.copy(identityPosition = position) }
    }

    fun selectColorTarget(target: ProfileColorTarget) {
        if (uiState.editor.isSaving) return
        uiState = uiState.copy(
            editor = uiState.editor.copy(
                selectedColorTarget = target,
                activeTarget = ProfileEditTarget.ThemeColors,
            ),
        )
    }

    fun applyColor(argb: Long) {
        if (uiState.editor.isSaving) return
        mutateDraft { config ->
            val nextColors = when (uiState.editor.selectedColorTarget) {
                ProfileColorTarget.Primary -> config.colors.copy(primaryArgb = argb)
                ProfileColorTarget.Secondary -> config.colors.copy(secondaryArgb = argb)
            }
            config.copy(colors = nextColors)
        }
    }

    fun updateCustomColor(argb: Long, mode: ChromaCoreInteractionMode) {
        if (uiState.editor.isSaving) return
        val session = uiState.editor.chromaSession
        uiState = uiState.copy(
            editor = uiState.editor.copy(
                customColorArgb = argb,
                chromaSession = session.copy(
                    previewColor = argb,
                    interactionMode = mode,
                ),
            ),
        )
        applyColor(argb)
    }

    fun toggleCustomColorPanel() {
        if (uiState.editor.isSaving) return
        val editor = uiState.editor
        val config = editor.draft ?: return
        val currentColor = when (editor.selectedColorTarget) {
            ProfileColorTarget.Primary -> config.colors.primaryArgb
            ProfileColorTarget.Secondary -> config.colors.secondaryArgb
        }
        val isNowVisible = !editor.chromaSession.isVisible

        uiState = uiState.copy(
            editor = editor.copy(
                isCustomColorPanelVisible = isNowVisible,
                customColorArgb = currentColor,
                chromaSession = editor.chromaSession.copy(
                    isVisible = isNowVisible,
                    persistedColor = currentColor,
                    sessionInitialColor = currentColor,
                    previewColor = currentColor,
                    interactionMode = ChromaCoreInteractionMode.Idle,
                ),
            ),
        )
    }

    fun confirmChromaSession() {
        if (uiState.editor.isSaving) return
        val session = uiState.editor.chromaSession
        uiState = uiState.copy(
            editor = uiState.editor.copy(
                customColorArgb = session.previewColor,
                isCustomColorPanelVisible = false,
                chromaSession = session.copy(
                    isVisible = false,
                    persistedColor = session.previewColor,
                    sessionInitialColor = session.previewColor,
                    interactionMode = ChromaCoreInteractionMode.Idle,
                ),
            ),
        )
        applyColor(session.previewColor)
    }

    fun cancelChromaSession() {
        if (uiState.editor.isSaving) return
        val session = uiState.editor.chromaSession
        uiState = uiState.copy(
            editor = uiState.editor.copy(
                customColorArgb = session.sessionInitialColor,
                isCustomColorPanelVisible = false,
                chromaSession = session.copy(
                    isVisible = false,
                    previewColor = session.sessionInitialColor,
                    interactionMode = ChromaCoreInteractionMode.Idle,
                ),
            ),
        )
        applyColor(session.sessionInitialColor)
    }

    fun moveModule(moduleId: ProfileModuleId, desiredSlot: ProfileLayoutSlot) {
        if (uiState.editor.isSaving) return
        mutateDraft { it.moveModuleToSlot(moduleId, desiredSlot) }
    }

    fun clearModule(moduleId: ProfileModuleId) {
        if (uiState.editor.isSaving) return
        mutateDraft { it.clearModule(moduleId) }
        uiState = uiState.copy(
            editor = uiState.editor.copy(
                pendingMedia = uiState.editor.pendingMedia - moduleId,
                activeTarget = when (uiState.editor.activeTarget) {
                    is ProfileEditTarget.Highlight ->
                        if ((uiState.editor.activeTarget as ProfileEditTarget.Highlight).moduleId == moduleId) {
                            ProfileEditTarget.Highlight(moduleId)
                        } else {
                            uiState.editor.activeTarget
                        }
                    else -> uiState.editor.activeTarget
                },
            ),
        )
    }

    fun updateTextModule(moduleId: ProfileModuleId, text: String) {
        if (uiState.editor.isSaving) return
        runCatching {
            mutateDraft { it.withTextModule(moduleId, SafeInput.normalizeMultilineHumanText(text, ProfileSmallTextMaxLength)) }
            uiState = uiState.copy(
                editor = uiState.editor.copy(
                    pendingMedia = uiState.editor.pendingMedia - moduleId,
                    activeTarget = ProfileEditTarget.Highlight(moduleId),
                ),
            )
        }.onFailure { error ->
            uiState = uiState.copy(editor = uiState.editor.copy(errorMessage = error.message))
        }
    }

    fun setModuleMedia(moduleId: ProfileModuleId, upload: ProfileMediaUpload) {
        if (uiState.editor.isSaving) return
        upload.validationError(moduleId)?.let { message ->
            uiState = uiState.copy(editor = uiState.editor.copy(errorMessage = message, feedbackMessage = null))
            return
        }
        mutateDraft { it.withPendingMedia(moduleId, upload.kind) }
        uiState = uiState.copy(
            editor = uiState.editor.copy(
                pendingMedia = uiState.editor.pendingMedia + (moduleId to upload),
                activeTarget = when (moduleId) {
                    ProfileModuleId.Main -> ProfileEditTarget.FotoPortada
                    else -> ProfileEditTarget.Highlight(moduleId)
                },
                errorMessage = null,
                feedbackMessage = "Contenido listo para guardar.",
            ),
        )
    }

    fun reportMediaPickerError(message: String) {
        if (uiState.editor.isSaving) return
        uiState = uiState.copy(editor = uiState.editor.copy(errorMessage = message, feedbackMessage = null))
    }

    private suspend fun saveEditLocked(attemptId: String, keepEditing: Boolean) {
        val editor = uiState.editor
        val currentDraft = editor.draft ?: return
        val rawDraft = currentDraft.copy(
            displayName = currentDraft.displayName.trim(),
            username = normalizeProfileUsername(currentDraft.username),
        )
        val draft = rawDraft.normalized()
        val previous = uiState.visualConfig?.normalized()
        logProfileSave("event=save_start save_attempt_id=$attemptId")

        validateDraftForSave(attemptId, draft, editor, rawDraft.userphrase)?.let { validationError ->
            publishSaveFailure(editor, draft, validationError)
            return
        }

        uiState = uiState.copy(
            editor = editor.copy(
                draft = draft,
                isSaving = true,
                activeSaveAttemptId = attemptId,
                lastSaveErrorCode = null,
                errorMessage = null,
                feedbackMessage = null,
            ),
        )

        val uploadedPaths = mutableListOf<String>()
        var owner: UserId? = null
        var committed = false

        try {
            val authenticatedUserId = resolveAuthenticatedOwner(attemptId, draft)
            owner = authenticatedUserId
            var nextConfig = draft.copy(userId = authenticatedUserId).normalized()

            if (editor.pendingMedia.isEmpty() &&
                previous != null &&
                nextConfig.hasSameWritableContentAs(previous)
            ) {
                logProfileSave(
                    "event=no_changes save_attempt_id=$attemptId"
                )
                uiState = uiState.copy(
                    editor = editor.copy(
                        isEditing = true,
                        draft = nextConfig,
                        isSaving = false,
                        activeSaveAttemptId = null,
                        lastSaveAttemptId = attemptId,
                        lastSaveErrorCode = null,
                        feedbackMessage = "No hay cambios pendientes.",
                        errorMessage = null,
                    ),
                )
                return
            }

            editor.pendingMedia.forEach { (moduleId, upload) ->
                val uploadedAt = clock.now()
                logProfileSave(
                    "event=upload_prepare save_attempt_id=$attemptId module=${moduleId.persistedName} " +
                        "kind=${upload.kind} mime=${upload.mimeType} has_thumbnail=${upload.thumbnailBytes != null}"
                )
                val mediaMeta = profileVisualRepository.uploadModuleMedia(
                    userId = authenticatedUserId,
                    moduleId = moduleId,
                    upload = upload,
                    uploadedAt = uploadedAt,
                    attemptId = attemptId,
                )
                logProfileSave(
                    "event=upload_complete save_attempt_id=$attemptId module=${moduleId.persistedName} " +
                        "mime=${mediaMeta.mimeType} has_thumbnail=${mediaMeta.thumbnailStoragePath != null}"
                )
                uploadedPaths += mediaMeta.storagePath
                mediaMeta.thumbnailStoragePath?.let(uploadedPaths::add)
                nextConfig = nextConfig.withSavedMedia(moduleId, mediaMeta).normalized()
            }

            val finalConfig = nextConfig.copy(updatedAt = clock.now()).normalized()
            try {
                profileVisualRepository.saveConfig(finalConfig, attemptId = attemptId)
                committed = true
                logProfileSave(
                    "event=commit_complete save_attempt_id=$attemptId"
                )
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                val remoteError = error.toProfileSaveError(
                    attemptId = attemptId,
                    phase = "commit",
                    operation = "firestore_commit_profile_visual",
                    userId = authenticatedUserId,
                    path = profileDocumentPath(authenticatedUserId),
                )
                if (uploadedPaths.isNotEmpty()) {
                    compensateUploadedMedia(attemptId, authenticatedUserId, uploadedPaths)
                    throw ProfileSaveException(
                        ProfileSavePartialRemoteFailure(
                            attemptId = attemptId,
                            phase = "commit",
                            operation = "firestore_commit_profile_visual",
                            userId = authenticatedUserId,
                            path = profileDocumentPath(authenticatedUserId),
                            completedOperations = listOf("storage_upload_profile_media"),
                            failedOperation = "firestore_commit_profile_visual",
                            failedCode = remoteError.code,
                            causeMessage = remoteError.causeMessage ?: error.message,
                        ),
                        error,
                    )
                }
                throw error
            }

            deleteReplacedMedia(previous, finalConfig, attemptId)
            logProfileSave(
                "event=save_success save_attempt_id=$attemptId committed=$committed"
            )
            uiState = uiState.copy(
                visualConfig = finalConfig,
                editor = if (keepEditing) {
                    editor.copy(
                        isEditing = true,
                        draft = finalConfig,
                        pendingMedia = emptyMap(),
                        isSaving = false,
                        activeSaveAttemptId = null,
                        lastSaveAttemptId = attemptId,
                        lastSaveErrorCode = null,
                        feedbackMessage = "Perfil visual guardado.",
                        errorMessage = null,
                    )
                } else {
                    ProfileEditorUiState(
                        lastSaveAttemptId = attemptId,
                        feedbackMessage = "Perfil visual guardado.",
                    )
                },
            )
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            val saveError = error.toProfileSaveError(
                attemptId = attemptId,
                phase = "save",
                operation = "profile_save",
                userId = owner,
                path = owner?.let(::profileDocumentPath),
            )
            logProfileSave(
                "event=save_failure save_attempt_id=$attemptId committed=$committed error_code=${saveError.code} " +
                    "exception=${error.profileSaveExceptionLog()}"
            )
            publishSaveFailure(editor, draft, saveError)
        }
    }

    private fun validateDraftForSave(
        attemptId: String,
        draft: ProfileVisualConfig,
        editor: ProfileEditorUiState,
        rawUserphrase: String = draft.userphrase,
    ): ProfileSaveError? {
        if (draft.displayName.isBlank()) {
            return ProfileSaveValidationError(
                attemptId = attemptId,
                phase = "validation",
                operation = "validate_profile_visual_draft",
                userId = draft.userId,
                causeMessage = "El nombre visible no puede estar vacio.",
            )
        }

        if (SafeInput.hasBlockedControlChars(rawUserphrase) ||
            SafeInput.hasBlockedControlChars(draft.displayName)
        ) {
            return ProfileSaveValidationError(
                attemptId = attemptId,
                phase = "validation",
                operation = "validate_profile_visual_draft",
                userId = draft.userId,
                causeMessage = "El texto contiene caracteres no permitidos.",
            )
        }

        if (!isValidProfileUsername(draft.username)) {
            return ProfileSaveValidationError(
                attemptId = attemptId,
                phase = "validation",
                operation = "validate_profile_visual_draft",
                userId = draft.userId,
                causeMessage = "El username debe tener 3-30 caracteres: letras minusculas, numeros, puntos o guiones bajos.",
            )
        }

        if (rawUserphrase.trim().length > ProfileUserphraseMaxLength) {
            return ProfileSaveValidationError(
                attemptId = attemptId,
                phase = "validation",
                operation = "validate_profile_visual_draft",
                userId = draft.userId,
                causeMessage = "La frase publica no puede superar los 100 caracteres.",
            )
        }

        if (containsEmoji(rawUserphrase)) {
            return ProfileSaveValidationError(
                attemptId = attemptId,
                phase = "validation",
                operation = "validate_profile_visual_draft",
                userId = draft.userId,
                causeMessage = "La frase publica no puede contener emojis.",
            )
        }

        editor.pendingMedia.forEach { (moduleId, upload) ->
            upload.validationError(moduleId)?.let { message ->
                return ProfileSaveValidationError(
                    attemptId = attemptId,
                    phase = "validation",
                    operation = "validate_profile_media",
                    userId = draft.userId,
                    path = "profile_visuals/${draft.userId.value}/${moduleId.storageFolder}",
                    causeMessage = message,
                )
            }
        }

        draft.modules.forEach { module ->
            val requiresMedia = module.contentType == ProfileModuleContentType.Photo ||
                module.contentType == ProfileModuleContentType.Video
            if (requiresMedia && module.media == null && module.id !in editor.pendingMedia.keys) {
                return ProfileSaveValidationError(
                    attemptId = attemptId,
                    phase = "validation",
                    operation = "validate_profile_visual_draft",
                    userId = draft.userId,
                    path = "profile_visuals/${draft.userId.value}/${module.id.storageFolder}",
                    causeMessage = "El modulo multimedia necesita un archivo valido antes de guardar.",
                )
            }
        }

        return null
    }

    private fun resolveAuthenticatedOwner(attemptId: String, draft: ProfileVisualConfig): UserId {
        val authenticatedUserId = profileVisualRepository.currentAuthenticatedUserId()
            ?: throw ProfileSaveException(
                ProfileSaveNotAuthenticated(
                    attemptId = attemptId,
                    phase = "auth",
                    operation = "resolve_current_user",
                    path = profileDocumentPath(draft.userId),
                    causeMessage = "ProfileVisualRepository.currentAuthenticatedUserId returned null.",
                ),
            )
        val targetOwner = ownerId ?: draft.userId
        if (authenticatedUserId != targetOwner) {
            throw ProfileSaveException(
                ProfileSavePermissionDenied(
                    attemptId = attemptId,
                    phase = "auth",
                    operation = "resolve_current_user",
                    userId = authenticatedUserId,
                    path = profileDocumentPath(targetOwner),
                    causeMessage = "Authenticated uid ${authenticatedUserId.value} does not match target ${targetOwner.value}.",
                ),
            )
        }
        logProfileSave(
            "event=auth_verified save_attempt_id=$attemptId"
        )
        return authenticatedUserId
    }

    private fun publishSaveFailure(
        editor: ProfileEditorUiState,
        draft: ProfileVisualConfig,
        error: ProfileSaveError,
    ) {
        uiState = uiState.copy(
            editor = editor.copy(
                isEditing = true,
                draft = draft,
                isSaving = false,
                activeSaveAttemptId = null,
                lastSaveAttemptId = error.attemptId,
                lastSaveErrorCode = error.code,
                feedbackMessage = null,
                errorMessage = error.message,
            ),
        )
    }

    private suspend fun compensateUploadedMedia(
        attemptId: String,
        owner: UserId,
        uploadedPaths: List<String>,
    ) {
        uploadedPaths.distinct().forEach { path ->
            runCatching {
                profileVisualRepository.deleteMedia(owner, path, attemptId = attemptId)
            }.onFailure { cleanupError ->
                logProfileSave(
                    "event=compensation_failure save_attempt_id=$attemptId " +
                        "exception=${cleanupError.profileSaveExceptionLog()}"
                )
            }
        }
    }

    private fun mutateDraft(transform: (ProfileVisualConfig) -> ProfileVisualConfig) {
        val draft = uiState.editor.draft ?: return
        uiState = uiState.copy(
            editor = uiState.editor.copy(
                draft = transform(draft).normalized(),
                errorMessage = null,
            ),
        )
    }

    private suspend fun deleteReplacedMedia(
        previous: ProfileVisualConfig?,
        current: ProfileVisualConfig,
        attemptId: String,
    ) {
        previous ?: return
        val currentPaths = current.modules.flatMap { module ->
            listOfNotNull(module.media?.storagePath, module.media?.thumbnailStoragePath)
        }.toSet()
        previous.modules.flatMap { module ->
            listOfNotNull(module.media?.storagePath, module.media?.thumbnailStoragePath)
        }.filterNot { it in currentPaths }
            .forEach { path ->
                runCatching {
                    profileVisualRepository.deleteMedia(current.userId, path, attemptId = attemptId)
                }.onFailure { cleanupError ->
                    logProfileSave(
                        "event=non_critical_cleanup_failure save_attempt_id=$attemptId " +
                            "exception=${cleanupError.profileSaveExceptionLog()}"
                    )
                }
            }
    }

    private suspend fun runAction(
        successMessage: String,
        action: suspend (UserId, UserId) -> DomainResult<*>,
    ) {
        val actor = viewerId ?: return
        val target = ownerId ?: return
        if (!actionMutex.tryLock()) return
        try {
            uiState = uiState.copy(actionState = ProfileActionState.Running)
            uiState = when (val result = action(actor, target)) {
                is DomainResult.Success -> uiState.copy(
                    actionState = ProfileActionState.Succeeded(successMessage),
                )

                is DomainResult.Failure -> uiState.copy(
                    actionState = ProfileActionState.Failed(result.error.toUiMessage()),
                )
            }
            load(actor, target)
        } finally {
            actionMutex.unlock()
        }
    }

    private suspend fun runObservationAction(observations: ObservationRepository) {
        val actor = viewerId ?: return
        val target = ownerId ?: return
        if (!actionMutex.tryLock()) return
        try {
            uiState = uiState.copy(actionState = ProfileActionState.Running)
            uiState = when (val result = observations.observe(actor, target)) {
                is DomainResult.Success -> {
                    val message = when (result.value) {
                        ObserveUserResult.Created -> "Observacion actualizada."
                        ObserveUserResult.AlreadyObserving -> "Ya observas este perfil."
                    }
                    uiState.copy(actionState = ProfileActionState.Succeeded(message))
                }

                is DomainResult.Failure -> uiState.copy(
                    actionState = ProfileActionState.Failed(result.error.toUiMessage()),
                )
            }
            load(actor, target)
        } finally {
            actionMutex.unlock()
        }
    }

    private suspend fun loadRemoteObservationState(viewerId: UserId, ownerId: UserId): RemoteObservationState {
        val observations = observationRepository ?: return RemoteObservationState()
        val isObserving = if (viewerId == ownerId) {
            false
        } else {
            observations.isObserving(viewerId, ownerId).getOrNull()
        }
        val observersCount = observations.observersCount(ownerId).getOrNull()
        return RemoteObservationState(
            isObserving = isObserving,
            observersCount = observersCount,
        )
    }

    private fun ProfileDetails.withRemoteObservation(remote: RemoteObservationState): ProfileDetails {
        val observing = remote.isObserving ?: return this
        if (relationshipState.isSelf) return this
        val nextRelationship = relationshipState.copy(
            isFollowing = observing,
            isMutual = observing && relationshipState.followsYou,
            canFollow = !observing && relationshipState.canFollow,
            canRequestFollow = !observing && relationshipState.canRequestFollow,
            canUnfollow = observing && !relationshipState.blockedByYou && !relationshipState.blockedYou,
        )
        return copy(relationshipState = nextRelationship)
    }

    private fun Throwable.toProfileSaveError(
        attemptId: String,
        phase: String,
        operation: String,
        userId: UserId?,
        path: String?,
    ): ProfileSaveError =
        when (this) {
            is ProfileSaveException -> saveError
            is IllegalArgumentException -> ProfileSaveValidationError(
                attemptId = attemptId,
                phase = phase,
                operation = operation,
                userId = userId,
                path = path,
                causeMessage = message,
            )
            is IllegalStateException -> ProfileSaveUnknownRemoteFailure(
                attemptId = attemptId,
                phase = phase,
                operation = operation,
                userId = userId,
                path = path,
                causeMessage = message,
            )
            else -> ProfileSaveUnknownRemoteFailure(
                attemptId = attemptId,
                phase = phase,
                operation = operation,
                userId = userId,
                path = path,
                causeMessage = message,
            )
        }

    private fun nextSaveAttemptId(): String =
        "profile_save_${nextSaveAttemptNumber++}"

    private fun profileDocumentPath(userId: UserId): String =
        "profile_visuals/${userId.value}"

    private fun ProfileVisualConfig.hasSameWritableContentAs(other: ProfileVisualConfig): Boolean {
        val left = normalized()
        val right = other.normalized()
        return left.displayName == right.displayName &&
            left.username == right.username &&
            left.userphrase == right.userphrase &&
            left.searchable == right.searchable &&
            left.identityPosition == right.identityPosition &&
            left.colors == right.colors &&
            left.categoryIcons == right.categoryIcons &&
            left.layoutPreset == right.layoutPreset &&
            left.modules == right.modules
    }

    private fun ProfileDetails.toDefaultVisualConfig(ownerId: UserId): ProfileVisualConfig {
        val visibleProfile = this.profile
        val username = safeProfileUsername(
            ownerId,
            visibleProfile?.handle?.value,
            visibleProfile?.displayName?.value,
        )
        return ProfileVisualConfig.createDefault(
            userId = ownerId,
            displayName = safeProfileDisplayName(
                userId = ownerId,
                displayName = visibleProfile?.displayName?.value.orEmpty(),
                username = username,
            ),
            username = username,
            observersCount = counters?.followersCount ?: 0,
            now = clock.now(),
        )
    }
}

@Composable
fun rememberProfileStateHolder(
    getProfile: GetProfileUseCase,
    followUser: FollowUserUseCase,
    unfollowUser: UnfollowUserUseCase,
    blockUser: BlockUserUseCase,
    unblockUser: UnblockUserUseCase,
    profileVisualRepository: ProfileVisualRepository,
    observationRepository: ObservationRepository? = null,
    clock: AppClock,
): ProfileStateHolder =
    remember(getProfile, followUser, unfollowUser, blockUser, unblockUser, profileVisualRepository, observationRepository, clock) {
        ProfileStateHolder(
            getProfile = getProfile,
            followUser = followUser,
            unfollowUser = unfollowUser,
            blockUser = blockUser,
            unblockUser = unblockUser,
            profileVisualRepository = profileVisualRepository,
            observationRepository = observationRepository,
            clock = clock,
        )
    }

private data class RemoteObservationState(
    val isObserving: Boolean? = null,
    val observersCount: Int? = null,
)
