package com.thomaskuenneth.monicopy.jvm.platform

import com.thomaskuenneth.monicopy.platform.DirectoryChooser
import org.koin.core.annotation.Single
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileSystemView

@Single
class JvmDirectoryChooser : DirectoryChooser {
    override fun chooseDirectory(title: String, initialPath: String?): String? {
        val initial = initialPath?.let(::File)
        if (SwingUtilities.isEventDispatchThread()) {
            return showDirectoryChooser(title, initial)?.absolutePath
        }
        val result = AtomicReference<File?>()
        SwingUtilities.invokeAndWait {
            result.set(showDirectoryChooser(title, initial))
        }
        return result.get()?.absolutePath
    }

    private fun showDirectoryChooser(title: String, initial: File?): File? {
        val chooser = JFileChooser(initial?.takeIf { it.isDirectory }, FileSystemView.getFileSystemView())
        chooser.dialogTitle = title
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        chooser.isAcceptAllFileFilterUsed = true
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile
        } else {
            null
        }
    }
}
