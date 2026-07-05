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
import com.thomaskuenneth.monicopy.generated.resources.close
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

@Composable
fun actionButtonText(copyState: CopyState): String = stringResource(
    when (copyState) {
        CopyState.IDLE -> Res.string.start
        CopyState.COPYING, CopyState.DELETING -> Res.string.pause
        CopyState.COPY_PAUSED, CopyState.DELETE_PAUSED -> Res.string.action_continue
        CopyState.FINISHED -> Res.string.close
    },
)

fun DirectoryValidationResult.isActionButtonEnabled(copyState: CopyState): Boolean =
    canProceed || copyState != CopyState.IDLE
