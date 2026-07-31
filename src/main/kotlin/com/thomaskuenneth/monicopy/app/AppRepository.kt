package com.thomaskuenneth.monicopy.app

interface AppRepository {
    fun getColorSchemeMode(): ColorSchemeMode
    fun setColorSchemeMode(value: ColorSchemeMode)
    fun getShowExtendedAboutDialog(): Boolean
    fun setShowExtendedAboutDialog(value: Boolean)
}
