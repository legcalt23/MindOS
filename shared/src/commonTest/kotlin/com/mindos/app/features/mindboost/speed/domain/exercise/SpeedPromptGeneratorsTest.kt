package com.mindos.app.features.mindboost.speed.domain.exercise

import com.mindos.app.features.mindboost.speed.domain.SpeedChoice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpeedPromptGeneratorsTest {

    @Test
    fun rng_isDeterministicForSameSeed() {
        val a = XorShiftSpeedRng(123L)
        val b = XorShiftSpeedRng(123L)
        repeat(50) {
            assertEquals(a.nextInt(7), b.nextInt(7))
            assertEquals(a.nextBoolean(), b.nextBoolean())
        }
    }

    @Test
    fun rng_nextInt_isWithinBounds() {
        val rng = XorShiftSpeedRng(9L)
        repeat(500) { assertTrue(rng.nextInt(4) in 0..3) }
    }

    @Test
    fun opposite_flipsSide() {
        assertEquals(SpeedChoice.RIGHT, SpeedChoice.LEFT.opposite())
        assertEquals(SpeedChoice.LEFT, SpeedChoice.RIGHT.opposite())
    }

    @Test
    fun multivia_correctChoiceMatchesArrowCue() {
        val gen = MultiviaGenerator()
        val rng = XorShiftSpeedRng(1L)
        repeat(200) {
            val p = gen.next(rng)
            val expected = if (p.cue == "◀") SpeedChoice.LEFT else SpeedChoice.RIGHT
            assertEquals(expected, p.correctChoice)
        }
    }

    @Test
    fun cifrado_matchEntailsCorrectKeyValue_andLeftMeansCoincide() {
        val gen = CifradoGenerator()
        val rng = XorShiftSpeedRng(5L)
        var sawLeft = false
        var sawRight = false
        repeat(300) {
            val p = gen.next(rng)
            val symbol = p.cue.substringBefore(" → ")
            val candidate = p.cue.substringAfter(" → ")
            val trueValue = gen.key.getValue(symbol)
            if (p.correctChoice == SpeedChoice.LEFT) {
                sawLeft = true
                assertEquals(trueValue, candidate, "LEFT=coincide ⇒ candidato == clave")
            } else {
                sawRight = true
                assertTrue(candidate != trueValue, "RIGHT=no coincide ⇒ candidato != clave")
            }
        }
        assertTrue(sawLeft && sawRight, "deben aparecer ambos resultados")
    }

    @Test
    fun compuerta_appliesGateRule() {
        val gen = CompuertaGenerator()
        val rng = XorShiftSpeedRng(7L)
        var sawOpen = false
        var sawClosed = false
        repeat(300) {
            val p = gen.next(rng)
            val arrow = if (p.cue.startsWith("◀")) SpeedChoice.LEFT else SpeedChoice.RIGHT
            val gateOpen = p.cue.contains("abierta")
            val expected = if (gateOpen) arrow else arrow.opposite()
            assertEquals(expected, p.correctChoice, "regla: abierta⇒flecha, cerrada⇒opuesto")
            if (gateOpen) sawOpen = true else sawClosed = true
        }
        assertTrue(sawOpen && sawClosed, "deben aparecer puerta abierta y cerrada")
    }

    @Test
    fun cifrado_requiresAtLeastTwoKeyEntries() {
        val tooSmall = mapOf("▲" to "1")
        kotlin.test.assertFailsWith<IllegalArgumentException> { CifradoGenerator(tooSmall) }
    }

    @Test
    fun generators_areDeterministicEndToEnd() {
        fun seq(): List<SpeedChoice> {
            val gen = CompuertaGenerator()
            val rng = XorShiftSpeedRng(42L)
            return (0 until 30).map { gen.next(rng).correctChoice }
        }
        assertEquals(seq(), seq())
    }
}
