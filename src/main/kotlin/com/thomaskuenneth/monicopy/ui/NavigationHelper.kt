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

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.thomaskuenneth.monicopy.NavigationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun <T> NavigationHelper(
    navigator: ThreePaneScaffoldNavigator<T>,
    navigationState: NavigationState,
    coroutineScope: CoroutineScope,
) {
    LaunchedEffect(navigator) {
        snapshotFlow { navigator.canNavigateBack() }
            .collect { canNavigateBack ->
                navigationState.update(
                    canNavigateBack = canNavigateBack,
                    navigateBack = { coroutineScope.launch { navigator.navigateBack() } },
                )
            }
    }
    DisposableEffect(Unit) {
        onDispose { navigationState.clear() }
    }
}
