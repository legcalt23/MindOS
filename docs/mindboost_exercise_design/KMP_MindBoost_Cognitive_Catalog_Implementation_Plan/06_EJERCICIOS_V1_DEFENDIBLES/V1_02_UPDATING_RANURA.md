# V1_02 — Updating de Ranura (Running Memory)

**Módulo:** Memoria · **Verdict:** REVISE · **Prioridad:** P1 · **Profile-update:** sí (tras gates) · **TransferLabel:** `NEAR_TASK_PRACTICE` · **Generador en repo:** **AUSENTE** (`EX2_ACTUALIZACION_SET` en catálogo, sin generador → `EX-EX2-MEM`) [hecho]

> **Invariantes (repetidas):** `null != 0` · Enfoque/Velocidad sin profile-update hasta timebase+jank reales (no aplica: accuracy-based) · cero claims de far-transfer ni para negarlos · anti-gaming solo cuenta si implementado+testeado con adversarios · banco pequeño/no-validado bloquea perfil.

---

## 1. Objetivo cognitivo realista
Práctica de **actualización de memoria de trabajo** (memory updating, paradigma keep-track): reemplazar continuamente el contenido de `k` ranuras manteniendo **solo el último estado** e inhibiendo lo obsoleto. Binding ranura↔valor explícito. NO mide inteligencia, "agilidad mental" ni multitarea.

**Diferenciador vs span (V1_01) y n-back:** el span mide *mantenimiento*; el updating mide *reemplazo dirigido* con estado final + binding. La ortogonalidad debe demostrarse (r<0.7), no asumirse.

## 2. Métrica primaria
`k efectivo` = nº de ranuras mantenidas con `accuracy ≥ 0.75` + `INTRUSION_RATE` (reportes de un estado **obsoleto**, no del último). Accuracy-based.

## 3. Métricas secundarias
- `BINDING_ERROR_RATE` (valor correcto pero en ranura equivocada — **distinto** de intrusión).
- `OMISSION_RATE`, `CV` de k (estabilidad).
- Latencia de reporte (feedback, NUNCA perfil — RT no autoritativo).

## 4. Gates de calidad

| Gate | Requisito | Si falla | Estado |
|---|---|---|---|
| Fillers obligatorios | updates a ranuras inexistentes entre reportes | retención pasiva = span disfrazado | bloque baja calidad |
| Intrusión vs binding separados | dos contadores distintos por probe | señal colapsada | inválido |
| Generador real | `Ex2UpdatingGenerator` debe existir | sin generador | **BLOCK** (no expuesto) |
| Updates/ranura variables | nº variable (no fijo) | "contar updates" | bloque baja calidad |
| Sonda de 1 ranura | control fácil; si falla → sesión contaminada | sonda 1 falla | sesión descartada |
| Min-n | `nEffective ≥ nMin` (≥8 probes válidos) | poca evidencia | `CALIBRATING` |

## 5. Anti-gaming (solo cuenta si implementado+testeado)
- **`MinEffort` / respuesta-por-defecto** → intrusión alta o accuracy baja → no `k` alto.
- **"Último visto sin tracking"** → atrapado por fillers a ranuras inexistentes (responder al filler = automatismo) → bloque baja calidad.
- **"Preparar solo algunas ranuras"** → atrapado por la sonda de UNA ranura aleatoria.
- **`BankMemorizer`** → generador procedural (valores de set pequeño/parecido) evita memorización.
- Detector de patrón de respuesta repetida (entropía baja).

## 6. Banco / generador
**Procedural**: stream de updates con fillers; nº de ranuras = eje de dificultad. `BankIntegrity = PROCEDURAL`. Anti-memorización vía `excludeRecent` [hecho: infra existe]. Holdout via `TransferProbePartition` (reservado, no servido). `expectedFinal` embebido como scoring-key en `UpdatingTrialPlan` (patrón `MemoryTrialPlan`).

## 7. UI / copy permitido
"Práctica de actualización en esta tarea" · "Recuerda el último valor de cada ranura" · "Actualiza y descarta lo viejo" · "Calibrando".

## 8. Copy prohibido (`memoryTerminologyScanner`)
"agilidad mental", "inteligencia fluida/Gf", "mejor en multitarea", "recuerda mejor en la vida real", "transfiere a tareas pendientes del trabajo".

## 9. Criterio EXACTO para actualizar perfil
`VALID` con: fillers presentes, intrusión/binding separados, `k` estable, `nEffective ≥ nMin`, `BankIntegrity = PROCEDURAL`. **Antes de tratar `k` como sub-señal independiente del perfil**, documentar ortogonalidad empírica (r<0.7 vs span V1_01 y n-back) en `EX-EX2-MEM`; si r≥0.7 ⇒ candidato a MERGE (no contar varianza doble en el brain-profile).

## 10. Pseudocódigo de scoring (Kotlin)

```kotlin
// commonMain — memory/domain/scoring/UpdatingScorer.kt  [POR_CONFIRMAR_EN_REPO]
data class ProbeOutcome { enum class Kind { CORRECT, INTRUSION, BINDING_ERROR, OMISSION } }

object UpdatingScorer {
    private const val K_ACCURACY = 0.75
    private const val MIN_PROBES = 8

    fun classifyProbe(reported: SlotValue?, slot: Slot, history: SlotHistory): ProbeOutcome.Kind = when {
        reported == null                          -> ProbeOutcome.Kind.OMISSION
        reported == history.latest(slot)          -> ProbeOutcome.Kind.CORRECT
        reported in history.obsoleteValues(slot)  -> ProbeOutcome.Kind.INTRUSION      // estado viejo
        history.isLatestOfAnotherSlot(reported)   -> ProbeOutcome.Kind.BINDING_ERROR  // valor de otra ranura
        else                                       -> ProbeOutcome.Kind.INTRUSION
    }

    fun score(probes: List<ProbeOutcome.Kind>): UpdatingResult {
        if (probes.size < MIN_PROBES)
            return UpdatingResult(state = CALIBRATING, kEffective = null, intrusionRate = null) // null≠0
        val n = probes.size.toDouble()
        val intrusion = probes.count { it == INTRUSION } / n
        val binding   = probes.count { it == BINDING_ERROR } / n
        val correct   = probes.count { it == CORRECT } / n
        // intrusion y binding son MUTUAMENTE EXCLUSIVOS por probe (propiedad testeada).
        val updatingScore = (1.0 - (intrusion + 0.5 * binding)).coerceIn(0.0, 1.0)
        val kEff = if (correct >= K_ACCURACY) effectiveK(probes) else null
        return UpdatingResult(state = VALID, kEffective = kEff,
            intrusionRate = intrusion, bindingErrorRate = binding, updatingScore = updatingScore)
    }
}
```

## 11. Tests obligatorios (enumerados)

**Unit**
- `generator_insertsFillersToNonexistentSlots`.
- `generator_variableUpdatesPerSlot`.
- `oneSlotProbe_isAlwaysEasy`.
- `classifyProbe_intrusionVsBinding_mutuallyExclusive`.
- `generator_isDeterministicForSameSeed`.

**Adversarial**
- `passiveRetention_ignoringUpdates_yieldsHighIntrusion_noHighK`.
- `respondToFiller_flagsAutomatism_lowQuality`.
- `minEffort_doesNotUpdateProfile`.
- `repeatedResponsePattern_lowEntropyDetected`.

**Integración**
- `noGenerator_exerciseNotExposed` — sin generador ⇒ registry no lo lista (`BLOCK`).
- `belowMinN_yieldsCalibratingNotZero`.
- `orthogonality_documented_beforeProfileSubSignal` — r<0.7 vs span/n-back registrado.

**Golden**
- `updatingScore_goldenLock`.
- `intrusionRate_goldenLock`.

## 12. Tabla de gates

| Gate disparado | `ScoreState` | Jugable | `updatesProfile` | `claimLevel` |
|---|---|---|---|---|
| sin generador | (no expuesto) | ❌ | ❌ | — |
| sonda 1-ranura falla | `INVALIDATED` | ✅ | ❌ | `NONE` |
| responder a fillers | `INVALIDATED` | ✅ | ❌ | `NONE` |
| `probes < 8` | `INSUFFICIENT_N` | ✅ | ❌ | `PRACTICE_SIGNAL` |
| k inestable | `CALIBRATING` | ✅ | ❌ | `TASK_PERFORMANCE` |
| r≥0.7 con span/n-back | `VALID` pero **MERGE** (no varianza doble) | ✅ | ✅ (no como eje nuevo) | `WITHIN_TASK_TREND` |
| todo OK + robusto | `VALID` | ✅ | ✅ | `WITHIN_TASK_TREND` |

## 13. Riesgo residual
- **Solape perceptual con n-back** hasta probar ortogonalidad; si r≥0.7 con span/n-back ⇒ MERGE.
- Practice-effect medio. Confusión UX "estado final" vs n-back (mitigar con copy y diseño).

## 14. Definition of Done
Generador `Ex2UpdatingGenerator` implementado en `implementedGenerators()` + tests; fillers + sonda-de-una obligatorios; dos métricas de error separadas; consume `INFRA_01`/`INFRA_06`; ortogonalidad documentada en `EX-EX2-MEM`; `memoryTerminologyScanner` verde; `registry ⟺ implemented`; compila ambos targets; Detekt verde sin regenerar baseline.
