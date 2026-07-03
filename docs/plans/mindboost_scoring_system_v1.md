# MindBoost Scoring System v1

**Destino:** `docs/plans/mindboost_scoring_system_v1.md` · **Fecha:** 2026-07-02 · **Estado:** diseño vinculante para MVP (implementación por fases).
**Fuentes de verdad:** memo de decisiones D1–D14 + addenda A1–A18 (prevalecen), PRD §14, auditoría §10, código real de `core/scoring/` (`ScoreResult`, `ScoreStateMachine`, `MinSampleGate`, `DetectorCoverage`, `BankIntegrity`, `ClaimLevel`) y `profile/domain/aggregation/` (`ProfileUpdateGate`, `MindBoostProfileAggregator`).

---

## 1. Goals

1. **Un solo lenguaje de scoring** sobre los 4 motores coexistentes (core/scoring; FocusScore; ReasoningScorer; ScoringEquations legacy) mediante **adapters en frontera** — nunca reescribiendo la matemática nativa validada (D7/A5).
2. **Sistema de dos capas** (D1): la capa interna es el veredicto de honestidad (`ScoreResult` + estados `CALIBRATING/EMERGING/ESTABLISHED` del perfil); la capa de usuario (score 0–100, bandas, rangos V1.1) se construye ENCIMA y consume la interna como gate. La interna es autoritativa.
3. **Nunca fabricar señal**: `null ≠ 0` en todos los niveles; la UI muestra "—" sin datos.
4. Scores de bloque **comparables entre sí** (misma escala, mismo mapping versionado) para habilitar un Global Score honesto (A4).
5. **Versionado y migrable**: toda fórmula tiene versión central; ningún score histórico cambia retroactivamente sin trazabilidad (§17–§18).
6. **Local-first** (D3): todo el cómputo es puro, determinista y testeable en `:shared:desktopTest`; sin backend ni normas poblacionales en MVP.

## 2. Non-Negotiable Rules

1. **`null ≠ 0`.** Un estado no-VALID jamás transporta `primarySignal`; ausencia de dato se renderiza "—", nunca "0" (invariante de `ScoreResult.init` y `ScoreUiState`).
2. **Solo `VALID` actualiza el perfil** (R-NO-REDUCE). `ProfileUpdateGate` es el ÚNICO puente `ScoreResult → ModuleContribution`; nunca se pasa un `Double` crudo.
3. **La velocidad sin precisión JAMÁS se premia** (D13, PRD §14.6): gate `accuracy ≥ 0.80` con rampa 0.80→0.95 (§5); RT bajo el umbral de impulsividad no cuenta como acierto aguas arriba.
4. **Anti-gaming solo cuenta si está testeado**: un detector "diseñado pero no testeado" es `DetectorCoverage.NONE/PARTIAL`, nunca `ADEQUATE`; subir cobertura exige suite red-team (adversario dispara + honesto no dispara).
5. **Banco no validado bloquea el perfil**: `SMALL_UNVALIDATED/CONTAMINATED/HOLDOUT_EXHAUSTED` → `BANK_CONTAMINATED` en runtime (state machine) además del `BankValidator` de build.
6. **RT-crítico exige timebase autoritativo** (`TimebaseQuality.AUTHORITATIVE`); jank > 10% de frames caídos invalida la medida temporal.
7. **La salida de seguridad jamás penaliza**: abandono en ejercicio emocional/frustración → `ABANDONED_SAFE`, sin culpa, sin score bajo. La fatiga solo ADAPTA, nunca penaliza (A14); la "penalización moderada por fallar" del PRD §13.5 queda eliminada.
8. **Prohibido el percentil poblacional** hasta normas propias (V2). Toda normalización es intra-persona contra la propia línea base.
9. **Claims**: solo "entrenas y mides X dentro de MindBoost contra tu propia línea base" (A18); `ClaimLevel` gatea la fuerza; el copy-scanner de build gatea el léxico.
10. **Motores nativos congelados**: golden/characterization tests ANTES del primer adapter; nunca scorer y wiring en el mismo PR (A5).
11. **Sesiones inválidas se excluyen** del scoring (nunca puntúan bajo); se persisten marcadas y cuentan solo en analytics (§4).

### Reconciliaciones explícitas (vinculantes)

| Conflicto | Resolución |
|---|---|
| **PRD 0–100 vs capa interna de estados** | Coexisten por diseño (D1): la capa interna (`ScoreResult`/estados) es el GATE; la capa de usuario 0–100 se computa encima consumiéndolo. El PRD §10/§14 se reescribe para reflejar la arquitectura de dos capas. |
| **5 sesiones (PRD §14.9) vs n≥20 trials (MinSampleGate)** | AMBOS, son unidades ortogonales (A2): trials gobiernan computabilidad estadística; sesiones/días gobiernan estabilidad temporal (rasgo vs estado). EMERGING = nMin + ≥2 sesiones válidas; ESTABLISHED = ≥5 sesiones válidas en ≥3 días + ≥2×nMin + estabilidad + anti-gaming limpio. |
| **Media simple (PRD §14.11) vs ponderada (aggregator)** | Ambas, con destino distinto (A4): la ponderada por `confidence×dataSufficiency` alimenta el radar interno del perfil (código actual, sin cambios); la media SIMPLE de bloques CALIBRADOS alimenta el Global Score de usuario. CALIBRATING nunca cuenta como 0: se excluye. |
| **28 rangos (PRD §14.12) vs honestidad** | El sistema de rangos se DISEÑA completo (§15) pero se difiere a V1.1 (A3). Rango solo desde ESTABLISHED; semántica de progresión intra-persona, sin comparabilidad inter-usuario hasta normas propias (V2). MVP muestra estado por bloque + score al calibrar + progreso contra la propia línea base. |

## 3. Data Model

**Existente (sin cambios de semántica):** `ScoreResult` (estado, `primarySignal: Signal?`, `Sampling{nEffective,minNRequired,confidence,crossSessionAggregated,confidenceInterval}`, `updatesProfile`, `preliminary`, `DataQuality`, `ClaimLevel`, `copySafetyFlags`), construido SOLO vía `ScoreResultFactory`; `ScoreProvenance` (ya transporta `scoringVersion` + `itemBankVersion`); `ModuleContribution` (normalizedScore 0–1 o null); `MindBoostProfileAggregator` (config 0.5/0.4/0.7/3); `ScoreResultDto` (`schemaVersion`, `Json ignoreUnknownKeys`).

**Nuevo (todo PURO, commonMain):**

- `core/scoring/user/ScoreComponents` — los 6 componentes normalizados 0..1 de una sesión: `precision`, `speed`, `difficulty`, `consistency`, `progress`, `errorControl` (+ `accuracyForGate`, `rtCv`).
- `core/scoring/user/BlockScoreEngine` — `sessionScore(components, weights): Double [0..1]` + `updateHistory(prev, session): Double` (EMA+MAD+clamp, §7).
- `core/scoring/user/UserScoreMapper` — mapping 0..1→0–100 logístico por bloque, versionado (§7).
- `core/scoring/CalibrationPolicy` — criterio compuesto sesiones×trials×días×estabilidad (§9), consumido por `ProfileUpdateGate` y el aggregator.
- `core/scoring/ScoringVersionRegistry` — registro central de versiones activas (§17).
- `core/session/SessionRecord` (D11/A12) — COMPLETED/ABANDONED, duración, `exerciseVersion/scoringVersion/bankVersion/schemaVersion`, checkpoints; store cifrado en namespace `mindboost_sessions` (→ AccountBound + eraser en el mismo cambio).
- `core/scoring/user/RankLadder` — 28 umbrales versionados (diseñado aquí, implementación V1.1).

Persistencia: `SecureKeyValueStore` cifrado; DTOs `@Serializable` con `schemaVersion`; solo resúmenes, nunca trials crudos.

## 4. Session Validity

**Sesión válida** = COMPLETED ∧ duración ≥ 120 s ∧ sin anomalías extremas (PRD §30) ∧ el veredicto de la state machine no es `INVALIDATED/SUSPECTED_GAMING`. Solo las sesiones válidas cuentan para calibración, EMA y perfil.

- **Abandono** (A12): persiste resumen parcial marcado `ABANDONED` (write-ahead STARTED→CHECKPOINT→FINALIZED; huérfanos → ABANDONED al arrancar). Excluido de scoring, emite `exercise_abandoned`. `ON_PAUSE` invalida el trial en curso (no cuenta como omisión).
- **120 s es condición de sesión, no de métrica**: en bloques lentos (Lógica/Decisiones/Creatividad) una sesión válida puede no aportar nMin — la validez por volumen se decide por métrica (tabla §11), nunca globalmente.
- **Sesión 1 de cada ejercicio = familiarización**: se infra-pondera en el fold del perfil y se descarta de la métrica de mejora a 30 días (probe-a-probe + RCI, A6).
- **La evaluación inicial (Mode.EVALUATION) y el Check-In de Decisiones NUNCA cuentan** como sesión de calibración (D9/A10/A17).

## 5. Exercise Score

El motor nativo del ejercicio produce sus métricas (intacto). El **adapter de bloque** extrae los 6 componentes PRD §14.2, todos normalizados 0..1:

- **Precisión** = accuracy corregida (d′ o corrección por false-alarms donde aplique; nunca hit-rate crudo en reconocimiento).
- **Velocidad** = RT mediano invertido y normalizado contra la propia línea base, SIEMPRE multiplicado por el gate de precisión:
  `effectiveSpeed = speed × gate(acc)` con `gate(acc) = 0` si `acc < 0.80`; rampa lineal `(acc − 0.80)/0.15` en `[0.80, 0.95)`; `1.0` si `acc ≥ 0.95`.
- **Dificultad** = nivel adaptativo ALCANZADO (staircase/parámetros de ítem) en la escala normalizada 1–10 del bloque, /10 — nunca un flag estático (subir score exige subir dificultad).
- **Consistencia** = `1 − CV robusto` (mediana/MAD, no SD — no penaliza lapsos legítimos aislados).
- **Progreso** = delta vs línea base propia estilo RCI (nunca vs valor absoluto; separa mejora real de regresión a la media).
- **Control de errores** = `1 − (tasa de errores impulsivos + perseveración + elecciones dominadas)`, acotado 0..1.

`exerciseScore = Σ wᵢ·componenteᵢ` con los pesos por bloque de §19 (Σw = 1.0). **Un ejercicio solo produce número si su `ScoreResult` es `VALID`**; cualquier gate fallido → sin score de ejercicio (no un 0).

## 6. Session Score

`sessionScore ∈ [0,1]` = media de los `exerciseScore` VALID de la sesión, ponderada por volumen de evidencia (trials o duración planificada). Si ningún ejercicio es VALID → la sesión **no tiene score** (`null ≠ 0`); sigue contando como sesión válida para calibración si cumple §4.

**Filtro MAD pre-EMA**: sea `H` el histórico de session-scores del bloque (últimas ~10). Si `|s − mediana(H)| > 3 × 1.4826 × MAD(H)` (con suelo de MAD versionado para evitar degeneración), la sesión se marca atípica: se persiste y cuenta para calibración, pero **no entra al EMA** (§7). Una sesión atípica extrema jamás mueve el block score.

## 7. Block Score

Dos representaciones:

**Interna (existente, sin cambios):** la dimensión del perfil agrega `ModuleContribution`s con media ponderada `confidence×dataSufficiency` (`MindBoostProfileAggregator`).

**De usuario (nueva, `BlockScoreEngine` + `UserScoreMapper`):**

1. **Durante calibración** (§9 no cumplida): NO existe 0–100. Se computa un provisional interno `average(last 5 valid sessionScores)` que alimenta la banda ordinal si el display gate lo permite (§13).
2. **Al completar calibración**: `BlockScore₀ = UserScoreMapper(average(last 5 valid))`.
3. **Después**: `NewBlockScore = 0.85 × prev + 0.15 × UserScoreMapper(sessionScore)`, con filtro MAD pre-EMA (§6) y **clamp de volatilidad `|NewBlockScore − prev| ≤ 8` puntos/sesión** (segunda línea tras el MAD).

**Mapping oficial (`UserScoreMapper`, PURO y versionado, `core/scoring/user/`):**

```
userScore = 100 / (1 + exp(-k · (z − z0)))
```

- **Anclaje de z por bloque**: `z = (c − μ_base) / σ_base`, donde `c` es el composite de sesión normalizado por dificultad (los 6 componentes ya incorporan la dificultad alcanzada, §5), y `μ_base/σ_base` son mediana y MAD×1.4826 de los composites de las **sesiones 2..5 de calibración del propio usuario** (la sesión 1 se excluye por familiarización). `σ_base` tiene suelo versionado por bloque (evita división por ~0). `z0 = 0`, `k` constante versionada por bloque (default 1.2). Semántica: **50 = tu propia línea base**; >50 = rendimiento por encima de tu baseline a dificultad comparable. NUNCA percentil poblacional.
- **Fallback lineal documentado**: `userScore = 100 × normalized` SOLO para bloques sin curva de baseline calibrada; todo score así mapeado se marca `preliminary` + versión `-linear-fallback` en provenance, y la UI lo etiqueta provisional.

La dimensión interna EPISODIC_MEMORY se agrega al bloque de usuario "Memoria" con peso WORKING_MEMORY 0.7 / EPISODIC 0.3 (D5); el perfil interno conserva las 9 dimensiones.

## 8. Global Score

- **Interno (radar):** media ponderada `confidence×dataSufficiency` del aggregator (código actual); composite solo con ≥3 dimensiones ESTABLISHED.
- **De usuario:** **media SIMPLE de los `BlockScore` de bloques CALIBRADOS** (ESTABLISHED). Los bloques CALIBRATING/EMERGING se **excluyen** de la media — jamás cuentan como 0. Con <8 bloques calibrados el número se muestra con etiqueta "Perfil global provisional" (D5/A4).
- Rango global (V1.1): exige ≥5/8 bloques calibrados.

## 9. Calibration

**Criterio compuesto (A2, autoridad: `CalibrationPolicy`):**

- **EMERGING** ⇔ `nEffective ≥ nMin` (MinSampleGate por métrica) ∧ **≥2 sesiones válidas**.
- **ESTABLISHED** ⇔ **≥5 sesiones válidas en ≥3 días distintos** ∧ `nEffective ≥ 2×nMin` ∧ `metricStability = STABLE` ∧ anti-gaming limpio (sin WITHHOLD activo).

**Tabla de fiabilidad mínima por métrica (A2, gates de calibración):**

| Métrica | nMin | Condición adicional para ESTABLISHED |
|---|---|---|
| RT-mediana | ≥20 | split-half ≥ 0.7 |
| Accuracy | ≥20 + IC Wilson | semianchura IC95 ≤ 0.10 (~n≥60 agregado) |
| Span | ≥2 administraciones | banda SEM reportada |
| Difference-scores (congruencia/switch/dual/post-error) | ≥20/celda, TODAS las celdas | SOLO secundarias; split-half ≥ 0.6 |
| SSRT | ≥30 stops | convergencia del staircase p(stop)≈0.5 |
| Brier | ≥40 decisiones | — |
| d′ | ≥20 señal + ≥20 ruido | corrección log-lineal |
| BUR (recuperación) | ≥12 eventos | — |
| Cowan's K | ≥20 | — |
| RCI | ≥2 administraciones de probe | \|RCI\| > 1.96 para afirmar cambio |

Durante calibración el provisional es `average(last 5 valid)` — nunca EMA. La UI comunica SIEMPRE el progreso "X de 5 sesiones". Adversariales obligatorios: 5 sesiones el mismo día NO establece; 20 trials en 1 sesión se queda en EMERGING como máximo.

## 10. Confidence Level

**Fórmula canónica única** (en `ProfileUpdateGate`; elimina constantes ad-hoc como el 0.4 fijo de Memoria):

```
confidence = clamp01(0.4·S + 0.3·(1 − D) + 0.3·E)
S = min(sesionesVálidas/5, 1) · factorDías (0.6 si todos el mismo día, 1.0 con ≥3 días)
D = CV robusto de los sessionScores (dispersión inter-sesión, cap 1.0)
E = estabilidad métrica: STABLE→1.0, UNKNOWN→0.5, UNSTABLE→0.0
```

Umbrales preservados: aggregator `minConfidence=0.4`, `establishedConfidence=0.7`; display gate `0.55` (§13). Propiedad exigida: `establishedConfidence=0.7` solo alcanzable con ≥5 sesiones en ≥3 días (test de paridad entre adapters: mismos datos longitudinales → misma confidence).

`confidence` es **ortogonal** a `dataSufficiency` — jamás se mapea una desde la otra (aprendizaje 2026-06-28, ya sellado en `ProfileUpdateGate`).

## 11. Data Sufficiency

`dataSufficiency = min(nEffective / minNRequired, 1.0)`; un veredicto VALID ya pasó `MinSampleGate`, luego es 1.0 (código actual). La tabla golden de `MinSampleGate` es la única fuente de nMin (§9); subir cualquier valor es un cambio deliberado con goldens. Los MIN_N dispersos 3..5 de `DefaultMetricsEngine` (Enfoque) migran a esta tabla como **tarea deliberada de calibración** con actualización de goldens en el mismo cambio — nunca como efecto colateral.

## 12. Anti-Gaming Gate

- **Framework**: `core/antigaming` — 11 detectores puros D1–D11 + `AntiGamingFramework.aggregate` (acción más severa gana). Telemetría con defaults limpios (el usuario vacío no dispara nada).
- **Acciones**: `WITHHOLD_PROFILE` (la sesión no alimenta el perfil) e `INVALIDATE` (sesión descartada). Nunca se emite copy acusatorio ni se escribe un score bajo: `SUSPECTED_GAMING` renderiza como "calibrando".
- **Regla #8 — safety exit JAMÁS penaliza**: la salida emocional/de frustración produce `idle`/`ABANDONED_SAFE`, exenta por diseño y probada explícitamente.
- **Cobertura**: `DetectorCoverage.ADEQUATE` solo con suite red-team (par adversario-dispara / honesto-no-dispara); `PARTIAL` es insuficiente para métricas `RT_CRITICAL` (gate de la state machine). Detector de fast-guessing de Velocidad (A8): suelo fisiológico de RT (SRT ~140–180 ms; CRT mayor), banda de precisión-azar por `chanceAccuracy`, función de precisión condicional (CAF), guard de muestra mínima (≥12), validado adversarialmente para PARTIAL→ADEQUATE.
- **Wiring a producción**: prioridad Lógica y Decisiones (hoy sin invocación), luego el resto. Un detector no cableado NO cuenta como cobertura.

## 13. Score Visibility Rules

**Display gate A1** — un entero 0–100 solo se muestra si:

```
ESTABLISHED
  ∨ (EMERGING ∧ confidence ≥ 0.55 ∧ ≥2 bloques/sesiones agregados cross-session)
```

Por debajo: **banda cualitativa ordinal** ("En desarrollo", "Consolidándose"…) + etiqueta "provisional" — nunca un entero. `CALIBRATING` → **"—" (em-dash), NUNCA "0"**.

Copy por estado (no acusatorio, no clínico, pasa el copy-scanner):

| Estado | UI |
|---|---|
| CALIBRATING | "Calibrando — X de 5 sesiones" |
| INSUFFICIENT_N | "Faltan datos para una lectura fiable" |
| DEVICE_UNRELIABLE | "Tu dispositivo no permite medir esta métrica con la precisión necesaria" |
| BANK_CONTAMINATED | "Contenido en validación" |
| SUSPECTED_GAMING | "Calibrando" (neutro) |
| ABANDONED_SAFE | sin mensaje de score; nunca culpa |
| Inactividad >30 días | "Requiere sesiones recientes" (§16) |

Cada dimensión en "—" lleva una línea explicativa (por qué: calibrando / no medible aún por seguridad de medida), no un guion mudo. Prohibido todo copy comparativo poblacional (test de invariante en el UI-builder + lexicon del scanner).

## 14. Provisional vs Official Scores

- **Provisional**: EMERGING que pasa el display gate → entero con badge "provisional" + banda de confianza (de `Sampling.confidenceInterval`); también todo score mapeado por el fallback lineal (§7) y todo `preliminary=true` (flag `PRELIMINARY_LABEL_REQUIRED`).
- **Oficial**: ESTABLISHED → score estable; (V1.1) habilita rango. El paso provisional→oficial es exactamente el criterio compuesto de §9 — no existe una tercera vía.
- La evaluación inicial produce solo bandas provisionales con confidence explícitamente baja; jamás un entero ni EMERGING (A17).

## 15. Ranks

**Diseño completo, implementación V1.1 (A3). Fuera del MVP.**

- `RankLadder`: los 28 rangos PRD §14.12 (Bronce III → Leyenda — World Class) como umbrales FIJOS y versionados sobre `BlockScore` 0–100, estrictamente monótonos.
- **Gates**: rango de bloque solo existe/se muestra con el bloque ESTABLISHED; rango global exige ≥5/8 bloques calibrados. EMERGING jamás habilita rango.
- **Semántica honesta**: niveles de progresión INTRA-persona (dificultad alcanzada + estabilidad), no escalera competitiva. La comparabilidad entre usuarios exige normas propias + backend → V2; hasta entonces el copy no puede implicar comparación (precedente FTC/Lumosity).
- Umbrales ajustables solo por nueva versión del ladder (§17); el rango es DERIVADO del BlockScore almacenado, nunca autoridad persistida propia (§18).
- MVP muestra en su lugar: estado por bloque (Provisional/Calibrado) + score 0–100 al calibrar + progreso contra la propia línea base + racha/consistencia (sustitutos de XP, D6).

## 16. Inactivity

**>30 días sin sesión válida en un bloque** → el bloque entra en **recalibración**:

- El score se marca **"requiere sesiones recientes"** y se desenfatiza/oculta; NO se resta puntuación ni rango arbitrario; el historial queda intacto. Es honestidad de medición (el estado actual es incierto), no castigo.
- **1 sesión válida nueva** restaura la visibilidad; su entrada al EMA pasa el filtro MAD + clamp normales (una vuelta atípica no distorsiona).
- V1.1: el rango entra en estado "recalibrando" y se restaura en 1–2 sesiones; jamás se borra progreso.

## 17. Versioning

- **`ScoringVersionRegistry`** (objeto central en `core/scoring/`): versión activa de (a) `BlockScoreEngine` (pesos por bloque), (b) `UserScoreMapper` (k, z0, σ_min por bloque; lineal-fallback), (c) cada adapter de bloque, (d) tabla nMin, (e) `RankLadder` (V1.1). Cada entrada registra fecha, motivo, impacto y regla de migración (PRD §14.8). Unifica los `SCORING_VERSION`/strings libres dispersos hoy.
- **Toda sesión persiste su versión**: `scoringVersion` + `itemBankVersion` vía `ScoreProvenance` (campos ya existentes) y el `SessionRecord` transversal añade `exerciseVersion`/`bankVersion`/`schemaVersion` (D11).
- Cambiar una fórmula = nueva versión en el registro + goldens nuevos; los goldens de la versión anterior se conservan como tests de la versión histórica.

## 18. Migration

- **Los scores históricos NO se re-derivan** (coste local, riesgo de reescritura silenciosa): cada score persistido conserva la versión que lo produjo y se lee tal cual.
- **Nunca mezclar versiones en un mismo EMA**: al activar una versión nueva de fórmula/mapping, el EMA del bloque se reinicia con `average(last 5 valid)` computado BAJO LA NUEVA versión (recalibración explícita, comunicada como tal); el histórico previo queda read-only para tendencias, anotado con su versión.
- **Compatibilidad con datos antiguos**: se conservan los namespaces existentes de `SecureKeyValueStore`; todos los DTOs se leen con `Json { ignoreUnknownKeys = true; encodeDefaults = true }` + `schemaVersion`; un blob de esquema desconocido/superior se cuarentena (patrón `SecureKeyValueStore`), nunca crashea ni se interpreta como 0.
- Cambio de umbrales de rango (V1.1): el rango se recomputa en display desde el `BlockScore` almacenado (derivado, §15) — sin migración de datos.

## 19. Block-Specific Scoring

**Pesos de los 6 componentes por bloque** (Σ = 1.00; default PRD §14.2 = 0.30/0.20/0.20/0.15/0.10/0.05; versionados en el registry):

| Bloque | Precisión | Velocidad | Dificultad | Consistencia | Progreso | Control errores |
|---|---|---|---|---|---|---|
| Enfoque | 0.30 | 0.10 | 0.15 | **0.30** | 0.10 | 0.05 |
| Decisiones | **0.40** (calidad/Brier) | 0.15 (controlada) | 0.15 | 0.15 | 0.10 | 0.05 |
| Lógica | **0.40** | 0.05 | **0.30** | 0.10 | 0.10 | 0.05 |
| Creatividad | — sin score 0–100 en MVP (proceso, no nota) | | | | | |
| Lenguaje | **0.40** (semántica) | 0.15 (verbal, secundaria) | 0.15 | 0.15 | 0.10 | 0.05 |
| Emociones | — `normalizedScore = null` en MVP | | | | | |
| Velocidad mental | 0.25 | **0.35** (gated) | 0.10 | 0.15 | 0.10 | 0.05 |
| Memoria | **0.35** | 0.05 | **0.30** (span) | 0.15 | 0.10 | 0.05 |

### Enfoque

Motor: `FocusScore`/`DefaultMetricsEngine` (el más maduro; ya usa MAD) — se congela con goldens y se adapta vía `FocusScoreAdapter → ScoreResult` (dimensión ATTENTION). Dominan consistencia+precisión (§14.7). Las métricas de diferencia (CONGRUENCY/SWITCH_COST/DUAL_TASK/POST_ERROR) son SOLO secundarias: nunca `primarySignal`, gate split-half ≥0.6, ≥20/celda. E9 usa d′ con corrección log-lineal (≥20 señal + ≥20 ruido) para no premiar sesgo de respuesta. Migración MIN_N 3..5 → MinSampleGate: tarea deliberada con goldens. Los probes reservados (`BaselineProtocol`/ProbeBank) son el patrón canónico de la métrica de 30 días: se extiende a los 8 bloques.

### Decisiones

Se puntúa **calidad de proceso, nunca outcome**: calibración probabilística vía **regla de scoring propia (Brier)** — `calidad = clamp01(1 − Brier/0.25)` (0.25 = pronóstico azar), con `nMin = 40` decisiones (MinSampleGate.BRIER). **Elección dominada = error** (entra en control de errores, A10-D4). Velocidad "controlada": solo en ejercicios con deadline/coste asimétrico (D5), siempre bajo el gate de precisión. Reversibility Triage se activa tras validación inter-rater (κ≥0.75, ≥2 raters) → `BankIntegrity.PROCEDURAL` (D14); Bayes Arena entra al perfil vía adapter a `ScoreResult`, nunca por su ruta legacy. El Check-In es "calibración": covariable de estado, jamás puntúa ni cuenta para el mínimo.

### Logica

`ReasoningScorer` produce ICR 0–100: el adapter **normaliza /100 a 0..1** antes de `primarySignal` (nunca doble escala). Dominan precisión+dificultad; la velocidad solo puede RESTAR vía penalización de impulsividad, jamás sumar. **Bancos**: generación paramétrica `structureId×surfaceVariant`, ventana de no-repetición de `structureId`, holdout reservado para retest (disjunto del entrenamiento — test de disjunción), ítems versionados (`bankVersion`) registrando p-value/discriminación desde el día 1. El banco actual de 6 ítems es `SMALL_UNVALIDATED` → `BANK_CONTAMINATED` bloquea el perfil (estado honesto) hasta banco validado por `BankValidator` + registro en `BankValidationGateTest`. La dificultad calculada DEBE aplicarse al contenido (hoy cosmética). Anti-memorización: RT colapsado + patrón de respuesta memorizada → baja confidence o invalida.

### Creatividad

**Sin score 0–100 absoluto en MVP** (D12/A7): se muestran MÉTRICAS OBSERVABLES DE PROCESO — fluidez (conteo de válidas, única estimable con fiabilidad aceptable a corto plazo), flexibilidad, variedad; "originalidad aproximada" solo con método local reproducible y `UncertaintyLabel.HIGH`. Copy del hub: "retroalimentación funcional, sin nota ni rango" (contradicción copy↔scoring corregida). `CreativityProfileSource` se diseña pero se difiere a V1.1: la dimensión CREATIVITY = "—" en MVP (honesto). Si en V1.1 contribuye: cap `ClaimLevel.PRACTICE_SIGNAL`, prompts rotados, gate de validez de rúbrica contra ideas triviales (la fluidez cruda es gameable). Jamás claims de "creatividad medida".

### Lenguaje

**Precisión semántica por rúbricas offline deterministas** (claves de corrección/conjuntos esperados versionados con el banco) — **sin LLM** en el camino de scoring. Velocidad verbal SECUNDARIA y siempre bajo el gate de precisión. Prerrequisito P1: cablear persistencia — sin store, VERBAL permanece CALIBRATING (correcto: no fabricar). `LanguageProfileSource` ya modela reliability continua con saturación a 10 sesiones: mapear reliability ≥0.6 → EMERGING; ≥0.8 + 5 sesiones/3 días → ESTABLISHED; su down-weighting por practice-effect es el patrón a replicar en los demás bloques. Claim intra-tarea (análisis textual dentro de MindBoost), nunca "pensamiento crítico" general.

### Emociones

**`normalizedScore = null` en MVP by design** (A8): EMOTIONAL_REGULATION = "—" hasta holdout/baseline (Ola4). **Prohibido derivar un 0–100 de tier o dataSufficiency** (madurez de datos ≠ competencia) — canario de regresión obligatorio. Componentes diseñados para el futuro: **consistencia** (acuerdo en ítems repetidos intra/inter-sesión — la señal más fiable), **interpretación** (accuracy sobre banco validado), **recuperación** (BUR, ≥12 eventos golden). La supresión NO es regulación: "no responder/no sentir" jamás se premia. Abandono → `ABANDONED_SAFE`; seguridad precede a rendimiento; fatiga solo adapta.

### Velocidad mental

**Doble gate irrenunciable** (A8): (1) timebase autoritativo (`CHOREOGRAPHER_FRAME` + `NANOS_NATIVE`, Android API≥34) — fallback desktop/iOS jugable pero excluido del perfil; (2) detector de fast-guessing calibrado (§12) para `PARTIAL→ADEQUATE`; hasta entonces PROCESSING_SPEED permanece CALIBRATING y la UI expone la "elegibilidad de medición" del dispositivo (nunca se relaja el gate). Fórmula: RT mediano vs propia línea base con `effectiveSpeed = speed × gate(acc)` (0 si acc<0.80, rampa 0.80→0.95); **CV robusto >0.30 actúa como techo**: la sesión se marca `MetricStability.UNSTABLE` → el veredicto no es VALID → no puede subir el block score. Respuesta tardía siempre incorrecta; RT-mediana es la métrica más fiable del producto (20–30 trials) — por eso el rigor del timebase.

### Memoria

Precisión + dificultad/span dominan; accuracy-based (`RT_IRRELEVANT` — el RT jamás entra en la señal). Span complejo (EX1): gate de procesamiento ≥0.85 → si falla, INVALIDATED; componente dificultad = span alcanzado; span estable exige ≥2 administraciones + banda SEM (no el máximo de una sesión); RCI para cambio. EX2: Cowan's K (nMin 20) como primaria. Reconocimiento/pares: gate de discriminación FAR ≤0.34 ("seleccionar todo" → INVALIDATED) — se conserva. **Recuerdo diferido (EX3): ventana 20–48 h** confirmada por la notificación local (A13); **la expiración silenciosa de la ventana NO puntúa como fallo** — el ítem expira sin señal (NothingDue), jamás un miss; el intervalo real se registra como covariable de retención. EPISODIC_MEMORY es dimensión separada; para el bloque usuario se agrega 0.7 WM / 0.3 episódica (D5).

## 20. Tests

Todos en `:shared:desktopTest` (∈ `check`, gate de build). Obligatorios:

1. **Golden por fórmula versionada**: `BlockScoreEngine` (Σ pesos=1.0 por bloque; input→output congelados por versión); `UserScoreMapper` (logística y fallback lineal); EMA 0.85/0.15 con casos conocidos; los goldens antiguos se preservan por versión.
2. **Property tests**: monotonía (más normalized ⇒ más userScore, para logística y lineal — mismo orden); rango [0,100]; estabilidad ante perturbaciones pequeñas; σ_base degenerado no produce NaN/∞.
3. **Invariante CALIBRATING → nunca número**: ninguna ruta (UI-builder, mapper, aggregator) produce un entero ni un 0 fabricado desde un estado no displayable; display gate: EMERGING con confidence<0.55 o 1 bloque → banda, no entero.
4. **VelocityGate**: acc<0.80 → effectiveSpeed=0; rampa 0.80–0.95; CV>0.30 → UNSTABLE → la sesión no sube el score.
5. **EMA/volatilidad**: clamp |Δ|≤8 bloquea salto por sesión atípica; MAD pre-EMA filtra el outlier (persistido pero sin efecto); sesión sin ejercicios VALID no mueve nada.
6. **CalibrationPolicy — matriz golden** (trials×sesiones×días×estabilidad×anti-gaming) → estado; adversariales: 5 sesiones mismo día NO establece; 20 trials/1 sesión ≤ EMERGING.
7. **Adversariales por detector D1–D11**: cada adversario dispara el suyo; el honesto realista no dispara ninguno; safety-exit exento; fast-guess a chance → SUSPECTED_GAMING (prerequisito para reclamar ADEQUATE); "seleccionar todo"/memorización → no-VALID/`BANK_CONTAMINATED` → nunca userScore.
8. **Canarios de honestidad por bloque**: Emociones `normalizedScore` null para todo tier/dataSufficiency; Creatividad sin número en MVP; holdout de Lógica disjunto del entrenamiento; expiración silenciosa de EX3 → NothingDue, no fallo.
9. **Versionado/migración**: toda sesión persiste `scoringVersion`; cambio de versión reinicia EMA (recalibración) sin corromper histórico; ninguna mezcla de versiones en un mismo EMA; DTOs antiguos legibles (`ignoreUnknownKeys`), esquema desconocido → cuarentena.
10. **Global/rangos**: media simple SOLO de calibrados (CALIBRATING excluido, no 0); (V1.1) `RankLadder` monótono, rango solo ESTABLISHED, global ≥5/8; inactividad 30 d → "requiere sesiones recientes" con historial intacto.
11. **Paridad de confidence**: mismos datos longitudinales → misma confidence en todos los adapters; 0.7 solo alcanzable con ≥5 sesiones/≥3 días.

## 21. Known Limitations

1. **Anclaje logístico pendiente de datos**: hasta tener la curva de baseline por bloque, el fallback lineal opera marcado provisional; k/z0 requieren validación por simulación JVM antes de congelarse.
2. **Sin normas poblacionales**: cero comparabilidad inter-usuario; los rangos (V1.1) son progresión intra-persona. Comparabilidad real → V2 (normas propias + backend).
3. **Anti-gaming client-side**: sin validación server-side (V2), un cliente modificado puede falsificar señal local. Mitigación parcial: gates deterministas + provenance versionada.
4. **Emociones y Creatividad sin número en MVP** (hueco honesto y deliberado): dos dimensiones del radar en "—" hasta holdout/baseline y scorer prudente validado.
5. **Velocidad limitada por plataforma**: perfil solo con Android API≥34 (timebase autoritativo); iOS/desktop entrenan sin lectura — perfil asimétrico por dispositivo, comunicado en UI.
6. **Difference-scores intrínsecamente poco fiables**: nunca sostienen un rango; solo secundarias (limitación psicométrica estructural, no de implementación).
7. **Practice effects**: la exclusión de la sesión 1 y los probes+RCI mitigan pero no eliminan la inflación por familiaridad; la métrica de 30 días reporta fracción de usuarios con cambio FIABLE, no delta bruto.
8. **120 s heterogéneos entre bloques**: en Lógica/Decisiones/Creatividad una sesión válida aporta pocos ítems; la calibración de bloques lentos tardará más sesiones que el mínimo de 5.
9. **Deuda de wiring previa**: fórmula canónica de confidence exige migrar los 5 ProfileSources con `timestampMs=0L` (A14) y los adapters ad-hoc; el criterio de sesiones es inverificable hasta que `SessionRecord` (A12) persista abandonos en todos los bloques.
10. **Riesgo de retención**: el criterio compuesto es estricto; mitigación: banda provisional desde 2 sesiones + progreso "X de 5" siempre visible — nunca relajar los gates como respuesta a métricas de retención.
