# MindOS — Contexto REAL del repo (fuente de verdad para /ceo)

Este archivo existe para que `/ceo` genere prompts **aterrizados en el repo real**, no en una app
KMP de plantilla. Si algo aquí contradice a un doc genérico, gana esto. Verifica con `Grep`/`Glob`
si dudas; no inventes.

## Módulos Gradle (de `settings.gradle.kts`)

```
:shared        → módulo KMP nuclear. Source sets:
                 commonMain, androidMain, desktopMain, iosMain
                 tests: commonTest (KMP) y desktopTest (JVM)
:androidApp    → app Android (Compose). applicationId release: com.neu.app
:desktopApp    → app JVM de escritorio (host de tests/preview de la lógica KMP)
mindos/        → build COMPUESTO (includeBuild). Módulos mind-* :
                 mind-command-runtime, mind-action-matching, mind-kmp-core,
                 mind-kmp-contracts, mind-android-voice, mind-speech, ...
functions/     → Firebase Cloud Functions (TypeScript, Node) — backend
```

- **NO existe `composeApp`.** **NO existe un módulo Gradle `:iosApp`.**
- iOS es el proyecto **Xcode `iosApp/`** que consume el framework KMP. En Windows la compilación
  iOS está **diferida** (sin toolchain macOS): el prompt no debe pedir builds iOS locales.

## Paquete y estructura de features

- Paquete raíz: **`com.mindos.app`** (histórico: algunos archivos viejos usan `com.example.mindos`
  o `com.mindverse.mindboost` en el core precompilado; los nuevos van a `com.mindos.app`).
- Features MindBoost:
  `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/**`
  - `focus/` (Enfoque, E1–E13), `speed/` (Velocidad Mental), `memory/` (Memoria),
    `logic/` (Lógica), `language/` (Lenguaje), `decisiones/`, `creativity/`, y el emocional.
  - `core/` transversal: `scoring/` (ScoreStateMachine, MinSampleGate, ScoreResultFactory),
    `antigaming/`, `bank/`, `timing/`, `copy/` (lexicon + scanner).
  - `profile/` — brain-profile agregado (perfil cognitivo global).
  - `presentation/ui/MindBoostHubScreen.kt` — hub que enruta a cada track.
- Otras features de la app (no MindBoost): `features/social/`, `features/dm/`, `features/profile/`
  (app profile card), `features/home/`, `features/arena/` (Bayesian Arena offline), etc.
- DI raíz: `shared/src/commonMain/kotlin/com/mindos/app/core/di/AppGraph.kt` (data class @Stable;
  `rememberAppGraph()` construye el wiring de producción).

## Tareas Gradle REALES (gates)

Usa estos nombres exactos en la sección de verificación del prompt:

| Propósito | Tarea |
|---|---|
| Tests de la lógica (JVM) | `:shared:desktopTest` |
| Compilar target desktop | `:shared:compileKotlinDesktop` |
| Compilar target Android de :shared | `:shared:compileAndroidMain` |
| Static analysis (ratchet one-way) | `:shared:detekt` |
| App Android end-to-end | `:androidApp:compileDebugKotlin` · `:androidApp:assembleDebug` |
| Cobertura (KMP → usar variante Desktop) | `:shared:koverVerifyDesktop` · `:shared:koverHtmlReportDesktop` |
| Backend TS | (en `functions/`) `npm run typecheck`, `npm test` (jest), `npm run scan:secrets` |

Notas:
- El plugin nuevo de `:shared` (`com.android.kotlin.multiplatform.library`, AGP 9) **no** genera
  `compileDebugKotlinAndroid`; para Android usa `:shared:compileAndroidMain` o
  `:androidApp:compileDebugKotlin`.
- Si dudas de un nombre de tarea: `:shared:tasks --all`.
- Hay scanners de build específicos: `:shared:arenaTerminologyScanner` y el copy-safety scanner de
  MindBoost (`mindboostCopySafetyScanner`) — el prompt no debe introducir léxico prohibido
  (clínico/IQ) ni siquiera en comentarios o disclaimers negados.

## Reglas de entorno (críticas para un prompt que funcione)

1. **Daemon del IDE:** si Android Studio posee el daemon Gradle en el worktree, el prompt debe
   **verificar estáticamente y entregar los comandos de gate al usuario**, no invocar Gradle CLI.
2. **Nada de clean destructivo** (`gradlew --stop`/`clean`/`--no-build-cache`) salvo petición
   explícita: mata daemons del IDE. Ante hiccup, re-ejecutar la tarea.
3. **PowerShell primero** (Windows). Citar props Gradle con punto: `"-Pkotlin.incremental=false"`.
   Scripts `.ps1` en ASCII o UTF-8 con BOM.
4. **Fases pequeñas** + gate tras cada una (`desktopTest` + `detekt` + compilación).
5. **Worktrees:** nunca trabajar sobre `main`/`master` (Vibe Kanban = un worktree por tarea).
6. **Sin commit/PR:** reportar cambios del working-tree; no ofrecer ni pedir commit/push/PR.
7. **Detekt ratchet one-way:** nunca regenerar `detekt-baseline.xml` para silenciar; arreglar o
   `@Suppress` localizado y justificado.
8. **Bucle de aprendizaje:** tras corregir un error real, añadir entrada en "Aprendizajes
   acumulados del proyecto" de `CLAUDE.md` (formato exacto en ese archivo).

## Persistencia y datos

- **Datos cognitivos sensibles:** `@Serializable` (con `schemaVersion`) + **`SecureKeyValueStore`**
  cifrado (Android: AES-256-GCM + Keystore; desktop/iOS: in-memory / diferido). **No Room, no
  SQLDelight** para estos datos. Solo se persisten **resúmenes** de sesión, nunca trials crudos.
  Repos `Local*` en commonMain, testeables con `InMemorySecureKeyValueStore`.
- **Namespaces por-usuario** (`MindBoostStorageNamespaces.AccountBound`): al añadir uno, actualizar
  el eraser de borrado de cuenta (`AppGraph`) + su test de cobertura, y el borrado en
  `functions/src/accountDeletion.ts` (GDPR Art. 17).
- **Firestore/Storage:** toda `allow list` exige `.limit()` en cliente acorde a `firestore.rules`;
  App Check en escrituras/lecturas privadas; timeouts en toda operación Firebase
  (`FirestoreDefaults`/`AuthDefaults`/`StorageDefaults`/`FunctionsDefaults`).

## Rigor charter (honestidad — no negociable)

- **`null ≠ 0`.** Dato insuficiente/no válido ⇒ `CALIBRATING` / "—", nunca 0 fabricado.
- El perfil se actualiza **solo** vía un `ScoreResult` sellado con sus gates: muestra mínima
  (`MinSampleGate`), timebase autoritativo (RT-crítico ⇒ Choreographer + nanos nativos),
  integridad de banco (`BankValidator`), anti-gaming con red-team.
- **Anti-gaming:** un detector cuenta solo si un adversario sintético lo dispara Y un usuario
  honesto realista NO lo dispara.
- **Sin claims** médicos/clínicos/diagnósticos ni promesas de IQ; sin transferencia lejana no
  respaldada. Lenguaje permitido: "perfil cognitivo funcional", "práctica", "progreso vs tu propia
  línea base", "dificultad adaptativa".

## Especialistas del repo a los que el prompt puede enrutar

Subagentes MindBoost (vía la tool Agent/Task) — ver `.claude/agents/` y el orquestador `/mb`:

- `efficacy-guardian` — validez científica, transferencia, anti-falso-progreso (gate de eficacia).
- `psychometrician` — scoring, métricas RT, dificultad adaptativa, fiabilidad.
- `kmp-architect` — estructura de módulos, expect/actual, fronteras, Gradle KMP.
- `cognitive-engine-dev` — motor cognitivo puro en commonMain (state machines, scoring).
- `compose-ui-dev` — pantallas Compose, design system, hub.
- `motion-experience` — animación/feel (solo bajo petición explícita de pulido).
- `data-sync-dev` — Firestore, persistencia offline, perfil cognitivo.
- `privacy-security` — datos sensibles, secretos, permisos.
- `test-engineer` — tests (verificación de la matemática del scoring con input→output conocido).
- `diff-auditor` — revisión final del diff en contexto fresco (gate de salida).

Reviewers de stack (ECC): `kotlin-reviewer`, `kotlin-build-resolver`, `swift-reviewer`,
`typescript-reviewer`, `security-reviewer`, `silent-failure-hunter`, `architect`, `planner`.

Skills de stack (auto-activadas por contexto): `compose-multiplatform-patterns`,
`kotlin-coroutines-flows`, `kotlin-patterns`, `kotlin-testing`, `android-clean-architecture`,
`create-cognitive-exercise`, `design-adaptive-scoring`, `review-cognitive-claims`,
`qa-release-gate`, `privacy-safety-review`, `production-audit`, `verification-loop`.

Comandos útiles: `/plan`, `/kotlin-build`, `/gradle-build`, `/kotlin-review`, `/kotlin-test`,
`/quality-gate`, `/security-scan`, `/mb <objetivo>` (orquestador MindBoost).
