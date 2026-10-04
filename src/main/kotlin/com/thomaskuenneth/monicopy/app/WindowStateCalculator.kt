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
package com.thomaskuenneth.monicopy.app

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition

data class ResolvedWindowState(
    val position: DpOffset?,
    val size: DpSize,
    val placement: WindowPlacement,
    val isMinimized: Boolean,
)

object WindowStateCalculator {
    fun resolve(
        saved: WindowPreferences,
        defaultSize: DpSize,
        screenPosition: DpOffset,
        screenSize: DpSize,
    ): ResolvedWindowState {
        val width = (saved.width?.dp ?: defaultSize.width).coerceIn(0.dp, screenSize.width)
        val height = (saved.height?.dp ?: defaultSize.height).coerceIn(0.dp, screenSize.height)
        val size = DpSize(width, height)
        val placement = WindowPlacement.entries.firstOrNull { it.name == saved.placement }
            ?: WindowPlacement.Floating
        val position = resolvePosition(saved, width, height, screenPosition, screenSize)
        return ResolvedWindowState(
            position = position,
            size = size,
            placement = placement,
            isMinimized = saved.isMinimized,
        )
    }

    private fun resolvePosition(
        saved: WindowPreferences,
        width: Dp,
        height: Dp,
        screenPosition: DpOffset,
        screenSize: DpSize,
    ): DpOffset? {
        val x = saved.x?.dp ?: return null
        val y = saved.y?.dp ?: return null
        val fitsHorizontally = x >= screenPosition.x && x + width <= screenPosition.x + screenSize.width
        val fitsVertically = y >= screenPosition.y && y + height <= screenPosition.y + screenSize.height
        return if (fitsHorizontally && fitsVertically) DpOffset(x, y) else null
    }

    fun captureFloatingBounds(
        placement: WindowPlacement,
        position: WindowPosition,
        size: DpSize,
        current: WindowPreferences,
    ): WindowPreferences {
        if (placement != WindowPlacement.Floating || position !is WindowPosition.Absolute) {
            return current
        }
        return current.copy(
            x = position.x.value,
            y = position.y.value,
            width = size.width.value,
            height = size.height.value,
        )
    }
}
