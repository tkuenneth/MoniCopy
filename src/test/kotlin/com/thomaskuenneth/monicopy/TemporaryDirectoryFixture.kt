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
@file:OptIn(ExperimentalPathApi::class)

package com.thomaskuenneth.monicopy

import de.infix.testBalloon.framework.core.TestFixture
import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.core.testPlatform
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively

/**
 * Temp directory per TestBalloon docs: keep on local failure for inspection, delete on success or CI.
 */
fun TestSuiteScope.temporaryDirectoryFixture(
    prefix: String = "${testSuiteInScope.testElementPath}-",
): TestFixture<Path> {
    val root = Path("build/tmp").also { Files.createDirectories(it) }
    return testFixture {
        Files.createTempDirectory(root, prefix)
    } closeWith { testsSucceeded ->
        if (testsSucceeded || testPlatform.environment("CI") != null) {
            deleteRecursively()
        } else {
            println("Temporary directory: file://${toAbsolutePath()}")
        }
    }
}
