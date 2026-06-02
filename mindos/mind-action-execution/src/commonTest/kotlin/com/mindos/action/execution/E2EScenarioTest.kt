package com.mindos.action.execution

import com.mindos.action.navigation.MindNavigationCommand
import com.mindos.action.understanding.ActionUnderstandingResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * E2E scenarios with documented expected outputs (ZIP04 E2E_SCENARIOS).
 *
 * | input                | understanding   | execution result                         |
 * |----------------------|-----------------|------------------------------------------|
 * | "open settings"      | open_settings   | Executed(OpenSettings)                   |
 * | "open ana"           | open_profile    | Executed(OpenProfile("u_ana"))           |
 * | "open story abc123"  | open_story      | Executed(OpenStory("st_abc"))            |
 * | "abre la historia de ana" | open_story_of_user | Executed(OpenStory("st_ana_current")) |
 * | "inbox" (alias)      | open_messages   | Executed(OpenMessages)  [notifications]  |
 * | "open ghost"         | open_profile    | Failed(ResolutionFailed) — not navigated |
 * | "@@@ %%%" (malformed)| NoMatch         | not executed — coordinator untouched     |
 */
class E2EScenarioTest {

    private fun fresh() = FakeCoordinator()

    private suspend fun run(text: String, coordinator: FakeCoordinator): ExecutionResult? {
        val mapper = IntentToActionMapper(counterRequestIds())
        val u = ExecReliability.matcher.understand(text) as? ActionUnderstandingResult.Understood ?: return null
        val action = mapper.map(u.intent, u.entities) ?: return null
        return ExecReliability.executor(coordinator).execute(action)
    }

    @Test
    fun openSettingsScenario() = runTest {
        val c = fresh()
        assertEquals(MindNavigationCommand.OpenSettings, (run("open settings", c) as ExecutionResult.Executed).command)
    }

    @Test
    fun openProfileScenario() = runTest {
        val c = fresh()
        assertEquals(MindNavigationCommand.OpenProfile("u_ana"), (run("open ana", c) as ExecutionResult.Executed).command)
    }

    @Test
    fun openStoryScenario() = runTest {
        val c = fresh()
        assertEquals(MindNavigationCommand.OpenStory("st_abc"), (run("open story abc123", c) as ExecutionResult.Executed).command)
    }

    @Test
    fun openStoryOfUserScenario() = runTest {
        val c = fresh()
        val result = run("abre la historia de ana", c) as ExecutionResult.Executed
        assertEquals(MindNavigationCommand.OpenStory("st_ana_current"), result.command)
    }

    @Test
    fun notificationsViaInboxAliasScenario() = runTest {
        val c = fresh()
        assertEquals(MindNavigationCommand.OpenMessages, (run("inbox", c) as ExecutionResult.Executed).command)
    }

    @Test
    fun unresolvableProfileScenario() = runTest {
        val c = fresh()
        val result = run("open ghost", c)
        assertTrue((result as ExecutionResult.Failed).failure is ExecutionFailure.ResolutionFailed)
        assertEquals(0, c.calls)
    }

    @Test
    fun malformedCommandScenario() = runTest {
        val c = fresh()
        assertEquals(null, run("@@@ %%%", c))
        assertEquals(0, c.calls)
    }
}
