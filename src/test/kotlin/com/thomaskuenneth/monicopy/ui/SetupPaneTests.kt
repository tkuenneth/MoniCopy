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

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import com.thomaskuenneth.monicopy.NavigationState
import com.thomaskuenneth.monicopy.copy.ControllableCopyEngine
import com.thomaskuenneth.monicopy.copy.CopyPreferences
import com.thomaskuenneth.monicopy.copy.CopyViewModel
import com.thomaskuenneth.monicopy.copy.DirectoryRole
import com.thomaskuenneth.monicopy.copy.RecordingCopyRepository
import com.thomaskuenneth.monicopy.copy.ScriptedDirectoryChooser
import com.thomaskuenneth.monicopy.createSubdirectory
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.delete_orphaned_files
import com.thomaskuenneth.monicopy.temporaryDirectoryFixture
import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.testSuite
import org.jetbrains.compose.resources.getString

val SetupPaneTests by testSuite(compartment = { TestCompartment.RealTime }) {
    temporaryDirectoryFixture().asParameterForEach {
        test("Tab visits each directory and its clear button, then the orphans checkbox") { directory ->
            val sourcePath = directory.createSubdirectory("source")
            val clearSource = getString(DirectoryRole.Source.clearLabel)
            val destinationPath = directory.createSubdirectory("dest")
            val clearDestination = getString(DirectoryRole.Destination.clearLabel)
            val deleteOrphans = getString(Res.string.delete_orphaned_files)
            val viewModel = CopyViewModel(
                engine = ControllableCopyEngine(),
                repository = RecordingCopyRepository(
                    CopyPreferences(
                        sourceDir = sourcePath,
                        destDir = destinationPath,
                    ),
                ),
                directoryChooser = ScriptedDirectoryChooser(),
            )
            runComposeUiTest {
                setContent {
                    val uiState by viewModel.uiState.collectAsState()
                    SetupPane(
                        uiState = uiState,
                        viewModel = viewModel,
                        navigationState = NavigationState(),
                    )
                }
                val tabOrder = listOf(
                    onNodeWithText(sourcePath),
                    onNodeWithContentDescription(clearSource),
                    onNodeWithText(destinationPath),
                    onNodeWithContentDescription(clearDestination),
                    onNodeWithText(deleteOrphans),
                )
                tabOrder.first().requestFocus()

                tabOrder.zipWithNext().forEach { (current, next) ->
                    current.assertIsFocused()
                    current.performKeyInput { pressKey(Key.Tab) }
                    next.assertIsFocused()
                }
            }
        }
    }
}
