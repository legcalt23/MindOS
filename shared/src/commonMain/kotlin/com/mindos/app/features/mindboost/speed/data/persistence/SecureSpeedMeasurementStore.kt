// Implementa el contrato relacional completo sobre un KV cifrado: superficie amplia y un
// deleteUserData necesariamente denso (multi-clave + limpieza de huérfanos atómica).
@file:Suppress("TooManyFunctions", "LongMethod", "CyclomaticComplexMethod")

package com.mindos.app.features.mindboost.speed.data.persistence

import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readParsed
import com.mindos.app.features.mindboost.focus.data.local.readVersioned
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implementación durable y cifrada en reposo del [SpeedMeasurementStore], sobre el mismo
 * [SecureKeyValueStore] (AES-256-GCM + Android Keystore) ya usado por MindBoost Enfoque.
 * Reusa cripto vetada — no se inventa nada (regla de proyecto, CLAUDE.md 2026-06-05).
 *
 * Disposición de claves dentro del namespace:
 * - `dp:<deviceProfileId>` → [DeviceProfile] (compartible, fuera del scope de usuario).
 * - `run:<userId>::<sessionId>` → [PersistedRunDto] (corrida completa; cascada implícita).
 *
 * El separador `::` desambigua `userId` de `sessionId` aunque el `sessionId` contenga `:`.
 * El [Mutex] hace atómico el borrado multi-clave + limpieza de huérfanos (§16).
 */
class SecureSpeedMeasurementStore(
    private val store: SecureKeyValueStore,
    private val json: Json = SpeedJson.instance,
) : SpeedMeasurementStore {

    private val mutex = Mutex()

    override suspend fun saveRun(run: MeasurementRun) = mutex.withLock {
        store.put(deviceKey(run.deviceProfile.id), json.encodeToString(run.deviceProfile))
        store.put(runKey(run.session.userId, run.session.id), json.encodeToString(PersistedRunDto(run = run)))
    }

    override suspend fun deviceProfile(id: String): DeviceProfile? =
        readUnversioned(deviceKey(id))

    override suspend fun allDeviceProfiles(): List<DeviceProfile> =
        store.keys(DEVICE_PREFIX).mapNotNull { key -> readUnversioned<DeviceProfile>(key) }

    override suspend fun session(sessionId: String): SessionRecord? = loadRun(sessionId)?.session

    override suspend fun sessionsForUser(userId: String): List<SessionRecord> =
        loadRunsForUser(userId).map { it.session }

    override suspend fun blocksForSession(sessionId: String): List<BlockRecord> =
        loadRun(sessionId)?.blocks?.sortedBy { it.blockIndex } ?: emptyList()

    override suspend fun trialsForBlock(blockId: String): List<TrialRecord> {
        val run = allRuns().firstOrNull { run -> run.blocks.any { it.id == blockId } } ?: return emptyList()
        return run.trials.filter { it.blockId == blockId }.sortedBy { it.trialIndex }
    }

    override suspend fun blockMetricsForSession(sessionId: String): List<BlockMetrics> =
        loadRun(sessionId)?.blockMetrics ?: emptyList()

    override suspend fun sessionMetrics(sessionId: String): SessionMetrics? = loadRun(sessionId)?.sessionMetrics

    override suspend fun versionSnapshot(sessionId: String): VersionSnapshot? = loadRun(sessionId)?.versionSnapshot

    override suspend fun qualityLogs(sessionId: String): List<QualityDecisionLog> =
        loadRun(sessionId)?.qualityLogs ?: emptyList()

    override suspend fun loadRun(sessionId: String): MeasurementRun? {
        val key = store.keys(RUN_PREFIX).firstOrNull { it.substringAfter(SEP) == sessionId } ?: return null
        return readRun(key)
    }

    override suspend fun deleteUserData(userId: String): DeletionReport = mutex.withLock {
        val userKeys = store.keys(runUserPrefix(userId))
        var sessions = 0
        var blocks = 0
        var trials = 0
        var blockMetrics = 0
        var sessionMetricsCount = 0
        var versionSnapshots = 0
        var qualityLogs = 0
        for (key in userKeys) {
            val run = readRun(key)
            if (run != null) {
                sessions += 1
                blocks += run.blocks.size
                trials += run.trials.size
                blockMetrics += run.blockMetrics.size
                sessionMetricsCount += if (run.sessionMetrics != null) 1 else 0
                versionSnapshots += 1
                qualityLogs += run.qualityLogs.size
            }
            store.remove(key)
        }

        // Limpieza de DeviceProfile huérfanos: recolecta los ids aún referenciados por
        // cualquier corrida restante y borra los `dp:` que no aparezcan.
        val referenced = allRuns().map { it.deviceProfile.id }.toSet()
        var orphans = 0
        for (deviceKey in store.keys(DEVICE_PREFIX)) {
            val id = deviceKey.removePrefix(DEVICE_PREFIX)
            if (id !in referenced) {
                store.remove(deviceKey)
                orphans += 1
            }
        }

        DeletionReport(
            sessions = sessions,
            blocks = blocks,
            trials = trials,
            blockMetrics = blockMetrics,
            sessionMetrics = sessionMetricsCount,
            versionSnapshots = versionSnapshots,
            qualityLogs = qualityLogs,
            orphanDeviceProfiles = orphans,
        )
    }

    private suspend fun allRuns(): List<MeasurementRun> =
        store.keys(RUN_PREFIX).mapNotNull { key -> readRun(key) }

    private suspend fun loadRunsForUser(userId: String): List<MeasurementRun> =
        store.keys(runUserPrefix(userId)).mapNotNull { key -> readRun(key) }

    private suspend inline fun <reified T> readUnversioned(key: String): T? =
        store.readParsed(key) { raw -> json.decodeFromString<T>(raw) }

    private suspend fun readRun(key: String): MeasurementRun? =
        store.readVersioned(
            key = key,
            currentVersion = PersistedRunDto.CURRENT_SCHEMA_VERSION,
            parser = { raw -> json.decodeFromString<PersistedRunDto>(raw) },
            versionOf = PersistedRunDto::schemaVersion,
        )?.run

    private fun deviceKey(id: String): String = "$DEVICE_PREFIX$id"
    private fun runUserPrefix(userId: String): String = "$RUN_PREFIX$userId$SEP"
    private fun runKey(userId: String, sessionId: String): String = "$RUN_PREFIX$userId$SEP$sessionId"

    private companion object {
        const val DEVICE_PREFIX = "dp:"
        const val RUN_PREFIX = "run:"
        const val SEP = "::"
    }
}
