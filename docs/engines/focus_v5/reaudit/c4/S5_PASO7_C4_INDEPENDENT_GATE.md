# S5 · Paso 7 · C4 — gate de salida re-ejecutado por lentes INDEPENDIENTES

<!-- FOCUS_V5_AUTHORITY_BLOCK_START -->
FOCUS_V5_CURRENT_AUTHORITY_INDEX: docs/engines/focus_v5/CURRENT_AUTHORITY_INDEX.md
<!-- FOCUS_V5_AUTHORITY_BLOCK_END -->

**ESTADO: `C4_EJECUTADO` — y NO descarga `T-S5`. Aporta 2 hallazgos `P1` CONFIRMADOS que el cierre canónico
no tenía, uno de ellos en un fichero que ese cierre calificó `APTO`.**

**R3 sigue CANDIDATA / UNMOUNTED. C4 no la promueve, no la congela y no la monta.**

---

## 0. Qué es esto y qué NO es

`S5_PASO7_CLOSURE.md` (el cierre canónico, 2026-08-18) emitió `CONDITIONAL` y declaró honestamente en su §1
que **sus 8 lentes NO eran independientes** — las ejecutó la propia sesión principal, sin autorización para
lanzar subagentes — y por eso creó la condición `C4`:

> C4 · Gate de salida re-ejecutado por lentes INDEPENDIENTES sobre el diff final · subagentes o auditoría
> externa · dictámenes conservados (el tracker de R2/R3 se perdió una vez: `MISSING_PRIMARY_EVIDENCE`).

Este documento es la ejecución de esa condición.

**Clase de revisión declarada ANTES de los resultados:** `AI_INDEPENDENT_SUBAGENT_LENSES`. Independiente en un
sentido preciso y limitado: cada lente corrió en un **contexto fresco**, con su propia carga, y con la
**prohibición explícita de leer `S5_PASO7_CLOSURE.md`** — de modo que ninguna pudo ser anclada por los
veredictos previos. **NO es** una auditoría humana ni externa; `EXTERNAL_HUMAN_AUDIT` sigue `NOT_OBTAINED`.
C4 pedía «subagentes **o** auditoría externa»: se ejecutó la primera rama, no la segunda.

---

## 1. Método y target pinneado

| Campo | Valor |
|---|---|
| Target | 39 ficheros `focus2` + `core/timing` (working-tree; la cadena S5 no está commiteada, `MB-FOCUS-0050`) |
| Manifiesto | `C4_REVIEW_TARGET_MANIFEST.txt` · sha256 `5258d91ed4f7751205ae9359f6cd6d3bdadd1183575698f80e607ba44d080028` |
| Integridad | **39/39 re-verificados byte a byte al terminar: 0 drift** (el árbol mutó durante la revisión, pero en OTRO workstream — Decisiones/calibración) |
| Lentes | 8, en paralelo, contexto fresco, ciegas al cierre canónico |
| Refutación | Cada hallazgo candidato se entregó a un refutador adversarial con instrucción de **refutar por defecto** |
| Dictámenes crudos | `C4_LENS_DICTAMENES.json` (derivado de los bytes del journal, no escrito a mano) |
| Run | `wf_fc49b1a8-e17` |

**Prohibiciones impuestas a toda lente:** solo lectura; **Gradle PROHIBIDO** (aprendizaje 2026-07-19 — dos
invocaciones concurrentes corrompen el build; los gates los corrió la sesión principal, nunca las lentes);
sin mutaciones git.

### 1.1 Techo declarado (sin truncamiento silencioso)

56 hallazgos crudos → 55 tras deduplicar → **35 de severidad P0–P2**. De esos 35, **solo 6 pasaron por
refutación adversarial**; los **29 restantes NO fueron refutation-tested** y quedan como `PLAUSIBLE`, no
como confirmados. Se declara aquí porque un tope no declarado se lee como cobertura completa.

---

## 2. Veredictos por lente

| Lente | Veredicto | Hallazgos |
|---|---|---|
| L1 · eficacia | `FAIL` | 7 |
| L2 · psicometría | `FAIL` | 5 |
| L3 · evidencia | `APTO_CON_OBSERVACIONES` | 8 |
| L4 · arquitectura KMP | `APTO_CON_OBSERVACIONES` | 7 |
| L5 · rendimiento / efecto observador | `APTO_CON_OBSERVACIONES` | 6 |
| L6 · privacidad y seguridad | `APTO_CON_OBSERVACIONES` | 6 |
| L7 · ingeniería de tests | `APTO_CON_OBSERVACIONES` | 10 |
| L8 · diff integrado | `APTO_CON_OBSERVACIONES` | 7 |

**2 lentes en FAIL.** El cierre canónico calificó `APTO` las dos cargas equivalentes (su L1 tras corrección, y
su L2 sin reservas). Esa discrepancia es el resultado que C4 existe para producir.

---

## 3. Hallazgos CONFIRMADOS (sobrevivieron refutación adversarial)

### C4-01 · `P1` — una sesión sin NADA clasificable se gradúa `Eligible`, y iOS vive entera en ese camino

**Convergencia: 5 de 8 lentes lo reportaron de forma independiente** (L1, L2, L3, L4, L8), y **3 refutadores
independientes fallaron al refutarlo**, los tres con confianza `high`; uno concluyó que *«el efecto compuesto
es peor de lo que el hallazgo afirma»*.

`FocusTemporalEligibility.evaluate` cuenta una ventana `NoObservation` como **clasificada, cubierta y no
excluida**. Consecuencia aritmética: una sesión cuyas ventanas son *todas* `NoObservation` produce
`windowsClassified > 0`, `coverageComplete = true`, `excludedFraction = 0.0` ⇒ `Verdict.Eligible`.

Esto es la inversión exacta que el charter prohíbe —**la ausencia fabricando perfección**— y es la MISMA
familia que `MB-FOCUS-0064`/`0065`, que Paso 5 corrigió en la capa de VENTANA. El arreglo de la capa de
ventana (declarar honestamente `NoObservation` en vez de fabricar un veredicto) **creó** el hueco en la capa
de SESIÓN, que sigue leyendo «no excluida» como «buena». Un arreglo honesto en la capa N que la capa N+1
interpreta como afirmación es el patrón del aprendizaje 2026-08-18 («un default que tapa una ausencia la
convierte en un hecho, y el guard de aguas abajo ya no puede verlo»), una capa más arriba.

**Por qué iOS importa:** el actual iOS declara `expectedCadenceHz = null` (honesto, correcto, y es
precisamente lo que `MB-FOCUS-0065` exigió). Sin cadencia esperada, `TemporalClassPolicy` no puede clasificar
⇒ toda ventana iOS es `NoObservation` ⇒ **toda sesión iOS sería `Eligible` con `excludedFraction = 0.0`**.
La plataforma más honesta produce el veredicto más favorable. Hoy es latente: focus2 está UNMOUNTED.

### C4-02 · `P1` — la sonda hermana que SÍ está montada conserva la cadencia fabricada, y el guard no puede verla

**Convergencia: 4 de 8 lentes** (L1, L4, L5, L8). Refutación: `refuted=false`, confianza `high`.

`FrameTimingProbe.android.kt:55` mantiene `view.display?.refreshRate?.toDouble() ?: DEFAULT_REFRESH_HZ` — el
mismo fallback que Paso 7 retiró de focus2 en `P7-F1`. Dos agravantes que el cierre canónico no registra:

1. **Esta sonda SÍ está cableada a producción** (lane de Velocidad). La que se arregló (focus2) no lo está.
   El arreglo se aplicó al probe inerte y no al vivo.
2. **El guard de honestidad de `P7-F1` está acotado por nombre de fichero**, así que estructuralmente no puede
   ver a su hermana. Es un guard cuyo alcance excluye el único sitio donde el defecto está vivo.

El cierre canónico lo registró como `P7-F5`, severidad **`P3`**, «otra lane, otro contrato, otra decisión».
Las lentes independientes lo sitúan en **`P1`** exactamente por ser el montado. **Discrepancia de severidad
registrada, no resuelta**: cambiarlo altera el comportamiento de otra lane y exige su propia decisión — la
disposición «no tocado» del cierre canónico se respeta.

---

## 4. Hallazgos REFUTADOS (la etapa adversarial hizo su trabajo)

| Afirmación | Resultado |
|---|---|
| «iOS cae a 60 Hz fabricados en `drainWindow`» | **REFUTADO** (`high`). iOS fija `expectedCadenceHz = null` incondicionalmente una línea más abajo; el reclamante citó la línea equivocada. |
| «Android reporta `droppedFrames = 0` sin que nada lo cace» | **REFUTADO** (`high`). El hecho es cierto; la inferencia no: existe un guard aguas abajo que el reclamante no leyó. |

2 falsos positivos eliminados de 6 sometidos a refutación. Sin esta etapa habrían entrado al informe como
defectos reales.

---

## 5. Observaciones convergentes NO refutation-tested (`PLAUSIBLE`, no confirmadas)

- **3 lentes** (L4, L6, L7): `Focus2PlatformPermissionTest` — el guard «focus2 no pide permisos, no lee
  sensores, no abre sockets» **escanea solo `commonMain`**, donde toda API de plataforma es estructuralmente
  inalcanzable. Los 9 ficheros de plataforma, que son el único sitio donde tal API podría aparecer, quedan
  fuera de su alcance. Un guard cuyo dominio excluye el lugar del riesgo.
- **3 lentes** (L2, L3, L8): el mismo `excludedFraction == null` se lee **fail-CLOSED (→1.0)** en
  `FocusTemporalEligibility` y **fail-OPEN (→0.0)** en `FocusEvidenceStatePolicy`, a ~50 líneas de distancia.
- **L6**: focus2 no tiene guard de revocación — una escritura que compita con el borrado de cuenta resucita
  filas de un sujeto borrado. **Corresponde a `MB-FOCUS-0048`, ya OPEN, con otro dueño.** Ver §7.
- **L3**: `HzBand` — cero productores, cero consumidores, y un KDoc que afirma un control de minimización de
  datos que nada ejecuta.

---

## 5bis. Lo que las 8 lentes NO vieron (crítico de completitud)

Aritmética de alcance, declarada por el propio crítico: los hallazgos de las 8 lentes citan **5 ficheros
distintos** de los **39** pinneados. **34 ficheros no produjeron ningún hallazgo**, ni confirmado ni refutado.
El crítico revisó ese hueco y encontró dos defectos que **ninguna de las 8 cargas podía ver**, porque no viven
en la cadena de frames sino en la secuenciación de corrutinas y en el consumidor terminal:

### C4-03 · `P2` — una respuesta puede perderse entre la apertura de la ventana y el armado del gate

`FocusTracerStateHolder.kt:121-124`: `presentNext()` abre la ventana en la línea 123, pero el
`CompletableDeferred` que recibirá la respuesta no se asigna hasta la 124. `onResponse` es
`responseGate?.complete(...)` — **no-op si es null**. Un tap que aterrice en ese hueco se descarta en
silencio; la ventana expira y se registra `Response.NONE` como **omisión limpia**.

Esto fabrica un **fallo** para los respondedores más rápidos: la inversión del charter en la dirección que
INFRAvalora, y sesgada sistemáticamente contra exactamente la población que el suelo de anticipación existe
para juzgar. Invisible a las 8 lentes porque no es un defecto de medición temporal sino de orden de corrutinas.

### C4-04 · `P2` — `evidenceOf` no lee el veredicto tipado; lo re-deriva del `Double?` crudo

`FocusEvidenceStatePolicy.kt:45` decide el estado de evidencia leyendo `temporal?.excludedFraction` en vez de
`rtShadowVerdict`. El veredicto de tres vías que el KDoc de su hermano llama «el tercer veredicto que sostiene
la carga» queda **puenteado en el único sitio donde se decide la evidencia de una sesión**. Consecuencia:
`NotCaptured` (sin probe, legítimo) y «un probe corrió y no clasificó nada» son **indistinguibles** — ambos
aterrizan en `PROVISIONAL`.

Es además el otro extremo de la asimetría de §5: el mismo `null` es `FULLY_EXCLUDED = 1.0` en un fichero y
`NO_BACKSTOP = 0.0` en el otro. Leyendo cualquiera de los dos por separado, la constante parece defendible;
el defecto solo existe en el par.

---

## 6. Qué descarga C4 y qué NO

| | |
|---|---|
| **Descarga** | El gate de salida ha sido re-ejecutado por lentes que no vieron los veredictos previos, sobre un target pinneado por bytes, con refutación adversarial y dictámenes conservados. |
| **NO descarga** | `T-S5`. C4 era una de cuatro condiciones y **abre trabajo nuevo** en vez de cerrarlo: 2 `P1` confirmados pendientes de disposición. |
| **NO descarga** | `C1` (iOS), `C2` (B5 físico), `C3` (política MNAR) — intactas. |
| **NO es** | Auditoría humana/externa. `EXTERNAL_HUMAN_AUDIT = NOT_OBTAINED`. |

---

## 7. Riesgo de seguridad surfaceado y NO corregido, por decisión declarada

`MB-FOCUS-0048` (revocación subject-scoped de focus2, GAP-1/GAP-2) fue reconfirmado independientemente por
L6. **No se toca**, por tres razones, en este orden: (1) tiene otro dueño (`data-sync-dev` +
`security-privacy`) y su gate son 9 interleavings; (2) cablear a medias una autoridad de revocación es
exactamente el fallo del aprendizaje 2026-07-14; (3) `:shared:desktopTest` está BLOQUEADO por WIP ajeno (§8),
así que ningún arreglo podría verificarse — y un arreglo no verificable es peor que un hallazgo documentado.

---

## 8. Estado de los gates en el momento de este C4

| Gate | Resultado |
|---|---|
| `:shared:detekt --rerun-tasks` | **VERDE** · exit 0 · `detekt.xml` 0 `<error>` · baseline `UNCHANGED_FROM_HEAD` |
| `:shared:desktopTest` | **BLOQUEADO — no por este trabajo.** `compileTestKotlinDesktop` falla con 2 errores en `BayesArenaStateHolderTest.kt` (workstream Decisiones), fichero cuyo mtime avanzó DURANTE la compilación ⇒ se está editando en vivo. `compileTestKotlinDesktop` compila TODO `desktopTest` como una unidad, así que un fichero ajeno roto bloquea el suite entero. No tocado (regla §4.10: no arreglar WIP ajeno). |
| `validate-audit-ledger` | VERDE (`189 findings`) |
| `validate-authority-entrypoints` | VERDE |
| `pii-secret-scan` (selftest + artefactos C4) | VERDE · `SCAN CLEAN` exit 0 |
| Invariante UNMOUNTED | **VERIFICADA independientemente**: 0 consumidores de producción de `FocusTemporalEligibility` / `evidenceOf` / `rtShadowAdmissible`; 0 referencias a `features.mindboost.focus2` fuera de su paquete |

---

## 9. Bloque listo para el ledger — NO escrito por esta sesión

El ledger tiene **un solo escritor** y durante esta tarea lo ha tenido OTRA sesión (173 → 184 → 189 findings
mientras yo trabajaba, con resellos correctos). Escribirlo en paralelo es `MB-LEDGER-0001`, la colisión que
el protocolo existe para impedir. **Decisión: no escribo el ledger.** El escritor único debe anexar:

```
C4-01 → MB-FOCUS-#### · P1 · OPEN
  FocusTemporalEligibility.evaluate cuenta NoObservation como ventana clasificada/cubierta/no-excluida ⇒
  una sesión sin NADA clasificable devuelve Verdict.Eligible con excludedFraction 0.0. iOS entero vive en
  ese camino (expectedCadenceHz = null por diseño honesto). Absencia fabricando perfección en la capa de
  SESIÓN; misma familia que MB-FOCUS-0064/0065, una capa más arriba. Latente (focus2 UNMOUNTED).
  Evidencia: C4_LENS_DICTAMENES.json (5 lentes independientes, 3 refutadores fallidos, confianza high).

C4-02 → MB-FOCUS-0071 (RE-SEVERIZAR P3 → P1, no un ID nuevo)
  FrameTimingProbe.android.kt:55 conserva el fallback DEFAULT_REFRESH_HZ y ESTÁ cableada a producción
  (Velocidad), mientras el guard de P7-F1 está acotado por nombre de fichero y no puede verla.
  Disposición «no tocado» del cierre canónico respetada; lo que cambia es la severidad.

C4-03 → MB-FOCUS-#### · P2 · OPEN
  FocusTracerStateHolder.kt:121-124 — una respuesta que aterriza entre presentNext() (ventana abierta) y la
  asignación del responseGate se descarta en silencio y se registra como omisión limpia (Response.NONE).
  Fabrica un FALLO para los respondedores más rápidos: inversión del charter en dirección INFRA, sesgada
  contra la población que el suelo de anticipación juzga. Latente (focus2 UNMOUNTED).

C4-04 → MB-FOCUS-#### · P2 · OPEN
  FocusEvidenceStatePolicy.kt:45 — evidenceOf decide leyendo excludedFraction crudo, NO rtShadowVerdict, así
  que NotCaptured y «clasificó nada» son indistinguibles. Par asimétrico con FocusTemporalEligibility.kt:96:
  el mismo null es 1.0 (fail-closed) allí y 0.0 (fail-open) aquí. El defecto solo es visible en el PAR.
```

**Vacuidad detectada en el oráculo de Paso 4 (registrar como observación, no como ID nuevo):** L1, L2 y L3
coinciden en que el dominio de entrada del oráculo adversarial es `{Clean, Excluded, null}` — **nunca
alimenta `NoObservation` ni `DegradedUsable` a `evaluate()`**. Su afirmación «codominio alcanzable
ejercitado» pinnea las SALIDAS omitiendo la mitad de las ENTRADAS, que es exactamente por qué `C4-01`
sobrevivió a una suite verde. Es el patrón «mitad discriminante» del propio charter, aplicado al revés.

---

## 10. Declaración

No se avanzó a la fase siguiente. No se montó ni cableó nada. Sin commit/push/merge/tag. Sin cambio de código
de producción: **cero ficheros `.kt` modificados por esta tarea** (manifiesto re-verificado 39/39 sin drift).
No se tocó WIP ajeno. No se escribió el ledger (§9). **R3 sigue `CANDIDATA / UNMOUNTED`.**
