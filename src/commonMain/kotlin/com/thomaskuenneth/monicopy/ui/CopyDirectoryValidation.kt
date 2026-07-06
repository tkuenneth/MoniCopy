package com.thomaskuenneth.monicopy.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.thomaskuenneth.monicopy.DirectoryValidationIssue
import com.thomaskuenneth.monicopy.DirectoryValidationResult
import com.thomaskuenneth.monicopy.copy.CopyState
import com.thomaskuenneth.monicopy.generated.resources.Res
import com.thomaskuenneth.monicopy.generated.resources.action_continue
import com.thomaskuenneth.monicopy.generated.resources.cannot_read
import com.thomaskuenneth.monicopy.generated.resources.cannot_write
import com.thomaskuenneth.monicopy.generated.resources.finish
import com.thomaskuenneth.monicopy.generated.resources.no_overlap
import com.thomaskuenneth.monicopy.generated.resources.pause
import com.thomaskuenneth.monicopy.generated.resources.start
import com.thomaskuenneth.monicopy.validateDirectories
import org.jetbrains.compose.resources.stringResource

@Composable
fun rememberDirectoryValidation(sourceDir: String?, destDir: String?): DirectoryValidationResult =
    remember(sourceDir, destDir) { validateDirectories(sourceDir, destDir) }

@Composable
fun DirectoryValidationResult.warningMessage(): String = when (issue) {
    null -> ""
    DirectoryValidationIssue.CannotRead -> stringResource(Res.string.cannot_read)
    DirectoryValidationIssue.CannotWrite -> stringResource(Res.string.cannot_write)
    DirectoryValidationIssue.Overlap -> stringResource(Res.string.no_overlap)
}

enum class ActionButtonLabel {
    Start, Pause, Continue, Finish,
}

fun CopyState.toActionButtonLabel(): ActionButtonLabel = when (this) {
    CopyState.IDLE -> ActionButtonLabel.Start
    CopyState.COPYING, CopyState.DELETING -> ActionButtonLabel.Pause
    CopyState.COPY_PAUSED, CopyState.DELETE_PAUSED -> ActionButtonLabel.Continue
    CopyState.FINISHED -> ActionButtonLabel.Finish
}

@Composable
fun actionButtonText(label: ActionButtonLabel): String = stringResource(
    when (label) {
        ActionButtonLabel.Start -> Res.string.start
        ActionButtonLabel.Pause -> Res.string.pause
        ActionButtonLabel.Continue -> Res.string.action_continue
        ActionButtonLabel.Finish -> Res.string.finish
    },
)

fun DirectoryValidationResult.isActionButtonEnabled(copyState: CopyState): Boolean =
    canProceed || copyState != CopyState.IDLE

sealed interface ActionBarState {
    data object Setup : ActionBarState
    data object InProgress : ActionBarState
    data object Finished : ActionBarState
}

fun CopyState.toActionBarState(): ActionBarState = when (this) {
    CopyState.IDLE -> ActionBarState.Setup
    CopyState.FINISHED -> ActionBarState.Finished
    CopyState.COPYING, CopyState.COPY_PAUSED, CopyState.DELETING, CopyState.DELETE_PAUSED -> ActionBarState.InProgress
}

fun CopyState.toInProgressCopyState(): CopyState = when (this) {
    CopyState.COPY_PAUSED, CopyState.DELETE_PAUSED, CopyState.DELETING -> this
    else -> CopyState.COPYING
}
