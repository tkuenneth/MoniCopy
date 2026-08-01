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
import com.thomaskuenneth.monicopy.platform.LogTimeFormatter
import com.thomaskuenneth.monicopy.temporaryDirectoryFixture
import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.testScope
import de.infix.testBalloon.framework.core.testSuite
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.test.assertEquals
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
    val directory = temporaryDirectoryFixture()

    testFixture {
        CopyViewModelHarness(directory())
    } asParameterForEach {
        test("copy then delete orphans reaches FINISHED") { harness ->
            harness.viewModel.onActionButtonClick()

            withTimeout(5.seconds) {
                harness.viewModel.uiState.first { it.copyState == CopyState.FINISHED }
            }

            assertEquals(listOf("copy", "deleteOrphans"), harness.engine.calls)
        }
    }
}

private class CopyViewModelHarness(directory: Path) {
    val engine = RecordingCopyEngine()
    val viewModel = CopyViewModel(
        engine = engine,
        repository = FixedCopyRepository(
            CopyPreferences(
                sourceDir = directory.resolve("source").also { it.createDirectories() }.toFile().absolutePath,
                destDir = directory.resolve("dest").also { it.createDirectories() }.toFile().absolutePath,
                deleteOrphans = true,
            ),
        ),
        directoryChooser = NoopDirectoryChooser(),
        logTimeFormatter = FixedLogTimeFormatter(),
    )
}

private class FixedCopyRepository(
    private val preferences: CopyPreferences,
) : CopyRepository {
    override fun load(): CopyPreferences = preferences
    override fun saveSourceDir(path: String?) = Unit
    override fun saveDestDir(path: String?) = Unit
    override fun saveDeleteOrphans(enabled: Boolean) = Unit
    override fun saveIgnores(ignores: List<String>) = Unit
}

private class RecordingCopyEngine : CopyEngine {
    val calls = mutableListOf<String>()
    override var copyStateProvider: () -> CopyState = { CopyState.IDLE }
    override fun resume() = Unit
    override fun cancel() = Unit

    override fun copy(
        fromPath: String,
        toPath: String,
        ignores: List<String>,
        onMessage: (String) -> Unit,
    ) {
        calls += "copy"
    }

    override fun deleteOrphans(
        sourcePath: String,
        destPath: String,
        ignores: List<String>,
        onMessage: (String) -> Unit,
    ) {
        calls += "deleteOrphans"
    }
}

private class NoopDirectoryChooser : DirectoryChooser {
    override fun chooseDirectory(title: String, initialPath: String?): String? = null
}

private class FixedLogTimeFormatter : LogTimeFormatter {
    override fun format(): String = "00:00:00"
}
