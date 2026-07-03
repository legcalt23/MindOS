import org.gradle.api.GradleException
import org.gradle.api.artifacts.ProjectDependency

plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

val productionRepositoryModules = listOf(
    "command-router-actions",
    "navigation-contracts",
    "command-router-patterns",
    "command-router-resolver",
    "command-router-core",
    "action-executor",
    "mind-normalization",
    "mind-security",
    "mind-observability",
    "mind-kmp-contracts",
    "mind-kmp-core",
    "mind-command-runtime",
    "mind-app-runtime",
    "mind-android-voice",
    "mind-speech",
    "mind-entity-extraction",
    "mind-action-core",
    "mind-action-patterns",
    "mind-action-matching",
    "mind-action-model",
    "mind-navigation",
    "mind-reference-resolver",
    "mind-action-validation",
    "mind-action-execution",
    "mind-testkit",
    "mind-decompose",
)

val productionModuleLayers = mapOf(
    "command-router-actions" to "legacy-contracts",
    "navigation-contracts" to "legacy-contracts",
    "command-router-patterns" to "legacy-engine",
    "command-router-resolver" to "legacy-engine",
    "command-router-core" to "legacy-api",
    "action-executor" to "legacy-execution",
    "mind-normalization" to "engine-foundation",
    "mind-security" to "engine-foundation",
    "mind-observability" to "engine-foundation",
    "mind-kmp-contracts" to "public-contracts",
    "mind-kmp-core" to "public-api",
    "mind-command-runtime" to "runtime-orchestration",
    "mind-app-runtime" to "app-public-api",
    "mind-android-voice" to "android-platform-adapter",
    "mind-speech" to "speech-contracts",
    "mind-entity-extraction" to "engine-foundation",
    "mind-action-core" to "engine-core",
    "mind-action-patterns" to "engine-core",
    "mind-action-matching" to "engine-core",
    "mind-action-model" to "execution-contracts",
    "mind-navigation" to "execution-contracts",
    "mind-reference-resolver" to "execution-services",
    "mind-action-validation" to "execution-services",
    "mind-action-execution" to "execution-orchestration",
    "mind-testkit" to "testkit",
    "mind-decompose" to "app-integration",
)

val requiredProductionRepositoryDocs = listOf(
    "README.md",
    "CONTRIBUTING.md",
    "SECURITY.md",
    "CHANGELOG.md",
    "CODEOWNERS",
    "docs/production/repository-structure.md",
    "docs/production/governance.md",
    "docs/production/release-process.md",
    "docs/production/implementation-roadmap.md",
    "docs/kmp-contracts.md",
    "docs/kmp-core-implementation.md",
    "docs/command-runtime.md",
    "docs/app-runtime.md",
    "docs/voice/baseline-routing-report.md",
    "docs/voice/speech-contracts.md",
    "docs/voice/headless-batch-pipeline.md",
    "docs/voice/phase-02-security-decisions.md",
    "docs/voice/phase-03-security-decisions.md",
    "docs/voice/phase-04-security-decisions.md",
    "docs/voice/phase-05-security-decisions.md",
    "docs/voice/production-batch-broker.md",
    "docs/adr/ADR-000-command-router-production-hardening.md",
    "docs/adr/ADR-voice-001-canonical-command-stack.md",
    "docs/command-router/known-blockers.md",
    "docs/checklists/production-launch-checklist.md",
    "docs/adr/0000-template.md",
    "docs/rfc/0000-template.md",
    "docs/build-logic-conventions.md",
    "docs/reviews/architect-review.md",
    "docs/reviews/security-review.md",
)

tasks.register("allTests") {
    description = "Runs all tests across all modules"
    group = "verification"
    subprojects.forEach { sub ->
        dependsOn("${sub.path}:allTests")
    }
}

tasks.register("verifyProductionRepository") {
    description = "Verifies ZIP07 production repository structure, governance docs, and main dependency boundaries."
    group = "verification"

    doLast {
        val missingModules = productionRepositoryModules.filter { !project(":$it").projectDir.isDirectory }
        if (missingModules.isNotEmpty()) {
            throw GradleException("Missing production modules: ${missingModules.joinToString()}")
        }

        val unclassifiedModules = productionRepositoryModules.filter { it !in productionModuleLayers }
        if (unclassifiedModules.isNotEmpty()) {
            throw GradleException("Production modules without architecture layer: ${unclassifiedModules.joinToString()}")
        }

        val missingDocs = requiredProductionRepositoryDocs.filter { path ->
            !layout.projectDirectory.file(path).asFile.isFile
        }
        if (missingDocs.isNotEmpty()) {
            throw GradleException("Missing production repository docs: ${missingDocs.joinToString()}")
        }

        val graph = linkedMapOf<String, List<String>>()
        subprojects
            .filter { subproject -> subproject.name in productionRepositoryModules }
            .forEach { subproject ->
                val dependencies = mutableListOf<String>()
                subproject.configurations
                    .filter { configuration ->
                        configuration.name.contains("Main", ignoreCase = true) &&
                            !configuration.name.contains("Test", ignoreCase = true)
                    }
                    .forEach { configuration ->
                        configuration.dependencies.forEach { dependency ->
                            if (dependency is ProjectDependency) {
                                dependencies += dependency.path.removePrefix(":")
                            }
                        }
                    }
                graph[subproject.name] = dependencies
                    .distinct()
                    .filter { dependencyName -> dependencyName in productionRepositoryModules }
            }

        val prohibitedDependencies = mapOf(
            "command-router-core" to setOf("action-executor", "navigation-contracts"),
            "command-router-resolver" to setOf("action-executor", "navigation-contracts"),
            "command-router-actions" to productionRepositoryModules.toSet() - "command-router-actions",
            "navigation-contracts" to productionRepositoryModules.toSet() - "navigation-contracts",
            "mind-action-core" to setOf("mind-action-matching", "mind-action-execution"),
            "mind-action-model" to productionRepositoryModules.toSet() - "mind-action-model",
            "mind-navigation" to productionRepositoryModules.toSet() - "mind-navigation",
            "mind-observability" to productionRepositoryModules.toSet() - "mind-observability",
            "mind-security" to productionRepositoryModules.toSet() - "mind-security",
            "mind-kmp-contracts" to productionRepositoryModules.toSet() - "mind-kmp-contracts",
        )

        val violations = prohibitedDependencies.flatMap { (source, forbiddenTargets) ->
            graph.getValue(source).filter { it in forbiddenTargets }.map { target -> "$source -> $target" }
        }
        if (violations.isNotEmpty()) {
            throw GradleException("Production dependency boundary violations: ${violations.joinToString()}")
        }

        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()
        val cycles = mutableListOf<String>()

        fun visit(module: String, path: List<String>) {
            if (module in visiting) {
                cycles += (path + module).joinToString(" -> ")
                return
            }
            if (!visited.add(module)) return
            visiting += module
            graph[module].orEmpty().forEach { visit(it, path + module) }
            visiting -= module
        }

        productionRepositoryModules.forEach { visit(it, emptyList()) }
        if (cycles.isNotEmpty()) {
            throw GradleException("Production dependency cycles detected: ${cycles.joinToString("; ")}")
        }
    }
}

tasks.register("verifyCommandRouterHardening") {
    description = "Verifies command-router hardening docs and regression test reports exist."
    group = "verification"
    dependsOn("allTests", "verifyProductionRepository")

    doLast {
        val requiredReports = listOf(
            "mind-security/build/test-results/jvmTest",
            "mind-reference-resolver/build/test-results/jvmTest",
            "mind-action-execution/build/test-results/jvmTest",
            "mind-navigation/build/test-results/jvmTest",
            "mind-kmp-core/build/test-results/jvmTest",
            "mind-command-runtime/build/test-results/jvmTest",
            "mind-speech/build/test-results/jvmTest",
            "mind-decompose/build/test-results/jvmTest",
        )
        val missingReports = requiredReports.filter { path ->
            layout.projectDirectory.dir(path).asFile
                .listFiles { file -> file.name.startsWith("TEST-") && file.extension == "xml" }
                .isNullOrEmpty()
        }
        if (missingReports.isNotEmpty()) {
            throw GradleException("Missing command-router hardening test reports: ${missingReports.joinToString()}")
        }
    }
}

tasks.register("productionReady") {
    description = "Runs repository governance verification and the full test suite."
    group = "verification"
    dependsOn("verifyProductionRepository", "allTests", "verifyCommandRouterHardening")
}
