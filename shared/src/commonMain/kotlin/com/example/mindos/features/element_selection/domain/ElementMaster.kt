package com.example.mindos.features.element_selection.domain

data class ElementMaster(
    val type: ElementType,
    val name: String,
    val title: String,
    val skyLore: String,
    val baseColor: Long,
    val coreColor: Long,
)
