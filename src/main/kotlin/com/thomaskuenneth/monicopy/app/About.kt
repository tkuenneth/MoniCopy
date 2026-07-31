package com.thomaskuenneth.monicopy.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.artwork_no_background
import com.thomaskuenneth.monicopy.generated.resources.title
import com.thomaskuenneth.monicopy.app.AppUiState
import com.thomaskuenneth.monicopy.ui.UIConstants.PREFERRED_HORIZONTAL_PADDING
import com.thomaskuenneth.monicopy.ui.UIConstants.PREFERRED_VERTICAL_PADDING
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun About(
    uiState: AppUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(
            horizontal = PREFERRED_HORIZONTAL_PADDING,
            vertical = PREFERRED_VERTICAL_PADDING
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(Res.drawable.artwork_no_background),
            contentDescription = null,
            modifier = Modifier.size(96.dp),
        )
        Text(
            text = stringResource(Res.string.title),
            modifier = Modifier.padding(top = PREFERRED_VERTICAL_PADDING),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = uiState.appVersion,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = uiState.platformName,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
