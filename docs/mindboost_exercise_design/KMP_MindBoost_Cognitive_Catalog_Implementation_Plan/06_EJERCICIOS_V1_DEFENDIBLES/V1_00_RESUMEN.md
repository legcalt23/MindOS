# V1 — Resumen del catálogo defendible (versión detallada)

> **Invariantes del plan (se repiten en CADA ficha, no negociables):**
> 1. `null != 0` — una señal no computable es `null`/`CALIBRATING`, **jamás** `0`. La UI pinta `—`, nunca un cero que parezca bajo rendimiento.
> 2. **Enfoque y Velocidad NO actualizan perfil** hasta tener **timebase autoritativa + medición real de jank** (`INFRA_02`). Hoy `frameDropFraction=0.0`/`criticalFrameJitter=false` están *hardcodeados* (`SpeedSessionStateHolder.kt:259-260`): el gate `device_unreliable` es decorativo.
> 3. **Cero claims de far-transfer** — ni para afirmarlos NI para negarlos en copy de usuario (negar "no mejora tu inteligencia" introduce el lexema prohibido; usar lenguaje funcional neutro).
> 4. **Anti-gaming solo cuenta si está implementado + testeado con adversarios.** Un detector "diseñado" no cubre nada. RT-crítico sin detector real ⇒ `CALIBRATING`, nunca `VALID`.
> 5. **Banco pequeño / no validado bloquea perfil** (`BankIntegrity.SMALL_UNVALIDATED` o `CONTAMINATED`). Solo `PROCEDURAL` (generador por seed) o `OK` (validado) permiten `VALID`.
>
> Distinción de evidencia usada en este doc: **[hecho]** verificado en repo · **[inferido]** deducible del diseño · **[POR_CONFIRMAR_EN_REPO]** ruta probable, sin verificar.

---

## 1. Por qué EXACTAMENTE estos 9

El v1 se concentra en señales **accuracy-based o de clasificación**, donde el perfil **no depende de RT absoluto** y por tanto **no está bloqueado por el jank-gate/timebase** (el bloqueador P0 #1, [hecho] confirmado en `AUDIT_V2_GROUND_TRUTH.md`). El criterio de selección fue triple:

1. **La métrica primaria es accuracy o clasificación**, no latencia física → inmune a `TimingCriticality.RT_CRITICAL` y al jank-gate hardcodeado.
2. **Existe ya un motor de honestidad o un generador real** que lo respalde (no vaporware): Memoria tiene `ReliableChangeIndex`/`RealImprovementCriterion`/`PlateauDetector` [hecho]; Decisiones tiene AGRS real [hecho]; Emociones tiene abandono + `TransferLabel` real [hecho].
3. **El constructo no es redundante** con otro ya cubierto (se aplicó MERGE: EX6→EX5, EX8→EX4).

Memoria aporta **5** (es el módulo psicométricamente más honesto del repo). Decisiones y Emociones aportan **clasificación/inhibición** accuracy-based. Creatividad aporta el **único** diseño defendible sin IA (rareza×plausibilidad), y entra solo-calibrando.

**Enfoque y Velocidad NO entran a v1**: todos sus ejercicios son RT/frame-críticos → entran como **minijuegos jugables sin profile-update** hasta cerrar `INFRA_02` (timebase + jank reales). Ver [03_ROADMAP](../03_ROADMAP_FASES_P0_P1_P2_P3.md).

---

## 2. Tabla v1 maestra

| # | Ejercicio | Ficha | Módulo | Métrica primaria | Estado generador (repo) | Profile-update | Gates P0/P1 que faltan |
|---|---|---|---|---|---|---|---|
| 1 | Span Adaptativo Dual | [V1_01](V1_01_SPAN_ADAPTATIVO_DUAL.md) | Memoria | `WORKING_MEMORY_SPAN` | **EXISTE** (`Ex1SpanGenerator`) [hecho] | sí (tras gates) | staircase solo trials válidos; reading-span rotatorio; sonda UNTRAINED |
| 2 | Updating de Ranura | [V1_02](V1_02_UPDATING_RANURA.md) | Memoria | `k` efectivo + `INTRUSION_RATE` | **AUSENTE** (catálogo sin generador, `EX-EX2-MEM`) [hecho] | sí (tras gates) | generador con fillers; sonda 1-ranura; ortogonalidad vs n-back |
| 3 | Grid Espacial con Binding | [V1_03](V1_03_GRID_ESPACIAL_BINDING.md) | Memoria | `COWAN_K` | **AUSENTE** (`EX-EX5-MEM`; sin Cowan-K en engine) [hecho] | sí (tras gates) | objetos baja-verbalizabilidad; distractor; d'; absorbe EX6 |
| 4 | Pares Asociados A-B/A-C | [V1_04](V1_04_PARES_AB_AC.md) | Memoria | `ASSOCIATIVE_INDEX` + `INTRUSION_RATE` | **EXISTE** (`Ex4PairsGenerator`; sin fase A-C) [hecho] | sí (tras gates) | fase A-C cableada; rotación; demora 24h real; absorbe EX8 |
| 5 | Episódica con Recall Diferido | [V1_05](V1_05_EPISODICA_RECALL_DIFERIDO.md) | Memoria | `DELAYED_RECALL_ACCURACY` | **EXISTE** (`Ex3ListGenerator`; sin diferido servido) [hecho] | sí (tras gates) | distractor pre-inmediato; probe +24h; FalseImprovement |
| 6 | Triaje de Reversibilidad | (Decisiones; ficha aparte) | Decisiones | accuracy triaje + tasa violación de lock | **EXISTE** (procedural `ReversibilityEngine`) [hecho] | **sí** (el más cercano a profile-ready) | **AGRS espacial neutralizado (`spatialEntropy=1.0`) → arreglar precondición** [hecho]; min-n=10/tipo; rotación |
| 7 | Pausa antes de actuar | (Emociones; ficha aparte) | Emociones | `COMMISSION_RATE` | **EXISTE** (`EM1Pausa`/`EM8AntiImpulso`) [hecho] | sí (tras gates) | suelo goAcc≥0.6; gate timebase en RT; absorbe EM8/EM12 |
| 8 | Recuperación post-error | (Emociones; ficha aparte) | Emociones | `POST_ERROR_RECOVERY` (accuracy) | **EXISTE** (`EM2Recuperacion`) [hecho] | sí (tras gates) | error genuino sin anticipaciones; suficiencia≥3 pares; absorbe EM11/EM15 |
| 9 | Detección de lo Inverosímil | (Creatividad; ficha aparte) | Creatividad | `rarityScore × plausibilityRate` | **AUSENTE** (banco 14 prompts, sin scorer rareza) [hecho] | **solo-calibrando** | banco de frecuencias auditado; verificado vs perfil absurdo |

> Las fichas 1–5 (Memoria) están detalladas en este directorio. Las fichas 6–9 viven en sus módulos respectivos; aquí se resumen sus gates clave.

---

## 3. Regla de profile-update común a los 9 (máquina de estados)

Ninguno actualiza perfil hasta que su `ScoreResult` (contrato de `INFRA_01`) sea `VALID`. El veredicto se computa con la **máquina de estados de gates en cascada de cortocircuito** (`ScoringStateMachine.evaluate`, [POR_CONFIRMAR_EN_REPO] `features/mindboost/scoring/contract/`):

```
abandono-emocional → timebase → jank → banco → gaming → invalidación
   → primaria-computable → min-n → cobertura-detector → accuracy-gate
   → estabilidad → VALID
```

`VALID` es el **único** path que pone `updatesProfile = true`. Cualquier estado no-`VALID` produce `primarySignal = null` y **no llama** al aggregator (no hay actualización a la baja: señal inválida es *ausencia* de update, jamás un valor bajo).

| Condición requerida para `VALID` | Infra | Aplica a |
|---|---|---|
| `ScoreResult.state == VALID` | `INFRA_01` | los 9 |
| `nEffective ≥ nMin` por celda | `INFRA_06` (`MinSampleGate`) | los 9 |
| banco validado o procedural | `INFRA_04` | 4, 5 (y 9 bloqueado por banco) |
| anti-gaming implementado+testeado | `INFRA_03` | los 9 |
| copy-scanner verde | `INFRA_05` | los 9 |

Varios entran como `CALIBRATING` hasta acumular sonda UNTRAINED / evidencia inter-sesión. **La mejora de capacidad solo se afirma vía sonda UNTRAINED + RCI** (`|RCI|>1.96`, `RealImprovementCriterion.isGenuine`), **no** por mejora intra-tarea (eso es practice-effect).

---

## 4. Limitación honesta transversal (la sonda UNTRAINED)

Sin la sonda de transferencia UNTRAINED (`EX-TRANSFER-PROBE-MEM`, **P0**, [hecho] hoy NO cableada: `ReliableChangeIndex`/`RealImprovementCriterion`/`RciResult` existen como motores puros **sin ningún caller**; `TransferProbePartition` reserva material pero "se consume solo por Phase-8", no implementada), el "progreso" de los 5 de Memoria es **practice-effect / familiaridad**, no mejora de capacidad.

Por eso:
- Ninguno muestra constructo absoluto: solo "señal de práctica contra tu línea base, en esta tarea" (`ClaimLevel.WITHIN_TASK_TREND` como techo).
- La palabra "mejora" queda **reservada** a la sonda UNTRAINED + RCI reliable.
- Hasta que la sonda esté cableada, los 5 de Memoria pueden alcanzar `VALID` (la señal accuracy es válida) pero su `claimLevel` está capado a rendimiento-en-tarea, nunca a "tu memoria mejoró".

---

## 5. Tabla de gates de módulo (precondiciones P0 antes de exponer cualquier ficha)

| Gate de módulo | Estado repo | Bloquea | Ficha(s) afectadas |
|---|---|---|---|
| `EX-TRANSFER-PROBE-MEM` (sonda UNTRAINED + RCI cableado) | **AUSENTE** (motores sin caller) [hecho] | claim de *mejora* (no `VALID`) | 1, 2, 3, 4, 5 |
| `memoryTerminologyScanner` (copy-gate de léxico IQ/clínico/far-transfer) | **AUSENTE** (solo arena/emotions tienen) [hecho] | `INFRA_05` verde | 1, 2, 3, 4, 5 |
| `MIN_N` realista (hoy `MIN_N=4`, demasiado bajo) [hecho] | débil | `nEffective` honesto | 1, 2, 3, 4, 5 |
| 0-sentinela auditado (`MemoryMetricValue.unavailable()` guarda `value=0.0`) [hecho] | riesgo | `null != 0` | 1, 2, 3, 4, 5 |
| Cowan-K + d' en `MemoryMetricsEngine` | **AUSENTE** [hecho] | `VALID` ficha 3 | 3 |
| `DeferredRecallScheduler` cableado al holder | modelos existen, sin caller [hecho] | demora 24h ficha 4, 5 | 4, 5 |

---

## 6. DoD común a las 5 fichas de Memoria

Cada ficha de Memoria se considera *Done* cuando, además de su DoD específico:
1. Consume `INFRA_01` (`ScoreResult`) + `INFRA_06` (`MinSampleGate`).
2. Tests unit + adversarial + integración + golden verdes (enumerados en cada ficha).
3. `memoryTerminologyScanner` (`INFRA_05`) verde sobre su carpeta.
4. `registry ⟺ implemented` (consistencia generador↔catálogo, patrón `ExerciseCatalogConsistencyTest`).
5. Compila `:shared:compileAndroidMain` + `:androidApp:compileDebugKotlin`.
6. Detekt ratchet **sin regenerar baseline** (supresiones localizadas si inevitable).
