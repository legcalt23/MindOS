package com.mindos.action.matching

/**
 * Fully auditable, deterministic ranking score. Every dimension is explicit — there is no
 * hidden heuristic and, critically, **no lexicographic PatternId tie-break**.
 *
 * Ordering (best = greatest), mirroring the documented policy:
 *  1. [priority]            — explicit author priority (higher wins)
 *  2. [exactness]           — 3 exact > 2 alias > 1 parametric
 *  3. [staticTokenMatches]  — more fixed tokens = more specific
 *  4. [typedSlotMatches]    — more typed slots = more specific
 *  5. [freeTextPenalty]     — fewer FreeText slots wins (lower better)
 *  6. [localeMatch]         — exact-locale alias beats locale-agnostic
 *  7. [declarationOrder]    — stable, earlier declaration wins (lower better)
 */
data class MatchScore(
    val priority: Int,
    val exactness: Int,
    val staticTokenMatches: Int,
    val typedSlotMatches: Int,
    val freeTextPenalty: Int,
    val localeMatch: Int,
    val declarationOrder: Int,
) : Comparable<MatchScore> {

    fun summary(): String =
        "prio=$priority exact=$exactness static=$staticTokenMatches typed=$typedSlotMatches " +
            "ftPenalty=$freeTextPenalty locale=$localeMatch order=$declarationOrder"

    /** True when two scores tie on every *semantic* dimension (ignores [declarationOrder]). */
    fun semanticTie(other: MatchScore): Boolean =
        priority == other.priority &&
            exactness == other.exactness &&
            staticTokenMatches == other.staticTokenMatches &&
            typedSlotMatches == other.typedSlotMatches &&
            freeTextPenalty == other.freeTextPenalty &&
            localeMatch == other.localeMatch

    override fun compareTo(other: MatchScore): Int {
        var c = priority.compareTo(other.priority); if (c != 0) return c
        c = exactness.compareTo(other.exactness); if (c != 0) return c
        c = staticTokenMatches.compareTo(other.staticTokenMatches); if (c != 0) return c
        c = typedSlotMatches.compareTo(other.typedSlotMatches); if (c != 0) return c
        c = other.freeTextPenalty.compareTo(freeTextPenalty); if (c != 0) return c // lower better
        c = localeMatch.compareTo(other.localeMatch); if (c != 0) return c
        return other.declarationOrder.compareTo(declarationOrder) // lower better
    }

    companion object {
        const val EXACTNESS_EXACT = 3
        const val EXACTNESS_ALIAS = 2
        const val EXACTNESS_PARAMETRIC = 1
    }
}
