# Matriz RBAC de agentes Codex

Leyenda: R = lectura; W = escritura local aprobada; T = prueba local no mutante; A = requiere aprobacion humana; D = denegado.

| Agente | Nivel | Codigo/config | Docs | Tests | Red | Secretos | Aprueba | Verifica escrituras |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| security_orchestrator | A0 | R | R | D | D | D | D | D |
| security_lead_ciso | A0 | R | R | D | D | D | Acepta riesgo humano asistido | D |
| appsec_auditor | A1 | R | R | T | D | D | D | D |
| secure_code_remediator | A2 | W+A | W+A | T+A | D | D | D | D |
| threat_modeling_agent | A1 | R | R | T | D | D | D | D |
| dependency_supply_chain | A1 | R | R | T offline | D | D | D | D |
| security_qa_verifier | A1 | R | R | T | D | D | D | Si, independiente |
| documentation_sanitizer | A0 | R | R | DLP | D | D | D | D |
| incident_response_commander | A1 | R | R | T | D | D | D | D |
| red_team_lab_authorized | A3 | R | R | Laboratorio con RoE | Denegada por defecto | D | Doble A | D |
| cloud_security_auditor | A1 | R | R | T offline | D | D | D | D |
| iam_secrets_crypto_auditor | A1 | R sin valores | R | T | D | Solo metadatos | D | D |
| devsecops_cicd_auditor | A1 | R | R | T offline | D | D | D | D |
| detection_engineering | A1 | R | R | T | D | D | D | D |
| blue_team_soc | A1 | R | R | T | D | D | D | D |
| forensics_agent | A1 | R | R | T estatico | D | D | D | D |
| compliance_privacy_agent | A1 | R | R | T | D | D | D | D |

## Reglas de enforcement

1. Solo `secure_code_remediator` puede presentar `files_written` no vacio.
2. El Remediator requiere task envelope A2, rutas exactas aprobadas y writer lock.
3. El Remediator entrega siempre a `security_qa_verifier`; su receipt nunca declara `verified`.
4. El verificador rechaza cambios sin diff, pruebas, hashes y procedencia; no puede escribir.
5. Los agentes A0/A1 producen propuestas, findings y receipts, no parches aplicados.
6. El agente A3 no obtiene escritura, red ni acceso a secretos por el mero hecho de tener doble aprobacion. Cada capacidad se autoriza expresamente en las RoE y queda fuera del flujo normal del repo.
7. Los 51 manifiestos legacy quedan fuera de esta matriz y denegados por defecto.
