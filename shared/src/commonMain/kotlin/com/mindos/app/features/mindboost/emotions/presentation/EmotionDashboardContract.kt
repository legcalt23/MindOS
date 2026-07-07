@file:Suppress("MaxLineLength") // Contract KDoc reads best whole.

package com.mindos.app.features.mindboost.emotions.presentation

/**
 * MC8u-D §6 — the dashboard/block-screen integration contract for Emociones. The global dashboard/hub is owned
 * OUTSIDE the emotions module, so this documents IN CODE the progress/status elements the main experience MUST
 * surface, and a test pins that [EmotionCalibrationProgressBuilder] already produces a model sufficient to render all
 * of them. A dashboard integration is therefore reduced to rendering the reusable model — the module never has to be
 * re-audited for what a premium cognitive trainer must show at a glance.
 */
object EmotionDashboardContract {

    /**
     * The elements the main experience must surface (block screen or dashboard). Each maps to a field the reusable
     * [EmotionCalibrationProgressUiModel] exposes, so a dashboard can render them without new module logic:
     *  - `blockScore` → blockScoreText (0–100 or "—")
     *  - `calibrationProgress` → progressText (0/5 → 5/5)
     *  - `provisionalOrOfficial` → statusLabel (never official before 5 valid sessions)
     *  - `sessionNotCounted` → notCountedNote
     *  - `lowConfidence` → lowConfidenceNote
     *  - `lastSession` → lastSessionText
     *  - `nextRecommendation` → nextRecommendationText
     *  - `nonClinicalDisclaimer` → disclaimer
     */
    val REQUIRED_SURFACE_ELEMENTS: Set<String> = setOf(
        "blockScore",
        "calibrationProgress",
        "provisionalOrOfficial",
        "sessionNotCounted",
        "lowConfidence",
        "lastSession",
        "nextRecommendation",
        "nonClinicalDisclaimer",
    )
}
