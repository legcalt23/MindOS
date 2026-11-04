# ADR — El guard va donde se decide el conjunto ALCANZABLE, no sobre el enum

**Fecha:** 2026-08-29 · **Fase:** R15 del plan blindado del catálogo cognitivo
**Hallazgo de entrada:** `MB-CATALOG-0023` (creado por mí en R13)
**Estado:** decidido · se BORRA la segunda tabla de etiquetas y se unifican las dos listas en una

---

## 1. FASE 0 — verificación símbolo a símbolo

Cuarto hallazgo consecutivo escrito por mí. Se verifica el **enunciado** y, por separado, la **prescripción**.

| # | Símbolo del enunciado | Verificado en | Resultado |
|---|---|---|---|
| 1 | `metricLabel()` con 4 ramas y `else -> metric.name.lowercase().replace('_',' ')` | `FocusProgressScreen.kt:130-136` | **CIERTO** |
| 2 | Filtraría «ies», «lisas», «rate correct score» | mismo `else` | **CIERTO** (21 de 25 métricas caen ahí) |
| 3 | Hoy INALCANZABLE | `metricLabel` tiene **UN** llamante en todo el árbol (`:125`) | **CIERTO** |
| 4 | `trend.metric` procede solo de `TRACKED_METRICS` | `DefaultTrendAnalyzer` devuelve el MISMO `metric` en sus dos rutas (`Trends.kt:58`, `:69`) | **CIERTO** |
| 5 | `TRACKED_METRICS` = ACCURACY, RT_MEDIAN, RT_CV, SUSTAINED_FOCUS_INDEX | `FocusProgressStateHolder.kt:74-79` | **CIERTO** |
| 6 | Dos listas que coinciden por accidente, nada lo impone | son declaraciones independientes en ficheros distintos | **CIERTO** |

Seis de seis. `MetricKey` tiene **25** entradas y en todo el árbol existen **exactamente dos** tablas
`MetricKey → String`: la canónica `TransferReport.label` (exhaustiva desde R13) y esta. No hay una tercera.

### 1.1 Refutación: la «salida estructural» que mi propia fila nombraba NO arregla el mecanismo

La fila prescribe como salida estructural «hacer exhaustivo también este `when`» — el movimiento con que R13
cerró `MB-CATALOG-0021`. **Medido, aquí no cierra nada.** `TRACKED_METRICS` es una lista literal escrita a mano
en OTRO fichero; la exhaustividad sobre `MetricKey` es ortogonal a ella. Añadir `MetricKey.IES` a la lista
**sigue compilando**: simplemente renderiza la rama que IES tuviera. Es decir, la exhaustividad no impide el
armado; solo cambia *qué* se filtra: de un identificador crudo (visiblemente roto) a una cadena plausible que
nadie revisó (**más difícil de notar**). Y exige inventar 21 cadenas en castellano que hoy no renderiza nadie.

**Por qué el mismo movimiento fue correcto en R13 y es incorrecto aquí:** en `TransferReport.label` el conjunto
alcanzable lo decide el MOTOR (`perMetric` → `changes`), así que el compilador se sienta exactamente en el
instante en que una métrica se vuelve renderizable. Aquí el conjunto alcanzable es una **lista literal**, así que
el compilador tiene que sentarse sobre ESA LISTA, no sobre el enum. **El guard va donde se decide el conjunto
alcanzable.**

### 1.2 La prescripción también estaba MAL CONTADA, y la que faltaba era la peor

La fila dice que fusionar las dos tablas «cambiaría 3 cadenas embarcadas». Medido contra el árbol de R13
(`git show HEAD`, donde `label()` tenía 4 ramas y `else -> metric.name`), fusionar habría cambiado **CUATRO**:

| métrica | tabla de tendencia (hoy) | canónica en R13 (`git show HEAD`) |
|---|---|---|
| `ACCURACY` | `Precisión` | `Precisión (prueba)` |
| `RT_MEDIAN` | `Reacción (mediana)` | `Reacción (prueba)` |
| `RT_CV` | `Estabilidad (RT-CV)` | `Estabilidad (prueba)` |
| `SUSTAINED_FOCUS_INDEX` | `Índice de foco sostenido` | **`SUSTAINED_FOCUS_INDEX`** ← caía al `else` |

La cuarta es justo la que mi conteo omitió, y es la dañina: fusionar en R13 habría sustituido una etiqueta buena
por un **identificador crudo**. Cuarta fase consecutiva con un error de alcance en una fila escrita por mí, y por
cuarta vez en la dirección de INFRAVALORAR.

### 1.3 Y la RAZÓN de rechazar la fusión era la equivocada

La fila justifica el rechazo por coste («3 cadenas sin arreglar nada vivo») y por precedente («pinnear, no
fusionar»). La razón real es más fuerte y es de honestidad: el marcador **«(prueba)»** significa *sonda
reservada*. La fila de tendencia se computa sobre `historyRepository.recent(...)`, es decir sobre las **sesiones
de entrenamiento del propio usuario**, que NO son una sonda. Fusionar etiquetaría una tendencia de entrenamiento
como si viniera de la sonda reservada: **mal-etiquetado de procedencia**, exactamente lo que el charter prohíbe
(invariante #7 — la afirmación de capacidad solo la sostiene la sonda no entrenada). Las dos pantallas conviven
en la MISMA vista, así que la distinción es visible y significativa. Se rechaza la fusión por eso, y se mecaniza.

---

## 2. Severidad

`MB-CATALOG-0023` — **ARMADO, no vivo. P3.** Confirmado: hoy ningún usuario ve un identificador crudo en la
fila de tendencia. Lo que se arregla es la **posibilidad silenciosa**, no un defecto vivo.

---

## 3. Decisión

**Hacer — una lista en vez de dos.** Nombrar una métrica pasa a ser la ÚNICA forma de rastrearla:

1. `FocusProgressStateHolder.TRACKED_TRENDS: List<Pair<MetricKey, String>>` sustituye a `TRACKED_METRICS`.
   Un elemento sin etiqueta **no compila** (error de tipo). Guard en BUILD, donde se decide el conjunto.
2. `TrendLine(label, trend)` en `FocusProgressUiState.kt`, espejo exacto de `TransferLine` (que ya lleva
   `label` desde R13 y `deltaText` desde R14, en esta misma feature). La cadena VIAJA con la fila: ninguna
   búsqueda puede fallar.
3. `FocusProgressScreen` pierde `metricLabel`, su `else` y su import de `MetricKey`. **Deletion over addition.**

Las 4 cadenas embarcadas se mueven **byte-idénticas**. Cero copy nueva.

**No hacer** (se registra, no se ejecuta):

- **Hacer exhaustivo `metricLabel`.** Refutado en §1.1: no impide el armado y añade 21 cadenas especulativas.
- **Fusionar las dos tablas.** Refutado en §1.3: mal-etiquetaría la procedencia. Se mecaniza el rechazo.
- **Tocar `MetricTrend`, `TrendAnalyzer` o `TransferReport.label`.** El dominio no cambia.

### 3.1 Criterios de aborto — declarados ANTES de escribir código

- **A1** — Alguna de las 4 etiquetas de tendencia embarcadas cambia aunque sea un byte.
- **A2** — Tras el cambio, añadir una métrica al conjunto rastreado sigue compilando sin darle nombre
  (es decir: el arreglo no arregla el mecanismo).
- **A3** — Cambia alguna de las 25 etiquetas de transferencia selladas en R13, o se rompe otro consumidor de
  `FocusProgressUiState.Ready.trends`.
- **A4** — Cambia la forma de `MetricTrend` / `TrendAnalyzer` (dominio), o una cadena dispara un escáner.

---

## 4. Adyacencia fail-closed que se preserva

Ningún gate se mueve. No hay cálculo, ni umbral, ni afirmación de capacidad implicada: esta fase solo decide
**de dónde sale el nombre de una fila**. `TransferReport` queda intacto (incluidos `label` y `deltaText`), el
gate RCI sigue gobernando `strong`, y `withheldForSample` (R12) sigue poblándose igual.

---

## 5. Correcciones durante la ejecución

Toda corrección se registra COMO corrección; no se reescribe el apartado que resultó equivocado.

**C1 — La PRESCRIPCIÓN de mi propia fila no arregla el mecanismo que describe.** `MB-CATALOG-0023` nombra como
salida estructural «hacer exhaustivo también este `when`». Medido: `TRACKED_METRICS` era una lista literal en
otro fichero, así que la exhaustividad sobre `MetricKey` es ORTOGONAL al armado — añadir una quinta métrica
seguía compilando. Lo único que habría cambiado es QUÉ se filtra: de un identificador crudo (visiblemente roto)
a una cadena plausible que nadie revisó, más 21 cadenas especulativas que nada renderiza. **El guard va donde se
decide el conjunto alcanzable**, y aquí eso es la lista, no el enum.

**C2 — El alcance de la fila estaba mal contado, y la omisión era la dañina.** Decía que fusionar cambiaría
«3 cadenas embarcadas»; contra el árbol de R13 (`git show HEAD`) son **CUATRO**, y la cuarta
(`SUSTAINED_FOCUS_INDEX`) caía entonces al `else -> metric.name`, así que fusionar habría sustituido una etiqueta
buena por un identificador crudo. Cuarta fase consecutiva con un error de alcance en una fila escrita por mí, y
por cuarta vez INFRAVALORANDO.

**C3 — Y la RAZÓN registrada para rechazar la fusión era la débil.** La fila la justificaba por coste y
precedente. La razón real es de honestidad: «(prueba)» marca la SONDA RESERVADA, y una fila de tendencia se
computa sobre las sesiones de entrenamiento del propio usuario. Fusionar mal-etiquetaría la procedencia. Eso deja
de ser un razonamiento y pasa a ser un test (`trendLabelsNeverClaimTheProbe_whileTransferLabelsDo`).

**C4 — La nota de inalcanzabilidad de la fila valía solo para la mitad del sujeto.** Decía «es `private` dentro
de un fichero Compose, luego INALCANZABLE desde `desktopTest`». Cierto de `metricLabel`, falso del conjunto
rastreado: `FocusProgressStateHolderTest` YA existía en `commonTest`. Esa mitad alcanzable es justo la que hacía
posible un guard por test, y no haberla mirado es lo que había estrechado el espacio de diseño a «guard de texto
en Gradle o nada».

**C5 — Mi mitad discriminante nació INALCANZABLE, y falló por la razón afortunada.** El helper `session()`
sellaba `measurementSufficient = null` (el default de `MetricValue`) y el gate del retest que yo mismo escribí en
R12 exige `== true`, así que NINGUNA línea de transferencia podía existir en ese fixture. El test cayó con
`NoSuchElementException`, no con una aserción — es decir, se salvó porque usé `.first { }`; escrito como
`none { }` habría pasado en VACÍO y yo habría dado por mecanizada una garantía inexistente. Se arregló
parametrizando el helper con un default que deja los tres tests preexistentes byte-idénticos en comportamiento.

**C6 — La mutación M2 encontró una debilidad real del guard, no solo lo confirmó.** Sin tipo explícito, la lista
se ensanchaba a `List<Serializable>` y el error de compilación aparecía en el DESTRUCTURING, lejos del elemento
culpable. Se añadió `List<Pair<MetricKey, String>>` y el error pasa a señalar la propia declaración
(`:81:61 Initializer type mismatch`). Toda la evidencia de mutación se rehízo contra la versión final.

**C7 — Ningún criterio de aborto se disparó.** A1 mecanizado por M1/M3/M4; **A2 por M2, que es un ERROR DE
COMPILACIÓN** (el guard vive en BUILD); A3 por la suite completa en verde y por medir que `Ready.trends` tiene un
único consumidor y un único sitio de construcción; A4 por los 4 escáneres y por no tocar el dominio.

**C8 — Incidentes de entorno, ninguno un cambio de código.** (a) Mi primer RED quedó ENMASCARADO por WIP ajeno en
fase RED (`LogicModels.kt`, `@Serializable` a medio poner, mtime 42 s antes de mi propia edición y `0` apariciones
de su símbolo en mi diff): se atribuyó y se esperó, no se arregló. (b) `Back-end (JVM) Internal error: Couldn't
inline method call` con causa raíz `FileNotFoundException` sobre símbolos ajenos, y con la huella `git status`
movida durante la corrida ⇒ INADMISIBLE. (c) `Failed to store cache entry … dirty-sources.txt` en M1 ⇒ INCONCLUSA.
(d) **Firma NUEVA:** `Failed to create MD5 hash for … shrunk-classpath-snapshot.bin (El sistema no puede encontrar
el archivo especificado)` con CERO errores de fuente — misma familia, se re-ejecuta. (e) `:androidApp:
compileDebugKotlin` volvió `UP-TO-DATE` y se re-ejecutó con `--rerun` en vez de aceptarse.
