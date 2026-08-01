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
package com.thomaskuenneth.monicopy.platform

import org.koin.core.annotation.Single
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileSystemView

@Single
class SwingDirectoryChooser : DirectoryChooser {
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
