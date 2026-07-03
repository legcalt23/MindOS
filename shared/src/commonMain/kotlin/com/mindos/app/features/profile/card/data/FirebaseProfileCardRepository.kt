package com.mindos.app.features.profile.card.data

import com.mindos.app.core.logging.safeErrorLog
import com.mindos.app.core.network.FirestoreDefaults
import com.mindos.app.core.network.withFirestoreReadTimeout
import com.mindos.app.core.network.withFirestoreWriteTimeout
import com.mindos.app.core.time.SystemEpochClock
import com.mindos.app.features.profile.card.domain.ProfileCardDoc
import com.mindos.app.features.profile.card.domain.repository.ProfileCardRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CancellationException

/**
 * Firestore persistence for the structured profile-card document.
 *
 * - PRIVATE draft (`profile_cards_private/{uid}`): owner-only, the editable source of truth.
 * - PUBLIC snapshot (`profile_cards_public/{uid}`): readable by any authenticated user.
 *
 * [save] writes BOTH in a single transaction (auto-publish-on-save): either both commit
 * or neither, so the public snapshot never lags the private draft or shows partial state.
 * Stores structured metadata only (a JSON `payload` string) — never bytes. Reads tolerate
 * the legacy schemaVersion-1 appearance-only blob (migrated on decode).
 */
class FirebaseProfileCardRepository(
    private val firestore: FirebaseFirestore = Firebase.firestore,
    private val auth: FirebaseAuth = Firebase.auth,
) : ProfileCardRepository {

    override suspend fun load(userId: String): ProfileCardDoc? = loadDoc(PrivateCollection, userId)

    override suspend fun loadPublic(userId: String): ProfileCardDoc? = loadDoc(PublicCollection, userId)

    private suspend fun loadDoc(collection: String, userId: String): ProfileCardDoc? {
        if (userId.isBlank()) return null
        return runCatching {
            val snapshot = withFirestoreReadTimeout {
                firestore.collection(collection).document(userId).get()
            }
            val data = if (snapshot.exists) snapshot.data<Map<String, Any?>>() else null
            val schema = (data?.get(SchemaField) as? Number)?.toInt() ?: LEGACY_SCHEMA
            if (data == null) null else ProfileCardDocCodec.decode(schema, data[PayloadField] as? String)
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            safeErrorLog(LogTag, "load profile card ($collection) failed", error)
            null
        }
    }

    override suspend fun save(userId: String, doc: ProfileCardDoc) {
        check(userId.isNotBlank()) { "Falta el identificador de usuario." }
        check(auth.currentUser?.uid == userId) { "No autenticado como el propietario de la tarjeta." }
        val payload = ProfileCardDocCodec.encode(doc)
        check(payload.length <= MaxPrivatePayload) { "La tarjeta supera el tamaño permitido para guardarse." }

        val now = SystemEpochClock.nowMillis()
        val privateRef = firestore.collection(PrivateCollection).document(userId)
        val publicRef = firestore.collection(PublicCollection).document(userId)

        withFirestoreWriteTimeout(FirestoreDefaults.BatchTimeout) {
            firestore.runTransaction {
                val priv = get(privateRef)
                val pub = get(publicRef)
                val privateMap: Map<String, Any?> = mapOf(
                    SchemaField to ProfileCardDocCodec.SCHEMA_VERSION.toLong(),
                    PayloadField to payload,
                    UpdatedAtField to now,
                )
                val publicMap: Map<String, Any?> = mapOf(
                    SchemaField to ProfileCardDocCodec.SCHEMA_VERSION.toLong(),
                    OwnerUidField to userId,
                    PayloadField to payload,
                    UpdatedAtField to now,
                )
                if (priv.exists) update(privateRef, privateMap) else set(privateRef, privateMap)
                if (pub.exists) update(publicRef, publicMap) else set(publicRef, publicMap)
                null
            }
            Unit
        }
    }

    private companion object {
        const val PrivateCollection = "profile_cards_private"
        const val PublicCollection = "profile_cards_public"
        const val PayloadField = "payload"
        const val SchemaField = "schemaVersion"
        const val OwnerUidField = "ownerUid"
        const val UpdatedAtField = "updatedAt"
        const val LEGACY_SCHEMA = 1
        const val MaxPrivatePayload = 16000
        const val LogTag = "ProfileCard"
    }
}
