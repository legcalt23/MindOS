# REDTEAM_TEST_MATRIX_V5 — Matriz red-team → test → gate → evidencia

```
DOCUMENT_STATUS = DRAFT · Producida por test-engineer (orden 3, agent_outputs/06) y completada por la sesión
/mb: los 11 escenarios que llegaron [SIN ORÁCULO] reciben aquí su oráculo concreto (marcados ★), y el
escenario ausente (switch-access, #34) queda cubierto. Gates SOLO del vocabulario GATE_CATALOG_V5.
Regla: "añadir tests" no es una celda válida — cada fila tiene input y oráculo ejecutables.
```

Formato: **#. Escenario — Prevención · Detección · Degradación · Recuperación · TEST(input→oráculo) ·
Failure behavior · Artefacto/slice · Gate**

1. **Cero datos** — MinSampleGate/nMin · nEffective en ScoreResult · sin contribución · N/A · sesión con 0
   trials → `ScoreResult=null`/`Missingness(INSUFFICIENT_N)`, nunca 0.0 · ausencia tipada · scorer S3/S4 · T-ADV
2. **Un dato (n=1)** — nMin>1 · nEffective<nMin · estado `INSUFFICIENT_DATA` ("—") · esperar · 1 trial →
   estado UX INSUFFICIENT_DATA, no número · ídem · S1/S3 · T-S1+T-ADV
3. **Muchos datos (grinding)** — day-cap+span-floor (EvidenceQuantityMaturity reutilizado) · días distintos vs
   bruto · madurez estancada · N/A · 500 trials en 1 día → la madurez NO sube por volumen intradía · grinding
   no compra madurez · evidence S1/S8 · T-S1
4. **Todo correcto (ceiling)** — staircase · accuracy≈1.0 sostenida · declara ceiling, no inventa distancia ·
   sube dificultad la siguiente · acc=1.0 estable → A4: HONEST sigue ALTO/confident (sin penalizar "sin
   variación") · reportar ceiling · S3/S8 · T-ADV
5. **Todo incorrecto (floor)** — staircase · accuracy≈azar · `null+reason` (A6) · baja dificultad · acc=azar en
   flanker → A6: jamás "0 perfecto" · nunca 0-con-semántica-de-capacidad · S3 · T-ADV
6. **Masher** — ventana + antigaming · oráculo A1 · INVALIDATED si RT uniforme+acc=azar · N/A · MASHER
   (1 tecla, RT~250 constante, acc~0.50, 0 MISS) vs HONEST (acc~0.92) → A1: score(H)>score(M) [v1 FALLA hoy:
   44<70 — documenta el hide de S0] · NO-GO de exposición · S3/S4 · T-ADV
7. **Waiter** — distribución de latencia esperada · RT sistemático al límite · patrón marcado/excluido · N/A ·
   WAITER → A3: score(W)<score(H) · sin premio por paciencia · S3 · T-ADV
8. **Omisiones** — marcador explícito de no-respuesta · conteo por condición · omisión = evento propio · N/A ·
   30% omisiones en E01 → hits/misses/FA/CR separados; omisión ≠ miss ≠ FA · nunca colapsada a "incorrecto" ·
   scorer E01 S4 · T-S4
9. **Anticipaciones** — filtro RT<banda fisiológica ANTES de tendencia central · conteo sobre RT CRUDOS
   (learning 2026-06-04: extremos jamás sobre lista filtrada por MAD) · excluidas del RT central, CONTADAS ·
   N/A · RT=80ms repetido → anticipación en rawRts, no en cleanRts; el conteo aparece en el resultado · nunca
   perder el conteo al limpiar · métricas S3 · T-ADV
10. **Timeout** — ventana + reloj monotónico · `TrialResolved(OMISSION)` · como #8 · N/A · 0 respuesta en
    choice → OMISSION, no "incorrecto" indistinguible · timeout ≠ error de discriminación · TrialClassifier
    S3 · T-S3
11. ★ **Cherry-picking** — no se fuerza terminar (UX) · **gate de tasa de finalización por ventana**: sesiones
    iniciadas vs completadas · evidencia de la ventana marcada `SELECTIVE_COMPLETION` si abandono>umbral
    (DESIGNED_NOT_RUN, revisión con datos) y las incompletas EXCLUIDAS explícitamente (nunca promediadas) ·
    N/A · **TEST: perfil sintético que abandona 8/10 sesiones con peor primer bloque → el agregado de la
    ventana queda marcado y las completadas no elevan la evidencia; perfil honesto con 10/10 completadas →
    sin marca (mitad discriminante)** · sin gate, el score visible se infla en silencio · T-ADV S3 · T-ADV
12. **Side bias** — cuotas balanceadas en generador · entropía de respuesta por lado (detector D6
    core/antigaming — WIRING A S3 OBLIGATORIO, no opcional: learning 2026-07-04 "detector sin call-site
    protege cero") · marcado/INVALIDATED · N/A · 100% LEFT → score(SIDE_BIAS)<score(HONEST) + D6 dispara;
    honesto balanceado NO dispara · sin wiring no hay detección · S3 · T-ADV
13. ★ **Periodicidad/metrónomo** — ISI/foreperiodo aleatorizados · **autocorrelación de inter-response-
    intervals: pico normalizado > umbral con ISI aleatorio ⇒ RHYTHMIC** (patrón RhythmTappingDetector ya
    existente en core/antigaming — reutilizar, no reinventar) · sesión marcada, excluida · N/A · **TEST:
    adversario que responde cada 500ms exactos ignorando ISI → RHYTHMIC dispara; honesto con RT correlado al
    ISI aleatorio → no dispara** · E01 explotable sin esto · T-ADV(B) S4 · T-S4+T-ADV
14. **Aprendizaje de ítems** — formas paralelas + bancos train/measure separados + sondas retenidas diseñadas
    ANTES de entrenar · desempeño forma entrenada vs held-out · claims retenidos hasta V4 · N/A · 50
    exposiciones a una forma, luego held-out → gate "requiere V4 PASSED" antes de claim de generalización;
    protocolo de exposición registrado en el ledger de exposición del banco · cero claim sin V4 · S3/S7/S8/S9 ·
    requiere V4 PASSED
15. **Cambio de dificultad mid-bloque** — dificultad sellada por bloque de medición + registro por-trial del
    parámetro resuelto · presentada vs nominal · trial fuera de spec excluido de evidencia (práctica conservada)
    · N/A · staircase muta un bloque de medición → golden "misma seed ⇒ plan byte-igual" FALLA si el bloque no
    está sellado · comparabilidad rota ⇒ exclusión · motor S3 · T-S3
16. ★ **Fórmula nueva con salto/rank flip** — dual/shadow obligatorio (TEC-011) · comparación shadow-vs-current
    · serie histórica preservada (RETIRED_VERSION) · rollback=flag · **TEST: cohorte sintética de ≥20 perfiles
    con desempeño conocido; scoring old y new en paralelo → oráculo: Kendall-τ(ranking_old, ranking_new) <
    umbral acordado en el ADR ⇒ la promoción de la fórmula queda BLOQUEADA con revisión explícita; y para cada
    perfil, |Δscore| > banda declarada ⇒ flag de salto** (umbral/banda = parte de ADR-FOCUS-SCORING,
    DESIGNED_NOT_RUN) · un flip silencioso reordena usuarios sin justificación · S9 · requiere V3 PASSED + ADR
17. **Cambio de dispositivo** — `ComparabilityNotEstablished` default (fail-closed por AUSENCIA del input) ·
    input de comparabilidad presente antes de promediar · `NON_COMPARABLE`, no promedia · N/A · sesión en
    dispositivo B sin bridge ADR-SCI-008 → excluida del promedio + estado NON_COMPARABLE · promediar
    cross-device sin bridge = violación máxima del charter · S1/S5 · T-S1
18. ★ **Cambio de modalidad** — paridad de constructo (§4.5) · **barrera de TIPO: la función de agregación de
    faceta exige un `BridgeApproval` (tipo construible SOLO desde un ADR registrado) para aceptar evidencia de
    modalidades distintas; sin él, no compila/no existe la ruta** · exclusión del carril de medición · N/A ·
    **TEST de propiedad: intentar fundir evidencia E01 (single-tap) + E07 (choice) en un score de faceta sin
    BridgeApproval ⇒ imposible por construcción (el test verifica que la única factory de BridgeApproval exige
    el id del ADR)** · fusión accidental de modalidades · S9 harness M0-M5 · requiere ADR-SCI-008
19. ★ **60/120 Hz, jank, frame loss** — detección de Hz real por segmento (fix C.1 — TAREA EXPLÍCITA de S5)
    + FrameStatsCalculator · segmento NO-GO por drops/jitter/discontinuidad · invalidación PROPORCIONAL por
    segmento (RT fuera, accuracy conservada) · segmento GO reanuda · **TEST: el MISMO stream sintético de
    frame-times a ~8.3ms → `fromFrameTimes(refreshHz=120)` clasifica limpio Y `fromFrameTimes(refreshHz=60)`
    lo clasifica distinto — prueba que la clase depende del Hz real; segundo test: stream 120Hz con drops
    reales (deltas 16.6ms) → con refreshHz=120 detecta drops, con 60 hardcodeado NO (documenta el bug)** ·
    sin C.1 la clase T se infla en silencio · S5 · T-S5
20. **Background/process death** — write-ahead antes del primer trial (seed sellada) · RecoveryScanner ·
    restore fail-closed (9 razones tipadas) · reanudar desde checkpoint o ABANDONED · kill -9 en
    STARTED/mid-block/pre-seal → recover() produce exactamente {recuperada | razón tipada de rechazo}, nunca
    duplica ni silencia · práctica visible con disclaimer si el write-ahead falló · S3 · T-S3
21. **Llamada/interrupción** — `SessionInterrupted` (estándar 16) · pérdida de foco · pausa automática; el
    trial en curso EXCLUIDO de RT (contaminado) · `SessionResumed`; el trial interrumpido se descarta · trial
    interrumpido → su RT jamás contribuye a métricas · tratarlo como trial normal = violación · S3 · T-S3
22. ★ **Rotación/recomposición** — reducer puro + estado serializable · el trial en curso no se resetea ·
    estado sobrevive · N/A · **TEST (JVM, sin emulador): round-trip de serialización del estado del reducer a
    mitad de ventana de respuesta → el estado restaurado conserva trial-index, deadline y cronómetro lógico;
    + regla de diseño: el deadline del trial se ancla al reloj MONOTÓNICO del motor, no al lifecycle del
    composable (la recomposición no re-arma la ventana)** · trial perdido al rotar · S3 (tarea explícita de
    su suite) · T-S3
23. **Offline** — diseño local-only · N/A · el offline ES el camino normal · N/A · sesión completa sin red →
    persiste idéntica a online · ninguno · S3 · T-S3
24. **Sync repetido/duplicado** — **NOT_APPLICABLE DECLARADO** para S1-S9 (Focus v2 local-only; sync bloqueado
    hasta ADR-FOCUS-DATA + ADR-DATA-001; prohibido reutilizar SyncQueue). Si algún día se aprueba: idempotencia
    por sessionId (patrón upsert existente) con test "2 syncs ⇒ 1 fila" · — · — · — · fuera de alcance con
    owner (plan §11) · T-S8 futuro
25. ★ **Fuera de orden (journal)** — `sequenceNo` estrictamente creciente · lectura valida secuencia
    (`requireOrderedUnique`, patrón FormDescriptorV1:118-123) · rechazo tipado, jamás re-ordenar en silencio ·
    N/A · **TEST: journal sintético con seq=5 antes que seq=4 → rechazo tipado en lectura; ASIGNACIÓN
    CORREGIDA: el diseño del sequenceNo es tarea de S8 (journal), NO de S4 — el GAP del contrato de datos
    queda con dueño: S8, gate T-S8** · orden implícito por wall-clock = violación · S8 · T-S8
26. ★ **Reloj civil/zona/DST** — timing del motor SOLO monotónico (MonotonicClock); la seed JAMÁS wall-clock
    no persistida (anti-patrón documentado: FocusGraph.kt:131-140) · property test · ninguna necesaria si el
    diseño cumple · N/A · **TEST (S3, tarea explícita): (a) property — mover el wall-clock ±13h a mitad de
    sesión no altera duración de trial, orden ni deadline (todo deriva de TimeSource.Monotonic); (b) golden —
    la seed de TODA sesión v2 proviene de SeedSource inyectado y queda persistida en el FormDescriptor ANTES
    del primer trial (assert: descriptor.seedHex existe y no cambia)** · seed wall-clock no persistida ⇒
    irreproducible para siempre (learning 2026-07-15) · S3 · T-S3
27. **Medianoche** — day-cap del evidence layer reutilizado · conteo de días a través de medianoche · N/A ·
    N/A · sesiones 23:59 y 00:05 → 2 días distintos (test heredado de core.session.cognitive.evidence) ·
    conteo erróneo distorsiona madurez · S1 · T-S1
28. **Schema futuro desconocido** — schemaVersion + readVersioned fail-closed · versión>conocida · quarantine
    tipado, jamás parcial · cuarentena hasta upgrade · DTO v99 leído por reader v3 → UnsupportedVersion, sin
    crash · deserializar "lo que se pueda" = violación · S1 · T-S1
29. **Corrupción** — cifrado+integridad del SecureKeyValueStore · `Corrupt` · quarantine · descarte · bytes
    corruptos en mindboost_focus2 → Corrupt→quarantine (patrón 2026-07-05 fail-closed simétrico) · fail-open
    = la violación ya corregida en el repo · S1 · T-S1
30. **Migración parcial** — reader N lee N y N-1; sin migración batch destructiva · mezcla de versiones por
    fila · cada fila resuelve por su propio tag · N/A · namespace con filas v1+v2 → cada lectura independiente;
    **la ventana de retención de readers queda DECIDIDA EN ADR-FOCUS-MIGRATION con gate de cierre = T-S1
    (la decisión "N-1" se ratifica o cambia al decidir el ADR, no queda abierta)** · criterio indefinido ·
    S1 · T-S1
31. **Permiso denegado** — **DECLARACIÓN EXPLÍCITA: Focus v2 NO introduce NINGÚN permiso de plataforma nuevo**
    (storage cifrado = Keystore/Keychain sin permiso runtime; sin notificaciones, sin sensores, sin red).
    Si una slice futura pretende añadir uno ⇒ CAT-008 (minimización, just-in-time, alternativa) + revisión
    privacy · N/A · N/A · N/A · test estático: el manifest de androidApp no gana permisos por focus2 ·
    permiso no declarado que aparece en el diff = hallazgo de review · S3/S5 · T-S5
32. **Revocación en caliente** — SubjectRevocationAuthority triádico · estado ANTES de payload ·
    STATUS_UNAVAILABLE ⇒ Failed(retryable), nunca ausencia fabricada · reintento · revocation-store falla con
    payload-store sano → Failed(REVOCATION_STATUS_UNAVAILABLE) (test discriminante patrón 2026-07-14) ·
    fail-open = violación ya corregida · S5 · T-S5
33. ★ **TalkBack/screen-reader activo** — detección de servicio de accesibilidad activo al iniciar sesión ·
    flag registrado en la sesión · **regla de constructo: con screen-reader activo, las métricas RT-CRÍTICAS
    de la sesión quedan excluidas de evidencia oficial (el constructo temporal cambia); accuracy y práctica SE
    CONSERVAN ÍNTEGRAS — el usuario entrena con feedback completo de exactitud, nunca se le excluye del
    ejercicio** · N/A · **TEST: sesión sintética con flag a11y activo → RT excluido del carril oficial,
    accuracy OFICIAL, práctica intacta; sin flag → sin exclusión (mitad discriminante)** · contaminar el
    carril RT o excluir del ejercicio entero son AMBOS fallos de equidad · S3 (captura del flag) + S5 (gate) ·
    T-S3+T-S5
34. ★ **Switch-access (1-2 botones)** — **requisito de diseño NUEVO (cierra la ausencia detectada): todo
    ejercicio choice L/R DEBE ofrecer modalidad de entrada secuencial alternativa (focus alterna entre las 2
    opciones con selección por activación única), y esa modalidad se registra como MODALIDAD DISTINTA** ·
    detección del servicio activo · la sesión switch-access es práctica completa; su evidencia es de SU
    modalidad (no comparable con touch sin bridge — regla #18) · N/A · **TEST: (a) navegabilidad — con
    switch-access sintético, E07 es completable de punta a punta; (b) el registro de modalidad de la sesión
    = SWITCH_SEQUENTIAL y su evidencia jamás se agrega con TOUCH_DIRECT sin BridgeApproval** · un ejercicio
    incompletable con switch-access es un fallo de equidad estructural · S3 (diseño) + S5 (verificación) ·
    T-S3+T-S5
35. ★ **Fatiga/entrenamiento excesivo** — cap de bloques largos (1/día) + **cap GENERAL de dosis de EVIDENCIA
    oficial diaria por ejercicio (interino conservador, valor exacto = ADR canónico ADR-SCI-005; el exceso
    JAMÁS se bloquea como práctica: pasa a TRAINING_FREE, SES-008)** · conteo de sesiones/evidencia por día ·
    exceso = práctica sin evidencia (no castigo) · N/A · **TEST: 20 sesiones de E07 en un día → las primeras
    N (cap interino) elegibles como evidencia, el resto TRAINING_FREE automático; el usuario nunca ve un
    bloqueo de jugar** · sin cap, el grinding contamina la evidencia; con bloqueo duro, se castiga el hábito
    (ambos errores) · S8 + portfolio regla 7 ampliada · T-S8
36. **Outlier legítimo (experto real)** — mitad discriminante A4 ampliada · **el oráculo T-ADV incluye DOS
    perfiles honestos: promedio (acc~0.92, RT variable) y EXPERTO (acc~0.97, RT rápido pero CORRELADO con
    dificultad/ISI, no metronómico)** · ninguno dispara detectores · N/A · TEST: EXPERT no dispara D1-D11 ni
    pierde score (extensión de A4) · falso positivo contra expertos = castigo por competencia · T-ADV S3/S4 ·
    T-ADV
37. **Flag mal configurada** — default fail-closed en las TRES impls · test de default · flag OFF inerte por
    diseño · flip restaura · valor inesperado/null/remota corrupta → default false (oculta); "OFF ⇒ 0
    superficies numéricas" + "ON ⇒ restaura 3" (S0) · flag que expone el score = reintroducción del P0 ·
    S0 · T-S0
38. **Analytics sin consentimiento** — `consent_changed` primero + ObservabilityHub única puerta · evento sin
    scope activo · el evento NO sale (drop local, breadcrumb técnico) · N/A · **TEST de frontera: emitir un
    evento agregado de focus2 sin consentimiento del scope → nada llega al sink (fake sink capturando);
    con scope activo → llega. Gate asignado: T-S1 (el contrato de consentimiento es de S1) y re-verificado
    en T-REL** · evento fugado = violación de privacidad · S1+S10 · T-S1+T-REL
39. **Borrado de cuenta durante operación en vuelo** — hold-through-erase (lock por-sujeto) · escritura
    concurrente con borrado · escritura in-flight rechazada tipada o el borrado espera el release · tras el
    borrado, toda escritura posterior falla · upsert en curso durante executeAccountDeletion → outcome tipado
    (patrón RevocationGapsTest existente) · escritura que sobrevive al borrado = hueco GDPR · S1 · T-S1
40. **(= #16)** rank-flip — mismo test; registrado dos veces por exigencia del mandato · S9 · requiere V3+ADR
41. **Purpose desconocido/no-aplicable** — disposition 3-vías sellado · deserialización de valor desconocido ·
    Indeterminate (fail-closed) · práctica conservada · enum futuro → Indeterminate, nunca crash ni elegible;
    NotEligibleByPurpose ≠ Indeterminate (test exhaustivo S1) · UNKNOWN como "no aplicable" = violación P1-8 ·
    S1 · T-S1
42. ★ **8 estados UX — transiciones inválidas** — **matriz de transiciones SELLADA como tabla de datos
    (estado × evento → estado’ | RECHAZO) en el contrato de S1; la máquina solo transita por lookup en la
    tabla** · property test exhaustivo: TODA pareja (estado, evento) no listada ⇒ permanece + rechazo tipado ·
    permanece en el estado actual · N/A · **TEST: (a) exhaustivo 8×|eventos|: cada celda o está en la tabla o
    rechaza; (b) casos dirigidos: WITHHELD_TECHNICAL→CURRENT sin gates ⇒ RECHAZO; STALE→CURRENT sin
    FreshEvidence ⇒ RECHAZO; v1-oculto NUNCA entra como RETIRED_VERSION (reservado a versiones que estuvieron
    CURRENT)** · transición ilegal sin test = estado fabricado · S1 · T-S1
43. ★ **Journal autorizado — scope-creep** — **schema como DTO SELLADO con serializer cerrado (sin campos
    extra posibles por construcción) + `ignoreUnknownKeys=false` en el reader del journal: un campo
    desconocido ⇒ rechazo tipado del registro** · validación en deserialización · registro rechazado, sesión
    marcada WITHHELD_TECHNICAL para outcomes replay-dependientes · N/A · **TEST: blob de journal con campo
    extra "touchX" → rechazo tipado; blob conforme → acepta (mitad discriminante)** · recolección silenciosa
    por scope-creep · S8 · T-S8
44. **Journal denegado (opción i)** — outcomes replay-dependientes BLOCKED explícitos · intento de re-scoring
    retroactivo/forense · rechazo con razón, jamás "aproximado" silencioso · N/A · re-scoring histórico con
    ADR=(i) → BLOCKED(NO_RAW_DATA), nunca mejor-esfuerzo sin declarar · fabricar aproximación no declarada =
    violación de decisión cerrada · S1/S9 · T-S1
45. **Almacenamiento lleno** — **cap de claves/tamaño (GAP HOY — el mecanismo NO existe; tarea explícita de
    S1 si ADR-DATA=(ii)/(iii): construir el cap ANTES de cualquier journal)** · escritura sobre cap · rechazo
    tipado, jamás truncar · N/A · encolar N+1 sobre el cap → rechazo tipado (patrón MAX_PLANNED_TRIALS) ·
    sin cap = DoS de storage local · S1 · T-S1
46. **Android API 26 (si se ratifica)** — ADR-FOCUS-PLATFORM-MIN decide primero · protocolo por banda ·
    clase T1 máxima; presupuestos de gama baja · N/A · dispositivo físico API26-28 con el protocolo de 5
    pasos → GO/NO-GO por clase (umbrales DESIGNED_NOT_RUN); **dependencia declarada: sin el hardware físico
    del inventario, S5 NO cierra para esta banda** · cerrar S5 sin el hardware = falso verde · S5 · T-S5
47. **iOS 15** — probe stub declarado (no se finge medición) · ausencia de instrumentación conocida · RT
    excluido TOTAL en iOS hasta port CADisplayLink; accuracy OFICIAL · port (1-2 días, requiere Mac+iPhone) ·
    sesión en iPhone → clase T0-para-RT; accuracy=OFICIAL, RT=NINGUNO (tabla mobile-perf §E) · RT iOS tratado
    como comparable = violación · S5 · T-S5 (BLOCKED_IOS hasta port)

## Cobertura

47 escenarios; los 11 que llegaron [SIN ORÁCULO] del orden 3 (★: 11, 13, 16/40, 18, 19, 22, 25, 26, 33, 42,
43) resueltos con oráculo concreto; #34 (switch-access, ausencia total) cubierto con requisito de diseño +
2 tests; #24 y #31 declarados NOT_APPLICABLE con condición de reapertura explícita; #35 ampliado de "solo
bloques largos" a cap general de evidencia con exceso→TRAINING_FREE (SES-008). Umbrales numéricos de nuevos
detectores (cherry-picking, periodicidad, rank-flip) = DESIGNED_NOT_RUN con revisión al llegar datos.
