Good, paths confirmed. The audit JSON plus repo grounding is sufficient. Delivering the markdown now.

---

# 9. Roadmap tecnico KMP

Backlog en formato issues. Prioridad de fase: **P0 infra** (desbloquea todo) → **P1 ejercicios mas defendibles** → **P2 prometedores con rediseño** → **P3 experimentales/caros**. Cada issue: titulo · prioridad · archivos probables · descripcion · criterios de aceptacion · tests obligatorios.

> Nota de modulos en repo: Enfoque = `focus/`, Velocidad = `speed/`, Lenguaje/Memoria/Decisiones/Emociones/Creatividad bajo `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/<modulo>/`. Velocidad ya tiene `TimebasePolicy.kt` + timebase Android autoritativo; Focus NO. Esto define el orden P0.

## FASE P0 — Infraestructura que desbloquea todo

### INFRA-1 · `ScoreResult` sellado + state machine de honestidad
- **Prioridad:** P0
- **Archivos probables:**
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/ScoreResult.kt` (NUEVO)
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/ScoreStateMachine.kt` (NUEVO)
  - `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/core/scoring/ScoreStateMachineTest.kt` (NUEVO)
- **Descripcion:** Tipo sellado unico que TODO ejercicio devuelve. Estados: `Valid(value, ci, n, nEffective)`, `Calibrating(reason, partialN)`, `InsufficientData(nNeeded, nHave)`, `Contaminated(cause)`, `UntrustedTimebase`, `NotApplicable`. Ningun estado no-`Valid` se serializa como numero. Regla nuclear: `null != 0` (regla NO negociable 7). Una unica funcion `canUpdateProfile(): Boolean` que solo es true en `Valid` con `nEffective >= nMin` y `confident`. El brain-profile consume EXCLUSIVAMENTE este tipo; jamas un `Double` crudo.
- **Criterios de aceptacion:**
  - Imposible construir un `Valid` con `n < nMin` (factory privado + `require`).
  - `Contaminated`/`UntrustedTimebase`/`InsufficientData` mapean a UI "calibrando"/"datos insuficientes", nunca a un percentil bajo.
  - Transiciones legales solo: `Calibrating → Valid` cuando se agrega evidencia inter-sesion; nunca `Valid → ` por debajo de su propio `nMin` (no degradar silenciosamente).
- **Tests obligatorios:** state machine exhaustiva (todas las transiciones legales/ilegales); property test "ningun `ScoreResult` no-Valid produce un numero"; canario "n=nMin-1 ⇒ InsufficientData".

### INFRA-2 · `TimingPolicy` (con signo) + gate `UNTRUSTED_TIMEBASE` portado a todos los modulos
- **Prioridad:** P0
- **Archivos probables:**
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/TimebasePolicy.kt` (EXISTE — extender con signo)
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/timing/TimingPolicy.kt` (NUEVO — superset compartido)
  - `shared/src/androidMain/kotlin/com/mindos/app/features/mindboost/speed/presentation/SpeedTimebase.android.kt` (EXISTE — fuente del expect/actual a portar)
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/**` (consumir gate)
- **Descripcion:** `TimebasePolicy.computeRtNanos` LANZA con rt<0; E5 Velocidad (cadencia anticipatoria) y E12/E5 anticipaciones REQUIEREN error temporal con signo. Crear `TimingPolicy` que devuelva `SignedTiming(deltaNanos, sign)` sin lanzar en negativos. El gate `device_unreliable`/`UNTRUSTED_TIMEBASE` hoy esta hardcodeado `frameDropFraction=0.0` en todo MindBoost (mencionado en E7/E10/EX08): debe medir frame-drop REAL via `frameTimeNanos` y, fuera de Android autoritativo, devolver `ScoreResult.UntrustedTimebase` que NO actualiza perfil (regla 1).
- **Criterios de aceptacion:**
  - `TimingPolicy` acepta RT negativos (adelanto) sin excepcion; signo preservado.
  - Cualquier metrica RT-critica en Focus/Emociones/Decisiones que corra sin timebase autoritativa retorna `UntrustedTimebase`, nunca un RT crudo.
  - `frameDropFraction` proviene de medicion real, no constante; expuesto al gate.
- **Tests obligatorios:** RT negativo no lanza (regresion del bug actual); `UntrustedTimebase` cuando timebase no es Android autoritativo; jank simulado > umbral ⇒ `Contaminated`.

### INFRA-3 · `AntiGamingFramework` con perfiles sinteticos adversarios
- **Prioridad:** P0
- **Archivos probables:**
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/antigaming/AntiGamingFramework.kt` (NUEVO)
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/simulation/SyntheticUserRunner.kt` (EXISTE — extender con adversarios)
  - `shared/src/commonTest/kotlin/com/mindos/app/features/mindboost/core/antigaming/*.kt` (NUEVO)
- **Descripcion:** Banco de generadores de respuesta adversarios reutilizable: `RhythmicTapper` (cadencia fija), `WaitingStrategist` (homogeneiza go-RT en stop-signal), `SpeedAccuracyHacker`, `RespondToAll` (go/no-go), `ButtonSpammer` (entropia ~0), `KeywordStuffer` (creatividad/lenguaje), `BankMemorizer`, `MinEffort`, `AbsentNoSearch` (busqueda visual), `RevealAllNoRead` (decisiones scan). Cada ejercicio define `assertSyntheticScoresLow(adversary)`. Regla NO negociable 2: anti-gaming "diseñado" no cuenta; solo implementado+testeado.
- **Criterios de aceptacion:**
  - Cada detector consumido por un ejercicio tiene su perfil sintetico que DEBE puntuar bajo o producir `Contaminated`.
  - Framework expone API uniforme: `run(exercise, adversary) -> ScoreResult` para que los tests de cada ejercicio lo enganchen.
- **Tests obligatorios:** uno por adversario × ejercicio afectado (matriz); ver §10 para criterios de fallo concretos.

### INFRA-4 · `ItemBankValidation` (banco grande, holdout, rotacion, HIGH_PRIOR, balanceo)
- **Prioridad:** P0
- **Archivos probables:**
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/bank/ItemBank.kt` (NUEVO)
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/bank/BankValidator.kt` (NUEVO)
  - bancos por modulo: `.../lenguaje/data/bank/*`, `.../decisiones/data/bank/*`, `.../emociones/data/bank/*`, `.../creatividad/data/bank/*`
- **Descripcion:** Contrato de banco con metadatos obligatorios: `size`, `exposurePartition`/`holdoutPartition`, `rotationPolicy`, `interRaterAgreement` (≥0.80 para bancos de juicio), `balance` (posicion de respuesta correcta, changeType, operatorType, longitud), `priorKnowledgeTag`. Resuelve los hallazgos recurrentes: "banco 6 fijos ⇒ memorizable", "correcto = 'b' en todos", "holdout fantasma (los eval se filtran y nunca se sirven)" (EM3). Regla NO negociable 5.
- **Criterios de aceptacion:**
  - Build falla si un ejercicio profile-updating usa un banco con `size < minBankSize` o sin holdout activo servido.
  - `selectNext()` nunca repite items del holdout en la particion de practica; partition de transferencia (UNTRAINED) servible.
  - Posicion de respuesta correcta balanceada (test que cuenta distribucion).
- **Tests obligatorios:** "holdout nunca servido en practica"; "balanceo de posicion correcta dentro de ±chi2"; "banco < minSize ⇒ build fail"; inter-rater < 0.80 ⇒ banco rechazado.

### INFRA-5 · `CopySafetyScanner` de build, multi-modulo, anti-negacion
- **Prioridad:** P0
- **Archivos probables:**
  - `shared/build.gradle.kts` (registrar task, espejo de `arenaTerminologyScanner` existente)
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/copy/ForbiddenLexicon.kt` (NUEVO)
  - hook a `check` (como el guard de terminologia Arena ya enganchado)
- **Descripcion:** Scanner que falla el build si aparece lexico de far-transfer (regla 4) o de apuesta, INCLUSO en negaciones y compuestos con guion (aprendizaje 2026-06-23: `\b` matchea a traves de guion). Cubre Lenguaje (hoy sin scanner — E6 Source Triage es P0 por riesgo "fake news"), Decisiones (E3 usa "apuesta"/"fichas"), Emociones, Creatividad. Ver §11 para la lista exacta.
- **Criterios de aceptacion:**
  - Detecta termino prohibido como substring de palabra y en compuesto (`non-clinical`, `anti-fake-news`).
  - Cubre `commonMain` + `commonTest` + `desktopTest` (los tests tambien filtran copy).
  - Enganchado a `check`; rojo bloquea merge.
- **Tests obligatorios:** cada lexema prohibido + su forma negada/compuesta ⇒ scanner rojo; copy permitido (§11) ⇒ verde; regresion del falso-positivo razonable (palabra que contiene substring legitimo, p.ej. "balance" semantico vs "bankroll").

### INFRA-6 · `MinSampleGate` central (n_min/n_efectivo/null/calibrating/agregacion inter-sesion)
- **Prioridad:** P0
- **Archivos probables:**
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/core/scoring/MinSampleGate.kt` (NUEVO)
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/metrics/DefaultMetricsEngine.kt` (EXISTE — consumir)
  - `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/MetricThresholds.kt` (EXISTE)
- **Descripcion:** Tabla unica por metrica (regla 3): `nMin` por celda, definicion de `nEffective`, condicion de `null`, de `calibrating`, de agregar inter-sesion, de bloquear claim, etiqueta de IC. Hoy hay `MIN_N=3`, `MIN_N_CONGRUENCY=3`, `MIN_POST_ERROR=3`, `accuracy min-n=5` dispersos — todos por debajo de los umbrales que pide la auditoria (≥20 por celda en condiciones comparativas; ≥30 stops para SSRT; ≥40-60 juicios para Brier). Centralizar y subir.
- **Criterios de aceptacion:**
  - Toda metrica comparativa (CONGRUENCY_EFFECT, SWITCH_COST, PRESSURE_DELTA, DUAL_TASK_COST) exige `nMin ≥ 20` por celda o retorna `InsufficientData`.
  - SSRT exige `nStops ≥ 30`; Brier exige `≥ 40` juicios agregados inter-sesion.
  - IC reportado en todo `Valid` comparativo.
- **Tests obligatorios:** tabla de umbrales (golden); "n por debajo ⇒ InsufficientData no 0"; agregacion inter-sesion suma celdas correctamente.

---

## FASE P0 — Ejercicios que requieren rebuild de runtime (bloqueantes propios)

### EX-E8-FOCUS · Dúo (Dual-task): capturar respuesta secundaria real o RETIRE
- **Prioridad:** P0 · **Modulo:** Enfoque
- **Archivos probables:** `.../focus/domain/exercise/E8DuoGenerator.kt`, `.../focus/domain/session/FocusSessionAssembler.kt`, `.../focus/presentation/FocusSessionStateHolder.kt`
- **Descripcion:** `DualTask(dual,go)` es un flag cosmetico sobre el MISMO go/no-go single-tap; no hay segunda tarea ⇒ `DUAL_TASK_COST` mide ruido (verdict REBUILD). El runtime de 1-respuesta no soporta dual de forma nativa.
- **Criterios de aceptacion:** capturar `accSecondary` de una tarea secundaria REAL al cierre del trial + `taskBalance`; gate `accSecondary >= umbral` o trial invalido. Si el runtime no puede capturar 2 respuestas/trial ⇒ **RETIRE** y absorber varianza en E9/E11 (regla 9).
- **Tests obligatorios:** sintetico que ignora la secundaria ⇒ `Contaminated`/invalid; `DUAL_TASK_COST` null si `accSecondary` no alcanza min-n.

### EX-E4-CHECKIN-DEC · Check-in Cognitivo: RT-CV no-confiable + semaforo no-maximizable
- **Prioridad:** P0 · **Modulo:** Decisiones
- **Archivos probables:** `.../decisiones/domain/checkin/*`, `.../decisiones/presentation/CheckInStateHolder.kt`
- **Descripcion:** RT-CV es 30% del CRI sobre epoch wall-clock + jitter de scheduler ⇒ varianza inflada; sin descarte de outliers por pausa larga. Es gate de readiness, no entrenamiento; gamificarlo rompe el gate honesto.
- **Criterios de aceptacion:** componente RT-CV marcada no-confiable hasta timebase frame-accurate (INFRA-2); descarte de outliers por pausa larga; min-n RT-CV con null honesto; presentado como SEMAFORO, no score maximizable; fatiga siempre covariable.
- **Tests obligatorios:** copy-scanner bloquea "score"/"puntuacion" en pantalla de check-in; outlier por pausa excluido; RT-CV ⇒ `UntrustedTimebase` sin timebase.

### EX-EM3-SEPARATOR · Separación 4-cat: inter-rater + holdout activo + detector de entropia
- **Prioridad:** P0 · **Modulo:** Emociones
- **Archivos probables:** `.../emociones/data/bank/SeparatorItems.kt`, `.../emociones/domain/session/EmotionSessionAssembler.kt`
- **Descripcion:** El banco `SEPARATOR_ITEMS` mide concordancia con el autor (no capacidad); holdout fantasma (los 12 eval se filtran y nunca se sirven); sin detector de entropia/boton-spam. Verdict BLOCK (REBUILD del banco, no del concepto).
- **Criterios de aceptacion:** inter-rater ≥80% por item con descarte de no-acuerdo (INFRA-4); holdout activo SERVIDO; detector de entropia (INFRA-3 `ButtonSpammer`); RT-floor proporcional a longitud del item.
- **Tests obligatorios:** `ButtonSpammer` ⇒ `Contaminated`; item con inter-rater <0.80 excluido; holdout efectivamente servido en eval.

### EX-E6-SOURCE-TRIAGE · Source Triage: copy-scanner Lenguaje + decoy bidireccional o RETIRE
- **Prioridad:** P0 · **Modulo:** Lenguaje
- **Archivos probables:** `.../lenguaje/data/bank/SourceTriageItems.kt`, `.../lenguaje/domain/scoring/*`, `shared/build.gradle.kts` (scanner Lenguaje)
- **Descripcion:** Riesgo de far-transfer "detectar fake news / fuentes fiables" = frontera etica/regulatoria; sin copy-scanner para Lenguaje; `authorityDecoy` no bidireccional; banco 6 fijos. Verdict BLOCK; transferLabel NO_TRANSFER.
- **Criterios de aceptacion:** scanner bloquea "fake news"/"desinformacion"/"medios"/"fuentes fiables" incluso negadas (INFRA-5); decoy de autoridad bidireccional; `sourceCount ≥ 4` en alto nivel; banco ≥40 con atributos EN el item; claim restringido a "aplicacion de criterios estructurales dados". Si el copy no se separa honestamente de "fake news" ⇒ **RETIRE** (regulatorio).
- **Tests obligatorios:** scanner rojo ante el lexico; decoy de autoridad alto-pero-irrelevante NO eleva seleccion; accuracy con banco fijo no supera holdout.

### EX-TRANSFER-PROBE-MEM · Sonda UNTRAINED + RCI cableados (corazon de honestidad de Memoria)
- **Prioridad:** P0 · **Modulo:** Memoria
- **Archivos probables:** `.../memory/domain/transfer/TransferProbePartition.kt`, `.../memory/domain/transfer/ReliableChangeIndex.kt`, `.../memory/domain/scheduler/*`
- **Descripcion:** RCI/`RealImprovementCriterion` sin caller; `TransferProbePartition` reservado pero nunca servido (Phase-8 ausente); formas paralelas A/B/C no equiparadas. Sin esto NINGUN ejercicio de Memoria puede afirmar mejora de capacidad.
- **Criterios de aceptacion:** formas paralelas equiparadas (equating) antes de comparar; gate de fatiga (no administrar con readiness HIGH); RCI gated por reliability ≥2-3 administraciones (`NotEnoughData` por defecto); claim solo con `isGenuine && reliable`; cero feedback; motores cableados a scheduler real.
- **Tests obligatorios:** RCI `NotEnoughData` con <2 admins; sonda UNTRAINED servida desde particion reservada; equating verificado en datos sinteticos; mejora declarada solo si `|RCI|>1.96`.

### EX-EM6-TOLERANCE · Tolerancia a la demanda: prueba de no-induccion o RETIRE
- **Prioridad:** P0 · **Modulo:** Emociones
- **Archivos probables:** `.../emociones/domain/exercise/ToleranceHoldGenerator.kt` (NUEVO o RETIRE), `.../emociones/domain/session/*`
- **Descripcion:** Prolongar la espera para medir "tolerancia a la frustracion" ES induccion emocional leve (frontera de seguridad); el nombre promete rasgo vital (PLACEBO_RISK). No producido hoy.
- **Criterios de aceptacion:** prueba documentada de no-induccion (unica carga = demora, sin contenido afectivo escalado); abandono NO penalizado (regla 8); catch-trials anti tap-spam; baseline previo; copy ultra-conservador; renombrar capacidad UI. Sin prueba de no-induccion ⇒ **RETIRE**.
- **Tests obligatorios:** abandono no reduce score; catch-trial detecta tap-spam; copy-scanner bloquea "frustracion"/"tolerancia vital".

---

## FASE P1 — Ejercicios mas defendibles (cablear primero)

> Todos comparten: consumir INFRA-1..6, gate timebase, min-n ≥20 por celda, perfil sintetico adversario testeado.

### EX-E1-VIGIA · Vigía (PVT): detector de cadencia
- **P1 · Enfoque ·** `.../focus/domain/exercise/E1*.kt`, `.../focus/domain/metrics/DefaultMetricsEngine.kt`
- Detector de cadencia (INFRA-3 `RhythmicTapper`) implementado+testeado; jitter≥150ms; slope null si `n_targets<20`; `UNTRUSTED_TIMEBASE` no actualiza perfil. **AC:** tap ritmico NO infla `SUSTAINED_FOCUS_INDEX`. **Tests:** `RhythmicTapper`⇒`Contaminated`; slope null con n<20.

### EX-E2-E5-CONGRUENCY · Filtro + Conflicto: fusionar salvo ortogonalidad
- **P1 · Enfoque ·** `.../focus/domain/exercise/{E2Flanker,E5}*.kt`
- E5 (MERGE) y E2 usan CONGRUENCY_EFFECT identico con el mismo `DefaultMetricsEngine` ⇒ brain-profile cuenta doble. Timebase autoritativa o perfil congelado; min-n≥20/celda; balanceo LEFT/RIGHT testeado; IES por condicion. **AC:** demostrar correlacion E2↔E5 < umbral (ortogonalidad empirica) o **MERGE en E2**. **Tests:** balanceo de lado dentro de chi2; correlacion sintetica E2/E5; efecto null si falta una celda.

### EX-E4-SSRT · Freno (Stop-Signal): detector waiting-strategy
- **P1 · Enfoque ·** `.../focus/domain/exercise/E4StopSignal*.kt`, `.../focus/domain/exercise/TrialClassifier.kt`
- Detector waiting-strategy (compara go-RT con/sin stop) implementado+testeado; `nStops≥30` enforced; stop multicanal; timebase o perfil congelado; SSRT flag `trustworthy` (p∈[0.25,0.75]). **AC:** waiting-strategist NO produce SSRT valido. **Tests:** `WaitingStrategist`⇒SSRT `Contaminated`; SSRT null con nStops<30.

### EX-E6-SWITCH · Cambio (Task-switching): previousRule + perseveracion
- **P1 · Enfoque ·** `.../focus/domain/exercise/E6CambioGenerator.kt`
- Registrar `previousRule` + aislar perseveracion; min-n≥20; cue dedicado verificado; `switchCost` null testeado. **AC:** CSI largo no contamina (mide lectura de cue separada). **Tests:** switchCost null si falta condicion; perseveracion aislada del switch genuino.

### EX-E9-MOT · Enjambre (MOT): medicion real de frame-drop
- **P1 · Enfoque ·** `.../focus/presentation/ui/components/FocusTrackingField.kt`, INFRA-2
- Medicion real de frame-drop ⇒ `Contaminated`; sonda balanceada 50/50 testeada; min-n por nivel o capacidad null; copy sin conduccion/deporte (INFRA-5). **Tests:** frame-drop simulado⇒`Contaminated`; sesgo si/no detectado; copy-scanner rojo ante "conduccion".

### EX-E10-RSVP · Flujo (RSVP): SOA efectivo por frameTimeNanos
- **P1 · Enfoque ·** `.../focus/presentation/ui/components/FocusStreamField.kt`, INFRA-2
- Medir SOA efectivo por `frameTimeNanos` + gate de jank⇒`Contaminated`; metrica honesta de deteccion SIN invocar "attentional-blink" hasta tener T1-T2 lag. **AC:** sin medicion de frame el bloque es `Contaminated`, no accuracy crudo. **Tests:** jank⇒`Contaminated`; copy no menciona attentional-blink.

### EX-E12-REENGAGE · Re-enganche: separar desenganche total de lapso
- **P1 · Enfoque ·** `.../focus/domain/exercise/E12*.kt`
- `reengagementLatency` null con <N lapsos; desenganche total separado (`Contaminated`); monotonia como eje medido; gate fatiga readiness-only; evidencia latencia↔RT_CV (no tedio). **Tests:** desenganche total no se cuenta como reenganche; latencia null con pocos lapsos.

### EX-E1-VEL-TINTE · Discriminación Cromática Binaria: jank-gate vivo
- **P1 · Velocidad ·** `.../speed/**`, `.../speed/domain/TimebasePolicy.kt`
- Jank-gate vivo (frameDrop real, INFRA-2), Android autoritativo, `axisRotation≥30%`, anclaje device-local probado, sintetico adversario puntua bajo y testeado, variante luminancia daltonismo-safe. **Tests:** `RandomTapper`⇒bajo; sin Android⇒`UntrustedTimebase`; ΔE device-local anclado.

### EX-E4-VEL-COMPUERTA · Go/No-Go Veloz Asimétrico: gate de comision prioritario
- **P1 · Velocidad ·** `.../speed/domain/MetricCalculator.kt`, `.../speed/domain/TrialEngine.kt`
- Gate de comision (LOWER por faRate prioritario sobre RAISE) testeado con sintetico impulsivo (baja dificultad, nunca acelera); techo incluye `faRate>0.30`; MIN_N de no-go con `faRate` null honesto; jank-gate vivo. **AC:** "impulsividad recompensada" eliminada. **Tests:** `RespondToAll`/impulsivo⇒nunca acelera; faRate null con pocos no-go.

### EX-E6-VEL-FLANKER · Flanker Express: CONGRUENCY_EFFECT en speed + estimulos no-linguisticos
- **P1 · Velocidad ·** `.../speed/domain/MetricCalculator.kt`
- `CONGRUENCY_EFFECT` computado en speed (deuda actual); ortogonalidad vs switch-cost/comision probada en sinteticos; estimulos no-linguisticos (flechas/formas) FORZADOS por copy-scanner; MIN_N por celda; jank-gate vivo. **AC:** elegir E6 como UNICA tarea de conflicto; MERGE/RETIRE E3-Velocidad salvo ortogonalidad. **Tests:** Stroop-palabra⇒scanner rojo (regla cultural); CONGRUENCY null si falta celda.

### EX-E1-EVIDENCE-LOCK · Evidence Lock (Lenguaje): banco ≥40 + HIGH_PRIOR + transfer
- **P1 · Lenguaje ·** `.../lenguaje/domain/scoring/ClosedScoringEngine.kt`, banco
- Wirear `selectNext`+`NearTransferVerifier`; activar flag `HIGH_PRIOR_KNOWLEDGE` por banco-tema; banco ≥40 con exposure/holdout/temas neutros; min-n≥12 + agregacion inter-sesion; copy-scanner Lenguaje en check. **Tests:** `BankMemorizer` no eleva trait; HIGH_PRIOR se eleva por tema conocido.

### EX-E4-ARG-XRAY · Argument X-Ray (Lenguaje): balancear posicion + delimitar vs Logica
- **P1 · Lenguaje ·** `.../lenguaje/data/bank/ArgumentItems.kt`, `ExerciseDefinition`
- Hoy "siempre b" rinde 100%. Balancear posicion de la correcta; banco ≥40 exposure/holdout; delimitar formalmente vs modulo Logica (`ReasoningScorer.justificationScore`) en `ExerciseDefinition`; o rebuild a labeling con `AnswerKey.RoleLabeling` + renderer + rubrica no-ambigüedad (inter-rater ≥80%). **Tests:** estrategia "siempre b" no supera azar; ortogonalidad vs Logica.

### EX-E5-SCOPE-NEG · Scope & Negation (Lenguaje): cuota "cero validas" + ortogonalidad vs E1
- **P1 · Lenguaje ·** `.../lenguaje/domain/scoring/*` (EvidenceSelection)
- Cuota ≥20% items sin inferencia valida (mata "marca-algo-siempre"); banco ≥50 balanceado por `operatorType` (≥8/operador) con `surfaceSimilarity` alto; ortogonalidad vs E1/Logica; min-n por operador; mono-idioma declarado. **AC:** si ortogonalidad con E1 baja ⇒ MERGE en familia "inferencia soportada y bajo alcance". **Tests:** "marca-algo" rinde bajo en items cero-validas; correlacion E1↔E5.

### EX-EX01-CREAT-INHIB · Supresión del Primer Impulso (Creatividad): banco prohibido + dedup
- **P1 · Creatividad ·** `.../creatividad/domain/exercise/Ex01*.kt`, banco
- Banco prohibido ≥40 objetos con inter-rater + near-duplicate/overlap testeado contra perfil spam + `inhibitionSuccessRate` como señal primaria + rotacion/holdout + copy-scanner del modulo. **AC:** fluidez de tecleo gameable eliminada. **Tests:** `KeywordStuffer`/basura-distinta⇒bajo; overlap detecta near-duplicates.

### EX-EX06-CREAT-RAREZA · Detección de lo Inverosímil (Creatividad): banco de frecuencias auditado
- **P1 · Creatividad ·** `.../creatividad/domain/scoring/*`, banco de frecuencias
- `rarityScore = media(1−freq_rel) × plausibilityRate` (absurdo no gana); CALIBRATING si cobertura baja; banco de frecuencias con cobertura declarada y auditada; plausibilidad prioriza banco-de-validas sobre heuristica; banco oculto + rotacion dura. **AC:** absurdo bien formado NO puntua alto. **Tests:** perfil absurdo⇒bajo; cobertura baja⇒CALIBRATING.

### EX-E1-BAYES · Bayes Arena (Decisiones): exclusion priori=posterior + AGRS real
- **P1 · Decisiones ·** `.../decisiones/domain/bayes/BayesEngine.kt`
- Timebase-gate `UNTRUSTED_TIMEBASE` (RT fuera de perfil); exclusion priori=posterior del BUR implementada+testeada (trials priori=posterior inflan BUR=1.0); `isDialDefault`/`dialPathEntropy` reales al AGRS (hoy hardcoded false); min-n=12 + null/calibrating; `seed!=0` + rotacion; copy-scanner decisiones. **Tests:** trial priori=posterior excluido del BUR; dial-constante detectado por AGRS.

### EX-E2-REVERSIBILITY · Triaje de Reversibilidad (Decisiones): entropia de swipe + min-n/tipo
- **P1 · Decisiones ·** `.../decisiones/domain/reversibility/*`
- Entropia de swipe computada y alimentada al AGRS (hoy `spatialEntropy=1.0` hardcoded); min-n=10/tipo + null/calibrating; marcadores rotan cross-sesion; lock-respetado premiado nunca tratado como lentitud; `seed!=0`; RT<400ms invalida trial. **AC:** este SI puede actualizar perfil (canUpdateProfile=yes) tras gates. **Tests:** swipe-constante⇒AGRS contaminado; accuracy null con n<10/tipo.

### EX-EM1-PAUSA · Pausa antes de actuar (Emociones): suelo go-accuracy invalidante
- **P1 · Emociones ·** `.../emociones/domain/exercise/Em1*.kt`, `.../emociones/domain/session/*`
- Suelo `goAccuracy>=0.6` invalidante (mata "never-respond" trivial); gate `UNTRUSTED_TIMEBASE` en metricas RT; test de paridad tono-respuesta; capacidad en allowlist UI. **AC:** comision 0% por no-responder ⇒ invalido, no "perfecto". **Tests:** `RespondToNothing`⇒invalid; RT-bars no actualizan perfil sin timebase.

### EX-EM2-POSTERROR · Recuperación tras error (Emociones): error genuino sin anticipaciones
- **P1 · Emociones ·** `.../emociones/domain/exercise/Em2*.kt`
- Error genuino = respuesta-en-ventana incorrecta excluyendo anticipaciones; suficiencia ≥3 pares probada por peor seed; gate timebase en termino slowing. **AC:** anticipacion NO cuenta como error. **Tests:** anticipacion excluida; suficiencia garantizada en peor seed; absorbe EM11/EM15 (ver merges P2).

---

## FASE P2 — Prometedores con rediseño / merges

- **EX-E3-FOCUS · Gatillo (Go/No-Go):** `INHIBITORY_CONTROL_INDEX` null-honesto; "responder a todo" (omision~0 + FA alto)⇒`Contaminated`; n≥20. Solapa E4/E1/E12.
- **EX-E7-FOCUS · Rastreo (busqueda visual):** ≥3 set sizes + R² umbral + pendientes presente/ausente separadas + gate `AbsentNoSearch` implementado+testeado.
- **EX-E11-FOCUS · Ancla (resistencia distraccion):** min-n≥20/condicion; balanceo baseline/intrusion; copy auditado declarando proxy de practica (INFRA-5).
- **EX-E13-FOCUS · Presión (deadline):** min-n≥20/condicion; deadline efectivo medido por frame; copy sin transferencia laboral/estres; reportar delta con IC.
- **EX-E2-VEL-DSST · Sustitución Símbolo-Dígito:** pendiente RT/keySize como PRIMARIA en `MetricCalculator`; `keySize ≤6` en runtime; `legendVisibleMs`; jank-gate. (MERGE candidato con throughput unico si no separa search de speed.)
- **EX-EX_NBACK-MEM · N-Back:** implementar `d'`+criterio en engine; descartar bloques con criterio extremo; copy-scanner prohibe IQ/Gf; mejora solo via sonda UNTRAINED. **Si no hay d' honesto ⇒ RETIRE** (redundante con span/updating).
- **EX-EX2-MEM · Updating de Ranura:** construir generador con fillers + sonda de una ranura + intrusion/binding separadas; doc que lo diferencia de n-back.
- **EX-EX5-MEM · Grid con Binding:** Cowan's K + d'; objetos baja-verbalizabilidad; separar location/binding/swap. Considerar MERGE con EX6.
- **EX-EX4-MEM · Pares Asociados A-B/A-C:** cablear fase A-C con overlap real; rotacion estricta; demora 24h anclada a `EpochClock`; separar intrusion/conjunction.
- **EX-EX3-MEM · Episodica diferida:** probe +24h anclada a `EpochClock`; distractor pre-inmediato; `PlateauDetector.FalseImprovement` vigilando gap inmediato-diferido.
- **EX-E3-FORECAST-DEC · Forecast Calibrado:** banco evidence-controlled validado (inter-rater ≥80% o probabilidad-por-diseño); outcome verificable; renombrar fuera de lexico de apuesta (INFRA-5); min-n=25 calErr; courage peso 0.15. **Si no hay banco defendible ⇒ RETIRE.**
- **EX-E8-AMBIG-DEC · Resolución bajo Ambigüedad:** VOI optimo matematicamente correcto y testeado con `payoffAsymmetry`; banda optimo±1 tolerante; exclusion priori=posterior. **Si no se separa de aversion-al-riesgo ⇒ RETIRE.**
- **EX-E2-SEM-SURGERY-LANG · Semantic Surgery:** banco ≥50 balanceado en 5 changeType + longitud/posicion pivote; decision explicita sobre localizacion o claim recortado a equivalencia.
- **EX-E3-COMP-LADDER-LANG · Compression Ladder:** `candidateCount≥4` en niveles altos; banco ≥30 balanceado en distortionType; claim "reconocimiento de fidelidad".
- **MERGES Emociones (colapsar, conservar como dificultad):** EM8→EM1; EM10→EM3; EM11→EM2; EM12→EM1 (secundaria); EM14→EM4; EM15→EM2; EM4 absorbe EM14. Cada merge: test que verifica que el catalogo no expone el clon como ejercicio profile-updating separado.
- **Creatividad rediseños:** EX03 (ventana de elaboracion gate duro anti keyword-stuffing), EX05 (cap de palabras anti-verbosidad), EX07 (onboarding + variante "escala"), EX02 (clasificador categoria↔lexico + RT por idea o MERGE en EX01).

---

## FASE P3 — Experimentales / caros / candidatos a RETIRE

- **EX-E3-VEL-MULTIVIA:** SWITCH_COST inexistente en speed + n por celda no garantizado. **MERGE/RETIRE a favor de E6-Velocidad** salvo ortogonalidad probada.
- **EX-E5-VEL-CADENCIA:** señal central es error negativo; rompe el motor de RT. **RETIRE si TimingPolicy con signo (INFRA-2) no se prioriza en release.**
- **EX-E7-VEL-RSVP / EX-E8-VEL-APUESTA:** bloqueados por jank real (P0). E8 mezcla velocidad+metacognicion: **RETIRE del modulo Velocidad** (reubicar en metacognicion).
- **EX-E7-PRESUPUESTO-DEC:** el mas caro (renderer drag nuevo); solver exacto = punto unico de fallo. Diferir; priorizar E1/E2/E4 reparados.
- **EX-E5-ESCANER-DEC / EX-E6-SESGO-DEC:** banco numerico validado o RETIRE; debiasing tiene transferencia debil.
- **EX-EX08-CREAT-IMPRO:** calibrador motor + timebase autoritativa multiplataforma o **RETIRE** (mide destreza de teclado, prohibido).
- **EX-EX04-CREAT-ANALOGIAS:** conservar SOLO modo proporcional A:B::C:? verificable; **RETIRE modo libre puntuable**.
- **mixed_session (Lenguaje):** marcar `isStrongUpdateAllowed=false` siempre; rotar 1 item aleatorio no-visto; test que verifica que nunca eleva trait a EMERGING/ESTABLISHED.
- **EX-EX8-MEM · Nombres-Rostros:** banco de caras diverso/licenciado o **MERGE en EX4**.
- **RETIRE directos:** EM5 (solapa EM-G + far-transfer), EM7→EM13, E7/E8-Lenguaje (no existen; RT-gated sin timebase), EX-EM13 (RT_CV gobernante sin timebase).

---

## Backlog cientifico

Validaciones, bancos, thresholds, estudios internos y **minimum viable evidence (MVE)** por metrica.

### Bancos a construir/validar (gating de claim)
| Banco | Modulo | Tamaño min | Validacion | Holdout/rotacion |
|---|---|---|---|---|
| Objetos prohibidos (inhibicion) | Creatividad EX01 | ≥40 | inter-rater respuestas prepotentes | si |
| Frecuencia lexica usos (rareza) | Creatividad EX06 | cobertura auditada | freq_rel empirica declarada | banco oculto |
| Separador 4-cat | Emociones EM3 | inter-rater ≥80%/item | descarte no-acuerdo | holdout SERVIDO |
| Valencia neutra (granularidad) | Emociones EM9 | inter-rater ≥80% | disjunto de SEPARATOR | si |
| Evidence-controlled (forecast) | Decisiones E3 | prob-por-diseño o IR≥80% | outcome verificable | si |
| Sesgo inequivoco/item | Decisiones E6 | IR≥80%, 1 sesgo/item | excludeRecent estricto | por familia ≥8 |
| Source triage (criterios EN item) | Lenguaje E6 | ≥40 | atributos en el item, no prestigio | decoy bidireccional |
| Evidence Lock / Scope-Neg / Arg | Lenguaje | ≥40-50 | balanceo posicion + operatorType | exposure/holdout |
| Caras diversas (nombres-rostros) | Memoria EX8 | licenciado | balance etnia/genero/edad | si |
| Formas paralelas A/B/C UNTRAINED | Memoria | equated | equating antes de comparar | particion reservada |

### Thresholds (MinSampleGate, INFRA-6)
- **Comparativas** (CONGRUENCY_EFFECT, SWITCH_COST, PRESSURE_DELTA, DUAL_TASK_COST, POST_ERROR): `nMin ≥ 20`/celda, IC obligatorio, null si falta una celda.
- **SSRT:** `nStops ≥ 30`, `p ∈ [0.25,0.75]` para `trustworthy`, comparar go-RT con/sin stop.
- **d' (n-back, change-detection, RSVP):** descartar bloques con criterio extremo; `nEffective` = max n con `d'≥1.0`.
- **Brier / calibration error:** `≥40-60` juicios agregados inter-sesion; null en sesion unica; anti-degeneracion de confianza.
- **RCI:** `|RCI|>1.96` reliable; gated por reliability de ≥2-3 administraciones; `NotEnoughData` por defecto.
- **BUR (bayes):** excluir trials priori=posterior; min-n=12.
- **Cowan's K:** `c*(hit−fa)` con d' honesto.

### MVE por metrica (que evidencia interna minima legitima el claim)
| Metrica | MVE para mostrar como señal | MVE para actualizar perfil |
|---|---|---|
| d' | bloque con criterio no-extremo, n≥20 | sonda UNTRAINED + RCI reliable |
| SSRT | nStops≥30 + p en banda + waiting-strategy descartada | timebase autoritativa |
| Brier | ≥40 juicios inter-sesion + confianza no-degenerada | nunca standalone; capa metacog separada |
| RCI | ≥2 administraciones equated | `isGenuine && reliable` |
| switch-cost | ≥20/celda + cue dedicado + perseveracion aislada | timebase autoritativa o perfil congelado |
| CONGRUENCY_EFFECT | ≥20/celda + balanceo lado + IES | ortogonalidad E2/E5 probada |
| Cowan's K | d' honesto + objetos baja-verbalizabilidad | banco rotado |

### Estudios internos (datos sinteticos + piloto)
1. **Ortogonalidad empirica** (regla 9): matrices de correlacion E2↔E5, E1↔E5-Lenguaje, EX5↔EX6, E1-Bayes↔E8, EX01↔EX02. Umbral MERGE si r>0.7.
2. **Equating de formas paralelas** A/B/C (Memoria) antes de cualquier RCI.
3. **Curva de memorizacion** por tamaño de banco (cuantas sesiones hasta plateau de familiaridad) ⇒ calibra `minBankSize`.
4. **Practice-effect vs transferencia**: delta entrenado vs UNTRAINED por ejercicio.
5. **Calibracion de deadlines** al RT base individual (E13, E8-Velocidad).
6. **Sesgo cultural/lexico** por banco textual (Lenguaje, Emociones, Decisiones).

---

# 10. Test plan adversarial

Para cada vector: **simula** · **sobre que ejercicios** · **criterio de fallo** (lo que NO debe ocurrir). Todos via `AntiGamingFramework` (INFRA-3); el test FALLA el build si el criterio de fallo se cumple.

### 10.1 Jank / frame-drop
- **Simula:** dispositivo gama media con frame-drop real inyectado en presentacion.
- **Sobre:** E9/E10-Focus, E1/E6/E7-Velocidad, MOT/RSVP.
- **Criterio de fallo:** el bloque produce un `Valid`/accuracy crudo en vez de `Contaminated`/`UntrustedTimebase`; o `frameDropFraction` sigue siendo 0.0 hardcoded.

### 10.2 Timebase falsa / device swap
- **Simula:** correr fuera de Android autoritativo; cambiar de dispositivo entre sesiones (latencia tactil distinta).
- **Sobre:** todo ejercicio con `rt_critico` (la mayoria de Focus/Velocidad/parte de Emociones/Decisiones).
- **Criterio de fallo:** RT/RT-CV/SSRT/timing actualiza perfil sin timebase autoritativa; o ΔE/latencia no anclada device-local cruza sesiones como comparable.

### 10.3 Rhythm tapping (tap ritmico)
- **Simula:** `RhythmicTapper` cadencia fija ±jitter mínimo.
- **Sobre:** E1-Vigía, E3-Gatillo, EM1/EM8/EM12 (anticipaciones).
- **Criterio de fallo:** `SUSTAINED_FOCUS_INDEX`/comision se infla; cadencia no detectada (detector ausente ⇒ test rojo).

### 10.4 Waiting strategy
- **Simula:** `WaitingStrategist` ralentiza go-RT para mantener p~0.5 en stop-signal.
- **Sobre:** E4-Freno (SSRT).
- **Criterio de fallo:** SSRT `trustworthy` cuando go-RT con-stop diverge de sin-stop; sin comparacion go-RT ⇒ rojo.

### 10.5 Speed-accuracy hacking
- **Simula:** sacrificar accuracy por velocidad y viceversa para maximizar la metrica explotable.
- **Sobre:** E4-Velocidad (comision), DSST, todos los RT-gated.
- **Criterio de fallo:** la dificultad ACELERA ante perfil impulsivo (faRate alto); IES no penaliza el trade-off.

### 10.6 Memorization
- **Simula:** `BankMemorizer` repite respuestas correctas memorizadas del banco fijo.
- **Sobre:** Lenguaje (E1/E2/E3/E4/E5/E6, mixed_session), Decisiones (bancos), Memoria (pares).
- **Criterio de fallo:** accuracy del banco fijo supera la del holdout/UNTRAINED y eleva el trait.

### 10.7 Answer leakage
- **Simula:** estrategia posicional ("siempre b"), holdout filtrado a practica, sonda predecible.
- **Sobre:** E4-Arg X-Ray (siempre-b=100%), EM3 (holdout fantasma), Source Triage.
- **Criterio de fallo:** "siempre b" supera azar; un item de holdout aparece en practica; posicion correcta no balanceada (chi2).

### 10.8 Keyword-stuffing
- **Simula:** `KeywordStuffer` incluye el tag/palabra requerida sin resolver; basura distinta.
- **Sobre:** EX03 (sintesis bajo restriccion), EX01/EX06 (creatividad), EX07.
- **Criterio de fallo:** `constraintSatisfactionRate`/fluency sube por presencia del tag; near-duplicates cuentan como ideas distintas.

### 10.9 Bank exhaustion
- **Simula:** muchas sesiones consecutivas hasta agotar items no vistos.
- **Sobre:** todos los de banco (Lenguaje, Decisiones, Emociones, Memoria, Creatividad).
- **Criterio de fallo:** el banco se repite sin rotacion/holdout; el "progreso" coincide con la curva de exposicion.

### 10.10 Cultural prior / HIGH_PRIOR_KNOWLEDGE
- **Simula:** usuario con conocimiento previo de prestigio/tema/trivia; banco textual sesgado.
- **Sobre:** Source Triage, Forecast, Bias, Nombres-Rostros, bancos textuales.
- **Criterio de fallo:** prior cultural infla accuracy y `HIGH_PRIOR_KNOWLEDGE` no se eleva; banco no balanceado por cultura/genero/etnia.

### 10.11 Abandonment manipulation
- **Simula:** abandono temprano en ejercicios emocionales/de frustracion.
- **Sobre:** EM6 (tolerancia), EM2/EM11/EM15 (post-error), EX08 (improvisacion bajo presion).
- **Criterio de fallo:** el abandono REDUCE el score o se trata como bajo rendimiento (viola regla 8 — debe ser señal de seguridad neutra).

### 10.12 Tecleo rapido (typing dexterity confound)
- **Simula:** teclado muy rapido vs muy lento por motricidad (no por cognicion).
- **Sobre:** EX01/EX05/EX08 (creatividad), cualquier metrica IBI/throughput.
- **Criterio de fallo:** velocidad de tecleo correlaciona con la señal sin normalizacion IBI por baseline motora; "lento = bloqueado".

### 10.13 Low-effort mode / button spam
- **Simula:** `ButtonSpammer`/`MinEffort` entropia ~0, una sola respuesta repetida.
- **Sobre:** EM3 (separacion), todos los multi-opcion, dial constante (Bayes), swipe constante (reversibility).
- **Criterio de fallo:** sin detector de entropia el bloque produce `Valid`; AGRS no marca dial/swipe constante.

### 10.14 Reveal-all / waiting (decisiones)
- **Simula:** `RevealAllNoRead` (escaner), sub/sobre-recolector (ambiguedad), reset masivo (presupuesto).
- **Sobre:** E5-Escaner, E8-Ambiguedad, E7-Presupuesto.
- **Criterio de fallo:** `alternativesScannedRatio` sube revelando-sin-leer (gate RT-por-revelacion <150ms ausente); fuerza bruta por reset no limitada.

### 10.15 Priori=posterior (bayes degenerado)
- **Simula:** no mover el dial cuando evidencia es nula.
- **Sobre:** E1-Bayes, E8-Ambiguedad.
- **Criterio de fallo:** trials priori=posterior cuentan como BUR=1.0 e inflan la señal.

---

# 11. Copy permitido y prohibido

## 11.1 Frases EXACTAS permitidas (UI)

**Estados de medicion (INFRA-1):**
- "Señal de practica"
- "Estimacion preliminar"
- "Datos insuficientes para estimar"
- "Calibrando tu linea base"
- "No actualizado por calidad de medicion"
- "Medicion contaminada — no contabilizada"
- "Proxy interno, no diagnostico"
- "Aun calibrando — vuelve tras mas sesiones"

**Construct honesto (proxy, no absoluto):**
- "Practica de atencion sostenida en esta tarea"
- "Tiempo de respuesta en esta tarea" (nunca "tu velocidad de procesamiento" sin Valid)
- "Aciertos frente a tu propia linea base"
- "Dificultad adaptativa"
- "Perfil cognitivo funcional"
- "Progreso contra tu linea base personal"
- "Reconocimiento de fidelidad del resumen" (Compression Ladder)
- "Aplicacion de los criterios dados en este ejercicio" (Source Triage)
- "Inferencia soportada por la evidencia mostrada" (Evidence Lock)

**Readiness / seguridad (Emociones, Check-in):**
- "Disponibilidad cognitiva de hoy" (semaforo, NO score)
- "Esto es un chequeo, no una nota"
- "Puedes parar cuando quieras" (abandono = señal, no culpa)

## 11.2 Lexemas/frases PROHIBIDAS (scanner, INFRA-5)

Detectar como substring de palabra Y en compuesto/negacion/guion (regla aprendizaje 2026-06-23).

**Far-transfer / vida real (regla 4):**
`inteligencia`, `IQ`, `CI`, `Gf`, `coeficiente intelectual`, `te hace mas listo`, `mejores decisiones`, `decides mejor`, `en la vida real`, `multitarea`, `productividad`, `trabajo`, `estudios`, `conduccion`, `conducir`, `al volante`, `regulacion emocional`, `inteligencia emocional`, `controlas tus emociones`, `autocontrol`, `personalidad`, `te distraes menos con el movil`, `usas menos el telefono`.

**Salud / clinico (boundary CLAUDE.md):**
`TDAH`, `ADHD`, `demencia`, `Alzheimer`, `ansiedad`, `depresion`, `diagnostico`, `clinico`, `terapeutico`, `tratamiento`, `cura`, `sintoma`, `trastorno`, `deterioro cognitivo`, `salud mental`.

**Apuesta (Decisiones E3/E5/E7 — renombrar):**
`apuesta`, `apostar`, `fichas`, `bote`, `casino`, `odds`, `cuota`, `bankroll`, `stake`, `bet`.

**Misinformacion (Lenguaje E6 — regulatorio):**
`fake news`, `noticias falsas`, `desinformacion`, `bulos`, `fuentes fiables`, `medios fiables`, `detectar mentiras`, `verificar hechos del mundo real`.

**Pseudociencia / promesa:**
`reconfigura tu cerebro`, `neuroplasticidad garantizada`, `potencia tu cerebro`, `desbloquea tu mente`, `% mas inteligente`, `aumenta tu CI N puntos`, `entrena tu cerebro como un musculo` (claim de transferencia).

**Constructos absolutos sin Valid (regla 6):** mostrar "velocidad de procesamiento", "memoria de trabajo", "control inhibitorio", "flexibilidad cognitiva" como NUMERO/percentil cuando el `ScoreResult` no es `Valid` ⇒ prohibido (lo enforca INFRA-1 en UI; el scanner cubre el copy estatico que lo afirme).

## 11.3 Backlog UX/copy

| Pantalla | Label/mensaje | Estado |
|---|---|---|
| Resultado de bloque | "Señal de practica registrada" + barra CALIBRATING (nunca 0) | INFRA-1 |
| Brain-profile radar | dimensiones en CALIBRATING ⇒ "—", jamas 0 | existente (respetar) |
| Insuficiencia | "Necesitamos N sesiones mas para estimar esto" (N de MinSampleGate) | INFRA-6 |
| Invalidacion | "Esta sesion no cuenta para tu perfil: medicion contaminada" | INFRA-2/3 |
| Timebase no fiable | "En este dispositivo solo medimos practica, no tu perfil" | INFRA-2 |
| Check-in (Decisiones) | semaforo verde/ambar/rojo + "chequeo, no nota" — sin numero maximizable | EX-E4-CHECKIN-DEC |
| Emociones (abandono) | "Puedes parar cuando quieras" — sin penalizacion visible | regla 8 |
| Source Triage | "Aplica los criterios de ESTE ejercicio" — sin "fake news" | EX-E6-SOURCE-TRIAGE |
| Creatividad calidad | calidad semantica como CALIBRATING, no nota | EX03/EX05/EX07 |
| Memoria mejora | "Mejora confirmada" solo si `isGenuine && reliable` (RCI) | EX-TRANSFER-PROBE-MEM |
| Forecast (renombrado) | "Confianza" / "estimacion" — nunca "apuesta"/"fichas" | EX-E3-FORECAST-DEC |

---

**Archivos clave verificados en repo (anclas para los issues):**
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/domain/TimebasePolicy.kt` (timebase con-signo a extender — INFRA-2)
- `shared/src/androidMain/kotlin/com/mindos/app/features/mindboost/speed/presentation/SpeedTimebase.android.kt` (expect/actual autoritativo a portar a Focus)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/speed/simulation/SyntheticUserRunner.kt` (base de INFRA-3)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/metrics/DefaultMetricsEngine.kt` (motor a consumir MinSampleGate/ScoreResult)
- `shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/domain/catalog/ExerciseCatalog.kt` + `ExerciseCatalogConsistencyTest.kt` (registry/implemented — punto de gating de exposicion)
- `shared/build.gradle.kts` (`arenaTerminologyScanner` existente = patron exacto para `CopySafetyScanner` INFRA-5)