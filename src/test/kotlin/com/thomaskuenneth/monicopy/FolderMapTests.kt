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

import de.infix.testBalloon.framework.core.testSuite
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

val FolderMapTests by testSuite {
    temporaryDirectoryFixture().asParameterForEach {
        test("does not follow directory symbolic links") { directory ->
            val root = directory.resolve("root").also { it.createDirectories() }
            val real = root.resolve("real").also { it.createDirectories() }
            val child = real.resolve("child").also { it.createDirectories() }
            val outside = directory.resolve("outside").also { it.createDirectories() }
            val secret = outside.resolve("secret").also { it.createDirectories() }
            Files.createSymbolicLink(root.resolve("linked"), outside)

            val map = FolderMap()
            map.fill(root.toFile())
            val folders = map.iterator.asSequence().map { it.toPath().toAbsolutePath().normalize() }.toSet()

            assertEquals(
                setOf(
                    root.toAbsolutePath().normalize(),
                    real.toAbsolutePath().normalize(),
                    child.toAbsolutePath().normalize(),
                ),
                folders,
            )
            assertFalse(folders.contains(outside.toAbsolutePath().normalize()))
            assertFalse(folders.contains(secret.toAbsolutePath().normalize()))
            assertTrue(Files.isSymbolicLink(root.resolve("linked")))
        }

        test("iterator yields deepest folders first") { directory ->
            val root = directory.resolve("root").also { it.createDirectories() }
            val mid = root.resolve("mid").also { it.createDirectories() }
            val leaf = mid.resolve("leaf").also { it.createDirectories() }

            val map = FolderMap()
            map.fill(root.toFile())
            val folders = map.iterator.asSequence().map { it.toPath().toAbsolutePath().normalize() }.toList()

            assertEquals(
                listOf(
                    leaf.toAbsolutePath().normalize(),
                    mid.toAbsolutePath().normalize(),
                    root.toAbsolutePath().normalize(),
                ),
                folders,
            )
        }

        test("fill clears previous contents") { directory ->
            val first = directory.resolve("first").also { it.createDirectories() }
            first.resolve("a").createDirectories()
            val second = directory.resolve("second").also { it.createDirectories() }
            second.resolve("b").createDirectories()

            val map = FolderMap()
            map.fill(first.toFile())
            map.fill(second.toFile())
            val folders = map.iterator.asSequence().map { it.toPath().toAbsolutePath().normalize() }.toSet()

            assertEquals(
                setOf(
                    second.toAbsolutePath().normalize(),
                    second.resolve("b").toAbsolutePath().normalize(),
                ),
                folders,
            )
        }

        test("regular files are not collected as folders") { directory ->
            val root = directory.resolve("root").also { it.createDirectories() }
            root.resolve("file.txt").writeBytes(byteArrayOf(1))

            val map = FolderMap()
            map.fill(root.toFile())
            val folders = map.iterator.asSequence().map { it.toPath().toAbsolutePath().normalize() }.toSet()

            assertEquals(setOf(root.toAbsolutePath().normalize()), folders)
        }
    }
}
