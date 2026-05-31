# MindOS Build Logic & Conventions (ZIP13)

The repository uses an included build, `build-logic/`, that provides **convention plugins** so the 22
modules share one build configuration instead of duplicating it.

## Convention plugins

| Plugin id | Applies | Adds |
|-----------|---------|------|
| `mindos.kmp-library` | `org.jetbrains.kotlin.multiplatform` | `jvm()` target, `kotlin("test")` in `commonTest` |
| `mindos.kmp-serialization` | `mindos.kmp-library` + `org.jetbrains.kotlin.plugin.serialization` | (compiler plugin for `@Serializable`) |

Wired via `pluginManagement { includeBuild("build-logic") }` in `settings.gradle.kts`. Plugin Kotlin
versions are pinned in `build-logic/build.gradle.kts` (matching the main catalog's Kotlin version).

### Before / after

```kotlin
// before — repeated in every module
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}
kotlin {
    jvm()
    sourceSets {
        commonMain.dependencies { /* … */ }
        commonTest.dependencies { implementation(libs.kotlin.test) /* … */ }
    }
}

// after — boilerplate gone; only module-specific deps remain
plugins { id("mindos.kmp-serialization") }
kotlin {
    sourceSets {
        commonMain.dependencies { /* … */ }
        // commonTest only when it has deps beyond kotlin.test (added by the convention)
    }
}
```

Module-specific dependencies (project deps, coroutines, serialization runtime, decompose…) stay in
each module and continue to use the `libs` version catalog.

## Dependency rules (enforced)

`./gradlew verifyProductionRepository` (root `build.gradle.kts`) enforces the module graph at build
time: required modules present, required docs present, **prohibited cross-module dependencies**
rejected, and **dependency cycles** detected. `./gradlew productionReady` runs that gate + `allTests`.

## Module catalog ("explain every module")

**Foundation (ZIP01) — generic command router (legacy, kept for compatibility):**

| Module | Responsibility |
|--------|---------------|
| `command-router-actions` | Typed `CommandAction` DSL, value types, metadata |
| `navigation-contracts` | Generic `Navigator`/`NavigationCommand`/`NavigationResult` |
| `command-router-patterns` | Original pattern engine (superseded by `mind-action-matching`) |
| `command-router-resolver` | Pattern→action resolver |
| `command-router-core` | `CommandRouter` orchestration + trace |
| `action-executor` | Generic action executor + navigation handler |

**Understanding layer (ZIP02):**

| Module | Responsibility |
|--------|---------------|
| `mind-normalization` | Deterministic normalization + tokenization (zero per-request regex) |
| `mind-entity-extraction` | Typed entities + validators + normalizers |
| `mind-action-core` | `ActionMatcher`, `ActionUnderstandingResult`, `MindIntent` contracts |
| `mind-action-patterns` | Declarative `ActionPattern`, slots, `RegistrySource` DSL |
| `mind-action-matching` | `RegistryCompiler`, indices + trie, ranking, `DefaultActionMatcher` |
| `mind-security` | Input security inspection + abuse/rate-limit controls |
| `mind-observability` | Telemetry, metrics, structured logs, immutable audit, dashboards |

**Execution layer (ZIP03):**

| Module | Responsibility |
|--------|---------------|
| `mind-action-model` | Sealed `MindAction`, `ReferenceKind`, `RequestId` |
| `mind-navigation` | Sealed `MindNavigationCommand`, `NavigationCoordinator` |
| `mind-reference-resolver` | `ReferenceResolver` (username→id…) + cache |
| `mind-action-validation` | `ActionValidator`, `Authorizer` |
| `mind-action-execution` | `ActionExecutor`, `IntentToActionMapper`, results |

**Integration & public API:**

| Module | Responsibility |
|--------|---------------|
| `mind-decompose` (ZIP10) | Real Decompose navigation: `RootComponent`, coordinator, deep links |
| `mind-kmp-contracts` (ZIP08) | Serializable, versioned DTO contracts for app/backend |
| `mind-kmp-core` (ZIP09) | Public KMP router implementation over the contracts |

**Tooling:**

| Module | Responsibility |
|--------|---------------|
| `mind-testkit` (ZIP04/11) | Seeded generators, fuzz, corpus, datasets, benchmark harness |

## Future evolution

- Migrate the version catalog into `build-logic` (type-safe `libs` inside convention plugins) to
  remove the pinned-version duplication.
- Add `mindos.kmp-application` / iOS-target conventions when non-JVM targets are enabled.
- Derive the `verifyProductionRepository` prohibited-dependency map from a declared layer index so it
  cannot drift as modules are added.
