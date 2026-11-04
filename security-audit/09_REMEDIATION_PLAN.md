# Plan de remediacion

## Oleada 0 - contencion inmediata

| Orden | Finding | Accion | Owner sugerido | Criterio de salida |
|---:|---|---|---|---|
| 1 | SEC-001 | Bloquear mecanicamente todo artefacto release que incluya auth provisional; mantener distribucion cerrada | Identity + Release | release falla cerrado y test prueba ausencia de Firebase auth provisional |
| 2 | SEC-002/003/004 | Congelar claims de borrado completo y activacion de nuevos writers/sync | Privacy + KMP | ningun flujo declara `deleted` antes de verificacion; feature gates default-off |
| 3 | SEC-012 | Mantener iOS no publicable | iOS + Release | app/archive real verificado en macOS sobre el mismo SHA |

## Oleada 1 - autoridad de identidad y lifecycle

1. Implementar proveedor VPS tipado, revocacion/refresh/logout/recuperacion y migracion; eliminar Firebase sintetico de release.
2. Modelar borrado remoto `ACCEPTED -> ERASING -> VERIFYING -> ERASED/RETRY_REQUIRED` con estado consultable e idempotente.
3. Crear `SubjectRevocationAuthority` unica para todos los reads, writes, journals, caches y cuarentenas; `UNKNOWN` falla cerrado.
4. Sustituir clears globales por borrado fisico por sujeto, con verificacion A/B y durabilidad de plataforma.
5. Introducir catalogo ejecutable de datos, retencion, export, correccion y borrado; todo store nuevo debe registrarse.

## Oleada 2 - privacidad de plataforma

1. Vincular medios/temporales/caches a owner y lifecycle; limpiar success/cancel/error/logout/delete.
2. Redactar logs antes de construir mensajes; prohibir `Throwable.message`, UID y path en release.
3. Separar privacidad visual de consentimiento; autoridad por finalidad para Analytics, Performance y Crashlytics.
4. Completar Data Safety y Privacy Manifest desde el mismo inventario verificable.

## Oleada 3 - backend y criptografia

1. Rate limit fail-closed para `loadHomeFeed`, presupuesto de queries y observabilidad de coste sin UID raw.
2. Clave HMAC dedicada, dominio/version y rotacion; verificar IAM/Secret Manager en cloud.
3. Reducir complejidad de Rules que roza el limite de expresiones y conservar negativos de ownership.

## Oleada 4 - supply chain y release

1. Gradle locks + dependency verification estricta; CocoaPods lock y fuente unica de versiones.
2. Scanner de advisories con estado `UNKNOWN` bloqueante y snapshot actual aprobado.
3. SBOM por artefacto, digest, firma y atestacion enlazados al SHA y evidencia.
4. Construir Android release firmado e iOS archive; inspeccionar manifest/plist/entitlements/privacy manifest y binarios.

## Dependencias de ejecucion

- SEC-001 precede cualquier beta publica.
- SEC-002/003/004/005 deben cerrarse sobre un mismo contrato; parches aislados pueden crear falsos terminales.
- SEC-011/012 preceden SEC-013, porque no debe atestarse un grafo no reproducible.
- Las decisiones DPO/CEO de retencion, telemetria y proveedor son entradas humanas, no controles sustituibles por codigo.

