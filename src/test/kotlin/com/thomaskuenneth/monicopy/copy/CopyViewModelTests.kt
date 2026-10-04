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
package com.thomaskuenneth.monicopy.copy

import com.thomaskuenneth.monicopy.createSubdirectory
import com.thomaskuenneth.monicopy.temporaryDirectoryBasedFixture
import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestFixture
import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.core.testScope
import de.infix.testBalloon.framework.core.testSuite
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

val CopyViewModelTests by testSuite(
    compartment = {
        TestCompartment.MainDispatcher(
            testConfig = TestConfig.testScope(isEnabled = false),
        )
    },
) {
    copyViewModelHarnessFixture().asParameterForEach {
        test("copy then delete orphans reaches FINISHED") { harness ->
            harness.viewModel.onActionButtonClick()

            val finished = harness.awaitFinished()

            assertEquals(listOf("copy", "deleteOrphans"), harness.engine.calls)
            assertTrue(finished.copyPhaseComplete)
            assertTrue(finished.orphanPhaseComplete)
        }

        test("copy without delete orphans reaches FINISHED") { harness ->
            harness.viewModel.onDeleteOrphansChanged(false)
            assertEquals(listOf(false), harness.repository.savedDeleteOrphans)

            harness.viewModel.onActionButtonClick()

            val finished = harness.awaitFinished()

            assertEquals(listOf("copy"), harness.engine.calls)
            assertTrue(finished.copyPhaseComplete)
            assertFalse(finished.orphanPhaseComplete)
        }

        test("copy phase completes when copy returns, before orphan delete finishes") { harness ->
            harness.engine.blockDelete = true
            harness.viewModel.onActionButtonClick()
            assertTrue(harness.engine.deleteEntered.await(5, TimeUnit.SECONDS))

            val duringDelete = harness.viewModel.uiState.value
            assertEquals(CopyState.DELETING, duringDelete.copyState)
            assertTrue(duringDelete.copyPhaseComplete)
            assertFalse(duringDelete.orphanPhaseComplete)

            harness.engine.releaseDelete()
            val finished = harness.awaitFinished()
            assertTrue(finished.orphanPhaseComplete)
        }

        test("cancel during copy returns to IDLE") { harness ->
            harness.engine.blockCopy = true
            harness.viewModel.onActionButtonClick()
            assertTrue(harness.engine.copyEntered.await(5, TimeUnit.SECONDS))
            assertEquals(CopyState.COPYING, harness.viewModel.uiState.value.copyState)

            harness.viewModel.cancelOperation()

            val idle = harness.viewModel.uiState.value
            assertEquals(CopyState.IDLE, idle.copyState)
            assertFalse(idle.copyPhaseComplete)
            assertFalse(idle.orphanPhaseComplete)
            assertEquals(1, harness.engine.cancelCount)
            assertTrue(harness.engine.copyFinished.await(5, TimeUnit.SECONDS))
        }

        test("pause and continue during copy") { harness ->
            harness.engine.blockCopy = true
            harness.viewModel.onActionButtonClick()
            assertTrue(harness.engine.copyEntered.await(5, TimeUnit.SECONDS))

            harness.viewModel.onActionButtonClick()
            assertEquals(CopyState.COPY_PAUSED, harness.viewModel.uiState.value.copyState)

            harness.viewModel.onActionButtonClick()
            assertEquals(CopyState.COPYING, harness.viewModel.uiState.value.copyState)
            assertEquals(1, harness.engine.resumeCount)

            harness.engine.releaseCopy()
            harness.awaitFinished()
        }

        test("pause and continue during delete orphans") { harness ->
            harness.engine.blockDelete = true
            harness.viewModel.onActionButtonClick()
            assertTrue(harness.engine.deleteEntered.await(5, TimeUnit.SECONDS))
            assertEquals(CopyState.DELETING, harness.viewModel.uiState.value.copyState)

            harness.viewModel.onActionButtonClick()
            assertEquals(CopyState.DELETE_PAUSED, harness.viewModel.uiState.value.copyState)

            harness.viewModel.onActionButtonClick()
            assertEquals(CopyState.DELETING, harness.viewModel.uiState.value.copyState)
            assertEquals(1, harness.engine.resumeCount)

            harness.engine.releaseDelete()
            val finished = harness.awaitFinished()
            assertEquals(listOf("copy", "deleteOrphans"), harness.engine.calls)
            assertTrue(finished.copyPhaseComplete)
            assertTrue(finished.orphanPhaseComplete)
        }

        test("add and remove ignored directories persist through the repository") { harness ->
            val ignorePath = harness.directory.createSubdirectory("ignored")
            harness.directoryChooser.enqueue(ignorePath)

            harness.viewModel.addIgnore()

            assertEquals(1, harness.viewModel.uiState.value.ignores.size)
            assertEquals(ignorePath, harness.viewModel.uiState.value.ignores.single().absolutePath)
            assertEquals(listOf(listOf(ignorePath)), harness.repository.savedIgnores)

            val ignored = harness.viewModel.uiState.value.ignores.single()
            harness.viewModel.toggleIgnoreSelection(ignored)
            harness.viewModel.removeSelectedIgnores()

            assertTrue(harness.viewModel.uiState.value.ignores.isEmpty())
            assertEquals(listOf(listOf(ignorePath), emptyList()), harness.repository.savedIgnores)
        }

        test("FINISHED action returns to IDLE") { harness ->
            harness.viewModel.onDeleteOrphansChanged(false)
            harness.viewModel.onActionButtonClick()
            harness.awaitFinished()

            harness.viewModel.onActionButtonClick()

            assertEquals(CopyState.IDLE, harness.viewModel.uiState.value.copyState)
        }

        test("preserve symbolic links preference loads, saves, and is passed to the engine") { defaultHarness ->
            val harness = CopyViewModelHarness(
                directory = defaultHarness.directory,
                preferences = CopyPreferences(
                    sourceDir = defaultHarness.directory.createSubdirectory("source"),
                    destDir = defaultHarness.directory.createSubdirectory("dest"),
                    deleteOrphans = false,
                    preserveSymbolicLinks = false,
                ),
            )
            assertFalse(harness.viewModel.uiState.value.preserveSymbolicLinks)

            harness.viewModel.onPreserveSymbolicLinksChanged(true)
            assertTrue(harness.viewModel.uiState.value.preserveSymbolicLinks)
            assertEquals(listOf(true), harness.repository.savedPreserveSymbolicLinks)

            harness.viewModel.onActionButtonClick()
            harness.awaitFinished()

            assertEquals(true, harness.engine.lastPreserveSymbolicLinks)
        }
    }

    for (role in DirectoryRole.entries) {
        testSuite("$role directory") {
            copyViewModelHarnessFixture().asParameterForEach {
                test("selecting a directory updates state and repository") { harness ->
                    val other = harness.viewModel.uiState.value.directory(role.other)
                    val picked = harness.directory.createSubdirectory("picked-$role")
                    harness.directoryChooser.enqueue(picked)

                    harness.viewModel.selectDirectory(role)

                    assertEquals(picked, harness.viewModel.uiState.value.directory(role))
                    assertEquals(other, harness.viewModel.uiState.value.directory(role.other))
                    assertEquals(listOf(picked), harness.repository.savedDirectories(role))
                    assertTrue(harness.repository.savedDirectories(role.other).isEmpty())
                }

                test("cancelling the chooser keeps the directory and saves nothing") { harness ->
                    val before = harness.viewModel.uiState.value.directory(role)
                    harness.directoryChooser.enqueue(null)

                    harness.viewModel.selectDirectory(role)

                    assertEquals(1, harness.directoryChooser.requestCount)
                    assertEquals(before, harness.viewModel.uiState.value.directory(role))
                    assertTrue(harness.repository.savedDirectories(role).isEmpty())
                    assertTrue(harness.repository.savedHistories.getValue(role).isEmpty())
                }

                test("clearing resets only this directory and persists the cleared value") { harness ->
                    val other = harness.viewModel.uiState.value.directory(role.other)
                    assertNotNull(harness.viewModel.uiState.value.directory(role))

                    harness.viewModel.clearDirectory(role)

                    assertNull(harness.viewModel.uiState.value.directory(role))
                    assertEquals(other, harness.viewModel.uiState.value.directory(role.other))
                    assertEquals(listOf(null), harness.repository.savedDirectories(role))
                    assertTrue(harness.repository.savedDirectories(role.other).isEmpty())
                    assertEquals(0, harness.directoryChooser.requestCount)
                }

                test("Start does nothing after clearing") { harness ->
                    harness.viewModel.clearDirectory(role)

                    harness.viewModel.onActionButtonClick()

                    assertEquals(CopyState.IDLE, harness.viewModel.uiState.value.copyState)
                    assertTrue(harness.engine.calls.isEmpty())
                }

                test("a cleared directory can be selected again and is created if missing") { harness ->
                    harness.viewModel.clearDirectory(role)
                    val picked = harness.directory.resolve("reselected-$role").toFile().absolutePath
                    harness.directoryChooser.enqueue(picked)

                    harness.viewModel.selectDirectory(role)

                    assertEquals(picked, harness.viewModel.uiState.value.directory(role))
                    assertEquals(listOf(null, picked), harness.repository.savedDirectories(role))
                    assertTrue(File(picked).isDirectory)
                }

                test("the history starts with the saved directory") { harness ->
                    val saved = harness.viewModel.uiState.value.directory(role)

                    assertEquals(listOf(saved), harness.viewModel.uiState.value.history(role))
                }

                test("the saved directory moves to the front of a persisted history that contains it") { defaultHarness ->
                    val saved = assertNotNull(defaultHarness.viewModel.uiState.value.directory(role))
                    val harness = defaultHarness.withHistory(role, listOf("/older", saved))

                    assertEquals(listOf(saved, "/older"), harness.viewModel.uiState.value.history(role))
                }

                test("the saved directory is added to the front of a persisted history that lacks it") { defaultHarness ->
                    val saved = assertNotNull(defaultHarness.viewModel.uiState.value.directory(role))
                    val harness = defaultHarness.withHistory(role, listOf("/older"))

                    assertEquals(listOf(saved, "/older"), harness.viewModel.uiState.value.history(role))
                }

                test("choosing a directory adds it to the front of this history only and persists it") { harness ->
                    val initial = harness.viewModel.uiState.value.history(role)
                    val otherHistory = harness.viewModel.uiState.value.history(role.other)
                    val picked = harness.directory.createSubdirectory("picked-$role")
                    harness.directoryChooser.enqueue(picked)

                    harness.viewModel.selectDirectory(role)

                    val expected = listOf(picked) + initial
                    assertEquals(expected, harness.viewModel.uiState.value.history(role))
                    assertEquals(listOf(expected), harness.repository.savedHistories.getValue(role))
                    assertEquals(otherHistory, harness.viewModel.uiState.value.history(role.other))
                    assertTrue(harness.repository.savedHistories.getValue(role.other).isEmpty())
                }

                test("selecting a recent directory fills the field and moves it to the front") { defaultHarness ->
                    val saved = assertNotNull(defaultHarness.viewModel.uiState.value.directory(role))
                    val harness = defaultHarness.withHistory(role, listOf(saved, "/older"))

                    harness.viewModel.selectRecentDirectory(role, "/older")

                    assertEquals("/older", harness.viewModel.uiState.value.directory(role))
                    assertEquals(listOf("/older"), harness.repository.savedDirectories(role))
                    assertEquals(listOf("/older", saved), harness.viewModel.uiState.value.history(role))
                    assertEquals(listOf(listOf("/older", saved)), harness.repository.savedHistories.getValue(role))
                    assertEquals(0, harness.directoryChooser.requestCount)
                }

                test("the current directory is not among the recent directories") { defaultHarness ->
                    val saved = assertNotNull(defaultHarness.viewModel.uiState.value.directory(role))
                    val harness = defaultHarness.withHistory(role, listOf(saved, "/older"))

                    assertEquals(listOf("/older"), harness.viewModel.uiState.value.recentDirectories(role))
                }

                test("choosing another directory makes the previous one recent") { harness ->
                    val previous = assertNotNull(harness.viewModel.uiState.value.directory(role))
                    assertTrue(harness.viewModel.uiState.value.recentDirectories(role).isEmpty())
                    harness.directoryChooser.enqueue(harness.directory.createSubdirectory("picked-$role"))

                    harness.viewModel.selectDirectory(role)

                    assertEquals(listOf(previous), harness.viewModel.uiState.value.recentDirectories(role))
                }

                test("clearing makes the cleared directory recent") { harness ->
                    val cleared = assertNotNull(harness.viewModel.uiState.value.directory(role))

                    harness.viewModel.clearDirectory(role)

                    assertEquals(listOf(cleared), harness.viewModel.uiState.value.recentDirectories(role))
                }

                test("clearing keeps the history") { harness ->
                    val history = harness.viewModel.uiState.value.history(role)

                    harness.viewModel.clearDirectory(role)

                    assertEquals(history, harness.viewModel.uiState.value.history(role))
                    assertTrue(harness.repository.savedHistories.getValue(role).isEmpty())
                }
            }
        }
    }
}

private val DirectoryRole.other: DirectoryRole
    get() = when (this) {
        DirectoryRole.Source -> DirectoryRole.Destination
        DirectoryRole.Destination -> DirectoryRole.Source
    }

private fun CopyViewModelHarness.withHistory(role: DirectoryRole, history: List<String>) =
    CopyViewModelHarness(
        directory = directory,
        preferences = preferences.copy(histories = mapOf(role to history)),
    )

private suspend fun CopyViewModelHarness.awaitFinished(): CopyUiState =
    withTimeout(5.seconds) {
        viewModel.uiState.first { it.copyState == CopyState.FINISHED }
    }

private fun TestSuiteScope.copyViewModelHarnessFixture(): TestFixture<CopyViewModelHarness> =
    temporaryDirectoryBasedFixture(
        create = ::CopyViewModelHarness,
        directoryOf = { it.directory },
    )

private class CopyViewModelHarness(
    val directory: Path,
    val preferences: CopyPreferences = CopyPreferences(
        sourceDir = directory.createSubdirectory("source"),
        destDir = directory.createSubdirectory("dest"),
        deleteOrphans = true,
    ),
) {
    val engine = ControllableCopyEngine()
    val repository = RecordingCopyRepository(preferences)
    val directoryChooser = ScriptedDirectoryChooser()
    val viewModel = CopyViewModel(
        engine = engine,
        repository = repository,
        directoryChooser = directoryChooser,
    )
}
