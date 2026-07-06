package com.thomaskuenneth.monicopy.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.thomaskuenneth.monicopy.DirectoryValidationResult
import com.thomaskuenneth.monicopy.NavigationState
import com.thomaskuenneth.monicopy.copy.CopyState
import com.thomaskuenneth.monicopy.copy.CopyUiState
import com.thomaskuenneth.monicopy.copy.CopyViewModel
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.cancel
import org.jetbrains.compose.resources.stringResource

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
                Crossfade(
                    targetState = uiState.isOperationMode,
                    modifier = Modifier.fillMaxSize(),
                    animationSpec = MoniCopyAnimations.crossfadeSpec,
                    label = "mainPane",
                ) { isOperationMode ->
                    when (isOperationMode) {
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
            }
            AnimatedContent(
                targetState = uiState.copyState.toActionBarState(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = UIConstants.PREFERRED_VERTICAL_PADDING)
                    .animateContentSize(),
                transitionSpec = { MoniCopyAnimations.fadeTransition() },
                label = "actionBar",
            ) { actionBarState ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        UIConstants.PREFERRED_HORIZONTAL_PADDING,
                        Alignment.CenterHorizontally,
                    ),
                ) {
                    when (actionBarState) {
                        ActionBarState.Setup -> {
                            PrimaryActionButton(
                                copyState = CopyState.IDLE,
                                validation = validation,
                                onClick = { viewModel.onActionButtonClick(onClose) },
                            )
                        }

                        ActionBarState.InProgress -> {
                            OutlinedButton(onClick = viewModel::cancelOperation) {
                                Text(stringResource(Res.string.cancel))
                            }
                            PrimaryActionButton(
                                copyState = uiState.copyState.toInProgressCopyState(),
                                validation = validation,
                                onClick = { viewModel.onActionButtonClick(onClose) },
                            )
                        }

                        ActionBarState.Finished -> {
                            PrimaryActionButton(
                                copyState = CopyState.FINISHED,
                                validation = validation,
                                onClick = { viewModel.onActionButtonClick(onClose) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryActionButton(
    copyState: CopyState,
    validation: DirectoryValidationResult,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = validation.isActionButtonEnabled(copyState),
    ) {
        AnimatedContent(
            targetState = copyState.toActionButtonLabel(),
            transitionSpec = { MoniCopyAnimations.fadeTransition() },
            label = "primaryActionLabel",
        ) { label ->
            Text(actionButtonText(label))
        }
    }
}
