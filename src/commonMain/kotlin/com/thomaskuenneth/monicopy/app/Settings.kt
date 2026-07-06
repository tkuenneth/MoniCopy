package com.thomaskuenneth.monicopy.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thomaskuenneth.monicopy.generated.resources.*
import com.thomaskuenneth.monicopy.shouldShowExtendedAboutDialogCheckbox
import com.thomaskuenneth.monicopy.ui.CheckboxWithLabel
import com.thomaskuenneth.monicopy.ui.UIConstants.PREFERRED_HORIZONTAL_PADDING
import com.thomaskuenneth.monicopy.ui.UIConstants.PREFERRED_VERTICAL_PADDING
import org.jetbrains.compose.resources.stringResource

@Composable
fun Settings(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onClick: (ColorSchemeMode) -> Unit = { viewModel.setColorSchemeMode(it) }
    Column(
        modifier = modifier.padding(
            horizontal = PREFERRED_HORIZONTAL_PADDING,
            vertical = PREFERRED_VERTICAL_PADDING
        ),
        verticalArrangement = Arrangement.spacedBy(PREFERRED_VERTICAL_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.color_scheme),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
        )
        SingleChoiceSegmentedButtonRow {
            SegmentedColorSchemeButton(
                selected = uiState.colorSchemeMode == ColorSchemeMode.System,
                mode = ColorSchemeMode.System,
                onClick = onClick,
            )
            SegmentedColorSchemeButton(
                selected = uiState.colorSchemeMode == ColorSchemeMode.Light,
                mode = ColorSchemeMode.Light,
                onClick = onClick,
            )
            SegmentedColorSchemeButton(
                selected = uiState.colorSchemeMode == ColorSchemeMode.Dark,
                mode = ColorSchemeMode.Dark,
                onClick = onClick,
            )
        }
        if (shouldShowExtendedAboutDialogCheckbox()) {
            CheckboxWithLabel(
                label = stringResource(Res.string.show_extended_about_dialog),
                checked = uiState.showExtendedAboutDialog,
            ) { checked -> viewModel.setShowExtendedAboutDialog(checked) }
        }
    }
}

@Composable
private fun SingleChoiceSegmentedButtonRowScope.SegmentedColorSchemeButton(
    selected: Boolean,
    mode: ColorSchemeMode,
    onClick: (ColorSchemeMode) -> Unit,
) {
    SegmentedButton(
        selected = selected,
        onClick = { onClick(mode) },
        shape = SegmentedButtonDefaults.itemShape(
            index = mode.ordinal,
            count = ColorSchemeMode.entries.size,
        ),
        label = {
            Text(
                text = stringResource(
                    when (mode) {
                        ColorSchemeMode.System -> Res.string.system
                        ColorSchemeMode.Light -> Res.string.light
                        ColorSchemeMode.Dark -> Res.string.dark
                    },
                ),
            )
        },
    )
}
