# ADR — Alcanzabilidad de medida de las SONDAS reservadas (Enfoque · R11)

- **Fecha:** 2026-08-26
- **Fase:** Catálogo cognitivo · R11
- **Hallazgo de entrada:** `MB-CATALOG-0015` (déficit determinista del bloque E2 de baseline)
- **Autoridad reutilizada:** `MeasurementReachability` (R10), `MinSampleGate`, `MeasurementSufficiency`
- **Estado:** decidido antes de escribir código

---

## 1. FASE 0 — verificación del enunciado contra el repo

`MB-CATALOG-0015` afirma que el bloque E2 del baseline reparte 12 ensayos incongruentes frente a un
piso ratificado de 20, de forma determinista para todo usuario y toda semilla. **Los siete símbolos
comprobables del enunciado son CIERTOS:**

| # | Símbolo | Verificado |
|---|---|---|
| 1 | `BaselineProtocols.INITIAL` bloque E2 → `targetTrialCount = 60` | ✅ `BaselineProtocol.kt:100` |
| 2 | `calibrationDifficulty` no fija `interference` → `AxisLevel.MIN` | ✅ los nueve ejes por defecto son `MIN` |
| 3 | `E2DifficultyMapping` → `incongruentProportion = lerp(0.20, 0.60, interference)` = **0.20** | ✅ `E2FiltroGenerator.kt:29` |
| 4 | `conditionQuota(60, 0.20, 0)` = `round(12.0)` = **12** → celdas 12/48 | ✅ `ConditionSchedule.kt:49` |
| 5 | `MinSampleGate` `CONGRUENCY_EFFECT` = **20/celda**, `requiresAllCells = true` | ✅ `MinSampleGate.kt:48` |
| 6 | Solo los HIT alimentan las celdas ⇒ la celda realizada solo puede ser **menor** que 12 | ✅ `DefaultMetricsEngine.kt:428` |
| 7 | La aritmética del gate de cierre (interference 0.75 → 30/30) | ✅ recalculada, correcta |

### 1.1 Lo que el enunciado NO contiene, y refuta su propia severidad

El hallazgo describe el déficit como que «la elegibilidad de una afirmación de medida» queda
comprometida. **Eso no es cierto hoy**, y la traza completa del consumidor lo demuestra:

- `BaselineAssembler.assemble` construye el snapshot con `metrics = probeBlock.metrics`
  (`BaselineAssembler.kt:49`) — es decir, con el **bloque de sonda**, nunca con los bloques de
  calibración. El `CONGRUENCY_EFFECT` de E2 **no entra** en `BaselineSnapshot.perMetric`.
- `DefaultCalibrationEngine.initialDifficulty` deriva la dificultad inicial **solo de accuracy**
  (`valid`/`correct`, `DefaultCalibrationEngine.kt:47-61`). No lee la métrica gobernante.
- `calibrate` deriva umbrales de la distribución de RT de calibración, no de la comparación.

⇒ **El `CONGRUENCY_EFFECT` del bloque E2 de baseline se calcula, se sella correctamente con
`measurementSufficient = false`, y no lo consume nadie.** El déficit es real y su consecuencia hoy
es NULA. Registrar esto es la mitad honesta del trabajo: una hipótesis previa de esta misma fase
—que el valor infra-muestreado se colaba como el lado `pre` del RCI— se comprobó y resultó **FALSA**.

### 1.2 Lo que sí es consecuente: la misma aritmética, en la SONDA

`ProbeBank.fixedProbeDifficulty` fija `perceptualLoad = 0.4` y `temporalUncertainty = 0.4`, y
**deja `interference` en `MIN`** (`ProbeBank.kt:20-23`). `SELECTIVE_PROBE` usa esa dificultad y su
paradigma es `FLANKER`, que `ProbeExerciseMapping` enruta al generador de `E2_FILTRO`. Por tanto la
sonda selectiva corre a `incongruentProportion = 0.20`, exactamente como el bloque de entrenamiento.

La diferencia es qué cuelga de ella. Una sonda reservada es **el único instrumento que el charter
admite para una afirmación de capacidad** (invariante #7: mejora de capacidad solo vía sonda no
entrenada + RCI). La capacidad de `SELECTIVE_PROBE` es `SELECTIVE_FILTERING`, cuya métrica
gobernante es `CONGRUENCY_EFFECT` (`ExerciseCatalog`, E2). A la longitud de sonda del protocolo
(55 ensayos) la celda es **11 frente a un piso de 20**.

⇒ **La sonda selectiva no puede sostener la afirmación para la que existe.** Está registrada en
`ProbeBank.all` (la sirven `InMemoryProbeBankRepository` y `LocalFocusRepositories`) pero
`BaselineProtocols.INITIAL` solo lleva `VIGILANCE_PROBE`, así que el defecto está **armado detrás
de que nadie la ha cableado todavía** — el mismo patrón que el RCI gateado por `confident`
(aprendizaje 2026-08-19).

---

## 2. Superficie de decisión (calculada, no razonada)

Espejo exacto de `conditionQuota(size, p, atLeast) = (size·p).roundToInt().coerceIn(…)`.

### 2.1 Estado actual

| n | interference | p | celdas | celda menor | piso | veredicto |
|---|---|---|---|---|---|---|
| 60 (bloque E2 baseline) | 0.00 | 0.20 | 12 / 48 | **12** | 20 | INALCANZABLE |
| 55 (sonda del protocolo) | 0.00 | 0.20 | 11 / 44 | **11** | 20 | INALCANZABLE |

### 2.2 Opción A — subir el tamaño, `interference` intacta (p = 0.20)

- **n mínimo = 98** (celda 20). A n = 97 la celda es 19.
- Coste: **+38 ensayos = +144 s**; el bloque pasa de 3.8 min a **6.2 min**.

### 2.3 Opción B — fijar `interference`, tamaño intacto (n = 60)

- Alcanzable en **`interference ∈ [0.32, 1.00]`**. Coste en tiempo: **0**.

| t | p | celdas | celda menor |
|---|---|---|---|
| 0.30 | 0.320 | 19 / 41 | 19 ❌ |
| 0.32 | 0.328 | 20 / 40 | 20 ✅ |
| 0.50 | 0.400 | 24 / 36 | 24 ✅ |
| **0.75** | 0.500 | **30 / 30** | **30 ✅ (óptimo)** |
| 1.00 | 0.600 | 36 / 24 | 24 ✅ |

**No monótono:** `interference = 1.00` da una celda menor de 24, **peor** que 0.75. Interferencia
máxima no es potencia máxima; el óptimo es el reparto equilibrado. (Misma forma que el no-monótono
de E5 que encontró R10.)

### 2.4 El límite superior invierte la respuesta ingenua

`MeasurementReachability` es un **límite SUPERIOR**: la celda la llenan solo los HIT, así que la
celda realizada ≤ la planificada. Con accuracy `a`, la celda planificada debe ser ≥ `ceil(20/a)`:

| accuracy | celda planificada mínima | Opción A | Opción B (n = 60) |
|---|---|---|---|
| 0.95 | 22 | n ≥ 108 | interference ≥ 0.40 |
| 0.90 | 23 | n ≥ 113 | interference ≥ 0.44 |
| 0.85 | 24 | n ≥ 118 | interference ≥ 0.48 |

⇒ **La opción ingenua falla.** «Subir el bloque a 98» planifica 20 y necesita 23 al 90 % de
accuracy: queda POR DEBAJO del piso en cuanto cuentas la atrición. La opción B a `t = 0.75`
planifica 30 contra 23 necesarios: aguanta con margen. Ningún razonamiento verbal produce esa
inversión; solo el cálculo.

---

## 3. Decisión

### 3.1 Lo que R11 hace

**Declarar y blindar por build** la alcanzabilidad de medida del protocolo de baseline y de las
sondas reservadas, con la superficie derivada de las autoridades de producción. El déficit deja de
ser una fila en un registro y pasa a ser una afirmación compilada que se **rompe** si alguien mueve
un tamaño de bloque, la dificultad de calibración o `fixedProbeDifficulty`.

### 3.2 Lo que R11 NO hace, y por qué

| No se hace | Razón |
|---|---|
| Subir el tamaño del bloque E2 | Decisión de producto (duración de sesión). Además §2.4 muestra que la cifra obvia es insuficiente. |
| Fijar `interference` en `calibrationDifficulty` o en `fixedProbeDifficulty` | Decisión psicométrica: cambia **qué mide** el baseline y la sonda, y por tanto los umbrales calibrados. |
| Gatear el lado PRE del RCI | Lo posee `MB-CATALOG-0013` y **exige cambio de esquema**: `BaselineSnapshot.perMetric` es `Map<MetricKey, Double>` y descarta el hecho de muestra. R8 ya rechazó estrechar el productor porque `perMetric` alimenta también `ArchetypeInference` (lazo de control). Fuera de alcance. |
| Crear un símbolo de producción `BaselineReachability` | **Rechazo deliberado.** Nada en runtime necesita preguntarlo: `MeasurementReachability` ya responde, y añadir un símbolo sin consumidor reproduce el anti-patrón «código cuyo único consumidor era una fase que nunca aterrizó» (aprendizaje 2026-08-18). La barrera correcta aquí es un test DERIVADO de producción, que va a ROJO al editar el protocolo. |

**Cambio de producción esperado: CERO.** R11 es declaración + evidencia + aritmética.

### 3.3 Criterio de aborto (declarado ANTES de escribir código)

Se aborta y se reconcilia, sin tocar nada, si al implementar ocurre cualquiera de estas:

1. Un test derivado contradice la superficie de §2 → la aritmética de la superficie está mal y este
   ADR se corrige antes de seguir (no se ajusta el test al número).
2. Pinnear la superficie exige mover un número de producción (una proporción, un `targetTrialCount`,
   un eje) → deja de ser declaración y pasa a ser decisión de producto: se para.
3. `VIGILANCE_PROBE` —la única sonda cableada— resulta también inalcanzable → el defecto es VIVO, no
   latente, y cambia de severidad y de fase: se para y se avisa.
4. Cualquier gate se pone en rojo por causa atribuible a este cambio.

---

## 4. Adyacencia registrada, sin acción

`EvaluationComparator` exige `measurementSufficient == true` (comparación explícita, no `!= false`).
Una métrica sin piso ratificado sella `null`, y `null == true` es falso ⇒ **una sonda cuya métrica
gobernante no tiene piso ratificado nunca puede emitir una afirmación de RCI**. Es el caso de
`INHIBITION_PROBE` (E3, gobernante `FALSE_ALARMS`, sin piso central). El comportamiento es
fail-closed y **correcto**; se pinnea para que nadie lo «arregle» al revés.

---

## 5. Correcciones a este documento

*(Se añaden aquí las divergencias encontradas al implementar, como correcciones fechadas; el
apartado que se corrige NO se reescribe en silencio.)*

- **C1 (2026-08-26).** La hipótesis inicial de la fase —que el `CONGRUENCY_EFFECT` infra-muestreado
  del bloque E2 se colaba como lado `pre` del RCI— se comprobó contra la traza completa y resultó
  **FALSA**: el snapshot se construye desde `probeBlock.metrics`. Registrado en §1.1. Es la razón
  por la que el hallazgo consecuente de esta fase es la SONDA y no el bloque de entrenamiento.

- **C2 (2026-08-26) — resultado del criterio de aborto.** Ninguna de las cuatro condiciones de §3.3
  se disparó. En particular, la (1): `ProbeMeasurementReachabilityTest` **deriva** la superficie de
  las autoridades de producción y coincide con §2 en los seis números que este documento afirma
  (celda 12 y 11; n mínimo 98 con 97 fallando; `interference` mínima 0.32; 30/30 a 0.75; 24 a 1.00),
  así que el ADR no necesitó corrección aritmética. Y la (3): `VIGILANCE_PROBE`, la única sonda
  cableada, SÍ cubre su piso, de modo que `MB-CATALOG-0019` queda **armado y no vivo**, que era la
  condición para tratarlo en esta fase en vez de escalarlo.

- **C3 (2026-08-26) — guard preexistente, no duplicado.** Al ir a mecanizar «el snapshot sale de la
  sonda» apareció que `BaselineAssemblerTest` ya lo tocaba, pero **no discriminaba**: su bloque de
  calibración llevaba `metrics = emptyMap()`, así que «solo la sonda» y «sonda fusionada con
  calibración» daban el mismo resultado. Se endureció EN SITIO (regla de R10: dos guards del mismo
  invariante son dos dueños) en vez de crear un fichero nuevo, y la mutación M3 confirma que ahora
  sí distingue.
