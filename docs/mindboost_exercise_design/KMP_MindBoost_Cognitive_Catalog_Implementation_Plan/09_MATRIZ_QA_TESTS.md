# 09 — Matriz QA de tests (detallada)

> Todo test adversarial corre vía `AntiGamingFramework` (`INFRA_03`, D1–D12) y **falla el build** si el
> criterio de fallo se cumple. **Invariantes que se repiten en cada matriz:** ninguna sesión
> contaminada/insuficiente/no-fiable actualiza perfil; `null != 0`; Enfoque/Velocidad sin profile-update
> hasta timebase+jank reales; cero claims de far-transfer ni para negarlos; anti-gaming solo cuenta si
> está implementado+testeado con adversarios; banco pequeño/no-validado bloquea perfil.
>
> Comandos de validación (`por_confirmar` nombres exactos de tarea): `:shared:desktopTest` (suite
> adversarial + state machine pura + property-based) · `:shared:detekt` (suite pesada en `desktopTest` para
> no entrar al ratchet) · `:shared:arenaTerminologyScanner`/`emotionsTerminologyScanner` (existentes) +
> el nuevo `mindboostCopySafetyScanner` (INFRA-05).
> Leyenda de estado: `hecho` · `inferido` · `por_confirmar` (`POR_CONFIRMAR_EN_REPO`).

---

## 9.1 Infra × tests obligatorios (con nombres de test enumerados)

| Infra | Test (nombre concreto) | Criterio de fallo (lo que NO debe ocurrir) | Estado |
|---|---|---|---|
| **INFRA-01 `ScoreResult`** | `state machine exhaustiva: transiciones legales/ilegales` | una transición ilegal compila/pasa | `inferido` |
| | `VALID es el UNICO estado con updatesProfile==true` | otro estado pone `updatesProfile=true` | |
| | `ningun estado no-VALID emite primarySignal no-null` (init/property) | un no-`VALID` produce un número | |
| | `n=nMin-1 ⇒ INSUFFICIENT_N con primarySignal==null` (canario) | n bajo produce `Valid`/`0` | |
| | `preliminary==false solo con confidence>=0.7 y agregacion inter-sesion` | robustez falsa | |
| **INFRA-02 `TimingPolicy`/jank** | `signedTiming_negativeRt_doesNotThrow` (regresión del bug `computeRtNanos`) | RT negativo lanza | `hecho` (bug real) |
| | `sin Android autoritativo ⇒ DEVICE_UNRELIABLE/UntrustedTimebase` | RT crudo actualiza perfil | |
| | `jank simulado > umbral ⇒ Contaminated` | bloque produce accuracy crudo | |
| | `frameDropFraction proviene de medición real (no 0.0 hardcoded)` | sigue `0.0`/`false` hardcodeado | `hecho` (gate muerto) |
| | `criticalFrameJitter solo gatea en frame del onset` | drop a mitad de bloque invalida RT ya capturado | |
| **INFRA-03 `AntiGaming` (D1–D12)** | `AntiGamingRedTeamTest: every adversarial profile is caught by its detector` | un adversario produce `Valid` | D2/D4 `hecho`; resto `ABSENT`/`PARTIAL` |
| | `honest user triggers no detector` (control FP) | `HonestUserProfile` dispara un detector | |
| | `safety exit in emotional module never penalized` (Regla #8) | abandono emocional reduce score | |
| | `bajo MIN_* (MIN_IRI=12/MIN_CHOICE=10/MIN_REPEATED=4) ⇒ idle()` | detector afirma con n insuficiente | |
| **INFRA-04 `ItemBank`** | `holdout nunca servido en practica` | un ítem de holdout aparece en práctica | `hecho` (banco fijo) |
| | `balanceo de posicion correcta dentro de ±chi²` | "siempre b" supera azar | |
| | `banco < minSize ⇒ build fail` | banco pequeño compila como profile-updating | |
| | `inter-rater < 0.80 ⇒ item rechazado` | ítem sin acuerdo entra al pool de juicio | |
| **INFRA-05 `CopyScanner`** | `cada lexema prohibido ⇒ rojo` (far-transfer/clínico/apuesta/misinfo) | léxico prohibido pasa | `hecho` (cobertura parcial: solo arena+emotions) |
| | `negacion/compuesto del lexema ⇒ rojo` (`non-clinical`, `anti-fake-news`) | compuesto con guion escapa el `\b` | |
| | `copy permitido ⇒ verde` + falso-positivo controlado (`balance` semántico ≠ `bankroll`) | falso positivo razonable rompe el build | |
| **INFRA-06 `MinSampleGate`** | `tabla de umbrales (golden)` | umbral cambia sin actualizar golden | `inferido` |
| | `n por debajo ⇒ InsufficientData (no 0)` | n bajo produce `0` | |
| | `agregacion inter-sesion suma celdas correctamente` | celdas no agregan / se duplican | |

---

## 9.2 Ejercicio × tests (mínimo por ejercicio profile-updating)

> Cada ejercicio profile-updating tiene **5 familias de test**: Unit (scorer determinista por seed) ·
> Adversarial (≥1 perfil D1–D12 aplicable) · Integración (`ScoreResult` correcto + `null≠0`) · Golden de
> scoring · Copy-scanner. Filas por ejercicio donde aplique.

| Ejercicio | Módulo | Unit (scorer/seed) | Adversarial (perfil ⇒ resultado) | Integración (`ScoreResult` + null≠0) | Golden | Copy-scanner |
|---|---|---|---|---|---|---|
| E1 Vigía (PVT) | Enfoque | `slope==null` si n<20; determinista por seed | `RhythmicTapper`(D1)⇒`Contaminated`; `Anticipator`(D4)⇒trial descartado | RT-crítico sin Android⇒`DEVICE_UNRELIABLE` | `SUSTAINED_FOCUS_INDEX` golden | sin "concentración en la vida diaria" |
| E2 Filtro | Enfoque | `CONGRUENCY_EFFECT` por seed; balanceo LEFT/RIGHT | `LeftStubborn`(D6)⇒entropía baja flag | `null` si falta una celda; IES por condición | congruency golden | flechas no Stroop-palabra |
| E4 Freno (SSRT) | Enfoque | SSRT Verbruggen por seed | `WaitingStrategist`(D2)⇒SSRT `untrustworthy` | `null` SSRT con nStops<30; `p∈[0.25,0.75]` | SSRT golden | — |
| E6 Cambio (switch) | Enfoque | `switchCost` por seed; `previousRule` | perseveración aislada del switch | `switchCost==null` si falta condición | switch-cost golden | sin "multitarea" |
| E9 Enjambre (MOT) | Enfoque | sonda 50/50 por seed | jank simulado⇒`Contaminated` | min-n/nivel o capacidad `null` | — | sin "conducción/deporte" |
| E10 Flujo (RSVP) | Enfoque | detección por seed | jank⇒`Contaminated` | SOA por `frameTimeNanos`; sin "attentional-blink" | — | sin attentional-blink |
| E1 Tinte Crítico | Velocidad | ΔE binario por seed | `RandomTapper`⇒bajo; `SpeedAccuracyHacker`(D3) | sin Android⇒`UntrustedTimebase`; ΔE device-local | RT golden | luminancia daltonismo-safe |
| E4 Compuerta | Velocidad | faRate por seed | `RespondToAll`(D4)⇒nunca acelera | `faRate==null` con pocos no-go | comisión golden | — |
| E6 Flanker Express | Velocidad | `CONGRUENCY_EFFECT` por seed | conflicto vs switch (ortogonalidad) | `null` si falta celda; min-n≥20/celda | congruency golden | Stroop-palabra⇒rojo |
| E2 DSST | Velocidad | pendiente RT/keySize por seed | `BankMemorizer`(D5) (símbolos repetidos) | `keySize≤6`; `legendVisibleMs` | throughput golden | — |
| E1 Evidence Lock | Lenguaje | `ClosedScoringEngine` por seed | `BankMemorizer`(D5)⇒no eleva trait | accuracy banco fijo ≤ holdout | scoring golden | `HIGH_PRIOR` por tema |
| E4 Argument X-Ray | Lenguaje | scorer por seed | "siempre b"⇒no supera azar | balanceo posición ±chi² | scoring golden | ortogonalidad vs Lógica |
| E5 Scope & Negation | Lenguaje | EvidenceSelection por seed | "marca-algo"⇒bajo en cero-válidas | min-n por operador; mono-idioma | scoring golden | — |
| E6 Source Triage | Lenguaje | scorer por seed | `authorityDecoy` alto NO eleva selección | banco≥40; decoy bidireccional | scoring golden | rechaza "fake news"+compuestos |
| EX_TRANSFER_PROBE | Memoria | RCI por seed; equating | `BankGrinder`(D7)⇒reexposición flag | `NotEnoughData` con <2 admins; cero feedback | RCI golden | mejora solo `isGenuine&&reliable` |
| EX4 Pares A-B/A-C | Memoria | overlap por seed | `RoteMemory`(D5) | demora 24h anclada `EpochClock` | recall golden | — |
| EX5 Grid (binding) | Memoria | Cowan's K + d' por seed | change-detection mode | separar location/binding/swap | K/d' golden | objetos baja-verbalizabilidad |
| E1 Bayes Arena | Decisiones | BUR por seed | priori=posterior excluido (D15); `PositionBias`(D6) | min-n=12; dial-default al AGRS real | BUR golden | rechaza "apuesta/fichas" |
| E2 Reversibilidad | Decisiones | por seed | swipe-constante⇒AGRS contaminado | accuracy `null` con n<10/tipo; RT<400⇒inválido | golden | — |
| EM1 Pausa | Emociones | go/no-go afectivo por seed | `RespondToNothing`⇒inválido (no "perfecto") | suelo `goAccuracy≥0.6`; RT⇒no perfil sin timebase | golden | TransferLabel render |
| EM2 Post-error | Emociones | `POST_ERROR_RECOVERY` por seed | anticipación excluida del "error" | suficiencia ≥3 pares peor seed | parity golden | absorbe EM11/EM15 |
| EM3 Separación 4-cat | Emociones | clasificación por seed | `ButtonSpammer`(D6/D13)⇒`Contaminated` | holdout servido; IR<0.80 excluido | golden | léxico clínico⇒rojo |
| EX01 Supresión (Creat.) | Creatividad | fluidez/dedup por seed | `KeywordStuffer`(D8); near-duplicate | `inhibitionSuccessRate` primaria | — | sin "mide tu creatividad" |
| EX06 Rareza (Creat.) | Creatividad | `rarityScore` por seed | absurdo bien formado⇒bajo | CALIBRATING si cobertura baja | freq golden | — |

> Ejercicios NO profile-updating (experimentales v2, retirados, fusionados): se testean en `07` y `08` con
> sus `*_noProfileWrite()` / `*_doesNotEmitContribution()`. No entran a esta matriz como filas
> profile-updating.

---

## 9.3 Plataforma × timebase

| Plataforma | Timebase real | Tipo (`TimebaseQuality`) | Resultado | Test |
|---|---|---|---|---|
| Android API34+ (speed) | Choreographer + MotionEvent `eventTimeNanos`, misma base uptime-nanos | `AUTHORITATIVE` | RT puede actualizar perfil **si jank real medido + demás gates** | `android_authoritative_canUpdate_whenJankMeasuredLow` |
| Android <34 (speed) | `eventTime*1e6` (`MILLIS_DERIVED`) | `NonAuthoritative(MILLIS_DERIVED)` | degradado; RT de cola corta no autoritativo | `androidApi33_millisDerived_isNonAuthoritative` |
| Android (focus/otros) | `MonotonicClock` (`TimeSource.Monotonic`, no frame-accurate) | `MONOTONIC_NONFRAME` | RT NO actualiza perfil hasta portar timebase (INFRA-02) | `focus_monotonic_doesNotUpdateProfile_forRtCritical` |
| Android (logic/decisiones/emotions) | `EpochClock` (`Clock.System` epoch-millis) | `WALLCLOCK_DERIVED` | apto para deliberación (segundos), NO para RT fino | `wallclock_rtCritical_isDeviceUnreliable` |
| iOS | CADisplayLink **ausente** (delega a fallback monotónico) | `UNTRUSTED`/`NonAuthoritative(NO_DISPLAY_LINK)` | `DEVICE_UNRELIABLE` → jugable, no perfil | `ios_noDisplayLink_neverAuthoritative` |
| Desktop | downgrade explícito (`SYSTEM_CLOCK_MONOTONIC`) | `NonAuthoritative` | `DEVICE_UNRELIABLE` → no perfil (jugable) | `desktop_downgrade_neverAuthoritative` |

> **Gate `device_unreliable` HOY:** existe el MODELO (`TimebasePolicy.deviceTimingRisk`, `SessionQualityEvaluator`)
> pero NO la medición — `frameDropFraction=0.0`/`criticalFrameJitter=false` están hardcodeados
> (`SpeedSessionStateHolder.kt:259-260`, `hecho`). Hasta que INFRA-02 mida frames reales, ningún `ScoreResult`
> de Speed puede afirmar "medido en timebase autoritativa".

---

## 9.4 Banco × validación

| Banco | Módulo | Tamaño actual / min objetivo | inter-rater | holdout | rotación | balanceo | Estado real |
|---|---|---|---|---|---|---|---|
| Logic (authored fijo) | Lógica | 6 / — | — | no | no (orden fijo) | — | `SMALL_UNVALIDATED` `hecho` (memorizable 1-2 sesiones) |
| Evidence Lock | Lenguaje | 6 / ≥40 | (objetivo) | sí | sí | posición correcta | `SMALL_UNVALIDATED` `hecho` |
| Scope & Negation | Lenguaje | 6 / ≥50 (≥8/operador) | (objetivo) | sí | sí | `operatorType` | `SMALL_UNVALIDATED` `hecho` |
| Argument X-Ray | Lenguaje | 6 / ≥40 | (objetivo) | sí | sí | posición correcta | `SMALL_UNVALIDATED` `hecho` |
| Source Triage | Lenguaje | 6 / ≥40 | atributos EN el ítem | decoy bidireccional | sí | — | `SMALL_UNVALIDATED` + riesgo regulatorio `hecho` |
| PAUSA_WORDS | Emociones | 24 / — | — | — | shuffle seed | — | `SMALL_UNVALIDATED` `hecho` |
| SEPARATOR_ITEMS | Emociones | 40 (10/cat, 12 eval) / IR≥0.80 | ≥0.80/item | **servido** (hoy filtrado, fantasma) | sí | — | holdout fantasma `hecho`→BLOCK |
| Valencia neutra (EM9) | Emociones | — / IR≥0.80 | ≥0.80 | disjunto de SEPARATOR | sí | — | no construido `por_confirmar` |
| Bayes Arena / Reversibility | Decisiones | procedural (`generateScenario(seed)`) | prob-por-diseño | sí | `seed!=0` | formato numérico | `PROCEDURAL` `hecho` (no memorizable, sin validación) |
| Evidence-controlled (forecast) | Decisiones | — / IR≥0.80 o prob-por-diseño | ≥0.80 | sí | sí | — | no construido |
| Objetos prohibidos (EX01) | Creatividad | 8 / ≥40 | objetos prepotentes | banco oculto | dura | — | `SMALL_UNVALIDATED` `hecho` |
| Frecuencia léxica (EX06) | Creatividad | — / cobertura auditada | freq_rel empírica | banco oculto | dura | — | no auditado |
| Pares / UNTRAINED | Memoria | grande/equated | — | partición reservada | estricta | — | equating ausente `hecho`→BLOCK |
| Caras diversas (EX8) | Memoria | — / licenciado | balance etnia/género/edad | sí | sí | — | sesgo étnico `hecho`→MERGE EX4 |

> Tests transversales de banco: `bank_belowMinSize_failsBuild`; `holdout_neverServedInPractice`;
> `correctPosition_withinChiSquare`; `interRater_below080_rejected`; `exposureTracking_present` (`ABSENT` hoy).

---

## 9.5 Claims × scanner

> Cada grupo de claims (ver `AUDIT_V2_TECNICO.md` §11 / `12_COPY_SAFETY_CLAIMS`) tiene 3 tests: lexema
> prohibido ⇒ **rojo**; copy permitido ⇒ **verde**; negación/compuesto del lexema ⇒ **rojo**. El scanner
> cubre `commonMain` + `commonTest` + `desktopTest`.

| Grupo de claims | Lexema prohibido (ejemplo) ⇒ rojo | Copy permitido ⇒ verde | Negación/compuesto ⇒ rojo | Estado cobertura |
|---|---|---|---|---|
| Far-transfer / vida real | `inteligencia`, `en la vida real`, `multitarea`, `conducción`, `regulación emocional`, `te distraes menos con el móvil` | "perfil cognitivo funcional", "progreso contra tu línea base" | `non-fake-news`, `anti-multitarea` | parcial (`ABSENT` global) |
| Salud / clínico | `TDAH`, `diagnóstico`, `terapia`, `depresión`, `ansiedad`, `deterioro cognitivo` | "entrenamiento funcional", "proxy interno, no diagnóstico" | `non-clinical`, `no-diagnóstico` | arena+emotions `hecho`; resto `ABSENT` |
| Apuesta (Decisiones) | `apuesta`, `fichas`, `bote`, `odds`, `cuota`, `bankroll`, `stake`, `bet`, `casino` | "confianza", "estimación", "probabilidad" | `anti-apuesta`, `no-casino` | arena `hecho`; decisiones `ABSENT` |
| Misinformación (Lenguaje) | `fake news`, `noticias falsas`, `desinformación`, `fuentes fiables`, `verificar hechos del mundo real` | "aplica los criterios de ESTE ejercicio" | `anti-fake-news`, `non-desinformación` | `ABSENT` (P0) |
| Pseudociencia / promesa | `reconfigura tu cerebro`, `% más inteligente`, `aumenta tu CI N puntos`, `entrena tu cerebro como un músculo` | "práctica de fluidez", "dificultad adaptativa" | `neuroplasticidad-garantizada` | `ABSENT` |
| Constructo absoluto sin `Valid` | mostrar "velocidad de procesamiento"/"control inhibitorio" como número cuando `ScoreResult != VALID` | mostrarlo solo en `VALID`; si no, "—"/"calibrando" | — | enforced por INFRA-01 en UI + scanner del copy estático |

> Mecanismo de honestidad de transfer: `TransferLabel { TASK_ONLY, PLAUSIBLE, NOT_DEMONSTRATED, PLACEBO_RISK }`
> (emotions, `hecho`) renderizado siempre junto al `FourPartFeedback`. `macroCategory` es de Velocidad
> (clasificación de patrón RT), NO un transfer-claim.

---

## 9.6 Perfil × actualización

> El núcleo del contrato: jugable ≠ actualiza-perfil. `updatesProfile == true` ⟺ `ScoreState == VALID`
> (un único `return` en la máquina lo pone `true`). Señal inválida = **ausencia** de actualización, nunca un
> valor bajo.

| Condición | `ScoreState` resultante | Acción sobre perfil | `ModuleContribution` | Test |
|---|---|---|---|---|
| `ScoreResult ≠ VALID` (cualquiera) | no-`VALID` | **no actualiza** | `normalizedScore=null`, suficiencia/confianza `0.0` | `nonValid_emitsNullContribution` |
| timebase no fiable (RT-crítico) | `DEVICE_UNRELIABLE` | no actualiza (jugable, feedback local) | `null` | `untrustedTimebase_noProfileWrite` |
| jank/frame-drop excesivo | `DEVICE_UNRELIABLE` | no actualiza | `null` | `jankExcessive_isDeviceUnreliable` |
| banco contaminado/pequeño/holdout agotado | `BANK_CONTAMINATED` | no actualiza | `null` | `bankContaminated_noProfileWrite` |
| anti-gaming REAL disparado | `SUSPECTED_GAMING` | no actualiza (copy neutro, sin acusar) | `null` | `gamingFlag_noProfileWrite` |
| invalidación dura (AGRS>0.75 / quality LOW) | `INVALIDATED` | no actualiza | `null` | `agrsOverThreshold_invalidated` |
| `n < nMin` por celda | `INSUFFICIENT_N` | CALIBRATING, no actualiza; `value==null` | `null` | `belowMinN_insufficientN_nullValue` |
| señal primaria no computable / inestable | `CALIBRATING` | no actualiza; UI "—" (nunca 0) | `null` | `calibrating_displaysDash_notZero` |
| abandono (ejercicio emocional/frustración) | `ABANDONED_SAFE` | señal de seguridad, **NO penaliza** (Regla #8) | `null`, sin culpa | `safetyExit_neverPenalized` |
| **mejora de capacidad** (transferencia) | `VALID` solo si sonda UNTRAINED + `|RCI|>1.96 && isGenuine && reliable` | actualiza con claim `WITHIN_TASK_TREND` | `value` normalizado | `genuineImprovement_onlyViaUntrainedProbeAndRci` |
| **todo OK, conf≥0.7 + inter-sesión** | `VALID` robusto | actualiza; `preliminary=false` | `value`, confianza alta | `robustValid_updatesProfile_preliminaryFalse` |

**Reglas invariantes (forzadas en `init` + property tests):**
- **R-VALID-ONLY:** `updatesProfile == true` ⟺ `state == VALID`.
- **R-NO-REDUCE:** ningún estado no-`VALID` escribe en `CognitiveProfileScore`/`DimensionState`; no existe "actualización a la baja".
- **R-NULL≠0:** no-`VALID` ⇒ `primarySignal=null`; UI pinta `—`/"calibrando"/"datos insuficientes", nunca `0`.
- **R-PRELIM:** `preliminary=false` solo en `VALID` robusto; mientras el §15 physical gate (timebase/jank reales) siga abierto, Speed fuerza `preliminary=true` en TODO veredicto.
