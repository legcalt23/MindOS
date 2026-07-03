package com.mindos.app.features.mindboost.focus.domain.session

import com.mindos.app.features.mindboost.focus.domain.metrics.MetricKey
import com.mindos.app.features.mindboost.focus.domain.metrics.MetricValue
import com.mindos.app.features.mindboost.focus.domain.personalization.PersonalizedScore

/**
 * An honest, user-facing summary line. Every headline number carries its
 * confidence and a plain caption so the UI never presents an under-powered
 * estimate as a hard fact (09_ui_ux §5).
 */
data class ReportLine(
    val key: MetricKey,
    val label: String,
    val metric: MetricValue,
    val caption: String,
)

/**
 * The honest report shown after a session. Intra-exercise progress is secondary;
 * the strong evidence of real change comes from un-trained probes (evaluation),
 * which is surfaced separately when available.
 */
data class FocusReport(
    val headline: List<ReportLine>,
    /** Plain text acknowledging uncertainty / the limits of what can be claimed. */
    val honestNote: String,
    /**
     * Archetype-personalized performance score for this session (0..100), bent by the user's
     * [com.mindos.app.features.mindboost.focus.domain.personalization.ScoringModifiers]. Null when no
     * archetype scoring is applied. Always confidence-gated and non-diagnostic.
     */
    val personalizedScore: PersonalizedScore? = null,
)
