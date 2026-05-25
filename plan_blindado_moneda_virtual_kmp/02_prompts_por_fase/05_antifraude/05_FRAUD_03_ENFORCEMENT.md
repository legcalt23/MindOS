# Antifraude 03 — Enforcement

## Objetivo

Aplicar decisiones de riesgo.

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

Integra antifraude en spend, sandbox purchase verify y rewards si existen: allow, throttle, review, freeze.

## Alcance permitido

Integración servicios, tests.

## Fuera de alcance

UI antifraude avanzada.

## Requisitos de seguridad

Auditar bloqueos y freezes. No bloquear por una señal débil aislada salvo regla crítica.

## Tests mínimos

Tests de operación bloqueada y auditada.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
