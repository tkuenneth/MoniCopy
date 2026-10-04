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

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val screenPosition = DpOffset(0.dp, 0.dp)
private val screenSize = DpSize(1600.dp, 1000.dp)
private val defaultSize = DpSize(840.dp, 480.dp)

val WindowStateCalculatorTests by testSuite {
    test("no saved preferences falls back to the default size, no position, Floating") {
        val resolved = WindowStateCalculator.resolve(
            saved = WindowPreferences(),
            defaultSize = defaultSize,
            screenPosition = screenPosition,
            screenSize = screenSize,
        )

        assertEquals(defaultSize, resolved.size)
        assertNull(resolved.position)
        assertEquals(WindowPlacement.Floating, resolved.placement)
        assertFalse(resolved.isMinimized)
    }

    test("saved isMinimized true is restored") {
        val resolved = WindowStateCalculator.resolve(
            saved = WindowPreferences(isMinimized = true),
            defaultSize = defaultSize,
            screenPosition = screenPosition,
            screenSize = screenSize,
        )

        assertTrue(resolved.isMinimized)
    }

    test("saved isMinimized false is restored") {
        val resolved = WindowStateCalculator.resolve(
            saved = WindowPreferences(isMinimized = false),
            defaultSize = defaultSize,
            screenPosition = screenPosition,
            screenSize = screenSize,
        )

        assertFalse(resolved.isMinimized)
    }

    test("saved size and position within the screen are restored") {
        val saved = WindowPreferences(x = 100f, y = 50f, width = 900f, height = 600f)

        val resolved = WindowStateCalculator.resolve(
            saved = saved,
            defaultSize = defaultSize,
            screenPosition = screenPosition,
            screenSize = screenSize,
        )

        assertEquals(DpSize(900.dp, 600.dp), resolved.size)
        assertEquals(DpOffset(100.dp, 50.dp), resolved.position)
    }

    test("saved Maximized placement is restored") {
        val saved = WindowPreferences(placement = WindowPlacement.Maximized.name)

        val resolved = WindowStateCalculator.resolve(
            saved = saved,
            defaultSize = defaultSize,
            screenPosition = screenPosition,
            screenSize = screenSize,
        )

        assertEquals(WindowPlacement.Maximized, resolved.placement)
    }

    test("saved Fullscreen placement is restored") {
        val saved = WindowPreferences(placement = WindowPlacement.Fullscreen.name)

        val resolved = WindowStateCalculator.resolve(
            saved = saved,
            defaultSize = defaultSize,
            screenPosition = screenPosition,
            screenSize = screenSize,
        )

        assertEquals(WindowPlacement.Fullscreen, resolved.placement)
    }

    test("unrecognized saved placement falls back to Floating") {
        val saved = WindowPreferences(placement = "not-a-real-placement")

        val resolved = WindowStateCalculator.resolve(
            saved = saved,
            defaultSize = defaultSize,
            screenPosition = screenPosition,
            screenSize = screenSize,
        )

        assertEquals(WindowPlacement.Floating, resolved.placement)
    }

    test("saved size larger than the screen is clamped to the screen size") {
        val saved = WindowPreferences(width = 5000f, height = 4000f)

        val resolved = WindowStateCalculator.resolve(
            saved = saved,
            defaultSize = defaultSize,
            screenPosition = screenPosition,
            screenSize = screenSize,
        )

        assertEquals(screenSize, resolved.size)
    }

    test("saved negative size is clamped to zero") {
        val saved = WindowPreferences(width = -100f, height = -50f)

        val resolved = WindowStateCalculator.resolve(
            saved = saved,
            defaultSize = defaultSize,
            screenPosition = screenPosition,
            screenSize = screenSize,
        )

        assertEquals(DpSize(0.dp, 0.dp), resolved.size)
    }

    test("saved position that no longer fits the screen is discarded") {
        val saved = WindowPreferences(x = 1500f, y = 50f, width = 900f, height = 600f)

        val resolved = WindowStateCalculator.resolve(
            saved = saved,
            defaultSize = defaultSize,
            screenPosition = screenPosition,
            screenSize = screenSize,
        )

        assertNull(resolved.position)
        assertEquals(DpSize(900.dp, 600.dp), resolved.size)
    }

    test("saved position left of or above the screen origin is discarded") {
        val saved = WindowPreferences(x = -50f, y = 50f, width = 900f, height = 600f)

        val resolved = WindowStateCalculator.resolve(
            saved = saved,
            defaultSize = defaultSize,
            screenPosition = screenPosition,
            screenSize = screenSize,
        )

        assertNull(resolved.position)
    }

    test("saved position is validated against a non-zero screen origin") {
        val offsetScreenPosition = DpOffset(100.dp, 30.dp)
        val saved = WindowPreferences(x = 50f, y = 10f, width = 900f, height = 600f)

        val resolved = WindowStateCalculator.resolve(
            saved = saved,
            defaultSize = defaultSize,
            screenPosition = offsetScreenPosition,
            screenSize = screenSize,
        )

        assertNull(resolved.position)
    }

    test("only x saved without y does not restore a position") {
        val saved = WindowPreferences(x = 100f, width = 900f, height = 600f)

        val resolved = WindowStateCalculator.resolve(
            saved = saved,
            defaultSize = defaultSize,
            screenPosition = screenPosition,
            screenSize = screenSize,
        )

        assertNull(resolved.position)
    }

    test("floating bounds are captured while the window is Floating") {
        val current = WindowPreferences(x = 0f, y = 0f, width = 840f, height = 480f)

        val captured = WindowStateCalculator.captureFloatingBounds(
            placement = WindowPlacement.Floating,
            position = WindowPosition.Absolute(60.dp, 90.dp),
            size = DpSize(1000.dp, 700.dp),
            current = current,
        )

        assertEquals(WindowPreferences(x = 60f, y = 90f, width = 1000f, height = 700f), captured)
    }

    test("floating bounds are kept unchanged while the window is Maximized") {
        val current = WindowPreferences(x = 60f, y = 90f, width = 1000f, height = 700f)

        val captured = WindowStateCalculator.captureFloatingBounds(
            placement = WindowPlacement.Maximized,
            position = WindowPosition.Absolute(0.dp, 0.dp),
            size = screenSize,
            current = current,
        )

        assertEquals(current, captured)
    }

    test("floating bounds are kept unchanged while the window is Fullscreen") {
        val current = WindowPreferences(x = 60f, y = 90f, width = 1000f, height = 700f)

        val captured = WindowStateCalculator.captureFloatingBounds(
            placement = WindowPlacement.Fullscreen,
            position = WindowPosition.Absolute(0.dp, 0.dp),
            size = screenSize,
            current = current,
        )

        assertEquals(current, captured)
    }

    test("floating bounds are kept unchanged while the position is not yet Absolute") {
        val current = WindowPreferences(x = 60f, y = 90f, width = 1000f, height = 700f)

        val captured = WindowStateCalculator.captureFloatingBounds(
            placement = WindowPlacement.Floating,
            position = WindowPosition.PlatformDefault,
            size = DpSize(1000.dp, 700.dp),
            current = current,
        )

        assertEquals(current, captured)
    }

    test("un-maximizing restores the last captured floating bounds, not the maximized ones") {
        val initial = WindowPreferences(x = 60f, y = 90f, width = 1000f, height = 700f)

        val afterMaximizing = WindowStateCalculator.captureFloatingBounds(
            placement = WindowPlacement.Maximized,
            position = WindowPosition.Absolute(0.dp, 0.dp),
            size = screenSize,
            current = initial,
        )
        val afterUnmaximizing = WindowStateCalculator.captureFloatingBounds(
            placement = WindowPlacement.Floating,
            position = WindowPosition.Absolute(60.dp, 90.dp),
            size = DpSize(1000.dp, 700.dp),
            current = afterMaximizing,
        )

        assertEquals(initial, afterMaximizing)
        assertEquals(initial, afterUnmaximizing)
    }
}
