@file:Suppress("MagicNumber") // Trial counts are the fixtures under test.

package com.mindos.app.features.mindboost.emotions.domain.session

import com.mindos.app.features.mindboost.emotions.domain.catalog.EmotionExerciseCatalog
import com.mindos.app.features.mindboost.emotions.domain.metrics.DefaultEmotionMetricsEngine
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionCategory
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionResponseKey
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulus
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionStimulusPayload
import com.mindos.app.features.mindboost.emotions.domain.model.EmotionTrialOutcome
import com.mindos.app.features.mindboost.emotions.domain.model.RecordedEmotionTrial
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EmotionSessionAssemblerTest {

    private val assembler = EmotionSessionAssembler(DefaultEmotionMetricsEngine())

    private fun classif(truth: EmotionCategory, chosen: EmotionCategory?) = RecordedEmotionTrial(
        stimulus = EmotionStimulus(0, null, EmotionStimulusPayload.Classification("x", truth, false), 0, 1000),
        latencyMs = 600,
        responseKey = if (chosen == null) EmotionResponseKey.NONE else EmotionResponseKey.CATEGORY_A,
        chosenCategory = chosen,
    )

    @Test
    fun assembler_is_the_classification_authority() {
        val trials = listOf(
            classif(EmotionCategory.INTERNAL_STATE, EmotionCategory.INTERNAL_STATE), // CORRECT
            classif(EmotionCategory.THOUGHT, EmotionCategory.FACT), // INCORRECT (thought↔fact confusion)
            classif(EmotionCategory.FACT, null), // NO_RESPONSE
        )
        val result = assembler.assembleBlock(trials, EmotionExerciseCatalog.EM3_SEPARADOR)
        assertEquals(
            listOf(EmotionTrialOutcome.CORRECT, EmotionTrialOutcome.INCORRECT, EmotionTrialOutcome.NO_RESPONSE),
            result.outcomes,
        )
    }

    @Test
    fun confusion_matrix_counts_truth_to_chosen() {
        val trials = listOf(
            classif(EmotionCategory.THOUGHT, EmotionCategory.FACT),
            classif(EmotionCategory.THOUGHT, EmotionCategory.FACT),
            classif(EmotionCategory.THOUGHT, EmotionCategory.THOUGHT),
            classif(EmotionCategory.FACT, EmotionCategory.FACT),
        )
        val confusion = assertNotNull(assembler.assembleBlock(trials, EmotionExerciseCatalog.EM3_SEPARADOR).confusion)
        assertEquals(2, confusion.counts[EmotionCategory.THOUGHT]?.get(EmotionCategory.FACT))
        assertEquals(1, confusion.counts[EmotionCategory.THOUGHT]?.get(EmotionCategory.THOUGHT))
        assertEquals(2, confusion.correct()) // one THOUGHT→THOUGHT + one FACT→FACT
        assertEquals(4, confusion.total())
    }

    @Test
    fun confusion_is_null_for_non_classification_exercises() {
        val goNoGo = RecordedEmotionTrial(
            stimulus = EmotionStimulus(
                0,
                EmotionResponseKey.PRIMARY,
                EmotionStimulusPayload.AffectiveGoNoGo(frameOpen = true, affectiveWord = "espera", congruent = true),
                0,
                1000,
            ),
            latencyMs = 500,
            responseKey = EmotionResponseKey.PRIMARY,
        )
        assertNull(assembler.assembleBlock(listOf(goNoGo), EmotionExerciseCatalog.EM1_PAUSA).confusion)
    }
}
