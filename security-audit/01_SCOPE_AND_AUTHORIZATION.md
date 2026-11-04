# Alcance y autorizacion

## Propietario y objetivo

- Propietario autorizante: **New Era UNIVERSE**.
- Objetivo defensivo: identificar y reducir riesgos de la aplicacion KMP Molvex, resuelta contra el repositorio MindOS/MindBoost.
- Raiz: `C:\Users\curro\AndroidStudioProjects\MindOS`.
- Fecha: 2026-08-04, zona Europe/Madrid.

## Activos autorizados

Repositorio completo, historial Git local, Android, iOS, KMP/Compose, Gradle, tests, Firebase/Firestore/Storage/Functions, CI/CD, auth de desarrollo, diseños de VPS/AI existentes, dependencias y artefactos locales.

## Limites aplicados

- Sin produccion, datos reales, cuentas de terceros, stores ni servicios externos activos.
- Sin DoS, exfiltracion, persistencia, borrado destructivo, despliegue, commit, push o publicacion.
- Red denegada por defecto. Solo se consultaron paginas oficiales de estandares y se descargo el Gradle Wrapper 9.3.1 fijado por URL y SHA-256 tras aprobacion interactiva.
- No se subio codigo, configuracion, binarios o datos del proyecto a servicios externos.
- Los valores de `.env`, `local.properties`, Google/Firebase plist/json y secretos de CI no fueron leidos ni reproducidos.

## Agentes activados

- Lider: Security Orchestrator / Lead Security Architect.
- Consultores read-only: gobernanza/ledger, KMP-offline, Android, iOS, IAM/Firebase/crypto, privacidad/AI y supply chain.
- Validacion final: auditor independiente de findings/artefactos, en contexto separado.

## Fuentes de verdad

Se leyeron `CLAUDE.md` secciones 1-6, `AGENTS.md`, `docs/AI_WORKFLOW.md`, `docs/VIBE_KANBAN_SETUP.md`, `docs/context_pack/00_CONTEXT_INDEX.md` a `09_DECISION_LOG.md`, documentacion de seguridad seleccionada y `MINDBOOST_AUDIT_LEDGER.md`.

Precedencia aplicada: decisiones `CEO_APPROVED` y contratos vigentes > documentacion historica > claims no verificados. La compilacion o un test verde no se interpretan como conformidad global.
