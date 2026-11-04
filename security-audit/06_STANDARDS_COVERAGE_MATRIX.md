# Matriz de estandares y cobertura

## Versiones oficiales consultadas

| Marco | Version/snapshot aplicado | Fuente oficial |
|---|---|---|
| OWASP MASVS | 2.1.0 | https://github.com/OWASP/owasp-masvs/releases/tag/v2.1.0 |
| OWASP MASTG/MASWE | catalogo web continuo, snapshot 2026-08-04 | https://mas.owasp.org/MASTG/ |
| OWASP ASVS | 5.0.0 | https://github.com/OWASP/ASVS/releases/tag/v5.0.0 |
| OWASP API Security Top 10 | 2023 | https://owasp.org/API-Security/editions/2023/en/0x11-t10/ |
| OWASP SCVS | 1.0 | https://owasp.org/www-project-software-component-verification-standard/ |
| OWASP SAMM | 2.0 model | https://owaspsamm.org/model/ |
| NIST SSDF | SP 800-218 v1.1 | https://csrc.nist.gov/pubs/sp/800/218/final |
| CWE | 4.20, 2026-04-30 | https://cwe.mitre.org/documents/ |
| CVSS | 4.0 | https://www.first.org/cvss/v4.0/specification-document |
| Android | Security best practices, snapshot 2026-08-04 | https://developer.android.com/privacy-and-security/security-tips |
| Apple | Apple Platform Security, snapshot 2026-08-04 | https://support.apple.com/guide/security/welcome/web |
| Firebase | Rules y Emulator Suite, snapshot 2026-08-04 | https://firebase.google.com/docs/rules |
| Kotlin | 2.4.10 actual; repo 2.3.10 | https://kotlinlang.org/docs/releases.html |
| Gradle | 9.3.1, coincide con wrapper | https://docs.gradle.org/9.3.1/release-notes.html |
| Compose Multiplatform | 1.11.x oficial; repo 1.10.3 | https://github.com/JetBrains/compose-multiplatform/releases |

No se declara conformidad global. El mapeo siguiente identifica evidencia parcial y gaps, no certifica cada control de los estandares.

## Cobertura tecnica obligatoria

| Area | Revisada | Automatizada | Manual | Dinamica | Android | iOS | Backend | Resultado | Limitacion |
|---|---:|---:|---:|---:|---:|---:|---:|---|---|
| Arquitectura KMP | SI | parcial | SI | parcial | SI | estatica | N/A | NO_APTO | sin Kotlin/Native runtime |
| Android | SI | SI | SI | parcial | SI | N/A | N/A | NO_APTO | sin dispositivo/AAB firmado |
| iOS | SI | no | SI | no | N/A | NOT_VERIFIED | N/A | NO_APTO | Windows, entrypoint bloqueado |
| Auth | SI | parcial | SI | emulador no Auth | SI | parcial | SI | P0 | consola/proveedor no observados |
| Autorizacion | SI | SI | SI | SI Rules | SI | parcial | SI | PASS parcial | Admin SDK requiere tests propios |
| Sesiones/tokens | SI | parcial | SI | no | SI | parcial | SI | P1 | revocacion/runtime no observados |
| Criptografia | SI | tests parciales | SI | no | SI | estatica | SI | P2 | sin fault injection/KMS |
| Almacenamiento local | SI | SI desktop | SI | no dispositivo | SI | estatica | N/A | P0/P1 | corrupcion/process death pendientes |
| Offline/sync | SI | SI | SI | parcial | SI | estatica | parcial | P0/P1 | features inertes distinguidas |
| Firebase/Firestore | SI | SI | SI | emulador | SI | guardado | SI | PASS parcial | enforcement App Check consola unknown |
| APIs | SI | 894 tests | SI | local unit | SI | parcial | SI | P1/P2 | sin fuzz/carga remota |
| AI Gateway | SI | gate local | SI | no | SI | parcial | SI | BLOCKED/inactivo | sin proveedor activo |
| Entitlements | SI | tests backend | SI | no | SI | parcial | SI | PASS parcial | store/IAP no verificados |
| Privacidad | SI | parcial | SI | no SDK runtime | SI | estatica | SI | P1 | decisiones DPO pendientes |
| Logs | SI | tests remotos | SI | no log capture | SI | estatica | SI | P2 | canarios release pendientes |
| Analytics | SI | no | SI | no | SI | pods | N/A | P1 | consentimiento/manifest runtime |
| Export/delete | SI | tests parciales | SI | no E2E | SI | estatica | SI | P0/P1 | derechos y residuo incompletos |
| Feature flags | SI | parcial | SI | no remote runtime | SI | parcial | SI | PROVISIONAL | consola Remote Config unknown |
| Dependencias | SI | npm offline | SI | no | SI | parcial | SI | P1 | sin advisory snapshot actual |
| Build | SI | Gradle local | SI | parcial | SI | no | SI | P1 | release firmado/iOS ausentes |
| CI/CD | SI | checks estaticos | SI | no runner | SI | config | SI | P1 | no atestacion/provenance |
| Secretos | parcial | selftests y Functions | SI | no validacion externa | parcial | rutas solo | SI | NOT_VERIFIED | 24 fuentes/tests untracked no cubiertos; ignored no leidos; historial acotado |
| Release artifacts | parcial | manifest | SI | no | manifest release | no | N/A | NO_APTO | sin AAB/archive/firma |
| Error handling | SI | SI | SI | parcial | SI | estatica | SI | P2 | Rules expression cap en negativos |
| Resilience/coste | SI | tests backend | SI | no carga | SI | parcial | SI | P2 | feed sin cuota |
| Tests de seguridad | SI | SI | SI | Firebase local | SI | no | SI | PASS parcial | no instrumentados/iOS |

## Mapeo resumido de findings

| Finding | MASVS/MASTG | ASVS/API | CWE | NIST SSDF |
|---|---|---|---|---|
| SEC-001 auth release | MASVS-AUTH, MASVS-RESILIENCE | ASVS V2; API2:2023 | CWE-1188/CWE-16 | PW.4, RV.1 |
| SEC-002/003 borrado | MASVS-PRIVACY, MASVS-STORAGE | ASVS V8; API1/API3 | CWE-459, CWE-862 | PW.4, RV.1 |
| SEC-006 media lifecycle | MASVS-STORAGE, MASVS-PRIVACY | ASVS V8 | CWE-922, CWE-459 | PW.4 |
| SEC-007 logs | MASVS-PRIVACY | ASVS V7/V8 | CWE-532 | PW.7 |
| SEC-008 telemetria | MASVS-PRIVACY | ASVS V8 | CWE-359 | PW.4 |
| SEC-009 coste | MASVS-NETWORK | API4:2023 | CWE-770 | PW.4, RV.1 |
| SEC-010 crypto domain | MASVS-CRYPTO | ASVS V6 | CWE-323 | PW.4 |
| SEC-011/012/013/016/017 supply y CI | MASVS-CODE | ASVS V14; SCVS | CWE-494, CWE-829, CWE-693 | PS.1-3, PW.4 |
| SEC-015 harness | MASVS-RESILIENCE | ASVS V14 | CWE-489 | PW.4 |
| SEC-018/019 integridad local | MASVS-CRYPTO, MASVS-STORAGE | ASVS V6/V8 | CWE-323, CWE-20 | PW.4, RV.1 |
| SEC-020 evidencia contradictoria | MASVS-CODE | ASVS V14; SAMM | CWE-1026 | PO.3, RV.1 |
