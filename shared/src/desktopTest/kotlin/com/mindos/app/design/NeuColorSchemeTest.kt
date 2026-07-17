package com.mindos.app.design

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Locks the two invariants that make the theme-aware layer safe to add:
 *  1. The dark scheme reuses [AppColors] verbatim → ZERO visual regression for
 *     the existing (dark) app.
 *  2. The light scheme is genuinely a distinct, light variant.
 */
class NeuColorSchemeTest {

    @Test
    fun dark_scheme_mirrors_AppColors_for_zero_regression() {
        val dark = neuDarkColorScheme()
        assertFalse(dark.isLight)
        assertEquals(AppColors.Background, dark.background)
        assertEquals(AppColors.Surface, dark.surface)
        assertEquals(AppColors.OnBackground, dark.onBackground)
        assertEquals(AppColors.NeuGreen, dark.primary)
        assertEquals(AppColors.NeuViolet, dark.secondary)
        assertEquals(AppColors.Error, dark.error)
    }

    @Test
    fun light_scheme_is_light_and_distinct_from_dark() {
        val light = neuLightColorScheme()
        val dark = neuDarkColorScheme()
        assertTrue(light.isLight)
        assertEquals(AppColors.LightBackground, light.background)
        assertNotEquals(dark.background, light.background)
        assertNotEquals(dark.onBackground, light.onBackground)
        assertNotEquals(dark.primary, light.primary)
    }
}
