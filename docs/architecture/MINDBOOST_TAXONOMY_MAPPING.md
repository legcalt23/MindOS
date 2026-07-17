# Mapeo de Taxonomía MindBoost: 8 Motores de Producto ↔ 9 Dimensiones Internas

**Fecha:** 2026-07-04
**Estado:** Sellado por decisión CEO
**Autor:** Staff Mobile Architect (documento de decisión de arquitectura)
**Tipo:** Documentación de arquitectura — SOLO documentación. No modifica scoring de producción, no sube trials crudos, no implementa IA Coach ni Elite, no migra namespaces, no edita reglas Firestore, no commitea.

## Propósito

Este documento fija, con anclaje en código real (`file:line`), la relación entre lo que el usuario ve (los **8 motores cognitivos** de producto) y la taxonomía interna real (**9 `CognitiveDimension` + 9 `CognitiveModuleId`**). Identifica un **riesgo de confusión concreto y vivo** —la superficie de perfil expone hoy 9 dimensiones al usuario, no 8— y especifica (sin implementar) el cambio mínimo para coherencia, preservando el rigor de scoring (`null != 0`, estados de calibración honestos) que NO se degrada en este turno.

**Alcance:** este documento es EXCLUSIVAMENTE de taxonomía y presentación. NO toca la decisión CEO separada de añadir sync a Firestore (métricas estructuradas, nunca trials crudos); esa continuidad privacy-by-design es un workstream aprobado aparte y aquí no se afirma ni se contradice. La frase "no sube trials crudos" del sello se refiere al estado actual local-first, no a una prohibición permanente de sync.

**Nota de honestidad de verificación:** casi todas las afirmaciones factuales están verificadas contra código con `file:line`. UN punto se marcó inicialmente como UNVERIFIED (la ruta planner/orchestrator) y **se ha resuelto en esta revisión** leyendo `TodayPlanStateHolder` y `MindBoostProfileService`; el resultado se refleja en §6 (R5 verificado como superficie real menor; el plan del hub, verificado como SEGURO).

---

## 1. Los 8 motores de producto (superficie de usuario)

La superficie de producto es exactamente **8 motores cognitivos**. En el hub, cada motor es una `TrackCard` seleccionable en `MindBoostHubScreen.kt` (`HubContent`, `MindBoostHubScreen.kt:223-264`):

1. **Enfoque** — `TrackCard(title = "Enfoque", ...)` (`MindBoostHubScreen.kt:223-227`)
2. **Memoria** — `TrackCard(title = "Memoria", subtitle = "Trabajo, episódica, asociativa y recuerdo diferido.", ...)` (`MindBoostHubScreen.kt:228-232`; subtítulo en `:230`)
3. **Velocidad Mental** — `TrackCard(title = "Velocidad Mental", ... tag = "BETA", ...)` (`MindBoostHubScreen.kt:233-238`)
4. **Lógica** — `TrackCard(title = "Lógica", ...)` (`MindBoostHubScreen.kt:239-243`)
5. **Decisiones** — `TrackCard(title = "Decisiones", ...)` (`MindBoostHubScreen.kt:244-248`)
6. **Emociones** — `TrackCard(title = "Emociones", ...)` (`MindBoostHubScreen.kt:249-253`)
7. **Lenguaje** — `TrackCard(title = "Lenguaje", ...)` (`MindBoostHubScreen.kt:254-258`)
8. **Creatividad** — `TrackCard(title = "Creatividad", ... tag = "PRÁCTICA", ...)` (`MindBoostHubScreen.kt:259-264`)

> El orden de las tarjetas en el hub no coincide con el orden del enunciado, pero el **conjunto es exactamente estos 8** (conteo directo de `TrackCard` en `HubContent`, `MindBoostHubScreen.kt:223-264`). El `sealed interface MindBoostTrack` tiene exactamente **`Hub` + 8 destinos de motor** (`MindBoostHubScreen.kt:383-393`; `Decisiones` es un `data class` que porta `sessionId`/`timestampMs`, el resto `data object`).

El subtítulo de "Memoria" YA anticipa que este motor cubre múltiples facetas: *"Trabajo, episódica, asociativa y recuerdo diferido."* (`MindBoostHubScreen.kt:230`).

---

## 2. Las dimensiones internas reales (9 + 9)

### 2.1. Los 9 `CognitiveDimension`

Fuente única de verdad: `CognitiveTaxonomy.kt:14-24`:

```
enum class CognitiveDimension {
    ATTENTION,             // :15
    PROCESSING_SPEED,      // :16
    WORKING_MEMORY,        // :17
    EPISODIC_MEMORY,       // :18
    REASONING,             // :19
    VERBAL,                // :20
    DECISION_MAKING,       // :21
    EMOTIONAL_REGULATION,  // :22
    CREATIVITY,            // :23
}
```

### 2.2. Los 9 `CognitiveModuleId` (cada uno con su `primaryDimension`)

Fuente: `CognitiveTaxonomy.kt:34-44`:

```
enum class CognitiveModuleId(val primaryDimension: CognitiveDimension) {
    ENFOQUE(ATTENTION),                    // :35
    VELOCIDAD(PROCESSING_SPEED),           // :36
    MEMORIA(WORKING_MEMORY),               // :37
    MEMORIA_EPISODICA(EPISODIC_MEMORY),    // :38
    LOGICA(REASONING),                     // :39
    LENGUAJE(VERBAL),                      // :40
    DECISIONES(DECISION_MAKING),           // :41
    EMOCIONES(EMOTIONAL_REGULATION),       // :42
    CREATIVIDAD(CREATIVITY),               // :43
}
```

El KDoc del enum documenta explícitamente el split de Memoria (`CognitiveTaxonomy.kt:30-32`):

> *"MEMORIA and MEMORIA_EPISODICA are both fed by the Memoria FEATURE (one app module), but inform distinct profile dimensions: complex-span (EX1) + associative (EX4) → WORKING_MEMORY; 24h delayed recall (EX3) → EPISODIC_MEMORY. The split keeps the honest signal (episodic ≠ working memory) without overloading one id."*

**Invariante módulo→dimensión (verificada).** Los 9 `CognitiveModuleId` fijan 9 `primaryDimension` **distintas**: la relación `moduleId → dimension` es uniformemente **1:1** (cada dimensión aparece exactamente una vez, `CognitiveTaxonomy.kt:35-43`). No hay ningún caso 1:2 a nivel de módulo. La única relación **1:2 vive a nivel de MOTOR de producto**: una sola tarjeta "Memoria" del hub abre una sola *feature* de app que produce DOS `moduleId` (`MEMORIA` + `MEMORIA_EPISODICA`) → DOS dimensiones (`WORKING_MEMORY` + `EPISODIC_MEMORY`). Es imprescindible no confundir "motor" con "moduleId": el 1:2 es *motor-producto → {2 moduleId, 2 dimensión}*.

Hay 9 `CognitiveModuleId` y 9 `CognitiveDimension`, pero solo **8 motores de producto** — porque **un motor (Memoria) alimenta dos ids/dimensiones internas**.

---

## 3. Tabla de mapeo: Motor producto (8) ↔ Dimensión(es) interna(s) ↔ ModuleId(s)

| # | Motor producto (usuario) | `CognitiveModuleId`(s) interno(s) | `CognitiveDimension`(s) interna(s) | Relación (motor→interno) |
|---|--------------------------|-----------------------------------|------------------------------------|--------------------------|
| 1 | Enfoque | `ENFOQUE` | `ATTENTION` | 1:1 |
| 2 | Velocidad Mental | `VELOCIDAD` | `PROCESSING_SPEED` | 1:1 |
| 3 | **Memoria** | **`MEMORIA` + `MEMORIA_EPISODICA`** | **`WORKING_MEMORY` + `EPISODIC_MEMORY`** | **1:2 (única)** |
| 4 | Lógica | `LOGICA` | `REASONING` | 1:1 |
| 5 | Lenguaje | `LENGUAJE` | `VERBAL` | 1:1 |
| 6 | Decisiones | `DECISIONES` | `DECISION_MAKING` | 1:1 |
| 7 | Emociones | `EMOCIONES` | `EMOTIONAL_REGULATION` | 1:1 |
| 8 | Creatividad | `CREATIVIDAD` | `CREATIVITY` | 1:1 |

**Anclaje del mapeo `moduleId → dimensión`:** cada `CognitiveModuleId` fija su `primaryDimension` en el constructor del enum (`CognitiveTaxonomy.kt:35-43`), y `ModuleContribution.dimension` la deriva: `val dimension: CognitiveDimension get() = moduleId.primaryDimension` (`ModuleContribution.kt:32`).

**Por qué Memoria es 1:2 y no un solo bloque.** El motor Memoria (una sola *feature* de app, un solo track `Memoria` en `MindBoostTrack`, `MindBoostHubScreen.kt:386`) produce **dos señales cognitivamente distintas**, cableadas por TRES adaptadores de fuente:

- **Memoria de trabajo** (`WORKING_MEMORY`): alimentada por EX1 complex-span (`spanStore` → `MemoryProfileSource`, `MindBoostProfileSources.kt:63`) y EX4 asociativo (`pairsStore` → `PairsProfileSource`, `MindBoostProfileSources.kt:64`); ambas contribuciones portan `moduleId = MEMORIA`, así que el agregador las **funde (blend)** en `WORKING_MEMORY` (KDoc `MindBoostProfileSources.kt:31-34`: *"carry moduleId MEMORIA → the aggregator BLENDS them into WORKING_MEMORY"*).
- **Memoria episódica** (`EPISODIC_MEMORY`): alimentada por EX3 recuerdo diferido 24h (`deferredStore` → `EpisodicProfileSource`, `MindBoostProfileSources.kt:65`); porta `moduleId = MEMORIA_EPISODICA` → su **propia** dimensión `EPISODIC_MEMORY` (`EpisodicProfileSource.kt:35`; KDoc `MindBoostProfileSources.kt:32-34`).

La retención a 24h (episódica) **no es** memoria de trabajo: son constructos con validez distinta. Fundir ambas en una sola cifra ocultaría una señal honesta separada. Por eso la taxonomía interna las mantiene separadas — decisión ya documentada en el KDoc del enum (`CognitiveTaxonomy.kt:30-32`) y en el KDoc de la fuente (`MindBoostProfileSources.kt:32-34`: *"the episodic taxonomy split: 24h retention is not working memory, so it is NOT folded under WORKING_MEMORY"*).

El fold real ocurre en `MindBoostProfileAggregator.aggregate`, que agrupa las contribuciones **por dimensión** (`val byDimension = contributions.groupBy { it.dimension }`, `MindBoostProfileAggregator.kt:37`) y construye **un `DimensionState` por cada uno de los 9 `CognitiveDimension.entries`** (`MindBoostProfileAggregator.kt:38-40`). Es decir: el agregador produce **9 estados de dimensión**, no 8.

---

## 4. Qué ve el usuario HOY (auditoría real)

### 4.1. Hub: 8 labels (CORRECTO)

El hub muestra exactamente **8 `TrackCard`** (§1, `MindBoostHubScreen.kt:223-264`). Aquí el usuario ve 8 motores. Sin problema de coherencia.

### 4.2. Perfil cerebral / radar: 9 dimensiones (RIESGO DE CONFUSIÓN)

El "perfil cognitivo" (contraparte swipe-left de la tarjeta de app) **SÍ está cableado y es alcanzable por el usuario**:

- `ProfileWithBrainProfilePager` monta un `HorizontalPager` de 2 páginas; en la página swipe-left (page 1) instancia `rememberMindBoostBrainProfileStateHolder(userId)` y renderiza `MindBoostBrainProfileScreen(...)` (`ProfileWithBrainProfilePager.kt:27-42`; pager en `:29`, holder en `:33`, pantalla en `:35`).
- El state holder construye el modelo con `BrainProfileUiModelBuilder.build(profile, service.recommend(profile))` tanto en el snapshot cacheado como en el refresh (`MindBoostBrainProfileStateHolder.kt:31` y `:44`).

El builder genera **una `DimensionUi` por cada uno de los 9 `CognitiveDimension`**:

```
dimensions = CognitiveDimension.entries.map { dimensionUi(profile.dimension(it)) }
```
(`BrainProfileUiModelBuilder.kt:40`). `CognitiveDimension.entries` = **9 elementos**.

Esas 9 se renderizan al usuario en DOS superficies simultáneas:

1. **El radar/polígono.** `BrainProfilePolygon(model.dimensions, ...)` (`MindBoostBrainProfileScreen.kt:73`) usa `val axisCount = dimensions.size` (`BrainProfilePolygon.kt:33`) → dibuja **9 ejes/vértices**. No hay ninguna agregación a 8 en la capa de dibujo. (La malla del relleno solo se traza si hay ≥3 vértices medidos, `BrainProfilePolygon.kt:49`, y una dimensión `CALIBRATING`/`null` se pinta como marcador hueco cerca del centro, `:60-72` — nunca como un 0.)
2. **El desglose por dimensión.** `model.dimensions.forEach { DimensionRow(it) }` (`MindBoostBrainProfileScreen.kt:75-77`) → **9 filas**, cada una con su `label`.

Los labels que el usuario lee provienen de `CognitiveDimension.displayLabel()` (`BrainProfileUiModel.kt:56-66`), que incluye **dos etiquetas de memoria separadas**:

- `WORKING_MEMORY -> "Memoria de trabajo"` (`BrainProfileUiModel.kt:59`)
- `EPISODIC_MEMORY -> "Memoria episódica"` (`BrainProfileUiModel.kt:60`)

**Conclusión de la auditoría (verificada):** el radar y el desglose exponen **9 dimensiones**, incluyendo **"Memoria de trabajo"** y **"Memoria episódica"** como **dos bloques separados**. El usuario que selecciona **un** motor "Memoria" en el hub verá **dos** entradas de memoria en su perfil. Esto es una **inconsistencia de superficie real y viva**: 8 motores en el hub, 9 bloques en el perfil.

---

## 5. Qué se guarda internamente: 9 dimensiones (y por qué)

El dominio persiste y agrega **9 dimensiones**:

- `MindBoostProfile.dimensions: Map<CognitiveDimension, DimensionState>` (`MindBoostProfile.kt:73`) — un `DimensionState` por dimensión.
- El agregador crea siempre las 9 (`CognitiveDimension.entries.associateWith { ... }`, `MindBoostProfileAggregator.kt:38`); un test lo fija: `assertEquals(CognitiveDimension.entries.size, profile.dimensions.size)` (`MindBoostProfileAggregatorTest.kt`).

**Por qué se mantiene la granularidad interna (no degradar a 8):**

- **Señal honesta separada:** WM y episódica son constructos distintos; fundirlas internamente perdería información de validez (§3).
- **Blend correcto de WM:** WM ya agrega DOS fuentes internas (span + pairs) vía `groupBy { it.dimension }` (`MindBoostProfileAggregator.kt:37`), con media ponderada por `confidence * dataSufficiency` (`MindBoostProfileAggregator.kt:60-64`). La granularidad interna permite que varias fuentes informen una dimensión sin colisión de id.
- **Rigor de scoring preservado:** `compositeIndex` es media plana SOLO de dimensiones `ESTABLISHED` (`MindBoostProfileAggregator.kt:96-98,107`), con `established.size < minEstablishedForComposite (=3)` → `compositeIndex = null` (`MindBoostProfileAggregator.kt:99-106`; default `minEstablishedForComposite = 3` en `AggregationConfig`, `MindBoostProfileAggregator.kt:22`). Una dimensión puede tener valor `EMERGING` y quedar EXCLUIDA del Global Score (solo cuentan `ESTABLISHED`). `null != 0` está enforced por invariante en `DimensionState.init`: `require((status == CALIBRATING) == (value == null))` (`MindBoostProfile.kt:35-37`).
- **Autoridad de calibración intacta:** `CalibrationPolicy` fija `EMERGING_MIN_SESSIONS = 2` (`CalibrationPolicy.kt:44`), `ESTABLISHED_MIN_SESSIONS = 5` (`CalibrationPolicy.kt:47`), `ESTABLISHED_MIN_DISTINCT_DAYS = 3` (`CalibrationPolicy.kt:50`); validez de sesión = `MIN_DURATION_MS = 120_000L` (`ProfileSessionEligibility.kt:21`, aplicado en `isEligible`, `:23-26`). El cambio de §7 NO toca nada de esto.

La granularidad interna de 9 es correcta y debe conservarse. El problema es **exclusivamente de presentación**: la superficie de usuario no debe exponer los 9 internos crudos.

**Recordatorio sobre la fórmula PRD.** La fórmula ponderada simple del PRD (`0.30*Acc + 0.20*Speed + ...`) es una **baseline conceptual**, nunca una instrucción para degradar el modelo. El scoring real es rigor-first (scorers bespoke por módulo + máquina de estados honesta); este documento no lo altera.

---

## 6. Riesgo de confusión: superficies donde el usuario ve 9 en vez de 8

| # | Superficie | Qué expone | Estado | `file:line` |
|---|-----------|------------|--------|-------------|
| R1 | Radar/polígono del perfil cerebral | 9 vértices (`axisCount = dimensions.size`, `dimensions` = 9) | VIVO | `BrainProfilePolygon.kt:33`, consumido por `MindBoostBrainProfileScreen.kt:73`, alimentado por `BrainProfileUiModelBuilder.kt:40` |
| R2 | Desglose por dimensión (lista de `DimensionRow`) | 9 filas, incl. "Memoria de trabajo" + "Memoria episódica" | VIVO | `MindBoostBrainProfileScreen.kt:75-77` (`forEach` sobre 9), labels `BrainProfileUiModel.kt:59-60` |
| R3 | Builder del modelo de UI | Materializa las 9 (`CognitiveDimension.entries.map`) sin agregar a 8 | VIVO | `BrainProfileUiModelBuilder.kt:40` |
| R4 | `CognitiveModuleId.displayLabel()` | Expone "Memoria" y "Memoria episódica" como labels distintos | VIVO (menor) | `BrainProfileUiModel.kt:72-73` |
| R5 | Tarjeta "Sugerencia" del perfil cerebral | La recomendación puede leer *"Entrena Memoria episódica …"* como si fuera un motor propio | VIVO (menor) | `MindBoostProfileService.recommend` pasa `sources.map { it.moduleId }.toSet()` (incluye `MEMORIA_EPISODICA` vía `EpisodicProfileSource.kt:35`) `MindBoostProfileService.kt:42-43`; texto en `BrainProfileUiModelBuilder.kt:107-116`; label "Memoria episódica" `BrainProfileUiModel.kt:73` |

**R5 — verificado (antes marcado UNVERIFIED, ahora resuelto).** La tarjeta única "Sugerencia" del perfil cerebral usa `service.recommend(profile)`, que pasa como `available` el conjunto `sources.map { it.moduleId }.toSet()` (`MindBoostProfileService.kt:43`). Ese conjunto incluye `MEMORIA_EPISODICA` cuando `EpisodicProfileSource` está cableado. Como `MindBoostProfileOrchestrator.recommend` prioriza `CALIBRATING` primero (`MindBoostProfileOrchestrator.kt:44-46,54-58`) y la episódica arranca `CALIBRATING`, es plausible que la sugerencia diga *"Entrena Memoria episódica para reunir más datos de esta área"* (`BrainProfileUiModelBuilder.kt:111-112`, label `BrainProfileUiModel.kt:73`). Es una superficie de confusión **real** aunque menor (una sola línea de texto, no un bloque del radar).

**Plan del hub (TodayPlanCard) — verificado SEGURO (no es un riesgo).** El "plan de hoy" del hub NO usa `service.recommend`: usa `TodayPlanStateHolder`, cuyo conjunto `DEFAULT_PLANNABLE` es exactamente `{ENFOQUE, MEMORIA, LOGICA, DECISIONES, EMOCIONES}` (`TodayPlanStateHolder.kt:42-48`) y **excluye deliberadamente `MEMORIA_EPISODICA`** (KDoc `TodayPlanStateHolder.kt:20-22`: *"MEMORIA_EPISODICA (same navigable track as Memoria)"*). Además, el `when` de `TodayPlanCard.onTrain` no tiene rama `MEMORIA_EPISODICA` → caería en `else -> Unit` (`MindBoostHubScreen.kt:213-220`). Por tanto la TodayPlanCard **nunca** puede mostrar una fila "Memoria episódica". La confusión NO ocurre por esta vía.

**Onboarding / evaluación inicial:** no existe una pantalla de evaluación inicial cross-motor que presente "N bloques por dimensión". El `OnboardingNote` del hub es texto informativo, no un listado de dimensiones (`MindBoostHubScreen.kt:276-297`); `FocusEvaluationScreen`/`FocusBaselineScreen` son internos del motor Enfoque (baseline por-motor, no un panel global de 9). Por tanto **no hay una superficie de onboarding que exponga 9 dimensiones**; el riesgo se concentra en el perfil cerebral (R1–R3, con R4/R5 menores).

---

## 7. Cambios mínimos para coherencia (ESPECIFICACIÓN, no implementación)

**Objetivo:** que la superficie de usuario del perfil muestre **8 bloques de motor** (uno "Memoria" unificado), conservando internamente las 9 dimensiones y **sin degradar el scoring ni romper `null != 0`**.

### 7.1. Dónde vive el mapper

Nuevo mapper puro de presentación — p. ej. `ProductMotorUiMapper` (u objeto `ProductMotorAggregation`) — en el paquete `...profile.presentation` (junto a `BrainProfileUiModelBuilder.kt`). Es **capa de presentación**, no de dominio: el dominio sigue exponiendo 9 (`MindBoostProfile.dimensions`, `MindBoostProfile.kt:73`). El mapper transforma la `List<DimensionUi>` de 9 en una `List<MotorUi>` de 8, plegando `WORKING_MEMORY` + `EPISODIC_MEMORY` en un único bloque **"Memoria"**. El mapper **reutiliza la FÓRMULA** de media ponderada del agregador (`confidence * dataSufficiency`, `MindBoostProfileAggregator.kt:60-64`), no la función de dominio (que opera sobre `ModuleContribution`, no sobre `DimensionUi`).

### 7.2. Qué consume el mapper

- `BrainProfilePolygon` pasaría a recibir la lista plegada de 8 `DimensionUi`/`MotorUi` — cambio en la llamada `MindBoostBrainProfileScreen.kt:73`. La geometría del canvas **se reutiliza sin tocar**: `axisCount = dimensions.size` (`BrainProfilePolygon.kt:33`) pasa a valer 8, el guard `>=3` para el relleno (`:49`) y el marcador hueco de `null` (`:60-72`) siguen intactos. Clave: el mapper debe emitir exactamente `List<DimensionUi>` (o un tipo estructuralmente idéntico) para que el polígono no cambie de firma.
- El desglose (`MindBoostBrainProfileScreen.kt:75-77`) iteraría sobre 8 elementos. Para no perder la señal episódica, el bloque "Memoria" puede mostrar **memoria episódica como sub-métrica interna** dentro de la fila (dos sub-valores etiquetados "Trabajo" / "Episódica"), preservando la granularidad honesta sin un 9º vértice separado.
- `BrainProfileUiModelBuilder.build` seguiría generando 9 `DimensionUi` (fuente interna intacta, `BrainProfileUiModelBuilder.kt:40`), y el mapper de 8 se aplicaría **después**, como paso de presentación en el borde de UI (o el builder expondría ambas listas: `dimensions` interno de 9 + `motors` de 8). Recomendado: builder devuelve las 9 y el mapper de 8 se aplica en la pantalla, para que los tests de invariantes por-dimensión existentes no cambien.
- **R5 (§6):** la tarjeta "Sugerencia" debe mapear `MEMORIA_EPISODICA` a la etiqueta de motor "Memoria" antes de renderizar el texto (o el `available` de `recommend` debe colapsar los dos ids de memoria a uno de cara al usuario). Cambio localizado en `BrainProfileUiModelBuilder.recommendationUi` (`BrainProfileUiModelBuilder.kt:107-117`) o en el `moduleLabel` derivado (`:108`).

### 7.3. Regla del bloque combinado "Memoria" (preserva `null != 0`)

Regla **única y comprometida** (elegida por el sesgo canónico de *under-claim*, no dos opciones):

- **Estado del bloque = MÍNIMO de los estados de las dos sub-dimensiones** (orden de madurez `ESTABLISHED > EMERGING > CALIBRATING`). No se proclama "Memoria establecida" si la episódica sigue calibrando. (Se descarta explícitamente la opción "máximo", que sobre-afirmaría.)
- **Valor mostrado del bloque Memoria** = media ponderada por `confidence * dataSufficiency` (misma fórmula que `MindBoostProfileAggregator.kt:60-64`) calculada **solo** sobre las sub-dimensiones con valor (`EMERGING`/`ESTABLISHED`).
- **Regla `null != 0` del bloque combinado:** si **cualquiera** de las dos sub-dimensiones no tiene valor (`score0to100 == null`, es decir `CALIBRATING`), el bloque NO fabrica un cero: muestra `"—"` para la parte ausente. Si **AMBAS** están `CALIBRATING`, el bloque entero es `CALIBRATING` con `score0to100 = null` (vértice hueco cerca del centro, exactamente como hoy trata el polígono un score null: `BrainProfilePolygon.kt:60-72`). Nunca se promedia un `null` como 0. Esto respeta el invariante `DimensionState.init` (`MindBoostProfile.kt:35-37`) y la regla de display del builder (`displayValue = score?.toString() ?: EM_DASH`, `BrainProfileUiModelBuilder.kt:65`).

### 7.4. Diff conceptual mínimo

- **NUEVO** `presentation/ProductMotorUiMapper.kt` (mapper puro `List<DimensionUi>(9) → List<MotorUi>(8)` con la regla de §7.3).
- **NUEVO** modelo `MotorUi` (o reutilizar `DimensionUi` con un campo opcional de sub-métrica episódica).
- **EDIT** `MindBoostBrainProfileScreen.kt:73` y `:75-77`: consumir la lista de 8 en lugar de las 9 crudas.
- **EDIT** (menor, R5) `BrainProfileUiModelBuilder.kt:107-117`: mapear la etiqueta de recomendación de `MEMORIA_EPISODICA` al motor "Memoria".
- **SIN CAMBIOS** en dominio (`MindBoostProfileAggregator`, `CognitiveTaxonomy`, `CalibrationPolicy`, `MindBoostProfile`, `ProfileSessionEligibility`), scoring, ni reglas de calibración.
- **Tests nuevos** del mapper: (a) ambas sub-dimensiones `CALIBRATING` → bloque `CALIBRATING`, `score = null` (no 0); (b) una `EMERGING` + una `CALIBRATING` → bloque con valor de la medible, estado = MÍNIMO (`CALIBRATING`), sub-métrica ausente como `"—"`; (c) conteo: 9 `DimensionUi` de entrada → exactamente 8 `MotorUi` de salida.

Este cambio es aditivo, revisable y no toca ninguna de las prohibiciones de este turno.

---

## 8. Sello

- **8 de cara al usuario, 9 interno — justificado.** La superficie de producto son 8 motores; la taxonomía interna son 9 dimensiones porque Memoria alimenta dos señales cognitivamente distintas (trabajo vs. episódica), decisión ya documentada en código (`CognitiveTaxonomy.kt:30-32`, `MindBoostProfileSources.kt:32-34`). La relación 1:2 es *motor-producto → {2 moduleId, 2 dimensión}*; `moduleId → dimensión` es uniformemente 1:1.
- **Estado actual (verificado):** el hub muestra 8 (correcto); el perfil cerebral expone HOY **9** (radar + desglose), incluyendo "Memoria de trabajo" y "Memoria episódica" separadas → **inconsistencia viva** (R1–R3, con R4/R5 menores, §6). Alcanzable por swipe-left vía `ProfileWithBrainProfilePager.kt:29-42`. El plan del hub (TodayPlanCard) está verificado como SEGURO (excluye `MEMORIA_EPISODICA`, `TodayPlanStateHolder.kt:42-48`).
- **Contrato sellado:** ninguna UI principal debe exponer la taxonomía interna cruda de 9. La superficie de usuario expone 8 motores; la granularidad de 9 vive solo en dominio/persistencia y, a lo sumo, como sub-métrica interna dentro del bloque "Memoria". El cambio mínimo de §7 materializa este contrato **sin degradar scoring, sin subir trials crudos, sin migrar namespaces y sin editar reglas** — pendiente de implementación en un turno posterior.
- **Sync a Firestore:** decisión CEO separada (métricas estructuradas, nunca trials crudos, local-first permanece, integra con el eraser de borrado de cuenta, no bloquea el entrenamiento). Este documento no la implementa ni la contradice; es taxonomía-only.

---

**Verificado en código:** taxonomía (`CognitiveTaxonomy.kt`), hub (`MindBoostHubScreen.kt`), builder (`BrainProfileUiModelBuilder.kt`), modelo UI/labels (`BrainProfileUiModel.kt`), pantalla y radar (`MindBoostBrainProfileScreen.kt`, `BrainProfilePolygon.kt`), agregación (`MindBoostProfileAggregator.kt`, `MindBoostProfile.kt`, `ModuleContribution.kt`), calibración (`CalibrationPolicy.kt`, `ProfileSessionEligibility.kt`), fuentes y adaptadores (`MindBoostProfileSources.kt`, `EpisodicProfileSource.kt`), servicio/orquestación/plan (`MindBoostProfileService.kt`, `MindBoostProfileOrchestrator.kt`, `TransversalDailyPlanner.kt`, `TodayPlanStateHolder.kt`), state holder y pager (`MindBoostBrainProfileStateHolder.kt`, `ProfileWithBrainProfilePager.kt`).
**UNVERIFIED restante:** ninguno material. El punto antes abierto (ruta planner/orchestrator) quedó resuelto: R5 verificado como superficie real menor; TodayPlanCard verificado como segura.
