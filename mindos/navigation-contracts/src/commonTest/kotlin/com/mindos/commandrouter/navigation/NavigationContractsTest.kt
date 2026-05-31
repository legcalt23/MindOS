package com.mindos.commandrouter.navigation

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NavigationContractsTest {

    private val json = Json { encodeDefaults = true }

    @Test
    fun navigationTarget_screen_roundTrip() {
        val target: NavigationTarget = NavigationTarget.Screen("settings", mapOf("tab" to "privacy"))
        val decoded = json.decodeFromString<NavigationTarget>(json.encodeToString(target))
        assertEquals(target, decoded)
    }

    @Test
    fun navigationTarget_back_roundTrip() {
        val target: NavigationTarget = NavigationTarget.Back
        val decoded = json.decodeFromString<NavigationTarget>(json.encodeToString(target))
        assertEquals(target, decoded)
    }

    @Test
    fun navigationTarget_external_roundTrip() {
        val target: NavigationTarget = NavigationTarget.External("https://example.com")
        val decoded = json.decodeFromString<NavigationTarget>(json.encodeToString(target))
        assertEquals(target, decoded)
    }

    @Test
    fun navigationResult_success_roundTrip() {
        val result: NavigationResult = NavigationResult.Success(NavigationTarget.Screen("home"))
        val decoded = json.decodeFromString<NavigationResult>(json.encodeToString(result))
        assertEquals(result, decoded)
    }

    @Test
    fun navigationResult_failure_targetNotFound_roundTrip() {
        val result: NavigationResult = NavigationResult.Failure.TargetNotFound("unknown_screen")
        val decoded = json.decodeFromString<NavigationResult>(json.encodeToString(result))
        assertEquals(result, decoded)
    }

    @Test
    fun routeParameters_get_returnsNullWhenMissing() {
        val params = RouteParameters(mapOf("a" to "1"))
        assertNull(params.get("missing"))
        assertEquals("1", params.get("a"))
    }

    @Test
    fun routeParameters_empty_isReusableSingleton() {
        assertEquals(RouteParameters.Empty, RouteParameters())
    }

    @Test
    fun navigationCommand_equality() {
        val cmd1 = NavigationCommand(NavigationTarget.Screen("settings"), commandId = "c1")
        val cmd2 = NavigationCommand(NavigationTarget.Screen("settings"), commandId = "c1")
        assertEquals(cmd1, cmd2)
    }
}
