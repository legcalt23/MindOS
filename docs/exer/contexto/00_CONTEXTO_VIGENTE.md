# Contexto reconciliado para /exer

Fecha de preparación: 2026-09-15. Este archivo interpreta los adjuntos para el encargo actual; no reemplaza el canon vivo ni acredita acceso al repositorio/grafo. Fuentes completas en [manifiesto](03_MANIFIESTO_FUENTES.md).

## 1. Hechos del encargo actual

El usuario pide un ZIP de Markdown para que GPT-6 Astra prepare un comando `/exer` en Codex. Su finalidad es diseñar los mejores ejercicios cognitivos posibles para Molvex, app iOS/Android, con entrenamiento profesional, una modalidad sencilla también seria, especialización profunda, revisiones continuas y alcance coherente. No pide implementar ejercicios ni cambios del motor.

Se debe revisar el grafo de proyecto Graphify+Obsidian y actualizar hechos obsoletos cuando haya acceso. Si otra manera de hacer algo modifica una decisión/instrucción vigente, por pequeña que sea la mejora, hay que preguntarle antes de adoptarla. Las mejoras del motor se diseñan en teoría para que el CEO decida cuáles implementar.

## 2. Decisiones explícitas de la conversación aportada

En [10_CONVERSACION_AUDITORIA_ENFOQUE.md](fuentes/10_CONVERSACION_AUDITORIA_ENFOQUE.md), al comienzo, el usuario respondió **1B 2B 3A 4A**. Su significado:

| Decisión | Interpretación vigente para este paquete | Límite |
|---|---|---|
| 1B | Molvex es el nombre actual; MindBoost es legado | No autoriza renombrar IDs/rutas o migrar código |
| 2B | Personalización individual condicionada por evidencia y objetivos, manteniendo robustez poblacional | No optimizar sobre ruido ni ocultar resultados poblacionales |
| 3A | Modalidad relajada/sencilla dentro del mismo entrenamiento serio | No crear un producto Game separado ni métricas incompatibles sin etiqueta |
| 4A | Núcleo cognitivo determinista/algorítmico sin IA generativa; posible IA Elite futura | La IA no decide scores, evidencia oficial ni verdad cognitiva |

El mensaje actual enfatiza el usuario promedio y exige consulta incluso para cambios pequeños. Mantener la compatibilidad entre eficacia poblacional y personalización individual. Si una elección obliga a cambiar su prioridad, consultar el caso concreto. No reabrir las cuatro decisiones ya respondidas.

## 3. Requisitos de producto y ciencia conservados

- Adultos 18+, inicialmente España y español de España; principiantes incluidos. Estudiantes y emprendedores son personas prioritarias, no baremos de capacidad. PRD §§5–6; validación §4.
- Ocho motores visibles: Enfoque, Decisiones, Lógica, Creatividad, Lenguaje, Emociones, Velocidad mental y Memoria. No son ocho factores independientes. PRD §6.
- Objetivo de catálogo ≥15 ejercicios completos por bloque y ≥120 total. No obliga a diseñarlos todos por invocación ni permite skins como familias nuevas. PRD §7; estándar §2.
- Profesional/sencillo, Free/Elite y entrenamiento/medición son ejes distintos. Free no se degrada cognitivamente por monetización. PRD §§7,15,25; Decision Log BUS-001.
- Planes de 2, 5, 10, 20 y 30 minutos, sujetos a la duración útil de cada ejercicio. PRD §9; estándar §7.4.
- Training y medición separados; contenido, exposición, versiones, integridad y comparabilidad controladas. Charter §§6–7,13; estándar §§7–11.
- Performance Score y Growth Index separados; ausencia no es cero; XP/racha/suscripción/volumen no son capacidad. Charter §§9–12; Decision Log SCO-001…015.
- Mínimo prudencial de calibración: 5 sesiones oficiales válidas, 3 días y ≥48 h, con cobertura/integridad/comparabilidad y evidencia adicional cuando proceda. No prueba mejora ni suficiencia psicométrica. Charter §§11.1–11.2; PRD §11.5.
- KMP/Compose, offline, common-first, determinismo/versionado, paridad móvil. Mínimos documentados API 26/iOS 15. Estado técnico §§3–5. Versiones reales y soporte actual requieren inspección posterior.
- Datos cognitivos privados, minimización, contexto subjetivo local por defecto; no modifica directamente capacidad oficial. Consentimientos por finalidad. PRD §18; estado técnico §13.
- Alpha ≈20 adultos, QA/usabilidad; no validación causal. Beta/calibración según potencia/precisión apropiadas. Transferencia cotidiana y seguimiento requieren pruebas independientes. Plan de validación §§7,10–14.
- Presupuesto adicional de desarrollo histórico 0 €. No iniciar servicios de pago ni herramientas extras para ejecutar /exer. Charter CEO §13; PRD/operación.

## 4. Contexto técnico con vigencia limitada

La fuente `05` mezcla arquitectura TARGET, declaraciones y unknowns. `09` incluye actualizaciones posteriores relevantes. Todas las afirmaciones de código siguientes son **reportes documentales históricos**, no comprobaciones actuales:

| Tema | Qué documentan las fuentes | Consecuencia para /exer |
|---|---|---|
| Canon | `docs/context_pack` único canon trazable; `docs/context-pack` superseded; `PHASE0_FOCUS_FINAL` inmutable. `09 §21`, 2026-07-17 | Verificar decisión viva; preservar snapshots |
| Ingestor | El antiguo ingestor fue reemplazado por evidence-only/facts-only. `05 §10.4`; `09 S6R3-001/004` | No depender de scores/ratios eliminados |
| Comparabilidad | `ComparabilityNotEstablished`, `Incompatible`, `Bridged(version)` con puente inalcanzable/fail-closed en el reporte | No promediar ni inventar puente por existir el tipo |
| Perfil Focus/Logic | Sigue preliminar por ADR-SCI-003/008. `09 S6R3-008` | No presentar oficialidad ni desbloquearla |
| Global | Barrera única reportada; oficial bloqueado hasta ADR-SCI-006 y requisitos; once dimensiones internas. `09 S6R3-009` | Mapear con ocho motores; no equiparar ni activar |
| Stores/purga | Actualizaciones posteriores en `09 S6R3-002/003/007` | No repetir como pendientes estados antiguos sin verificar; no ejecutar migración |
| iOS/gates | Documentos conservan límites de verificación | No declarar build/timing/paridad probados |

## 5. Decisiones científicas abiertas relevantes

Según `09 §19` y §20.1, necesitan resolución según su uso: subhabilidades/agregación y speed–accuracy por familia; estimador longitudinal; error/trend/cambio fiable; dosis oficial/exposición; global/pesos; normas/rangos; puentes de dispositivo/modalidad/accesibilidad; VEPR-30 y diseño confirmatorio. No cerrar ADR-SCI-003…008 con constantes plausibles por escribir un ejercicio.

Un diseño puede avanzar con contratos condicionados o training-only. Medición oficial, ranking y claims permanecen bloqueados cuando dependen de esas decisiones/evidencias.

## 6. Entradas ausentes

No se han aportado: repo real, snapshot actual del motor, vault de Obsidian, índice/configuración de Graphify, informe de resultados de la auditoría de Enfoque, handoff ejecutado, datos humanos de Molvex o cliente de Codex donde instalar `/exer`.

El paquete puede preparar el sistema completo y sus reglas. La instancia receptora debe localizar estas entradas dentro de su acceso autorizado. Si no están, entrega integración pendiente y trabajo conceptual válido; no afirma inspección, actualización o compatibilidad ya verificadas.
