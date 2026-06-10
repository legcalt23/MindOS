package com.mindos.app.core.notifications

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationPermissionGateTest {

    @Test
    fun preAndroid13_isAlwaysGrantedAndNoPromptNeeded() {
        val state = NotificationPermissionState(
            platformApiLevel = 32, // Android 12L
            isGranted = true,
            isPermanentlyDenied = false,
            dismissedInThisSession = false,
        )
        assertFalse(state.requiresRuntimeRequest)
        assertFalse(state.shouldShowPrompt)
        assertFalse(state.shouldDirectToSettings)
    }

    @Test
    fun preAndroid13_evenIfFlaggedDenied_stillNoRuntimeRequest() {
        // Defensive: caller wouldn't set isGranted=false pre-13 in practice,
        // but the gate must still ignore it.
        val state = NotificationPermissionState(
            platformApiLevel = 30, // Android 11
            isGranted = false,
            isPermanentlyDenied = true,
            dismissedInThisSession = false,
        )
        assertFalse(state.requiresRuntimeRequest)
        assertFalse(state.shouldShowPrompt)
    }

    @Test
    fun android13_grantedAlready_noPrompt() {
        val state = NotificationPermissionState(
            platformApiLevel = 33, // Android 13
            isGranted = true,
            isPermanentlyDenied = false,
            dismissedInThisSession = false,
        )
        assertFalse(state.requiresRuntimeRequest)
        assertFalse(state.shouldShowPrompt)
    }

    @Test
    fun android13_notGrantedAndFreshContext_promptsUser() {
        val state = NotificationPermissionState(
            platformApiLevel = 33,
            isGranted = false,
            isPermanentlyDenied = false,
            dismissedInThisSession = false,
        )
        assertTrue(state.requiresRuntimeRequest)
        assertTrue(state.shouldShowPrompt)
        assertFalse(state.shouldDirectToSettings)
    }

    @Test
    fun android13_permanentlyDenied_redirectsToSettings_notPrompt() {
        val state = NotificationPermissionState(
            platformApiLevel = 34, // Android 14
            isGranted = false,
            isPermanentlyDenied = true,
            dismissedInThisSession = false,
        )
        assertTrue(state.requiresRuntimeRequest)
        assertFalse(state.shouldShowPrompt)
        assertTrue(state.shouldDirectToSettings)
    }

    @Test
    fun android13_dismissedThisSession_doesNotReprompt() {
        val state = NotificationPermissionState(
            platformApiLevel = 33,
            isGranted = false,
            isPermanentlyDenied = false,
            dismissedInThisSession = true,
        )
        assertTrue(state.requiresRuntimeRequest)
        assertFalse(state.shouldShowPrompt, "Must not re-prompt within same session")
    }

    @Test
    fun android14_treatedSameAs13() {
        val freshDenied = NotificationPermissionState(
            platformApiLevel = 34,
            isGranted = false,
            isPermanentlyDenied = false,
            dismissedInThisSession = false,
        )
        assertTrue(freshDenied.shouldShowPrompt)
    }

    @Test
    fun grantedBeatsAllOtherFlags() {
        val state = NotificationPermissionState(
            platformApiLevel = 34,
            isGranted = true,
            // These flags would normally suppress prompts, but isGranted is the final word.
            isPermanentlyDenied = true,
            dismissedInThisSession = true,
        )
        assertFalse(state.requiresRuntimeRequest)
        assertFalse(state.shouldShowPrompt)
        assertFalse(state.shouldDirectToSettings)
    }

    @Test
    fun android13Constant_matchesRealApiLevel() {
        // Pin the threshold so future refactors can't accidentally bump it.
        assertTrue(NotificationPermissionState.Android13ApiLevel == 33)
    }
}
