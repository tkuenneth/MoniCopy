package com.thomaskuenneth.monicopy.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thomaskuenneth.monicopy.NavigationState
import com.thomaskuenneth.monicopy.app.AppViewModel
import com.thomaskuenneth.monicopy.app.colorScheme
import com.thomaskuenneth.monicopy.copy.CopyViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MoniCopyApp(
    appViewModel: AppViewModel = koinViewModel(),
    platformContent: @Composable (AppViewModel, NavigationState) -> Unit = { _, _ -> },
) {
    val copyViewModel: CopyViewModel = koinViewModel()
    val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()
    val copyUiState by copyViewModel.uiState.collectAsStateWithLifecycle()
    val navigationState = remember { NavigationState() }

    MaterialTheme(colorScheme = colorScheme(appUiState.colorSchemeMode)) {
        MoniCopyScreen(
            uiState = copyUiState,
            viewModel = copyViewModel,
            navigationState = navigationState,
        )
        AppBottomSheets(
            appViewModel = appViewModel,
            appUiState = appUiState,
        )
        platformContent(appViewModel, navigationState)
    }
}
