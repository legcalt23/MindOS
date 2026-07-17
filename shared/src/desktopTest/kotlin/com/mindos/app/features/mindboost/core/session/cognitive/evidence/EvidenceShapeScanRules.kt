package com.mindos.app.features.mindboost.core.session.cognitive.evidence

/**
 * SYSTEM-1 6R3 (§10 permanent mutation-fixture artifact) · A PURE, desktopTest-only mirror of the Tier-1/Tier-2
 * rule list the Gradle `evidenceBoundaryScanner` task (`shared/build.gradle.kts`) applies over the REAL
 * `core.session.cognitive.evidence` tree. Unlike the Gradle task (which walks real files) or
 * [CognitiveSessionEvidenceBoundaryTest] (which scans the real tree), [scanText] takes an already-in-memory
 * SYNTHETIC snippet — this is what lets [EvidenceShapeScanRulesTest] prove the rule set actually discriminates a
 * forbidden evaluative shape from a legitimate facts-only one WITHOUT mutating the real evidence package (that
 * other mutation proof — reintroduce a forbidden field in the real tree, watch the Gradle task+test go RED, revert
 * — is a one-time, Gradle-only procedure documented for the CEO; see the 6-R3 dispatch report).
 *
 * The literal rule lists here are a deliberate COPY of the Gradle task's, not a shared reference across build
 * units (Gradle's `buildSrc`-less Kotlin DSL task and this JVM test source set are different compilation units) —
 * a change to one is a visible diff against the others, never a silent single-point-of-truth edit.
 * [CognitiveSessionEvidenceBoundaryTest] consolidates onto THIS object for its own two lists (same desktopTest
 * compilation unit, same package, zero behavior change) so the two TEST-SIDE checks (real-tree scan + synthetic
 * mutation fixture) cannot drift from each other, while the Gradle task keeps its own independent copy —
 * preserving the Gradle-vs-test "twin" defense-in-depth the reopen doc §10 calls for.
 */
object EvidenceShapeScanRules {

    /** 6R3 §10/§18-C3 Tier 1: type tokens an evaluative ratio/score (or an opaque keyed/type-erased escape hatch)
     *  needs to exist at all — structural, forward-proof. */
    val tier1BannedTypeTokens: List<String> = listOf(
        "Double", "Float", "DoubleArray", "FloatArray", "Duration", "typealias", "Map", "Any",
    )

    /**
     * 6R3 §10 Tier 2: the SPECIFIC retired field/type names the psychometrician found reconstructible into a
     * profile proxy from the contract's shipped fields alone. `spanMs` is deliberately ABSENT (kept on
     * [ExerciseScaleEvidence] — see the reopen doc §10). §18 C2.3 added the retired block-readiness-for-ranking
     * vocabulary.
     */
    val tier2RetiredEvaluativeLiterals: List<String> = listOf(
        "EvidenceQuantityMaturity",
        "EvidenceQuantityPolicy",
        "FreshnessStatus",
        "FreshnessPolicy",
        "cappedObservationCount",
        "observationCountProgress",
        "distinctDayProgress",
        "orderedNormalizedScores",
        "COMPARABLE_WITHIN_SCALE",
        "rankSessionCount",
        "acceleratingSessionCount",
        "distinctDays",
        "RankReadiness",
        "RankPolicy",
        "RankBlockReason",
        "blockingReasons",
    )

    private val banned: List<String> = tier1BannedTypeTokens + tier2RetiredEvaluativeLiterals

    /**
     * Every banned token that word-boundary-matches somewhere in [text] (whole text, comments included — mirrors
     * the real scanner's anti-negation stance: naming a banned symbol even to discuss/negate it still trips).
     * Empty result = clean. [fileName] is a cosmetic label carried into each offender string (useful when a
     * caller batches several synthetic snippets); it never affects matching.
     */
    fun scanText(text: String, fileName: String = "Synthetic.kt"): List<String> =
        banned
            .filter { Regex("\\b" + Regex.escape(it) + "\\b").containsMatchIn(text) }
            .map { "$fileName -> $it" }
}
