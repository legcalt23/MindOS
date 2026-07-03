package com.mindos.app.features.profile.card.domain.repository

import com.mindos.app.features.profile.card.domain.ProfileCardDoc

/**
 * Persistence boundary for the structured profile-card document.
 *
 * Two surfaces (CEO decision): the owner edits a PRIVATE draft; every committed
 * save is auto-published to a PUBLIC snapshot readable by any authenticated user.
 * Firestore stores structured metadata only — media binaries live in Storage via
 * [ProfileCardMediaRepository].
 */
interface ProfileCardRepository {
    /** Loads the owner's PRIVATE draft (the editable source of truth). Null if none. */
    suspend fun load(userId: String): ProfileCardDoc?

    /** Loads a user's PUBLIC snapshot (what other authenticated users see). Null if none. */
    suspend fun loadPublic(userId: String): ProfileCardDoc?

    /**
     * Persists [doc] for [userId]: writes the private draft AND publishes the clean
     * public snapshot (auto-publish-on-save). Throws on failure so the caller can keep
     * the local preview and surface an error (never silently "saved").
     */
    suspend fun save(userId: String, doc: ProfileCardDoc)
}
