# MindBoost Product Doctrine

MindBoost es el entrenador cognitivo (Android + iOS) dentro de Mindverse. Debe comunicar
disciplina, progreso medible y entrenamiento adaptativo personal — con **honestidad calibrada**.

## Posición estratégica

No es un juego de puzzles casual: es un entrenador diario de la capacidad cognitiva. La experiencia
debe hacer que el usuario entienda rápido tres cosas:

1. Su línea base cognitiva actual.
2. Qué está entrenando hoy.
3. Si está mejorando contra sí mismo con el tiempo.

## Tesis de MVP

El MVP debe ser estrecho, serio y completo. Evita muchos mini-juegos superficiales. Un MVP creíble
gana haciendo que uno o dos loops de entrenamiento se sientan precisos, adaptativos y medibles.

Núcleo recomendado:

1. Onboarding con intención + calibración de baseline.
2. Un set pequeño de ejercicios de alta calidad mapeados a dominios cognitivos.
3. Dificultad adaptativa por rendimiento (precisión, tiempo de reacción, racha, señales de fatiga).
4. Plan de sesión diario que se sienta personalizado.
5. Feedback tras cada sesión.
6. Dashboard de progreso con métricas longitudinales.
7. Loop de hábito: racha, objetivo diario, recuperación significativa tras días perdidos.

## Dominios cognitivos (dimensiones de diseño, no claims)

Atención · memoria de trabajo · velocidad de procesamiento · control inhibitorio · flexibilidad
cognitiva · reconocimiento de patrones · codificación/recuperación de memoria · metacognición ·
resistencia mental. Úsalos como dimensiones de diseño y de métrica, **no** como afirmaciones
clínicas.

## Reglas de calidad de feature

Cada feature debe responder:

- ¿Qué dominio cognitivo entrena?
- ¿Qué señal mide?
- ¿Cómo adapta la dificultad?
- ¿Cómo sabe el usuario que mejoró (contra su propia línea base)?
- ¿Cómo mejora la retención sin volverse manipulador?

Si no responde a esto, probablemente no es MVP-crítico.

## Rigor charter — honestidad (NO negociable)

Estos invariantes distinguen a MindBoost de un brain-game de humo. Un buen prompt `/ceo` los
preserva siempre:

- **`null ≠ 0`.** Un dato insuficiente o no válido es `CALIBRATING` / "—", **nunca** un 0
  fabricado. La UI muestra "—", no un cero que parezca rendimiento real.
- **Gate único al perfil.** El perfil cognitivo se actualiza **solo** a través de un `ScoreResult`
  sellado que ha pasado sus gates: muestra mínima (`MinSampleGate`), timebase autoritativo para
  métricas RT-críticas, integridad de banco (`BankValidator`), y anti-gaming.
- **Anti-gaming testeado.** Un detector "cuenta" solo si existe un adversario sintético que lo
  dispara Y un usuario honesto realista que NO lo dispara (red-team). Nada de detectores sin prueba
  adversarial.
- **Cobertura declarada por el detector.** La fuerza de un claim de perfil (ADEQUATE/PARTIAL/NONE)
  la declara el detector, no el consumidor; sin detector calibrado ⇒ la dimensión sigue
  `CALIBRATING`.
- **Sin señal fabricada.** No cablear al perfil un ejercicio cuyo contenido no sea juzgable
  (etiqueta correcta aleatoria, descripción-plantilla, respuesta no seleccionable). Auditar el
  contenido, no solo el scorer.

## Disciplina de claims

Evita afirmaciones no respaldadas:

- "Aumenta el IQ", "Trata el TDAH", "Previene la demencia", "Clínicamente probado" (sin validación
  real), "Mejora cognitiva garantizada", "mejora tu vida real" (transferencia lejana no respaldada).

Lenguaje aceptable:

- "Entrena atención y memoria de trabajo mediante ejercicios estructurados."
- "Sigue tu progreso contra tu propia línea base."
- "La dificultad adaptativa mantiene las sesiones retadoras pero alcanzables."
- "Construye un hábito diario de entrenamiento cognitivo."

Hay scanners de build (`mindboostCopySafetyScanner`, `arenaTerminologyScanner`) que bloquean léxico
prohibido **incluso en comentarios y disclaimers negados** ("no es un test de IQ" también falla). El
prompt no debe introducir ese léxico.

## Estándar de UX

La app debe sentirse: seria · clara · rápida · medible · minimalista · premium · no infantil · no
sobrecargada. Menos pantallas con lógica más fuerte. La claridad de progreso vence a la decoración
visual. (Animaciones **solo** bajo petición explícita de pulido.)
