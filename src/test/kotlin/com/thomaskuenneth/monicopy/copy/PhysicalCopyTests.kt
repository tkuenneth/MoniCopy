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

import com.thomaskuenneth.monicopy.MD5
import com.thomaskuenneth.monicopy.TestIoSizes
import com.thomaskuenneth.monicopy.temporaryDirectoryFixture
import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.testSuite
import java.io.File
import java.nio.file.Path
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

val PhysicalCopyTests by testSuite(
    compartment = { TestCompartment.Sequential },
) {
    temporaryDirectoryFixture().asParameterForEach {
        test("copies files at half, just-under, full, and one-past the I/O buffer size") { directory ->
            val halfBuffer = randomBytes(TestIoSizes.half)
            val underBuffer = randomBytes(TestIoSizes.justUnder)
            val fullBuffer = randomBytes(TestIoSizes.exact)
            val pastBuffer = randomBytes(TestIoSizes.justOver)

            val source = directory.resolve("source").also { it.createDirectories() }
            val dest = directory.resolve("dest").also { it.createDirectories() }
            source.resolve("half.bin").writeBytes(halfBuffer)
            source.resolve("under.bin").writeBytes(underBuffer)
            source.resolve("full.bin").writeBytes(fullBuffer)
            source.resolve("past.bin").writeBytes(pastBuffer)

            val engine = DefaultCopyEngine().apply {
                copyStateProvider = { CopyState.COPYING }
            }
            engine.copy(source.toString(), dest.toString(), emptyList(), OnMessage { _, _ -> })

            val files = listOf(
                "half.bin" to halfBuffer,
                "under.bin" to underBuffer,
                "full.bin" to fullBuffer,
                "past.bin" to pastBuffer,
            )
            for ((name, buffer) in files) {
                assertCopiedMatchesBuffer(dest.resolve(name), buffer)
                assertProjectMd5MatchesBuffer(source.resolve(name).toFile(), buffer)
                assertProjectMd5MatchesBuffer(dest.resolve(name).toFile(), buffer)
            }
        }
    }
}

private fun randomBytes(size: Int): ByteArray =
    ByteArray(size).also { SecureRandom().nextBytes(it) }

private fun assertCopiedMatchesBuffer(copied: Path, buffer: ByteArray) {
    assertTrue(copied.isRegularFile())
    assertEquals(md5Hex(buffer), md5Hex(copied.readBytes()))
}

private fun assertProjectMd5MatchesBuffer(file: File, buffer: ByteArray) {
    val checksum = MD5().getChecksum(file)
    assertNotNull(checksum)
    assertEquals(md5Hex(buffer), checksum)
}

private fun md5Hex(content: ByteArray): String {
    val digest = MessageDigest.getInstance("MD5").digest(content)
    return digest.joinToString("") { byte -> "%02x".format(byte) }
}
