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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thomaskuenneth.monicopy.copy.CopyUiState
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.copy_progress
import com.thomaskuenneth.monicopy.generated.resources.copying_complete
import com.thomaskuenneth.monicopy.generated.resources.deleting_complete
import com.thomaskuenneth.monicopy.generated.resources.find_files
import com.thomaskuenneth.monicopy.generated.resources.find_orphans
import com.thomaskuenneth.monicopy.generated.resources.finding_files_paused_a11y
import com.thomaskuenneth.monicopy.generated.resources.finding_orphans_paused_a11y
import com.thomaskuenneth.monicopy.generated.resources.number_of_files_and_directories
import com.thomaskuenneth.monicopy.generated.resources.orphan_progress
import com.thomaskuenneth.monicopy.generated.resources.progress_a11y
import com.thomaskuenneth.monicopy.generated.resources.progress_paused_a11y
import com.thomaskuenneth.monicopy.generated.resources.progress_percent
import org.jetbrains.compose.resources.stringResource

private val StatusColumnMaxWidth = 360.dp

private val ThickWavyStrokeWidth = 8.dp
private val ThickWavyHeight = 14.dp

private sealed interface DiscoveryStatus {
    data object Finding : DiscoveryStatus
    data class Counts(val fileCount: Long, val subfolderCount: Long) : DiscoveryStatus
}

private sealed interface CopyPhaseStatus {
    data class InProgress(val percent: Int, val paused: Boolean) : CopyPhaseStatus
    data object Complete : CopyPhaseStatus
}

private sealed interface OrphanPhaseStatus {
    data object Finding : OrphanPhaseStatus
    data class InProgress(val percent: Int, val paused: Boolean) : OrphanPhaseStatus
    data object Complete : OrphanPhaseStatus
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InFlightPane(uiState: CopyUiState) {
    val listState = rememberLazyListState()
    LaunchedEffect(uiState.logMessages.size) {
        if (uiState.logMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.logMessages.lastIndex)
        }
    }
    val panePadding = PaddingValues(
        horizontal = UIConstants.PREFERRED_HORIZONTAL_PADDING,
        vertical = UIConstants.PREFERRED_VERTICAL_PADDING,
    )
    val discoveryStatus = when {
        uiState.isFindingFiles -> DiscoveryStatus.Finding
        uiState.fileCount != null && uiState.subfolderCount != null ->
            DiscoveryStatus.Counts(uiState.fileCount, uiState.subfolderCount)
        else -> null
    }
    val copyPhaseStatus = when {
        uiState.copyProgressPercent == null -> null
        uiState.copyPhaseComplete -> CopyPhaseStatus.Complete
        else -> CopyPhaseStatus.InProgress(
            percent = uiState.copyProgressPercent,
            paused = uiState.isPaused,
        )
    }
    val orphanPhaseStatus = when {
        uiState.isFindingOrphans -> OrphanPhaseStatus.Finding
        uiState.orphanPhaseComplete -> OrphanPhaseStatus.Complete
        uiState.orphanProgressPercent == null -> null
        else -> OrphanPhaseStatus.InProgress(
            percent = uiState.orphanProgressPercent,
            paused = uiState.isPaused,
        )
    }
    Row(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxSize(),
            contentPadding = panePadding,
            verticalArrangement = Arrangement.spacedBy(
                UIConstants.PREFERRED_VERTICAL_PADDING,
                Alignment.CenterVertically,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (discoveryStatus != null) {
                item(key = "discovery") {
                    AnimatedContent(
                        targetState = discoveryStatus,
                        transitionSpec = { MoniCopyAnimations.fadeTransition() },
                        modifier = Modifier.animateItem(),
                        label = "discoveryStatus",
                    ) { status ->
                        when (status) {
                            DiscoveryStatus.Finding -> {
                                val label = stringResource(Res.string.find_files)
                                ScanningStatus(
                                    label = label,
                                    paused = uiState.isPaused,
                                    contentDescription = if (uiState.isPaused) {
                                        stringResource(Res.string.finding_files_paused_a11y)
                                    } else {
                                        label
                                    },
                                )
                            }
                            is DiscoveryStatus.Counts -> {
                                Text(
                                    text = stringResource(
                                        Res.string.number_of_files_and_directories,
                                        status.fileCount,
                                        status.subfolderCount,
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
            if (copyPhaseStatus != null) {
                item(key = "copyPhase") {
                    AnimatedContent(
                        targetState = copyPhaseStatus,
                        contentKey = { status ->
                            when (status) {
                                is CopyPhaseStatus.InProgress -> "inProgress"
                                CopyPhaseStatus.Complete -> "complete"
                            }
                        },
                        transitionSpec = { MoniCopyAnimations.fadeTransition() },
                        modifier = Modifier.animateItem(),
                        label = "copyPhaseStatus",
                    ) { status ->
                        when (status) {
                            is CopyPhaseStatus.InProgress -> {
                                StatusProgress(
                                    label = stringResource(Res.string.copy_progress),
                                    percent = status.percent,
                                    paused = status.paused,
                                )
                            }
                            CopyPhaseStatus.Complete -> {
                                PhaseCompleteStatus(
                                    text = stringResource(
                                        Res.string.copying_complete,
                                        uiState.filesCopied,
                                        uiState.filesSkipped,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
            if (orphanPhaseStatus != null) {
                item(key = "orphanPhase") {
                    AnimatedContent(
                        targetState = orphanPhaseStatus,
                        contentKey = { status ->
                            when (status) {
                                OrphanPhaseStatus.Finding -> "finding"
                                is OrphanPhaseStatus.InProgress -> "inProgress"
                                OrphanPhaseStatus.Complete -> "complete"
                            }
                        },
                        transitionSpec = { MoniCopyAnimations.fadeTransition() },
                        modifier = Modifier.animateItem(),
                        label = "orphanPhaseStatus",
                    ) { status ->
                        when (status) {
                            OrphanPhaseStatus.Finding -> {
                                val label = stringResource(Res.string.find_orphans)
                                ScanningStatus(
                                    label = label,
                                    paused = uiState.isPaused,
                                    contentDescription = if (uiState.isPaused) {
                                        stringResource(Res.string.finding_orphans_paused_a11y)
                                    } else {
                                        label
                                    },
                                )
                            }
                            is OrphanPhaseStatus.InProgress -> {
                                StatusProgress(
                                    label = stringResource(Res.string.orphan_progress),
                                    percent = status.percent,
                                    paused = status.paused,
                                )
                            }
                            OrphanPhaseStatus.Complete -> {
                                PhaseCompleteStatus(
                                    text = stringResource(Res.string.deleting_complete),
                                )
                            }
                        }
                    }
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(0.5f)
                .fillMaxSize()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentPadding = panePadding,
        ) {
            itemsIndexed(
                uiState.logMessages,
                key = { index, _ -> index },
            ) { _, line ->
                Text(
                    text = line,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun PhaseCompleteStatus(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = text },
    )
}

@Composable
private fun ScanningStatus(
    label: String,
    paused: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    StatusColumn(
        modifier = modifier.semantics(mergeDescendants = true) {
            this.contentDescription = contentDescription
        },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (paused) {
            CircularProgressIndicator(progress = { 0f })
        } else {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatusProgress(
    label: String,
    percent: Int,
    paused: Boolean,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (percent / 100f).coerceIn(0f, 1f),
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "statusProgress",
    )
    val description = if (paused) {
        stringResource(Res.string.progress_paused_a11y, label, percent)
    } else {
        stringResource(Res.string.progress_a11y, label, percent)
    }
    StatusColumn(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = description
        },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        ThickWavyProgressIndicator(
            progress = { animatedProgress },
            paused = paused,
        )
        Text(
            text = stringResource(Res.string.progress_percent, percent),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatusColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .widthIn(max = StatusColumnMaxWidth)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UIConstants.SMALL_VERTICAL_PADDING),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThickWavyProgressIndicator(
    progress: () -> Float,
    paused: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val strokeWidthPx = with(LocalDensity.current) { ThickWavyStrokeWidth.toPx() }
    val stroke = remember(strokeWidthPx) {
        Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
    }
    LinearWavyProgressIndicator(
        progress = progress,
        modifier = modifier
            .fillMaxWidth()
            .height(ThickWavyHeight),
        stroke = stroke,
        trackStroke = stroke,
        amplitude = if (paused) {
            { 0f }
        } else {
            WavyProgressIndicatorDefaults.indicatorAmplitude
        },
    )
}
