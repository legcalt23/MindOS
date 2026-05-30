package com.example.mindos.features.element_selection.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.example.mindos.features.element_selection.presentation.ElementSelectionStateHolder
import kotlin.test.Test

/**
 * Desktop smoke test: ElementSelectionScreen builds its composable graph without
 * crashing when supplied with a default state holder.
 *
 * The screen is heavy on custom Canvas drawing and gesture detectors, so the
 * primary risk is initialisation-time exceptions (NPEs from null state, OOB
 * indices in animation math, etc.). This test guards against those without
 * locking in implementation details.
 *
 * It does NOT assert specific labels because element names are decorative and
 * may change with content updates; the value here is "does it crash?".
 */
@OptIn(ExperimentalTestApi::class)
class ElementSelectionScreenSmokeTest {

    @Test
    fun elementSelectionScreen_composes_without_crashing() = runComposeUiTest {
        val stateHolder = ElementSelectionStateHolder()

        setContent {
            ElementSelectionScreen(
                onElementSelected = { /* no-op for smoke test */ },
                stateHolder = stateHolder,
            )
        }

        // If composition reached this point without throwing, the screen is healthy.
        // We assert one frame settles by waiting for idle; this also exercises the
        // first layout pass.
        waitForIdle()
    }
}
