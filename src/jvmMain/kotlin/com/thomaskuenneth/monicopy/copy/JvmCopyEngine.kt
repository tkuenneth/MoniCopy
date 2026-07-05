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
import com.thomaskuenneth.monicopy.generated.resources.find_files
import com.thomaskuenneth.monicopy.generated.resources.finished_copying
import com.thomaskuenneth.monicopy.generated.resources.finished_deleting
import com.thomaskuenneth.monicopy.generated.resources.number_of_files_and_directories
import com.thomaskuenneth.monicopy.generated.resources.started_copying
import com.thomaskuenneth.monicopy.generated.resources.started_deleting
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

class JvmCopyEngine : CopyEngine, Pausable {

    private val logger = Logger.getGlobal()
    private val copier = FileCopier()
    private val mdFrom = MD5()
    private val sbFrom = StringBuilder()
    private val mdTo = MD5()
    private val sbTo = StringBuilder()
    private val lock = Any()

    override var copyStateProvider: () -> CopyState = { CopyState.IDLE }

    override fun resume() {
        synchronized(lock) {
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            (lock as Object).notifyAll()
        }
    }

    override fun checkForPause() {
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

    override fun copy(fromPath: String, toPath: String, ignores: List<String>, onMessage: (String) -> Unit) {
        copy(File(fromPath), File(toPath), ignores, onMessage)
    }

    override fun deleteOrphans(
        sourcePath: String,
        destPath: String,
        ignores: List<String>,
        onMessage: (String) -> Unit,
    ) {
        deleteOrphans(File(sourcePath), File(destPath), ignores, onMessage)
    }

    private fun copy(from: File, to: File, ignores: List<String>, onMessage: (String) -> Unit) {
        val offset = from.absolutePath.length + 1
        onMessage(blockingGetString(Res.string.started_copying))
        onMessage(blockingGetString(Res.string.find_files))
        val store = FileStore(this)
        val files = store.fill(from, ignores) ?: return
        val numberOfFiles = store.numberOfFiles
        var numberOfProcessedFiles = 0L
        val ratio = 100f / numberOfFiles.toFloat()
        onMessage(
            blockingGetString(
                Res.string.number_of_files_and_directories,
                numberOfFiles,
                store.numberOfDirectories - 1,
            ),
        )
        var lastPrinted = -1
        for (fileToCopy in files) {
            checkForPause()
            val destination = File(to, fileToCopy.absolutePath.substring(offset))
            if (mustBeCopied(fileToCopy, destination)) {
                val readFromBuffer = mdFrom.canReadFromBuffer()
                logger.log(
                    Level.INFO,
                    "copying ${fileToCopy.absolutePath} (readFromBuffer is $readFromBuffer)",
                )
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
                logger.log(Level.INFO, "no need to copy")
            }
            val percent = (ratio * ++numberOfProcessedFiles).toInt()
            if (percent % 10 == 0 && lastPrinted != percent) {
                onMessage("$percent percent done")
                lastPrinted = percent
            }
        }
        onMessage(blockingGetString(Res.string.finished_copying))
    }

    private fun deleteOrphans(sourceDir: File, destDir: File, ignores: List<String>, onMessage: (String) -> Unit) {
        var offset = destDir.absolutePath.length
        onMessage(blockingGetString(Res.string.started_deleting))
        val store = FileStore(this)
        val files = store.fill(destDir, ignores) ?: return
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
        }
        deleteOrphanedDirs(destDir, onMessage)
        onMessage(blockingGetString(Res.string.finished_deleting))
    }

    private fun deleteOrphanedDirs(base: File, onMessage: (String) -> Unit) {
        val folders = FolderMap()
        folders.fill(base)
        val iterator = folders.iterator
        while (iterator.hasNext()) {
            checkForPause()
            val f = iterator.next()
            val absolutePath = f.absolutePath
            if (!f.isDirectory) {
                logger.log(Level.SEVERE, "$absolutePath is not a directory")
                continue
            }
            val children = f.list()
            if (children != null && children.isEmpty()) {
                logger.log(Level.INFO, "deleting directory $absolutePath")
                if (!f.delete()) {
                    onMessage(blockingGetString(Res.string.could_not_delete_path, absolutePath))
                }
            }
        }
    }

    @Synchronized
    private fun mustBeCopied(fileToCopy: File, destination: File): Boolean {
        logger.log(Level.INFO, "preparing to copy ${fileToCopy.absolutePath}")
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
