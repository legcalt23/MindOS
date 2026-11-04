# Firebase Rules - salida saneada

- Emulator Suite local, no produccion.
- Firestore Security Rules: 136/136 aserciones PASS.
- Storage Rules: 44 aserciones dinamicas PASS.
- Storage static guard: 10 checks PASS.
- Negativos de ownership/cross-user/schema observaron denegaciones esperadas.
- Algunos negativos complejos de `profile_visuals` alcanzaron el limite de 1.000 expresiones y fallaron cerrado; se recomienda reducir complejidad.
- No se incluyen UIDs, tokens, payloads ni logs completos.

