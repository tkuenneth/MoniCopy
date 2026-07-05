package com.thomaskuenneth.monicopy.app
import java.util.prefs.Preferences

private const val KEY_COLOR_SCHEME_MODE = "colorSchemeMode"
private const val KEY_SHOW_EXTENDED_ABOUT_DIALOG = "showExtendedAboutDialog"

class JvmAppRepository : AppRepository {
    private val prefs = Preferences.userNodeForPackage(JvmAppRepository::class.java)

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
