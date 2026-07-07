package com.mindos.app.features.mindboost.emotions.domain.model

/** Confidence tier driven by completed sessions (decision F). */
enum class ProfileTier { EXPLORATORY, PRELIMINARY, INITIAL_TREND, STABLE }

/** Direction of a score's recent trend; UNKNOWN until enough confident data. */
enum class ProfileTrend { UNKNOWN, IMPROVING, STEADY, DECLINING }

/**
 * Honest cap on transfer claims. Never stronger than NOT_DEMONSTRATED below the STABLE tier, regardless
 * of any single exercise's label.
 *
 * MC8u (P0-3): the former `EmotionalRegulationProfile` 10-competence-score data class was DELETED here.
 * It was dead code (never constructed) and a standing regression risk — wiring block metrics straight into
 * per-competence "scores" would convert gameable/low-n/chance-inflated signals into a judgment of the user's
 * emotional worth (charter §2 violation). Emociones has NO per-competence emotional score model by design;
 * calibration is session-COUNT only (see EmotionAggregateFactory) and the brain-profile contribution stays
 * CALIBRATING. A source-scan guard test (EmotionalRegulationProfileAbsenceTest) fails the build if the type
 * ever reappears in production.
 */
enum class TransferConfidence { NONE, NOT_DEMONSTRATED, LIMITED }
