# Flujo de ejecución y alcance finito

## 1. Definir la unidad de resultado

Convertir la solicitud en [brief](../plantillas/01_BRIEF_Y_CONTRATO.md): objetivo, bloque/subhabilidad, población, modalidad, estado del motor, fuentes, salida pedida, cantidad y usos deseados. No empezar por un nombre comercial ni por una lista de paradigmas conocidos.

Si se invoca `/exer` sin detalles, recuperar el trabajo activo. Si no lo hay, Enfoque es el contexto inicial por la conversación adjunta, pero no implica diseñar automáticamente 120 ejercicios. Preparar mapa de entradas y siguiente alcance útil. Si falta la auditoría, completar contexto y propuesta de blueprint; no emitir un catálogo final «adaptado al motor actual».

La falta de un informe de auditoría no impide reconstruir hechos directamente desde el repo cuando el usuario ha autorizado la lectura y el encargo lo necesita. No ejecutar de forma encubierta la auditoría completa de 62 secciones; hacer una comprobación focalizada de capacidades y declarar su alcance.

## 2. Fases y salidas

| Fase | Trabajo | Salida / condición de avance |
|---|---|---|
| F0 Contexto | Fuentes, decisiones, memoria y snapshot | Brief, ledger y conflictos delimitados |
| F1 Modelo cognitivo | Definición operacional, límites, evidencia, mecanismos alternativos | Blueprint de constructos y criterios de selección |
| F2 Arquitectura de práctica | Comparar candidatas, cubrir huecos y descartar redundancias | Familia seleccionada o alternativas pendientes |
| F3 Dossier | Procedimiento, contenido, dificultad, dosis, feedback, medición y accesibilidad | Especificación completa para el uso declarado |
| F4 Compatibilidad | Requisitos frente al motor actual y vNext | Matriz y propuestas al CEO |
| F5 Revisión | Ciencia, práctica, adversarios, factibilidad e integridad | Hallazgos cerrados o bloqueo delimitado |
| F6 Entrega | Dossier, evidencia, plan empírico, decisiones y memoria | Resultado utilizable y checkpoint |

Revisar coherencia al terminar cada fase. No esperar al cierre para descubrir que el mecanismo depende de una capacidad inexistente del motor.

## 3. Comparación de alternativas

Para un mecanismo nuevo, estudiar normalmente 2–4 alternativas que difieran en la demanda real, no en apariencia. Comparar beneficio plausible, pureza suficiente, rango adaptativo, transferencia investigable, restricciones móviles y coste de complejidad. Registrar descarte y razón. Estos números organizan el trabajo; no son cuotas científicas ni impiden una alternativa necesaria adicional.

Elegir la opción más sólida dentro de las decisiones vigentes. Si elegirla requiere cambiar canon, conservar base y alternativa pendientes. No añadir complejidad solo porque un especialista pueda describirla. Una solución simple puede exigir práctica intensa y bien dirigida.

## 4. Estados del resultado

Mantener tres campos independientes:

- `document_completion`: COMPLETE / PARTIAL — si el alcance documental se ha cubierto.
- `design_disposition`: REVIEWED / CONDITIONAL / BLOCKED — si quedan decisiones o defectos del diseño.
- `readiness`: DESIGN_ONLY / IMPLEMENTATION_SPEC_CANDIDATE / EMPIRICAL_VALIDATION_PENDING — qué fase podría seguir y bajo qué autorización.

Son etiquetas de informe. No cambian el lifecycle del motor ni las elegibilidades. Un documento completo puede tener un diseño condicionado. Un diseño sólido sin datos puede ser candidato de implementación futura, sin ser entrenamiento validado u oficial.

## 5. Cierre acotado

Configuración operativa propuesta: revisión de requisitos, revisión sustantiva, revisión integrada independiente; después hasta dos rondas de corrección focalizada por los hallazgos existentes. No imponer más rondas si no queda riesgo concreto. Si después sigue un P0/P1 del diseño, cerrar la ejecución como BLOCKED/CONDITIONAL con causa, evidencia faltante y siguiente acción. No bajar severidad, ignorarlo o declarar perfección para cumplir un límite.

Reabrir solo con evidencia nueva, contradicción comprobada, cambio de requisitos, auditoría/versiones que afecte dependencias o fallo observado. No reabrir por sinónimos, otra opinión equivalente o una idea fuera de alcance. Las mejoras pequeñas que cambien canon se consultan igualmente; las mejoras de redacción propias se corrigen.

## 6. Clasificar las mejoras

| Momento | Ejemplo de razón |
|---|---|
| BEFORE_DESIGN_ACCEPTANCE | Constructo indefinido, procedimiento ambiguo, falta una decisión que altera la demanda |
| BEFORE_IMPLEMENTATION | Falta contrato de una capacidad o autorización de cambio del motor |
| BEFORE_OFFICIAL_MEASUREMENT | Comparabilidad, estimación o criterio de elegibilidad aún sin validar |
| BEFORE_CONFIRMATORY_STUDY | Outcome, potencia, control o análisis sin preregistrar |
| FUTURE_RESEARCH | Pregunta valiosa que no impide el uso actual declarado |

Todo elemento diferido lleva razón, dueño funcional, dependencia y criterio de reentrada. No convertir el backlog en una lista sin fin de deseos.

## 7. Continuidad

Guardar decisiones, hashes, estado de lectura, alternativas descartadas y dependencias. Al continuar, verificar el delta de fuentes antes de reusar la conclusión. No asumir que un cambio de nombre o fecha significa cambio de contenido. No reejecutar toda la investigación si la pregunta y sus fuentes siguen vigentes.
