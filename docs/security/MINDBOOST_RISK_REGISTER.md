# MindBoost — Risk Register

Fecha de corte: 2026-06-20

| ID | Riesgo | Severidad | Prob. | Impacto | Estado | Control/acción | Owner sugerido |
|---|---|---:|---:|---:|---|---|---|
| MB-R-001 | Excepciones SDK con PII en Crashlytics | Low | Media | Media | Fixed | Evento tipado, tokens allowlisted, excepción local sin causa; tests canario | Mobile/AppSec |
| MB-R-002 | UID/dispositivo en exports debug y evidencias QA | Low | Media | Media | Mitigated | UUID de archivo, pseudonimización, almacenamiento privado; falta retención | Privacy/QA |
| MB-R-003 | Prompt injection hacia agentes con Bash/escritura | Low/local | Media | Alta | Mitigated | Boundary explícita y retirada de wildcard; reducir tools por rol | AI Security/DevSecOps |
| MB-R-004 | Publicación accidental de iOS/Desktop con actuals de desarrollo | Medium release risk | Baja | Alta | Open gate | Excluir targets o implementar Keychain/Data Protection/timebase/export policy y validar | KMP/Release |
| MB-R-005 | Configuración/ACL/retención de Crashlytics desconocida | Low | Baja | Media | Needs manual review | Canary sintético en proyecto no productivo y revisión IAM/retención/export | CloudSec/Privacy |
| MB-R-006 | Retención indefinida de `validation/` | Low/local | Media | Media | Open | Inventario, TTL, cifrado, cuentas sintéticas y borrado tras QA | QA/Privacy |
| MB-R-007 | Nuevas APIs MindBoost sin authz/rate limit server-side | Medium future | Media | Alta | Preventive | Callable autoritativa, App Check, ownership, límites y tests emulator | Backend/CloudSec |
| MB-R-008 | Dependencia o workflow altera el artefacto release | Medium supply-chain | Baja | Alta | Controlled/open evidence | Pinning, audit, SBOM, firma cerrada y evidencia de release | DevSecOps |
| MB-R-009 | Datos cognitivos enviados a IA futura sin aislamiento | High future | Baja | Alta | Not implemented/gated | DPIA, minimización, tenant isolation, no training, retention y red-team | AI Security/Privacy |

## Riesgo aceptado temporalmente

- El detalle de trials permanece en exports debug porque es necesario para validar precisión temporal; se mantiene solo en builds debug, almacenamiento privado y contenido pseudonimizado. Debe aplicarse TTL y uso exclusivo de cuentas sintéticas.
- Bash permanece disponible en 16 agentes de implementación. La mitigación actual es procesal; reducir tools por rol es backlog obligatorio antes de automatización desatendida.

## Condiciones que elevan severidad

- Evidencia de token, email real, UID estable o dato cognitivo identificable en telemetría remota.
- Distribución de iOS/Desktop como target soportado sin controles equivalentes.
- Agentes ejecutándose de forma autónoma sobre contenido externo con credenciales disponibles.
- API MindBoost remota que confíe en userId, scoring o ownership enviado por cliente.

