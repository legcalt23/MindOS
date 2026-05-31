package com.mindos.action.matching

import com.mindos.action.entity.EntityExtractorRegistry
import com.mindos.action.pattern.registrySource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegistryCompilerTest {

    private fun errorCodes(result: RegistryCompilationResult): List<IssueCode> =
        (result as RegistryCompilationResult.Failure).errors.map { it.code }

    @Test
    fun compilesValidRegistry() {
        val source = registrySource {
            exact("p_settings", "open settings", intent = "open_settings")
            alias("ajustes", locale = "es", intent = "open_settings")
            parametric("p_profile", "open {target:username}", intent = "open_profile")
        }
        val result = RegistryCompiler.compile(source)
        assertTrue(result is RegistryCompilationResult.Success)
        val reg = (result as RegistryCompilationResult.Success).registry
        assertEquals(1, reg.exactCount)
        assertEquals(1, reg.aliasCount)
        assertEquals(1, reg.parametricCount)
    }

    @Test
    fun detectsDuplicatePatternId() {
        val source = registrySource {
            exact("dup", "a", intent = "x")
            exact("dup", "b", intent = "y")
        }
        assertTrue(errorCodes(RegistryCompiler.compile(source)).contains(IssueCode.DUPLICATE_PATTERN_ID))
    }

    @Test
    fun detectsDuplicateExactPhraseAcrossIntents() {
        val source = registrySource {
            exact("p1", "open settings", intent = "intent_a")
            exact("p2", "open settings", intent = "intent_b")
        }
        assertTrue(errorCodes(RegistryCompiler.compile(source)).contains(IssueCode.DUPLICATE_EXACT_PHRASE))
    }

    @Test
    fun detectsDuplicateAliasAcrossIntents() {
        val source = registrySource {
            alias("home", locale = "en", intent = "intent_a")
            alias("home", locale = "en", intent = "intent_b")
        }
        assertTrue(errorCodes(RegistryCompiler.compile(source)).contains(IssueCode.DUPLICATE_ALIAS))
    }

    @Test
    fun detectsTemplateParseError() {
        val source = registrySource {
            parametric("p", "open {target:not_a_type}", intent = "x")
        }
        assertTrue(errorCodes(RegistryCompiler.compile(source)).contains(IssueCode.TEMPLATE_PARSE_ERROR))
    }

    @Test
    fun detectsUnsupportedSlotType() {
        val source = registrySource {
            parametric("p", "open {target:username}", intent = "x")
        }
        val options = CompileOptions(entityRegistry = EntityExtractorRegistry(emptyList()))
        assertTrue(errorCodes(RegistryCompiler.compile(source, options)).contains(IssueCode.UNSUPPORTED_SLOT_TYPE))
    }

    @Test
    fun detectsInvalidGreedySlotNotLast() {
        val source = registrySource {
            parametric("p", "msg {t:free_text:greedy} {u:username}", intent = "x")
        }
        assertTrue(errorCodes(RegistryCompiler.compile(source)).contains(IssueCode.INVALID_GREEDY_SLOT))
    }

    @Test
    fun detectsInvalidGreedySlotNotFreeText() {
        val source = registrySource {
            parametric("p", "open {x:username:greedy}", intent = "x")
        }
        assertTrue(errorCodes(RegistryCompiler.compile(source)).contains(IssueCode.INVALID_GREEDY_SLOT))
    }

    @Test
    fun detectsParametricConflictAsErrorByDefault() {
        val source = registrySource {
            parametric("a", "open {x:username}", intent = "open_profile")
            parametric("b", "open {x:profile_id}", intent = "open_other")
        }
        val result = RegistryCompiler.compile(source)
        assertTrue(result is RegistryCompilationResult.Failure)
        assertTrue(errorCodes(result).contains(IssueCode.PATTERN_CONFLICT))
    }

    @Test
    fun conflictBecomesWarningWhenAllowed() {
        val source = registrySource {
            parametric("a", "open {x:username}", intent = "open_profile")
            parametric("b", "open {x:profile_id}", intent = "open_other")
        }
        val result = RegistryCompiler.compile(source, CompileOptions(failOnConflicts = false))
        assertTrue(result is RegistryCompilationResult.Success)
        assertTrue((result as RegistryCompilationResult.Success).warnings.any { it.code == IssueCode.PATTERN_CONFLICT })
    }

    @Test
    fun samePriorityDifferentStaticTokensDoNotConflict() {
        val source = registrySource {
            parametric("a", "open {x:username}", intent = "open_profile")
            parametric("b", "view {x:username}", intent = "view_profile")
        }
        assertTrue(RegistryCompiler.compile(source) is RegistryCompilationResult.Success)
    }

    @Test
    fun differentPriorityDoesNotConflict() {
        val source = registrySource {
            parametric("a", "open {x:username}", intent = "open_profile", priority = 0)
            parametric("b", "open {x:profile_id}", intent = "open_other", priority = 10)
        }
        assertTrue(RegistryCompiler.compile(source) is RegistryCompilationResult.Success)
    }

    @Test
    fun warnsOnIncompleteLocaleCoverage() {
        val source = registrySource {
            alias("settings", locale = "en", intent = "open_settings")
            alias("ajustes", locale = "es", intent = "open_settings")
            alias("perfil", locale = "es", intent = "open_profile") // missing en for open_profile
        }
        val result = RegistryCompiler.compile(source)
        assertTrue(result is RegistryCompilationResult.Success)
        assertTrue((result as RegistryCompilationResult.Success).warnings.any { it.code == IssueCode.INCOMPLETE_LOCALE })
    }

    @Test
    fun emptyRegistryWarns() {
        val source = registrySource { }
        val result = RegistryCompiler.compile(source)
        assertTrue(result is RegistryCompilationResult.Success)
        assertTrue((result as RegistryCompilationResult.Success).warnings.any { it.code == IssueCode.EMPTY_REGISTRY })
    }
}
