package com.mindos.action.entity

/**
 * Precompiled shape validators. Each is a single bounded character class — strictly
 * linear, no nested quantifiers or alternation, so there is **no catastrophic
 * backtracking** risk even on adversarial input (PR3). Compiled once as object fields,
 * never per request.
 */
internal object EntityShapes {
    /** Username body (after optional '@'): 2–30 of [a-z0-9_.], edge-dot checked in code. */
    val USERNAME = Regex("^[a-z0-9_.]{2,30}$")

    /** Hashtag body (after '#'): 1–100 of [a-z0-9_]. */
    val HASHTAG_BODY = Regex("^[a-z0-9_]{1,100}$")

    /** Opaque, case-sensitive id: 1–128 of [A-Za-z0-9_-]. Length floor enforced per extractor. */
    val OPAQUE_ID = Regex("^[A-Za-z0-9_-]{1,128}$")

    /** Signed integer, up to 19 digits (fits Long). */
    val NUMERIC = Regex("^-?[0-9]{1,19}$")
}
