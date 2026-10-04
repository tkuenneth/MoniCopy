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

import androidx.window.core.layout.WindowSizeClass
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Insets
import java.awt.desktop.AboutHandler
import java.awt.desktop.PreferencesHandler
import java.awt.desktop.QuitHandler

fun Insets.minimumWindowSize(): Dimension = Dimension(
    WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND + left + right,
    WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND + top + bottom,
)

fun Desktop.installPreferencesHandler(handler: PreferencesHandler) {
    if (isSupported(Desktop.Action.APP_PREFERENCES)) {
        setPreferencesHandler(handler)
    }
}

fun Desktop.installAboutHandler(handler: AboutHandler?) {
    if (isSupported(Desktop.Action.APP_ABOUT)) {
        setAboutHandler(handler)
    }
}

fun Desktop.installQuitHandler(handler: QuitHandler) {
    if (isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
        setQuitHandler(handler)
    }
}
