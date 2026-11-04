package com.mindos.app.features.mindboost.focus2.domain.runtime

import com.mindos.app.features.mindboost.core.runtime.RngVersion
import com.mindos.app.features.mindboost.core.runtime.SeedSource
import com.mindos.app.features.mindboost.core.runtime.SessionSeed
import com.mindos.app.features.mindboost.core.timing.ClockDomain
import com.mindos.app.features.mindboost.core.timing.HzBand
import com.mindos.app.features.mindboost.core.timing.InputTimestampPrecision
import com.mindos.app.features.mindboost.core.timing.ProbeCapability
import com.mindos.app.features.mindboost.core.session.purpose.ProfileEligibilityDisposition
import com.mindos.app.features.mindboost.core.session.purpose.ProfileMeasurementEligibility
import com.mindos.app.features.mindboost.core.session.purpose.SessionMeasurementPurpose
import com.mindos.app.features.mindboost.focus2.domain.generator.FocusExerciseSpec
import kotlinx.serialization.Serializable

/**
 * Focus V5 · S3 · tarea (4) — the PRE-TRIAL SEAL of a Focus v2 session.
 *
 * Everything a session can never change about itself lives here, and it is written to disk BEFORE the first
 * trial is presented. Two separate rules make that ordering non-negotiable:
 *
 *  - The purpose must be sealed before any evidence exists, or "what was this run for?" becomes a question
 *    answered after seeing the result — which is how a bad session quietly becomes practice and a good one
 *    quietly becomes measurement (ADR-FOCUS-PURPOSE).
 *  - The seed must come from an injected [SeedSource] and be PERSISTED before the first trial (REDTEAM #26b).
 *    A seed minted from the wall clock and never written down makes the session irreproducible forever
 *    (learning 2026-07-15). The descriptor is where it is written down.
 *
 * Enum-valued fields persist as their STRING name, never as an ordinal and never through a generated enum
 * serializer. Two reasons: an ordinal silently changes meaning the day someone alphabetises the enum, and a
 * generated serializer THROWS on a name this build does not know, whereas the repo's contract for an
 * unrecognised purpose is to resolve it to `UNKNOWN` and DENY. Reading must fail closed, not crash.
 */
@Serializable
data class FocusSessionDescriptor(
    val schemaVersion: Int = SCHEMA_VERSION,
    val sessionId: String,
    val exerciseId: String,
    /** Sealed purpose, as written. Re-derive with [purpose]; never trust a stored boolean. */
    val purposeName: String,
    val seedValue: Long,
    val rngVersionId: String,
    val generatorVersion: Int,
    val scorerVersion: Int,
    val constructModelVersion: Int,
    /**
     * The sealed exercise spec, stored as the NEUTRAL [FocusExerciseSpec] rather than any one exercise's
     * concrete type (S4 · `MB-FOCUS-0040`). The engine reads only [FocusExerciseSpec.responseWindowMs] and
     * [FocusExerciseSpec.totalTrials] from it; widening the field to the interface is what let the vigilance
     * tracer seal a session without fabricating flanker parameters, and it moved nothing in the engine core.
     * It serialises polymorphically across the closed spec set, so a spec a newer build wrote fails the decode
     * (`ignoreUnknownKeys = false` in `FocusSessionRestore`) rather than resolving to a wrong default.
     */
    val spec: FocusExerciseSpec,
    val inputModalityName: String,
    /**
     * Whether a screen reader was active when the session started (REDTEAM #33).
     *
     * S3 CAPTURES this fact; S5 owns the gate that acts on it. Capturing it here rather than deriving it later
     * is the whole point: the accessibility state at trial time cannot be reconstructed afterwards.
     */
    val screenReaderActive: Boolean,
    /**
     * S5 temporal capabilities (schema v2, ADR-FOCUS-TEMPORAL-QUALITY §3/§13 [R2-HIST]) — FIXED session facts,
     * sealed pre-trial, kept apart from the post-hoc aggregated observations (privacy P1-03). Empty
     * string = NOT CAPTURED (a v1 record): downstream resolves it to "cannot claim any temporal
     * class", never to a plausible default.
     */
    val probeCapabilityName: String = "",
    val clockDomainName: String = "",
    val inputPrecisionName: String = "",
    val nominalHzBandName: String = "",
    /** Semantic version of the probe/adapter observation contract. 0 = pre-temporal record. */
    val temporalObservationVersion: Int = 0,
) {

    /** Fail-closed: an unrecognised name resolves to `UNKNOWN`, which denies. It never guesses a neighbour. */
    val purpose: SessionMeasurementPurpose get() = SessionMeasurementPurpose.fromWire(purposeName)

    /** `null` when this build does not know the algorithm the seed was minted under. A seed without its
     *  algorithm is not a seed, so callers must treat null as "cannot reproduce", never as "use the default". */
    val rngVersion: RngVersion? get() = RngVersion.fromId(rngVersionId)

    val seed: SessionSeed? get() = rngVersion?.let { SessionSeed(seedValue, it) }

    val inputModality: InputModality get() = InputModality.fromWire(inputModalityName)

    /** `null` = the probe capability was never captured (v1 record) — NOT a fallback probe. */
    val probeCapability: ProbeCapability?
        get() = ProbeCapability.entries.firstOrNull { it.name == probeCapabilityName }

    /** Fail-closed: unrecognised/absent resolves to [ClockDomain.UNKNOWN] (never authoritative). */
    val clockDomain: ClockDomain
        get() = ClockDomain.entries.firstOrNull { it.name == clockDomainName } ?: ClockDomain.UNKNOWN

    /** Fail-closed: unrecognised/absent resolves to UNKNOWN (never admissible for shadow RT). */
    val inputPrecision: InputTimestampPrecision
        get() = InputTimestampPrecision.entries.firstOrNull { it.name == inputPrecisionName }
            ?: InputTimestampPrecision.UNKNOWN

    /** `null` = never captured. The DECLARED nominal band; the detected one is an observation, not a seal. */
    val nominalHzBand: HzBand? get() = HzBand.entries.firstOrNull { it.name == nominalHzBandName }

    /**
     * The stable, reproducible identity of the seed (REDTEAM #26b asserts this exists and does not change).
     *
     * Lower-case, zero-padded to 16 digits so it is a fixed-width token rather than a number whose text form
     * depends on its magnitude.
     */
    val seedHex: String get() = seedValue.toULong().toString(HEX_RADIX).padStart(HEX_DIGITS, '0')

    /**
     * Whether the versioned lineage is complete enough for cross-session comparability.
     *
     * This is the `lineageComplete` input `ProfileMeasurementEligibility` requires, DERIVED from facts rather
     * than passed in as a boolean by the caller — a pre-computed eligibility flag is forgeable by drift, which
     * is the exact failure ADR-FOCUS-PURPOSE closed.
     */
    val lineageComplete: Boolean
        get() = rngVersion != null &&
            generatorVersion > 0 &&
            scorerVersion > 0 &&
            constructModelVersion > 0 &&
            exerciseId.isNotBlank()

    /** The three-way verdict. Tutorial and familiarization land on `NotEligibleByPurpose` BY CONSTRUCTION. */
    fun evidenceDisposition(): ProfileEligibilityDisposition =
        ProfileMeasurementEligibility.disposition(purpose, lineageComplete)

    /**
     * Whether RT-derived quantities from this session may ever be OFFICIAL evidence (REDTEAM #33, #34).
     *
     * TWO conditions, because there are two different ways the temporal construct stops being the intended one:
     *
     *  - **A screen reader is active.** The participant is listening to a label before they can answer, so the
     *    latency measures listening plus deciding rather than deciding.
     *  - **The modality is not direct touch.** A sequential-selection answer costs one activation for the
     *    focused option and an extra advance for the other, so its latency is partly a function of where the
     *    cursor happened to be — which depends on the PREVIOUS trial's answer, not on this trial's decision.
     *
     * Note the second condition is about the INPUT PATH, not about the person: the confound is cursor travel,
     * and it would apply equally to someone who chose sequential input for any other reason. An unrecognised
     * modality is treated as inadmissible, because a value this build cannot interpret is not a value it may
     * assume is safe.
     *
     * Accuracy is untouched in both cases and the exercise is never withheld: excluding the person from
     * training would be the other half of the same unfairness. The gate that consumes this lands in S5; S3
     * owes the honest capture.
     */
    val rtEvidenceAdmissible: Boolean
        get() = !screenReaderActive && inputModality == InputModality.TOUCH_DIRECT

    init {
        require(sessionId.isNotBlank()) { "sessionId must not be blank" }
        require(schemaVersion > 0) { "schemaVersion must be positive, was $schemaVersion" }
    }

    companion object {
        /** v2 (S5): + temporal capability fields. v1 remains readable (restore accepts ≤ current). */
        const val SCHEMA_VERSION: Int = 2
        private const val HEX_RADIX: Int = 16
        private const val HEX_DIGITS: Int = 16

        /**
         * Seals a session. This IS the sealing act: there is no setter and no re-seal, so a purpose cannot be
         * revised once a trial has been shown.
         *
         * The seed is minted HERE, from the injected [seedSource], so no call site can pass one it derived from
         * a clock. [SeedSource] is a `fun interface`, so a test injects a fixed seed without a fake framework.
         */
        @Suppress("LongParameterList") // Every parameter is a distinct fact that must be sealed; grouping them
        // into a config object would just move the same list one file away and add an indirection to read through.
        fun seal(
            sessionId: String,
            exerciseId: String,
            purpose: SessionMeasurementPurpose,
            seedSource: SeedSource,
            generatorVersion: Int,
            scorerVersion: Int,
            constructModelVersion: Int,
            spec: FocusExerciseSpec,
            inputModality: InputModality,
            screenReaderActive: Boolean,
            // S5 temporal capture. The "not captured" defaults are honest and FAIL-CLOSED downstream
            // (no capability ⇒ NotCaptured/T0, never eligible) — they are capture facts, not gates.
            probeCapability: ProbeCapability? = null,
            clockDomain: ClockDomain? = null,
            inputPrecision: InputTimestampPrecision? = null,
            nominalHzBand: HzBand? = null,
            temporalObservationVersion: Int = 0,
        ): FocusSessionDescriptor {
            val minted = seedSource.mint()
            return FocusSessionDescriptor(
                sessionId = sessionId,
                exerciseId = exerciseId,
                purposeName = purpose.name,
                seedValue = minted.value,
                rngVersionId = minted.version.id,
                generatorVersion = generatorVersion,
                scorerVersion = scorerVersion,
                constructModelVersion = constructModelVersion,
                spec = spec,
                inputModalityName = inputModality.name,
                screenReaderActive = screenReaderActive,
                probeCapabilityName = probeCapability?.name ?: "",
                clockDomainName = clockDomain?.name ?: "",
                inputPrecisionName = inputPrecision?.name ?: "",
                nominalHzBandName = nominalHzBand?.name ?: "",
                temporalObservationVersion = temporalObservationVersion,
            )
        }
    }
}

/**
 * How the person answers (REDTEAM #34).
 *
 * This is a MODALITY, not an accessibility label, and it is recorded because evidence gathered under one
 * modality is not comparable with evidence gathered under another without an explicit bridge (portfolio rule
 * #18). A switch-access run is a complete, first-class session — it just belongs to its own comparison class.
 *
 * [UNKNOWN] exists for the same reason `SessionMeasurementPurpose.UNKNOWN` does: a row written by a newer build
 * must resolve to something that denies pooling, not to a plausible-looking default.
 */
enum class InputModality {
    /** Direct touch on the option itself. */
    TOUCH_DIRECT,

    /**
     * Sequential: focus alternates between the two options and a single activation selects the focused one.
     *
     * Required of EVERY choice mechanic. An exercise that cannot be completed with one or two switches is not
     * "less accessible", it is structurally uncompletable for those users — which is why the plan makes this a
     * design requirement rather than an enhancement.
     */
    SWITCH_SEQUENTIAL,

    UNKNOWN,
    ;

    companion object {
        fun fromWire(raw: String?): InputModality = entries.firstOrNull { it.name == raw } ?: UNKNOWN
    }
}

/**
 * The accessibility services active when a session begins.
 *
 * Read once at session start and sealed into the descriptor. Sampling it later would let a mid-session change
 * rewrite what the earlier trials meant.
 */
data class AccessibilityContext(
    val screenReaderActive: Boolean = false,
    val switchAccessActive: Boolean = false,
    /**
     * Set only when the PARTICIPANT has declared they are tapping the screen directly. No platform signal sets
     * it, because no platform signal can: absence of a detected switch-access service is not evidence of direct
     * touch. The declaration UI does not exist yet, so this is false everywhere today.
     */
    val directTouchDeclared: Boolean = false,
) {
    /**
     * The modality, fail-CLOSED.
     *
     * An earlier version returned `TOUCH_DIRECT` whenever switch-access was not detected, and the companion
     * below called that "fail-closed" — it was the opposite. Android's check only sees Android's own
     * switch-access service, so an external switch controller, a Bluetooth keyboard, a head pointer or an eye
     * tracker all fell through to `TOUCH_DIRECT`, and `rtEvidenceAdmissible` then ADMITTED a reaction time
     * produced by an input path with unknown motor cost. The charter already resolves this asymmetry the other
     * way for the Speed module's timebase: an RT-critical signal whose capture path cannot be positively
     * confirmed is excluded by construction, never admitted by default. This now matches it.
     */
    val impliedModality: InputModality
        get() = when {
            switchAccessActive -> InputModality.SWITCH_SEQUENTIAL
            directTouchDeclared -> InputModality.TOUCH_DIRECT
            else -> InputModality.UNKNOWN
        }

    companion object {
        /** Nothing detected and nothing declared. Playable, and never a source of RT evidence. */
        val NONE = AccessibilityContext()
    }
}
