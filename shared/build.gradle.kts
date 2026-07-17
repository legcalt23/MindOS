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
