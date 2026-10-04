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
package com.thomaskuenneth.monicopy

import androidx.window.core.layout.WindowSizeClass
import de.infix.testBalloon.framework.core.testSuite
import java.awt.Dimension
import java.awt.Insets
import kotlin.test.assertEquals

val MinimumWindowSizeTests by testSuite {
    test("without window decorations the minimum is exactly where compact width and height end") {
        assertEquals(
            Dimension(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND, WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND),
            Insets(0, 0, 0, 0).minimumWindowSize(),
        )
    }

    test("window decorations on every side are added so the content area keeps the minimum") {
        val decorations = Insets(28, 1, 2, 3)

        assertEquals(
            Dimension(
                WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND + 1 + 3,
                WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND + 28 + 2,
            ),
            decorations.minimumWindowSize(),
        )
    }
}
