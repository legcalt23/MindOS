package com.mindos.app.features.profile.domain.repository

import com.mindos.app.features.profile.domain.model.MindOSProfile
import com.mindos.app.features.profile.domain.model.PresenceState
import com.mindos.app.features.profile.domain.model.ProfileAudioClip
import com.mindos.app.features.profile.domain.model.ProfileImageRef
import kotlinx.coroutines.flow.StateFlow

/**
 * Persistence boundary for the MindOS profile. Each mutator returns the updated
 * aggregate so callers (the state holder) don't have to re-fetch.
 *
 * The current implementation is in-memory ([com.mindos.app.features.profile
 * .data.InMemoryMindOSProfileRepository]); a Firebase-backed implementation can
 * be dropped in later without touching presentation code.
 */
interface MindOSProfileRepository {
    suspend fun getProfile(userId: String): MindOSProfile?

    suspend fun updateCoverPhoto(userId: String, image: ProfileImageRef): MindOSProfile

    suspend fun updateProfilePhoto(userId: String, image: ProfileImageRef): MindOSProfile

    suspend fun updateUsernamePrefixEmoji(userId: String, emoji: String?): MindOSProfile

    suspend fun updateProfileAudio(userId: String, clip: ProfileAudioClip?): MindOSProfile

    suspend fun updateCustomLink(
        userId: String,
        slotIndex: Int,
        url: String?,
        icon: ProfileImageRef?,
    ): MindOSProfile
}

/**
 * Observer ("observador") graph. Conceptually a follower system, but the UI
 * always speaks of *observadores*, never followers.
 */
interface ProfileObserverRepository {
    suspend fun observeUser(currentUserId: String, targetUserId: String)

    suspend fun unobserveUser(currentUserId: String, targetUserId: String)

    suspend fun isObserving(currentUserId: String, targetUserId: String): Boolean

    suspend fun observerCount(targetUserId: String): Int
}

/**
 * Foreground presence. "Online" means the user is actively in the app
 * foreground; backgrounding the app makes them offline. There is no remote
 * presence backend yet, so the in-memory implementation only tracks the local
 * device's foreground state cleanly (never fabricated in the composable).
 */
interface ProfilePresenceRepository {
    fun presence(userId: String): StateFlow<PresenceState>

    fun setForegroundPresence(userId: String, isOnline: Boolean)
}
