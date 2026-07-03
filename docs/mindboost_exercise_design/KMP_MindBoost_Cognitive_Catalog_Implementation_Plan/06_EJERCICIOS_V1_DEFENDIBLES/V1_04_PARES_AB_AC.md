# V1_04 — Pares Asociados A-B / A-C

**Módulo:** Memoria · **Verdict:** REVISE · **Prioridad:** P1 · **Profile-update:** sí (tras gates) · **TransferLabel:** `NEAR_TASK_PRACTICE` · **Absorbe:** EX8 (Nombres-Rostros) · **Generador en repo:** **EXISTE** (`Ex4PairsGenerator`; **sin fase A-C, sin demora servida**) [hecho]

> **Invariantes (repetidas):** `null != 0` · Enfoque/Velocidad sin profile-update hasta timebase+jank reales (no aplica: accuracy-based) · cero claims de far-transfer ni para negarlos · anti-gaming solo cuenta si implementado+testeado con adversarios · banco pequeño/no-validado bloquea perfil.

---

## 1. Objetivo cognitivo realista
Práctica de **memoria asociativa y resistencia a la interferencia**: aprender A-B y luego A-C (mismo cue A, target distinto), midiendo intrusiones de B cuando se pide C (interferencia proactiva/retroactiva). Accuracy-based. NO mide "memoria" general, capacidad de aprendizaje vital ni "recordar nombres en reuniones".

**Practice-effect ALTO** [hecho]: el banco autorado es pequeño (~parte de 38 líneas) → "progreso" = ítems aprendidos. La señal solo es honesta con rotación estricta + holdout servido.

## 2. Métrica primaria
`ASSOCIATIVE_INDEX = correctTargets / pairCount` (recall correcto de la asociación **vigente**) + `INTRUSION_RATE` (reportar B cuando se pide C). Accuracy-based.

## 3. Métricas secundarias
- `CONJUNCTION_ERROR_RATE` (separado de intrusión).
- `proactiveInterference = abIntrusions / (abIntrusions + correctC)`.
- `interferenceIndex` (<1 = interferencia), `DELAYED_RECALL_ACCURACY`, asimetría forward/backward, curva de adquisición A-B.

## 4. Gates de calidad

| Gate | Requisito | Si falla | Estado |
|---|---|---|---|
| Fase A-C cableada | overlap real del cue A (hoy ausente → `EX-EX4-MEM`) | sin medida de interferencia | **BLOCK** |
| Rotación estricta | contenido nuevo por sesión (`excludeRecentPairIds`) | banco memorizado | `BANK_CONTAMINATED` |
| Demora 24h real | anclada a `EpochClock` y **servida** (no simulada) | ensayo encubierto / no consolidación | claim de mejora `confident=false` |
| Intrusión vs conjunction separados | dos contadores | señal colapsada | inválido |
| Min-n | `nEffective ≥ nMin` (≥8 pares) | poca evidencia | `CALIBRATING` |

## 5. Anti-gaming (solo cuenta si implementado+testeado)
- **`BankMemorizer`** → rotación + banco grande + holdout: accuracy del set memorizado **no supera** el holdout (la señal de trait no sube). Detector de exposición (no reexponer par reciente) [hecho: `excludeRecentPairIds` existe].
- **`MinEffort`** → accuracy baja.
- **Adivinar del banco** → distractores que incluyen los targets B.
- **Saltar la demora** → atrapado por anclaje a `EpochClock`.

## 6. Banco / generador
Banco grande de pares A-B/A-C **balanceado y rotado**; `minBankSize` ≥ el que evite plateau de familiaridad (calibrar con curva de memorización, backlog científico; objetivo ≥300 pares). **Holdout** (`TransferProbePartition`) para medir transferencia. `BankIntegrity = SMALL_UNVALIDATED` hoy → debe pasar a `OK`/holdout activo para `VALID`. Matching tolerante (lemmatize) para typos. **Mono-idioma declarado** (sesgo léxico).

## 7. UI / copy permitido
"Práctica de asociaciones en esta tarea" · "Recuerda la pareja vigente" · "Actualiza una asociación (cambió el número de alguien)" · "Calibrando".

## 8. Copy prohibido (`memoryTerminologyScanner`)
"memoria asociativa en la vida real", "recuerdas nombres mejor", "nunca olvidas un nombre", "mejor en reuniones/networking", "memoria fotográfica", "inteligencia/memoria general".

## 9. Criterio EXACTO para actualizar perfil
`VALID` con: A-C cableada, rotación activa, demora 24h **servida**, `nEffective ≥ nMin`, `BankIntegrity` con holdout activo. **Mejora de capacidad SOLO** sobre ítems UNTRAINED (holdout), **nunca** por accuracy del set practicado.

## 10. Pseudocódigo de scoring (Kotlin)

```kotlin
// commonMain — memory/domain/scoring/PairedAssociatesScorer.kt  [POR_CONFIRMAR_EN_REPO]
object PairedAssociatesScorer {
    private const val MIN_PAIRS = 8

    enum class RecallKind { CORRECT_C, INTRUSION_B, CONJUNCTION, OMISSION }

    fun classify(reported: Target?, cue: Cue, ab: Map<Cue, Target>, ac: Map<Cue, Target>): RecallKind = when {
        reported == null              -> RecallKind.OMISSION
        reported == ac[cue]           -> RecallKind.CORRECT_C
        reported == ab[cue]           -> RecallKind.INTRUSION_B    // target obsoleto del par A-B
        isConjunction(reported, cue)  -> RecallKind.CONJUNCTION    // mezcla de features de otros pares
        else                          -> RecallKind.INTRUSION_B
    }

    fun score(outcomes: List<RecallKind>): PairedResult {
        if (outcomes.size < MIN_PAIRS)
            return PairedResult(state = CALIBRATING, associativeIndex = null, intrusionRate = null) // null≠0
        val n = outcomes.size.toDouble()
        val correctC   = outcomes.count { it == RecallKind.CORRECT_C }
        val intrusionB = outcomes.count { it == RecallKind.INTRUSION_B }
        val conjunction = outcomes.count { it == RecallKind.CONJUNCTION } / n
        val associative = correctC / n
        val proactive   = if (intrusionB + correctC == 0) null
                          else intrusionB.toDouble() / (intrusionB + correctC)
        return PairedResult(state = VALID, associativeIndex = associative,
            intrusionRate = intrusionB / n, conjunctionRate = conjunction,
            proactiveInterference = proactive)
    }

    /** La claim de MEJORA solo sale del holdout, nunca del set practicado. */
    fun improvementClaim(trainedDelta: Double, untrainedDelta: Double, rci: RciResult): Boolean =
        rci is RciResult.Value && rci.reliable && untrainedDelta > 0.05
}
```

## 11. Tests obligatorios (enumerados)

**Unit**
- `classify_intrusionB_whenCRequestedButBReported`.
- `classify_conjunction_separatedFromIntrusion`.
- `acPhase_sharesCueWithAbPhase`.
- `delay_anchoredToEpochClock`.
- `generator_isDeterministicForSameSeed`.

**Adversarial**
- `bankMemorizer_setAccuracyApproxHoldout_noTraitRise`.
- `minEffort_doesNotUpdateProfile`.
- `guessFromBank_neutralizedByDistractors`.
- `skipDelay_caughtByEpochClockAnchor`.

**Integración**
- `noAcPhase_exerciseBlocked` — sin fase A-C ⇒ `BLOCK` (no expuesto).
- `delay24h_actuallyServedAfterWindow`.
- `belowMinN_yieldsCalibratingNotZero`.
- `improvementClaim_onlyFromUntrainedHoldout`.

**Golden**
- `associativeIndex_goldenLock`.
- `proactiveInterference_goldenLock`.

## 12. Tabla de gates

| Gate disparado | `ScoreState` | Jugable | `updatesProfile` | `claimLevel` |
|---|---|---|---|---|
| sin fase A-C | (no expuesto) | ❌ | ❌ | — |
| banco repetido detectado | `BANK_CONTAMINATED` | ✅ | ❌ | `PRACTICE_SIGNAL` |
| demora 24h no servida | `CALIBRATING` | ✅ | ❌ (claim mejora `confident=false`) | `TASK_PERFORMANCE` |
| `pares < 8` | `INSUFFICIENT_N` | ✅ | ❌ | `PRACTICE_SIGNAL` |
| todo OK, holdout activo | `VALID` | ✅ | ✅ | `WITHIN_TASK_TREND` |
| mejora vía holdout + RCI reliable | `VALID` + claim mejora | ✅ | ✅ | (mejora afirmable) |

## 13. Riesgo residual
- **Practice-effect ALTO** domina si rotación/holdout fallan.
- Sesgo cultural/léxico si los pares son palabras (declarar mono-idioma).
- **EX8 (Nombres-Rostros)** absorbido como variante `material = FACE_NAME`: standalone tiene sesgo étnico fuerte → requiere banco de caras diverso licenciado o queda como variante teórica (P3, aplazado).
- Abandono de la probe 24h reduce datos.

## 14. Definition of Done
Fase A-C implementada (`overlap` real) + tests; `DeferredRecallScheduler`/`LocalDeferredRecallRepository` cableados al holder (modelos existen en `domain/deferred/`); demora 24h vía `EpochClock` **servida**; banco rotado con holdout (`INFRA_04`); consume `INFRA_01`/`INFRA_06`; `memoryTerminologyScanner` verde; `registry ⟺ implemented`; compila ambos targets; Detekt verde sin regenerar baseline.
