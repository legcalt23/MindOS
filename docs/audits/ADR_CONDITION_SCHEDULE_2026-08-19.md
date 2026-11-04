# ADR — El reparto de condiciones se SORTEA POR CUOTA, no por moneda por ensayo

- **Fecha:** 2026-08-19
- **Fase:** plan blindado del catálogo cognitivo · **R9**
- **Hallazgo:** `MB-CATALOG-0014` (abierto por R8 al CALCULAR el techo, no al suponerlo)
- **Estado:** DECIDIDO antes de tocar código (§5 es el compromiso; §6 el alcance; §7 lo que NO se decide aquí)

---

## 1. FASE 0 — verificación del enunciado contra el repo

El hallazgo dice, verbatim:

> Que un retest pueda sostener una afirmación de capacidad en `CONGRUENCY_EFFECT` depende de la SEMILLA.
> `calibrationDifficulty` no fija `interference` … el bloque E2 de baseline (60 ensayos, Bernoulli por ensayo,
> NO cuota) produce una mediana de ~12 incongruentes frente al piso central de 20/celda.

**Cada símbolo verificado contra el árbol real:**

| Afirmación | Verificación | Veredicto |
|---|---|---|
| `calibrationDifficulty` no fija `interference` | `BaselineProtocol.kt:79-82` — solo `perceptualLoad` y `temporalUncertainty`; los otros 7 ejes caen al default `AxisLevel.MIN` de `DifficultyVector` | **CIERTO** |
| `incongruentProportion = 0.20` en ese punto | `E2DifficultyMapping`: `lerp(0.20, 0.60, d.interference.value)` con `value = 0.0` | **CIERTO** |
| El bloque de baseline es de 60 ensayos | `BaselineProtocols.INITIAL`, bloque `E2_FILTRO`, `targetTrialCount = 60` | **CIERTO** |
| El sorteo es Bernoulli por ensayo | `E2FiltroGenerator`: `val congruent = random.nextDouble() >= params.incongruentProportion`, dentro del `map` por índice | **CIERTO** |
| El piso es 20/celda con todas las celdas presentes | `MinSampleGate.specFor(CONGRUENCY_EFFECT)` = `nMinPerCell = 20`, `requiresAllCells = true` | **CIERTO** |
| El conteo de estímulos es una COTA SUPERIOR de la celda real | Solo los HIT alimentan las celdas de RT; un fallo o una omisión no aportan RT | **CIERTO** |

**El enunciado es correcto.** Es la primera vez en este plan que un hallazgo heredado sobrevive intacto a la
FASE 0 (R8 tuvo que corregir el suyo en seis puntos). Lo que sí resulta incompleto es su **alcance**: §2.

## 2. Corrección de alcance — E2 es el síntoma, no la familia

`MB-CATALOG-0014` nombra `CONGRUENCY_EFFECT`/E2 porque es donde R8 lo tropezó. Pero la causa —«la pertenencia
a la condición se sortea con una moneda independiente por ensayo»— es **el patrón de TODOS los generadores de
Enfoque cuya condición es la celda de una métrica con piso central propio**:

| Ejercicio | Métrica primaria | Piso central | Bandera sorteada | Cómo se sortea hoy |
|---|---|---|---|---|
| E2 Filtro | `CONGRUENCY_EFFECT` | 20/celda, todas | `Flanker.congruent` | `nextDouble() >= p` por ensayo |
| E5 Conflicto | `CONGRUENCY_EFFECT` | 20/celda, todas | `Conflict.congruent` | `nextDouble() >= p` por ensayo |
| E6 Cambio | `SWITCH_COST` | 20/celda, todas | `TaskSwitch.isSwitch` | `nextDouble() < p` por ensayo (`index > 0`) |
| E8 Duo | `DUAL_TASK_COST` | 20/celda, todas | `DualTask.dual` | `nextDouble() < p` por ensayo |
| E13 Presión | `PRESSURE_DELTA` | 20/celda, todas | `Pressure.underPressure` | `nextDouble() < p` por ensayo |
| E4 Freno | `SSRT` | 30 paradas | `StopSignal.isStop` | `nextDouble() < p` por ensayo (+ un índice forzado) |
| **E12 Reenganche** | `POST_ERROR_*` | 20/celda, todas | — | **la celda es CONDUCTUAL** (si el usuario erró), no sorteada |

Ese conjunto **no se enumera a mano**: se DERIVA de `MeasurementSufficiency.central` (la tabla que construyó R8)
como «las métricas primarias del ejercicio cuyo id central NO es `ACCURACY_RT_BASE`». `SET_SIZE_SLOPE` (E7) y
`DISTRACTION_RESISTANCE_INDEX` (E11) quedan fuera solos, porque no tienen piso ratificado (`central` devuelve
`null` = «la pregunta no aplica»). E12 es la **única exclusión declarada**: su celda no la reparte el generador.

> **Corregido al implementar (§8·C1):** esta tabla, escrita a mano, olvidó **E3 Gatillo**, que entra por
> `POST_ERROR_SLOWING` igual que E12. Lo cazó la derivación en su primera ejecución. Las exclusiones son DOS.

Arreglar solo E2 dejaría a cinco hermanos con el mismo defecto — el fallo que `CLAUDE.md` describe como
«parchear solo la ruta que nombra el ticket».

## 3. Reachability CALCULADA (no supuesta)

Binomial exacta sobre el generador real, en los dos extremos del eje que gobierna cada proporción. `P(elegible)`
= probabilidad de que la celda MENOR alcance el piso (para SSRT, de que las paradas alcancen 30):

| Caso | n | p | `P(elegible)` HOY | celda menor CON cuota | veredicto con cuota |
|---|---|---|---|---|---|
| **E2 baseline (calibración, `interference`=MIN)** | 60 | 0.20 | **1.1 %** | 12 | DENIEGA siempre |
| E2 entrenamiento, `interference`=MAX | 60 | 0.60 | 88.3 % | 24 | ACEPTA siempre |
| E5, `interference`=MIN | 60 | 0.25 | 9.3 % | 15 | DENIEGA siempre |
| E5, `interference`=MAX | 60 | 0.65 | 65.3 % | 21 | ACEPTA siempre |
| E6, `interference`=MIN | 59 | 0.30 | 30.0 % | 18 | DENIEGA siempre |
| E6, `interference`=MAX | 59 | 0.60 | 86.3 % | 24 | ACEPTA siempre |
| E8, `dualTaskLoad`=MIN | 60 | 0.40 | 88.3 % | 24 | ACEPTA siempre |
| **E8, `dualTaskLoad`=MAX** | 60 | 0.75 | **9.3 %** | 15 | DENIEGA siempre |
| E13, `pressure`=MIN | 60 | 0.40 | 88.3 % | 24 | ACEPTA siempre |
| E13, `pressure`=MAX | 60 | 0.60 | 88.3 % | 24 | ACEPTA siempre |
| E4, `inhibitoryDemand`=MIN | 60 | 0.25 | 0.0 % | 15 | DENIEGA siempre |
| E4, `inhibitoryDemand`=MAX | 60 | 0.33 | 0.5 % | 20 | DENIEGA siempre |

Tres lecturas que el hallazgo no contenía:

1. **La fila E8 es peor que la de E2.** Subir la dificultad del ejercicio (más ensayos duales) **destruye** la
   comparación: 88 % → 9 %. Es decir, el motor adaptativo, haciendo exactamente su trabajo, mueve en silencio la
   PROBABILIDAD de que la sesión pueda sostener una afirmación. Nadie declaró jamás ese acoplamiento.
2. **`SSRT` en E4 es un techo duro, no una cola**: con `n = 60` el máximo alcanzable son ~20 paradas frente a un
   piso de 30. Ninguna dificultad lo cruza. Es la forma exacta de `MB-CATALOG-0009` (R7) en otro eje.
3. La cuota no «arregla» la elegibilidad: la vuelve **determinista**. La mitad de las filas pasan a aceptar
   siempre y la otra mitad a denegar siempre — y ambas cosas se pueden afirmar en un test de build.

## 4. Diagnóstico

Un reparto de condiciones tiene dos propiedades independientes que hoy comparten un solo mecanismo:

- **la proporción esperada** — es diseño del ejercicio, la gobierna el eje de dificultad, y debe conservarse;
- **el tamaño de celda realizado** — es la MUESTRA de la comparación, y no debería depender del azar.

Sortear con una moneda por ensayo las acopla: fija la primera y deja la segunda como variable aleatoria. Es la
familia de R7 (`MB-CATALOG-0009`) girada: allí un valor cargaba dos significados y se separó modelando; aquí un
mecanismo entrega dos propiedades y se separa sorteando por cuota.

Y es peor que un techo, como ya observó R8: un techo se declara y se acabó; una **cola** deja la afirmación de
capacidad a merced del sorteo — con un 1 % de usuarios cuya semilla les concede una comparación que el diseño
no les concede.

## 5. Decisión

**Se sortea por CUOTA: exactamente `round(n · p)` ensayos de la condición, y luego se baraja con el MISMO
generador de aleatoriedad.**

- La proporción esperada **no cambia** (`round(n·p)/n ≈ p`), así que ninguna curva de dificultad se re-parametriza.
  Esto es deliberado y es el criterio de aborto: **si alguna proporción hubiera que tocarla, la decisión sería de
  producto y no se tomaría aquí** (mismo compromiso que R7: re-pesar es psicometría, re-modelar una entrada no).
- La varianza del tamaño de celda pasa a **cero**. La elegibilidad se vuelve una función determinista de
  `(n, dificultad)`, computable y afirmable en un test.
- El orden sigue siendo impredecible para el usuario (se baraja), que es lo único que la aleatoriedad tenía que
  proteger. Una lista de ensayos contrabalanceada es además la práctica estándar en flanker/Stroop/task-switching;
  la moneda por ensayo era la desviación, no la cuota.
- **La tabla central NO se toca** (anti-patrón cerrado por R7 y prohibido explícitamente por el propio hallazgo).

### Por qué no las otras dos opciones que el hallazgo nombra

| Opción | Por qué no en R9 |
|---|---|
| Subir el bloque E2 de baseline | Cambia la duración del baseline (`estimatedDurationMs`) y por tanto el producto. Decisión de producto: se DECLARA, no se toma. |
| Fijar `interference` en `calibrationDifficulty` | Cambia qué significa «dificultad de calibración» y hace el baseline más duro. Decisión de producto **y** psicométrica. Se DECLARA. |

Ambas siguen siendo necesarias para que `CONGRUENCY_EFFECT` llegue a ser elegible en baseline. La cuota no las
sustituye: las hace **decidibles**, porque después de ella el déficit es un número fijo (12 frente a 20) en vez
de una lotería.

## 6. Alcance del cambio

1. Un único helper `internal fun conditionSchedule(size, proportion, random, atLeast)` junto a `lerp`, en
   `TrialGenerator.kt` (mismo paquete, misma visibilidad). Ni objeto, ni interfaz, ni archivo nuevo.
2. Seis generadores lo consumen: E2, E4, E5, E6, E8, E13. E12 queda excluido **con la razón declarada en el test**.
3. **E2 sube de versión.** Su propio contrato lo exige: «Bump on any change to the drawn sequence». Y son DOS
   declaraciones independientes que deben concordar (`E2FiltroGenerator.E2_GENERATOR_VERSION` y
   `FocusFormContract.FORMS[E2_FILTRO]`), sostenidas por `ProductionFocusGraphWiringTest`. Además
   `generatedPlanDigest` cambiaría de todos modos: es exactamente el mecanismo que existe para cazar una deriva
   sin bump. E4/E5/E6/E8/E13 son generadores LEGACY sin versión ni digest: no hay nada que subir.
4. Ningún test existente afirma una secuencia exacta de estos generadores (verificado uno a uno): todos afirman
   determinismo, monotonía (`switches(1.0) > switches(0.0)`) o invariantes estructurales, y todos sobreviven —
   la cuota conserva el orden de la monotonía y refuerza el determinismo.

## 7. Lo que este ADR NO decide (y queda declarado con dueño)

- **El tamaño del bloque de baseline** y **el valor de `interference` en calibración**: decisión de producto.
  Tras R9 el déficit deja de ser probabilístico y pasa a ser un número exacto sobre el que decidir.
- **`SSRT` es inalcanzable en bloques de 60** (techo duro de ~20 paradas frente a 30). Hallazgo nuevo.
- **La dificultad mueve la elegibilidad de la afirmación** (E8: 88 % → 9 %; con cuota: acepta → deniega). Que el
  lazo de control decida en silencio si una sesión puede sostener una afirmación es un acoplamiento no declarado.
  Hallazgo nuevo; su remedio no es la cuota.
- **El conteo de estímulos es una cota superior de la celda real** (solo los HIT aportan RT). La cuota fija la
  cota, no la celda; la celda seguirá siendo menor y sigue midiéndose donde se mide.

## 8. Correcciones descubiertas AL IMPLEMENTAR (registradas como correcciones, no reescritas)

### C1 — la tabla de §2 estaba incompleta: faltaba E3

La primera ejecución del forcing-function falló, y falló **contra este ADR**: la derivación mecánica devolvió
`{E2, E3, E4, E5, E6, E8, E12, E13}` y la tabla de §2 declaraba siete de esos ocho. **E3 Gatillo** entra en el
conjunto gobernado por `POST_ERROR_SLOWING`, exactamente igual que E12 — es decir, por una celda **conductual**,
no por su reparto go/no-go.

Es la clase de error que el propio mecanismo existe para cazar: una enumeración a mano —la mía, escrita en §2—
frente a una derivación de la autoridad. Ganó la derivación en su primera ejecución.

Consecuencia: la fila de exclusión de §2 debe leerse **`E3_GATILLO` y `E12_REENGANCHE`**. Y la razón importa:
el reparto go/no-go de E3 SÍ lo sortea el generador, pero `FALSE_ALARMS`/`OMISSIONS` no tienen piso central
ratificado, así que ninguna afirmación descansa en esa celda y no debe cuota. Si algún día se les ratifica un
piso, la derivación saca a E3 de las exclusiones y el test falla hasta que alguien lo decida — que es el
comportamiento correcto y no había forma de obtenerlo con una lista escrita a mano.

### C2 — la cola medida es más ancha de lo que R8 estimó

R8 calculó `P(celda >= 20) ≈ 1.1 %` para el bloque de baseline. La ejecución en ROJO sobre 40 semillas devolvió
celdas entre **4 y 20**, con una semilla exactamente en 20. La estimación era correcta; verla concreta es lo que
la vuelve accionable: en una muestra de 40 usuarios, uno recibió la comparación que el diseño no le concede.

