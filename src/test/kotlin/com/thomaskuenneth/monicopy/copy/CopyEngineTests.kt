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

import com.thomaskuenneth.monicopy.TestIoSizes
import com.thomaskuenneth.monicopy.temporaryDirectoryFixture
import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.testSuite
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.random.Random
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

val CopyEngineTests by testSuite(
    compartment = { TestCompartment.Sequential },
) {
    val copySizes = buildList {
        addAll(
            listOf(
                TestIoSizes.tiny,
                TestIoSizes.small,
                TestIoSizes.justUnder,
                TestIoSizes.exact,
                TestIoSizes.justOver,
                TestIoSizes.wellOver,
            ),
        )
        val random = Random(42)
        repeat(5) { add(random.nextInt(1, TestIoSizes.buffer * 2 + 1)) }
    }.distinct().sorted()

    temporaryDirectoryFixture().asParameterForEach {
        for (size in copySizes) {
            test("file of $size bytes is copied correctly") { directory ->
                val ws = CopyEngineWorkspace(directory)
                val content = ByteArray(size).also { SecureRandom().nextBytes(it) }
                val sourceFile = ws.source.resolve("payload.bin").also { it.writeBytes(content) }

                ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList())

                val copied = ws.dest.resolve("payload.bin")
                assertTrue(copied.isRegularFile())
                assertEquals(size.toLong(), Files.size(copied))
                assertContentEquals(content, copied.readBytes())
                assertContentEquals(content, sourceFile.readBytes())
            }
        }

        test("nested source tree is recreated under destination") { directory ->
            val ws = CopyEngineWorkspace(directory)
            val nested = ws.source.resolve("a").resolve("b").resolve("c").also { it.createDirectories() }
            val content = byteArrayOf(7, 8, 9)
            nested.resolve("deep.bin").writeBytes(content)

            ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList())

            val copied = ws.dest.resolve("a").resolve("b").resolve("c").resolve("deep.bin")
            assertTrue(copied.isRegularFile())
            assertContentEquals(content, copied.readBytes())
        }

        test("ignored source directories are not copied") { directory ->
            val ws = CopyEngineWorkspace(directory)
            ws.source.resolve("kept.txt").writeBytes(byteArrayOf(1))
            val ignored = ws.source.resolve("ignored").also { it.createDirectories() }
            ignored.resolve("secret.txt").writeBytes(byteArrayOf(2))

            ws.engine.copy(
                ws.source.toString(),
                ws.dest.toString(),
                listOf(ignored.toFile().absolutePath),
                            )

            assertTrue(ws.dest.resolve("kept.txt").isRegularFile())
            assertFalse(ws.dest.resolve("ignored").resolve("secret.txt").exists())
        }

        test("orphaned destination files are deleted without touching the source") { directory ->
            val ws = CopyEngineWorkspace(directory)
            val keepContent = byteArrayOf(1, 2, 3, 4)
            val sourceKeep = ws.source.resolve("keep.txt").also { it.writeBytes(keepContent) }
            ws.dest.resolve("keep.txt").writeBytes(keepContent)
            val orphan = ws.dest.resolve("orphan.txt").also { it.writeBytes(byteArrayOf(9, 9, 9)) }
            val sourceOnly = ws.source.resolve("only-in-source.txt").also { it.writeBytes(byteArrayOf(7)) }

            ws.engine.deleteOrphans(ws.source.toString(), ws.dest.toString(), emptyList())

            assertFalse(orphan.exists())
            assertTrue(ws.dest.resolve("keep.txt").isRegularFile())
            assertContentEquals(keepContent, sourceKeep.readBytes())
            assertTrue(sourceOnly.isRegularFile())
            assertContentEquals(byteArrayOf(7), sourceOnly.readBytes())
            assertFalse(ws.dest.resolve("only-in-source.txt").exists())
        }

        test("empty orphaned destination directories are pruned") { directory ->
            val ws = CopyEngineWorkspace(directory)
            ws.source.resolve("keep.txt").writeBytes(byteArrayOf(1))
            ws.dest.resolve("keep.txt").writeBytes(byteArrayOf(1))
            val orphanDir = ws.dest.resolve("empty-orphan").also { it.createDirectories() }
            orphanDir.resolve("nested").createDirectories()

            ws.engine.deleteOrphans(ws.source.toString(), ws.dest.toString(), emptyList())

            assertFalse(ws.dest.resolve("empty-orphan").exists())
            assertTrue(ws.dest.resolve("keep.txt").isRegularFile())
            assertTrue(ws.source.resolve("keep.txt").isRegularFile())
        }

        test("destination root is not deleted when it becomes empty") { directory ->
            val ws = CopyEngineWorkspace(directory)
            ws.dest.resolve("orphan.txt").writeBytes(byteArrayOf(9))
            val orphanDir = ws.dest.resolve("empty-orphan").also { it.createDirectories() }
            orphanDir.resolve("nested").createDirectories()

            ws.engine.deleteOrphans(ws.source.toString(), ws.dest.toString(), emptyList())

            assertTrue(ws.dest.exists())
            assertTrue(Files.isDirectory(ws.dest))
            assertFalse(ws.dest.resolve("orphan.txt").exists())
            assertFalse(ws.dest.resolve("empty-orphan").exists())
        }

        test("ignored destination directories are not scanned for orphans") { directory ->
            val ws = CopyEngineWorkspace(directory)
            ws.source.resolve("keep.txt").writeBytes(byteArrayOf(1))
            ws.dest.resolve("keep.txt").writeBytes(byteArrayOf(1))
            val ignored = ws.dest.resolve("ignored").also { it.createDirectories() }
            ignored.resolve("orphan.txt").writeBytes(byteArrayOf(9))

            ws.engine.deleteOrphans(
                ws.source.toString(),
                ws.dest.toString(),
                listOf(ignored.toFile().absolutePath),
                            )

            assertTrue(ignored.resolve("orphan.txt").isRegularFile())
            assertTrue(ws.dest.resolve("keep.txt").isRegularFile())
        }

        test("orphaned destination symbolic links are deleted") { directory ->
            val ws = CopyEngineWorkspace(directory)
            ws.source.resolve("keep.txt").writeBytes(byteArrayOf(1))
            ws.dest.resolve("keep.txt").writeBytes(byteArrayOf(1))
            val target = ws.root.resolve("link-target").also { it.createDirectories() }
            target.resolve("payload.bin").writeBytes(byteArrayOf(9))
            Files.createSymbolicLink(ws.dest.resolve("orphan-link"), target)

            ws.engine.deleteOrphans(ws.source.toString(), ws.dest.toString(), emptyList())

            assertFalse(Files.exists(ws.dest.resolve("orphan-link"), LinkOption.NOFOLLOW_LINKS))
            assertTrue(target.resolve("payload.bin").isRegularFile())
            assertTrue(ws.dest.resolve("keep.txt").isRegularFile())
        }

        test("matching destination symbolic links are kept") { directory ->
            val ws = CopyEngineWorkspace(directory)
            val target = ws.root.resolve("link-target").also { it.createDirectories() }
            val sourceLink = Files.createSymbolicLink(ws.source.resolve("kept-link"), target)
            Files.createSymbolicLink(ws.dest.resolve("kept-link"), target)

            ws.engine.deleteOrphans(ws.source.toString(), ws.dest.toString(), emptyList())

            val destLink = ws.dest.resolve("kept-link")
            assertTrue(Files.isSymbolicLink(destLink))
            assertEquals(Files.readSymbolicLink(sourceLink), Files.readSymbolicLink(destLink))
        }

        test("directory symbolic links are not followed when pruning orphan folders") { directory ->
            val ws = CopyEngineWorkspace(directory)
            ws.source.resolve("keep.txt").writeBytes(byteArrayOf(1))
            ws.dest.resolve("keep.txt").writeBytes(byteArrayOf(1))
            val outside = ws.root.resolve("outside").also { it.createDirectories() }
            val outsideChild = outside.resolve("must-survive").also { it.createDirectories() }
            outsideChild.resolve("nested").createDirectories()
            Files.createSymbolicLink(ws.dest.resolve("trap"), outside)

            ws.engine.deleteOrphans(ws.source.toString(), ws.dest.toString(), emptyList())

            assertFalse(Files.exists(ws.dest.resolve("trap"), LinkOption.NOFOLLOW_LINKS))
            assertTrue(outsideChild.exists())
            assertTrue(outsideChild.resolve("nested").exists())
        }

        test("symbolic links are remembered and preserved when enabled") { directory ->
            val ws = CopyEngineWorkspace(directory)
            val targetDir = ws.root.resolve("link-target").also { it.createDirectories() }
            targetDir.resolve("via-link.txt").writeBytes(byteArrayOf(42, 17, 99))
            val link = Files.createSymbolicLink(ws.source.resolve("linked-dir"), targetDir)
            ws.source.resolve("regular.txt").writeBytes(byteArrayOf(1, 2, 3))

            ws.engine.copy(
                ws.source.toString(),
                ws.dest.toString(),
                emptyList(),
                                onProgress = {},
                onCounts = { _, _ -> },
                onCopyDecision = {},
                preserveSymbolicLinks = true,
            )

            val destLink = ws.dest.resolve("linked-dir")
            assertEquals(
                listOf(link.toAbsolutePath().toString()),
                ws.engine.rememberedSymbolicLinks.map { it.absolutePath },
            )
            assertTrue(Files.isSymbolicLink(destLink))
            assertEquals(Files.readSymbolicLink(link), Files.readSymbolicLink(destLink))
            assertContentEquals(byteArrayOf(1, 2, 3), ws.dest.resolve("regular.txt").readBytes())
            assertTrue(targetDir.resolve("via-link.txt").isRegularFile())
        }

        test("symbolic links are remembered but not preserved when disabled") { directory ->
            val ws = CopyEngineWorkspace(directory)
            val targetDir = ws.root.resolve("link-target").also { it.createDirectories() }
            targetDir.resolve("via-link.txt").writeBytes(byteArrayOf(42, 17, 99))
            val link = Files.createSymbolicLink(ws.source.resolve("linked-dir"), targetDir)
            ws.source.resolve("regular.txt").writeBytes(byteArrayOf(1, 2, 3))

            ws.engine.copy(
                ws.source.toString(),
                ws.dest.toString(),
                emptyList(),
                                onProgress = {},
                onCounts = { _, _ -> },
                onCopyDecision = {},
                preserveSymbolicLinks = false,
            )

            assertEquals(
                listOf(link.toAbsolutePath().toString()),
                ws.engine.rememberedSymbolicLinks.map { it.absolutePath },
            )
            assertFalse(Files.exists(ws.dest.resolve("linked-dir"), LinkOption.NOFOLLOW_LINKS))
            assertContentEquals(byteArrayOf(1, 2, 3), ws.dest.resolve("regular.txt").readBytes())
        }

        test("relative symbolic link targets are preserved as-is") { directory ->
            val ws = CopyEngineWorkspace(directory)
            ws.source.resolve("target.txt").writeBytes(byteArrayOf(1, 2, 3))
            val relativeTarget = Path.of("target.txt")
            val link = Files.createSymbolicLink(ws.source.resolve("alias.txt"), relativeTarget)

            ws.engine.copy(
                ws.source.toString(),
                ws.dest.toString(),
                emptyList(),
                                onProgress = {},
                onCounts = { _, _ -> },
                onCopyDecision = {},
                preserveSymbolicLinks = true,
            )

            val destLink = ws.dest.resolve("alias.txt")
            assertTrue(Files.isSymbolicLink(destLink))
            assertEquals(relativeTarget, Files.readSymbolicLink(link))
            assertEquals(relativeTarget, Files.readSymbolicLink(destLink))
            assertContentEquals(byteArrayOf(1, 2, 3), ws.dest.resolve("target.txt").readBytes())
        }

        test("file symbolic links are preserved by the engine") { directory ->
            val ws = CopyEngineWorkspace(directory)
            val nested = ws.source.resolve("nested").also { it.createDirectories() }
            val target = ws.root.resolve("payload.bin").also { it.writeBytes(byteArrayOf(7, 8, 9)) }
            val link = Files.createSymbolicLink(nested.resolve("alias.bin"), target)

            ws.engine.copy(
                ws.source.toString(),
                ws.dest.toString(),
                emptyList(),
                                onProgress = {},
                onCounts = { _, _ -> },
                onCopyDecision = {},
                preserveSymbolicLinks = true,
            )

            val destLink = ws.dest.resolve("nested/alias.bin")
            assertTrue(Files.isSymbolicLink(destLink))
            assertEquals(Files.readSymbolicLink(link), Files.readSymbolicLink(destLink))
        }

        test("ignored symbolic links are not preserved to the destination") { directory ->
            val ws = CopyEngineWorkspace(directory)
            val target = ws.root.resolve("link-target").also { it.createDirectories() }
            val ignoredLink = Files.createSymbolicLink(ws.source.resolve("ignored-link"), target)
            Files.createSymbolicLink(ws.source.resolve("kept-link"), target)
            ws.source.resolve("regular.txt").writeBytes(byteArrayOf(1))

            ws.engine.copy(
                ws.source.toString(),
                ws.dest.toString(),
                listOf(ignoredLink.toAbsolutePath().toString()),
                                onProgress = {},
                onCounts = { _, _ -> },
                onCopyDecision = {},
                preserveSymbolicLinks = true,
            )

            assertFalse(Files.exists(ws.dest.resolve("ignored-link"), LinkOption.NOFOLLOW_LINKS))
            assertTrue(Files.isSymbolicLink(ws.dest.resolve("kept-link")))
            assertContentEquals(byteArrayOf(1), ws.dest.resolve("regular.txt").readBytes())
        }

        test("preserved symbolic link is removed by orphan deletion after source link is gone") { directory ->
            val ws = CopyEngineWorkspace(directory)
            val target = ws.root.resolve("link-target").also { it.createDirectories() }
            val sourceLink = Files.createSymbolicLink(ws.source.resolve("linked"), target)
            ws.source.resolve("keep.txt").writeBytes(byteArrayOf(1))

            ws.engine.copy(
                ws.source.toString(),
                ws.dest.toString(),
                emptyList(),
                                onProgress = {},
                onCounts = { _, _ -> },
                onCopyDecision = {},
                preserveSymbolicLinks = true,
            )
            assertTrue(Files.isSymbolicLink(ws.dest.resolve("linked")))

            Files.delete(sourceLink)
            ws.engine.deleteOrphans(ws.source.toString(), ws.dest.toString(), emptyList())

            assertFalse(Files.exists(ws.dest.resolve("linked"), LinkOption.NOFOLLOW_LINKS))
            assertTrue(ws.dest.resolve("keep.txt").isRegularFile())
            assertTrue(target.exists())
        }

        test("preserves a symbolic link by replacing an existing destination file") { directory ->
            val ws = CopyEngineWorkspace(directory)
            val target = ws.root.resolve("link-target").also { it.createDirectories() }
            val link = Files.createSymbolicLink(ws.source.resolve("linked"), target)
            ws.dest.resolve("linked").writeBytes(byteArrayOf(7, 8, 9))

            ws.engine.copy(
                ws.source.toString(),
                ws.dest.toString(),
                emptyList(),
                                onProgress = {},
                onCounts = { _, _ -> },
                onCopyDecision = {},
                preserveSymbolicLinks = true,
            )

            val destLink = ws.dest.resolve("linked")
            assertTrue(Files.isSymbolicLink(destLink))
            assertEquals(Files.readSymbolicLink(link), Files.readSymbolicLink(destLink))
            assertFalse(Files.isRegularFile(destLink, LinkOption.NOFOLLOW_LINKS))
        }

        test("preserves symbolic links when the source contains only links") { directory ->
            val ws = CopyEngineWorkspace(directory)
            val target = ws.root.resolve("link-target").also { it.createDirectories() }
            val dirLink = Files.createSymbolicLink(ws.source.resolve("linked-dir"), target)
            val fileLink = Files.createSymbolicLink(ws.source.resolve("alias.txt"), Path.of("missing.txt"))
            var fileCount: Long? = null
            var subfolderCount: Long? = null

            ws.engine.copy(
                ws.source.toString(),
                ws.dest.toString(),
                emptyList(),
                                onProgress = {},
                onCounts = { files, folders ->
                    fileCount = files
                    subfolderCount = folders
                },
                onCopyDecision = {},
                preserveSymbolicLinks = true,
            )

            assertEquals(0L, fileCount)
            assertEquals(0L, subfolderCount)
            assertEquals(2, ws.engine.rememberedSymbolicLinks.size)
            assertTrue(Files.isSymbolicLink(ws.dest.resolve("linked-dir")))
            assertEquals(Files.readSymbolicLink(dirLink), Files.readSymbolicLink(ws.dest.resolve("linked-dir")))
            assertTrue(Files.isSymbolicLink(ws.dest.resolve("alias.txt")))
            assertEquals(Files.readSymbolicLink(fileLink), Files.readSymbolicLink(ws.dest.resolve("alias.txt")))
            assertFalse(Files.exists(ws.dest.resolve("alias.txt")))
        }

        test("zero-byte files are copied") { directory ->
            val ws = CopyEngineWorkspace(directory)
            ws.source.resolve("empty.bin").writeBytes(byteArrayOf())

            ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList())

            val copied = ws.dest.resolve("empty.bin")
            assertTrue(copied.isRegularFile())
            assertEquals(0L, Files.size(copied))
            assertContentEquals(byteArrayOf(), copied.readBytes())
        }

        test("dangling symbolic links are preserved by the engine") { directory ->
            val ws = CopyEngineWorkspace(directory)
            val relativeTarget = Path.of("does-not-exist")
            Files.createSymbolicLink(ws.source.resolve("dangling"), relativeTarget)
            ws.source.resolve("keep.txt").writeBytes(byteArrayOf(1))

            ws.engine.copy(
                ws.source.toString(),
                ws.dest.toString(),
                emptyList(),
                                onProgress = {},
                onCounts = { _, _ -> },
                onCopyDecision = {},
                preserveSymbolicLinks = true,
            )

            val destLink = ws.dest.resolve("dangling")
            assertTrue(Files.isSymbolicLink(destLink))
            assertEquals(relativeTarget, Files.readSymbolicLink(destLink))
            assertFalse(Files.exists(destLink))
            assertTrue(ws.dest.resolve("keep.txt").isRegularFile())
        }

        test("destination symbolic link is kept when a source entry exists even if targets differ") { directory ->
            val ws = CopyEngineWorkspace(directory)
            val sourceTarget = ws.root.resolve("source-target").also { it.createDirectories() }
            val destTarget = ws.root.resolve("dest-target").also { it.createDirectories() }
            Files.createSymbolicLink(ws.source.resolve("shared-link"), sourceTarget)
            Files.createSymbolicLink(ws.dest.resolve("shared-link"), destTarget)

            ws.engine.deleteOrphans(ws.source.toString(), ws.dest.toString(), emptyList())

            val destLink = ws.dest.resolve("shared-link")
            assertTrue(Files.isSymbolicLink(destLink))
            assertEquals(destTarget, Files.readSymbolicLink(destLink))
            assertTrue(sourceTarget.exists())
            assertTrue(destTarget.exists())
        }

        test("destination file is kept when a source directory exists at the same relative path") { directory ->
            val ws = CopyEngineWorkspace(directory)
            ws.source.resolve("same-name").createDirectories()
            ws.dest.resolve("same-name").writeBytes(byteArrayOf(1, 2, 3))

            ws.engine.deleteOrphans(ws.source.toString(), ws.dest.toString(), emptyList())

            assertTrue(ws.dest.resolve("same-name").isRegularFile())
            assertContentEquals(byteArrayOf(1, 2, 3), ws.dest.resolve("same-name").readBytes())
            assertTrue(Files.isDirectory(ws.source.resolve("same-name")))
        }

        test("cancel while paused stops deleting orphans") { directory ->
            val ws = CopyEngineWorkspace(directory)
            ws.dest.resolve("orphan-a.txt").writeBytes(byteArrayOf(1))
            ws.dest.resolve("orphan-b.txt").writeBytes(byteArrayOf(2))

            val state = AtomicReference(CopyState.DELETE_PAUSED)
            ws.engine.copyStateProvider = { state.get() }
            val done = CountDownLatch(1)
            val worker = thread(name = "delete-cancel-test", isDaemon = true) {
                try {
                    ws.engine.deleteOrphans(ws.source.toString(), ws.dest.toString(), emptyList())
                } finally {
                    done.countDown()
                }
            }

            try {
                assertTrue(awaitThreadState(worker, Thread.State.WAITING))
                ws.engine.cancel()
                assertTrue(done.await(30, TimeUnit.SECONDS))
                assertTrue(ws.dest.resolve("orphan-a.txt").exists())
                assertTrue(ws.dest.resolve("orphan-b.txt").exists())
            } finally {
                ws.engine.resume()
            }
        }

        test("delete orphans waits while paused and completes after resume") { directory ->
            val ws = CopyEngineWorkspace(directory)
            ws.source.resolve("keep.txt").writeBytes(byteArrayOf(1))
            ws.dest.resolve("keep.txt").writeBytes(byteArrayOf(1))
            ws.dest.resolve("orphan-a.txt").writeBytes(byteArrayOf(9))
            ws.dest.resolve("orphan-b.txt").writeBytes(byteArrayOf(8))

            val state = AtomicReference(CopyState.DELETE_PAUSED)
            ws.engine.copyStateProvider = { state.get() }
            val done = CountDownLatch(1)
            val worker = thread(name = "delete-pause-test", isDaemon = true) {
                try {
                    ws.engine.deleteOrphans(ws.source.toString(), ws.dest.toString(), emptyList())
                } finally {
                    done.countDown()
                }
            }

            try {
                assertTrue(awaitThreadState(worker, Thread.State.WAITING))
                assertTrue(ws.dest.resolve("orphan-a.txt").exists())
                assertTrue(ws.dest.resolve("orphan-b.txt").exists())

                state.set(CopyState.DELETING)
                ws.engine.resume()
                assertTrue(done.await(30, TimeUnit.SECONDS))
                assertFalse(ws.dest.resolve("orphan-a.txt").exists())
                assertFalse(ws.dest.resolve("orphan-b.txt").exists())
                assertTrue(ws.dest.resolve("keep.txt").isRegularFile())
            } finally {
                state.set(CopyState.DELETING)
                ws.engine.resume()
            }
        }

        test("mustBeCopied skips when size and modification time match") { directory ->
            val ws = CopyEngineWorkspace(directory)
            val sourceFile = ws.source.resolve("same.bin").also { it.writeBytes(byteArrayOf(1, 2, 3, 4, 5)) }
            val destFile = ws.dest.resolve("same.bin").also { it.writeBytes(byteArrayOf(9, 8, 7, 6, 5)) }
            val mtime = sourceFile.toFile().lastModified()
            destFile.toFile().setLastModified(mtime)
            val checksumBefore = md5Hex(destFile)

            ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList())

            assertEquals(checksumBefore, md5Hex(destFile))
            assertEquals(mtime, destFile.toFile().lastModified())
        }

        test("mustBeCopied recopies when sizes differ") { directory ->
            val ws = CopyEngineWorkspace(directory)
            ws.source.resolve("sized.bin").writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6))
            ws.dest.resolve("sized.bin").writeBytes(byteArrayOf(1, 2))

            ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList())

            assertContentEquals(byteArrayOf(1, 2, 3, 4, 5, 6), ws.dest.resolve("sized.bin").readBytes())
        }

        test("mustBeCopied skips content rewrite when MD5 matches despite different mtime") { directory ->
            val ws = CopyEngineWorkspace(directory)
            val content = ByteArray(TestIoSizes.fitsComfortably).also { SecureRandom().nextBytes(it) }
            val sourceFile = ws.source.resolve("hash.bin").also { it.writeBytes(content) }
            val destFile = ws.dest.resolve("hash.bin").also { it.writeBytes(content) }
            destFile.toFile().setLastModified(sourceFile.toFile().lastModified() - 60_000)
            val checksumBefore = md5Hex(destFile)

            ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList())

            assertEquals(checksumBefore, md5Hex(destFile))
            assertEquals(sourceFile.toFile().lastModified(), destFile.toFile().lastModified())
        }

        test("mustBeCopied recopies when MD5 differs with same size and different mtime") { directory ->
            val ws = CopyEngineWorkspace(directory)
            val sourceContent = ByteArray(TestIoSizes.fitsModerately) { 1 }
            val destContent = ByteArray(TestIoSizes.fitsModerately) { 2 }
            val sourceFile = ws.source.resolve("diff.bin").also { it.writeBytes(sourceContent) }
            val destFile = ws.dest.resolve("diff.bin").also { it.writeBytes(destContent) }
            destFile.toFile().setLastModified(sourceFile.toFile().lastModified() - 60_000)

            ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList())

            assertContentEquals(sourceContent, destFile.readBytes())
        }

        test("engine recopies from the MD5 buffer when the source fits in memory") { directory ->
            val ws = CopyEngineWorkspace(directory)
            val sourceContent = ByteArray(TestIoSizes.fitsEasily).also { SecureRandom().nextBytes(it) }
            val destContent = ByteArray(sourceContent.size) { 0 }
            val sourceFile = ws.source.resolve("buffered.bin").also { it.writeBytes(sourceContent) }
            val destFile = ws.dest.resolve("buffered.bin").also { it.writeBytes(destContent) }
            destFile.toFile().setLastModified(sourceFile.toFile().lastModified() - 60_000)
            assertTrue(sourceContent.size <= TestIoSizes.buffer)

            ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList())

            assertContentEquals(sourceContent, destFile.readBytes())
            assertEquals(sourceFile.toFile().lastModified(), destFile.toFile().lastModified())
        }

        test("mustBeCopied streams a recopy when MD5 differs and source exceeds the I/O buffer") { directory ->
            val ws = CopyEngineWorkspace(directory)
            val size = TestIoSizes.justOver
            val sourceContent = ByteArray(size).also { SecureRandom().nextBytes(it) }
            val destContent = ByteArray(size).also { SecureRandom().nextBytes(it) }
            val sourceFile = ws.source.resolve("stream-diff.bin").also { it.writeBytes(sourceContent) }
            val destFile = ws.dest.resolve("stream-diff.bin").also { it.writeBytes(destContent) }
            destFile.toFile().setLastModified(sourceFile.toFile().lastModified() - 60_000)

            ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList())

            assertContentEquals(sourceContent, destFile.readBytes())
            assertEquals(sourceFile.toFile().lastModified(), destFile.toFile().lastModified())
        }

        test("mustBeCopied skips rewrite when MD5 matches for a source larger than the I/O buffer") { directory ->
            val ws = CopyEngineWorkspace(directory)
            val size = TestIoSizes.justOver
            val content = ByteArray(size).also { SecureRandom().nextBytes(it) }
            val sourceFile = ws.source.resolve("stream-same.bin").also { it.writeBytes(content) }
            val destFile = ws.dest.resolve("stream-same.bin").also { it.writeBytes(content) }
            destFile.toFile().setLastModified(sourceFile.toFile().lastModified() - 60_000)
            val checksumBefore = md5Hex(destFile)

            ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList())

            assertEquals(checksumBefore, md5Hex(destFile))
            assertEquals(sourceFile.toFile().lastModified(), destFile.toFile().lastModified())
        }

        test("copy waits while paused and completes after resume") { directory ->
            val ws = CopyEngineWorkspace(directory)
            ws.source.resolve("a.bin").writeBytes(byteArrayOf(1))
            ws.source.resolve("b.bin").writeBytes(byteArrayOf(2))

            val state = AtomicReference(CopyState.COPY_PAUSED)
            ws.engine.copyStateProvider = { state.get() }
            val done = CountDownLatch(1)
            val worker = thread(name = "copy-pause-test", isDaemon = true) {
                try {
                    ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList())
                } finally {
                    done.countDown()
                }
            }

            try {
                assertTrue(awaitThreadState(worker, Thread.State.WAITING))
                assertFalse(ws.dest.resolve("a.bin").exists())
                assertFalse(ws.dest.resolve("b.bin").exists())

                state.set(CopyState.COPYING)
                ws.engine.resume()
                assertTrue(done.await(30, TimeUnit.SECONDS))
                assertTrue(ws.dest.resolve("a.bin").isRegularFile())
                assertTrue(ws.dest.resolve("b.bin").isRegularFile())
            } finally {
                state.set(CopyState.COPYING)
                ws.engine.resume()
            }
        }

        test("cancel while paused stops copying") { directory ->
            val ws = CopyEngineWorkspace(directory)
            ws.source.resolve("a.bin").writeBytes(byteArrayOf(1))
            ws.source.resolve("b.bin").writeBytes(byteArrayOf(2))

            val state = AtomicReference(CopyState.COPY_PAUSED)
            ws.engine.copyStateProvider = { state.get() }
            val done = CountDownLatch(1)
            val worker = thread(name = "copy-cancel-test", isDaemon = true) {
                try {
                    ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList())
                } finally {
                    done.countDown()
                }
            }

            try {
                assertTrue(awaitThreadState(worker, Thread.State.WAITING))
                ws.engine.cancel()
                assertTrue(done.await(30, TimeUnit.SECONDS))
                assertFalse(ws.dest.resolve("a.bin").exists())
                assertFalse(ws.dest.resolve("b.bin").exists())
            } finally {
                ws.engine.resume()
            }
        }

        test("copy still finishes when file scan returns null") { directory ->
            val ws = CopyEngineWorkspace(directory)
            ws.installNullFillStore()
            val progress = mutableListOf<Int>()
            var fileCount: Long? = null
            var subfolderCount: Long? = null

            ws.engine.copy(
                ws.source.toString(),
                ws.dest.toString(),
                emptyList(),
                progress::add,
                { files, folders ->
                    fileCount = files
                    subfolderCount = folders
                },
                onCopyDecision = {},
            )

            assertEquals(0L, fileCount)
            assertEquals(0L, subfolderCount)
            assertEquals(listOf(100), progress)
        }

        test("onCopyDecision reports copied and skipped files") { directory ->
            val ws = CopyEngineWorkspace(directory)
            val sourceFile = ws.source.resolve("same.bin").also { it.writeBytes(byteArrayOf(1, 2, 3, 4, 5)) }
            val destFile = ws.dest.resolve("same.bin").also { it.writeBytes(byteArrayOf(9, 8, 7, 6, 5)) }
            destFile.toFile().setLastModified(sourceFile.toFile().lastModified())
            ws.source.resolve("new.bin").writeBytes(byteArrayOf(7, 8, 9))

            val decisions = mutableListOf<Boolean>()
            ws.engine.copy(
                ws.source.toString(),
                ws.dest.toString(),
                emptyList(),
                                onProgress = {},
                onCounts = { _, _ -> },
                onCopyDecision = decisions::add,
            )

            assertEquals(1, decisions.count { it })
            assertEquals(1, decisions.count { !it })
        }

        test("deleteOrphans still finishes when file scan returns null") { directory ->
            val ws = CopyEngineWorkspace(directory)
            ws.installNullFillStore()
            ws.engine.copyStateProvider = { CopyState.DELETING }
            val progress = mutableListOf<Int>()

            ws.engine.deleteOrphans(
                ws.source.toString(),
                ws.dest.toString(),
                emptyList(),
                progress::add,
            )

            assertEquals(listOf(100), progress)
        }
    }
}

private fun md5Hex(path: Path): String {
    val digest = MessageDigest.getInstance("MD5")
    digest.update(path.readBytes())
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}

private fun awaitThreadState(thread: Thread, expected: Thread.State, timeoutMs: Long = 10_000): Boolean {
    val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
    while (System.nanoTime() < deadline) {
        if (thread.state == expected) return true
        Thread.sleep(10)
    }
    return thread.state == expected
}
