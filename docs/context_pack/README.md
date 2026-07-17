# MindBoost / Molvex — Context Pack (canon vivo)

**Ruta canónica:** `docs/context_pack/` (underscore) · **packRelease:** 2.0.2 ·
**Reconciliación de canon en git:** 2026-07-17 · **Owner:** New Era UNIVERSE

Este directorio es el **único canon vivo, versionado y trazable en git** de producto, ciencia, gobierno y
decisiones de MindBoost/Molvex. Las herramientas del repo (la skill `/ceo`, los agentes, `scripts/memoryctl.mjs`,
CI) deben enlazar rutas `docs/context_pack/NN` — nunca la variante con guion.

## Linaje

- **Contenido base:** la variante **underscore** preservada (correcciones 2.0.2 ya presentes en `03`, `05` y `09`,
  autoría 2026-07-12), verificada byte-a-byte contra su origen en quarantine. El contenido científico no se altera
  en esta reconciliación.
- **Adjudicación:** ChatGPT (D-H1) declaró `docs/context_pack` el único canon vivo (`canonicalRelease: 2.0.2`).
  Antes de esta tarea, ambas copias (hyphen y underscore) estaban **untracked** (`MB-ENG-0006`).
- **`docs/context-pack` (hyphen):** `SUPERSEDED_SNAPSHOT_DO_NOT_USE`. Permanece **fuera de HEAD**, preservado en
  quarantine como snapshot histórico. Retenía la atribución PMID incorrecta (`35953659` → Passell); **no usar**.

## Manifiesto (lee `00_CONTEXT_INDEX.md` primero)

| # | Documento | Propósito |
|---|---|---|
| 00 | `00_CONTEXT_INDEX.md` | Índice, precedencia, rutas de lectura por tarea, contrato de arranque, `packRelease`. |
| 01 | `01_MINDBOOST_PRODUCT_PRD.md` | Producto, usuarios, experiencia, alcance, negocio, aceptación. |
| 02 | `02_CEO_COPILOT_CHARTER.md` | Auditor ejecutivo post-fase; autoridad, evidencia, severidad, DoD. |
| 03 | `03_SCIENTIFIC_MEASUREMENT_CHARTER.md` | Validez, scoring, calibración, transferencia, claims (atribución PMID corregida). |
| 04 | `04_EXERCISE_QUALITY_STANDARD.md` | Diseño, métricas y gates de ejercicios. |
| 05 | `05_TECHNICAL_CURRENT_STATE.md` | Realidad técnica, límites, arquitectura objetivo, unknowns. |
| 06 | `06_AI_DEVELOPMENT_OPERATING_SYSTEM.md` | `/mb`, 10 agentes base, routing, gobierno de ejecución. |
| 07 | `07_PHASE_REVIEW_PROTOCOL.md` | Auditoría post-fase y generación del prompt de reapertura. |
| 08 | `08_ALPHA_BETA_VALIDATION_PLAN.md` | Alpha/beta, evidencia humana, gates de publicación. |
| 09 | `09_DECISION_LOG.md` | Decisiones `APPROVED`/`SUPERSEDED`/`PROHIBITED` y ADR backlog. |

Precedencia ante conflicto: `00_CONTEXT_INDEX.md §3`. Rutas mínimas de lectura por tipo de tarea: `§4`.

## Hashes canónicos finales (SHA-256, en HEAD)

```text
5c58e6c1374469490210c97ba53dbc9e9d2d2c33380ecab7c88ca294f3328331  00_CONTEXT_INDEX.md
ef135cf8e41249e97592ecdee5e2dd7c98cacdf5c30eb0ecf36cc68420327f46  01_MINDBOOST_PRODUCT_PRD.md
3f4196f476e356879e7243218363b9b2ee0a580c0b1c38d0cf992479e484529a  02_CEO_COPILOT_CHARTER.md
155a71ac9220cd5bb588748fa6b4bc57f9a2458d4c730fc0a592ebf9b6d08588  03_SCIENTIFIC_MEASUREMENT_CHARTER.md
d8d38ffc9e649e97961ff16af1c47a61bfc0f2de9780d47abf5aa53dc692be51  04_EXERCISE_QUALITY_STANDARD.md
b4ab1768cba883322267313186804b8c5e95785782649befec79fe0f7906aa00  05_TECHNICAL_CURRENT_STATE.md
9af6c6554d7c15180fb13af3e7f830960e5a608eaf24335a23c3c694a405832c  06_AI_DEVELOPMENT_OPERATING_SYSTEM.md
186846826ca87af4f7a0d2976c0110d79e9d29cfc0cc865348252eb42e38fb43  07_PHASE_REVIEW_PROTOCOL.md
4c2f9dfbf8cb8a8f4c9488eeb298731bb8d61008a3b1c1622845cd5a256aa3e8  08_ALPHA_BETA_VALIDATION_PLAN.md
6568daf607446740ce2da4dcf7b217a58875571427c81562319272f5609b6814  09_DECISION_LOG.md
```

- **`00` y `09` se editaron mínimamente** en la reconciliación (packRelease + registro de cambios); `01`–`08`
  son byte-idénticos a la variante underscore base. No se hizo sustitución masiva de versiones: `packRelease`
  (versión del paquete) ≠ `documentVersion` (versión propia de cada documento).
- **Validación:** `sha256sum docs/context_pack/*.md` debe reproducir esta tabla; o `git show HEAD:docs/context_pack/<f>`.

## Canon vivo · mirror externo · snapshot congelado

- **Canon vivo (autoridad):** `docs/context_pack/` en HEAD (esta copia). Es lo que enlazan las herramientas.
- **Mirror externo vivo:** `DOCS MB/00..09` (fuera del repo) se **sincroniza** con este canon tras backup
  recuperable. No es autoridad; es una copia de conveniencia.
- **Snapshot congelado Fase 0:** `DOCS MB/PHASE0_FOCUS_FINAL/00..09` es **INMUTABLE** (evidencia histórica). Puede
  **divergir** del canon vivo por diseño; no se sincroniza.

## Sincronización (no diverjas en silencio — `00 §8`)

- Al cambiar el pack: eleva `packRelease` o la `documentVersion` afectada, registra fecha/razón, actualiza el
  Decision Log (`09`) y el `Registro de cambios`, y aplica el cambio **en el canon vivo y en el mirror externo** en
  el mismo paso. No modifiques en silencio un umbral científico ni una promesa al usuario.

## Naming

El pack usa "MindBoost" como marca; la marca externa vigente es **Molvex** (código interno sigue siendo
MindBoost / `com.mindos.app`). `CONFLICTO` menor pendiente de ratificación; no renombrar código.
