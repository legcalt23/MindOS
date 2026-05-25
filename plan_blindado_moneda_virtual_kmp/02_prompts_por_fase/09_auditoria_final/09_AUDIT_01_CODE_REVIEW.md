# Auditoría final 01 — Revisión exhaustiva

## Objetivo

Auditar implementación completa.

## Contexto obligatorio

Estás implementando un sistema de moneda virtual blindado para una app Kotlin Multiplatform con backend autoritativo.

Reglas absolutas:

- El cliente nunca es fuente de verdad.
- No aceptes saldo final desde cliente.
- No implementes pagos reales todavía.
- Usa TPV sandbox/simulado.
- Toda operación económica debe ser idempotente.
- Toda operación económica debe ser auditada.
- Ledger append-only.
- Tests obligatorios.

## Tarea única

Haz auditoría Red Team del código implementado: backend, DB, KMP, pagos sandbox, admin, antifraude, infra y tests. Crea `docs/security/final-audit-report.md` con hallazgos por severidad.

## Alcance permitido

Auditoría/documentación.

## Fuera de alcance

Implementar fixes salvo errores críticos triviales.

## Requisitos de seguridad

Busca bypasses, endpoints peligrosos, falta de locks, falta de idempotencia, secrets, logs sensibles y authz rota.

## Tests mínimos

Ejecuta tests y reporta resultado.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
