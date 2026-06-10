package com.mindos.app.features.profile.presentation.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Attaches an accessibility content description to any element. */
internal fun Modifier.semanticsLabel(label: String): Modifier =
    this.semantics { contentDescription = label }
