package com.mindos.app.features.mindboost.creativity.domain.model

import kotlinx.serialization.Serializable

/**
 * Safe, non-clinical labels for local practice feedback.
 *
 * Framed strictly as practice progress against the user's own session — never as a
 * diagnosis, intelligence claim, or clinical inference. Modeled as a closed enum so
 * free-form (potentially unsafe) summary text can never be produced.
 */
@Serializable
enum class CreativityPracticeLabel {
    KEEP_PRACTICING,
    STEADY_PRACTICE,
    STRONG_LOCAL_SESSION,
}

/**
 * Output of a local, versioned, deterministic rubric over a [CreativityResponse].
 *
 * [signals] are local practice telemetry (e.g. fluency, elaboration proxies), NOT clinical
 * or graded measures. [aiDerived] is always `false` in the MVP and the [init] block enforces
 * it — no AI-derived field may ever be populated here.
 */
@Serializable
data class CreativityRubricResult(
    val rubricVersion: Int,
    val signals: Map<String, Double>,
    val summary: CreativityPracticeLabel,
    val aiDerived: Boolean = false,
) {
    init {
        require(rubricVersion >= 1) { "rubricVersion must be >= 1" }
        require(!aiDerived) { "MVP rubric results must never be AI-derived" }
    }
}
