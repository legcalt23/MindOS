# 14 Evidence Manifest (Hard Gates Execution Registry)

Este manifiesto registra y audita el estado de validación de cada gate de seguridad y calidad técnica diseñado para MindBoost. Ninguna funcionalidad de riesgo (IA, competitiva en red, etc.) puede ser liberada o activada en runtime sin que su respectivo gate esté en estado `PASSED` respaldado por evidencia ejecutada.

## Registro de Gates

| Gate | Estado | Evidencia requerida | Responsable | Comando/Test | Resultado | Bloquea |
| ---- | ------ | ------------------- | ----------- | ------------ | --------- | ------- |
| **G01: Adversarial Metric Suite** | `DESIGNED` | Reporte con 200+ ataques adversarios (dictionary spam, random text, semantic evasion). Tasa de detección > 95%, falsos positivos < 5%. | QA / Red-Team | `./gradlew :shared:test --tests "*MetricIntegrityAdversarialTest*"` | `PENDING` | AI Layers Activation & Competitive Live Scoring |
| **G02: Codex Security Audit (Timer/Payload)** | `DESIGNED` | Reporte de auditoría estática firmado por Codex validando resistencia a tampers de reloj, background pauses, y manipulación de payload offline. | Codex Security | `codex-security-scan --target shared/src/commonMain/kotlin/com/mindos/app/features/mindboost/focus/data/local/` | `PENDING` | Offline MVP Deployment and Sync Release |
| **G03: Frontier LLM Cost Simulation** | `DESIGNED` | Ejecución exitosa de 10.000 sesiones simuladas en backend para validar costes y latencias acumulados en el Evaluator Cascade. Coste P95 < $0.05/sesión. | Backend / Cloud | `npm run simulate-cascade-costs -- --sessions=10000` | `PENDING` | AI Cascade Production Release |
| **G04: Metric Integrity Layer Attacks** | `DESIGNED` | Reporte de suite adversaria bloqueando inyección de prompts locales y spam de vocabulario. | Red-Team | `./gradlew :shared:test --tests "*MetricIntegrityLayerTest*"` | `PENDING` | Competitive Rating Production Release |
| **G05: Privacy Gateway DLP Canaries** | `DESIGNED` | Validación del 100% de redacción de PII (teléfonos, emails, secretos canarios) insertados deliberadamente en payloads de prueba. | Privacy Lead | `npm run test-privacy-gateway-canary` | `PENDING` | Client Data Transmission to LLM Backend |
| **G06: Privacy Gateway Verification** | `DESIGNED` | Auditoría de tráfico de red local confirmando que no se transmiten textos en crudo sin tokenizar / sanitizar. | Security Lead | `npm run test-privacy-gateway-enforcement` | `PENDING` | Client Data Transmission to LLM Backend |
| **G07: Coaching Safety Assessment** | `DESIGNED` | Evaluación de 500 respuestas generadas por el modelo de IA. 0 incidencias de lenguaje clínico, despectivo o inapropiado. | Cognitive Safety | `python scripts/safety/eval_coaching_safety.py --dataset inputs_safety.json` | `PENDING` | AI Coach / Feedback Layer Activation |
| **G08: Telemetry Friction Budget** | `DESIGNED` | Registro de retención en Beta Cerrada validando que la fatiga/fricción cognitiva no aumente el churn rate D1/D7 de forma anómala. | Product Lead | `python scripts/analytics/eval_friction_budget.py` | `PENDING` | Full Beta Release and Scaling |
| **G09: Hard Execution Gates Verification** | `DESIGNED` | Cobertura de tests unitarios confirmando que fallos de DLP o Cost Gateway cortocircuitan (hard break) la ejecución en runtime de la llamada IA. | EM Platform | `./gradlew :shared:test --tests "*HardExecutionGatesTest*"` | `PENDING` | Production Deployment of any AI features |
| **G10: Pre-Gate AI Inactivity** | `DESIGNED` | Verificación estática y dinámica de que las llamadas a la IA están desactivadas por defecto mediante Feature Flags y no se ejecutan en el MVP Local. | Engineering Lead | `python scripts/security/verify_no_active_ai_pre_gate.py` | `PENDING` | Local MVP Offline Release |

---

## Definición de Estados Permitidos
* **DESIGNED**: El gate está definido arquitectónicamente y se conoce la evidencia que debe presentar. (Estado inicial por defecto).
* **IMPLEMENTED**: El arnés de prueba, comando o suite de verificación está construido pero no se ha ejecutado de forma oficial.
* **TESTED**: El comando o test ha sido ejecutado, generando evidencias.
* **PASSED**: El test ha sido superado con éxito cumpliendo todos los umbrales exigidos. Requiere log y hash de evidencia.
* **FAILED**: El test se ha ejecutado pero no cumple los criterios de aceptación. La feature afectada está bloqueada.
* **BLOCKED**: No se puede ejecutar el test del gate porque depende de un componente que aún no está construido.
* **WAIVED_BY_HUMAN**: Un gate omitido de forma explícitamente por firma humana autorizada. (Requiere justificación técnica y firma en el log).

---

## Historial de Revisiones e Inspecciones

*Ninguna inspección registrada todavía. Runtime pendiente de pruebas.*
