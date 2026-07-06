package com.thomaskuenneth.monicopy.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thomaskuenneth.monicopy.ui.UIConstants
import com.thomaskuenneth.monicopy.ui.CheckboxWithLabel
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.color_scheme
import com.thomaskuenneth.monicopy.generated.resources.dark
import com.thomaskuenneth.monicopy.generated.resources.light
import com.thomaskuenneth.monicopy.generated.resources.show_extended_about_dialog
import com.thomaskuenneth.monicopy.generated.resources.system
import com.thomaskuenneth.monicopy.shouldShowExtendedAboutDialogCheckbox
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
            horizontal = UIConstants.LARGE_HORIZONTAL_PADDING,
            vertical = UIConstants.LARGE_VERTICAL_PADDING,
        ),
        verticalArrangement = Arrangement.spacedBy(UIConstants.PREFERRED_VERTICAL_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
