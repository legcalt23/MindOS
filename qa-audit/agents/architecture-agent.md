# architecture-agent

## AGENT_RESULT

agent_id: `architecture-agent`
scope: `Estatico: settings.gradle.kts, shared/build.gradle.kts, gradle/libs.versions.toml + contexto CLAUDE.md`
status: `partial` (estatico; build NO ejecutado por presupuesto ~14%)
confidence: `78`
files_examined_count: `3` (+ contexto arquitectonico de CLAUDE.md)
commands_run_count: `0` (sin Gradle)
evidence_files:
- `settings.gradle.kts`
- `shared/build.gradle.kts`
- `gradle/libs.versions.toml`

### Hechos confirmados

- Toolchain: Kotlin **2.3.10**, AGP **9.1.0**, Compose Multiplatform **1.10.3**, JVM target **17**.
- Módulos raíz: `:shared`, `:androidApp`, `:desktopApp` (TYPESAFE_PROJECT_ACCESSORS). Build secundario aparte en `mindos/` (+ `mindos/build-logic`).
- `:shared` targets: `android` (namespace `com.mindos.app.shared`, compileSdk 36, minSdk 29), `jvm("desktop")`, e **iOS** (`iosArm64` + `iosSimulatorArm64`, framework estático "Shared") **solo si `os.name == Mac OS X`**. CocoaPods aplicado solo en macOS.
- Source sets: `commonMain`, `commonTest`, `desktopTest`, `androidMain` (iosMain existe; targets iOS no se configuran en Windows).
- Backend: **dev.gitlive firebase 2.4.0** (multiplataforma) en commonMain: app(api)/auth/firestore/functions/storage. Android suma Firebase BOM 33.15.0 (catálogo tiene appcheck/crashlytics/analytics/perf/config).
- Serialización: `serialization-core` + `serialization-json` 1.9.0. `kotlinx-datetime` **0.6.0**. Coroutines 1.10.2.
- AndroidMain: CameraX 1.3.3, **ML Kit face-detection 16.1.6**, Coil 2.7.0, Media3 1.5.1, activity-compose.
- Detekt: ratchet con baseline (**2961** findings históricos tolerados); escanea common/android/desktop/ios main + commonTest. `ignoreFailures=false`.

### findings

- id: `ARCH-001`
  title: `kotlinx-datetime 0.6.0 pineado pero 0.7.1 entra transitivo (Clock.System removido en runtime)`
  severity: `P2`
  category: `build`
  target: `shared`
  reproducibility: `not_reproduced` (documentado en CLAUDE.md; requiere desktopTest para confirmar hoy)
  user_impact: `ninguno directo; rompe cobertura de tests y puede romper código que use Clock.System`
  technical_impact: `NoClassDefFoundError kotlinx/datetime/Clock$System en JVM test si se usa Clock.System.now() directo`
  suspected_root_cause: `Compose MP/Kotlin 2.3 traen datetime 0.7.x transitivo que movió Clock a kotlin.time`
  recommended_next_action: `Regla detekt/lint que prohíba Clock.System.now() fuera de SystemEpochClock/AppClock; verificar con :shared:desktopTest`

- id: `ARCH-002`
  title: `ML Kit face-detection + CameraX en androidMain = captura biométrica sensible`
  severity: `P2`
  category: `security`
  target: `android`
  reproducibility: `not_reproduced`
  user_impact: `datos faciales son biométricos; riesgo de privacidad si se persisten/transmiten`
  technical_impact: `dependencia presente; falta verificar que el procesamiento es 100% on-device y no se persiste`
  suspected_root_cause: `feature de cámara/rostro (perfil/cognitivo) sin auditar el ciclo de vida del dato`
  recommended_next_action: `HAND-OFF a security-agent/privacy-review: confirmar on-device only, sin persistencia/upload, con consentimiento`

- id: `ARCH-003`
  title: `iOS solo compilable en macOS — actuals de iosMain no verificables en Windows`
  severity: `P3`
  category: `platform-specific`
  target: `ios`
  reproducibility: `always` (estructural)
  user_impact: `riesgo de build iOS roto descubierto tarde`
  technical_impact: `expect sin actual en iosMain no se detecta sin target iOS (ya ocurrió, ver CLAUDE.md)`
  recommended_next_action: `Job CI en macOS que compile iosArm64/iosSimulatorArm64; checklist expect/actual al añadir expect`

- id: `ARCH-004`
  title: `materialIconsExtended 1.7.3 desalineado del resto de Compose (1.10.3)`
  severity: `P4`
  category: `build`
  target: `common`
  reproducibility: `always`
  technical_impact: `version skew; JetBrains dejó de publicar; migración a Material Symbols pendiente`
  recommended_next_action: `tarea de migración a vector resources; bajo riesgo`

summary_for_brain: |
  KMP real confirmado (Kotlin 2.3.10/AGP 9.1.0/Compose 1.10.3). 3 módulos raíz + build mindos/ aparte.
  iOS solo en macOS. Backend gitlive Firebase multiplataforma. 2 riesgos P2: (1) datetime 0.6.0 vs
  0.7.1 transitivo (build/tests), (2) ML Kit face-detection = biométrico, requiere privacy review.
  Build NO ejecutado (presupuesto). Confirmar ARCH-001 con :shared:desktopTest cuando haya budget.
