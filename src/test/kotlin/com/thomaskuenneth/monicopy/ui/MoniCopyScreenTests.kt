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

import androidx.compose.material3.DividerDefaults
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.thomaskuenneth.monicopy.NavigationState
import com.thomaskuenneth.monicopy.copy.ControllableCopyEngine
import com.thomaskuenneth.monicopy.copy.CopyPreferences
import com.thomaskuenneth.monicopy.copy.CopyViewModel
import com.thomaskuenneth.monicopy.copy.RecordingCopyRepository
import com.thomaskuenneth.monicopy.copy.ScriptedDirectoryChooser
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.add_ignore
import com.thomaskuenneth.monicopy.generated.resources.copy_all_files_and_folders_inside
import com.thomaskuenneth.monicopy.generated.resources.delete_orphaned_files
import com.thomaskuenneth.monicopy.generated.resources.ignored_directories
import com.thomaskuenneth.monicopy.generated.resources.start
import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import org.jetbrains.compose.resources.getString

val MoniCopyScreenTests by testSuite(compartment = { TestCompartment.RealTime }) {
    test("the setup pane's scroll area starts at the window's top edge") {
        moniCopyScreenTest {
            assertEquals(window().getBoundsInRoot().top, setupPaneScrollArea().getBoundsInRoot().top)
        }
    }

    test("the action button is centred below the divider") {
        moniCopyScreenTest {
            val pane = setupPaneScrollArea().getBoundsInRoot()
            val button = onNodeWithText(labels.start).getBoundsInRoot()
            val window = window().getBoundsInRoot()

            assertEquals(window.bottom - button.bottom, button.top - pane.bottom - DividerDefaults.Thickness)
        }
    }

    test("the setup pane's vertical padding scrolls with its content") {
        moniCopyScreenTest {
            val pane = setupPaneScrollArea()

            pane.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, -SCROLL_DISTANCE) }
            assertEquals(
                pane.getBoundsInRoot().top + UIConstants.PREFERRED_VERTICAL_PADDING,
                onNodeWithText(labels.heading).getBoundsInRoot().top,
            )

            pane.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, SCROLL_DISTANCE) }
            assertEquals(
                pane.getBoundsInRoot().bottom - UIConstants.PREFERRED_VERTICAL_PADDING,
                onNodeWithText(labels.deleteOrphans).getBoundsInRoot().bottom,
            )
        }
    }

    test("the ignored directories list fills its pane down to the bottom padding") {
        moniCopyScreenTest {
            assertEquals(
                setupPaneScrollArea().getBoundsInRoot().bottom - UIConstants.PREFERRED_VERTICAL_PADDING,
                ignoredDirectoriesList().getBoundsInRoot().bottom,
            )
        }
    }

    test("nothing is focused when the window opens") {
        moniCopyScreenTest {
            mainClock.advanceTimeBy(SETTLE_MILLIS)

            onAllNodes(isFocused()).assertCountEquals(0)
        }
    }

    test("opening the ignored directories does not move the focus into them") {
        moniCopyScreenTest(MINIMUM_WINDOW) {
            mainClock.advanceTimeBy(SETTLE_MILLIS)
            onNodeWithText(labels.ignoredDirectories).performClick()
            mainClock.advanceTimeBy(SETTLE_MILLIS)
            onNodeWithText(labels.add).assertExists()

            onAllNodes(isFocused()).assertCountEquals(0)
        }
    }

    test("the ignored directories pane keeps its top padding") {
        moniCopyScreenTest {
            assertEquals(
                window().getBoundsInRoot().top + UIConstants.PREFERRED_VERTICAL_PADDING,
                onNodeWithText(labels.ignoredDirectories).getBoundsInRoot().top,
            )
        }
    }
}

private const val SCROLL_DISTANCE = 100_000f
private const val SETTLE_MILLIS = 2_000L
private val MINIMUM_WINDOW = DpSize(
    WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND.dp,
    WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND.dp,
)
private val WIDE_SHORT_WINDOW = DpSize(920.dp, 300.dp)

private class MoniCopyScreenLabels(
    val heading: String,
    val start: String,
    val deleteOrphans: String,
    val ignoredDirectories: String,
    val add: String,
)

private class MoniCopyScreenScope(
    composeUiTest: ComposeUiTest,
    val labels: MoniCopyScreenLabels,
) : ComposeUiTest by composeUiTest {
    fun window(): SemanticsNodeInteraction = onAllNodes(isRoot()).onFirst()

    fun setupPaneScrollArea(): SemanticsNodeInteraction =
        onNode(hasScrollAction() and hasAnyDescendant(hasText(labels.heading)))

    fun ignoredDirectoriesList(): SemanticsNodeInteraction =
        onNode(hasScrollToIndexAction() and hasAnyAncestor(hasAnyDescendant(hasText(labels.ignoredDirectories))))
}

private suspend fun moniCopyScreenTest(windowSize: DpSize = WIDE_SHORT_WINDOW, block: MoniCopyScreenScope.() -> Unit) {
    val labels = MoniCopyScreenLabels(
        heading = getString(Res.string.copy_all_files_and_folders_inside),
        start = getString(Res.string.start),
        deleteOrphans = getString(Res.string.delete_orphaned_files),
        ignoredDirectories = getString(Res.string.ignored_directories),
        add = getString(Res.string.add_ignore),
    )
    val viewModel = CopyViewModel(
        engine = ControllableCopyEngine(),
        repository = RecordingCopyRepository(CopyPreferences()),
        directoryChooser = ScriptedDirectoryChooser(),
    )
    runComposeUiTest {
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            MoniCopyScreen(
                uiState = uiState,
                viewModel = viewModel,
                navigationState = NavigationState(),
                modifier = Modifier.size(windowSize),
            )
        }
        MoniCopyScreenScope(this, labels).block()
    }
}
