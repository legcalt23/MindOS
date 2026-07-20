# S4 — Informe de acoplamiento (Tracer B «Centinela» sobre el motor de S3)

> Focus V5 · Slice S4 · tarea (4). Estado alcanzado: `INTERNAL_TECHNICAL_TRACER` ×2 + `ENGINE_GENERALIZED`.
> El tracer **NO está montado** (`focus2Enabled` no existe). iOS **NO EJECUTADO** (sin macOS).

## 0. Qué prueba este informe

El punto entero del Tracer B es responder una pregunta con evidencia, no con confianza: **¿el motor de medición
que construyó S3 es genérico, o es en secreto el motor del flanker?** La forma de saberlo es construir una segunda
mecánica —vigilancia continua (FOC2-E01 Centinela)— sobre EL MISMO motor y anotar **cada cambio que esa mecánica
obligó a hacerle al motor**. Un motor genuinamente general los tolera con un diff casi vacío; uno acoplado
al flanker exige reescrituras. Este documento enumera todos los cambios, cada uno justificado, y declara el
presupuesto (plan §5/S4: «si >N cambios estructurales, el motor se revisa antes de S5»).

**Veredicto de presupuesto: el motor exigió exactamente UN ensanche de tipo (cero cambio de comportamiento).**
Muy dentro de presupuesto.

## 1. El núcleo del motor — CERO ediciones

Estos archivos son el motor de medición. Ninguno se editó en S4 (el paquete `focus2/` es WIP untracked
—working-tree desde S1, sin commitear— así que no hay `git diff` contra S3; la no-edición es verificable por
inspección: el núcleo no contiene código de vigilancia, no se abrió para edición, y los ~3760 tests de S3 más
el golden del flanker pasan sin cambios):

| Archivo | Rol | Editado en S4 |
|---|---|---|
| `focus2/domain/runtime/FocusSessionReducer.kt` | reductor puro (clasificación por trial) | **NO** |
| `focus2/domain/runtime/FocusSessionRuntime.kt` | shell con estado sobre el reductor | **NO** |
| `focus2/domain/runtime/FocusRuntimeModel.kt` | snapshot serializable + inputs + `TrialFacts` | **NO** |
| `focus2/domain/runtime/FocusSessionRestore.kt` | restore fail-closed | **NO** |
| `focus2/domain/session/FocusSessionLifecycle.kt` | máquina de estados (tabla de transición) | **NO** |
| `focus2/domain/scoring/DescriptiveScorer.kt` | scorer descriptivo (accuracy/d′/criterio por condición) | **NO** |
| `focus2/domain/scoring/DescriptiveModel.kt` | vocabulario de medida (`SdtCell`, `TaskSpec`, `Verdict`) | **NO** |
| `focus2/domain/telemetry/*` | catálogo de eventos + reconciliación | **NO** |

La razón por la que el núcleo no se movió es de diseño: el reductor consume `(snapshot, input, TaskSpec)` y el
shell le pasa `List<TrialFacts>`. Ambos son paradigma-neutrales por construcción —una decisión que S3 tomó
explícitamente (`FocusRun.trialFacts` ya narrowa a `List<TrialFacts>` «so the engine doesn't grow a dependency
on the flanker generator that S4's vigilance tracer could not satisfy»)—. La vigilancia se expresó enteramente
como un `TaskSpec` distinto (`SentinelTask.SPEC`) y un stream de `TrialFacts` con `ConditionId.VIGILANCE`. El
scorer ya anticipaba el caso go-only (`ConditionId.VIGILANCE` ya existía; `chanceLevel=null`, `yes=TAP`,
`no=NONE` documentados en su KDoc). **No hubo que tocar una sola línea de medición.**

Demostrado end-to-end por `SentinelEngineGeneralizationTest`: el MISMO `FocusSessionRuntime` envolviendo el
MISMO `FocusSessionReducer` corre una sesión de vigilancia de 120 trials y produce una medida `Valid` que
discrimina, puntuada desde las tallies propias del motor (`snapshot.cells()`), sin código de vigilancia en el
motor.

## 2. El ÚNICO cambio estructural del motor — `MB-FOCUS-0040`

**Ensanche del campo `spec` del descriptor sellado, de `VectorFilterSpec` a la interfaz neutral `FocusExerciseSpec`.**

- **Qué se descubrió.** `FocusSessionDescriptor` (el sello pre-trial) almacenaba un `VectorFilterSpec` concreto
  —con `flankerCount`, `separation`, `similarity`—, pero el motor lee de él **exactamente dos escalares**:
  `responseWindowMs` (`FocusSessionRuntime` arma cada deadline) y `totalTrials`
  (`FocusSessionRestore.incoherent` valida el índice del snapshot). Todo lo demás del `VectorFilterSpec` es
  invisible al motor. El descriptor **sobre-comprometía**: nombraba el tipo concreto del Tracer A donde sólo
  necesitaba ese par neutral, y una sesión de vigilancia no podía sellarse sin fabricar parámetros de flanker
  que no tiene.
- **La corrección.** Nueva `@Serializable sealed interface FocusExerciseSpec { responseWindowMs; totalTrials }`
  (`focus2/domain/generator/FocusExerciseSpec.kt`); `VectorFilterSpec` y `SentinelSpec` la implementan;
  `FocusSessionDescriptor.spec` pasa a la interfaz.
- **Por qué es un ensanche, no un cambio de comportamiento.** `FocusSessionRuntime`, `FocusSessionRestore` y
  `FocusSessionReducer` **no se editaron**: ya leían `descriptor.spec.responseWindowMs` /
  `descriptor.spec.totalTrials`, ambos miembros de la interfaz. `FocusRunPlanner.prepare` tampoco (pasa un
  `VectorFilterSpec`, que ES un `FocusExerciseSpec`). El diff del motor de comportamiento es literalmente vacío;
  sólo cambia una firma de tipo en el descriptor.
- **Seguridad de serialización.** El `spec` serializa polimórficamente (jerarquía sellada cerrada, con
  `@SerialName` estable por impl). Round-trip preservado (`FocusSessionDescriptorTest`: el descriptor sellado con
  `VectorFilterSpec` Y el sellado con `SentinelSpec` sobreviven `decode(encode(x))==x`). El restore sigue
  fail-closed: `Json{ignoreUnknownKeys=false}` hace que un `spec` escrito por un build más nuevo **falle el
  decode** (rechazo) en vez de resolver a un default. Sin registros en producción (feature no montada) ⇒ sin
  migración.

## 3. Acoplamiento de presentación corregido — `MB-FOCUS-0041`

**`FocusTracerStateHolder` leía un campo específico del flanker (`descriptor.spec.interStimulusDelayMs`).**

- **Qué se descubrió.** El loop de sesión (`FocusTracerStateHolder.kt:107`) sacaba el ISI de un campo de
  sesión del `VectorFilterSpec`, que la interfaz neutral no expone (ni debe: el ISI de vigilancia es **por-trial
  jittered**, no de sesión — meterlo en la interfaz falsearía el Sentinel).
- **La corrección.** El loop ahora lee el ISI resuelto por-trial del plan
  (`run.plan.trials[index].interStimulusDelayMs`), que es la fuente honesta («registro por-trial del parámetro
  resuelto»). La ventana de respuesta sigue leyéndose del descriptor SELLADO (autoridad de medición, en la
  interfaz). **Behavior-idéntico para el flanker** (su ISI es constante), verificado por
  `FocusTracerStateHolderTest` (21/21 verde).

## 4. No-cambios: módulos del ejercicio B, no ediciones del motor

- **`DeterministicPlanDraw` (nuevo).** El shuffle Fisher-Yates pineado y el bounded-draw por rechazo se
  extrajeron de `VectorFilterPlanner` a un helper compartido del paquete `generator`. Es dedup de un algoritmo
  crítico de determinismo (el repo advierte repetidamente contra «the copy that drifts»), **no** un cambio del
  motor. Byte-idéntico: el golden de secuencia independiente del flanker
  (`VectorFilterPlannerTest.the_plan_permutation_is_pinned_against_an_independently_computed_sequence`) sigue
  verde, lo que prueba que la permutación no cambió.
- **`SentinelRunPlanner` / `SentinelRun` / `SentinelTask` (nuevos).** Son los módulos del ejercicio B (el
  «generador-wrapper» + la spec de clasificación). Espejan `FocusRunPlanner`/`FocusRun`/`VectorFilterTask`
  reusando `FocusSessionDescriptor.seal` (motor, sin cambios) y `SentinelPlanner`. **No** modifican el motor;
  son exactamente lo que el plan permite («SOLO módulos del ejercicio B»).

## 5. Acoplamiento DIFERIDO (declarado, fuera de presupuesto S4)

Estos son reales y honestos; NO se resolvieron en S4 porque pertenecen al montaje (S5+), no al techo de tracer
técnico interno:

1. **La capa de presentación sigue atada al Tracer A.** `FocusTracerStateHolder` y `FocusTracerScreen` sostienen
   un `FocusRun`/`VectorFilterPlan` y renderizan la mecánica de elección. Correr el Sentinel end-to-end en la UI
   exigiría generalizar el holder/screen (o un holder paralelo). **Diferido**: el Sentinel se demuestra a nivel
   de MOTOR (dominio), que es lo que S4 debe probar; el renderer single-tap de vigilancia y la generalización de
   la UI son montaje. La demostración `ENGINE_GENERALIZED` no pasa por la UI.
2. **Restore de una sesión Sentinel.** `EngineVersions.CURRENT` está fijado al Tracer A; restaurar un Sentinel
   necesitaría su propio `EngineVersions` (hoy daría `IDENTITY_MISMATCH` fail-closed, correcto). Es trabajo de
   montaje.
3. **Bloqueantes de montaje pre-existentes** (heredados de S3, siguen OPEN): `MB-FOCUS-0026` (UI de declaración
   de modalidad), `MB-FOCUS-0028` (recovery scanner global), `MB-FOCUS-0035` (single-instance por usuario).

## 6. Conclusión

El Tracer B se construyó sobre el motor de S3 con **un (1) ensanche de tipo del descriptor** (cero cambio de
comportamiento del núcleo) y **una (1) corrección de acoplamiento de presentación** (behavior-idéntica para el
flanker). El núcleo de medición —reductor, runtime, snapshot, restore, scorer, telemetría— no se movió, y una
sesión de vigilancia completa corrió por él end-to-end. **`ENGINE_GENERALIZED` queda demostrado, no afirmado.**
El presupuesto de cambios estructurales (1) está muy por debajo del umbral de revisión pre-S5.
