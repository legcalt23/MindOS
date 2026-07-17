package com.mindos.app.features.mindboost.profile.domain.calibration

/**
 * Decides whether a completed session may count as PROFILE EVIDENCE — feeding both the DC-3 cross-session
 * evidence ([CalibrationPolicy]) and the score/confidence inputs of a session-based source. This is an
 * EVIDENCE-QUALITY gate, NEVER a psychometric score.
 *
 * A session is eligible only if ALL hold:
 *  - a real end timestamp ([endedAtMs] > 0) — no fabricated/absent time;
 *  - [endedAtMs] is not in the future relative to [nowMs] (SYSTEM-1 Slice 6-R3, C2.4 item 4) — a corrupt or
 *    clock-skewed future timestamp must never count as evidence, let alone be treated as the "latest" one;
 *  - a non-corrupt, sufficient span (`endedAtMs - startedAtMs >= [MIN_DURATION_MS]`; a negative span from
 *    corrupt timestamps fails here) — "duration without evidence is open time, not training";
 *  - [hasMeasurableEvidence]: the module's minimal "produced a signal" flag (Enfoque = metrics present,
 *    Lógica = attempts present).
 *
 * [nowMs] must be an INJECTED clock reading (never `Clock.System` directly — see `core.time.EpochClock`),
 * so callers stay deterministic and testable.
 *
 * De-duplication by session id and the distinct-day count are handled downstream by [CalibrationPolicy].
 * Anomaly / anti-gaming detection (INFRA-03) is a separate, deferred layer — deliberately NOT modeled here.
 */
object ProfileSessionEligibility {

    /** A session must span at least this long to inform the profile (>= 2 min). */
    const val MIN_DURATION_MS: Long = 120_000L

    fun isEligible(startedAtMs: Long, endedAtMs: Long, hasMeasurableEvidence: Boolean, nowMs: Long): Boolean =
        endedAtMs > 0L &&
            endedAtMs <= nowMs &&
            (endedAtMs - startedAtMs) >= MIN_DURATION_MS &&
            hasMeasurableEvidence
}
