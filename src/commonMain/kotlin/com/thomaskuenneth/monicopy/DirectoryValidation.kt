package com.thomaskuenneth.monicopy

import java.io.File

enum class DirectoryValidationIssue {
    CannotRead, CannotWrite, Overlap,
}

data class DirectoryValidationResult(
    val issue: DirectoryValidationIssue? = null,
    val canProceed: Boolean = issue == null,
)

fun validateDirectories(sourcePath: String?, destPath: String?): DirectoryValidationResult {
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

fun prepareDirectories(sourcePath: String, destPath: String) {
    File(sourcePath).mkdirs()
    File(destPath).mkdirs()
}
