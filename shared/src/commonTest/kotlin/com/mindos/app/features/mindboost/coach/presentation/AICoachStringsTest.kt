@file:Suppress("MaxLineLength")

package com.mindos.app.features.mindboost.coach.presentation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** MC8u-F — coach copy is not clinical and not manipulative (no cure/IQ-guarantee/therapy/fear-to-sell); clinical nouns only appear as negation disclaimers. */
class AICoachStringsTest {

    private val accents = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'ñ' to 'n')
    private fun fold(s: String): String = buildString { for (c in s.lowercase()) append(accents[c] ?: c) }

    @Test
    fun promotional_copy_has_no_clinical_or_manipulative_language() {
        val forbidden = listOf(
            "cura", "garantiz", "iq", "coeficiente", "terapia", "terapeut", "diagnostic", "medicacion", "trastorno", "ansiedad", "depresion",
            "ultima oportunidad", "solo hoy", "no te quedes atras", "te lo pierdes", "date prisa",
        )
        AICoachStrings.PROMOTIONAL.forEach { s ->
            val folded = fold(s)
            forbidden.forEach { bad -> assertFalse(folded.contains(bad), "'$bad' must not appear in promotional copy: '$s'") }
        }
    }

    @Test
    fun disclaimers_are_negations_not_claims() {
        AICoachStrings.DISCLAIMERS.forEach { assertTrue(fold(it).contains("no "), "a disclaimer must be a negation: '$it'") }
    }

    @Test
    fun the_mc8u_g_key_texts_are_present() {
        assertTrue(AICoachStrings.ANALYZES.contains("MindBoost"))
        assertTrue(AICoachStrings.CAN_DELETE_MEMORY.contains("memoria IA"))
        assertTrue(AICoachStrings.STRUCTURED_SUMMARIES.contains("datos crudos"))
        assertTrue(AICoachStrings.TRAINING_AVAILABLE_AT_LIMIT.contains("límite diario"))
        assertTrue(AICoachStrings.DISCLAIMERS.any { fold(it).contains("garantiz") }, "the no-guarantee disclaimer must be present as a negation")
    }
}
