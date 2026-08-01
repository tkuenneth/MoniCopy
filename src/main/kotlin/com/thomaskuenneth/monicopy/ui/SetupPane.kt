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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.HingePolicy
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thomaskuenneth.monicopy.NavigationState
import com.thomaskuenneth.monicopy.copy.CopyUiState
import com.thomaskuenneth.monicopy.copy.CopyViewModel
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.add_ignore
import com.thomaskuenneth.monicopy.generated.resources.back
import com.thomaskuenneth.monicopy.generated.resources.copy_all_files_and_folders_inside
import com.thomaskuenneth.monicopy.generated.resources.delete_ignore
import com.thomaskuenneth.monicopy.generated.resources.delete_orphaned_files
import com.thomaskuenneth.monicopy.generated.resources.ic_arrow_back
import com.thomaskuenneth.monicopy.generated.resources.ignored_directories
import com.thomaskuenneth.monicopy.generated.resources.to
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import java.io.File

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SetupPane(
    uiState: CopyUiState,
    viewModel: CopyViewModel,
    navigationState: NavigationState,
) {
    val navigator = rememberSupportingPaneScaffoldNavigator(
        scaffoldDirective = calculatePaneScaffoldDirective(
            windowAdaptiveInfo = currentWindowAdaptiveInfoV2(),
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
                    navigationState = navigationState,
                    showBackButton = mainPaneHidden,
                )
            }
        },
    )
    NavigationHelper(
        navigator = navigator,
        navigationState = navigationState,
        coroutineScope = scope,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DirectoriesPane(
    uiState: CopyUiState,
    viewModel: CopyViewModel,
    showIgnoredDirectoriesButton: Boolean,
    onShowIgnoredDirectories: () -> Unit,
) {
    val validation = rememberDirectoryValidation(uiState.sourceDir, uiState.destDir)
    val warning = validation.warningMessage()
    val (source, destination, deleteOrphans, ignoredDirectories) =
        remember { FocusRequester.createRefs() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .focusProperties { enter = { source } }
            .focusGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UIConstants.SMALL_VERTICAL_PADDING, Alignment.CenterVertically),
    ) {
        Text(stringResource(Res.string.copy_all_files_and_folders_inside))
        DirectoryLink(
            path = uiState.sourceDir,
            onClick = viewModel::selectSource,
            modifier = Modifier
                .focusRequester(source)
                .focusProperties { next = destination },
        )
        Text(stringResource(Res.string.to))
        DirectoryLink(
            path = uiState.destDir,
            onClick = viewModel::selectDest,
            modifier = Modifier
                .focusRequester(destination)
                .focusProperties { next = deleteOrphans },
        )
        if (warning.isNotEmpty()) {
            Text(warning, color = Color.Red)
        }
        Spacer(Modifier.height(UIConstants.PREFERRED_VERTICAL_PADDING))
        CheckboxWithLabel(
            label = stringResource(Res.string.delete_orphaned_files),
            checked = uiState.deleteOrphans,
            onCheckedChange = viewModel::onDeleteOrphansChanged,
            modifier = Modifier
                .focusRequester(deleteOrphans)
                .focusProperties {
                    next = if (showIgnoredDirectoriesButton) {
                        ignoredDirectories
                    } else {
                        FocusRequester.Default
                    }
                },
        )
        if (showIgnoredDirectoriesButton) {
            Spacer(Modifier.height(UIConstants.PREFERRED_VERTICAL_PADDING))
            TextButton(
                onClick = onShowIgnoredDirectories,
                modifier = Modifier.focusRequester(ignoredDirectories),
            ) {
                Text(stringResource(Res.string.ignored_directories))
            }
        }
    }
}

@Composable
private fun DirectoryLink(
    path: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
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
    navigationState: NavigationState,
    showBackButton: Boolean,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = UIConstants.PREFERRED_VERTICAL_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(UIConstants.PREFERRED_HORIZONTAL_PADDING),
        ) {
            if (showBackButton && navigationState.canNavigateBack) {
                OutlinedIconButton(
                    onClick = navigationState.navigateBack,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    colors = IconButtonDefaults.outlinedIconButtonColors(),
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_arrow_back),
                        contentDescription = stringResource(Res.string.back),
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                }
            }
            Text(stringResource(Res.string.ignored_directories))
        }
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(UIConstants.PREFERRED_HORIZONTAL_PADDING),
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(CardDefaults.outlinedCardBorder(), MaterialTheme.shapes.medium),
            ) {
                items(uiState.ignores, key = { it.absolutePath }) { directory ->
                    IgnoredDirectoryListItem(
                        directory = directory,
                        selected = directory in uiState.selectedIgnores,
                        onClick = { viewModel.toggleIgnoreSelection(directory) },
                    )
                }
            }
            Column(
                modifier = Modifier.width(120.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(UIConstants.SMALL_VERTICAL_PADDING),
            ) {
                OutlinedButton(
                    onClick = viewModel::addIgnore,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.add_ignore))
                }
                OutlinedButton(
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

@Composable
private fun IgnoredDirectoryListItem(
    directory: File,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val parentPath = directory.parent
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val pathColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                },
            )
            .padding(
                horizontal = UIConstants.SMALL_HORIZONTAL_PADDING,
                vertical = UIConstants.SMALL_VERTICAL_PADDING,
            ),
    ) {
        Text(
            text = directory.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = contentColor,
        )
        if (parentPath != null) {
            Text(
                text = parentPath,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = pathColor,
            )
        }
    }
}
