# 01 · Veredicto ejecutivo — Catalogo cognitivo MindBoost

> Fuentes: `AUDIT_V2_PRODUCTO.md`, `AUDIT_V2_TECNICO.md`, `AUDIT_V2_GROUND_TRUTH.md`.
> Evidencia: `hecho` = verificado con `archivo:linea` en GROUND_TRUTH · `inferido` = deduccion del auditor ·
> `por_confirmar` = `POR_CONFIRMAR_EN_REPO`. Rutas no verificadas = `ruta probable`.

## 1. Estado actual del catalogo (en una frase)

El catalogo **NO esta listo para venderse como "perfil cognitivo"**. De ~81 ejercicios catalogados, **~9** pueden
tocar el perfil tras cablear gates, y varios solo en estado `calibrating`. Lo que bloquea el release **no es falta
de ejercicios**: es **infraestructura de medicion ausente, replicada decenas de veces** sobre un contenido inflado
por cantidad (al menos 18 ejercicios miden varianza ya cubierta por otro).

Lo que SI esta solido hoy (`hecho`, para no exagerar el problema):
- El **manejo `null != 0`** ya es consistente: `DimensionState.kt:35 require((CALIBRATING)==(value==null))`,
  `MetricValue.unavailable()`, barras `Double?`, `RciResult.NotEnoughData`. (Excepcion: `ScoringEquations` usa
  `0.0` como sentinela legitimo de penalizacion, no de "sin datos").
- El **anti-gaming psicometrico de RT** es real y testeado: filtro de anticipacion (Focus/Speed), guard RT<400
  AGRS (Decisiones), waiting-strategy via SSRT `trustworthy` (Focus), fast-commission (Focus/Speed),
  quality-gate (Speed), abandono (Emotions).
- La **cobertura de tests deterministicos** es alta (~203 archivos mindboost; generadores seed→secuencia,
  scorers, classifiers, 1 golden de metrics engine, simulacion sintetica por perfiles).

Eso hace el problema **arreglable con infra compartida**, no un rediseño total. Pero hasta arreglarlo, el catalogo
es honesto por convencion, no por construccion enforced.

## 2. Por que NO esta listo como perfil

Porque la mayoria de las metricas que alimentarian un "perfil" hoy **miden el dispositivo, el contenido
memorizable o estrategias de juego, no cognicion**:

- El cluster mas grande de ejercicios es **RT-critico** (Enfoque E1–E13, Velocidad E1–E8, partes de
  Memoria/Decisiones/Emociones/Creatividad) y **ninguno fuera de Speed/Android tiene timebase autoritativa**.
- El **gate de fiabilidad de dispositivo es codigo muerto**: el modelo existe y esta testeado, pero recibe
  ceros hardcodeados, asi que **nunca** marca un dispositivo como contaminado por jitter.
- Los **bancos de juicio son pequeños, fijos y sin validar** → el "progreso" es memorizacion, no aprendizaje.
- El **copy-scanner que impediria claims peligrosos no existe** para 5 de 7 modulos.

## 3. Los 3 bloqueadores estructurales (con evidencia)

### Bloqueador 1 — Jank-gate / timebase muertos en TODO MindBoost salvo Speed/Android
`hecho`. El modelo de riesgo de dispositivo existe (`TimebasePolicy.deviceTimingRisk` :66-73;
`SessionQualityEvaluator.kt:46-69` clasifica CONTAMINATED/LOW/HIGH/ACCEPTABLE) **pero su entrada de jank esta
cableada a constantes**: `SpeedSessionStateHolder.kt:259-260` pasa `frameDropFraction = 0.0` y
`criticalFrameJitter = false` literalmente. **No hay ningun colector de `frameTimeNanos` que calcule drops/jitter
en ningun modulo.** La unica timebase autoritativa frame-accurate (Choreographer + MotionEvent, misma base
`ANDROID_UI_UPTIME_NANOS`) esta **aislada en `speed/Android`** (`SpeedTimebase.android.kt:41,59-69`;
`TimebasePolicy.kt:11-14,54`). Focus mide RT con `MonotonicClock` (`MonotonicClock.kt:13-21`,
`FocusFixedBlockRunner.kt:83,86`) **a nivel de corrutina, no de vsync**. Logic/Decisiones/Emotions miden sobre
`EpochClock` wall-clock (`EpochClock.kt:31-33`). iOS no tiene `CADisplayLink` (solo en docs).
**Consecuencia:** cada metrica RT/frame mide GPU+pantalla+latencia tactil, no cognicion. Invalida ~40 ejercicios
para profile-update. Es un **P0 transversal** y prerequisito de todo Enfoque/Velocidad.

### Bloqueador 2 — Anti-gaming conductual diseñado, no implementado
`hecho`. El nucleo psicometrico de RT esta implementado+testeado (ver §1). Pero el anti-gaming "conductual
avanzado" **no existe sobre datos reales**:
- **Cadencia/ritmo de tap real:** existe SOLO como feature sintetica en `arena`
  (`FraudSignalDetectors.kt:22-24 inputCadenceMismatch`; `AscensionTrials.kt:108`). **Ningun ejercicio MindBoost
  mide la cadencia de toques del usuario real.** Arena es 100% simulacion offline sin pipeline de telemetria.
- **Sesgo por posicion/longitud:** **no existe**. El unico gancho —`entropyFactor` del AGRS— esta **neutralizado**:
  los dos holders reales fijan `spatialEntropy=1.0` (`BayesArenaStateHolder.kt:71`,
  `ReversibilityTriageStateHolder.kt:73`, comentario "no spatial grid → no spatial-gaming signal").
- **Keyword-stuffing:** **no existe**; la capa de integridad de Creatividad esta BLOQUEADA por diseño
  (`CreativityAiLayers.kt:44 "would score anti-gaming signals. BLOCKED"`); el validador solo chequea timing
  monotonico, no-vacio y minIdeas (`CreativityResponseValidator.kt:18-26`).

**Consecuencia:** un competidor con dos telefonos y una tarde gamea media tabla (tap ritmico en E1; "siempre b" en
Lenguaje E4 —correcta="b" en los 6 items—; "responder a todo" en Go/No-Go; boton-spam en EM3 sin detector de
entropia). **Invariante (d): anti-gaming solo cuenta si esta implementado+testeado con perfiles sinteticos
adversarios.** Hoy la mitad no lo esta.

### Bloqueador 3 — Bancos sin inter-rater ni holdout activo
`hecho`. Inventario real: Logic 6 items fijos en orden fijo (`LogicContentProvider.kt:29-36`); Language 36 fijos
(6 bancos × 6) + 7 demo; Emotions PAUSA_WORDS(24) + SEPARATOR_ITEMS(40); Creativity 14 prompts; Decisiones
procedural numerico sin banco ni validacion. **Cero discriminability psicometrica, cero `interRater`, cero
`culturalLoad`, cero `exposure`, cero holdout activo** (el `SeparatorItem.isEval` reserva 12/40 pero
`EM3SeparadorGenerator.kt:27` los filtra `{!it.isEval}` para una "Ola-4" no implementada → **holdout fantasma**),
cero validacion empirica.
**Consecuencia:** memorizables en 1–2 sesiones; el "progreso" es la curva de exposicion. **Invariante (e): banco
pequeño/no-validado bloquea perfil.**

## 4. Riesgo dominante (no tecnico): el screenshot regulatorio

`inferido` (riesgo) sobre base `hecho` (cobertura de scanners). Hoy el copy real es defensivo y limpio
(disclaimers tipo "no es un test de inteligencia", `TransferReport.kt:54`, `MindBoostHubScreen.kt:201`). **Pero
nada en el build lo impone fuera de `arena` y `emotions`** (`shared/build.gradle.kts` solo registra tasks
`arena*`/`emotions*`). El lexico de apuesta y de inteligencia solo se bloquea en `arena`; `decisiones` (roza
apuesta/probabilidad) y `language` (`ClaimEngine.kt:31 ROBUST_GENERALIZATION → "evidencia fuerte de transferencia
amplia"`) quedan FUERA de cualquier gate. Diseños propuestos como Source Triage ("detectar fake news"), EM6
("tolerancia a la frustracion"), EM13 ("regulacion") y Decisiones E3 (lexico "apuesta") son claims de
far-transfer / salud / juego que un copy-scanner inexistente **no frenaria en build**. **Invariante (c): cero
claims de far-transfer/clinico/IQ/fake-news/regulacion, ni para negarlos.**

## 5. Release recomendado (GO condicional)

**v1 honesto de ~9 ejercicios** (`AUDIT_V2_PRODUCTO.md` §6), todos accuracy-based o de clasificacion, ninguno
RT-critico para el perfil, todos etiquetados "practica / calibrando", ninguno mostrando constructo absoluto:

1. Span Adaptativo Dual (Memoria) · 2. Updating de Ranura (Memoria) · 3. Grid Espacial con Binding (Memoria) ·
4. Pares Asociados A-B/A-C (Memoria) · 5. Episodica con Recall Diferido (Memoria) · 6. Triaje de Reversibilidad
(Decisiones) · 7. Pausa antes de actuar / go-no-go afectivo (Emociones) · 8. Recuperacion post-error (Emociones) ·
9. Deteccion de lo Inverosimil / Rareza (Creatividad).

Condicion **GO** (sin esto es NO-GO): caen los 3 bloqueadores en su minimo P0 — copy-scanner global enganchado a
`check`, anti-gaming testeado con sinteticos para cada ejercicio que lo use, bancos con inter-rater≥80% + holdout
servido + rotacion para los de juicio. Enfoque y Velocidad **no entran a v1**: entran como **minijuegos sin
perfil** (jugables, sin profile-update) hasta jank-gate vivo + timebase autoritativa.

## 6. Release prohibido (NO-GO duro)

- **Listar los ~81 como "perfil cognitivo".** Infla la varianza (≥18 redundantes), incluye ruido (E8 Duo sin
  segunda tarea, RT/frame sin timebase) y claims peligrosos sin scanner.
- **Cualquier ejercicio RT/frame-critico actualizando perfil** sin timebase autoritativa + jank-gate medido
  (invariante (b)).
- **Cualquier banco de juicio sin holdout activo + inter-rater** elevando un trait (invariante (e)).
- **Cualquier copy** con "fake news", "IQ/inteligencia/Gf", "regulacion emocional", "tolerancia a la
  frustracion", "mejores decisiones vitales", "apuesta/fichas" (invariante (c)).
- **EM6 (tolerancia a la demanda)** sin prueba documentada de no-induccion: prolongar la espera para medir
  frustracion **es** induccion emocional leve (frontera de seguridad no-clinica).

## 7. Decision final

**NO-GO para perfil completo. GO condicional** para el v1 de ~9 ejercicios "practica/calibrando", **si y solo si**
se completan los gates P0 minimos (jank-gate vivo + timebase autoritativa portada + anti-gaming testeado con
sinteticos + bancos validados con holdout/inter-rater + copy-scanner global en `check` + min-n central con
null-honesto).

Secuencia obligatoria: **P0 infra → P1 v1 → P2 experimental → P3 diferido/retirado**, con el bloqueo duro de que
**ningun P1 que actualice perfil arranca antes de sus gates P0**. Detalle en `03_ROADMAP_FASES_P0_P1_P2_P3.md`;
veredicto por ejercicio en `02_DECISION_PRODUCTO_CATALOGO.md`.
