# 8. Sistema de metricas definitivo

# SECCIÓN 8 — Sistema de métricas definitivo

> Lente: psicometría y medición. Todo lo que sigue está anclado a código real leído (`shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/**`) y a las auditorías por módulo. No repito sus hallazgos: los uso como insumo y decido la capa de medición.

---

## 8.0 Veredicto de la capa de medición (antes de nada)

El repo tiene **dos capas de medición que no se hablan**:

1. **La capa autoritativa y correcta**, escrita hace poco, que sabe hacer psicometría: `focus2/domain/scoring/DescriptiveScorer.kt` (d′ + criterio por condición, corrección loglineal de Hautus incondicional, error estándar de d′, veredicto por límite inferior del IC95, ausencia tipada `Descriptive.Absent`, escalera de veredicto ordenada) y `core/scoring/MinSampleGate.kt` (tabla golden de n mínimos: d′ 20/celda, SSRT 30, Brier 40, comparativos 20 con TODAS las celdas).
2. **La capa viva**, que es la que produce los números que el usuario ve: `focus/domain/metrics/DefaultMetricsEngine.kt`, `speed/domain/metrics/MetricCalculator.kt` + `score/SpeedScoreEngine.kt`, `memory/domain/metrics/MemoryMetricsEngine.kt`, `emotions/domain/metrics/DefaultEmotionMetricsEngine.kt`, `logic/domain/scoring/ReasoningScorer.kt`, `creativity/domain/scoring/CreativityRubricScorer.kt`.

**La capa viva no consume la autoridad.** `MinSampleGate.kt:38-40` lo confiesa en su propio KDoc: *"replaces the dispersed MIN_N=3..5 of DefaultMetricsEngine (migrating the live engine to consume this is a deliberate calibration change tracked separately)"*. Resultado operativo: `DefaultMetricsEngine.kt:493-499` marca `confident = true` con `MIN_N_CONGRUENCY = 3` por celda para el efecto de congruencia, mientras la autoridad del mismo repo exige **20 por celda y todas las celdas presentes**. El sistema declara confianza donde su propia norma dice que no hay medida. [Mejora crítica]

Segundo problema estructural: **`RT_CV` no significa lo mismo en dos módulos**. Enfoque usa `SD/mediana` (`DefaultMetricsEngine.kt:65-70`, explícitamente comentado "NOT SD/mean"); Velocidad usa `SD/media` (`MetricEdgeCasePolicy.kt:53-56`). Son dos estimadores distintos con la misma etiqueta. Cualquier comparación entre módulos, cualquier compuesto que los mezcle y cualquier serie longitudinal que cruce ejercicios es inválida por construcción. [Riesgo científico]

Tercer problema: **la honestidad está implementada como silencio permanente**. `ProbeReliabilityEstimator.kt:17` fija `ReliabilitySource.EMPTY` como default de producción ⇒ `EvaluationResult.hasReliableChange` **nunca puede ser true**. Es correcto no mentir, pero significa que hoy el producto **no puede demostrar ninguna mejora a nadie, nunca**, con la arquitectura actual. Eso no es honestidad, es ausencia de instrumento. §8.5 propone la salida (SEM intra-individual, N-of-1), que no requiere estudio poblacional.

---

## 8.1 (a) Métricas internas por capacidad — definición operativa

Regla de forma para TODAS: cada métrica es una tupla `(valor, n, celdas, unidad, versión_scorer, ausencia_tipada)`. `Ausente ≠ 0`. Ningún valor entra en un compuesto sin su `n` y sin su celda mínima. Se declara **numerador / denominador / filtro / n mínimo / regla de ausencia**.

### Núcleo transversal (obligatorio en los 8 módulos)

| Métrica | Definición operativa | n mín | Ausencia |
|---|---|---|---|
| `d′` (sensibilidad) | `z(H) − z(F)` con corrección loglineal incondicional (+0.5 conteo, +1.0 total), H = hits/ensayos-señal, F = FA/ensayos-ruido, **por condición** | 20/celda, ambas celdas | `NO_SIGNAL_TRIALS` / `NO_NOISE_TRIALS` / `INSUFFICIENT_N` |
| `SE(d′)` + IC95 | `√[H(1−H)/(Ns·φ(zH)²) + F(1−F)/(Nn·φ(zF)²)]`; se afirma discriminación sólo si `d′ − 1.96·SE > 0` | igual que d′ | igual |
| `c` (criterio) | `−0.5·[z(H) + z(F)]` | igual | igual |
| `acc_corr` (exactitud corregida por azar) | `(p − p_azar)/(1 − p_azar)`; **si `p ≤ p_azar` ⇒ ausencia tipada `AT_OR_BELOW_CHANCE`, jamás 0.0 clampado**; si el paradigma no tiene azar definido ⇒ `NOT_APPLICABLE` | 20 puntuables | tipada |
| `RT_mediana` | mediana de RT de aciertos, tras filtro de anticipación y rechazo MAD (z>3, mínimo 4 valores) | 20 | `INSUFFICIENT_N` |
| `RT_CV` | **`SD/mediana`, un solo estimador en todo el producto**, nombre `RT_CV_MEDIAN` para hacer explícito el denominador | 20 | `INSUFFICIENT_N` |
| `tasa_lapsos` | `#(RT > umbral_individual) / #RT_crudos_de_acierto`; **sobre crudos, NO sobre MAD-filtrados** (ya correcto: `DefaultMetricsEngine.kt:72-79`, `MetricEdgeCasePolicy.kt:90-95`) | 20 | 0.0 sólo si hay ≥20 RT y ninguno supera el umbral |
| `tasa_anticipación` | `#(RT < piso) / #ensayos`; piso **individualizado** por percentil 1 del propio historial, no un literal | 20 | tipada |
| `veredicto_sesión` | escalera ordenada: `InsufficientData` → `Invalidated(DEGENERATE_RESPONDING)` (una sola respuesta distinta entre puntuables) → `Invalidated(NO_DISCRIMINATION)` (ningún IC95 supera 0) → `Valid` | — | — |

Esta tabla **ya existe implementada y verificada adversarialmente** en `focus2/domain/scoring/DescriptiveScorer.kt:80-213`. La decisión correcta no es reescribirla ocho veces: es **promoverla a `core/scoring/` y hacer que los ocho módulos la consuman** con su propio `TaskSpec` (condiciones, `chanceLevel`, mapeo respuesta→sí/no). Es el mayor retorno de toda la sección: un solo estimador auditado, ocho consumidores.

### Por capacidad

| Capacidad | Métrica principal (definición operativa) | Secundarias obligatorias | Estado hoy |
|---|---|---|---|
| **attention (vigilancia)** | `d′` sobre objetivos escasos (20 señal / 80 ruido mín.) + `pendiente de decremento` = pendiente OLS de RT vs índice de ensayo, en ms/ensayo, ≥6 puntos | `RT_CV_MEDIAN`, lapsos, criterio `c` (deriva del sesgo, no de la sensibilidad) | Existe en focus2; en `focus` v1 se sustituye por `SUSTAINED_FOCUS_INDEX` (inventado) |
| **attention (control de interferencia)** | `efecto_congruencia` = `mediana(RT_incong) − mediana(RT_cong)` sobre aciertos, **20/celda y ambas celdas** | exactitud por congruencia (separada, nunca agregada), **adaptación de conflicto (Gratton)**: congruencia condicionada al ensayo previo — hoy ausente y es la métrica de oro del paradigma | `DefaultMetricsEngine.kt:364-376` correcto en forma, roto en n (3/celda) |
| **attention (inhibición)** | `SSRT` por método de integración (Verbruggen) con `p(respond\|signal)` reportado | `p(respond\|signal)` ∈ [0.25,0.75] como control de calidad; enlentecimiento proactivo (RT-go bloques con vs sin stop) | `SsrtCalculator.kt:39-53` **correcto**; contaminado por incluir el burn-in de la escalera en `meanSsd` (`DefaultMetricsEngine.kt:388-389` promedia TODOS los SSD) |
| **attention (flexibilidad)** | `switch_cost` = `mediana(RT_switch) − mediana(RT_repeat)` sobre aciertos | `mixing_cost` (bloques mixtos vs puros) — ausente; asimetría de coste | Forma correcta (`:288-300`), n insuficiente, banco de 8 estados memorizable |
| **working_memory** | `span_PCU` = media, sobre listas, de (ítems en posición correcta / longitud de lista), **con recall por producción u orden**; y `span_umbral` = longitud con 50% de listas perfectas por escalera 1-up/1-down | exactitud y RT de la tarea de procesamiento; **gate de validez: descartar el trial si exactitud de procesamiento < 0.85**; curva de posición serial; errores de orden vs de ítem vs intrusiones | `MemoryScorer.scoreComplexSpan` (`MemoryScoring.kt:71-91`) está **bien diseñado** y es **inalcanzable** desde el runtime; lo que se emite es `hits` crudo |
| **working_memory (dual)** | `dual_task_cost` = `(acc_single − acc_dual)/acc_single`, exige **dos respuestas puntuadas** | coste en RT, no sólo en exactitud | Emitido sin segunda tarea real ⇒ no medible |
| **processing_speed** | `RT_mediana_decisión` = `RT_tarea − RT_simple(E1)` del mismo usuario y sesión (aísla latencia de decisión de latencia motora) | `RT_CV_MEDIAN`, throughput correcto/s, `tasa_anticipación`, exactitud **separada** de tardanza (nunca colapsadas en `pc`) | `MetricCalculator` no resta SRT ni segrega condiciones; `pc` de E1 es 1.0 por construcción |
| **logical_reasoning** | `precisión_inferencial_no_vista` = aciertos sobre ítems isomorfos **nunca presentados**, con ≥5 pares por estructura | tasa de sobreinferencia por tipo de distractor; consistencia premisa-correcta ↔ respuesta-correcta (detecta adivinación); d′ en detección de inconsistencia (requiere ítems consistentes E inconsistentes) | `ReasoningScorer` produce `totalIcr` agregando 6 dimensiones heterogéneas ⇒ no interpretable |
| **verbal_reasoning** | `%` de ítems con opción correcta **Y** anclaje de evidencia exacto (F1 sobre spans, penaliza sobre-marcado) en **forma paralela no vista** | latencia (hoy no se captura: `ScoringEngine.kt:54-57` la declara opcional y nadie la rellena); tasa de error por patrón (`INF_OVER`, `SUM_DISTORT`…); recuperación tras error del mismo patrón | Sin latencia y sin material nuevo, el módulo mide familiaridad con 6 ítems |
| **decision_making** | `error_absoluto_en_logit` = `abs(logit(posterior_usuario) − logit(posterior_normativo))` — **nunca en espacio lineal de probabilidad** | `índice_de_negligencia_de_tasa_base` = correlación entre el error y `abs(base_rate − 0.5)`; dirección sistemática del sesgo; d′ reversible/irreversible sobre banco con features reales | Hoy `meanAbsoluteError` lineal: un error de 0.05 en 0.5 pesa igual que en 0.99. Falso |
| **emotional_regulation_for_performance** | `d′` afectivo (no comisión cruda) + `robustez` = `acc_bajo_presión / acc_en_calma` | coste de interferencia afectiva con ≥20/celda; `RT_CV` de ensayos go; post-error accuracy adjustment con ≥6 pares | `COMMISSION_RATE` cruda confunde sesgo de respuesta con inhibición (`DefaultEmotionMetricsEngine.kt:28-82`) |
| **creativity** | `originalidad_intrapersonal` = proporción de ideas ausentes en las últimas N sesiones del propio usuario (banco de frecuencia local) | `flexibilidad` = categorías canónicas distintas / ideas; `switch_rate` = pares consecutivos que cambian de categoría; curva de producción por ventanas de 30 s | `CreativityRubricScorer.kt:32-38` mide `fluency` (conteo) y `elaboration` (palabras/idea) — ninguna es originalidad, y `elaboration` premia verbosidad |
| **metacognition** | ver §8.3 | — | Prácticamente inexistente |

**Nota sobre `learning_strategy` y `spatial_reasoning`:** no hay hoy ninguna métrica que las mida. `ARC_CLUSTERING` sería el instrumento natural de estrategia de codificación pero es artefactual (§8.4). No inventar una métrica para llenar el radar: dejarlas en ausencia tipada. [Riesgo científico si se rellenan]

---

## 8.2 (b) Métricas de transferencia

La transferencia es lo único que separa este producto de un juego. Hoy **no se mide en ninguna parte del producto vivo**. Tres niveles, con reglas distintas:

### T1 — Transferencia cercana (misma capacidad, material no entrenado)

**Definición operativa:** `Δ_untrained` = cambio en la métrica principal de la capacidad, medido sobre **ítems del conjunto reservado que el entrenamiento nunca tocó**.

- La partición ya existe y es correcta: `TransferProbePartition.kt:27-38` valida disjunción en `init` y es determinista por semilla (un ítem reservado nunca vuelve a entrenamiento).
- **[Mejora crítica] El tamaño de la reserva es estructuralmente insuficiente.** Ratio 0.20 (`TransferProbePartition.kt:52-53`) sobre ~142 palabras y 56 pares ⇒ **~28 palabras y ~11 pares reservados en total**. Una sola administración de probe con d′ exige ~20 ensayos por celda. Con 28 palabras alcanza para **una** administración, y el diseño longitudinal exige un mínimo de 3 (baseline, medio, post) más re-tests. En la segunda administración el "probe no entrenado" ya está entrenado por su propia administración anterior. **La reserva actual no soporta el diseño que existe para soportar.** [Dato necesario: tamaño mínimo de banco = `k_administraciones × n_por_celda × n_celdas / (1 − solapamiento)`; con 4 administraciones y 20/celda × 2 celdas ⇒ ≥160 ítems reservados ⇒ banco total ≥800 palabras. Objetivo declarado en el propio módulo: >2000.]

### T2 — Transferencia estructural (misma estructura, superficie distinta)

**Definición operativa:** `transfer_gap` = `acc(ítem_entrenado) − acc(isomorfo_no_entrenado)`, con **separación temporal ≥1 sesión** (nunca inmediato) y **≥5 pares isomorfos por estructura**.

- Un `transfer_gap` grande = aprendizaje superficial (memorización del ítem). Un gap ≈0 con ambas exactitudes altas = aprendizaje de la estructura. Es el índice más informativo y el más barato de implementar.
- Hoy: `transferConsistencyScore` es **binario sobre n=1 e inmediato** ⇒ ruido puro, y además gobierna decisiones de dificultad. Eliminar (§8.4).

### T3 — Transferencia funcional (situación de la vida real)

**Definición operativa:** protocolo declarado que mide desempeño en una tarea instrumental, no en el ejercicio.

- `TransferProtocols` (`memory/domain/transfer/TransferProtocol.kt:38-140`) declara 10 puentes con `bridge` + `measurement` explícitos. **Es el mejor artefacto conceptual del repo en este eje.** Sólo 2 tienen `v1Available = true`: `STUDY` (retención a 24h de material propio) y `VOCABULARY` (pares L1-L2 a 24h y 7 días).
- **Regla de honestidad:** T3 sólo se afirma con medida propia del protocolo (retención de material del usuario a 24h/7d), **nunca** derivada de la mejora en el ejercicio. Los 8 protocolos con `v1Available = false` no se muestran, ni siquiera como "próximamente medible".
- **Prohibido:** afirmar T3 desde T1 o T2. Ese es exactamente el claim de transferencia lejana no respaldada que el charter prohíbe.

### Regla de reporte de transferencia

```
mejora_en_tarea      = Δ sobre material entrenado          → NUNCA se llama "mejora"
transferencia_cercana= Δ sobre material reservado (T1)     → requiere RCI (§8.5)
transferencia_estruct= gap entrenado−isomorfo (T2)         → requiere ≥5 pares y 1 sesión de separación
transferencia_funcion= medida del protocolo (T3)           → sólo STUDY y VOCABULARY hoy
```

`RealImprovementCriterion.classify` (`memory/domain/evaluation/RealImprovementCriterion.kt:48-68`) ya codifica esta lógica correctamente y distingue `PRACTICE_EFFECT` de `GENUINE_IMPROVEMENT` exigiendo `untrainedDelta > 0.05` **Y** `effectSize ≥ 0.30` **Y** `sustainedWeeks ≥ 2`. **Está bien hecho y no tiene ningún productor de datos.** Es una función pura sin llamador en producción.

---

## 8.3 (c) Calibración metacognitiva

Estado real: **el producto no mide calibración en ninguna parte**, y donde cree medirla, mide otra cosa.

### Lo que hay y por qué no sirve

- `ReasoningScorer.kt:33-37`: `confidenceScore = if (isCorrect) p else 1 − p`. Esto es la **regla de puntuación lineal**, que **no es estrictamente propia**: su óptimo se alcanza reportando `p = 0` o `p = 1`, nunca la creencia sincera. Es decir, el sistema **incentiva activamente el reporte extremo de confianza** y luego penaliza el error confiado en `:56-58`. Se contradice a sí mismo. [Riesgo científico]
- `LogicErrorTag.OVERCONFIDENT_ERROR` con umbral literal `0.85` (`ReasoningScorer.kt:101`): heurística por ítem, no calibración. Un ítem no puede establecer calibración.
- Memoria: `confidenceBias = 0.0` fijo (documentado en la auditoría), `CALIBRATION_INDEX`/`BRIER_SCORE`/`GAMMA` declarados y no computados.
- Lenguaje: `ScoringTiming.confidence` (`ScoringEngine.kt:54-57`) es opcional y nadie lo rellena.
- Decisiones: el Bayes Arena **sí captura `confidence` junto a `absoluteError`** y no los cruza. Es el dato de calibración más limpio del repo, tirado.

### Sistema de calibración propuesto

Tres métricas, ninguna interpretable con menos de **40 juicios agregados** (`MinSampleGate.BRIER = 40`, ya en la tabla golden):

| Métrica | Definición operativa | Interpretación |
|---|---|---|
| **Brier** | `(1/N)·Σ(p_i − o_i)²`, `o ∈ {0,1}` | Regla **estrictamente propia**: el óptimo es reportar la creencia sincera. Sustituye al score lineal actual. Rango 0 (perfecto) a 1. |
| **Descomposición de Murphy** | `Brier = Fiabilidad − Resolución + Incertidumbre`, con `p` binado en ≥5 bins de ≥8 juicios | **Fiabilidad** = desviación de la calibración (bajo = bueno). **Resolución** = capacidad de discriminar (alto = bueno). Un usuario puede tener Brier mediocre por baja resolución (sabe poco) o por mala fiabilidad (sabe pero se cree otra cosa) — son intervenciones distintas. |
| **Sesgo de sobre/infra-confianza** | `mean(p) − mean(o)` | Signado. >0 sobreconfianza, <0 infraconfianza. Es lo único que se puede mostrar al usuario antes de tener 40 juicios, y sólo con ≥20 y con banda de incertidumbre. |

Complementaria y de alto valor funcional (la que de verdad se transfiere): **resolución metacognitiva sobre el propio estado**, ya sugerida en la auditoría de Decisiones — correlación entre fatiga autorreportada en el check-in y `RT_CV`/lapsos reales de esa sesión. Mide si el usuario sabe leer su propio estado, que es una habilidad accionable, medible sin banco de ítems y no memorizable.

**Reglas duras:**
- La confianza se captura **antes** del feedback y **por ítem**, con escala discreta (5 o 7 niveles mapeados a probabilidad), no continua libre.
- Nunca mostrar Brier crudo al usuario (no es interpretable). Mostrar fiabilidad/resolución como "tu confianza acierta / tu confianza distingue", con banda.
- Calibración **no** entra en ningún compuesto de capacidad. Es ortogonal: se puede ser muy capaz y estar mal calibrado.

---

## 8.4 (d) Métricas ACTUALES a ELIMINAR, con nombre y archivo

Criterio de eliminación: (i) no mide el constructo que su nombre afirma, (ii) es explotable trivialmente, (iii) es un compuesto inventado sin validación, o (iv) fabrica un valor desde ausencia de dato.

### Eliminación inmediata (mienten hoy, en producción)

| Métrica | Ubicación | Por qué se elimina |
|---|---|---|
| `SUSTAINED_FOCUS_INDEX` | `DefaultMetricsEngine.kt:440-461` | Compuesto inventado `100·(1−rtCv)·(1−lapseRate)`. Multiplicativo sin justificación, sin validación, sin unidad interpretable, y sensible a un `rtCv` que es `SD/mediana` (no un CV). Se presenta como índice de capacidad. **No sirve para inferir nada.** |
| `BLOCK_STABILITY` | `DefaultMetricsEngine.kt:419-430` | `(1 − rtCv).coerceIn(0,1)`. Con `SD/mediana`, `CV > 1` es común en vigilancia ⇒ satura en 0 para una fracción grande de usuarios ⇒ **efecto suelo**: deja de discriminar exactamente donde importa. Redundante con `RT_CV`. |
| `FATIGUE_INDEX` (Enfoque) | `DefaultMetricsEngine.kt:476-491` | Dos defectos: (1) `confident = true` **hardcodeado** en la línea 487 tras un guard de `cvs.size >= 2` ⇒ declara confianza con **2 bloques**; (2) `((raw+1)/2)` hace que "sin fatiga" sea 0.5, valor que ningún consumidor puede interpretar. Diferencia de dos medias de ≤3 valores = ruido. |
| `LISAS` | `DefaultMetricsEngine.kt:244-256` | No es LISAS. LISAS pondera por la **SD de la proporción de errores entre observaciones**; aquí `sdPe = √(PE(1−PE))` es la SD de Bernoulli **dentro** del bloque, así que la fórmula colapsa a `meanRT + sdRT·√(PE/(1−PE))` — una transformación monótona de PE, no un compuesto estandarizado, y **no acotada** cuando PE→1. Etiqueta de literatura sobre una fórmula distinta. [Riesgo científico] |
| `POST_ERROR_SLOWING` = 0.0 y `POST_ERROR_ACCURACY` = 0.0 por ausencia | `DefaultMetricsEngine.kt:194-212` | Un usuario **sin errores** obtiene `POST_ERROR_ACCURACY = 0.0`. Fabricar el mínimo desde ausencia de dato es la violación del charter en su dirección "castigadora". Debe ser ausencia tipada. |
| `metric(): value = if (isFinite) value else 0.0` | `DefaultMetricsEngine.kt:147` | Mapea silenciosamente no-finito a 0.0. `DescriptiveScorer.kt:152-153` lo señala explícitamente como el defecto que evita. Es la puerta trasera del `null ≠ 0`. |
| `MetricValue.unavailable(...)` con `value = 0.0` | `focus/domain/metrics/MetricKey.kt` (helper) | La propia representación de "ausente" codifica 0.0. Cualquier consumidor que lea `.value` sin mirar `confident` lee un cero fabricado — y existe `rawValueOf` que lo hace por diseño. `MetricValue.value` debe ser `Double?`. |
| `total` de `CognitiveProfileScore` | `SpeedScoreEngine.kt:94-99` | Tres defectos: (1) `control` es **siempre no-nulo** porque `impIdx ?: 0.0` trata "no computable" como "impulsividad cero" ⇒ `bars` nunca está vacío ⇒ **el total se calcula con una sola barra y se presenta como compuesto de tres**, incluso cuando la macro-categoría dice `CALIBRATING`; (2) la penalización `×0.5` es un **acantilado discontinuo** en `pc = 0.80` y `cv = 0.30` ⇒ un usuario en 0.799 puntúa la mitad que uno en 0.801, lo que destruye la comparabilidad longitudinal; (3) `usefulSpeed` se normaliza contra `usefulSpeedReferenceVce = 5.0`, constante arbitraria sin norma, con `coerceIn(0,1)` ⇒ **techo** para todo el que la supere. |
| `VCE` | `MetricEdgeCasePolicy.kt:119-126` | `pc²/(rt_s·(1+0.7·cv))`. `β = 2.0` y `λ = 0.7` son literales sin derivación ni calibración. Es una preferencia de diseño disfrazada de medida. Además alimenta `usefulSpeed` **y** `FatigueIndex` (abajo). |
| `fatIdx` (Velocidad) | `FatigueIndex.kt:25-34` | Suma `−Δ(VCE) + Δ(RT) + Δ(lapsos)`, pero **VCE es función de pc, RT y CV** ⇒ los tres sumandos están correlacionados por construcción ⇒ el índice **triplica la misma señal** y no tiene unidad ni distribución de referencia. Sobre 3 bloques, es ruido amplificado. |
| `pc` de E1 y todo lo derivado | `MetricCalculator.kt:140` vía mecánica de E1 | `pc` es 1.0 por construcción en E1 (no hay condición de error). La barra "control" y `VCE` en E1 no miden control: miden que la mecánica marca todo correcto. |
| `WORKING_MEMORY_SPAN` | `MemoryMetricsEngine.kt:77-86` | `value = trial.hits.toDouble()` con el comentario `// partial-credit unit score`. **El comentario contradice al código**: `hits` es un conteo crudo de reconocimientos, no PCU. Además `n = studiedCount` ⇒ `confident` con 4 ítems estudiados. No es un span. |
| `DELAYED_RECALL_ACCURACY` | `MemoryMetricsEngine.kt:41-44` | La clave se elige por `trial.delayMs > 0`, y `delayMs` viene **del plan**, no del tiempo transcurrido. Subir el eje DELAY del adaptativo **renombra** la métrica de inmediata a diferida sin que pase un segundo. Es una etiqueta de consolidación sobre recuerdo inmediato. [Riesgo científico grave] |
| `ARC_CLUSTERING` | `MemoryMetricsEngine.kt:109-128` | El orden que consume proviene del filtrado de `studyList`, es decir, **el orden de estudio**, no el de recuperación. Mide el orden en que el generador colocó la lista. Artefactual. |
| `ASSOCIATIVE_INDEX` y `CONJUNCTION_ERROR_RATE` | `MemoryMetricsEngine.kt:90-103` | Sin presentación del cue, el usuario nunca elige un target equivocado *dado un cue* ⇒ `CONJUNCTION_ERROR ≈ 0` **estructuralmente**, y el sistema reporta "resistencia perfecta a la confusión" como logro. El índice asociativo es reconocimiento de targets. |
| `CONSOLIDATION_INDEX` | `MemoryMetricsEngine.kt:130-141` | `delayed/immediate` con `coerceAtLeast(0.0)` y **sin cota superior**; con demora falsa (arriba) el ratio ≈1.0 se leerá como consolidación perfecta. |
| `averageIcr` | `ReasoningScorer.kt:42-52` | Suma ponderada de 6 dimensiones heterogéneas (respuesta, regla, premisas, justificación, confianza, independencia) en un escalar 0-100. **No es interpretable**: dos usuarios con el mismo 72 pueden tener perfiles opuestos, y una mejora en el escalar no identifica qué mejoró. Descomponer o eliminar. |
| `transferConsistencyScore` | `ReasoningScorer.kt:70` + holder | Binario sobre **n=1** e inmediato, y además **gobierna decisiones de dificultad**. Ruido con poder de decisión. |
| `confidenceCalibrationScore` | `ReasoningScorer.kt:33-37` | Regla de puntuación lineal ⇒ **no estrictamente propia** ⇒ incentiva reportar 0 o 1. Sustituir por Brier (§8.3). |
| `EMOTION_THOUGHT_SEPARATION` | `DefaultEmotionMetricsEngine.kt:128-134` | `(etAcc − 0.25)/0.75` calculado **sólo sobre 2 de las 4 categorías**, con `p_azar = 0.25` que corresponde a las 4. El denominador de azar no coincide con el conjunto medido ⇒ el índice está sesgado por construcción. |
| `GRANULARITY_INDEX` | `DefaultEmotionMetricsEngine.kt:140-145` | `categorías_distintas_acertadas / categorías_presentadas`. Es **cobertura**, no granularidad. Con ≥3 categorías presentadas y una respuesta correcta por categoría alcanza 1.0. |
| `ANTICIPATIONS` con piso fijo | `DefaultEmotionMetricsEngine.kt:239` (`IMPULSIVE_FLOOR_MS = 350.0`) | Umbral literal sin calibración por usuario. Un usuario lento por motricidad no produce anticipaciones nunca; uno rápido las produce siempre. Individualizar por percentil del propio historial. |
| `elaboration` | `CreativityRubricScorer.kt:34-38` | Palabras por idea. Premia verbosidad, no elaboración. Explotable escribiendo frases largas. |
| `fluency` como señal de creatividad | `CreativityRubricScorer.kt:32-33` | Conteo de ideas distintas ≥2 caracteres. Sin originalidad intrapersonal, la fluidez sola no distingue ideas nuevas de repetición del propio repertorio. Mantener sólo como covariable, nunca como señal principal. |
| `accuracy` de Reversibilidad, `lockViolations`, `meanDelayPenalty` | módulo decisiones | La etiqueta correcta se genera por moneda al aire y el ejercicio es explotable al ~100% vía el lock. **Ninguna métrica del ejercicio mide lo que dice.** Retirar el ejercicio del cómputo hasta que exista banco con features reales. |
| `meanAbsoluteError` lineal | módulo decisiones (Bayes) | Espacio de probabilidad no lineal: 0.05 cerca de 0.5 ≠ cerca de 0.99. Sustituir por error en logit. |
| Ejes "Integridad" y "Reto enfrentado" del polígono | pantalla de resultado de decisiones | Constantes. Un eje constante en un radar es decoración presentada como medida. |

### No eliminar, pero PROHIBIR mostrar hasta que exista el paradigma

`DUAL_TASK_COST` (sin segunda tarea puntuada), `ACCURACY` de E9/E10 de Enfoque (sin tracking ni doble respuesta reales), `SET_SIZE_SLOPE` sin separar present/absent, `DISTRACTION_RESISTANCE_INDEX` truncado en [0,1] cerca de 1.0. Todas son formas correctas alimentadas por runtimes que no ejecutan el paradigma. El defecto no es la fórmula: es el flujo de captura.

---

## 8.5 (e) Mejora longitudinal controlando el efecto práctica

### El problema, dicho sin adornos

Toda mejora observada sobre material entrenado es, por defecto, **efecto práctica** hasta que se demuestre lo contrario. El repo lo sabe (`RealImprovementCriterion`), lo tiene implementado y **no lo ejecuta**. Además tiene **dos implementaciones duplicadas e idénticas** del RCI Jacobson-Truax (`memory/domain/evaluation/ReliableChangeIndex.kt` y `focus/domain/evaluation/ReliableChangeIndex.kt`), ambas correctas y ambas sin datos.

### Por qué el RCI clásico no se puede alimentar aquí

Jacobson-Truax exige `SEdiff = √2 · SD_pre · √(1 − r_xx)`, donde `SD_pre` es la **desviación típica poblacional** en pre-test y `r_xx` la **fiabilidad test-retest poblacional**. Ninguna de las dos existe ni existirá sin un estudio normativo. Por eso `ReliabilitySource.EMPTY` (`ProbeReliabilityEstimator.kt:17`) es el default: la puerta está cerrada con llave y **la llave no está en el producto**.

### La salida correcta: RCI N-of-1 con SEM intra-individual

**No hace falta población.** Se estima el error de medida **dentro del propio usuario**, a partir de administraciones repetidas del probe reservado en una **fase de estabilización** (sin entrenamiento intercalado en esa capacidad).

```
Fase 0 (estabilización)   : k ≥ 3 administraciones del probe reservado, separadas ≥24h,
                            SIN entrenamiento de esa capacidad entre ellas.
SEM_within                = SD de las k administraciones de Fase 0  (error de medida propio)
Cambio fiable             = |post − media(Fase 0)| > 1.96 · √2 · SEM_within
Tamaño de efecto N-of-1   = (post − media(Fase 0)) / SEM_within
```

Propiedades: no requiere norma poblacional; es interpretable ("tu cambio supera tu propio ruido"); es honesto (si el usuario es muy variable, el listón sube para él, que es exactamente lo correcto); y encaja **sin cambiar la firma** de `ReliableChangeIndex.compute` — basta con un `ReliabilitySource` que produzca `ReliabilityEstimate(metric, reliability, sdPre)` desde el historial propio, donde `sdPre = SEM_within` y `reliability` se despeja para que `sdPre·√(1−r) = SEM_within` (es decir, alimentar el mismo seam con semántica intra-individual, documentada). **Coste: un productor. La maquinaria ya existe.** [Mejora crítica, mayor retorno del eje longitudinal]

Las administraciones de Fase 0 **también** son las primeras 3 que exige `ProbeReliabilityEstimator.MIN_ADMINISTRATIONS = 3`. El diseño ya estaba anticipado; falta el productor.

### Diseño de medición longitudinal completo

| Fase | Qué corre | Adapta | Qué produce |
|---|---|---|---|
| **B0 — Baseline** | `MemoryBaselineProtocols.INITIAL` (`MemoryBaselineProtocol.kt:67-99`): bloques de calibración a dificultad **fija** + **≥1 probe reservado**, con cuestionario de estado (sueño, ánimo, hora) | **NO** (`require(mode != TRAINING)`) | Punto pre + covariables de estado |
| **E — Estabilización** | 2 administraciones adicionales del probe reservado, ≥24h de separación, sin entrenar esa capacidad | NO | `SEM_within` |
| **T — Entrenamiento** | Sesiones adaptativas normales sobre material **de entrenamiento** (nunca reservado) | SÍ | Sólo "mejora en la tarea". **Prohibido llamarla mejora.** |
| **P — Probe periódico** | Probe reservado + isomorfos no entrenados, cada 2 semanas, dificultad fija | NO | `Δ_untrained`, `transfer_gap` |
| **F — Funcional** | Protocolo T3 (`STUDY` / `VOCABULARY`): retención a 24h y 7d de material propio del usuario | NO | Transferencia funcional |

### Reglas anti-práctica no negociables

1. **Formas paralelas obligatorias.** El probe de la administración *k* nunca comparte ítems con la *k−1*. Con la reserva actual (28 palabras / 11 pares) esto es **imposible** ⇒ el banco es el bloqueante duro del diseño longitudinal, no la estadística. [Dato necesario]
2. **La dificultad del probe no adapta.** Un instrumento que se mueve no mide cambio. Ya está bien impuesto en `MemoryBaselineBlock.init` (`:33-38`).
3. **Covariables de estado en cada administración** (sueño, hora, ánimo). Sin ellas, una mejora puede ser "durmió mejor". Ya declaradas en `MemoryStateQuestionnaire`.
4. **Efecto práctica se estima, no se asume nulo.** La pendiente de las k administraciones de Fase 0 **es** la curva de práctica del probe. El cambio post-entrenamiento se juzga contra la **extrapolación de esa pendiente**, no contra el primer punto. Si el probe mejora solo por repetirse, eso queda descontado. [Mejora crítica — es la única forma de separar práctica de capacidad con N=1]
5. **La clasificación se hace con `RealImprovementCriterion.classify`, sin tocarla.** Sus umbrales (`MIN_DELTA = 0.05`, `MIN_EFFECT_SIZE = 0.30`, `MIN_SUSTAINED_WEEKS = 2`) son defendibles y conservadores. Alimentarlos, no ajustarlos.
6. **`POOR_CONSOLIDATION` exige demora real.** Hoy es inalcanzable porque la demora es una etiqueta (§8.4). Sin demora real, retirar ese patrón de la clasificación en vez de dejarlo dormido.
7. **Ninguna afirmación de mejora sin (a) probe no entrenado, (b) cambio > 1.96·√2·SEM_within descontada la pendiente de práctica, (c) sostenido ≥2 semanas.** Los tres, conjuntamente. Uno solo no basta.

---

## 8.6 Qué está BIEN hecho y qué está ROTO (sin diplomacia)

### Bien hecho — conservar y propagar

1. **`focus2/domain/scoring/DescriptiveScorer.kt`** — es el mejor artefacto psicométrico del repo, con diferencia. d′ y criterio por condición; corrección loglineal de Hautus **incondicional** (con la justificación correcta escrita: las alternativas condicionales sesgan al alza precisamente a los mejores); error estándar de d′ con la fórmula estándar; veredicto exigiendo que el **límite inferior del IC95** supere cero; exactitud corregida por azar que devuelve **ausencia tipada** por debajo del azar en vez de clampar a 0; veredicto de respuesta degenerada derivado de **conteos crudos**, inmune al sesgo de cualquier corrección; escalera de veredicto **ordenada** para que "no pudimos medir" preceda a "no discriminó". Y consume `MinSampleGate` en vez de re-declarar el piso (`:47`). **Esto es el estándar; el resto del repo debe converger aquí.**
2. **`core/scoring/MinSampleGate.kt`** — tabla golden única, `InsufficientData` que nunca es un número, `requiresAllCells` para los comparativos. Correcta. Su problema es que nadie de la capa viva la llama.
3. **`memory/domain/evaluation/ReliableChangeIndex.kt`** — Jacobson-Truax implementado correctamente, con `CRITICAL_Z = 1.96` y gate `NotEnoughData` cuando la fiabilidad no es usable. **Sí, el RCI de memoria está bien hecho.** Lo que falta es el productor de `ReliabilityEstimate`, no el estimador.
4. **`RealImprovementCriterion`** — la lógica que distingue `PRACTICE_EFFECT` de `GENUINE_IMPROVEMENT` exigiendo delta en el **probe no entrenado** es exactamente el criterio correcto, y `isGenuine` añade tamaño de efecto y persistencia. Conceptualmente impecable.
5. **`TransferProbePartition`** — disjunción validada en `init`, determinista por semilla, exhaustiva. El mecanismo es correcto; el tamaño no.
6. **`MemoryBaselineProtocol`** — invariantes en `init` que impiden un baseline adaptativo o sin probe. Así se escribe un instrumento.
7. **Separación filtro-MAD vs conteo-crudo** (`DefaultMetricsEngine.kt:72-79`, `MetricEdgeCasePolicy.kt:84-95`) — lapsos y anticipaciones sobre crudos, tendencia central sobre filtrados. Correcto y no trivial.
8. **`SsrtCalculator`** — método de integración de Verbruggen implementado bien, con la inclusión correcta de los RT de stops fallidos como go-RT y el flag `trustworthy` sobre `p(respond|signal)` ∈ [0.25,0.75].
9. **`MetricEdgeCasePolicy`** — la tabla de bordes documentada, con `faRate = null` cuando no hay condición no-go (no 0.0) y `null` = "no computable" como política declarada. La disciplina es correcta aunque algunos consumidores la rompan.
10. **`MemoryScorer`** (`MemoryScoring.kt:71-140`) — clasificación de errores por tipo (omisión / error de orden / intrusión / conjunción) alineada posicionalmente. Es el diseño correcto de un scorer de memoria. Está **inalcanzable** desde el runtime, pero el código es bueno.
11. **`DefaultEmotionMetricsEngine`** — pese a los defectos de constructo señalados, su **disciplina de confianza** es la mejor de los motores vivos: `isCommissionConfident` exige n de no-go, tasa de aciertos go por encima de un piso, respuestas mínimas y ausencia de gaming (`:206-210`); `postErrorRecovery` exige **base de comparación post-correcta** además de casos post-error (`:182-184`). Eso es medir honestamente.
12. **`ConfounderEngine`** (`ConfounderEngine.kt:33-44`) — modelar riesgo de conocimiento previo, sesgo de variante y dominio no declarado como confounders explícitos que degradan el peso de evidencia es lo correcto. Falta alimentarlo.

### Roto — en orden de daño

1. **La capa viva no consume `MinSampleGate`.** Ocho motores con ocho tablas de n mínimos incompatibles (3, 4, 5, 6, 8…) contra una autoridad que exige 20-40. Todo lo demás es secundario a esto. [Mejora crítica]
2. **`RT_CV` con dos denominadores distintos** (`SD/mediana` en Enfoque, `SD/media` en Velocidad) bajo el mismo nombre. Incomparabilidad silenciosa. [Mejora crítica]
3. **Compuestos inventados presentados como índices**: `SUSTAINED_FOCUS_INDEX`, `VCE`, `total` de Velocidad, `averageIcr`, `fatIdx`. Ninguno tiene derivación, calibración ni validación. Cuatro de los cinco son lo que el usuario ve como "su resultado".
4. **`total` de Velocidad calculado con una barra y presentado como tres**, con acantilado de penalización en 0.80/0.30. Rompe la comparabilidad longitudinal por diseño.
5. **`null ≠ 0` aún vivo en tres formas**: `metric()` mapeando no-finito a 0.0 (`:147`), `MetricValue.unavailable` codificando 0.0, `impIdx ?: 0.0` fabricando control perfecto desde ausencia (`SpeedScoreEngine.kt:91`), y `POST_ERROR_ACCURACY = 0.0` sin errores (`:208`).
6. **Etiquetas que mienten sobre lo que se midió**: `DELAYED_RECALL_ACCURACY` sin demora, `WORKING_MEMORY_SPAN` que es un conteo de reconocimientos, `ARC_CLUSTERING` sobre el orden de estudio, `ASSOCIATIVE_INDEX` sin cue, `LISAS` que no es LISAS, `GRANULARITY_INDEX` que es cobertura. **Seis constructos nombrados con literatura y medidos con otra cosa.** [Riesgo científico]
7. **Cero medición de transferencia y cero medición de calibración en el producto vivo**, pese a tener la infraestructura de ambas construida y testeada.
8. **El ciclo de evaluación honesto (baseline → estabilización → probe → RCI → RealImprovement) está completo en código y sin un solo llamador de producción.** Es integración, no investigación.
9. **Los bancos son demasiado pequeños para el diseño que los usa** (~142 palabras / 56 pares; reserva ~28/~11). Con reconocimiento y bancos así, el efecto práctica domina la señal en semanas. Ningún arreglo estadístico compensa esto.

---

## 8.7 Contrato mínimo de métrica (implementable, una sola vez, en `core/scoring/`)

```
Measurement<T> =
  | Present(value: T, n: Int, cells: Map<CellId, Int>, unit: Unit, scorerVersion: Int)
  | Absent(reason: NO_SIGNAL | NO_NOISE | INSUFFICIENT_N | AT_OR_BELOW_CHANCE |
                   NOT_APPLICABLE | DEGENERATE_RESPONDING | TIMEBASE_UNRELIABLE |
                   BANK_UNVALIDATED)
```

Invariantes de build (tests en `desktopTest`, dentro de `check`):

- Ninguna métrica de producción construye un valor sin pasar por `MinSampleGate.evaluate`.
- `Absent` no tiene campo `value`. Ningún consumidor puede leer un número de una ausencia.
- Todo compuesto declara sus componentes y **falla** (devuelve `Absent`) si cualquiera está ausente. Prohibido calcular la media de las barras presentes.
- Todo estimador que soporte un veredicto de "distinto de cero" expone su error estándar y el veredicto usa el IC, no el punto (ya exigido por `MinSampleGate.requiresConfidenceInterval = true`).
- Nombre de métrica ⇒ definición operativa única en todo el repo (un solo `RT_CV`, un solo `d′`, un solo `accuracy`).

---

## 8.8 Orden de ejecución y gates

| # | Acción | Gate de verificación | Coste |
|---|---|---|---|
| 1 | Promover `DescriptiveScorer` a `core/scoring/` con `TaskSpec` por módulo; migrar Enfoque, Velocidad, Emociones y Decisiones a `d′`/criterio/exactitud-corregida-por-azar | Los 5 perfiles adversariales de S2 (pulsador, adivinador rápido, deadline-rider, honesto, degenerado) deben clasificarse igual en los 4 módulos | Alto, único |
| 2 | Unificar `RT_CV` a `SD/mediana` en todo el repo; renombrar a `RT_CV_MEDIAN` | Test que falle si aparece `sd/mean` bajo el nombre `cv` en cualquier módulo | Bajo |
| 3 | Eliminar las 12 métricas de la tabla de §8.4 "eliminación inmediata"; su desaparición de la UI es parte del gate | Ningún consumidor compila tras el borrado (el compilador es el gate) | Medio |
| 4 | `MetricValue.value: Double?` + borrar el mapeo no-finito→0.0 | Test de mutación: forzar todas las celdas bajo el piso ⇒ contar cuántos tests caen; si caen pocos, la suite no medía | Medio |
| 5 | Migrar los motores vivos a `MinSampleGate` (reclasifica muestras pequeñas: es un cambio de calibración deliberado, con goldens actualizados) | `desktopTest` verde con goldens nuevos + registro explícito del cambio de calibración | Medio |
| 6 | Productor de `ReliabilityEstimate` desde SEM intra-individual + fase de estabilización | Simulación N-of-1: un respondedor sintético sin cambio real NO debe producir `reliable = true` en ≥95% de corridas | Medio |
| 7 | Cablear baseline + probe reservado + `RealImprovementCriterion` a un flujo real | Respondedor sintético con mejora sólo en material entrenado ⇒ debe clasificarse `PRACTICE_EFFECT`, nunca `GENUINE_IMPROVEMENT` | Alto |
| 8 | Capturar confianza por ítem y computar Brier + descomposición de Murphy | Respondedor sintético perfectamente calibrado ⇒ fiabilidad ≈0; respondedor extremo ⇒ Brier peor que el sincero (**prueba de que la regla es estrictamente propia**, que la actual no pasa) | Medio |
| 9 | Ampliar bancos (objetivo ≥800 palabras / ≥300 pares) y elevar la reserva a ≥160 ítems | Test que falle si `reservedWordCount < k_administraciones × n_por_celda × celdas` | Alto, sin atajo |

**Sin 1, 2 y 4, cualquier trabajo en 6-8 construye estadística longitudinal sobre estimadores que no son comparables consigo mismos.** El orden no es negociable.

**Techo declarado:** todo lo anterior produce medición **intra-individual**. No produce percentiles, no produce comparación con otras personas y no autoriza ninguna afirmación sobre capacidad general. El lenguaje permitido sigue siendo "progreso frente a tu propia línea base", "orientativo", "perfil cognitivo funcional".