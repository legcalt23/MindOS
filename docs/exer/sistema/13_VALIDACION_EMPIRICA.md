# Validación futura del entrenamiento

## 1. Separar preguntas

¿Se puede ejecutar? ¿Se entiende? ¿La persona usa la operación prevista? ¿La medida es suficientemente precisa para ese uso? ¿La práctica causa mejora? ¿Se generaliza? ¿Se mantiene? Cada pregunta requiere evidencia diferente. `/exer` diseña el programa de validación, no lo ejecuta con usuarios ni otorga sus resultados.

## 2. Programa escalonado

| Etapa | Pregunta principal | Evidencia futura | Qué no permite concluir |
|---|---|---|---|
| Lógica y simulación | ¿Las reglas, parámetros y métricas son coherentes? | Casos sintéticos, propiedades, escenarios adversarios | Beneficio humano |
| Verificación móvil | ¿La implementación presenta/recoge lo definido? | Timing/dispositivos, offline, accesibilidad, recuperación | Validez cognitiva por compilar |
| Alpha de comprensión | ¿Se entiende y se practica lo previsto? | Observación, think-aloud fuera de medición, errores, carga, abandono | Eficacia causal o normas |
| Calibración psicométrica | ¿Qué significado y precisión tiene la medida? | Formas, exposición, fiabilidad/error, validez, subgrupos/dispositivos | Transferencia por una correlación |
| Prueba de eficacia | ¿La intervención causa cambio en outcome independiente? | Comparación controlada/preregistrada con análisis adecuado | Generalización más allá del estudio |
| Durabilidad y funcionamiento | ¿Persiste y ayuda fuera de la tarea? | Follow-up y outcomes funcionales independientes | Beneficio universal |

El canon propone alpha de aproximadamente 20 adultos durante 14–30 días para QA/usabilidad y un objetivo a 30 días con seguimiento a 90. Son decisiones/planes de producto; no sustituyen potencia o precisión para cada pregunta. No hacer que todos los participantes prueben 120 ejercicios sin considerar carga ni llamar a esa cobertura validación exhaustiva.

## 3. Protocolo de proceso de respuesta

Antes de buscar efectos, observar cómo la persona entiende la consigna, qué mira, qué recuerda, qué estrategia usa y por qué falla. Think-aloud y preguntas pueden alterar la tarea: usarlos en una fase separada de las mediciones que requieran condiciones controladas. Incluir principiantes, variación relevante de dispositivos y barreras de acceso.

Definir evidencias que harían rediseñar: estrategia dominante que evita el objetivo, confusión persistente, techo/suelo, carga irrelevante, error sistemático por interfaz, efectos de modalidad no etiquetados. No seleccionar solo usuarios que entiendan rápido.

## 4. Medición para el uso previsto

Planificar error y fiabilidad, equivalencia de formas, dificultad, dependencia local, efecto de práctica, precisión de diferencias/cambio y sensibilidad a la condición experimental. Elegir estimadores y tamaño según el uso; no fijar ICC/SEM/MDC universales.

Investigar convergencia y discriminación con familias suficientemente diferentes. Separar grupos por los factores relevantes sin estereotipar. Examinar invariancia o sesgo cuando se comparen resultados por edad, educación, cultura, dispositivo o accesibilidad. Si no puede sostenerse la comparación, restringir la interpretación.

## 5. Estudio causal

Antes de reclutar, especificar población, exclusiones, intervención, comparador activo creíble, asignación, unidad de aleatorización, duración, seguimiento, outcome primario independiente, estimando, análisis y criterio de relevancia práctica. Distinguir tiempo ofrecido, tiempo practicado, práctica válida y adherencia; no excluir abandonos para mejorar resultados.

Igualar expectativas/contacto/carga cuando sea posible sin entrenar inadvertidamente el mismo mecanismo que se quiere contrastar. Reconocer límites del cegamiento de participantes; cegar evaluadores/analistas cuando sea viable. Registrar expectativas si el protocolo lo justifica.

Fijar potencia o precisión con supuestos explícitos de efecto mínimo relevante, varianza, correlaciones, multiplicidad, agrupación y attrition. Si faltan supuestos, diseñar piloto de estimación y sensibilidad; no elegir muestra después de ver significación. Predefinir análisis intention-to-treat, datos faltantes, exclusiones técnicas, desviaciones y análisis secundarios/exploratorios.

No usar eficacia comparativa de profesional/sencilla como hecho anterior al estudio. Si interesa equivalencia/no inferioridad, establecer margen justificado y análisis apropiado; una diferencia no significativa no demuestra igualdad. Menor carga puede modificar adherencia y dosis total recibida; medir ambas antes de interpretar.

## 6. Personalización y causalidad individual

La adaptación produce datos seleccionados por la propia política. No atribuir diferencias de resultado a una familia ignorando por qué se la asignó. Registrar política y asignaciones suficientes para auditar decisiones.

Evaluar primero reglas transparentes y efecto medio con heterogeneidad prudente. Diseños N-of-1 o cruzados pueden sufrir aprendizaje persistente, carryover y práctica; no presentarlos como solución universal. Proponer experimentación adaptativa más sofisticada solo si una pregunta concreta la exige y se cuenta con datos/validación.

## 7. Transferencia cotidiana y reporte

Operacionalizar VEPR-30 antes de usarlo: dominio funcional, instrumento, umbral de cambio fiable/relevante, periodo, denominador, missingness y guardrails. No es causal por ser un outcome cotidiano. Incorporar seguimiento de 90 días cuando corresponda al protocolo; no afirmar durabilidad antes.

Reportar aleatorizados/elegibles/incluidos/analizados, tamaños de efecto e intervalos, cambios absolutos, attrition, exposición, limitaciones, resultados negativos, desviaciones y conflictos de interés. Reconciliar versiones usadas. No sustituir el outcome primario por el secundario que salió mejor.

## 8. Gate de propuesta empírica

Usar [plan de validación](../plantillas/08_PLAN_DE_VALIDACION.md). El diseño debe indicar qué resultado lleva a mantener, modificar, restringir a entrenamiento, repetir bajo mejor medición o retirar una hipótesis. Las acciones con participantes, datos reales, consentimiento o publicación pertenecen a una fase autorizada aparte.
