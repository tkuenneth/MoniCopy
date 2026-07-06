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
import com.thomaskuenneth.monicopy.app.SheetVisibility
import com.thomaskuenneth.monicopy.app.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheets(
    appViewModel: AppViewModel,
    appUiState: AppUiState,
) {
    if (appUiState.aboutVisibility == SheetVisibility.Visible) {
        AppBottomSheet(onDismiss = { appViewModel.setShouldShowAbout(false) }) {
            About(modifier = Modifier.fillMaxWidth())
        }
    }
    if (appUiState.settingsVisibility == SheetVisibility.Visible) {
        AppBottomSheet(onDismiss = { appViewModel.setShouldShowSettings(false) }) {
            Settings(viewModel = appViewModel, modifier = Modifier.fillMaxWidth())
        }
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
