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

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import com.thomaskuenneth.monicopy.NavigationState
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.about
import com.thomaskuenneth.monicopy.generated.resources.back
import com.thomaskuenneth.monicopy.generated.resources.file
import com.thomaskuenneth.monicopy.generated.resources.help
import com.thomaskuenneth.monicopy.generated.resources.open_source_licenses
import com.thomaskuenneth.monicopy.generated.resources.quit
import com.thomaskuenneth.monicopy.generated.resources.settings
import com.thomaskuenneth.monicopy.generated.resources.view
import com.thomaskuenneth.monicopy.platform.OperatingSystem
import org.jetbrains.compose.resources.stringResource

@Composable
fun FrameWindowScope.MoniCopyMenuBar(
    operatingSystem: OperatingSystem,
    navigationState: NavigationState,
    exit: () -> Unit,
    showAbout: () -> Unit,
    showOpenSourceLicenses: () -> Unit,
    showSettings: () -> Unit,
) {
    MenuBar {
        if (operatingSystem != OperatingSystem.MacOS) {
            Menu(text = stringResource(Res.string.file)) {
                Item(text = stringResource(Res.string.settings), onClick = showSettings)
                Item(
                    text = stringResource(Res.string.quit),
                    onClick = exit,
                    shortcut = KeyShortcut(Key.F4, alt = true),
                )
            }
        }
        Menu(text = stringResource(Res.string.view)) {
            Item(
                enabled = navigationState.canNavigateBack,
                shortcut = KeyShortcut(Key.Escape),
                text = stringResource(Res.string.back),
                onClick = navigationState.navigateBack,
            )
        }
        Menu(text = stringResource(Res.string.help)) {
            if (operatingSystem != OperatingSystem.MacOS) {
                Item(text = stringResource(Res.string.about), onClick = showAbout)
            }
            Item(
                text = stringResource(Res.string.open_source_licenses),
                onClick = showOpenSourceLicenses,
            )
        }
    }
}
