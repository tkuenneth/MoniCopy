package com.thomaskuenneth.monicopy.copy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thomaskuenneth.monicopy.blockingGetString
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.add_ignored_directory
import com.thomaskuenneth.monicopy.generated.resources.destination_folder
import com.thomaskuenneth.monicopy.generated.resources.message_template
import com.thomaskuenneth.monicopy.generated.resources.source_folder
import com.thomaskuenneth.monicopy.platform.DirectoryChooser
import com.thomaskuenneth.monicopy.platform.LogTimeFormatter
import com.thomaskuenneth.monicopy.prepareDirectories
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CopyState {
    IDLE, COPYING, COPY_PAUSED, DELETING, DELETE_PAUSED, FINISHED
}

data class CopyUiState(
    val copyState: CopyState = CopyState.IDLE,
    val sourceDir: String? = null,
    val destDir: String? = null,
    val ignores: List<File> = emptyList(),
    val selectedIgnores: Set<File> = emptySet(),
    val deleteOrphans: Boolean = false,
    val logMessages: List<String> = emptyList(),
) {
    val isOperationMode: Boolean
        get() = copyState != CopyState.IDLE
}

class CopyViewModel(
    private val engine: CopyEngine,
    private val repository: CopyRepository,
    private val directoryChooser: DirectoryChooser,
    private val logTimeFormatter: LogTimeFormatter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CopyUiState())
    val uiState: StateFlow<CopyUiState> = _uiState.asStateFlow()

    init {
        engine.copyStateProvider = { _uiState.value.copyState }
        loadPreferences()
        maybePrepareDirectories()
    }

    override fun onCleared() {
        repository.saveIgnores(_uiState.value.ignores.map { it.absolutePath })
        super.onCleared()
    }

    fun addIgnore() {
        val title = blockingGetString(Res.string.add_ignored_directory)
        val result = directoryChooser.chooseDirectory(title, _uiState.value.sourceDir)
        if (result != null) {
            val directory = File(result)
            mutate { state ->
                if (directory in state.ignores) state else state.copy(ignores = state.ignores + directory)
            }
            repository.saveIgnores(_uiState.value.ignores.map { it.absolutePath })
        }
    }

    fun removeSelectedIgnores() {
        mutate { state ->
            state.copy(
                ignores = state.ignores.filterNot { it in state.selectedIgnores },
                selectedIgnores = emptySet(),
            )
        }
        repository.saveIgnores(_uiState.value.ignores.map { it.absolutePath })
    }

    fun toggleIgnoreSelection(directory: File) {
        mutate { state ->
            val selected = if (directory in state.selectedIgnores) {
                state.selectedIgnores - directory
            } else {
                state.selectedIgnores + directory
            }
            state.copy(selectedIgnores = selected)
        }
    }

    fun onDeleteOrphansChanged(enabled: Boolean) {
        mutate { it.copy(deleteOrphans = enabled) }
        repository.saveDeleteOrphans(enabled)
    }

    fun selectSource() {
        val title = blockingGetString(Res.string.source_folder)
        val result = directoryChooser.chooseDirectory(title, _uiState.value.sourceDir)
        if (result != null) {
            repository.saveSourceDir(result)
            mutate { it.copy(sourceDir = result) }
            maybePrepareDirectories()
        }
    }

    fun selectDest() {
        val title = blockingGetString(Res.string.destination_folder)
        val result = directoryChooser.chooseDirectory(title, _uiState.value.destDir)
        if (result != null) {
            repository.saveDestDir(result)
            mutate { it.copy(destDir = result) }
            maybePrepareDirectories()
        }
    }

    fun cancelOperation() {
        when (_uiState.value.copyState) {
            CopyState.COPYING, CopyState.COPY_PAUSED, CopyState.DELETING, CopyState.DELETE_PAUSED -> {
                mutate { it.copy(copyState = CopyState.IDLE, logMessages = emptyList()) }
                engine.cancel()
            }
            CopyState.IDLE, CopyState.FINISHED -> Unit
        }
    }

    fun onActionButtonClick() {
        when (_uiState.value.copyState) {
            CopyState.IDLE -> {
                val from = _uiState.value.sourceDir ?: return
                val to = _uiState.value.destDir ?: return
                mutate { it.copy(copyState = CopyState.COPYING, logMessages = emptyList()) }
                viewModelScope.launch(Dispatchers.Default) {
                    val ignores = _uiState.value.ignores.map { it.absolutePath }
                    engine.copy(from, to, ignores, ::appendLog)
                    if (_uiState.value.copyState == CopyState.IDLE) return@launch
                    nextStep()
                }
            }
            CopyState.COPYING -> {
                mutate { it.copy(copyState = CopyState.COPY_PAUSED) }
            }
            CopyState.COPY_PAUSED -> {
                mutate { it.copy(copyState = CopyState.COPYING) }
                engine.resume()
            }
            CopyState.DELETING -> {
                mutate { it.copy(copyState = CopyState.DELETE_PAUSED) }
            }
            CopyState.DELETE_PAUSED -> {
                mutate { it.copy(copyState = CopyState.DELETING) }
                engine.resume()
            }
            CopyState.FINISHED -> {
                mutate { it.copy(copyState = CopyState.IDLE, logMessages = emptyList()) }
            }
        }
    }

    private fun loadPreferences() {
        val prefs = repository.load()
        _uiState.update {
            it.copy(
                deleteOrphans = prefs.deleteOrphans,
                sourceDir = prefs.sourceDir,
                destDir = prefs.destDir,
                ignores = prefs.ignores.map(::File),
            )
        }
    }

    private fun maybePrepareDirectories() {
        val state = _uiState.value
        if (state.sourceDir != null && state.destDir != null) {
            prepareDirectories(state.sourceDir, state.destDir)
        }
    }

    private fun appendLog(msg: String) {
        val time = logTimeFormatter.format()
        val line = blockingGetString(Res.string.message_template, time, msg).trimEnd()
        mutate { it.copy(logMessages = it.logMessages + line) }
    }

    private fun nextStep() {
        if (_uiState.value.copyState == CopyState.IDLE) return
        when (_uiState.value.copyState) {
            CopyState.COPYING -> {
                if (_uiState.value.deleteOrphans) {
                    val from = _uiState.value.sourceDir ?: return
                    val to = _uiState.value.destDir ?: return
                    var shouldDelete = false
                    mutate { state ->
                        if (state.copyState == CopyState.IDLE) state
                        else {
                            shouldDelete = true
                            state.copy(copyState = CopyState.DELETING)
                        }
                    }
                    if (!shouldDelete) return
                    viewModelScope.launch(Dispatchers.Default) {
                        val ignores = _uiState.value.ignores.map { it.absolutePath }
                        engine.deleteOrphans(from, to, ignores, ::appendLog)
                        if (_uiState.value.copyState == CopyState.IDLE) return@launch
                        nextStep()
                    }
                } else {
                    mutate { state ->
                        if (state.copyState == CopyState.IDLE) state
                        else state.copy(copyState = CopyState.FINISHED)
                    }
                }
            }
            CopyState.DELETING -> {
                mutate { state ->
                    if (state.copyState == CopyState.IDLE) state
                    else state.copy(copyState = CopyState.FINISHED)
                }
            }
            else -> Unit
        }
    }

    private fun mutate(transform: (CopyUiState) -> CopyUiState) {
        _uiState.update(transform)
    }
}
