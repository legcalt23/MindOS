# 03 · Roadmap por fases — P0 / P1 / P2 / P3

> Fuentes: `AUDIT_V2_TECNICO.md` §9 (issues INFRA-1..6, EX-*), §10 (test plan adversarial), §11 (copy),
> §12 (desbloqueo por ejercicio); `AUDIT_V2_PRODUCTO.md` §6/§7/§8; `AUDIT_V2_GROUND_TRUTH.md` (evidencia).
> Anclas de repo (`hecho`, verificadas en GROUND_TRUTH): `speed/domain/TimebasePolicy.kt`,
> `speed/presentation/SpeedTimebase.android.kt`, `speed/simulation/SyntheticUserRunner.kt`,
> `focus/domain/metrics/DefaultMetricsEngine.kt`, `focus/domain/catalog/ExerciseCatalog.kt`,
> `shared/build.gradle.kts` (`arenaTerminologyScanner`/`emotionsTerminologyScanner`). Rutas nuevas = `ruta probable`.

## Bloqueo estructural (leer antes de planificar)

```
                ┌──────────────────────── GATES P0 MINIMOS ───────────────────────┐
                │ INFRA-1 ScoreResult sellado (null!=0)                            │
                │ INFRA-2 TimingPolicy con signo + jank-gate REAL (no 0.0)         │
                │ INFRA-3 AntiGamingFramework + sinteticos adversarios             │
                │ INFRA-4 ItemBankValidation (size/holdout/inter-rater/balanceo)   │
                │ INFRA-5 CopySafetyScanner global en `check`                      │
                │ INFRA-6 MinSampleGate central                                    │
                └──────────────────────────────┬──────────────────────────────────┘
                                                │  (cada P1 declara cuales necesita)
   ╔════════════════════════════════════════════▼═══════════════════════════════════════╗
   ║ REGLA DURA: ningun ejercicio P1 que ACTUALICE PERFIL arranca hasta que sus           ║
   ║ dependencias P0 esten verdes. Invariante (b): Enfoque/Velocidad no tocan perfil       ║
   ║ sin timebase autoritativa + jank-gate medido. Invariante (e): banco no-validado       ║
   ║ bloquea perfil. Invariante (d): anti-gaming solo cuenta si testeado con sinteticos.   ║
   ╚══════════════════════════════════════════════════════════════════════════════════════╝
```

Un P1 puede empezar como **minijuego sin perfil** en cualquier momento (jugable, sin profile-update). Lo que el
bloqueo prohibe es **encender el profile-update** antes de los gates.

---

## FASE P0 — Infraestructura transversal + rebuilds de runtime

Objetivo: una pieza de infra desbloquea decenas de ejercicios. Se construyen primero. Detalle completo de cada
issue (criterios de aceptacion, tests, DoD) en `AUDIT_V2_TECNICO.md` §9.

### P0.A — Infra transversal (sin dependencias entre si; pueden ir en paralelo salvo donde se indica)

| Issue | Objetivo | Desbloquea | Dependencias |
|---|---|---|---|
| **INFRA-1 ScoreResult sellado + state machine** | Tipo unico que TODO ejercicio devuelve: `Valid/Calibrating/InsufficientData/Contaminated/UntrustedTimebase/NotApplicable`. `canUpdateProfile()` solo true en `Valid` con `nEffective≥nMin`. Enforza (a) `null!=0`. | Todos | — |
| **INFRA-2 TimingPolicy con signo + jank-gate real** | RT negativo sin lanzar (hoy `computeRtNanos` LANZA con rt<0); `frameDropFraction` medido por `frameTimeNanos`, no `0.0`; fuera de Android autoritativo → `UntrustedTimebase`. Enforza (b). | ~40 ejercicios RT/frame (Enfoque, Velocidad, partes Emociones/Decisiones) | — (extiende `TimebasePolicy.kt`) |
| **INFRA-3 AntiGamingFramework + sinteticos** | Banco de adversarios reutilizable (RhythmicTapper, WaitingStrategist, RespondToAll, ButtonSpammer, KeywordStuffer, BankMemorizer, AbsentNoSearch, RevealAllNoRead...). Cada ejercicio: `assertSyntheticScoresLow`. Enforza (d). | Todos los que usen un detector | extiende `SyntheticUserRunner.kt` |
| **INFRA-4 ItemBankValidation** | Contrato de banco: `size`, holdout servido, rotacion, `interRaterAgreement≥0.80`, balanceo de posicion. Build falla si un banco profile-updating es pequeño o sin holdout. Enforza (e). | Lenguaje, Decisiones, Emociones, Memoria, Creatividad | — |
| **INFRA-5 CopySafetyScanner global** | Scanner de build multi-modulo, anti-negacion/anti-guion (espejo de `arenaTerminologyScanner`), enganchado a `check`. Bloquea far-transfer/IQ/Gf/fake-news/regulacion/apuesta. Enforza (c). | Lenguaje, Decisiones, Emociones, Creatividad, AppStrings | patron en `shared/build.gradle.kts` |
| **INFRA-6 MinSampleGate central** | Tabla unica por metrica: `nMin` por celda, `nEffective`, condicion de null/calibrating/IC. Sube umbrales (hoy MIN_N=3..5) a ≥20/celda comparativa, ≥30 stops SSRT, ≥40-60 Brier. | Todas las metricas | consume INFRA-1 |

Orden interno recomendado: INFRA-1 e INFRA-2 primero (los demas referencian su tipo y su gate); INFRA-3/4/5/6 a
continuacion, en paralelo.

### P0.B — Rebuilds de runtime bloqueantes (cada uno es su propio bloqueador, no infra)

| Issue | Modulo | Objetivo | Depende de |
|---|---|---|---|
| **EX-E8-FOCUS** Duo | Enfoque | Capturar accuracy secundaria REAL o **RETIRE** (hoy `DualTask` es flag cosmetico → ruido) | INFRA-1, INFRA-6 |
| **EX-E4-CHECKIN-DEC** Check-in | Decisiones | RT-CV no-confiable hasta timebase + presentar como SEMAFORO no-maximizable | INFRA-2, INFRA-5 |
| **EX-EM3-SEPARATOR** Separacion 4-cat | Emociones | Inter-rater≥80% + holdout SERVIDO + detector de entropia | INFRA-3, INFRA-4 |
| **EX-E6-SOURCE-TRIAGE** Source Triage | Lenguaje | Copy-scanner Lenguaje + decoy bidireccional o **RETIRE** (riesgo "fake news") | INFRA-4, INFRA-5 |
| **EX-TRANSFER-PROBE-MEM** Sonda UNTRAINED + RCI | Memoria | Formas paralelas equiparadas + RCI gated + scheduler real (corazon de honestidad) | INFRA-1, INFRA-4 |
| **EX-EM6-TOLERANCE** Tolerancia demanda | Emociones | Prueba documentada de no-induccion o **RETIRE** (frontera de seguridad) | INFRA-5 |

**Salida de P0:** infra verde + estos 6 rebuilds resueltos (varios pueden terminar en RETIRE). Sin esto no
arranca ningun P1 con profile-update.

---

## FASE P1 — v1 defendible (los 9 ejercicios + reparaciones cableables)

Objetivo: cablear los gates de los ejercicios mas defendibles. **Cada uno declara sus dependencias P0 y no
enciende profile-update hasta tenerlas verdes.** Todos comparten: consumir INFRA-1..6, min-n≥20/celda, perfil
sintetico adversario testeado, ningun constructo absoluto en UI.

### P1 — Catalogo v1 que actualiza perfil (tras sus gates)

| Ejercicio | Modulo | Perfil | Dependencias P0 | Por que sobrevive |
|---|---|---|---|---|
| Span Adaptativo Dual (EX1) | Memoria | si | INFRA-1,4,6 | Accuracy-based, no RT-critico para el perfil; literatura solida |
| Updating de Ranura (EX2) | Memoria | si | INFRA-1,3,4,6 | Accuracy-based, ortogonal a span si se construye el generador |
| Grid Espacial con Binding (EX5) | Memoria | si | INFRA-1,4,6 | Visoespacial (Cowan's K) sin RT; absorbe EX6 |
| Pares A-B/A-C (EX4) | Memoria | si | INFRA-1,4,6 + EX-TRANSFER-PROBE | Accuracy + interferencia; absorbe EX8 |
| Episodica con Recall Diferido (EX3) | Memoria | si | INFRA-1,4,6 + EX-TRANSFER-PROBE | Unico que toca LTM |
| Triaje de Reversibilidad (E2) | Decisiones | si | INFRA-1,3,6 | Clasificacion accuracy poco dependiente de RT absoluto |
| Pausa go/no-go afectivo (EM1) | Emociones | si | INFRA-1,2,3,6 | Accuracy con suelo go; absorbe EM8/EM12 |
| Recuperacion post-error (EM2) | Emociones | si | INFRA-1,2,6 | Accuracy-recovery robusta; absorbe EM11/EM15 |
| Deteccion de lo Inverosimil (EX06) | Creatividad | si | INFRA-1,3,4,6 | Rareza×plausibilidad: el absurdo no gana; el mas defendible de Creatividad |

### P1 — Reparaciones de Enfoque/Velocidad (NO entran al perfil en v1)

Entran como **minijuegos sin perfil**; su profile-update queda bloqueado por INFRA-2 (timebase autoritativa +
jank-gate) hasta validacion fisica on-device. Issues: EX-E1-VIGIA, EX-E2-E5-CONGRUENCY (probar ortogonalidad o
MERGE), EX-E4-SSRT, EX-E6-SWITCH, EX-E9-MOT, EX-E10-RSVP, EX-E12-REENGAGE (Enfoque); EX-E1-VEL-TINTE,
EX-E4-VEL-COMPUERTA, EX-E6-VEL-FLANKER (Velocidad). Detalle en `AUDIT_V2_TECNICO.md` §9 (P1) y §12.

### P1 — Lenguaje cableable (no perfil hasta banco validado)

EX-E1-EVIDENCE-LOCK, EX-E4-ARG-XRAY (balancear posicion), EX-E5-SCOPE-NEG (cuota cero-validas + ortogonalidad).
Dependen de INFRA-4 + INFRA-5.

**Salida de P1:** v1 de ~9 ejercicios "practica/calibrando" con profile-update encendido tras gates; Enfoque/
Velocidad/Lenguaje jugables sin perfil. Es el release **GO condicional** de `01_VEREDICTO`.

---

## FASE P2 — Experimental v2 (prometedores con rediseño / merges)

Objetivo: ampliar el catalogo con rediseños y consolidar clones. Nada de P2 actualiza perfil hasta cerrar su
rediseño + ortogonalidad. Issues clave (`AUDIT_V2_TECNICO.md` §9 P2):

- **Memoria:** EX_NBACK (d' honesto o RETIRE), EX7 Orden Serial, MERGE EX6→EX5.
- **Enfoque:** EX-E3 (Go/No-Go), EX-E7 (busqueda), EX-E11 (ancla), EX-E13 (presion).
- **Velocidad:** EX-E2-DSST.
- **Lenguaje:** EX-E2 Semantic Surgery, EX-E3 Compression Ladder.
- **Decisiones:** EX-E3 Forecast (banco validado o RETIRE), EX-E8 Ambiguedad (VOI correcto o RETIRE).
- **Emociones (MERGES):** EM8→EM1, EM10→EM3, EM11→EM2, EM12→EM1, EM14→EM4, EM15→EM2; EM4 absorbe EM14; EM9
  (banco valencia o MERGE EM3). Cada merge: test que verifica que el catalogo NO expone el clon como ejercicio
  profile-updating separado.
- **Creatividad (rediseños):** EX03 (gate anti keyword-stuffing), EX05 (cap de palabras), EX07 (onboarding +
  variante "escala"), EX02 (clasificador o MERGE EX01).

Dependencia comun: estudios internos de **ortogonalidad empirica** (umbral MERGE si r>0.7), **equating** de formas
paralelas (Memoria) antes de cualquier RCI, y **curva de memorizacion** por tamaño de banco. Ver §9 "Backlog
cientifico".

**Salida de P2:** catalogo v2 con n-back (si d' honesto), updating/grid/pares maduros, Emociones colapsado a
EM1/EM2/EM3 limpios, Creatividad con calidad CALIBRATING.

---

## FASE P3 — Diferidos / caros / retirados

Objetivo: cerrar lo que no aporta señal nueva o es demasiado caro/riesgoso para el v1-v2.

- **MERGE/RETIRE por redundancia:** Velocidad E3 (→E6), Lenguaje E7/E8 (no existen), Memoria EX8 (→EX4),
  Emociones EM5/EM7 (→EM13).
- **RETIRE-condicional (si su unico camino tecnico no se prioriza):** Enfoque E8 Duo (si runtime 1-respuesta no
  captura secundaria), Velocidad E5 Cadencia (rompe el motor de RT salvo TimingPolicy con signo dedicada),
  Velocidad E7 RSVP / E8 Apuesta, Emociones EM6 (sin no-induccion), EM13 (RT_CV sin timebase),
  Creatividad EX04-libre / EX08 (destreza de teclado, prohibido por charter).
- **Diferidos caros:** Decisiones E5 Escaner / E6 Caza-Trampas / E7 Presupuesto (renderer drag nuevo, solver como
  punto unico de fallo, debiasing con transferencia debil).
- **mixed_session (Lenguaje):** `isStrongUpdateAllowed=false` siempre + rotar item no-visto + test que verifica
  que nunca eleva un trait a EMERGING/ESTABLISHED.

**Salida de P3:** catalogo limpio (≥18 ejercicios fuera o consolidados), sin ruido ni claims peligrosos.

---

## Dependencias criticas (resumen)

1. **INFRA-2 (timebase + jank-gate) bloquea** todo profile-update de Enfoque y Velocidad y las metricas RT de
   Emociones/Decisiones-E4. Es el desbloqueo de mayor impacto.
2. **INFRA-4 (bancos) + INFRA-5 (copy-scanner) bloquean** Lenguaje, Decisiones-narrativas, Emociones-juicio,
   Creatividad-banco.
3. **EX-TRANSFER-PROBE-MEM (RCI/UNTRAINED) bloquea** cualquier claim de "mejora de capacidad" en Memoria
   (EX1/EX4/EX3 solo afirman mejora via sonda UNTRAINED + RCI reliable).
4. **INFRA-3 (anti-gaming + sinteticos) bloquea** el profile-update de todo ejercicio gameable hasta que su
   adversario especifico puntue bajo en test (invariante (d)).
5. **INFRA-1 + INFRA-6** son transversales: ningun `Valid` sale sin `nEffective≥nMin`, y ningun estado no-`Valid`
   se serializa como numero (invariante (a) `null != 0`).

## Verificacion por fase (sin ejecutar nada desde este plan)

Cada issue trae sus `tests obligatorios` en `AUDIT_V2_TECNICO.md` §9/§10. Gate de salida de P0: el
`CopySafetyScanner` y los scanners de terminologia existentes en verde sobre `check`; la matriz adversarial §10
(jank, timebase-swap, rhythm-tap, waiting-strategy, memorization, answer-leakage, keyword-stuffing,
bank-exhaustion, button-spam, priori=posterior) FALLANDO el build cuando se cumple el criterio de fallo. Gate de
salida de P1: cada ejercicio v1 con su `assertSyntheticScoresLow` verde y su min-n enforced.
