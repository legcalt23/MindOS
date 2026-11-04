package com.mindos.app.features.mindboost.core.timing

/**
 * S5 · ADR-FOCUS-TEMPORAL-QUALITY §4.1 [R2-HIST] — temporal-integrity class of a session's OBSERVATION.
 *
 * Derived from the session's own measurement (sealed capability + real frame stats), never from the
 * device model. [T2_LATENCY_BOUNDED]/[T3_EXTERNALLY_CALIBRATED] have NO producer in S5 (they require
 * a physical latency bench / maintained external calibration): [TemporalClassPolicy] can only return
 * T0/T1, and a test pins that unreachability.
 */
enum class TemporalClass {
    /** No authoritative probe, unknown/mismatched clock domain, or insufficient session sample. */
    T0_NON_AUTHORITATIVE,

    /** Frame cadence observed by an authoritative monotonic probe (Choreographer / CADisplayLink). */
    T1_FRAME_OBSERVED,

    /** T1 + physically bounded display+touch latency (bench). Unreachable in S5. */
    T2_LATENCY_BOUNDED,

    /** T2 + maintained external calibration. Unreachable in S5. */
    T3_EXTERNALLY_CALIBRATED,
}

/**
 * ADR-FOCUS-TEMPORAL-QUALITY §12 [R2-HIST] — identity of the monotonic time base a probe's durations belong to.
 *
 * Sealed into the session descriptor. No type in this vertical exposes an absolute timestamp, so
 * cross-domain subtraction is impossible by construction; the domain exists so that fact is auditable.
 */
enum class ClockDomain {
    ANDROID_CHOREOGRAPHER_UPTIME,
    IOS_CADISPLAYLINK_MACH_TIME,
    NONE_MONOTONIC_FALLBACK,

    /** Fail-closed default for unknown/unsealed readings (never authoritative). */
    UNKNOWN,
}

/**
 * ADR-FOCUS-TEMPORAL-QUALITY §3 [R2-HIST] — the probe actually running, as a FIRST-CLASS sealed fact.
 *
 * A no-op probe reports [NONE_MONOTONIC_FALLBACK] explicitly; absence of observation is never
 * inferred from empty stats (that inference is the fabricated-CLEAN trap this vertical closes).
 */
enum class ProbeCapability(val expectedDomain: ClockDomain?) {
    ANDROID_CHOREOGRAPHER(ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME),
    IOS_CADISPLAYLINK(ClockDomain.IOS_CADISPLAYLINK_MACH_TIME),
    NONE_MONOTONIC_FALLBACK(null),
}

/**
 * ADR-FOCUS-TEMPORAL-QUALITY §3/§4.1 [R2-HIST] — input-timestamp precision is ORTHOGONAL capability, not a
 * temporal class (the orden-2 "T1+" = [TemporalClass.T1_FRAME_OBSERVED] with [NANOS_NATIVE]).
 */
enum class InputTimestampPrecision {
    /** `MotionEvent.getEventTimeNanos()` (Android API 34+) or equivalent native source. */
    NANOS_NATIVE,

    /** Millisecond event time widened to nanos (Android 29–33 baseline). */
    MILLIS_DERIVED,

    /** Fail-closed default (never admissible for shadow RT). */
    UNKNOWN,
}
