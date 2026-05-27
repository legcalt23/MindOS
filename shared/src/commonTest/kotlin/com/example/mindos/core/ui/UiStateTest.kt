package com.example.mindos.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UiStateTest {

    @Test
    fun loadingIsLoading() {
        assertTrue(UiState.Loading.isLoading)
    }

    @Test
    fun submittingIsLoading() {
        assertTrue(UiState.Submitting.isLoading)
    }

    @Test
    fun refreshingIsLoading() {
        assertTrue(UiState.Refreshing("data").isLoading)
    }

    @Test
    fun successIsNotLoading() {
        assertFalse(UiState.Success("data").isLoading)
    }

    @Test
    fun errorIsError() {
        val error = UserFacingError("title", "msg", "code")
        assertTrue(UiState.Error(error).isError)
    }

    @Test
    fun successDataOrNull() {
        assertEquals("hello", UiState.Success("hello").dataOrNull)
    }

    @Test
    fun refreshingPreservesPreviousData() {
        assertEquals("old", UiState.Refreshing("old").dataOrNull)
    }

    @Test
    fun errorDataOrNullIsNull() {
        val error = UserFacingError("title", "msg")
        assertNull(UiState.Error(error).dataOrNull)
    }

    @Test
    fun idleDataOrNullIsNull() {
        assertNull(UiState.Idle.dataOrNull)
    }

    @Test
    fun loadingDataOrNullIsNull() {
        assertNull(UiState.Loading.dataOrNull)
    }

    @Test
    fun emptyIsNotError() {
        assertFalse(UiState.Empty().isError)
    }

    @Test
    fun offlineIsNotLoading() {
        assertFalse(UiState.Offline.isLoading)
    }
}
