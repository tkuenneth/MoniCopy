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
package com.thomaskuenneth.monicopy.ui

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thomaskuenneth.monicopy.NavigationState
import com.thomaskuenneth.monicopy.app.AppViewModel
import com.thomaskuenneth.monicopy.app.colorScheme
import com.thomaskuenneth.monicopy.copy.CopyViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MoniCopyApp(
    appViewModel: AppViewModel = koinViewModel(),
    platformContent: @Composable (AppViewModel, NavigationState) -> Unit = { _, _ -> },
) {
    val copyViewModel: CopyViewModel = koinViewModel()
    val appUiState by appViewModel.uiState.collectAsStateWithLifecycle()
    val copyUiState by copyViewModel.uiState.collectAsStateWithLifecycle()
    val navigationState = remember { NavigationState() }

    MaterialTheme(
        colorScheme = colorScheme(appUiState.colorSchemeMode),
        motionScheme = MotionScheme.expressive(),
    ) {
        MoniCopyScreen(
            uiState = copyUiState,
            viewModel = copyViewModel,
            navigationState = navigationState,
        )
        AppBottomSheets(
            appViewModel = appViewModel,
            appUiState = appUiState,
            preserveSymbolicLinks = copyUiState.preserveSymbolicLinks,
            onPreserveSymbolicLinksChanged = copyViewModel::onPreserveSymbolicLinksChanged,
        )
        platformContent(appViewModel, navigationState)
    }
}
