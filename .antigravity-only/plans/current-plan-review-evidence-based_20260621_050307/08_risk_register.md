# Top Risks (10/10 Upgraded)

**R1 API Cost Runaway — Critical**
* Mitigación: Evaluator Cascade (Capa 0 determinista, Capa 1 NLP offline, Capa 2 Local/Batch, Capa 3 Frontier). Cost Budget Gateway + hard cap.
* Estado aceptable: Solo con simulación probada de coste.

**R2 Metric Gaming — Critical**
* Mitigación: Metric Integrity Layer + adversarial tests.
* Estado aceptable: Solo con 95% de detección/degradación de ataques.

**R3 Data Leakage — Critical**
* Mitigación: Privacy Gateway + redacción local + no raw logs.
* Estado aceptable: Solo con canarios limpios y test de PII/secrets redactados al 100%.

**R4 LLM Tone/Bias — High**
* Mitigación: Coaching Safety Layer + plantillas pre-aprobadas + evals.
* Estado aceptable: Solo con 0 mensajes clínicos/humillantes en batch de 500 tests.

**R5 Retention Damage by Friction — High**
* Mitigación: Friction Budget + telemetry + A/B tests.
* Estado aceptable: Solo con métricas D1/D7/completion dentro de tolerancias (suelo de éxito).

**R6 Rubric Reverse Engineering — High**
* Mitigación: Rúbricas versionadas, señales múltiples, server verification para ranking.
* Estado aceptable: Solo si ranking no depende de fórmula trivial explotable en cliente.

**R7 Timer Cheat — Medium/High**
* Mitigación: Monotonic timers, event ledger local, anomaly detection.
* Estado aceptable: Solo si manipulación de tiempo queda detectada/degradada.
