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
