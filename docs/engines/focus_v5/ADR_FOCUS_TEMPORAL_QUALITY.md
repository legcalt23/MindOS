# ADR-FOCUS-TEMPORAL-QUALITY — Calidad temporal por ventana y elegibilidad por métrica (S5)

```
ESTADO = REOPENED_AFTER_EXTERNAL_AUDIT (2026-07-21) — la R2 queda como HISTORIA (SUPERSEDED, en curso, por la
R3 candidata). Una auditoría externa reprodujo con el scorer REAL (`DescriptiveScorer` + `Probit`) el
contraejemplo Absent→Present que FALSA el gate de medición «S_full-o-Absent» (§6.1) y el `monotonicGuard mg-1`
(§10.3c): base 30H/10M/10FA/30CR ⇒ S_full = 1.31085; excluir 5 misses ⇒ S_adm = 1.68002 > S_full ⇒ Absent; excluir
ADEMÁS 15 hits (MÁS exclusión técnica) ⇒ S_adm = 1.29291 ≤ S_full ⇒ Present. Más degradación técnica ABRE el gate
que menos degradación cerraba; y `CI-lower(S_full)>0` hace la elegibilidad dependiente del rendimiento (sesgo de
selección). ⇒ el gate de medición se ELIMINA como criterio de elegibilidad temporal; la elegibilidad temporal
pasa a ser OUTCOME-INDEPENDENT (D-R3-01) y S5 NO certifica accuracy ni RT (D-R3-02). Corrección en la R3
candidata (hallazgos R3-AUD-01..12 + decisiones cerradas D-R3-01..04). Ninguna implementación dependiente avanza
sobre la R2. Supersede la R1 (FROZEN_FOR_S5 → REOPENED_AFTER_AUDIT, 12 hallazgos S5R-AUD-01..12 §20). R1 y R2 =
SUPERSEDED, conservadas como historia. R3 = CANDIDATA (escrita abajo, §R3-0..R3-22), NO congelada: el gate de
ENTRADA §8 (6 lentes read-only) está COMPLETO (2026-07-21, tras el reset del límite de sesión) y la falsación
quedó CONFIRMADA por DOS lentes independientes (psicómetra base 30/10/10/30; assessment base 60/20/20/60).
PENDIENTE para `FROZEN_FOR_S5_R3`: el gate de SALIDA (7 lentes sobre esta R3) + los criterios DoD §12.
HALLAZGO TRANSVERSAL del gate: la R2 marcó como «hecho/CABLEADO/persistido» múltiples correcciones que en el
código SHIPPED son `DESIGNED_NOT_RUN`/`PENDING_IMPLEMENTATION` (marcador que aún gatea, revocación no cableada,
enum fantasma, versiones no persistidas, `seal()` con params opcionales); la separación honesta
DECISION/IMPLEMENTATION/VALIDATION vive en §R3-15.
UMBRALES = config versionada EN SOMBRA (DESIGNED_NOT_RUN): el contrato fija nombres, roles y valores
iniciales; ningún valor es ciencia confirmada hasta validación física (Paso 6) + revisión empírica.
AUTORIDAD = contrato de reapertura «S5 · Paso 3» (2026-07-21) + gate de entrada de 7 lentes
(§20: eficacia · psicómetra · kmp-architect · privacy-security · mobile-performance ·
assessment-psychometrics · security-privacy) + decisiones cerradas (S5_REOPEN_STATUS §4).
REGISTRO = provisional en 09 §20.5 + ADR_REGISTER_V5 (estado PROVISIONAL→R2); ratificación documental
final en Paso 7.
MARCO PADRE = ADR-FOCUS-TIMING (estructura APPROVED: clases T derivadas de la propia sesión, jamás del
modelo de dispositivo; «nada RT-crítico entra a evidencia oficial en v1 de v2»).
```

═══════════════════════════════════════════════════════════════════════════════════════════════════════════
# R3 CANDIDATA (2026-07-21) — reapertura tras auditoría externa · gate de entrada §8 (6 lentes) COMPLETO
═══════════════════════════════════════════════════════════════════════════════════════════════════════════

> Esta R3 SUPERSEDE la R1 y la R2 (conservadas abajo como historia). Estado: **CANDIDATA**, no congelada
> (falta el gate de salida de 7 lentes + DoD §12). Autoridad: contrato «REAPERTURA OBLIGATORIA R2→R3»
> (2026-07-21) + gate de entrada de 6 lentes read-only (psicómetra, assessment-psychometrics,
> mobile-performance, kmp-architect, security-privacy, privacy-security). Decisiones cerradas por la
> auditoría: **D-R3-01..04** (§R3-5, §R3-11, §R3-10, §R3-13). Una falsación reproducible domina cualquier APTO.

> **Convención de citas por sección (medida sobre el propio documento, no nueva).** Dentro de este bloque
> vigente un `§N` desnudo tiene **DOS destinos posibles**, y la cita por sí sola no los distingue:
>
> 1. el **contrato de reapertura** «REAPERTURA OBLIGATORIA» — cuya plantilla vive en
>    `docs/context_pack/07_PHASE_REVIEW_PROTOCOL.md`, en un bloque cercado con numeración PROPIA §1..§13 —, que
>    es lo que citan los títulos de `§R3-21` (§8 = «Agent routing obligatorio»), `§R3-20` (§10 = «Tests y gates
>    obligatorios», con exactamente 5 subsecciones = las «5 familias de tests») y `§R3-22` (§12 = «Definition of
>    Done específica»);
> 2. las **secciones R2 retiradas de este mismo ADR**: así se usan `§6.1`, `§10.2` y `§10.3c` en este bloque
>    vigente — unas veces con prefijo explícito («R2 §6.1») y otras sin él.
>
> Consecuencia concreta: el `§10` del título de `§R3-20` y el `§10.2` de unas líneas más abajo apuntan a
> **documentos distintos**. Reglas que el documento sí sigue de forma estable: las secciones vivas se citan
> **`§R3-N`**, y los demás documentos van **con prefijo** (`09 §20.5`, `S5_REOPEN_STATUS §4`). Por DEBAJO del
> marcador de retiro, un `§N` desnudo sí es una sección R2 de este ADR — que es exactamente lo que afirma el
> token `[R2-HIST]` de las citas en `shared/src`. `§R3-17` ya desambiguaba un caso a mano; esta nota declara la
> convención en vez de repetirla caso a caso. **No cambia ninguna cláusula, no congela nada; R3 sigue CANDIDATA.**

## R3-0. Reencuadre nuclear: la R2 no solo era falsa, se declaraba implementada

La R2 tenía DOS clases de defecto, y la segunda es la más peligrosa:

1. **Defecto matemático (FALSADO, reproducible).** El gate de medición «`S_full`-o-`Absent`» (R2 §6.1) y su
   `monotonicGuard mg-1` (R2 §10.3c) afirman «ninguna `Absent → Present` bajo peor calidad». DOS lentes
   independientes reprodujeron el contraejemplo con el scorer REAL (`DescriptiveScorer` + `Probit`): **más
   exclusión técnica ABRE el gate que menos exclusión cerraba** (§R3-2). `mg-1` (a) y (c) son FALSOS; solo (b)
   (no-inflación del valor emitido) sobrevive.
2. **Defecto de honestidad de estado (el central de R3).** La R2 describe como YA APLICADAS correcciones que
   en el código shipped son `DESIGNED_NOT_RUN` o `PENDING_IMPLEMENTATION`: el marcador de render **aún gatea**
   la clasificación (§R3-9/L1), la revocación **no está cableada** (§R3-12/GAP-1), `ONSET_INTEGRITY`/
   `PRE_ONSET_RESPONSE` **siguen en el enum** de producción, `thresholdsVersion`/`temporalQualityModelVersion`
   **no se persisten**, `seal()` **conserva los 5 params temporales opcionales** (la forma que la propia R2
   llama «el problema»), y el `validate()` fail-closed de tallies **no existe**. La R2 usó pasado y «CABLEADO»
   para trabajo pendiente. R3 lo corrige con la separación de tres campos DECISION/IMPLEMENTATION/VALIDATION
   (§R3-15).

Consecuencia: la elegibilidad temporal se rehace **outcome-independent** (D-R3-01) y S5 **no certifica accuracy
ni RT** (D-R3-02); todo lo que S5 produce honestamente es la lane **descriptiva** en sombra (techo
`TRAINING_ONLY`/`PROVISIONAL`, jamás perfil/rank). El montaje es la slice **S6**, que NO desbloquea medición.

## R3-1. Definiciones formales (estado, valor, ausencia, orden parcial) — evidencia §9.1

- **Hechos técnicos pre-outcome** `τ(s)` de una sesión `s`: capability observada, dominio de reloj, continuidad
  de reloj, cadencia esperada/observada, cobertura/report-loss, integridad de ventana por bucket, versión de
  umbrales/modelo, blueprint/modalidad. NINGUNO deriva de `hits/misses/FA/CR`, accuracy, d′, criterio o CI.
- **Elegibilidad temporal** `E(s) ∈ {Eligible, NotAuthoritative(cause:Set), NotCaptured}`. `E` factoriza
  EXCLUSIVAMENTE como `E = g(τ(s))`. No hay estado `Present`/`Absent` con valor: la elegibilidad NO emite un
  número (esa confusión era la raíz de mg-1).
- **Valor descriptivo** `S_full(s) = d′` (por condición) sobre TODAS las ventanas con su outcome observado,
  siempre acompañado de `excludedFraction`. `S_full` NUNCA es certificado como medición en S5; es descripción.
- **Ausencia** = `Descriptive.Absent(reason)` tipada (nunca `0.0`; charter «null≠0»), producida solo por el
  scorer ante muestra insuficiente / sin señal / no-aplicable — es un hecho del DATO, no del gate temporal.
- **Orden parcial de degradación** `D`: `s ⊑ s'` si `s'` tiene ≥ exclusión técnica que `s` con outcomes fijos.
  **Invariante R3 (reemplaza mg-1):** `E` es MONÓTONA-DÉBIL en `D` por construcción, porque `E=g(τ)` y `τ`
  bajo `D` solo puede empeorar coverage/subir excludedFraction → `E` solo puede moverse hacia
  `NotAuthoritative`/`NotCaptured`, jamás hacia `Eligible`. Dos lemas SEPARADOS (precisión psicómetra NB-2):
  **(Lema 1, TIPO → outcome-independence)** como `E=g(τ)` no recibe outcomes, es invariante a cualquier
  permutación de outcomes — esto es lo que mató a `mg-1`. **(Lema 2, umbrales one-sided → monotonía)** los adders
  de causa de `FocusTemporalEligibility.evaluate:84-90` son ISÓTONOS (`sessionClass≠T1`, `!coverageComplete`,
  `excludedFraction>max`) → más degradación solo AÑADE causas. La monotonía NO se sigue del tipo (el tipo da la
  outcome-independence); se sigue de que `g` es isótona en `excludedFraction`/coverage, guarded por el test M1.

## R3-2. El contraejemplo R2 íntegro (dos bases independientes) — evidencia §9.2

Fórmula real: `d′ = z((hits+0.5)/(Ns+1)) − z((FA+0.5)/(Nn+1))` (Hautus loglinear, `DescriptiveScorer.kt:158-159`);
`SE` con conteos crudos `Ns,Nn` (`:103-109`); `CI_lower = d′ − 1.959963984540054·SE` (`:120,220`).

**Base A (psicómetra, 30H/10M/10FA/30CR):**

| Muestra | Celda | d′ | SE | CI_lower |
|---|---|---|---|---|
| S_full | 30/10/10/30 | 1.31085 | 0.30326 | 0.71646 |
| D1 (−5 misses) | 30/5/10/30 | 1.68002 | 0.33522 | 1.02300 |
| D2 (−5M −15H) | 15/5/10/30 | 1.29291 | 0.37036 | 0.56702 |

Gate R2 (Present sii admisibles≥20 ∧ `CI_lower(S_full)>0` ∧ `S_adm ≤ S_full` ∧ sin diff/trend/backstop;
VALUE=S_full): **D1 → Absent** (1.680 > 1.311); **D2 → Present** (1.293 ≤ 1.311; admisibles 20/40; exclusión
20/80=25%<50%). Más exclusión (D2) elegible; menos (D1) rechazada.

**Base B (assessment, 60H/20M/20FA/60CR):** S_full=1.32962 (CI_lower=0.90836; literal corregido NB-1
psicómetra: 1.3294→1.32962). A (excluir 10 misses, 60/10/20/60): S_adm=1.7102 → **Absent**. B′ (excluir 10M+35H,
25/10/20/60): S_adm=1.2133 ≤ 1.32962 → **Present** (emite S_full). Márgenes 0.38/0.12 ≫ ruido del probit ~0.001.
Convergen: `mg-1`(a)+(c) **FALSOS**.

## R3-3. Por qué `S_adm` NO es monótono bajo eliminación — evidencia §9.3

`S_adm` es función de la COMPOSICIÓN SDT de las ventanas excluidas, que el proceso de exclusión técnica NO
controla: excluir misses/FA sube el hit/fa-rate y sube `S_adm`; excluir hits/CR lo baja. Por tanto `S_adm` —y
el predicado `S_adm ≤ S_full`— es **no monótono en el grado de exclusión**. `mg-1` asumía «admisible solo
encoge ⇒ veredicto solo va a Absent», pero encoger el conjunto admisible quitando ventanas FAVORABLES BAJA
`S_adm` y ABRE el gate. Falla adicional de la justificación R2 (:232-235): «emitir S_full solo cuando
S_adm≤S_full ⇒ las excluidas eran net-favorables ⇒ no infla» es falaz — S_full CUENTA esas ventanas
truncation-risk; el gate certifica el número que INCLUYE los outcomes marcados no fiables.

## R3-4. `D_label` vs `D_real` — la D-invariancia es tautología, no robustez — evidencia §9.4

- `D_label`: reetiqueta la clase técnica de ≥1 ventana (CLEAN→DEGRADED→EXCLUDED) **sin tocar ningún outcome**.
  `S_full` no depende de la etiqueta ⇒ `S_full ∘ D_label = S_full`. Invariancia CIERTA pero **trivial**: probar
  que una función de los outcomes no cambia cuando no cambias los outcomes es tautología.
- `D_real`: el render tardío / onset sesgado cambia la EXPOSICIÓN perceptiva → puede cambiar el outcome mismo,
  `Y(D_real) ≠ Y(0)` (un HIT con ventana completa se vuelve MISS/OMISSION con render tardío). `S_full` sobre los
  `Y(D_real)` OBSERVADOS hereda ese sesgo; no hay invariancia frente a `D_real`.
- **VALIDATION_STATUS = UNKNOWN:** afirmar que la accuracy medida bajo render tardío = la que el sujeto habría
  producido con presentación correcta exige `Y(D_real)=Y(0)`, precisamente lo que S5 no puede establecer (sin
  ancla de commit ni fotodiodo; §R3-9). La validez causal de la accuracy bajo render tardío es UNKNOWN hasta el
  Paso 6. La «D-invariancia» es bookkeeping de etiquetas, no causalidad.

## R3-5. Propiedad de outcome-independence (D-R3-01) — evidencia §9.5

**D-R3-01 (CERRADA):** la elegibilidad temporal es función SOLO de `τ(s)`. Prohibido leer
`hits/misses/FA/CR/accuracy/d′/criterion/CI_lower` o «si excluir ayuda/perjudica al score» en la ruta de
elegibilidad. La validez conductual/anti-automatización es un EJE SEPARADO (§R3-11), no disfrazada de calidad
temporal.

**Enunciado formal (property test a congelar):** sea `π` cualquier permutación de outcomes que preserve, por
celda, `totalTrials` y todos los invariantes técnicos (`resolvedTrials`, `windowsClassified`, el bucket de cada
ventana, `excludedByReason`, `sessionClass`, continuidad de reloj, cadencia esperada/observada, versiones,
blueprint). Entonces `E(s) = E(π·s)` para toda `s`.

**Conformidad shipped (PARCIAL — precisión efficacy C2 + psicómetra NB-5):** `FocusTemporalEligibility.evaluate`
(`:47-98`) satisface la propiedad HOY — su `buildSet` de causas (`:84-90`) deriva solo de `sessionClass`,
`coverageComplete` (`windowsClassified==resolvedTrials`, `:83`) y `excludedFraction` (`:70-82`); ningún hit/FA/d′/
CI entra; `totalTrials` (`:69`) cuenta resueltos sin discriminar acierto/error; y verificado (psicómetra) que `E`
**NO tiene piso propio** (el `n≥20` vive en el SCORER, no en `E`), así que NO hay reentrada outcome-dependent hoy.
R3 NO reescribe el gate: lo **congela**. Estado real = **conforme-pero-sin-sellar**, NO «probado»: PENDIENTE
(F-R3-G) congelar la propiedad con el property test + la mutación M-OI. DOS guardas FORWARD (assessment), no
defectos actuales: (1) si algún día se añade un piso a `E`, ponerlo sobre conteos DELIVERED/técnicos —nunca
ADMISIBLES— o la outcome-dependence reentra por el piso; (2) la invalidación per-session `CI_lower` del SCORER es
eje conductual (§R3-11), con regla explícita de NO-agregación ingenua sobre supervivientes.

## R3-6. Álgebra de trial y de tallies + ejemplo reconstruible (R3-AUD-04/05) — evidencia §9.6

**Partición total de trial (diseño; IMPLEMENTATION_STATUS = PENDING):**

```
sealed interface TrialDisposition {                 // cardinalidad 1:1 con "presentado"
  data class Resolved(outcome: TrialOutcome, windowIntegrity: TemporalWindowVerdict)  // exactamente 1 SdtCell
  data class AbortedAfterPresentation(reason: AbortReason)                            // cero SdtCell
}
enum class AbortReason { PAUSED_OR_INTERRUPTED_WHILE_OPEN, CROSS_EPOCH_DISCONTINUITY }  // solo las never-resolved
// TemporalWindowVerdict.Excluded(reasons: Set<WindowIntegrityExclusionReason>)  // TRUNCATION_RISK/BUFFER_OVERFLOW SOLO aquí
sealed interface SessionInvalidationReason { ClockDiscontinuity; Behavioral(reason: InvalidationReason) }
```

Reglas: cada presentado tiene exactamente UNA disposición terminal; cada `Resolved` exactamente UN outcome; un
`Aborted` NUNCA fabrica `SdtCell`; los síntomas de ventana múltiples se modelan como `Set` (no precedencia por
orden de `when`, espejo de `FocusTemporalEligibility.Verdict.Excluded(causes:Set)`). Mapeo: pausa-antes-de-
presentar → ninguna disposición (nada abierto); pausa/interrupt/**process-death** con ventana abierta →
`AbortedAfterPresentation(PAUSED_OR_INTERRUPTED_WHILE_OPEN)` (familia unificada); background → el holder lo mapea
a `setPaused`; **timeout → `Resolved`** (produce OMISSION/CR, no abort); cross-epoch →
`AbortedAfterPresentation(CROSS_EPOCH_DISCONTINUITY)`. **Precedencia #1 (reloj):** cardinalidad EXISTENCIAL (≥1
cross-epoch ⇒ sesión `SessionInvalidationReason.ClockDiscontinuity`, un salto imposible es discreto y
falsable). GAP hallado: `SessionInvalidationReason` no existe hoy; un cross-epoch solo sube `contaminatedCount`,
nunca invalida la SESIÓN — la precedencia #1 del ADR §8 no tiene productor.

**Álgebra de tallies (congelada contra el DTO real):**
`deliveredTrials = resolvedTrials + abortedAfterPresentation`; `resolvedTrials = Σ SdtCell.totalTrials`
(`FocusRuntimeModel.kt:142` ✓); `Σ bucketTallies(cond) ≡ officialCell(cond).totalTrials` (✓ por construcción).
`deliveredTrials` debe MOVERSE al snapshot (hoy solo se deriva en el holder `:103-104`). **Defecto:** la
clasificación de `resolved-excluded` (`temporalTallying :151-163`) y la de `aborted` (`temporalDiscardCounting
:166-179`) escriben en la MISMA fila `EXCLUDED` y el MISMO `excludedByReason:Map<String,Int>` — el `cell` sí
está aislado, pero el mapa de razones colapsa dos poblaciones incompatibles bajo la misma clave. R3 exige
almacenarlas en estructuras FÍSICAMENTE distintas (consecuencia del fix de partición).

**Ejemplo reconstruible (4 trials, CONGRUENT):** t0 HIT/Clean → `cell.hits=1`; t1 MISS/Excluded(TRUNCATION_RISK)
→ `cell.misses=1`, fila EXCLUDED `{TRUNCATION_RISK:1}`; t2 ventana abierta + PAUSA → `cell` sin cambio,
`contaminatedCount=1`; t3 FA/Clean → `cell.falseAlarms=1`. Reconstrucción: `resolved=Σcell=3`;
`delivered=3+1=4` (=4 presentados) ✓; `Σbuckets(CLEAN 2 + EXCLUDED 1)=3=officialCell` ✓. Pero `excludedByReason`
de la fila EXCLUDED tras t2 = `{TRUNCATION_RISK:1, PAUSE_DISCARD:1}` es indistinguible sin conocimiento externo
de qué clave pertenece a `cell` — el defecto a corregir.

**`validate()` fail-closed (NO EXISTE hoy; grep 0):** debe correr en restore Y complete y verificar
`Σbuckets(cond)==officialCell(cond)`, `bucketName ∈ {conjunto reconocido}` (hoy `TemporalCellTally.bucket:82`
devuelve `null` en silencio ante nombre desconocido), `Σ(razones never-resolved)==contaminatedCount`, aritmética
chequeada (overflow), y falla cerrado ante corrupto/futuro.

## R3-7. Modelo de doble drenaje (R3-AUD-06) — evidencia §9.7

Dos streams disjuntos: **drain#1 PRESENTAR** (intervalo previo/ISI, salud de sesión — hoy `presentNext()`
`:177-181` lo drena y DESCARTA, KDoc «a session trend consumer lands with Paso 4»; sin consumidor); **drain#2
RESOLVER** (ventana perceptivo-respuesta — hoy `submit()` `:203-212`, único que alimenta `windowIntegrity` y
`temporalSampledFrames`). Los dos están funcionalmente separados PERO **no tipados** (mismo
`drainWindow():TemporalWindowObservation?` en ambos call-sites) → una edición futura puede conectar drain#1 a la
clasificación por accidente. R3: **tipar** la distinción (`IsiObservation` vs `ResponseWindowObservation`, o dos
métodos con tipos de retorno distintos) para que sea invariante, no disciplina. Una única ecuación
`Σ tertileTallies == resolvedTrials` NO puede representar ambos drenajes: los tertiles cuentan resolved; el
drain#1 no produce tertiles. Honestidad: llamar «wired» al drain#1 es falso hoy (hook vacío).

## R3-8. `TertileBasis` (R3-AUD-06) — evidencia §9.8

`sealed interface TertileBasis`:
- **`FixedCountOrdinal`** (base, ADR §5.1): `tertileOf(trialIndex, plannedTrials) = clamp(trialIndex*3/plannedTrials, LAST)`.
  Streaming-safe (`plannedTrials` ya es un hecho del input; cero reloj/estado nuevo). Solo el bin agregado se
  persiste; ningún timestamp/serie cruda. **Bordes:** N=0 → `require(plannedTrials>0)`; N=1 → siempre FIRST
  (degenerado; `MIN_TERTILE_N` rechaza la comparación); N=2 → **LAST vacío** (trampa 0/0 salvo que el consumidor
  de tendencia trate un tercio vacío como `NotCaptured`); N no divisible por 3 → reparto determinista desigual
  (10→4/3/3); fin anticipado → LAST sub-representado (mismo guard); extensión → **imposible** (`present()` rechaza
  más allá de `plannedTrials`, `FocusSessionReducer.kt:119`); timeout → mismo `trialIndex` que un Respond.
- **`TimeBoxedActiveFraction`** (diferida): fracción de tiempo activo monotónico, pausa NO consume tiempo activo.
  Exige un acumulador `activeElapsedNanos` que NO existe; su denominador solo se conoce al terminar → incompatible
  con «counts, never trials» salvo anclándolo a una duración PLANEADA (análoga a `plannedTrials`). Solo se adopta
  si el tracer B (vigilancia, foreperiod jittered) demuestra que el reparto por-cuenta distorsiona el por-tiempo.

## R3-9. Capability / cadencia / error por plataforma + taxonomía de proxy (R3-AUD-07, D-R3-03) — evidencia §9.9/§9.10

**Semántica oficial (ninguna prueba fotón/commit/present):** `Choreographer.frameTimeNanos` = inicio de vsync
PROGRAMADO (subestima el retardo → sesga a CLEAN); FrameMetrics `INTENDED_VSYNC + TOTAL_DURATION` termina en
buffer-swap (enqueue), **proxy de swap-completion, NO present/photon**; `dropCountSinceLastInvocation` =
report-loss del listener (no frames de display caídos; el objeto se muta → `new FrameMetrics(src)` por frame);
`MotionEvent.getEventTimeNanos` (API 34) = resolución ns ≠ EXACTITUD (error dominante 5–33ms de sampling/callback,
no la cuantización ≤1ms); `CADisplayLink` = cadencia del sistema, no composite de Compose.

**D-R3-03 (CERRADA):** prohibido «commit real»/«onset real»/«desbloquea rt−D por API»/«exactitud por
NANOS_NATIVE». Nombre neutro `FRAME_ISSUED_PROXY` (o `firstVsyncAfterStateSetNanos`). RENOMBRES pendientes (8
sitios código + 4 ADR): `presentToFirstFrameCommitNanos` → `firstVsyncAtOrAfterStateSetNanos`
(`TemporalWindowObservation.kt:19`, `TemporalClassPolicy.kt:47-59`, `WindowedTemporalProbe*.kt`); borrar
`ONSET_INTEGRITY`/`PRE_ONSET_RESPONSE` de `TemporalWindowVerdict.kt:18-22` (renacen en S6); «real frame
stats/drops» → «observed frame deltas» (`TemporalVocabulary.kt:6`, `FrameTimingProbe.kt:29`); ADR §3/§6/§18/§19
overclaims de «ancla de commit real». La futura elegibilidad RT se basará en `validatedEndToEndErrorBound` +
asociación estímulo-frame + validación física, **no** en la unidad/nivel de API del timestamp.

**Modelo de cadencia de 6 hechos ortogonales:** `panelCapability`(banded), `expectedCadence(targetIntervalNanos?)`,
`observedCadence(medianDeltaNanos, solo descripción)`, `rateChange`, `reportLoss(unobservedFrames)`,
`coverage`. **Regla load-bearing:** `expectedCadence` es el ÚNICO denominador de drop defendible y **S5 NO puede
sourcearlo** (necesita `getFrameData().preferredFrameRateNanos`/`targetTimestamp`/`DEADLINE` — todo S6) ⇒ si
`targetIntervalNanos==null` ∨ `rateChange` ∨ `coverage<floor` → **`NotCaptured`/no-autoritativo, NUNCA CLEAN**.
Fórmulas: `missedFrames_i = max(0, round(d_i/T)−1)`; `dropFraction = (Σmissed+reportLoss)/expectedFrames ∈ [0,1)`
(denominador = ESPERADO, no eventos); `discontinuity = ∃i: d_i≤0` (→ sesión INVALIDATED, precedencia #1). Fixes a
`FrameStatsCalculator`: `drops=count{>nominal·1.5}` cuenta EVENTOS no frames; `maxJitter=maxOf{abs(it−nominal)}`
TRAGA deltas negativos (discontinuidad invisible). **Oráculos:** 60-on-120 → `NotCaptured` en S5 (CLEAN diferido
S6); half-rate-jank → `NotCaptured` en S5 (EXCLUDED diferido S6). El par discriminante {60-on-120, half-rate} es
gate de aceptación de S6, NO se declara verde en S5.

## R3-10. Autoridad estructural de estados (R3-AUD-10, minimalidad D) — evidencia §9

`FocusEvidenceStatePolicy.evidenceOf()` (`:36-48`): `when` de 4 ramas, NINGUNA devuelve `OFFICIAL_ELIGIBLE`,
`else→PROVISIONAL`. `FocusEvidenceState` (`:16`) NO es `@Serializable`, sin `fromWire`, no se persiste (tipo de
retorno in-memory recomputado) ⇒ cero superficie de deserialización/`valueOf` a atacar. Idéntico al patrón
hermano probado `SessionEvidenceStatePolicy.stateOf` (lane cognitiva). **DECISION:** el mecanismo actual (when
exhaustivo que nunca devuelve el valor + scanner de sitio-de-construcción como defensa-en-profundidad) es
suficiente y NO bloqueante. **Escalón opcional (Opción 1):** quitar `OFFICIAL_ELIGIBLE` del tipo S5 (garantía por
compilador, no por test) al coste de comparar solo los 3 nombres compartidos en el test de paridad de precedencia
contra `SessionEvidenceState`. Rechazadas: sealed-ctor-privado (el serializador generado podría construir en el
mismo módulo), capability-token (sobre-ingeniería: el problema de S5 es «no debe existir», no «quién lo
construye»), frontera de módulo (exigiría un Gradle module nuevo, sin precedente en `:shared`).

## R3-11. Estado exacto de S5: descriptivo / sombra / training-only (D-R3-02) — evidencia §9.11

**D-R3-02 (CERRADA):** en S5, `SessionDescriptive` puede calcular rendimiento observado; `S_full`/`S_adm`/
`excludedFraction`/simulaciones existen SOLO en SOMBRA; ningún diagnóstico temporal produce evidencia de
medición. **Techo de estado (nombre a FIJAR en Paso 7 — precisión efficacy C1):** el techo shipped es
`PROVISIONAL` para una sesión LIMPIA + dosis-completa (`FocusEvidenceStatePolicy.kt:47`, KDoc `:25`), y
`TRAINING_ONLY`/`NotCaptured` para una sesión comprometida/no-capturada; aquí `PROVISIONAL` NO insinúa
elegibilidad-de-perfil (barrera mecánica + `OFFICIAL_ELIGIBLE` inconstructible) — pero como en la lane cognitiva
`PROVISIONAL` SÍ insinúa perfil, Paso 7 debe fijar un nombre inequívoco (mantener `PROVISIONAL` con la negación
explícita, o renombrar el techo de focus2). S6 monta e instrumenta pero NO desbloquea medición: siguen como gates
SEPARADOS (1) asociación estímulo-frame defendible, (2) error temporal end-to-end acotado físicamente,
(3) device-effects + paridad humana, (4) shadow longitudinal, (5) aprobación científica.
**Eje conductual separado (no es calidad temporal):** el scorer emite `Invalidated(NO_DISCRIMINATION)` cuando
`CI_lower(S_full)≤0` (`DescriptiveScorer.kt:208-209`) y `DEGENERATE_RESPONDING` — defendibles POR SESIÓN
(«¿medimos algo?»), pero **prohibido agregarlos sobre supervivientes** sin corrección de truncamiento (sesgo MNAR
al alza **estimado** por la lente assessment en +0.17..0.24 d′, heterogéneo: máximo cerca del umbral, ~0 para los
fuertes — estimación conservadora, no constante medida). Barreras perfil/rank intactas ⇒ el sesgo de agregación
es riesgo FORWARD de S6, no breach shipped. **Deuda del eje CONDUCTUAL (NB-4 psicómetra):** borrar
`PRE_ONSET_RESPONSE` del enum TEMPORAL es honesto (clasificar «pre-onset» exige un onset fiable que S5 no tiene),
pero el gating de patrón impulsivo/anticipación es obligación del eje CONDUCTUAL (charter #5); el scorer ya
clasifica anticipaciones por-trial (`anticipationFloorMs`), pero el GATE de patrón impulsivo NO existe y como el
RT no se retiene en S5 (tally-only) NO puede añadirse aquí — aceptable solo porque la lane es `TRAINING_ONLY`.
Debe RE-establecerse en el eje conductual en S6 junto con la lane RT, NO «renacer» como valor de enum temporal;
trazado como deuda conductual, no temporal.

## R3-12. Contrato de revocación linealizable (R3-AUD-09) — evidencia §9.12

**GAP-1 RESURRECCIÓN (CRÍTICO, vivo):** focus2 no tiene acoplamiento de revocación. seal/checkpoint son `put`
crudo sin guard (`SecureFocusSessionStore.kt:106-107`); `FocusWriteOutcome` = solo `Stored|Failed`, **sin
`RejectedRevoked`** (`:169-174`); erasure = `clearNamespace(mindboost_focus2)` blanket (`AppGraph.kt:159`), toca
`SubjectRevocationAuthority` en NINGÚN sitio. La afirmación R2 §14 «CABLEA…RejectedRevoked» describe código
inexistente. **GAP-2 SANGRADO MULTI-CUENTA (HIGH, vivo):** el blanket clear al borrar A borra el buffer y el
**tombstone REVOKED** de B (namespace compartido; claves ya subject-prefixed ⇒ apto para el fix F2 de 6-R3).
**GAP-3 (decisión a auditar, NO resolver):** `restore()`/`FocusSessionRestore` = adopt/recover cross-process
muerto en prod que CONTRADICE D-R3-04; o dead-code a borrar o decisión nueva con su propia auditoría.

**Contrato (diseño; IMPLEMENTATION_STATUS = PENDING):** reutilizar `SubjectRevocationAuthority` (2026-07-14)
verbatim sobre `mindboost_revocation` (write-once, nunca AccountBound), inyectada en ambos stores.
`withSubjectLock(userId){ markRevoked(userId) FIRST; deleteSubjectKeys("focus2_session:<uid>:*");
deleteSubjectKeys("focus2_consent:<uid>:*") }`. seal/checkpoint/consent-write ⇒ `guardedWrite`; reads que puedan
correr contra erase ⇒ `guardedRead` triádico (`Allowed|SubjectRevoked|StatusUnavailable`; StatusUnavailable
NUNCA procede a payload — fail-closed). Erase subject-scoped (sacar `focus2Store` del blanket; re-pin
`AccountBoundErasureCoverageTest`). Consent axis SEPARADO: revoke `TRIAL_JOURNAL` con subject Active reescribe
REVOKED (no borra), no toca el tombstone de subject. Holder ante `RejectedRevoked`: `start()` no presenta trial;
`submit/checkpoint` mid-run = abort terminal; `StatusUnavailable` = rechazo retryable (degradar a práctica). En
abort: `probe.stop()` (libera el callback + buffer) + purga de memoria.

**Linearization point:** el `statusOf(subject)` leído MIENTRAS se tiene `SubjectLocks.forKey(subject)`; en erase,
el `markRevoked` put dentro de `withSubjectLock`; cross-process, el tombstone DURABLE (el mutex en memoria es
irrelevante tras reinicio — `statusOf` re-lee el latch durable al inicio de cada op). **Qué sustituye la tabla de
9 interleavings:** (1) **scanner single-serializer** — ningún `put(` crudo sobre `mindboost_focus2` fuera de
`guardedWrite` (un solo call-site sin guard rompe la linealizabilidad para TODOS los schedules); (2) argumento
single-lock (obligación de prueba documentada); (3) latch monótono write-once (test idempotencia +
`Revocation`∉AccountBound). Los 9 interleavings solo fijan los instantes-frontera; se prueban con barreras
DETERMINISTAS (`GatedSecureKeyValueStore` con `CompletableDeferred` dentro de la sección crítica; process-death =
authority+stores frescos sobre el MISMO backing map), NUNCA sleeps.

## R3-13. Retención coherente con drain-not-adopt (D-R3-04) — evidencia §9.13

**D-R3-04 (CERRADA):** el residuo write-ahead en cold start es un intento ABANDONADO ⇒ purga-antes-de-leer,
idempotente, SIN `MAX_ABANDONED_SESSION_AGE_HOURS` ni wall-clock. El código YA es TTL-free
(`FocusTracerStateHolder.kt:154-157`: scan+discard antes de sealDescriptor, `NonCancellable`; el `adopt` viejo
se eliminó a propósito; scan Failed no borra nada). **A BORRAR de la R2 §14:** `MAX_ABANDONED_SESSION_AGE_HOURS`
+ el startup wall-clock sweep (contradicen D-R3-04). Declaración honesta: la cota de retención es «próximo
focus2 `start()` O account deletion»; una app que no corre no puede imponer un TTL físico. account-deletion y
subject-revocation siguen siendo rutas inmediatas independientes. **Hallazgo (MEDIA):** `start()` descarta el
`FocusWriteOutcome` de cada `discard` del bucle de drenaje (`:156`) — un purge fallido es silencioso; fix:
agregar los `Failed` y exponerlos en `FocusStartOutcome`/breadcrumb (como `complete()` hace con `bufferPurged`).

## R3-14. Versiones exactas y migración (R3-AUD-11) — evidencia §9.14

**`TemporalCapture` requerido (diseño; PENDING):**
```
data class TemporalCapture(probeCapability, clockDomain, inputPrecision, nominalHzBand: HzBand?, temporalObservationVersion)
fun seal(..., temporalCapture: TemporalCapture): FocusSessionDescriptor   // SIN default → error de compilación si se omite
fun migrateFromV1(...): FocusSessionDescriptor                            // SOLO fixtures de test
```
`nominalHzBand` nullable DENTRO del holder requerido: fuerza el ACTO de captura pero permite «no determinable»
honesto (VRR/LTPO). **Hoy `seal()` conserva los params temporales como opcionales-con-default** (precisión
kmp-architect: `FocusSessionDescriptor.kt:186-190` — los 4 primeros con `?=null` + `temporalObservationVersion: Int = 0`)
— la forma que la R2 llama «el problema»: «caller v1 honesto» y «olvidé cablear la captura» son byte-idénticos.
`migrateFromV1` se aísla por AUSENCIA de caller de producción (el runtime es drain-not-adopt: un proceso vivo
siempre sella con el build actual; los v1 en disco se leen por `restore()` decode, nunca se re-sellan).
Versiones desconocidas/futuras YA fallan cerrado (2 mecanismos): futuro → `SCHEMA_UNSUPPORTED`
(`FocusSessionRestore.kt:99-100`); lector v1 ante blob v2 → `ignoreUnknownKeys=false` (`:33`) → decode exception →
`*_UNREADABLE`. **Golden de wire-format AUSENTE** (grep 0 en `FocusSessionDescriptorTest`): sin un literal JSON
pinneado, un rename de campo pasa inadvertido (round-trip del mismo build es limpio). AÑADIR el golden.

**Dónde vive cada versión + disposición R3:**

| Versión | Vive en | Estado shipped | Disposición R3 |
|---|---|---|---|
| `temporalObservationVersion` | descriptor | existe (`:74`) | conservar |
| `temporalQualityModelVersion` | snapshot | **ausente** | AÑADIR (deuda R2) + constante en `TemporalClassPolicy` |
| `thresholdsVersion` | snapshot | **ausente** (solo in-memory en el config) | AÑADIR al snapshot (deuda R2) |
| `monotonicGuardVersion` | resultado | no existe | **OBSOLETO** — versionaba el mecanismo `S_full`-o-`Absent` FALSADO/retirado (D-R3-01). Si D-R3-01 introduce un gate nuevo, ESE gate lleva su propio nombre de versión; no reciclar |
| `retentionPolicyVersion` | descriptor | no existe | **OBSOLETO** — D-R3-04 reemplaza la política por edad por un comportamiento estructural incondicional (no una política versionable) |
| `metricFormulaVersion` | scorer | sin cambio en S5 | conservar |

## R3-15. Tabla DECISIÓN → IMPLEMENTACIÓN-pendiente → VALIDACIÓN-pendiente (R3-AUD-12) — evidencia §9.15

Separación de tres campos (el corazón de R3-AUD-12). `DECISION_STATUS` = qué decidió la auditoría;
`IMPLEMENTATION_STATUS` = qué hay en el código shipped HOY; `VALIDATION_STATUS` = qué falta demostrar
físicamente/empíricamente.

| Tema | DECISION | IMPLEMENTATION (shipped hoy) | VALIDATION |
|---|---|---|---|
| Gate de medición §6.1 / `mg-1` | **RETIRADO** (falsado, D-R3-01) | el gate §6.1 nunca existió en código (era `DESIGNED_NOT_RUN`); el shipped `FocusTemporalEligibility` YA es outcome-independent | property test + M-OI a congelar |
| Elegibilidad temporal outcome-independent | **CERRADA** (D-R3-01) | conforme (`FocusTemporalEligibility:47-98`); falta mover el piso a conteos delivered + congelar la propiedad | — |
| S5 no certifica accuracy/RT | **CERRADA** (D-R3-02) | conforme (lane de medición inconstructible; techo TRAINING_ONLY/PROVISIONAL) | causal UNKNOWN → Paso 6 |
| Marcador = proxy no-clasificante | decidido (R2) | **CONFORME (Paso 5, 2026-08-18)**: `markerVerdict` borrado, `truncationSlackNanos` fuera de la firma y del holder; AC#3 satisfecho por PROPIEDAD (`theRenderMarkerNeverChangesAVerdict`, 5 formas × 6 valores), no por escáner | — |
| `ONSET_INTEGRITY`/`PRE_ONSET_RESPONSE` fuera del enum S5 | decidido (R2 §4.2) | **CONFORME (Paso 5)**: borrados tras verificar 0 productores; AC#4 protegido por escáner de fuentes sobre `commonMain` | — |
| Partición de trial tipada (TrialDisposition/AbortReason/SessionInvalidationReason) | **CERRADA** (R3-AUD-04) | PENDING (enum plano mezcla poblaciones; `SessionInvalidationReason` no existe) | — |
| `validate()` fail-closed de tallies | **CERRADA** (R3-AUD-05) | PENDING (no existe; grep 0) | — |
| `BUFFER_OVERFLOW` + buffer acotado (LongArray ring) | **CERRADA** (§R3-16/§7) | **CONFORME (Paso 5)**: `FrameTimeRing` (cap duro) + `bufferOverflow` ⇒ `NoObservation` + sobrecarga `LongArray` ⇒ 0 alloc/frame POR CONSTRUCCIÓN | Microbenchmark de allocations/GC — **`NOT_RUN`** (exige dispositivo, Paso 6) |
| Modelo de cadencia de 6 hechos + NotCaptured sin expected cadence | **CERRADA** (R3-AUD-07) | **CONFORME (Paso 5)**: `expectedCadenceHz` (lo que la plataforma DECLARA) y `detectedCadenceHz` (lo que llegó) son hechos separados; desacuerdo ⇒ `NoObservation`; iOS declara `expectedCadenceHz = null` | oráculos 60-on-120/half-rate VERDES en `desktopTest`; gate de montaje sigue S6 |
| Revocación linealizable + erase subject-scoped | **CERRADA** (R3-AUD-09) | **CONTRADICHO** («CABLEADO» falso; GAP-1/GAP-2 vivos) | 9 interleavings con barreras |
| `TemporalCapture` requerido + versiones persistidas | **CERRADA** (R3-AUD-11) | PENDING (`seal()` con 5 params `?=null`; versiones ausentes del snapshot; golden ausente) | — |
| Retención sin TTL (drain-not-adopt) | **CERRADA** (D-R3-04) | conforme; a BORRAR `MAX_ABANDONED_SESSION_AGE_HOURS` del §14; purga silenciosa ante fallo a corregir | — |
| Autoridad estructural de `OFFICIAL_ELIGIBLE` | **CERRADA** (mecanismo actual suficiente; Opción 1 opcional) | conforme | — |
| Capa de plataforma (actuals Android/iOS) | `CONTRACT_INERT_NO_WIRING` | conforme (solo compila; no montado) | montaje = S6 |
| Export de datos | **DIFERIDO** (derecho/gate de beta del PRD; nunca «entregado» en S5) | no aplica | S8+ |
| Perfil/rank/global/score oficial | **INALCANZABLE** | conforme (3 barreras) | — |

Reconciliación documental (contenido vivo, no notas posteriores): `05_TECHNICAL_CURRENT_STATE` y `09_DECISION_LOG`
deben reflejar IMPLEMENTATION=PENDING donde la R2 decía «hecho»; el YAML `user_outcome`, «P3 vertical», «export
diferido», «delete local pendiente», «CABLEADO», context-pack 2.0.0 vs **2.0.2** (canónico; el 2.0.0 fue la
entrega externa desfasada, no una incoherencia del repo), y «S6 no promete commit real» se corrigen en el mismo
cambio de docs (§R3 pendiente de Paso 7). Archivos ajenos del working tree: `DMListScreenIcon.ios.kt`,
`AppMediaViewer.ios.kt` (port iOS, preservados, no staged).

## R3-16. Follow-ups con owner y gate — evidencia §9.16

| ID | Follow-up | Owner | Gate |
|---|---|---|---|
| F-R3-A | Cerrar GAP-1/GAP-2 (revocación guarded + erase subject-scoped) | data-sync-dev + security-privacy | 9 interleavings verdes + scanner single-serializer |
| F-R3-B | Partición de trial tipada + `SessionInvalidationReason` + `validate()` | cognitive-engine-dev + kmp-architect | property tests de álgebra + fail-closed |
| F-R3-C | Marcador→proxy no-gateante (L1) + borrar enum fantasma | cognitive-engine-dev | scanner AC#3 + AC#4 verdes — **CERRADO 2026-08-18 (Paso 5)**; AC#4 por escáner, AC#3 por propiedad (declarado) |
| F-R3-D | Buffer `LongArray` ring + `BUFFER_OVERFLOW` + `DisposableEffect` + Microbenchmark | mobile-performance | §7 observer gate PASS — **PARCIAL 2026-08-18**: ring+overflow+DisposableEffect HECHOS; Microbenchmark `NOT_RUN` |
| F-R3-E | Modelo de cadencia de 6 hechos + NotCaptured-sin-expected | mobile-performance | oráculos S5=NotCaptured — **CERRADO 2026-08-18 (Paso 5)** |
| F-R3-F | `TemporalCapture` requerido + versiones al snapshot + golden wire-format | kmp-architect | compila-error si se omite + golden pinneado |
| F-R3-G | Property test outcome-independence + M-OI + piso sobre delivered + regla no-agregación | psychometrician + test-engineer | M-OI pone rojo al reintroducir `CI_lower` |
| F-R3-H | GAP-3 `restore()`: borrar dead-code O decisión nueva auditada | kmp-architect | ADR nuevo si se cablea |
| F-R3-I | Borrar `MAX_ABANDONED_SESSION_AGE_HOURS` §14 + purga no-silenciosa | data-sync-dev | — |
| F-R3-J | Contrato UX de sesión comprometida (§R3-17, 8 requisitos) + DPIA (proxies de tecnología asistiva) | product-ux + privacy-security | gate de aceptación S6 + copy-scanner |
| F-R3-K | Paso 6 físico (fotodiodo, device matrix) — techo `PROVISIONAL` hasta entonces | assessment + CEO | validación física — **DISEÑADO 2026-08-18 (Paso 6), `NOT_RUN`**: protocolo M1-M9 + matriz 4 Android / 2 iOS + inventario pedido al CEO en [`S5_PASO6_PHYSICAL_PROTOCOL.md`](S5_PASO6_PHYSICAL_PROTOCOL.md). Corrección de método (`MB-FOCUS-0067`): un fotodiodo NO mide `D` por sí solo (no ve el evento de entrada) |
| F-R3-L | Resolver `criticalJitterFactor` (config versionada o excepción declarada) + escribir el validador `init{}` de `TemporalQualityThresholds` | kmp-architect + mobile-performance | invariantes `windowDropCleanMax<windowDropExcludeMin` y `MIN_FRAMES_WINDOW<MIN_FRAMES_SESSION` — **CERRADO 2026-08-18 (Paso 5)**: excepción no-versionada DECLARADA + pinneada por comportamiento (2.4×/2.6×); `init{}` escrito con test anti-exceso que rechaza la comparación cross-población |

Ninguno se cablea a medias; todos fail-closed hasta su gate. NO se continúa con la implementación (steps 5–14)
en esta invocación.

## R3-17. Minimización y contrato UX (privacy-security) — evidencia §9

Minimización campo-por-campo: **LIMPIA** (sin trials crudos, sin epoch, sin ID de dispositivo, Hz banded, cero
sink remoto en focus2). Nota BAJA no-bloqueante: `inputModalityName`/`screenReaderActive` = proxies de tecnología
asistiva (afín a discapacidad), ya minimizados; documentar en DPIA. Consent separado del summary: correcto y
verificado por código (el reducer no recibe consent; `FocusJournalAdmission` es puro). Contrato UX de sesión
comprometida (la «§6» del contrato de reapertura = ESTE §R3-17, no la «## 6» R2 de este ADR; gate de aceptación
S6, 8 requisitos): solo descriptivo; sin comparación baseline/progreso/capacidad/dispositivo; mensaje
neutral «no comparable» (por `mindboostCopySafetyScanner`); repetir sin penalización; no rompe racha/XP; no
actualiza perfil/rank (barrera mecánica); no muestra clase T como badge cognitivo ni RT en ms absolutos;
distingue «practicaste» de «esta sesión no es evidencia fiable».

## R3-18. Umbrales ejecutables (R3-AUD-08) — evidencia §9

Resolver-o-quitar-consumidor: `MIN_TERTILE_N` (ausente; bloqueante solo al implementar tertiles),
`thresholdsVersion`/`temporalQualityModelVersion` (ausentes del snapshot — deuda R2), `criticalJitterFactor`
(ausente de `TemporalQualityThresholds`; el flag `criticalFrameJitter` existe ya computado — o se añade a la
config versionada o se declara excepción no-versionada dependiente de Hz de plataforma). NaN/±Inf: `HzBand.of`
es safe por construcción (cae a `OTHER`); el guard fail-closed que pide el ADR vive en `FrameStatsCalculator`
(VERIFICAR por separado). Comparación float: denominadores enteros acotados (buffer 1024) → documentar como
no-issue (más barato que aritmética racional). Validador `init{}` (NO existe): cuando se escriba, separar
`windowDropCleanMax < windowDropExcludeMin` (misma población, NECESARIO) de `sessionExcludedMax ∈ [0,1]` (sin
comparación cruzada); `MIN_FRAMES_WINDOW < MIN_FRAMES_SESSION` SÍ es necesario (misma población). Confirmado del
contrato: `windowDropExcludeMin < sessionExcludedMax` NO es invariante dimensional necesario (poblaciones
distintas).

## R3-19. Observer-effect §7 (mobile-performance) — gate BLOQUEANTE antes de montaje — evidencia §9
> **Cierre de S5 (Paso 7, 2026-08-18): `CONDITIONAL`, `T-S5` NO emitido.** Ver
> [`S5_PASO7_CLOSURE.md`](S5_PASO7_CLOSURE.md). Este gate sigue en `FAIL_RESIDUAL_B5_UNMEASURED`: B1-B4/B6
> están cerrados en código, B5 (magnitud del efecto observador) sigue SIN MEDIR y exige dispositivo
> (`MB-FOCUS-0066`; protocolo en `S5_PASO6_PHYSICAL_PROTOCOL.md §4`). El gate de salida además corrigió dos
> defectos de la capa de observación: `MB-FOCUS-0069` (el adapter Android fabricaba la cadencia esperada
> con `?: DEFAULT_REFRESH_HZ`, de modo que una ausencia de referencia clasificaba) y `MB-FOCUS-0070` (serie
> ordenada en memoria con cero lectores, además desalineada por un append previo al check de rechazo).


> **ESTADO 2026-08-18 (Paso 5, [`S5_PASO5_OBSERVER_AND_REDTEAM.md`](S5_PASO5_OBSERVER_AND_REDTEAM.md)):
> `FAIL` → `FAIL_RESIDUAL_B5_UNMEASURED`.** B1, B2, B3, B4 y B6 CERRADOS en código con gates verdes. **B5
> sigue abierto**: la causa dominante (allocations en el hot path) está eliminada por construcción, pero la
> magnitud residual del efecto observador **no está medida** — el Microbenchmark exige dispositivo (Paso 6).
> El gate **NO se declara PASS** y sigue bloqueando el montaje (S6). El texto original se conserva íntegro.

**FAIL — no montar. 6 bloqueantes:** B1 buffer no acotado / sin `BUFFER_OVERFLOW` (`ArrayList(1024)` es capacidad
inicial, no cap; `FrameTimingProbe.android.kt:21` sin tamaño); B2 autoboxing por frame → «0 alloc/frame» FALSO
por inspección (`LongArray` ring requerido); B3 sin `DisposableEffect` en `rememberWindowedTemporalProbe()` →
callback Choreographer auto-reposteante nunca parado; B4 report-loss confundido con drops; B5 el probe induce
GC/calor/batería que luego clasifica (jank auto-cumplido, confunde el térmico §10.2); B6 = L1 (marcador gatea).
PASS: sin lock/IO/scoring en el callback. «0 alloc/frame» = **NOT_VERIFIED** (falso por inspección; exige
Microbenchmark).

## R3-20. Tests congelados como aceptación de implementación (§10)

**Matemática/property:** (M0) caso exacto D0→D1→D2 (base A) + base B; (M1) para toda secuencia de degradación
técnica, `E` nunca mejora; (M2) permutar outcomes conservando trazas técnicas no cambia `E` (metamórfico, 1000
seeds); (M3) sesión limpia a nivel azar NO se invalida por elegibilidad temporal (sí puede por el eje conductual
del scorer, que es distinto); (M4) NaN/∞/cero-datos/límites/overflow; (M5) añadir pausa/abort nunca mejora `E`;
(M-OI) **mutación**: reintroducir `S_adm`/`CI_lower`/gate outcome-dependent en `E` DEBE poner ≥1 test rojo — con
el **fixture base straddleando la frontera** (`CI_lower=0` / `S_adm=S_full`), o la mutación puede pasar verde
vacuamente (anti-vacuidad, NB-3 psicómetra; learnings 2026-07-20/07-15).
**Álgebra de trial:** cada delivered una disposición; aborted no crea `SdtCell`; razones primarias no duplican
trials; pausa pre- vs post-presentación; discontinuidad de reloj invalida con precedencia existencial; los dos
drenajes conservan cardinalidades separadas; fixed-count y time-boxed producen tertiles correctos (bordes N=0/1/2).
**Plataforma:** el proxy vsync/FrameMetrics NO se lee como onset/commit (scanner AC#3); expected cadence ausente
nunca fabrica CLEAN; 60-on-120 y half-rate → `NotCaptured` en S5; VRR/rate-change/report-loss tipados;
`getEventTimeNanos` no crea por sí solo un error bound; `BUFFER_OVERFLOW` fail-closed y memoria acotada; enum sin
`ONSET_INTEGRITY`/`PRE_ONSET_RESPONSE` (AC#4). **Privacidad/seguridad:** revocación antes/durante/después de
write; process death entre tombstone y purga; reinicio completa la purga; borrar A no toca B; ninguna key
reaparece; cold-start purga antes de leer; revocar journal no altera el summary necesario; cero dato cognitivo en
logs/analytics. **Schema/autoridad:** fixture v1 real → migración explícita; v2 sin `TemporalCapture` no compila;
`migrateFromV1` no acepta un descriptor v2 vivo; schema futuro rechazado; mismatch de versión/fingerprint
fail-closed; `OFFICIAL_ELIGIBLE` no construible por API/serializer/caller externo.

## R3-21. Agentes del gate de entrada (§8) — outputs preservados

6 lentes read-only (sin Gradle, sin editar), 2026-07-21 tras el reset del límite de sesión. Outputs íntegros en
los ficheros `.output` de la sesión + integración en `scratchpad/S5_R3_lens_integration.md`. Veredictos:
psicómetra **R3-AUD-01 FALSADO / -02 -03 CONFIRMADOS**; assessment **falsación independiente CONFIRMADA** (base
60/20/20/60) + MNAR +0.17..0.24 d′; mobile-performance **§7 FAIL** (6 bloqueantes) + L1 marcador-gatea +
8+4 renombres D-R3-03; kmp-architect **R3-AUD-04 no-representable / -05 parcial / -06 con matices / -08 tabla /
-10 sin-bloqueante / -11 pendiente**; security-privacy **GAP-1/2 vivos** + contrato linealizable; privacy-security
minimización **LIMPIA** + purga silenciosa (MEDIA) + contrato UX §6. Ninguna mayoría: una falsación reproducible
dominó. **PENDIENTE: gate de SALIDA** (efficacy · psychometrician · assessment · kmp-architect · mobile-performance
· privacy-security · diff-auditor) sobre ESTA R3.

## R3-22. DoD §12 y veredicto de esta invocación

`FROZEN_FOR_S5_R3` requiere TODOS: `mg-1` retirado ✓ (§R3-0/1); ninguna `Absent→Present` en R3 (E no emite valor;
outcome-independent) ✓; elegibilidad outcome-independent ✓ (D-R3-01); accuracy/RT inconstructibles en S5 ✓
(D-R3-02); `PROVISIONAL` no se usa para datos no validados ✓; FrameMetrics no «commit/onset real» ✓ (D-R3-03);
API 34/ns no = exactitud ✓; trial algebra/doble drenaje/tertiles representables sin contradicción ✓ (con los
fixes de §R3-6/7/8); símbolos indefinidos enumerados con disposición ✓ (§R3-14/18); revocación/retención con
contrato coherente + estado HONESTO (GAP-1/2 declarados vivos, no «CABLEADO») ✓; autoridad de estados estructural
✓; status/plan/ADR/source-pack a reconciliar (Paso 7) — PENDIENTE; **todos los gates documentales revisaron la R3
DEFINITIVA — PENDIENTE (gate de salida no corrido); no se modificó implementación dependiente ✓**.

**VEREDICTO = `BLOCKED` para congelar, con prerequisito EXACTO:** correr el gate de salida de 7 lentes sobre esta
R3 candidata + la reconciliación documental del Paso 7 (§R3-15). La R3 candidata está COMPLETA en contenido (16
ítems de evidencia §9, 5 familias de tests §10, decisiones D-R3-01..04, 12 hallazgos con veredicto y
DECISION/IMPLEMENTATION/VALIDATION). La falsación núcleo está PROBADA y confirmada por dos lentes independientes.
NO se continúa con la implementación (steps 5–14). Techo honesto: **CANDIDATA / BLOCKED**.

═══════════════════════════════════════════════════════════════════════════════════════════════════════════
# HISTORIA — R2 (SUPERSEDED por R3, conservada íntegra como evidencia; NO es autoritativa)
═══════════════════════════════════════════════════════════════════════════════════════════════════════════

## 0. Reencuadre nuclear de la R2 (resultado del gate de 7 lentes)

La R1 contenía tres sobreafirmaciones que el gate falsó con evidencia reproducible; la R2 las corrige:

1. **No existe ancla de commit defendible en S5** (S5R-AUD-01). El «marcador de render» Android
   (`System.nanoTime` ↔ `Choreographer.frameTimeNanos`) es el **inicio de vsync programado del frame**
   (familia `INTENDED_VSYNC`), NO prueba de que el estímulo se dibujó/compuso/presentó, y su callback
   se re-postea cada frame independientemente de que algo se dibuje ⇒ solo puede **subestimar** el
   retardo (sesga hacia CLEAN). ⇒ **el marcador se degrada a un proxy renombrado que NO gatea ninguna
   clasificación en S5**; ambas plataformas usan la rama conservadora. El ancla real (FrameMetrics) es
   trabajo de la slice de montaje S6 (§19).
2. **La monotonía estaba AFIRMADA, no garantizada** (S5R-AUD-03). El psicómetra reprodujo con el scorer
   real que excluir 5 misses (conducta fija) sube d′ de **1.310708 → 1.679992** y el CI-inferior de
   **0.716380 → 1.023001** — la SE ENSANCHA (0.303→0.335) pero el estimado sube ~3× más, así que
   «menos n ⇒ CI más ancho» (argumento de la R1 §10.3) NO cierra la cláusula de no-inflación. La R2
   la sustituye por un **mecanismo probado**: emisión de `S_full` (D-invariante) o `Absent` (§6.1, §10.3).
3. **P3 NO es una slice vertical de plataforma** (S5R-AUD-11). Los actuals Android/iOS no tienen caller
   de runtime, el descriptor real queda `NotCaptured`, `eventTime` no llega al reducer y `NANOS_NATIVE`
   es infalsable. ⇒ **la capa de plataforma de P3 se reclasifica `CONTRACT_INERT_NO_WIRING`** (actuals
   solo verificados por COMPILACIÓN); la capa de DOMINIO se prueba vertical con un harness de
   integración `desktopTest` (§15); el montaje productivo es la slice S6 (§19).

Consecuencia transversal: como en S5 **no hay ancla de commit + no hay `eventTime` real de plataforma +
la precisión es `MILLIS_DERIVED`**, la **lane de RT/medición es INCONSTRUCTIBLE en S5** — espejo exacto
de `OFFICIAL_ELIGIBLE` inconstructible y de la regla cerrada «nada RT-crítico entra a evidencia oficial
en v1». Todo lo que S5 produce honestamente es la **lane descriptiva** (techo `PROVISIONAL`), en sombra
y sin montar.

## 1. Contexto y reencuadre de v1

Dos hechos del repo reordenan el riesgo respecto al enunciado original de S5:

1. **En v1 ningún RT es evidencia y el runtime ni siquiera lo retiene**: `FocusRuntimeSnapshot` es
   tally-only. El scorer (`DescriptiveScorer`) emite accuracy/d′/criterio/omisiones por condición desde
   conteos SDT; no emite ninguna estadística de RT.
2. **La cantidad en riesgo es la ACCURACY**, no el RT: la ventana de respuesta se arma desde el dispatch
   de `PRESENT_TRIAL` (`presentedAt = clock.elapsedNanos()` en el dispatch, ANTES de cualquier
   frame-commit). Un render tardío acorta la ventana perceptiva efectiva y un onset sesgado reclasifica
   respuestas; ambos canales tocan d′/criterio.

**Dos lanes explícitas** (S5R-AUD-02/04, decisión R2):

- **Lane DESCRIPTIVA** — d′/accuracy sobre TODAS las ventanas con su outcome observado (= `S_full`),
  siempre acompañada de `excludedFraction`. Es lo que un usuario vería; techo `PROVISIONAL`; JAMÁS
  cableada a perfil/rank. `NO_OBSERVATION` cuenta aquí (la conducta es conducta; S5R-AUD-02 refutado
  como defecto por eficacia: no hay «accuracy de medición» separada que contaminar en v1).
- **Lane de MEDICIÓN** — gate de certificación (§6.1) que emite `S_full` o `Absent`; en S5 es SOMBRA y,
  por §0, estructuralmente inconstructible para RT. Su parte de accuracy existe como tipo pero se
  certifica solo bajo el gate monótono.

La parte «vistosa» original (excluir RT por ventana) es bookkeeping en sombra; la parte tratada como
benigna («accuracy conservada salvo discontinuidad de reloj») era **falsa** y es donde la R2 concentra
las salvaguardas.

## 2. Decisión nuclear

**Dos lanes (descriptiva vs medición) × exclusión por ventana × gate monótono `S_full`-o-`Absent`**, con
backstop binario de sesión y terminal `TRAINING_ONLY`; accuracy conservada en la lane descriptiva; RT
CLEAN-only e inconstructible en S5; todo conteo ENTERO y estratificado por condición; el marcador de
render degradado a **proxy que no clasifica** (rama conservadora en toda plataforma).

### 2.1 Tabla de alternativas (S5-04; ratificada + ampliada por el gate R2)

| # | Alternativa | Veredicto R2 |
|---|---|---|
| 1 | Binaria de sesión | **Backstop** (`excludedFraction > sessionExcludedMax`; discontinuidad de reloj) |
| 2 | Por segmento | **N/A**: no existe subestructura de segmento; reformulada como histograma por-trial (§5) + bins de tercio (§5.1) |
| 3 | Por ventana (trial) | **Núcleo** de la exclusión (abre MNAR → §10) |
| 4 | Elegibilidad por métrica | **Núcleo** (accuracy y RT tienen vulnerabilidades distintas; §6.1) |
| 5 | Down-weight `1 − frameDropFraction` | **RECHAZADA**. Motivo estadístico PRECISO (S5R-AUD-06 P2): N fraccional es **incompatible con el estimador binomial ACTUAL** (`dPrimeSe` exige Ns/Nn enteros para `h(1−h)/(Ns·φ²)`); no es «imposibilidad universal», es incompatibilidad con esta fórmula + perilla continua inauditable |
| 6 | Inflar incertidumbre (DEGRADED) | **Tier medio, DIFERIDO a Paso 4**. Función designada `rt-infl-1`: `SE·√(1 + λ·n_DEGRADED/n_admitido)`, λ=1.0 inicial, mantiene N entero (solo escala SE) ⇒ no repite el rechazo de la alt 5. NO se produce en S5 |
| 7 | Freeze / no-elegible | **Terminal** `TRAINING_ONLY`. Falsada como mecanismo PRIMARIO (assessment): presupone serie longitudinal persistente que S5 no crea; degenera a `Absent`, que el gate §6.1 ya expresa mejor |
| 8 | Nueva serie de comparabilidad | **Fuera de alcance** (cambio de dispositivo/modalidad, no jank transitorio) |
| — | Cap `min(S_adm, S_full)` (assessment lo evaluó como candidato) | **FALSADA numéricamente** (assessment §B): con conducta fija, empeorar una ventana MISS sube `S_adm` de 0.8·full a 0.95·full ⇒ `min` sube +0.111 → viola no-upgrade. El cap frena EN `S_full` pero no impide el ascenso HACIA `S_full` desde abajo |
| — | Worst-case-credited `d′_wc` (assessment lo propuso como núcleo) | **NO adoptado como emisor** (§20 adjudicación): imputa una ventana truncada (un HIT observado como miss) como miss ⇒ muestra un número reducido = penalización de hardware en régimen de exclusión ligera (charter §14). Su aporte (falsación del cap, patología de CI-inferior) SÍ se adopta |

Composición R2: **lanes 4** + **ventana 3** + **gate monótono `S_full`-o-`Absent`** (§6.1) + **6** tier
medio diferido + **1** backstop + **7** terminal. Cada fila se somete a las simulaciones §16.

## 3. Modelo de capacidades por plataforma (S5-06)

Piso de producto: **Android API 29** (ADR-FOCUS-PLATFORM-MIN, 09 §20.2, TEC-002 SUPERSEDED) · iOS 15.
**Estado de la capa de plataforma en S5: `CONTRACT_INERT_NO_WIRING`** — los actuals se verifican SOLO por
compilación (Android `compileAndroidMain` en JVM; iOS CI macOS); no hay caller de runtime ni montaje
productivo (§15, §19).

| Plataforma | Fuente de frames | Fuente de input | Techo estructural |
|---|---|---|---|
| Android 29–30 | Choreographer frame callbacks; `FrameMetrics` (clase API 24; `INTENDED_VSYNC`/`VSYNC` API 26) | `MotionEvent.eventTime` (ms→ns derivado) | T1 |
| Android 31–33 | + `FrameMetrics.DEADLINE`/`GPU_DURATION` (API 31) | ídem | T1 |
| Android 34+ | ídem | + `MotionEvent.getEventTimeNanos()` nativo ⇒ `InputTimestampPrecision.NANOS_NATIVE` | T1 (precisión de input nativa) |
| iOS 15+ | `CADisplayLink` (`timestamp`/`targetTimestamp`/`duration`; familia `mach_absolute_time`) | `UITouch.timestamp` (misma familia) | T1 **con caveat declarado** (§3.1) |
| Desktop | NoOp | — | T0 (jamás oficial) |

**Regla de captura Android de S5 (corregida, S5R-AUD-01/06):** el marcador Choreographer NO clasifica —
es un proxy renombrado `firstVsyncAfterStateSetNanos` (§6.2). El ancla FrameMetrics (onset defendible =
`INTENDED_VSYNC + TOTAL_DURATION`; `DEADLINE` en 31+; `dropCountSinceLastInvocation` como report-loss
tipado; copia inmediata del objeto reciclado) es **obligación de la slice de montaje S6**, no de S5.
`getEventTimeNanos` NUNCA es requisito de base (API 34, no 29). El error de cuantización `MILLIS_DERIVED`
vs `NANOS_NATIVE` es **≤1 ms** = sub-frame en todo refresco (S5R-AUD-06 P2#2) ⇒ **API 29–33 NO se excluye
de accuracy/T1 por esa etiqueta**; `InputTimestampPrecision` gatea SOLO la lane de RT, y aún ahí el error
DOMINANTE es la captura en el callback de puntero (5–33 ms), no la cuantización.

### 3.1 Límite epistémico de iOS (declarado, no resuelto en S5)

`CADisplayLink` observa la cadencia de refresco del sistema, **no** el frame que Compose Multiplatform
(Skia/Metal) compositó. Es un proxy de jank para clasificar integridad temporal, pero NO un ancla de
commit ⇒ iOS opera por la rama conservadora (§4). Documentado en el KDoc del `actual`. El port
CADisplayLink SÍ se hace/compila en S5 (CI macOS); `CONDITIONAL` se reserva para lo que exige hardware.

## 4. Clases temporales y buckets de integridad

### 4.1 `TemporalClass` — integridad de la OBSERVACIÓN (por sesión)

`T0_NON_AUTHORITATIVE` (sin probe autoritativo / dominio `UNKNOWN` / `sampledFrames < MIN_FRAMES_SESSION`;
jamás RT-elegible) · `T1_FRAME_OBSERVED` (cadencia observada por probe de dominio autoritativo con Hz
real) · `T2_LATENCY_BOUNDED` / `T3_EXTERNALLY_CALIBRATED` (**sin constructor en S5**; exigen banco físico).
La clase deriva de la MEDICIÓN de la sesión, jamás del modelo de dispositivo. La precisión de input es un
campo ORTOGONAL de capability, no una clase. **Trampa prohibida (psicómetra P0-3):** `FrameStats.UNAVAILABLE`
tiene `frameDropFraction=0.0`; el gate de muestra va PRIMERO. **Guard de finitud (S5R-AUD-06):** un Hz
nominal no-finito/≤0 pasado a `FrameStatsCalculator` fabricaría 0 drops ⇒ CLEAN; el calculador DEBE
fail-closed a `UNAVAILABLE` ante Hz no-finito/≤0 (no solo los `actuals`).

### 4.2 `WindowIntegrity` — veredicto por ventana (por trial)

| Bucket | Condición (S5) | Accuracy (descriptiva) | RT (sombra) |
|---|---|---|---|
| `CLEAN` | sin anomalía: drops ≤ `windowDropCleanMax`, sin jitter crítico | atribuible | inadmisible en S5 (§0) |
| `DEGRADED_USABLE` | anomalías presentes bajo el piso de exclusión | atribuible | inadmisible en S5 (`rt-infl-1` = Paso 4) |
| `NO_OBSERVATION` | probe sin observación de la ventana (report loss, `sampledFrames < MIN_FRAMES_WINDOW`) | atribuible (conducta es conducta) | jamás |
| `EXCLUDED_TRUNCATION_RISK` | drops `> windowDropExcludeMin` o jitter crítico | outcome CONSERVADO en `S_full`; retirado de `S_adm` (§6.1) | jamás |

**Cambio R2 clave (S5R-AUD-01):** en S5 el veredicto es **conservador en TODA plataforma** — el marcador
NO gatea. Esto elimina la asimetría Android/iOS que la R1 producía (mobile-performance: un marcador
no-nulo enrutaba toda ventana Android a la rama laxa que solo llegaba a `DegradedUsable`, mientras iOS
excluía; drops idénticos ⇒ resultados distintos). Sin marcador defendible, la única posición honesta:
`drops > windowDropExcludeMin` (o jitter crítico) ⇒ `EXCLUDED_TRUNCATION_RISK`.

**Buckets EXCLUDED retirados de la taxonomía S5 (S5R-AUD-01 CD-3):** `PRE_ONSET_RESPONSE` y
`ONSET_INTEGRITY` se ELIMINAN del enum en S5 — no tienen productor y no son detectables sin un ancla de
onset que S5 no posee. Un enum cerrado que promete una protección no construida es deshonesto. Renacen en
S6 cuando exista el marcador FrameMetrics + `eventTime` real.

### 4.3 Taxonomía unificada de exclusión (`WindowExclusionReason`, S5)

Enum cerrado, contado POR CONDICIÓN: `PAUSE_DISCARD` (trial abierto descartado por pausa; contarlo por
condición cierra el cherry-picking de pausar ante los difíciles) · `CLOCK_EPOCH` (guard cross-epoch
existente `atElapsedNanos < presentedAt`) · `TRUNCATION_RISK` (rama conservadora: `drops >
windowDropExcludeMin`) · `BUFFER_OVERFLOW` (**nuevo, S5R-AUD-06 P2#6**: ventana nunca drenada que supera
la capacidad del buffer ⇒ fail-closed tipado, no crecimiento ilimitado). `contaminatedCount` legacy se
conserva con `Σ PAUSE_DISCARD + CLOCK_EPOCH` como invariante testeable.

## 5. Granularidad: la ventana crítica es el TRIAL

- Unidad de clasificación = el trial. Sin subestructura de «segmento» persistida.
- **Dos drenajes por trial**: drain#1 al PRESENTAR (cubre el ISI anterior → alimenta SOLO la tendencia de
  sesión; JAMÁS la clase de una ventana) y drain#2 al RESOLVER (clasifica ESA ventana). Así el jank de ISI
  no altera ningún resultado (CA-1.1) y la deriva de sesión coexisten.
- Idempotencia/carreras: contador de GENERACIÓN entero en el probe; callbacks de generación vieja no
  escriben. `start()`/`stop()` fuera de orden = no-op observable.

### 5.1 Bins de tercio para la tendencia (S5R-AUD-08)

El detector de tendencia (§10.2, Paso 4) necesita «primer tercio vs último» sin serie ordenada (que
privacy prohíbe). La R2 persiste **bins ordinales fijos** (privacy-security §B, opción (1)+(3)):
`SessionTertile{FIRST,MIDDLE,LAST}` + `TertileWindowTally(tertileName, bucketName, count)` en el snapshot,
plegados en el MISMO punto que las celdas temporales; `tertileOf(trialIndex, plannedTrials) =
trialIndex*3/plannedTrials` (clamp a LAST) donde `plannedTrials` es un HECHO llevado en el input, jamás
re-derivado por el reducer. Invariante `Σ tertileTallies.count == resolvedTrials`. Gate derivado (nunca
persistido) `tertileCoverageComplete`; N-1 sin tercios ⇒ `NotCaptured`. Piso anti-vacuidad `MIN_TERTILE_N`.
El runtime es **drain-not-adopt** (muerte de proceso ⇒ intento descartado, no reanudado), así que esta
forma es segura y forward-compatible. La FORMA + el pliegue son P3; el DETECTOR que la consume es Paso 4.

## 6. Cadena de contaminación por métrica (S5-03)

| Métrica | Modo de degradación | Regla R2 |
|---|---|---|
| Accuracy/d′ (lane descriptiva) | render tardío acorta ventana; **piso de anticipación dispatch-anchored** (S5R-AUD-01 CD-1) | la lane descriptiva emite `S_full` sobre outcomes observados; el piso de 150 ms es dispatch-anchored y su corrección `rt − D` **exige un ancla de commit que S5 no tiene** ⇒ limitación DECLARADA, corrección diferida a S6; sin corrección, ningún trial con RT dispatch-anchored acuña un HIT en la lane de MEDICIÓN (esa lane es inconstructible en S5) |
| Accuracy/d′ (lane de medición) | exclusión selectiva de errores infla d′ (S5R-AUD-03, PROBADO) | gate `S_full`-o-`Absent` (§6.1); nunca se emite un número mejorado por exclusión |
| Accuracy por condición | jank covaría con condición | detector diferencial §10.1 ⇒ CONTRASTE `Absent(DIFFERENTIAL_TEMPORAL_EXCLUSION)`; niveles por condición sostenibles |
| Omisiones | ventana truncada | `EXCLUDED_TRUNCATION_RISK` conservado en `S_full`, retirado de `S_adm` |
| RT (SOMBRA) | drops truncan RT largos | **INCONSTRUCTIBLE en S5** (§0: sin ancla de commit + sin `eventTime` real + `MILLIS_DERIVED`); cuando exista (S6): CLEAN-only + `NANOS_NATIVE` + `excludedFraction` obligatoria; detector §10.2 ⇒ `Absent(NON_EXCHANGEABLE_TIME_TRUNCATION)` |
| Verdicts de conteo (degenerate/gaming) | jank como excusa | INDEPENDIENTES de la clase temporal; se evalúan sobre conteos crudos SIEMPRE |

### 6.1 Gate monótono de la lane de medición (S5R-AUD-02/03/04 — psicómetra, adjudicado §20)

Sea `S_full` el estadístico (d′ por condición, su CI-inferior, contraste) sobre TODAS las ventanas
quality-blind (las filas EXCLUDED conservan sus conteos SDT enteros — §7), y `S_adm` sobre el subconjunto
admisible (todo salvo `EXCLUDED_*`).

```
Present(condición) SII:
  conteos ADMISIBLES por celda ≥ nMinPerCell (MinSampleGate.DPRIME)   // piso sobre admisibles
  ∧ CI_inferior(S_full) > 0                                            // discriminado
  ∧ S_adm ≤ S_full                                                     // la exclusión NO ayudaría
  ∧ ningún trigger diferencial/tendencia/backstop
VALOR emitido cuando Present = S_full     // D-invariante: función solo de (condición, isSignal, response)
Si no Present ⇒ Absent(razón)             // → TRAINING_ONLY vía §8
```

- El número de la lane de medición ∈ {`S_full`, `Absent`}; NUNCA `S_adm`, NUNCA `min(S_adm,S_full)` (ambos
  filtran desde abajo — falsados en §2.1/§20). Toda transición por empeoramiento de calidad es
  `S_full → Absent`, jamás una subida numérica. `CI_inferior` emitido = el de `S_full` (D-invariante) ⇒
  **inmune a la patología de estrechamiento de SE** que assessment demostró para un CI recomputado sobre
  admisibles (M2, §16).
- **Por qué es honesto, no solo seguro:** se emite `S_full` solo cuando `S_adm ≤ S_full`, i.e. las
  ventanas excluidas eran net-favorables (contarlas no puede inflar). Cuando son net-desfavorables
  (misses truncados/FAs sesgados), contarlas penalizaría (`S_full` bajo) Y dropearlas inflaría (`S_adm`
  alto) — ambos mal ⇒ `Absent`, con la lane descriptiva mostrando `S_full + excludedFraction`.
- El piso `MinSampleGate.DPRIME` (≥20 señal ∧ ≥20 ruido/condición) va sobre los conteos ADMISIBLES: una
  sesión mayoritariamente EXCLUDED carece de admisibles suficientes ⇒ `Absent(INSUFFICIENT_ADMISSIBLE_N)`
  (subsume S5R-AUD-02 CD-4 sin el retiro naïf que INFLARÍA). Versión: `monotonicGuardVersion = "mg-1"`.

### 6.2 Marcador de render — PROXY, no ancla de commit (S5R-AUD-01)

Renombrado `presentToFirstFrameCommitNanos` → **`firstVsyncAfterStateSetNanos`** y documentado como
proxy: el primer vsync observado tras el set de estado (`Choreographer.frameTimeNanos ≥ mark`), NO prueba
de draw/submission/composición/presentación. En S5 **NO participa en la clasificación** (§4.2). Un test
mecánico fija que el campo no es leído por `TemporalClassPolicy` en la ruta de veredicto. El ancla real
(FrameMetrics-derivada; `INTENDED_VSYNC + TOTAL_DURATION`, `DEADLINE` en 31+) es S6.

## 7. Sufficient statistics — forma exacta + álgebra formal (S5R-AUD-07)

Celda persistida:

```
TemporalCellTally(
  conditionName,   bucketName,   cell: SdtCell,   excludedByReason: Map<reason, Int>
)
TertileWindowTally(tertileName, bucketName, count)   // §5.1
```

**Álgebra formal (partición declarada):**

- `resolvedTrials` = Σ sobre todas las celdas de `cell.total()` = trials con outcome. **DISJUNTOS por
  (condición, bucket)**: cada trial resuelto aparece en EXACTAMENTE una celda.
- `deliveredTrials` = trials presentados. `discardedOpenTrials` = presentados nunca resueltos
  (`PAUSE_DISCARD`/`CLOCK_EPOCH`). `deliveredTrials = resolvedTrials + discardedOpenTrials`.
- `officialCell(condición)` = Σ_buckets `cell` = la `ConditionTally` oficial legacy (invariante
  `Σ buckets ≡ oficial`).
- `excludedByReason` = **ANOTACIÓN del MISMO trial**, no un conjunto nuevo: un trial `EXCLUDED` tiene su
  outcome en `cell` (bucket EXCLUDED) Y su razón en `excludedByReason` — NO se cuenta dos veces (el
  outcome vive en `cell`; la razón es metadato de por qué esa ventana fue excluida). Las razones
  never-resolved (`PAUSE_DISCARD`/`CLOCK_EPOCH`) NO tienen `cell` (no hubo outcome) y su Σ ≡
  `contaminatedCount`.
- `temporalCoverageComplete` = (Σ ventanas clasificadas == `deliveredTrials`). `excludedFraction` = celdas
  EXCLUDED / clasificadas. Ambos DERIVADOS (funciones puras), jamás persistidos.

**Validador fail-closed (`validate()`, corre en restore y complete):** no-negatividad · suma con overflow
chequeado · condición/bucket/reason conocidos del dossier+versión · conservación por condición
(`Σ buckets == oficial`) · `Σ tertileTallies == resolvedTrials` · consistencia `contaminated` · consistencia
oficial↔sombra cuando la cobertura es completa · reglas distintas para N-1/cobertura parcial (→
`NotCaptured`) · mapas/listas duplicados o corruptos → rechazo · schema unknown/future → rechazo tipado.
**Un snapshot corrupto jamás fabrica `Eligible`.**

Solo agregados enteros/enums/mapas de conteo; ninguna lista ordenada por tiempo (privacy P0-02). Las
estructuras por-ventana del cómputo son efímeras (`windowVerdicts` en memoria) y se descartan al plegarse.

## 8. Estados y precedencia (composición)

Punto ÚNICO de derivación, llamado desde `complete()`. Precedencia (aplicación de mayor a menor; el estado
resultante de menor a mayor valor es `INVALIDATED ≺ TRAINING_ONLY ≺ PROVISIONAL ≺ OFFICIAL_ELIGIBLE`):

1. `INVALIDATED` — discontinuidad de reloj MONOTÓNICO (salto atrás/imposible; jamás wall-clock,
   falsificable) o verdict `Invalidated` (degenerate/gaming, independiente del jank).
2. `TRAINING_ONLY` — cualquier gate en false, `excludedFraction > sessionExcludedMax` (backstop),
   admisible bajo piso, o propósito no de medición.
3. `PROVISIONAL` — todo admisible; **techo de v1**.
4. `OFFICIAL_ELIGIBLE` — **INCONSTRUCTIBLE** (test exhaustivo + scanner de sitio de construcción, §12/S5R-AUD-12).

`FocusEvidenceState` es un enum **LOCAL de focus2** (S5R-AUD-12; §8.1). Prohibido fabricar un
`CognitiveSessionRun` sintético para reusar `SessionEvidenceGates`.

### 8.1 Autoridad de estados local (S5R-AUD-12 — corrige la R1 §8, que prescribía importar `SessionEvidenceState`)

La R1 prescribía `import SessionEvidenceState` de la lane cognitiva; **la barrera de consumer-sweep de esa
lane lo PROHÍBE** (ningún archivo commonMain fuera del paquete puede NOMBRAR sus tipos hasta una decisión
de cableado revisada). La implementación usa correctamente un enum LOCAL `FocusEvidenceState` con los 4
nombres cerrados. La R2 ratifica el enum local y añade DOS guardas mecánicas (kmp-architect, reusando el
patrón `CognitiveSessionEvidenceBoundaryTest.bridged_comparability_has_no_construction_site`):

1. **Scanner de sitio de construcción**: un `desktopTest` escanea las fuentes de producción `focus2/**`
   por el literal `FocusEvidenceState.OFFICIAL_ELIGIBLE` y afirma que aparece SOLO dentro de
   `FocusEvidenceStatePolicy.evidenceOf` — convierte «único productor» de convención KDoc en garantía
   mecánica («OFFICIAL inalcanzable desde CUALQUIER callsite», no solo desde `evidenceOf`).
2. **Test de paridad de PRECEDENCIA** (no solo de nombres): afirma que el orden de severidad de
   `FocusEvidenceState` coincide con el de `SessionEvidenceState` canónico, para que los dos vocabularios
   no deriven en SIGNIFICADO manteniéndose idénticos en nombre.

La R2 documenta: la duplicación es deliberada (decisión de 3 vocabularios, §18); `evidenceOf` es el único
productor **por guarda mecánica**; solo un puente futuro revisado colapsaría los dos tipos en uno.

## 9. `FocusTemporalEligibility` y razones de ausencia

Veredicto temporal: `Eligible` · `Excluded(reasons)` · `NotCaptured`. **`Degraded(inflateUncertainty)` NO
se produce en S5** (S5R-AUD-04): la inflación de SE (`rt-infl-1`) es Paso 4 y no se congela sin fórmula —
su fórmula queda designada en §2.1/§6 pero DESIGNED_NOT_RUN. `NotCaptured` (obligatorio): un registro N-1
restaurado o `temporalCells` vacío NO es «0 ventanas malas» (fabricaría máximo) NI «0 buenas»; es «no
capturado», mismo molde que `Verdict.InsufficientData`, y **NO degrada** la lane descriptiva (eficacia
AUD-02 refutado: `NotCaptured` → `PROVISIONAL`, accuracy intacta).

`AbsenceReason` se extiende con: `INSUFFICIENT_ADMISSIBLE_N` (→ NIVELES; si toca la condición primaria,
cascada a `TRAINING_ONLY`) · `DIFFERENTIAL_TEMPORAL_EXCLUSION` (→ CONTRASTE solo; niveles sostenibles) ·
`NON_EXCHANGEABLE_TIME_TRUNCATION` (→ estadístico de RT de sesión solo; accuracy conservada). Clock
discontinuity tiene precedencia #1 (`INVALIDATED`, §8), derivada SOLO de invariantes monotónicas. Guard
anti-vacuidad: todo operando de toda comparación pasa por `requirePresent` que FALLA ante ausencia.

## 10. Detectores anti-cherry-picking e invariante de monotonía

### 10.1 Exclusión diferencial por condición
> **Estado real (Paso 7, `MB-FOCUS-0054`): NO IMPLEMENTADO.** No existe ningún consumidor de esta regla
> en el árbol; la constante que la parametrizaba se retiró de la config para que su presencia no siguiera
> implicando que la detección ocurre. La política de missingness que la justificaría es `MB-FOCUS-0053`,
> abierta. Lo de abajo es DISEÑO, no comportamiento.

`max − min` de `excludedFraction` por condición `> differentialExclusionDelta` ⇒ CONTRASTE
`Absent(DIFFERENTIAL_TEMPORAL_EXCLUSION)`; niveles por condición sobre admisibles se sostienen.

### 10.2 Tendencia temporal (MNAR / térmica)
Deriva monótona de exclusión (primer tercio vs último, sobre los bins §5.1) ⇒ RT de sesión
`Absent(NON_EXCHANGEABLE_TIME_TRUNCATION)` + confound térmico citado; accuracy conservada. **Detector =
Paso 4**; la forma persistida (bins) = P3.

### 10.3 Invariante de monotonía (No-Free-Progress temporal) — PROBADO, no afirmado (S5R-AUD-03)

**Contraejemplo que la R1 no cubría** (psicómetra, aritmética exacta con el scorer real): full
30H/10M/10FA/30CR ⇒ d′=1.310708, CI-inf=0.716380; excluir 5 misses (conducta fija) ⇒ d′=1.679992,
CI-inf=1.023001. La SE ENSANCHA (0.303→0.335) pero el estimado sube ~3× más ⇒ el argumento «menos n ⇒ CI
más ancho» NO cierra la no-inflación. Simétrico para excluir 5 FAs; excluir un correcto (H/CR) BAJA d′.

**Mecanismo (sustituye la afirmación de la R1):** el gate §6.1 emite `S_full` (D-invariante) o `Absent`.
Sea `D` un operador que empeora la clase/bucket de ≥1 ventana sin tocar ningún `(condición, isSignal,
response)` — bajo `D` el conjunto admisible solo se ENCOGE (CLEAN→DEGRADED→EXCLUDED es monótono):

- **(a) No-upgrade:** valor emitido ∈ {`S_full` (constante bajo `D`), `Absent` (⪯ presente)} ⇒ estado no
  crece; toda transición es hacia `Absent`.
- **(b) No-inflación de estimado/CI/cambio/contraste:** `S_full` es función solo de
  `(condición, isSignal, response)` — invariante bajo `D`; el CI emitido es el de `S_full` (sin patología
  de SE); reliable-change/contraste se computan de `S_full` por condición.
- **(c) No `Absent → Present`:** si `D(R)` es `Absent`, cualquier `D'` peor encoge más el admisible ⇒
  sigue `Absent`.

Funciona excluyendo M/FA/H/CR por separado y con exclusión concentrada por condición (§10.1) / outcome /
tercio (§10.2). Enunciación **anti-inflación**, NUNCA «peor calidad ⇒ peor score» (eso penalizaría el
hardware — charter §14; es la razón de rechazar el `d′_wc` worst-case como emisor, §20). Property-test
1000 seeds (S14) + mutación M1 (emitir `S_adm` ⇒ ≥5 tests rojos) + M2 (CI naïf ⇒ el caso de exclusión de
hit se pone rojo). `monotonicGuardVersion = "mg-1"`.

## 11. Umbrales — config versionada en sombra (S5-05)

Cuatro ROLES con constantes SEPARADAS: presupuesto UX de frame ≠ umbrales de integridad ≠ pisos de
suficiencia (`MinSampleGate`) ≠ mapeo observación→clase (`MIN_FRAMES_*`).

| Nombre | Valor inicial | Estado |
|---|---|---|
| `MIN_FRAMES_WINDOW` | 3 | DESIGNED_NOT_RUN |
| `MIN_FRAMES_SESSION` | 120 (~2 s @60 Hz) | DESIGNED_NOT_RUN |
| `windowDropCleanMax` | 0.05 | DESIGNED_NOT_RUN; sensibilidad Paso 4 |
| `windowDropExcludeMin` | 0.15 | DESIGNED_NOT_RUN; sensibilidad Paso 4 |
| `criticalJitterFactor` | 2.5× período nominal | DESIGNED_NOT_RUN |
| `sessionExcludedMax` | 0.50 | DESIGNED_NOT_RUN; sensibilidad Paso 4 |
| `differentialExclusionDelta` | 0.15 | **DISEÑADO SOLO AQUÍ — RETIRADO de `TemporalQualityThresholds` en Paso 7** (`MB-FOCUS-0054`): §10.1 lo define, pero su detector nunca se construyó (R2 lo definió, R3 lo retiró, Pasos 4-6 no lo reconstruyeron) y la constante llevaba tres fases con CERO consumidores, leyéndose como umbral vigente. Vuelve a la config con su detector y su test |
| `truncationSlack` (por dossier) | flanker: 500 ms | **RESERVADO / NO USADO en S5** (S5R-AUD-01/05): solo gateaba la rama de marcador, ahora deshabilitada; se activa con el ancla FrameMetrics de S6 |
| `MIN_TERTILE_N` | (por dossier) | DESIGNED_NOT_RUN (§5.1) |
| `hzBands` | {60,90,120,144,240,OTHER} | fijo (banding de persistencia) |
| `bufferCapacityFrames` | 1024 | fijo; **overflow ⇒ `BUFFER_OVERFLOW` fail-closed** (§4.3) |

**Validación de umbrales (S5R-AUD-06 P2#4, en `init`):** todos finitos, en rango `[0,1]` donde aplica,
ORDEN correcto (`windowDropCleanMax < windowDropExcludeMin < sessionExcludedMax`; `MIN_FRAMES_WINDOW <
MIN_FRAMES_SESSION`), `thresholdsVersion` no vacía, slack válido si presente. **Congelación:** cambiar un
valor = nueva `thresholdsVersion` PERSISTIDA en el snapshot (S5R-AUD-10; la R1 no la persistía);
re-clasificación retroactiva PROHIBIDA. **Calibración sin datos cognitivos (S5R-AUD-06 P3#9):** los
umbrales son propiedades del RENDER, ortogonales a la cognición ⇒ se calibran con arnés debug de frame
streams bajo jank/térmica inyectados (sink local, cero respuestas cognitivas) + oráculos sintéticos
(Paso 4); nunca se pancea con respuestas cognitivas.

Aritmética de tamaño: presentados ≥ (40/condición)/(1−e) ⇒ flanker 2-cond ≈ ≥95; vigilancia ≈ ≥118 a e=0.15.

## 12. Relojes, dominios y DST (S5-08)

Cuatro papeles que NUNCA se mezclan: Duración (monotónico intra-dominio, solo deltas) · Instante civil
(epoch wall, SOLO metadato `startedAt`, más `sealedAtWallClockMillis` label-only de §14) · Fecha local (no
participa) · Identidad de dominio (`ClockDomain` sellado; `ANDROID_CHOREOGRAPHER_UPTIME` ·
`IOS_CADISPLAYLINK_MACH_TIME` · `NONE_MONOTONIC_FALLBACK` · `UNKNOWN`). **Imposibilidad por construcción:**
ningún tipo del vertical expone un timestamp absoluto ⇒ la resta cross-dominio no puede escribirse.
Discontinuidad: SOLO invariantes monotónicas (guard cross-epoch `CLOCK_EPOCH`). El delta no-monotónico de
frames (`b < a`, señal de discontinuidad) hoy se traga en `maxFrameJitterNanos` muerto (S5R-AUD-06) —
gap declarado: la detección de discontinuidad de FrameStats es follow-up; el guard del reducer se conserva.

## 13. Versionado, esquema, migración y rollback (S5-07 + S5R-AUD-10)

| Versión | Vive en | Cambia cuando |
|---|---|---|
| `temporalObservationVersion` | descriptor (sellado) | cambia el contrato del probe/adapter |
| `temporalQualityModelVersion` | **snapshot (persistido, R2)** | cambia clasificador/policy de buckets |
| `thresholdsVersion` | **snapshot (persistido, R2)** | cambian valores de §11 |
| `monotonicGuardVersion` | resultado | cambia el gate §6.1 |
| `retentionPolicyVersion` | descriptor | cambia la política de retención §14 |
| `metricFormulaVersion` | scorer | **SIN CAMBIO en S5** |

**Corrección R1→R2 (S5R-AUD-10):** la R1 declaraba `thresholdsVersion`/`temporalQualityModelVersion`
«estampadas» pero NO existían como campos persistidos (solo parámetro in-memory). La R2 los PERSISTE en el
snapshot; un resume/reconstrucción no puede clasificar bajo el `DEFAULT` del build actual en silencio.

**DTO N-1 SEPARADO del constructor v2 (S5R-AUD-10):** `FocusSessionDescriptor.seal(...)` deja de aceptar el
grupo temporal como parámetros opcionales `null` (donde «caller v1 honesto» y «olvidé cablear captura» son
byte-idénticos). Se introduce un value holder REQUERIDO `TemporalCapture(probeCapability, clockDomain,
inputPrecision, nominalHzBand, temporalObservationVersion)` y DOS entradas: `seal(..., temporalCapture:
TemporalCapture)` (sin defaults; todo callsite v2 real es forzado por el COMPILADOR a aportar valores
reales) vs `migrateFromV1(...)` nombrado explícitamente para la ruta N-1. Omitir la captura en un callsite
v2 real pasa a ser un ERROR DE COMPILACIÓN (guarda más fuerte y barata que un registro de versiones).

- Descriptor `SCHEMA_VERSION` 2 (+capabilities + `retentionPolicyVersion` + `sealedAtWallClockMillis`).
  Snapshot `SCHEMA_VERSION` 2 (+`temporalCells`, `tertileTallies`, `temporalQualityModelVersion`,
  `thresholdsVersion`).
- **N-1**: lector v2 acepta v1 ⇒ campos temporales ausentes ⇒ `NotCaptured` (jamás Eligible/Excluded).
  Lector v1 ante blob v2 ⇒ rechazo fail-closed tipado (`ignoreUnknownKeys=false`). Fixture N-1 obligatorio.
- **Resume cross-build**: cobertura temporal parcial ⇒ `temporalCoverageComplete == false` /
  `tertileCoverageComplete == false` ⇒ RT nunca, tendencia `NotCaptured`; accuracy intacta. Resume real no
  existe (drain-not-adopt); cuando se construya, rechaza/degrada ante CUALQUIER mismatch de versión.
- **Rollback**: los campos nuevos dejan de emitirse; los persistidos caducan con el write-ahead efímero;
  `focus2/domain/evidence` + `core/timing` son aditivos y borrables sin tocar v1.

## 14. Privacidad, retención, revocación y consentimiento (dictámenes privacy-security + security-privacy)

- **Retención por campo (P0-01, CORREGIDO — S5R-AUD-09 PRIV-D1):** la R1 afirmaba retención = «vida de la
  sesión, purga en complete()/discard()». **Falso para una sesión ABANDONADA** (muerte de proceso, el
  usuario nunca vuelve): nunca se completa/descarta ⇒ el bound real es «siguiente arranque de focus2 O
  borrado de cuenta», SIN TTL de edad. La R2: (§D privacy) `sealedAtWallClockMillis` (label-only) +
  `retentionPolicyVersion` + `MAX_ABANDONED_SESSION_AGE_HOURS` + barrido en el ARRANQUE de la app
  (reutiliza el slot `recoverPendingDeletion`, sin job nuevo) + manejo de rollback conservador (edad
  negativa ⇒ «no determinable» ⇒ descarta en el próximo reloj-adelante; nunca «sesión nueva»). Purga de
  buffers efímeros en stop/discard: ya correcta.
- **Prohibición de series (P0-02):** cumplida (§7; solo enteros/enums/mapas; ningún `List` indexado por
  tiempo; `windowVerdicts` en memoria, jamás persistida).
- **Minimización (P1-02):** Hz detectado BANDEADO; térmica colapsada a 3 niveles; battery-saver boolean;
  lectura fallida ⇒ `UNKNOWN` tipado.
- **Revocación / resurrección (P1-01 + F1/F2 — CABLEADO, no solo gateado):** la R1 dejaba esto como «gate
  declarado (cablear O CONDITIONAL)». El contrato §5 AUD-09 ordena «Cablea». La R2 CABLEA
  `SecureFocusSessionStore` y `SecureFocusConsentStore` a `SubjectRevocationAuthority` (patrón probado,
  learning 2026-07-14): ambos reciben `revocationStore`, construyen la autoridad sobre el namespace
  permanente `Revocation` (nunca AccountBound), `guardedWrite` en seal/checkpoint (nuevo
  `FocusWriteOutcome.RejectedRevoked` ⇒ el holder ABORTA la corrida, distinto de `Failed`), `guardedRead`
  defensa-en-profundidad, y **borrado subject-scoped** que sustituye el `clearNamespace()` general — lo que
  cierra F1 (resurrección) **y F2** (borrado colateral de una segunda cuenta co-residente en el mismo
  device) en el MISMO cambio. Revocar `TRIAL_JOURNAL` a mitad de sesión no altera los sufficient
  statistics (el reducer no tiene parámetro de consentimiento; test obligatorio). Tabla de 9 interleavings
  (privacy §C) es la especificación de aceptación.
- **Consentimiento (P2-01 + F4):** 4 estados triádicos; scope único `TRIAL_JOURNAL`; sirven la MISMA
  finalidad (elegibilidad honesta) ⇒ necesarios para el servicio, sin consentimiento aparte. **F4 (Baja):**
  `FocusConsentRecord.toStatus` ordena por los dos millis wall-clock; un rollback entre grant→revoke
  misclasifica REVOKED→UNKNOWN (ambos deniegan, sin exposición) — fix oportunista (ordenar por orden de
  escritura, no por los millis).
- **Export (P1-04 — DIFERIDO, scope reconciliado con el contrato §3):** el plan prometía «export/delete»;
  la R2 reconcilia — **S5 entrega DELETE local** (borrado de cuenta + revocación de consentimiento +
  purga write-ahead, test-cubierto) y **DIFIERE el export de usuario** con owner (privacy/CEO) + gate
  (ADR-DATA-002 + PRI-004/005/006). NINGÚN export existe en el repo; `accountDeletion.ts` tiene CERO
  referencias a focus2. S5 solo deja tipos `@Serializable` export-ready. El `user_outcome` del plan
  «puede exportar y borrar todo» se corrige a «puede borrar todo; export = gap declarado con owner».
- **Data-flow (S5-12):** cero respuestas cognitivas a analítica; dos únicos sinks — store local cifrado
  (enteros/enums, local-only) y (tras el cableado) un breadcrumb de string fijo en fallo de revocación,
  sin campos de datos. Ningún `Throwable` externo crudo a telemetría.
- **Líneas rojas:** identificador de dispositivo/modelo/marca — RECHAZADO; trials crudos / series de
  frame-time — RECHAZADO.

## 15. Flag / no-wiring + reclasificación de la vertical (S5R-AUD-11)

**La capa de PLATAFORMA de P3 es `CONTRACT_INERT_NO_WIRING`:** los actuals Android/iOS no tienen caller de
runtime (confirmado: `FocusTracerStateHolder` referenciado solo por su declaración + su test), no hay
montaje productivo, `eventTime` no llega al reducer y `NANOS_NATIVE` es infalsable. Los actuals se verifican
SOLO por compilación (Android `compileAndroidMain`; iOS CI macOS sobre el diff final).

**La capa de DOMINIO SÍ se prueba vertical** con un HARNESS de integración `desktopTest` (el contrato
AUD-11(A) acepta «una sesión focus2 real en un harness/integration host»): un probe determinista de test
reporta una capability NO-DEFAULT, se sella un descriptor real (no `NotCaptured`), se arranca/detiene el
probe, `eventTime` se plumbea al reducer y se producen `temporalCells` + `tertileTallies` — probando que la
vertical de dominio es CÓDIGO VIVO, no muerto. El resultado sigue sin alcanzar perfil/score/ranking (las
tres barreras intactas: gap de tipos, boundary test, no-montaje; `OFFICIAL_ELIGIBLE` inconstructible;
`FocusEvidenceStatePolicy` sin call-site que altere un veredicto mostrado). El montaje productivo es S6 (§19).

## 16. Plan de validación

**Simulaciones (Paso 4; oráculo exacto por fila — psicómetra + assessment §C, 15 filas):** MCAR
(|d′adm−d′full|<tol) · MNAR-errors (excluir misses ⇒ d′ FLAT en `S_full`; admisible subiría a ~1.68 ⇒
`Absent`) · MNAR-corrects (hits degradados no excluidos ⇒ d′ sin cambio) · exclusión de cada outcome SDT
por separado (M→Absent, FA→Absent, H→emitir `S_full`=1.310708, CR→`S_full`=1.310708) · CI-inferior
ascendente (naïf sube, gate FLAT) · 60 vs 120 Hz misma actuación · small-clean vs large-degraded (admisible
manda ⇒ `INSUFFICIENT_ADMISSIBLE_N`) · probe UNAVAILABLE (jamás CLEAN) · todo degradado (`Absent` +
`InsufficientData` + `TRAINING_ONLY` con guard de conteos) · degenerate + jank (`Invalidated`) · exclusión
diferencial (contraste `Absent` con niveles presentes). Más: property-test de monotonía 1000 seeds (S14),
mutación M1 (emitir `S_adm` ⇒ ≥5 rojos), M2 (CI naïf), arnés anti-vacuidad (`requirePresent`).

**Suite de timing (S5R-AUD-06):** intentional-low-fps (60-on-120) NO todo-drops; jank-induced half-rate
(>50% dropped) NO CLEAN (el par discriminante para el fix de fuente de Hz — el sourcing de cadencia
esperada es S6); Hz no-finito/≤0 NO fabrica CLEAN; buffer overflow ⇒ `BUFFER_OVERFLOW`; jank solo-ISI
byte-idéntico.

**Validación física (Paso 6, DESIGNED_NOT_RUN):** protocolo con FOTODIODO/cámara de alta velocidad para el
`D` real (state-set→fotones, incognoscible desde código), VRR/LTPO real, curvas térmicas,
`refreshRate`-reportado vs cadencia presentada, `getEventTimeNanos` vs latencia de callback,
`dropCountSinceLastInvocation` por OEM. Matriz de dispositivos (API 29/30/31/33/34+; 60 LCD/OLED, 90, 120
fijo, 120 LTPO/VRR, 144; low-end/mid/flagship; battery-saver; cold vs 10-min soak; iOS 60 + 120 ProMotion).
El CEO solo aporta INVENTARIO; sin hardware ⇒ `CONDITIONAL`, jamás DONE.

> **Paso 6 ejecutado 2026-08-18 — el protocolo vive en [`S5_PASO6_PHYSICAL_PROTOCOL.md`](S5_PASO6_PHYSICAL_PROTOCOL.md), `DESIGNED_NOT_RUN / CONDITIONAL`.** Ese documento RECONCILIA este párrafo con lo que R2/R3/Paso 5 cambiaron: la banda API 26-28 es **VOID** (mínimo canónico API 29), la derivación `RT = eventTime − frameCommit` del orden-2 §C.3 está superada por `D-R3-03` + B6, y la columna «accuracy OFICIAL» del orden-2 §E es **falsa** bajo `D-R3-02`. Además separa fotodiodo (flanco de pantalla) de cámara de alta velocidad (reloj común) — no son intercambiables — y reencuadra el objetivo de `D` como su DISPERSIÓN, no su media.

## 17. Criterios de aceptación consolidados (testables; fuente entre paréntesis)

1. Jank solo-ISI ⇒ `SessionDescriptive` byte-idéntico (CA-1.1; dos drenajes §5).
2. Ventana con drops `> windowDropExcludeMin` ⇒ `EXCLUDED_TRUNCATION_RISK`; outcome conservado en `S_full`,
   retirado de `S_adm` (§4.2/§6.1).
3. El marcador `firstVsyncAfterStateSetNanos` NO es leído por `TemporalClassPolicy` en la ruta de veredicto
   (scanner); Android e iOS clasifican drops idénticos IGUAL (rama conservadora) (S5R-AUD-01).
4. `PRE_ONSET_RESPONSE`/`ONSET_INTEGRITY` NO existen en el enum S5 (removidos; S5R-AUD-01 CD-3).
5. Exclusiones/descartes POR CONDICIÓN contra `nMinPerCell`; pausa selectiva ⇒ celda `Absent` (§10.1).
6. Monotonía (§10.3): contraejemplo (d′ 1.311→1.680) reproducido; gate `S_full`-o-`Absent` lo neutraliza;
   property-test 1000 seeds + M1 (≥5 rojos) + M2.
7. Exclusión de cada outcome SDT: M/FA→`Absent`, H/CR→emitir `S_full` (nunca `S_adm`, nunca inflado).
8. `MILLIS_DERIVED` (API 29–33): accuracy/T1 ADMISIBLE; solo la lane de RT inadmisible (S5R-AUD-06 P2#2).
9. `UNAVAILABLE`/`NotCaptured` jamás CLEAN ni `Eligible`; NoOp jamás >T0; Hz no-finito ⇒ fail-closed.
10. `evidenceOf` jamás construye `OFFICIAL_ELIGIBLE` (exhaustivo) **Y** scanner: `.OFFICIAL_ELIGIBLE` solo
    en `evidenceOf` + paridad de PRECEDENCIA con la lane canónica (S5R-AUD-12).
11. Reducer puro: la observación llega como dato en el input; `plannedTrials`/`tertile` son hechos del
    input; cero acceso a reloj/probe dentro de `reduce()`.
12. Validador §7 fail-closed en restore y complete; snapshot corrupto jamás `Eligible`; álgebra de tallies
    es partición formal (S5R-AUD-07).
13. Persistencia: ninguna lista ordenada por tiempo; `tertileTallies` bins fijos; revocar `TRIAL_JOURNAL`
    mid-sesión no cambia nada; banding golden; `thresholdsVersion`/`temporalQualityModelVersion`
    PERSISTIDOS (S5R-AUD-08/10).
14. Revocación CABLEADA: 9 interleavings; post-borrado ninguna clave focus2 reaparece; F2 (borrado
    colateral) cerrado por subject-scoped delete; retención con `MAX_ABANDONED_SESSION_AGE_HOURS` + barrido
    de arranque O gap declarado con owner (S5R-AUD-09).
15. `TemporalCapture` REQUERIDO en `seal()` v2 (omisión = error de compilación); `migrateFromV1` separado;
    N-1 blob v1 ⇒ `NotCaptured`, blob v2 bajo lector v1 ⇒ rechazo tipado (S5R-AUD-10).
16. HARNESS de dominio `desktopTest`: sesión real sella capability NO-default, arranca/detiene el probe,
    plumbea `eventTime`, produce tallies; perfil/rank/score inalcanzables (S5R-AUD-11).
17. `core/timing` sin imports de features; focus2 sin referencias nuevas a perfil; boundary tests verdes
    sin tocar carve-out.
18. Ninguna cadena de UI/analítica afirma RT en ms absolutos, «precisión», ni surfacea la clase T como
    calidad cognitiva; reduced-motion/low-end jamás penalización cognitiva.
19. Gates del repo en serie con ejecución real demostrada (N executed + XML frescos + conteo), tras el diff
    FINAL; iOS = run CI macOS fresco sobre el SHA final (no `ee65a55`).

## 18. Riesgos declarados y follow-ups (con owner)

| Riesgo/gap | Estado | Owner / dónde se cierra |
|---|---|---|
| Tres vocabularios temporales (Speed · lane cognitiva · T0-T3) + enum `FocusEvidenceState` local | deliberado, con guardas mecánicas (§8.1) | kmp-architect, post-S5 |
| iOS: CADisplayLink no ancla el commit de CMP/Skia | declarado; rama sin-marcador | port de marcador iOS = S6 |
| Ancla de commit real (FrameMetrics: DEADLINE/GPU_DURATION/report-loss/missed-deadline) | NO en S5 (marcador = proxy) | **S6 (§19)** |
| Sourcing de cadencia ESPERADA (Android panel-rate falso-positivo; iOS median falso-negativo) | fix de plataforma | **S6** (`targetTimestamp`/`DEADLINE`) |
| VRR/LTPO: Hz cambia mid-sesión | riesgo top-1 | detección S5 (parcial) + validación física Paso 6 |
| `truncationSlack` reservado/no usado | activa con el ancla FrameMetrics | S6 |
| Buffer con `ArrayList<Long>` (autoboxing; «0 alloc» = **NOT_VERIFIED**, probablemente falso) | benchmark pendiente | swap a estructura primitiva + Microbenchmark = S5 P5 / S6 |
| Resurrección/borrado colateral focus2 (F1/F2) | **CABLEADO en S5** (§14) | S5 (este diff) |
| Retención por edad del write-ahead abandonado (F3/PRIV-D1) | barrido de arranque O gap declarado | S5 (§14) o declarado con owner |
| Export de usuario inexistente (PRI-004) | gap de producto declarado | producto/CEO; S5 solo deja tipos |
| Registro permanente + retención | NO se crea en S5 | CEO (ADR-FOCUS-DATA) |
| T2 (banco físico de latencia) | sin constructor | estudio futuro 0€ |
| `d′_UB` (best-case) para reliable-change/contraste cross-sesión | especificado, sin consumidor en focus2 | C-06 (cuando exista comparación cross-sesión) |

## 19. Slice de montaje posterior «S6 · Focus2 Mount» (definida por S5R-AUD-11)

Precondición de CUALQUIER exposición al usuario; NO parte de S5:

1. Enforce single-instance-per-user (`MB-FOCUS-0035`).
2. Captura REAL de `TemporalCapture` en el composable de montaje (vía el grupo de parámetros requerido §13).
3. Marcador de commit real Android (FrameMetrics + `invalidate()`; API-tiered §3) — desbloquea `rt − D`,
   `PRE_ONSET_RESPONSE`, onset-CLEAN, y la lane de RT.
4. Plumbing del `eventTime` real de plataforma a `onResponse()`/el reducer (hace `NANOS_NATIVE` un hecho
   ejercido).
5. Sourcing de cadencia esperada (`targetTimestamp`/`DEADLINE`) — cierra el falso-negativo iOS / falso-
   positivo Android de Hz.
6. Resolver o diferir explícitamente el export/delete UX.
7. Call-site de producción tras la disciplina de feature-flag del repo (exposición deliberada, reversible).
8. Re-correr la suite completa (desktopTest/detekt/android/iOS) sobre el diff de montaje.

## 20. Reconciliación del gate de 7 lentes (S5R-AUD-*, provenance de la R2)

7 lentes read-only (sin Gradle, sin editar): eficacia, psicómetra, kmp-architect, privacy-security,
mobile-performance, assessment-psychometrics, security-privacy. Convergencias y una DISCREPANCIA adjudicada:

- **Marcador (AUD-01):** eficacia + kmp-architect + mobile-performance CONVERGEN: proxy, no commit;
  asimetría Android/iOS. Adoptado: renombrar + rama conservadora en toda plataforma; FrameMetrics = S6.
- **Monotonía (AUD-03):** psicómetra PROBÓ el contraejemplo (1.311→1.680). Mecanismo adoptado:
  `S_full`-o-`Absent`.
- **DISCREPANCIA psicómetra vs assessment sobre el emisor de la lane de medición:** psicómetra propuso
  `S_full`-o-`Absent`; assessment propuso `d′_wc` (worst-case-credited). **Adjudicación: `S_full`-o-`Absent`**
  — trazado contra charter §14, `d′_wc` imputa un HIT truncado (observado como miss) como miss ⇒ muestra un
  número reducido = penalización de hardware en exclusión ligera, mientras `S_full`-o-`Absent` RETIENE ese
  caso (`S_adm>S_full`→Absent) y su CI-inferior D-invariante es inmune a la patología de SE. Se ACREDITAN de
  assessment: la falsación del cap `min(S_adm,S_full)` (+0.111), la patología de CI-inferior (razón de
  emitir el CI de `S_full`), las 15 filas de oráculo + S14 + M1/M2, y el arnés `requirePresent`.
- **AUD-04:** RT CLEAN-only + `NANOS_NATIVE` + ancla ⇒ inconstructible en S5; accuracy en API 29–33 admisible
  (mobile-performance + psicómetra convergen); `rt-infl-1` diferido; 3 AbsenceReasons → niveles/contraste/RT.
- **AUD-08/09/10/11/12:** privacy-security + security-privacy (revocación cableada F1/F2, retención F3,
  export diferido, trend bins) y kmp-architect (versionado `TemporalCapture`, CONTRACT_INERT + harness,
  autoridad de estados local con scanner) — sin discrepancia; todas adoptadas arriba.

Hallazgos NUEVOS del gate (además de los 12 AUD): F2 (borrado colateral, Alta), ND-1 (D sub-slack ignorado
— dissuelto al deshabilitar el marcador), PRIV-D1 (retención abandonada sin TTL), F4 (orden de consent por
wall-clock). Todos incorporados. El registro en `MINDBOOST_AUDIT_LEDGER.md` es responsabilidad de la sesión
principal (single-writer); los subagentes reportaron, no editaron.
