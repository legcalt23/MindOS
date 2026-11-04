package com.mindos.app.features.mindboost.focus2

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * REDTEAM #31 (S5 · `T-S5`) — **Focus v2 introduces NO new platform permission.** Encrypted storage
 * is Keystore/Keychain (no runtime permission), and the engine has no notifications, no sensors and
 * no network.
 *
 * A declaration is not a control, so this pins the app manifest's permission set. The three below
 * predate focus2 and belong to unrelated features (camera/mic for media capture, notifications for
 * the app shell). A permission appearing in a diff that also touches focus2 now turns this red and
 * has to be justified in review — which is exactly what row #31 asks for, since a permission gained
 * silently is one nobody ever decided to ask the user for.
 */
class Focus2PlatformPermissionTest {

    @Test
    fun theAppManifestDeclaresOnlyTheKnownPreFocus2Permissions() {
        val manifest = locate("androidApp/src/main/AndroidManifest.xml")
        val declared = PERMISSION_PATTERN.findAll(manifest.readText()).map { it.groupValues[1] }.toSet()

        assertEquals(PRE_FOCUS2_PERMISSIONS, declared, "the app's permission set changed — justify it in review")
    }

    @Test
    fun theFocus2SourcesRequestNoPermissionAndOpenNoSocket() {
        val focus2 = locate("shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus2")
        val timing = locate("shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/timing")
        val offenders = (focus2.walkTopDown() + timing.walkTopDown())
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                val text = file.readText()
                FORBIDDEN_APIS.any { text.contains(it) }
            }
            .map { it.name }
            .toList()

        assertTrue(offenders.isEmpty(), "focus2 / core.timing must request nothing from the platform: $offenders")
    }

    private fun locate(relative: String): File {
        var base: File? = File(".").absoluteFile
        repeat(MAX_LOOKUP_DEPTH) {
            val current = base ?: error("no parent")
            File(current, relative).takeIf { it.exists() }?.let { return it }
            base = current.parentFile
        }
        error("expected to locate $relative from ${File(".").absolutePath}")
    }

    private companion object {
        val PERMISSION_PATTERN = Regex("""uses-permission android:name="([^"]+)"""")

        val PRE_FOCUS2_PERMISSIONS = setOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.POST_NOTIFICATIONS",
        )

        /** Anything that would imply a permission, a sensor or a network egress the engine must not have. */
        val FORBIDDEN_APIS = listOf(
            "requestPermission",
            "android.permission",
            "SensorManager",
            "LocationManager",
            "java.net.",
            "HttpURLConnection",
        )

        const val MAX_LOOKUP_DEPTH = 8
    }
}
