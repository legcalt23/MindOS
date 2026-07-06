package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.features.mindboost.emotions.domain.catalog.TransferLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** MC8q: the result screen must render a human-readable transfer label, never the raw enum name. */
class EmotionStringsTest {

    @Test
    fun transferLabel_does_not_render_raw_enum() {
        TransferLabel.entries.forEach { label ->
            val text = EmotionStrings.transferLabel(label)
            assertFalse(text.contains(label.name), "raw enum '$label' leaked to user: '$text'")
            assertTrue(text.isNotBlank(), "each label maps to non-blank copy")
        }
        val texts = TransferLabel.entries.map { EmotionStrings.transferLabel(it) }
        assertEquals(TransferLabel.entries.size, texts.toSet().size, "each transfer label maps to a distinct string")
    }

    @Test
    fun transferLabel_placeboRisk_is_user_safe() {
        // Never the alarming developer token "PLACEBO_RISK" — an honest, non-clinical, non-jargon label.
        val text = EmotionStrings.transferLabel(TransferLabel.PLACEBO_RISK)
        assertEquals("Sin evidencia de transferencia", text)
        assertFalse(text.contains("PLACEBO"), "the raw enum token must never reach the user")
    }
}
