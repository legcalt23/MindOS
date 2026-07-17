package com.mindos.app.features.mindboost.core.session.cognitive.evidence

import com.mindos.app.features.mindboost.core.scoring.ScoreModuleId

/**
 * SYSTEM-1 slice 6-R2/6-R3 · The full EVIDENCE-ONLY output for ONE block. It reports, per exercise scale, HOW MUCH
 * quality-valid evidence exists ([scaleGroups] — one per [MeasurementLineage], ALL preserved, none discarded by
 * recency), the block-level [crossScaleComparability] signal, and a minimized-personal [audit]. It contains NO
 * block/dimension estimate, NO calibration status, NO confidence/sufficiency, NO profile taxonomy, and — as of
 * 6R3 §18 C2.3 — NO block-readiness-for-ranking vocabulary at all (not even a count or a distance-to-threshold):
 * those are profile/rank-lane INTERPRETATIONS of this evidence and belong to a separate, not-yet-built bridge that
 * would import THIS type, never the reverse (enforced by the boundary scanner test).
 *
 * [crossScaleComparability] is [ComparabilityVerdict.Incompatible] whenever [scaleGroups] holds 2+ groups (a
 * KNOWN cross-scale incompatibility — differing scale keys are never pooled) and `null` only when fewer than 2
 * groups exist (no pair to compare AT ALL, never an "unknown" verdict about a relationship that doesn't exist).
 * A single group's OWN within-scale ceiling stays on [ExerciseScaleEvidence.lineage.comparability].
 */
data class CognitiveSessionEvidence(
    val blockId: ScoreModuleId,
    val scaleGroups: List<ExerciseScaleEvidence>,
    val crossScaleComparability: ComparabilityVerdict?,
    val audit: SessionEvidenceAudit,
    val clockProvenance: ClockProvenance,
)
