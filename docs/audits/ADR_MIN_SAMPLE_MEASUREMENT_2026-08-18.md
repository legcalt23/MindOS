# ADR — `DefaultMetricsEngine` y `MinSampleGate`: separar la pregunta de control de la pregunta de medida

- **Fecha:** 2026-08-18
- **Hallazgo:** `MB-CATALOG-0011`
- **Fase:** plan blindado del catálogo cognitivo · R8
- **Estado:** ACEPTADO (implementado en la misma fase)

---

## 1. FASE 0 — reconciliación del hallazgo contra el repo real

El hallazgo dice: *«`DefaultMetricsEngine` no consume `MinSampleGate`. Mantiene sus pisos locales
(`MIN_N_ACCURACY`/`MIN_N_RT`/`MIN_N_POST_ERROR`, 3..5) mientras la autoridad central exige ≥20 para la misma
familia de métricas. Dos fuentes de verdad sobre cuánta muestra basta.»*

Verificado símbolo a símbolo, **seis** hechos corrigen o acotan ese enunciado:

| # | Enunciado del hallazgo | Hecho verificado |
|---|---|---|
| 1 | Tres constantes locales | Son **cinco**: además `MIN_N_SLOPE = 6` y `MIN_N_CONGRUENCY = 3` |
| 2 | `MinSampleGate` sin consumir | Lo consumen ≥8 sitios de producción (`CalibrationKernel`, `DescriptiveScorer` de focus2, `MemorySpanProfileAdapter`, `RecognitionScoreAdapter`, `ReversibilityProfileAdapter`, `SentinelModel`, `VectorFilterModel`, `FocusRunPlanner`). El que no lo consume es **solo** `DefaultMetricsEngine` |
| 3 | «Dos fuentes de verdad sobre la misma pregunta» | Son **dos preguntas distintas** respondidas por **un solo booleano**: ver §2 |
| 4 | Riesgo implícito: contaminar el perfil | El camino al perfil desde este motor está **fail-closed**: `FocusProfileSource.contribution()` devuelve `null` incondicionalmente (gate D1, `purpose = UNKNOWN`, `lineageComplete = false`). No hay deriva de perfil posible hoy |
| 5 | — | El camino **consecuente y vivo** es el **RCI** (`EvaluationComparator` ← `FocusEvaluationService` ← `FocusEvaluationStateHolder` ← `FocusRootScreen`), es decir la afirmación de *cambio fiable de capacidad* |
| 6 | — | El RCI está hoy cerrado por una **segunda** puerta (`ReliabilitySource.EMPTY`), así que el defecto está **armado detrás de una puerta fail-closed**, no vivo |

## 2. El defecto real: un booleano que responde dos preguntas

`MetricValue.confident` se consulta hoy desde dos familias de consumidores con necesidades de muestra
legítimamente distintas:

**(a) Lazo de control en sesión** — `AdaptiveEngine` (mueve la dificultad leyendo `ACCURACY` y `RT_CV`),
`RecoveryPolicy`, `OvertrainingGuard`, `ArchetypeInference`, `PersonalizedSessionScorer`, las leyendas de la UI
(`FocusSessionAssembler`, `FocusSummaryScreen`, `FocusProgressScreen`) y la agregación por bloque
(`weightedAverage`, `sessionFatigue`).

**(b) Afirmación de medida** — `EvaluationComparator` (RCI = *cambio fiable*, invariante #7 del charter),
`DefaultCalibrationEngine.baselineSnapshot` (el lado *pre* de esa misma comparación) y `Trends`.

Un piso de 5 es defendible para (a) y no lo es para (b). Un piso de 20/celda es correcto para (b) y
**rompería** (a). Subir el booleano único a 20 no es «migrar a la autoridad central»: es contestar la pregunta
de control con la respuesta de la pregunta de medida.

Es la misma familia de defecto que R7 (`MB-CATALOG-0009`): **un valor que carga dos hechos**.

## 3. Techo alcanzable (calculado ANTES de decidir, lección de R7)

Tamaños de bloque REALES en producción: baseline 75/60/60/55 (`BaselineProtocols.INITIAL`), sonda de
evaluación 55 (`EVALUATION_PROBE_TRIALS`), entrenamiento `coreMs / 3800` acotado a **[24, 160]**.

`calibrationDifficulty` **no fija `interference`**, que por defecto es `AxisLevel.MIN`, así que
`E2DifficultyMapping` da `incongruentProportion = lerp(0.20, 0.60, 0.0) = 0.20`. En el bloque E2 de baseline
(60 ensayos, sorteo Bernoulli por ensayo) eso son **≈12 ensayos incongruentes**, y solo los HIT aportan RT.

**Conclusión (corregida al medirla, §7·C2):** el sorteo es **Bernoulli por ensayo**, no una cuota, así que no
hay techo duro: la mediana ronda los 12 incongruentes frente a un piso de 20, y una **cola fina (~0.8 %,
Binomial(60, 0.20) ≥ 20)** sí lo cruza. Eso es PEOR que un techo, no mejor: que un retest pueda o no sostener
una afirmación de capacidad depende de la SEMILLA. Y el conteo es de estímulos — solo los HIT alimentan las
celdas de RT, así que la muestra real es menor todavía. El hecho se PINEA por distribución (mediana < piso y
proporción de semillas que lo cruzan < 5 %); no se «arregla» bajando la tabla central — bajar el umbral para
que algo pase es exactamente el anti-patrón que R7 cerró.

## 4. Decisión

1. **NO se suben los pisos de `confident`.** `confident` queda como la respuesta a la pregunta de control en
   sesión. Cero cambio de calibración, cero deriva de dificultad, cero cambio en la UI.
2. **`DefaultMetricsEngine` SÍ consume `MinSampleGate`**, pero para producir un **segundo hecho con nombre
   propio**: `MetricValue.measurementSufficient`, la suficiencia de muestra **de grado medida**, evaluada con
   los conteos POR CELDA en el punto donde esos conteos existen.
3. **Tri-estado deliberado**: `null` = «esta métrica no está en la tabla central, la pregunta no aplica»;
   `true`/`false` = veredicto de `MinSampleGate`. No se reutiliza `false` para «no aplica» (centinela ambiguo,
   lección R7).
4. **Solo cambia el consumidor de (b) que puede cambiar**: `EvaluationComparator` (RCI) exige
   `measurementSufficient == true`. Los de (a) siguen leyendo `confident` sin tocarse.
   **Corrección sobre el diseño inicial (§7·C1): `DefaultCalibrationEngine.baselineSnapshot` NO se toca.**
5. **No se persiste.** Se deriva en vivo, en el mismo proceso que lo consume (`retestMetrics = block.metrics`).
   Un veredicto persistido es una proyección que envejece bajo su regla vieja — doctrina ya escrita en el KDoc
   de `ProfileMeasurementEligibility`. **Sin migración y sin tocar `MetricValueDto`.**

### Mapeo `MetricKey` → `ScoreMetricId`

| MetricKey | ScoreMetricId | nMin |
|---|---|---|
| `ACCURACY`, `RT_MEDIAN`, `RT_CV`, `LAPSES`, `IES`, `RATE_CORRECT_SCORE`, `LISAS`, `BLOCK_STABILITY`, `SUSTAINED_FOCUS_INDEX` | `ACCURACY_RT_BASE` | 20 |
| `CONGRUENCY_EFFECT` | `CONGRUENCY_EFFECT` | 20/celda |
| `SWITCH_COST` | `SWITCH_COST` | 20/celda |
| `DUAL_TASK_COST` | `DUAL_TASK_COST` | 20/celda |
| `PRESSURE_DELTA` | `PRESSURE_DELTA` | 20/celda |
| `POST_ERROR_SLOWING`, `POST_ERROR_ACCURACY` | `POST_ERROR` | 20/celda |
| `SSRT` | `SSRT` | 30 |
| `FALSE_ALARMS`, `OMISSIONS`, `ANTICIPATIONS`, `FATIGUE_INDEX`, `SET_SIZE_SLOPE`, `VIGILANCE_DECREMENT_SLOPE`, `DISTRACTION_RESISTANCE_INDEX`, `INHIBITORY_CONTROL_INDEX`, `NEAR_TRANSFER_INDEX` | — (sin id central) | `null` |

Las nueve últimas quedan **fuera de toda afirmación de cambio fiable** hasta que la tabla central ratifique un
piso para ellas. Es fail-closed y es un hueco declarado con dueño, no un descuido.

## 5. Estrategia de goldens (la precondición que el hallazgo exigía)

El hallazgo se dejó abierto porque «subir al piso central RECLASIFICA muestras pequeñas y rompe los golden de
métricas». La estrategia que lo desbloquea es hacer el cambio **estrictamente aditivo**:

1. **Ningún golden existente puede cambiar de valor.** `confident` conserva su semántica y sus cinco pisos, así
   que ninguna expectativa de métrica, dificultad adaptativa, leyenda de UI o agregación se mueve.
2. **Criterio de aborto declarado:** si al ejecutar la suite completa cualquier golden exige edición, el cambio
   NO es aditivo y la fase se detiene — editar un golden para que pase sería reclasificar por la puerta de
   atrás (lección 2026-07-20: cambiar el fixture para salvar la aserción).
3. **Tests nuevos** que pinean lo que sí cambia: mapeo exhaustivo sobre `MetricKey.entries` (un `MetricKey`
   nuevo obliga a decidir), suficiencia por celda, RCI rechaza lo insuficiente, **mitad discriminante** (RCI
   sigue disparando con muestra suficiente), y el techo inalcanzable de E2 en baseline.
4. **Mutación ejecutada**: reintroducir `confident` como gate del RCI debe poner el suite en ROJO.

## 6. Consecuencias

- El RCI pasa a exigir muestra de grado medida. Hoy es **inerte** (reliabilities vacías), así que el cambio no
  altera ninguna salida observable — y queda correcto para el día en que el estudio de fiabilidad puebla la
  costura, que es precisamente cuando el defecto se habría activado.
- `CONGRUENCY_EFFECT` no podrá sostener un RCI con el bloque de baseline actual. Alternativas para una fase
  futura, **ninguna ejecutada aquí**: subir el bloque E2 de baseline, fijar `interference` en
  `calibrationDifficulty`, o sortear las celdas por cuota en vez de por Bernoulli. Es decisión de producto.
- Los pisos locales siguen existiendo y siguen siendo la autoridad del lazo de control. Eso es deliberado y
  queda documentado aquí para que nadie lo lea como la deuda que este ADR no cerró.

## 7. Correcciones descubiertas AL IMPLEMENTAR (no en FASE 0)

Dos afirmaciones de este mismo ADR resultaron falsas al escribir el código. Se dejan escritas, no se
reescribe la historia: son el registro de que la verificación siguió corriendo después de la fase de diseño.

### C1 — `baselineSnapshot` NO puede exigir suficiencia de medida

El diseño original (§4.4) nombraba `DefaultCalibrationEngine.baselineSnapshot` como el segundo consumidor a
endurecer, por ser el lado *pre* de la comparación. Verificado contra el repo, no procede, por dos razones
independientes y ambas suficientes:

1. **Radio de impacto fuera de (b).** `BaselineSnapshot.perMetric` no alimenta solo el RCI: también lo lee
   `ArchetypeInference.baselineValue`, que es un consumidor del **lazo de control**. Estrecharlo con una regla
   de medida movería exactamente lo que este ADR promete no mover.
2. **El dato no existe en el lado pre.** `perMetric` se persiste como `Map<String, Double>`
   (`FocusDtos`/`FocusPersistenceMappers`): guarda VALORES, no las muestras que los produjeron. En el momento
   de comparar, la suficiencia del pre **no es recuperable**, y hacerla recuperable es un cambio de esquema con
   migración — justo lo que §4.5 excluye.

**Resultado:** solo se gatea el lado *retest*, y el hueco del lado *pre* queda **declarado** en el KDoc de
`EvaluationComparator` y como hallazgo con dueño en el ledger. Un gate a medias declarado es honesto; un gate a
medias silencioso es el defecto que R7 cerró.

### C2 — el techo de `CONGRUENCY_EFFECT` no es un techo, es una cola

Ver §3. El primer test escrito afirmaba `max(incongruentes) < 20` sobre 200 semillas y **falló**, que es
precisamente para lo que se calcula en vez de suponerse. La afirmación correcta es distribucional.

### C3 — dos fixtures declaran el hecho nuevo (y el criterio de aborto NO se disparó)

El criterio de §5.2 es sobre **goldens**: ningún valor esperado podía moverse. Ninguno se movió. Sí hubo que
tocar dos **fixtures** que construyen `MetricValue` a mano y por tanto dejaban el campo nuevo sin declarar:

| Fixture | Cambio | Por qué no es «editar el test para que pase» |
|---|---|---|
| `ReliableChangeIndexTest` (mapa de retest) | añade `measurementSufficient = true` | Con `n = 50` y piso 20, `true` es **lo que el motor estampa**: el fixture pasa a decir la verdad que omitía. Lo que el test discrimina —fiabilidad presente vs ausente— queda intacto |
| `FocusTestFixtures.metricSet` | **deriva** el campo con `MeasurementSufficiency.evaluate(key, 50, true)` | No se afirma a mano: se aplica la misma regla que el motor, así que una métrica sin piso sigue saliendo `null` honestamente |

La prueba de que esto no es la trampa de 2026-07-20 («cambiar el fixture para salvar la aserción») es la
mutación: con el gate revertido a `confident`, los tests nuevos se ponen ROJOS (§8). Un fixture debilitado no
habría podido distinguir las dos versiones.

## 8. Verificación por mutación (ejecutada, no descrita)

| Mutación | Resultado |
|---|---|
| RCI vuelve a gatear por `confident` | **ROJO** — 3 tests de `RciMeasurementSufficiencyTest` |
| `cellsOf` usa la SUMA en vez de la celda menor | **ROJO** — `theEngineJudgesACongruencyEffectOnItsSmallerCell` |
| Se elimina la pasada `stampMeasurementSufficiency` | **ROJO** — 2 tests de `MeasurementSufficiencyTest` |

Las tres restauraciones se verificaron **byte-idénticas por SHA-256**.
