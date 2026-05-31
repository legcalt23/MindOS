package com.mindos.action.normalization

import kotlinx.serialization.Serializable

/**
 * A single token with three aligned forms and its span in the case-preserved text.
 *
 * - [raw]: original case, NFC-normalized, control-stripped — used for **case-sensitive**
 *   entities (ids) and FreeText (preserves case + accents).
 * - [display]: [raw] case-folded (locale-invariant lowercase), **accents preserved** —
 *   used for usernames/mentions/hashtags.
 * - [normalized]: [display] with diacritics optionally folded (per config) — used for
 *   static-token matching and the exact/alias index keys.
 *
 * Keeping all three avoids two precision bugs at once:
 * lowercasing case-sensitive ids, and folding "café" (free text) into "cafe".
 */
@Serializable
data class Token(
    val raw: String,
    val display: String,
    val normalized: String,
    val start: Int,
    val end: Int,
)
