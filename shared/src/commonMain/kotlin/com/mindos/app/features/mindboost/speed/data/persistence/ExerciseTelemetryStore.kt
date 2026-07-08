package com.mindos.app.features.mindboost.speed.data.persistence

import com.mindos.app.features.mindboost.focus.data.local.SecureKeyValueStore
import com.mindos.app.features.mindboost.focus.data.local.readParsed
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Versión del ESQUEMA del record genérico (envoltura). Distinta de `telemetryVersion` (formato del summary). */
const val EXERCISE_TELEMETRY_RECORD_VERSION: String = "1.0.0-mc9c-r3"

/**
 * MC9-C-R3 — estado de sincronización local de un [ExerciseTelemetryRecord]. Hoy TODO record es
 * [LOCAL_ONLY]: no existe sync a Firestore. [PENDING_SYNC]/[SYNCED]/[SYNC_FAILED] son CONTRATO FUTURO
 * (la capa de sync/retry aún no está implementada) — se dejan modelados para no re-versionar el esquema
 * cuando llegue.
 */
@Serializable
enum class TelemetrySyncStatus {
    LOCAL_ONLY,
    PENDING_SYNC,
    SYNCED,
    SYNC_FAILED,
}

/**
 * MC9-C-R2/R3 — registro histórico de telemetría de un ejercicio por sesión. GENÉRICO: guarda el summary
 * específico del ejercicio como JSON (`summaryJson`) para no acoplar el store a un ejercicio ni migrar el
 * esquema de `TrialRecord`/`MeasurementRun`.
 *
 * Versiones (ortogonales, no confundir):
 * - [schemaVersion]: versión del ESQUEMA de este record genérico (envoltura).
 * - `telemetryVersion`: versión del FORMATO del summary específico del ejercicio (payload).
 * - (dentro del summary) `summaryVersion`/`metricsVersion`: esquema E5 y versión de fórmulas del mapper.
 */
@Serializable
data class ExerciseTelemetryRecord(
    val sessionId: String,
    val userId: String,
    val exerciseId: String,
    val telemetryVersion: String,
    val summaryJson: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long = createdAtEpochMs,
    val syncStatus: TelemetrySyncStatus = TelemetrySyncStatus.LOCAL_ONLY,
    val schemaVersion: String = EXERCISE_TELEMETRY_RECORD_VERSION,
)

/**
 * Almacén local de telemetría por-ejercicio. La impl durable vive sobre el MISMO `SecureKeyValueStore`
 * (namespace `mindboost_speed`, ya cubierto por el borrado de cuenta), con prefijo de clave `tele:` — no toca
 * el pipeline de `MeasurementRun` (prefijos `run:`/`dp:`).
 *
 * PRIVACIDAD (MC9-C-R3): [forSession] exige el `userId` y lee una clave EXACTA — nunca escanea otros usuarios
 * ni puede devolver el registro de otro usuario. [forUser] devuelve orden determinista (reciente primero).
 * La idempotencia es por clave exacta (`userId::sessionId`): re-guardar la misma sesión sobrescribe, no duplica.
 */
interface ExerciseTelemetryStore {
    suspend fun save(record: ExerciseTelemetryRecord)

    /** Registro de [sessionId] PERTENECIENTE a [userId] (clave exacta), o `null`. Nunca cruza usuarios. */
    suspend fun forSession(userId: String, sessionId: String): ExerciseTelemetryRecord?

    /** Historial de [userId], reciente primero (`createdAtEpochMs` desc, desempate por `sessionId`). */
    suspend fun forUser(userId: String): List<ExerciseTelemetryRecord>

    /** Borra la telemetría de [userId] (espejo del borrado por usuario del store de medición). Devuelve el conteo. */
    suspend fun deleteUserData(userId: String): Int
}

/** Orden histórico determinista: reciente primero, desempate estable por sessionId (§3). */
private fun List<ExerciseTelemetryRecord>.mostRecentFirst(): List<ExerciseTelemetryRecord> =
    sortedWith(compareByDescending<ExerciseTelemetryRecord> { it.createdAtEpochMs }.thenBy { it.sessionId })

/** Impl durable y cifrada, sobre el `SecureKeyValueStore` del namespace de Velocidad, prefijo `tele:`. */
class SecureExerciseTelemetryStore(
    private val store: SecureKeyValueStore,
    private val json: Json = SpeedJson.instance,
) : ExerciseTelemetryStore {

    private val mutex = Mutex()

    override suspend fun save(record: ExerciseTelemetryRecord) = mutex.withLock {
        store.put(key(record.userId, record.sessionId), json.encodeToString(record))
    }

    override suspend fun forSession(userId: String, sessionId: String): ExerciseTelemetryRecord? =
        read(key(userId, sessionId))

    override suspend fun forUser(userId: String): List<ExerciseTelemetryRecord> =
        store.keys(userPrefix(userId)).mapNotNull { read(it) }.mostRecentFirst()

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

    override suspend fun forSession(userId: String, sessionId: String): ExerciseTelemetryRecord? =
        records[key(userId, sessionId)]

    override suspend fun forUser(userId: String): List<ExerciseTelemetryRecord> =
        records.values.filter { it.userId == userId }.mostRecentFirst()

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
