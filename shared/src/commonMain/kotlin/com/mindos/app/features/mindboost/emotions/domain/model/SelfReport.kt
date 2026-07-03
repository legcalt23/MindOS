package com.mindos.app.features.mindboost.emotions.domain.model

/**
 * Closed, functional (non-clinical) emotion labels. Display strings live in EmotionStrings (i18n-ready).
 * Deliberately functional labels (e.g. "preocupación", "tensión"), never clinical category names.
 */
enum class EmotionLabel {
    CALMA,
    INTERES,
    TENSION,
    FRUSTRACION,
    IRRITACION,
    IMPACIENCIA,
    DECEPCION,
    PREOCUPACION,
    INSEGURIDAD,
    BLOQUEO,
    CANSANCIO,
    CONFIANZA,
    SATISFACCION,
    NEUTRALIDAD,
}

/**
 * Brief self-report taken BEFORE/AFTER a session only (never intra-trial; an exception is the single
 * proxy-triggered mid-session check-in, which reuses only [intensity]). Scales are 1..5. The optional
 * [label] keeps introspection optional. Self-report is stored SEPARATELY from behavioral proxies and
 * is never fused with them.
 */
data class SelfReport(
    val valence: Int,
    val arousal: Int,
    val intensity: Int,
    val label: EmotionLabel? = null,
) {
    init {
        require(valence in SCALE && arousal in SCALE && intensity in SCALE) {
            "self-report scales must be in $SCALE"
        }
    }

    companion object {
        val SCALE = 1..5
    }
}
