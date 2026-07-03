// Test method names use backticks with spaces; this repo's Detekt does not
// exempt them from FunctionNaming, so suppress at file scope.
@file:Suppress("FunctionNaming")

package com.mindos.app.design.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the pure floating-label / placeholder logic added when
 * `AppTextField` gained `prefix` + `placeholder` support. These cover the
 * decision functions only (no composition needed).
 */
class AppTextFieldLogicTest {

    @Test
    fun `label floats when focused even if empty`() {
        assertTrue(appTextFieldIsFloating(focused = true, value = "", hasPrefix = false))
    }

    @Test
    fun `label floats when value is not empty`() {
        assertTrue(appTextFieldIsFloating(focused = false, value = "hola", hasPrefix = false))
    }

    @Test
    fun `label floats when a prefix is present so the prefix is visible`() {
        assertTrue(appTextFieldIsFloating(focused = false, value = "", hasPrefix = true))
    }

    @Test
    fun `label rests inside when empty unfocused and no prefix`() {
        assertFalse(appTextFieldIsFloating(focused = false, value = "", hasPrefix = false))
    }

    @Test
    fun `placeholder shows when floating and empty`() {
        assertTrue(appTextFieldShowsPlaceholder(isFloating = true, value = "", placeholder = "https://mindos.app"))
    }

    @Test
    fun `placeholder hidden when the field has text`() {
        assertFalse(appTextFieldShowsPlaceholder(isFloating = true, value = "x", placeholder = "https://mindos.app"))
    }

    @Test
    fun `placeholder hidden while the label still rests inside`() {
        assertFalse(appTextFieldShowsPlaceholder(isFloating = false, value = "", placeholder = "https://mindos.app"))
    }

    @Test
    fun `no placeholder when none is provided`() {
        assertFalse(appTextFieldShowsPlaceholder(isFloating = true, value = "", placeholder = null))
    }
}
