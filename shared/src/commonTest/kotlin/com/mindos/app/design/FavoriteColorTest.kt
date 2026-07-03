package com.mindos.app.design

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

/** Unit tests for the centralized favorite-color mapper. */
class FavoriteColorTest {

    @Test
    fun null_accent_falls_back_to_loader_blue() {
        assertEquals(FavoriteColor.LoaderBlue, favoriteAccentColor(null))
    }

    @Test
    fun null_accent_uses_provided_fallback() {
        val custom = Color(0xFF13ECA4)
        assertEquals(custom, favoriteAccentColor(null, fallback = custom))
    }

    @Test
    fun stored_argb_maps_to_that_color() {
        assertEquals(Color(0xFFE53935), favoriteAccentColor(0xFFE53935L))
    }

    @Test
    fun stored_argb_wins_over_fallback() {
        val purple = 0xFF8F5CFFL
        assertEquals(Color(purple), favoriteAccentColor(purple, fallback = Color.Red))
    }
}
