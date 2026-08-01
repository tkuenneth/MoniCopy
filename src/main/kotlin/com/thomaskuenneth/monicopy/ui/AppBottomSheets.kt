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
package com.thomaskuenneth.monicopy.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thomaskuenneth.monicopy.app.About
import com.thomaskuenneth.monicopy.app.AppUiState
import com.thomaskuenneth.monicopy.app.AppViewModel
import com.thomaskuenneth.monicopy.app.OpenSourceLicensesSheet
import com.thomaskuenneth.monicopy.app.SheetVisibility
import com.thomaskuenneth.monicopy.app.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheets(
    appViewModel: AppViewModel,
    appUiState: AppUiState,
) {
    if (appUiState.aboutVisibility == SheetVisibility.Visible) {
        AppBottomSheet(onDismiss = { appViewModel.showAboutSheet(false) }) {
            About(uiState = appUiState, modifier = Modifier.fillMaxWidth())
        }
    }
    if (appUiState.settingsVisibility == SheetVisibility.Visible) {
        AppBottomSheet(onDismiss = { appViewModel.showSettingsSheet(false) }) {
            Settings(
                uiState = appUiState,
                viewModel = appViewModel,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (appUiState.openSourceLicensesVisibility == SheetVisibility.Visible) {
        OpenSourceLicensesSheet(
            onDismiss = { appViewModel.showOpenSourceLicenses(false) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        content()
    }
}
