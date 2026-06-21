# Codex Security & Anti-Cheat Handoff

**Objetivo:** Revisión defensiva estricta en modo read-only. No realizar pentest ofensivo externo. No tocar producción.

**1. Timer Cheat Audit**
* Analizar manipulación de reloj local.
* Analizar pause/resume abuse y background/foreground abuse.
* Auditar reinicio de sesión y replay de eventos.

**2. Reverse Engineering de Rúbrica**
* Revisar thresholds hardcoded.
* Analizar fórmulas explotables en cliente.
* Auditar protección contra ranking fraud.

**3. Prompt Injection en Functional Transfer**
* Threat model de inyección de prompt (incluso con IA apagada).
* Revisar sanitización de inputs y separación datos/instrucciones.
* Validar ausencia de raw logging y correcto funcionamiento de Data Loss Prevention (DLP).
