package com.example.neuv3.features.element_selection.presentation

import com.example.neuv3.features.element_selection.domain.ElementMaster

sealed class SelectionState {
    data object Idle : SelectionState()
    data class Expanded(val element: ElementMaster) : SelectionState()
}
