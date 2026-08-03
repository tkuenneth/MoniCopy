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

package com.thomaskuenneth.monicopy.copy

import com.thomaskuenneth.monicopy.temporaryDirectoryFixture
import de.infix.testBalloon.framework.core.testSuite
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes
import kotlin.test.assertEquals

val RelativePathsTests by testSuite {
    temporaryDirectoryFixture().asParameterForEach {
        test("nested entry is relative to the root") { directory ->
            val root = directory.resolve("root").also { it.createDirectories() }.toFile()
            val entry = directory.resolve("root/a/b.txt").also {
                it.parent.createDirectories()
                it.writeBytes(byteArrayOf(1))
            }.toFile()

            assertEquals("a${File.separator}b.txt", relativePathUnder(root, entry))
        }

        test("entry equal to the root yields an empty relative path") { directory ->
            val root = directory.resolve("root").also { it.createDirectories() }.toFile()

            assertEquals("", relativePathUnder(root, root))
        }

        test("trailing separator on the root does not appear in the relative path") { directory ->
            val rootDir = directory.resolve("root").also { it.createDirectories() }
            val root = File(rootDir.toFile().absolutePath + File.separator)
            val entry = rootDir.resolve("child.txt").also { it.writeBytes(byteArrayOf(1)) }.toFile()

            assertEquals("child.txt", relativePathUnder(root, entry))
        }
    }
}
