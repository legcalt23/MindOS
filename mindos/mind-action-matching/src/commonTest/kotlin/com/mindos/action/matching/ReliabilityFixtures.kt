package com.mindos.action.matching

import com.mindos.action.pattern.registrySource
import com.mindos.testkit.CommandGenerator
import com.mindos.testkit.CommandGrammar
import com.mindos.testkit.CommandTemplate

/**
 * Shared fixtures for the ZIP04 reliability suites: a registry plus a grammar/alias set that mirror
 * it, so generated commands have a known expected intent. All aliases are locale "und" so they
 * resolve under the default context.
 */
internal object Reliability {

    val source = registrySource {
        exact("p_settings", "open settings", intent = "open_settings")
        exact("p_settings_short", "settings", intent = "open_settings")
        alias("home", locale = "und", intent = "open_home")
        alias("inbox", locale = "und", intent = "open_messages")
        parametric("p_profile", "open {target:username}", intent = "open_profile")
        parametric("p_story", "open story {storyId:story_id}", intent = "open_story")
        parametric("p_msg", "message {user:username} {text:free_text:greedy}", intent = "send_message")
    }

    val matcher: DefaultActionMatcher = DefaultActionMatcher(RegistryCompiler.compile(source).getOrThrow())

    val grammar = CommandGrammar(
        templates = listOf(
            CommandTemplate("open_settings", "open settings"),
            CommandTemplate("open_settings", "settings"),
            CommandTemplate("open_profile", "open {target}"),
            CommandTemplate("open_story", "open story {storyId}"),
            CommandTemplate("send_message", "message {user} {text}"),
        ),
        slots = mapOf(
            "target" to listOf("ana", "bob", "carol", "dave", "emma"),
            "storyId" to listOf("abc123", "st_42", "xyz789", "id0001"),
            "user" to listOf("ana", "bob", "carol"),
            "text" to listOf("hello", "hi there", "good morning", "hola que tal"),
        ),
    )

    val aliases = listOf("home" to "open_home", "inbox" to "open_messages")

    val generator = CommandGenerator(grammar)
}
