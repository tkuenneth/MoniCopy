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
@file:OptIn(ExperimentalTestApi::class)

package com.thomaskuenneth.monicopy.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val BACKUP_PATH = "/Volumes/Backup"
private const val PICTURES_PATH = "/Users/thomas/Pictures"
private const val LONG_NAME_PATH = "/Volumes/A folder name that is much too long for a narrow chip row"
private const val LABEL = "Recent folders"
private val NARROW_ROW_WIDTH = 120.dp

val RecentDirectoryChipsTests by testSuite(compartment = { TestCompartment.RealTime }) {
    testSuite("label") {
        val labelCases = mapOf(
            "shows only the folder name" to (PICTURES_PATH to "Pictures"),
            "ignores a trailing separator" to ("$BACKUP_PATH/" to "Backup"),
            "shows the path of the root folder, which has no name" to ("/" to "/"),
        )
        for ((name, case) in labelCases) {
            val (path, expected) = case
            test(name) {
                assertEquals(expected, directoryChipLabel(path))
            }
        }
    }

    testSuite("chips") {
        test("shows one chip per directory with its folder name") {
            runComposeUiTest {
                setContent { RecentDirectoryChips(label = LABEL, paths = listOf(BACKUP_PATH, PICTURES_PATH), onSelect = {}) }

                onNodeWithText("Backup").assertIsDisplayed()
                onNodeWithText("Pictures").assertIsDisplayed()
            }
        }

        test("clicking a chip selects its full path") {
            val selected = mutableListOf<String>()
            runComposeUiTest {
                setContent { RecentDirectoryChips(label = LABEL, paths = listOf(BACKUP_PATH, PICTURES_PATH), onSelect = { selected += it }) }

                onNodeWithText("Pictures").performClick()
            }
            assertEquals(listOf(PICTURES_PATH), selected)
        }

        test("the chip row is labelled for assistive technologies") {
            runComposeUiTest {
                setContent { RecentDirectoryChips(label = LABEL, paths = listOf(BACKUP_PATH), onSelect = {}) }

                onNode(hasContentDescription(LABEL)).assertExists()
            }
        }

        test("each chip describes its full path for assistive technologies") {
            runComposeUiTest {
                setContent { RecentDirectoryChips(label = LABEL, paths = listOf(BACKUP_PATH, PICTURES_PATH), onSelect = {}) }

                onNodeWithText("Backup").assert(hasContentDescription(BACKUP_PATH))
                onNodeWithText("Pictures").assert(hasContentDescription(PICTURES_PATH))
            }
        }

        test("a folder name too long for the row is truncated with an ellipsis") {
            runComposeUiTest {
                setContent {
                    Box(Modifier.width(NARROW_ROW_WIDTH)) {
                        RecentDirectoryChips(label = LABEL, paths = listOf(LONG_NAME_PATH), onSelect = {})
                    }
                }

                val layouts = mutableListOf<TextLayoutResult>()
                onNodeWithText(directoryChipLabel(LONG_NAME_PATH), useUnmergedTree = true)
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                val layout = layouts.single()
                assertTrue(layout.hasVisualOverflow)
                assertEquals(TextOverflow.Ellipsis, layout.layoutInput.overflow)
                assertTrue(onNodeWithText(directoryChipLabel(LONG_NAME_PATH)).getBoundsInRoot().right <= NARROW_ROW_WIDTH)
            }
        }
    }
}
