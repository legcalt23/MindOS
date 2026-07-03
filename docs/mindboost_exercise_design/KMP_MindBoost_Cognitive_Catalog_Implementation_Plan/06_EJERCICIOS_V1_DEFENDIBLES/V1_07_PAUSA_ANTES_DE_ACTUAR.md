# V1_07 — Pausa antes de actuar (Go/No-Go afectivo)

**Módulo:** Emociones · **Verdict:** REVISE · **Prioridad:** P1 · **Profile-update:** sí (tras gates) · **TransferLabel:** NEAR_TASK_PRACTICE / `TASK_ONLY` · **Absorbe:** EM8 (anti-impulso), EM12 (anticipaciones)

> **Mapa hecho / inferido / por_confirmar.** El paradigma go/no-go afectivo EXISTE y es jugable: EM1
> está wired (`AffectiveGoNoGo`, `EmotionGoNoGoField`, `classifyGoNoGo`, `goNoGoMetrics`), ground-truth
> `04_7_Emociones_audit.md` EM1/EM8/EM12. **[HECHO]** = verificado en auditoría; **[INFERIDO]** =
> diseño coherente; **[POR_CONFIRMAR_EN_REPO]** = símbolo/ruta a verificar. Rutas probables bajo
> `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions/`.

> **Invariantes transversales que este ejercicio repite (no negociables):**
> - **null != 0.** `commissionScore` no computable ⇒ `null`/`CALIBRATING`/`unavailable`, nunca `0.0`.
>   Comisión 0% por "no responder a nada" NO es "inhibición perfecta": es `INVALIDATED`.
> - **Cero claims de far-transfer, NI para negarlos.** Nada de "regulación emocional", ni siquiera
>   "esto no regula tus emociones". Copy dentro de la tarea.
> - **Anti-gaming solo cuenta si está implementado + testeado con adversarios.** El **suelo de
>   go-accuracy NO existe hoy** (`goAccuracy`/MISS no se enforce en `goNoGoMetrics`) ⇒ hasta
>   implementarlo y testearlo con `RespondToNothing`, no es cobertura.
> - **Banco pequeño / no-validado bloquea perfil del componente que dependa del contenido.** `PAUSA_WORDS`
>   (24) es **distractor** (no determina la respuesta) ⇒ verificable, pero pequeño; falta test de
>   paridad tono↔respuesta.
> - **Enfoque/Velocidad sin profile-update hasta timebase+jank reales.** Misma doctrina aquí: la
>   comisión (accuracy) actualiza perfil; **todo RT** (RT_MEDIAN, ANTICIPATIONS, coste de interferencia)
>   va bajo `UNTRUSTED_TIMEBASE` y NUNCA actualiza perfil (emotions mide con `monotonicClock.nowNanos()`,
>   monotónico pero **NO frame-accurate**; no hay medición de jank).

---

## 1. Objetivo cognitivo realista

Práctica de **control inhibitorio (comisión)** en contexto de **distractor afectivo leve**. NO es
terapia, NO mide regulación emocional ni infiere estado interior. El distractor afectivo es **leve por
diseño** (frontera de seguridad: techo HARD, sin contenido aversivo). La inhibición la define el
**acierto** (no responder a un no-go), no el RT.

**Honesto:** se reporta como **control inhibitorio en contexto afectivo**, no como regulación. El
distractor leve podría no generar la carga afectiva pretendida (mide control inhibitorio "frío"); esto
se declara, no se oculta.

| Sub-constructo | Operacionalización | Al perfil |
|---|---|---|
| Inhibición de comisión | `COMMISSION_RATE` = FA / total no-go | **Sí** (accuracy, tras gates) |
| Atención sostenida (go) | `goAccuracy` (suelo invalidante) | Gate, no score |
| Velocidad go | `RT_MEDIAN` go-HITs | No (RT-crítica) |
| Anticipación (EM12) | `ANTICIPATIONS` (RT<350ms) | No (RT-crítica, secundaria) |

---

## 2. Métrica primaria

`COMMISSION_RATE` (responder a un no-go) — **accuracy-based de inhibición**, computable **sin timebase
autoritativa**.

```kotlin
// POR_CONFIRMAR_EN_REPO: goNoGoMetrics (emotions/domain/metrics/DefaultEmotionMetricsEngine.kt)
fun commissionScore(s: GoNoGoSummary): EmotionMetricValue {
    // GATE A — suelo de go-accuracy INVALIDANTE (hoy AUSENTE) : null != 0
    if (s.goAccuracy < GO_ACCURACY_FLOOR /* 0.6 */) {
        return EmotionMetricValue.invalidated(reason = GO_ACCURACY_FLOOR_NOT_MET)
        // "no responder a nada" => comisión 0 trivial => INVALIDATED, NO "inhibición perfecta"
    }
    // GATE B — min no-go
    if (s.nNoGo < MIN_NOGO /* 6 */) {
        return EmotionMetricValue.unavailable(reason = INSUFFICIENT_NOGO)  // CALIBRATING
    }
    // GATE C — paridad tono<->respuesta (el lado afectivo no debe sesgar la respuesta motora)
    if (!s.toneResponseParityVerified) {
        return EmotionMetricValue.unavailable(reason = TONE_PARITY_UNVERIFIED)
    }
    val commission = s.falseAlarmsNoGo.toDouble() / s.nNoGo
    val score = 1.0 - commission                                   // alto = mejor inhibición
    return EmotionMetricValue.confident(
        key = COMMISSION_RATE, value = score, n = s.nNoGo,
        confident = s.crossSessionAggregated && s.confidence >= ROBUST_CONFIDENCE,
    )
}
```

**Regla clave:** la **comisión (accuracy) actualiza perfil**; el RT **no** (sin timebase). Absorbe **EM8**
(anti-impulso = mismo paradigma con `NO_GO_RATIO` más alto, p.ej. 0.40) y **EM12** (anticipaciones =
secundaria de EM1, NUNCA primaria sin timebase).

---

## 3. Métricas secundarias

Solo feedback interno; **bajo `UNTRUSTED_TIMEBASE`**, jamás perfil:

| Métrica | Definición | Por qué NO al perfil |
|---|---|---|
| `RT_MEDIAN` | mediana RT go-HITs | RT-crítica; jitter de frame contamina |
| `ANTICIPATIONS` | RT < floor 350 ms (EM12) | RT-crítica; jitter ≈ ventana de anticipación |
| `AFFECTIVE_INTERFERENCE_COST` | RT congruente vs incongruente | RT-crítica; saliencia ≠ control |
| `goAccuracy` | aciertos go / total go | Gate invalidante, no score |
| `ABANDONMENT` | sesión incompleta | Señal de **seguridad**; abandono NO penaliza |

---

## 4. Gates de calidad

1. **Suelo `goAccuracy ≥ 0.6` INVALIDANTE [POR IMPLEMENTAR — hoy ausente].** Si el usuario "no responde
   a nada", la comisión es 0% trivialmente ⇒ `INVALIDATED` (no "inhibición perfecta"). Es el gate
   anti-gaming nº1 de este ejercicio.
2. **`MIN_NOGO = 6`** (n mínimo de no-go para estimar comisión) **[HECHO]**; si no, `CALIBRATING`.
3. **Métricas RT bajo `UNTRUSTED_TIMEBASE`** [HECHO doctrina]: no actualizan perfil sin timebase
   autoritativa. Añadir flag `untrustedTimebase` a `EmotionMetricValue` **[POR_CONFIRMAR_EN_REPO]**.
4. **Distractor afectivo leve only** (techo HARD); **abandono no penaliza** (señal de seguridad) **[HECHO]**.
5. **Paridad tono↔respuesta** verificada (que el lado afectivo no sesgue la respuesta motora) **[POR IMPLEMENTAR]**.

**Tabla de estados (jugable ≠ actualiza-perfil):**

| Estado | Disparador | Jugable | updatesProfile | Copy |
|---|---|---|---|---|
| `VALID` | goAcc≥0.6 · nNoGo≥6 · paridad OK · comisión confident | ✅ | ✅ | "señal de práctica de control de respuesta" |
| `INVALIDATED` | goAcc<0.6 (RespondToNothing) | ✅ | ❌ | "sesión no contabilizada" (sin culpa) |
| `CALIBRATING` | nNoGo<6 · paridad sin verificar | ✅ | ❌ | "calibrando" (`—`) |
| `ABANDONED_SAFE` | abandono | ✅ | ❌ | "puedes parar cuando quieras" |

---

## 5. Anti-gaming

| Ataque | Mecanismo | Detector (estado real) |
|---|---|---|
| `RespondToNothing` (never-respond) | comisión 0 trivial | suelo `goAccuracy<0.6` ⇒ `INVALIDATED` — **[POR IMPLEMENTAR]** |
| `ButtonSpammer` (responde a todo) | comisión alta | comisión alta **no se premia** (1−commission baja) — **[HECHO por construcción]** |
| `RhythmicAnticipator` | tap rítmico justo sobre el floor | floor 350 ms mitiga parcial; **sin detector de cadencia** — **[INFERIDO falta]** |
| `ToneBiasExploiter` | sesga la respuesta por el tono | **paridad tono↔respuesta** testeada — **[POR IMPLEMENTAR]** |

> **Regla #2 explícita:** hoy el anti-gaming real = filtro de anticipación (floor 350 ms) + comisión sobre
> denominador no-go + abandono. El **suelo go-accuracy** y la **paridad de tono** son los dos detectores
> que faltan; hasta estar implementados + testeados con `RespondToNothing`/`ToneBiasExploiter`, NO cuentan
> como cobertura y el ejercicio queda REVISE.

---

## 6. Banco / generador

Estímulos go/no-go con **distractor afectivo leve rotado**; el `NO_GO_RATIO` es el **eje de dificultad**
(EM1 base vs EM8 alto). Sin contenido aversivo (frontera de seguridad). `PAUSA_WORDS` (24) **[HECHO]** =
distractor, **no determina la respuesta** (verificable), no memorizable como contenido pero pequeño;
rotación por seed OK. Falta **test de paridad tono↔respuesta**.

---

## 7. UI / copy permitido

- "Práctica de control de respuesta en esta tarea"
- "Frena cuando toque" · "Pausa breve antes de responder" · "respuestas impulsivas (en esta tarea)"
- "Puedes parar cuando quieras" · "Calibrando" · "estimación preliminar"

---

## 8. Copy prohibido (bloquear en copy-scanner)

"regulación emocional", "controla tus emociones/ansiedad", "tolerancia a la frustración", "inteligencia
emocional", "autocontrol", "control de impulsos", "impulsividad" como rasgo, "menos impulsivo", "en la
vida real". **[HECHO]** `emotionsTerminologyScanner` ya bloquea léxico clínico sobre
`features/mindboost/emotions/**`; falta **allowlist de NOMBRES DE CAPACIDAD para UI** (un `displayName`
neutro de lexema pero que prometa un rasgo debe bloquearse) — **[POR IMPLEMENTAR, P0 transversal]**.

---

## 9. Criterio EXACTO para actualizar perfil

`VALID` ⟺ **TODAS**: (a) `goAccuracy ≥ 0.6` (suelo no disparado); (b) `nNoGo ≥ 6`; (c) paridad
tono↔respuesta verificada; (d) `COMMISSION_RATE` confident. La **comisión (accuracy)** actualiza perfil;
el **RT no** (sin timebase). Absorbe EM8 (anti-impulso, mismo paradigma con otro ratio) y EM12
(anticipaciones, degradada a secundaria). `preliminary=false` solo con agregación inter-sesión +
`confidence ≥ 0.7`. Las barras RT **nunca** al perfil hoy.

---

## 10. Tests obligatorios (enumerados)

**Unit (`GoNoGoMetricsTest`):**
1. `commissionRate_equalsFalseAlarmsOverTotalNoGo`
2. `goAccuracyFloor_below0_6_invalidatesBlock`
3. `minNoGo_below6_returnsCalibratingUnavailable`
4. `nullNeverZero_invalidatedHasNoZeroScore`
5. `commissionScore_equalsOneMinusCommission`
6. `toneParityUnverified_returnsCalibrating`
7. `em8_isEm1WithHigherNoGoRatio_sameMetric` (golden de fusión)

**Property-based (Kotest `Arb`):**
8. `forAll_anyResponseSequence_commissionInZeroToOne`

**Adversarial (`GoNoGoAdversarialTest`):**
9. `respondToNothing_goAccuracyZero_yieldsInvalidatedNotPerfect`
10. `buttonSpammer_highCommission_notRewarded`
11. `rhythmicAnticipator_belowFloor_filteredAsAnticipation`
12. `toneBiasExploiter_failsParityTest`

**Integración / timebase (`GoNoGoIntegrationTest`):**
13. `rtBars_neverFlowToProfile_underUntrustedTimebase`
14. `abandonment_doesNotReduceScore`
15. `calibrating_whenNNoGoBelow6`
16. `anticipations_em12_isSecondaryNotPrimary` (degradación verificada)

**Copy / safety:**
17. `copyScanner_blocksProhibitedLexemes_onDisplayName`
18. `capabilityNameAllowlist_rejectsTraitPromisingName`

---

## 11. Riesgo residual

El distractor afectivo leve podría no generar la carga afectiva pretendida ⇒ mide control inhibitorio
"frío" (se reporta honestamente como control inhibitorio en contexto afectivo, no como regulación). La
comisión es **robusta sin RT**; el riesgo real es **vender las barras RT como perfil** sin timebase
(mitiga: gate `UNTRUSTED_TIMEBASE` + test 13). Banco pequeño de distractor (mitiga: rotación + paridad).

---

## 12. Definition of Done

- **Suelo `goAccuracy ≥ 0.6` invalidante** implementado + testeado (tests 2, 9).
- **`MIN_NOGO`** enforce + `CALIBRATING` honesto (tests 3, 15).
- **Paridad tono↔respuesta** testeada (tests 6, 12).
- **EM8 y EM12 fusionados** en EM1 (EM8 = nivel de dificultad por `NO_GO_RATIO`; EM12 = secundaria)
  (tests 7, 16).
- Gate `UNTRUSTED_TIMEBASE` en TODAS las barras RT (test 13); abandono no penaliza (test 14).
- Consume `INFRA_01` (ScoreResult/estados), `INFRA_03` (banco/rotación), `INFRA_06` (copy-scanner +
  allowlist de capacidad) — **[POR_CONFIRMAR_EN_REPO]**.
- copy-scanner VERDE + allowlist de nombres de capacidad (tests 17, 18); compila ambos targets; Detekt VERDE.
