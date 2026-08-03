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

import com.thomaskuenneth.monicopy.FileStore
import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories

/**
 * Per-test workspace for engine I/O tests.
 *
 * Built inside the test body (not as a TestBalloon fixture value): [DefaultCopyEngine]
 * retains large I/O buffers, and test-level fixtures keep their value in the test envelope
 * for the duration of the test, which can exhaust the heap across large sequential cases.
 */
class CopyEngineWorkspace(val root: Path) {
    val source: Path = root.resolve("source").also { it.createDirectories() }
    val dest: Path = root.resolve("dest").also { it.createDirectories() }
    val engine = DefaultCopyEngine().apply {
        copyStateProvider = { CopyState.COPYING }
    }

    fun ignoreMessages(@Suppress("UNUSED_PARAMETER") message: String) = Unit

    fun installNullFillStore() {
        engine.fileStoreFactory = {
            object : FileStore(engine) {
                override fun fill(file: File?, ignores: List<String>?): MutableList<File>? = null
            }
        }
    }
}
