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
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

val SymbolicLinkPreserverTests by testSuite {
    temporaryDirectoryFixture().asParameterForEach {
        test("preserves a relative symbolic link target as-is") { directory ->
            val from = directory.resolve("from").also { it.createDirectories() }
            val to = directory.resolve("to").also { it.createDirectories() }
            from.resolve("target.txt").writeBytes(byteArrayOf(1, 2, 3))
            val relativeTarget = Path.of("target.txt")
            val link = Files.createSymbolicLink(from.resolve("alias.txt"), relativeTarget)

            SymbolicLinkPreserver().preserve(
                links = listOf(link.toFile()),
                fromRoot = from.toFile(),
                toRoot = to.toFile(),
                onError = { _, _ -> error("unexpected error") },
            )

            val destLink = to.resolve("alias.txt")
            assertTrue(Files.isSymbolicLink(destLink))
            assertEquals(relativeTarget, Files.readSymbolicLink(destLink))
        }

        test("preserves a directory symbolic link without following it") { directory ->
            val from = directory.resolve("from").also { it.createDirectories() }
            val to = directory.resolve("to").also { it.createDirectories() }
            val target = directory.resolve("target").also { it.createDirectories() }
            target.resolve("payload.bin").writeBytes(byteArrayOf(1, 2, 3))
            val link = Files.createSymbolicLink(from.resolve("linked"), target)

            val errors = mutableListOf<Pair<String, String>>()
            SymbolicLinkPreserver().preserve(
                links = listOf(link.toFile()),
                fromRoot = from.toFile(),
                toRoot = to.toFile(),
                onError = { source, message -> errors += source.absolutePath to message },
            )

            val destLink = to.resolve("linked")
            assertTrue(errors.isEmpty())
            assertTrue(Files.isSymbolicLink(destLink))
            assertEquals(Files.readSymbolicLink(link), Files.readSymbolicLink(destLink))
        }

        test("preserves a file symbolic link under a nested path") { directory ->
            val from = directory.resolve("from").also { it.createDirectories() }
            val to = directory.resolve("to").also { it.createDirectories() }
            val nested = from.resolve("nested").also { it.createDirectories() }
            val target = directory.resolve("target.txt").also { it.writeBytes(byteArrayOf(9)) }
            val link = Files.createSymbolicLink(nested.resolve("alias.txt"), target)

            SymbolicLinkPreserver().preserve(
                links = listOf(link.toFile()),
                fromRoot = from.toFile(),
                toRoot = to.toFile(),
                onError = { _, _ -> error("unexpected error") },
            )

            val destLink = to.resolve("nested/alias.txt")
            assertTrue(Files.isSymbolicLink(destLink))
            assertEquals(Files.readSymbolicLink(link), Files.readSymbolicLink(destLink))
        }

        test("replaces an existing destination entry") { directory ->
            val from = directory.resolve("from").also { it.createDirectories() }
            val to = directory.resolve("to").also { it.createDirectories() }
            val target = directory.resolve("target").also { it.createDirectories() }
            val link = Files.createSymbolicLink(from.resolve("linked"), target)
            to.resolve("linked").writeBytes(byteArrayOf(7))

            SymbolicLinkPreserver().preserve(
                links = listOf(link.toFile()),
                fromRoot = from.toFile(),
                toRoot = to.toFile(),
                onError = { _, _ -> error("unexpected error") },
            )

            assertTrue(Files.isSymbolicLink(to.resolve("linked")))
        }

        test("reports an error when the source is not a symbolic link") { directory ->
            val from = directory.resolve("from").also { it.createDirectories() }
            val to = directory.resolve("to").also { it.createDirectories() }
            val regular = from.resolve("regular.txt").also { it.writeBytes(byteArrayOf(1)) }

            val errors = mutableListOf<String>()
            SymbolicLinkPreserver().preserve(
                links = listOf(regular.toFile()),
                fromRoot = from.toFile(),
                toRoot = to.toFile(),
                onError = { source, _ -> errors += source.name },
            )

            assertEquals(listOf("regular.txt"), errors)
            assertFalse(Files.exists(to.resolve("regular.txt"), LinkOption.NOFOLLOW_LINKS))
        }

        test("preserves a dangling symbolic link without requiring the target") { directory ->
            val from = directory.resolve("from").also { it.createDirectories() }
            val to = directory.resolve("to").also { it.createDirectories() }
            val relativeTarget = Path.of("does-not-exist")
            val link = Files.createSymbolicLink(from.resolve("dangling"), relativeTarget)

            SymbolicLinkPreserver().preserve(
                links = listOf(link.toFile()),
                fromRoot = from.toFile(),
                toRoot = to.toFile(),
                onError = { _, _ -> error("unexpected error") },
            )

            val destLink = to.resolve("dangling")
            assertTrue(Files.isSymbolicLink(destLink))
            assertEquals(relativeTarget, Files.readSymbolicLink(destLink))
            assertFalse(Files.exists(destLink))
        }
    }
}
