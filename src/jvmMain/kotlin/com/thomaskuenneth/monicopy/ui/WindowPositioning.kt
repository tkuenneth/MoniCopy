package com.thomaskuenneth.monicopy.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState

@Composable
fun FrameWindowScope.rememberCenteredWindowState(
    width: Dp = 400.dp,
    height: Dp = 300.dp,
): WindowState = rememberWindowState(
    width = width,
    height = height,
    position = getCenteredPosition(width, height),
)

@Composable
fun FrameWindowScope.getCenteredPosition(
    width: Dp = 400.dp,
    height: Dp = 300.dp,
): WindowPosition = window.locationOnScreen.let { locationOnScreen ->
    with(LocalDensity.current) {
        window.size.let { size ->
            val parentWidth = size.width.toDp()
            val parentHeight = size.height.toDp()
            val offsetX = (parentWidth - width) / 2
            val offsetY = (parentHeight - height) / 2
            WindowPosition.Absolute(
                x = locationOnScreen.x.toDp() + offsetX,
                y = locationOnScreen.y.toDp() + offsetY,
            )
        }
    }
}
