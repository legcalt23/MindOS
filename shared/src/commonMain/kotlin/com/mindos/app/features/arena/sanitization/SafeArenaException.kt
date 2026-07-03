package com.mindos.app.features.arena.sanitization

/**
 * Phase 07.5 / Task 8-9: Safe error categories. Carry enough type information to debug, but no
 * identifiers, rating values, or free-text payload.
 */
enum class ArenaErrorCategory {
    RATING_MATH_ERROR,
    EVIDENCE_VALIDATION_ERROR,
    MATCH_INVARIANT_ERROR,
    LEDGER_ERROR,
    SIMULATION_ERROR,
    SANITIZATION_ERROR,
    UNKNOWN,
}

/**
 * Phase 07.5: Exception that is safe to cross the Arena boundary.
 *
 * Its [message] is derived solely from the [category] (no original message, no UID, no rating), and
 * its [cause] is always stripped to null. A fresh stack trace is captured at the [from] call site so
 * debugging context (where it was wrapped) survives without leaking the original payload.
 * This realizes 16_telemetry_redaction_plan.md: "overrides getMessage() to strip payload details".
 */
class SafeArenaException private constructor(
    val category: ArenaErrorCategory,
) : RuntimeException() {

    override val message: String = "ArenaError:${category.name}"

    override val cause: Throwable? get() = null

    companion object {
        fun of(category: ArenaErrorCategory): SafeArenaException = SafeArenaException(category)

        /**
         * Wraps any throwable as a [SafeArenaException], mapping its type to a safe [ArenaErrorCategory]
         * and discarding its message and cause entirely.
         */
        fun from(throwable: Throwable): SafeArenaException = when (throwable) {
            is SafeArenaException -> throwable
            is IllegalArgumentException -> of(ArenaErrorCategory.EVIDENCE_VALIDATION_ERROR)
            is IllegalStateException -> of(ArenaErrorCategory.MATCH_INVARIANT_ERROR)
            is ArithmeticException -> of(ArenaErrorCategory.RATING_MATH_ERROR)
            else -> of(ArenaErrorCategory.UNKNOWN)
        }
    }
}
