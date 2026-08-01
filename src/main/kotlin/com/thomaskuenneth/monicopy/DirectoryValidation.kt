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
