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
import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.testSuite
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.writeBytes
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

val CopyCountReportingTests by testSuite(
    compartment = { TestCompartment.Sequential },
) {
    temporaryDirectoryFixture().asParameterForEach {
        val cases = mapOf(
            "without delete orphans" to false,
            "with delete orphans" to true,
        )

        for ((label, deleteOrphans) in cases) {
            test("reports file and folder counts $label") { directory ->
                val ws = CopyEngineWorkspace(directory)
                // 3 regular files, 2 subfolders, 2 symbolic links (must not inflate fileCount)
                ws.source.resolve("root.txt").writeBytes(byteArrayOf(1))
                val docs = ws.source.resolve("docs").also { it.createDirectories() }
                docs.resolve("readme.txt").writeBytes(byteArrayOf(2))
                val images = docs.resolve("images").also { it.createDirectories() }
                images.resolve("photo.bin").writeBytes(byteArrayOf(3))
                val linkTarget = ws.root.resolve("link-target").also { it.createDirectories() }
                Files.createSymbolicLink(ws.source.resolve("linked-dir"), linkTarget)
                Files.createSymbolicLink(ws.source.resolve("alias.txt"), Path.of("root.txt"))

                if (deleteOrphans) {
                    ws.dest.resolve("orphan.txt").writeBytes(byteArrayOf(9))
                    ws.dest.resolve("orphan-dir").also { it.createDirectories() }.resolve("gone.txt").writeBytes(byteArrayOf(8))
                    Files.createSymbolicLink(ws.dest.resolve("orphan-link"), linkTarget)
                }

                var fileCount: Long? = null
                var subfolderCount: Long? = null
                val decisions = mutableListOf<Boolean>()

                ws.engine.copy(
                    ws.source.toString(),
                    ws.dest.toString(),
                    emptyList(),
                    onMessage = OnMessage { _, _ -> },
                    onProgress = {},
                    onCounts = { files, folders ->
                        fileCount = files
                        subfolderCount = folders
                    },
                    onCopyDecision = decisions::add,
                    preserveSymbolicLinks = true,
                )

                assertEquals(3L, fileCount)
                assertEquals(2L, subfolderCount)
                assertEquals(3, decisions.size)
                assertTrue(decisions.all { it })
                assertEquals(2, ws.engine.rememberedSymbolicLinks.size)

                if (deleteOrphans) {
                    ws.engine.copyStateProvider = { CopyState.DELETING }
                    ws.engine.deleteOrphans(ws.source.toString(), ws.dest.toString(), emptyList(), onMessage = OnMessage { _, _ -> })
                }

                assertTrue(ws.dest.resolve("root.txt").isRegularFile())
                assertTrue(ws.dest.resolve("docs/readme.txt").isRegularFile())
                assertTrue(ws.dest.resolve("docs/images/photo.bin").isRegularFile())
                assertTrue(Files.isSymbolicLink(ws.dest.resolve("linked-dir")))
                assertTrue(Files.isSymbolicLink(ws.dest.resolve("alias.txt")))

                if (deleteOrphans) {
                    assertFalse(ws.dest.resolve("orphan.txt").exists())
                    assertFalse(ws.dest.resolve("orphan-dir").exists())
                    assertFalse(Files.exists(ws.dest.resolve("orphan-link"), LinkOption.NOFOLLOW_LINKS))
                }
            }
        }
    }
}
