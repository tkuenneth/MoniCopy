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

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import com.thomaskuenneth.monicopy.copy.DirectoryRole
import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import org.jetbrains.compose.resources.getString

private const val PATH = "/Volumes/Backup/Documents"
private const val NEXT_CONTROL = "Next control"
private const val DECOY_CONTROL = "Control composed in between"

private val activationKeyNames = mapOf(
    Key.Enter to "Enter",
    Key.NumPadEnter to "NumPad Enter",
    Key.Spacebar to "Space",
)

val DirectoryFieldTests by testSuite(compartment = { TestCompartment.RealTime }) {
    for (role in DirectoryRole.entries) {
        testSuite("$role") {
            test("shows the path and a clear button when a directory is set") {
                directoryFieldTest(role, PATH) {
                    onNodeWithText(PATH).assertIsDisplayed()
                    clearButton().assertIsDisplayed()
                }
            }

            test("hides the clear button when no directory is set") {
                directoryFieldTest(role, path = null) {
                    onNodeWithContentDescription(clearLabel).assertDoesNotExist()
                }
            }

            test("clicking the clear button clears without opening the chooser") {
                directoryFieldTest(role, PATH) {
                    clearButton().performClick()

                    assertEquals(listOf(FieldEvent.Clear to role), events)
                }
            }

            test("clearing moves keyboard focus back to the field") {
                directoryFieldTest(role, PATH) {
                    clearButton().performClick()

                    field().assertIsFocused()
                }
            }

            test("Tab moves from the field to the clear button, then to the next control") {
                directoryFieldTest(role, PATH) {
                    field().requestFocus()

                    field().performKeyInput { pressKey(Key.Tab) }
                    clearButton().assertIsFocused()

                    clearButton().performKeyInput { pressKey(Key.Tab) }
                    nextControl().assertIsFocused()
                }
            }

            test("Tab moves from the field to the next control when no directory is set") {
                directoryFieldTest(role, path = null) {
                    field().requestFocus()

                    field().performKeyInput { pressKey(Key.Tab) }

                    nextControl().assertIsFocused()
                }
            }

            test("hovering the clear button shows its tooltip") {
                directoryFieldTest(role, PATH) {
                    clearButton().assertShowsTooltipOnHover(clearLabel)
                }
            }

            for (path in listOf(PATH, null)) {
                test("hovering the field shows its tooltip when the path is ${path ?: "not set"}") {
                    directoryFieldTest(role, path) {
                        field().assertShowsTooltipOnHover(title)
                    }
                }
            }

            test("clicking the field opens the chooser") {
                directoryFieldTest(role, PATH) {
                    field().performClick()

                    assertEquals(listOf(FieldEvent.Select to role), events)
                }
            }

            for ((key, keyName) in activationKeyNames) {
                test("pressing $keyName on the focused field opens the chooser") {
                    directoryFieldTest(role, PATH) {
                        field().requestFocus()

                        field().performKeyInput { pressKey(key) }

                        assertEquals(listOf(FieldEvent.Select to role), events)
                    }
                }

                test("pressing $keyName on the focused clear button clears without opening the chooser") {
                    directoryFieldTest(role, PATH) {
                        clearButton().requestFocus()

                        clearButton().performKeyInput { pressKey(key) }

                        assertEquals(listOf(FieldEvent.Clear to role), events)
                    }
                }
            }

            for (path in listOf(PATH, null)) {
                test("assistive technologies get the field's purpose and value when the path is ${path ?: "not set"}") {
                    directoryFieldTest(role, path) {
                        field()
                            .assert(hasText(fieldText))
                            .assert(hasContentDescription(title))
                            .assert(hasClickAction())
                    }
                }
            }

            test("assistive technologies can open the chooser") {
                directoryFieldTest(role, PATH) {
                    field().performSemanticsAction(SemanticsActions.OnClick)

                    assertEquals(listOf(FieldEvent.Select to role), events)
                }
            }
        }
    }
}

private enum class FieldEvent {
    Select, Clear
}

private class DirectoryFieldScope(
    composeUiTest: ComposeUiTest,
    val fieldText: String,
    val title: String,
    val clearLabel: String,
) : ComposeUiTest by composeUiTest {
    val events = mutableListOf<Pair<FieldEvent, DirectoryRole>>()

    fun field(): SemanticsNodeInteraction = onNodeWithText(fieldText)

    fun clearButton(): SemanticsNodeInteraction = onNodeWithContentDescription(clearLabel)

    fun nextControl(): SemanticsNodeInteraction = onNodeWithText(NEXT_CONTROL)

    fun SemanticsNodeInteraction.assertShowsTooltipOnHover(tooltip: String) {
        performMouseInput { enter(center) }
        waitUntil { onAllNodes(hasText(tooltip)).fetchSemanticsNodes().isNotEmpty() }
    }
}

private suspend fun directoryFieldTest(
    role: DirectoryRole,
    path: String?,
    block: DirectoryFieldScope.() -> Unit,
) {
    val title = getString(role.title)
    val clearLabel = getString(role.clearLabel)
    runComposeUiTest {
        val scope = DirectoryFieldScope(this, path ?: EMPTY_DIRECTORY_PLACEHOLDER, title, clearLabel)
        setContent {
            val nextControl = remember { FocusRequester() }
            Column {
                DirectoryField(
                    role = role,
                    path = path,
                    onSelect = { scope.events += FieldEvent.Select to it },
                    onClear = { scope.events += FieldEvent.Clear to it },
                    nextFocusRequester = nextControl,
                )
                TextButton(onClick = {}) {
                    Text(DECOY_CONTROL)
                }
                TextButton(
                    onClick = {},
                    modifier = Modifier.focusRequester(nextControl),
                ) {
                    Text(NEXT_CONTROL)
                }
            }
        }
        scope.block()
    }
}
