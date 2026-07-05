package com.thomaskuenneth.monicopy.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thomaskuenneth.monicopy.copy.CopyUiState
import com.thomaskuenneth.monicopy.copy.CopyViewModel

@Composable
fun MoniCopyScreen(
    uiState: CopyUiState,
    viewModel: CopyViewModel,
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
                    .padding(all = 16.dp)
            ) {
                when (uiState.isOperationMode) {
                    true -> InFlightPane(
                        logMessages = uiState.logMessages,
                    )

                    false -> SetupPane(
                        uiState = uiState,
                        viewModel = viewModel,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
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
