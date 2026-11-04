# Baseline y entorno

## Snapshot

- Rama: `product/focus2-v5`.
- HEAD: `8a06b9acdfb93c83b9d8d4508c3c10ca8dc0f3e5`.
- Worktree: sucio antes de la auditoria; 65 entradas observadas por el revisor KMP. Se preservaron todas.
- Submodules: no existe `.gitmodules` ni gitlinks; `git submodule status` no fue utilizable por una instalacion Git incompleta en el sandbox.
- Grafo Graphify: obsoleto, construido desde `40708489`, por lo que solo se uso para navegacion, no como evidencia.

## Toolchain observado

| Herramienta | Version/estado |
|---|---|
| PowerShell | 5.1.26100.8875 |
| Git | 2.54.0.windows.1 |
| Node | 24.14.1; Functions declara Node 20 |
| npm | 11.12.1 |
| Java | Temurin 21.0.10 LTS |
| Gradle Wrapper | 9.3.1, SHA-256 fijado en wrapper |
| Kotlin repo | 2.3.10 |
| Compose Multiplatform repo | 1.10.3 |
| Firebase CLI | instalado localmente |
| Semgrep/CodeQL/Gitleaks/Trivy/OSV/Syft/Grype | no disponibles; no instalados |

## Inventario cuantitativo

- 3.092 archivos propios inventariados, excluyendo `build`, `node_modules`, Graphify y auditorias previas.
- 1.005 archivos `shared/src/commonMain`, 315 `commonTest`, 277 `desktopTest`, 36 `iosMain`, 35 `androidMain` y 1 `iosTest`.
- 3 modulos raiz y 24 modulos incluidos en el build compuesto `mindos`.
- 77 archivos de backend bajo `functions/src` y 8 workflows GitHub Actions.

## Gates ejecutados

| Comando | Resultado |
|---|---|
| `node scripts/pii-secret-scan.mjs --selftest` | PASS, 15 reglas |
| `node scripts/validate-audit-ledger.mjs` | PASS inicial, 138 filas, SHA valido |
| `npm run test:release-gates` | PASS, 6/6 |
| `npm run test:rules` | PASS: Storage 44, Firestore y seguridad 136/136 |
| `npm run verify --prefix functions` | PASS: scan, typecheck, 40 suites/894 tests |
| `npm run build --prefix functions` | PASS |
| `.\gradlew.bat :shared:detekt` | PASS |
| `.\gradlew.bat :shared:desktopTest --rerun-tasks` | frontend interrumpido tras completar XML; 570 suites/4.032 tests/0F/0E/0S frescos |
| `.\gradlew.bat :androidApp:compileDebugKotlin` | PASS |
| `.\gradlew.bat :androidApp:lintDebug` | PASS; 2 warnings no-seguridad y 2 hints |
| `.\gradlew.bat :androidApp:processReleaseMainManifest` | PASS |
| `scripts/security/tests/security-controls.tests.ps1` | PASS, 18 controles, sin red |
| `python scripts/security/verify_no_active_ai_pre_gate.py` | PASS; gate sigue pendiente de humano |
| `node scripts/check-release-evidence.mjs --validate` | PASS como `NO_GO`, 5 evidencias pendientes |
| `npm audit --offline --json` raiz/Functions | 0 advisories en cache local; no prueba actualidad |

## Limitaciones del baseline

- No macOS/Xcode/CocoaPods, simulador iOS, dispositivo Android ni Managed Device.
- No AAB/APK release firmado. Se genero e inspecciono solo el manifest fusionado release.
- El scan de historial por patrones localizo unicamente rutas de fixtures/scanner; un barrido exhaustivo por objeto excedio 120 s. No se validaron credenciales.
- `.env` y dos `local.properties` ignorados existen y fueron tratados como rutas sensibles sin lectura.
- La ejecucion local uso Node 24, no el Node 20 declarado por Functions; CI debe seguir siendo la referencia de paridad.
