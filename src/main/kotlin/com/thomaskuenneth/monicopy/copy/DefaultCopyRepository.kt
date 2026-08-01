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
        val ignores = prefs.get(KEY_IGNORES, "").split("\n")
            .filter { it.isNotEmpty() && File(it).isDirectory }
        return CopyPreferences(
            deleteOrphans = prefs.getBoolean(DELETE_ORPHANS, false),
            sourceDir = prefs.get(KEY_FILE_FROM, "").takeIf { it.isNotEmpty() },
            destDir = prefs.get(KEY_FILE_TO, "").takeIf { it.isNotEmpty() },
            ignores = ignores,
        )
    }

    override fun saveSourceDir(path: String?) {
        prefs.put(KEY_FILE_FROM, path ?: "")
    }

    override fun saveDestDir(path: String?) {
        prefs.put(KEY_FILE_TO, path ?: "")
    }

    override fun saveDeleteOrphans(enabled: Boolean) {
        prefs.putBoolean(DELETE_ORPHANS, enabled)
    }

    override fun saveIgnores(ignores: List<String>) {
        prefs.put(KEY_IGNORES, ignores.joinToString("\n"))
        prefs.flush()
    }

    companion object {
        private const val KEY_FILE_FROM = "fileFrom"
        private const val KEY_FILE_TO = "fileTo"
        private const val KEY_IGNORES = "ignores"
        private const val DELETE_ORPHANS = "deleteOrphanedFiles"
    }
}
