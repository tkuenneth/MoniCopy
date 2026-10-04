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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import java.io.File

internal fun directoryChipLabel(path: String): String = File(path).name.ifEmpty { path }

@Composable
internal fun RecentDirectoryChips(
    label: String,
    paths: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(UIConstants.SMALL_HORIZONTAL_PADDING, Alignment.CenterHorizontally),
    ) {
        paths.forEach { path ->
            SuggestionChip(
                onClick = { onSelect(path) },
                label = {
                    Text(
                        text = directoryChipLabel(path),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                modifier = Modifier.semantics { contentDescription = path },
            )
        }
    }
}
