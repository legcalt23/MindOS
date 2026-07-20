# EXERCISE_PORTFOLIO_V5 — Matriz científica del portfolio de Enfoque (18 ejercicios)

```
DOCUMENT_STATUS = DRAFT · EXECUTION_AUTHORIZATION = NOT_GRANTED · Gate: S2 (dictamen efficacy-guardian, agent_outputs/02)
Regla: TODA transferencia es HYPOTHESIS (jamás claim). Estados C0: ACCEPT / PROTOTYPE / RESEARCH_ONLY.
Ningún ejercicio muestra subscore en C0; el feedback es descriptivo por-tarea (plantilla §D del dictamen).
Reuse de generador v1 = PASA a nivel de GENERACIÓN; PROHIBIDO heredar sus governingMetrics (H6): las métricas
gobernantes v1 con difference-scores/SSRT (CONGRUENCY_EFFECT, SET_SIZE_SLOPE, SWITCH_COST, DUAL_TASK_COST,
PRESSURE_DELTA, SSRT) quedan RETIRADAS como score (E013 Hedge: paradoja de fiabilidad).
```

## Constructo (ADR-FOCUS-CONSTRUCT, opción recomendada (a))

Las CINCO subhabilidades del PRD §6.1 organizan el portfolio como categorías de práctica (APPROVED producto,
CAT-002). Como factores medibles: sostenida/selectiva/inhibición = HYPOTHESIS defendibles; **control
atencional = sin subscore** (existencia CONFLICTED: E001 vs E002/E005); **estabilidad bajo tiempo = propiedad
de sostenida** (dinámica, no capacidad). RESTRAIN (freno motor) NO es de las 5 → training-only, ruta a perfil
ADR_REQUIRED. SUSTAIN/CONSTRAIN/RESTRAIN del pack = hipótesis rivales M1 del harness M0–M5 (S9).

## Tabla maestra (cobertura: sostenida ≥3 · selectiva ≥3 · inhibición ≥4 · control 2* · estabilidad 2† · 15 mecánicas distintas)

| id | nombre | subhab. primaria | secundarias | mecánica | modalidad | dur. | reuse v1 | C0 |
|---|---|---|---|---|---|---|---|---|
| FOC2-E01 | Centinela | Sostenida | estabilidad | vigilancia: target raro en flujo monótono | single-tap | 3-5m | E1 Vigía | ACCEPT · **TRACER B** |
| FOC2-E02 | Relevo | Sostenida | — | 2AFC continuo con regla estable | choice L/R | 2-4m | nuevo | ACCEPT |
| FOC2-E03 | Horizonte | Sostenida | estabilidad | time-on-task extendido (decremento) | single-tap | 8-12m | E1 + bloque largo | PROTOTYPE |
| FOC2-E04 | Constelación | Selectiva | — | búsqueda por conjunción, template visible | tap ubicación | 3-5m | E7 Rastreo | ACCEPT |
| FOC2-E05 | Puerta de ruido | Selectiva | — | detección señal/ruido (SDT) sí-no | choice | 3-5m | nuevo | ACCEPT (gate visual) |
| FOC2-E06 | Límite de canal | Selectiva | inhibición | selección espacial dentro/fuera de zona | tap | 3-5m | nuevo | ACCEPT |
| FOC2-E07 | Filtro vectorial | Inhibición | selectiva | flanker: target central vs flancos | choice L/R | 3-5m | E2 Filtro ⚠re-scoring | ACCEPT · **TRACER A** |
| FOC2-E08 | Guardia de mapping | Inhibición | control | interferencia dimensional/Simon-like | choice L/R | 3-5m | E5 ⚠re-scoring | ACCEPT |
| FOC2-E09 | Rasgo prioritario | Inhibición | selectiva | Stroop dimensional NO-léxico | choice L/R | 3-5m | E5 parcial/nuevo | ACCEPT |
| FOC2-E10 | Señal cruzada | Inhibición | control | cue perceptible irrelevante-predictivo | choice L/R | 3-5m | nuevo | ACCEPT (transparente) |
| FOC2-E11 | Guardián de meta | Control* | inhibición | mantener regla bajo señal competidora | choice L/R | 3-5m | E5/E11 parcial | ACCEPT |
| FOC2-E12 | Excepción visible | Control* | inhibición | override de mapping dominante | choice/tap | 3-5m | E3 adyacente | ACCEPT |
| FOC2-E13 | Ritmo estable | Estabilidad† | sostenida | estabilidad RT-CV SIN deadline agresivo | tap/choice | 4-6m | E2-continuo/nuevo | ACCEPT (sustituye v1 E13) |
| FOC2-E14 | Compás variable | Estabilidad† | sostenida | exactitud/d′/criterio por condición ISO vs JIT | single-tap | 8-9m | E1 (generador solo) | PROTOTYPE |
| FOC2-E15 | Mantén la línea | (RESTRAIN)‡ | — | go/no-go: retener respuesta prepotente | tap/no-tap | 3-5m | E3 Gatillo | ACCEPT training-only |
| FOC2-E16 | Contramanda | (RESTRAIN)‡ | — | stop-signal: cancelar respuesta iniciada | stop | 4-6m | E4 Freno | **RESEARCH_ONLY** |
| FOC2-E17 | Retoma el hilo | (Recuperación)§ | sostenida | interrupción sintética interna + reentrada | tap/choice | 4-6m | E11 parcial | PROTOTYPE |
| FOC2-E18 | Reinicio tras error | (Recuperación)§ | sostenida | recuperación post-error controlada | tap/choice | 4-6m | E12 | PROTOTYPE |

\* sin subscore (alimentan M0-M5) · † propiedad, no capacidad · ‡ fuera de las 5; perfil ADR_REQUIRED · § sonda, nunca subscore.

## Dossier-lite por ejercicio (exclusiones · confounds→mitigación · eje dificultad · métricas · transferencia HYP · evidencia · falsificador)

**E01 Centinela** — Excl.: no infiere productividad/atención cotidiana/rasgo unitario. Conf.: criterio
(→ d′/A′ + criterio separados), gaming rítmico (→ ISI aleatorios + análisis de periodicidad), estado (E021 →
instrucciones estandarizadas). Dif.: discriminabilidad→frecuencia→duración (uno por vez). Métr.: hit/miss/FA/CR,
d′, criterio, secuencias de lapsos, slope, anticipaciones; RT-hit secundario. Transf.: T1 probable, T2 con
formas nuevas — HYP. Evid.: F03 alerting VERIFIED-en-doc; pack FOC-E01 ACCEPT REPORTED. Fals.: correlación ~0
con E02 tras controlar velocidad basal ⇒ no hay SUSTAIN, solo skill del juego.

**E02 Relevo** — Excl.: no multitarea/capacidad general/velocidad pura. Conf.: sesgo de alternancia (→ balance
de clases + tests de patrón), staircase midiendo visión/motor (→ discriminabilidad fija, adaptar variabilidad).
Dif.: separación perceptual→variabilidad→duración. Métr.: exactitud por clase, balanced accuracy, MAD/CV
robusto, change points. Transf.: T2/T3 — HYP. Evid.: pack FOC-E02 ACCEPT REPORTED; E053 LOW. Fals.: sin
varianza incremental sobre E01 ⇒ redundante, se elimina uno.

**E03 Horizonte** — Excl.: la pendiente no es rasgo/voluntad/salud. Conf.: throttling/batería (→ registrar
frames, invalidar SEGMENTOS no la sesión), aburrimiento/hora. Dif.: SOLO duración progresa. Métr.: desempeño
por bins/cuartil, slope/curvatura robusta, recuperación tras pausa, abandono informativo. Transf.: T1 + dinámica
— HYP. Evid.: F03 VERIFIED-en-doc; pack PROTOTYPE. Fals.: pendiente no sobrevive a controles emparejados /
test-retest ~0. Carga: máx. 1 bloque largo/día (ética).

**E04 Constelación** — Excl.: no memoria (template visible)/visión clínica. Conf.: escaneo sistemático/saliencia
(→ layouts procedurales restringidos + mapas de posición testeados). Dif.: set size→heterogeneidad→semejanza.
Métr.: exactitud por set size, misses/FA en target-absent, cobertura espacial; **slope SECUNDARIO** (E013 —
corrección al v1 que usa SET_SIZE_SLOPE como gobernante). Transf.: T2/T3 — HYP. Evid.: F03 orienting; pack
FOC-E06 ACCEPT. Fals.: la señal sigue posición/saliencia, no conjunción.

**E05 Puerta de ruido** — Excl.: no agudeza/visión clínica. Conf.: pantalla OLED/LCD/brillo (→ clase visual por
dispositivo + gate visual; daltonismo neutralizado por luminancia), sesgo de respuesta (→ catch trials).
Dif.: SNR→estructura del distractor→timing. Métr.: d′/no-paramétrico, criterio, calibración. Transf.: T1/T2/T3
— HYP. Evid.: SDT marco sólido; pack FOC-E05 ACCEPT. Fals.: la clase de pantalla domina (falla invariancia por
dispositivo). Nota: separa sensibilidad/criterio ⇒ el mejor cumplimiento del Charter §8.1 del portfolio.

**E06 Límite de canal** — Excl.: no negligencia clínica. Conf.: mirar solo al centro (→ balance angular + catch
events), borde visual como muleta. Dif.: ancho de canal→distancia→simultaneidad. Métr.: hits internos,
intrusiones externas, omisiones, exactitud por distancia, criterio. Transf.: T3 a E07 — HYP. Evid.: pack
FOC-E07 ACCEPT; F03. Fals.: percepción/campo visual explican la intrusión.

**E07 Filtro vectorial (TRACER A)** — Excl.: no "inhibición general"/productividad/transferencia. Conf.:
contaminación de Velocidad (→ RT y exactitud SEPARADOS), proporción de congruencia aprendida (→ cuotas
balanceadas + catch cells), **difference-score RT PROHIBIDO como score único** (→ exactitud por condición +
intrusiones + d′). Dif.: separación target-flanco→similitud→proporción. Métr.: exactitud por congruencia,
intrusiones, balanced accuracy por condición, criterio; RT secundario por condición. Transf.: T2 formas nuevas,
T3 — HYP. Evid.: F03 executive; E013 VERIFIED-en-doc; pack FOC-E04 ACCEPT (tracer prioritario). Fals.: techo de
exactitud sin varianza / modelo de velocidad explica la estructura. ⚠ v1 usa CONGRUENCY_EFFECT gobernante → RETIRADO.

**E08 Guardia de mapping** — Excl.: no task-switching/memoria/autocontrol. Conf.: proporción de conflicto,
verbalización, difference-score (→ exactitud/errores de mapping por condición; mapping estable en bloque,
NUNCA adaptado trial-a-trial). Dif.: fuerza predictiva→similitud→proporción. Métr.: exactitud/errores de mapping
por condición, sensibilidad/criterio. Transf.: near a E09/E07 — HYP. Evid.: pack FOC-E08 ACCEPT; E020
CONFLICTED. Fals.: M5 (método) agrupa la covarianza.

**E09 Rasgo prioritario** — Excl.: no lectura/switching. Conf.: atender solo posición (→ atributos redundantes),
saliencia débil = visión (→ control perceptual), daltonismo (→ forma+textura). NO-léxico obligatorio. Dif.:
saliencia relativa→congruencia→similitud (dimensión fija por bloque). Métr.: exactitud por congruencia/
saliencia, intrusiones, criterio. Transf.: near — HYP. Evid.: pack FOC-E10 ACCEPT. Fals.: diferencia individual
no fiable / la explica velocidad.

**E10 Señal cruzada** — Excl.: no aprendizaje implícito puro/credulidad. Conf.: descubrir el patrón (→ awareness
check separado no puntuado); ética de contingencia encubierta (→ contingencia fija por forma, transparente en
producto, debrief+consentimiento en investigación). Dif.: cue-target interval→saliencia→validez. Métr.:
exactitud por cue válido/inválido, capturas, criterio. Transf.: near — HYP. Evid.: pack FOC-E09 ACCEPT
(práctica transparente); F03 cueing. Fals.: sin efecto de validez fiable.

**E11 Guardián de meta*** — Excl.: no memoria (regla SIEMPRE visible)/switching. Conf.: regla oculta contamina
Memoria (→ visible), proporción de conflicto. Dif.: fuerza del distractor→demora de mantenimiento. Métr.:
errores de mapping/lapses, exactitud bajo competencia. Transf.: alimenta M0-M5 — HYP, sin subscore. Evid.:
E001/E002 CONFLICTED; E006. Fals.: M0/M4/M5 explican mejor que un factor de control.

**E12 Excepción visible*** — Excl.: no SSRT/switching/decisión de valor. Conf.: atender solo al badge (→ catch
cells), inversión = Flexibilidad (→ regla estable por bloque), lentitud estratégica (→ exactitud Y RT
reportados). Dif.: frecuencia de override→saliencia→compatibilidad. Métr.: exactitud base/override, intrusiones
del mapping dominante, criterio. Transf.: near a E15; relación empírica con E16 — HYP. Evid.: pack FOC-E13
ACCEPT (menos dependiente de race-model que stop). Fals.: es solo aprendizaje de mapping.

**E13 Ritmo estable†** — Excl.: NO es velocidad; sin deadline agresivo como eje (no importar Velocidad — 02 §8).
Conf.: contaminación de Speed (→ deadline generoso, RT-CV sobre respuestas válidas), throttling. Dif.:
variabilidad/carga moderada, sin comprimir tiempo. Métr.: RT-CV robusto (MAD), estabilidad por ventana, lapsos
sobre RT crudos (learning 2026-06-04: lapsos JAMÁS sobre lista filtrada por MAD). Transf.: T1 — HYP. Evid.:
P03 VERIFIED-en-doc (mediana/RT-CV, no media). Fals.: "estabilidad" indistinguible del jitter del dispositivo.
Sustituye honestamente al v1 E13 Presión (H4).

**E14 Compás variable†** — REDISEÑADO en S2 (2026-07-20). La fila anterior, «Constancia larga», era una VISTA
de E03 —mismo bloque largo, misma sesión, mismo generador, y E03 ya reporta la curva por cuartil— cuya única
aportación propia era la PENDIENTE, un difference-score de la clase retirada por la regla 1 y con fiabilidad
individual ≈0 por construcción. Estaba dominada por los dos lados: su exactitud es E03, su estabilidad de RT es
E13. Se sustituye por una manipulación de la PREDICTIBILIDAD DEL RITMO, no del tiempo transcurrido.
**Mecánica:** bloque single-tap con formas no-léxicas, en tramos contrabalanceados e intercalados que alternan
ISI **ISO** (isócrono, llegada anticipable) e ISI **JIT** (distribución ancha no-envejeciente, no anticipable),
con media de ISI, tasa de objetivos, discriminabilidad y duración IGUALADAS entre condiciones: lo único que
difiere es la predictibilidad temporal. **Excl.:** no mide velocidad, ni ritmo motor, ni "constancia" como
rasgo; ISO−JIT **PROHIBIDO como puntuación** (vinculante, no declarativo; regla 1). **Conf.:** expectativa
temporal aprendida en ISO (→ análisis de periodicidad de respuestas como métrica de gaming), arousal por
incertidumbre (→ dosis igualada), orden/secuencia (→ tramos contrabalanceados), criterio distinto por condición
(→ d′ y criterio SIEMPRE separados, regla 6), throttling/fatiga (→ se cancela: condiciones entrelazadas en el
mismo intervalo, así que cualquier degradación monótona afecta a ambas por igual). **Dif.:** SOLO
discriminabilidad del objetivo; la predictibilidad es CONDICIÓN, jamás eje (sellada por bloque). **Métr.:** d′,
criterio y exactitud POR CONDICIÓN (ISO/JIT); FA en catch de objetivo ausente; periodicidad de respuesta en ISO;
RT-hit por condición **secundario** (regla 2). **Dosis (derivada, no aspiracional):** ≥30 objetivos/condición al
~27% ⇒ ~110 trials/condición, ~220 totales; ISI medio 2.0 s ⇒ **8-9 min reales** ⇒ **ES bloque largo** y
**compite con E03 por la cuota de la regla 7** (se alternan por día). **Transf.:** T1 y a E01/E02 con formas
nuevas — HYP. **Evid.:** F03 (preparación temporal / alerting); P01 (difference-scores de baja fiabilidad ⇒
primario por condición); `MinSampleGate` DPRIME nMinPerCell=20 VERIFIED. **Fals.:** ≥120 usuarios × ≥2 sesiones:
(i) split-half par/impar Spearman-Brown de d′_ISO y d′_JIT, cada una ≥0.60; (ii) test-retest de cada d′ ≥0.50;
(iii) r(ISO,JIT) **desatenuada ≤0.90**; (iv) objetivos efectivos ≥20/condición en ≥90% de sesiones. Falla
cualquiera ⇒ **RETIRAR E14**. Mitad discriminante obligatoria: simulador δ~N(0.45,0.30) DEBE pasar (i)-(iv), y
espejo negativo δ~N(0.05,0.30) DEBE fallar (iii); ambos corridos ANTES de ratificar el falsificador.
**PROTOTYPE, no ACCEPT:** la envolvente de dosis NO cierra — no caben a la vez bloque corto, d′ por condición con
n≥20/celda y manipulación detectable. **Disidencia registrada:** ambos especialistas prefieren retirar la
faceta† (portfolio a 17, E13 bajo sostenida) si no cierra, y no cierra; el CEO optó por el rediseño. **Riesgo
dominante NO resuelto:** que sea E01 con dos etiquetas — solo lo decide (iii), y ese dato no existe hoy.
Detalle completo en `09_DECISION_LOG.md` §20.4.4.

**E15 Mantén la línea‡** — Excl.: no autocontrol moral/stop-signal (E051: go/no-go ≠ stop). Conf.: esperar
siempre / periodicidad (→ rachas controladas + catch), velocidad-precisión (→ comisiones Y omisiones). Dif.:
discriminabilidad→frecuencia no-go→tempo. Métr.: comisiones, omisiones-go, d′/criterio, post-error. Transf.:
near a E12 — HYP. Evid.: pack FOC-E11 ACCEPT; E051 VERIFIED-en-doc. Fals.: la señal es criterio, no refreno.
Perfil: ADR_REQUIRED (no es de las 5).

**E16 Contramanda (RESEARCH_ONLY)** — Excl.: no go/no-go; JAMÁS llamar "SSRT" al resultado en producto. Conf.:
ralentización estratégica (→ diagnostics de race-model), trigger failures, dependencia crítica de timing físico
(T2 requerido — hoy inalcanzable). Dif.: solo SSD adaptado ~50% stop-success. Métr.: distribución go-RT,
stop-success por SSD, trigger-failure; SSRT SOLO si pasa gates. Evid.: E050 consenso, E052 SSRT inestable
(VERIFIED-en-doc); pack PROTOTYPE. ⚠ v1 usa SSRT gobernante → inaceptable en C0; sin score.
**Transf.: NINGUNA afirmable, ni siquiera T1** (añadido en S2). Cancelar una respuesta ya iniciada es freno
motor (RESTRAIN), que NO pertenece a las 5 subhabilidades del PRD §6.1: no hay ruta declarable a "inhibición de
distractores". Su relación empírica con E12/E15 es pregunta de investigación abierta, jamás claim. Copy C0
máximo: "práctica de cancelar una respuesta ya empezada". Todo salto a autocontrol, impulsividad o vida
cotidiana = transferencia lejana prohibida (T07). **Fals.** (añadido en S2 — la fila era la ÚNICA del documento
sin condición de refutación, en un documento cuyo método entero es falsificador-driven): sobre bloques con SSD
adaptada, queda refutado como candidato a medición si se da CUALQUIERA de — **(a) supuesto de carrera roto:**
RT medio de stop-fallido **≥** RT medio de go en **>20%** de las sesiones (la desigualdad del modelo de carrera
es la precondición que da sentido a toda estimación); **(b) staircase que no converge:** p(responder|señal)
fuera de **0.45-0.55** o IC95% que excluya 0.50 al cierre del bloque; **(c) el dispositivo domina la varianza:**
SSD ENTREGADA (medida por frame stats) desviada de la nominal por más que la diferencia entre usuarios que se
pretende medir ⇒ la varianza entre personas es del hardware; **(d) fallos de disparo:** trigger-failure estimada
**>0.10**. **Mitad discriminante obligatoria:** un perfil sintético con freno bien especificado DEBE pasar
(a)-(d); si no pasa nadie, el falsificador está clampando y no prueba nada. Testeable HOY en sintético: (a),
(b), (d). **BLOQUEADO hasta T2:** (c). **Razón de que ahora sí sea ratificable:** un RESEARCH_ONLY sin
falsificador no es un estado, es un aplazamiento indefinido — no existía condición bajo la cual graduarse ni
bajo la cual retirarse; ahora tiene ambas. Su T-ADV queda DEFERRED-by-design (no aspira a medición en C0), y se
activa el día que alguien proponga graduarlo.

**E17 Retoma el hilo§** — Excl.: no multitarea/productividad; **NUNCA notificaciones reales** (privacidad/OS).
Conf.: memorizar siguiente respuesta (→ estado visible), coste = releer instrucciones (→ controles con pausa
emparejada SIN tarea interruptora). Dif.: longitud de estado→duración→semejanza de interrupción. Métr.:
resumption lag (secundario), error inmediato, trials-hasta-baseline vs pausa emparejada. Transf.: T2 con
interruptores nuevos = outcome clave — HYP. Evid.: E048 (coste agudo ~7s) VERIFIED-en-doc; E047 REPORTED
(intervención ambiental, no rasgo); pack PROTOTYPE. Fals.: el coste no sobrevive a controles emparejados.
⚠ v1 E11 "resistencia a la distracción digital" (claim everyday implícito) → reencuadrado (H5).

**E18 Reinicio tras error§** — Excl.: no regulación emocional/resiliencia. Conf.: errores deliberados (→ gaming
detection), regresión a la media (→ controles emparejados por dificultad/rareza), **no inducir errores** (pocos
errores = poca evidencia, no fabricar). Dif.: base fija; oportunidades = errores naturales. Métr.: exactitud/RT
en +1…+k, trials-a-baseline, cascadas, missing-por-falta-de-errores. Transf.: T2/T3 — HYP. Evid.: post-error
slowing real con mecanismo discutido; pack PROTOTYPE. Fals.: el efecto es regresión a la media.

## Selección de tracers (S3/S4) — justificación por criterios de riesgo arquitectónico

**Tracer A = FOC2-E07 (flanker)** · **Tracer B = FOC2-E01 (vigilancia)**. Un motor sobreajustado a una mecánica
muere con este par: (1) modalidad distinta (choice L/R vs single-tap go-only); (2) estructura temporal distinta
(trials discretos densos vs monitoreo continuo esparcido); (3) estructura de trial distinta (ventana obligatoria
vs no-responder-es-la-norma con FA/decremento); (4) confounds DISJUNTOS (velocidad/difference-score/proporción
vs criterio/ritmo/estado/decremento); (5) cubre CONSTRAIN+SUSTAIN, las 2 subhabilidades más defendibles;
(6) ambos ACCEPT accuracy-primary (sobreviven el gate de timebase; nada depende de SSRT ni de dispositivos
RESEARCH_ONLY). Pares rechazados: SUSTAIN+SUSTAIN (no estresa), CONSTRAIN+CONSTRAIN (misma estructura),
stop-signal (bloqueado por timing). Qué NO demuestra el par: factor Focus, M0-M5, transferencia, validez de RT,
RESTRAIN; con alpha ≈20, nada psicométrico.

## Reglas transversales del portfolio

1. Primario SIEMPRE exactitud/d′/criterio POR CONDICIÓN; difference-scores solo descriptivos (R1/H2).
2. RT secundario en C0 en TODO el portfolio; oficial solo ≥T2 (ADR-FOCUS-TIMING).
3. Un solo eje de dificultad por decisión (Quality §9.3); dificultad sellada por bloque para medición.
4. Formas paralelas + bancos training/measurement separados + sondas retenidas diseñadas ANTES de entrenar (R5).
5. Estímulos no-léxicos en el núcleo (02 §2.3); ES-first solo en copy.
6. Sensibilidad y criterio SIEMPRE separados en SUSTAIN/CONSTRAIN (Charter §8.1).
7. Máximo 1 bloque largo (8-12m)/día; el throttling se registra como confound técnico, no cognitivo.
8. Cada ejercicio que aspire a measurement necesita su propio T-ADV (adversarios de SU mecánica).
9. **[R6-V5 P2-3] TODA fila con "reuse v1" implica ⚠re-scoring OBLIGATORIO (H6), no solo E07/E08: los
   governingMetrics v1 de E1 (SUSTAINED_FOCUS_INDEX del chip FOCO), E7 (SET_SIZE_SLOPE), E3, E12 y E4 (SSRT)
   quedan igualmente RETIRADOS; el reuso autorizado es EXCLUSIVAMENTE del generador de estímulos.**
10. **[R6-V5 #34] Equidad de modalidad: toda mecánica choice L/R ofrece modalidad secuencial para
   switch-access (foco alternante + activación única), registrada como modalidad DISTINTA (SWITCH_SEQUENTIAL)
   que jamás se agrega con TOUCH_DIRECT sin BridgeApproval (ADR-SCI-008). Con screen-reader activo, las
   métricas RT-críticas quedan fuera de evidencia oficial; accuracy y práctica se conservan íntegras.**
