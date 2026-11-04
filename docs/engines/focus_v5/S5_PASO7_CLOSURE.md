# S5 · Paso 7 — cierre de S5: gate de salida sobre el diff final + cierre del plan maestro

<!-- FOCUS_V5_AUTHORITY_BLOCK_START -->
FOCUS_V5_CURRENT_AUTHORITY_INDEX: docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md
<!-- FOCUS_V5_AUTHORITY_BLOCK_END -->

**ESTADO: `CONDITIONAL` — `T-S5` NO EMITIDO (2026-08-18).**
Gate del plan (§5, P7): «`T-S5` o CONDITIONAL honesto». Es CONDITIONAL, y §7 nombra exactamente qué falta.

**R3 sigue CANDIDATA / UNMOUNTED. Paso 7 no la promueve, no la congela y no la monta.**

---

## 0. Lo que este documento se niega a hacer

1. **No convierte «revisado por mí» en «auditado por terceros».** El gate de salida del plan pide 8 lentes.
   Esta sesión no tenía autorización para lanzar subagentes, así que las 8 lentes las ejecutó la sesión
   principal en pasadas separadas y dirigidas. La clase de evidencia es `AI_SELF_REVIEW_MULTI_LENS`, NO
   `INDEPENDENT_AGENT_REVIEW` ni `HUMAN_EXTERNAL_AUDIT` (§1). Lo que las lentes encontraron es real y está
   corregido; lo que NO son es independientes, y eso no se disimula.
2. **No emite `T-S5`.** El techo honesto ya estaba escrito en `S5_REOPEN_STATUS §5`: mientras Paso 6 sea
   `DESIGNED_NOT_RUN` y el runtime iOS no haya compilado, S5 cierra CONDITIONAL. Emitir el sello igualmente
   sería exactamente el «status-inflation» que `MB-FOCUS-0048` registra.
3. **No arregla WIP ajeno.** El árbol contiene trabajo simultáneo de otras pistas (emociones · Ola 4,
   auditorías de módulos, iosMain de dm/social). Está fuera del diff bajo revisión y no se ha tocado (§2.2).

---

## 1. Clase de evidencia de este gate (declarada antes de los resultados)

| Qué pide el plan | Qué ocurrió | Clase |
|---|---|---|
| Gate de salida de **8 lentes** sobre el diff final | 8 pasadas dirigidas de la sesión principal, cada una con su pregunta, su recorrido de fuentes y su veredicto | `AI_SELF_REVIEW_MULTI_LENS` |
| Contexto fresco (`diff-auditor`) | La lente 8 se ejecutó al final, sobre el diff YA corregido por las siete anteriores | `SAME_SESSION_FINAL_PASS` — más débil que contexto fresco, declarado |
| Gates mecánicos | `desktopTest` / `detekt` / `compileDebugKotlin` ejecutados de verdad, en serie, con conteo de XML frescos | `EXECUTED_PRIMARY` |
| Compilación iOS | No ejecutable en host Windows | `NOT_EXECUTED` |

**Corolario honesto:** un hallazgo que estas lentes NO vieron no queda descartado por este documento. Lo que
sostiene el cierre es la lista de defectos que sí encontraron y corrigieron (§3-§4), no la ausencia de otros.

---

## 2. El diff final bajo revisión

### 2.1 Alcance S5 (lo que este gate revisa)

Base: `HEAD 8a06b9a`. Todo lo de abajo es working-tree (S5 nunca se ha commiteado — `MB-FOCUS-0050`).

**Modificado (12 archivos, +498 / −37 antes de Paso 7):**

| Archivo | Papel en S5 |
|---|---|
| `core/timing/FrameStatsCalculator.kt` | guard fail-closed de cadencia no-finita + sobrecarga primitiva (Paso 5) |
| `core/timing/FrameTimingProbe.android.kt` | `DisposableEffect` (Paso 5, B3) |
| `focus2/domain/runtime/FocusRuntimeModel.kt` | celdas estratificadas v2 + `openConditionName` + frames de sesión |
| `focus2/domain/runtime/FocusSessionReducer.kt` | fold shadow por bucket + descartes atribuibles por condición |
| `focus2/domain/runtime/FocusSessionRuntime.kt` | veredicto temporal como dato de entrada |
| `focus2/domain/runtime/FocusSessionDescriptor.kt` | sellado de capacidades temporales (schema v2) |
| `focus2/presentation/FocusTracerStateHolder.kt` | posesión del probe, dos drenajes por trial |
| `focus2/presentation/ui/FocusTracerScreen.kt` | descripción a11y ciega a la respuesta (`MB-FOCUS-0051`) |
| `core/timing/FrameStatsCalculatorTest.kt` | goldens de NaN/mitad-de-tasa |
| `docs/context_pack/{05,09}` · `ADR_REGISTER_V5.md` | reconciliación documental |

**Nuevo (29 archivos):** 11 de producción (`core/timing/**` = anillo, política, umbrales, observación,
veredicto, vocabulario, probe + 3 actuals; `focus2/domain/evidence/**` = elegibilidad temporal y estado de
evidencia) y 18 de test (~2.300 líneas), incluidos el oráculo adversarial (522) y la política (321).

### 2.2 Fuera de alcance (presente en el árbol, NO revisado, NO tocado)

`features/mindboost/emotions/**` (Ola 4, escritor concurrente) · `features/dm|social/**` iosMain (port iOS) ·
`docs/audits/**`, `audit-output/`, `security-audit/`, `docs/mindboost_calibracion_progresiva/` ·
`package.json` · `scripts/pii-secret-scan*`, `parse-junit-xml`, `build-r3-evidence-archive` (pista R3 reaudit,
sin cambios esta fase).

---

## 3. Gate de salida — 8 lentes

Cada lente: una pregunta, un recorrido, un veredicto. Los hallazgos van en §4.

### L1 · eficacia (¿mide honestamente lo que dice medir?) — **APTO_CON_OBSERVACIONES → APTO tras corrección**

Recorrido: `TemporalClassPolicy`, `FrameStatsCalculator`, los tres actuals del probe, `FocusTemporalEligibility`.

**Encontró `P7-F1` (P1): el adapter Android fabricaba la cadencia esperada.** `view.display?.refreshRate`
caía a `DEFAULT_REFRESH_HZ` (60,0) cuando la vista estaba desacoplada o el rate no era válido. En un panel de
60 Hz la expectativa fabricada COINCIDE con la detectada, la ventana resuelve, y el veredicto —CLEAN o
EXCLUDED— sale de una constante que nadie midió. Es la misma inversión que `MB-FOCUS-0065` corrigió en iOS
(que declara `expectedCadenceHz = null` honestamente): las dos plataformas tienen que fallar igual o no son
comparables. El guard de política no podía verlo, porque para cuando la observación llega la ausencia ya viene
disfrazada de afirmación. **Corregido** (§4).

Verificado además: `null ≠ 0` en ambas direcciones (`frameDropFraction()` devuelve `null` sin muestra, no 0,0;
`FrameStats.UNAVAILABLE` ante cadencia no finita); `NoObservation` nunca retira accuracy; `bufferOverflow` va a
`NoObservation` y no a `Excluded` (el tamaño de nuestro buffer no es un hecho sobre el trial del usuario).

### L2 · psicometría (¿la aritmética sostiene el veredicto?) — **APTO**

Recorrido: `FocusTemporalEligibility.evaluate`, `TemporalQualityThresholds.init`, oráculo adversarial.

Sin doble conteo: `excludedByReason` de un bucket EXCLUDED también cuenta `TRUNCATION_RISK`, pero el
denominador solo suma `PAUSE_DISCARD`/`CLOCK_EPOCH` como descartes, y los excluidos resueltos entran una sola
vez por `cell.totalTrials`. `excludedFraction == null` no pasa el techo (`FULLY_EXCLUDED = 1.0`): fail-closed,
no un 0,0 falso. `coverageComplete` compara ventanas clasificadas contra trials OFICIALMENTE resueltos, así que
una ventana sin veredicto degrada la elegibilidad en vez de pasar inadvertida. El validador de config solo
ordena magnitudes de la MISMA población, y declara por qué no ordena `windowDropExcludeMin < sessionExcludedMax`.

### L3 · assessment / estados de evidencia — **APTO**

`FocusEvidenceStatePolicy`: `OFFICIAL_ELIGIBLE` es INCONSTRUCTIBLE por test exhaustivo (`D-R3-02`), el backstop
cae a `TRAINING_ONLY`, y `NotCaptured` NO degrada la lane descriptiva. Nada RT-crítico entra a evidencia
oficial, que es la regla cerrada de `§4.4` del contrato.

**Observación `P7-F4` (P3) — enunciado CORREGIDO el 2026-08-28 (`MB-FOCUS-0080`):** `Verdict.Degraded` no
tenía productor y su KDoc lo «reservaba para Paso 4»; Paso 4 y Paso 5 cerraron sin construirlo. Este párrafo
decía que se mantenía declarado «porque el ADR §9 lo nombra como parte del vocabulario» — y esa razón era
FALSA: §9 vive por debajo del marcador `# HISTORIA — R2 (SUPERSEDED…)`, así que es evidencia conservada y no
vocabulario vigente (`MB-FOCUS-0079`). Retirada la razón, no quedaba ninguna: la variante está **BORRADA**, lo
que restaura la simetría con las entradas sin productor que Paso 5 borró de `WindowExclusionReason` en vez de
reservarlas. Lo que licenció el borrado es que el bloqueo se disolvió — `D-R3-02` (CERRADA, R3-11) deja todo
diagnóstico temporal en SOMBRA y declara que ninguno produce evidencia de medición en S5, luego no existe
afirmación de medida que un veredicto degradado pueda ablandar ni aritmética que congelar. El pin de
inalcanzabilidad del oráculo se retiró con ella y lo sustituye un `when` exhaustivo: si S6 reintroduce el
tier, el compilador para hasta que alguien lo trate. **Ausencia impuesta por el compilador, no vigilada en
ejecución.**

### L4 · arquitectura KMP — **APTO**

`core/timing` no importa NADA de `features.*` fuera de su propio paquete (verificado por grep): la dirección de
dependencia se mantiene, y por eso el calculador puede compartirse con la lane de Velocidad sin acoplarla al
versionado `tqt-1`. `expect`/`actual` completos en los tres source sets. El `TemporalWindowObservation` viaja
como DATO al reducer; ninguna capa de plataforma decide nada.

### L5 · rendimiento móvil / efecto observador — **APTO_CON_OBSERVACIONES**

Anillo `LongArray` de tamaño fijo + sobrecarga primitiva ⇒ cero allocations por frame POR CONSTRUCCIÓN (el tipo
lo garantiza, no una afirmación). `DisposableEffect` en Android e iOS ata el callback auto-reposteante a la vida
de la composición. Contador de generación contra la recomposición que relanza `start` sin que el `stop` previo
haya aterrizado.

**Observación (sin cambio):** la MAGNITUD residual del efecto observador sigue sin medir — `MB-FOCUS-0066`,
bloqueante B5 de `§R3-19`. Paso 6 escribió el protocolo que lo cerraría; hace falta dispositivo.

**Observación cruzada `P7-F5` (P3, otro dueño):** `FrameTimingProbe.android.kt` —la sonda de la lane de
Velocidad, tocada en Paso 5 solo para el `DisposableEffect`— conserva el mismo fallback a `DEFAULT_REFRESH_HZ`
que L1 acaba de retirar de focus2. Ahí alimenta la clasificación de calidad de Velocidad, no la de Focus, así
que cambiarlo altera el comportamiento de OTRA lane y exige su propia decisión. Registrado, no tocado.

### L6 · privacidad y seguridad — **APTO_CON_OBSERVACIONES**

Persistido: solo agregados enteros por (condición × bucket) y conteos por razón; ninguna serie ordenada, ningún
timestamp absoluto sale del adapter (todas las magnitudes son duraciones intra-dominio). El descriptor sella la
cadencia en BANDA (`HzBand`), no en Hz exactos (`§R3-17`). `Focus2PlatformPermissionTest` fija que focus2 y
`core/timing` no piden permisos, no leen sensores y no abren sockets.

**Encontró `P7-F2` (P2): una serie ordenada viva en memoria, sin lector.** `windowVerdicts` acumulaba el
veredicto de cada ventana «para los detectores de Paso 4». Paso 4 y Paso 5 cerraron sin ellos: un escritor, cero
lectores, tres fases. Además se añadía ANTES del check de rechazo del runtime, así que un tap fuera de fase
metía una entrada fantasma sin ventana resuelta detrás — el primer consumidor que llegara habría heredado una
serie desalineada en silencio. **Eliminado** (§4).

**Observación (sin cambio):** `MB-FOCUS-0048` (`F-R3-A` GAP-1/GAP-2, revocación subject-scoped de focus2) sigue
OPEN, con dueño distinto y un gate de 9 interleavings. Cablear a medias una autoridad de revocación es el fallo
del learning 2026-07-14; se deja declarado, no a medio hacer.

### L7 · ingeniería de tests (¿los tests prueban lo que dicen?) — **APTO tras corrección**

El oráculo adversarial fija el codominio alcanzable y falla si `evaluate` deja de producir cualquier veredicto o
causa; los goldens de cadencia llevan su mitad discriminante; la paridad doc↔config se verificó por mutación.

**Encontró `P7-F6` (P2) en un test de ESTA fase.** El primer guard escrito para `P7-F1` acotaba la búsqueda al
lambda del provider con `substringBefore("}")`, que **corta en la llave de cierre del `takeIf { … }` interior**:
al re-introducir el fallback por mutación, el guard siguió VERDE. Se detectó porque la mutación es obligatoria
antes de dar un test por bueno, no porque el test se leyera mal. Reescrito a fichero completo (la constante no
tiene otro uso legítimo en ese adapter) y **re-verificado: mutación ⇒ ROJO, restauración byte-idéntica por SHA
⇒ VERDE**.

### L8 · auditoría del diff integrado (pasada final) — **APTO_CON_OBSERVACIONES**

Recorrido: el diff completo de §2.1 tras las siete lentes anteriores, contra los documentos que lo describen.

**Encontró `P7-F3` (P2): `differentialExclusionDelta`, umbral versionado con cero consumidores.** La R2 definió
el detector de exclusión diferencial, la R3 lo retiró y ninguna fase posterior lo reconstruyó, pero la constante
siguió en `TemporalQualityThresholds` durante tres fases — y **Paso 6 la pinneó en el bloque de paridad de su
protocolo describiéndola como «el detector de exclusión diferencial»**, convirtiendo un hueco de diseño en una
cifra con aspecto de umbral vigente. Es el mismo defecto que el propio archivo documenta al borrar
`rtPlausibleCeilingNanos` («config cuyo último consumidor desapareció se queda ahí a riesgo de todos: se lee
como umbral vivo y vuelve a criar un consumidor»). **Eliminada** de la config, del bloque de paridad y del test;
el ADR §10.1 y su tabla de umbrales quedan anotados como DISEÑO, no comportamiento (§4).

Sin `TODO`/`FIXME` en el alcance. Sin código muerto restante tras `P7-F2`/`P7-F3`. Doc↔código reconciliado.

---

## 4. Hallazgos del gate y su disposición

| ID | Qué | Sev | Disposición | Evidencia |
|---|---|---|---|---|
| `P7-F1` → `MB-FOCUS-0069` | Android fabricaba `expectedCadenceHz = 60.0` ante una vista sin display | P1 | **CORREGIDO** — el provider es `() -> Double?`, sin constante de reserva; sin cadencia declarada no se computa NINGÚN conteo de drops | `WindowedTemporalProbeCadenceHonestyTest` (4 tests) + mutación ROJO/VERDE |
| `P7-F2` → `MB-FOCUS-0070` | `windowVerdicts`: serie ordenada en memoria, un escritor, cero lectores, y append antes del check de rechazo | P2 | **ELIMINADO** — el veredicto sigue llegando al reducer, que es la autoridad única | `focus2` 38 clases / 291 tests verdes tras el borrado |
| `P7-F3` = `MB-FOCUS-0054` | `differentialExclusionDelta`: umbral versionado sin consumidores, amplificado por el bloque de paridad de Paso 6 | P2 | **CERRADO por eliminación** (opción b de su propia condición de desbloqueo) | config + bloque de paridad + test + ADR §10.1/tabla |
| `P7-F4` → `MB-FOCUS-0080` | `Verdict.Degraded` sin productor y con KDoc apuntando a una fase cerrada | P3 | **CERRADO POR ELIMINACIÓN (2026-08-28)** — la disposición documental de esta fase se apoyaba en ADR §9, que está por DEBAJO del marcador de retiro; retirada esa razón, la variante se BORRA y su ausencia la impone el `when` exhaustivo del oráculo | mutación: reintroducir la variante rompe la compilación en el sitio declarado; restauración byte-idéntica por SHA-256 |
| `P7-F5` → `MB-FOCUS-0071` | La sonda de la lane de Velocidad conserva el fallback de cadencia que focus2 retira | P3 | **REGISTRADO, NO TOCADO** — otra lane, otro contrato, otra decisión | grep + `FrameTimingProbe.android.kt:55-56` |
| `P7-F6` → `MB-FOCUS-0072` | Un guard de esta misma fase era vacuo bajo mutación (`substringBefore("}")` cortaba en una llave interior) | P2 | **CORREGIDO en la misma tarea** | mutación ROJO tras la reescritura |
| `MB-FOCUS-0051` | El lector de pantalla anunciaba la respuesta correcta y la congruencia | P1 | **CERRADO** — descripción ciega a la respuesta + test de INVARIANCIA (las 4 combinaciones dirección × congruencia producen texto idéntico), no una lista negra de subcadenas | `FocusTracerA11yLeakTest` (2 tests) |

**No cerrados y por qué:** `MB-FOCUS-0066` (B5 sin medir, exige dispositivo) · `MB-FOCUS-0053` (política de
missingness/MNAR: es un diseño que falta, y borrar su constante huérfana no lo sustituye) · `MB-FOCUS-0048`
(revocación focus2, otro dueño) · `MB-FOCUS-0050` (la cadena de autoridad sigue sin commitear) ·
`MB-FOCUS-0067/0068` (campaña física no ejecutada).

---

## 5. Cierre del plan maestro (contrato de doc 06 §4, campo por campo)

Interpretación declarada de «cierre 14§»: el contrato de plan maestro de `06 §4` tiene 13 campos más la
disposición de la fase; se cierra cada uno contra lo que S5 realmente entregó. Los cuatro campos que el bloque
de `S5_REOPEN_STATUS §5` omitió se cierran igual, porque omitirlos en el plan no los hace desaparecer.

| # | Campo | Cierre |
|---|---|---|
| 1 | `objective` | **PARCIAL.** Base temporal honesta multiplataforma: construida, testeada y no montada. Honesta sí; multiplataforma solo en el papel mientras iOS no compile. |
| 2 | `user_outcome` | **PARCIAL.** «El rendimiento del dispositivo jamás se interpreta como su capacidad» se sostiene por construcción (nada RT-crítico entra a evidencia oficial; la degradación técnica no puede fabricar progreso). «Puede exportar» sigue DIFERIDO con dueño (S5R-AUD-09); borrar sí está entregado. |
| 3 | `scientific_outcome` | **ENTREGADO en shadow.** Elegibilidad por métrica y ventana, fail-closed, determinista y versionada; cero caminos donde la degradación fabrique progreso (probado por el oráculo, no afirmado). No cableada. |
| 4 | `current_repo_evidence` | Cerrado: §2.1 inventaría el diff real; el authority map y el data-flow map están en `S5_REOPEN_STATUS §5-§6`. |
| 5 | `assumptions` | «API 29 floor» se mantiene (`ADR-FOCUS-PLATFORM-MIN`; la banda 26-28 quedó VOID en Paso 6). «focus2 local-only sin sync» se mantiene: sin red, sin permisos, verificado por test. |
| 6 | `unknowns` | De los tres iniciales: la segmentación runtime está RESUELTA (celdas estratificadas); el adapter iOS está ESCRITO pero su compilación es `NOT_EXECUTED`; el overhead real del probe sigue SIN MEDIR (`MB-FOCUS-0066`). Las 6 incógnitas de §8.4 tienen protocolo (Paso 6) y ninguna tiene medición. |
| 7 | `out_of_scope` | Respetado sin excepciones: cero cambios en fórmula de score, journal per-trial, sync/cloud, normas/ranking y claims. |
| 8 | `source_of_truth` | `CURRENT_AUTHORITY_INDEX.md` es el índice vigente; el scanner de autoridad se ejecutó tras cada edición documental de esta fase. |
| 9 | `risks` | §7 los enumera con dueño y condición de desbloqueo. El riesgo dominante no es técnico: es que un lector tome CONDITIONAL por DONE. |
| 10 | `dependencies` | Dispositivos físicos (CEO) · runner macOS (CI) · dueño de `data-sync-dev` para la revocación · decisión de producto sobre `TRAINING_FREE`. |
| 11 | `slices_pasos` | P0-P7 ejecutados en orden. P6 `DESIGNED_NOT_RUN/CONDITIONAL`; P7 (este) `CONDITIONAL`. |
| 12 | `release_impact` | **NINGUNO, verificado.** focus2 no está montado: fuera de su paquete solo aparece en `MindBoostStorageNamespaces` (reserva) y en `AppGraph` (cableado únicamente al borrador de cuenta). Ninguna pantalla lo instancia; `mindboost_focus2` está vacío en runtime. |
| 13 | `documentation_updates` | ADR temporal, `ADR_REGISTER_V5`, `05_TECHNICAL_CURRENT_STATE`, `09_DECISION_LOG`, `S5_REOPEN_STATUS`, este documento y el ledger. `GATE_CATALOG_V5` y `FOCUS_ENGINE_MASTER_PLAN_V5` siguen en DENYLIST (custodia D, sellados). |
| 14 | `rollback` | Trivial y verificado: todo S5 es working-tree sin commitear sobre `8a06b9a`, sin migración, sin flag y sin consumidor de producción. Revertir es descartar archivos; ningún dato de usuario cambia de forma. |

---

## 6. Qué entrega S5 y qué NO

**Entrega:** un instrumento de calidad temporal por ventana que se niega a clasificar cuando no tiene con qué
(sin cadencia declarada, sin muestra, con buffer desbordado o con cadencias en desacuerdo ⇒ `NoObservation`);
estadísticos suficientes estratificados por condición × bucket que nunca son una serie ordenada; una
elegibilidad de RT en shadow, versionada y fail-closed, con `NotCaptured` como tercer estado de primera clase;
y una barrera mecánica: `OFFICIAL_ELIGIBLE` es inconstructible en S5.

**No entrega:** ninguna medición física (Paso 6 `NOT_RUN`); ninguna verificación de que el código iOS compile;
ninguna cota validada de error end-to-end, y por tanto ninguna elegibilidad RT real (`D-R3-03` sigue con sus
tres condiciones sin cumplir); ninguna política de missingness (`MB-FOCUS-0053`); ningún montaje.

---

## 7. Veredicto del gate y condiciones exactas para `T-S5`

**`CONDITIONAL`.** `T-S5` se emite cuando, y solo cuando:

| # | Condición | Dueño | Gate |
|---|---|---|---|
| C1 | `compileKotlinIosSimulatorArm64` + `iosSimulatorArm64Test` verdes sobre ESTE árbol | CI macOS | tarea ejecutada, no `UP-TO-DATE`, con conteo de tests |
| C2 | `M5` del protocolo físico ejecutado ⇒ `MB-FOCUS-0066` cerrado ⇒ `§R3-19` deja de ser `FAIL_RESIDUAL_B5_UNMEASURED` | CEO (dispositivo) + sesión | presupuestos de `S5_PASO6_PHYSICAL_PROTOCOL §4`, medidos con `FrameTimingMetric`/Perfetto |
| C3 | Política de missingness/MNAR decidida (`MB-FOCUS-0053`) | psicómetra + eficacia | política preregistrada, o exclusión declarada con su coste |
| C4 | Gate de salida re-ejecutado por lentes INDEPENDIENTES sobre el diff final | subagentes o auditoría externa | dictámenes conservados (el tracker de R2/R3 se perdió una vez: `MISSING_PRIMARY_EVIDENCE`) |

`C1` es la única ejecutable sin hardware nuevo. **Nada de esto habilita el montaje**, que es S6 y tiene su
propio gate de entrada.

---

## 8. Riesgos residuales

1. **Que CONDITIONAL se lea como DONE.** Mitigación: el estado va en la primera línea de este documento, en el
   §0 del ledger y en `S5_REOPEN_STATUS §12`.
2. **iOS `NOT_EXECUTED`.** El adapter iOS lleva tres fases escrito y nunca compilado; cada fase que pasa añade
   código Native no verificado sobre código Native no verificado.
3. **`MB-FOCUS-0048`** (revocación subject-scoped de focus2): sin resolver antes del montaje, un sujeto revocado
   con filas residuales sigue siendo un hueco.
4. **La independencia del gate.** §1 lo declara: estas 8 lentes no son independientes. `C4` existe por eso.
5. **El árbol sigue sin commitear** (`MB-FOCUS-0050`): toda la cadena de autoridad de S5 vive en working-tree,
   así que su ancla es un manifiesto de bytes, no un commit firmado.
