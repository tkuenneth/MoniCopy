package com.thomaskuenneth.monicopy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.rememberDialogState
import com.thomaskuenneth.monicopy.app.AppViewModel
import com.thomaskuenneth.monicopy.app.Settings
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.app_icon
import com.thomaskuenneth.monicopy.generated.resources.settings_short
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun FrameWindowScope.SettingsWindow(
    visible: Boolean,
    viewModel: AppViewModel,
    onCloseRequest: () -> Unit,
) {
    if (visible) {
        DialogWindow(
            state = rememberDialogState(position = getCenteredPosition()),
            onCloseRequest = onCloseRequest,
            icon = painterResource(Res.drawable.app_icon),
            resizable = false,
            title = stringResource(Res.string.settings_short),
        ) {
            Settings(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
            )
        }
    }
}
