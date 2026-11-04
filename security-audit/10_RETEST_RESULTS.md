# Resultados de revalidacion

## Estado

La mision opera en `audit_and_propose_patches`. No se aplicaron cambios de producto; por tanto ningun P0/P1 se marca corregido y no se afirma que un intento de explotacion haya dejado de funcionar.

| Finding | Fix aplicado | Prueba posterior | Resultado |
|---|---:|---|---|
| SEC-001 | no | source/build wiring | ABIERTO; Firebase provisional sigue alcanzable |
| SEC-002 | no | Functions 894 tests + inspeccion callable/worker | ABIERTO; tests verdes confirman el contrato actual asincrono, no su seguridad semantica |
| SEC-003/004 | no | 4.032 tests desktop + revision de stores | ABIERTO; deuda existente no cerrada |
| SEC-005 | no | censo de rutas y tests de deletion existentes | ABIERTO; no hay canary exhaustivo |
| SEC-006/007/008 | no | inspeccion estatica | ABIERTO; runtime movil no ejecutado |
| SEC-009/010 | no | Functions verify | ABIERTO; no existen los nuevos negativos propuestos |
| SEC-011/012/013/014/016/017 | no | inventario y gates locales | ABIERTO; sin locks/SBOM/attestation/snapshot online; wrapper incluido conserva modo `100644` |
| SEC-015 | no | inspeccion manifest + source | ABIERTO; impacto acotado a P3 |
| SEC-018/019/020 | no | fuente, ledger y artefactos | ABIERTO; rutas latentes/gobernanza no cerradas |

## Controles revalidados sin cambio

- Firestore/Storage Rules: negativos locales pasan, 136/136 + 44 aserciones.
- Backend: typecheck/build y 894 tests pasan.
- KMP: Detekt pasa; XML fresco desktop 4.032/4.032 sin fallos.
- Android: compile debug, lint debug y manifest release pasan.
- Security controls: 18/18; AI pre-gate permanece bloqueado.

Estos resultados reducen falsos positivos y demuestran controles existentes. No neutralizan los hallazgos arquitectonicos abiertos. El primer QA independiente rechazo el borrador y sus observaciones se incorporaron; su recibo se conserva en `INDEPENDENT_QA.md`.
