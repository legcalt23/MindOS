# Preparación 01 — Auditar repositorio

## Objetivo

Entender el repo antes de tocar código.

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

Inspecciona el repositorio completo y produce un mapa técnico: módulos, backend, KMP, DB, migraciones, auth, testing, CI, configuración y riesgos iniciales. No modifiques código salvo crear `docs/architecture/repo-audit.md`.

## Alcance permitido

Solo lectura del código y creación del documento de auditoría.

## Fuera de alcance

Implementar features, refactors grandes, migraciones o endpoints.

## Requisitos de seguridad

Identifica dónde podrían vivir wallet, ledger, pagos, antifraude, admin y UI Moneda. Detecta secretos accidentalmente versionados.

## Tests mínimos

No aplica tests funcionales. Si existe suite, ejecuta smoke test y documenta resultado.

## Salida requerida

Devuelve:

- Resumen de cambios.
- Archivos modificados.
- Migraciones creadas.
- Tests añadidos.
- Comandos ejecutados.
- Resultado de tests.
- Riesgos pendientes.
