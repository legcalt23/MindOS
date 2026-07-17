package com.mindos.app.features.mindboost.core.session.cognitive.evidence

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId
import com.mindos.app.features.mindboost.core.session.cognitive.CognitiveSessionRun

/**
 * SYSTEM-1 6R3 · The PURE, NOT-WIRED contract that turns a subject-scoped [CognitiveSessionRun] history into
 * DESCRIPTIVE, PER-EXERCISE-SCALE EVIDENCE for ONE block. It NEVER produces a block estimate, an aggregator-shaped
 * profile-currency contribution, a calibration status, a confidence, or a trend; it NEVER invokes an aggregator,
 * mutates a profile, updates a live rank/calibration, or touches Firestore/SyncQueue. It lives in `core` and
 * imports NO `profile.*` type — the boundary scanner test fails the build if that changes.
 *
 * Guarantees: absence stays absence (an empty batch yields empty [CognitiveSessionEvidence.scaleGroups], never a
 * fabricated value); every distinct score scale is preserved as its OWN group (no recency displacement, P1-04/05);
 * eligibility is RE-DERIVED from raw facts, never trusted from the forgeable eligibility booleans (P1-06); the
 * output is order-independent (no fold, so no tie-break artifact, P1-10); identity conflicts and future stamps are
 * excluded and intrinsically-inconsistent times are quarantined (P1-09); the audit's counts are conservation-
 * checked (P1-12); and — as of 6R3 §18 C2.3 — this contract ships NO block-readiness-for-ranking vocabulary at
 * all (not a count, not a size, not a distance-to-threshold). Per 6R3 §7 it ships raw counts only — no
 * maturity/progress band, no recency verdict, and no raw per-session score list.
 *
 * The [SubjectScopedSessionBatch] input (never a bare `List`) makes the "one subject" contract explicit at the
 * type level (P1-07 — see that type's honest limitation), [EvaluationAsOf] makes the clock an injected,
 * provenance-tagged value (P1-09), and [RtCriticalityRegistry] (§18 C4) makes the per-exercise timing-sensitivity
 * authority an injected, testable seam too — all three default to their fail-closed production values, so every
 * existing caller is unaffected.
 */
object CognitiveSessionEvidenceExtractor {

    /** Versions this extraction rule; surfaced in [SessionEvidenceAudit.policyVersions]. */
    const val RULE_VERSION: String = "cognitive-session-evidence-v2"

    /** Frozen: the per-session evidence-quantity band's rule version before 6R3 retired the band itself as an
     *  evaluative output (see the slice reopen doc §7); kept only so a historical [PolicyVersionBundle] stays
     *  honestly attributable, never re-derived as a live computation. */
    private const val RETIRED_EVIDENCE_QUANTITY_RULE_VERSION: String = "evidence-quantity-v1"

    /** Frozen: the retired block-readiness-for-ranking rule's version before 6R3 §18 C2.3 removed that vocabulary
     *  entirely from this contract's public shape; kept only so a historical [PolicyVersionBundle] stays honestly
     *  attributable, never re-derived as a live computation (there is no live source for it anymore). */
    private const val RETIRED_BLOCK_READINESS_RULE_VERSION: String = "rank-policy-v1"

    /** Frozen: the per-lane recency verdict's rule version before 6R3 retired the verdict itself as an evaluative
     *  output (see the slice reopen doc §7); kept only so a historical [PolicyVersionBundle] stays honestly
     *  attributable, never re-derived as a live computation. */
    private const val RETIRED_RECENCY_RULE_VERSION: String = "freshness-policy-v1"

    private data class Dedup(
        val survivors: List<CognitiveSessionRun>,
        val duplicateCollapsedCount: Int,
        val conflictRunCount: Int,
    )

    private data class Disposition(
        val eligible: List<CognitiveSessionRun>,
        val excludedByReason: List<ExclusionCount>,
    )

    /**
     * Extract evidence for [blockId] from one subject's [batch]. [asOf] is a required injected clock reading;
     * [registry] defaults to the fail-closed PRODUCTION [RtCriticalityRegistry] (§18 C4) — a caller may inject a
     * different one (today, only tests do).
     */
    fun extractEvidence(
        blockId: ScoreModuleId,
        batch: SubjectScopedSessionBatch,
        asOf: EvaluationAsOf,
        registry: RtCriticalityRegistry = RtCriticalityRegistry(),
    ): CognitiveSessionEvidence {
        val runs = batch.runs
        val forBlock = runs.filter { it.blockId == blockId }
        val dedup = dedupe(forBlock)
        val disposition = disposeSurvivors(dedup.survivors, asOf, registry)
        val groups = buildScaleGroups(disposition.eligible)
        val crossScaleComparability = if (groups.size >= 2) ComparabilityVerdict.Incompatible else null
        val audit = auditOf(runs.size, forBlock.size, dedup, disposition, groups.size)
        return CognitiveSessionEvidence(blockId, groups, crossScaleComparability, audit, asOf.provenance)
    }

    // Collapse IDENTICAL duplicate sessionIds to one; a sessionId with DIFFERING payloads is a conflict → drop ALL
    // its runs (fail-closed, counting runs not ids).
    private fun dedupe(forBlock: List<CognitiveSessionRun>): Dedup {
        val byId = forBlock.groupBy { it.sessionId }
        val identical = byId.filterValues { it.distinct().size == 1 }
        val conflicting = byId.filterValues { it.distinct().size > 1 }
        return Dedup(
            survivors = identical.map { it.value.first() },
            duplicateCollapsedCount = identical.values.sumOf { it.size - 1 },
            conflictRunCount = conflicting.values.sumOf { it.size },
        )
    }

    // Each survivor gets exactly one disposition: intrinsic time inconsistency (quarantine, permanent) →
    // future-relative-to-now → a quality/purpose exclusion → eligible.
    private fun disposeSurvivors(
        survivors: List<CognitiveSessionRun>,
        asOf: EvaluationAsOf,
        registry: RtCriticalityRegistry,
    ): Disposition {
        val eligible = mutableListOf<CognitiveSessionRun>()
        val excluded = LinkedHashMap<ExclusionReason, Int>()
        fun bump(reason: ExclusionReason) { excluded[reason] = (excluded[reason] ?: 0) + 1 }
        survivors.forEach { run ->
            when {
                SessionEvidenceGates.isTimeInconsistent(run) -> bump(ExclusionReason.QUARANTINED_INCONSISTENT_TIME)
                SessionEvidenceGates.orderKey(run) > asOf.epochMs -> bump(ExclusionReason.FUTURE_TIMESTAMP)
                else -> SessionEvidenceGates.qualifyingExclusion(run, registry)?.let(::bump) ?: eligible.add(run)
            }
        }
        val excludedCounts = excluded.entries.sortedBy { it.key.ordinal }.map { ExclusionCount(it.key, it.value) }
        return Disposition(eligible, excludedCounts)
    }

    private fun buildScaleGroups(eligible: List<CognitiveSessionRun>): List<ExerciseScaleEvidence> =
        eligible.groupBy { SessionEvidenceGates.scaleKey(it) }
            .map { (key, groupRuns) -> buildScaleEvidence(key, groupRuns) }
            .sortedWith(
                compareBy(
                    { it.lineage.scaleKey.exerciseId },
                    { it.lineage.scaleKey.exerciseVersion },
                    { it.lineage.scaleKey.scoringVersion },
                ),
            )

    private fun buildScaleEvidence(key: ScoreScaleKey, groupRuns: List<CognitiveSessionRun>): ExerciseScaleEvidence {
        val sorted = groupRuns.sortedWith(compareBy({ SessionEvidenceGates.orderKey(it) }, { it.sessionId }))
        val distinctDayCount = sorted.map { SessionEvidenceGates.dayEpoch(it) }.distinct().size
        return ExerciseScaleEvidence(
            lineage = MeasurementLineage(key, ComparabilityVerdict.ComparabilityNotEstablished, LineagePolicy.VERSION),
            observationCount = sorted.size,
            distinctDayCount = distinctDayCount,
            spanMs = spanOf(sorted),
        )
    }

    // Overflow-safe first→last span of an ascending-ordered list; null for < 2 points.
    private fun spanOf(ascending: List<CognitiveSessionRun>): Long? {
        if (ascending.size < 2) return null
        val first = SessionEvidenceGates.orderKey(ascending.first())
        val last = SessionEvidenceGates.orderKey(ascending.last())
        return (last - first).coerceAtLeast(0L)
    }

    private fun auditOf(
        rawInputCount: Int,
        forBlockCount: Int,
        dedup: Dedup,
        disposition: Disposition,
        distinctScaleCount: Int,
    ): SessionEvidenceAudit {
        val tally = LinkedHashMap<ExclusionReason, Int>()
        fun add(reason: ExclusionReason, count: Int) {
            if (count > 0) tally[reason] = (tally[reason] ?: 0) + count
        }
        add(ExclusionReason.NOT_FOR_BLOCK, rawInputCount - forBlockCount)
        add(ExclusionReason.IDENTITY_CONFLICT, dedup.conflictRunCount)
        disposition.excludedByReason.forEach { (reason, count) -> add(reason, count) }
        val exclusions = tally.entries.sortedBy { it.key.ordinal }.map { ExclusionCount(it.key, it.value) }
        val includedCount = disposition.eligible.size
        val (stateCounts, routeEligibilityCounts) = evidenceCountsOf(exclusions, includedCount)
        return SessionEvidenceAudit(
            rawInputCount = rawInputCount,
            forBlockCount = forBlockCount,
            uniqueIdentityCount = dedup.survivors.size,
            duplicateCollapsedCount = dedup.duplicateCollapsedCount,
            identityConflictRunCount = dedup.conflictRunCount,
            includedUniqueCount = includedCount,
            excludedUniqueCount = disposition.excludedByReason.sumOf { it.count },
            exclusions = exclusions,
            distinctScaleCount = distinctScaleCount,
            policyVersions = PolicyVersionBundle(
                evidenceRuleVersion = RULE_VERSION,
                lineagePolicyVersion = LineagePolicy.VERSION,
                evidenceQuantityPolicyVersion = RETIRED_EVIDENCE_QUANTITY_RULE_VERSION,
                rankPolicyVersion = RETIRED_BLOCK_READINESS_RULE_VERSION,
                freshnessPolicyVersion = RETIRED_RECENCY_RULE_VERSION,
            ),
            stateCounts = stateCounts,
            routeEligibilityCounts = routeEligibilityCounts,
        )
    }

    // §18 C2.1: re-bucket the SAME exclusion tally + included count through the SINGLE per-session authority
    // SessionEvidenceStatePolicy.dispositionOf (never a second, independently-derived source of truth). Both count
    // groups derive from ONE disposition per reason, so a state bucket and its route-eligibility twin can never
    // drift. Zero-count buckets are omitted, mirroring `exclusions`.
    private fun evidenceCountsOf(
        exclusions: List<ExclusionCount>,
        includedCount: Int,
    ): Pair<List<StateCount>, List<RouteEligibilityCount>> {
        val stateTotals = LinkedHashMap<SessionEvidenceState, Int>()
        val eligibilityTotals = LinkedHashMap<EvidenceRouteEligibility, Int>()
        fun bump(reason: ExclusionReason?, amount: Int) {
            val disposition = SessionEvidenceStatePolicy.dispositionOf(reason)
            stateTotals[disposition.state] = (stateTotals[disposition.state] ?: 0) + amount
            eligibilityTotals[disposition.routeEligibility] =
                (eligibilityTotals[disposition.routeEligibility] ?: 0) + amount
        }
        exclusions.forEach { bump(it.reason, it.count) }
        if (includedCount > 0) bump(null, includedCount)
        val stateCounts = stateTotals.entries.sortedBy { it.key.ordinal }.map { StateCount(it.key, it.value) }
        val routeEligibilityCounts = eligibilityTotals.entries
            .sortedBy { it.key.ordinal }
            .map { RouteEligibilityCount(it.key, it.value) }
        return stateCounts to routeEligibilityCounts
    }
}
