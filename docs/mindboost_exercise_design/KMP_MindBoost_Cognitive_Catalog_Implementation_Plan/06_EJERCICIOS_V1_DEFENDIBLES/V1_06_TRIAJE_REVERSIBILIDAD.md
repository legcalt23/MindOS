# V1_06 — Triaje de Reversibilidad

**Módulo:** Decisiones · **Verdict:** REVISE · **Prioridad:** P1 · **Profile-update:** **sí** (el v1 más cercano a profile-ready) · **TransferLabel:** NEAR_TASK_PRACTICE / `TASK_ONLY`

> **Mapa hecho / inferido / por_confirmar.** Este ejercicio EXISTE y es jugable end-to-end
> (`ReversibilityTriageStateHolder` + `ReversibilityEngine`, ground-truth `04_6_Decisiones_audit.md` E2).
> Lo que sigue distingue: **[HECHO]** = verificado en la auditoría de código; **[INFERIDO]** = deducción
> de diseño coherente con el repo; **[POR_CONFIRMAR_EN_REPO]** = símbolo/ruta probable que un agente debe
> verificar antes de tocar. Rutas probables bajo
> `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/decisiones/` (paquete
> `POR_CONFIRMAR_EN_REPO`: el audit cita holders en `decisiones/presentation/` y motores Alpha-Core en
> `com.mindverse.mindboost.core.scoring.ScoringEquations` / `ReversibilityEngine`).

> **Invariantes transversales que este ejercicio repite (no negociables):**
> - **null != 0.** Una métrica no computable produce `null`/`CALIBRATING`, JAMÁS `0.0`. La UI pinta `—`,
>   nunca un cero. (El sentinela `0.0` de `ScoringEquations.calculatePHD` cuando `AGRS>0.75` es
>   invalidación, no "sin datos"; este ejercicio NO lo propaga como score mostrable.)
> - **Cero claims de far-transfer, NI para negarlos.** No prometer "decides mejor en la vida"; tampoco
>   escribir "esto no te hace decidir mejor en la vida" (mencionar el dominio ya es contaminar). Copy
>   estrictamente dentro de la tarea.
> - **Anti-gaming solo cuenta si está implementado + testeado con adversarios.** El AGRS está
>   IMPLEMENTADO pero NEUTRALIZADO (`spatialEntropy=1.0`, `isDialDefault=false` hardcodeados): hasta
>   alimentar entropía real de swipe y testearla con perfiles adversarios, NO es cobertura.
> - **Banco pequeño / no-validado bloquea perfil.** El banco es procedural (no memorizable como texto)
>   pero seed=0 fijo + sin rotación de marcadores ⇒ memorizable como patrón; sin rotación testeada,
>   `BANK_CONTAMINATED`/`CALIBRATING`.
> - **Enfoque/Velocidad sin profile-update hasta timebase+jank reales.** No es este ejercicio (su señal
>   primaria es accuracy, no RT), pero la misma doctrina aplica al componente RT: el RT del lock va bajo
>   `UNTRUSTED_TIMEBASE` (epoch wall-clock) y NUNCA actualiza perfil.

---

## 1. Objetivo cognitivo realista

Práctica de **discriminación reversible/irreversible + control inhibitorio de la prisa**: clasificar una
acción como de **una vía** (irreversible, Type-1, "deliberar") o **dos vías** (reversible, Type-2,
"rápido") y **respetar el lock** de las irreversibles (esperar la ventana de revisión antes de
confirmar). Es una tarea **accuracy-based** y poco dependiente del RT absoluto: lo que mide es el
*orden* respecto al lock y el *lado* elegido, no la latencia fina.

**Lo que SÍ mide [HECHO/INFERIDO]:** (a) accuracy de triaje por tipo; (b) tasa de violación de lock como
proxy conductual limpio de impulsividad (actuar sobre una irreversible sin esperar). **Lo que NO mide
(prohibido afirmar):** "mejores decisiones vitales/financieras", "menos impulsivo clínicamente",
autocontrol global. La señal de impulsividad/cautela es la **más limpia del módulo** cuando el lock
premia la espera correcta — pero es *de esta tarea*.

| Sub-constructo | Operacionalización | Dependencia de RT |
|---|---|---|
| Discriminación rev/irrev | `selectedReversible == isReversible` por tipo | Ninguna (accuracy) |
| Control inhibitorio | `respondedBeforeLock` en Type-1 ⇒ violación | Umbral de lock (epoch ⇒ degradado en bordes) |
| Cautela honesta | espera correcta del lock | NUNCA tratada como lentitud (λ_irrev=0.15) |

---

## 2. Métrica primaria

`accuracyTriage` (reversible vs irreversible, **separada por tipo**) **+** `lockViolationRate` (proxy de
impulsividad: fracción de Type-1 en que `respondedBeforeLock == true`). Ambas son accuracy-based y NO
dependen del RT absoluto.

**Scoring compuesto [INFERIDO del audit E2, pesos 0.45/0.35/0.20]:**

```kotlin
// POR_CONFIRMAR_EN_REPO: ReversibilityScorer (probable decisiones/domain/scoring/)
fun scoreTriage(s: TriageSummary): ScoreResult {
    // GATE 0 — min-n por tipo (null != 0)
    if (s.nReversible < MIN_N_PER_TYPE || s.nIrreversible < MIN_N_PER_TYPE) {
        return ScoreResult.calibrating(reason = INSUFFICIENT_N)   // primarySignal = null
    }
    // GATE 1 — AGRS real (entropía de swipe) debe estar ACTIVO; si neutralizado -> no VALID
    if (!s.agrsFedRealSwipeEntropy) {
        return ScoreResult.calibrating(reason = ANTIGAMING_COVERAGE_ABSENT)
    }
    // GATE 2 — AGRS disparado invalida la sesión (no la puntúa a la baja: la descarta)
    if (s.agrsRolling > AGRS_INVALID_SESSION_THRESHOLD /* 0.75 */) {
        return ScoreResult.invalidated(reason = AGRS_OVER_THRESHOLD)
    }
    val accuracy = (s.accReversible + s.accIrreversible) / 2.0           // balanceada por tipo
    val impulse  = 1.0 - s.lockViolationRate                              // alto = cauto
    val efficiency = s.meanIedReversibles                                // IED solo en reversibles
    val raw = W_ACC * accuracy + W_IMP * impulse + W_EFF * efficiency    // 0.45/0.35/0.20
    return ScoreResult.valid(
        primarySignal = Signal(key = TRIAGE_ACCURACY, value = raw, unit = RATIO_0_1),
        preliminary = !s.crossSessionAggregated || s.confidence < ROBUST_CONFIDENCE,
    )
}
```

**Invalidaciones (cada trial):** `respondedBeforeLock` en Type-1 cuenta como **violación, no como acierto
rápido**; `RT < 400 ms` invalida el trial (respuesta no procesada); swipe-constante (entropía baja);
`FRS > 0.20`. La **espera honesta correcta NUNCA** se trata como lentitud vía IES.

---

## 3. Métricas secundarias

Solo feedback interno; **jamás** actualizan perfil (todas RT-críticas o diagnósticas):

| Métrica | Definición | Por qué NO al perfil |
|---|---|---|
| `rtMedianReversibles` | mediana RT de HITs reversibles | RT-crítica, epoch ⇒ `UNTRUSTED_TIMEBASE` |
| `IED` (rev/irrev) | penaliza demora λ=0.8 rev / 0.15 irrev | usa RT epoch |
| `delayPenaltyAcum` | penalización de demora acumulada | feedback de cautela |
| omisión vs comisión | separadas (no respondió vs respondió mal) | diagnóstico |
| confianza declarada | autoinforme opcional | ruidoso |

**Regla:** `RT < 400 ms` invalida el trial completo (no solo la métrica RT). El RT del lock medido en
epoch puede clasificar mal "antes/después del lock" en los bordes ⇒ marcar la **precisión del umbral
como degradada** en los metadatos del summary.

---

## 4. Gates de calidad (precondición de listado)

1. **AGRS HOY NEUTRALIZADO — arreglarlo es precondición de profile-update [HECHO].** Los holders pasan
   `spatialEntropy=1.0` y `isDialDefault=false` hardcodeados (`ReversibilityTriageStateHolder` :73-74,
   POR_CONFIRMAR_EN_REPO líneas exactas). Hay que **computar la entropía real del swipe** (recorrido del
   gesto / lado elegido) y alimentarla al término `entropyFactor` del AGRS (`ScoringEquations.calculateAGRS`,
   peso 0.3, hoy muerto).

   ```kotlin
   // Entropía de swipe real (anti "siempre el mismo lado")
   fun swipeEntropy(history: List<SwipeSide>): Double {
       if (history.size < MIN_SWIPES_FOR_ENTROPY) return 1.0   // neutral hasta tener muestra
       val pL = history.count { it == LEFT }.toDouble() / history.size
       val pR = 1.0 - pL
       return if (pL == 0.0 || pR == 0.0) 0.0                  // determinista -> entropía 0 -> contamina AGRS
              else -(pL * log2(pL) + pR * log2(pR))            // [0,1]
   }
   ```

2. **min-n = 10/tipo** (reversible/irreversible) con `null`/`CALIBRATING` honesto. n por debajo ⇒
   `INSUFFICIENT_N`, primarySignal `null`.
3. **Marcadores de coste ROTAN cross-sesión** (impide memorizar "icono X = irreversible"). Se mide
   *derivación de coste*, no reconocimiento de icono.
4. **`RT < 400 ms` invalida el trial** (respuesta no procesada). **`seed ≠ 0`** + rotación de banco.
5. **Lock-respetado PREMIADO**, nunca tratado como lentitud (la UI no muestra "lento" en irreversibles;
   λ_irrev=0.15 ya lo cubre).
6. **Timebase del lock = `UNTRUSTED_TIMEBASE`** (epoch): la clasificación rev/irrev es válida con timebase
   débil; la precisión del umbral del lock queda degradada y el RT no actualiza perfil.

**Tabla de estados (jugable ≠ actualiza-perfil):**

| Estado | Disparador | Jugable | updatesProfile | Copy |
|---|---|---|---|---|
| `VALID` | n≥10/tipo, AGRS real activo<0.75, RT≥400, marcadores rotando | ✅ | ✅ | "señal de práctica de triaje" |
| `CALIBRATING` | n<10/tipo · AGRS no alimentado · marcadores sin rotación testeada | ✅ | ❌ | "calibrando" (`—`) |
| `INVALIDATED` | AGRS>0.75 · RT<400 masivo | ✅ | ❌ | "sesión no contabilizada" |
| `SUSPECTED_GAMING` | swipe-constante (entropía baja) real disparado | ✅ | ❌ | neutro, sin acusar |

---

## 5. Anti-gaming

| Ataque | Mecanismo | Detector (estado real) |
|---|---|---|
| `RevealAllNoRead` / swipe-constante | siempre el mismo lado | entropía de swipe baja ⇒ AGRS `SUSPECTED_GAMING` — **[POR IMPLEMENTAR]** (hoy neutralizado) |
| `ConstantSideSwiper` | IZQ/DER mecánico sin leer | misma entropía baja ⇒ contamina AGRS |
| `BankMemorizer` (marcador fijo) | memoriza icono↔tipo | **rotación de marcadores** rompe la estrategia — **[POR IMPLEMENTAR]** |
| `SpeedAccuracyHacker` | confirma <400ms | `RT < 400 ms` invalida el trial — **[INFERIDO falta]** (hoy `coerceAtLeast(0)`) |
| `PreLockRusher` | responde antes del lock en Type-1 | cuenta como **violación**, no acierto rápido — **[HECHO parcial]** (`validateLockDuration`) |

> **Regla #2 explícita:** mientras el AGRS reciba `spatialEntropy=1.0` fijo, los detectores 1-3 son
> **diseño, no cobertura**. El ejercicio NO puede ser `VALID` con confianza alta hasta que el detector de
> swipe-constante esté implementado + testeado con `ConstantSideSwiper`/`RevealAllNoRead`.

---

## 6. Banco / generador

Escenarios de acción **procedurales** (`ReversibilityEngine.generateScenario`: `reversible`/`delayCost`/
`lockMs` templated) con marcador de coste **rotado**. **[HECHO]** HOY = 6 fijos `seed=0L`, sin rotación.
**Rediseño:**

- `seed` por sesión (no `0L`); pool ≥30 escenarios/celda de dificultad.
- **Rotación visual de marcadores entre sesiones** (medir derivación de coste, no iconos memorizados).
- `exposure-tracking` por escenario; partición de **holdout**.
- Sin inter-rater (la reversibilidad es **objetiva por parámetros del escenario**), pero el escenario
  debe ser **inequívoco** (escenarios cortos y neutros; sin conflicto de atributos cerca de 0.5).

---

## 7. UI / copy permitido

- "Práctica de triaje de reversibilidad en esta tarea"
- "¿Esta acción se puede deshacer?"
- "Respetar la ventana de revisión" · semáforo no culpabilizante · lock = anillo de progreso funcional
- "Calibrando" / "estimación preliminar" / "no actualizado por calidad de medición"

---

## 8. Copy prohibido (bloquear en copy-scanner)

"tomas mejores decisiones", "decides mejor en la vida/finanzas", "serás más prudente en la vida real",
"reduce tu impulsividad", "menos impulsivo clínicamente", "controla tus impulsos", "autocontrol",
"apuesta/fichas/payoff/mercado", cualquier transfer a conducta real / autocontrol global / CI.

> **[POR_CONFIRMAR_EN_REPO]** No existe copy-scanner para `decisiones/**` (solo arena y emotions tienen
> gate de build). Crear `decisionesTerminologyScanner` espejo de `emotionsTerminologyScanner` enganchado
> a `check` es parte del DoD.

---

## 9. Criterio EXACTO para actualizar perfil

`VALID` ⟺ **TODAS**: (a) **AGRS real activo** (entropía de swipe alimentada, no `1.0`); (b)
`nEffective ≥ 10/tipo`; (c) marcadores rotando (banco no contaminado); (d) `RT < 400 ms` excluido;
(e) `agrsRolling ≤ 0.75`. Es el **único v1 marcado `canUpdateProfile=yes`** porque la señal es de
**clasificación, no RT** — pero el AGRS neutralizado debe arreglarse **antes** de listar. El componente
RT (lock, IED) permanece bajo `UNTRUSTED_TIMEBASE` y nunca actualiza perfil. `preliminary=false` solo con
agregación inter-sesión + `confidence ≥ 0.7`.

---

## 10. Tests obligatorios (enumerados)

**Unit (`ReversibilityScorerTest` / `SwipeEntropyTest`):**
1. `accuracyTriage_separatedByType_computesPerTypeMean`
2. `swipeEntropy_uniformSides_returnsNearOne`
3. `swipeEntropy_constantSide_returnsZero`
4. `swipeEntropy_belowMinSwipes_returnsNeutralOne`
5. `entropyFedToAgrs_lowEntropy_raisesAgrs`
6. `rtUnder400ms_invalidatesTrial`
7. `respondedBeforeLock_inType1_countsAsViolationNotHit`
8. `lockRespected_neverPenalizedAsSlowness`
9. `minN_below10PerType_returnsCalibratingPrimaryNull`
10. `nullNeverZero_calibratingSummaryHasNullPrimary`

**Adversarial (`ReversibilityAdversarialTest`):**
11. `constantSideSwiper_lowEntropy_yieldsSuspectedGaming`
12. `revealAllNoRead_contaminatesAgrs`
13. `bankMemorizer_fixedMarker_rotationBreaksStrategy`
14. `speedAccuracyHacker_under400_trialsDiscarded`
15. `preLockRusher_allType1_lockViolationRateMaxNotRewarded`

**Integración / regresión (`ReversibilityIntegrationTest`):**
16. `agrsNeutralized_hardcodedEntropy_failsRed` (regresión del bug actual: `spatialEntropy=1.0` ⇒ test ROJO)
17. `calibrating_whenNPerTypeBelow10`
18. `rtComponent_neverFlowsToProfile_underUntrustedTimebase`
19. `markerRotation_crossSession_changesIconAssignment`
20. `seedNonZero_producesDistinctSequences` (vs `seed=0` legacy)

**Property-based (Kotest `Arb`, hoy ausente en mindboost):**
21. `forAll_anySwipeHistory_entropyInZeroToOne`
22. `forAll_anyValidSummary_updatesProfileImpliesStateValid`

---

## 11. Riesgo residual

Si el escenario de coste es memorizable pese a la rotación, la accuracy infla (mitiga: rotación de
marcadores + exposure-tracking + holdout). Confound de **comprensión lectora** del escenario (mitiga:
escenarios cortos/neutros). **Precisión del umbral del lock degradada por epoch-clock** en bordes
(aceptado: RT no actualiza perfil; la clasificación rev/irrev no depende del umbral fino). Banda de tipos
cerca de 0.5 frustra/abandona (mitiga: dificultad adaptativa que no fuerce ambigüedad).

---

## 12. Definition of Done

- AGRS recibe **entropía real de swipe** (regresión del hardcode `spatialEntropy=1.0`, test 16 ROJO→VERDE).
- Marcadores **rotan** cross-sesión con test (13, 19).
- `RT < 400 ms` invalida trial (test 6, 14); `seed ≠ 0` (test 20).
- min-n=10/tipo + `null`/`CALIBRATING` en summary (tests 9, 10, 17).
- Consume `INFRA_01` (ScoreResult/máquina de estados), `INFRA_03` (banco/rotación), `INFRA_06`
  (copy-scanner decisiones) — **[POR_CONFIRMAR_EN_REPO]** rutas de INFRA.
- copy-scanner de decisiones VERDE (sin léxico de apuesta/transfer); compila ambos targets
  (`:shared:compileAndroidMain` + `:shared:compileKotlinDesktop`); Detekt VERDE.
- RT (lock/IED) bajo `UNTRUSTED_TIMEBASE`, fuera del scoring de perfil (test 18).
