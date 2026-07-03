// MeasurementRun/DeletionReport agregan muchos campos; el contrato del store cubre todas
// las entidades del §5 (superficie amplia por diseño).
@file:Suppress("LongParameterList", "TooManyFunctions")

package com.mindos.app.features.mindboost.speed.data.persistence

import com.mindos.app.features.mindboost.speed.domain.model.BlockMetrics
import com.mindos.app.features.mindboost.speed.domain.model.BlockRecord
import com.mindos.app.features.mindboost.speed.domain.model.DeviceProfile
import com.mindos.app.features.mindboost.speed.domain.model.QualityDecisionLog
import com.mindos.app.features.mindboost.speed.domain.model.SessionMetrics
import com.mindos.app.features.mindboost.speed.domain.model.SessionRecord
import com.mindos.app.features.mindboost.speed.domain.model.TrialRecord
import com.mindos.app.features.mindboost.speed.domain.model.VersionSnapshot
import kotlinx.serialization.Serializable

/**
 * Una corrida de medición completa: la unidad natural y atómica de persistencia de
 * MVP-0 (estímulo → respuesta → timestamp → trial → bloque → métricas → calidad).
 * El `deviceProfile` PUEDE compartirse entre corridas/usuarios (mismo dispositivo).
 */
@Serializable
data class MeasurementRun(
    val session: SessionRecord,
    val deviceProfile: DeviceProfile,
    val versionSnapshot: VersionSnapshot,
    val blocks: List<BlockRecord>,
    val trials: List<TrialRecord>,
    val blockMetrics: List<BlockMetrics>,
    val sessionMetrics: SessionMetrics?,
    val qualityLogs: List<QualityDecisionLog>,
)

/** Recuento del borrado transaccional por usuario (§16, acceptance #20/#21). */
data class DeletionReport(
    val sessions: Int,
    val blocks: Int,
    val trials: Int,
    val blockMetrics: Int,
    val sessionMetrics: Int,
    val versionSnapshots: Int,
    val qualityLogs: Int,
    val orphanDeviceProfiles: Int,
) {
    val totalRows: Int
        get() = sessions + blocks + trials + blockMetrics + sessionMetrics +
            versionSnapshots + qualityLogs + orphanDeviceProfiles
}

/**
 * Almacén local de medición de Velocidad Mental. Preserva las garantías relacionales del
 * §5/§16 (referencias por id, cascada, borrado transaccional por usuario, limpieza de
 * `DeviceProfile` huérfanos, versionado) SIN un motor SQL — este repo no usa Room.
 *
 * Dos implementaciones:
 * - [InMemorySpeedMeasurementStore]: modelo semántico autoritativo (tablas en memoria con
 *   cascada y huérfanos); base de las pruebas y del simulador (§12).
 * - [SecureSpeedMeasurementStore]: durable y cifrado en reposo (Android Keystore) para la
 *   app real.
 */
interface SpeedMeasurementStore {

    /** Persiste una corrida completa (cascada implícita: los hijos viajan dentro). */
    suspend fun saveRun(run: MeasurementRun)

    suspend fun deviceProfile(id: String): DeviceProfile?
    suspend fun allDeviceProfiles(): List<DeviceProfile>
    suspend fun session(sessionId: String): SessionRecord?
    suspend fun sessionsForUser(userId: String): List<SessionRecord>
    suspend fun blocksForSession(sessionId: String): List<BlockRecord>
    suspend fun trialsForBlock(blockId: String): List<TrialRecord>
    suspend fun blockMetricsForSession(sessionId: String): List<BlockMetrics>
    suspend fun sessionMetrics(sessionId: String): SessionMetrics?
    suspend fun versionSnapshot(sessionId: String): VersionSnapshot?
    suspend fun qualityLogs(sessionId: String): List<QualityDecisionLog>

    /**
     * Reconstruye una corrida completa para exportación/depuración. Null si la corrida no
     * existe.
     */
    suspend fun loadRun(sessionId: String): MeasurementRun?

    /**
     * Borrado atómico y transaccional de TODOS los datos de [userId] (§16): logs de
     * calidad, snapshots de versión, métricas de bloque, métricas de sesión, trials,
     * bloques y sesiones — y finalmente limpieza de `DeviceProfile` huérfanos (no borra
     * perfiles compartidos aún referenciados). Datos de otros usuarios intactos.
     */
    suspend fun deleteUserData(userId: String): DeletionReport
}
