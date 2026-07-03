package com.mindos.app.features.mindboost.profile.domain.calibration

import com.mindos.app.features.mindboost.profile.domain.model.DimensionStatus
import com.mindos.app.features.mindboost.profile.domain.model.SessionEvidence

/**
 * One session's calibration descriptor — transient input to [CalibrationPolicy.summarize]. A source builds
 * these from its own session list; they are NEVER persisted in `ModuleContribution` (only the reduced
 * [SessionEvidence] counts are). [valid] is the source's best available "counts as a session" signal
 * (completed + long enough + not anomalous); [dayEpoch] is a coarse distinct-day key.
 */
data class CalibrationSession(
    val sessionId: String,
    val dayEpoch: Long,
    val valid: Boolean,
)

/**
 * DC-3 upstream calibration authority — the ONLY place the "≥N valid sessions cross-session" maturity
 * clause lives. Pure and fully testable. It owns BOTH the counting rules ([summarize]: keep only valid
 * sessions, de-duplicate by id, count distinct days) and the maturity thresholds ([eligibleCeiling]).
 *
 * A dimension's SESSION-COUNT maturity is capped here, upstream, so the display layer
 * (`UserDisplayScoreMapper`) stays simple (DimensionStatus + confidence only) and never re-derives
 * maturity from history. This enforces DC-3 for SESSION-BASED sources only.
 *
 * Claim boundary — enforced TODAY for the active session-based sources that emit [SessionEvidence]:
 * Enfoque (ATTENTION) and Lógica (REASONING). The permitted claim is "DC-3 enforced for active
 * session-based profile sources (Enfoque/Lógica)", NEVER "DC-3 globally enforced". Session-based sources
 * NOT yet gated (they emit no evidence): Lenguaje (unwired in production today) and Decisiones (via
 * ProfileUpdateGate, currently EMERGING-capped by confidence). Trial-accumulation modules (Memoria) have
 * no session concept and are excluded by design — inferring sessions from an aggregated trial log would
 * fabricate the temporal unit.
 *
 * Follow-ups (documented, not this slice): (1) Lenguaje/Decisiones must emit [SessionEvidence] before they
 * may aspire to ESTABLISHED; (2) when a second module maps to one session-based dimension, define the
 * ceiling semantics for mixed evidence-present/absent contributions (today each active session-based
 * dimension has exactly one contributing module, so [MindBoostProfileAggregator] taking the dominant
 * contribution's evidence is unambiguous).
 */
object CalibrationPolicy {

    /** ≥ this many valid cross-session administrations to leave CALIBRATING for EMERGING (DC-3). */
    const val EMERGING_MIN_SESSIONS: Int = 2

    /** ≥ this many valid sessions to be eligible for ESTABLISHED (DC-3), together with the day spread. */
    const val ESTABLISHED_MIN_SESSIONS: Int = 5

    /** ≥ this many distinct days the valid sessions must span to be eligible for ESTABLISHED (DC-3). */
    const val ESTABLISHED_MIN_DISTINCT_DAYS: Int = 3

    // Maturity from lowest to highest — the single ordering used to cap a candidate status.
    private val MATURITY_LOW_TO_HIGH =
        listOf(DimensionStatus.CALIBRATING, DimensionStatus.EMERGING, DimensionStatus.ESTABLISHED)

    /** Reduce raw session descriptors to coarse evidence: keep valid, de-dup by id, count distinct days. */
    fun summarize(sessions: List<CalibrationSession>): SessionEvidence {
        val valid = sessions.filter { it.valid }.distinctBy { it.sessionId }
        return SessionEvidence(
            validSessions = valid.size,
            distinctDays = valid.map { it.dayEpoch }.distinct().size,
        )
    }

    /** The MAXIMUM [DimensionStatus] the cross-session evidence permits (a ceiling; never a floor). */
    fun eligibleCeiling(evidence: SessionEvidence): DimensionStatus = when {
        evidence.validSessions >= ESTABLISHED_MIN_SESSIONS &&
            evidence.distinctDays >= ESTABLISHED_MIN_DISTINCT_DAYS -> DimensionStatus.ESTABLISHED
        evidence.validSessions >= EMERGING_MIN_SESSIONS -> DimensionStatus.EMERGING
        else -> DimensionStatus.CALIBRATING
    }

    /** Cap [candidate] (the confidence-derived status) to what the session evidence permits — the lower wins. */
    fun applyCeiling(candidate: DimensionStatus, evidence: SessionEvidence): DimensionStatus {
        val ceiling = eligibleCeiling(evidence)
        return if (MATURITY_LOW_TO_HIGH.indexOf(ceiling) < MATURITY_LOW_TO_HIGH.indexOf(candidate)) {
            ceiling
        } else {
            candidate
        }
    }
}
