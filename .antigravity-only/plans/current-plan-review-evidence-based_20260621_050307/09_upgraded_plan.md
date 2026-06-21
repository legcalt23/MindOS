# Upgraded Implementation Plan (10/10 Architecture)

**1. MVP LOCAL — GO**
Debe construirse primero y aislarse de la IA.
* Base de datos local, Ejercicios deterministas, Timer local con anticheat.
* Scoring local no clínico, Progress tracking, Rúbricas versionadas.
* SIN LLM OBLIGATORIO, sin datos sensibles filtrados.

**2. AI LAYERS — CONDITIONAL GO (Bloqueadas hasta Gates)**
* Evaluator Cascade (4 Capas: 0 Local Rules, 1 Local NLP, 2 Cheap Batch, 3 Frontier).
* Cost Budget Gateway.
* Privacy Gateway (Redacción de PII antes de evaluar).
* Metric Integrity Layer (Coherence, Dictionary spam).
* Prompt Injection Defense.
* Coaching Safety Layer (Tono formativo, 0 clínica).
* Friction Budget (Micro-recompensas para compensar carga cognitiva).

**3. COMPETITIVE SYSTEM — DEFINICIÓN 10/10**
1. No es pay-to-win.
2. Score robusto (anti-gaming y diccionario).
3. Score honesto (Intervalo de confianza).
4. Score privado.
5. Rentable (Cost budget).
6. Pedagógico (Fricción útil).
7. Auditable.
8. Reversible (Kill-switches).
9. Resistente (Tests adversarios).
10. Defendible.
