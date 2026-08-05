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
import kotlin.test.assertTrue

val NanoInstantGeneratorTests by testSuite(
    compartment = { TestCompartment.Concurrent },
) {
    val sampleSizes = listOf(2, 64, 256)

    for (sampleSize in sampleSizes) {
        test("successive instants are distinct and non-decreasing for $sampleSize samples") {
            val instants = List(sampleSize) { NanoInstantGenerator.getPreciseInstant() }

            assertEquals(sampleSize, instants.toSet().size)
            assertTrue(instants.zipWithNext().all { (earlier, later) -> !earlier.isAfter(later) })
            assertEquals(instants.sorted(), instants)
        }
    }
}
