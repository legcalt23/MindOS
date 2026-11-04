# Parches propuestos, no aplicados

Modo de la mision: `audit_and_propose_patches`. Ningun diff de esta carpeta se aplico ni se compilo. Deben entrar en tareas separadas con owner, tests, migracion y auditor independiente.

| Diff | Finding | Alcance | Nota |
|---|---|---|---|
| `0001-release-auth-fail-closed.diff` | SEC-001 | contencion Gradle | aplicable, pero bloquea release actual; no implementa VPS ni cierra el finding |
| `0002-platform-log-redaction.diff` | SEC-007 | Android/iOS logging | elimina mensaje/cause del sink release |
| `0003-preview-harness-debug-only.diff` | SEC-015 | Android | aplicable como contencion runtime; no cierra el criterio de ausencia binaria |

SEC-002/003/004 no reciben un parche pequeno: comparten una maquina de estados y una autoridad de revocacion. Aplicar solo un booleano o un `try/catch` produciria una falsa correccion. El contrato y sus tests estan definidos en `09_REMEDIATION_PLAN.md` y `tests/REGRESSION_TEST_PLAN.md`.

SEC-016 requiere un cambio de mode Git (`100644 -> 100755`), no un diff textual; debe aplicarse y comprobarse en un runner Linux. Ninguno de estos diffs debe considerarse fix cerrado sin las regresiones funcionales y artifact-level del plan.
