package com.mindos.app.features.element_selection.domain

object ElementMasterCatalog {
    val masters: List<ElementMaster> = listOf(
        ElementMaster(
            type = ElementType.Gravity,
            name = "Gravedad",
            title = "La Ley del Centro",
            skyLore = "No empuja. No amenaza. La Gravedad simplemente llama, y todo lo falso pierde altura. Quien escucha su pozo interior aprende que la fuerza no nace del movimiento, sino de la capacidad de sostener el mundo sin romperlo.",
            baseColor = 0xFFFF3E8A,
            coreColor = 0xFF050407,
        ),
        ElementMaster(
            type = ElementType.Shadow,
            name = "Sombra",
            title = "El Umbral sin Rostro",
            skyLore = "La Sombra no niega la luz: la protege del orgullo. En su humo viven las decisiones que nadie vio, los duelos que no pidieron testigo y la calma antigua de quien puede entrar en la noche sin convertirse en ella.",
            baseColor = 0xFF6F5A8F,
            coreColor = 0xFF0B0912,
        ),
        ElementMaster(
            type = ElementType.Light,
            name = "Luz",
            title = "El Juramento Dorado",
            skyLore = "La Luz permanece en el centro porque no compite por el centro. Su pulso no quema: revela. Donde toca, cada grieta deja de ser vergüenza y se vuelve mapa para caminar con nobleza hacia la verdad.",
            baseColor = 0xFFFFD97A,
            coreColor = 0xFFFFF6CF,
        ),
        ElementMaster(
            type = ElementType.Lightning,
            name = "Rayo",
            title = "La Decisión Instantánea",
            skyLore = "El Rayo es pensamiento convertido en filo. Cruza el vacío antes de que el miedo termine su frase. No concede comodidad, pero entrega una certeza: cuando el espíritu decide, el cuerpo aprende a obedecer.",
            baseColor = 0xFF8F5CFF,
            coreColor = 0xFFE8F0FF,
        ),
        ElementMaster(
            type = ElementType.Air,
            name = "Aire",
            title = "La Forma que Respira",
            skyLore = "El Aire no se deja poseer. Rodea, atraviesa, perdona y vuelve. Su maestro enseña que la libertad no es huir del peso, sino moverse con tanta presencia que ninguna cadena encuentra dónde cerrarse.",
            baseColor = 0xFF5FE4DE,
            coreColor = 0xFFE7FFFB,
        ),
    )
}
