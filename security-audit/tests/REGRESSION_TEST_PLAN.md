# Plan de pruebas negativas y de regresion

| ID | Test discriminante | Plataforma/gate | Aceptacion |
|---|---|---|---|
| SEC-001 | release source/dependency graph no contiene Firebase auth provisional; sin VPS config falla build | Gradle/Android | fail-closed antes de artefacto |
| SEC-002 | callable devuelve `ACCEPTED`; worker fallido deja `RETRY_REQUIRED`; solo verificacion total produce `ERASED` | Functions | estados monotonicamente correctos |
| SEC-003 | writers/readers/journals niegan `REVOKED` y `STATUS_UNAVAILABLE`; cuarentena se borra por sujeto | common/desktop + platform | cero lectura/resurreccion |
| SEC-004 | A/B conservation, fail-one-continue-all, process death, full storage, indice Keychain corrupto | Android/iOS | cero residuo y A intacto |
| SEC-005 | canary generado por catalogo en cada coleccion/subcoleccion/prefijo/export | Emulator Suite | cero residuo y export completo |
| SEC-006 | success/cancel/error/logout/delete limpia medios; cache key no contiene URL/token | instrumentados | cero canarios del sujeto |
| SEC-007 | Throwable con UID/path/message canario no aparece en logs release | common + Android/iOS | allowlist solamente |
| SEC-008 | fresh install/deny/revoke no genera trafico de finalidad opcional | Android/iOS proxy local | default y revocacion efectivos |
| SEC-009 | segunda rafaga moderada se limita y queries por respuesta no superan presupuesto | Functions unit/emulator | rechazo tipado, sin carga |
| SEC-010 | produccion exige clave dedicada; domains/versions no correlacionan | Functions unit | fallback prohibido |
| SEC-011 | mutation de lock/checksum y resolucion limpia reproducible | Gradle CI | sustitucion falla |
| SEC-012 | cambio `iosApp/**` dispara build app/archive sobre el mismo SHA y lock | macOS CI | app real y pods fijados |
| SEC-013 | digest/SBOM/attestation distintos al artefacto son rechazados | release gate | identidad completa |
| SEC-014 | JSON vacio/truncado/error registry bloquean | CI unit | unknown = fail |
| SEC-015 | release no contiene extra/harness | Android artifact | ausencia binaria |
| SEC-016 | wrapper incluido conserva modo `100755` y corre `productionReady` en Ubuntu | CI | gate ejecutado, no solo declarado |
| SEC-017 | tooling aislado usa version/binario/checksum y Node compatibles | CI bootstrap | sin descarga no atestada |
| SEC-018 | swap ciphertext entre sujetos/keys/scopes/sesiones falla cerrado | common + Android/iOS | AAD e identidad validada |
| SEC-019 | snapshots negativos/duplicados/unknown/overflow no alcanzan scorer | common property/fuzz | restore fail-closed |
| SEC-020 | paquete de auditoria exige SHA, resultados, manifest y clase de review | governance gate | claims incompletos rechazados |

Reglas: datos sinteticos, ningun servicio real, Gradle serial por worktree y evidencia XML/exit code archivada.
