package com.mindos.app.features.mindboost.speed.presentation.e8

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * SYSTEM-1 slice 6-R3 (R3-AUD-P2-02, subject/runId validation) · LEAD DECISION locked in by this scanner: the
 * per-subject scope handed to `SecureCognitiveSessionRunStore` / `SecureSessionRecordStore` (and therefore
 * embedded in every `SubjectRevocationAuthority` tombstone key) MUST be the pseudonymous, charset-safe Firebase
 * authenticated uid -- never a user-VISIBLE identity field (email/displayName/username).
 * `SecureCognitiveSessionRunStore`'s own `validateSubject` only rejects blank and the ':' delimiter; it cannot
 * itself verify WHICH identity string was handed to it. This scanner is the cheap, build-time proof that the
 * real production wiring chain (HomeScreen -> MindBoostHubScreen -> SpeedRootScreen) feeds the right one, by
 * tracing the actual source text of each hop -- so a future edit that swapped in `displayName`/an email field
 * would fail this test even though it would still compile.
 *
 * VERIFIED (manual trace, this checkpoint): `HomeScreen.kt` derives `ownerId` from the authenticated
 * `user.uid` (Firebase's opaque pseudonymous id) and passes it to `MindBoostHubScreen` as `userId`;
 * `MindBoostHubScreen` keeps `userId`/`displayName` as two DISTINCT parameters and forwards `userId` verbatim to
 * `SpeedRootScreen`; `SpeedRootScreen` scopes both stores with `subject = userId`. No visible username/email is
 * used as a subject anywhere in this chain.
 */
class GoNoGoSubjectProvenanceTest {

    @Test
    fun homeScreen_derivesTheHubsUserId_fromTheAuthenticatedFirebaseUid_notADisplayField() {
        val text = sourceOf("features/home/presentation/HomeScreen.kt")
        assertTrue(text.contains("UserId(user.uid)"), "ownerId must be derived from the authenticated user.uid")
        assertTrue(text.contains("userId = ownerId.value"), "MindBoostHubScreen must receive the uid-derived id")
        assertNoVisibleIdentityLeaksIntoSubject(text, "HomeScreen.kt")
    }

    @Test
    fun mindBoostHubScreen_keepsUserIdDistinctFromDisplayName_andForwardsItVerbatimToSpeedRootScreen() {
        val text = sourceOf("features/mindboost/presentation/ui/MindBoostHubScreen.kt")
        assertTrue(
            Regex("""fun MindBoostHubScreen\(\s*userId:\s*String,\s*displayName:\s*String,""").containsMatchIn(text),
            "userId and displayName must stay two distinct parameters -- never one derived from the other",
        )
        assertTrue(
            Regex("""SpeedRootScreen\(\s*userId\s*=\s*userId,""").containsMatchIn(text),
            "SpeedRootScreen must receive the hub's userId verbatim, never displayName",
        )
    }

    @Test
    fun speedRootScreen_scopesBothStoresToTheReceivedUserId_neverToDisplayNameOrEmail() {
        val text = sourceOf("features/mindboost/speed/presentation/ui/SpeedRootScreen.kt")
        assertTrue(text.contains("fun SpeedRootScreen(") && text.contains("userId: String,"))
        assertTrue(
            text.contains("SecureSessionRecordStore(sessionsStore, revocationStore = revocationStore, subject = userId)"),
            "SecureSessionRecordStore must be scoped by the received userId",
        )
        assertTrue(
            text.contains("SecureCognitiveSessionRunStore(runsStore, revocationStore = revocationStore, subject = userId)"),
            "SecureCognitiveSessionRunStore must be scoped by the received userId",
        )
        assertNoVisibleIdentityLeaksIntoSubject(text, "SpeedRootScreen.kt")
    }

    private fun assertNoVisibleIdentityLeaksIntoSubject(text: String, fileName: String) {
        val forbidden = listOf(
            "subject = email", "subject = displayName", "subject = username",
            "userId = displayName", "userId = email",
        )
        val offenders = forbidden.filter { text.contains(it) }
        assertTrue(offenders.isEmpty(), "$fileName must never scope identity by a visible field; found: $offenders")
    }

    private fun sourceOf(relativeFromAppPackage: String): String {
        val relative = "src/commonMain/kotlin/com/mindos/app/$relativeFromAppPackage"
        val file = locateFile(relative) ?: error("expected to locate $relative from ${File(".").absolutePath}")
        return file.readText()
    }

    private fun locateFile(relative: String): File? {
        var base: File? = File(".").absoluteFile
        repeat(MAX_DEPTH) {
            val current = base ?: return null
            File(current, relative).takeIf { it.isFile }?.let { return it }
            File(current, "shared/$relative").takeIf { it.isFile }?.let { return it }
            base = current.parentFile
        }
        return null
    }

    private companion object {
        const val MAX_DEPTH = 8
    }
}
