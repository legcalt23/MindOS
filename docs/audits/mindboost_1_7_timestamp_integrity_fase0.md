# FASE-0 — Integridad temporal del perfil (1.7 timestamps reales)

- **Fecha:** 2026-07-03
- **Estado:** INVENTARIO (sin cambios de código)
- **Criterio CEO:** MindBoost no debe fabricar actualidad. Si no sabemos cuándo ocurrió una evidencia,
  el sistema debe decir "timestamp desconocido/no fiable", nunca inventar tiempo.

## Hallazgo central (determina el riesgo real)

`ModuleContribution.updatedAtMs` **hoy NO se consume**. El agregador (`MindBoostProfileAggregator.
buildDimensionState`) nunca lo lee; `DimensionState` no tiene campo de tiempo; el DTO/persistencia
sólo guarda el `updatedAtMs` **del perfil** (nivel agregado), no el de cada contribución. Por tanto:

- **Riesgo actual (vivo):** BAJO — ningún `0L` fabricado es visible al usuario ni entra al scoring hoy.
- **Riesgo forward (real):** ALTO si algún día se surfacea "última medición"/recencia/orden temporal:
  los `0L` (época 1970) se mezclarían con tiempos reales (Focus/Logic/Speed) como si fueran actuales.
  Es exactamente "fabricar actualidad" latente. 1.7 lo cierra antes de que se convierta en mentira.

El tiempo **de cálculo** del perfil (`MindBoostProfile.updatedAtMs = clock.nowMillis()`) YA es real:
lo inyecta `EpochClock` en `MindBoostProfileService` (prod = `SystemEpochClock`). No se fabrica.

## Reloj canónico: ya existe

`core/time/EpochClock` (`fun interface { nowMillis(): Long }`) + `SystemEpochClock` (ÚNICO punto que
toca `Clock.System`) es el proveedor inyectado y testeable que pide el criterio #5. `MindBoostProfile
Service` ya lo usa para el sello de cálculo. **kotlinx-datetime 0.7.1:** compatible — el único
`Clock.System.now()` del código vive aislado en `SystemEpochClock`; los tests inyectan un fake y no
tropiezan con el `NoClassDefFoundError` (learning 2026-05-29). **1.7 no debe añadir ningún
`Clock.System` nuevo en las fuentes.**

## Tabla por ProfileSource

| Source | Dimensión | Timestamp hoy | Origen del valor | Categoría | Riesgo | Corrección |
|---|---|---|---|---|---|---|
| `FocusProfileSource` | ATTENTION | `profile?.updatedAtMs ?: latest.endedAtMs` | fin de sesión real | **evento real** | ninguno | mantener |
| `LogicProfileSource` | REASONING | `sessions.first().endedAt` | fin de sesión real | **evento real** | ninguno | mantener |
| `SpeedProfileSource` | PROCESSING_SPEED | `latest.session.timestampEpochMs` | captura real | **evento real** | ninguno | mantener |
| `LanguageProfileSource` | VERBAL | `profile.updatedAtEpochMs` | projector `asOf` = máx. tiempo de packet real | **evento real** | ninguno (además bloqueada por integridad) | mantener |
| `MemoryProfileSource` (EX1 span) | WORKING_MEMORY | `timestampMs=0L` → `provenance.timestampMs` | **fabricado 0L** | **desconocido** | forward | → `null` |
| `PairsProfileSource` (EX4) | WORKING_MEMORY | `timestampMs=0L` | **fabricado 0L** | **desconocido** | forward | → `null` |
| `EpisodicProfileSource` (EX3) | EPISODIC_MEMORY | `timestampMs=0L` | **fabricado 0L** | **desconocido** | forward | → `null` |
| `DecisionsProfileSource` (E2) | DECISION_MAKING | `timestampMs=0L` | **fabricado 0L** | **desconocido** | forward | → `null` |
| `EmotionProfileSource` | EMOTIONAL_REGULATION | `updatedAtMs=0L` | surface read-only no expone tiempo | **desconocido** | forward | → `null` |
| — agregador — | (perfil) | `MindBoostProfile.updatedAtMs = nowMs` | `EpochClock` inyectado (real) | **cálculo** | ninguno | mantener |

Nota: el `0L` de las 4 fuentes gate-based nace en su llamada `*Score.scoreOf(..., timestampMs = 0L)`,
que fluye por `ProfileUpdateGate.toModuleContribution { updatedAtMs = provenance.timestampMs }`.

## Categorización pedida por el CEO

- **Evento real:** Focus/Logic/Speed/Language (fin de sesión / captura / máx. tiempo de evidencia).
- **Cálculo:** sello del perfil `MindBoostProfile.updatedAtMs` (reloj inyectado; ya real).
- **Sincronización:** no está en el path de contribución al perfil. `SessionSummary.syncStatus`
  (PENDING/SYNCED/FAILED) es de Lenguaje y no se surfacea al perfil. N/A para `ModuleContribution`.
- **Desconocido:** Memoria span/Pairs/Episodic, Decisiones, Emociones (log agregado o surface sin tiempo).

## Diseño propuesto (representacional, sin tocar scoring)

1. **`ModuleContribution.updatedAtMs: Long = 0L` → `Long? = null`.** `null` = "tiempo de evento
   desconocido/no fiable", honesto y explícito; nunca 1970 disfrazado de actual. Como el campo NO se
   consume, es un cambio de representación puro, sin impacto en scoring/estado/display.
2. **Fuentes de evento real** pasan su tiempo real (no-null): sin cambios de valor.
3. **Fuentes desconocidas** (5) pasan `null` (Memoria/Pairs/Episodic/Decisiones/Emociones).
4. **PROHIBIDO** inyectar un reloj en las fuentes para sellar "ahora": el tiempo de LECTURA no es el
   tiempo del EVENTO — sellar "now" al leer sería fabricar actualidad (peor que 0L). Fail-closed = `null`.
5. **Invariante nueva (fail-closed):** un test que exija que ninguna ProfileSource emita
   `updatedAtMs == 0L` — o hay un tiempo de evento real (>0) o `null`. Blinda "no fabricar actualidad".
6. **Opcional/diferido:** hilar el "tiempo del último trial" real para las fuentes gate-based SI su
   log lleva timestamps por-trial (verificación por store). Fuera del mínimo; hoy `null` es lo honesto.
7. **Naming (a decidir):** mantener `updatedAtMs` nullable (mínimo diff) vs. renombrar a `observedAtMs`
   (más claro, más churn en 9 sitios). Recomiendo mantener el nombre + KDoc de semántica.

## Tests mínimos

- Actualizar asserts de `0L` → `null` en los tests de Memoria/Pairs/Episodic/Decisiones/Emoción.
- Mantener asserts de tiempo real (Focus/Logic/Speed/Language) intactos.
- **Invariante fail-closed:** ninguna fuente emite `updatedAtMs == 0L` (con fixtures reales por fuente).
- (si aplica) confirmar que el sello de cálculo del perfil usa el `EpochClock` inyectado (fake en test).

## Riesgo de regresión

**BAJO.** El campo no lo consume el agregador ni la persistencia de contribución (no se persiste). El
único impacto es en tests que asertan `updatedAtMs`; los de evento real no cambian, los de `0L` pasan a
`null`. La nulabilidad del campo compila en las 9 fuentes (un `Long` encaja en `Long?`); sólo los 5
sitios `0L`/`= 0L` cambian a `null`. Sin cambio de comportamiento observable en producción.

## Recomendación final

**Slice único.** Es un cambio representacional cohesivo sobre un solo campo del modelo compartido más 5
fuentes y sus tests. Dividirlo fragmentaría un cambio de modelo atómico. El "threading de último-trial"
(punto 6) queda como follow-up opcional, no como fase separada obligatoria.

Prioridad: integridad temporal del perfil oficial — una métrica sin timestamp fiable no debe parecer
evidencia actual.
