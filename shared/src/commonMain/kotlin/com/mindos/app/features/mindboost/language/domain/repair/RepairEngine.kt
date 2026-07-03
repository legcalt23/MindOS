package com.mindos.app.features.mindboost.language.domain.repair

import com.mindos.app.features.mindboost.language.domain.model.ErrorType

/**
 * Repair protocol engine (v6 §6.5). Detects a recurring error pattern, prescribes the
 * standard repair plan for it, and resolves the next action from the retry outcome.
 */
interface RepairEngine {
    /**
     * Detects the dominant reparable pattern from a sequence of error observations, or `null`
     * if no error repeats often enough. Frequency is computed over the RAW list (no dedup).
     */
    fun detectPattern(errors: List<ErrorType>): ErrorPattern?

    /** The standard repair plan for a detected [pattern]. Defined for every [ErrorType]. */
    fun planFor(pattern: ErrorPattern): RepairPlan

    /** The repair plan for a single error type (used when a pattern is already known). */
    fun planForError(errorType: ErrorType): RepairPlan

    /** Resolves the post-retry branch: improve → [RepairPlan.nextIfImprove], else nextIfFail. */
    fun resolveOutcome(plan: RepairPlan, improved: Boolean): RepairOutcome
}
