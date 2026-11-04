# Riesgo residual y evidencia faltante

## Riesgo aceptable solo fuera de release

- El harness preview (SEC-015) tiene impacto actual bajo, pero debe salir de `main` antes de release.
- La separacion HMAC (SEC-010) es hardening mientras no exista secreto expuesto, pero debe cerrarse antes de retener pseudonimos reales.

## Riesgo no aceptable para release

- Proveedor de identidad provisional alcanzable y VPS no validado.
- Borrado remoto con confirmacion anticipada y authority local incompleta.
- Derechos, retencion, medios y telemetria sin evidencia E2E.
- iOS sin app/archive verificado.
- Grafos Gradle/CocoaPods no cerrados y artefactos sin provenance.
- Focus2 restore/binding local siguen bloqueando activacion y el paquete `audit-output/` anterior no debe usarse como certificacion.

## `NOT_VERIFIED`

- Compilacion, runtime, Keychain, Data Protection, ATS, entitlements, Privacy Manifest y firma iOS.
- APK/AAB release firmado, instalacion, process death, full-storage, backup/restore y root/jailbreak.
- Revocacion real de tokens, App Check enforcement Firestore, IAM, KMS/Secret Manager, backups y cuotas cloud.
- Trafico real de Analytics/Performance/Crashlytics y declaraciones de stores.
- CVE/KEV/EPSS actuales de dependencias; los audits fueron offline.
- Historia Git exhaustiva y artefactos historicos de release.
- Proveedores IA, pagos, webhooks externos y datos reales, deliberadamente fuera de prueba activa.

## Supuestos

- Las rutas inertes permanecen fail-closed y no se activaran sin gate.
- No se distribuye ningun artefacto del worktree auditado.
- Los secretos ignorados no se consideran verificados ni comprometidos; no se leyeron sus valores. Los 24 fuentes/tests untracked se clasifican `NOT_SCANNED`.
- La evidencia local corresponde al SHA base mas cambios preexistentes dirty; no constituye una atestacion reproducible.
