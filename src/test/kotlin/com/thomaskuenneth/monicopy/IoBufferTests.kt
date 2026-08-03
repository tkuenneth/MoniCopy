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

import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.testSuite
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

val IoBufferTests by testSuite(
    compartment = { TestCompartment.Sequential },
) {
    test("MD5 and FileCopier default buffers match IoBuffers.DEFAULT_LENGTH") {
        assertEquals(IoBuffers.DEFAULT_LENGTH, MD5().buffer.size)
        assertEquals(IoBuffers.DEFAULT_LENGTH, FileCopier().bufferLength)
    }

    temporaryDirectoryFixture().asParameterForEach {
        test("FileCopier streams files larger than the buffer") { directory ->
            val size = TestIoSizes.wellOver
            val content = ByteArray(size).also { SecureRandom().nextBytes(it) }
            val from = directory.resolve("large-src.bin").also { it.writeBytes(content) }
            val to = directory.resolve("large-dst.bin").toFile()

            assertTrue(FileCopier().copy(from.toFile(), to))
            assertContentEquals(content, to.readBytes())
        }

        test("MD5 hashes files larger than the buffer without loading them whole") { directory ->
            val size = TestIoSizes.moderatelyOver
            val content = ByteArray(size).also { SecureRandom().nextBytes(it) }
            val file = directory.resolve("hash-me.bin").also { it.writeBytes(content) }
            val md5 = MD5()

            val checksum = md5.getChecksum(file.toFile())

            assertEquals(expectedMd5Hex(content), checksum)
            assertFalse(md5.canReadFromBuffer())
            assertEquals(size.toLong(), md5.lengthOfFile)
        }

        test("MD5 records length beyond Int.MAX_VALUE without truncating to int") { directory ->
            val size = Int.MAX_VALUE.toLong() + 1L
            val file = directory.resolve("beyond-int.bin").toFile()
            RandomAccessFile(file, "rw").use { it.setLength(size) }
            assertEquals(size, file.length())
            val md5 = MD5()

            val checksum = md5.getChecksum(file)

            assertNotNull(checksum)
            assertEquals(size, md5.lengthOfFile)
            assertFalse(md5.canReadFromBuffer())
            assertEquals(expectedMd5HexOfZeros(size), checksum)
        }

        test("FileCopier copies from a buffer using a long length") { directory ->
            val content = byteArrayOf(1, 2, 3, 4, 5, 6, 7)
            val to = directory.resolve("from-buffer.bin").toFile()

            assertTrue(FileCopier().copy(content, content.size.toLong(), to))
            assertContentEquals(content, to.readBytes())
        }

        test("FileCopier rejects a long length past the buffer size") { directory ->
            val content = byteArrayOf(1, 2, 3)
            val to = directory.resolve("rejected.bin").toFile()
            val copier = FileCopier()

            assertFalse(copier.copy(content, content.size.toLong() + 1L, to))
            assertFalse(to.exists())
        }

        test("FileCopier copies a zero-byte file") { directory ->
            val from = directory.resolve("empty-src.bin").also { it.writeBytes(byteArrayOf()) }
            val to = directory.resolve("empty-dst.bin").toFile()

            assertTrue(FileCopier().copy(from.toFile(), to))
            assertTrue(to.exists())
            assertEquals(0L, to.length())
            assertContentEquals(byteArrayOf(), to.readBytes())
        }

        test("MD5 hashes a zero-byte file") { directory ->
            val file = directory.resolve("empty.bin").also { it.writeBytes(byteArrayOf()) }.toFile()
            val md5 = MD5()

            val checksum = md5.getChecksum(file)

            assertEquals(expectedMd5Hex(byteArrayOf()), checksum)
            assertEquals(0L, md5.lengthOfFile)
            assertFalse(md5.canReadFromBuffer())
        }
    }
}

private fun expectedMd5Hex(content: ByteArray): String {
    val digest = MessageDigest.getInstance("MD5").digest(content)
    return digest.joinToString("") { byte -> "%02x".format(byte) }
}

private fun expectedMd5HexOfZeros(length: Long): String {
    val digest = MessageDigest.getInstance("MD5")
    val zeros = ByteArray(TestIoSizes.exact)
    var remaining = length
    while (remaining > 0) {
        val n = minOf(zeros.size.toLong(), remaining).toInt()
        digest.update(zeros, 0, n)
        remaining -= n
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}
