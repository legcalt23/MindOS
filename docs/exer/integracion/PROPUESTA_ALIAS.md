# Activación aprobada · alias conversacional

El usuario aprobó el 2026-09-15: «Sí, añade el alias conversacional /exer».
Este bloque se añadió a `AGENTS.md`, conservando su contenido previo. La prueba en una nueva interacción sigue pendiente.

```markdown
### `/exer`

Cuando un mensaje comience exactamente por `/exer` como palabra completa,
carga `docs/exer/SKILL.md` y aplica la entrada y los módulos pertinentes.
Si el mensaje es solo `/exer`, recupera el checkpoint y delimita el siguiente
trabajo de diseño. Los sufijos (`incorporar-auditoria`, `disenar`, `revisar`,
`proponer-motor`, `continuar`) son instrucciones de chat.

Este alias es una convención conversacional de este proyecto, no un comando
nativo registrado en el cliente. Solo opera si el cliente entrega el texto
al modelo. EXER diseña; no implementa ejercicios ni modifica el motor,
scoring, flags, datos o despliegues. Las decisiones explícitas actuales
del usuario gobiernan sobre las fuentes históricas.
```

**Prueba posterior:** en una nueva interacción del proyecto Molvex, enviar
`/exer continuar`; observar que el modelo carga la entrada y el checkpoint.
Si el cliente intercepta el texto, documentar el resultado y elegir otro
mecanismo con el usuario. No sustituir silenciosamente por `$exer`.

La alternativa de skill local usaría una carpeta de descubrimiento admitida
por el cliente con nombre `exer`, preservando el árbol completo de referencias.
No se ha instalado una segunda copia ni alterado configuración personal.
