package com.mindos.app.features.mindboost.core.session

import kotlinx.serialization.Serializable

/** One resolved difficulty axis. A typed pair, not a `Map` entry: the axis set is closed and must stay closed. */
@Serializable
data class AxisValueV1(val axis: String, val value: Double) {
    init {
        require(axis.isNotBlank()) { "A difficulty axis name must not be blank." }
        require(!value.isNaN() && !value.isInfinite()) { "A difficulty axis value must be finite." }
        require(value in AXIS_MIN..AXIS_MAX) { "A difficulty axis value must be in $AXIS_MIN..$AXIS_MAX." }
    }

    private companion object {
        const val AXIS_MIN = 0.0
        const val AXIS_MAX = 1.0
    }
}

/** One resolved timing parameter, in milliseconds. */
@Serializable
data class TimingValueV1(val key: String, val millis: Long) {
    init {
        require(key.isNotBlank()) { "A timing key must not be blank." }
        require(millis >= 0) { "A timing value must not be negative." }
        require(millis <= MAX_TIMING_MS) { "A timing value above ${MAX_TIMING_MS}ms is not a session parameter." }
    }

    private companion object {
        const val MAX_TIMING_MS = 3_600_000L // one hour: far past any real window/ISI, still bounds a hostile row.
    }
}

/**
 * Exactly the inputs that decide a session's form, in a shape that cannot express an incoherent one.
 *
 * ## Why typed lists instead of `Map<String, Double>`
 *
 * The previous model carried `resolvedDifficulty: Map<String, Double>` and canonicalized it by joining entries as
 * `"$key=$value"` with `,`. That encoding cannot distinguish `{"a": 1.0, "b": 2.0}` from `{"a=1.0,b": 2.0}` —
 * both render `a=1.0,b=2.0`. In practice the keys came from a closed enum so the ambiguity was not reachable
 * through `createSession`; but "unreachable today through one entry point" is not the same property as "cannot
 * happen", and restore reads the map back from persisted bytes, which are not a trusted entry point. An open
 * `Map<String, *>` is an open contract: it can hold a key nobody declared, and nothing but a convention says
 * otherwise. A validated list of typed pairs makes the ambiguity unrepresentable rather than merely unlikely.
 *
 * ## Why validation lives in `init`
 *
 * `09_DECISION_LOG.md:390-391` fixes the shape of enforcement for this codebase: a constraint must be
 * *"impuesto por la AUSENCIA del input, no por un verdict que un consumidor podría ignorar"*. A `validate()`
 * that returns a result is a verdict a caller can ignore; a failing constructor is an absence. So an invalid
 * descriptor does not exist — including one decoded from a persisted row, where the throw surfaces as the
 * store's existing quarantine path rather than as a trusted value.
 */
@Serializable
data class FormDescriptorV1(
    /**
     * The seed's 64 bits as EXACTLY 16 lowercase hex chars, zero-padded — so `0` and `0000000000000000`
     * are one value rather than two spellings of it.
     */
    val seedHex: String,
    /** Stable string id of the RNG algorithm — never an enum ordinal, which reorders silently. */
    val rngVersionId: String,
    val engineContractVersion: Int,
    val generatorVersion: Int,
    val exerciseId: String,
    val exerciseVersion: Int,
    val difficultyMappingVersion: Int,
    /** EFFECTIVE axis values, snapshotted and ordered by axis name. Never a live reference to a moving profile. */
    val resolvedDifficulty: List<AxisValueV1>,
    val sessionTemplateVersion: Int,
    /**
     * The trial count the session was built for. Load-bearing: it decides how many draws the generator consumes,
     * so restoring with a different count silently rebuilds a DIFFERENT form. A de-tautologised reopen test and
     * an independent diff audit found exactly that, separately — it is in the descriptor because it decides the
     * form, which is the only membership rule this type has.
     */
    val plannedTrialCount: Int,
    /** The timing actually resolved for this session (window/ISI), ordered by key — not the current config. */
    val resolvedTimingPolicy: List<TimingValueV1>,
    val responseMappingVersion: Int,
    val contentVersion: Int,
) {
    init {
        require(seedHex.matches(SEED_HEX)) {
            "seedHex must be exactly $SEED_HEX_LENGTH lowercase hex chars: an unpadded or upper-cased seed is a " +
                "different string for the same 64 bits, which would make two identical forms disagree."
        }
        require(rngVersionId.isNotBlank()) { "rngVersionId must not be blank." }
        require(exerciseId.isNotBlank()) { "exerciseId must not be blank." }
        requirePositive(engineContractVersion, "engineContractVersion")
        requirePositive(generatorVersion, "generatorVersion")
        requirePositive(exerciseVersion, "exerciseVersion")
        requirePositive(difficultyMappingVersion, "difficultyMappingVersion")
        requirePositive(sessionTemplateVersion, "sessionTemplateVersion")
        requirePositive(responseMappingVersion, "responseMappingVersion")
        requirePositive(contentVersion, "contentVersion")
        require(plannedTrialCount in 1..MAX_PLANNED_TRIALS) {
            "plannedTrialCount must be in 1..$MAX_PLANNED_TRIALS. An unbounded count read from a persisted row " +
                "is an allocation instruction from untrusted input."
        }
        requireOrderedUnique(resolvedDifficulty.map { it.axis }, "resolvedDifficulty")
        requireOrderedUnique(resolvedTimingPolicy.map { it.key }, "resolvedTimingPolicy")
    }

    /** Redacted: [seedHex] is reproducibility material and must never reach a log, a crash report or an exception. */
    override fun toString(): String = "FormDescriptorV1(exerciseId=$exerciseId, redacted)"

    private fun requirePositive(value: Int, name: String) {
        require(value >= 1) { "$name must be >= 1 (was $value): version 0 or negative names no published contract." }
    }

    /**
     * Ordered AND unique. Ordering is what makes the canonical bytes independent of insertion order; uniqueness is
     * what stops one axis being declared twice with different values, where the digest would depend on which one
     * a reader happened to take.
     */
    private fun requireOrderedUnique(keys: List<String>, name: String) {
        require(keys == keys.sorted()) {
            "$name must be ordered by key so its canonical form is insertion-independent."
        }
        require(keys.toSet().size == keys.size) { "$name must not declare the same key twice." }
    }

    companion object {
        /** Not a performance limit — a bound on what a hostile or corrupt persisted row can ask us to allocate. */
        const val MAX_PLANNED_TRIALS = 1_000
        const val SEED_HEX_LENGTH = 16
        private val SEED_HEX = Regex("^[0-9a-f]{$SEED_HEX_LENGTH}$")
    }
}

/**
 * A copy that shares no mutable state with the caller.
 *
 * `List<T>` in Kotlin is a read-only *view*, not an immutable type: a caller can pass a `MutableList`, keep the
 * reference, and mutate it afterwards. An identity holding such a list would silently stop matching its own
 * digest — the digest was computed over the values at construction, and the values later changed underneath it.
 * The digest would still be *right*; the identity would just no longer be what it described.
 *
 * `ReproducibleSessionIdentity.of` and `fromPersisted` take this snapshot before computing anything, so the only
 * lists an identity ever holds are ones nobody else has a reference to. Callers may still hold aliased
 * descriptors; identities never do, and the identity is what has to stay coherent.
 */
internal fun FormDescriptorV1.defensiveCopy(): FormDescriptorV1 = copy(
    resolvedDifficulty = resolvedDifficulty.toList(),
    resolvedTimingPolicy = resolvedTimingPolicy.toList(),
)
