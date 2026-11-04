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
    // CocoaPods registers `cocoapods` as a nested extension of the `kotlin` extension,
    // NOT the project — configuring it via `project.extensions` threw
    // UnknownDomainObjectException on the macOS CI runner (invisible on Windows, where
    // this whole block is skipped). gitlive's Firebase SDK does NOT link the native
    // Firebase iOS SDK transitively, so every gitlive module used in commonMain needs its
    // matching pod on the link path: firebase-app→FirebaseCore, -auth→FirebaseAuth,
    // -firestore→FirebaseFirestore, -functions→FirebaseFunctions, -storage→FirebaseStorage.
    // Required for both the framework link and the iosSimulatorArm64 test binary.
    (kotlin as org.gradle.api.plugins.ExtensionAware)
        .extensions.configure<CocoapodsExtension>("cocoapods") {
            summary = "Shared Kotlin Multiplatform module for MindOS"
            homepage = "https://example.com/mindos"
            version = "1.0"
            ios.deploymentTarget = "15.0"
            // Pinned to the exact Firebase iOS SDK version gitlive 2.4.0 links against
            // (its firebase-cocoapods = "11.8.0"). Unpinned, CocoaPods resolves the latest
            // (12.x), which (a) mismatches gitlive's cinterop bindings and (b) needs an even
            // newer toolchain. 11.8.0 is built with Swift 6, so the CI runner must use
            // Xcode 16 (see .github/workflows/ios-build.yml).
            pod("FirebaseCore") { version = "11.8.0" }
            pod("FirebaseAuth") { version = "11.8.0" }
            pod("FirebaseFirestore") { version = "11.8.0" }
            pod("FirebaseFunctions") { version = "11.8.0" }
            pod("FirebaseStorage") { version = "11.8.0" }
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
// whole-word, hyphen/negation-crossing (zero claims even to negate them). Scope: the modules listed in
// `mindboostCopyFeatureModules` below — that list is the single source of truth and is deliberately NOT
// re-enumerated here, because a prose copy of it goes stale (this comment said "the 7 feature modules"
// and "EXCLUDES emotions" long after emotions and focus2 joined) — plus AppStrings. Still OUT, each with
// a measured hit list rather than an assumption: core/copy (the lexicon must name what it forbids),
// arena (its own gate), `profile` and `coach` (see the note on the list below). commonMain fails if NO
// dir is present (typo guard); test-dir tasks no-op when absent (checkout-safe).
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
        // COPY_SAFETY_LEXICON_PARITY_START — pinned byte-for-byte against ForbiddenLexicon.ALL by
        // CopySafetyLexiconParityTest. Two lists that drift silently = a gate that scans the wrong words.
        // These are FORMS, not lemmas: ForbiddenLexicon declares an Inflection per term and expands it,
        // so the feminine and plural of a Spanish lemma are covered here too (MB-EMO-0023). DERIVED —
        // regenerate from the declarations, never hand-edit a form into this block.
        val forbidden = listOf(
            // far-transfer
            "inteligencia", "inteligencias", "IQ", "Gf", "coeficiente intelectual", "coeficiente de inteligencia",
            "te hace mas listo", "mejores decisiones", "decides mejor", "en la vida real", "multitarea",
            "multitareas", "productividad", "productividades", "al volante", "conduccion", "conducciones",
            "regulacion emocional", "inteligencia emocional", "controlas tus emociones", "autocontrol",
            "autocontroles", "personalidad", "personalidades", "te distraes menos con el movil",
            "usas menos el telefono", "rindes mejor en el trabajo", "mejor en los estudios", "transferencia amplia",
            "transferencia lejana", "generalizacion amplia",
            // clinical
            "TDAH", "ADHD", "demencia", "demencias", "Alzheimer", "ansiedad", "ansiedades", "depresion",
            "depresiones", "diagnostico", "diagnostica", "diagnosticos", "diagnosticas", "clinico", "clinica",
            "clinicos", "clinicas", "terapeutico", "terapeutica", "terapeuticos", "terapeuticas", "tratamiento",
            "tratamientos", "cura", "curas", "sintoma", "sintomas", "trastorno", "trastornos", "deterioro cognitivo",
            "salud mental",
            // gambling
            "apuesta", "apuestas", "apostar", "fichas", "bote", "botes", "casino", "casinos", "odds", "cuota",
            "bankroll", "stake", "bet",
            // misinformation
            "fake news", "noticias falsas", "desinformacion", "desinformaciones", "bulos", "fuentes fiables",
            "medios fiables", "detectar mentiras", "verificar hechos del mundo real",
            // pseudoscience
            "reconfigura tu cerebro", "neuroplasticidad garantizada", "potencia tu cerebro", "desbloquea tu mente",
            "entrena tu cerebro como un musculo",
        )
        // COPY_SAFETY_LEXICON_PARITY_END
        // COPY_SAFETY_MATCHER_PARITY_START — pinned against MindBoostCopySafetyScanner by
        // CopySafetyLexiconParityTest. A gate has three parts: the mechanism, the vocabulary it
        // compares against, and HOW it compares. MB-EMO-0023 was the vocabulary; MB-EMO-0030 is
        // this: the accent fold and the `\b` anchoring below exist twice, and until now only the
        // word list was pinned, so changing the folding on one side would leave the other green
        // while the build gate and the runtime compliance tests policed different rules.
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
        // COPY_SAFETY_MATCHER_PARITY_END
    }
}

// "focus2" is the Focus V5 engine package. It is listed from its very first commit (S1) so the v2 engine can
// never be born outside the copy-safety gate: a module that is added to the scanner only once it already has
// user-facing copy has, by definition, shipped un-scanned copy first.
// Ola 3 · R3 · P0 — `emotions` joined the list, closing MB-EMO-0016. Until now the module's ONLY lexical control
// was its own `EmotionTerminologyComplianceTest`, with a private term list over a hand-maintained set of strings:
// a gate that scans a list someone has to remember to extend. Four consecutive waves shipped user copy the build
// never looked at. The gate is exhaustive by DIRECTORY, which is what makes forgetting impossible.
// R19 · 2026-09-07 — `presentation` joins: the hub is where Enfoque is MOUNTED and it was outside the
// gate, so `MindBoostHubScreen` shipped a forbidden lexeme with the build green. Measured before adding:
// 2 files, 1 offending line (now corrected) => the gate goes red on that defect and on nothing else.
// STILL OUT, measured and registered, not forgotten: `profile` (62 files, 3 offending lines after the
// R19 fix — its dimension label "Regulación emocional" IS a forbidden far-transfer literal, so gating it
// needs a product decision on what that dimension is called) and `coach` (27 files, 33 offending lines —
// its safety policy must NAME the terms it blocks, so the directory cannot be gated as it stands).
val mindboostCopyFeatureModules = listOf(
    "focus", "focus2", "memory", "speed", "logic", "decisiones", "language", "creativity", "emotions",
    "presentation",
)

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

// SYSTEM-1 slice 6-R2/6-R3: the MECHANICAL no-wiring barrier for the evidence-only cognitive-session contract
// (core.session.cognitive.evidence). Build-gate twin of CognitiveSessionEvidenceBoundaryTest so the barrier's own
// removal is a build-config diff (visible in review), not a silently-deletable test. Fails if the evidence package
// imports profile.* OR names a retired profile-currency symbol OR reintroduces a retired evaluative field/type
// shape (word-boundary, comments included; the `\b` avoids false positives on the package's own
// SessionEvidenceGates/SessionEvidenceAudit and on CoachCalibrationState). 6R3 §7/§10 added the evaluative-shape
// Tier 1/Tier 2 checks (`spanMs` is deliberately NOT banned — kept on ExerciseScaleEvidence, see the reopen doc §10).
// 6R3 §18 C2.3/C3 added: Tier 2 the retired block-readiness-for-ranking vocabulary, and Tier 1 four more type
// tokens (`Duration`/`typealias`/keyed-collection`/`the top type`) that a future evaluative field could reach for.
abstract class EvidenceBoundaryScannerTask : org.gradle.api.DefaultTask() {
    @get:org.gradle.api.tasks.Internal
    abstract val sourceDir: org.gradle.api.file.DirectoryProperty

    @org.gradle.api.tasks.TaskAction
    fun scan() {
        val dir = sourceDir.get().asFile
        if (!dir.isDirectory) {
            throw org.gradle.api.GradleException("evidence boundary gate: package dir missing (${dir.path})")
        }
        val forbiddenSymbols = listOf(
            "MindBoostProfileAggregator", "toCognitiveModuleId", "CognitiveModuleId", "DimensionStatus",
            "ProfileTrend", "SessionEvidence", "CalibrationPolicy", "ModuleContribution", "toModuleContribution",
            "ProfileEstimate", "CalibrationState",
        )
        // 6R3 §7/§10/§18-C3 Tier 1: type tokens an evaluative ratio/score (or an opaque escape hatch around this
        // scanner's own literal-token approach) needs to exist at all — structural and forward-proof (catches a
        // FUTURE `List<Double>`/`.toDouble()`/keyed-lookup/type-erased-field reintroduction, not just a named one).
        val tier1BannedTypeTokens = listOf(
            "Double", "Float", "DoubleArray", "FloatArray", "Duration", "typealias", "Map", "Any",
        )
        // 6R3 §7/§10 Tier 2: the SPECIFIC retired field/type names the psychometrician found reconstructible into
        // a profile proxy from this contract's shipped fields alone, PLUS (§18 C2.3) the retired
        // block-readiness-for-ranking vocabulary.
        val tier2RetiredEvaluativeLiterals = listOf(
            "EvidenceQuantityMaturity", "EvidenceQuantityPolicy", "FreshnessStatus", "FreshnessPolicy",
            "cappedObservationCount", "observationCountProgress", "distinctDayProgress", "orderedNormalizedScores",
            "COMPARABLE_WITHIN_SCALE", "rankSessionCount", "acceleratingSessionCount", "distinctDays",
            "RankReadiness", "RankPolicy", "RankBlockReason", "blockingReasons",
        )
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val text = file.readText()
            text.lineSequence().map { it.trim() }.forEach { line ->
                if (line.startsWith("import com.mindos.app.features.mindboost.profile.")) {
                    throw org.gradle.api.GradleException(
                        "evidence boundary gate: profile.* import in ${file.name}: $line",
                    )
                }
            }
            forbiddenSymbols.forEach { symbol ->
                if (Regex("\\b" + Regex.escape(symbol) + "\\b").containsMatchIn(text)) {
                    throw org.gradle.api.GradleException(
                        "evidence boundary gate: retired profile-currency symbol '$symbol' in ${file.name}",
                    )
                }
            }
            (tier1BannedTypeTokens + tier2RetiredEvaluativeLiterals).forEach { token ->
                if (Regex("\\b" + Regex.escape(token) + "\\b").containsMatchIn(text)) {
                    throw org.gradle.api.GradleException(
                        "evidence boundary gate: retired evaluative shape '$token' reintroduced in ${file.name}",
                    )
                }
            }
        }
    }
}

tasks.register<EvidenceBoundaryScannerTask>("evidenceBoundaryScanner") {
    group = "verification"
    description = "Fails if core.session.cognitive.evidence imports profile.*, names a retired profile-currency " +
        "symbol, or reintroduces a retired evaluative field/type shape"
    sourceDir.set(
        layout.projectDirectory.dir(
            "src/commonMain/kotlin/com/mindos/app/features/mindboost/core/session/cognitive/evidence",
        ),
    )
}

tasks.named("check") {
    dependsOn("evidenceBoundaryScanner")
}

// Focus V5 · S1 · ADR-FOCUS-PURPOSE — the MECHANICAL half of "no code path answers eligibility from the 6-value
// SessionPurpose enum".
//
// S1 retired `SessionPurpose.mayContributeToProfile()`, which had been a second, disagreeing authority on whether
// a session may feed the profile. Deleting it makes the property true TODAY; it does not keep it true. Re-adding
// `fun SessionPurpose.mayContributeToProfile()` tomorrow compiles, is callable, and silently restores the double
// authority. A KDoc saying "do not reintroduce that shape" is prose, and prose is not a barrier (learning
// 2026-07-10: the barrier is a build scanner whose removal is a reviewable diff, not a deletable test).
//
// The rule is a POSITIVE allowlist rather than a denylist of eligibility-sounding names, because a denylist is
// fail-OPEN: `isProfileWorthy()` would sail through a list that only knows `mayContributeToProfile`. This file is
// a pure enum-vocabulary file; a function in it IS the defect's shape. The one permitted function is the total,
// name-preserving widening injection to the 11-value measurement vocabulary, which decides nothing.
val sessionPurposeAllowedFunctions = listOf("asMeasurementPurpose")

abstract class SessionPurposeDerivationScannerTask : org.gradle.api.DefaultTask() {
    @get:org.gradle.api.tasks.Internal
    abstract val sourceFile: org.gradle.api.file.RegularFileProperty

    @get:org.gradle.api.tasks.Internal
    abstract val allowedFunctions: org.gradle.api.provider.ListProperty<String>

    @org.gradle.api.tasks.TaskAction
    fun scan() {
        val file = sourceFile.get().asFile
        if (!file.isFile) {
            throw org.gradle.api.GradleException(
                "session-purpose derivation gate: expected file missing (${file.path}). If it moved, update this " +
                    "task in the SAME change — a gate pointed at nothing passes vacuously.",
            )
        }
        val allowed = allowedFunctions.get().toSet()
        // Anchored at the start of the line (after optional indentation and modifiers) so KDoc prose mentioning
        // "fun" cannot match: doc lines begin with '*'. Catches BOTH a top-level extension and a member function
        // declared inside the enum body — the two ways the retired derivation could come back.
        val declaration = Regex(
            "^\\s*(?:(?:public|internal|private|protected|inline|operator|infix|suspend|override|expect|actual)" +
                "\\s+)*fun\\s+(?:[\\w.]+\\.)?(\\w+)\\s*\\(",
        )
        val offenders = file.readLines().withIndex().mapNotNull { (index, line) ->
            declaration.find(line)?.groupValues?.get(1)
                ?.takeIf { it !in allowed }
                ?.let { "${file.name}:${index + 1}  fun $it(" }
        }
        if (offenders.isNotEmpty()) {
            throw org.gradle.api.GradleException(
                buildString {
                    appendLine("session-purpose derivation gate FAILED.")
                    appendLine()
                    appendLine("The 6-value SessionPurpose enum must not derive anything. Found:")
                    offenders.forEach { appendLine("  $it") }
                    appendLine()
                    appendLine("Eligibility is decided ONLY by ProfileMeasurementEligibility.disposition(), on the")
                    appendLine("11-value SessionMeasurementPurpose vocabulary (ADR-FOCUS-PURPOSE, 09 §20.2). If this")
                    appendLine("function genuinely decides nothing, add its name to sessionPurposeAllowedFunctions")
                    appendLine("in shared/build.gradle.kts and say why in the same diff.")
                },
            )
        }
    }
}

tasks.register<SessionPurposeDerivationScannerTask>("sessionPurposeDerivationScanner") {
    group = "verification"
    description = "Fails if the 6-value SessionPurpose enum regrows an eligibility derivation (ADR-FOCUS-PURPOSE)"
    sourceFile.set(
        layout.projectDirectory.file(
            "src/commonMain/kotlin/com/mindos/app/features/mindboost/core/session/cognitive/CognitiveSessionEnums.kt",
        ),
    )
    allowedFunctions.set(sessionPurposeAllowedFunctions)
}

tasks.named("check") {
    dependsOn("sessionPurposeDerivationScanner")
}

// SYSTEM-1 6R3 §18 C3 (consumer-sweep): the second, INVERSE half of the mechanical no-wiring barrier — instead of
// scanning the evidence package's OWN content, this scans every OTHER commonMain file for a word-boundary
// reference to one of the evidence package's public type names. Any hit outside [allowedFiles] fails the build,
// mechanically blocking a future profile/rank/cloud wiring attempt until an explicit, reviewed decision adds the
// consuming file to the allowlist (today it holds exactly one file, and only because of a pre-existing KDoc
// cross-reference — see the task registration below for why).
abstract class EvidenceConsumerSweepScannerTask : org.gradle.api.DefaultTask() {
    @get:org.gradle.api.tasks.Internal
    abstract val commonMainDir: org.gradle.api.file.DirectoryProperty

    @get:org.gradle.api.tasks.Internal
    abstract val evidencePackageDir: org.gradle.api.file.DirectoryProperty

    @get:org.gradle.api.tasks.Internal
    abstract val allowedFiles: org.gradle.api.provider.ListProperty<String>

    @org.gradle.api.tasks.TaskAction
    fun scan() {
        val root = commonMainDir.get().asFile
        val evidenceDir = evidencePackageDir.get().asFile
        if (!root.isDirectory || !evidenceDir.isDirectory) {
            throw org.gradle.api.GradleException(
                "evidence consumer-sweep gate: expected dir missing (root=${root.path}, evidence=${evidenceDir.path})",
            )
        }
        val allowlist = allowedFiles.get().map { File(root, it).canonicalFile }.toSet()
        val evidencePath = evidenceDir.canonicalFile.toPath()
        // The evidence package's own public vocabulary: the model types, the pure policies/registries that
        // produce them, and the sole extractor entry point. Kept as one literal list (not derived by reflection)
        // so a reviewer sees the exact protected surface in this diff.
        val protectedTypeNames = listOf(
            "CognitiveSessionEvidence", "ExerciseScaleEvidence", "MeasurementLineage", "SubjectScopedSessionBatch",
            "CognitiveSessionEvidenceExtractor", "EvaluationAsOf", "ClockProvenance", "ScoreScaleKey",
            "ComparabilityVerdict", "LineagePolicy", "ExclusionReason", "ExclusionCount", "StateCount",
            "RouteEligibilityCount", "PolicyVersionBundle", "SessionEvidenceAudit", "SessionEvidenceState",
            "EvidenceRouteEligibility", "SessionEvidenceDisposition", "SessionEvidenceStatePolicy", "RtCriticality",
            "RtCriticalityDeclaration", "RtCriticalityRegistry", "SessionEvidenceGates",
        )
        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.canonicalFile.toPath().startsWith(evidencePath) }
            .filterNot { it.canonicalFile in allowlist }
            .forEach { file ->
                val text = file.readText()
                protectedTypeNames.forEach { name ->
                    if (Regex("\\b" + Regex.escape(name) + "\\b").containsMatchIn(text)) {
                        throw org.gradle.api.GradleException(
                            "evidence consumer-sweep gate: '$name' referenced outside the evidence package in " +
                                "${file.path} — add to the allowlist only with an explicit, reviewed reason.",
                        )
                    }
                }
            }
    }
}

tasks.register<EvidenceConsumerSweepScannerTask>("evidenceConsumerSweepScanner") {
    group = "verification"
    description = "Fails if any commonMain file outside core.session.cognitive.evidence references one of its " +
        "public types (mechanical no-consumer barrier until an explicit wiring decision)"
    commonMainDir.set(layout.projectDirectory.dir("src/commonMain/kotlin"))
    evidencePackageDir.set(
        layout.projectDirectory.dir(
            "src/commonMain/kotlin/com/mindos/app/features/mindboost/core/session/cognitive/evidence",
        ),
    )
    // The ONLY entry today: a data-sync-dev KDoc analogy ("...cannot verify its runs truly belong to one subject,
    // exactly like [...evidence.SubjectScopedSessionBatch] cannot verify...") in a file this agent must not edit
    // (core/session/cognitive/*.kt is owned by a different wave). It is a doc cross-reference, never an import or
    // a construction — verified by reading the file. The allowlist stays otherwise empty: no wired consumer exists.
    allowedFiles.set(
        listOf(
            "com/mindos/app/features/mindboost/core/session/cognitive/SecureCognitiveSessionRunStore.kt",
        ),
    )
}

tasks.named("check") {
    dependsOn("evidenceConsumerSweepScanner")
}

// SYSTEM-1 6R3 §18 C3 (B2, kmp-architect W4 review) — the FOURTH mechanical barrier for the evidence-only
// contract. `evidenceBoundaryScanner` bans specific type TOKENS; `EvidencePublicShapeGoldenTest` pins the FIELD
// shape of the 24 types it already knows to check. Neither catches (a) an in-package top-level extension
// property/function computing a scaled ratio that avoids every banned token (e.g. an `includedPercent: Int`
// extension on an existing pinned type), or (b) a brand-new, differently-named top-level wrapper type the golden
// was never told to check. This task closes both with a POSITIVE allowlist: it extracts the NAME of every
// TOP-LEVEL declaration in the package (column-0 anchored — a line starting with whitespace is a class/object
// member by this repo's enforced ktlint/detekt formatting, never top-level) and fails if that name is not on
// [evidenceTopLevelAllowedDeclarations]. A genuinely new, legitimate top-level declaration therefore requires an
// explicit, reviewed addition to that list in the SAME change.
//
// OUT OF SCOPE (documented, not solved here): a computed MEMBER property with no backing field INSIDE an
// already-pinned class (as opposed to a top-level extension) needs member-level parsing, disproportionate to a
// plain-text regex scanner — still an open, still-honest gap; see the reopen doc for the residual-gaps ledger.
val evidenceTopLevelAllowedDeclarations = listOf(
    "CognitiveSessionEvidence", "ExerciseScaleEvidence", "ScoreScaleKey", "ComparabilityVerdict",
    "MeasurementLineage", "LineagePolicy", "SubjectScopedSessionBatch", "SessionEvidenceState",
    "EvidenceRouteEligibility", "SessionEvidenceDisposition", "SessionEvidenceStatePolicy", "ClockProvenance",
    "EvaluationAsOf", "RtCriticality", "RtCriticalityDeclaration", "RtCriticalityRegistry",
    "SessionEvidenceGates", "ExclusionReason", "ExclusionCount", "StateCount", "RouteEligibilityCount",
    "PolicyVersionBundle", "SessionEvidenceAudit", "CognitiveSessionEvidenceExtractor",
)

abstract class EvidenceTopLevelDeclarationAllowlistScannerTask : org.gradle.api.DefaultTask() {
    @get:org.gradle.api.tasks.Internal
    abstract val sourceDir: org.gradle.api.file.DirectoryProperty

    @get:org.gradle.api.tasks.Internal
    abstract val allowedDeclarations: org.gradle.api.provider.ListProperty<String>

    @org.gradle.api.tasks.TaskAction
    fun scan() {
        val dir = sourceDir.get().asFile
        if (!dir.isDirectory) {
            throw org.gradle.api.GradleException(
                "evidence top-level allowlist gate: package dir missing (${dir.path})",
            )
        }
        val allowed = allowedDeclarations.get().toSet()
        // Modifiers that may precede the declaration keyword, e.g. `internal object`, `sealed interface`,
        // `data class`, `enum class`, `value class`, `const val`. Kept as one alternation so a future modifier
        // combination (`data object`, `annotation class`) needs no new branch, only a new alternative here.
        val modifierAndKeyword = Regex(
            "^(?:(?:public|internal|private|protected|sealed|data|enum|value|annotation|abstract|open|final|" +
                "inline|external|expect|actual|suspend|operator|infix|tailrec|const|override|noinline|" +
                "crossinline)\\s+)*(class|interface|object|fun|val|var|typealias)\\b(.*)$",
        )
        fun declarationName(rawLine: String): String? {
            if (rawLine.isEmpty() || rawLine.first().isWhitespace()) return null
            val match = modifierAndKeyword.find(rawLine) ?: return null
            val keyword = match.groupValues[1]
            val rest = match.groupValues[2].trim()
            if (rest.isEmpty()) return null
            return if (keyword == "class" || keyword == "interface" || keyword == "object" || keyword == "typealias") {
                Regex("^([A-Za-z_][A-Za-z0-9_]*)").find(rest)?.groupValues?.get(1)
            } else {
                // fun/val/var: strip a leading generic type-parameter list, then take the LAST dot-segment
                // before '(' (fun) or ':'/'=' (val/var) — this is the extension-form name, e.g.
                // `fun Receiver.name(` -> "name", `val Receiver.name: Int` -> "name".
                val withoutGenerics = Regex("^<[^>]*>\\s*").replace(rest, "")
                val head = if (keyword == "fun") {
                    withoutGenerics.substringBefore("(")
                } else {
                    withoutGenerics.substringBefore(":").substringBefore("=")
                }.trim()
                Regex("^[A-Za-z_][A-Za-z0-9_]*$").find(head.substringAfterLast("."))?.value
            }
        }
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            file.readText().lineSequence().forEach { rawLine ->
                val name = declarationName(rawLine) ?: return@forEach
                if (name !in allowed) {
                    throw org.gradle.api.GradleException(
                        "evidence top-level allowlist gate: undeclared top-level declaration '$name' in " +
                            "${file.name} -- add it to allowedDeclarations only with an explicit, reviewed reason.",
                    )
                }
            }
        }
    }
}

tasks.register<EvidenceTopLevelDeclarationAllowlistScannerTask>("evidenceTopLevelDeclarationAllowlistScanner") {
    group = "verification"
    description = "Fails if a NEW top-level declaration (type, or extension property/function) appears in " +
        "core.session.cognitive.evidence that is not on the reviewed allowedDeclarations allowlist (B2)"
    sourceDir.set(
        layout.projectDirectory.dir(
            "src/commonMain/kotlin/com/mindos/app/features/mindboost/core/session/cognitive/evidence",
        ),
    )
    allowedDeclarations.set(evidenceTopLevelAllowedDeclarations)
}

tasks.named("check") {
    dependsOn("evidenceTopLevelDeclarationAllowlistScanner")
}

// ─────────────────────────────────────────────────────────────────────────────────────────────
// MB-FOCUS-0079 — ADR authority boundary.
//
// `ADR_FOCUS_TEMPORAL_QUALITY.md` is VIGENTE on its cover and half RETIRED inside: one line declares
// that everything below it is preserved R2 evidence, not live authority. Nothing distinguished a
// citation into that block used as EVIDENCE from one used as AUTHORITY, and production crossed the
// line 27 times.
//
// This is a Gradle task and NOT a test, on purpose and by measurement. The marker it checks lives in
// a KDoc COMMENT, and a comment edit leaves the bytecode identical: with the same guard written as a
// `desktopTest` case, stripping a real marker left `:shared:desktopTest UP-TO-DATE` with zero fresh
// XML — the guard inherited a green over the very drift it exists to catch. `@InputDirectory` hashes
// file CONTENT, so here the same edit re-runs the gate.
//
// DELIBERATELY NOT a ban on citing the retired block. Splitting the ADR in two was the other
// candidate and it is the wrong one, measurably: the live `R3-*` block is NOT self-contained (it
// invokes the T0/T1 taxonomy, AC#3/AC#4 and the backstop arithmetic whose only definition sits below
// the marker), so a citation into history is sometimes NECESSARY. What must never happen is that
// such a citation reads as live authority.
//
// CEILING, declared rather than papered over:
//  - Asserts a citation is ATTRIBUTED, never that the attribution is TRUE. Whether a retired clause
//    was carried forward is a per-section judgement and lives in the disposition document.
//  - Attribution is read from the citing line plus the tail of the one above it, because a KDoc
//    splits "charter" and its section sign across a line break. Further than one line, or a document
//    outside the allowlist, would be misread.
//  - The section sign is OVERLOADED inside the ADR across two numbering spaces: the reopening contract
//    (what the live R3-* headers cite) and this ADR own retired R2 sections. So the token asserts "read
//    this number against the retired R2 block", which is a CONVENTION the citation alone does not carry;
//    measured 2026-08-29, and the ADR now states that convention in a note above its first live section.
//  - Scope is `shared/src` only. Docs cite the same sections far more often; classifying those is
//    not what makes a retired clause behave like a live contract, and it is recorded as residual.
// ─────────────────────────────────────────────────────────────────────────────────────────────
abstract class AdrAuthorityBoundaryTask : org.gradle.api.DefaultTask() {
    @get:org.gradle.api.tasks.InputFile
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.RELATIVE)
    abstract val adrDocument: org.gradle.api.file.RegularFileProperty

    @get:org.gradle.api.tasks.InputDirectory
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.RELATIVE)
    abstract val sourceDir: org.gradle.api.file.DirectoryProperty

    private data class Citation(val where: String, val viaMarker: Boolean, val viaOtherDocument: Boolean) {
        val attributed: Boolean get() = viaMarker || viaOtherDocument
    }

    @org.gradle.api.tasks.TaskAction
    fun scan() {
        checkMarkerAndSectionPositions()
        val sources = sourceDir.get().asFile.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { ADR_NAME.containsMatchIn(it.readText()) }
            .toList()
        if (sources.size < MIN_CITING_FILES) {
            throw GradleException("anti-vacuity: only ${sources.size} files cite the ADR, expected >= $MIN_CITING_FILES")
        }

        val citations = sources.flatMap { citationsIn(it) { text -> text } }
        if (citations.size < MIN_CITATIONS) {
            throw GradleException("anti-vacuity: only ${citations.size} plain-section citations found, expected >= $MIN_CITATIONS")
        }
        val unattributed = citations.filterNot { it.attributed }
        if (unattributed.isNotEmpty()) {
            throw GradleException(
                "Unattributed citation into the RETIRED block of the ADR (add the evidence token, or " +
                    "cite the live R3 block): ${unattributed.map { it.where }}",
            )
        }

        // Discriminant half, run by the gate itself: without it "no unattributed citations" could mean
        // the classifier never looks. Strip the token from the same corpus IN MEMORY and the SAME
        // classifier must report exactly the sites that carried it - no fewer.
        val marked = citations.count { it.viaMarker }
        val stripped = sources
            .flatMap { file -> citationsIn(file) { text -> text.replace(EVIDENCE_MARKER, "") } }
            .count { !it.attributed }
        if (stripped != marked) {
            throw GradleException("self-check failed: stripping the token flagged $stripped sites, expected $marked")
        }
    }

    private fun checkMarkerAndSectionPositions() {
        val lines = adrDocument.get().asFile.readLines()
        val markers = lines.withIndex().filter { (_, l) -> l.startsWith(MARKER_PREFIX) }
        if (markers.size != 1) {
            throw GradleException("expected exactly one retirement marker in the ADR, found ${markers.size}")
        }
        if (!markers.single().value.contains("SUPERSEDED")) {
            throw GradleException("the retirement marker must declare supersession: '${markers.single().value}'")
        }
        val marker = markers.single().index
        val plain = lines.withIndex().filter { (_, l) -> PLAIN_HEADER.containsMatchIn(l) }.map { it.index }
        val live = lines.withIndex().filter { (_, l) -> R3_HEADER.containsMatchIn(l) }.map { it.index }
        // Anti-vacuity: a header pattern that stopped matching would make both "all below" and "all
        // above" vacuously true, and this gate would go green over a document it never parsed.
        if (plain.size < MIN_PLAIN_SECTIONS) {
            throw GradleException("anti-vacuity: only ${plain.size} plain sections parsed, expected >= $MIN_PLAIN_SECTIONS")
        }
        if (live.size < MIN_LIVE_SECTIONS) {
            throw GradleException("anti-vacuity: only ${live.size} R3 sections parsed, expected >= $MIN_LIVE_SECTIONS")
        }
        val plainAbove = plain.filter { it < marker }.map { it + 1 }
        val liveBelow = live.filter { it > marker }.map { it + 1 }
        if (plainAbove.isNotEmpty()) {
            throw GradleException("plainly numbered sections inside the LIVE block, at ADR lines $plainAbove")
        }
        if (liveBelow.isNotEmpty()) {
            throw GradleException("R3 sections below the retirement marker, at ADR lines $liveBelow")
        }
    }

    private fun citationsIn(file: File, transform: (String) -> String): List<Citation> {
        val lines = transform(file.readText()).split("\n").map { it.trimEnd('\r') }
        val found = mutableListOf<Citation>()
        for ((index, line) in lines.withIndex()) {
            val match = CITATION_GROUP.find(line) ?: continue
            val previous = lines.getOrNull(index - 1).orEmpty().replace(KDOC_LEAD, "")
            val before = (previous + " " + line.take(match.range.first)).takeLast(ATTRIBUTION_WINDOW)
            found += Citation(
                where = "${file.name}:${index + 1}",
                viaMarker = line.contains(EVIDENCE_MARKER),
                viaOtherDocument = OTHER_DOCUMENTS.any { it in before } && !ADR_NAME.containsMatchIn(before),
            )
        }
        return found
    }

    private companion object {
        const val MARKER_PREFIX = "# HISTORIA"
        const val EVIDENCE_MARKER = "[R2-HIST]"
        const val ATTRIBUTION_WINDOW = 80

        // Measured on the live tree before the rule was written: 21 plain sections, 23 R3 sections,
        // 27 citations across 19 files. Floors sit just under the measurement so that losing a chunk
        // of the corpus fails loudly instead of passing over a smaller one.
        const val MIN_PLAIN_SECTIONS = 20
        const val MIN_LIVE_SECTIONS = 20
        const val MIN_CITATIONS = 27
        const val MIN_CITING_FILES = 19

        val ADR_NAME = Regex("ADR[_-]FOCUS[_-]TEMPORAL[_-]QUALITY")

        // Top-level sections are written "## 4." and subsections "### 4.1" WITHOUT a trailing dot, so
        // a pattern that demands one silently skips all twelve subsections - including 4.1, whose
        // taxonomy the live block consumes without ever redefining it.
        val PLAIN_HEADER = Regex("^#{1,4} [0-9]+([.][0-9]+)*[.]? ")
        val R3_HEADER = Regex("^#{1,4} R3-[0-9]+[.]? ")

        // The section sign is written as an escape so this build script holds none literally.
        const val SECTION_SIGN = "\u00A7"
        val CITATION_GROUP =
            Regex(SECTION_SIGN + "[0-9]+([.][0-9]+)*(/" + SECTION_SIGN + "[0-9]+([.][0-9]+)*)*")
        val KDOC_LEAD = Regex("^\\s*[*]\\s?")

        // Documents other than this ADR that legitimately use the section sign in the same neighbourhood.
        val OTHER_DOCUMENTS = listOf("charter")
    }
}

tasks.register<AdrAuthorityBoundaryTask>("adrAuthorityBoundaryScanner") {
    group = "verification"
    description = "Fails if a citation into the RETIRED block of ADR-FOCUS-TEMPORAL-QUALITY is unattributed"
    adrDocument.set(rootProject.layout.projectDirectory.file("docs/engines/focus_v5/ADR_FOCUS_TEMPORAL_QUALITY.md"))
    sourceDir.set(layout.projectDirectory.dir("src"))
}

tasks.named("check") {
    dependsOn("adrAuthorityBoundaryScanner")
}

// ─────────────────────────────────────────────────────────────────────────────────────────────
// FOC-P0-05 · focusQualityGateScanner — closes root cause CR-6 (no single, unavoidable quality gate).
//
// The ONE path through which a Focus session becomes a RECORDED one is `FocusSessionAssembler.admit`,
// which consults `SessionQualityGate` and stamps its verdict on the session. This gate keeps that path
// single at BUILD time, over the whole `focus/` production source set, by content (`@InputDirectory`
// hashes the text, so editing a KDoc or a call site re-runs it — a test would stay UP-TO-DATE, see the
// 2026-08-28 learning). Rules, each one failing loudly:
//  1. The assembler itself consults the gate (anti-vacuity: without this, "every recorder is gated"
//     could mean the gate was deleted).
//  2. Every file that assembles a session (`assembleSession(`) also passes it through `.admit(`, EXCEPT
//     the two probe lanes pinned below, which have no lifecycle probe yet (ledger finding, owner P1).
//     The allowlist is checked for EXACT equality: a new ungated caller fails, and gating a pinned lane
//     is a deliberate edit of this list, not a silent drift.
//  3. Every file that appends to the session history is gated (a session must not be persisted un-admitted).
//  4. `Admitted` is minted ONLY by the gate: no production file may assign it into a session directly.
// Declared ceiling: this is a textual gate on the production source set of `focus/`. It does not see
// `focus2/` (separate lane, P1 promotion) nor tests, and it cannot tell whether the admitted session is
// the one the caller then reads — that half is pinned by `FocusQualityGateWiringTest` at runtime.
// ─────────────────────────────────────────────────────────────────────────────────────────────
abstract class FocusQualityGateScannerTask : org.gradle.api.DefaultTask() {
    @get:org.gradle.api.tasks.InputDirectory
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.RELATIVE)
    abstract val sourceDir: org.gradle.api.file.DirectoryProperty

    @org.gradle.api.tasks.TaskAction
    fun scan() {
        val files = sourceDir.get().asFile.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        val texts = files.associateWith { it.readText() }
        val assembler = files.singleOrNull { it.name == ASSEMBLER }
            ?: throw GradleException("anti-vacuity: $ASSEMBLER not found under ${sourceDir.get().asFile}")
        if (!texts.getValue(assembler).contains(GATE_CALL)) {
            throw GradleException("$ASSEMBLER no longer consults the gate ($GATE_CALL): the admission path is broken")
        }

        val recorders = files.filter { it != assembler && texts.getValue(it).contains(ASSEMBLE_SESSION) }
        val gated = recorders.filter { texts.getValue(it).contains(ADMIT_CALL) }.map { it.name }
        val ungated = recorders.filterNot { texts.getValue(it).contains(ADMIT_CALL) }.map { it.name }.toSet()
        if (gated.isEmpty()) {
            throw GradleException("anti-vacuity: no file passes a Focus session through $ADMIT_CALL")
        }
        val violations = ungated - UNGATED_PROBE_LANES
        if (violations.isNotEmpty()) {
            throw GradleException(
                "A file assembles a Focus session without passing it through FocusSessionAssembler.admit: " +
                    "$violations. Every recorded session carries the gate's verdict (FOC-P0-05).",
            )
        }
        if (ungated != UNGATED_PROBE_LANES) {
            throw GradleException(
                "The ungated probe lanes pinned by FOC-P0-05 changed: expected $UNGATED_PROBE_LANES, found " +
                    "$ungated. Gating a lane (or losing one) is a deliberate edit of this allowlist + ledger row.",
            )
        }

        val ungatedAppenders = files
            .filter { texts.getValue(it).contains(HISTORY_APPEND) && !texts.getValue(it).contains(ADMIT_CALL) }
            .map { it.name }
        if (ungatedAppenders.isNotEmpty()) {
            throw GradleException(
                "A file persists a Focus session to history without admitting it: $ungatedAppenders",
            )
        }

        val minting = files.filter { it.name != GATE_FILE && MINT_ADMITTED.containsMatchIn(texts.getValue(it)) }
        if (minting.isNotEmpty()) {
            throw GradleException(
                "Only $GATE_FILE may mint SessionAdmission.Admitted; found it in ${minting.map { it.name }}",
            )
        }
    }

    private companion object {
        const val ASSEMBLER = "FocusSessionAssembler.kt"
        const val GATE_FILE = "SessionQualityGate.kt"
        const val GATE_CALL = "SessionQualityGate.admit("
        const val ASSEMBLE_SESSION = "assembleSession("
        const val ADMIT_CALL = ".admit("
        const val HISTORY_APPEND = "historyRepository.append("

        // A single `=` (not `==`): an assignment or named argument that mints the verdict, never a comparison.
        val MINT_ADMITTED = Regex("admission\\s*=(?!=)\\s*SessionAdmission\\.Admitted")

        // EMPTY on 2026-09-03: `MB-FOCUS-0114` wired the lifecycle probe and `admit` into both RCI probe
        // lanes (`FocusBaselineStateHolder`, `FocusEvaluationStateHolder`), so no Focus lane assembles a
        // session outside the gate any more. Removing those two names from this set IS that fix landing.
        //
        // Kept as an EMPTY set rather than deleted, because the equality check below is what makes the pin
        // work in both directions: an empty expectation turns "a new lane skips the gate" into a build
        // failure, and adding a name back has to be a deliberate edit here plus a ledger row.
        //
        // The reason originally written here for exempting the two lanes — "their RCI is already fail-closed
        // through `ReliabilitySource.EMPTY`" — was FALSE, and false in a way this ledger had already recorded
        // once (`MB-CATALOG-0021`): EMPTY stops a transfer line from being STRONG, not from being LISTED.
        val UNGATED_PROBE_LANES = emptySet<String>()
    }
}

tasks.register<FocusQualityGateScannerTask>("focusQualityGateScanner") {
    group = "verification"
    description = "Fails if a Focus session can be recorded without passing through FocusSessionAssembler.admit"
    sourceDir.set(layout.projectDirectory.dir("src/commonMain/kotlin/com/mindos/app/features/mindboost/focus"))
}

tasks.named("check") {
    dependsOn("focusQualityGateScanner")
}
