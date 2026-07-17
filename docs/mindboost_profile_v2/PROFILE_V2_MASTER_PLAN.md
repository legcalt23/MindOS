# PROFILE V2 — Master Plan (REOPEN)

- **Estado:** `REOPEN` (doc-only). No es DONE. Reabierto por el CEO; el cierre anterior queda superado por este archivo.
- **Base SHA:** `71e17038733e23a603c8ecb85ee5ff9f965a62db` (detached HEAD).
- **Worktree:** SUCIO — contiene el trabajo 6-R3 C1–C5 en vuelo (ajeno a esta reapertura, preservado, §3).
- **Autoridad y precedencia (00 §3):** 1) ley/seguridad/privacidad/protección del usuario · 2) fuente de verdad +
  objetivo supremo del CEO · 3) `03_SCIENTIFIC_MEASUREMENT_CHARTER` · 4) `01_MINDBOOST_PRODUCT_PRD` ·
  5) `09_DECISION_LOG` · 6) slice/contrato · 7) código. **El código no es requisito por existir.**
- **Alcance de naming:** "Molvex" = naming de producto. CERO renombres de paquetes/clases/ids/namespaces (non-goal §18).

> **Regla madre (heredada, no demolible):** la demolición es de la CAPA DE ESTIMACIÓN (score/confidence/sufficiency/
> trend fabricados ad-hoc por adapter + media ponderada del aggregator), NUNCA de la CAPA DE INVARIANTES (missing≠0,
> gates fail-closed, CALIBRATING→"—", evidencia sellada, `evidence` no importa `profile.*`, sufficiency⊥uncertainty).

> **REOPEN R2 — correcciones de falsación aplicadas (2026-07-12).** Siete revisores en contexto fresco falsaron
> este archivo contra el código vivo (no contra HEAD). Veredictos: UX `NO APTO cond.` · psicometría-readiness
> `REOPEN` · modelo-de-medición `REOPEN` · honestidad/eficacia `NO APTO` · arquitectura-KMP `No apto F0` ·
> datos/sync `CONDICIONAL` · privacidad/seguridad `rechazo condicional`. Hallazgo transversal decisivo, **re-verificado
> por mí leyendo el archivo** (no confié en los agentes): el worktree mutó durante la reapertura y
> `MindBoostProfileAggregator.summarize` **SÍ** consulta hoy `mindboostGlobalScoreEnabled` (provisional) y la constante
> `ADR_SCI_006_APPROVED` (oficial) — la afirmación previa "flag no cableado / half-landed" era **FALSA**. Todas las
> correcciones P0/P1 verificadas se integran abajo (§3/§4/§6/§7/§7-data/§9/§10/§10-sync/§11/§14/§15/§16/§18/§19/§20/
> §21/§25/§30). Regla metodológica reforzada: cada afirmación "VERIFIED" se contrasta con `git diff HEAD` del archivo
> citado, no solo con un `Read` puntual — un worktree que muta en paralelo puede invalidar el snapshot de redacción.

---

## 1. Executive verdict

Profile V2 reconstruye la medición del perfil cognitivo sobre cuatro fronteras científicas explícitas
(observation-model por familia → serie comparable → estimador longitudinal candidato → proyección de
subhabilidad/bloque), alimentado por un **único** camino de ingestión facts-only (6-R3) + los runs/stores
inmutables subject-scoped como fuente primaria, con snapshot versionado como caché descartable, UI honesta y un
contrato de sync backend-agnóstico **apagado** hasta ADR-DATA. **Ningún modelo se cierra por elegancia ni por
coste O(1);** normal-normal es un candidato CONDICIONADO por familia, no la solución universal. **Global OFICIAL
OFF por constante** `ADR_SCI_006_APPROVED` (robusto: ni flag ni banco ni Remote Config lo abren); **Global
PROVISIONAL** (SCO-014) gated por el flag remoto `mindboostGlobalScoreEnabled` + `established.size≥3` — hoy
inalcanzable solo por barrera INCIDENTAL (`≤2` dimensiones), que F9 vigila (§15/§20). **F1 queda mecánicamente
BLOQUEADA** por dos prerequisitos verificados: (a) 6-R3 no está reproduciblemente verde y arrastra un P1 en esta
capa; (b) `ADR-SCI-003` (estimador longitudinal) abierto. La pista de ítem/IRT está **BLOQUEADA** (instrumentación
de nivel-ítem inexistente, §7-data); la pista de persona (F4) no.
Este documento es doc-only: no toca código, Gradle, flags, wiring, adapters ni sync.

---

## 2. Scope y non-scope

**In-scope:** el perfil COGNITIVO `features/mindboost/profile/**` (dominio, agregación, adapters, calibración,
display, presentación) y sus consumidores en `HomeScreen`/hub; el contrato de consumo del paquete facts-only
`core/session/cognitive/evidence/**` (solo CONSUMO por la puerta sancionada, sin editarlo); el diseño (no la
activación) del measurement core v2, la persistencia/snapshot v2 y el contrato de sync portable.

**Non-scope (§18):** rebrand a Molvex en código · migración física a VPS · `features/profile/card` (app profile
card) y perfil social · ranking público/World Class/PvP (SCO-017/019) · features nuevas de ejercicio / E9 /
mappers por-módulo (Fase B del scoring) · IA Coach y su DTO · editar el paquete `evidence` (territorio 6-R3) ·
resolver `ADR-SCI-005..009`/`ADR-DATA-001/002` más allá de posiciones interinas declaradas · normas
poblacionales/comparativas entre usuarios (el perfil v2 es intra-persona, vs tu propia línea base).

---

## 3. Baseline repo evidence (VERIFIED, `git`/lectura directa)

- **`git rev-parse HEAD`** = `71e17038…` (detached). **Frescura del grafo:** `graphify-out/GRAPH_REPORT.md` fue
  construido desde `35016318` ≠ HEAD ⇒ **grafo OBSOLETO**: toda aserción de este plan sobre código se verificó por
  lectura/`grep` directos, no por el grafo (regla de la reapertura §2).
- **`git status --short`:** ~92 entradas. **El trabajo 6-R3 vive en ESTE worktree** y es ajeno — modificados
  `core/session/SecureSessionRecordStore.kt`, `SessionRecordStore.kt`, `SessionRecoveryScanner.kt`,
  `core/config/FeatureFlags.kt`, `core/di/AppGraph.kt`, `profile/presentation/BrainProfileUiModelBuilder.kt`,
  `MindBoostProfileAggregatorTest.kt`; untracked `core/session/cognitive/**`, `storage/LegacyNamespacePurge.kt`,
  `core/scoring/normalization/**`, `core/sync/**`, `focus/data/sync/**`, tests de purga cross-account. **No se
  tocan.** Lo ÚNICO propio de esta reapertura: `docs/mindboost_profile_v2/` (untracked).
- **Canon 2.0:** `docs/context_pack/` (untracked, 10 docs) — leídos 00/01/03/04/05/07/08/09 completos + greps
  dirigidos de 02/06. Es la autoridad (6-R3 §0 verificó su SHA byte-a-byte contra el adjunto del CEO).
- **Estado real de 6-R3 (`SYSTEM1_SLICE_6R3_REOPEN.md`):** contrato C1–C5 registrado; su cierre final (tras §26,
  L888) dice literal **"Gates RETENIDOS. NOT DONE."**; P1 abierto `SessionRecordStore` (S6R3-003, foldeado §14 pero
  pendiente de cierre); ADRs SCI-003/004/005/006/008/009 y DATA-001/002 **ABIERTOS** (SCI-007 no es dependencia de
  este plan). ⇒ **F1 BLOQUEADA** (§21).
- **Inventario de los 9 adapters legacy + su fuente de datos REAL (VERIFIED por imports/ctor):**

  | Adapter (`profile/data/adapter/`) | Dimensión | Lee (fuente durable) | ¿Lee `CognitiveSessionRun`? |
  |---|---|---|---|
  | `FocusProfileSource` | ATTENTION | `FocusProfileRepository` (`SessionResult`) + `FocusScoreCalculator`/`TrendAnalyzer` | **No** |
  | `LogicProfileSource` | REASONING | `LogicTrainingRepository` (`LogicSession`) | **No** |
  | `SpeedProfileSource` | PROCESSING_SPEED | `SpeedMeasurementStore` (`SessionRecord`/`SessionMetrics`) + `SpeedAntiGaming` | **No** (E8 sí emite runs, pero el adapter lee el store speed, no el run-store) |
  | `LanguageProfileSource` | VERBAL | `EvidenceRepository`+`SessionSummaryRepository` (hoy pasado `null` → CALIBRATING) | **No** |
  | `EmotionProfileSource` | EMOTIONAL_REGULATION | `EmotionProfileReadOnly` (`normalizedScore` SIEMPRE null) | **No** |
  | `MemoryProfileSource` | WORKING_MEMORY | `SpanResultStore` | **No** |
  | `PairsProfileSource` | (WM asociativa) | `PairsResultStore` | **No** |
  | `EpisodicProfileSource` | (memoria diferida) | `DeferredResultStore` | **No** |
  | `DecisionsProfileSource` | DECISION_MAKING | `ReversibilityResultStore` | **No** |

- **Consumidor vivo → UI (VERIFIED):** `RememberBrainProfileStateHolder` cablea Emotion/Focus/Logic/Memory(3)/
  Decisions/Speed (Language `null`) → `mindBoostProfileSources` → `MindBoostProfileService.refresh` (pull sources
  → `aggregator.aggregate` → persist a `mindboost_profile`) → `BrainProfileUiModelBuilder` →
  `UserDisplayScoreMapper.displayScore(state)` → `DisplayScore.{ProvisionalStable|ProvisionalEmerging|Withheld}`
  (VERIFIED `UserDisplayScoreMapper.kt:44-53` — **NO existe `Official`**: el rename C2.4-5 de 6-R3 ya aterrizó). **Un
  número SÍ puede llegar a UI**: `ProvisionalEmerging` con `confidence ≥ 0.55` en EMERGING (`DISPLAY_CONFIDENCE_GATE`),
  `ProvisionalStable` en ESTABLISHED — en AMBOS casos un ENTERO 0-100 **DESNUDO sin banda** (`toPercent`, `:89-90`),
  falsa precisión que §14 corrige. Por el gate de banco vacío (efficacy 6-R3 §6.2), hoy **solo Focus (ATTENTION) y
  Logic (REASONING)** pueden producir contribución no-nula ⇒ únicas dimensiones que pueden surfacear un número; y
  DC-3 está ACTIVO para ambas (`CalibrationPolicy` gatea ESTABLISHED por `validSessions≥5 && distinctDays≥3` **sin
  floor de 48h** — over-claim vivo, §21/F5).
- **Runs para recomputación (VERIFIED — hallazgo decisivo):** el ÚNICO productor de `CognitiveSessionRun` en
  producción es `GoNoGoCognitiveSessionAdapter.toCognitiveSessionRun` (`:119`) → `GoNoGoSessionStateHolder` →
  `CognitiveSessionRunStore` (subject-scoped, `SpeedRootScreen.kt:75`). **Los otros 8 módulos NO emiten runs.**
  `CognitiveSessionRun` **no tiene campo `userId`/`subject`** (identidad implícita en el namespace del store).
  ⇒ **"recompute desde runs" es imposible hoy para 8/9 módulos**; su única fuente durable son sus stores
  por-módulo (§12 corrige la matriz de migración en consecuencia).
- **Schemas/snapshots (VERIFIED):** `MindBoostProfileDtos.kt` tiene `schemaVersion: Int = MindBoostProfileSchema.CURRENT`;
  `LocalMindBoostProfileRepository` persiste por-uid (`PROFILE_KEY_PREFIX + userId`) con decode versionado
  (`versionOf = ::schemaVersion`). Namespace `mindboost_profile` en `AccountBound` con eraser local; **sin espejo
  cloud** (ningún `mindboost_*` aparece en `functions/src/accountDeletion.ts`).
- **Flag C2.5 (VERIFIED — YA CABLEADO; corrige el cierre previo):** `FeatureFlags.mindboostGlobalScoreEnabled`
  existe (`:60`, default `false` en las 3 impls) **Y `MindBoostProfileAggregator.summarize` SÍ lo consulta** hoy
  (re-leído esta sesión, `MindBoostProfileAggregator.kt:122-173`): tras el gate previo `established.size ≥ 3`,
  `summarize` computa DOS índices con barreras propias — `provisionalCompositeIndex` (`:163-173`) = `null` salvo
  `globalScoreEnabled()` (`:39` → `FeatureFlagsHub.current.mindboostGlobalScoreEnabled`, flag **remoto**), y
  `officialCompositeIndex` (`:150-155`) = `null` salvo la **constante** `ADR_SCI_006_APPROVED` (`:56`, hardcoded
  `false`) **y** las 9 dimensiones ESTABLISHED. El aggregator de PRODUCCIÓN figura `M` en `git status` (no solo su
  test): es trabajo 6-R3 C2.5 que aterrizó en ESTE worktree, ausente en HEAD `71e17038`. ⇒ el claim previo "flag no
  cableado / half-landed" era **FALSO**; `established.size ≥ 3` es gate PREVIO, no el único. Consecuencia de barrera
  en §15/§20 (Global oficial vs provisional).

---

## 4. Claims matrix (afirmaciones del plan ANTERIOR reclasificadas)

| # | Claim del cierre anterior | Estado | Evidencia / corrección |
|---|---|---|---|
| C1 | "Measurement core = normal-normal DLM por dimensión" | `CONTRADICTED` | Las familias tienen modelos de observación distintos (accuracy/ICR, RT/`SessionMetrics`, ordinal `EvidenceState`, count/span, sin-score en Emotion). Normal-normal es candidato CONDICIONADO por familia (§8), no universal. |
| C2 | "sufficiency ⊥ confidence por construcción" | `CONTRADICTED` (framing) | Son objetos separados sin aliasing, pero la PRECISIÓN puede depender legítimamente de cantidad/calidad de datos (§9 corregido). No es una ortogonalidad estructural absoluta. |
| C3 | "shrinkage implementa la regla anti-regresión-a-la-media" | `CONTRADICTED` | Shrinkage reduce sobrerreacción a extremos ruidosos y puede introducir sesgo; NO elimina regresión a la media ni sustituye un baseline multisesión (§4.3-corr). |
| C4 | "staleness = ensanchamiento del intervalo (verdad automática)" | `INFERRED→corr.` | El ensanchamiento es un CANDIDATO de modelo (hipótesis de process noise) que requiere validación; la decisión aprobada es STALE + reevaluación, no bajada (SCO-010). |
| C5 | "Growth = RCI+SEM (solución única)" | `CONTRADICTED` | ADR-SCI-004 depende de 003 + fiabilidad + baseline + comparabilidad + exposición + práctica + dificultad + dispositivo + formas; sin SEM/práctica defendible ⇒ `INSUFFICIENT_DATA` (estado canónico 03 §10.3). |
| C6 | "todo O(1) local-first" | `CONTRADICTED` | Incremental O(k series); snapshot read O(dimensions); cold rebuild/migración O(n runs) (§9-corr). |
| C7 | "recompute-desde-runs es la migración v1→v2" | `CONTRADICTED` | 8/9 módulos no emiten runs (§3). La matriz de migración cae a legacy-read-only + candidato CALIBRATING para esos 8 (§12). |
| C8 | "IDN-003 hace el sync portable (Firebase→VPS decidido)" | `CONTRADICTED` | IDN-003 gobierna AUTH, no el backend del perfil. El sync del perfil depende de ADR-DATA-001/002 (§10). |
| C9 | "dosis interim = 1 evidencia oficial/ejercicio/día" | `CONTRADICTED` | Contradice SES-009 (dosis por familia, no universal) y es farmeable rotando 121 ejercicios (§13). |
| C10 | "flag `mindboostGlobalScoreEnabled` + invariante en summarize ya entregados por 6-R3" | `VERIFIED — ya cableado` | Corregido: `summarize` SÍ consulta el flag (provisional) y la const `ADR_SCI_006_APPROVED` (oficial), `MindBoostProfileAggregator.kt:122-173` (worktree, `M`; ausente en HEAD). El cierre previo lo negaba (`REPORTED`) por leer un snapshot anterior al aterrizaje de C2.5. |
| C11 | "namespace `mindboost_profile` ya en accountDeletion.ts" | `CONTRADICTED` (corregido en cierre previo) | Solo eraser LOCAL; sin espejo cloud. Mantener corregido. |
| C12 | "evidence↛profile boundary intacto + solo E8 emite runs" | `VERIFIED` | `evidenceBoundaryScanner` presente; único productor de run = `GoNoGoCognitiveSessionAdapter`. |
| C13 | "D-1…D-8 marcadas como preguntas al CEO" | `CONTRADICTED` (proceso) | Deben resolverse (02 §10/§14: solo se eleva gasto/acción externa). Resueltas en §27. |

---

## 5. Architecture principles (heredados del canon, aplicados)

Offline-first (05 §5.1) · common-first (lógica/estimación pura en `commonMain`, 05 §5.2/TEC-007) · domain isolation
(UI/datos/plataforma no duplican verdad cognitiva) · **append/recompute** (runs/evidencia inmutables; proyección
recomputable, 05 §5.4 / 04 §14.3) · idempotency · **version everything** (ejercicio/fórmula/schema/prior/norma,
TEC-010) · **fail closed** (integridad/comparabilidad falla ⇒ entrena, no mide oficialmente, 03 §14.3) · privacy by
design (minimizar, local, PRI-008) · no hidden AI en Free (BUS-002) · cross-platform parity (iOS no es "verificado"
sin toolchain) · reversible migration (TEC-011: dual/shadow o puente) · **reloj inyectado** (todo tipo/estimador/
gateway de measurement recibe el tiempo por parámetro `nowMs`/`EpochClock`, NUNCA `Clock.System.now()` directo —
learning 2026-05-29, rompe `desktopTest` con kotlinx-datetime 0.7.1; "desconocido" = `null`, jamás `0L`). **Barrera
de honestidad:** `null≠0` (SCO-004), latest-raw prohibido (SCO-005), XP/racha/suscripción/contexto nunca tocan
score (SCO-003, CTX-003).

---

## 6. Measurement pipeline (frontera científica explícita)

```text
Facts-only evidence (6-R3: por-run facts, sin score en el DTO; scores desde el RUN sellado)
  → ObservationModel POR FAMILIA            (§7 — modelo de observación defendible por tipo de respuesta)
    → ComparableSeries                       (una ScoreScaleKey; INCOMPATIBLE entre keys; no mean cross-serie)
      → LongitudinalStateEstimator CANDIDATO (§8 — estimación + SEM en ESCALA PRIMARIA; ADR-SCI-003, ABIERTO)
        ├─► SubskillProjection → BlockProjection (ADR-SCI-001, ABIERTO — NO precede a su definición)
        │     → PerformanceScore              (presentación 0–100 versionada; ≠ dato primario, 03 §9.1)
        └─► ReliableChange / Growth           (§10 — consume el ESTIMADOR+SEM+baseline, NO el 0–100 con
                                               pérdida; paralelo a PerformanceScore, no aguas abajo; ADR-SCI-004)
```

- **Fuente de valores:** el gateway toma los HECHOS/disposiciones del extractor facts-only y los VALORES del
  `run.score` sellado (`ScoreState.VALID` re-verificado). El DTO facts-only no lleva score por diseño (S6R3-001).
- **Regla de nivel (03 §4.3):** la inferencia asciende explícitamente; no se salta un nivel sin justificar la
  agregación. **`ADR-SCI-003` no puede agregar dimensiones que `ADR-SCI-001` aún no ha definido** (dependencia
  dura, §20).

---

## 7. Observation-model boundary (POR FAMILIA — corrige el normal-normal universal)

Cada familia de ejercicio declara su `ObservationModel` versionado (Exercise Dossier §5
`measurement.{primaryMetrics,timingContract,speedAccuracyPolicy,uncertaintyMethod,scoringVersion}`), no una
fórmula global (03 §8.3 prohíbe "una fórmula universal improvisada para los 120 ejercicios"; 04 §10.2 prohíbe
`score = accuracy×speed` sin rationale+simulación). Modelos de observación admisibles por tipo de respuesta:

| Tipo de respuesta (familia) | Modelo de observación candidato | Ejemplos en repo |
|---|---|---|
| Discreta correcto/incorrecto (crédito 0/1) | **Binomial / beta-binomial**; proporción con error binomial | Memory discriminación (k/n), Pairs (hit/false-alarm, SDT) |
| Compuesto de crédito parcial CONTINUO (subhabilidades ponderadas) | **Beta / normal-approx de la proporción acotada [0,1]**, o **GPCM/graded-response** sobre los componentes ordinales — **NUNCA** beta-binomial (no es conteo Bernoulli; su suficiente `k/n` no existe en el store) | Logic ICR (`ReasoningScorer.totalIcr`: media ponderada final-answer/regla/premisa/justificación/confianza/independencia) |
| RT en aciertos válidos | **Modelo de RT / transformación** (log/recíproco), resumen robusto (mediana). **Techo honesto HOY = mediana** (el run persiste mediana/media/min/max/CV, NO la distribución); distribucional (drift-diffusion) exige instrumentar cuantiles (siguen siendo resúmenes) | Speed `SessionMetrics`, Focus RT |
| Ordinal / estado de evidencia | **Modelo ordinal** (no promediar ranks como intervalo) | Language `EvidenceState` |
| Conteo / capacidad | **Poisson/conteo o span** con su error | Memory span, Pairs (conteo) |
| Sin score defendible | **Ninguno** → sin observación oficial (CALIBRATING nativo) | Emotion (`normalizedScore` SIEMPRE null; `ProfileTier` NO emite score) |
| Ítem calibrado (post-datos) | **IRT/Rasch/GPCM/2PL** tras instrumentación de nivel-ítem (§7-data) — **BLOQUEADO, no solo diferido** | ninguno hoy — ver §7-data |

**`NormalNormalEstimator`** permanece SOLO como candidato **shadow** para familias donde se cumplan las CINCO
condiciones: (1) escala/transformación defendible; (2) error de observación defendible; (3) residuos suficientemente
compatibles con la aproximación gaussiana; (4) dificultad, dispositivo y formas controlados; (5) los intervalos
superan simulación y validación (§15, §22). Ninguna de las cinco se asume; se demuestran por ADR-SCI-003. Prohibido
elegir el modelo por elegancia o por coste O(1).

---

## 7-data. Data & item readiness (dos pistas SEPARADAS: persona vs ítem)

**Corrección P0 (readiness psicométrico).** El cierre previo afirmaba "IRT diferible SIN perder instrumentación".
Es **FALSO** contra el repo, y hay que separar dos pistas que el plan anterior fundía:

- **Pista de PERSONA (F4, funciona hoy sobre datos de sesión):** el `LongitudinalStateEstimator` estima el estado
  latente del SUJETO desde la serie de resúmenes de sesión (`CognitiveSessionRun` = accuracy/RT-mediana/CV/conteos
  agregados). No necesita matriz persona×ítem. Esta pista NO está bloqueada por readiness de ítem.
- **Pista de ÍTEM (IRT/Rasch/GPCM/2PL + análisis de ítems 08 §11.2/§11.3: dificultad, discriminación/información,
  distractores, dependencia local, techo/suelo):** **BLOQUEADA, no diferida.** Requiere una **matriz de respuestas
  por ítem** (persona×ítem×forma×dificultad-prevista×exposición×orden×RT×crédito) que **NO EXISTE**: `CognitiveSessionRun`
  es un RESUMEN de sesión y su KDoc lo sella — *"It NEVER carries raw trials or per-item content"* (`CognitiveSessionRun.kt:94`).
  Y §16 lista "respuestas por ítem" entre lo que **NUNCA sale del dispositivo**; ninguna de las opciones de sync
  (§10-sync A/B/C) transporta respuestas por ítem. ⇒ sin un carril de datos nuevo, IRT y 08 §11.2/§11.3 son
  **inejecutables**, no "solo les falta volumen".

**Instrumentación de nivel-ítem + research-data lane (owner requerido, hoy inexistente).** El registro por ítem es
un **artefacto SEPARADO** del `CognitiveSessionRun` (que sigue siendo resumen): captura los eventos de la taxonomía
04 §18 (`TrialPresented/ResponseStarted/ResponseSubmitted/TrialResolved`, con id de ítem, tiempo monotónico,
crédito) + los campos Dossier §5 (`itemBankVersion/generatorVersion`, forma/familia/ítem, dificultad
prevista/observada, exposición, blueprint, orden). Debe enrutarse por la **capa 4 de investigación consentida**
(08 §19.1): dataset opt-in, pseudonimizado, minimizado, con su propio paso en `accountDeletion.ts` — NO por el
carril de perfil/sync. **Sin ese carril, la pista de ítem queda BLOQUEADA** (por eso el check §30 se desmarca).
Esta instrumentación no está asignada a ninguna F0-F10 hoy → es **precondición pre-beta con owner** (DAG §20), o se
declara BLOQUEADA por la Fase B del scoring (§18); no puede figurar como "logro ya conseguido".

**Gate mecánico de completitud (falta hoy).** Espejo de `evidenceBoundaryScanner`/`BankValidationGateTest`: un
`RunInstrumentationCompletenessTest` cableado a `check` que RECHACE un run oficial con campos Dossier §5 vacíos
(04 §5: "los valores vacíos bloquean el gate") como precondición de que una dimensión salga de CALIBRATING. Sin
él, "instrumentación no diferible" no tiene dientes.

**Riesgo declarado (R-register §25, fila añadida):** una beta natural/autoseleccionada produce exposición sesgada,
restricción de rango (sesga ICC/test-retest, 08 §11.3) y potencia DIF insuficiente (08 §11.5) → el gate exige
potencia/precisión preregistradas antes de recoger datos confirmatorios.

---

## 8. Longitudinal estimator candidates (todos CANDIDATOS — ADR-SCI-003)

Ninguno se cierra en este documento. ADR-SCI-003 elige por familia con simulación (§15) y shadow (§11):

1. **Estado gaussiano conjugado** (normal-normal estático) — simple, cerrado; solo bajo las 5 condiciones (§7).
2. **Modelo dinámico / state-space** (DLM con process noise) — permite modelar deriva temporal; el process noise es
   una **hipótesis a validar**, no una verdad (§4.4-corr).
3. **Variante robusta** (likelihood heavy-tailed, p. ej. t de Student) — resistente a outliers extremos (03 §12.4).
4. **Binomial/beta-binomial** para respuestas discretas — coherente con el ObservationModel discreto (§7); su
   variante DINÁMICA (GLM dinámico / binomial state-space / particle filter) es el candidato de deriva temporal
   para familias discretas/ordinales, para NO forzar el DLM gaussiano (#2) como única opción con dinámica.
5. **Modelos de RT / transformaciones** para familias temporales.
6. **IRT/Rasch/GPCM/2PL** como opción tras readiness de datos (§7-data) — post-beta, detrás de gate de datos.

**Cold start / prior / actualización / cambio de prior / rollback (obligatorio, §4.3-corr):** prior débilmente
informativo VERSIONADO por familia (`priorVersion`); actualización bayesiana/robusta según modelo; un **cambio de
prior NO puede producir Growth artificial** (se congela la serie anterior o se abre nueva, 03 §11.5/§21.2); todo
cambio de modelo/prior es `major` con dual/shadow (TEC-011) y rollback declarado. Un baseline sigue exigiendo
**varias observaciones** (03 §9.5): el shrinkage no lo sustituye.

---

## 9. Sufficiency versus uncertainty (dos objetos separados — corrige §4.2)

Separación semántica y de tipos, sin aliasing y sin conversión directa. La precisión (uncertainty) PUEDE depender
de la cantidad/calidad de datos (sufficiency), pero **no es el mismo objeto ni `1 − sufficiency`** (03 §12.2:
"confianza no es consistencia"; checklist 07 §9 "¿data sufficiency se confunde con confidence?").

```text
EvidenceSufficiency                         ModelUncertainty
- observationCount                          - observation error (SEM del scorer)
- independentDayCount                       - posterior / estimation error
- temporalSpan                              - model uncertainty
- blueprintCoverage                         - version comparability
- task/form diversity                       - technical quality
- exposure status                           - interval calibration status
- integrity
- device comparability
- validation status
```

Prohibido llamar "confidence" a consistencia, dispersión o conteo. `EvidenceSufficiency` gobierna elegibilidad y
cobertura; `ModelUncertainty` gobierna el intervalo y la etiqueta de confianza mostrada (03 §12.1).

**Procedencia del SEM (obligatoria; hoy sin fuente defendible).** El `observation error` de `ModelUncertainty`
debe derivarse de una fuente psicométrica declarada — error binomial n-based, split-half, o formas paralelas — y
está **PROHIBIDO derivarlo de la varianza inter-sesión** (eso es exactamente el `confianza≠consistencia` que §9
prohíbe, 03 §12.2 / 07 §9). Los scorers vivos (ICR/Focus/Go-No-Go) emiten un punto SIN SEM por sesión, e IRT
(SEM condicional) está bloqueado (§7-data) ⇒ hasta que exista una fuente de SEM defendible, el intervalo NO es
computable y la dimensión no puede mostrar banda (§14). El **programa de fiabilidad empírica** que produce estos
números (split-half / test-retest / ICC / SEM / MDC, **estratificado por ejercicio/subhabilidad/bloque y por
USO** — feedback vs perfil vs rank; 08 §11.3, 03 §12.3) es actividad de **beta** y **precondición de F6**
(Growth/RCI necesitan SEM y MDC); no lo produce ninguna fase de código y se declara como tal en el DAG §20.

### 9-SoT. Source of truth y snapshots (corrige "todo O(1)")

```text
Complejidad real:
  Incremental update: O(k)          (k = series afectadas por la nueva evidencia)  ← ruta de RUNS (solo E8 hoy)
  Snapshot read:      O(dimensions)  (≤ 8)
  Cold rebuild/migración: O(n)       (n = runs/observaciones elegibles del sujeto)
  Ruta legacy read-only (8/9 dim HOY = DOMINANTE): hereda el coste de los adapters existentes sin cambios
                                     (leen stores por-módulo, no una serie de runs); no re-auditada aquí

Arquitectura de autoridad:
  Immutable subject-scoped runs/evidence (+ stores por-módulo)  = FUENTE PRIMARIA
  Versioned snapshot                                            = CACHÉ DERIVADA DESCARTABLE
  UI projection                                                 = DERIVADO
```

**Escala (números de orden de magnitud):** el cómputo es on-device y por-sujeto ⇒ **constante por usuario**,
independiente de la base total (el backend hace CERO scoring). A 1M MAU / ~300k DAX: sync de snapshot ~300k
writes/día ≈ 3.5/s medio (~30–60/s pico vespertino); snapshot ~2–6 KB/usuario. Puntos de presión reales: picos de
fan-in vespertino (absorbidos por cola local con backoff+jitter — el sync jamás en ruta crítica, TEC-004);
migraciones de schema en 1M dispositivos (migración perezosa en lectura + recompute local, nunca migración
server-side de datos cognitivos); merge multi-device (esquivado: el snapshot es proyección recomputable, conflicto
→ `CONFLICT_REVIEW`).

**Campos mínimos del snapshot v2:** `schemaVersion`, `algorithmVersion`, `observationModelVersion`, `priorVersion`,
`constructVersion`, `blueprintVersion`, `scaleKey/lineage`, `policyVersionBundle`, `dataCutoff/asOf`,
`sourceDigest/lineage`, `status`, `migrationProvenance`. Estos son metadatos de **envelope/versión**; el payload
por-dimensión (`EvidenceSufficiency`/`ModelUncertainty` como objetos tipados separados, §9) es **recomputado en
lectura desde la fuente primaria**, no una segunda copia autoritativa persistida (el snapshot es caché descartable;
F8 fija el shape exacto del DTO v2). `sourceDigest` reutiliza el fingerprint canónico ya existente
(`core/sync/SessionFingerprint.kt`, FNV-1a sobre campos allowlisted, "NOT a cryptographic/anti-tamper guarantee")
— NO inventar un segundo mecanismo. Valores admisibles de `status`/`migrationProvenance` = los resultados de la
matriz §12 (`Rebuild`/`LEGACY`/`NON_COMPARABLE_VERSION`/`UNASSESSED`/fail-closed), enum fijado en F8.
**Snapshot corrupto/incompleto/incompatible:** no se usa; se reconstruye si existen runs/stores completos; si no,
se marca `LEGACY`/`NON_COMPARABLE`; **nunca** se transforma en silencio (primitiva ya existente:
`SecureKeyValueStore.readParsed`/`readVersioned` → `quarantine`, `:124-154`).

---

## 10. Growth / reliable change (corrige RCI+SEM único) y §10-sync

**Growth (§4.5-corr):** objeto SEPARADO del Performance Score (SCO-001, 03 §10.2), que consume el
**estimador+SEM+baseline** (§6, NO el 0–100 con pérdida). ADR-SCI-004 depende EXPLÍCITAMENTE de: ADR-SCI-003 ·
error/fiabilidad estimados (programa de fiabilidad de **beta**, §9) · baseline estable · comparabilidad ·
exposición · práctica · dificultad · dispositivo · formas paralelas. Estados 03 §10.3
(`INSUFFICIENT_DATA/NO_RELIABLE_CHANGE/LIKELY_/RELIABLE_/STALE/NON_COMPARABLE_VERSION`). **Sin estimación
defendible de práctica o SEM ⇒ `INSUFFICIENT_DATA`** (estado CANÓNICO del enum; el cierre previo usaba
"INCONCLUSIVE", que NO existe en 03 §10.3). Si Growth se reporta por bloque/dimensión (radar por-dimensión), el
cambio fiable a ese nivel requiere la agregación de ADR-SCI-001 ⇒ arista **004→001** en el DAG (§20). Método de
cambio fiable **por tipo de observación**: RCI+SEM asume gaussiano; familias ordinal (`EvidenceState`)/conteo
declaran su propio método de reliable-change, no un RCI gaussiano por defecto.

### 10-sync. Sync options (contrato agnóstico; ejecución BLOQUEADA hasta ADR-DATA)

IDN-003 gobierna solo AUTH. Separar capas: Identity/Auth · Evidence persistence · Projection sync · Validation ·
Export/Delete · Analytics. **Hasta ADR-DATA-001/002:** no activar nuevo sync Profile V2; no crear schema cloud
definitivo; no presentar resúmenes sincronizados como autoridad. Comparación técnica (a decidir EN el ADR, no
devolver al CEO salvo gasto/acción externa, 02 §14):

| Opción | Recompute | Auditabilidad | Privacidad | Coste | Cross-device | Borrado | Recuperación | Integridad | Migración | Lock-in |
|---|---|---|---|---|---|---|---|---|---|---|
| **A** sync de runs/evidence minimizados | Reconstruible en cualquier device | Alta (append-only) | Mayor superficie (más dato) | Mayor storage/writes | Fuerte | Cascada por-sujeto | Alta | Alta | Recompute | Bajo |
| **B** sync solo de snapshots | No reconstruible desde cloud | Media (snapshot opaco) | **Menor** (solo resúmenes allowlisted) | Bajo | Débil (snapshot no autoritativo) | Doc único | Media | Media | Recompute local | Bajo |
| **C** híbrido (snapshot + runs bajo consentimiento) | Reconstruible con consentimiento | Alta | Consentimiento por finalidad (PRI-001/002) | Medio | Fuerte | Cascada | Alta | Alta | Recompute | Bajo |

**Reconciliación con el diseño hermano (obligatoria, no ignorar).** Ya existe `docs/architecture/MINDBOOST_FIRESTORE_SYNC_DESIGN.md`
(2026-07-04, "Nota de diseño — decisión CEO") que especifica el MISMO árbol para la MISMA entidad:
`users/{uid}/cognitiveProfile/main` (espejo del `MindBoostProfileDto`), `blockScores/{blockId}`, `sessions/{sessionId}`,
reglas Firestore (owner + App Check + list-limit + rechazo de `trials`), integración con `accountDeletion.ts`, e
infraestructura ya construida (`core/sync/{SyncQueue,SessionFingerprint,...}`, flag EXISTENTE
`mindboost_sync_firestore_enabled`). Profile V2 **adopta ese diseño como Impl-1** (o lo supersede con justificación
explícita en ADR-DATA), NO lo reinventa. Los flags `PROFILE_V2_SHADOW_*` se mapean contra
`mindboost_sync_firestore_enabled` (una autoridad de sync, no dos paralelas).

**Corrección del "no-LWW".** 05 §6.2 prohíbe last-write-wins **sobre el historial de sesiones** (evidencia
inmutable) — NO sobre una caché derivada recomputable. El diseño hermano decide, con rationale, **LWW por
`updatedAtMs` para `cognitiveProfile/main` y `blockScores`** (snapshots derivados atómicos; merge por-campo
rompería el `compositeIndex`), y **union append-only** para `sessions` (inmutables, id único). Profile V2 hereda
esa distinción: **append/no-LWW para runs/evidence; LWW-sobre-caché-derivada admisible para el snapshot** (a fijar
en ADR-DATA, no un dogma global). Riesgo LWW: dos dispositivos que recomputan a la vez → re-agregar desde
`sessions` unidas; estrategia de rehidratación tras reinstalación = pendiente de ADR-DATA.

Contrato agnóstico (commonMain, puro): `ProfileSnapshotSyncStore { push/pull/deleteAll }` con envelope allowlisted
(test de forma reflection) + máquina de sync 05 §11.3 (`LOCAL_PENDING→UPLOADING→ACKNOWLEDGED→VERIFIED→PROJECTED`;
fallos `RETRYABLE/PERMANENT_REJECTED/CONFLICT_REVIEW`) + `deleteAll` fail-closed con proof. Impl-1 Firebase
(owner-only, App Check, `FirestoreDefaults`, emulador cruzado, `.limit()` espejo si hay subcolección).

**Borrado ↔ colección (gate mecánico, no solo política).** Precedente VIVO: el pilotaje de Focus escribe
`users/{uid}/sessions` que HOY **no tiene regla Firestore dedicada** (cae en el catch-all deny → no explotable
ahora) **ni paso de borrado** en `accountDeletion.ts` — el compromiso "misma-change" ya lapsó una vez. Antes de
activar cualquier sync Impl-1 (Profile V2 o reactivar Focus): (1) cerrar ese gap (regla + paso de borrado,
espejo de `user_posts`), (2) añadir un check mecánico `functions/scripts/` (análogo a `check-storage-rules.js`)
que enumere cada `.collection()/.doc()` que el cliente escribe y falle si no tiene contraparte en
`accountDeletion.ts` — el espejo cloud de `AccountBoundErasureCoverageTest`. Toda colección/tabla nueva entra en
`accountDeletion.ts` EN EL MISMO CAMBIO (política) **respaldada por ese gate** (mecánica).

**Confidencialidad frente al operador (resolver T3/T4, no dejar contradicción).** El diseño hermano sube las
métricas derivadas (`normalizedScore`, bandas, dimensiones) **en claro** — el operador de Firestore/VPS **SÍ
puede leerlas** (no hay cifrado end-to-end). ⇒ ADR-DATA debe declarar EXPLÍCITAMENTE: o (a) alguna opción
garantiza E2E (el operador nunca deriva el valor cognitivo, ni con admin/legal/breach), o (b) "opaco" solo
significa "sin trials crudos ni claves" y la legibilidad del RESUMEN por el operador es **riesgo ACEPTADO y
documentado**, no mitigado — en cuyo caso §16 T4 retira la palabra "opaco". **Opción A** ("reconstruible en
cualquier device") solo es viable si su allowlist EXACTO de campos (test de forma reflection) NO contiene "listas
de scores por-sesión"/RT por-sesión (§16 "qué nunca sale"); si no puede demostrarse, se marca **no viable**, no
candidata neutral. Impl-2 VPS = ADR-ID-001/ADR-DATA-001 (región UE; el servidor jamás ve trials crudos ni claves).

---

## 11. Flags y shadow scoring (multi-flag; corrige "una sola flag")

Flags conceptuales mínimos (defaults de release entre paréntesis), en las 3 impls de `FeatureFlags` (learning
2026-06-22):

- `PROFILE_V2_SHADOW_COMPUTE_ENABLED` (dev/alpha ON para recoger divergencia; release OFF)
- `PROFILE_V2_SHADOW_PERSIST_ENABLED` (persiste candidato bajo clave `shadow_v2`, nunca leído por UI) (OFF) —
  el blob `shadow_v2` HEREDA el tratamiento de versión/staleness del snapshot v2 (§9-SoT): al reencender tras un
  OFF→ON debe REVALIDAR `algorithmVersion`/`priorVersion` o descartarse, nunca leerse obsoleto.
- `PROFILE_V2_CANDIDATE_READ_ENABLED` (**OFF**)
- `PROFILE_V2_DISPLAY_ENABLED` (**OFF**)
- `PROFILE_V2_BLOCK_<ID>_ENABLED` (por bloque; **OFF**)
- `PROFILE_V2_GLOBAL_ENABLED` — **NO es un símbolo nuevo: es el `mindboostGlobalScoreEnabled` EXISTENTE reutilizado**
  (una sola autoridad de flag; el nombre `PROFILE_V2_GLOBAL_ENABLED` es solo el rol conceptual). Gatea el composite
  PROVISIONAL (SCO-014). El composite OFICIAL es ortogonal e independiente del flag: exige la **constante de código**
  `ADR_SCI_006_APPROVED=false` (`MindBoostProfileAggregator.kt:56`, jamás un flag remoto) + las 9 dimensiones
  ESTABLISHED. Relación: `provisional ⇐ flag`, `oficial ⇐ const ∧ 9-ESTABLISHED` (no AND entre sí).

**Ninguna fórmula candidata modifica el perfil visible** hasta el gate. **Shadow scoring** compara legacy vs
candidato SIN mostrar candidato (08 §12.2) y analiza: saltos, sign flips, false improvement/decline, rank flips
(aunque ranking siga OFF), subgrupos, dispositivo, dificultad, exposición, casos extremos, missingness, cobertura,
regresión a la media. La separación compute/persist/read/display permite rollback seguro por capa.

**Fuente de datos del análisis de divergencia (reconciliar con T7).** El análisis **por-cohorte** (subgrupos /
dispositivo cruzando usuarios) NO es computable puramente on-device y chocaría con T7 ("ninguna fase añade payload
cognitivo a logs/analytics"). Por eso: **F4 opera SOLO sobre la matriz de simulación SINTÉTICA (§15)** — divergencia
per-device/subgrupo sintética, cero dato real, sin problema de privacidad. El shadow-scoring **poblacional real**
(cohorte alpha/beta) es entregable EXCLUSIVO de F7 y requiere su propio **consentimiento "Investigación"**
(08 §19.1 capa 4 / PRI-001/002) diseñado ANTES de F7 — no un efecto colateral no declarado del "reporte de
divergencia".

---

## 12. Migration matrix (conservadora; corrige "recompute-desde-runs auto-aprobado")

No se aprueba automáticamente. Antes de cualquier rebuild candidato hay que DEMOSTRAR: completitud, subject-scoping,
durabilidad, versiones, ausencia de gaps y reproducibilidad de cada adapter legacy. Matriz:

| Situación | Resultado |
|---|---|
| Runs completos y compatibles | Rebuild del candidato desde runs |
| Runs incompletos pero snapshot legacy legible | Legacy read-only + candidato CALIBRATING |
| Datos incompatibles | `NON_COMPARABLE_VERSION` / nuevo baseline |
| Snapshot corrupto | Rebuild (si hay runs completos) o fail-closed |
| Sin datos | `UNASSESSED` |

**Aplicación al repo REAL (hallazgo §3):** solo E8/Speed emite runs; los otros 8 módulos NO ⇒ la rama "runs
completos" es **inalcanzable hoy para 8/9 dimensiones**; su rama por defecto es **legacy read-only + candidato
CALIBRATING** (leen sus stores por-módulo, que son resúmenes durables, no una serie de runs con dossier). Que una
dimensión pase a candidato medible requiere que su módulo EMITA runs con los campos del Dossier §5 (trabajo
por-bloque, Fase B del scoring, fuera de este plan) — hasta entonces CALIBRATING honesto (idéntico a hoy, sin
regresión de UX).

**Eliminación de los 9 adapters (F10) SOLO cuando:** existe replacement mapping 1-a-1 · tests de paridad verdes ·
cero consumidores legacy · rollback window terminada · migración cerrada · export/delete cubren ambos schemas ·
**`grep` + dependency scanner en cero** (criterio MECÁNICO y verificable — NO "telemetría de uso", que hoy no
está instrumentada en `profile/`: no inventar un gate no medible).

**`InterSessionConfidence`** (Fase A, `core/scoring/normalization/`, untracked): **marcar deprecated ahora**
(prohibir nuevos consumidores); eliminar SOLO tras cero refs y compatibilidad demostrada. **No se pregunta al CEO.**

---

## 13. TRAINING_FREE y dosis

**TRAINING_FREE (regla ya decidida, 03 §7.1 / SES-008 / C2.1):** el entrenamiento no acelera evidencia oficial,
calibración ni rango. Su corrección en el enum vivo `SessionPurpose.mayContributeToCalibration()/mayContributeToRanking()`
(que hoy devuelven `true` para TRAINING_FREE, blast radius E8) es **PRECONDICIÓN antes de cualquier wiring de
Profile V2**, con blast-radius audit + tests (F3). El gateway v2 es inmune por diseño (re-deriva propósito, jamás
lee `mayContribute*`), pero la divergencia del enum vivo se corrige igualmente.

**Dosis:** se **RECHAZA** la propuesta universal "1 evidencia oficial por ejercicio y día" (contradice SES-009 —
dosis por familia vía ADR; es universal; farmeable rotando 121 ejercicios ⇒ dosis agregada absurda). Hasta
ADR-SCI-005: recoger las observaciones necesarias; clasificar el exceso como `TRAINING_ONLY`; usar una política
shadow **predeclarada por familia/blueprint**; no denominar "oficial" a ninguna contribución nueva.

---

## 14. UI states / claims (progressive disclosure — decisión existente, sin preguntar al CEO)

Progressive disclosure (PRD §11.8). **Nivel 1 no fusiona dos ejes ortogonales:** próxima acción **PRIMERO**
(PRD §11.8), estado de **CALIBRACIÓN** (`UNASSESSED/QUICK_PROVISIONAL/CALIBRATING/CALIBRATED/STALE/RECALIBRATING/
NON_COMPARABLE`, 03 §11.4 = "cuánto conocemos tu nivel") y, POR SEPARADO, estado de **GROWTH**
(`INSUFFICIENT_DATA/NO_RELIABLE_CHANGE/…`, 03 §10.3 = "ha cambiado tu nivel"). El Performance Score y el Growth
Index del PRD §11.8 **no se dejan caer en silencio**: degradan al chip de calibración/"—" y a `INSUFFICIENT_DATA`
respectivamente. (`NO_RELIABLE_CHANGE` es estado de Growth §10.3, NO de calibración §11.4 — no mezclar los enums.)
- **Nivel 2:** cobertura; calidad; tendencia; recomendación.
- **Nivel 3:** intervalo/rango plausible; versión; comparabilidad; metodología.

**La regla de honestidad aplica al PUNTO, no solo a la banda.** Hoy `UserDisplayScoreMapper.toPercent` emite un
ENTERO 0-100 desnudo (sin intervalo) para ESTABLISHED y EMERGING≥0.55 — un punto sin intervalo es falsa precisión
MÁXIMA (peor que "punto+banda honesta"). Hasta que exista calibración de intervalo defendible (`interval
calibration status`, §9 + fuente de SEM): se **prohíbe el 0-100 desnudo en TODA dimensión** (solo posición
categórica, o punto SIEMPRE encabezado por su banda explícita) — esto cierra el over-claim vivo del "63" de
Focus/Logic (§3). `QUICK_PROVISIONAL` (03 §11.4, estado ANTERIOR a CALIBRATING = **no calibrado**) surfacea una
**posición categórica** ("Lectura inicial registrada"), **NUNCA un 0-100 ni una banda numérica** (corrige R-10).

Durante la transición: no mostrar candidato; no fabricar 0; no sustituir `current` por `best` (03 §10.4); no
esconder descensos; **"—"** cuando no hay estimación defendible (invariante `BrainProfileUiModelBuilder`
`DisplayScore.Withheld`, VERIFIED). El rename `DisplayScore.Official`→provisional **YA ESTÁ HECHO**
(`ProvisionalStable/ProvisionalEmerging/Withheld`, C2.4-5 de 6-R3, VERIFIED `UserDisplayScoreMapper.kt:44-53`) —
no es acción pendiente. Los estados nuevos de F5 (STALE/RECALIBRATING/NON_COMPARABLE/QUICK_PROVISIONAL/
NO_RELIABLE_CHANGE) llevan **copy no técnico como criterio de aceptación de F8** (p. ej. NON_COMPARABLE→"No
comparable (versión nueva)"; NO_RELIABLE_CHANGE→"Sin cambios claros todavía", NUNCA "sin progreso"), corrido por
la variante del `mindboostCopySafetyScanner` del source-set.

---

## 15. Simulation matrix (obligatoria en el plan)

Casos (cada uno con criterio de aceptación): n=0 · n=1 · n pequeño · evidencia abundante · sesiones idénticas ·
datos en cualquier orden · duplicados exactos · conflictos parciales · outlier extremo · baseline muy bajo ·
baseline muy alto · mejora por práctica sin habilidad · cambio real sin práctica · variabilidad alta · ceiling/floor
· cambio de dispositivo · cambio de forma · cambio de dificultad · cambio de versión · inactividad larga · reloj
futuro · timezone/DST · sync reordenado · pérdida de snapshot · snapshot corrupto · cuenta A→sign out→cuenta B ·
121 ejercicios rotados para farmear dosis · serie fragmentada · prior cambiado · modelo cambiado · **ADVERSARIOS
(03 §27 / 07 §Scoring / 08 §12.3, obligatorios — son el exit-gate de F4):** azar-rápido/adivinación impulsiva ·
lento-perfecto · omitir-para-evitar-penalización · memorización/repetición de ítems · score cerca de umbral.

**Invariantes (deben sostenerse en TODOS los casos):** missing nunca es 0 · duplicado no cambia el estado · orden
no altera la semántica del resultado · inactividad no baja arbitrariamente la media · entrenamiento no crea progreso
oficial · escala incompatible no se mezcla · versión desconocida falla cerrada · candidato nunca llega a UI antes
del gate · **gating de impulsividad: RT sub-umbral plausible NO cuenta como acierto ni sube la estimación**
(doctrina #5) · **memorización/repetición de ítems NO sube la capacidad estimada** (doctrina #2) · **Global OFICIAL
inalcanzable por la constante `ADR_SCI_006_APPROVED=false`** (robusto: ni F9, ni registrar un banco, ni Remote
Config lo abren) · **Global PROVISIONAL (SCO-014) gated por flag remoto + `established.size≥3`** — HOY inalcanzable
solo porque `established.size≤2` (barrera INCIDENTAL que F9 erosiona), por lo que cruzar una 3ª dimensión a
ESTABLISHED **no** debe surfacear un Global provisional sin decisión explícita (ver F9). No confundir ambos en un
único invariante "inalcanzable".

**Métricas de simulación:** bias · RMSE · interval coverage · false-positive improvement · false-negative change ·
sensitivity to practice · robustness to outliers · calibration by ability level · subgroup/device divergence ·
numerical determinism.

---

## 16. Security / privacy (threat model del perfil cognitivo)

Clasificación (05 §13.1): perfil cognitivo = sensible por impacto; pseudonimizado sigue siendo personal (PRI-003);
privado por defecto (SCO-021).

| # | Amenaza | Mitigación |
|---|---|---|
| T1 | Robo del dispositivo | AES-256-GCM + Keystore; backup rules excluyen prefs; v2 no añade stores planos |
| T2 | Cuenta cruzada (A→sign out→B) | **El aislamiento subject-scoped 6-R3 cubre `Sessions`/`SessionRuns`, NO el snapshot `mindboost_profile`**: el KDoc de `SecureNamespaceUserDataEraser` nombra `mbprofile:<uid>` como namespace LEGACY blanket-cleared ⇒ borrar A destruye el snapshot local de B (documentado, diferido 2026-06-29). `SubjectScopedSessionBatch` **NO es control de acceso** (su KDoc: aislamiento viene del repo namespace-scoped, W1). **Brecha declarada, precondición a cerrar antes de que F7/F8 escriban más claves `<uid>` ahí**: erasar vía `LocalMindBoostProfileRepository.clear(userId)` en vez de `clearNamespace()`, + test E2E "borrar A no toca snapshot de B" (HOY inexistente) |
| T3 | Breach del backend | Envelope de RESÚMENES allowlisted (test de forma); reglas default-deny + App Check. **El cifrado client-side E2E está SIN DECIDIR (ADR-DATA)** — no asumirlo como propiedad ya provista (§10-sync) |
| T4 | Operador curioso (VPS/Firestore) | **Resolver T3/T4, sin contradicción:** el diseño hermano sube el resumen cognitivo EN CLARO (el operador SÍ lo lee). ADR-DATA declara si alguna opción garantiza E2E; si no, se retira "opaco" y la legibilidad del resumen por el operador es **riesgo ACEPTADO documentado**, no mitigado. Authz por subject; logs sin payload |
| T5 | Gaming/poisoning del usuario | Gates re-derivados fail-closed (anti-gaming fact W1, banco, timebase, dosis, dedup); a lo sumo banda de cantidad, nunca capacidad; rank inalcanzable |
| T6 | Falso progreso por artefacto | Series por `ScoreScaleKey`; INCOMPATIBLE por defecto; puentes solo versionados; RT excluido sin timebase frame-locked (C4) |
| T7 | Fuga por telemetría/logs | `NonFatalTelemetryEvent` tipado; **ninguna fase añade payload cognitivo a logs/analytics**. Barrera MECÁNICA (no solo review humano): scanner ligero (patrón `mindboostCopySafetyScanner`) sobre `measurement/**`/`profile/**` que falle si un `breadcrumb`/`customKey` interpola un valor de variable con nombre cognitivo (score/estimate/theta/percentile) — hoy `Observability.breadcrumb/customKey` aceptan texto libre |
| T8 | Exports debug | Pseudonimizados sin ids estables; el envelope de sync jamás se exporta en claro |
| T9 | Borrado incompleto (GDPR Art. 17) | `mindboost_profile` en AccountBound+eraser (borrado de la PROPIA cuenta, fail-closed con proof). Precedente vivo del gap: `users/{uid}/sessions` sin regla ni paso de borrado (§10-sync H5). Toda colección nueva en `accountDeletion.ts` el MISMO cambio **+ check mecánico de paridad colección↔borrado** (§10-sync) |
| T10 | Inferencia por metadatos (counts+timestamps) | Inferencia ACEPTADA y documentada; mitigada por minimización + privado por defecto + no-export. **Técnica concreta en ADR-DATA:** redondeo de timestamps a bucket de día (nunca minuto/hora exacta); no sincronizar conteos por-sesión individual, solo agregados por ventana |

**Qué NUNCA sale del dispositivo:** trials/eventos crudos, distribuciones RT por-sesión, respuestas por ítem,
seeds/formas, contexto subjetivo (CTX-001/005), claves Keystore, listas de scores por-sesión.

---

## 17. (Estructura del documento) — cumplida por §1–§30

Secciones exigidas por la reapertura §17, mapeadas: 1 Executive verdict (§1) · 2 Scope/non-scope (§2) · 3 Baseline
(§3) · 4 Claims matrix (§4) · 5 Architecture principles (§5) · 6 Measurement pipeline (§6) · 7 Observation-model
boundary (§7) · 8 Longitudinal estimator candidates (§8) · 9 Sufficiency vs uncertainty (§9) · 10 Growth (§10) ·
11 Exercise/construct integration (§18) · 12 Data/item readiness (§7-data) · 13 Source of truth/snapshots (§9-SoT)
· 14 Sync options (§10-sync) · 15 Security/privacy (§16) · 16 Flags/shadow (§11) · 17 Migration matrix (§12) ·
18 Legacy demolition (§12) · 19 UI states/claims (§14) · 20 Dependency DAG (§20) · 21 Fases F0–F10 (§21) ·
22 Simulation matrix (§15) · 23 Test strategy (§23) · 24 Rollback (§24) · 25 Risk register (§25) · 26 ADR backlog
(§26) · 27 Decisions closed (§27) · 28 Genuine external decisions (§28) · 29 Evidence limitations (§29) ·
30 Definition of Done (§30).

---

## 18. Exercise / construct integration (los 121 ejercicios)

Cada ejercicio entra o no entra al perfil v2 según DOS campos ortogonales (04 §3.1/§3.2):
`validationStatus ∈ {IDEA…TRAINING_READY…OFFICIAL_MEASUREMENT…RETIRED}` (evidencia acumulada de la versión) y
`evidenceEligibility ∈ {TRAINING_ONLY, PROVISIONAL_PROFILE_ELIGIBLE, PROFILE_ELIGIBLE, RANK_ELIGIBLE}` (qué permite
UNA ejecución). **`RANK_ELIGIBLE` no se deriva de `PROFILE_ELIGIBLE`.** Un `TRAINING_READY` **no puede contribuir
al perfil por existir** (CAT-006).

Campos requeridos por run (Exercise Dossier §5, según aplique): `exerciseId`, `exerciseVersion`, `validationStatus`,
`evidenceEligibility`, `constructId`, subhabilidad primaria/secundarias, `scaleKey`, `scoringVersion`,
`generatorVersion|itemBankVersion`, forma/familia/ítem, dificultad prevista/observada, exposición, blueprint,
dispositivo/modalidad, calidad temporal, accesibilidad/`comparabilityImpact`, `policyVersionBundle`.

**No se agregan directamente los 121 ejercicios.** El pipeline **termina fail-closed en la serie comparable más
baja** (una `ScoreScaleKey`) hasta que exista un puente (`Bridged`, ADR-SCI-008) o un modelo de agregación
defendible (ADR-SCI-001). **Comparabilidad intra-key condicionada a EQUATING:** tratar varias seeds/formas de una
misma `ScoreScaleKey` como serie longitudinal exige evidencia de equivalencia de formas / dificultad calibrada
(04 §8.2, 03 §13.3) — sin ella, práctica o seed-más-fácil se disfraza de mejora. **Readiness IRT:** la dificultad
adaptativa (04 §9) + el aprendizaje intra-sesión violan la independencia local que IRT asume (08 §11.2) → capturar
posición/orden por ítem real es parte del carril de ítem (§7-data), no un campo del run resumen. **Precisión sobre
E8** (corrige "solo E8 emite runs con estos campos"): E8 emite UN run con identidad a nivel EJERCICIO
(`exerciseId/exerciseVersion/scoringVersion`) + conteos/RT de sesión, pero **carece** de los campos de nivel-ítem
del Dossier §5 (`itemBankVersion/generatorVersion`, forma/familia/ítem, dificultad por ítem, exposición, blueprint,
orden) y su `DifficultyTrace` va NULL. Los demás módulos quedan en su store por-módulo → CALIBRATING.

---

## 19. Gateway y elegibilidad (single source, sin segunda verdad)

Un único `ProfileEvidenceGateway` (measurement), el ÚNICO consumidor allowlisted del paquete facts-only (su allowlist
consumer arranca esencialmente vacía — hoy 1 archivo por una cross-ref KDoc incidental, como la de
`EvidenceConsumerSweepScannerTask`; alta = diff revisable + gate CI). Diseño:

- **Frontera measurement MECÁNICA (espejo EXACTO de los DOS scanners 6-R3, no solo el forward):** (1)
  `measurementBoundaryScanner` self-scan de `measurement/**` con **Tier-1 shape-ban** (prohíbe `Double/Float/
  DoubleArray/FloatArray/Map/Any/Duration/typealias` crudos en las salidas de F4/F7, para que un `Double` desnudo no
  escape el sellado del estimador/gateway) — espejo de `EvidenceBoundaryScannerTask`; **y (2) un consumer-sweep
  INVERSO** que barre TODO `commonMain` (no solo `profile.presentation/data`) por referencias a los tipos públicos de
  `measurement.**`, allowlist que arranca vacía y crece SOLO por diff revisado — espejo de
  `EvidenceConsumerSweepScannerTask`, cableado a `check`. El forward-import-ban SOLO es insuficiente (6-R3 lo demostró
  por evidencia: el leak no importaba nada prohibido); el barrido inverso + el shape-ban son la barrera real.
- **Autoridad canónica VERSIONADA** de elegibilidad (`policyVersionBundle`), no reglas duplicadas a mano.
- **Resultado de elegibilidad SELLADO** (`ObservationDecision`/`SessionEvidenceDisposition`, razones codificadas
  auditables) — nunca los 3 booleanos forjables (`profileEligible/calibrationEligible/rankingEligible`).
- **Revalidación fail-closed en el gateway** re-derivando de hechos crudos + `clockSource` (lección 6R-P1-06).
- **Rechazo de versiones desconocidas** (`scoringVersion`/`exerciseVersion` no registrada ⇒ NOT_OFFICIAL,
  fail-closed; espejo de `RtCriticalityRegistry` UNKNOWN⇒crítico).
- **Test de equivalencia** entre la fuente de política (autoridad canónica) y la validación del boundary (evitar
  drift entre dos verdades) — obligatorio.

---

## 20. Dependency DAG

```text
6-R3 CERRADO (gates verdes, P1 SessionRecordStore resuelto) ──┐
                                                              ├─► F1 (BLOQUEADA hoy)
ADR-SCI-003 (estimador) APROBADO ─────────────────────────────┘

ADR-SCI-001 (subhabilidades/agregación) ─► SubskillProjection/BlockProjection (F5) [en paralelo redactable]
ADR-SCI-003 ─► LongitudinalStateEstimator (F4) ─► ADR-SCI-004 (Growth) ─► ReliableChange (F6)
ADR-SCI-004 (Growth) ─► ADR-SCI-001   (si Growth se reporta por bloque/dimensión — cambio fiable por-bloque)
Programa de fiabilidad empírica (BETA: split-half/test-retest/ICC/SEM/MDC estratificado) ─► F6 (RCI/MDC)
ADR-SCI-002 (velocidad-precisión por familia) ─► ObservationModel RT (F2)
ADR-SCI-005 (dosis/exposición) ─► contribución oficial (bloquea "oficial", F7+)
ADR-SCI-006 (global/pesos) ─► Global OFICIAL (const, PERMANENTEMENTE OFF hoy)
ADR-SCI-008 (device/modalidad/accesibilidad — PUENTE de hardware) ─► Bridged/mezcla cross-device (F5+)
ADR-SCI-009 (invariancia de medición / DIF de EQUIDAD: educación/alfabetización/habilidad digital/edad/género)
            ─► precondición de CUALQUIER pooling o comparación entre subgrupos (03 §15, 08 §11.5) — DISTINTO de 008
Instrumentación de nivel-ítem + research-data lane (PRE-BETA, §7-data) ─► pista IRT/análisis-de-ítems (BLOQUEADA)
ADR-DATA-001 (event schema/store/sync) + ADR-DATA-002 (retención/borrado) ─► Sync (fuera de F, gated)
```

**Reglas del DAG:** ADR-SCI-003 NO puede agregar dimensiones que ADR-SCI-001 no ha definido; ADR-SCI-004 no reporta
por-bloque sin 001 (arista 004→001). La `ScoreScaleKey` **particiona por clase de dispositivo/modalidad** hasta que
ADR-SCI-008 valide un puente — sin puente, no hay pooling cross-device dentro de una key. Ningún pooling/comparación
entre subgrupos demográficos antes de ADR-SCI-009 (invariancia/DIF). Redactables en paralelo
(problem/opciones/simulación): 001, 002, 003, 004. **No aprobables** hasta cerrar dependencias: 004 (tras 003),
Global oficial (tras 006), mezcla device (tras 008), subgrupos (tras 009), sync (tras DATA-001/002), pista IRT
(tras el research-lane §7-data).

---

## 21. Fases F0–F10 (entry/exit gates)

**Gate estándar (doc-only NO aplica Gradle):** cuando una fase futura toque código, su exit gate =
`:shared:desktopTest` + `:shared:detekt` + `:shared:compileAndroidMain` + `:shared:evidenceBoundaryScanner`
(+ `:androidApp:compileDebugKotlin` si toca wiring de app; + `:shared:mindboostCopySafetyScanner*` si toca dirs
escaneados), en serie, mismo worktree; iOS = NOT VERIFIED en Windows (P1 de paridad honesto).

```yaml
- id: F0
  objective: REOPEN + freeze; redactar borradores ADR-SCI-001/002/003/004 (problema/opciones/evidencia/simulación)
  userOutcome: ninguno (UI sin cambios; CALIBRATING como hoy)
  scientificOutcome: fronteras de modelo y dependencias fijadas; normal-normal degradado a candidato condicionado
  entryGates: [este documento aprobado por review §18]
  repoEvidence: docs/mindboost_profile_v2/** ; docs/context_pack/**
  filesExpected: [docs/mindboost_profile_v2/PROFILE_V2_MASTER_PLAN.md, +2 borradores ADR]
  owners: [CEO, lead-orchestrator]
  agents: [psychometrician, mindboost-assessment-psychometrics, efficacy-guardian, kmp-architect, data-sync-dev, privacy-security, mindboost-product-ux, diff-auditor]
  implementation: doc-only
  tests: n/a
  exitGates: [diff-auditor APTO sobre el archivo final]
  failureMode: modelo cerrado por elegancia; dependencia ADR saltada
  rollback: revert del doc (nada lo consume)
  flags: none
  migration: none
  privacyImpact: none
  documentation: [este archivo, Decision Log al registrar cada ADR]
- id: F1
  objective: raíles + tipos measurement (puro, NO cableado); VERIFICAR el cableado del flag global YA existente (no "reconciliar un flag no cableado") + resolver que PROFILE_V2_GLOBAL_ENABLED = alias del mindboostGlobalScoreEnabled existente
  userOutcome: ninguno
  scientificOutcome: tipos de EvidenceSufficiency/ModelUncertainty/CalibrationState/ObservationDecision (sin lógica de estimación); reloj inyectado, nunca Clock.System
  entryGates: [6-R3 CERRADO reproduciblemente verde, ADR-SCI-003 APROBADO]   # BLOQUEADA hoy — ambos abiertos
  repoEvidence: FeatureFlags.kt (flag global ya existe), MindBoostProfileAggregator.summarize (flag YA cableado, :122-173)
  filesExpected: [measurement/** tipos, FeatureFlags.kt (+flags v2), measurementBoundaryScanner + consumer-sweep inverso]
  owners: [cognitive-engine-dev, kmp-architect]
  agents: [kmp-architect]
  implementation: tipos measurement (reciben tiempo por nowMs/EpochClock inyectado, NUNCA Clock.System.now() — learning kotlinx-datetime 0.7.1); measurementBoundaryScanner self-scan Tier-1 shape-ban (Double/Float/Map/Any/Duration/typealias) + consumer-sweep INVERSO sobre TODO commonMain (allowlist vacía), espejo de los DOS scanners 6-R3 (§19)
  tests: [invariantes de tipo; AMBOS scanners RED/GREEN con mutación; allowlist consumer vacía; desktopTest compila sin Clock.System]
  exitGates: [gate estándar]
  failureMode: cablear antes de tener estimador aprobado
  rollback: flags OFF; revert
  flags: [PROFILE_V2_SHADOW_COMPUTE/PERSIST, CANDIDATE_READ, DISPLAY, BLOCK_<id>, GLOBAL — todos OFF en release]
  migration: none
  privacyImpact: none
  documentation: [Technical Current State]
- id: F2
  objective: ObservationModel por familia + ComparableSeries (puro)
  userOutcome: ninguno
  scientificOutcome: cada familia declara su modelo de observación versionado; series por ScoreScaleKey; INCOMPATIBLE cross-key
  entryGates: [F1; ADR-SCI-002 redactado para familias RT]
  repoEvidence: adapters §3 (fuentes por familia), MeasurementLineage (6-R3)
  filesExpected: [measurement/observation/**]
  owners: [cognitive-engine-dev]
  agents: [psychometrician]
  implementation: modelos binomial/RT/ordinal/count + regla no-mean-cross-serie
  tests: [golden por familia; property: no se mezclan keys]
  exitGates: [gate estándar]
  failureMode: fórmula universal; promediar ordinal como intervalo
  rollback: revert
  flags: none (puro)
  migration: none
  privacyImpact: none
  documentation: [ADR-SCI-002]
- id: F3
  objective: precondiciones W1+W4 + fix TRAINING_FREE (blast radius E8)
  userOutcome: ninguno (E8 sigue funcionando)
  scientificOutcome: elegibilidad sellada+versionada; hecho anti-gaming en el run; registro exercise∈block; propósito re-derivado
  entryGates: [6-R3 CERRADO]
  repoEvidence: SessionEligibility (forjable), SessionPurpose.mayContribute* (TRAINING_FREE=true), catálogo
  filesExpected: [core/session/cognitive/** (edits), ExerciseBlockRegistry]
  owners: [cognitive-engine-dev, data-sync-dev]
  agents: [privacy-security, efficacy-guardian]
  implementation: sellar eligibility (ctor no público + policyVersion + re-validación DTO); persistir anti-gaming fact; corregir enum Y re-derivar en lectura (o migrar) los booleanos calibrationEligible/rankingEligible YA PERSISTIDOS en runs históricos (no solo el enum a futuro); gate BLOQUEANTE antes de cualquier consumidor run→calibración/rango
  tests: [regresión E8 completa; DTO re-validation adversarial; registry↔catálogo; run histórico con mayContribute*=true NO cuenta (re-derivación en lectura)]
  exitGates: [gate estándar + review privacy/efficacy]
  failureMode: romper el sellado E8; corregir enum sin blast-radius
  rollback: revert slice (v2 aún no consume)
  flags: none
  migration: [enum SessionPurpose = major, dual si aplica]
  privacyImpact: alto (E8 sealing) — review obligatorio
  documentation: [Decision Log W3, Technical Current State]
- id: F4
  objective: LongitudinalStateEstimator candidatos + shadow COMPUTE + simulation matrix
  userOutcome: ninguno (candidato jamás visible)
  scientificOutcome: estimador elegido por familia con simulación; intervalos calibrados; regresión-a-la-media auditada
  entryGates: [F2; ADR-SCI-003 APROBADO con simulaciones]
  repoEvidence: InterSessionConfidence (deprecar), MASTER_PLAN_RECONCILED Fase A (kernel de normalización)
  filesExpected: [measurement/estimator/**, sim/**]
  owners: [cognitive-engine-dev]
  agents: [psychometrician, mindboost-assessment-psychometrics]
  implementation: candidatos §8 + prior versionado + cold-start; shadow compute SOLO sobre la matriz de simulación SINTÉTICA (§15) — el shadow-scoring poblacional real (subgrupos/dispositivo) es EXCLUSIVO de F7 (sin acceso sancionado a evidencia real antes del gateway); sin persistir aún
  tests: [simulation matrix §15 completa con métricas §15; goldens; property]
  exitGates: [gate estándar + registrar ADR-SCI-003]
  failureMode: cerrar normal-normal universal; shrinkage vendido como anti-regresión
  rollback: SHADOW_COMPUTE OFF; revert
  flags: [PROFILE_V2_SHADOW_COMPUTE_ENABLED]
  migration: [priorVersion/algorithmVersion versionados]
  privacyImpact: none (puro)
  documentation: [ADR-SCI-003, Decision Log]
- id: F5
  objective: Sufficiency/Uncertainty objects + CalibrationStateMachine + BlueprintRegistry (opt-in) + SubskillProjection
  userOutcome: ninguno
  scientificOutcome: 7 estados de calibración (03 §11.4) con 5-sesiones/3-días/48h/cobertura; subhabilidad/bloque
  entryGates: [F4; ADR-SCI-001 APROBADO para subskill/block]
  repoEvidence: CalibrationPolicy (ESTABLISHED por validSessions≥5 && distinctDays≥3, SIN floor 48h — over-claim VIVO hoy), SessionEvidence (sin campo temporal), aggregator (media ponderada)
  filesExpected: [measurement/calibration/**, measurement/projection/**]
  owners: [cognitive-engine-dev]
  agents: [psychometrician, mindboost-assessment-psychometrics]
  implementation: EvidenceSufficiency/ModelUncertainty separados; state machine; blueprint (Bridged único constructor); añadir SPAN temporal a SessionEvidence (primer↔último, re-derivado del clockSource, nunca fabricado) → floor de 48h REAL (distinctDays≥3 NO implica 48h); copy no-técnico de los estados nuevos. Guard interino del over-claim vivo: la prohibición del 0-100 desnudo (§14) hasta que exista el span
  tests: [truth-table estados; CALIBRATED inalcanzable sin blueprint; grinding no supera banda; 3 días en <48h NO alcanza ESTABLISHED]
  exitGates: [gate estándar]
  failureMode: aliasing sufficiency↔confidence; CALIBRATED sin blueprint
  rollback: revert
  flags: none (puro)
  migration: none
  privacyImpact: none
  documentation: [ADR-SCI-001]
- id: F6
  objective: Growth / reliable change
  userOutcome: ninguno (aún OFF)
  scientificOutcome: RCI con SEM + corrección de práctica; sin baseline/SEM ⇒ INSUFFICIENT_DATA
  entryGates: [F5; ADR-SCI-004 APROBADO (depende de 003); programa de fiabilidad BETA disponible (SEM/MDC estratificado, §9) — RCI necesita SEM, MDC necesita test-retest]
  repoEvidence: 03 §10.2/§10.3
  filesExpected: [measurement/growth/**]
  owners: [cognitive-engine-dev]
  agents: [psychometrician]
  implementation: estados 03 §10.3; INSUFFICIENT_DATA por defecto (NO "INCONCLUSIVE", que no existe en el enum); método de cambio fiable POR tipo de observación (RCI gaussiano solo si aplica)
  tests: [sin baseline/SEM⇒INSUFFICIENT_DATA; nunca delta de series no comparables; ordinal/conteo no usan RCI gaussiano]
  exitGates: [gate estándar + registrar ADR-SCI-004]
  failureMode: RCI genérico sin práctica/SEM
  rollback: revert
  flags: none (puro)
  migration: none
  privacyImpact: none
  documentation: [ADR-SCI-004]
- id: F7
  objective: ProfileEvidenceGateway + shadow PERSIST + análisis de divergencia
  userOutcome: ninguno (candidato persistido, nunca leído)
  scientificOutcome: único consumidor allowlisted; elegibilidad sellada; equivalencia policy↔boundary
  entryGates: [F3, F4; ADR-SCI-005 posición interina para dosis]
  repoEvidence: evidence boundary scanner, MindBoostProfileService.refresh
  filesExpected: [measurement/gateway/**, MindBoostProfileService (edit), alta EXACTA en allowlist C3]
  owners: [cognitive-engine-dev, data-sync-dev]
  agents: [efficacy-guardian, kmp-architect]
  implementation: gateway re-derivación total; consumer-sweep inverso + Tier-1 shape-ban activos (§19); gateway construido SOLO sobre un repo ya acotado por namespace/subject (subjectToken es auditoría, NO control); persist candidato bajo shadow_v2; reporte divergencia legacy-vs-candidato — si usa cohorte REAL, consentimiento "Investigación" (capa 4) diseñado ANTES
  tests: [e2e: válido entra 1 vez; inválido no; duplicado no cambia; ausencia no baja; reorden no cambia; versiones no se mezclan; null-through-aggregator (05 §9.2); equivalencia policy↔boundary (OBLIGATORIO §19); cuenta A→sign out→B: el batch del gateway para B contiene CERO runs de A (no-fuga a nivel gateway, no solo store)]
  exitGates: [gate estándar + review efficacy]
  failureMode: segunda verdad de scoring; candidato leído por UI
  rollback: quitar alta de allowlist (build re-prueba no-consumo); SHADOW_PERSIST OFF
  flags: [PROFILE_V2_SHADOW_PERSIST_ENABLED]
  migration: none (candidato no visible)
  privacyImpact: medio — review
  documentation: [Technical Current State]
- id: F8
  objective: Presentación v2 (flag OFF) + snapshot v2 + migration matrix
  userOutcome: ninguno hasta flip (F9)
  scientificOutcome: UI honesta con banda condicionada + estado + growth panel; snapshot v2 con campos §9-SoT
  entryGates: [F5, F7]
  repoEvidence: BrainProfileUiModelBuilder (CALIBRATING→"—"), MindBoostProfileDtos (schemaVersion)
  filesExpected: [profile/presentation/** (edits), data/dto|mapper (v2)]
  owners: [compose-ui-dev, cognitive-engine-dev]
  agents: [mindboost-product-ux]
  implementation: builder v2 tras PROFILE_V2_DISPLAY_ENABLED(OFF); próxima-acción ARRIBA (PRD §11.8); ejes calibración/growth separados (§14); estado de ERROR explícito + reintento en el state holder (refresh/save que lanza ⇒ bandera de error, NO loading colgado perpetuo — hoy MindBoostProfileService.save no está envuelto); copy no-técnico de estados F5; matriz de migración §12; snapshot corrupto⇒rebuild/fail-closed
  tests: [pin CALIBRATING→"—"/null; snapshot v1 legible; recompute-desde-stores; snapshot corrupto no se usa; refresh que lanza ⇒ estado de error con reintento, no loading colgado]
  exitGates: [gate estándar]
  failureMode: banda sin calibración; current→best swap; fabricar 0
  rollback: DISPLAY OFF
  flags: [PROFILE_V2_DISPLAY_ENABLED]
  migration: [snapshot v1→v2: recompute local; v1 conservado hasta primer write v2]
  privacyImpact: none nuevo
  documentation: [Technical Current State]
- id: F9
  objective: flip por bloque (CANDIDATE_READ→DISPLAY) cuando el bloque cumpla el gate real
  userOutcome: dimensión medible visible SOLO si su ejercicio emite runs + banco validado + timebase + blueprint + validación física §15
  scientificOutcome: sin over-claim; probablemente arranca en CERO o solo E8 (resto CALIBRATING, igual que hoy)
  entryGates: [F8; por bloque: OFFICIAL_MEASUREMENT-eligible + paridad shadow]
  repoEvidence: efficacy 6-R3 §6.2 (solo Focus/Logic no-null hoy; ninguno emite runs con dossier)
  filesExpected: [flags por bloque + docs de criterio por bloque]
  owners: [lead-orchestrator]
  agents: [efficacy-guardian, psychometrician]
  implementation: encendido por-bloque con criterio documentado; shadow invertido (legacy en sombra); gate EXPLÍCITO sobre el efecto colateral established.size→Global (cruzar una 3ª dimensión a ESTABLISHED NO surfacea Global PROVISIONAL sin decisión — flag remoto es la única barrera restante; el OFICIAL sigue tras la const ADR-SCI-006); declarar el trato de Focus/Logic bajo la barra F9 (retener su número actual hasta cumplir dossier/banco/timebase, o justificar QUICK_PROVISIONAL sin dossier)
  tests: [soak 1 ciclo con v2 visible y legacy en sombra; cruzar 3 dimensiones ESTABLISHED NO enciende Global provisional sin ADR-SCI-006/decisión]
  exitGates: [gate estándar + review efficacy/psychometrician por bloque]
  failureMode: encender un bloque sin runs+dossier (over-claim)
  rollback: BLOCK_<id> OFF; comparación sombra continua
  flags: [PROFILE_V2_CANDIDATE_READ_ENABLED, PROFILE_V2_BLOCK_<id>_ENABLED]
  migration: none
  privacyImpact: none
  documentation: [Decision Log por bloque]
- id: F10
  objective: demolición legacy (9 adapters + fan-in + puerto + ModuleContribution + ProfileUpdateGate + SessionEvidence)
  userOutcome: ninguno (v2 ya es la ruta visible)
  scientificOutcome: una sola autoridad de medición
  entryGates: [F9 estable ≥1 soak; condiciones de eliminación §12 TODAS verdes]
  repoEvidence: read-audit/telemetry cero uso; grep+dep scanner cero
  filesExpected: [deletes + Tier-2 scanner + tests de invariantes MIGRADOS]
  owners: [refactor-cleaner, cognitive-engine-dev]
  agents: [diff-auditor]
  implementation: borrar tras paridad; migrar tests de honestidad ANTES del delete; símbolos retirados al ban-list
  tests: [invariantes MIGRADOS verdes antes del delete; scanner RED con reintroducción]
  exitGates: [gate estándar + diff-auditor]
  failureMode: borrar con consumidores legacy vivos; perder un test de invariante
  rollback: git revert del slice de borrado
  flags: none (limpieza)
  migration: [export/delete cubren ambos schemas antes del borrado]
  privacyImpact: none
  documentation: [Technical Current State, Decision Log]
```

---

## 22. Simulation matrix → ver §15 (matriz + invariantes + métricas). Cada fórmula nueva pasa 03 §27 (hipótesis →
dataset sintético con casos límite → simular estrategias → invariantes → tests input→output + property → doble
scoring → estabilidad/sesgo/longitudinal → gate científico+técnico → versionar+migrar → monitorizar).

## 23. Test strategy

- **Unit/golden/property** por familia (§7): domino [0,1]∪{null}; degenerado⇒bajo; determinismo numérico.
- **Simulación** (§15): matriz completa con métricas (bias/RMSE/coverage/false-pos/false-neg/robustness/…).
- **Integración/wiring** (05 §9.2): dato válido entra 1 vez; inválido no; duplicado no cambia; ausencia no baja;
  reorden no cambia; versiones incompatibles no se mezclan; null-through-aggregator e2e.
- **Migración** (07 §12): datos de versión anterior; snapshot corrupto/incompleto; recompute desde stores; fail-closed.
- **Shadow** (08 §12.2): divergencia legacy-vs-candidato con análisis de saltos/rank-flips/subgrupos.
- **Boundary/scanner**: `measurementBoundaryScanner` + allowlist consumer + mutación RED/GREEN.
- **Adversarial** (04 §11 / 07 §6): degenerados, farming rotando ejercicios, reloj futuro, cuenta A→B.
- **UI/plataforma**: CALIBRATING→"—"; Android compile; iOS NOT VERIFIED declarado.

## 24. Rollback

Por capa de flag (compute/persist/read/display/block/global) — apagar revierte instantáneamente sin borrar datos.
Fases puras (F2/F4/F5/F6): revert trivial (nada las consume). F3 (E8 sealing): revert del slice. F10 (borrado):
git revert. **Ninguna fase borra la capa de invariantes antes de que su reemplazo esté verde.**

## 25. Risk register (top-11)

| # | Riesgo | Mitigación |
|---|---|---|
| R-1 | Colisión con 6-R3 en el mismo worktree | F0 doc-only ∥ 6-R3; F1+ BLOQUEADA hasta cierre 6-R3; gates seriales; verificar que MIS archivos aparecen en findings |
| R-2 | Sobre-modelar psicometría sin datos (IRT-fantasía, equating inventado) | Modelo por familia con alternativa simple; IRT tras gate de datos §7-data; equating solo por puente registrado; ADR-SCI-003/004 con simulación antes de cablear |
| R-3 | Reintroducir bugs documentados (0L centinela, confidence↔sufficiency, defaults que desactivan gates, recencia, booleanos forjables) | Checklist de Aprendizajes como gate de review por fase; invariantes con test; separación de objetos §9 |
| R-4 | Romper el evidence boundary / consumo no sancionado | Alta en allowlist = 1 diff revisable; `measurementBoundaryScanner` + evidence scanner; mutación RED/GREEN |
| R-5 | Segunda fuente de verdad de scoring | §26 absorbe/supersede cada fase; kernel Fase A = única normalización por-ejercicio; equivalencia policy↔boundary (F7) |
| R-6 | Shadow-flip con divergencia silenciosa | F7 reporte de divergencia; F9 soak con sombra invertida; flip por-bloque reversible |
| R-7 | UI rota o dato fabricado en transición | Flags OFF; pin CALIBRATING→"—" en todas las fases; ninguna fase toca ruta visible sin flag |
| R-8 | Hueco de erasure con superficies nuevas | Reuso `mindboost_profile`; toda colección/tabla nueva en eraser+CF el MISMO cambio; `deleteAll` fail-closed |
| R-9 | Scanners de build en rojo (lexicon/detekt) en archivos nuevos | Nombres lexicon-safe; patrones detekt conocidos (tabla de gates, imports de enum-entries, SCREAMING_SNAKE); variante del copy-scanner del source-set |
| R-10 | El perfil nunca sale de CALIBRATING → producto parece muerto | Mitigación YA EMBARCADA (respuesta de F0-F7): progreso-a-primera-lectura + próxima acción (`BrainProfileUiModelBuilder`/`RecommendationCard`) — el valor percibido NO depende de un número. `QUICK_PROVISIONAL` (03 §11.4) surfacea una POSICIÓN CATEGÓRICA ("Lectura inicial registrada"), NUNCA un 0-100 ni banda numérica (mejora F5+, no día 1). Flip F9 empieza por el bloque con mejor pipeline |
| R-11 | Beta autoseleccionada: exposición sesgada, restricción de rango, potencia DIF/IRT insuficiente | Potencia/precisión preregistradas antes de datos confirmatorios (§7-data); el riesgo afecta fiabilidad (ICC/test-retest, 08 §11.3) e invariancia (DIF, 08 §11.5), no solo el fitting IRT |

## 26. ADR backlog (reconciliación con MASTER_PLAN_RECONCILED y W1–W5)

**MASTER_PLAN_RECONCILED (Fases A–F):** A (kernel normalización + fix CL) SE CONSERVA en los scorers por-ejercicio
(no se duplica en el estimador; el estimador consume el score sellado). B (mappers por-módulo) = prerequisito de F9
por bloque (emitir runs honestos), FUERA de este plan. C (lifecycle+EMA+CV) SUPERSEDED por F4/F5 (posterior en vez
de EMA; CV-confidence ya SUPERSEDED en Decision Log §18). D (Global/rangos/decay) = flag global existente + SCO-014/015;
rangos/decay PROPUESTA (D-3). E (CognitiveSummaryDto) fuera. F (Monte Carlo) ABSORBIDA en F4/§15.

**W1–W5:** W1 (sellar eligibility + anti-gaming fact) = F3. W2 (purga sign-out) SUPERSEDED por subject-scoping 6-R3
(sign-out = pausa, nunca purge; conservación). W3 (TRAINING_FREE) = F3/§13. W4 (registro exercise∈block) = F3.
W5 (CalibrationBlueprint + equating) = F5 (estructura opt-in) + ADR-SCI-008 (puentes numéricos).

**ADR requeridos (09 §19), redactables en paralelo vs bloqueantes:** SCI-001 (subhabilidades — bloquea F5) ·
SCI-002 (velocidad-precisión — F2) · SCI-003 (estimador — bloquea F4, y F1) · SCI-004 (Growth — bloquea F6, depende
003) · SCI-005 (dosis — bloquea "oficial") · SCI-006 (Global oficial — const, permanentemente OFF) · SCI-008
(device/modalidad — PUENTE hardware) · **SCI-009 (invariancia/DIF de equidad — bloquea pooling/comparación entre
subgrupos, distinto de 008)** · DATA-001/002 (schema/sync/retención — bloquean sync). **001/002/003/004 redactables
en paralelo; no aprobables fuera de su orden de dependencia (§20).** Precondiciones NO-ADR declaradas: instrumentación
de nivel-ítem + research-lane (§7-data, bloquea IRT) y programa de fiabilidad beta (§9, bloquea F6).

## 27. Decisions closed (D-1…D-10 — resueltas, no devueltas al CEO)

| ID | Resolución |
|---|---|
| D-1 | Programa `REOPEN` hasta corregir este documento (aplicado). |
| D-2 | `TRAINING_FREE` se corrige ANTES de cualquier wiring (F3, precondición). |
| D-3 | Ranges/decay por ADR; **no mean decay por inactividad** (STALE + ensanchamiento candidato, nunca bajada). |
| D-4 | Sync por ADR-DATA; activación externa gated (flag OFF; no schema cloud definitivo). |
| D-5 | Recompute SOLO tras auditoría de runs; hoy imposible para 8/9 módulos ⇒ legacy read-only + candidato CALIBRATING. |
| D-6 | Progressive disclosure (banda solo si calibración defendible). |
| D-7 | Regla universal de dosis RECHAZADA; shadow por familia hasta ADR-SCI-005. |
| D-8 | `InterSessionConfidence` deprecado ahora; eliminación tras cero refs (no se pregunta al CEO). |
| D-9 | Brecha de borrado cross-account de `mindboost_profile` (blanket `clearNamespace`): precondición a cerrar antes de F7/F8 con `LocalMindBoostProfileRepository.clear(userId)` + test E2E; técnica, no CEO (§16 T2). |
| D-10 | Sync del perfil: ADOPTAR `MINDBOOST_FIRESTORE_SYNC_DESIGN.md` (decisión CEO 2026-07-04) como Impl-1 y reconciliar flag/LWW/E2E en ADR-DATA; no reinventar (§10-sync). Activación externa sigue elevable (§28). |

## 28. Genuine external decisions (elevables al CEO — 02 §14: solo gasto/publicación/borrado/contacto externo)

Ninguna decisión técnica/científica de este plan requiere al CEO. Las genuinamente externas, cuando lleguen su fase:
(a) **gasto/operación del VPS** (ADR-ID-001/ADR-DATA-001, BUS-010 = 0 €) — elevar antes de aprovisionar; (b)
**activar sync a cloud real** (acción externa con datos) — elevar en su gate; (c) **publicar cualquier claim de
score/global** — bloqueado por gates, no es decisión de este plan. Todo lo demás se resuelve con ADR + evidencia.

## 29. Evidence limitations (no verificado)

- **iOS:** NOT VERIFIED (sin toolchain Kotlin/Native en Windows) — P1 de paridad honesto, no un falso "sin P0/P1".
- **Gates Gradle:** NO ejecutados (reapertura doc-only; el IDE puede poseer el daemon del worktree). El estado
  "6-R3 verde" es `REPORTED`, no reproducido aquí.
- **Grafo Graphify:** OBSOLETO (`35016318` ≠ HEAD) — no usado como evidencia; todo verificado por lectura directa.
- **Worktree paralelo:** puede mutar (Vibe Kanban/n8n); las líneas exactas citadas son del snapshot `71e17038` +
  lecturas de esta sesión.

## 30. Definition of Done (de esta reapertura)

- [x] Plan completo adjunto (`docs/mindboost_profile_v2/PROFILE_V2_MASTER_PLAN.md`).
- [x] normal-normal = candidato condicionado, no universal (§7/§8).
- [x] sufficiency/uncertainty separados sin aliasing (§9).
- [x] sin dosis universal (§13).
- [x] auth y sync separados (§10-sync).
- [x] O(1) corregido a O(k)/O(dimensions)/O(n) (§9-SoT).
- [x] migración con fallback conservador (§12: legacy read-only + CALIBRATING para 8/9 módulos).
- [x] los 121 ejercicios con frontera de elegibilidad explícita (§18).
- [ ] **pista de ítem/IRT BLOQUEADA (no "diferida sin perder instrumentación")**: la instrumentación de nivel-ítem
  NO existe, §16 la prohíbe salir del dispositivo, y el research-lane no tiene owner ni fase (§7-data). La pista de
  PERSONA (F4) sí funciona sobre datos de sesión — son dos pistas separadas.
- [x] Global OFICIAL OFF por **constante** `ADR_SCI_006_APPROVED` (robusto); Global PROVISIONAL gated por **flag
  remoto + `established.size≥3`** (§15/§20 — NO un único invariante "inalcanzable").
- [x] baseline §3/C10 corregido: flag **YA cableado** (`VERIFIED`, no "half-landed"); `DisplayScore` =
  `ProvisionalStable/ProvisionalEmerging/Withheld` (no `Official`).
- [x] frontera measurement = scanner INVERSO (consumer-sweep) + Tier-1 shape-ban, no solo forward-import-ban (§19).
- [x] EpochClock inyectado, nunca `Clock.System` (§5/F1).
- [x] sin wiring ni código productivo (doc-only).
- [x] D-1…D-10 resueltas, no como preguntas al CEO (§27).
- [x] F1 mecánicamente bloqueada por 6-R3 + ADR-SCI-003 (§20/§21).
- [x] review fresco §18 ×7 completado (UX/psicometría-readiness/modelo-medición/honestidad-eficacia/KMP/datos-sync/
  privacidad) — hallazgos P0/P1 integrados (changelog R2).
- [ ] `diff-auditor` en contexto fresco APTO sobre el archivo final (ÚLTIMO paso, aún pendiente).
- [ ] P2/P3 no implementados con owner/razón/gate (se registran en el reporte de cierre §21-report).

> DoD NO cumplida hasta que el `diff-auditor` en contexto fresco emita APTO sobre este archivo tras los reviews §18.
