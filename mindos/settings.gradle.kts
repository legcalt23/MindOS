pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "mindos-command-router"

// ── Foundation (ZIP01) ────────────────────────────────────────────────
include(":command-router-actions")
include(":navigation-contracts")
include(":command-router-patterns")
include(":command-router-resolver")
include(":command-router-core")
include(":action-executor")

// ── Action Understanding Layer (ZIP02) ────────────────────────────────
include(":mind-normalization")
include(":mind-security")
include(":mind-observability")
include(":mind-kmp-contracts")
include(":mind-kmp-core")
include(":mind-entity-extraction")
include(":mind-action-core")
include(":mind-action-patterns")
include(":mind-action-matching")

// ── Action Execution Layer (ZIP03) ────────────────────────────────────
include(":mind-action-model")
include(":mind-navigation")
include(":mind-reference-resolver")
include(":mind-action-validation")
include(":mind-action-execution")

// ── Testing & Reliability (ZIP04) ─────────────────────────────────────
include(":mind-testkit")

// ── Decompose Integration (ZIP10) ─────────────────────────────────────
include(":mind-decompose")
