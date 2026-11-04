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
- **Focus V5 — autoridad vigente:** [`docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md`](docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md)
  es el índice de procedencia y disposición del motor de Enfoque; los artefactos legacy del bundle V5 (sellados en
  `SHA256_MANIFEST_V5.txt`) se leen SOLO a través del índice (2 tienen el sello `VOID_PROVENANCE`). Verificado por
  `scripts/validate-authority-entrypoints.mjs`.
  <!-- FOCUS_V5_AUTHORITY_BLOCK_START -->
  FOCUS_V5_CURRENT_AUTHORITY_INDEX: docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md
  <!-- FOCUS_V5_AUTHORITY_BLOCK_END -->

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

### 2026-09-15 — Una reimplementación en espacio temporal demuestra propiedades de una FUNCIÓN, nunca de un WIRING: el gate vivía en el call-site y la auditoría lo declaró ausente; «no lo encontré leyendo» no es «no existe productor»; y un guard de placeholders `XXX` colisiona con el numeral romano XXX

**Error detectado:**
Auditoría TOTAL de solo lectura del motor de Enfoque (35 PARTES; refutación adversarial por 24 agentes sin Gradle sobre 16 claims, 47 votos). Tres errores propios, cazados por la refutación y no por mí. (1) **C7**: afirmé que `PersonalizedSessionScorer` «ignora la admisión» porque reimplementé la función en el scratchpad a partir de su firma y ninguna rama leía `admission`; refutado 3/3: el único call-site de producción gatea con `if (admitted && scoreSurfacesEnabled())` (`FocusSessionStateHolder.kt:463-469`). La contención existía; vivía en el llamador. (2) **C14(c)**: afirmé «`DUAL_TASK_COST` ≡ 0» infiriendo que no existía bloque simple de referencia; existe (`E8DuoGenerator.kt:37-45` sirve celdas dual y simple en la misma sesión; `ParadigmMetricsTest.kt:59` produce 0,4). El defecto real —la secundaria no puntuada— ya tenía fila dueña (`MB-FOCUS-0141`). (3) El ensamblador del informe llevaba un guard de placeholders (`TODO`/`TBD`/`XXX` con borde de palabra) que abortó sobre el título legítimo «PARTE XXX» (numeral romano 30). Y cuatro atribuciones corregidas por las lentes: el motor adaptativo vive en `AdaptiveEngine.kt` (no existe `DefaultAdaptiveEngine.kt`), `withdrawn()` vive en `FocusSessionAssembler` (no en `LocalFocusRepositories`), E12 NO es inerte (consume `INHIBITORY_DEMAND`), y el sesgo del SSRT de E4 es CONSERVADOR, no halagador.

**Causa raíz:**
(1) Una reimplementación en espacio temporal reproduce la FIRMA del símbolo y, con ella, todas las propiedades de la función —y ninguna del wiring—: la admisión no era un parámetro del scorer, era una condición del llamador, así que el modelo temporal era fiel a la función y ciego al sistema. (2) «No encontré X» se leyó como «X no existe»: una ausencia en la LECTURA se convirtió en una afirmación sobre el PRODUCTOR sin un grep del generador. (3) Un guard textual de placeholders sin lista de exclusión colisiona con cualquier token legítimo del mismo alfabeto.

**Solución aplicada:**
C7 retirado del informe y su residuo registrado en la fila dueña (`MB-ENG-0007`: el escáner pinnea solo `admit`, no el gate del llamador); C14(c) sustituido por la cita a `MB-FOCUS-0141`; atribuciones corregidas en el informe, con un ANEXO de refutación que declara por claim los votos, los refutados y la corrección de precisión aplicada; guard del ensamblador estrechado retirando `PARTE XXX` del texto ANTES de casar. Ledger: 12 hallazgos nuevos (`MB-FOCUS-0166`..`0177`, 1 P0) y 11 filas actualizadas con metadatos, ningún cambio de estado. Kotlin tocado: 0.

**Regla futura:**
Cuando reimplementes un símbolo en espacio temporal para falsar una afirmación, **la reimplementación solo puede sostener afirmaciones sobre la FUNCIÓN**; toda afirmación sobre el WIRING («X se llama sin gate», «nadie comprueba Y antes de Z») exige leer el CALL-SITE real, y el grep del símbolo con sus llamadores es la evidencia, no el modelo. Antes de afirmar «≡ 0», «nunca se produce» o «no existe productor», **grepea el productor** (generador, fixture, test) y cita la línea que lo produce o la ausencia del grep: «no lo encontré leyendo» no es evidencia. Cuando la refutación corrija una atribución (fichero, línea, severidad, dirección del sesgo), **incorpora la corrección y deja el hallazgo más estrecho y más verdadero**: ese es el resultado que la refutación existe para producir, no una derrota; y un claim con CERO votos sigue siendo NO JUZGADO (aquí C1 recibió 2 y se reporta con ese techo, no como 3/3). Trata un guard de placeholders como un matcher de léxico: **excluye los tokens legítimos del mismo alfabeto** (numerales romanos, siglas) antes de casar, o abortará sobre contenido correcto. Corolarios de entorno, tres conocidos que volvieron a pagar: (1) `node -e` y los heredocs de la herramienta Bash degradan `\\b` a `\b`: los scripts con regex van con la herramienta Write; (2) un validador que resuelve su ruta desde `dirname(import.meta.url)` exige replicar `sandbox/scripts/` + `sandbox/<fichero>` en el scratchpad; (3) el `Last update` de cada fila se lee y se compara ANTES de pisarlo, el CAS se ancla en la línea `Canonical SHA-256` (bajo el lock, no solo antes de pedirlo), `writeLedgerUnderLock` devuelve `{ok, reason}` y la identidad byte a byte se verifica DESPUÉS de escribir.

**Archivos afectados:**
- `docs/audits/AUDITORIA_TOTAL_ENFOQUE_MOLVEX_2026-09-14.md` (nuevo: 35 PARTES, ANEXO de refutación, §58/§59/§49/§42/§62)
- `MINDBOOST_AUDIT_LEDGER.md` (`MB-FOCUS-0166`..`0177` nuevos OPEN; `0135`/`0126`/`0096`/`0099`/`0136`/`MB-ENG-0007`/`0062`/`0141`/`0164`/`0101`/`0093` con metadatos; ningún cambio de estado) · **Kotlin tocado: 0 ficheros**

### 2026-09-14 — Un test que exige que la autoridad de evidencia esté VACÍA se pone en ROJO con la primera evidencia real; un runbook hereda las premisas de quien lo escribió (la línea base no era opcional y el SDK remoto no refresca antes de 12 h); y un guion honesto pero LENTO cae por un veredicto que PRECEDE al que se quiere observar, así que no lo atestigua

**Error detectado:**
Corrida del runbook de Enfoque en el emulador con el sensor de ciclo de vida encendido por Remote Config
(acotado al app.id debug, autorizado por el fundador). Cuatro cosas, tres sobre trabajo propio. (1) El
mutante P3-11 de `evidence-mutation-tests.mjs`, escrito en la fase anterior, tenía como precondición
`runs.length === 0` sobre `PORTFOLIO_RUNTIME_EVIDENCE.json`: pasaba SOLO mientras el sink estuviera vacío,
así que la primera corrida real registrada lo habría puesto en ROJO —un guard que denuncia la evidencia que
existe para proteger—. (2) El runbook daba por hecho que se podía entrar a entrenar: Focus Home exige una
línea base registrada, y con el flag OFF la base previa no había pasado la admisión (§7.1). Y el SDK de
Remote Config no vuelve a buscar antes de **12 h**: publicar la plantilla no cambió nada en una app ya
arrancada hasta forzar el fetch (force-stop + `last_fetch_time_in_millis=-1` en
`shared_prefs/frc_<app-id>_firebase_settings.xml` vía `run-as`, sólo posible en debug). (3) La corrida 1
con el guion de 91 filas (~1-1,5 s por respuesta) fue DENEGADA por `RESPONSE_PATTERN_NOT_INTERPRETABLE`:
E1 estrecha `responseWindowMs` de 1400 a 950 ms, así que un respondedor honesto pero lento queda
indistinguible de uno ciego. Como ese veredicto precede a `LIFECYCLE_NOT_OBSERVED` en
`SessionQualityGate.kt:55-62`, la corrida no decía NADA sobre el sensor: ni lo confirmaba ni lo refutaba.
(4) Dos errores de arnés: `awk '$1>1789392800'` sobre epoch en NANOSEGUNDOS de 19 dígitos imprimió todo
(comparación numérica degradada en silencio), y un one-liner que iteraba `gates` como array cuando
`validate-portfolio.mjs --json` lo emite como objeto por nombre de gate.

**Causa raíz:**
(1) Un guard cuya precondición es «la autoridad está vacía» codifica el estado del árbol del día en que se
escribió, no una propiedad del validador: es verde por ausencia de datos y rojo por presencia de datos
buenos, exactamente al revés de lo que un mutante debe hacer. (2) Un runbook escrito ANTES de ejecutarse
hereda premisas no verificadas («se puede entrenar», «publicar es activar»), y el SDK remoto tiene un
intervalo mínimo de fetch que convierte un flip de consola en un cambio DIFERIDO. (3) Una jerarquía de
precedencia de veredictos tiene una consecuencia para la observación: sólo el veredicto de mayor
precedencia que se dispare es el que se ve, así que cualquier defecto del ARNÉS que dispare un veredicto
más alto que el observado enmascara el resultado y la corrida es INCONCLUSA, no negativa. (4) awk trata
un literal de 19 dígitos por encima de su precisión de doble como aproximado y `gates` no es una lista.

**Solución aplicada:**
(1) P3-11 reescrito como DISCRIMINANTE sobre la copia sandbox: mide primero que las corridas reales por sí
solas NO disparan G10 (`pre = !hasGate(...)`), y sólo entonces inyecta una corrida fabricada sin hash y
exige el ROJO; verificado por mutación (clausula VERIFIED de G10 desactivada -> exactamente P3-11 FAIL,
los otros 17 PASS, fichero restaurado byte-idéntico). (2) Línea base ejecutada con el flag ON antes de las
corridas (el bloque de sonda pasó la admisión y dibujó la forma 1); runbook corregido en §1.4 (12 h y la
vía `run-as` en debug) y §1.5 (línea base como precondición); activación verificada en
`files/frc_<app-id>_firebase_activate.json`, no supuesta. (3) Corrida 1 repetida con un guion de 5 filas
a ~0,4 s (delays 139-298 ms): `Admitted` 16/16; corrida 2 `INTERRUPTED` por `KEYCODE_HOME` con el proceso
vivo (veredicto de máxima precedencia, observable con cualquier guion). Las tres frases `honestNote`
transcritas verbatim desde `uiautomator`; el intento A queda registrado como primera instancia de
`MB-FOCUS-0146`, no como fallo del sensor. (4) `substr($1,1,10)+0` en segundos y
`Object.entries(j.gates)`. Toda la evidencia es de EMULADOR con drivers guionizados: evidencia parcial,
ningún GO; `MB-FOCUS-0101` sigue OPEN y `0165` MITIGATED. Kotlin tocado: 0.

**Regla futura:**
Un mutante NUNCA condiciona su verde a que la autoridad de evidencia esté vacía: su precondición se mide
sobre el validador («los datos reales solos no disparan el gate»), y su mitad discriminante inyecta el
defecto sobre esa base — así sigue verde el día que llegue la primera evidencia real, que es justo el día
que importa. Antes de ejecutar un runbook, ejecuta FASE-0 sobre sus PREMISAS de entrada («¿se puede llegar
a la pantalla?», «¿el flag ya está activo en el proceso?»), y para Remote Config trata publicar y activar
como dos hechos distintos: el SDK no refresca antes de 12 h, así que verifica la activación en el fichero
del dispositivo y fuerza el fetch en debug con `run-as` en vez de borrar datos. Cuando un sistema tenga
una JERARQUÍA de veredictos, elige el arnés de modo que ningún veredicto de precedencia MAYOR que el que
quieres observar pueda dispararse por culpa del propio arnés: un guion a ~1 s por respuesta contra una
ventana de 950 ms produce `RESPONSE_PATTERN` (precede al de ciclo de vida) y la corrida es INCONCLUSA para
el sensor aunque parezca un resultado; el veredicto de MÁXIMA precedencia (`INTERRUPTED`) es el único
observable con cualquier guion. Distingue siempre «el sensor falló» de «otro veredicto lo tapó». Y una
corrida en emulador con guiones es evidencia PARCIAL por construcción: registra las entradas con
`provenance` que lo diga, no promuevas ninguna fila cuyo gate exija dispositivo y humano. Corolarios de
entorno: awk no compara enteros de 19 dígitos (recorta a segundos con `substr` antes de comparar); el
JSON de `validate-portfolio.mjs` expone `gates` como OBJETO por nombre, no como array; el intervalo mínimo
de fetch se mide en `frc_<app-id>_firebase_settings.xml` y la activación en `frc_<app-id>_firebase_activate.json`.

**Archivos afectados:**
- `scripts/portfolio/evidence-mutation-tests.mjs` (P3-11 reescrito como discriminante sobre el sandbox)
- `docs/audits/ENFOQUE_CORRIDA_DISPOSITIVO_2026-09-14.md` (§1.4 12 h y `run-as`, §1.5 línea base, §6, §7 dossier de emulador, §7.2)
- `PORTFOLIO_RUNTIME_EVIDENCE.json` (2 corridas con `provenance` EMULADOR, untracked) · `MINDBOOST_AUDIT_LEDGER.md` (`MB-FOCUS-0165`/`0101`/`0116`/`0091`/`0135`/`0146`, `MB-PORTFOLIO-0024`/`0025`/`0007` con metadatos; ningún cambio de estado) · **Kotlin tocado: 0 ficheros**

### 2026-09-13 — Un P0 puede llevar quince días CERRADO bajo otro nombre mientras el informe sigue diciendo «los dos P0 siguen abiertos»: una fase que cierra un invariante sin leer su fila dueña fabrica un ID nuevo y deja el viejo huérfano; retener por defaults anidados es CONTENCIÓN, no gate; y un sensor «cableado» puede estar apagado por flag en TODAS las plataformas

**Error detectado:**
§14 de la auditoría adversarial de ENFOQUE, diez preguntas de re-auditoría ejecutadas contra el árbol de HOY y
refutadas por 15 agentes de solo lectura (12 CONFIRMED, 3 PARTIALLY_REFUTED). Ningún Kotlin tocado: todos los
hallazgos son de REGISTRO, de PREMISA o de alcance. (1) **Q1**: el segundo P0, `MB-FOCUS-0090` («periodo del banco y
umbral de fiabilidad EN FASE»), tenía las tres partes de su gate satisfechas con evidencia y test desde R13
(`ProbeBank` reserva 4 frente a `MIN_ADMINISTRATIONS = 3` con guard derivado; `releaseProbe` bajo `NonCancellable`;
`ProbeBankTest.theFirstGatedRetestNeverLandsOnTheBaselineForm` administra MIN+1 y exige forma distinta de la de
baseline) — y la fila seguía OPEN sin un solo evento desde su alta del 2026-08-29. R9 había registrado
`MB-FOCUS-0122` NEW con el MISMO enunciado y sin referencia cruzada, R13 lo cerró, y el informe llevaba quince días
diciendo «los dos P0 siguen abiertos» (línea 1008). La respuesta honesta a Q1 era NO por un motivo de registro, no de
código. (2) **Q2**: creí que dos capas de defaults retenían el `strongEvidence`; son TRES (`FocusGraph.kt:156` →
`FocusEvaluationService.kt:17` → `ProbeReliabilityEstimator.kt:35`) más una cuarta latente, y **ningún test pinnea
que producción cablee `EMPTY`**: `FocusEvaluationServiceTest` fija el default del CONSTRUCTOR y
`ProductionFocusGraphWiringTest` sólo escanea `focusEngine =`/`seedSource =`. (3) **Q5**: respondí «el sensor de
ciclo de vida está cableado» y el refutador corrigió: con `focusLifecycleProbeEnabled = false` se instala
`DisabledAppLifecycleProbe` en TODAS las plataformas, no sólo en desktop/iOS. (4) **Q8**: la premisa del diagnóstico
que §13 pedía («el dato ya está persistido») es FALSA: no hay identidad de forma por fila en `SessionResult`, DTOs,
mappers ni nube; sólo el contador del cursor, y `recordProbe` indexa por `endedAtMs` sin `probeId`. (5) **Q10**: la
autoridad de evidencia en ejecución que el ledger designa (`PORTFOLIO_RUNTIME_EVIDENCE.json`, gate de
`MB-PORTFOLIO-0024`) no está trackeada, no está en el working tree y no la ignora `.gitignore`, aunque un checkpoint
la declaró «new». (6) La regla del 2026-09-07 cita «los 66 eventos CLOSED»; hoy son 74, con actor no normalizado (61
`primary session`, 13 `claude-code`). Y dos casi-duplicados míos: iba a abrir un ID para «defaults anidados del RCI»
y otro para «el flag de sync no puede cablear egreso», y AMBOS invariantes ya tenían dueño (`0102`, `0062`).

**Causa raíz:**
Una fase que cierra un invariante trabaja sobre el enunciado que tiene delante, no sobre el registro: si el
enunciado llega reescrito (R9 lo redactó de nuevo en vez de citar `0090`), la fase abre un ID nuevo, lo cierra con
evidencia impecable, y la fila original queda huérfana con el estado del día en que nació. Nada lo detecta porque el
validador comprueba consistencia registro/evento por ID, no que dos IDs enuncien lo mismo, y porque el informe se
actualiza por fase, no por invariante. La segunda causa es de lectura: «retenido» se lee como «protegido», y un
literal fuerte retenido por una cadena de defaults está a UNA línea de embarcarse sin que ningún gate lo vea. La
tercera es de premisa: una lista de diagnósticos pendientes hereda las suposiciones de quien la escribió («el dato
está persistido», «el sensor está cableado»), y ejecutarla sin FASE-0 ejecuta la suposición. La cuarta es de sink:
un checkpoint puede declarar «new» un fichero que nunca llegó al árbol, y a partir de ahí todo gate que apunte a él
es insatisfacible en silencio.

**Solución aplicada:**
Cero Kotlin. `0090` OPEN → CLOSED citando `0117` + `0122` + reruns frescos (14 clases dueñas 92/0F/0E; suite
4468/0F/0E sobre 624 XML frescos, delta 0 frente a §13) y su techo (`0135` sigue OPEN); `0122` gana la dependencia a
`0090`; el defecto de proceso queda como `MB-FOCUS-0163` (P3, CLOSED, precedente `0113`). Los dos casi-duplicados se
convirtieron en ACTUALIZACIONES de la fila dueña (`0102` con las tres capas y la ausencia de pin; `0062` con «inerte
por construcción, no sólo por flag»), no en IDs nuevos. `0121` absorbe la premisa refutada de Q8 con su consumidor
nombrado (periodo = `reservedForms.size`, nunca un literal; `recordProbe` debe llevar `probeId`). Dos hallazgos nuevos
con dueño: `0164` (`impulsivityDecision` precede a `performanceDecision` sin leer las tres señales; KDoc de
`AdaptiveModels` sobreafirma; `AdaptiveAbsenceHoldTest` no pinnea el paso previo) y `0165` (el sink de evidencia
designado no existe; misma corrida que cierra `0101`). `0089`, `0155` con metadatos corregidos. §28 escrito en el
informe con el veredicto PROVISIONAL (Q10 NO) y sin recomputar §7.

**Regla futura:**
Antes de registrar CUALQUIER hallazgo, **busca su fila dueña por invariante, no por enunciado**: grep del símbolo o
de la constante en el ledger (`MIN_ADMINISTRATIONS`, `reservedForms`, el nombre de la clase) y lee las filas que
salgan enteras — si una ya posee el invariante, el trabajo es ACTUALIZAR esa fila y añadirle la dependencia, jamás
un ID nuevo; y cuando cierres un invariante, **cierra TODAS las filas que lo enuncian** en la misma escritura. Al
terminar una fase, relee el informe buscando frases de estado global («los dos P0 siguen abiertos», «ninguna sonda
está cableada») y comprueba cada una contra el registro de HOY: una frase así envejece por fase y nadie la vuelve a
leer. Distingue **retenido por defaults** de **retenido por gate**: si una afirmación fuerte está muerta sólo
porque nadie pobló una costura con default, el dueño mecánico es un pin en el test de wiring de producción que exija
que esa costura siga vacía (o pinnee lo que se cablee) — sin él, poblarla es un cambio silencioso. Cuando digas «el
sensor X está cableado», di también **con qué flag y en qué plataformas está ENCENDIDO**: un `actual` instalado
detrás de un flag OFF por defecto es un sensor apagado en producción, y la respuesta honesta lleva ese techo. Toda
lista de diagnósticos pendientes se ejecuta con FASE-0 por ítem **sobre su PREMISA** («¿existe el dato?», «¿existe
el sink?»), no sólo sobre su símbolo: aquí dos premisas eran falsas y ejecutarlas habría producido un diagnóstico
ciego y una evidencia sin sitio donde aterrizar. Y cuando una regla de este fichero cite un recuento («66 eventos
CLOSED»), léelo como del día en que se escribió: la cifra se REMIDE antes de citarla. Corolarios de entorno, tres
conocidos que volvieron a pagar: (1) el `Last update` de cada fila se LEE antes de pisarlo y el script aborta si no
coincide — aquí fue LEERLA lo que destapó que `0089` llevaba una fecha ANTERIOR a su última transición; (2) el splice de un bloque nuevo
antes del pie del informe REPONE el salto de línea que el ancla consume; (3) `writeLedgerUnderLock` devuelve
`{ok, reason}` y se comprueba, y la identidad byte a byte del fichero con el candidato se verifica DESPUÉS de
escribir, no se asume.

**Archivos afectados:**
- `docs/audits/AUDITORIA_ADVERSARIAL_ENFOQUE_2026-08-29.md` (§28, nota de vigencia del checklist de re-auditoría; veredicto PROVISIONAL)
- `MINDBOOST_AUDIT_LEDGER.md` (`MB-FOCUS-0090` CLOSED por reconciliación; `0163` nuevo CLOSED; `0164`/`0165` nuevos OPEN; `0102`/`0121`/`0122`/`0089`/`0062`/`0155` actualizados) · **Kotlin tocado: 0 ficheros**

### 2026-09-07 — Un guard que vigila el CALL-SITE es ciego a la DECLARACIÓN, y la mutación lo prueba dejándolo VERDE; una prescripción cuyo remedio está medido como DAÑINO no se salta, se sustituye por el dueño mecánico de la ausencia con polaridad invertida; y una lista de tests propuestos no es una lista de trabajo

**Error detectado:**
§13 de la auditoría adversarial de ENFOQUE, doce tests nacidos `PROPUESTA` (T1–T12). Ejecutados en orden con
FASE-0 por ítem, **nueve de los doce ya tenían dueño vivo o son insatisfacibles o dañinos tal como están
escritos**, y sólo tres eran hueco real. Los tres huecos son de familias distintas y ninguno lo veía la suite.
(1) **T8**: `AdaptiveContractAbsenceGuardTest` ya vigilaba con cuatro expresiones regulares el CALL-SITE del
contrato adaptativo (centinela de fatiga, histéresis saturada, mapa de maestría vacío, señal construida en
línea), y **ninguna mira la DECLARACIÓN**: un `= null` en el parámetro deja los cuatro VERDES y devuelve
exactamente el vector que `MB-FOCUS-0071` nombra —un call-site futuro que SÍ produce la señal puede omitirla en
silencio, y ningún escaneo textual por fichero distingue «no se pasó» de «no se midió»—. El KDoc del `data
class` ya AFIRMABA la propiedad y nada la sostenía. (2) **T10**: su mitad de renderizadores («los 13 exponen nodo
semántico») está medida como **dañina** (`MB-FOCUS-0110`: en una tarea de discriminación visual la descripción o
REVELA la respuesta o se anuncia dentro de la ventana de la que se deriva el tiempo de reacción), pero la
ausencia **no tenía dueño mecánico**, así que la decisión de producto podía tomarse en silencio. (3) **T12**: el
guard que afirma que abortar devuelve la forma reservada se cumplía **sin haber quemado nada** — «la forma
volvió» y «la forma nunca se tomó» eran indistinguibles para sus dos aserciones. Y un cuarto, sobre mi propio
trabajo: la primera versión del guard de T10 casaba contra el fichero ENTERO, así que un KDoc que mencionara la
etiqueta habría contado como violación.

**Causa raíz:**
Un guard de texto tiene **dos superficies distintas** que se confunden porque protegen el mismo invariante: el
sitio donde se USA un símbolo y el sitio donde se DECLARA. Vigilar la primera es lo natural (ahí ocurrió el
defecto original) y deja la segunda sin dueño, y la segunda es justo por donde vuelve el defecto **aflojando el
tipo** en vez de reintroduciendo el valor. La segunda causa es de gobernanza: una lista de tests propuestos se
lee como una lista de trabajo, y en un repo que trata «dos guards del mismo invariante son dos dueños» como
defecto, implementarla al pie es crear nueve segundos dueños. La tercera es que **una prescripción refutada deja
un hueco distinto del que enunciaba**: si el remedio que pide es dañino, lo que falta no es el remedio, es que
nadie se entere el día que alguien lo aplique.

**Solución aplicada:**
(1) `theThreeSessionSignalsCarryNoDefault` lee la lista de parámetros de `AdaptiveInput` con los comentarios
retirados y exige que ninguna de las tres señales lleve `=`, con su anti-vacuidad (si el `data class` deja de
declarar una, el test dice que hay que mover el guard), su **mitad discriminante** (el default de `config` es
legítimo, así que el guard no puede prohibir todo `=`) y su mitad de no-leer-de-más (un parámetro ausente
devuelve `null`). **M1** (`fatigue` a `= null`) deja ROJO 1 de 4 sólo el test nuevo y los **tres guards
preexistentes VERDES**, que es la prueba de que eran ciegos y de que el nuevo no duplica a ninguno. (2) La mitad
dañina de T10 **NO se implementa**: se le da a la ausencia el dueño que le faltaba con
`FocusStimulusSemanticsTest`, que pinnea la LIMITACIÓN con **polaridad invertida** —un ROJO aquí no es una
regresión, significa que la decisión se tomó, y la aserción se invierte con esa decisión como evidencia, nunca se
borra— más su mitad discriminante (el único control del paquete conserva su descripción y su rol). (3) T12 no
gana test (la primitiva ya tiene cuatro dueños): se endurece **en sitio** con una aserción de anti-vacuidad
derivada del trazado del holder (la quema precede a `administerProbe` y `emitRunning` es la única fuente de
`Running`, así que afirmar `Running` prueba que la reserva ocurrió). **M4** deja ROJO 1 de 8 sólo la aserción
nueva, con las **dos preexistentes VERDES**. Los nueve ítems restantes se cierran **con su dueño citado por
nombre de test** en §27 del informe, no como trabajo pendiente, y de paso se corrige el recuento de
`MB-FOCUS-0103`: los `copy(` de producción que omiten `cycleWeek` son **CUATRO**, no tres.

**Regla futura:**
Cuando un invariante ya tenga un guard, pregunta **qué superficie vigila**: si mira el CALL-SITE, la
DECLARACIÓN sigue sin dueño, y por ahí el defecto vuelve aflojando el tipo (`= null`, un parámetro que se hace
opcional, una anotación que se retira) sin mover un solo carácter en los sitios que el guard lee. La prueba de
que hacía falta un guard nuevo, y no un patrón más, es **mutar y comprobar que los viejos quedan VERDES**: si
alguno cae, estabas duplicando. Antes de implementar un ítem de una lista de tests propuestos, **búscale el
dueño vivo**: un test que es la negación lógica de otro verde no es cobertura, y en un repo donde dos dueños del
mismo invariante son un defecto, la lista se ejecuta ítem a ítem con FASE-0, no de arriba abajo — y los ítems
que ya tienen dueño se cierran **citándolo por nombre de test**, que es una afirmación más pequeña y verdadera
que marcarlos implementados. Cuando refutes la prescripción de un ítem porque su remedio está **medido como
dañino**, no pases al siguiente: pregunta **qué queda sin dueño al no implementarlo**. Casi siempre es la
ausencia misma, y el guard correcto la pinnea con **polaridad invertida**, declarando en su KDoc que un ROJO
significa que la decisión se tomó y que la aserción se invierte con esa decisión como evidencia — nunca se borra.
Y todo guard de texto necesita sus **tres** mitades, no una: anti-vacuidad (¿leyó algo?), discriminante (¿el caso
legítimo sigue pasando?) y no-leer-de-más (¿un comentario que DESCRIBE lo prohibido cuenta como violación?) — la
tercera es la que falta siempre y produce hallazgos falsos que parecen rigor. Corolarios de entorno, uno NUEVO y
tres conocidos: (1) **un validador que resuelve su ruta desde `dirname(import.meta.url)` ignora el `cwd`**, así
que «validar en una copia del scratchpad» exige replicar la estructura (`sandbox/scripts/` + `sandbox/<fichero>`)
o estarás validando el fichero del repo y leyendo su verde como si fuera el de tu candidato — el mío dijo `OK,
317 findings` sobre el original mientras el candidato tenía 321; (2) una huella `git status` **idéntica** antes y
después no significa que tu trabajo no esté: un fichero nuevo dentro de un directorio ya listado como `??` no
añade línea, así que la huella se ATRIBUYE comprobando que tus ficheros existen, nunca se lee como ausencia; (3)
el `Last update` de una fila del ledger se **lee del registro** antes de pisarlo, y el compare-and-swap se ancla
en la línea `Canonical SHA-256`; (4) `writeLedgerUnderLock` devuelve `{ok, reason}` y su retorno se comprueba —
ignorarlo reporta éxito sobre una escritura rechazada.

**Archivos afectados:**
- `shared/src/desktopTest/.../focus/domain/adaptive/AdaptiveContractAbsenceGuardTest.kt` (+1 guard de la declaración, +4 aserciones discriminantes, 2 helpers)
- `shared/src/desktopTest/.../focus/presentation/ui/FocusStimulusSemanticsTest.kt` (nuevo, 3 tests, limitación pinneada con polaridad invertida)
- `shared/src/commonTest/.../focus/presentation/FocusEvaluationStateHolderTest.kt` (aserción de anti-vacuidad en sitio)
- `docs/audits/AUDITORIA_ADVERSARIAL_ENFOQUE_2026-08-29.md` (§27) · `MINDBOOST_AUDIT_LEDGER.md` (`MB-FOCUS-0159`..`0162` nuevos y CLOSED; `0103` con su recuento corregido)


### 2026-09-07 — Un gate de texto solo cubre los directorios de su lista, y un test que EXIGE la copy prohibida la vuelve inarreglable; un control que el operador puede satisfacer EDITANDO no es un control; y un default de 0,0 en un término de corrección AFIRMA que repetir no cambia nada

**Error detectado:**
Bucket 90 d de la auditoría adversarial de ENFOQUE (§12 R16–R20). **Ninguna de las cinco prescripciones
sobrevivió a su medición**, y el defecto vivo lo destapó la que parecía más inocua. (1) **R19**:
`BrainProfileUiModelBuilder.DISCLAIMER` —renderizado en el perfil de la app **sin entrar siquiera en
MindBoost**— y `MindBoostHubScreen.OnboardingNote` embarcaban lexemas prohibidos negándolos por su nombre,
con el build VERDE dos años. Causa medida: ni `profile` ni `presentation` estaban en
`mindboostCopyFeatureModules`, así que el escáner no los miraba; y el test que pinneaba el disclaimer
**EXIGÍA** el lexema (`assertContains(disclaimer, "…")`), de modo que arreglar la copy obligaba a cambiar el
test — la clase de defecto de 2026-08-27, reincidente. El hub afirmaba además que cada ejercicio «mide», la
afirmación que el catálogo de Enfoque acababa de retirar. (2) **R20** pedía «quien implementa no cierra», y es
**inejecutable como regla de personas**: GOV-001 registra que el CEO es el único humano, los 66 eventos CLOSED
llevan la sesión implementadora como actor, y `validate-audit-ledger.mjs` **nunca lee esa columna** — un check
sobre ella sería teatro. De paso se midió que **ninguno de los dos validadores de gobernanza era invocable**
desde `package.json`, `.github/**` ni Gradle. (3) **R16** colgaba de un KDoc rancio de
`TransferReportBuilder.label` («nueve métricas cruzan el piso a la longitud de la sonda»): cierto cuando se
escribió, falso desde que cada métrica sella SU celda; hoy cruza **una**. (4) **R17**: el párrafo de límite
declarado de `EvaluationResult` daba **dos razones y las dos eran falsas** — el hecho de la muestra sí se
produce (`DefaultMetricsEngine` lo sella y `DefaultCalibrationEngine` lo **DESCARTA** al mapear a `Double`),
y no exige migración (`FocusJson` lleva `ignoreUnknownKeys`; un bump sería **dañino**, porque
`FocusSchema.CURRENT` es COMPARTIDO y `readVersioned` cuarentena —borra— toda clave más nueva).

**Causa raíz:**
Un gate de texto tiene una tercera parte además del mecanismo y del vocabulario: **el ALCANCE**, y el alcance es
una lista escrita a mano que nadie revisa cuando nace un directorio. Peor, su comentario de cabecera enumeraba
la lista en prosa y llevaba meses rancio («los 7 módulos», «EXCLUYE emotions»), así que el lector creía saber el
alcance sin mirarlo. La segunda causa es de **satisfacibilidad**: una regla que el sujeto auditado puede
cumplir editando el documento auditado no restringe nada; y una regla sobre una columna que el validador no lee
no restringe nada tampoco. La tercera es `null != 0` en su forma **AFIRMATIVA**: añadir a una fórmula un término
de corrección con default `0,0` no deja el hueco abierto, **AFIRMA** que la magnitud vale cero —aquí, que
repetir la tarea no mueve el resultado—, que es fabricar la respuesta halagadora desde una ausencia.

**Solución aplicada:**
(1) Las dos cadenas se reescriben **acotando positivamente** (precedente `CreativityStrings`: decir qué ES, no
negar qué no es), el test pasa a afirmar la PROPIEDAD vía `MindBoostCopySafetyScanner.scanAll(...)` sin nombrar
ningún lexema, y `presentation` entra en el gate **tras medirlo** (2 ficheros, 1 línea ofensora → 0). Lo que
sigue FUERA se registra **con su recuento y su razón**, no se olvida: `profile` (3 líneas; su etiqueta de
dimensión canónica ES un literal prohibido — colisión que es decisión de producto, `MB-FOCUS-0153`) y `coach`
(33 líneas; su política de seguridad debe NOMBRAR lo que bloquea, `MB-FOCUS-0154`). Dos mutaciones: M1
reintroduce el lexema en el hub y el gate ampliado se pone ROJO nombrando fichero y término (antes de R19 la
misma mutación quedaba VERDE); M2 devuelve el disclaimer viejo y cae exactamente el test nuevo. (2) R20 se
ejecuta en una forma **más estrecha pero real**: una regla que compara el campo `EXECUTION_AUTHORIZATION`
—propiedad exclusiva del CEO— entre las autoridades VIVAS, que **FALLA en el árbol de hoy** con un único error
y sin colateral; el fallo **es** el objetivo y **no se resuelve editando ninguno de los dos documentos**. Los
validadores se hacen invocables (`check:governance`) y **deliberadamente NO se cablean a CI**. (3) y (4) se
ejecutan como corrección **documental con cero cambio de comportamiento**, y las dos correcciones que el RCI no
aplica se DECLARAN como techo con dueño en vez de implementarse.

**Regla futura:**
Antes de dar por cubierto un gate de texto, **lee su lista de alcance, no su comentario**: la lista es la única
fuente de verdad y un comentario que la enumera en prosa se vuelve mentira con el primer directorio nuevo (el
arreglo es que el comentario APUNTE a la lista y no la copie). Al ampliar el alcance, **mide primero** los
directorios candidatos y registra con su recuento y su razón los que quedan fuera: «no lo metí» y «no se puede
meter porque su etiqueta canónica colisiona» son hechos distintos y solo el segundo tiene dueño. Trata **un test
que EXIGE la presencia de la copy prohibida como parte del defecto**: pinnea la violación en vez de la garantía
y vuelve la copy inarreglable — sustitúyelo por uno que afirme la propiedad consultando el escáner canónico, sin
nombrar nada. Y prefiere copy que **acota positivamente** a copy que niega: negar satisface al usuario y viola el
invariante, acotar satisface a los dos. Cuando una prescripción de gobernanza diga «X no puede hacer Y», pregunta
**si el sujeto puede satisfacerla editando lo que se le audita**: si puede, no es un control; la forma ejecutable
es un invariante sobre algo que el sujeto no posee, y su fallo se ESCALA en vez de silenciarse — nunca lo
cablees a un gate de build, porque convertirías una pregunta que no es del operador en presión para editar un
campo ajeno. Y comprueba que el validador que propones **lea de verdad la columna** sobre la que quieres reglar:
una regla sobre un campo que nadie lee es teatro con el build en verde. Antes de añadir a una fórmula un término
de corrección, pregunta **qué AFIRMA su default**: `0,0` en un término de cambio esperado no es «pendiente», es
«repetir no cambia nada», la respuesta halagadora fabricada — si el dato normativo no existe, **declara el techo
con dueño** y no añadas el campo. Verifica también **las razones que un comentario da para no hacer algo**, no
solo su enunciado: aquí las dos eran falsas y la segunda proponía un cambio (bump de un schema COMPARTIDO) que
habría borrado la línea base, el perfil y el historial. Corolarios de entorno, uno NUEVO y tres conocidos: (1)
**un guard de edición que inspecciona el FICHERO ENTERO aborta por violaciones ajenas y preexistentes** — el mío
paró sobre seis líneas de más de 120 en `build.gradle.kts`, que ni es escaneado por detekt ni las había escrito
yo, y como abortaba antes de escribir dejó a medias el lote; el guard debe inspeccionar **solo el texto que
inserta**; (2) **no asumas la firma de una API leyendo un KDoc truncado**: escribí `scanner.scan(texto)` y la
forma de un argumento se llama `scanAll` (`scan` exige la lista de términos), lo cazó el compilador; (3) **el
baseline de un conteo de tests se LEE del checkpoint anterior, no se recuerda** — creí un delta de +18 sobre 4446
y la fila de R15 ya decía 4464, así que el delta neto real era 0; (4) al sellar el ledger, el compare-and-swap se
ancla en la línea `Canonical SHA-256`, jamás en «el primer hash de 64 hex del fichero».

**Archivos afectados:**
- `shared/src/commonMain/.../mindboost/profile/presentation/BrainProfileUiModelBuilder.kt` · `.../mindboost/presentation/ui/MindBoostHubScreen.kt` (copy embarcada corregida)
- `shared/build.gradle.kts` (`presentation` en el alcance; comentario que apunta a la lista en vez de copiarla) · `shared/src/desktopTest/.../profile/presentation/BrainProfileUiModelBuilderTest.kt` (guard reescrito)
- `shared/src/commonMain/.../focus/domain/reporting/TransferReport.kt` · `.../focus/domain/evaluation/{ReliableChangeIndex,EvaluationResult}.kt` · `.../mindboost/storage/MindBoostStorageNamespaces.kt` (KDoc corregidos, techos declarados)
- `scripts/validate-authority-entrypoints.mjs` (+ su suite) · `package.json` (`check:governance`)
- `docs/audits/AUDITORIA_ADVERSARIAL_ENFOQUE_2026-08-29.md` (§26) · `MINDBOOST_AUDIT_LEDGER.md` (`MB-FOCUS-0148`..`0158`; `0099` con su autoridad corregida)

### 2026-09-07 — Una prescripción que pide RETIRAR un símbolo exige la misma medición de call-sites que una que pide reutilizarlo: retirar lo que nadie lee no es un remedio; un exploit descrito antes de un arreglo previo cambia de MECANISMO en vez de desaparecer; y un pin sobre un productor que sortea por ensayo tiene COLA, así que una sola semilla es intermitente por diseño

**Error detectado:**
Fase R14 del bucket 60d. Su prescripción tenía dos mitades —«corregir E9/E10/E8 **o** retirar su métrica
gobernante»— y cuatro cosas resultaron distintas de lo escrito. (1) La mitad B es **un no-op**: en Enfoque
`ExerciseDefinition.governingMetric` se declara en los trece ejercicios y **no tiene un solo lector de
producción**; `DefaultAdaptiveEngine.performanceDecision` nombra sus tres claves directamente (`ACCURACY`,
`RT_CV`, `FALSE_ALARMS`) y jamás consulta el campo. Retirarlo no cambia ningún comportamiento, luego no es
remedio de nada — y su KDoc afirmaba justo lo contrario («la métrica que dirige la decisión adaptativa
primaria»), que es de dónde salió la prescripción. (2) El enunciado de H-06 decía que en E9 «retener siempre
acierta el 66,7 %-68,8 %», y medido contra el productor real **retener en TODO ya no compra nada**: desde
`MB-FOCUS-0091`/`FOC-P0-04`, cero respuestas produce una ausencia tipada (`NotMeasurable(NO_RESPONSES)` y
`unavailable(ACCURACY, …, NO_RESPONSES)`). El exploit alcanzable es **una sola pulsación por bloque**, y la
otra mitad del enunciado es PEOR de lo escrito: el único eje que el bucle puede mover (`perceptualLoad`,
su `governingAxis`) paga MÁS al no-respondedor, y en carga máxima la mediana cruza `accUpThreshold` — el
bucle le SUBE la dificultad. (3) Los tres tests de aceptación que el propio plan pide (T5/T6/T7) son
**insatisfacibles hoy**, porque describen la propiedad que traería un rediseño que la fase no ejecuta. (4)
En mi primer diseño del guard de E9 iba a fijar el resultado de UNA semilla: el sondeo es un sorteo por
ensayo, así que a 55 ensayos la desviación típica del explotador es ≈ 0,064 frente a un borde de banda a
0,035 — un pin intermitente por diseño, cazado calculando antes de compilar.

**Causa raíz:**
«Retirar X» se lee como trabajo y «reutilizar X» se lee como atajo, así que sólo al segundo se le exige la
pregunta que separa un símbolo presente de una capacidad viva: **quién lo llama en producción**. Un campo
declarado en trece sitios con un KDoc convincente parece portante, y su retirada parece un remedio, cuando el
efecto real es cero. (2) es la forma temporal del mismo defecto: un hallazgo describe el exploit del árbol en
que se escribió, y un arreglo posterior en OTRA capa puede cerrar la puerta barata sin cerrar la cara — el
exploit no desaparece, se MUEVE, y quien cite la aritmética vieja mide un sistema que ya no existe. (4) es
`null != 0` traducido a estadística: comparar un estimador puntual contra un borde sin mirar su dispersión.

**Solución aplicada:**
La mitad B se **REFUTA con medición** y NO se ejecuta (precedente R11/R12); lo único que se corrige es el
documento que la generó: el KDoc de `governingMetric` declara ahora que el campo lo lee NADIE en Enfoque, que
el motor nombra sus claves directamente y que retirarlo no cambia comportamiento. La mitad A (retirar la
afirmación de la copy) sí era cierta y se ejecuta: tres descripciones embarcadas afirmaban una medición que
ningún mecanismo produce, reescritas a lo que el ejercicio hace, con las otras diez **byte-idénticas**. Los
tres enunciados se mecanizan invirtiendo la polaridad de T5/T6/T7 y **pinneando la LIMITACIÓN** con su guard
(precedente 2026-08-19): el guard afirma que la estrategia ciega SÍ compra la banda, que pulsar en todo SÍ es
1,0 y que ignorar el contador SÍ da el óptimo, de modo que el día del rediseño se pone ROJO y la aserción se
invierte con evidencia en vez de borrarse. E9 se pinnea por **DISTRIBUCIÓN** (mediana sobre 7 semillas más la
proporción de semillas dentro de banda), ninguna cifra escrita a mano —cada número se le pide al productor y
se empuja por `DefaultMetricsEngine`— y con su mitad discriminante (el silencio debe seguir siendo ausencia
tipada). Cuatro mutaciones con atribución 1-a-1 y restauración verificada por SHA-256.

**Regla futura:**
Aplica a «retirar X» **la misma medición de call-sites que a «reutiliza X»**: un `grep` del símbolo excluyendo
sus tests responde si la retirada es un remedio o un no-op, y un no-op registrado como remedio deja el defecto
intacto con la casilla marcada. Si el campo tiene un KDoc que afirma dirigir algo, **verifica el consumidor
antes de creerle**: el KDoc suele ser la fuente de la prescripción. Cuando verifiques un hallazgo escrito
ANTES de otros arreglos, **re-mide su exploit contra los gates de HOY**: un gate cerrado normalmente no elimina
el exploit, lo DESPLAZA a la variante más cara (aquí de «retener siempre» a «una pulsación»), y la aritmética
del informe queda describiendo un sistema que ya no existe — el enunciado puede seguir siendo cierto y su
mecanismo ser otro. Cuando el test de aceptación que un plan pide sea **insatisfacible** porque describe la
propiedad de un rediseño que no vas a ejecutar, no lo saltes: **invierte su polaridad y pinnea la limitación**,
con la nota de que debe invertirse con evidencia el día que se levante. Antes de pinnear un valor contra un
umbral, pregunta **si el productor sortea por ensayo**: si lo hace, el estadístico tiene cola, así que calcula
su dispersión frente al margen ANTES de escribir la aserción y pinnea por distribución (mediana + proporción
de semillas), nunca por una semilla. Corolarios de entorno, uno NUEVO y tres conocidos: (1) **el estado previo
de una fila de evento del ledger se LEE del registro, no se recuerda** — escribí `CLOSED → CLOSED` para
`MB-FOCUS-0091` y el registro decía `MITIGATED`, así que el validador rechazó la escritura con
`register/event contradiction`; la corrección va sobre la primera causa y se revalida, y `MITIGATED` no se
promueve a `CLOSED` porque una cobertura de regresión no es el gate del invariante completo; (2) un extractor
de léxico que lee de MÁS informa falsos positivos que parecen rigor (el mío cazó vocabulario PERMITIDO listado
en un KDoc y la lista `ENGLISH_CLAIMS`, que está fuera de `ALL` a propósito): parsea sólo las declaraciones que
componen el conjunto vivo y ponle auto-comprobación en AMBAS direcciones; (3) los tests de `desktopTest` no los
escanea detekt pero SÍ el escáner de copy de ese source-set; (4) `$SCRATCH` no está definido en la herramienta
Bash: usa la ruta completa del scratchpad o expórtala en la misma llamada.

**Archivos afectados:**
- `shared/src/desktopTest/.../focus/domain/exercise/FocusBlindStrategyTest.kt` (nuevo, 5 guards, T5/T6/T7 invertidos)
- `shared/src/commonMain/.../focus/domain/model/ExerciseDefinition.kt` (KDoc de `governingMetric` corregido) · `.../focus/domain/catalog/ExerciseCatalog.kt` (3 descripciones reescritas)
- `docs/audits/AUDITORIA_ADVERSARIAL_ENFOQUE_2026-08-29.md` (§24) · `MINDBOOST_AUDIT_LEDGER.md` (`MB-FOCUS-0139`/`0143` CLOSED; `0140` MITIGATED; `0137`/`0138`/`0141`/`0142` nuevos OPEN)

### 2026-09-06 — Un `read` que CUARENTENA borra el dueño único de un hecho irreproducible, y colapsar su fallo en 0 fabrica la respuesta favorable sobre la propia línea base; dos constantes de ficheros distintos exigen un guard DERIVADO de las dos, no una tabla escrita a mano; y CERO refutaciones no es refutado, es NO JUZGADO

**Error detectado:**
Fase R13 del bucket 60d, sobre `MB-FOCUS-0122`. El enunciado resultó CIERTO y verificado contra el árbol de HOY:
`ProbeBank` reservaba **tres** formas paralelas por sonda mientras `ProbeReliabilityEstimator.MIN_ADMINISTRATIONS`
valía **tres**, los dos carriles (evaluación y progreso) piden `week = 1` sobre un cursor con clave común
`userId:probeId`, y la propia línea base consume una administración. Consecuencia medida: la primera
administración con la puerta de fiabilidad abierta es la número MIN+1, que dibuja el índice de forma MIN — es
decir, **exactamente la forma que el usuario ya vio en su línea base**, así que el primer retest capaz de
sostener una afirmación de cambio fiable era el que corría sobre material repetido. Y tres cosas más, todas
sobre mi propio trabajo. (1) La salida obvia —subir el suministro— **no rompe la fase, la mueve**: con 4 formas
y umbral 3 la coincidencia desaparece hoy y vuelve el día que alguien toque cualquiera de los dos números, que
viven en ficheros distintos y nadie relaciona. (2) Al escribir el tercer gate, `repeatsEarlierForm` es
tri-estado (`Boolean?`) y el `!= true` que sale solo habría dejado pasar `null` —«esta fila no lleva la
identidad de la forma»— **como si fuese material fresco**: la respuesta favorable fabricada desde el
desconocimiento. (3) El ancla de la cuarta mutación (`repeatsEarlierForm = administration.repeatsEarlierForm,`)
aparece **dos veces** en el holder —en la llamada a `evaluate(...)` y en el estado `Finished(...)`— y el arnés
abortó con `anchor count 2` ANTES de escribir. Y (4), lo más grave de la fase y lo que ninguna prescripción
contenía: el **gate de salida adversarial** devolvió, convergente en dos lentes independientes, un P1 VIVO en
producción. `LocalProbeBankRepository.readCursor` resolvía el cursor de formas con `readParsed`, que ante
`SecureReadResult.Failure` devuelve `null` **y además pone la clave en CUARENTENA, es decir la BORRA**; el
repositorio colapsaba ese `null` en `0` y a continuación escribía `1` incondicionalmente, con lo que el
reinicio quedaba permanente. Cadena medida: `formSeed` vuelve a `reservedForms[0]` —la forma que el usuario ya
hizo en su **línea base**— y `repeatsEarlierForm` sale `false`, así que la copy **retira su cláusula de
divulgación**, afirma material distinto, y el gate de frescura que yo acababa de instalar pasa sobre un hecho
que nadie midió. Es el aprendizaje de 2026-07-20 (un `read` que colapsa fallo en ausencia) por una vía nueva, y
lo encontró el gate, no yo.

**Causa raíz:**
Una coincidencia entre dos constantes que viven en ficheros distintos no tiene dueño: cada una es correcta por
separado y el defecto sólo existe en su RELACIÓN, así que ningún test que pinnee una de las dos lo ve. Y la
reparación aritmética (subir una de ellas) restaura la propiedad sin crear el invariante, de modo que el defecto
vuelve en silencio con la siguiente edición legítima. La segunda causa es de tipos: un booleano nullable que
gobierna una afirmación tiene TRES respuestas —sí, no, no puedo decirlo— y `!= true` colapsa las dos últimas en
la primera, que es justo la dirección cara. La tercera, la del P1, es de **propiedad del hecho**: el cursor es el
dueño ÚNICO de qué formas se sirvieron y no se puede re-derivar de nada, así que un helper de lectura cuya
política ante el fallo es CUARENTENAR —correcta para un dato regenerable, que es para lo que se diseñó— lo
destruye de forma irreversible; y colapsar ese fallo en `0` no elige un valor neutro, elige el MÁS favorable que
existe («no se ha servido ninguna forma»), que es exactamente el que devuelve la línea base sellada como fresca.

**Solución aplicada:**
Dos mitades, y ninguna sustituye a la otra. (a) **Estructural:** el estimador gana un tercer gate
`formIsFresh = (repeatsEarlierForm == false)`, así que una administración repetida —o de identidad desconocida—
no puede sostener una afirmación de cambio fiable pase lo que pase con los números. El hecho **no se persiste**:
se DERIVA del que `nextProbe` ya declaraba desde R9, así que no nace un segundo dueño del mismo hecho, y viaja
como parámetro **obligatorio sin default** en `FocusEvaluationService.evaluate`. (b) **Aritmética:**
`reservedForms` pasa de 3 a 4. El cierre mecánico es un guard **DERIVADO de las dos autoridades a la vez**
(`ProbeReliabilityEstimator.MIN_ADMINISTRATIONS + 1` para la puerta, `ProbeBank.reservedForms` para el
suministro) e iterando `ProbeBank.all`, con su mitad discriminante (el suministro sigue siendo finito: tras la
última forma la siguiente administración envuelve y lo declara). Mutaciones: **M1** (quitar la cuarta forma) y
**M2** (`MIN_ADMINISTRATIONS` 3→4) ponen en ROJO **el MISMO test**, que es la prueba de que está derivado de
ambas y no de una; M3 (`formIsFresh = true`) 2 rojos, M4 (el holder pasa un literal `false`) 1 rojo. El ancla de
M4 se reescribió con su línea predecesora para volverla única. (c) **El P1 se paró y se arregló antes de sellar**
nada: `readCursor` lee el `SecureReadResult` CRUDO y devuelve `Int?` con TRES respuestas (0 primera
administración genuina, el conteo, `null` ilegible), no cuarentena nunca, sólo avanza un cursor que pudo LEER, y
falla CERRADO —una identidad desconocida declara la repetición y retiene, igual que una envoltura real—;
`releaseProbe` deja de rebobinar una posición que no conoce. M5 (fail-open restaurado) y M6 (cuarentena
restaurada) dejan 2 rojos de 13 cada una, pero por aserciones **DISTINTAS** del mismo guard —frescura frente a
supervivencia de la entrada—, que es la prueba de que ninguna de las dos mitades sobra.

**Regla futura:**
Cuando un defecto viva en la **relación entre dos constantes de ficheros distintos** (un suministro y un umbral,
un tamaño de buffer y una ventana, un TTL y un periodo de refresco), no basta con separarlas: el arreglo
aritmético mueve la coincidencia y la devuelve con la siguiente edición legítima. Hacen falta **las dos mitades**
— una estructural que impida la consecuencia pase lo que pase con los números, y una aritmética con un guard
**DERIVADO de ambas autoridades**; y la prueba de que el guard está derivado es que **mutar cualquiera de las dos
por separado ponga en ROJO el mismo test**. Una tabla de valores escrita a mano no pinnea la relación: pinnea
dos números. Todo hecho **tri-estado** que gatee una afirmación se compara con la comparación EXPLÍCITA
(`== false`), nunca con la negación (`!= true`): la tercera respuesta es «no puedo decirlo» y debe RETENER, no
colar la favorable. Y el parámetro que lo transporta va **obligatorio y sin default**, porque un default
re-fabrica la respuesta favorable exactamente en las corridas en que el hecho importa y **ningún guard textual
por fichero puede verlo** (precedente `MB-FOCUS-0071`). Antes de construir un almacén para un hecho, pregunta
**quién es su dueño hoy**: si algo ya lo declara, la salida es DERIVARLO (precedente R9, `MB-FOCUS-0119`).
Antes de resolver una lectura con un helper compartido (`readParsed`/`readValueOrNull`), pregunta **qué hace ante
`Failure`**: si CUARENTENA, sólo sirve para datos REGENERABLES, y un contador que es el dueño único de un hecho
que no se puede re-derivar de nada no lo es — leer el resultado CRUDO y mapear a un tri-estado es la única forma
honesta (precedente 2026-07-05, y esta es su reincidencia). Al colapsar un fallo de lectura en un número,
**pregunta en qué dirección miente ese número**: un `0` en un contador de consumo no es neutro, es el máximo
favorable («nada se ha gastado»), y aguas abajo eso reaparece como una afirmación al usuario. Y cuando el arreglo
sea «no colapses», el guard necesita **dos mitades separadas** —que la ausencia RETENGA y que la entrada
SOBREVIVA— porque una mutación puede matar una y dejar la otra viva: si tus dos mutaciones caen por la misma
aserción, no has probado dos propiedades, has probado una. **Trata un hallazgo con CERO refutaciones como NO
JUZGADO, jamás como refutado**: si tu arnés marca muerto lo que nadie llegó a examinar (aquí porque el límite de
uso mató 18 de 28 agentes), la ausencia de veredicto se lee como veredicto favorable — el mismo defecto que el
código persigue, cometido por el arnés que lo audita. Y **audita tu propia corrección con el estándar del defecto
que corrige**: el KDoc que escribí para el guard de divulgación prometía cubrir el salto del estado a la pantalla
y sólo pinnea el contrato de copy; se declara el TECHO y se registra el hueco (`MB-FOCUS-0136`) en vez de dejarlo
como alcance decidido. Y **mide el efecto de tu propio arreglo antes de darlo por hecho**: aquí el desacoplamiento es de UNA unidad, así
que el usuario obtiene exactamente UN retest fiable con material fresco por sonda — un número que ninguna cifra
de producto declara y que se registró con dueño (`MB-FOCUS-0133`) en vez de elegirlo por mi cuenta. Cierra lo que
de verdad cerraste y no más: `MB-FOCUS-0119` sigue **MITIGATED** porque el suministro **sigue envolviendo**, sólo
que una administración más tarde y sin poder sostener ya una afirmación. Corolarios de entorno, dos conocidos que
volvieron a morder: (1) un ancla de mutación de una sola línea **puede no ser única** cuando el mismo campo se
pasa a una llamada y a un constructor de estado en el mismo fichero — el guard de conteo abortó antes de escribir
y el árbol quedó intacto, que es exactamente para lo que existe; se extiende el ancla con su línea predecesora,
no se relaja el guard; (2) un fichero de test **NUEVO** entra entero en el ratchet de detekt: tres líneas de más
de 120 caracteres en el guard nuevo rompieron el gate, y `MaxLineLength` se arregla PARTIENDO la llamada
(`assertFalse(valor, "mensaje")` en varias líneas), nunca regenerando el baseline — comprobando después que el
fichero conserva su EOL (aquí CRLF).

**Archivos afectados:**
- `shared/src/commonMain/.../focus/data/local/LocalFocusRepositories.kt` (**el P1**: `readCursor` tri-estado sin cuarentena, avance condicionado, fail-closed, `releaseProbe` sin rebobinado ciego)
- `shared/src/commonMain/.../focus/domain/evaluation/ProbeBank.kt` (cuarta forma reservada por sonda) · `.../ProbeReliabilityEstimator.kt` (tercer gate `formIsFresh`)
- `shared/src/commonMain/.../focus/domain/evaluation/FocusEvaluationService.kt` (`repeatsEarlierForm: Boolean?` obligatorio, sin default)
- `shared/src/commonMain/.../focus/presentation/{FocusEvaluationStateHolder,FocusProgressStateHolder}.kt` (el hecho real frente a `null` declarado)
- `shared/src/commonTest/.../focus/data/local/LocalFocusRepositoriesTest.kt` (+2 guards del P1) · `.../focus/domain/evaluation/{ProbeBankTest,FocusEvaluationServiceTest}.kt` · `.../focus/presentation/FocusEvaluationStateHolderTest.kt` (+2 tests netos y un KDoc con techo declarado)
- `docs/audits/AUDITORIA_ADVERSARIAL_ENFOQUE_2026-08-29.md` (§23) · `MINDBOOST_AUDIT_LEDGER.md` (`MB-FOCUS-0122` CLOSED y `0134` CLOSED; `0119` MITIGATED y `0121` actualizados; `0133`/`0135`/`0136` nuevos OPEN)

### 2026-09-07 — Un arnés que responde a TODO es la estrategia CIEGA, no el jugador honesto: mientras ninguna puerta lee el patrón de respuesta, el fixture deshonesto es indistinguible del honesto; y un veredicto nuevo que PRECEDE a otro en la jerarquía vacía en silencio los tests del que desplaza

**Error detectado:**
Fase R15 del bucket 60d (H-09: «cero anti-gaming en todo el motor Enfoque», enunciado verificado CIERTO con grep
antes de tocar producción). Al cablear la puerta, **nueve tests que se creían honestos se pusieron ROJOS** con
`NotMeasurable(RESPONSE_PATTERN_NOT_INTERPRETABLE)`. Medido: `FocusTestFixtures.driveWithResponses` y los bucles
propios de `FocusQualityGateWiringTest` y `FocusSessionStateHolderTest` respondían a TODO campo visible —uno de
ellos con un parámetro llamado literalmente `tapEverything`—, que en un carril go/no-go **es** la estrategia de
responder a ciegas: precisión medida en torno a 0,20 frente a un techo ciego de 0,80 en E1. Es decir, el fixture
que tres suites usaban como «el usuario honesto» era el adversario. Y había un segundo defecto encima: como el
veredicto de patrón de respuesta **precede** en la jerarquía a `LIFECYCLE_NOT_OBSERVED`, dejar el arnés ciego
habría hecho que los tests del sensor no observado pasaran a cumplirse por la rama EQUIVOCADA del mismo enum
sellado — la vacuidad de 2026-09-03, reintroducida por el propio arreglo.

**Causa raíz:**
Un fixture solo es tan honesto como el gate más estricto que lo mira. Mientras NINGUNA puerta leía el patrón de
respuesta, «responder a todo» y «jugar la tarea» eran indistinguibles para toda la suite, así que el arnés
barato se escribió una vez y se copió tres veces sin que nada pudiera denunciarlo. El día que un gate empieza a
leer una señal que antes nadie leía, los tests que «pasaban» no estaban ejercitando esa señal: estaban ciegos a
ella, y sus rojos no son regresiones, son la primera medición.

**Solución aplicada:**
El arnés JUEGA la tarea: `honestResponseTo(field)` pasa a ser el dueño ÚNICO de «qué contesta un jugador
honesto» (go respondido, no-go retenido, elección hacia el lado correcto) y los tres arneses lo consultan en vez
de llevar cada uno su copia; cero cambio en el umbral de la puerta. El control se cablea en
`FocusSessionAssembler.admit` —único call-site de producción de la puerta, ya fijado por el escáner
`focusQualityGateScanner`— y el veredicto se **DERIVA allí** desde los ensayos, con el parámetro `gaming`
**obligatorio y sin default**. La prescripción decía «la ruta de evaluación» y el punto honesto resultó MÁS
amplio (los tres carriles); la corrección se declaró en vez de heredarse. El techo ciego se calcula como
`max(p, 1-p)` sobre la proporción de objetivos SERVIDA, no como un 0,5 plano, y una planificación degenerada de
una sola clase (la forma de E10) hace IDLE en vez de disparar. Cuatro mutaciones con atribución 1-a-1: M1 corta
el cableado (3 rojos), M2 el detector nunca dispara (2), M3 retira la guarda de degeneración (1), M4 devuelve el
techo a 0,5 (4).

**Regla futura:**
Antes de dar por honesto un fixture que «conduce» una tarea, pregunta **qué estrategia está jugando de verdad**
y compárala con el techo de la estrategia más barata: responder a todo campo visible NO es jugar, es la
estrategia ciega, y en una tarea de detección su techo es `max(p, 1-p)` sobre la proporción servida —jamás 0,5,
que es el default halagador que este repo ya tenía registrado—. Cuando cablees un gate que lee una señal que
antes no leía nadie, **cuenta los tests que se ponen rojos y trátalos como una medición, no como una
regresión**: la salida correcta casi nunca es ablandar el gate recién puesto, es arreglar el fixture; y si
varias suites llevan su propia copia del conductor, dale un dueño ÚNICO en vez de arreglar tres copias. Antes de
elegir la POSICIÓN de un veredicto nuevo dentro de una jerarquía de precedencia, comprueba **a quién desplaza**:
un veredicto que precede a otro hace que los tests del desplazado se cumplan por su rama, y quedan verdes
afirmando lo que ya no comprueban. Para un techo que depende de la población servida, mide siempre el caso
DEGENERADO (una sola clase): ahí el techo es 1,0 y leerlo como alcanzable acusa a todo jugador de un defecto que
pertenece al ejercicio, así que la salida honesta es IDLE. Declara la cobertura **medida**, no supuesta: el
detector la publica (`COVERAGE`) y el consumidor la consulta, y si es PARCIAL, escribe POR QUÉ con el dato que
falta (aquí `TrialResult` no persiste ni la tecla ni un timebase entre respuestas, así que dos de los once
detectores no son cableables sin fabricarles la entrada). Y declara también la consecuencia incómoda: aquí un
usuario honesto que de verdad rinde al nivel del techo queda retenido igual que uno ciego, y eso se registra
como hallazgo con dueño en vez de omitirse. Corolarios de entorno, uno NUEVO y tres conocidos: (1) **un guard de
compare-and-swap vale lo que valga su EXTRACCIÓN** — el mío tomaba el primer hash de 64 hex entre acentos graves
del fichero y en el ledger hay hashes citados en otras filas, así que abortó denunciando un escritor concurrente
**fantasma** con el fichero intacto; se ancla en la línea `Canonical SHA-256`, y el fallo simétrico (casar una
línea rancia) habría dejado pisar una escritura ajena de verdad; (2) `spawnSync` de un `.bat` en Windows
necesita `shell: true` **y** la ruta ABSOLUTA entre comillas: sin shell el log sale vacío y con shell pero ruta
relativa cmd responde que no reconoce el comando aunque el fichero esté en el cwd; (3) un `import` ESM de una
ruta absoluta de Windows exige `file:///C:/...`, o Node aborta con `ERR_UNSUPPORTED_ESM_URL_SCHEME`; (4)
`perl -0pi` con un `\n` dentro de `\Q...\E` NO casa (la barra se cita literalmente) y la sustitución falla en
silencio: para mover líneas, `sed -i '<n>d'` más `sed -i '/ancla/a …'`.

**Archivos afectados:**
- `shared/src/commonMain/.../focus/domain/quality/FocusAntiGaming.kt` (nuevo: adapter de telemetría + `BlindStrategyCeilingDetector`) · `.../focus/domain/quality/SessionQualityGate.kt` (razón nueva + parámetro `gaming` obligatorio) · `.../focus/domain/session/FocusSessionAssembler.kt` (veredicto derivado en el único call-site)
- `shared/src/commonMain/.../focus/presentation/{FocusBaselineStateHolder,FocusEvaluationStateHolder}.kt` (copy no acusatoria)
- `shared/src/commonTest/.../focus/FocusTestFixtures.kt` (`honestResponseTo`, dueño único) · `.../focus/presentation/{FocusQualityGateWiringTest,FocusSessionStateHolderTest}.kt` · `.../focus/domain/quality/SessionQualityGateTest.kt` · `.../focus/domain/session/FocusSessionAdmissionTest.kt` · `.../focus/data/mapper/FocusPersistenceMappersTest.kt`
- `shared/src/desktopTest/.../focus/domain/quality/FocusAntiGamingTest.kt` (nuevo, red-team) · `.../core/scoring/AdequateCoverageInvariantTest.kt` (sitio registrado)
- `docs/audits/AUDITORIA_ADVERSARIAL_ENFOQUE_2026-08-29.md` (§25) · `MINDBOOST_AUDIT_LEDGER.md` (`MB-FOCUS-0097` OPEN→MITIGATED; `0144`/`0146`/`0147` nuevos; `0145` CLOSED)

### 2026-09-06 — Una métrica se sella sobre SU celda, nunca sobre el conteo de ensayos del bloque: el peso de una media ponderada no es una muestra; un guard que entrega el conteo de ensayos a un piso afirma lo CONTRARIO del hallazgo; y una sonda con población CERRADA de formas convierte la alcanzabilidad en un hecho que se pregunta al productor, no a una autoridad que declina

**Error detectado:**
Fase R12 del bucket 60d. Su prescripción escrita era «dimensionar la sonda desde `MinSampleGate` hacia arriba,
calculando la alcanzabilidad con `MeasurementReachability` y negarse a emitir informe si `reachable = false`».
Cuatro cosas, y dos son sobre trabajo propio. (1) El MECANISMO no existe para la sonda: `verdictFor(E1_VIGIA, 55, …)`
devuelve `null`, no `reachable = false`, porque su tabla `cells` cubre SOLO las seis comparativas con cuota y `RT_CV`
es una métrica de una celda; el test T3 del plan y el gate de cierre de `MB-FOCUS-0092` eran insatisfacibles tal
como estaban escritos. (2) El guard que ya existía —`the one probe actually wired can clear its floor`— «demostraba»
que la sonda cruza su piso entregando `probeTrialCount` como celda de `RT_CV`: un ensayo al que nadie tenía que
responder no produce tiempo de reacción, así que 55 ensayos a una tasa de señal de 0,20 llevan unas 11. El guard
afirmaba lo contrario del hallazgo H-02 y estaba VERDE. (3) Medido con el productor real (`FocusGraph.fixedBlockForm`)
sobre las TRES formas reservadas: **11, 11 y 13** señales frente a un piso de 20; ninguna carga perceptual cruza (el
eje solo hace las señales más raras; máximo 0,30 a carga 0); la longitud mínima a la que TODAS cruzan es **110**, y
98 y 109 fallan por la forma 1003 con 18, porque `nextInt(count)` re-siembra el flujo y cada longitud es una forma
distinta, no un prefijo. (4) Lo grave estaba una capa más abajo y no en la prescripción: `DefaultMetricsEngine.sessionMetrics`
sellaba cada métrica ponderada por ensayos —y `LAPSES`— con el CONTEO DE ENSAYOS como `n`, así que `RT_CV`, que a
nivel de bloque negaba honestamente la suficiencia con celda 11, volvía a nivel de sesión con `n = 55` y
`measurementSufficient = true`. Y lo di por LATENTE tras leer UN call-site del comparador (la pantalla de evaluación
usa `blocks.single().metrics`); el otro (`FocusProgressStateHolder.kt:64`) alimenta el comparador con
`retest.sessionMetrics`, así que dos pantallas emitían veredictos DISTINTOS sobre el mismo retest, y el fabricado era
el de progreso.

**Causa raíz:**
Una media ponderada por ensayos tiene DOS números que se parecen: el PESO de cada bloque (sus ensayos) y la MUESTRA
de la métrica (su celda: para una métrica de RT, las señales atendidas). Usar el peso como `n` es `null != 0` en su
forma de PESO: no fabrica un valor desde la ausencia, fabrica una suficiencia desde un número real del bloque leído
como otro número real del bloque, y por eso ningún guard de ausencia lo ve. El guard de la sonda cometía la misma
sustitución desde el otro lado, y además fabricaba su fixture (un entero) en vez de consultar al productor: un guard
que fabrica su sujeto no puede notar el día que el productor cambia. Y una autoridad que DECLINA una pregunta
(`verdictFor` → `null`) se lee como tranquilidad si nadie se la hace al generador: para una sonda con población
CERRADA de formas la alcanzabilidad no es una expectativa sobre un sorteo, es un hecho enumerable.

**Solución aplicada:**
El motor sella `n = contributions.sumOf { it.first.n }` y `LAPSES` sobre `lapseCells.sumOf { it.n }` con
`lapseCell >= MIN_N_RT`; test `sessionMetrics_stampTheMetricsOwnCell_notTheTrialCount` (55 ensayos, 11 señales:
`RT_MEDIAN`/`RT_CV`/`LAPSES` con n 11 e insuficientes; `ACCURACY` con n 55 suficiente, la mitad discriminante),
ROJO antes del arreglo (`expected 11 but was 55`). El guard de la sonda se reescribe EN SITIO como
`the wired probe cannot sample the metric its claim rests on`: consulta `FocusGraph.fixedBlockForm` para cada forma
reservada y exige insuficiencia en todas, con anti-vacuidad y con ACCURACY cruzando el mismo piso; y
`the probe remedy surface is pinned` fija 110, la frontera 109 en rojo y un barrido de carga 0..100 sin cruce. R12
NO se ejecuta como prescribe: `verdictFor` no se cablea (declina para E1), la mitad «negarse a emitir» ya tiene
dueño en `EvaluationComparator` sobre la muestra REALIZADA (`measurementSufficient == true` +
`withheldForSample`), y redimensionar la sonda es decisión de producto que invalida líneas base capturadas
(`MB-FOCUS-0130`, con dependencia de `MB-FOCUS-0126`). Mutaciones: M1 y M2 (motor) 1 rojo de 12 cada una, el mismo
test; M3 (tasa de señal del productor 0,30 → 0,60) 2 rojos de 12, exactamente los dos guards nuevos, motor verde.
Gate completo: 4446/0F/0E sobre 621 XML frescos (+2 exactos), detekt 0, huella de árbol idéntica.

**Regla futura:**
En toda media ponderada, **el peso y la muestra son dos números distintos**: el `n` que se sella es la suma de las
CELDAS de la métrica, jamás el conteo de ensayos que hace de peso; y una métrica de RT tiene por celda las
respuestas que produjeron un tiempo, no los ensayos servidos. Antes de dar un defecto de un productor por LATENTE,
**enumera TODOS los consumidores del valor que produce** (aquí el comparador tenía dos call-sites, bloque y sesión, y
solo uno estaba protegido): un defecto que un consumidor no ve puede ser exactamente el que otro renderiza.
Cuando un guard entregue un número a un piso (`nMinPerCell`), pregunta **de qué celda es ese número**: un guard que
pasa el conteo de ensayos como celda de una métrica de RT no protege el invariante, afirma su negación con el
build en verde. Para una sonda con población CERRADA de formas reservadas, la alcanzabilidad **no se estima: se
enumera** preguntando al productor real por cada forma, y un `null` de una autoridad de alcanzabilidad no es un
veredicto favorable, es una pregunta que hay que hacer en otro sitio. Una superficie de remedio sobre un generador
que re-siembra por longitud **no es una propiedad de prefijo**: cada longitud es otra forma, así que la frontera se
pinnea en AMBOS lados (110 cruza, 109 no) y se busca hasta un techo declarado, nunca se extrapola. Y, como en R9,
R10 y R11, verifica el ENUNCIADO y el MECANISMO de la prescripción por separado: aquí el enunciado era cierto y más
fuerte de lo escrito, el mecanismo no existía, y la mitad útil ya tenía dueño. Corolarios de entorno: un test
temporal de MEDICIÓN escrito en el árbol se borra con su XML y se verifica la ausencia antes de correr el gate; los
backups de mutación van al scratchpad (nunca junto al fuente, o salen en el `git status` de un gate posterior); la
herramienta Bash puede morir con `fork: retry: Resource temporarily unavailable` a mitad de una comprobación y se
repite por PowerShell, no se cambia nada; y un mensaje de config-cache de Gradle sobre `build.gradle.kts` «changed»
no es una sesión concurrente hasta que el mtime con FECHA completa lo diga.

**Archivos afectados:**
- `shared/src/commonMain/.../focus/domain/metrics/DefaultMetricsEngine.kt` (`weightedAverage` sella la suma de celdas; `LAPSES` sobre su celda)
- `shared/src/commonTest/.../focus/domain/metrics/DefaultMetricsEngineTest.kt` (+1 test con mitad discriminante)
- `shared/src/desktopTest/.../focus/domain/evaluation/ProbeMeasurementReachabilityTest.kt` (guard reescrito para consultar al productor, +1 test de superficie de remedio, helper `signalsIn`)
- `docs/audits/AUDITORIA_ADVERSARIAL_ENFOQUE_2026-08-29.md` (§22, nota de vigencia R12) · `MINDBOOST_AUDIT_LEDGER.md` (`MB-FOCUS-0092` MITIGATED con gate reescrito; `0128`/`0129` CLOSED; `0130`/`0131`/`0132` nuevos OPEN)

### 2026-09-04 — Una rama MUERTA puede estar ARMADA: no fabrica un veredicto hoy, hace que un gate futuro se satisfaga RENOMBRANDO una etiqueta; y un guard que fabrica su propio fixture no puede ver cambiar a su productor

**Error detectado:**
Fase R11 del bucket 60d. Su prescripcion escrita era redisenar el banco de sondas para que declarasen un
paradigma AUSENTE del catalogo, con un test de aceptacion propuesto: «falla si el `Paradigm` de cualquier
`ProbeDefinition` aparece en `ExerciseCatalog.implemented`». El ENUNCIADO resulto CIERTO (las tres sondas
declaran paradigmas entrenados) y la PRESCRIPCION resulto **no ejecutable**, por cinco bloqueadores medidos: su
test es la **negacion logica exacta** de `FocusClaimLevelGateTest.everyProbeParadigmIsAlsoATrainedParadigm`,
vivo y verde —anadirlo sin borrar el otro son dos duenos de un invariante con polaridad OPUESTA—; una sonda con
paradigma nuevo REVIENTA los dos carriles de produccion en `requireNotNull`;
`ExerciseCatalogConsistencyTest` fuerza `implemented == wiredExerciseIds`, asi que toda sonda RENDERIZABLE
tiene por fuerza paradigma entrenado y R11 es **estructuralmente insatisfacible**; y no hay piso ratificado de
`MinSampleGate` para un instrumento nuevo. Pero lo grave no era eso. De los 14 paradigmas del enum, exactamente
DOS no los entrena nadie, y se comportaban de forma OPUESTA: `SIMON` tiene 1 sola referencia en `shared/src` (su
entrada del enum) y cae en `else -> null`, mientras `MACKWORTH_CLOCK` tenia una segunda referencia que le daba
**renderizador entrenado** (`-> E1_VIGIA`). Ninguna sonda lo declaraba: rama muerta. Y armada — renombrar
`VIGILANCE_PROBE.paradigm` a `MACKWORTH_CLOCK` **satisfacia el test que R11 propone** dejando la tarea del
usuario byte-identica, con TODOS los guards de renderizabilidad y capacidad en VERDE y en rojo solo el guard
cuyo propio mensaje lee ese rojo como la invitacion a revisar la escalera de claims. Un segundo hallazgo, el mas
grave y ausente de la prescripcion: `BaselineSnapshot` no registra QUE instrumento la produjo y
`BaselineRepository` solo expone `load`/`save` —no puede invalidar—, mientras E1 y E2 comparten exactamente
`RT_MEDIAN`; cambiar `EVALUATION_WEEK` de 1 a 2 compara en silencio un pre de PVT contra un post de FLANKER y lo
renderiza como el cambio fiable del usuario. Y un tercero: el KDoc de `ProbeSeedSpace` afirmaba una disyuncion
ESTRUCTURAL («la fuente de entrenamiento es el reloj de pared, los rangos no se solapan nunca») cuyo productor
cambio el 2026-07-15 a `Random.Default.nextLong()`; sus TRES guards siguieron verdes dos meses.

**Causa raiz:**
Una rama muerta se juzga por lo que produce HOY —nada— y por eso pasa por inofensiva. Pero un mapeo
taxonomia-a-renderizador tiene una segunda propiedad: define **que etiquetas son satisfacibles**, y una entrada
sin sonda que la reclame no es codigo muerto, es una **puerta abierta con el cartel quitado**. El invariante que
lo habria cazado —la tarea que se RENDERIZA es la que la sonda DECLARA— no tenia dueno, porque el guard
existente pinnea la CAPACIDAD, y el par MACKWORTH-E1 la satisface exactamente. Y (3) es la forma general del
mismo defecto en los tests: un guard que **construye el fixture cuya propiedad afirma** no verifica al sistema,
verifica a si mismo; el dia que el productor real cambia, el guard no puede notarlo porque nunca lo consulto.

**Solucion aplicada:**
La rama se **BORRA** (precedente `MB-CATALOG-0022`, `MB-FOCUS-0080`) y el invariante huerfano gana dueno:
`everyProbeRendersTheParadigmItDeclares` compara lo declarado con lo renderizado, mas la mitad discriminante
`paradigmsTheCatalogueDoesNotTrainFailClosed`, que DERIVA el conjunto no entrenado del catalogo en vez de
enumerarlo (una sonda futura con paradigma nuevo cae sola) y lleva su propia anti-vacuidad: si ese conjunto
queda vacio, el test falla diciendo que la taxonomia o el catalogo cambiaron. El KDoc de `ProbeSeedSpace` pasa a
declarar la garantia REAL y no la que caduco, y el guard nuevo **MUESTREA al productor** (200 acunaciones) en vez
de fabricar semillas. R11 NO se ejecuta y su test de aceptacion NO se anade. Se registran con dueno la refutacion
(`MB-FOCUS-0124`), la comparacion cruzada de lineas base (`MB-FOCUS-0126`) y la duplicidad de guards
(`MB-FOCUS-0127`). Mutaciones: M1 (rama restaurada) 1 rojo de 13, solo el fail-closed; **M2 (el ataque completo:
rama restaurada mas sonda renombrada) 3 rojos de 13, con el guard de mapeo y el de capacidad VERDES** —la prueba
de que los guards viejos eran ciegos al ataque—; M3 (productor devuelto al reloj) 1 rojo de 9, y el guard viejo
del fixture VERDE bajo esa misma mutacion.

**Regla futura:**
Al auditar un mapeo de una taxonomia a un renderizador (paradigma-ejercicio, tipo-vista, evento-handler),
**enumera los valores del enum que NADIE reclama y comprueba que fallan CERRADOS**: los que tienen rama viva no
son codigo muerto, son etiquetas satisfacibles sin trabajo — y si alguna vez un gate futuro pide «declara una
etiqueta que el sistema no soporta», esa rama la convierte en un renombrado. Que dos valores huerfanos del mismo
enum se comporten de forma OPUESTA (uno fail-closed y otro con renderizador) es la senal, y se ve con un grep de
referencias por valor, no leyendo el `when`. Antes de anadir el guard que una prescripcion pide, **busca si su
inverso ya tiene dueno vivo**: un test que es la negacion logica de otro test verde no es cobertura, es una
contradiccion que alguien resolvera borrando el que estorbe. Y verifica si la prescripcion es siquiera
**satisfacible** contra los contratos que el CI ya impone: aqui `implemented == wiredExerciseIds` hacia imposible
lo pedido, y eso no se deduce leyendo el enunciado, se deduce leyendo el test de consistencia. Todo guard que
afirme una propiedad de un productor debe **CONSULTAR al productor**, no fabricar un fixture con su forma
esperada: un guard auto-fabricado sobrevive al dia en que el productor cambia, que es exactamente el dia que
existia para cazar — y la mitad discriminante que lo prueba es mutar el productor y comprobar que el guard viejo
sigue VERDE. Deriva del sistema los conjuntos que un test recorre (`Paradigm.entries - trained`) y ponles
anti-vacuidad, o el guard se vuelve tautologico en silencio cuando el conjunto se vacie. Y mide el alcance de un
hallazgo en AMBAS direcciones antes de creerte su severidad: aqui la colision es 3 de 3 sondas a nivel de banco
pero 1 de 3 en produccion (los dos carriles fijan la semana 1), y el defecto MAYOR no estaba en la prescripcion.
Corolarios de entorno, uno NUEVO y tres que volvieron a morder: (1) **medir el EOL con `grep -c` a traves de la
herramienta Bash da un 100 por cien CRLF perfecto y FALSO** —el escape se degrada a la letra literal `r`, que
aparece en casi toda linea de Kotlin—, asi que el EOL se mide con Node y **por fichero** (aqui 2 LF y 2 CRLF en
el mismo directorio); mi guard aborto antes de escribir, que es por lo que existe; (2) unos ficheros ajenos
modificados NO son una sesion concurrente hasta que mires el mtime con **FECHA completa** — los mios eran de dias
anteriores y llegue a declarar en voz alta una concurrencia inexistente; la hora sola no atribuye nada; (3) el
scanner de copy volvio a cazar `odds` en un KDoc mio describiendo una probabilidad, el mismo lexema y el mismo
error de 2026-08-19: al escribir un comentario sobre probabilidades, no uses el vocabulario de apuestas ni en
ingles; (4) valida la formula del sello REPRODUCIENDO el valor vivo antes de estampar uno nuevo, y valida el
ledger completo corriendo el validador REAL sobre una copia en el scratchpad antes de escribir en el repo.

**Archivos afectados:**
- `shared/src/commonMain/.../focus/domain/evaluation/ProbeExerciseMapping.kt` (rama `MACKWORTH_CLOCK` BORRADA) · `.../ProbeDefinition.kt` (KDoc de `ProbeSeedSpace` corregido)
- `shared/src/commonTest/.../focus/domain/evaluation/ProbeExerciseMappingTest.kt` (+2 guards) · `.../ProbeContaminationTest.kt` (+1 guard de productor, 1 renombrado, KDoc corregido)
- `MINDBOOST_AUDIT_LEDGER.md` (`MB-FOCUS-0123` CLOSED; `0125` MITIGATED; `0124`/`0126`/`0127` nuevos OPEN)

### 2026-09-04 — Un simbolo que existe no es un simbolo cableado: la premisa de una prescripcion se mide con un grep de CALL-SITES, no leyendo su KDoc; y cuando el hecho ya tiene dueno unico, el arreglo es DERIVARLO, no construirle un segundo almacen

**Error detectado:**
Fase R9 del bucket 30d. Su prescripcion escrita era una sola frase: «persistir la identidad de la forma
administrada y un registro de exposicion **reutilizando `core/bank/ExposureTracker`, que ya existe**». Las dos
mitades del enunciado resultaron ciertas y la PRESCRIPCION resulto **falsa en su premisa y danina al pie**.
Medido: `ExposureTracker` tiene **cero call-sites de produccion** en todo `shared/src` (solo su propio test), y
la justificacion del informe («Memoria si lo usa») es falsa — un grep de `xposure` bajo `memory/` no devuelve
nada; lo que Memoria usa de verdad es `EmotionExposureStore`, una interfaz `suspend` durable construida A
PROPOSITO en vez de reutilizar `ExposureTracker`, es decir el precedente del repo apunta al lado CONTRARIO.
Peor: su forma es incompatible con la capa que R9 tocaria (clase en memoria con `MutableMap`, no `suspend`, sin
`SecureKeyValueStore`, sin `schemaVersion`, sin namespace, sin gancho de borrado) y **no tiene operacion
INVERSA**, asi que cablearla habria reintroducido en un TERCER contador — que ademas se reinicia en cada
arranque de proceso — exactamente el defecto de quemar-sin-administrar que `MB-FOCUS-0117` acababa de cerrar.
Y un cuarto hallazgo que la prescripcion no contenia: `MIN_ADMINISTRATIONS` vale 3 y `reservedForms` tiene 3
entradas, asi que la PRIMERA fiabilidad test-retest calculable caeria justo sobre la forma repetida.

**Causa raiz:**
«Ya existe» describe la presencia de un SIMBOLO, no la de una capacidad. Un componente con test propio y KDoc
convincente se lee como infraestructura viva, y la unica pregunta que separa las dos cosas —¿quien lo llama en
produccion?— no se responde leyendo el fichero, se responde con un grep. La segunda causa es de propiedad: el
hecho que R9 queria registrar (que forma se sirvio y si repite) **ya tenia dueno unico y durable** en el cursor
de formas que `nextProbe` avanza; construir un almacen nuevo no habria anadido informacion, habria anadido un
SEGUNDO dueno del mismo hecho, que es la fuente de divergencia que este repo persigue en todas partes.

**Solucion aplicada:**
Cero almacenes nuevos. `nextProbe` **DERIVA** el hecho del cursor que ya lleva (`index >= reservedForms.size`
es exactamente «todas las formas se sirvieron y esta vuelve») y lo publica como
`ProbeAdministration.repeatsEarlierForm`, **obligatorio y sin default** — un default dejaria que un scheduler
futuro callara una repeticion, y el silencio aguas abajo se lee como «esta forma es nueva», la respuesta
favorable fabricada. El estado `Finished` lo transporta y el resultado **APENDE** una clausula de divulgacion
sobre una base congelada **byte-identica**. Correccion de mi propio trabajo dentro de la fase: la primera
version REESCRIBIA la frase en un `if/else`, que es literalmente lo que el KDoc de `TransferReport` prohibe
(«do not reword a claim in order to disclose an omission»); su `WITHHELD_CLAUSE` ya demostraba la forma
correcta. La funcion de copy se extrajo a `internal` para que el emparejamiento «clausula presente exactamente
cuando la forma repite» sea alcanzable desde un test: inline era un literal privado que ningun guard podia ver,
que es la forma que dejo enviar la repeticion sin declarar. Y se corrigio mi propio encuadre a mitad de fase:
llegue a describir la copy como una afirmacion **falsa** y no lo es — su comparacion es sonda-contra-
ENTRENAMIENTO, que el espacio de semillas reservado garantiza pase lo que pase; es **ambigua**, no falsa.

**Regla futura:**
Antes de aceptar una prescripcion de la forma «reutiliza X, que ya existe», **mide sus call-sites de
produccion**: un `grep` del nombre del simbolo excluyendo su propio test responde en un comando lo que ningun
KDoc responde, y si el informe justifica el reuso diciendo que otro modulo ya lo usa, **verifica tambien esa
frase** — aqui era falsa y el modulo citado habia construido a proposito lo contrario. Cuando el componente
propuesto gestione un recurso que se consume, comprueba si tiene **inversa**: sin ella, cablearlo reintroduce
el defecto de consumir-sin-usar que ya se cerro en otro sitio. Y antes de construir un registro para un hecho,
pregunta **quien es su dueno hoy**: si algo ya lo sabe de forma durable, la salida correcta es DERIVARLO de ese
dueno, no crear un segundo almacen — construirlo no anade informacion, anade divergencia. Al divulgar una
omision sobre copy congelada por un gate, **APENDE una clausula y deja la base byte-identica**; reescribir la
frase cambia una afirmacion de producto de paso, y ese es un acto distinto del que la fase autoriza. Extrae la
funcion de copy a `internal` para que un test pueda pinnear el emparejamiento condicion-clausula; un literal
privado dentro de un composable es invisible para todo guard. Cuando un umbral de habilitacion (`MIN_*`)
coincida EXACTAMENTE con el tamano de un suministro finito, **registralo aunque este latente**: significa que
el primer resultado calculable cae sobre el caso degenerado, y poblar la costura «sin tocar call-sites» lo
armaria en silencio. Corolarios de entorno, dos ya conocidos y uno nuevo: la herramienta Bash vuelve a romper
un heredoc con comillas dentro (`unexpected EOF while looking for matching`) — los scripts con sintaxis
delicada van con la herramienta Write, nunca por heredoc; el EOL es propiedad del FICHERO y dentro del mismo
paquete conviven ambos (`ProbeDefinition.kt` y `LocalFocusRepositories.kt` CRLF frente a
`FocusEvaluationStateHolder.kt` y `FocusEvaluationScreen.kt` LF), asi que un ancla LF aborta con conteo 0; y
**`writeLedgerUnderLock` toma un objeto de opciones con un `token` de `acquire()`, no `(path, fn)`** — mi
llamada con la firma inventada devolvio `{ok:false, reason:"TOKEN_INVALID_PRE"}` y **no escribio nada**, que es
justo por que la regla de comprobar su retorno existe: con la firma equivocada habria reportado exito sobre un
fichero intacto. Un agregador de XML que reporta `tests=0` para dos suites que existen no es un cero, es un
regex degradado por el escape del shell: contrastalo leyendo la cabecera cruda del XML antes de creertelo.

**Archivos afectados:**
- `shared/src/commonMain/.../focus/domain/evaluation/ProbeDefinition.kt` (`repeatsEarlierForm` obligatorio) · `.../data/local/LocalFocusRepositories.kt` · `.../data/InMemoryProbeBankRepository.kt`
- `shared/src/commonMain/.../focus/presentation/state/FocusMeasurementUiState.kt` · `.../FocusEvaluationStateHolder.kt` · `.../ui/FocusEvaluationScreen.kt` (`retestClaimText` + `REPEATED_FORM_CLAUSE`)
- `shared/src/commonTest/.../focus/domain/evaluation/ProbeBankTest.kt` · `.../data/local/LocalFocusRepositoriesTest.kt` · `.../presentation/FocusEvaluationStateHolderTest.kt` (+7 tests)
- `MINDBOOST_AUDIT_LEDGER.md` (`MB-FOCUS-0119` MITIGATED; `MB-FOCUS-0120` CLOSED/REFUTADO; `0121`/`0122` nuevos)

### 2026-09-03 — Un fixture que NO responde hace pasar una puerta de retención por la razón EQUIVOCADA: el veredicto tiene varias entradas y la inacción satisface la aserción por otra rama del mismo enum; y una fórmula de sello se valida REPRODUCIENDO el valor vivo antes de estampar uno nuevo

**Error detectado:**
Fase del bucket 30d: `MB-FOCUS-0114` (sonda de ciclo de vida + `SessionQualityGate.admit` en los DOS carriles de
medición) ejecutada junto con R8. Cuatro cosas, y tres son sobre mi propio trabajo. (1) Al cablear la puerta, los
dos tests de camino feliz se pusieron ROJOS. El XML decía `NotMeasurable(NO_RESPONSES)`: los fixtures solo hacían
`advanceUntilIdle()`, así que **todos los ensayos expiraban** y la sesión era honestamente no medible. (2) Lo
grave estaba al lado: los tests de RETENCIÓN (interrumpido, sin sensor) **pasaban** — y habrían pasado igual con
la puerta rota, porque `NO_RESPONSES` produce el mismo estado de UI que `INTERRUPTED` y que `LIFECYCLE_NOT_OBSERVED`.
La aserción se cumplía por una rama del enum que nada tiene que ver con lo que el test dice proteger. (3) El reloj
por defecto del fixture es una constante, así que **todo RT vale 0 ms** — por debajo del suelo de anticipación de
150 ms —, de modo que "responder" sin arreglar el reloj habría convertido cada respuesta en una ANTICIPACIÓN.
(4) Detekt rechazó `runBaseline` con `LongMethod` 63 > 60; recorté el comentario de 7 a 3 líneas y **siguió
diciendo 63**, lo que demuestra que la regla NO cuenta comentarios.

**Causa raíz:**
Un veredicto sellado con VARIAS causas de rechazo (`INTERRUPTED` / `NO_RESPONSES` / `LIFECYCLE_NOT_OBSERVED`) tiene
tantas puertas de entrada como causas, y un fixture que no ejercita la tarea entra por la más barata. Como el
consumidor (la UI) colapsa las tres en el mismo mensaje, un test que afirma el ESTADO DE UI es indistinguible de
un test que no afirma nada: la inacción del fixture es lo que lo satisface. Es la familia de la vacuidad, pero con
una forma nueva — no falta la mitad discriminante, es que el sujeto llega por otra rama del mismo tipo sellado.

**Solución aplicada:**
Los fixtures **conducen la tarea de verdad**: `driveWithResponses` sondea el campo cada 100 ms y responde 250 ms
después de cada onset (RT medido en [250, 350] ms: por encima del suelo de anticipación y por debajo de toda
ventana de respuesta de este carril), con `virtualMonotonicClock()` atado al `testScheduler` para que el RT no sea
0. Y las aserciones bajan del estado de UI a la **razón TIPADA** leída del registro persistido
(`probeHistory(...).single().admission == NotMeasurable(INTERRUPTED)`), así que un fixture que dejara de responder
volvería a poner el test en ROJO en vez de dejarlo verde por `NO_RESPONSES`. `LongMethod` se resolvió EXTRAYENDO
`admitProbe(...)`, no recortando prosa. La copy nueva la cazó `FocusClaimLevelGateTest`: mi cadena decía "no hubo
respuestas que **medir**" y `medir` está en `EFFICACY_CLAIMS_C0` — reescrita a "No registramos ninguna respuesta".
Y de R8: la prescripción escrita (un write-ahead de la administración) es **insuficiente** porque la forma
reservada se consume en el PICK (`nextProbe` avanza el cursor antes de devolver nada), así que la primitiva honesta
es la INVERSA exacta del burn — `releaseProbe` — invocada desde el `finally` del propio run (no desde `abort()`,
que es solo UNO de los tres caminos de fallo: abandono, disposal del scope, throw no capturado) y bajo
`NonCancellable`, porque la compensación corre justo cuando el job que la contiene está siendo cancelado.

**Regla futura:**
Cuando un tipo sellado de rechazo tenga VARIAS causas, un test de retención debe afirmar **la causa tipada**, nunca
el estado de UI que las colapsa: si no, cualquier fixture perezoso lo satisface por la rama equivocada y el guard
es decorativo. Y antes de dar por bueno un fixture que "ejercita" una sesión, pregunta **qué produce si no hace
nada**: si la inacción es a su vez un veredicto legítimo del sujeto, el fixture tiene que actuar de verdad —
conducir la tarea con un reloj atado al tiempo VIRTUAL, porque un reloj constante hace que todo RT sea 0 y caiga
en el suelo de anticipación. Al fabricar una respuesta sintética, **calcula la ventana**: el RT medido debe quedar
por encima del suelo de anticipación y por debajo de la ventana de respuesta, y eso se deriva de las constantes,
no se estima. `LongMethod` de detekt **no cuenta comentarios** (medido): recortar prosa nunca lo arregla, hay que
extraer. Para un recurso escaso que se consume en el PICK, un write-ahead responde "qué iba a pasar" y no
"devuelve lo que cogiste": la primitiva correcta es la inversa exacta del consumo, colocada en el `finally` del
trabajo (que cubre los tres caminos de fallo) y bajo `NonCancellable`; declara su techo en el KDoc de la interfaz
(aquí: el corte es "el bloque terminó", no "se dibujó el primer estímulo"). Y **valida toda fórmula de hash
reproduciendo el valor VIVO antes de estampar uno nuevo**: mi cálculo añadía un `\n` extra al cuerpo y habría
sellado un valor que ningún validador acepta — se detectó porque primero recalculé el sello actual y no coincidía.
Corolarios de entorno, todos ya conocidos y todos vueltos a morder: la herramienta Bash **colapsa la barra doble
dentro de un heredoc citado**, así que un ancla con `\n` escrito así llega como salto real y el `replace` no casa
(constrúyela con `String.fromCharCode(92)`); una mutación que vuelve VERDE (aquí: tomar el bloque de ANTES de la
puerta) **denuncia el comentario, no el código** — el mío afirmaba una protección que `persist` ya daba, y se
corrigió el comentario en vez de inventarle un test; y un `writeLedgerUnderLock` devuelve `{ok, reason}`, así que
ignorar su retorno reporta éxito sobre una escritura rechazada.

**Archivos afectados:**
- `shared/src/commonMain/.../focus/domain/repository/FocusRepositories.kt` · `.../data/local/LocalFocusRepositories.kt` · `.../data/InMemoryProbeBankRepository.kt` (`releaseProbe`)
- `shared/src/commonMain/.../focus/presentation/{FocusBaselineStateHolder,FocusEvaluationStateHolder,RememberFocusStateHolders}.kt` · `.../state/FocusMeasurementUiState.kt` · `.../ui/{FocusBaselineScreen,FocusEvaluationScreen}.kt`
- `shared/build.gradle.kts` (`UNGATED_PROBE_LANES` a `emptySet()`)
- `shared/src/commonTest/.../focus/FocusTestFixtures.kt` (`driveWithResponses`, `virtualMonotonicClock`) · `.../presentation/{FocusBaselineStateHolderTest,FocusEvaluationStateHolderTest}.kt` · `.../evaluation/ProbeBankTest.kt` · `.../data/local/LocalFocusRepositoriesTest.kt`
- `MINDBOOST_AUDIT_LEDGER.md` (`MB-FOCUS-0114` CLOSED; `MB-FOCUS-0117` CLOSED; `0118`/`0119` nuevos)

### 2026-09-03 — Cerrar un item contra lo que TROPIEZAS y no contra su PRESCRIPCIÓN deja fuera justo la mitad que el plan nombraba; y un test que enumera lo que NO cubre documenta el hueco como alcance decidido en vez de levantarlo como deuda

**Error detectado:**
Sellé R10 (accesibilidad de ENFOQUE) como CLOSED en el ledger. Su prescripción escrita decía tres cosas: nodo
semántico en la superficie de respuesta, `contentDescription` en los 13 renderizadores, **y un discriminador no
cromático en E6**. Arreglé el par GO/STOP de E4 —que encontré yo— y los tres controles de glifo, y di la fase por
terminada. E6 seguía intacto: bajo la regla COLOR el TONO decide la respuesta y la forma mostrada al lado es
contradictoria por diseño la mitad de las veces, así que una persona que no resuelve ese tono no puede contestar
aproximadamente **la mitad de los ensayos de cada sesión**, y su fallo se anota como `SWITCH_COST` — la barrera del
instrumento escrita como propiedad cognitiva de la persona. Medido, el par era `FocusError` contra `NeuCyan`: **1.91:1**
de contraste de luminancia. Y el agravante: el guard que yo mismo había escrito **listaba `RuleSwitch` en
`renderedElsewhere`**, es decir dejaba el hueco por escrito sin levantar hallazgo.

**Causa raíz:**
Verifiqué el ENUNCIADO del item (la accesibilidad es real) y no fui item por item por su PRESCRIPCIÓN, así que el
alcance de la fase acabó siendo «lo que encontré leyendo», que se solapa con la prescripción pero no la contiene. Es
la mitad que faltaba de la regla que este fichero ya tenía —verificar enunciado y prescripción **por separado**—:
estaba escrita para el caso en que la prescripción es FALSA, y no para el caso en que es CIERTA y se salta. Y un
`renderedElsewhere` en un test se lee como «esto se decidió fuera de alcance», no como «esto está sin cubrir»: un
test que enumera lo que no cubre convierte deuda en documentación.

**Solución aplicada:**
La dimensión de color de E6 gana un segundo canal, la **LUMINANCIA** (`FocusRuleColorDark` #C4143C: 3.89:1 contra el
valor claro, 3.36:1 contra el lienzo). Se eligió luminancia y no una marca achromatica porque la marca sería una
TERCERA propiedad visual que el usuario tendría que aprender a mitad de una tarea de alternancia, mientras que la
luminancia no obliga a aprender nada: «el rojo» y «el oscuro» son el mismo estímulo. El par vive en **una sola
función** (`ruleColorFill`) que el test consume, para que cambiar cualquiera de los dos fills mueva el test — pinnear
las dos constantes por separado seguiría pasando sobre el par viejo el día que alguien cambie lo que el campo dibuja.
Dos mutaciones: M5 (par histórico restaurado) ROJA solo en el test del par con `1.9135851:1`, M6 (valor oscuro llevado
a casi negro) ROJA solo en el de visibilidad con `1.009245:1` — que es la mitad discriminante y la razón de que el
segundo test exista.

**Regla futura:**
Cierra un item de un plan **contra la lista de su prescripción, punto por punto**, y di en el checkpoint qué punto se
ejecutó, cuál se refutó con medida y cuál se registró con dueño: «arreglé lo que encontré» es un alcance distinto del
que el plan pidió, y la diferencia es invisible desde dentro de la fase. Cuando escribas un test que enumere lo que
**no** cubre, pregúntate si esa lista es alcance decidido o deuda sin registrar; si es deuda, la fila del ledger va en
el mismo cambio que el test. Antes de arreglar un defecto que un informe describe con un alcance amplio («los 13
renderizadores»), **mide cuántos fallan de verdad**: aquí 12 de 13 son contestables sin tono (E10 discrimina por la
IDENTIDAD de la letra, no por su color; E9 por un anillo de 1.8x de radio; E8 por alfa 0.4; E2/E5 por glifo y
posición), así que el arreglo era de un sitio y el enunciado quedó corregido en vez de heredado. Para un
discriminando que debe sobrevivir sin visión del color, el canal es la **luminancia** (WCAG ≥3:1) y no una marca
nueva, siempre que la marca fuera a colisionar con otra dimensión de la tarea; y **comprueba si el arreglo es siquiera
alcanzable calculando, no razonando**: con el rojo original, NINGÚN valor claro llega a 3:1 —ni el blanco puro, que da
2.94— así que oscurecer era la única salida, y eso no se deduce mirando los colores. Todo umbral de contraste lleva su
**contra-presión** en el mismo cambio (aquí: ambos valores ≥3:1 contra el lienzo), o el umbral se satisface llevando
un color a negro y se cambia una barrera por otra.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/design/AppColors.kt` (`FocusRuleColorDark`)
- `shared/src/commonMain/.../focus/presentation/ui/components/FocusRuleSwitchField.kt` (`ruleColorFill`, dueño único del par)
- `shared/src/commonTest/.../focus/presentation/ui/components/FocusFieldAchromaticTest.kt` (+2 tests)
- `MINDBOOST_AUDIT_LEDGER.md` (`MB-FOCUS-0112` P1 CLOSED; `MB-FOCUS-0113` P3 CLOSED, el defecto de proceso)

### 2026-09-03 — Un canal CONTINUO metido en una firma de distinción la vuelve decorativa: el test pasa por una diferencia que nadie puede percibir; y un extractor que sobre-reporta es tan inútil como uno que no lee nada, solo que falla en la dirección que parece diligente

**Error detectado:**
Item R10 del bucket 30d (accesibilidad de ENFOQUE), en sus dos mitades. (1) Extraje la tabla visual de los estímulos
a `FocusFieldVisual.kt` y escribí un guard que exige que todo par de estados dibujados se distinga SIN color. Pasó a la
primera. La mutación M1 —retirar la barra achromática que acababa de añadir al disco STOP— **también pasó**: la firma
llevaba `scale: Float` crudo, y GO (1.0) frente a STOP (1.1) son valores distintos, así que el par se declaraba
distinguible por un **10 % de radio** que se anima en 120 ms mientras el usuario corre contra una señal de parada. Es
decir: el guard existía, corría, y no protegía nada. (2) El guard estructural de la otra mitad (ningún control
dimensiona su propia área táctil con un literal) usaba un extractor que troceaba por líneas en blanco, y eso **fusionó
dos cadenas independientes de una misma función**: una viñeta decorativa `Modifier.size(10.dp)` y, diez líneas más
abajo, un enlace `Modifier.clickable(...)`. El guard "fallaba" sobre dos cadenas que individualmente están bien.
(3) Al reescribir el extractor, `indexOf('(', dot)` se tragaba el resto del fichero: `.clearAndSetSemantics { }` es un
segmento de lambda final y **no tiene paréntesis**, así que la búsqueda seguía hasta el siguiente que hubiera.

**Causa raíz:**
Una firma de distinción responde a una pregunta binaria («¿puede alguien decir que estos dos son distintos?») y un
`Float` responde a otra («¿son numéricamente iguales?»). Meter el segundo en la primera hace que el test se cumpla por
una diferencia real pero **imperceptible**, y como el número es real, no hay nada que huela mal al leer el código.
El caso peligroso es justo el que parece inofensivo: dos estados con tamaños *casi* iguales. Y (2) es la forma
espejo: un extractor de texto se juzga por si lee, y nadie comprueba si lee **de más** — un falso positivo se lee como
rigor.

**Solución aplicada:**
El `scale` crudo sale de la firma y entra `SizeBand { SMALL, MEDIUM, LARGE }`, con los cortes **derivados de la
pregunta que el test hace**, no del rango de valores: Idle (0.55) cae en una banda aparte porque esa diferencia SÍ es
perceptible, y GO (1.0) con STOP (1.1) caen en la misma, que es la verdad. Con eso M1 pone el guard en ROJO con el
mensaje exacto (`Signal and Stop render identically with no colour vision`), y M3 —ensanchar la banda hasta que Idle y
Signal coincidan— lo pone en rojo por el otro extremo, que es la mitad discriminante. `bar`/`innerRing` se declararon
**obligatorios sin default**: un default reinstauraría la fabricación en silencio. Para (2), extractor real con
paréntesis balanceados anclado en `CALL_SEGMENT` (`\.\w+\s*\(`) **en el punto**, de modo que un segmento sin paréntesis
TERMINA la cadena en vez de tragarse el fichero; y un test propio (`theExtractorKeepsTwoChainsInOneFunctionApart`) que
lo corre sobre el caso limpio que la versión ingenua fallaba, para que nadie afloje el troceador el día que el falso
positivo estorbe.

**Regla futura:**
En una firma de igualdad/distinción, **ningún canal continuo**: si el test pregunta «¿se distinguen?», cada campo debe
responder a esa pregunta, así que un `Float` de tamaño/alfa/duración se cuantiza en bandas y **los cortes se derivan de
la percepción, no del rango**. Antes de dar por bueno un guard de pares, pregúntate qué par está más cerca y comprueba
que la mutación que borra la marca de verdad lo pone en ROJO: un guard que pasa a la primera y sobrevive a su propia
mutación es decorativo. Cuando un campo nuevo de una firma sea el que impide fabricar la distinción, decláralo
**obligatorio**, sin default. Todo extractor de texto necesita DOS controles, no uno: anti-vacuidad (¿leyó algo?) y una
**auto-comprobación sobre un caso limpio conocido** (¿leyó de más?) — el segundo es el que falta siempre, y su ausencia
produce hallazgos falsos que parecen trabajo bien hecho. Al parsear cadenas de llamadas en Kotlin, ancla el segmento en
el punto y exige que el `(` pertenezca a ESA llamada: un segmento de lambda final (`.semantics { }`,
`.clearAndSetSemantics { }`) no tiene paréntesis y cualquier `indexOf('(')` salta a otra llamada. Corolario de alcance
que costó la fase entera si se salta: **FASE-0 debe decir qué carril MONTA el hub** — aquí `MindBoostHubScreen` monta
Enfoque V1, no `focus2`, así que la autoridad de accesibilidad (`A11yTargets`, 48dp/44pt) vivía en el carril que NO se
monta mientras el que sí se monta usaba literales de 36dp; duplicar el número habría creado dos dueños del mismo
hecho, así que la salida correcta es MOVERLO a `core/`, y eso es una necesidad del item, no scope creep. Y un guard
TEXTUAL sobre fuente Kotlin de producción **sí** es sólido (reintroducir el defecto cambia el bytecode e invalida la
tarea de test), a diferencia de uno sobre un `.md` o un KDoc: la distinción de 2026-08-28 es sobre el SUJETO, no sobre
la técnica.

**Archivos afectados:**
- `shared/src/commonMain/.../focus/presentation/ui/components/{FocusFieldVisual,FocusGlyphButton,FocusStimulusField,FocusMeasureRunningContent}.kt`
- `shared/src/commonMain/.../focus/presentation/ui/{FocusSessionScreen,FocusHomeScreen}.kt`
- `shared/src/commonMain/.../core/a11y/A11yTargets.kt` (+3 actuals; movido desde `focus2/presentation/a11y`)
- `shared/src/commonTest/.../components/FocusFieldAchromaticTest.kt` · `shared/src/desktopTest/.../focus/presentation/ui/FocusControlTargetTest.kt`
- `MINDBOOST_AUDIT_LEDGER.md` (`MB-FOCUS-0106`..`0108` CLOSED; `0109`..`0111` nuevos)

### 2026-09-03 — Un plan de remediación se ejecuta contra el árbol de HOY, no contra el que auditó: la mitad del bucket ya estaba implementada; y un ancla de splice que empieza por `\n` se COME el terminador de la fila anterior

**Error detectado:**
Ejecución del bucket 0–72 h (§12 R1–R5) de la auditoría adversarial de ENFOQUE. Cuatro cosas. (1) Dos de
los cinco ítems —R2 (gate de bloque sin respuestas) y R5 (escáner de calidad en `check`)— **ya estaban
implementados**, y mejor que mi prescripción, por la sesión paralela `FOC-P0-04`/`FOC-P0-05`; R1 estaba a
medias por `FOC-P0-00`. Implementarlos habría creado **dos dueños del mismo invariante**. (2) Al splicear
las filas nuevas del ledger usé el ancla `'\n## 3. …'` y `out.replace(anchor, block + '\n' + anchor)`: como
**no hay línea en blanco** entre la última fila de §2 y la cabecera `## 3.`, el ancla se llevó el salto de
línea que TERMINABA la fila de `MB-FOCUS-0101`, y mi primera fila quedó pegada a la suya; el guard de
escritura saltó con `double pipe in table row`. (3) Sellé un evento que ponía `MB-FOCUS-0089` en
`MITIGATED` y dejé la fila del REGISTRO en `OPEN`: `validate-audit-ledger.mjs` falló con
`register/event contradiction`. (4) Un lote de ediciones abortó con `anchor count 0` porque
`TransferReport.kt` es CRLF y mis anclas eran LF — dentro del MISMO paquete conviven los dos finales de
línea (`TransferReport`/`FocusProgressScreen`/`FocusHomeStateHolder`/`FocusHomeScreen` CRLF; el resto LF).

**Causa raíz:**
(1) Un plan de remediación describe el árbol que auditó, no el de hoy; con una sesión paralela trabajando
el mismo worktree, sus premisas caducan en días. (2) Un ancla que EMPIEZA por `\n` no marca «antes de la
cabecera»: consume el terminador de la línea anterior, así que insertar «antes del ancla» sin reponerlo
concatena dos filas. Con tablas markdown eso produce `||`, que es exactamente el defecto de 2026-08-29
llegando por una vía nueva —no por el contenido de una celda, sino por el splice—. (3) El validador lee el
REGISTRO como estado materializado y el evento como transición: escribir solo uno de los dos es una
contradicción, no un olvido cosmético. (4) El EOL es propiedad del FICHERO, no del paquete ni del módulo.

**Solución aplicada:**
(1) **FASE-0 por ítem antes de tocar nada**: R2 y R5 no se ejecutaron y su premisa se registró como FALSA
en el informe (§21.3) con el porqué; R1 se ejecutó solo en lo que quedaba (9 KDoc/comentarios que el gate
de literales de producción no escanea). (2) `out.replace(anchor, '\n' + block + anchor)` —el salto se
REPONE al principio del bloque— y el guard de `||` se conservó, porque es lo que lo cazó al ESCRIBIR en vez
de al validar. (3) La fila de §2 pasó a `MITIGATED` con su justificación, en la misma escritura bajo lock, y
se recomputó el SHA. (4) Helper `toEol(s, crlf)` aplicado **por fichero** (`src.includes(CR+LF)`), con las
constantes construidas por `String.fromCharCode(13/10)` para que ni el heredoc ni `node -e` las degraden.

**Regla futura:**
Antes de ejecutar un ítem de un plan de remediación escrito hace más de un día, **verifica su premisa contra
el árbol de HOY**: en un worktree compartido lo más probable no es que el defecto siga igual, sino que otra
sesión ya lo haya cerrado —y reimplementarlo no es trabajo redundante, es un segundo dueño del mismo
invariante—. Deja constancia de la premisa refutada, que es tan resultado como el código. Cuando splicees
contenido delante de un ancla que empieza por `\n`, **repón el salto al principio del bloque**
(`'\n' + block + anchor`), y comprueba si el ancla y lo anterior están separados por una línea en blanco
antes de decidir; el guard de `||` a la escritura es lo que convierte ese fallo en un error inmediato en vez
de en un registro corrupto. Todo cambio de estado en el ledger son **DOS filas** (registro §2 + evento §3)
escritas en la MISMA operación, o el validador lo rechaza —y hace bien—. Y no asumas el EOL por paquete:
detéctalo por fichero antes de casar cualquier ancla multilínea. Corolario que volvió a pagar: mi propio
guard anti-falso-verde abortó el sellado con `gate task did NOT execute (UP-TO-DATE): desktopTest`, así que
la evidencia se ganó con `:shared:desktopTest --rerun` (619 XML frescos, 4415/0F/0E) y el script aprendió a
qué log pertenece cada tarea de gate, en vez de buscarlas todas en uno.

**Archivos afectados:**
- `shared/src/commonMain/.../focus/**` (9 KDoc/comentarios + 1 error factual + retirada de «Semana N de 8» + 2 rótulos) · 3 comentarios de test
- `docs/audits/AUDITORIA_ADVERSARIAL_ENFOQUE_2026-08-29.md` (§21 Nota de vigencia) · `MINDBOOST_AUDIT_LEDGER.md` (`MB-FOCUS-0089` OPEN → MITIGATED; `0102`..`0105` nuevos)

### 2026-09-02 — La herramienta Bash colapsa `\\` a `\` dentro de un heredoc citado: un regex Kotlin escrito así en `build.gradle.kts` rompe la CONFIGURACIÓN de Gradle y vuelve inatribuible toda la tanda; y un arnés de mutación que aborta cuando el ancla no casa exactamente una vez es lo que protege el árbol

**Error detectado:**
Fase `FOC-P0-05` U3. (1) Escribí la tarea Gradle `focusQualityGateScanner` con un heredoc `<<'EOF'` de la herramienta
Bash que contenía `Regex("admission\\s*=(?!=)\\s*SessionAdmission\\.Admitted")`; en disco quedó `\s`/`\.` y la
configuración de Gradle falló con `Unsupported escape sequence` en `shared/build.gradle.kts:1103`. Las SEIS corridas de
mutación del scanner que lancé sobre ese estado fallaron todas con ese mismo error, no por la regla que decían probar:
ninguna era atribuible y hubo que repetirlas. Antes, un comando Bash con varios heredocs `<<'EOF'` seguidos ni siquiera
parseó. (2) Dos anclas de mutación no casaron: `"import "` (31 apariciones) y una con `\r\n` sobre un fichero LF; el
arnés exige exactamente 1 aparición y abortó ANTES de escribir, así que el árbol quedó intacto. (3) La primera corrida de
la mutación B1 puso 16/22 tests en rojo, incluidos tests independientes de la rama mutada; una repro aislada dio 1 rojo
y dos repeticiones sobre las 5 suites dieron exactamente los 5 esperados.

**Causa raíz:**
(1) La herramienta Bash procesa el texto del heredoc antes de que el shell lo vea: una barra doble llega como barra
simple aunque el delimitador vaya entre comillas simples. El fichero resultante rompe la CONFIGURACIÓN de Gradle, que
precede a cualquier tarea, así que toda tarea lanzada después falla en segundos con el mismo mensaje y ningún resultado
de esa tanda habla de lo que se quería probar. (2) Un ancla textual es sensible al final de línea: una que contenga
`\r\n` jamás casa en un fichero LF, y una palabra corriente casa demasiadas veces. (3) Una corrida de mutación puede
arrastrar estado de la compilación concurrente de la sesión vecina y producir rojos que no dependen de la mutación.

**Solución aplicada:**
(1) Reparé el fichero con un script Node que restauró las barras dobles (`String.fromCharCode(92)`), verifiqué la línea
con `Select-String` y repetí las 5 mutaciones del scanner: 5/5 ROJO con el mensaje exacto de su regla, baseline y final
verdes, restauraciones por SHA-256. (2) Anclas de una sola línea y únicas (`package …` más un comentario marcador), y el
arnés siguió abortando si el conteo no es exactamente 1. (3) La primera corrida de B1 se declaró INCONCLUSA y la
atribución se hizo con las dos repeticiones consistentes.

**Regla futura:**
Nunca escribas Kotlin, Groovy ni regex con barras invertidas a través de un heredoc de la herramienta Bash: usa la
herramienta Write, el bundle `apply-bundle.mjs` o un script Node; y tras escribir cualquier fichero que deba contener
`\\`, comprueba con `Select-String -SimpleMatch`/`grep -F` que las barras dobles siguen ahí ANTES de lanzar nada. Si
TODAS las tareas Gradle de una tanda fallan con el mismo error en segundos, es un fallo de configuración del build
script: no leas ningún resultado de esa tanda como atribución, arregla el script y repite la tanda entera. Las anclas de
mutación van en una sola línea, únicas y en el EOL del fichero objetivo; conserva un arnés que aborte antes de escribir
cuando el conteo de anclas no sea exactamente 1 y comprueba el SHA del fichero REAL tras cada restauración. Un resultado
de mutación que ponga en rojo tests independientes de la rama mutada es INCONCLUSO: se repite hasta obtener dos corridas
consistentes y solo entonces se atribuye.

**Archivos afectados:**
- `shared/build.gradle.kts` (reparado dentro de la misma fase; arnés de verificación, sin producción adicional)

### 2026-09-02 — El hook de PowerShell bloquea CUALQUIER comando que combine `Remove-Item` con un `//` en su texto (también un comentario Kotlin pasado como argumento); y `"$var: texto"` es un error de parseo, no una interpolación

**Error detectado:**
Fase `FOC-P0-03`. (1) Un único comando PowerShell que hacía tres cosas —sustituir un comentario `// FOC-P0-03 …`
en un `.kt` vía Node, borrar `detekt.xml` con `Remove-Item` y lanzar detekt— fue BLOQUEADO por el hook con
`Remove-Item on system path '//' is blocked`: el hook no analiza qué se borra, casa el token `Remove-Item` con
cualquier `//` presente en el texto del comando, y el `//` estaba en el comentario Kotlin que iba como argumento
de la sustitución. (2) El arnés de mutación falló al PARSEAR (no ejecutó nada) por `"$className: NO FRESH XML"`:
PowerShell lee `$className:` como una variable con ámbito/unidad (`$drive:name`) y aborta con
`InvalidVariableReferenceWithDrive`.

**Causa raíz:**
(1) El guard es textual y advisory sobre el comando completo, no sobre la ruta que recibe `Remove-Item`; un `//`
en cualquier argumento lo dispara. (2) En una cadena entre comillas dobles, `$nombre:` es sintaxis de variable
calificada; solo `${nombre}:` delimita el nombre.

**Solución aplicada:**
(1) Borrar el informe con `[System.IO.File]::Delete((Resolve-Path …).Path)` en vez de `Remove-Item` cuando el mismo
comando lleve texto con `//` (o separar el borrado en otra llamada). (2) `"${className}: …"`. El arnés se relanzó
completo: las cuatro mutaciones quedaron atribuidas 1-a-1 y las restauraciones verificadas por SHA-256.

**Regla futura:**
En una llamada PowerShell que incluya `Remove-Item`, no metas texto con `//` (comentarios de código, URLs) en el
mismo comando: usa `[System.IO.File]::Delete` o parte la llamada. Y toda interpolación seguida de dos puntos va
como `${var}:`. Un error de PARSEO de PowerShell no ejecuta ninguna línea —comprueba con los SHA que el árbol sigue
intacto antes de relanzar—, a diferencia de un fallo en mitad del script, que deja la mutación puesta.

**Archivos afectados:**
- (ninguno de producción — arnés de verificación)

### 2026-09-02 — En PowerShell `$D` y `$d` son LA MISMA variable: un arnés de mutación que asigna `$d = [xml]…` a mitad de bucle redirige la restauración y la mutación siguiente a un fichero espurio; y `java.exe` expande comodines de `--tests` contra los ficheros del cwd

**Error detectado:**
Fase `FOC-P0-04`. (1) Mi bucle PowerShell de mutación usaba `$D` para la ruta del motor y, dentro del mismo
bucle, `$d = [xml](Get-Content …)` para leer el XML de resultados. PowerShell es case-insensitive en nombres de
variable, así que la segunda asignación PISÓ la ruta: el `Copy-Item backup $D` «restauró» el motor escribiendo
un fichero llamado literalmente `System.Xml.XmlDocument` en la raíz del repo, y la mutación B se aplicó a ESE
fichero mientras el motor real seguía con la mutación A puesta. El síntoma que lo delató: la mutación B puso en
ROJO el MISMO test que la A (el del bloque silencioso) en vez del suyo (el del coste dual) — una mutación que no
cambia de test es una mutación que no se aplicó. (2) Antes, `--tests "*Rci*"` hizo fallar Gradle en 1 s con
`Task 'MINDBOOST_EXERCISE_AUXILIARIES.csv' not found`: el lanzador `java.exe` de Windows expande comodines de
argumento contra los ficheros del cwd, y `*Rci*` casó (sin distinguir mayúsculas) con `…EXERCISE…csv`.

**Causa raíz:**
(1) Nombres de variable case-insensitive + un bucle que mezcla dos convenciones (`$D` ruta, `$d` documento).
Nada avisa: la asignación es legal y el `Copy-Item` a una ruta relativa inexistente CREA el fichero. (2) La
expansión de comodines la hace el lanzador de la JVM, no PowerShell, así que las comillas no protegen; cualquier
patrón sin punto puede casar con un nombre de fichero del directorio de trabajo.

**Solución aplicada:**
Se eliminó el fichero espurio tras comprobar que era una copia del backup (no ajeno), se restauró el motor real
desde el backup del scratchpad con verificación SHA-256 (2/2 OK) y se repitió la mutación B con variables de
nombre largo y distinto (`$enginePath`, `$xmlPath`, `$report`): ROJO exactamente en el test del coste dual.
Los patrones de `--tests` pasaron a la forma con paquete (`*.metrics.FocusIndexHonestyTest`), que no puede casar
con un fichero.

**Regla futura:**
En scripts PowerShell NUNCA uses dos variables que solo difieran en mayúsculas: son la misma. Usa nombres largos
y distintos para ruta-de-fichero y documento-parseado, sobre todo dentro de un bucle de mutación/restauración.
Tras cada restauración comprueba el SHA del fichero REAL (no el éxito del `Copy-Item`) y lista la raíz del repo
buscando ficheros espurios (`System.Xml*`, nombres de tipo .NET). Y trata «la mutación B puso en rojo el mismo
test que la A» como prueba de que B NO se aplicó, no como cobertura redundante. Para `--tests`, usa siempre un
patrón con el paquete (`*.paquete.Clase`), nunca `*Palabra*` a secas: `java.exe` lo expande contra el cwd.

**Archivos afectados:**
- (ninguno de producción — arnés de verificación; el fichero espurio `System.Xml.XmlDocument` fue eliminado)

### 2026-09-01 — `git commit` sin pathspec barre TODO el índice: en un árbol compartido, los renames que otra sesión dejó pre-staged entran en TU commit atómico

**Error detectado:**
Al crear el commit atómico de `FOC-P0-01` (7 ficheros nuevos) con `git add <7 rutas>` + `git commit -F msg`, el
commit resultante (`31a7996`) contenía **10** ficheros: los 7 míos más tres renames `R100` (`Probit.kt`,
`SsrtCalculator.kt`, `SsrtCalculatorTest.kt`) que otra sesión había dejado **staged** en el índice y que no
formaban parte de mi PR.

**Causa raíz:**
`git commit` sin pathspec commitea el índice ENTERO, no «lo que acabo de añadir». En un worktree compartido el
índice puede llevar cambios ajenos ya staged (aquí `RM`), invisibles si solo se mira `git status` en busca de
`??`/` M`. `git add` de mis ficheros no los aisló: solo los sumó a lo que ya estaba.

**Solución aplicada:**
`git reset --soft HEAD~1` (deshace el commit, conserva el índice) y recommit con pathspec explícito:
`git commit -F msg -- <7 rutas>`, que commitea SOLO esas rutas y deja el resto del índice intacto. Verificado
con `git show --name-status HEAD` (exactamente 7 `A`) y con `git status` (los 3 renames ajenos siguen `RM`).

**Regla futura:**
En este árbol, TODO commit se hace con pathspec explícito (`git commit -- <rutas>`), nunca a índice completo, y
se verifica con `git show --name-status HEAD` que el número de entradas es EXACTAMENTE el de la PR antes de dar
el commit por bueno. Antes de commitear, mirar también las columnas de índice de `git status --porcelain`
(`A`/`M`/`R` en la PRIMERA columna): son cambios ajenos que un commit sin pathspec se llevaría. Si un commit
salió con ficheros de más y no se ha hecho push, `reset --soft` + recommit acotado; jamás `reset --hard`.

**Archivos afectados:**
- (ninguno de producción — disciplina de commit; commits `31a7996` → `bdc252f`)

### 2026-08-29 — Un `||` dentro de una celda de tabla markdown parte la fila, y el validador lee la MITAD de una frase como si fuera un ESTADO

**Error detectado:**
Al sellar el ledger con siete hallazgos nuevos, `validate-audit-ledger.mjs` falló con
`invalid state "CORRECT_REJECTION` … `" for MB-FOCUS-0091` y, a continuación, con una
`register/event contradiction` sobre ese mismo hallazgo. El texto de la fila citaba código Kotlin real:
el operador de disyunción entre `HIT` y `CORRECT_REJECTION`.

**Causa raíz:**
En una tabla markdown el pipe es separador de columnas, y dos pipes seguidos producen **dos** separadores: la
fila gana dos columnas y todo se desplaza. El validador lee el estado por POSICIÓN (`cells[5]`), así que pasó a
leer un trozo de la prosa como si fuera el estado del hallazgo — y como el evento de §3 sí decía `OPEN`, el
segundo error («contradicción registro/evento») era un SÍNTOMA del primero, no un defecto independiente. Es la
misma familia que el aprendizaje de 2026-07-20 (un pipe escapado dentro de una celda) pero por una vía nueva:
aquí el pipe no venía de una notación matemática sino de **un operador del lenguaje citado literalmente**, que
es justo lo que uno escribe sin pensar al describir un defecto de código.

**Solución aplicada:**
Dos spans de código separados por prosa (`` `HIT` `` o `` `CORRECT_REJECTION` ``) en vez de un único span con el
operador dentro. Guard añadido al script de escritura: si el texto resultante contiene dos pipes seguidos,
lanza — de modo que la siguiente fila que lo intente falla al ESCRIBIR en vez de fallar al validar.

**Regla futura:**
Nunca escribas un pipe (ni dos) dentro de una celda de una tabla markdown que un validador vaya a trocear por
posición, **tampoco entre acentos graves**: el backtick no escapa nada para un `split` por pipe. Al citar código
en el ledger, parte los operadores que contienen pipes en spans separados unidos por prosa. Y cuando el
validador reporte DOS errores sobre el mismo ID —uno de estado inválido y otro de contradicción
registro/evento— **arregla solo el primero y revalida**: el segundo suele ser su sombra, y «arreglarlo» tocando
la fila de §3 introduciría un defecto real donde no lo había.

**Archivos afectados:**
- `MINDBOOST_AUDIT_LEDGER.md` (fila `MB-FOCUS-0091`)

### 2026-08-29 — `null != 0` en su dirección de MÁXIMO: la ausencia viaja en un centinela cuyos campos se leen como impecables; fail-closed sobre el VALOR no es fail-closed sobre el HECHO, y un parámetro POR DEFECTO es un vector que un guard textual por fichero NO puede ver

**Error detectado:**
Disposición de `MB-FOCUS-0071`, el último hallazgo de Focus V5 abierto con dueño. La fila decía que la sonda de
Velocidad conservaba el `?: DEFAULT_REFRESH_HZ` que `MB-FOCUS-0069` acababa de retirar de focus2, y que el guard
de honestidad estaba acotado por NOMBRE DE FICHERO y por eso no podía verla. **Las dos mitades resultaron
literalmente CIERTAS** —lo raro, después de tres fases seguidas refutando enunciados— pero su PRESCRIPCIÓN
(«replica el `?:` de focus2») era incompleta y el defecto real era MAYOR de lo enunciado: `QualitySignals` no
transportaba `sampledFrames` en absoluto, así que Velocidad **no tenía NINGÚN sample gate** — justo la trampa que
el KDoc de `core.timing.TemporalClassPolicy`, en el mismo paquete, nombra como su guarda portante nº1. Cadena
viva medida: medición ausente → `FrameStats.UNAVAILABLE` (que lleva `frameDropFraction = 0.0` y
`criticalFrameJitter = false`) → `deviceTimingRisk = LOW` → no contaminada → `CHOREOGRAPHER_FRAME` →
**`SessionQuality.HIGH`**, el rating SUPERIOR fabricado desde la ausencia. Y de camino, el KDoc de
`FrameTimingProbe` AFIRMABA que la ausencia estaba protegida por el clasificador: falso dos veces
(`TimingPolicyClassifier` no inspecciona `sampledFrames`, y no tiene un solo call-site de producción).

**Causa raíz:**
Un centinela de ausencia (`UNAVAILABLE`) es honesto como TIPO y deshonesto como CARGA: sus campos numéricos
llevan los valores que un dispositivo impecable produciría, así que cualquier umbral evaluado sin preguntar antes
«¿se midió algo?» lee la ausencia como perfección. Es `null != 0` en su dirección cara —la que INFLA— y su
particularidad es que el productor no miente: falta una PREGUNTA en el consumidor. La segunda causa es de
simetría: el proveedor Android rellenaba la ausencia (`?: 60.0`) mientras iOS la declaraba honestamente, y dos
adapters que fallan distinto no son comparables aunque ambos compilen.

**Solución aplicada:**
DOS mitades **conjuntamente necesarias**, y el red team probó que ninguna sola habría cambiado nada: antes de la
fase el conjunto {`UNAVAILABLE`} ∩ {`HIGH`} era **vacío** (donde la ausencia estaba garantizada —desktop/iOS— el
`source` ya impedía HIGH; donde HIGH era alcanzable —Android— el fallback impedía la ausencia). (1) El proveedor
pasa a `() -> Double?` sin fallback, lo que **CREA** la ausencia honesta en Android; (2) `QualitySignals.sampledFrames`
OBLIGATORIO (sin default, que reinstauraría en silencio la fabricación) + sample gate ANTES de los umbrales, que
**IMPIDE** que esa ausencia recién declarada se lea como dispositivo impecable. Dirección fail-closed estricta:
retener HIGH, **jamás acusar** — ni una carga de frames imposible (drops y jitter sin un solo frame observado)
convierte la ausencia en `CONTAMINATED`. Se cerró además un vector que yo no había visto: `fromFrameTimes` tenía
`refreshHz: Double = DEFAULT_REFRESH_HZ`, así que un futuro llamante podía re-fabricar el nominal de 60 Hz **sin
nombrar nunca la constante en su propio fichero**, invisible para un guard textual por fichero. El guard se
amplió **en el fichero que YA era dueño** del invariante (`WindowedTemporalProbeCadenceHonestyTest`), no en uno
nuevo: «que no puede verla» ERA la razón registrada de su P1, así que cerrar el hallazgo exigía quitarle
justamente esa ceguera. Cuatro correcciones más al ADR salieron del gate adversarial, no de mí.

**Regla futura:**
Un centinela de «no se midió» cuyos campos sean numéricos DEBE llevar el conteo de muestra al consumidor, y el
consumidor DEBE preguntarlo **antes** que cualquier umbral: si el centinela viaja con `0.0`/`false`, todo umbral
evaluado antes del sample gate fabrica perfección. Pregunta siempre **en qué dirección** fabrica la ausencia: el
charter vigila el 0 fabricado, pero la misma ausencia metida en una comparación de penalización produce el
MÁXIMO. Distingue **fail-closed sobre el VALOR** de **fail-closed sobre el HECHO**: crear la ausencia (proveedor
nullable) y manejarla (gate del consumidor) son dos arreglos distintos y casi siempre hacen falta LOS DOS —
comprueba explícitamente si tu mitad sola cambia algo en alguna plataforma, porque «vacío hoy» no es «arreglado».
**Y cuando cierres un defecto de la forma «la ausencia fabrica X», enumera TODOS los consumidores de esa ausencia
dentro de la MISMA función antes de declarar el cierre.** Aquí el sample gate protegió `sessionQuality` y dejó
`deviceTimingRisk` —derivado siete líneas MÁS ARRIBA, antes del gate— publicando el mismo valor halagador
(`DeviceTimingRisk.LOW`) desde la misma ausencia, y renderizado al usuario una línea POR DEBAJO de la que yo
acababa de arreglar (`SpeedExerciseScreen:464` frente a `:463`). Lo encontró el gate adversarial, no yo, y la
prueba de que mis tests no lo cubrían estaba en mis propios tests: pinneaba `deviceTimingRisk` para el caso
MEDIDO y nunca para el no medido. Cerrar así habría sido «documentado como arreglado y no arreglado». Corolario
de orquestación: **el gate de salida se corre DESPUÉS de darte la fase por terminada y ANTES de sellar el
ledger** —sellar primero convierte un hallazgo refutable en una fila append-only— y se corre con un `agentType`
SIN Bash (`architect`), que es el mecanismo de `MB-FOCUS-0083`: una prohibición en el prompt no es un control.
De sus 9 agentes, 3 objeciones bloqueantes y 5 correcciones desmintieron mi propio trabajo: un quinto defecto que
mi §8 no contenía, una severidad que yo había BAJADO sin reconciliar (un dictamen C4 previo ya fijaba ese
hallazgo en P1 y lo marcaba `ALREADY WIRED`), y una prescripción que apuntaba al mecanismo equivocado (creía que
lo que hace honesta a la lane hermana es leer la cadencia POR VENTANA, y `drainWindow()` drena en `:71` y lee el
proveedor en `:79` —igual de retroactivo—; lo que la protege es `cadenceResolved`, que se NIEGA a clasificar
cuando la cadencia declarada y la detectada discrepan). **Antes de asignar severidad a un hallazgo, busca si el
repo ya lo registró**: bajar un P1 previo sin medirlo es re-asignar, no reconciliar.
Trata **todo parámetro por defecto de un valor de referencia** como un vector de re-fabricación que ningún guard
textual por fichero puede ver: hazlo obligatorio si todos los call-sites ya lo pasan (coste cero, invariante
ganado). Cuando el hallazgo diga «el guard no puede ver esto», el cierre exige **ampliar ese guard**, y se amplía
EN EL FICHERO QUE YA ERA DUEÑO del invariante: dos guards del mismo invariante son dos dueños. Y verifica el
ENUNCIADO y la PRESCRIPCIÓN por separado incluso cuando el enunciado resulte cierto — aquí lo era, y seguir la
prescripción al pie habría dejado el defecto mayor intacto. Corolarios de entorno, uno NUEVO y tres que
volvieron a morder: (1) **`NoSuchFileException: in-progress-results-*.bin` con TODOS los XML frescos no es una
corrida perdida**: la suite escribió sus 610 XML y el fallo llegó después, al consumir el binario que la sesión
vecina ya había limpiado — y la salida obvia (borrar `test-results`) habría destruido evidencia ajena, que es
`MB-FOCUS-0083` visto desde el otro lado; (2) la evidencia que ATRIBUYE una corrida no es qué invocación la
lanzó sino **si sus XML nombran tus tests nuevos** — los míos aparecían por nombre, así que esa suite compiló mis
fuentes, cuyo SHA-256 no se había movido; (3) `Could not delete '...classes/kotlin/desktop/main/com'` +
`Daemon compilation failed: null` con **cero errores `e: file:`** es corrupción concurrente: se re-ejecuta, no se
cambia una línea, y se ATRIBUYE por mtime (la vecina tocó tres ficheros de `logic/` DENTRO de mi ventana);
(4) una huella `git status --porcelain` idéntica antes y después NO prueba árbol quieto —no ve cambios de
CONTENIDO ni ediciones a ficheros ya listados— así que hay que cruzarla con mtimes siempre.

**Archivos afectados:**
- `shared/src/androidMain/.../core/timing/FrameTimingProbe.android.kt` (proveedor nullable, fallback borrado)
- `shared/src/commonMain/.../core/timing/{FrameTimingProbe,FrameStatsCalculator}.kt` (KDoc falso corregido; `refreshHz` obligatorio en los DOS overloads)
- `shared/src/commonMain/.../speed/domain/quality/SessionQualityEvaluator.kt` (`sampledFrames` obligatorio + sample gate + `deviceTimingRisk` derivado POR DEBAJO del gate: segundo canal) · `.../speed/domain/MetricThresholds.kt` (`FRAME_TIMING_UNMEASURED`)
- `shared/src/commonMain/.../speed/presentation/SpeedSessionStateHolder.kt` · `.../speed/simulation/SyntheticUserRunner.kt`
- `shared/src/commonTest/.../speed/domain/quality/SessionQualityEvaluatorTest.kt` (+4, la última del segundo canal) · `shared/src/desktopTest/.../core/timing/{WindowedTemporalProbeCadenceHonestyTest,FrameStatsCalculatorTest}.kt` (+3, alcance ampliado en sitio)
- `docs/audits/ADR_SPEED_FRAME_HONESTY_2026-08-29.md` (nuevo, con §7 de autocorrecciones y §8 de hallazgos nuevos) · `MINDBOOST_AUDIT_LEDGER.md` (`MB-FOCUS-0071` CLOSED; `MB-FOCUS-0085`..`0088` nuevos)

### 2026-08-29 — Un test que solo afirma DESIGUALDADES no pinnea una fórmula: el bug sobrevivió porque medía el SIGNO y no la MAGNITUD; y una regla escrita en este mismo fichero no barre el resto del fichero que arregló

**Error detectado:**
`MB-EFFICACY-0002`, elegido por severidad y no por vecindad temática. El bloque de penalización de
`DefaultReasoningScorer` escribía `W1 * if (impulsive) 1.0 else 0.0 + W2 * if (conceptual) 1.0 else 0.0` **sin
paréntesis**. En Kotlin la rama `else` se extiende todo lo posible, así que eso parsea como
`W1 * (if (impulsive) 1.0 else (0.0 + W2 * (if (conceptual) 1.0 else 0.0)))`. Medido con el scorer real sobre los
fixtures del repo, dos de las cuatro celdas de la tabla de verdad estaban mal: un error conceptual solo valía
**1.5 puntos en vez de 15** (los dos pesos se MULTIPLICABAN, 0.10 × 0.15), y **desaparecía por completo** cuando
la respuesta era además impulsiva (25 esperados, 10 reales). Las dos desviaciones van en la dirección que **INFLA**
la puntuación. Y el dato más incómodo: la regla de precedencia ya estaba escrita en este mismo fichero
(2026-06-15) y el `weightedBase` **diez líneas más arriba** ya llevaba sus paréntesis — la regla se aplicó al sitio
que la generó y nadie barrió el resto del fichero.

**Causa raíz:**
No fue falta de tests: el scorer tenía seis aserciones y **todas eran DESIGUALDADES** («el impulsivo puntúa menos
que el no impulsivo», «un error conceptual resta»). La versión inflada las satisface TODAS, porque la penalización
seguía existiendo — solo que valía una décima parte. Un test de ordenación mide el SIGNO de un término; una fórmula
con términos aditivos necesita que alguien mida su MAGNITUD, y eso solo lo hace una IGUALDAD. Por eso el defecto
podía vivir indefinidamente con la suite en verde.

**Solución aplicada:**
Dos pares de paréntesis y un comentario que nombra el peligro (el `weightedBase` ya era el precedente). El guard
nuevo va **en el fichero que ya era dueño** del scorer y afirma la **ADITIVIDAD** de los dos términos
(`neither − both == (neither − onlyImpulsive) + (neither − onlyConceptual)`), deliberadamente **independiente de los
pesos**: repesar es decisión de producto, perder un término por precedencia es un defecto, y pinnear los literales
habría hecho el test frágil ante la decisión legítima y ciego ante el defecto. El aislamiento del término conceptual
se hace copiando la MISMA opción con su etiqueta de distractor quitada, así que identidad, posición, corrección y
toda la base de justificación quedan fijas y lo único que puede mover la puntuación es el bloque de penalización.
Anti-vacuidad obligatoria: cada penalización debe valer algo y ninguna celda puede tocar el clamp inferior (si una
llega a 0 el clamp recorta las diferencias y la igualdad se cumple por la razón equivocada). El fallo se reprodujo
con el scorer real **antes** de tocar producción (`separately 10.0 + 1.5, together 10.0`), y dos mutaciones
dispararon **aserciones distintas del mismo test**: M1 (defecto exacto reintroducido) cayó en la aditividad con los
números originales; M2 (bloque anulado) cayó en la anti-vacuidad — la prueba de que el guard anti-vacuidad no es
decorativo.

**Regla futura:**
Cuando un test proteja una **fórmula con términos aditivos**, una batería de desigualdades no la pinnea: afirma una
**igualdad sobre la magnitud** (aditividad, o el valor exacto de la contribución de cada término), porque un término
que vale la décima parte de lo que debería sigue satisfaciendo todas las ordenaciones. Y hazla **independiente de
los parámetros que producto puede cambiar legítimamente** — un guard que pinnea los pesos se rompe con una decisión
de negocio y no se rompe con el defecto. Cuando escribas una regla en este fichero a raíz de un fallo puntual,
**barre el fichero entero (y el árbol) buscando el mismo patrón antes de darla por aplicada**: aquí la regla llevaba
dos meses escrita, el sitio de al lado corregido, y el defecto vivo diez líneas más abajo. **Mide el alcance en las
DOS direcciones antes de asignar la severidad**: hacia arriba, el barrido por dos métodos independientes probó que
esta era la ÚNICA ocurrencia absorbente del árbol (las demás llevan el `if` como último operando, donde no hay nada
que absorber); hacia abajo, el defecto **no** contamina el perfil cognitivo porque `LogicProfileSource` es
fail-closed en su primera línea — los consumidores vivos son el bucle adaptativo y la copy embarcada, que es una
afirmación más pequeña y verdadera en lugar de una grande y falsa. Corolario que refina una regla propia:
**`FROM-CACHE` en una tarea de COMPILACIÓN no invalida un gate cuya tarea de TEST sí corrió sin sufijo.** Mi regla
de 2026-08-28 dice que `FROM-CACHE` es tan «no corrió» como `UP-TO-DATE`, y aplicada sin distinguir habría anulado
una corrida buena: lo que hay que exigir es que **la tarea que ES el gate** aparezca sin sufijo y que sus artefactos
caigan dentro de la ventana — aquí 610 XML **todos** frescos, 4348/0F/0E, con el +1 sobre los 4347 previos
atribuible exactamente al guard nuevo. Y un corolario que la fase encontró al medir su propio
alcance: **arreglar un cálculo no limpia lo que ese cálculo ya escribió en disco.** Aquí
`LogicAttempt.scoreBreakdown` y `LogicSession.result.averageIcr` persisten el ICR inflado en hasta 30 sesiones por
usuario, y dos pantallas vivas lo leen; se registró como hallazgo con las dos salidas medidas (recomputar desde los
campos ya guardados, que conservan los ocho sumandos por separado, o declarar que la ventana se purgue sola) sin
implementar ninguna, porque eso es trabajo de otra fase. Lo inaceptable no es dejarlo purgar: es no decidirlo.

**Archivos afectados:**
- `shared/src/commonMain/.../logic/domain/scoring/ReasoningScorer.kt` (dos pares de paréntesis + comentario)
- `shared/src/commonTest/.../logic/domain/scoring/ReasoningScorerTest.kt` (+1 guard weight-independent, 6 → 7 tests)
- `MINDBOOST_AUDIT_LEDGER.md` (`MB-EFFICACY-0002` CLOSED con el alcance precisado en ambas direcciones; `MB-EFFICACY-0017` nuevo: historico persistido contaminado)

### 2026-08-29 — Una UNIDAD no es una ESCALA: la prescripción de un hallazgo puede ser peor que su defecto; `roundToInt()` es la forma REDONDEO de `null != 0`; y un guard de CONTEO no valida la ATRIBUCIÓN

**Error detectado:**
Fase R14 del catálogo, sobre `MB-CATALOG-0024` —tercer hallazgo consecutivo escrito por mí—. Los **5 símbolos
resultaron CIERTOS** y el defecto era real y vivo: `TransferRow` pintaba `delta.roundToInt()` en dos pantallas,
así que con los **fixtures canónicos del propio repo** una mejora de precisión de `0.80 → 0.82` se renderizaba
`+0` y una caída de variabilidad de `0.30 → 0.20` (33 % menos) se renderizaba `0` — indistinguible de un cero
real, porque `Int` no tiene cero negativo y el `sign` solo añadía «+». Pero la **PRESCRIPCIÓN que yo mismo
había escrito era falsa y además dañina**: decía «formatear por unidad», y medido sobre el motor `MetricUnit`
**no lleva escala** — `INDEX` cubre 0..100 (`SUSTAINED_FOCUS_INDEX`) y 0..1 (`BLOCK_STABILITY`,
`DISTRACTION_RESISTANCE_INDEX`, `FATIGUE_INDEX`), y `RATIO` cubre 0..1 (`RT_CV`) y aciertos/segundo sin cota
(`RATE_CORRECT_SCORE`). Aplicarla habría multiplicado el foco sostenido por 100 y mostrado un cambio de 3
puntos como **«+300»**. Y el alcance volvió a estar INFRAVALORADO: la fila decía dos métricas; cruzando las
nueve que cruzan el piso central a 55 ensayos con su rango real son **cuatro**.

**Causa raíz:**
Un enum de unidades responde «en qué se mide», no «de qué tamaño es». Son dos hechos distintos y el repo solo
tenía el primero, así que cualquier lógica que derive presentación de la unidad hereda una ambigüedad que no
está declarada en ninguna parte. Y el defecto de render es la familia `null != 0` en su forma de **redondeo**:
no «ausencia pintada como cero», sino **una medida real pintada como cero** — con su mitad omisiva, que un
declive real quede indistinguible de la quietud.

**Solución aplicada:**
Se formatea por **MAGNITUD**, no por unidad: bajo la unidad, dos decimales con signo explícito en AMBAS
direcciones; a partir de la unidad, el entero de hoy **byte-idéntico**. Eso no necesita la unidad, maneja las
dos escalas de `RATIO` y las tres de `INDEX`, y evita la tabla `MetricKey → escala` que sería una **segunda
fuente de verdad** frente al motor (el anti-patrón que R13 acababa de rechazar). Coste: **cero**. `MetricChange`
no cambia y sus 11 sitios de construcción quedan intactos. El formateo baja al dominio
(`TransferLine.deltaText`, un único sitio de construcción), lo que **BORRA la tercera copia** de un bloque de
render byte-idéntico en dos pantallas y lo vuelve testeable sin Compose. La legibilidad que NO se decide
(«+2 puntos» en vez de `0,02`) se registra con dueño, porque exige justo la escala que no existe. Mutación x3,
todas restauradas byte-idénticas por SHA-256.

**Regla futura:**
Antes de derivar CUALQUIER decisión (formato, umbral, validación) de un enum de unidades, **mide los rangos
reales que cubre cada valor**: una unidad dice en qué se mide, no de qué tamaño es, y dos métricas pueden
compartir unidad y diferir en un factor de 100. Si el enum resulta ambiguo, la salida correcta casi nunca es
una tabla nueva que lo desambigüe —eso es una segunda fuente de verdad— sino **derivar del propio dato**: aquí
formatear por magnitud salió más barato, más correcto y sin tocar un solo sitio de construcción. Aplica a tus
propios hallazgos la asimetría que ya cuesta tres fases seguidas: **verifica el ENUNCIADO y la PRESCRIPCIÓN por
separado** — aquí el enunciado era cierto, el defecto real, y seguir la prescripción al pie habría embarcado un
defecto peor que el que arreglaba. Y trata `roundToInt()` sobre un valor de dominio como un candidato a `null !=
0`: pregúntate **qué rango de valores reales colapsa a cero**, y si el signo distingue una caída de la quietud
(`Int` no tiene cero negativo, así que un `if (d > 0) "+" else ""` hace que un declive pequeño y un cero exacto
impriman lo mismo). El corolario metodológico más caro de la fase: **un guard de anti-vacuidad sobre el CONTEO
no valida la ATRIBUCIÓN.** Mi extractor de tests muertos mintió dos veces; la segunda PASÓ su propio guard
(2 muertos == 2 en la cabecera) y aun así imprimió **los nombres equivocados**, porque su `[\s\S]*?` se tragaba
los `<testcase …/>` auto-cerrados que preceden a un fallo. Con esos nombres, M1 parecía haber matado dos tests
preexistentes en vez de los míos — un resultado que habría invalidado la fase entera. Lo resolvió leer el
**MENSAJE** del fallo, que nombra la aserción; el nombre del testcase es un dato derivado y el mensaje no.
Corolarios de entorno, uno NUEVO y tres conocidos: (1) **el ledger tiene DOS tablas de eventos** —§3 por
hallazgo, §4 por tarea— y el validador solo lee la de §3; peor, **descarta en silencio** toda fila cuyo
`cells[5]` no sea un token de estado limpio, así que una fila bien intencionada en la tabla equivocada es
INVISIBLE en vez de un error, y la cabecera de §4 describe un orden de columnas que el validador no impone —
**el validador es la autoridad, no la cabecera**; lo cazó `validate-audit-ledger.mjs`, no yo, y a la segunda
pasada; (2) `NoClassDefFoundError` + `initializationError` en una corrida de mutación es INCONCLUSA, no un rojo
—la mía coincidió con 7-8 JVM ajenas sobre el worktree— y se repite exigiendo una ventana quieta de VARIAS
muestras consecutivas; (3) `Daemon compilation failed: null` con stack en `IncrementalCachesManager.close` y
CERO errores `e:` de fuente se re-ejecuta, no se cambia una línea; (4) **borra tu backup de mutación del árbol
antes de reportar**: el `.r14bak` sobrevivió a la restauración y salió en el `git status` de un gate posterior.

**Archivos afectados:**
- `shared/src/commonMain/.../focus/domain/reporting/TransferReport.kt` (`deltaText` puro + `TransferLine.deltaText`)
- `shared/src/commonMain/.../focus/presentation/ui/{FocusEvaluationScreen,FocusProgressScreen}.kt` (bloque de render duplicado BORRADO)
- `shared/src/commonTest/.../focus/domain/reporting/TransferReportBuilderTest.kt` (endurecido EN SITIO, +3 tests, 10 → 13)
- `docs/audits/ADR_DELTA_RENDERING_2026-08-29.md` (nuevo, con §5 de correcciones) · `MINDBOOST_AUDIT_LEDGER.md` (`MB-CATALOG-0024` CERRADO con alcance corregido y prescripción retirada; `0025` nuevo)

### 2026-08-29 — Una regla de diseño se cita por su ENUNCIADO y se aplica por su CRITERIO: medido, «polisémico ⇒ frase» es en realidad «frase cuando el root desnudo tiene falsos positivos», y estrechar un gate que no los tiene es aflojarlo

**Error detectado:**
Fase R14, sobre `MB-EMO-0031` —tercer hallazgo consecutivo escrito por mí—. La fila decía que la familia
GAMBLING lleva **cuatro** palabras sueltas polisémicas y prescribía convertirlas en frases citando la regla
del propio lexicon («polysemic roots are encoded as PHRASES, never bare words»). Medido sobre los mismos 934
ficheros que camina el gate, las dos mitades del enunciado son falsas. (1) **Son tres, no cuatro**: `cuota`,
`stake` y `odds` están a una inflexión de un falso positivo real (`cuotas` en `VectorFilterPlanner`, `stakes`
en `EmotionCalibrationPrompts`, `odd` en `SeparatorItemFactoryTest`), pero `bet` mide **0** y su vecina `bets`
mide **0** — no está a distancia de nada. Y el cuerpo de mi propia fila decía «los tres», igual que el KDoc de
`CopySafetyLexiconParityTest`: la cabecera se contradecía con su propio cuerpo y con el test. (2) **La
prescripción es dañina.** El criterio que el repo aplica de verdad no es la polisemia sino los falsos
positivos MEDIDOS: los roots que sí se volvieron frase golpean copy honesta sin parar —`transferencia` 34
veces en 20 ficheros, `mental` 13, `trabajo` 9, `CI` 5, `balance` 4, `cognitivo` y `conducir` 1— y estos
cuatro golpean **cero**. No hay alivio que comprar. Y como el matcher está anclado con `\b`, toda aparición
de una frase contiene su palabra cabeza en el mismo índice: una frase caza un SUBCONJUNTO estricto de lo que
caza la palabra suelta. Aplicar la prescripción habría **aflojado** un gate bloqueante a cambio de nada.

**Causa raíz:**
Una regla de diseño se escribe con un enunciado corto y se aplica con un criterio que casi nunca está escrito
al lado. Aquí el enunciado («polisémicos como frase, nunca palabra suelta») es memorable y el criterio (la
cuenta de falsos positivos que lo motivó) vivía sólo en la cabeza de quien lo escribió. Un lector posterior
—yo— citó el enunciado, no encontró el criterio, y dedujo que había una excepción sin justificar donde en
realidad había una precondición no cumplida. Agravante propio: el KDoc terminaba diciendo que estrechar
«es una decisión de política de lexicon, no un arreglo de morfología», que se lee como *pendiente*, no como
*medido y descartado*. Ese medio renglón es lo que generó el hallazgo.

**Solución aplicada:**
Cero cambio de comportamiento: los cuatro lemas siguen desnudos. Lo que cambia es el documento que produjo la
prescripción falsa — el KDoc declara ahora el criterio CON SUS CIFRAS (34/13/9/5/4/1/1 frente a 0) y dice por
qué estrechar sería aflojar. Y la parte que hace el cierre mecánico y no narrativo: un guard nuevo,
`aPhraseIsStrictlyWeakerThanItsHeadWord`, pinnea la propiedad de subconjunto sobre el matcher real (con su
mitad discriminante: existe copy de apuestas que la palabra cabeza caza y la frase pierde), de modo que la
razón por la que se rechazó la prescripción deja de ser un razonamiento y pasa a ser un test. El guard se
añadió **en el fichero que ya era dueño** de estos tres lemas, no en uno nuevo. De paso se corrigió una
sobre-afirmación mía en ese mismo test: su comentario llamaba «measured false positive» a las cinco formas de
su lista, y `bets`/`bankrolls` miden 0 — están fuera porque nadie las pide, no porque fueran a fallar. Dos
mutaciones, y el reparto es el resultado: **M1** aplica la prescripción exacta (`literal("cuota")` →
`literal("cuota de apuesta")`) y deja ROJOS los dos guards que son dueños del CONTENIDO del lexicon con los
otros ocho verdes —incluido el guard nuevo, que pinnea una propiedad del *matcher* y por tanto no debe
dispararse ahí—; **M2** rompe el anclaje `\b` y entonces sí cae el guard nuevo, por su mitad discriminante y
con el mensaje exacto que la describe. Cada guard cae en la mutación que le toca y en ninguna otra.

**Regla futura:**
Cuando una regla de diseño te empuje a cambiar algo, **busca el criterio que la regla aplica de verdad, no su
enunciado**: reproduce la medición que la motivó sobre los casos que la regla YA gobierna y sobre el tuyo. Si
tu caso no cumple la precondición (aquí: cero falsos positivos frente a decenas), no eres una excepción sin
justificar — estás fuera del alcance de la regla, y la salida correcta es **escribir el criterio con sus
cifras al lado del enunciado** para que el siguiente lector no vuelva a abrir el mismo hallazgo. Antes de
estrechar cualquier matcher, comprueba la **dirección**: con anclaje `\b`, una frase caza un subconjunto
estricto de su palabra cabeza, así que sustituir una por otra es SIEMPRE una pérdida de cobertura — legítima
sólo si compra alivio de falsos positivos medido, y en un gate que BLOQUEA el coste de un falso positivo es
ruidoso e inmediato mientras el de un falso negativo es silencioso. Y no dejes en un KDoc una frase que
suene a pendiente («es una decisión de política, no un arreglo de morfología») para algo que ya mediste y
descartaste: eso no documenta la decisión, la reabre. Corolario ya conocido y vuelto a morder: **un fallo de
compilación en ficheros que tu diff no toca se ATRIBUYE antes de tocar nada** — aquí `deltaText` con 4
apariciones en `commonMain` y ambos ficheros ` M` no era fase RED con símbolo ausente sino un refactor en
vuelo de la sesión paralela (`String` frente a `Double`), y se espera, no se arregla; y `e: Daemon
compilation failed: null` con CERO errores `e:` de fuente sigue siendo corrupción incremental, que se
re-ejecuta. Y una **regla operativa NUEVA**, que casi cuesta cara: mi gate completo falló con
`NoSuchFileException: test-results/desktopTest/binary/results-generic.bin` y CERO XML escritos, y la salida
obvia —limpiar ese directorio— habría **destruido la evidencia de la sesión vecina**, que estaba corriendo su
propia suite en ese instante (había un `in-progress-results-*.bin` de 598 KB modificado un minuto antes). Es
`MB-FOCUS-0083` visto desde el otro lado: allí un revisor borró MI evidencia, aquí casi la borro yo a otro.
**Antes de limpiar un directorio de resultados compartido, comprueba si hay un `in-progress-*.bin` vivo**; si lo
hay, no se limpia: se espera con un bucle que aguarde a que desaparezca y se encadena el gate propio detrás. Un
directorio de resultados es de quien está escribiendo en él, no de quien llega. Corolario del mismo incidente:
la huella `git status` de mi corrida SÍ se movió (`94a0224ec464` → `dadf01b3fe8c`), y en vez de declararla
inadmisible sin más la ATRIBUÍ — un solo `.kt` de `shared/src` tocado dentro de la ventana, `TransferReport.kt`,
ajeno a mi diff, con `compileKotlinDesktop` y `compileTestKotlinDesktop` UP-TO-DATE al arrancar (o sea: Gradle
verificó por CONTENIDO que las entradas de compilación no habían cambiado) y mis tres ficheros con su SHA-256
intacto. Una huella que se mueve no es automáticamente una corrida inválida: es una corrida que **hay que
atribuir**, y lo que la salva o la hunde es si lo que se movió está en el conjunto de entradas del gate. Dos
cierres más, ambos sobre basura propia. (1) **Barre los artefactos de tu propio arnés antes de dar la fase por
cerrada**: yo había reportado «no queda ningún `.r14bak`» y quedaba uno — y encima de un fichero que mis dos
mutaciones NUNCA tocaron, lo que significa que el arnés corrió en un sitio que yo no había previsto. Un backup
huérfano de un fichero ajeno no se restaura por reflejo: se DIFERENCIA primero (aquí el delta era un carácter en
un `sign`, ninguna de mis dos mutaciones), y si no es tuyo se saca del árbol en vez de borrarlo. (2) El `cd` de
una llamada anterior **volvió a morder, y esta vez fabricó una falsa alarma**: con el cwd colgado en un
subdirectorio, un `find shared/src -name 'TransferReport*'` devolvió VACÍO y llegué a escribir que la sesión
vecina estaba borrando ficheros fuente. No borraba nada. La firma que lo delata es un `git status -- <ruta>`
avisando de una ruta DUPLICADA (`.../reporting/shared/src/.../reporting/`): cuando una ruta aparezca repetida en
un mensaje de error, no diagnostiques el repo, diagnostica tu cwd — y lleva un `pwd` en la misma llamada.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/copy/ForbiddenLexicon.kt` (criterio medido en el KDoc; `bet` separado de `stake` con su razón real; predicción de `bote` corregida)
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/copy/CopySafetyLexiconParityTest.kt` (+1 guard de subconjunto con mitad discriminante; comentario sobre-afirmado corregido)
- `MINDBOOST_AUDIT_LEDGER.md` (`MB-EMO-0031` CLOSED con enunciado corregido: cuatro → tres, y prescripción RETIRADA por medición)

### 2026-08-29 — Verificar un hallazgo PROPIO antes de construir su gate refutó tres enunciados seguidos; un signo de sección puede apuntar a dos documentos dentro del MISMO fichero; y una huella `git status` no ve los cambios de CONTENIDO en un fichero ya modificado

**Error detectado:**
`MB-FOCUS-0084` («el bloque VIGENTE del ADR invoca como obligaciones vivas definiciones que sólo existen por
debajo de su marcador de retiro») lo escribí yo en la fase anterior. Al ir a implementar su gate de cierre,
verificarlo primero refutó **tres** enunciados sucesivos: (1) el original —los `§N` de las cabeceras `R3-*` son
back-references al **contrato de reapertura**, cuya plantilla está EN EL REPO
(`docs/context_pack/07_PHASE_REVIEW_PROTOCOL.md`, bloque cercado con numeración PROPIA §1..§13): su §10 «Tests y
gates obligatorios» tiene exactamente 5 subsecciones y casa con «5 familias de tests §10», su §12 es «Definition
of Done específica» y casa con «DoD §12»—; (2) el candidato de reemplazo «16 punteros `§9.1..§9.16` colgantes»
—los 16 ítems EXISTEN y **son** `R3-1..R3-16`, el conteo casa exacto—; (3) el candidato «hay marcas que repiten
una cláusula que su sucesora contradice» —auditados los dos casos de mayor riesgo (§5 vs `R3-7`, §11 vs
`R3-18`), en ambos el KDoc ya enuncia la verdad viva—. Y de paso apareció que la justificación que yo había
escrito para NO re-apuntar citas —«para la mayoría no existe sucesora»— es falsa: son **21 de 27** las que sí
tienen sucesora viva candidata. Peor: **la nota que escribí para arreglar todo esto traía su propia
sobre-afirmación** («un `§N` desnudo se lee contra el contrato, NO contra las secciones R2»), falsa para
`§6.1`/`§10.2`/`§10.3c`; la cazó el gate adversarial, no yo.

**Causa raíz:**
Un hallazgo propio no tiene revisor externo, así que su enunciado llega a la fase siguiente con el aval implícito
de haber sido registrado — y un registro no es una verificación. El defecto de fondo del documento no era de
autoridad sino de **espacio de nombres**: el signo de sección está sobrecargado entre dos numeraciones dentro del
MISMO fichero (el contrato de reapertura y las secciones R2 retiradas del propio ADR), de modo que el `§10` del
título de `R3-20` y el `§10.2` de unas líneas más abajo apuntan a documentos distintos. El documento ya lo sabía
—`R3-17` desambiguaba un caso a mano y dos citas llevan prefijo «R2» explícito—, pero la convención no estaba
escrita en ninguna parte, así que cada lector la reinventa.

**Solución aplicada:**
`MB-FOCUS-0084` se cierra como **REFUTADO**, **sin una sola línea de Kotlin**, y NO se implementó el gate que
prescribía (regla 2026-08-19: premisa falsa ⇒ no seguir el plan al pie). Único remedio: una **nota de convención**
al inicio del bloque vigente declarando las DOS ramas del signo con sus pruebas internas, más el hecho añadido al
TECHO ya declarado del guard (`[R2-HIST]` significa «lee este número contra el bloque R2», que es CONVENCIÓN y no
algo que la cita porte). Se rechazó añadir guard mecánico: la sobrecarga restante es de prosa y no tiene
consecuencia viva medida, y un guard sobre un invariante sin consecuencia reproduce el anti-patrón del símbolo sin
consumidor. El gate adversarial corrió con `agentType: 'architect'` (Read/Grep/Glob, **sin Bash**) — el mecanismo
de `MB-FOCUS-0083`: 18 agentes, 5 lentes, y la única `CONFIRMADA` fue la que verificó 1-a-1 las 27 marcas
`[R2-HIST]` contra el contenido real de su sección.

**Regla futura:**
Un hallazgo **escrito por ti** exige MÁS escrutinio que uno heredado, no menos, y el momento de verificarlo es
**antes de construir su gate de cierre**, no después: aquí tres enunciados sucesivos cayeron y el trabajo de la
fase acabó siendo *no implementar*. Cuando refutes un enunciado, **refuta también el reemplazo** antes de
adoptarlo — el segundo candidato se siente probado por el trabajo que costó el primero, y no lo está. Al
interpretar una cita por sección, pregunta **de qué documento es la numeración**: si el fichero mezcla dos
espacios de nombres, el mismo `§N` apunta a sitios distintos en la misma página, y la señal de que ocurre es que
alguien ya lo desambiguó a mano en algún sitio (busca «= ESTE», «no la», prefijos explícitos). Un número de
sección casa con su destino cuando **el contenido** casa: `§10` = «5 familias de tests» se confirmó contando 5
subsecciones en el documento candidato, no razonando. Y aplica a tu propia corrección el estándar del defecto que
corrige: la nota que escribí para arreglar una sobre-afirmación traía otra, exactamente como el párrafo COVERAGE
de 2026-08-28. Corolarios de entorno, uno NUEVO: (1) **una huella `git status --porcelain` NO detecta cambios de
CONTENIDO en un fichero que ya figuraba como modificado**, así que abrazar un gate sólo con ella es insuficiente —
hay que cruzarla con **mtimes** de los ficheros del fallo; fue lo que probó que la sesión vecina editaba
`ForbiddenLexicon.kt` (04:25) y `MindBoostCopySafetyScanner.kt` (04:28) **mientras** mi suite corría, haciendo esa
corrida INADMISIBLE; (2) una corrida posterior con el árbol quieto puede seguir roja **contra clases rancias** y
producir **1 solo XML** (Gradle re-ejecutando sólo la clase que falló) — un agregado sobre eso no es la suite, y
el remedio es `--rerun`, que aquí dio 4347/0F/0E sobre 610 XML todos frescos; (3) un fallo cuyos ficheros tienen
mtime POSTERIOR a tu última edición y cuyos símbolos no aparecen en tu diff es WIP ajeno en fase RED: se ATRIBUYE
y se deja quieto.

**Archivos afectados:**
- `docs/engines/focus_v5/ADR_FOCUS_TEMPORAL_QUALITY.md` (nota de convención; marcador de retiro 524 → 542)
- `docs/engines/focus_v5/reaudit/c4/ADR_AUTHORITY_BOUNDARY_2026-08-28.md` (§4.1 corregida, §7 nueva con las tres refutaciones)
- `shared/build.gradle.kts` (4 líneas en el TECHO ya declarado del guard) · **Kotlin tocado: 0 ficheros**
- `MINDBOOST_AUDIT_LEDGER.md` (`MB-FOCUS-0084` CLOSED/REFUTADO; `MB-FOCUS-0079` con su justificación corregida)


### 2026-08-28 — Un guard cuyo sujeto vive en un COMENTARIO no puede ser un test: la tarea no se invalida y hereda su verde; y la razón que mantenía vivo un símbolo puede ser falsa por la misma frontera que su hallazgo hermano vigila

**Error detectado:**
Disposición de `MB-FOCUS-0079` (nada distingue una cita al bloque SUPERSEDED del ADR usada como EVIDENCIA de una
usada como AUTORIDAD) y `MB-FOCUS-0080` (`Verdict.Degraded`, variante sellada sin productor). Cuatro cosas, y tres
son sobre mi propio trabajo. (1) Escribí el guard como test de `desktopTest` y **la mutación lo refutó**: al quitar
un marcador real, `> Task :shared:desktopTest UP-TO-DATE` con **0 XML frescos** — `BUILD SUCCESSFUL`, y sin haber
ejecutado nada. El marcador vive en un **KDoc**, y una edición de comentario que no mueve líneas deja el bytecode
idéntico, así que la tarea de test no se invalida **justo ante la deriva que el guard existe para cazar**. (2) El
alcance registrado del hallazgo —«68 citas en 29 ficheros»— **no es reproducible**: medí ocho predicados candidatos
sobre el árbol vivo y ninguno lo produce; la cifra defendible es 29 líneas en 19 ficheros, de las que **27 apuntan
al ADR y 2 al `charter`**. (3) La razón por la que `MB-FOCUS-0080` seguía abierto —«el ADR §9 la nombra como parte
del vocabulario»— es **FALSA por la frontera del hallazgo hermano**: §9 está en la línea 819, muy por debajo del
marcador de retiro de la 524. (4) Dos defectos en mi propio guard antes de ejecutarlo: escribí el signo de sección
LITERAL (el escáner se encontraría a sí mismo, contradiciendo su propio KDoc) y el patrón de cabecera exigía punto
final, así que **se saltaba las 12 subsecciones** `### N.M` — incluida §4.1, cuya taxonomía el bloque vigente
consume sin redefinir.

**Causa raíz:**
Un test se re-ejecuta cuando cambia su **classpath**, no cuando cambia el **texto** que lee de disco. Para todo
guard cuyo sujeto sea prosa —un marcador en un comentario, una cabecera de un `.md`, un token de atribución— eso
lo vuelve estructuralmente fail-open: Gradle no sabe que esos bytes son inputs suyos, y un `.md` fuera de todo
source set no invalida absolutamente nada. Es la familia de «un gate que no corre deja el artefacto del pasado»,
pero un nivel más abajo: aquí no hay artefacto rancio que borrar, es la **relación de invalidación** la que no
existe. Y (3) es la forma recursiva del defecto que la fase arregla: un documento medio retirado se cita mal por
defecto, incluso —sobre todo— en la fila del ledger que decide si un símbolo vive.

**Solución aplicada:**
El guard pasa a ser **tarea Gradle** (`:shared:adrAuthorityBoundaryScanner`, enganchada a `check`) con
`@InputDirectory`/`@InputFile`, que hashean **contenido**: la misma edición que dejaba el test UP-TO-DATE ahora
re-ejecuta el gate. **Un solo dueño**: el test se BORRÓ, no se dejó como defensa en profundidad. El gate lleva su
**mitad discriminante dentro** (re-escanea el corpus en memoria sin el token y exige que las no-atribuidas igualen
EXACTAMENTE a las marcadas), sus anti-vacuidades (20 secciones llanas / 20 `R3-*` / 27 citas / 19 ficheros) y el
signo de sección **como escape**, para no encontrarse a sí mismo. Las 27 citas llevan `[R2-HIST]` y **ninguna se
re-apuntó a una sección `R3-*`**: para la mayoría no existe sucesora y re-apuntarlas habría FABRICADO una sucesión.
`MB-FOCUS-0080` se cierra **BORRANDO** la variante, cuya ausencia impone ahora un `when` exhaustivo. Cuatro
mutaciones concluyentes (marcador retirado ⇒ ROJO con el sitio exacto; sección llana colada sobre el marcador ⇒
ROJO con la línea exacta; clasificador que declara todo atribuido ⇒ ROJO por la auto-comprobación; reintroducir la
variante ⇒ ROJO de compilación) más **una INCONCLUSA declarada como tal**, que es la que produjo el rediseño.

**Regla futura:**
Antes de elegir la forma de un guard, pregunta **qué edición debe despertarlo** y comprueba que esa edición
invalida su tarea: si el sujeto es prosa (comentario, `.md`, token de atribución), un test es fail-open y la forma
correcta es una tarea Gradle con `@InputDirectory`/`@InputFile` — el repo ya tiene esa forma en sus escáneres, y el
aprendizaje de 2026-06-28 («un gate que valida datos ESTRUCTURADOS es un test») **no aplica al texto**: son los dos
lados de la misma línea. Corolario que casi me engaña: un `BUILD SUCCESSFUL` en una corrida de MUTACIÓN es tan
sospechoso como en una de verificación — **una mutación que no pone el gate en rojo no prueba que el gate sea
decorativo hasta que demuestres que el gate CORRIÓ**; comprueba la línea de tarea y los XML frescos antes de sacar
cualquier conclusión, en las dos direcciones. Cuando el arreglo cambie la forma del guard, **borra la versión
vieja**: dos guards del mismo invariante son dos dueños. Y **verifica la razón que mantiene abierto un hallazgo
con el mismo estándar que su enunciado**: aquí la razón citaba una sección retirada, y retirarla dejó el hallazgo
sin ningún motivo para seguir vivo — el trabajo no fue implementar nada, fue medir que la justificación era falsa.
Al medir el alcance de un hallazgo heredado, **si ningún predicado razonable reproduce su cifra, la cifra no es
imprecisa: no es reproducible**, y sustituirla exige decir con qué predicado se midió la nueva. Por último, cuando
un documento «vigente» esté medio retirado, mide si su bloque vivo es **autocontenido** antes de elegir entre
prohibir y atribuir: aquí 7 de 14 secciones citadas no aparecen en el bloque vigente y las otras 7 las invoca él
mismo hacia abajo, así que prohibir las citas habría roto 27 sitios honestos — la medición eligió el diseño.

**Archivos afectados:**
- `shared/build.gradle.kts` (tarea `adrAuthorityBoundaryScanner` + `check`) · BORRADO: `shared/src/desktopTest/.../core/timing/AdrAuthorityBoundaryTest.kt`
- `shared/src/commonMain/.../focus2/domain/evidence/FocusTemporalEligibility.kt` (`Verdict.Degraded` borrada) · `shared/src/desktopTest/.../focus2/simulation/FocusTemporalEligibilityAdversarialOracleTest.kt` (`when` exhaustivo)
- 27 citas marcadas con `[R2-HIST]` en 19 ficheros de `shared/src` (commonMain 19, desktopTest 8 líneas, androidMain 2, iosMain 2)
- `docs/engines/focus_v5/reaudit/c4/ADR_AUTHORITY_BOUNDARY_2026-08-28.md` (nuevo) · `docs/engines/focus_v5/S5_PASO7_CLOSURE.md` (§4 y párrafo `P7-F4` corregidos)
- `MINDBOOST_AUDIT_LEDGER.md` (`MB-FOCUS-0079` y `MB-FOCUS-0080` CLOSED; `MB-FOCUS-0084` nuevo)

### 2026-08-28 — Una costura muerta se cierra BORRÁNDOLA, no implementándola; un hallazgo propio puede estar INFRAVALORADO (no solo sobreactuado); y un `else` en una tabla de etiquetas es un fallback silencioso que embarca el identificador interno como copy

**Error detectado:**
Fase R13 del catálogo, sobre `MB-CATALOG-0022` —segundo hallazgo consecutivo escrito por mí—. Los **5 símbolos
resultaron CIERTOS** (y el primero MÁS fuerte de lo enunciado: `forCapacity` tiene **una sola ocurrencia en todo
`shared/src`, su propia definición**), pero la consecuencia era falsa: «el veredicto puede descansar en una métrica
que no indexa la capacidad» no describe ningún defecto, porque **nadie une sonda→capacidad→veredicto** — el único
símbolo que podría hacerlo no lo llama nadie. No había defecto que impedir: había una **costura muerta**. Y trazando
ese mismo consumidor hasta la pantalla apareció lo que el hallazgo no contenía y que contradice a OTRO hallazgo mío:
`MB-CATALOG-0021`, que registré en R12 como P3 con la frase «no es un defecto vivo», **está VIVO**. Yo había asumido
que `ReliabilitySource.EMPTY` lo mantenía inerte; lo que `EMPTY` bloquea es que una línea sea *strong*, **no que se
LISTE**: las líneas caen en `preliminary` y se pintan igual. Cadena completa: `DefaultCalibrationEngine` mete en
`perMetric` toda métrica `confident` (pisos EN SESIÓN 3..6) → el motor emite `IES`, `RATE_CORRECT_SCORE`, `LISAS`,
`BLOCK_STABILITY`, `SUSTAINED_FOCUS_INDEX` → las cinco mapean a `ACCURACY_RT_BASE` (piso 20, celda única), luego son
suficientes a 55 ensayos de sonda → pasan el gate y llegan a `changes` → `label()` cae a `else -> metric.name`. Un
usuario ve **`SUSTAINED_FOCUS_INDEX`** en una pantalla en castellano. La prueba más limpia: `FocusProgressScreen`
pinta la MISMA métrica con DOS nombres, «Índice de foco sostenido» en la fila de tendencia y el identificador crudo
en la de transferencia, doce líneas más abajo.

**Causa raíz:**
Dos causas distintas y ninguna es aritmética. (1) Un hallazgo que dice «nada impide X» describe una AUSENCIA, y una
ausencia no es un defecto salvo que exista un consumidor que la sufra; cuando no lo hay, lo que queda es código
conservado para una fase que no llegó — y ese código **se lee como capacidad existente**, que es el anti-patrón que
el repo ya tenía escrito. (2) Un `else` en una tabla de nombres no es un default: es la decisión de embarcar el
identificador interno cuando nadie decidió el nombre. Sobrevive porque el conjunto que lo alcanza no está en el mismo
sitio que la tabla —aquí lo deciden `perMetric`, el motor y el piso central, tres símbolos lejanos— así que «hoy solo
se listan cuatro» es una creencia sobre otro fichero, no una propiedad de éste.

**Solución aplicada:**
`0022` se cierra **BORRANDO `ProbeBank.forCapacity`** (una línea), no creando el símbolo que ya se había rechazado en
R11 y R12; la restricción sobre la fase futura sobrevive al cierre en la propia fila del ledger, y se declara el techo
(el borrado no deja guard mecánico contra reintroducirlo). `0021` se cierra retirando el `else` y haciendo el `when`
**EXHAUSTIVO** sobre `MetricKey`: el guard queda en BUILD, así que una métrica nueva **no compila** hasta que alguien
decide cómo se llama de cara al usuario. Las 4 etiquetas preexistentes se conservan **byte-idénticas** y las 21 nuevas
conservan el marcador «(prueba)», que es el ÚNICO indicador de sonda en `FocusSummaryScreen`. Se RECHAZÓ fusionar las
dos tablas de etiquetas: cambiaría 3 cadenas embarcadas sin arreglar nada vivo, y el precedente del repo para copias
múltiples es **pinnearlas**, no fusionarlas; la segunda copia es `private` en un fichero Compose —inalcanzable desde
`desktopTest`— así que se REGISTRA con dueño en vez de subirle la visibilidad. Mutación x3, todas restauradas
byte-idénticas por SHA-256.

**Regla futura:**
Cuando un hallazgo diga «nada impide X», **busca primero el consumidor que sufriría X**: si no existe, no tienes un
defecto, tienes código muerto, y la salida correcta es **borrarlo** —vuelve con su consumidor y su test— en vez de
construir el símbolo que lo gobernaría. Cerrar por borrado exige su propia forma de evidencia: la **ausencia de
rotura** (compilar todos los source-sets y correr la suite completa), y declarar que un borrado no deja guard contra
la reintroducción. Y aplica a tus propios hallazgos la simetría que se olvida: un hallazgo tuyo puede estar
**INFRAVALORADO**, no solo inflado — dos fases seguidas rebajé una severidad y en la segunda descubrí que había
subestimado la de al lado. La forma concreta del error: **di por inerte una ruta porque un gate vecino estaba
apagado, sin comprobar QUÉ apaga ese gate.** `ReliabilitySource.EMPTY` impide clasificar una fila como fiable; no
impide renderizarla. Antes de escribir «hoy no es alcanzable», enumera las propiedades de la ruta y comprueba cuál
gobierna cada una. Trata un `else` en una tabla de NOMBRES como un fallback silencioso, nunca como un default: si el
conjunto que lo alcanza lo deciden otros ficheros, «hoy solo llegan cuatro» es una creencia, no un invariante — y el
arreglo es un `when` exhaustivo (guard en BUILD), no más ramas con el `else` intacto. **Mide el alcance con grep antes
de registrarlo**: iba a escribir «3 pantallas» para el defecto del delta y son 2 (`FocusSummaryScreen` pinta la
etiqueta pero no el delta). Corolarios de entorno, uno NUEVO y dos ya conocidos que volvieron a morder: (1) **una
tarea Gradle que falla SOLO en su paso de `Failed to store cache entry` deja igualmente registrado su snapshot de
entradas como construido**, así que al reaplicar la misma mutación más tarde sale `compileKotlinDesktop UP-TO-DATE`
*pese a que el fuente cambió* y el test no vuelve a correr: mi M2 dio `BUILD SUCCESSFUL in 11s` con la mutación
puesta, que parecía «la mutación no mata nada» y era un falso verde — se detecta contando el output (XML rancio) y se
resuelve con `--rerun` en compilación **y** en test, jamás con `clean`; (2) en PowerShell `@($null).Count` vale **1**,
así que contar hallazgos de detekt con `@($file.error).Count` da 1 sobre un informe VACÍO — es el mismo espejismo de
R12 con otro disfraz, y se resuelve con XPath (`SelectNodes('//error')`) o leyendo el XML crudo; (3) `Could not pack
tree` sobre `.class` de módulos que tu fase no toca es la sesión paralela: se espera una ventana quieta **exigiendo
varias muestras consecutivas** (una sola lectura de «2 JVMs» no basta, el vecino arranca entre el sondeo y el build).

**Archivos afectados:**
- `shared/src/commonMain/.../focus/domain/evaluation/ProbeBank.kt` (`forCapacity` BORRADO) · `.../focus/domain/reporting/TransferReport.kt` (`when` exhaustivo, 21 etiquetas nuevas)
- `shared/src/commonTest/.../focus/domain/reporting/TransferReportBuilderTest.kt` (endurecido EN SITIO, +3 tests)
- `docs/audits/ADR_METRIC_LABELS_2026-08-28.md` (nuevo, con §5 de correcciones) · `MINDBOOST_AUDIT_LEDGER.md` (`MB-CATALOG-0021` P3→P2 y CERRADO; `0022` CERRADO por borrado; `0023`/`0024` nuevos)

### 2026-08-28 — Una copia `private` no se hace alcanzable, se BORRA; el enunciado de un hallazgo puede nombrar el consumidor MENOR; y un test que pasa por dos razones sigue pasando cuando le quitas una

**Error detectado:**
Cierre de `MB-EMO-0032`. (1) La fila que yo mismo había escrito decía que la cuarta copia del fold gobierna «un gate
que BLOQUEA salida de runtime». Al leer el fichero entero, el fold tiene **dos** consumidores y el que la fila omitía
es el mayor: `classifyRequest`, el **clasificador de crisis/autolesión**. Medido, 5 de los 7 pares son portantes en ese
gate y **dos están en la ruta de crisis**: sin `ñ→n`, `"quiero hacerme daño"` deja de casar el término `hacerme dano`,
cae a `NORMAL_TRAINING` y **va al modelo** en vez de devolver la respuesta de crisis. Severidad registrada P3; medida,
P2. (2) El test preexistente `crisis_takes_priority` **no protegía nada de eso**: su frase es `"no quiero vivir, quiero
hacerme daño"`, y `"no quiero vivir"` es a su vez término de crisis que no necesita plegado — así que pasa igual con el
par de la eñe eliminado, y lo demostró la mutación, no un razonamiento. (3) Mi primera medición de qué pares son
portantes fue **VACUA**: inyectaba tildes en posiciones arbitrarias (`ño quiero vivir`, `áutolesion`) y «demostraba» que
los 7 lo son; sólo mide que la letra plana aparece. (4) Mi inventario de tablas dio **0 en el build script**, que
contradecía lo que ya sabía — la regex describía `mapOf('á' to 'a')` y el build script usa `when ('á' -> 'a')`. (5) Un
`/*` escrito dentro de un KDoc abrió un comentario **anidado** y comentó el resto de la clase.

**Causa raíz:**
Un símbolo `private` es inalcanzable desde un test de paridad, y la salida que parece obvia —subirle la visibilidad para
poder compararlo— conserva el duplicado y se limita a **vigilarlo**. La deriva sigue siendo posible; sólo se vuelve
detectable. Y la razón de que el duplicado sobreviviera tanto es que **una copia correcta es invisible al comportamiento**:
se comporta idéntico hoy y sólo diverge mañana, así que ningún test de comportamiento la ve nacer.

**Solución aplicada:**
La tabla privada y su `fold` se **BORRAN**; el gate consume `MindBoostCopySafetyScanner.fold`. La deriva pasa de
detectable a **imposible**, que es la jerarquía que este repo ya prefiere (estructura sobre vigilancia). Lo que
deliberadamente NO se comparte es el **matcheo**: el gate sigue usando `contains` y no el anclado a palabra del escáner,
porque un gate que BLOQUEA es correcto siendo más amplio, y eso queda declarado en el KDoc en vez de quedar implícito.
Dos guards, cada uno cazando lo que el otro no puede: uno de **comportamiento** en la ruta de crisis, biconditional (la
grafía acentuada DEBE alcanzar su categoría; la misma frase con ese carácter cambiado por uno que el fold deja intacto
NO debe — que es lo que prueba que el match corre por el plegado de ESE carácter y no por otro término de la frase), y
uno **estructural** contra que la copia renazca. La medición se rehízo declarando la ortografía real a mano y simulando
el clasificador completo, con el fixture verificado bajo el fold completo **antes** de creer ninguna cifra, y el
inventario se cruzó por dos métodos independientes.

**Regla futura:**
Cuando un duplicado esté `private` y por eso no se pueda pinnear, la pregunta correcta no es «cómo lo hago alcanzable»
sino **«por qué sigue existiendo»**: borrar la copia y consumir la autoridad canónica cierra el hueco de verdad, y subir
la visibilidad sólo lo instrumenta. Al deduplicar un gate de seguridad, **comparte el dato y NO el veredicto**: si su
matcheo es deliberadamente distinto (más amplio para bloquear), declara esa asimetría, o el siguiente lector la
«unificará». Verifica el **CONSUMIDOR** de un hallazgo leyendo el fichero entero, no sólo la línea que la fila cita: aquí
el fold tenía dos llamadas y la fila nombraba la de menor severidad — y la severidad vive en el consumidor, no en el
enunciado. Cuando midas si algo es portante, **prueba que tu fixture no pasa por dos caminos**: un caso que cumple por
dos razones sigue cumpliendo cuando le quitas una, y `crisis_takes_priority` llevaba dos disparadores en la misma frase.
Toda medición necesita un **cruce independiente** antes de creerla: mis dos números discreparon (5 folds por `grep "fun
fold"` frente a 3+0 por regex de tabla) y esa discrepancia es lo que destapó que mi regex sólo describía una de las dos
construcciones que el árbol usa. Y una **mutación que sólo rompe un guard es un resultado, no un fallo**: aquí re-inlinar
una copia CORRECTA dejó los 8 tests de comportamiento en verde y sólo el estructural en rojo, que es exactamente la
prueba de que el estructural no sobra. Corolario de lenguaje: **los comentarios de bloque de Kotlin ANIDAN**, así que un
`/*` dentro de un KDoc necesita dos cierres y el primero comenta el resto del fichero — nunca escribas sintaxis de
comentario dentro de un comentario, descríbela. Corolarios de entorno, todos conocidos y todos vueltos a morder: el
**`cd` de una llamada anterior sobrevive**, y aquí no sólo sacó al `gradlew` de alcance sino que falsificó la
precondición que yo había puesto para detectarlo (el `rm` del informe y el `[ -f ]` que confirmaba su borrado corrieron
en el mismo directorio equivocado, así que la evidencia parecía **más** fuerte, no más débil) — invoca los gates con la
raíz explícita y un `pwd` en la misma llamada; y una corrida cuya instrumentación registre el directorio de clases
cayendo **durante** la ejecución (5742 → 56) con la huella del árbol idéntica no es roja ni verde, es **INADMISIBLE**.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/coach/safety/AICoachSafetyPolicy.kt` (tabla y `fold` privados borrados; KDoc con la asimetría de matcheo declarada)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/copy/MindBoostCopySafetyScanner.kt` (`fold` declarado CANÓNICO y público a propósito, con lo que un gate de crisis depende de él)
- `shared/src/commonTest/.../coach/safety/AICoachSafetyPolicyTest.kt` (+1 test biconditional en la ruta de crisis)
- `shared/src/desktopTest/.../core/copy/CopySafetyLexiconParityTest.kt` (+1 guard estructural, con su techo fail-open declarado; comentario obsoleto corregido)
- `MINDBOOST_AUDIT_LEDGER.md` (`MB-EMO-0032` CLOSED, severidad P3 → P2 y consumidor corregido)

### 2026-08-28 — Un gate escrito en UN idioma es fail-open en todos los demás que el repo habla; la medición elige entre ensanchar el vocabulario y estrechar la SUPERFICIE; y un párrafo de cobertura escrito para arreglar una sobre-afirmación puede traer la suya

**Error detectado:**
Cierre de `MB-EMO-0029` y `MB-EMO-0030`. (1) El copy-safety scanner lleva un lexicon **español**, así que la mitad
inglesa del mismo defecto no tenía gate ninguno: `CreativityExport.DISCLAIMER` negaba una afirmación de salud
nombrándola, en inglés, y esa cadena es **la primera línea de cada CSV y un campo de cada JSON** que el usuario
exporta y comparte. Build verde durante toda la vida de la feature. (2) Al ir a arreglarlo, la salida obvia
—ampliar el lexicon al inglés— resultó **destructiva**: medido sobre los mismos 934 ficheros, un vocabulario inglés
amplio da 67 hits en código, **159 en comentarios** y 22 en literales, y de esos 22 **uno solo está en
`commonMain`**. Habría roto 180 sitios honestos para cazar 1. (3) Auditando mi propio trabajo del día anterior
encontré que el párrafo COVERAGE que escribí **para sustituir una sobre-afirmación** contenía a su vez **dos
clausulas falsas** ("los lexemas ingleses están ausentes del lexicon" —10 de los 63 lemas son ingleses o siglas—,
"las palabras inglesas solo aparecen en KDoc" —una estaba embarcada—), y la segunda la contradecía **la fila del
ledger que yo mismo había escrito ese mismo día**. (4) `MB-EMO-0030` afirma que el matcher existe **dos** veces;
`grep -rn "fun fold"` dice que existe **cuatro**, y la cuarta es `private` y gobierna un gate que BLOQUEA salida de
runtime del coach. (5) Mis tres primeros comentarios del arreglo **citaban textualmente la copy retirada** — que es
exactamente cómo la mitad española sobrevivió cuatro olas.

**Causa raíz:**
Un gate de léxico tiene un cuarto eje además del mecanismo, el vocabulario y la morfología: **el IDIOMA**. Y ese eje
no se ensancha como los otros tres, porque el vocabulario clínico inglés **es el idioma en que el código se
documenta a sí mismo**: las palabras que hay que prohibir en la copy son las mismas que los KDoc y las listas de
guarda de los tests necesitan nombrar. Ensanchar `ALL` al inglés no es "más cobertura", es cambiar la relación
señal/ruido de 1:0 a 1:180. La asimetría con el español no es un descuido: en español esas palabras **no** aparecen
en la prosa del repo, y esa diferencia se mide, no se razona.

**Solución aplicada:**
No se ensancha el vocabulario: se **estrecha la superficie**. `ForbiddenLexicon.ENGLISH_CLAIMS` (39 términos)
nace **DISJUNTO** de `ALL` y pinneado disjunto, y `ShippedEnglishCopyGateTest` lo aplica **solo a literales de
producción**: 1 hit real, **0 falsos positivos sobre 3976 literales de 543 ficheros**. Se pinnean las **tres**
partes del escáner, no solo el vocabulario: la SUPERFICIE (la lista de módulos se **parsea** de
`shared/build.gradle.kts`, jamás se copia a mano — una tercera copia a mano es justo la deriva que
`CopySafetyLexiconParityTest` existe para impedir) y el EXTRACTOR (con anti-vacuidad de 400 ficheros / 2000
literales). El KDoc **describe** la copy retirada en vez de citarla. `MB-EMO-0030` se cierra con el **enunciado
corregido** y pinneando las tres copias alcanzables del matcher en **ambas direcciones** (los caracteres que deben
plegarse y los que no), con sondas construidas por **code-point** para que el test no dependa de su propia
codificación, y leyendo los dos `Regex(...)` **de sus propias fuentes** en vez de compararlos contra un literal. La
cuarta copia (`private`) se **registra** (`MB-EMO-0032`) en vez de forzarle la visibilidad: cambiar el contrato de
un símbolo de seguridad de otro módulo no es plumbing de esta fase. Cinco mutaciones, todas concluyentes, todas
restauradas byte-idénticas por SHA-256: M26 (restaurar la copy embarcada) 3 ROJOS, M27 1/8, M28 1/8, M29 1/3, M30
(quitar al extractor la separación comentario/literal) 2/3.

**Regla futura:**
Antes de ensanchar un gate de léxico a **otro idioma**, mide las tres corrientes **por separado** —código,
comentarios y literales— sobre el árbol real: si los hits de comentario superan a los de copy en un orden de
magnitud, la salida correcta **no es más vocabulario, es menos superficie** (literales de producción), y la
asimetría resultante entre idiomas queda justificada por la medida en los DOS lados, no por argumento. Y **prueba
tu separador antes de creerte sus cifras**: el mío pre-blanqueaba las tres corrientes y reportaba 0 comentarios y 0
literales; lo cazó una sonda propia que revienta si alguna corriente se rompe — un extractor sin sonda que devuelve
0 informa "todo limpio" de todo. **Audita tu propio arreglo anterior con el mismo estándar que el defecto que
arregló**: un párrafo de cobertura escrito para sustituir una sobre-afirmación es exactamente donde se cuela otra,
porque nadie vuelve a leerlo — y compáralo contra lo que TÚ escribiste en el ledger ese día, que es la evidencia
que lo desmiente. **Verifica el enunciado de un hallazgo con un grep antes de implementarlo, no solo sus
símbolos**: aquí "existe dos veces" era "existe cuatro", y esa diferencia cambió la fase (se pinnea una tercera
copia y nace un hallazgo por la cuarta). Cuando una de las copias sea `private`, es **inalcanzable desde un test de
paridad**: regístrala con dueño en vez de subirle la visibilidad de paso. Al retirar copy prohibida, **describe la
forma vieja, no la cites**: un comentario que guarda el texto es un sitio más para el escáner y la licencia
documental que resucita el defecto. Corolarios de entorno, todos ya conocidos y todos vueltos a morder: un
`desktopTest NO-SOURCE` con `compileTestKotlinDesktop UP-TO-DATE` y **0 ficheros `.class`** frente a 5742 en main
es directorio de clases vaciado por la sesión concurrente, no una regresión; **lee el mensaje real del XML antes de
aceptar un resultado de mutación** — mi M28 dio 8 de 8 ROJO y el mensaje decía `NoClassDefFoundError`, así que la
corrida era INCONCLUSA, no una cobertura magnífica; `e: Daemon compilation failed: null` con stack de
`IncrementalCachesManager.close` y **cero errores `e:` de fuente** es la misma familia (se re-ejecuta, no se cambia
una línea); y el **sello del ledger puede moverse a mitad de fase** (231 → 237 hallazgos por otra sesión), así que
la escritura necesita compare-and-swap sobre el sello vivo, no sobre el que leíste al empezar.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/creativity/data/export/CreativityExporter.kt` (copy embarcada + KDoc que describe en vez de citar)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/copy/ForbiddenLexicon.kt` (`ENGLISH_CLAIMS` + dos cláusulas falsas retiradas del párrafo COVERAGE)
- `shared/src/desktopTest/.../core/copy/ShippedEnglishCopyGateTest.kt` (nuevo, 3 tests) · `.../core/copy/CopySafetyLexiconParityTest.kt` (6 → 8 tests)
- `shared/build.gradle.kts` (centinelas `COPY_SAFETY_MATCHER_PARITY_START/END`) · `shared/src/commonTest/.../creativity/data/export/CreativityExporterTest.kt` (2 aserciones que pinneaban la violación)
- `MINDBOOST_AUDIT_LEDGER.md` (`MB-EMO-0029` y `MB-EMO-0030` CLOSED, este último con enunciado corregido; `MB-EMO-0032` nuevo)

### 2026-08-28 — Fail-closed sobre el VALOR no es fail-closed sobre el HECHO: un gate que colapsa tres razones en un `null` borra justo la que habla de TU diseño

**Error detectado:**
Fase R12 del catálogo, sobre `MB-CATALOG-0019` —un hallazgo que había escrito yo mismo la fase anterior—.
Los **8 símbolos resultaron CIERTOS** y aun así el enunciado era falso en su parte más importante: decía que
`SELECTIVE_PROBE` «no puede sostener la afirmación de capacidad para la que existe», y **esa afirmación no
existe en el código**. `ProbeBank.forCapacity` no tiene ningún consumidor, `MetricChange` no lleva etiqueta de
capacidad y `TransferLine.label` etiqueta por métrica. Nadie une sonda→capacidad→veredicto, así que la sonda no
puede mal-etiquetar nada. Siguiendo el consumidor hasta el final apareció el defecto que SÍ cuelga de una
afirmación, y no estaba en el hallazgo: `EvaluationComparator` colapsaba **tres hechos distintos en el mismo
`null`** —(a) el retest no tiene valor, (b) lo medimos y la muestra no alcanzó el piso ratificado, (c) la métrica
no tiene piso—, y el caso (b) desaparecía del informe que se renderiza en **tres pantallas**: ni `strongEvidence`
ni `preliminary`, nada. El usuario no puede distinguir «esta sonda no mide eso» de «lo medimos y la muestra se
quedó corta», y solo el primero es un hecho sobre él.

**Causa raíz:**
R8 había decidido —correctamente— que un valor infra-muestreado **no puede cruzar**: su propio test lo declara,
«Absent, never a number (`null != 0`)». Pero fail-closed sobre el VALOR se implementó tirando también el
CONOCIMIENTO de que se tiró, y ese conocimiento es un hecho sobre NUESTRO muestreo, no sobre el usuario. El hueco
es estructural y no de un ejercicio: el motor marca `confident` con pisos EN SESIÓN de 3..5 (`MIN_N_CONGRUENCY=3`,
`MIN_N_POST_ERROR=3`, `MIN_N_RT=5`) mientras el piso CENTRAL exige 20, así que **toda** métrica gobernada puede
entrar en `perMetric` y caerse después en el gate de retest, en silencio. Y el «Aún sin cambio fiable» de la nota
promete alcanzabilidad: para una celda que no puede llegar a su piso a esa longitud de sonda, el «aún» es falso —
no lo arregla practicar más.

**Solución aplicada:**
`EvaluationResult.withheldForSample` (con default, así ningún call-site se rompe), poblado por un filtro **SEPARADO
del fold** —`baseline.perMetric.keys.filter { retestMetrics[it]?.measurementSufficient == false }`—, de modo que el
bloque `changes` queda **byte-idéntico** y los criterios de aborto «no cambia `changes`» y «no cambia
`hasReliableChange`» se satisfacen POR CONSTRUCCIÓN en vez de por test. `== false` y no `!= true`: `null` es «no hay
piso ratificado», que no es una medida retenida. `honestNote` **COMPONE** una cláusula sobre las dos cadenas
existentes, conservadas byte-idénticas, en vez de reescribirlas. Se rechazó por SEGUNDA vez crear un símbolo
sonda→capacidad (sin consumidor sería el anti-patrón de 2026-08-18) y se rechazó una sección de UI porque `label()`
cae a `else -> metric.name` en 12 de 16 métricas y filtraría enums crudos a copy en castellano. Los cuatro criterios
de aborto se declararon en el ADR ANTES del código; dos quedaron mecanizados por mutación x4.

**Regla futura:**
Cuando decidas que un valor no puede cruzar un gate, pregunta **si el HECHO de haberlo retenido también debe
quedarse fuera**: casi nunca. Tirar el número protege al usuario; tirar el conocimiento de que lo tiraste convierte
un límite de tu diseño en silencio, y el silencio se lee como un hecho sobre él (la forma **omisiva** de `null != 0`,
hermana de la atributiva de 2026-08-26). Ante un `?: return null` en un gate, **enumera cuántas razones distintas
caen en ese `null`**: si son más de una, al menos una habla de ti y no del usuario. Cuando añadas un hecho nuevo a
una ruta que emite afirmaciones, **compútalo FUERA del fold existente**: dejar el bloque viejo byte-idéntico es lo
que hace que «no altero el comportamiento» sea una propiedad estructural y no una esperanza verificada a posteriori;
y **compón** la copy nueva sobre la vieja en vez de reescribirla, porque cambiar el texto de un claim es decisión de
producto y revelar una omisión no lo es. Un hallazgo escrito **por ti** exige MÁS escrutinio, no menos: no tiene
revisor externo, y aquí sus 8 símbolos eran ciertos mientras su severidad estaba inventada. Corolarios operativos
que costaron tiempo real: (1) **un `BUILD SUCCESSFUL in 13s` con `desktopTest NO-SOURCE` sobre un directorio con 0
ficheros `.class` es un falso verde que NO se detecta leyendo el log** —hay que contar el output— y lo produjo un
compile fallido anterior que dejó el directorio vacío con la marca de «al día»; se arregla con `--rerun` sobre el
task de compilación, no con `clean`; (2) **un resultado de mutación que no puedes ATRIBUIR no es un resultado**: mi
M2 mató `matcherAccentFoldIsPinnedInBothDirections`, un test de la sesión paralela, así que se repitió exigiendo que
la suite objetivo apareciera en la ventana —y entonces sí mató el test correcto—; (3) al agregar XML de
`test-results` **filtra por marca de tiempo de esta corrida**, porque la sesión paralela escribe el MISMO directorio
y sus suites se cuelan como si fueran tuyas; (4) `NoSuchFileException: in-progress-results-*.bin` y `Could not pack
tree ... dirty-sources.txt` se suman a la familia de firmas de Gradle concurrente: se espera a que el árbol quede
libre (contar JVMs) y se re-ejecuta, no se cambia una línea.

**Archivos afectados:**
- `shared/src/commonMain/.../focus/domain/evaluation/EvaluationResult.kt` · `.../focus/domain/reporting/TransferReport.kt`
- `shared/src/desktopTest/.../focus/domain/evaluation/RciMeasurementSufficiencyTest.kt` · `shared/src/commonTest/.../focus/domain/reporting/TransferReportBuilderTest.kt` (ambos endurecidos EN SITIO, +3 tests cada uno)
- `docs/audits/ADR_WITHHELD_MEASUREMENT_2026-08-28.md` (nuevo, con §5 de correcciones) · `MINDBOOST_AUDIT_LEDGER.md` (`MB-CATALOG-0020` nuevo y CERRADO; `0021`/`0022` nuevos; `0019` alcance precisado)

### 2026-08-28 — Una prohibición escrita en el prompt de un subagente NO es un mecanismo: mi propio gate de salida invocó Gradle y destruyó la evidencia que auditaba

**Error detectado:**
Cerrada la medición de la fase (verde global demostrado: `4327 tests / 0F / 0E` con los 609 XML verificados frescos),
lancé el gate de salida obligatorio como workflow de 7 agentes **read-only**, con la prohibición en mayúsculas
repetida en el prompt de cada uno: *"NEVER invoke Gradle... a second Gradle invoker corrupts the shared worktree...
violating it invalidates your report"*, además de la lista explícita de herramientas permitidas. Los agentes
**invocaron Gradle igualmente**: lanzadores `gradlew` a las 19:23:24, 19:24:06 y 19:25:08, y
`shared/build/test-results/desktopTest` cayó de **609 XML a 2** en tres minutos. El revisor destruyó exactamente
los artefactos que existía para auditar. Lo delató una contradicción entre dos comandos —`ls` listaba 609 y `find`
sólo 8—, no ninguna alarma: sin esa discrepancia habría seguido citando un conteo cuyos ficheros ya no existían.

**Causa raíz:**
Confundí una instrucción con un control. Un subagente con la herramienta Bash **puede** ejecutar Gradle; que le
digas que no lo haga es disciplina, no mecanismo — la misma familia que "un comentario no es un gate" y que
"`confident = false` no es un guard porque existe un lector que lo ignora". El agravante es de orden: puse la
evidencia de la fase **dentro** del árbol que el revisor podía tocar, así que el único fallo posible del gate era
también el que borraba su objeto de estudio. Y hay una asimetría que no había visto: un revisor que corrompe un
build cuesta una re-ejecución; un revisor que borra artefactos de medición cuesta una afirmación que ya no se
puede volver a ganar, porque el árbol ha cambiado y esa corrida es irrepetible.

**Solución aplicada:**
`TaskStop` sobre el workflow en cuanto la discrepancia salió; nada se "arregló" ni se re-ejecutó. Se separó lo que
sobrevive de lo que no, y se dijo cuál es cuál: el **veredicto** del verde sigue sostenido por `dt_run.log`, que
vive en el scratchpad **fuera del repo** y conserva `11 actionable tasks: 11 executed`, la tarea sin sufijo y
`BUILD SUCCESSFUL` —y Gradle falla el build si falla un test, así que "0 failures" es estructural—; pero el
**conteo exacto 4327/0/0** quedó sin artefacto en disco y se reporta explícitamente como no re-verificable de
forma independiente. El incidente se registró como `MB-FOCUS-0083` (P1) **contra la propia fase que lo cometió**,
no como una nota al pie.

**Regla futura:**
Antes de lanzar cualquier revisor sobre un árbol compartido, **copia la evidencia FUERA del árbol** (logs, XML,
informes) y cita esa copia: la evidencia dentro del alcance del revisor es evidencia que el revisor puede borrar.
Y no confíes la exclusión de una herramienta al prompt: si un subagente no debe ejecutar Gradle, **quítale la
capacidad** (un `agentType` sin Bash, o una allowlist de herramientas), porque una prohibición en prosa no la
impone nadie — si la única barrera es que el agente obedezca, no hay barrera. Corolario de diagnóstico que aquí
salvó la fase: cuando dos comandos que deberían coincidir discrepen (`ls *.xml` = 609 frente a `find -name '*.xml'`
= 8), **no elijas el que te conviene**: uno de los dos está viendo un directorio que alguien está reescribiendo en
ese instante, y averiguar cuál es más urgente que el número. Y cuando un gate de salida destruya evidencia, la
salida honesta es declarar qué afirmación sobrevive y con qué soporte —el veredicto sí, el conteo no— en vez de
seguir citando el número porque lo mediste de verdad hace un rato.

**Archivos afectados:**
- (ninguno de producción — orquestación de subagentes; evidencia en `MINDBOOST_AUDIT_LEDGER.md` §2/§3, hallazgo `MB-FOCUS-0083`, task `TASK-2026-08-28-FOCUS-V5-S5-GATE`)

### 2026-08-28 — Un gate que NO CORRE puede reportarse como éxito: el exit code que ves es el del comando ENVOLVENTE, y siempre hay un informe rancio dispuesto a confirmarlo; y un verde global sobre un worktree compartido es una medición PERECEDERA atada a su snapshot

**Error detectado:**
Fase que debía convertir el subconjunto verificado de Focus V5 · S5 en un gate global. (1) Tras leer unos XML
me quedé con el shell dentro de `shared/build/test-results/desktopTest`; la llamada siguiente ejecutaba
`./gradlew.bat :shared:detekt`, que **ahí no existe**. El build salió **127** y, como el comando estaba envuelto
en `cmd >> log; echo "EXIT=$?"`, el harness reportó *"completed (exit code 0)"* —el del `echo`—. Y había un
`detekt.xml` de las **02:05:23**, de otro estado del árbol, diciendo **0 errores**: un artefacto verde listo para
una lectura que nadie había ganado. La reinvocación correcta tampoco ejecutó: `> Task :shared:detekt`
**FROM-CACHE** / `1 from cache`. (2) Ya con el gate corriendo de verdad, la pasada 1 dio **4327 tests / 0F / 0E**
con el árbol demostrablemente quieto; la pasada de REPETIBILIDAD, seis minutos después, falló en
`compileTestKotlinDesktop` con 4 errores `e:` sobre `withheldForSample`. Y al agregar los XML **el número seguía
diciendo 4327 / 0 / 0**: eran los ficheros de la pasada anterior. Sin el conteo de frescura habría reportado
verde una corrida que ni siquiera llegó a ejecutar los tests.

**Causa raíz:**
Un gate tiene tres partes que fallan por separado: que se INVOQUE, que se EJECUTE y que su ARTEFACTO sea de esta
corrida. El exit code sólo habla —a veces— de la primera, y envolverlo en un compuesto lo lava por completo:
`cmd; echo EXIT=$?` hace que el estado que el harness propaga sea el del `echo`. Peor: el cwd de la herramienta
**persiste entre llamadas**, así que un `cd` de un paso anterior puede sacar el gate de alcance sin que nada lo
anuncie. Y como los informes se escriben en sitio, un gate que no corre deja intacto el del pasado — la
combinación produce el peor de los estados posibles: verde disponible, ejecución cero. Lo mismo vale para el
agregado de XML, que es la evidencia canónica del repo: leerlo sin filtrar por ventana temporal es leer la
corrida anterior. (2) Y un verde no es una propiedad del repo sino de un SNAPSHOT: con otra sesión trabajando el
mismo worktree, el árbol puede pasar a no compilar a mitad de corrida porque el vecino está en la fase RED de su
TDD — un fallo legítimo que **no lleva ninguna firma de corrupción** y que, mal diagnosticado, invita a
re-ejecutar en bucle o a "arreglar" código ajeno.

**Solución aplicada:**
El artefacto se **BORRA antes** de invocar el gate (`rm -f detekt.xml`), lo que convierte la lectura rancia en
estructuralmente imposible en vez de depender de vigilancia. Se exigen las **tres señales** juntas:
`N actionable tasks: N executed`, la tarea **sin sufijo** (`UP-TO-DATE` y `FROM-CACHE` son ambos "no corrió") y
el mtime del artefacto dentro de la ventana; para los tests, además, `find -newermt` sobre los XML antes de
agregar. El propio gate se probó **VIVO por mutación**: una violación de `MaxLineLength` inyectada en
`TemporalWindowVerdict.kt` puso detekt en `FAILED` con 1 `<error>`, y la restauración se verificó byte a byte por
SHA-256. Cada invocación se abrazó con una huella `git status` antes/después: la pasada 1 quedó demostrada sobre
un árbol inalterado (`8b9fdf9c…` idéntica), y la pasada 2 quedó ANULADA por su propia huella, que probó que dos
ficheros ajenos cambiaron mientras corría. El fallo ajeno se **atribuyó** (`withheldForSample`: 0 apariciones en
`commonMain`, 2 en test ⇒ fase RED del vecino) y no se tocó una línea.

**Regla futura:**
Nunca leas el exit code de un gate a través de un comando envolvente: `cmd; echo EXIT=$?` propaga el estado del
`echo`, y un `cd` de un paso anterior sobrevive en el shell y puede dejar el binario fuera de alcance. **Borra el
informe ANTES de invocar el gate** — un gate que no corre deja el artefacto del pasado, y un verde heredado es
indistinguible de uno ganado. Amplía la regla de 2026-07-19: `FROM-CACHE` es tan "no corrió" como `UP-TO-DATE`, y
la evidencia son TRES señales (tarea ejecutada, tarea sin sufijo, artefacto dentro de la ventana), nunca una.
**Filtra los XML por ventana temporal antes de agregarlos**: un agregado sobre el directorio entero devuelve
felizmente el resultado de la corrida anterior, y ése es el falso verde más fácil de publicar. Cuando compartas
worktree con otra sesión, **abraza cada invocación con una huella `git status`**: si el árbol se movió, el
resultado no es rojo ni verde, es INADMISIBLE, y cita siempre el snapshot junto al verde. Y aprende a distinguir
un fallo de compilación por **WIP ajeno en fase RED** (símbolo con 0 apariciones en producción y N en test, sin
ninguna de las firmas de corrupción conocidas) de la corrupción por Gradle concurrente: el primero se ATRIBUYE y
se deja quieto, el segundo se re-ejecuta en serie. Firma nueva de esta última, a añadir al discriminador:
`Failed to clean up output files for task ':shared:compileKotlinDesktop'`.

**Archivos afectados:**
- (ninguno de producción — disciplina de verificación de gates; evidencia en `MINDBOOST_AUDIT_LEDGER.md` §3/§4, task `TASK-2026-08-28-FOCUS-V5-S5-GATE`, hallazgos `MB-FOCUS-0081` y `MB-FOCUS-0082`)

### 2026-08-27 — Un gate de léxico se ensancha MIDIENDO sus falsos positivos, no razonándolos; un test que EXIGE la copy prohibida la vuelve inarreglable; y un comentario que se concede una excepción que el invariante nunca dio es el documento que fabrica el defecto

**Error detectado:**
Cierre de `MB-EMO-0023`. El copy-safety scanner del build era ciego a las formas femenina y plural
(`\bclinico\b` no casa `clínica`), y sobre ese hueco vivían TRES disclaimers, uno de ellos renderizado
en producción. Al ir a arreglarlo aparecieron tres cosas que el enunciado del hallazgo no contenía. (1)
Ensanchar el matcher NO es gratis: de los 12 hits que produce una regla de flexión general, **3 son
falsos positivos sobre copy honesta** — `cuota`→`cuotas` marca las cuotas de CONDICIÓN (muestreo) de
`VectorFilterPlanner`, `stake`→`stakes` marca "low-stakes" en KDoc inglés, y singularizar `odds`
produce la palabra inglesa corriente "odd". (2) Uno de los nueve sitios era un TEST que afirmaba
`assertTrue(s.disclaimer.contains("clínica"))`: la copy prohibida no se podía arreglar sin cambiar el
test que la pinneaba. (3) La causa de los tres disclaimers era **un comentario**: declaraba
"\"evaluación clínica\" is an allowed disclaimer phrase" en la línea de arriba y "never names a forbidden
lexeme even to negate it" en la de abajo — dos afirmaciones que no pueden ser ciertas a la vez, y el
gate le daba la razón a la primera porque no veía el femenino.

**Causa raíz:**
(1) Un matcher de léxico tiene una asimetría que se olvida: un falso NEGATIVO embarca un claim, pero un
falso POSITIVO rompe el build sobre copy legítima, y ambos salen de la MISMA decisión de morfología. La
lista ya contenía palabras sueltas polisémicas (`cuota`, `stake`, `bet`, `odds`) cuyo otro sentido usa
el repo a diario; flexionarlas en bloque las activa. Ninguna lectura del lexicon predice cuáles: sólo
pasarlas por el árbol. (2) Un test que afirma la PRESENCIA de un lexema prohibido invierte el gate: deja
de proteger la copy y pasa a protegerla del arreglo. (3) Un comentario que se concede una excepción que
el invariante nunca otorgó es peor que copy mala, porque la copy mala es un sitio y el comentario es una
licencia: de ahí salieron tres.

**Solución aplicada:**
Cada término DECLARA cómo flexiona (`Inflection` = `LITERAL`/`NUMBER`/`GENDER_NUMBER`) **sin default** —
un default restauraría en silencio el masculino-singular para el siguiente adjetivo que alguien añada—;
`ALL` pasa de 63 lemas a 89 formas y el bloque de paridad de Gradle se REGENERA de las declaraciones. La
regla final genera un SUBCONJUNTO de las 75 formas medidas, así que ninguna forma llega sin medir: **9
hits reales, 0 falsos positivos**, y 0 hits sobre los 1756 ficheros Kotlin de `:shared`. Los 3 falsos
positivos quedan `LITERAL` **con su fichero y su línea en el KDoc**, y la cobertura se pinnea en AMBAS
direcciones: los adjetivos cubren femenino y plural, y los `LITERAL` no se pueden ensanchar. La copy se
reescribe diciendo lo que la medida ES ("Refleja tu práctica en el entrenamiento; es orientativo"), que
es la única forma que la regla anti-negación permite y además se lee mejor que una negación. El KDoc que
sobre-afirmaba su cobertura declara ahora lo que NO cubre (inglés, concordancia dentro de frases,
conjugación). Tres mutaciones: M25 (restaurar la copy vieja) deja el escáner ROJO — la prueba de punta a
punta—, M23 y M24 rompen los dos pines de la regla.

**Regla futura:**
Antes de ensanchar un matcher de léxico (flexión, stemming, sinónimos), **mide los falsos positivos sobre
el árbol real ANTES de implementar**, y deja que la medición decida el diseño: la salida correcta no es
una regla global sino una **clase declarada por término, sin default**, con las excepciones anotadas con
el fichero y la línea que las justifica — una excepción sin su evidencia es indistinguible de un
descuido. Comprueba que tu regla final genere un SUBCONJUNTO de lo que mediste; si genera una forma que
no pasó por el árbol, no la has medido. Y cuando declares una excepción, escribe también la mitad
discriminante que impide ensancharla: sin ella, "todo a LITERAL" deja tu suite verde y el gate vacío.
**Trata un test que EXIGE la presencia de copy prohibida como parte del defecto, no como contexto**: si
para arreglar la copy tienes que cambiar un test, pregúntate si ese test pinneaba la garantía o la
violación — y sustitúyelo por la propiedad que la copy existe para dar (que acota la lectura), no por el
literal. Cuando un comentario se conceda una excepción a un invariante, **búscale los descendientes**:
una licencia escrita produce varios sitios, y arreglar los sitios sin borrar la licencia los devuelve.
Prefiere copy que dice lo que algo ES a copy que niega lo que no es: satisface el invariante
estructuralmente en vez de por vigilancia. Corolarios de entorno, todos ya conocidos y todos vueltos a
morder: `BUILD SUCCESSFUL` con la tarea `UP-TO-DATE` no es evidencia (aquí `compileKotlinDesktop` salió
verde por el build de OTRA sesión; `--rerun` lo destapó); `Could not delete .../classes/kotlin/desktop/main`
más corrupción de storage incremental es Gradle concurrente sobre el worktree compartido —diagnosticar
con la línea de comandos del proceso ajeno, esperar y re-ejecutar en serie, no cambiar una línea—; y un
fallo de test cuya aserción dice literalmente "the set of block sizes production can mint changed" sobre
ficheros que tu diff no toca es WIP ajeno: se atribuye y se reporta, no se arregla.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/copy/ForbiddenLexicon.kt` (mecanismo + KDoc de cobertura)
- `shared/build.gradle.kts` (bloque de paridad regenerado, no escrito a mano) · `shared/src/desktopTest/.../core/copy/CopySafetyLexiconParityTest.kt` (+3 tests)
- `shared/src/commonMain/.../speed/domain/score/{GoNoGoFeedback,SpeedScoreEngine}.kt` · `.../speed/presentation/{SpeedDashboardModel,SpeedStrings}.kt`
- `shared/src/commonMain/.../emotions/coach/EmotionCoachSummary.kt` · `.../emotions/domain/feedback/EmotionFeedbackCopy.kt` · `.../emotions/presentation/EmotionStrings.kt` · `.../emotions/presentation/ui/components/EmotionHistoryCard.kt`
- `shared/src/commonTest/.../emotions/coach/EmotionCoachSummaryTest.kt` · `MINDBOOST_AUDIT_LEDGER.md` (`MB-EMO-0023` CLOSED; `MB-EMO-0029`..`MB-EMO-0031` nuevos)

### 2026-08-27 — Un enunciado CIERTO puede tener una consecuencia FALSA: verifica la TRAZA DEL CONSUMIDOR, y busca la misma aritmética donde sí cuelga una afirmación

**Error detectado:**
Fase R11 del catálogo, sobre `MB-CATALOG-0015` («el bloque E2 del baseline reparte 12 ensayos incongruentes
frente a un piso de 20, de forma determinista»). Los **7 símbolos comprobables resultaron CIERTOS**. Partiendo de
ahí formulé la consecuencia obvia —ese valor infra-muestreado se cuela como el lado `pre` del RCI, la única
afirmación de capacidad del producto— y estuve a punto de registrarla como hallazgo vivo. Al trazar el consumidor
resultó **FALSA**: `BaselineAssembler.assemble` construye el snapshot con `probeBlock.metrics`, es decir con el
bloque de SONDA, nunca con los de calibración; y `DefaultCalibrationEngine.initialDifficulty` deriva la dificultad
inicial **solo de accuracy**, no de la métrica gobernante. El `CONGRUENCY_EFFECT` de E2 se calcula, se sella
correctamente con `measurementSufficient = false`… y no lo consume nadie. El déficit es real y su consecuencia hoy
es NULA.

**Causa raíz:**
Verificar los símbolos de un hallazgo (que existan, que los números salgan) NO verifica su consecuencia. Un
enunciado aritmético puede ser exacto y su severidad estar inventada, porque la severidad vive en el CONSUMIDOR y
el consumidor no aparece en el enunciado. Y el corolario que importa: si la aritmética es real pero ahí no cuelga
nada, casi siempre cuelga de otro sitio. Aquí `ProbeBank.fixedProbeDifficulty` fija carga perceptual y
incertidumbre temporal y **deja `interference` en `MIN`**, así que `SELECTIVE_PROBE` (FLANKER, enrutado al
generador de E2) corre a la misma proporción 0.20 y su celda menor es **11 de 55**. La diferencia es que una
sonda reservada es **el único instrumento que el charter admite para una afirmación de capacidad** (invariante
#7). El defecto consecuente estaba en la sonda, no en el bloque de entrenamiento, y el hallazgo no la mencionaba.

**Solución aplicada:**
R11 no toca producción: **cero cambio**. Se rechazó deliberadamente crear un símbolo `BaselineReachability`
porque nada en runtime necesita preguntarlo —`MeasurementReachability` ya responde— y un símbolo sin consumidor
reproduce el anti-patrón de 2026-08-18 («lo que se conserva para la fase siguiente se lee como capacidad
existente»). La barrera es un test DERIVADO del protocolo (`BaselineProtocols` + `ProbeBank` + `ExerciseCatalog` +
`MeasurementReachability`), que va a ROJO si alguien mueve un tamaño de bloque, la dificultad de calibración o
`fixedProbeDifficulty`. El guard preexistente de `BaselineAssemblerTest` **no discriminaba** («sonda» de «sonda +
calibración», porque el bloque de calibración llevaba `metrics = emptyMap()`): se endureció EN SITIO con una
métrica CONFIDENT de calibración que debe NO llegar al snapshot. Mutación x3 con restauración byte-idéntica por
SHA-256: 5 ROJOS de 12 con atribución 1-a-1, y las dos mitades discriminantes en verde.

**Regla futura:**
Tras verificar los símbolos de un hallazgo, **traza su CONSUMIDOR hasta el final antes de creerte su severidad**:
formula la consecuencia que te parece obvia y ve a refutarla: aquí «el pre del RCI» era falso y encontrarlo costó
dos lecturas. Si la aritmética es real pero no la consume nadie, **busca la misma aritmética donde sí cuelga una
afirmación** — en este repo eso significa mirar las SONDAS, no los bloques de entrenamiento, porque la sonda es el
único instrumento que puede sostener una afirmación de capacidad; y un defecto en una sonda registrada pero no
seleccionada está ARMADO, no muerto. Cuando propongas subir una muestra para cruzar un piso, **calcula contra la
asimetría del límite SUPERIOR**: la reachability es un techo (solo los HIT llenan las celdas), así que aterrizar
justo en el piso falla en la práctica — aquí la respuesta ingenua (bloque a 98, celda 20) queda POR DEBAJO del
piso al 90 % de accuracy (hacen falta 23 planificados), mientras fijar `interference = 0.75` da 30/30 sin añadir
un solo ensayo; y la superficie **no es monótona** (a 1.00 la celda menor cae a 24, peor que a 0.75). Ninguno de
esos dos hechos sale de razonar: salen de calcular. Corolarios que costaron tiempo real: (1) **un pre-check que
parsea CERO términos informa «limpio» de todo** — mi propio comprobador de léxico extraía comillas sueltas, cazó
el sufijo de flexión `"as"` como si fuera un lexema y luego, al afinarlo, sacó 0 lemas y dijo LIMPIO; todo
extractor necesita un piso anti-vacuidad y una mitad discriminante (correrlo sobre el propio lexicon debe dar
hits); (2) **un anchor de mutación con `\n` sobre fuentes CRLF no casa, y la mutación se vuelve un no-op que
parece un test que pasa** — normaliza el anchor al terminador del fichero y **exige exactamente 1 ocurrencia**, o
estarás «probando» cobertura sin cambiar un byte; (3) `koverFindJar FAILED` con `FileAlreadyExistsException` sobre
el jar del agente en la caché de Gradle es la firma de sesión concurrente (ya documentada en R7), no una
regresión: se diagnostica y se reintenta sin `--rerun-tasks`, demostrando la ejecución por la línea de tarea sin
sufijo + XML frescos agregados.

**Archivos afectados:**
- `shared/src/desktopTest/.../focus/domain/evaluation/ProbeMeasurementReachabilityTest.kt` (nuevo)
- `shared/src/commonTest/.../focus/domain/evaluation/BaselineAssemblerTest.kt` (guard endurecido en sitio)
- `docs/audits/ADR_PROBE_REACHABILITY_2026-08-26.md` (nuevo, con §5 de correcciones) · `MINDBOOST_AUDIT_LEDGER.md` (`MB-CATALOG-0019` nuevo; `0015` alcance precisado; `0013` causa precisada)

### 2026-08-26 — Encender un gate no es cubrirlo: un lexicon en masculino singular es fail-open en femenino y plural; una lectura cuya celda menor no alcanza el piso NO se arregla creciendo el bloque; y una mutación que vuelve VERDE denuncia al test, no al código

**Error detectado:**
Emociones · Ola 3 (R3, el Separador con Modo B). (1) La fase empezó cerrando `MB-EMO-0016` —añadir `emotions` al
copy-safety scanner del build—, y el cierre parecía limpio: mutación roja, tres escáneres verdes. Al ir a escribir
en el ledger una cifra que recordaba de memoria, la medí, y el escáner recién encendido resultó **ciego a las
formas femenina y plural**: `\bclinico\b` no casa `clínica`. Medido de forma reproducible sobre los 9 módulos
escaneados × 3 source sets: 8 ficheros, **7 en `commonMain`** (speed=4, emotions=3), y entre ellos
`EmotionStrings.HISTORY_NOT_CLINICAL = "No es una evaluación clínica."`, **renderizado en producción** — un
disclaimer que nombra el lexema para NEGARLO, que es exactamente lo que la regla anti-negación prohíbe, con el
gate en verde. Agravante: el KDoc del propio `ForbiddenLexicon` AFIRMA que "crosses hyphens/negations, so
non-clinical … are both caught" — cierto para el masculino, falso para la forma en que el módulo escribe de
verdad sus disclaimers. (2) La auditoría pide para el índice de robustez "3 bloques × 16", y el 16 no compra la
lectura que lo motiva. (3) De seis mutaciones, **DOS volvieron VERDES y las dos denunciaban tests míos, no
código**: sustituir la lectura del gate central por un literal del mismo valor no la ve nadie, y decidir la
dirección por el estimador puntual en vez de por el intervalo tampoco — siendo esa la regla CENTRAL del kernel.

**Causa raíz:**
(1) Un gate tiene tres partes, no dos: el mecanismo, aquello contra lo que compara, y **la morfología con que
compara**. Las dos primeras ya estaban en el radar del repo; la tercera no, y es la que convierte "el módulo está
escaneado" en algo distinto de "el módulo está cubierto". Un lexicon de lemas en masculino singular es fail-open
en todas las flexiones, y la flexión femenina es precisamente la que usan los disclaimers (`evaluación clínica`,
`valoración diagnóstica`), así que el hueco está alineado con el peor caso en vez de con el caso raro. (2) Un
piso ratificado por CELDA no se satisface haciendo el bloque más largo si el bloque entero ES una celda: la celda
de calma de una sesión es un bloque, así que crecer 10→16 mueve el número sin cruzar el piso. (3) Una mutación
sólo prueba algo si existe un test cuyo predicado la distinga: un test que compara contra el gate central no puede
separar "lee el gate" de "copia su valor" (ambos coinciden en todo el dominio), y un veredicto de dirección
juzgado por el punto coincide con el juzgado por el intervalo en todas partes MENOS en la banda que importa —
justo la que ningún fixture visitaba.

**Solución aplicada:**
(1) El hallazgo se REGISTRA con su evidencia exacta (script reproducible + `fichero:línea` del render) como P1 y
NO se arregla aquí: cubrir el hueco obliga a reescribir copy de otro módulo y a decidir el léxico, que no es un
paso de R3 — pero `MB-EMO-0016` se cierra diciendo explícitamente que **el cierre significa que el módulo SE
ESCANEA, no que su copy esté limpia**, con puntero al hueco. (2) La aritmética se calcula ANTES de implementar y
decide: con una caída real de .90 a .70, el intervalo es [0.56, 1.21] a 10 ensayos de calma y [0.61, 1.12] a 16 —
**los dos contienen el 1**; la sesión más pequeña que resuelve ese efecto necesita 120 ensayos. Así que el índice
se AGRUPA entre sesiones (`crossSessionAllowed = true` en el propio spec) y el bloque no crece: divergencia
declarada frente a la auditoría, con su aritmética, y mecanizada en un test que fija el rechazo a AMBAS
geometrías. (3) Las dos mutaciones verdes se convierten en trabajo: el test que sobre-afirmaba se renombra a lo
que comprueba y **declara su techo** ("esto NO prueba que lea el gate en runtime; verificado por mutación"), y el
guard que faltaba se escribe con el par que lo hace discriminante — mismo índice .778, una fila al piso (intervalo
que aún cruza el 1 ⇒ `NO_DETECTABLE_COST`) y otra con cinco veces la evidencia (⇒ `PRESSURE_COSTS_ACCURACY`).

**Regla futura:**
Al encender un gate de léxico sobre un módulo nuevo, **mide su cobertura MORFOLÓGICA antes de cerrar el hallazgo**:
un lexicon de lemas en masculino singular no ve femenino ni plural, y son las flexiones que la copy defensiva usa.
Cerrar "el módulo entra en la lista" es legítimo, pero el cierre debe decir en su propia fila que no afirma que la
copy esté limpia — un cierre que se lee como cobertura es peor que un hallazgo abierto. Y si el KDoc del propio
lexicon afirma cobertura ("negations are caught"), compruébalo contra la forma real que el repo escribe, no contra
el ejemplo del comentario. Cuando un piso ratificado sea POR CELDA, pregunta **qué es una celda** antes de
proponer crecer nada: si la celda es un bloque entero, alargar el bloque mueve el número sin cruzar el piso, y la
salida es agrupar (si el spec lo permite) o declarar que la lectura no cabe en una sesión — calculado, no razonado.
Y trata **toda mutación que vuelva VERDE como un hallazgo sobre tus tests**: no la descartes como "esa no aplica".
Dos preguntas la convierten en trabajo: ¿existe algún fixture en la banda donde las dos implementaciones difieren?
(si el punto y el intervalo coinciden en todo tu dominio de prueba, no estás probando la regla que dice tu KDoc); y
¿puede un test de comportamiento distinguir siquiera estas dos versiones? Si la respuesta es no —leer una autoridad
central frente a copiar su valor es indistinguible desde fuera—, **renombra el test a lo que sí comprueba y declara
el techo** en vez de dejar el nombre afirmando la garantía que no da. Corolarios de entorno que costaron tiempo:
`python3` es el stub de Microsoft Store (usa Node); los literales acentuados se corrompen al pasar por el shell,
así que un script que folde acentos va a un FICHERO y no inline (un one-liner "midió" 0 ficheros donde había 8, y
la diferencia era la codificación, no el repo); y una fila de evento del ledger se añade al FINAL de su sección,
nunca tras un ancla a mitad de un lote anterior, o el validador lee un evento posterior como el estado
materializado y el registro se contradice a sí mismo (lo detectó `validate-audit-ledger.mjs`, no yo).

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions/domain/metrics/SeparatorRobustness.kt` (nuevo)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions/domain/exercise/{SeparatorDifficulty,EM3SeparadorGenerator}.kt` · `.../domain/metrics/EmotionHoldoutEvidence.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions/presentation/{EmotionSessionStateHolder,state/EmotionUiState,ui/components/EmotionFourCategoryField}.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions/data/{dto/EmotionDtos,local/EmotionAggregateFactory}.kt` · `shared/build.gradle.kts`
- `shared/src/desktopTest/.../emotions/domain/metrics/SeparatorRobustnessTest.kt` · `.../presentation/EmotionOptionOrderTest.kt` · `shared/src/commonTest/.../emotions/data/local/EmotionAggregateRobustnessTest.kt`
- `MINDBOOST_AUDIT_LEDGER.md` (`MB-EMO-0016` CLOSED; `MB-EMO-0023`..`MB-EMO-0028` nuevos)

### 2026-08-26 — Una escalera que alivia en el MISMO ensayo fabrica el efecto post-error que mide; un paso de UI que RELEE su estado desde `uiState` desaparece en silencio cuando otra capa sostiene la pantalla; y una mitad discriminante generalizada se DERIVA de la regla, no de una propiedad que solo coincide

**Error detectado:**
Emociones · Ola 3 (fusiones R1/R2). (1) La escalera ponderada up/down que fija la tasa de error de EM2 aplicaba el
resultado del ensayo en curso ANTES de presentar el siguiente, asi que los ensayos post-error se presentaban
sistematicamente mas faciles que los post-correcto: la "recuperacion" que la metrica PRIMARIA del ejercicio mide
contenia la ayuda que el propio ejercicio acababa de dar, en la direccion halagadora. (2) `showErrorMarker` hacia
`uiState as? Running ?: return`, pero `present()` invoca antes la captura de calibracion de Ola 1, que deja
`ConfidencePrompt` en pantalla mientras espera; el marcador neutro de post-error — la unica pieza de feedback
alrededor de la que R2 esta construido — **no llegaba NUNCA** a la pantalla en ningun ensayo con tira de
confianza, con la suite entera en verde. (3) Al recortar el menu, la mitad discriminante del test de tamaño de
bloque disparo su propio guard de vacuidad (ningun ejercicio implementado gobierna ya sobre comision) y se
generalizo MAL **dos veces seguidas**: primero contra la metrica gobernante — falso para EM1, que se dimensiona
por la celda de tonos de su delta de SSRT y no por `HIT_RATE` — y luego sobre todos los implementados — falso
para EM3, que esta EN el tamaño base, de modo que afirmar que se muere ahi **contradecia la asercion primaria del
mismo fichero**.

**Causa raiz:**
(1) Una escalera tiene dos tiempos que se confunden con facilidad: cuando OBSERVA un resultado y cuando lo APLICA.
Si coinciden, la dificultad presentada es funcion del resultado inmediatamente anterior, que es exactamente la
variable que una metrica post-error compara. El instrumento se vuelve parte del efecto. (2) Un paso de UI que
recupera su propio estado leyendolo de vuelta del contenedor global asume que nadie mas puede tener la pantalla;
en cuanto una capa transversal la ocupa legitimamente, el `?: return` convierte "otro tiene la pantalla" en "no
hay nada que hacer" y el paso desaparece sin error, sin log y sin test rojo. (3) Una mitad discriminante escrita
para UN caso concreto codifica, sin decirlo, propiedades que en ese caso coinciden; al generalizarla se conserva
la coincidencia en vez de la regla.

**Solucion aplicada:**
(1) `AccuracyStaircase.onOutcome` aplica el resultado ANTERIOR y retiene el actual: la dificultad presentada pasa
a ser funcion de resultados de hasta dos ensayos atras, el residuo marginal corre en direccion CONSERVADORA
(post-error algo mas dificil) y el punto fijo no se mueve, solo crece la varianza. El test construye la
referencia SIN retardo **dentro del mismo bucle** que la mitad discriminante, asi que primero reproduce el
confound y luego lo mide. (2) El modelo se PASA como parametro y el guard pasa a ser "sigue dentro del flujo de
ensayo"; se rechazo la correccion alternativa (mover el marcador antes de la tira), porque decirle al usuario que
fallo y preguntarle despues como de seguro estaba destruye el juicio de calibracion — y esa ordenacion se pineo
con una asercion propia (`markerBeforeStrip`) para que nadie la "arregle" al reves. (3) El conjunto se DERIVA de
la regla de dimensionado (solo los ejercicios cuyo bloque se agrando) y la metrica de cada uno sale de una tabla
`SIZE_DRIVERS` que declara de que lectura salio su tamaño, con la gobernante como defecto. Cinco mutaciones
ejecutadas en la fase, todas con restauracion byte-identica por SHA-256.

**Regla futura:**
Cuando un instrumento adaptativo comparta variable con la metrica que el ejercicio afirma medir, SEPARA el
instante en que observa del instante en que aplica: si la escalera se mueve con el resultado que la metrica
compara, el numero que publicas incluye tu propia ayuda. Comprueba la direccion del residuo y quedate con la
CONSERVADORA. Un paso de UI nunca debe recuperar su estado releyendolo del contenedor global: **pasalo**, y haz
que el guard afirme la precondicion real ("sigo en el flujo"), no la identidad del estado — un `as? X ?: return`
sobre estado compartido es un fallo silencioso disfrazado de defensa, y su sintoma es que solo un test que
CONDUCE la sesion entera lo ve. Cuando una correccion tenga dos formas y una de ellas destruya una medida
vecina, pinea el ORDEN con su propia asercion, o la version equivocada volvera como "arreglo". Al generalizar una
mitad discriminante que se puso vacua, no la reescribas contra la propiedad que el caso viejo tenia: **derivala
de la regla que estas probando** y comprueba que no contradice la asercion primaria del mismo fichero — si el
sujeto ya esta EN el valor que usas como contrafactual, no hay contrafactual que afirmar. Y un tamaño puede
derivarse de una lectura distinta de la gobernante (la celda MENOR que hay que llenar): declararlo en una tabla
evita afirmar una justificacion que el diseño nunca tuvo.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/staircase/{AccuracyStaircase,StopSignalTracker}.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions/presentation/EmotionSessionStateHolder.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions/domain/{exercise/EmotionExerciseGenerators,catalog/EmotionExerciseCatalog,feedback/EmotionNextRecommendationEngine}.kt`
- `shared/src/commonTest/.../core/staircase/AccuracyStaircaseTest.kt` · `.../emotions/presentation/EmotionRecoverySessionTest.kt` · `.../emotions/domain/exercise/EmotionBlockSizeSufficiencyTest.kt` · `.../emotions/domain/catalog/EmotionExerciseCatalogConsistencyTest.kt`
- `MINDBOOST_AUDIT_LEDGER.md` (`MB-EMO-0018`..`MB-EMO-0022`)

### 2026-08-26 — Cuando VARIOS hechos deciden juntos si una afirmación puede existir y ningún símbolo los relaciona, `null != 0` reaparece en su forma ATRIBUTIVA: el límite de tu diseño se le atribuye al usuario

**Error detectado:**
Fase R10 del catálogo. El hallazgo `MB-CATALOG-0017` decía que «la dificultad mueve en silencio la
elegibilidad de una afirmación de medida» (E8: 88.3% de sesiones elegibles a carga mínima, 9.3% a máxima).
Los 6 símbolos del enunciado resultaron CIERTOS, pero al calcular la superficie completa apareció algo que el
hallazgo no contenía y que lo supera: la variable dominante **no es la dificultad, es el TAMAÑO DE BLOQUE**, y
lo elige el USUARIO al escoger los minutos de sesión (`coreMs / 3_800` acotado a `[24,160]`: 55 / 94 / 157 /
160 ensayos). A n=157 todo es medible en todo el eje; a n=55 casi nada. Y E5 a n=55 resultó **NO MONÓTONO**:
deniega en AMBOS extremos porque a interferencia máxima la celda *congruente* cae a **19**, uno por debajo del
piso. Ningún razonamiento verbal produce ese 19; solo el cálculo.

**Causa raíz:**
Tres hechos —el piso ratificado (`MinSampleGate`), la proporción de condición que dicta la dificultad, y el
tamaño de bloque que dicta la plantilla— determinan JUNTOS si una afirmación sobre el usuario puede existir, y
**ningún símbolo del repo relacionaba los tres**. No había ningún cálculo que corregir: cada pieza hacía bien
su trabajo. El defecto era de DECLARACIÓN, y su consecuencia es la familia `null != 0` en una forma nueva: la
**atributiva**. Cuando una puntuación de diferencia sale infra-muestreada, «la sesión de este usuario se quedó
corta» (hecho sobre la persona) y «la forma planificada nunca pudo llegar» (hecho sobre NUESTRO diseño) son
indistinguibles — y presentar el segundo como el primero es exactamente lo que el charter prohíbe. Mecanismo
estructural detrás: para los SEIS ejercicios gobernados, el eje que el motor adaptativo mueve
(`ExerciseCatalog.governingAxis` vía `stepUp`/`stepDown`) es EXACTAMENTE el eje que gobierna su proporción de
condición, así que el motor, haciendo lo único que sabe hacer, redimensiona las celdas de la comparación.

**Solución aplicada:**
`MeasurementReachability.verdictFor(exerciseId, trialCount, difficulty)`: autoridad pura que responde qué
puede sostener un bloque PLANIFICADO, antes de que nadie lo juegue. Es un **LÍMITE SUPERIOR** y la asimetría es
el punto: la muestra realizada solo puede ser menor (solo los HIT alimentan las celdas de RT), así que
`reachable == false` significa IMPOSIBLE antes del primer ensayo y `reachable == true` significa posible,
jamás ganado. Tri-estado como `MeasurementSufficiency` (`null` = «no aplica», nunca reutilizado como «no»).
**Cero aritmética duplicada**: llama a `conditionQuota`, la misma función que el generador. **Cero números
movidos**: ni una proporción, ni un rango de eje, ni un `coreMs`, ni la tabla central (anti-patrón de R7); lo
que exige decisión de producto se registró (`MB-CATALOG-0018`) en vez de ejecutarse. El set gobernado se
DERIVA de `MeasurementSufficiency.central`; los tamaños de bloque se DERIVAN de `SessionTemplate` +
`BaselineProtocols`. Mutación x4 con restauración byte-idéntica por SHA-256.

**Regla futura:**
Cuando una afirmación sobre el usuario dependa de VARIOS hechos que viven en símbolos distintos (un piso, un
parámetro que la dificultad mueve, un tamaño que la plantilla fija), **crea el símbolo que los relaciona y haz
que responda ANTES de recoger datos**: sin él, un resultado insuficiente mezcla un hecho sobre la persona con
un hecho sobre tu diseño, y el segundo se le atribuye a ella. Declara explícitamente si tu veredicto es un
límite SUPERIOR o INFERIOR, porque de eso depende qué significa cada valor. Y **calcula la superficie completa
antes de creerte el enunciado**: aquí el cálculo produjo tres hechos que el hallazgo no contenía, incluido un
no-monótono que ningún razonamiento verbal alcanza (una celda a 19 frente a un piso de 20). Corolarios que
costaron tiempo real en esta fase: (1) **una tabla pinneada se DERIVA o se CALCULA, jamás se escribe a mano** —
mis tres cifras de la fila n=75 eran falsas y el ORDEN del pin lo decide el `sortedBy { it.name }` del enum
(`E13_` ordena ANTES que `E2_`), no el orden en que uno las lee; (2) **no formatees un índice pasando por
`Double`** (`(0.29*100).toInt()` da 28), formatea desde el entero; (3) **la evidencia de un gate se identifica
por la RUTA EXACTA del artefacto y por el estado del build** — mi detector de «fallo de infraestructura»
matcheaba la línea normal `> Task :shared:koverFindJar` y mi ruta de XML incluía un `[desktop]` que solo está en
el atributo `name=`, no en el nombre de fichero, así que leí dos veces un XML AUSENTE o RANCIO como si fuera un
resultado (un verde falso y un rojo falso); (4) con dos sesiones compilando el MISMO worktree, `Could not
delete .../classes/...`, `Could not add file ... a ZIP` y `Detected multiple Kotlin daemon sessions` son
corrupción, no regresiones — y la otra sesión puede BORRAR `test-results/desktopTest` a mitad de tu agregación,
lo que inutiliza el XML como evidencia agregada: entonces la evidencia honesta es la consola de TU invocación,
y se reporta como tal en vez de inventar un conteo; (5) escribe el ledger con `scripts/ledger-lock.mjs`, que ya
existe y da exclusión mutua real con fencing tokens.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/metrics/MeasurementReachability.kt` (nuevo)
- `shared/src/desktopTest/.../focus/domain/metrics/MeasurementReachabilityTest.kt` (nuevo) · `.../focus/domain/exercise/ConditionScheduleTest.kt`
- `docs/audits/ADR_MEASUREMENT_REACHABILITY_2026-08-20.md` (nuevo, con §8 de correcciones) · `MINDBOOST_AUDIT_LEDGER.md` (`MB-CATALOG-0017` CLOSED; `0018` nuevo; `0016` precisado)

### 2026-08-26 — Un documento VIGENTE cuya cola está SUPERSEDED es más peligroso que uno retirado entero; y antes de mecanizar un invariante hay que comprobar si YA está mecanizado

**Error detectado:**
Focus V5 · S5 · Paso 7 · disposición de `MB-FOCUS-0076` (`DegradedUsable` es una distinción sin
consecuencia). Tres defectos, todos míos, y ninguno lo vio una suite verde. (1) La razón por la que el
hallazgo seguía ABIERTO —«requiere la aritmética del tier `rt-infl-1` del ADR»— citaba una sección que
**no existe en el contrato vigente**: `ADR_FOCUS_TEMPORAL_QUALITY.md` marca en UNA sola línea que todas
sus secciones de numeración llana son historia R2 superseded, y las 4 apariciones de `rt-infl-1` están
todas por debajo de esa línea. El KDoc que escribí para cerrarlo repetía la misma cita (`§4.2`, `§9`).
(2) Añadí un fichero de test para el invariante «toda causa de exclusión tiene productor vivo» que era
un **DUPLICADO**: Paso 5 ya lo había mecanizado en `TemporalClassPolicyTest`, y el propio ADR lo decía.
(3) Mi pin de la equivalencia tenía un guard anti-vacuidad **tautológico** (contaba iteraciones del
bucle, es decir reafirmaba el rango) y arrancaba en `n = 1`, que son 60 frames: por debajo de
`MIN_FRAMES_SESSION`, así que **ambos brazos salían T0** y la igualdad se cumplía por la razón
equivocada.

**Causa raíz:**
Un documento retirado entero se ignora; uno **vigente por fuera y medio retirado por dentro** se cita,
porque su cabecera dice VIGENTE y la frontera interna es una línea de texto que nada mecaniza. Es el
aprendizaje 2026-08-18 («un protocolo caducado es una instrucción viva») un nivel más arriba: allí el
documento entero estaba caducado, aquí sólo su cola, y eso lo hace más difícil de ver. Agravante propio:
cuando fui a **verificar** la cita, acoté la sonda con `sed -n '/^## R3-0/,$p'`, que llega hasta EOF e
**incluye el bloque HISTORIA** — el conteo salió 4 de 4 y "demostró" exactamente lo contrario de lo que
pasa. Y (2) es la otra cara: asumí que un invariante que el gate de salida reclamaba no estaba mecanizado
sin buscar el guard existente, así que fabriqué un segundo dueño para el mismo invariante.

**Solución aplicada:**
(1) El hallazgo se cierra **retirando la afirmación**, no implementando el efecto: la autoridad viva
(R3-11 / `D-R3-02` CERRADA) deja `excludedFraction` y las simulaciones en SOMBRA y declara que ningún
diagnóstico temporal produce evidencia de medición en S5 — luego no existe afirmación de medida que una
ventana degradada pueda ablandar, y no hay aritmética que congelar. Cero cambio de comportamiento; el que
mentía era el documento. (2) El duplicado se **BORRÓ** y el guard preexistente se endureció EN SITIO, al
descubrirse que era **fail-open**: decidía con `contains("WindowExclusionReason.<ENTRADA>")`, que no
distingue un SITIO DE CONSTRUCCIÓN de una MENCIÓN — un consumidor que sólo nombra la entrada (`.name`) o
un comentario lo satisfacían. Probado por mutación: sustituir el único productor real por un round-trip
`valueOf(...name)` deja el guard viejo VERDE. (3) El pin pasa a afirmar CONTENIDO (`windowsClassified == n`
y veredicto `Eligible`), arranca en `n = 2` y añade el caso **MIXTO**. La clase de defecto de (1) queda
registrada como hallazgo ABIERTO con dueño (`MB-FOCUS-0079`), no cerrada — y el gate de salida demostró
que mi propia remediación estaba mal dimensionada: yo enumeré «2 citas» y el defecto seguía **vivo en
producción** (`FocusTemporalEligibility.kt` declaraba como contrato una sección llana y conservaba la
variante sin productor `Verdict.Degraded` porque «el ADR la nombra», cierto sólo en HISTORIA). Medido:
**68 citas en 29 ficheros**. Se corrigieron las 5 de las que colgaba una decisión; el resto queda por
clasificar, y la asimetría de `Verdict.Degraded` frente a las entradas que Paso 5 SÍ borró queda como
`MB-FOCUS-0080`.

**Regla futura:**
Antes de citar un ADR/protocolo **vigente** como autoridad, localiza su frontera interna
(`grep -n 'SUPERSEDED'`) y comprueba en qué lado cae tu cita: un documento cuya cola es historia se cita
mal por defecto, porque su cabecera dice VIGENTE. Y **acota la sonda por esa frontera, jamás por el final
del fichero** — un rango abierto a EOF incluye la historia y puede "verificar" lo contrario de lo que
ocurre; si una sonda de ausencia devuelve el mismo número que el total del fichero, tu rango no está
acotando nada. Antes de mecanizar un invariante, **busca el guard que ya existe** (el propio ADR suele
decir en qué paso se mecanizó): dos guards del mismo invariante son dos dueños y una divergencia futura,
y el diff correcto es endurecer el que hay. Al auditar un guard basado en texto, pregunta si su predicado
distingue un **sitio de construcción** de una **mención**: `contains("Tipo.ENTRADA")` cuenta como productor
a un consumidor que sólo la nombra (`.name`, `== ENTRADA`) y a un comentario, así que es fail-open — usa
lookahead negativo y elimina comentarios antes de casar, **declara el techo que el regex NO cubre** (aquí
un `== ENTRADA` pelado, una rama `when` o un `import` siguen contando como productor: mi primera prosa
afirmaba que el lookahead los cubría y era falso en los tres sitios donde lo escribí), y pruébalo por
mutación (sustituye el productor
real por un round-trip que no construya nada: si sigue verde, el guard es decorativo). Un guard
anti-vacuidad debe afirmar **contenido**, no los límites del bucle: contar iteraciones reafirma el rango
que tú mismo escribiste. Y comprueba que cada fila del barrido EJERCITA de verdad lo que compara: una
sesión que colapsa a T0 satisface cualquier igualdad por la razón equivocada. Una equivalencia entre dos
variantes se pinnea también en configuración **MIXTA**: comparar sólo todo-A contra todo-B sobrevive a que
la variante adquiera una consecuencia uniforme en toda la sesión. Corolario de entorno: `Unable to delete
directory .../classes/kotlin/desktop/main - a process has files open`, `Failed to load cache entry` y
`FileNotFoundException` sobre un `.class` del build dir son la MISMA firma de Gradle concurrente sobre un
worktree compartido que `NoClassDefFoundError`/`initializationError` — añádelas al discriminador de
"inconcluso" y re-ejecuta en serie, no cambies una línea. Y un `BUILD SUCCESSFUL in 7s` sin línea de
conteo de tests es la tarea UP-TO-DATE: no es evidencia (2026-07-19). Y cuando registres una remediación puntual, **MIDE su alcance con un grep reproducible antes de escribir
el número**: enumerar «los 2 sitios» de memoria y sellarlo como verificado es sustituir evidencia por
recuerdo — aquí los sitios eran 68 en 29 ficheros y uno de los que faltaban estaba en producción, dentro
del mismo fichero que la fase daba por cerrado. Corolario: un gate de salida solo sirve si le dejas
contradecirte; de sus 6 hallazgos confirmados, 4 desmentían mi propio trabajo, y uno de ellos desmentía
la fila del registro que iba a sellar la disciplina de citas.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/timing/TemporalWindowVerdict.kt` y `.../focus2/domain/evidence/FocusTemporalEligibility.kt` (KDoc: citas a R3, afirmaciones retiradas)
- `shared/src/desktopTest/.../core/timing/TemporalClassPolicyTest.kt` (guard AC#4 endurecido) · BORRADO: `WindowExclusionReasonProducerTest.kt` (duplicado)
- `shared/src/desktopTest/.../focus2/domain/evidence/FocusTemporalEligibilityTest.kt` · `.../focus2/simulation/FocusTemporalEligibilityAdversarialOracleTest.kt` (pin como propiedad + caso mixto + anti-vacuidad de contenido)
- `docs/engines/focus_v5/reaudit/c4/S5_PASO7_C4_DISPOSITION.md` §5 · `MINDBOOST_AUDIT_LEDGER.md` (`MB-FOCUS-0076` CLOSED; `0078`/`0079` nuevos)

### 2026-08-19 — La resistencia a la memorizacion de un banco NO es una propiedad de su tamano; y una correccion estadistica resuelve un punto INDEFINIDO en un valor confiado

**Error detectado:**
Ola 2 · R3 del modulo Emociones. (1) La auditoria pide "banco generativo >=300" y esboza plantillas DISJUNTAS por categoria
(una apertura por categoria). Implementarlo asi habria EMPEORADO el defecto que R3 existe para arreglar: con 28 frases
autoradas hay 28 cosas que memorizar; con 300 frases repartidas en 4 aperturas disjuntas hay **CUATRO** — la primera
palabra decide la respuesta. (2) Al anadir la metrica de 4 vias que faltaba (d-prima uno-contra-resto por categoria), dos
tests se pusieron rojos por una razon legitima: una categoria que el usuario **nunca eligio** puntuaba **+0.46**. En
uno-contra-resto las celdas estan desbalanceadas POR CONSTRUCCION (la senal es un tercio del ruido), asi que la correccion
loglineal empuja mas la celda pequena; y el punto que estaba corrigiendo — aciertos 0 y falsas alarmas 0, el ORIGEN del
ROC — es uno donde d-prima esta **matematicamente indefinida**. Un usuario que jamas pulso `impulso` leia como que
discrimina impulsos.

**Causa raiz:**
(1) Se confundio el TAMANO del banco con la propiedad que de verdad importa: cuantas cosas distintas hay que aprender para
acertar sin leer. Un banco grande de estructura predecible tiene un espacio de estrategias PEQUENO. (2) Una correccion
incondicional no distingue entre "este valor es extremo" y "este valor no existe": en el primer caso desplaza un numero
real, en el segundo **inventa uno**, y lo inventa en la direccion que el desbalance de celdas dicte. Es el aprendizaje
2026-07-20 (correccion incondicional + celdas desbalanceadas) en su forma mas aguda: alli el sesgo torcia un estimador,
aqui rellena un hueco donde no habia estimador ninguno.

**Solucion aplicada:**
(1) El material lexico se **CRUZA**: el mismo verbo y el mismo sujeto aparecen bajo mas de una categoria (`Siento X en Y`
estado vs `Siento que Y` pensamiento; `Quiero A` impulso vs `Acabo de A` hecho, mismo infinitivo). La propiedad se MIDIO
antes y despues con un **techo de estrategia-por-prefijo** (para cada grupo de N palabras iniciales, mayoria / total): 40
-> 347 items y techo de 1 palabra 98.2% -> 81.3%, con el 98.2% descubierto instrumentando, no razonando. Efecto lateral
util: cruzar el material es lo que crea los ejes de confusion que el feedback dirigido necesitaba. (2) El caso degenerado
se decide desde los **CONTEOS CRUDOS** y no desde el estadistico corregido: nunca elegida ⇒ 0.0. NO se clampa en ningun
otro sitio — una etiqueta usada MAL (aciertos 0, falsas alarmas > 0) sigue ganando su d-prima negativa. (3) El test que
afirmaba una LIMITACION (la sonda de holdout era demasiado pequena para comparar) llevaba su propio guard, y ese guard es
lo que se puso rojo al crecer el banco: la asercion se **INVIRTIO** en vez de borrarse, con una mitad discriminante nueva
que sigue rechazando una sonda corta.

**Regla futura:**
Antes de crecer un banco de items "para que sea mas dificil memorizarlo", **MIDE el techo de la estrategia mas barata**
(prefijo de N palabras, posicion de la opcion, longitud) sobre el banco actual y sobre el propuesto: si las plantillas son
disjuntas por categoria, mas items REDUCEN lo memorizable. Cruza el material lexico entre categorias en vez de dar a cada
una su vocabulario. Y no des por buena una propuesta de diseno solo porque venga de la auditoria: un plan puede pedir la
cifra correcta con la estructura equivocada. Toda **correccion estadistica incondicional** debe auditarse por sus puntos
DEGENERADOS, no solo por su sesgo: si existe una combinacion de conteos en la que el estadistico esta indefinido (el
origen del ROC, una division 0/0, una varianza sin muestra), la correccion lo convertira en un numero confiado — decide
ESE caso desde hechos crudos y deja el resto del rango intacto, con una mitad discriminante que pruebe que no clampaste de
mas. Un test que pinnea una LIMITACION debe llevar un guard que lo ponga ROJO el dia que la limitacion se levante, para
que la asercion se pueda invertir con evidencia en vez de borrarse en silencio. Corolario de alcance: si al promover una
metrica nueva descubres una trampa aguas abajo que no es tuya (aqui un 0-100 que hace `coerceIn(0,1)` sobre una entrada no
acotada), **pinnea la trampa con un test y registrala**, no amplies la fase para arreglar el contrato de otro.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions/domain/exercise/{SeparatorItemFactory,EmotionContent,EmotionHoldoutProbe,EmotionSeparatorBank}.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions/domain/metrics/{EmotionCategoryDiscrimination,EmotionMetricKey,DefaultEmotionMetricsEngine}.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions/domain/session/EmotionBlockResult.kt` · `.../domain/feedback/{EmotionFeedbackCopy,EmotionFeedbackBuilder,FourPartFeedback}.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/Probit.kt` (movido desde `focus2/domain/scoring`)
- `MINDBOOST_AUDIT_LEDGER.md` (`MB-EMO-0010`..`MB-EMO-0017`)

### 2026-08-19 — Un sorteo por ensayo fija la PROPORCION y deja el TAMANO DE CELDA al azar: la muestra de una comparacion no se sortea, se REPARTE; y el set que debe la cuota se DERIVA, no se enumera

**Error detectado:**
El hallazgo heredado `MB-CATALOG-0014` decia que la elegibilidad de una afirmacion de capacidad en
`CONGRUENCY_EFFECT` dependia de la SEMILLA. Sobrevivio intacto a la FASE 0 —los 6 simbolos verificados uno a
uno, el primero de este plan que lo consigue— pero su ALCANCE estaba incompleto. Medido antes de tocar nada:
el bloque E2 de baseline (60 ensayos, `incongruentProportion = 0.20`) producia entre **4 y 20** ensayos
incongruentes sobre 40 semillas, con una semilla EXACTAMENTE en el piso central de 20/celda — es decir, 1
usuario de 40 recibiendo una comparacion que el diseno no le concede, y 39 no. Y el defecto no era de E2:
era de los SEIS generadores cuya condicion sorteada es la celda de una metrica con piso central propio
(E2, E4, E5, E6, E8, E13).

**Causa raiz:**
Una division de condiciones tiene DOS propiedades independientes: la **proporcion esperada** (diseno del
ejercicio, gobernada por un eje de dificultad) y el **tamano de celda realizado** (la muestra de la
comparacion). Un Bernoulli por ensayo las ACOPLA: fija la primera correctamente y deja la segunda como
variable aleatoria. Como una puntuacion de DIFERENCIA se juzga por su celda MENOR contra un piso ratificado
(`MinSampleGate`, `requiresAllCells`), el sorteo se cuela directamente en el veredicto de elegibilidad. Es
la forma de "una cola es peor que un techo" del learning anterior, pero un escalon mas abajo: alli el techo
se calculaba, aqui ni siquiera hay un numero que calcular porque cada sesion trae el suyo.

**Solucion aplicada:**
Cuota: `conditionSchedule(size, proportion, random)` reparte exactamente `round(n*p)` y **baraja con el
mismo RNG pinneado** (`DeterministicRng` extiende `Random`, asi que `shuffled(random)` sigue siendo
determinista y cross-platform). La proporcion esperada NO se mueve —criterio de aborto declarado en el ADR
ANTES de tocar codigo y convertido en test: solo puede diferir por redondeo, `|real - declarada| <= 1/slots`—
y la varianza del tamano de celda pasa a CERO, asi que la elegibilidad se vuelve funcion determinista de
`(n, dificultad)` que un test de build puede afirmar. **La tabla central no se toco** (anti-patron cerrado
por R7); las otras dos salidas que el propio hallazgo nombraba (subir el bloque, fijar `interference`) se
declararon decisiones de producto y quedaron registradas, no ejecutadas. E2 sube a `generatorVersion 3` en
sus DOS declaraciones independientes (generador y `FocusFormContract`), que es lo que hace real el
cross-check. Mutacion x3 con restauracion byte-identica por SHA-256, incluida una **por-generador** (solo E8
vuelve a Bernoulli) para probar que la cobertura es de la familia y no solo de E2.

**Regla futura:**
Cuando una condicion que el usuario ve sorteada sea la CELDA de una comparacion, no la sortees por ensayo:
**repartela**. Un Bernoulli honra la proporcion y regala la muestra al azar, y la muestra es justo lo que un
piso ratificado mide. Barajar el reparto no es opcional: una cuota en bloque cambia un defecto de muestreo
por uno peor (condicion predecible y confundida con tiempo-en-tarea). El set de generadores que deben cuota
se **DERIVA** de la autoridad central (`MeasurementSufficiency.central`) y se afirma con un test que falla si
alguien anade una metrica comparativa sin decidir como se extrae su celda — enumerarlo a mano no vale: mi
propia tabla escrita a mano olvido E3, y fue la derivacion la que lo caza en su primera ejecucion (registrar
esa correccion COMO correccion en el ADR, no reescribir el apartado en silencio). Calcular la reachability
antes de arreglar paga dos veces: aqui revelo dos hechos que el hallazgo no contenia — SSRT es un TECHO DURO
en 60 ensayos (30 paradas exigidas frente a ~20 alcanzables) y **la dificultad mueve en silencio la
elegibilidad de una afirmacion de medida** (E8 pasa de 88.3% a 9.3% de sesiones elegibles entre carga minima
y maxima, es decir el motor adaptativo decide si la sesion puede sostener una afirmacion). Y no actualices un
golden leyendo el valor nuevo del propio sujeto: escribe la **referencia independiente**, anclala a vectores
canonicos publicados y haz que se NIEGUE a emitir si algun ancla falla —la mia fallo 2 de 8 y el fallo era
MIO, no suyo, que es exactamente la evidencia discriminante que un golden re-leido del sujeto jamas da—;
solo entonces la afirmacion de independencia del fichero sube de REPORTED a VERIFIED. Corolario operativo:
el scanner de copy lee comentarios y para en el PRIMER hit, asi que un KDoc tuyo con un lexema de la familia
prohibida (aqui `odds`, describiendo nuestro propio defecto) rompe el gate — y conviene evitar tambien los
vecinos que aun no estan en el lexicon; y un `Daemon compilation failed: null` con `[IC] Expected output item`
sobre un `.class` ajeno y CERO errores `e:` de fuente es corrupcion incremental, no una regresion: se
re-ejecuta, no se cambia una linea.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/exercise/TrialGenerator.kt` (`conditionSchedule`) + `{E2Filtro,E4Freno,E5Conflicto,E6Cambio,E8Duo,E13Presion}Generator.kt` + `domain/engine/FocusFormContract.kt`
- `shared/src/desktopTest/.../focus/domain/exercise/ConditionScheduleTest.kt` (nuevo) · `.../focus/domain/evaluation/RciMeasurementSufficiencyTest.kt` · `shared/src/commonTest/.../focus/domain/engine/E2FormGoldenTest.kt`
- `scripts/focus-e2-form-reference.mjs` (nuevo, referencia independiente anclada) · `docs/audits/ADR_CONDITION_SCHEDULE_2026-08-19.md` (nuevo) · `MINDBOOST_AUDIT_LEDGER.md` (`MB-CATALOG-0014` CLOSED; `0015`/`0016`/`0017` nuevos)


### 2026-08-19 — Un veredicto que suma TODOS los buckets absorbe el de "no se pudo medir" y fabrica el maximo; y un oraculo que pinnea su CODOMINIO mientras mata de hambre a su DOMINIO no prueba nada

**Error detectado:**
Gate independiente C4 sobre Focus V5 · S5 · Paso 7. (1) `FocusTemporalEligibility.evaluate` sumaba
`cell.totalTrials` sobre TODOS los buckets, `NO_OBSERVATION` incluido: `windowsClassified` igualaba
`resolvedTrials` ⇒ `coverageComplete = true`; el bucket `EXCLUDED` estaba vacio ⇒ `excludedFraction = 0.0`;
el conjunto de causas salia vacio ⇒ **`Verdict.Eligible`**. Una sesion en la que no se clasifico
ABSOLUTAMENTE NADA salia impecable, y es alcanzable hoy: el `actual` de iOS declara `expectedCadenceHz = null`,
asi que `cadenceResolved` falla en CADA ventana mientras `temporalSampledFrames` sigue acumulando deltas y
lleva la clase de sesion a T1. (2) `FocusEvidenceStatePolicy` decidia con `(temporal?.excludedFraction ?:
NO_BACKSTOP) > max` y `NO_BACKSTOP = 0.0` cubria DOS hechos distintos: «no llego evaluacion» y «llego una y
no pudo derivar fraccion». (3) `DegradedUsable` promete en su KDoc que «constrains shadow RT» y no restringe
nada. Los tres sobrevivieron a una suite VERDE.

**Causa raiz:**
El oraculo adversarial que existe para pinnear este SUT afirmaba su **codominio alcanzable** mientras su
**dominio de entrada** sobre la jerarquia sellada eran dos valores y un nulo: `NoObservation` y
`DegradedUsable` —la mitad de la jerarquia, y la mitad sobre la que vive iOS— no llegaban a `evaluate` desde
NINGUN test del arbol. Es la regla de la mitad discriminante aplicada al reves. Y `coverageComplete` era
ademas una TAUTOLOGIA: el reducer alimenta el tally oficial y la celda sombra en el MISMO fold, asi que las
dos sumas coincidian siempre que el probe devolviera algun veredicto — y `NoObservation` **es** un veredicto;
solo podia ser falso con el probe apagado.

**Solucion aplicada:**
`NO_OBSERVATION` sale del numerador Y del denominador, y la ausencia se **modela** (`windowsUnobserved`) en
vez de restarse: doble fail-closed (cobertura real + el guard `?: FULLY_EXCLUDED` que ya existia). El
discriminador se escribio como **allowlist exhaustivo** sobre el enum, no como denylist `!= NO_OBSERVATION`:
un denylist es fail-OPEN dos veces (un bucket futuro entra al numerador sin que nadie lo decida, y un nombre
escrito por un schema MAS NUEVO llega como `null` porque `fromWire` es fail-closed, y contaria como ventana
limpia). El centinela de (2) se sustituyo por una rama nombrada que declara por que las dos lanes leen el
mismo `null` de forma opuesta. El dominio del oraculo se completo con un `when` **exhaustivo** sobre la
jerarquia sellada, asi que una variante nueva ROMPE LA COMPILACION en vez de quedarse sin alimentar. Mutacion
ejecutada: 6 tests en ROJO por `AssertionError`, los otros 35 en verde (mitad discriminante), restauracion
byte-identica por SHA-256.

**Regla futura:**
Cuando derives una ratio o una cobertura de un conjunto de categorias, comprueba si alguna significa **«no se
pudo medir»**: esa no entra ni en el numerador ni en el denominador, y su cuenta se modela aparte — sumarla al
total hace que la ausencia total fabrique el MAXIMO, que es la direccion cara de `null != 0`. El discriminador
de «esto si es medicion» se escribe como allowlist exhaustiva sobre el enum (y decide explicitamente el caso
`null`, que es lo que produce un schema mas nuevo en una ventana de downgrade N-1); un denylist admite en
silencio todo lo que se anada despues. Una igualdad de cobertura entre dos sumas producidas por el MISMO fold
es una tautologia, no una verificacion: comprueba de que forma puede ser falsa antes de confiar en ella. Un
oraculo que afirma cobertura del **codominio** debe demostrar que alimenta el **dominio** completo, y ese
guard va en BUILD (un `when` exhaustivo que no compila si aparece una variante), no en ejecucion
(`sealedSubclasses`). Refutar un hallazgo exige el mismo estandar de evidencia que confirmarlo: si la
refutacion es del sistema de tipos (una funcion no es `suspend`, luego no hay punto de suspension donde
intercalar), **no anadas un test que no puede fallar** —seria reproducir la vacuidad que estas documentando—;
anade la **precondicion** en la que descansa la refutacion, que normalmente no esta escrita en ninguna parte.
Y no cierres lo que exige una decision ajena: un tier que ABLANDA un veredicto no puede congelarse antes de la
aritmetica que lo justifica; se pinnea como se comporta y se registra con dueno. Corolario operativo NUEVO: el
propio ledger puede tener DOS sesiones escribiendo a la vez (aqui, la del catalogo R8 anadio
`MB-CATALOG-0013`/`0014` a mitad de esta fase), asi que una escritura al ledger necesita **compare-and-swap
sobre el sello** (leer el hash vivo, abortar si cambio, reconciliar y reintentar) ademas de comprobar que no
colisionan los IDs; y una cita por NUMERO DE LINEA en un documento de evidencia deja de resolver en cuanto
alguien edita el fichero — cita por contenido con el `grep` que la localiza.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus2/domain/evidence/{FocusTemporalEligibility,FocusEvidenceStatePolicy}.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus2/presentation/FocusTracerStateHolder.kt` (precondicion de confinamiento declarada)
- `shared/src/commonTest/.../focus2/simulation/FocusTemporalEligibilityProperty.kt` · `shared/src/desktopTest/.../focus2/domain/evidence/{FocusTemporalEligibilityTest,FocusEvidenceStatePolicyTest}.kt` · `.../focus2/simulation/FocusTemporalEligibilityAdversarialOracleTest.kt` (Fase H)
- `docs/engines/focus_v5/reaudit/c4/S5_PASO7_C4_DISPOSITION.md` (nuevo) · `MINDBOOST_AUDIT_LEDGER.md` (`MB-FOCUS-0073`..`0077`, `MB-FOCUS-0071` P3→P1)

### 2026-08-19 — Un booleano que responde DOS preguntas con pisos legitimos distintos no se arregla subiendolo: se desdobla; y el techo de un gate se CALCULA, porque una cola es peor que un techo

**Error detectado:**
El hallazgo abierto `MB-CATALOG-0011` decia: «`DefaultMetricsEngine` no consume `MinSampleGate`: mantiene pisos locales de
3..5 mientras la autoridad central exige >= 20 para la misma familia de metricas — dos fuentes de verdad». Verificado
contra el repo, el enunciado era falso en tres puntos (las constantes locales son CINCO, `MinSampleGate` SI tiene >= 8
consumidores de produccion, y el motor era el UNICO que no lo consumia) y su prescripcion —subir los pisos— habria sido
un defecto nuevo. El defecto real vivia en otra parte: `MetricValue.confident` responde a la vez a la pregunta del
**lazo de control en sesion** (¿puede el motor adaptativo mover la dificultad con esto? piso 3..6, correcto) y a la de
**afirmacion de medida** (¿puede una afirmacion sobre el USUARIO descansar aqui? >= 20/celda). El consumidor consecuente
no era el perfil —los 9 `ProfileSource` siguen fail-closed— sino el **RCI**, la unica afirmacion de capacidad del
producto (invariante #7 del charter): gateaba por `confident`, asi que un efecto de congruencia medido sobre **3
ensayos por celda** podia sostener un «cambio fiable». Estaba ARMADO detras de una segunda puerta fail-closed
(`ReliabilitySource.EMPTY`), es decir, se habria activado exactamente el dia que el estudio de fiabilidad poblara esa
costura «sin tocar call-sites».

**Causa raiz:**
Un unico booleano para dos preguntas cuyos umbrales correctos difieren en un orden de magnitud. Subirlo a 20 habria
silenciado el lazo de control (la dificultad dejaria de moverse); dejarlo tal cual mantenia la afirmacion de capacidad
apoyada en el piso equivocado. Las dos salidas «obvias» eran erroneas porque la pregunta estaba mal planteada: no habia
un umbral que reconciliar, habia **dos hechos que separar**. Es la familia de R7 (`MB-CATALOG-0009`) vista del otro
lado: alli un valor cargaba dos significados, aqui un booleano carga dos preguntas.

**Solucion aplicada:**
`confident` NO se toca (cero deriva de dificultad, cero cambio de UI, **cero golden movido**: el cambio es
estrictamente aditivo). Se anade `MetricValue.measurementSufficient: Boolean?`, derivado de `MinSampleGate` con los
conteos **POR CELDA** en el punto donde esos conteos existen, y solo el RCI cambia de gate. Tri-estado deliberado:
`null` = «esta metrica no tiene piso ratificado, la pregunta no aplica», jamas reutilizado como «no». El gate se
escribe `== true` y no `!= false`: con `!= false` toda metrica no gobernada pasaria por defecto. Las celdas de una
puntuacion de DIFERENCIA se juzgan por la **menor**, nunca por la suma. Verificado por mutacion x3 (RCI de vuelta a
`confident`; `cellsOf` con la suma; sin la pasada de estampado) con restauracion byte-identica por SHA-256.

**Regla futura:**
Cuando dos consumidores lean el MISMO flag con necesidades de muestra distintas, no reconcilies el umbral: **desdobla el
hecho**, deja el flag viejo con su semantica y anade uno nuevo con nombre propio; asi el cambio es aditivo y ningun
golden se mueve, que es lo que desbloquea un hallazgo aparcado por «rompe los goldens». Antes de implementar un hallazgo
heredado, **verifica su enunciado Y su prescripcion**: aqui las dos eran falsas, y seguirlo al pie habria roto el lazo de
control. Todo campo tri-estado se consulta con la comparacion EXPLICITA (`== true`); `!= false` convierte «no aplica»
en un pase. Una puntuacion de diferencia se juzga por su celda MENOR — 40 ensayos en una condicion y 2 en la otra son 42
de muestra y cero comparacion — y eso hay que probarlo con un caso donde suma y minimo den veredictos OPUESTOS, porque
un test de la funcion pura no ve un motor mal cableado. **El techo alcanzable se CALCULA sobre el generador real, no se
razona**: mi primer test afirmaba `max(incongruentes) < 20` sobre 200 semillas y fallo — el sorteo es Bernoulli por
ensayo, no una cuota, asi que no hay techo sino **cola**, y una cola es PEOR que un techo porque deja la elegibilidad de
la afirmacion a merced de la semilla; pinealo por distribucion (mediana + proporcion que cruza + guard anti-vacuidad de
que la celda existe). Un diseño puede resultar falso AL IMPLEMENTARLO: aqui `baselineSnapshot` no podia endurecerse
(su `perMetric` alimenta tambien `ArchetypeInference`, del lazo de control, y las muestras del lado pre no se
persisten) — registra la correccion COMO correccion en el propio ADR en vez de reescribir el diseño, y declara el hueco
en el KDoc del gate a medias. Un fixture que anade el campo nuevo NO es «editar el test para que pase» si (a) declara lo
que el motor estampa de verdad —mejor aun, lo DERIVA de la misma regla— y (b) la mutacion sigue poniendolo en ROJO.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/metrics/MeasurementSufficiency.kt` (nuevo) · `MetricKey.kt` · `DefaultMetricsEngine.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/evaluation/EvaluationResult.kt`
- `shared/src/desktopTest/.../focus/domain/metrics/MeasurementSufficiencyTest.kt` · `.../focus/domain/evaluation/RciMeasurementSufficiencyTest.kt` (nuevos)
- `shared/src/commonTest/.../focus/FocusTestFixtures.kt` · `.../focus/domain/evaluation/ReliableChangeIndexTest.kt`
- `docs/audits/ADR_MIN_SAMPLE_MEASUREMENT_2026-08-18.md` (nuevo) · `MINDBOOST_AUDIT_LEDGER.md` (`MB-CATALOG-0011` CLOSED; `MB-CATALOG-0013`/`0014` nuevos)

### 2026-08-18 — Un centinela que significa a la vez "medido" y "no aplica" hunde el techo de un gate por debajo de su propio umbral; y el ORDEN en que se sirve un item es informacion que nadie declara

**Error detectado:**
Dos gates decorativos de la MISMA familia, cerrados en la fase R7 del plan blindado del catalogo. (1) El AGRS de
Decisiones no podia alcanzar su umbral de invalidacion (`0.75`): con pesos `0.4*rt + 0.3*entropia + 0.3*dial` y los
DOS ejercicios pasando `spatialEntropy = 1.0`, el peor caso posible puntuaba **0.7**. Ninguna conducta invalidaba una
sesion, y el eje "Integridad" que ve el usuario (`(1 - gamingScore) * 100`) no podia bajar de 30. (2) En Lenguaje, la
respuesta correcta estaba en la ranura `"b"` en **32 de 36** items cerrados (cuatro bancos al 100%, EvidenceLock 5/6;
solo SourceTriage equilibrado): "pulsar siempre la segunda" acertaba el 89% sin leer nada. Y al instrumentarlo aparecio
algo peor: los items de ORDENACION se entregaban a la pantalla en su **orden clave exacto** (`options = ordered` y acto
seguido `key = Ordering(ordered.map { it.id })`), asi que "de arriba abajo" era un 100% perfecto.

**Causa raiz:**
(1) El defecto NO era el reparto de pesos: era un **centinela ambiguo**. `1.0` significaba a la vez "medido y maximamente
diverso" y "aqui no hay rejilla espacial" — y un deslizador no tiene rejilla, asi que el valor era honesto como
afirmacion y devastador como entrada: un termino constante no es "neutro", consume su peso y baja el techo de todos los
demas. (2) "El orden no es contenido" era cierto para el CODIGO (la clave nombra la opcion por id, la pantalla renderiza
solo el texto, el mapa de distractores va por id) y falso para el USUARIO, que es el unico que ve la posicion. Nadie
instrumenta lo que ninguna capa consume, asi que el sesgo se acumulo banco a banco sin que ningun test lo mirara.

**Solucion aplicada:**
(1) La no-aplicabilidad se MODELA (`spatialEntropy: Double?`, `isDialDefault: Boolean?`) y `calculateAGRS` **renormaliza**
sobre los pesos aplicables. La prueba de que el cambio es seguro es que una tarea totalmente instrumentada no cambia ni
un decimal (los pesos suman 1.0: los tests de Alpha Core y del orquestador siguieron verdes sin tocarlos), y el peor caso
de los dos ejercicios de Decisiones pasa a **1.0**. NO se re-peso la ecuacion: re-pesar es una decision psicometrica,
re-modelar una entrada mal tipada no lo es. (2) Barajado determinista al SERVIR (`ItemPresentation` + `DeterministicRng`
/`SessionSeed` ya existentes, semilla inyectada por `SeedSource` **sin default constante**), no rebalanceo de contenido.
Ambas con mutacion ejecutada (reintroducir el centinela / quitar el barajado del holder ⇒ ROJO) y restauracion
byte-identica por SHA.

**Regla futura:**
Cuando el techo alcanzable de un gate no llegue a su umbral, antes de tocar los pesos pregunta si alguna **entrada es un
centinela que significa "no aplica"**: si lo es, el arreglo es de modelado (`null` + renormalizacion), no de pesos, y se
valida exigiendo que las tareas totalmente instrumentadas den EXACTAMENTE el mismo numero que antes. Todo parametro de un
control de calidad que pueda ser no-medible en alguna tarea se declara nullable y **obligatorio** (sin default: un default
restaura el hardcode en silencio). Para cualquier lista que se renderiza al usuario cuya clave nombre un elemento por id,
**el orden es informacion no instrumentada**: mide su distribucion antes de darla por buena, y prefiere
**aleatorizar al servir** a rebalancear el contenido — rebalancear arregla los items que existen y no garantiza nada del
siguiente que se escriba. Verifica primero que nada consume el orden posicionalmente (grep de `options[0]`/`.first()`/
`indexOf`), y no barajes lo que SI es contenido ordenado (los tramos de evidencia son citas del pasaje). Un test que mide
una distribucion necesita **guard de forma** (afirmar que todos los items tienen 3 opciones, o el chi2 tiene los grados de
libertad equivocados) y **mitad discriminante** que siga fallando sobre el contenido autorado. Corolario operativo: si otra
sesion trabaja el MISMO worktree, sus fallos parecen tuyos — un `.kt` untracked ajeno con un import mal rompe la
compilacion del modulo entero, y su Gradle concurrente produjo aqui tres firmas distintas de falso fallo (53/53
`NoClassDefFoundError`, fallo de empaquetado de cache sobre un `.class` desaparecido, y el jar del agente kover bloqueado
por otra JVM; ademas un directorio de clases de test VACIO que Gradle reportaba `UP-TO-DATE`). Diagnostica, espera a que el
arbol quede libre y re-ejecuta en serie: no cambies una linea por esos fallos, y atribuye cada finding de `detekt` a su
dueno antes de "arreglarlo".

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindverse/mindboost/core/scoring/ScoringEquations.kt` · `core/session/DecisionSessionOrchestrator.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/presentation/{BayesArenaStateHolder,ReversibilityTriageStateHolder}.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/language/presentation/{ItemPresentation,LanguageSessionStateHolder}.kt`
- `shared/src/commonTest/kotlin/com/mindverse/mindboost/core/tests/AgrsApplicabilityTest.kt` · `shared/src/desktopTest/.../decisiones/presentation/{DecisionesGamingReachabilityTest,BayesArenaStateHolderTest}.kt` · `.../language/presentation/LanguageOptionOrderTest.kt`
- `MINDBOOST_AUDIT_LEDGER.md` (`MB-CATALOG-0009`/`0010` CLOSED, `MB-CATALOG-0012` nuevo)

### 2026-08-18 — Una probabilidad subjetiva se ancla al AZAR DE LA TAREA, no a una escala fija; y persistir conteos en vez de ensayos solo vale si son estadísticos SUFICIENTES, cosa que se prueba con un test de ida y vuelta

**Error detectado:**
Al construir la capa transversal de calibración de Emociones (Ola 1 · N1) el repo ya tenía un Brier:
`ScoringEquations.calculateBrierScore`/`calculateCalibrationError` (core legacy `com.mindverse.mindboost`).
Reutilizarlo habría metido tres defectos a la vez: (1) **clampa la probabilidad declarada a `[0.5, 1.0]`**, que
solo es correcto para dos alternativas y es FALSO para la clasificación de cuatro vías de Emociones — un usuario
que dice "poco seguro" en una tarea con 25 % de azar habría entrado como si hubiera declarado 50 %, inflando
sistemáticamente la sobreconfianza medida; (2) devuelve **`0.0` sobre entrada vacía** (un Brier perfecto
fabricado desde la ausencia, con `AlphaCoreTest:187` pinneando ese 0.0 como esperado); y (3) **no tiene ningún
call-site de producción**, así que llevaba tiempo verde sin medir nada. En paralelo, el charter prohíbe persistir
trials crudos, y un juicio de confianza por ensayo ES dato de ensayo: guardarlo habría violado la regla, y
guardar solo un Brier ya calculado habría impedido re-leerlo con otro umbral.

**Causa raíz:**
Una "probabilidad de acertar" no vive en `[0, 1]` neutro: su suelo lo fija el AZAR de la tarea anfitriona. Sin
ese ancla, la misma pulsación significa cosas distintas en un 2AFC y en una clasificación de cuatro vías, y
cualquier sobreconfianza medida mezcla el sesgo del usuario con el sesgo de la escala. Y la tensión
"resumen-only vs necesito re-calcular" no se resuelve eligiendo un bando: se resuelve comprobando si el resumen
es **suficiente** — si dentro de cada celda todos los datos comparten el mismo valor del parámetro, los conteos
recuperan el estadístico EXACTAMENTE y no hay nada que aproximar.

**Solución aplicada:**
Kernel nuevo en `core/calibration` (no en el feature: la auditoría lo exige y `core` no puede depender de
Emociones). La probabilidad declarada se deriva de los **puntos medios de bins de igual anchura sobre
`[chance, 1.0]`** — con azar 0.25 salen 0.375 / 0.625 / 0.875, sin ningún 0.0 ni 1.0 degenerado y **sin una sola
constante ajustada a mano**. `EmotionCalibrationHost` decide qué paradigma puede llevar juicio y con qué azar
(go/no-go excluido: la respuesta correcta a un no-go es RETENER, no hay elección que valorar). El veredicto solo
afirma dirección si el **IC del 95 % excluye el cero**, y su valor neutro se llama `NO_DETECTABLE_BIAS`, nunca
"bien calibrado". Persistencia = `(nivel, azar) → (n, aciertos)`: es un RESUMEN y a la vez suficiente, y eso se
**demuestra** con un test que compara la lectura viva contra la releída campo a campo (Brier, sobreconfianza,
IC, resolución, buckets). El piso lo LEE de `MinSampleGate` en runtime (`BRIER = 40`) en vez de re-declararlo,
así que no puede divergir; la auditoría decía `n ≥ 30` y ganó el gate por canónico y más estricto.

**Regla futura:**
Toda métrica que compare una **confianza declarada** con un acierto debe anclar la escala al **azar de la tarea
anfitriona** (`chanceLevel`), que es un parámetro obligatorio y no un default: sin él, la sobreconfianza mide la
escala en vez de al usuario. Nunca reutilices un Brier/calibración existente sin comprobar su clamp, su valor
sobre entrada vacía y si tiene call-sites — un cálculo estadístico sin consumidor lleva años sin que nadie note
sus supuestos. Antes de guardar conteos en lugar de datos crudos, demuestra que son **estadísticos suficientes**
(dentro de la celda el parámetro es constante) y píneálo con un test de **ida y vuelta** que compare vivo vs
releído; si algún estadístico futuro necesita el ORDEN, esa forma deja de ser suficiente y hay que volver aquí,
no apañarlo. El veredicto neutro de un IC se llama "no detectamos sesgo", nunca "estás bien calibrado": lo
primero habla de la evidencia, lo segundo es un piropo que la evidencia no sostiene. Y una capa transversal que
un paradigma no puede servir (go/no-go) se **declara excluida con test**, no se fuerza: medir un acto
metacognitivo que no ocurrió es fabricar señal.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/calibration/{CalibrationKernel,BlockPrediction}.kt` (nuevos)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions/domain/calibration/EmotionCalibrationHost.kt` · `emotions/data/local/EmotionCalibrationSummaries.kt` · `emotions/presentation/{EmotionCalibrationCapture,EmotionCalibrationCardBuilder}.kt` (nuevos)
- `shared/src/commonTest/.../core/calibration/**` · `.../emotions/data/local/EmotionCalibrationPersistenceTest.kt` · `.../emotions/presentation/{EmotionCalibrationCaptureTest,EmotionCalibrationCardBuilderTest}.kt`
- `MINDBOOST_AUDIT_LEDGER.md` (`MB-EMO-0006`..`MB-EMO-0009`)

### 2026-08-18 — Un gate cuyo vocabulario (o cuya entrada) es una SEGUNDA copia sin pinnear es decorativo; y un umbral que ninguna combinación de conducta puede alcanzar no es un gate, es un adorno

**Error detectado:**
Tres defectos de la MISMA familia —un control que existe, corre en verde y no mira nada— hallados al
ejecutar el plan blindado del catálogo cognitivo. (1) El `mindboostCopySafetyScanner` estaba VERDE sobre la
frase más afirmativa que la app envía a producción: `ClaimEngine.ROBUST_GENERALIZATION.safeCopy` prometía
una generalización que ningún estado de evidencia del motor sostiene (E5 solo dice que la habilidad
entrenada se mantiene en material nuevo y tras una pausa), y el scanner no la veía porque **el lexema no
estaba en el lexicon**; esa copy se renderiza como TITULAR (`ProgressUiBuilder.kt:37`). (2) El lexicon vive
DOS veces —`ForbiddenLexicon` (tests) y una copia a mano dentro de la task de Gradle, que debe vivir fuera
del árbol escaneado para no auto-dispararse— y su sincronización era **un comentario**. (3) En Arena Bayes
el término `dial` del AGRS tenía su hueco en la ecuación (peso 0.3) y **nada alimentándolo**:
`isDialDefault = false` hardcodeado, así que enviar la estimación sin tocar jamás el slider —el patrón que
ese término existe para ver— puntuaba igual que un usuario deliberado. Y al cablearlo apareció el defecto
peor: **el AGRS de Decisiones no puede alcanzar su propio umbral de invalidación**.

**Causa raíz:**
Un gate tiene DOS partes —el mecanismo y aquello contra lo que compara— y solo el mecanismo se prueba sola.
Un scanner con un lexicon incompleto, una tabla de umbrales duplicada a mano, o un detector cuya entrada es
un literal: en los tres casos el mecanismo funciona perfectamente y el gate no cubre nada, y como está
VERDE, el verde se lee como cobertura. El caso del umbral inalcanzable es la forma extrema: con pesos
`0.4*rt + 0.3*entropía + 0.3*dial`, invalidación por encima de `0.75`, y los DOS ejercicios de Decisiones
pasando `spatialEntropy = 1.0` —que es la verdad: no hay rejilla espacial en un slider ni en una tarjeta—,
el término de entropía queda fijado en 0 y el peor caso posible puntúa **0.7**. Ninguna conducta invalida
una sesión. El error de diseño no es el 1.0 (es honesto), es haber conservado el peso de un término que en
ese ejercicio no puede aportar.

**Solución aplicada:**
(1) Copy recortada a lo que la evidencia licencia + el lexema añadido como **FRASE** (`transferencia
amplia`/`lejana`, `generalizacion amplia`), nunca la raíz suelta —marcaría `TransferLabel`,
`NearTransferVerifier` y los disclaimers honestos—, con mitad discriminante (el vocabulario de
transferencia CERCANA sigue verde), un test que itera `ClaimType.entries` y **mutación ejecutada**
(reintroducir la frase ⇒ scanner ROJO). (2) Bloque con centinelas en `shared/build.gradle.kts` +
`CopySafetyLexiconParityTest` (patrón de `TemporalQualityThresholdsParityTest`): comparación de conjuntos,
guard anti-vacuidad (≥50 términos parseados), mitad discriminante que borra un término y exige que la
paridad ROMPA, y test de duplicados. (3) El hecho de interacción se CAPTURA en la pantalla y viaja como
parámetro **obligatorio** `dialMoved` (sin default: un default restauraría el hardcode en silencio). (4) El
techo inalcanzable NO se "arregló" tocando pesos: re-pesar una ecuación de anti-gaming es decisión
psicométrica. Se pinneó con un test que afirma el 0.7 exacto y ROMPE si cambian pesos, umbral o la entrada
de entropía, y quedó como hallazgo abierto con dueño.

**Regla futura:**
Al auditar un gate, audita **contra qué compara**, no solo si corre: un scanner es tan bueno como su
lexicon, un clasificador como su tabla de umbrales, un detector como su entrada. Si esa referencia existe
DOS veces (una consumida por tests y otra por el build), píneala con centinelas + test de paridad con mitad
discriminante — "keep in sync" en un comentario no falla ningún build. Toda entrada de un control de
calidad se **captura**, jamás se hardcodea, y su parámetro es **obligatorio**: un default es el hardcode
esperando volver. Antes de dar por bueno un gate con umbral, calcula su **techo alcanzable** con las
entradas que los call-sites REALES pasan: si ninguna combinación de conducta lo cruza, el gate es decorativo
—y la salida honesta es registrarlo y pinnear el techo, no re-pesar la ecuación de paso—. Un término cuyo
valor es constante en un ejercicio no es "neutro": consume su peso y baja el techo de todos los demás. Y
verifica el gate por **mutación** (reintroduce el defecto y exige ROJO); ejecutarlo en verde no prueba nada,
que es exactamente cómo estos tres llevaban meses.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/language/domain/claim/ClaimEngine.kt` · `.../core/copy/ForbiddenLexicon.kt` · `shared/build.gradle.kts` (centinelas de paridad)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/presentation/{BayesArenaStateHolder,ui/BayesArenaScreen}.kt`
- `shared/src/desktopTest/.../core/copy/{MindBoostCopySafetyScannerTest,CopySafetyLexiconParityTest}.kt` · `.../decisiones/presentation/BayesArenaStateHolderTest.kt`
- `MINDBOOST_AUDIT_LEDGER.md` (`MB-CATALOG-0005`..`0011`)

> **Nota 2026-08-18 (fase R7):** el techo inalcanzable que esta entrada dejo *abierto con dueno* (`MB-CATALOG-0009`)
> ya esta CERRADO, y se cerro respetando su propia regla: no se re-peso la ecuacion. La causa resulto ser un centinela
> ambiguo en la ENTRADA (`spatialEntropy = 1.0` = "medido y diverso" y tambien "aqui no hay rejilla"), asi que el arreglo
> fue de modelado — `null` + renormalizacion de los pesos aplicables — y una tarea totalmente instrumentada sigue dando
> el mismo numero. Ver la entrada del mismo dia sobre centinelas ambiguos.

### 2026-08-18 — Un default que tapa una ausencia de la plataforma la convierte en un hecho, y el guard de aguas abajo ya no puede verlo; el código cuyo único consumidor era una fase que nunca aterrizó sobrevive tres fases

**Error detectado:**
Gate de salida de Focus V5 · S5 (Paso 7), tres defectos de la MISMA familia hallados por tres lentes distintas.
(1) El adapter **Android fabricaba la cadencia esperada**: `view.display?.refreshRate ?: DEFAULT_REFRESH_HZ`.
En un panel de 60 Hz la expectativa FABRICADA coincide con la detectada, la ventana **resuelve** y el veredicto
de integridad temporal (CLEAN o EXCLUDED) sale de una constante que nadie midió — mientras iOS, desde el
learning anterior, declara `expectedCadenceHz = null` honestamente. (2) `windowVerdicts`: una **serie ordenada
viva en memoria con un escritor y CERO lectores** durante tres fases, cuyo comentario prometía «los detectores
de Paso 4»; Paso 4 y Paso 5 cerraron sin construirlos. Peor: se añadía ANTES del check de rechazo del runtime,
así que un tap fuera de fase dejaba una entrada fantasma sin ventana resuelta detrás. (3)
`differentialExclusionDelta`: umbral **versionado** sin un solo consumidor desde la R3 — y la fase anterior lo
había **pinneado en un bloque de paridad** describiéndolo como «el detector de exclusión diferencial», que no
existe. Y un cuarto contra la propia fase: el guard escrito para (1) resultó **vacuo bajo mutación**.

**Causa raíz:**
Un valor por defecto en la FRONTERA con la plataforma no rellena un hueco: **cambia el tipo del hecho**. Aguas
abajo la política ya tenía su guard correcto («sin cadencia declarada no hay definición de frame perdido ⇒ no
clasificable»), pero ese guard solo puede actuar si la ausencia LLEGA como ausencia; cuando el adapter la viste
de afirmación, el guard ve un dato válido y clasifica. Y el código que solo iba a tener sentido cuando llegara
una fase futura no muere solo: sobrevive a la fase que no llegó, se documenta a sí mismo como vivo, y cada fase
posterior lo lee como capacidad existente — hasta que alguien lo pinnea en un documento y el hueco de diseño se
convierte en una cifra con aspecto de umbral vigente.

**Solución aplicada:**
(1) El provider pasa a `() -> Double?` **sin constante de reserva**; sin cadencia declarada NO se computa ningún
conteo de drops (reportar cero drops contra una referencia ausente ES la fabricación), y `sampledFrames`
conserva la unidad (deltas) para que el piso de sesión acumule lo mismo sea o no clasificable. (2) La serie se
BORRA: el veredicto ya llega al reducer, que es la autoridad única; una serie ordenada vuelve CON el detector
que la necesite. (3) La constante se RETIRA de la config, del bloque de paridad y del test, y el ADR que la
diseña queda anotado como DISEÑO-no-comportamiento. (4) El guard vacuo se reescribió y se re-verificó por
mutación (ROJO con el defecto presente, restauración byte-idéntica por SHA ⇒ VERDE).

**Regla futura:**
En un `actual` de plataforma, **nunca sustituyas por un default un hecho que la plataforma no pudo dar**:
devuelve `null` y deja que la política decida que no es clasificable. Un default ahí es invisible para todos
los guards de aguas abajo, porque llega como dato válido. Corolario de simetría: si un `actual` ya declara la
ausencia honestamente (iOS) y otro la rellena (Android), **las plataformas no son comparables** aunque ambas
compilen — audita los actuals EN PAREJA, no de uno en uno. Todo lo que se conserve «para la fase siguiente»
—una lista, una constante versionada, una variante de `sealed`— se BORRA si esa fase cierra sin construirlo, y
vuelve con su consumidor y su test; para detectarlo basta un grep de lectores (una colección privada que solo
se escribe no la marca Detekt, porque «se usa»). Nunca pinnees en un documento un umbral sin verificar que
tiene consumidor: pinnearlo lo asciende a norma vigente. Y un guard basado en texto que acota su búsqueda con
`substringBefore("}")` **corta en la primera llave de cierre, que suele ser la de un lambda interior**: acota
por fichero o por bloque delimitado, y da el test por bueno SOLO tras verlo ROJO con el defecto reintroducido.

**Archivos afectados:**
- `shared/src/androidMain/kotlin/com/mindos/app/features/mindboost/core/timing/WindowedTemporalProbe.android.kt`
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/timing/TemporalQualityThresholds.kt` · `focus2/presentation/FocusTracerStateHolder.kt` · `focus2/domain/evidence/FocusTemporalEligibility.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/timing/WindowedTemporalProbeCadenceHonestyTest.kt` (nuevo)
- `docs/engines/focus_v5/S5_PASO7_CLOSURE.md` (nuevo) · `docs/engines/focus_v5/{ADR_FOCUS_TEMPORAL_QUALITY,S5_REOPEN_STATUS,S5_PASO6_PHYSICAL_PROTOCOL}.md`

### 2026-08-18 — Un instrumento que ve UN extremo de la latencia mide la mitad; y un protocolo caducado es una instrucción viva, no papel muerto

**Error detectado:**
Dos defectos en el mismo documento (Focus V5 · S5 · Paso 6, protocolo de validación física). (1) El
protocolo nombraba **fotodiodo y cámara de alta velocidad como instrumentos intercambiables**. No lo son: un
fotodiodo mide el flanco de la PANTALLA y **no ve el evento de entrada**, así que entrega su instante en el
reloj del osciloscopio mientras la app entrega el `state-set` en el reloj del dispositivo — y la diferencia
entre dos relojes no sincronizados **no es una latencia**. Ejecutarlo así habría producido un número con
aspecto de `D` que no es `D`, justo el número que `D-R3-03` exige como `validatedEndToEndErrorBound` para
desbloquear elegibilidad RT. (2) El protocolo fuente databa de cuatro semanas antes (pre-R2/R3/Paso 5) y **8
de sus cláusulas estaban superseded**; la peor, una columna «Accuracy: OFICIAL» que es **falsa bajo
`D-R3-02`** y está redactada como autorización vigente. Nadie lo había marcado: seguía siendo el documento
que un ejecutor abriría.

**Causa raíz:**
Una latencia es la diferencia entre dos eventos, así que el instrumento debe **observar AMBOS extremos en el
MISMO reloj**. Un sensor que solo ve un extremo no mide una duración por muy preciso que sea; su precisión
es irrelevante para el error que comete. Y un documento de protocolo tiene una propiedad que un doc de
diseño no tiene: **está escrito para ejecutarse**, así que una afirmación retirada que sobrevive dentro de él
no es documentación obsoleta — es una instrucción, y al re-ejecutarse vuelve con el aval de una medición
física.

**Solución aplicada:**
(1) Dos niveles con reloj común EXPLÍCITO: Nivel A = cámara de alta velocidad que filma dedo y pantalla en el
MISMO encuadre (la cámara **es** el reloj común; cuantización declarada 4.17 ms a 240 fps, 1.04 ms a 960, y
veredicto `INCONCLUSIVE` si la dispersión medida se acerca a esa cuantización), Nivel B = fotodiodo **más
actuador** (única vía hacia T2). Reencuadre del objetivo: lo que hay que acotar es la **DISPERSIÓN** de `D`,
no su media — un sesgo constante se cancela en la comparación intra-sujeto, la dispersión entra íntegra en
la varianza del RT. (2) Tabla de reconciliación R1–R8 que declara cada cláusula superseded con su autoridad
sustituta, ANTES de cualquier procedimiento. (3) Los umbrales GO/NO-GO del documento se comparan contra
`TemporalQualityThresholds` por un test de `desktopTest` sobre un bloque delimitado por centinelas,
**verificado por mutación** (0.05 a 0.07 en el doc ⇒ ROJO; restaurado byte-idéntico por SHA ⇒ VERDE).

**Regla futura:**
Antes de aceptar un instrumento para medir una latencia, comprobar que **ve los dos extremos y que sus
instantes viven en el mismo reloj**; si solo ve uno, no mide la magnitud aunque su resolución sea excelente.
Declarar siempre la cuantización del instrumento: si la dispersión medida se acerca a ella, el resultado es
`INCONCLUSIVE`, no un número. Para una magnitud que alimentará una comparación intra-sujeto, el objetivo es
la **dispersión**, no la media. Antes de ejecutar un protocolo escrito hace más de un ciclo de revisión,
**reconciliarlo cláusula a cláusula contra las autoridades vigentes y declarar lo superseded EN EL PROPIO
documento**. Todo umbral numérico que un documento repita de una config de producción debe estar **pinneado
mecánicamente** (bloque con centinelas + test que compara contra la config real, probado por mutación), o
doc y clasificador divergen en silencio. Corolario del eje de rendimiento: **no se mide el probe con el
probe** — la métrica del efecto observador la produce `FrameTimingMetric`/Perfetto, jamás `FrameStats`
(misma auto-referencia del learning anterior, otro eje); y un A/B cuyo confound es la deriva térmica va
**INTERCALADO** (ABAB), o el segundo brazo carga con el calor del primero.

**Archivos afectados:**
- `docs/engines/focus_v5/S5_PASO6_PHYSICAL_PROTOCOL.md` (nuevo) · `docs/engines/focus_v5/{ADR_FOCUS_TEMPORAL_QUALITY,S5_REOPEN_STATUS}.md`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/timing/TemporalQualityThresholdsParityTest.kt` (nuevo)

### 2026-08-18 — Un instrumento que se auto-referencia fabrica su propia perfección: la cadencia ESPERADA y la DETECTADA son dos hechos, y un parámetro no-finito debe fallar cerrado

**Error detectado:**
Dos defectos independientes en la misma capa de medición temporal (Focus V5 · S5 · Paso 5). (1)
`FrameStatsCalculator.fromFrameTimes(frames, refreshHz)` con `refreshHz = NaN` devolvía **cero drops y sin
jitter crítico**: con NaN como periodo nominal, TODA comparación `delta > umbral` es falsa, así que una
cadencia de referencia AUSENTE producía la medición impecable. Cero, negativo e infinito daban lo mismo. El
calculador es COMPARTIDO con la lane de Velocidad, así que el defecto no era exclusivo de Focus. (2) El
adapter iOS (`CADisplayLink`) **auto-detectaba su propia cadencia por la mediana de deltas y clasificaba
contra ella**: una ventana pacing a la MITAD del ritmo salía `CLEAN` — el falso negativo que el charter
prohíbe, una sesión genuinamente degradada declarada impecable.

**Causa raíz:**
Un estadístico de calidad necesita un REFERENTE externo, y ambos defectos son formas de quedarse sin él. En
(1) el referente llegaba corrupto y el código lo trataba como válido porque NaN no dispara ninguna
comparación (`NaN > x` es siempre `false`, así que un guard escrito como umbral nunca salta). En (2) el
referente era el propio dato: clasificar una traza contra su propia mediana hace que cualquier ritmo estable
—incluido uno degradado— sea consistente consigo mismo. La intención de (2) era correcta (clasificar contra
el máximo del panel ProMotion produce el falso positivo opuesto: el pacing normal a 60 Hz en un panel de 120
leería como «todo son drops» y excluiría sesiones honestas), pero la salida honesta ante dos errores
disponibles no es elegir el conveniente: es declarar que no hay expectativa defendible. `CADisplayLink`
observa la cadencia del SISTEMA, no el frame que el bucle Skia/Metal de la app compuso.

**Solución aplicada:**
(1) Guard fail-closed: `if (frames.size < MIN || !refreshHz.isFinite() || refreshHz <= 0.0) return
FrameStats.UNAVAILABLE`, con test de mitad discriminante (la MISMA traza a 60 Hz sigue clasificando como jank
crítico — sin ella, «clampar todo a UNAVAILABLE» pasaría el test). (2) La cadencia se modela como DOS hechos
separados en la observación: `expectedCadenceHz` (lo que la plataforma DECLARA) y `detectedCadenceHz` (lo que
llegó). El veredicto exige AMBOS presentes, finitos, positivos y concordantes dentro de una tolerancia
VERSIONADA; cualquier otra cosa ⇒ `NoObservation` ⇒ `NotCaptured`. iOS declara `expectedCadenceHz = null` con
su límite epistémico en el KDoc. La comparación es numérica contra la tolerancia, NO por banda (`HzBand`):
dos rates distintos pueden caer ambos en `OTHER` y darían un falso «coinciden».

**Regla futura:**
Todo parámetro de referencia `Double` que gobierne una clasificación debe validarse `isFinite() && > 0`
ANTES de usarse — nunca confiar en que un umbral lo cace, porque `NaN` hace falsa toda comparación y produce
el veredicto MÁS favorable en cualquier fórmula de penalización (learning 2026-07-15, dirección máximo). Un
instrumento NUNCA debe clasificar una traza contra un referente derivado de esa misma traza: sin un hecho
externo (lo que la plataforma dice que debería llegar) el estadístico es auto-consistente por construcción y
la degradación se vuelve invisible. Cuando las dos únicas referencias disponibles producen errores opuestos
(falso positivo vs falso negativo), la salida correcta es un TERCER veredicto de «no clasificable», no
escoger el error menos incómodo. Compara rates por tolerancia numérica versionada, no por pertenencia a una
banda con un cajón `OTHER`. Y un buffer del instrumento que se desborda produce `NoObservation`, jamás
`Excluded`: el tamaño de nuestro buffer no es un hecho sobre el trial del usuario y no puede retirarle
accuracy.

**Archivos afectados:**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/timing/{FrameStatsCalculator,TemporalClassPolicy,TemporalWindowObservation,FrameTimeRing}.kt`
- `shared/src/iosMain/kotlin/com/mindos/app/features/mindboost/core/timing/WindowedTemporalProbe.ios.kt`
- `shared/src/desktopTest/kotlin/com/mindos/app/features/mindboost/core/timing/{FrameStatsCalculatorTest,TemporalClassPolicyTest,FrameTimeRingTest}.kt`

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
