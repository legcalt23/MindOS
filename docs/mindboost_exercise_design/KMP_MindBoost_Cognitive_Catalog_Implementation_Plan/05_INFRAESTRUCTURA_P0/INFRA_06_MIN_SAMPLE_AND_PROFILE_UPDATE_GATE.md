# INFRA_06 — `MinSampleGate` central + `ProfileUpdateGate`

> **Issue P0 listo para Jira/GitHub.** Fuentes: `AUDIT_V2_TECNICO.md` (INFRA-6 + §Thresholds),
> `AUDIT_V2_GROUND_TRUTH.md` (§scoring min-n), `INFRA_P0.md`. Confianza: `[hecho]` / `[inferido]` /
> `POR_CONFIRMAR_EN_REPO`.

---

## Título
INFRA-6 · `MinSampleGate` central (nMin/celda, nEffective, null/calibrating, agregación inter-sesión,
IC) + `ProfileUpdateGate`

## Prioridad
**P0** — los thresholds dispersos actuales están **por debajo** de lo que exige la auditoría.

## Objetivo
Tabla única por métrica que defina `nMin`, `nEffective`, condición de `null`/`calibrating`, regla de
agregación inter-sesión y etiqueta de IC. Hoy hay `MIN_N=5/3/3/6/3` dispersos en
`DefaultMetricsEngine` `[hecho, GROUND_TRUTH §scoring]` — todos por debajo de los umbrales pedidos
(comparativas ≥20/celda; SSRT nStops≥30; Brier ≥40-60; RCI |>1.96|).

## Archivos probables
- `ruta probable` `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/MinSampleGate.kt` (NUEVO)
- `ruta probable` `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/ProfileUpdateGate.kt` (NUEVO)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/metrics/DefaultMetricsEngine.kt` (EXISTE — consumir)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/MetricThresholds.kt` (EXISTE)
- `ruta probable` `shared/src/commonTest/.../core/scoring/MinSampleGateTest.kt` (NUEVO)

## Dependencias
- INFRA_01 (alimenta `Sampling`/`INSUFFICIENT_N`), INFRA_04 `[por_confirmar — banco/holdout, no en este issue]` para `BANK_CONTAMINATED`.

## Cambios técnicos

### Tabla de thresholds (golden) `[hecho, AUDIT §Thresholds]`

| Métrica | `nMin` | `nEffective` (def) | `null`/`calibrating` | Agregación inter-sesión | IC |
|---|---|---|---|---|---|
| Comparativas (CONGRUENCY_EFFECT, SWITCH_COST, PRESSURE_DELTA, DUAL_TASK_COST, POST_ERROR) | **≥20/celda** | trials por celda tras filtros | null si falta **cualquier** celda | suma celdas correctamente | **obligatorio** |
| SSRT | **nStops ≥30** | stops con `p(respond\|signal) ∈ [0.25,0.75]` | null fuera de banda (waiting-strategy) | acumula stops | sí |
| d' (n-back, change-detection, RSVP) | bloque criterio no-extremo, n≥20 | max n con `d'≥1.0` | descartar bloques criterio extremo | por bloque | sí |
| Brier / calibration error | **≥40-60 juicios** | juicios agregados inter-sesión | null en sesión única | **siempre inter-sesión** | sí |
| RCI | ≥2-3 administraciones | — | `NotEnoughData` por defecto | pre/post equated | `\|RCI\|>1.96` reliable |
| BUR (bayes) | n=12 | trials con priori≠posterior | excluir priori=posterior | rolling | sí |
| Cowan's K | — | `c*(hit−fa)` con d' honesto | banco rotado | — | sí |
| Accuracy/RT base (Focus) | ≥20 (`MATURE_N=60`) `[hecho]` | trials confident | `confident=false` | weighted by trials `[hecho]` | banda FocusScore |

```kotlin
data class MinSampleSpec(
    val metric: MetricId, val nMinPerCell: Int, val requiresAllCells: Boolean,
    val crossSessionAllowed: Boolean, val requiresConfidenceInterval: Boolean,
)

object MinSampleGate {
    private val specs: Map<MetricId, MinSampleSpec> = /* tabla anterior como golden */ TODO()

    /** Devuelve estado de muestreo. NUNCA un número si no se cumple nMin (null != 0). */
    fun evaluate(metric: MetricId, nEffective: Int, cellsPresent: Boolean): SampleVerdict {
        val spec = specs.getValue(metric)
        return when {
            spec.requiresAllCells && !cellsPresent -> SampleVerdict.InsufficientData(spec.nMinPerCell, nEffective)
            nEffective < spec.nMinPerCell -> SampleVerdict.InsufficientData(spec.nMinPerCell, nEffective)
            else -> SampleVerdict.Sufficient(nEffective, requiresIc = spec.requiresConfidenceInterval)
        }
    }
}

sealed interface SampleVerdict {
    data class Sufficient(val nEffective: Int, val requiresIc: Boolean) : SampleVerdict
    data class InsufficientData(val nNeeded: Int, val nHave: Int) : SampleVerdict   // → ScoreState.INSUFFICIENT_N
}
```

### `ProfileUpdateGate`

```kotlin
/** ÚNICO punto que escribe en el perfil. Consume SOLO ScoreResult (nunca Double crudo). */
object ProfileUpdateGate {
    fun apply(result: ScoreResult, aggregator: MindBoostProfileAggregator) {
        if (!result.canUpdateProfile()) return        // jugable, no toca perfil (ausencia, no valor bajo)
        aggregator.contribute(result.toModuleContribution())
    }
}
```

## Criterios de aceptación
- Toda métrica comparativa exige `nMin ≥ 20`/celda o retorna `InsufficientData` (no 0).
- SSRT exige `nStops ≥ 30`; Brier exige `≥40` juicios agregados inter-sesión (null en sesión única).
- IC reportado en todo `Sufficient` comparativo.
- `ProfileUpdateGate` no llama al aggregator si `canUpdateProfile()==false`.

## Tests obligatorios
- Tabla de thresholds como **golden** (cambiar un umbral rompe el test).
- `n por debajo de nMin ⇒ InsufficientData`, nunca 0 (canario `null != 0`).
- Agregación inter-sesión suma celdas correctamente; falta de una celda ⇒ null.
- `ProfileUpdateGate` con veredicto no-VALID ⇒ aggregator NO invocado (mock/spy).

## Riesgos
- Subir `nMin` puede dejar más sesiones en `CALIBRATING` (UX); es el comportamiento honesto correcto.
- Coherencia con los `MIN_N` existentes de `DefaultMetricsEngine`: este gate los **reemplaza** como
  fuente de verdad; no duplicar.

## Definition of Done
- [ ] `MinSampleGate` + `ProfileUpdateGate` creados; `DefaultMetricsEngine` consume el gate.
- [ ] Golden de thresholds + canarios verde.
- [ ] `:shared:detekt` + `:shared:desktopTest` verdes.

## Comando/check de validación
```
:shared:compileKotlinDesktop · :shared:desktopTest · :shared:detekt
```
