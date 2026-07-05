package com.thomaskuenneth.monicopy.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPosition

@Composable
fun FrameWindowScope.getCenteredPosition(): WindowPosition = window.locationOnScreen.let { locationOnScreen ->
    with(LocalDensity.current) {
        window.size.let { size ->
            val width = size.width.toDp()
            val height = size.height.toDp()
            val offsetX = (width - 400.dp) / 2
            val offsetY = (height - 300.dp) / 2
            WindowPosition.Absolute(
                x = locationOnScreen.x.toDp() + offsetX,
                y = locationOnScreen.y.toDp() + offsetY,
            )
        }
    }
}
