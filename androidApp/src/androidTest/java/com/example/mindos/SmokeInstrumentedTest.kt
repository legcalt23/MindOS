package com.example.mindos

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mindos.core.security.SafeInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Minimal on-device smoke test that proves the instrumented-test pipeline
 * (AndroidJUnitRunner + Gradle Managed Devices) is wired and that the shared
 * KMP module is linked into the instrumented APK.
 *
 * Deliberately free of Compose-UI-test and Firebase dependencies so it is robust
 * in CI. Critical-flow instrumented tests (login / DM / wallet / account deletion)
 * layer on top of this once the Firebase emulator suite (T5.1) is wired into the
 * device test environment — see docs/architecture/PHASE5_NOTES.md.
 */
@RunWith(AndroidJUnit4::class)
class SmokeInstrumentedTest {

    @Test
    fun appContext_hasExpectedPackage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.mindos.app.debug", context.packageName)
    }

    @Test
    fun sharedModule_isLinkedOnDevice() {
        // Exercises commonMain code on the real device runtime, confirming the
        // shared module is correctly bundled into the instrumented APK.
        assertTrue(SafeInput.isSafeUsername("valid_user"))
        assertFalse(SafeInput.isSafeUsername("ab")) // below the 3-char minimum
    }
}
