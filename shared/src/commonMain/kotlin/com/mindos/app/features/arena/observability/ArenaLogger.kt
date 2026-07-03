package com.mindos.app.features.arena.observability

import com.mindos.app.features.arena.rating.model.PlayerRatingState
import com.mindos.app.features.arena.sanitization.DataSanitizer

/**
 * Phase 07.5 / Task 9: Safe logger for the arena.
 *
 * - [log] emits only typed [NonFatalTelemetryEvent]s through the sink and returns their safe string
 *   form. There is intentionally no overload that accepts a raw [PlayerRatingState] for logging.
 * - [logRatingStateAsSafeEvent] projects a rating state down to a [NonFatalTelemetryEvent.RatingStateSnapshot]
 *   that contains ZERO rating values, so "passing a rating state to the logger" yields an obfuscated
 *   string (16_telemetry_redaction_plan.md AC #2).
 * - [safeLine] runs free text through the [DataSanitizer] before it can be emitted.
 */
class ArenaLogger(
    private val sink: ObservabilitySink,
    private val sanitizer: DataSanitizer,
) {

    fun log(event: NonFatalTelemetryEvent): String {
        sink.record(event)
        return format(event)
    }

    fun safeLine(text: String): String = sanitizer.scrub(text)

    fun logRatingStateAsSafeEvent(state: PlayerRatingState): String {
        val event = NonFatalTelemetryEvent.RatingStateSnapshot(
            integrityState = state.integrityState,
            activityState = state.activityState,
            verificationState = state.verificationState,
            provisional = state.provisionalState,
        )
        return log(event)
    }

    private fun format(event: NonFatalTelemetryEvent): String = when (event) {
        is NonFatalTelemetryEvent.AggregateMatchMetrics ->
            "${event.eventName.name}{hardwareClass=${event.hardwareClass}," +
                "averageMatchDurationMs=${event.averageMatchDurationMs}}"
        is NonFatalTelemetryEvent.RatingStateSnapshot ->
            "${event.eventName.name}{integrity=${event.integrityState.name}," +
                "activity=${event.activityState.name},verification=${event.verificationState.name}," +
                "provisional=${event.provisional}}"
        is NonFatalTelemetryEvent.SafeError ->
            "${event.eventName.name}{category=${event.category.name}}"
    }
}
