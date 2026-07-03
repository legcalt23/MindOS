# 16 — Prompts para agentes de código (Cursor / Claude) — versión extendida

> **Reglas comunes para TODOS los prompts** (no repetir en cada bloque, pero son vinculantes):
> - No tocar fuera del scope del issue; no refactor masivo; no regenerar el baseline de Detekt para silenciar
>   findings (aprendizajes 2026-06-04/18).
> - No inventar rutas: si no la confirmas leyendo el repo, marca `ruta probable` / `POR_CONFIRMAR_EN_REPO`.
> - Ejecuta los tests del scope ANTES de terminar; pega el output.
> - Respeta los invariantes: **`null != 0`**; **RT no toca perfil sin timebase autoritativa** (Speed/Android es
>   el único `AUTHORITATIVE`; Focus/Emotions/Decisiones degradan); **cero claims de far-transfer NI para
>   negarlos**; **anti-gaming solo si implementado+testeado con adversarios**; **banco pequeño/no-validado
>   bloquea perfil**.
> - Trabaja en una rama por issue; NUNCA sobre `main`/`master` (worktree por tarea).
> - Comandos Gradle: en PowerShell citar propiedades con punto (`"-Pkotlin.incremental=false"`); Android del
>   módulo compartido = `:shared:compileAndroidMain` (NO `compileDebugKotlinAndroid`); no paralelizar tareas que
>   compartan `shared/build`.
> - KDoc honesto: marca lo no demostrado como hipótesis; lo no soportado es `null`/`preliminary`/`calibrating`.

Comando de validación base de cada issue (sustituir `<Filter>`):
`./gradlew :shared:desktopTest --tests "<Filter>" :shared:detekt` y, si toca Android, `:shared:compileAndroidMain`.

---

## P0 — Infraestructura que desbloquea todo

### P0-1 · Implementar `ScoreResult` sellado + state machine
```
Implementa ScoreResult sellado y su state machine segun 05_INFRAESTRUCTURA_P0/INFRA_01.
Crea (rutas probables): features/mindboost/scoring/contract/ScoreResult.kt y ScoringStateMachine.kt en
commonMain; test en commonTest/desktopTest.
Estados: VALID/CALIBRATING/INSUFFICIENT_N/INVALIDATED/DEVICE_UNRELIABLE/BANK_CONTAMINATED/SUSPECTED_GAMING/
ABANDONED_SAFE. Agrupa el constructor en sub-bloques (ScoreProvenance/DataQuality/AntiGaming/Sampling) para
evitar LongParameterList. init: require((VALID)==(primarySignal!=null)) y que no-VALID prohiba update.
ScoringStateMachine.evaluate(ScoreInputs): Verdict con gates de seguridad en cascada de cortocircuito:
abandono -> timebase -> jank -> banco -> gaming -> invalidacion -> n -> cobertura-detector -> accuracy-gate ->
estabilidad -> VALID. VALID es el UNICO path que pone updatesProfile=true. NINGUN estado no-VALID serializa a numero.
LIMITES: NO toques FocusScore/CognitiveProfileScore/RciResult/DimensionState (son Raw de adaptadores); NO
cambies ModuleContribution; data/enum/object puro (sin Composable, sin hex) para no romper Detekt.
Tests obligatorios (nombres): ScoringStateMachineTest con:
  rtCritical_sinTimebaseAutoritativa_nuncaActualizaPerfil
  gamingFlagReal_fuerzaSuspectedGaming_noUpdate
  detectorAusente_enRtCritico_caeACalibrating_noValid
  nPorDebajoDeMinN_produceInsufficientN_conPrimarySignalNull
  bancoContaminado_yHoldoutAgotado_bloqueanUpdate
  abandonoEmocional_esAbandonedSafe_sinPenalizar
  validEsElUnicoEstado_conUpdatesProfileTrue (exhaustivo sobre ScoreState)
  ningunEstadoNoValid_emitePrimarySignalNoNull (property)
  preliminarySoloFalse_conConfidence07_yAgregacionInterSesion
Comando: ./gradlew :shared:desktopTest --tests "*ScoringStateMachineTest" :shared:detekt
```

### P0-2 · `TimingPolicy` con signo + jank real (medición que alimenta el gate)
```
Segun INFRA_02 (PARTE A). Extiende speed/domain/TimebasePolicy.kt para que computeRtNanos NO lance con rt<0
(devuelve SignedTiming con signo). Crea speed/domain/timing/TimingPolicy.kt (expect FrameTimingProbe) +
TimingPolicyClassifier.kt (puro). actual Android = Choreographer.postFrameCallback que mide FrameStats reales
(frameDropFraction, maxFrameJitterNanos, criticalFrameJitter del frame del onset). Reemplaza el
frameDropFraction=0.0 / criticalFrameJitter=false HARDCODEADOS de SpeedSessionStateHolder.kt:259-260 por
FrameTimingProbe.stop(). Desktop/iOS = NonAuthoritative (downgrade explicito; iOS = NO_DISPLAY_LINK hasta CADisplayLink).
LIMITES: NO portes a Focus en este issue (issue P0-2b aparte); NO inventes CADisplayLink (deja iOS degradado);
solo el subsistema timing de Speed.
Tests (nombres): TimingPolicyClassifierTest con:
  rtNegativo_noLanza_signoPreservado
  platformSourceNoChoreographer_esNonAuthoritative
  millisDerived_api33Omenor_esNonAuthoritative
  criticalFrameJitterEnOnset_esDeviceUnreliable
  frameDropSobreUmbral_esDeviceUnreliable
  choreographerSinJank_esAuthoritative
Comando: ./gradlew :shared:desktopTest --tests "*TimingPolicyClassifierTest" :shared:detekt
```

### P0-2b · Portar la timebase/probe a Focus (issue separado)
```
OBJETIVO: que el RT de Focus deje de ser monotonico-de-corrutina (MonotonicClock.nowNanos en el hilo, NO el
frame de vsync) y consuma el contrato de TimingPolicy de P0-2, de modo que RT-critico de Focus sin
Choreographer degrade a DEVICE_UNRELIABLE y NO actualice perfil.
Ancla (hecho): focus/presentation/exercise/MonotonicClock.kt (commonMain, sin expect/actual);
FocusFixedBlockRunner.kt:83 (onsetNanos=monotonicClock.nowNanos()), :86 (latencyMs). Speed/Android ya tiene
SpeedTimebase.android.kt (Choreographer+MotionEvent) como expect/actual a replicar (NO copiar a mano UUIDs ni
proyectos binarios).
PASOS: (1) introduce expect rememberFrameTimingProbe()/FocusTimebaseHandle en focus, replicando el patron de
Speed; (2) el FocusFixedBlockRunner toma onset del frame de presentacion (no de la corrutina) cuando hay
handle autoritativo; (3) el adaptador FocusScoreResultAdapter setea deviceTimingQuality:
  - Android con Choreographer + jank medido OK -> AUTHORITATIVE
  - cualquier otro -> MONOTONIC_NONFRAME (hoy) -> RT_CRITICAL nunca pasa timebaseTrusted -> DEVICE_UNRELIABLE.
LIMITES: NO cambies los generadores E1-E13 ni el assembler de outcome; NO toques el scoring de Focus; solo la
captura de onset/response y el wiring del handle. NO marques Focus como AUTHORITATIVE en desktop/iOS.
Tests (nombres): FocusTimebaseTest con:
  focusSinChoreographer_rtCriticoEsDeviceUnreliable
  focusOnsetDesdeFrame_noDesdeCorrutina_cuandoHayHandle
  focusFallbackDesktop_esNonAuthoritative
  focusScoreResultAdapter_rtCritico_noActualizaPerfilSinAuthoritative
Comando: ./gradlew :shared:desktopTest --tests "*FocusTimebaseTest" :shared:compileAndroidMain :shared:detekt
```

### P0-3 · `AntiGamingFramework` + adversarios
```
Segun INFRA_03. Crea core/antigaming/AntiGamingFramework.kt + adversarios extendiendo
speed/simulation/SyntheticUserRunner.kt (EXISTE): RhythmicTapper, WaitingStrategist, RespondToAll,
ButtonSpammer, KeywordStuffer, BankMemorizer, MinEffort, AbsentNoSearch, RevealAllNoRead, SpeedAccuracyHacker.
API uniforme: run(exercise, adversary) -> ScoreResult. Por cada detector documenta: que observa, threshold,
accion sobre ScoreResult (firedFlags / InvalidationReason). Solo entran flags de detectores REALES
(ANTICIPATION, RT_UNDER_400, SSRT_WAITING_STRATEGY, FAST_COMMISSION, QUALITY_GATE_TRIP); los ausentes
(tap-cadence solo Arena, sesgo posicional inexistente, keyword-stuffing BLOCKED) se modelan como
DetectorCoverage ausente que degrada el claim, NO como capacidad fingida.
LIMITES: solo el framework + adversarios; NO cables ejercicios todavia (eso es P1-B por ejercicio).
Tests (nombres): por adversario un test que FALLA si puntua alto:
  rhythmicTapper_noInflaIndice (assertSyntheticScoresLow o Contaminated)
  waitingStrategist_rompeSsrt_trustworthyFalse
  respondToAll_noSubeHitRate_enGoNoGo
  buttonSpammer_entropiaCero_esContaminated
  bankMemorizer_noSuperaHoldout
  speedAccuracyHacker_iesPenalizaTradeOff
Comando: ./gradlew :shared:desktopTest --tests "*AntiGaming*Test" :shared:detekt
```

### P0-4 · `ItemBankValidation`
```
Segun INFRA_04. Crea core/bank/ItemBank.kt + BankValidator.kt. Metadatos por item: difficulty,
discriminability, constructTag, culturalLoad, priorKnowledgeRisk, interRaterAgreement, holdoutGroup,
exposureCount, validationStatus. Build falla si un ejercicio profile-updating usa banco<minSize o sin holdout
activo servido. selectNext() nunca repite holdout en practica; posicion correcta balanceada; inter-rater>=0.80
donde el correcto es juicio humano.
LIMITES: NO migres los bancos existentes (Logic 6 / Language 36 / Creatividad 14) en este issue; solo el
contrato + validador + el gate de build. Marca cada banco existente con su validationStatus real (SMALL_UNVALIDATED).
Tests (nombres): BankValidatorTest con:
  holdoutNuncaServido_enPractica
  balanceoPosicionCorrecta_dentroDeChi2
  bancoMenorQueMinSize_buildFail
  interRaterMenor080_bancoRechazado
  selectNext_noRepiteHoldout
Comando: ./gradlew :shared:desktopTest --tests "*BankValidatorTest" :shared:detekt
```

### P0-5 · `CopySafetyScanner` (build, multi-modulo, anti-negacion)
```
Segun INFRA_05 y 12_COPY_SAFETY_CLAIMS / AUDIT_V2_TECNICO §11. Crea
features/mindboost/core/copy/ForbiddenLexicon.kt + task Gradle en shared/build.gradle.kts ESPEJO de
arenaTerminologyScanner (lineas ~198-272), enganchada a check, cubriendo features/mindboost/** en
commonMain+commonTest+desktopTest. Detecta lexico prohibido como substring de palabra Y en
negacion/compuesto/guion (\b cruza el guion). Lista exacta en AUDIT_V2_TECNICO §11.2: far-transfer
(inteligencia/IQ/Gf/mejores decisiones/multitarea/regulacion emocional/autocontrol/te distraes menos con el
movil), clinico (TDAH/depresion/diagnostico/trastorno), apuesta (apuesta/fichas/casino/odds/bankroll),
misinformacion (fake news/desinformacion/fuentes fiables). Folding de acentos espanol. requireDir=true para
que un typo no desactive el gate.
LIMITES: allowlist de excepciones para substrings legitimos (p.ej. "balance" semantico vs "bankroll") con
test de regresion; NO toques copy de UI en este issue (eso es Q-1 por modulo).
Tests (nombres): cada lexema + su forma negada/compuesta -> rojo; copy permitido (§11.1) -> verde; falso
positivo legitimo controlado. CopySafetyScannerTest:
  lexemaProhibido_comoSubstring_esRojo
  lexemaProhibido_enNegacion_nonClinical_esRojo
  copyPermitido_senalDePractica_esVerde
  substringLegitimo_balanceSemantico_enAllowlist
Comando: ./gradlew :shared:mindboostCopySafetyScanner :shared:detekt
```

---

## P1 — Adaptación de ejercicios a los gates

### P1-A · Adaptar un ejercicio v1 a los gates
```
Adapta <EJERCICIO> segun su ficha 06_EJERCICIOS_V1_DEFENDIBLES/V1_0X y su fila en AUDIT_V2_PRODUCTO §12.
Conecta ScoreResult (INFRA_01), MinSampleGate (INFRA_06), AntiGaming (INFRA_03) y ItemBank (INFRA_04) si aplica.
Implementa los gates EXACTOS de la ficha (p.ej. processingAccuracy>=0.85; suelo goAcc>=0.6; AGRS con entropia
real; min-n=12). RT NO actualiza perfil sin timebase autoritativa. Mejora de capacidad SOLO via sonda UNTRAINED.
LIMITES: NO cambies el runtime de otros ejercicios; NO regeneres baseline Detekt; mantiene el ScoreResult
no-VALID como ausencia de update (no valor bajo).
Tests: unit + el adversario aplicable (P0-3) + integracion null!=0. Filtro: "*<Exercise>GateTest".
Comando: ./gradlew :shared:desktopTest --tests "*<Exercise>GateTest" :shared:mindboostCopySafetyScanner :shared:detekt
```

### P1-B · Escribir tests adversariales de un ejercicio
```
Para <EJERCICIO>, escribe los tests adversariales de 09_MATRIZ_QA_TESTS / 10_MATRIZ_ANTI_GAMING (AUDIT_V2_TECNICO §10):
corre el/los adversario(s) aplicables via AntiGamingFramework y FALLA si el criterio de fallo se cumple
(p.ej. RhythmicTapper infla SUSTAINED_FOCUS_INDEX; "siempre b" supera azar; ButtonSpammer produce VALID).
Anade golden test de scoring y property null!=0.
LIMITES: NO modifiques la logica del ejercicio salvo un bug que el test revele (y entonces documenta el bug).
Comando: ./gradlew :shared:desktopTest --tests "*<Exercise>AdversarialTest"
```

---

## P1/P2 — Issues específicos pedidos (límites + comando de test)

### S-1 · Portar la timebase autoritativa a Focus (resumen accionable)
```
(Es P0-2b; aqui como issue independiente si se prioriza Focus antes que el resto.)
OBJETIVO: Focus deja de medir RT a nivel de corrutina (FocusFixedBlockRunner.kt:83-86) y consume el contrato
de TimingPolicy de Speed; sin Choreographer autoritativo, RT-critico de Focus -> DEVICE_UNRELIABLE (no perfil).
LIMITES: replica el patron expect/actual de SpeedTimebase.android.kt; NO inventes CADisplayLink iOS; NO toques
generadores ni scoring; solo onset/response + wiring del handle + adaptador deviceTimingQuality.
INVARIANTE: ningun RT de Focus actualiza perfil mientras deviceTimingQuality != AUTHORITATIVE.
Tests: FocusTimebaseTest (focusSinChoreographer_rtCriticoEsDeviceUnreliable;
focusOnsetDesdeFrame_noDesdeCorrutina; focusFallbackDesktop_esNonAuthoritative).
Comando: ./gradlew :shared:desktopTest --tests "*FocusTimebaseTest" :shared:compileAndroidMain :shared:detekt
```

### S-2 · Fusionar los clones de Emociones (EM2≡EM11≡EM15, EM1≡EM8, EM3≡EM10, EM4≡EM14)
```
OBJETIVO (ADR-07, dissent #5): colapsar los 7 clones de Emociones a EM1/EM2/EM3 (+EM4 absorbe EM14), de modo
que el catalogo NO exponga un clon como ejercicio profile-updating separado y el brain-profile NO cuente la
varianza dos veces.
Ancla (hecho): EmotionExerciseCatalog lista 15 ids con 6 generadores reales (EM1Pausa, EM2Recuperacion,
EM3Separador, EM8AntiImpulso, EM10SepPresion, EM11Continuidad). Mapa de merges (AUDIT_V2_PRODUCTO §"Mapa de
redundancias"): EM8->EM1 (parametro no-go ratio); EM12->EM1 (anticipaciones, secundaria); EM10->EM3;
EM11->EM2; EM15->EM2; EM14->EM4; EM7->EM13.
PASOS: (1) cada clon se convierte en un NIVEL/parametro de dificultad del canonico (no un id de catalogo
profile-updating); (2) actualiza el catalogo y su test de consistencia (espejo de
ExerciseCatalogConsistencyTest de focus) para que el clon NO aparezca como exposable con canUpdateProfile; (3)
conserva el renderer compartido (los EM reusan 3 renderers).
LIMITES: NO borres el codigo del generador del clon si se reutiliza como nivel; NO cambies el banco; NO toques
EM3 banco (eso es EX-EM3 aparte); solo el catalogo/exposicion + el wiring de niveles. Respeta el
emotionsTerminologyScanner existente (no introducir lexico clinico).
INVARIANTE: catalogo nunca expone EM7/8/10/11/12/14/15 como ejercicio profile-updating independiente.
Tests (nombres): EmotionCatalogMergeTest con:
  clonNoExpuesto_comoEjercicioProfileUpdating (EM8/EM11/EM15/EM14/EM10/EM12/EM7)
  em2_absorbeEM11_yEM15_comoNiveles
  em1_absorbeEM8_yEM12
  catalogoConsistente_registryIgualImplemented
Comando: ./gradlew :shared:desktopTest --tests "*EmotionCatalogMergeTest" :shared:mindboostCopySafetyScanner :shared:detekt
```

### S-3 · Cablear `EX_TRANSFER_PROBE` (sonda UNTRAINED + RCI) en Memoria
```
OBJETIVO (ADR-06, P0): conectar la sonda UNTRAINED y el RCI que HOY existen sin caller, de modo que la mejora
de capacidad solo se afirme con isGenuine && reliable.
Ancla (hecho): RealImprovementCriterion.kt (untrainedDelta>0.05 && effectSize>=0.30 && sustainedWeeks>=2) y
ReliableChangeIndex.kt (RciResult.{NotEnoughData|Value(reliable=|RCI|>1.96)}) existen pero SIN caller;
TransferProbePartition reservado pero nunca servido (Phase-8 ausente); formas paralelas A/B/C NO equiparadas.
PASOS: (1) crea/cablea el scheduler que sirve la particion UNTRAINED reservada (no la de practica); (2)
equating de formas paralelas A/B/C ANTES de comparar; (3) gate de fatiga: no administrar la sonda con readiness
HIGH; (4) RCI gated por reliability de >=2-3 administraciones, NotEnoughData por defecto; (5) cero feedback en la
sonda; (6) claim "Mejora confirmada" SOLO si isGenuine && reliable.
LIMITES: NO cambies el scoring de los ejercicios de Memoria; NO sirvas la sonda como practica; NO muestres
feedback en la sonda; solo el scheduler + equating + wiring del RCI + el gate de claim en UI-model.
INVARIANTE: RCI NotEnoughData con <2 administraciones; ninguna mejora se afirma sin |RCI|>1.96 sobre UNTRAINED.
Tests (nombres): TransferProbeWiringTest con:
  rci_notEnoughData_conMenosDe2Administraciones
  sondaUntrained_servidaDesdeParticionReservada_noPractica
  equating_verificadoEnDatosSinteticos
  mejoraDeclarada_soloSi_RciMayorQue196_yUntrained
  sonda_noMuestraFeedback
  fatigaHigh_noAdministraSonda
Comando: ./gradlew :shared:desktopTest --tests "*TransferProbeWiringTest" :shared:detekt
```

---

## Q — Auditoría de copy por módulo

### Q-1 · Auditar copy de un módulo
```
Lee 12_COPY_SAFETY_CLAIMS / AUDIT_V2_TECNICO §11. Revisa TODOS los strings de UI de <MODULO> (AppStrings +
pantallas). Reemplaza cualquier claim de far-transfer/clinico/IQ/apuesta/fake-news por copy permitido (frases
EXACTAS de §11.1). Asegura que ningun constructo absoluto se muestre sin ScoreResult.VALID.
INVARIANTE: cero claims de far-transfer NI para negarlos (no anadir "no es un test de IQ" si el scanner marca
"IQ"; usar "perfil cognitivo funcional"/"senal de practica").
LIMITES: NO toques logica; solo strings. Reporta cada reemplazo (antes/despues).
Comando: ./gradlew :shared:mindboostCopySafetyScanner (debe quedar verde)
```
