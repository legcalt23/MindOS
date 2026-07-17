# Fuente de verdad QA específica de MindBoost

## Identidad y criterio rector

MindBoost es un entrenador cognitivo serio, adaptativo, medible y personalizado. No es una app médica, terapéutica, diagnóstica ni un juego cerebral infantil. Toda validación debe proteger, por este orden:

1. Entrenamiento real.
2. Medición clara y coherente.
3. Adaptación controlada.
4. Progreso demostrable dentro de la app.
5. Confianza y privacidad.
6. Elite como capa de comprensión, no como bloqueo del entrenamiento base.

Leer primero `.agents/qa-system/MindBoost_PRD_v1.0.md`, copia íntegra del PRD 1.0 del 1 de julio de 2026 incluida en el sistema. Después buscar en el repositorio una versión posterior aprobada cuyo nombre contenga `MindBoost`, `PRD` o `Product Requirements`. Una versión posterior solo sustituye a la incluida si declara versión/fecha/owner y su aprobación puede demostrarse. Este resumen fija mínimos de QA y sirve de índice; no sustituye el texto completo.

## Plataforma y stack del MVP

- Kotlin Multiplatform.
- Compose Multiplatform.
- Firebase Firestore y Firebase Auth.
- Persistencia/caché local multiplataforma.
- RevenueCat o stores nativas para Elite.
- API de IA mediante backend intermedio recomendado.
- Android mínimo: API 26 / Android 8.0.
- iOS mínimo: iOS 15.
- La sesión iniciada debe continuar sin red, guardarse localmente y sincronizarse al recuperar conexión.

No asumir nombres de módulos. Descubrirlos en `settings.gradle(.kts)`, catálogos, targets KMP y configuración real.

## Ocho motores cognitivos

1. Enfoque.
2. Decisiones.
3. Lógica.
4. Creatividad.
5. Lenguaje.
6. Emociones.
7. Velocidad mental.
8. Memoria.

Cada motor requiere score 0–100, estado provisional/calibrado, rango independiente, historial y contribución al score global. Emociones nunca debe presentarse como terapia o diagnóstico. Velocidad nunca debe premiarse cuando destruye precisión.

## Flujos de misión crítica

### F1 — Cuenta y acceso

Registro → login → persistencia de sesión → logout → login posterior. No puede existir acceso cruzado entre usuarios.

### F2 — Onboarding y evaluación

Bienvenida → objetivos → bloques prioritarios → hábitos/tiempo → evaluación de 6–10 minutos cubriendo ocho bloques → perfil provisional → primer plan. No asignar rango oficial durante la evaluación inicial.

### F3 — Entrenamiento

Elegir bloque → elegir duración (120 s, 5, 10, 20 o 30 min) → iniciar ejercicio → responder → completar → calcular resultado → persistir → actualizar perfil → mostrar recomendación. La IA no puede bloquear este flujo.

### F4 — Calibración y rango

Cinco sesiones válidas por bloque, completadas, de al menos 120 segundos y sin anomalías extremas → bloque calibrado → rango oficial. Antes: `Perfil provisional`.

### F5 — Offline y recuperación

Iniciar sesión de entrenamiento online → perder red → continuar → terminar → guardar local → matar/reabrir app → recuperar red → sincronizar exactamente una vez → mismo score y mismo historial local/cloud.

### F6 — Elite

Paywall → sandbox de compra → entitlement → IA Coach → recomendación → explicación → confirmación humana → aplicar al plan → restaurar compra. El entrenamiento gratuito permanece ilimitado y sin anuncios.

### F7 — Eliminación y privacidad

El usuario puede acceder a privacidad y eliminar cuenta conforme al producto. La IA recibe resúmenes mínimos, no datos crudos ilimitados.

## Datos mínimos de sesión a preservar

Identidad de usuario y sesión; tiempo; bloque/ejercicio; duración elegida y real; dificultad inicial/final; nivel máximo; totales, aciertos, errores y omisiones; precisión; tiempos de reacción y variabilidad; score bruto/normalizado/delta; XP; calibración; anomalías; completada/abandonada; versiones de ejercicio/scoring; plataforma; sync status.

Toda corrección debe preservar compatibilidad y significado. Campos faltantes, duplicados, valores no finitos, timestamps incoherentes o versiones ausentes son defectos.

## Reglas de IA Coach

- Trabajar principalmente con resúmenes estructurados.
- No diagnosticar, recomendar medicación, sustituir profesionales ni prometer resultados.
- No modificar planes sin confirmación explícita.
- No inventar datos ni usar miedo/culpa para convertir.
- Respetar el límite diario configurado sin afectar entrenamiento.

## Privacidad obligatoria

- Sin DNI en MVP.
- Minimización de datos.
- Consentimiento separado para IA Coach si utiliza progreso.
- Separación entre entrenamiento y facturación.
- No venta de datos.
- Logs mínimos y redactados.
- GDPR desde lanzamiento en España/Europa.

El sistema QA puede observar que existen controles y recorridos, pero cualquier cambio de seguridad o privacidad se deriva a Codex/owner y no se aplica dentro de `/qa`.

## Bugs que fuerzan `FAIL`

- Pérdida, corrupción o duplicación grave de progreso.
- Crash, ANR o bloqueo durante un flujo afectado.
- Score, rango, calibración o Global Score incoherente.
- Registro/login imposible o acceso cruzado.
- Resultado no guardado o resincronización incorrecta.
- Elite no desbloquea tras pago sandbox o se desbloquea sin entitlement.
- IA usa datos de otro usuario, datos incorrectos o modifica plan sin confirmación.
- Evaluación inicial no termina o asigna rango definitivo.
- App inutilizable sin red después de iniciar una sesión.
- UI principal inutilizable en Android o iOS aplicable.
- Exposición de datos, secretos o PII.
- Regresión que impida entrenar cualquiera de los ocho bloques alcanzados por el cambio.

## Experiencia visual que debe conservarse

Dark-first, científica, cósmica sutil, minimalista, premium, calmada y de alto rendimiento. Evitar saturación, aspecto infantil, recompensa superficial y gamificación vacía. El dashboard debe comunicar en cinco segundos: estado actual, entrenamiento recomendado y progreso.
