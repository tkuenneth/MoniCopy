package com.thomaskuenneth.monicopy.copy

import java.io.File
import java.util.prefs.Preferences

class JvmCopyRepository : CopyRepository {
    private val prefs = Preferences.userNodeForPackage(JvmCopyRepository::class.java)

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
