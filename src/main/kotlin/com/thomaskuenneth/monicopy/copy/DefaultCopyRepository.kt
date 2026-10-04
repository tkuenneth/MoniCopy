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

import org.koin.core.annotation.Single
import java.io.File
import java.util.prefs.Preferences

// Keep former jvm.copy Preferences node so existing installs retain settings.
private val prefs: Preferences =
    Preferences.userRoot().node("com/thomaskuenneth/monicopy/jvm/copy")

@Single
class DefaultCopyRepository : CopyRepository {
    override fun load(): CopyPreferences {
        return CopyPreferences(
            deleteOrphans = prefs.getBoolean(DELETE_ORPHANS, false),
            preserveSymbolicLinks = prefs.getBoolean(PRESERVE_SYMBOLIC_LINKS, true),
            sourceDir = prefs.get(KEY_FILE_FROM, "").takeIf { it.isNotEmpty() },
            destDir = prefs.get(KEY_FILE_TO, "").takeIf { it.isNotEmpty() },
            ignores = readDirectories(KEY_IGNORES),
            histories = DirectoryRole.entries.associateWith { readDirectories(it.historyKey) },
        )
    }

    override fun saveSourceDir(path: String?) {
        prefs.put(KEY_FILE_FROM, path ?: "")
        prefs.flush()
    }

    override fun saveDestDir(path: String?) {
        prefs.put(KEY_FILE_TO, path ?: "")
        prefs.flush()
    }

    override fun saveDeleteOrphans(enabled: Boolean) {
        prefs.putBoolean(DELETE_ORPHANS, enabled)
        prefs.flush()
    }

    override fun savePreserveSymbolicLinks(enabled: Boolean) {
        prefs.putBoolean(PRESERVE_SYMBOLIC_LINKS, enabled)
        prefs.flush()
    }

    override fun saveIgnores(ignores: List<String>) {
        writeDirectories(KEY_IGNORES, ignores)
    }

    override fun saveHistory(role: DirectoryRole, paths: List<String>) {
        writeDirectories(role.historyKey, paths)
    }

    private fun readDirectories(key: String): List<String> =
        prefs.get(key, "").split("\n").filter { it.isNotEmpty() && File(it).isDirectory }

    private fun writeDirectories(key: String, paths: List<String>) {
        prefs.put(key, paths.joinToString("\n"))
        prefs.flush()
    }

    companion object {
        private const val KEY_FILE_FROM = "fileFrom"
        private const val KEY_FILE_TO = "fileTo"
        private const val KEY_IGNORES = "ignores"
        private const val DELETE_ORPHANS = "deleteOrphanedFiles"
        private const val PRESERVE_SYMBOLIC_LINKS = "preserveSymbolicLinks"

        private val DirectoryRole.historyKey: String
            get() = when (this) {
                DirectoryRole.Source -> "fileFromHistory"
                DirectoryRole.Destination -> "fileToHistory"
            }
    }
}
