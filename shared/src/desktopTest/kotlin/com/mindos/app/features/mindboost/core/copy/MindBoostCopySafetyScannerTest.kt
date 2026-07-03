package com.mindos.app.features.mindboost.core.copy

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * INFRA-05 Phase 1 — unit tests for the pure copy-safety matcher + canonical lexicon.
 *
 * Real forbidden words are assembled from fragments (e.g. "intelig" + "encia") or taken from
 * [ForbiddenLexicon] so THIS source file never contains a contiguous forbidden literal — the same
 * hygiene the emotions compliance test uses, so a future broad scan can never (correctly) flag it.
 * Sentinels for the mechanics tests are made-up tokens, never real forbidden terms.
 */
class MindBoostCopySafetyScannerTest {

    @Test
    fun scanner_matches_whole_word_case_insensitive() {
        val sentinel = "zoster" + "x"
        assertTrue(MindBoostCopySafetyScanner.containsForbidden("a $sentinel b", listOf(sentinel)))
        assertTrue(MindBoostCopySafetyScanner.containsForbidden(sentinel.uppercase(), listOf(sentinel)))
    }

    @Test
    fun scanner_does_not_match_inner_substring() {
        val sentinel = "zoster" + "x"
        assertFalse(MindBoostCopySafetyScanner.containsForbidden("${sentinel}extra", listOf(sentinel)))
        assertFalse(MindBoostCopySafetyScanner.containsForbidden("pre$sentinel", listOf(sentinel)))
    }

    @Test
    fun scanner_folds_spanish_accents() {
        assertTrue(MindBoostCopySafetyScanner.containsForbidden("ZÓSTER", listOf("zoster")))
    }

    @Test
    fun scanner_crosses_hyphen_and_negation_boundary() {
        val root = "zoster" + "y"
        assertTrue(MindBoostCopySafetyScanner.containsForbidden("anti-$root", listOf(root)))
        assertTrue(MindBoostCopySafetyScanner.containsForbidden("no es un $root", listOf(root)))
    }

    @Test
    fun every_canonical_lexicon_term_self_detects() {
        for (term in ForbiddenLexicon.ALL) {
            val content = "alfa $term omega"
            assertTrue(
                MindBoostCopySafetyScanner.containsForbidden(content),
                "Canonical lexicon term not self-detected by scanAll: '$term'",
            )
        }
    }

    @Test
    fun negated_far_transfer_disclaimer_is_red() {
        // doc 12 §6 red example "Esto no es un test de inteligencia" — assembled, not a literal.
        val needle = "intelig" + "encia"
        assertTrue(needle in ForbiddenLexicon.FAR_TRANSFER, "Lexicon must include the far-transfer root")
        assertTrue(MindBoostCopySafetyScanner.containsForbidden("esto no es un test de $needle"))
    }

    @Test
    fun negated_clinical_disclaimer_is_red() {
        // Surfaced conflict: doc 12 §2 lists "proxy interno, no diagnostico" as permitted, but the
        // clinical lexeme is forbidden even when negated (invariant #c). The invariant wins.
        val needle = "diagnost" + "ico"
        assertTrue(needle in ForbiddenLexicon.CLINICAL, "Lexicon must include the clinical root")
        assertTrue(MindBoostCopySafetyScanner.containsForbidden("proxy interno, no $needle"))
    }

    @Test
    fun gambling_bankroll_is_red_but_semantic_balance_is_green() {
        val needle = "bank" + "roll"
        assertTrue(needle in ForbiddenLexicon.GAMBLING, "Lexicon must include the gambling root")
        assertTrue(MindBoostCopySafetyScanner.containsForbidden("calcula tu $needle"))
        // "balance" is semantic/permitted and deliberately NOT in the lexicon (FP regression).
        assertFalse(MindBoostCopySafetyScanner.containsForbidden("tu balance de aciertos hoy"))
    }

    @Test
    fun polysemic_permitted_phrases_are_green() {
        // These would false-positive if the lexicon used bare polysemic words instead of phrases.
        val green = listOf(
            "memoria de trabajo",
            "perfil cognitivo funcional",
            "disponibilidad cognitiva de hoy",
            "intervalo de confianza",
            "progreso contra tu linea base personal",
        )
        for (phrase in green) {
            assertTrue(
                MindBoostCopySafetyScanner.scanAll(phrase).isEmpty(),
                "Permitted polysemic phrase wrongly flagged: '$phrase' -> ${MindBoostCopySafetyScanner.scanAll(phrase).map { it.term }}",
            )
        }
    }

    @Test
    fun canonical_permitted_copy_is_green() {
        // doc 12 §2 permitted phrases that are genuinely clean (excludes "no diagnostico", see above).
        val permitted = listOf(
            "Senal de practica en esta tarea",
            "Estimacion preliminar",
            "Datos insuficientes para estimar",
            "Calibrando tu linea base",
            "No actualizado por calidad de medicion",
            "Medicion contaminada — no contabilizada",
            "Aun calibrando — vuelve tras mas sesiones",
            "Practica de atencion sostenida en esta tarea",
            "Tiempo de respuesta en esta tarea",
            "Aciertos frente a tu propia linea base",
            "Dificultad adaptativa",
            "Perfil cognitivo funcional",
            "Reconocimiento de fidelidad del resumen",
            "Aplicacion de los criterios dados en este ejercicio",
            "Inferencia soportada por la evidencia mostrada",
            "Disponibilidad cognitiva de hoy",
            "Esto es un chequeo, no una nota",
            "Puedes parar cuando quieras",
        )
        val hits = MindBoostCopySafetyScanner.scan(permitted.joinToString("\n"), ForbiddenLexicon.ALL)
        assertTrue(hits.isEmpty(), "Permitted copy wrongly flagged: ${hits.map { it.term }}")
    }
}
