# Claude Code Handoff (10/10 Strict Orders)

**Objetivo:** Construir SOLAMENTE las Fases 01 a 07 del MVP Local.

**Mandatos de Implementación:**
* DB local.
* Entidades: sesión, ejercicio, intento, score local, progreso.
* Motor determinista de ejercicios.
* Timer con señales anticheat (monotonic).
* Rúbrica local versionada.
* UI básica del loop competitivo.
* Persistencia offline.
* Tests unitarios de scoring determinista.

**Prohibiciones Absolutas:**
* No LLM.
* No embeddings externos.
* No gateway IA.
* No prompts con datos de usuario a modelos externos.

**Preparación de Arquitectura:**
* Dejar interfaces preparadas para Evaluator Cascade, Privacy Gateway, Cost Budget Gateway y Metric Integrity Layer (interfaces vacías o con mocks deterministas), pero NO implementarlas ni activarlas.
