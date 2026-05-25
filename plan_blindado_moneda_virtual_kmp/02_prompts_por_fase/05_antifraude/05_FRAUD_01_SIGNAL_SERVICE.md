# Antifraude 01 — Fraud signals

## Objetivo

Registrar señales antifraude.

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

Implementa FraudSignalService para registrar señales con userId, walletId, type, severity, metadata y source.

## Alcance permitido

Servicio, repo, tests.

## Fuera de alcance

Bloqueos automáticos.

## Requisitos de seguridad

No guardar PII innecesaria. Hash de device/IP si aplica por política.

## Tests mínimos

Tests de inserción y severidad.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
