package com.mindos.app.features.arena

import com.mindos.app.features.arena.config.TaskMotorType
import com.mindos.app.features.arena.evidence.motors.MotorEvidenceRules
import com.mindos.app.features.arena.hardware.IllegalMatchException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Phase 05 / Task 4: The three MVP motors and their evidence-acceptance + comparability rules. */
class MotorIntegrationTest {

    @Test
    fun focusEvidenceAccepted() {
        val evidence = ArenaTestFixtures.verifiedEvidence(
            motorType = TaskMotorType.FOCUS_INTERFERENCE_CONTROL,
            hardwareClass = null,
        )
        assertTrue(MotorEvidenceRules.acceptsEvidence(evidence))
    }

    @Test
    fun workingMemoryEvidenceAccepted() {
        val evidence = ArenaTestFixtures.verifiedEvidence(
            motorType = TaskMotorType.WORKING_MEMORY_SEQUENCE_UPDATING,
            hardwareClass = null,
        )
        assertTrue(MotorEvidenceRules.acceptsEvidence(evidence))
    }

    @Test
    fun mentalSpeedEvidenceAcceptedOnlyWithHardwareClass() {
        val withHardware = ArenaTestFixtures.verifiedEvidence(
            motorType = TaskMotorType.MENTAL_SPEED_CHOICE_REACTION,
            hardwareClass = ArenaTestFixtures.ANDROID_120,
        )
        assertTrue(MotorEvidenceRules.acceptsEvidence(withHardware))
        // Mental Speed evidence simply cannot be constructed without a hardware class.
        assertFailsWith<IllegalArgumentException> {
            ArenaTestFixtures.verifiedEvidence(
                motorType = TaskMotorType.MENTAL_SPEED_CHOICE_REACTION,
                hardwareClass = null,
            )
        }
    }

    @Test
    fun mentalSpeedComparisonAcrossIncompatibleHardwareClassesIsRejected() {
        val android = ArenaTestFixtures.verifiedEvidence(
            motorType = TaskMotorType.MENTAL_SPEED_CHOICE_REACTION,
            hardwareClass = ArenaTestFixtures.ANDROID_120,
        )
        val ios = ArenaTestFixtures.verifiedEvidence(
            motorType = TaskMotorType.MENTAL_SPEED_CHOICE_REACTION,
            hardwareClass = ArenaTestFixtures.IOS_120,
        )
        assertFalse(MotorEvidenceRules.areComparable(android, ios))
        assertFailsWith<IllegalMatchException> { MotorEvidenceRules.requireComparable(android, ios) }

        val sameClass = ArenaTestFixtures.verifiedEvidence(
            motorType = TaskMotorType.MENTAL_SPEED_CHOICE_REACTION,
            hardwareClass = ArenaTestFixtures.ANDROID_120,
        )
        assertTrue(MotorEvidenceRules.areComparable(android, sameClass))
    }

    @Test
    fun focusAndWorkingMemoryDoNotInheritMentalSpeedHardwareRestriction() {
        val a = ArenaTestFixtures.verifiedEvidence(
            motorType = TaskMotorType.FOCUS_INTERFERENCE_CONTROL,
            hardwareClass = ArenaTestFixtures.ANDROID_120,
        )
        val b = ArenaTestFixtures.verifiedEvidence(
            motorType = TaskMotorType.FOCUS_INTERFERENCE_CONTROL,
            hardwareClass = ArenaTestFixtures.IOS_120,
        )
        assertTrue(MotorEvidenceRules.areComparable(a, b))
    }

    @Test
    fun motorTypeCannotBeUnknownOrNull() {
        // TaskMotorType is a closed enum with exactly the three MVP motors; there is no UNKNOWN value.
        assertEquals(3, TaskMotorType.entries.size)
        assertEquals(
            setOf(
                TaskMotorType.FOCUS_INTERFERENCE_CONTROL,
                TaskMotorType.WORKING_MEMORY_SEQUENCE_UPDATING,
                TaskMotorType.MENTAL_SPEED_CHOICE_REACTION,
            ),
            TaskMotorType.entries.toSet(),
        )
        assertEquals("MOT_SPD_01", MotorEvidenceRules.motorId(TaskMotorType.MENTAL_SPEED_CHOICE_REACTION))
    }
}
