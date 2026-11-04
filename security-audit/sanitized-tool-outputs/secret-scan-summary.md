# Secret scanning - salida saneada

- Scanner propio self-test: PASS, 15 reglas.
- Functions rastreado: gate local ejecutado dentro de `npm run verify --prefix functions`.
- Snapshot dirty: 24 archivos de fuente/test de producto no rastreados quedaron `NOT_SCANNED` por scanners basados en indice Git. No se afirma que contengan secretos.
- Archivos ignorados sensibles detectados por nombre (`.env`, `local.properties`): valores no leidos ni reproducidos.
- Historial dirigido: solo fixtures/scanners conocidos.
- Historial exhaustivo y repo-wide dirty: `NOT_VERIFIED` por timeout/cobertura.
- No se validaron credenciales contra servicios externos.
