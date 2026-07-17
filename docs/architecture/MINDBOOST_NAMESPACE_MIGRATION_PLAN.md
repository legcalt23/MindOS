# Plan de Migración de Namespaces de MindBoost — Decisión CEO

**Fecha:** 2026-07-04
**Autor:** Staff Mobile Architect
**Tipo:** Plan — decisión CEO (documento de arquitectura, NO ejecución)
**Estado:** Propuesta para aprobación

> **AVISO CRÍTICO — NADA SE MIGRA EN ESTE TURNO.** Este documento es SOLO documentación. No se renombra ningún paquete, no se mueve ningún archivo, no se toca el scoring de producción, no se suben trials crudos, no se editan reglas de Firestore, no se implementa IA Coach ni Elite, no se hace commit. Todo lo que sigue es un plan por fases, no destructivo, para ejecución futura previa aprobación.

---

## 1. Propósito

MindBoost (dentro de Mindverse) convive hoy con varias raíces de paquete Kotlin fruto de renombrados históricos del proyecto (Mindverse → MindOS). Este documento:

1. Enumera el **estado real** de los namespaces con conteos verificados sobre el código.
2. Fija la **fuente de verdad** y clasifica lo legacy.
3. Cuantifica el **blast radius** (referencias cruzadas de imports).
4. Enumera **riesgos concretos** de una migración en este repo, anclados a aprendizajes ya registrados.
5. Propone un **orden por fases** (lotes pequeños, un símbolo/archivo por paso) con **gates** tras cada fase.
6. Marca **qué NO tocar** todavía.

La superficie de producto de MindBoost son exactamente **8 motores cognitivos**: Enfoque, Decisiones, Lógica, Creatividad, Lenguaje, Emociones, Velocidad mental, Memoria. La taxonomía interna es de **9 dimensiones cognitivas** y **9 module IDs** (el motor Memoria mapea a DOS dimensiones internas: WORKING_MEMORY + EPISODIC_MEMORY; el resto es 1:1). Esto está codificado literalmente en `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/profile/domain/model/CognitiveTaxonomy.kt:14-43` (enum `CognitiveDimension` con 9 valores; enum `CognitiveModuleId` con 9 entradas, incluyendo `MEMORIA(WORKING_MEMORY)` en `:37` y `MEMORIA_EPISODICA(EPISODIC_MEMORY)` en `:38`). **Esta migración de namespaces NO altera** esa taxonomía ni el modelo de scoring.

---

## 2. Árbol actual de namespaces (conteos reales)

Conteos obtenidos con `grep -rl "^package <raíz>"` sobre `shared/src`, `androidApp/src`, `desktopApp/src`, excluyendo copias de worktree (`.claude/worktrees/`) y artefactos generados (`*/build/`).

> **ADVERTENCIA DE FRESCURA — conteos a fecha 2026-07-04.** El árbol se muta en paralelo por agentes concurrentes (Vibe Kanban / n8n). Los números finos de `com.mindos.app` cambian entre corridas seguidas; trátalos como **orden de magnitud** y **regenéralos justo antes de ejecutar cualquier fase**. Los conteos de `com.mindverse` (12 archivos) y los 35 import-sites son estables y sí se reproducen.

| Raíz de paquete | Archivos `.kt` main (aprox.) | Archivos `.kt` incl. tests (aprox.) | Rol |
|---|---|---|---|
| `com.mindos.app` | **~910** (shared) | **~1300** (shared) | **FUENTE DE VERDAD activa** |
| `com.mindverse.mindboost` | **9** (main, commonMain) | **12** (incl. 3 tests) | Legacy core (scoring/dominio/mdp de Decisiones) |
| `com.example.*` | **0** en trees de código real | — | Histórico: ver nota abajo |

**Detalle de `com.mindverse.mindboost` (los 9 archivos main, todos en `shared/src/commonMain/kotlin/com/mindverse/mindboost/core/`):**

- `scoring/ScoringEquations.kt` (`object ScoringEquations`; fórmulas CDE/PHD de Decisiones, `ScoringEquations.kt:11`, `:34`)
- `domain/Models.kt` (modelos `@Serializable`: `Models.kt:9,20,30,44,52,87,93,101,108,117,143,150`)
- `adaptive/DifficultyAdapter.kt`
- `mdp/bayes/BayesEngine.kt`
- `mdp/reversib/ReversibilityEngine.kt`
- `coach/CoachEngine.kt`
- `session/DecisionSessionOrchestrator.kt`
- `storage/Repositories.kt`
- `storage/SqlDelightRepositories.kt`

Tests legacy (3): `commonTest/.../core/tests/AlphaCoreTest.kt`, `commonTest/.../core/tests/DecisionSessionOrchestratorTest.kt`, `desktopTest/.../core/tests/SqlDelightRepositoryTest.kt`.

**Nota sobre `com.example`:** en el árbol de código real (source sets compilados) hay **0** archivos con `package com.example.*`. Los únicos matches fuera de worktrees son:
- `CHAT1VS1.kt` en la RAÍZ del repo — `package com.example.neu` (`CHAT1VS1.kt:1`). Es un archivo suelto en la raíz, **no** dentro de ningún source set de `shared`/`androidApp`/`desktopApp` (no aparece referenciado en `settings.gradle.kts`; trackeado por git pero es un scratch de UI Compose, no código de producción). **UNVERIFIED:** su propósito exacto; a confirmar si es un experimento descartable o debe eliminarse aparte del plan de namespaces.
- `.codex-security-worktrees/platform-hardening/CHAT1VS1.kt` — copia de worktree, fuera de alcance.

Las decenas de matches `com.example` de un grep ingenuo provienen ÍNTEGRAMENTE de `.claude/worktrees/determined-burnell-2714b8/` (copia de árbol de trabajo de un agente, en un estado anterior al renombrado a `com.mindos`). **No es código activo** y no debe migrarse desde este repo.

**`com.mindverse` en `shared/build/generated/`:** SQLDelight genera código bajo `com.mindverse.mindboost.core.storage.db` porque así está configurado en `shared/build.gradle.kts:126` (`packageName.set("com.mindverse.mindboost.core.storage.db")`). Es artefacto regenerable, pero **fija el namespace legacy también desde la config de build** — dato relevante para la migración (ver §5 y §6).

---

## 3. Fuente de verdad y legacy — justificación

**Fuente de verdad = `com.mindos.app`.** Justificación anclada en código:

1. Es la raíz de la aplicación y del `namespace`/`applicationId` de producción: `androidApp/build.gradle.kts:43` (`namespace = "com.mindos.app"`), `:51` (`applicationId = "com.mindos.app"`), y `shared/build.gradle.kts:28` (`namespace = "com.mindos.app.shared"`).
2. Concentra prácticamente todo el código: **~910 archivos main** frente a 9 legacy.
3. Aloja el **modelo de scoring vigente y toda la máquina de estados honesta**, que es innegociable y NO se degrada:
   - `core/scoring/ScoreStateMachine.kt` (máquina de estados honesta CALIBRATING/EMERGING/ESTABLISHED).
   - `profile/domain/calibration/CalibrationPolicy.kt` — umbrales `EMERGING_MIN_SESSIONS = 2` (`:44`), `ESTABLISHED_MIN_SESSIONS = 5` (`:47`), `ESTABLISHED_MIN_DISTINCT_DAYS = 3` (`:50`); techo de elegibilidad en `eligibleCeiling(...)` (`:66-71`).
   - `profile/domain/calibration/ProfileSessionEligibility.kt` — `MIN_DURATION_MS = 120_000L` (`:21`); gate de validez `isEligible(...)` (`:23-26`).
   - `profile/domain/aggregation/MindBoostProfileAggregator.kt` — `compositeIndex` = promedio simple SOLO de dimensiones ESTABLISHED (`:107` `established.mapNotNull { it.value }.average()`), con guard `established.size < config.minEstablishedForComposite → compositeIndex = null` (`:99-106`). Confirma el canon: `null != 0`, una dimensión puede tener valor (EMERGING) y quedar fuera del Global Score, y el índice compuesto exige ≥3 established o es `null`.
   - `profile/domain/model/CognitiveTaxonomy.kt` — taxonomía de 9 dimensiones / 9 IDs.

> **Nota de canon (rigor-first).** El scoring es a medida por módulo (scorers bespoke) y la fórmula ponderada simple del PRD (`0.30·Acc + 0.20·Speed + …`) es una **baseline CONCEPTUAL**, nunca una instrucción para degradar el modelo. Esta migración de paquetes lo deja intacto.

**Legacy = `com.mindverse.mindboost`.** Es el **núcleo original ("Alpha Core") de Decisiones**: scoring de decisiones (`ScoringEquations`), modelos de dominio (`Models.kt`), motores MDP (Bayes, Reversibilidad), orquestador de sesión y repos (in-memory + SQLDelight). Subsistema **autocontenido** consumido HOY por la feature Decisiones bajo `com.mindos` (ver §4). Su namespace antiguo es un residuo del renombrado del proyecto, no una separación arquitectónica deliberada.

**Legacy = `com.example.*`.** Histórico/scratch. En código real solo persiste `CHAT1VS1.kt` (`com.example.neu`) en la raíz, fuera de source sets. No forma parte del grafo de compilación de la app.

---

## 4. Paquetes legacy vs core activo — por subárbol y consumidores (blast radius)

### 4.1 `com.mindverse.mindboost.core` (legacy, 9 archivos) — subárboles y quién los consume

| Subárbol legacy | Contenido | Consumido por |
|---|---|---|
| `core/domain` (`Models.kt`) | `DecisionSession`, `DiaryEntry`, `BayesScenario`, `DifficultyVector`, `ReversibilityScenario`, `CognitiveReadinessState`, `BrierPrediction`, `CoachTemplate`, `SensorEvidence`, `PredictionEvent`… (todos `@Serializable`) | Toda la feature `com.mindos.app.features.mindboost.decisiones` (data + presentation + ui) y el propio core legacy |
| `core/scoring` (`ScoringEquations`) | Fórmulas CDE/PHD de Decisiones | `core/session/DecisionSessionOrchestrator.kt:13`; test `AlphaCoreTest.kt:13` |
| `core/mdp/bayes` (`BayesEngine`) | Motor bayesiano de escenarios | `decisiones/presentation/BayesArenaStateHolder.kt:13`; `core/session/DecisionSessionOrchestrator.kt:12` |
| `core/mdp/reversib` (`ReversibilityEngine`, `ReversibilityDecisionType`) | Motor de triaje de reversibilidad | `decisiones/presentation/ReversibilityTriageStateHolder.kt:13-14`; state/UI de reversibilidad |
| `core/session` (`DecisionSessionOrchestrator`, `CheckInInput`, `DecisionRound`, `DecisionSessionResult`) | Orquestación de la sesión de Decisiones | ≥7 state holders y screens de `decisiones` |
| `core/coach` (`CoachEngine`, `CoachSignals`, `CoachTemplate`) | Motor de mensajes de coach (interno de Decisiones) | `core/session/DecisionSessionOrchestrator.kt:5-6`; `AlphaCoreTest.kt:3` |
| `core/adaptive` (`DifficultyAdapter`) | Adaptación de dificultad de Decisiones | Interno del core legacy (hoja: no importa `com.mindverse` de otros subárboles) |
| `core/storage` (`Repositories`, `SqlDelightRepositories`, `InMemory*Repository`) | Persistencia de sesiones/diario | Tests (`AlphaCoreTest.kt:14-16`, `SqlDelightRepositoryTest.kt`); usa código generado por SQLDelight en `com.mindverse.mindboost.core.storage.db` |

**Dirección de dependencias (verificada):** `com.mindverse` **nunca importa `com.mindos`** (grep de `import com.mindos` dentro de `com/mindverse/` → 0 resultados). El core legacy es una **hoja de dependencias limpia**: solo importa `com.mindverse.*` + stdlib. Esto es favorable: se puede migrar sin arrastrar `com.mindos` hacia dentro.

### 4.2 Blast radius por imports cruzados — partición EXACTA (verificada)

**35 archivos** contienen `import com.mindverse` (excluyendo worktrees y `build/`). Partición reproducible por namespace propietario × source set:

| Propietario del archivo | commonMain | commonTest | desktopTest | Total |
|---|---:|---:|---:|---:|
| **Consumidores `com.mindos…decisiones`** | 15 | 0 | 9 | **24** |
| **Intra-`com.mindverse` (legacy)** | 8 | 2 | 1 | **11** |
| **Total** | 23 | 2 | 10 | **35** |

Lectura: la migración del core legacy tocaría **15 archivos de producción + 9 de test** en la feature Decisiones (los 24 consumidores) **más** los 11 import-sites intra-legacy (de los 12 archivos legacy, uno — `DifficultyAdapter.kt`, hoja pura — no importa ningún `com.mindverse`). El blast radius está **acotado a la feature Decisiones y su core**, NO a toda la app.

---

## 5. Riesgos de migración concretos de este repo

Cada riesgo está anclado a un aprendizaje ya registrado en `CLAUDE.md` (sección "Aprendizajes acumulados del proyecto") o a un hecho verificado.

1. **Referencias cruzadas de imports (35 archivos).** Renombrar un símbolo/paquete legacy rompe simultáneamente todos sus import sites. Mitigación: un archivo por paso, actualizando sus consumidores en el MISMO cambio. Aprendizaje aplicable: *2026-06-28 — "Añadir una variante a un sealed UiState y su renderer son la MISMA fase"* (el compilador exhaustivo obliga a tocar consumidores en el mismo commit).

2. **Serialización de datos cifrados — VERIFICADO-SEGURO (no bloquea).** Se auditó si el nombre de paquete de los modelos legacy `@Serializable` (`Models.kt`) contamina los blobs cifrados en disco. **No lo hace:** `SecureDecisionSessionRepository` NO serializa el modelo de dominio `com.mindverse` directamente — define un **DTO privado** `DecisionSessionDto` (paquete `com.mindos.app.features.mindboost.decisiones.data`, `SecureDecisionSessionRepository.kt:13-27`) y mapea con `toDto`/`toDomain` (`:68-86`); `save()` codifica `DecisionSessionDto.serializer()` (`:41`) y `parse()` lo decodifica (`:54-55`). El blob en disco es el **DTO**, cuyo FQN ya es `com.mindos`, y kotlinx-serialization JSON **no** incrusta el nombre de paquete para clases `@Serializable` no polimórficas. Por tanto, renombrar el paquete de `Models.kt` **no invalida** blobs ya escritos: la forma persistida está desacoplada del modelo de dominio legacy. (`SecureDiaryRepository` sigue el mismo patrón DTO.) Aprendizajes relacionados que confirman la disciplina DTO/versionado: *2026-06-19 — "El modelo tipado de storage debe nacer compatible…"* y *"Un prefijo versionado no puede confundirse con Base64 legacy"*. **Residual (bajo, no bloqueante):** confirmar por igualdad que `SecureDiaryRepository` mantiene su propio DTO local antes de mover `Models.kt`.

3. **Configuración SQLDelight fija el namespace legacy.** `shared/build.gradle.kts:126` genera en `com.mindverse.mindboost.core.storage.db`. Mover `core/storage` exige cambiar `packageName` y regenerar; los imports del código generado (`DecisionSessionQueries`, la clase `Database`) cambian de paquete. Aprendizajes: *2026-06-16 — "SQLDelight añade Queries al nombre del archivo .sq"* y *"custom Boolean requiere adapter explícito"* — el pipeline SQLDelight es sensible a nombres; cualquier cambio de `packageName` debe validarse con `:shared:desktopTest` (que ejercita `SqlDelightRepositoryTest`).

4. **Igualdad estructural de `AppGraph` (data class).** `core/di/AppGraph.kt` es un `data class` cuya igualdad estructural está fijada por `AppGraphTest.graph_isEquatable_dataClass`, y contiene el `SecureNamespaceUserDataEraser` con la lista de namespaces `AccountBound`. Aprendizajes: *2026-06-19 — "Los defaults mutables rompen la igualdad estructural de AppGraph"* y *2026-06-28 — "Añadir un namespace a AccountBound exige actualizar el eraser"*. Riesgo: si la migración toca `AppGraph` o los namespaces, preservar defaults estables (singletons no-op) y actualizar el pin del test.

5. **PAQUETE ≠ NAMESPACE de storage (regla dura).** Los strings `mindboost_decisiones`, `mindboost_profile` (`MindBoostStorageNamespaces.kt:19`) y demás son **STRINGS de almacenamiento**, NO paquetes Kotlin. Renombrar paquetes **NO debe cambiar** esos strings — hacerlo rompería el acceso a datos cifrados ya escritos y abriría un hueco GDPR (los namespaces `AccountBound` deben seguir cubiertos por el eraser; aprendizaje 2026-06-28).

6. **Baseline de Detekt (ratchet one-way).** Editar un archivo legacy lo reintroduce en el ratchet con su deuda previa. Aprendizaje: *2026-06-19 — "Editar archivos legacy activa su deuda completa en el ratchet Detekt"*. `ScoringEquations.kt:1` ya lleva `@file:Suppress("LongParameterList","MagicNumber","ReturnCount","TooManyFunctions")` — al moverlo hay que **preservar esas supresiones** o Detekt fallará. Riesgo colateral: `TopLevelPropertyNaming` (aprendizaje 2026-07-01) y `MaxLineLength` pueden dispararse si el renombrado alarga líneas.

7. **expect/actual multiplataforma.** Verificado: **el core legacy `com.mindverse` NO tiene expect/actual** (grep en common/android/desktop/ios → 0). Riesgo BAJO para este subárbol. PERO la regla general sigue vigente para cualquier otro paquete que sí los tenga: aprendizaje *2026-05-28 — "Cada expect DEBE tener actual en TODOS los targets"*. Antes de mover cualquier paquete, confirmar con grep que no contiene `expect`/`actual`; si los contiene, validar los 3 targets (`compileAndroidMain`, `compileKotlinDesktop`, e iOS si aplica en macOS).

8. **Romper tests (11 tests legacy/decisiones + 9 de test-decisiones).** Los tests importan los tipos legacy por FQN. Cada paso de renombrado debe actualizar sus tests en el mismo cambio y correr `:shared:desktopTest`. Aprendizaje: *2026-06-19 — "Los gates Gradle del mismo worktree deben ejecutarse en serie"* (no paralelizar detekt/compile/test sobre el mismo `shared/build`).

9. **Fricción con agentes concurrentes (Vibe Kanban / n8n).** El árbol se muta en paralelo (aprendizaje 2026-07-01, nota de entorno). Un renombrado en bloque colisiona con WIP ajeno. Mitigación: lotes mínimos, un símbolo por paso, verificar SOLO los archivos propios en cada gate.

---

## 6. Orden de migración por fases (lotes pequeños)

Principio: **primero las hojas sin dependientes internos; luego los modelos; el core de scoring y los motores MDP al final por su blast radius.** Un símbolo/archivo (o par archivo+consumidores) por paso. **NADA de esto se ejecuta este turno.** Cada fase termina en verde antes de empezar la siguiente.

> **Paquete destino — DECISIÓN DE PRODUCTO PENDIENTE (UNVERIFIED).** Se propone reubicar el core legacy bajo `com.mindos.app.features.mindboost.decisiones.core.*` (queda dentro de la feature que ya lo consume, dirección de dependencias correcta), o bajo `com.mindos.app.features.mindboost.core.*` si se decide compartirlo. El destino exacto lo fija el CEO **antes de la Fase 1**. Los pasos siguientes se redactan **agnósticos al destino** (`<paquete destino>`) para no asumir una decisión no aprobada.

**Fase 0 — Preparación (sin mover código).**
- Confirmar por igualdad que `SecureDiaryRepository` usa su propio DTO local (riesgo §5.2 ya VERIFICADO-SEGURO para sesiones); round-trip opcional sellar→guardar→reabrir con repos frescos sobre el mismo store como red de seguridad.
- Fijar el `<paquete destino>` con el CEO.
- Confirmar (grep) que `com.mindverse` no tiene expect/actual (ya verificado: no los tiene) ni imports hacia `com.mindos` (ya verificado: no los tiene).
- Regenerar los conteos del §2 sobre el commit vigente.

**Fase 1 — Hojas sin dependientes internos.**
- `core/coach` (`CoachEngine`, `CoachSignals`, `CoachTemplate`) → `<paquete destino>`: solo lo consumen `DecisionSessionOrchestrator` y `AlphaCoreTest`. Mover + actualizar 2 consumidores.
- `core/adaptive` (`DifficultyAdapter`) → `<paquete destino>`: consumo interno mínimo (no importa otros subárboles legacy).

**Fase 2 — Modelos de dominio (`core/domain/Models.kt`) → `<paquete destino>`.**
- Es la dependencia más ancha (importado por ~todos los consumidores de `decisiones` + core legacy). La compatibilidad de serialización ya está VERIFICADA-SEGURA (§5.2): el blob es un DTO `com.mindos`, no el modelo. Actualizar todos sus import sites (data/presentation/ui/state de `decisiones` + tests) en lotes por tipo si es necesario, **preservando intactos los strings de storage-namespace**.

**Fase 3 — Persistencia (`core/storage`) → `<paquete destino>`.**
- Cambiar `packageName` de SQLDelight en `shared/build.gradle.kts:126`, regenerar, mover `Repositories.kt`/`SqlDelightRepositories.kt` y actualizar imports del código generado. Validar `SqlDelightRepositoryTest`.

**Fase 4 — Motores MDP (`core/mdp/bayes`, `core/mdp/reversib`) → `<paquete destino>`.**
- Blast radius medio (state holders de Bayes y Reversibilidad). Un motor por paso.

**Fase 5 — Orquestación (`core/session/DecisionSessionOrchestrator`) → `<paquete destino>`.**
- Depende de coach + domain + bayes + scoring. Migrar tras todos ellos.

**Fase 6 — Core de scoring (`core/scoring/ScoringEquations`) → `<paquete destino>` — AL FINAL.**
- Es el símbolo con mayor riesgo semántico (fórmulas CDE/PHD de Decisiones) y el que más suprime en Detekt (`ScoringEquations.kt:1`). Migrar el ÚLTIMO, cuando todo lo demás esté migrado y verde, **preservando las `@file:Suppress` existentes**. **No cambiar ni una constante ni una fórmula** — es un renombrado de paquete puro, verificable por `AlphaCoreTest`.

**Fase 7 — Limpieza de residuos.**
- Decidir destino de `CHAT1VS1.kt` (`com.example.neu`, raíz) — probablemente eliminar (fuera del plan de namespaces, tarea aparte).
- Retirar la copia de worktree `.claude/worktrees/...` con `com.example` si procede (fuera del grafo de compilación; no bloquea).

---

## 7. Gates OBLIGATORIOS tras CADA fase

Ejecutar **en serie** (nunca en paralelo sobre el mismo `shared/build` — aprendizaje 2026-06-19), verificando SOLO que los archivos propios pasan (el árbol muta por agentes concurrentes):

1. `:shared:compileKotlinDesktop` — compila el target Desktop del módulo compartido.
2. `:shared:desktopTest` — corre la suite JVM (incluye `AlphaCoreTest`, `DecisionSessionOrchestratorTest`, `SqlDelightRepositoryTest` y los tests de `decisiones`).
3. `:shared:detekt` — ratchet de estilo (verificar que el archivo migrado NO aparece en findings; preservar `@file:Suppress`; **no regenerar el baseline para silenciar**).
4. `:androidApp:compileDebugKotlin` — compila `:shared:compileAndroidMain` end-to-end desde la app (necesario porque `:shared` solo no detecta impls incompletas de otros módulos; aprendizaje 2026-06-22).
5. **Scanners aplicables** cuando el archivo migrado esté en su scope: `:shared:mindboostCopySafetyScanner` (+ variantes `…CommonTest`/`…DesktopTest` si se editan tests; aprendizaje 2026-07-01). `:shared:arenaTerminologyScanner` solo si se tocara `arena` (no aplica aquí).
6. Si el IDE posee el daemon Gradle en este worktree, **no invocar Gradle CLI**: usar verificación estática/compilación alternativa y entregar los comandos de gate al usuario (convención del proyecto).

**Regla de avance:** no pasar a la siguiente fase si algún gate falla.

---

## 8. Qué NO tocar todavía

1. **No renombrar en bloque.** Nada de "buscar/reemplazar `com.mindverse` → `com.mindos`" sobre todo el árbol. Un símbolo/archivo por paso, con sus consumidores, y gate entre pasos.
2. **No mover el core de scoring (`ScoringEquations`) ni `core/mdp/bayes`/`reversib` hasta el final**, cuando el resto esté migrado y verde. Son el mayor blast radius semántico.
3. **No tocar paquetes con expect/actual sin validar los 3 targets** (Android/Desktop/iOS). El core legacy `com.mindverse` no los tiene (verificado), pero la regla aplica a cualquier otro paquete que se aborde después.
4. **No confundir renombrado de PAQUETE con renombrado de NAMESPACE de storage.** Los strings `mindboost_decisiones`, `mindboost_profile` (`MindBoostStorageNamespaces.kt:19`) y demás deben permanecer BYTE A BYTE iguales — cambiarlos rompería el acceso a datos cifrados ya escritos y el `AccountBound` eraser (hueco GDPR).
5. **No cambiar el modelo de scoring ni sus umbrales.** Ni `CalibrationPolicy` (`EMERGING_MIN_SESSIONS=2`, `ESTABLISHED_MIN_SESSIONS=5`, `ESTABLISHED_MIN_DISTINCT_DAYS=3`), ni `ProfileSessionEligibility.MIN_DURATION_MS=120_000L`, ni la regla de `compositeIndex` (promedio de ESTABLISHED, ≥3 o `null`). La migración es de PAQUETES, no de lógica. La fórmula ponderada simple del PRD es una **baseline conceptual**, nunca instrucción para degradar el modelo.
6. **No subir trials crudos, no implementar IA Coach/Elite, no editar reglas de Firestore, no hacer commit** — fuera del alcance de este documento. El sync Firestore privacy-by-design (guardado local inmediato que permanece, solo métricas estructuradas — nunca trials crudos de alta granularidad en MVP, reglas estrictas `auth.uid == userId` + App Check + lecturas acotadas + escrituras validadas + sin acceso cross-user + sin auto-Elite de cliente, consentimiento GDPR, integración con el eraser de borrado de cuenta, y que **nunca bloquee el entrenamiento**) es una decisión CEO separada, a planificar en su **propio documento**.
7. **`CHAT1VS1.kt` y las copias de worktree** se tratan como limpieza separada (Fase 7), no como parte del renombrado de namespaces del core.

---

**Cierre — honestidad de estado.** Este plan cubre la migración de namespaces con conteos (aproximados, con marca de frescura) y blast radius (partición exacta 15+9+11) verificados sobre el código real. NO afirma cumplimiento total del PRD: el sync Firestore, el IA Coach, Elite y el aterrizaje del scoring en cada uno de los 8 motores son trabajos separados y **parcialmente pendientes**. El riesgo de serialización que un borrador previo marcó como bloqueante quedó **VERIFICADO-SEGURO** (§5.2): la persistencia usa un DTO `com.mindos`, no el modelo legacy, por lo que renombrar `Models.kt` no invalida datos ya escritos. El único punto marcado **UNVERIFIED** que precede a la ejecución es la elección del `<paquete destino>` (decisión de producto, §6), a fijar por el CEO antes de la Fase 1.