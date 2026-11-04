# S5 · Paso 7 — Disposición de C4-01..C4-04 (fase de remediación)

**Estado:** `IMPLEMENTED` · `T-S5` sigue **NO EMITIDO**
**Entrada:** [`S5_PASO7_C4_INDEPENDENT_GATE.md`](S5_PASO7_C4_INDEPENDENT_GATE.md) · objetivo fijado en
[`C4_REVIEW_TARGET_MANIFEST.txt`](C4_REVIEW_TARGET_MANIFEST.txt) · dictámenes en
[`C4_LENS_DICTAMENES.json`](C4_LENS_DICTAMENES.json)
**Alcance:** disposición de los cuatro hallazgos que abrió el gate C4 y de la vacuidad del oráculo que
explica por qué sobrevivieron a una suite verde. **Sin montaje, sin promoción, sin commit.** `focus2`
sigue `UNMOUNTED`; R3 sigue `CANDIDATA`.

---

## 0. Qué es y qué no es este documento

Es el registro de **qué se hizo con cada hallazgo**, incluido el que se **refuta**. No es un cierre de
S5, no emite `T-S5` y no toca `C1`/`C2`/`C3`. Una disposición honesta incluye «este hallazgo era falso»
con la misma evidencia que exige un «este hallazgo era real».

---

## 1. C4-01 — CORREGIDO · P1

**Defecto.** `FocusTemporalEligibility.evaluate` sumaba `cell.totalTrials` sobre **todos** los buckets,
`NO_OBSERVATION` incluido. Consecuencia en cadena:

| magnitud | antes | por qué |
|---|---|---|
| `windowsClassified` | = `resolvedTrials` | una ventana no observada contaba como clasificada |
| `coverageComplete` | `true` | por la igualdad de arriba |
| `excludedFraction` | `0.0` | el bucket `EXCLUDED` está vacío |
| `rtShadowVerdict` | **`Eligible`** | el conjunto de causas sale vacío |

Una sesión en la que **no se pudo clasificar absolutamente nada** salía impecable. Es la inversión de
`null ≠ 0` en su dirección cara: la ausencia fabricando el máximo, no el mínimo.

**Alcanzable hoy, no teórico.** El `actual` de iOS declara `expectedCadenceHz = null`; `cadenceResolved`
falla para **cada** ventana ⇒ `NoObservation` en todas. Y `temporalSampledFrames` sigue acumulando
deltas sean o no clasificables (es su contrato desde el learning del 2026-08-18), así que el piso de
sesión se cumple y `sessionClass` llega a `T1_FRAME_OBSERVED`. Las tres causas quedan vacías.

**Corrección.** `NO_OBSERVATION` sale del numerador **y** del denominador:

- `windowsClassified` cuenta solo buckets clasificados ⇒ `coverageComplete` deja de ser vacío. El
  reducer alimenta el tally oficial y la celda sombra en el **mismo fold**, así que las dos sumas
  coincidían siempre que el probe devolviera *algún* veredicto por ventana — y `NoObservation` **es** un
  veredicto. `coverageComplete` solo podía ser `false` cuando el probe devolvía `null` (probe apagado);
  la ventana observable-pero-no-clasificable le era invisible. Justo el caso de iOS.
- `excludedFraction` pasa a `null` cuando no hubo nada clasificable ⇒ el guard `?: FULLY_EXCLUDED`
  existente dispara `SESSION_BACKSTOP_EXCEEDED`. **Doble fail-closed**, por cobertura y por backstop.
- Campo nuevo `windowsUnobserved`: la ausencia se **modela**, no se resta. Es la misma regla que obliga
  a `C4-04`, aplicada en el tipo en vez de en el call-site.

**No se re-pesó ni se re-clasificó nada.** Ninguna sesión que antes clasificaba algo cambia de
veredicto: las tres filas de la tabla de verdad que usan `null` como ventana no clasificada dan
exactamente los mismos números.

---

## 2. C4-02 — RE-SEVERIZADO, NO TOCADO · P3 → P1

`FrameTimingProbe.android.kt:55-56` conserva `?: DEFAULT_REFRESH_HZ`, el patrón que `MB-FOCUS-0069`
eliminó de `focus2`. La diferencia que el cierre canónico no ponderó: **esa sonda está cableada a
producción** en la lane de Velocidad, mientras el guard de honestidad de `P7-F1` está acotado por
nombre de fichero y no puede verla.

**Disposición: severidad al alza, código intacto.** Cambiarlo altera el veredicto de calidad de otra
lane, con su propio dueño y su propia decisión psicométrica; un gate de salida de `focus2` no la toma
por ella. Esto **respeta** la disposición original del cierre y corrige solo lo que estaba mal
calibrado: la severidad. `MB-FOCUS-0071` pasa a P1 con dueño explícito.

---

## 3. C4-03 — REFUTADO · sin cambio de comportamiento

**Afirmación.** Un tap que aterriza entre `presentNext()` y la asignación de `responseGate` se
descarta y se registra como `Response.NONE`, fabricando un fallo para los respondedores más rápidos.

**Refutación, y es del compilador, no de un test.** `presentNext()` está declarada `fun`, no
`suspend fun`. El sistema de tipos de Kotlin garantiza entonces que **ninguna** de sus llamadas puede
suspender, y no hay ningún otro punto de suspensión entre ella y el `also { responseGate = it }` de la
línea siguiente. Un único dispatcher no puede intercalar donde no puede ceder. La ventana que el
hallazgo describe **no existe**.

**Y el arreglo reflejo sería peor.** Armar el gate ANTES de presentar aceptaría un tap emitido cuando
todavía no había estímulo, puntuándolo contra una ventana que no se había abierto. El orden actual es
el correcto.

**No se añadió test, deliberadamente.** Un test que no puede fallar es exactamente la vacuidad que este
mismo gate encontró en el oráculo de Paso 4; añadirlo aquí sería reproducir el defecto mientras se
documenta. Lo que sí se añadió es la **precondición en la que descansa la refutación**, escrita junto
al campo: el holder está confinado a un dispatcher. Esa precondición no estaba declarada en ninguna
parte, y sin ella la refutación no es auditable.

**Residuo real, distinto del afirmado.** `responseGate` es un `var` sin `@Volatile`. Si un día el
llamador de `onResponse` vive en otro hilo, el problema no es intercalado sino **publicación**. Es una
obligación del slice que monte el holder, registrada al lado de la que `start()` ya declara
(`MB-FOCUS-0035`), y no se tapa con un mutex dentro de un holder no montado.

---

## 4. C4-04 — CORREGIDO · P2 · sin cambio de comportamiento

`evidenceOf` decidía con `(temporal?.excludedFraction ?: NO_BACKSTOP) > max`, donde `NO_BACKSTOP = 0.0`.
Ese centinela hacía **dos trabajos con un nombre**:

1. «no llegó ninguna evaluación» (`temporal == null`), y
2. «llegó una evaluación y no pudo derivar fracción» (`excludedFraction == null`).

Y entraba en una comparación `>`, que es la forma en que un valor que significa *no aplica* se
convierte en una medición que significa *no se excluyó nada*. El mismo `null` vale `1.0` (fail-closed)
en `FocusTemporalEligibility` y valía `0.0` (fail-open) aquí — asimetría **correcta**, porque la lane
de RT no puede afirmar nada sin captura temporal y la de exactitud no la necesita (es modality-fair,
charter §14), pero por eso mismo tiene que estar **escrita**, no inferida de un cero.

**Corrección.** El centinela desaparece; la regla se enuncia como rama (`backstopExceeded`), y el KDoc
declara por qué las dos lanes leen el mismo `null` de forma opuesta. Comportamiento idéntico en todos
los casos previos; lo que cambia es que ahora el caso (2) está **afirmado por un test** en vez de
heredado de una constante.

---

## 5. C4-05 — CERRADO · P2 (lo destapó ampliar el dominio; se cierra retirando la afirmación)

`TemporalWindowVerdict` documentaba que `DegradedUsable` «constrains shadow RT». **Nada lo hacía.** La
ventana cuenta como clasificada, aporta 0 a la ratio de exclusión y la sesión sale `Eligible`. Es una
distinción sin consecuencia — la misma familia que la serie write-only que Paso 7 borró.

### 5.1 Por qué se cierra ahora, cuando C4 lo dejó abierto

C4 lo dejó abierto porque su razón de cierre requería «la aritmética del tier `rt-infl-1` del ADR».
**Ese tier no existe.** Verificado sobre el fichero vivo: `rt-infl-1` no aparece ni una vez dentro del
bloque `R3-*`, que es el único autoritativo — el ADR marca en una sola línea que **todas** sus secciones
de numeración llana son historia R2 superseded:

```bash
A=docs/engines/focus_v5/ADR_FOCUS_TEMPORAL_QUALITY.md
H=$(grep -n 'SUPERSEDED por R3' "$A" | cut -d: -f1)          # abre el bloque HISTORIA
grep -n 'rt-infl-1' "$A" | awk -F: -v h="$H" '$1<h'          # 0 lineas: no existe en el bloque vigente
```

> **Trampa que esta sonda evita, y en la que caí primero.** Acotar con
> `sed -n '/^## R3-0/,$p'` llega hasta EOF, así que **incluye el bloque HISTORIA** que viene después:
> el conteo salía 4 de 4 y "demostraba" justo lo contrario. Las 4 apariciones reales de `rt-infl-1`
> están por debajo de la línea que abre HISTORIA. Una sonda sobre un documento cuya cola es historia
> tiene que acotar por esa frontera, no por el final del fichero.

La autoridad viva es **R3-11 / `D-R3-02` (CERRADA)**: en S5, `excludedFraction` y las simulaciones viven
**sólo en sombra**, y **ningún diagnóstico temporal produce evidencia de medición**. Es decir: en S5 no
existe ninguna afirmación de medida que una ventana degradada pueda ablandar. La premisa del hallazgo
—«hay un efecto prometido que falta»— era correcta sobre el KDoc y falsa sobre el contrato.

Así que el cierre honesto no es implementar el ablandamiento (eso sería congelar aritmética que posee
S6, exactamente lo que la fila prohibía): es **retirar la afirmación**. El comportamiento no cambia ni
un campo; cambia el documento que mentía sobre él.

### 5.2 El pin, y por qué la primera versión no valía

La equivalencia se afirma como **propiedad**, no como caso:

- `TemporalEvaluation` es un `data class`, así que `assertEquals` compara **todos** los campos,
  veredicto y causas incluidos.
- Se barre todo el rango de tamaños de sesión del oráculo, y además una sesión **MIXTA** (un
  subconjunto arbitrario de ventanas degradadas entre limpias): un pin que sólo compare
  todo-degradado contra todo-limpio seguiría pasando si el bucket adquiriese una consecuencia
  uniforme en toda la sesión.
- Mitad discriminante: `NoObservation` **no** colapsa en el mismo comportamiento — sí restringe.
- Guard anti-vacuidad de **contenido**, no de límites del bucle: `windowsClassified == n` y veredicto
  `Eligible`. La primera versión del barrido arrancaba en `n = 1`, que son 60 frames, por debajo de
  `MIN_FRAMES_SESSION`: **ambos brazos salían T0** y la igualdad se cumplía por la razón equivocada.
  Contar iteraciones habría sido reafirmar el rango, no el comportamiento.

Mutación ejecutada: mover `DEGRADED_USABLE` a la rama `false` de `isClassified()` (darle consecuencia)
pone en rojo los dos pins con `AssertionError` — no con error de compilación — y la restauración es
byte-idéntica por SHA-256. El día que alguien le dé una consecuencia de verdad, esto cae y obliga a
reescribir el KDoc en el mismo cambio.

### 5.3 Tres hallazgos nuevos que salieron de cerrar éste

- **`MB-FOCUS-0078` (CERRADO, con techo declarado).** El guard AC#4 que exige productor vivo para cada
  causa de exclusión era **fail-open**: decidía con `contains("WindowExclusionReason.<ENTRADA>")`, que no
  distingue un sitio de construcción de una mención — un consumidor que nombra la entrada con sufijo
  (`REASON.name`) o un comentario la satisfacían. Probado por mutación: sustituir el único productor real
  por un round-trip `.name` deja el guard viejo **verde**. Endurecido en sitio (lookahead negativo,
  comentarios eliminados, anti-vacuidad sobre el número de fuentes escaneadas). **Techo que NO se cierra
  y se declara:** un `== REASON` pelado, una rama `when` o un `import` de la entrada seguirían contando
  como productor. Hoy no existe ningún sitio así — los únicos matches fuera del fichero declarante son
  las tres construcciones reales — y distinguirlos de una construcción no es algo que un regex haga con
  robustez. Se cierra el fail-open **medido y mutado**, no la clase entera.
- **`MB-FOCUS-0079` (ABIERTO, con dueño).** Nada distingue una cita al bloque R2 superseded del ADR como
  **evidencia preservada** de una cita como **autoridad viva** — y la frontera importa, porque el propio
  bloque R3 cita la historia legítimamente (`R3-11 … evidencia §9.11`). El defecto es citarla como norma
  en vigor, y ocurrió en **producción**: `FocusTemporalEligibility.kt` encabezaba el fichero declarando
  como contrato una sección llana, y justificaba conservar `Verdict.Degraded` con «the ADR names it».
  Alcance **medido, no estimado**: 68 líneas en 29 ficheros, repartidas por commonMain, desktopTest,
  desktopMain, iosMain y docs. Esta fase corrige las **5** citas de las que colgaba una decisión
  (`rt-infl-1` vivo en `shared/src` pasa de 1 a 0); las 68 quedan por clasificar y nada mecaniza la
  frontera. Cerrarlo exige un guard o partir el ADR — decisión de su dueño.
- **`MB-FOCUS-0080` (ABIERTO, con dueño).** `Verdict.Degraded` es una variante sellada **sin productor**
  que sobrevivía por una razón retirada, y recibe el trato **opuesto** al de sus gemelas: Paso 5 borró
  las entradas sin productor de `WindowExclusionReason` y mecanizó la regla, mientras ésta se conserva
  porque «el ADR la nombra» — cierto sólo dentro de HISTORIA. Aquí se corrige **sólo el KDoc**, que es lo
  que era falso; borrarla o conservarla es la misma decisión psicométrica por la que 0076 estuvo
  abierto, y sigue pinneada como inalcanzable, que es fail-closed.

### 5.4 Lo que el gate de salida me corrigió a mí

Seis hallazgos confirmados, **cero refutados**. Cuatro contradecían directamente mi propio trabajo y se
corrigieron antes de sellar nada:

1. `rt-infl-1` seguía citado **en producción** (`FocusTemporalEligibility.kt`), así que mi enumeración
   «2 artefactos» era falsa y la sellaba como `VERIFIED_PRIMARY`. Un grep de cinco segundos la refutaba.
2. La razón para conservar `Verdict.Degraded` descansaba en el bloque superseded — el defecto que esta
   misma fase abre, dentro del fichero que esta misma fase cierra.
3. El alcance de 0079 estaba subestimado en un orden de magnitud (2 frente a 68).
4. Mi prosa afirmaba que el lookahead cubre `== REASON`. No lo cubre. Tres sitios lo decían: el comentario
   del test, la fila del ledger y el aprendizaje.

Y dos guards vacíos preexistentes en el oráculo, que este trabajo hereda y arregla porque son exactamente
la patología que la fase documenta: `assertEquals(FULL_WINDOW_DOMAIN.size, reached.size)` era una
tautología (`map` preserva el tamaño), y `assertEquals(chains, eligibleObserved.coerceAtMost(chains))` era
la línea siguiente escrita al revés, con un mensaje que prometía una propiedad **por cadena** que un
contador global no puede sostener.

### 5.5 Lo que esta fase se quitó de encima

La primera versión de esta fase añadió un fichero de test nuevo para el invariante AC#4. Era un
**duplicado**: Paso 5 ya lo había mecanizado en `TemporalClassPolicyTest`. El gate de salida lo detectó
y el duplicado se **borró**. Dos guards del mismo invariante son dos dueños y una divergencia futura;
endurecer el que ya existía es el diff más pequeño y deja un solo dueño.

---

## 6. La causa raíz: por qué una suite verde no lo vio

El oráculo adversarial de Paso 4 pinnea el **codominio alcanzable** de `evaluate()`. Su **dominio de
entrada** sobre `TemporalWindowVerdict` era, literalmente, dos valores y un nulo:

```
FocusTemporalEligibilityAdversarialOracleTest.kt  (estado PRE-fase, lineas 51-52)
    private val excluded = TemporalWindowVerdict.Excluded(WindowExclusionReason.TRUNCATION_RISK)
    private val clean    = TemporalWindowVerdict.Clean
```

Las dos declaraciones siguen existiendo, desplazadas a 54-55 por el `FULL_WINDOW_DOMAIN` que anade esta
fase. Se localizan sin depender del numero de linea con
`grep -nE 'val (excluded|clean) =' <fichero>`, y se citan asi **a proposito**: una cita posicional
deja de resolver en cuanto alguien edita el fichero, y una evidencia que no se puede volver a comprobar
no es evidencia. Esta fase acaba de desplazarlas ella misma.

`NoObservation` y `DegradedUsable` —la mitad de la jerarquía sellada, y la mitad sobre la que vive
iOS— **no llegaban a este SUT desde ningún test**: aparecen solo en la capa de clasificación
por-ventana (`TemporalClassPolicyTest`) y en el fold del reducer, nunca en la elegibilidad de sesión.
Afirmar un codominio mientras se mata de hambre al dominio es la regla de la «mitad discriminante» del
charter aplicada al revés.

**Guard añadido (Fase H).** El dominio completo se declara y se comprueba con un `when` **exhaustivo**
sobre la jerarquía sellada: añadir una variante a `TemporalWindowVerdict` **rompe la compilación** del
oráculo, así que la nueva variante no puede quedarse sin alimentar como quedaron estas dos. Un chequeo
reflexivo (`sealedSubclasses`) habría sido una aserción en ejecución; esto lo es en build.

---

## 7. Lo que esta fase NO hizo

- **No emite `T-S5`.** `C1` (CI iOS sobre este árbol) sigue bloqueada por falta de autorización de
  commit; `C2` (validación física, `MB-FOCUS-0066`) y `C3` (política MNAR, `MB-FOCUS-0053`) no se
  tocaron.
- **No monta nada.** `focus2` sigue sin consumidores de producción.
- **No toca la lane de Velocidad** (C4-02) **ni redefine `DegradedUsable`** (C4-05): la fase P9 que
  cerró C4-05 lo hizo retirando una afirmación falsa del KDoc, con **cero cambio de comportamiento** —
  cuánto debe pesar una ventana degradada sigue siendo aritmética que posee S6, y `Verdict.Degraded`
  sigue sin productor y pinneada como inalcanzable (`MB-FOCUS-0080`).
- **No re-audita**: las 29 observaciones que C4 dejó como `PLAUSIBLE` sin refutación siguen sin probar,
  y siguen declaradas como tales en §1.1 del gate.
