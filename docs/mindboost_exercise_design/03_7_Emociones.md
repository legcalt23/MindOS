# Emociones

# MindBoost Emociones — Catalogo de 8 ejercicios de calidad suprema

> Fronteras de seguridad vinculantes: entrenamos un **perfil cognitivo funcional**, no diagnostico, no CI, no terapia. Toda salida sin soporte estadistico se reporta `null` (em-dash), nunca `0`; todo score arranca `preliminary = true`. La induccion es **leve-only** con techo duro `InductionIntensity.OPERATIONAL_CEILING = 3`: el `DefaultEmotionAdaptiveEngine` jamas auto-sube intensidad; solo sube **dificultad** sobre UN `EmotionAxis` por decision. Los `OverloadProxy` ARMAN un check-in, **nunca infieren estado emocional**. El `EmotionSessionAssembler` es la unica autoridad de clasificacion; los contadores en vivo del holder son display-only. Toda copy pasa por `EmotionTerminologyScanner` (whole-word, accent- y hyphen-insensitive).

Los 8 ejercicios son mecanicamente distintos (no variaciones del mismo loop). Mapean sobre los simbolos reales del repo: `EmotionTrialGenerator`, `EmotionStimulusPayload` (`AffectiveGoNoGo`/`TwoAltPerceptual`/`Classification`), `EmotionResponseKey`, `EmotionMetricKey`, `EmotionAxis`, `DefaultEmotionMetricsEngine`, `DefaultEmotionAdaptiveEngine`, `EmotionAdaptiveInput/Decision`, `AdaptiveZone`, `EmotionCapacity`, `TransferLabel`, renderers `EmotionGoNoGoField`/`EmotionTwoAltField`/`EmotionFourCategoryField`.

---

## EM-A · Freno con Senuelo Afectivo (Stop-on-Affect)
### Modulo
Principal: Emociones (capacidad `PAUSE_BEFORE_ACTION` / control inhibitorio bajo carga afectiva leve). Secundarios: Enfoque (inhibicion go/no-go), Velocidad (timebase de alta resolucion para la SSD), Profile (contribucion coarse one-way).

### Objetivo cognitivo
Entrenar la **inhibicion de una respuesta ya iniciada** (parada reactiva) cuando aparece una senal de stop, con un distractor afectivo congruente/incongruente que aumenta la carga sin inducir estado. Aisla control inhibitorio del mero tiempo de reaccion via SSRT estimado por escalera SSD.

### Hipotesis de transferencia
SI plausible: near transfer a tareas go/no-go y stop-signal del propio paradigma (mejora de SSRT contra baseline personal). NO afirmar: reduccion de impulsividad "en la vida real", control de impulsos clinicos, ni far transfer a decisiones financieras/sociales. `TransferLabel.PLAUSIBLE` para near; el copy diferido nunca afirma control emocional general.

### Loop principal
1. Estimulo: marco GO (abierto) aparece; el usuario debe responder rapido (PRIMARY). 2. Decision: en una fraccion de trials, tras una **SSD** (stop-signal delay) el marco cambia a senal de STOP -> debe RETENER. Un `affectiveWord` mild se muestra como distractor (congruente/incongruente con el frame). 3. Feedback: inmediato por trial (acierto-go / inhibicion correcta / falsa alarma / omision) con color+forma. 4. Adaptacion: la SSD sube/baja por escalera 1-up/1-down (online) para fijar ~50% de inhibicion exitosa; la dificultad afectiva sube otro eje. 5. Siguiente trial.

### Reglas
GO = responder PRIMARY dentro de la ventana. STOP = NO responder tras la senal. La SSD es **online** (depende de la respuesta previa): inhibicion exitosa -> SSD +step (mas dificil); fallida -> SSD -step. El `affectiveWord` jamas determina la respuesta (honestidad: la regla es solo frame GO->STOP). ~30% de trials son stop.

### Inputs
Tap unico (PRIMARY) sobre zona grande para GO; **ausencia de tap** = inhibicion. Sin swipe ni teclado. Exit siempre visible.

### Variables de dificultad (rangos)
1. `EmotionAxis.INHIBITORY_DEMAND` (proporcion de stop-trials, 0.20–0.40). 2. SSD base online (120–450 ms, escalera). 3. `EmotionAxis.TEMPORAL_PRESSURE` (ventana de respuesta GO, 1100–650 ms). 4. `EmotionAxis.AFFECTIVE_INTERFERENCE` (ratio incongruente, 0.0–0.6). 5. ISI jitter (300–900 ms, anti-ritmo). 6. `EmotionAxis.TIME_ON_TASK` (trials/bloque, 24–40). 7. Intensidad afectiva del banco (1–3, techo duro 3).

### Algoritmo adaptativo
Escalera SSD 1-up/1-down online por-trial dentro del bloque (converge a ~50% inhibicion). Entre bloques, `DefaultEmotionAdaptiveEngine.decide(EmotionAdaptiveInput)` mueve UN eje: prioridad `safetyStop -> reduceIntensity -> checkIn -> cooldown -> stepDown -> hold -> stepUp`. Step-up gated: `!escalationLocked && consecutiveErrors==0 && selfReport.intensity<=2 && zone mastery (COMMISSION)`. Plateau: ausencia de mejora de SSRT en N bloques -> switch de eje (de `TEMPORAL_PRESSURE` a `AFFECTIVE_INTERFERENCE`). Baseline personal ancla la SSD inicial.

### Metricas primarias
`COMMISSION_RATE` (falsa-alarma en stop = falló inhibicion), SSRT estimado (derivado de SSD media + RT-go median; expuesto como `IMPULSIVITY` index gated `confident=n>=minN`), `PAUSE_BEFORE_ACTION_RATE`.

### Metricas secundarias
`RT_MEDIAN` (solo HIT-go, post-MAD), `RT_CV`, `OMISSIONS` (lapsos go), `AFFECTIVE_INTERFERENCE_COST` (1.0 - cost congruente vs incongruente, gated MIN_CELL=5), `ANTICIPATIONS` (RT<350 crudos).

### Senales de fatiga o mala calidad
`OverloadProxy.SUSTAINED_SLOWING` (deriva RT-go vs baseline rolling intra-sesion), `ERROR_STREAK`, `EXTREME_RT_VARIABILITY`, RT-go median creciente con SSD estancada (escalera no converge -> bloque contaminado). `n<minN` en stop-trials -> SSRT `unavailable`.

### Scoring sugerido
`inhibitionScore = w1*(1 - COMMISSION_RATE) + w2*(1 - normSSRT) + w3*goAccuracy`, con `normSSRT = clamp((SSRT - SSRT_floor)/(SSRT_ceil - SSRT_floor),0,1)`. Barra `null` si `!confident`. `preliminary=true`. Pesos `w1=0.5,w2=0.35,w3=0.15`.

### Antitrampa / calidad de datos
RT<`IMPULSIVE_FLOOR_MS`(350) = anticipacion (no cuenta como HIT). Estrategia de "no responder nunca para no fallar stop" se detecta: `goAccuracy` cae -> `OMISSIONS` alto invalida el bloque. SSD online impide farming de la ventana. Latencias no-monotonas rechazadas.

### Variantes
1. **Freno visual** (frame abierto/cerrado, base). 2. **Freno por cambio de color** (GO verde -> STOP rojo+forma, daltonico-safe). 3. **Freno selectivo** (responder solo a frames de un color; STOP en el otro — anade discriminacion). Las tres reusan el mismo SSD-tracker.

### UX
Pantalla: zona tap grande central (`MinTouchTarget` >= 48dp via token), cue de regla persistente arriba, `affectiveWord` como subtitulo neutro. Feedback inmediato: borde verde/rojo + glifo (check/stop) ~200 ms. Stop = overlay rojo con forma de mano (no solo hue). Microanimacion funcional: el frame "se cierra" al disparar STOP (no decorativa). Duracion ideal 3–4 min/bloque, 2 bloques.

### Implementacion KMP
Reusa **renderer go/no-go** (`EmotionGoNoGoField`) con extension de dos fases (GO -> STOP, patron del E4 Freno de focus). Genera `EmotionStimulusPayload.AffectiveGoNoGo(frameOpen, affectiveWord, congruent)` ampliado con un `stopSignalDelayMs` resuelto online por el holder y registrado en `RecordedEmotionTrial`. `EmotionTrialGenerator.generate(difficulty, spec, seed)` deterministico (esquema + tracker SSD; la SSD efectiva la fija el runtime). Clasificacion: el `EmotionTrialClassifier`/`EmotionSessionAssembler` necesita una rama `classifyStop` (HIT-go / CORRECT_REJECTION / FALSE_ALARM / OMISSION). Metricas via `DefaultEmotionMetricsEngine.goNoGoMetrics` extendida con SSRT. Adaptacion: `DefaultEmotionAdaptiveEngine` (zona COMMISSION). Persistencia: DTO `@Serializable`+`schemaVersion` -> `SecureKeyValueStore` (namespace Emotions, solo summary). Registro de eventos: `trial.go`, `trial.stop_success`, `trial.stop_fail`, `ssd.update`, `proxy.armed`, `checkin.shown`.

```kotlin
data class StopOnAffectTrial(val ssdMs: Long, val wasStop: Boolean, val frameOpen: Boolean,
    val congruent: Boolean, val responded: Boolean, val latencyMs: Long?)
```

### Riesgos
SSRT es ESTIMADO (no medido directo): si la escalera no converge, no reportar. La estrategia de omision masiva contamina; gate de `goAccuracy` minimo obligatorio. Riesgo de medir solo velocidad si `INHIBITORY_DEMAND` muy bajo.

### Revision critica
El Revisor exige: (a) SSRT solo se expone si la escalera convergio (rango SSD estable >= K trials) y `goAccuracy >= 0.6`, si no `IMPULSIVITY.unavailable`; (b) prohibir el "no-respondas-nunca" con un suelo de go-accuracy que invalide el bloque, no que premie; (c) el `affectiveWord` debe estar escaneado y dentro del techo leve, y un test de paridad debe probar que NO correlaciona con la respuesta correcta (anti render deshonesto); (d) requiere rama `classifyStop` en el assembler (deuda real, no opcional). Sin (a)–(d), es un go/no-go disfrazado.

### Veredicto
Aprobado con cambios — solido y medible, pero exige la rama `classifyStop` en el assembler y el doble gate (convergencia SSD + suelo go-accuracy) antes de exponer SSRT.

---

## EM-B · Reentrada tras Error (Post-Error Re-engagement)
### Modulo
Principal: Emociones (`POST_ERROR_RECOVERY`). Secundarios: Enfoque (control atencional post-error), Profile.

### Objetivo cognitivo
Entrenar la **recuperacion funcional tras un error**: tras un fallo, mantener accuracy sin sobre-frenar (post-error slowing patologico) ni impulsividad compensatoria. Mide el composite condicional ya codificado en `postErrorRecovery` (EM2): slowing solo penaliza si la accuracy NO se recupero.

### Hipotesis de transferencia
SI plausible: near transfer a estabilidad de rendimiento tras error en tareas de eleccion. NO afirmar: resiliencia emocional, tolerancia a la frustracion "vital", ni mejora de autoestima. `TransferLabel.PLAUSIBLE`.

### Loop principal
1. Estimulo: discriminacion perceptual de 2 alternativas (LEFT/RIGHT); algunos trials son `harder` (near-threshold, error-likely plantado). 2. Decision: elegir lado. 3. Feedback: inmediato; tras un error, el siguiente trial se marca como "ventana de reentrada". 4. Adaptacion: ajusta `discriminability` y densidad de trials `harder`. 5. Siguiente trial.

### Reglas
Correcto = lado correcto dentro de ventana. Los trials `harder` garantizan secuencias post-error medibles **sin induccion injusta** (la dificultad es perceptual, no afectiva). Se requieren `MIN_POST_ERROR=3` pares para reportar. Se mide el bloque post-error (1–3 trials tras cada fallo).

### Inputs
Tap LEFT/RIGHT en mitades tactiles de pantalla completa. Sin teclado.

### Variables de dificultad (rangos)
1. `EmotionAxis.POST_ERROR_LOAD` (densidad de `harder`, 0.15–0.45). 2. `discriminability` base (0.85–0.45). 3. `EmotionAxis.TEMPORAL_PRESSURE` (ventana, 1400–800 ms). 4. `EmotionAxis.AFFECTIVE_INTERFERENCE` (distractor leve, 0.0–0.5). 5. ISI jitter (300–800 ms). 6. `EmotionAxis.TIME_ON_TASK` (28–44 trials).

### Algoritmo adaptativo
Zona `GENERAL` (~0.80–0.85). `decide`: si `consecutiveErrors>0` -> nunca step-up (gate). Step-down si accuracy < banda o `POST_ERROR_ACCURACY_DROP` proxy. Plateau (sin mejora de `POST_ERROR_RECOVERY`) -> switch a `POST_ERROR_LOAD`. Baseline ancla `discriminability` inicial.

### Metricas primarias
`POST_ERROR_RECOVERY` (composite: slowing penalizado SOLO si `!accRecovered`, `accRecovered = postErrAcc >= baseAcc - ACC_TOL`), `POST_ERROR_ACCURACY`.

### Metricas secundarias
`POST_ERROR_SLOWING` (RT post-error vs base; descriptivo), `IMPULSIVITY` post-error (RT<`POST_ERROR_IMPULSE_MS`=250), `ACCURACY`, `RT_MEDIAN`, `RT_CV`.

### Senales de fatiga o mala calidad
`POST_ERROR_ACCURACY_DROP`, `POST_ERROR_IMPULSIVITY` (proxies que ARMAN check-in). `n<MIN_POST_ERROR` -> `POST_ERROR_RECOVERY.unavailable`. Deriva global de accuracy = fatiga.

### Scoring sugerido
`recoveryScore = clamp(1 - penalty, 0, 1)` donde `penalty = (accRecovered ? impulsePenalty : slowingPenalty + accGapPenalty)`; `accGap = max(0, baseAcc - postErrAcc)`. `null` si no confident. `preliminary=true`.

### Antitrampa / calidad de datos
Trial `harder` plantado deterministicamente (seed) impide al usuario predecirlo. RT<250 post-error = impulsividad (penaliza, no premia). Se excluyen del computo los pares donde el "error" fue una anticipacion (no es error genuino de discriminacion).

### Variantes
1. **Reentrada perceptual** (orientacion/gabor-like, base). 2. **Reentrada por conteo** (mas/menos elementos, 2-alt). 3. **Reentrada con cue de regla** (la regla LEFT/RIGHT cambia cada N trials -> error de regla + recuperacion). Reusan renderer 2-alt.

### UX
Mitades LEFT/RIGHT full-screen, estimulo central. Feedback de error: pulso neutro (NO culpa) + frase de encuadre "sigue, contra tu baseline". Sin penalizacion visual agresiva. Microanimacion: transicion suave al siguiente (anti-rumiacion). Duracion 3–4 min/bloque.

### Implementacion KMP
Reusa **EM2RecuperacionGenerator** (ya wired) + **renderer 2-alt** (`EmotionTwoAltField`). `EmotionStimulusPayload.TwoAltPerceptual(correct, discriminability, harder)` ya existe. Clasificacion: `EmotionTrialClassifier.classify` (directional). Metricas: `DefaultEmotionMetricsEngine.choiceMetrics` + `postErrorRecovery`. Adaptacion: `DefaultEmotionAdaptiveEngine`. Persistencia summary via `SecureKeyValueStore`. Eventos: `trial.choice`, `trial.error`, `post_error.window`, `post_error.recovered`, `proxy.armed`. **No requiere renderer nuevo** (es un redisenio premium de EM2 con metrica composite ya codificada).

### Riesgos
Si los trials `harder` son demasiado frecuentes, frustracion -> arma check-in y deload. Distinguir error de regla de error perceptual exige etiquetar la causa en el trial.

### Revision critica
El Revisor exige: (a) excluir anticipaciones del conjunto de "errores" que abren ventana post-error (un error genuino requiere respuesta-en-ventana incorrecta); (b) `ACC_TOL` y `POST_ERROR_IMPULSE_MS` nombrados, no magicos; (c) probar que `harder` plantado garantiza `>=MIN_POST_ERROR` pares en el peor seed, si no el bloque no puede reportar y debe avisarlo; (d) la frase de encuadre debe pasar el scanner (sin culpa, sin claim). Cumplido en gran parte por el codigo EM2 existente; (a) y (c) son el riesgo real.

### Veredicto
Aprobado con cambios — la metrica composite ya es honesta; falta blindar la definicion de "error genuino" (excluir anticipaciones) y garantizar suficiencia post-error por seed.

---

## EM-C · Separador Estado/Pensamiento/Impulso/Hecho (Granularity Quad)
### Modulo
Principal: Emociones (`EMOTION_THOUGHT_SEPARATION`, `GRANULARITY`). Secundarios: Lenguaje (clasificacion semantica neutra), Profile.

### Objetivo cognitivo
Entrenar la **separacion metacognitiva** de cuatro registros de una frase neutra: estado interno, pensamiento, impulso, hecho. Mide granularidad (categorias distintas correctas / 4) y exactitud ajustada por azar.

### Hipotesis de transferencia
SI plausible (con cautela): near transfer a etiquetado de registros en frases del mismo banco. NO afirmar: insight terapeutico, regulacion emocional clinica, ni que "separar pensamientos cure ansiedad". `TransferLabel.NOT_DEMONSTRATED` (holdout authored pero transferencia empirica pendiente).

### Loop principal
1. Estimulo: frase neutra cotidiana (`Classification.phrase`). 2. Decision: clasificar en 1 de 4 categorias fijas (`CATEGORY_A..D` mapeadas a quadrant fijo toda la sesion). 3. Feedback: correcto/incorrecto + la categoria correcta. 4. Adaptacion: sube `CATEGORY_AMBIGUITY` (frases mas ambiguas). 5. Siguiente trial.

### Reglas
4 botones fijos en quadrantes estables (mapeo quadrant->categoria constante toda la sesion). El holdout (`isEval`) usa items disjuntos del entrenamiento. Correcto = categoria == `truth`. Exactitud ajustada por azar: `((etAcc - 0.25)/(1-0.25))`.

### Inputs
Seleccion de 4 botones (no LEFT/RIGHT). Tap unico por trial.

### Variables de dificultad (rangos)
1. `EmotionAxis.CATEGORY_AMBIGUITY` (proporcion de frases ambiguas, 0.1–0.6). 2. Longitud de frase (4–14 palabras). 3. `EmotionAxis.TEMPORAL_PRESSURE` (ventana, 6000–2500 ms). 4. Densidad de pares cuasi-sinonimos (0.0–0.4). 5. `EmotionAxis.TIME_ON_TASK` (16–28 trials). 6. Ratio eval/holdout (0.2–0.3 fijo).

### Algoritmo adaptativo
Zona `GENERAL`. `decide` sube `CATEGORY_AMBIGUITY` solo con mastery; step-down si `CATEGORY_ACCURACY` < banda. Plateau -> mas pares cuasi-sinonimos. Baseline ancla ambiguedad inicial.

### Metricas primarias
`EMOTION_THOUGHT_SEPARATION` (accuracy ajustada por azar), `CATEGORY_ACCURACY`, `GRANULARITY_INDEX` (categorias distintas correctas / 4).

### Metricas secundarias
`RT_MEDIAN` (solo CORRECT, post-MAD), `EmotionConfusionMatrix` (4x4, solo para esta capacity), confusiones por par (descriptivo, no clinico).

### Senales de fatiga o mala calidad
Colapso a una sola categoria (boton-spam) -> `GRANULARITY_INDEX` bajo + matriz degenerada. RT muy bajo uniforme = respuesta sin lectura. `n<minN` por categoria -> celda `unavailable`.

### Scoring sugerido
`separationScore = 0.6*adjAcc + 0.4*GRANULARITY_INDEX`, `adjAcc = (etAcc-0.25)/0.75 clamp 0..1`. Reporta tambien matriz de confusion como senal descriptiva. `null` si no confident.

### Antitrampa / calidad de datos
Mapeo quadrant->categoria FIJO impide aprender posicion en vez de concepto (pero rota entre sesiones). Boton-spam detectado por entropia de respuestas baja. Holdout disjunto evita inflar por memorizacion de items vistos. RT-floor por lectura minima (no aceptar < tiempo de lectura plausible por longitud).

### Variantes
1. **Separador 4-cat** (base). 2. **Separador binario progresivo** (estado-vs-hecho, luego pensamiento-vs-impulso — reduce carga inicial). 3. **Separador con frase-fuente** (clasificar Y senalar la palabra-clave que decide — anade evidencia, requiere drag/seleccion de token). V1/V2 reusan renderer 4-cat; V3 necesita renderer de seleccion de token.

### UX
4 botones en grid 2x2 estable (`MinTouchTarget`), frase central legible <1s. Feedback: resalta boton correcto. Sin animacion decorativa. Duracion 3–5 min (menos trials, mas lectura). Texto escalable.

### Implementacion KMP
Reusa **EM3SeparadorGenerator** (wired) + **renderer 4-cat** (`EmotionFourCategoryField`). `EmotionStimulusPayload.Classification(phrase, truth, isEval)` ya existe; `RecordedEmotionTrial.chosenCategory` transporta la eleccion. Clasificacion + `EmotionConfusionMatrix` en `EmotionSessionAssembler` (solo `EMOTION_THOUGHT_SEPARATION`). Metricas: `DefaultEmotionMetricsEngine.classificationMetrics`. Banco: `EmotionContent.SEPARATOR_ITEMS` (40, 7 train + 3 eval/categoria, disjuntos). **No requiere renderer nuevo** salvo V3 (token-select). Eventos: `trial.classify`, `category.confusion`, `holdout.item`.

### Riesgos
Dependencia de idioma/cultura en las frases -> requiere banco localizado y re-escaneado por idioma. Ambiguedad intrinseca de "pensamiento vs impulso" puede generar items sin verdad clara -> revision humana del banco.

### Revision critica
El Revisor exige: (a) cada item del banco debe tener verdad inter-rater alta (>= 80% acuerdo) o se descarta — sin esto la "verdad" es arbitraria; (b) el RT-floor por lectura debe escalar con longitud de frase, no ser constante; (c) `GRANULARITY_INDEX`=categorias/4 es fragil con pocos trials — debe gated por `n` por categoria; (d) NOT_DEMONSTRATED es honesto: prohibido cualquier copy que sugiera beneficio emocional. (a) es bloqueante: sin acuerdo inter-rater, mide ruido.

### Veredicto
Aprobado con cambios — mecanica y metrica solidas, pero la calidad del banco (acuerdo inter-rater + localizacion) es condicion previa a exponerlo.

---

## EM-D · Espera Calibrada (Tolerance Hold)
### Modulo
Principal: Emociones (`FRUSTRATION_TOLERANCE`, capacidad `FRUSTRATION_TOLERANCE`). Secundarios: Enfoque (espera sostenida), Profile.

### Objetivo cognitivo
Entrenar la **persistencia segura ante demora**: mantener una accion sostenida (o esperar una ventana variable) sin abandonar antes de tiempo ni actuar impulsivamente, midiendo tolerancia a la demora **sin** induccion de frustracion (la demora es la unica fuente de carga, dentro del techo leve).

### Hipotesis de transferencia
SI plausible: near transfer a tareas de espera/demora del paradigma. NO afirmar: tolerancia a la frustracion "en la vida", autocontrol global, ni paciencia como rasgo. `TransferLabel.PLACEBO_RISK` (alto riesgo de claim inflado -> copy ultra-conservador).

### Loop principal
1. Estimulo: aparece un "objetivo madurando" (barra que se llena en tiempo variable). 2. Decision: tap SOLO cuando este maduro (ventana al final); tap temprano = impulsivo, no-tap = abandono. 3. Feedback: maduro/temprano/abandono. 4. Adaptacion: alarga el tiempo de maduracion. 5. Siguiente trial.

### Reglas
La ventana de "maduro" es variable y NO senalizada por countdown explicito (se infiere del llenado). Tap antes de la ventana = `FALSE_ALARM` (impulsivo). No-tap hasta el fin = `ABANDONMENT`/`OMISSION`. Tap en ventana = HIT. El abandono SIEMPRE permitido (exit visible) y nunca penalizado con culpa.

### Inputs
Tap unico temporizado (PRIMARY). Opcional hold-and-release como variante. Exit/abandono siempre disponible.

### Variables de dificultad (rangos)
1. `EmotionAxis.TIME_ON_TASK` (tiempo de maduracion, 1500–9000 ms). 2. Varianza del tiempo de maduracion (CV 0.1–0.5, incertidumbre). 3. Anchura de la ventana "maduro" (900–300 ms). 4. `EmotionAxis.AFFECTIVE_INTERFERENCE` (distractor leve durante la espera, 0.0–0.4). 5. Numero de trials (12–22; pocos por la duracion). 6. Densidad de "objetivos que nunca maduran" (catch trials, 0.0–0.15).

### Algoritmo adaptativo
Zona `TOLERANCE` (~0.70–0.80, safety estricta). `decide` sube tiempo de maduracion solo con mastery; `REPEATED_EXIT_ATTEMPTS`/`REPEATED_PAUSE` -> check-in -> reduce. NUNCA sube con `selfReport.intensity>2`. Plateau -> sube varianza (incertidumbre), no el tiempo medio.

### Metricas primarias
`SAFE_PERSISTENCE_INDEX` (HITs maduros / oportunidades, penalizando impulsivos pero NO abandonos seguros), `FRUSTRATION_TOLERANCE_INDEX` (tiempo medio esperado en HITs vs baseline).

### Metricas secundarias
`IMPULSIVITY` (tap temprano), `ABANDONMENT` (count, descriptivo, NO penaliza score de tolerancia — es senal de seguridad), `RT_CV` de la ventana de respuesta.

### Senales de fatiga o mala calidad
`REPEATED_EXIT_ATTEMPTS`, `SUSTAINED_SLOWING`, abandono creciente -> ARMAN check-in (no infieren frustracion). Catch-trials respondidos = tap-spam (invalida). `n<minN` -> indices `unavailable`.

### Scoring sugerido
`toleranceScore = matureHits / (matureHits + earlyTaps)` (abandonos excluidos del denominador — un abandono seguro NO es fracaso). `persistenceTime = median(waitTime | HIT)` normalizado vs baseline. `null` si denominador < minN. `preliminary=true`.

### Antitrampa / calidad de datos
Catch-trials (nunca maduran) detectan tap-spam: responder a uno = penalizado/invalida bloque. Tiempo de maduracion con varianza impide aprender un timer fijo. Hold-variant requiere presion continua (anti auto-clicker).

### Variantes
1. **Barra madurando** (base). 2. **Hold sostenido** (mantener presion hasta la senal, soltar en ventana). 3. **Espera con eleccion** (esperar para mayor recompensa-puntos vs tap ya para menos — coste/beneficio, sin gambling lexicon). Las tres usan timebase de alta resolucion.

### UX
Barra/objeto central que se llena; SIN countdown numerico (evita reducir a lectura de timer). Estados maduro/temprano/abandono por color+forma. Exit grande siempre visible. Microanimacion: llenado suave (funcional, es el estimulo). Duracion 3–5 min. Mensajes sin culpa: abandonar es valido.

### Implementacion KMP
**Requiere renderer nuevo** (timed single-tap con barra de maduracion + catch). Modela sobre `EmotionStimulusPayload.AffectiveGoNoGo`-like ampliado o un nuevo payload `ToleranceHold(maturationMs, windowMs, isCatch)`. Generator deterministico (tiempos seeded). Clasificacion: nueva rama `classifyTolerance` (HIT/FALSE_ALARM/ABANDONMENT) en `EmotionSessionAssembler`. Metricas nuevas en `DefaultEmotionMetricsEngine` (`SAFE_PERSISTENCE_INDEX`, `FRUSTRATION_TOLERANCE_INDEX`, hoy catalogadas no producidas). Adaptacion: `DefaultEmotionAdaptiveEngine` zona TOLERANCE. Eventos: `trial.mature_hit`, `trial.early_tap`, `trial.abandon`, `catch.spam`, `proxy.armed`. Persistencia summary.

### Riesgos
Riesgo alto de cruzar de "espera funcional" a induccion de frustracion -> por eso techo leve y zona TOLERANCE con safety estricta. Aburrimiento con tiempos largos -> limitar maduracion <= 9s y pocos trials. `PLACEBO_RISK`: facil de sobre-vender.

### Revision critica
El Revisor ataca: (a) esto roza la induccion emocional — debe probarse via scanner + diseno que la UNICA carga es demora temporal, sin contenido afectivo escalado; (b) el abandono NUNCA debe penalizar el score ni mostrar culpa (codificar `ABANDONMENT` como senal de seguridad, no de fracaso); (c) catch-trials obligatorios o el tap-spam infla todo; (d) sin baseline de tiempo de espera, `FRUSTRATION_TOLERANCE_INDEX` no es interpretable -> NOT computable hasta baseline. Exige las 4 correcciones; (a) y (b) son fronteras de seguridad no negociables.

### Veredicto
Aprobado con cambios — concepto valioso pero el de mayor riesgo de seguridad: solo procede con zona TOLERANCE estricta, abandono no-penalizado, catch-trials y baseline previo; copy ultra-conservador por `PLACEBO_RISK`.

---

## EM-E · Calibrador Confianza/Acierto (Metacognitive Calibration)
### Modulo
Principal: Emociones (`CALIBRATION_ACCURACY`, capacidad `CALIBRATION_ACCURACY`). Secundarios: Profile, todos los modulos de eleccion (capa de confianza reusable).

### Objetivo cognitivo
Entrenar el **alineamiento confianza-acierto** (metacognicion): tras decidir, reportar confianza; bien calibrado = alta confianza en aciertos, baja en errores. Mide `CALIBRATION_ERROR` (hoy catalogada no producida).

### Hipotesis de transferencia
SI plausible: near transfer a calibracion en el mismo paradigma de decision. NO afirmar: mejor toma de decisiones vital, autoconocimiento, ni reduccion de sesgos clinicos. `TransferLabel.PLAUSIBLE`.

### Loop principal
1. Estimulo: decision perceptual 2-alt (LEFT/RIGHT, reusa EM-B). 2. Decision: elegir lado + DESPUES reportar confianza (escala 2–3 niveles: bajo/medio/alto). 3. Feedback: correcto/incorrecto + (diferido por bloque) curva de calibracion. 4. Adaptacion: ajusta dificultad para mantener accuracy en banda (~0.75) que maximiza varianza de confianza. 5. Siguiente trial.

### Reglas
Confianza se reporta SOLO tras la respuesta (no antes, para no sesgar). 3 niveles de confianza fijos. La dificultad se mantiene en banda intermedia a proposito (accuracy ~0.7–0.8) para que haya aciertos Y errores que calibrar. Feedback de calibracion es DIFERIDO (por bloque), no por trial (evita gaming).

### Inputs
Tap LEFT/RIGHT (decision) + seleccion de 3 botones de confianza (bajo/medio/alto). Dos taps por trial.

### Variables de dificultad (rangos)
1. `discriminability` (mantenida en banda, 0.7–0.55). 2. `EmotionAxis.TEMPORAL_PRESSURE` (ventana decision, 1500–900 ms). 3. `EmotionAxis.AFFECTIVE_INTERFERENCE` (distractor, 0.0–0.4). 4. Granularidad de confianza (2 o 3 niveles). 5. `EmotionAxis.TIME_ON_TASK` (24–40 trials; calibracion necesita n alto). 6. Ratio de trials ambiguos (para poblar confianza media, 0.2–0.4).

### Algoritmo adaptativo
Zona `GENERAL` pero objetivo INVERSO: mantener accuracy en banda media (no maximizar). `decide` ajusta `discriminability` para que accuracy ~0.75; NO sube indefinidamente. Plateau de `CALIBRATION_ERROR` -> mas trials ambiguos. Baseline ancla discriminability.

### Metricas primarias
`CALIBRATION_ERROR` (|confianza media reportada - accuracy real| por nivel, agregado tipo ECE — Expected Calibration Error), `AFFECTIVE_DECISION_QUALITY` (si se anade coste/beneficio).

### Metricas secundarias
`ACCURACY`, `RT_MEDIAN`, resolution (separacion de accuracy entre niveles de confianza), over/under-confidence bias (descriptivo, no clinico).

### Senales de fatiga o mala calidad
Confianza colapsada a un solo nivel (siempre "alto") = no calibracion -> `CALIBRATION_ERROR` no interpretable, arma check-in. RT de confianza ~0 = reporte sin reflexion. `n<minN` por nivel -> `unavailable`.

### Scoring sugerido
`calibrationScore = 1 - ECE`, `ECE = sum_k (n_k/N) * |conf_k - acc_k|` sobre niveles k. `resolution = var(acc_k)`. Reporta ambos; `calibrationScore null` si algun nivel con `n_k<minN`. `preliminary=true`.

### Antitrampa / calidad de datos
Confianza POST-respuesta evita ajustar la decision a la confianza. Feedback de calibracion diferido (no por trial) evita aprender a reportar la confianza "correcta" por ensayo-error. Colapso de confianza detectado por entropia. Dos taps obligan a no spamear un solo boton.

### Variantes
1. **Calibracion perceptual** (base, 2-alt + confianza). 2. **Calibracion con apuesta de puntos** (asignar puntos segun confianza — scoring proper-score tipo Brier, sin lexicon de gambling). 3. **Calibracion 4-cat** (sobre EM-C: clasificar + confianza). V1/V2 reusan 2-alt + capa confianza; V3 sobre 4-cat.

### UX
Tras el tap de decision, aparece fila de 3 botones de confianza (bajo/medio/alto, iconos distintos). Feedback diferido: curva de calibracion honesta al final del bloque (diagonal = perfecto). Sin claim. Duracion 4–5 min. Texto escalable.

### Implementacion KMP
Reusa **EM2RecuperacionGenerator** + **renderer 2-alt** (`EmotionTwoAltField`) **mas una capa de confianza nueva** (3 botones). El `RecordedEmotionTrial` necesita un campo `reportedConfidence: Int?`. Metrica `CALIBRATION_ERROR` nueva en `DefaultEmotionMetricsEngine`. `SubjectiveState`/`SelfReport` ya existe para confianza subjetiva — reusar su patron. Clasificacion via `choiceMetrics` + agregacion ECE en el assembler. **Requiere renderer aumentado** (decision + confidence overlay), no totalmente nuevo. Eventos: `trial.choice`, `trial.confidence`, `calibration.curve`. Persistencia summary.

### Riesgos
Necesita n alto para ECE estable -> bloques mas largos (riesgo fatiga). Dos taps por trial alarga el loop (rompe "loop corto" si no se optimiza la UI de confianza). Sesgo de orden si confianza siempre en mismo lado.

### Revision critica
El Revisor exige: (a) ECE con binning honesto y gated por `n_k` minimo por nivel — un nivel con 2 trials no entra; (b) la capa de confianza no debe romper el loop corto: pre-render de botones, no pantalla nueva; (c) feedback diferido obligatorio (por trial = gaming); (d) prohibir interpretar over-confidence como rasgo — es descriptivo de la tarea. (a) es bloqueante: ECE con celdas vacias es ruido.

### Veredicto
Aprobado con cambios — metrica metacognitiva valiosa y reusable, pero exige gating estricto de ECE por nivel y una UI de confianza que no rompa el loop corto.

---

## EM-F · Etiquetado Rapido de Registro (Speeded Affective Granularity)
### Modulo
Principal: Emociones (`GRANULARITY`). Secundarios: Lenguaje, Velocidad (presion temporal). 

### Objetivo cognitivo
Entrenar la **granularidad bajo presion**: discriminar rapido entre registros afectivos neutros (p.ej. agradable/desagradable/neutro/mixto) de palabras/frases cotidianas, distinto de EM-C (4 registros logicos): aqui el eje es valencia/activacion en banco neutro, con presion temporal y midiendo velocidad de etiquetado sin perder exactitud.

### Hipotesis de transferencia
SI plausible: near transfer a velocidad de categorizacion afectiva del banco. NO afirmar: inteligencia emocional, empatia, ni reconocimiento de emociones ajenas. `TransferLabel.NOT_DEMONSTRATED`.

### Loop principal
1. Estimulo: palabra/frase neutra del banco. 2. Decision: clasificar valencia en 1 de 4 (CATEGORY_A..D = p.ej. agradable/desagradable/neutro/mixto). 3. Feedback: inmediato. 4. Adaptacion: acorta ventana (presion) y sube ambiguedad. 5. Siguiente trial.

### Reglas
4 categorias fijas (mapeo quadrant estable). La valencia "verdad" del banco esta pre-etiquetada con acuerdo inter-rater. Ventana corta fuerza etiquetado rapido. Correcto = categoria == verdad. Holdout disjunto.

### Inputs
Seleccion de 4 botones, tap unico rapido.

### Variables de dificultad (rangos)
1. `EmotionAxis.TEMPORAL_PRESSURE` (ventana, 3500–1200 ms). 2. `EmotionAxis.CATEGORY_AMBIGUITY` (items mixtos, 0.1–0.5). 3. Longitud (1–6 palabras). 4. Densidad de cuasi-vecinos de valencia (0.0–0.4). 5. `EmotionAxis.TIME_ON_TASK` (24–40 trials). 6. ISI jitter (300–700 ms).

### Algoritmo adaptativo
Zona `GENERAL` con IES como gobernante (velocidad-exactitud). `decide` baja ventana solo con mastery; step-down si IES empeora o accuracy < banda. Plateau -> ambiguedad. Baseline ancla ventana inicial.

### Metricas primarias
`GRANULARITY_INDEX` (categorias distintas correctas / 4), `CATEGORY_ACCURACY`, IES (`RT_MEDIAN_hits / accuracy`).

### Metricas secundarias
`RT_MEDIAN` (CORRECT, post-MAD), `RT_CV`, `ANTICIPATIONS`, matriz de confusion de valencia (descriptiva).

### Senales de fatiga o mala calidad
IES creciente sostenido = fatiga/velocidad-sin-control. Colapso a una categoria. RT-floor por lectura. `OverloadProxy.EXTREME_RT_VARIABILITY`. `n<minN` -> celdas `unavailable`.

### Scoring sugerido
`granularitySpeedScore = 0.5*adjAcc + 0.5*(1 - normIES)`, `normIES = clamp((IES - IES_floor)/(IES_ceil - IES_floor),0,1)`. `null` si no confident. `preliminary=true`.

### Antitrampa / calidad de datos
Anticipaciones (RT<350) excluidas de HIT. RT-floor por lectura segun longitud. Banco con verdad inter-rater (descarta items ambiguos sin acuerdo). Holdout disjunto. Mapeo quadrant rota entre sesiones (no posicion).

### Variantes
1. **Valencia 4-cat veloz** (base). 2. **Activacion 4-cat** (calma/tension/neutro/mixto en vez de valencia). 3. **Doble eje** (valencia + activacion en 2 taps secuenciales — anade carga). V1/V2 reusan 4-cat; V3 dos fases.

### UX
Grid 2x2 estable, estimulo central, ventana visible como aro decreciente (funcional). Feedback inmediato resaltando correcto. Duracion 3–4 min. Texto escalable.

### Implementacion KMP
Reusa **renderer 4-cat** (`EmotionFourCategoryField`) + patron de **EM3SeparadorGenerator**. `EmotionStimulusPayload.Classification(phrase, truth, isEval)` reusable (truth = valencia). Banco NUEVO en `EmotionContent` (valencia neutra, escaneado, inter-rater) — disjunto de `SEPARATOR_ITEMS`. Metricas: `classificationMetrics` + IES. Adaptacion: `DefaultEmotionAdaptiveEngine` (IES-driven). **No requiere renderer nuevo** (V1/V2). Eventos: `trial.classify`, `valence.confusion`, `holdout.item`. Persistencia summary.

### Riesgos
Solapa con EM-C salvo por eje (valencia vs registro logico) y presion temporal -> debe quedar claro que mide cosa distinta. Dependencia cultural de valencia -> banco localizado. Riesgo de "reconocer emociones" claim (prohibido).

### Revision critica
El Revisor ataca: (a) ?es realmente distinto de EM-C o es una variante trivial? Defensa: eje afectivo (valencia/activacion) + presion temporal + IES gobernante, no registro logico sin presion — aceptado SI el banco es disjunto y el eje es valencia, no registro; (b) "agradable/desagradable" roza claim afectivo -> banco debe ser neutro-cotidiano y escaneado, sin contenido intimo; (c) inter-rater obligatorio o la verdad de valencia es opinion; (d) RT-floor por lectura no constante. Exige (a)-(c); sin banco disjunto + inter-rater colapsaria a EM-C.

### Veredicto
Aprobado con cambios — distinto de EM-C solo si el eje es valencia/activacion con presion temporal e IES y el banco es disjunto con inter-rater; de lo contrario, fusionar con EM-C.

---

## EM-G · Estabilidad bajo Interferencia Afectiva (Affective Flanker/Stroop)
### Modulo
Principal: Emociones (`REGULATION_STABILITY`, `AFFECTIVE_INTERFERENCE_COST`). Secundarios: Enfoque (control de interferencia), Profile.

### Objetivo cognitivo
Entrenar el **control de interferencia afectiva**: responder a una dimension objetivo (p.ej. la direccion de una flecha / el color del marco) ignorando un distractor afectivo congruente/incongruente. Mide el coste de interferencia y la ESTABILIDAD (RT-CV) bajo carga afectiva — distinto del go/no-go (no es inhibir, es seleccionar bajo conflicto).

### Hipotesis de transferencia
SI plausible: near transfer al control de interferencia del paradigma. NO afirmar: regulacion emocional, estabilidad de animo, ni resistencia al estres. `TransferLabel.PLAUSIBLE`.

### Loop principal
1. Estimulo: objetivo direccional (LEFT/RIGHT) con un distractor afectivo congruente/incongruente (la tonalidad del marco/palabra apunta a un lado o al contrario). 2. Decision: responder a la dimension objetivo, ignorar el distractor. 3. Feedback: inmediato. 4. Adaptacion: sube ratio incongruente y presion. 5. Siguiente trial.

### Reglas
La respuesta depende SOLO de la dimension objetivo (honestidad: el distractor NUNCA es la respuesta). Trials congruentes (distractor coincide) vs incongruentes (conflicto). El coste = RT/accuracy incongruente - congruente, solo con HIT-con-RT en ambos lados.

### Inputs
Tap LEFT/RIGHT en mitades tactiles.

### Variables de dificultad (rangos)
1. `EmotionAxis.AFFECTIVE_INTERFERENCE` (ratio incongruente, 0.2–0.6). 2. Saliencia del distractor (0.3–1.0). 3. `EmotionAxis.TEMPORAL_PRESSURE` (ventana, 1300–700 ms). 4. `EmotionAxis.INHIBITORY_DEMAND` (proporcion de no-go intercalados, 0.0–0.2 — opcional). 5. ISI jitter (300–800 ms). 6. `EmotionAxis.TIME_ON_TASK` (28–44 trials).

### Algoritmo adaptativo
Zona `GENERAL`. `decide` sube `AFFECTIVE_INTERFERENCE` con mastery; step-down si accuracy < banda o `EXTREME_RT_VARIABILITY`. Plateau de `AFFECTIVE_INTERFERENCE_COST` -> sube saliencia. Gate de fatiga antes de step-up. Baseline ancla ratio inicial.

### Metricas primarias
`AFFECTIVE_INTERFERENCE_COST` (1.0 - cost, gated MIN_CELL=5 por celda congruente/incongruente), `REGULATION_STABILITY` (1 - normalized RT-CV en incongruentes).

### Metricas secundarias
`ACCURACY`, `RT_MEDIAN` (HIT, post-MAD), `RT_CV`, `COMMISSION_RATE` (si hay no-go), `OMISSIONS`.

### Senales de fatiga o mala calidad
RT-CV creciente en incongruentes = inestabilidad/fatiga. Celda congruente o incongruente con `n<MIN_CELL` -> coste `unavailable`. `SUSTAINED_SLOWING`. Deriva de accuracy.

### Scoring sugerido
`stabilityScore = w1*(1 - normCost) + w2*REGULATION_STABILITY + w3*accuracy`; `normCost = clamp(cost/COST_MAX,0,1)`; `cost = RT_incong - RT_cong` (solo si ambas celdas confident). `null` si alguna celda < MIN_CELL. `w1=0.4,w2=0.4,w3=0.2`. `preliminary=true`.

### Antitrampa / calidad de datos
Coste solo computa con HIT-con-RT en AMBOS lados (regla §2.1.4). Distractor verificado por test de no-correlacion con la respuesta (anti render deshonesto). Anticipaciones excluidas. Daltonico-safe (forma+posicion, no solo color).

### Variantes
1. **Flanker afectivo** (flechas con marco de tono, base). 2. **Stroop-afectivo neutro** (palabra cotidiana cuyo tono apunta a un lado, responder a otra dimension). 3. **Simon-afectivo** (posicion del estimulo conflictua con la respuesta). Las tres reusan 2-alt + medicion de congruencia (patron E5 de focus).

### UX
Mitades LEFT/RIGHT, estimulo central con distractor claramente secundario. Cue de regla persistente. Feedback inmediato. Daltonico-safe. Microanimacion: ninguna decorativa. Duracion 3–4 min.

### Implementacion KMP
Reusa **renderer 2-alt** (`EmotionTwoAltField`) + patron de congruencia del E5 (Stroop/Simon) de focus. `EmotionStimulusPayload.TwoAltPerceptual` extendido o nuevo `AffectiveConflict(target, distractorSide, congruent, salience)`. Clasificacion: `EmotionTrialClassifier` (directional) + agregacion de coste congruente/incongruente en `EmotionSessionAssembler`. Metricas: `DefaultEmotionMetricsEngine.choiceMetrics` + `AFFECTIVE_INTERFERENCE_COST` (ya parcialmente codificada) + `REGULATION_STABILITY` (catalogada no producida). Adaptacion: `DefaultEmotionAdaptiveEngine`. **Renderer reusable** (anadir capa de distractor afectivo). Eventos: `trial.choice`, `congruency.incongruent`, `cost.cell`. Persistencia summary.

### Riesgos
"Estabilidad regulatoria" roza claim emocional -> copy debe decir "estabilidad de respuesta bajo interferencia", no "regulacion emocional". Solapa con EM-B si no se enfatiza el conflicto (no el error).

### Revision critica
El Revisor exige: (a) coste `unavailable` si cualquier celda < MIN_CELL=5 — no rellenar con 0; (b) test de paridad que el distractor afectivo NO predice la respuesta correcta; (c) `REGULATION_STABILITY` debe llamarse "estabilidad de respuesta" en UI, jamas "regulacion emocional" (frontera de seguridad + scanner); (d) distinguir de EM-B: aqui se mide conflicto congruente/incongruente, no recuperacion post-error. (a) y (c) bloqueantes.

### Veredicto
Aprobado con cambios — paradigma de conflicto clasico y medible; exige gating MIN_CELL, prueba de distractor no-predictivo y copy que evite el claim "regulacion emocional".

---

## EM-H · Mantener-el-Plan bajo Distractor (Goal-Maintenance Sequence)
### Modulo
Principal: Emociones (`REGULATION_STABILITY` aplicada a persistencia de meta). Secundarios: Enfoque (memoria de regla activa), Profile.

### Objetivo condicional
Entrenar el **mantenimiento de una regla/meta activa a lo largo de una secuencia** pese a distractores afectivos leves e intrusiones, midiendo la estabilidad de aplicar la regla correcta trial-a-trial (no cambio de regla como E6: aqui la regla es FIJA y el reto es no derivar de ella bajo carga).

### Hipotesis de transferencia
SI plausible: near transfer a mantenimiento de regla en el paradigma. NO afirmar: perseverancia vital, disciplina, ni autorregulacion global. `TransferLabel.NOT_DEMONSTRATED`.

### Loop principal
1. Estimulo: secuencia de estimulos; una regla FIJA declarada al inicio del bloque (p.ej. "responde LEFT si el simbolo es circulo"). Distractores afectivos leves aparecen entre trials. 2. Decision: aplicar la regla a cada estimulo. 3. Feedback: inmediato; errores de "olvido de regla" (deriva) se distinguen de errores perceptuales. 4. Adaptacion: alarga la secuencia y sube distractores. 5. Siguiente trial.

### Reglas
La regla es FIJA todo el bloque (no switch). El reto es no olvidarla/derivar tras distractores. Correcto = aplicar la regla al estimulo. Se mide deriva de accuracy en funcion de la posicion-en-secuencia y de la proximidad a un distractor.

### Inputs
Tap LEFT/RIGHT (o 4-cat segun regla) en mitades tactiles. Sin teclado.

### Variables de dificultad (rangos)
1. Longitud de secuencia entre recordatorios de regla (`EmotionAxis.TIME_ON_TASK`, 8–24 trials sin recordatorio). 2. `EmotionAxis.AFFECTIVE_INTERFERENCE` (densidad de distractores, 0.1–0.5). 3. `EmotionAxis.TEMPORAL_PRESSURE` (ventana, 1400–800 ms). 4. Complejidad de la regla (1–2 condiciones). 5. Saliencia de distractor (0.3–1.0). 6. ISI jitter (300–800 ms).

### Algoritmo adaptativo
Zona `GENERAL`. `decide` alarga la secuencia (mas distancia al recordatorio) solo con mastery; step-down si la deriva de accuracy crece. Plateau -> sube distractores. Gate de fatiga. Baseline ancla longitud inicial.

### Metricas primarias
`REGULATION_STABILITY` (1 - pendiente de deriva de accuracy en secuencia), `ACCURACY` global, `AFFECTIVE_INTERFERENCE_COST` (accuracy cerca vs lejos de distractor).

### Metricas secundarias
`RT_MEDIAN`, `RT_CV`, deriva intra-secuencia (slope), `POST_ERROR_RECOVERY` (tras error de regla), `OMISSIONS`.

### Senales de fatiga o mala calidad
Deriva de accuracy creciente = fatiga o perdida de regla -> `OverloadProxy.SUSTAINED_SLOWING`/`ERROR_STREAK` arman check-in. Accuracy random (~chance) = regla no retenida -> bloque invalido. `n<minN` -> `unavailable`.

### Scoring sugerido
`maintenanceScore = accuracy * (1 - normDriftSlope)`, `driftSlope = regress(accuracy ~ positionInSequence)` (negativa = deriva). `normDriftSlope = clamp(-driftSlope/SLOPE_MAX,0,1)`. `null` si secuencia < minN o accuracy ~ chance. `preliminary=true`.

### Antitrampa / calidad de datos
La regla se declara una vez; recordatorios espaciados impiden re-leerla cada trial. Accuracy ~ chance invalida (no retuvo la regla -> no mide mantenimiento). Distractor no-predictivo (test de paridad). Anticipaciones excluidas.

### Variantes
1. **Regla simple 2-alt** (1 condicion, base). 2. **Regla compuesta** (2 condiciones AND/OR). 3. **Regla 4-cat** (mantener mapeo de 4 categorias bajo distractor). V1/V2 reusan 2-alt; V3 reusa 4-cat.

### UX
Cue de regla persistente pero discreto (esquina), recordatorio explicito solo en intervalos. Estimulo central, distractores claramente secundarios. Feedback inmediato. Duracion 4–5 min (secuencias largas). Texto escalable, daltonico-safe.

### Implementacion KMP
Reusa **renderer 2-alt** (`EmotionTwoAltField`) y/o **4-cat** segun regla. Nuevo payload `GoalMaintenance(rule, target, distractorPresent, positionInSequence)` o extension de `TwoAltPerceptual`/`Classification` con `positionInSequence`. Generator deterministico (secuencia + posiciones de distractor seeded). Clasificacion: `EmotionTrialClassifier` (directional/category) + agregacion de deriva por posicion en `EmotionSessionAssembler`. Metricas: `DefaultEmotionMetricsEngine` con `REGULATION_STABILITY` nueva (slope de deriva). Adaptacion: `DefaultEmotionAdaptiveEngine`. **Renderer reusable** (la regla es overlay de cue, patron E6 sin el switch). Eventos: `trial.apply_rule`, `rule.reminder`, `drift.measured`, `distractor.near`. Persistencia summary.

### Riesgos
Solapa con EM-G (interferencia) y E6 (task-switch). Diferenciador: regla FIJA + medicion de DERIVA por posicion, no congruencia por trial ni switch. Si la secuencia es corta, no hay deriva que medir.

### Revision critica
El Revisor ataca: (a) ?como se distingue un error de regla (deriva) de un error perceptual? -> el trial debe etiquetar si la respuesta dada corresponde a OTRA regla plausible vs ruido; sin esto la "deriva" es inferida, no medida; (b) accuracy ~ chance debe invalidar (no retuvo la regla) — un usuario que nunca entendio la regla no esta "derivando"; (c) secuencias minimas para slope estable (>= K posiciones); (d) `REGULATION_STABILITY` en UI = "estabilidad de aplicacion de regla", no emocional. (a) es el reto duro: medir deriva de meta requiere distinguir tipo de error.

### Veredicto
Aprobado con cambios — concepto fuerte de mantenimiento de meta, pero solo medible si el trial distingue error-de-regla de error-perceptual y se invalida el desempeno ~ chance; sin eso, la "deriva" no es interpretable.

---

## Resumen de cobertura y deuda tecnica
- Renderers **reusados** (sin nuevo): EM-A (go/no-go + fase stop), EM-B (2-alt), EM-C (4-cat), EM-F (4-cat), EM-G (2-alt+distractor), EM-H (2-alt/4-cat+cue).
- Renderers **nuevos/aumentados**: EM-D (timed-hold con barra+catch, NUEVO), EM-E (2-alt + capa de confianza, AUMENTADO).
- Ramas de assembler nuevas requeridas: `classifyStop` (EM-A), `classifyTolerance` (EM-D), agregacion de confianza/ECE (EM-E), deriva-por-posicion (EM-H).
- Metricas hoy catalogadas-no-producidas que estos ejercicios activarian honestamente (gated `confident`): `SAFE_PERSISTENCE_INDEX`/`FRUSTRATION_TOLERANCE_INDEX` (EM-D), `CALIBRATION_ERROR` (EM-E), `REGULATION_STABILITY` (EM-G/EM-H), `IMPULSIVITY` via SSRT (EM-A).
- Fronteras de seguridad: todos mantienen induccion leve-only (techo 3), proxies que arman check-in sin inferir estado, copy escaneado, `null` nunca `0`, `preliminary=true`, `TransferLabel` honesto (NOT_DEMONSTRATED/PLACEBO_RISK donde corresponde).