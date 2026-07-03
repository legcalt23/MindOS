package com.mindos.app.features.mindboost.profile.data.local

import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readVersioned
import com.mindos.app.features.mindboost.profile.data.dto.MindBoostProfileDto
import com.mindos.app.features.mindboost.profile.data.dto.MindBoostProfileJson
import com.mindos.app.features.mindboost.profile.data.dto.MindBoostProfileSchema
import com.mindos.app.features.mindboost.profile.data.mapper.toDomain
import com.mindos.app.features.mindboost.profile.data.mapper.toDto
import com.mindos.app.features.mindboost.profile.domain.model.MindBoostProfile
import com.mindos.app.features.mindboost.profile.domain.repository.MindBoostProfileRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Stores the latest profile snapshot, encrypted via [SecureKeyValueStore]. Reuses the same secure
 * store surface as the other MindBoost cognitive data (Android = AES-256-GCM Keystore; desktop/iOS =
 * in-memory dev stub). A future-versioned blob is quarantined on read and surfaces as `null`.
 */
class LocalMindBoostProfileRepository(
    private val store: SecureKeyValueStore,
    private val json: Json = MindBoostProfileJson.instance,
) : MindBoostProfileRepository {

    override suspend fun load(userId: String): MindBoostProfile? =
        store.readVersioned(
            key = PROFILE_KEY_PREFIX + userId,
            currentVersion = MindBoostProfileSchema.CURRENT,
            parser = json::decodeFromString,
            versionOf = MindBoostProfileDto::schemaVersion,
        )?.toDomain()

    override suspend fun save(profile: MindBoostProfile) {
        store.put(PROFILE_KEY_PREFIX + profile.userId, json.encodeToString(profile.toDto()))
    }

    override suspend fun clear(userId: String) {
        store.remove(PROFILE_KEY_PREFIX + userId)
    }

    private companion object {
        const val PROFILE_KEY_PREFIX = "mbprofile:"
    }
}
