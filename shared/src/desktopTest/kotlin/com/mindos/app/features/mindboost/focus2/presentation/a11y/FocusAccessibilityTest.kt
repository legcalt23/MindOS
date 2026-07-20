package com.mindos.app.features.mindboost.focus2.presentation.a11y

import androidx.compose.ui.unit.dp
import com.mindos.app.features.mindboost.focus2.domain.scoring.Response
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Focus V5 · S3 · tarea (6) — the accessibility rules that can be checked without a device.
 *
 * What a JVM test can prove here is narrow and worth being explicit about: it pins the target-size TABLE and
 * the switch-access INTERACTION MODEL. It does not prove that the rendered buttons are actually 48dp on a
 * phone, which needs the Accessibility Scanner on-device and belongs to the S5 verification.
 */
class FocusAccessibilityTest {

    /**
     * The anti-pattern this guards is named in the plan (P1-16): a single "44pt universal aspiracional" value.
     * Asserting the two minimums are DIFFERENT is the assertion that catches it — asserting each one alone
     * would still pass if someone set both to 44.
     */
    @Test
    fun the_two_platform_minimums_are_the_vendors_own_numbers_and_are_not_the_same_number() {
        assertEquals(48.dp, A11yTargets.ANDROID_MINIMUM, "Material accessibility guidance")
        assertEquals(44.dp, A11yTargets.IOS_MINIMUM, "Apple Human Interface Guidelines")
        assertNotEquals(
            A11yTargets.ANDROID_MINIMUM,
            A11yTargets.IOS_MINIMUM,
            "collapsing these into one value ships a build that fails its own platform's guidance",
        )
    }

    @Test
    fun desktop_follows_the_larger_minimum_so_it_is_never_below_either_platform() {
        assertEquals(A11yTargets.ANDROID_MINIMUM, A11yTargets.DESKTOP_MINIMUM)
        assertEquals(A11yTargets.DESKTOP_MINIMUM, platformMinimumTarget, "this test runs on the desktop actual")
        assertTrue(A11yTargets.DESKTOP_MINIMUM >= A11yTargets.IOS_MINIMUM)
    }

    // --- REDTEAM #34: the exercise must be completable with one or two switches ----------------------------

    @Test
    fun both_options_are_reachable_with_a_single_switch_because_the_focus_wraps() {
        val cursor = SequentialFocusCursor(listOf(Response.LEFT, Response.RIGHT))
        assertEquals(Response.LEFT, cursor.focused)
        assertEquals(Response.RIGHT, cursor.advance().focused)
        assertEquals(Response.LEFT, cursor.advance().advance().focused, "one switch has no 'back'; it must wrap")
    }

    @Test
    fun every_option_can_actually_be_selected_and_not_merely_focused() {
        val options = listOf(Response.LEFT, Response.RIGHT)
        // For each option, count the advances needed and assert the activation yields exactly that option.
        options.forEachIndexed { target, expected ->
            var cursor = SequentialFocusCursor(options)
            repeat(target) { cursor = cursor.advance() }
            assertEquals(expected, cursor.activate(), "option #$target must be selectable, not just reachable")
        }
    }

    @Test
    fun a_single_option_mechanic_works_too_so_the_model_is_not_hard_coded_to_a_left_right_pair() {
        // S4's vigilance tracer has one response, not two. A cursor that assumed a pair would break there.
        val cursor = SequentialFocusCursor(listOf(Response.TAP))
        assertEquals(Response.TAP, cursor.activate())
        assertEquals(Response.TAP, cursor.advance().activate())
    }

    @Test
    fun a_cursor_over_nothing_is_refused_at_construction() {
        assertTrue(runCatching { SequentialFocusCursor(emptyList<Response>()) }.isFailure)
        assertTrue(runCatching { SequentialFocusCursor(listOf(Response.LEFT), focusedIndex = 4) }.isFailure)
    }
}
