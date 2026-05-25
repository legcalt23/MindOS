# Preparación 02 — Decisiones de arquitectura

## Objetivo

Fijar decisiones técnicas adaptadas al stack real.

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

Crea `docs/architecture/virtual-currency-adr.md` con decisiones: backend framework, migraciones, repositorios, transacciones, idempotencia, ledger, pagos sandbox, KMP UI, testing e infraestructura.

## Alcance permitido

Documentación ADR.

## Fuera de alcance

Implementar código.

## Requisitos de seguridad

Incluye trade-offs y justificación de seguridad para cada decisión.

## Tests mínimos

No aplica.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
