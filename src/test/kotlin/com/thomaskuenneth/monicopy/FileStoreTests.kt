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
package com.thomaskuenneth.monicopy

import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.testSuite
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

val FileStoreTests by testSuite(
    compartment = { TestCompartment.Concurrent },
) {
    temporaryDirectoryFixture().asParameterForEach {
        test("ignored directories are skipped when scanning") { directory ->
            val root = directory.toFile()
            val kept = directory.resolve("kept.txt").also { it.writeText("ok") }.toFile()
            val ignoredDir = directory.resolve("ignored").also { it.createDirectories() }.toFile()
            ignoredDir.resolve("secret.txt").writeText("skip-me")

            val store = FileStore(null)
            val files = store.fill(root, listOf(ignoredDir.absolutePath))

            assertEquals(1, files.size)
            assertEquals(kept.absolutePath, files.single().absolutePath)
            assertTrue(files.none { it.name == "secret.txt" })
            assertEquals(1, store.numberOfDirectories)
        }

        test("symbolic links are skipped when scanning") { directory ->
            val root = directory.resolve("scan-root").also { it.createDirectories() }
            val kept = root.resolve("kept.txt").also { it.writeText("ok") }.toFile()
            val target = directory.resolve("link-target").also { it.createDirectories() }
            target.resolve("via-link.txt").writeText("skip-me")
            Files.createSymbolicLink(root.resolve("linked"), target)

            val store = FileStore(null)
            val files = store.fill(root.toFile(), emptyList())

            assertEquals(1, files.size)
            assertEquals(kept.absolutePath, files.single().absolutePath)
            assertTrue(files.none { it.name == "via-link.txt" })
        }

        test("fill with null file returns null") {
            val store = FileStore(null)
            assertNull(store.fill(null, emptyList()))
        }
    }
}
