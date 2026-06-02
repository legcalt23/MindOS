package com.mindos.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CommandInputSecurityGateTest {

    private val gate = DefaultCommandInputSecurityGate()

    @Test
    fun textWarningsDoNotRejectNaturalCommandsBeforeEntityPolicy() {
        val decision = gate.inspectText(
            "open m\u0430rta",
            SecurityContext(source = CommandInputSource.TEXT),
        )

        assertEquals(SecurityDecision.Allowed, decision)
    }

    @Test
    fun deepLinkRejectsCyrillicHomoglyphSpoof() {
        val decision = gate.inspectDeepLink(
            "mindos://profile/m\u0430rta",
            SecurityContext(source = CommandInputSource.DEEP_LINK),
        )

        val rejected = assertIs<SecurityDecision.Rejected>(decision)
        assertEquals(true, "HOMOGLYPH_CONFUSABLE" in rejected.codes)
    }

    @Test
    fun deepLinkRejectsDecodedZeroWidthCharacter() {
        val decision = gate.inspectDeepLink(
            "mindos://profile/ana%E2%80%8B",
            SecurityContext(source = CommandInputSource.DEEP_LINK),
        )

        val rejected = assertIs<SecurityDecision.Rejected>(decision)
        assertEquals(true, "INVALID_CONTROL_CHARACTER" in rejected.codes)
    }

    @Test
    fun resolvedDtoFieldsRejectHomoglyphRefs() {
        val decision = gate.inspectResolvedFields(
            mapOf("username" to "m\u0430rta"),
            SecurityContext(source = CommandInputSource.DTO),
        )

        assertIs<SecurityDecision.Rejected>(decision)
    }
}
