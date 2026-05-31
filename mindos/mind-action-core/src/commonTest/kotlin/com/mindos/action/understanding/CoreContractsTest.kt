package com.mindos.action.understanding

import com.mindos.action.entity.DeterministicConfidence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CoreContractsTest {

    @Test
    fun mindIntentIdRejectsBlank() {
        assertFailsWith<IllegalArgumentException> { MindIntentId(" ") }
    }

    @Test
    fun slotNameRejectsBlank() {
        assertFailsWith<IllegalArgumentException> { SlotName("") }
    }

    @Test
    fun understoodCarriesIntentAndEntities() {
        val result = ActionUnderstandingResult.Understood(
            intent = MindIntent(MindIntentId("open_settings")),
            entities = emptyMap(),
            confidence = DeterministicConfidence.EXACT,
        )
        assertEquals("open_settings", result.intent.id.value)
        assertNull(result.trace) // explain off by default
    }

    @Test
    fun rejectionReasonsAreTyped() {
        val r: RejectionReason = RejectionReason.InvalidEntity(SlotName("target"), "username", "bad")
        assertEquals("target", (r as RejectionReason.InvalidEntity).slot.value)
    }

    @Test
    fun defaultContextIsLocaleAgnosticNoExplain() {
        val ctx = UnderstandingContext()
        assertEquals("und", ctx.localeTag)
        assertEquals(false, ctx.explain)
    }
}
