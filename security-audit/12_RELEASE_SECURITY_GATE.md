# Release Security Gate

## Veredicto

**NO_APTO**

Fecha: 2026-08-04  
Base: `8a06b9acdfb93c83b9d8d4508c3c10ca8dc0f3e5` + worktree preexistente dirty

## Motivos bloqueantes

1. SEC-001 P0: autenticacion provisional alcanzable desde release y proveedor VPS no implementado/verificado.
2. SEC-002/003 P0: borrado confirmado antes de purga y autoridad local de revocacion incompleta.
3. SEC-004/005/006/008 P1: lifecycle, derechos, medios y telemetria no demuestran borrado/consentimiento completo.
4. SEC-011/012/013 P1: grafos y artefactos de release no reproducibles/atestados; iOS no ejecutado.
5. SEC-019/020 P1: restore Focus2 no valida todo el estado y evidencia anterior de auditoria es contradictoria/no certificable.
6. No se inspeccionaron AAB firmado ni app/archive iOS.

## Controles positivos que no cambian el veredicto

- Reglas Firebase locales y sus negativos pasan.
- Backend, Detekt, Android compile/lint y suite desktop pasan en el alcance descrito.
- Android release manifest desactiva backup/cleartext y no es debuggable/testOnly.
- App Check de callables/Storage y Play Integrity release estan cableados; enforcement Firestore de consola sigue `NOT_VERIFIED`.
- Actions revisadas estan fijadas por SHA y checkout no persiste credenciales.

## Condiciones minimas para reevaluar

- Cero P0/P1 abiertos o mitigacion mecanica que haga las rutas inaccesibles.
- Auth release VPS probada; Firebase sintetico ausente del binario.
- Borrado E2E con estados monotonicamente verificables y cero canarios locales/remotos.
- Consentimiento/telemetria/retencion/export/correccion verificados en ambas plataformas.
- Gradle/CocoaPods locked/verificados, SBOM y atestacion del artefacto.
- AAB firmado y app/archive iOS construidos e inspeccionados sobre el mismo commit.
- Revision independiente del diff final y regresiones negativas verdes.
