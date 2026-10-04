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
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getBoundsInRoot
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
import java.nio.file.Path
import kotlin.test.assertTrue
import org.jetbrains.compose.resources.getString

val SetupPaneTests by testSuite(compartment = { TestCompartment.RealTime }) {
    temporaryDirectoryFixture().asParameterForEach {
        test("Tab visits each directory, its clear button and its chip row, then the orphans checkbox") { directory ->
            val paths = SetupPanePaths(directory)
            val labels = SetupPaneLabels.load()

            setupPaneTest(paths.preferencesWithHistories()) {
                assertTabOrder(
                    onNodeWithContentDescription(labels.sourceTitle),
                    onNodeWithContentDescription(labels.clearSource),
                    chip(paths.earlierSource),
                    onNodeWithContentDescription(labels.destinationTitle),
                    onNodeWithContentDescription(labels.clearDestination),
                    chip(paths.earlierDestination),
                    onNodeWithText(labels.deleteOrphans),
                )
            }
        }

        test("each chip row appears beneath its directory") { directory ->
            val paths = SetupPanePaths(directory)
            val labels = SetupPaneLabels.load()

            setupPaneTest(paths.preferencesWithHistories()) {
                val sourceField = onNodeWithContentDescription(labels.sourceTitle).getBoundsInRoot()
                val sourceChip = chip(paths.earlierSource).getBoundsInRoot()
                val destinationField = onNodeWithContentDescription(labels.destinationTitle).getBoundsInRoot()
                val destinationChip = chip(paths.earlierDestination).getBoundsInRoot()

                assertTrue(sourceChip.top >= sourceField.bottom)
                assertTrue(destinationField.top >= sourceChip.bottom)
                assertTrue(destinationChip.top >= destinationField.bottom)
            }
        }

        test("the current directories get no chips") { directory ->
            val paths = SetupPanePaths(directory)
            val labels = SetupPaneLabels.load()

            setupPaneTest(CopyPreferences(sourceDir = paths.source, destDir = paths.destination)) {
                chip(paths.source).assertDoesNotExist()
                chip(paths.destination).assertDoesNotExist()
                assertTabOrder(
                    onNodeWithContentDescription(labels.sourceTitle),
                    onNodeWithContentDescription(labels.clearSource),
                    onNodeWithContentDescription(labels.destinationTitle),
                    onNodeWithContentDescription(labels.clearDestination),
                    onNodeWithText(labels.deleteOrphans),
                )
            }
        }
    }
}

private class SetupPanePaths(directory: Path) {
    val source = directory.createSubdirectory("source")
    val destination = directory.createSubdirectory("destination")
    val earlierSource = directory.createSubdirectory("earlier-source")
    val earlierDestination = directory.createSubdirectory("earlier-destination")

    fun preferencesWithHistories() = CopyPreferences(
        sourceDir = source,
        destDir = destination,
        histories = mapOf(
            DirectoryRole.Source to listOf(source, earlierSource),
            DirectoryRole.Destination to listOf(destination, earlierDestination),
        ),
    )
}

private class SetupPaneLabels(
    val sourceTitle: String,
    val clearSource: String,
    val destinationTitle: String,
    val clearDestination: String,
    val deleteOrphans: String,
) {
    companion object {
        suspend fun load() = SetupPaneLabels(
            sourceTitle = getString(DirectoryRole.Source.title),
            clearSource = getString(DirectoryRole.Source.clearLabel),
            destinationTitle = getString(DirectoryRole.Destination.title),
            clearDestination = getString(DirectoryRole.Destination.clearLabel),
            deleteOrphans = getString(Res.string.delete_orphaned_files),
        )
    }
}

private fun ComposeUiTest.chip(path: String): SemanticsNodeInteraction =
    onNodeWithText(directoryChipLabel(path))

private fun assertTabOrder(vararg nodes: SemanticsNodeInteraction) {
    nodes.first().requestFocus()
    nodes.toList().zipWithNext().forEach { (current, next) ->
        current.assertIsFocused()
        current.performKeyInput { pressKey(Key.Tab) }
        next.assertIsFocused()
    }
}

private fun setupPaneTest(preferences: CopyPreferences, block: ComposeUiTest.() -> Unit) {
    val viewModel = CopyViewModel(
        engine = ControllableCopyEngine(),
        repository = RecordingCopyRepository(preferences),
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
        block()
    }
}
