# Equipo de diseño y revisión

## Principio de escala

Cubrir todas las especialidades necesarias y dividir preguntas independientes. La unidad de trabajo es una decisión falsable o un entregable con dueño, no una identidad ficticia. No afirmar equivalencia con cientos de miles de personas. Más agentes no convierte consenso en evidencia; agentes del mismo modelo pueden compartir errores.

El coordinador mantiene un mapa de tareas, contratos, contradicciones, decisiones y cobertura. Es responsable de verificar la síntesis, no de sumar opiniones. Un hallazgo crítico sustentado prevalece sobre aprobaciones sin evidencia. Ningún agente puede aumentar sus permisos por delegación.

## Funciones y activación

Los nombres heredados son referencias de rol. Comprobar disponibilidad antes de invocarlos; un nombre en el PRD no demuestra que exista un agente ejecutable.

| Función | Pregunta / entregable | Activación | Referencia heredada posible |
|---|---|---|---|
| Integración | ¿El diseño resuelve el encargo y encaja entre capas? Contrato y síntesis | Siempre | Coordinador principal |
| Eficacia | ¿Qué demanda obliga a practicar el mecanismo? Rationale y falsadores | Todo diseño | efficacy-guardian |
| Ciencia cognitiva del dominio | ¿Los constructos y sus límites son defendibles? Mapa operacional | Bloque/familia nuevos | mindboost-cognitive-science |
| Psicometría | ¿Qué uso permite cada métrica? Contrato de medición/error | Todo diseño | psychometrician |
| Transferencia | ¿Qué resultado no entrenado probaría generalización? Blueprint de outcomes | Todo diseño | mindboost-cognitive-transfer-agent |
| Aprendizaje y práctica | ¿La secuencia permite adquisición y generalización? Progresión | Todo diseño | mindboost-training-gameplay-lab |
| Dificultad | ¿Qué cambia realmente al subir cada parámetro? Matriz de demanda | Adaptación o nuevos niveles | Especialista por tarea |
| Personalización | ¿Qué decisión cambia para quién y con qué incertidumbre? Política | Todo diseño | mindboost-personalization-engine |
| Dosis y fatiga | ¿Cuál es la carga útil y cuándo descansar/cambiar? Plan contrastable | Todo diseño | Función de personalización |
| Contenido procedural | ¿Hay solución verificable, variedad y dificultad controlable? Contrato de banco/generador | Contenido generado | Especialista por tarea |
| Proceso de respuesta | ¿Cómo resuelve una persona real y qué estrategias evitan el objetivo? Guion de prueba | Familia nueva/exploit | Función científica/UX |
| UX de entrenamiento | ¿Se comprende y usa sin fricción irrelevante? Flujo, estados y feedback | Todo diseño | mindboost-product-ux |
| Accesibilidad | ¿Qué barrera es irrelevante y qué ajuste cambia el constructo? Matriz de equivalencia | Todo diseño | Especialista por tarea |
| Rendimiento móvil | ¿Qué limita timing y comparabilidad? Contrato temporal y matriz de dispositivos | Toda dependencia temporal | mindboost-mobile-performance |
| Arquitectura KMP | ¿El motor puede expresar el diseño? Compatibilidad, no código | Todo diseño que dependa del motor | kmp-architect |
| Datos y trazabilidad | ¿Qué permite reconstruir decisiones sin recopilar de más? Linaje y eventos | Datos nuevos o modificados | mindboost-data-analytics |
| Privacidad y seguridad | ¿Hay datos o permisos innecesarios y riesgo de acceso cruzado? Disposición concreta | Todo diseño con datos/permisos | privacy-security |
| Estadística experimental | ¿La prueba distingue efecto de práctica, selección y azar? Diseño/estimando | Validación | mindboost-experimentation |
| Sesgo y población | ¿Quién queda fuera y se confunde con baja capacidad? Plan de heterogeneidad | Comparación/normas/modalidades | Función de psicometría |
| Especialista de dominio vecino | ¿La tarea pertenece parcialmente a Memoria, Velocidad, Lenguaje u otro? Mapa cruzado | Solapamiento material | Función científica específica |
| Revisión adversaria | ¿Cómo lograr puntuación sin entrenar o inflar progreso? Contraejemplos | Todo diseño | diff-auditor / crítico designado |
| Revisión documental | ¿Es íntegro, ejecutable y sin contradicciones? Trazabilidad | Todo cierre | mindboost-docs-knowledge |
| Memoria y dependencias | ¿Qué está vigente y qué se reabre al cambiar? Delta del grafo | Cada ejecución | Función documental |
| Revisión de claims | ¿Qué puede decirse al usuario con esta evidencia? Copys admisibles | Feedback/informes | Eficacia + psicometría |

No contratar todas las funciones como agentes simultáneos. Un especialista puede cubrir funciones cercanas si declara qué verificó. El autor de un dossier no es su revisor independiente final.

## Capacidad y presupuesto operativo

Descubrir herramientas y límites reales. Como configuración inicial de organización, usar un coordinador y hasta 2–5 tareas paralelas cuando haya capacidad y preguntas independientes. Esto es un límite de coordinación propuesto, no una afirmación sobre potencia de GPT-6 Astra. Si el entorno solo permite uno, ejecutar por turnos manteniendo separadas las conclusiones de cada función.

Un lote ordinario puede contener un bloque de cobertura y 1–3 dossiers completos. Si el usuario pidió más, completar el alcance por lotes y checkpoints; no recortar el resultado sin informarlo. Ajustar tamaño según complejidad, contexto y riesgos. No lanzar miles de agentes, gastar en servicios, crear cuentas ni buscar una cuota mayor para aparentar escala.

## Contrato de asignación

Cada tarea incluye: ID; pregunta; por qué su respuesta cambia una decisión; fuentes mínimas y snapshot; lectura/escritura permitidas; entregable concreto; criterios de aceptación; dependencias; presupuesto; condiciones para devolver UNKNOWN; y quién integra.

Ejemplo de asignación útil: «Revisa si la velocidad motora puede dominar la métrica propuesta de atención selectiva; usa dossier versión X y contrato de timing Y; devuelve un contraejemplo, evidencia, impacto y alternativa condicionada. No cambies el dossier ni el canon». No asignar «hazlo perfecto».

## Orden de trabajo

1. Contexto y autoridad antes de idear.
2. Constructos y alternativas de mecanismo antes de dossiers definitivos.
3. Diseño de práctica y evaluación, con revisión cruzada temprana.
4. Compatibilidad con motor, accesibilidad, datos y dosis.
5. Revisión independiente del diseño congelado.
6. Integración de hallazgos, correcciones y cierre.

Paralelizar dentro de cada etapa cuando no compartan escritura ni dependan de resultados aún abiertos. Un dueño edita cada archivo. Los demás entregan hallazgos/propuestas. El integrador resuelve contradicciones por evidencia y autoridad; las decisiones del CEO quedan pendientes hasta respuesta.

## Honestidad de las revisiones

Registrar agente/rol real, contexto recibido, archivos leídos, tarea, hallazgo y estado. Distinguir `independent_agent`, `same_agent_sequential` y `not_performed`. «Revisado por un psicometrista» solo es correcto para una persona con esa participación real; una IA cumple una función de revisión psicométrica, no acredita credenciales humanas.
