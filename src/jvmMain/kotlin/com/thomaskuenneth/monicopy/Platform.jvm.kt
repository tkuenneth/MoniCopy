package com.thomaskuenneth.monicopy

import java.awt.Desktop
import java.awt.desktop.AboutHandler
import java.awt.desktop.PreferencesHandler
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.ResourceBundle
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileSystemView

enum class OperatingSystem {
    Linux, Windows, MacOS, Unknown,
}

actual val platformName: String = buildString {
    append(System.getProperty("os.name") ?: "")
    append(' ')
    append(System.getProperty("os.version") ?: "")
    appendLine()
    append(System.getProperty("java.vendor") ?: "")
    append(' ')
    append(System.getProperty("java.vendor.version") ?: "")
    append(" (")
    append(System.getProperty("os.arch") ?: "")
    append(')')
}

val operatingSystem: OperatingSystem = when {
    platformName.contains("mac os x", ignoreCase = true) -> OperatingSystem.MacOS
    platformName.contains("windows", ignoreCase = true) -> OperatingSystem.Windows
    platformName.contains("linux", ignoreCase = true) -> OperatingSystem.Linux
    else -> OperatingSystem.Unknown
}

actual val appVersion: String = ResourceBundle.getBundle("version").getString("VERSION")

actual fun shouldShowExtendedAboutDialogCheckbox(): Boolean = operatingSystem == OperatingSystem.MacOS

actual fun chooseDirectory(title: String, initialPath: String?): String? {
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

actual fun formatLogTime(): String = DateFormat.getTimeInstance().format(Date())

actual fun validateDirectories(sourcePath: String?, destPath: String?): DirectoryValidationResult {
    if (sourcePath == null || destPath == null) {
        return DirectoryValidationResult(canProceed = false)
    }
    val from = File(sourcePath)
    val to = File(destPath)
    return when {
        !from.canRead() -> DirectoryValidationResult(issue = DirectoryValidationIssue.CannotRead)
        !to.canWrite() -> DirectoryValidationResult(issue = DirectoryValidationIssue.CannotWrite)
        to.absolutePath.contains(from.absolutePath) -> DirectoryValidationResult(issue = DirectoryValidationIssue.Overlap)
        else -> DirectoryValidationResult()
    }
}

actual fun prepareDirectories(sourcePath: String, destPath: String) {
    File(sourcePath).mkdirs()
    File(destPath).mkdirs()
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

fun Desktop.installPreferencesHandler(handler: PreferencesHandler) {
    if (isSupported(Desktop.Action.APP_PREFERENCES)) {
        setPreferencesHandler(handler)
    }
}

fun Desktop.installAboutHandler(handler: AboutHandler?) {
    if (isSupported(Desktop.Action.APP_ABOUT)) {
        setAboutHandler(handler)
    }
}
