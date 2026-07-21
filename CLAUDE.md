# CLAUDE.md — Gobernanza de MindOS (operador: Claude Code)

> **Audiencia y relación entre archivos:** este archivo gobierna a **Claude Code**, el operador
> principal del repo, y es la **fuente de verdad** de doctrina de producto, rigor charter, mapa
> real del repo, reglas de entorno, persistencia y protocolo del bucle de aprendizaje.
> [`AGENTS.md`](AGENTS.md) (agentes no-Claude: Vibe Kanban, codex CLI, n8n) **defiere a este
> archivo** y solo añade sus reglas específicas de agente y su propio ledger de aprendizajes.

> **Flujo con agentes / Vibe Kanban:** antes de operar, lee
> [`docs/AI_WORKFLOW.md`](docs/AI_WORKFLOW.md) (reglas de edición, ramas, commits, build/test,
> checklist pre-merge) y [`docs/VIBE_KANBAN_SETUP.md`](docs/VIBE_KANBAN_SETUP.md) (entorno local).
> Ningún agente trabaja sobre `main`/`master`: un worktree por tarea.

> **⛔ LEDGER DE AUDITORÍA OBLIGATORIO (alta prioridad):** el registro canónico, acumulativo y
> append-only de hallazgos/invariantes de MindBoost vive en
> [`MINDBOOST_AUDIT_LEDGER.md`](MINDBOOST_AUDIT_LEDGER.md) (raíz del repo). **Léelo al empezar
> CUALQUIER tarea y actualízalo ANTES de terminarla** (por pequeña que sea: fixes, refactors, docs,
> tests, reviews read-only, investigaciones, diagnósticos, migraciones, cambios de gobernanza, tareas
> sin código, bloqueadas/interrumpidas, y trabajo de subagentes). Ninguna tarea es
> `DONE`/`COMPLETE`/`APTO`/`CLOSED`/`VERIFIED`/congelada hasta: leer el ledger → reconciliar el delta →
> añadir hallazgos nuevos → actualizar los afectados → registrar evidencia → añadir checkpoint →
> verificar que ningún hallazgo previo desapareció → recomputar el SHA-256. Reglas: no borrar filas;
> no reutilizar IDs; `MITIGATED`/`BLOCKED` ≠ `CLOSED`; `SUPERSEDED` apunta al ID nuevo; cerrar exige
> evidencia + gate (no narrativa); **single writer** = la sesión principal (los subagentes reportan,
> no editan). Validador: `node scripts/validate-audit-ledger.mjs`. La obligación está persistida en el
> grafo de memoria (`feedback_mindboost-audit-ledger-protocol`). Sin datos personales ni secretos en
> el ledger.

## 1. Identidad y doctrina de producto

**Mindverse** es una plataforma AI-native de inteligencia humana, productividad, aprendizaje,
bienestar y creación personal. **MindBoost** es su entrenador cognitivo (Kotlin Multiplatform +
Compose Multiplatform; Android + iOS). No es un juego casual: es un sistema **medible** de
progreso cognitivo cuyo bucle nuclear es `baseline → plan diario → sesión adaptativa → feedback
inmediato → progresión longitudinal (vs tu propia línea base) → insight → hábito`. El producto
debe ser escalable, creíble, premium y privacy-first; cada decisión debe mejorar confianza, valor
medible, escalabilidad, mantenibilidad o ventaja estratégica.

**Límites de producto (no negociables):**

- MindBoost NO es un producto de salud ni un instrumento de evaluación de salud: no emite
  valoraciones sobre ninguna condición de salud, no promete mejoras garantizadas de capacidad
  general y no usa neurociencia vaga ni claims de transferencia lejana no respaldada.
- El léxico prohibido canónico vive en
  `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/copy/ForbiddenLexicon.kt`
  y lo imponen los scanners de build (§3). **No nombrar sus lexemas ni para negarlos** — tampoco
  en comentarios, tests o docs dentro del alcance escaneado.
- Lenguaje permitido: "perfil cognitivo funcional", "práctica", "entrenamiento", "progreso frente
  a tu propia línea base", "dificultad adaptativa", "orientativo".

**Dominios cognitivos canónicos** (no cambiar sin decisión explícita de producto): attention,
working_memory, processing_speed, logical_reasoning, verbal_reasoning, spatial_reasoning,
creativity, metacognition, decision_making, emotional_regulation_for_performance,
learning_strategy.

**Conceptos canónicos** (preferirlos o mapear a los equivalentes ya existentes del repo): User,
CognitiveProfile, CognitiveDomain, Assessment, AssessmentResult, TrainingPlan, TrainingSession,
Exercise, ExerciseAttempt, Score, DifficultyState, Recommendation, CoachMessage, AnalyticsEvent,
Reflection, ProgressSnapshot.

**Gates de calidad de una feature MindBoost** — no está lista sin: (1) objetivo cognitivo claro,
(2) mecánica de entrenamiento, (3) método de scoring, (4) adaptación de dificultad o razón
explícita de dificultad estática, (5) feedback al usuario, (6) eventos de analítica, (7) revisión
de seguridad de claims, (8) tests de la lógica importante, (9) revisión de privacidad de datos
cognitivos/conductuales, (10) ruta de verificación.

**Estándares de ingeniería:** inspeccionar la arquitectura existente antes de editar; preservar
convenciones y reutilizar componentes del repo; diffs pequeños y revisables; tests cuando cambia
el comportamiento; validar inputs y proteger datos de usuario; minimizar la recolección de datos;
separar UI / lógica de negocio / scoring / persistencia / analítica; explicar dependencias antes
de añadirlas. No cambiar en silencio claims de producto, semántica de scoring ni alcance de
recolección de datos sin explicar por qué.

**Cierre de toda implementación:** qué cambió · archivos afectados · tests añadidos/actualizados ·
cómo verificar · riesgos y follow-up.

**Subagentes:** usa los especialistas de `.claude/agents/` cuando la tarea encaje; delega reviews
especializadas para mantener limpio el contexto principal. Flujo preferido para features MindBoost:
`mindboost-chief-architect` → `mindboost-cognitive-science` (mecanismo y claims) →
`mindboost-product-ux` → `mindboost-assessment-diagnostics` (baseline/scoring) →
`mindboost-personalization-engine` → `mindboost-backend` / `mindboost-frontend` →
`mindboost-data-analytics` → `mindboost-security-privacy` → `mindboost-qa-safety` (veredicto
final). Máximo 2–3 agentes concurrentes; si una lente devuelve output vacío, reintenta solo esa.

## 2. Rigor charter — honestidad de medición (no negociable)

- **`null ≠ 0`.** Dato insuficiente o no válido ⇒ `CALIBRATING` / "—", **nunca** un 0 fabricado
  ni un valor bajo.
- El perfil cognitivo se actualiza **solo** vía un `ScoreResult` sellado que pasó TODOS sus gates:
  muestra mínima (`MinSampleGate`), timebase autoritativo para señal RT-crítica
  (`CHOREOGRAPHER_FRAME` + `NANOS_NATIVE`; con fallback la sesión es jugable pero queda excluida
  del perfil), integridad de banco derivada de `BankIntegrityAuthority` (nunca un literal
  `BankIntegrity.*` en el call-site; registro opt-in, fail-closed a `SMALL_UNVALIDATED`), y
  anti-gaming con cobertura declarada por el propio detector (`DetectorCoverage`).
- **Anti-gaming:** un detector cuenta solo si (a) tiene call-site vivo en el gate que dice
  proteger, (b) un adversario sintético lo dispara y (c) un usuario honesto realista NO lo dispara
  (suite red-team). Dirección fail-closed: retener la actualización del perfil, nunca acusar ni
  castigar por señal no inferible.
- `dataSufficiency` (tamaño de muestra) y `confidence` (estabilidad) son ortogonales; nunca mapear
  uno desde el otro. Todo ProfileSource se verifica end-to-end a través de
  `MindBoostProfileAggregator`, no solo su contribución aislada.
- UI honesta: los placeholders no se renderizan como hechos (nada de presencia, contadores o
  estados fabricados sin fuente de datos real).

## 3. Mapa REAL del repo (no es una plantilla KMP)

Módulos (de `settings.gradle.kts`):

```
:shared      → módulo KMP nuclear. Source sets: commonMain, androidMain, desktopMain, iosMain.
               Tests: commonTest (KMP) y desktopTest (JVM — aquí corre la lógica).
:androidApp  → app Android (Compose). applicationId release: com.neu.app.
:desktopApp  → app JVM de escritorio (host de preview/tests de la lógica KMP).
mindos/      → build COMPUESTO (includeBuild). Módulos mind-*: mind-command-runtime,
               mind-action-matching, mind-android-voice, mind-speech, …
functions/   → Firebase Cloud Functions (TypeScript/Node) — backend.
```

- **NO existe `composeApp` ni un módulo Gradle `:iosApp`.** iOS es el proyecto Xcode `iosApp/`
  que consume el framework KMP; en Windows la compilación iOS está **diferida** (sin macOS).
- Paquete raíz: **`com.mindos.app`** (legacy en archivos viejos: `com.example.mindos`, y
  `com.mindverse.mindboost` en el core precompilado; lo nuevo va a `com.mindos.app`).
- Features MindBoost: `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/**` —
  `focus/`, `speed/`, `memory/`, `logic/`, `language/`, `decisiones/`, `creativity/`,
  `emotions/`; transversal `core/` (`scoring/`, `antigaming/`, `bank/`, `timing/`, `copy/`);
  `profile/` (brain-profile agregado); hub en `presentation/ui/MindBoostHubScreen.kt`.
- Otras features: `features/social/`, `features/dm/`, `features/profile/` (app profile card),
  `features/home/`, `features/arena/` (Bayesian Arena offline). DI raíz:
  `shared/src/commonMain/kotlin/com/mindos/app/core/di/AppGraph.kt` (`rememberAppGraph()`).

Tareas Gradle REALES (gates):

| Propósito | Tarea |
|---|---|
| Tests de la lógica (JVM) | `:shared:desktopTest` |
| Compilar target desktop | `:shared:compileKotlinDesktop` |
| Compilar target Android de `:shared` | `:shared:compileAndroidMain` |
| Static analysis (ratchet one-way) | `:shared:detekt` |
| App Android end-to-end | `:androidApp:compileDebugKotlin` · `:androidApp:assembleDebug` |
| Cobertura (KMP ⇒ variante Desktop) | `:shared:koverVerifyDesktop` · `:shared:koverHtmlReportDesktop` |
| Copy-safety MindBoost | `:shared:mindboostCopySafetyScanner` (+ `…CommonTest` / `…DesktopTest`) |
| Terminología Arena | `:shared:arenaTerminologyScanner` (+ `…CommonTest` / `…DesktopTest`) |
| Backend TS (en `functions/`) | `npm run typecheck` · `npm test` · `npm run scan:secrets` |

- El plugin de `:shared` (`com.android.kotlin.multiplatform.library`, AGP 9) **no** genera
  `compileDebugKotlinAndroid`; para Android usa `:shared:compileAndroidMain` o
  `:androidApp:compileDebugKotlin`. Si dudas de un nombre de tarea: `:shared:tasks --all`.
- Los scanners de copy/terminología leen archivos COMPLETOS (comentarios incluidos), paran en el
  primer hit y matchean palabra completa a través de guiones; corre siempre la variante del
  source-set que tocaste y re-corre hasta verde.

## 4. Reglas de entorno (Windows / PowerShell / IDE)

1. **Daemon del IDE:** si Android Studio posee el daemon Gradle de este worktree, NO invoques
   Gradle CLI: verifica estáticamente y entrega los comandos de gate al usuario.
2. **Nada de clean destructivo** (`--stop` / `clean` / `--no-build-cache`) salvo petición
   explícita; ante un hiccup, re-ejecuta la tarea. Excepción documentada: rutas absolutas a un
   proyecto EXTERNO en el error ⇒ cache corrupto ⇒ `clean` justificado (aprendizaje 2026-05-27).
3. **PowerShell primero.** Citar props Gradle con punto: `"-Pkotlin.incremental=false"`. Scripts
   `.ps1` en ASCII o UTF-8 con BOM (Windows PowerShell 5.1).
4. **Fases pequeñas** + gate tras cada fase (`desktopTest` + `detekt` + compilación). No avanzar
   con un gate en rojo. Gates Gradle del mismo worktree SIEMPRE en serie, nunca en paralelo.
5. **Worktrees:** nunca sobre `main`/`master`. Para builds Android en un worktree nuevo, crear
   `local.properties` con `sdk.dir` usando `/` (no `\`).
6. **Sin ofertas de commit/PR:** reporta los cambios del working-tree; no ofrezcas ni pidas
   commit/push/PR.
7. **Detekt ratchet one-way:** nunca regenerar `detekt-baseline.xml` para silenciar; jerarquía:
   arreglar de verdad > constante nombrada > `@Suppress` localizado y justificado.
8. **Hooks de Claude Code:** SIEMPRE Node self-locating vía `.claude/hooks/mindos-guard.cjs`
   (`node -e "…require(…)"`), nunca `python3` (stub de Microsoft Store) ni sintaxis bash-only;
   advisory (`exit 0`), nunca bloqueantes. Probar el parseo real (`printf '{…}' | <cmd>`) antes de
   dar un hook por bueno.
9. **Reportes largos** (auditorías, análisis multi-agente) van a markdown dentro del repo; el chat
   solo lleva updates breves. No sobreactivar el Token Budget Gate para tareas locales, aditivas o
   de UI simple.
10. El árbol de trabajo puede mutar en paralelo (Vibe Kanban / n8n): confirma que TUS archivos
    aparecen o desaparecen de los findings de un gate; no "arregles" WIP untracked ajeno.

## 5. Persistencia y datos

- **Datos cognitivos sensibles:** `@Serializable` (con `schemaVersion`) + `SecureKeyValueStore`
  cifrado (Android: AES-256-GCM + Android Keystore; desktop: in-memory; iOS: Keychain durable en
  curso). **No Room ni SQLDelight** para estos datos; persistir solo **resúmenes** de sesión,
  nunca trials crudos. Repos `Local*` en commonMain, testeables con
  `InMemorySecureKeyValueStore`.
- **Namespaces por-usuario** (`MindBoostStorageNamespaces.AccountBound`): al añadir uno,
  actualizar EN EL MISMO CAMBIO el eraser de `AppGraph` (+ su test de cobertura) y
  `functions/src/accountDeletion.ts`. El borrado de datos de cuenta debe fallar CERRADO: nunca
  reportar completado sin demostrar que borró.
- **Firestore/Storage:** toda `allow list` exige cap de `request.query.limit` y la query cliente
  su `.limit()` espejo; App Check en contenido privado; timeouts en toda operación Firebase
  (`FirestoreDefaults` / `AuthDefaults` / `StorageDefaults` / `FunctionsDefaults`). Mutaciones de
  posts y snapshots públicos solo vía Cloud Functions autoritativas.
- **Telemetría/exports:** ningún sink remoto acepta `Throwable` externos crudos (usar el evento
  tipado allowlisted); exportaciones debug pseudonimizadas, sin identificadores estables; logs
  PII-free.
- **Tiempo:** nunca `Clock.System.now()` en holders/presentación — inyectar `EpochClock`;
  "desconocido" se modela con `null`, jamás con centinelas (`0L`, `""`, `-1`).

## 6. Navegación de contexto (Graphify, memoria, docs)

Este repo tiene una memoria estructural persistente en `graphify-out/` (grafo AST local, sin coste
LLM). **Si existe `graphify-out/graph.json`, orienta primero con el grafo** — no releas el repo ni
grepees a ciegas al empezar:

```bash
graphify query "<pregunta>"   # subgrafo acotado (módulos, símbolos, llamadas, rutas); --dfs, --budget N
graphify explain "<Nodo>"     # nodo + vecinos en lenguaje llano
graphify path "<A>" "<B>"     # camino más corto entre dos símbolos/conceptos
graphify affected "<X>"       # impacto inverso: qué se ve afectado por un cambio en X
graphify update .             # refrescar (AST-only, incremental) — tras cambios estructurales
```

- NO leas `graphify-out/graph.json` en crudo (~40 MB) ni uses `graphify extract . --out .` para
  refrescos (exige API key LLM). Frescura: compara el `Built from commit:` de
  `graphify-out/GRAPH_REPORT.md` con `git rev-parse HEAD`. Wrapper robusto:
  `.claude/scripts/graphify-refresh.sh`; scripts npm: `npm run memory:build` / `memory:update` /
  `memory:watch`. Los hooks git de graphify NO están instalados (opt-in deliberado: los agentes
  automáticos generarían churn de rebuilds).
- Lee `graphify-out/GRAPH_REPORT.md` para visión arquitectónica amplia; si existe
  `graphify-out/wiki/index.md`, úsalo para navegación amplia en vez de browsear fuente cruda.
- Lee fuente directa (Read/Grep dirigido) solo para editar, verificar líneas exactas, o cuando el
  grafo esté obsoleto o no cubra lo pedido (para el contenido exacto de un doc, lee el `.md`).
- Recordatorios automáticos: los hooks `PreToolUse` de `.claude/settings.json` inyectan el aviso
  Graphify vía `.claude/hooks/mindos-guard.cjs` (Node, advisory, nunca bloquean).
- **Toolkit `.claude/`** (port curado de ECC): skills, subagentes, slash-commands y guard —
  índice en [`.claude/README.md`](.claude/README.md). Atajos: `/kotlin-build` · `/gradle-build` ·
  `/kotlin-review` · `/kotlin-test` · `/plan` · `/quality-gate` · `/refactor-clean` ·
  `/security-scan`. Las skills de stack se auto-activan por contexto.
- **Browser automation:** `agent-browser` (dependencia del proyecto, no global):
  `npx agent-browser open <url>` → `snapshot -i` (refs `@eN`) → `click @eN` / `fill @eN "texto"`;
  re-snapshot tras cada cambio de página; `close --all` al terminar. Carga la guía con
  `npx agent-browser skills get core`. No hay dev-server web por defecto; objetivos típicos:
  emulador de Firebase Hosting o una URL externa.
- Docs clave: PRD en [`docs/MindBoost_PRD_Completo.md`](docs/MindBoost_PRD_Completo.md);
  arquitectura en `docs/architecture/`; diseño de ejercicios en `docs/mindboost_exercise_design/`.

## Regla núcleo: bucle de mejora automática del proyecto

Cada vez que corrijas un error, bug, fallo de compilación, fallo de arquitectura, fallo de integración, fallo de imports, fallo de dependencias, fallo de tipos, fallo de UI, fallo de estado, fallo de navegación, fallo de permisos, fallo de Firebase, fallo de Gradle, fallo de Kotlin, fallo de Compose o cualquier problema causado por una mala suposición previa, debes actualizar este archivo.

Tu obligación es convertir cada error corregido en una regla útil para evitar que vuelva a ocurrir.

---

## Protocolo obligatorio después de corregir un error

Después de resolver cualquier error real del proyecto, añade una entrada nueva en la sección:

`## Aprendizajes acumulados del proyecto`

La entrada debe tener este formato exacto:

```md
### YYYY-MM-DD — [Título corto del error]

**Error detectado:**
Describe el error concreto que ocurrió.

**Causa raíz:**
Explica por qué ocurrió realmente. No describas síntomas. Identifica la causa técnica.

**Solución aplicada:**
Describe qué cambio solucionó el problema.

**Regla futura:**
Define una regla clara que deberás seguir en futuras modificaciones para no repetir este error.

**Archivos afectados:**
- `ruta/del/archivo.ext`
```

---

## Criterios para actualizar este archivo

Debes actualizar este archivo cuando ocurra cualquiera de estos casos:

1. Has corregido un error de compilación.
2. Has corregido un crash.
3. Has corregido un bug visual.
4. Has corregido una navegación rota.
5. Has corregido una integración mal conectada.
6. Has corregido imports incorrectos.
7. Has corregido un problema de Gradle, Kotlin, Compose, Firebase o permisos.
8. Has detectado que una decisión anterior era incorrecta.
9. Has descubierto una convención específica de este proyecto.
10. Has aprendido una restricción técnica que debe recordarse.

No actualices este archivo por cambios triviales de texto, estilos menores o modificaciones que no enseñen una regla reutilizable.

---

## Reglas para escribir aprendizajes

Los aprendizajes deben ser:

- Concretos.
- Técnicos.
- Reutilizables.
- Breves.
- Basados en errores reales corregidos.
- Escritos para evitar repetir el mismo fallo en el futuro.

No escribas reflexiones largas. No escribas teoría genérica. No escribas documentación innecesaria. No inventes problemas. No añadas reglas si no se ha corregido un error real.

---

## Uso obligatorio antes de modificar código

Antes de hacer cualquier cambio en el código, revisa la sección `Aprendizajes acumulados del proyecto`.

Debes aplicar esas reglas como restricciones activas durante la edición.

Si una regla previa entra en conflicto con una instrucción nueva del usuario, sigue la instrucción nueva del usuario, pero conserva la regla anterior salvo que haya quedado obsoleta.

Si una regla queda obsoleta, no la borres: añade una nota debajo indicando que ha sido reemplazada y explica por qué.

---

## Referencia rápida — errores que obligan a actualizar

- compilación
- crash
- navegación
- imports
- dependencias
- Gradle
- Kotlin
- Compose
- Firebase
- permisos
- arquitectura
- estado
- integración
- UI rota
- suposición incorrecta del agente

No añadas teoría. No añadas reglas genéricas. No inventes aprendizajes. No borres aprendizajes antiguos salvo que el usuario lo pida.

Actualiza este archivo solo si el error corregido enseña una regla reutilizable.

---

## Aprendizajes acumulados del proyecto

### 2026-07-21 — Portar un módulo KMP que nunca compiló para Kotlin/Native: el gate iOS se corre en CI (runner macOS) desde Windows, y la migración es por CAPAS (imports jvm-default → stdlib JVM-only → cinterop iOS → actual faltante)

**Error detectado:**
El módulo `:shared` declara los targets iOS con `if (isMacOs)`, así que en Windows la compilación iOS estaba diferida y **nunca se había compilado para Kotlin/Native**. Al levantarla en un runner macOS de GitHub Actions, el primer compile de Native reveló **129 errores en 28 archivos** (social 28, mindboost 19, arena 13, core 4) — no drift de `actual`, sino que TODO el módulo compartido arrastraba dependencias solo-JVM. Antes de eso, el build ni siquiera pasaba la fase de configuración de Gradle ni la instalación de pods.

**Causa raíz:**
Un módulo que solo se compiló para JVM (Android + desktop) acumula, invisible, deudas que Native no tolera, en cuatro capas distintas que afloran una tras otra (el compilador aborta y "tapa" la siguiente): (1) el código depende del import por defecto `kotlin.jvm.*` que SOLO existe en JVM; (2) usa stdlib solo-JVM; (3) los `actual` de iOS (escritos pero nunca compilados) tienen imports de plataforma incorrectos y les falta `@OptIn`; (4) hay `expect` sin `actual` de iOS. Además la integración CocoaPods/Firebase para iOS estaba incompleta (solo Core+Auth) y mal cableada.

**Solución aplicada (por capas, cada una validada por un run de CI ~40 min; 129 → 12 → 2 → 1 → 0):**
- **Build/CocoaPods:** `cocoapods` es una sub-extensión de la extensión `kotlin`, NO del proyecto (`(kotlin as ExtensionAware).extensions.configure<CocoapodsExtension>("cocoapods")`, no `project.extensions`). gitlive NO enlaza el SDK nativo de Firebase transitivamente: cada módulo gitlive necesita su pod (`firebase-app→FirebaseCore`, `-auth→FirebaseAuth`, `-firestore→FirebaseFirestore`, `-functions→FirebaseFunctions`, `-storage→FirebaseStorage`). **Fijar los pods a la versión EXACTA que gitlive linka** (gitlive 2.4.0 → `firebase-cocoapods = 11.8.0`), o CocoaPods baja la 12.x (rompe cinterop de gitlive) y exige toolchain aún más nuevo. **Firebase iOS ≥11.7 usa Swift 6** (`sending`, `public import`) → el runner necesita **Xcode 16** (el default macos-14 es 15.4/Swift 5.10); seleccionar el más nuevo (`xcode-select -s`).
- **Capa 1 — imports jvm-default:** `@JvmInline` y `@Volatile` son import por defecto en JVM (`kotlin.jvm.*`) pero NO en Native → añadir explícito `import kotlin.jvm.JvmInline` y `import kotlin.concurrent.Volatile` (esta última es la multiplataforma; la de `kotlin.jvm` es JVM-only). No-op en JVM.
- **Capa 2 — stdlib JVM-only en commonMain:** `toSortedSet` (TreeSet) → `distinct().sorted()`; `Map.getOrDefault` (método JDK) → `map[k] ?: default`; `String.format("%02d")` → `padStart`; `kotlinx.datetime.Clock.System` (frágil, learning 2026-05-29) → `kotlin.time.Clock.System` (stdlib, todos los targets).
- **Capa 3 — cinterop de los `actual` iOS:** `NSObject` está en `platform.darwin` (no `platform.objc`); `AVAudioSession`/`…CategoryPlayback` en `platform.AVFAudio` (no `AVFoundation`); `memcpy` en `platform.posix`; `CMTime` de un método ObjC llega como `CValue<CMTime>` → leer campos con `.useContents { value }`, construir con `CMTimeMake(0,1)` (NO `CMTime()`, que pide `rawPtr`); `NSDate.timeIntervalSince1970` es una **extension property** que necesita su propio import (`import platform.Foundation.timeIntervalSince1970`); `NSDate.date()` (método de clase) no se expone → usar el ctor `NSDate()`; los NS_ENUM de PDFKit (`PDFDisplayMode`/`PDFDisplayDirection`) se exponen como **NSInteger crudo**, no enum-class → asignar el entero (`displayMode = 1`, `displayDirection = 0`); `Dispatchers.IO` es internal en Native → `Dispatchers.Default`; `Spacer` de `foundation.layout` (no `material3`); `Float`→`Double` con `.toDouble()`; puente `ByteArray→NSData` para el `Data` de gitlive (`usePinned`+`addressOf`+`NSData.create`); todo sitio cinterop exige `@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)` (o `@file:OptIn`).
- **Capa 4 — `actual` faltante (el drift REAL que el gate existe para cazar):** `expect DMCajoneraEntryIcon` tenía `actual` en android/desktop pero no en iosMain → crear el `actual` iOS espejando el de desktop (sin default en el param, la regla expect/actual lo hereda del `expect`).

**Regla futura:**
Para portar a Native un módulo KMP que solo compiló para JVM: (1) el gate iOS desde Windows se corre en CI en un runner macOS (`compileKotlinIosSimulatorArm64` para drift de `actual`; el simulador comparte todos los `actual` con el device, así que basta una arch y se ahorra la mitad del build de pods); cada run frío recompila TODOS los pods desde fuente (~40 min, Firestore arrastra gRPC/BoringSSL en C++). (2) No se puede compilar "solo una feature" para iOS: el compile mezcla commonMain+iosMain, así que TODO el módulo debe ser Native-compatible (el port toca social/arena/mindboost, no solo la feature objetivo). (3) Los errores vienen por CAPAS: arreglar una capa destapa la siguiente; batchear todos los fixes de una capa por push para no gastar iteraciones de CI. (4) Todo cambio de commonMain debe seguir siendo JVM-válido (imports no-op, stdlib multiplataforma, misma salida). (5) El código de un `actual` iOS nunca compilado suele tener imports de plataforma mal (`platform.objc` vs `platform.darwin`, `AVFoundation` vs `AVFAudio`) y le falta `@OptIn(ExperimentalForeignApi)`; ante duda de una convención cinterop (nombre de entrada de NS_ENUM, extension property) VERIFICARLA (docs/fuente K/N) antes de gastar un run de 40 min adivinando. (6) `kotlin.time.Clock`/`Instant` son stable desde Kotlin 2.2 — preferirlos a `kotlinx.datetime.Clock.System`. (7) `iosSimulatorArm64Test` compila `commonTest`+`iosTest` y enlaza el SDK nativo: el task de Kotlin/Native NO imprime conteo de tests a consola (solo XML) — verificar que el task **ejecutó** (no `up-to-date`) y pasó; para un número, subir `build/test-results` como artefacto. (8) `kotlin-ci.yml` solo dispara en push/PR a `main` (sin `workflow_dispatch`): en una rama WIP el gate JVM definitivo no se puede lanzar sin tocar main; verificar JVM-safety estáticamente + por el propio compile Native.

**Archivos afectados:**
- `shared/build.gradle.kts` (cocoapods sobre `kotlin`, 5 pods pinneados a 11.8.0) · `.github/workflows/ios-build.yml` (Xcode 16, compile sim-arch + sim-test)
- commonMain (imports jvm-default + stdlib multiplataforma): `core/{analytics/MindBoostAnalytics,config/FeatureFlags,logging/Observability,time/EpochClock}.kt`, `features/{arena/**,mindboost/**,social/**}` value-objects + `focus/presentation/FocusHomeStateHolder.kt` + `social/data/publishing/InMemoryPublishingRepositories.kt` + `social/domain/publishing/PublishingUseCases.kt` + 2 screens (`format`)
- iosMain (cinterop): `core/media/PlatformFilePicker.ios.kt`, `dm/{data/DMStorageData,presentation/media/DMAudioRecorder}.ios.kt`, `social/{data/repository/ProfileVisualStorageData,domain/feed/GlobalFeedTime,presentation/feed/FeedAudioPlayer,presentation/media/AppMediaViewer,presentation/profile/ProfileMediaPlatform,presentation/publisher/PublisherPlatformStore}.ios.kt` + NUEVO `dm/presentation/DMListScreenIcon.ios.kt`

### 2026-07-20 — `sed` sobre un fichero con contenido no-ASCII lo DESTRUYE dos veces (alternancia BRE + decodificación UTF-8), y el hash canónico de un fichero versionado es irreproducible bajo `core.autocrlf` sin `.gitattributes`

**Error detectado:**
Para quitar una notación de valor absoluto que rompía una tabla markdown del ledger, ejecuté
`sed -i "s/\\\\<pipe>d'\\\\<pipe>/abs(d')/g"` sobre `MINDBOOST_AUDIT_LEDGER.md`. El fichero pasó de 196 KB a
**2.066.328 bytes**. Al revertir la inserción quedaron **3446 caracteres destruidos en 272 líneas** y, además,
**todos los literales `d'` borrados**. En paralelo, el validador del ledger reportaba `hash mismatch` sobre un
árbol RECIÉN restaurado de git — es decir, era estructuralmente incapaz de pasar en esta máquina.

**Causa raíz:**
Dos defectos distintos que se disfrazaron el uno al otro. (1) En **GNU sed BRE la secuencia barra-invertida +
pipe es el operador de ALTERNANCIA**, no un pipe escapado: el patrón se redujo a "vacío O `d'` O vacío", la rama
vacía casó en CADA posición e insertó el reemplazo entre todos los caracteres, mientras la otra rama CONSUMIÓ
cada `d'` real. Peor: `sed` decodifica como UTF-8, y sus propias inserciones partieron los caracteres multibyte,
así que emitió U+FFFD por byte — el daño no es la inserción (reversible), es la **pérdida de los bytes no-ASCII**
(irreversible desde la copia de trabajo). (2) El hash canónico del ledger se acuña sobre contenido unido con LF,
pero `core.autocrlf=true` **sin `.gitattributes`** lo materializa en CRLF en cada checkout ⇒ el hash recomputado
nunca coincide. El defecto original que perseguía el `sed` también era real: un pipe escapado dentro de una celda
de tabla markdown rompe las columnas porque el validador parte por `|` sin honrar el escape.

**Solución aplicada:**
Recuperación **sin inferencia**, que es la parte que importa: reconstruir un registro de auditoría adivinando
caracteres por contexto es exactamente la fabricación que el ledger existe para impedir. (1) copia corrupta
preservada como evidencia ANTES de escribir nada; (2) `git checkout HEAD` restauró la base commiteada —que
contenía **CERO** filas `MB-FOCUS`, porque R0/S0/S1/S2 eran íntegramente working-tree sin commitear—; (3) las 30
llamadas `Edit`/`Write` al ledger se extrajeron **VERBATIM del transcript de la sesión** (`.jsonl`) y se
reprodujeron en orden; (4) se reprodujo el único comando de shell que había cambiado contenido; (5) diff de
esqueleto contra la copia corrupta: 466/466 líneas. **Prueba criptográfica de recuperación exacta:** la
reconstrucción reproduce por sí sola el hash `2d324081…4da995`, el sellado por el último comando pre-incidente.
Fixes: `.gitattributes` fija `MINDBOOST_AUDIT_LEDGER.md text eol=lf`, y los pipes escapados pasaron a `abs(d')`.

**Regla futura:**
**Nunca `sed` (ni `-i` ni en pipe) sobre un fichero con contenido no-ASCII** — em-dash, `§`, acentos, flechas: el
daño UTF-8 es irreversible aunque la sustitución se revierta. Usar Edit/Write o `node`. En un patrón sed, la
secuencia barra-invertida + pipe NO es un pipe escapado: es alternancia, y una alternancia con rama vacía casa en
cada posición (verificar todo patrón con `sed -n 's/…/…/gp'` ANTES de `-i`). Si un fichero versionado sella un
hash sobre su propio contenido, **fijar su final de línea en `.gitattributes`** o el hash será irreproducible en
Windows y el validador imposible de pasar en un árbol limpio. Nunca meter un `|` (ni escapado) en una celda de
tabla markdown que un validador vaya a trocear: usar notación alternativa (`abs(x)`). Y ante un fichero corrupto,
el orden correcto es **preservar la copia dañada → restaurar de git → reproducir las ediciones desde una fuente
con los bytes reales (el transcript) → verificar por hash**; jamás reparar por inferencia y presentarlo como el
registro.

**Archivos afectados:**
- `.gitattributes` (nuevo) · `MINDBOOST_AUDIT_LEDGER.md` (`MB-LEDGER-0007`) · `scripts/validate-audit-ledger.mjs` (sin cambios; el defecto era del fichero, no del validador)

### 2026-07-20 — Partir una métrica POR CONDICIÓN divide la muestra por 4 y puede vaciar el suite entero: cinco de seis aserciones pasan sobre operandos AUSENTES

**Error detectado:**
La tarea (5) de Focus V5 · S2 exigía correr un scorer candidato (exactitud/d′/criterio **por condición**) contra
cinco perfiles adversariales, con el n=55/perfil ya ratificado. Al ir por condición, ese 55 se reparte en 2
condiciones × 2 celdas (señal/ruido) ⇒ **~14 por celda**, por debajo del piso de `MinSampleGate.DPRIME` = 20.
Todas las celdas habrían devuelto `Absent(INSUFFICIENT_N)`, y una comparación cuyos dos operandos son ausencias
**no se ejecuta**: A1 ("honesto > pulsador"), A2, A3, A5 y A6 pasan TRIVIALMENTE. Cinco verdes de seis, cero
números producidos. El informe habría dicho "fórmula validada en S2" y el fallo habría aparecido meses después,
con datos reales, llevando esa etiqueta puesta.

**Causa raíz:**
El n se ratificó para un estadístico **de sesión**; la tarea lo cambió a un estadístico **por condición** sin
recalcularlo. Y el comportamiento honesto del scorer ante muestra insuficiente —devolver ausencia en vez de un
número— es justo lo que convierte las aserciones de ordenamiento en tautologías: `absent > absent` no falla,
simplemente no se evalúa. La honestidad del productor crea la vacuidad del test. Es el patrón 2026-07-15 (un
golden que solo afirma rango y auto-consistencia) en su forma más peligrosa, porque aquí el suite ni siquiera
está mal escrito: está bien escrito y aun así no prueba nada.

**Solución aplicada:**
(1) n recalculado desde el piso hacia arriba: E07=112 (28 señal + 28 ruido por condición), E01=120 (24 señal al
20% + 96 ruido). (2) **Guard anti-vacuidad**: todo operando de toda comparación pasa por `requirePresent`, que
**falla** ante una ausencia en vez de saltársela. (3) La mitad discriminante (A4) afirma **los conteos de celda**,
no solo los valores, así que el suite no puede ponerse verde hasta que la muestra sea real. (4) Verificado por
mutación, no por estar verde: forzando todas las celdas bajo el piso, **11 de 20 tests se ponen en rojo**.

**Regla futura:**
Al cambiar un estadístico de "por sesión" a "por condición" (o al añadir cualquier estratificación), RECALCULAR
el tamaño de muestra contra `MinSampleGate` ANTES de correr nada: la estratificación divide la muestra por el
producto de los niveles, y un n ratificado para el agregado casi nunca sobrevive. En todo suite donde el sujeto
pueda devolver "no hay dato", las aserciones de ORDENAMIENTO (`a > b`) son vacuas por construcción: envolver cada
operando en un guard que FALLE ante la ausencia, y añadir al menos una aserción que exija los CONTEOS. Verificar
la no-vacuidad con la mutación "haz que todo devuelva ausencia" y contar cuántos tests caen: si caen pocos, el
suite mide poco. Y `Absent` no es lo mismo que `0.0`: mantenerlo tipado es lo que hace que el guard sea posible
—con un `Double` centinela la vacuidad sería invisible—.

**Archivos afectados:**
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/focus2/simulation/{CandidateDescriptiveScorer,SyntheticTrialStreams,FocusV5CandidateOracleTest}.kt`

### 2026-07-20 — Una corrección estadística INCONDICIONAL sesga de forma desigual cuando las celdas están desbalanceadas; y un veredicto de "no discriminó" no debe apoyarse en un umbral sobre el estadístico corregido

**Error detectado:**
El scorer candidato aplica la corrección loglineal de Hautus (1995) —sumar 0.5 a cada conteo y 1.0 a cada
total— **siempre**, para que d′ sea finito incluso con tasas de 0 o 1. En la tarea de vigilancia las celdas están
desbalanceadas por diseño (24 señal / 96 ruido, objetivos escasos). Un usuario que responde a TODO da
H = 24.5/25 = 0.980 y F = 96.5/97 = 0.995: la celda grande se corrige **menos**, así que
**d′ = −0.51**, no 0. El veredicto de "no discriminó nada" comparaba |d′| contra una banda cercana a cero, y
−0.51 se sale de ella ⇒ esa sesión salía **VALID**. Segundo defecto en el mismo sitio: un usuario que elegía lado
a cara o cruz sacaba d′ = 0.50 y 0.25 (ruido de muestreo, error estándar ~0.37) y también salía VALID, porque el
veredicto comparaba un PUNTO contra un umbral — mientras `MinSampleGate` ya marcaba
`requiresConfidenceInterval = true` para DPRIME.

**Causa raíz:**
Una corrección incondicional es la elección correcta (las alternativas —`1/(2N)` o descartar celdas extremas— son
CONDICIONALES, hacen el estimador discontinuo y sesgan al alza justo a los mejores), pero su sesgo **no es
uniforme entre celdas de distinto tamaño**. Construir un umbral sobre el estadístico ya corregido hereda ese
sesgo. Y un estimador sin su incertidumbre no puede sostener la afirmación "esto es distinto de cero".

**Solución aplicada:**
(1) El veredicto ya no se apoya en |d′|: una sesión con **una sola respuesta distinta** en todos los ensayos
puntuables es `Invalidated(DEGENERATE_RESPONDING)`, hecho derivado de **conteos de respuesta** y no del
estadístico corregido — más directo, inmune al sesgo y disponible sin leer latencia (leerla habría violado la
regla "RT secundario"). (2) d′ lleva su error estándar
(`var = H(1−H)/(Ns·φ(zH)²) + F(1−F)/(Nn·φ(zF)²)`) y el veredicto exige que el **límite inferior del intervalo del
95%** supere cero. (3) El sesgo NO se ajustó para taparlo: se documentó como el precio declarado de que d′ sea
siempre finito.

**Regla futura:**
Antes de construir un umbral sobre un estadístico CORREGIDO, comprobar si la corrección sesga de forma desigual
según el tamaño de las celdas — con celdas desbalanceadas (tareas de vigilancia, tasas base bajas, cualquier
diseño de objetivos escasos) casi siempre lo hace. Preferir derivar los veredictos de HECHOS CRUDOS
(conteos, número de respuestas distintas) antes que de estadísticos derivados: el hecho crudo no hereda el sesgo
de ninguna corrección. Ningún veredicto de "es distinto de cero" puede apoyarse en un estimador puntual: si el
repo ya exige intervalo (`MinSampleGate.requiresConfidenceInterval`), honrarlo. Y toda regla que endurezca un
veredicto necesita su **mitad discriminante** en el mismo cambio (aquí: el usuario honesto DEBE seguir superando
cero), o "apretar hasta que no pase nadie" se disfraza de arreglo.

**Archivos afectados:**
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/focus2/simulation/CandidateDescriptiveScorer.kt`

### 2026-07-20 — Reutilizar un helper de lectura compartido puede ROMPER el contrato que tu propia clase promete; y un tombstone es justo el dato que `quarantine` borra

**Error detectado:**
`SecureFocusConsentStore` (Focus V5 · S1) prometía en su KDoc que revocar **reescribe** el registro y nunca lo
borra, porque "un registro borrado es indistinguible de uno que nunca se preguntó, y 'el usuario dijo que no' es
exactamente el hecho que no podemos perder". El código usaba `readVersioned` → `readParsed` → `readValueOrNull`,
que ante CUALQUIER `SecureReadResult.Failure` devuelve `null` **y llama a `quarantine`**, y `quarantine` hace
`entries.remove(key)`. Resultado: un fallo de backend, un payload corrupto o un registro escrito por una versión
NUEVA (justo el downgrade N-1 que contempla ADR-FOCUS-MIGRATION) convertía **REVOKED → UNKNOWN de forma
permanente**, y la app volvería a pedirle consentimiento a quien ya lo había rechazado. Lo cazaron DOS lentes
independientes del gate de salida (privacy-security y efficacy-guardian); ningún test lo veía.

**Causa raíz:**
Se asumió que un helper de lectura compartido es neutral respecto al contrato del llamador. No lo es:
`readValueOrNull` está diseñado para datos RECUPERABLES (un resumen corrupto se puede regenerar jugando otra
sesión), y por eso su política —cuarentena y sigue— es correcta ahí. Un registro de consentimiento NO es
recuperable: es una declaración del usuario que no se puede volver a derivar de nada. Aplicarle la política del
dato regenerable destruye precisamente el hecho que el tipo existe para conservar. Es el learning 2026-07-05
(un `read` que colapsa fallo en ausencia) con una vuelta más: aquí el colapso no venía de MI `catch`, venía del
helper que reutilicé, así que el defecto no era visible leyendo mi propio archivo.

**Solución aplicada:**
La clase lee el `SecureReadResult` CRUDO y lo mapea a un `sealed ConsentLoad {Found, Absent, Unreadable}`
privado; se añadió un CUARTO valor público `FocusConsentStatus.UNAVAILABLE` para distinguir "no pudimos
averiguarlo" de "nunca se preguntó" (los dos deniegan, pero solo uno justifica volver a preguntar). Ninguna ruta
de lectura de consentimiento llama a `quarantine`, así que un registro ilegible SIGUE EN DISCO y es recuperable
por una versión posterior. Tres tests lo fijan, y el de fallo de backend afirma además que **el registro sigue
leyéndose en el store subyacente** — sin esa segunda aserción el test pasaría igual con la cuarentena puesta.

**Regla futura:**
Antes de reutilizar un helper de lectura compartido (`readValueOrNull`/`readParsed`/`readVersioned`), comprobar
qué hace ante `Failure`: si CUARENTENA (borra), solo es apto para datos REGENERABLES. Para un dato irrepetible
—consentimiento, tombstone de revocación, marca de auditoría, cualquier declaración del usuario— leer el
resultado crudo y mapear a un tipo de 3 vías, NUNCA cuarentenar. Un estado de consentimiento tiene CUATRO
valores, no tres: "nunca preguntado", "sí", "no" y "no se pudo leer" son hechos distintos, y colapsar el cuarto
en el primero re-pregunta a quien ya dijo que no. Cuando un KDoc PROMETE una garantía de durabilidad, el test que
la respalda debe afirmar el estado del ALMACÉN (el registro sigue ahí), no solo el estado devuelto. Y la versión
futura de schema es `Unreadable`, jamás `Absent`: bajo ventana N-1, confundir el "no" de mañana con silencio es
el mismo defecto con otro disfraz.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus2/data/consent/FocusDataConsent.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/focus2/data/consent/FocusDataConsentTest.kt`

### 2026-07-20 — Cambiar el FIXTURE de un test para que siga en verde puede vaciarlo de poder discriminante sin que nada lo detecte

**Error detectado:**
Al retipar `EligibilityInputs.purpose` (6 valores → 11), el test
`SessionEligibilityPolicyTest."under 120s without a protocol does not count for calibration"` pasó de usar
`TRAINING_FREE` a `INITIAL_ASSESSMENT`. Seguía verde. Pero `INITIAL_ASSESSMENT` **no está** en la allowlist de
calibración, así que `calibrationEligible` era `false` **por propósito**, y los 90 s eran inertes: borrar la regla
de los 120 s por completo habría dejado el test igual de verde. El cambio se hizo para que una aserción vecina
(`assertTrue(profileEligible)`) siguiera pasando. Lo cazó `diff-auditor` en contexto fresco; la suite completa en
verde no puede detectar su propia pérdida de poder.

**Causa raíz:**
Ante un test que se pone rojo por un cambio de tipos, hay dos reparaciones que se ven idénticas en el diff:
ajustar el fixture **conservando** lo que el test discrimina, o ajustarlo hasta que pase. La segunda es una
regresión invisible, porque el artefacto que la delataría —un test rojo— es exactamente lo que se acaba de
eliminar. El riesgo es máximo justo cuando un cambio de tipo obliga a tocar muchos fixtures a la vez y "que
compile y pase" se convierte en el criterio de éxito.

**Solución aplicada:**
Restaurado `TRAINING_FREE` (que SÍ es elegible para calibración, así que la denegación vuelve a ser atribuible a
la duración) y la aserción vecina cambiada a `assertFalse` para declarar la realidad honesta: ese propósito no
llega a la vía de perfil. Se añadió un comentario explicando que la ELECCIÓN DEL PROPÓSITO es la que sostiene el
test, para que la próxima edición no lo repita.

**Regla futura:**
Al tocar el fixture de un test para adaptarlo a un cambio de tipos, preguntar siempre: **¿qué regla afirma este
test, y mi fixture nuevo sigue haciéndola la ÚNICA causa del resultado esperado?** Si la aserción ya se cumple
por un gate distinto (propósito, flag, allowlist), el test dejó de probar lo que dice su nombre. Verificación
barata: borra mentalmente la regla bajo prueba — si el test seguiría verde, está vacío. Nunca cambiar un fixture
para salvar una aserción VECINA: si esa aserción ya no es cierta, la honesta es invertirla, no reescribir el caso
alrededor de ella. Un cambio de tipo que toca N fixtures exige revisar los N por poder discriminante, no solo por
compilación — y esta clase de defecto no la encuentra la suite, la encuentra una revisión de diff en contexto
fresco.

**Archivos afectados:**
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/session/cognitive/SessionEligibilityPolicyTest.kt`

### 2026-07-19 — Un subagente lanzado en background PUEDE invocar Gradle: eso viola "gates en serie" y produce una cascada de `NoClassDefFoundError` que parece 25 regresiones

**Error detectado:**
Lancé los dos agentes del gate de salida (`efficacy-guardian`, `diff-auditor`) con `run_in_background: true` y,
mientras trabajaban, ejecuté `:shared:desktopTest` desde la sesión principal. Resultado: **25 tests fallando en
clases sin relación con mi cambio** (`FeatureFlagsTest`, `AppGraphTest`, `AppRouteStackTest`, `ObservabilityHubTest`,
`TimeoutDefaultsTest`…), varias con `initializationError`. La causa real era
`java.lang.NoClassDefFoundError: com/mindos/app/core/config/StaticFeatureFlags` — clases AUSENTES del classpath de
test. `Get-Process` confirmó **5 JVMs**, tres arrancadas en el mismo minuto que mi build.

**Causa raíz:**
El `diff-auditor` hace su trabajo bien: para auditar de verdad EJECUTA los gates (`desktopTest --rerun-tasks`,
`detekt`, compilaciones). Yo asumí que un agente "read-only" solo lee ficheros. Dos invocaciones Gradle
concurrentes escribiendo `shared/build` del MISMO worktree corrompen los outputs incrementales — exactamente el
fallo documentado el 2026-06-19. La regla "gates Gradle del mismo worktree SIEMPRE en serie" ya existía; lo que
faltaba era darse cuenta de que **un subagente en background es un segundo invocador de Gradle**.

**Solución aplicada:**
No se cambió NADA de código a partir de esa corrida (habría sido "arreglar" fantasmas). Se diagnosticó la firma
(`NoClassDefFoundError` en cascada sobre clases no tocadas + JVMs concurrentes), se esperó a que el worktree
quedara libre y se re-ejecutó en serie: **3776/3776 verde**. La corrida corrupta se conservó como evidencia y el
incidente se registró en el ledger.

**Regla futura:**
Antes de lanzar un subagente que pueda correr gates (`diff-auditor`, `test-engineer`, `kotlin-build-resolver`,
cualquier revisor con Bash), decide QUIÉN ejecuta Gradle: o la sesión principal o el agente, nunca ambos. Dos
opciones válidas: (a) lanzar el agente con instrucción explícita de NO invocar Gradle y pasarle TÚ los resultados
verificados; (b) no tocar Gradle mientras el agente viva. Diagnóstico: una cascada de fallos en clases que tu diff
NO toca, con `NoClassDefFoundError`/`initializationError`, es corrupción de build, NO regresiones — verifícalo con
`Get-Process` (JVMs concurrentes) y re-ejecuta en serie ANTES de cambiar una sola línea. Corolario: un agente que
ejecuta gates y reporta un fallo REAL (aquí `DailyPlanGeneratorTest`) puede mezclarlo con fallos artefacto en la
misma corrida; atribuye cada fallo por causa antes de aceptarlo o descartarlo.

**Archivos afectados:**
- (ninguno — disciplina de orquestación; evidencia en `MINDBOOST_AUDIT_LEDGER.md` §3, task `TASK-2026-07-19-FOCUS-V5-S0-CONTAINMENT`)

### 2026-07-19 — `BUILD SUCCESSFUL` con la tarea de test `UP-TO-DATE` NO es evidencia de que los tests corrieran: un gate de baseline exige ejecución REAL (`--rerun-tasks` + conteo de XML frescos)

**Error detectado:**
Al reproducir el baseline del repo (gate `T-BASE` del plan Focus V5) los 5 comandos Gradle devolvieron exit 0 y
`BUILD SUCCESSFUL`. Estuve a punto de reportar "baseline verde". El log de `:shared:desktopTest` decía
`11 actionable tasks: 1 executed, 10 up-to-date` y, en la lista de tareas, `> Task :shared:desktopTest UP-TO-DATE`:
**cero tests ejecutados**. Confirmado independientemente: 536 `TEST-*.xml` en disco, **0 escritos en los últimos
30 minutos**. El verde era HEREDADO de una build anterior (de otro HEAD/otra sesión), no demostrado sobre este árbol.

**Causa raíz:**
Gradle considera `test` UP-TO-DATE si sus inputs no cambiaron, y `BUILD SUCCESSFUL` describe el resultado del BUILD,
no del SUITE. El exit code 0 es idéntico en "3760 tests pasaron" y en "no corrió ningún test". Cuando el baseline
previo conocido era ROJO (aquí: `integration-baseline`, 17 fallos en `71e17038`) y la caché viene de otro árbol,
aceptar el UP-TO-DATE es exactamente el falso verde que el charter prohíbe: se AFIRMA un gate sin output real.

**Solución aplicada:**
Re-ejecución con `--rerun-tasks` (no destructivo; no es `clean` ni `--stop` ni `--no-build-cache`): `11 actionable
tasks: 11 executed`, `> Task :shared:desktopTest` sin sufijo, 2m48s, y agregando los 536 XML **recién escritos**:
`tests=3760 failures=0 errors=0 skipped=0`. Igual para `:shared:detekt` (1s up-to-date → re-run real, `detekt.xml`
con 0 `<error>`). El primer intento se registró en el ledger como falso verde RECHAZADO, no se borró.

**Regla futura:**
Un gate de baseline/exit-gate NUNCA se declara verde por `BUILD SUCCESSFUL` + exit 0. Verificar SIEMPRE las dos
señales: (1) la línea `N actionable tasks: N executed` (si dice `up-to-date` para la tarea que ES el gate, no corrió)
y (2) la propia tarea sin sufijo `UP-TO-DATE`/`FROM-CACHE` en el log. Ante duda, forzar con `--rerun-tasks` y
corroborar con evidencia externa al log: contar los `TEST-*.xml` escritos en la ventana temporal de la corrida
(`find … -newermt "-20 minutes"`) y AGREGAR `tests/failures/errors/skipped` de los XML — el conteo de tests es el
dato que se reporta, nunca "BUILD SUCCESSFUL". Esto aplica con más fuerza cuando el baseline conocido era rojo o la
caché puede venir de otro HEAD/worktree. Corolario para el ledger: un verde heredado es `REPORTED_GREEN` como mucho;
solo la ejecución real con conteo de tests sostiene `MITIGATED`/`TEST_VERIFIED`.

**Archivos afectados:**
- (ninguno — disciplina de verificación de gates; evidencia en `MINDBOOST_AUDIT_LEDGER.md` §3/§4, task `TASK-2026-07-19-FOCUS-V5-R0-BASELINE`)

### 2026-07-15 — `null ≠ 0` tiene una INVERSIÓN peor: dato ausente que fabrica un 100/100; y un centinela `0.0` en el helper `unavailable` es el mismo defecto una capa más abajo

**Error detectado:**
`DefaultMetricsEngine` (Enfoque) daba **100/100 de foco sostenido y 1.0 de estabilidad PERFECTA a un usuario que
no respondió nada**. Cadena: `RtStatistics.median(emptyList()) = 0.0` (documentado "callers gate on n") →
`rtCv = 0.0` → `BLOCK_STABILITY = (1.0 - 0.0).coerceIn(0,1) = 1.0` → `SUSTAINED_FOCUS_INDEX = 100.0 * 1.0 * 1.0
= 100.0`. El único guard era `confident = false`, un flag que el caller puede ignorar (`rawValueOf` lo ignora
por diseño). No era breach vivo de perfil sólo porque `FocusProfileSource.kt:55-61` es fail-closed y deja
ATTENTION en CALIBRATING — el defecto estaba armado justo debajo de esa puerta. Lo cazó una review adversarial
de eficacia; se reprodujo como test rojo ANTES de aceptarlo.

**Causa raíz:**
El charter prohíbe fabricar un **0** desde dato ausente, y toda la vigilancia del repo apunta a ese lado. Pero la
misma ausencia, metida en una fórmula de "penalización" (`1 - x`, `100 * (1-cv) * (1-lapseRate)`), fabrica el
**máximo**, no el mínimo: cero dispersión medida se lee como dispersión cero = perfección. Un producto de
entrenamiento está estructuralmente tentado a shipear el halago, así que la inversión es MÁS peligrosa que el 0.
Agravante: `MetricValue.unavailable(...)` codifica "desconocido" como `value = 0.0, confident = false` — es decir,
el propio helper de ausencia usa el centinela que el charter prohíbe (mismo patrón que `0L` timestamp, learning
2026-07-04).

**Solución aplicada:**
`blockStability(rtCv, cleanRtCount)` y `addFocusIndices(..., cleanRtCount)` devuelven `MetricValue.unavailable`
cuando `cleanRtCount == 0` (o `n == 0`); `cleanRtCount` es parámetro OBLIGATORIO, no derivado — el índice no
puede computarse sin él. Test `FocusIndexHonestyTest` con la mitad discriminante obligatoria: el bloque real de
8 hits DEBE seguir dando su valor confident y alto (si no, el "fix" sería clampar a 0 y romper la métrica para
todos). `LongMethod` se resolvió extrayendo `blockStability`, no suprimiendo.

**Regla futura:**
Al auditar `null ≠ 0`, buscar las DOS direcciones: dato ausente que fabrica un mínimo Y dato ausente que fabrica
un MÁXIMO. Toda fórmula de la forma `1 - x` / `100 * (1-a) * (1-b)` alimentada por un estadístico que devuelve
`0.0` sobre lista vacía fabrica perfección — auditar cada `median`/`mean`/`cv` cuyo contrato diga "returns 0.0
for empty (callers gate on n)": ese comentario es una obligación que NADIE enforcea. `confident = false` NO es un
guard (existe `rawValueOf` que lo ignora); el guard va en el productor, devolviendo `unavailable`, no en la
disciplina del consumidor. El sample-count que hace computable una métrica es parámetro obligatorio, no derivado
(mismo patrón que `diagnostics` obligatorio, learning 2026-07-08). Un test de "ausencia no fabrica" DEBE llevar
su mitad discriminante (un caso real que sigue dando valor confident), o pasa clampando todo a cero. Y `unavailable`
sigue codificando 0.0: `MetricValue.value: Double?` queda como follow-up declarado, no cerrado.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/metrics/DefaultMetricsEngine.kt`
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/focus/domain/metrics/FocusIndexHonestyTest.kt`

### 2026-07-15 — `kotlin.random.Random(seed)` NO es un contrato: pinnear el PRNG si el replay/golden debe sobrevivir a un upgrade de Kotlin; y una semilla wall-clock no persistida hace la exposición incomputable para siempre

**Error detectado:**
Los 13 generadores de Enfoque son puros (`generate(difficulty, spec, seed)`) y hacen `Random(seed)`, pero (a)
`kotlin.random.Random(Long)` devuelve `XorWowRandom` y la stdlib **no garantiza contractualmente** el algoritmo
entre versiones — un golden test o un replay se convierten en secuencias distintas el día que cambie; y (b) la
semilla era `graph.epochClock.nowMillis()` acuñada en un composable (`FocusRootScreen.kt:42,63`), nunca
persistida (`SessionRecord` no tiene campo seed; `grep` de replay/seed-persistence en `mindboost/` = 0 hits).

**Causa raíz:**
Se asumió que "el generador es determinista" ⇒ "la sesión es reproducible". Son cosas distintas: la pureza del
generador es necesaria y NO suficiente. Sin persistir `(seed, version, difficulty, spec)` ninguna sesión real se
puede replicar, auditar ni pinnear; el historial de ítems de toda sesión ya ejecutada es **irrecuperable** (no se
reconstruye después a ningún precio). Además una semilla wall-clock es observable por el usuario (predecible ⇒
superficie de gaming) y dos sesiones en el mismo milisegundo son IDÉNTICAS.

**Solución aplicada:**
`core/runtime/`: `RngVersion` + `SessionSeed(value, version)` (nunca un `Long` desnudo — un `Long` pierde la
versión que le da sentido) + `DeterministicRng` (splitmix64 pinneado, extiende `kotlin.random.Random` para ser
drop-in vía `nextBits`) + `SeedSource`/`PlatformSeedSource` (entropía inyectada, no reloj).
`DeterministicRng.forSeed` es fail-closed por versión. Golden vectors calculados con una referencia BigInt
INDEPENDIENTE (no leídos del propio Kotlin) y anclados al vector canónico publicado `0xE220A8397B1DCDAF`;
mutación de un bit en `MIX_2` ⇒ rojo (probado).

**CORRECCIÓN (2026-07-15, misma fecha) — esta entrada registró un control que NO existía:**
La regla de abajo decía "pinnear también los valores DERIVADOS", pero el test que se escribió NO los pinneaba:
`derived_operations_the_generators_call_are_pinned` sólo afirmaba (a) que los valores caían en rango y (b) que la
misma semilla coincidía CONSIGO MISMA — cero valores esperados — mientras su KDoc afirmaba lo contrario. Una
review adversarial probó el contrafactual: cambiando la derivación de `nextInt(bound)` de la stdlib (rechazo →
módulo plano) los valores CAMBIAN y el test SEGUÍA VERDE. Además `neighbouring_seeds_…` construía el RNG DENTRO
del lambda (`List(16) { rng(SEED).nextLong() }`), así que un lado era el primer draw repetido 16 veces y pasaba
por la razón equivocada. Ambos corregidos: literales pinneados (`nextInt(10)`→`[6,4,8,3,7]`,
`nextDouble()`→`[0.726404219165592, …]`, `nextLong(1000)`→`[0,676,658]`), derivados DOS veces de forma
independiente (referencia BigInt propia + revisor, coincidentes), y mutación probada (derivación cambiada ⇒
ROJO). **Tercera vez que construyo el defecto que acababa de documentar** (ver F-REG-03 y E07/E08/E09 en V2d).

**Regla futura:**
Si un golden test o un replay debe sobrevivir a un upgrade de Kotlin, NO apoyarse en `kotlin.random.Random(seed)`
(algoritmo no contractual): pinnear el PRNG (~40 líneas) y anclarlo con vectores de una referencia INDEPENDIENTE
+ el vector canónico publicado — un golden cuyas expectativas se leen del propio sujeto es una tautología, no un
test. **Un golden que sólo afirma RANGO (`all { it in 0..9 }`) y AUTO-CONSISTENCIA (misma semilla ⇒ mismo valor)
NO pinnea nada: ambas propiedades sobreviven a un cambio de algoritmo.** Un golden pinnea literales o no es un
golden; y la prueba de que lo es se hace mutando lo que dice proteger, no ejecutándolo en verde. Las derivaciones
(`nextInt`/`nextDouble`) vienen de la stdlib: pinnear también los valores DERIVADOS que los generadores llaman,
en su ORDEN EXACTO de llamada (cada llamada consume draws). Escribir la regla en el ledger NO es implementarla:
antes de registrar un control como aplicado, ejecutar su mutación. Una semilla viaja SIEMPRE
con su `RngVersion`. **Entropía ≠ control de exposición**: una semilla impredecible arregla la predictibilidad y
NO hace nada por la exposición de ítems; sólo `(seed, version, difficulty, spec)` persistido + ledger de
exposición la hace computable. Corrección explícita de una afirmación falsa que llegué a escribir: seeds
consecutivos NO producen secuencias parecidas (XorWow y splitmix64 dispersan la semilla) — los defectos reales
del wall-clock son la colisión en el mismo milisegundo y la predictibilidad.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/runtime/{SessionSeed,DeterministicRng}.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/runtime/DeterministicRngGoldenTest.kt`

### 2026-07-14 — Un gate de revocación de lectura debe ser TRIÁDICO (`ACTIVE/REVOKED/STATUS_UNAVAILABLE`), no un booleano: un fail-OPEN es exposición y un fail-CLOSED fabrica ausencia; y la autoridad es ÚNICA sobre un namespace dedicado (separa lectura-de-estado de lectura-de-payload)

**Error detectado:**
SYSTEM-1 6R3 P2-01. Un intento previo gateaba las lecturas de `SecureSessionRecordStore`/`SecureCognitiveSessionRunStore`
con un booleano fail-OPEN (`isDefinitelyRevoked`): cuando el tombstone NO se podía leer (fallo transitorio de backend)
devolvía "no revocado" y PROCEDÍA a leer el payload → EXPOSICIÓN (un sujeto revocado con filas residuales se vuelve
legible cada vez que el read del tombstone falla). El fail-CLOSED opuesto (`isRevoked`) tampoco sirve para lecturas:
convierte un fallo de backend en `Missing` (fabrica ausencia, rompe `null≠failure`). Además el tombstone vivía DENTRO
del namespace de datos de cada store → DOS tombstones independientes (no una autoridad única) y era imposible separar
"falló la lectura del estado de revocación" de "falló la lectura del payload" en un test (un fake que falla todas las
reads no discrimina).

**Causa raíz:**
Un gate de revocación de LECTURA tiene TRES estados, no dos: ACTIVE / REVOKED / STATUS_UNAVAILABLE. Colapsarlo a un
booleano fuerza a elegir entre exposición (fail-open) y ausencia-fabricada (fail-closed); el estado indeterminado
(STATUS_UNAVAILABLE) debe ser un TERCER resultado que NO toca payload y NO se persiste como revocación real. Y un
tombstone por-store en el namespace de datos no puede ser "autoridad única" ni testeable con estado/payload separados.

**Solución aplicada:**
`SubjectRevocationAuthority` (una sola, sobre un namespace DEDICADO `mindboost_revocation`, inyectada y compartida por
ambos stores → UN tombstone por sujeto). `sealed SubjectRevocationState { Active | Revoked | StatusUnavailable(reason) }`.
`guardedRead`/`guardedWrite` corren el check + la operación DENTRO de un `Mutex` por-sujeto (sin TOCTOU); en
REVOKED/STATUS_UNAVAILABLE el bloque de payload NUNCA se invoca. Mapeo por método: accessors CON canal de fallo
(`get:ReadResult`, `findEvidence:SessionRecordRead`) → `Failed(REVOCATION_STATUS_UNAVAILABLE)`/`ReadFailed`; accessors
SIN canal (`find`/`all`/`listRecent`/`listByBlock`) → breadcrumb + default conservador (alimentan recovery/historial, no
el perfil; lanzar crashearía `LaunchedEffect`). Erasure = HOLD-THROUGH-ERASE (`withSubjectLock { markRevoked; borrar }`,
deadlock-free porque el borrado solo llama `store.remove`, nunca re-entra un guarded op). El namespace dedicado permite
inyectar un revocation-store que FALLA mientras el payload-store funciona → T1–T12 discriminantes de verdad.

**Regla futura:**
Un gate de revocación sobre una LECTURA es TRIÁDICO, nunca booleano: ACTIVE ejecuta y devuelve Found/Missing/backend-fault
real; REVOKED no toca payload (SubjectRevoked→Missing en el adapter externo); STATUS_UNAVAILABLE no toca payload y
devuelve `Failed(retryable)` — jamás fabrica ausencia ni expone. Para que sea autoridad ÚNICA y testeable, el tombstone
vive en un namespace DEDICADO (no en el de datos), inyectado y compartido por todos los stores del sujeto; ese namespace
es PERMANENTE (anti-resurrección) y NUNCA se añade a `AccountBound` (test negativo). check+operación en UNA región
serializada (sin TOCTOU); erasure hold-through-erase (o release-before-erase documentado) sin re-entrar el lock. Los
tests separan lectura-de-estado (revocation store) de lectura-de-payload (data store): un fake que falla todas las reads
no discrimina STATUS_UNAVAILABLE de un fallo de payload. `Set-Content -Encoding UTF8` (PowerShell 5.1) añade BOM a los
`.kt` → rompe el `@file:Suppress` de detekt; escribir Kotlin con `UTF8Encoding($false)` (sin BOM).

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/session/SubjectRevocationAuthority.kt` (nuevo; reemplaza `SubjectRevocationGuard`)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/session/{SecureSessionRecordStore,cognitive/SecureCognitiveSessionRunStore}.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/storage/MindBoostStorageNamespaces.kt` (namespace `Revocation`, no en AccountBound)
- `shared/src/commonMain/kotlin/com/mindos/app/features/privacy/application/AccountDeletionCoordinator.kt` + `core/di/AppGraph.kt` + ~18 sitios de wiring (revocationStore inyectado, param requerido)
- `shared/src/desktopTest/.../core/session/{SessionRecordStoreRevocationGapsTest,cognitive/CognitiveSessionRunStoreRevocationGapsTest}.kt` (T1–T12)
- `docs/mindboost_velocidad_implementation_plan/SYSTEM1_SLICE_6R3_REOPEN.md` (§40.7)

### 2026-07-14 — Una escritura NECESARIA (`upsert` write-ahead) nunca puede convertirse en éxito silencioso: outcome TIPADO y observable, `CancellationException` relanzada, "best-effort" solo si contrato + nombre + caller lo declaran

**Error detectado:**
SYSTEM-1 6R3 P2-01. `SecureSessionRecordStore.upsert` devolvía `Unit` y su helper `putCatching` TRAGABA un fallo de
backend genuino (breadcrumb + return). El caller que más importa —`SessionRecoveryScanner.recover()`, que marca
huérfanos `ABANDONED` vía `upsert` desde un `LaunchedEffect` de 6 root-screens— NO podía distinguir "persistí el
ABANDONED" de "el backend falló y no persistí nada": reportaba recovery exitoso aunque la escritura no ocurriera.
(Envolver un `store.put` desnudo alcanzable desde `LaunchedEffect` para que no crashee la pantalla es correcto —Android
`put()` no está internamente try/caught— pero TRAGAR el resultado como `Unit`-éxito NO lo es: son dos cosas distintas.)

**Causa raíz:**
Un guard best-effort que oculta el resultado colapsa dos verdades distintas —"escrito" y "no escrito"— en el mismo
`Unit`. Una escritura write-ahead es la BASE del recovery/sellado; si el caller no recibe si persistió, no puede
reintentar ni degradar honestamente. "Best-effort" no es licencia para mentir: solo es válido si el CONTRATO, el
NOMBRE del método y el CALLER lo declaran explícitamente, y el caller RECIBE y actúa sobre el outcome.

**Solución aplicada:**
`upsert` devuelve un outcome TIPADO (`Stored` / `RejectedRevoked` / `RetryableFailure(reason)`); `CancellationException`
se relanza siempre; el fallo se surfacea por el canal diseñado (retry/telemetría SIN payload), nunca como éxito. Los
callers (recorder write-ahead, `SessionRecoveryScanner`, root-screens, tests) consumen el outcome; el recovery scanner
NO declara recuperado un huérfano si el `upsert` de ABANDONED no fue `Stored`. Un caller que elige best-effort lo
declara en su nombre/contrato y deja el fallo observable, nunca lo convierte en éxito.

**Regla futura:**
Una escritura necesaria (write-ahead / seal / checkpoint) NUNCA devuelve `Unit` tragando el fallo: outcome TIPADO y
observable, `CancellationException` relanzada. Envolver el crash (UI/`LaunchedEffect`) y TRAGAR el resultado son cosas
distintas — envuelve el crash, PROPAGA el outcome. "Best-effort" es válido SOLO si contrato + nombre del método +
caller lo declaran, y el caller recibe y actúa sobre el resultado (retry/telemetría sin payload). Un caller de
recovery/sellado que reporta éxito sin comprobar que la escritura persistió es un falso éxito tan grave como un crash.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/session/{SessionRecordStore,SecureSessionRecordStore,SessionRecorder,SessionRecoveryScanner}.kt`
- `docs/mindboost_velocidad_implementation_plan/SYSTEM1_SLICE_6R3_REOPEN.md` (§40)

### 2026-07-14 — `forEach { remove }` en un borrado "best-effort-total" aborta al primer fallo y deja datos personales atrás (hueco de erasure GDPR); y un default `InMemory*Store` en un holder de producción es una isla de persistencia silenciosa

**Error detectado:**
SYSTEM-1 6R3 R3-FINAL P2-01. (1) `SecureCognitiveSessionRunStore.deleteAllRuns` recorría las claves del sujeto con
`allKeys.forEach { key -> store.remove(key) }` dentro de un `try` único: si un `remove` lanzaba a mitad del bucle,
el `forEach` ABORTABA y las claves restantes NO se intentaban → filas personales del sujeto borrado quedaban en el
namespace COMPARTIDO, y devolvía `Failed`. Su propio KDoc afirmaba "attempts every matched key even if one fails,
surfaces the first failure. Best-effort total, mirroring IndexedSecureKeyValueStore.clearNamespace" — el código NO
lo hacía (divergencia KDoc↔código + hueco GDPR Art. 17). (2) `GoNoGoSessionStateHolder.cognitiveRunStore` tenía un
default `= InMemoryCognitiveSessionRunStore()`: un call-site descuidado persistiría el historial oficial de E8 en un
store en-proceso desechable (perdido al salir) y saltándose el subject-scoping por-cuenta.

**Causa raíz:**
`Collection.forEach` propaga la primera excepción y termina el bucle — semántica incompatible con "intentar TODAS
las claves". El `clearNamespace` real (`IndexedSecureKeyValueStore`) ya lo hacía bien (bucle con `deleteCatching`
que captura el fallo por-clave, relanza `CancellationException`, y solo lanza el PRIMER fallo tras intentar todo);
`deleteAllRuns` decía imitarlo pero usaba el patrón abortante. Y un valor por defecto convierte una dependencia de
persistencia crítica en opcional (el mismo antipatrón de "gate con default que lo desactiva").

**Solución aplicada:**
(1) `deleteAllRuns` ahora es best-effort-total real: bucle `for` con un helper `removeCatching(store, key)` (captura
el fallo por-clave, RELANZA `CancellationException`), acumula solo el `firstFailure`, intenta TODAS las claves, y
devuelve `Cleared` solo si ninguna falló (si no, `Failed`). Espeja exactamente el contrato de `clearNamespace`. (2)
Se quitó el default del holder → `cognitiveRunStore` es obligatorio (el único caller de producción, `E8InhibicionScreen`,
ya inyecta el `SecureCognitiveSessionRunStore(subject=userId)` durable). Tests nuevos (regresión que DISCRIMINA el
arreglo: se falla un `remove` de una PRIMARY key —que precede a los índices en el orden de borrado—, así el bug viejo
dejaría los índices atrás y el arreglo deja solo la clave que falló como único superviviente; + propagación de
`CancellationException` sin tragarla a `Failed`; + runId con el delimitador `:`; + subject Unicode). `desktopTest`
del store 27→31, verde. El lado de purga/marcador/concurrencia (`LegacyNamespacePurge`) YA estaba cubierto
(`aMidPurgeFailure_…retry`, `concurrentCallers_…exactlyOnce`) — no se duplicó.

**Regla futura:**
Un borrado que afirme "best-effort total / intenta todas las claves" NUNCA debe usar `forEach { remove }` (aborta al
primer throw): usa un bucle con un helper por-clave que capture el fallo y RELANCE `CancellationException`, acumule el
primer fallo, intente TODAS las claves y devuelva `Failed` solo tras intentarlas — imita `IndexedSecureKeyValueStore
.clearNamespace`. Una erasure sobre un namespace COMPARTIDO por-sujeto que aborta a mitad es un hueco GDPR (deja datos
del sujeto borrado). Verifica el KDoc contra el código: "mirroring X" debe hacerlo de verdad. Un test de erasure
best-effort debe DISCRIMINAR el arreglo del bug: falla una clave que PRECEDE a otras en el orden de borrado y afirma
que las posteriores SÍ se borraron (si fallas la última, el bug abortante da el mismo resultado y el test no prueba
nada). Un store de persistencia crítico en un holder de producción es dependencia OBLIGATORIA (sin default
`InMemory*`): un default convierte "historial durable + aislado por cuenta" en una isla en-proceso desechable.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/session/cognitive/SecureCognitiveSessionRunStore.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/presentation/e8/GoNoGoSessionStateHolder.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/session/cognitive/{CognitiveSessionRunStoreTest,CognitiveSessionRunStoreTestFakes}.kt`

### 2026-07-13 — Feed home fan-out-on-read (A2): un read-model materializado sin writer es huérfano; seguir a alguien YA es la autorización de lectura (sin reads extra de privacidad/bloqueo); caché por-instancia en vez de colección Firestore

**Error detectado:**
El cliente (`GlobalFeedRepositoryProvider.android.kt:57-72`) lee el feed materializado `feeds/{viewer}/posts`
pero NINGUNA Cloud Function lo escribe: `createPost` solo escribe `posts/`+`user_posts/`, y `onPostDeleted` solo
PURGA `feeds/`. El read-model está SIEMPRE vacío en producción → el feed home no muestra nada (A2·HIGH).

**Causa raíz:**
Se materializó un read-path (`feeds/`) sin decidir el modelo de fan-out: había lector y purgador, pero no writer.
Un read-model materializado sin writer es un path huérfano (lee una colección que nada puebla).

**Solución aplicada:**
DEC-1 = fan-out-on-read: NO poblar `feeds/`; callable backend `loadHomeFeed` (Admin SDK) que computa el feed
on-demand: `follows where followerId==viewer limit 200` → autores (+ self) → chunks `in`≤30 →
`posts where userId in <chunk> orderBy createdAt desc limit PAGE` → merge/dedupe/orden desc/cap (puro, testeado).
Índice compuesto NUEVO `posts (userId ASC, createdAt DESC)` en `firestore.indexes.json` (deploy hand-off). Caché
por-instancia best-effort (`Map` con TTL 45s, cap 500, keyed `viewer:limit`), NO una colección `feed_cache/`.
`feeds/` se conserva como código muerto defensivo (retirar rules+erasure de una colección probadamente sin writer
añade riesgo de review por cero beneficio). Backend gates VERDES (typecheck, 894 jest incl. 11 nuevos,
scan:secrets). Cambio de cliente androidMain (parsear la respuesta del callable con DTOs `@Serializable`+gitlive)
= fase siguiente con spec exacta en el ADR (gate Kotlin hand-off; deploy backend-primero es orden-safe: pre-deploy
→ `DomainResult.failure` → feed vacío = status quo). Doc: `docs/audits/ADR_FEED_FANOUT_2026-07-12.md`.

**Regla futura:**
(1) Antes de LEER una colección materializada (`feeds/`, snapshots, read-models), verifica que exista un WRITER;
una materialización con lector+purgador pero sin writer es huérfana (siempre vacía). (2) En fan-out-on-read, iterar
los FOLLOWEES del viewer ES la autorización: `postPrivacyAllowsRead(author)` (`firestore.rules`) = público OR
`exists(follows/{viewer}_{author})`, así que la rama follow-existe es SIEMPRE true para cada followee (público Y
privado); y `blockUser` borra AMBOS docs `follows`, así que un follow vivo garantiza `noBlockBetween`. ⇒ CERO reads
extra de `privacy/`/`blocks/` — la lista de follows es el conjunto autorizado. Deriva la política del Admin SDK de
las MISMAS reglas, no inventes una nueva. (3) Para una caché de servidor barata (DEC-2, sin Memorystore), prefiere
un `Map` por-instancia con TTL (efímero, cero dato en reposo) a una colección Firestore (`feed_cache/`), que
obligaría a AccountBound+eraser+rules+política TTL+coste de escritura por una caché; nombra el techo de staleness
(≤TTL para reflejar block/privacy). (4) Query `where X in [...] orderBy Y` exige índice compuesto `(X ASC, Y DESC)`
declarado en `firestore.indexes.json` + deploy `firebase deploy --only firestore:indexes` (hand-off); hasta el
deploy, la query da `FAILED_PRECONDITION`. (5) `in` de Firestore admite ≤30 valores (chunkea).

**Archivos afectados:**
- `functions/src/homeFeed.ts` (nuevo) · `functions/src/homeFeed.test.ts` (nuevo) · `functions/src/index.ts` (export)
- `firestore.indexes.json` (índice `posts` userId+createdAt) · `docs/audits/ADR_FEED_FANOUT_2026-07-12.md` (nuevo)

### 2026-07-13 — Borrado de cuenta a escala (A3): paginar con reconsulta-tras-mutación (sin cursor persistido), job Cloud Tasks v1, y la idempotencia por-paso ES la reanudación

**Error detectado:**
`accountDeletion.ts` cargaba el set COMPLETO de cada query de borrado en memoria de una sola invocación:
`deleteByQuery`/`updateQueryDocs`/`moveIdempotencyDocs` hacían `query.get()` sin `.limit()`, y el purge corría
INLINE dentro de la callable `deleteAccount` (timeout 60s). Una cuenta con muchos docs (posts/visits/notifications)
→ OOM o timeout de la callable → erasure GDPR atascada (finding A3·HIGH).

**Causa raíz:**
`query.get()` materializa TODO el resultado; una callable `onCall` tiene timeout corto y NO reintenta. Ambos
límites hacen imposible completar un borrado grande síncrono. Además el mock jest de `firebase-admin` solo cubría
`where(...).get()` (sin `.limit()`), así que añadir paginación rompía toda la suite hasta extender el mock
(mismo patrón que el learning TTL 2026-07-12: una API nueva del SDK exige extender el stub).

**Solución aplicada:**
(1) Paginación por `.limit(DELETE_BATCH_SIZE)` en bucle, con **reconsulta-tras-mutación en vez de cursor
`startAfter`**: como el delete borra el doc y el update reescribe el campo del `where` (walletId→pseudónimo), los
docs procesados SALEN del filtro y la siguiente página con el mismo `.limit()` devuelve los siguientes — sin
cursor que persistir. `updateQueryDocs` lleva un guard `pending === 0 → break` (defensa anti-hang si un doc
matchea pero no tiene campo pseudonimizable). (2) Job de fondo Cloud Tasks v1: worker
`processAccountDeletion = functions.runWith({timeoutSeconds:540, memory:'512MB'}).tasks.taskQueue({retryConfig,
rateLimits}).onDispatch(...)`; la callable `deleteAccount` ahora **encola** vía
`getFunctions().taskQueue('processAccountDeletion').enqueue({uid, correlationId})` en vez de purgar inline (NO
migrar a v2 — firebase-functions 5.1.1/admin 12.7.0, learning 2026-06-19). (3) **La idempotencia por-paso ES la
reanudación**: cada `markStep` es el checkpoint; un reintento de Cloud Tasks re-ejecuta `executeAccountDeletion`
que, por reconsulta-vacía, salta lo ya hecho y completa con residuo cero — sin persistir un cursor intra-query
por lote (más simple, mismo resultado). El erasure sigue fail-closed (Auth se borra LAST). La callable valida el
secreto de pseudónimo (fail-closed) ANTES de encolar. Mock jest extendido: `where()`/`collection()` devuelven un
query encadenable con `.limit()`; `firebase-admin/functions` mockea el `enqueue`; `firebase-functions` mockea
`runWith().tasks.taskQueue().onDispatch()`.

**Regla futura:**
Para borrar/actualizar en masa en Cloud Functions, NUNCA `query.get()` sin `.limit()`: paginar. Si la mutación
saca el doc del filtro `where` (delete, o update del propio campo filtrado), usar reconsulta-tras-mutación (bucle
`.limit(N).get()` hasta `< N`) — no hace falta cursor `startAfter` ni persistirlo por lote; añadir un guard
anti-hang si algún doc puede matchear sin mutarse. Un borrado que no cabe en una invocación va a un worker Cloud
Tasks (v1: `functions.tasks.taskQueue().onDispatch`, encolar con `getFunctions().taskQueue().enqueue`) con
`timeoutSeconds` elevado; la reanudación viene del retry de Cloud Tasks + idempotencia por-paso (los `markStep`
son los checkpoints), no de un cursor persistido. La callable que encola debe fail-closed ANTES de encolar (no
reportar éxito de un job que el worker no podrá completar) y propagar el fallo de enqueue. Deploy = hand-off
founder: habilitar `cloudtasks.googleapis.com` + `roles/cloudtasks.enqueuer` (+ `cloudfunctions.invoker`) al SA
de Functions. Al añadir `.limit()` (u otra API del SDK) a queries con tests, extender el stub de `firebase-admin`
en el mock ANTES de correr jest. OJO deriva de contrato: pasar el purge a asíncrono hace que la callable retorne
antes de borrar el Auth — el cliente KMP (`FirebaseSessionRepository.kt:102` "Auth is now gone server-side")
asume borrado síncrono; el shape de retorno no cambia (no rompe), pero el borrado servidor es ahora eventual
(fail-closed en el worker) — declarar como follow-up cliente.

**Archivos afectados:**
- `functions/src/accountDeletion.ts`
- `functions/src/accountDeletion.test.ts`
- `functions/src/index.ts`

### 2026-07-12 — Contador de seguidores caliente (A1): shardear SOLO los campos con fan-in; read-model por trigger; reads-before-writes en la tx; borrar shards antes del padre; la siembra transaccional serializa un thundering-herd de primeros-escritores

**Error detectado:**
`socialGraph.ts` mutaba `counters/{uid}` con read-then-write (`tx.get(counter)` + `tx.set(old±1)`) en cada
follow/request/block. Una cuenta con fan-in viral (>~1 escritura/s a ese doc) excede el techo de Firestore
(~1 escritura sostenida/s/doc) → las transacciones ABORTAN y los follows fallan (auditoría A1·CRITICAL).

**Causa raíz:**
Un contador con fan-in de terceros (muchos usuarios escriben el MISMO doc víctima) sobre un único documento no
escala; y el patrón read-then-write en transacción añade contención de lock óptimista (ABORTED) además del
techo de throughput.

**Solución aplicada:**
Shardear SOLO los campos con fan-in concurrente de terceros (`followersCount`, `incomingPendingRequestsCount`)
a `counters/{uid}/shards/{0..N-1}` con `FieldValue.increment` (nunca read-then-write); los campos single-writer
(`followingCount`/`outgoingPendingRequestsCount`/`blockedUsersCount`, escritos solo por el propio dueño) siguen
en el padre con `increment` atómico (su piso `>=0` lo garantiza la idempotencia ya existente, no un `Math.max`).
El read-model del cliente `counters/{uid}` NO cambia (reglas de lectura intactas): lo materializa un trigger
`aggregateCounterShards` (onWrite de la subcolección) que re-suma los shards con coerción `>=0`. Backfill
transaccional D-02: la primera escritura de un uid siembra `shards/0` con el legacy + marca `migratedToShards`
en `shards/_meta`; los primeros-escritores concurrentes que ven el marker incrementan en vez de re-sembrar.

**Regla futura:**
(1) Shardea SOLO los campos con fan-in de terceros; los single-writer se quedan en el padre con `increment`
(shardearlos sería overhead). (2) Con read-then-write eliminado, el piso `>=0` de los single-writer lo da la
idempotencia (el decremento solo corre si la relación existía); la coerción `>=0` explícita va en la lectura
AGREGADA de shards (trigger/`sumShards`), que `FieldValue.increment` no clampa. (3) Reads-before-writes de
Firestore: la lectura de migración (marker + legacy condicional) DEBE preceder a todo `tx.set`; separa el
cómputo de deltas (puro, desde los snapshots) de las escrituras. (4) El marker vive en un doc dedicado
(`_meta`), nunca en un shard que reciba increments (leerlo en cada tx sería read-then-write sobre un doc
caliente). (5) GDPR: borra la subcolección `shards` ANTES del doc padre y haz que el trigger sea no-op con
shard-set vacío, para que un trigger tardío no resucite el contador borrado (test de residuo cero). (6) TECHO
declarado: la siembra transaccional (D-02) garantiza siembra única pero SERIALIZA un thundering-herd de
primeros-escritores simultáneos sobre un uid nunca migrado (ventana de migración; en el emulador dispara
`ABORTED: Transaction lock timeout` a ~100 concurrentes) — coste transitorio de una vez por uid; mitigación =
backfill eager (plan §9) y/o subir N. La contención real (~1 write/s/doc) NO es reproducible en el emulador (se
prueba con el load-test §5 en staging, hand-off founder); el emulador SÍ verifica suma exacta + siembra única.

**Archivos afectados:**
- `functions/src/lib/counterShards.ts` (nuevo) · `functions/src/socialGraph.ts` (8 handlers + trigger)
- `functions/src/accountDeletion.ts` (borra shards antes del padre) · `functions/src/index.ts`
- `firestore.rules` (subcolección `shards` deny-all) · `firestore.security.rules.test.mjs` (T27b/T27c)
- `functions/src/socialGraph.test.ts` (nuevo) · `functions/src/accountDeletion.test.ts` (residuo cero) · `functions/src/security.test.ts` (asserts al modelo shardeado)
- `functions/scripts/counter-shards.concurrency.emulator.mjs` (nuevo) · `package.json` (`test:counter-shards-emulator`)

### 2026-07-12 — La política TTL de Firestore exige un campo Timestamp real (un NUMBER de milis no la dispara); y los mocks jest de `firebase-admin` sin `Timestamp` rompen al añadir `Timestamp.fromMillis`

**Error detectado:**
S12a (higiene backend, D-07). Los buckets de `rate_limits` (`rateLimiter.ts`) y los docs de `profile_visits`
(`profileVisits.ts`) persistían timestamps como NUMBER (`windowStart`/`updatedAt`, `firstVisitedAt`/`lastVisitedAt`)
y ningún campo con el que activar una política TTL → ambas colecciones crecen sin cota (coste Firestore no acotado,
findings A7/A9). Al añadir `expireAt` con `admin.firestore.Timestamp.fromMillis(...)`, los tests `ip-rate-limit.test.ts`
y `security.test.ts` —que ejecutan el `tx.set` real dentro de su `runTransaction` mockeado— habrían fallado con
TypeError: sus mocks de `firebase-admin` exponían `firestore()`/`FieldValue` pero NO `firestore.Timestamp`, así que
`admin.firestore.Timestamp` era `undefined` y `.fromMillis` reventaba.

**Causa raíz:**
La política TTL de Firestore borra un doc cuando el campo configurado es un Timestamp en el pasado; un NUMBER de
epoch-millis NO es un tipo Timestamp y la política lo ignora. Por eso el campo TTL debe ser
`admin.firestore.Timestamp`, no reutilizar los NUMBER existentes ni `FieldValue.serverTimestamp()` (sentinel, no sirve
para una fecha futura). Y un mock jest de `firebase-admin` solo cubre las rutas que existían: al introducir una API
nueva del SDK (`Timestamp.fromMillis`) hay que extender el mock, o el primer test que alcance el `tx.set` falla.

**Solución aplicada:**
`expireAt: Timestamp` en los tres writes (2 `tx.set` de rateLimiter — ventana `windowStart + windowMs*2`; `tx.set` +
`tx.update` de profileVisits — 30d deslizantes desde la última visita), vía helpers documentados
(`bucketExpireAt`/`visitExpireAt`). Stub `Timestamp = { fromMillis: (ms) => ({ __timestampMs: ms }) }` añadido a los
mocks de `ip-rate-limit.test.ts` y `security.test.ts` (`wallet-rate-limit.test.ts` no se toca: sus mocks devuelven
antes de alcanzar `tx.set`). Test nuevo que fija que el bucket escribe `expireAt` = `windowStart + windowMs*2`.
Activación de la política es hand-off founder (`gcloud firestore fields ttls update expireAt --collection-group=...`),
no code. Gates backend verdes ejecutados (typecheck, 866/866 jest, scan:secrets).

**Regla futura:**
Para una política TTL de Firestore, el campo debe ser un `admin.firestore.Timestamp` real (NUMBER de millis y
`serverTimestamp()` NO la disparan); construirlo con `Timestamp.fromMillis(futuroMs)` y activar la política
out-of-band con `gcloud firestore fields ttls update <campo> --collection-group=<col> --enable-ttl`. Al introducir una
API del SDK no usada antes (`Timestamp.fromMillis`) en código con tests que ejecutan el callback de `runTransaction`,
extender el stub de `firebase-admin` en CADA test que alcance ese write (los que devuelven antes del `tx.set` no lo
necesitan). Añadir el campo TTL a colecciones de higiene es aditivo y no altera aserciones que usan `toMatchObject`
sobre el resultado, pero SÍ rompe cualquier mock incompleto del SDK.

**Archivos afectados:**
- `functions/src/lib/rateLimiter.ts`
- `functions/src/profileVisits.ts`
- `functions/src/ip-rate-limit.test.ts`
- `functions/src/security.test.ts`

### 2026-07-11 — Una clave de infraestructura reservada nueva en un namespace COMPARTIDO existente es invisible a las APIs de dominio (prefijo estrecho) pero visible a un dump crudo `.keys("")`; hay que grepear TODOS los source-sets de test antes de asumir que es segura

**Error detectado:**
SYSTEM-1 Slice 6-R3 §18 C1 (purga de claves legacy sin sujeto). Al diseñar `LegacyNamespacePurge` (marker
reservado `legacy_purge_marker:v1` escrito en el MISMO `SecureKeyValueStore` que `SessionRecordStore`/
`CognitiveSessionRunStore`), un test PRE-EXISTENTE (`CognitiveSessionRunStoreTest.deleteAll clears primaries and
indices`) hacía `assertTrue(kv.keys("").isEmpty())` justo después de `store.deleteAll()`. El marker sobrevive
DELIBERADAMENTE a un `deleteAll()` por-sujeto (es infraestructura de dispositivo, no una fila de usuario), así
que ese test se habría roto en silencio si no se hubiera grepeado el patrón `\.keys\(""\)` a través de TODO el
árbol de tests (no solo los ficheros que un cambio "obviamente" toca).

**Causa raíz:**
Una clave reservada nueva en un namespace ya compartido por `SessionRecordStore`/`CognitiveSessionRunStore` es
**invisible** a las APIs de dominio (`find`/`all`/`get`/`listRecent`/`listByBlock` escanean con un prefijo
ESTRECHO por-sujeto, p. ej. `session_record:$subject:`, que la clave reservada nunca matchea) pero **visible** a
cualquier test que haga un dump crudo del backing store con `.keys("")` (prefijo vacío = "todo el namespace").
Los tests que ya usaban un prefijo específico (`.keys("session_record:$subject:")`) o solo verificaban
sustrings de VALORES (no de claves) resultaron ilesos; solo los que dependían de "el namespace entero está
vacío/tiene N claves" se rompían.

**Solución aplicada:**
(1) Grep exhaustivo de `\.keys\(""\)` en TODO `shared/src` (los 4 source-sets) antes de dar el diseño por
seguro — reveló ~15 sitios; se analizó cada uno contra el flujo REAL de llamadas (¿se invoca un método
"gateado" antes del dump? ¿la aserción es de conteo exacto o de ausencia de un token?). (2) El valor persistido
del marker se diseñó como un ENTERO PLANO (`"1"`, sin JSON ni nombres de campo) precisamente para minimizar el
riesgo de colisión con los checks `rawBlobs.none { it.contains(token) }` de otros tests (un token de 1-2
caracteres numéricos no puede casar con un id/palabra real). (3) El ÚNICO test que de verdad se rompía se
corrigió para afirmar la invariante REAL ("ninguna clave del sujeto borrado permanece") en vez de la más débil
"el namespace entero está vacío".

**Regla futura:**
Antes de añadir una clave reservada/de infraestructura a un `SecureKeyValueStore` namespace YA compartido por
otros repos/stores, grepear `\.keys\(""\)` (y variantes tipo `.keys(prefix = "")`) en LOS CUATRO source-sets
(`commonTest`/`desktopTest`/`androidTest`/`iosTest`), no solo en los ficheros de test del store que se edita —
un dump crudo de un test de OTRO feature (wiring tests que inspeccionan `sessionsBacking.keys("")` para probar
minimización de contenido) puede verse afectado igual. Diseñar el VALOR persistido de la clave reservada para
que sea estructuralmente imposible de confundir con un token de test real (un entero plano, no JSON con nombres
de campo en inglés). Preferir que la aserción rota se corrija a la invariante REAL que el test quería probar
(p. ej. "ninguna clave del sujeto borrado permanece"), no a una versión debilitada que solo esconda el síntoma.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/storage/LegacyNamespacePurge.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/session/SecureSessionRecordStore.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/session/cognitive/SecureCognitiveSessionRunStore.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/session/cognitive/CognitiveSessionRunStoreTest.kt`
- `docs/mindboost_velocidad_implementation_plan/LEGACY_KEY_MIGRATION.md`

### 2026-07-10 — Un tipo de salida con la MISMA forma que la moneda del agregador es un riesgo mecánico de wiring aunque nadie lo cablee; la barrera debe ser frontera de dependencia + scanner de build, no un test borrable ni "nadie escribió el mapper"

**Error detectado:**
Reapertura adversarial de SYSTEM-1 Slice 6-R (ingestor de `CognitiveSessionRun`). El contrato "puro y no
cableado" emitía un `ProfileEstimate(currentEstimate, stabilityHeuristic, sessionProgress)` cuya forma es 1:1 la
tripleta `ModuleContribution(normalizedScore, confidence, dataSufficiency)` que el `MindBoostProfileAggregator`
consume; el propio test de la slice ya hacía ese fold y demostraba que 4 sesiones estables ATRAVIESAN el gate del
agregador (`minSufficiency 0.5`/`minConfidence 0.4`). La única "barrera" era que nadie había escrito el mapper +
`preliminary=true` (metadato) + los umbrales del agregador. Además: (a) seleccionaba la escala por RECENCIA
(`maxWith(orderKey,sessionId)`), así una versión vieja con timestamp fresco DESPLAZABA la serie válida; (b)
confiaba en los booleanos derivados forjables `profileEligible`/`calibrationEligible`/`rankingEligible` (un
`data class` público) para timebase/anti-gaming; (c) `ingest(blockId, runs: List<CognitiveSessionRun>, nowMs)`
aceptaba una lista desnuda con "un solo usuario" solo como precondición escrita; (d) llamaba "calibración
ESTABLISHED" a un conteo de repeticiones de UN ejercicio; (e) EWMA/avg-5 eran fórmulas de un PRD superado sin
modelo de error/equating/práctica.

**Causa raíz:**
Un tipo cuya FORMA coincide con lo que el agregador consume es cableable accidentalmente por cualquier slice
futuro sin violar ningún tipo — "no wired" es un estado del árbol, no una barrera. Confiar en booleanos derivados
que un fixture/DTO/hotfix puede construir a `true` es un bypass vivo. Seleccionar por recencia trata "más nuevo"
como "autoritativo". Un conteo de repeticiones no es calibración psicométrica.

**Solución aplicada:**
Reescritura EVIDENCE-ONLY reubicada a `core.session.cognitive.evidence` (donde importar `profile.*` es la
dirección de dependencia PROHIBIDA): salida `CognitiveSessionEvidence` = `ExerciseScaleEvidence` POR lineage
(todas las escalas preservadas, sin descarte por recencia) con SOLO conteos/banda-de-cantidad/scores crudos
descriptivos — CERO campo con forma de `normalizedScore`/`confidence`/`dataSufficiency`, CERO etiqueta de
dimensión. Se BORRARON `ProfileScorePolicy` (EWMA/avg-5), `PerformanceTrendPolicy`, `ProfileEstimate`,
`CalibrationState` (nuisance atractivo, no shadow). Barrera MECÁNICA doble: `CognitiveSessionEvidenceBoundaryTest`
(word-boundary, comentarios incluidos) + tarea Gradle `evidenceBoundaryScanner` cableada a `check` (su borrado es
un diff de build revisable, no un test borrable). Elegibilidad RE-DERIVADA de hechos crudos (nunca los 3
booleanos), timebase re-derivado de `clockSource` para bloques RT-críticos. `SubjectScopedSessionBatch` (ctor
privado + `of()`) hace imposible pasar un `List` desnudo. `EvaluationAsOf` tipado; futuros excluidos;
tiempo-intrínsecamente-inconsistente en cuarentena permanente. `EvidenceQuantityMaturity` (nunca "calibrated"),
day-cap + span-floor de 48h consistentes (grinding no alcanza la banda alta). Rank sigue fail-closed por invariante
de tipo. Conservación en el audit (`init`). W1–W5 (sellar `SessionEligibility`, purga en `signOut`, decisión
producto `TRAINING_FREE`, registro exercise∈block, `CalibrationBlueprint`+equating) DECLARADAS como precondiciones
de wiring, NO resueltas en silencio. 4/4 revisores frescos APTO; gates verdes (iOS NOT VERIFIED).

**Regla futura:**
Un contrato "puro no cableado" que alimentará perfil/rango NO puede emitir un tipo con la forma de la moneda del
consumidor (aggregator/`ModuleContribution`): la barrera es (1) ubicarlo donde importar los tipos de
perfil/rango sea la dirección prohibida, y (2) un SCANNER DE BUILD (tarea Gradle cableada a `check`, espejo de
`mindboostCopySafetyScanner`) que falle si reaparece el import o el símbolo — un test es defensa-en-profundidad,
NO la barrera (es borrable sin diff de build). El scanner (word-boundary, lee comentarios) marca TU PROPIO KDoc
que nombra el símbolo prohibido aunque sea para NEGARLO (anti-negación, como el copy-scanner): reformula sin el
literal. NUNCA confíes en booleanos de elegibilidad derivados y forjables (`data class` público) para gatear;
RE-DERIVA de hechos crudos y del `clockSource`; si un hecho (anti-gaming) no está en el registro, no lo afirmes.
No selecciones "la escala/serie autoritativa" por recencia: preserva todas y marca `NON_COMPARABLE` cross-escala.
Un conteo de repeticiones es "cantidad de evidencia" (day-capped + span-floor), NUNCA "calibración". Borra las
fórmulas de un plan superado (EWMA/avg-5) en vez de dejarlas como shadow. Toda precondición de wiring que aún no
existe (blueprint/equating/sellado/purga) se declara con owner y barrera fail-closed, no se cablea a medias.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/session/cognitive/evidence/**` (9 nuevos)
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/session/cognitive/evidence/**` (7 nuevos)
- `shared/build.gradle.kts` (tarea `evidenceBoundaryScanner` → `check`)
- BORRADO: `shared/src/{commonMain,desktopTest}/kotlin/.../profile/domain/ingestion/**`
- `docs/mindboost_velocidad_implementation_plan/SYSTEM1_SLICE_6R2_REOPEN.md`

### 2026-07-08 — Un gate de calidad con `diagnostics = null` por defecto es bypaseable; el cap binario y el input perdido invisible ocultan medición no fiable

**Error detectado:**
Auditoría R3 de E8 (Velocidad, Go/No-Go). Cuatro grietas latentes de honestidad de medición: (1)
`GoNoGoScorer.score(metrics, diagnostics = null)` — el gate de calidad de datos dependía de que el caller pasara
diagnostics (disciplina humana); un caller futuro podía puntuar saltándose el gate. (2) El cap de integridad era
BINARIO (`impossiblyFastTrials > 0 ⇒ ×0.30`): un único evento de plataforma anulaba toda la sesión. (3) Un tap sin
onset capturado se DESCARTABA en silencio en la pantalla (input perdido invisible). (4) El summary persistía solo
RATES, no el resultado scored ni los conteos crudos ni la distribución RT ⇒ la doc afirmaba "reconstruible" y era
FALSO. Además "E8 no es isla" era ENGAÑOSO: el `OfficialExerciseRunAdapter` no tenía consumidor (ni historial ni
perfil lo leen).

**Causa raíz:**
Un valor por defecto (`= null`) convierte un gate de seguridad en opt-in. Un cap binario confunde "un artefacto"
con "medición sistemáticamente no fiable". Descartar un input sin contarlo oculta pérdida de datos. Persistir un
proyectado lossy (rates) e igual afirmar reconstructibilidad es un overclaim. Y un adapter "oficial" SIN wiring a
un store/perfil no es integración: sigue siendo isla (parcial).

**Solución aplicada:**
(1) `score(metrics, diagnostics)` — `diagnostics` OBLIGATORIO: el gate es garantía de COMPILACIÓN, no de disciplina
(no test hace falta; el tipo lo enforcea). (2) `TimingIntegrityLevel` escalonado CLEAN/MINOR_ANOMALIES(penalización
proporcional, piso 0.50)/COMPROMISED(×0.30 + baja confianza + inelegible), umbral `violations/total ≤ 0.10`. (3)
`missingOnsetTaps` contado por el holder (`onStimulusTap(rtMs: Long?)`, null = perdido), alimenta diagnostics →
integridad → elegibilidad → `anomalyFlags`; nunca fabrica RT=0. (4) El summary persiste conteos crudos +
distribución RT completa + resultado scored + elegibilidad por capa (`profileEligible`/`calibrationEligible`/
`rankingEligible`); `contributesToOfficialProfile = profileEligible && PROFILE_WIRING_ENABLED(false)` con criterio
exacto documentado para el flip. Contrato sistémico reutilizable en
`docs/mindboost_exercise_design/EXERCISE_QUALITY_CONTRACT.md` (4 niveles de readiness L1–L4; deuda transversal §21).

**Regla futura:**
Un gate de integridad/calidad NUNCA lleva un default que lo desactive (`= null`/`= true`): hazlo un parámetro
OBLIGATORIO para que el compilador lo enforce. Un cap de calidad debe ser ESCALONADO (proporcional para anomalías
aisladas, duro+inelegible para sistemáticas), no binario. Un input medido pero no temporizable se CUENTA (métrica
+ flag), jamás se descarta en silencio ni se fabrica un centinela. Persistir "rates" y afirmar "reconstruible" es
overclaim: persiste el resultado scored + conteos crudos + distribución completa, o no lo afirmes. "No es isla"
exige un CONSUMIDOR real del run/adapter (historial/perfil), no solo que el adapter exista; si no hay consumidor,
es PARCIAL. Nunca colapsar "jugable localmente" (L1) con "listo para MVP" (L3): son niveles distintos con gates
distintos (ver Exercise Quality Contract). La deuda transversal (Firestore, perfil, lifecycle, process-death,
analytics, historial unificado `CognitiveSessionRun`) que bloquea requisitos del PRD se declara BLOQUEANTE con
owner, no se esconde como backlog genérico.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/score/{GoNoGoScore,GoNoGoQualityDiagnostics,GoNoGoTelemetrySummaryV1,GoNoGoOfficialRunAdapter}.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/presentation/e8/{GoNoGoSessionStateHolder,E8InhibicionScreen}.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/speed/**`
- `docs/mindboost_exercise_design/EXERCISE_QUALITY_CONTRACT.md` · `docs/mindboost_velocidad_implementation_plan/MC9D_R3_E8_BRUTAL_REVIEW.md`

### 2026-07-08 — La fórmula de confianza CL del plan de scoring podía dar negativo / dividir por cero (CV no acotado)

**Error detectado:**
El "Scoring & Measurement Master Plan" (untracked) definía el nivel de confianza inter-sesión como
`CL = (1 − SD/Mean)·(1 − e^(−0.15·n))`. Con `CV = SD/Mean`, el término `(1 − CV)` se vuelve NEGATIVO cuando
`CV > 1` (dispersión alta con media baja — habitual en scores normalizados `[0,1]`), y `Mean = 0` divide por
cero. Un CL negativo o NaN alimentando `confidence` habría corrompido la agregación del perfil.

**Causa raíz:**
La fórmula asumía `CV ∈ [0,1]` y `Mean > 0`, pero ninguno está garantizado: el coeficiente de variación no
está acotado superiormente y la media de una ventana de scores puede ser 0 (todo ceros). Además el plan
mezclaba conceptualmente "confianza" (estabilidad) con "suficiencia de muestra" — que en este repo son
campos ORTOGONALES (`ModuleContribution.dataSufficiency` vs `confidence`, learning 2026-06-28).

**Solución aplicada:**
Función pura `InterSessionConfidence.interSessionConfidence(recentScores)` en
`core/scoring/normalization/`: guard `n<2 ⇒ 0.0` (dispersión indefinida; honesto, no fabricado), guard
`mean≤0 ⇒ 0.0` (fail-closed), `dispersionTerm=(1−cv).coerceIn(0,1)` (CV>1 ⇒ 0), `sampleTerm=1−e^(−0.15n)`,
`CL=(dispersion·sample).coerceIn(0,1)`. SD muestral (n−1) con guard n≥2. **NO cableada a producción** (el
cableado a `confidence` es Fase C: reclasificaría perfiles existentes). Extremos cubiertos por test
(n=0⇒0; CV>1⇒0; CV=0 & n grande⇒→1). Limitación documentada: CV es sensible a la escala (media pequeña ⇒
penaliza a un usuario bajo-pero-estable); evaluar dispersión normalizada-por-escala en Fase C.

**Regla futura:**
Una fórmula de confianza/estabilidad NUNCA debe poder salir de `[0,1]`: clampar el resultado Y guardar sus
entradas (CV no acotado ⇒ `(1−cv).coerceIn(0,1)`; media 0 ⇒ fail-closed a 0.0, no división). "Sin datos
suficientes" ⇒ `0.0` SOLO donde 0.0 es el valor honesto (n<2), nunca un valor fabricado que finja certeza
(`null != 0`). Confianza (estabilidad) y suficiencia (tamaño de muestra) son ORTOGONALES: jamás derivar una
de la otra. Al reconciliar un plan externo contra el repo (FASE-0), VERIFICAR cada símbolo antes de tocar
código: p.ej. el plan quería añadir `scoringVersion`/`anomalyFlags` a `SessionRecord`, pero su diseño los
excluye a propósito (la versión vive en `ScoreProvenance.scoringVersion`, las anomalías en
`DataQuality.invalidatedBy`) — NO se modifica `SessionRecord`.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/normalization/{ScoreNormalization,InterSessionConfidence}.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/scoring/normalization/{ScoreNormalizationTest,InterSessionConfidenceTest}.kt`
- `docs/mindboost_scoring/MASTER_PLAN_RECONCILED.md`

### 2026-07-06 — Dos hooks de Graphify parseaban stdin con `python3` (stub de Microsoft Store) → no-op silencioso en Windows

**Error detectado:**
Los dos hooks `PreToolUse` de recordatorio Graphify en `.claude/settings.json` (matcher `Bash` para grep/find
y matcher `Read|Glob` para lectura de fuente) parseaban el JSON de stdin con `python3 -c "…"`. En esta máquina
`python3` resuelve a `C:\Users\curro\AppData\Local\Microsoft\WindowsApps\python3` (alias stub de Microsoft
Store): ejecutarlo imprime "no se encontró Python… instalar desde Microsoft Store" y sale con código 49 sin
emitir nada. Resultado: `CMD`/`HIT` quedaban vacíos, el `case`/`if` nunca casaba y el recordatorio Graphify
NUNCA se inyectaba, pese a que `graphify-out/graph.json` existe (la puerta de archivo sí pasaba). La
automatización "existía" pero era un no-op silencioso (confirmado: `printf '{…}' | python3 -c …` → stdout vacío,
exit 49; `node` sí funciona, v24).

**Causa raíz:**
Un hook cuyo cuerpo depende de `python3` (o de sintaxis bash-only como `$(...)`/`case … esac`) no es portable a
este entorno Windows/PowerShell: `python3` es el stub de la Store y el shell del hook no garantiza sh. El guard
Node del repo (`mindos-guard.cjs`) ya demostraba el patrón correcto —`node -e "require(CLAUDE_PROJECT_DIR/…)"`,
que corre igual en pwsh/cmd/bash y en worktrees—; los dos hooks rotos no lo seguían.

**Solución aplicada:**
Se plegó la lógica de ambos recordatorios DENTRO de `mindos-guard.cjs` (parseo de stdin con Node, misma puerta
`fs.existsSync('graphify-out/graph.json')`, mensaje corto, no bloqueante, `exit 0`); en `settings.json` los
cuatro matchers (`Write|Edit|MultiEdit`, `Read|Glob`, `Bash` Pre y `Bash` Post) apuntan a la MISMA invocación
self-locating `node -e`. Cero dependencia de `python3`. Semántica advisory intacta (inyecta `additionalContext`,
nunca bloquea). Verificado en vivo: un Read de un `.md` disparó el recordatorio. De paso se reconciliaron los
permisos Gradle duplicados `Bash(./gradlew …)` a la forma canónica PowerShell `.\gradlew.bat` del entorno, y se
añadieron permisos read-only `git diff`/`git log`.

**Regla futura:**
Todo hook de Claude Code en esta máquina DEBE implementarse como Node self-locating
(`node -e "…require(process.env.CLAUDE_PROJECT_DIR||process.cwd()/.claude/hooks/…)"`), NUNCA con `python3`
(stub de la Store) ni sintaxis bash-only (`$(...)`, `case`), y NUNCA bloquear (`exit 0`, solo avisar). Antes de
dar por buena una automatización de hook en Windows, PROBAR su parseo real (`printf '{…}' | <cmd>`): un hook que
"existe" pero no emite es un no-op silencioso. Preferir consolidar la lógica en `mindos-guard.cjs` (un patrón, un
archivo) a dispersarla en comandos inline por matcher. Los permisos Gradle van en forma PowerShell canónica
(`.\gradlew.bat`), no duplicados Bash/PowerShell.

**Archivos afectados:**
- `.claude/settings.json`
- `.claude/hooks/mindos-guard.cjs`
- `docs/tooling/CLAUDE_CODE_SETUP_AUDIT.md`

### 2026-07-05 — Validación de banco en build-time y gate de integridad en runtime DESCONECTADOS: un literal `BankIntegrity` hardcodeado es un bypass del gate de contenido

**Error detectado:**
INFRA-04 dejó DOS mitades sin conectar: (1) `BankValidator` valida bancos en build-time (tamaño/holdout/chi²
de posición) vía `BankValidationGateTest`, pero (2) el gate de runtime `ScoreStateMachine` decide por el enum
`BankIntegrity` que cada adapter fija a mano. NO existía mapping `BankValidationResult → BankIntegrity`. Las
tres fuentes de Memoria (`MemorySpanScore`/`MemoryPairsScore`/`MemoryDeferredScore` → sus adapters) inyectaban
`BankIntegrity.PROCEDURAL` (no-bloqueante) COMO LITERAL, sin banco registrado ni validado. En producción
(`RememberBrainProfileStateHolder` pasa stores reales) esto alimentaba WORKING_MEMORY/EPISODIC_MEMORY a
EMERGING con señal cuyo contenido NUNCA pasó por el validador — integrity theatre: el gate "existía" pero era
puenteable escribiendo el enum bonito.

**Causa raíz:**
Un valor de integridad HARDCODEADO en el call-site permite afirmar "banco procedural válido" sin que exista tal
banco. La autoridad de integridad debe DERIVARSE de la misma validación que corre el gate de build, no elegirse
por literal. Build-gate y runtime-gate deben compartir una única fuente de verdad o divergen en silencio.

**Solución aplicada:**
`BankIntegrityAuthority.integrityFor(BankRef)` como ÚNICA autoridad: banco ausente en el registro →
`SMALL_UNVALIDATED`; presente pero `!BankValidator.validate().isValid` → `SMALL_UNVALIDATED`; válido +
`BankKind.STATIC_FIXED` → `OK`; válido + `ALGORITHMIC_GENERATOR` → `PROCEDURAL`. Los adapters/scorers pierden
el literal y reciben `bankRef: BankRef` (default `BankRef(<Adapter>.BANK_ID)`); `itemBankIntegrity =
BankIntegrityAuthority.integrityFor(bankRef)`. Con el registro `ProfileUpdatingBanks.all` VACÍO (opt-in), las
fuentes de Memoria caen fail-closed a `SMALL_UNVALIDATED` → `blocksProfileEligibility()` → `BANK_CONTAMINATED`
→ contribución null → WORKING_MEMORY/EPISODIC_MEMORY CALIBRATING (downgrade aceptado por CEO como consecuencia
honesta; NO grandfather, NO excepción que mantenga el bypass). Los tests de adapter/score inyectan un registro
válido (`BankTestFixtures.validBank(BANK_ID, kind)`) para seguir cubriendo VALID/gaming/invalidated; los tests
e2e de `MindBoostProfileSources` afirman CALIBRATING/null bajo el registro vacío de producción.

**Regla futura:**
La integridad de banco que consume un gate de runtime debe DERIVARSE de la misma validación del build-gate vía
una autoridad única (`BankIntegrityAuthority.integrityFor`), NUNCA un literal `BankIntegrity.*` en el adapter.
Toda fuente profile-updating sin banco registrado Y validado debe caer fail-closed a un estado bloqueante
(`SMALL_UNVALIDATED`/`BANK_CONTAMINATED`) — no puede producir perfil oficial maduro. Registro opt-in vacío por
defecto: activar un banco es una decisión deliberada (registrar + revisión de eficacia), no el default. Detekt:
añadir un param a un fold de boundary puede cruzar `LongParameterList` (umbral 6) — `@Suppress` local con
justificación; y cuidado con que el propio comentario de `@Suppress` no cruce `MaxLineLength` (120). Al añadir
un miembro a una interfaz de dominio (`ItemBank.kind`), actualizar TODAS sus implementaciones — incluidas las de
test (aquí 3: `BankValidatorTest`, `ExposureTrackerTest`, `BankTestFixtures`); `:shared:detekt` no lo detecta,
solo `:shared:desktopTest` (compileTestKotlinDesktop) — correr AMBOS gates, no solo detekt.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/bank/{BankIntegrityAuthority,ItemBank}.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/domain/scoring/{RecognitionScoreAdapter,MemorySpanProfileAdapter,MemoryPairsProfileAdapter,DeferredRecallProfileAdapter,MemorySpanScore,MemoryPairsScore,MemoryDeferredScore}.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/bank/{BankIntegrityAuthorityTest,BankTestFixtures,BankValidatorTest}.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/{memory/domain/scoring,profile/adapter}/**`

### 2026-07-05 — Un `read` de storage que colapsa TODO estado de error a "ausente" convierte el erasure GDPR en un falso éxito silencioso

**Error detectado:**
En el backend durable de iOS (`IosKeychainBackend`, Phase 1.2), `copyMatchingValue`/`read` devolvía `null` ante
CUALQUIER `OSStatus` no-`errSecSuccess`, mezclando "ítem ausente" con "lectura fallida de verdad". Como el store
de arriba (`IndexedSecureKeyValueStore`) guarda el conjunto de keys vivas bajo una key reservada `__index__`, un
fallo real leyendo `__index__` (p. ej. `errSecNotAvailable`/`errSecAuthFailed`/`errSecMissingEntitlement`)
durante el borrado de cuenta hacía que `readIndex()` devolviera vacío → `clearNamespace()` iteraba cero data-keys,
borraba el índice y **retornaba con normalidad** → el eraser no veía throw → `AccountDeletionCoordinator`
reportaba `Completed` con datos cognitivos huérfanos en Keychain. Erasure GDPR con éxito FALSO (pérdida silenciosa
+ falsa observabilidad). Cazado por una review adversarial multi-lente; el core puro era correcto.

**Causa raíz:**
Un `read` fail-open: el contrato "no-éxito → null = ausente" borra la distinción entre ausencia real y fallo real.
`delete` en el mismo archivo YA la hacía bien (solo `errSecItemNotFound` = no-op; cualquier otro estado lanza),
pero `read` no era simétrico. Un valor centinela ("null = ausente") que también significa "no pude leer" hace que
todo consumidor que trate "ausente" como "no hay nada que borrar/listar" (erasure, `keys`, `deleteByUser`) mienta.

**Solución aplicada:**
`read` fail-CLOSED y simétrico con `delete`: `null` SOLO ante `errSecItemNotFound` (ausencia real) o datos
no-UTF-8; cualquier OTRO estado **lanza** `KeyValueBackendException`. Así un fallo real de lectura del índice
propaga por `readIndex` → `clearNamespace` → eraser → `AccountDeletionCoordinator` (que envuelve `eraseUserData`)
→ `LocalEraseBlocked` (retryable, fail-closed: NO borra la cuenta Auth mientras quede dato local). Se eligió
PROPAGAR (no mapear a `SecureReadResult.Failure`, que dispararía un quarantine-DELETE de dato bueno ante un fallo
transitorio). KDocs de contrato (`KeyValueBackend.read`, `IndexedSecureKeyValueStore`, el actual iOS) corregidos:
`read` ya no "nunca lanza". Regresión: `clearNamespace_whenIndexReadFails_throws_notSilentSuccess` +
`read_whenBackendReadGenuinelyFails_propagates` (fake backend con `failRead` inyectable).

**Regla futura:**
Un `read` de storage sensible NUNCA debe colapsar un fallo real de lectura a "ausente"/`null`/vacío: debe ser
fail-closed (lanzar) salvo ausencia genuina (y decode inválido). Si el `delete`/`write` del mismo backend ya
distinguen "no existe" de "falló", el `read` DEBE ser simétrico. Cualquier operación de erasure/listado que derive
su resultado de un `read` que pueda mentir "vacío" produce un borrado de éxito FALSO — inaceptable para datos
cognitivos (GDPR Art. 17). El erasure debe fallar CERRADO (bloquear + reintentar), nunca reportar completado si no
puede demostrar que borró. Verificar la cadena completa read→index→clearNamespace→eraser→coordinator, no solo el
método aislado. Preferir propagar el fallo a mapearlo a un estado que dispare borrado (quarantine) de dato bueno.

**Archivos afectados:**
- `shared/src/iosMain/kotlin/com/mindos/app/features/mindboost/focus/data/local/IosKeychainBackend.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/data/local/IndexedSecureKeyValueStore.kt`
- `shared/src/iosMain/kotlin/com/mindos/app/features/mindboost/focus/data/local/SecureKeyValueStore.ios.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/focus/data/local/IndexedSecureKeyValueStoreTest.kt`

### 2026-07-04 — Un detector testeado que nadie invoca protege cero; y el centinela "no aplica" debe validarse contra el predicado de CADA consumidor

**Error detectado:**
(1) `SpeedAntiGaming.evaluate` (batería anti-gaming testeada de Velocidad) era CÓDIGO MUERTO en producción:
ningún caller — el gate de perfil solo leía `COVERAGE`, así que una sesión gamed con calidad HIGH y timebase
autoritativo habría entrado al path preliminar sin ningún chequeo anti-gaming. (2) Al cablearlo, la
convención planeada para E1/no-elección (`chanceAccuracy = 1.0`) habría hecho disparar D3 sobre un E1 rápido
HONESTO: su predicado es `accuracy <= chanceAccuracy + margen`, y con chance=1.0 eso es trivialmente cierto
para accuracy=1.0 → un falso positivo estructural. Detectado leyendo el predicado real antes de fijar la
convención (no se llegó a commitear roto).

**Causa raíz:**
(1) El detector se construyó (INFRA-03) con su suite red-team pero el wiring quedó como TODO en un KDoc; un
control "existente y testeado" sin call-site vivo no es un control. (2) Un valor centinela para "esta lógica
no aplica" (1.0 = "todo correcto") puede caer DENTRO del rango disparador de un consumidor concreto; cada
detector interpreta los inputs con su propia fórmula.

**Solución aplicada:**
`PartialFastGuessingDetector` (mitad rápida de la propia sesión consistente-con-azar + bloque global por
encima de azar → WITHHOLD; guards: solo elección, ≥10 trials rápidos, nunca acusa por velocidad sola) añadido
a la batería; `SpeedProfileSource.qualifyingSessions` ahora corre `SpeedAntiGaming.evaluate` sobre los trials
reales y excluye veredictos no limpios (task-aware). Centinela seguro: no-elección → `chanceAccuracy = 0.0`
("sin concepto de azar"), que apaga ESTRUCTURALMENTE los tres gates de azar (guard `chance in (0,1)`) en vez
de depender de que ninguna fórmula tropiece con 1.0. `COVERAGE` sigue PARTIAL (SATF completo requiere §15).

**Regla futura:**
Todo detector/control de integridad debe tener un CALL-SITE vivo en el gate que dice proteger (verificar con
grep que `evaluate` se invoca fuera de tests) — "diseñado y testeado" sin wiring es cobertura cero. Antes de
fijar un centinela de "no aplica", LEER el predicado de cada consumidor y elegir un valor que caiga en el
guard estructural (p. ej. `0.0` con `chance in (0,1)`), no un valor semánticamente bonito (1.0) que algún
predicado trate como disparador. La dirección fail-closed de un anti-gaming task-aware es "no acusar"
(idle), nunca "bloquear" lo no-inferible. Al cablear, mantener el claim de cobertura honesto (PARTIAL si
quedan bandas), y validar el fixture honesto contra TODOS los detectores de la batería, no solo el nuevo.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/quality/SpeedAntiGaming.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/data/adapter/SpeedProfileSource.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/speed/domain/quality/SpeedAntiGamingTest.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/profile/adapter/SpeedProfileSourceTest.kt`

### 2026-07-04 — Un gate de validez de sesión por SOLO duración cuenta tiempo abierto; exigir evidencia medible y aplicarlo también al score

**Error detectado:**
La validez de `sessionEvidence` (DC-3) en Focus/Logic era `duración >= 120s` y NADA más; el score usaba la
última sesión (Focus) / las 5 recientes (Logic) SIN filtro. Una sesión de ≥120s pero sin evidencia medible
(Focus `sessionMetrics` vacío / Logic `attempts` vacío = idle/abandonada) contaba como sesión válida para
DC-3 y podía alimentar el score. Duración sin evidencia es tiempo abierto, no entrenamiento.

**Causa raíz:**
Se asumió que "duró lo suficiente" ⇒ "es evidencia válida". Son ORTOGONALES: la DURACIÓN y la PRESENCIA DE
EVIDENCIA MEDIBLE son gates independientes. Ni Focus `SessionResult` ni Logic `LogicSession` tienen flag de
completado/abandonado/anomalía, así que la señal honesta de engagement es la evidencia que el modelo SÍ
expone (Focus metrics no vacío; Logic attempts no vacío).

**Solución aplicada:**
`ProfileSessionEligibility` (autoridad pura junto a `CalibrationPolicy`): `elegible = endedAt>0 AND
(endedAt−startedAt)≥120s AND hasMeasurableEvidence`. Focus/Logic filtran por `eligibleForProfile()` ANTES de
score/confidence/trend/sessionEvidence (si nada elegible → `null` → CALIBRATING). El dedup por id y el
conteo de días distintos siguen en `CalibrationPolicy`. Anti-gaming (INFRA-03) queda fuera (capa aparte).

**Regla futura:**
Un gate de "sesión válida para el perfil" debe exigir DURACIÓN suficiente Y evidencia medible presente Y
timestamp real (endedAt>0, span no negativo), y aplicarse a TODAS las entradas que informan el perfil
(sessionEvidence Y score/confidence), no solo a una: si una sesión no es evidencia válida, no debe influir
en nada del perfil. Centralizar la elegibilidad en una autoridad pura (no duplicar el criterio por source).
CUIDADO con los fixtures: uno que inyecta el AGREGADO (score/result/metrics) pero deja trials/attempts/metrics
vacíos se vuelve INELEGIBLE bajo el gate → hacerlo realista (Focus metrics no vacío; Logic
`LogicTestFixtures.attempt()`), sin alterar la fórmula/`averageIcr`.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/calibration/ProfileSessionEligibility.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/data/adapter/{Focus,Logic}ProfileSource.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/profile/**` (eligibility unit + Focus/Logic/Sources coverage + fixtures)

### 2026-07-04 — El elvis `?:` no cae en `0L`; un timestamp `0L` finge actualidad — usar `.takeIf { it > 0L }` y modelar "desconocido" como `null`

**Error detectado:**
5 de 9 ProfileSources sellaban `ModuleContribution.updatedAtMs = 0L` (época 1970) cuando el tiempo del
evento era desconocido, y `FocusProfileSource` tenía un latente peor: `profile?.updatedAtMs ?: latest.endedAtMs`
emite `0L` si el perfil almacenado trae `updatedAtMs = 0L` (el `?:` solo cae en `null`, NO en `0L`). Un 0L
disfrazado de timestamp real es "actualidad fabricada": hoy `updatedAtMs` no se consume, pero en cuanto se
surface recencia/"última medición" mezclaría 1970 con tiempos reales.

**Causa raíz:**
`updatedAtMs: Long = 0L` usaba 0L como centinela de "sin tiempo", pero 0L es un epoch VÁLIDO (1970-01-01),
indistinguible de un tiempo real. El elvis `?:` y las comparaciones no lo tratan como ausencia. Faltaba un
valor explícito de "desconocido".

**Solución aplicada:**
`ModuleContribution.updatedAtMs: Long? = null` (`null` = desconocido/no fiable, nunca 0L). Regla uniforme en
todas las fuentes: `<candidato>.takeIf { it > 0L }` (evento real Focus/Logic/Speed/Language → tiempo real >0 o
`null`; `ProfileUpdateGate` mapea `provenance.timestampMs` 0L→`null` para las gate-based Memoria/Decisiones;
Emoción → `null`). El sello del PERFIL (`MindBoostProfile.updatedAtMs`) sigue siendo el `EpochClock` inyectado
(real). Invariante fail-closed testeada: ninguna fuente de producción emite `updatedAtMs == 0L`.

**Regla futura:**
Nunca usar `0L` (ni `""`, ni `-1`) como centinela de "ausente" para un valor cuyo dominio real incluye ese
valor (un epoch, un id): 0L es un timestamp válido y el elvis `?:`/comparaciones lo tratan como presente.
Modelar "desconocido" con `null` (tipo nullable) y filtrar el candidato con `.takeIf { it > 0L }`. Un
ProfileSource nunca debe fabricar recencia: tiempo de evento real (>0) o `null`, jamás 0L. El sello temporal
del agregado usa el reloj inyectado (`EpochClock`), nunca un source; no añadir `Clock.System` a las fuentes
(kotlinx-datetime 0.7.1; learning 2026-05-29).

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/model/ModuleContribution.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/aggregation/ProfileUpdateGate.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/data/adapter/{Focus,Logic,Speed,Language,Emotion}ProfileSource.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/profile/**` (gate/aggregator/emotion/speed/sources tests)

### 2026-07-03 — Un adapter módulo→perfil debe gatear por `BankIntegrity`, no solo por estado de evidencia

**Error detectado:**
No era un bug vivo (la fuente no está cableada en producción): la FASE-0 de un slice de blindaje
(`LANGUAGE_EVIDENCE_INTEGRITY_HARDENING`) reveló que `LanguageProfileSource` gateaba la contribución
al brain-profile por `EvidenceState` (E1+) pero NO por integridad de banco. Si alguien cableara los
repos reales —que persistirían evidencia del banco DEMO `LanguageDemoBank`, `SMALL_UNVALIDATED`— la
dimensión VERBAL saldría de CALIBRATING con **señal fabricada**: el evidence-gate no distingue un banco
validado de uno demo/stub.

**Causa raíz:**
El adapter asumía "hay evidencia medible" ⇒ "la evidencia es fiable". Son ortogonales: la integridad
del BANCO (¿el contenido sostiene una inferencia?) y la provenance del EVALUADOR (¿el scorer es un
modelo real o un stub con `thetaMean` hardcodeado?) son gates independientes del estado de evidencia.
Un adapter módulo→perfil sin gate de banco puede filtrar señal de contenido no validado.

**Solución aplicada:**
`BankIntegrity.blocksProfileEligibility()` como fuente ÚNICA del set bloqueante `{CONTAMINATED,
HOLDOUT_EXHAUSTED, SMALL_UNVALIDATED}` (`ScoreStateMachine.bankContaminated` ahora delega ahí, misma
semántica). `LanguageProfileSource` gana `contentIntegrity` (default `SMALL_UNVALIDATED`) +
`provenanceValidated` (default `false`) y un `internal fun evaluate(): LanguageEvidenceDecision` puro
con prioridad `SMALL_UNVALIDATED_BANK → STUB_PROVENANCE → INSUFFICIENT_VALID_EVIDENCE → Eligible`;
`Blocked → null → CALIBRATING`. Defaults **fail-closed**. Sin persistencia nueva, sin cambio de copy.
Probado end-to-end (repo cifrado real con 5 sesiones + evidencia E3 → VERBAL CALIBRATING, fuera del
Global Score).

**Regla futura:**
Todo adapter módulo→perfil (`Cognitive*ProfileSource`) debe gatear por
`BankIntegrity.blocksProfileEligibility()` ADEMÁS del estado de evidencia: un banco demo/no validado o
provenance stub NUNCA debe producir una contribución no-nula. El **gate de lectura** (no una bandera
persistida `profileEligible`) es la autoridad de elegibilidad — evita una segunda fuente de verdad. Los
defaults del gate deben ser fail-closed. Un flag de provenance NO debe ser un literal en el call-site:
derivarlo de versiones auditables (evaluator/rubric/projector). Verificar end-to-end vía
`MindBoostProfileAggregator` que la dimensión queda CALIBRATING (`value==null`) y no entra al
`OverallSummary.compositeIndex`.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/ScoreTypes.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/ScoreStateMachine.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/data/adapter/{LanguageProfileSource,LanguageEvidenceDecision}.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/profile/adapter/LanguageProfileSourceTest.kt`
- `docs/mindboost_profile_2a/LANGUAGE_EVIDENCE_INTEGRITY_HARDENING_2026-07-03.md`

### 2026-07-03 — Un callback de "confirmado" que descarta su payload (`onSealed = { _, _ -> onExit() }`) es pérdida silenciosa de datos

**Error detectado:**
Al sellar una sesión de Decisiones (MindBoost, B1), la UI confirmaba un guardado que nunca ocurría:
`DecisionsRootScreen` cableaba el stage Result con `onSealed = { _, _ -> onExit() }`, descartando la
`DecisionSession` y el `DiaryEntry` (diario reflexivo de texto libre) que `DecisionResultScreen` SÍ emitía
(`onSealed(holder.session, entry)`). Reabrir la app no mostraba ni el diario ni la sesión.

**Causa raíz:**
Un slice previo construyó el flujo (holder `seal()` → `onSealed(session, entry)`) pero dejó la persistencia como
TODO: el callback del hub ignoraba ambos argumentos y solo navegaba. Los repos cifrados
(`SecureDecisionSessionRepository`/`SecureDiaryRepository`, namespace `mindboost_decisiones`) existían y estaban
testeados, pero nunca se instanciaban ni se invocaban. Un callback `{ _, _ -> }` que la UI presenta como
"guardado" es una mentira de UX + pérdida garantizada de datos.

**Solución aplicada:**
Se extrajo `internal fun handleSealed(session, entry, persist, exit)` (pura, unit-testable, espejo de
`handleReversibilityFinish` del mismo archivo) que **persiste ANTES de salir**; `DecisionsRootScreen` gana un
param `onSessionSealed` con default `{ _, _ -> }`; el hub instancia los dos repos sobre el store `Decisiones`
ya existente y cablea `onSessionSealed = { s, e -> mindBoostScope.launch { sessionRepo.save(s); diaryRepo.save(e) } }`.
El save va en `mindBoostScope` (`rememberCoroutineScope` a nivel del hub, FUERA del `when(track)`), que sobrevive
a la navegación Decisiones→Hub (mismo patrón fire-and-forget que `onReversibilityTrials`). Test rojo→verde:
(a) orden persist-antes-de-exit; (b) round-trip real sellar→save→"reabrir" (repos frescos sobre el mismo
`InMemorySecureKeyValueStore`)→sesión+diario visibles byte a byte. `mindboost_decisiones` ya estaba en
AccountBound+eraser → sin hueco GDPR nuevo.

**Regla futura:**
Todo callback de "completado/confirmado" que reciba un payload (`onSealed`/`onFinished`/`onSaved`) debe
CONSUMIRLO (persistir/emitir) ANTES de navegar; un `{ _, _ -> onExit() }` que la UI presenta como guardado es
pérdida silenciosa. Extraer la orquestación persist-antes-de-navegar a una función pura (`internal fun`, espejo
de las ya existentes del módulo) y probarla con round-trip REAL (save→reabrir con repos frescos sobre el mismo
store), no solo "se llamó el callback". Lanzar el save en un scope que sobreviva a la navegación
(`rememberCoroutineScope` del contenedor que permanece montado, no el del hijo que se desmonta). Riesgo residual
conocido: matar el proceso entre el `launch` y su suspensión pierde la escritura (idéntico al patrón preexistente;
B1 no lo agrava; el write-ahead/checkpoint transversal es Fase 2). No confundir el nombre `DiaryEntry.encryptedContent`
con cifrado a nivel de dominio: guarda bytes en claro y el cifrado real ocurre en `SecureKeyValueStore` (Android
Keystore AES-256-GCM); el modelo NO cifra.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/presentation/ui/DecisionsRootScreen.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/presentation/ui/MindBoostHubScreen.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/decisiones/presentation/ui/HandleSealedFinishTest.kt`

### 2026-07-01 — Overlay de carga: `matchParentSize` en TODOS los hijos colapsa el Box, y `mergeDescendants`+`contentDescription` duplica el anuncio a11y

**Error detectado:**
Al crear `AppLoadingOverlay` (sistema de carga global), (1) el `Box` exterior envolvía el contenido en
`Box(Modifier.matchParentSize()){content()}` Y el scrim también usaba `matchParentSize()` → el Box exterior
no tenía ningún hijo que lo dimensionara y colapsaba a 0 (overlay invisible). (2) El scrim usaba
`semantics(mergeDescendants=true){ contentDescription = message }` mientras el loader hijo aportaba su propio
`contentDescription` y el `Text` del mensaje → TalkBack/VoiceOver anunciaba el mensaje 2-3 veces
("Cargando…, Cargando contenido, Cargando…").

**Causa raíz:**
(1) `Modifier.matchParentSize()` mide el hijo contra el tamaño del padre pero NO contribuye a determinar ese
tamaño. Si TODOS los hijos del Box lo usan, el padre no tiene de qué dimensionarse → colapsa. (2) La política
de merge de `ContentDescription` en Compose ACUMULA (no reemplaza): un nodo `mergeDescendants` con su propio
`contentDescription` lo concatena con los de sus descendientes. `Modifier.blur` es draw-only → no altera la medida.

**Solución aplicada:**
(1) El wrapper de contenido NO usa `matchParentSize` (dimensiona el Box desde el contenido); solo el scrim
superpuesto lo usa. (2) El scrim usa `clearAndSetSemantics { liveRegion=Polite; contentDescription = message
?: A11yLoading; progressBarRangeInfo = Indeterminate }` → UN anuncio determinista (mejor que el
`mergeDescendants` de `PhotoUploadLoader`).

**Regla futura:**
En un overlay `Box { contenido; if(visible) scrim }`, exactamente UN hijo debe dimensionar el Box (el wrapper
de contenido, SIN `matchParentSize`); los hijos superpuestos (scrim) usan `matchParentSize`. Nunca ponerlo en
todos los hijos → colapsa a 0. Para el a11y de un grupo de carga modal, usar `clearAndSetSemantics` (anuncio
único), NO `mergeDescendants`+`contentDescription` (duplica, porque la política de ContentDescription acumula).
El loader base puede llevar su propio `contentDescription`/`ProgressBarRangeInfo.Indeterminate` para uso
standalone; el grupo lo re-declara con clearAndSet. Para portar un loader CSS `ease-in-out`, usar
`CubicBezierEasing(0.42f,0f,0.58f,1f)` (simétrico), NO `FastOutSlowInEasing` (asimétrico). Constantes top-level
de un archivo NUEVO deben ir en `SCREAMING_SNAKE_CASE` (TopLevelPropertyNaming); las de archivos ya baselined
que son PascalCase (p.ej. `PhotoUploadLoader`) solo pasan por estar en el baseline. EXCEPCIÓN: un
`CompositionLocal` top-level (`val LocalXxx = staticCompositionLocalOf{...}`) DEBE conservar el nombre idiomático
`LocalXxx` con `@Suppress("TopLevelPropertyNaming")` (no es constante; el prefijo `Local` es contrato de Compose).
Para tintar un loader ya migrado por el color favorito SIN regresión, leer
`favoriteAccentColor(LocalFavoriteAccent.current, fallback = <colorDeMarcaDeLaPantalla>)`: conserva el color de
marca hasta que exista un provider raíz. Añadir un campo a `AppGraph` para inyectar una dependencia exige un
DEFAULT singleton estable (un `object` no-op como `EmptyProfileCardRepository`, NUNCA `Clase()` por-instancia) o
rompe `AppGraphTest.graph_isEquatable_dataClass` (ver 2026-06-19).

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/design/components/{AppLoadingOverlay,AppBarsLoader,AppLoadingSamples}.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/design/FavoriteColor.kt`
- `shared/src/commonTest/kotlin/com/mindos/app/design/{FavoriteColorTest,components/AppBarsLoaderProgressTest}.kt`

### 2026-06-30 — Un detector declara su propia `COVERAGE`; el consumidor la consulta (no el literal `DetectorCoverage`)

**Error detectado:**
`:shared:desktopTest` falló (`AdequateCoverageInvariantTest`) al añadir un gate de cobertura en
`SpeedProfileSource`: el nuevo literal `DetectorCoverage.PARTIAL` en ese archivo no estaba en el registro
de sitios respaldados por un detector testeado, así que el escáner de la invariante lo marcó como
claim de cobertura no respaldado.

**Causa raíz:**
La invariante (`ADEQUATE/PARTIAL ⇒ control testeado invocado`) escanea TODO archivo de producción que
contenga el literal `DetectorCoverage.ADEQUATE`/`.PARTIAL`. `SpeedProfileSource` usaba el literal para
NEGAR la contribución (PARTIAL → no adecuado → CALIBRATING), pero el escáner no distingue intención: ve
el literal y exige respaldo. Además tener el nivel de cobertura hardcodeado en el consumidor permite que
derive a un claim más fuerte que el detector real.

**Solución aplicada:**
El detector DECLARA su propia cobertura como única fuente de verdad: `SpeedAntiGaming.COVERAGE =
DetectorCoverage.PARTIAL`. El consumidor (`SpeedProfileSource.contribution`) consulta
`SpeedAntiGaming.COVERAGE.adequateFor(RT_CRITICAL)` — NO contiene el literal, así que deja de ser un
"claiming site". `SpeedAntiGaming.kt` (que ahora tiene el literal) se registró en el invariante mapeado a
un detector que SÍ invoca (`RhythmTappingDetector`), y su red-team (`SpeedAntiGamingTest`) es su prueba
adversarial. El gate quedó como expression-body (`if (...) eligibleContribution else null`) → 0 returns
(ReturnCount OK), con la lógica bespoke preservada en un `internal eligibleContribution` testeable.

**Regla futura:**
Para gatear un consumidor por cobertura anti-gaming, que el DETECTOR declare `val COVERAGE =
DetectorCoverage.X` (única fuente de verdad) y el consumidor consulte `Detector.COVERAGE.adequateFor(...)`,
NUNCA hardcodee el literal en el consumidor. Todo archivo de producción que contenga un literal
`DetectorCoverage.ADEQUATE`/`.PARTIAL` DEBE registrarse en `AdequateCoverageInvariantTest` mapeado a un
control testeado que invoque (con su suite adversarial). Para gatear una métrica RT-crítica con cobertura
PARTIAL no hace falta el routing completo por `ScoreResultFactory`: basta `COVERAGE.adequateFor(RT_CRITICAL)`
(misma regla que el state machine) — el outcome (CALIBRATING) es idéntico con mucho menos riesgo.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/quality/SpeedAntiGaming.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/data/adapter/SpeedProfileSource.kt`
- `shared/src/desktopTest/.../core/scoring/AdequateCoverageInvariantTest.kt` · `.../profile/adapter/SpeedProfileSourceTest.kt`

### 2026-06-30 — Componer detectores anti-gaming genéricos da cobertura PARCIAL, no ADEQUATE; y los tests deben modelar el flujo de captura REAL

**Error detectado:**
`SpeedAntiGaming` (nuevo) componía los detectores INFRA-03 generales D1/D3/D4 y su KDoc afirmaba que un
futuro wiring "podría reclamar `DetectorCoverage.ADEQUATE` para Velocidad". Una verificación adversarial
(workflow, 3 lentes) confirmó falsos NEGATIVOS reales: fast-guessing uniforme ~260ms a chance (el floor de
D3 es `median<250`, exclusivo), alternar rápido/lento que mantiene la mediana ≥250, y la banda [100,250)ms.
Además los tests usaban dificultad VARIABLE por trial, pero los bloques MVP-0 son de dificultad FIJA, así que
el guard RT↔dificultad de D1 (correlación) es trivialmente flat en producción y nunca se ejercitaba de verdad.

**Causa raíz:**
(1) Los umbrales de los detectores generales (D3 250ms, D4 30%) no están calibrados para el gaming específico
de Velocidad, que tiene una banda de explotación de "adivinar rápido pero no ultra-rápido". Componer
detectores genéricos NO equivale a cobertura adecuada. (2) El test modelaba un flujo idealizado (dificultad
variable + RT que la sigue) que no ocurre en el runtime real (dificultad fija por bloque), dejando un guard
muerto sin cobertura de test — el mismo patrón "verifica el flujo de captura vivo" de EX4/EX1.

**Solución aplicada:**
KDoc corregido a **COBERTURA PARCIAL**: un futuro wiring debe usar `DetectorCoverage.PARTIAL` (insuficiente
para RT_CRITICAL → Velocidad sigue CALIBRATING), NUNCA `ADEQUATE`; ADEQUATE exige un detector de fast-guessing
calibrado por §15. Añadido `MIN_TRIALS_FOR_DETECTION=12` (un ratio/mediana sobre muestra diminuta no es
evidencia de gaming → verdict NONE). Tests corregidos a dificultad fija `{0.5}` (D1 se reduce correctamente a
la cadencia-IRI sola; el honesto se salva por la varianza de IRI del foreperiodo aleatorio) + test del guard.

**Regla futura:**
Antes de reclamar `DetectorCoverage.ADEQUATE` para un módulo, VERIFICAR adversarialmente que los detectores
realmente cazan sus adversarios — componer detectores genéricos suele dar solo PARCIAL (umbrales no calibrados
dejan bandas de explotación). Reclamar ADEQUATE sin eso viola el `AdequateCoverageInvariantTest`. Los tests de
un detector deben construir adversarios y un honesto que reflejen el flujo de captura REAL (en Velocidad MVP-0:
dificultad FIJA por bloque, foreperiodo aleatorio), no un flujo idealizado que deje guards muertos sin cobertura.
Todo detector de ratio/mediana necesita un guard de mínimo de muestra (como D1 tiene `MIN_IRI`).

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/quality/SpeedAntiGaming.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/speed/domain/quality/SpeedAntiGamingTest.kt`

### 2026-06-30 — Un generador paramétrico con label aleatorio + descripción plantilla fabrica una señal de perfil

**Error detectado:**
`ReversibilityEngine.generateScenario` (E2 Reversibilidad, Decisiones) producía `description =
"Reversibility triage level N"` (plantilla genérica, sin contenido de decisión) e `isReversible =
random.nextDouble() >= irreversibility` (CARA O CRUZ). La tarjeta no daba ninguna pista de
reversibilidad y la etiqueta correcta era aleatoria → la tarea es INjuzgable, pero `ReversibilityScore`
alimentaba un `REVERSIBILITY_DISCRIMINATION` a DECISION_MAKING en producción (hub escribe
`mindboost_decisiones`, brain-profile lo lee) → señal cognitiva FABRICADA.

**Causa raíz:**
El adapter clasificaba el contenido como `BankIntegrity.PROCEDURAL` (válido procedural), tratando un
generador placeholder degenerado como si fuera un banco real. Además el state machine solo bloqueaba
`CONTAMINATED`/`HOLDOUT_EXHAUSTED`, NO `SMALL_UNVALIDATED` — así que la invariante de la carta
("banco pequeño/no validado bloquea el perfil") no se enforzaba en runtime (solo en el `BankValidator`
de build de INFRA-04).

**Solución aplicada:**
(1) `ScoreStateMachine.bankContaminated` ahora también bloquea `SMALL_UNVALIDATED` (enforce de la
invariante en runtime). (2) `ReversibilityProfileAdapter.toScoreResult` gana un parámetro
`bankIntegrity` (default `PROCEDURAL`, mantiene el adapter genérico/reusable para un banco real). (3)
`ReversibilityScore.scoreOf` (el fold de producción) pasa `BankIntegrity.SMALL_UNVALIDATED` → todo
resultado es `BANK_CONTAMINATED` (no actualiza el perfil) sin importar las respuestas → DECISION_MAKING
queda CALIBRATING hasta que exista un banco validado real. El ejercicio sigue jugable (minijuego). Los
gates de discriminación/anti-gaming siguen testeados en el adapter (banco `PROCEDURAL` por defecto).

**Regla futura:**
Antes de cablear un ejercicio al perfil, AUDITAR su CONTENIDO real, no solo el scorer: una etiqueta
correcta aleatoria o una descripción-plantilla sin señal hace la tarea injuzgable y cualquier métrica
derivada es fabricada. Un generador placeholder degenerado debe marcarse `SMALL_UNVALIDATED` (no
`PROCEDURAL`) para que el bank gate lo bloquee. Mantener `BankIntegrity` como parámetro del adapter (no
hardcoded) para separar "el adapter" de "el contenido actual". Verificar que el state machine bloquea
de verdad el valor de integridad elegido (hoy: CONTAMINATED/HOLDOUT_EXHAUSTED/SMALL_UNVALIDATED).

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/ScoreStateMachine.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/domain/scoring/{ReversibilityProfileAdapter,ReversibilityScore}.kt`
- `shared/src/desktopTest/.../core/scoring/ScoringStateMachineTest.kt` · `.../decisiones/domain/scoring/ReversibilityScoreTest.kt` · `.../profile/adapter/{DecisionsProfileSourceTest,MindBoostProfileSourcesTest}.kt`

### 2026-06-30 — `kotlin.test.assertTrue/assertFalse` NO tienen overload de mensaje lazy `() -> String`

**Error detectado:**
`:shared:compileTestKotlinDesktop` falló con "Argument type mismatch: actual type is 'Function0<String>',
but 'String?' was expected" al escribir `assertTrue(cond) { "mensaje $var" }` en un test nuevo.

**Causa raíz:**
A diferencia de `kotlin.require`/`check`/`error` (stdlib, que sí toman `lazyMessage: () -> String`) y de
algunos `assert*` de JUnit, las funciones de `kotlin.test` solo exponen `assertTrue(actual: Boolean,
message: String? = null)` y `assertTrue(message: String? = null, block: () -> Boolean)` — NINGUNA acepta
`(Boolean, () -> String)`. El `{ ... }` se interpretó como el `block: () -> Boolean` del otro overload.

**Solución aplicada:**
Construir el mensaje en un `String` eager y pasarlo como 2º argumento: `val msg = "..."; assertTrue(cond, msg)`.

**Regla futura:**
En tests `kotlin.test` (KMP commonTest/desktopTest), el mensaje de `assertTrue`/`assertFalse`/`assertEquals`
es un `String?` eager, nunca un lambda. Si el mensaje es caro de construir, calcularlo antes en un `val`.
Reservar la sintaxis `{ "msg" }` para `require`/`check`/`requireNotNull`/`checkNotNull`, no para `assert*`.

**Archivos afectados:**
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/scoring/AdequateCoverageInvariantTest.kt`

### 2026-06-30 — Una invariante de reglas no se auto-extiende a colecciones nuevas; el secret-scanner no cubría fuente cliente

**Error detectado:**
Una auditoría de secretos encontró (1) 4 reglas `allow list` de marketplace
(`marketplace_catalog`, `marketplace_assets`, `user_entitlements/.../items`,
`user_inventory/.../items`) SIN `request.query.limit`, pese a que el resto del archivo lo impone
(invariante 2026-05-26); y (2) que `functions/scripts/scan-secrets.js` (el gate de secretos de CI)
solo escaneaba `.ts/.js/.json/.rules/.md/.xml/.plist/.yml/.yaml` y NO `.kt/.kts/.swift` — toda la
fuente cliente (847 archivos) quedaba fuera del único gate automático contra secretos.

**Causa raíz:**
Las dos invariantes existían pero no se auto-aplican: una regla `allow list` añadida DESPUÉS del
endurecimiento 2026-05-26 no hereda el límite (ni el validador `firebase_validate_security_rules` ni
el emulador exigen que toda `list` tenga `request.query.limit`), y el allowlist de extensiones del
scanner nunca se amplió a la fuente cliente aunque cualquier literal en `.kt/.swift` es extraíble de
un APK/IPA por ingeniería inversa.

**Solución aplicada:**
(1) Las 4 reglas ahora exigen `request.query.limit != null && <= N` (catálogo/assets=50,
entitlements/inventory=100), manteniendo la autorización (`signedIn()`/`isOwner()`) ortogonal al
límite; +12 tests de regresión (M12–M23) en `firestore.security.rules.test.mjs` (134/134 verde en
emulador). (2) Se añadieron `.kt/.kts/.swift` a `isScannable()`; `scan:secrets` sigue verde con 847
archivos cliente en alcance (re-confirma cero secretos hardcodeados) y sus 21 tests unit pasan.
NOTA: marketplace no tiene cliente Kotlin aún (grep 0 hits), así que el límite es un contrato
forward sin desfase regla↔cliente (invariante 2026-05-29).

**Regla futura:**
Al añadir una colección con `allow list`, aplicar SIEMPRE el cap `request.query.limit` (autorización
y límite son controles ortogonales) y añadir su test de regresión `list-sin-limit→FAIL` +
`list-con-limit→OK` en el mismo cambio — ninguna herramienta lo exige automáticamente. Todo
secret-scanner debe cubrir la fuente CLIENTE (`.kt/.kts/.swift`), no solo backend: un literal
compilado en un APK/IPA es tan extraíble como uno de servidor.

**Archivos afectados:**
- `firestore.rules`
- `firestore.security.rules.test.mjs`
- `functions/scripts/scan-secrets.js`
- `.gitignore` (defensa anti-commit de dumps de conversaciones IA personales no trackeados)

### 2026-06-30 — Una métrica RT-crítica no debe alimentar el perfil sin gate de timebase autoritativo en el profile-source

**Error detectado:**
`SpeedProfileSource` (cableado en producción, `MindBoostProfileSources.kt:57`) alimentaba la dimensión
PROCESSING_SPEED filtrando solo runs `LOW`/`CONTAMINATED`. En desktop/iOS el `FallbackSpeedTimebaseHandle`
persiste `SYSTEM_CLOCK_MONOTONIC`/`MILLIS_DERIVED` → `deviceTimingRisk=MODERATE` → `SessionQuality.ACCEPTABLE`,
que SÍ entraba al perfil: un RT medido con base temporal no-autoritativa (fallback monotónico, no
Choreographer) actualizaba silenciosamente el perfil cognitivo (falso-positivo de validez VIVO).

**Causa raíz:**
El gate de honestidad del adapter (`honestSpeedScore`) solo exigía que la barra de velocidad fuese
computable, NO que el timebase fuese autoritativo. La clasificación de calidad (`SessionQualityEvaluator`)
ya niega `HIGH` a runs no-nativos, pero `ACCEPTABLE` no se excluía del perfil — y para una métrica
RT-crítica, "aceptable para jugar" ≠ "autoritativo para medir". El `TimingPolicyClassifier` que define
`Authoritative` (`CHOREOGRAPHER_FRAME && NANOS_NATIVE`) existía pero estaba muerto (no cableado al
profile-source).

**Solución aplicada:**
En `SpeedProfileSource.qualifyingSessions` se añadió `hasAuthoritativeTimebase(sessionId)` que excluye toda
sesión cuyo `SessionMetrics` no sea `timestampSource==CHOREOGRAPHER_FRAME && inputTimestampPrecision==NANOS_NATIVE`
(métricas ausentes → inverificable → excluida). Al vivir el gate en `qualifyingSessions`, todo el pipeline
(conteo→dataSufficiency/confidence, trend, latest) refleja solo medidas autoritativas. Velocidad solo
alimenta el perfil en Android API≥34 (Choreographer + nanos nativos); fallback desktop/iOS y Android API<34
quedan jugables pero excluidos. Los campos de timebase ya estaban en `SessionMetrics` (cargado por el adapter)
y `QualityDecisionLog` — no hicieron falta nuevos métodos de store.

**Regla futura:**
Toda señal RT-crítica que alimente el perfil debe gatearse por timebase AUTORITATIVO en el propio
profile-source (`CHOREOGRAPHER_FRAME && NANOS_NATIVE`), no solo por la clasificación de calidad de la sesión:
`SessionQuality.ACCEPTABLE` (jugable) NO implica timebase válido para medir. El gate va donde se decide qué
sesiones informan el perfil (`qualifyingSessions`), para que el CONTEO (sufficiency/confidence/trend) también
excluya las no-autoritativas, no solo el último valor. Verificar SIEMPRE los campos de autoridad (timestampSource/
inputTimestampPrecision) contra lo que persiste el `Fallback*Handle` de cada plataforma antes de fijar la regla.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/data/adapter/SpeedProfileSource.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/profile/adapter/SpeedProfileSourceTest.kt`

### 2026-06-25 — El hub de MindBoost necesita `verticalScroll`: los tracks nuevos quedaban fuera de pantalla

**Error detectado:**
El track "Creatividad" (y "Lenguaje") no se podían seleccionar para entrenar: la tarjeta existía y el
módulo compilaba, pero no aparecía en pantalla.

**Causa raíz:**
`HubContent` de `MindBoostHubScreen` apila las tarjetas en un `Column` SIN `verticalScroll`. Al crecer
el hub a 8 tracks, las últimas tarjetas (Creatividad es la última) se renderizan por debajo del
viewport del teléfono y quedan inalcanzables. No es gating ni falta de wiring — es overflow sin scroll.

**Solución aplicada:**
Se añadió `.verticalScroll(rememberScrollState())` (+ `navigationBarsPadding()`) al `Column` del hub.

**Regla futura:**
Toda lista vertical de tarjetas/opciones que pueda crecer (hubs, menús de ejercicios) DEBE usar
`Modifier.verticalScroll(rememberScrollState())` (o `LazyColumn`). Al añadir un track al hub, verificar
que sea ALCANZABLE en un teléfono, no solo que compile: un Composable que compila no equivale a una UI
alcanzable.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/presentation/ui/MindBoostHubScreen.kt`

### 2026-06-25 — `Modifier.background(if cond Color else Brush)` no resuelve (tipo común `Any`)

**Error detectado:**
`:shared:compileKotlinDesktop` falló en el botón "Añadir amigo" del nuevo perfil con
"Argument type mismatch: actual type is 'Any', but 'Color'/'Brush' was expected" en `.background(...)`.

**Causa raíz:**
Se pasó `background(if (cond) Color(...) else Brush.verticalGradient(...))`. `Color` y `Brush` no
comparten supertipo útil, así que el `if/else` se infiere como `Any`, y ninguno de los dos overloads
de `Modifier.background` (uno toma `Color`, otro `Brush`) es aplicable.

**Solución aplicada:**
Ramificar el `Modifier`, no el argumento:
`.then(if (cond) Modifier.background(color) else Modifier.background(brush))`.

**Regla futura:**
Cuando una propiedad visual de Compose dependa de una condición y las dos ramas sean de tipos distintos
no relacionados (`Color` vs `Brush`, dp vs lambda overloads, etc.), ramificar el `Modifier` con
`.then(if … Modifier.x(a) else Modifier.x(b))`, nunca meter el `if` dentro del argumento. Mismo patrón
ya usado para `border` condicional en este repo.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/profile/card/presentation/ui/ProfileCardHeader.kt`

### 2026-06-22 — Añadir un miembro a `FeatureFlags` obliga a actualizar TODAS sus implementaciones

**Error detectado:**
`:androidApp:compileDebugKotlin` falló con "Class 'RemoteConfigFeatureFlags' is not abstract and
does not implement abstract member" para `arenaRatingEngineEnabled`, `arenaShadowRankedEnabled` y
`arenaMentalSpeedRankedEnabled`. `:shared:compileAndroidMain` compilaba: el desfase solo aparece al
compilar el módulo `androidApp`.

**Causa raíz:**
La interfaz `core/config/FeatureFlags.kt` (en `:shared`) ganó tres flags de Arena y se actualizaron
`DefaultFeatureFlags`/`StaticFeatureFlags`, pero NO la implementación Android
`RemoteConfigFeatureFlags`, que vive en `androidApp/src/main/java/com/mindos/app/MindOSApplication.kt`
—fuera de `:shared`—. Compilar solo `:shared` no detecta la implementación incompleta del otro módulo.

**Solución aplicada:**
Se añadieron los tres `override val arena*` a `RemoteConfigFeatureFlags` con el patrón existente
`boolOrDefault(DefaultFeatureFlags.KEY_*, DefaultFeatureFlags.*Enabled)` (claves y defaults ya
existían en `DefaultFeatureFlags`).

**Regla futura:**
Al añadir un miembro a `FeatureFlags` (u otra interfaz con impls repartidas por módulos), actualizar
TODAS las implementaciones: `DefaultFeatureFlags`, `StaticFeatureFlags` (en `:shared`) y
`RemoteConfigFeatureFlags` (en `androidApp`). Verificar con `:androidApp:compileDebugKotlin`, no solo
`:shared:compileAndroidMain`: las impls del módulo de app no se compilan al validar únicamente `:shared`.

**Archivos afectados:**
- `androidApp/src/main/java/com/mindos/app/MindOSApplication.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/core/config/FeatureFlags.kt`

### 2026-06-20 — La telemetría remota nunca debe aceptar Throwable externos

**Error detectado:**
Los fallos de borrado de cuenta y publicación social llegaban por identidad a `Crashlytics.recordException`, conservando mensaje y cadena de causas del SDK.

**Causa raíz:**
El contrato central `ObservabilitySink.recordNonFatal` aceptaba un `Throwable` arbitrario y no imponía minimización en la frontera con telemetría de terceros.

**Solución aplicada:**
El contrato se sustituyó por `NonFatalTelemetryEvent` con operación y tipo normalizados; Crashlytics crea una excepción local sin causa ni mensaje externo y los tests usan canarios para impedir regresiones.

**Regla futura:**
Ningún sink remoto puede aceptar excepciones externas crudas; convertirlas antes a campos allowlisted y probar que mensaje, causa e identidad original no cruzan la frontera.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/core/logging/Observability.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/core/logging/SafeDebugLogging.kt`
- `androidApp/src/main/java/com/mindos/app/MindOSApplication.kt`
- `shared/src/commonTest/kotlin/com/mindos/app/core/logging/ObservabilityHubTest.kt`

### 2026-06-20 — Las exportaciones debug no deben incorporar identificadores estables

**Error detectado:**
Los nombres y contenidos de exportaciones de Velocidad incluían UID, session ID, modelo y versión del dispositivo, y podían terminar copiados a evidencias locales de QA.

**Causa raíz:**
Se trató el gating debug y el almacenamiento privado como controles suficientes, sin minimizar los datos antes de escribirlos ni desacoplar el nombre de archivo de la identidad.

**Solución aplicada:**
Los exportadores usan nombres aleatorios, reemplazan identificadores estables y metadatos de dispositivo por valores de depuración, y no muestran mensajes crudos de errores de escritura.

**Regla futura:**
Toda exportación diagnóstica debe minimizar o pseudonimizar identidades antes de I/O y usar nombres no derivados del usuario, aunque esté limitada a builds debug o almacenamiento privado.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/debug/DebugExport.kt`
- `shared/src/androidMain/kotlin/com/mindos/app/features/mindboost/speed/debug/DebugExporter.android.kt`
- `shared/src/desktopMain/kotlin/com/mindos/app/features/mindboost/speed/debug/DebugExporter.desktop.kt`
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/speed/debug/DebugExportTest.kt`

### 2026-06-20 — Los fixtures de tests para lógica gated deben usar flags de confianza realistas

**Error detectado:**
`PersonalizedSessionScorerTest.isGated_whenTooFewTrials` falló: construía un `MetricValue` de ACCURACY
con `n=10` pero `confident=true`, esperando que el scorer devolviera `value=null`. El scorer puntúa
cualquier ACCURACY marcada `confident`, así que devolvió un valor.

**Causa raíz:**
El fixture fabricó un estado imposible: `DefaultMetricsEngine` marca `confident=false` para muestras
pequeñas, pero el test puso `confident=true` con `n=10`. La lógica gated del scorer depende del flag
`confident`, no de `n` directamente.

**Solución aplicada:**
El test modela la realidad: muestra pequeña → `confident=false` en el `MetricValue`, y el scorer
devuelve `value=null` con `confident=false`.

**Regla futura:**
Al testear lógica gated por `MetricValue.confident`, construir los fixtures con flags coherentes con lo
que produce el `MetricsEngine` (n pequeño ⇒ `confident=false`). Nunca fijar `confident=true` con un `n`
por debajo del umbral para "forzar" un camino.

**Archivos afectados:**
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/focus/domain/personalization/PersonalizedSessionScorerTest.kt`
- `CLAUDE.md`

### 2026-06-19 — Los overrides npm deben respetar rangos independientes

**Error detectado:**
`npm ls uuid --all` falló después de aplicar un override global a `uuid@11.1.1`: `universal-analytics@0.5.4` requiere `uuid@^14` y su instalación quedó marcada como inválida.

**Causa raíz:**
Se interpretó “override exacto” como reemplazo global sin comprobar que el árbol raíz contiene consumidores con contratos mayores e incompatibles.

**Solución aplicada:**
El override raíz se acotó a `uuid@<11.1.1` con reemplazo exacto `11.1.1`; así se endurecen versiones antiguas sin alterar el contrato `uuid@^14`.

**Regla futura:**
Antes de fijar un override npm global, ejecutar `npm ls <paquete> --all` y revisar todos los rangos; usar selector de versión acotado cuando existan ramas legítimas incompatibles.

**Archivos afectados:**
- `package.json`
- `docs/security/DEPENDENCY_HARDENING_2026-06-19.md`
- `AGENTS.md`
- `CLAUDE.md`


### 2026-06-19 — Las mutaciones de posts requieren autoridad backend e identidad canónica

**Error detectado:**
Las reglas permitían escrituras directas del cliente en `posts` y `user_posts`, y Android podía construir la identidad del autor desde campos fallback si faltaba `profile_visuals/{uid}`.

**Causa raíz:**
La autoridad de creación estaba dividida entre el cliente y la callable, y la compatibilidad de bootstrap se trató como sustituto de una identidad canónica.

**Solución aplicada:**
Las reglas deniegan toda mutación cliente de posts, Android usa `createPost`, la callable valida el payload y deriva la identidad exclusivamente del perfil canónico antes de escribir ambos mirrors.

**Regla futura:**
Toda mutación de posts debe pasar por la callable autoritativa; nunca aceptar ni reconstruir identidad de autor desde campos controlados por el cliente.

**Archivos afectados:**
- `firestore.rules`
- `functions/src/posts.ts`
- `functions/src/lib/validators.ts`
- `shared/src/androidMain/kotlin/com/mindos/app/features/social/data/feed/GlobalFeedRepositoryProvider.android.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Los mapas de recibos DM deben limitar claves y propietario

**Error detectado:**
Un participante podía reemplazar `lastReadAtByUser` y escribir el recibo de lectura del otro participante o añadir claves ajenas al chat.

**Causa raíz:**
La regla validaba solo que el campo fuese un mapa, sin limitar sus claves ni comprobar qué entrada había cambiado.

**Solución aplicada:**
Las claves se restringieron a los participantes canónicos y `Map.diff().affectedKeys()` se limita a `request.auth.uid`; en creación solo se permite la clave del creador.

**Regla futura:**
Todo mapa Firestore indexado por UID debe validar la allowlist de claves y la propiedad de las claves modificadas; validar únicamente `is map` no protege su integridad.

**Archivos afectados:**
- `firestore.rules`
- `firestore.security.rules.test.mjs`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — El gate Android KMP se ejecuta desde la app y las propiedades se citan en PowerShell

**Error detectado:**
El gate intentó ejecutar la tarea inexistente `:shared:compileDebugKotlinAndroid`; el siguiente intento interpretó `-Pkotlin.incremental=false` como una tarea por el parsing de PowerShell.

**Causa raíz:**
Se asumió el nombre de tarea del plugin Android clásico aunque `shared` usa Android Multiplatform Library, y no se protegió el argumento Gradle con punto al invocarlo desde PowerShell.

**Solución aplicada:**
Se verificaron las tareas reales y se ejecutó `:androidApp:compileDebugKotlin`, que compila `:shared:compileAndroidMain`, pasando `"-Pkotlin.incremental=false"` entre comillas.

**Regla futura:**
Para validar Android de `shared`, usar `:androidApp:compileDebugKotlin` o verificar primero `:shared:tasks --all`; en PowerShell citar siempre propiedades Gradle `-P...` que contengan puntos.

**Archivos afectados:**
- `AGENTS.md`
- `CLAUDE.md`


### 2026-06-19 — Un prefijo versionado no puede confundirse con Base64 legacy

**Error detectado:**
La primera detección de envelope trataba cualquier ciphertext que empezara por `v` como versionado, aunque un Base64 legacy válido también puede empezar por ese carácter.

**Causa raíz:**
Se comprobó solo el primer carácter y no la presencia del delimitador que distingue el envelope de la codificación legacy.

**Solución aplicada:**
Solo se parsea una versión cuando existe `:`; los valores sin delimitador conservan la ruta legacy y las versiones futuras siguen devolviendo estado tipado.

**Regla futura:**
Todo formato versionado añadido sobre datos existentes debe usar una firma no ambigua y mantener un test/razonamiento explícito de compatibilidad con el alfabeto legacy.

**Archivos afectados:**
- `shared/src/androidMain/kotlin/com/mindos/app/features/mindboost/focus/data/local/SecureKeyValueStore.android.kt`
- `docs/security/SECURE_STORAGE_FAILURE_DECISION_2026-06-19.md`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — El modelo tipado de storage debe nacer compatible con Detekt

**Error detectado:**
`:shared:detekt` rechazó el nuevo secure storage por catches tipados como swallowed, exceso de returns, nombre de constante de instancia y líneas largas en tests.

**Causa raíz:**
Se implementó primero la semántica de seguridad sin adaptar su control de flujo y estilo al ratchet del módulo.

**Solución aplicada:**
Se documentó el catch que convierte parse errors en cuarentena, se redujeron returns con expresiones `when/if`, se normalizó el nombre de instancia y se ajustaron los tests.

**Regla futura:**
Al añadir resultados sellados y parsers de seguridad, diseñar desde el inicio un único retorno principal y localizar cualquier supresión de excepción convertida en estado tipado.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/data/local/SecureKeyValueStore.kt`
- `shared/src/androidMain/kotlin/com/mindos/app/features/mindboost/focus/data/local/SecureKeyValueStore.android.kt`
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/logic/data/LocalLogicTrainingRepositoryTest.kt`
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/memory/data/LocalMemoryRepositoriesTest.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Los creates inmutables de Storage deben exigir resource null

**Error detectado:**
El emulador aceptó sobrescribir un post attachment aunque el match declaraba `allow update: false`.

**Causa raíz:**
La rama `allow create` de profile/post no expresaba la precondición `resource == null`, dejando la inmutabilidad dependiente de la clasificación de operación del runtime.

**Solución aplicada:**
Todos los creates de media exigen explícitamente `resource == null` y un check estático fija las tres rutas.

**Regla futura:**
Para objetos Storage inmutables, combinar siempre `allow update: false` con `resource == null` en `allow create`; no confiar solo en la clasificación create/update del emulador.

**Archivos afectados:**
- `storage.rules`
- `storage.dm.rules.test.mjs`
- `functions/scripts/check-storage-rules.js`
- `docs/security/STORAGE_RULES_DYNAMIC_DECISION_2026-06-19.md`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Los nombres thumbnail no deben caer al validador genérico

**Error detectado:**
El test dinámico aceptó un `*_thumb.jpg` mayor de 1 MiB porque, tras fallar `validThumb`, el mismo nombre coincidía con `validImage` y obtenía el límite de imagen principal.

**Causa raíz:**
Las ramas de validación de thumbnail e imagen genérica se combinaron con OR sin hacer sus patrones mutuamente excluyentes.

**Solución aplicada:**
La rama genérica excluye explícitamente el patrón canónico de thumbnail; se añadieron regresiones dinámica y estática.

**Regla futura:**
Cuando distintos tipos de archivo tengan límites diferentes, sus clasificadores deben ser mutuamente excluyentes; probar siempre que el tipo más restringido no pueda caer a una rama más permisiva.

**Archivos afectados:**
- `storage.rules`
- `storage.dm.rules.test.mjs`
- `functions/scripts/check-storage-rules.js`
- `docs/security/STORAGE_RULES_DYNAMIC_DECISION_2026-06-19.md`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Storage Rules con firestore.get requieren ambos emuladores

**Error detectado:**
El harness dinámico de Storage terminó con `Cannot determine host and port of firestore` al evaluar reglas que contienen `firestore.exists/get`.

**Causa raíz:**
Se inició únicamente el emulador de Storage, pero su runtime necesita el endpoint de Firestore para resolver dependencias cross-service de las reglas.

**Solución aplicada:**
El comando dinámico inicia `firestore,storage` dentro de la misma ejecución de emuladores.

**Regla futura:**
Todo test de Storage Rules que referencie documentos Firestore debe arrancar ambos emuladores, aunque el escenario concreto parezca cortocircuitar esa rama.

**Archivos afectados:**
- `package.json`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Los guardrails estáticos deben tolerar formato equivalente

**Error detectado:**
El check estático de Storage Rules rechazó el harness válido porque buscaba `assert.equal(occurrences,` como texto contiguo y el argumento estaba en la línea siguiente.

**Causa raíz:**
La comprobación de estructura se implementó con un `includes` dependiente del formato en vez de aceptar espacios y saltos de línea.

**Solución aplicada:**
El guardrail usa una expresión regular acotada que valida la llamada y su primer argumento sin depender del layout.

**Regla futura:**
Los checks estáticos de código deben validar invariantes estructurales y tolerar whitespace equivalente; reservar `includes` para literales canónicos exactos.

**Archivos afectados:**
- `functions/scripts/check-storage-rules.js`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Editar archivos legacy activa su deuda completa en el ratchet Detekt

**Error detectado:**
`:shared:detekt` rechazó los archivos modificados por reglas legacy de longitud, nombres Compose y parámetros sin uso, además de los catches que convierten fallos de recuperación en estados tipados.

**Causa raíz:**
Al retirar una supresión existente y ampliar wiring legacy, los archivos volvieron a entrar en el ratchet con deuda previa; las excepciones del coordinador se transforman deliberadamente en resultados de recuperación.

**Solución aplicada:**
Se restauraron y localizaron las supresiones estrictamente necesarias en funciones/archivos afectados y se documentó el catch tipado del coordinador.

**Regla futura:**
Al modificar un archivo legacy, preservar sus supresiones localizadas y ejecutar Detekt inmediatamente; toda nueva supresión debe quedar limitada al archivo o función cuya API o flujo la haga inevitable.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/core/di/AppGraph.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/home/presentation/HomeScreen.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/privacy/SettingsScreen.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/privacy/application/AccountDeletionCoordinator.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Los smoke tests autenticados no pueden asumir Firebase inicializado

**Error detectado:**
El test de navegación autenticada falló al montar `MindOSApp` porque `HomeScreen` construye repositorios Firebase concretos y el runtime desktop no tenía `FirebaseApp` inicializada.

**Causa raíz:**
Se asumió que inyectar `AppGraph` aislaba todo el árbol autenticado, aunque `HomeScreen` todavía crea colaboradores Firebase fuera del grafo.

**Solución aplicada:**
La entrada visible de ajustes se extrajo a un composable puro y se probó su callback sin inicializar Firebase; el flujo de borrado se mantiene cubierto por el test dedicado de Settings.

**Regla futura:**
No montar el árbol autenticado completo en tests desktop hasta que todos sus colaboradores Firebase estén inyectados; probar componentes puros o inyectar explícitamente cada dependencia externa.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/home/presentation/HomeScreen.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/home/presentation/HomeAccountActionsTest.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/MindOSAppNavigationSmokeTest.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Los defaults mutables rompen la igualdad estructural de AppGraph

**Error detectado:**
`AppGraphTest.graph_isEquatable_dataClass` falló porque cada construcción de `AppGraph` creaba un `InMemoryPendingAccountDeletionStore` distinto como valor por defecto.

**Causa raíz:**
Se introdujo una dependencia mutable con una instancia nueva en un parámetro por defecto de un data class cuya igualdad estructural está fijada por tests.

**Solución aplicada:**
El default se sustituyó por un singleton no-op estable; producción sigue inyectando el journal cifrado y los tests de borrado inyectan un store en memoria explícito.

**Regla futura:**
Los parámetros por defecto de `AppGraph` deben tener identidad estable; toda dependencia mutable o stateful debe inyectarse explícitamente desde producción o desde el test.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/core/di/AppGraph.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/privacy/application/AccountDeletionCoordinator.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Un release publicable nunca puede usar firma debug

**Error detectado:**
`assembleRelease` y `bundleRelease` generaban artefactos con el keystore debug cuando faltaban las propiedades de firma, dejando una ruta publicable protegida solo por un warning.

**Causa raíz:**
Se mezcló la validación R8 de CI con la generación del artefacto publicable y se consideró suficiente advertir del fallback en logs.

**Solución aplicada:**
La variante `release` ahora falla durante configuración cuando se solicita un artefacto publicable sin las cuatro propiedades de firma; CI omite ese bundle si no tiene secretos y el workflow de publicación exige evidencias externas aprobadas.

**Regla futura:**
Ningún build type publicable puede caer a firma debug. Separar o saltar validaciones de release sin secretos y exigir fallo cerrado en `assembleRelease`/`bundleRelease`.

**Archivos afectados:**
- `androidApp/build.gradle.kts`
- `.github/workflows/kotlin-ci.yml`
- `.github/workflows/release.yml`
- `docs/release/RELEASE_EVIDENCE.json`
- `scripts/check-release-evidence.mjs`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Firebase Functions 6 cambia el entrypoint TypeScript a v2

**Error detectado:**
Al subir `firebase-functions` de 4.9.0 a 6.6.0 para cerrar CVE transitivas, el typecheck dejó de resolver `CallableContext`, `runWith`, `firestore.document` y `storage.object`; los wrappers de tests también cambiaron de firma.

**Causa raíz:**
La línea 6 expone v2 desde el entrypoint principal, mientras este backend está implementado y probado sobre la API v1. La compatibilidad de Node y peer dependencies no garantiza compatibilidad del API importado.

**Solución aplicada:**
Se seleccionó `firebase-functions` 5.1.1, última línea compatible con los imports v1 actuales, se mantuvo `firebase-admin` 12.7.0 por su peer range y se actualizaron las transitivas vulnerables del lockfile dentro de rangos compatibles.

**Regla futura:**
Antes de subir `firebase-functions` a 6+, migrar explícitamente todos los imports a `firebase-functions/v1` o los triggers a v2 en una tarea dedicada; validar `npm run typecheck` además de peer dependencies y audit.

**Archivos afectados:**
- `functions/package.json`
- `functions/package-lock.json`
- `docs/security/DEPENDENCY_HARDENING_2026-06-19.md`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-19 — Los gates Gradle del mismo worktree deben ejecutarse en serie

**Error detectado:**
`:shared:desktopTest` produjo 539 fallos `NoClassDefFoundError` y después no pudo regenerar el snapshot de classpath al ejecutarse a la vez que `:shared:detekt` sobre el mismo directorio `shared/build`.

**Causa raíz:**
Dos invocaciones Gradle independientes escribieron simultáneamente los outputs de compilación KMP del mismo worktree. La carrera dejó clases y snapshots incrementales incompletos; el primer reintento conservó ese estado corrupto.

**Solución aplicada:**
Se detuvieron los daemons, se limpió `:shared` y se repitió `:shared:desktopTest` en serie, sin build/configuration cache y con compilación incremental desactivada; los 549 tests pasaron.

**Regla futura:**
No ejecutar en paralelo tareas Gradle que compartan worktree y directorios `build`; serializar `detekt`, compilación y tests. Si aparece una cascada de clases ausentes o snapshots ilegibles, detener daemons, limpiar el módulo y revalidar sin caches antes de cambiar código.

**Archivos afectados:**
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-16 — SQLDelight custom Boolean requiere adapter explícito

**Error detectado:**
`:shared:desktopTest` falló al cambiar `safety_flag` a `INTEGER AS Boolean`: el código generado intentó importar `Boolean` como tipo custom y exigía un adapter.

**Causa raíz:**
Se asumió que SQLDelight mapearía automáticamente `INTEGER AS Boolean` al primitivo Kotlin en este setup. En realidad, esa sintaxis activa un tipo custom y cambia el constructor generado de la base.

**Solución aplicada:**
Se mantuvo `safety_flag` como `INTEGER NOT NULL CHECK (safety_flag IN (0, 1))` y se conservó el mapper explícito `Long`/`Boolean` en el repositorio.

**Regla futura:**
No usar `AS Boolean` en SQLDelight sin comprobar el adapter generado; para flags simples, preferir `INTEGER CHECK (0, 1)` con mapper explícito si no se quiere cambiar el wiring de la base.

**Archivos afectados:**
- `shared/src/commonMain/sqldelight/com/mindverse/mindboost/core/storage/db/DiaryEntry.sq`
- `shared/src/commonMain/kotlin/com/mindverse/mindboost/core/storage/SqlDelightRepositories.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-16 — SQLDelight añade Queries al nombre del archivo .sq

**Error detectado:**
`:shared:desktopTest` falló porque los repositorios importaban `DecisionSessionQueries` y `DiaryEntryQueries`, pero SQLDelight generó `DecisionSessionQueriesQueries` y `DiaryEntryQueriesQueries`.

**Causa raíz:**
Los archivos de esquema se nombraron `DecisionSessionQueries.sq` y `DiaryEntryQueries.sq`, sin considerar que SQLDelight deriva la clase de queries añadiendo el sufijo `Queries` al nombre base del archivo.

**Solución aplicada:**
Se renombraron los esquemas a `DecisionSession.sq` y `DiaryEntry.sq`, manteniendo las queries internas y los repositorios con imports limpios.

**Regla futura:**
Al crear archivos SQLDelight, nombrar el `.sq` por entidad o agregado, no con sufijo `Queries`, para evitar clases generadas con `QueriesQueries`.

**Archivos afectados:**
- `shared/src/commonMain/sqldelight/com/mindverse/mindboost/core/storage/db/DecisionSession.sq`
- `shared/src/commonMain/sqldelight/com/mindverse/mindboost/core/storage/db/DiaryEntry.sq`
- `shared/src/commonMain/kotlin/com/mindverse/mindboost/core/storage/SqlDelightRepositories.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-16 — Estados nuevos deben cerrar UI y ratchet Detekt

**Error detectado:**
`:shared:desktopTest` falló porque `LanguageStage.MENU` no estaba cubierto en `LanguageRootScreen`; después, `:shared:detekt` rechazó nuevas fórmulas por complejidad, números mágicos, nombres de constructor requeridos y exceso de funciones.

**Causa raíz:**
Se añadió un estado de flujo sin actualizar todos los `when` exhaustivos. Además, las ecuaciones públicas de Alpha Core copiaban una especificación matemática con muchas constantes y nombres obligatorios sin adaptar el archivo al ratchet Detekt del repo.

**Solución aplicada:**
Se añadió la rama exhaustiva `MENU` sin wiring nuevo de UI, se localizó la supresión de `TooManyFunctions` del holder y se normalizaron/suprimieron de forma localizada las reglas Detekt inevitables en las fórmulas y plantillas de Alpha Core.

**Regla futura:**
Al introducir un nuevo estado de UI, actualizar todos los `when` exhaustivos antes de compilar; al portar fórmulas de especificación, declarar constantes o supresiones locales antes de ejecutar Detekt.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/language/presentation/ui/LanguageRootScreen.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/language/presentation/LanguageSessionStateHolder.kt`
- `shared/src/commonMain/kotlin/com/mindverse/mindboost/core/domain/Models.kt`
- `shared/src/commonMain/kotlin/com/mindverse/mindboost/core/scoring/ScoringEquations.kt`
- `shared/src/commonMain/kotlin/com/mindverse/mindboost/core/adaptive/DifficultyAdapter.kt`
- `shared/src/commonMain/kotlin/com/mindverse/mindboost/core/mdp/bayes/BayesEngine.kt`
- `shared/src/commonMain/kotlin/com/mindverse/mindboost/core/mdp/reversib/ReversibilityEngine.kt`
- `shared/src/commonMain/kotlin/com/mindverse/mindboost/core/coach/CoachEngine.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-15 — Los ports Compose deben adaptarse a extensiones de scope y al ratchet Detekt

**Error detectado:**
La primera compilación de Personal Identity no resolvió imports de `consume` y `align`; después, Detekt rechazó el entry point requerido `LabScreen()`.

**Causa raíz:**
El paquete externo asumía extensiones importables de otra versión de Compose, aunque en este repo `consume()` es miembro y `align` pertenece a `BoxScope`. Además, el entry point composable con nombre PascalCase no tenía la supresión localizada de `FunctionNaming`.

**Solución aplicada:**
Se retiraron los imports incompatibles y se añadió `@file:Suppress("FunctionNaming")` únicamente al archivo del entry point composable.

**Regla futura:**
Al portar Compose externo, validar imports contra la versión del repo y ejecutar compilación más Detekt; no importar extensiones que sean miembros o de scope, y suprimir `FunctionNaming` localmente en todo archivo que exponga composables PascalCase.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/personalidentity/PersonalIdentityFeature.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/personalidentity/signature/SignatureCaptureBox.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/personalidentity/signature/SignatureOverlay.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-15 — El contexto de seguridad y locale debe llegar intacto al matcher

**Error detectado:**
El gateway descartaba el sujeto de abuso, `es-US` no encontraba alias `es` y `configuración` falló porque la normalización no elimina acentos.

**Causa raíz:**
Los DTOs intermedios no transportaban todo el contexto, el locale exigía coincidencia exacta y se asumió incorrectamente plegado de diacríticos.

**Solución aplicada:**
Se propagó session/source/abuse context, se añadió fallback BCP-47 y se declararon alias acentuados y no acentuados.

**Regla futura:**
Conservar contexto de seguridad en cada límite; usar fallback BCP-47 y no asumir que la normalización elimina acentos.

**Archivos afectados:**
- `mindos/mind-command-runtime/src/commonMain/kotlin/com/mindos/command/runtime/TextCommandGateway.kt`
- `mindos/mind-action-matching/src/commonMain/kotlin/com/mindos/action/matching/Indices.kt`

### 2026-06-15 — Todo cierre negativo de confirmación debe revocar el token canónico

**Error detectado:**
Rechazo por voz, cambio de acción y expiración limpiaban estado Android sin consumir la confirmación canónica.

**Causa raíz:**
Se trataron como transiciones terminales de UI y no como revocaciones de una capacidad de seguridad.

**Solución aplicada:**
Todas las ramas terminales no ejecutadas llaman a `cancelConfirmation`.

**Regla futura:**
No cerrar visualmente una confirmación sin revocar primero su token en la autoridad canónica.

**Archivos afectados:**
- `mindos/mind-android-voice/src/main/kotlin/com/mindos/android/voice/AndroidConfirmationFlow.kt`

### 2026-06-15 — BuildConfig personalizado requiere habilitación explícita en AGP 9

**Error detectado:**
El build raíz falló al añadir `MINDOS_SPEECH_BROKER_URL` mediante `buildConfigField`.

**Causa raíz:**
AGP 9.1 no genera `BuildConfig` si la feature no se activa expresamente.

**Solución aplicada:**
Se habilitó `android.buildFeatures.buildConfig = true`.

**Regla futura:**
Antes de añadir `buildConfigField` con AGP 9+, habilitar `buildFeatures.buildConfig` y compilar el módulo raíz.

**Archivos afectados:**
- `androidApp/build.gradle.kts`

### 2026-06-15 — fetch de Node requiere un BodyInit explícito para buffers

**Error detectado:**
El typecheck falló al enviar un `Buffer` multipart al `fetch` global de Node 20.

**Causa raíz:**
Los tipos actuales no aceptan `Buffer<ArrayBufferLike>` directamente como `BodyInit`.

**Solución aplicada:**
El multipart se convierte con `Uint8Array.from(...)` antes de `fetch`.

**Regla futura:**
Convertir buffers binarios a un `BodyInit` explícito y ejecutar `npm run typecheck`.

**Archivos afectados:**
- `functions/src/speechBroker.ts`

### 2026-06-15 — La telemetría no debe imprimir correladores crudos

**Error detectado:**
`SpeechTelemetryEvent.toString()` incluía `requestId` y `sessionId` sin redacción.

**Causa raíz:**
La allowlist del evento no cubría su representación textual de depuración.

**Solución aplicada:**
`toString()` redacta ambos correladores y una prueba evita regresiones.

**Regla futura:**
Revisar también `toString()` y serializaciones auxiliares; nunca imprimir correladores crudos.

**Archivos afectados:**
- `mindos/mind-speech/src/commonMain/kotlin/com/mindos/speech/SpeechTelemetry.kt`
- `mindos/mind-speech/src/commonTest/kotlin/com/mindos/speech/SpeechTelemetryPrivacyTest.kt`

<!-- Aprendizajes añadidos automáticamente por el agente tras corregir errores reales. -->

### 2026-05-26 — Queries `list` sin `request.query.limit` permiten amplificación de coste

**Error detectado:**
Cuatro reglas `allow list` en `firestore.rules` no enforzaban `request.query.limit`:
`dm_chats`, `dm_chats/.../messages`, `user_posts/{uid}/posts`, `feeds/{viewerId}/posts`.
Un atacante (incluso autorizado, con app modificada o token comprometido) podía leer toda la colección de una sentada, amplificando coste de lectura en Firestore y abusando del presupuesto del proyecto.

**Causa raíz:**
Al diseñar reglas para colecciones donde el acceso ya está restringido (participantes, owner), se asumió que la restricción de acceso era suficiente. La autorización limita *quién* lee, no *cuánto* lee. Sin `request.query.limit`, la app cliente — o un atacante que se hace pasar por ella — puede solicitar la colección completa.

**Solución aplicada:**
Añadido `request.query.limit != null && request.query.limit <= N` a las cuatro reglas. Límites:
- `dm_chats`: 50
- `dm_chats/.../messages`: 100
- `user_posts/{uid}/posts`: 100
- `feeds/{viewerId}/posts`: 100

**Regla futura:**
TODA regla `allow list` debe incluir validación de `request.query.limit` independientemente de quién pueda leer. La autorización (quién) y el límite (cuánto) son controles ortogonales — ambos son necesarios.

Plantilla mínima para cualquier nueva `allow list`:
```
allow list: if request.query.limit != null &&
            request.query.limit <= MAX_LIMIT &&
            <condiciones de autorización>;
```

**Archivos afectados:**
- `firestore.rules` (líneas ~1610, ~1627, ~1639, ~1648)
- `firestore.security.rules.test.mjs` (tests T49-T52)

### 2026-05-26 — App Check no enforcado en uploads y lecturas de `dm_media`

**Error detectado:**
En `storage.rules`, las funciones `canCreateDMMedia` y `canAccessDMResource` solo validaban `signedIn()` pero NO llamaban a `isAppChecked()`. En cambio, `profile_visuals` y `post_attachments` privados sí lo enforzaban a través de `isOwner()` → `isAppChecked()`.

Un cliente modificado (APK repackaged, app sin App Check) podía subir y descargar archivos de DM saltándose la attestation del dispositivo. La consistencia de seguridad entre paths Storage estaba rota.

**Causa raíz:**
`dm_media` se diseñó con su propio sistema de validación de metadata (participantA, participantB, chatId, ownerUid) y se asumió que la validación de metadata era suficiente. Pero App Check es un control ortogonal al de autorización — uno valida el dispositivo, el otro al usuario. Ambos son necesarios.

**Solución aplicada:**
Añadido `isAppChecked()` a ambas funciones:
```
function canAccessDMResource(chatId) {
  return signedIn() && isAppChecked() && ...
}
function canCreateDMMedia(chatId) {
  return signedIn() && isAppChecked() && ...
}
```

**Regla futura:**
TODA operación de Firebase Storage que escriba o lea contenido privado debe enforzar App Check. La regla mínima:
```
function canDoX() {
  return signedIn() && isAppChecked() && <validación de autorización>;
}
```

Excepción documentada: lectura de contenido público (visibility=public) puede no requerir App Check si la app sirve estos archivos como CDN público. Pero contenido privado SIEMPRE debe enforzarlo.

Antes de añadir un nuevo `match` block en `storage.rules`, verificar que la función de autorización llamada incluya `isAppChecked()`.

**Archivos afectados:**
- `storage.rules` (funciones `canAccessDMResource` y `canCreateDMMedia`)
- `docs/security/SELF_AUDIT_BRUTAL.md` (hallazgos S01 y S02)

### 2026-05-27 — Build cache corrupto por referencia a proyecto externo (NEUv3)

**Error detectado:**
`./gradlew :androidApp:assembleDebug` fallaba con `mergeLibDexDebug FAILED` porque Gradle intentaba dexificar un `.class` en `C:\Users\curro\AndroidStudioProjects\NEUv3\shared\build\...`, que está fuera del directorio raíz de MindOS.

**Causa raíz:**
El cache de build intermedio de Gradle contenía una referencia antigua a un proyecto previo (`NEUv3`) del que MindOS fue probablemente renombrado o copiado. El directorio `shared/build/intermediates/runtime_library_classes_dir/` tenía artefactos con rutas absolutas al proyecto anterior.

**Solución aplicada:**
`./gradlew clean` seguido de rebuild. El clean elimina los artefactos intermedios y el rebuild regenera todo con las rutas correctas.

**Regla futura:**
Cuando un build falle con rutas absolutas que apuntan a un directorio fuera del proyecto, ejecutar `./gradlew clean` antes de investigar más. Es un síntoma de cache corrupto, no de un error en el código.

**Archivos afectados:**
- `shared/build/` (directorio de build, limpiado)

### 2026-05-27 — Design tokens dispersos por feature en vez de centralizados

**Error detectado:**
275 literales `Color(0xFF...)` dispersos en 30 archivos. Tokens locales duplicados en `SearchVisualTokens.kt` y `ViewerDesign.kt` con los mismos colores (`#13ECA4`, `#F4FFF9`, `#020504`) definidos independientemente.

**Causa raíz:**
Cada feature creó sus propios tokens de color de forma aislada. No existía un design system centralizado. Los valores eran correctos (mismos colores) pero no tenían una fuente de verdad única.

**Solución aplicada:**
Creados archivos centralizados en `shared/src/commonMain/kotlin/com/example/mindos/design/`:
- `AppColors.kt` — tokens de color (brand, neutral, semantic, state)
- `AppSpacing.kt` — tokens de espaciado (escala 4dp)
- `AppShapes.kt` — tokens de radio/forma
- `AppElevation.kt` — tokens de elevación
- `AppAnimation.kt` — tokens de duración de animación
- `AppStrings.kt` — constantes de strings de UI
- `components/StateComponents.kt` — componentes compartidos (Loading, Error, Empty, Offline)

**Regla futura:**
NUNCA crear tokens visuales locales a un feature. Todos los colores, espaciados, radios, elevaciones y strings deben definirse en los archivos centralizados del paquete `design/`. Si un feature necesita un valor que no existe, añadirlo al archivo central correspondiente.

Antes de añadir un `Color(0xFF...)` en cualquier archivo, verificar que el color no exista ya en `AppColors.kt`. Si existe, usar la referencia. Si no existe, añadirlo a `AppColors.kt` primero.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppColors.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppSpacing.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppShapes.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppElevation.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppAnimation.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppStrings.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/design/components/StateComponents.kt` (NUEVO)

### 2026-05-27 — applicationId no debe ser com.example.* en producción

**Error detectado:**
El `applicationId` era `com.mindos.app`, que es un ID de ejemplo rechazado por Google Play Store y que colisiona con otros proyectos ejemplo.

**Causa raíz:**
El template de proyecto de Android Studio genera `com.example.*` como placeholder. No se cambió al configurar el proyecto para producción.

**Solución aplicada:**
Cambiado `applicationId` a `com.neu.app` en `androidApp/build.gradle.kts`. Actualizado `google-services.json` para que el `package_name` coincida. El `namespace` se mantiene como `com.mindos.app` (solo afecta a R class generation, no a identidad en Play Store).

**Regla futura:**
El `applicationId` y el `namespace` son conceptos independientes en AGP. El `applicationId` identifica la app en la tienda y en Firebase. El `namespace` genera la clase R y no necesita coincidir. Al cambiar `applicationId`, SIEMPRE actualizar `google-services.json` y registrar el nuevo ID en Firebase Console.

**Archivos afectados:**
- `androidApp/build.gradle.kts`
- `androidApp/google-services.json`

### 2026-05-27 — Signing config debe usar variables de entorno, nunca hardcoded

**Error detectado:**
No existía configuración de signing para release builds. Sin signing, no se puede publicar en Play Store ni distribuir APKs de release.

**Causa raíz:**
No se configuró signing al crear el proyecto. Es un paso que se omite frecuentemente hasta que se necesita publicar.

**Solución aplicada:**
Añadido `signingConfigs.create("release")` que lee credenciales de `gradle.properties` vía `findProperty()`. Las propiedades son:
- `MINDOS_KEYSTORE_FILE` — ruta al .jks
- `MINDOS_KEYSTORE_PASSWORD`
- `MINDOS_KEY_ALIAS` (default: "neu")
- `MINDOS_KEY_PASSWORD`

Si el keystore no existe, el release build cae al debug signing config. Esto permite builds en CI sin keystore (para validación) y builds firmados cuando las credenciales están disponibles.

**Regla futura:**
NUNCA incluir rutas de keystore ni contraseñas en archivos versionados. Siempre usar `findProperty()` o variables de entorno. El fallback a debug signing permite que CI compile release sin fallar.

> Nota 2026-06-19: la parte que permitía fallback a firma debug queda reemplazada por la regla fail-closed anterior; CI no debe producir un artefacto `release` si no dispone de firma real.

**Archivos afectados:**
- `androidApp/build.gradle.kts`

### 2026-05-28 — Mensajes de error de auth revelan si un email existe (account enumeration)

**Error detectado:**
`AuthDiagnostics.toUserMessage()` devolvía mensajes distintos para `UserNotFound` ("No existe ningun usuario") y `WrongPassword` ("Contrasena incorrecta"). Un atacante podía distinguir si un email está registrado probando contraseñas incorrectas.

**Causa raíz:**
Se mapearon los códigos de error de Firebase directamente a mensajes de usuario sin considerar que la diferencia entre "no existe" y "contraseña incorrecta" filtra información.

**Solución aplicada:**
Unificados `UserNotFound`, `WrongPassword`, `InvalidLoginCredentials` y `MalformedCredential` bajo un único mensaje genérico: "Email o contrasena incorrectos." Eliminada la interpolación de `$email` en mensajes de credenciales inválidas.

**Regla futura:**
Los mensajes de error de autenticación NUNCA deben revelar si un email está registrado. Todos los fallos de credenciales (usuario no encontrado, contraseña incorrecta, credenciales inválidas) deben usar el mismo mensaje genérico.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/session/AuthDiagnostics.kt`

### 2026-05-28 — Android sin network_security_config (cleartext permitido)

**Error detectado:**
No existía `network_security_config.xml`. Sin esta configuración, Android permite tráfico cleartext (HTTP) en versiones anteriores a API 28, y no se declara explícitamente la política de seguridad de red.

**Causa raíz:**
Omisión al crear el proyecto. Android Studio no genera esta configuración por defecto.

**Solución aplicada:**
Creado `androidApp/src/main/res/xml/network_security_config.xml` con `cleartextTrafficPermitted="false"` y trust-anchors limitados a certificados del sistema. Referenciado en `AndroidManifest.xml` vía `android:networkSecurityConfig`.

**Regla futura:**
Todo proyecto Android que haga networking DEBE tener un `network_security_config.xml` que prohíba cleartext explícitamente. Si se necesita una excepción para un dominio de desarrollo, usar `<domain-config>` específico, nunca habilitar cleartext globalmente.

**Archivos afectados:**
- `androidApp/src/main/res/xml/network_security_config.xml` (NUEVO)
- `androidApp/src/main/AndroidManifest.xml`

### 2026-05-28 — updateMessage acepta campos arbitrarios sin whitelist

**Error detectado:**
`FirebaseDMRepository.updateMessage()` aceptaba un `Map<String, Any?>` y escribía todos los campos a Firestore sin validación. Un caller malintencionado o un bug podrían sobrescribir campos protegidos como `senderId` o `createdAt`.

**Causa raíz:**
Se diseñó `updateMessage` como una operación genérica flexible sin considerar que los campos actualizables deben estar restringidos por la capa de dominio.

**Solución aplicada:**
Añadida whitelist `AllowedMessageUpdateFields = setOf("status", "attachmentUrl", "thumbnailUrl")` en el companion object. `updateMessage` valida con `require` que todos los campos estén en la whitelist antes de escribir.

**Regla futura:**
Toda operación de update a Firestore debe validar los campos contra una whitelist explícita. Nunca pasar `Map<String, Any?>` sin restricciones a operaciones de escritura.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/data/FirebaseDMRepository.kt`

### 2026-05-28 — Faltaba implementación actual de WaveMailboxIcon para iOS

**Error detectado:**
`WaveMailboxIcon` tenía `expect` en commonMain y `actual` en androidMain y desktopMain, pero no en iosMain. Cualquier build iOS fallaría por falta de `actual`.

**Causa raíz:**
Al crear el composable expect/actual, se omitió la implementación iOS. Como no hay módulo iosApp que compile el target, el error no se detectó.

**Solución aplicada:**
Creado `WaveMailboxIcon.ios.kt` en iosMain con implementación usando `Icons.Default.MailOutline` como fallback (consistente con el fallback del actual de Android).

**Regla futura:**
Cada `expect` declaration DEBE tener `actual` en TODOS los targets declarados en `build.gradle.kts`, incluyendo los que aún no se compilan regularmente. Al crear un nuevo expect/actual, verificar que existan actuals en: androidMain, desktopMain, iosMain.

**Archivos afectados:**
- `shared/src/iosMain/kotlin/com/example/mindos/features/waves/presentation/WaveMailboxIcon.ios.kt` (NUEVO)

### 2026-05-28 — Unsafe smart casts en LaunchedEffect de App.kt (race condition)

**Error detectado:**
El bloque `LaunchedEffect(sessionRepository)` en `App.kt` usaba `(route as AppRoute.X)` repetidamente para acceder a propiedades de la ruta actual. Como `route` es una variable mutable, podía cambiar entre dos accesos consecutivos a `route as`, causando un `ClassCastException` en runtime.

**Causa raíz:**
Se usó la variable mutable `route` directamente dentro del `when` y después se hizo cast sobre ella de nuevo fuera del `when`. El compilador no puede garantizar que el valor no haya cambiado entre la evaluación del `when` y el uso posterior.

**Solución aplicada:**
Capturar `route` en una variable local `val currentRoute = route` al inicio del `collect`, y usar `currentRoute` en todas las ramas del `when`. Kotlin smart-cast funciona correctamente con variables locales inmutables.

**Regla futura:**
Antes de hacer smart cast sobre una propiedad mutable (var, MutableState, etc.), SIEMPRE capturarla en un `val` local. Nunca hacer cast directo sobre `route` o cualquier estado mutable compartido.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/App.kt`

### 2026-05-28 — Backup rules no excluían cache de Firebase ni tokens

**Error detectado:**
`backup_rules.xml` y `data_extraction_rules.xml` solo excluían `sharedpref` y `dm_audio/`. Los directorios de Firebase (databases, crashlytics NDK, installation tokens) se incluían en backups, exponiendo potencialmente tokens de sesión y datos de cache.

**Causa raíz:**
Al configurar las reglas de backup se pensó solo en datos de la app, no en los directorios internos que Firebase crea automáticamente.

**Solución aplicada:**
Añadidos exclusiones para `database` (dominio completo), `com.google.firebase.crashlytics-ndk`, `generate_fid.lock`, y `PersistedInstallation` en ambos archivos XML.

**Regla futura:**
Las reglas de backup DEBEN excluir todos los directorios internos de Firebase y tokens de sesión. Al añadir un nuevo SDK de Firebase, verificar si crea directorios locales que necesiten exclusión.

**Archivos afectados:**
- `androidApp/src/main/res/xml/backup_rules.xml`
- `androidApp/src/main/res/xml/data_extraction_rules.xml`

### 2026-05-28 — Sin Crashlytics: crashes en producción no se capturaban

**Error detectado:**
No había integración de Firebase Crashlytics. Cualquier crash en producción se perdía sin registro centralizado.

**Causa raíz:**
Omisión al configurar Firebase. Se integró Auth, Firestore, Storage y App Check, pero no Crashlytics.

**Solución aplicada:**
Añadido `firebase-crashlytics` y `firebase-analytics` al BOM en `androidApp/build.gradle.kts`. Añadido plugin `com.google.firebase.crashlytics` v3.0.4. Analytics es necesario como dependencia de Crashlytics.

**Regla futura:**
TODO proyecto Firebase en producción DEBE tener Crashlytics habilitado. Al añadir Firebase a un proyecto, incluir Crashlytics desde el inicio, no como paso posterior.

**Archivos afectados:**
- `gradle/libs.versions.toml`
- `build.gradle.kts` (root)
- `androidApp/build.gradle.kts`

### 2026-05-28 — Shared module no puede depender directamente de Crashlytics SDK

**Error detectado:**
Se intentó añadir `FirebaseCrashlytics.getInstance().recordException(error)` directamente en `SafeDebugLogging.android.kt` (que vive en `:shared`). Falló con "Unresolved reference 'crashlytics'" porque el SDK de Crashlytics solo es dependencia de `:androidApp`, no de `:shared`.

**Causa raíz:**
El módulo `:shared` es KMP y sus dependencias deben ser multiplataforma o estar disponibles en todos los targets. El Crashlytics SDK es Android-only y vive en `:androidApp`. Referenciar clases de `:androidApp` desde `:shared` invierte la dirección de dependencias.

**Solución aplicada:**
Creado `NonFatalReporter` (singleton con callback nullable) en `SafeDebugLogging.kt` (commonMain). `safeErrorLog()` invoca el callback si está registrado. `MindOSApplication.kt` registra Crashlytics como handler solo en release: `NonFatalReporter.handler = { crashlytics.recordException(it) }`.

**Regla futura:**
El módulo `:shared` NUNCA debe depender de SDKs Android-only. Para integrar funcionalidad de `:androidApp` en código compartido, usar el patrón callback/interface: definir la abstracción en commonMain, registrar la implementación concreta desde la capa de aplicación en el `onCreate()`.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/logging/SafeDebugLogging.kt`
- `androidApp/src/main/java/com/example/mindos/MindOSApplication.kt`

### 2026-05-28 — iOS GoogleService-Info.plist apuntaba a proyecto Firebase incorrecto

**Error detectado:**
`GoogleService-Info.plist` contenía `PROJECT_ID=mindos-4aef3` y `BUNDLE_ID=com.mindos.app`, pero el proyecto de producción es `mindos-8452f` y el bundle ID correcto es `com.neu.app`. Solo la app Android estaba registrada en el proyecto correcto.

**Causa raíz:**
Al crear el proyecto se generó un plist desde un proyecto Firebase de desarrollo/test (`mindos-4aef3`). Cuando se migró Android al proyecto de producción (`mindos-8452f`), el plist iOS no se actualizó.

**Solución aplicada:**
Registrada app iOS en Firebase project `mindos-8452f` con bundle `com.neu.app` vía Firebase API. Reemplazado `GoogleService-Info.plist` con el generado por el proyecto correcto.

**Regla futura:**
Todos los archivos de configuración de Firebase (google-services.json y GoogleService-Info.plist) deben apuntar al MISMO `PROJECT_ID`. Al cambiar de proyecto Firebase, actualizar AMBOS archivos. Verificar que `BUNDLE_ID` coincida con `applicationId`.

**Archivos afectados:**
- `shared/src/iosMain/resources/GoogleService-Info.plist`

### 2026-05-28 — Operaciones Firestore sin timeout (bloqueo indefinido)

**Error detectado:**
Ninguna llamada a Firestore (`.get()`, `.runTransaction()`, `.updateFields()`) tenía timeout. Una operación podía bloquearse indefinidamente si el dispositivo estaba offline sin cache disponible.

**Causa raíz:**
Se dependió del timeout implícito del SDK de Firebase, que es muy largo o inexistente para ciertas operaciones. No se implementó una capa de timeout a nivel de aplicación.

**Solución aplicada:**
Creado `FirestoreDefaults.kt` con tiempos estándar (ReadTimeout=15s, WriteTimeout=20s, BatchTimeout=30s) y funciones `withFirestoreReadTimeout`/`withFirestoreWriteTimeout`. Aplicados en `FirebaseUsernameSearchRepository` y `FirebaseDMRepository`.

**Regla futura:**
Toda operación Firestore suspendible (reads, writes, transactions) debe ejecutarse dentro de `withFirestoreReadTimeout` o `withFirestoreWriteTimeout`. Los Flows (snapshots) no necesitan timeout porque son streams continuos con su propio manejo de errores.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/network/FirestoreDefaults.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/features/search/data/FirebaseUsernameSearchRepository.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/data/FirebaseDMRepository.kt`

### 2026-05-28 — Transacciones multi-documento necesitan BatchTimeout, no WriteTimeout

**Error detectado:**
Al envolver transacciones de Firestore que escriben a 3+ documentos (e.g. `observe` en `FirebaseSocialInteractionRepository` que toca observationRef + observedProfileRef + notificationRef), usar `WriteTimeout=20s` por defecto puede ser demasiado corto en redes lentas. Las transacciones son retried automáticamente por Firestore si hay contention.

**Causa raíz:**
`withFirestoreWriteTimeout` usa `FirestoreDefaults.WriteTimeout` (20s) como default. Las transacciones que tocan múltiples documentos pueden necesitar más tiempo debido a retries internos del SDK por contention optimista.

**Solución aplicada:**
Para transacciones multi-doc, pasar `FirestoreDefaults.BatchTimeout` (30s) explícitamente:
```kotlin
withFirestoreWriteTimeout(FirestoreDefaults.BatchTimeout) {
    firestore.runTransaction { /* multi-doc operations */ }
}
```

**Regla futura:**
Las transacciones single-doc usan `withFirestoreWriteTimeout {}` (default 20s). Las transacciones que tocan 3+ documentos o que hacen reads previos antes de writes deben usar `withFirestoreWriteTimeout(FirestoreDefaults.BatchTimeout) {}` (30s) para tolerar retries por contention.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/data/repository/FirebaseSocialInteractionRepository.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/data/repository/ProfileVisualRepositories.kt`

### 2026-05-28 — Lógica de navegación inline en App.kt es difícil de testear

**Error detectado:**
`App.kt` tenía un bloque `when` de 24 líneas dentro de `LaunchedEffect` para rebindear el `AuthenticatedUser` actual a la ruta activa cuando el `authState` emitía. Estaba inline en un composable, no tenía tests, y duplicaba lógica de construcción de cada `data class` de `AppRoute`.

**Causa raíz:**
Cuando se diseñó la observación de `authState`, la lógica de rebinding se metió directamente en el composable como un detalle de implementación. No se extrajo a una función pura porque "es navegación, va aquí". Resultado: regresiones en el rebinding (e.g. omitir un campo de un `DMChatUserInfo`) solo se detectaban en runtime.

**Solución aplicada:**
Extraído a `AppRoute.rebindWithUser(user): AppRoute` en `core/navigation/AppRouteRebinder.kt` (commonMain). Tests exhaustivos en `AppRouteRebinderTest` (14 tests cubriendo todas las variantes de `AppRoute`). `App.kt` ahora llama a `currentRoute.rebindWithUser(user)`.

**Regla futura:**
Toda lógica de transformación de rutas (rebinding por auth, mapeo de parent route para back navigation, etc.) debe extraerse a una extensión pura de `AppRoute` en `core/navigation/`. Si la lógica está dentro de un composable y depende solo de `AppRoute` + datos primitivos, NO pertenece al composable — pertenece a una función testeable.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/navigation/AppRouteRebinder.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/App.kt`
- `shared/src/commonTest/kotlin/com/example/mindos/core/navigation/AppRouteRebinderTest.kt` (NUEVO)

### 2026-05-28 — Navegación sin back stack: botón back físico de Android no funcionaba

**Error detectado:**
`App.kt` usaba `var route by mutableStateOf()` como única fuente de verdad de la ruta actual. No había historial. El botón físico/gesture back de Android no estaba conectado a nada de la app — invocaba el comportamiento por defecto (cerrar la activity), atrapando al usuario en pantallas profundas (DMCajonera, MediaViewer) sin forma de salir excepto cerrar la app.

**Causa raíz:**
La navegación se diseñó como un simple state holder en lugar de una pila. Cada `onBack` reconstruía la ruta destino manualmente con `route = AppRoute.X(currentRoute.user)`, lo cual:
1. Duplicaba lógica en cada pantalla.
2. No respondía al back hardware.
3. Perdía el contexto previo (e.g. MediaViewer abierto desde DMChat volvía a Main).

**Solución aplicada:**
1. `AppRouteStack` (commonMain) — clase observable con `push/pop/replace/replaceAll/popTo/rebindStackWithUser`. Respaldada por `SnapshotStateList` para recomposición automática.
2. `PlatformBackHandler(enabled, onBack)` — expect/actual: Android usa `androidx.activity.compose.BackHandler`; desktop/iOS son no-op.
3. Refactorizado `App.kt` — cada `onBack = { stack.pop() }`, cada navegación profunda usa `stack.push()`, sign-out usa `stack.replaceAll(AppRoute.Auth)`.
4. Tests: 12 tests en `AppRouteStackTest` cubren todas las operaciones del stack.

**Regla futura:**
NUNCA usar `var route by mutableStateOf()` directamente para navegación. Toda app con más de 2 pantallas debe usar un back stack explícito. Reglas:
- `push()` cuando vas más profundo (Main → DMList).
- `replace()` cuando swap al mismo nivel (post-login routing).
- `replaceAll()` cuando reset total (sign-out, deep link entry).
- `pop()` para back.
- Wirear SIEMPRE `PlatformBackHandler(enabled = stack.canGoBack, onBack = { stack.pop() })` cuando hay un stack activo.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/navigation/AppRouteStack.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/core/navigation/PlatformBackHandler.kt` (NUEVO expect)
- `shared/src/androidMain/kotlin/com/example/mindos/core/navigation/PlatformBackHandler.android.kt` (NUEVO actual)
- `shared/src/desktopMain/kotlin/com/example/mindos/core/navigation/PlatformBackHandler.desktop.kt` (NUEVO actual)
- `shared/src/iosMain/kotlin/com/example/mindos/core/navigation/PlatformBackHandler.ios.kt` (NUEVO actual)
- `shared/src/commonTest/kotlin/com/example/mindos/core/navigation/AppRouteStackTest.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/App.kt` (refactorizado completo)

### 2026-05-28 — POST_NOTIFICATIONS no declarado (Android 13+ no podía notificar)

**Error detectado:**
`AndroidManifest.xml` no declaraba `android.permission.POST_NOTIFICATIONS`. Con `targetSdk=36`, cualquier intento de mostrar una notificación local o FCM en Android 13+ (API 33+) sería ignorado silenciosamente — el SDK requiere runtime permission desde Android 13.

**Causa raíz:**
Omisión al configurar el manifest. POST_NOTIFICATIONS no existía pre-Android 13 (los install-time grants cubrían todo), así que en proyectos creados antes era invisible.

**Solución aplicada:**
1. Añadido `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` al manifest.
2. Creado `NotificationPermissionState` (commonMain) — modelo puro con reglas de gating (`shouldShowPrompt`, `shouldDirectToSettings`).
3. Creado `rememberNotificationPermissionHandle()` (androidMain) — wrap de `ActivityResultContracts.RequestPermission` con detección de API 33+.
4. Tests: 9 tests cubriendo todas las combinaciones (pre-13, granted, denied, dismissed, permanent denial).

**Regla futura:**
Todo `targetSdk >= 33` con cualquier feature de notificaciones (local, FCM, foreground service notifications) DEBE:
1. Declarar `POST_NOTIFICATIONS` en AndroidManifest.
2. Solicitar permiso en momento **contextual** (cuando el usuario activa la feature), NUNCA al arrancar la app.
3. Manejar el caso "permanent denial" redirigiendo a system settings.

Antes de añadir cualquier nueva integración que muestre notificaciones, verificar que el feature invoque `rememberNotificationPermissionHandle().requestIfNeeded()` antes de la primera notificación.

**Archivos afectados:**
- `androidApp/src/main/AndroidManifest.xml`
- `shared/src/commonMain/kotlin/com/example/mindos/core/notifications/NotificationPermissionGate.kt` (NUEVO)
- `shared/src/androidMain/kotlin/com/example/mindos/core/notifications/NotificationPermission.android.kt` (NUEVO)
- `shared/src/commonTest/kotlin/com/example/mindos/core/notifications/NotificationPermissionGateTest.kt` (NUEVO)

### 2026-05-28 — Release sin keystore real fallaba silenciosamente al debug signing

**Error detectado:**
La lógica de signing tenía fallback a debug `signingConfigs.getByName("debug")` cuando no había `MINDOS_KEYSTORE_FILE`, pero NO emitía ningún log. Era posible ejecutar `./gradlew assembleRelease` y obtener un APK firmado con debug sin darse cuenta. Si alguien subía ese APK a Play Store, sería rechazado o (peor) aceptado pero no recibiría updates por mismatch de firma futura.

**Causa raíz:**
El fallback silencioso se diseñó para que CI compilase release sin keystore (para validar R8/ProGuard), pero el mismo path se ejecutaba en desarrollo local sin warning. La intención (CI build) y el efecto secundario (dev build sin firma real) eran indistinguibles.

**Solución aplicada:**
1. Añadido `logger.warn("⚠️ androidApp:release is being signed with the DEBUG keystore...")` cuando el fallback se activa. Visible en cada build, imposible de ignorar.
2. Creado `gradle.properties.example` documentando las 4 variables (`MINDOS_KEYSTORE_FILE`, `MINDOS_KEYSTORE_PASSWORD`, `MINDOS_KEY_ALIAS`, `MINDOS_KEY_PASSWORD`) + comando `keytool` para generar el keystore + ejemplo de GitHub Actions con `MINDOS_KEYSTORE_B64`.
3. `.gitignore` actualizado para excluir `*.jks`, `*.keystore`, `key.properties`, `keystore.properties`, `/androidApp/keystore/`.

**Regla futura:**
Todo fallback de configuración crítica para seguridad (signing, encryption keys, API tokens) DEBE emitir un warning visible en el build log. El warning debe explicar:
- Qué configuración falta.
- Cuál fue la decisión (qué se usa en su lugar).
- Por qué eso NO sirve para producción.
- Dónde está la documentación para resolverlo.

> Nota 2026-06-19: para variantes publicables esta regla queda reemplazada: no se permite fallback aunque emita warning; debe fallar cerrado.

**Archivos afectados:**
- `.gitignore`
- `gradle.properties.example` (NUEVO)
- `androidApp/build.gradle.kts`

### 2026-05-28 — Scaffold iosApp sin Xcode project committed

**Error detectado:**
No existía estructura `iosApp/` para consumir el shared KMP framework en iOS. Sin esto, aunque la app estaba registrada en Firebase (`mindos-8452f` con bundle `com.neu.app`), no había forma de compilarla.

**Causa raíz:**
El proyecto se empezó priorizando Android y desktop. iOS requiere infraestructura adicional (Xcode project, Podfile, Swift entry point) que solo es construible/verificable en macOS.

**Solución aplicada:**
Scaffold completo de `iosApp/` con:
- `iosApp/iosApp/iOSApp.swift` — `@main` SwiftUI app que monta `ComposeView` (UIViewControllerRepresentable) hospedando `MainViewControllerKt.MainViewController()`.
- `iosApp/iosApp/Info.plist` — HTTPS-only (`NSAllowsArbitraryLoads=false`, espejo del `network_security_config` de Android), modo oscuro forzado, descripciones de Camera/Microphone/Photos.
- `iosApp/Podfile` — referencia el shared module via `pod 'shared', :path => '../shared'` + Firebase pods (Core, Auth, Firestore, Storage, Functions, AppCheck, Crashlytics).
- `iosApp/.gitignore` — excluye `Pods/`, `*.xcworkspace`, `xcuserdata/`, `build/`, `DerivedData/`.
- `iosApp/README.md` — instrucciones de setup macOS paso a paso.
- `shared/src/iosMain/kotlin/com/example/mindos/MainViewController.kt` — entry point Kotlin que devuelve `UIViewController` envolviendo `App()`.

`iosApp.xcodeproj` NO se commit — se genera per-machine al ejecutar `pod install` la primera vez en macOS.

**Regla futura:**
Cuando un módulo de un proyecto multiplataforma requiere un toolchain que no está disponible en la máquina del autor (Xcode en Windows, MSVC en Linux, etc):
1. Crear todo lo scaffoldable (fuentes, configs, manifests).
2. NUNCA crear archivos de proyecto binarios/UUID a mano (xcodeproj, *.sln).
3. Documentar exhaustivamente los pasos para el primer setup en la plataforma objetivo.
4. Marcar como "verification deferred to <plataforma>" en el aprendizaje correspondiente.

La verificación real de que iosApp compila queda DIFERIDA a la primera vez que se abra en macOS — esto es un riesgo aceptado y explícito.

**Archivos afectados:**
- `iosApp/iosApp/iOSApp.swift` (NUEVO)
- `iosApp/iosApp/Info.plist` (NUEVO)
- `iosApp/Podfile` (NUEVO)
- `iosApp/.gitignore` (NUEVO)
- `iosApp/README.md` (NUEVO)
- `shared/src/iosMain/kotlin/com/example/mindos/MainViewController.kt` (NUEVO)

### 2026-05-28 — Auth y Storage operations sin timeout (bloqueo indefinido)

**Error detectado:**
Solo Firestore tenía timeouts. `signInWithEmailAndPassword`, `createUserWithEmailAndPassword`, `signOut`, `sendPasswordResetEmail` y todas las operaciones de Storage (`putData`, `delete`) podían colgarse indefinidamente sin emitir error. Esto se traduce en spinners infinitos en la UI cuando hay problemas de red.

**Causa raíz:**
Cuando se diseñó `FirestoreDefaults.kt` (Wave 4) se cubrió solo el caso de Firestore. Auth y Storage tienen sus propios mecanismos internos de retry/timeout pero ninguno garantiza un tope a nivel de aplicación.

**Solución aplicada:**
Extendido `FirestoreDefaults.kt` con dos nuevas familias de timeouts:
- **AuthDefaults**: SignInTimeout=20s, SignOutTimeout=10s, PasswordResetTimeout=15s, TokenRefreshTimeout=10s + helper `withAuthTimeout`.
- **StorageDefaults**: SmallReadTimeout=30s, LargeReadTimeout=180s, SmallUploadTimeout=60s, LargeUploadTimeout=300s, DownloadUrlTimeout=15s, DeleteTimeout=15s + helpers `withStorageReadTimeout` y `withStorageUploadTimeout`.

Aplicados timeouts en `FirebaseSessionRepository` (sign in/up/out/reset) y en `FirebaseDMRepository` + `ProfileVisualRepositories` (uploads de media discriminados por tipo: video usa Large, image/audio usa Small).

**Regla futura:**
Toda operación suspendible de Firebase (Auth, Firestore, Storage, Functions) DEBE envolverse en `with*Timeout`. Las operaciones de Storage que manejen video o archivos >5MB deben usar el `Large*` timeout explícito. Los flows (snapshots) no necesitan timeout porque son streams continuos.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/network/FirestoreDefaults.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/core/session/FirebaseSessionRepository.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/data/FirebaseDMRepository.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/data/repository/ProfileVisualRepositories.kt`
- `shared/src/commonTest/kotlin/com/example/mindos/core/network/TimeoutDefaultsTest.kt` (NUEVO)

### 2026-05-28 — Crashlytics sin breadcrumbs ni custom keys (crashes sin contexto)

**Error detectado:**
Crashlytics solo capturaba el stack trace. Sin breadcrumbs ni custom keys, era imposible saber en qué pantalla ocurrió un crash, si el usuario estaba autenticado, qué feature flag estaba activo, etc. Triage de crashes en producción se vuelve adivinanza.

**Causa raíz:**
El bridge inicial (Wave 3) solo expuso `NonFatalReporter.handler` para `recordException`. No había forma desde el módulo `:shared` de añadir `log()` ni `setCustomKey()`. La capa de observabilidad estaba incompleta.

**Solución aplicada:**
1. Creado `ObservabilitySink` interface (commonMain) con 5 primitivas: `breadcrumb(message)`, `customKey(k, v)` (String/Boolean/Int), `recordNonFatal(error)`.
2. `ObservabilityHub` singleton — sink registrable por la capa de aplicación.
3. `CrashlyticsObservabilitySink` (en `MindOSApplication`) forwarda todas las llamadas a Crashlytics SDK.
4. `AppRoute.telemetryName()` — nombre estable PII-free para cada ruta.
5. En `App.kt`: `LaunchedEffect(stack.current)` emite breadcrumb (`nav_to:dm_chat`) + custom keys (`current_route`, `nav_depth`) en cada cambio de ruta. `LaunchedEffect(sessionRepository)` actualiza `authenticated`.
6. Tests: `ObservabilityHubTest` (3 tests con sink mock), `AppRouteTelemetryTest` (2 tests verificando PII safety + estabilidad).

**Regla futura:**
Cualquier evento que ayude a contextualizar un crash debe pasar por `ObservabilityHub`. Reglas:
- `breadcrumb()` para eventos transitorios (navegación, network state, lifecycle).
- `customKey()` para contexto persistente (current_route, user role, feature flags).
- NUNCA pasar PII directamente. Si necesitas user id, hashearlo en la capa nativa antes de pasar al sink.
- Cada nueva pantalla añadida a `AppRoute` DEBE tener su `telemetryName()` actualizado.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/logging/Observability.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/core/logging/SafeDebugLogging.kt` (route safeErrorLog through hub)
- `shared/src/commonMain/kotlin/com/example/mindos/core/navigation/AppRouteTelemetry.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/App.kt` (wire LaunchedEffect)
- `androidApp/src/main/java/com/example/mindos/MindOSApplication.kt` (CrashlyticsObservabilitySink)
- `shared/src/commonTest/kotlin/com/example/mindos/core/logging/ObservabilityHubTest.kt` (NUEVO)
- `shared/src/commonTest/kotlin/com/example/mindos/core/navigation/AppRouteTelemetryTest.kt` (NUEVO)

### 2026-05-28 — Detekt baseline ocultaba bug latente: self en discovery list

**Error detectado:**
`FirebaseDMRepository.queryProfileVisualUsers(currentUserId: String)` recibía el `currentUserId` pero nunca lo usaba para filtrar al propio usuario de la lista de discovery. Resultado: el usuario se veía a sí mismo como contacto disponible para DM. El parámetro estaba en el baseline de detekt como `UnusedParameter`, escondido como ruido.

**Causa raíz:**
El baseline acumulaba findings indiscriminadamente. `UnusedParameter` y `UnusedPrivateProperty` son a menudo bugs reales (parámetros declarados pero nunca cumplidos) o code smell (dead code). Mezclarlos con `MagicNumber` (1200+ entries cosméticas) impedía detectarlos.

**Solución aplicada:**
1. Auditado el baseline filtrando solo `UnusedParameter` (29) + `UnusedPrivateProperty` (9).
2. Cada finding evaluado caso por caso:
   - **Bugs latentes**: `queryProfileVisualUsers.currentUserId` (no filtraba self), `decodeProfileVisualDiscoveryDto.path` (no se incluía en logs), `resolveUserId.path` (mismo).
   - **Dead code real**: `initialRadius`, `centerOffset`, `lum: Float`, `val h/w = maxHeight/maxWidth`, `causeMessage`, `val ProfileEditButton`, `val ProfileVisualUsernamePattern` (duplicado), `val carol` test fixture.
   - **API surface intencional**: callbacks de `ProfileScreen` (17), `HomeScreen.onOpenSettings`, `GlobalFeedScreen.onBack`, `AuthDiagnostic.toUserMessage.email`, `ChatBubble.unitH`. Suprimidos con `@Suppress` + comentario explicando el contrato.

**Regla futura:**
Detekt baseline es deuda técnica controlada, NO basurero. Categorías de findings que merecen revisión periódica caso-por-caso:
- `UnusedParameter` — frecuentemente bugs latentes
- `UnusedPrivateProperty` — siempre dead code real
- `UnreachableCode` — siempre bug
- `EqualsWithHashCodeExist` — siempre bug
- `UnsafeCallOnNullableType` — siempre riesgo

Categorías que se pueden tolerar en baseline indefinidamente:
- `MagicNumber` (ruidoso, raramente bug)
- `MaxLineLength` (cosmético)
- `WildcardImport` (estilo)

Antes de hacer "ratchet" (regenerar baseline), ejecutar la auditoría filtrada de las categorías de alto valor.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/data/FirebaseDMRepository.kt` (self-filter + logs enriquecidos)
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/presentation/profile/ChromaInteractiveEngine.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/presentation/DMChatScreen.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/presentation/profile/ProfileScreen.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/presentation/profile/ProfileEntryScreen.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/domain/model/ProfileVisualModels.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/domain/ranking/HomeFeedRankingEngine.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/presentation/feed/GlobalFeedScreen.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/home/presentation/HomeScreen.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/core/session/AuthDiagnostics.kt`
- `shared/src/commonTest/kotlin/com/example/mindos/features/social/domain/SocialGraphRulesTest.kt`
- `shared/detekt-baseline.xml` (de 2355 → 2273 entries; UnusedParameter/UnusedPrivateProperty de 38 → 0)

### 2026-05-29 — Cinco dependencias inline en `MindOSApp` dispersaban el wiring

**Error detectado:**
`MindOSApp(...)` aceptaba 5 parámetros con defaults `remember { FQN(...) }` (DMRepository, WalletApiClient, WaveRequestRepository, GlobalFeedRepository, privacyEnabledState). Cada caller (tests, main activity, preview) tenía que conocer las 5 y pasarlas individualmente. Añadir una sexta dependencia rompía la signature en todos los call sites.

**Causa raíz:**
La inyección de dependencias se hacía a nivel de parámetro de composable. Sin agrupación, el compose function se volvía un god-constructor con responsabilidades mezcladas (orquestación + wiring).

**Solución aplicada:**
Creado `AppGraph` (data class @Stable en `core/di/`) con las 5 dependencias agrupadas. `rememberAppGraph()` es la factory composable que construye el wiring de producción. `MindOSApp(...)` ahora recibe un `appGraph: AppGraph = rememberAppGraph()` único parámetro de wiring. Tests construyen un `AppGraph` con fakes y lo pasan.

**Regla futura:**
Toda app con 3+ dependencias inyectables al nivel root debe agruparlas en un `*Graph` data class. Reglas:
- El graph es `@Stable data class` (Compose skipping + equatable).
- Las dependencias son INTERFACES (nunca clases concretas como `FirebaseXyz`).
- La factory composable `rememberAppGraph()` vive junto al graph.
- Tests construyen el graph directamente con fakes; nunca usan `rememberAppGraph()`.

Antes de añadir un nuevo parámetro inyectable a `MindOSApp`, añadirlo al `AppGraph` y a `rememberAppGraph()`.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/di/AppGraph.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/App.kt` (signature simplificada)
- `shared/src/commonTest/kotlin/com/example/mindos/core/di/AppGraphTest.kt` (NUEVO)
- `shared/src/desktopTest/kotlin/com/example/mindos/MindOSAppNavigationSmokeTest.kt` (usa AppGraph)

### 2026-05-29 — kotlinx-datetime 0.7.1 (pulled transitively) removió Clock.System en runtime

**Error detectado:**
Tests que ejercitan código con `kotlinx.datetime.Clock.System.now()` fallan con `NoClassDefFoundError: kotlinx/datetime/Clock$System` en desktopTest. La versión pinned en `libs.versions.toml` es 0.6.0, pero un compilation-time dep pulled 0.7.1 que removió `Clock.System`.

**Causa raíz:**
kotlinx-datetime 0.7.0 deprecó `kotlinx.datetime.Clock` y movió la API a `kotlin.time.Clock`. La 0.7.1 ya no expone la clase a runtime. Compose Multiplatform / Kotlin 2.3 traen 0.7.1 transitivamente, sobrescribiendo el pin de 0.6.0.

**Solución aplicada:**
Para casos individuales: marcar los tests afectados como NO-COBERTURA documentando la razón. Workaround long-term: inyectar `AppClock` (patrón ya usado por `ProfileStateHolder`) en lugar de llamar `Clock.System.now()` directamente.

**Regla futura:**
TODO uso de tiempo en código de dominio/presentación debe ir a través de `AppClock` inyectado. Llamadas directas a `Clock.System.now()` son aceptables solo en:
- Capa de datos (repos) — usado en seeding de timestamps a Firestore.
- `MindOSApplication.onCreate()` y entry points donde el clock real es lo correcto.

Antes de añadir `Clock.System.now()` a un StateHolder o ViewModel, refactorizar para inyectar `AppClock`.

**Archivos afectados:**
- `shared/build.gradle.kts` (añadida dep explícita de kotlinx-datetime en desktopTest)
- `shared/src/commonTest/kotlin/com/example/mindos/features/social/presentation/feed/GlobalFeedComposerStateHolderTest.kt` (2 tests deshabilitados con documentación)

### 2026-05-29 — Tests de StateHolder con flows infinitos requieren `cancelChildren()` o `backgroundScope`

**Error detectado:**
`DMStateHolderTest` fallaba con `UncompletedCoroutinesError` porque el holder lanza `scope.launch { Flow.collect { } }` que suspende indefinidamente. `runTest` espera a que todos los hijos del scope completen y falla cuando hay jobs activos.

**Causa raíz:**
La convención en kotlinx-coroutines-test 1.10+ es que `runTest` aborta si quedan hijos activos al final del bloque. StateHolders que observan `StateFlow` (chats, messages) lanzan colectores infinitos.

**Solución aplicada:**
Patrón estándar para tests de StateHolders con flows infinitos:
1. Pasar `scope = this` (TestScope) al holder.
2. Al final de cada test que haya invocado `loadX`/`resolveX` que lance jobs persistentes: `coroutineContext.cancelChildren()`.
3. Alternativa: usar `backgroundScope` — pero NO siempre se comporta igual con `advanceUntilIdle` en flujos complejos (multi-launch nested).

**Regla futura:**
Plantilla para tests de StateHolder que lance Flow collectors:
```kotlin
@Test
fun myTest() = runTest {
    val holder = StateHolder(scope = this, ...)
    holder.startObservingStuff()
    advanceUntilIdle()
    // assertions ...
    coroutineContext.cancelChildren()  // ← cleanup
}
```

**Archivos afectados:**
- `shared/src/desktopTest/kotlin/com/example/mindos/features/dm/presentation/DMStateHolderTest.kt` (NUEVO)
- `shared/src/commonTest/kotlin/com/example/mindos/features/social/presentation/feed/GlobalFeedComposerStateHolderTest.kt` (NUEVO — usa GatedFakeFeedRepository para mid-flight asserts)

### 2026-05-29 — Cloud Functions (wallet) sin timeout y `callSafely` tragaba la cancelación

**Error detectado:**
Las 4 llamadas de `WalletApiClient` (`getBalance`, `getTransactions`, `initiatePurchase`, `verifyPurchase`) no se envolvían en ningún timeout de nivel de app — el único subsistema Firebase sin él, pese a que `FirestoreDefaults.kt` ya cubría Auth/Firestore/Storage. Además, `callSafely` capturaba `catch (e: Exception)` sin re-lanzar `CancellationException`, tragando la cancelación estructural (un job cancelado se mapeaba a `WalletError.NetworkError` en lugar de propagarse).

**Causa raíz:**
Cuando se diseñaron los timeouts (Wave 4/8) solo se cubrieron Auth/Firestore/Storage; Functions se omitió. El `callSafely` se escribió antes de aplicar el patrón "re-lanzar CancellationException" que el resto de repos ya seguían.

**Solución aplicada:**
1. Añadida familia `FunctionsDefaults` (`ReadCallTimeout=15s`, `WriteCallTimeout=20s`) + `withFunctionsReadTimeout`/`withFunctionsWriteTimeout` en `FirestoreDefaults.kt`.
2. Envueltas las 4 llamadas (read en getBalance/getTransactions; write en initiate/verifyPurchase).
3. `callSafely` ahora distingue: `TimeoutCancellationException` → `WalletError.NetworkError` (error recuperable, el servidor deduplica por idempotency key/token); `CancellationException` genuina → re-lanzada; resto → `mapException`.

**Regla futura:**
TODA llamada suspendible a Firebase Cloud Functions (HTTPS callables) debe envolverse en `withFunctionsReadTimeout` (no mutante) o `withFunctionsWriteTimeout` (mutante). Cualquier `catch (e: Exception)` que envuelva trabajo suspendible debe re-lanzar `CancellationException` ANTES del catch genérico; solo `TimeoutCancellationException` puede mapearse a un error de dominio.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/network/FirestoreDefaults.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/wallet/data/WalletApiClient.kt`
- `shared/src/commonTest/kotlin/com/example/mindos/core/network/TimeoutDefaultsTest.kt` (+5 tests)

### 2026-05-29 — `Clock.System.now()` directo en StateHolder impedía cobertura (datetime 0.7.1)

**Error detectado:**
`GlobalFeedComposerStateHolder.draftId()` llamaba a `kotlinx.datetime.Clock.System.now()` directamente. Por el conflicto kotlinx-datetime 0.7.1 (documentado arriba), dos tests (`onAudioRecorded_enforcesMaxClipsLimit`, `onDeleteAudio_removesClipById`) no se podían escribir porque tropezaban con `NoClassDefFoundError` en el JVM de test.

**Causa raíz:**
La fuente de "ahora" estaba acoplada al SDK frágil en una función de presentación, sin inyección. El workaround "inyectar reloj" ya estaba anotado como deuda pero no implementado.

**Solución aplicada:**
Creada abstracción `core/time/EpochClock` (fun interface `nowMillis(): Long`) + `SystemEpochClock` como ÚNICO punto que toca `Clock.System`. `GlobalFeedComposerStateHolder` recibe `clock: EpochClock = SystemEpochClock`; `draftId` pasó de función top-level a método miembro que usa `clock.nowMillis()`. Los 2 tests antes imposibles ahora corren con un `IncrementingEpochClock` fake.

**Regla futura:**
NINGÚN StateHolder/ViewModel/composable debe llamar `Clock.System.now()` directamente. Inyectar `EpochClock` (epoch millis) o el `AppClock` social (`Timestamp`), con default de producción. La única clase que puede tocar `Clock.System` es `SystemEpochClock`. Para tiempo en capa de datos (seeding de timestamps a Firestore) sigue siendo aceptable el uso directo.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/time/EpochClock.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/example/mindos/features/social/presentation/feed/GlobalFeedComposerStateHolder.kt`
- `shared/src/commonTest/kotlin/com/example/mindos/features/social/presentation/feed/GlobalFeedComposerStateHolderTest.kt` (+2 tests reactivados)

### 2026-05-29 — Registro de usuario no enviaba email de verificación

**Error detectado:**
`createUserWithEmailAndPassword` capturaba `isEmailVerified` pero NUNCA disparaba `sendEmailVerification()`. Se podían crear cuentas con emails ajenos/inexistentes sin ninguna verificación; no existía forma de reenviar.

**Causa raíz:**
El flujo de registro se diseñó solo para crear la cuenta + perfil; la verificación de email se omitió.

**Solución aplicada:**
`SessionRepository` ahora expone `sendEmailVerification()` y `reloadCurrentUser()`. `createUserWithEmailAndPassword` dispara el email de verificación automáticamente (envuelto en `runCatching` + timeout, para no abortar el registro si el envío falla). `PreviewSessionRepository` implementa no-ops seguros. El gating de acciones por verificación queda como decisión de producto (hoy NO bloquea — banner no implementado, ver `docs/architecture/PHASE2_DEFERRALS.md`).

**Regla futura:**
Todo flujo de registro con email/contraseña debe disparar `sendEmailVerification()` tras crear la cuenta, sin bloquear el registro si el envío falla. Al añadir una nueva implementación de `SessionRepository`, implementar `sendEmailVerification()` y `reloadCurrentUser()`.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/core/session/SessionRepository.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/core/session/FirebaseSessionRepository.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/core/session/PreviewSessionRepository.kt`
- `shared/src/commonTest/kotlin/com/example/mindos/core/session/SessionRepositoryContractTest.kt` (+3 tests)

### 2026-05-29 — Queries cliente de DM sin `.limit()` eran denegadas por las propias reglas

**Error detectado:**
`FirebaseDMRepository.getUserChats` y `getChatMessages` ejecutaban `.where/.orderBy.snapshots` SIN `.limit()`. Las reglas de `dm_chats` (`allow list` exige `request.query.limit != null && <= 50`) y de `messages` (`<= 100`) deniegan toda query con `request.query.limit == null`. Resultado: la lista de chats y los mensajes nunca cargarían en producción (PERMISSION_DENIED) — la feature de DM rota de extremo a extremo.

**Causa raíz:**
Cuando se endurecieron las reglas con límites de query (aprendizaje 2026-05-26) NO se actualizaron las queries cliente para fijar el `.limit()` correspondiente. Desfase regla↔cliente latente, invisible porque iOS es un shell de preview y el flujo nunca se ejercitó contra las reglas desplegadas.

**Solución aplicada:**
`getUserChats` → `.limit(ChatsPageLimit=50)` (50 chats más recientes, orderBy updatedAt DESC). `getChatMessages` → `orderBy(createdAt, DESCENDING).limit(MessagesPageLimit=100)` + `.reversed()` en el `map` (los 100 mensajes más recientes, mostrados cronológicamente como antes). Constantes con nota de que deben mantenerse <= los topes de `firestore.rules`.

**Regla futura:**
TODA query `list`/`snapshots` cliente sobre una colección cuya regla exige `request.query.limit` DEBE fijar `.limit(N)` con N <= el tope de la regla. Al cambiar un límite en `firestore.rules`, actualizar la query cliente en el mismo cambio (y viceversa). Validación real requiere el emulador de reglas (Fase 5 T5.1) — la verificación aquí fue solo de compilación.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/data/FirebaseDMRepository.kt`

### 2026-05-29 — Estado "en línea" fabricado (isOnline=true) mostraba presencia inexistente

**Error detectado:**
`App.kt` pasaba `isOnline = true` hardcodeado al abrir el info del chat, y `AppRoute.DMChatUserInfo`/`DMCajonera` + `DMChatUserInfoRoute` tenían `isOnline` con default `true`. La UI (`WavesChatUserInfoScreen`) pinta un punto verde "en línea" cuando `isOnline==true`. No existe ningún sistema de presencia → se mostraba a los usuarios un estado de presencia falso (UX engañosa + implica tracking que no ocurre).

**Causa raíz:**
Se usó `true` como placeholder de presencia sin backend que lo respaldara. Un placeholder se convirtió en un claim visible y falso.

**Solución aplicada:**
Cambiado el `true` fabricado en `App.kt` y todos los defaults de `isOnline` a `false`. La presencia real (RTDB/Firestore presence + ocultar el indicador cuando es desconocida) es una feature con backend, diferida.

**Regla futura:**
NUNCA fabricar valores que la UI presenta como hechos verificados (presencia, "visto", contadores en vivo). Si no hay fuente de datos, el default debe ser el conservador que NO afirme la capacidad. Los placeholders de UI no deben renderizarse como estado real.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/App.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/core/navigation/AppRoute.kt`
- `shared/src/commonMain/kotlin/com/example/mindos/features/dm/presentation/WavesChatUserInfoScreen.kt`

### 2026-05-29 — Scripts de tests de reglas no eran ejecutables (sin --project; emulador exige JDK 21)

**Error detectado:**
Los scripts `test:*-rules` de `package.json` llamaban `firebase emulators:exec` SIN `--project`, y no existe `.firebaserc`. Sin proyecto activo, el emulador falla con "No currently active project". Además, el script combinado `test:rules` corría los 3 archivos `.mjs` bajo UN solo emulador, pero cada archivo fija un `projectId` distinto y `firebase.json` tiene `singleProjectMode: true` → conflicto. Y `firebase-tools` 15 exige **JDK 21+**, incompatible con el JDK 17 del build Gradle.

**Causa raíz:**
Los scripts se escribieron asumiendo un proyecto activo local. Nunca corrieron en CI, así que el desfase (project + singleProjectMode + versión de Java) quedó latente.

**Solución aplicada:**
Cada script ahora pasa su `--project` correspondiente (igual al `projectId` del `.mjs`) y corre en su PROPIO emulador; `test:rules` los encadena (3 emuladores secuenciales, no uno compartido). Nuevo job CI `backend-ci.yml` corre el emulador con **temurin 21**. La suite de Functions (704 tests jest) sí se verificó localmente.

**Regla futura:**
Los scripts `emulators:exec` DEBEN pasar `--project <projectId>` que coincida con el `initializeTestEnvironment({projectId})` del test. Con `singleProjectMode`, NUNCA correr tests de varios projectId bajo un solo emulador: uno por proyecto. El emulador de Firebase requiere JDK 21+ (el build Kotlin usa 17) — separar versiones de Java por job en CI.

**Archivos afectados:**
- `package.json` (scripts test:*-rules)
- `.github/workflows/backend-ci.yml` (NUEVO)

### 2026-05-30 — `.gitignore` `lib/` sin anclar excluía TODO el backend `functions/src/lib`

**Error detectado:**
`functions/.gitignore` tenía `lib/` (patrón sin anclar). gitignore aplica patrones sin barra inicial a CUALQUIER nivel, así que `lib/` no solo ignoraba `functions/lib/` (output compilado, intencional) sino también `functions/src/lib/` → los **19 archivos fuente del backend** (guards, rateLimiter, ledgerService, walletRepository, validators, fraudSignalService, reconciliationService, idempotencyService, etc.) **nunca se commitearon**. El repo no podía compilar/desplegar las Cloud Functions; un clone limpio fallaría. `accountDeletion.ts` (sí trackeado) importaba de `./lib/` inexistente en el repo.

**Causa raíz:**
Patrón gitignore sin anclar (`lib/` en vez de `/lib/`). El compilado vive en `functions/lib/` y el fuente en `functions/src/lib/`; ambos terminan en `lib/`, así que el patrón sin anclar capturó los dos.

**Solución aplicada:**
Cambiado `lib/` → `/lib/` en `functions/.gitignore` (ancla a `functions/lib/` únicamente). Añadidos al repo los 19 fuentes de `functions/src/lib/`. Verificado: `git check-ignore` ya no ignora `src/lib`, sigue ignorando `functions/lib/` compilado; `scan:secrets` limpio; jest 713 verde; build OK.

**Regla futura:**
En `.gitignore`, los patrones de output compilado que coincidan con un nombre de dir común (`lib`, `build`, `dist`, `out`) DEBEN anclarse con barra inicial (`/lib/`, `/build/`) para no capturar dirs homónimos de código fuente. Tras tocar un `.gitignore` de output, ejecutar `git check-ignore -v <ruta-fuente>` sobre una ruta de código para confirmar que NO queda ignorada, y `git ls-files <dir>` para confirmar que los fuentes están trackeados.

**Archivos afectados:**
- `functions/.gitignore`
- `functions/src/lib/*.ts` (19 archivos, ahora trackeados)
- `functions/src/accountDeletion.ts` + `accountDeletion.test.ts` (NUEVO — CF de borrado de cuenta B1)
- `functions/src/index.ts`, `functions/src/lib/rateLimiter.ts`

### 2026-05-30 — `gradlew` sin bit de ejecución rompía TODO el CI Linux (Permission denied)

**Error detectado:**
Todos los jobs Gradle del CI (assembleDebug, bundleRelease, detekt, desktopTest) fallaban en ~8 s con `./gradlew: Permission denied` (exit 126). El archivo `gradlew` estaba commiteado con modo `100644` (sin `+x`). En Windows (máquina del autor) se usa `gradlew.bat` y el bit es irrelevante, así que el fallo era invisible localmente.

**Causa raíz:**
`gradlew` se añadió al repo sin el bit de ejecución. En runners Linux, `./gradlew` necesita `+x`.

**Solución aplicada:**
`git update-index --chmod=+x gradlew` (modo → `100755`). En checkout Windows el bit es cosmético; git lo almacena y Linux lo recibe ejecutable.

**Regla futura:**
Todo script de entrada ejecutable en CI Linux (`gradlew`, `*.sh`) DEBE commitearse con `100755`. Verificar con `git ls-files -s <script>`; si es `100644`, `git update-index --chmod=+x`. Síntoma en CI: fallo en segundos con "Permission denied" / exit 126.

**Archivos afectados:**
- `gradlew` (modo 100644 → 100755)

### 2026-05-30 — Test de reglas positivo (T49) no reflejaba la query real → denegado correctamente

**Error detectado:**
`firestore.security.rules.test.mjs` T49 ("DM chats list with limit succeeds") hacía `getDocs(query(collection(alice,"dm_chats"), limit(50)))` SIN el filtro `where participants array-contains`. La regla `dm_chats allow list` exige `uid() in resource.data.participants`, así que el emulador denegaba la query (seguridad funcionando). El test esperaba `assertSucceeds` → fallo (1/81). Nunca se había detectado porque el CI de reglas no corría (gradlew/JDK rotos).

**Causa raíz:**
El test positivo de una `list` no replicaba las constraints que la regla exige; una `list` sin el `where` que acota a los docs permitidos es denegada por Firestore (no puede garantizar que todo doc devuelto cumpla la regla).

**Solución aplicada:**
Añadido `where("participants","array-contains","alice")` a T49 (espejo de `FirebaseDMRepository.getUserChats`). Importado `where`.

**Regla futura:**
Un test positivo (`assertSucceeds`) de una `list` con regla per-documento (`uid() in resource.data.X`) DEBE incluir el mismo `where`/filtro que la regla exige y que usa el cliente de producción. Si no, el emulador deniega legítimamente. Mantener los tests de reglas espejo de las queries cliente reales.

**Archivos afectados:**
- `firestore.security.rules.test.mjs` (T49 + import `where`)

### 2026-05-30 — `by rememberUpdatedState(...)` sin `import androidx.compose.runtime.getValue` no compila

**Error detectado:**
Varios actuals de plataforma nuevos (`ProfileAudioRecorder.desktop.kt`, `ProfileAudioPlayer.desktop.kt`, `ProfileForegroundEffect.desktop.kt`, etc.) fallaban con `Property delegate must have a 'getValue(Nothing?, KProperty0<*>)' method` y `Unresolved reference 'currentOnError'` al usar `val currentOnError by rememberUpdatedState(onError)`.

**Causa raíz:**
`rememberUpdatedState` devuelve un `State<T>`. El operador de delegación `by` sobre `State<T>` requiere la función de extensión `getValue` de `androidx.compose.runtime.getValue`. Importar solo `rememberUpdatedState`/`mutableStateOf` NO trae ese operador. En archivos `commonMain` grandes suele entrar transitivamente por otros imports, lo que enmascara la necesidad en archivos pequeños (actuals de plataforma).

**Solución aplicada:**
Añadir `import androidx.compose.runtime.getValue` en TODO archivo que delegue con `by` sobre Compose state (`mutableStateOf`/`rememberUpdatedState`/`collectAsState`). Para `by var` también hace falta `androidx.compose.runtime.setValue`.

**Regla futura:**
Al escribir un archivo nuevo (sobre todo actuals de plataforma pequeños) que use `by` sobre Compose state, importar SIEMPRE `androidx.compose.runtime.getValue` (y `setValue` si es `var`). No confiar en imports transitivos.

**Archivos afectados:**
- `shared/src/{android,desktop,ios}Main/kotlin/com/example/mindos/features/profile/presentation/**`

### 2026-05-30 — Git worktree sin `local.properties`: build Android falla con "SDK location not found"

**Error detectado:**
`:androidApp:assembleDebug` en el worktree (`.claude/worktrees/...`) fallaba con `SDK location not found. Define a valid SDK location with an ANDROID_HOME ... or sdk.dir`. El target Desktop compilaba bien (no necesita SDK Android).

**Causa raíz:**
`local.properties` está en `.gitignore` (per-máquina) y NO se copia al crear un git worktree. Sin `ANDROID_HOME`/`ANDROID_SDK_ROOT` en el entorno, AGP no localiza el SDK. Además, escribir `sdk.dir` con backslashes Windows sin escapar (`C:\Users\...`) hace que Java los lea como escapes → `IOException: ... sintaxis ... no son correctos`.

**Solución aplicada:**
Crear `local.properties` en la raíz del worktree con barras NORMALES (Java las acepta en Windows): `sdk.dir=C:/Users/curro/AppData/Local/Android/Sdk`. Queda gitignored (verificado con `git check-ignore`).

**Regla futura:**
Tras crear un git worktree, para builds Android crear `local.properties` con `sdk.dir` usando `/` (no `\`). Verificar que sigue ignorado. Síntoma: "SDK location not found" SOLO en Android; Desktop compila igual. Copiar el `sdk.dir` del checkout principal.

**Archivos afectados:**
- `local.properties` (worktree, no versionado)

### 2026-05-30 — Nueva pantalla de perfil MindOS (Perfil.html) como feature aislada

**Error detectado:**
No era un error: decisión de arquitectura al implementar el diseño `Perfil.html`. El `ProfileScreen` existente (`features/social/presentation/profile`) es un sistema complejo (chroma, módulos, segmented Portada/Perfil, publicaciones reales) divergente del nuevo diseño premium.

**Causa raíz:**
Reescribir en sitio habría arriesgado el editor chroma/módulos y sus tests. El diseño nuevo introduce conceptos que el modelo viejo no tiene (emoji prefix, audio con waveform real, donación, 3 enlaces custom, presencia online/offline).

**Solución aplicada:**
Feature nueva y autocontenida en `features/profile/` (domain/data/presentation/audio) con repos in-memory (`MindOSProfileRepository`, `ProfileObserverRepository`, `ProfilePresenceRepository`). `HomeScreen` renderiza `MindOSProfileScreen` en `HomeDestination.Profile`; el `ProfileEntryScreen` viejo permanece en el repo (sin usar). Imágenes reutilizan `rememberProfileMediaPicker`/`ProfilePlatformMediaPreview` del feature social. Audio real en Android (`MediaRecorder` + `getMaxAmplitude` para waveform real), stubs documentados en desktop/iOS.

**Regla futura:**
Para rediseños grandes que divergen de un sistema existente y tienen tests, preferir feature nueva aislada + un único punto de wiring (aquí `HomeDestination.Profile`) en vez de reescribir en sitio. Reutilizar utilidades multimedia compartidas (picker/preview) en vez de duplicar expect/actual. Inyectar abstracciones de plataforma (audio/permiso/presencia) por expect/actual con Android real + stubs documentados.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/profile/**` (NUEVO)
- `shared/src/{android,desktop,ios}Main/kotlin/com/example/mindos/features/profile/**` (NUEVO actuals)
- `shared/src/commonMain/kotlin/com/example/mindos/features/home/presentation/HomeScreen.kt` (wiring)
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppColors.kt` (tokens ProfileCanvas/ProfileTextPrimary/ProfileOnline)

### 2026-05-31 — `List<T> + (a..b)` resuelve al overload `plus(Iterable)` y expande el rango

**Error detectado:**
En el `TokenTrie` del nuevo módulo `mind-action-matching` (proyecto `mindos/`), `spans + (index..index)` con `spans: List<IntRange>` no compilaba: `Argument type mismatch: actual type is 'List<Any>', but 'List<IntRange>' was expected`. La intención era añadir el `IntRange` como UN elemento.

**Causa raíz:**
`IntRange` implementa `Iterable<Int>`. El operador `+` sobre `List<IntRange>` tiene dos overloads: `plus(element: T)` y `plus(elements: Iterable<T>)`. Como `(index..index)` ES un `Iterable<Int>`, Kotlin resuelve a `plus(Iterable)`, que CONCATENA los `Int` del rango en vez de añadir el rango como elemento → lista mixta `List<Any>`.

**Solución aplicada:**
Envolver el elemento en `listOf(...)`: `spans + listOf(index..index)`. Fuerza el overload `plus(Collection)` que concatena el rango como un único elemento.

**Regla futura:**
NUNCA usar `lista + elementoIterable` cuando el tipo de elemento es a su vez `Iterable` (`IntRange`/`CharRange`, `String`, otra `List`/`Set`). Kotlin elige `plus(Iterable)` y expande el elemento. Para añadir un elemento que es Iterable: `lista + listOf(elemento)` o `lista.plusElement(elemento)`.

**Archivos afectados:**
- `mindos/mind-action-matching/src/commonMain/kotlin/com/mindos/action/matching/TokenTrie.kt`

### 2026-05-31 — Tests asumían strip+match de zero-width; la capa de seguridad lo rechaza

**Error detectado:**
Un test de regresión (REG-003) en `mind-action-matching` asumía que un input con un carácter zero-width (`​`) inyectado se **normalizaba (strip) y matcheaba** igual (`"open set​tings"` → `Understood(open_settings)`). Falló: el matcher devolvía `Rejected`, no `Understood`.

**Causa raíz:**
La capa de seguridad (`mind-security`, integrada en `DefaultActionMatcher`) inspecciona el input ANTES del matching y **rechaza** caracteres invisibles/format (zero-width, control) como defensa anti-spoofing/homoglyph. El comportamiento correcto pasó de "strip silencioso" (asunción de la fase de normalización) a "rechazo explícito tipado" (decisión de seguridad), que es una garantía más fuerte. La asunción del test quedó obsoleta tras integrarse la capa de seguridad.

**Solución aplicada:**
Actualizado REG-003 para fijar el comportamiento real y seguro: input con zero-width → `Rejected` o `NoMatch` (nunca un `Understood` espurio). El assert acepta ambos resultados seguros.

**Regla futura:**
Al escribir tests sobre el matcher, recordar que `DefaultActionMatcher` aplica **inspección de seguridad antes de normalizar/matchear**: inputs con caracteres invisibles/control/homoglyph se **rechazan** (no se sanean silenciosamente). Nunca asumir strip+match para Unicode sospechoso; asumir rechazo tipado. Los tests de robustez deben aceptar `Rejected`/`NoMatch` como el resultado seguro correcto, no `Understood`.

**Archivos afectados:**
- `mindos/mind-action-matching/src/commonTest/kotlin/com/mindos/action/matching/RegressionSuiteTest.kt`

### 2026-06-04 — Lapsos contados sobre RT ya filtrados por MAD (el filtro borra justo los lapsos)

**Error detectado:**
En `DefaultMetricsEngine.blockMetrics` (MindBoost Enfoque), los lapsos se contaban sobre `cleanRts` (RT de aciertos ya filtrados por MAD). Un test con RTs `[300,320,600,700,310,330]` y `lapseRtMs=500` esperaba 2 lapsos pero obtenía 0: el rechazo de outliers por MAD eliminaba los RT lentos (600, 700) ANTES de contarlos como lapsos.

**Causa raíz:**
Se reutilizó la lista limpia (post-MAD) para todas las métricas. Pero un lapso es una respuesta legítimamente lenta, no un artefacto de medición. El filtro MAD protege la tendencia central (RT mediano / RT-CV) descartando colas extremas — y esas colas extremas SON los lapsos. Mezclar ambos propósitos hace que el filtro borre la señal que otra métrica necesita.

**Solución aplicada:**
Contar lapsos sobre `rawHitRts` (filtrado solo de anticipaciones, NO de MAD). La tendencia central sigue usando `cleanRts`; el conteo de lapsos usa los crudos. Ambos coexisten.

**Regla futura:**
El rechazo de outliers (MAD/SD) se aplica SOLO a métricas de tendencia central (mediana, CV, IES). Las métricas que cuentan eventos extremos por definición (lapsos = RT lentos, anticipaciones = RT ultra-rápidos) se calculan sobre los datos crudos (tras filtrar solo la categoría opuesta). Nunca contar un evento extremo sobre una lista de la que se han eliminado los extremos.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/mindboost/focus/domain/metrics/DefaultMetricsEngine.kt`
- `shared/src/commonTest/kotlin/com/example/mindos/features/mindboost/focus/domain/metrics/DefaultMetricsEngineTest.kt`

### 2026-06-04 — `kotlin.math.cos/sin` sobre `Float` devuelve `Double` y rompe APIs Compose (Offset/Float)

**Error detectado:**
En `FocusAtmosphere.kt`, `Offset(x = w * (0.30f + 0.10f * cos(drift)), ...)` con `drift: Float` no compilaba: "Argument type mismatch: actual type is 'Double', but 'Float' was expected".

**Causa raíz:**
`kotlin.math.cos`/`sin` solo tienen overload `(Double): Double`. Al pasar un `Float`, Kotlin promueve a `Double` y devuelve `Double`. Las APIs gráficas de Compose (`Offset`, radios, `drawCircle`) esperan `Float`, así que la expresión resultante es `Double` y falla.

**Solución aplicada:**
Convertir explícitamente: `val d = drift.toDouble()` y `cos(d).toFloat()`. Centralizar la conversión una vez por bloque de canvas.

**Regla futura:**
En código gráfico de Compose, toda llamada a funciones de `kotlin.math` (`cos`, `sin`, `sqrt`, `pow`…) dentro de expresiones que alimentan `Offset`/`Size`/radios/`Float` debe envolverse con `.toFloat()`, y el argumento angular pasarse como `Double`. No mezclar literales `0.3f` con resultados de trig sin convertir.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/mindboost/focus/presentation/ui/components/FocusAtmosphere.kt`

### 2026-06-04 — Feature nueva grande vs ratchet de Detekt: estrategia de findings

**Error detectado:**
Al añadir la feature MindBoost Enfoque (≈45 archivos), `:shared:detekt` falló con ~62 findings nuevos fuera del baseline (MagicNumber, FunctionNaming, LongMethod, LongParameterList, CyclomaticComplexMethod, TooManyFunctions, ReturnCount, UnusedParameter), bloqueando el build por el ratchet.

**Causa raíz:**
El baseline de Detekt solo cubre el código existente. Todo archivo nuevo se evalúa con las reglas por defecto y cualquier finding rompe el build (ratchet one-way).

**Solución aplicada (jerarquía: arreglar > justificar > suprimir idiomático):**
1. **Arreglar de verdad** las categorías de alto valor y estructurales: `UnusedParameter` (era un `template` realmente sin usar — bug latente); `ReturnCount`/`LoopWithTooManyJumpStatements` refactorizados a forma sin múltiples `return`/`continue`; `MaxLineLength` envolviendo; `blockMetrics` (LongMethod) extrayendo `addErrorTallies`. Constructor del StateHolder con 11 params → recibe el `FocusGraph` (4 params).
2. **Nombrar constantes de dominio** reutilizables (`MAD_SCALE`, `PERCENT`, `CV_VERY_STABLE`…).
3. **Suprimir con justificación** lo idiomático: Composables → `@file:Suppress("FunctionNaming","LongMethod","MagicNumber")`; motores con tabla de decisión/estadística densa → `@Suppress("CyclomaticComplexMethod"/"TooManyFunctions"/"MagicNumber")` con comentario; `AppColors` (archivo-token de hex) → `@Suppress("MagicNumber")` a nivel de `object`.

**Regla futura:**
Para una feature nueva grande, NO regenerar el baseline a ciegas. Orden: (1) arreglar `UnusedParameter`/`UnusedPrivateProperty`/`UnreachableCode` SIEMPRE (suelen ser bugs); (2) refactorizar `ReturnCount`/`MaxLineLength`/`LongMethod` cuando es barato; (3) suprimir con `@Suppress` + comentario lo idiomático de Compose/estadística (preferible al baseline porque es local y revisable). Las supresiones de `MagicNumber` son aceptables en: Composables, `data class` de config cuyos defaults SON la constante, enums cuyos valores SON los datos, y el `object AppColors`.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/mindboost/focus/**`
- `shared/src/commonMain/kotlin/com/example/mindos/design/AppColors.kt`

### 2026-06-05 — `kotlinx-serialization-core` no produce formato string; falta `-json`

**Error detectado:**
Para la persistencia local de MindBoost se necesitaba serializar DTOs a string, pero `commonMain` solo declaraba `kotlinx-serialization-core`. `core` aporta `@Serializable` y los descriptores/`KSerializer`, pero NO un formato: `Json`, `encodeToString`/`decodeFromString` viven en el artefacto separado `kotlinx-serialization-json`, que no estaba.

**Causa raíz:**
El repo usa `@Serializable` solo para la serialización de Firebase (gitlive), que consume los descriptores de `core` sin necesitar un formato. Por eso `core` bastaba hasta ahora y `-json` nunca se añadió.

**Solución aplicada:**
Añadido `kotlinx-serialization-json` (misma versión pineada 1.9.0, multiplataforma) al catálogo y a `commonMain`, con comentario justificando la adición. El plugin `kotlinSerialization` ya estaba aplicado.

**Regla futura:**
Si necesitas (de)serializar a/desde String (JSON), `kotlinx-serialization-core` NO basta: añade `kotlinx-serialization-json` a la MISMA versión. `core` solo sirve cuando otro formato (p. ej. el de Firebase gitlive) consume los descriptores. Antes de escribir `Json { }`/`encodeToString`, verifica que `-json` esté declarado.

**Archivos afectados:**
- `gradle/libs.versions.toml`
- `shared/build.gradle.kts`

### 2026-06-05 — Patrón de persistencia local cifrada para datos sensibles (KMP)

**Error detectado:**
No era un bug: decisión de arquitectura/seguridad al persistir datos cognitivos de MindBoost (perfil, baseline, resultados de probe), que son sensibles (pueden inferir rasgos de salud).

**Causa raíz:**
Persistir datos cognitivos requiere cifrado en reposo y aislamiento de la superficie cripto/plataforma para mantener la lógica testeable sin device.

**Solución aplicada:**
Superficie cripto/plataforma MÍNIMA: `expect fun rememberSecureKeyValueStore(namespace): SecureKeyValueStore` (get/put/remove/keys/clearNamespace). Android = AES-256-GCM con clave en Android Keystore (device-bound), **replicando** el patrón ya probado de `PublisherPlatformStore.android.kt` (no se inventó cripto); desktop/iOS = in-memory (no escriben datos sensibles a disco; iOS Keychain diferido). Toda la lógica (DTOs `@Serializable` con `schemaVersion`, mappers, repos `Local*`) vive en `commonMain` y se testea contra `InMemorySecureKeyValueStore`. Solo se persisten **resúmenes** de sesión (sin trials crudos).

**Regla futura:**
Para datos sensibles en KMP: (1) NUNCA inventar cripto — reutilizar el patrón AES/GCM+Keystore existente; (2) aislar plataforma en una superficie KV genérica mínima vía expect/actual, dejando DTOs/mappers/repos en commonMain (testeables con un fake in-memory); (3) versionar DTOs con `schemaVersion` + `Json { ignoreUnknownKeys = true; encodeDefaults = true }`; (4) verificar que las SharedPreferences están excluidas de backups (`backup_rules.xml`/`data_extraction_rules.xml` ya tienen `sharedpref path="."` — cubre cualquier prefs nuevo); (5) la clave del Keystore nunca se respalda (device-bound), doble protección; (6) persistir solo resúmenes, nunca datos crudos de alto volumen.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/example/mindos/features/mindboost/focus/data/local/SecureKeyValueStore.kt`
- `shared/src/androidMain/kotlin/com/example/mindos/features/mindboost/focus/data/local/SecureKeyValueStore.android.kt`
- `shared/src/{desktop,ios}Main/kotlin/com/example/mindos/features/mindboost/focus/data/local/SecureKeyValueStore.*.kt`

### 2026-06-12 — La tarea de compilación Android del módulo KMP es `compileAndroidMain`, no `compileDebugKotlinAndroid`

**Error detectado:**
`./gradlew :shared:compileDebugKotlinAndroid` falló con "task 'compileDebugKotlinAndroid' not found in project ':shared'".

**Causa raíz:**
`:shared` usa el plugin nuevo `com.android.kotlin.multiplatform.library` (AGP 9) declarado vía `kotlin { android { ... } }`. Ese plugin NO genera las tareas por-variante `compile<Variant>KotlinAndroid` del plugin clásico; expone una única compilación `main` con la tarea `compileAndroidMain`.

**Solución aplicada:**
Usar `:shared:compileAndroidMain` para compilar el target Android del módulo compartido. (`:androidApp:assembleDebug` sigue construyendo el APK end-to-end.)

**Regla futura:**
Para compilar solo el target Android de `:shared`, usar `:shared:compileAndroidMain`. Tareas útiles del módulo: `:shared:compileKotlinDesktop` (desktop main), `:shared:desktopTest` (tests JVM), `:shared:compileAndroidMain` (Android main), `:shared:detekt` (ratchet). Si una tarea "no se encuentra", listar con `:shared:tasks --all` antes de asumir nombres del plugin clásico.

**Archivos afectados:**
- (ninguno — comando de build)

### 2026-06-12 — MindBoost Velocidad Mental (MVP-0): persistencia con SecureKeyValueStore, no Room

**Error detectado:**
No es un bug: decisión de arquitectura al implementar MVP-0 de Velocidad Mental. El plan markdown especifica entidades Room, pero el repo no tiene Room/KSP/SQLDelight y su patrón establecido para datos cognitivos sensibles es `@Serializable` + `SecureKeyValueStore` cifrado + repos en commonMain testeables con fakes.

**Causa raíz:**
Añadir Room sobre AGP 9.1.0 / Kotlin 2.3.10 / el plugin KMP-library nuevo es alto riesgo de build, y empujaría los tests de borrado transaccional / limpieza de huérfanos / integridad de persistencia a un emulador, contradiciendo el mandato "simulación primero como suite automatizada" (§12 del plan). El cifrado en reposo de Room tampoco es por defecto.

**Solución aplicada:**
Feature `features/mindboost/speed/` (espejo de `focus/`). Las 8 entidades del plan son data classes `@Serializable` puras (`domain/model/SpeedRecords.kt`); la integridad relacional (referencias por id, cascada, borrado transaccional por usuario, limpieza de `DeviceProfile` huérfanos, versionado) vive en `SpeedMeasurementStore` con dos impls: `InMemorySpeedMeasurementStore` (modelo semántico autoritativo, base de tests/simulador) y `SecureSpeedMeasurementStore` (durable, cifrado vía `SecureKeyValueStore`). Toda la lógica (timebase, métricas, edge-cases, trial engine, calidad, simulación) es commonMain y se testea en `desktopTest` sin dispositivo. La única superficie de plataforma es el `expect rememberSpeedTimebaseHandle`/`SpeedTapArea` (Android = Choreographer + MotionEvent; desktop/iOS = fallback monotónico documentado).

**Regla futura:**
Para una feature de datos cognitivos sensibles en este repo, NO introducir Room/SQLDelight: usar el patrón `@Serializable` + `SecureKeyValueStore` + repos commonMain + fake in-memory. Si un plan externo pide Room, preservar la INTENCIÓN relacional (FK por id, cascada, borrado atómico por usuario, huérfanos, versionado) en una abstracción de repositorio testeable en JVM, y documentar la desviación.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/**` (NUEVO)
- `shared/src/{android,desktop,ios}Main/kotlin/com/mindos/app/features/mindboost/speed/**` (NUEVO actuals)
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/speed/**` (NUEVO, 67 tests)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/presentation/ui/MindBoostHubScreen.kt` (track Velocidad)

### 2026-06-14 — El build **debug** usa `src/debug/google-services.json` placeholder → no autentica contra Firebase real

**Error detectado:**
El APK debug (`com.mindos.app.debug`) fallaba TODO login email/contraseña con `RecaptchaCallWrapper … [ API key not valid. Please pass a valid API key. ]`, idéntico en cada intento. Se perdió mucho tiempo editando restricciones de la API key, registrando el SHA-1 de debug y desactivando reCAPTCHA en el proyecto `mindos-8452f` — sin ningún efecto.

**Causa raíz:**
El repo commitea `androidApp/src/debug/google-services.json`, un **placeholder** (`project_id: mindos-debug-placeholder`, `project_number: 000000000000`, y una `api_key` claramente falsa con sufijo de equis). El plugin Gradle `google-services` usa el `google-services.json` **específico de variante** (`src/debug/`) con precedencia sobre el de la raíz. Así que el build debug enviaba una API key FALSA a Firebase → `API key not valid`. Todas las ediciones en la consola de `mindos-8452f` eran irrelevantes: el build debug **nunca contacta** ese proyecto. La raíz `androidApp/google-services.json` (key real de producción, paquete `com.mindos.app`) solo la usa el build **release**.

**Solución aplicada:**
Diagnóstico, no fix de código. El build debug, tal cual, **no puede** autenticar contra Firebase real (placeholder trackeado: intencional o setup incompleto). Para autenticar en debug hay que reemplazar `src/debug/google-services.json` por una config real (registrando `com.mindos.app.debug` + SHA-1 de debug en `mindos-8452f` y descargando su `google-services.json`). Diagnóstico clave: un GET de navegador `https://identitytoolkit.googleapis.com/v1/projects?key=<KEY_REAL>` devuelve JSON (key real OK) pero la app debug sigue fallando → la app usa OTRA key (la del override de variante).

**Regla futura:**
Ante un fallo de Firebase Auth/Firestore SOLO en una variante (p. ej. `API key not valid` en debug), ANTES de tocar Google Cloud, revisar si existe `androidApp/src/<variant>/google-services.json` que sobrescriba al de la raíz (el plugin prioriza el de variante). Verificar `project_id`/`api_key` reales (no `…-placeholder` ni una key con sufijo de equis). La validación física §15 que requiere login debe usar un build que SÍ autentique (release con config real, o debug con `src/debug/google-services.json` real); un build debug vanilla se queda en la pantalla de Auth y nunca llega a MindBoost.

**Archivos afectados:**
- `androidApp/src/debug/google-services.json` (placeholder — causa raíz, sin cambios)
- `androidApp/google-services.json` (config real, solo release)
- `docs/mindboost_velocidad_implementation_plan/PHYSICAL_VALIDATION_GATE.md` (caveat añadido)

**Nota de reemplazo (2026-06-14):**
El bloqueo descrito quedó resuelto: `com.mindos.app.debug` ya está registrada en Firebase, el
archivo de variante contiene configuración real y el emulador tiene App Check debug autorizado.
La regla de diagnóstico sobre precedencia por variante sigue vigente.

---

### 2026-06-15 — No deduplicar señales antes de calcular su frecuencia

**Error detectado:**
El progreso de Mindboost Lógica nunca podía marcar la señal de posible impulsividad aunque hubiera
dos o más respuestas impulsivas.

**Causa raíz:**
Los errores se deduplicaban con `distinct()` antes de contar impulsividad, por lo que el recuento
máximo posible era uno.

**Solución aplicada:**
La frecuencia se calcula sobre la lista completa de errores y la lista deduplicada se conserva solo
para reporting. Se añadió una regresión con dos intentos impulsivos.

**Regla futura:**
Cuando una decisión dependa de repetición o frecuencia, contar siempre sobre eventos crudos; aplicar
`distinct()` únicamente después y solo para presentación o almacenamiento resumido.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/logic/domain/adaptive/LogicDifficultyAdapter.kt`
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/logic/domain/adaptive/LogicDifficultyAdapterTest.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-15 — Scripts para Windows PowerShell 5.1 deben evitar UTF-8 sin BOM no ASCII

**Error detectado:**
`powershell -File scripts/verify-mindos.ps1 -Full` fallaba al parsear con una cadena sin cerrar,
antes de ejecutar cualquier quality gate.

**Causa raíz:**
El `.ps1` estaba guardado como UTF-8 sin BOM. Windows PowerShell 5.1 lo leyó como ANSI y los bytes
de la `Ó` de `FALLÓ` incluyeron un carácter interpretado como comilla tipográfica, abriendo una
cadena que nunca se cerraba.

**Solución aplicada:**
Se convirtieron los mensajes del script a ASCII para que `powershell -File` los interprete de forma
estable en PowerShell 5.1.

**Regla futura:**
Los scripts `.ps1` ejecutados mediante `powershell.exe` deben usar texto ASCII o UTF-8 con BOM;
no introducir caracteres no ASCII en UTF-8 sin BOM y validar siempre el script con Windows
PowerShell 5.1.

**Archivos afectados:**
- `scripts/verify-mindos.ps1`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-15 — La transferencia isomorfa debe evaluar ambos intentos

**Error detectado:**
El desglose de transferencia de un ítem isomorfo podía guardar `1.0` aunque el usuario fallara el
isomorfo, siempre que hubiera acertado antes el gemelo entrenado.

**Causa raíz:**
El cálculo consultaba únicamente el resultado previo con la misma estructura lógica y no incorporaba
la corrección de la respuesta actual.

**Solución aplicada:**
La consistencia de transferencia solo vale `1.0` cuando tanto el gemelo entrenado como el isomorfo
actual son correctos; cualquier fallo produce `0.0`. Se añadió una regresión de sesión completa.

**Regla futura:**
Toda métrica comparativa o de transferencia debe calcularse con señales de ambos lados de la
comparación; no derivarla únicamente del baseline o intento entrenado.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/logic/presentation/LogicStateHolder.kt`
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/logic/presentation/LogicStateHolderTest.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-15 — Parentetizar `if` al combinarlo con fórmulas Kotlin

**Error detectado:**
El scorer ICR de Mindboost Lógica solo aplicaba el peso de respuesta final cuando la respuesta era
correcta y omitía los pesos de regla, premisas, justificación, confianza e independencia.

**Causa raíz:**
Se escribió `peso * if (condición) 1.0 else 0.0 + resto` sin paréntesis. En Kotlin, el `else`
absorbió la suma posterior como parte de la expresión condicional, alterando la fórmula.

**Solución aplicada:**
Se parentetizó explícitamente el término condicional:
`peso * (if (condición) 1.0 else 0.0) + resto`.

**Regla futura:**
En fórmulas Kotlin, todo `if` usado como operando debe ir entre paréntesis antes de continuar con
sumas, restas o multiplicaciones; añadir un test numérico del resultado compuesto.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/logic/domain/scoring/ReasoningScorer.kt`
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/logic/domain/scoring/ReasoningScorerTest.kt`
- `AGENTS.md`
- `CLAUDE.md`

### 2026-06-17 — `Modifier.weight` exige receptor `ColumnScope`/`RowScope` en un composable extraído

**Error detectado:**
`:shared:compileKotlinDesktop` falló con "Unresolved reference 'weight'" en `LanguageRootScreen` al usar
`Modifier.weight(1f)` dentro de un `@Composable private fun MenuView(...)`.

**Causa raíz:**
`Modifier.weight` es una extensión de `ColumnScope`/`RowScope`. Aunque `MenuView` se llama dentro de un
`Column`, al extraerlo a una función composable normal su cuerpo NO tiene el receptor `ColumnScope`, así que
`weight` no resuelve.

**Solución aplicada:**
Declarar el composable como extensión del scope: `private fun ColumnScope.MenuView(...)`. La llamada sigue
estando dentro del `Column { }` del padre, que aporta el receptor.

**Regla futura:**
Todo composable extraído que use `weight` (u otros miembros de `ColumnScope`/`RowScope` como `align`) debe
declararse como extensión `ColumnScope.`/`RowScope.`; no basta con invocarlo dentro de un `Column`/`Row`.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/language/presentation/ui/LanguageRootScreen.kt`
- `CLAUDE.md`

### 2026-06-18 — Ejercicios de Enfoque implementados pero inalcanzables (presentación cableada a E1)

**Error detectado:**
El catálogo de Enfoque declaraba 13 ejercicios y existían 13 generadores de dominio completos y
testeados (E1–E13), pero solo E1 era seleccionable: 12 ejercicios implementados quedaban invisibles
en el entrenador.

**Causa raíz:**
La tubería de presentación estaba acoplada a un único ejercicio: `FocusGraph` exponía solo
`e1Generator`/`e1Definition`, `FocusSessionStateHolder` estaba hardwired a E1, `FocusHomeScreen` no
tenía menú de selección y `ExerciseCatalog.implemented` listaba solo E1. No era un filtro silencioso
(el gate era correcto): era integración de presentación incompleta, sin red anti-regresión que atara
generador ↔ `implemented` ↔ renderer.

**Solución aplicada:**
`FocusExerciseGenerators.registry` (mapa id→generador) como única fuente de verdad de lo wired;
`FocusGraph` parametrizado (`generators` + `definitionFor`/`generatorFor`); `FocusSessionStateHolder`
y la ruta de sesión parametrizados por `ExerciseId`; menú en `FocusHomeScreen` driven por
`implemented`; se expuso E3 (go/no-go, render-compatible con el campo single-tap actual). Se añadieron
`domainReady` y `pendingUiIntegration` al catálogo y un `ExerciseCatalogConsistencyTest` que rompe el
build si un generador render-compatible se termina sin exponerlo, o si se expone un id sin
generador/renderer.

**Regla futura:**
Para baterías de ejercicios no exponer por catálogo: usar un registry id→generador como única fuente
de verdad, parametrizar session holder + navegación por id, y un test que ate `implemented` ⟺
`registry.keys`. Distinguir "dominio listo" de "UI alcanzable" (`domainReady` vs
`implemented`/`pendingUiIntegration`): un generador con tests NO equivale a un ejercicio jugable hasta
tener renderer + captura de respuesta. Solo exponer paradigmas compatibles con el renderer existente
(E1/E3 single-tap); elección/espacial/stream requieren su propio renderer. Al cambiar `implemented`,
revisar los defaults que dependen de él (p. ej. `DefaultDailyPlanGenerator`) y sus tests.

**Archivos afectados:**
- `shared/.../focus/domain/catalog/ExerciseCatalog.kt`
- `shared/.../focus/domain/exercise/FocusExerciseGenerators.kt` (NUEVO)
- `shared/.../focus/di/FocusGraph.kt`
- `shared/.../focus/presentation/FocusSessionStateHolder.kt`
- `shared/.../focus/presentation/{FocusHomeStateHolder,RememberFocusStateHolders}.kt`
- `shared/.../focus/presentation/state/FocusUiState.kt`
- `shared/.../focus/presentation/ui/{FocusHomeScreen,FocusRootScreen}.kt`
- `shared/.../focus/domain/catalog/ExerciseCatalogConsistencyTest.kt` (NUEVO)
- `shared/.../focus/presentation/FocusHomeStateHolderTest.kt` (NUEVO)
- `shared/.../focus/domain/session/DailyPlanGeneratorTest.kt`
- `CLAUDE.md`

### 2026-06-18 — Integración del paradigma de dos alternativas (E2/E5) en Enfoque

**Error detectado:**
Al integrar ejercicios de elección (flanker E2, Stroop/Simon E5) en el runtime de Enfoque, el
`FocusSessionAssembler` clasificaba SIEMPRE con `TrialClassifier.classify` (go/no-go) desde un
`RecordedTrial(stimulus, latencyMs)` que no llevaba la tecla pulsada. En tareas de dos alternativas,
acertar depende de QUÉ lado se eligió, no de si se respondió: toda respuesta en ventana se habría
contado como acierto y el efecto de congruencia nunca habría salido.

**Causa raíz:**
El runtime/ensamblador se diseñaron para el slice E1 (single-tap). `classifyChoice` existía en el
dominio pero nunca se cableó: faltaba transportar la tecla de respuesta hasta el ensamblador, que es
la autoridad de clasificación (los contadores en vivo del holder son secundarios).

**Solución aplicada:**
`ResponseKey.isDirectional` (LEFT/RIGHT) marca un trial de elección; `RecordedTrial` gana
`responseKey`; `assembleBlock` despacha a `classifyChoice` cuando `correctResponseKey.isDirectional`,
si no a `classify`. El holder captura LEFT/RIGHT (`onResponse(key)`, `onTap`=PRIMARY) y la pantalla se
divide en dos mitades táctiles; nuevos `FocusFieldState.Choice` + `FocusChoiceField`. E2/E5 expuestos
vía `registry` + `implemented`, con el `ExerciseCatalogConsistencyTest` actualizado.

**Regla futura:**
Para un nuevo paradigma de Enfoque: (1) decidir la modalidad de respuesta y propagar TODO lo que la
clasificación necesita hasta el `RecordedTrial` (no asumir que la latencia basta); (2) el ensamblador
—no los contadores del holder— es la fuente de verdad de outcome/métricas; (3) una métrica
comparativa (congruencia, switch cost…) solo es fiable con HIT-con-RT en AMBOS lados de la
comparación; (4) exponer en `registry` + `implemented` a la vez y mantener el delta en
`pendingUiIntegration` (el test de consistencia lo exige).

**Archivos afectados:**
- `shared/.../focus/domain/model/Stimulus.kt` · `domain/session/FocusSessionAssembler.kt`
- `shared/.../focus/presentation/FocusSessionStateHolder.kt` · `presentation/state/FocusUiState.kt`
- `shared/.../focus/presentation/ui/FocusSessionScreen.kt` · `ui/components/FocusChoiceField.kt` (NUEVO)
- `shared/.../focus/domain/exercise/FocusExerciseGenerators.kt` · `domain/catalog/ExerciseCatalog.kt`
- `shared/.../focus/domain/session/FocusSessionAssemblerChoiceTest.kt` (NUEVO)
- `CLAUDE.md`

### 2026-06-18 — Integración del paradigma stop-signal (E4 Freno) en Enfoque

**Error detectado:**
Al cablear E4 al runtime, la clasificación go/no-go existente marcaba como HIT un trial de stop
respondido — porque E4 pone `isTarget=true` en TODOS los trials (se responde por defecto). Un stop
respondido es una inhibición FALLIDA, no un acierto; además la SSD real (online) no llegaba a
`TrialResult.stopSignalDelayMs`, así que el SSRT no era computable desde el ensamblador.

**Causa raíz:**
El stop-signal no es go/no-go: todos los trials piden respuesta y el de stop pide ABORTAR. La SSD es
ONLINE (depende de las respuestas, escalera 1-up/1-down) y por diseño el generador la deja null; el
runtime debe resolverla por trial con `StopSignalTracker`. El ensamblador re-clasifica y necesita una
clasificación específica de stop y la SSD usada.

**Solución aplicada:**
`TrialClassifier.classifyStop` (respondió→FALSE_ALARM con RT; retuvo→CORRECT_REJECTION). El holder crea
el `StopSignalTracker` del generador, presenta en DOS fases (GO; si no responde dentro de la SSD,
cambia a `FocusFieldState.Stop` rojo el resto de la ventana), registra la SSD usada en `RecordedTrial`
y actualiza el tracker (`successfulStop = sin respuesta`). El ensamblador despacha
classifyStop/classifyChoice/classify y propaga `stopSignalDelayMs` a `TrialResult`. El SSRT (ya en
dominio) computa desde el bloque ensamblado.

**Regla futura:**
Un paradigma con dificultad ONLINE (depende de la respuesta) NO cabe en el patrón generate-y-reproduce
puro: el generador emite el ESQUEMA + un tracker, y el runtime resuelve el parámetro por trial y lo
registra en `RecordedTrial` para que el ensamblador lo propague. La clasificación vive en
`TrialClassifier` (una función por modalidad) y holder + ensamblador deben usar EXACTAMENTE la misma
vía de despacho. Al extraer ramas con varios `return`, respetar `ReturnCount` (≤2) partiendo en
funciones — nunca regenerar el baseline de Detekt para silenciarlo.

**Archivos afectados:**
- `shared/.../focus/domain/exercise/TrialClassifier.kt` (classifyStop)
- `shared/.../focus/domain/session/FocusSessionAssembler.kt` (dispatch stop + propaga SSD)
- `shared/.../focus/presentation/FocusSessionStateHolder.kt` (tracker + GO→STOP dos fases)
- `shared/.../focus/presentation/state/FocusUiState.kt` (FocusFieldState.Stop) · `ui/components/FocusStimulusField.kt`
- `shared/.../focus/domain/exercise/FocusExerciseGenerators.kt` · `domain/catalog/ExerciseCatalog.kt`
- `shared/.../focus/domain/session/FocusStopSignalAssemblerTest.kt` (NUEVO)
- `CLAUDE.md`

### 2026-06-18 — Task-switching (E6) exige enriquecer el payload, no mostrar la respuesta

**Error detectado:**
`E6CambioGenerator` fijaba `correctResponse` con un `nextBoolean()` aleatorio y `TaskSwitch` solo
llevaba `rule/isSwitch/correctResponse`. Al integrarlo, el usuario solo vería una respuesta que
pulsar: no habría tarea de cambio real (sin estímulo al que aplicar la regla) y `SWITCH_COST` no
mediría nada cognitivo.

**Causa raíz:**
El task-switching exige aplicar la regla ACTIVA a un estímulo multidimensional; el payload no
transportaba esas dimensiones, así que un render fiel era imposible sin enriquecer el dominio.

**Solución aplicada:**
`StimulusPayload.TaskSwitch` gana `colorIsRed` + `shapeIsCircle` (estímulo color/forma); el generador
deriva `correctResponse` de (rule, dimensión) — RULE_A=color (rojo→izq), RULE_B=forma (círculo→izq).
Nuevo `FocusFieldState.RuleSwitch` + `FocusRuleSwitchField` (forma coloreada + cue de regla) reutiliza
las mitades táctiles LEFT/RIGHT (`ChoiceResponseLayer`). `SWITCH_COST` (ya en dominio) computa desde
el bloque ensamblado.

**Regla futura:**
Si un paradigma exige que el usuario DERIVE la respuesta de un estímulo (task-switch, conflicto real,
búsqueda), el payload de dominio debe transportar las features del estímulo y el generador derivar
`correctResponse` de ellas — NUNCA exponer la respuesta directamente (render deshonesto). Enriquecer
el payload con defaults para no romper construcciones existentes; los tests de generador determinista
siguen valiendo porque las nuevas extracciones salen del mismo `Random`.

**Archivos afectados:**
- `shared/.../focus/domain/model/Stimulus.kt` (TaskSwitch +color/forma)
- `shared/.../focus/domain/exercise/E6CambioGenerator.kt` (deriva correctResponse)
- `shared/.../focus/presentation/state/FocusUiState.kt` (RuleSwitch) · `ui/components/FocusRuleSwitchField.kt` (NUEVO)
- `shared/.../focus/presentation/FocusSessionStateHolder.kt` · `ui/FocusSessionScreen.kt` (ChoiceResponseLayer)
- `shared/.../focus/domain/exercise/FocusExerciseGenerators.kt` · `domain/catalog/ExerciseCatalog.kt`
- `shared/.../focus/domain/session/FocusTaskSwitchTest.kt` (NUEVO)
- `CLAUDE.md`

### 2026-06-18 — Paradigmas temporales/dinámicos (MOT/RSVP/dual) caben en el runtime de una sola respuesta

**Error detectado:**
No era un bug: decisión de integración. E9 (MOT, objetos en movimiento), E10 (RSVP, ráfaga rápida) y
E8 (dual-task) parecían exigir un runtime con sub-temporización o múltiples respuestas por trial, lo
que habría obligado a reescribir el bucle `presentAndAwait` (una respuesta por trial).

**Causa raíz:**
El dominio ya modela los tres como UN solo juicio por trial: E9 sondea un objeto (`isTarget` =
"¿el sondeado era objetivo?"), E10 es detección (`isTarget=true` → tocar al ver el target), E8 es
go/no-go con un flag `dual`. La dificultad (movimiento/velocidad/carga) es de PRESENTACIÓN, no de
respuesta.

**Solución aplicada:**
El `FocusFieldState` lleva lo necesario para que el renderer "tonto" corra su PROPIA animación sobre
la ventana de respuesta (E10 `LaunchedEffect` que avanza la ráfaga; E9 `animateFloatAsState`
fases highlight→move→probe en `Canvas`; E8 contador concurrente), mientras el holder sigue esperando
UNA pulsación. La estabilidad/variación por trial se logra con un `layoutSeed = stimulus.index`
(`remember(field)` + `Random(seed)`), sin tocar el dominio. E9 deriva el objeto sondeado de
`targetIndices` + `probedIsTarget` sin enriquecer el payload.

**Regla futura:**
Si la dificultad de un paradigma es temporal (stream) o espacio-dinámica (movimiento) pero el dominio
lo modela con una sola respuesta, NO reescribas el runtime: mete el movimiento/temporización
ENTERAMENTE en el composable (keyed por un seed por trial para estabilidad + variación) y deja que el
holder capture una pulsación sobre la ventana. En `Canvas`, `cos/sin` de `kotlin.math` devuelven
`Double` → `.toFloat()` antes de alimentar `Offset`/radios. Estas animaciones son funcionales (la
tarea), no decorativas (respeta "no animaciones por defecto").

**Archivos afectados:**
- `shared/.../focus/presentation/state/FocusUiState.kt` (Tracking/Stream/Dual)
- `shared/.../focus/presentation/ui/components/{FocusTrackingField,FocusStreamField,FocusDualTaskField}.kt` (NUEVOS)
- `shared/.../focus/presentation/FocusSessionStateHolder.kt` (fieldFor → 3 mapeos) · `ui/FocusSessionScreen.kt`
- `shared/.../focus/domain/exercise/FocusExerciseGenerators.kt` · `domain/catalog/ExerciseCatalog.kt` (13/13)
- `shared/.../focus/domain/session/FocusAdvancedParadigmsAssemblerTest.kt` (NUEVO)
- `CLAUDE.md`

### 2026-06-22 — Arena Module III: gate de integridad VERIFIED y Detekt en archivos nuevos

**Error detectado:**
Al implementar el Arena Bayesian Core (Fases 03–07.5) surgieron dos conflictos: (1) la fuente de
verdad sobre el gate de rating era ambigua —el plan detallado `03/rating_update_algorithm.md` exige
`Integrity == OPEN`, pero `17_metric_integrity_hardening.md` exige `IntegrityStateEnum == VERIFIED`—;
(2) `:shared:detekt` rechazó dos archivos nuevos por `ComplexCondition` (`UuidV4`: 4 condiciones `==`
unidas por `||`) y `MatchingDeclarationName` (`RatingConfig.kt` con única declaración top-level
`RatingEngineConfig`).

**Causa raíz:**
(1) El árbol `docs/module_iii_plan` (el plan auditado) tiene menor prioridad que los docs de revisión
(`17_*`) en el orden de fuentes de verdad; el gate correcto es VERIFIED. (2) Detekt (default config +
buildUponDefaultConfig) aplica `ComplexCondition` (umbral 4 operandos) y `MatchingDeclarationName`
(un único top-level obliga a que el archivo se llame igual) a TODO archivo nuevo fuera del baseline.

**Solución aplicada:**
(1) Se añadió `IntegrityStateEnum.VERIFIED` y el motor sólo aplica delta cuando el estado es VERIFIED;
los demás estados son `Rejected(INTEGRITY_NOT_VERIFIED)`. (2) La condición de posición de guion se
movió a un `setOf(4,6,8,10)` (`i in DASH_POSITIONS`); el archivo se renombró a `RatingEngineConfig.kt`.

**Regla futura:**
Cuando el plan detallado contradiga un doc de revisión posterior, gana el doc de mayor prioridad en el
orden de fuentes; documenta la reconciliación en el KDoc del símbolo afectado. En archivos nuevos: una
condición con ≥4 operandos booleanos se factoriza a pertenencia en colección; un archivo con una sola
declaración top-level DEBE llamarse igual que ella (o añade una 2ª declaración para que
`MatchingDeclarationName` no aplique). Para tests pesados/IO o escaneo de fuentes, ubícalos en
`desktopTest` (no lo escanea Detekt); `commonTest` sí se escanea.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/arena/**` (NUEVO: rating/engine, hardware,
  matchmaking, evidence/ledger, evidence/motors, sanitization, observability, simulation, config)
- `shared/src/commonMain/kotlin/com/mindos/app/features/arena/config/Enums.kt` (+VERIFIED y enums Fase 03)
- `shared/src/desktopTest/kotlin/com/mindos/app/features/arena/**` (NUEVO: 65 tests de aceptación)
- `CLAUDE.md`

### 2026-06-23 — Cobertura Kover en KMP: usar la tarea por-target `koverVerifyDesktop`, no la fusionada

**Error detectado:**
Al añadir Kover (`org.jetbrains.kotlinx.kover` 0.9.8) para medir/forzar cobertura del Arena core,
la tarea fusionada `koverVerify`/`koverVerifyAll` mezcla los targets KMP: el target Android no ejecuta
los tests JVM (los tests de arena viven en `desktopTest`), así que las clases `commonMain` figuran sin
cubrir por el lado Android y una regla a nivel "all code" puede fallar pese a estar el código cubierto
por `desktopTest`.

**Causa raíz:**
En KMP, Kover crea variantes de reporte por target (`...Desktop`, `...Android`) además de la fusionada.
La cobertura real proviene del target cuyos tests se ejecutan (desktop). La regla configurada en
`kover { reports { filters {} verify {} } }` se hereda por TODAS las variantes, pero sólo la variante
cuyo test corrió tiene datos de ejecución.

**Solución aplicada:**
Medir y verificar con las tareas Desktop: `:shared:koverLogDesktop`, `:shared:koverHtmlReportDesktop`,
`:shared:koverVerifyDesktop`. Se filtró el alcance a `…arena.rating.engine.*` y `…arena.sanitization.*`
y se fijó `minBound(90)` (cobertura real 96.18%). La regla se DEJÓ fuera de `check` (ejecución manual)
para no introducir fallos sorpresa en CI; el guard de terminología SÍ se amplió a `commonTest`/
`desktopTest` y se enganchó a `check`. Compatibilidad verificada: Kover 0.9.8 aplica limpio sobre
Gradle 9.3.1 / AGP 9.1.0 / Kotlin 2.3.10. Se probó que la regla realmente bloquea subiendo el bound a
99 (falló con 96.18%) y restaurándolo a 90.

**Regla futura:**
Para cobertura en este repo KMP, usar las tareas `kover*Desktop` (el target con tests JVM), nunca la
fusionada, y filtrar `reports.filters.includes.classes(...)` al paquete medido. Verificar la versión de
Kover contra Maven Central antes de fijarla (Gradle 9 es reciente). No enganchar `koverVerify*` a `check`
sin acordar el gate; sí es seguro ampliar guards estáticos aditivos (terminology/production) a test dirs.

**Archivos afectados:**
- `gradle/libs.versions.toml` (+kover 0.9.8)
- `shared/build.gradle.kts` (plugin kover + bloque `kover {}` filtrado + scanners de terminología sobre test dirs)
- `CLAUDE.md`

### 2026-06-23 — El scanner de terminología hace match por palabra completa a través de guiones

**Error detectado:**
`:shared:arenaTerminologyScanner` falló con "Forbidden terminology found: 'clinical' in file
UserSafeExplanationMapping.kt": un comentario KDoc decía "non-clinical product language". El segundo
intento ("non-diagnostic") habría fallado igual por 'diagnostic'.

**Causa raíz:**
El scanner usa `Regex("\\b$term\\b")`. El guion `-` es un carácter no-palabra, así que `\bclinical\b`
SÍ coincide dentro de "non-clinical" (y `\bdiagnostic\b` dentro de "non-diagnostic"). Los términos
prohibidos (ELO, IQ, clinical, diagnostic, gambling, stake, bet, casino, bankroll, odds, cash-out…)
se detectan aunque vayan precedidos/seguidos de guion u otro signo.

**Solución aplicada:**
Reescribir el comentario con lenguaje neutro ("plain product language"), sin los lexemas prohibidos
ni siquiera como parte de un compuesto con guion. Verificado con `arenaTerminologyScanner` (commonMain)
y los scanners de `commonTest`/`desktopTest`.

**Regla futura:**
En cualquier fuente del módulo `arena` (incluidos comentarios y tests), no escribir un término
prohibido NI como parte de un compuesto con guion (`non-clinical`, `anti-...`). Antes de cerrar una
tarea que toque `arena`, ejecutar `:shared:arenaTerminologyScanner` (+ variantes de test). Para copy
de usuario, preferir lenguaje funcional ("calibración", "práctica", "perfil"), nunca clínico.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/arena/session/explanation/UserSafeExplanationMapping.kt`
- `CLAUDE.md`

### 2026-06-25 — El borrado de cuenta no purgaba la media/docs de profile-cards (erasure diferido al GC)

**Error detectado:**
`executeAccountDeletion(uid)` purgaba los prefijos de Storage `profile_visuals/${uid}/` y
`post_attachments/${uid}/` y borraba docs sueltos (`profiles`, `profile_visuals`, `privacy`,
`counters`), pero NO tocaba la feature profile-card: ni los docs `profile_cards_private/{uid}` /
`profile_cards_public/{uid}` ni sus prefijos de Storage. Al borrar la cuenta, las imágenes de la
tarjeta de perfil quedaban en Storage (preocupación GDPR Art. 17) y solo se reclamaban con retraso por
el GC programado `reconcileProfileCardMedia` (tras la ventana de gracia + siguiente run).

**Causa raíz:**
La feature profile-card (docs + prefijos `profile_cards_{private,public}/`) se añadió DESPUÉS del
flujo de borrado y se apoyó únicamente en el GC perezoso como recolector. El borrado inmediato nunca
se amplió al nuevo namespace de datos por-usuario; el GC es backstop durable, no erasure inmediato.

**Solución aplicada:**
En `executeAccountDeletion`: (2) se añadieron `profile_cards_private/{uid}` y
`profile_cards_public/{uid}` a los `delete()` de docs sueltos; (7) se añadieron
`deleteStoragePrefix('profile_cards_private/'+uid+'/')` y el público al paso de Storage bloqueante
(fallo ⇒ job retryable, sin borrar Auth), con sus `*PrefixRef` redactados vía `logSafeStoragePrefix`
en los extras de `markStep`. Tests actualizados (single-docs + "cleans up user Storage prefixes"). El
GC programado sigue siendo el backstop durable. Verificado: `npm run typecheck`, `jest accountDeletion`
(14/14), `scan:secrets` limpio.

**Regla futura:**
Al introducir un nuevo namespace de datos por-usuario (prefijo de Storage `X/{uid}/` o doc
`X/{uid}`), reflejarlo SIEMPRE en `executeAccountDeletion`: docs sueltos en el paso de single-docs y
prefijos en el paso de Storage bloqueante (con `logSafeStoragePrefix` en los extras). Un GC perezoso
es backstop, no sustituye el erasure inmediato exigible por GDPR Art. 17. Logs PII-free con los
helpers `logSafe*` existentes.

**Archivos afectados:**
- `functions/src/accountDeletion.ts`
- `functions/src/accountDeletion.test.ts`
- `CLAUDE.md`

### 2026-06-28 — Un copy-scanner de build escanea archivos COMPLETOS (comentarios + disclaimers negados)

**Error detectado:**
Al cablear el `mindboostCopySafetyScanner` global (INFRA-05, espejo de `arenaTerminologyScanner`), el
build falló sobre copy "defensivo" legítimo: `"no es un test de inteligencia"`, `"Not an IQ test"` en
comentarios KDoc, `"NO es un diagnóstico"` en disclaimers, e incluso `"en la vida real"` y `ansiedad`
(palabra-estímulo de memoria). El scanner para en el PRIMER término por corrida, así que la enumeración
manual dejó pasar varios (`TrainingCycle`, `MetricEdgeCasePolicy`, `MemoryExerciseCatalog`,
`SpeedDashboardScreen` continuación de comentario).

**Causa raíz:**
El task lee `file.readText()` ENTERO (no solo strings de UI): comentarios y disclaimers cuentan. El
match es whole-word con folding de acentos y cruza guiones/negaciones (`\b` trata `-`/espacios como
borde), así que un lexema prohibido dispara aunque vaya negado o en un compuesto. La forma femenina no
cae en la masculina (`clínica`≠`clinico`, `diagnóstica`≠`diagnostico`), lo que confunde el grep manual.

**Solución aplicada:**
Lexicon canónico en `core/copy/ForbiddenLexicon.kt` FUERA de los dirs escaneados (no se auto-trippea);
scanner puro literal-free; task con lista de paridad. Se reformuló TODO el copy en alcance (7 módulos +
`AppStrings`) a vocabulario permitido (doc 12 §2: "no es una nota", "proxy funcional", "orientativo"),
incluidos comentarios. En archivos de TEST escaneados, los literales prohibidos se ensamblan por
fragmentos (`"i" + "q"`, `"intelig" + "encia"`) como en `EmotionTerminologyComplianceTest`.

**Regla futura:**
Para un copy/terminology-scanner de build: (1) el lexicon vive FUERA del árbol escaneado; (2) reformula
comentarios y disclaimers, no solo strings de UI — el anti-negación prohíbe nombrar el lexema aunque sea
para negarlo; (3) el scanner para en el primer hit → enumera con un grep COMPLETO (acentos + femenino +
multi-palabra) o itera; (4) polisémicos como frase, nunca palabra suelta (`deterioro cognitivo` no
`cognitivo`; `rindes mejor en el trabajo` no `trabajo`); (5) palabras-estímulo de contenido que choquen
con el lexicon clínico se sustituyen por sinónimos no-clínicos (`ansiedad`→`inquietud`).

**Archivos afectados:**
- `shared/build.gradle.kts` · `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/copy/*`
- `shared/.../mindboost/{creativity,focus,speed,memory}/**` (copy reformulada)
- `CLAUDE.md`

### 2026-06-28 — Añadir un namespace a `AccountBound` exige actualizar el eraser de borrado de cuenta

**Error detectado:**
`AccountBoundErasureCoverageTest.account_bound_is_the_canonical_namespace_set` fallaba: el namespace
`mindboost_profile` (brain-profile, Fase 2A) está en `MindBoostStorageNamespaces.AccountBound` pero NO
en la lista del `SecureNamespaceUserDataEraser` de `AppGraph` → los datos cognitivos del perfil NO se
borraban al eliminar la cuenta (hueco GDPR Art. 17). Pre-existente, no introducido por INFRA-05.

**Causa raíz:**
Al añadir `Profile` a `AccountBound` (Fase 2A) no se actualizó ni la lista del eraser en
`AppGraph.rememberAppGraph()` ni el pin del test. El test de cobertura existe justo para forzar ambas
actualizaciones, y estaba (correctamente) en rojo.

**Solución aplicada:**
Se añadió `profileStore` (`rememberSecureKeyValueStore(MindBoostStorageNamespaces.Profile)`) a las keys
del `remember` y al `listOf(...)` del `SecureNamespaceUserDataEraser`, y `Profile` al set canónico del
test. Borrado inmediato de `mindboost_profile` restaurado; `desktopTest` verde.

**Regla futura:**
Al añadir un namespace a `MindBoostStorageNamespaces.AccountBound`, actualizar SIEMPRE en el mismo
cambio: (1) la lista del `SecureNamespaceUserDataEraser` en `AppGraph` (keys del `remember` + `listOf`),
y (2) el pin de `AccountBoundErasureCoverageTest`. Un namespace en `AccountBound` sin entrada en el
eraser es un dato sensible que nunca se borra.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/core/di/AppGraph.kt`
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/storage/AccountBoundErasureCoverageTest.kt`
- `CLAUDE.md`

### 2026-06-28 — Una state machine de "cascada de guards" choca con 3 reglas Detekt a la vez

**Error detectado:**
`:shared:detekt` rechazó `ScoreStateMachine.kt` (INFRA-01) con 6 `MaxLineLength` (>120): las lambdas
de gate inline con enums totalmente cualificados (`ScoreState.DEVICE_UNRELIABLE`,
`ClaimLevel.PRACTICE_SIGNAL`, `TimingCriticality.RT_CRITICAL`) se pasaban de 120 columnas.

**Causa raíz:**
Una máquina de estados que es una cascada de guards tropieza con TRES reglas Detekt simultáneas y en
tensión: (1) `ReturnCount` (≤2) prohíbe el patrón natural de 11 `if (...) return X` tempranos;
(2) `TooManyFunctions` (≤11 en object) prohíbe partir la cascada en 11 funciones de gate con nombre;
(3) `MaxLineLength` (120) revienta las lambdas inline cuando los enums van cualificados.

**Solución aplicada:**
Tabla ordenada `(state, claim, predicate)` como `private data class Gate`, resuelta con
`gates.firstOrNull { it.fires(inputs) }?.let { noUpdate(it.state, it.claim) } ?: validVerdict(...)`
(1 return, 0 funciones extra), e **import de las entradas de enum** usadas
(`import …ScoreState.DEVICE_UNRELIABLE`, etc.) para acortar las líneas por debajo de 120 sin tocar el
baseline.

**Regla futura:**
Para las state machines de cascada que vienen (INFRA-02/03): modelar los gates como una tabla de datos
ordenada resuelta con `firstOrNull`/`firstNotNullOfOrNull` (no `return` tempranos ni N funciones con
nombre), e importar las entradas de enum para no superar `MaxLineLength`. Nunca regenerar el baseline
para silenciar. Detekt en este repo: `ReturnCount`≤2, `TooManyFunctions`≤11/object, `MaxLineLength`=120.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/ScoreStateMachine.kt`
- `CLAUDE.md`

### 2026-06-28 — El "bug" del plan (computeRtNanos lanza) estaba mal ubicado; y patrón para señal de plataforma testeable sin device

**Error detectado:**
El plan INFRA-02 afirmaba que `TimebasePolicy.computeRtNanos` LANZA con rt<0. En el repo real
`computeRtNanos` devuelve el delta con signo y NO lanza; el guard de RT negativo está en
`TrialEngine.kt:71` `require(TimebasePolicy.isValidRt(rtNanos))`. Seguir el plan al pie habría llevado
a "arreglar" el archivo equivocado.

**Causa raíz:**
El plan se redactó desde una auditoría previa y ubicó el throw en el símbolo equivocado. Regla FASE-0:
verificar CADA símbolo del plan contra el repo antes de tocar código.

**Solución aplicada:**
`signedRt` se añadió como helper PURO aditivo en `core/timing/TimingPolicy.kt` (sin tocar `TrialEngine`,
cuyo guard sirve a E5/E12 = fase futura). El jank-gate se implementó con la medición real desacoplada en
un calculador PURO `FrameStatsCalculator` (testeable en `desktopTest` con streams sintéticos de frames),
y `FrameTimingProbe` como **interface + `@Composable expect fun rememberFrameTimingProbe()`** (no
`expect class`, idioma del repo, ver `SpeedTimebaseHandle`) con actual Android (Choreographer) y
fallback no-op desktop/iOS. El `SpeedSessionStateHolder` recibió `frameProbe: FrameTimingProbe =
NoOpFrameTimingProbe` (param con default → 0 impacto en los 1800 tests; Android inyecta el real).

**Regla futura:**
(1) Para una señal medida en plataforma (jank, sensores), extraer SIEMPRE un calculador PURO que el
`actual` delega, para poder testear el umbral sin dispositivo. (2) Para inyectar una dependencia de
plataforma en un holder vivo sin romper tests, añadir un param con default no-op (último), y proveer el
real solo desde el composable de plataforma. (3) El clasificador timebase que depende de enums de un
feature (TimestampSource/MetricThresholds de `speed`) vive en ESE feature, no en `core` (core no
depende de features); solo `FrameStats`/`FrameTimingProbe` neutrales viven en `core/timing`.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/timing/*` (NUEVO)
- `shared/src/{android,desktop,ios}Main/.../core/timing/FrameTimingProbe.*.kt` (NUEVO actuals)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/TimebaseClassifier.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/presentation/{SpeedSessionStateHolder,ui/SpeedExerciseScreen}.kt`
- `CLAUDE.md`

### 2026-06-28 — El `ProfileUpdateGate` del plan asumía un aggregator mutable; el real es PURO

**Error detectado:**
El plan (doc 04 §4 / INFRA-06) define `ProfileUpdateGate { aggregator.contribute(result.toModuleContribution()) }`
y lo ubica en `core/scoring`. En el repo real `MindBoostProfileAggregator` es una función PURA
`aggregate(userId, List<ModuleContribution>, nowMs): MindBoostProfile` — NO tiene `contribute(...)`. Seguir
el plan literal no compilaría, y además invertiría la dirección de dependencias (core → feature `profile`).

**Causa raíz:**
El plan se redactó sobre un diseño hipotético del aggregator. Regla FASE-0: verificar la API real antes de
cablear. Además `core` debe permanecer neutral (no depender de features); el bridge ScoreResult→perfil
acopla `core/scoring` con `profile/domain/model` (ModuleContribution/CognitiveModuleId).

**Solución aplicada:**
`MinSampleGate` (tabla golden de nMin) queda en `core/scoring` (puro, sin deps de feature). El
`ProfileUpdateGate` + `toModuleContribution` + `ScoreModuleId.toCognitiveModuleId` se ubican en el módulo
`profile` (profile → core, dirección correcta) y exponen `contributionFor(result): ModuleContribution?`
(null = ausencia de update, nunca un valor bajo) + `contributions(list)`, que el caller pasa al aggregator
PURO. Un `primarySignal` no normalizado (fuera de 0..1) mapea a `normalizedScore=null` (no lanza; el
aggregator lo trata como no-cualificante).

**Regla futura:**
(1) El veredicto sellado se transforma en `ModuleContribution?` y se PASA al aggregator puro; nunca asumir
un sink mutable. (2) Todo bridge que acople `core` con un feature vive en el FEATURE, no en `core`. (3) La
migración de `DefaultMetricsEngine` para consumir `MinSampleGate` (subir MIN_N=3..5 → ≥20) se DIFIRIÓ: es un
cambio de calibración que reclasifica muestras pequeñas y rompería los golden de métricas; hacerlo es una
tarea deliberada con actualización de goldens, no un efecto colateral de plumbing.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/MinSampleGate.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/aggregation/ProfileUpdateGate.kt` (NUEVO)
- `CLAUDE.md`

### 2026-06-28 — Añadir una variante a un `sealed UiState` y su renderer son la MISMA fase, no dos

**Error detectado:**
El plan dividía el trabajo en "P2 holder flow" (añadir `MemorySessionUiState.Processing` + el gate) y
"P3 UI" (pintar la fase processing) como fases separadas. Implementar solo P2 deja el build roto: el
`when (uiState)` exhaustivo de `MemorySessionScreen` (sin `else`, estilo del repo) no compila al faltar
la rama `Processing`.

**Causa raíz:**
Un `sealed interface` de UiState fuerza un `when` exhaustivo en su renderer. Añadir una variante y NO
tocar el renderer es un error de compilación, no una fase posterior. La suposición del plan (estado y
render desacoplables) era incorrecta para sealed types con `when` exhaustivo.

**Solución aplicada:**
P2 añadió la variante `Processing`, el `processingGate`/`submitProcessing`, la fase en `runSession`
(`presentProcessing` con `processingTasksOf` exhaustivo: ComplexSpan→tasks; List/Pairs→emptyList, que
saltan a recall sin cambios) y `lastSpanTrial` en `finish`; e INCLUYÓ obligatoriamente la rama
`ProcessingContent` en el screen (afford. Sí/No por prompt) para compilar. El test se actualizó en el
mismo cambio (los flujos que tocan EX1 deben responder processing antes de recall).

**Regla futura:**
Al añadir una variante a un `sealed` UiState (u otro sealed con `when` exhaustivo), actualizar TODOS
sus `when` (renderers, mappers, holders) en el MISMO cambio — nunca planificar "añadir estado" y
"renderizar estado" como fases distintas. Un flujo turn-based con gate `CompletableDeferred` se extiende
añadiendo el campo gate, el callback `submit*`, el `await*`, limpiarlo en `abort()`, y presentar la fase
solo cuando aplica (rama exhaustiva del plan; las demás devuelven vacío y saltan la fase).

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/presentation/state/MemoryUiState.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/presentation/MemorySessionStateHolder.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/presentation/ui/MemorySessionScreen.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/memory/presentation/MemorySessionStateHolderTest.kt`
- `CLAUDE.md`

### 2026-06-28 — Un fallback silencioso asimétrico convierte un test de discriminación en gameable

**Error detectado:**
DR-P2 (flujo de recuerdo diferido EX3): `buildTest` resolvía las palabras estudiadas HONESTAMENTE (si
ninguna resuelve → null → NothingDue), pero el muestreo de distractores iba en
`runCatching{…}.getOrDefault(emptyList())`. Si el sampler fallaba (ejercicio no soportado, o pool − exclusiones
< 6), se PRESENTABA un test SIN señuelos: `distractorCount==0` → `falseAlarmRate` fija 0.0 →
`isDiscriminating` siempre true → "seleccionar todo" daba un 100% gratis — justo el free-100% que el KDoc
afirmaba prevenir. Latente hoy (pool ≥100, solo ~5 excluidas) pero hueco de honestidad real. Lo cazaron
las DOS lentes del workflow adversarial (MEDIUM/LOW).

**Causa raíz:**
Asimetría: un lado del test de discriminación (estudiadas) fallaba honestamente; el otro (distractores)
degradaba en silencio a un test sin señuelos en vez de tratarse como "no presentable". Un test de
reconocimiento SIN señuelos no puede medir discriminación → no debe puntuarse.

**Solución aplicada (detente y corrige):**
Ambos lados fallan SIMÉTRICAMENTE: `buildTest` devuelve null (→ NothingDue) si no resuelve ninguna
estudiada O si no se pueden muestrear los `DISTRACTOR_COUNT` señuelos requeridos. Nunca se presenta un
test sin señuelos. Test de regresión: ítem DUE con ejercicio no soportado (sampler lanza) → NothingDue,
no un Finished con distractorCount==0.

**Regla futura:**
En un test de discriminación (señal corregida por false-alarms), AMBOS lados —los objetivos Y los
señuelos— deben fallar de la misma forma honesta: si cualquiera no se puede presentar en su totalidad, el
ítem se SALTA, nunca se puntúa degradado. Un `runCatching{…}.getOrDefault(emptyList())` sobre el muestreo
de señuelos es un fallback silencioso que rompe el gate anti-gaming (FAR estructuralmente 0 ⇒ siempre
discriminante). Exigir el nº COMPLETO de señuelos (no ≥1) antes de presentar; probar el camino de fallo.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/presentation/DeferredRecallSessionStateHolder.kt`
- `shared/src/desktopTest/.../memory/presentation/DeferredRecallSessionStateHolderTest.kt`
- `CLAUDE.md`

### 2026-06-28 — Un probe cuya respuesta no es seleccionable corrompe la señal en silencio (infra-crédito)

**Error detectado:**
EX4-P2b: el `pairsTrialOf` del holder derivaba `targetCount = scored.studiedCount = plan.probes.size`. A
dificultad alta (RETRIEVAL ≥ 0.5) `Ex4PairsGenerator` emite probes BACKWARD cuyo `expectedTarget` es la
PISTA (cue) del par, pero el recall flat-multi-select solo ofrece los TARGETS como candidatos (las pistas
no están). Así, los probes backward son inalcanzables (siempre OMISSION), pero contaban en `targetCount`:
un usuario honesto perfecto quedaba **capado a hitRate 0.5** en sesiones bidireccionales, y el índice
asociativo persistido mezclaba escalas 0..1 (unidireccional) y 0..0.5 (bidireccional). Silencioso (sin
crash, sin log). Lo cazaron las DOS lentes del workflow adversarial (HIGH); ningún test lo veía porque el
único test end-to-end corría a EASIEST (unidireccional).

**Causa raíz:**
La derivación asumió que TODO probe es contestable, sin verificar que el conjunto de candidatos del recall
fuese fiel al conjunto de probes. Un probe cuya respuesta esperada no está entre los candidatos no es
medible y no debe entrar en el denominador.

**Solución aplicada (detente y corrige):**
`pairsTrialOf` filtra a probes ANSWERABLE (`expectedTarget.id in candidateIds`); `targetCount` y `hits`
se cuentan solo sobre ellos (mismo denominador → hitRate comparable entre dificultades); devuelve `null`
si no hay ninguno (nada que registrar). Los probes backward (pista no candidata) quedan excluidos del
score en V1 (medir B→A real exige un recall por-probe con pistas como opciones — diferido). Test que fija
el arreglo: a RETRIEVAL=MAX, `probes.size == 2*forward` (confirma bidireccional) y `targetCount == forward`
con selección completa → `hits == targetCount` (hitRate 1.0, no 0.5).

**Regla futura:**
Al derivar una señal de un flujo de captura, VERIFICAR que el conjunto presentado al usuario (candidatos,
opciones) sea fiel al conjunto evaluado (probes/preguntas): un ítem cuya respuesta correcta no es
seleccionable NO entra en el denominador (infla a la baja en silencio). Para señales accuracy-based,
`targetCount` debe ser el nº de ítems REALMENTE contestables, no el total generado. Probar la derivación
en TODAS las ramas de dificultad (no solo la más fácil): un test a EASIEST enmascara bugs de las ramas
duras (bidireccional, interferencia, etc.).

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/presentation/MemorySessionStateHolder.kt`
- `shared/src/desktopTest/.../memory/presentation/MemorySessionStateHolderTest.kt`
- `CLAUDE.md`

### 2026-06-28 — Diseñar la señal/anti-gaming ANTES de verificar el flujo de captura vivo = puntuación gameable

**Error detectado:**
EX4-P1 (Pares Asociados) modeló la señal como hit-rate crudo (aciertos/probes) y el anti-gaming como
entropía de POSICIÓN de opción por probe. Al aterrizar P2 contra el holder real, el flujo de recall de
EX4 es un MULTI-SELECT plano sobre candidatos (targets + 6 distractores) → `Set<String>`, y `scorePairs`
marca HIT sii el target está en `selected`. Es decir: **seleccionar todo → 100% de acierto** (señal
inflable), y el adversario de "posición" NI SIQUIERA EXISTE en ese flujo.

**Causa raíz:**
Se diseñó el scorer/anti-gaming sobre un flujo de captura ASUMIDO (cued recall por probe con posiciones)
sin verificar el flujo vivo (FASE-0). Un hit-rate de recognition multi-select es trivialmente gameable y
viola el charter ("no señales fabricadas; el anti-gaming solo cuenta si se prueba contra el adversario
REAL"). El anti-gaming de P1 probaba un adversario imposible.

**Solución aplicada (detente y corrige + documenta):**
Señal corregida a DISCRIMINACIÓN: `PairsTrial(targetCount, hits, distractorCount, falseAlarms)`; un trial
solo cuenta si DISCRIMINA (false-alarm rate ≤ 0.34); el índice es el hit-rate medio SOLO sobre trials
discriminantes; un bloque con <85% discriminantes → INVALIDATED (espejo del gate de validez de
procesamiento del span). "Seleccionar todo" → FAR=1 → INVALIDATED (probado como adversario sintético). Se
RETIRÓ `MemoryPairsAntiGaming` (modelo de posición erróneo): el gate de discriminación ES el anti-gaming
testeado (no hay secuencia de respuesta por probe en el multi-select). El adapter volvió a manejar
INVALIDATED (`onlyInvalidated` → QUALITY_LOW), igual que el del span.

**Regla futura:**
Antes de diseñar el scorer o el anti-gaming de un ejercicio, VERIFICAR el flujo de captura vivo real (qué
señal produce de verdad el holder), no el asumido por el plan. Para recognition de selección múltiple, la
señal honesta DEBE corregir la sobre-selección (descuento por false-alarms / gate de discriminación), no
el hit-rate crudo (gameable con "seleccionar todo"). El anti-gaming debe probarse contra el adversario que
EXISTE en ese flujo; si el gate de validez ya neutraliza al adversario (→INVALIDATED) y se prueba, basta —
no inventar un detector para una señal que el flujo no captura.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/domain/scoring/AssociativePairsScorer.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/domain/scoring/MemoryPairsProfileAdapter.kt`
- `shared/src/.../memory/domain/scoring/MemoryPairsAntiGaming.kt` (ELIMINADO) + su test (ELIMINADO)
- `shared/src/desktopTest/.../memory/domain/scoring/{AssociativePairsScorerTest,MemoryPairsProfileAdapterTest}.kt`
- `CLAUDE.md`

### 2026-06-28 — `dataSufficiency` ≠ `confidence`: una contribución vía gate puede descartarse en silencio

**Error detectado:**
Tras cablear MEMORIA al brain-profile (EX1 span → `ProfileUpdateGate`), la dimensión WORKING_MEMORY se
quedaba CALIBRATING PARA SIEMPRE pese a un `ScoreResult` VALID con ≥20 trials: la contribución se
descartaba silenciosamente y los docstrings ("EMERGING/preliminary tras ≥20 trials") eran falsos.

**Causa raíz:**
`ScoreResult.toModuleContribution()` mapeaba `dataSufficiency = sampling.confidence`. Para un bloque
único el adapter fija `confidence = 0.4` (estabilidad baja, NO falta de muestra). El agregador exige
`dataSufficiency >= minSufficiency(0.5)` para contar la contribución → 0.4 < 0.5 → descartada. Se
confundió "pocos bloques ⇒ menos estable" (confidence) con "muestra insuficiente" (dataSufficiency).
Ningún test lo detectaba: los unit tests asertaban la `ModuleContribution` aislada, no el plegado
end-to-end por `MindBoostProfileAggregator`.

**Solución aplicada:**
`dataSufficiency` ahora deriva de la suficiencia de MUESTRA (`nEffective/minNRequired`, =1.0 cuando el
veredicto es VALID porque ya pasó MinSampleGate); `confidence` sigue separado y mantiene la señal en
EMERGING (no ESTABLISHED). Test end-to-end añadido (≥20 trials → `service.refresh` → WORKING_MEMORY
EMERGING/0.5/preliminary). Blast radius en producción: solo MEMORIA (Focus/Speed/Logic/Lenguaje/Emociones
construyen `ModuleContribution` directamente, no por el gate).

**Regla futura:**
`dataSufficiency` (tamaño de muestra) y `confidence` (certeza/estabilidad) son ORTOGONALES: nunca mapear
uno desde el otro. Todo ProfileSource que produzca su contribución vía `ProfileUpdateGate` debe
verificarse END-TO-END a través del `MindBoostProfileAggregator` (un test sobre la contribución aislada
enmascara el descarte por `minSufficiency`). Un veredicto VALID ya pasó MinSampleGate ⇒
dataSufficiency=1.0. Recordatorio: un ProfileSource NO debe usar `SystemEpochClock` por defecto (rompe
desktopTest, kotlinx-datetime 0.7.1); pasar `timestampMs=0L` si el sello es metadato no consumido (ver
2026-05-29). Comparar Doubles calculados (media ponderada) con tolerancia, no igualdad exacta.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/aggregation/ProfileUpdateGate.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/data/adapter/MemoryProfileSource.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/data/MindBoostProfileSources.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/presentation/RememberBrainProfileStateHolder.kt`
- `shared/src/desktopTest/.../profile/adapter/{MemoryProfileSourceTest,MindBoostProfileSourcesTest}.kt` · `.../aggregation/ProfileUpdateGateTest.kt`
- `CLAUDE.md`

### 2026-06-28 — Un gate que valida DATOS estructurados es un test en `check`, no un scanner de texto Gradle

**Error detectado:**
El plan INFRA-04 describe el gate de bancos como "espejo de `arenaTerminologyScanner`" (una task Gradle).
Pero validar un banco (tamaño efectivo, holdout servido, balance chi² de posición, inter-rater) requiere
introspeccionar OBJETOS Kotlin compilados, no escanear texto fuente — una task Gradle no puede hacerlo.

**Causa raíz:**
`arenaTerminologyScanner`/`mindboostCopySafetyScanner` funcionan porque leen `file.readText()` buscando
lexemas; la validación de bancos opera sobre estructura/estadística, no sobre substrings.

**Solución aplicada:**
El gate es un test `BankValidationGateTest` en `desktopTest` que corre `BankValidator` sobre el registro
`ProfileUpdatingBanks.all`. Verificado que `desktopTest` ∈ `:shared:check` (`check --dry-run` lista 13
refs a desktopTest), así que el test ES el gate de build. El registro está VACÍO hoy (ningún ejercicio es
profile-updating): los bancos fijos existentes (Logic 6, Language 36, Emotions, Creativity 14) quedan
"explícitamente no-profile" por ausencia del registro; migrar un banco al contrato es trabajo P1
por-ejercicio (futuro), hecho SOLO cuando ese ejercicio enciende profile-update. `core/bank` es neutral
(sin deps de feature).

**Regla futura:**
Para un gate que valide datos estructurados/estadísticos (bancos, anti-gaming sintéticos, INFRA-03):
implementarlo como test en `desktopTest` (no como task Gradle de texto), confirmar que `desktopTest` ∈
`check`, y usar un registro opt-in (vacío hasta que algo cumpla) en vez de migrar contenido vivo
prematuramente. Un gate sobre un registro vacío es honesto si la lógica de rechazo está probada aparte
(aquí `BankValidatorTest` con casos adversarios).

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/bank/*` (NUEVO)
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/bank/*` (NUEVO)
- `CLAUDE.md`

### 2026-06-28 — Un framework anti-gaming "cuenta" solo con telemetría de defaults-limpios + suite red-team

**Error detectado:**
No es un bug: decisión de diseño para que el invariante "anti-gaming solo cuenta si testeado con
adversarios sintéticos" sea verificable de verdad (INFRA-03), sin falsos positivos sobre usuarios reales.

**Causa raíz:**
Un detector "diseñado" no cuenta; necesita (1) un perfil adversario que lo dispare Y (2) un usuario
honesto realista que NO dispare ninguno. Si la telemetría no tiene defaults limpios, el control de
falsos positivos es frágil.

**Solución aplicada:**
`TrialTelemetry` con TODOS los campos en valor neutro/limpio por defecto → `TrialTelemetry()` no dispara
ningún detector (control FP base). 11 detectores PUROS (`GamingDetector` → `DetectorSignal` →
`AntiGamingFramework.aggregate` que toma la acción más severa). Suite red-team `AntiGamingRedTeamTest`
(D12): cada adversario dispara su detector, un `honestUser` realista (12 trials, RT correlado con
dificultad, keys balanceadas) no dispara NINGUNO, y `safetyExit` emocional NUNCA penaliza (Regla #8: D11
hace `idle` si `isEmotionalOrFrustration`). Estadística pura (cv/correlación/entropía Shannon) en helpers
top-level testeables. Acciones: WITHHOLD_PROFILE/INVALIDATE; nunca emite copy ni escribe score bajo.

**Regla futura:**
Para un detector/gate estadístico: (1) telemetría con defaults limpios (el "usuario vacío" no dispara);
(2) cada detector necesita su par adversario-dispara + honesto-no-dispara en una suite red-team; (3) los
controles de seguridad (Regla #8) se prueban explícitamente (perfil que cumpliría el patrón pero está
exento). Wiring a producción se DIFIERE: D2/D4 consumen señales que SsrtCalculator/TrialClassifier ya
producen (el adapter las alimenta), y D6 inyectar entropía real al `entropyFactor` del AGRS (reemplazar
`spatialEntropy=1.0` en los holders de decisiones) es integración por-ejercicio P1, no parte del framework.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/antigaming/*` (NUEVO)
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/antigaming/*` (NUEVO)
- `CLAUDE.md`

### 2026-06-28 — Cablear un ejercicio P1 al perfil: núcleo aditivo (scorer+adapter) vs wiring vivo

**Error detectado:**
No es un bug: P1 (cablear un ejercicio para que actualice el perfil) es trabajo categóricamente distinto
al P0 (infra). EX1 Span (Memoria): el `DefaultMemoryMetricsEngine` ya producía `WORKING_MEMORY_SPAN =
trial.hits` — un span SIMPLE sin gate de validez de procesamiento, que V1_01 llama "span simple
disfrazado" e INSERVIBLE para perfil. El `ComplexSpanScorer` requerido NO existía; el pipeline vivo
(`ScoredTrial`) no captura `processingCorrect`; y EX1 tope hoy = `CALIBRATING` (sin detector anti-gaming
de Memoria → `DetectorCoverage.NONE`).

**Causa raíz:**
"Profile-update: sí (tras gates)" del plan está gateado por componentes ausentes. Cablear de verdad al
perfil es integración multi-capa (captura viva de processing + persistencia + adapter del brain-profile),
de alto riesgo y SIN valor de perfil hoy (EX1 igual sería CALIBRATING).

**Solución aplicada:**
Núcleo ADITIVO y seguro: `ComplexSpanScorer` (gate processingAccuracy≥0.85 → INVALIDATED; span
accuracy-based mean serial-correct sobre trials válidos; sessionSpan ponderado, null si no hay bloques
válidos) + `MemorySpanProfileAdapter` (memory→core: SpanBlockResult→ScoreResult vía ScoreResultFactory,
RT_IRRELEVANT, BankIntegrity.PROCEDURAL, MinSampleGate.ACCURACY_RT_BASE para nMin, `detectorCoverage`
param default NONE → tope CALIBRATING honesto). Tests unit+adversarial (minEffort/processing-azar →
INVALIDATED)+integración (vía ProfileUpdateGate→ModuleContribution(MEMORIA))+golden. NINGÚN edit a la
sesión/modelo vivos.

**Regla futura:**
Para un ejercicio P1: (1) implementar primero el scorer honesto + el adapter a ScoreResult como núcleo
ADITIVO y testeado (incluye los adversarios que el doc enumera); (2) el adapter expone `detectorCoverage`
con default NONE para que el tope CALIBRATING sea explícito hasta que exista el detector anti-gaming; (3)
el min-n de sesión usa MinSampleGate (autoridad central) y el por-bloque vive en el scorer; (4) RT nunca
entra en el signal/ModuleContribution (accuracy-based); (5) DIFERIR el wiring vivo (captura de processing
en el holder + persistencia + adapter del brain-profile) — no aporta valor de perfil mientras el estado
tope sea CALIBRATING, y es integración multi-capa de riesgo.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/memory/domain/scoring/{ComplexSpanScorer,MemorySpanProfileAdapter}.kt` (NUEVO)
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/memory/domain/scoring/*` (NUEVO)
- `CLAUDE.md`

### 2026-07-01 — `@file:Suppress("FunctionNaming")` NO cubre `TopLevelPropertyNaming`; los `const val` top-level van en SCREAMING_SNAKE_CASE

**Error detectado:**
`:shared:detekt` rechazó `PhotoUploadLoader.kt` (design/components, nuevo) con 4 `TopLevelPropertyNaming`
por `private const val CornerRatio/CycleMillis/DotSwingStartDeg/DotSwingEndDeg` en PascalCase, pese al
`@file:Suppress("MagicNumber","FunctionNaming","LongMethod")` del archivo.

**Causa raíz:**
Se asumió que el patrón de supresión a nivel de archivo para Composables (`FunctionNaming`/`MagicNumber`/
`LongMethod`) también legitimaba el nombre de las constantes. Son reglas ORTOGONALES: `FunctionNaming`
gobierna funciones; `TopLevelPropertyNaming` exige `[A-Z][_A-Z0-9]*` (SCREAMING_SNAKE_CASE) para toda
propiedad top-level compile-time-constante (`const val`). Un `private val = 48.dp` (no `const`) NO cae en
esta regla (inicializador no constante) y no se marca.

**Solución aplicada:**
Renombrados los `const val` a `CORNER_RATIO`, `CYCLE_MILLIS`, `DOT_SWING_START_DEG`, `DOT_SWING_END_DEG`.
`:shared:compileKotlinDesktop` verde, `PhotoUploadLoader.kt` desaparece de los findings de detekt, sus 9
tests de `desktopTest` verdes, y `:androidApp:compileDebugKotlin` verde.

**Regla futura:**
Todo `const val` top-level (aunque sea `private` y viva en un archivo de Composables) va en
SCREAMING_SNAKE_CASE por `TopLevelPropertyNaming`. El `@file:Suppress` de reglas de Composable NO cubre el
naming de propiedades; arreglar/suprimir cada regla por separado. Un `val` top-level con inicializador no
constante (p. ej. `48.dp`) sí puede ir en PascalCase (no es `const`). NOTA de entorno: el árbol de trabajo
se muta en paralelo por otros agentes (Vibe Kanban/n8n) — la lista de ofensores de un gate de módulo
(`detekt`) cambió entre dos corridas seguidas (todos `??` untracked, p. ej. AppTextField→AppBarsLoader/
AppLoadingOverlay). Verifica TU archivo confirmando que NO aparece en los findings; no "arregles" WIP
untracked ajeno.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/design/components/PhotoUploadLoader.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/design/components/PhotoUploadLoaderTest.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/design/{AppColors,AppStrings}.kt`

### 2026-07-01 — El copy-scanner tiene variantes por source-set: verificar un `commonTest`/`desktopTest` exige su variante, no la tarea base

**Error detectado:**
Un comentario en `ReversibilityScenarioBankTest.kt` (commonTest, `decisiones` ∈ scope) decía "The product is
non-clinical … (Clinical/IQ/gambling terms …)". `:shared:mindboostCopySafetyScannerCommonTest` falló con
"Forbidden copy terminology found: 'IQ'". El fallo apareció DOS fases después de crear el archivo, la primera
vez que se corrió la variante CommonTest del scanner.

**Causa raíz:**
Al crear el archivo se verificó SOLO con `:shared:mindboostCopySafetyScanner` (tarea BASE, que escanea
`commonMain`), NO con `…ScannerCommonTest` (que escanea `commonTest`). El scanner lee archivos COMPLETOS
incluidos comentarios (regla 2026-06-28) y la anti-negación prohíbe nombrar el lexema aunque sea para NEGARLO
—"non-clinical"/"Clinical"/"IQ"/"gambling" en un comentario disparan—. Como la variante base no toca
`commonTest`, la violación quedó latente. El scanner PARA en el primer hit, así que un fallo puede ocultar más
términos aguas abajo.

**Solución aplicada:**
Comentario reescrito sin nombrar ningún lexema prohibido ("The `excluded` stems below reject the sensitive
real-world domains … the build copy-safety scanner additionally enforces the forbidden lexicon"). Re-corridas
las TRES variantes (base + `…CommonTest` + `…DesktopTest`) → verdes. Confirmado que los stems del `excluded`
(dinero/invers/financ/abogad/demanda/suicid/autolesi/enferm) NO están en el lexicon (pasan como datos del test).

**Regla futura:**
Al añadir/editar un archivo en un dir escaneado por el copy-scanner (focus/memory/speed/logic/decisiones/
language/creativity + design), correr la VARIANTE de su source-set: `…ScannerCommonTest` para `commonTest`,
`…ScannerDesktopTest` para `desktopTest`; la tarea base `mindboostCopySafetyScanner` solo cubre `commonMain`.
Como el scanner para en el primer hit y lee comentarios, tras un fix re-correr hasta VERDE. No nombrar un
lexema prohibido ni para negarlo, tampoco en comentarios de test.

**Archivos afectados:**
- `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/decisiones/domain/content/ReversibilityScenarioBankTest.kt`
