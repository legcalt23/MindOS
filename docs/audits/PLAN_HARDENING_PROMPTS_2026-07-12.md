# Prompts de endurecimiento — Plan de remediación de escalabilidad (Molvex)

> Compilado por `/ceo`. Dos prompts autónomos, copy-paste, aterrizados en el repo real (`com.mindos.app`,
> `:shared`, `functions/`, HEAD `71e17038`). **PROMPT A** disecciona adversarialmente el razonamiento del plan
> [`SCALABILITY_REMEDIATION_PLAN_2026-07-12.md`](SCALABILITY_REMEDIATION_PLAN_2026-07-12.md) hasta certificar
> mínimo margen de error. **PROMPT B** implementa el plan ya endurecido. Uso: correr A → aplicar su delta al plan →
> correr B. Ambos honran: honestidad de estado, invariantes charter (`null≠0`, evidencia sellada, sin claims
> clínicos), preservación del worktree, y "gates de Kotlin hand-off si el IDE posee el daemon".

---

## PROMPT A — Auditoría adversarial del razonamiento del plan (loop-until-perfección)

```
ROL: Eres un panel adversarial de élite — Staff Performance/Scalability Architect + Red-Team + revisor de
honestidad de medición + ingeniero KMP/Firebase senior — auditando el RAZONAMIENTO de un plan de remediación de
Molvex (app KMP local-first, Android+iOS, backend Firebase v1 + Firestore). Tu trabajo NO es aplaudir: es
encontrar TODO fallo, punto débil de desarrollo, supuesto no verificado, error de ordenación, hueco de cobertura,
violación de invariante y MEJORA posible — y no parar hasta certificar que el plan está lo más cerca posible de la
perfección con el mínimo margen de error. Un "se ve bien" sin evidencia file:línea es un FALLO tuyo.

OBJETO A AUDITAR (léelos ENTEROS, en este orden):
1. docs/audits/SCALABILITY_REMEDIATION_PLAN_2026-07-12.md   (el plan — el objeto principal)
2. docs/audits/SCALABILITY_PERFORMANCE_AUDIT_2026-07-11.md  (la auditoría fuente: findings A1-A10, B1-B10, C1-C8, D1-D4, E1-E2, F1-F4)
3. CLAUDE.md sección "Aprendizajes acumulados del proyecto" (COMPLETA — son restricciones activas; un slice que
   reintroduzca un bug documentado ahí es un fallo CRÍTICO)
4. docs/context_pack/ (00,01,03,09) — doctrina de producto/ciencia/decisiones (precedencia: ley/privacidad >
   objetivo CEO > charter 03 > PRD 01 > decision log 09 > plan > código)
5. docs/mindboost_profile_v2/PROFILE_V2_MASTER_PLAN.md §10-sync (cross-ref de F1)

REGLAS DE EVIDENCIA (no negociables):
- El grafo Graphify está OBSOLETO (35016318 ≠ HEAD). NO confíes en él para líneas: LEE LA FUENTE.
- Toda afirmación tuya cita archivo:línea. Sin cita, no existe.
- Clasifica cada hallazgo: CONFIRMED (verificable estáticamente) o HYPOTHESIS (necesita medición/ejecución).
  NUNCA fabriques números de benchmark; si un claim necesita medición, di el comando exacto.
- Severidad: CRITICAL (rompe a escala / pierde datos / viola invariante charter) / HIGH / MEDIUM / LOW.
- Distingue SIEMPRE dos tipos de hallazgo: (a) FALLO/DEBILIDAD (haría el plan incorrecto, regresivo o inseguro)
  y (b) MEJORA (lo haría mejor sin ser un fallo). Ambos son obligatorios.

ATACA EL PLAN POR ESTAS SUPERFICIES (todas; añade las que descubras):

A. COBERTURA COMPLETA. Verifica que CADA finding de la auditoría (A1-A10, B1-B10, C1-C8, D1-D4, E1-E2, F1-F4) esté
   mapeado a un slice en §6 del plan O explícitamente diferido con owner. Enumera los NO mapeados. (Pista: revisa
   si C4 —hub Column vs LazyColumn— y todo hallazgo "menor" quedaron fuera de la tabla de trazabilidad.)

B. CORRECTITUD TÉCNICA POR SLICE. Para cada slice S1-S15, verifica el approach contra la fuente real y el stack:
   - S1 (contador shardeado, A1): ¿FieldValue.increment + suma-en-lectura es correcto? ¿La lectura suma shards en
     una query acotada o hace N gets? ¿El backfill lazy tiene carrera (dos increments concurrentes sembrando el
     shard 0 con el valor legacy dos veces)? ¿El dual-read es consistente? ¿N=10 shards aguanta la tasa objetivo?
     ¿Rompe algún consumidor de counters/{uid} (reglas, cliente)? Lee functions/src/socialGraph.ts entero.
   - S2 (borrado paginado + Cloud Tasks, A3): ¿el job es idempotente y reanudable desde checkpoint? ¿el erasure
     sigue fail-closed (no borra Auth sin demostrar residuo cero)? ¿Cloud Tasks encaja con Functions v1? ¿el
     cursor sobrevive a reintentos? Lee functions/src/accountDeletion.ts.
   - S3 (write-ahead de payload, B1/B3/B6/B7): ¿el journal persiste SOLO resúmenes (charter) o se cuela un trial
     crudo? ¿el namespace nuevo entra en MindBoostStorageNamespaces.AccountBound + el eraser de AppGraph.kt (+ su
     test de cobertura) + functions/src/accountDeletion.ts EN EL MISMO CAMBIO? ¿el replay es idempotente (no
     duplica una sesión ya guardada)? ¿el scope sobrevive a la navegación? ¿agrupar B1/B3/B6/B7/F3 en UN slice es
     demasiado grande y debería partirse por holder? Lee MindBoostHubScreen.kt, SessionRecoveryScanner.kt,
     core/session/**.
   - S4 (feed fan-out-on-read, A2): ¿la caché de servidor tiene invalidación correcta? ¿la query cliente lleva
     .limit() espejo de la regla? ¿coste de lectura por feed-load a 1M es aceptable vs la alternativa descartada?
   - S5 (caché SecureKeyValueStore, D1/D3/C8): ¿la invalidación-en-write es completa (put/remove/quarantine/
     clearNamespace)? ¿se limpia en sign-out (subject-scoping)? ¿el caché puede servir un blob que los gates
     invalidarían (viola null≠0)? ¿la SecretKey cacheada se suelta en KeyPermanentlyInvalidated? ¿el decorator va
     en commonMain (testeable en desktopTest) o solo androidMain? Lee SecureKeyValueStore.android.kt + .kt.
   - S6 (retry/backoff, B2): ¿re-lanza CancellationException antes del catch? ¿solo reintenta operaciones
     idempotentes (no un initiatePurchase sin idempotency-key)? Lee core/network/FirestoreDefaults.kt.
   - S7 (freeze animación, C5): YA IMPLEMENTADO — verifica el diff en FocusAtmosphere.kt +
     FocusMeasureRunningContent.kt + FocusSessionScreen.kt: ¿el guard `if (animated) drift else 0f` de verdad
     evita la suscripción del Canvas (no re-dibuja per-frame)? ¿quedó alguna superficie RT-crítica sin congelar
     (grep FocusAtmosphere)? ¿el default `true` preserva las pantallas decorativas?
   - S8/S9 (config callables, A4/A5/A6): ¿minInstances solo en económicas (coste)? ¿maxInstances guard? ¿migración
     v1→v2 asumida sin verificar los imports v1 (learning 2026-06-19)?
   - S10 (outbox offline, B4/B5/B8), S11 (DeviceTier, E1/E2), S12 (TTL/dedup, A7/A8/A9), S13 (arranque, C1/C2/C3/
     C6/C7), S14 (App Check reglas, A10), S15 (D2/D4/B9/B10): verifica cada uno igual de duro.

C. INVARIANTES CHARTER (cualquier violación = CRITICAL). Recorre el plan buscando: ¿algún caché/snapshot/slice
   puede mostrar un valor donde debería ir CALIBRATING/"—" (null≠0)? ¿algún slice actualiza el perfil por una vía
   que no sea evidencia sellada con sus gates? ¿algún slice añade un namespace sin cerrar AccountBound+eraser+
   accountDeletion en el mismo cambio? ¿algún prompt nombra un lexema prohibido (clínico/IQ)?

D. REINTRODUCCIÓN DE BUGS DOCUMENTADOS. Cruza cada slice contra "Aprendizajes acumulados" de CLAUDE.md: 0L
   centinela vs null, confidence⊥sufficiency, defaults que desactivan gates, selección por recencia, booleanos de
   elegibilidad forjables, `List<T> + IntRange`, `Modifier.background(if Color else Brush)`, `kotlin.math` sobre
   Float→Double, `assertTrue` sin overload lazy, `SharedPreferences` shared-file, detekt ratchet (TopLevelProperty
   Naming/MagicNumber/ReturnCount/TooManyFunctions), copy-scanner por source-set, etc. Enumera cualquier riesgo.

E. DAG / ORDENACIÓN / DEPENDENCIAS. ¿Hay un slice que dependa de otro no declarado? ¿La secuencia §16 respeta las
   dependencias (S10→S6, S15.D2→S5, S4→DEC-1, S8/S9→DEC-2)? ¿Algún slice borra la capa de invariantes antes de que
   su reemplazo esté verde? ¿Dos gates Gradle del mismo worktree se solaparían?

F. GATES. Para cada slice, ¿el gate elegido PRUEBA de verdad el cambio? (p.ej. ¿el test de S3 simula muerte de
   proceso real o solo "se llamó al callback"? ¿S1 tiene un test de concurrencia real de 100 follows? ¿S5 tiene un
   spy que confirme que la 2ª lectura NO re-descifra?). Marca los gates insuficientes.

G. HONESTIDAD DE EJECUCIÓN. ¿El plan es honesto sobre el límite de sesión, los gates hand-off, y el worktree sucio
   (177 entradas — riesgo de tocar WIP ajeno)? ¿Alguna afirmación de estado sin respaldo?

H. PROPORCIONALIDAD Y ALCANCE. ¿Algún slice es scope-creep (p.ej. F1/F2 que se declararon fuera)? ¿Alguna segunda
   fuente de verdad de scoring? ¿El plan crea complejidad sin reducir riesgo?

I. MEJORAS. Independiente de los fallos: ¿qué haría el plan MEJOR? (mejor secuenciación para "instant-feel"
   antes; un gate más barato; una simplificación; reusar un patrón existente del repo en vez de construir; un
   test que cierre un hueco; una métrica de éxito medible por slice).

PROTOCOLO DE CONVERGENCIA (esto es lo que pide el founder: "no parar hasta certificar mínimo margen de error"):
- Trabaja en RONDAS. Cada ronda ataca una lente distinta (A→I) y registra hallazgos nuevos.
- NO declares terminado hasta que se cumplan TODAS: (1) cada finding de la auditoría está mapeado o diferido con
  owner; (2) cada slice verificado contra fuente real; (3) cero violación de invariante charter; (4) cero
  reintroducción de bug documentado; (5) DAG/orden sin ciclos ni deps ocultas; (6) cada gate PRUEBA su cambio;
  (7) DOS rondas consecutivas sin ningún hallazgo material nuevo.
- Si tras 2 rondas limpias aún tienes dudas, haz una ronda "completeness critic": ¿qué NO he mirado? (una
  modalidad de fallo, un consumidor no rastreado, un source-set, iOS). Solo entonces certifica.

SALIDA (en este orden):
1. VEREDICTO GLOBAL del plan: APTO / APTO-CON-CAMBIOS / NO-APTO + 2 frases.
2. TABLA de hallazgos: id · tipo(FALLO|MEJORA) · severidad · slice/sección · archivo:línea · CONFIRMED|HYPOTHESIS ·
   descripción · corrección concreta (1-2 líneas). Ordenada por severidad.
3. HUECOS DE COBERTURA: findings de la auditoría no mapeados a ningún slice.
4. DELTA DE PLAN ENDURECIDO: la lista exacta y no-regresiva de cambios a aplicar a
   SCALABILITY_REMEDIATION_PLAN_2026-07-12.md (qué slice se parte, qué gate se refuerza, qué dep se añade, qué
   finding se mapea) — redactada para que otra vuelta la aplique sin ambigüedad.
5. CERTIFICACIÓN: la checklist de convergencia (7 criterios) con [x]/[ ] y evidencia; nº de rondas; y una
   DECLARACIÓN DE RIESGO RESIDUAL honesta (qué queda sin poder verificar estáticamente: iOS, mediciones dinámicas,
   comportamiento a escala real) con su comando de verificación.

RESTRICCIONES: read-only (no edites código ni el plan; produces el DELTA para que otro lo aplique). No fabriques
benchmarks. No propongas reset/clean/force-push. Cero claim clínico/IQ. Si trabajas como multi-agente y el límite
de sesión bloquea subagentes, degrada a rondas solo-inline iteradas — la convergencia es la misma.
```

---

## PROMPT B — Implementación del plan endurecido (orquestador por fases)

```
ROL: Eres el implementador senior de Molvex — KMP (:shared, commonMain, tests en desktopTest) + backend
(functions/ TypeScript, Firebase v1/Firestore) — ejecutando el plan de remediación de escalabilidad YA ENDURECIDO
por la auditoría adversarial (PROMPT A). Implementas con el mínimo margen de error, fase por fase, slice por slice,
en el orden recomendado, sin parar salvo que un gate quede rojo, un cambio Misión-Crítica no tenga su review, o
necesites una decisión del founder.

INPUT AUTORITATIVO:
1. docs/audits/SCALABILITY_REMEDIATION_PLAN_2026-07-12.md (§9 specs de slice, §10 prompts autónomos S1/S3/S5, §16
   secuencia, §13 gates, §15 rollback/stop) — TRAS aplicar el DELTA de PROMPT A.
2. docs/audits/SCALABILITY_PERFORMANCE_AUDIT_2026-07-11.md (anclas file:línea de cada finding).
3. CLAUDE.md "Aprendizajes acumulados" (restricciones activas — respétalas TODAS).

DECISIONES DEL FOUNDER (ratificadas, no re-litigar): DEC-1 feed = fan-out-on-read + caché; DEC-2 = gasto mínimo
(minInstances solo económicas + Cloud Tasks free-tier, CERO Memorystore, contador Firestore shardeado); DEC-3 =
F1/F2 FUERA (tracks propios); DEC-4 = híbrido (subconjunto seguro inline; P0 con review post-reset).

ORDEN DE EJECUCIÓN (respeta el DAG; una fase no empieza sin la anterior en verde/hand-off):
- YA HECHO: S7 (freeze animación C5) — verifica su gate primero; si rojo, arréglalo antes de seguir.
- P1 seguro inline: S5 (caché) → S6 (retry) → S12 (higiene backend). Additivos, sin gasto, sin dinero/GDPR.
- P0 Misión-Crítica (SOLO con review independiente disponible; si subagentes caídos por límite de sesión, ESPERA
  al reset y decláralo): S1 (contador) → S2 (borrado) → S3 (outbox payload) → S4 (feed, tras DEC-1).
- P2: S8/S9 (config, tras DEC-2) → S10 → S11 → S13 → S14 → S15.

BUCLE POR SLICE (idéntico para cada uno):
1. LEE la fuente real de ese slice (archivos citados en el plan/auditoría) ANTES de tocar nada. Confirma que los
   archivos/líneas siguen ahí (worktree con 177 entradas mutando: verifica que el finding es de un archivo REAL,
   no de WIP ajeno untracked que no debes tocar).
2. IMPLEMENTA el diff MÍNIMO correcto que cierra el finding (no refactor especulativo, no abstracción de una sola
   implementación). Honra el approach del plan y las correcciones del DELTA de PROMPT A.
3. INVARIANTES DURAS en cada slice:
   - null≠0: dato insuficiente ⇒ CALIBRATING/"—", nunca 0 fabricado. Un caché/snapshot NUNCA sirve un valor que
     los gates invalidarían.
   - El perfil solo se actualiza vía evidencia sellada con sus gates. No crear segunda fuente de verdad de scoring.
   - Namespace/colección/tabla NUEVA ⇒ MindBoostStorageNamespaces.AccountBound + eraser de AppGraph.kt (+ su test
     de cobertura AccountBoundErasureCoverageTest) + functions/src/accountDeletion.ts EN EL MISMO CAMBIO.
   - Solo resúmenes persistidos, nunca trials crudos. Cifrado @Serializable+SecureKeyValueStore (no Room/SQLDelight).
   - Sin lexema prohibido ni en comentarios/tests dentro del alcance escaneado. Los dirs de features MindBoost
     están ESCANEADOS por el copy-scanner (mindboostCopyFeatureModules): cita las invariantes POR REFERENCIA
     (ForbiddenLexicon/CLAUDE.md), NUNCA copiando a comentarios/tests redacción del plan que nombre lexemas
     prohibidos; corre la variante del scanner del source-set tocado hasta verde (D-18).
   - Detekt es ratchet one-way: NUNCA regeneres el baseline para silenciar; arregla de verdad > constante nombrada
     (SCREAMING_SNAKE) > @Suppress localizado y justificado. `const val` top-level en SCREAMING_SNAKE_CASE.
4. TEST que fija el cambio (donde aplique): el test debe PROBAR el comportamiento, no solo "se llamó" — S1: 100
   follows concurrentes → conteo exacto, 0 ABORTED; S3: matar-antes-de-suspensión no pierde el resumen + replay
   idempotente (round-trip real con store fresco); S5: 2ª lectura no re-descifra (spy) + put/sign-out desalojan.
   Tests de lógica en desktopTest; backend en jest.
5. GATE por superficie, EN SERIE (nunca 2 Gradle en paralelo sobre shared/build):
   - Backend: `npm --prefix functions run typecheck && npm --prefix functions test && npm --prefix functions run scan:secrets` — CÓRRELOS TÚ, reporta salida real.
   - Reglas: el script test:*-rules con su --project (emulador JDK 21).
   - Kotlin: `.\gradlew.bat :shared:desktopTest` · `:shared:detekt` · `:shared:compileAndroidMain`
     (+ `:androidApp:compileDebugKotlin` si toca wiring de app; + copy/terminology scanners si tocas dirs
     escaneados). Si Android Studio POSEE el daemon Gradle del worktree: NO corras Gradle por CLI — verifica
     estáticamente y ENTREGA los comandos exactos al founder, marca el gate `NO EJECUTADO (hand-off)`.
   - iOS: NOT VERIFIED en Windows — decláralo, no lo finjas.
6. REVIEW INDEPENDIENTE para P0 (S1/S2 dinero/GDPR: security-privacy + diff-auditor; S3 pérdida de datos:
   silent-failure-hunter + diff-auditor). Si subagentes NO DISPONIBLE (límite de sesión), NO ejecutes el slice
   P0: déjalo listo y espera al reset; decláralo.
7. CIERRE del slice: reporta con honestidad de estado (EJECUTADO Y VERDE / EJECUTADO Y FALLIDO / VERIFICADO
   ESTÁTICAMENTE / NO EJECUTADO(hand-off) / BLOQUEADO). Actualiza el "Estado resumible" (§18) del plan. Si
   corregiste un error real Y el gate salió VERDE, añade la entrada en "Aprendizajes acumulados" de CLAUDE.md
   (formato exacto: error/causa raíz/solución/regla futura/archivos). Si el gate es hand-off, la entrada queda
   PENDIENTE de verde — no la escribas como verificada.

STOP CONDITIONS (para y reporta, no sigas): gate rojo que no es flake; cambio dinero/GDPR sin su review
disponible; un finding pertenece a WIP ajeno; el límite de sesión se alcanza a mitad de slice (deja el slice en
estado revertible). ROLLBACK: por slice, git revert (nunca reset/clean destructivo). NO ofrezcas commit/PR.

ESTILO: fases pequeñas, diff mínimo, español, sin relleno. Al terminar cada fase, un pase integrador confirma:
gates verdes/hand-off documentados, invariantes intactos, sin colisión con WIP ajeno, entradas CLAUDE.md.
Reporta al founder al final de cada fase (breve) y para en los STOP.
```

---

## Uso

1. **Corre PROMPT A** (idealmente post-reset con panel multi-agente; si no, solo-inline iterado). Produce el
   veredicto + delta de endurecimiento.
2. **Aplica el delta** al plan `SCALABILITY_REMEDIATION_PLAN_2026-07-12.md` (una vuelta corta, revisable).
3. **Corre PROMPT B** sobre el plan endurecido, en el orden recomendado.
