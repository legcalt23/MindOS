# 10 · Matriz Anti-Gaming (detector × ejercicio × adversario × resultado esperado)

> Fuente: `INFRA_P0.md` (D1–D12), `AUDIT_V2_TECNICO.md` (§10 test plan adversarial),
> `AUDIT_V2_GROUND_TRUTH.md` (anti-gaming real). Leyenda: `hecho` · `inferido` · `por_confirmar`.
> Todos los tests corren vía `AntiGamingFramework` (INFRA-03); el build FALLA si el resultado
> esperado NO se cumple (rojo-si-puntúa-alto). **Invariante #a (`null != 0`):** todo "resultado
> esperado" no-VALID se sirve como `Contaminated`/`WITHHOLD`/`Invalidated`, NUNCA score bajo.
> **Invariante #d:** una celda solo cuenta cuando hay test adversarial implementado.

---

## 1. Matriz núcleo (detector × ejercicio × adversario)

| Detector | Adversario | Ejercicios objetivo (`hecho`/`inferido`) | Observa | Resultado esperado | Estado |
|---|---|---|---|---|---|
| D1 Rhythm tapping | `RhythmicTapper` | Focus E1 Vigía, E3 Gatillo; Emociones EM1/EM8/EM12 | IRI casi constante, indep. de estímulo | no infla `SUSTAINED_FOCUS_INDEX`/comisión → `WITHHOLD_PROFILE` | `ABSENT`→REVISE crítico |
| D2 Waiting strategy | `WaitingStrategist` | Focus E4 Freno (SSRT) | go-RT inflado; `p(respond)∉[0.25,0.75]` | SSRT `trustworthy=false` → `WITHHOLD` (solo SSRT) | `IMPLEMENTED+TESTED` |
| D3 Speed-accuracy hack | `SpeedAccuracyHacker` | Velocidad E4 Compuerta, E2 DSST; todo RT-gated | RT ultrabajo + accuracy≈azar | la dificultad NO acelera; `INVALIDATE` | `PARTIAL`→REVISE |
| D4 Too-fast-to-process | `ButtonSpammer`/`AnticipatorProfile` | Focus E1–E13; Velocidad E1–E8; EM1/EM2/EM12 | RT < piso; `anticipationRate>0.30` | trial descartado; bloque `WITHHOLD` | `IMPLEMENTED+TESTED` (trial) |
| D5 Memorization | `BankMemorizer` | Lenguaje E1–E6+mixta; Decisiones bancos; Memoria pares EX4/EX8 | RT↓+acc→1.0 en repetido; isomorfo plano | banco fijo NO supera holdout/UNTRAINED → `WITHHOLD`+rotación | `ABSENT`→**BLOCK** |
| D6 Answer-position bias | `LeftStubborn`/`ButtonSpammer` | EM3 Separación; multi-opción; Bayes dial; Reversibility swipe | entropía respuesta baja vs correctKey | "siempre b"/un lado no supera azar; `INVALIDATE` (revive `entropyFactor` AGRS) | `ABSENT`→REVISE crítico |
| D7 Repeated-bank exploit | `BankGrinder`/`BankMemorizer` | Lenguaje (6 fijos/banco); Decisiones; Emociones EM3; Memoria | `reexposedFraction>0.5` | holdout no aparece en práctica; `WITHHOLD`+rotación | `ABSENT`→**BLOCK** |
| D8 Keyword-stuffing | `KeywordStuffer` | Creatividad EX01/EX03/EX06/EX07; Lenguaje abierto | TTR bajo + densidad keyword + duplicados | `constraintSatisfaction`/fluency NO sube; near-dup no cuenta; `INVALIDATE` | `ABSENT+BLOCKED`→**BLOCK** |
| D9 Low-effort reveal | `RevealAllNoRead` | Decisiones E5 Escáner; Lenguaje/Logic con explicación | reveal antes de commit; RT post-reveal | `alternativesScannedRatio` no sube revelando-sin-leer; `WITHHOLD` revelados | `ABSENT`→REVISE |
| D10 Motor/tecleo contam. | `MinEffort`/`ButtonMasher` | Creatividad EX01/EX05/EX08; Velocidad E5; Enfoque E1/E9 | multi-touch, sub-50ms intra-trial | reclasifica `DEVICE_UNRELIABLE`, NO fraude, NO penaliza | `ABSENT`→REVISE |
| D11 Abandonment manip. | `Quitter` vs `SafetyExit` | Logic/Memoria (cognitivo); **Emociones desactivado** | abandono covaría con dificultad | cognitivo: `WITHHOLD`; **emocional: NUNCA penaliza** (Regla #8) | `PARTIAL`→REVISE |
| D12 Synthetic harness | todos + `HonestUser` + `AbsentNoSearch` | suite sobre ejercicios reales | atrapa D1–D11; honest no dispara | red anti-regresión verde | `ABSENT`→**BLOCK** transversal |

## 2. Vectores del test plan (§10 TECNICO) mapeados a celdas

| Vector (§10) | Detector(es) | Criterio de FALLO (lo que NO debe ocurrir) |
|---|---|---|
| 10.1 Jank/frame-drop | INFRA-02 + D4 | bloque produce `Valid`/accuracy crudo en vez de `Contaminated`; `frameDropFraction` sigue 0.0 hardcoded (`hecho`) |
| 10.2 Timebase falsa/device swap | INFRA-02 | RT/SSRT/timing actualiza perfil sin timebase autoritativa |
| 10.3 Rhythm tapping | D1 | `SUSTAINED_FOCUS_INDEX`/comisión se infla; cadencia no detectada |
| 10.4 Waiting strategy | D2 | SSRT `trustworthy` con go-RT con/sin-stop divergente |
| 10.5 Speed-accuracy hack | D3 | la dificultad ACELERA ante perfil impulsivo; IES no penaliza |
| 10.6 Memorization | D5 | accuracy del banco fijo supera holdout/UNTRAINED y eleva trait |
| 10.7 Answer leakage | D6 | "siempre b" supera azar; holdout en práctica; posición no balanceada (chi²) |
| 10.8 Keyword-stuffing | D8 | fluency/constraintSatisfaction sube por tag; near-dup cuenta como ideas |
| 10.9 Bank exhaustion | D7 | banco se repite sin rotación; "progreso"=curva de exposición |
| 10.10 Cultural prior | D5 + INFRA-04 `culturalLoad` | prior cultural infla accuracy y `HIGH_PRIOR` no se eleva |
| 10.11 Abandonment manip. | D11 | abandono REDUCE score o se trata como bajo rendimiento (viola Regla #8) |
| 10.12 Tecleo rápido | D10 | velocidad de tecleo correlaciona con señal sin normalización IBI |
| 10.13 Low-effort/button spam | D6/D10 | sin detector de entropía el bloque produce `Valid`; AGRS no marca dial/swipe constante |
| 10.14 Reveal-all/waiting | D9 | `alternativesScannedRatio` sube revelando-sin-leer; fuerza bruta por reset no limitada |
| 10.15 Priori=posterior (Bayes) | D6/AGRS | trials priori=posterior cuentan BUR=1.0 e inflan señal |

## 3. Celdas que HOY cuentan vs no cuentan (Regla #2)
- **Cuentan (`hecho`):** D2×E4-Focus (SSRT); D4-trial×Focus/Speed (anticipación); AGRS RT<400×Decisiones.
- **No cuentan (diseño/ausente):** todo D1, D3-invalidación, D5, D6, D7, D8, D9, D10, D11-manip, D12.
- **Conclusión:** la matriz es mayormente **rojo de cobertura** hasta que D12 (harness sobre
  ejercicios reales) esté verde. Ningún ejercicio con celda no-verde puede afirmar anti-gaming.

## 4. Criterio de cierre por celda (Definition of Done de la matriz)
Una celda (detector×ejercicio) está VERDE cuando: (1) el detector está implementado; (2) existe un
perfil sintético adversario; (3) un test falla el build si el adversario obtiene `Valid`/score alto;
(4) `HonestUser` no dispara esa celda. Si falta cualquiera de los 4, la celda es ROJA y el ejercicio
NO puede tener `updatesProfile=true` por esa vía de gaming.
