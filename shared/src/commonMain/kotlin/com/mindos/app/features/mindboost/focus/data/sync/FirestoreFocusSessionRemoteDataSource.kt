package com.mindos.app.features.mindboost.focus.data.sync

import com.mindos.app.features.mindboost.core.sync.RawWriteOutcome
import com.mindos.app.features.mindboost.core.sync.RemoteSessionProbe

/**
 * Pure [FocusSessionRemoteDataSource] over a [FocusSessionFirestoreGateway]. This is the production data source
 * for the Enfoque pilot; all Firestore specifics live behind the gateway, so this class is fully unit-testable
 * with a fake gateway (the real gitlive gateway is emulator-gated).
 *
 * `create` delegates to the gateway. `probe` maps the verified read to a [RemoteSessionProbe]: only a
 * [FirestoreSessionRead.Found] yields a probe; [FirestoreSessionRead.NotFound] and
 * [FirestoreSessionRead.Unreadable] both yield null, so the classifier fails closed to
 * `Conflict(UNVERIFIED_EXISTING)` rather than a silent duplicate.
 */
class FirestoreFocusSessionRemoteDataSource(
    private val gateway: FocusSessionFirestoreGateway,
) : FocusSessionRemoteDataSource {

    override suspend fun create(dto: FocusSessionCloudDto): RawWriteOutcome = gateway.createSession(dto)

    override suspend fun probe(userId: String, sessionId: String): RemoteSessionProbe? =
        when (val read = gateway.readSession(userId, sessionId)) {
            is FirestoreSessionRead.Found -> RemoteSessionProbe(
                exists = true,
                userId = read.userId,
                fingerprint = read.fingerprint,
            )
            FirestoreSessionRead.NotFound, FirestoreSessionRead.Unreadable -> null
        }
}
