package com.mindos.action.execution

import com.mindos.action.matching.DefaultActionMatcher
import com.mindos.action.matching.RegistryCompiler
import com.mindos.action.navigation.MindNavigationCommand
import com.mindos.action.pattern.registrySource
import com.mindos.action.understanding.ActionUnderstandingResult
import com.mindos.action.validation.DefaultActionValidator
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The complete MindOS flow, end to end:
 * text → [ZIP02] understanding → IntentToActionMapper → [ZIP03] validate → resolve → coordinator.
 *
 * Confirms every invariant in one place: typed throughout, deterministic, no false positives,
 * and the coordinator (not the matcher) owns navigation.
 */
class FullPipelineE2ETest {

    private val source = registrySource {
        exact("p_settings", "open settings", intent = "open_settings")
        parametric("p_profile", "open {target:username}", intent = "open_profile")
    }
    private val matcher = DefaultActionMatcher(RegistryCompiler.compile(source).getOrThrow())
    private val mapper = IntentToActionMapper(counterRequestIds())

    private fun pipeline(coordinator: FakeCoordinator) =
        DefaultActionExecutor(DefaultActionValidator(), sampleResolvers(), coordinator)

    private suspend fun run(text: String, coordinator: FakeCoordinator): ExecutionResult? {
        val understood = matcher.understand(text) as? ActionUnderstandingResult.Understood ?: return null
        val action = mapper.map(understood.intent, understood.entities) ?: return null
        return pipeline(coordinator).execute(action)
    }

    @Test
    fun openProfileTextResolvesAndNavigatesToUserId() = runTest {
        val coordinator = FakeCoordinator()
        val result = run("open ana", coordinator)
        assertEquals(MindNavigationCommand.OpenProfile("u_ana"), (result as ExecutionResult.Executed).command)
        assertEquals(MindNavigationCommand.OpenProfile("u_ana"), coordinator.lastCommand)
    }

    @Test
    fun openSettingsTextNavigates() = runTest {
        val coordinator = FakeCoordinator()
        val result = run("open settings", coordinator)
        assertEquals(MindNavigationCommand.OpenSettings, (result as ExecutionResult.Executed).command)
    }

    @Test
    fun unknownTextNeverReachesExecutor() = runTest {
        val coordinator = FakeCoordinator()
        assertEquals(null, run("florble", coordinator))
        assertEquals(0, coordinator.calls)
    }

    @Test
    fun unresolvableUsernameFailsWithoutNavigating() = runTest {
        val coordinator = FakeCoordinator()
        val result = run("open ghost", coordinator)
        assertTrue((result as ExecutionResult.Failed).failure is ExecutionFailure.ResolutionFailed)
        assertEquals(0, coordinator.calls)
    }
}
