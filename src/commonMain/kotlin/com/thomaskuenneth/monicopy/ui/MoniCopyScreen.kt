package com.thomaskuenneth.monicopy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thomaskuenneth.monicopy.NavigationState
import com.thomaskuenneth.monicopy.copy.CopyUiState
import com.thomaskuenneth.monicopy.copy.CopyViewModel

@Composable
fun MoniCopyScreen(
    uiState: CopyUiState,
    viewModel: CopyViewModel,
    navigationState: NavigationState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val validation = rememberDirectoryValidation(uiState.sourceDir, uiState.destDir)
    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(UIConstants.PREFERRED_VERTICAL_PADDING)
            ) {
                when (uiState.isOperationMode) {
                    true -> InFlightPane(
                        logMessages = uiState.logMessages,
                    )

                    false -> SetupPane(
                        uiState = uiState,
                        viewModel = viewModel,
                        navigationState = navigationState,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = UIConstants.PREFERRED_VERTICAL_PADDING),
                horizontalArrangement = Arrangement.Center,
            ) {
                Button(
                    onClick = { viewModel.onActionButtonClick(onClose) },
                    enabled = validation.isActionButtonEnabled(uiState.copyState),
                ) {
                    Text(actionButtonText(uiState.copyState))
                }
            }
        }
    }
}
