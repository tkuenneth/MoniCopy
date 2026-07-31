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
