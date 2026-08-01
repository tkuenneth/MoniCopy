/*
 * Copyright 2017 - 2026 Thomas Kuenneth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thomaskuenneth.monicopy.app

import androidx.lifecycle.ViewModel
import com.thomaskuenneth.monicopy.platform.OperatingSystem
import com.thomaskuenneth.monicopy.platform.PlatformInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.KoinViewModel

enum class SheetVisibility {
    Hidden, Visible,
}

data class AppUiState(
    val platformName: String,
    val appVersion: String,
    val appBuildVersion: String,
    val operatingSystem: OperatingSystem,
    val showExtendedAboutDialogCheckbox: Boolean,
    val aboutVisibility: SheetVisibility = SheetVisibility.Hidden,
    val settingsVisibility: SheetVisibility = SheetVisibility.Hidden,
    val colorSchemeMode: ColorSchemeMode = ColorSchemeMode.System,
    val showExtendedAboutDialog: Boolean = false,
)

@KoinViewModel
class AppViewModel(
    private val repository: AppRepository,
    platformInfo: PlatformInfo,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AppUiState(
            platformName = platformInfo.platformName,
            appVersion = platformInfo.appVersion,
            appBuildVersion = platformInfo.appBuildVersion,
            operatingSystem = platformInfo.operatingSystem,
            showExtendedAboutDialogCheckbox = platformInfo.showExtendedAboutDialogCheckbox,
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
