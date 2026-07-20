<!-- Focus V5 · S2 tarea (4) · Dossier de TRACER B. Formato: seccion 5 de
     docs/context_pack/04_EXERCISE_QUALITY_STANDARD.md + los dos encabezados adversariales del formato de
     docs/mindboost_exercise_design/ (FIX-FOCUS-S2-DOSSIER-FORMAT, 09_DECISION_LOG.md 20.4.1).
     Los campos PENDING_S3 son la UNICA forma no vacia sancionada para lo que S3 crea. -->

# Dossier FOC2-E01 «Centinela» — Focus Engine V5 (Tracer B)

```yaml
identity:
  exerciseId: FOC2-E01
  codename: "Centinela"
  engine: focus_v5
  tracer: B
  displayName: "Centinela"
  shortDescription: "Vigilancia sostenida: un solo toque cuando aparece la señal, silencio el resto del tiempo."
  module: mindboost.focus
  subskillLabel: SUSTAIN            # ADR-FOCUS-CONSTRUCT (a): etiqueta de navegación, NO subscore
  paradigm: VIGILANCE_GO_ONLY
  responseModality: SINGLE_TAP      # go-only, sin alternativas
  status: DESIGNED
  version: 0.1.0-candidate
  scoringVersion: SCORE-FOC2-E01-0.1.0-candidate
  scoringVersionProvenance: >
    Etiqueta de procedencia, no re-verificación. Corresponde al scorer candidato ejecutado en la
    tarea (5) del plan Focus V5 (simulación de perfiles adversariales sintéticos: HONEST, EXPERT,
    MASHER, WAITER). Este dossier CONSUME ese output; no lo recalcula. El binding definitivo
    scoringVersion→implementación se sella en S3.
  sourceOfTruth:
    - "docs/engines/focus_v5/EXERCISE_PORTFOLIO_V5.md:24 (tabla maestra)"
    - "docs/engines/focus_v5/EXERCISE_PORTFOLIO_V5.md:47-53 (dossier-lite)"
    - "docs/engines/focus_v5/EXERCISE_PORTFOLIO_V5.md:158-164 (razonamiento del par de tracers)"
    - "docs/context_pack/09_DECISION_LOG.md §20.4 (ADR-FOCUS-CONSTRUCT, ADR-FOCUS-SCORING, reglas 1-9)"
    - "docs/context_pack/04_EXERCISE_QUALITY_STANDARD.md §5 (formato de dossier)"
  owner: cognitive-engine-dev
  reviewers: [efficacy-guardian, claims-auditor, qa-safety]

user_value:
  promise: >
    Practicar el mantenimiento de la atención durante minutos, cuando casi nada ocurre y la señal
    es escasa. El usuario ve, sesión a sesión, cuántas señales detectó y cuántas veces respondió
    sin señal — su propio historial, no una comparación con otras personas.
  userFacingCopy:
    title: "Centinela"
    subtitle: "Toca solo cuando aparezca la señal."
    postSessionHeadline: "Detectaste {hits} de {signals} señales."
    tone: "sobrio, factual, sin celebración desproporcionada"
  whyItMatters: >
    Vigilar durante un intervalo largo con señales escasas es una demanda distinta a reaccionar
    rápido en ráfagas: el reto no es la velocidad, es no soltar la tarea cuando no pasa nada.
  progressNarrative: "progreso frente a tu propia línea base"
  notPromised:
    - "No promete mejorar la atención en el trabajo, el estudio ni ninguna actividad fuera de la app."
    - "No entrega una puntuación de capacidad, ni una nota, ni un percentil."
    - "No compara al usuario con otras personas."
  sessionLengthMinutes: [3, 5]
  expectedFrequency: "1 sesión por día como máximo dentro de un plan; no hay racha obligatoria."

construct:
  targetDemand: "Mantenimiento de la respuesta ante señales escasas a lo largo de un intervalo prolongado (vigilancia)."
  operationalDefinition: >
    Detección de un objetivo infrecuente (20% de los ensayos) inmerso en una corriente de
    no-objetivos (80%), con ISI largos y respuesta go-only de un solo toque. La demanda se opera
    como capacidad de discriminación señal/ruido (d') y sesgo de respuesta (criterio c) POR
    CONDICIÓN, no como velocidad.
  subskillLabelRationale: >
    "SUSTAIN" es una ETIQUETA DE NAVEGACIÓN del portafolio (ADR-FOCUS-CONSTRUCT (a)). En C0 NO
    se emite ningún subscore por subhabilidad, ni se agrega SUSTAIN con otros ejercicios.
  stimulusClass: NON_LEXICAL        # Regla 5
  stimulusRationale: >
    Formas geométricas abstractas sin contenido verbal, para que el desempeño no dependa de
    vocabulario, lectura ni idioma.
  theoreticalGrounding:
    - "Paradigma de vigilancia con eventos infrecuentes y detección de un solo canal."
    - "Teoría de detección de señales (d', criterio) como marco de separación entre discriminación y sesgo."
    - "Corrección loglineal de Hautus (1995) para celdas con conteos extremos."
  excludedInterpretations:
    - "NO mide un factor general de Focus. El par de tracers no establece ningún factor común."
    - "NO establece los niveles M0-M5 ni ninguna escala de dominio."
    - "NO demuestra transferencia a ninguna tarea fuera de este ejercicio."
    - "NO valida el tiempo de respuesta como índice de nada: el RT es secundario y descriptivo."
    - "NO mide RESTRAIN ni ninguna otra subhabilidad del portafolio distinta de la etiqueta SUSTAIN."
    - "Con alpha ~20 no se sostiene ninguna afirmación psicométrica: ni fiabilidad, ni validez de constructo, ni estructura factorial."
    - "NO es un instrumento de evaluación de ningún tipo fuera del entrenamiento; su salida es orientativa."
  provenanceOfExclusions: "agent_outputs/02_efficacy_guardian.md:104 (no-afirmaciones del dictamen)"

gameplay:
  loop: >
    Se presenta una secuencia de formas, una cada vez, separadas por ISI largos. La mayoría son
    no-objetivos: el usuario no hace nada. Cuando aparece el objetivo, el usuario toca UNA vez,
    en cualquier punto de la pantalla, dentro de la ventana de respuesta.
  inputSurface: FULL_SCREEN_SINGLE_TAP
  sideless: true
  sidelessRationale: "No hay izquierda/derecha ni alternativas: un único gesto posible."
  trialTimeline:
    fixationMs: 400
    stimulusOnScreenMs: 250
    responseWindowMs: 1200          # desde onset del estímulo
    isiMs: { min: 2200, max: 4600, distribution: UNIFORM }
  isiRationale: >
    ISI largos y variables sostienen la demanda de vigilancia (no se puede anticipar el ritmo)
    y evitan que el usuario entre en un patrón motor automático.
  targetRate: 0.20
  blockStructure:
    trialsPerBlock: 40
    blocksPerSession: 3
    breakBetweenBlocksSeconds: 20
  totalTrialsPerSession: 120
  abortConditions:
    - "El usuario abandona la sesión: el bloque en curso se descarta, los bloques completos se conservan."
    - "Fondo/interrupción del sistema > 5 s: el bloque en curso se marca INVALIDATED_INTERRUPTED."
  practiceBlock:
    enabled: true
    trials: 12
    targetRate: 0.33
    excludedFromMeasurement: true

content:
  stimulusSet: "Formas geométricas planas monocromas (círculo, cuadrado, triángulo, hexágono, rombo, cruz)."
  targetDefinition: "Una forma designada al inicio del bloque (p. ej. el rombo) actúa como objetivo; el resto son no-objetivos."
  targetRotatesPerBlock: true
  lexicality: NON_LEXICAL
  bankStrategy:                     # Regla 4
    parallelForms: true
    parallelFormCount: 4
    trainingBank: "FOC2-E01-BANK-TRAIN-v0.1"
    measurementBank: "FOC2-E01-BANK-MEAS-v0.1"
    banksAreDisjoint: true
    heldOutProbes:
      designedBeforeTraining: true
      count: 24
      bankId: "FOC2-E01-PROBES-HELDOUT-v0.1"
      neverServedInTraining: true
  generatorReuse:
    source: "generador de secuencias de vigilancia v1 (Focus v1)"
    scope: GENERATOR_ONLY
    note: >
      El reuso autorizado alcanza SOLO al generador de secuencias. El eje de medida se re-deriva
      desde cero; ningún índice v1 se hereda (ver validation.knownLimitations, SUSTAINED_FOCUS_INDEX).
  bankIntegrity:
    integrityLevel: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
    integrityAuthority: BankIntegrityAuthority   # nunca un literal BankIntegrity.* en el call-site
    registeredInProfileUpdatingBanks: false
    failClosedDefault: SMALL_UNVALIDATED
  exposureControl:
    seedPersisted: true
    seedType: SessionSeed
    rngVersion: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)

difficulty:
  adaptive: true
  adaptedParameters:
    - parameter: isiMs
      direction: "ISI más largos = más demanda de vigilancia"
      range: [1800, 6000]
    - parameter: stimulusOnScreenMs
      direction: "menos tiempo en pantalla = más demanda perceptiva"
      range: [140, 320]
    - parameter: distractorSimilarity
      direction: "no-objetivos más parecidos al objetivo = discriminación más fina"
      levels: [LOW, MEDIUM, HIGH]
  fixedParameters:
    - parameter: targetRate
      value: 0.20
      rationale: >
        La tasa de objetivos se mantiene FIJA porque define la relación señal/ruido sobre la que
        se calcula d'. Moverla haría incomparables las sesiones entre sí.
  adaptationRule:
    algorithm: STAIRCASE_1UP_2DOWN_ON_ACCURACY
    governedBy: ACCURACY_PER_CONDITION      # Regla 1: nunca por RT
    stepSizeInitial: 2
    stepSizeAfterReversals: 1
    reversalsToConverge: 6
    rtNeverDrivesDifficulty: true
  difficultyStateInitial: "derivado de la última sesión completada del propio usuario; sin historial, nivel medio"
  difficultyIsNotAScore: >
    El nivel de dificultad alcanzado NO se muestra como puntuación, nota ni percentil
    (ADR-FOCUS-SCORING: BLOCKED_BY_DESIGN).

measurement:
  modeEligibility: [TRAINING_ONLY]
  contributesToProfile: false
  profileContributionRationale: >
    Estado DESIGNED, banco no validado y sin evidencia psicométrica: la contribución al perfil
    cognitivo funcional queda cerrada por diseño. Cualquier señal es orientativa y local a la sesión.

  primaryMetrics:                   # Regla 1: exactitud / d' / criterio POR CONDICIÓN
    - id: HIT_RATE_VIGILANCE
      definition: "hits / ensayos-señal, dentro de la condición VIGILANCE"
      perCondition: true
    - id: FALSE_ALARM_RATE_VIGILANCE
      definition: "falsas alarmas / ensayos-ruido, dentro de la condición VIGILANCE"
      perCondition: true
    - id: DPRIME_VIGILANCE
      definition: "z(hitRate) - z(falseAlarmRate), con corrección loglineal de Hautus (1995)"
      perCondition: true
      standardErrorComputed: true
      verdictBasedOn: "límite inferior del intervalo del 95% (debe superar cero)"
    - id: CRITERION_VIGILANCE
      definition: "-0.5 * (z(hitRate) + z(falseAlarmRate))"
      perCondition: true
    - id: CHANCE_CORRECTED_ACCURACY
      value: NOT_APPLICABLE
      typedAbsence: true
      rationale: >
        Esta tarea no tiene un nivel de azar bien definido. La probabilidad de un "no" correcto
        depende de cuán dispuesto esté el usuario a responder — que es exactamente lo que mide el
        criterio. Se devuelve una ausencia tipada NOT_APPLICABLE en lugar de fabricar un
        denominador. d' y criterio cargan el peso de la medida.

  secondaryMetrics:                 # Regla 2: RT SIEMPRE secundario, jamás primario
    - id: MEDIAN_HIT_RT_MS
      role: DESCRIPTIVE_ONLY
      neverGoverns: true
    - id: HIT_RT_IQR_MS
      role: DESCRIPTIVE_ONLY
      neverGoverns: true
    - id: TIME_ON_TASK_DELTA_HIT_RATE
      definition: "hitRate del bloque 3 menos hitRate del bloque 1"
      role: DESCRIPTIVE_ONLY
      neverGoverns: true
      note: "Descriptivo del decremento a lo largo de la sesión; NO se interpreta como rasgo."

  retiredMetricsNotUsed:            # Regla 1: seis métricas retiradas, no pueden gobernar
    - CONGRUENCY_EFFECT
    - SET_SIZE_SLOPE
    - SWITCH_COST
    - DUAL_TASK_COST
    - PRESSURE_DELTA
    - SSRT

  aggregationPolicy:
    signalNoiseConvention:
      signalTrial: "ensayo CON objetivo — 20% de los ensayos del bloque"
      noiseTrial:  "ensayo SIN objetivo (no-objetivo) — 80% de los ensayos del bloque"
      hit: "toque en un ensayo-señal dentro de la ventana de respuesta (1200 ms desde onset)"
      falseAlarm: "toque en un ensayo-ruido dentro de su ventana"
      miss: "silencio ante un ensayo-señal (ninguna respuesta dentro de la ventana)"
      correctRejection: "silencio ante un ensayo-ruido (ninguna respuesta dentro de la ventana)"
      identity: "hits + misses = ensayos-señal;  falsasAlarmas + rechazosCorrectos = ensayos-ruido"
      note: >
        Esta convención es la definición operativa del d' reportado por la simulación
        (d'=3.352 en el perfil HONEST). Sin ella, ese número no tiene significado registrado.
    correction:
      method: "loglineal de Hautus (1995)"
      applied: ALWAYS
      appliedRationale: >
        Se aplica incondicionalmente (no solo con celdas en 0 o 1) para que d' sea SIEMPRE finito
        y comparable entre sesiones. Coste conocido: con celdas desbalanceadas (24 señal vs 96
        ruido) la corrección sesga — la celda grande se corrige proporcionalmente menos.
    perConditionOnly: true
    crossConditionCollapse: FORBIDDEN
    crossExerciseAggregation: FORBIDDEN
    sessionLevelScore: null
    sessionLevelScoreRationale: "ADR-FOCUS-SCORING BLOCKED_BY_DESIGN: no se emite ningún número de capacidad."

  omissionRules:
    goOnlyVigilanceSemantics: >
      En esta paradigma NO responder es la NORMA (80% de los ensayos). Por eso las dos formas de
      silencio significan cosas opuestas y se contabilizan por separado:
    rules:
      - condition: "silencio ante un ensayo-SEÑAL"
        classification: OMISSION
        treatment: SIGNAL
        note: >
          La omisión ES la señal que mide el ejercicio, no ruido de medida. NO se descarta, NO se
          imputa, NO se trata como dato faltante. Entra directamente en el denominador del hitRate.
      - condition: "silencio ante un ensayo-RUIDO (no-objetivo)"
        classification: CORRECT_REJECTION
        treatment: CORRECT_RESPONSE
        note: >
          NO es una omisión. Es la respuesta correcta esperada en el 80% de los ensayos.
      - condition: "toque fuera de cualquier ventana de respuesta (respuesta huérfana)"
        classification: ORPHAN_TAP
        treatment: "se cuenta en integrityCounters; no altera hits/FA/miss/CR"
      - condition: "toque múltiple dentro de la misma ventana"
        classification: MULTI_TAP
        treatment: "solo el primer toque cuenta; los extra se registran en integrityCounters"
    noSentinelValues: "Un ensayo sin RT capturable se marca con RT null, nunca con 0, -1 ni ''."

  calibrationRequirements:
    trialsPerSession: 120
    signalTrials: 24                # 120 * 0.20
    noiseTrials: 96                 # 120 * 0.80
    dosageRationale: >
      La dosis se fija en 120 ensayos porque MinSampleGate exige nMinPerCell=20 para DPRIME, y el
      cálculo de d' se hace POR CONDICIÓN, es decir por CELDA (señal y ruido por separado). Con
      120 ensayos al 20%, la celda-señal reúne 24 ensayos, que SUPERA el piso de 20. El n=55
      originalmente ratificado quedaba POR DEBAJO del piso al repartirse por condición (55 * 0.20
      = 11 ensayos-señal < 20): habría producido un d' sin base de muestra suficiente.
    minSampleGateReference: "MinSampleGate, familia DPRIME, nMinPerCell=20"
    minValidBlocksForSessionReport: 2
    belowMinimumBehavior: "CALIBRATING — no se emite ninguna métrica; nunca un valor bajo fabricado."

  timebase:
    criticality: RT_SECONDARY
    requiredForPrimaryMetrics: false
    rationale: >
      Las métricas primarias son de exactitud, no de latencia: no exigen timebase autoritativo.
      El RT secundario se marca como no autoritativo cuando el timebase cae a fallback.
    authoritativeTimebase: "CHOREOGRAPHER_FRAME + NANOS_NATIVE"
    fallbackBehavior: "sesión jugable; MEDIAN_HIT_RT_MS marcado UNVERIFIED_TIMEBASE"

  dataSufficiencyVsConfidence:
    dataSufficiency: "derivada del tamaño de muestra por celda (24/20 en la celda-señal)"
    confidence: "derivada de la estabilidad entre bloques"
    orthogonal: true
    neverDerivedFromEachOther: true

feedback:
  inTrial:
    enabled: false
    rationale: >
      Retroalimentar cada ensayo destruiría la demanda de vigilancia (convertiría la espera en un
      bucle de refuerzo inmediato) y contaminaría el criterio de respuesta.
  endOfBlock:
    enabled: true
    content:
      - "Señales detectadas: {hits} de {signalTrials}"
      - "Toques sin señal: {falseAlarms}"
    prohibited: ["puntuación", "nota", "percentil", "comparación con otras personas", "estimación de capacidad"]
  endOfSession:
    enabled: true
    content:
      - "Resumen de los bloques válidos de la sesión"
      - "Comparación contra la propia línea base del usuario, si existe historial suficiente"
    calibratingCopy: "—"
    calibratingRule: "Sin muestra suficiente se muestra '—', nunca un 0 ni un valor bajo fabricado."
  progressView:
    baselineComparison: "progreso frente a tu propia línea base"
    populationComparison: FORBIDDEN
  toneGuidelines:
    - "Factual y sobrio."
    - "Sin promesas de mejora."
    - "Sin lenguaje de evaluación de ningún tipo fuera de la práctica."

accessibility:
  motor:
    inputTarget: FULL_SCREEN
    minTargetSizeDp: "toda la superficie de juego"
    singleGesture: true
    noPrecisionRequired: true
    noSustainedHold: true
    note: "Un solo toque en cualquier punto: la exigencia motora es la mínima posible."
  visual:
    contrastRatioMin: 4.5
    colorIsNeverTheOnlyCue: true
    shapeDistinguishableWithoutColor: true
    minStimulusSizeDp: 96
    reducedMotionSupported: true
    reducedMotionBehavior: "transiciones instantáneas; la temporización de ensayos NO cambia"
  auditory:
    audioRequired: false
    soundOptional: true
  cognitive:
    instructionsPlainLanguage: true
    instructionsRereadable: true
    practiceBlockAlwaysAvailable: true
    noTimePressureOnInstructions: true
  screenReader:
    sessionChromeLabelled: true
    stimulusStreamExcludedFromAnnouncement: true
    stimulusExclusionRationale: >
      Anunciar cada estímulo alteraría la temporización y la propia tarea. La superficie de juego
      se marca con clearAndSetSemantics y una descripción del estado de la sesión, no del estímulo.
  interruptions:
    pauseAllowed: true
    pauseInvalidatesCurrentBlock: true
    pausePenalty: NONE
  safetyExit:
    alwaysAvailable: true
    neverPenalized: true
    neverCountedAsGaming: true

privacy_security:
  dataPersisted:
    - "Resumen de bloque: conteos (hits, falsasAlarmas, misses, rechazosCorrectos), d', criterio, error estándar"
    - "Parámetros de dificultad alcanzados"
    - "SessionSeed + RngVersion"
    - "Marcas de integridad (integrityCounters)"
  dataNotPersisted:
    - "Ensayos crudos individuales"
    - "Trazas de toque con coordenadas"
    - "Cualquier identificador estable en exportaciones de depuración"
  storage:
    mechanism: SecureKeyValueStore
    android: "AES-256-GCM + Android Keystore"
    desktop: "in-memory"
    ios: "Keychain durable"
    serializationSchemaVersioned: true
    namespace: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
    accountBoundRegistration: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
    erasureCoverage: >
      Al registrar el namespace en AccountBound, DEBEN actualizarse en el mismo cambio el eraser de
      AppGraph (+ su test de cobertura) y functions/src/accountDeletion.ts. Borrado fail-closed.
  telemetry:
    remoteSink: false
    rawThrowablesToRemoteSink: FORBIDDEN
    piiInLogs: NONE
  integrityThreats:
    - id: DEGENERATE_RESPONDING
      description: "Responder a todos los ensayos (machaque) o a ninguno."
      detected: true
      detector: DegenerateRespondingRule
      detectorLogic: >
        Derivada SOLO de conteos: si en toda la sesión existe una única respuesta distinta (todo
        toque, o todo silencio), el bloque se marca Invalidated. NO se usa un umbral sobre |d'|.
      whyNotDprimeThreshold: >
        La corrección loglineal con celdas desbalanceadas (24 vs 96) puede producir d' NEGATIVO
        para un machacador (la simulación da -0.511, no 0), porque la celda grande se corrige
        proporcionalmente menos. Un umbral sobre |d'| clasificaría mal; la regla de conteos no.
      action: INVALIDATE_BLOCK
      neverAccuses: true
    - id: NO_DISCRIMINATION
      description: "Respuesta parcial sin relación con la señal (esperar y tocar al azar)."
      detected: true
      detector: "límite inferior del intervalo del 95% de d' no supera cero"
      action: INVALIDATE_BLOCK
    - id: SIDE_BIAS
      applicable: false
      status: NO_APLICABLE_POR_DISENO
      rationale: >
        La respuesta es de un solo toque en toda la pantalla: no existen lados ni alternativas que
        sesgar. No se omite del registro — se documenta explícitamente como no aplicable para que
        la ausencia de detector sea una decisión de diseño registrada, no un hueco.
    - id: RHYTHMIC_TAPPING
      description: "Toques a intervalo constante que ignoran el estímulo."
      detected: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
      note: "En C0 queda cubierto por DEGENERATE_RESPONDING y NO_DISCRIMINATION en los casos probados."
  detectorCoverage:
    declaredBy: DETECTOR_ITSELF
    level: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
    failClosedDirection: "retener la actualización del perfil; nunca acusar ni penalizar"
  failClosedPolicy: >
    Cualquier duda sobre integridad, timebase o suficiencia de muestra retiene la señal
    (CALIBRATING / Invalidated). Nunca escribe un valor bajo, nunca culpa al usuario.

technical:
  status: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  generatorClass: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  generatorSignature: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  scorerClass: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  scorerSignature: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  stateHolderClass: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  rendererComposable: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  uiStateVariant: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  registryEntry: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  sourceSet: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  packagePath: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  storageNamespace: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  recordDtoClass: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  schemaVersion: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  rngVersion: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  seedPersistenceFields: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  timebaseHandle: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  clockInjection: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  profileAdapterClass: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  profileWiring: NOT_WIRED_BY_DESIGN
  detektSuppressionsAnticipated: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  buildScannersInScope: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  gateCommands: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  technicalBlockRationale: >
    Este dossier es un artefacto de gate de DISEÑO (status DESIGNED). Ningún símbolo de
    implementación existe todavía en el árbol; declarar nombres aquí crearía un contrato falso.
    El bloque entero se sella en S3, bajo el gate T-S3, con owner cognitive-engine-dev. La
    etiqueta PENDING_S3 se usa en la forma sancionada por el CEO en
    docs/context_pack/09_DECISION_LOG.md §20.4.1, entrada FIX-FOCUS-S2-DOSSIER-FORMAT
    (consequences) — no es relleno genérico, es un compromiso con owner y gate nombrados.

validation:
  evidenceLevel: SYNTHETIC_ADVERSARIAL_ORDERING_ONLY
  evidenceScopeStatement: >
    La simulación demuestra ORDENAMIENTO DE PERFILES ADVERSARIALES SINTÉTICOS bajo un scorer
    candidato. NO demuestra validez de constructo, ni fiabilidad, ni transferencia, ni nada
    psicométrico con humanos.

  goldenCases:
    simulationRun: "tarea (5) del plan Focus V5 — simulación de perfiles adversariales sintéticos, scorer SCORE-FOC2-E01-0.1.0-candidate"
    scorerDescription: >
      exactitud + d' + criterio POR CONDICIÓN, corrección de azar, corrección loglineal de Hautus
      (1995) aplicada SIEMPRE (incondicional, para que d' sea siempre finito), error estándar de d'
      calculado, veredicto basado en el INTERVALO del 95% (límite inferior debe superar cero), no
      en el punto.
    cases:
      - profile: E01_HONEST
        condition: VIGILANCE
        n: { signal: 24, noise: 96 }
        hits: 23
        falseAlarms: 3
        dprime: 3.352
        dprimeSE: 0.472
        criterion: 0.121
        verdict: Valid
      - profile: E01_EXPERT
        condition: VIGILANCE
        n: { signal: 24, noise: 96 }
        hits: 23
        falseAlarms: 0
        dprime: 4.12
        dprimeSE: 0.638
        criterion: 0.505
        verdict: Valid
      - profile: E01_MASHER
        n: { signal: 24, noise: 96 }
        hits: 24
        falseAlarms: 96
        dprime: -0.511
        dprimeSE: 0.768
        criterion: -2.309
        verdict: Invalidated(DEGENERATE_RESPONDING)
        note: >
          d' NEGATIVO, no cero, porque la corrección loglineal sesga con celdas desbalanceadas
          (24 vs 96): la celda grande se corrige menos. Por eso el veredicto sale de una regla de
          RESPUESTA DEGENERADA derivada solo de conteos, no de un umbral sobre |d'|.
      - profile: E01_WAITER
        n: { signal: 24, noise: 96 }
        hits: 9
        falseAlarms: 44
        dprime: -0.201
        dprimeSE: 0.29
        criterion: 0.204
        verdict: Invalidated(NO_DISCRIMINATION)
      - profile: E01_SIDE_BIAS
        verdict: NO_APLICABLE_POR_DISENO
        note: "Toque único: no hay lado que sesgar."
    orderingDemonstrated: "HONEST y EXPERT separan del ruido; MASHER y WAITER se invalidan por vías distintas."

  redTeamSuite:
    status: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
    requirement: >
      Cada detector solo cuenta si (a) tiene call-site vivo en el gate que dice proteger,
      (b) un adversario sintético lo dispara y (c) el perfil HONEST no lo dispara.
  unitTests:
    status: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  integrationTests:
    status: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  goldenTestPinning:
    status: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
    requirement: >
      Un golden que solo afirme rango o auto-consistencia NO pinea nada. Debe pinear literales,
      derivados de una referencia independiente, y probarse mutando lo que dice proteger.
  humanPilot:
    status: NOT_STARTED
    blockedBy: "estado DESIGNED; no hay implementación"
  reliabilityEvidence: NONE
  constructValidityEvidence: NONE
  transferEvidence: NONE

  knownLimitations:
    - id: LIM-01
      title: "SUSTAINED_FOCUS_INDEX RETIRADO — el caso canónico de la ausencia que fabrica un MÁXIMO"
      severity: BLOCKING_FOR_REUSE
      reference: "regla 9, EXERCISE_PORTFOLIO_V5.md:178-180; CLAUDE.md, aprendizaje 2026-07-15"
      description: >
        El índice SUSTAINED_FOCUS_INDEX de v1 está RETIRADO y NO puede reaparecer en ninguna forma.
        Calculaba una penalización de la forma (1 - cv) sobre una lista vacía: sin ninguna respuesta
        registrada, la dispersión medida era 0, luego (1 - 0) = 1, luego el índice daba 100/100. Es
        decir: entregaba la puntuación MÁXIMA a un usuario que no respondió NADA. La vigilancia de
        "null != 0" apunta al lado del mínimo fabricado; este es la inversión, y es peor, porque un
        producto de entrenamiento está estructuralmente tentado a mostrar el halago.
        Consecuencia para FOC2-E01: el reuso autorizado del generador v1 es SOLO del generador. El
        eje de medida se re-deriva desde cero sobre d'/criterio con conteos explícitos, y toda
        fórmula alimentada por un estadístico que devuelve 0.0 sobre lista vacía debe devolver
        ausencia tipada en su lugar. El recuento de ensayos que hace computable cada métrica es
        parámetro OBLIGATORIO, nunca derivado.
    - id: LIM-02
      title: "Sin nivel de azar bien definido"
      description: >
        La probabilidad de un rechazo correcto depende de la disposición del usuario a responder,
        que es lo que mide el criterio. La exactitud corregida por azar devuelve NOT_APPLICABLE en
        vez de fabricar un denominador. No se inventa ningún nivel de azar.
    - id: LIM-03
      title: "Sesgo de la corrección loglineal con celdas desbalanceadas"
      description: >
        Con 24 señal vs 96 ruido, la corrección de Hautus no es simétrica: la celda grande se
        corrige proporcionalmente menos, y un patrón degenerado puede salir con d' negativo. Por eso
        el veredicto de degeneración se deriva de conteos, no de un umbral sobre |d'|.
    - id: LIM-04
      title: "Alpha ~20: nada psicométrico se sostiene"
      description: >
        Con el número de ensayos disponible no puede afirmarse fiabilidad, validez de constructo ni
        estructura factorial. La salida es orientativa y local a la sesión.
    - id: LIM-05
      title: "Un solo ejercicio no establece una subhabilidad"
      description: >
        La etiqueta SUSTAIN es navegación. FOC2-E01 no produce un subscore SUSTAIN ni contribuye a
        ningún agregado (ADR-FOCUS-CONSTRUCT (a)).
    - id: LIM-06
      title: "Decremento por tiempo en tarea no interpretado"
      description: >
        TIME_ON_TASK_DELTA_HIT_RATE se registra como descriptivo. En una sesión de 3-5 minutos con
        3 bloques, la varianza esperada es alta y no soporta ninguna interpretación de rasgo.
    - id: LIM-07
      title: "RT no validado"
      description: >
        El RT es secundario y descriptivo. Con timebase de fallback se marca UNVERIFIED_TIMEBASE.
        Nunca gobierna dificultad, veredicto ni métrica primaria.

  exitCriteriaForNextStage:
    - "S3 sella el bloque technical completo con símbolos reales y gates verdes."
    - "Suite red-team con call-sites vivos y perfil honesto que no dispara ningún detector."
    - "Golden vectors pineados contra referencia independiente y probados por mutación."
    - "Banco de medida y banco de entrenamiento disjuntos, con sondas retenidas nunca servidas."
```

---

## Revisión crítica

**D1 — «El d'=3.352 no tiene definición operativa registrada; sin ella el número es decorativo.»**
Atendida. `measurement.aggregationPolicy.signalNoiseConvention` fija señal (20% con objetivo), ruido (80% sin objetivo), y las cuatro celdas: hit = toque en objetivo dentro de ventana; FA = toque en no-objetivo; miss = silencio ante objetivo; correct rejection = silencio ante no-objetivo, más la identidad de conteos. El d' reportado se lee contra esa tabla.

**D2 — «El bloque `technical` vacío bloquea el gate; los campos vacíos son relleno prohibido por `04_EXERCISE_QUALITY_STANDARD.md:251`.»**
Atendida. Ningún campo queda vacío: todo `technical` lleva `PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)` en la forma literal sancionada en `09_DECISION_LOG.md §20.4.1` (`FIX-FOCUS-S2-DOSSIER-FORMAT`, consequences). No es relleno genérico: es un compromiso con owner y gate nombrados. `technicalBlockRationale` explica por qué declarar símbolos inexistentes crearía un contrato falso.

**D3 — «La trazabilidad del scorer no está probada por este dossier.»**
Atendida por procedencia, no por re-ejecución. `scoringVersion: SCORE-FOC2-E01-0.1.0-candidate` con `scoringVersionProvenance` explícito, y la simulación nombrada en `validation.goldenCases.simulationRun`. Mismo patrón que el dossier hermano FOC2-E07. Este dossier consume el output de la tarea (5); no lo recalcula.

**D4 — «`SIDE_BIAS` está ausente: ¿hueco o decisión?»**
Atendida. Aparece explícitamente en `privacy_security.integrityThreats` con `applicable: false`, `status: NO_APLICABLE_POR_DISENO` y su razón (toque único en pantalla completa: no hay lados). Se documenta en vez de omitirse precisamente para que la ausencia de detector sea auditable.

**D5 — «Un umbral sobre |d'| detectaría al machacador; ¿por qué una regla de conteos?»**
Rechazada la premisa. La simulación muestra el MASHER con d' = −0.511, no ≈0, porque la corrección loglineal es asimétrica con celdas 24 vs 96. Un umbral sobre |d'| clasificaría mal ese caso. La regla `DEGENERATE_RESPONDING` se deriva solo de conteos (una única respuesta distinta en toda la sesión) y es inmune a ese sesgo.

**D6 — «Falta el nivel de azar; toda tarea de detección tiene uno.»**
Rechazada. En go-only con criterio libre, la probabilidad de un rechazo correcto depende de la disposición a responder — que es lo que mide el criterio. Fabricar un denominador sería inventar el dato. Se devuelve `NOT_APPLICABLE` como ausencia tipada (LIM-02) y d'/criterio cargan el peso.

**D7 — «120 ensayos son más de los ratificados (n=55): ¿inflación de dosis?»**
Rechazada. `MinSampleGate` exige `nMinPerCell=20` para DPRIME y el cálculo va POR CONDICIÓN, es decir por celda. Con n=55 al 20%, la celda-señal reúne 11 ensayos: por debajo del piso. Con 120 reúne 24: lo supera. No es inflación, es el mínimo que hace computable la métrica primaria.

**D8 — «El silencio es el 80% de los ensayos: la medida se ahoga en no-respuestas.»**
Atendida por `measurement.omissionRules`. El silencio no es un solo evento: ante señal es OMISSION y ES la señal medida (entra en el denominador del hitRate, no se descarta ni se imputa); ante no-objetivo es CORRECT REJECTION, la respuesta correcta esperada. No responder es la norma de esta paradigma; la medida separa las dos formas de silencio en vez de colapsarlas.

**D9 — «El generador viene de v1: ¿arrastra el eje de medida de v1?»**
Atendida. `content.generatorReuse.scope: GENERATOR_ONLY`, y LIM-01 documenta por qué: `SUSTAINED_FOCUS_INDEX` de v1 daba 100/100 con cero respuestas (`1 − cv` sobre lista vacía). El eje de medida se re-deriva desde cero.

**D10 — «El RT podría gobernar la dificultad por la puerta de atrás (staircase).»**
Atendida. `difficulty.adaptationRule.governedBy: ACCURACY_PER_CONDITION` y `rtNeverDrivesDifficulty: true`. El RT vive solo en `secondaryMetrics` con `role: DESCRIPTIVE_ONLY` y `neverGoverns: true` (Regla 2).

**D11 — «Las exclusiones de constructo podrían leerse como afirmaciones fuera de dominio.»**
Atendida. `construct.excludedInterpretations` está redactado íntegramente como negaciones de CONSTRUCTO, tomadas de `agent_outputs/02_efficacy_guardian.md:104`. Ninguna categoría sanitaria se nombra, ni siquiera para negarla.

**D12 — «¿Puede este ejercicio filtrarse al perfil cognitivo funcional?»**
No. `measurement.modeEligibility: [TRAINING_ONLY]`, `contributesToProfile: false`, `technical.profileWiring: NOT_WIRED_BY_DESIGN`, banco no registrado en `ProfileUpdatingBanks` con fail-closed a `SMALL_UNVALIDATED`, y `ADR-FOCUS-SCORING BLOCKED_BY_DESIGN`.

---

## Veredicto

**APTO como artefacto de gate de diseño (status `DESIGNED`), condicionado a S3.**

Los 12 bloques de `04_EXERCISE_QUALITY_STANDARD.md §5` están completos; ningún campo queda vacío. Las ocho exigencias de la revisión están cubiertas: bloques + dos encabezados finales (1), `scoringVersion` con procedencia (2), `PENDING_S3` en `technical` entero y en los campos de `validation` que crea S3 (3), convención señal/ruido go-only que da definición operativa al d'=3.352 (4), `SIDE_BIAS` como no aplicable por diseño (5), dosis de 120 ensayos justificada contra el piso `nMinPerCell=20` (6), `SUSTAINED_FOCUS_INDEX` retirado como caso canónico de ausencia que fabrica un máximo (7), y reglas de omisión propias de vigilancia go-only (8).

**Alcance de la evidencia:** la simulación demuestra ORDENAMIENTO DE PERFILES ADVERSARIALES SINTÉTICOS bajo un scorer candidato. NO demuestra validez de constructo, ni fiabilidad, ni transferencia, ni nada psicométrico con humanos.

**Bloqueos declarados, no ocultos:** implementación (`technical` completo), suite red-team con call-sites vivos, golden vectors pineados por mutación, y registro de banco — todos con owner `cognitive-engine-dev` y gate `T-S3`. No hay cableado al perfil y no puede haberlo mientras el estado sea `DESIGNED`.
