# V1_05 — Episódica con Recall Diferido

**Módulo:** Memoria · **Verdict:** REVISE · **Prioridad:** P1 · **Profile-update:** sí (tras gates) · **TransferLabel:** `NEAR_TASK_PRACTICE` · **Generador en repo:** **EXISTE** (`Ex3ListGenerator`; **sin diferido servido, sin distractor**) [hecho]

> **Invariantes (repetidas):** `null != 0` · Enfoque/Velocidad sin profile-update hasta timebase+jank reales (no aplica: accuracy-based) · cero claims de far-transfer ni para negarlos · anti-gaming solo cuenta si implementado+testeado con adversarios · banco pequeño/no-validado bloquea perfil.

---

## 1. Objetivo cognitivo realista
Práctica de **memoria episódica y consolidación**: recall inmediato vs **+24h**, con codificación elaborativa (curva de olvido). Único ejercicio v1 que toca memoria a largo plazo. NO mide "memoria" general, no promete recuerdos vitales, no previene declive.

**Practice-effect ALTO** [hecho] (banco `WordPool` pequeño): la señal solo es honesta con rotación estricta + holdout + el diferido real servido. Sin distractor pre-inmediato, **mide recencia/buffer fonológico**, no episódica.

## 2. Métrica primaria
`DELAYED_RECALL_ACCURACY` (recall a +24h sobre material nuevo). Accuracy-based. Comparada honestamente con recall inmediato. **Requiere la probe diferida servida** (hoy no cableada → `EX-EX3-MEM`).

## 3. Métricas secundarias
- `IMMEDIATE_RECALL_ACCURACY` (para disociar de LTM).
- `gap inmediato↔diferido` = `CONSOLIDATION_INDEX = delayed / immediate`.
- `FORGETTING_RATE`, `ARC_CLUSTERING` (ARC de Roenker; `ArcClustering.kt` existe [hecho]), intrusiones extralista (falsos recuerdos), `CALIBRATION_INDEX` (JOL).

## 4. Gates de calidad

| Gate | Requisito | Si falla | Estado |
|---|---|---|---|
| Distractor pre-inmediato | tarea distractora antes del recall inmediato | mide recencia, no episódica | flag "mide recencia" |
| Probe +24h servida | anclada a `EpochClock`, tras la ventana real | sin LTM/consolidación | claim mejora `confident=false` |
| `FalseImprovement` vigilante | gap inmediato↔diferido monitorizado | mejora ilusoria (inmediato↑, diferido plano) | `PlateauStatus.FalseImprovement` |
| Min-n | `nEffective ≥ nMin` (lista ≥8 + probe completada) | poca evidencia | `CALIBRATING` |

## 5. Anti-gaming (solo cuenta si implementado+testeado)
- **`BankMemorizer`** → material rotado + holdout: el set memorizado no eleva el trait (la mejora sale solo del holdout UNTRAINED).
- **`MinEffort`** → accuracy baja.
- **Subvocalizar la lista en el delay** → neutralizado por **tarea distractora** (hoy ausente).
- **Inventar respuestas** → atrapado por intrusiones extralista (falsos recuerdos).
- **Ensayo encubierto entre sesiones** → no controlable (limitación honesta declarada).

## 6. Banco / generador
Material episódico rotado (listas categorizables/escenas); **holdout** para transferencia; `minBankSize` calibrado para evitar familiaridad. Particiones inmediato/diferido **equiparadas**. `BankIntegrity = SMALL_UNVALIDATED` hoy → debe pasar a holdout activo. Matching tolerante (lemmatize) para producción libre.

## 7. UI / copy permitido
"Práctica de memoria a largo plazo en esta tarea" · "Lo que recuerdas un día después" · "Recuerda la lista, hoy y mañana" · "Datos insuficientes hasta completar la prueba de mañana" · "Calibrando".

## 8. Copy prohibido (`memoryTerminologyScanner`)
"mejora tu memoria", "no olvidarás", "previene el declive/olvido", "memoria a largo plazo en la vida real", "estudias mejor", cualquier claim clínico.

## 9. Criterio EXACTO para actualizar perfil
`VALID` con: distractor presente, probe +24h **servida**, `FalseImprovement` **no** disparado, `nEffective ≥ nMin`, holdout activo. **Mejora de capacidad SOLO** sobre material UNTRAINED.

## 10. Pseudocódigo de scoring (Kotlin)

```kotlin
// commonMain — memory/domain/scoring/EpisodicScorer.kt  [POR_CONFIRMAR_EN_REPO]
object EpisodicScorer {
    private const val MIN_ITEMS = 8

    fun scoreImmediate(recalled: Set<Item>, presented: List<Item>, hadDistractor: Boolean): ImmediateResult {
        if (presented.size < MIN_ITEMS)
            return ImmediateResult(state = CALIBRATING, accuracy = null)        // null≠0
        val acc = recalled.count { it in presented }.toDouble() / presented.size
        // Sin distractor, esto mide RECENCIA: se flaguea, no puede ser señal episódica de perfil.
        val measuresRecency = !hadDistractor
        return ImmediateResult(state = VALID, accuracy = acc,
            measuresRecency = measuresRecency, arcClustering = ArcClustering.compute(recalled, presented))
    }

    fun scoreDelayed(recalled: Set<Item>, presented: List<Item>,
                     immediateAcc: Double?, probeServed: Boolean): DelayedResult {
        if (!probeServed) return DelayedResult(state = CALIBRATING, accuracy = null) // probe no servida → null
        if (presented.size < MIN_ITEMS) return DelayedResult(state = CALIBRATING, accuracy = null)
        val delayedAcc = recalled.count { it in presented }.toDouble() / presented.size
        val consolidation = if (immediateAcc == null || immediateAcc == 0.0) null
                            else delayedAcc / immediateAcc
        val forgetting = if (immediateAcc == null) null else (immediateAcc - delayedAcc).coerceAtLeast(0.0)
        return DelayedResult(state = VALID, accuracy = delayedAcc,
            consolidationIndex = consolidation, forgettingRate = forgetting)
    }

    /** FalseImprovement: inmediato sube pero diferido plano => mejora ilusoria. */
    fun falseImprovement(trainedDelta: Double, untrainedDelta: Double): Boolean =
        trainedDelta > 0.15 && untrainedDelta < 0.05   // espejo de PlateauDetector
}
```

## 11. Tests obligatorios (enumerados)

**Unit**
- `distractor_insertedBeforeImmediateRecall`.
- `delayedProbe_anchoredToEpochClock`.
- `consolidationIndex_equalsDelayedOverImmediate`.
- `forgettingRate_inZeroToOne`.
- `falseImprovement_firesWhenImmediateUpDelayedFlat`.
- `generator_isDeterministicForSameSeed`.

**Adversarial**
- `bankMemorizer_doesNotRaiseTrait`.
- `noDistractor_flaggedAsRecency`.
- `inventedAnswers_caughtByExtralistIntrusions`.

**Integración**
- `delayedProbe_actuallyServedAfterWindow`.
- `belowMinN_yieldsCalibratingNotZero`.
- `immediateUpDelayedFlat_blocksMejoraClaim`.

**Golden**
- `arcClustering_goldenLock`.
- `consolidationIndex_goldenLock`.

## 12. Tabla de gates

| Gate disparado | `ScoreState` | Jugable | `updatesProfile` | `claimLevel` |
|---|---|---|---|---|
| sin distractor | `CALIBRATING` (flag recencia) | ✅ | ❌ | `TASK_PERFORMANCE` |
| probe +24h no servida | `CALIBRATING` | ✅ | ❌ | `TASK_PERFORMANCE` |
| `FalseImprovement` disparado | `CALIBRATING` (mejora bloqueada) | ✅ | ❌ | `TASK_PERFORMANCE` |
| `items < 8` | `INSUFFICIENT_N` | ✅ | ❌ | `PRACTICE_SIGNAL` |
| todo OK + diferido servido | `VALID` | ✅ | ✅ | `WITHIN_TASK_TREND` |
| mejora vía holdout UNTRAINED + RCI | `VALID` + claim mejora | ✅ | ✅ | (mejora afirmable) |

## 13. Riesgo residual
- **Sin distractor mide recencia**, no episódica.
- **Ensayo encubierto entre sesiones** no controlable (limitación honesta).
- La demora 24h exige **scheduler real** (dependencia con `EX-TRANSFER-PROBE-MEM`); abandono de la probe +24h reduce datos (re-engagement).
- Producción libre con typos (matching tolerante).

## 14. Definition of Done
Probe +24h vía `EpochClock` + `DeferredRecallScheduler` **servida**; distractor pre-inmediato implementado; `PlateauDetector.FalseImprovement` cableado al flujo; `ArcClustering` integrado; consume `INFRA_01`/`INFRA_06`; banco con holdout (`INFRA_04`); `memoryTerminologyScanner` verde; `registry ⟺ implemented`; compila ambos targets; Detekt verde sin regenerar baseline.
