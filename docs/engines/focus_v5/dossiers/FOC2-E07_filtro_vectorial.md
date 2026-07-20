<!-- Focus V5 · S2 tarea (4) · Dossier de TRACER A. Formato: seccion 5 de
     docs/context_pack/04_EXERCISE_QUALITY_STANDARD.md + los dos encabezados adversariales del formato de
     docs/mindboost_exercise_design/ (FIX-FOCUS-S2-DOSSIER-FORMAT, 09_DECISION_LOG.md 20.4.1).
     Los campos PENDING_S3 son la UNICA forma no vacia sancionada para lo que S3 crea. -->

# FOC2-E07 «Filtro vectorial» — Tracer A (CONSTRAIN / inhibicion)

```yaml
identity:
  exerciseId: foc2-e07-filtro-vectorial
  exerciseVersion: 0.1.0-design
  visibleName: "Filtro vectorial"
  internalName: "FOC2-E07 flanker CONSTRAIN (Tracer A)"
  blockId: FOCUS_V5_CONSTRAIN
  owners: [cognitive-engine-dev, efficacy-guardian, focus-portfolio-owner]
  status: DESIGNED

user_value:
  oneSentencePromise: "Practica mantener la respuesta correcta cuando lo que rodea al objetivo empuja hacia el lado contrario."
  everydayRelevanceHypothesis: "HIPOTESIS NO VERIFICADA: la practica con distractores direccionales conflictivos podria acompanarse de mejor desempeno en tareas visuales analogas. No se afirma transferencia a la vida diaria; el portfolio marca la transferencia como HYP (T2 formas nuevas, T3) y no como resultado."
  targetPersonas: ["usuario que entrena Enfoque en sesiones cortas", "usuario que quiere seguir su propio progreso frente a su linea base"]
  sessionDurations: [5]

construct:
  primaryConstructId: FOCUS_CONSTRAIN
  secondaryConstructIds: []
  operationalDefinition: "Exactitud y sensibilidad (d') POR CONDICION de congruencia al identificar la direccion de un objetivo central rodeado de flancos congruentes o incongruentes, con deadline generoso, respuesta de eleccion izquierda/derecha y estimulos no-lexicos."
  excludedInterpretations:
    - "NO mide 'inhibicion general' ni autocontrol."
    - "NO mide productividad ni rendimiento cotidiano."
    - "NO demuestra transferencia a otras tareas ni a la vida diaria."
    - "NO establece un factor Focus unitario."
    - "NO establece los constructos M0-M5."
    - "NO valida el tiempo de respuesta como indice de capacidad."
    - "NO mide RESTRAIN (freno de una respuesta ya iniciada)."
    - "Con alpha aproximado 20, NO sostiene ninguna afirmacion psicometrica (fiabilidad, validez de constructo, invariancia)."
    - "Las cinco subhabilidades de Enfoque son ETIQUETAS de navegacion (ADR-FOCUS-CONSTRUCT opcion a); en C0 NINGUN subscore se muestra."
    - "NO es una nota, ni un valor de capacidad: ADR-FOCUS-SCORING sigue BLOCKED_BY_DESIGN."
  mechanismRationale: "El paradigma flanker crea competencia de respuesta entre el objetivo central y sus flancos. Con estimulos no-lexicos y cuotas de congruencia balanceadas, la exactitud por condicion refleja la resolucion de esa competencia y no la velocidad de lectura ni la proporcion aprendida."
  confounds:
    - "Contaminacion de Velocidad -> deadline generoso; RT y exactitud reportados SEPARADOS; RT nunca gobierna."
    - "Proporcion de congruencia aprendida -> cuotas 50/50 balanceadas + celdas catch."
    - "Difference-score de RT como score unico -> PROHIBIDO; primario es d'(INCONGRUENT), un nivel dentro de condicion."
    - "Techo de exactitud sin varianza -> dificultad adaptativa sobre separacion/similitud; falsador declarado."
    - "Sesgo de lado -> convencion de senal/ruido que expone el sesgo como criterio (c), no lo pliega en d'."
    - "Clase de dispositivo/pantalla -> declarado; invariancia por dispositivo NO verificada en C0."
  evidenceLabels: ["F03 executive", "E013 VERIFIED-en-doc", "pack FOC-E04 ACCEPT (tracer prioritario)", "Hautus (1995) correccion loglineal", "Acklam z(p) aproximacion racional"]

gameplay:
  coreLoop: "Aparece una fila de simbolos no-lexicos; el usuario responde la direccion del elemento CENTRAL tocando la mitad izquierda o derecha de la pantalla. Feedback minimo, siguiente ensayo."
  inputModality: "touch"
  tutorialFlow: "Explicacion de la regla (responde solo al central) + 8 ensayos guiados con correccion inmediata; no puntuan."
  practiceFlow: "12 ensayos de practica sin registro, mezcla congruente/incongruente, hasta 2 rondas si la exactitud de practica es baja."
  terminationRules:
    - "Fin natural: 112 ensayos completados."
    - "Abandono explicito del usuario."
    - "Fin por tiempo maximo de sesion (5 min)."
  interruptionRules:
    - "App a segundo plano: sesion pausada; el ensayo en curso se descarta, no se puntua."
    - "Reanudacion dentro de la misma sesion permitida; reanudacion posterior NO: la sesion queda incompleta."
  failureStates: ["ningun estado de fallo terminal: no hay perder ni game over"]
  antiFrustrationRules:
    - "Deadline generoso (no penalizar lentitud)."
    - "Sin racha visible ni castigo por error."
    - "Dificultad adaptativa suave sobre separacion y similitud, con suelo y techo."

content:
  source: procedural
  generatorVersion: "GEN-FOC2-E07-0.1.0 (REUTILIZA el generador flanker del ancestro v1 03_1_Enfoque.md; el EJE DE MEDIDA se re-deriva desde cero — el scoring v1, que hace CONGRUENCY_EFFECT metrica primaria y construye su formula sobre el, esta RETIRADO por EXERCISE_PORTFOLIO_V5.md:89)"
  itemBankVersion: "BANK-FOC2-E07-0.1.0 — bancos SEPARADOS training / measurement (Regla 4); formas paralelas; sondas retenidas disenadas ANTES de entrenar"
  seedPolicy: "Semilla determinista tipada (SessionSeed + RngVersion, DeterministicRng splitmix64 pinneado); nunca semilla de reloj; persistida con la sesion para replay."
  solutionValidator: "La direccion correcta se DERIVA de la orientacion del elemento central generado; nunca se almacena como etiqueta independiente ni aleatoria."
  exposurePolicy: "Cuotas balanceadas 50/50 congruente/incongruente y 50/50 izquierda/derecha por bloque; celdas catch; ledger de exposicion sobre el banco de measurement."
  localizationRisks: ["ninguno por diseno: estimulos NO-LEXICOS en el nucleo (Regla 5); solo el copy de instrucciones se localiza"]

difficulty:
  parameters: ["separacion objetivo-flanco", "similitud objetivo-flanco", "proporcion de congruencia (FIJA en measurement)"]
  levelDefinitions:
    - "L1 separacion amplia, flancos poco similares"
    - "L2 separacion media"
    - "L3 separacion estrecha"
    - "L4 separacion estrecha + alta similitud"
  adaptationPolicyVersion: "ADAPT-FOC2-E07-0.1.0"
  startingRule: "Primera sesion: L2. Sesiones posteriores: nivel final de la sesion anterior valida."
  promotionRule: "Exactitud INCONGRUENT >= 0.85 sostenida en una ventana -> sube un nivel."
  demotionRule: "Exactitud INCONGRUENT <= 0.60 en una ventana -> baja un nivel."
  explorationRule: "Celdas catch a nivel fijo intercaladas, excluidas de la adaptacion."
  ceilingFloorProtection: "Suelo L1, techo L4; la proporcion de congruencia NUNCA se adapta (confound); en modo measurement la dificultad queda CONGELADA."

measurement:
  modeEligibility: [TRAINING_ONLY]
  primaryMetrics:
    - "d'(INCONGRUENT) — sensibilidad POR CONDICION, un NIVEL dentro de una condicion, jamas una resta entre condiciones"
    - "exactitud(INCONGRUENT) corregida por azar"
    - "exactitud(CONGRUENT) corregida por azar"
    - "criterio c por condicion (medida del sesgo de lado)"
  secondaryMetrics:
    - "RT mediano por condicion (SECUNDARIO en C0, Regla 2; JAMAS gobierna)"
    - "efecto de congruencia (descriptivo, NUNCA gobernante — Regla 1)"
    - "intrusiones y omisiones por condicion"
    - "d'(CONGRUENT) como contraste descriptivo"
  timingContract: "Deadline generoso por ensayo; RT registrado pero no gobernante. El eje primario es accuracy-based, por lo que el ejercicio NO depende del timebase autoritativo para su metrica principal."
  omissionRules: "Ensayo sin respuesta dentro del deadline = OMISSION; cuenta en el denominador de exactitud, no se imputa direccion."
  outlierRules: "El rechazo de outliers se aplica SOLO a la tendencia central de RT (secundaria). Nunca a los conteos del 2x2 ni a d'."
  speedAccuracyPolicy: "Exactitud y RT se reportan SEPARADOS; ninguna combinacion velocidad-exactitud puntua. Responder rapido no mejora nada."
  aggregationPolicy: |
    Convencion senal/ruido: LEFT es la respuesta "si" — senal = ensayo con objetivo a la IZQUIERDA, ruido = ensayo con
    objetivo a la derecha. hit = objetivo izquierda respondido izquierda; falsa alarma = objetivo derecha respondido
    izquierda. Se rechaza explicitamente el atajo 2AFC d' = sqrt(2)*z(pc): ese atajo asume un observador SIN sesgo —
    exactamente lo que el perfil SIDE_BIAS viola — y pliega el sesgo DENTRO de la sensibilidad. Bajo la convencion
    elegida, el criterio c ES la medida del sesgo de lado y d' queda limpio de el.
    Correccion loglineal de Hautus (1995) aplicada SIEMPRE, incondicionalmente, para que d' sea siempre finito.
    z(p) por aproximacion racional de Acklam, con goldens de fuente independiente.
    Agregacion POR CONDICION; nunca se promedian condiciones en un solo numero.
    Veredicto de validez basado en el INTERVALO: el limite inferior del IC 95% de d' debe superar cero (no el punto).
    Ordenamiento de perfiles adversariales sinteticos bajo el scorer candidato (20 tests verdes, probado por mutacion). NO es evidencia humana ni psicometrica:
      HONEST    Valid | CONGRUENT n=28/28 d'=2.536+-0.455 c=-0.096 | INCONGRUENT n=28/28 d'=3.285+-0.652 c=0.471
      EXPERT    Valid | CONGRUENT d'=3.742+-0.698 | INCONGRUENT d'=3.478+-0.667
      MASHER    Invalidated(DEGENERATE_RESPONDING) | d'=0.0+-0.815 c=-2.114 (ambas condiciones)
      WAITER    Invalidated(NO_DISCRIMINATION) | CONGRUENT d'=0.5+-0.368 | INCONGRUENT d'=0.25+-0.359
      SIDE_BIAS Invalidated(DEGENERATE_RESPONDING) | identico a MASHER en el 2x2 (solo difiere en latencia)
  officialEvidenceCap: "TRAINING_ONLY. No alimenta el perfil cognitivo funcional. Sin banco de measurement validado ni detector anti-gaming registrado, la evidencia queda topada; ninguna dimension sale de CALIBRATING por este ejercicio."
  calibrationRequirements: "Dosis 112 ensayos = 56 por condicion (28 senal + 28 ruido). Corregida al alza desde el n=55 ratificado porque, al puntuar POR CONDICION, quedaban ~14 por celda y MinSampleGate exige 20. Sin la dosis completa por condicion, la sesion no produce veredicto valido."
  scoringVersion: "SCORE-FOC2-E07-0.1.0-candidate"
  uncertaintyMethod: "Error estandar de d' calculado analiticamente; veredicto por limite inferior del IC 95%, nunca por el punto."

feedback:
  realtime: ["marca breve de acierto/error por ensayo", "progreso de ensayos restantes"]
  sessionSummary:
    - "exactitud por condicion"
    - "numero de ensayos validos"
    - "comparacion frente a tu propia linea base cuando existan sesiones previas suficientes"
    - "estado CALIBRANDO con guion (—) cuando la evidencia es insuficiente; NUNCA un cero fabricado"
  deterministicNextActions:
    - "repetir el ejercicio manana"
    - "bajar un nivel si la exactitud incongruente fue baja"
    - "continuar al siguiente ejercicio del plan"
  prohibitedClaims:
    - "cualquier lenguaje clinico o de evaluacion de salud, incluso para negarlo"
    - "afirmar mejora de la atencion general o de la capacidad cognitiva global"
    - "afirmar transferencia a estudios, trabajo o vida diaria"
    - "afirmar 'inhibicion' o autocontrol como rasgo de la persona"
    - "presentar una nota, puntuacion de capacidad, edad cerebral, percentil o comparacion entre usuarios"
    - "presentar el efecto de congruencia, el RT o cualquier difference-score como resultado gobernante"
    - "presentar subscores de subhabilidad (en C0 NINGUNO se muestra)"
    - "afirmar cualquier propiedad psicometrica (fiabilidad, validez, invariancia)"

accessibility:
  requiredCapabilities: ["vision de formas y orientacion", "toque en dos zonas amplias"]
  supportedAdjustments: ["tamano de estimulo", "alto contraste", "deadline extendido", "sin animacion de transicion"]
  alternativeMode: "modo de contraste alto y estimulo ampliado"
  comparabilityImpact: "Cualquier ajuste de tamano, contraste o deadline rompe la comparabilidad con la linea base previa; la sesion se marca NO COMPARABLE y no se compara contra sesiones con otros ajustes."
  screenReaderPolicy: "El nucleo es visual-espacial y NO es accesible por lector de pantalla; se anuncian instrucciones, progreso y resumen, nunca el estimulo. No se ofrece un sustituto auditivo que cambiaria el constructo."
  reducedMotionPolicy: "Sin movimiento decorativo; con movimiento reducido activo, las transiciones son cortes secos. La presentacion del estimulo es estatica por diseno."

privacy_security:
  requiredPermissions: []
  optionalPermissions: []
  rawSensitiveData: ["ensayos crudos (direccion, respuesta, latencia) durante la sesion, EN MEMORIA"]
  localOnlyData: ["resumen de sesion cifrado en SecureKeyValueStore"]
  retentionClass: "local cifrado, account-bound, borrado con la cuenta; solo RESUMENES, nunca ensayos crudos persistidos"
  integrityThreats:
    - "aporreo (MASHER) -> detectado, Invalidated(DEGENERATE_RESPONDING)"
    - "no responder / esperar (WAITER) -> detectado, Invalidated(NO_DISCRIMINATION)"
    - "sesgo de lado sistematico (SIDE_BIAS) -> detectado via criterio, Invalidated(DEGENERATE_RESPONDING)"
    - "repeticion del mismo item (exposicion) -> ledger de exposicion + bancos separados"
    - "modificacion del reloj -> irrelevante para el eje primario (accuracy-based)"

technical:
  engineModule: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  uiRoute: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  schemaVersion: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  offlinePolicy: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  syncIdempotencyKey: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  performanceBudget: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  featureFlag: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)

validation:
  unitTests: [PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)]
  propertyTests: [PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)]
  goldenCases:
    - "z(p) de Acklam contra goldens de fuente INDEPENDIENTE (ya ejecutados en la simulacion del scorer candidato)"
    - "los 5 perfiles sinteticos con sus valores exactos: HONEST / EXPERT Valid; MASHER / SIDE_BIAS Invalidated(DEGENERATE_RESPONDING); WAITER Invalidated(NO_DISCRIMINATION)"
    - "el resto de goldens de motor y UI: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)"
  uiTests: [PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)]
  accessibilityTests: [PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)]
  deviceMatrix: [PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)]
  humanPilotPlan: PENDING_S3 (owner: cognitive-engine-dev, gate: T-S3)
  knownLimitations:
    - "El scoring del ancestro v1 esta RETIRADO (CONGRUENCY_EFFECT gobernante). Solo el GENERADOR se reutiliza."
    - "Invariancia por clase de dispositivo NO verificada."
    - "Falsador declarado abierto: techo de exactitud sin varianza, o un modelo de velocidad que explique la estructura."
    - "Con alpha aproximado 20, nada psicometrico es sostenible."
    - "TRAINING_ONLY: no alimenta el perfil cognitivo funcional."
  nextGate: "T-S3"
```

## Revision critica

1. **El ancestro contamina por reutilizacion silenciosa.** `docs/mindboost_exercise_design/03_1_Enfoque.md:119,:128` hace de CONGRUENCY_EFFECT metrica primaria y construye su formula encima. `EXERCISE_PORTFOLIO_V5.md:89` lo RETIRA. Si S3 reutiliza el generador sin re-derivar el eje de medida, el difference-score vuelve por la puerta de atras. Exigencia: `content.generatorVersion` registra la reutilizacion del generador Y la re-derivacion desde cero del eje; ningun simbolo del scoring v1 puede importarse.
2. **d' de una resta seria el mismo defecto con otro nombre.** `d'(INCONGRUENT) - d'(CONGRUENT)` es un difference-score. El primario es un NIVEL dentro de una condicion. El efecto de congruencia queda computable pero DESCRIPTIVO.
3. **El atajo 2AFC habria escondido justo el adversario que hay que cazar.** `d'=sqrt(2)*z(pc)` asume observador insesgado; SIDE_BIAS lo viola y el atajo le habria dado sensibilidad prestada. Con LEFT como "si", SIDE_BIAS cae como criterio extremo (c=-2.114) y d'=0.0.
4. **El veredicto por punto habria pasado ruido.** WAITER tiene d'=0.5+-0.368 en CONGRUENT: punto positivo, limite inferior bajo cero. Solo el criterio de intervalo lo invalida.
5. **La dosis ratificada n=55 era insuficiente al ir POR CONDICION** (~14/celda vs MinSampleGate 20). Corregida a 112 = 56/condicion. Si S3 recorta la dosis, el veredicto valido desaparece.
6. **RT en primaryMetrics es el fallo mas facil de cometer en S3.** Regla 2: RT SECUNDARIO en C0, siempre en `measurement.secondaryMetrics`.
7. **Subscores.** Las 5 subhabilidades son etiquetas de navegacion; en C0 no se muestra ninguno. CONSTRAIN es una de las tres que podrian tener subscore post-V3, lo que hace tentador adelantarlo. No.
8. **Techo de exactitud.** EXPERT da d'(CONGRUENT)=3.742 > d'(INCONGRUENT)=3.478: sin varianza suficiente el orden entre condiciones se invierte. Es el falsador declarado, no un defecto oculto.
9. **Estimulos no-lexicos.** Regla 5. Cualquier tentacion de usar letras o flechas con carga verbal reintroduce lectura como confound.

## Veredicto

**APROBADO para pasar a T-S3** como Tracer A, con `identity.status = DESIGNED`, `modeEligibility = [TRAINING_ONLY]` y ADR-FOCUS-SCORING mantenido BLOCKED_BY_DESIGN.

Cambios requeridos antes de cerrar T-S3:
1. Motor S3 sin ningun import del scoring v1; solo el generador se reutiliza.
2. Dosis 112 (56/condicion) fijada en el motor; test que falle si baja de 20 por celda.
3. Test de barrera: RT no puede aparecer en `primaryMetrics`; efecto de congruencia no puede gobernar el veredicto.
4. Bancos training/measurement separados y sondas retenidas creadas ANTES de la primera sesion de entrenamiento (Regla 4).
5. Los 5 perfiles sinteticos como goldens del motor con sus valores exactos, probados por mutacion.
6. Copy de resumen revisado por el scanner de lexico prohibido: CALIBRANDO se muestra como guion, nunca como cero.