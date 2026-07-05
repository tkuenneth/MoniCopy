package com.thomaskuenneth.monicopy.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thomaskuenneth.monicopy.copy.CopyViewModel
import com.thomaskuenneth.monicopy.app.AppViewModel
import com.thomaskuenneth.monicopy.app.colorScheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MoniCopyApp(
    onClose: () -> Unit,
    platformContent: @Composable (AppViewModel) -> Unit = {},
) {
    val appViewModel: AppViewModel = koinViewModel()
    val copyViewModel: CopyViewModel = koinViewModel()
    val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()
    val copyUiState by copyViewModel.uiState.collectAsStateWithLifecycle()

    MaterialTheme(colorScheme = colorScheme(appUiState.colorSchemeMode)) {
        MoniCopyScreen(
            uiState = copyUiState,
            viewModel = copyViewModel,
            onClose = onClose,
        )
        platformContent(appViewModel)
    }
}
