package com.mindos.app.features.mindboost.focus2.presentation.a11y

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mindos.app.features.mindboost.focus2.domain.runtime.AccessibilityContext

/**
 * Focus V5 · S3 · tarea (6) — accessibility, read from the platform rather than assumed.
 *
 * Two things live here, and both exist because the plan forbids a specific shortcut.
 *
 * **Minimum target sizes are NATIVE values, not one aspirational number.** Android's guidance is 48dp and
 * Apple's HIG is 44pt. They are different numbers from different vendors, and picking the smaller one "because
 * it's the stricter-looking constant" ships an Android build that fails its own accessibility scanner, while
 * picking a single 44 "universal" number is the exact anti-pattern P1-16 names. So the table below records both
 * and the platform picks its own.
 *
 * **Accessibility state is read, never assumed.** [rememberFocusAccessibility] asks the platform; a platform
 * that cannot answer returns [AccessibilityContext.NONE], which is the conservative reading — it claims no
 * assistive technology is active, so nothing is excluded from evidence on a guess.
 */
object A11yTargets {

    /** Android: Material accessibility guidance, enforced by the Accessibility Scanner. */
    val ANDROID_MINIMUM: Dp = 48.dp

    /** iOS: Human Interface Guidelines. A point and a dp are both 1/160 inch at baseline density. */
    val IOS_MINIMUM: Dp = 44.dp

    /** Desktop has no touch guidance of its own; it follows the larger of the two, which is never wrong. */
    val DESKTOP_MINIMUM: Dp = ANDROID_MINIMUM
}

/** This platform's minimum interactive target. Declared per platform; never a shared compromise value. */
expect val platformMinimumTarget: Dp

/**
 * Reads the assistive technologies currently active.
 *
 * Called ONCE at session start and sealed into the descriptor. Re-reading mid-session would let a change part
 * way through rewrite what the earlier trials meant.
 */
@Composable
expect fun rememberFocusAccessibility(): AccessibilityContext

/**
 * The switch-access input model (REDTEAM #34), as a pure state machine.
 *
 * A person using one or two switches cannot point at a target; they move a focus ring and activate whatever it
 * lands on. The requirement is not "make the buttons reachable" — it is that the exercise be COMPLETABLE this
 * way, which means the interaction has to be modelled, not just styled. Modelling it here rather than inside a
 * composable is what makes "E07 can be finished with two switches" a testable claim instead of a design note.
 *
 * Note it is a cursor over an arbitrary option list, not a left/right pair: an exercise that hard-codes two
 * positions cannot be reused by the vigilance tracer in S4, which has one.
 */
data class SequentialFocusCursor<T>(val options: List<T>, val focusedIndex: Int = 0) {

    init {
        require(options.isNotEmpty()) { "a cursor over no options can never be activated" }
        require(focusedIndex in options.indices) { "focusedIndex $focusedIndex is outside ${options.indices}" }
    }

    val focused: T get() = options[focusedIndex]

    /** Moves focus to the next option, wrapping. Wrapping matters: with one switch there is no "back". */
    fun advance(): SequentialFocusCursor<T> = copy(focusedIndex = (focusedIndex + 1) % options.size)

    /** The single activation. Returns what was focused; the caller decides what that means. */
    fun activate(): T = focused
}
