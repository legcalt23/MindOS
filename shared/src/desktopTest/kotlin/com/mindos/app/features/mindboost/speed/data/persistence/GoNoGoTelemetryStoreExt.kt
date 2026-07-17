package com.mindos.app.features.mindboost.speed.data.persistence

import com.mindos.app.features.mindboost.speed.domain.SpeedExerciseId
import com.mindos.app.features.mindboost.speed.domain.score.GoNoGoTelemetrySummaryV1
import kotlinx.serialization.encodeToString

/**
 * SYSTEM-1 slice 4B · F8 — E8's typed telemetry helpers are **TEST/DEBUG-ONLY** now.
 *
 * `GoNoGoTelemetrySummaryV1` stays as a legacy/non-official type (CEO ruling), but its productive WRITE path was
 * removed with the double-truth in slice 4B (the sole official cognitive history is `CognitiveSessionRun`). These
 * helpers were the "easy-to-reuse productive write helper" the CEO ordered off the production surface, so they now
 * live in `desktopTest`: production `:shared` (commonMain) can no longer reference `saveGoNoGoSummary` at all
 * (compile-time guarantee), and `E8ProductiveTelemetryAbsenceTest` guards the E8 productive path against re-adding
 * any telemetry write. They remain here only to keep the legacy round-trip covered by
 * [GoNoGoTelemetryPersistenceTest] (same package). A corrupt JSON / non-E8 `exerciseId` never throws (→ null).
 */

/** Persiste un summary de E8 (idempotente por clave `userId::sessionId`: re-guardar sobrescribe, no duplica). */
suspend fun ExerciseTelemetryStore.saveGoNoGoSummary(
    userId: String,
    summary: GoNoGoTelemetrySummaryV1,
    createdAtEpochMs: Long = summary.createdAtEpochMs,
) {
    save(
        ExerciseTelemetryRecord(
            sessionId = summary.sessionId,
            userId = userId,
            exerciseId = summary.exerciseId,
            // telemetryVersion = FORMATO/ESQUEMA del summary (summary.summaryVersion), NO las fórmulas del mapper
            // (esas viven en summary.metricsVersion, persistidas dentro del JSON del summary).
            telemetryVersion = summary.summaryVersion,
            summaryJson = SpeedJson.instance.encodeToString(summary),
            createdAtEpochMs = createdAtEpochMs,
        ),
    )
}

/** Summary tipado de E8 para [sessionId] de [userId], o `null` si no existe / no es E8 / JSON corrupto. */
suspend fun ExerciseTelemetryStore.goNoGoSummaryForSession(
    userId: String,
    sessionId: String,
): GoNoGoTelemetrySummaryV1? = forSession(userId, sessionId)?.let(::decodeGoNoGoSummary)

/** Historial tipado de summaries de E8 de [userId], reciente primero; descarta records no-E8 o corruptos. */
suspend fun ExerciseTelemetryStore.goNoGoSummariesForUser(
    userId: String,
): List<GoNoGoTelemetrySummaryV1> = forUser(userId).mapNotNull(::decodeGoNoGoSummary)

/** Decodifica un record a summary de E8, o `null` si no es E8 (mismatch de ejercicio) o el JSON está corrupto. */
private fun decodeGoNoGoSummary(record: ExerciseTelemetryRecord): GoNoGoTelemetrySummaryV1? {
    if (record.exerciseId != SpeedExerciseId.E8_INHIBICION.code) return null
    // decodeFromString es CPU-puro (no suspend): runCatching no puede tragar una cancelación aquí.
    return runCatching {
        SpeedJson.instance.decodeFromString<GoNoGoTelemetrySummaryV1>(record.summaryJson)
    }.getOrNull()
}
