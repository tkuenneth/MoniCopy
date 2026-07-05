package com.thomaskuenneth.monicopy.copy

data class CopyPreferences(
    val sourceDir: String? = null,
    val destDir: String? = null,
    val ignores: List<String> = emptyList(),
    val deleteOrphans: Boolean = false,
)

interface CopyRepository {
    fun load(): CopyPreferences
    fun saveSourceDir(path: String?)
    fun saveDestDir(path: String?)
    fun saveDeleteOrphans(enabled: Boolean)
    fun saveIgnores(ignores: List<String>)
}
