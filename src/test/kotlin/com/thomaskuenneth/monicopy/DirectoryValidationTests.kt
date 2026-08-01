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
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

val DirectoryValidationTests by testSuite(
    compartment = { TestCompartment.Concurrent },
) {
    temporaryDirectoryFixture().asParameterForEach {
        test("dest nested under source is reported as Overlap") { directory ->
            val source = directory.toFile()
            val dest = directory.resolve("nested-dest").also { it.createDirectories() }.toFile()

            val result = validateDirectories(source.absolutePath, dest.absolutePath)

            assertEquals(DirectoryValidationIssue.Overlap, result.issue)
            assertFalse(result.canProceed)
        }

        test("unreadable source is reported as CannotRead") { directory ->
            val missingSource = directory.resolve("missing-source").toFile().absolutePath
            val dest = directory.resolve("dest").also { it.createDirectories() }.toFile().absolutePath

            val result = validateDirectories(missingSource, dest)

            assertEquals(DirectoryValidationIssue.CannotRead, result.issue)
            assertFalse(result.canProceed)
        }

        test("unwritable destination is reported as CannotWrite") { directory ->
            val source = directory.resolve("source").also { it.createDirectories() }.toFile().absolutePath
            val missingDest = directory.resolve("missing-dest").toFile().absolutePath

            val result = validateDirectories(source, missingDest)

            assertEquals(DirectoryValidationIssue.CannotWrite, result.issue)
            assertFalse(result.canProceed)
        }

        test("readable source and writable destination can proceed") { directory ->
            val source = directory.resolve("source").also { it.createDirectories() }.toFile().absolutePath
            val dest = directory.resolve("dest").also { it.createDirectories() }.toFile().absolutePath

            val result = validateDirectories(source, dest)

            assertNull(result.issue)
            assertTrue(result.canProceed)
        }

        test("prepareDirectories creates missing source and destination") { directory ->
            val source = directory.resolve("to-create-source")
            val dest = directory.resolve("to-create-dest")
            assertFalse(source.exists())
            assertFalse(dest.exists())

            prepareDirectories(source.toString(), dest.toString())

            assertTrue(source.isDirectory())
            assertTrue(dest.isDirectory())
        }
    }

    test("null source or dest cannot proceed") {
        assertFalse(validateDirectories(null, "/tmp").canProceed)
        assertFalse(validateDirectories("/tmp", null).canProceed)
        assertNull(validateDirectories(null, null).issue)
    }
}
