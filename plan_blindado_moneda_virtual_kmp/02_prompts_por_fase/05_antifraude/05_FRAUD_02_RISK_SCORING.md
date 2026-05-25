# Antifraude 02 — Risk scoring inicial

## Objetivo

Evaluar riesgo antes de operaciones económicas.

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

Implementa RiskScoringService con reglas iniciales: velocity, repeated purchases, attestation missing/fail, refund history stub, IP/device anomalies stub.

## Alcance permitido

Servicio y tests.

## Fuera de alcance

ML o sistemas externos.

## Requisitos de seguridad

Fail safe configurable. No revelar score al cliente.

## Tests mínimos

Tests de score bajo/medio/alto.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
