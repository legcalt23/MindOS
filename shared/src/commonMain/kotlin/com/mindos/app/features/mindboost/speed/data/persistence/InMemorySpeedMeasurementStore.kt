// El store modela el contrato relacional completo; su superficie de funciones es amplia
// por diseño (una "tabla" + consultas por entidad). Se suprime TooManyFunctions.
@file:Suppress("TooManyFunctions")

package com.mindos.app.features.mindboost.speed.data.persistence

import com.mindos.app.features.mindboost.speed.domain.model.BlockMetrics
import com.mindos.app.features.mindboost.speed.domain.model.BlockRecord
import com.mindos.app.features.mindboost.speed.domain.model.DeviceProfile
import com.mindos.app.features.mindboost.speed.domain.model.QualityDecisionLog
import com.mindos.app.features.mindboost.speed.domain.model.SessionMetrics
import com.mindos.app.features.mindboost.speed.domain.model.SessionRecord
import com.mindos.app.features.mindboost.speed.domain.model.TrialRecord
import com.mindos.app.features.mindboost.speed.domain.model.VersionSnapshot
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Modelo semántico autoritativo del almacén: una "tabla" (Map) por entidad, con cascada
 * y limpieza de huérfanos implementadas explícitamente. Es lo que ejercitan TODAS las
 * pruebas y el simulador (§12), de modo que las garantías del §16 se validan en la JVM de
 * escritorio sin dispositivo (acceptance #20/#21/#23). Thread-safe vía [Mutex].
 *
 * No es durable (vive en el proceso). La durabilidad cifrada la aporta
 * [SecureSpeedMeasurementStore], que satisface el MISMO contrato.
 */
class InMemorySpeedMeasurementStore : SpeedMeasurementStore {

    private val mutex = Mutex()

    // "Tablas". La clave es el id natural de cada entidad.
    private val deviceProfiles = linkedMapOf<String, DeviceProfile>()
    private val sessions = linkedMapOf<String, SessionRecord>()
    private val blocks = linkedMapOf<String, BlockRecord>()
    private val trials = linkedMapOf<String, TrialRecord>()
    private val blockMetrics = linkedMapOf<String, BlockMetrics>() // key = blockId
    private val sessionMetrics = linkedMapOf<String, SessionMetrics>() // key = sessionId
    private val versionSnapshots = linkedMapOf<String, VersionSnapshot>() // key = sessionId
    private val qualityLogs = linkedMapOf<String, QualityDecisionLog>()

    override suspend fun saveRun(run: MeasurementRun) = mutex.withLock {
        deviceProfiles[run.deviceProfile.id] = run.deviceProfile
        sessions[run.session.id] = run.session
        versionSnapshots[run.versionSnapshot.sessionId] = run.versionSnapshot
        run.blocks.forEach { blocks[it.id] = it }
        run.trials.forEach { trials[it.id] = it }
        run.blockMetrics.forEach { blockMetrics[it.blockId] = it }
        run.sessionMetrics?.let { sessionMetrics[it.sessionId] = it }
        run.qualityLogs.forEach { qualityLogs[it.id] = it }
    }

    override suspend fun deviceProfile(id: String): DeviceProfile? = mutex.withLock { deviceProfiles[id] }

    override suspend fun allDeviceProfiles(): List<DeviceProfile> = mutex.withLock { deviceProfiles.values.toList() }

    override suspend fun session(sessionId: String): SessionRecord? = mutex.withLock { sessions[sessionId] }

    override suspend fun sessionsForUser(userId: String): List<SessionRecord> =
        mutex.withLock { sessions.values.filter { it.userId == userId } }

    override suspend fun blocksForSession(sessionId: String): List<BlockRecord> =
        mutex.withLock { blocks.values.filter { it.sessionId == sessionId }.sortedBy { it.blockIndex } }

    override suspend fun trialsForBlock(blockId: String): List<TrialRecord> =
        mutex.withLock { trials.values.filter { it.blockId == blockId }.sortedBy { it.trialIndex } }

    override suspend fun blockMetricsForSession(sessionId: String): List<BlockMetrics> = mutex.withLock {
        val blockIds = blocks.values.filter { it.sessionId == sessionId }.map { it.id }.toSet()
        blockMetrics.values.filter { it.blockId in blockIds }
    }

    override suspend fun sessionMetrics(sessionId: String): SessionMetrics? =
        mutex.withLock { sessionMetrics[sessionId] }

    override suspend fun versionSnapshot(sessionId: String): VersionSnapshot? =
        mutex.withLock { versionSnapshots[sessionId] }

    override suspend fun qualityLogs(sessionId: String): List<QualityDecisionLog> =
        mutex.withLock { qualityLogs.values.filter { it.sessionId == sessionId } }

    override suspend fun loadRun(sessionId: String): MeasurementRun? = mutex.withLock {
        val session = sessions[sessionId] ?: return@withLock null
        val device = deviceProfiles[session.deviceProfileId] ?: return@withLock null
        val version = versionSnapshots[sessionId] ?: return@withLock null
        val sessionBlocks = blocks.values.filter { it.sessionId == sessionId }.sortedBy { it.blockIndex }
        val blockIds = sessionBlocks.map { it.id }.toSet()
        MeasurementRun(
            session = session,
            deviceProfile = device,
            versionSnapshot = version,
            blocks = sessionBlocks,
            trials = trials.values.filter { it.blockId in blockIds }.sortedWith(
                compareBy({ it.blockId }, { it.trialIndex }),
            ),
            blockMetrics = blockMetrics.values.filter { it.blockId in blockIds },
            sessionMetrics = sessionMetrics[sessionId],
            qualityLogs = qualityLogs.values.filter { it.sessionId == sessionId },
        )
    }

    override suspend fun deleteUserData(userId: String): DeletionReport = mutex.withLock {
        // Transaccional: todo el cálculo + mutación ocurre bajo un único lock.
        val userSessionIds = sessions.values.filter { it.userId == userId }.map { it.id }.toSet()
        val userBlockIds = blocks.values.filter { it.sessionId in userSessionIds }.map { it.id }.toSet()

        // Orden de cascada del §16 (hijos antes que padres).
        val deletedQualityLogs = removeWhere(qualityLogs) { it.sessionId in userSessionIds }
        val deletedVersionSnapshots = removeWhere(versionSnapshots) { it.sessionId in userSessionIds }
        val deletedBlockMetrics = removeWhere(blockMetrics) { it.blockId in userBlockIds }
        val deletedSessionMetrics = removeWhere(sessionMetrics) { it.sessionId in userSessionIds }
        val deletedTrials = removeWhere(trials) { it.blockId in userBlockIds }
        val deletedBlocks = removeWhere(blocks) { it.sessionId in userSessionIds }
        val deletedSessions = removeWhere(sessions) { it.userId == userId }

        // Limpieza de DeviceProfile huérfanos: solo los que ya no referencia NINGUNA sesión.
        val referencedDeviceIds = sessions.values.map { it.deviceProfileId }.toSet()
        val deletedOrphans = removeWhere(deviceProfiles) { it.id !in referencedDeviceIds }

        DeletionReport(
            sessions = deletedSessions,
            blocks = deletedBlocks,
            trials = deletedTrials,
            blockMetrics = deletedBlockMetrics,
            sessionMetrics = deletedSessionMetrics,
            versionSnapshots = deletedVersionSnapshots,
            qualityLogs = deletedQualityLogs,
            orphanDeviceProfiles = deletedOrphans,
        )
    }

    /** Elimina del mapa toda entrada cuyo valor cumpla [predicate]; devuelve cuántas. */
    private fun <V> removeWhere(map: MutableMap<String, V>, predicate: (V) -> Boolean): Int {
        val keys = map.entries.filter { predicate(it.value) }.map { it.key }
        keys.forEach { map.remove(it) }
        return keys.size
    }
}
