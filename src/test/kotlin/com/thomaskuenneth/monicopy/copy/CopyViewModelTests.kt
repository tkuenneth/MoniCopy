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

import com.thomaskuenneth.monicopy.platform.DirectoryChooser
import com.thomaskuenneth.monicopy.temporaryDirectoryBasedFixture
import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestFixture
import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.core.testScope
import de.infix.testBalloon.framework.core.testSuite
import java.nio.file.Path
import java.util.ArrayDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
            val ignorePath = harness.directory.resolve("ignored").also { it.createDirectories() }.toFile().absolutePath
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

        test("selecting source and destination updates state and repository") { harness ->
            val source = harness.directory.resolve("picked-source").also { it.createDirectories() }.toFile().absolutePath
            val dest = harness.directory.resolve("picked-dest").also { it.createDirectories() }.toFile().absolutePath
            harness.directoryChooser.enqueue(source, dest)

            harness.viewModel.selectSource()
            harness.viewModel.selectDest()

            assertEquals(source, harness.viewModel.uiState.value.sourceDir)
            assertEquals(dest, harness.viewModel.uiState.value.destDir)
            assertEquals(listOf(source), harness.repository.savedSourceDirs)
            assertEquals(listOf(dest), harness.repository.savedDestDirs)
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
                    sourceDir = defaultHarness.directory.resolve("source").also { it.createDirectories() }.toFile().absolutePath,
                    destDir = defaultHarness.directory.resolve("dest").also { it.createDirectories() }.toFile().absolutePath,
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
}

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
    preferences: CopyPreferences = CopyPreferences(
        sourceDir = directory.resolve("source").also { it.createDirectories() }.toFile().absolutePath,
        destDir = directory.resolve("dest").also { it.createDirectories() }.toFile().absolutePath,
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

private class RecordingCopyRepository(
    private val preferences: CopyPreferences,
) : CopyRepository {
    val savedSourceDirs = mutableListOf<String?>()
    val savedDestDirs = mutableListOf<String?>()
    val savedDeleteOrphans = mutableListOf<Boolean>()
    val savedPreserveSymbolicLinks = mutableListOf<Boolean>()
    val savedIgnores = mutableListOf<List<String>>()

    override fun load(): CopyPreferences = preferences
    override fun saveSourceDir(path: String?) {
        savedSourceDirs += path
    }

    override fun saveDestDir(path: String?) {
        savedDestDirs += path
    }

    override fun saveDeleteOrphans(enabled: Boolean) {
        savedDeleteOrphans += enabled
    }

    override fun savePreserveSymbolicLinks(enabled: Boolean) {
        savedPreserveSymbolicLinks += enabled
    }

    override fun saveIgnores(ignores: List<String>) {
        savedIgnores += ignores
    }
}

private class ControllableCopyEngine : CopyEngine {
    val calls = mutableListOf<String>()
    val copyEntered = CountDownLatch(1)
    val copyFinished = CountDownLatch(1)
    val deleteEntered = CountDownLatch(1)
    private val copyGate = CountDownLatch(1)
    private val deleteGate = CountDownLatch(1)
    var blockCopy = false
    var blockDelete = false
    var resumeCount = 0
    var cancelCount = 0
    var lastPreserveSymbolicLinks: Boolean? = null

    override var copyStateProvider: () -> CopyState = { CopyState.IDLE }

    override fun resume() {
        resumeCount++
    }

    override fun cancel() {
        cancelCount++
        copyGate.countDown()
        deleteGate.countDown()
    }

    override fun copy(
        fromPath: String,
        toPath: String,
        ignores: List<String>,
    ) {
        copyEntered.countDown()
        if (blockCopy) {
            copyGate.await()
        }
        calls += "copy"
        copyFinished.countDown()
    }

    override fun copy(
        fromPath: String,
        toPath: String,
        ignores: List<String>,
        onProgress: (Int) -> Unit,
        onCounts: (fileCount: Long, subfolderCount: Long) -> Unit,
        onCopyDecision: (copied: Boolean) -> Unit,
        preserveSymbolicLinks: Boolean,
    ) {
        lastPreserveSymbolicLinks = preserveSymbolicLinks
        copy(fromPath, toPath, ignores)
    }

    override fun deleteOrphans(
        sourcePath: String,
        destPath: String,
        ignores: List<String>,
    ) {
        deleteEntered.countDown()
        if (blockDelete) {
            deleteGate.await()
        }
        calls += "deleteOrphans"
    }

    fun releaseCopy() {
        copyGate.countDown()
    }

    fun releaseDelete() {
        deleteGate.countDown()
    }
}

private class ScriptedDirectoryChooser : DirectoryChooser {
    private val results = ArrayDeque<String?>()

    fun enqueue(vararg paths: String?) {
        results.addAll(paths.toList())
    }

    override fun chooseDirectory(title: String, initialPath: String?): String? =
        if (results.isEmpty()) null else results.removeFirst()
}

