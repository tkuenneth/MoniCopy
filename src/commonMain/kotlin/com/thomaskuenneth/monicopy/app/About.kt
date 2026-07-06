package com.thomaskuenneth.monicopy.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import com.thomaskuenneth.monicopy.ui.UIConstants
import com.thomaskuenneth.monicopy.appVersion
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.artwork_no_background
import com.thomaskuenneth.monicopy.generated.resources.title
import com.thomaskuenneth.monicopy.platformName
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun About(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(UIConstants.SMALL_VERTICAL_PADDING),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(Res.drawable.artwork_no_background),
            contentDescription = null,
            modifier = Modifier.size(96.dp),
        )
        Text(
            text = stringResource(Res.string.title),
            modifier = Modifier.padding(top = UIConstants.PREFERRED_VERTICAL_PADDING),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = appVersion,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = platformName,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
