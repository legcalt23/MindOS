# ADR — Un redondeo a entero convierte una mejora medida en un cero fabricado; y la unidad NO determina la escala

**Fecha:** 2026-08-29 · **Fase:** R14 del plan blindado del catálogo cognitivo
**Hallazgo de entrada:** `MB-CATALOG-0024` (creado por mí en R13)
**Estado:** decidido · un cambio de producción que retira un cero fabricado y BORRA la tercera copia de un bloque duplicado

---

## 1. FASE 0 — verificación símbolo a símbolo de `MB-CATALOG-0024`

Tercer hallazgo consecutivo escrito por mí. La regla de R12/R13 aplica con más fuerza, no con menos: se verifica
el **enunciado** y también la **prescripción**.

| # | Símbolo del enunciado | Verificado | Resultado |
|---|---|---|---|
| 1 | `TransferRow` pinta `line.change.rawDelta.roundToInt()` | `FocusEvaluationScreen.kt:163-165`, `FocusProgressScreen.kt:198-200` | **CIERTO**, y el bloque es **byte-idéntico** en las dos pantallas (tercera copia del mismo render) |
| 2 | El alcance son 2 pantallas | grep de `line.change` | **CIERTO** (`FocusSummaryScreen` pinta la etiqueta, no el delta) |
| 3 | `ACCURACY` es `MetricUnit.PROPORTION` 0..1 | `DefaultMetricsEngine.kt:52-59` (`correct / accuracyN`) | **CIERTO** |
| 4 | `RT_CV` es un ratio pequeño | `DefaultMetricsEngine.kt:124` (`MetricUnit.RATIO`) | **CIERTO** |
| 5 | `MetricUnit` no viaja en `MetricChange` | `EvaluationResult.kt:8-15` | **CIERTO** (`metric`, `pre`, `post`, `rci`) |

Cinco de cinco ciertos. Y aun así la **PRESCRIPCIÓN es falsa**, por lo que sigue.

### 1.1 Refutación de mi propia prescripción — la unidad NO determina la escala

`MB-CATALOG-0024` prescribe «formateo por unidad (`MetricUnit` no viaja en `MetricChange`)». Medido sobre el
motor real, `MetricUnit` **no es una escala**: dos de sus seis valores cubren magnitudes que difieren en un
factor de 100.

| unidad | métrica | rango real | fuente |
|---|---|---|---|
| `INDEX` | `SUSTAINED_FOCUS_INDEX` | **0..100** | `DefaultMetricsEngine.kt:517` (`100.0 * …`) |
| `INDEX` | `BLOCK_STABILITY` | **0..1** | `:489` (`(1.0 - rtCv).coerceIn(0,1)`) |
| `INDEX` | `DISTRACTION_RESISTANCE_INDEX` | **0..1** | `:384` |
| `INDEX` | `FATIGUE_INDEX` | **0..1** | `:550` (`(raw + 1)/2`) |
| `RATIO` | `RT_CV` | **0..1** aprox. | `:124` |
| `RATIO` | `RATE_CORRECT_SCORE` | aciertos/segundo, **sin cota superior** | `:267` |

Implementar la prescripción (multiplicar las proporciones por 100, que es la convención ya existente en
`FocusSessionAssembler.kt:136` y `FocusBaselineScreen.kt:131`) habría escalado también `SUSTAINED_FOCUS_INDEX`
por unidad `INDEX`, **renderizando un cambio de 3 puntos de foco como «+300»**. Un defecto peor que el que
arregla.

La salida correcta no necesita la unidad en absoluto: se formatea por **MAGNITUD**. Eso además evita el
anti-patrón que R13 acaba de rechazar (una segunda tabla `MetricKey → escala` sería una segunda fuente de
verdad frente al motor).

### 1.2 El defecto medido, con los fixtures del propio repo

No hace falta derivarlo: los fixtures canónicos de `TransferReportBuilderTest` ya contienen el defecto.

| fixture (línea) | delta | render HOY | lectura del usuario |
|---|---|---|---|
| `MetricChange(ACCURACY, 0.8, 0.82, …)` (`:54,:72,:87,:105`) | **+0.02** (2 puntos porcentuales) | `+0` | «medimos, y no cambió nada» |
| `MetricChange(RT_CV, 0.30, 0.20, …)` (`:43,:88`) | **−0.10** (33 % menos variabilidad) | `0` | idéntico a un cero real |

Y hay una asimetría en las mismas tres líneas: `val sign = if (delta > 0) "+" else ""` combinado con
`roundToInt()` (que no produce cero negativo en `Int`) hace que una **mejora** pequeña salga `+0` y un
**empeoramiento** pequeño salga `0` — es decir, un declive real es **indistinguible de «sin cambio»**.

Métricas alcanzables a 55 ensayos de sonda cuyo delta cae bajo el redondeo (las nueve que cruzan el piso
central según R13, cruzadas con su rango real): **`ACCURACY`, `RT_CV`, `BLOCK_STABILITY`,
`RATE_CORRECT_SCORE`** — cuatro de nueve. Mi propio hallazgo decía dos. **Infravalorado otra vez**, y por la
misma razón que en R13: enumeré de memoria en vez de medir.

---

## 2. Severidad

`MB-CATALOG-0024` — **VIVO, P2**. No es un claim falso sobre capacidad (el gate RCI sigue gobernando
`strong`), pero el número que se muestra al usuario es incorrecto: un valor medido se presenta como cero.
Es la familia `null != 0` en su forma de **redondeo** — no «ausencia pintada como cero», sino «una medida real
pintada como cero» — y su forma omisiva (un declive indistinguible de la quietud).

---

## 3. Decisión

**Hacer:**

1. **`TransferReportBuilder.deltaText(delta)`**, función PURA en el dominio, que formatea por MAGNITUD:
   por debajo de la unidad, dos decimales con signo explícito en AMBAS direcciones; a partir de la unidad,
   el entero de hoy **byte-idéntico**. Cero dependencia de `MetricUnit`.
2. **`TransferLine` gana `deltaText: String`**, calculado en el ÚNICO sitio de construcción
   (`TransferReport.kt:35`). Queda testeable en `commonTest` sin Compose.
3. **Las dos pantallas pierden las tres líneas duplicadas** y pintan `line.deltaText`. Se borra la tercera
   copia del mismo bloque de render (deletion over addition).

**No hacer** (se registra, no se ejecuta):

- **Añadir `MetricUnit` a `MetricChange`.** Refutado en §1.1: la unidad no da la escala, y el campo tiene HOY
  **cero consumidores de dominio** (solo se serializa como `.unit.name` en dos mappers). Darle un consumidor
  que decide mal es peor que dejarlo sin consumidor.
- **Una tabla `MetricKey → escala`.** Segunda fuente de verdad frente al motor: el anti-patrón que R13 rechazó.
- **Escalar las proporciones a puntos porcentuales** (`+2 pts` en vez de `0,02`), que sería más legible. Exige
  saber la escala por métrica, que es exactamente lo que no existe. Es una decisión de producto sobre copy, no
  de plumbing. Se registra.
- **Tocar `FocusProgressScreen.metricLabel`** (sigue ARMADA, registrada en R13) ni ninguna otra copia.

### 3.1 Criterios de aborto — declarados ANTES de escribir código

- **A1** — Algún delta con `|delta| >= 1` deja de renderizarse **byte-idéntico** a hoy (incluido el cero exacto,
  que debe seguir siendo `"0"`, y el signo `+` solo en positivos).
- **A2** — Algún delta no nulo con `|delta| < 1` sigue renderizándose como un entero (es decir, el defecto sigue
  vivo), o una mejora y un declive de la misma magnitud producen la MISMA cadena.
- **A3** — El cambio altera `changes`, `strongEvidence`, `preliminary`, `hasReliableChange` o alguna de las 25
  etiquetas selladas en R13.
- **A4** — Alguna cadena nueva dispara el copy-safety scanner, o el `when` exhaustivo de R13 deja de compilar.

---

## 4. Adyacencia fail-closed que se preserva

Ningún gate se mueve. `MetricChange` no cambia (sus 11 sitios de construcción quedan intactos), el gate
`measurementSufficient == true` sigue decidiendo qué entra en `changes`, `withheldForSample` (R12) sigue
poblándose con `== false`, y el veredicto `strong` sigue siendo el RCI. Este ADR solo cambia **cómo se ESCRIBE
un número** que ya se renderizaba.

---

## 5. Correcciones durante la ejecución

Toda corrección se registra COMO corrección; no se reescribe el apartado que resultó equivocado.

**C1 — La PRESCRIPCIÓN de mi propio hallazgo quedó refutada, no su enunciado.** `MB-CATALOG-0024` (escrito por mí
en R13) acertaba los cinco símbolos y prescribía «formateo por unidad». Medido sobre el motor, `MetricUnit` no es
una escala: `INDEX` cubre 0..100 y 0..1, `RATIO` cubre 0..1 y aciertos/segundo sin cota. Implementar la
prescripción habría renderizado un cambio de 3 puntos de foco sostenido como **«+300»**. El diseño lo eligió la
medición: formatear por MAGNITUD no necesita la unidad y no crea una segunda fuente de verdad.

**C2 — Y el alcance volvió a estar INFRAVALORADO, tercera fase consecutiva.** El hallazgo decía dos métricas
(`ACCURACY`, `RT_CV`); cruzando las nueve que cruzan el piso central a 55 ensayos con su rango real son **cuatro**
(añadir `BLOCK_STABILITY` y `RATE_CORRECT_SCORE`). La causa es la misma que en R13: enumeré de memoria.

**C3 — Ningún criterio de aborto se disparó.** A1 y A2 quedaron mecanizados y probados por mutación (M2 y M1);
A3 por la suite completa en verde con las 25 etiquetas de R13 intactas; A4 por los cuatro escáneres y los dos
gates de Android.

**C4 — Mi extractor de tests muertos mintió DOS veces, y la segunda es la peligrosa.** La primera versión casaba
`name="…" classname` y «reportó» los 13 casos como muertos. La segunda añadió un guard de conteo —que PASÓ, 2 == 2—
y aun así imprimió los **nombres equivocados**: su `[\s\S]*?` se tragaba los `<testcase …/>` auto-cerrados que
preceden a un fallo, devolviendo el nombre del test anterior. Con esos nombres, M1 parecía haber matado dos tests
preexistentes y no los míos — un resultado que habría invalidado la fase entera. Lo resolvió leer los **mensajes**
de fallo (`expected:<+0[,02]> but was:<+0[]>`), que nombran la aserción. Un guard de anti-vacuidad sobre el CONTEO
no valida la ATRIBUCIÓN.

**C5 — La primera corrida de M1 fue INCONCLUSA y se declaró como tal.** Dio `tests="1" failures="1"` con
`initializationError` y `NoClassDefFoundError: MetricKey`: la firma conocida de directorio de clases reescrito por
la sesión paralela, que en ese momento tenía **7–8 JVMs**. No es un rojo ni un verde. Repetida en ventana quieta
(8 muestras consecutivas ≤ 2 JVMs) mató exactamente los dos tests previstos, con `AssertionError` real.

**C6 — `Daemon compilation failed: null` con CERO errores de fuente.** Stack en `IncrementalCachesManager.close`
más `Could not delete …/classes/kotlin/desktop/test`: familia conocida de Gradle concurrente. Se diagnosticó, se
esperó y se re-ejecutó; no se cambió una línea.
