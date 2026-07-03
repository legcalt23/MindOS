# INFRA_01 — `ScoreResult` sellado + máquina de estados de honestidad

> **Issue P0 listo para Jira/GitHub.** Fuentes leídas: `INFRA_P0.md`, `AUDIT_V2_TECNICO.md` (INFRA-1),
> `AUDIT_V2_GROUND_TRUTH.md`. Confianza: `[hecho]` / `[inferido]` / `POR_CONFIRMAR_EN_REPO`.

---

## Título
INFRA-1 · `ScoreResult` sellado + state machine que impide construir `VALID` con `n<nMin`

## Prioridad
**P0** — desbloquea todo el catálogo (el brain-profile consumirá este tipo, nunca `Double` crudo).

## Objetivo
Un único tipo de veredicto que TODO ejercicio produce, con una state machine pura y testeable que
garantiza por construcción los invariantes `null != 0` y "solo `VALID` actualiza perfil". Hoy no
existe (`[hecho, GROUND_TRUTH §scoring]`: cada módulo su propio tipo; `ScoringEquations` emite `Double`
con `0.0` como sentinela).

## Archivos probables
- `ruta probable` `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/ScoreResult.kt` (NUEVO)
- `ruta probable` `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/ScoreState.kt` (NUEVO)
- `ruta probable` `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/ScoreStateMachine.kt` (NUEVO)
- `ruta probable` `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/core/scoring/ScoreStateMachineTest.kt` (NUEVO)

## Dependencias
- Ninguna upstream (es la base). Downstream: INFRA_02 (alimenta `DEVICE_UNRELIABLE`), INFRA_06
  (`MinSampleGate` alimenta `INSUFFICIENT_N`/`Sampling`), doc 04 (`ProfileUpdateGate`), doc 13 (matriz
  estado×acción).

## Cambios técnicos

### Estados (los 8 pedidos)

```kotlin
enum class ScoreState {
    VALID,               // ÚNICO estado que actualiza perfil
    CALIBRATING,         // jugable, acumulando; value null
    INSUFFICIENT_N,      // n < nMin para la celda
    INVALIDATED,         // contaminado/anómalo (AGRS>0.75, quality LOW): se descarta la sesión
    DEVICE_UNRELIABLE,   // timebase no autoritativa para RT crítico, o jank real > umbral (INFRA_02)
    BANK_CONTAMINATED,   // banco memorizado/sin validar/holdout agotado (invariante 5)
    SUSPECTED_GAMING,    // detector anti-gaming REAL disparado (invariante 4)
    ABANDONED_SAFE,      // abandono en ejercicio emocional/frustración — NO culpa (regla seguridad)
}
```

### State machine pura (gates en cascada de cortocircuito)

El PRIMER gate que falla fija el estado. Orden: **seguridad** (abandono → timebase → jank → banco →
gaming → invalidación) vence a **evidencia** (n → cobertura-detector → accuracy-gate → estabilidad) que
vence a `VALID`.

```kotlin
data class ScoreInputs(
    val criticality: TimingCriticality,
    val isEmotionalOrFrustration: Boolean,
    val completed: Boolean,
    val quality: DataQuality,
    val antiGaming: AntiGaming,
    val sampling: Sampling,
    val primaryComputable: Boolean,
    val stability: MetricStability,
    val secondaryAccuracyGate: SecondaryGate,
)

object ScoreStateMachine {
    data class Verdict(val state: ScoreState, val updatesProfile: Boolean,
                       val preliminary: Boolean, val claimLevel: ClaimLevel)

    fun evaluate(inputs: ScoreInputs): Verdict {
        // --- SEGURIDAD (cortocircuito) ---
        if (!inputs.completed && inputs.isEmotionalOrFrustration)
            return noUpdate(ScoreState.ABANDONED_SAFE, ClaimLevel.NONE)
        if (inputs.criticality == TimingCriticality.RT_CRITICAL && !timebaseTrusted(inputs.quality))
            return noUpdate(ScoreState.DEVICE_UNRELIABLE, ClaimLevel.PRACTICE_SIGNAL)        // R1 + invariante 2
        if (inputs.criticality != TimingCriticality.RT_IRRELEVANT && jankExcessive(inputs.quality))
            return noUpdate(ScoreState.DEVICE_UNRELIABLE, ClaimLevel.PRACTICE_SIGNAL)        // jank real (INFRA_02)
        if (bankContaminated(inputs.quality.itemBankIntegrity))
            return noUpdate(ScoreState.BANK_CONTAMINATED, ClaimLevel.PRACTICE_SIGNAL)        // invariante 5
        if (!inputs.antiGaming.isClean)
            return noUpdate(ScoreState.SUSPECTED_GAMING, ClaimLevel.NONE)                    // invariante 4
        if (inputs.quality.invalidatedBy != null)
            return noUpdate(ScoreState.INVALIDATED, ClaimLevel.NONE)

        // --- EVIDENCIA ---
        if (!inputs.primaryComputable)
            return noUpdate(ScoreState.CALIBRATING, ClaimLevel.PRACTICE_SIGNAL)              // null!=0
        if (!inputs.sampling.meetsMinN)
            return noUpdate(ScoreState.INSUFFICIENT_N, ClaimLevel.PRACTICE_SIGNAL)
        if (!inputs.antiGaming.detectorCoverage.adequateFor(inputs.criticality))
            return noUpdate(ScoreState.CALIBRATING, ClaimLevel.PRACTICE_SIGNAL)              // detector ausente (invariante 4)
        if (inputs.criticality == TimingCriticality.RT_CRITICAL && !inputs.secondaryAccuracyGate.passes)
            return noUpdate(ScoreState.CALIBRATING, ClaimLevel.PRACTICE_SIGNAL)
        if (inputs.stability == MetricStability.UNSTABLE)
            return noUpdate(ScoreState.CALIBRATING, ClaimLevel.TASK_PERFORMANCE)

        // --- VALID: ÚNICA vía a profile-update ---
        val robust = inputs.sampling.confidence >= ROBUST_CONFIDENCE && inputs.sampling.crossSessionAggregated
        return Verdict(ScoreState.VALID, updatesProfile = true, preliminary = !robust,
            claimLevel = if (robust) ClaimLevel.WITHIN_TASK_TREND else ClaimLevel.TASK_PERFORMANCE)
    }

    private fun noUpdate(s: ScoreState, c: ClaimLevel) = Verdict(s, false, true, c)
    private fun timebaseTrusted(q: DataQuality) = q.deviceTimingQuality == TimebaseQuality.AUTHORITATIVE
    private fun jankExcessive(q: DataQuality) = q.criticalFrameJitter || q.frameDropFraction > MAX_FRAME_DROP
    private fun bankContaminated(b: BankIntegrity) = when (b) {
        BankIntegrity.CONTAMINATED, BankIntegrity.HOLDOUT_EXHAUSTED -> true
        BankIntegrity.OK, BankIntegrity.SMALL_UNVALIDATED, BankIntegrity.PROCEDURAL, BankIntegrity.NOT_APPLICABLE -> false
    }
    private const val ROBUST_CONFIDENCE = 0.7   // espejo aggregator establishedConfidence [hecho]
    private const val MAX_FRAME_DROP = 0.10
}
```

### Factory que impide `VALID` con `n<nMin`

```kotlin
object ScoreResultFactory {
    /** ÚNICA vía de construcción. La state machine es la autoridad; este wrapper sella la invariante. */
    fun from(adapterOutput: AdapterOutput): ScoreResult {
        val verdict = ScoreStateMachine.evaluate(adapterOutput.inputs)
        // Defensa en profundidad: VALID exige meetsMinN y señal presente.
        check(verdict.state != ScoreState.VALID ||
              (adapterOutput.inputs.sampling.meetsMinN && adapterOutput.primarySignal != null)) {
            "VALID con n<nMin o sin señal es imposible por construcción."
        }
        return ScoreResult(
            primarySignal = if (verdict.state == ScoreState.VALID) adapterOutput.primarySignal else null,
            state = verdict.state, updatesProfile = verdict.updatesProfile,
            preliminary = verdict.preliminary, claimLevel = verdict.claimLevel,
            /* ...resto de campos del adaptador... */
        )
    }
}
```

### `canUpdateProfile()`

```kotlin
fun ScoreResult.canUpdateProfile(): Boolean = updatesProfile   // == (state == VALID), garantizado en init
```

## Criterios de aceptación
- Imposible construir `VALID` con `n < nMin` (factory + `require`/`check` + `init`).
- `Contaminated`/`UntrustedTimebase`/`InsufficientData` mapean a UI "calibrando"/"datos insuficientes",
  **nunca** a percentil bajo (invariante 1).
- Transiciones legales: `CALIBRATING → VALID` solo al agregar evidencia inter-sesión; nunca
  `VALID → ` por debajo de su propio `nMin` (no degradar silenciosamente).
- `updatesProfile == true` ⟺ `state == VALID` (un único `return` lo pone true).
- Ningún estado no-`VALID` produce `primarySignal != null`.

## Tests obligatorios
- State machine exhaustiva: cada estado alcanzable + cada gate de cortocircuito.
- `RT-critical sin timebase autoritativa ⇒ DEVICE_UNRELIABLE` y `updatesProfile=false`.
- `gaming flag real ⇒ SUSPECTED_GAMING` no-update; `detector ausente en RT-crítico ⇒ CALIBRATING` (no VALID).
- `n=nMin-1 ⇒ INSUFFICIENT_N` con `primarySignal=null`.
- `banco CONTAMINATED/HOLDOUT_EXHAUSTED ⇒ BANK_CONTAMINATED`.
- `abandono emocional ⇒ ABANDONED_SAFE` sin penalizar.
- **Property test (Kotest `Arb` sobre `ScoreInputs`):** `updatesProfile ⇒ state==VALID` SIEMPRE; ningún
  no-VALID emite número. (Property-based hoy ausente en mindboost `[hecho, GROUND_TRUTH §tests]`.)
- Canario de serialización: no-VALID ⇒ `primaryValue=null` en el DTO.

## Riesgos
- Reconciliar 2 escalas (0..1 Speed/perfil vs 0..100 Focus/ICR) y 2 banderas (`preliminary` vs
  `confident`) `[hecho, GROUND_TRUTH §scoring]`: el adaptador normaliza, no la state machine.
- `LongParameterList` en `ScoreResult` → mitigado con sub-bloques (`ScoreProvenance`/`DataQuality`/`Sampling`).

## Definition of Done
- [ ] Tipos + state machine + factory creados en `core/scoring/`.
- [ ] Suite de state machine + property test verde.
- [ ] `:shared:detekt` sin nuevos findings.
- [ ] `:shared:desktopTest` verde (tests pesados en `desktopTest`, no escaneado por Detekt).
- [ ] Cero cambios en `FocusScore`/`CognitiveProfileScore`/`RciResult` (golden existentes intactos).

## Comando/check de validación
```
:shared:compileKotlinDesktop · :shared:desktopTest · :shared:detekt
```
