# V1_08 — Recuperación tras error

**Módulo:** Emociones · **Verdict:** REVISE · **Prioridad:** P1 · **Profile-update:** sí (tras gates) · **TransferLabel:** NEAR_TASK_PRACTICE / `TASK_ONLY` · **Absorbe:** EM11 (continuidad), EM15 (recuperación encadenada)

> **Mapa hecho / inferido / por_confirmar.** EM2 está wired (`TwoAltPerceptual`, `classifyChoice`,
> `postErrorRecovery`, `choiceMetrics`), ground-truth `04_7_Emociones_audit.md` EM2/EM11/EM15.
> **[HECHO]** = verificado en auditoría; **[INFERIDO]** = diseño coherente; **[POR_CONFIRMAR_EN_REPO]** =
> símbolo/ruta a verificar. Rutas probables bajo
> `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/emotions/`.

> **Invariantes transversales que este ejercicio repite (no negociables):**
> - **null != 0.** `POST_ERROR_RECOVERY` no computable (pares<3) ⇒ `unavailable`/`CALIBRATING`, jamás
>   `0.0`. (Hoy `EmotionMetricValue.unavailable` lo respeta cuando pares<3.)
> - **Cero claims de far-transfer, NI para negarlos.** Nada de "resiliencia", ni "te recuperas mejor en
>   la vida"; tampoco la negación. Copy dentro de la tarea.
> - **Anti-gaming solo cuenta si está implementado + testeado con adversarios.** La **exclusión de
>   anticipaciones del set de errores NO existe hoy** (`postErrorRecovery` define error = `INCORRECT` sin
>   excluir respuestas <floor) ⇒ hasta implementarla y testearla con `AnticipationAsError`, no es cobertura.
> - **Banco pequeño / no-validado bloquea perfil.** El banco es **procedural** (`TwoAltPerceptual`, no
>   memorizable), pero la **suficiencia ≥3 pares** debe garantizarse por el **peor seed**, o `CALIBRATING`.
> - **Enfoque/Velocidad sin profile-update hasta timebase+jank reales.** El **componente accuracy** de la
>   recuperación es robusto y actualiza perfil; el **componente slowing (RT post-error)** es RT-crítico ⇒
>   bajo `UNTRUSTED_TIMEBASE`, **fuera del perfil** sin timebase autoritativa.

---

## 1. Objetivo cognitivo realista

Práctica de **recuperación funcional tras un error**: mantener accuracy en los trials que siguen a un
fallo. La métrica es **accuracy-recovery robusta**. NO mide resiliencia emocional ni autocontrol vital.
Composite condicional: la accuracy-recovery es robusta sin RT; el *slowing* requiere timebase.

| Sub-constructo | Operacionalización | Al perfil |
|---|---|---|
| Recuperación de accuracy | accuracy en N trials post-error vs baseline | **Sí** (accuracy) |
| Post-error slowing | RT post-error vs baseline | No (RT-crítica) |
| Densidad de errores | errores genuinos / total | Diagnóstico |
| Recuperación a 1/2/3 trials | curva de recuperación | Diagnóstico |

**Honesto:** un error es una **respuesta-en-ventana incorrecta**; una **anticipación NO es un error de
proceso** y no debe abrir una ventana post-error espuria.

---

## 2. Métrica primaria

`POST_ERROR_RECOVERY` — **componente de accuracy** (accuracy en los N trials post-error vs baseline). El
componente de *slowing* (RT post-error) es RT-crítico ⇒ **fuera del perfil** sin timebase.

```kotlin
// POR_CONFIRMAR_EN_REPO: postErrorRecovery (emotions/domain/metrics/DefaultEmotionMetricsEngine.kt)
fun postErrorRecovery(trials: List<RecordedEmotionTrial>): EmotionMetricValue {
    // GATE A — error GENUINO excluye anticipaciones (hoy AUSENTE: usa INCORRECT a secas)
    val genuineErrors = trials.withIndex().filter { (_, t) ->
        t.outcome == INCORRECT && t.latencyMs >= ANTICIPATION_FLOOR_MS   // <- exclusión clave
    }
    val pairs = genuineErrors.mapNotNull { (i, _) -> trials.getOrNull(i + 1)?.let { i to it } }
    // GATE B — suficiencia >=3 pares (garantizada por el PEOR seed) : null != 0
    if (pairs.size < MIN_POST_ERROR /* 3 */) {
        return EmotionMetricValue.unavailable(reason = INSUFFICIENT_POST_ERROR_PAIRS)  // CALIBRATING
    }
    val postErrAcc = pairs.count { (_, next) -> next.outcome == CORRECT }.toDouble() / pairs.size
    val baseline = baselineAccuracy(trials)
    val recovery = (postErrAcc - baseline).coerceIn(-1.0, 1.0)
    // composite: penaliza impulsividad SOLO si no recuperó; slowing fuera del perfil (timebase)
    val impulsive = fastIncorrectShare(pairs)
    val score = (postErrAcc - 0.5 * impulsive).coerceIn(0.0, 1.0)
    return EmotionMetricValue.confident(
        key = POST_ERROR_RECOVERY, value = score, n = pairs.size,
        confident = pairs.size >= MIN_POST_ERROR && crossSessionAggregated,
    )
}
```

---

## 3. Métricas secundarias

Feedback interno; el slowing **bajo `UNTRUSTED_TIMEBASE`**, jamás perfil:

| Métrica | Definición | Por qué NO al perfil |
|---|---|---|
| `POST_ERROR_SLOWING` | RT post-error − RT baseline | RT-crítica; jitter de frame |
| densidad de errores | errores genuinos / total | diagnóstico |
| recuperación a 1/2/3 | accuracy a +1/+2/+3 del error | diagnóstico de curva |
| `ACCURACY` global | aciertos / total | control |
| `RT_CV` | variabilidad de RT | RT-crítica |
| `ABANDONMENT` | sesión incompleta | seguridad; no penaliza |

---

## 4. Gates de calidad

1. **Error genuino [POR IMPLEMENTAR — hoy ausente] = respuesta-en-ventana incorrecta, EXCLUYENDO
   anticipaciones** (`latencyMs ≥ ANTICIPATION_FLOOR_MS`). Una anticipación no es un error de proceso y no
   abre ventana post-error.
2. **Suficiencia ≥ 3 pares (error→post) garantizada por el PEOR seed [POR IMPLEMENTAR]**; si no,
   `CALIBRATING`. (EM11 ya planta `MIN_HARD`/cada-3 ⇒ su lógica de suficiencia se porta a EM2 base.)
3. **Término *slowing* bajo gate timebase** (no perfil sin timebase autoritativa) **[HECHO doctrina]**.
4. **La dificultad adaptativa NUNCA fuerza fracaso deliberado** (frontera de seguridad: inducir errores
   roza frustración; densidad inducida por dificultad, no por sabotaje).

**Tabla de estados (jugable ≠ actualiza-perfil):**

| Estado | Disparador | Jugable | updatesProfile | Copy |
|---|---|---|---|---|
| `VALID` | ≥3 pares genuinos · accuracy-recovery confident | ✅ | ✅ | "señal de práctica de recuperación" |
| `CALIBRATING` | <3 pares (peor seed) | ✅ | ❌ | "calibrando" (`—`) |
| `INVALIDATED` | accuracy global basura (MinEffort) | ✅ | ❌ | "sesión no contabilizada" |
| `ABANDONED_SAFE` | abandono | ✅ | ❌ | "puedes parar cuando quieras" |

---

## 5. Anti-gaming

| Ataque | Mecanismo | Detector (estado real) |
|---|---|---|
| `MinEffort` (esfuerzo mínimo) | accuracy global baja, no recuperación | accuracy global baja ⇒ no recuperación alta — **[INFERIDO]** |
| `AnticipationAsError` | respuestas <floor inflan el conteo de errores | **exclusión de anticipaciones** del set de errores — **[POR IMPLEMENTAR]** |
| `FastRandomFlooder` | rápido-aleatorio infla densidad de errores "baratos" | composite penaliza impulsividad + exclusión de anticipaciones — **[PARCIAL]** (composite sí, exclusión no) |
| `PatternRepeater` | patrón fijo de respuesta | detector de patrón — **[INFERIDO falta]** |

> **Regla #2 explícita:** hoy `postErrorRecovery` cuenta **cualquier** `INCORRECT` como error, incluidas
> anticipaciones ⇒ una respuesta rápida-incorrecta abre ventana post-error espuria. Hasta excluir
> anticipaciones (test `AnticipationAsError`) el anti-gaming NO es cobertura y el ejercicio queda REVISE.

---

## 6. Banco / generador

**Procedural** (`TwoAltPerceptual`, harder plantado por seed) **[HECHO]**. Densidad de errores inducida
por **dificultad adaptativa** (sin forzar fracaso = frontera de seguridad). Rotación. Sin validación de
discriminabilidad psicométrica (el `discriminability` es gap perceptual, no IRT). **EM11** garantiza la
suficiencia post-error (planta `MIN_HARD`/cada-3) ⇒ su densidad de harder debe ser el **default de EM2**.

---

## 7. UI / copy permitido

- "Práctica de recuperación tras fallar en esta tarea"
- "Sigue tras un error" · "volver a tu rendimiento tras un error" · "contra tu propio baseline"
- "Calibrando" · "estimación preliminar" · sin culpa

---

## 8. Copy prohibido (bloquear en copy-scanner)

"resiliencia emocional", "resiliencia", "tolerancia a la frustración", "te recuperas mejor en la vida",
"regulación emocional", "no te rindas" (motivacional), "autoestima", "espiral"/"no entres en bucle". Y la
**negación** de cualquier far-transfer también está prohibida. **[POR_CONFIRMAR_EN_REPO]** allowlist de
nombres de capacidad para UI (P0 transversal).

---

## 9. Criterio EXACTO para actualizar perfil

`VALID` ⟺ **TODAS**: (a) **error genuino** (sin anticipaciones); (b) **≥3 pares** por el peor seed;
(c) **componente accuracy** (no RT) confident. Absorbe **EM11** (continuidad de feedback) y **EM15**
(recuperación encadenada): mismos `POST_ERROR_RECOVERY`, varían densidad ⇒ **un ejercicio con niveles**.
El slowing (RT) permanece `UNTRUSTED_TIMEBASE`, fuera del perfil. `preliminary=false` solo con agregación
inter-sesión + `confidence ≥ 0.7`.

---

## 10. Tests obligatorios (enumerados)

**Unit (`PostErrorRecoveryTest`):**
1. `genuineError_excludesAnticipations`
2. `recovery_equalsPostErrorAccuracyMinusBaseline`
3. `sufficiency_below3Pairs_returnsCalibratingUnavailable`
4. `nullNeverZero_calibratingHasNoZeroValue`
5. `composite_penalizesImpulsivity_onlyWhenNotRecovered`
6. `slowingComponent_excludedFromProfileScore`
7. `em11_density_isEm2DefaultConfiguration` (golden de fusión)

**Property-based (Kotest `Arb`):**
8. `forAll_pairsAtLeast3_scoreInZeroToOne`

**Adversarial (`PostErrorAdversarialTest`):**
9. `anticipation_NOT_countedAsError`
10. `minEffort_yieldsLowAccuracy_notHighRecovery`
11. `fastRandomFlooder_cheapErrors_doNotInflateRecovery`
12. `patternRepeater_detectedOrNeutralized`

**Integración / suficiencia (`PostErrorIntegrationTest`):**
13. `slowingRT_neverFlowsToProfile_underUntrustedTimebase`
14. `sufficiency_guaranteedByWorstSeed_em11Density`
15. `calibrating_whenFewerThan3Pairs`
16. `abandonment_doesNotReduceScore`

---

## 11. Riesgo residual

Con pocos errores la métrica es **ruidosa** (de ahí la suficiencia ≥3 + `unavailable` honesto si no se
alcanza). **Inducir errores roza la frontera de frustración** ⇒ la dificultad adaptativa nunca fuerza
fracaso deliberado; la densidad de EM15 (errores encadenados) requiere **techo leve estricto** para no
inducir frustración. El componente accuracy es robusto; el riesgo es **vender el slowing como perfil** sin
timebase (mitiga: gate + test 13).

---

## 12. Definition of Done

- **Error genuino sin anticipaciones** + **suficiencia ≥3 pares** con tests (tests 1, 9, 14).
- **EM11/EM15 fusionados** en EM2 (densidad de harder = niveles de un mismo ejercicio) (tests 7, 14).
- **Slowing fuera del perfil** sin timebase (test 6, 13).
- Consume `INFRA_01` (ScoreResult/estados), `INFRA_06` (copy-scanner + allowlist de capacidad) —
  **[POR_CONFIRMAR_EN_REPO]**.
- copy-scanner VERDE (sin léxico de resiliencia/transfer ni su negación); abandono no penaliza (test 16);
  compila ambos targets; Detekt VERDE.
