# Infra 02 — Compose local seguro

## Objetivo

Levantar entorno local reproducible.

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

Crea/ajusta docker-compose para backend, PostgreSQL, Redis si aplica, migraciones y variables de entorno de desarrollo no secretas.

## Alcance permitido

Compose, env.example, docs.

## Fuera de alcance

Producción cloud real.

## Requisitos de seguridad

Usar `.env.example`; no commitear `.env` real.

## Tests mínimos

Smoke test local documentado.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
