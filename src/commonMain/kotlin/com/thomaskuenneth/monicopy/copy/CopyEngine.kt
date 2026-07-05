package com.thomaskuenneth.monicopy.copy

interface CopyEngine {
    var copyStateProvider: () -> CopyState
    fun resume()
    fun copy(fromPath: String, toPath: String, ignores: List<String>, onMessage: (String) -> Unit)
    fun deleteOrphans(
        sourcePath: String,
        destPath: String,
        ignores: List<String>,
        onMessage: (String) -> Unit,
    )
}
