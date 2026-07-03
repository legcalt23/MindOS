# Especificacion del evidence ledger

## Proposito

El ledger conserva trazabilidad minimizada de tareas de seguridad sin almacenar prompts, secretos, PII ni contenido de evidencia. El formato es JSON Lines append-only y se escribe bajo `.security-state/evidence-ledger.jsonl`, directorio ignorado por Git.

## Registro permitido

```json
{
  "schema": "mindos.security.evidence-ledger/v1",
  "timestamp_utc": "2026-06-21T10:00:00Z",
  "task_id": "SEC-20260621-001",
  "agent": "appsec_auditor",
  "event": "receipt_validated",
  "artifact_type": "agent_receipt",
  "artifact_hash": "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
  "relative_path": "build/security-evidence/receipt.json",
  "result": "needs_review",
  "approval_refs": [],
  "notes": "metadata-only"
}
```

Campos requeridos: `schema`, `timestamp_utc`, `task_id`, `agent`, `event`, `artifact_type`, `artifact_hash`, `result`. `relative_path`, `approval_refs` y `notes` son opcionales y deben estar saneados.

## Reglas

- `artifact_hash` usa SHA-256 y nunca contiene el artefacto.
- Las rutas son relativas, canonicas, dentro del workspace y no apuntan a nombres sensibles.
- No se permiten campos libres como `prompt`, `content`, `secret`, `token`, `email`, `user`, `raw_log` o `command_output`.
- `notes` se limita a una etiqueta no sensible; no admite saltos de linea.
- El archivo se abre con exclusividad y solo se anexa. No se trunca, reescribe ni borra.
- El ledger no es una cadena de custodia juridica por si solo; para DFIR se requiere almacenamiento WORM y procedimiento aprobado fuera del repo.

## Retencion

El repositorio no automatiza retencion ni borrado. El propietario define retencion fuera de Git según necesidad legal y minimizacion. Si un registro contiene datos indebidos, se bloquea la automatizacion y se escala; no se reescribe silenciosamente el ledger.
