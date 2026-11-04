# Evidencia de validacion

## Identidad de ejecucion

- Raiz: `C:\Users\curro\AndroidStudioProjects\MindOS`
- Rama: `product/focus2-v5`
- Commit base: `8a06b9acdfb93c83b9d8d4508c3c10ca8dc0f3e5`
- Worktree: dirty antes de la auditoria; se preservaron todos los cambios ajenos
- Host: Windows/PowerShell 5.1, zona Europe/Madrid
- Red: deny-by-default; solo se consultaron fuentes publicas oficiales para versionar estandares. No se subio codigo.

## Gates ejecutados

| Comando | Resultado | Evidencia/limitacion |
|---|---|---|
| `node scripts/pii-secret-scan.mjs --selftest` | PASS, exit 0 | 15 reglas; valida el scanner, no sustituye un barrido online |
| `npm run test:storage-rules:static` | PASS, exit 0 | 10 checks estaticos |
| `npm run test:rules` | PASS, exit 0 | Storage 44 aserciones; Firestore 136/136; emuladores locales solamente |
| `npm run verify --prefix functions` | PASS, exit 0 | 40 suites, 894 tests, typecheck y secret scan |
| `npm run build --prefix functions` | PASS, exit 0 | TypeScript local |
| `.\gradlew.bat :shared:detekt` | PASS, exit 0 | Ejecucion serial |
| `.\gradlew.bat :shared:desktopTest` | PASS up-to-date, exit 0 | Se forzo despues con `--rerun-tasks` |
| `.\gradlew.bat :shared:desktopTest --rerun-tasks` | XML completo: 4.032/4.032, 0F/0E/0S | 570 suites frescas; la sesion unificada fue interrumpida por frontend despues de escribir XML, por lo que no se afirma exit 0 del re-run |
| `.\gradlew.bat :androidApp:compileDebugKotlin` | PASS, exit 0 | Compila Android/shared; no es build release firmado |
| `.\gradlew.bat :androidApp:lintDebug` | PASS, exit 0 | 2 warnings de autoboxing y 2 hints; sin issue de seguridad reportado |
| `.\gradlew.bat :androidApp:processReleaseMainManifest` | PASS, exit 0 | Manifest fusionado inspeccionado: `allowBackup=false`, cleartext false, release no debuggable/testOnly |
| `powershell -File scripts/security/tests/security-controls.tests.ps1` | PASS, exit 0 | 18 controles, network 0 |
| `python scripts/security/verify_no_active_ai_pre_gate.py` | PASS, exit 0 | IA sigue bloqueada/inactiva; no valida un proveedor |
| `node scripts/check-release-evidence.mjs --validate` | Valid, `NO_GO` | pendientes IAP, Data Safety, privacy URL, listing/IARC y UGC report/appeal |
| `npm run test:release-gates` | PASS, exit 0 | 6/6 gates del harness |
| `npm audit --offline --json` raiz/functions | 0 advisories en cache | No demuestra ausencia actual de CVE; resultado informativo |
| `node scripts/validate-audit-ledger.mjs` | PASS inicial | 138 filas; ledger ya estaba dirty/unanchored |

Los stdout/stderr completos y timestamps por comando no se archivaron durante la ejecucion inicial. La tabla es evidencia de sesion, no un recibo firmado. Para desktop tests si se conserva evidencia primaria local: 570 XML, 4.032 tests y digest determinista en `sanitized-tool-outputs/desktop-junit-summary.json`; el exit code del re-run forzado sigue honestamente `NOT_RECORDED`.

## Pruebas negativas Firebase

El harness local intento lecturas/escrituras cruzadas, ownership y shapes invalidos. Los `PERMISSION_DENIED` observados correspondieron a negativos esperados. Algunos negativos complejos de `profile_visuals` alcanzaron el limite de 1.000 expresiones de Rules: permanecen denegados, pero se registra fragilidad y necesidad de simplificacion antes de ampliar el schema.

## Secretos e historial

- `--selftest` valida el scanner, no el worktree. `functions verify` ejecuto su gate sobre el alcance rastreado de Functions.
- El snapshot contenia 24 archivos de fuente/test de producto no rastreados; los scanners basados en `git ls-files` no los cubren. Estado: `NOT_SCANNED`, sin afirmar secreto presente.
- No se imprimieron valores de `.env` o `local.properties` ignorados.
- Busqueda historica dirigida: solo fixtures/scanners conocidos.
- Barrido exhaustivo por cada commit: `NOT_VERIFIED`; el intento local excedio 120 s.
- Gitleaks/TruffleHog/Semgrep/CodeQL/OSV/Trivy/Syft/Grype no estaban instalados y no se instalaron. La cobertura repo-wide queda `NOT_VERIFIED`.

## Plataformas no ejecutadas

- iOS/Xcode/Kotlin Native app, simulador y dispositivo: `NOT_VERIFIED` por host Windows.
- Android instrumentado, root, proxy y fault injection: `NOT_VERIFIED`.
- AAB/APK release firmado, certificado y firma final: `NOT_VERIFIED`.
- Firebase Console: IAM, App Check enforcement para Firestore, Secret Manager/KMS, backups, quotas y proyectos reales: `NOT_VERIFIED`.
- App Store/Google Play, proveedores IA/IAP/telemetria: fuera de pruebas activas.
