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

@Single
class DefaultWindowRepository(
    private val prefs: Preferences = Preferences.userRoot().node("com/thomaskuenneth/monicopy/jvm/window"),
) : WindowRepository {
    override fun load(): WindowPreferences = WindowPreferences(
        x = prefs.getFloatOrNull(KEY_X),
        y = prefs.getFloatOrNull(KEY_Y),
        width = prefs.getFloatOrNull(KEY_WIDTH),
        height = prefs.getFloatOrNull(KEY_HEIGHT),
        placement = prefs.get(KEY_PLACEMENT, null),
        isMinimized = prefs.getBoolean(KEY_IS_MINIMIZED, false),
    )

    override fun save(preferences: WindowPreferences) {
        preferences.x.putOrRemove(KEY_X)
        preferences.y.putOrRemove(KEY_Y)
        preferences.width.putOrRemove(KEY_WIDTH)
        preferences.height.putOrRemove(KEY_HEIGHT)
        if (preferences.placement != null) {
            prefs.put(KEY_PLACEMENT, preferences.placement)
        } else {
            prefs.remove(KEY_PLACEMENT)
        }
        prefs.putBoolean(KEY_IS_MINIMIZED, preferences.isMinimized)
        prefs.flush()
    }

    private fun Float?.putOrRemove(key: String) {
        if (this != null) {
            prefs.putFloat(key, this)
        } else {
            prefs.remove(key)
        }
    }

    private fun Preferences.getFloatOrNull(key: String): Float? =
        if (key in keys()) getFloat(key, 0f) else null

    companion object {
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
        private const val KEY_WIDTH = "width"
        private const val KEY_HEIGHT = "height"
        private const val KEY_PLACEMENT = "placement"
        private const val KEY_IS_MINIMIZED = "isMinimized"
    }
}
