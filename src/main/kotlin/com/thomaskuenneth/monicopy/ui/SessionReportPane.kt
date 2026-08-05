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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Badge
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.thomaskuenneth.monicopy.copy.CopySessionReason
import com.thomaskuenneth.monicopy.copy.CopySessionReport
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.session_issues
import com.thomaskuenneth.monicopy.generated.resources.session_reason_could_not_copy
import com.thomaskuenneth.monicopy.generated.resources.session_reason_could_not_delete
import com.thomaskuenneth.monicopy.generated.resources.session_reason_could_not_set_last_modified
import com.thomaskuenneth.monicopy.generated.resources.session_reason_interrupted
import com.thomaskuenneth.monicopy.generated.resources.session_reason_not_a_directory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.jetbrains.compose.resources.stringResource

@Composable
fun SessionReportPane(
    report: CopySessionReport,
    modifier: Modifier = Modifier,
) {
    val listItems = remember(report) {
        buildList {
            for (reason in CopySessionReason.entries) {
                val byTime = report.entries[reason].orEmpty()
                if (byTime.isEmpty()) continue
                val events = byTime.entries
                    .sortedBy { it.key }
                    .map { (time, message) -> SessionReportEvent(time, message) }
                add(SessionReportListItem.Header(reason, events.size))
                events.forEach { event ->
                    add(SessionReportListItem.Event(reason, event))
                }
            }
        }
    }
    val listState = rememberLazyListState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val itemFade = MoniCopyAnimations.rememberCrossfadeSpec()
    val itemPlacement = MoniCopyAnimations.rememberPlacementSpec()
    val lastItemKey = listItems.lastOrNull()?.stableKey
    LaunchedEffect(lastItemKey) {
        if (lastItemKey != null) {
            bringIntoViewRequester.bringIntoView()
        }
    }
    val timeFormatter = remember {
        DateTimeFormatter
            .ofLocalizedTime(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault())
    }
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.session_issues),
            modifier = Modifier
                .padding(bottom = UIConstants.PREFERRED_VERTICAL_PADDING)
                .semantics { heading() },
        )
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(CardDefaults.outlinedCardBorder(), MaterialTheme.shapes.medium),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.medium,
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(UIConstants.PREFERRED_HORIZONTAL_PADDING),
                verticalArrangement = Arrangement.spacedBy(UIConstants.SMALL_VERTICAL_PADDING),
            ) {
                itemsIndexed(
                    items = listItems,
                    key = { _, item -> item.stableKey },
                ) { index, item ->
                    val itemModifier = Modifier
                        .animateItem(
                            fadeInSpec = itemFade,
                            fadeOutSpec = itemFade,
                            placementSpec = itemPlacement,
                        )
                        .then(
                            if (index == listItems.lastIndex) {
                                Modifier.bringIntoViewRequester(bringIntoViewRequester)
                            } else {
                                Modifier
                            },
                        )
                    when (item) {
                        is SessionReportListItem.Header -> {
                            SessionReasonHeader(
                                reason = item.reason,
                                count = item.count,
                                modifier = itemModifier,
                            )
                        }
                        is SessionReportListItem.Event -> {
                            SessionEventRow(
                                event = item.event,
                                timeFormatter = timeFormatter,
                                modifier = itemModifier,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionReasonHeader(
    reason: CopySessionReason,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val label = reason.label()
    val description = "$label, $count"
    val countFade = MoniCopyAnimations.rememberFadeTransition()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = description
            },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.alignByBaseline(),
        )
        AnimatedContent(
            targetState = count,
            transitionSpec = { countFade },
            modifier = Modifier.alignByBaseline(),
            label = "sessionReasonCount",
        ) { value ->
            Badge(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ) {
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun SessionEventRow(
    event: SessionReportEvent,
    timeFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier,
) {
    val time = timeFormatter.format(event.timestamp)
    val message = event.message.substringBefore('\n')
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "$time, $message"
            },
        horizontalArrangement = Arrangement.spacedBy(UIConstants.SMALL_HORIZONTAL_PADDING),
    ) {
        Text(
            text = time,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alignByBaseline(),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
            maxLines = 3,
            modifier = Modifier
                .weight(1f)
                .alignByBaseline(),
        )
    }
}

@Composable
private fun CopySessionReason.label(): String =
    stringResource(
        when (this) {
            CopySessionReason.CouldNotCopy -> Res.string.session_reason_could_not_copy
            CopySessionReason.CouldNotDelete -> Res.string.session_reason_could_not_delete
            CopySessionReason.NotADirectory -> Res.string.session_reason_not_a_directory
            CopySessionReason.Interrupted -> Res.string.session_reason_interrupted
            CopySessionReason.CouldNotSetLastModified -> Res.string.session_reason_could_not_set_last_modified
        },
    )

private sealed interface SessionReportListItem {
    val stableKey: String

    data class Header(val reason: CopySessionReason, val count: Int) : SessionReportListItem {
        override val stableKey: String
            get() = "header-$reason"
    }

    data class Event(val reason: CopySessionReason, val event: SessionReportEvent) : SessionReportListItem {
        override val stableKey: String
            get() = "event-$reason-${event.timestamp}"
    }
}

private data class SessionReportEvent(
    val timestamp: Instant,
    val message: String,
)
