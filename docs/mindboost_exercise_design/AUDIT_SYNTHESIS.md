# Auditoria del catalogo MindBoost (66 ejercicios)

## Veredicto ejecutivo

**APROBADO CON CAMBIOS — bloqueante por ejercicio, no por catalogo.** Recuento: **3 approve · 49 revise · 14 block** (sobre 66). Ningun ejercicio queda como `approve` sin reservas en los modulos de alto riesgo (Decisiones, Emociones, Creatividad, Lenguaje RT-gated). Lo que esta listo es la **arquitectura de honestidad**: gating `preliminary=true`, regla `null nunca 0`, `TransferLabel` NOT_DEMONSTRATED/PLACEBO_RISK, scanner de terminologia whole-word y la disciplina de separar senal metacognitiva del score. Lo que vende humo y debe bloquearse hoy: todo lo **RT-gated sin timebase autoritativa** (E5/E7 Velocidad, E7 Lenguaje, E8 Creatividad, cadencia/RSVP), todo lo que afirma o roza **transferencia a la vida real** (debiasing, multitarea, fake news, conduccion, regulacion emocional), y todo lo que **fabrica psicometria con n insuficiente** (d', SSRT, belief-bias, Brier, RCI sobre formas no equiparadas). Lo que falta es estructural y repetido: **bancos validados/inter-rater, detectores anti-gaming implementados (no solo disenados), y una capa de copy que jamas reproduzca el lexema prohibido ni para negarlo.**

## Tabla de veredictos por ejercicio

| Modulo | Ejercicio | Veredicto | Niveles | Claims | #P0 | Riesgo critico #1 |
|---|---|---|---|---|---|---|
| Enfoque | E1 Vigia Probabilistico (SDT) | revise | clear | low | 3 | Tap ritmico gamea; detector solo es diseno, no impl |
| Enfoque | E2 Filtro de Distraccion (Flanker) | revise | clear | low | 3 | Efecto congruencia (~40ms) < jitter de frame |
| Enfoque | E3 Freno Adaptativo (Stop-Signal) | revise | clear | medium | 3 | Waiting strategy destruye SSRT; gate sin verificar |
| Enfoque | E4 Conmutador de Reglas (Switch) | revise | clear | low | 3 | Cue mal disenado mide lectura, no reconfiguracion |
| Enfoque | E5 Seguimiento Multiple (MOT) | revise | mixed | high | 3 | Frame-rate contamina tracking; sin guardia = BLOCK |
| Enfoque | E6 Cazador Visual (Busqueda) | revise | clear | low | 2 | Pendiente con <3 set sizes / R2 bajo = ruido |
| Enfoque | E7 Reenganche Atencional | revise | mixed | medium | 3 | Puede medir tedio, no reenganche (constructo sin validar) |
| Enfoque | E8 Doble Via Conflictiva (Dual-task) | revise | mixed | high | 3 | Sin gate accuracy secundaria, coste = abandono |
| Velocidad | 1. Discriminacion Cromatica | revise | clear | low | 4 | dE sin calibrar mide pantalla, no percepcion |
| Velocidad | 2. Sustitucion Simbolo-Digito | revise | clear | medium | 4 | Con leyenda visible mide busqueda, no codificacion |
| Velocidad | 3. Conmutacion Perceptiva | revise | clear | low | 3 | Switch-cost con ~38 HITs/celda = ruido adaptado |
| Velocidad | 4. Compuerta Logica Express (Go/No-Go) | revise | clear | medium | 4 | Adaptativo puede premiar velocidad sobre inhibicion |
| Velocidad | 5. Cadencia Anticipatoria | **block** | clear | medium | 4 | Motor RT lanza con rt<0; senal central es negativa |
| Velocidad | 6. Resolucion de Conflicto (Flanker Express) | revise | clear | low | 3 | Redundancia E3/E4: triple conteo de la misma varianza |
| Velocidad | 7. Rafaga Serial (RSVP) | **block** | clear | medium | 3 | SOA nominal != real; jank mide GPU, no percepcion |
| Velocidad | 8. Calibracion de Apuesta Temporal | revise | clear | medium | 4 | Elegir deadline lento premia la cobardia |
| Logica | E1 Cadena Deductiva Progresiva | revise | clear | low | 2 | 6 items autorados memorizables, sin PlateauDetector |
| Logica | E2 Detector de Contradiccion | revise | clear | low | 2 | d' no estimable por sesion pero listado como primario |
| Logica | E3 Necesario vs Suficiente | revise | clear | low | 1 | Distractor distinguible por longitud/posicion |
| Logica | E4 Caza-Contraejemplo | **block** | clear | low | 2 | Validez depende de generador inexistente |
| Logica | E5 Silogismo Categorico (belief-bias) | revise | clear | medium | 1 | belief-bias index no honesto por sesion |
| Logica | E6 Inferencia Bayesiana Cualitativa | **block** | clear | high | 2 | "Magnitud cualitativa" no falsable; mide al autor |
| Logica | E7 Transferencia Espejo | **block** | clear | medium | 3 | UN solo logicalStructureId: ejercicio no viable |
| Logica | E8 Ordenar la Derivacion | **block** | clear | low | 3 | Drag introduce ruido motor; Kendall vs permutacion fija |
| Logica | E9 Inferencia Suprimida | **block** | clear | medium | 2 | Supresion depende de conocimiento del mundo |
| Logica | E10 Puente de Analogia Estructural | **block** | clear | medium | 2 | Dependencia cultural/lexica; roza CI verbal |
| Lenguaje | E1 Evidence Lock Pro | revise | clear | low | 4 | Mide conocimiento previo sin flag HIGH_PRIOR |
| Lenguaje | E2 Semantic Surgery | revise | clear | low | 3 | SpanLocalization nueva sin test de scoring |
| Lenguaje | E3 Compression Ladder | revise | clear | low | 3 | Kendall con candidateCount=3 = senal ruidosa |
| Lenguaje | E4 Argument X-Ray | revise | clear | medium | 3 | Ambiguedad de rol: correcto no unico |
| Lenguaje | E5 Scope & Negation Drill | revise | clear | low | 3 | Triple solape con E1 y Logica |
| Lenguaje | E6 Source Triage | revise | clear | medium | 3 | Se lee como "detecta fake news" (far transfer) |
| Lenguaje | E7 Lexical Access Sprint | **block** | clear | low | 3 | Modulo sin timebase real: todo RT es ruido |
| Lenguaje | E8 Calibrated Comprehension | **block** | clear | medium | 3 | Brier con 6-10 items reportado como metacognicion |
| Memoria | 1. Span Adaptativo Dual | revise | clear | low | 3 | processingDeadline sin gate de timebase |
| Memoria | 2. N-Back Doble Modalidad | revise | clear | medium | 3 | d' inestable con pocos targets/lures |
| Memoria | 3. Updating de Ranura | **approve** | clear | low | 1 | (paradigma limpio; falta maquina de estados §4.2) |
| Memoria | 4. Grid Espacial con Binding | revise | clear | low | 3 | Recoding verbal convierte visoespacial en verbal |
| Memoria | 5. Pares Asociados A-B/A-C | revise | mixed | medium | 4 | Practice effect: progreso = memorizar items |
| Memoria | 6. Episodica con Recall Diferido | revise | mixed | medium | 4 | Probe +24h no completada pero claim emitida |
| Memoria | 7. Orden Serial Temporal | **approve** | clear | low | 3 | (sin disociar orden/item es otro span redundante) |
| Memoria | 8. Sonda de Transferencia UNTRAINED | revise | clear | high | 5 | Formas paralelas no equiparadas: RCI sobre basura |
| Decisiones | E1 Bayes Arena (posterior continuo) | revise | mixed | medium | 5 | Dial continuo mide destreza motora |
| Decisiones | E2 Triaje de Reversibilidad | revise | clear | medium | 4 | Memorizacion de marcadores de coste |
| Decisiones | E3 Forecast Calibrado | revise | mixed | medium | 4 | Banco de trivia mide cultura, no calibracion |
| Decisiones | E4 Check-in Cognitivo Go/No-Go | revise | clear | high | 4 | CRI gamificado pierde el gate honesto |
| Decisiones | E5 Escaner de Alternativas | revise | mixed | low | 4 | "Dominante objetiva" ambigua = feedback opinable |
| Decisiones | E6 Deteccion de Sesgo | revise | unclear | critical | 4 | Overclaim de debiasing (transferencia no replicada) |
| Decisiones | E7 Presupuesto de Decision | revise | mixed | low | 4 | Solver incorrecto: feedback y score mienten a la vez |
| Decisiones | E8 Resolucion bajo Ambiguedad | revise | mixed | medium | 5 | VOI optimo con bug = stopping error es ruido |
| Emociones | EM-A Freno con Senuelo Afectivo | revise | clear | medium | 3 | SSRT basura si la escalera no converge |
| Emociones | EM-B Reentrada tras Error | revise | clear | low | 2 | Anticipaciones abren ventanas post-error espurias |
| Emociones | EM-C Separador (Granularity Quad) | **block** | clear | high | 3 | Verdad del item ambigua sin inter-rater |
| Emociones | EM-D Espera Calibrada (Tolerance Hold) | **block** | clear | critical | 4 | Roza INDUCCION de frustracion (frontera de seguridad) |
| Emociones | EM-E Calibrador Confianza/Acierto | revise | clear | medium | 1 | ECE con celdas vacias = ruido con apariencia de precision |
| Emociones | EM-F Etiquetado Rapido de Registro | **block** | clear | critical | 2 | Verdad de valencia sin inter-rater = opinion del autor |
| Emociones | EM-G Estabilidad bajo Interferencia | revise | clear | high | 2 | REGULATION_STABILITY = claim de regulacion emocional |
| Emociones | EM-H Mantener-el-Plan bajo Distractor | **block** | clear | high | 3 | Deriva inferida, no medida (sin tipo de error) |
| Creatividad | E1 Supresion del Primer Impulso | revise | mixed | medium | 4 | noveltyProxy: usuario juega al banco (familiaridad) |
| Creatividad | E2 Rotacion Forzada de Categorias | revise | mixed | medium | 4 | switchCost ideacional != ejecutivo de RT |
| Creatividad | E3 Sintesis Bajo Restriccion | revise | mixed | medium | 4 | Verificacion por tag = keyword-stuffing gameable |
| Creatividad | E4 Analogias Remotas | **block** | unclear | high | 4 | Sin IA, "puente valido" = estructura, no calidad |
| Creatividad | E5 Elaboracion Incremental | revise | mixed | low | 3 | elaboration=palabras premia verbosidad |
| Creatividad | E6 Deteccion de lo Inverosimil | **approve** | clear | low | 3 | (banco de frecuencias = punto unico de fallo) |
| Creatividad | E7 Reencuadre del Problema | revise | mixed | medium | 4 | Deteccion de "marco cambiado" casi adivinanza |
| Creatividad | E8 Improvisacion Restringida | **block** | unclear | high | 5 | Mide velocidad de tecleo, no fluidez ideacional |

## Lo mas fuerte del catalogo

- **Arquitectura de honestidad psicometrica de serie:** `preliminary=true` global, `null nunca 0`, em-dash en CALIBRATING, y la separacion sistematica de la senal metacognitiva del score de rendimiento (E3 Forecast, EM-E, E8 Lenguaje). Es la columna vertebral que distingue este catalogo del entrenamiento cerebral fraudulento estandar.
- **Senal de teoria de deteccion correcta donde importa:** d'/criterio c en vez de hit-rate (E1 Enfoque, N-Back, Grid), con correccion de continuidad y descarte de criterio extremo. Pocos productos del mercado separan sensibilidad de sesgo.
- **Metricas comparativas con regla de ambos-lados:** switch-cost, congruency, dual-task-cost e interference solo se computan con HIT-RT valido en AMBAS condiciones, y devuelven null honesto si falta un lado. Esto bloquea la fabricacion mas comun de efectos.
- **Process-first real en Logica/Lenguaje:** ICR con Jaccard sobre premisas/reglas/justificacion (E1 Logica, Evidence Lock, Argument X-Ray) impide acertar por suerte y mide el camino, no el resultado binario.
- **Paradigmas genuinamente distintos y bien aislados:** Updating de Ranura (intrusion vs binding), Orden Serial por posicion, Reversibilidad (lock-violation como impulsividad conductual limpia), E6 Creatividad (rareza*plausibilidad). Estos `approve`/casi-approve no inflan cobertura con la misma varianza.
- **Autocritica institucionalizada:** varios ejercicios documentan su propio peligro (Dual-task admite que NO es dual-task estricto; Tolerance Hold declara PLACEBO_RISK; E4 Creatividad reconoce dependencia de IA). El catalogo se audita a si mismo antes de mentir.
- **Scanner de terminologia y TransferLabel como gates duros:** whole-word/accent/hyphen-insensitive + NOT_DEMONSTRATED/PLACEBO_RISK convierten la honestidad de transferencia en codigo, no en buena intencion.

## Riesgos criticos transversales

| Riesgo | Severidad | Probabilidad | Impacto | Fix |
|---|---|---|---|---|
| **RT-gating sin timebase autoritativa** (efectos del orden del jitter de frame; modulos enteros con contador monotono) | critical | alta | Toda metrica temporal cross-device es ruido presentado como precision | Gate `device_unreliable`/UNTRUSTED_TIMEBASE obligatorio; sin Choreographer/vsync no actualiza perfil. (E2/E3/E5 Enfoque, Vel 5/7, Lenguaje E7, Creatividad E8, EM-A) |
| **Anti-gaming disenado pero no implementado/testeado** (tap ritmico, waiting-strategy, accuracy secundaria, scanRatio sin lectura, keyword-stuffing) | critical | alta | El gate existe en el doc; el usuario lo rompe en runtime y el score miente | Implementar + testear cada detector con perfil sintetico adversario ANTES de `implemented`; sin el, BLOCK. (E1/E3/E8 Enfoque, MOT, E5/E7 Decisiones, E1/E3 Creatividad) |
| **Psicometria fabricada con n insuficiente** (d', SSRT, belief-bias, Brier, RCI, switch-cost por celda) | critical | alta | "Mejora fiable" calculada sobre estimadores cuyo IC cruza cero | min-n explicito por celda/categoria; null/calibrating honesto; agregacion inter-sesion antes de cualquier claim. (E2/E5 Logica, EM-E, E8 Lenguaje, Sonda UNTRAINED, Vel 3) |
| **Far transfer sugerido por dominio real** (multitarea, fake news, conduccion/deporte, mejores decisiones, regulacion emocional) | high | alta | Claim no soportado heredado por copy/marketing; riesgo regulatorio | Capa de copy que jamas nombre el dominio real ni el lexema prohibido, ni para negarlo; negaciones viven solo en el doc de diseno. (Enfoque E4/E5/E8, Source Triage, Decisiones E1/E6/E8, EM-G/H) |
| **Banco de items sin validacion/inter-rater como punto unico de fallo** (verdad del item = opinion del autor; memorizable) | high | alta | Mide cultura/memorizacion, no el constructo; el score es ruido autorreferente | Inter-rater >=80% o descarte; banco grande + rotacion/holdout como hard-gate; flag HIGH_PRIOR_KNOWLEDGE. (EM-C/F, Logica E4/E6/E7/E9/E10, Memoria 5/6, Decisiones E3/E6) |
| **Proxy presentado como medida absoluta en UI** (originalidad, flexibilidad, d', scores con decimales) | high | media | Falsa precision; el usuario lee un proxy de practica como capacidad medida | UI nunca etiqueta con el nombre del constructo absoluto; "proxy de practica" + em-dash sin banco/IC suficiente. (Creatividad 1-8, Logica E2, transversal) |
| **Redundancia inter-ejercicio: misma varianza contada 3+ veces** (control-a-velocidad; alcance/evidencia/forma) | high | media | Brain-profile sobre-pondera un solo factor disfrazado de varios | Validar ortogonalidad EMPIRICA (no conceptual) antes de tratarlas como sub-senales. (Enfoque E2/E3/E4 vs Vel 3/4/6; Lenguaje E5 vs E1 vs Logica) |

## Riesgos por categoria

**Cientificos.** Constructos que dependen de conocimiento del mundo (E9 Logica, belief-bias concreto, banco de trivia en E3 Decisiones) miden cultura, no cognicion. Paradigmas donde la dificultad es de presentacion pero el constructo afirmado es otro: "creatividad bajo presion" (E8 Creatividad) no es defendible — la presion mata divergencia. MOT/dual-task/switch ideacional miden proxies que el copy debe declarar, no constructos ejecutivos puros. Reenganche (E7 Enfoque) no esta validado contra recuperacion de RT_CV: podria medir tedio.

**Psicometricos.** El patron dominante: estimadores que necesitan n grande (d', SSRT, Brier/ECE, Kendall-tau, RCI, switch-cost por celda) reportados sobre sesiones cortas. La Sonda UNTRAINED es el caso mas peligroso: RCI declara "mejora fiable" sobre formas paralelas no equiparadas. Jaccard gameable sin penalty por seleccion irrelevante (E1 Logica, E4 Logica, Evidence Lock). Belief-bias y d' listados como primarios pero solo honestos agregados historicamente, sin capa de serie.

**Tecnicos.** Ausencia de timebase autoritativa en modulos enteros (Lenguaje, Creatividad) y motores reutilizados que rompen el ejercicio: Cadencia (motor RT lanza con rt<0 cuando la senal ES negativa), RSVP (SOA nominal != frame real), classifyStop/classifyTolerance inexistentes en el assembler (EM-A, EM-D). Renderers nuevos (drag E8 Logica, sentence-labeling E4 Lenguaje, SpanLocalization E2 Lenguaje) expuestos sin test renderer↔clave. Solvers (E7/E8 Decisiones) cuyo bug hace mentir feedback y score simultaneamente.

**UX.** transfer=0.0 inicial leido como regresion (E7 Logica). Doble objetivo confunde que se mide (E8 Lenguaje). CRI gamificable (E4 Decisiones). Drag/dial introducen destreza motora como confound (E8 Logica, E1/E7 Decisiones). Bloques largos para n estable inducen fatiga que contamina la propia metrica (EM-E, E7 Enfoque).

**Legales-eticos.** Tres frentes calientes: (1) **debiasing/racionalidad** (E6/E1/E8 Decisiones, E6 Logica) — literatura no replicada, claim critico; (2) **regulacion/induccion emocional** (EM-C/D/F/G/H) — vocabulario terapeutico, frontera anti-induccion en Tolerance Hold, nombres de metrica que sugieren regulacion clinica; (3) **capacidad global** (inteligencia logica, velocidad mental general, CI verbal, fake news, multitarea) nombrada aun en negacion. El sistemic pattern "negacion-que-introduce-el-lexema" es real y debe cortarse en la capa de copy.

## Red Team extremo

**Regulador (proteccion al consumidor / salud digital).** Abre la app, hace screenshot de un resultado de E4 Decisiones ("control bajo") o de cualquier metrica `REGULATION_STABILITY`, y lo presenta como dispositivo medico no declarado que infiere inhibicion/regulacion emocional. Cita el lexico de apuesta de Decisiones (E3 "fichas apostadas", "mercado de prediccion") como gamificacion de gambling junto a un publico que el modulo de Emociones marca como potencialmente vulnerable. **Peor vulnerabilidad:** inconsistencia inter-modulo — Emociones y Creatividad prohiben gambling lexicon, Decisiones lo usa libre; eso prueba que la prohibicion no es sistematica sino cosmetica.

**Cientifico esceptico (revisor de Psychological Science).** Pide los datos de equating de las formas paralelas de la Sonda UNTRAINED y demuestra que el RCI declara mejoras fiables sobre formas no equiparadas — ruido. Luego corre tap ritmico en E1 Enfoque y waiting-strategy en E3, y comprueba que los gates "anti-gaming" son comentarios de diseno sin implementacion. **Peor vulnerabilidad:** el catalogo afirma medir constructos (SSRT, d', belief-bias) con n por debajo del umbral de estabilidad de esos mismos estadisticos; la honestidad del `preliminary=true` no salva que la metrica primaria sea no-estimable.

**Competidor.** Publica un benchmark lado-a-lado mostrando que en gama media el efecto de congruencia (~40ms) y el SOA de RSVP (±1 frame) caen dentro del jitter del dispositivo, y que MindBoost no tiene timebase autoritativa en Lenguaje ni Creatividad. **Peor vulnerabilidad:** modulos enteros RT-gated corriendo sobre contador monotono — el competidor demuestra que el numero cambia al cambiar de telefono, no al cambiar la cognicion.

**Usuario avanzado.** Memoriza el banco autorado (E7 Logica tiene UNA estructura; E1 Logica 6 items), juega al banco de frecuencias de Creatividad tecleando rareza no-prohibida, revela todas las alternativas sin leer en E5 Decisiones, y elige siempre el deadline lento en Vel 8. Sube su "mejora" en redes. **Peor vulnerabilidad:** practice effect + bancos pequenos sin holdout duro = el "progreso" es memorizacion, exactamente lo que el charter prohibe, y es trivialmente explotable hoy.

## Correcciones P0/P1/P2

**P0 — bloqueante de release (sin esto, miente o danna):**
1. Implementar y testear con perfil sintetico adversario TODOS los detectores anti-gaming (tap ritmico, waiting-strategy, accuracy secundaria, scanRatio-sin-lectura, keyword-stuffing); sin el detector, el ejercicio se bloquea (E1/E3/E8 Enfoque, MOT, E5/E7 Decisiones, E1/E3 Creatividad).
2. Gate `device_unreliable`/UNTRUSTED_TIMEBASE obligatorio en todo RT-gating; integrar timebase de alta resolucion o no actualizar perfil (E2/E3/E5 Enfoque, Vel 5/7, Lenguaje E7, Creatividad E8, EM-A).
3. **BLOCK** los dos motores rotos antes de exponer: TimingPolicy con signo para Cadencia (Vel 5) y SOA efectivo por frameTimeNanos + gate de jank para RSVP (Vel 7); implementar classifyStop (EM-A) y classifyTolerance (EM-D).
4. Inter-rater >=80% por item o descarte como precondicion de release en todo banco cuya verdad sea de juicio (EM-C, EM-F, E4/E6/E9 Logica); equating estadistico de formas paralelas antes de cualquier RCI (Sonda UNTRAINED).
5. min-n explicito por celda/categoria con barra null honesta para d', SSRT, Brier/ECE, switch-cost, congruency (E2/E5 Logica, EM-E, E8 Lenguaje, Vel 3, Enfoque E2/E8).
6. Solver exacto testeado como fuente unica de verdad para feedback y score (E7/E8 Decisiones); banco generado + PlateauDetector/holdout duro donde hoy hay items autorados (E1/E7 Logica, Memoria 5/6, Creatividad E1).
7. Capa de copy + review-cognitive-claims que bloquee debiasing/racionalidad/mejores-decisiones (Decisiones E1/E6/E8, Logica E6), regulacion/induccion emocional y nombres de metrica clinicos (EM-C/D/F/G/H), y fake-news/multitarea/conduccion (Source Triage, Enfoque E4/E5/E8). El lexema prohibido no aparece en UI ni para negarlo.
8. Eliminar el lexico de apuesta del copy de Decisiones (E2/E3): "puntos de confianza"/"puntuacion por calibracion", nunca apuesta/fichas/mercado/payoff.
9. Abandono en EM-D codificado como senal de seguridad que NUNCA penaliza ni culpa; probar que la unica carga es demora temporal sin contenido afectivo escalado (frontera anti-induccion).

**P1 — antes de tratar la metrica como estable:**
1. Validar ortogonalidad EMPIRICA antes de contar sub-senales separadas (Enfoque E2/E3/E4 vs Vel 3/4/6; Lenguaje E5 vs E1 vs Logica).
2. Declarar y enforce los proxies como proxies en UI con em-dash sin banco/IC suficiente (Creatividad 1-8 originalidad; Logica E2 d'; flexibilityIndex).
3. Gate de fatiga = readiness-only, perfil estable no se actualiza con fatiga (E5/E7 Enfoque, EM-E, Memoria temporal).
4. Balanceo anti-bias (lado, longitud, posicion de pivote, authorityDecoy bidireccional) testeado (Enfoque E2/E4, Lenguaje E2/E6, E3 Logica).
5. Implementar el contrato de score §4.2 (state/nEffective/dataQuality/invalidatedBy/display) donde falta (Memoria 1-4/7, Creatividad 1-8).
6. Penalty explicito por seleccion irrelevante en todo Jaccard (E1/E4 Logica, Evidence Lock).
7. Renderers nuevos con test renderer↔clave y gate de `implemented` (E8 Logica drag, E4/E2 Lenguaje).

**P2 — endurecimiento y honestidad fina:**
1. Variantes que cierran agujeros conceptuales (sprint-recovery E8 Creatividad, payoff asimetrico E8 Decisiones, coste-asimetrico E2 Decisiones, atributos en conflicto E5 Decisiones).
2. Bancos localizados/mono-idioma sin claim translingueistico, re-escaneados por idioma (Lenguaje, EM-C/F, E10 Logica).
3. Mostrar banda de error del dispositivo en vez de ms puntuales como precision fina (Vel 5/7, RT en general).
4. Caps de carga WM donde contamina la senal diana (chainLength, statementCount, relationCount en Logica).

## Que NO cablear todavia / Que cablear primero

**NO cablear (block hoy):**
- Cadencia Anticipatoria (Vel 5) y RSVP (Vel 7) — motores rotos.
- Logica E4/E6/E7/E9/E10 — bancos/generadores inexistentes o dependencia cultural/CI verbal.
- Lenguaje E7/E8 — sin timebase real / Brier no estimable.
- EM-C/D/F/H — inter-rater ausente, frontera de induccion, deriva inferida.
- Creatividad E4/E8 — calidad no medible sin IA / mide tecleo.

**Cablear primero (lo defendible, con sus P0 menores cerrados):**
- Memoria 3 (Updating de Ranura) y 7 (Orden Serial) — los dos `approve`; cerrar §4.2 y disociacion order/item.
- Creatividad E6 (Deteccion de lo Inverosimil) — el mas honesto sin IA; declarar cobertura de banco.
- Enfoque E6 (Busqueda), Logica E3 (Necesario vs Suficiente), Lenguaje E1/E3/E5 — `revise` con P0 acotados y verificables.

## Veredicto final

El catalogo no vende humo en su arquitectura — vende humo en sus huecos: gates anti-gaming sin implementar, RT sin timebase, psicometria sobre n insuficiente y bancos sin validar, todo ello hoy explotable por un usuario avanzado en una tarde. La honestidad declarativa (`preliminary`, `null`, TransferLabel) es real y diferenciadora, pero protege contra la mala-fe del producto, no contra la fragilidad de la medida. Aprobado con cambios: cablear solo los 3 honestos primero, bloquear los 14 hasta cerrar P0, y cortar de raiz —en la capa de copy y en codigo testeado— la transferencia a la vida real y el lexema prohibido, que es el unico riesgo con dientes regulatorios.