# 02 · Decision de producto — Catalogo completo ejercicio por ejercicio

> Fuentes: `AUDIT_V2_PRODUCTO.md` (tabla §2, redundancias, claims), `AUDIT_V2_GROUND_TRUTH.md` (evidencia
> `archivo:linea`). Veredicto **GO = APPROVE**. Otros: REVISE, BLOCK, MERGE, REBUILD, RETIRE.
> Columna "Perfil": `no` / `si` / `solo-calibrando` = si puede actualizar el perfil cognitivo tras sus gates.
> Invariantes que gobiernan toda la tabla (ver `00_README`/`01_VEREDICTO`): (a) `null != 0`; (b) Enfoque/Velocidad
> no tocan perfil sin timebase autoritativa + jank-gate medido; (c) cero claims far-transfer/clinico/IQ/
> fake-news/regulacion, ni para negarlos; (d) anti-gaming solo cuenta si implementado+testeado con sinteticos;
> (e) banco pequeño/no-validado bloquea perfil.

## Leyenda de prioridad

P0 = bloqueador (infra o rebuild de runtime) · P1 = v1 defendible (tras P0) · P2 = experimental/rediseño ·
P3 = diferido/retirado.

## ENFOQUE (`focus/`) — 13 ejercicios

| Ejercicio | Veredicto | Prio | Perfil | Riesgo principal | Accion exacta |
|---|---|---|---|---|---|
| E1 Vigia (PVT) | REVISE | P1 | no | Tap ritmico infla indice; detector de cadencia ausente (`hecho`: cadencia solo en arena) | Detector de cadencia testeado + jitter≥150ms + slope null si n_targets<20 + UNTRUSTED no toca perfil |
| E2 Filtro (Flanker) | BLOCK | P1 | no | Efecto ~40ms < jitter de frame; sin balanceo de lado | Timebase autoritativa o perfil congelado + min-n≥20/celda + balanceo LEFT/RIGHT + IES |
| E3 Gatillo (Go/No-Go) | REVISE | P2 | solo-calibrando | "Responder a todo" sube hit-rate | INHIBITORY_INDEX null-honesto; FA-alto+omision~0 → Contaminated; n≥20 |
| E4 Freno (SSRT) | REVISE | P1 | no | Waiting-strategy infla SSRT (`hecho`: SSRT trustworthy existe) | Detector waiting-strategy testeado + n_stops≥30 + stop multicanal + timebase |
| E5 Conflicto (Stroop) | MERGE | P1 | no | Misma varianza y motor que E2 (`hecho`: mismo CONGRUENCY/`DefaultMetricsEngine`) | Probar ortogonalidad r(E2,E5)<umbral o **MERGE en E2** |
| E6 Cambio (switch) | REVISE | P1 | no | CSI largo → precomputacion; perseveracion no aislada | Registrar previousRule + min-n≥20 + cue dedicado + switchCost null testeado |
| E7 Rastreo (busqueda) | REVISE | P2 | no | "Ausente sin buscar" aplana la pendiente | ≥3 set sizes + R² umbral + presente/ausente separadas + gate AbsentNoSearch |
| E8 Duo (dual-task) | REBUILD | P0 | no | No genera 2ª tarea; el "coste" mide ruido (`hecho`: flag cosmetico) | Capturar accuracy secundaria real + taskBalance; si runtime 1-respuesta no lo soporta → **RETIRE** |
| E9 Enjambre (MOT) | REVISE | P1 | no | Frame-drop sin medicion (`hecho`: hardcoded 0.0) | Medicion real de frame → Contaminated + sonda 50/50 + min-n/nivel + copy sin conduccion/deporte |
| E10 Flujo (RSVP) | BLOCK | P1 | no | SOA por corrutina, no vsync | SOA por frameTimeNanos + gate jank + sin invocar attentional-blink hasta T1-T2 |
| E11 Ancla (distraccion) | REVISE | P2 | solo-calibrando | Copy "te distraes menos con el movil" | min-n≥20 + balanceo baseline/intrusion + copy auditado como proxy |
| E12 Re-enganche | REVISE | P1 | no | Mide tedio, no reenganche | reengagementLatency null con <N lapsos + desenganche separado (Contaminated) |
| E13 Presion (deadline) | REVISE | P2 | solo-calibrando | Copy roza estres laboral (frontera salud) | min-n≥20 + deadline por frame + copy sin transferencia laboral + delta con IC |

## VELOCIDAD (`speed/`) — 8 ejercicios

| Ejercicio | Veredicto | Prio | Perfil | Riesgo principal | Accion exacta |
|---|---|---|---|---|---|
| E1 Tinte Critico (2-AFC) | REVISE | P1 | solo-calibrando | Sin jank-gate mide GPU+pantalla; ΔE no comparable cross-device | Jank-gate vivo + Android autoritativo + axisRotation≥30% + anclaje device-local + sintetico adversario + variante daltonismo-safe |
| E2 Cifrado Veloz (DSST) | REVISE | P2 | solo-calibrando | keySize bajo degenera en busqueda visual | Pendiente RT/keySize como primaria + keySize≤6 + legendVisibleMs + jank-gate |
| E3 Multivia (switch) | BLOCK | P3 | no | SWITCH_COST no existe en engine; redundante con E6 | Implementar SWITCH_COST + ortogonalidad o **MERGE en E6** |
| E4 Compuerta (Go/No-Go) | REVISE | P1 | solo-calibrando | Gate de comision no testeado = impulsividad premiada | Gate de comision prioritario testeado con sintetico impulsivo + faRate>0.30 en techo + jank-gate |
| E5 Cadencia (timing) | BLOCK | P3 | no | RT<0 LANZA `computeRtNanos`; TimingPolicy con signo inexistente | TimingPolicy con signo + cancelar latencia tactil device-local o **RETIRE** |
| E6 Flanker Express | REVISE | P1 | solo-calibrando | CONGRUENCY_EFFECT no existe en engine de speed | Implementar CONGRUENCY + ortogonalidad vs switch/comision + estimulos no-linguisticos + jank-gate |
| E7 RSVP Veloz | BLOCK | P3 | no | Jank-gate muerto; SOA es ficcion | Onset real por frameTimeNanos + gate jank o **RETIRE** |
| E8 Apuesta Temporal | BLOCK | P3 | no | Mezcla velocidad+metacognicion; "cobardia premiada"; lexico apuesta | Sacar de Velocidad + metacog separado del score o **RETIRE** |

## LENGUAJE (`language/`) — 6 ejercicios + sesion mixta + 2 inexistentes

| Ejercicio | Veredicto | Prio | Perfil | Riesgo principal | Accion exacta |
|---|---|---|---|---|---|
| E1 Evidence Lock | REVISE | P1 | no | Banco 6 fijos; HIGH_PRIOR nunca se eleva | Banco≥40 + exposure/holdout + selectNext+NearTransferVerifier wired + min-n≥12 |
| E2 Semantic Surgery | REVISE | P2 | no | Variante-bias (equivalente = mas corta) | Banco≥50 balanceado en 5 changeType + decision sobre localizacion |
| E3 Compression Ladder | REVISE | P2 | no | candidateCount=3 baja resolucion; drag = confound motor | candidateCount≥4 + banco≥30 balanceado + claim "reconocimiento de fidelidad" |
| E4 Argument X-Ray | REBUILD | P1 | no | Correcta="b" en 6/6 (posicional explotable) (`hecho`) | Balancear posicion + banco≥40 + delimitar vs Logica; o rebuild a labeling (inter-rater≥80%) |
| E5 Scope & Negation | REBUILD | P1 | no | Triple solape con E1 y Logica; sin items "cero validas" | Cuota≥20% sin-inferencia + banco≥50 (≥8/operador) + ortogonalidad vs E1/Logica |
| E6 Source Triage | BLOCK | P0 | no | Claim "fake news" = bomba regulatoria; sin scanner Lenguaje | Copy-scanner en `check` + decoy bidireccional + sourceCount≥4 + claim "criterios estructurales dados"; o **RETIRE** |
| Sesion mixta | BLOCK | P3 | no | `items.first()` = maxima memorizacion; puede filtrar al perfil | isStrongUpdateAllowed=false siempre + rotar item no-visto + test que nunca eleve trait |
| E7 Lexical Access | RETIRE | P3 | no | No existe en codigo; RT sin timebase = ruido; duplica Speed | **RETIRE** |
| E8 Calibrated Comp | RETIRE | P3 | no | No existe; Brier de sesion = ruido | **RETIRE** o MERGE como capa metacog opcional |

## MEMORIA (`memory/`) — 8 ejercicios + n-back + transfer probe

| Ejercicio | Veredicto | Prio | Perfil | Riesgo principal | Accion exacta |
|---|---|---|---|---|---|
| EX1 Span Dual (complex-span) | REVISE | P1 | si | Confound aritmetico; sin sonda UNTRAINED = familiaridad | Staircase solo trials validos + RT fuera del perfil + reading-span rotatorio + claim ligado a UNTRAINED |
| EX_NBACK | BLOCK | P2 | no | No existe; peor reputacion far-transfer; over-claim Gf | Implementar d'+criterio + copy-scanner prohibe IQ/Gf + UNTRAINED; sin d' honesto → **RETIRE** |
| EX2 Updating Ranura | REVISE | P1 | si | Sin generador; solape perceptual con n-back | Generador con fillers + sonda 1-ranura + intrusion/binding separados |
| EX5 Grid Espacial | REVISE | P1 | si | Verbalizacion = memoria verbal disfrazada | Cowan's K + d' + objetos baja-verbalizabilidad + location/binding/swap separados |
| EX6 Deteccion Cambio | MERGE | P2 | si | Misma varianza que EX5 | Demostrar r<0.7 con EX5 o queda como modo de EX5 |
| EX4 Pares A-B/A-C | REVISE | P1 | si | Fase A-C y demora 24h no cableadas | Cablear A-C + rotacion estricta + demora 24h anclada a `EpochClock` |
| EX8 Nombres-Rostros | MERGE | P3 | si | Caso de EX4; sesgo cultural fuerte | Banco diverso licenciado o **MERGE en EX4** |
| EX3 Episodica Diferida | REVISE | P1 | si | Sin distractor mide recencia; diferido no cableado | Distractor pre-inmediato + probe +24h + FalseImprovement |
| EX7 Orden Serial | REVISE | P2 | si | Sin generador; colapsa en "otro span" | Scoring por posicion + disociar item/orden |
| EX_TRANSFER_PROBE | BLOCK | P0 | solo-calibrando | Corazon de honestidad NO cableado (`hecho`: RCI sin caller, particion no servida) | Formas paralelas equiparadas + RCI gated por reliability + scheduler real |

## DECISIONES (`decisiones/` + `mindverse.mindboost.core`) — 8 ejercicios

| Ejercicio | Veredicto | Prio | Perfil | Riesgo principal | Accion exacta |
|---|---|---|---|---|---|
| E1 Bayes Arena | REVISE | P1 | solo-calibrando | priori=posterior cuenta BUR=1.0 (infla); AGRS espacial neutralizado (`hecho`: spatialEntropy=1.0) | Excluir priori=posterior del BUR + isDialDefault/dialPathEntropy reales al AGRS + min-n=12 |
| E2 Triaje Reversibilidad | REVISE | P1 | si | AGRS swipe neutralizado; marcadores no rotan | Entropia de swipe al AGRS + min-n=10/tipo + rotacion cross-sesion + RT<400ms invalida |
| E4 Check-in Go/No-Go | REVISE | P0 | solo-calibrando | RT-CV (30%) sobre epoch = falsa precision | RT-CV no-confiable hasta timebase + descarte outliers + presentar como semaforo no-score |
| E3 Forecast Calibrado | BLOCK | P2 | no | Solo diseño; lexico "apuesta"; trivia cultural | Banco evidence-controlled validado + renombrar fuera de apuesta + min-n=25; o **RETIRE** |
| E5 Escaner Alternativas | BLOCK | P3 | no | Scan-ratio inflable; gate RT-revelacion ausente | Dominante inequivoca + gate RT<150ms o **MERGE E7 / RETIRE** |
| E6 Caza-Trampas | BLOCK | P3 | no | Debiasing con transferencia debil; trivia de psicologia | Banco numerico validado + excludeRecent o **RETIRE** |
| E7 Presupuesto Decision | BLOCK | P3 | no | Solver = punto unico de fallo; drag = destreza motora | Solver exacto testeado + steppers discretos o diferir |
| E8 Resolucion Ambiguedad | BLOCK | P2 | no | Mide aversion al riesgo (rasgo); VOI no-trivial | VOI optimo correcto + banda tolerante + diferenciar de E1 o **RETIRE** |

## EMOCIONES (`emotions/`) — 15 ejercicios (catalogo) / 6 generadores reales

| Ejercicio | Veredicto | Prio | Perfil | Riesgo principal | Accion exacta |
|---|---|---|---|---|---|
| EM1 Pausa go/no-go | REVISE | P1 | si | Sin suelo go-accuracy: "never-respond" trivial | Suelo goAcc≥0.6 invalidante + gate timebase en RT + paridad tono-respuesta |
| EM2 Recuperacion post-error | REVISE | P1 | si | Anticipaciones contadas como error | Error genuino excluyendo anticipaciones + suficiencia≥3 pares por peor seed |
| EM3 Separar 4-cat | BLOCK | P0 | no | Banco sin inter-rater; holdout fantasma (`hecho`: isEval filtrado, Ola-4 ausente) | Inter-rater≥80% + holdout SERVIDO + detector de entropia (ButtonSpammer) |
| EM4 Calibrador intensidad | REBUILD | P2 | no | No implementado; ECE con celdas vacias | Cadena completa (generador+confidence+ECE gated) o **MERGE EM14** |
| EM5 Decisiones sesgo afectivo | RETIRE | P3 | no | Solapa EM-G; claim far-transfer | **RETIRE** |
| EM6 Tolerancia demanda | BLOCK | P0 | no | Prolongar espera = induccion emocional (frontera) | Prueba documentada de no-induccion + abandono no-penalizado o **RETIRE** |
| EM7 Reentrada tras pausa | MERGE | P3 | no | n=1-2 eventos = vacio estadistico | **MERGE en EM13** |
| EM8 Anti-impulso | MERGE | P2 | si | Identico a EM1 | **MERGE en EM1** como dificultad |
| EM9 Etiquetado rapido | BLOCK | P2 | no | Sin banco de valencia colapsa en EM3; roza IE | Banco valencia validado disjunto o **MERGE EM3** |
| EM10 Separacion bajo presion | MERGE | P2 | no | Identico a EM3 + dependencia timebase | **MERGE en EM3** |
| EM11 Continuidad feedback | MERGE | P2 | si | Clon de EM2 | **MERGE en EM2** |
| EM12 Espera activa | MERGE | P2 | no | ANTICIPATIONS RT-critica como primaria | **MERGE en EM1** (secundaria) |
| EM13 Variabilidad (RT_CV) | BLOCK | P3 | no | RT_CV sin timebase = "regulacion" falsa | Timebase autoritativa o degradar a feedback de sesion (no perfil) |
| EM14 Calibracion confianza | MERGE | P2 | no | Duplicado exacto de EM4 | **MERGE en EM4** |
| EM15 Recuperacion encadenada | MERGE | P2 | si | Tercer clon post-error; roza induccion | **MERGE en EM2** |

## CREATIVIDAD (`creativity/`) — 8 ejercicios (2 generadores reales)

| Ejercicio | Veredicto | Prio | Perfil | Riesgo principal | Accion exacta |
|---|---|---|---|---|---|
| EX01 Supresion Primer Impulso | REVISE | P1 | solo-calibrando | Sin banco prohibido mide fluidez de tecleo | Banco prohibido≥40 con inter-rater + dedup/overlap + inhibitionSuccessRate primaria + rotacion |
| EX02 Rotacion Categorias | BLOCK | P2 | solo-calibrando | switchCost no existe; identico a EX01 | Clasificador categorial + assembler o **MERGE EX01** |
| EX03 Sintesis Restriccion | REBUILD | P2 | solo-calibrando | Tag suelto = keyword-stuffing trivial (`hecho`: capa integrity BLOCKED) | Ventana de elaboracion como gate duro + banco con tension + calidad CALIBRATING |
| EX04 Analogias Remotas | BLOCK | P3 | solo-calibrando | Puente sintactico absurdo pasa filtro = titular fatal | Solo modo proporcional A:B::C:? verificable; **RETIRE modo libre** |
| EX05 Elaboracion Incremental | REBUILD | P2 | solo-calibrando | elaboration=palabras premia verbosidad | Cap de palabras + coherenceProxy como banda + anti-reinicio |
| EX06 Deteccion Inverosimil | REBUILD | P1 | si | Banco de frecuencias = punto unico de fallo | Banco de frecuencias auditado + rarity×plausibility vs perfil absurdo + rotacion dura |
| EX07 Reencuadre Problema | REBUILD | P2 | solo-calibrando | Marcadores lexicos = adivinanza; mide comprension de instrucciones | Onboarding obligatorio + variante "escala" verificable + calidad CALIBRATING |
| EX08 Improvisacion Tiempo Real | BLOCK | P3 | no | Mide destreza de tecleo, no ideacion (prohibido por charter) | Calibrador motor + timebase autoritativa o **RETIRE** |

## Resumen de la tabla

- **GO (APPROVE) hoy, sin condiciones:** ninguno. Todo el catalogo pasa por gates antes de tocar perfil.
- **Camino al v1 (REVISE/REBUILD → GO tras gates P0+P1):** los 9 del catalogo recomendado (`AUDIT_V2_PRODUCTO.md`
  §6): EX1, EX2, EX5, EX4, EX3 (Memoria); E2 Reversibilidad (Decisiones); EM1, EM2 (Emociones); EX06 (Creatividad).
- **MERGE (colapsar, no exponer como ejercicio separado):** E5→E2 (Enfoque); EX6→EX5, EX8→EX4 (Memoria);
  V-E3→V-E6 (Velocidad); EM8/EM12→EM1, EM10→EM3, EM11/EM15→EM2, EM14→EM4, EM7→EM13 (Emociones); EX02→EX01
  (Creatividad). ≥18 ejercicios miden varianza ya cubierta.
- **RETIRE:** Lenguaje E7/E8; Emociones EM5; mas RETIRE-condicional (E8-Enfoque, V-E5/E7/E8, EM6, EX04-libre,
  EX08) si su unico camino tecnico no se prioriza.
- **Bloqueadores P0 a nivel de ejercicio:** E8 Duo (rebuild runtime), Check-in (semaforo), EM3 (banco+holdout),
  Source Triage (copy-scanner), Transfer Probe (RCI), EM6 (no-induccion).

El detalle tecnico por ejercicio (archivos probables, criterios de aceptacion, tests obligatorios, DoD) esta en
`AUDIT_V2_TECNICO.md` §12 (tabla de desbloqueo) y §9 (issues P0/P1/P2/P3). El orden de ejecucion, en
`03_ROADMAP_FASES_P0_P1_P2_P3.md`.
