package com.aistudio.cleanmind.app

import com.aistudio.cleanmind.app.presentation.home.HomeViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for HomeViewModel in Phase 1 (Foundation).
 * Validates that the ViewModel exposes a strictly neutral, unanalyzed state
 * without fake or fabricated storage values.
 */
class HomeViewModelTest {

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        viewModel = HomeViewModel()
    }

    @Test
    fun initialState_hasUnanalyzedValues() {
        val state = viewModel.uiState.value

        assertFalse("Storage must not be marked as analyzed initially", state.isAnalyzed)
        assertFalse("Should not be actively analyzing initially", state.isAnalyzing)
        assertNull("Used space must not have fictitious values", state.usedSpaceFormatted)
        assertNull("Free space must not have fictitious values", state.freeSpaceFormatted)
        assertNull("Total space must not have fictitious values", state.totalSpaceFormatted)
        assertNull("Percentage must be null initially", state.usedPercentage)
        assertEquals("Análise ainda não realizada", state.statusMessage)
        assertFalse("Settings dialog should be hidden initially", state.showSettingsDialog)
    }

    @Test
    fun onSettingsClicked_opensSettingsDialog() {
        viewModel.onSettingsClicked()
        assertTrue(viewModel.uiState.value.showSettingsDialog)
    }

    @Test
    fun onDismissSettings_closesSettingsDialog() {
        viewModel.onSettingsClicked()
        assertTrue(viewModel.uiState.value.showSettingsDialog)

        viewModel.onDismissSettings()
        assertFalse(viewModel.uiState.value.showSettingsDialog)
    }
}
