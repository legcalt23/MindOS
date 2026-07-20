# Orden 1 — efficacy-guardian (2026-07-19) — output íntegro

> Read-only. Gate S2. Integrado en: EXERCISE_PORTFOLIO_V5.md, plan §3/§5 S2-S4, ADR-FOCUS-CONSTRUCT,
> REDTEAM_TEST_MATRIX (falsificadores). Veredicto: CONDICIONAL — APTO para S3/S4 con tracers FOC2-E07 + FOC2-E01,
> C0 training-only, con bloqueantes B-1..B-6 antes de score/perfil.

## Encuadre de autoridad
- APPROVED (canon): PRD §6.1 (5 subhabilidades), Charter, Exercise Quality Standard.
- Corpus repo docs/knowledge/cognitive_science: evidencia escéptica canónica.
- Research pack: EXTERNAL_UNVERIFIED — propuesta; no adquiere autoridad por existir.
- **Hallazgo transversal:** el PRD lista las 5 subhabilidades como interfaces de navegación (como Charter §4.1 con
  los 8 bloques). Dos de las cinco — control atencional y estabilidad bajo tiempo — NO son factores latentes
  independientes defendibles: APPROVED como etiquetas de producto; CONTRADICTED/HYPOTHESIS como subscores.

## A) Operacionalización de las 5 subhabilidades

| Subhabilidad (PRD §6.1) | Faceta-pack | Estado como factor independiente |
|---|---|---|
| Atención sostenida | FOCUS_SUSTAIN | HYPOTHESIS (familia defendible) |
| Atención selectiva | FOCUS_CONSTRAIN (filtrado) | HYPOTHESIS |
| Inhibición de distractores | FOCUS_CONSTRAIN (blindaje) | HYPOTHESIS |
| Control atencional | etiqueta superior disputada | **CONTRADICTED como subscore** (E006 Oberauer; E001 vs E002/E005) |
| Estabilidad bajo tiempo | propiedad de SUSTAIN | **CONTRADICTED como capacidad separada** (02 §2) |

**Frontera crítica:** FOCUS_RESTRAIN (inhibición de RESPUESTA: go/no-go, stop-signal) NO corresponde a ninguna
de las 5 aprobadas — "inhibición de distractores" es inhibición de interferencia perceptivo-atencional, no freno
motor (E051 Raud 2020: mecanismos distintos). v1 mete E3/E4 (RESPONSE_INHIBITION) en Enfoque = cruce de
constructo. Alojables como práctica; ruta a perfil ADR_REQUIRED.

Por subhabilidad (definición funcional · paradigmas+evidencia · exclusiones · confounds · falsificador):
- **A.1 Sostenida:** meta+política de respuesta estables con demanda baja/estímulo infrecuente. PVT/CPT/SART
  (F03 alerting VERIFIED-en-doc), oddball, vigilancia 2AFC. NO mide: memoria, velocidad pura, motivación,
  multitarea. Confounds: motivación/estado (E021 McGough: una advertencia cambia el SART), criterio rare-target
  (separar d′ de criterio), gaming rítmico, velocidad-precisión, fiabilidad individual baja (E013 Hedge).
  Falsificador: covariación entre formas desaparece al controlar práctica; advertencia mueve el "rasgo" más que
  la repetición; "estabilidad" indistinguible del ruido del dispositivo.
- **A.2 Selectiva:** priorizar señal frente a competencia perceptiva. Búsqueda por conjunción, SDT, cueing (F03
  orienting). E010 Peng&Miller g≈.25 pero población infantil/clínica → aplicabilidad LOW. Confounds:
  discriminabilidad basal, pantalla (E026 latencia 35-140ms), estrategia de escaneo, slopes no fiables (E013).
  Falsificador: techo de exactitud; covariación sigue dispositivo, no mecanismo.
- **A.3 Inhibición de distractores:** limitar impacto de señal irrelevante competidora (flanker, Simon no-léxico,
  cross-cue; F03 executive/conflict). NO mide inhibición unitaria (E020 Rouder CONFLICTED). Confound MÁXIMO:
  difference-score RT (incongruente−congruente) amplifica error (E013). Falsificador: solo efectos de diferencia
  no fiables; modelo de velocidad explica la estructura.
- **A.4 Control atencional:** paraguas de producto; NO factor identificado. E001 Unsworth (84,4% recuperan factor,
  carga ~.51) vs E002 Rey-Mermet (desaparece con difusión jerárquica) vs E005 Löffler (=velocidad de captación).
  Veredicto existencia (03 §1): CONFLICTED. NUNCA subscore oficial independiente en C0; composite hipotético
  etiquetado o no mostrarse; ADR_REQUIRED.
- **A.5 Estabilidad bajo tiempo:** vigilance decrement = propiedad/dinámica de SUSTAIN, no capacidad. Confounds:
  motivación/hora/sueño, **thermal throttling y batería** (degradación técnica ≈ cognitiva), fiabilidad de
  pendientes ~0. Se reporta como dinámica ("cómo cambia tu estabilidad en la sesión"), no subscore. **RIESGO
  v1 E13 Presión:** eje deadline/presión importa VELOCIDAD al motor (02 §8 prohíbe deadlines como eje primario).

**A.6 Modelos rivales M0-M5** (pack 02 §4 + Charter §22/§25): M0 tareas independientes / M1 3-facetas /
M2 factor superior / M3 bifactor / M4 velocidad de captación / M5 método-mapping. Distinguirlos exige ≥2 métodos
por faceta + recuperación de parámetros en simulación + validación cruzada + invariancia por dispositivo +
utilidad incremental sobre Memoria/Velocidad. M2 CONFLICTED hoy; M4/M5 rivales vivos probables (E002/E005/E013).
ADR_REQUIRED para el modelo de bloque (Charter §24 #1/#2); alpha ≈20 NO distingue ningún par.

## B) Matriz de 18 ejercicios (≥15; ≥2 por subhabilidad; diversidad real)

Estados C0: ACCEPT / PROTOTYPE / RESEARCH_ONLY. Toda transferencia = HYPOTHESIS.

| id | nombre | subhab primaria | mecanismo | modalidad | dur | reuse v1 | Estado C0 |
|---|---|---|---|---|---|---|---|
| FOC2-E01 | Centinela | Sostenida | target raro en flujo monótono | single-tap | 3-5m | E1 Vigía | ACCEPT · **TRACER B** |
| FOC2-E02 | Relevo | Sostenida | 2AFC continuo regla estable | choice L/R | 2-4m | NUEVO | ACCEPT |
| FOC2-E03 | Horizonte | Sostenida | time-on-task extendido | single-tap | 8-12m | E1 largo | PROTOTYPE |
| FOC2-E04 | Constelación | Selectiva | búsqueda conjunción template visible | tap ubicación | 3-5m | E7 Rastreo | ACCEPT |
| FOC2-E05 | Puerta de ruido | Selectiva | SDT señal/ruido sí-no | choice pres/aus | 3-5m | NUEVO | ACCEPT (gate visual) |
| FOC2-E06 | Límite de canal | Selectiva | selección espacial dentro/fuera | tap | 3-5m | NUEVO | ACCEPT |
| FOC2-E07 | Filtro vectorial | Inhib. distract. | flanker target vs flancos | choice L/R | 3-5m | E2 Filtro (⚠ re-scoring) | ACCEPT · **TRACER A** |
| FOC2-E08 | Guardia de mapping | Inhib. distract. | interferencia dimensional/Simon | choice L/R | 3-5m | E5 (⚠ re-scoring) | ACCEPT |
| FOC2-E09 | Rasgo prioritario | Inhib. distract. | Stroop dimensional NO-léxico | choice L/R | 3-5m | E5 parcial/NUEVO | ACCEPT |
| FOC2-E10 | Señal cruzada | Inhib. distract. | cue irrelevante-predictivo | choice L/R | 3-5m | NUEVO | ACCEPT (transparente) |
| FOC2-E11 | Guardián de meta | Control atencional* | regla bajo señal competidora | choice L/R | 3-5m | E5/E11 parcial | ACCEPT |
| FOC2-E12 | Excepción visible | Control atencional* | override de mapping dominante | choice/tap | 3-5m | E3 adyacente | ACCEPT |
| FOC2-E13 | Ritmo estable | Estab. tiempo | estabilidad RT-CV sin deadline agresivo | tap/choice | 4-6m | E2-continuo/NUEVO | ACCEPT (sustituye v1 E13) |
| FOC2-E14 | Constancia larga | Estab. tiempo | curva de estabilidad por cuartil | single-tap | 8-12m | E1 (=E03 variante) | PROTOTYPE |
| FOC2-E15 | Mantén la línea | (RESTRAIN†) | go/no-go retener prepotente | tap/no-tap | 3-5m | E3 Gatillo | ACCEPT (training-only) |
| FOC2-E16 | Contramanda | (RESTRAIN†) | stop-signal cancelar iniciada | stop | 4-6m | E4 Freno | **RESEARCH_ONLY** |
| FOC2-E17 | Retoma el hilo | (Recuperación‡) | interrupción sintética + reentrada | tap/choice | 4-6m | E11 parcial | PROTOTYPE |
| FOC2-E18 | Reinicio tras error | (Recuperación‡) | recuperación post-error controlada | tap/choice | 4-6m | E12 | PROTOTYPE |

\* sin subscore propio (alimentan M0-M5). † no es de las 5; ruta a perfil ADR_REQUIRED. ‡ sonda, nunca subscore.
Cobertura: Sostenida ≥3, Selectiva ≥3, Inhib. ≥4, Control 2 (sin subscore), Estabilidad 2 (propiedad). 15
mecánicas distintas (vigilancia, 2AFC, búsqueda, SDT, espacial, flanker, Simon, dimensional, cueing,
goal-maintenance, override, go/no-go, stop, interrupción, post-error).

El detalle por ejercicio (exclusiones · confounds+mitigación · eje de dificultad · métricas · transferencia ·
evidencia · falsificador) está integrado fila a fila en EXERCISE_PORTFOLIO_V5.md (fuente: este dictamen).

## C) Dos tracers — par re-derivado por criterios

**Tracer A (S3) = FOC2-E07 Filtro vectorial** (CONSTRAIN/inhibición; discreto, choice L/R, deadline generoso).
**Tracer B (S4) = FOC2-E01 Centinela** (SUSTAIN/sostenida; continuo, single-tap, ISI largos, targets escasos).
Criterios: (1) modalidad de respuesta distinta; (2) estructura temporal distinta (denso stimulus-locked vs
esparcido continuo); (3) estructura de trial distinta (ventana obligatoria vs no-responder-es-norma);
(4) confounds DISJUNTOS (A: velocidad+difference-score+proporción congruencia; B: criterio+ritmo+estado+
decremento) → un arreglo de una clase no enmascara la otra; (5) cubre 2/3 facetas y las 2 subhabilidades más
defendibles; (6) ambos ACCEPT accuracy-primary con RT secundario → sobreviven el gate de timebase.
Rechazados como par: SUSTAIN+SUSTAIN (no estresa), CONSTRAIN+CONSTRAIN (comparte estructura), stop-signal
(bloqueado por timing → confundiría "arquitectura ok" con "medida bloqueada shippeada").
**Qué NO demuestra:** factor Focus, M0-M5, transferencia, validez de RT, RESTRAIN; con alpha ≈20 nada psicométrico.

## D) Límites de claims C0
Plantilla claim por ejercicio: "Diseñado para practicar [demanda concreta]. Aquí ves tu progreso en este
ejercicio frente a tu propia línea base; es orientativo."
Feedback de sesión máximo: descriptivo + una acción + límite ("…Esto refleja tu práctica en este ejercicio; tu
perfil sigue calibrándose.").
Prohibido sin datos humanos: saltos a rendimiento cotidiano/estudio/trabajo/capacidad general, comparación
normativa, "subir nivel = mejora de capacidad", RT como "velocidad mental" sin gate, control atencional o
estabilidad como subscores. Base: T01 Simons, T07, Charter §19.3, PRD §27.

## E) Riesgos científicos top-8
R1 difference-scores como score individual (CONGRUENCY_EFFECT, SET_SIZE_SLOPE, SWITCH_COST, DUAL_TASK_COST,
PRESSURE_DELTA, SSRT del v1) → primario = exactitud/d′/criterio POR CONDICIÓN; diferencia solo descriptiva.
R2 rival de velocidad M4 colapsa el factor → modelar Speed rival; separar RT/exactitud; utilidad incremental.
R3 método/mapping M5 → ≥2 métodos por faceta; invariancia por dispositivo antes de comparar.
R4 RT móvil no comparable → fail-closed hasta calibración física; RT secundario en C0.
R5 práctica/item-memory → formas paralelas, bancos train/measure separados, sondas retenidas ANTES de entrenar.
R6 cruce de constructo (E3/E4 RESTRAIN, E6 switching→Flexibilidad, E8/E9 MOT/dual fuera del core, E10 RSVP).
R7 estado/motivación como rasgo → instrucciones estandarizadas; contexto local nunca ajusta score oficial.
R8 regresión a la media como mejora → baselines multi-observación, reliable-change con SEM (E054; Charter §9.5).

## Hallazgos de gate
H1 BLOQUEA control atencional/estabilidad como subscores (PASA como etiquetas). H2 BLOQUEA governingMetrics v1
difference-score/SSRT (E2/E5/E6/E7/E8/E13/E4). H3 BLOQUEA/RIESGO cruce de constructo del catálogo v1.
H4 RIESGO E13 Presión importa Velocidad → sustituir por FOC2-E13. H5 RIESGO E11 Ancla claim implícito
"resistencia a la distracción digital" → reencuadrar recuperación sintética. H6 PASA reuso de generadores
E1/E2/E3/E5/E7/E12; BLOQUEA heredar sus governingMetrics.

## VEREDICTO: CONDICIONAL — APTO para S3/S4 (tracers FOC2-E07+FOC2-E01, C0, training-only)
Bloqueantes ANTES de score/perfil (no de S3): B-1 sin difference-score/SSRT como score; B-2 control
atencional/estabilidad sin subscore (ADR); B-3 RESTRAIN ruta a perfil ADR + MOT/RSVP/stop RESEARCH_ONLY;
B-4 RT fail-closed hasta timebase físico; B-5 ADR modelo de bloque + M0-M5 en beta; B-6 sondas retenidas
diseñadas ANTES de entrenar; alpha ≈20 no valida psicometría.
