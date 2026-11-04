# Auditoria integral de ciberseguridad de Molvex / MindOS

Fecha de corte: 2026-08-04  
Modo: `audit_and_propose_patches`  
Veredicto: **NO_APTO**

## Resumen ejecutivo

El repositorio real auditado es MindOS/MindBoost; la etiqueta "Molvex" se conserva como nombre de la mision. La revision cubrio KMP, Android, iOS por inspeccion estatica, Firebase/Functions, persistencia offline, privacidad, supply chain y CI. Firestore/Storage Rules, backend, Detekt, Android Lint, compilacion Android y 4.032 tests desktop quedaron verdes en el alcance ejecutado.

El resultado no permite release. Los 20 hallazgos consolidados incluyen la build Android de release instanciando Firebase Auth sin un `DEV_AUTH_ONLY` mecanico, pese a que la autoridad de producto exige VPS para lanzamiento. El borrado cloud responde como completado cuando solo ha encolado el trabajo, y el cliente elimina su marcador recuperable. En local siguen abiertas las autoridades incompletas de revocacion, cuarentena y borrado por sujeto ya registradas en el ledger. iOS no se construye como app en CI y conserva defectos latentes de media, logging y privacidad. Gradle y CocoaPods carecen de cierre reproducible mediante locks/verificacion.

## Riesgo dominante

| Prioridad | Situacion |
|---|---|
| P0 | Auth provisional alcanzable en release; resurreccion/residuo post-borrado y cuarentena ya abiertos en ledger. |
| P1 | Semantica falsa de borrado cloud, borrado cruzado local, derechos/inventario incompletos, telemetria sin gobierno por finalidad, iOS y supply chain no verificables para release. |
| P2 | Amplificacion de lecturas, logging local de identificadores, binding criptografico insuficiente, tooling supply chain y caches sensibles. |
| P3 | Harness de verificacion visual compilado en `main`. |

## Alcance de evidencia

- `TEST_VERIFIED`: reglas Firebase locales, backend, Detekt, Android Lint, compilacion Android, manifest fusionado release, tests desktop y controles de seguridad agentic.
- `REPO_VERIFIED`: flujos source-to-sink y configuraciones citadas en `07_FINDINGS.md`.
- `NOT_VERIFIED`: app iOS/Xcode, dispositivos, instrumentacion Android, AAB firmado, Firebase Console/IAM/App Check enforcement, KMS/Secret Manager, proveedores externos, CVE online y secret scan repo-wide del snapshot dirty.
- `BLOCKED`: `APTO` y `APTO_CON_LIMITACIONES` mientras exista cualquier P0/P1 abierto o una plataforma release sin inspeccion.

## Accion recomendada

1. Contener release con un gate fail-closed de auth y mantener iOS bloqueado.
2. Redisenar el borrado como job observable e idempotente, con revocacion tombstone-first y borrado por sujeto.
3. Cerrar telemetria/retencion/derechos por finalidad y eliminar logs/caches sensibles.
4. Sellar Gradle/CocoaPods, producir SBOM y atestacion del artefacto.
5. Revalidar Android release firmado, iOS app/archive y escenarios A/B, process death y corrupcion.
