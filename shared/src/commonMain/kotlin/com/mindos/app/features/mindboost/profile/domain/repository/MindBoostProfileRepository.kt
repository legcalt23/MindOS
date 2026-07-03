package com.mindos.app.features.mindboost.profile.domain.repository

import com.mindos.app.features.mindboost.profile.domain.model.MindBoostProfile

/**
 * Persists the latest computed [MindBoostProfile] snapshot. Local-first and encrypted at rest; the
 * snapshot is a coarse derived cache (non-PII), never uploaded. `load` returns null when nothing is
 * stored yet (or the stored blob was unreadable and quarantined).
 */
interface MindBoostProfileRepository {
    suspend fun load(userId: String): MindBoostProfile?
    suspend fun save(profile: MindBoostProfile)
    suspend fun clear(userId: String)
}
