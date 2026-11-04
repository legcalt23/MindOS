# Entrega, trazabilidad y continuidad

## 1. Resultado según alcance

| Alcance | Salida necesaria |
|---|---|
| Preparación/contexto | Brief, snapshot, fuente vigente, memoria/unknowns y siguiente trabajo delimitado |
| Un ejercicio | Dossier completo, alternativas, compatibilidad, revisión y validación futura |
| Una familia | Contrato común, variantes justificadas, diferencias reales y dossiers pertinentes |
| Un bloque/catálogo | Blueprint, cobertura, redundancias, selección/descarte, dossiers por lotes y mapa de preparación |
| Nueva auditoría | Handoff, delta actual/vNext, impacto por diseño, propuestas y decisiones |
| Revisión | Hallazgos contra fuentes, disposición, correcciones propias y gates aún abiertos |

Un alcance pequeño no necesita decenas de archivos de salida. Se pueden agrupar tablas y plantillas en un solo dossier si conserva sus contratos. Las fuentes completas se enlazan una vez; no se duplican para aumentar volumen.

## 2. Orden de presentación al CEO

Resultado principal y recomendación; evidencia y límites que cambian su interpretación; diseño/compatibilidad; hallazgos; decisiones concretas; siguiente fase autorizable. No esconder el resultado detrás de la narración de agentes.

Para cada decisión mostrar opciones comparables y el cambio preciso. La recomendación es una propuesta hasta aprobación. No hablar de «todos los gates verdes» cuando solo se revisó documentación.

## 3. Trazabilidad bidireccional

Cada requisito relevante debe enlazar a un elemento de diseño y a su forma de comprobación. Cada elemento costoso del diseño debe justificar qué requisito/hipótesis sirve. Usar IDs estables para requisitos, decisiones, estudios, constructos, familias, ejercicios, capacidades, hallazgos y pruebas.

Si no puede justificarse una complejidad, retirarla de la propuesta nueva o clasificarla como investigación con pregunta y gate. Cambiar complejidad del canon requiere consulta al CEO, no eliminación automática.

## 4. Estados y versionado

Conservar IDs; elevar versión cuando cambien demanda, reglas, contenido, dificultad, feedback, métrica, adaptación, elegibilidad o significado. Distinguir corrección documental y cambio semántico. No prometer compatibilidad de historia sin puente estudiado.

Guardar snapshot de fuentes y decisiones, estado de revisión, alternativas descartadas y pendientes. No modificar retroactivamente el dossier usado en un estudio; producir nueva versión con diferencias.

## 5. Checkpoint de reanudación

Registrar tarea solicitada, objetivo, alcance hecho/pendiente, rutas, hashes, decisiones aprobadas y propuestas, preguntas, hallazgos, pruebas realmente ejecutadas, agentes reales, dependencias y siguiente acción. No guardar razonamiento privado extenso ni datos personales innecesarios.

Al reanudar, comprobar fuentes cambiadas y decisiones nuevas. Si no cambió nada pertinente, continuar la tarea pendiente. Si cambió el motor, aplicar análisis de impacto. Si cambió la preferencia del usuario, conservar la anterior como histórica y pedir únicamente aclaraciones no resueltas.

## 6. Memoria final

Actualiza solo hechos/decisiones autorizados mediante el protocolo. Registra diseños nuevos como propuestos/revisados, no validados. No escribir resultados sintéticos como estudios humanos. Informar si el grafo no pudo actualizarse o quedó parcial.

## 7. Informe de completitud

Incluir `document_completion`, `design_disposition` y `readiness`; fuentes leídas y faltantes; verificación real y no realizada; decisiones pendientes; limitaciones; resultado de revisiones. Un resultado honesto y condicionado es preferible a una falsa certificación de «100/100».

No cerrar solo con una lista de ideas. El valor del sistema reside en que el siguiente equipo pueda implementar lo aprobado sin inventar reglas y que una evaluación independiente pueda demostrar que una hipótesis era falsa.
