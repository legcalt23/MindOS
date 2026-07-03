package com.mindos.app.features.mindboost.profile.domain.model

/**
 * MindBoostProfile — the global, functional cognitive "brain profile" (Fase 2A).
 *
 * It aggregates COARSE, non-PII signals that each MindBoost training module exposes one-way (mirroring
 * `EmotionProfileReadOnly`: a module exposes its contribution; the profile consumes it and never reaches
 * into module internals). The taxonomy below is the shared vocabulary every adapter maps into.
 *
 * Product safety: these are functional, descriptive dimensions — NEVER clinical/diagnostic labels.
 */

/** The functional cognitive dimensions MindBoost trains. Descriptive, not clinical. */
enum class CognitiveDimension {
    ATTENTION,
    PROCESSING_SPEED,
    WORKING_MEMORY,
    EPISODIC_MEMORY,
    REASONING,
    VERBAL,
    DECISION_MAKING,
    EMOTIONAL_REGULATION,
    CREATIVITY,
}

/**
 * A MindBoost training module that can contribute signal to the profile, paired with the dimension it
 * primarily informs. Keeping module and dimension distinct lets several modules inform one dimension later.
 *
 * MEMORIA and MEMORIA_EPISODICA are both fed by the Memoria FEATURE (one app module), but inform distinct
 * profile dimensions: complex-span (EX1) + associative (EX4) → WORKING_MEMORY; 24h delayed recall (EX3) →
 * EPISODIC_MEMORY. The split keeps the honest signal (episodic ≠ working memory) without overloading one id.
 */
enum class CognitiveModuleId(val primaryDimension: CognitiveDimension) {
    ENFOQUE(CognitiveDimension.ATTENTION),
    VELOCIDAD(CognitiveDimension.PROCESSING_SPEED),
    MEMORIA(CognitiveDimension.WORKING_MEMORY),
    MEMORIA_EPISODICA(CognitiveDimension.EPISODIC_MEMORY),
    LOGICA(CognitiveDimension.REASONING),
    LENGUAJE(CognitiveDimension.VERBAL),
    DECISIONES(CognitiveDimension.DECISION_MAKING),
    EMOCIONES(CognitiveDimension.EMOTIONAL_REGULATION),
    CREATIVIDAD(CognitiveDimension.CREATIVITY),
}

/** Direction of change of a signal over recent sessions. UNKNOWN until there is enough data. */
enum class ProfileTrend {
    IMPROVING,
    STABLE,
    DECLINING,
    UNKNOWN,
}
