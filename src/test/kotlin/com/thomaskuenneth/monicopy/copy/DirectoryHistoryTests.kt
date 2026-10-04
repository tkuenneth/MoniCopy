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

import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertEquals

val DirectoryHistoryTests by testSuite {
    test("a new directory goes to the front") {
        assertEquals(listOf("/c", "/a", "/b"), listOf("/a", "/b").withRecentDirectory("/c"))
    }

    test("an existing directory moves to the front without being duplicated") {
        assertEquals(listOf("/b", "/a", "/c"), listOf("/a", "/b", "/c").withRecentDirectory("/b"))
    }

    test("an empty history starts with the directory") {
        assertEquals(listOf("/a"), emptyList<String>().withRecentDirectory("/a"))
    }

    test("the history keeps the $DIRECTORY_HISTORY_SIZE most recent directories") {
        val full = (1..DIRECTORY_HISTORY_SIZE).map { "/$it" }

        val history = full.withRecentDirectory("/new")

        assertEquals(listOf("/new") + full.dropLast(1), history)
    }

    test("moving an existing directory to the front of a full history drops nothing") {
        val full = (1..DIRECTORY_HISTORY_SIZE).map { "/$it" }

        val history = full.withRecentDirectory(full.last())

        assertEquals(listOf(full.last()) + full.dropLast(1), history)
    }
}
