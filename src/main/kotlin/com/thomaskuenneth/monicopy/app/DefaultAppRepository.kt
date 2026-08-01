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

import org.koin.core.annotation.Single
import java.util.prefs.Preferences

private const val KEY_COLOR_SCHEME_MODE = "colorSchemeMode"
private const val KEY_SHOW_EXTENDED_ABOUT_DIALOG = "showExtendedAboutDialog"

// Keep former jvm.app Preferences node so existing installs retain settings.
private val prefs: Preferences =
    Preferences.userRoot().node("com/thomaskuenneth/monicopy/jvm/app")

@Single
class DefaultAppRepository : AppRepository {
    override fun getColorSchemeMode(): ColorSchemeMode =
        ColorSchemeMode.valueOf(prefs.get(KEY_COLOR_SCHEME_MODE, ColorSchemeMode.System.name))

    override fun setColorSchemeMode(value: ColorSchemeMode) {
        prefs.put(KEY_COLOR_SCHEME_MODE, value.name)
        prefs.flush()
    }

    override fun getShowExtendedAboutDialog(): Boolean =
        prefs.getBoolean(KEY_SHOW_EXTENDED_ABOUT_DIALOG, false)

    override fun setShowExtendedAboutDialog(value: Boolean) {
        prefs.putBoolean(KEY_SHOW_EXTENDED_ABOUT_DIALOG, value)
        prefs.flush()
    }
}
