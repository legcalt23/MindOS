# Pattern Engine

## Overview

The pattern engine takes normalized text input and returns a deterministic `PatternMatchResult`:

```kotlin
sealed interface PatternMatchResult {
    data class Matched(val match: PatternMatch, val pattern: CommandPattern) : PatternMatchResult
    data object NoMatch : PatternMatchResult
    data class Ambiguous(val candidates: List<PatternMatchCandidate>) : PatternMatchResult
}
```

No exceptions. No fallbacks. One definitive answer.

## Pattern types

### ExactCommandPattern

Matches normalized input exactly. Higher specificity than any parameterized pattern with the same template length.

```kotlin
ExactCommandPattern(
    id = PatternId("open-settings"),
    command = "open settings",
    priority = PatternPriority(10)
)
```

Specificity: `PatternSpecificity(staticTokenCount=2, parameterCount=0)`.

### ParameterizedCommandPattern

Template with `{param}` placeholders. Parameters are validated against the regex `[a-zA-Z_][a-zA-Z0-9_]*`.

```kotlin
ParameterizedCommandPattern(
    id = PatternId("open-target"),
    template = "open {target}"
)
```

**Parameter matching rules:**
- `{param}` in final position: greedy — matches all remaining whitespace-delimited tokens
- `{param}` in non-final position: matches exactly one token
- A parameter cannot be empty — it must consume at least one token

## Ranking algorithm

When multiple patterns match the same input:

1. Filter all matching patterns
2. Sort by `(priority DESC, specificity DESC, patternId ASC)` — the last criterion ensures a stable sort order
3. Group the top-ranked candidates that share the same `(priority, specificity)`
4. If exactly one: `Matched`
5. If more than one: `Ambiguous`

The tie-break by `patternId` (lexicographic) is used only to make the sort deterministic, not to break genuine semantic ambiguity — two patterns with the same priority and same specificity matching the same input ARE ambiguous.

## Specificity comparison

```
ExactCommandPattern("open settings"):           (static=2, params=0)
ParameterizedCommandPattern("open {target}"):   (static=1, params=1)
ParameterizedCommandPattern("{action} {obj}"):  (static=0, params=2)
```

More static tokens → higher specificity. Fewer parameters → higher specificity (at the same static token count).

## Normalization

`DefaultCommandNormalizer`:
- `trim()` leading/trailing whitespace
- `lowercase()`
- Collapse internal whitespace to single space (includes tabs, newlines)

Normalization happens in `DefaultCommandRouter` before passing to `PatternRegistry`. Pattern templates are also normalized at construction time so matching is always against the same canonical form.

## Extension

Implement `CommandPattern` to add new pattern syntaxes:

```kotlin
class RegexCommandPattern(
    override val id: PatternId,
    override val priority: PatternPriority,
    private val pattern: Regex
) : CommandPattern {
    override fun match(normalizedInput: String): PatternMatch? = ...
    override fun specificity(): PatternSpecificity = PatternSpecificity(0, 1)
}
```
