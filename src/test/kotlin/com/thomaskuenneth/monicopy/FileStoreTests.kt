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

import de.infix.testBalloon.framework.core.testSuite
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

val FileStoreTests by testSuite {
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

        test("symbolic links are collected when scanning") { directory ->
            val root = directory.resolve("scan-root").also { it.createDirectories() }
            val kept = root.resolve("kept.txt").also { it.writeText("ok") }.toFile()
            val target = directory.resolve("link-target").also { it.createDirectories() }
            target.resolve("via-link.txt").writeText("skip-me")
            val link = Files.createSymbolicLink(root.resolve("linked"), target).toFile()

            val store = FileStore(null)
            val files = store.fill(root.toFile(), emptyList())

            assertEquals(1, files.size)
            assertEquals(kept.absolutePath, files.single().absolutePath)
            assertTrue(files.none { it.name == "via-link.txt" })
            assertEquals(listOf(link.absolutePath), store.symbolicLinks.map { it.absolutePath })
        }

        test("ignored symbolic links are not collected when scanning") { directory ->
            val root = directory.resolve("scan-root").also { it.createDirectories() }
            val kept = root.resolve("kept.txt").also { it.writeText("ok") }.toFile()
            val target = directory.resolve("link-target").also { it.createDirectories() }
            val ignoredLink = Files.createSymbolicLink(root.resolve("ignored-link"), target).toFile()
            Files.createSymbolicLink(root.resolve("kept-link"), target)

            val store = FileStore(null)
            val files = store.fill(root.toFile(), listOf(ignoredLink.absolutePath))

            assertEquals(1, files.size)
            assertEquals(kept.absolutePath, files.single().absolutePath)
            assertEquals(listOf(root.resolve("kept-link").toFile().absolutePath), store.symbolicLinks.map { it.absolutePath })
        }

        test("empty directory reports one directory and no files or links") { directory ->
            val root = directory.resolve("empty").also { it.createDirectories() }.toFile()

            val store = FileStore(null)
            val files = store.fill(root, emptyList())

            assertTrue(files.isEmpty())
            assertEquals(1, store.numberOfDirectories)
            assertTrue(store.symbolicLinks.isEmpty())
        }

        test("nested files and folders are counted without following links") { directory ->
            val root = directory.resolve("scan-root").also { it.createDirectories() }
            root.resolve("a.txt").writeText("a")
            val nested = root.resolve("sub").also { it.createDirectories() }
            nested.resolve("b.txt").writeText("b")
            nested.resolve("deeper").createDirectories()
            val outside = directory.resolve("outside").also { it.createDirectories() }
            outside.resolve("via-link.txt").writeText("nope")
            Files.createSymbolicLink(root.resolve("linked"), outside)
            Files.createSymbolicLink(root.resolve("alias.txt"), Path.of("a.txt"))

            val store = FileStore(null)
            val files = store.fill(root.toFile(), emptyList())

            assertEquals(2, files.size)
            assertEquals(3, store.numberOfDirectories)
            assertEquals(2, store.symbolicLinks.size)
            assertTrue(files.none { it.name == "via-link.txt" })
        }

        test("dangling symbolic links are still collected") { directory ->
            val root = directory.resolve("scan-root").also { it.createDirectories() }
            val dangling = Files.createSymbolicLink(
                root.resolve("missing-link"),
                Path.of("does-not-exist"),
            ).toFile()

            val store = FileStore(null)
            val files = store.fill(root.toFile(), emptyList())

            assertTrue(files.isEmpty())
            assertEquals(listOf(dangling.absolutePath), store.symbolicLinks.map { it.absolutePath })
        }
    }

    test("fill with null file returns null") {
        val store = FileStore(null)
        assertNull(store.fill(null, emptyList()))
    }
}
