package com.mindos.app.features.profile.domain

import com.mindos.app.features.profile.domain.model.LinkValidationResult
import com.mindos.app.features.profile.domain.model.ProfileLinkValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ProfileLinkValidatorTest {

    @Test
    fun acceptsHttpsUrl() {
        val result = ProfileLinkValidator.validate("https://mindos.app/perfil")
        assertIs<LinkValidationResult.Valid>(result)
        assertEquals("https://mindos.app/perfil", result.normalizedUrl)
    }

    @Test
    fun normalizesBareDomainToHttps() {
        val result = ProfileLinkValidator.validate("mindos.app")
        assertIs<LinkValidationResult.Valid>(result)
        assertEquals("https://mindos.app", result.normalizedUrl)
    }

    @Test
    fun rejectsEmpty() {
        assertIs<LinkValidationResult.Invalid>(ProfileLinkValidator.validate("   "))
    }

    @Test
    fun rejectsNonHttpScheme() {
        assertIs<LinkValidationResult.Invalid>(ProfileLinkValidator.validate("ftp://mindos.app"))
    }

    @Test
    fun rejectsHostWithoutDot() {
        assertIs<LinkValidationResult.Invalid>(ProfileLinkValidator.validate("localhost"))
    }

    @Test
    fun rejectsWhitespace() {
        assertIs<LinkValidationResult.Invalid>(ProfileLinkValidator.validate("mind os.app"))
    }
}
