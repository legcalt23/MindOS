package com.mindos.app.features.mindboost.focus.domain.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SessionBlueprintTest {

    @Test
    fun everyBlueprint_sumsExactlyToTemplateLength() {
        SessionBlueprints.all.forEach { blueprint ->
            assertEquals(
                blueprint.template.totalMinutes * 60_000L,
                blueprint.totalMs,
                "Blueprint for ${blueprint.template.name} must sum to its template length",
            )
        }
    }

    @Test
    fun forTemplate_returnsMatchingBlueprint() {
        SessionTemplate.entries.forEach { template ->
            assertEquals(template, SessionBlueprints.forTemplate(template).template)
        }
    }

    @Test
    fun longerTemplates_addPeakAndTransferPhases() {
        // The 5-minute maintenance session has no transfer phase; the 20-minute one does.
        assertEquals(0L, SessionBlueprints.MAINTENANCE_5.durationOf(SessionPhase.TRANSFER))
        assertTrue(SessionBlueprints.DEVELOPMENT_20.durationOf(SessionPhase.TRANSFER) > 0L)
        assertTrue(SessionBlueprints.STANDARD_10.durationOf(SessionPhase.PEAK) > 0L)
    }

    @Test
    fun blueprint_rejectsPhasesThatDoNotSumToTemplate() {
        assertFailsWith<IllegalArgumentException> {
            SessionBlueprint(
                SessionTemplate.MAINTENANCE_5,
                listOf(PhaseSlot(SessionPhase.CORE, 10_000)), // far short of 5 minutes
            )
        }
    }
}
