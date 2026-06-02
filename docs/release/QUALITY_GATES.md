# Quality gates

**Fecha:** 2026-06-02  
**Alcance:** Android release. iOS queda bloqueado hasta reentrada.

## Automatizados en CI

El workflow `.github/workflows/release.yml` bloquea publicacion si falla cualquiera de estos pasos:

- `npm run scan:secrets` en `functions/`.
- `npm audit --omit=dev --audit-level=high` en root y `functions/`.
- `npm run typecheck` y `npm test -- --runInBand` en `functions/`.
- `npm run test:rules` para reglas Firestore/Storage.
- `./gradlew :shared:desktopTest :androidApp:assembleDebug :androidApp:bundleRelease check`.
- `mindos/./gradlew productionReady`.
- Worktree limpio al final del preflight.

## Instrumented / visual

`android-instrumented.yml` ejecuta `:androidApp:pixel6Api34DebugAndroidTest` en Gradle Managed
Device y sube los resultados como artifact. Antes de pasar de beta cerrada a produccion debe estar
verde con flujos criticos:

- arranque y auth;
- navegacion principal;
- DM basico;
- wallet/sandbox;
- borrado de cuenta;
- media privada.

Los tests instrumentados siguen siendo manuales por coste/emulador. Si se convierten en requisito
de PR, mover `workflow_dispatch` a `pull_request` y mantener KVM habilitado.

## Performance

Antes de release publico:

- medir startup cold/warm en dispositivo real o emulador estable;
- revisar jank en Home/feed/DM;
- confirmar que no hay OOM al seleccionar adjuntos grandes;
- registrar umbrales de rollback en `RELEASE_RUNBOOK.md`.

No hay Macrobenchmark/Baseline Profile automatizado todavia. Es un riesgo residual documentado, no
una autorizacion para publicar sin medicion manual.

## Store readiness

Estos gates son externos al repo y deben quedar evidenciados antes de publicar:

- politica de privacidad publicada;
- Play Data Safety rellenado;
- content rating/IARC;
- assets de tienda;
- API keys restringidas segun `docs/security/FIREBASE_API_KEY_RESTRICTIONS.md`;
- keystore real y secrets configurados.
