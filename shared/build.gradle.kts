import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.sqldelight)
    // CocoaPods is declared with apply=false so the plugin is resolvable on every host,
    // but it is only actually applied on macOS (see `if (isMacOs)` block below) because
    // CocoaPods integration requires Xcode/pod which only exist on Mac. This lets Windows
    // and Linux developers build the Android/Desktop targets without iOS tooling.
    alias(libs.plugins.kotlinNativeCocoapods) apply false
}

val isMacOs = System.getProperty("os.name") == "Mac OS X"

if (isMacOs) {
    apply(plugin = "org.jetbrains.kotlin.native.cocoapods")
}

kotlin {
    android {
        namespace = "com.mindos.app.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    if (isMacOs) {
        listOf(
            iosArm64(),
            iosSimulatorArm64(),
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "Shared"
                isStatic = true
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Pinned explicitly: compose-multiplatform 1.10+ deprecated the
            // `compose.materialIconsExtended` DSL alias. The artifact is anchored to 1.7.3
            // (JetBrains stopped publishing newer versions); migration to Material Symbols
            // vector resources is tracked as a future feature task.
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            // material3 is pinned explicitly in the version catalog because compose-multiplatform
            // 1.10+ deprecated the `compose.material3` DSL alias and emits a deprecation warning
            // when it is used. The recommended pattern is to declare the dependency directly.
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.serializationCore)
            // JSON format for local-first persistence (MindBoost). Core has no string
            // format; JSON is the multiplatform companion used for encrypted local blobs.
            implementation(libs.kotlinx.serializationJson)
            implementation(libs.sqldelight.runtime)
            api(libs.firebase.app)
            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.functions)
            implementation(libs.firebase.storage)
            implementation(libs.kotlinx.datetime)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
        }

        val desktopTest by getting {
            dependencies {
                // Compose Multiplatform UI test framework runs on the JVM via Skiko/AWT.
                // Pulled in only for the desktop test target so Android/iOS test targets
                // remain unaffected. compose.desktop.currentOs supplies the platform-specific
                // Skiko native binary for the host running the tests.
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
                implementation(compose.desktop.currentOs)
                // kotlinx-datetime is declared as `implementation` in commonMain. Gradle
                // does not always include `implementation`-scoped runtime deps in the
                // desktop test runtime classpath when no other commonTest dependency
                // pulls them transitively. Add it explicitly so tests that exercise
                // code paths invoking `Clock.System.now()` resolve at runtime.
                implementation(libs.kotlinx.datetime)
                implementation(libs.sqldelight.sqliteDriver)
            }
        }

        androidMain.dependencies {
            api(project.dependencies.platform(libs.firebase.androidBom))
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.mlkit.face.detection)
            implementation(libs.coil.compose)
            implementation(libs.coil.gif)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.ui)
        }
    }
}

sqldelight {
    databases {
        create("MindBoostDecisionDatabase") {
            packageName.set("com.mindverse.mindboost.core.storage.db")
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

// Detekt is configured to scan all KMP source sets that ship in this module.
// A baseline file (shared/detekt-baseline.xml) captures the existing 2961 findings
// at the time of the audit — those are tolerated as historical debt. New findings
// outside the baseline will fail the build, providing a one-way ratchet:
// code can only get cleaner, never noisier.
//
// To add new findings to the baseline (e.g. after a deliberate temporary regression):
//   ./gradlew :shared:detektBaseline
detekt {
    parallel = true
    buildUponDefaultConfig = true
    ignoreFailures = false
    baseline = file("detekt-baseline.xml")
    source.setFrom(
        "src/commonMain/kotlin",
        "src/androidMain/kotlin",
        "src/desktopMain/kotlin",
        "src/iosMain/kotlin",
        "src/commonTest/kotlin",
    )
}

// Phase 03-07.5 hardening: measure & enforce coverage of the Bayesian core math + sanitization gate.
// Arena tests execute on the JVM (desktop) target, so use the Desktop kover tasks:
//   ./gradlew :shared:koverLogDesktop      (print coverage of the scoped classes)
//   ./gradlew :shared:koverHtmlReportDesktop
//   ./gradlew :shared:koverVerifyDesktop   (fails if engine+sanitizer coverage < 90%)
// Scope is intentionally limited to the rating engine and the sanitization gate.
kover {
    reports {
        filters {
            includes {
                classes(
                    "com.mindos.app.features.arena.rating.engine.*",
                    "com.mindos.app.features.arena.sanitization.*",
                )
            }
        }
        verify {
            rule {
                minBound(90)
            }
        }
    }
}

if (isMacOs) {
    extensions.configure<CocoapodsExtension>("cocoapods") {
        summary = "Shared Kotlin Multiplatform module for MindOS"
        homepage = "https://example.com/mindos"
        version = "1.0"
        ios.deploymentTarget = "15.0"
        pod("FirebaseCore")
        pod("FirebaseAuth")
        extraSpecAttributes["resources"] = "['src/iosMain/resources/GoogleService-Info.plist']"

        framework {
            baseName = "Shared"
            isStatic = true
        }
    }
}

abstract class ArenaTerminologyScannerTask : org.gradle.api.DefaultTask() {
    @get:org.gradle.api.tasks.InputDirectory
    abstract val sourceDir: org.gradle.api.file.DirectoryProperty

    @org.gradle.api.tasks.TaskAction
    fun scan() {
        val dir = sourceDir.get().asFile
        if (!dir.exists()) return
        val forbiddenTerms = listOf("ELO", "Elo", "elo", "IQ", "clinical", "diagnostic", "gambling", "stake", "bet", "casino", "bankroll", "odds", "cash-out", "gana dinero", "cerebro élite", "debilidad mental", "inteligencia general")
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val content = file.readText()
            forbiddenTerms.forEach { term ->
                if (Regex("\\b$term\\b").containsMatchIn(content)) {
                    throw GradleException("Forbidden terminology found: '$term' in file ${file.name}")
                }
            }
        }
    }
}

abstract class ArenaProductionGuardTask : org.gradle.api.DefaultTask() {
    @get:org.gradle.api.tasks.InputDirectory
    abstract val sourceDir: org.gradle.api.file.DirectoryProperty

    @org.gradle.api.tasks.TaskAction
    fun scan() {
        val dir = sourceDir.get().asFile
        if (!dir.exists()) return
        val forbiddenReferences = listOf("Firebase.firestore", "Firebase.functions", "firestore.collection", "mindos-production")
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val content = file.readText()
            forbiddenReferences.forEach { ref ->
                if (content.contains(ref)) {
                    throw GradleException("Production reference found: '$ref' in file ${file.name}")
                }
            }
        }
    }
}

tasks.register<ArenaTerminologyScannerTask>("arenaTerminologyScanner") {
    group = "verification"
    description = "Scans for forbidden terminology in arena module"
    sourceDir.set(layout.projectDirectory.dir("src/commonMain/kotlin/com/mindos/app/features/arena"))
}

tasks.register<ArenaProductionGuardTask>("arenaProductionGuard") {
    group = "verification"
    description = "Scans for production references in arena module"
    sourceDir.set(layout.projectDirectory.dir("src/commonMain/kotlin/com/mindos/app/features/arena"))
}

// Phase 01 / Task 1 hardening: the terminology guard also covers arena TEST sources, so a forbidden
// term (e.g. "ELO") appearing in any new arena test file fails the build. The task no-ops if a dir
// is absent, so this is safe across checkouts.
tasks.register<ArenaTerminologyScannerTask>("arenaTerminologyScannerCommonTest") {
    group = "verification"
    description = "Scans for forbidden terminology in arena commonTest sources"
    sourceDir.set(layout.projectDirectory.dir("src/commonTest/kotlin/com/mindos/app/features/arena"))
}

tasks.register<ArenaTerminologyScannerTask>("arenaTerminologyScannerDesktopTest") {
    group = "verification"
    description = "Scans for forbidden terminology in arena desktopTest sources"
    sourceDir.set(layout.projectDirectory.dir("src/desktopTest/kotlin/com/mindos/app/features/arena"))
}

tasks.named("check") {
    dependsOn(
        "arenaTerminologyScanner",
        "arenaTerminologyScannerCommonTest",
        "arenaTerminologyScannerDesktopTest",
        "arenaProductionGuard",
    )
}

// MindBoost Emociones (Fase 2B) terminology gate. Forbidden lexemes live HERE (outside the scanned
// tree) so the scanner never self-trips. Matching mirrors the in-code EmotionTerminologyScanner:
// case + Spanish-accent folding, whole-word, hyphen-crossing. The commonMain task fails if its dir is
// missing (a typo can't silently disable the gate); test-dir tasks no-op when absent (checkout-safe).
abstract class EmotionsTerminologyScannerTask : org.gradle.api.DefaultTask() {
    @get:org.gradle.api.tasks.Internal
    abstract val sourceDir: org.gradle.api.file.DirectoryProperty

    @get:org.gradle.api.tasks.Input
    abstract val requireDir: org.gradle.api.provider.Property<Boolean>

    @org.gradle.api.tasks.TaskAction
    fun scan() {
        val dir = sourceDir.get().asFile
        if (!dir.exists()) {
            if (requireDir.get()) {
                throw org.gradle.api.GradleException(
                    "emotions terminology gate: required source dir missing (typo?): ${dir.path}",
                )
            }
            return
        }
        val forbidden = listOf(
            "diagnostico", "diagnosticar", "diagnostica", "terapia", "terapeutico", "tratamiento",
            "cura", "curar", "depresion", "ansiedad", "trauma", "trastorno", "patologia",
            "enfermedad mental", "crisis", "panico", "tdah", "toc", "bipolar", "estres",
            "suicidio", "autolesion",
        )
        fun fold(raw: String): String {
            val lowered = raw.lowercase()
            val sb = StringBuilder(lowered.length)
            for (ch in lowered) {
                sb.append(
                    when (ch) {
                        'á' -> 'a'; 'é' -> 'e'; 'í' -> 'i'; 'ó' -> 'o'; 'ú' -> 'u'; 'ü' -> 'u'; 'ñ' -> 'n'
                        else -> ch
                    },
                )
            }
            return sb.toString()
        }
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val folded = fold(file.readText())
            forbidden.forEach { term ->
                if (Regex("\\b" + Regex.escape(term) + "\\b").containsMatchIn(folded)) {
                    throw org.gradle.api.GradleException(
                        "Forbidden terminology found: '$term' in file ${file.name}",
                    )
                }
            }
        }
    }
}

tasks.register<EmotionsTerminologyScannerTask>("emotionsTerminologyScanner") {
    group = "verification"
    description = "Scans for forbidden clinical terminology in the emotions module (commonMain)"
    sourceDir.set(layout.projectDirectory.dir("src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions"))
    requireDir.set(true)
}

tasks.register<EmotionsTerminologyScannerTask>("emotionsTerminologyScannerCommonTest") {
    group = "verification"
    description = "Scans for forbidden terminology in emotions commonTest sources"
    sourceDir.set(layout.projectDirectory.dir("src/commonTest/kotlin/com/mindos/app/features/mindboost/emotions"))
    requireDir.set(false)
}

tasks.register<EmotionsTerminologyScannerTask>("emotionsTerminologyScannerDesktopTest") {
    group = "verification"
    description = "Scans for forbidden terminology in emotions desktopTest sources"
    sourceDir.set(layout.projectDirectory.dir("src/desktopTest/kotlin/com/mindos/app/features/mindboost/emotions"))
    requireDir.set(false)
}

tasks.named("check") {
    dependsOn(
        "emotionsTerminologyScanner",
        "emotionsTerminologyScannerCommonTest",
        "emotionsTerminologyScannerDesktopTest",
    )
}

// INFRA-05 · MindBoost global copy-safety gate. Forbidden lexemes live HERE (outside the scanned tree)
// so the scanner never self-trips; this list is a PARITY copy of
// shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/copy/ForbiddenLexicon.kt — keep
// both in sync. Matching mirrors the in-code MindBoostCopySafetyScanner: case + Spanish-accent folding,
// whole-word, hyphen/negation-crossing (zero claims even to negate them). Scope: the 7 feature modules
// (focus/memory/speed/logic/decisiones/language/creativity) + AppStrings; EXCLUDES core/copy (the
// lexicon), emotions and arena (their own gates), and profile/presentation. commonMain fails if NO dir
// is present (typo guard); test-dir tasks no-op when absent (checkout-safe).
abstract class MindBoostCopySafetyScannerTask : org.gradle.api.DefaultTask() {
    @get:org.gradle.api.tasks.Internal
    abstract val sourceDirs: org.gradle.api.file.ConfigurableFileCollection

    @get:org.gradle.api.tasks.Input
    abstract val requireAnyDir: org.gradle.api.provider.Property<Boolean>

    @org.gradle.api.tasks.TaskAction
    fun scan() {
        val dirs = sourceDirs.files.filter { it.exists() }
        if (dirs.isEmpty()) {
            if (requireAnyDir.get()) {
                throw org.gradle.api.GradleException("mindboost copy-safety gate: no source dirs present (typo?)")
            }
            return
        }
        val forbidden = listOf(
            "inteligencia", "IQ", "Gf", "coeficiente intelectual", "coeficiente de inteligencia",
            "te hace mas listo", "mejores decisiones", "decides mejor", "en la vida real",
            "multitarea", "productividad", "al volante", "conduccion",
            "regulacion emocional", "inteligencia emocional", "controlas tus emociones",
            "autocontrol", "personalidad", "te distraes menos con el movil",
            "usas menos el telefono", "rindes mejor en el trabajo", "mejor en los estudios",
            "TDAH", "ADHD", "demencia", "Alzheimer", "ansiedad", "depresion",
            "diagnostico", "clinico", "terapeutico", "tratamiento", "cura", "sintoma",
            "trastorno", "deterioro cognitivo", "salud mental",
            "apuesta", "apostar", "fichas", "bote", "casino", "odds", "cuota",
            "bankroll", "stake", "bet",
            "fake news", "noticias falsas", "desinformacion", "bulos",
            "fuentes fiables", "medios fiables", "detectar mentiras", "verificar hechos del mundo real",
            "reconfigura tu cerebro", "neuroplasticidad garantizada", "potencia tu cerebro",
            "desbloquea tu mente", "entrena tu cerebro como un musculo",
        )
        fun fold(raw: String): String {
            val lowered = raw.lowercase()
            val sb = StringBuilder(lowered.length)
            for (ch in lowered) {
                sb.append(
                    when (ch) {
                        'á' -> 'a'; 'é' -> 'e'; 'í' -> 'i'; 'ó' -> 'o'; 'ú' -> 'u'; 'ü' -> 'u'; 'ñ' -> 'n'
                        else -> ch
                    },
                )
            }
            return sb.toString()
        }
        dirs.forEach { dir ->
            dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
                val folded = fold(file.readText())
                forbidden.forEach { term ->
                    if (Regex("\\b" + Regex.escape(fold(term)) + "\\b").containsMatchIn(folded)) {
                        throw org.gradle.api.GradleException(
                            "Forbidden copy terminology found: '$term' in file ${file.name}",
                        )
                    }
                }
            }
        }
    }
}

val mindboostCopyFeatureModules =
    listOf("focus", "memory", "speed", "logic", "decisiones", "language", "creativity")

tasks.register<MindBoostCopySafetyScannerTask>("mindboostCopySafetyScanner") {
    group = "verification"
    description = "Scans forbidden far-transfer/clinical/IQ/gambling/misinformation copy in MindBoost commonMain + AppStrings"
    sourceDirs.from(
        mindboostCopyFeatureModules.map {
            layout.projectDirectory.dir("src/commonMain/kotlin/com/mindos/app/features/mindboost/$it")
        } + layout.projectDirectory.dir("src/commonMain/kotlin/com/mindos/app/design"),
    )
    requireAnyDir.set(true)
}

tasks.register<MindBoostCopySafetyScannerTask>("mindboostCopySafetyScannerCommonTest") {
    group = "verification"
    description = "Scans forbidden copy terminology in MindBoost commonTest sources"
    sourceDirs.from(
        mindboostCopyFeatureModules.map {
            layout.projectDirectory.dir("src/commonTest/kotlin/com/mindos/app/features/mindboost/$it")
        },
    )
    requireAnyDir.set(false)
}

tasks.register<MindBoostCopySafetyScannerTask>("mindboostCopySafetyScannerDesktopTest") {
    group = "verification"
    description = "Scans forbidden copy terminology in MindBoost desktopTest sources"
    sourceDirs.from(
        mindboostCopyFeatureModules.map {
            layout.projectDirectory.dir("src/desktopTest/kotlin/com/mindos/app/features/mindboost/$it")
        },
    )
    requireAnyDir.set(false)
}

tasks.named("check") {
    dependsOn(
        "mindboostCopySafetyScanner",
        "mindboostCopySafetyScannerCommonTest",
        "mindboostCopySafetyScannerDesktopTest",
    )
}
