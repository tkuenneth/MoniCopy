package com.thomaskuenneth.monicopy.app

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class SheetVisibility {
    Hidden, Visible,
}

data class AppUiState(
    val aboutVisibility: SheetVisibility = SheetVisibility.Hidden,
    val settingsVisibility: SheetVisibility = SheetVisibility.Hidden,
    val colorSchemeMode: ColorSchemeMode = ColorSchemeMode.System,
    val showExtendedAboutDialog: Boolean = false,
)

class AppViewModel(
    private val repository: AppRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AppUiState(
            colorSchemeMode = repository.getColorSchemeMode(),
            showExtendedAboutDialog = repository.getShowExtendedAboutDialog(),
        ),
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun showAboutSheet(show: Boolean) {
        _uiState.update { state ->
            state.copy(
                aboutVisibility = if (show) {
                    SheetVisibility.Visible
                } else {
                    SheetVisibility.Hidden
                },
            )
        }
    }

    fun showSettingsSheet(show: Boolean) {
        _uiState.update { state ->
            state.copy(
                settingsVisibility = if (show) {
                    SheetVisibility.Visible
                } else {
                    SheetVisibility.Hidden
                },
            )
        }
    }

    fun setColorSchemeMode(colorSchemeMode: ColorSchemeMode) {
        _uiState.update { it.copy(colorSchemeMode = colorSchemeMode) }
        repository.setColorSchemeMode(colorSchemeMode)
    }

    fun setShowExtendedAboutDialog(showExtendedAboutDialog: Boolean) {
        _uiState.update { it.copy(showExtendedAboutDialog = showExtendedAboutDialog) }
        repository.setShowExtendedAboutDialog(showExtendedAboutDialog)
    }
}
