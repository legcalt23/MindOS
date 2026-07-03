package com.mindos.app.features.mindboost.memory.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mindos.app.features.mindboost.memory.presentation.state.DeferredRecallUiState
import com.mindos.app.features.mindboost.memory.presentation.state.RecallOptionUi
import com.mindos.app.features.mindboost.memory.presentation.ui.DeferredRecallContent
import kotlin.test.Test
import kotlin.test.assertEquals

/** DR screen (P-UI) · the delayed-recall renderer shows the right surface per state and submits selections. */
@OptIn(ExperimentalTestApi::class)
class DeferredRecallScreenTest {

    @Test
    fun nothingDue_showsExplanationAndBack() = runComposeUiTest {
        setContent {
            DeferredRecallContent(DeferredRecallUiState.NothingDue, onSubmit = {}, onExit = {})
        }
        onNodeWithText("MindBoost · Repaso").assertIsDisplayed()
        onNodeWithText("Volver").assertIsDisplayed()
    }

    @Test
    fun recall_rendersCandidates_andSubmitsTheSelectedIds() = runComposeUiTest {
        var submitted: Set<String>? = null
        setContent {
            DeferredRecallContent(
                state = DeferredRecallUiState.Recall(
                    instruction = "Selecciona las que recuerdes",
                    candidates = listOf(RecallOptionUi("w1", "casa"), RecallOptionUi("w2", "perro")),
                ),
                onSubmit = { submitted = it },
                onExit = {},
            )
        }
        onNodeWithText("Selecciona las que recuerdes").assertIsDisplayed()
        onNodeWithText("casa").assertIsDisplayed()
        onNodeWithText("perro").assertIsDisplayed()

        onNodeWithText("casa").performClick() // select one candidate
        onNodeWithText("Enviar").performClick()
        assertEquals(setOf("w1"), submitted, "only the selected candidate id is submitted")
    }

    @Test
    fun finished_showsHonestRetentionSummary() = runComposeUiTest {
        setContent {
            DeferredRecallContent(
                state = DeferredRecallUiState.Finished(recalled = 3, studied = 5, honestNote = "Retención a 24h."),
                onSubmit = {},
                onExit = {},
            )
        }
        onNodeWithText("Recordados: 3/5").assertIsDisplayed()
        onNodeWithText("Retención a 24h.").assertIsDisplayed()
    }

    @Test
    fun recall_submitsEmpty_whenNothingSelected() = runComposeUiTest {
        var submitted: Set<String>? = null
        setContent {
            DeferredRecallContent(
                state = DeferredRecallUiState.Recall(
                    instruction = "i",
                    candidates = listOf(RecallOptionUi("w1", "casa")),
                ),
                onSubmit = { submitted = it },
                onExit = {},
            )
        }
        onNodeWithText("Enviar").performClick()
        assertEquals(emptySet(), submitted, "submitting with no selection yields an empty set, never null")
    }
}
