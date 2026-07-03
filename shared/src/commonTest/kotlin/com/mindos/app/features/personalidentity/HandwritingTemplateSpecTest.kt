package com.mindos.app.features.personalidentity

import com.mindos.app.features.personalidentity.model.HandwritingGlyph
import com.mindos.app.features.personalidentity.model.HandwritingFontProfile
import com.mindos.app.features.personalidentity.model.HandwritingTemplateSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HandwritingTemplateSpecTest {
    @Test
    fun defaultTemplateIncludesSpanishCharacters() {
        val spec = HandwritingTemplateSpec()

        assertTrue('\u00D1' in spec.characters)
        assertTrue('\u00F1' in spec.characters)
        assertTrue('\u00BF' in spec.characters)
        assertTrue('\u00A1' in spec.characters)
    }

    @Test
    fun completionRatioUsesDistinctRequiredCharacters() {
        val spec = HandwritingTemplateSpec(rows = listOf("A A B"))
        val profile = HandwritingFontProfile(
            id = "test",
            name = "Test",
            templateSpec = spec,
            glyphs = mapOf('A' to HandwritingGlyph('A')),
        )

        assertEquals(0.5f, profile.completionRatio)
    }
}
