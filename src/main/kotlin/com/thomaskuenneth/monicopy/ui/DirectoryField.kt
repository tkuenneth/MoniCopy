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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.thomaskuenneth.monicopy.copy.DirectoryRole
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.ic_close
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

internal const val EMPTY_DIRECTORY_PLACEHOLDER = "\u2026"

@Composable
internal fun DirectoryField(
    role: DirectoryRole,
    path: String?,
    onSelect: (DirectoryRole) -> Unit,
    onClear: (DirectoryRole) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    nextFocusRequester: FocusRequester = FocusRequester.Default,
) {
    val clearFocusRequester = remember { FocusRequester() }
    val title = stringResource(role.title)
    OutlinedCard(modifier = modifier) {
        Row(
            modifier = Modifier.padding(UIConstants.EXTRA_SMALL_PADDING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WithPlainTooltip(
                text = title,
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Text(
                    text = path ?: EMPTY_DIRECTORY_PLACEHOLDER,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .focusProperties { next = if (path != null) clearFocusRequester else nextFocusRequester }
                        .clip(MaterialTheme.shapes.small)
                        .clickable(role = Role.Button) { onSelect(role) }
                        .semantics { contentDescription = title }
                        .padding(
                            horizontal = UIConstants.PREFERRED_HORIZONTAL_PADDING,
                            vertical = UIConstants.SMALL_VERTICAL_PADDING,
                        ),
                )
            }
            if (path != null) {
                val clearLabel = stringResource(role.clearLabel)
                WithPlainTooltip(text = clearLabel) {
                    IconButton(
                        onClick = {
                            onClear(role)
                            focusRequester.requestFocus()
                        },
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier
                            .focusRequester(clearFocusRequester)
                            .focusProperties { next = nextFocusRequester },
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_close),
                            contentDescription = clearLabel,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithPlainTooltip(
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(text) } },
        state = rememberTooltipState(),
        modifier = modifier,
        content = content,
    )
}
