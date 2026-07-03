# 15 — Riesgos, ADR y decisiones (dissent log completo)

> Origen: deliberación del comité de `00_CHARTER.md §2` (12 agentes con veto), auditorías
> `AUDIT_V2_PRODUCTO.md` (§13 dissent log) y `AUDIT_V2_TECNICO.md`, y el ground-truth verificado en
> `AUDIT_V2_GROUND_TRUTH.md`. Cada ADR lleva **contexto · decisión · consecuencias · alternativas
> descartadas**. Estado: **(hecho)** verificado en repo · **(inferido)** del diseño · **(POR_CONFIRMAR_EN_REPO)**.
>
> **Invariantes que toda decisión respeta:** `null != 0`; Enfoque/Velocidad sin profile-update hasta
> timebase+jank reales; cero claims de far-transfer NI para negarlos; anti-gaming solo si
> implementado+testeado con adversarios; banco pequeño/no-validado bloquea perfil.

---

## A. Architecture Decision Records

### ADR-01 — Score honesto sellado, no `Double` crudo

- **Contexto.** El repo tiene **4 estilos de "estado de score" incompatibles** (hecho, `AUDIT_V2_GROUND_TRUTH.md`):
  banda+`confident` (Focus `FocusScore`), `preliminary`+nullables (Speed `CognitiveProfileScore`), sealed gated
  (Memory `RciResult`), máquina de madurez (`DimensionStatus`). Decisiones usa `Double` crudo con `0.0` como
  sentinela (`ScoringEquations.PHD=0.0` si AGRS>0.75). Un `Double` puede colar un estado inválido como
  "rendimiento bajo".
- **Decisión.** `ScoreResult` sellado (8 estados) como **veredicto de borde read-only** que se *deriva* de los
  scores existentes vía `ScoreResultAdapter<Raw>`; `ProfileUpdateGate` consume **solo** ese tipo, nunca
  `Double`. Estados: `VALID/CALIBRATING/INSUFFICIENT_N/INVALIDATED/DEVICE_UNRELIABLE/BANK_CONTAMINATED/
  SUSPECTED_GAMING/ABANDONED_SAFE`. **Aceptada.**
- **Consecuencias.** (+) `null != 0` forzado en `init`; imposible construir `VALID` con `primarySignal==null`.
  (+) Los 4 estilos quedan intactos (son `Raw`); cero rotura de tests/golden/Detekt. (−) Cada módulo debe
  escribir un adaptador. (−) Hay que reconciliar 2 escalas (0..1 vs 0..100) y 2 banderas (`preliminary` vs
  `confident`) en el adaptador.
- **Alternativas descartadas.** (a) Refactor de los 4 tipos a un supertipo común — rompe el ratchet Detekt y
  todos los golden; descartada. (b) Mantener `Double` + convención — un bug filtra inválido como bajo;
  descartada por `null != 0`.

### ADR-02 — RT fuera del perfil sin timebase autoritativa

- **Contexto.** (hecho) Solo **Speed/Android** tiene timebase frame-accurate (`Choreographer.frameTimeNanos` +
  `MotionEvent.eventTimeNanos`, misma base `ANDROID_UI_UPTIME_NANOS`, prohibido `elapsedRealtimeNanos`). Focus
  mide con `MonotonicClock` a nivel de corrutina (no frame); logic/decisiones/emotions sobre wall-clock epoch;
  Speed/Desktop·iOS caen a fallback. El jank-gate está muerto (`frameDropFraction=0.0` hardcoded).
- **Decisión.** RT-crítico sin timebase `AUTHORITATIVE` ⇒ `DEVICE_UNRELIABLE`: jugable, **no** actualiza perfil.
  Enfoque y Velocidad **no entran a v1 de perfil**. **Aceptada.**
- **Consecuencias.** (+) Un competidor con 2 teléfonos no puede mostrar scores divergentes de perfil (los RT no
  escriben perfil fuera de Android autoritativo). (+) iOS sin CADisplayLink degrada honestamente. (−) El cluster
  más grande de ejercicios (todo Enfoque/Velocidad) queda como minijuego hasta portar timebase+jank.
- **Alternativas descartadas.** (a) Usar el RT de Focus/wall-clock "como aproximación" — mide GPU+pantalla, no
  cognición; descartada. (b) Mostrar RT con disclaimer pero actualizando perfil — viola "no actualizar perfil
  bajo dispositivo-no-confiable" (`00_CHARTER.md §1`); descartada.

### ADR-03 — Anti-gaming solo si implementado+testeado con adversarios

- **Contexto.** (hecho) De ~8 capacidades pedidas, ~4 son reales (anticipación Focus/Speed, RT<400/AGRS
  Decisiones, SSRT-trust waiting-strategy, fast-commission + quality-gate Speed + abandono Emotions); cadencia de
  tap existe **solo en Arena sintético**; sesgo posicional **no existe** (`spatialEntropy=1.0` fijo,
  `entropyFactor` del AGRS muerto); keyword-stuffing **BLOCKED por diseño** (`CreativityAiLayers.kt:44`).
- **Decisión.** Un detector "diseñado" no cuenta. Cada detector **consumido** por un ejercicio exige su perfil
  sintético adversario en **test rojo** (falla el build si el adversario puntúa alto). La cobertura ausente
  **degrada el claim y bloquea `VALID`** (no se finge capacidad). **Aceptada.**
- **Consecuencias.** (+) Imposible listar un ejercicio "anti-gameado en el papel". (−) Hay que construir
  `AntiGamingFramework` + ~10 adversarios (`RhythmicTapper`, `WaitingStrategist`, `RespondToAll`, `ButtonSpammer`,
  `KeywordStuffer`, `BankMemorizer`, `MinEffort`, `AbsentNoSearch`, `RevealAllNoRead`, `SpeedAccuracyHacker`).
- **Alternativas descartadas.** (a) Confiar en los detectores reales existentes y asumir el resto — deja la
  mitad del catálogo gameable; descartada. (b) Marcar "anti-gaming: diseñado" en docs — viola regla #2;
  descartada.

### ADR-04 — Banco validado como precondición de perfil

- **Contexto.** (hecho) **Cero validación empírica en todo el sistema.** Logic(6 fijos, orden fijo),
  Language(36 fijos), Emotions SEPARATOR(40, holdout fantasma: los `isEval` se filtran y nunca se sirven),
  Creatividad(14). Sin `discriminability` IRT, `culturalLoad`, `interRater`, `exposure`. Argument X-Ray:
  correcta="b" en 6/6 (posicional explotable).
- **Decisión.** Build **falla** si un ejercicio profile-updating usa banco `< minSize` o sin holdout activo
  servido; `selectNext()` nunca repite holdout en práctica; posición correcta balanceada (chi²); inter-rater
  `≥0.80` donde el "correcto" es juicio humano. **Aceptada.**
- **Consecuencias.** (+) "Progreso" deja de ser memorización. (−) Bloquea de inmediato todo Lenguaje, parte de
  Decisiones/Emociones hasta construir bancos `≥40-50` validados.
- **Alternativas descartadas.** (a) Rotar el banco pequeño por seed — no resuelve memorización tras pocas
  sesiones; descartada. (b) Confiar en el claim "calibrated" de los KDoc — es claim de autor, no validación;
  descartada.

### ADR-05 — Copy-scanner en `check`, anti-negación, multi-módulo

- **Contexto.** (hecho) Hay 2 scanners reales (`arenaTerminologyScanner`, `emotionsTerminologyScanner`) en
  `shared/build.gradle.kts`, ambos enganchados a `check`, whole-word case-insensitive, **cruzan guiones**.
  **Cobertura cero** para focus/memory/speed/logic/decisiones/language/creativity y el global `AppStrings`.
- **Decisión.** Scanner que falla el build ante lexema prohibido **incluso en negación/compuesto/guion**,
  cubriendo commonMain+commonTest+desktopTest de TODO MindBoost; es **control técnico**, no revisión manual.
  **Aceptada.**
- **Consecuencias.** (+) Mata el riesgo regulatorio en build, no en review. (+) Barato (espejo de los 2
  existentes). (−) Riesgo de falso-positivo en substrings legítimos (p.ej. "balance" semántico) ⇒ allowlist de
  excepciones + test de regresión.
- **Alternativas descartadas.** (a) Revisión manual de copy por PR — humano falible, no auditable; descartada.
  (b) Scanner solo en commonMain — los tests también filtran copy de proceso; descartada.

### ADR-06 — Mejora de capacidad solo vía sonda UNTRAINED + RCI

- **Contexto.** (hecho) `RealImprovementCriterion` (untrained delta>0.05 && effectSize≥0.30 && sustained≥2
  semanas) y `ReliableChangeIndex` (`|RCI|>1.96`) existen pero **sin caller**; `TransferProbePartition`
  reservado pero nunca servido. La mejora intra-tarea es practice-effect.
- **Decisión.** Solo el cambio en **tarea no entrenada** con **RCI reliable** se afirma como mejora. UI muestra
  "Mejora confirmada" solo si `isGenuine && reliable`. **Aceptada.**
- **Consecuencias.** (+) Diferencia a MindBoost del brain-training desacreditado. (−) Exige cablear formas
  paralelas equiparadas (equating) + scheduler + gate de fatiga antes de cualquier claim de mejora.
- **Alternativas descartadas.** (a) Mostrar la curva intra-tarea como "mejora" — es familiaridad; descartada.
  (b) Afirmar far-transfer con literatura genérica — over-claim indefendible; descartada.

### ADR-07 — Catálogo corto > catálogo grande

- **Contexto.** (hecho, `AUDIT_V2_PRODUCTO.md §"Mapa de redundancias"`) De 71, **≥18 ejercicios miden varianza
  ya cubierta**: E5≡E2 (congruencia), EX6≡EX5 (K visoespacial), EM4≡EM14, EM2≡EM11≡EM15 (post-error),
  EM1≡EM8 (go/no-go afectivo), EX4≡EX8 (asociativa), EX01≡EX02 (fluidez), V-E3≡V-E6.
- **Decisión.** Fusionar/retirar; el brain-profile **no cuenta varianza dos veces**. Exigir ortogonalidad
  empírica `r<0.7` antes de listar dos del mismo cluster. **Aceptada.**
- **Consecuencias.** (+) Catálogo limpio (9 v1) más vendible y defendible. (−) Producto pierde "sensación de
  variedad" de 15 ejercicios de Emociones.
- **Alternativas descartadas.** (a) Mantener los 71 "por percepción de profundidad" — infla el perfil y dobla la
  varianza; descartada (regla 10 del charter).

---

## B. Dissent log completo (8 desacuerdos del comité)

> Formato: tema · postura **Producto** · postura **Contraparte** (agente con veto) · **decisión final** del
> Director + razón. Ancla: `AUDIT_V2_PRODUCTO.md §13`.

| # | Tema | Producto (postura) | Contraparte (veto) | Decisión final + razón |
|---|---|---|---|---|
| 1 | Velocidad E4 Go/No-Go | "es la mejor señal de inhibición; promoverla a canónica y listarla ya" | Psicómetra (C): "el gate de comisión NO está testeado con sintético impulsivo ⇒ es impulsividad recompensada; listar sin eso es fraude de medición" | **Psicómetra gana.** No se lista sin gate de comisión testeado; minijuego sin perfil entretanto. Razón: medición sin gate adversario = falso progreso. |
| 2 | Memoria EX_NBACK | "n-back es el más reconocible del mercado; vende inteligencia" | Ciencia (A): "es el paradigma con PEOR reputación de far-transfer; cualquier claim de Gf/IQ es indefendible" | **Ciencia gana.** Si entra: `d'` honesto + copy-scanner que prohíbe IQ/Gf en código; sin `d'` honesto ⇒ RETIRE. No se vende inteligencia. |
| 3 | Lenguaje E6 Source Triage | "detectar fuentes fiables es el feature más vendible del módulo" | Privacidad/Regulatorio (K): "el claim roza fake news/evaluación de medios = frontera regulatoria; el copy-scanner ni existe para Lenguaje" | **Privacidad gana, BLOCK→RETIRE** si el copy no se separa honestamente. Razón: un screenshot regulatorio supera cualquier upside de marketing. |
| 4 | Emociones EM6 Tolerancia | "tolerancia a la frustración es un beneficio emocional muy demandado" | Seguridad (J): "prolongar deliberadamente la espera para medir frustración ES inducción emocional leve; cruza la frontera no-clínico" | **Seguridad gana.** RETIRE salvo prueba documentada de no-inducción. Ningún beneficio justifica inducir afecto negativo. |
| 5 | Emociones 15→3 | "15 ejercicios señalan profundidad y dan variedad" | Ing+Psico (G,C): "9 de 15 son clones (EM7/8/10/11/12/14/15) o no-implementados; inflado por cantidad, no señal" | **Ing+Psico ganan.** Colapsar a EM1/EM2/EM3 (tras banco) = 3 limpios. Regla 10: mejores señales, no más ejercicios. |
| 6 | Creatividad EX08 Improvisación | "creatividad bajo presión es un gancho potente" | Ciencia (A): "mide velocidad de tecleo, no ideación; la presión mata la divergencia; sin timebase es destreza de UI (prohibida por charter)" | **Ciencia gana.** RETIRE salvo calibrador motor + timebase autoritativa; reposicionar a anti-bloqueo, no creatividad-bajo-presión. |
| 7 | Velocidad E5 Cadencia | "el timing predictivo es un paradigma único y diferenciado" | Ingeniero (G): "la señal central (RT<0/adelanto) ROMPE `computeRtNanos` que lanza con rt<0; listar algo que crashea el motor es inviable" | **Ingeniero gana.** RETIRE hasta `TimingPolicy` con signo priorizada en un release dedicado. |
| 8 | Decisiones E1 Bayes BUR | "calibración bayesiana es el corazón conceptual del módulo; listarla" | Psicómetra (C): "trials priori=posterior se cuentan como BUR=1.0 e inflan la señal; AGRS neutralizado por hardcode ⇒ número bonito sin validez" | **Psicómetra gana, queda en v2.** Entra solo con exclusión priori=posterior + AGRS real + min-n=12. |

**Patrón del dissent.** En 8/8 ganó la contraparte con veto (Psicómetra, Ciencia, Privacidad, Seguridad,
Ingeniero), no Producto. El charter (`§2`) da **veto bloqueante** al Regulador/Claims Auditor (K) y al Red Team
(J); Producto no puede sobreescribir un veto de medición, seguridad o claims.

---

## C. Tabla de riesgos (probabilidad × impacto × mitigación)

> Probabilidad/Impacto: Baja/Media/Alta. **Severidad** = combinación. Ancla a hallazgos verificados.

| Riesgo | Prob. | Impacto | Severidad | Mitigación | Estado |
|---|---|---|---|---|---|
| Practice-effect domina sin sonda UNTRAINED (la "mejora" es familiaridad) | Alta | Alto | **Crítica** | `EX_TRANSFER_PROBE` P0 cableado; mejora solo vía RCI `|>1.96|` + UNTRAINED (ADR-06) | abierto (hecho: RCI sin caller) |
| Jank-gate muerto ⇒ RT/frame mide GPU+pantalla en ~40 ejercicios | Alta | Alto | **Crítica** | INFRA_02: `FrameTimingProbe` real (Choreographer); `frameDropFraction` medido; fuera de Android autoritativo ⇒ `DEVICE_UNRELIABLE` | abierto (hecho: `0.0` hardcoded) |
| Claim regulatorio en screenshot (fake news / IQ / regulación / apuesta) | Media | Existencial | **Crítica** | INFRA_05 copy-scanner en `check`, anti-negación, multi-módulo (ADR-05); cero claims NI para negarlos | abierto (hecho: 5 módulos sin scanner) |
| Banco memorizable ⇒ "progreso" = exposición | Alta | Medio | **Alta** | INFRA_04: banco `≥40-50`, holdout servido, rotación, inter-rater `≥0.80` (ADR-04) | abierto (hecho: 6/36/14 fijos) |
| Anti-gaming "diseñado" deja la mitad del catálogo gameable | Media | Alto | **Alta** | INFRA_03: framework + adversarios; cobertura ausente bloquea `VALID` (ADR-03) | abierto (hecho: ~4/8 reales) |
| Device-swap produce scores de perfil divergentes | Media | Alto | **Alta** | ADR-02: RT no escribe perfil fuera de Android autoritativo; test `DeviceSwapInvarianceTest` | mitigado por diseño (test POR_CONFIRMAR) |
| iOS sin CADisplayLink | Media | Medio | **Media** | RT-crítico = `DEVICE_UNRELIABLE` en iOS hasta portar (A.3) | abierto (hecho: `SpeedTimebase.ios.kt:14` fallback) |
| Banco de frecuencias (Creatividad EX06) sesgado/incompleto | Media | Medio | **Media** | auditoría de cobertura; CALIBRATING hasta validar; `rarity×plausibility` | abierto |
| Sesgo cultural/léxico en bancos textuales | Media | Medio | **Media** | mono-idioma declarado; balanceo; re-escaneo por idioma; inter-rater | abierto |
| Métrica comparativa con celda vacía colando 0 | Media | Medio | **Media** | INFRA_06: comparativa `null` si falta celda; `nMin≥20`/celda | abierto |
| `null→0` silencioso en algún adaptador | Baja | Alto | **Media** | property test `no-VALID nunca produce número`; `init` de `ScoreResult` | mitigado |
| Falso-positivo del copy-scanner (substring legítimo) | Baja | Bajo | **Baja** | allowlist de excepciones + test de regresión (ADR-05) | mitigado |
| Sub-agente de documentación filtra texto de proceso/budget-gate | Baja | Bajo | **Baja** | agentes de doc solo Read/Write; sin shell | mitigado |
| `ScoringEquations` usa `0.0` como sentinela (Decisiones) confundible con "sin datos" | Baja | Medio | **Baja** | el adaptador de Decisiones mapea `0.0`-sentinela a `INVALIDATED`, no a `value=0` | abierto (hecho: `PHD=0.0` si AGRS>0.75) |

**Riesgos residuales tras arreglos (lo que queda aunque P0 se complete):** practice-effect nunca es cero (solo
acotado por UNTRAINED+RCI); el banco de frecuencias de Creatividad es punto único de fallo; iOS queda
degradado hasta portar CADisplayLink; el sesgo cultural exige re-validación por idioma. Ninguno bloquea el v1
honesto si los claims se mantienen en "práctica/calibrando".
