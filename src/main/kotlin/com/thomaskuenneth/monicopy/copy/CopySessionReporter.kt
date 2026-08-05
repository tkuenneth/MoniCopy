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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

enum class CopySessionReason {
    CouldNotCopy,
    CouldNotDelete,
    NotADirectory,
    Interrupted,
    CouldNotSetLastModified,
}

data class CopySessionReport(
    val entries: Map<CopySessionReason, Map<Instant, String>> = emptyMap(),
) {
    val hasContent: Boolean
        get() = entries.values.any { it.isNotEmpty() }
}

object CopySessionReporter {
    private var active = false
    private val entries = HashMap<CopySessionReason, HashMap<Instant, String>>()
    private val _report = MutableStateFlow(CopySessionReport())
    val report: StateFlow<CopySessionReport> = _report.asStateFlow()

    val hasContent: Boolean
        get() = report.value.hasContent

    fun begin() {
        synchronized(this) {
            entries.clear()
            for (reason in CopySessionReason.entries) {
                entries[reason] = HashMap()
            }
            active = true
            publishLocked()
        }
    }

    fun log(reason: CopySessionReason, message: String, thrown: Throwable? = null) {
        synchronized(this) {
            if (!active) return
            val text = if (thrown == null) {
                message
            } else {
                buildString {
                    append(message)
                    append('\n')
                    append(thrown.stackTraceToString().trimEnd())
                }
            }
            entries.getValue(reason)[NanoInstantGenerator.getPreciseInstant()] = text
            publishLocked()
        }
    }

    fun snapshot(): Map<CopySessionReason, Map<Instant, String>> =
        report.value.entries

    private fun publishLocked() {
        _report.value = CopySessionReport(
            entries = entries.mapValues { (_, byTime) -> byTime.toMap() },
        )
    }
}

object NanoInstantGenerator {
    private val baselineInstant: Instant = Instant.now()
    private val baselineNanoTime: Long = System.nanoTime()

    fun getPreciseInstant(): Instant {
        val nanosElapsed = System.nanoTime() - baselineNanoTime
        return baselineInstant.plusNanos(nanosElapsed)
    }
}