> Auditoría adversarial del catálogo MindBoost — Red Team + Claims Auditor + Psicometrista, bajo 00_CHARTER.md.

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

---

# Anexo — auditoría detallada por módulo



## Enfoque

＃ Auditoría adversarial — Módulo Enfoque (8 ejercicios)

Panel: Red Team Hostil (J) · Claims Auditor con veto (K) · Psicometrista Senior (C) · Director Científico. Lente: dura, no complaciente. El documento de diseño ya incluye una "Revisión crítica" interna razonable por ejercicio; este panel la asume como punto de partida y la ataca por debajo (lo que la propia revisión NO cubre).

**Veredicto global del módulo: REVISE.** Ningún ejercicio es "approve limpio" porque (a) los gates de medición están descritos como diseño pero NO verificados en implementación (riesgo de "deuda de implementación" = falso progreso), (b) varios paradigmas dependen de un timebase de dispositivo y de frame-rate de `Canvas` sin un gate `device_unreliable` explícito y verificado, y (c) el documento es un catálogo de diseño: no hay copy/UX final auditada, así que el veto de claims solo puede dar luz verde condicional. Lo más sólido del módulo: la disciplina de "score null-si-no-confident", `preliminary=true`, y la honestidad de transferencia (todos rotulan far como hipótesis y prohíben TDAH/IQ). El mayor agujero transversal: **el catálogo describe los gates correctos pero no demuestra que estén implementados ni testeados**; "diseño correcto" ≠ "métrica honesta en runtime".

---

## E1 — Vigía Probabilístico (Sustained Attention / SDT d′, criterio c)

**Veredicto: REVISE.**

**Lo más fuerte:** uso correcto de Teoría de Detección de Señal (separa sensibilidad d′ de sesgo de criterio c), exige corrección de continuidad para d′ infinito, separa omission/commission, y ancla a baseline personal. Score `null` con n_targets<20.

**Tabla de riesgos:**
| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Mide cadencia motora, no vigilancia, si el jitter es insuficiente; "tap rítmico" se gamea trivialmente | científico | high | med | métrica nula encubierta | jitter ≥150ms OBLIGATORIO + verificar detector de tap rítmico en tests |
| d′/c fabricados con pocos targets raros (targetRate 0.08 × bloque 40 = ~3 targets) | psicométrico | high | high | falso progreso | n_targets≥20 EFECTIVO por bloque o `value=null`; bloquear combinaciones de dificultad que no alcancen min-n |
| `VIGILANCE_DECREMENT_SLOPE` confunde decremento real con fatiga del día | científico | high | med | claim de "resistencia" inválido | si fatiga detectada, slope solo es readiness de hoy; NO actualiza perfil estable |
| Timebase: RT/d′ sin reloj fiable | técnico | high | med | datos basura | gate `device_unreliable` con `MonotonicClock`; sin él, no actualiza perfil |
| Variante auditiva-tono depende de hardware de audio/volumen | técnico | med | med | ruido de medición | mantener auditiva como secundaria/opt-out; no mezclar con perfil visual |

**Debe ser null/preliminary:** d′, criterio c, `VIGILANCE_DECREMENT_SLOPE`, `SUSTAINED_FOCUS_INDEX` cuando n_targets<20 o `confident=false`; SIEMPRE `preliminary=true`; `decrementSlope` no actualiza perfil estable bajo fatiga.

**Red Team extremo:** un competidor demuestra en 5 min que tapeando a ritmo fijo (ignorando el estímulo) el "índice de foco sostenido" sube — porque el detector de cadencia no estaba implementado, solo diseñado; el científico añade que d′ calculado con 3 targets es estadísticamente indistinguible de azar y el "decremento de vigilancia" reportado es indistinguible de cansancio del usuario.

---

## E2 — Filtro de Distracción (Flanker dinámico / efecto de congruencia)

**Veredicto: REVISE.**

**Lo más fuerte:** `CONGRUENCY_EFFECT` solo con HIT-RT en ambas condiciones, `null` honesto si falta un lado, IES por condición para trade-off velocidad-error, y exige balanceo LEFT/RIGHT contra sesgo motor.

**Tabla de riesgos:**
| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Sin balanceo de lado, mide impulsividad lateral, no inhibición | psicométrico | high | med | métrica equivocada | balanceo LEFT/RIGHT de respuestas correctas OBLIGATORIO y testeado |
| Carga perceptual mide agudeza visual si contraste mal calibrado | científico | med | med | confunde percepción con control | calibrar/controlar `targetContrast`; documentar que no es test de visión |
| Congruency effect ruidoso con pocos incongruentes; n insuficiente → fabricar media | psicométrico | high | med | falso valor | `congruencyEffect=null` si <min-n incongruente con RT válido |
| Timebase: el efecto es una diferencia de RT de decenas de ms; jitter de dispositivo lo destruye | técnico | high | high | métrica de ruido | gate device_unreliable; el efecto de congruencia (~30-80ms) es del orden del jitter de frame |

**Debe ser null/preliminary:** `congruencyEffectMs`, `accIncong`, IES por condición si n incongruente con RT válido < umbral o `confident=false`; `preliminary=true`.

**Red Team extremo:** un psicofísico señala que el efecto de congruencia es de ~40 ms y el jitter de presentación/captura en un teléfono Android de gama media supera eso, así que el "control de interferencia" reportado es ruido de timing; un regulador no tiene de qué quejarse en claims (copy honesto) pero la métrica no sostendría revisión por pares.

---

## E3 — Freno Adaptativo (Stop-Signal / SSRT Verbruggen)

**Veredicto: REVISE.**

**Lo más fuerte:** el ejercicio psicométricamente más exigente y mejor blindado del catálogo: `trustworthy=false→null` sin excepción, gate anti-"waiting strategy" (comparar go-RT con/sin stop), p(respond|signal) en banda 0.25-0.75, señal stop multicanal (color+forma+posición) por accesibilidad, SSD registrada por trial.

**Tabla de riesgos:**
| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| "Waiting strategy" (ralentizar GO para esperar el stop) destruye el SSRT; el gate es diseño, no impl verificada | científico | critical | med | métrica destruida | implementar y testear el gate de inflación de go-RT; sin él, bloquear |
| SSRT es RT/timing puro: sin timebase fiable es basura | técnico | high | med | falso SSRT | `device_unreliable`→null obligatorio; SSRT depende de ms reales |
| n_stops insuficiente para integración Verbruggen | psicométrico | high | med | SSRT inestable | min n_stops o `null` |
| Claim "control inhibitorio" rozando autorregulación/impulsividad de vida real | legal_etico | med | med | claim medium | copy debe limitar a inhibición motora de laboratorio; PROHIBIR gambling/autocontrol/impulsos de vida |

**Debe ser null/preliminary:** `ssrtMs`, `pRespond` si `!trustworthy`, n_stops bajo, waiting-strategy detectada, o device_unreliable; SIEMPRE `preliminary=true`.

**Red Team extremo:** el científico demuestra que sin el gate de waiting-strategy implementado, un usuario que simplemente responde lento al GO obtiene un SSRT artificialmente "bueno", invalidando toda la métrica; un regulador apunta a cualquier copy que sugiera "control de impulsos" como insinuación de beneficio conductual no probado.

---

## E4 — Conmutador de Reglas (Task-switching / SWITCH_COST)

**Veredicto: REVISE.**

**Lo más fuerte:** el usuario DERIVA la respuesta (no se muestra), `SWITCH_COST` solo con HIT-RT en ambas condiciones, separa error de perseveración de error perceptual (registra regla previa), prohíbe explícitamente el claim "multitasking".

**Tabla de riesgos:**
| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Mide lectura/latencia del cue, no reconfiguración de set, si el cue está mal diseñado | científico | high | med | métrica equivocada | cue dedicado, inequívoco, distinto del estímulo; testear |
| Perseveración confundida con error perceptual sin registrar regla previa | psicométrico | high | med | señal contaminada | registrar regla previa por trial; aislar perseveración |
| Switch cost `null` si falta una condición — diseño correcto pero no verificado | psicométrico | med | med | falso valor | min-n por condición + null honesto testeado |
| Riesgo de copy "flexibilidad cognitiva general"/"adaptabilidad" | legal_etico | med | low | claim medium | limitar a alternancia de reglas de la tarea; prohibir "adaptabilidad general" |

**Debe ser null/preliminary:** `switchCostMs`, `accSwitch`, `perseverationRate` si falta RT en una condición o `confident=false`; `preliminary=true`.

**Red Team extremo:** un revisor científico muestra que con CSI largo el "coste de cambio" desaparece porque el usuario pre-configura la regla antes del estímulo, así que la métrica mide diseño del cue, no flexibilidad; un competidor ataca cualquier deriva de copy hacia "mejor multitarea" (mito ya admitido en el propio doc).

---

## E5 — Seguimiento Múltiple (MOT)

**Veredicto: REVISE (cerca de BLOCK por dependencia de frame-rate).**

**Lo más fuerte:** honestidad sobre la limitación del runtime (una sonda por trial, no tracking continuo real), sonda balanceada target/no-target, staircase a umbral ~79%, y prohibición explícita de claims de conducción/deporte.

**Tabla de riesgos:**
| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Frame-rate del `Canvas` en dispositivos lentos contamina el movimiento → "tracking capacity" falso | técnico | critical | med | métrica inválida | guardia de frame-rate OBLIGATORIA; drops→CONTAMINATED/null; sin ella, BLOCK |
| Una sonda por trial → umbral converge lentísimo; n insuficiente fabrica capacidad | psicométrico | high | high | capacidad inventada | min-n por nivel explícito o `trackingCapacity=null` |
| Sesgo de respuesta sí/no infla accuracy | psicométrico | high | med | accuracy inflada | sonda balanceada + detección de sesgo de respuesta, testeada |
| Tentación de claim "atención para conducir/deporte" (el doc lo menciona como interés) | legal_etico | high | med | claim medium+ | PROHIBIR en copy toda transferencia a conducción/deporte; solo proxy de tarea |
| Carga alta → fatiga; resultados fatigados contaminan perfil | medicion | med | high | perfil sesgado | bloques cortos + fatiga no actualiza perfil estable |

**Debe ser null/preliminary:** `trackingCapacity`, `probeAccuracy` si n por nivel insuficiente, accuracy≈azar, o frame-rate no verificado; `preliminary=true`; CONTAMINATED bajo drops de frame.

**Red Team extremo:** un ingeniero demuestra que en un teléfono que cae a 30fps los objetos "saltan" y el usuario falla por presentación, no por capacidad — la "capacidad de tracking" reportada es función del hardware; un regulador/ científico destruye cualquier insinuación de que esto mejora la conducción (el propio doc admite que el interés deportivo/conducción "NO es garantizable").

---

## E6 — Cazador Visual (Búsqueda visual / SET_SIZE_SLOPE)

**Veredicto: REVISE.**

**Lo más fuerte:** exige ≥3 set sizes con min-n y R² mínimo para la regresión (o pendiente `null`), separa pendiente presente/ausente (la ausente ~2× en serial), gate anti-"ausente sin buscar".

**Tabla de riesgos:**
| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Pendiente de regresión con <3 set sizes o R² bajo = ruido presentado como "eficiencia" | psicométrico | high | high | falsa pendiente | ≥3 set sizes con min-n + R² umbral o `setSizeSlope=null` |
| "Ausente sin buscar" (responder ausente rápido) aplana la pendiente artificialmente | científico | high | med | métrica gameada | detección RT_ausente << esperado por set size; testear |
| Items pequeños → mide agudeza visual, no eficiencia de búsqueda | científico | med | med | confunde visión con atención | itemSize por encima de umbral perceptual |
| Sesiones largas (varios set sizes × n) → fatiga | medicion | med | med | perfil fatigado | fatiga no actualiza perfil; bloques manejables |

**Debe ser null/preliminary:** `setSizeSlopePresentMs`, `setSizeSlopeAbsentMs` si <3 set sizes con RT válido, R² bajo, o `confident=false`; `preliminary=true`.

**Red Team extremo:** un psicofísico muestra que con 2 set sizes y ruido normal la pendiente puede salir negativa o R² ~0.1, y aun así el sistema reportaría "eficiencia de búsqueda" — pura fabricación; un competidor demuestra el gaming de "siempre ausente rápido" si el gate no está implementado.

---

## E7 — Reenganche Atencional (Sostenida con recuperación post-lapso)

**Veredicto: REVISE (el más riesgoso del módulo en diseño de tarea).**

**Lo más fuerte:** reconoce abiertamente el riesgo de "medir tolerancia al aburrimiento en vez de reenganche" y exige que la monotonía sea una `DifficultyAxis` MEDIDA, no aburrimiento gratuito; probe metacognitivo de baja frecuencia para no contaminar.

**Tabla de riesgos:**
| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Induce mind-wandering deliberado → roza "dificultad artificial prohibida"; puede medir tedio, no reenganche | científico | high | med | construct inválido | monotonía como eje medido + validar que `reengagementLatency` correlaciona con recuperación de RT_CV, no con tedio |
| `reengagementLatency` con pocos lapsos = inestable; fabricar latencia | psicométrico | high | high | métrica inventada | `null` si <N lapsos; nunca 0 |
| No separar desenganche total (racha de omisiones) de lapso puntual mezcla dos constructos | científico | high | med | señal mezclada | separar y rotular; CONTAMINATED si desenganche sostenido |
| Probe metacognitivo ("¿estabas enganchado?") roza inferencia de estado interior | legal_etico | med | med | claim/privacidad | mantener como readiness, no como inferencia psicológica; no exponer como "estado mental" |
| Longitud 5-8 min + monotonía → fatiga severa | medicion | high | high | perfil fatigado | gate de fatiga REFORZADO; fatiga→readiness only, posible aborto |

**Debe ser null/preliminary:** `reengagementLatencyMs` si <N lapsos, `lapseRate`/`confidenceAlignment` si n_noGo o n_probes insuficiente, y todo el bloque bajo desenganche sostenido o fatiga; `preliminary=true`; bajo fatiga NO actualiza perfil estable.

**Red Team extremo:** un científico cognitivo destruye el constructo argumentando que sin demostrar que la latencia de reenganche correla con recuperación de RT_CV (y no con cuánto tolera el usuario el aburrimiento), el ejercicio mide tedio disfrazado de meta-atención; un regulador cuestiona el probe "¿estabas enganchado?" como inferencia de estado psicológico si el copy no lo enmarca como readiness puro.

---

## E8 — Doble Vía Conflictiva (Dual-task / DUAL_TASK_COST)

**Veredicto: REVISE.**

**Lo más fuerte:** el más honesto en su autocrítica — admite explícitamente que NO es dual-task psicofísico estricto (secundaria evaluada solo al cierre, runtime de una respuesta), que NO entrena "multitarea", gate obligatorio de accuracy secundaria (si no, el "coste bajo" premia abandonar la secundaria), `taskBalance` para detectar priorización, y copy que DEBE desmentir el mito de multitarea.

**Tabla de riesgos:**
| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Sin gate de accuracy secundaria, "coste bajo" = abandonar la secundaria; gate es diseño, no impl verificada | científico | critical | med | métrica destruida | implementar+testear gate de accuracy mínima en AMBAS tareas; sin él, BLOCK |
| Secundaria evaluada solo al final = aproximación débil; sobre-afirmar = claim falso | científico | high | med | validez de construct | documentar la limitación en copy; NO presentar como dual-task estricto |
| Tentación de claim "multitarea"/"productividad" (mito) | legal_etico | high | med | claim medium+ | copy DEBE desmentir activamente el mito; prohibir "multitarea"/"productividad" |
| Timebase + carga visual confunde con agudeza | técnico | med | med | ruido | device_unreliable; evitar sobrecarga visual perceptual |
| `dualTaskCost` con condición faltante | psicométrico | high | med | falso coste | `null` si falta condición single/dual válida o secundaria≈azar |

**Debe ser null/preliminary:** `dualTaskCostMs`, `accPrimaryDual`, `accSecondary`, `taskBalance` si falta condición válida, accuracy secundaria≈azar, o `confident=false`; `preliminary=true`.

**Red Team extremo:** un científico muestra que la "reducción del coste dual" es indistinguible de que el usuario aprendió a ignorar la secundaria (que solo se evalúa al final), salvo que el gate de accuracy secundaria esté implementado y testeado; un regulador/competidor ataca cualquier copy que insinúe "mejor multitarea" — mito que el propio documento admite que hay que desmentir, lo que convierte cualquier deriva en violación de fronteras.

---

## Correcciones transversales (todo el módulo)

**P0 (bloquean approve):**
1. Convertir cada "gate de diseño" (anti-cadencia E1, anti-waiting-strategy E3, frame-rate E5, anti-"ausente sin buscar" E6, accuracy-secundaria E8) en código IMPLEMENTADO con test de regresión que demuestre que el gaming produce `null`/CONTAMINATED, no un score. Deuda de implementación = falso progreso.
2. Gate `device_unreliable` verificado para TODA métrica de RT/SSRT/slope/congruency/switch-cost: sin `MonotonicClock` fiable → no actualiza perfil. (E2, E3 y E8 son del orden de decenas de ms, < jitter de frame en gama media.)
3. min-n efectivo por condición/nivel verificado antes de computar d′, congruency, switch-cost, set-size-slope, tracking-capacity, reengagement-latency, dual-cost; en su defecto `value=null`, display "—", nunca 0.
4. Auditar copy/UX/nombres finales con Claims Auditor antes de publicar: prohibir transferencia a conducción/deporte (E5), impulsos/autocontrol de vida (E3), multitarea/productividad (E8), adaptabilidad/flexibilidad general (E4), concentración general/TDAH (E1, E7). El catálogo está limpio; el copy de producto aún no existe y es donde se cuela el veto.

**P1:**
1. Gate de fatiga reforzado y verificado en los ejercicios largos (E1, E5, E7): bajo fatiga solo readiness de hoy, NO perfil estable.
2. Validación de constructo de E7 (reengagementLatency correla con recuperación de RT_CV, no con tedio) antes de afirmar que mide reenganche.
3. Balanceo LEFT/RIGHT por condición (E2, E4) y balanceo de sonda (E5) testeado contra sesgo motor/de respuesta.
4. Encajar el probe metacognitivo de E7 como readiness puro, nunca como inferencia de estado interior.

**P2:**
1. Variantes auditivas (E1) y cross-modal (E8) marcadas como secundarias/opt-out por dependencia de hardware de audio.
2. Documentar explícitamente, en cada summary, los 5 niveles de progreso y afirmar fuerte solo intra-tarea y vs baseline; far como hipótesis rotulada siempre.
3. Exponer `nEffective`, `dataQuality`, `invalidatedBy` en cada `*Summary` (varios DTOs solo llevan `n`+`confident`).

---


## Velocidad (03_2_Velocidad.md) — 8 ejercicios

## Auditoria — Modulo Velocidad (8 ejercicios)

Panel: Red Team Hostil (J) + Claims Auditor con veto (K) + Psicometrista Senior (C) + Director Cientifico (A). Lente: el spec esta bien escrito y ya incorpora muchas defensas (null!=0, preliminary, MAD solo en tendencia central, gate de comparativas, anclaje por dispositivo). Pero el spec describe DISENO; el riesgo dominante de todo el modulo es DEUDA DE IMPLEMENTACION presentada como "reuso directo", y un gate fisico §3/§15 ABIERTO que hace que TODO el modulo sea preliminary por construccion. Ningun ejercicio es `approve` mientras el timebase autoritativo no este validado fuera de simulacion: el veredicto techo realista de hoy es **revise** para casi todos, con **block** donde la semantica de medicion esta rota hasta nuevo codigo.

Nota transversal de claims (K): los nombres de UX son funcionales y limpios ("Tinte Critico", "Cifrado Veloz", "Compuerta", "Cadencia"). El copy interno respeta el lexico (usa "perfil cognitivo funcional", "baseline personal", "preliminary"). E4 menciona TDAH explicitamente como PROHIBIDO (correcto, es una nota de prohibicion, no un claim). NO encuentro lexico prohibido afirmado como claim en ningun ejercicio. El riesgo de claims real es de NIVEL DE PROGRESO: varias "hipotesis de transferencia near" se acercan a vender transferencia. Deben rotularse SIEMPRE como hipotesis nivel 3-4 NO afirmada, nunca como beneficio.

---

### 1. Discriminacion Cromatica Binaria (Tinte Critico) — VEREDICTO: revise

**Lo mas fuerte:** anclaje obligatorio al baseline del MISMO dispositivo (no cross-device), rotacion de eje contra aprendizaje de mapeo fijo, variante luminancia daltonismo-safe, y reconoce explicitamente que `deltaPerceptual` no es comparable cross-device. La revision critica propia ya exige las tres correcciones correctas.

**Que DEBE ser null/preliminary:** todo el score es `preliminary=true` (gate fisico abierto). `vce`, `rtMedian`, pendiente RT-vs-delta `null` si `n<MIN_N` o `confident=false`. `deltaPerceptual` NUNCA se reporta como unidad absoluta cross-device — solo como delta intra-sujeto.

**Red team extremo:** un cientifico del color te destruye: ΔE CIELAB sobre una pantalla movil sin calibrar ni perfil ICC no es ΔE perceptual real — varia por brillo de pantalla, modo oscuro, filtro de luz azul del usuario y temperatura ambiente. Afirmar "40→4 unidades ΔE" implica una precision colorimetrica que el hardware no soporta: es falsa precision. Un competidor diria que mides el brillo de la pantalla del usuario, no su velocidad perceptiva.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| ΔE nominal sin calibracion = falsa precision colorimetrica | medicion | high | alta | score refleja hardware de pantalla | Reportar delta solo como escalon adaptativo relativo intra-dispositivo, nunca como ΔE absoluto; no mostrar "unidades ΔE" en UX |
| Aprendizaje del mapeo fijo LEFT/RIGHT se confunde con velocidad | cientifico | high | alta | falso progreso intra-tarea | axisRotationDeg activo >=30% trials (ya exigido); testear con SyntheticUserRunner |
| Daltonismo invalida la tarea para ~8% varones | ux | med | media | dato CONTAMINATED no marcado | Variante luminancia obligatoria como default seleccionable + el rojo de error con forma (ya citado) |
| Adivinador rapido podria puntuar | psicometrico | med | media | gaming | Probar que anticipationRate+impIdx tumban perfil sintetico aleatorio (ya exigido, falta evidencia) |

**P0:** no exponer ΔE como magnitud absoluta; preliminary global; axisRotation>=30%; test sintetico de adivinador.
**P1:** variante luminancia como default daltonismo-safe; pendiente perceptiva reportada como descriptiva.

---

### 2. Sustitucion Simbolo-Digito Rapida (Cifrado Veloz) — VEREDICTO: revise

**Lo mas fuerte:** la propia revision critica reconoce el riesgo letal (con leyenda visible y keySize bajo, mide BUSQUEDA VISUAL + sacadas oculares, no velocidad) y eleva la pendiente RT/keySize a metrica primaria para separar search de speed. Leyenda regenerada por bloque mata memorizacion. Render honesto (no muestra la respuesta como affordance).

**Que DEBE ser null/preliminary:** preliminary global. La pendiente RT/keySize `null` si pocos niveles de keySize muestreados. Throughput `null` si n insuficiente. NUNCA afirmar que el DSST "predice inteligencia/salud" (la nota ya lo prohibe; mantenerla en copy).

**Red team extremo:** un psicometrista que conoce el DSST te dice que el clasico DSST mide velocidad PORQUE la clave se internaliza con la practica; al regenerarla cada bloque para "evitar memorizar", conviertes la tarea en busqueda pura y matas la validez de constructo del propio DSST que invocas como prestigio. O es DSST (y memorizan) o es search (y no es velocidad de codificacion): no puedes vender ambos.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Con leyenda visible degenera en busqueda visual, no velocidad | cientifico | high | alta | constructo equivocado | Pendiente RT/keySize como primaria explicita (ya); capar keySize a 6 runtime; legendVisibleMs finito |
| Invocar prestigio DSST mientras se rompe su mecanismo | legal_etico | med | media | claim de validez prestada enganoso | No citar "DSST predice X"; describir solo throughput perceptivo-asociativo entrenado |
| Duplica E7 (busqueda) bajo otro nombre | tecnico | med | media | redundancia de senal en profile | Declarar ortogonalidad medible vs busqueda; reportar componentes separados |

**P0:** keySize capado en runtime, legendVisibleMs, pendiente RT/keySize primaria, preliminary.
**P1:** copy sin claim de validez DSST prestada; declarar ortogonalidad vs busqueda.

---

### 3. Conmutacion Perceptiva de Tarea (Multivia Veloz) — VEREDICTO: revise

**Lo mas fuerte:** SWITCH_COST solo valido con HIT-RT en AMBOS lados (switch y repeat); si falta un lado -> `null` y el motor NO usa un switch-cost no fiable (cumple §4.8 de gates de comparativas). Reconoce honestamente que `MetricCalculator` NO computa switch-cost hoy: es DEUDA REAL, no reuso. Separa congruencia de switch-cost.

**Que DEBE ser null/preliminary:** SWITCH_COST `null` siempre que cualquier celda (switch/repeat) no alcance MIN_N. La propia revision admite que 4 bloques x24 con switch 0.4 dan ~38 switch HITs "justo en el limite": eso es n insuficiente para un estimador de diferencia fiable -> debe ser `preliminary`/`null`, nunca `stable`. Efecto de congruencia reportado SEPARADO.

**Red team extremo:** un metodologo te dice que un switch-cost basado en ~38 HITs por celda tiene un error estandar tan grande que el intervalo de confianza cruza cero — reportar un numero ahi es ruido disfrazado de senal, y si lo usas para mover el adaptativo estas adaptando a ruido. El switch-cost fiable necesita cientos de trials por celda; tu sesion no los da.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Switch-cost con n marginal por celda = estimador ruidoso | psicometrico | high | alta | senal inestable, RCI imposible | MIN_N documentado por celda; null honesto bajo umbral; nunca stable en sesion unica |
| Switch-cost declarado "reuso" pero es codigo nuevo | tecnico | high | alta | deuda oculta, riesgo de no implementarse | Declarar computo SWITCH_COST como trabajo NUEVO en speed (ya reconocido) |
| Congruencia contamina switch-cost si se mezclan | medicion | med | media | confound | Reportar congruencia y switch-cost separados (ya exigido) |
| Solapa con E6 en "control a velocidad" | tecnico | med | media | redundancia | Declarar ortogonalidad: switch=reconfiguracion secuencial |

**P0:** MIN_N por celda + null honesto; switch-cost como deuda nueva; separar congruencia.
**P1:** ortogonalidad explicita vs E6/E4 en el profile.

---

### 4. Compuerta Logica Express (Go/No-Go Veloz Asimetrico) — VEREDICTO: revise

**Lo mas fuerte:** PROHIBIDO premiar velocidad sin penalizar comision — el score castiga `faRate` directamente, techo incluye `faRate>0.30`, y el gate de comision (LOWER por faRate) tiene PRIORIDAD sobre RAISE. Asimetria seguridad (bajar > subir). Aqui SI existe no-go real (a diferencia de E1/E6). Macro FAST_IMPULSIVE honesto.

**Que DEBE ser null/preliminary:** `faRate` `null` honesto si los trials no-go no alcanzan MIN_N (con noGoRate 0.10 y 30 trials/bloque hay pocos no-go por bloque). post-error slowing `null` si pocas comisiones. preliminary global.

**Red team extremo:** un regulador clinico te observa de cerca porque go/no-go es EL paradigma usado en investigacion de TDAH — aunque tu copy lo prohibe explicitamente, basta que un usuario screenshotee "control inhibitorio bajo" para que parezca un mini-diagnostico. El riesgo no es tu codigo, es la inferencia que el usuario hace. Debes blindar el copy de resultado para que nunca sugiera deficit de inhibicion como rasgo.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Adaptativo empuja RT a costa de inhibicion si gate mal calibrado | cientifico | high | media | genera impulsividad recompensada | Gate comision prioritario testeado con sintetico impulsivo (ya exigido) |
| Usuario infiere mini-diagnostico de inhibicion (paradigma TDAH-asociado) | legal_etico | high | media | claim implicito clinico | Copy de resultado NUNCA framea faRate como rasgo/deficit; solo "control de hoy vs tu baseline" |
| faRate poco confident con noGoRate bajo | psicometrico | med | alta | techo basado en estimador ruidoso | MIN_N de no-go antes de reportar/aplicar techo por faRate |
| Comision podria reflejar fatiga, no rasgo | medicion | med | media | perfil estable contaminado | Fatiga alta -> solo readiness, no perfil (ya en charter; aplicar) |

**P0:** MIN_N no-go; gate comision prioritario testeado; copy de resultado anti-inferencia clinica; faRate null honesto.
**P1:** separar comision-por-fatiga de comision-por-rasgo en el ultimo tercio.

---

### 5. Cadencia Anticipatoria (Tiempo de Coincidencia) — VEREDICTO: block (hasta TimingPolicy nueva + gate fisico)

**Lo mas fuerte:** la revision critica es brutal y CORRECTA: `TimebasePolicy.computeRtNanos` LANZA con rt<0, pero la senal central aqui es el error NEGATIVO (adelanto) — reusar el motor de RT como esta ROMPE el ejercicio. Exige una `TimingPolicy` con signo NUEVA, anclaje al baseline del dispositivo y CONTAMINATED en fallback desktop/iOS. Penaliza la impulsividad temporal por construccion.

**Que DEBE ser null/preliminary:** TODO `null`/CONTAMINATED fuera de Android API34+ autoritativo (la latencia tactil es un sesgo constante que invalida cross-device). En Android, preliminary hasta validar. `timingError` requiere semantica con signo que el codigo NO tiene hoy.

**Por que block y no revise:** la semantica de medicion NO COMPILA hoy (motor de RT prohibe negativos, la senal ES negativa). No es un fix de parametro: es un subsistema de timing con signo nuevo. Hasta que exista y se valide en hardware, el ejercicio no puede medir lo que dice medir.

**Red team extremo:** un ingeniero de hardware te dice que la latencia de touch-to-event en Android varia 20-80 ms entre dispositivos y entre estados termicos del MISMO dispositivo; tu "error temporal con signo en ms" en un budget de ±40 ms esta DENTRO del ruido de latencia tactil. Sin timebase de vsync real y anclaje estricto, mides el jitter del digitalizador, no la anticipacion del usuario.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Motor RT lanza con rt<0; la senal central es negativa | tecnico | critical | alta | ejercicio no funciona | TimingPolicy con signo NUEVA (no reuso); tests del signo |
| Latencia tactil (20-80ms) >= tolerancia minima (±40ms) | medicion | critical | alta | score = ruido de hardware | tol minima nunca por debajo del ruido de latencia; anclaje baseline; CONTAMINATED fuera de Android |
| Sin oclusion mide reaccion visual, no prediccion | cientifico | high | media | constructo equivocado | Oclusion obligatoria; probar que fuerza extrapolacion |
| Falsa precision "±40 ms" en UX | legal_etico | med | media | enganoso | No mostrar ms absolutos como precision fina sin banda de error del dispositivo |

**P0:** TimingPolicy con signo implementada+testeada; tol minima >= ruido de latencia; oclusion obligatoria; CONTAMINATED fuera de Android autoritativo.
**P1:** mostrar banda de error del dispositivo, no ms puntuales.

---

### 6. Resolucion de Conflicto Veloz (Flanker Express) — VEREDICTO: revise

**Lo mas fuerte:** CONGRUENCY_EFFECT solo con HIT-RT en AMBOS lados (cumple gate de comparativas); estimulos NO-linguisticos obligatorios (flechas/formas, NO palabra-color) para evitar dependencia idiomatica PROHIBIDA; exige declarar ortogonalidad medible vs E3 (switch) y E4 (go/no-go). Reconoce el computo de congruencia como DEUDA.

**Que DEBE ser null/preliminary:** efecto de congruencia `null` si cualquier celda (congruente/incongruente) < MIN_N. preliminary global. La variante Stroop-palabra debe quedar BLOQUEADA salvo i18n validada (dependencia cultural prohibida).

**Red team extremo:** un competidor publica que tienes TRES ejercicios (E3, E4, E6) midiendo "control a velocidad" con efectos diferenciales de RT entre condiciones — y que en la practica las tres correlacionan tan alto que tu "brain profile" esta contando la misma varianza tres veces, inflando la sensacion de cobertura. Si no demuestras ortogonalidad empirica (no solo conceptual), es padding de catalogo.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Redundancia con E3/E4 (las tres = control a velocidad) | cientifico | high | media | profile cuenta varianza repetida | Declarar Y eventualmente validar ortogonalidad empirica, no solo conceptual |
| Congruency-effect null en sesiones cortas tratado como senal | psicometrico | med | alta | falso numero | MIN_N por celda; null honesto |
| Variante Stroop-palabra = dependencia idiomatica prohibida | legal_etico | med | baja | viola regla cultural | Bloquear Stroop-palabra salvo i18n validada; default flechas |

**P0:** MIN_N por celda + null honesto; estimulos no-linguisticos por defecto; congruencia como deuda nueva.
**P1:** evidencia de ortogonalidad vs E3/E4 antes de tratarlas como sub-senales distintas.

---

### 7. Rafaga de Deteccion Serial (RSVP Veloz) — VEREDICTO: block (hasta SOA efectivo por frame + gate de jank)

**Lo mas fuerte:** la revision critica es excelente: `itemDurationMs` NOMINAL es una mentira si el dispositivo no renderiza a esa cadencia; exige onset REAL por `frameTimeNanos`, SOA efectivo (no nominal), gate de jank -> CONTAMINATED, piso de itemDuration en frames del dispositivo, y atribucion del tap al item VISIBLE en tapUptime (no al objetivo asumido). d' separa deteccion real de sesgo de respuesta.

**Que DEBE ser null/preliminary:** umbral `itemDurationMs` `null`/CONTAMINATED si la varianza de frame supera umbral (jank). hitRate/faRate/d' preliminary. Atribucion de tap dudosa -> trial invalido, no HIT asumido.

**Por que block:** igual que E5, la validez depende de un subsistema de medicion que el spec admite NO tener (SOA efectivo por frame, gate de jank). Con SOA nominal de 80 ms y frames de 16.6 ms, la granularidad es ±1 frame y el jank arruina el dato: mides el GPU, no la percepcion. Block por estado de medicion, no por concepto.

**Red team extremo:** un investigador de RSVP te dice que tu attentional-blink (dualTargetRate) y tu umbral al 79% asumen control de SOA a nivel de milisegundos que NINGUN runtime Compose sobre Android garantiza con jank y throttling termico; reportar un "umbral de exposicion" es prometer una precision temporal que el stack grafico no puede entregar.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| SOA nominal != real; jank arruina el dato | medicion | critical | alta | mide GPU, no percepcion | SOA efectivo por frameTimeNanos; gate de jank -> CONTAMINATED; piso itemDuration en frames |
| Tap unico no distingue a que item responde | tecnico | high | alta | HIT asumido falso | Atribuir tap al item visible en tapUptime; trial invalido si ambiguo |
| Umbral 79% como claim de precision temporal | legal_etico | med | media | falsa precision | Reportar umbral con banda + estado de calidad de frame |
| attentional-blink con n bajo | psicometrico | med | media | senal ruidosa | dualTargetRate solo como descriptivo preliminary |

**P0:** SOA efectivo + gate de jank + piso en frames; atribucion de tap al item visible; CONTAMINATED fuera de Android.
**P1:** umbral con banda; AB solo descriptivo.

---

### 8. Calibracion de Apuesta Temporal (Velocidad-Confianza Metacognitiva) — VEREDICTO: revise

**Lo mas fuerte:** la revision critica detecta el agujero clave (elegir siempre el deadline mas lento cumple accuracy trivialmente y baja calibrationError -> "premiarias la cobardia") y exige penalizar under-commit, recentrar/estrechar la rejilla, SEPARAR la senal metacognitiva del score de velocidad, y multi-bloque antes de cualquier claim. La separacion del modificador metacognitivo del speed score evita contaminar dos constructos.

**Que DEBE ser null/preliminary:** `calibrationError` `null` hasta varios bloques (metacognicion temporal es ruidosa); `overCommitRate`/`underCommitRate` `null` con n bajo. preliminary global. La nota de copy contra "metacognicion general" debe mantenerse.

**Red team extremo:** un usuario experto esceptico pregunta: ¿que distingue tu "calibracion velocidad-exactitud" del ancho de deadline que el adaptativo ya conoce? Si el motor recentra la rejilla sobre el umbral real del usuario, la "eleccion" se vuelve cosmetica — mides si el usuario toca el chip que el motor ya marco como optimo, no metacognicion. Riesgo de medir obediencia a la sugerencia del sistema, no autoconocimiento.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Gaming eligiendo siempre deadline holgado (cobardia premiada) | psicometrico | high | media | calibrationError trivialmente bajo | Penalizar under-commit + recentrar/estrechar rejilla (ya exigido) |
| Recentrado convierte la eleccion en obediencia al sistema | cientifico | high | media | mide sugestion, no metacognicion | Documentar que el usuario fija ANTES de ver sugerencia; offset minimo entre opciones |
| Contaminar score de speed con senal metacognitiva ruidosa | medicion | med | media | inflacion/confusion de constructo | metacog reportado SEPARADO, no dentro de velocidad (ya exigido) |
| Claim de "metacognicion" en nivel de transferencia | legal_etico | med | baja | sobreinterpretacion | Copy: "calibracion del trade-off velocidad-exactitud entrenada", nunca "metacognicion general" |

**P0:** penalizar under-commit; recentrar/estrechar rejilla; metacog separado del speed; multi-bloque + null antes de claim.
**P1:** garantizar que la eleccion precede a la sugerencia del sistema.

---

## Sintesis del modulo

- **revise:** E1, E2, E3, E4, E6, E8 (disenos solidos, defensas correctas en spec, pero todos preliminary por el gate fisico abierto + deuda de metrica comparativa/calibracion que el propio spec admite como trabajo NUEVO; ninguno es approve hasta cerrar §15 y los MIN_N).
- **block:** E5 (Cadencia) y E7 (Rafaga RSVP): la SEMANTICA DE MEDICION no existe hoy (TimingPolicy con signo / SOA efectivo + gate de jank) y la senal cae dentro del ruido de hardware sin un subsistema de timing nuevo. Conceptos validos, implementacion-de-medicion ausente.
- **Transversal P0 de modulo:** (1) `CognitiveProfileScore.preliminary=true` global hasta cerrar gate fisico §3/§15. (2) Fuera de Android API34+ -> `device_unreliable`/CONTAMINATED, no actualiza perfil estable. (3) TODA metrica comparativa (switch-cost, congruencia, calibrationError) con MIN_N por celda y `null` honesto. (4) Probar anti-trampa con `SyntheticUserRunner` (adivinador rapido, impulsivo) en E1/E4. (5) Copy de resultados blindado contra inferencia clinica, especialmente E4 (go/no-go).
- **Riesgo de claims dominante:** no es lexico prohibido (no lo hay afirmado), es NIVEL DE PROGRESO: las "hipotesis de transferencia near" deben rotularse SIEMPRE como hipotesis nivel 3-4 NO afirmada (la regla de no mezclar niveles), nunca presentarse como beneficio al usuario.

---


## Logica (03_3_Logica.md) — 10 ejercicios

## Auditoria — Modulo Logica (panel hostil: Red Team + Claims Auditor con veto + Psicometrista + Director Cientifico)

### Veredicto ejecutivo del modulo
**REVISE / BLOCK selectivo.** El diseño es honestamente cientifico en su intencion (process-first ICR, near-transfer explicito, `null≠0`, anti-claims correcto) y reusa infra real del repo. PERO el modulo descansa sobre **tres deudas estructurales transversales que el propio doc admite y que ninguna seccion resuelve**, y que invalidan los claims de medicion de casi todos los ejercicios hasta cerrarlas:

1. **No hay generador procedural.** `DefaultLogicContentProvider` = pack autorado fijo de 6 items. Cada ejercicio "declara un contrato de generador seeded" que **no existe**. Con 6 items autorados, en 2-3 sesiones el usuario memoriza la superficie -> **false improvement masivo y no detectado** (Logica no tiene `PlateauDetector` — confirmado en E1 §revision). Esto es deuda de implementacion vendida como diseño.
2. **No hay capa de agregacion cross-sesion.** d-prime (E2), belief-bias index (E5), suppression discrimination (E9), surface-lure rate (E10) y la propia transfer consistency (E7) **NO son estimables por sesion** con 6-10 items. El doc lo reconoce repetidamente ("hueco real", "agregacion diferida") pero igual lista estas como "metricas primarias". Una metrica primaria que SIEMPRE sale `null` por sesion no es primaria: es una promesa.
3. **El contrato de score (§4.2 charter) NO esta cableado.** Ningun ejercicio describe `state`/`nEffective`/`dataQuality`/`invalidatedBy`/`interval`. El doc menciona `confident=false` como sustituto informal, pero NO hay maquina de estados `insufficient_data→calibrating→preliminary→stable`, ni gate de fatiga que ponga `update_long_term_profile=false`, ni gate de dispositivo. La cabecera dice "todo score arranca preliminary=true" — eso es un booleano, no la maquina de estados del charter.

Consecuencia: **ningun ejercicio puede ir a `ExerciseCatalog.implemented`** sin (a) generador seeded validado, (b) maquina de estados de score real, (c) gate de fatiga/plateau. Estos son P0 a nivel de MODULO, no por-ejercicio.

Riesgo de claims a nivel modulo: **bajo-medio.** El copy es disciplinado ("contra tu baseline", "near transfer", cero IQ/clinico). El riesgo real esta concentrado en E6 (Bayes — roza "racionalidad/debiasing", far transfer implicito) y E5/E9 (belief-bias / supresion — facil deslizar a "pensamiento critico mejorado en la vida real", que seria far transfer level-5 prohibido).

---

### E1 — Cadena Deductiva Progresiva
**Veredicto: REVISE.**
- **Lo mas fuerte:** ICR process-first real (premiseScore Jaccard + ruleScore + justification) hace que acertar la conclusion por suerte NO baste; reusa el paradigma actual (riesgo de implementacion bajo); minimumReasoningTimeMs e impulsividad sobre crudos (coherente con 2026-06-15).
- **Que debe ser null/preliminary:** accuracy de conclusion con n<umbral -> `null`; ICR `preliminary` hasta serie estable; `transferLevel` `calibrating` hasta banco isomorfo real.
- **Red Team:**
  - **Jaccard gameable marcando TODAS las premisas** — el doc lo admite y exige penalizar irrelevantes, pero el scoring §scoring NO incluye el termino de penalty por premisa irrelevante (solo premia las correctas). Sin ese termino, "marcar todo" sube premiseScore. **P0.**
  - **Lectura vs logica:** densidad de texto mide comprension lectora; `surfaceAbstraction` simbolico mitiga pero no hay control de comprension lectora baseline. **P1.**
  - **6 items autorados memorizables** -> false improvement (no PlateauDetector). **P0 modulo.**
  - **chainLength=6 / premiseCount=9** satura memoria de trabajo y contamina la señal deductiva (mide WM, no deduccion). **P1 (cap).**

### E2 — Detector de Contradiccion Latente
**Veredicto: REVISE.**
- **Lo mas fuerte:** consistentes balanceados (`consistentRatio`) bloquean "siempre inconsistente"; nucleo minimo exigido impide acierto-por-suerte del binario.
- **Que debe ser null/preliminary:** **d-prime SIEMPRE `null` por sesion** (n por categoria 3-5 << umbral); sesgo de respuesta `preliminary`; binario accuracy con n bajo `confident=false`.
- **Red Team:**
  - **d-prime listado como metrica primaria pero no estimable por sesion** y sin capa de agregacion -> es humo si se muestra. **P0** (rotular `null`/calibrando o moverla a serie historica inexistente).
  - **Nucleo minimo no-unico:** si hay multiples pares contradictorios, ¿contra cual se compara el Jaccard? El generador DEBE garantizar unicidad o aceptar cualquier nucleo valido. **P0 generador.**
  - **statementCount=7 mide WM, no consistencia** — el doc pide cap a 5. **P1.**
  - Sesgo "siempre inconsistente" se confunde con fatiga (falsas alarmas crecientes) — no hay gate de fatiga que lo separe. **P1.**

### E3 — Necesario vs Suficiente
**Veredicto: REVISE.**
- **Lo mas fuerte:** ataca una confusion canonica real (condicional≠bicondicional); render honesto (respuesta nunca affordance, 2026-06-18); micro-leccion dirigida por `distractorType`.
- **Que debe ser null/preliminary:** accuracy de clasificacion con 25% azar y n bajo -> `confident=false` obligatorio; sesgo bicondicional `preliminary`.
- **Red Team:**
  - **25% azar con 4 opciones + n bajo = accuracy ruidosa.** ICR process-first compensa parcial pero la accuracy binaria debe reportar n y `confident=false`. **P1.**
  - **Sesgo de superficie:** si el distractor "necesaria/suficiente" se distingue por longitud/posicion, se gamea sin razonar. Exige aleatorizar orden Y longitud de opciones. **P0.**
  - **Confusion persistente tras micro-leccion = plateau no detectado** (sin contador de re-exposicion). **P1.**
  - Es el ejercicio mas dependiente de comprension precisa del enunciado -> riesgo de medir lenguaje. **P1.**

### E4 — Caza-Contraejemplo
**Veredicto: BLOCK (hasta generador validado).**
- **Lo mas fuerte:** ejercicio de altisimo valor (genera/selecciona, no solo reconoce); `CounterexampleValidator.invalidates` (premisesRemainTrue && conclusionBecomesFalse) es autoridad objetiva; items sin contraejemplo balanceados controlan sesgo de refutacion.
- **Que debe ser null/preliminary:** near-miss rate `preliminary`; falsas alarmas con n bajo `null`.
- **Red Team:**
  - **La validez del near-miss depende ENTERAMENTE del generador**, que no existe. Con pack autorado fijo se memoriza cual es el caso -> el ejercicio NO mide razonamiento sino recuerdo. **BLOCK P0:** generador con `CounterexampleValidator` aplicado en construccion + tests de unicidad antes de exponer.
  - **"Marcar todos los casos" gamea el Jaccard** salvo penalty explicito por caso invalido seleccionado — el §scoring no lo formaliza. **P0.**
  - `caseDimensionality=4` x 8 casos = carga perceptiva/visual, no logica. **P1 (cap).**

### E5 — Silogismo Categorico (belief-bias)
**Veredicto: REVISE.**
- **Lo mas fuerte:** aisla FORMA de CONTENIDO (raro y valioso); modo abstracto/sin-sentido elimina atajo de creencia; `invalidRatio` balanceado bloquea respuesta fija.
- **Que debe ser null/preliminary:** **belief-bias index SIEMPRE `null` por sesion** (necesita pares congruente/incongruente balanceados, n insuficiente); accuracy binaria (50% azar) `confident=false`.
- **Red Team:**
  - **El belief-bias index es la joya pero solo es honesto agregado historicamente** — y no hay capa de serie. Listarlo como metrica primaria por sesion es falsa precision. **P0.**
  - **Claims:** "juzgar validez formal" es seguro, pero un copy descuidado deslizaria a "piensas mas racionalmente / menos sesgado en la vida" = far transfer level-5 PROHIBIDO. Debe pasar `review-cognitive-claims`. **P1 (veto preventivo K).**
  - Modo concreto introduce contenido culturalmente sesgado -> exigir paridad de contenido congruente/incongruente por construccion. **P1.**
  - Belief-bias index creciente intra-sesion se confunde con fatiga sin gate. **P1.**

### E6 — Inferencia Bayesiana Cualitativa
**Veredicto: BLOCK (riesgo de claims medium + magnitud no falsable).**
- **Lo mas fuerte:** mantener cualitativo (sin numeros) respeta la frontera de no-estadistica-clinica; ataca base-rate neglect y P(E|H)/P(H|E) (errores reales); selector ordinal reduce azar.
- **Que debe ser null/preliminary:** error ordinal de magnitud `preliminary`; base-rate neglect rate `preliminary`.
- **Red Team:**
  - **"Magnitud cualitativa" (sube-mucho/poco) es resbaladiza:** ¿quien decide que "sube mucho"? Si el generador no deriva la magnitud de una escala bayesiana interna determinista con **zona muerta amplia entre niveles**, la metrica mide la heuristica del AUTOR, no la del usuario -> no falsable. **P0.**
  - **Riesgo de claims MEDIUM:** roza "racionalidad/debiasing/decision-making mejorada". El charter prohibe afirmar reduccion de sesgos en la vida real (far transfer). El nombre y el copy DEBEN auditarse por K. **P0 claims.**
  - Con base-rates verbales sin numeros, riesgo de medir comprension verbal de probabilidad, no razonamiento -> exigir modo de bandas puramente visuales. **P1.**

### E7 — Transferencia Espejo
**Veredicto: BLOCK.**
- **Lo mas fuerte:** es el ejercicio que MEJOR cumple el principio near-transfer del repo (level 3-4 explicito); reusa infra real (`transferConsistencyFor`, `isIsomorphicProbe`, parametro `transferConsistencyScore` ya en la firma del scorer); regla 2026-06-15 (transfer=1.0 sii AMBOS lados correctos) es psicometricamente correcta.
- **Que debe ser null/preliminary:** **transfer consistency `null`/"calibrando" hasta n suficiente de pares** — y la regla AMBOS-correctos produce 0.0 muy frecuente al inicio, que NO debe mostrarse como "fallo" sino como calibracion.
- **Red Team:**
  - **El pack autorado tiene UN solo `logicalStructureId` probe** (`conditional_refutation_p_and_not_q`). Con 1 estructura, este ejercicio **no es viable**: no puede medir transferencia de nada. **BLOCK P0:** banco minimo >=8 estructuras isomorficas x >=2 variantes de superficie.
  - **Transfer=0.0 frecuente al inicio se leera como estancamiento** -> debe comunicarse `null`/em-dash "calibrando transferencia", nunca como regresion. **P0 (honestidad de display).**
  - Decoys estructurales mal construidos confunden en vez de discriminar -> validar por `CounterexampleValidator`/tests. **P1.**
  - **Claims:** aunque mida transferencia, sigue siendo SOLO near/level-3-4 — prohibido afirmar far transfer. El charter §4.5 exige las 7 condiciones (RCI, replicacion, sin contaminacion) para que cuente como EVIDENCIA; por sesion es siempre HIPOTESIS rotulada. **P0.**

### E8 — Ordenar la Derivacion (drag/ordering)
**Veredicto: BLOCK (renderer + ruido motor).**
- **Lo mas fuerte:** unico ejercicio que entrena estructura de prueba/no-circularidad; aceptar CUALQUIER orden topologico valido del DAG (no permutacion fija) es correcto; reusa la matematica Kendall-tau de `ClosedScoringEngine.Ordering` de Lenguaje.
- **Que debe ser null/preliminary:** Kendall-tau con pocos pasos `preliminary`; tiempo de razonamiento separado del motor (sin separar, RT es `null`).
- **Red Team:**
  - **Drag-and-drop introduce ruido MOTOR que contamina RT e ICR** — mide destreza tactil, no logica. `minimumReasoningTimeMs` no aplica igual. Exige registrar timestamp de primera-decision-firme, no del ultimo drag. **P0.**
  - **Kendall-tau vs ¿que orden?** Con multiples ordenes validos, comparar contra una permutacion de referencia penaliza ordenes correctos. `OrderingValidator` DEBE validar el DAG, no una permutacion. **P0.**
  - **Renderer nuevo** = riesgo de exponer en `implemented` SIN renderer (aprendizaje 2026-06-18: dominio listo ≠ jugable). **BLOCK hasta renderer + captura de respuesta.**

### E9 — Inferencia Suprimida (suppression task de Byrne)
**Veredicto: BLOCK (validez de contenido no resuelta).**
- **Lo mas fuerte:** fenomeno logico profundo y unico (defeasible vs monotono); items control (deduccion pura) balanceados detectan supresion espuria; 3 opciones reducen azar.
- **Que debe ser null/preliminary:** suppression discrimination (accuracy habilitadora vs control por separado) `null`/`preliminary` (necesita pares balanceados, agregacion inexistente).
- **Red Team:**
  - **Tension no resuelta de validez:** la supresion DEPENDE de conocimiento del mundo (condiciones habilitadoras). Abstraido a P/Q el fenomeno DESAPARECE; concreto mide conocimiento cultural, no logica. **BLOCK P0:** las condiciones habilitadoras DEBEN declararse EXPLICITAMENTE en el enunciado (logica defeasible explicita), no asumirse del mundo. Sin eso, mide cultura y se descarta.
  - **3-way "depende" es iman de respuestas evasivas** -> penalizar o exigir justificacion del "depende". **P1.**
  - Discriminacion necesita agregacion historica inexistente. **P0 modulo.**

### E10 — Puente de Analogia Estructural
**Veredicto: BLOCK (riesgo rojo de dependencia cultural).**
- **Lo mas fuerte:** razonamiento relacional (mapear relacion, no rasgo) es nucleo logico de alto valor; distractor superficial fuerte es el discriminador clave; aleatorizar posicion de la correcta.
- **Que debe ser null/preliminary:** surface-lure error rate `preliminary`; accuracy con `relationCount=3` `confident=false` (posible desborde WM).
- **Red Team:**
  - **RIESGO ROJO de dependencia cultural/lexica:** las analogias A:B::C:D son el ejemplo canonico de sesgo por vocabulario/educacion. **BLOCK P0:** TODAS las relaciones deben ser formalmente definidas (contencion, orden, oposicion, causalidad simple) sobre estimulos abstractos/visuales, NUNCA sobre semantica de palabras. Una sola relacion lexica -> se descarta por frontera cultural (es practicamente un test de vocabulario verbal disfrazado, que roza CI verbal PROHIBIDO).
  - **"Superficial" debe definirse operacionalmente** (que rasgo es superficial vs relacional) o el autor decide arbitrariamente -> la metrica no es objetiva. **P0.**
  - `relationCount=3` mide WM (mantener 3 relaciones), no analogia. **P1 (cap/separar).**

---

### Que debe ser null / preliminary (resumen modulo)
- TODA accuracy binaria/4-opciones por sesion con n<umbral -> `confident=false`, mostrar n.
- d-prime (E2), belief-bias index (E5), suppression discrimination (E9), surface-lure rate (E10), transfer consistency (E7) -> `null`/"calibrando" POR SESION; solo `preliminary` tras capa de agregacion historica (inexistente).
- ICR de todos -> arranca `preliminary`; `stable` solo con reliability gate + serie limpia + sin fatiga + dispositivo confiable + n suficiente.
- Cualquier sesion con `impulsiveCount>=2` o RT-CV creciente -> `fatigue_contaminated`, `update_long_term_profile=false`.

### Correcciones P0 (obligatorias pre-produccion, nivel modulo)
1. Implementar generadores seeded deterministas REALES por ejercicio (E1-E10), validados por tests de unicidad/validez, antes de cualquier `implemented`. El pack de 6 items autorados NO sale a produccion.
2. Cablear la maquina de estados de score del charter §4.2 (`value:null≠0`, `state`, `nEffective`, `dataQuality`, `invalidatedBy`, `interval`) — hoy solo hay `preliminary` booleano.
3. Gate de fatiga (no actualiza perfil estable) y deteccion de plateau/memorizacion (Logica NO tiene PlateauDetector) — sin esto, 6 items autorados producen false improvement.
4. Capa de agregacion cross-sesion para d-prime/belief-bias/suppression/transfer; mientras no exista, esas metricas se muestran `null`/"calibrando", NUNCA un numero.
5. E4/E7/E8/E9/E10 son **BLOCK** hasta cerrar su requisito especifico (generador validado / banco >=8 estructuras / renderer drag + tiempo cognitivo separado / condiciones habilitadoras explicitas / relaciones formales-abstractas auditadas por K).
6. Penalty explicito por seleccion irrelevante en todos los Jaccard (E1, E2, E4) — "marcar todo" no debe puntuar.
7. E6: auditoria de claims por `review-cognitive-claims` (anti "racionalidad/debiasing") + magnitud bayesiana con zona muerta deterministe.

### Correcciones P1 (producto excelente)
- Caps anti-WM: E1 chainLength<=4 inicial / E2 statementCount<=5 / E4 caseDimensionality / E10 relationCount.
- Aleatorizar orden Y longitud de opciones (E3, E10) para matar sesgo de superficie.
- Contador de confusion/error persistente tras micro-leccion (plateau dirigido) en E3/E5/E9.
- Modo de bandas puramente visuales en E6; paridad de contenido congruente/incongruente en E5.
- Separar tiempo motor de cognitivo en E8; control del "depende" evasivo en E9.

### Correcciones P2 (ventaja competitiva / investigacion)
- Estudio de validez de contenido (lectura vs logica) con control de comprension lectora baseline.
- Validacion empirica de near-transfer con RCI replicado (E7) antes de afirmar level-3-4 con fuerza.
- Banco rotativo de surfaceVariantId para resistir exposicion longitudinal.

### Red Team extremo (como lo destruyen)
- **Un regulador/cientifico** abre la app, juega 3 sesiones con 6 items autorados, ve subir el score, y demuestra que es **memorizacion pura sin PlateauDetector** -> "false improvement" = el pecado capital de los brain games (efecto Lumosity/FTC). Y senala que d-prime/belief-bias/transfer se muestran como numeros con n=4 = **falsa precision sancionable**.
- **Un competidor** publica que E6 ("Bayes/racionalidad") y E9/E5 ("menos sesgos") implican **far transfer a la toma de decisiones real** sin evidencia replicada — exactamente el claim level-5 que el propio charter prohibe — y lo usa para acusar a MindBoost de lo mismo que dice combatir.
- **Un psicometrista** demuestra que E10 (analogias verbales) y E1/E3 (densidad de texto) miden **vocabulario y comprension lectora**, no logica, colapsando la validez de constructo de medio modulo.

---


## Lenguaje

# Auditoría hostil — Módulo Lenguaje (8 ejercicios)

Panel: Red Team Hostil · Claims Auditor (veto) · Psicometrista Senior · Director Científico.
Lente aplicada: charter `00_CHARTER.md` (null≠0, 5 niveles de progreso no mezclados, gates de fatiga/dispositivo/transferencia, léxico prohibido incl. compuestos con guion).

**Veredicto global del módulo: REVISE.** Ningún ejercicio merece `approve` tal cual. El diseño es honesto en intención (cerrado, null≠0, gates declarados, evita IQ/diagnóstico), pero **todos comparten tres deudas P0 transversales que impiden aprobar**: (1) wiring de adaptación/transferencia/persistencia documentado como hueco ("hoy hueco", "demo no persiste") → un ejercicio que mide pero no progresa es un cuestionario estático que vende "entrenamiento adaptativo" que no existe; (2) **el módulo Lenguaje no tiene timebase real** (contador monótono, no reloj de alta resolución) y casi todos reportan "RT mediano de HITs" como métrica → todo RT es ruido device-dependiente sin el gate `UNTRUSTED_TIMEBASE`; (3) ninguno declara explícitamente el **n mínimo numérico** ni el umbral ρ de fiabilidad para pasar de `calibrating→preliminary→stable`, así que "preliminary=true siempre" es necesario pero no suficiente. E7 y E8 tienen además bloqueantes propios (timebase y Brier-con-n-bajo).

---

## E1 — Evidence Lock Pro

**Veredicto: REVISE.** (El propio doc dice "Aprobado con cambios" y lista un bloqueante sin resolver: sin wiring es cuestionario estático.)

**Lo más fuerte:** el paradigma de selección evidencial con Jaccard sobre `requiredEvidenceIds` penalizando over-claiming (excesivo) tanto como under-claiming es psicométricamente sólido y honesto; el mecanismo "inferencia mínima suficiente" es entrenable y cerrado (sin juez abierto).

| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| Mide conocimiento previo/cultura, no inferencia (`HIGH_PRIOR_KNOWLEDGE`) | high | media | invalida el constructo | banco de temas neutros + flag obligatorio + descartar trial si prior alto |
| Wiring de adaptación/transfer/persistencia hueco | high | alta | "adaptativo" es falso → falso progreso | P0: invocar `selectNext`/`verify` y persistir antes de exponer |
| "marcar todo" ⇒ 0.5 puede premiar adivinar la conclusión | med | media | inflación de score | test obligatorio: marcar-todo ⇒ 0.5 NUNCA 1.0; Jaccard exacto |
| RT mediano de HITs sin timebase real | med | alta | métrica secundaria ruidosa | rotular RT como informativo no-confiable o exigir timebase |
| `overClaimTrap` mal calibrado mide astucia de test | med | media | constructo contaminado | validación de ítem-discriminación del trap |

**Debe ser null/preliminary:** `INF_score` null si `Σweight<umbral` (correcto); `captureRate`/`overClaimIndex` preliminary hasta n_min explícito (no declarado — P0); RT secundario debe quedar `device_unreliable` sin timebase.

**Claims:** riesgo BAJO. Copy bien acotado a near-transfer "solo si NearTransferVerifier lo demuestra". No vende comprensión lectora general. Mantener.

**Red Team extremo:** un científico lo destruye mostrando que sin el flag de conocimiento previo el ítem mide si el usuario ya sabía el tema, no su inferencia; y un regulador señala que se llama "adaptativo" mientras el holder no adapta — publicidad de una capacidad inexistente.

---

## E2 — Semantic Surgery

**Veredicto: REVISE.**

**Lo más fuerte:** rúbrica cerrada por `changeType` (polaridad/cuantificador/alcance/conector/aspecto) con perfil de debilidad por tipo — discriminación semántica fina medible sin juicio abierto; la localización por token-tap añade una segunda señal verificable.

| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| Variant-bias: equivalente siempre la más corta / pivote siempre palabra rara | high | alta | se gamea por heurística superficial | balancear longitud y posición de pivote y de la equivalente (exigido) |
| Localización mide lectura visual, no semántica | med | media | constructo contaminado | pivote no-saliente, posición balanceada |
| `AnswerKey.SpanLocalization` nueva sin test de scoring (rama `when` olvidada) | high | media | scoring incorrecto silencioso | test de scoring de la clave nueva; `ExerciseCatalogConsistencyTest` no la cubre |
| Wiring adaptación hueco | high | alta | no adapta | integrar adaptación real |
| Dependencia de idioma afirmada como transferible | low | baja | claim translingüístico falso | declarar banco mono-idioma (ya exigido) |

**Debe ser null/preliminary:** `typeProfile[changeType]` null si n<umbral por tipo (correcto); `LEX_score` preliminary; localización combinada (0.6/0.4) preliminary hasta validar balanceo.

**Claims:** riesgo BAJO. Niega explícitamente "vocabulario más rico" y "comprensión global". Bien.

**Red Team extremo:** un competidor demuestra que un usuario gana sin leer eligiendo siempre la candidata más corta o tocando la palabra más larga — variant-bias convierte el ejercicio en un test de saliencia visual, no de semántica.

---

## E3 — Compression Ladder

**Veredicto: REVISE.**

**Lo más fuerte:** Kendall-tau parcial (no todo-o-nada) sobre fidelidad de orden, con penalización separada de adición (inventar) vs omisión — señal graduada honesta y resistente a la suerte; claim ya autolimitado a "reconocimiento de fidelidad", no "saber resumir".

| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| Claim "síntesis" cuando solo es reconocimiento (elegir ≠ producir) | med | media | sobre-claim de habilidad | copy estricto "discriminas resúmenes fieles" (exigido) |
| Kendall-tau con `candidateCount`=3 = pocos valores discretos → señal ruidosa | high | alta | métrica de baja resolución reportada como fina | forzar `candidateCount`≥4 en niveles altos (exigido) |
| Riesgo cultural en textos | med | media | mide familiaridad temática | banco neutro |
| Wiring adaptación hueco | high | alta | no adapta | activar adaptación |
| `additionPenaltyWeight` arbitrario sin calibrar | low | media | score distorsionado | documentar/validar el peso |

**Debe ser null/preliminary:** `fidelityIndex` null si insuficiente (correcto); `COMP_score` preliminary; con `candidateCount`<4 el Kendall-tau debería marcarse `low_sample`/baja resolución, no reportarse como estable.

**Claims:** riesgo BAJO-MEDIO. El riesgo es que el copy se deslice a "mejora tu capacidad de resumir/sintetizar"; el doc lo prohíbe pero hay que vigilarlo en UX.

**Red Team extremo:** un psicometrista muestra que con 3 candidatos el Kendall-tau solo toma ~3 valores, así que la "fidelidad fina" reportada tiene resolución de moneda al aire; reconocer un resumen no es producir uno, y el marketing podría implicar lo segundo.

---

## E4 — Argument X-Ray

**Veredicto: REVISE / cerca de BLOCK.** (El doc admite un bloqueante: banco sin validar ambigüedad de rol + renderer nuevo sin probar.)

**Lo más fuerte:** mapeo estructural de argumento (conclusión/premisa/irrelevante + suficiencia) con matriz de confusión rol-a-rol y prohibición de usar verdad-en-el-mundo — análogo verbal honesto del `JustificationClaim`.

| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| Ambigüedad de rol (oración premisa-y-conclusión) → "correcto" no único | high | alta | ítems sin respuesta única = constructo roto | BLOQUEANTE: rúbrica que prohíba ambigüedad + validación de unicidad de rol |
| Renderer nuevo "sentence-labeling" no probado | high | media | superficie UI no cubierta | `ExerciseCatalogConsistencyTest` renderer↔clave + tests UI |
| Mide familiaridad con metalenguaje ("premisa") no la habilidad | med | alta | mide jerga, no razonamiento | etiquetas funcionales ("afirmación principal"/"razón"/"no aporta") |
| Solapamiento con módulo Lógica → doble conteo en brain-profile | med | media | infla una dimensión | documentar diferenciación verbal vs lógico-formal en `ExerciseDefinition` |
| Wiring adaptación/transfer hueco + coste de autoría alto | high | alta | no adapta | wirear |

**Debe ser null/preliminary:** `ARG_score` null si insuficiente; `roleScore` preliminary; con renderer/banco sin validar, el ejercicio NO debe exponerse (estado `domainReady` pero NO `implemented`).

**Claims:** riesgo MEDIO. "detección de falacia estructural" debe NO leerse como "pensamiento crítico general"; el doc lo niega pero es el ejercicio con mayor tentación de sobre-claim ("aprende a detectar falacias"). Vigilar copy.

**Red Team extremo:** un científico encuentra un ítem donde una oración es legítimamente premisa Y conclusión, demostrando que el "rol correcto único" es una ficción del diseñador; un regulador objeta que medir si sabes la palabra "premisa" no es medir razonamiento argumental.

---

## E5 — Scope & Negation Drill

**Veredicto: REVISE.**

**Lo más fuerte:** "se sigue" = verdadero en todo modelo (necesidad, no plausibilidad), con cuota de casos `validInferenceCount=0` que rompe la heurística "siempre hay una válida" — anti-gaming real; Jaccard penaliza over/under-claim simétricamente.

| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| Solapamiento triple con E1 y con Lógica (NECESSARY_SUFFICIENT) | high | alta | triplica la misma señal en brain-profile | delimitar: E5=alcance del operador, E1=soporte evidencial, Lógica=forma; documentar |
| Sin cuota ~20% de "ninguna válida" → se aprende a marcar siempre algo | high | media | gaming por sesgo de respuesta | cuota obligatoria de ítems sin inferencia válida |
| Mide lógica formal en vez de procesamiento verbal | med | media | constructo equivocado | enunciados en lenguaje natural, sin notación (exigido) |
| Jaccard exacto frustrante en alta dificultad | low | media | abandono/ruido | crédito parcial controlado |
| Dependencia de idioma + wiring hueco | high | alta | claim falso + no adapta | banco mono-idioma; wirear |

**Debe ser null/preliminary:** `SCOPE_score` null si insuficiente (correcto); `operatorProfile[op]` null si n<umbral por operador; RT secundario device-unreliable sin timebase.

**Claims:** riesgo BAJO. Bien acotado, niega razonamiento probabilístico y comprensión de textos largos.

**Red Team extremo:** un científico muestra que sin la cuota de "ninguna válida" el usuario maximiza marcando siempre la opción más parecida — gana sin calcular alcance; y un revisor de brain-profile demuestra que E1+E5+Lógica están midiendo el mismo over-claiming tres veces y sumándolo como si fueran capacidades independientes.

---

## E6 — Source Triage

**Veredicto: REVISE.**

**Lo más fuerte:** triaje por criterios estructurales explícitos en el ítem (relevancia/especificidad/contradicción) con anti-sesgo-de-autoridad — entrena aplicación de criterios dados, no juicio de credibilidad real, lo cual es honesto y cerrado.

| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| Se lee como "detecta fake news / alfabetización mediática" | high | media | claim no soportado, frontera ética | copy estricto: "aplicación de criterios estructurales dados", nunca detección real |
| `authorityDecoy` induce la regla inversa "ignora la fuente seria" | high | alta | enseña heurística falsa | balancear ítems donde la prestigiosa SÍ es la mejor (exigido) |
| Kendall-tau con `sourceCount`=3 baja resolución | med | alta | métrica ruidosa | `sourceCount`≥4 en alto nivel |
| Enseña a "leer la rúbrica del test" no razonamiento de fuentes | med | media | gaming por predecir criterio | criterios variables y no predecibles |
| Wiring adaptación hueco | high | alta | no adapta | wirear |

**Debe ser null/preliminary:** `SRC_score` null si insuficiente; `criterionProfile[c]` null si n<umbral; `authorityBiasRate` solo sobre `nDecoyTrials` (correcto), preliminary.

**Claims:** riesgo MEDIO. Es el ejercicio con mayor riesgo de claim engañoso ("te enseña a detectar desinformación"). El léxico no es prohibido, pero la promesa de transferencia al mundo real (detección de fake news) violaría la regla de transferencia lejana sin evidencia. P0: copy blindado.

**Red Team extremo:** un regulador lo destruye señalando que comercializar "triaje de fuentes / credibilidad" insinúa alfabetización mediática real cuando solo se aplican etiquetas inventadas por el diseñador; un científico muestra que el `authorityDecoy` unidireccional enseña el anti-patrón "desconfía siempre del experto".

---

## E7 — Lexical Access Sprint

**Veredicto: BLOCK (hasta integrar timebase de alta resolución + gates anti-impulsividad).**

**Lo más fuerte:** es el único RT-gated, bien modelado sobre Speed (IES anti trade-off, anticipaciones sobre crudos respetando el aprendizaje 2026-06-04, MAD solo sobre tendencia central, velocidad solo sobre HITs). El diseño psicométrico es el más maduro del módulo.

| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| Módulo Lenguaje no tiene timebase real (contador monótono) → RT = ruido | critical | alta | toda métrica de velocidad inválida | P0 BLOQUEANTE: integrar timebase alta resolución (Choreographer) o `device_unreliable`+no actualizar perfil |
| Sin `ContainImpulsivity`/anti-anticipación real premia tapping rápido | critical | alta | falso progreso por impulsividad | P0: gates obligatorios y testeados antes de exponer |
| Solapa con Speed (RT genérico) → doble conteo | high | media | infla dimensión speed | mapear a `CognitiveDimension` VERBAL no speed (exigido) |
| Frecuencia léxica del español no calibrada empíricamente | med | alta | `semanticDistance`/dificultad subjetiva | calibración empírica de frecuencia/distancia |
| Dependencia fuerte de idioma | med | alta | claim translingüístico | banco mono-idioma calibrado |

**Debe ser null/preliminary:** `IES` null si `acc==0` o `n<nMin` (correcto, "null nunca 0"); `LEX_speed_score` preliminary siempre; **sin timebase confiable TODO el score debe ser `device_unreliable` + `invalidatedBy=UNTRUSTED_TIMEBASE` + no actualiza perfil estable** (charter §4.4). Anticipaciones/lapsos sobre crudos.

**Claims:** riesgo BAJO en copy (niega vocabulario más amplio y velocidad lectora). El riesgo no es de claims sino de medición inválida presentada como velocidad real.

**Red Team extremo:** un ingeniero demuestra que en un dispositivo lento el "RT mediano" varía 80 ms entre teléfonos sin timebase vsync, así que la velocidad reportada es del hardware, no del usuario; y sin anti-anticipación un usuario sube su score machacando la pantalla más rápido que el deadline.

---

## E8 — Calibrated Comprehension

**Veredicto: BLOCK (hasta umbral n agregado para Brier + test anti-degeneración).**

**Lo más fuerte:** separa comprensión y metacognición en dos barras independientes (no las mezcla en un número), staircase NO usa Brier (evita doble penalización), confianza obligatoria pre-feedback. La arquitectura de calibración es honesta y bien pensada.

| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| Brier con 6–10 ítems estadísticamente débil reportado como metacognición | critical | alta | ruido vendido como señal | P0 BLOQUEANTE: umbral n explícito (acumulado inter-sesión), barra `null` honesta |
| Gaming del Brier (siempre MED) | high | alta | score inflado por estrategia | penalización de confianza degenerada obligatoria y testeada |
| Doble objetivo confunde "qué se mide" | med | media | confusión UX/falsa precisión | UX separa claramente las dos señales |
| Sin n_min numérico declarado | high | alta | `preliminary` no basta | declarar n_min y ρ_min para META |
| Mezclar comprensión+calibración en una dificultad | med | baja | señal contaminada | ya separado (correcto) |

**Debe ser null/preliminary:** `META_score`/`calibrationScore` null si n<umbral (P0 — el umbral debe ser explícito y probablemente acumulado, no por sesión); `COMPREHENSION_score` preliminary; `overconfIndex` null si pocos errores. Ambas barras `preliminary=true` siempre. Confianza degenerada ⇒ `LOW_EVALUATOR_CONFIDENCE` ⇒ META null.

**Claims:** riesgo BAJO-MEDIO. Niega "metacognición general" y "saber lo que sabes en cualquier dominio". El riesgo es la falsa precisión de un Brier ruidoso presentado como "tu calibración"; el em-dash honesto lo mitiga si se respeta el umbral.

**Red Team extremo:** un psicometrista calcula que el error estándar del Brier con 8 ítems es tan grande que la "calibración" reportada es indistinguible de azar entre sesiones; un red-teamer muestra que marcar siempre MED minimiza el Brier sin discriminar nada, ganando la barra sin metacognición real.

---

## Síntesis de correcciones transversales (P0 para TODO el módulo)
1. **Wiring real** de `AdaptationEngine.selectNext` + `NearTransferVerifier.verify` + persistencia event-sourced en cada holder antes de exponer (hoy hueco en E1–E6, E8). Sin esto NO se puede llamar "adaptativo".
2. **Timebase de alta resolución** o gate `UNTRUSTED_TIMEBASE`: cualquier ejercicio que reporte RT (E1, E2, E5, E8 secundario; E7 primario) debe degradar a `device_unreliable` sin reloj vsync. E7 está BLOQUEADO por esto.
3. **n_min numérico explícito + ρ_min** por métrica para las transiciones `calibrating→preliminary→stable`. "preliminary=true siempre" es necesario pero no define cuándo deja de ser null.
4. **Tests anti-gaming obligatorios**: marcar-todo⇒0.5 (E1), confianza-degenerada⇒META null (E8), variant-bias longitud/posición (E2/E3/E6), cuota "ninguna válida" (E5).
5. **Delimitación brain-profile**: E1/E5/Lógica (over-claiming) y E4/Lógica (estructura) y E7/Speed (RT) no deben triple-contar la misma capacidad.

P1: etiquetas funcionales no técnicas (E4), copy blindado anti-"fake news" (E6), banco temático neutro (E1/E3). P2: calibración empírica de frecuencia léxica (E7), crédito parcial controlado en Jaccard (E5).

---


## Memoria

## Auditoría — Módulo MindBoost · Memoria (8 ejercicios)

**Contexto:** módulo fuerte de base. A diferencia de catálogos más laxos, este ancla TODA claim de mejora real a una sonda UNTRAINED + `ReliableChangeIndex` + `RealImprovementCriterion`, usa d' en vez de hit-rate crudo, separa intrusión/binding/swap, y rota contenido con `RecentItemRegistry`. La honestidad estructural está bien pensada. Pero hay un agujero transversal grave: **el contrato de score §4.2 (ScoreState/dataQuality/invalidatedBy) y el gate de dispositivo §4.4 NO aparecen en ninguna ficha**, pese a que 6 de 8 ejercicios usan deadlines/RT/ISI como palanca de dificultad. Eso es un P0 que baja varios veredictos. Abajo, ejercicio por ejercicio.

---

### 1. Span Adaptativo Dual (rediseño EX1) — **REVISE**

**Lo más fuerte:** umbral de validez de processing ≥85% para que el trial alimente el staircase (impide sacrificar la tarea concurrente), y el reconocimiento explícito de que la claim de mejora va SOLO contra la sonda UNTRAINED.

| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| `processingDeadlineMs` (3500→1800) es palanca temporal sin gate de timebase: en dispositivo lento un deadline "perdido" es artefacto, no fallo cognitivo | high | media | falso bajón de span / perfil contaminado | RT/deadline relativos al RT base del usuario (ya pedido en revisión crítica pt.4) + `dataQuality=device_unreliable`→no actualiza perfil |
| Processing aritmético = confound de habilidad numérica/educativa | med | alta | mide cálculo, no WM | reading-span como default rotatorio (ya exigido); documentar el confound en copy |
| Practice effect MEDIO en estructura del processing | med | alta | falso progreso intra-tarea | rotación + claim solo sobre UNTRAINED |
| Contrato de score §4.2 ausente (no expone state/nEffective/invalidatedBy) | high | alta | score inflado sin gates | implementar la máquina de estados completa |

**Debe ser null/preliminary/calibrating:** `WORKING_MEMORY_SPAN` con n de trials válidos < umbral → `value=null`, display "—", state `insufficient_data` (NUNCA 0). `DUAL_TASK_COST` exige span_single Y span_dual válidos en la MISMA sesión (medida comparativa, §4.8): falta un lado → null. `CALIBRATION_INDEX` (overlay EX16) preliminary hasta n suficiente.

**P0:** (a) gate de dispositivo en `processingDeadlineMs`; (b) máquina de estados §4.2; (c) span reportado solo sobre trials con processing ≥85%. **P1:** reading-span rotatorio default; deadline calibrado al RT base. **P2:** symmetry-span visoespacial.

**Red Team extremo:** un psicometrista mostraría que "subir span" correlaciona con dominar el truco aritmético, no con WM; y un regulador señalaría que sin gate de timebase un usuario con teléfono viejo recibe un perfil peor por hardware — falsa precisión accionable.

---

### 2. N-Back Adaptativo de Doble Modalidad — **REVISE**

**Lo más fuerte:** reporta `RECOGNITION_DPRIME` + `RESPONSE_CRITERION` (no hit-rate), descarta bloques con criterio extremo, y la ficha PROHIBE explícitamente claims de Gf/IQ — el n-back es el caso de uso más abusado del sector y aquí se trata con honestidad.

| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| Riesgo de claims de far-transfer a inteligencia (lexico/expectativa) | high | media | claim prohibido §1 | copy blindado a "updating de WM" + progreso vs baseline; scanner |
| `isiMs`/`stimulusOnMs` como palancas temporales sin gate de timebase | high | media | d' contaminado por hardware | gate §4.4 device_unreliable |
| d' inestable con pocos targets/lures por bloque | high | alta | sensibilidad ruidosa declarada como real | `confident=false` si targets/lures < umbral para z estable (ya citado) → state preliminary |
| Practice effect MEDIO-ALTO en stream fijo | med | alta | falso progreso | stimulusSetSize rotatorio + jitter (citado) |

**Debe ser null/preliminary/calibrating:** `RECOGNITION_DPRIME` con z inestable (hit/FA en 0 o 1, o n bajo) → null/preliminary, nunca d' calculado sobre proporciones extremas sin corrección (aplicar corrección de Snodgrass-Corwin o marcar null). `DUAL_TASK_COST` requiere d'_single Y d'_dual válidos. `nEffective` debe definirse como "n máx con d'≥1.0 SOSTENIDO", no pico.

**P0:** (a) corrección/null en z extremos; (b) gate de timebase; (c) copy sin Gf/IQ (veto K). **P1:** feedback diferido por defecto para medir aprendizaje real, no andamiaje. **P2:** n-back continuo adaptativo V2.

**Red Team extremo:** un científico citaría el meta-análisis de far-transfer nulo del n-back; si el copy roza "entrena tu inteligencia", el regulador veta el ejercicio entero. Un competidor demostraría que un d' "alto" salió de un criterio conservador (casi nunca responde sí), no de sensibilidad.

---

### 3. Updating de Ranura (Running Memory / EX2) — **APPROVE (con P0 de score-state)**

**Lo más fuerte:** separa formalmente `INTRUSION_RATE` (fallo de updating) de `BINDING_ERROR_RATE` (ranura equivocada) — evita medir "una sopa"; fillers a ranuras inexistentes detectan respuesta automática; sonda de una ranura aísla la señal del coste de output. Paradigma genuinamente distinto del n-back.

| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| `updateRateMs` palanca temporal sin gate device | med | media | k contaminado por hardware | gate §4.4 (menos crítico: no es RT de respuesta, es ritmo de presentación) |
| Confusión usuario con n-back | low | media | frustración UX | UX clara "estado final" |
| Contrato §4.2 ausente | med | alta | score sin estados | máquina de estados |

**Debe ser null/preliminary/calibrating:** `WORKING_MEMORY_SPAN` (k) con probes < umbral → null/insufficient_data. `BINDING_ERROR_RATE` e `INTRUSION_RATE` preliminary hasta n suficiente; nunca reportar 0 cuando es "no medido".

**P0:** máquina de estados §4.2; null si probes < umbral. **P1:** probeOne por defecto (señal limpia). **P2:** spatial updating como puente visoespacial.

**Red Team extremo:** el ataque más fuerte ("¿es n-back con etiquetas?") ya está neutralizado por las dos métricas de error separadas y los fillers obligatorios. El riesgo residual es de score-state, no de constructo.

---

### 4. Grid Espacial con Binding — **REVISE**

**Lo más fuerte:** separa location vs binding vs swap (tres señales), usa `COWAN_K` con false alarms (no hit-rate) en change-detection, y bloquea el recoding verbal con alta similitud cromática/forma + grid distractor.

| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| Recoding verbal ("círculo rojo arriba-izq") convierte un test visoespacial en verbal disfrazado | high | alta | mide constructo equivocado | objetos sin nombre verbal fácil + alta similitud (citado, debe ser obligatorio no opcional) + supresión articulatoria opcional |
| `delayMs` 0→8000 con máscara y `studyMsPerItem` dependen de timebase | med | media | K contaminado | gate device en componentes temporales |
| K calculado sobre c demasiado bajo o trials < umbral | high | alta | K inestable declarado estable | `confident=false`/null (citado) → state |
| Random-placement (desistimiento) parece dato | med | media | perfil contaminado | accuracy≈chance → dataQuality anomalous, no actualiza perfil |

**Debe ser null/preliminary/calibrating:** `COWAN_K` con trials < umbral o c bajo → null. `BINDING_ERROR_RATE` solo computable sobre celdas con localización correcta (condicional) — si correctCells=0, binding = null, no 0. `RECOGNITION_DPRIME` con z extremos → null.

**P0:** (a) bloqueo de recoding verbal OBLIGATORIO (no variante opcional); (b) detección random-placement→anomalous; (c) score-state. **P1:** gate timebase en delay/study. **P2:** Corsi-like de orden.

**Red Team extremo:** un revisor demuestra que con objetos nombrables (rojo/azul/círculo) un usuario verbaliza y el "K visoespacial" sube sin memoria espacial real — el ejercicio mediría lo mismo que el span verbal y la cobertura cognitiva del módulo (que vende canal visoespacial separado) sería falsa.

---

### 5. Pares Asociados A-B/A-C (rediseño EX4) — **REVISE**

**Lo más fuerte:** el paradigma A-B/A-C mide interferencia proactiva controlada (lo más cercano a "actualizar una asociación real"); separa intrusión A-B de error de conjunción; demora real anclada a `EpochClock` (no saltable).

| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| Practice effect ALTO (`HIGH`): si se reutiliza banco, el "progreso" es memorización de ítems | critical | alta | falso progreso vendido como mejora de capacidad | excludeRecentPairIds ESTRICTO + claim solo sobre pares UNTRAINED (citado, debe ser hard-gate) |
| Probe +24h depende de re-engagement; si no se completa, ¿se reporta delta crudo? | high | media | claim de "mejora real" sin diferido | `confident=false`/null si probe +24h incompleta; gate explícito |
| Producción libre con matching tolerante puede aceptar respuestas erróneas o rechazar correctas | med | media | accuracy ruidosa | lemmatize + normalización validada; medir tasa de match ambiguo |
| `VOCABULARY` (L1-L2) roza claim de transferencia a aprendizaje real | low | media | sobre-claim nivel 4-5 | rotular como hipótesis nivel 3-4, nunca evidencia sin RCI |

**Debe ser null/preliminary/calibrating:** `ASSOCIATIVE_INDEX` con pares < umbral → null. `INTERFERENCE_INDEX` (recall_AC_overlap / recall_AC_noOverlap) requiere AMBAS condiciones en la sesión (comparativa §4.8) → falta una → null. `DELAYED_RECALL_ACCURACY` null si probe +24h no completada (no 0, no "asumido"). `PROACTIVE_INTERFERENCE` preliminary.

**P0:** (a) rotación de contenido como hard-gate (no recomendación); (b) null si probe diferida incompleta; (c) interference index solo con ambos lados; (d) claim de mejora solo sobre UNTRAINED. **P1:** matching tolerante validado. **P2:** caras-nombres EX8.

**Red Team extremo:** con HIGH practice-effect, si el banco se repite aunque sea parcialmente, un científico demuestra que la curva de mejora es aprendizaje de ítems específicos (memorización), no capacidad asociativa — y "mejoraste tu memoria asociativa" se vuelve una claim falsa y demandable.

---

### 6. Episódica con Recall Diferido y Reconstrucción (rediseño EX3) — **REVISE**

**Lo más fuerte:** exige recall diferido +24h ANCLADO a reloj como parte del scoring de consolidación; tarea distractora antes del inmediato (mide LTM, no recencia); `PlateauDetector.FalseImprovement` vigila el gap inmediato↔diferido (detecta el truco de "subir inmediato sin consolidar").

| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| Practice effect ALTO en listas; reutilización = familiaridad, no memoria | critical | alta | falso progreso | rotación obligatoria + UNTRAINED |
| Probe +24h frecuentemente no completada → métrica gobernante (`DELAYED_RECALL_ACCURACY`) sin datos | high | alta | claim de mejora sin su métrica principal | `confident=false`/null si +24h incompleta (citado, hacerlo hard) |
| ARC clustering inestable con pocas categorías/ítems | med | media | proxy ruidoso | preliminary hasta n; reportar con caveat |
| JOL `CALIBRATION_INDEX` con pocos juicios | med | media | calibración ruidosa | calibrating hasta n |
| Intrusiones extralista (falsas memorias) — buena señal, pero si no se penalizan, recall inflado | med | media | accuracy inflada | penalización citada, mantener |

**Debe ser null/preliminary/calibrating:** `DELAYED_RECALL_ACCURACY` (gobernante) null si probe +24h no completada — NUNCA imputar con el inmediato. `CONSOLIDATION_INDEX` (delayed/immediate) null si falta delayed. `FORGETTING_RATE` null si falta cualquier punto. `ARC_CLUSTERING` preliminary. `CALIBRATION_INDEX`/JOL calibrating.

**P0:** (a) null estricto si +24h incompleta (no imputación); (b) rotación obligatoria; (c) distractor obligatorio pre-inmediato; (d) FalseImprovement gate activo. **P1:** ARC con caveat de n. **P2:** testing-effect variante.

**Red Team extremo:** el "memoriza una lista" es el ejercicio más antiguo y con mayor practice-effect; sin diferido real es solo un buffer test, y si una sola administración o listas repetidas alimentan la claim, un regulador lo califica de pseudo-medición. El gap inmediato↔diferido es lo único que lo salva — debe ser ejecutorio, no documental.

---

### 7. Orden Serial Temporal — **APPROVE (con P0 de disociación y score-state)**

**Lo más fuerte:** scoring estrictamente por POSICIÓN (no "ítems presentes"); disocia order memory de item memory con variante de reconstrucción; mide transposiciones y gradiente posicional para validar que la señal es de orden (gradiente plano = azar = baja calidad). Aporta una dimensión cognitiva genuinamente distinta.

| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| Sin disociar orden de item memory, es "otro span" | high | media | constructo redundante | variante reconstrucción que da los ítems (citado) + métrica orderSpecificScore |
| `presentationRateMs`/`delayMs` dependen de timebase | med | media | order accuracy contaminada | gate device en componentes temporales |
| Chunking del usuario infla span de orden | med | media | falso progreso | grouping continuo + tap concurrente (citado) |
| Contrato §4.2 ausente | med | alta | score sin estados | máquina de estados |

**Debe ser null/preliminary/calibrating:** `SERIAL_ORDER_ACCURACY` con trials < umbral → null. `orderSpecificScore` (orden por encima de item memory) requiere AMBOS (order accuracy E item accuracy) válidos en la sesión → falta uno → null. Transposition gradient con pocos trials → preliminary.

**P0:** (a) disociación order/item ejecutoria (variante reconstrucción incluida); (b) score-state; (c) gradiente plano→anomalous. **P1:** gate timebase. **P2:** supresión articulatoria.

**Red Team extremo:** un competidor demostraría que sin la variante de reconstrucción, "span de orden" y "span de ítem" correlacionan casi perfectamente y el módulo está vendiendo dos ejercicios que miden lo mismo. La disociación debe estar en producción, no solo en el doc.

---

### 8. Sonda de Transferencia UNTRAINED — **REVISE**

**Lo más fuerte:** es el corazón de la honestidad del módulo. Material reservado (`TransferProbePartition`), dificultad FIJA, cero feedback (no entrena al medir), RCI gated por `RciResult.NotEnoughData` hasta ≥2-3 administraciones, claim de mejora SOLO si `genuine && RCI.reliable`, y NO administra con fatiga HIGH. Conceptualmente impecable.

| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| Formas paralelas A/B/C NO equiparadas en dificultad → el delta es ruido y el RCI declara "mejora fiable" sobre basura | critical | alta | claim de mejora real FALSA (la peor del módulo) | equating estadístico de formas ANTES de comparar (P0 bloqueante); hasta equiparar, solo delta crudo con caveat fuerte |
| RCI con reliability mal estimada (ρ inventada o de un solo punto) | critical | media | "reliable=true" sin base | reliability estimada con ≥2-3 administraciones; `NotEnoughData` por defecto (citado) |
| Componente +24h UNTRAINED con baja tasa de completado | high | alta | `CONSOLIDATION_INDEX` UNTRAINED sin datos | null si incompleto; no imputar |
| Probe administrada con fatiga moderada (no solo HIGH) puede sesgar | med | media | medición sesgada | gate fatiga estricto; registrar `fatigueWasLow` y descartar si false |
| Si la probe usa palancas temporales fijas en dispositivo no perfilado | high | media | RCI sobre RT contaminado por hardware | timebase confiable obligatorio o métrica no-temporal; device_unreliable→no computa RCI |

**Debe ser null/preliminary/calibrating:** TODO output es preliminary/insufficient_data hasta ≥2-3 administraciones (RCI `NotEnoughData`). `NEAR_TRANSFER_INDEX` null con una sola administración. `untrainedDelta` reportable como delta crudo CON caveat "sin evidencia de cambio fiable" hasta RCI reliable. `effectSize` (Cohen's d) preliminary hasta n y SD estable. Nivel de progreso: esto es lo único que puede tocar niveles 3-4; nivel 5 (far) NUNCA sin replicación.

**P0 (bloqueantes):** (a) equating de formas paralelas — sin esto la sonda PRODUCE claims falsas y debe bloquearse; (b) RCI gated por reliability con ≥2-3 administraciones; (c) gate de fatiga (no solo HIGH); (d) claim solo si `genuine && RCI.reliable`; (e) si mide RT, timebase confiable o no computa. **P1:** far-transfer probe etiquetada como hipótesis. **P2:** calibration probe con JOL.

**Red Team extremo:** esta pieza es a la vez la salvación y la mayor responsabilidad legal del módulo. Un psicometrista destruye todo el sistema con una frase: "tus formas A/B/C no están equiparadas, así que tu RCI mide diferencia de dificultad entre formas, no cambio del usuario — y has declarado 'mejora fiable de capacidad' sobre ruido". Sin equating documentado y verificado, la sonda no debe alimentar ninguna claim.

---

## Conclusión transversal (P0 que aplica a TODO el módulo)

1. **Contrato de score §4.2 ausente en las 8 fichas.** Ninguna expone `state` / `dataQuality` / `nEffective` / `invalidatedBy` / `display "—"`. Es la máquina de estados que hace honesto al producto y NO está cableada al diseño. P0 global.
2. **Gate de dispositivo §4.4 ausente.** 6/8 usan deadlines/RT/ISI/presentationRate como palanca; ninguna declara `device_unreliable`/`UNTRUSTED_TIMEBASE`→no-actualiza-perfil. P0 global.
3. **Gate de fatiga §4.3** mencionado como "señal" pero no como regla ejecutoria (`update_long_term_profile=false`) en ninguna ficha salvo implícitamente en la sonda. P0 global.
4. **Equating de formas paralelas** (ej. 8) es el riesgo crítico individual: produce claims falsas de mejora fiable.
5. **Niveles de progreso §4.1** bien manejados conceptualmente (la sonda separa entrenado de UNTRAINED), pero cada ficha debe declarar EXPLÍCITAMENTE en cuál de los 5 niveles afirma, y los de practice-effect ALTO (5, 6) deben blindar el nivel 1 contra reutilización de banco.

**Claims:** no detecté léxico prohibido literal en el copy propuesto; el módulo es disciplinado (prohíbe Gf/IQ explícitamente en n-back). Riesgo de claims es por OMISIÓN de gates (declarar "mejora real" sin RCI/equating/UNTRAINED), no por vocabulario. Mantener el scanner estricto sobre el copy final.

---


## Decisiones

# Auditoría — Módulo Decisiones (panel hostil: Red Team J · Claims Auditor K [veto] · Psicometrista C · Director Científico A)

**Resumen ejecutivo:** Módulo conceptualmente fuerte y diferenciado (es el más rico del catálogo: BUR bayesiano, reversibilidad/impulsividad, calibración proper-scored, VOI). Pero TODOS los ejercicios comparten 4 talones de Aquiles que el panel considera P0 transversales y que impiden cualquier `approve` limpio:

- **(T1) Banco de ítems textual no validado** (E1, E3, E6, E8 sobre todo): mide cultura/idioma/conocimiento previo, no la capacidad diana. Es el riesgo de validez nº1 del módulo.
- **(T2) Solver/optimo como única fuente de verdad** (E1 BayesEngine, E7 knapsack, E8 VOI): si el "óptimo" tiene un bug, el feedback Y el score MIENTEN simultáneamente. Sin tests exhaustivos del motor, es un generador de falso progreso con apariencia de rigor.
- **(T3) Confounds no-cognitivos**: destreza motora fina (dial E1, drag E7), lectura (todos los textuales), rasgo de personalidad (aversión al riesgo en E8, perfeccionismo en E5/E7). El charter §4.4 exige timebase confiable para RT; ningún ejercicio declara device-gating explícito (`device_unreliable`/`UNTRUSTED_TIMEBASE`) pese a usar RT, deadlines y locks en ms.
- **(T4) Contrato de score honesto sub-especificado**: el módulo dice "`null` si n insuficiente, `preliminary=true` siempre" pero NO instancia la máquina de estados completa del charter §4.2 (state, nEffective, dataQuality, invalidatedBy, interval) por métrica. Varias métricas que DEBEN nacer `null`/`calibrating` no están marcadas como tal.

Ninguno vende humo descarado, pero E1, E3, E5, E6, E7, E8 mezclan o rozan la mezcla de niveles de progreso y carecen de un gate de medición instanciado → `revise`. E2 y E4 son los más cercanos a producción pero también `revise`.

---

## E1 — Bayes Arena: Calibrador de Posterior Continuo

**Veredicto: revise**

**Lo más fuerte:** El BUR en espacio logit es una métrica de actualización bayesiana genuina y poco gameable, y la Revisión crítica ya identifica los 3 confounds clave (motor, feedback-coherente, priori=posterior). El punto (3) — excluir trials priori≈posterior del BUR — es psicométricamente impecable y rara vez visto.

**Riesgos:**
| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| Confound destreza motora del dial continuo → mide fine-motor, no razonamiento | high | media | invalida la métrica primaria | dialGranularity grueso (20 pasos) + banda ≥1 paso en condición de medida (ya propuesto P0 en revisión: HACERLO vinculante, no "sugerido") |
| Banco textual → mide lectura/cultura (T1) | high | alta | sesgo sistemático por idioma | plantillas neutras validadas + glyphs no-numéricos; reportar `device_unreliable`-análogo si legibilidad falla |
| BUR colapsa a 1.0 cuando logitMath≈0 → falso "actualizador perfecto" | high | alta | infla score sistemáticamente | excluir trials priori≈posterior del scoring (NO contar como BUR=1.0) |
| RT sin gate de timebase | med | alta | RT/IES no comparables cross-device | declarar `UNTRUSTED_TIMEBASE`→no actualiza perfil estable si no hay Choreographer/vsync |
| Mezcla nivel transferencia: "reducción del base-rate neglect en variantes no entrenadas" se presenta junto a near sin RCI | med | media | claim de nivel 3 sin las 7 condiciones | rotular como hipótesis nivel 3/4, nunca evidencia |

**Debe ser null/preliminary/calibrating:** BUR mediana hasta n≥MIN_N de trials VÁLIDOS (excluidos priori≈posterior); CAG necesita acierto-en-banda con n suficiente → `calibrating` antes; IES y RT → `device_unreliable` sin timebase. `preliminary=true` siempre (correcto). El "scoreBayes" agregado debe ser `null` si cualquiera de sus componentes lo es.

**P0:** (1) dialGranularity grueso + banda ≥1 paso vinculante; (2) excluir priori≈posterior del BUR; (3) mismo BayesEngine para feedback y clasificación; (4) gate de timebase para RT/IES; (5) plantillas textuales neutras validadas. **P1:** dirección de sesgo (over/under) como diagnóstico, no como score. **P2:** variante frecuencias-naturales como condición experimental de transferencia cercana.

**Red Team extremo:** Un psicometrista destruye esto mostrando que el 30-40% de trials con base-rate≈posterior dan BUR=1.0 automático → el "score de razonamiento bayesiano" sube sin que el usuario razone; un regulador añade que el banco textual mide comprensión lectora en español, no cognición, y que comparar RT entre un Pixel y un gama-baja invalida IES sin que el doc lo advierta.

---

## E2 — Triaje de Reversibilidad: Puerta de Una Vía vs Dos Vías

**Veredicto: revise**

**Lo más fuerte:** La señal de impulsividad (tasa de violación de lock) es conductual, limpia y difícil de gamear; la asimetría lambda 0.8/0.15 reversible/irreversible está bien pensada y la Revisión crítica ya blinda lo principal (premiar la espera honesta, no tratarla como lentitud).

**Riesgos:**
| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| Memorización de marcadores de coste (familiaridad UI) → mide reconocer iconos, no derivar coste | high | alta | falso progreso por aprendizaje superficial | rotar/variar marcadores entre sesiones (ya P0 en revisión: vinculante) |
| Claim de impulsividad roza terreno clínico | med | media | "reducción de impulsividad" puede leerse como rasgo | copy ya prohíbe "impulsividad clínica" — verificar que UX/coach NUNCA digan "eres impulsivo"; solo "hoy respondiste antes del tiempo de revisión" |
| RT en reversibles sin gate de timebase | med | alta | RT no comparable | `UNTRUSTED_TIMEBASE` gate |
| Lock como fricción percibida arbitraria | low | media | abandono/frustración | justificar el lock como medida, no castigo |
| Banda de tipos ≈0.5 frustra | low | media | contamina por estrés | mantener accuracy-band adaptativa |

**Debe ser null/preliminary:** Accuracy de triaje y lock-violation-rate → `calibrating` hasta n por tipo suficiente (separar reversible/irreversible). RT reversibles → `device_unreliable` sin timebase. IED → `null` si n insuficiente. Nunca aplicar IES de velocidad a irreversibles (ya cubierto).

**P0:** (1) marcadores rotan entre sesiones; (2) scoring premia lock-respetado, jamás lo penaliza como lentitud; (3) UI no muestra "demasiado lento" en irreversibles; (4) gate timebase. **P1:** variante coste-asimétrico para validar calibración al coste. **P2:** reversibilidad parcial (renderer 3 zonas).

**Red Team extremo:** Un competidor demuestra que tras 5 sesiones el usuario reconoce los 6 iconos de memoria y clasifica al instante sin razonar coste alguno → el ejercicio mide memoria visual de glyphs, no triaje de reversibilidad, y el "progreso" es puro aprendizaje de la UI.

---

## E3 — Forecast Calibrado: Apuesta tu Confianza

**Veredicto: revise**

**Lo más fuerte:** Brier es un proper scoring rule legítimo y el anti-colapso-al-50% vía decisional courage con peso suave (0.15) está correctamente pensado. La calibración confianza-acierto es la señal metacognitiva más defendible del módulo.

**Riesgos:**
| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| Banco de ítems: si se acierta por conocimiento, mide cultura/trivia, no calibración (T1) | critical | alta | invalida toda la métrica primaria | ítems de evidencia-CONTROLADA (p(acierto) fijada por diseño, no por saber) — la propia revisión lo marca como condición sine qua non |
| Brier solo es proper con outcome objetivamente verificable | high | media | preguntas de opinión rompen el scoring | prohibir ítems de opinión; ground truth determinista |
| calErr con 5 bins exige n≥~25 → reportar bajo eso es falsa precisión | high | alta | curva de calibración mentirosa con n bajo | `null`/`insufficient_data` < n_min por bins |
| courage mal pesado empuja a sobreconfianza temeraria | med | baja | daño a calibración | peso suave 0.15, nunca dominante (ya cubierto) |

**Debe ser null/preliminary:** **calibration error y la curva confianza-vs-acierto → `insufficient_data`/`null` por debajo de ~25 trials con bins poblados** (esto es lo más crítico: una curva de calibración con n<25 es ruido presentado como precisión). Brier → `calibrating` antes de n_min. CAG dirección de sesgo → diagnóstico, no score, hasta estabilidad. `preliminary=true` siempre.

**P0:** (1) ítems evidence-controlled obligatorios (sin esto = block); (2) prohibir ítems de opinión; (3) calErr `null` < n_min; (4) gate timebase para RT. **P1:** variante confianza pre/post evidencia. **P2:** mercado de predicción personal.

**Red Team extremo:** Un científico señala que sin ítems de evidencia-controlada el ejercicio mide cuánta trivia sabe el usuario (un experto en deportes "calibra" mejor en preguntas de deportes), no su metacognición; y que mostrar una curva de calibración con 12 trials es falsa precisión que un regulador marcaría como engañosa.

---

## E4 — Check-in Cognitivo Go/No-Go: Estado de Disponibilidad

**Veredicto: revise**

**Lo más fuerte:** Es el gate de readiness que protege la validez del resto del módulo (charter §4.3), está correctamente concebido como MEDIDA no entrenamiento (mode != TRAINING, no adapta para subir score), y la Revisión crítica ya blinda los 4 puntos clave (no gamificar, descartar interrupciones, MIN_N, fatiga como covariable).

**Riesgos:**
| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| CRI gamificado → usuario lo manipula, se pierde el gate honesto | high | media | invalida la función de gate de todo el módulo | presentar como semáforo de estado, NUNCA como score a maximizar (P0) |
| RT-CV contaminado por interrupciones del entorno (no cognición) | high | alta | readiness falso | detección de outliers (pausa larga) y descarte, no penalización |
| Bloque corto (60 trials) → RT-CV `confident=false` | med | alta | CRI fabricado con n bajo | MIN_N definido; reportar `null` honesto, nunca CRI inventado |
| Riesgo de claim de "estado mental"/fatiga clínica | high | media | frontera clínica (charter prohíbe TDAH/fatiga patológica) | copy estrictamente "readiness de hoy", jamás inferir estado interior |
| Percibido como "examen" → estrés contamina la medida | med | media | medida sesgada | UX no culpabilizante ("hoy conviene sesión ligera") |

**Debe ser null/preliminary:** **CRI → siempre `preliminary` hasta tener baseline personal** (el doc lo dice; verificar que NUNCA se muestre un número de readiness sin baseline). RT-CV → `null` si n<MIN_N o si hay pausa-outlier detectada. CRI degrada `confident` de la sesión completa de decisiones (correcto: es el detector de fatiga del módulo §4.3).

**P0:** (1) CRI como semáforo, no score; (2) descarte de outliers por interrupción; (3) MIN_N con `null` honesto; (4) copy no-clínico estricto (esto es lente K: "deteccion de fatiga" debe ser "readiness", "estado de disponibilidad" no debe sugerir diagnóstico). **P1:** variante SART. **P2:** doble-CRI pre/post para drift atribuible a la sesión.

**Red Team extremo:** Un regulador ataca el nombre "Estado de Disponibilidad / readiness" si la UX deriva hacia "tu cerebro está cansado hoy" → eso es inferir estado interior/fatiga, prohibido; y un psicometrista muestra que un RT-CV con 60 trials y una interrupción del teléfono produce un CRI "posponer" espurio que mina la confianza del usuario en todo el producto.

---

## E5 — Escáner de Alternativas: Anti-Fijación en la Primera Opción

**Veredicto: revise**

**Lo más fuerte:** Ataca un sesgo real y medible (satisficing/anclaje) con una métrica conductual directa (alternativesScannedRatio) que no depende de auto-reporte. El gate de RT-por-revelación contra "revelar sin leer" es la pieza anti-trampa correcta.

**Riesgos:**
| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| "Dominante objetiva" ambigua con atributos en conflicto → feedback opinable | high | alta | el "óptimo" es discutible, scoring injusto | dominancia de Pareto estricta o suma ponderada con pesos MOSTRADOS (verificable) |
| scanRatio inflable revelando todo sin leer | high | alta | métrica diana corrupta | gate RT-por-revelación (<150ms=no leyó) OBLIGATORIO, no opcional |
| scanRatio alto ≠ bueno (over-scanning con coste = ineficiencia) | med | media | premia perfeccionismo | variante con coste debe penalizar over-scanning, no solo under |
| Atributos textuales → riesgo idioma (T1) | med | media | sesgo cultural | atributos numéricos/icónicos comparables sin lenguaje |
| Mide paciencia/perfeccionismo en vez de calidad de búsqueda | med | media | confound de rasgo | separar exploración eficiente de exhaustividad ciega |

**Debe ser null/preliminary:** Accuracy y scanRatio → `calibrating` hasta n suficiente. ICD agregado → `null` si scanRatio se computa sobre revelaciones-sin-lectura (dataQuality=anomalous). Satisficing index → diagnóstico, no score, hasta estabilidad. Limitar w_acc cuando scanRatio muy bajo (acertar-por-suerte) — bien pensado, instanciarlo.

**P0:** (1) dominante matemáticamente inequívoca (Pareto/suma ponderada con pesos visibles); (2) gate RT-por-revelación obligatorio; (3) variante-coste penaliza over-scanning; (4) atributos numéricos/icónicos no textuales. **P1:** atributos en conflicto (integración multi-atributo). **P2:** opción oculta superior.

**Red Team extremo:** Un científico demuestra que con atributos en conflicto la "opción dominante" es una elección de diseño arbitraria (¿qué peso lleva precio vs calidad?), así que el ejercicio penaliza al usuario por no adivinar la función de utilidad oculta del diseñador — mide lectura de mente, no anti-fijación.

---

## E6 — Detección de Sesgo: Caza-Trampas de Razonamiento

**Veredicto: revise (cercano a block sin las correcciones de claims)**

**Lo más fuerte:** El doble juicio (responder + etiquetar el sesgo) con jointCorrectRate separa entendimiento real de suerte, lo cual es psicométricamente honesto. La propia Revisión crítica reconoce explícitamente la debilidad de la evidencia de debiasing.

**Riesgos:**
| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| **Overclaim de debiasing** (la transferencia de debiasing es notoriamente débil/no replicada) | critical | alta | claim científicamente insostenible (lente K veto) | copy LIMITADO a "reconocimiento del patrón en ESTA tarea"; jamás "te volverás menos sesgado" |
| Ítems textuales (framing, escenarios) = bomba cultural/idioma (T1) | high | alta | mide cultura, no detección | validación de ítems + formato numérico donde sea posible (base-rate, anclaje) |
| Memorización de ítems con repetición | high | alta | mide memoria, no detección | banco grande + excludeRecent OBLIGATORIO |
| "Sesgo correcto" discutible en ítems ambiguos | high | media | etiquetado injusto | un solo sesgo plantado en V1 (inequívoco); compuesto diferido |
| Sin banco validado = trivia de psicología | high | alta | no es entrenamiento | banco validado pre-producción |

**Debe ser null/preliminary:** biasDetectionAccuracy y todas las métricas → `null`/`calibrating` por familia de sesgo hasta n por familia suficiente (5 familias = n efectivo se fragmenta; cuidado con falsa precisión por familia). Perfil de vulnerabilidad por familia → `preliminary` hasta replicación. Nivel de progreso: SOLO nivel 1-2; cualquier insinuación de "menos sesgado" es nivel 5 prohibido.

**P0:** (1) copy honesto: NO debiasing claim (lente K, bloqueante); (2) banco validado + excludeRecent obligatorio; (3) un solo sesgo inequívoco en V1; (4) formato numérico donde sea posible. **P1:** variante solo-detección (aísla metacognición). **P2:** sesgo compuesto.

**Red Team extremo:** Un regulador de claims veta directamente cualquier copy tipo "reduce tus sesgos cognitivos" citando que la literatura de debiasing apenas muestra near-transfer y casi nunca transfer duradero → el ejercicio sería publicidad engañosa; y un científico añade que sin banco validado esto es un quiz de psicología 101 cuyo "progreso" es memorizar 30 viñetas.

---

## E7 — Presupuesto de Decisión: Asignación bajo Restricción

**Veredicto: revise**

**Lo más fuerte:** Optimality ratio contra un solver determinista (knapsack/LP) es feedback objetivamente verificable, y la integración multi-atributo bajo carga WM es un mecanismo cognitivo legítimo y distinto del resto del módulo.

**Riesgos:**
| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| **Solver incorrecto → feedback Y score mienten** (T2) | critical | media | falso progreso con apariencia de rigor matemático | solver testeado exhaustivamente (knapsack exacto n pequeño); MISMA fuente de verdad para feedback y score |
| Drag mide destreza motora (T3) | high | alta | confound motor | steppers discretos en condición de medida primaria; drag solo en variante avanzada |
| CDE confundido con métrica de habilidad | high | media | "afrontó complejidad" ≠ "decidió bien" | CDE describe complejidad, NUNCA es la diana; diana = optimality ratio |
| Fuerza bruta vía reset masivo (mide perseverancia) | med | alta | gaming | limitar resets |
| WM oculta degenera en memoria de listas | med | media | mide memoria, no integración | verificar que la restricción se integre en la decisión, no se recite |
| nItems alto satura WM perceptiva, no cognitiva | med | media | confound de carga perceptiva | controlar legibilidad/layout |

**Debe ser null/preliminary:** Optimality ratio → `calibrating` hasta n suficiente. CDE → es covariable de complejidad, **nunca se reporta como score de habilidad** (riesgo de que el usuario lo lea como "soy bueno"). Constraint violation rate → diagnóstico. Decremento de optimalidad bajo memoryLoad → `preliminary` (capacidad WM efectiva es claim fuerte).

**P0:** (1) solver exacto testeado, fuente única; (2) steppers en medida primaria; (3) CDE jamás como score; (4) límite de resets. **P1:** restricciones ocultas (WM real, verificada). **P2:** reasignación costosa (sunk cost).

**Red Team extremo:** Un ingeniero introduce un off-by-one en el knapsack y el ejercicio le dice al usuario "fallaste el óptimo" cuando acertó → genera frustración y un score falso indistinguible de un fallo real del usuario; sin tests de solver exhaustivos, el ejercicio entero es un oráculo no auditado.

---

## E8 — Resolución bajo Ambigüedad: Decidir con Información Incompleta

**Veredicto: revise**

**Lo más fuerte:** Cierra un hueco real (valor de la información / parada óptima) genuinamente distinto de E1 (E1 mide la actualización; E8 mide CUÁNDO parar de recolectar). El trade-off explorar-vs-decidir es un mecanismo cognitivo de alto valor.

**Riesgos:**
| Riesgo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|
| **VOI óptimo no trivial → bug = stopping error es ruido** (T2) | critical | alta | métrica primaria mentirosa | derivar umbral de cálculo VOI explícito testeado (incl. payoffAsymmetry), no heurística |
| Confunde aversión al riesgo (rasgo) con error | high | alta | penaliza cautela racional | banda óptimo±1 tolerante; copy NO culpa la cautela; con payoff simétrico, sobre-recolectar poco es defendible |
| BUR del posterior final colapsa a 1.0 si prior≈posterior (pocas evidencias) | high | media | infla BUR secundario | excluir esos del BUR |
| Política degenerada (siempre max/nunca) no detectada | med | media | no mide adaptación | correlación compras-vs-incertidumbre; flag si no covaría |
| Solapamiento conceptual con E1 | low | baja | redundancia de módulo | diferenciar explícitamente (ya hecho) |

**Debe ser null/preliminary:** Information stopping error → `null` si el VOI no está validado (dataQuality=anomalous hasta tests verdes del motor). BUR final → excluir priori≈posterior. CAG confianza final → `calibrating`. Net payoff → diagnóstico económico, no score de habilidad por sí solo.

**P0:** (1) VOI óptimo matemáticamente correcto y testeado (incl. asimetría); (2) banda tolerante + copy que no culpa la cautela; (3) excluir BUR priori≈posterior; (4) detección de política degenerada; (5) gate timebase para RT/decisiones<400ms. **P1:** payoff asimétrico (umbral desplazado). **P2:** coste creciente / evidencia ruidosa.

**Red Team extremo:** Un científico demuestra que el "stopping error" mide aversión al riesgo (un rasgo estable de personalidad), no habilidad entrenable → bajo un payoff simétrico, un usuario cauto "sobre-recolecta" racionalmente y el ejercicio lo penaliza como si hubiera errado, midiendo temperamento y vendiéndolo como progreso cognitivo.

---

## Veredicto de módulo

**revise** en los 8 ejercicios. El módulo es el más sofisticado del catálogo y NINGUNO vende humo grosero, pero todos comparten gates de medición sin instanciar (máquina de estados del charter §4.2 incompleta por métrica), confounds no-cognitivos sin neutralizar de forma vinculante, dependencia de bancos de ítems no validados (T1) y de solvers no testeados (T2), y E6 tiene un riesgo de claims CRÍTICO (debiasing). Prioridad de remediación transversal P0: validación de bancos (E1/E3/E6/E8) · tests exhaustivos de motores (E1/E7/E8) · device-gating de RT en todos · copy auditado por K (E4/E6 sobre todo) · exclusión de trials priori≈posterior del BUR (E1/E8).

---


## Emociones (03_7_Emociones.md) — 8 ejercicios EM-A a EM-H

## Auditoria del modulo Emociones — Panel adversarial (Red Team + Claims Auditor con veto + Psicometrista Senior + Director Cientifico)

**Contexto global:** El doc declara fronteras de seguridad correctas (induccion leve-only techo 3, proxies que arman check-in sin inferir estado, scanner whole-word accent/hyphen-insensitive, `null` nunca `0`, `preliminary=true`, `TransferLabel` honesto, assembler como unica autoridad). Esto eleva el piso de todo el modulo. Aun asi, NINGUN ejercicio merece `approve` limpio: todos venden al menos una metrica hoy "catalogada no producida" o dependen de un banco/baseline/convergencia no garantizado. Regla de oro aplicada: si la metrica primaria no es computable sin un gate aun no implementado, el ejercicio es `revise` (no `approve`).

**Riesgo de claims transversal (Claims Auditor, VETO):** El modulo rotula capacidades con nombres semi-clinicos: `FRUSTRATION_TOLERANCE`, `REGULATION_STABILITY`, `EMOTION_THOUGHT_SEPARATION`, `POST_ERROR_RECOVERY`, `CALIBRATION_ACCURACY`. Como simbolos de codigo (`EmotionAxis`/`EmotionCapacity`) son aceptables; PERO en cuanto cualquiera aparezca en copy/UX/onboarding el scanner whole-word NO los bloquea (no contienen lexico prohibido literal) y aun asi comunican una promesa emocional/regulatoria que el ejercicio no entrega. El scanner es necesario pero NO suficiente: falta una allowlist de NOMBRES DE CAPACIDAD para UI separada de los simbolos de codigo. P0 transversal.

---

### EM-A · Freno con Senuelo Afectivo (Stop-on-Affect) — revise
**Lo mas fuerte:** Stop-signal con escalera SSD online es el paradigma de inhibicion mejor validado; la revision ya exige doble gate (convergencia SSD + suelo go-accuracy) y la rama `classifyStop` como deuda real, mas test de paridad afectiva.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| SSRT estimado; sin convergencia se reporta basura con precision falsa | psicometrico | high | med | Score IMPULSIVITY falso | SSRT solo si SSD estable >=K trials Y goAcc>=0.6, si no `unavailable` |
| "No respondas nunca" gamea inhibicion (0% comision trivial) | medicion | high | high | Falso progreso | Suelo go-accuracy que INVALIDA el bloque |
| Nombre IMPULSIVITY roza claim de rasgo | legal_etico | med | high | Implica trait clinico | Renombrar "indice de freno (tarea)" en UI |
| `classifyStop` no existe en assembler (deuda) | tecnico | high | high | No clasifica sin ella | Implementar antes de exponer |
| Timebase SSD no fiable en devices lentos | tecnico | high | med | SSRT invalido | device_unreliable -> no actualiza perfil |

**Red Team extremo:** Un cientifico de SSRT muestra que sin verificar convergencia ni el supuesto race-model tu "IMPULSIVITY" es ruido con dos decimales; un regulador subraya que el nombre sugiere un rasgo clinico que no mides.

---

### EM-B · Reentrada tras Error (Post-Error Re-engagement) — revise
**Lo mas fuerte:** `POST_ERROR_RECOVERY` ya honesta (slowing penaliza solo si la accuracy no se recupero), codificada en EM2, sin renderer nuevo; exclusion de anticipaciones como gate.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Anticipaciones contadas como "error" abren ventanas espurias | medicion | high | high | Metrica contaminada | Error genuino = respuesta-en-ventana incorrecta |
| `harder` plantado puede no dar MIN_POST_ERROR en peor seed | psicometrico | med | high | Bloque no reportable sin avisar | Probar suficiencia por peor seed; si no, `unavailable` |
| Frase de encuadre puede derivar a copy motivacional | legal_etico | low | med | Claim sutil | Pasa scanner; sin culpa ni promesa |

**Red Team extremo:** Un psicometrista demuestra que con 28 trials y densidad baja puedes terminar con 2 pares post-error y un "recovery" sobre n=2, indistinguible de azar, salvo que garantices suficiencia por seed.

---

### EM-C · Separador Estado/Pensamiento/Impulso/Hecho — block
**Lo mas fuerte:** Transferencia ejemplar (`NOT_DEMONSTRATED`), holdout disjunto, accuracy ajustada por azar, matriz de confusion descriptiva.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| "Verdad" del item ambigua sin inter-rater: mide ruido | psicometrico | critical | high | Score sin validez | Inter-rater >=80% o descartar item |
| "Separacion de estado/pensamiento" roza vocabulario terapeutico | legal_etico | high | high | Claim de insight clinico | Copy estrictamente neutro |
| GRANULARITY_INDEX=cat/4 fragil con pocos trials | psicometrico | high | high | Score saltarin | Gated por n por categoria |
| RT-floor constante no escala con longitud | medicion | med | med | Falsos validos | RT-floor proporcional |

**Red Team extremo:** Un cientifico cognitivo pregunta quien decidio que "tengo ganas de irme" es impulso y no estado: sin inter-rater publicado, la clave es la opinion del autor, asi que mides concordancia con un anotador, no una capacidad — pseudo-terapia disfrazada de juego.

---

### EM-D · Espera Calibrada (Tolerance Hold) — block
**Lo mas fuerte:** Reconoce su peligro (`PLACEBO_RISK`), zona TOLERANCE estricta, abandono no penalizado, catch-trials anti tap-spam.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Roza INDUCCION de frustracion (demora prolongada ES carga) | legal_etico | critical | high | Cruza a induccion emocional | Probar que la unica carga es demora, sin contenido afectivo escalado |
| FRUSTRATION_TOLERANCE_INDEX = claim de rasgo vital | legal_etico | critical | high | Promesa de paciencia/autocontrol | Renombrar; copy ultra-conservador |
| Sin baseline el indice no es interpretable | psicometrico | high | high | Score sin referente | NOT computable hasta baseline |
| Abandono mal codificado podria penalizar | legal_etico | high | med | Coerce a usuario en distres | Abandono NUNCA penaliza ni muestra culpa |
| Renderer nuevo + classifyTolerance (deuda) | tecnico | high | high | No ejecutable hoy | Implementar payload ToleranceHold |

**Red Team extremo:** Un eticista/regulador senala que prolongar la espera para medir "tolerancia a la frustracion" ES induccion emocional leve mal disfrazada, y el nombre promete un rasgo de autocontrol vital que ningun dato local sostiene — placebo con riesgo de distres.

---

### EM-E · Calibrador Confianza/Acierto — revise
**Lo mas fuerte:** ECE/Brier es metrica metacognitiva real; confianza POST-respuesta, feedback diferido por bloque (anti-gaming), curva diagonal honesta, capa reusable.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| ECE con celdas vacias / n_k bajo = ruido con precision falsa | psicometrico | high | high | Calibracion falsa | Gated por n_k minimo por nivel |
| n alto -> bloques largos -> fatiga contamina la calibracion | ux | med | high | Fatiga | Limitar trials; readiness-only si fatiga |
| Dos taps/trial rompe loop corto si es pantalla nueva | ux | med | med | Friccion/abandono | Botones pre-render |
| Interpretar over/under-confidence como rasgo | legal_etico | med | med | Claim de sesgo clinico | Mantener descriptivo |

**Red Team extremo:** Un estadistico muestra que un ECE de 3 bins con uno a n=2 es inestable trial-a-trial: tu "calibracion" cambia por azar de muestreo, no por aprendizaje; etiquetar "sobreconfiado" sin n es una afirmacion psicologica injustificada.

---

### EM-F · Etiquetado Rapido de Registro — block
**Lo mas fuerte:** Eje distinto a EM-C (valencia/activacion + presion + IES), anticipaciones excluidas, holdout disjunto, mapeo rota entre sesiones.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Valencia sin inter-rater: verdad es opinion | psicometrico | critical | high | Mide concordancia, no capacidad | Inter-rater obligatorio; descartar items sin acuerdo |
| "Etiquetar emocion" roza claim de inteligencia emocional (prohibido) | legal_etico | critical | high | Claim prohibido explicito | Banco neutro escaneado; nunca "reconocer emociones" |
| Sin banco disjunto colapsa a EM-C | cientifico | high | high | Variante trivial | Banco disjunto + eje valencia |
| RT-floor constante no escala | medicion | med | med | Falsos validos | RT-floor proporcional |

**Red Team extremo:** O el banco de valencia tiene inter-rater publicado (y no es trivial vs EM-C) o no lo tiene (y "agradable/desagradable" es opinion del autor vendida como granularidad afectiva); sumado al riesgo de claim "reconocimiento de emociones", se bloquea hasta evidenciar el banco.

---

### EM-G · Estabilidad bajo Interferencia Afectiva — revise
**Lo mas fuerte:** Conflicto clasico (flanker/Stroop/Simon) bien validado; coste solo con HIT-con-RT en ambas celdas, distractor verificado no-predictivo, daltonico-safe.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| REGULATION_STABILITY en UI = claim de regulacion emocional | legal_etico | critical | high | Frontera violada | UI = "estabilidad de respuesta bajo interferencia" |
| Celda con n<MIN_CELL=5 rellenada con 0 | psicometrico | high | high | Coste falso | Coste unavailable si celda<MIN_CELL; nunca 0 |
| Distractor podria correlacionar con la respuesta | medicion | med | med | Mide saliencia, no control | Test de paridad: distractor no predice respuesta |

**Red Team extremo:** Un regulador captura una pantalla que dice "Regulacion emocional" sobre un test de flechas y te acusa de prometer regulacion afectiva clinica con una tarea de atencion selectiva — el paradigma es solido pero el NOMBRE de la capacidad es el agujero.

---

### EM-H · Mantener-el-Plan bajo Distractor — block
**Lo mas fuerte:** Mantenimiento de regla FIJA con deriva por posicion es constructo distinto a switch (E6) y conflicto (EM-G); transferencia `NOT_DEMONSTRATED`.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| No distingue error-de-regla de error-perceptual: deriva inferida, no medida | medicion | critical | high | Metrica no interpretable | Trial etiqueta si la respuesta corresponde a OTRA regla plausible |
| Accuracy~chance tratada como "deriva" | psicometrico | high | high | Confunde no-aprendizaje con perdida de meta | Accuracy~chance INVALIDA el bloque |
| Slope con pocas posiciones inestable | psicometrico | high | high | Score saltarin | >=K posiciones; unavailable si no |
| REGULATION_STABILITY en UI = claim emocional | legal_etico | high | high | Frontera violada | UI = "estabilidad de aplicacion de regla" |

**Red Team extremo:** Sin etiquetar el tipo de error no puedes saber si la accuracy cae por perdida de meta, dificultad perceptual o fatiga — tu pendiente de deriva es una narrativa, no una medida; un usuario que nunca capto la regla aparece como alguien que "perdio el plan".

---

## Sintesis del panel
- **Approve:** 0/8.
- **Revise:** EM-A, EM-B, EM-E, EM-G (paradigmas validados; bloqueante = gates de computo + renombrar capacidades en UI).
- **Block:** EM-C, EM-D, EM-F, EM-H (verdad del banco sin inter-rater [C,F]; induccion/claim de rasgo [D]; deriva no-medible/error no-etiquetado [H]).
- **P0 transversal:** (1) Allowlist de NOMBRES DE CAPACIDAD para UI separada de simbolos de codigo; (2) toda metrica "catalogada no producida" arranca `unavailable`/`calibrating`, jamas `0`; (3) inter-rater >=80% como precondicion de release para C/F; (4) EM-D requiere prueba de no-induccion antes de cualquier exposicion.

---


## Creatividad (03_8_Creatividad.md) — 8 ejercicios

## Auditoria del modulo Creatividad — Panel adversarial (Red Team Hostil + Claims Auditor con veto + Psicometrista Senior + Director Cientifico)

**Contexto que ayuda a la defensa del modulo:** el doc ya internaliza la regla del Charter §4.6 (sin IA NO se mide originalidad real; solo proxies), fail-closes la IA (`CreativityRuntimePolicy`, G01..G10, `requireLocalOnly()`), declara `aiDerived=false` enforced, y casi todos los ejercicios ya integran una "Revision critica" con correcciones aceptadas. Eso eleva el suelo. Pero el doc es DISENO, no implementacion: el contrato de score §4.2 (state machine con `value:null`, `state`, `nEffective`, `dataQuality`, `invalidatedBy`, `baseline:personal`, `asOf`) NO aparece explicitamente en ningun DTO ni scorer. Todos dicen "Bar null si n<minN" y "preliminary=true" pero NINGUNO especifica `state`, `nEffective`, `dataQuality`, `invalidatedBy`, ni el nivel de progreso (1-5). Esto es deuda P0 transversal que afecta a los 8.

**Hallazgos transversales (aplican a TODOS, no se repiten en cada ficha salvo agravante):**
- **[P0] Contrato de score incompleto:** ningun ejercicio expone la maquina de estados completa del Charter §4.2. "preliminary=true" hardcodeado viola la transicion `insufficient_data→calibrating→preliminary→stable`; preliminary no es un default, es un estado alcanzado tras n_min + rho_min.
- **[P0] Nivel de progreso (1-5) nunca declarado:** §4.1 exige que cada reporte diga en cual de los 5 niveles esta. Ninguna ficha lo hace. Todas las hipotesis de transferencia son "near" (nivel 3-4) pero el score se reporta como practica intra-tarea (nivel 1-2). Riesgo de mezcla.
- **[P0] Fatiga sin contrato de no-actualizacion de perfil:** §4.3 exige `update_long_term_profile=false / update_today_readiness=true` bajo fatiga. Las fichas mencionan COOLDOWN/STEP_DOWN (adaptacion) pero NINGUNA dice explicitamente que la sesion fatigada NO actualiza el perfil estable. Confunden gate de dificultad con gate de medicion.
- **[P0] n_min nunca cuantificado:** todas dicen "n<minN" sin valor. Sin n_min, fiabilidad ni RCI, ninguna metrica puede salir de calibrating. Psicometrista lo bloquea.
- **[P1] Banco de items = punto unico de fallo no auditado:** EX01 (8 objetos), EX02 (6 temas), EX06 (banco de frecuencias). Bancos pequenos + dependientes de locale. Sin holdout obligatorio implementado, "mejora" = familiaridad con banco. Varias revisiones lo exigen pero queda como diseno, no gate.
- **[P1] Heuristicas locales sin IA = forma sobre fondo:** validacion por presencia de tag, conectores relacionales, overlap lexico. Gameable; el propio doc lo reconoce y manda CALIBRATING, pero la honestidad depende de que el `CreativityPracticeLabel` y el copy lleguen al usuario, lo cual no esta verificado en UI.

**Claims Auditor (veto):** el lexico de copy es disciplinado ("practica de...", "rareza respecto a comunes, no originalidad absoluta", "no mide creatividad"). NO encuentro lexico prohibido (IQ/diagnostico/clinico/terapia/TDAH). Riesgo de claims general = LOW-MEDIUM, concentrado en (a) nombres de metrica que suenan a medida real (`noveltyProxy`, `rarityScore`, `meanDomainDistanceSolved`, `flexibilityIndex`) que un usuario lee como "mi originalidad/flexibilidad", y (b) ejercicio 8 cuyo objetivo original "creatividad bajo presion" es un claim cientificamente cuestionable (la presion mata la divergencia) — ya reposicionado a "anti-bloqueo", debe enforzarse.

---

### Ejercicio 1 — Supresion del Primer Impulso (Originalidad por Inhibicion Prepotente)
**Veredicto: revise**

**Lo mas fuerte:** mecanismo solido y bien fundado (inhibicion de respuesta prepotente via banco de "usos prohibidos"); ya saca `noveltyProxy` del score total (correcto, era teatro); revision critica honesta.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| `noveltyProxy` = rareza lexica vs banco que el equipo controla; con 8 objetos el usuario juega al banco (familiaridad), justo lo prohibido | medicion | high | alta | falso progreso por memorizacion | rotacion dura de objetos + holdout obligatorio implementado (gate, no nota); expandir banco; `noveltyProxy` descriptivo CALIBRATING |
| `inhibitionSuccessRate` inflable tecleando basura no-prohibida | cientifico | med | media | metrica nucleo inflada | exigir que cuente solo ideas que pasan trivialidad + elaboracion minima (ya en correccion, falta enforce en formula) |
| Solapamiento lexico no capta sinonimia semantica (sin IA) | tecnico | med | alta | falsos negativos en rechazo trivial | declarar limite; no usar para claim de "diversidad real" |
| Score no expone state/nEffective/dataQuality/invalidatedBy | psicometrico | high | alta | falsa precision | implementar contrato §4.2 |

**Debe ser null/calibrating/preliminary:** `noveltyProxy` (CALIBRATING hasta banco con cobertura declarada); `practiceScore` (null si fluency<n_min); `categorySpread` (preliminary). Nivel de progreso: declarar 1-2 (intra-tarea / vs baseline), nunca 3+.
**P0:** holdout de objetos no vistos implementado; sacar noveltyProxy del score (ya hecho, verificar en scorer); contrato §4.2; declarar nivel de progreso. **P1:** expandir/rotar banco por sesion; enforce trivialidad+elaboracion en inhibitionSuccessRate. **P2:** banco multi-locale.
**Red Team extremo:** un competidor demuestra en 10 min que memorizando las 8 listas de prohibidos sube el score sin generar nada novedoso → "miden familiaridad con vuestro banco, no inhibicion". Un psicometrista pregunta la fiabilidad test-retest de `noveltyProxy`: no hay → la metrica no existe.

---

### Ejercicio 2 — Rotacion Forzada de Categorias (Flexibilidad / Switching Semantico)
**Veredicto: revise**

**Lo mas fuerte:** ya degrada `switchCost` a proxy explicito (no medida ejecutiva), aplica la regla de ambos-lados (`null` si falta lado) coherente con §4.8, e items fuera de banco = `unclassified` que NO penaliza (correcto).

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| `switchCost` ideacional NO es switchCost ejecutivo de RT; mide disponibilidad lexica disfrazada de flexibilidad | cientifico | high | alta | claim de "flexibilidad cognitiva" no sostenido | reportar como proxy ideacional explicito (ya); copy "cambio de enfoque" no "flexibilidad cognitiva"; `withinCategoryDepth` como respaldo |
| Clasificador categoria-lexico falla en frontera → contamina `switchAccuracy` (metrica nucleo) | medicion | high | alta | metrica primaria ruidosa | items pre-clasificados; desconocido=unclassified (ya); reportar dataQuality |
| `flexibilityIndex` suena a rasgo medible | legal_etico | med | media | usuario interpreta como "soy flexible" | renombrar a proxy; copy honesto |
| Sin n_min ni fiabilidad de switchAccuracy | psicometrico | high | alta | sale de calibrating sin base | definir n_min, rho_min |

**Debe ser null/calibrating/preliminary:** `switchCost` (null si falta un lado, ya); `flexibilityIndex` (preliminary, es derivado fragil); `switchAccuracy` (preliminary hasta fiabilidad). Nivel: 1-2.
**P0:** banco categorial pre-clasificado + unclassified no penaliza (verificar enforce); switchCost null sin ambos lados; contrato §4.2; n_min. **P1:** copy/renombre de flexibilityIndex; withinCategoryDepth como respaldo. **P2:** modelado de similitud categorial.
**Red Team extremo:** un cientifico de set-shifting destruye el claim: "vuestro switchCost en segundos sobre generacion lexica no comparte varianza con el switch cost ejecutivo de la literatura; estais re-etiquetando un test de vocabulario". El clasificador frontera produce switchAccuracy con error de medida mayor que la senal → ruido vendido como flexibilidad.

---

### Ejercicio 3 — Sintesis Bajo Restriccion (Combinatoria Forzada)
**Veredicto: revise**

**Lo mas fuerte:** anade `calibrationError` (metacognicion, util y honesto); ya exige tag+ventana de elaboracion (no keyword suelto) y marca calidad semantica CALIBRATING sin IA.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Verificacion por presencia de tag = keyword-stuffing gameable; premia incluir palabra, no resolver | medicion | high | alta | mide lo opuesto a creatividad | cumplimiento = tag + ventana de elaboracion coherente (ya en correccion, falta spec de la ventana); calidad CALIBRATING |
| `constraintConflict` al azar puede hacer trials imposibles | ux | med | media | frustracion, abandono | banco de restricciones con tension pre-modelada y resolubilidad garantizada |
| `constraintSatisfactionRate` se vende como "resuelve restricciones" cuando es presencia+longitud | cientifico | med | media | sobre-claim | declarar que mide presencia+elaboracion, no satisfaccion semantica |
| Score sin contrato §4.2 | psicometrico | high | alta | falsa precision | implementar |

**Debe ser null/calibrating/preliminary:** TODA metrica de calidad semantica = CALIBRATING hasta `isAiSurfaceUnlocked` (ya); `constraintSatisfactionRate` = preliminary y rotulada como presencia+elaboracion; `practiceScore` null si soluciones<n_min. Nivel: 1-2.
**P0:** spec de la "ventana de elaboracion" que valida cumplimiento (sin ella el ejercicio es keyword-stuffing); banco de conflictos resolubles; calidad CALIBRATING; contrato §4.2. **P1:** validar resolubilidad de cada trial. **P2:** IA para satisfaccion real (bloqueada).
**Red Team extremo:** un evaluador escribe la lista literal de tags requeridos como "solucion" y obtiene satisfactionRate=1.0 → "vuestra metrica de combinatoria premia copiar las palabras del enunciado". El doc ya lo sabe; sin la ventana de elaboracion implementada y testeada, es vaporware.

---

### Ejercicio 4 — Analogias Remotas (Conexion de Dominios Distantes)
**Veredicto: block (sin las correcciones P0) / revise (con ellas)**

**Lo mas fuerte:** el doc reconoce que es el mas dependiente de IA y prioriza la variante proporcional A:B::C:? como modo verificable localmente — esa variante SI es honesta.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Sin IA, "puente valido" = estructura, no calidad; un puente sintacticamente correcto pero absurdo pasa | medicion | critical | alta | mide "sabe escribir 'porque ambos...'", no conexion | declarar bridgeAcceptanceRate = ESTRUCTURA, calidad CALIBRATING (ya); priorizar A:B::C:? como modo principal |
| `meanDomainDistanceSolved` es circular: "resuelto"=paso heuristica debil | psicometrico | high | alta | metrica auto-referente sin validez | no usar como medida de "alcance asociativo"; descriptivo CALIBRATING |
| Anti-plantilla debil → degenera en rellenar frase fija | tecnico | high | alta | gaming trivial | anti-plantilla agresivo (gate, no nota) |
| Alta dependencia idiomatica (conectores por locale) | tecnico | med | alta | metrica rota fuera del locale curado | banco de conectores por locale; bloquear locales no soportados |

**Debe ser null/calibrating/preliminary:** `bridgeAcceptanceRate` (mide estructura, declararlo; calidad real CALIBRATING); `meanDomainDistanceSolved` (CALIBRATING/descriptivo, circular); `relationVariety` (preliminary). El modo libre (no proporcional) NO debe puntuar calidad hasta IA. Nivel: 1-2 como mucho, y solo el modo proporcional.
**P0:** la variante proporcional A:B::C:? debe ser el modo de entrada/principal verificable; modo libre = practica sin score de calidad (CALIBRATING); anti-plantilla agresivo implementado; contrato §4.2. **P1:** banco de conectores/pares por locale; declarar `meanDomainDistanceSolved` no-puntuable. **P2:** IA (bloqueada).
**Red Team extremo:** un regulador/cientifico envia "el sol es como un banco porque ambos abren por la manana" (absurdo gramatical) y obtiene aceptado → titular: "la app dice que conectar disparates es creatividad". Es el ejercicio que mas facilmente destruye la credibilidad del modulo si el modo libre puntua sin IA.

---

### Ejercicio 5 — Elaboracion Incremental (Profundizacion Progresiva)
**Veredicto: revise**

**Lo mas fuerte:** entrena la 2a senal real del `CreativityRubricScorer` (elaboration); ya introduce techo anti-verbosidad, `coherenceProxy` con banda (ni copia ni desconexion) y prioriza la variante adversa (mejora/riesgo) verificable.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| `elaboration` = palabras/capa premia verbosidad, no profundidad (sin IA no se distingue) | medicion | high | alta | logorrea puntua como profundidad | techo de palabras que cuentan (ya); minWordsPerLayer con cap superior; declarar limite |
| `coherenceProxy` por overlap lexico es burdo: capa profunda con vocabulario nuevo (overlap bajo) marcada "desconectada" | tecnico | med | alta | falsos rechazos | banda no-monotona como eje (ya), no como bonus; tolerar overlap bajo legitimo |
| `depthReached` inflable encadenando capas largas y vacias | cientifico | med | media | profundidad falsa | novelty lexica intra-cadena como gate (ya en antitrampa); enforce |
| Score sin contrato §4.2 | psicometrico | high | alta | falsa precision | implementar |

**Debe ser null/calibrating/preliminary:** "profundidad real" = CALIBRATING sin IA (solo se mide cantidad+coherencia-proxy); `elaboration` con techo; `practiceScore` null si capas<n_min. Nivel: 1-2.
**P0:** techo de palabras enforced (sin el, mide logorrea); coherenceProxy como eje con banda; contrato §4.2. **P1:** variante adversa (mejora/riesgo con termino negativo/condicional verificable) como modo medible. **P2:** IA para profundidad real.
**Red Team extremo:** un usuario teclea capas de 200 palabras de relleno coherente-pero-vacio y "alcanza profundidad 10" → "vuestra metrica de profundidad es un contador de palabras con maquillaje". Defendible solo si el techo y la banda de coherencia estan implementados y testeados.

---

### Ejercicio 6 — Deteccion de lo Inverosimil (Originalidad por Restriccion Negativa)
**Veredicto: approve (con P0 de contrato/banco)**

**Lo mas fuerte:** es el ejercicio MAS honesto del set sin IA — convierte "originalidad" en un proxy auditable (rareza relativa a banco) y, crucialmente, multiplica `rarityScore * plausibilityRate` para impedir que el absurdo gane. Copy explicito correcto ("rareza respecto a comunes, no originalidad absoluta"). CALIBRATING ante banco insuficiente ya integrado. Plausibilidad = pertenencia a banco PRIORITARIO sobre heuristica.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Banco de frecuencias = punto unico de fallo; frecuencia mal estimada premia rareza falsa | medicion | high | media | proxy sesgado | banco declara tamano/cobertura; `rarityScore` CALIBRATING si cobertura baja (ya); auditar banco |
| Plausibilidad heuristica sin IA: absurdo bien formado pasa | cientifico | med | media | rareza por absurdo | plausibilidad=banco prioritario; heuristica solo da rareza provisional NO puntuable (ya) |
| Variante 3 (anti-banco vivo) infla rareza relativa si histórico corto | psicometrico | med | media | progreso artificial | minimo de histórico antes de activar rareza personal (ya exigido) |
| Score sin maquina de estados explicita | psicometrico | high | alta | falsa precision | implementar §4.2 |

**Debe ser null/calibrating/preliminary:** `rarityScore` = CALIBRATING si cobertura de banco baja (ya); rareza provisional de items fuera-de-banco NO puntuable hasta confirmacion (ya); `practiceScore` null si validas<n_min. Nivel: 1-2, y declarar que rareza es proxy, no originalidad. Copy ya correcto.
**P0:** declarar cobertura/tamano del banco y gate CALIBRATING (ya en diseno, falta en DTO); contrato §4.2; minimo histórico variante 3. **P1:** auditoria/curacion documentada del banco de frecuencias por locale. **P2:** IA para plausibilidad robusta.
**Red Team extremo:** el ataque mas fuerte es al banco: "vuestra 'originalidad' es lo que el curador del banco decidio que es comun" — mitigado porque el copy NO afirma originalidad absoluta y el score es CALIBRATING sin cobertura. Es el unico que sobrevive a un regulador con el copy actual.

---

### Ejercicio 7 — Reencuadre del Problema (Flexibilidad de Marco / Problem-Finding)
**Veredicto: revise**

**Lo mas fuerte:** problem-finding es genuinamente creativo e infravalorado; `calibrationError` (metacognicion) como senal central honesta; ya prioriza la variante "escala" (individual/grupo/sistema) como modo verificable y exige onboarding.

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Deteccion de "dimension de marco cambiada" por marcadores lexicos es casi adivinanza; `frameDiversity`="uso vocabulario de N categorias" | medicion | high | alta | metrica nucleo = proxy debil | declarar frameDiversity como proxy por marcadores; calidad CALIBRATING; escala como modo entrada |
| Tarea conceptualmente dificil → mide comprension-de-instrucciones, no creatividad | ux | high | alta | confound mayor en early sessions | onboarding/ejemplo obligatorio (ya); descartar primeras sesiones del baseline (warm-up) |
| Reformulacion = parafrasis del original pasa si usa vocabulario de otra dimension | tecnico | med | alta | falso positivo | overlap alto con enunciado = rechazo (ya); enforce ortogonalidad |
| Score sin contrato §4.2 + sin n_min | psicometrico | high | alta | falsa precision | implementar |

**Debe ser null/calibrating/preliminary:** `frameDiversity` (proxy por marcadores, CALIBRATING para "calidad de reencuadre"); primeras N sesiones = warm-up no-baseline (curva de aprendizaje); `practiceScore` null si validas<n_min. Nivel: 1-2.
**P0:** onboarding obligatorio enforced; frameDiversity declarado proxy + calidad CALIBRATING; warm-up excluido del baseline; contrato §4.2. **P1:** variante escala como modo de entrada; ortogonalidad enforced. **P2:** IA para deteccion de marco real.
**Red Team extremo:** un cientifico nota que la curva de aprendizaje conceptual contamina el baseline ("la 'mejora' es que por fin entendieron la instruccion, no mas flexibilidad de marco"). Sin warm-up excluido y onboarding, es Reliable Change espurio.

---

### Ejercicio 8 — Improvisacion Restringida en Tiempo Real (Fluidez Bajo Presion)
**Veredicto: block (sin P0) / revise (con P0 completos)**

**Lo mas fuerte:** ya integra el contrato de presion leve-only (techo HARD=3, `pressureCueIntensity` nunca auto-sube, check-in, exit visible, espejo `OverloadProxy`/`DefaultEmotionAdaptiveEngine`), y la revision reposiciona el objetivo de "creatividad bajo presion" (claim falso) a "reduccion de bloqueo/auto-censura" (honesto).

| Riesgo | Tipo | Sev | Prob | Impacto | Fix |
|---|---|---|---|---|---|
| Mide velocidad de tecleo, no fluidez ideacional: teclado rapido gana, lento "bloquea" por motricidad — exactamente "familiaridad con UI", prohibido | medicion | critical | alta | confound motor invalida la metrica nucleo | normalizar IBI por baseline de tecleo (mini-calibrador no-creativo) — SIN esto, descartado |
| Objetivo "creatividad bajo presion" cientificamente cuestionable (presion mata divergencia, lit. incubacion) | cientifico | high | alta | claim falso | reposicionar a "anti-bloqueo" (ya); enforce en copy y en nombre |
| Presion temporal en poblacion vulnerable / induccion de frustracion | legal_etico | high | media | dano emocional leve | leve-only techo 3 + check-in + opt-out (ya); §4.7 enforce |
| `validThroughput`/`rhythmStability` se leen como medida de fluidez creativa | claims | med | media | sobre-interpretacion | copy: ritmo de generacion, no creatividad |
| IBI device-dependent sin timebase confiable | tecnico | high | alta | UNTRUSTED_TIMEBASE | reusar SpeedTimebase (Choreographer); sin el → device_unreliable, no actualiza perfil (§4.4) |

**Debe ser null/calibrating/preliminary:** `validThroughput`/`rhythmStability` = null/device_unreliable sin timebase confiable Y sin calibrador motor; con timebase pero sin baseline de tecleo → CALIBRATING (confound motor no descontado); `practiceScore` null si validas<n_min. Nivel: 1-2, y reposicionado a anti-bloqueo. **invalidatedBy** debe incluir UNTRUSTED_TIMEBASE y HIGH_FATIGUE.
**P0:** calibrador de tecleo para normalizar IBI (sin el = block); objetivo reposicionado a anti-bloqueo enforced en copy; presion leve-only techo 3 + check-in (ya, verificar); timebase confiable o device_unreliable; contrato §4.2 con UNTRUSTED_TIMEBASE. **P1:** variante sprint-recovery como modo defendible (mide recuperacion, no pico). **P2:** modelado fino de carga.
**Red Team extremo:** un competidor publica que dos usuarios con identica creatividad obtienen scores opuestos solo por velocidad de teclado → "es un test de mecanografia con nombre de creatividad". Un cientifico cita la literatura de incubacion: la presion REDUCE la divergencia, asi que un score que sube bajo presion mide conformidad/obviedad, no creatividad. Es el ejercicio mas atacable del set; solo el calibrador motor + reposicionamiento lo salvan.

---

## Resumen de veredictos
- **approve:** E6 (con P0 de contrato/banco) — el unico que sobrevive a un regulador con su copy actual.
- **revise:** E1, E2, E3, E5, E7 — mecanismos validos pero score sin contrato §4.2, niveles de progreso sin declarar, y proxies que necesitan rotulado honesto + enforce de gates ya disenados.
- **block hasta P0:** E4 (modo libre puntua estructura como si fuera calidad → absurdos pasan) y E8 (confound motor invalida la metrica; calibrador de tecleo es condicion necesaria).

Ningun ejercicio es approve "limpio": el contrato de score honesto §4.2 (state/nEffective/dataQuality/invalidatedBy/baseline/asOf) y la declaracion de nivel de progreso (1-5) son deuda P0 transversal en los 8.

---


# Anexo — barrido de claims

| Módulo | Ejercicio | Ubicación | Problema | Sev | Fix |
|---|---|---|---|---|---|
| Enfoque | Conmutador de Reglas (Task-switching) | Hipótesis de transferencia: 'NO: afirmar "mejor multitasking" real (la multitarea es mito)' | El texto invoca el lexema 'multitasking'/'multitarea' para desmentirlo, pero el charter prohíbe el término aun en negación/compuesto. Además 'la multitarea es mito' es una aseveración científica categórica fuera de scope de un doc de diseño de ejercicio. Riesgo de que copy derivado herede el término. | low | Reescribir sin el lexema prohibido: 'NO afirmar transferencia a hacer varias cosas a la vez en la vida real ni a adaptabilidad general; el ejercicio entrena reconfiguración de regla contra tu baseline.' |
| Enfoque | Doble Vía Conflictiva (Dual-task) | Objetivo cognitivo y Hipótesis de transferencia: 'NO entrena "multitarea"... NO: afirmar capacidad de multitarea general (mito)... Copy debe desmentir el mito de multitarea' | Uso repetido del término prohibido 'multitarea/multitasking' (incl. instrucción 'Copy debe desmentir el mito de multitarea'). Aunque la intención es honesta, el charter exige no introducir el lexema ni siquiera para negarlo, porque el copy de usuario podría reproducirlo. | med | Sustituir por: 'Este ejercicio NO entrena la capacidad de atender dos tareas a la vez como habilidad general; solo mide y reduce el coste de coordinación (DUAL_TASK_COST) contra tu baseline. El copy de usuario debe describir "coste de hacer dos cosas al tiempo", sin afirmar mejora de coordinación general.' |
| Enfoque | Seguimiento Múltiple (MOT) | Hipótesis de transferencia: 'interés en deportes/conducción documentado pero NO garantizable' y Variables/copy de conducción | Mencionar 'deportes/conducción documentado' insinúa transferencia lejana a dominios reales aunque se matice con 'no garantizable'. Es far transfer sin evidencia presentado de forma sugerente; el propio Revisor exige 'copy honesto sin claims de conducción'. | med | Eliminar la insinuación de dominio real: 'NO se afirma transferencia a conducción, deporte ni atención general (far transfer no demostrado). Solo near transfer a tareas de tracking del propio paradigma contra tu baseline.' No citar 'documentado' para conducción/deporte. |
| Velocidad | Sustitución Símbolo-Dígito (Cifrado Veloz) | Hipótesis de transferencia: 'NO afirmar: que el DSST "predice inteligencia"... no es marcador clínico aunque el DSST se use en investigación' | Introduce el marco 'predice inteligencia' y 'marcador clínico' (lexemas próximos a IQ/clínico) para negarlos, pero el encuadre acopla el ejercicio al constructo de inteligencia. Riesgo de que el lector/copy asocie DSST con medición de inteligencia. | low | Reescribir sin acoplar a inteligencia: 'NO afirmar relación con capacidad cognitiva global ni valor clínico/diagnóstico. Es práctica de throughput perceptivo-asociativo contra tu baseline personal.' |
| Velocidad | Discriminación Cromática Binaria (Tinte Crítico) | Hipótesis de transferencia: 'NO se debe afirmar: mejora de "velocidad mental general"' | 'velocidad mental general' es lenguaje de capacidad absoluta/global; aunque va en cláusula NO, conviene que ningún ejercicio nombre 'velocidad mental general' como constructo aspiracional. El nombre del módulo es 'Velocidad' pero el copy debe anclar a baseline, no a una capacidad mental general. | low | Mantener la negación pero reforzar baseline: 'NO se afirma mejora de la velocidad de pensar en general ni transferencia a tareas semánticas; solo near transfer a decisiones perceptivas binarias, medido contra tu propio baseline.' |
| Logica | Cadena Deductiva Progresiva | Hipótesis de transferencia: 'NO se debe afirmar: mejora de "inteligencia logica" general' | El término 'inteligencia lógica' (capacidad global, próximo a IQ) aparece como constructo, aun en negación. El charter prefiere 'razonamiento funcional' / 'perfil cognitivo funcional', no 'inteligencia X'. | low | Reemplazar por: 'NO se afirma mejora del razonamiento general ni far transfer a matemáticas/código. Copy: "encadenar inferencias válidas contra tu baseline".' (eliminar 'inteligencia lógica'). |
| Logica | Inferencia Bayesiana Cualitativa | Objetivo/Copy y Revisión crítica: riesgo de presentarse como 'test de racionalidad' / 'debiasing' | El ejercicio roza el claim de 'racionalidad'/'reducción de sesgos en la vida real' (far transfer / pseudociencia). El propio doc lo marca como alto riesgo. Debe quedar prohibido cualquier copy de 'te vuelve más racional' o 'reduce tus sesgos'. | med | Fijar copy permitido y prohibido explícitamente: PERMITIDO 'práctica de actualización cualitativa de creencias contra tu baseline'. PROHIBIDO 'racionalidad', 'debiasing', 'reduce sesgos', 'piensas mejor'. Pasar copy por review-cognitive-claims antes de exponer. |
| Logica | Puente de Analogia Estructural | Hipótesis de transferencia: 'NO: "creatividad" general, CI verbal' | Cita 'CI verbal' como constructo a negar; mejor evitar el lexema CI por completo (el charter prohíbe IQ/CI incluso en compuestos). También 'creatividad general' como capacidad absoluta. | low | Reescribir: 'NO se afirma mejora de la creatividad ni de ninguna capacidad verbal global, ni far transfer. Copy: "mapear relaciones estructurales contra tu baseline".' (eliminar 'CI verbal'). |
| Lenguaje | Source Triage (Triaje de Fuentes) | Riesgos: 'no debe leerse como "te enseña a detectar fake news"' y Hipótesis: 'NO: alfabetización mediática real, detección de desinformación en el mundo' | El doc reconoce el riesgo, pero el encuadre (triaje de fuentes, credibilidad, contradicción) puede derivar en copy que prometa 'detectar desinformación/fake news', que es far transfer no soportado. Necesita prohibición de copy explícita, no solo un riesgo anotado. | med | Añadir regla de copy dura: PROHIBIDO 'fake news', 'desinformación', 'alfabetización mediática', 'pensamiento crítico'. PERMITIDO 'práctica de aplicar criterios estructurales de relevancia/contradicción dados en el ítem, contra tu baseline'. El feedback nunca debe sugerir capacidad de juzgar credibilidad real. |
| Lenguaje | Lexical Access Sprint | Hipótesis de transferencia: 'NO: vocabulario más amplio... ni velocidad lectora global' | Negaciones correctas, pero 'velocidad lectora global' y el solapamiento con 'velocidad de proceso' pueden inducir copy de capacidad global. El Revisor ya exige mapear a dimensión verbal, no speed; el copy debe no prometer 'leerás más rápido'. | low | Reforzar: 'NO se afirma que leerás más rápido, que sabrás más palabras, ni mejora de velocidad de pensar; solo near transfer a la recuperación léxica del propio paradigma, contra tu baseline.' |
| Decisiones | E1 Bayes Arena / E6 Detección de Sesgo / E8 Resolución bajo Ambigüedad | E6 Hipótesis: 'la literatura de debiasing es debil; solo near transfer'; E6 Revisión: 'jamas "te volveras menos sesgado"'; E1/E8 razonamiento bayesiano | Conjunto de ejercicios con alto riesgo de claim de 'debiasing'/'tomarás mejores decisiones'. El doc lo acota, pero falta una regla de copy unificada: ninguno puede prometer mejores decisiones reales (financieras/médicas/vitales) ni reducción de sesgos en la vida. | med | Regla transversal de Decisiones en el copy: PROHIBIDO 'tomarás mejores decisiones', 'menos sesgado', 'más racional', 'mejor juicio' (vida real). PERMITIDO 'práctica de actualización/clasificación/triaje contra tu baseline; near transfer a tareas isomorfas no garantizado'. Auditar E1/E6/E8 con review-cognitive-claims. |
| Decisiones | E3 Forecast Calibrado / E2 Triaje de Reversibilidad | E3 'Apuesta tu Confianza', E3 variante 'Mercado de prediccion personal... fichas apostadas', E2 'Puerta de Una Via vs Dos Vias' | Léxico de apuesta ('apuesta', 'fichas apostadas', 'payoff', 'mercado de predicción') roza el lexicón de gambling que el charter restringe en otros módulos (Emociones/Creatividad lo prohíben explícitamente: 'sin gambling lexicon'). Inconsistencia: Decisiones usa 'apuesta/fichas' libremente. | med | Sustituir el marco de apuesta por calibración neutra: 'Forecast Calibrado: Declara tu Confianza'; variante 'asignación de confianza con puntuación propia (proper score)', no 'fichas apostadas'/'mercado'. Evitar 'apuesta', 'apostar', 'payoff' en copy de usuario; usar 'puntos de confianza' / 'recompensa por calibración'. |
| Emociones | EM-C Separador (Granularity Quad) | Hipótesis: 'NO afirmar: insight terapeutico, regulacion emocional clinica, ni que "separar pensamientos cure ansiedad"' | Mencionar 'cure ansiedad'/'insight terapéutico' (aun negándolo) acopla el ejercicio a un claim clínico/terapéutico. El charter prohíbe diagnóstico/terapia; nombrar 'ansiedad' como diana a negar es riesgoso si se filtra a copy. | low | Reescribir sin nombrar condiciones: 'NO se afirma beneficio terapéutico, regulación emocional clínica ni efecto sobre ningún estado de salud. Es práctica de separación metacognitiva de registros, contra tu baseline.' (el doc ya marca TransferLabel.NOT_DEMONSTRATED — mantenerlo). |
| Emociones | EM-D Espera Calibrada (Tolerance Hold) | Variante 3: 'esperar para mayor recompensa-puntos vs tap ya para menos — coste/beneficio, sin gambling lexicon' | Aunque anota 'sin gambling lexicon', la mecánica de 'esperar por mayor recompensa' es un paradigma de descuento por demora cercano a apuesta; si el copy usa 'recompensa/premio' puede derivar. Bajo riesgo porque ya está acotado, pero PLACEBO_RISK alto exige copy ultra-conservador. | low | Mantener la nota y reforzar: usar 'puntos' neutros, nunca 'premio/recompensa/apuesta'; el copy debe decir 'práctica de espera funcional contra tu baseline', nunca 'entrena tu paciencia/autocontrol'. (Coherente con TransferLabel.PLACEBO_RISK ya declarado.) |
| Emociones | EM-G Estabilidad bajo Interferencia / EM-H Mantener-el-Plan | EM-G 'REGULATION_STABILITY' y nombre 'Estabilidad regulatoria'; EM-H 'REGULATION_STABILITY aplicada a persistencia de meta' | El nombre de métrica 'REGULATION_STABILITY' puede leerse como 'regulación emocional' (claim clínico/afectivo). El doc ya exige llamarlo 'estabilidad de respuesta' en UI, pero la etiqueta interna sigue sugiriendo regulación emocional. | low | Confirmar en copy/UI: SIEMPRE 'estabilidad de respuesta bajo interferencia' / 'estabilidad de aplicación de regla'; NUNCA 'regulación emocional' ni 'autorregulación'. Mantener el gate del EmotionTerminologyScanner sobre la etiqueta visible. |
| Creatividad | Transversal (1–8) — proxy de originalidad | Nota transversal y por-ejercicio: 'noveltyProxy'/'rarityScore' como proxy de originalidad | Riesgo de falsa precisión / 'mide tu originalidad' cuando en realidad es rareza léxica relativa a un banco (proxy). El doc ya lo declara honestamente ('proxy de práctica, no medida de originalidad real'), pero el riesgo es que la UI muestre un score de 'originalidad' con apariencia de medida absoluta. | med | Regla de copy dura: la UI NUNCA debe etiquetar la barra como 'Originalidad' a secas; usar 'Rareza respecto a respuestas comunes (proxy de práctica)'. Cualquier número de novelty/rarity sin banco suficiente = CALIBRATING/em-dash, nunca un valor con decimales presentado como medida de originalidad. |
| Creatividad | Transversal (1–8) | Cada 'Hipótesis de transferencia': 'NO: aumento de "creatividad general", innovación en dominios reales' | Repetido encuadre de 'creatividad general' / 'innovación real' como capacidad absoluta a negar. Correcto que se niegue, pero conviene una regla de copy única para que ningún ejercicio prometa 'serás más creativo'. | low | Regla transversal: PROHIBIDO 'serás más creativo', 'creatividad general', 'innovación', 'pensamiento lateral' como resultado. PERMITIDO 'práctica de fluidez/flexibilidad/elaboración/rareza contra tu baseline; near transfer al propio paradigma no garantizado'. |
| Creatividad | 8. Improvisación Restringida en Tiempo Real | Objetivo original 'creatividad bajo presión' y Revisión crítica (e): 'reposicionar... NO creatividad bajo presión sino reducción de bloqueo' | El título y objetivo iniciales ('Fluidez Bajo Presión', 'creatividad bajo presión') prometen un constructo (creatividad bajo presión) que la propia revisión declara no defendible; además mide en parte destreza de tecleo (familiaridad con UI). Si no se reposiciona en el copy final, es claim no soportado. | med | Aplicar la corrección ya aceptada en el copy de usuario: reposicionar a 'práctica de reducción de bloqueo/auto-censura ideacional contra tu baseline', NUNCA 'creatividad bajo presión'. Normalizar IBI por baseline motora antes de reportar throughput. |

**Patrones sistémicos de claims:**
- Negación-que-introduce-el-lexema: el catálogo es honesto, pero repetidamente nombra términos prohibidos (multitarea, inteligencia/CI, racionalidad, debiasing, fake news, regulación emocional, curar ansiedad) dentro de cláusulas 'NO afirmar'. El riesgo es que el copy de usuario o el marketing hereden el lexema. Recomendación: una capa de copy de usuario que jamás reproduzca el término prohibido, ni para negarlo; las negaciones viven solo en el doc de diseño, nunca en UI.
- Far transfer sugerido por dominio real: varios ejercicios insinúan transferencia a dominios de la vida (conducción/deporte en MOT; decisiones médicas/financieras en Bayes/Decisiones; fake news en Source Triage) con un matiz de 'no garantizado'. La insinuación es en sí un riesgo de claim. Regla: no nombrar dominios reales de aplicación ni siquiera como ejemplo aspiracional.
- Lexicón de apuesta inconsistente entre módulos: Emociones y Creatividad prohíben explícitamente 'gambling lexicon', pero Decisiones (E3, E2) usa 'apuesta', 'fichas apostadas', 'mercado de predicción', 'payoff' con libertad. Unificar: ningún módulo usa léxico de apuesta en copy de usuario; sustituir por 'puntos de confianza'/'puntuación por calibración'.
- Capacidad absoluta vs baseline personal: persisten constructos globales ('velocidad mental general', 'inteligencia lógica', 'creatividad general', 'regulación emocional') aun en negación. El charter exige anclar SIEMPRE a 'contra tu baseline personal' y al paradigma concreto; el copy final debe evitar nombrar la capacidad global incluso para descartarla.
- Proxy presentado como medida (falsa precisión): originalidad (Creatividad), 'inteligencia'/d-prime/scores con decimales. El catálogo gestiona esto bien con CALIBRATING/null/em-dash y etiquetas de proxy, pero el punto único de fallo es la UI: cualquier barra con decimales sin IC o sin banco suficiente debe ser em-dash, y las etiquetas deben decir 'proxy de práctica', nunca el nombre del constructo absoluto.
- Fortaleza a preservar: el catálogo ya incorpora gates fuertes (preliminary=true, null nunca 0, TransferLabel NOT_DEMONSTRATED/PLACEBO_RISK, EmotionTerminologyScanner whole-word/accent/hyphen-insensitive, review-cognitive-claims). Las violaciones encontradas son mayormente de redacción/copy y de consistencia inter-módulo, no de mecánica de scoring; ninguna alcanza severidad critical.
