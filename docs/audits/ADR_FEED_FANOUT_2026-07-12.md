# ADR — Modelo de feed: fan-out-on-read con caché de servidor acotado (S4 · A2)

- **Fecha:** 2026-07-12
- **Estado:** ACEPTADO (implementa DEC-1 ratificada; obligatorio por D-10 antes de tocar código)
- **Slice:** S4 del `SCALABILITY_REMEDIATION_PLAN_2026-07-12.md`
- **Cierra:** A2 (`SCALABILITY_PERFORMANCE_AUDIT_2026-07-11.md`) — el feed materializado `feeds/{viewer}/posts`
  se LEE pero ninguna Cloud Function lo escribe.
- **Base:** HEAD `71e17038`. Anclas re-verificadas contra el árbol actual (worktree sucio).

---

## 1. Contexto verificado (no del plan — del código a fecha de hoy)

- **Read-path cliente:** `GlobalFeedRepositoryProvider.android.kt:57-72` (`loadGlobalFeed`) lee
  `feeds/{currentUid}/posts` ordenado por `createdAt` desc con `.limit(...)`. Es el ÚNICO lector real de
  Firebase; `desktopMain`/`iosMain` usan `InMemoryGlobalFeedRepository` (stub). El doc materializado se
  parsea con `toMaterializedFeedPost` (forma `{ postId, post: {...campos de post...} }`).
- **Writer:** NO EXISTE. `createPost` (`posts.ts:74-96`) solo escribe `posts/{id}` + `user_posts/{uid}/posts/{id}`.
  No hay `onPostCreated`. `onPostDeleted` (`posts.ts:162-179`) solo PURGA `feeds/` (defensa). ⇒ `feeds/` está
  **siempre vacío en producción**: el feed home no muestra nada hoy.
- **Reglas `feeds/`:** `firestore.rules:1702-1708` — `get` owner+`validMaterializedFeedEntry`, `list` owner+limit≤100,
  `create/update/delete: if false` (solo backend). Erasure: `accountDeletion.ts:432` borra `feeds/{uid}/posts`.
- **Grafo social:** `follows/{followerId}_{followeeId}` = `{ followerId, followeeId, createdAt }`
  (`socialGraph.ts:71-85`). `blockUser` (`socialGraph.ts:346-360,383-384`) LEE y BORRA ambos
  `follows/{a}_{b}` y `follows/{b}_{a}` en la tx del bloqueo.
- **Política de privacidad de lectura de posts (reglas, autoridad):** `postPrivacyAllowsRead(author)`
  (`firestore.rules:1272-1278`) = `author` es `Public` **OR** `exists(follows/{viewer}_{author})`;
  y `canReadPostData` (`:1280-1291`) además exige `noBlockBetween(viewer, author)` (`:169-175`).

## 2. Decisión

**Fan-out-on-read servido por una callable `loadHomeFeed`** (Admin SDK, v1), con **caché de servidor por
instancia best-effort** (DEC-2: cero Memorystore, cero colección Firestore nueva). NO se pobla `feeds/` (se
descarta fan-out-on-write). El read-path cliente pasa de leer `feeds/` a invocar la callable (fase siguiente, §7).

### Por qué fan-out-on-read (DEC-1)
- Local-first: el coste de servidor solo aparece al abrir el feed social, y es cacheable.
- Sin write-storm de celebridad: un post de una cuenta con C seguidores NO genera C escrituras (lo que sí haría
  fan-out-on-write en `onPostCreated`). El coste se traslada al lector y se acota + cachea.

## 3. Algoritmo acotado

Entrada: `loadHomeFeed({ limit? })`, `viewer = requireAuth(context)` (+ `requireAppCheck`). Sin `requireEmailVerified`
(leer el feed no lo exige; espeja la postura de lectura de `feeds/`, que solo pedía `isOwner`).

1. **Autores** = followees del viewer + el propio viewer (para incluir posts propios). Query:
   `follows where followerId == viewer limit FOLLOWING_CAP` (**cap de following = 200**). Solo filtro de
   igualdad + limit ⇒ usa el índice de campo automático, **sin índice compuesto**. Deduplicado en un `Set`.
2. **Chunking:** los `authorIds` se parten en grupos de **≤ 30** (límite de Firestore `in`; D-10).
   ⇒ ≤ `ceil(200/30) = 7` grupos.
3. **Posts candidatos:** por grupo, `posts where userId in <grupo> orderBy createdAt desc limit PAGE`
   (PAGE = `limit` saneado a `[1, 100]`, default 50). Requiere **índice compuesto `posts (userId ASC,
   createdAt DESC)`** (nuevo — §6). Las queries de grupo corren en paralelo (`Promise.all`).
4. **Merge (puro):** aplanar candidatos, deduplicar por `postId`, ordenar por `createdAt` desc (desempate
   estable por `postId`), tomar `PAGE`. Se proyectan SOLO los campos que el cliente lee (id, userId,
   authorUsername, authorDisplayName, postType, text, description, textStyle, attachments, `createdAt` como
   epoch-millis) — no se filtra `updatedAt` ni campos internos (JSON-safe + minimiza superficie).
5. **Salida:** `{ posts: FlatPost[] }`.

### Corrección de privacidad SIN reads extra (invariante clave)
Iterar los **followees del viewer** hace que, para cada autor de la lista, `exists(follows/{viewer}_{author})`
sea TRUE por construcción ⇒ `postPrivacyAllowsRead` = TRUE (rama follow-existe) para público Y privado.
`blockUser` borra ambos docs `follows` ⇒ un `follows` vivo garantiza `noBlockBetween` = TRUE. El viewer mismo
pasa por `isOwner`. Por tanto **cada post devuelto satisface exactamente `canReadPostData` de las reglas**, sin
leer `privacy/` ni `blocks/`. (La callable usa Admin SDK y bypassa reglas: esta derivación ES la autorización.)

### Techos deliberados (ponytail — nombrados, no ocultos)
- **`in` = 30 / following cap = 200:** un viewer que sigue a > 200 cuentas ve el feed de un subconjunto de 200
  (orden de `follows` por doc-id, no por recencia — sin `orderBy` para evitar un 2º índice). Se sube el cap si
  hace falta. La inmensa mayoría sigue < 200.
- **Sin paginación (single-page):** el cliente actual (`loadGlobalFeed(limit)`) tampoco pagina (carga top-N).
  Se entrega paridad. Scroll-back (cursor `before`) = follow-up trivial (añadir input `before` + filtro
  `createdAt < before` + `nextCursor` en la respuesta); YAGNI hasta que el cliente lo pida.

## 4. Caché de servidor (DEC-2: sin Memorystore)

**Elección: caché en memoria por-instancia best-effort**, NO `feed_cache/{viewer}` en Firestore.

Racional (por qué NO el doc Firestore):
- Un doc `feed_cache/{viewer}` sería una **colección nueva con datos de OTROS usuarios** (posts de followees)
  ⇒ obligaría a AccountBound + eraser + `accountDeletion.ts` + reglas deny-client + política TTL (hand-off
  gcloud) + escrituras Firestore (coste). Superficie GDPR/seguridad grande para una caché.
- La caché por-instancia (un `Map` a nivel de módulo con TTL) es ** efímera** (evapora en cold start, nunca
  persiste datos cross-user en reposo), **cero coste Firestore**, **cero colección/reglas/erasure nuevos**.
  DEC-1 la permite explícitamente ("caché por instancia best-effort").

Diseño:
- `Map<`viewer:limit`, { expiresAtMs, page }>`, `CACHE_TTL_MS = 45_000`, `CACHE_MAX_ENTRIES = 500`.
- **Read-through:** al entrar, si hay entrada fresca (`expiresAtMs > now`) para `${viewer}:${limit}`, se
  devuelve sin tocar Firestore. Si no, se computa y se escribe la entrada.
- **Cota de memoria:** al superar `CACHE_MAX_ENTRIES`, se desaloja la entrada más antigua (orden de inserción
  del `Map`) antes de insertar. Best-effort, no LRU estricto.

### Política de invalidación
- **Solo por TTL** (best-effort). NO hay invalidación activa por nuevo post/follow/block: es cara y de dudoso
  valor en una caché per-instancia (createPost puede caer en otra instancia). Un post/cambio nuevo aparece en
  ≤ `CACHE_TTL_MS`.
- **Techo de consistencia eventual (nombrado):** durante ≤ TTL (45 s), una entrada cacheada puede seguir
  mostrando el post de un followee que acaba de bloquear al viewer o pasar a privado. Es una regresión ACOTADA
  y no un empeoramiento del estado actual (hoy el feed está vacío; el path sin caché es perfectamente correcto,
  la caché solo difiere ≤45 s la reflexión de un cambio). Mitigación si se quisiera cero staleness: bajar TTL o
  desactivar la caché (la callable sin caché es correcta byte a byte).

## 5. Coste estimado por feed-load a 1M (órdenes de magnitud — SIN benchmarks fabricados)

Supuestos explícitos: 1 lectura de doc = 1 unidad de lectura Firestore; viewer sigue F cuentas (cap 200);
página P (default 50, máx 100). NO se afirma ninguna latencia ni hit-rate medido.

- **Cold load (cache miss):** `min(F,200)` reads (follows) + `ceil(min(F,200)/30)` grupos × P reads (posts).
  Default (F≥200, P=50): ~200 + 7×50 = **~550 reads**. Peor caso (P=100): ~200 + 7×100 = **~900 reads**.
- **Warm load (cache hit, misma instancia, < TTL):** **~0 reads** (memoria).
- **A 1M:** con fracción de hits `h` (best-effort, DEPENDE de instancias y patrón de ráfaga — pull-to-refresh,
  reapertura), reads efectivos/load ≈ `(1−h) × ~550`. `h` **debe medirse** (métrica §8: ratio de hit logueado),
  NO se asume. La caché ataca el patrón de ráfaga (varias aperturas seguidas en la misma instancia caliente).
- **vs fan-out-on-write (descartado):** on-write, un post de una cuenta con C seguidores = C escrituras
  (write-storm de celebridad, ilimitado en C). On-read el coste es por-lector, acotado (~550 reads) y cacheable.
  Para un feed read-heavy con autores virales, on-read evita la amplificación de escritura ilimitada. (DEC-1.)

## 6. Cambios de esquema / índices

- **NUEVO índice compuesto** `firestore.indexes.json`: `posts` → `(userId ASC, createdAt DESC)` (para
  `where userId in [...] orderBy createdAt desc`). Deploy: `firebase deploy --only firestore:indexes`
  (hand-off founder). Hasta que el índice exista, la query devuelve `FAILED_PRECONDITION` — la callable NO se
  invoca en producción hasta desplegar índice + función.
- **SIN cambio de reglas.** La callable usa Admin SDK (bypassa reglas). No se añade colección cliente-visible.
  Las reglas `feeds/` (`:1702-1708`) se conservan (ver §7).
- **SIN cambio de erasure / AccountBound.** La caché es memoria efímera, no dato en reposo ⇒ sin obligación
  GDPR de borrado. `feeds/{uid}/posts` sigue en el erasure (`:432`) como no-op defensivo (colección vacía).

## 7. Plan de transición del read-path cliente

**Decisión sobre `feeds/`:** **sustituir el lector por la callable**; **NO retirar** las reglas/erasure de
`feeds/`. Retirar seguridad/GDPR de una colección probadamente sin writer añade riesgo de review por cero
beneficio funcional; se quedan como defensa muerta documentada (posible cleanup en un track propio).

**Cambio de cliente = FASE SIGUIENTE (declarado, no ejecutado en este slice).** Motivo: (a) parsear la
respuesta de una callable en gitlive-KMP no es trivial (exige DTOs `@Serializable` + `HttpsCallableResult.data
(strategy)`, contrato de serialización que hoy no existe), y (b) el gate Kotlin es hand-off (IDE posee el
daemon) ⇒ no verificable en esta sesión. Un cambio de serialización no-trivial y no-gateado no se envía a
ciegas. **Seguridad de orden de deploy:** desplegar SIEMPRE backend (índice + `loadHomeFeed`) ANTES de cambiar
el cliente; si el cliente se cambiara antes, `firebaseFeedOperation` captura el fallo → `DomainResult.failure`
→ feed vacío (status quo actual), no crash.

**Spec exacta del follow-up (un archivo: `GlobalFeedRepositoryProvider.android.kt`):**
1. Contrato de respuesta de la callable: `{ posts: [ { id, userId, authorUsername, authorDisplayName,
   postType, text, description, textStyle, attachments, createdAt(millis) } ] }` — misma forma plana que
   `toGlobalFeedPost` ya parsea.
2. En `loadGlobalFeed(limit)`: reemplazar la query a `feeds/{uid}/posts` por
   `GitLiveFirebase.functions.httpsCallable("loadHomeFeed").invoke(mapOf("limit" to limit))` dentro de
   `withFunctionsReadTimeout { ... }`, y mapear `result.data(...)` (opción A: DTOs `@Serializable` +
   `kotlinx.serialization`; opción B: si se añade el SDK Android `com.google.firebase.functions` como dep
   explícita —requiere justificar dep—, obtener `result.getData()` como `Map` y reusar `toGlobalFeedPost`).
   Opción A es la preferida (sin dep nueva).
3. Gates hand-off: `:shared:desktopTest` · `:shared:detekt` · `:shared:compileAndroidMain`.

## 8. Verificación / métricas de éxito

- **Backend (ejecutado en este slice):** `npm --prefix functions run typecheck && test && scan:secrets`; tests
  jest de: merge puro (orden/dedupe/cap), chunking, caché (hit/miss/expiración/desalojo), y happy-path de la
  callable (follows→autores→posts→página) con mock mínimo.
- **Índice/prod (hand-off):** desplegar índice + función; confirmar la query no da `FAILED_PRECONDITION`.
- **Métrica de hit-rate (D-17):** loguear hit/miss de la caché per-instancia; el ahorro real de reads DEPENDE
  del hit-rate MEDIDO en staging, no de un supuesto.

## 9. Riesgo residual honesto

- Hit-rate de la caché per-instancia NO verificable estáticamente (depende de nº de instancias / patrón de
  carga) — medir en staging.
- Staleness ≤ TTL en reflejar block/privacy (§4) — acotado, no persiste, no regresa sobre el estado actual
  (feed vacío).
- Cambio de cliente y deploy de índice = hand-off founder (Kotlin gate no corrible aquí; índice fuera de banda).
