package com.mindos.app.features.mindboost.emotions.domain.model

import com.mindos.app.features.mindboost.emotions.domain.metrics.EmotionMetricValue

/** Confidence tier driven by completed sessions (decision F). */
enum class ProfileTier { EXPLORATORY, PRELIMINARY, INITIAL_TREND, STABLE }

/** Direction of a score's recent trend; UNKNOWN until enough confident data. */
enum class ProfileTrend { UNKNOWN, IMPROVING, STEADY, DECLINING }

/**
 * Honest cap on transfer claims. Never stronger than NOT_DEMONSTRATED below the STABLE tier, regardless
 * of any single exercise's label.
 */
enum class TransferConfidence { NONE, NOT_DEMONSTRATED, LIMITED }

/**
 * Functional regulation profile built ONLY from training data. NOT a diagnosis, health assessment, or
 * personality test. Visible name: "Perfil funcional de regulación en entrenamiento".
 *
 * Each score is a nullable [EmotionMetricValue] (carries n + confident). The presentation layer shows
 * nothing as a hard number below INITIAL_TREND or while `confident == false`.
 */
data class EmotionalRegulationProfile(
    val schemaVersion: Int,
    val sessionsCompleted: Int,
    val regulationStabilityScore: EmotionMetricValue?,
    val recoveryAfterErrorScore: EmotionMetricValue?,
    val impulseControlUnderAffectScore: EmotionMetricValue?,
    val emotionalGranularityScore: EmotionMetricValue?,
    val emotionThoughtSeparationScore: EmotionMetricValue?,
    val calibrationAccuracyScore: EmotionMetricValue?,
    val affectiveDecisionQualityScore: EmotionMetricValue?,
    val frustrationToleranceScore: EmotionMetricValue?,
    val pauseBeforeActionScore: EmotionMetricValue?,
    val safePersistenceScore: EmotionMetricValue?,
    // meta-fields
    val dataSufficiency: Double,
    val lastSessionQuality: Double?,
    val safetyLoad: Double,
    val practiceEffectRisk: EmotionMetricValue?,
    val transferConfidence: TransferConfidence,
    val tier: ProfileTier,
    val confidence: Double,
    val trend: ProfileTrend,
    val updatedAtMillis: Long,
)
