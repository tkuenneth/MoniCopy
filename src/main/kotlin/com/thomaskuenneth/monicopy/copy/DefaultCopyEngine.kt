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
import com.thomaskuenneth.monicopy.generated.resources.could_not_delete_path
import com.thomaskuenneth.monicopy.generated.resources.finished_copying
import com.thomaskuenneth.monicopy.generated.resources.finished_deleting
import com.thomaskuenneth.monicopy.generated.resources.started_copying
import com.thomaskuenneth.monicopy.generated.resources.started_deleting
import org.koin.core.annotation.Single
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

@Single
class DefaultCopyEngine : CopyEngine, Pausable {

    private val logger = Logger.getGlobal()
    private val copier = FileCopier()
    private val mdFrom = MD5()
    private val sbFrom = StringBuilder()
    private val mdTo = MD5()
    private val sbTo = StringBuilder()
    private val lock = Any()
    @Volatile
    private var cancelled = false
    private val symbolicLinks = ArrayList<File>(FileStore.SYMBOLIC_LINKS_INITIAL_CAPACITY)

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
                    logger.log(Level.INFO, "pausing")
                    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                    (lock as Object).wait()
                } catch (ex: InterruptedException) {
                    logger.log(Level.SEVERE, "interruption while waiting to resume", ex)
                } finally {
                    logger.log(Level.INFO, "resuming")
                }
            }
        }
    }

    override fun copy(
        fromPath: String,
        toPath: String,
        ignores: List<String>,
        onMessage: (String) -> Unit,
    ) {
        copy(fromPath, toPath, ignores, onMessage, onProgress = {}, onCounts = { _, _ -> }, onCopyDecision = {})
    }

    override fun copy(
        fromPath: String,
        toPath: String,
        ignores: List<String>,
        onMessage: (String) -> Unit,
        onProgress: (Int) -> Unit,
        onCounts: (fileCount: Long, subfolderCount: Long) -> Unit,
        onCopyDecision: (copied: Boolean) -> Unit,
    ) {
        copy(File(fromPath), File(toPath), ignores, onMessage, onProgress, onCounts, onCopyDecision)
    }

    override fun deleteOrphans(
        sourcePath: String,
        destPath: String,
        ignores: List<String>,
        onMessage: (String) -> Unit,
    ) {
        deleteOrphans(sourcePath, destPath, ignores, onMessage, onProgress = {})
    }

    override fun deleteOrphans(
        sourcePath: String,
        destPath: String,
        ignores: List<String>,
        onMessage: (String) -> Unit,
        onProgress: (Int) -> Unit,
    ) {
        deleteOrphans(File(sourcePath), File(destPath), ignores, onMessage, onProgress)
    }

    private fun copy(
        from: File,
        to: File,
        ignores: List<String>,
        onMessage: (String) -> Unit,
        onProgress: (Int) -> Unit,
        onCounts: (fileCount: Long, subfolderCount: Long) -> Unit,
        onCopyDecision: (copied: Boolean) -> Unit,
    ) {
        cancelled = false
        try {
            copyInternal(from, to, ignores, onMessage, onProgress, onCounts, onCopyDecision)
        } catch (_: CopyCancelledException) {
        }
    }

    private fun deleteOrphans(
        sourceDir: File,
        destDir: File,
        ignores: List<String>,
        onMessage: (String) -> Unit,
        onProgress: (Int) -> Unit,
    ) {
        cancelled = false
        try {
            deleteOrphansInternal(sourceDir, destDir, ignores, onMessage, onProgress)
        } catch (_: CopyCancelledException) {
        }
    }

    private fun copyInternal(
        from: File,
        to: File,
        ignores: List<String>,
        onMessage: (String) -> Unit,
        onProgress: (Int) -> Unit,
        onCounts: (fileCount: Long, subfolderCount: Long) -> Unit,
        onCopyDecision: (copied: Boolean) -> Unit,
    ) {
        val offset = from.absolutePath.length + 1
        onMessage(blockingGetString(Res.string.started_copying))
        val store = fileStoreFactory()
        val files = store.fill(from, ignores)
        rememberSymbolicLinksFrom(store)
        if (files == null) {
            onCounts(0L, 0L)
            reportInitialProgress(0L, onProgress)
            onMessage(blockingGetString(Res.string.finished_copying))
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
                val readFromBuffer = mdFrom.canReadFromBuffer()
                val ok = if (readFromBuffer) {
                    copier.copy(mdFrom.buffer, mdFrom.lengthOfFile, destination)
                } else {
                    copier.copy(fileToCopy, destination)
                }
                if (!ok) {
                    onMessage(
                        blockingGetString(
                            Res.string.could_not_copy,
                            fileToCopy.absolutePath,
                            copier.lastLocalizedMessage,
                        ),
                    )
                } else {
                    destination.setLastModified(fileToCopy.lastModified())
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
        onMessage(blockingGetString(Res.string.finished_copying))
    }

    private fun deleteOrphansInternal(
        sourceDir: File,
        destDir: File,
        ignores: List<String>,
        onMessage: (String) -> Unit,
        onProgress: (Int) -> Unit,
    ) {
        var offset = destDir.absolutePath.length
        onMessage(blockingGetString(Res.string.started_deleting))
        val store = fileStoreFactory()
        val files = store.fill(destDir, ignores)
        rememberSymbolicLinksFrom(store)
        if (files == null) {
            reportInitialProgress(0L, onProgress)
            onMessage(blockingGetString(Res.string.finished_deleting))
            return
        }
        val numberOfFiles = store.numberOfFiles
        val folders = collectFolders(destDir)
        val total = numberOfFiles + folders.size.toLong()
        var processed = 0L
        var lastReported = reportInitialProgress(total, onProgress)
        for (fileToDelete in files) {
            checkForPause()
            val filename = fileToDelete.absolutePath
            if (filename[offset] == File.separatorChar) {
                offset += 1
            }
            val name = filename.substring(offset)
            val sourceFile = File(sourceDir, name)
            if (!sourceFile.exists()) {
                if (!fileToDelete.delete()) {
                    onMessage(
                        blockingGetString(
                            Res.string.could_not_delete,
                            filename,
                            sourceFile.absolutePath,
                        ),
                    )
                }
            }
            lastReported = reportSteppedProgress(
                processed = ++processed,
                total = total,
                lastReported = lastReported,
                onProgress = onProgress,
            )
        }
        for (folder in folders) {
            checkForPause()
            val absolutePath = folder.absolutePath
            if (!folder.isDirectory) {
                logger.log(Level.SEVERE, "$absolutePath is not a directory")
            } else {
                val children = folder.list()
                if (children != null && children.isEmpty()) {
                    logger.log(Level.INFO, "deleting directory $absolutePath")
                    if (!folder.delete()) {
                        onMessage(blockingGetString(Res.string.could_not_delete_path, absolutePath))
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
        onMessage(blockingGetString(Res.string.finished_deleting))
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
            logger.log(Level.INFO, "not found in destination")
            return true
        }
        val lenFileToCopy = fileToCopy.length()
        val lenDestination = destination.length()
        if (lenFileToCopy != lenDestination) {
            logger.log(
                Level.INFO,
                "different size in destination: $lenFileToCopy != $lenDestination",
            )
            return true
        }
        val lastModifiedSource = fileToCopy.lastModified()
        val lastModifiedDest = destination.lastModified()
        if (lastModifiedSource == lastModifiedDest) {
            return false
        }
        logger.log(
            Level.INFO,
            "different modification date: %tc != %tc".format(lastModifiedSource, lastModifiedDest),
        )
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
            logger.log(Level.SEVERE, "interruption while joining threads", e)
            return true
        }
        val copy = sbFrom.toString() != sbTo.toString()
        if (copy) {
            logger.log(
                Level.INFO,
                "different md5 hashes: $sbFrom != $sbTo",
            )
        } else {
            try {
                val succeeded = destination.setLastModified(fileToCopy.lastModified())
                logger.log(
                    Level.INFO,
                    "${destination.absolutePath} setLastModified(): $succeeded",
                )
            } catch (e: IllegalArgumentException) {
                logger.log(Level.SEVERE, "setLastModified()", e)
            }
        }
        return copy
    }
}
