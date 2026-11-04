# Revisión crítica y cierre

## 1. Tres revisiones con propósito

**R1 — Requisitos y concepto:** ¿se usan las decisiones vigentes, el constructo está definido y se separa la auditoría real de su encargo? ¿La memoria sustenta el contexto? Resolver contradicciones antes de detallar.

**R2 — Diseño sustantivo:** ¿la práctica exige el mecanismo, tiene progresión y feedback útiles, controla atajos, carga y confusores, y permite evaluar resultados independientes? Revisar medición, accesibilidad y compatibilidad mientras aún es barato cambiar el diseño propuesto.

**R3 — Revisión integrada independiente:** entregar a un agente distinto el brief, fuentes, snapshot y dossier congelado. Pedir un resultado aplicable y contraejemplos, sin indicar la respuesta deseada. Si no hay otro agente, hacer una pasada secuencial y declarar que no es independiente.

Corregir hallazgos y reexaminar solo los riesgos afectados. Seguir el presupuesto y las condiciones de parada de [flujo](04_FLUJO_Y_ALCANCE.md). No llamar «revisión constante» a releer la misma prosa sin nuevas comprobaciones.

## 2. Hallazgos reproducibles

Cada hallazgo: ID, documento/versión, afirmación o comportamiento, evidencia/contraejemplo, consecuencia, severidad, gate afectado, cambio propuesto, dueño y verificación de cierre. «Podría ser mejor» sin consecuencia no es un bloqueo.

| Severidad | Criterio de este sistema |
|---|---|
| P0 | Diseño permite daño grave, exposición/corrupción o un claim/score materialmente falso como válido |
| P1 | Falta definición o contrato esencial; demanda eludible; comparabilidad/permiso crítico omitido; contradicción que impide aceptar ese uso |
| P2 | Mejora importante dentro del alcance sin un camino crítico demostrado |
| P3 | Refinamiento relevante y pequeño |

La severidad depende de consecuencia y uso, no de incomodidad para cerrar. Un P1 de medición puede impedir oficialidad pero no diseño training-only. Una propuesta de cambiar canon sigue pendiente del CEO aunque sea P3. No resolverla llamándola «fix técnico».

## 3. Rúbrica de diseño

Evaluar cada dimensión como `INSUFFICIENT`, `SPECIFIED`, `SUPPORTED_FOR_DESIGN` o `EMPIRICALLY_SUPPORTED`, y adjuntar evidencia. Las categorías no son una escala de capacidad ni probabilidades.

Dimensiones: fidelidad del constructo; mecanismo de aprendizaje; calidad/dosis de práctica; dificultad y progresión; personalización; resistencia a memorización/atajos; transferencia investigable; medición/uncertainty; complementariedad de catálogo; móvil/timing; accesibilidad; privacidad; compatibilidad y completitud.

No compensar un fallo crítico con estética, número de variantes o una buena media. Si la auditoría exige una rúbrica 0–100, conservarla como `design_quality_index` separado y explicar pesos, sensibilidad y gates. No inventar que ese índice predice eficacia o porcentaje de cerebro entrenado. Cambiar una rúbrica vigente requiere propuesta al CEO.

## 4. Falsación mínima

Aplicar los escenarios pertinentes, con resultados esperados y estado de ejecución:

- Respuesta aleatoria, siempre igual o ausencia de respuesta.
- Alta accuracy por prevalencia desequilibrada.
- Rapidez a costa de errores y precisión ultralenta.
- Anticipación, memorización, ítems nuevos con misma estrategia.
- Dificultad más baja produce mejor score sin cambio de capacidad.
- Práctica repetida se confunde con crecimiento general.
- Cambio de hardware, refresco, audio, input o accesibilidad.
- Baseline bajo, fatiga, recuperación y regresión a la media.
- Poca evidencia, techo/suelo o una sola familia dominante.
- Pausa/background, datos incompletos, reordenados o duplicados.
- Mezcla de versiones, modalidades, finalidades o sujetos.
- Propuesta de auditoría se presenta como aprobada/implementada.

Un cálculo manual/simulación sintética verifica lógica, no eficacia humana. Etiquetar `DESK_ANALYSIS`, `SYNTHETIC_SIMULATION`, `EXISTING_TEST_EXECUTED` o `NOT_RUN`. No inventar resultados de un motor no accesible.

## 5. Ejemplos de contraejemplos útiles

Con 95 ensayos no objetivo y 5 objetivo, responder siempre «no objetivo» consigue 95/100 correctos y detecta 0/5 objetivos. Por tanto, accuracy global aislada no identifica detección en ese diseño. Es un cálculo sintético, no una recomendación de prevalencia.

Un tiempo observado que cae de 500 a 450 ms tras cambiar de dispositivo admite explicaciones distintas de mejora cognitiva. Sin modelo/validación, no adjudicar los 50 ms al usuario ni «corregir» por una constante sin evidencia.

Cinco sesiones consecutivas el mismo día no cumplen 3 días/48 h. Cinco sesiones que sí lo cumplen tampoco prueban fiabilidad o transferencia automáticamente. El gate de cantidad no sustituye los demás.

## 6. Definition of Done de diseño

- Brief cubierto; todos los campos aplicables determinados o unknown accionable.
- Requisitos vinculados a fuentes, decisiones y evidencias.
- Alternativas y descartes con razón, sin cuota de ideas superficiales.
- No queda P0/P1 sin resolver para el uso que se propone aceptar. Una nota de disposición no lo resuelve: mantener ese uso bloqueado/condicionado mientras persista.
- Corregir antes de `REVIEWED` los P2/P3 seguros, relevantes y dentro del alcance del nuevo diseño que no cambien canon. Los que cambian canon se consultan y permanecen propuestos; todo diferido justifica alcance, consecuencia, dueño y reentrada.
- Decisiones que cambian canon se han preguntado y no adoptado sin respuesta.
- Motor actual y vNext separados; no claims de ejecución/validación inexistentes.
- Plan empírico especifica qué falta y cuándo se revisará.
- Memoria y archivos finales coherentes; cambios reales registrados.

`REVIEWED` significa revisado conceptualmente para ese alcance. No equivale a aprobación del CEO, `TRAINING_READY`, evidencia oficial, release o eficacia demostrada. Si falta un gate, entregar condicionado y visible.
