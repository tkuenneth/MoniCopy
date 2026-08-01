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

import com.thomaskuenneth.monicopy.javaTemporaryDirectoryFixture
import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.testSuite
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

val CopyEngineTests by testSuite(
    compartment = {
        TestCompartment.Sequential
    },
) {
    val workspace = javaTemporaryDirectoryFixture()
    val harness = testFixture {
        CopyEngineWorkspace(workspace())
    }

    test("random-sized file with random content is copied correctly") {
        val ws = harness().also { it.resetTrees() }
        val size = SecureRandom().nextInt(1, 256 * 1024)
        val content = ByteArray(size).also { SecureRandom().nextBytes(it) }
        val sourceFile = ws.source.resolve("payload.bin").also { it.writeBytes(content) }

        ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList(), ws::ignoreMessages)

        val copied = ws.dest.resolve("payload.bin")
        assertTrue(copied.isRegularFile())
        assertEquals(size.toLong(), Files.size(copied))
        assertContentEquals(content, copied.readBytes())
        assertContentEquals(content, sourceFile.readBytes())
    }

    test("nested source tree is recreated under destination") {
        val ws = harness().also { it.resetTrees() }
        val nested = ws.source.resolve("a").resolve("b").resolve("c").also { it.createDirectories() }
        val content = byteArrayOf(7, 8, 9)
        nested.resolve("deep.bin").writeBytes(content)

        ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList(), ws::ignoreMessages)

        val copied = ws.dest.resolve("a").resolve("b").resolve("c").resolve("deep.bin")
        assertTrue(copied.isRegularFile())
        assertContentEquals(content, copied.readBytes())
    }

    test("ignored source directories are not copied") {
        val ws = harness().also { it.resetTrees() }
        ws.source.resolve("kept.txt").writeBytes(byteArrayOf(1))
        val ignored = ws.source.resolve("ignored").also { it.createDirectories() }
        ignored.resolve("secret.txt").writeBytes(byteArrayOf(2))

        ws.engine.copy(
            ws.source.toString(),
            ws.dest.toString(),
            listOf(ignored.toFile().absolutePath),
            ws::ignoreMessages,
        )

        assertTrue(ws.dest.resolve("kept.txt").isRegularFile())
        assertFalse(ws.dest.resolve("ignored").resolve("secret.txt").exists())
    }

    test("orphaned destination files are deleted without touching the source") {
        val ws = harness().also { it.resetTrees() }
        val keepContent = byteArrayOf(1, 2, 3, 4)
        val sourceKeep = ws.source.resolve("keep.txt").also { it.writeBytes(keepContent) }
        ws.dest.resolve("keep.txt").writeBytes(keepContent)
        val orphan = ws.dest.resolve("orphan.txt").also { it.writeBytes(byteArrayOf(9, 9, 9)) }
        val sourceOnly = ws.source.resolve("only-in-source.txt").also { it.writeBytes(byteArrayOf(7)) }

        ws.engine.deleteOrphans(ws.source.toString(), ws.dest.toString(), emptyList(), ws::ignoreMessages)

        assertFalse(orphan.exists())
        assertTrue(ws.dest.resolve("keep.txt").isRegularFile())
        assertContentEquals(keepContent, sourceKeep.readBytes())
        assertTrue(sourceOnly.isRegularFile())
        assertContentEquals(byteArrayOf(7), sourceOnly.readBytes())
        assertFalse(ws.dest.resolve("only-in-source.txt").exists())
    }

    test("empty orphaned destination directories are pruned") {
        val ws = harness().also { it.resetTrees() }
        ws.source.resolve("keep.txt").writeBytes(byteArrayOf(1))
        ws.dest.resolve("keep.txt").writeBytes(byteArrayOf(1))
        val orphanDir = ws.dest.resolve("empty-orphan").also { it.createDirectories() }
        orphanDir.resolve("nested").createDirectories()

        ws.engine.deleteOrphans(ws.source.toString(), ws.dest.toString(), emptyList(), ws::ignoreMessages)

        assertFalse(ws.dest.resolve("empty-orphan").exists())
        assertTrue(ws.dest.resolve("keep.txt").isRegularFile())
        assertTrue(ws.source.resolve("keep.txt").isRegularFile())
    }

    test("ignored destination directories are not scanned for orphans") {
        val ws = harness().also { it.resetTrees() }
        ws.source.resolve("keep.txt").writeBytes(byteArrayOf(1))
        ws.dest.resolve("keep.txt").writeBytes(byteArrayOf(1))
        val ignored = ws.dest.resolve("ignored").also { it.createDirectories() }
        ignored.resolve("orphan.txt").writeBytes(byteArrayOf(9))

        ws.engine.deleteOrphans(
            ws.source.toString(),
            ws.dest.toString(),
            listOf(ignored.toFile().absolutePath),
            ws::ignoreMessages,
        )

        assertTrue(ignored.resolve("orphan.txt").isRegularFile())
        assertTrue(ws.dest.resolve("keep.txt").isRegularFile())
    }

    test("symbolic links are skipped and not copied") {
        val ws = harness().also { it.resetTrees() }
        val targetDir = ws.root.resolve("link-target").also {
            if (it.exists()) it.deleteRecursively()
            it.createDirectories()
        }
        targetDir.resolve("via-link.txt").writeBytes(byteArrayOf(42, 17, 99))
        Files.createSymbolicLink(ws.source.resolve("linked-dir"), targetDir)
        ws.source.resolve("regular.txt").writeBytes(byteArrayOf(1, 2, 3))

        ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList(), ws::ignoreMessages)

        assertFalse(ws.dest.resolve("linked-dir").exists())
        assertFalse(ws.dest.resolve("linked-dir").resolve("via-link.txt").exists())
        assertContentEquals(byteArrayOf(1, 2, 3), ws.dest.resolve("regular.txt").readBytes())
        assertTrue(targetDir.resolve("via-link.txt").isRegularFile())
    }

    test("mustBeCopied skips when size and modification time match") {
        val ws = harness().also { it.resetTrees() }
        val sourceFile = ws.source.resolve("same.bin").also { it.writeBytes(byteArrayOf(1, 2, 3, 4, 5)) }
        val destFile = ws.dest.resolve("same.bin").also { it.writeBytes(byteArrayOf(9, 8, 7, 6, 5)) }
        val mtime = sourceFile.toFile().lastModified()
        destFile.toFile().setLastModified(mtime)
        val checksumBefore = md5Hex(destFile)

        ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList(), ws::ignoreMessages)

        assertEquals(checksumBefore, md5Hex(destFile))
        assertEquals(mtime, destFile.toFile().lastModified())
    }

    test("mustBeCopied recopies when sizes differ") {
        val ws = harness().also { it.resetTrees() }
        ws.source.resolve("sized.bin").writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6))
        ws.dest.resolve("sized.bin").writeBytes(byteArrayOf(1, 2))

        ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList(), ws::ignoreMessages)

        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5, 6), ws.dest.resolve("sized.bin").readBytes())
    }

    test("mustBeCopied skips content rewrite when MD5 matches despite different mtime") {
        val ws = harness().also { it.resetTrees() }
        val content = ByteArray(8 * 1024).also { SecureRandom().nextBytes(it) }
        val sourceFile = ws.source.resolve("hash.bin").also { it.writeBytes(content) }
        val destFile = ws.dest.resolve("hash.bin").also { it.writeBytes(content) }
        destFile.toFile().setLastModified(sourceFile.toFile().lastModified() - 60_000)
        val checksumBefore = md5Hex(destFile)

        ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList(), ws::ignoreMessages)

        assertEquals(checksumBefore, md5Hex(destFile))
        assertEquals(sourceFile.toFile().lastModified(), destFile.toFile().lastModified())
    }

    test("mustBeCopied recopies when MD5 differs with same size and different mtime") {
        val ws = harness().also { it.resetTrees() }
        val sourceContent = ByteArray(4096) { 1 }
        val destContent = ByteArray(4096) { 2 }
        val sourceFile = ws.source.resolve("diff.bin").also { it.writeBytes(sourceContent) }
        val destFile = ws.dest.resolve("diff.bin").also { it.writeBytes(destContent) }
        destFile.toFile().setLastModified(sourceFile.toFile().lastModified() - 60_000)

        ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList(), ws::ignoreMessages)

        assertContentEquals(sourceContent, destFile.readBytes())
    }

    test("copy waits while paused and completes after resume") {
        val ws = harness().also {
            it.resetTrees()
            it.engine.copyStateProvider = { CopyState.COPYING }
        }
        ws.source.resolve("a.bin").writeBytes(byteArrayOf(1))
        ws.source.resolve("b.bin").writeBytes(byteArrayOf(2))

        val state = AtomicReference(CopyState.COPY_PAUSED)
        ws.engine.copyStateProvider = { state.get() }
        try {
            val done = CountDownLatch(1)
            val worker = thread(name = "copy-pause-test", isDaemon = true) {
                try {
                    ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList(), ws::ignoreMessages)
                } finally {
                    done.countDown()
                }
            }

            assertTrue(awaitThreadState(worker, Thread.State.WAITING))
            assertFalse(ws.dest.resolve("a.bin").exists())
            assertFalse(ws.dest.resolve("b.bin").exists())

            state.set(CopyState.COPYING)
            ws.engine.resume()
            assertTrue(done.await(30, TimeUnit.SECONDS))
            assertTrue(ws.dest.resolve("a.bin").isRegularFile())
            assertTrue(ws.dest.resolve("b.bin").isRegularFile())
        } finally {
            ws.engine.copyStateProvider = { CopyState.COPYING }
            ws.engine.resume()
        }
    }

    test("cancel while paused stops copying") {
        val ws = harness().also {
            it.resetTrees()
            it.engine.copyStateProvider = { CopyState.COPYING }
        }
        ws.source.resolve("a.bin").writeBytes(byteArrayOf(1))
        ws.source.resolve("b.bin").writeBytes(byteArrayOf(2))

        val state = AtomicReference(CopyState.COPY_PAUSED)
        ws.engine.copyStateProvider = { state.get() }
        try {
            val done = CountDownLatch(1)
            val worker = thread(name = "copy-cancel-test", isDaemon = true) {
                try {
                    ws.engine.copy(ws.source.toString(), ws.dest.toString(), emptyList(), ws::ignoreMessages)
                } finally {
                    done.countDown()
                }
            }

            assertTrue(awaitThreadState(worker, Thread.State.WAITING))
            ws.engine.cancel()
            assertTrue(done.await(30, TimeUnit.SECONDS))
            assertFalse(ws.dest.resolve("a.bin").exists())
            assertFalse(ws.dest.resolve("b.bin").exists())
        } finally {
            ws.engine.copyStateProvider = { CopyState.COPYING }
            ws.engine.resume()
        }
    }
}

private class CopyEngineWorkspace(val root: Path) {
    val source: Path = root.resolve("source")
    val dest: Path = root.resolve("dest")
    val engine = DefaultCopyEngine().apply {
        copyStateProvider = { CopyState.COPYING }
    }

    fun resetTrees() {
        if (source.exists()) source.deleteRecursively()
        if (dest.exists()) dest.deleteRecursively()
        source.createDirectories()
        dest.createDirectories()
    }

    fun ignoreMessages(@Suppress("UNUSED_PARAMETER") message: String) = Unit
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
