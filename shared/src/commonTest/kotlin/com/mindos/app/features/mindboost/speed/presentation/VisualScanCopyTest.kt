package com.mindos.app.features.mindboost.speed.presentation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MC9-C · E5 — the Búsqueda Visual feedback copy is serious and on-topic. Forbidden-lexicon and normative
 * claims are enforced elsewhere (the build `mindboostCopySafetyScanner` over the speed feature tree, and
 * [SpeedStringsHonestyTest] over `VISIBLE_COPY`, which now includes the visual-search feedback lines), so this
 * test does NOT restate those forbidden lexemes as literals — it checks the visual-search vocabulary is present
 * and no arcade/humiliating tone leaks.
 */
class VisualScanCopyTest {

    private val vs = listOf(
        SpeedStrings.VS_FEEDBACK_CALIBRATING,
        SpeedStrings.VS_FEEDBACK_GOOD_VISUAL_CONTROL,
        SpeedStrings.VS_FEEDBACK_FAST_LOW_PRECISION,
        SpeedStrings.VS_FEEDBACK_HIGH_VISUAL_LOAD_COST,
        SpeedStrings.VS_FEEDBACK_UNSTABLE_SEARCH,
        SpeedStrings.VS_FEEDBACK_BALANCED,
    )

    private val accents = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'ñ' to 'n')
    private fun fold(s: String) = buildString { for (c in s.lowercase()) append(accents[c] ?: c) }

    @Test
    fun feedback_references_visual_search_and_precision_or_stability() {
        val all = fold(vs.joinToString(" "))
        assertTrue(all.contains("visual"), "E5 feedback must reference visual search")
        assertTrue(
            all.contains("busqueda") || all.contains("deteccion") || all.contains("explor") || all.contains("objetivo"),
            "E5 feedback must reference search/detection",
        )
        assertTrue(
            all.contains("precisi") || all.contains("estabilid") || all.contains("control"),
            "E5 feedback must reference precision/stability/control",
        )
    }

    @Test
    fun feedback_has_no_arcade_infantile_or_humiliating_tone() {
        val banned = listOf(
            "turbo", "gamer", "sobrehumano", "sobrehumana", "reflejos superiores",
            "eres lento", "nivel gamer", "vista sobrehumana", "superpoder",
        )
        vs.forEach { s ->
            val f = fold(s)
            banned.forEach { assertFalse(f.contains(it), "arcade/humiliating copy '$it' leaked into: '$s'") }
        }
    }

    @Test
    fun feedback_makes_no_ranking_or_normative_claim() {
        val banned = listOf("ranking", "percentil", "por encima del promedio", "mejor que")
        vs.forEach { s ->
            val f = fold(s)
            banned.forEach { assertFalse(f.contains(it), "normative claim '$it' in: '$s'") }
        }
    }
}
