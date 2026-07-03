package com.mindos.app.features.mindboost.memory.presentation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.mindos.app.features.mindboost.memory.domain.model.MemoryExerciseId
import com.mindos.app.features.mindboost.memory.presentation.state.MemoryExerciseCardUi
import com.mindos.app.features.mindboost.memory.presentation.state.MemoryHomeUiState
import com.mindos.app.features.mindboost.memory.presentation.ui.MemoryHomeScreen
import kotlin.test.Test
import kotlin.test.assertTrue

/** P-UI · the Home surfaces a deferred-review entry ONLY when reviews are due, and it navigates on tap. */
@OptIn(ExperimentalTestApi::class)
class MemoryHomeReviewEntryTest {

    private fun ready(dueDeferredCount: Int) = MemoryHomeUiState.Ready(
        displayName = "Curro",
        archetypeName = "EXPLORER",
        cycleWeek = 1,
        dueDeferredCount = dueDeferredCount,
        exercises = listOf(
            MemoryExerciseCardUi(MemoryExerciseId.EX3_LISTA_ESTRATEGIA, "Lista con Estrategia", "Recuerda una lista."),
        ),
    )

    @Test
    fun reviewEntry_shows_andNavigates_whenReviewsAreDue() = runComposeUiTest {
        var reviewed = false
        setContent {
            MemoryHomeScreen(
                state = ready(dueDeferredCount = 2),
                onStart = {},
                onReview = { reviewed = true },
                onProgress = {},
                onBack = {},
            )
        }
        onNodeWithText("Repaso pendiente · hazlo ahora").assertIsDisplayed()
        onNodeWithText("Repaso pendiente · hazlo ahora").performClick()
        assertTrue(reviewed, "tapping the review entry navigates to the deferred-recall review")
    }

    @Test
    fun reviewEntry_isAbsent_whenNothingIsDue() = runComposeUiTest {
        setContent {
            MemoryHomeScreen(
                state = ready(dueDeferredCount = 0),
                onStart = {},
                onReview = {},
                onProgress = {},
                onBack = {},
            )
        }
        onNodeWithText("Lista con Estrategia").assertIsDisplayed() // home rendered…
        onNodeWithText("Repaso pendiente · hazlo ahora").assertDoesNotExist() // …but no review entry
    }

    @Test
    fun progressEntry_navigates_onTap() = runComposeUiTest {
        var progressed = false
        setContent {
            MemoryHomeScreen(
                state = ready(dueDeferredCount = 0),
                onStart = {},
                onReview = {},
                onProgress = { progressed = true },
                onBack = {},
            )
        }
        onNodeWithText("Ver progreso").performClick()
        assertTrue(progressed, "tapping 'Ver progreso' opens the progress screen")
    }
}
