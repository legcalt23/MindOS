# ADR — Un `else` en una tabla de etiquetas filtra el identificador interno como si fuera copy

**Fecha:** 2026-08-28 · **Fase:** R13 del plan blindado del catálogo cognitivo
**Hallazgo de entrada:** `MB-CATALOG-0022` (creado por mí en R12)
**Estado:** decidido · una supresión (costura muerta) + un cambio de producción que RETIRA un fallback silencioso

---

## 1. FASE 0 — verificación símbolo a símbolo de `MB-CATALOG-0022`

Segundo hallazgo consecutivo escrito por mí mismo. La regla de R12 aplica con más fuerza, no con menos.

| # | Símbolo del enunciado | Verificado | Resultado |
|---|---|---|---|
| 1 | `ProbeBank.forCapacity` no tiene consumidor de producción | `grep -rn forCapacity shared/src` | **CIERTO, y más fuerte**: 1 sola ocurrencia en TODO el árbol — su propia definición (`ProbeBank.kt:57`). Cero consumidores, producción **y test** |
| 2 | `MetricChange` no lleva capacidad | `EvaluationResult.kt:8-15` | **CIERTO** (`metric`, `pre`, `post`, `rci`) |
| 3 | `hasReliableChange` es `changes.any { reliable }` | `EvaluationResult.kt:36-37` | **CIERTO** |
| 4 | Inerte por `ReliabilitySource.EMPTY` | `ProbeReliabilityEstimator.kt:28` | **CIERTO** (default de producción) |
| 5 | `INITIAL` solo selecciona `VIGILANCE_PROBE` | `BaselineProtocol.kt:114` | **CIERTO** |

Cinco de cinco ciertos. Y aun así la CONSECUENCIA es falsa, por lo que sigue.

### 1.1 Refutación — no hay defecto que impedir, hay una costura muerta que borrar

`MB-CATALOG-0022` dice que «el veredicto que emite puede descansar en una métrica que no la indexa, sin que
ningún símbolo lo impida». La segunda mitad es cierta; la primera no describe un defecto:

- Ningún consumidor atribuye un veredicto a una capacidad, porque **el único símbolo que podría hacerlo
  (`forCapacity`) no lo llama nadie**. `TransferReport` no tiene campo de capacidad; `honestNote` habla de
  «pruebas NO entrenadas», que es cierto para cualquier métrica; cada fila se etiqueta POR MÉTRICA.
- Luego `0022` no es un defecto vivo ni armado: es (a) una **precondición** de una fase futura y (b) una
  **costura muerta** conservada para esa fase.

Y para (b) el repo ya tiene regla escrita (aprendizaje 2026-08-18): *«Todo lo que se conserve "para la fase
siguiente" se BORRA si esa fase cierra sin construirlo, y vuelve con su consumidor y su test»*. Conservar
`forCapacity` es exactamente el anti-patrón que esa regla nombra — «se lee como capacidad existente».

### 1.2 Lo que la traza SÍ encontró — y contradice otro hallazgo mío

Siguiendo el consumidor de `TransferReport` hasta la pantalla apareció que **`MB-CATALOG-0021`, que yo
registré en R12 como P3 y con la frase «no es un defecto vivo», está VIVO**:

| paso | símbolo | hecho |
|---|---|---|
| 1 | `DefaultCalibrationEngine.kt:71` | `perMetric` = **toda** métrica `confident` (pisos EN SESIÓN 3..6) |
| 2 | `DefaultMetricsEngine` | emite `IES`, `RATE_CORRECT_SCORE`, `LISAS`, `BLOCK_STABILITY`, `SUSTAINED_FOCUS_INDEX` |
| 3 | `MeasurementSufficiency.central` | las cinco → `ACCURACY_RT_BASE` (piso 20, celda única) ⇒ suficientes a 55 ensayos de sonda |
| 4 | `EvaluationComparator` | pasan el gate `== true` ⇒ entran en `changes` |
| 5 | `TransferReportBuilder.label` | 4 ramas + **`else -> metric.name`** |
| 6 | 3 pantallas | `FocusEvaluationScreen:162`, `FocusProgressScreen:197`, `FocusSummaryScreen:161` pintan `line.label` |

`ReliabilitySource.EMPTY` bloquea que una línea sea **strong**; NO bloquea que se liste. Las líneas caen en
`preliminary` y se renderizan igual. Es decir: hoy un usuario ve **`SUSTAINED_FOCUS_INDEX`** en una pantalla en
castellano. La prueba más limpia de que es real: `FocusProgressScreen` pinta la MISMA métrica dos veces con
DOS nombres distintos — «Índice de foco sostenido» en la fila de tendencia (`:125`) y `SUSTAINED_FOCUS_INDEX`
crudo en la fila de transferencia (`:197`).

### 1.3 La segunda copia — armada, no viva, e inalcanzable desde un test

Existen **DOS** tablas `MetricKey → String`, cada una con su propio `else`:

| # | símbolo | fallback | estado |
|---|---|---|---|
| 1 | `TransferReport.kt:47` `label()` | `else -> metric.name` | **VIVO** (5 métricas alcanzables a 55 ensayos) |
| 2 | `FocusProgressScreen.kt:130` `metricLabel()` | `else -> metric.name.lowercase()...` | **ARMADO** |

La #2 es inalcanzable hoy solo porque `FocusProgressStateHolder.TRACKED_METRICS` resulta ser un subconjunto de
sus 4 ramas — dos listas que coinciden por accidente, sin nada que lo imponga. Es `private` dentro de un fichero
Compose, así que **no es alcanzable desde `desktopTest`**: se REGISTRA con dueño en vez de subirle la
visibilidad de paso (precedente explícito, aprendizaje 2026-08-28 · R11).

---

## 2. Severidad, dividida honestamente

- **`0022` — NO es un defecto.** Precondición + costura muerta. Se cierra BORRANDO, no implementando.
- **`0021` — VIVO, P2, y yo lo infravaloré.** Identificador interno renderizado como copy en 3 pantallas.
  No inflo: no es un claim falso sobre el usuario, es un nombre ilegible; pero es exactamente la familia
  «la UI no renderiza como hecho lo que no lo es», y lo agrava que la misma pantalla dé dos nombres al mismo
  métrico.
- **La segunda copia — ARMADA.** Fail-open por coincidencia de dos listas.

---

## 3. Decisión

**Hacer:**

1. **BORRAR `ProbeBank.forCapacity`.** Cero consumidores en todo el árbol; la búsqueda que hace es trivial de
   reescribir el día que exista un consumidor, y ese día el consumidor la trae con su test. Cierra `0022`.
2. **Retirar el `else` de `TransferReport.label`**, convirtiéndolo en un `when` EXHAUSTIVO sobre `MetricKey`.
   El guard va en BUILD, no en ejecución (aprendizaje 2026-08-19): una `MetricKey` nueva **no compila** hasta
   que alguien decide cómo se llama de cara al usuario. Las 4 etiquetas existentes se conservan
   **byte-idénticas**.

**No hacer** (se registra, no se ejecuta):

- Fusionar las dos tablas en una. Sirven contextos distintos (fila de tendencia vs fila de sonda: el sufijo
  «(prueba)» es el ÚNICO marcador de sonda en `FocusSummaryScreen`), y fusionarlas cambiaría 3 cadenas
  embarcadas sin arreglar ningún defecto vivo. El precedente del repo para copias múltiples es **pinnearlas**,
  no forzar la fusión.
- Tocar `FocusProgressScreen.metricLabel`. Su `else` está armado, no vivo, y es `private` en un fichero Compose.
- Arreglar el formateo del delta. `TransferRow` pinta `delta.roundToInt()`, y `ACCURACY`/`RT_CV` son
  proporciones 0..1, así que **su delta se renderiza siempre como `+0`**. Es un defecto vivo distinto y su
  arreglo necesita formateo por unidad (`MetricUnit` no viaja en `MetricChange`): es un cambio de datos que
  toca las 3 pantallas. Se registra.

### 3.1 Criterios de aborto — declarados ANTES de escribir código

- **A1** — Alguna de las 4 etiquetas existentes (`ACCURACY`, `RT_MEDIAN`, `RT_CV`, `LAPSES`) deja de ser
  byte-idéntica.
- **A2** — `label` conserva un `else`, o alguna rama devuelve un texto que contiene `_` o coincide con
  `metric.name` (es decir, sigue filtrando el identificador).
- **A3** — Alguna etiqueta nueva dispara el copy-safety scanner.
- **A4** — Borrar `forCapacity` rompe la compilación de `commonMain` o de cualquier source-set de test (sería
  la prueba de que el grep de consumidores era falso).

---

## 4. Adyacencia fail-closed que se preserva

Ningún gate se mueve. Una métrica sin piso ratificado sigue sin producir `MetricChange`; `withheldForSample`
(R12) sigue poblándose con `== false`; `hasReliableChange` sigue siendo `changes.any { reliable }`. Este ADR
solo cambia **cómo se NOMBRA** una línea que ya se renderizaba.

---

## 5. Correcciones durante la ejecución

Toda corrección se registra COMO corrección; no se reescribe el apartado que resultó equivocado.

**C1 — La hipótesis de entrada quedó refutada, por segunda fase consecutiva.** `MB-CATALOG-0022` (escrito por mí
en R12) afirmaba una consecuencia —«el veredicto puede descansar en una métrica que no la indexa»— que no existe:
sin consumidor de `forCapacity`, nadie une sonda→capacidad→veredicto. No había defecto que impedir. El hallazgo se
cierra **borrando la costura**, y la restricción sobre la fase futura se conserva en su propia fila del ledger.

**C2 — Y esta vez el error de severidad fue por DEFECTO, no por exceso.** `MB-CATALOG-0021`, también mío y también
de R12, decía «no es un defecto vivo». Es falso: di por inerte la ruta porque `ReliabilitySource.EMPTY` estaba
apagado, sin comprobar qué apaga ese gate — bloquea que una fila sea *strong*, no que se liste. Se corrige la
severidad en el ledger (P3 → P2) además de cerrarlo.

**C3 — Ningún criterio de aborto se disparó.** A1 y A2 quedaron mecanizados y probados por mutación (M2 y M1); A3
por los cuatro escáneres en verde con las 21 etiquetas nuevas; A4 por compilar los cuatro source-sets y correr la
suite completa sin `forCapacity`.

**C4 — Un falso verde NUEVO, invisible en el log.** La mutación M2 devolvió `BUILD SUCCESSFUL in 11s` con la
mutación aplicada, lo que parecía «M2 no mata ningún test». No lo era: `compileKotlinDesktop` salió `UP-TO-DATE`
**pese a que el fuente había cambiado**, porque una corrida anterior había compilado esa misma mutación y había
fallado SOLO en su paso de `Failed to store cache entry` — el snapshot de entradas quedó registrado como
construido. Se detectó contando el output (el XML era de las 22:46:53) y se resolvió con `--rerun` en compilación
Y en test; entonces M2 mató exactamente su test. Añádase a la familia de firmas de C4/R12.

**C5 — El conteo de hallazgos de detekt volvió a mentir, con otro disfraz.** `@($file.error).Count` devuelve **1**
sobre un informe vacío, porque en PowerShell `@($null).Count` es 1. Verificado con XPath (`//error` = 0) y con el
XML crudo (`<checkstyle>` vacío).

**C6 — Alcance medido, no recordado.** Iba a registrar el defecto del delta como «3 pantallas»; el grep dice **2**
(`FocusSummaryScreen` pinta la etiqueta pero no el delta). Se registró la cifra medida.
