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

import com.thomaskuenneth.monicopy.platform.DirectoryChooser
import java.util.concurrent.CountDownLatch

internal class RecordingCopyRepository(
    private val preferences: CopyPreferences,
) : CopyRepository {
    val savedSourceDirs = mutableListOf<String?>()
    val savedDestDirs = mutableListOf<String?>()
    val savedDeleteOrphans = mutableListOf<Boolean>()
    val savedPreserveSymbolicLinks = mutableListOf<Boolean>()
    val savedIgnores = mutableListOf<List<String>>()

    fun savedDirectories(role: DirectoryRole): List<String?> = when (role) {
        DirectoryRole.Source -> savedSourceDirs
        DirectoryRole.Destination -> savedDestDirs
    }

    override fun load(): CopyPreferences = preferences
    override fun saveSourceDir(path: String?) {
        savedSourceDirs += path
    }

    override fun saveDestDir(path: String?) {
        savedDestDirs += path
    }

    override fun saveDeleteOrphans(enabled: Boolean) {
        savedDeleteOrphans += enabled
    }

    override fun savePreserveSymbolicLinks(enabled: Boolean) {
        savedPreserveSymbolicLinks += enabled
    }

    override fun saveIgnores(ignores: List<String>) {
        savedIgnores += ignores
    }
}

internal class ControllableCopyEngine : CopyEngine {
    val calls = mutableListOf<String>()
    val copyEntered = CountDownLatch(1)
    val copyFinished = CountDownLatch(1)
    val deleteEntered = CountDownLatch(1)
    private val copyGate = CountDownLatch(1)
    private val deleteGate = CountDownLatch(1)
    var blockCopy = false
    var blockDelete = false
    var resumeCount = 0
    var cancelCount = 0
    var lastPreserveSymbolicLinks: Boolean? = null

    override var copyStateProvider: () -> CopyState = { CopyState.IDLE }

    override fun resume() {
        resumeCount++
    }

    override fun cancel() {
        cancelCount++
        copyGate.countDown()
        deleteGate.countDown()
    }

    override fun copy(
        fromPath: String,
        toPath: String,
        ignores: List<String>,
    ) {
        copyEntered.countDown()
        if (blockCopy) {
            copyGate.await()
        }
        calls += "copy"
        copyFinished.countDown()
    }

    override fun copy(
        fromPath: String,
        toPath: String,
        ignores: List<String>,
        onProgress: (Int) -> Unit,
        onCounts: (fileCount: Long, subfolderCount: Long) -> Unit,
        onCopyDecision: (copied: Boolean) -> Unit,
        preserveSymbolicLinks: Boolean,
    ) {
        lastPreserveSymbolicLinks = preserveSymbolicLinks
        copy(fromPath, toPath, ignores)
    }

    override fun deleteOrphans(
        sourcePath: String,
        destPath: String,
        ignores: List<String>,
    ) {
        deleteEntered.countDown()
        if (blockDelete) {
            deleteGate.await()
        }
        calls += "deleteOrphans"
    }

    fun releaseCopy() {
        copyGate.countDown()
    }

    fun releaseDelete() {
        deleteGate.countDown()
    }
}

internal class ScriptedDirectoryChooser : DirectoryChooser {
    private val results = mutableListOf<String?>()
    var requestCount = 0
        private set

    fun enqueue(vararg paths: String?) {
        results.addAll(paths)
    }

    override fun chooseDirectory(title: String, initialPath: String?): String? {
        requestCount++
        return results.removeFirstOrNull()
    }
}

