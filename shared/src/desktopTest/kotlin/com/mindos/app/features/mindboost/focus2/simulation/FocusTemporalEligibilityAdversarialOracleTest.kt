package com.mindos.app.features.mindboost.focus2.simulation

import com.mindos.app.features.mindboost.core.timing.ClockDomain
import com.mindos.app.features.mindboost.core.timing.InputTimestampPrecision
import com.mindos.app.features.mindboost.core.timing.ProbeCapability
import com.mindos.app.features.mindboost.core.timing.TemporalClass
import com.mindos.app.features.mindboost.core.timing.TemporalQualityThresholds
import com.mindos.app.features.mindboost.core.timing.TemporalWindowVerdict
import com.mindos.app.features.mindboost.core.timing.WindowExclusionReason
import com.mindos.app.features.mindboost.focus2.domain.evidence.FocusEvidenceState
import com.mindos.app.features.mindboost.focus2.domain.evidence.FocusEvidenceStatePolicy
import com.mindos.app.features.mindboost.focus2.domain.evidence.FocusTemporalEligibility
import com.mindos.app.features.mindboost.focus2.domain.evidence.FocusTemporalEligibility.ExclusionCause
import com.mindos.app.features.mindboost.focus2.domain.evidence.FocusTemporalEligibility.TemporalEvaluation
import com.mindos.app.features.mindboost.focus2.domain.evidence.FocusTemporalEligibility.Verdict
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusRuntimeInput
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusRuntimeSnapshot
import com.mindos.app.features.mindboost.focus2.domain.runtime.FocusSessionReducer
import com.mindos.app.features.mindboost.focus2.domain.runtime.VectorFilterTask
import com.mindos.app.features.mindboost.focus2.domain.scoring.ConditionId
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * S5 · Paso 4 — SHADOW eligibility ADVERSARIAL oracle (`desktopTest`; NO production change, NO wiring —
 * `FocusTemporalEligibility.evaluate` stays UNMOUNTED, verified out-of-band by a zero-consumer sweep and by
 * the constant-pool `Focus2DependencyBoundaryTest`).
 *
 * The property CORE (`nonExclusionState` / `comparableByExclusion` / `reopeningsOverAscendingExclusion`) lives
 * in `commonTest` (`FocusTemporalEligibilityProperty.kt`) so it is cross-platform; this oracle feeds REAL,
 * reducer-driven `evaluate()` outputs through that core. Every evaluated snapshot is one the engine can
 * actually reach (reducer-driven, never hand-assembled).
 *
 * Coverage of the Paso-4 remediation phases:
 *  - Fase B: [monotonicity_isolatedProperty_comparablePairsShareNonExclusionState_neverReopen] proves the
 *    invariant as a property over a domain where ONLY exclusion varies (nonExclusionState pinned constant).
 *  - Fase C: the backstop constant 0.50 is pinned directly, the boundary is strict, and the configured max is
 *    read (not hardcoded); an underivable fraction fails closed with no NaN/Inf/fabricated-zero.
 *  - Fase D: an 8-row truth table over every cause combination pins verdict, cause SET, rtShadowAdmissible and
 *    evidence-state — exercising BOTH SUTs and the OFFICIAL_ELIGIBLE inconstructibility.
 *  - Fase E: honest and adversary halves each carry a declared cardinality, not merely `> 0`.
 *  - Fase F: [checkerSensitivitySentinel_flagsNonMonotoneModel] proves the CHECKER has teeth (it is NOT a
 *    mutation of `evaluate()`; real SUT mutation is the out-of-band campaign recorded in the mutation report).
 */
class FocusTemporalEligibilityAdversarialOracleTest {

    private val spec = VectorFilterTask.SPEC
    private val defaults = TemporalQualityThresholds.DEFAULT
    private val excluded = TemporalWindowVerdict.Excluded(WindowExclusionReason.TRUNCATION_RISK)
    private val clean = TemporalWindowVerdict.Clean

    /** Drive the reducer to a snapshot with the given per-window temporal verdicts (null = unclassified). */
    private fun sessionOf(
        verdicts: List<TemporalWindowVerdict?>,
        response: Response = Response.LEFT,
        rtNanos: Long = RT_NANOS,
    ): FocusRuntimeSnapshot {
        var s = FocusSessionReducer.reduce(
            FocusRuntimeSnapshot.initial("p4-oracle"), FocusRuntimeInput.StartSession(0L), spec,
        ).snapshot
        var at = START_AT
        verdicts.forEach { verdict ->
            s = FocusSessionReducer.reduce(
                s, FocusRuntimeInput.PresentTrial(at, WINDOW_NANOS, PLANNED, ConditionId.CONGRUENT), spec,
            ).snapshot
            s = FocusSessionReducer.reduce(
                s,
                FocusRuntimeInput.Respond(
                    at + rtNanos, response, ConditionId.CONGRUENT, isSignal = true,
                    temporalVerdict = verdict,
                    temporalSampledFrames = if (verdict != null) FRAMES_PER_WINDOW else 0,
                ),
                spec,
            ).snapshot
            at += STEP_NANOS
        }
        return s
    }

    private fun evalAt(
        verdicts: List<TemporalWindowVerdict?>,
        capability: ProbeCapability? = ProbeCapability.ANDROID_CHOREOGRAPHER,
        clock: ClockDomain = ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME,
        thresholds: TemporalQualityThresholds = defaults,
    ): TemporalEvaluation =
        FocusTemporalEligibility.evaluate(capability, clock, sessionOf(verdicts), thresholds)

    // Exhaustive over the sealed hierarchy ON PURPOSE: a variant added to `Verdict` stops this file
    // compiling until someone handles it here AND exercises it in the codomain allowlist below. That
    // build-time guard is what replaced the deleted producerless `Degraded` tier (`MB-FOCUS-0080`).
    private fun Verdict.kind(): String = when (this) {
        is Verdict.Eligible -> "Eligible"
        is Verdict.Excluded -> "Excluded"
        is Verdict.NotCaptured -> "NotCaptured"
    }

    // ================================================================================================
    // FASE B — MONOTONICITY as an ISOLATED property. The domain varies ONLY exclusion; nonExclusionState
    // is pinned constant within each chain, so no second axis can move behind the invariant's back.
    // ================================================================================================
    @Test
    fun monotonicity_isolatedProperty_comparablePairsShareNonExclusionState_neverReopen() {
        var chains = 0
        var eligibleObserved = 0
        var excludedObserved = 0
        var comparablePairs = 0
        var reopenings = 0
        for (n in MIN_WINDOWS..MAX_WINDOWS) {
            // Chain: fixed n windows, k of them excluded (k = 0..n). Only the clean/excluded SPLIT changes;
            // every window still resolves, so windowsClassified, coverage and session class stay fixed.
            val chain = (0..n).map { k -> evalAt(List(n) { if (it < k) excluded else clean }) }

            // ISOLATION proof: the whole chain shares ONE non-exclusion state.
            val states = chain.map { it.nonExclusionState(defaults) }.toSet()
            assertEquals(1, states.size, "chain n=$n moved a non-exclusion axis: $states")

            // The chain is ordered by ASCENDING exclusion (the order the property is defined over).
            val fractions = chain.map { it.excludedFraction ?: 1.0 }
            assertEquals(fractions.sorted(), fractions, "exclusion not ascending at n=$n: $fractions")

            // Every i<=j pair is comparableByExclusion, so the checker below is applied to a real chain.
            for (i in chain.indices) for (j in i until chain.size) {
                assertTrue(
                    comparableByExclusion(chain[i], chain[j], defaults),
                    "pair ($i,$j) at n=$n is not comparableByExclusion",
                )
                comparablePairs++
            }

            val eligibility = chain.map { it.rtShadowVerdict.isEligible() }
            // Per CHAIN, which is what the message promises. The global counter below cannot say
            // this: with 14 eligible configs across 5 chains, one chain could produce none and a
            // global `>= chains` would still pass. Index 0 is the least-excluded (all-clean) config.
            assertTrue(eligibility.first(), "chain n=$n: the k=0 (all-clean) config must be Eligible")
            eligibility.forEach { if (it) eligibleObserved++ else excludedObserved++ }
            reopenings += reopeningsOverAscendingExclusion(eligibility)
            chains++
        }
        // The invariant: zero re-openings anywhere in the domain.
        assertEquals(0, reopenings, "more exclusion re-opened eligibility somewhere in the domain")
        // Fase E cardinality (declared exactly, not `> 0`): both halves are non-empty with known counts.
        assertEquals(MAX_WINDOWS - MIN_WINDOWS + 1, chains, "chain count drifted")
        assertTrue(chains >= MIN_CHAINS, "domain too small: $chains chains")
        assertTrue(eligibleObserved >= chains, "some chain never produced an Eligible (k=0) config")
        assertTrue(excludedObserved >= chains, "some chain never produced an Excluded config")
        assertEquals(EXPECTED_COMPARABLE_PAIRS, comparablePairs, "comparable-pair cardinality drifted")
    }

    // ================================================================================================
    // FASE F (sentinel) — the CHECKER MUST catch a non-monotone model. This proves the checker is not
    // blind; it is NOT a mutation of evaluate() (real SUT mutation is the out-of-band Fase-F campaign).
    // ================================================================================================
    @Test
    fun checkerSensitivitySentinel_flagsNonMonotoneModel() {
        val nonMonotone = listOf(true, true, false, false, true) // eligible AGAIN after being excluded (R2 shape)
        assertTrue(
            reopeningsOverAscendingExclusion(nonMonotone) > 0,
            "the checker failed to flag a non-monotone (R2-style) model — it has no teeth",
        )
        val realChain = (0..MAX_WINDOWS).map { k ->
            evalAt(List(MAX_WINDOWS) { if (it < k) excluded else clean }).rtShadowVerdict.isEligible()
        }
        assertEquals(
            0, reopeningsOverAscendingExclusion(realChain),
            "real evaluate() is monotone; the sentinel confirms the checker separates broken from correct",
        )
    }

    // ================================================================================================
    // FASE C — the backstop threshold 0.50 is PINNED (constant + strict boundary + configured, not hardcoded),
    // and an underivable fraction fails CLOSED with no NaN/Inf/fabricated zero.
    // ================================================================================================
    @Test
    fun defaultBackstopMax_isPinnedToExactlyHalf() {
        assertEquals(BACKSTOP_MAX_DEFAULT, defaults.sessionExcludedMax, 0.0, "the default session backstop moved")
    }

    @Test
    fun backstopBoundary_isStrict_exactHalfEligible_immediateNeighboursBracketIt() {
        // Rational-bracket resolution: window counts are integers, so fractions are rationals k/denominator.
        // The exact boundary 0.50 is representable (2/4); the nearest reachable neighbours bracket it.
        val atHalf = evalAt(listOf(clean, clean, excluded, excluded)) // 2/4 = 0.50 exactly
        assertEquals(0.5, atHalf.excludedFraction ?: -1.0, 0.0)
        assertTrue(atHalf.rtShadowVerdict.isEligible(), "fraction exactly 0.50 must NOT trip (comparison is strict >)")

        val justBelow = evalAt(listOf(clean, clean, clean, clean, excluded, excluded, excluded)) // 3/7 ≈ 0.4286
        assertTrue((justBelow.excludedFraction ?: 1.0) < BACKSTOP_MAX_DEFAULT, "bracket-below not below 0.50")
        assertTrue(justBelow.rtShadowVerdict.isEligible(), "a fraction below 0.50 must stay Eligible")

        val justAbove = evalAt(listOf(clean, clean, excluded, excluded, excluded)) // 3/5 = 0.60
        assertEquals(FRACTION_060, justAbove.excludedFraction ?: -1.0, EPS)
        val crossed = justAbove.rtShadowVerdict
        assertTrue(
            crossed is Verdict.Excluded && ExclusionCause.SESSION_BACKSTOP_EXCEEDED in crossed.causes,
            "crossing 0.50 did not trip SESSION_BACKSTOP_EXCEEDED",
        )
    }

    @Test
    fun inBandFraction_tripsTheDefaultBackstop_pinningItInsideTheBand() {
        // 4/7 ≈ 0.571 is strictly inside (0.50, 0.60): under the default max 0.50 it MUST trip. Without this,
        // a silent regression of the default up to < 0.571 would pass (the 0.50/0.60 straddle only pins [0.50,0.60)).
        val e = evalAt(listOf(clean, clean, clean, excluded, excluded, excluded, excluded))
        val f = e.excludedFraction ?: 0.0
        assertTrue(f > BACKSTOP_MAX_DEFAULT && f < FRACTION_060, "fraction $f is not inside the pinning band")
        assertTrue(e.rtShadowVerdict is Verdict.Excluded, "a fraction in (0.50,0.60) must trip the default backstop")
    }

    @Test
    fun evaluateReadsTheConfiguredBackstopMax_notAHardcodedConstant() {
        // Inject a relaxed max (0.66): a fraction of 0.60 that WOULD trip the default 0.50 now stays Eligible,
        // and 0.667 trips. Kills both a hardcoded constant AND a >/>= strictness regression at the configured max.
        val relaxed = TemporalQualityThresholds(sessionExcludedMax = RELAXED_MAX)
        val below = evalAt(listOf(clean, clean, excluded, excluded, excluded), thresholds = relaxed) // 3/5 = 0.60 < 0.66
        assertEquals(FRACTION_060, below.excludedFraction ?: -1.0, EPS)
        assertTrue(
            below.rtShadowVerdict.isEligible(),
            "0.60 < injected max 0.66 must be Eligible — evaluate() ignored the injected threshold",
        )
        val above = evalAt(listOf(clean, excluded, excluded), thresholds = relaxed) // 2/3 = 0.667 > 0.66
        assertTrue(
            (above.excludedFraction ?: 0.0) > RELAXED_MAX && above.rtShadowVerdict is Verdict.Excluded,
            "0.667 > injected max 0.66 must be Excluded",
        )
    }

    @Test
    fun excludedFraction_isAlwaysFiniteInUnitInterval_nullOnlyWhenNoWindowsObserved() {
        // NaN/±Inf are INCONSTRUCTIBLE: numerator and denominator are non-negative ints and the SUT guards
        // denominator > 0, so every derivable fraction is finite in [0,1]; the only null is the empty-cells
        // (NotCaptured) case, never a fabricated 0.0.
        var finite = 0
        var nulls = 0
        for (n in 0..MAX_WINDOWS) for (k in 0..n) {
            val verdicts = if (n == 0) emptyList() else List(n) { if (it < k) excluded else clean }
            val f = evalAt(verdicts).excludedFraction
            if (f == null) {
                nulls++
                assertEquals(0, n, "null fraction appeared with n=$n>0 windows — unreachable via the reducer")
            } else {
                finite++
                assertEquals(f, f, "fraction was NaN") // NaN != NaN
                assertTrue(f in 0.0..1.0, "fraction $f escaped [0,1]")
            }
        }
        assertTrue(finite > 0 && nulls > 0, "domain did not exercise both the finite and the null branch")
    }

    // ================================================================================================
    // FASE D — TRUTH TABLE over all 8 cause combinations. Pins verdict, the cause SET (order-independent:
    // the contract is the UNION of triggered causes, NOT a precedence ladder), rtShadowAdmissible and the
    // evidence-state — exercising FocusTemporalEligibility AND FocusEvidenceStatePolicy together.
    // ================================================================================================
    @Test
    fun truthTable_ofAllEightCauseCombinations_pinsVerdictCauseAdmissibilityAndEvidence() {
        val N = null // an unclassified (probe-off) window: resolves the trial but leaves coverage incomplete
        val rows = listOf(
            // name, verdicts, capability, expected cause SET (empty => Eligible), expected evidence state
            TruthRow("none", listOf(clean, clean), AUTH, emptySet(), FocusEvidenceState.PROVISIONAL),
            TruthRow("nonAuth", listOf(clean, clean), NON_AUTH_CAP, setOf(ExclusionCause.NON_AUTHORITATIVE_CLASS), FocusEvidenceState.PROVISIONAL),
            TruthRow("incomplete", listOf(clean, clean, clean, N), AUTH, setOf(ExclusionCause.COVERAGE_INCOMPLETE), FocusEvidenceState.PROVISIONAL),
            TruthRow("backstop", listOf(clean, excluded, excluded, excluded), AUTH, setOf(ExclusionCause.SESSION_BACKSTOP_EXCEEDED), FocusEvidenceState.TRAINING_ONLY),
            TruthRow("nonAuth+incomplete", listOf(clean, clean, clean, N), NON_AUTH_CAP, setOf(ExclusionCause.NON_AUTHORITATIVE_CLASS, ExclusionCause.COVERAGE_INCOMPLETE), FocusEvidenceState.PROVISIONAL),
            TruthRow("nonAuth+backstop", listOf(clean, excluded, excluded, excluded), NON_AUTH_CAP, setOf(ExclusionCause.NON_AUTHORITATIVE_CLASS, ExclusionCause.SESSION_BACKSTOP_EXCEEDED), FocusEvidenceState.TRAINING_ONLY),
            TruthRow("incomplete+backstop", listOf(excluded, excluded, excluded, clean, N), AUTH, setOf(ExclusionCause.COVERAGE_INCOMPLETE, ExclusionCause.SESSION_BACKSTOP_EXCEEDED), FocusEvidenceState.TRAINING_ONLY),
            TruthRow("all-three", listOf(excluded, excluded, excluded, clean, N), NON_AUTH_CAP, setOf(ExclusionCause.NON_AUTHORITATIVE_CLASS, ExclusionCause.COVERAGE_INCOMPLETE, ExclusionCause.SESSION_BACKSTOP_EXCEEDED), FocusEvidenceState.TRAINING_ONLY),
        )
        // Every one of the 2^3 combinations appears exactly once.
        assertEquals(EXPECTED_TRUTH_ROWS, rows.size, "truth table is not the full 2^3")
        assertEquals(rows.map { it.expectedCauses }.toSet().size, rows.size, "a cause combination is duplicated/missing")

        for (row in rows) {
            val clock = if (row.capability == NON_AUTH_CAP) ClockDomain.NONE_MONOTONIC_FALLBACK else ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME
            val eval = FocusTemporalEligibility.evaluate(row.capability, clock, sessionOf(row.verdicts))
            val verdict = eval.rtShadowVerdict
            if (row.expectedCauses.isEmpty()) {
                assertTrue(verdict is Verdict.Eligible, "row '${row.name}' expected Eligible, got ${verdict.kind()}")
            } else {
                assertTrue(verdict is Verdict.Excluded, "row '${row.name}' expected Excluded, got ${verdict.kind()}")
                assertEquals(row.expectedCauses, verdict.causes, "row '${row.name}' cause set mismatch")
            }
            // rtShadowAdmissible follows the verdict (with modality + native precision held admissible).
            val admissible = FocusEvidenceStatePolicy.rtShadowAdmissible(
                rtModalityAdmissible = true,
                inputPrecision = InputTimestampPrecision.NANOS_NATIVE,
                temporal = eval,
            )
            assertEquals(verdict is Verdict.Eligible, admissible, "row '${row.name}' rtShadowAdmissible mismatch")
            // Evidence-state: the temporal axis demotes ONLY through the backstop fraction, never through
            // NON_AUTH/INCOMPLETE — and OFFICIAL_ELIGIBLE is never produced.
            val evidence = FocusEvidenceStatePolicy.evidenceOf(
                writeAheadAdmissible = true, doseComplete = true, scorerInvalidated = false, temporal = eval,
            )
            assertEquals(row.expectedEvidence, evidence, "row '${row.name}' evidence-state mismatch")
            assertTrue(evidence != FocusEvidenceState.OFFICIAL_ELIGIBLE, "row '${row.name}' produced OFFICIAL_ELIGIBLE")
        }
    }

    // ================================================================================================
    // FASE G — OFFICIAL_ELIGIBLE is INCONSTRUCTIBLE across the whole boolean × temporal domain, and
    // rtShadowAdmissible requires ALL THREE of its conjuncts (eligible temporal, native precision, modality).
    // ================================================================================================
    @Test
    fun officialEligible_isInconstructible_acrossTheBooleanAndTemporalDomain() {
        var evaluated = 0
        var provisional = 0
        var trainingOnly = 0
        var invalidated = 0
        val temporalSamples = listOf(
            evalAt(listOf(clean, clean)),                                   // Eligible
            evalAt(listOf(clean, excluded, excluded, excluded)),           // Excluded/backstop
            evalAt(emptyList()),                                            // NotCaptured
            evalAt(listOf(clean, clean, clean, null)),                     // Excluded/coverage
        )
        for (writeAhead in listOf(true, false)) {
            for (dose in listOf(true, false)) {
                for (invalid in listOf(true, false)) {
                    for (t in temporalSamples) {
                        val evidence = FocusEvidenceStatePolicy.evidenceOf(writeAhead, dose, invalid, t)
                        assertTrue(evidence != FocusEvidenceState.OFFICIAL_ELIGIBLE, "OFFICIAL_ELIGIBLE was constructed")
                        when (evidence) {
                            FocusEvidenceState.PROVISIONAL -> provisional++
                            FocusEvidenceState.TRAINING_ONLY -> trainingOnly++
                            FocusEvidenceState.INVALIDATED -> invalidated++
                            FocusEvidenceState.OFFICIAL_ELIGIBLE -> Unit
                        }
                        evaluated++
                    }
                }
            }
        }
        assertEquals(EXPECTED_EVIDENCE_EVALS, evaluated, "evidence domain cardinality drifted")
        assertTrue(provisional > 0 && trainingOnly > 0 && invalidated > 0, "not all reachable states were exercised")
    }

    @Test
    fun rtShadowAdmissible_requiresEligibleTemporalNativePrecisionAndModality() {
        val eligible = evalAt(listOf(clean, clean))
        val excludedEval = evalAt(listOf(clean, excluded, excluded, excluded))
        assertTrue(eligible.rtShadowVerdict is Verdict.Eligible)
        assertTrue(excludedEval.rtShadowVerdict is Verdict.Excluded)
        // All three conjuncts present => admissible.
        assertTrue(FocusEvidenceStatePolicy.rtShadowAdmissible(true, InputTimestampPrecision.NANOS_NATIVE, eligible))
        // Drop each conjunct in turn => inadmissible.
        assertFalse(FocusEvidenceStatePolicy.rtShadowAdmissible(false, InputTimestampPrecision.NANOS_NATIVE, eligible))
        assertFalse(FocusEvidenceStatePolicy.rtShadowAdmissible(true, InputTimestampPrecision.MILLIS_DERIVED, eligible))
        assertFalse(FocusEvidenceStatePolicy.rtShadowAdmissible(true, InputTimestampPrecision.NANOS_NATIVE, excludedEval))
        assertFalse(FocusEvidenceStatePolicy.rtShadowAdmissible(true, InputTimestampPrecision.NANOS_NATIVE, null))
    }

    // ================================================================================================
    // FASE E — DISCRIMINATING HALVES with DECLARED cardinality (not `> 0`).
    // ================================================================================================
    @Test
    fun honestHalf_variesWindowsRtAndResponse_allStayEligible_withDeclaredCardinality() {
        var honest = 0
        for (n in MIN_WINDOWS..MAX_WINDOWS) {
            for ((response, rt) in listOf(Response.LEFT to FAST_RT, Response.RIGHT to SLOW_RT)) {
                val eval = FocusTemporalEligibility.evaluate(
                    ProbeCapability.ANDROID_CHOREOGRAPHER, ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME,
                    sessionOf(List(n) { clean }, response, rt),
                )
                assertEquals(0.0, eval.excludedFraction ?: -1.0, 0.0, "honest session had exclusion at n=$n")
                assertTrue(eval.rtShadowVerdict.isEligible(), "honest fully-covered T1 session not Eligible (n=$n)")
                honest++
            }
        }
        assertEquals((MAX_WINDOWS - MIN_WINDOWS + 1) * 2, honest, "honest-half cardinality drifted")
    }

    @Test
    fun adversaryHalf_everyLabelledAttackIsNonEligible_withDeclaredCardinality() {
        val attacks: List<Pair<String, TemporalEvaluation>> = listOf(
            "all-excluded (backstop)" to evalAt(List(MAX_WINDOWS) { excluded }),
            "non-authoritative" to evalAt(listOf(clean, clean), NON_AUTH_CAP, ClockDomain.NONE_MONOTONIC_FALLBACK),
            "coverage-incomplete" to evalAt(listOf(clean, clean, clean, null)),
            "over-boundary (0.60)" to evalAt(listOf(clean, clean, excluded, excluded, excluded)),
            "combined all-three" to evalAt(listOf(excluded, excluded, excluded, clean, null), NON_AUTH_CAP, ClockDomain.NONE_MONOTONIC_FALLBACK),
            "empty cells (NotCaptured)" to evalAt(emptyList()),
        )
        var nonEligible = 0
        for ((label, eval) in attacks) {
            assertFalse(eval.rtShadowVerdict.isEligible(), "adversary attack '$label' was Eligible — half is not discriminating")
            nonEligible++
        }
        assertEquals(EXPECTED_ATTACKS, nonEligible, "adversary-half cardinality drifted")
    }

    /**
     * Anti-gaming (discards): opening a window and pausing it records a PAUSE_DISCARD that RAISES the excluded
     * fraction — a paused/interrupted window can never be laundered into LOWER exclusion. Uses the proven
     * single-discard mechanism (2 clean + 1 open-then-pause => 1/3), asserting only the direction (strictly up).
     */
    @Test
    fun aPauseDiscardRaisesExclusion_neverLowersIt() {
        val honest = evalAt(listOf(clean, clean))
        assertEquals(0.0, honest.excludedFraction ?: -1.0, 0.0, "honest baseline had exclusion")
        var s = sessionOf(listOf(clean, clean))
        s = FocusSessionReducer.reduce(
            s, FocusRuntimeInput.PresentTrial(ADVERSARY_OPEN_AT, WINDOW_NANOS, PLANNED, ConditionId.CONGRUENT), spec,
        ).snapshot
        s = FocusSessionReducer.reduce(s, FocusRuntimeInput.Pause(ADVERSARY_OPEN_AT + PAUSE_DELTA_NANOS), spec).snapshot
        val withDiscard = FocusTemporalEligibility.evaluate(
            ProbeCapability.ANDROID_CHOREOGRAPHER, ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME, s,
        )
        assertTrue(
            (withDiscard.excludedFraction ?: 0.0) > (honest.excludedFraction ?: 1.0),
            "a pause-discard failed to raise the excluded fraction (anti-gaming property broken)",
        )
    }

    // ================================================================================================
    // Reachable codomain + all causes are exercised. The codomain assertion is an EXHAUSTIVE ALLOWLIST
    // (set equality), not a denylist: it already forbids any verdict kind nobody decided to exercise,
    // which is why deleting the producerless `Degraded` tier (`MB-FOCUS-0080`) cost no coverage — the
    // `!is Verdict.Degraded` line it used to carry was a denylist of one, strictly weaker than this.
    // ================================================================================================
    @Test
    fun reachableCodomainAndAllCauses_areExercised() {
        val kinds = mutableSetOf<String>()
        val causes = mutableSetOf<ExclusionCause>()
        var evaluations = 0
        kinds += evalAt(emptyList()).rtShadowVerdict.kind() // NotCaptured (empty cells)
        kinds += evalAt(listOf(clean, clean), capability = null, clock = ClockDomain.UNKNOWN).rtShadowVerdict.kind()
        kinds += evalAt(List(MIN_WINDOWS + 1) { clean }).rtShadowVerdict.kind() // Eligible
        evaluations += 3
        val caps = listOf(
            ProbeCapability.ANDROID_CHOREOGRAPHER to ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME,
            ProbeCapability.NONE_MONOTONIC_FALLBACK to ClockDomain.NONE_MONOTONIC_FALLBACK,
        )
        for ((cap, clock) in caps) {
            for (n in MIN_WINDOWS..MAX_WINDOWS) {
                for (k in 0..n) {
                    val verdicts = List(n) {
                        when {
                            it < k -> excluded
                            it == n - 1 && k < n -> null // a null window => incomplete coverage when not fully excluded
                            else -> clean
                        }
                    }
                    val v = FocusTemporalEligibility.evaluate(cap, clock, sessionOf(verdicts)).rtShadowVerdict
                    kinds += v.kind()
                    if (v is Verdict.Excluded) causes += v.causes
                    evaluations++
                }
            }
        }
        assertTrue(evaluations >= MIN_CODOMAIN_DOMAIN, "domain too small: $evaluations")
        assertEquals(
            setOf("Eligible", "Excluded", "NotCaptured"), kinds,
            "reachable codomain not fully exercised (a dropped verdict kind would slip through): $kinds",
        )
        assertEquals(
            setOf(
                ExclusionCause.NON_AUTHORITATIVE_CLASS,
                ExclusionCause.COVERAGE_INCOMPLETE,
                ExclusionCause.SESSION_BACKSTOP_EXCEEDED,
            ),
            causes,
            "not all exclusion causes exercised (a backstop-only regression would slip through): $causes",
        )
    }

    // ================================================================================================
    // NON-INTERFERENCE — outcome/RT never move eligibility, across the domain (not one pair).
    // Both arms answer WITHIN the window (fast vs slow HIT/false-alarm); the true no-response path is
    // covered by the sibling runtime tests.
    // ================================================================================================
    @Test
    fun outcomeAndRt_neverMoveEligibility_acrossTheDomain() {
        var pairs = 0
        var eligibleSeen = 0
        var excludedSeen = 0
        for (n in MIN_WINDOWS..MAX_WINDOWS) {
            for (k in 0..n) {
                val verdicts = List(n) { if (it < k) excluded else clean }
                val cap = ProbeCapability.ANDROID_CHOREOGRAPHER
                val clock = ClockDomain.ANDROID_CHOREOGRAPHER_UPTIME
                val fast = FocusTemporalEligibility.evaluate(cap, clock, sessionOf(verdicts, Response.LEFT, FAST_RT))
                val slow = FocusTemporalEligibility.evaluate(cap, clock, sessionOf(verdicts, Response.RIGHT, SLOW_RT))
                assertEquals(fast.rtShadowVerdict, slow.rtShadowVerdict, "outcome/RT moved the verdict at n=$n k=$k")
                assertEquals(fast.excludedFraction, slow.excludedFraction, "outcome/RT moved the fraction at n=$n k=$k")
                if (fast.rtShadowVerdict.isEligible()) eligibleSeen++ else excludedSeen++
                pairs++
            }
        }
        assertTrue(pairs >= MIN_CHAINS, "too few non-interference pairs: $pairs")
        assertTrue(eligibleSeen > 0 && excludedSeen > 0, "non-interference domain not discriminating")
    }

    // ================================================================================================
    // FASE H (C4) — the INPUT domain. Every phase above pins what comes OUT of `evaluate()`; none of them
    // checked what goes IN, and what went in was {Clean, Excluded, null}. `NoObservation` and
    // `DegradedUsable` — half the sealed hierarchy, and the half the iOS probe produces for every single
    // window — reached this SUT from no test at all. Pinning a codomain while starving the domain is the
    // charter's own "discriminant half" rule applied backwards, and it is exactly how an all-unobserved
    // session came out Eligible through a green build.
    // ================================================================================================

    /**
     * The domain key of a window verdict, as an EXHAUSTIVE `when`.
     *
     * The exhaustiveness is the mechanism, not a formality: add a variant to [TemporalWindowVerdict] and
     * this stops COMPILING, so the new variant cannot be left unfed the way these two were. A reflective
     * `sealedSubclasses` check would have been a runtime assertion; this is a build-time one.
     */
    private fun TemporalWindowVerdict.domainKey(): String = when (this) {
        TemporalWindowVerdict.Clean -> "Clean"
        TemporalWindowVerdict.DegradedUsable -> "DegradedUsable"
        TemporalWindowVerdict.NoObservation -> "NoObservation"
        is TemporalWindowVerdict.Excluded -> "Excluded"
    }

    @Test
    fun theOracleFeedsEveryWindowVerdict_notOnlyTheTwoItUsedTo() {
        val fed = FULL_WINDOW_DOMAIN.map { it?.domainKey() ?: "unclassified" }.toSet()
        assertEquals(
            setOf("Clean", "DegradedUsable", "NoObservation", "Excluded", "unclassified"),
            fed,
            "the oracle stopped feeding part of the window-verdict domain",
        )
        // And every one of them must actually reach evaluate() — a domain listed but never evaluated is
        // the same vacuity one indirection later.
        // `reached.size == FULL_WINDOW_DOMAIN.size` was a TAUTOLOGY: `map` preserves size, so it
        // restated the very list it derived from. What can actually be FALSE is the CONTENT — that
        // feeding different window verdicts does not collapse onto one session verdict. An
        // `evaluate()` returning NotCaptured for every input satisfied the old assertion.
        val reached = FULL_WINDOW_DOMAIN.associate { v ->
            (v?.domainKey() ?: "unclassified") to evalAt(List(MIN_WINDOWS + 1) { v }).rtShadowVerdict.kind()
        }
        assertTrue(reached.values.toSet().size >= 2, "the whole window domain collapsed to one verdict: $reached")
        assertEquals("Eligible", reached.getValue("Clean"), "an all-clean session must stay eligible")
        assertNotEquals(
            "Eligible",
            reached.getValue("NoObservation"),
            "an unobserved session must not reach an eligible shadow verdict",
        )
    }

    @Test
    fun aSessionThatClassifiedNothing_isNeverEligible_throughTheReducer() {
        // C4-01 driven through the real reducer rather than a hand-built evaluation: the frames still
        // accumulate (the floor is met, the class stays T1), and every window is unobservable.
        val evaluation = evalAt(List(MAX_WINDOWS) { TemporalWindowVerdict.NoObservation })
        assertEquals(TemporalClass.T1_FRAME_OBSERVED, evaluation.sessionClass, "class must stay authoritative")
        assertEquals(0, evaluation.windowsClassified)
        assertEquals(MAX_WINDOWS, evaluation.windowsUnobserved)
        assertFalse(evaluation.coverageComplete)
        assertNull(evaluation.excludedFraction)
        val verdict = assertIs<Verdict.Excluded>(evaluation.rtShadowVerdict)
        assertEquals(
            setOf(ExclusionCause.COVERAGE_INCOMPLETE, ExclusionCause.SESSION_BACKSTOP_EXCEEDED),
            verdict.causes,
        )
    }

    @Test
    fun addingUnobservedWindows_isMonotone_neverReopensEligibility() {
        // The monotonicity property of Fase B, on the axis Fase B could not see. Substituting observation
        // away is one-sided: it can only ever remove what we know, so eligibility must never return.
        var sawExcluded = false
        var lastClassified = Int.MAX_VALUE
        for (unobserved in 0..MAX_WINDOWS) {
            val verdicts = List(MAX_WINDOWS) { if (it < unobserved) TemporalWindowVerdict.NoObservation else clean }
            val e = evalAt(verdicts)
            assertTrue(
                e.windowsClassified <= lastClassified,
                "classified count rose while observation was being removed: ${e.windowsClassified}",
            )
            lastClassified = e.windowsClassified
            assertEquals(unobserved, e.windowsUnobserved)
            if (e.rtShadowVerdict is Verdict.Excluded) sawExcluded = true
            if (sawExcluded) {
                assertFalse(
                    e.rtShadowVerdict.isEligible(),
                    "removing observation re-opened eligibility at unobserved=$unobserved",
                )
            }
        }
        assertTrue(sawExcluded, "the chain never reached an excluded state — the property was vacuous")
    }

    @Test
    fun unobservedWindowsDoNotInflateTheExcludedFraction_discriminantHalf() {
        // Without this half, "treat unobserved as excluded" would also pass the two tests above while
        // making an unobservable platform indistinguishable from a degraded one. The fraction must keep
        // answering only "of what we DID classify, how much was bad".
        val evaluation = evalAt(
            listOf(clean, clean, clean, TemporalWindowVerdict.NoObservation),
        )
        assertEquals(3, evaluation.windowsClassified)
        assertEquals(1, evaluation.windowsUnobserved)
        assertEquals(0.0, evaluation.excludedFraction)
        val verdict = assertIs<Verdict.Excluded>(evaluation.rtShadowVerdict)
        assertEquals(setOf(ExclusionCause.COVERAGE_INCOMPLETE), verdict.causes)
    }

    @Test
    fun degradedUsableIsIndistinguishableFromClean_overTheWholeSweep() {
        // `MB-FOCUS-0076`, stated as the property it actually is instead of a single case.
        //
        // The first version of this test asserted the numbers of an all-DegradedUsable session and
        // stopped. An all-Clean session yields those same numbers, so it never demonstrated the claim —
        // a DISTINCTION WITHOUT CONSEQUENCE. The claim IS the equality, so the equality is what gets
        // asserted, over every session size this oracle sweeps. `TemporalEvaluation` is a data class,
        // so this compares every field including the verdict and its causes.
        //
        // It doubles as the guard: the live contract (R3-11, `D-R3-02`) keeps temporal diagnostics in
        // SHADOW with no measurement evidence in S5, and the
        // day someone implements it this goes RED and forces the KDoc on
        // `TemporalWindowVerdict.DegradedUsable` to be rewritten in the same change.
        var largestSweptSize = 0
        for (n in MIN_WINDOWS..MAX_WINDOWS) {
            val degraded = evalAt(List(n) { TemporalWindowVerdict.DegradedUsable })
            assertEquals(
                evalAt(List(n) { TemporalWindowVerdict.Clean }),
                degraded,
                "n=$n — a degraded window must still be indistinguishable from a clean one in S5",
            )
            // Discriminant half, and the reason the KDoc no longer puts these two in one clause: the
            // unobserved window is not a classification, so it DOES constrain. If this ever holds, the
            // two have been re-coupled and the doc is wrong again.
            assertNotEquals(
                degraded,
                evalAt(List(n) { TemporalWindowVerdict.NoObservation }),
                "n=$n — NoObservation must not collapse into the DegradedUsable behaviour",
            )
            // Anti-vacuity that asserts CONTENT, not the loop bounds: counting iterations only
            // restates the range. What has to hold is that the row actually EXERCISED the bucket —
            // a session collapsed to T0 satisfies the equality for the WRONG reason (both arms
            // excluded by session class, not by the bucket having no consequence). This sweep used
            // to start at n=1, which was exactly that: one window is FRAMES_PER_WINDOW frames, below
            // `MIN_FRAMES_SESSION`, so both arms were T0 and the row proved nothing.
            assertEquals(n, degraded.windowsClassified, "n=$n - the degraded windows must be classified, not collapsed")
            assertIs<Verdict.Eligible>(degraded.rtShadowVerdict, "n=$n - the row must reach an eligible verdict")
            // And the property is not just about HOMOGENEOUS sessions: swapping an arbitrary SUBSET
            // of clean windows for degraded ones must not move a single field either. A pin that only
            // ever compares all-degraded against all-clean would still pass if the bucket acquired a
            // consequence that happens to be uniform across the session.
            val mixed = evalAt(List(n) { i ->
                if (i % 2 == 0) TemporalWindowVerdict.DegradedUsable else TemporalWindowVerdict.Clean
            })
            assertEquals(
                evalAt(List(n) { TemporalWindowVerdict.Clean }),
                mixed,
                "n=$n - a session that MIXES degraded and clean windows must be indistinguishable too",
            )
            largestSweptSize = n
        }
        assertEquals(MAX_WINDOWS, largestSweptSize, "guard: the sweep must reach the largest session it covers")
    }

    private data class TruthRow(
        val name: String,
        val verdicts: List<TemporalWindowVerdict?>,
        val capability: ProbeCapability?,
        val expectedCauses: Set<ExclusionCause>,
        val expectedEvidence: FocusEvidenceState,
    )

    private companion object {
        val AUTH: ProbeCapability = ProbeCapability.ANDROID_CHOREOGRAPHER
        val NON_AUTH_CAP: ProbeCapability = ProbeCapability.NONE_MONOTONIC_FALLBACK

        /** Every window verdict this SUT can be fed, plus the unclassified (probe-off) window. */
        val FULL_WINDOW_DOMAIN: List<TemporalWindowVerdict?> = listOf(
            TemporalWindowVerdict.Clean,
            TemporalWindowVerdict.DegradedUsable,
            TemporalWindowVerdict.NoObservation,
            TemporalWindowVerdict.Excluded(WindowExclusionReason.TRUNCATION_RISK),
            null,
        )

        const val START_AT = 1_000L
        const val ADVERSARY_OPEN_AT = 90_000_000_000L
        const val WINDOW_NANOS = 1_200_000_000L
        const val PLANNED = 50
        const val RT_NANOS = 400_000_000L
        const val STEP_NANOS = 2_000_000_000L
        const val FRAMES_PER_WINDOW = 60
        const val PAUSE_DELTA_NANOS = 100_000L
        const val FAST_RT = 200_000_000L
        const val SLOW_RT = 900_000_000L

        const val MIN_WINDOWS = 2
        const val MAX_WINDOWS = 6
        const val MIN_CHAINS = 5
        const val MIN_CODOMAIN_DOMAIN = 30
        const val EXPECTED_COMPARABLE_PAIRS = 80 // sum over n=2..6 of C(n+2,2) = 6+10+15+21+28
        const val EXPECTED_TRUTH_ROWS = 8
        const val EXPECTED_EVIDENCE_EVALS = 32 // 2 writeAhead × 2 dose × 2 invalid × 4 temporal samples
        const val EXPECTED_ATTACKS = 6
        const val BACKSTOP_MAX_DEFAULT = 0.50
        const val FRACTION_060 = 0.6
        const val RELAXED_MAX = 0.66
        const val EPS = 1e-9
    }
}
