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

interface CopyEngine {
    var copyStateProvider: () -> CopyState
    fun resume()
    fun cancel()

    fun copy(
        fromPath: String,
        toPath: String,
        ignores: List<String>,
        onMessage: (String) -> Unit,
    )

    fun copy(
        fromPath: String,
        toPath: String,
        ignores: List<String>,
        onMessage: (String) -> Unit,
        onProgress: (Int) -> Unit,
        onCounts: (fileCount: Long, subfolderCount: Long) -> Unit,
    ) {
        copy(fromPath, toPath, ignores, onMessage)
    }

    fun deleteOrphans(
        sourcePath: String,
        destPath: String,
        ignores: List<String>,
        onMessage: (String) -> Unit,
    )

    fun deleteOrphans(
        sourcePath: String,
        destPath: String,
        ignores: List<String>,
        onMessage: (String) -> Unit,
        onProgress: (Int) -> Unit,
    ) {
        deleteOrphans(sourcePath, destPath, ignores, onMessage)
    }
}
