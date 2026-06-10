package com.mindos.app.features.mindboost.memory.domain.personalization

/**
 * The 11 cognitive profiles of the memory system (doc §12). The system personalizes
 * by the user's real pattern of weaknesses, not by preference. Most are inferred
 * from metrics ([com.mindos.app.features.mindboost.memory.domain.personalization.ArchetypeInference]);
 * [BEGINNER] and [DEMANDING_STUDENT] are user-declared (a goal/onboarding choice),
 * never inferred from performance.
 */
enum class MemoryArchetype {
    /** a) First-timer: gentle onboarding, low difficulty. */
    BEGINNER,

    /** b) Low working-memory span (≤ p25 complex span). */
    LOW_WORKING_MEMORY,

    /** c) Forgets fast (delayed/immediate ratio < 0.50). */
    FAST_FORGETTER,

    /** d) Confuses similar items (low d-prime, high lure intrusions). */
    CONFUSES_SIMILAR,

    /** e) Underconfident (confidence bias < -0.20). */
    UNDERCONFIDENT,

    /** f) Overconfident (confidence bias > +0.20). */
    OVERCONFIDENT,

    /** g) Slow but accurate. */
    SLOW_BUT_ACCURATE,

    /** h) Fast but inaccurate. */
    FAST_BUT_INACCURATE,

    /** i) Demanding student (opositor/high-performer) — user-declared. */
    DEMANDING_STUDENT,

    /** j) Advanced. */
    ADVANCED,

    /** k) Elite. */
    ELITE,
}
