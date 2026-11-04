# ADR — Measurement reachability is a declared function of (exercise, block size, difficulty)

- **Fase:** plan blindado del catálogo cognitivo · R10
- **Hallazgo:** `MB-CATALOG-0017` — *la dificultad mueve en silencio la elegibilidad de una afirmación de medida*
- **Fecha:** 2026-08-20
- **Estado:** decidido antes de escribir código (criterio de aborto declarado en §6)
- **Depende de:** `MB-CATALOG-0014` (R9, cuota) — sin la cuota este ADR sería imposible: con un Bernoulli
  por ensayo no hay una cifra que declarar, cada sesión trae la suya.

---

## 1. FASE 0 — verificación del enunciado contra el repo

Cada símbolo del hallazgo, verificado uno a uno. **Los 6 son CIERTOS**, y la verificación además
**precisa el mecanismo**, que el hallazgo no nombraba.

| # | Afirmación del hallazgo | Verificación | Veredicto |
|---|---|---|---|
| 1 | `DUAL_TASK_COST` tiene piso central 20 y exige todas las celdas | `MinSampleGate`: `spec(DUAL_TASK_COST, 20, allCells = true)` | **CIERTO** |
| 2 | `E8DifficultyMapping` mueve `dualProportion` con `dualTaskLoad` | `dualProportion = lerp(0.40, 0.75, d.dualTaskLoad.value)` | **CIERTO** |
| 3 | E8 pasa de ACEPTA a DENIEGA al subir la carga | n=60: celda menor 24 ⇒ 15 (piso 20) | **CIERTO** |
| 4 | E5 va en la dirección contraria | n=60: 15 ⇒ 21 | **CIERTO** (pero ver §3·H2) |
| 5 | Una puntuación de diferencia se juzga por su celda MENOR | `DefaultMetricsEngine.cellsOf` = `min(a, b)` | **CIERTO** |
| 6 | El acoplamiento no está declarado en ninguna parte | cero símbolos que relacionen dificultad con elegibilidad de medida | **CIERTO** |

### 1.1 El mecanismo, que el hallazgo no nombraba

El eje que el motor adaptativo mueve (`ExerciseCatalog.governingAxis`) es **exactamente** el eje que
gobierna la proporción de condición de cada generador. No es una coincidencia desafortunada: es
estructural.

| Ejercicio | `governingAxis` (lo que el motor mueve) | Parámetro que ese eje mueve | Métrica cuya celda es |
|---|---|---|---|
| E2_FILTRO | `INTERFERENCE` | `incongruentProportion` | `CONGRUENCY_EFFECT` |
| E4_FRENO | `INHIBITORY_DEMAND` | `stopProportion` | `SSRT` |
| E5_CONFLICTO | `INTERFERENCE` | `incongruentProportion` | `CONGRUENCY_EFFECT` |
| E6_CAMBIO | `INTERFERENCE` | `switchFrequency` | `SWITCH_COST` |
| E8_DUO | `DUAL_TASK_LOAD` | `dualProportion` | `DUAL_TASK_COST` |
| E13_PRESION | `PRESSURE` | `pressureProportion` | `PRESSURE_DELTA` |

`DefaultAdaptiveEngine.performanceDecision` hace `stepUp(gov, …)` / `stepDown(gov, …)` sobre ese eje.
Es decir: **el motor adaptativo, haciendo literalmente lo único que sabe hacer, redimensiona las celdas
de la comparación.** El hallazgo describía el síntoma; esto es la causa.

---

## 2. Corrección de alcance: la variable dominante NO es la dificultad

Al calcular la superficie completa (§3) aparece un hecho que el hallazgo **no contiene y que lo
supera en importancia**: la elegibilidad depende mucho más del **tamaño de bloque** que de la
dificultad, y el tamaño de bloque lo elige el **usuario** al escoger la duración de la sesión.

`SessionTemplate.estimatedTrialCount()` = `coreMs / 3_800`, acotado a `[24, 160]`:

| Plantilla | `coreMs` | Ensayos |
|---|---|---|
| `MAINTENANCE_5` ("Mantenimiento", 5 min) | 210 000 | **55** |
| `STANDARD_10` ("Estándar", 10 min) | 360 000 | **94** |
| `DEVELOPMENT_20` ("Desarrollo", 20 min) | 600 000 | **157** |
| `ELITE_30` ("Élite", 30 min) | 840 000 | **160** (tope) |

Más los bloques fijos de baseline/evaluación: **75** (E1), **60** (E2, E3), **55** (sonda).

A n=157/160 **todo es elegible en todo el eje**. A n=55 casi nada lo es de forma robusta. Dicho de
otra forma: *elegir «Mantenimiento» en vez de «Desarrollo» decide si la sesión puede sostener una
afirmación de medida* — y el usuario elige eso sin ninguna indicación de que tenga esa consecuencia.
Se registra como hallazgo propio (`MB-CATALOG-0018`).

---

## 3. La superficie de alcanzabilidad, CALCULADA

Celda menor = `min(round(slots·p), slots − round(slots·p))` para una puntuación de diferencia;
para SSRT (que no exige todas las celdas) = número de paradas. E6 tiene `slots = n − 1` (el ensayo 0
no tiene predecesor, así que no puede ser un cambio).

| n | E2 · CONGRUENCY | E4 · SSRT | E5 · CONGRUENCY | E6 · SWITCH | E8 · DUAL_TASK | E13 · PRESSURE |
|---|---|---|---|---|---|---|
| **55** | eje ≥ 0.39 | **NUNCA** | eje 0.27–0.98 | eje ≥ 0.21 | eje ≤ 0.70 ⚠ | siempre |
| **60** | eje ≥ 0.32 | **NUNCA** | eje ≥ 0.19 | eje ≥ 0.11 | eje ≤ 0.78 ⚠ | siempre |
| **75** | eje ≥ 0.15 | **NUNCA** | eje ≥ 0.03 | siempre | eje ≤ 0.97 ⚠ | siempre |
| **94** | eje ≥ 0.02 | eje ≥ 0.80 | siempre | siempre | siempre | siempre |
| **157** | siempre | siempre | siempre | siempre | siempre | siempre |
| **160** | siempre | siempre | siempre | siempre | siempre | siempre |

⚠ = **INVERTIDO**: más difícil ⇒ deja de ser medible.

### Tres hechos que el hallazgo no contenía

- **H1 · El tamaño de bloque domina.** Ver §2. `MB-CATALOG-0018`.
- **H2 · E5 a n=55 es NO MONÓTONO.** Elegible sólo en `[0.27, 0.98]`: deniega en **ambos** extremos.
  A interferencia máxima la celda *congruente* cae a **19**, uno por debajo del piso. Nadie adivinaría
  que «más difícil» hace la medición imposible por un solo ensayo. Ningún razonamiento verbal produce
  este hecho; sólo el cálculo.
- **H3 · SSRT sí es alcanzable, a partir de n=94.** `MB-CATALOG-0016` decía «inalcanzable en un bloque
  de 60 ensayos, a cualquier dificultad» — CIERTO, y ahora se puede precisar: también inalcanzable a
  n=55, alcanzable a n=94 con `inhibitoryDemand ≥ 0.80`, y siempre a n≥157. El hallazgo se actualiza
  con el cruce exacto en vez de quedarse en el techo parcial.

---

## 4. Diagnóstico

No hay ningún defecto de cálculo que corregir: cada pieza hace bien su trabajo. El defecto es de
**declaración**. Existen tres hechos —el piso ratificado, la proporción que la dificultad dicta, y el
tamaño de bloque que la plantilla dicta— que juntos determinan si una afirmación sobre el usuario
puede existir, y **ningún símbolo del repo relaciona los tres**. La consecuencia es la familia
`null != 0` en su forma atributiva: cuando `measurementSufficient` sale `false`, hoy se confunden dos
causas muy distintas —

1. la sesión de este usuario se quedó corta (hecho sobre el usuario), y
2. **la forma planificada nunca pudo haber llegado** (hecho sobre nuestro diseño),

— y atribuir (2) al usuario es exactamente lo que el charter prohíbe.

---

## 5. Decisión

**Se DECLARA la alcanzabilidad; no se cambia ningún número.**

1. **`MeasurementReachability`** (nuevo, `focus/domain/metrics/`): autoridad pura que responde
   `verdictFor(exerciseId, trialCount, difficulty)` con la celda menor planificada, el piso ratificado
   y el veredicto. Tri-estado como `MeasurementSufficiency`: `null` = «este ejercicio no tiene métrica
   comparativa con piso ratificado, la pregunta no aplica» — nunca reutilizado como «no».
2. **Cero aritmética duplicada.** La autoridad **llama a `conditionQuota`** (la misma función que
   el generador usa) y cuenta; no reimplementa `round(n·p)`. Una segunda implementación de la cuota
   sería una segunda fuente de verdad, que es el defecto que R7/R8/R9 llevan tres fases cerrando.
3. **El enlace ejercicio → proporción SÍ se verifica.** Que la aritmética coincida por construcción es
   correcto; que el *cableado* coincida no puede darse por bueno. El test ejecuta el **generador real**
   para `(id, n, dificultad)`, cuenta los estímulos marcados de verdad, y exige que coincidan. Ese
   check no es vacuo: el generador podría ignorar su propio mapeo.
4. **El set se DERIVA, no se enumera** — de `MeasurementSufficiency.central`, igual que en R9. Añadir
   una métrica comparativa al catálogo sin decidir cómo se extrae su celda **rompe el build**.
5. **La tabla de la superficie (§3) se pinnea** a los tamaños de bloque de producción, derivados de
   `SessionTemplate` y `BaselineProtocol`, no escritos a mano. Cambiar un `coreMs`, un rango de
   proporción o un piso central mueve la tabla y rompe el build.
6. **`ConditionScheduleTest` deja de llevar su propia copia** del mapa ejercicio→condición y lee la
   autoridad de producción. R9 dejó esa tabla en el test; promoverla es lo que la convierte en una
   declaración y elimina la segunda fuente.

---

## 6. Criterio de aborto (declarado ANTES de implementar)

**La autoridad debe reproducir, para TODA tripleta (ejercicio, n, dificultad) alcanzable en
producción, EXACTAMENTE el conteo de celda que el generador real produce.** Si diverge en un solo
punto, la autoridad es una segunda fuente de verdad y este diseño se abandona en vez de parchearse.

Se verifica ejecutando los seis generadores reales sobre la rejilla completa y comparando conteos.

---

## 7. Lo que este ADR NO decide

Deliberadamente fuera de alcance, con dueño registrado:

- **No se toca ninguna proporción ni ningún rango de eje.** Acotar `dualProportion` para que ambas
  celdas superen el piso en todo el eje cambia la dificultad que el usuario siente: decisión de
  producto (`MB-CATALOG-0017`, residuo).
- **No se toca ningún tamaño de bloque ni `coreMs`.** Decisión de producto (`MB-CATALOG-0018`).
- **No se baja la tabla central.** Anti-patrón cerrado en R7.
- **No se surface al usuario.** Qué se le dice a una persona cuando su sesión no puede sostener una
  medida es diseño de producto y de copy (y el copy pasa por el scanner). Se declara en código; la
  presentación es de quien decida el producto.
- **No se cablea a ningún `ProfileSource`.** Los 9 siguen fail-closed. El hallazgo dice que esto es
  *precondición* de encender uno; una precondición se construye antes, no a la vez.

---

## 8. Correcciones encontradas al implementar

*(Una corrección se registra COMO corrección; no se reescribe el apartado de arriba en silencio.)*

### C1 — La autoridad llama a `conditionQuota`, no a `conditionSchedule` (§5.2 corregido)

`conditionSchedule(size, proportion, random)` exige un `Random`, y una declaración sobre lo que un bloque
PUEDE sostener no tiene por qué barajar nada ni poseer una fuente de aleatoriedad. Al implementarlo se
extrajo `conditionQuota(size, proportion, atLeast)` —el `round(size·p)` puro— y `conditionSchedule` pasó a
llamarlo. El objetivo del punto 2 (una sola implementación de la aritmética) se cumple mejor así: antes la
promesa de «una sola fuente» estaba en el KDoc y no en el código, porque el schedule llevaba su propia copia
inline de la expresión.

### C2 — La tabla de §3 omitía un tamaño de bloque REAL: n=75

§2 nombra el bloque fijo de 75 ensayos (E1, baseline) en el texto, pero la tabla de §3 no tenía su fila. Se
añade, CALCULADA con los generadores reales, no razonada. Es una fila con contenido propio: a n=75 E6 ya es
siempre elegible (a n=60 aún no) mientras E8 sigue INVERTIDO y E4 sigue sin llegar. El hueco lo destapó la
derivación de `productionBlockSizes` desde `SessionTemplate` + `BaselineProtocols`, que es exactamente lo
que §5.5 exige para que la tabla no se escriba a mano — y lo primero que hizo fue cazar mi propia omisión.

### C3 — Dos cifras de la fila n=75 que había escrito a mano eran falsas

La primera versión del pin decía `E2:>=0.16 E5:>=0.06 E8:<=0.86`; el cálculo da `E2:>=0.15 E5:>=0.03
E8:<=0.97`. Ninguna de las tres se puede obtener razonando: dependen de dónde cae `round(75·p)` respecto a
un piso de 20. Se corrigen contra el cálculo, y el orden del pin lo fija `ExerciseId.name` (`E13_` ordena
ANTES que `E2_`), no la intuición de leer «E2, E4, E5, E6, E8, E13».

### C4 — `conditionQuota` se movió a `core/runtime` a mitad de fase (cambio ajeno)

Una sesión paralela (Emociones · Ola 3) promovió `conditionSchedule`/`conditionQuota` de
`focus.domain.exercise` a `core.runtime` mientras esta fase estaba en curso, porque el ejercicio de
inhibición de Emociones tiene la misma obligación. La ubicación nueva es mejor (junto a `DeterministicRng`)
y la autoridad se adapta a ella: importa `core.runtime.conditionQuota`. No se revirtió trabajo ajeno.
