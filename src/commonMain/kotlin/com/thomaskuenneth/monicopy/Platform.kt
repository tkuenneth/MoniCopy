package com.thomaskuenneth.monicopy

expect val platformName: String

expect val appVersion: String

expect fun shouldShowExtendedAboutDialogCheckbox(): Boolean

expect fun chooseDirectory(title: String, initialPath: String?): String?

expect fun formatLogTime(): String

enum class DirectoryValidationIssue {
    CannotRead, CannotWrite, Overlap,
}

data class DirectoryValidationResult(
    val issue: DirectoryValidationIssue? = null,
    val canProceed: Boolean = issue == null,
)

expect fun validateDirectories(sourcePath: String?, destPath: String?): DirectoryValidationResult

expect fun prepareDirectories(sourcePath: String, destPath: String)
