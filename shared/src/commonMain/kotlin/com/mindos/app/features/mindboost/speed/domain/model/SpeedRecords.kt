// Los records son DTOs planos del §5; sus constructores con muchos campos son la forma
// idiomática del modelo de datos (no un code smell). Se suprime LongParameterList.
@file:Suppress("LongParameterList")

package com.mindos.app.features.mindboost.speed.domain.model

import com.mindos.app.features.mindboost.speed.domain.DeviceTimingRisk
import com.mindos.app.features.mindboost.speed.domain.InputTimestampPrecision
import com.mindos.app.features.mindboost.speed.domain.SessionQuality
import com.mindos.app.features.mindboost.speed.domain.TimestampSource
import com.mindos.app.features.mindboost.speed.domain.Timebase
import kotlinx.serialization.Serializable

/**
 * Modelos de datos atómicos de MVP-0 (§5).
 *
 * Equivalen 1:1 a las entidades del plan. El plan las describe como Room entities;
 * este repo NO usa Room (sin Room/KSP/SQLDelight) y su patrón establecido para datos
 * cognitivos sensibles es `@Serializable` + `SecureKeyValueStore` cifrado + repos en
 * `commonMain` testeables con fakes (regla de proyecto en CLAUDE.md; feature Enfoque).
 * Se preservan TODAS las garantías relacionales del §5/§16 (referencias por id,
 * cascada, borrado transaccional por usuario, limpieza de huérfanos, versionado) en la
 * capa de persistencia (`data/persistence`), no en un motor SQL.
 *
 * Jerarquía de referencias (FK lógicas):
 * DeviceProfile ◄─ SessionRecord ◄─ BlockRecord ◄─ TrialRecord
 *                              ▲            └─ BlockMetrics
 *                              ├─ SessionMetrics
 *                              ├─ VersionSnapshot
 *                              └─ QualityDecisionLog
 *
 * Toda métrica nula significa "no computable", NUNCA "valor bajo" (§7).
 */

/**
 * Perfil técnico del dispositivo. PUEDE compartirse entre usuarios (un mismo teléfono).
 * No se borra por `userId`; solo se limpian las instancias huérfanas (§16).
 */
@Serializable
data class DeviceProfile(
    val id: String,
    val deviceModel: String,
    val osVersion: String,
    val apiLevel: Int,
    val simpleReactionMedianMs: Double,
    val deviceTimingRisk: DeviceTimingRisk,
)

/**
 * Contenedor técnico de medición (Measurement Run), NO una sesión cognitiva de
 * entrenamiento (§2). Agrupa lógicamente bloques, trials y exportaciones.
 */
@Serializable
data class SessionRecord(
    val id: String,
    val userId: String,
    val timestampEpochMs: Long,
    val deviceProfileId: String,
    val durationMs: Long,
)

/** Un bloque de trials de dificultad fija dentro de una corrida (§5). */
@Serializable
data class BlockRecord(
    val id: String,
    val sessionId: String,
    val blockIndex: Int,
    val exerciseId: String,
    val startedAtEpochMs: Long,
    val durationMs: Long?,
    val fixedDifficulty: Double,
    val deadlineNanos: Long?,
    val timestampSource: TimestampSource,
    val inputTimestampPrecision: InputTimestampPrecision,
    val timebase: Timebase,
)

/**
 * Unidad atómica: estímulo → respuesta → timestamp → trial. Referencia a [BlockRecord]
 * por [blockId]. `rtNanos = responseUptimeNanos - stimulusUptimeNanos` (§4.1).
 */
@Serializable
data class TrialRecord(
    val id: String,
    val blockId: String,
    val trialIndex: Int,
    val exerciseId: String,
    val stimulusUptimeNanos: Long,
    val responseUptimeNanos: Long,
    val rtNanos: Long,
    val isCorrect: Boolean,
    val isAnticipation: Boolean,
    val isLapse: Boolean,
    val lapseMultiplier: Double,
    val deadlineNanos: Long?,
    val difficultyEstimate: Double,
    val timestampSource: TimestampSource,
    val inputTimestampPrecision: InputTimestampPrecision,
    /** Siempre ANDROID_UI_UPTIME_NANOS. */
    val timebase: Timebase,
)

/**
 * Métricas agregadas a nivel de bloque (§5, §7). Gobiernan la adaptación a partir de
 * MVP-1 (en MVP-0 solo se calculan y persisten). Nulos explícitos: nunca 0 silencioso.
 */
@Serializable
data class BlockMetrics(
    val blockId: String,
    val totalTrials: Int,
    val pc: Double,
    val rtMedianNanos: Double?,
    val rtMeanNanos: Double?,
    val cv: Double?,
    val lapseRate: Double,
    val anticipationRate: Double,
    val faRate: Double?,
    val omissionRate: Double,
    val impIdx: Double?,
    val fatIdx: Double?,
    /** Velocidad Cognitiva Efectiva — informativa, no autoritativa (§6). */
    val vce: Double?,
    val timestampSource: TimestampSource,
    val inputTimestampPrecision: InputTimestampPrecision,
    val timebase: Timebase,
)

/** Métricas agregadas a nivel de corrida completa (§5). */
@Serializable
data class SessionMetrics(
    val sessionId: String,
    val totalTrials: Int,
    val pc: Double,
    val rtMedianNanos: Double?,
    val rtMeanNanos: Double?,
    val cv: Double?,
    val lapseRate: Double,
    val anticipationRate: Double,
    val faRate: Double?,
    val omissionRate: Double,
    val impIdx: Double?,
    val fatIdx: Double?,
    val vce: Double?,
    val timestampSource: TimestampSource,
    val inputTimestampPrecision: InputTimestampPrecision,
    val timebase: Timebase,
)

/** Snapshot de versiones de parámetros activos en la corrida (§5, versionado). */
@Serializable
data class VersionSnapshot(
    val sessionId: String,
    val metricsVersion: String,
    val staircaseVersion: String,
    val difficultyModelVersion: String,
    val lapseMultiplier: Double,
)

/**
 * Traza auditable de la decisión de calidad de una corrida (§5, §8, acceptance #17).
 * Toda corrida `LOW`/`CONTAMINATED` debe tener motivos registrados.
 */
@Serializable
data class QualityDecisionLog(
    val id: String,
    val sessionId: String,
    val deviceTimingRisk: DeviceTimingRisk,
    val sessionQuality: SessionQuality,
    val reasons: List<String>,
    val createdAtEpochMs: Long,
)
