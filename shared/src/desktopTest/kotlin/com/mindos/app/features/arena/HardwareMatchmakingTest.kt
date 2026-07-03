package com.mindos.app.features.arena

import com.mindos.app.features.arena.config.TaskMotorType
import com.mindos.app.features.arena.hardware.IllegalMatchException
import com.mindos.app.features.arena.matchmaking.MatchmakingPoolState
import com.mindos.app.features.arena.matchmaking.SessionInit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Task 5: HardwareClass segmentation for Mental Speed (offline domain invariant, not live matchmaking). */
class HardwareMatchmakingTest {

    private fun session(
        id: String,
        task: TaskMotorType,
        hardware: com.mindos.app.features.arena.hardware.HardwareClass,
    ) = SessionInit(
        syntheticSessionId = id,
        syntheticPlayerId = "P-$id",
        taskType = task,
        hardwareClass = hardware,
    )

    @Test
    fun sessionInitRequiresAHardwareClassAndNonBlankIds() {
        // hardwareClass is a non-null constructor parameter — required by construction.
        val s = session("S1", TaskMotorType.MENTAL_SPEED_CHOICE_REACTION, ArenaTestFixtures.ANDROID_120)
        assertEquals(ArenaTestFixtures.ANDROID_120, s.hardwareClass)
        assertFailsWith<IllegalArgumentException> {
            SessionInit(" ", "P", TaskMotorType.FOCUS_INTERFERENCE_CONTROL, ArenaTestFixtures.ANDROID_120)
        }
    }

    @Test
    fun mentalSpeedMatchAcrossDifferingHardwareClassesIsRejected() {
        val pool = MatchmakingPoolState(TaskMotorType.MENTAL_SPEED_CHOICE_REACTION)
        val android = session("A", TaskMotorType.MENTAL_SPEED_CHOICE_REACTION, ArenaTestFixtures.ANDROID_120)
        val ios = session("B", TaskMotorType.MENTAL_SPEED_CHOICE_REACTION, ArenaTestFixtures.IOS_120)
        assertFalse(pool.canMatch(android, ios))
        assertFailsWith<IllegalMatchException> { pool.requireMatch(android, ios) }
    }

    @Test
    fun mentalSpeedMatchWithinSameHardwareClassIsAllowed() {
        val pool = MatchmakingPoolState(TaskMotorType.MENTAL_SPEED_CHOICE_REACTION)
        val a = session("A", TaskMotorType.MENTAL_SPEED_CHOICE_REACTION, ArenaTestFixtures.ANDROID_120)
        val b = session("B", TaskMotorType.MENTAL_SPEED_CHOICE_REACTION, ArenaTestFixtures.ANDROID_120)
        assertTrue(pool.canMatch(a, b))
        pool.requireMatch(a, b) // must not throw
    }

    @Test
    fun focusAndWorkingMemoryAllowCrossHardwareMatches() {
        val focusPool = MatchmakingPoolState(TaskMotorType.FOCUS_INTERFERENCE_CONTROL)
        val a = session("A", TaskMotorType.FOCUS_INTERFERENCE_CONTROL, ArenaTestFixtures.ANDROID_60)
        val b = session("B", TaskMotorType.FOCUS_INTERFERENCE_CONTROL, ArenaTestFixtures.IOS_120)
        assertTrue(focusPool.canMatch(a, b))
        focusPool.requireMatch(a, b) // must not throw — no Mental Speed restriction inherited
    }

    @Test
    fun poolRejectsSessionsOfTheWrongTask() {
        val pool = MatchmakingPoolState(TaskMotorType.MENTAL_SPEED_CHOICE_REACTION)
        val msSession = session("A", TaskMotorType.MENTAL_SPEED_CHOICE_REACTION, ArenaTestFixtures.ANDROID_120)
        val focusSession = session("B", TaskMotorType.FOCUS_INTERFERENCE_CONTROL, ArenaTestFixtures.ANDROID_120)
        assertFalse(pool.canMatch(msSession, focusSession))
    }
}
