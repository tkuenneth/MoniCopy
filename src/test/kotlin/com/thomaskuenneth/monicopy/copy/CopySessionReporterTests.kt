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
package com.thomaskuenneth.monicopy.copy

import de.infix.testBalloon.framework.core.TestCompartment
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

val CopySessionReporterTests by testSuite(
    compartment = { TestCompartment.Sequential },
) {
    test("begin prepares an empty map for every reason") {
        withCopySession {
            CopySessionReporter.begin()

            val snapshot = CopySessionReporter.snapshot()
            assertEquals(CopySessionReason.entries.toSet(), snapshot.keys)
            assertTrue(snapshot.values.all { it.isEmpty() })
            assertFalse(CopySessionReporter.hasContent)
            assertFalse(CopySessionReporter.report.value.hasContent)
        }
    }

    test("log stores the message under the reason and a timestamp") {
        withCopySession {
            CopySessionReporter.begin()
            CopySessionReporter.log(CopySessionReason.CouldNotCopy, "copy-failed")

            assertTrue(CopySessionReporter.hasContent)
            val byTime = CopySessionReporter.snapshot().getValue(CopySessionReason.CouldNotCopy)
            assertEquals(1, byTime.size)
            assertEquals("copy-failed", byTime.values.single())
            assertEquals("copy-failed", CopySessionReporter.report.value
                .entries.getValue(CopySessionReason.CouldNotCopy).values.single())
        }
    }

    test("log with throwable includes the stack trace in the message") {
        withCopySession {
            CopySessionReporter.begin()
            CopySessionReporter.log(
                CopySessionReason.Interrupted,
                "boom",
                IllegalStateException("detail"),
            )

            val text = CopySessionReporter.snapshot()
                .getValue(CopySessionReason.Interrupted)
                .values
                .single()
            assertTrue(text.startsWith("boom"))
            assertTrue(text.contains("IllegalStateException"))
            assertTrue(text.contains("detail"))
        }
    }

    test("begin clears previous entries and publishes an empty report") {
        withCopySession {
            CopySessionReporter.begin()
            CopySessionReporter.log(CopySessionReason.CouldNotDelete, "first-run")
            assertTrue(CopySessionReporter.report.value.hasContent)

            CopySessionReporter.begin()

            assertFalse(CopySessionReporter.hasContent)
            assertFalse(CopySessionReporter.report.value.hasContent)
            CopySessionReporter.log(CopySessionReason.CouldNotDelete, "second-run")
            val byTime = CopySessionReporter.snapshot().getValue(CopySessionReason.CouldNotDelete)
            assertEquals(listOf("second-run"), byTime.values.toList())
        }
    }

    test("log is ignored before begin") {
        withCopySession {
            CopySessionReporter.log(CopySessionReason.NotADirectory, "ignored")

            assertFalse(CopySessionReporter.hasContent)
            assertTrue(CopySessionReporter.snapshot().isEmpty())
            assertTrue(CopySessionReporter.report.value.entries.isEmpty())
        }
    }
}

private inline fun withCopySession(block: () -> Unit) {
    synchronized(CopySessionReporter) {
        try {
            block()
        } finally {
            resetCopySessionReporter()
        }
    }
}

private fun resetCopySessionReporter() {
    val entries = CopySessionReporter::class.java.getDeclaredField("entries")
    entries.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    (entries.get(CopySessionReporter) as MutableMap<*, *>).clear()
    val active = CopySessionReporter::class.java.getDeclaredField("active")
    active.isAccessible = true
    active.setBoolean(CopySessionReporter, false)
    val report = CopySessionReporter::class.java.getDeclaredField("_report")
    report.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val flow = report.get(CopySessionReporter) as kotlinx.coroutines.flow.MutableStateFlow<CopySessionReport>
    flow.value = CopySessionReport()
}
