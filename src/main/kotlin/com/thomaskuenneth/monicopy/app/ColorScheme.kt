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
package com.thomaskuenneth.monicopy.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

enum class ColorSchemeMode {
    System, Light, Dark,
}

@Composable
fun ColorSchemeMode.isDark(): Boolean = when (this) {
    ColorSchemeMode.Dark -> true
    ColorSchemeMode.Light -> false
    ColorSchemeMode.System -> isSystemInDarkTheme()
}

@Composable
fun colorScheme(colorSchemeMode: ColorSchemeMode): ColorScheme = when (colorSchemeMode.isDark()) {
    true -> darkColorScheme()
    false -> lightColorScheme()
}
