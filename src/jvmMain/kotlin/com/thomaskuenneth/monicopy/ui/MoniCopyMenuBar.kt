package com.thomaskuenneth.monicopy.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import com.thomaskuenneth.monicopy.NavigationState
import com.thomaskuenneth.monicopy.OperatingSystem
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.about
import com.thomaskuenneth.monicopy.generated.resources.back
import com.thomaskuenneth.monicopy.generated.resources.file
import com.thomaskuenneth.monicopy.generated.resources.help
import com.thomaskuenneth.monicopy.generated.resources.quit
import com.thomaskuenneth.monicopy.generated.resources.settings
import com.thomaskuenneth.monicopy.generated.resources.view
import com.thomaskuenneth.monicopy.operatingSystem
import org.jetbrains.compose.resources.stringResource

@Composable
fun FrameWindowScope.MoniCopyMenuBar(
    navigationState: NavigationState,
    exit: () -> Unit,
    showAbout: () -> Unit,
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
        if (operatingSystem != OperatingSystem.MacOS) {
            Menu(text = stringResource(Res.string.help)) {
                Item(text = stringResource(Res.string.about), onClick = showAbout)
            }
        }
    }
}
