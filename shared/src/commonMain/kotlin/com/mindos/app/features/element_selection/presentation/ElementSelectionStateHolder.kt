package com.mindos.app.features.element_selection.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mindos.app.features.element_selection.domain.ElementMaster
import com.mindos.app.features.element_selection.domain.ElementMasterCatalog
import com.mindos.app.features.element_selection.domain.ElementType

data class ElementSelectionUiState(
    val masters: List<ElementMaster>,
    val selection: SelectionState = SelectionState.Idle,
    val intenseElement: ElementType? = null,
    val impactSeed: Int = 0,
) {
    val selectedElement: ElementMaster?
        get() = (selection as? SelectionState.Expanded)?.element
}

@Stable
class ElementSelectionStateHolder(
    masters: List<ElementMaster> = ElementMasterCatalog.masters,
) {
    var uiState by mutableStateOf(ElementSelectionUiState(masters = masters))
        private set

    fun onOrbTapped(element: ElementMaster) {
        val current = uiState.selectedElement
        uiState = if (current?.type == element.type) {
            uiState.copy(impactSeed = uiState.impactSeed + 1)
        } else {
            uiState.copy(selection = SelectionState.Expanded(element))
        }
    }

    fun onOutsideTapped() {
        if (uiState.selection != SelectionState.Idle) {
            uiState = uiState.copy(
                selection = SelectionState.Idle,
                intenseElement = null,
            )
        }
    }

    fun onLongPressStarted(element: ElementMaster) {
        uiState = uiState.copy(
            selection = SelectionState.Expanded(element),
            intenseElement = element.type,
        )
    }

    fun onLongPressEnded(element: ElementMaster) {
        if (uiState.intenseElement == element.type) {
            uiState = uiState.copy(intenseElement = null)
        }
    }
}

@Composable
fun rememberElementSelectionStateHolder(
    masters: List<ElementMaster> = ElementMasterCatalog.masters,
): ElementSelectionStateHolder = remember(masters) {
    ElementSelectionStateHolder(masters = masters)
}
