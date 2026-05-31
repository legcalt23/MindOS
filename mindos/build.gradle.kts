import org.gradle.api.GradleException
import org.gradle.api.artifacts.ProjectDependency

plugins {
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

tasks.register("productionReady") {
    description = "Runs repository governance verification and the full test suite."
    group = "verification"
    dependsOn("verifyProductionRepository", "allTests")
}
