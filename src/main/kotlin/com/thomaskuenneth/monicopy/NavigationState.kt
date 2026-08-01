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
package com.thomaskuenneth.monicopy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class NavigationState {
    var canNavigateBack by mutableStateOf(false)
        private set

    var navigateBack: () -> Unit by mutableStateOf({})
        private set

    fun update(canNavigateBack: Boolean, navigateBack: () -> Unit) {
        this.canNavigateBack = canNavigateBack
        this.navigateBack = navigateBack
    }

    fun clear() {
        canNavigateBack = false
        navigateBack = {}
    }
}
