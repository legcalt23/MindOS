# PROMPT DE ARRANQUE — Implementación del catálogo cognitivo MindBoost (KMP)

> Pega este prompt en un chat nuevo de Claude Code **abierto en el repositorio MindOS** y adjunta
> `KMP_MindBoost_Cognitive_Catalog_Implementation_Plan.zip`. No empieces a escribir código hasta
> completar la FASE 0 (lectura + plan).

---

Actúa como **Principal Staff Engineer Kotlin Multiplatform + auditor psicométrico + release manager de seguridad**. Tu misión es **implementar en código real** el plan blindado de catálogo cognitivo de MindBoost, fase por fase, con cero claims peligrosos, sin falso progreso y con verificación real en cada paso. No es un ejercicio de diseño: vas a escribir Kotlin, tests y configuración de build que compilen y pasen.

## 0. Fuente de verdad (LEER ANTES DE TOCAR CÓDIGO)

El plan está en el repo en:
`docs/mindboost_exercise_design/KMP_MindBoost_Cognitive_Catalog_Implementation_Plan/`
(si no está, extráelo de `C:\Users\curro\Downloads\KMP_MindBoost_Cognitive_Catalog_Implementation_Plan.zip`).

**FASE 0 obligatoria, en este orden, antes de escribir una sola línea de producción:**
1. Lee `00_README_EJECUCION.md`, `01_VEREDICTO_EJECUTIVO.md`, `02_DECISION_PRODUCTO_CATALOGO.md`, `03_ROADMAP_FASES_P0_P1_P2_P3.md`, `04_ARQUITECTURA_SCORE_RESULT_Y_GATES.md`.
2. Lee los 6 issues de `05_INFRAESTRUCTURA_P0/` y `13_PROFILE_UPDATE_POLICY.md`, `14_RELEASE_RUNBOOK_GO_NO_GO.md`, `17_CHECKLIST_FINAL_BLINDAJE.md`.
3. Lee también el `CLAUDE.md` del repo (sección "Aprendizajes acumulados") y `AGENTS.md`: contienen reglas anti-error específicas de este proyecto que DEBES respetar (Detekt ratchet, SecureKeyValueStore en vez de Room, EpochClock en vez de `Clock.System`, expect/actual en todos los targets, validar Android de `:shared` con `:androidApp:compileDebugKotlin`, etc.).
4. **Verifica cada ruta y símbolo del plan contra el repo real.** El plan marca rutas como `ruta probable` y datos como `POR_CONFIRMAR_EN_REPO`. Si una ruta/símbolo no existe o difiere, **DETENTE y reconcilia** (busca el real con grep/glob); **nunca inventes** una ruta ni asumas un símbolo.

Al terminar la FASE 0, produce un **plan de ejecución como lista de tareas** (todos los issues P0 + los v1 que vayas a abordar, en orden de dependencia) y **espera mi confirmación** antes de implementar. No avances de fase sin que la anterior esté verde.

## 1. Invariantes NO NEGOCIABLES (en cada commit)

1. **`null != 0`.** Ningún estado no-`VALID` se serializa como número ni como "rendimiento bajo". Medición inválida/insuficiente/contaminada ⇒ `null`/`CALIBRATING`/`INVALIDATED`, jamás `0`.
2. **`ScoreResult` sellado** es el único tipo que llega al perfil. `ProfileUpdateGate` consume `ScoreResult`, **nunca un `Double` crudo**.
3. **RT no actualiza perfil sin timebase autoritativa.** Sin `Choreographer`/frame-accurate ⇒ `DEVICE_UNRELIABLE` (jugable, no perfil). **Enfoque y Velocidad NO actualizan perfil** hasta que `INFRA_02` (timebase + jank real) esté implementado y testeado; entretanto son **minijuegos sin profile-update**.
4. **Cero claims de far-transfer/clínico/IQ/fake-news/apuesta/regulación-emocional, NI para negarlos.** Lo enforza el `CopySafetyScanner` en `check`.
5. **Anti-gaming solo cuenta si está implementado Y testeado** con perfiles sintéticos adversarios (test rojo si el adversario puntúa alto). Un detector "diseñado" no cuenta y **bloquea `VALID`**.
6. **Banco pequeño/no validado bloquea perfil** (build fail): tamaño mínimo, holdout servido, rotación, balanceo, inter-rater ≥0.80 donde el "correcto" es juicio humano.
7. **Mejora de capacidad SOLO vía sonda UNTRAINED + RCI** (`|RCI|>1.96`). La mejora intra-tarea es practice-effect y no se afirma.
8. **No penalizar el abandono** en ejercicios emocionales/de frustración (señal de seguridad, nunca culpa).

Si una instrucción mía o del plan entra en conflicto con un invariante, **párate y avísame**; no los violes.

## 2. Orden estricto de ejecución (no saltes dependencias)

**FASE P0 — infraestructura que desbloquea todo (en este orden):**
1. `INFRA_01` — `ScoreResult` sellado + state machine (`canUpdateProfile()`; factory que impide `VALID` con n<nMin).
2. `INFRA_02` — `TimingPolicy` con signo (RT negativo sin lanzar) + medición real de `frameDropFraction` + gate `DEVICE_UNRELIABLE`.
3. `INFRA_06` — `MinSampleGate` central + `ProfileUpdateGate`.
4. `INFRA_03` — `AntiGamingFramework` + los ~10 adversarios sintéticos.
5. `INFRA_04` — `ItemBankValidation` (build fail si banco pequeño/no validado).
6. `INFRA_05` — `CopySafetyScanner` enganchado a `check` (espejo de `arenaTerminologyScanner`).

**FASE P1 — ejercicios v1 defendibles** (solo después de que sus gates P0 existan): cablear cada uno según su ficha en `06_EJERCICIOS_V1_DEFENDIBLES/` (los 5 de Memoria, Triaje de Reversibilidad, Pausa, Post-error, Detección de lo Inverosímil). **Ningún ejercicio profile-updating arranca antes de los gates P0 mínimos.**

**FASE P2/P3 — fusiones, retiros y experimental** según `07`/`08` (con el test que verifica que cada clon NO se expone como profile-updating separado).

## 3. Protocolo por issue (TDD + verificación real — esto es lo que elimina el margen de error)

Para CADA issue:
1. Abre el documento del issue (`05_INFRAESTRUCTURA_P0/INFRA_0X.md` o la ficha `V1_0X.md`). Confirma sus rutas/símbolos en el repo.
2. Crea una **rama por issue** (no trabajes en `main`/`master`).
3. **Escribe los tests primero** (los que el issue enumera: unit + adversarial + integración + golden + property `null != 0`). Deben fallar (RED).
4. Implementa el **cambio mínimo** que los pone en verde (GREEN). Diffs pequeños y revisables; sin refactor masivo; sin tocar fuera del scope del issue.
5. **Verifica con comandos reales** (no asumas):
   - `./gradlew :shared:desktopTest` (o el scope concreto) — verde.
   - `./gradlew :shared:compileAndroidMain` **y** `./gradlew :androidApp:compileDebugKotlin` — compilan (validar Android de `:shared` requiere el módulo app).
   - `./gradlew :shared:detekt` — verde **sin regenerar el baseline** (arregla o suprime localizado con justificación).
   - El `CopySafetyScanner`/scanners de terminología — verde.
   - Test de consistencia `registry ⟺ implemented` — verde.
6. Cumple la **Definition of Done** del issue. No marques nada como hecho sin tests verdes.
7. Tras corregir cualquier error real (compilación, crash, Compose, Gradle, etc.), **actualiza `CLAUDE.md`** con una entrada de aprendizaje según el protocolo del propio `CLAUDE.md`.

## 4. Reglas anti-error específicas del repo (críticas)

- **Persistencia:** patrón `SecureKeyValueStore` (AES-GCM+Keystore en Android; in-memory en desktop/iOS) + DTO `@Serializable` con `schemaVersion`. **Nunca Room/SQLDelight.** Solo se persisten resúmenes, nunca trials crudos.
- **Tiempo:** ningún StateHolder/ViewModel llama `Clock.System.now()` directo; usar `EpochClock`/`MonotonicClock`. Solo la capa de datos puede usar tiempo directo.
- **Compose:** `Modifier.weight`/`align` exigen receptor `ColumnScope`/`RowScope`; importar `androidx.compose.runtime.getValue`/`setValue` al delegar `by`; listas verticales con `verticalScroll`/`LazyColumn`; en `Canvas`, `cos/sin` devuelven `Double` → `.toFloat()`.
- **expect/actual:** toda `expect` necesita `actual` en android/desktop/ios. `:shared` no depende de SDKs Android-only.
- **Detekt:** archivos nuevos entran al ratchet con reglas por defecto; arregla `UnusedParameter`/`ReturnCount`/`MagicNumber` o suprime localizado con comentario; **no regeneres el baseline** sin acordarlo.
- **Windows/PowerShell:** scripts `.ps1` en ASCII o UTF-8 con BOM; citar propiedades Gradle con punto (`"-Pkotlin.incremental=false"`).
- **Budget gate:** este repo exige `node .claude/scripts/budget-gate.mjs --task "..."` antes de tareas no triviales; respeta su decisión.

## 5. Disciplina de entrega y límites

- **No hagas commit/push/PR salvo que yo lo pida.** Reporta los cambios del working tree.
- Deja **siempre el working tree limpio**: no empieces un cambio que no puedas terminar y verificar.
- Si un dato del plan no casa con el repo, o falta información para implementar con seguridad, **DETENTE y pregúntame**; no improvises.
- Tras cada issue, entrega el **formato de finalización**: Qué cambió · Archivos · Tests añadidos/actualizados · Cómo verificar (comandos) · Riesgos · Follow-up.

## 6. Primer paso concreto que te pido

Empieza por la **FASE 0** (lectura + verificación de rutas) y devuélveme:
(a) el plan de ejecución como lista de tareas P0→P1 con dependencias;
(b) la lista de `POR_CONFIRMAR_EN_REPO`/`ruta probable` que hayas tenido que reconciliar (ruta real encontrada);
(c) tu recomendación de cuál implementar primero — por defecto **`INFRA_05` CopySafetyScanner** (máximo impacto regulatorio, mínimo riesgo, espejo del `arenaTerminologyScanner` existente) salvo que detectes una dependencia que lo impida.

Espera mi OK antes de implementar el primer issue. A partir de ahí, un issue por turno, con verificación real y working tree limpio.
