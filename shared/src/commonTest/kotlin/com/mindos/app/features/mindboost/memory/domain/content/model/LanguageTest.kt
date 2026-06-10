package com.mindos.app.features.mindboost.memory.domain.content.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LanguageTest {

    @Test
    fun spanish_isoCodeIsEs() {
        assertEquals("es", Language.SPANISH_ES.isoCode())
    }

    @Test
    fun spanish_displayNameNotBlank() {
        assertTrue(Language.SPANISH_ES.displayName().isNotBlank())
    }
}
