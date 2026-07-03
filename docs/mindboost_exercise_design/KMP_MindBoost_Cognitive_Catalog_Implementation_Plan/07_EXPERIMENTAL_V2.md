# 07 — Catálogo experimental v2 (detallado)

> **Prometedores pero NO listos.** Ninguno actualiza perfil hasta cerrar su precondición.
> Cada ejercicio aquí es **jugable** (regla: jugable ≠ actualiza-perfil) y produce feedback local,
> pero su `ScoreResult` queda fijo en un estado no-`VALID` (`CALIBRATING` / `DEVICE_UNRELIABLE` /
> `BANK_CONTAMINATED` / `SUSPECTED_GAMING` / `INSUFFICIENT_N`) hasta que su bloqueador raíz se resuelva.
>
> **Invariantes que se repiten en cada fila (no negociables):**
> - `null != 0`: una señal no computable se muestra como `—`/"calibrando", JAMÁS como `0` ni percentil bajo.
> - **Enfoque/Velocidad sin profile-update** hasta tener **timebase autoritativa + jank/frame-drop reales**
>   (hoy `frameDropFraction=0.0`/`criticalFrameJitter=false` están hardcodeados en `SpeedSessionStateHolder.kt:259-260`; Focus mide con `MonotonicClock`, no frame-accurate).
> - **Cero claims de far-transfer**, ni siquiera para negarlos: el copy no nombra "vida real / multitarea / fake news / conducción / inteligencia", ni en compuesto ni en negación (scanner `\b` cruza guiones).
> - **Anti-gaming solo cuenta si está implementado + testeado con adversarios** (Regla #2): un detector "diseñado" NO desbloquea promoción. Estado real anclado a `INFRA_03` (D1–D12).
> - **Banco pequeño / no-validado bloquea perfil**: sin holdout servido, inter-rater ≥0.80 y tamaño mínimo, el claim de capacidad queda `BANK_CONTAMINATED` por construcción.

## Criterio de promoción EXACTO (v2 → v1)

Un experimental se promueve a v1 **solo si se cumplen TODAS** las siguientes condiciones (AND lógico, no "mayoría"):

1. **Issue del roadmap CERRADO** (`AUDIT_V2_TECNICO.md` §9 / `03_ROADMAP`) con su Definition of Done verde.
2. **Precondición raíz resuelta** (la columna "Bloqueador raíz" de la tabla, p.ej. timebase autoritativa, banco ≥N con holdout, métrica computada en el engine).
3. **Tests adversariales verdes**: el/los perfil(es) sintético(s) aplicables (D1–D12 de `INFRA_03`) producen `SUSPECTED_GAMING`/`Contaminated`/`Invalidated`, **nunca** `Valid`; y `HonestUserProfile` **no** dispara (control de falsos positivos).
4. **Min-n por celda** cumplido vía `MinSampleGate` (INFRA-06): comparativas ≥20/celda; SSRT ≥30 stops con `p(respond|signal)∈[0.25,0.75]`; Brier ≥40 juicios inter-sesión; RCI ≥2-3 administraciones equated; BUR min-n=12 con exclusión priori=posterior.
5. **Ortogonalidad empírica `r < 0.7`** vs los ejercicios v1 que midan varianza similar (donde haya riesgo de redundancia — regla 9). Si `r ≥ 0.7` ⇒ **MERGE**, no promoción.
6. **Copy-scanner verde** (INFRA-05): el copy de la pantalla no contiene léxico prohibido; el constructo se reporta como proxy, no absoluto.

Si **una** condición falla, el ejercicio permanece en v2 y su `ScoreResult` sigue no-`VALID`. No hay promoción "por optimismo".

## Tabla maestra (precondición · criterio de promoción exacto · issue · estado real anti-gaming)

Leyenda de estado: `hecho` (verificado en ground-truth) · `inferido` · `por_confirmar` (`POR_CONFIRMAR_EN_REPO`).
Ruta probable salvo indicación. Issue = id del backlog en `AUDIT_V2_TECNICO.md` §9.

| Ejercicio | Módulo | Bloqueador raíz | Precondición de promoción (qué cierra el bloqueador) | Criterio de promoción EXACTO (gate verificable) | Adversario(s) (INFRA_03) | Estado real anti-gaming | Issue del roadmap | `ScoreResult` mientras tanto |
|---|---|---|---|---|---|---|---|---|
| **E1 Vigía (PVT)** | Enfoque | RT/jank no medidos | Timebase autoritativa portada a Focus (INFRA-02) + detector de cadencia `RhythmicTapper` implementado+testeado; jitter≥150 ms; `slope = null` si `n_targets < 20` | `RhythmicTapper`(D1)⇒`Contaminated` Y `SUSTAINED_FOCUS_INDEX` no se infla; `slope==null` con n<20 (test); RT-crítico sin Android autoritativo⇒`DEVICE_UNRELIABLE` | D1 `RhythmicTapper`, D4 `Anticipator` | D1 `ABSENT`→REVISE; D4 trial `hecho` | `INFRA-02` + `EX-E1-VIGIA` | `DEVICE_UNRELIABLE` (RT) / `CALIBRATING` |
| **E3 Gatillo (Go/No-Go)** | Enfoque | RT | `INHIBITORY_CONTROL_INDEX` null-honesto; "responder a todo" (omisión≈0 + FA alto)⇒`Contaminated`; n≥20 | `RespondToAll`(D4 bloque)⇒`WITHHOLD_PROFILE`; index `null` con n<20; solapa E4/E1/E12 ⇒ exigir `r<0.7` vs E4 o MERGE | D4 `RespondToAll`, D1 `RhythmicTapper` | D4 bloque `PARTIAL`→REVISE | `EX-E3-FOCUS` (P2) | `CALIBRATING` |
| **E12 Re-enganche** | Enfoque | RT + constructo no validado | `reengagementLatency = null` con `<N` lapsos; desenganche total **separado** del lapso (`Contaminated`); evidencia interna latencia↔RT_CV (no tedio) | desenganche total NO se cuenta como reenganche (test); latencia `null` con pocos lapsos; gate fatiga readiness-only; ortogonalidad latencia↔RT_CV documentada | D11 `Quitter` (cognitivo) | constructo `por_confirmar` | `EX-E12-REENGAGE` (P1) | `CALIBRATING` |
| **E1 Tinte Crítico** | Velocidad | jank + pantalla (ΔE) | Jank-gate VIVO (frameDrop real, INFRA-02) + Android autoritativo + sintético adversario puntúa bajo; `axisRotation≥30%`; anclaje ΔE device-local | `RandomTapper`⇒bajo; sin Android⇒`UntrustedTimebase`; ΔE device-local anclado (no cruza sesiones como comparable); variante luminancia daltonismo-safe | D3 `SpeedAccuracyHacker`, D4 `RandomTapper` | jank-gate `ABSENT` (hardcoded)→REVISE | `EX-E1-VEL-TINTE` (P1) | `DEVICE_UNRELIABLE` |
| **E4 Compuerta (Go/No-Go veloz)** | Velocidad | impulsividad recompensada | Gate de comisión testeado con sintético impulsivo (baja dificultad ante faRate, **nunca acelera**) + jank-gate; techo incluye `faRate>0.30`; `faRate=null` honesto con pocos no-go | `RespondToAll`/impulsivo⇒nunca acelera (test); `faRate==null` con n<MIN_N; "impulsividad recompensada" eliminada | D4 `RespondToAll`, D3 `SpeedAccuracyHacker` | dificultad-acelera `hecho`→REVISE | `EX-E4-VEL-COMPUERTA` (P1) | `CALIBRATING` |
| **E6 Flanker Express** | Velocidad | métrica ausente + jank | `CONGRUENCY_EFFECT` implementado en `MetricCalculator` (deuda actual) + ortogonalidad vs switch/comisión + estímulos NO-lingüísticos (flechas/formas) forzados por scanner + jank-gate | Stroop-palabra⇒scanner rojo (regla cultural); `CONGRUENCY_EFFECT==null` si falta una celda; `r<0.7` vs E3-Vel switch o **MERGE/RETIRE** E3-Vel | D6 `LeftStubborn` (posicional) | `CONGRUENCY` ausente en speed `hecho`→BLOCK métrica | `EX-E6-VEL-FLANKER` (P1) | `INSUFFICIENT_N` |
| **E1 Evidence Lock** | Lenguaje | banco fijo (36) | Banco≥40 con holdout servido + flag `HIGH_PRIOR_KNOWLEDGE` por banco-tema + `selectNext`/`NearTransferVerifier` wired + ortogonalidad vs Scope&Negation | `BankMemorizer`(D5)⇒no eleva trait; `HIGH_PRIOR` se eleva por tema conocido; accuracy banco fijo ≤ holdout; `r<0.7` vs E5 | D5 `BankMemorizer`, D7 `BankGrinder` | D5/D7 `ABSENT` (sin exposure)→BLOCK | `EX-E1-EVIDENCE-LOCK` (P1) | `BANK_CONTAMINATED` |
| **E5 Scope & Negation** | Lenguaje | solape + banco | Cuota ≥20% ítems "cero inferencias válidas" + banco≥50 balanceado por `operatorType` (≥8/operador) + ortogonalidad vs E1/Lógica | "marca-algo-siempre" rinde bajo en ítems cero-válidas (test); `r<0.7` vs E1; min-n por operador; mono-idioma declarado. Si `r≥0.7` con E1 ⇒ **MERGE** | D5 `BankMemorizer`, D6 posicional | banco/holdout `ABSENT`→BLOCK | `EX-E5-SCOPE-NEG` (P1) | `BANK_CONTAMINATED` |
| **EX7 Orden Serial** | Memoria | generador ausente | Scoring estricto por posición + disociar item/orden en sintéticos | item-correcto-orden-incorrecto puntúa parcial (test); generador con fillers construido; disociación item↔orden verificada en sintéticos | D5 `RoteMemory` | generador `ABSENT`→BLOCK | (Memoria backlog) | `INSUFFICIENT_N` |
| **EX_TRANSFER_PROBE** | Memoria | **no cableado (P0, pieza clave de honestidad)** | Formas paralelas A/B/C **equiparadas (equating)** + RCI gated (≥2-3 admin, `NotEnoughData` por defecto) + scheduler real que SIRVE la sonda UNTRAINED | RCI `NotEnoughData` con <2 admins (test); sonda UNTRAINED servida desde partición reservada; equating verificado en sintéticos; mejora declarada SOLO si `|RCI|>1.96 && isGenuine`; cero feedback en la sonda | D7 `BankGrinder` (anti-reexposición) | RCI/`RealImprovementCriterion` `hecho` pero **sin caller** | `EX-TRANSFER-PROBE-MEM` (**P0**) | `INSUFFICIENT_N` (`NotEnoughData`) |
| **E1 Bayes Arena** | Decisiones | BUR inflado + AGRS hardcoded | Exclusión `priori=posterior` del BUR (trials sin evidencia inflan `BUR=1.0`) + AGRS real (`isDialDefault`/`dialPathEntropy`, hoy false) + min-n=12 | trial `priori=posterior` excluido del BUR (test); dial-constante detectado por AGRS; `seed!=0`+rotación; copy-scanner decisiones verde | D6 `PositionBias` (revive `entropyFactor`), D15 priori=posterior | AGRS `entropyFactor` muerto (`spatialEntropy=1.0`)→REVISE | `EX-E1-BAYES` (P1) | `CALIBRATING` |
| **EM3 Separación 4-cat** | Emociones | banco sin validar | Inter-rater ≥0.80/item (descarte de no-acuerdo) + holdout **SERVIDO** (hoy los 12 eval se filtran y nunca se sirven) + detector de entropía `ButtonSpammer` | `ButtonSpammer`(D6/D13)⇒`Contaminated`; ítem con IR<0.80 excluido; holdout efectivamente servido en eval; RT-floor proporcional a longitud | D6/D13 `ButtonSpammer`, D11 `SafetyExit` (Regla #8) | holdout fantasma `hecho`→BLOCK | `EX-EM3-SEPARATOR` (**P0**) | `BANK_CONTAMINATED` |
| **EX03 Síntesis Restricción** | Creatividad | gameable por tag | Ventana de elaboración como **gate duro** (anti keyword-stuffing) + banco con tensión real | `KeywordStuffer`(D8)⇒`constraintSatisfactionRate` NO sube por presencia del tag; near-duplicates no cuentan como ideas distintas | D8 `KeywordStuffer` | content-integrity **BLOCKED por diseño** `hecho`→BLOCK | (Creatividad backlog) | `CALIBRATING` (tope `PRACTICE_SIGNAL`) |
| **EX05 Elaboración Incremental** | Creatividad | premia verbosidad | Cap de palabras (anti-verbosidad) + `coherenceProxy` como banda, no nota | verbosidad sin contenido NO eleva señal (test); `coherenceProxy` mostrado como CALIBRATING; normalización IBI por baseline motora (anti tecleo-rápido, D12.tecleo) | D8 `KeywordStuffer`, tecleo-rápido | BLOCKED `hecho`→BLOCK | (Creatividad backlog) | `CALIBRATING` |
| **EX07 Reencuadre Problema** | Creatividad | adivinanza de marcadores | Onboarding obligatorio + variante "escala" verificable; `frameDiversity` como proxy declarado | marcador no-resuelto no puntúa (test); variante "escala" tiene clave verificable; `frameDiversity` reportado como proxy, no originalidad real | D8 `KeywordStuffer` | BLOCKED `hecho`→BLOCK | (Creatividad backlog) | `CALIBRATING` |

## Dependencias cruzadas (qué INFRA desbloquea cada experimental)

| INFRA (P0) | Experimentales que desbloquea |
|---|---|
| INFRA-01 `ScoreResult`/state machine | TODOS (cada estado no-`VALID` se deriva de aquí) |
| INFRA-02 `TimingPolicy` + jank real | E1-Vigía, E3, E12 (Enfoque); E1-Tinte, E4, E6 (Velocidad) |
| INFRA-03 `AntiGamingFramework` (D1–D12) | E1-Vigía(D1), E4-Compuerta(D4), E1-Bayes(D6), EM3(D6/D13), EX03/05/07(D8), Transfer-Probe(D7) |
| INFRA-04 `ItemBankValidation` | E1-Evidence-Lock, E5-Scope-Neg (Lenguaje); EM3 (Emociones); EX_TRANSFER_PROBE (Memoria) |
| INFRA-05 `CopySafetyScanner` | E6-Flanker (no-lingüístico), todo Lenguaje, EX03/05/07 (Creatividad), E1-Bayes (decisiones) |
| INFRA-06 `MinSampleGate` | E6-Flanker (≥20/celda), E1-Bayes (min-n=12), Transfer-Probe (RCI), EM3 |

## Nota de gobernanza

La promoción de cualquier experimental a v1 **no** ocurre por optimismo: requiere su **issue cerrado** en el
roadmap (`AUDIT_V2_TECNICO.md` §9), **tests adversariales verdes** (perfil sintético aplicable de `INFRA_03`
disparando el estado correcto + `HonestUserProfile` sin falsos positivos) y, donde haya riesgo de redundancia,
**ortogonalidad empírica `r<0.7`** demostrada vs los v1 que midan varianza similar.

`EX_TRANSFER_PROBE` es la **prioridad de v2** porque su cierre desbloquea la afirmación de *mejora real de
capacidad* (nivel 3–4 de transferencia) para **TODO el módulo Memoria**: sin sonda UNTRAINED servida + RCI
gated por fiabilidad, ningún ejercicio de Memoria puede pasar de "mejora dentro de la tarea entrenada"
(nivel 1–2). Hasta entonces, todo claim de Memoria se restringe a `WITHIN_TASK_TREND` como máximo.
