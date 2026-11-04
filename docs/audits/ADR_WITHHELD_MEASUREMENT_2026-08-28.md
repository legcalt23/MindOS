# ADR — Una medida RETENIDA por muestra insuficiente es un hecho, no un hueco

**Fecha:** 2026-08-28 · **Fase:** R12 del plan blindado del catálogo cognitivo
**Hallazgo de entrada:** `MB-CATALOG-0019` (creado por mí en R11)
**Estado:** decidido · cambio de producción ADITIVO, sin mover ningún gate

---

## 1. FASE 0 — verificación símbolo a símbolo de `MB-CATALOG-0019`

El hallazgo entra a esta fase **escrito por mí la fase anterior**. Eso obliga a más escrutinio, no a
menos: la regla de R8 es verificar el enunciado *y su prescripción*, y un hallazgo propio no tiene
revisor externo que lo contradiga.

| # | Símbolo del enunciado | Verificado | Resultado |
|---|---|---|---|
| 1 | `ProbeBank.fixedProbeDifficulty` deja `interference` sin fijar | `ProbeBank.kt:20-23` | **CIERTO** (queda en `AxisLevel.MIN`) |
| 2 | `SELECTIVE_PROBE` usa esa dificultad fija | `ProbeBank.kt:36` | **CIERTO** |
| 3 | `ProbeExerciseMapping` enruta `FLANKER` → `E2_FILTRO` | `ProbeExerciseMapping.kt:26` | **CIERTO** |
| 4 | La métrica que indexa `SELECTIVE_FILTERING` es `CONGRUENCY_EFFECT` | `MeasurementSufficiency.central` | **CIERTO** (única del mapa) |
| 5 | Piso central 20 por celda, `requiresAllCells` | `MinSampleGate.kt:48` | **CIERTO** |
| 6 | Celda menor 11 sobre 55 ensayos de sonda | `conditionQuota(55, 0.20)` | **CIERTO** |
| 7 | `SELECTIVE_PROBE ∈ ProbeBank.all` | `ProbeBank.kt:56` | **CIERTO** |
| 8 | `BaselineProtocols.INITIAL` no lo selecciona | `BaselineProtocol.kt:85-115` | **CIERTO** (usa `VIGILANCE_PROBE`) |

Ocho de ocho ciertos. Y aun así el enunciado **es impreciso**, por lo que sigue.

### 1.1 Refutación — no existe ninguna «afirmación de capacidad» en el código

`MB-CATALOG-0019` dice que `SELECTIVE_PROBE` «no puede sostener su afirmación de capacidad». Trazado
el consumidor hasta el final, esa afirmación **no existe como tal**:

- `ProbeBank.forCapacity(capacity)` **no tiene ningún consumidor** fuera de `ProbeBank` (grep sobre
  `shared/src`: solo `ProbeBank.all`, servido por `InMemoryProbeBankRepository` y
  `LocalFocusRepositories`). Nada selecciona una sonda por capacidad.
- `MetricChange(metric, pre, post, rci)` **no lleva etiqueta de capacidad**. `EvaluationResult.changes`
  es una lista plana de métricas.
- `TransferLine.label` etiqueta por MÉTRICA («Reacción (prueba)», «Estabilidad (prueba)»), no por
  capacidad, y `honestNote` es genérico.

Es decir: `ProbeDefinition.capacity` es **decorativo en la ruta de evaluación**. La afirmación que el
producto emite es por métrica, y está honestamente etiquetada como tal. La severidad que yo escribí en
R11 —una afirmación de capacidad mal sostenida— **no se materializa**, porque nadie une sonda→capacidad
→veredicto.

### 1.2 Lo que la traza SÍ encontró

`EvaluationComparator.compare` colapsa **tres hechos distintos en el mismo `null`**:

```kotlin
val retest = retestMetrics[metric]?.takeIf { it.measurementSufficient == true }
    ?: return@mapNotNull null
```

| Caso | Situación | Hecho real |
|---|---|---|
| (a) | el retest no tiene valor para esa métrica | ausencia genuina — no aplica |
| (b) | hay valor, `measurementSufficient == false` | **la medimos y la muestra no alcanzó el piso ratificado** |
| (c) | hay valor, `measurementSufficient == null` | no hay piso ratificado; la pregunta no aplica |

El caso (b) es un hecho sobre **nuestro diseño de muestreo**, y hoy se pierde: la métrica desaparece del
informe, no aparece ni en `strongEvidence` ni en `preliminary`. El usuario no puede distinguir «esta
sonda no mide eso» de «lo medimos y la muestra se quedó corta».

Y el hueco es estructural, no exclusivo de E2: el motor marca `confident` con pisos **en sesión** de 3–5
(`MIN_N_CONGRUENCY = 3`, `MIN_N_POST_ERROR = 3`, `MIN_N_RT = 5`, `MIN_N_ACCURACY = 5`) mientras el piso
**central** exige 20. Toda métrica gobernada puede por tanto entrar en `perMetric` y caerse después en el
gate de retest.

### 1.3 Tirar el NÚMERO es correcto; tirar el HECHO no lo es

`RciMeasurementSufficiencyTest:44-46` ya declara por qué el número no puede pasar: «Absent, never a
number (`null != 0`)». Eso es R8 y **no se toca**. Lo que este ADR corrige es lo otro: que junto al
número se tira también el conocimiento de que se tiró. Fail-closed sobre el **valor**, no sobre el
**hecho**.

---

## 2. Severidad, dividida honestamente

No la inflo. Hoy `ReliabilitySource.EMPTY` mantiene `reliabilities` vacío, así que todo RCI es
`NotEnoughData` y `hasReliableChange` es **siempre falso**. Por tanto:

- **VIVO — omisión.** Métricas computadas y retenidas desaparecen del informe sin dejar rastro
  recuperable aguas abajo. El texto que el usuario lee («Aún sin cambio fiable en pruebas no
  entrenadas») se renderiza en TRES pantallas (`FocusEvaluationScreen:141`, `FocusProgressScreen:179`,
  `FocusSummaryScreen:113`). El «aún» promete alcanzabilidad: es falso para una métrica cuya celda no
  puede llegar al piso a esa longitud de sonda — más sesiones no la producirán nunca.
- **ARMADO — sustitución.** El día que un estudio empírico pueble la costura de fiabilidad (la misma que
  R8 nombró: «sin tocar call-sites»), `hasReliableChange` se computará sobre las métricas que
  SOBREVIVIERON —las 9 gobernadas por `ACCURACY_RT_BASE`, todas alcanzables a 55 ensayos— mientras la que
  indexa la capacidad de la sonda está invisiblemente ausente. El veredicto se apoyaría en otra cosa.

La afirmación honesta es esa: **omisión viva, sustitución armada**. No «la nota miente hoy».

---

## 3. Decisión

**Hacer** (técnico, sin decisión de producto):

1. `EvaluationComparator` deja de colapsar (b) con (a)/(c). El caso (b) —y solo (b), discriminado por el
   tri-estado `measurementSufficient == false`— se acumula en `EvaluationResult.withheldForSample`.
2. `TransferReportBuilder` añade UNA cláusula a `honestNote` cuando esa lista no está vacía. Las dos
   cadenas existentes se conservan **byte-idénticas** y se componen, en vez de reescribirse: no es una
   decisión de copy nueva, es dejar de omitir un hecho.

**No hacer** (decisión de producto, se registra, no se ejecuta):

- Fijar `interference = 0.75` en `fixedProbeDifficulty` (llevaría la celda de 11 a 30 a coste cero de
  tiempo). Cambia lo que la sonda mide: es psicometría.
- Alargar la sonda a n ≥ 98 (≈ 6.2 min). Implausible para una sonda.
- Retirar `SELECTIVE_PROBE` de `ProbeBank.all`. Descarta contenido bueno por un parámetro.
- Sección de UI para las métricas retenidas. `TransferReportBuilder.label` cae a `else -> metric.name`
  para 12 de 16 métricas, así que una sección nueva filtraría nombres de enum crudos a copy en
  castellano. Se registra como hallazgo aparte.

**No crear** un símbolo de producción que relacione sonda→capacidad→métrica: nada en runtime necesita
preguntarlo (`forCapacity` lleva sin consumidor desde que existe), y sería el anti-patrón de 2026-08-18
—«lo que se conserva para la fase siguiente se lee como capacidad existente»— que R11 ya rechazó una vez.

### 3.1 Criterios de aborto — declarados ANTES de escribir código

Si cualquiera se dispara, el cambio se revierte y la fase entrega solo declaración:

- **A1** — `result.changes` cambia para alguna entrada existente. El cambio debe ser estrictamente
  aditivo sobre la ruta de la afirmación.
- **A2** — `hasReliableChange` cambia para alguna entrada existente.
- **A3** — alguna de las dos cadenas de `honestNote` deja de ser byte-idéntica en la ruta sin retención.
- **A4** — `withheldForSample` puede contener una métrica con `measurementSufficient == null` (sin piso
  ratificado). Eso confundiría (b) con (c), que es el defecto que este ADR existe para separar.

---

## 4. Adyacencia fail-closed que se preserva

El gate sigue siendo `== true`, nunca `!= false`: una métrica sin piso ratificado (`null`) sigue
DENEGANDO. Añadir la lista de retenidas no relaja nada — la métrica retenida sigue sin producir
`MetricChange`, sigue sin poder alimentar `hasReliableChange`, y su número sigue sin cruzar la frontera.

---

## 5. Correcciones durante la ejecución

Toda corrección se registra COMO corrección; no se reescribe el apartado que resultó equivocado.

**C1 — La hipótesis con la que entré a la fase quedó refutada en su parte principal.**
`MB-CATALOG-0019` (escrito por mí en R11) afirmaba que la sonda «no puede sostener la afirmación de capacidad
para la que existe». Los 8 símbolos son ciertos, pero **esa afirmación no existe en el código**: `forCapacity`
sin consumidor, `MetricChange` sin etiqueta de capacidad, `TransferLine.label` por métrica. La severidad estaba
inventada. El hallazgo se PRECISA en el ledger en vez de cerrarse por narrativa, y la parte que sí es real —el
déficit de muestreo como precondición de cablear la sonda— se conserva intacta.

**C2 — Ningún criterio de aborto se disparó.** A1/A2 quedaron satisfechos *por construcción* al computar
`withheldForSample` fuera del fold (el bloque `changes` es byte-idéntico); A3 y A4 quedaron mecanizados y
probados por mutación (M4 y M2 respectivamente).

**C3 — Un resultado de mutación se rechazó por no ser atribuible.** La primera corrida de M2 puso en rojo
`matcherAccentFoldIsPinnedInBothDirections`, que es un test de la sesión paralela, con una sola suite ejecutada.
Un rojo que no puedo atribuir a mi mutación no es evidencia: se repitió exigiendo que la suite objetivo
apareciera en la ventana de la corrida, y entonces mató el test correcto
(`aMetricWithNoRatifiedFloorIsNotReportedAsWithheld_discriminantHalf`).

**C4 — Un falso verde intermedio, invisible en el log.** Tras un compile abortado por concurrencia, una corrida
devolvió `BUILD SUCCESSFUL in 13s` con `> Task :shared:desktopTest NO-SOURCE`. El directorio de clases de test
tenía **0 ficheros `.class`**: no se ejecutó ni un test. No se detecta leyendo el log —se detecta contando el
output—. Se resolvió con `--rerun` sobre el task de compilación, nunca con `clean`.

**C5 — Techo declarado del pre-check de léxico.** El comprobador local deriva 228 formas de 63 lemas con el
modelo de flexión anterior, mientras el lexicon vigente declara 89. Sobre-genera, así que un «LIMPIO» es
conservador y no puede producir falsos negativos, pero **no es un espejo exacto**: la autoridad sigue siendo
`mindboostCopySafetyScanner` y sus tres variantes, ejecutadas y en verde.
