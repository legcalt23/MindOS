# V1_03 — Grid Espacial con Binding

**Módulo:** Memoria · **Verdict:** REVISE · **Prioridad:** P1 · **Profile-update:** sí (tras gates) · **TransferLabel:** `NEAR_TASK_PRACTICE` · **Absorbe:** EX6 (Detección de Cambio) · **Generador en repo:** **AUSENTE** (`EX5_GRID_ESPACIAL` en catálogo, sin generador y **sin Cowan-K en el engine**) [hecho]

> **Invariantes (repetidas):** `null != 0` · Enfoque/Velocidad sin profile-update hasta timebase+jank reales (no aplica: accuracy-based) · cero claims de far-transfer ni para negarlos · anti-gaming solo cuenta si implementado+testeado con adversarios · banco pequeño/no-validado bloquea perfil.

---

## 1. Objetivo cognitivo realista
Práctica de **memoria de trabajo visoespacial**: capacidad de objetos **ligados a ubicaciones** (Cowan's K, paradigma Luck & Vogel). Cubre la varianza visoespacial — ausente en V1_01/V1_02/V1_04/V1_05 — sin depender de RT. NO mide "orientación", "navegación", "sentido espacial" ni inteligencia espacial.

**Decisión de catálogo:** absorbe EX6 (change-detection) como **modo** `recallMode = CHANGE_DETECTION` del mismo generador, no como entrada separada (misma varianza; verificar r<0.7 entre modos antes de tratarlos como distintos).

## 2. Métrica primaria
`COWAN_K = setSize × (hitRate − falseAlarmRate)`, con **d' honesto** (no hit-rate crudo). En modo reconstrucción: `visuospatialScore = locationAcc × bindingAcc`. Accuracy-based.

## 3. Métricas secundarias
- `BINDING_ERROR_RATE`, `swapRate` (binding cruzado location↔objeto).
- Errores de sola-localización vs binding (**separados**).
- `RESPONSE_CRITERION` (sesgo), `DELAYED_RECALL_ACCURACY`, `CV` de K.

## 4. Gates de calidad

| Gate | Requisito | Si falla | Estado |
|---|---|---|---|
| Baja verbalizabilidad | objetos no nombrables (formas/tonos vecinos) | recoding verbal = memoria verbal disfrazada | confound flagueado |
| Separación de errores | location / binding / swap medidos aparte | señal colapsada | inválido |
| Distractor | grid distractor entre encoding y test | mide iconic memory/rehearsal | bloque baja calidad |
| d' con criterio no-extremo | `c` no extremo; bloques con criterio extremo descartados | sesgo de respuesta | bloque descartado |
| Detección random-placement | accuracy ≈ chance → baja calidad | adivinar "algo cambió" | descartado |
| Min-n | `nEffective ≥ nMin` (≥8 change + ≥8 no-change, c≥3) | poca evidencia | `CALIBRATING` |

## 5. Anti-gaming (solo cuenta si implementado+testeado)
- **`ButtonSpammer` / `MinEffort`** → `d' ≈ 0` → no K alto.
- **"Siempre cambió / nunca cambió"** → neutralizado por **d'** (no hit-rate) + descarte de criterio extremo.
- **Recoding verbal del layout** → neutralizado por objetos de baja verbalizabilidad + alta similitud cromática/forma. Test adversarial: objetos verbalizables vs no → diferencia de recoding detectada.
- **Rehearsal sostenido** → neutralizado por grid distractor.
- **Cobertura hoy [hecho]:** `MemoryMetricsEngine` **no implementa d' ni Cowan's K**. Construirlos (con corrección log-linear de Hautus para hit/fa = 0/1) es precondición de `VALID`.

## 6. Banco / generador
**Procedural**: configuraciones de grid aleatorias; setSize = eje de dificultad; objetos de un set de **baja-verbalizabilidad rotado** (gabor/formas abstractas/tonos vecinos). `BankIntegrity = PROCEDURAL`. Anti-memorización vía `excludeRecent`. Posiciones **balanceadas** (anti sesgo de centro). `expected` embebido como scoring-key en `GridBindingTrialPlan`.

## 7. UI / copy permitido
"Práctica de memoria visoespacial en esta tarea" · "Recuerda dónde estaba cada figura" · "Detecta si algo cambió" · "Calibrando".

## 8. Copy prohibido (`memoryTerminologyScanner`)
"sentido de la orientación", "memoria espacial en la vida real", "navegas mejor", "inteligencia espacial", "memoria fotográfica", far-transfer espacial.

## 9. Criterio EXACTO para actualizar perfil
`VALID` con: objetos baja-verbalizabilidad confirmados, `d'` con criterio no-extremo, location/binding/swap separados, `nEffective ≥ nMin`, `BankIntegrity = PROCEDURAL`. Si correlación con otra tarea visoespacial (modo change-detection) r≥0.7 ⇒ **no contar doble**.

## 10. Pseudocódigo de scoring (Kotlin)

```kotlin
// commonMain — memory/domain/scoring/CowanKScorer.kt  [POR_CONFIRMAR_EN_REPO]
object CowanKScorer {
    private const val MIN_CHANGE = 8
    private const val MIN_NOCHANGE = 8
    private const val EXTREME_CRITERION = 1.5   // |c| por encima => criterio extremo

    /** Corrección log-linear de Hautus para hit/fa = 0 ó 1. */
    private fun rate(events: Int, total: Int) = (events + 0.5) / (total + 1.0)

    fun changeDetectionK(setSize: Int, hits: Int, changeTrials: Int,
                         falseAlarms: Int, noChangeTrials: Int): CowanKResult {
        if (changeTrials < MIN_CHANGE || noChangeTrials < MIN_NOCHANGE)
            return CowanKResult(state = CALIBRATING, k = null, dPrime = null)   // null≠0
        val hitRate = rate(hits, changeTrials)
        val faRate  = rate(falseAlarms, noChangeTrials)
        val dPrime  = zScore(hitRate) - zScore(faRate)
        val criterion = -0.5 * (zScore(hitRate) + zScore(faRate))
        if (kotlin.math.abs(criterion) > EXTREME_CRITERION)
            return CowanKResult(state = INVALIDATED, k = null, dPrime = dPrime) // criterio extremo
        val k = setSize * (hitRate - faRate)
        if (k <= 0.0) return CowanKResult(state = INVALIDATED, k = null, dPrime = dPrime) // ≈chance
        return CowanKResult(state = VALID, k = k, dPrime = dPrime, criterion = criterion)
    }

    // Modo reconstrucción: separa location de binding (mutuamente exclusivos por celda).
    fun reconstructionScore(cells: List<CellOutcome>): ReconResult {
        val n = cells.size.toDouble()
        val locationAcc = cells.count { it.locationCorrect } / n
        val bindingAcc  = cells.count { it.bindingCorrect } / n
        val swapRate    = cells.count { it.isSwap } / n
        return ReconResult(visuospatialScore = locationAcc * bindingAcc,
            locationAcc = locationAcc, bindingAcc = bindingAcc, swapRate = swapRate)
    }
}
```

## 11. Tests obligatorios (enumerados)

**Unit**
- `cowanK_equalsSetSizeTimesHitMinusFa`.
- `logLinearCorrection_appliedWhenHitOrFaIsZeroOrOne`.
- `extremeCriterion_discardsBlock`.
- `reconstruction_classifiesLocationVsBindingVsSwap_mutuallyExclusive`.
- `generator_balancesGridPositions`.
- `generator_isDeterministicForSameSeed`.

**Adversarial**
- `buttonSpammer_dPrimeNearZero_noK`.
- `alwaysChanged_neutralizedByDPrime`.
- `verbalizableObjects_showRecodingAdvantage` — objetos nombrables vs no → confound detectado.
- `responseBias_discardsBlock`.

**Integración**
- `belowMinN_yieldsCalibratingNotZero`.
- `changeDetectionMode_correlation_lt_0_7_vsReconstruction` — verifica que EX6-modo no duplica varianza.
- `engineMissingDPrime_blocksValid` — sin d' implementado ⇒ no `VALID`.

**Golden**
- `cowanK_goldenLock`.
- `dPrime_goldenLock`.

## 12. Tabla de gates

| Gate disparado | `ScoreState` | Jugable | `updatesProfile` | `claimLevel` |
|---|---|---|---|---|
| sin d'/Cowan-K en engine | `CALIBRATING` | ✅ | ❌ | `PRACTICE_SIGNAL` |
| criterio extremo | `INVALIDATED` | ✅ | ❌ | `NONE` |
| accuracy ≈ chance | `INVALIDATED` | ✅ | ❌ | `NONE` |
| objetos verbalizables (confound) | `CALIBRATING` (flag) | ✅ | ❌ | `TASK_PERFORMANCE` |
| `change<8` o `noChange<8` | `INSUFFICIENT_N` | ✅ | ❌ | `PRACTICE_SIGNAL` |
| todo OK + robusto | `VALID` | ✅ | ✅ | `WITHIN_TASK_TREND` |

## 13. Riesgo residual
- **Recoding verbal residual** si los objetos son nombrables (mide memoria verbal disfrazada).
- **Redundancia con EX6** (mismo engine/banco): absorbido como `recallMode = CHANGE_DETECTION`; verificar r<0.7 entre modos.
- Drag/precisión en pantallas pequeñas (UX).

## 14. Definition of Done
Cowan's K + d' implementados en `MemoryMetricsEngine` + tests; consume `INFRA_01`/`INFRA_06`; objetos baja-verbalizabilidad validados; EX6 fusionado como modo (no entrada separada de catálogo); location/binding/swap separados; `memoryTerminologyScanner` verde; `registry ⟺ implemented`; compila ambos targets; Detekt verde sin regenerar baseline.
