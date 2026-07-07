package com.mindos.app.features.mindboost.speed.presentation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MC9-A-R §2 — user-visible Velocidad Mental copy is honest: no internal milestone/build labels ("MVP-0",
 * "Hito", "MC9-A"), the preliminary framing stays visible, and no official/rank/normative claim appears while
 * the physical-validation gate §15 is open.
 */
class SpeedStringsHonestyTest {

    private val accents = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'ñ' to 'n')
    private fun fold(s: String) = buildString { for (c in s.lowercase()) append(accents[c] ?: c) }

    @Test
    fun visible_copy_exposes_no_internal_milestone_or_build_labels() {
        val internal = listOf("mvp", "mc9", "mc8", "hito", "milestone", "sprint", "build interno")
        SpeedStrings.VISIBLE_COPY.forEach { s ->
            val f = fold(s)
            internal.forEach { assertFalse(f.contains(it), "internal label '$it' leaked into visible copy: '$s'") }
        }
    }

    @Test
    fun preliminary_framing_remains_visible() {
        val all = fold(SpeedStrings.VISIBLE_COPY.joinToString(" "))
        assertTrue(all.contains("preliminar") || all.contains("orientativ"), "preliminary framing must stay visible")
    }

    @Test
    fun no_official_rank_or_normative_claim_while_the_gate_is_open() {
        val claims = listOf(
            "ranking", "percentil", "puntuacion oficial", "rango oficial",
            "por encima del promedio", "clasificacion mundial", "nota oficial",
        )
        SpeedStrings.VISIBLE_COPY.forEach { s ->
            val f = fold(s)
            claims.forEach { assertFalse(f.contains(it), "official/normative claim '$it' in visible copy: '$s'") }
        }
    }
}
