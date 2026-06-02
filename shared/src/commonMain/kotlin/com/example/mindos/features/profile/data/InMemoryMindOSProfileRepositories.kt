package com.example.mindos.features.profile.data

import com.example.mindos.features.profile.domain.model.MindOSProfile
import com.example.mindos.features.profile.domain.model.PresenceState
import com.example.mindos.features.profile.domain.model.ProfileAudioClip
import com.example.mindos.features.profile.domain.model.ProfileCustomLink
import com.example.mindos.features.profile.domain.model.ProfileImageRef
import com.example.mindos.features.profile.domain.repository.MindOSProfileRepository
import com.example.mindos.features.profile.domain.repository.ProfileObserverRepository
import com.example.mindos.features.profile.domain.repository.ProfilePresenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory [MindOSProfileRepository]. Persists for the lifetime of the process
 * only — a clean stand-in until a Firebase-backed implementation is wired.
 *
 * `getProfile` creates a default aggregate on first access for any user id, so
 * callers never have to special-case a missing profile. The authoritative
 * username is overlaid by the state holder, so the seeded placeholder username
 * is never shown.
 */
class InMemoryMindOSProfileRepository : MindOSProfileRepository {

    private val mutex = Mutex()
    private val profiles = mutableMapOf<String, MindOSProfile>()

    override suspend fun getProfile(userId: String): MindOSProfile = mutex.withLock {
        profiles.getOrPut(userId) {
            MindOSProfile(userId = userId, username = userId)
        }
    }

    override suspend fun updateCoverPhoto(userId: String, image: ProfileImageRef): MindOSProfile =
        mutate(userId) { it.copy(fotoPortada = image) }

    override suspend fun updateProfilePhoto(userId: String, image: ProfileImageRef): MindOSProfile =
        mutate(userId) { it.copy(fotoPerfil = image) }

    override suspend fun updateUsernamePrefixEmoji(userId: String, emoji: String?): MindOSProfile =
        mutate(userId) { it.copy(usernamePrefixEmoji = emoji?.takeIf { e -> e.isNotBlank() }) }

    override suspend fun updateProfileAudio(userId: String, clip: ProfileAudioClip?): MindOSProfile =
        mutate(userId) { it.copy(audio = clip) }

    override suspend fun updateCustomLink(
        userId: String,
        slotIndex: Int,
        url: String?,
        icon: ProfileImageRef?,
    ): MindOSProfile = mutate(userId) { profile ->
        val updatedSlots = profile.customLinks.map { slot ->
            if (slot.slotIndex == slotIndex) {
                ProfileCustomLink(
                    slotIndex = slotIndex,
                    url = url?.takeIf { it.isNotBlank() },
                    iconReference = icon?.reference,
                    iconLocalBytes = icon?.localBytes,
                )
            } else {
                slot
            }
        }
        profile.copy(customLinks = updatedSlots)
    }

    private suspend fun mutate(
        userId: String,
        transform: (MindOSProfile) -> MindOSProfile,
    ): MindOSProfile = mutex.withLock {
        val current = profiles.getOrPut(userId) { MindOSProfile(userId = userId, username = userId) }
        val updated = transform(current).bumped()
        profiles[userId] = updated
        updated
    }
}

/**
 * In-memory observer graph for tests/previews. Defaults to zero so fake
 * follower counts are never introduced accidentally; tests that need a visual
 * seed must pass [baseObserverCount] explicitly.
 */
class InMemoryProfileObserverRepository(
    private val baseObserverCount: Int = DEFAULT_BASE_COUNT,
) : ProfileObserverRepository {

    private val mutex = Mutex()
    private val observers = mutableMapOf<String, MutableSet<String>>()

    override suspend fun observeUser(currentUserId: String, targetUserId: String) = mutex.withLock {
        observers.getOrPut(targetUserId) { mutableSetOf() }.add(currentUserId)
        Unit
    }

    override suspend fun unobserveUser(currentUserId: String, targetUserId: String) = mutex.withLock {
        observers[targetUserId]?.remove(currentUserId)
        Unit
    }

    override suspend fun isObserving(currentUserId: String, targetUserId: String): Boolean = mutex.withLock {
        observers[targetUserId]?.contains(currentUserId) == true
    }

    override suspend fun observerCount(targetUserId: String): Int = mutex.withLock {
        baseObserverCount + (observers[targetUserId]?.size ?: 0)
    }

    companion object {
        const val DEFAULT_BASE_COUNT = 0
    }
}

/**
 * In-memory presence. Tracks each user's foreground state as a [StateFlow] so
 * the UI reacts immediately when the local user moves to background/foreground.
 */
class InMemoryProfilePresenceRepository : ProfilePresenceRepository {

    private val flows = mutableMapOf<String, MutableStateFlow<PresenceState>>()

    override fun presence(userId: String): StateFlow<PresenceState> = flowFor(userId).asStateFlow()

    override fun setForegroundPresence(userId: String, isOnline: Boolean) {
        flowFor(userId).value = PresenceState.fromForeground(isOnline)
    }

    private fun flowFor(userId: String): MutableStateFlow<PresenceState> =
        flows.getOrPut(userId) { MutableStateFlow(PresenceState.Offline) }
}
