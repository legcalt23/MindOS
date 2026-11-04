# Motor actual, auditoría y mejoras conceptuales

## 1. Tres realidades

Mantener separadas: `CURRENT_OBSERVED` (implementación examinada en snapshot concreto), `APPROVED_IMPLEMENTATION_UNKNOWN` (decisión vigente cuya implementación no se ha comprobado) y `PROPOSED_VNEXT` (alternativa pendiente del CEO). Reservar `APPROVED_NOT_IMPLEMENTED` para ausencia comprobada con alcance suficiente en un snapshot. Aprobación e implementación son campos independientes: puede existir código sin decisión aprobada o una decisión sin código observado. Lo reportado por una auditoría sin acceso a sus evidencias lleva además `REPORTED`.

No existe un motor «actual» verificable solo porque el documento se titule Current State. No inferir wiring, plataforma, tests ni comparabilidad de una clase, nombre, contrato o diagrama.

## 2. Recepción de la auditoría

Usar [handoff de auditoría](../plantillas/04_HANDOFF_AUDITORIA.md). Verificar que la entrada sea un informe de resultados y no un encargo/promesa. El TXT del paquete es un encargo. Registrar fecha efectiva, commit y cambios locales, alcance, archivos revisados, pruebas ejecutadas/omitidas, límites, decisiones propuestas/aprobadas y autor real.

Extraer: taxonomía actual y propuesta; cobertura y catálogo existente; métricas y linaje; dificultad/adaptación; personalización/dosis; integridad/elegibilidad; plataforma/accesibilidad; práctica/transferencia; bloqueos; unknowns y requisitos del siguiente diseño. Cada conclusión tiene una referencia directa y estado.

Una recomendación de la auditoría no queda aprobada por incluirse en el handoff. Una decisión aprobada no queda implementada por figurar en un roadmap. Si el informe dice «resuelto», verificar qué tipo de resolución es: documental, conceptual, código, test o evidencia empírica.

## 3. Contrato mínimo de capacidades

| Capacidad | Qué debe conocerse para depender de ella |
|---|---|
| Identidad/catálogo | IDs reales, versiones, pertenencia a bloque/familia y estados |
| Ejecución | Estados, acciones, eventos, determinismo, seed y manejo de cancelación |
| Contenido | Bancos/generadores, validadores, límites, exposición y formas |
| Dificultad | Parámetros expresables, políticas, frecuencia de actualización y registro |
| Tiempo | Reloj, onset/input, calidad, interrupciones y dispositivos observados |
| Métricas | Inputs, filtros, fórmulas, unidades, versiones y ausencia de doble autoridad |
| Evidencia | Finalidad, validación de sesión, comparabilidad y barreras |
| Perfil | Estimador y consumidores, nivel preliminar/oficial, sin reconstrucciones encubiertas |
| Datos | Sellado, subject, linaje, retención, offline, idempotencia y sincronización |
| UX | Estados de sesión, accesibilidad, feedback y condiciones que la interfaz puede expresar |

Si el trabajo es diseñar, basta especificar qué capacidad necesita y con qué semántica. Los nombres de módulos/rutas actuales solo se escriben si se observaron; las rutas propuestas se marcan como tales.

## 4. Evidencia histórica decisiva del paquete

`05 §10.1` describe un ingestor antiguo. `05 §10.4` y `09 §20.1` reportan reemplazo por un contrato evidence-only/facts-only. No diseñar contra `orderedNormalizedScores` o campos eliminados como si estuvieran disponibles. El contrato documentado conserva hechos, no estimaciones/tendencias que puedan reconstituir perfil de forma indirecta.

`09` informa que comparabilidad no establecida impide agregación; `Bridged(version)` no equivale a un puente implementado; metadatos de criticidad RT ausentes llevan a comportamiento conservador. Focus/Logic permanecen preliminares por ADR abiertos. El global oficial está bloqueado y su barrera técnica referencia once dimensiones internas; el producto tiene ocho motores visibles. Mapear, no equiparar.

Las correcciones de stores y purga posteriores están reportadas en `09`; no resucitar problemas ya cerrados documentalmente ni ejecutar su migración desde /exer. Tampoco afirmar que esas correcciones siguen vigentes sin repo. La auditoría nueva manda sobre la descripción de qué código existe cuando aporta evidencia, no sobre decisiones de producto por defecto.

## 5. Matriz de compatibilidad por ejercicio

Para cada requisito incluir capacidad/versión necesaria, capacidad actual observada, fuente/snapshot, estado y degradación permitida:

- `COMPATIBLE_OBSERVED`: contrato expresable con evidencia suficiente para esa afirmación; no prueba eficacia.
- `CONDITIONAL`: funciona bajo condiciones explícitas o una parte sigue sin verificación.
- `REQUIRES_PROPOSED_CHANGE`: necesita una mejora aún no aprobada/implementada.
- `UNKNOWN`: no se conoce la capacidad real.
- `INCOMPATIBLE`: hay evidencia de incompatibilidad.

Un ejercicio puede ser compatible para entrenamiento y no para medición. Un fallback no puede cambiar el mecanismo o una política sin consulta. Si el fallback válido es training-only, explicarlo sin presentarlo como derrota del objetivo de diseño.

## 6. Comparar presente y futuro

Cuando una limitación importa, estudiar:

1. Mejor diseño viable con las capacidades comprobadas actuales.
2. Mejor diseño con la mejora mínima necesaria propuesta.
3. Alternativa de investigación solo si responde a una incertidumbre importante.

Comparar beneficio plausible, evidencia, impureza, cobertura, coste de complejidad, datos/privacidad, validación, accesibilidad y compatibilidad de historial. No rebajar el objetivo científico silenciosamente para ajustarse al motor. No convertir el diseño en una reescritura del motor.

Toda mejora propuesta lleva ID, contrato antes/después, justificación, alternativas, impacto por dossier, prioridad, dependencia, gates, migración conceptual, reversibilidad, incertidumbre y pregunta al CEO. La decisión «aprobar diseño de mejora» no autoriza automáticamente implementarla.

## 7. Actualización después de una auditoría

Comparar snapshot/decisiones anteriores y nuevos. Calcular dependencias transitivas: constructo→familia→parámetro/métrica→ejercicio→condición→claim/validación. Marcar únicamente los objetos afectados como `REVIEW_REQUIRED` con razón.

No borrar un dossier anterior ni renombrarlo como nuevo sin historial. Versionar cuando cambie significado. Si cambia solo una cita sin efecto semántico, registrar corrección documental; si cambia el fundamento del diseño, revisar la conclusión.

## 8. Contrato de salida

Entregar handoff normalizado, mapa actual/vNext, matriz de compatibilidad, propuestas, decisiones pendientes y lista de dossiers por revisar. Si faltan resultados de auditoría o repo, decirlo. No reemplazar la evidencia faltante por una arquitectura ideal descrita en presente.
