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
package com.thomaskuenneth.monicopy.screenshots

import com.thomaskuenneth.monicopy.app.DefaultWindowRepository
import com.thomaskuenneth.monicopy.app.WindowPreferences
import com.thomaskuenneth.monicopy.copy.DefaultCopyRepository
import com.thomaskuenneth.monicopy.copy.DirectoryRole
import java.io.File
import kotlin.random.Random

private const val SCREENSHOT_ROOT = "/tmp/monicopy-screenshots"
private const val FOLDERS = 300
private const val FILES_PER_FOLDER = 200
private const val ORPHAN_FOLDERS = 300
private const val ALREADY_THERE_RATIO = 0.4
private const val WINDOW_WIDTH = 600f
private const val WINDOW_HEIGHT = 480f
private const val WINDOW_POSITION = 120f

fun main() {
    val root = File(SCREENSHOT_ROOT)
    root.deleteRecursively()
    val source = File(root, "source")
    val destination = File(root, "destination")
    val ignored = File(source, "Outdated")
    val recentSources = listOf(File(root, "Photos"), File(root, "Projects"))
    val recentDestinations = listOf(File(root, "Archive"))
    (recentSources + recentDestinations + ignored).forEach { it.mkdirs() }

    val random = Random(42)
    writeTree(source, "Folder", FOLDERS, random) { sourceFile, index ->
        if (index < FOLDERS * ALREADY_THERE_RATIO) {
            val copy = File(destination, sourceFile.relativeTo(source).path)
            copy.parentFile.mkdirs()
            sourceFile.copyTo(copy)
            copy.setLastModified(sourceFile.lastModified())
        }
    }
    writeTree(ignored, "Old", 2, random)
    writeTree(File(destination, "Orphans"), "Orphan", ORPHAN_FOLDERS, random)

    with(DefaultCopyRepository()) {
        saveSourceDir(source.path)
        saveDestDir(destination.path)
        saveDeleteOrphans(true)
        saveIgnores(listOf(ignored.path))
        saveHistory(DirectoryRole.Source, (listOf(source) + recentSources).map(File::getPath))
        saveHistory(DirectoryRole.Destination, (listOf(destination) + recentDestinations).map(File::getPath))
    }
    DefaultWindowRepository().save(
        WindowPreferences(
            x = WINDOW_POSITION,
            y = WINDOW_POSITION,
            width = WINDOW_WIDTH,
            height = WINDOW_HEIGHT,
            placement = "Floating",
        ),
    )
}

private fun writeTree(
    root: File,
    prefix: String,
    folders: Int,
    random: Random,
    onFile: (File, Int) -> Unit = { _, _ -> },
) {
    repeat(folders) { folderIndex ->
        val folder = File(root, "$prefix ${folderIndex + 1}").apply { mkdirs() }
        repeat(FILES_PER_FOLDER) { fileIndex ->
            val file = File(folder, "file ${fileIndex + 1}.txt")
            file.writeBytes(random.nextBytes(random.nextInt(512, 4096)))
            onFile(file, folderIndex)
        }
    }
}
