# V1_01 — Span Adaptativo Dual (Complex Span)

**Módulo:** Memoria · **Verdict:** REVISE · **Prioridad:** P1 · **Profile-update:** sí (tras gates) · **TransferLabel:** `NEAR_TASK_PRACTICE` · **Generador en repo:** `Ex1SpanGenerator` **EXISTE** [hecho]

> **Invariantes (repetidas):** `null != 0` · Enfoque/Velocidad sin profile-update hasta timebase+jank reales (no aplica aquí: accuracy-based) · cero claims de far-transfer ni para negarlos · anti-gaming solo cuenta si implementado+testeado con adversarios · banco pequeño/no-validado bloquea perfil.

---

## 1. Objetivo cognitivo realista
Práctica de **memoria de trabajo ejecutiva verbal**: mantenimiento de memoranda bajo una carga de **procesamiento concurrente** (complex/operation/reading span). NO mide "tu memoria", inteligencia, ni Gf. El constructo es el paradigma WM más defendible del repo (Conway et al.), pero solo si el componente de procesamiento se cumple de verdad — sin él, es un span simple disfrazado.

**Distinción clave:** la señal de perfil es el **span accuracy-based** (longitud recordada en orden), **no** la latencia de procesamiento (RT-crítico → fuera del perfil).

## 2. Métrica primaria
`WORKING_MEMORY_SPAN` = longitud de secuencia recordada en orden serial correcto, **accuracy-based**. Staircase sobre longitud; span = umbral de recall correcto (~punto 50% de reversal), medido **solo** sobre trials con `processingAccuracy ≥ 0.85`.

## 3. Métricas secundarias (feedback interno, NUNCA perfil)
- `SERIAL_ORDER_ACCURACY` (aciertos por posición).
- `INTRUSION_RATE` (memoranda de otra serie), `OMISSION_RATE`.
- `processingAccuracy` de la tarea distractora (es un **gate**, no señal de perfil).
- `IES_proc = meanRT / (1 − errProc)` y `dualCost = span_single − span_dual`: solo feedback metacognitivo. RT **fuera del perfil** (epoch-clock no autoritativo).
- `CV` de span inter-bloque (estabilidad).

## 4. Gates de calidad (obligatorios para profile-update)

| Gate | Umbral | Si falla | Estado resultante |
|---|---|---|---|
| Validez de procesamiento | `processingAccuracy ≥ 0.85` por bloque | el bloque no es complex-span | `INVALIDATED` |
| Staircase solo trials válidos | recall medido solo sobre trials con processing cumplido | staircase contaminado | trial descartado |
| Min-n | `nEffective ≥ nMin` (≥8 trials válidos/bloque, ≥2 bloques) | poca evidencia | `CALIBRATING` |
| Anticipation-floor en processing | RT_proc ≥ floor (~150 ms) [POR_CONFIRMAR_EN_REPO: ausente en Memoria hoy] | respuesta-por-defecto | trial inválido |
| RT fuera de perfil | siempre | — | RT nunca fluye al perfil (`RT_IRRELEVANT` para la primaria) |

## 5. Anti-gaming (solo cuenta si implementado+testeado)
- **`MinEffort` / `ButtonSpammer` en procesamiento** → `processingAccuracy < 0.85` → `INVALIDATED`. Detector: ratio procesamiento-correcto + **entropía** de la respuesta de processing.
- **Estrategia "blindar memoranda, azar en processing"** → atrapada por el gate de validez (el span de ese bloque no cuenta).
- **`BankMemorizer`**: el banco de secuencias es **procedural** (dígitos/posiciones aleatorios por seed) → no memorizable como texto. El banco aritmético se **rota** (reading-span ↔ operation-span) para evitar aprender la estructura fija.
- **Cobertura real hoy [hecho]:** Memoria NO tiene anticipation-floor ni detector de criterio. Hasta implementarlos, `DetectorCoverage.adequateFor(RT_SECONDARY)` falla → el span entra como `CALIBRATING`, no `VALID` con confianza alta.

## 6. Banco / generador
**Procedural** (sin banco fijo memorizable): secuencias aleatorias de memoranda + ítems de procesamiento. `minBankSize` N/A (es generador). `BankIntegrity = PROCEDURAL` [hecho: `Ex1SpanGenerator` es determinista por seed].
- **Procesamiento por defecto = reading-span** (juicio de plausibilidad de frase) para **matar el confound aritmético/educativo**; operation-span como rotación.
- Memoranda desde `WordPool` (autorado pequeño) → para profile, exigir banco ≥200 palabras equilibradas (categoría/frecuencia/longitud) **o** generación procedural pura. Anti-memorización vía `excludeRecentWordIds` [hecho: existe].

## 7. UI / copy permitido (scanner verde)
"Práctica de memoria de trabajo en esta tarea" · "Mantén palabras mientras resuelves" · "Aciertos frente a tu propia línea base" · "Estimación preliminar" · "Calibrando tu línea base" · "Datos insuficientes para confirmar".

## 8. Copy prohibido (bloquear en `memoryTerminologyScanner`)
"mejora tu memoria", "inteligencia/Gf/IQ", "memoria fotográfica", "recuerdas mejor en la vida real", "más concentración en el día a día", "transfiere a estudios/trabajo/multitarea", "test de IQ", cualquier far-transfer (ni afirmado ni negado en copy de usuario).

## 9. Criterio EXACTO para actualizar perfil
`ScoreResult.state == VALID` con: `processingAccuracy ≥ 0.85`, `nEffective ≥ nMin`, span estable inter-bloque (`MetricStability.STABLE`), `BankIntegrity ∈ {PROCEDURAL, OK}`. **Mejora de capacidad** solo se afirma vía sonda UNTRAINED + RCI (`|RCI| > 1.96` reliable + `isGenuine`), **nunca** por subir el span intra-tarea (= familiaridad). Hasta cablear la sonda, `claimLevel` tope = `WITHIN_TASK_TREND`.

## 10. Pseudocódigo de scoring (Kotlin)

```kotlin
// commonMain — memory/domain/scoring/ComplexSpanScorer.kt  [POR_CONFIRMAR_EN_REPO]
data class SpanBlockResult(
    val state: BlockState,            // VALID | INVALIDATED | CALIBRATING
    val span: Double?,               // null si no computable (NUNCA 0)
    val processingAccuracy: Double,
    val serialOrderAccuracy: Double?,
    val intrusionRate: Double?,
    val validTrials: Int,
)

object ComplexSpanScorer {
    private const val PROCESSING_VALIDITY = 0.85
    private const val MIN_VALID_TRIALS = 8

    fun scoreBlock(trials: List<SpanTrial>): SpanBlockResult {
        val procAcc = trials.count { it.processingCorrect }.toDouble() / trials.size
        // GATE 1: validez de procesamiento — sin esto NO es complex-span.
        if (procAcc < PROCESSING_VALIDITY) {
            return SpanBlockResult(BlockState.INVALIDATED, span = null,
                processingAccuracy = procAcc, serialOrderAccuracy = null,
                intrusionRate = null, validTrials = 0)
        }
        // Staircase SOLO sobre trials válidos (processing cumplido).
        val valid = trials.filter { it.processingCorrect }
        if (valid.size < MIN_VALID_TRIALS) {
            return SpanBlockResult(BlockState.CALIBRATING, span = null,
                processingAccuracy = procAcc, serialOrderAccuracy = null,
                intrusionRate = null, validTrials = valid.size)
        }
        val span = staircaseThreshold(valid)            // longitud al 50% reversal
        val serialAcc = serialOrderAccuracy(valid)      // aciertos por posición
        val intrusion = intrusionRate(valid)
        return SpanBlockResult(BlockState.VALID, span = span,
            processingAccuracy = procAcc, serialOrderAccuracy = serialAcc,
            intrusionRate = intrusion, validTrials = valid.size)
    }

    // Agregación inter-bloque: promedio ponderado por trials válidos (solo bloques VALID).
    fun sessionSpan(blocks: List<SpanBlockResult>): Double? {
        val good = blocks.filter { it.state == BlockState.VALID && it.span != null }
        if (good.isEmpty()) return null                  // null, jamás 0
        val wSum = good.sumOf { it.validTrials.toDouble() }
        return good.sumOf { it.span!! * it.validTrials } / wSum
    }
}
```

## 11. Tests obligatorios (enumerados)

**Unit**
- `staircase_convergesToRecallThreshold` — converge al umbral de recall.
- `span_equalsLengthAt50PercentReversal` — span = longitud al 50% reversal.
- `generator_isDeterministicForSameSeed` — secuencia reproducible por seed.
- `generator_differentSeeds_produceDifferentSequences`.
- `sessionSpan_returnsNull_whenNoValidBlocks` — `null`, nunca `0`.
- `sessionSpan_weightsBlocksByValidTrials`.

**Adversarial**
- `minEffortOnProcessing_invalidatesBlock` — `MinEffort` ⇒ `INVALIDATED` (no span alto).
- `buttonSpammerOnProcessing_lowEntropyDetected` — entropía baja detectada.
- `shieldMemorandaRandomProcessing_doesNotRaiseSpan` — blindar memoranda con processing al azar ⇒ bloque no cuenta.
- `bankMemorizer_proceduralSequencesNotMemorizable`.

**Integración**
- `belowMinN_yieldsCalibratingNotZero` — `CALIBRATING` con n<nMin (primarySignal null).
- `rtDoesNotFlowToProfile` — RT_proc nunca llega al `ModuleContribution`.
- `registryEqualsImplemented_ex1Wired` — consistencia generador↔catálogo.

**Golden**
- `spanScore_goldenLock` — locks numéricos del span para un dataset fijo (espejo de `MetricsEngineRegressionTest`).
- `serialOrderAccuracy_goldenLock`.

## 12. Tabla de gates (estado → updatesProfile)

| Gate disparado | `ScoreState` | Jugable | `updatesProfile` | `claimLevel` | Copy obligado |
|---|---|---|---|---|---|
| `processingAccuracy < 0.85` | `INVALIDATED` | ✅ | ❌ | `NONE` | "sesión no contabilizada" |
| `validTrials < 8` o `< 2 bloques` | `INSUFFICIENT_N` | ✅ | ❌ | `PRACTICE_SIGNAL` | "datos insuficientes" |
| detector anti-gaming ausente (hoy) | `CALIBRATING` | ✅ | ❌ | `PRACTICE_SIGNAL` | "calibrando" (`—`) |
| span inestable inter-bloque | `CALIBRATING` | ✅ | ❌ | `TASK_PERFORMANCE` | "estimación preliminar" |
| todo OK, conf<0.7 | `VALID` (prelim) | ✅ | ✅ | `TASK_PERFORMANCE` | "estimación preliminar" |
| todo OK + conf≥0.7 + inter-sesión | `VALID` (robusto) | ✅ | ✅ | `WITHIN_TASK_TREND` | "progreso vs tu línea base (esta tarea)" |

## 13. Riesgo residual
- **Practice-effect medio:** el span sube por familiaridad del paradigma, no por capacidad, hasta cablear la sonda UNTRAINED.
- **Confound del componente de procesamiento** (aritmético) si no se rota a reading-span.
- Subvocalización rápida; primacía/recencia; uso de notas externas (no controlable, declarado).

## 14. Definition of Done
Consume `INFRA_01` (ScoreResult) + `INFRA_06` (MinSampleGate); reading-span rotatorio activo; tests unit+adversarial+integración+golden verdes; `memoryTerminologyScanner` (`INFRA_05`) verde; `registry ⟺ implemented`; compila `:shared:compileAndroidMain` + `:androidApp:compileDebugKotlin`; Detekt ratchet sin regenerar baseline.
