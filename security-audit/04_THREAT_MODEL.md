# Modelo de amenazas

## Activos

Credenciales y tokens; UID e identidad; perfiles y DMs; voz/media; respuestas, reaction time, sesiones, scores y evidencia cognitiva; entitlements/wallet; consentimiento; datos contextuales locales; claves y ciphertext; configuracion remota; costes/cuotas; codigo, CI, firma y artefactos.

## Actores

Usuario legitimo; usuario autenticado malicioso; remoto no autenticado; app local maliciosa; dispositivo perdido/rooteado/jailbroken; MITM; dependencia o proveedor comprometido; colaborador/runner CI comprometido; modelo/contenido externo hostil futuro.

## Limites de confianza

1. UI -> dominio/repositorio KMP.
2. Common -> actual Android/iOS/Desktop.
3. App -> almacenamiento y cache local.
4. Usuario A -> almacenamiento persistente -> usuario B.
5. App -> Firebase/Auth/Rules/Functions.
6. Rules -> Admin SDK, que las omite y debe reautorizar.
7. Functions -> proveedor speech/Secret Manager.
8. DEV/ALPHA -> BETA/PRODUCTION.
9. CI -> repositorios -> artefacto -> store.

## Casos de abuso prioritarios

| Caso | Resultado |
|---|---|
| Sustituir UID/ownership en Firestore | Tests negativos PASS; no bypass confirmado. |
| Activar auth provisional en release | CONFIRMED_SECURITY_DEFECT; sin gate de variante. |
| Invocar harness por Intent | Confirmado, sin acceso a datos reales; P3. |
| Escribir despues del borrado | Confirmado para writers sin autoridad unificada; P0 existente. |
| Borrar A y afectar B | Confirmado por namespaces globales; P1 existente. |
| Obtener confirmacion falsa de borrado | Confirmado cloud->cliente; P1. |
| Reutilizar ciphertext bajo otra clave | Posible con control local privilegiado por ausencia de AAD; P2 existente. |
| Persistir URL capability/media tras logout | Likely/confirmado por cache y lifecycle; P1/P2. |
| Exfiltrar por logs/crash | Remoto sanitizado; logs locales Android/iOS conservan datos; P2. |
| Amplificar coste del feed | Confirmado sin rate-limit explicito; P2. |
| SSRF en broker speech | No confirmado: URL es config operativa, esquema/credenciales/timeout validados. |
| Bypass de reglas Firebase | No confirmado en 136 tests; default deny vigente. |
| Comprometer dependencias/build | Locks/verificacion/atestacion ausentes; P1. |
| Prompt injection/Elite | Runtime LLM externo inactivo; riesgo futuro BLOCKED por gate. |

## STRIDE resumido

- Spoofing: auth de entorno y preview intent; identity derivada backend reduce spoofing social.
- Tampering: storage local privilegiado, ciphertext swap, restore invalido y dependency substitution.
- Repudiation: borrado async sin estado observable; HMAC sin versionado de dominio.
- Information disclosure: logs, caches media, tombstones y telemetria no gobernada.
- Denial of service/coste: home feed sin cuota y Rules con complejidad cercana al limite.
- Elevation of privilege: no bypass confirmado de Rules/callables; Admin SDK sigue siendo frontera critica.

## Supuestos y exclusiones

No se asumio consola Firebase configurada, proveedor seguro o artefacto firmado. Root/jailbreak se usa para modelar confidencialidad e integridad local, no para afirmar proteccion absoluta. iOS permanece inerte mientras su entrypoint fail-closed no sea retirado.
