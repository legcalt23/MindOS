package com.mindos.testkit

/**
 * Curated ambiguity cases (ZIP11 AMBIGUITY_DATASET): conflicting patterns, overlapping aliases and
 * uncertain entity matches — each with a **deterministic** expected resolution. The engine must
 * resolve all of these the same way every time. Verified against a concrete registry in the
 * matching module's test suite.
 *
 * Expected values reference this reference registry:
 *   exact "open settings" -> open_settings; "settings" -> open_settings
 *   alias "inbox"/"home" (und)
 *   parametric "open {target:username}" -> open_profile
 *             "open story {storyId:story_id}" -> open_story
 *             "message {user:username} {text:free_text:greedy}" -> send_message
 */
object AmbiguityDataset {

    val cases: List<AmbiguityCase> = listOf(
        AmbiguityCase(
            input = "open settings",
            description = "exact phrase vs parametric 'open {username}' — exact wins by exactness, deterministically",
            expected = "open_settings",
        ),
        AmbiguityCase(
            input = "open story",
            description = "'story' is a valid username, so 'open {username}' matches; 'open story {id}' needs 3 tokens",
            expected = "open_profile",
        ),
        AmbiguityCase(
            input = "open ana",
            description = "only the parametric profile pattern matches a single trailing username",
            expected = "open_profile",
        ),
        AmbiguityCase(
            input = "open story abc123",
            description = "3-token form matches the more specific story pattern, not 'open {username}'",
            expected = "open_story",
        ),
        AmbiguityCase(
            input = "open \$\$\$",
            description = "username slot rejects '\$\$\$' — uncertain entity resolves to no false positive",
            expected = "no_match",
        ),
        AmbiguityCase(
            input = "settings",
            description = "short exact phrase resolves unambiguously",
            expected = "open_settings",
        ),
    )
}
