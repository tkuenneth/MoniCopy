package com.thomaskuenneth.monicopy.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.*
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thomaskuenneth.monicopy.copy.CopyUiState
import com.thomaskuenneth.monicopy.copy.CopyViewModel
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.add_ignore
import com.thomaskuenneth.monicopy.generated.resources.close
import com.thomaskuenneth.monicopy.generated.resources.copy_all_files_and_folders_inside
import com.thomaskuenneth.monicopy.generated.resources.delete_ignore
import com.thomaskuenneth.monicopy.generated.resources.delete_orphaned_files
import com.thomaskuenneth.monicopy.generated.resources.ignored_directories
import com.thomaskuenneth.monicopy.generated.resources.to
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SetupPane(
    uiState: CopyUiState,
    viewModel: CopyViewModel,
) {
    val navigator = rememberSupportingPaneScaffoldNavigator(
        scaffoldDirective = calculatePaneScaffoldDirective(
            windowAdaptiveInfo = currentWindowAdaptiveInfo(),
            verticalHingePolicy = HingePolicy.AlwaysAvoid,
        ),
    )
    val scope = rememberCoroutineScope()
    val mainPaneHidden = navigator.scaffoldValue[SupportingPaneScaffoldRole.Main] == PaneAdaptedValue.Hidden
    val supportingPaneHidden =
        navigator.scaffoldValue[SupportingPaneScaffoldRole.Supporting] == PaneAdaptedValue.Hidden
    SupportingPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        modifier = Modifier.fillMaxSize(),
        mainPane = {
            AnimatedPane {
                DirectoriesPane(
                    uiState = uiState,
                    viewModel = viewModel,
                    showIgnoredDirectoriesButton = supportingPaneHidden,
                    onShowIgnoredDirectories = {
                        scope.launch {
                            navigator.navigateTo(SupportingPaneScaffoldRole.Supporting)
                        }
                    },
                )
            }
        },
        supportingPane = {
            AnimatedPane {
                IgnoredDirectoriesPane(
                    uiState = uiState,
                    viewModel = viewModel,
                    showCloseButton = mainPaneHidden,
                    onClose = {
                        scope.launch {
                            navigator.navigateBack()
                        }
                    },
                )
            }
        },
    )
}

@Composable
private fun DirectoriesPane(
    uiState: CopyUiState,
    viewModel: CopyViewModel,
    showIgnoredDirectoriesButton: Boolean,
    onShowIgnoredDirectories: () -> Unit,
) {
    val validation = rememberDirectoryValidation(uiState.sourceDir, uiState.destDir)
    val warning = validation.warningMessage()
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(Res.string.copy_all_files_and_folders_inside))
        DirectoryLink(
            path = uiState.sourceDir,
            onClick = viewModel::selectSource,
        )
        Text(stringResource(Res.string.to))
        DirectoryLink(
            path = uiState.destDir,
            onClick = viewModel::selectDest,
        )
        if (warning.isNotEmpty()) {
            Text(warning, color = Color.Red)
        }
        Spacer(Modifier.height(16.dp))
        CheckboxWithLabel(
            label = stringResource(Res.string.delete_orphaned_files),
            checked = uiState.deleteOrphans,
            onCheckedChange = viewModel::onDeleteOrphansChanged,
        )
        if (showIgnoredDirectoriesButton) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onShowIgnoredDirectories) {
                Text(stringResource(Res.string.ignored_directories))
            }
        }
    }
}

@Composable
private fun DirectoryLink(path: String?, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
    ) {
        Text(
            text = path ?: "\u2026",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun IgnoredDirectoriesPane(
    uiState: CopyUiState,
    viewModel: CopyViewModel,
    showCloseButton: Boolean,
    onClose: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(Res.string.ignored_directories))
            if (showCloseButton) {
                TextButton(onClick = onClose) {
                    Text(stringResource(Res.string.close))
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline)),
            ) {
                items(uiState.ignores, key = { it }) { path ->
                    val selected = path in uiState.selectedIgnores
                    Text(
                        text = path,
                        maxLines = 1,
                        overflow = TextOverflow.StartEllipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleIgnoreSelection(path) }
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                },
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
            Column(
                modifier = Modifier.width(120.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = viewModel::addIgnore,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.add_ignore))
                }
                Button(
                    onClick = viewModel::removeSelectedIgnores,
                    enabled = uiState.selectedIgnores.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.delete_ignore))
                }
            }
        }
    }
}
