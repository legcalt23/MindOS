package com.example.mindos.features.profile.data

import com.example.mindos.features.profile.domain.model.MindOSProfile
import com.example.mindos.features.profile.domain.model.PresenceState
import com.example.mindos.features.profile.domain.model.ProfileAudioClip
import com.example.mindos.features.profile.domain.model.ProfileCustomLink
import com.example.mindos.features.profile.domain.model.ProfileImageRef
import com.example.mindos.features.profile.domain.repository.MindOSProfileRepository
import com.example.mindos.features.profile.domain.repository.ProfileObserverRepository
import com.example.mindos.features.profile.domain.repository.ProfilePresenceRepository
import com.example.mindos.features.social.domain.model.ProfileMediaUpload
import com.example.mindos.features.social.domain.model.ProfileMediaUploadKind
import com.example.mindos.features.social.domain.model.ProfileModuleId
import com.example.mindos.features.social.domain.model.ProfileVisualConfig
import com.example.mindos.features.social.domain.model.safeProfileDisplayName
import com.example.mindos.features.social.domain.model.safeProfileUsername
import com.example.mindos.features.social.domain.repository.ObservationRepository
import com.example.mindos.features.social.domain.repository.ProfileVisualRepository
import com.example.mindos.features.social.domain.valueobject.UserId
import com.example.mindos.features.social.shared.util.AppClock
import com.example.mindos.features.social.shared.util.DomainResult
import com.example.mindos.features.social.shared.util.getOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Production adapter for the premium profile screen.
 *
 * It deliberately reuses the existing, rules-backed `profile_visuals` contract
 * instead of inventing an unprotected collection. The premium cover maps to the
 * main visual module and the avatar maps to small_1. Fields that do not yet have
 * a Firebase contract are left unchanged rather than stored in process memory.
 */
class FirebaseMindOSProfileRepository(
    private val visualRepository: ProfileVisualRepository,
    private val clock: AppClock,
) : MindOSProfileRepository {

    override suspend fun getProfile(userId: String): MindOSProfile? {
        val owner = UserId(userId)
        return visualRepository.getConfig(owner)?.toMindOSProfile()
    }

    override suspend fun updateCoverPhoto(userId: String, image: ProfileImageRef): MindOSProfile =
        updateImage(userId = userId, image = image, moduleId = ProfileModuleId.Main)

    override suspend fun updateProfilePhoto(userId: String, image: ProfileImageRef): MindOSProfile =
        updateImage(userId = userId, image = image, moduleId = ProfileModuleId.SmallOne)

    override suspend fun updateUsernamePrefixEmoji(userId: String, emoji: String?): MindOSProfile =
        unsupportedProfileField()

    override suspend fun updateProfileAudio(userId: String, clip: ProfileAudioClip?): MindOSProfile =
        unsupportedProfileField()

    override suspend fun updateCustomLink(
        userId: String,
        slotIndex: Int,
        url: String?,
        icon: ProfileImageRef?,
    ): MindOSProfile = unsupportedProfileField()

    private suspend fun updateImage(
        userId: String,
        image: ProfileImageRef,
        moduleId: ProfileModuleId,
    ): MindOSProfile {
        val bytes = image.localBytes?.takeIf { it.isNotEmpty() }
            ?: return currentProfile(userId)
        val owner = UserId(userId)
        val current = currentVisual(owner)
        val uploadedAt = clock.now()
        val attemptId = "mindos_profile_${moduleId.persistedName}_${uploadedAt.epochMillis}"
        val media = visualRepository.uploadModuleMedia(
            userId = owner,
            moduleId = moduleId,
            upload = ProfileMediaUpload(
                kind = ProfileMediaUploadKind.Photo,
                bytes = bytes,
                mimeType = bytes.inferredImageMimeType(),
            ),
            uploadedAt = uploadedAt,
            attemptId = attemptId,
        )
        val updated = current
            .withSavedMedia(moduleId, media)
            .copy(updatedAt = clock.now())
            .normalized()
        visualRepository.saveConfig(updated, attemptId = attemptId)
        return updated.toMindOSProfile()
    }

    private suspend fun currentProfile(userId: String): MindOSProfile =
        currentVisual(UserId(userId)).toMindOSProfile()

    private suspend fun currentVisual(owner: UserId): ProfileVisualConfig =
        visualRepository.getConfig(owner) ?: defaultVisual(owner)

    private fun defaultVisual(owner: UserId): ProfileVisualConfig {
        val username = safeProfileUsername(owner, owner.value)
        return ProfileVisualConfig.createDefault(
            userId = owner,
            displayName = safeProfileDisplayName(owner, username, username),
            username = username,
            observersCount = 0,
            now = clock.now(),
        )
    }
}

class FirebaseMindOSProfileObserverRepository(
    private val observationRepository: ObservationRepository,
) : ProfileObserverRepository {

    override suspend fun observeUser(currentUserId: String, targetUserId: String) {
        val result = observationRepository.observe(UserId(currentUserId), UserId(targetUserId))
        if (result is DomainResult.Failure) error(result.error.message)
    }

    override suspend fun unobserveUser(currentUserId: String, targetUserId: String) {
        // No production delete/unobserve contract exists for `observations` yet.
        // Do not simulate a successful remote state change.
    }

    override suspend fun isObserving(currentUserId: String, targetUserId: String): Boolean =
        observationRepository
            .isObserving(UserId(currentUserId), UserId(targetUserId))
            .getOrNull() == true

    override suspend fun observerCount(targetUserId: String): Int =
        observationRepository
            .observersCount(UserId(targetUserId))
            .getOrNull()
            ?.coerceAtLeast(0)
            ?: 0
}

class NoRemoteProfilePresenceRepository : ProfilePresenceRepository {
    private val offline = MutableStateFlow(PresenceState.Offline)

    override fun presence(userId: String): StateFlow<PresenceState> = offline

    override fun setForegroundPresence(userId: String, isOnline: Boolean) {
        // Presence has no backend contract. Keep the public state offline rather
        // than fabricating online status in production.
    }
}

private fun ProfileVisualConfig.toMindOSProfile(): MindOSProfile {
    val cover = module(ProfileModuleId.Main).media?.storagePath
        ?: mainProfilePhotoUrl?.takeIf { it.isNotBlank() }
    val avatar = module(ProfileModuleId.SmallOne).media?.storagePath
    return MindOSProfile(
        userId = userId.value,
        username = username,
        fotoPerfil = avatar?.let { ProfileImageRef(reference = it) } ?: ProfileImageRef.Empty,
        fotoPortada = cover?.let { ProfileImageRef(reference = it) } ?: ProfileImageRef.Empty,
        observerCount = observersCount.coerceAtLeast(0),
    )
}

private fun unsupportedProfileField(): Nothing =
    error("Esta opcion de perfil aun no tiene backend de produccion.")

@Suppress("CyclomaticComplexMethod", "MagicNumber")
private fun ByteArray.inferredImageMimeType(): String =
    when {
        size >= 3 && this[0] == 0xFF.toByte() && this[1] == 0xD8.toByte() && this[2] == 0xFF.toByte() ->
            "image/jpeg"
        size >= 8 &&
            this[0] == 0x89.toByte() &&
            this[1] == 0x50.toByte() &&
            this[2] == 0x4E.toByte() &&
            this[3] == 0x47.toByte() ->
            "image/png"
        size >= 12 &&
            this[0] == 0x52.toByte() &&
            this[1] == 0x49.toByte() &&
            this[2] == 0x46.toByte() &&
            this[3] == 0x46.toByte() &&
            this[8] == 0x57.toByte() &&
            this[9] == 0x45.toByte() &&
            this[10] == 0x42.toByte() &&
            this[11] == 0x50.toByte() ->
            "image/webp"
        else -> "image/jpeg"
    }
