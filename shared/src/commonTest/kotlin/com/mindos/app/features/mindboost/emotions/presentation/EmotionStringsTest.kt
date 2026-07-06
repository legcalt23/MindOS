package com.mindos.app.features.mindboost.emotions.presentation

import com.mindos.app.features.mindboost.emotions.domain.catalog.TransferLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** MC8q: the result screen must render a human-readable transfer label, never the raw enum name. */
class EmotionStringsTest {

    @Test
    fun transfer_label_never_surfaces_the_raw_enum_name() {
        TransferLabel.entries.forEach { label ->
            val text = EmotionStrings.transferLabel(label)
            assertFalse(text.contains(label.name), "raw enum '$label' leaked to user: '$text'")
            assertTrue(text.isNotBlank(), "each label maps to non-blank copy")
        }
    }

    @Test
    fun transfer_label_maps_every_value_to_a_distinct_string() {
        val texts = TransferLabel.entries.map { EmotionStrings.transferLabel(it) }
        assertEquals(TransferLabel.entries.size, texts.toSet().size, "each transfer label maps to a distinct string")
    }

    @Test
    fun transfer_label_placebo_risk_is_honest_not_alarming_jargon() {
        assertEquals("Sin evidencia de transferencia", EmotionStrings.transferLabel(TransferLabel.PLACEBO_RISK))
    }
}
