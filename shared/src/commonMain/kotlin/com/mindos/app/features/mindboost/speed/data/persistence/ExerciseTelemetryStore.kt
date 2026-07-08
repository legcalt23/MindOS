package com.mindos.app.features.mindboost.speed.data.persistence

import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readParsed
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * MC9-C-R2 — registro histórico de telemetría de un ejercicio por sesión. GENÉRICO: guarda el summary
 * específico del ejercicio como JSON (`summaryJson`) para no acoplar el store a un ejercicio ni migrar el
 * esquema de `TrialRecord`/`MeasurementRun`. `telemetryVersion` versiona el formato del summary.
 */
@Serializable
data class ExerciseTelemetryRecord(
    val sessionId: String,
    val userId: String,
    val exerciseId: String,
    val telemetryVersion: String,
    val summaryJson: String,
    val createdAtEpochMs: Long,
)

/**
 * Almacén local de telemetría por-ejercicio. La impl durable vive sobre el MISMO `SecureKeyValueStore`
 * (namespace `mindboost_speed`, ya cubierto por el borrado de cuenta), con prefijo de clave `tele:` — no toca
 * el pipeline de `MeasurementRun` (prefijos `run:`/`dp:`).
 */
interface ExerciseTelemetryStore {
    suspend fun save(record: ExerciseTelemetryRecord)
    suspend fun forSession(sessionId: String): ExerciseTelemetryRecord?
    suspend fun forUser(userId: String): List<ExerciseTelemetryRecord>

    /** Borra la telemetría de [userId] (espejo del borrado por usuario del store de medición). Devuelve el conteo. */
    suspend fun deleteUserData(userId: String): Int
}

/** Impl durable y cifrada, sobre el `SecureKeyValueStore` del namespace de Velocidad, prefijo `tele:`. */
class SecureExerciseTelemetryStore(
    private val store: SecureKeyValueStore,
    private val json: Json = SpeedJson.instance,
) : ExerciseTelemetryStore {

    private val mutex = Mutex()

    override suspend fun save(record: ExerciseTelemetryRecord) = mutex.withLock {
        store.put(key(record.userId, record.sessionId), json.encodeToString(record))
    }

    override suspend fun forSession(sessionId: String): ExerciseTelemetryRecord? =
        store.keys(PREFIX).firstOrNull { it.substringAfter(SEP) == sessionId }?.let { read(it) }

    override suspend fun forUser(userId: String): List<ExerciseTelemetryRecord> =
        store.keys(userPrefix(userId)).mapNotNull { read(it) }

    override suspend fun deleteUserData(userId: String): Int = mutex.withLock {
        val keys = store.keys(userPrefix(userId))
        keys.forEach { store.remove(it) }
        keys.size
    }

    private suspend fun read(key: String): ExerciseTelemetryRecord? =
        store.readParsed(key) { raw -> json.decodeFromString<ExerciseTelemetryRecord>(raw) }

    private fun key(userId: String, sessionId: String): String = "$PREFIX$userId$SEP$sessionId"
    private fun userPrefix(userId: String): String = "$PREFIX$userId$SEP"

    private companion object {
        const val PREFIX = "tele:"
        const val SEP = "::"
    }
}

/** Impl in-memory para tests y desktop/iOS (no escribe a disco). */
class InMemoryExerciseTelemetryStore : ExerciseTelemetryStore {

    private val records = mutableMapOf<String, ExerciseTelemetryRecord>()
    private val mutex = Mutex()

    override suspend fun save(record: ExerciseTelemetryRecord) = mutex.withLock {
        records[key(record.userId, record.sessionId)] = record
        Unit
    }

    override suspend fun forSession(sessionId: String): ExerciseTelemetryRecord? =
        records.values.firstOrNull { it.sessionId == sessionId }

    override suspend fun forUser(userId: String): List<ExerciseTelemetryRecord> =
        records.values.filter { it.userId == userId }

    override suspend fun deleteUserData(userId: String): Int = mutex.withLock {
        val toRemove = records.keys.filter { it.startsWith("$userId$SEP") }
        toRemove.forEach { records.remove(it) }
        toRemove.size
    }

    private fun key(userId: String, sessionId: String): String = "$userId$SEP$sessionId"

    private companion object {
        const val SEP = "::"
    }
}
