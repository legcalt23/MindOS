package com.mindos.app.features.mindboost.focus.data.sync

import com.mindos.app.core.network.withFirestoreReadTimeout
import com.mindos.app.core.network.withFirestoreWriteTimeout
import com.mindos.app.features.mindboost.core.sync.RawWriteOutcome
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FirebaseFirestoreException
import dev.gitlive.firebase.firestore.FirestoreExceptionCode
import dev.gitlive.firebase.firestore.code
import dev.gitlive.firebase.firestore.firestore
import kotlin.coroutines.cancellation.CancellationException

/**
 * REAL gitlive Firestore gateway for `users/{userId}/sessions/{sessionId}`.
 *
 * Write is **check-then-create** (`get` → if absent `set`); the security rule `allow update: if false` is the
 * authoritative server-side overwrite guard, so a lost check-then-create race degrades to a denied update
 * (`PERMISSION_DENIED`), never a silent overwrite. SDK errors are mapped to [RawWriteOutcome]; a
 * `PERMISSION_DENIED`/`UNAUTHENTICATED` is never success. `CancellationException` propagates.
 *
 * **EMULATOR-GATED — the only file in this pilot whose gitlive calls are NOT unit-verified in this environment
 * (no Gradle/emulator here).** It must pass `:androidApp:compileDebugKotlin` and be behaviour-verified against
 * the Firestore emulator (JDK 21) before the coordinator is mounted. Phase 1.1 does NOT wire it into production;
 * auth ownership is enforced upstream (coordinator `currentUserId` + mapper owner check) and by the rules.
 */
class GitLiveFocusSessionFirestoreGateway(
    private val firestore: FirebaseFirestore = Firebase.firestore,
) : FocusSessionFirestoreGateway {

    override suspend fun createSession(dto: FocusSessionCloudDto): RawWriteOutcome = try {
        val reference = documentRef(dto.userId, dto.sessionId)
        val existing = withFirestoreReadTimeout { reference.get() }
        if (existing.exists) {
            RawWriteOutcome.ALREADY_EXISTS
        } else {
            withFirestoreWriteTimeout { reference.set(dto) }
            RawWriteOutcome.SUCCESS
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (firestoreError: FirebaseFirestoreException) {
        firestoreError.code.toRawWriteOutcome()
    } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") error: Throwable) {
        RawWriteOutcome.UNKNOWN
    }

    override suspend fun readSession(userId: String, sessionId: String): FirestoreSessionRead = try {
        val snapshot = withFirestoreReadTimeout { documentRef(userId, sessionId).get() }
        if (!snapshot.exists) {
            FirestoreSessionRead.NotFound
        } else {
            val remote = snapshot.data<FocusSessionCloudDto>()
            FirestoreSessionRead.Found(remote.userId, FocusSessionCloudMapper.fingerprintOf(remote))
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") error: Throwable) {
        // Permission/network/corrupt — cannot verify, so never treat as a clean duplicate.
        FirestoreSessionRead.Unreadable
    }

    private fun documentRef(userId: String, sessionId: String) =
        firestore.collection(USERS).document(userId).collection(SESSIONS).document(sessionId)

    private companion object {
        const val USERS = "users"
        const val SESSIONS = "sessions"
    }
}

/** Maps a gitlive [FirestoreExceptionCode] to a fail-closed [RawWriteOutcome]. Denials are never success. */
private fun FirestoreExceptionCode.toRawWriteOutcome(): RawWriteOutcome = when (this) {
    FirestoreExceptionCode.ALREADY_EXISTS -> RawWriteOutcome.ALREADY_EXISTS
    FirestoreExceptionCode.PERMISSION_DENIED,
    FirestoreExceptionCode.UNAUTHENTICATED,
    -> RawWriteOutcome.PERMISSION_DENIED
    FirestoreExceptionCode.UNAVAILABLE,
    FirestoreExceptionCode.DEADLINE_EXCEEDED,
    -> RawWriteOutcome.NETWORK_ERROR
    FirestoreExceptionCode.INVALID_ARGUMENT,
    FirestoreExceptionCode.FAILED_PRECONDITION,
    -> RawWriteOutcome.INVALID_ARGUMENT
    else -> RawWriteOutcome.UNKNOWN
}
