package com.mindos.app.features.mindboost.presentation

import com.mindos.app.features.mindboost.core.copy.ForbiddenLexicon
import com.mindos.app.features.mindboost.creativity.presentation.CreativityStrings
import com.mindos.app.features.mindboost.speed.presentation.SpeedStrings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * B5 copy-honesty regression for the hub track subtitles of Creatividad and Velocidad. Creatividad's hub
 * used to promise "sin puntuación ni evaluación" while a visible rubric scorer runs and reports process
 * feedback; Velocidad used a generic BETA. Both subtitles are now centralized constants and must stay
 * honest: no forbidden lexeme (whole-word, mirroring the build scanner, so a benign word like "Beta"
 * is not a false positive), no "sin evaluación" promise, and nothing implying an official grade/rank or a
 * guaranteed gain. Pure product-copy invariant — it does NOT touch scoring, profile, or training logic.
 */
class HubTrackCopyHonestyTest {

    private val subtitles = mapOf(
        "creatividad" to CreativityStrings.HUB_SUBTITLE,
        "velocidad" to SpeedStrings.HUB_SUBTITLE,
    )

    /** Whole-word, case-insensitive — same semantics as the build scanner (avoids substring false positives). */
    private fun forbiddenHit(text: String): String? =
        ForbiddenLexicon.ALL.firstOrNull { term ->
            Regex("\\b" + Regex.escape(term) + "\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
        }

    @Test
    fun hubSubtitles_containNoForbiddenLexeme() {
        subtitles.forEach { (name, copy) ->
            assertNull(forbiddenHit(copy), "$name hub subtitle must not contain a forbidden lexeme: \"$copy\"")
        }
    }

    @Test
    fun hubSubtitles_doNotPromiseNoEvaluation() {
        subtitles.forEach { (name, copy) ->
            val lower = copy.lowercase()
            assertFalse(
                lower.contains("sin puntuación ni evaluación"),
                "$name must not promise 'sin puntuación ni evaluación'",
            )
            assertFalse(lower.contains("sin evaluación"), "$name must not promise 'sin evaluación'")
        }
    }

    @Test
    fun hubSubtitles_doNotSuggestOfficialRankOrGuarantee() {
        subtitles.forEach { (name, copy) ->
            val lower = copy.lowercase()
            assertFalse(lower.contains("oficial"), "$name must not suggest an official grade/rank")
            assertFalse(lower.contains("garantiz"), "$name must not suggest a guaranteed gain")
        }
    }

    // Positive guard (efficacy-guardian follow-up): a re-wording must not drop the honesty clause and
    // become "honest by omission". Each subtitle keeps the exact disclaimer that makes its claim true.
    @Test
    fun hubSubtitles_keepTheirHonestyDisclaimer() {
        assertTrue(
            CreativityStrings.HUB_SUBTITLE.lowercase().contains("sin nota ni rango"),
            "Creatividad subtitle must keep its no-grade/no-rank disclaimer",
        )
        assertTrue(
            SpeedStrings.HUB_SUBTITLE.lowercase().contains("sin puntuación validada"),
            "Velocidad subtitle must keep its no-validated-score disclaimer",
        )
    }
}
