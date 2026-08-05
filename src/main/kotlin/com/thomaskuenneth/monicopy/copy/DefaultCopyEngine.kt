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

import com.thomaskuenneth.monicopy.FileCopier
import com.thomaskuenneth.monicopy.FileStore
import com.thomaskuenneth.monicopy.FolderMap
import com.thomaskuenneth.monicopy.MD5
import com.thomaskuenneth.monicopy.Pausable
import com.thomaskuenneth.monicopy.blockingGetString
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.could_not_copy
import com.thomaskuenneth.monicopy.generated.resources.could_not_delete
import com.thomaskuenneth.monicopy.generated.resources.could_not_set_last_modified
import com.thomaskuenneth.monicopy.generated.resources.interruption_while_joining_threads
import com.thomaskuenneth.monicopy.generated.resources.interruption_while_waiting_to_resume
import com.thomaskuenneth.monicopy.generated.resources.not_a_directory
import org.koin.core.annotation.Single
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption

@Single
class DefaultCopyEngine : CopyEngine, Pausable {

    private val copier = FileCopier()
    private val mdFrom = MD5()
    private val sbFrom = StringBuilder()
    private val mdTo = MD5()
    private val sbTo = StringBuilder()
    private val lock = Any()
    @Volatile
    private var cancelled = false
    private val symbolicLinks = ArrayList<File>(FileStore.SYMBOLIC_LINKS_INITIAL_CAPACITY)
    private val symbolicLinkPreserver = SymbolicLinkPreserver()

    internal val rememberedSymbolicLinks: List<File>
        get() = symbolicLinks

    internal var fileStoreFactory: () -> FileStore = { FileStore(this) }

    override var copyStateProvider: () -> CopyState = { CopyState.IDLE }

    override fun cancel() {
        cancelled = true
        resume()
    }

    override fun resume() {
        synchronized(lock) {
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            (lock as Object).notifyAll()
        }
    }

    override fun checkForPause() {
        if (cancelled) throw CopyCancelledException()
        synchronized(lock) {
            val state = copyStateProvider()
            if (state == CopyState.COPY_PAUSED || state == CopyState.DELETE_PAUSED) {
                try {
                    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                    (lock as Object).wait()
                } catch (ex: InterruptedException) {
                    CopySessionReporter.log(
                        CopySessionReason.Interrupted,
                        blockingGetString(Res.string.interruption_while_waiting_to_resume),
                        ex,
                    )
                }
            }
        }
    }

    override fun copy(
        fromPath: String,
        toPath: String,
        ignores: List<String>,
    ) {
        copy(fromPath, toPath, ignores, onProgress = {}, onCounts = { _, _ -> }, onCopyDecision = {}, preserveSymbolicLinks = true)
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
        copy(File(fromPath), File(toPath), ignores, onProgress, onCounts, onCopyDecision, preserveSymbolicLinks)
    }

    override fun deleteOrphans(
        sourcePath: String,
        destPath: String,
        ignores: List<String>,
    ) {
        deleteOrphans(sourcePath, destPath, ignores, onProgress = {})
    }

    override fun deleteOrphans(
        sourcePath: String,
        destPath: String,
        ignores: List<String>,
        onProgress: (Int) -> Unit,
    ) {
        deleteOrphans(File(sourcePath), File(destPath), ignores, onProgress)
    }

    private fun copy(
        from: File,
        to: File,
        ignores: List<String>,
        onProgress: (Int) -> Unit,
        onCounts: (fileCount: Long, subfolderCount: Long) -> Unit,
        onCopyDecision: (copied: Boolean) -> Unit,
        preserveSymbolicLinks: Boolean,
    ) {
        cancelled = false
        try {
            copyInternal(from, to, ignores, onProgress, onCounts, onCopyDecision, preserveSymbolicLinks)
        } catch (_: CopyCancelledException) {
        }
    }

    private fun deleteOrphans(
        sourceDir: File,
        destDir: File,
        ignores: List<String>,
        onProgress: (Int) -> Unit,
    ) {
        cancelled = false
        try {
            deleteOrphansInternal(sourceDir, destDir, ignores, onProgress)
        } catch (_: CopyCancelledException) {
        }
    }

    private fun copyInternal(
        from: File,
        to: File,
        ignores: List<String>,
        onProgress: (Int) -> Unit,
        onCounts: (fileCount: Long, subfolderCount: Long) -> Unit,
        onCopyDecision: (copied: Boolean) -> Unit,
        preserveSymbolicLinks: Boolean,
    ) {
        val offset = from.absolutePath.length + 1
        val store = fileStoreFactory()
        val files = store.fill(from, ignores)
        rememberSymbolicLinksFrom(store)
        if (files == null) {
            onCounts(0L, 0L)
            reportInitialProgress(0L, onProgress)
            maybePreserveSymbolicLinks(preserveSymbolicLinks, from, to)
            return
        }
        val numberOfFiles = store.numberOfFiles
        val subfolderCount = store.numberOfDirectories - 1
        onCounts(numberOfFiles, subfolderCount)
        var numberOfProcessedFiles = 0L
        var lastReported = reportInitialProgress(numberOfFiles, onProgress)
        for (fileToCopy in files) {
            checkForPause()
            val destination = File(to, fileToCopy.absolutePath.substring(offset))
            if (mustBeCopied(fileToCopy, destination)) {
                onCopyDecision(true)
                try {
                    val readFromBuffer = mdFrom.canReadFromBuffer()
                    val ok = if (readFromBuffer) {
                        copier.copy(mdFrom.buffer, mdFrom.lengthOfFile, destination)
                    } else {
                        copier.copy(fileToCopy, destination)
                    }
                    if (!ok) {
                        reportCouldNotCopy(fileToCopy.absolutePath, "length mismatch after copy")
                    } else {
                        destination.setLastModified(fileToCopy.lastModified())
                    }
                } catch (e: IOException) {
                    reportCouldNotCopy(fileToCopy.absolutePath, e.localizedMessage)
                }
            } else {
                onCopyDecision(false)
            }
            lastReported = reportSteppedProgress(
                processed = ++numberOfProcessedFiles,
                total = numberOfFiles,
                lastReported = lastReported,
                onProgress = onProgress,
            )
        }
        maybePreserveSymbolicLinks(preserveSymbolicLinks, from, to)
    }

    private fun maybePreserveSymbolicLinks(
        preserveSymbolicLinks: Boolean,
        from: File,
        to: File,
    ) {
        if (!preserveSymbolicLinks) return
        symbolicLinkPreserver.preserve(symbolicLinks, from, to) { source, detail ->
            reportCouldNotCopy(source.absolutePath, detail)
        }
    }

    private fun reportCouldNotCopy(path: String, detail: String?) {
        val message = blockingGetString(Res.string.could_not_copy, path, detail.orEmpty())
        CopySessionReporter.log(CopySessionReason.CouldNotCopy, message)
    }

    private fun deleteOrphansInternal(
        sourceDir: File,
        destDir: File,
        ignores: List<String>,
        onProgress: (Int) -> Unit,
    ) {
        val store = fileStoreFactory()
        val files = store.fill(destDir, ignores)
        rememberSymbolicLinksFrom(store)
        if (files == null) {
            reportInitialProgress(0L, onProgress)
            return
        }
        val numberOfFiles = store.numberOfFiles
        val folders = collectFolders(destDir)
        val total = numberOfFiles + folders.size.toLong()
        var processed = 0L
        var lastReported = reportInitialProgress(total, onProgress)
        for (fileToDelete in files) {
            checkForPause()
            deleteOrphanEntry(fileToDelete, sourceDir, destDir)
            lastReported = reportSteppedProgress(
                processed = ++processed,
                total = total,
                lastReported = lastReported,
                onProgress = onProgress,
            )
        }
        for (linkToDelete in store.symbolicLinks) {
            checkForPause()
            deleteOrphanEntry(linkToDelete, sourceDir, destDir)
        }
        for (folder in folders) {
            checkForPause()
            if (!folder.isDirectory) {
                val message = blockingGetString(Res.string.not_a_directory, folder.absolutePath)
                CopySessionReporter.log(CopySessionReason.NotADirectory, message)
            } else {
                val children = folder.list()
                if (children != null && children.isEmpty()) {
                    if (folder.absoluteFile != destDir.absoluteFile) {
                        deletePath(folder)
                    }
                }
            }
            lastReported = reportSteppedProgress(
                processed = ++processed,
                total = total,
                lastReported = lastReported,
                onProgress = onProgress,
            )
        }
    }

    private fun deleteOrphanEntry(
        destEntry: File,
        sourceDir: File,
        destDir: File,
    ) {
        val name = relativePathUnder(destDir, destEntry)
        val sourceEntry = File(sourceDir, name)
        if (Files.exists(sourceEntry.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            return
        }
        deletePath(destEntry)
    }

    private fun deletePath(path: File) {
        try {
            Files.delete(path.toPath())
        } catch (e: IOException) {
            val message = blockingGetString(
                Res.string.could_not_delete,
                path.absolutePath,
                e.localizedMessage,
            )
            CopySessionReporter.log(CopySessionReason.CouldNotDelete, message)
        }
    }

    private fun collectFolders(base: File): List<File> {
        val folderMap = FolderMap()
        folderMap.fill(base)
        val folders = ArrayList<File>()
        val iterator = folderMap.iterator
        while (iterator.hasNext()) {
            folders.add(iterator.next())
        }
        return folders
    }

    private fun rememberSymbolicLinksFrom(store: FileStore) {
        symbolicLinks.clear()
        symbolicLinks.addAll(store.symbolicLinks)
    }

    private fun reportInitialProgress(
        total: Long,
        onProgress: (Int) -> Unit,
    ): Int {
        val percent = if (total == 0L) 100 else 0
        onProgress(percent)
        return percent
    }

    private fun reportSteppedProgress(
        processed: Long,
        total: Long,
        lastReported: Int,
        onProgress: (Int) -> Unit,
    ): Int {
        if (total <= 0L) {
            onProgress(100)
            return 100
        }
        val percent = ((100.0 * processed) / total).toInt().coerceIn(0, 100)
        if (percent % 10 == 0 && percent != lastReported) {
            onProgress(percent)
            return percent
        }
        return lastReported
    }

    @Synchronized
    private fun mustBeCopied(fileToCopy: File, destination: File): Boolean {
        mdFrom.reset()
        if (!destination.exists()) {
            return true
        }
        val lenFileToCopy = fileToCopy.length()
        val lenDestination = destination.length()
        if (lenFileToCopy != lenDestination) {
            return true
        }
        val lastModifiedSource = fileToCopy.lastModified()
        val lastModifiedDest = destination.lastModified()
        if (lastModifiedSource == lastModifiedDest) {
            return false
        }
        val tFrom = Thread {
            sbFrom.setLength(0)
            mdFrom.getChecksum(fileToCopy)?.let { sbFrom.append(it) }
        }
        val tTo = Thread {
            sbTo.setLength(0)
            mdTo.getChecksum(destination)?.let { sbTo.append(it) }
        }
        tFrom.start()
        tTo.start()
        try {
            tFrom.join()
            tTo.join()
        } catch (e: InterruptedException) {
            CopySessionReporter.log(
                CopySessionReason.Interrupted,
                blockingGetString(Res.string.interruption_while_joining_threads),
                e,
            )
            return true
        }
        val copy = sbFrom.toString() != sbTo.toString()
        if (!copy) {
            try {
                destination.setLastModified(fileToCopy.lastModified())
            } catch (e: IllegalArgumentException) {
                CopySessionReporter.log(
                    CopySessionReason.CouldNotSetLastModified,
                    blockingGetString(
                        Res.string.could_not_set_last_modified,
                        destination.absolutePath,
                    ),
                    e,
                )
            }
        }
        return copy
    }
}
